package org.oddlama.vane.permissions;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.RemoteServerCommandEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionDefault;
import org.oddlama.vane.annotation.VaneModule;
import org.oddlama.vane.annotation.config.ConfigBoolean;
import org.oddlama.vane.annotation.config.ConfigString;
import org.oddlama.vane.annotation.config.ConfigStringListMap;
import org.oddlama.vane.annotation.config.ConfigStringListMapEntry;
import org.oddlama.vane.annotation.persistent.Persistent;
import org.oddlama.vane.core.module.Module;

@VaneModule(name = "permissions", bstats = 8641, config_version = 1, lang_version = 1, storage_version = 1)
public class Permissions extends Module<Permissions> {

	// Configuration
	@ConfigBoolean(
		def = false,
		desc = "Remove all default permissions from ANY SOURCE (including other plugins and minecraft permissions) to start with a clean preset. This will allow you to exactly set which player have which permissions instead of having to resort to volatile stateful changes like negative permissions. This will result in OPed players to lose access to commands, if not explicitly added back via permissions. The wildcard permissions can be viewed using `perms list permissions`. The wildcard permissions `minecraft` and `craftbukkit` may be especially useful."
	)
	public boolean config_remove_defaults;

	@ConfigString(
		def = "default",
		desc = "The permission group that will be given to players that have no other permission group."
	)
	public String config_default_group;

	@ConfigStringListMap(
		def = {
			@ConfigStringListMapEntry(
				key = "default",
				list = { "bukkit.command.help", "bukkit.broadcast", "bukkit.broadcast.user" }
			),
			@ConfigStringListMapEntry(
				key = "user",
				list = {
					"vane.permissions.groups.default",
					"vane.admin.modify_world",
					"vane.regions.commands.region",
					"vane.trifles.commands.heads",
				}
			),
			@ConfigStringListMapEntry(
				key = "verified",
				list = { "vane.permissions.groups.user", "vane.permissions.commands.vouch" }
			),
			@ConfigStringListMapEntry(
				key = "admin",
				list = {
					"vane.permissions.groups.verified",
					"vane.admin.bypass_spawn_protection",
					"vane.portals.admin",
					"vane.regions.admin",
					"vane.*.commands.*",
				}
			),
		},
		desc = "The permission groups. A player can have multiple permission groups assigned. Permission groups can inherit other permission groups by specifying vane.permissions.groups.<groupname> as a permission."
	)
	public Map<String, List<String>> config_groups;

	// Persistent storage
	@Persistent
	public Map<UUID, Set<String>> storage_player_groups = new HashMap<>();

	// Variables
	public final Map<String, Set<String>> permission_groups = new HashMap<>();
	private final Map<UUID, PermissionAttachment> player_attachments = new HashMap<>();

	public Permissions() {
		new org.oddlama.vane.permissions.commands.Permission(this);
		new org.oddlama.vane.permissions.commands.Vouch(this);
	}

	@Override
	public void on_enable() {
		schedule_next_tick(() -> {
			if (config_remove_defaults) {
				for (var perm : getServer().getPluginManager().getPermissions()) {
					perm.setDefault(PermissionDefault.FALSE);
					getServer().getPluginManager().recalculatePermissionDefaults(perm);

					// But still allow the console to execute commands
					Permissions.this.add_console_permission(perm);
				}
			}
		});
	}

