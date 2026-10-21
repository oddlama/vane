package org.oddlama.vane.core.config

import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import kotlin.math.max
import kotlin.math.min
import org.bstats.bukkit.Metrics
import org.bstats.charts.SimplePie
import org.bukkit.configuration.file.YamlConfiguration
import org.oddlama.vane.core.YamlLoadException
import org.oddlama.vane.core.functional.Consumer2
import org.oddlama.vane.util.TextUtil

/**
 * Base class for reflected configuration fields.
 *
 * @param T the Kotlin type represented by this config field.
 * @param owner object containing the reflected field.
 * @param field reflected backing field.
 * @param mapName maps Java field names to YAML paths.
 * @param typeName human-readable type name shown in comments.
 * @param description static description text.
 */
@Suppress("UNCHECKED_CAST")
abstract class ConfigField<T>(
    /** Owner object containing the reflected field. */
    protected var owner: Any?,
    /** Reflected backing field. */
    protected var field: Field,
    mapName: (String?) -> String?,
    typeName: String?,
    description: String?,
) : Comparable<ConfigField<*>> {
    /** Full YAML path for this field. */
    protected var path: String = mapName(field.name.substring("config".length))!!

    /** Human-readable type name used in generated comments. */
    protected var typeName: String?

    /** Sorting priority used when generating YAML output. */
    protected var sortPriority: Int = 0

    /** Split YAML path components. */
    private val yamlPathComponents: Array<String?> =
        path.split('.').dropLastWhile { it.isEmpty() }.toTypedArray()

    /** Group path part (without leaf basename). */
    private val yamlGroupPath: String?

    /** Leaf field name at the end of the YAML path. */
    private val basename: String?

    /** Lazily resolved description, allowing dynamic overrides. */
    private val description: () -> String?

    init {
        val lastDot = path.lastIndexOf(".")
        this.yamlGroupPath = if (lastDot == -1) "" else path.substring(0, lastDot)
        this.basename = yamlPathComponents[yamlPathComponents.size - 1]
        this.typeName = typeName

        when (this.path) {
            "Lang" -> this.sortPriority = -10
            "Enabled" -> this.sortPriority = -9
            "MetricsEnabled" -> this.sortPriority = -8
        }

        field.isAccessible = true

        this.description = {
            try {
                @Suppress("UNCHECKED_CAST")
                owner!!.javaClass.getMethod("${field.name}Desc").invoke(owner) as String?
            } catch (_: NoSuchMethodException) {
                description
            } catch (e: InvocationTargetException) {
                throw RuntimeException(
                    "Could not call ${owner!!.javaClass.name}.${field.name}Desc() to override description value",
                    e,
                )
            } catch (e: IllegalAccessException) {
                throw RuntimeException(
                    "Could not call ${owner!!.javaClass.name}.${field.name}Desc() to override description value",
                    e,
                )
            }
        }
    }

    /** Returns an overridden default value from `${fieldName}Def()` when present. */
    @Suppress("UNCHECKED_CAST")
    protected fun overriddenDef(): T? =
        try {
            owner!!.javaClass.getMethod("${field.name}Def").invoke(owner) as T?
        } catch (_: NoSuchMethodException) {
            null
        } catch (e: InvocationTargetException) {
            throw RuntimeException(
                "Could not call ${owner!!.javaClass.name}.${field.name}Def() to override default value",
                e,
            )
        } catch (e: IllegalAccessException) {
            throw RuntimeException(
                "Could not call ${owner!!.javaClass.name}.${field.name}Def() to override default value",
                e,
            )
        }

    /** Returns an overridden metrics opt-in from `${fieldName}Metrics()` when present. */
    @Suppress("UNCHECKED_CAST")
    protected fun overriddenMetrics(): Boolean? =
        try {
            owner!!.javaClass.getMethod("${field.name}Metrics").invoke(owner) as Boolean?
        } catch (_: NoSuchMethodException) {
            null
        } catch (e: InvocationTargetException) {
            throw RuntimeException(
                "Could not call ${owner!!.javaClass.name}.${field.name}Metrics() to override metrics status",
                e,
            )
        } catch (e: IllegalAccessException) {
            throw RuntimeException(
                "Could not call ${owner!!.javaClass.name}.${field.name}Metrics() to override metrics status",
                e,
            )
        }

    /** Escapes a string for safe YAML double-quoted output. */
    protected fun escapeYaml(s: String?): String =
        (s ?: "").replace("\\", "\\\\").replace("\"", "\\\"")

    /** Returns the YAML path. */
    fun getYamlGroupPath(): String = path

    /** Returns the YAML path. */
    fun yamlPath(): String = path

    /** Returns the YAML group path without basename. */
    fun yamlGroupPath(): String? = yamlGroupPath

    /** Returns the YAML basename component. */
    fun basename(): String? = basename

    /** Normalizes special suffixes for stable path ordering. */
    private fun modifyYamlPathForSorting(path: String): String =
        if (path.endsWith(".enabled")) path.substring(0, path.lastIndexOf(".enabled")) else path

    /** Compares fields by explicit priority and hierarchical path ordering. */
    override fun compareTo(other: ConfigField<*>): Int {
        if (sortPriority != other.sortPriority) return sortPriority - other.sortPriority
        for (i in 0..<min(yamlPathComponents.size, other.yamlPathComponents.size) - 1) {
            val c = yamlPathComponents[i]!!.compareTo(other.yamlPathComponents[i]!!)
            if (c != 0) return c
        }
        return modifyYamlPathForSorting(yamlPath())
            .compareTo(modifyYamlPathForSorting(other.yamlPath()))
    }

    /** Appends a wrapped description comment line. */
    protected fun appendDescription(builder: StringBuilder, indent: String) {
        // A `<field>Desc()` override may return null; `WordUtils.wrap` passed null straight
        // through, so the interpolation below keeps emitting the same "# null" line it always has.
        val wrapped =
            description()?.let { TextUtil.wordWrap(it, max(60, 80 - indent.length), "\n$indent# ") }
        builder.append("$indent# $wrapped\n")
    }

    /** Appends a YAML list value definition. */
    protected fun <U> appendListDefinition(
        builder: StringBuilder?,
        indent: String?,
        prefix: String?,
        list: MutableCollection<out U?>,
        append: Consumer2<StringBuilder?, U?>,
    ) {
        if (builder == null) return
        list.forEach { i ->
            builder.append(indent)
            builder.append(prefix)
            builder.append("  - ")
            append.apply(builder, i)
            builder.append("\n")
        }
    }

    /** Appends a valid value-range comment. */
    protected fun <U> appendValueRange(
        builder: StringBuilder,
        indent: String?,
        min: U?,
        max: U?,
        invalidMin: U?,
        invalidMax: U?,
    ) {
        builder.append("$indent# Valid values: ")
        builder.append(
            when {
                min != invalidMin && max != invalidMax -> "[$min,$max]"
                min != invalidMin -> "[$min,)"
                max != invalidMax -> "(,$max]"
                else -> "Any $typeName"
            }
        )
        builder.append("\n")
    }

    /** Appends a default-value comment. */
    protected fun appendDefaultValue(builder: StringBuilder, indent: String?, def: Any?) {
        builder.append("$indent# Default: $def\n")
    }

    /** Appends the YAML field definition. */
    protected fun appendFieldDefinition(builder: StringBuilder, indent: String?, def: Any?) {
        builder.append("$indent$basename: $def\n")
    }

    /** Verifies that the YAML path exists. */
    @Throws(YamlLoadException::class)
    protected fun checkYamlPath(yaml: YamlConfiguration) {
        if (!yaml.contains(path, true))
            throw YamlLoadException("yaml is missing entry with path '$path'")
    }

    /** Validates a floating-point value range. */
    protected fun validateDoubleRange(yamlPath: String, value: Double, min: Double, max: Double) {
        if (!min.isNaN() && value < min)
            throw YamlLoadException(
                "Configuration '$yamlPath' has an invalid value: Value must be >= $min"
            )
        if (!max.isNaN() && value > max)
            throw YamlLoadException(
                "Configuration '$yamlPath' has an invalid value: Value must be <= $max"
            )
    }

    /** Validates an integer value range. */
    protected fun validateIntRange(yamlPath: String, value: Int, min: Int, max: Int) {
        if (min != Int.MIN_VALUE && value < min)
            throw YamlLoadException(
                "Configuration '$yamlPath' has an invalid value: Value must be >= $min"
            )
        if (max != Int.MAX_VALUE && value > max)
            throw YamlLoadException(
                "Configuration '$yamlPath' has an invalid value: Value must be <= $max"
            )
    }

    /** Parses and validates namespaced key strings. */
    protected fun requireNamespacedKeyParts(
        yamlPath: String,
        str: String?,
        kind: String,
    ): Pair<String, String> {
        if (str == null)
            throw YamlLoadException("Invalid type for yaml path '$yamlPath', expected string")
        val parts = str.split(":").filter { it.isNotEmpty() }
        if (parts.size != 2)
            throw YamlLoadException(
                "Invalid $kind entry in list '$yamlPath': '$str' is not a valid namespaced key"
            )
        return Pair(parts[0], parts[1])
    }

    /** Returns the default value for this field. */
    abstract fun def(): T?

    /** Returns whether this field should emit a bStats metric. */
    open fun metrics(): Boolean = false

    /** Writes YAML for this field. */
    abstract fun generateYaml(
        builder: StringBuilder,
        indent: String,
        existingCompatibleConfig: YamlConfiguration?,
    )

    /** Validates that this field is loadable from YAML. */
    @Throws(YamlLoadException::class) abstract fun checkLoadable(yaml: YamlConfiguration)

    /** Loads this field from YAML into the owning object. */
    abstract fun load(yaml: YamlConfiguration)

    /** Returns the current reflected value. */
    fun get(): T? =
        try {
            field.get(owner) as T?
        } catch (e: IllegalAccessException) {
            throw RuntimeException("Invalid field access on '${field.name}'. This is a bug.", e)
        }

    /** Registers this field as a simple pie metric when enabled. */
    open fun registerMetrics(metrics: Metrics?) {
        if (metrics == null || !metrics()) return
        metrics.addCustomChart(SimplePie(yamlPath()) { get().toString() })
    }

    /** Returns YAML path components. */
    fun components(): Array<String?> = yamlPathComponents

    /** Returns the group depth (path components minus basename). */
    fun groupCount(): Int = yamlPathComponents.size - 1

    /** Grouping and ordering helpers. */
    companion object {
        /** Returns whether two fields belong to the same YAML group. */
        fun sameGroup(a: ConfigField<*>?, b: ConfigField<*>?): Boolean {
            if (a == null || b == null) return false
            if (a.yamlPathComponents.size != b.yamlPathComponents.size) return false
            for (i in 0..<a.yamlPathComponents.size - 1) {
                if (a.yamlPathComponents[i] != b.yamlPathComponents[i]) return false
            }
            return true
        }

        /** Returns the number of common group components between two fields. */
        fun commonGroupCount(a: ConfigField<*>?, b: ConfigField<*>?): Int {
            if (a == null || b == null) return 0
            var i = 0
            while (i < min(a.yamlPathComponents.size, b.yamlPathComponents.size) - 1) {
                if (a.yamlPathComponents[i] != b.yamlPathComponents[i]) return i
                ++i
            }
            return i
        }
    }
}
