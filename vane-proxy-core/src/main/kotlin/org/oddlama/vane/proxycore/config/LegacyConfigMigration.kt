package org.oddlama.vane.proxycore.config

import com.electronwill.nightconfig.core.CommentedConfig
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Renames `config.toml` keys written before v1.22.0 (`auth_multiplex`, `managed_servers`, ...) to
 * their current names. Multiplexer ids and server names are user-defined and stay unchanged.
 *
 * @param file the config file, backed up before the first change.
 */
class LegacyConfigMigration(private val file: File) {
    /** The backup written before migrating, or `null` if nothing needed migrating. */
    var backup: File? = null
        private set

    /** Migrates [config] in place. */
    fun migrate(config: CommentedConfig) {
        rename(config, "auth_multiplex", "AuthMultiplex")
        rename(config, "managed_servers", "ManagedServers")

        for (multiplexer in subTables(config, "AuthMultiplex")) {
            rename(multiplexer, "port", "Port")
            rename(multiplexer, "allowed_uuids", "AllowedUuids")
        }

        for (server in subTables(config, "ManagedServers")) {
            rename(server, "displayName", "DisplayName")
            rename(server, "online", "Online")
            rename(server, "offline", "Offline")
            rename(server, "start", "Start")

            for (state in listOf("Online", "Offline")) {
                val section = server.get<Any?>(state) as? CommentedConfig ?: continue
                rename(section, "motd", "MOTD")
                rename(section, "favicon", "Favicon")
                rename(section, "quotes", "Quotes")
            }

            val start = server.get<Any?>("Start") as? CommentedConfig ?: continue
            rename(start, "cmd", "CMD")
            rename(start, "timeout", "Timeout")
            rename(start, "kickMsg", "KickMsg")
            rename(start, "allowAnyone", "AllowAnyone")
        }
    }

    /** Returns the tables directly below the table [key], e.g. each managed server. */
    private fun subTables(config: CommentedConfig, key: String): List<CommentedConfig> =
        (config.get<Any?>(key) as? CommentedConfig)
            ?.entrySet()
            ?.mapNotNull { it.getValue<Any?>() as? CommentedConfig }
            .orEmpty()

    /** Moves the value of [old] to [new], unless [new] is already set. */
    private fun rename(config: CommentedConfig, old: String, new: String) {
        if (!config.contains(listOf(old)) || config.contains(listOf(new))) return
        if (backup == null) {
            backup =
                File(file.parentFile, "${file.name}.pre-1.22.bak").also {
                    if (!it.exists())
                        Files.copy(file.toPath(), it.toPath(), StandardCopyOption.COPY_ATTRIBUTES)
                }
        }
        config.set<Any?>(listOf(new), config.remove<Any?>(listOf(old)))
    }
}
