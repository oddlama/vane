package org.oddlama.vane.permissions.commands;

import java.util.Collections;
import java.util.HashSet;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.PermissionDefault;

import org.oddlama.vane.annotation.command.Aliases;
import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.annotation.lang.LangString;
import org.oddlama.vane.core.command.Command;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.permissions.Permissions;
import org.oddlama.vane.util.Message;

@Name("permission")
@Aliases({"perm"})
public class Permission extends Command<Permissions> {
	@LangString  private String  lang_list_header_groups;
	@LangString  private String  lang_list_header_permissions;
	@LangMessage private Message lang_list_header_player_groups;
	@LangMessage private Message lang_list_header_player_permissions;
	@LangMessage private Message lang_list_header_group_permissions;
	@LangString  private String  lang_list_player_offline;
	@LangMessage private Message lang_list_group;
	@LangMessage private Message lang_list_permission;
	@LangMessage private Message lang_group_assigned;
	@LangMessage private Message lang_group_removed;
	@LangMessage private Message lang_group_already_assigned;
	@LangMessage private Message lang_group_not_assigned;

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
		permissions.choice("permission_group",
					sender -> get_module().permission_groups.keySet(),
					(sender, g) -> g,
					(sender, str) -> get_module().permission_groups.containsKey(str) ? str : null)
			.exec(this::list_permissions_for_group);

		// add group to player
		params().fixed("add").ignore_case()
			.choose_any_player()
			.choice("permission_group",
					sender -> get_module().permission_groups.keySet(),
					(sender, g) -> g,
					(sender, str) -> get_module().permission_groups.containsKey(str) ? str : null)
			.exec(this::add_player_to_group);

		// remove group from player
		params().fixed("remove").ignore_case()
			.choose_any_player()
			.choice("permission_group",
					sender -> get_module().permission_groups.keySet(),
					(sender, g) -> g,
					(sender, str) -> get_module().permission_groups.containsKey(str) ? str : null)
			.exec(this::remove_player_from_group);
	}

	private String permission_default_value_color_code(PermissionDefault def) {
		switch (def) {
			default:     return "§6";
			case FALSE:  return "§c";
			case NOT_OP: return "§5";
			case OP:     return "§b";
			case TRUE:   return "§a";
		}
	}

	private String permission_value_color_code(boolean value) {
		return permission_default_value_color_code(value ? PermissionDefault.TRUE : PermissionDefault.FALSE);
	}

	private void list_groups(CommandSender sender) {
		sender.sendMessage(lang_list_header_groups);
		get_module().permission_groups.keySet()
			.stream()
			.sorted((a, b) -> a.compareTo(b))
			.forEach(group -> {
				sender.sendMessage(lang_list_group.format(group));
			});
	}

	private void list_permissions(CommandSender sender) {
		sender.sendMessage(lang_list_header_permissions);
		get_module().getServer().getPluginManager().getPermissions().stream()
			.sorted((a, b) -> a.getName().compareTo(b.getName()))
			.forEach(perm -> {
				sender.sendMessage(lang_list_permission.format(
							perm.getName(),
							permission_default_value_color_code(perm.getDefault()),
							perm.getDefault().toString().toLowerCase(),
							perm.getDescription()));
			});
	}

	private void list_permissions_for_player(CommandSender sender, OfflinePlayer offline_player) {
		sender.sendMessage(lang_list_header_player_permissions.format(offline_player.getName()));
		var player = offline_player.getPlayer();
		if (player == null) {
			// Player is offline, show configured permissions only.
			// Information from other plugins might be missing.
			sender.sendMessage(lang_list_player_offline);
			final var groups = get_module().storage_player_groups.get(offline_player.getUniqueId());
			if (groups == null) {
				sender.sendMessage("§b∅");
			} else {
				for (var group : groups) {
					list_permissions_for_group_no_header(sender, group);
				}
			}
		} else {
			var effective_permissions = player.getEffectivePermissions();
			if (effective_permissions.isEmpty()) {
				sender.sendMessage("§b∅");
			} else {
				player.getEffectivePermissions()
					.stream()
					.sorted((a, b) -> a.getPermission().compareTo(b.getPermission()))
					.forEach(att -> {
						var perm = get_module().getServer().getPluginManager().getPermission(att.getPermission());
						if (perm == null) {
							get_module().log.warning("Encountered unregistered permission '" + att.getPermission() + "'");
							return;
						}
						sender.sendMessage(lang_list_permission.format(
									perm.getName(),
									permission_value_color_code(att.getValue()),
									String.valueOf(att.getValue()),
									perm.getDescription()));
					});
			}
		}
	}

	private void list_permissions_for_group_no_header(CommandSender sender, String group) {
		for (var p : get_module().permission_groups.getOrDefault(group, Collections.emptySet())) {
			var perm = get_module().getServer().getPluginManager().getPermission(p);
			if (perm == null) {
				get_module().log.warning("Use of unregistered permission '" + p + "' might have unintended effects.");
				sender.sendMessage(lang_list_permission.format(
							p,
							permission_value_color_code(true),
							String.valueOf(true),
							""));
			} else {
				sender.sendMessage(lang_list_permission.format(
							perm.getName(),
							permission_value_color_code(true),
							String.valueOf(true),
							perm.getDescription()));
			}
		}
	}

	private void list_permissions_for_group(CommandSender sender, String group) {
		sender.sendMessage(lang_list_header_group_permissions.format(group));
		list_permissions_for_group_no_header(sender, group);
	}

	private void list_groups_for_player(CommandSender sender, OfflinePlayer offline_player) {
		var set = get_module().storage_player_groups.get(offline_player.getUniqueId());
		if (set == null) {
			sender.sendMessage("§b∅");
		} else {
			sender.sendMessage(lang_list_header_player_permissions.format(offline_player.getName()));
			for (var group : set) {
				sender.sendMessage(lang_list_group.format(group));
			}
		}
	}

	private void save_and_recalculate(OfflinePlayer player) {
		get_module().save_persistent_storage();

		// Recalculate permissions if player is currently online
		if (player.isOnline()) {
			get_module().recalculate_player_permissions(player.getPlayer());
		}
	}

	private void add_player_to_group(CommandSender sender, OfflinePlayer player, String group) {
		var set = get_module().storage_player_groups.get(player.getUniqueId());
		if (set == null) {
			set = new HashSet<String>();
			get_module().storage_player_groups.put(player.getUniqueId(), set);
		}
		var added = set.add(group);

		if (added) {
			sender.sendMessage(lang_group_assigned.format(group, player.getName()));
			save_and_recalculate(player);
		} else {
			sender.sendMessage(lang_group_already_assigned.format(group, player.getName()));
		}
	}

	private void remove_player_from_group(CommandSender sender, OfflinePlayer player, String group) {
		var set = get_module().storage_player_groups.get(player.getUniqueId());
		var removed = false;
		if (set != null) {
			removed = set.remove(group);
		}

		if (removed) {
			sender.sendMessage(lang_group_removed.format(group, player.getName()));
			save_and_recalculate(player);
		} else {
			sender.sendMessage(lang_group_not_assigned.format(group, player.getName()));
		}
	}
}
