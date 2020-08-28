package org.oddlama.vane.core;

import java.io.IOException;
import java.io.FileOutputStream;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;
import java.io.File;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

import org.oddlama.vane.annotation.VaneModule;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.annotation.lang.LangString;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.util.Message;

@VaneModule(name = "core", bstats = 8637, config_version = 1, lang_version = 1, storage_version = 1)
public class Core extends Module<Core> {
	@LangString
	public String lang_command_not_a_player;
	@LangString
	public String lang_command_permission_denied;

	@LangMessage
	public Message lang_invalid_time_format;

	// Module registry
	private SortedSet<Module<?>> vane_modules = new TreeSet<>((a, b) -> a.get_name().compareTo(b.get_name()));
	public void register_module(Module<?> module) { vane_modules.add(module); }
	public void unregister_module(Module<?> module) { vane_modules.remove(module); }
	public SortedSet<Module<?>> get_modules() { return Collections.unmodifiableSortedSet(vane_modules); }

	// Vane global command catch-all permission
	public Permission permission_command_catchall = new Permission("vane.*.commands.*", "Allow access to all vane commands (ONLY FOR ADMINS!)", PermissionDefault.FALSE);

	public Core() {
		// Create global command catch-all permission
		register_permission(permission_command_catchall);

		// Components
		new org.oddlama.vane.core.commands.Vane(this);
		new CommandHider(this);
	}

	public void create_resource_pack() {
		var pack = new ResourcePackGenerator();
		pack.set_description("Vane plugin resource pack");
		pack.set_icon_png(new File(getDataFolder(), "pack.png"));

		for (var m : vane_modules) {
			m.create_resource_pack(pack);
		}

		pack.write(new File("vane-resource-pack.zip"));
	}
}
