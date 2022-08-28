package org.oddlama.vane.permissions.commands;

import java.util.Collections;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.PermissionDefault;
import org.oddlama.vane.annotation.command.Aliases;
import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.command.Command;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.permissions.Permissions;

@Name("permission")
@Aliases({ "perm" })
public class Permission extends Command<Permissions> {

	@LangMessage
	private TranslatedMessage lang_list_empty;

	@LangMessage
	private TranslatedMessage lang_list_header_groups;

	@LangMessage
	private TranslatedMessage lang_list_header_permissions;

	@LangMessage
	private TranslatedMessage lang_list_header_player_groups;

	@LangMessage
	private TranslatedMessage lang_list_header_player_permissions;

	@LangMessage
	private TranslatedMessage lang_list_header_group_permissions;

	@LangMessage
	private TranslatedMessage lang_list_player_offline;

	@LangMessage
	private TranslatedMessage lang_list_group;

	@LangMessage
	private TranslatedMessage lang_list_permission;

	@LangMessage
	private TranslatedMessage lang_group_assigned;

	@LangMessage
	private TranslatedMessage lang_group_removed;

	@LangMessage
	private TranslatedMessage lang_group_already_assigned;

	@LangMessage
	private TranslatedMessage lang_group_not_assigned;

	public Permission(Context<Permissions> context) {
		super(context);
		// Add help
		params().fixed("help").ignore_case().exec(this::print_help);

		// Command parameters
		var list = params().fixed("list").ignore_case();

		// list groups
		var groups = list.fixed("groups").ignore_case();
		groups.exec(this::list_groups);
		groups.choose_any_player().exec(this::list_groups_for_player);

		// list permissions
		var permissions = list.fixed("permissions").ignore_case();
		permissions.exec(this::list_permissions);
		permissions.choose_any_player().exec(this::list_permissions_for_player);
		permissions
			.choice(
				"permission_group",
				sender -> get_module().permission_groups.keySet(),
				(sender, g) -> g,
				(sender, str) -> get_module().permission_groups.containsKey(str) ? str : null
			)
			.exec(this::list_permissions_for_group);

		// add group to player
		params()
			.fixed("add")
			.ignore_case()
			.choose_any_player()
			.choice(
				"permission_group",
				sender -> get_module().permission_groups.keySet(),
				(sender, g) -> g,
				(sender, str) -> get_module().permission_groups.containsKey(str) ? str : null
			)
			.exec(this::add_player_to_group);

		// remove group from player
		params()
			.fixed("remove")
			.ignore_case()
			.choose_any_player()
			.choice(
				"permission_group",
				sender -> get_module().permission_groups.keySet(),
				(sender, g) -> g,
				(sender, str) -> get_module().permission_groups.containsKey(str) ? str : null
			)
			.exec(this::remove_player_from_group);
	}

	private String permission_default_value_color_code(PermissionDefault def) {
		switch (def) {
			default:
				return "§6";
			case FALSE:
				return "§c";
			case NOT_OP:
				return "§5";
			case OP:
				return "§b";
			case TRUE:
				return "§a";
		}
	}

	private String permission_value_color_code(boolean value) {
		return permission_default_value_color_code(value ? PermissionDefault.TRUE : PermissionDefault.FALSE);
	}

	private void list_groups(CommandSender sender) {
		lang_list_header_groups.send(sender);
		get_module()
			.permission_groups.keySet()
			.stream()
			.sorted((a, b) -> a.compareTo(b))
			.forEach(group -> lang_list_group.send(sender, "§b" + group));
	}

	private void list_permissions(CommandSender sender) {
		lang_list_header_permissions.send(sender);
		get_module()
			.getServer()
			.getPluginManager()
			.getPermissions()
			.stream()
			.sorted((a, b) -> a.getName().compareTo(b.getName()))
			.forEach(perm -> lang_list_permission.send(
				sender,
				"§d" + perm.getName(),
				permission_default_value_color_code(perm.getDefault()) + perm.getDefault().toString().toLowerCase(),
				perm.getDescription()
			));
	}

	private void list_permissions_for_player(CommandSender sender, OfflinePlayer offline_player) {
		lang_list_header_player_permissions.send(sender, "§b" + offline_player.getName());
		var player = offline_player.getPlayer();
		if (player == null) {
			// Player is offline, show configured permissions only.
			// Information from other plugins might be missing.
			lang_list_player_offline.send(sender);
			final var groups = get_module().storage_player_groups.get(offline_player.getUniqueId());
			if (groups == null) {
				lang_list_empty.send(sender);
			} else {
				for (var group : groups) {
					list_permissions_for_group_no_header(sender, group);
				}
			}
		} else {
			var effective_permissions = player.getEffectivePermissions();
			if (effective_permissions.isEmpty()) {
				lang_list_empty.send(sender);
			} else {
				player
					.getEffectivePermissions()
					.stream()
					.sorted((a, b) -> a.getPermission().compareTo(b.getPermission()))
					.forEach(att -> {
						var perm = get_module().getServer().getPluginManager().getPermission(att.getPermission());
						if (perm == null) {
							get_module()
								.log.warning("Encountered unregistered permission '" + att.getPermission() + "'");
							return;
						}
						lang_list_permission.send(
							sender,
							"§d" + perm.getName(),
							permission_value_color_code(att.getValue()) + att.getValue(),
							perm.getDescription()
						);
					});
			}
		}
	}

	private void list_permissions_for_group_no_header(CommandSender sender, String group) {
		for (var p : get_module().permission_groups.getOrDefault(group, Collections.emptySet())) {
			var perm = get_module().getServer().getPluginManager().getPermission(p);
			if (perm == null) {
				get_module().log.warning("Use of unregistered permission '" + p + "' might have unintended effects.");
				lang_list_permission.send(
					sender,
					"§d" + p,
					permission_value_color_code(true) + true,
					""
				);
			} else {
				lang_list_permission.send(
					sender,
					"§d" + perm.getName(),
					permission_value_color_code(true) + true,
					perm.getDescription()
				);
			}
		}
	}

	private void list_permissions_for_group(CommandSender sender, String group) {
		lang_list_header_group_permissions.send(sender, "§b" + group);
		list_permissions_for_group_no_header(sender, group);
	}

	private void list_groups_for_player(CommandSender sender, OfflinePlayer offline_player) {
		var set = get_module().storage_player_groups.get(offline_player.getUniqueId());
		if (set == null) {
			lang_list_empty.send(sender);
		} else {
			lang_list_header_player_groups.send(sender, "§b" + offline_player.getName());
			for (var group : set) {
				lang_list_group.send(sender, group);
			}
		}
	}

	private void add_player_to_group(final CommandSender sender, final OfflinePlayer player, final String group) {
		if (get_module().add_player_to_group(player, group)) {
			lang_group_assigned.send(sender, "§b" + player.getName(), "§a" + group);
		} else {
			lang_group_already_assigned.send(sender, "§b" + player.getName(), "§a" + group);
		}
	}

	private void remove_player_from_group(final CommandSender sender, final OfflinePlayer player, final String group) {
		if (get_module().remove_player_from_group(player, group)) {
			lang_group_removed.send(sender, "§b" + player.getName(), "§a" + group);
		} else {
			lang_group_not_assigned.send(sender, "§b" + player.getName(), "§a" + group);
		}
	}
}
