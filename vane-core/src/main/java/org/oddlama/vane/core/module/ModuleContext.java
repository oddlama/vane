package org.oddlama.vane.core.module;

import static org.oddlama.vane.util.Util.prepend;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.plugin.Plugin;

import org.oddlama.vane.annotation.command.Aliases;
import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.annotation.command.VaneCommand;
import org.oddlama.vane.annotation.lang.LangString;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.core.command.params.AnyParam;

/**
 * A ModuleContext is an association to a specific Module and also a
 * grouping of config and language variables with a common namespace.
 */
public class ModuleContext<T> implements Context<T> {
	protected T module;
	private String namespace;
	private List<ModuleComponent> components = new ArrayList<>();

	public ModuleContext(T module, String namespace) {
		this(module, namespace, true);
	}

	public ModuleContext(T module, String namespace, boolean compile_self) {
		this.module = module;
		this.namespace = namespace;

		if (compile_self) {
			compile_self();
		}
	}

	private void get_namespaced_variable(String variable) {
		return namespace + variable;
	}

	protected void compile_self() {
		// Compile localization and config fields
		module.lang_manager.compile(this, this::get_namespaced_variable);
		module.config_manager.compile(this, this::get_namespaced_variable);
	}

	@Override
	public void compile(ModuleComponent<T> component) {
		components.add(component);
		module.lang_manager.compile(component, this::get_namespaced_variable);
		module.config_manager.compile(component, this::get_namespaced_variable);
	}

	@Override
	public T get_module() {
		return module;
	}

	@Override
	public String get_namespace() {
		return namespace;
	}

	@Override
	public void enable() {
		for (var component : components) {
			component.on_enable();
		}
	}

	@Override
	public void disable() {
		for (var component : components) {
			component.on_disable();
		}
	}

	@Override
	public void config_change() {
		for (var component : components) {
			component.on_config_change();
		}
	}
}
