package org.oddlama.vane.core.module;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.oddlama.vane.core.ResourcePackGenerator;
import org.oddlama.vane.core.module.Module;

/**
 * A ModuleContext is an association to a specific Module and also a
 * grouping of config and language variables with a common namespace.
 */
public class ModuleContext<T extends Module<T>> implements Context<T> {
	protected Context<T> context;
	protected T module; // cache to not generate chains of get_context()
	protected String name;
	private List<Context<T>> subcontexts = new ArrayList<>();
	private List<ModuleComponent<T>> components = new ArrayList<>();

	public ModuleContext(Context<T> context, String name) {
		this(context, name, true);
	}

	public ModuleContext(Context<T> context, String name, boolean compile_self) {
		this.context = context;
		this.module = context.get_module();
		this.name = name;

		if (compile_self) {
			compile_self();
		}
	}

	@Override
	public String yaml_path() {
		return Context.append_yaml_path(context.yaml_path(), name, "_");
	}

	public String variable_yaml_path(String variable) {
		return Context.append_yaml_path(yaml_path(), variable, "_");
	}

	private void compile_component(Object component) {
		module.lang_manager.compile(component, this::variable_yaml_path);
		module.config_manager.compile(component, this::variable_yaml_path);
		module.persistent_storage_manager.compile(component, this::variable_yaml_path);
	}

	protected void compile_self() {
		// Compile localization and config fields
		compile_component(this);
		context.add_child(this);
	}

	@Override
	public void compile(ModuleComponent<T> component) {
		components.add(component);
		compile_component(component);
	}

	@Override
	public void add_child(Context<T> subcontext) {
		subcontexts.add(subcontext);
	}

	@Override
	public Context<T> get_context() {
		return context;
	}

	@Override
	public T get_module() {
		return module;
	}

	@Override
	public void enable() {
		on_enable();
		for (var component : components) {
			component.on_enable();
		}
		for (var subcontext : subcontexts) {
			subcontext.enable();
		}
	}

	@Override
	public void disable() {
		for (int i = subcontexts.size() - 1; i >= 0; --i) {
			subcontexts.get(i).disable();
		}
		for (int i = components.size() - 1; i >= 0; --i) {
			components.get(i).on_disable();
		}
		on_disable();
	}

	@Override
	public void config_change() {
		on_config_change();
		for (var component : components) {
			component.on_config_change();
		}
		for (var subcontext : subcontexts) {
			subcontext.config_change();
		}
	}

	@Override
	public void generate_resource_pack(final ResourcePackGenerator pack) throws IOException {
		on_generate_resource_pack(pack);
		for (var component : components) {
			component.on_generate_resource_pack(pack);
		}
		for (var subcontext : subcontexts) {
			subcontext.generate_resource_pack(pack);
		}
	}
}
