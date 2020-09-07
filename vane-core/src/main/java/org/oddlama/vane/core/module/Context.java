package org.oddlama.vane.core.module;

import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;
import org.json.JSONObject;

import org.bukkit.scheduler.BukkitTask;

import org.oddlama.vane.core.ResourcePackGenerator;

/**
 * A ModuleContext is an association to a specific Module and also a
 * grouping of config and language variables with a common namespace.
 */
public interface Context<T extends Module<T>> {
	public static String append_yaml_path(String ns1, String ns2, String separator) {
		if (ns1.isEmpty()) {
			return ns2;
		}
		return ns1 + separator + ns2;
	}

	/** create a subcontext namespace */
	default public ModuleContext<T> namespace(String name) {
		return new ModuleContext<T>(this, name);
	}

	/** create a subcontext group */
	default public ModuleGroup<T> group(String group, String description) {
		return new ModuleGroup<T>(this, group, description);
	}

	/**
	 * Compile the given component (processes lang and config definitions)
	 * and registers it for on_enable, on_disable and on_config_change events.
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

	default public void on_enable() {}
	default public void on_disable() {}
	default public void on_config_change() {}
	default public void on_generate_resource_pack(final ResourcePackGenerator pack) throws IOException {}

	default public BukkitTask schedule_task(Runnable task, long delay_ticks) {
		return get_module().getServer().getScheduler().runTaskLater(get_module(), task, delay_ticks);
	}

	default public BukkitTask schedule_next_tick(Runnable task) {
		return get_module().getServer().getScheduler().runTask(get_module(), task);
	}

	default public void add_storage_migration_to(long to, String name, Consumer<JSONObject> migrator) {
		get_module().persistent_storage_manager.add_migration_to(to, name, migrator);
	}

	default public String storage_path_of(String field) {
		if (!field.startsWith("storage_")) {
			throw new RuntimeException("Configuration fields must be prefixed storage_. This is a bug.");
		}
		return variable_yaml_path(field.substring("storage_".length()));
	}

	default public void save_persistent_storage() {
		get_module().save_persistent_storage();
	}
}
