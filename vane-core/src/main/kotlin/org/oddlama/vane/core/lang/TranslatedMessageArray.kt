package org.oddlama.vane.core.lang

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.oddlama.vane.core.module.Module

/**
 * Represents an ordered list of translatable messages.
 *
 * @param module the owning module.
 * @param key the base translation key.
 * @param defaultTranslation fallback translation list.
 */
class TranslatedMessageArray(
    /** Owning module. */
    private val module: Module<*>,
    /** Base translation key. */
    private val key: String?,
    /** Fallback translation entries. */
    private val defaultTranslation: MutableList<String>,
) {
    /** Returns the number of message entries. */
    fun size(): Int = defaultTranslation.size

    /** Returns the base translation key. */
    fun key(): String? = key

    /** Formats all entries as plain strings using fallback translations. */
    fun str(vararg args: Any?): MutableList<String?> =
        try {
            val argsAsStrings = stringifyArgsForStr(key, args)
            defaultTranslation.mapTo(mutableListOf()) { s -> String.format(s, *argsAsStrings) }
        } catch (e: Exception) {
            throw RuntimeException("Error while formatting message '${key()}'", e)
        }

    /** Formats all entries as components, preferring client-side translations when enabled. */
    fun format(vararg args: Any?): MutableList<Component?> {
        if (module.core?.configClientSideTranslations != true) {
            return str(*args).mapTo(mutableListOf()) { s ->
                LegacyComponentSerializer.legacySection().deserialize(s!!)
            }
        }

        return defaultTranslation.indices.mapTo(mutableListOf()) { i ->
            val list = args.map { o ->
                when (o) {
                    is ComponentLike -> o
                    is String -> LegacyComponentSerializer.legacySection().deserialize(o)
                    else ->
                        throw RuntimeException(
                            "Error while formatting message '${key()}', got invalid argument $o"
                        )
                }
            }
            Component.translatable("$key.$i", list)
        }
    }
}
