package org.oddlama.vane.permissions;

import java.util.HashMap;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import org.bukkit.event.player.PlayerQuitEvent;

import org.oddlama.vane.annotation.VaneModule;
import org.oddlama.vane.annotation.config.ConfigVersion;
import org.oddlama.vane.annotation.config.ConfigBoolean;
import org.oddlama.vane.annotation.config.ConfigString;
import org.oddlama.vane.annotation.config.ConfigStringListMap;
import org.oddlama.vane.annotation.config.ConfigStringListMapEntry;
import org.oddlama.vane.annotation.persistent.Persistent;
import org.oddlama.vane.annotation.lang.LangVersion;
import org.oddlama.vane.core.module.Module;

@VaneModule(name = "permissions", bstats = 8641)
public class Permissions extends Module<Permissions> {
	// Configuration
	@ConfigVersion(1)
	public long config_version;

	@ConfigBoolean(def = true, desc = "Remove all default permissions to start with a clean preset.")
	public boolean config_remove_defaults;

	@ConfigString(def = "default", desc = "The permission group that will be given to new players.")
	public String config_default_group;

	@ConfigStringListMap(def = {
		@ConfigStringListMapEntry(key = "default", list = {"bukkit.command.help", "bukkit.broadcast", "bukkit.broadcast.user"}),
		@ConfigStringListMapEntry(key = "admin", list = {"vane.permissions.groups.default"}),
	}, desc = "The permission groups. A player can have multiple permission groups assigned. Permission groups can inherit other permission groups by specifying vane.permissions.groups.<groupname> as a permission.")
	public Map<String, List<String>> config_groups;

	// Language
	@LangVersion(1)
	public long lang_version;

	// Persistent storage
	@Persistent
	public Map<String, List<String>> player_permission_groups = new HashMap<>();

	// Variables
	private final Map<String, List<String>> permission_groups = new HashMap<>();
	private final Map<UUID, PermissionAttachment> player_attachments = new HashMap<>();

	@Override
	public void on_enable() {
		player_permission_groups.put("a", new ArrayList<String>());
		schedule_next_tick(() -> {
			if (config_remove_defaults) {
				for (var perm : getServer().getPluginManager().getPermissions()) {
					perm.setDefault(PermissionDefault.FALSE);
					getServer().getPluginManager().removePermission(perm);
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

	/** Resolve references to other permission groups in the hierarchy. */
	private void flatten_groups() {
		permission_groups.clear();
		config_groups.forEach((k, v) -> {
			final var list = new ArrayList<String>();
			for (var perm : v) {
				if (perm.startsWith("vane.permissions.groups.")) {
					final var group = perm.substring("vane.permissions.groups.".length());
					final var group_perms = permission_groups.get(group);
					if (group_perms == null) {
						log.severe("Permission group '" + group + "' referenced before definition by group '" + k + "'; Ignoring statement!");
						continue;
					}
					list.addAll(group_perms);
				} else {
					list.add(perm);
				}
			}
			permission_groups.put(k, list);
		});
	}

	private void register_player(final Player player) {
		// Register PermissionAttachment
		final var attachment = player.addAttachment(this);
		player_attachments.put(player.getUniqueId(), attachment);

		// TODO command to assign / list / remove groups from players
		// TODO command list all groups

		// Update list of commands for client side root tab completion
		player.updateCommands();
	}

	private void unregister_player(final Player player) {
		final var attachment = player_attachments.remove(player.getUniqueId());
		if (attachment != null) {
			player.removeAttachment(attachment);
		}
	}
}
