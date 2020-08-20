package org.oddlama.vane.core.module;

import static org.oddlama.vane.util.Util.prepend;

import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.plugin.Plugin;

import org.oddlama.vane.annotation.command.Aliases;
import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.annotation.command.VaneCommand;
import org.oddlama.vane.annotation.lang.LangString;
import org.oddlama.vane.core.command.params.AnyParam;

public abstract class ModuleComponent<T extends Module<T>> {
	private Context<T> context;

	public ModuleComponent(Context<T> context) {
		this.context = context;

		// Compile localization and config fields, and register callbacks
		context.compile(this);
	}

	public Context<T> get_context() {
		return context;
	}

	public T get_module() {
		return context.get_module();
	}

	protected abstract void on_enable();
	protected abstract void on_disable();
	protected void on_config_change() {}
}
