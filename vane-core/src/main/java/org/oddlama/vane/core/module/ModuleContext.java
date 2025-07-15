package org.oddlama.vane.core.module;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.oddlama.vane.core.functional.Consumer1;
import org.oddlama.vane.core.resourcepack.ResourcePackGenerator;

/**
 * A ModuleContext is an association to a specific Module and also a grouping of config and language
 * variables with a common namespace.
 */
public class ModuleContext<T extends Module<T>> implements Context<T> {

    protected Context<T> context;
    protected T module; // cache to not generate chains of get_context()
    protected String name;
    private List<Context<T>> subcontexts = new ArrayList<>();
    private List<ModuleComponent<T>> components = new ArrayList<>();
    private String description;
    private String separator;

    public ModuleContext(Context<T> context, String name, String description, String separator) {
        this(context, name, description, separator, true);
    }

    public ModuleContext(Context<T> context, String name, String description, String separator, boolean compile_self) {
        this.context = context;
        this.module = context.get_module();
        this.name = name;
        this.description = description;
        this.separator = separator;

        if (compile_self) {
            compile_self();
        }
    }

    @Override
    public String yaml_path() {
        return Context.append_yaml_path(context.yaml_path(), name, separator);
    }

    public String variable_yaml_path(String variable) {
        return Context.append_yaml_path(yaml_path(), variable, separator);
    }

    @Override
    public boolean enabled() {
        return context.enabled();
    }

    private void compile_component(Object component) {
        module.lang_manager.compile(component, this::variable_yaml_path);
        module.config_manager.compile(component, this::variable_yaml_path);
        if (description != null) {
            module.config_manager.add_section_description(yaml_path(), description);
        }
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

    @Override
    public void for_each_module_component(final Consumer1<ModuleComponent<?>> f) {
        for (var component : components) {
            f.apply(component);
        }
        for (var subcontext : subcontexts) {
            subcontext.for_each_module_component(f);
        }
    }
}
