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

/**
 * A ModuleContext is an association to a specific Module and also a
 * grouping of config and language variables with a common namespace.
 */
public interface Context<T extends Module<?>> {
	/** create a subcontext namespace */
	default public ModuleContext<T> namespace(String namespace) {
		return new ModuleContext<T>(get_module(), this.get_namespace() + "_" + namespace);
	}

	/** create a subcontext group */
	default public ModuleGroup<T> group(String namespace, String description) {
		return new ModuleGroup<T>(get_module(), this.get_namespace() + "_" + namespace, description);
	}

	/**
	 * Compile the given component (processes lang and config definitions)
	 * and registers it for on_enable, on_disable and on_config_change events.
	 */
	public void compile(ModuleComponent<T> component);

	public T get_module();
	public String get_namespace();
	public void enable();
	public void disable();
	public void config_change();
}