	@Override
	public void on_config_change() {
		flatten_groups();
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void on_player_join(PlayerJoinEvent event) {
		register_player(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void on_player_kick(PlayerKickEvent event) {
		unregister_player(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void on_player_quit(PlayerQuitEvent event) {
		unregister_player(event.getPlayer());
	}

	private final Map<CommandSender, PermissionAttachment> sender_attachments = new HashMap<>();

	private void add_console_permissions(final CommandSender sender) {
		// Register attachment for sender if not done already
		if (!sender_attachments.containsKey(sender)) {
			final var attachment = sender.addAttachment(this);
			sender_attachments.put(sender, attachment);

			final var attached_perms = console_attachment.getPermissions();
			attached_perms.forEach((p, v) -> attachment.setPermission(p, v));
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void on_server_command_event(ServerCommandEvent event) {
		final var sender = event.getSender();
		if (sender instanceof Player && sender.isOp()) {
			// Console command sender will always have the correct permission attachment
			// Command block shall be ignored for now (causes lag, see #178)
			add_console_permissions(sender);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void on_remote_server_command_event(RemoteServerCommandEvent event) {
		final var sender = event.getSender();
		if (sender.isOp()) {
			add_console_permissions(sender);
		}
	}

	/** Resolve references to other permission groups in the hierarchy. */
	private void flatten_groups() {
		permission_groups.clear();
		config_groups.forEach((k, v) -> {
			final var set = new HashSet<String>();
			for (var perm : v) {
				if (perm.startsWith("vane.permissions.groups.")) {
					// Resolving will be delayed to second pass
				} else {
					set.add(perm);
				}
			}
			permission_groups.put(k, set);
		});

		// Resolve group inheritance
		var modified = new Object() {
			boolean value = false;
		};
		do {
			modified.value = false;
			config_groups.forEach((k, v) -> {
				final var set = permission_groups.get(k);
				for (var perm : v) {
					if (perm.startsWith("vane.permissions.groups.")) {
						final var group = perm.substring("vane.permissions.groups.".length());
						final var group_perms = permission_groups.get(group);
						if (group_perms == null) {
							log.severe(
								"Nonexistent permission group '" +
								group +
								"' referenced by group '" +
								k +
								"'; Ignoring statement!"
							);
							continue;
						}
						modified.value |= set.addAll(group_perms);
					}
				}
			});
		} while (modified.value);
	}

	private void register_player(final Player player) {
		// Register PermissionAttachment
		final var attachment = player.addAttachment(this);
		player_attachments.put(player.getUniqueId(), attachment);

		// Attach permissions
		recalculate_player_permissions(player);
	}

	public void recalculate_player_permissions(final Player player) {
		// Clear attachment
		final var attachment = player_attachments.get(player.getUniqueId());
		final var attached_perms = attachment.getPermissions();
		attached_perms.forEach((p, v) -> attachment.unsetPermission(p));

		// Add permissions again
		var groups = storage_player_groups.get(player.getUniqueId());
		if (groups == null || groups.isEmpty()) {
			// Assign player to a default permission group
			groups = Set.of(config_default_group);
		}

		for (var group : groups) {
			for (var p : permission_groups.getOrDefault(group, Collections.emptySet())) {
				final var perm = getServer().getPluginManager().getPermission(p);
				if (perm == null) {
					log.warning("Use of unregistered permission '" + p + "' might have unintended effects.");
				}
				attachment.setPermission(p, true);
			}
		}

		// Update list of commands for client side root tab completion
		player.updateCommands();
	}

	private void unregister_player(final Player player) {
		final var attachment = player_attachments.remove(player.getUniqueId());
		if (attachment != null) {
			player.removeAttachment(attachment);
		}
	}

	public void save_and_recalculate(final OfflinePlayer player) {
		mark_persistent_storage_dirty();

		// Recalculate permissions if player is currently online
		if (player.isOnline()) {
			recalculate_player_permissions(player.getPlayer());
		}
	}

	public boolean add_player_to_group(final OfflinePlayer player, final String group) {
		var set = storage_player_groups.computeIfAbsent(player.getUniqueId(), k -> new HashSet<String>());

		final var added = set.add(group);
		if (added) {
			log.info("[audit] Group " + group + " assigned to " + player.getUniqueId() + " (" + player.getName() + ")");
			save_and_recalculate(player);
		}

		return added;
	}

	public boolean remove_player_from_group(final OfflinePlayer player, final String group) {
		var set = storage_player_groups.get(player.getUniqueId());
		var removed = false;
		if (set != null) {
			removed = set.remove(group);
		}

		if (removed) {
			log.info(
				"[audit] Group " + group + " removed from " + player.getUniqueId() + " (" + player.getName() + ")"
			);
			save_and_recalculate(player);
		}

		return removed;
	}
}
