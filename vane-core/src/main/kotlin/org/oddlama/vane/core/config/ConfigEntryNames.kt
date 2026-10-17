package org.oddlama.vane.core.config

import org.oddlama.vane.core.YamlLoadException

/**
 * Maps the names of entries in a config dictionary (recipes, loot tables) between their internal
 * form and the YAML key they are written under.
 *
 * Entry names become recipe and loot keys, so every name is kept exactly as written. Only the
 * entries in [yamlNames] are renamed: vane's own defaults, written as PascalCase headings
 * (`TerralithGeneric`) and read back from that heading or from their internal name
 * (`terralith_generic`, as configs from before v1.22.0 spell it).
 *
 * @param yamlNames internal name to YAML heading, for each renamed default entry.
 */
class ConfigEntryNames(private val yamlNames: Map<String, String>) {
    private val internalNames: Map<String, String> = yamlNames.entries.associate { (name, yaml) -> yaml to name }

    /** Returns the YAML key that the entry [name] is written under. */
    fun toYaml(name: String): String = yamlNames[name] ?: name

    /** Returns the internal name of the entry written under the YAML key [key]. */
    fun fromYaml(key: String): String = internalNames[key] ?: key

    /**
     * Returns the internal name of each key in [keys], in order.
     *
     * @throws YamlLoadException if two keys name the same entry, such as `Generic` and `generic`.
     * Saving would otherwise silently drop one of them.
     */
    @Throws(YamlLoadException::class)
    fun fromYaml(keys: Iterable<String>, what: String): List<String> {
        val seen = mutableMapOf<String, String>()
        return keys.map { key ->
            val name = fromYaml(key)
            seen.put(name, key)?.let { other ->
                throw YamlLoadException("The $what '$other' and '$key' are the same entry; remove one of them.")
            }
            name
        }
    }
}
