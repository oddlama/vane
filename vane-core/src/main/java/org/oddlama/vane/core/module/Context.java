package org.oddlama.vane.core.module;

import static org.oddlama.vane.util.Util.prepend;

import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.plugin.Plugin;

import org.oddlama.vane.annotation.command.Aliases;
import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.annotation.command.VaneCommand;
import org.oddlama.vane.annotation.lang.LangString;
import org.oddlama.vane.core.command.params.AnyParam;

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
	public void enable();
	public void disable();
	public void config_change();

	default public void on_enable() {}
	default public void on_disable() {}
	default public void on_config_change() {}

	default public BukkitTask schedule_task(Runnable task, long delay_ticks) {
		return get_module().getServer().getScheduler().runTaskLater(get_module(), task, delay_ticks);
	}

	default public BukkitTask schedule_next_tick(Runnable task) {
		return get_module().getServer().getScheduler().runTask(get_module(), task);
	}
}
