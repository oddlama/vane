package org.oddlama.vane.core;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

import org.oddlama.vane.annotation.VaneModule;
import org.oddlama.vane.annotation.config.ConfigVersion;
import org.oddlama.vane.annotation.lang.LangString;
import org.oddlama.vane.annotation.lang.LangVersion;
import org.oddlama.vane.core.commands.CommandVane;

@VaneModule("core")
public class Core extends Module {
	@ConfigVersion(1)
	public long config_version;

	@LangVersion(1)
	public long lang_version;

	@LangString
	public String lang_command_not_a_player;

	// Variables
	private CommandVane command_vane = new CommandVane(this);
	private SortedSet<Module> vane_modules = new TreeSet<>((a, b) -> a.get_name().compareTo(b.get_name()));

	@Override
	protected void on_enable() {
		register_command(command_vane);
	}

	@Override
	protected void on_disable() {
		unregister_command(command_vane);
	}

	@Override
	protected void on_config_change() {
	}

	public void register_module(Module module) {
		vane_modules.add(module);
	}

	public void unregister_module(Module module) {
		vane_modules.remove(module);
	}

	public SortedSet<Module> get_modules() {
		return Collections.unmodifiableSortedSet(vane_modules);
	}
}
