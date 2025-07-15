package org.oddlama.vane.core.module;

import java.io.IOException;
import java.util.function.Consumer;
import org.bukkit.scheduler.BukkitTask;
import org.json.JSONObject;
import org.oddlama.vane.core.functional.Consumer1;
import org.oddlama.vane.core.resourcepack.ResourcePackGenerator;

/**
 * A ModuleContext is an association to a specific Module and also a grouping of config and language
 * variables with a common namespace.
 */
public interface Context<T extends Module<T>> {
    public static String append_yaml_path(String ns1, String ns2, String separator) {
        if (ns1.isEmpty()) {
            return ns2;
        }
        return ns1 + separator + ns2;
    }

    /** create a sub-context namespace */
    public default ModuleContext<T> namespace(String name) {
        return new ModuleContext<T>(this, name, null, ".");
    }

    /** create a sub-context namespace */
    public default ModuleContext<T> namespace(String name, String description) {
        return new ModuleContext<T>(this, name, description, ".");
    }

    /** create a sub-context namespace */
    public default ModuleContext<T> namespace(String name, String description, String separator) {
        return new ModuleContext<T>(this, name, description, separator);
    }

    /** create a sub-context group */
    public default ModuleGroup<T> group(String group, String description) {
        return new ModuleGroup<T>(this, group, description);
    }

    public default ModuleGroup<T> group(String group, String description, boolean default_enabled) {
        final var g = new ModuleGroup<T>(this, group, description);
        g.config_enabled_def = default_enabled;
        return g;
    }

    /** create a sub-context group */
    public default ModuleGroup<T> group_default_disabled(String group, String description) {
        final var g = group(group, description);
        g.config_enabled_def = false;
        return g;
    }

    /**
     * Compile the given component (processes lang and config definitions) and registers it for
     * on_enable, on_disable and on_config_change events.
     */
    public void compile(ModuleComponent<T> component);

    public void add_child(Context<T> subcontext);

    public Context<T> get_context();

    public T get_module();

    public String yaml_path();

    public String variable_yaml_path(String variable);

    public boolean enabled();

    public void enable();

    public void disable();

    public void config_change();

    public void generate_resource_pack(final ResourcePackGenerator pack) throws IOException;

    public void for_each_module_component(final Consumer1<ModuleComponent<?>> f);

    public default void on_enable() {}

    public default void on_disable() {}

    public default void on_config_change() {}

    public default void on_generate_resource_pack(final ResourcePackGenerator pack) throws IOException {}

    public default BukkitTask schedule_task_timer(Runnable task, long delay_ticks, long period_ticks) {
        return get_module().getServer().getScheduler().runTaskTimer(get_module(), task, delay_ticks, period_ticks);
    }

    public default BukkitTask schedule_task(Runnable task, long delay_ticks) {
        return get_module().getServer().getScheduler().runTaskLater(get_module(), task, delay_ticks);
    }

    public default BukkitTask schedule_next_tick(Runnable task) {
        return get_module().getServer().getScheduler().runTask(get_module(), task);
    }

    public default void add_storage_migration_to(long to, String name, Consumer<JSONObject> migrator) {
        get_module().persistent_storage_manager.add_migration_to(to, name, migrator);
    }

    public default String storage_path_of(String field) {
        if (!field.startsWith("storage_")) {
            throw new RuntimeException("Configuration fields must be prefixed storage_. This is a bug.");
        }
        return variable_yaml_path(field.substring("storage_".length()));
    }

    public default void mark_persistent_storage_dirty() {
        get_module().mark_persistent_storage_dirty();
    }
}
