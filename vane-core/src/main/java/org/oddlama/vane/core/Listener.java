package org.oddlama.vane.core;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;
import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.plugin.Plugin;

import org.oddlama.vane.annotation.command.Aliases;
import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.annotation.command.VaneCommand;
import org.oddlama.vane.annotation.lang.LangString;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.ModuleComponent;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.core.command.params.AnyParam;

public class Listener<T extends Module<T>> extends ModuleComponent<T> implements org.bukkit.event.Listener {
	public Listener(Context<T> context) {
		super(context);
	}

	@Override
	protected void on_enable() {
		get_module().register_listener(this);
	}

	@Override
	protected void on_disable() {
		get_module().unregister_listener(this);
	}
}
