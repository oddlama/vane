package org.oddlama.vane.core.module;

import static org.oddlama.vane.util.Util.prepend;
import static org.oddlama.vane.util.Util.change_annotation_value;

import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.plugin.Plugin;

import org.oddlama.vane.annotation.command.Aliases;
import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.annotation.command.VaneCommand;
import org.oddlama.vane.annotation.config.ConfigBoolean;
import org.oddlama.vane.annotation.lang.LangString;
import org.oddlama.vane.core.command.params.AnyParam;

/**
 * A ModuleGroup is a ModuleContext that automatically adds an enable variable
 * with description to the context. If the group is disabled, on_enable() will
 * not be called.
 */
public class ModuleGroup<T> extends ModuleContext<T> {
	@ConfigBoolean(def = true, desc = "<will be overwritten at runtime>")
	private boolean config_enable;

	public ModuleGroup(T module, String namespace, String description) {
		this(module, namespace, true);
	}

	public ModuleGroup(T module, String namespace, String description, boolean compile_self) {
		super(module, namespace, false);

		// Set description of config_enable
		Field field = ModuleGroup.class.getField("config_enable");
		final ConfigBoolean annotation = field.getAnnotation(ConfigBoolean.class);
		change_annotation_value(annotation, "value", description);

		if (compile_self) {
			compile_self();
		}
	}

	public boolean enabled() {
		return config_enabled;
	}

	@Override
	public void enable() {
		if (config_enable) {
			super.enable();
		}
	}

	@Override
	public void disable() {
		if (config_enable) {
			super.disable();
		}
	}
}
