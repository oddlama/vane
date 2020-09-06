package org.oddlama.vane.core.module;

import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;

import org.bukkit.scheduler.BukkitTask;

import org.oddlama.vane.core.ResourcePackGenerator;

public abstract class ModuleComponent<T extends Module<T>> {
	private Context<T> context = null;

	public ModuleComponent(Context<T> context) {
		if (context == null) {
			// Delay until set_context is called.
			return;
		}
		set_context(context);
	}

	public void set_context(Context<T> context) {
		if (this.context != null) {
			throw new RuntimeException("Cannot replace existing context! This is a bug.");
		}
		this.context = context;
		context.compile(this);
	}

	public Context<T> get_context() {
		return context;
	}

	public T get_module() {
		return context.get_module();
	}

	public boolean enabled() {
		return context.enabled();
	}

	protected abstract void on_enable();
	protected abstract void on_disable();
	protected void on_config_change() {}
	protected void on_generate_resource_pack(final ResourcePackGenerator pack) throws IOException {}

	public final BukkitTask schedule_task(Runnable task, long delay_ticks) {
		return context.schedule_task(task, delay_ticks);
	}

	public final BukkitTask schedule_next_tick(Runnable task) {
		return context.schedule_next_tick(task);
	}

	public final void add_storage_migration_to(long to, String name, Consumer<Map<String, Object>> migrator) {
		context.add_storage_migration_to(to, name, migrator);
	}

	public final String storage_path_of(String field) {
		return context.storage_path_of(field);
	}


	public final void save_persistent_storage() {
		context.save_persistent_storage();
	}
}
