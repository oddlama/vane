package org.oddlama.vane.core.module;

import static org.oddlama.vane.util.Util.prepend;

import java.util.Collections;
import java.util.List;
import java.lang.reflect.Field;

import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.plugin.Plugin;

import org.oddlama.vane.annotation.command.Aliases;
import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.annotation.command.VaneCommand;
import org.oddlama.vane.annotation.config.ConfigBoolean;
import org.oddlama.vane.annotation.lang.LangString;
import org.oddlama.vane.core.command.params.AnyParam;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
/**
 * A ModuleGroup is a ModuleContext that automatically adds an enable variable
 * with description to the context. If the group is disabled, on_enable() will
 * not be called.
 */
public class ModuleGroup<T extends Module<T>> extends ModuleContext<T> {
	@ConfigBoolean(def = true, desc = "")
	private boolean config_enabled;
	private String config_enabled_desc;

	// Having the annotation desc = "" will cause this method to be called instead.
	public String config_enabled_desc() {
		return config_enabled_desc;
	}

	public ModuleGroup(Context<T> context, String group, String description) {
		this(context, group, description, true);
	}

	public ModuleGroup(Context<T> context, String group, String description, boolean compile_self) {
		super(context, group, false);
		this.config_enabled_desc = description;

		if (compile_self) {
			compile_self();
		}
	}

	public boolean enabled() {
		return config_enabled;
	}

	@Override
	public String yaml_path() {
		return Context.append_yaml_path(context.yaml_path(), name, ".");
	}

	@Override
	public void enable() {
		if (config_enabled) {
			super.enable();
		}
	}

	@Override
	public void disable() {
		if (config_enabled) {
			super.disable();
		}
	}
}
