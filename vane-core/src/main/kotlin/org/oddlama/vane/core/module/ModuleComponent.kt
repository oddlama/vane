package org.oddlama.vane.core.module

import java.io.IOException
import java.util.function.Consumer
import org.bukkit.NamespacedKey
import org.bukkit.scheduler.BukkitTask
import org.json.JSONObject
import org.oddlama.vane.core.resourcepack.ResourcePackGenerator

/**
 * Base class for context-bound module components with lifecycle and scheduling helpers.
 *
 * @param T the module type this component belongs to.
 * @param context optional initial context; when null, [setContext] must be called later.
 */
abstract class ModuleComponent<T : Module<T?>?>(context: Context<T?>?) {
    /** The bound context for this component. */
    private var context: Context<T?>? = null

    init {
        if (context != null) {
            setContext(context)
        }
    }

    /**
     * Binds this component to a context and compiles reflected fields.
     *
     * @param context the context to attach.
     */
    fun setContext(context: Context<T?>) {
        if (this.context != null) {
            throw RuntimeException("Cannot replace existing context! This is a bug.")
        }
        this.context = context
        @Suppress("UNCHECKED_CAST") context.compile(this as ModuleComponent<T?>)
    }

    /** Returns the currently bound context. */
    fun getContext(): Context<T?>? = context

    /** Returns the owning module. */
    val module: T?
        get() = context!!.module

    /** Returns whether this component is currently enabled. */
    open fun enabled(): Boolean = context!!.enabled()

    /** Called when this component is enabled. */
    protected abstract fun onEnable()

    /** Called when this component is disabled. */
    protected abstract fun onDisable()

    /** Called after config values were reloaded. */
    protected open fun onConfigChange() {}

    /** Dispatches enable lifecycle handling. */
    internal fun dispatchEnable() = onEnable()

    /** Dispatches disable lifecycle handling. */
    internal fun dispatchDisable() = onDisable()

    /** Dispatches config-change lifecycle handling. */
    internal fun dispatchConfigChange() = onConfigChange()

    /**
     * Allows components to contribute files and translations to the resource pack.
     *
     * @param pack the target resource pack generator.
     * @throws IOException if generation fails.
     */
    @Throws(IOException::class) fun onGenerateResourcePack(pack: ResourcePackGenerator?) {}

    /** Schedules a repeating task through the bound context. */
    fun scheduleTaskTimer(task: Runnable, delayTicks: Long, periodTicks: Long): BukkitTask =
        context!!.scheduleTaskTimer(task, delayTicks, periodTicks)

    /** Schedules a delayed task through the bound context. */
    fun scheduleTask(task: Runnable, delayTicks: Long): BukkitTask =
        context!!.scheduleTask(task, delayTicks)

    /** Schedules a task for the next server tick. */
    fun scheduleNextTick(task: Runnable): BukkitTask = context!!.scheduleNextTick(task)

    /** Registers a storage migration step. */
    fun addStorageMigrationTo(to: Long, name: String?, migrator: Consumer<JSONObject?>) {
        context!!.addStorageMigrationTo(to, name, migrator)
    }

    /** Returns the storage path of a field within this context. */
    fun storagePathOf(field: String): String? = context!!.storagePathOf(field)

    /** Marks persistent storage as dirty. */
    fun markPersistentStorageDirty() {
        context!!.markPersistentStorageDirty()
    }

    /** Creates a namespaced key rooted at this component's context path. */
    fun namespacedKey(value: String?): NamespacedKey =
        NamespacedKey(this.module!!, getContext()!!.variableYamlPath(value)!!)
}
