package org.oddlama.vane.core.module

import org.oddlama.vane.annotation.config.ConfigBoolean

/**
 * A ModuleGroup is a ModuleContext that automatically adds an enabled variable with description to
 * the context. If the group is disabled, onModuleEnable() will not be called.
 *
 * @param context the parent context.
 * @param group the group key.
 * @param configEnabledDesc the config description used for the generated enabled flag.
 * @param compileSelf whether to compile the context immediately.
 */
open class ModuleGroup<T : Module<T?>?>
@JvmOverloads
constructor(
    context: Context<T?>,
    group: String?,
    /** Description text used for the generated enabled config field. */
    private val configEnabledDesc: String?,
    compileSelf: Boolean = true,
) : ModuleContext<T?>(context, group, null, ".", false) {
    /**
     * Controls whether this module group is enabled.
     *
     * The effective annotation description is resolved via [configEnabledDesc].
     */
    @ConfigBoolean(def = true, desc = "") var configEnabled: Boolean = false

    /** Defines the default value for [configEnabled]. */
    @JvmField var configEnabledDef: Boolean = true

    /** Returns the configured default enabled value. */
    fun configEnabledDef(): Boolean = configEnabledDef

    /** Returns the configured description for the enabled config field. */
    fun configEnabledDesc(): String? = configEnabledDesc

    init {
        if (compileSelf) compileSelf()
    }

    /** Returns whether this group is currently enabled. */
    override fun enabled(): Boolean = configEnabled

    /** Enables the group only when [configEnabled] is true. */
    override fun enable() {
        if (configEnabled) super.enable()
    }

    /** Disables the group only when [configEnabled] is true. */
    override fun disable() {
        if (configEnabled) super.disable()
    }

    /** Factory methods for [ModuleGroup]. */
    companion object {
        /**
         * Creates a new module group with immediate compilation enabled.
         *
         * @param context the parent context.
         * @param group the group key.
         * @param description the description for the enabled config field.
         * @return a new [ModuleGroup].
         */
        @JvmStatic
        fun <U : Module<U?>?> create(
            context: Context<U?>,
            group: String?,
            description: String?,
        ): ModuleGroup<U?> = ModuleGroup(context, group, description)
    }
}
