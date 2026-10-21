package org.oddlama.vane.core.module

import java.io.IOException
import org.oddlama.vane.core.functional.Consumer1
import org.oddlama.vane.core.resourcepack.ResourcePackGenerator

/**
 * A ModuleContext is an association to a specific Module and also a grouping of config and language
 * variables with a common namespace.
 *
 * @param T the module type.
 * @param context the parent context.
 * @param name this context name segment.
 * @param description optional config section description.
 * @param separator separator used for nested path composition.
 * @param compileSelf whether to compile this context during initialization.
 */
open class ModuleContext<T : Module<T?>?>
@JvmOverloads
constructor(
    /** Parent context. */
    override var context: Context<T?>,
    /** Context name segment. */
    protected var name: String?,
    /** Optional config section description. */
    private val description: String?,
    /** Path separator for nested YAML scopes. */
    private val separator: String?,
    compileSelf: Boolean = true,
) : Context<T?> {
    /** Cached module reference from [context]. */
    override var module: T? = null
        protected set

    /** Child contexts registered under this context. */
    private val subcontexts: MutableList<Context<T?>> = mutableListOf()

    /** Components compiled and attached directly to this context. */
    private val components: MutableList<ModuleComponent<T?>> = mutableListOf()

    /** Guard flag used to prevent duplicate compilation. */
    private var compiled = false

    init {
        module = context.module
        if (compileSelf) compileSelf()
    }

    /** Returns this context YAML path. */
    override fun yamlPath(): String? = Context.appendYamlPath(context.yamlPath()!!, name, separator)

    /** Returns the YAML path for a variable under this context. */
    override fun variableYamlPath(variable: String?): String? =
        Context.appendYamlPath(yamlPath()!!, variable, separator)

    /** Returns whether this context is enabled. */
    override fun enabled(): Boolean = context.enabled()

    /** Compiles language, config, and persistent-storage fields of a component. */
    private fun compileComponent(component: Any) {
        module!!.langManager.compile(component) { variableYamlPath(it) ?: "" }
        module!!.configManager.compile(component) { variableYamlPath(it) ?: "" }
        if (description != null)
            module!!.configManager.addSectionDescription(yamlPath(), description)
        module!!.persistentStorageManager.compile(component) { variableYamlPath(it) ?: "" }
    }

    /** Compiles this context and registers it as a child of its parent context. */
    internal fun compileSelf() {
        if (compiled) return
        compiled = true

        compileComponent(this)

        try {
            context.addChild(this)
        } catch (_: Exception) {
            // Parent contexts are responsible for handling invalid self-references.
        }
    }

    /** Compiles and registers a component under this context. */
    override fun compile(component: ModuleComponent<T?>?) {
        components.add(component!!)
        compileComponent(component)
    }

    /** Adds a child context. */
    override fun addChild(subcontext: Context<T?>?) {
        subcontexts.add(subcontext!!)
    }

    /** Enables this context and all attached components/children. */
    override fun enable() {
        onModuleEnable()
        components.forEach { it.dispatchEnable() }
        subcontexts.forEach { it.enable() }
    }

    /** Disables child contexts and components in reverse order. */
    override fun disable() {
        subcontexts.reversed().forEach { it.disable() }
        components.reversed().forEach { it.dispatchDisable() }
        onModuleDisable()
    }

    /** Propagates config-change handling to components and child contexts. */
    override fun configChange() {
        onConfigChange()
        components.forEach { it.dispatchConfigChange() }
        subcontexts.forEach { it.configChange() }
    }

    /** Propagates resource-pack generation to components and child contexts. */
    @Throws(IOException::class)
    override fun generateResourcePack(pack: ResourcePackGenerator?) {
        onGenerateResourcePack(pack)
        components.forEach { it.onGenerateResourcePack(pack) }
        subcontexts.forEach { it.generateResourcePack(pack) }
    }

    /** Visits every attached component recursively. */
    override fun forEachModuleComponent(f: Consumer1<ModuleComponent<*>?>?) {
        components.forEach { f!!.apply(it) }
        subcontexts.forEach { it.forEachModuleComponent(f) }
    }

    /** Hook invoked before components and subcontexts are enabled. */
    override fun onModuleEnable() {}

    /** Hook invoked after components and subcontexts are disabled. */
    override fun onModuleDisable() {}

    /** Hook invoked before config-change propagation. */
    override fun onConfigChange() {}

    /** Hook invoked before resource-pack generation propagation. */
    @Throws(IOException::class) override fun onGenerateResourcePack(pack: ResourcePackGenerator?) {}
}
