package org.oddlama.vane.core.module

import java.io.IOException
import java.util.function.Consumer
import org.bukkit.scheduler.BukkitTask
import org.json.JSONObject
import org.oddlama.vane.core.functional.Consumer1
import org.oddlama.vane.core.resourcepack.ResourcePackGenerator

/**
 * A ModuleContext is an association to a specific Module and also a grouping of config and language
 * variables with a common namespace.
 */
interface Context<T : Module<T?>?> {
    /** Creates a sub-context namespace. */
    @Suppress("UNCHECKED_CAST")
    fun namespace(name: String?): ModuleContext<T?> {
        val ctx: Context<T?> = this as Context<T?>
        return ModuleContext(ctx, name, null, ".")
    }

    /** Creates a described sub-context namespace. */
    @Suppress("UNCHECKED_CAST")
    fun namespace(name: String?, description: String?): ModuleContext<T?> {
        val ctx: Context<T?> = this as Context<T?>
        return ModuleContext(ctx, name, description, ".")
    }

    /** Creates a described sub-context namespace with a custom separator. */
    @Suppress("UNCHECKED_CAST")
    fun namespace(name: String?, description: String?, separator: String?): ModuleContext<T?> {
        val ctx: Context<T?> = this as Context<T?>
        return ModuleContext(ctx, name, description, separator)
    }

    /** Creates a sub-context group with a configurable enabled flag. */
    @Suppress("UNCHECKED_CAST")
    fun group(group: String?, description: String?): ModuleGroup<T?> {
        val ctx: Context<T?> = this as Context<T?>
        return ModuleGroup.create(ctx, group, description)
    }

    /** Creates a sub-context group and sets its default enabled state. */
    @Suppress("UNCHECKED_CAST")
    fun group(group: String?, description: String?, defaultEnabled: Boolean): ModuleGroup<T?> {
        val ctx: Context<T?> = this as Context<T?>
        val g = ModuleGroup.create(ctx, group, description)
        g.configEnabledDef = defaultEnabled
        return g
    }

    /** Creates a sub-context group that is disabled by default. */
    fun groupDefaultDisabled(group: String?, description: String?): ModuleGroup<T?> {
        val g = group(group, description)
        g.configEnabledDef = false
        return g
    }

    /**
     * Compile the given component (processes lang and config definitions) and registers it for
     * onModuleEnable, onModuleDisable and onConfigChange events.
     */
    fun compile(component: ModuleComponent<T?>?)

    /** Adds a child context. */
    fun addChild(subcontext: Context<T?>?)

    /** Returns the parent context. */
    val context: Context<T?>?

    /** Returns the owning module. */
    val module: T?

    /** Returns this context YAML path. */
    fun yamlPath(): String?

    /** Returns the YAML path for a variable under this context. */
    fun variableYamlPath(variable: String?): String?

    /** Returns whether this context is enabled. */
    fun enabled(): Boolean

    /** Enables this context and descendants. */
    fun enable()

    /** Disables this context and descendants. */
    fun disable()

    /** Dispatches a configuration reload event. */
    fun configChange()

    /** Generates resource pack contributions for this context tree. */
    @Throws(IOException::class) fun generateResourcePack(pack: ResourcePackGenerator?)

    /** Visits every registered module component. */
    fun forEachModuleComponent(f: Consumer1<ModuleComponent<*>?>?)

    /** Hook called before child/component enable dispatch. */
    fun onModuleEnable() {}

    /** Hook called after child/component disable dispatch. */
    fun onModuleDisable() {}

    /** Hook called before child/component config-change dispatch. */
    fun onConfigChange() {}

    /** Hook called before child/component resource-pack generation dispatch. */
    @Throws(IOException::class) fun onGenerateResourcePack(pack: ResourcePackGenerator?) {}

    /** Schedules a repeating task in the owning module scheduler. */
    fun scheduleTaskTimer(task: Runnable, delayTicks: Long, periodTicks: Long): BukkitTask =
        this.module!!.server.scheduler.runTaskTimer(this.module!!, task, delayTicks, periodTicks)

    /** Schedules a delayed task in the owning module scheduler. */
    fun scheduleTask(task: Runnable, delayTicks: Long): BukkitTask =
        this.module!!.server.scheduler.runTaskLater(this.module!!, task, delayTicks)

    /** Schedules a task for the next server tick. */
    fun scheduleNextTick(task: Runnable): BukkitTask =
        this.module!!.server.scheduler.runTask(this.module!!, task)

    /** Registers a persistent-storage migration step. */
    fun addStorageMigrationTo(to: Long, name: String?, migrator: Consumer<JSONObject?>) {
        this.module!!.persistentStorageManager.addMigrationTo(to, name, migrator)
    }

    /** Resolves the persistent-storage path for a storage field. */
    fun storagePathOf(field: String): String? {
        require(field.startsWith("storage")) {
            "Configuration fields must be prefixed storage. This is a bug."
        }
        return variableYamlPath(field.substring("storage".length))
    }

    /** Marks persistent storage as dirty. */
    fun markPersistentStorageDirty() {
        this.module!!.markPersistentStorageDirty()
    }

    /** Utility helpers for context path handling. */
    companion object {
        /** Appends a path segment to a namespace using the provided separator. */
        @JvmStatic
        fun appendYamlPath(ns1: String, ns2: String?, separator: String?): String? =
            if (ns1.isEmpty()) ns2 else ns1 + separator + ns2
    }
}
