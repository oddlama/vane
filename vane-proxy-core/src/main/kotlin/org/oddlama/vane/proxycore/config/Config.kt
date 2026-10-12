package org.oddlama.vane.proxycore.config

import com.electronwill.nightconfig.core.CommentedConfig
import com.electronwill.nightconfig.core.file.CommentedFileConfig
import java.io.File

/**
 * Parsed proxy-core TOML configuration model.
 *
 * @constructor Loads and validates the configuration from [file].
 */
class Config(file: File) {
    // multiplexerId, { Integer port, List<UUID> AllowedUuids }
    /** Auth multiplexers by configured numeric identifier. */
    @JvmField
    var authMultiplex: LinkedHashMap<Int?, AuthMultiplex?>?

    /** Managed backend server configurations by server id. */
    @JvmField
    var managedServers: LinkedHashMap<String?, ManagedServer?>?

    /** BUSY Bar settings, or `null` when the table is missing. */
    @JvmField
    var busyBar: BusyBarConfig?

    /** Backup of the file taken before legacy keys were migrated, or `null` if none were found. */
    var legacyBackup: File? = null

    init {
        val config = CommentedFileConfig.builder(file)
            .autosave()
            .preserveInsertionOrder()
            .sync()
            .build()

        config.load()

        legacyBackup = LegacyConfigMigration(file).apply { migrate(config) }.backup
        // Autosave only notices top-level changes, so write the renamed nested keys explicitly.
        if (legacyBackup != null) config.save()

        val authMultiplex = linkedMapOf<Int?, AuthMultiplex?>()
        val managedServers = linkedMapOf<String?, ManagedServer?>()

        val registeredPorts = mutableSetOf<Int?>()
        val multiplexersConfig = config.get<CommentedConfig>("AuthMultiplex")
        for (multiplexerConf in multiplexersConfig.entrySet()) {
            val keyString = multiplexerConf.key
            val key = keyString.toIntOrNull()
                ?: throw IllegalArgumentException("Multiplexer ID '$keyString' is not an integer!")

            val value = multiplexerConf.getValue<Any?>()
            require(value is CommentedConfig) { "Multiplexer '$key' has an invalid configuration!" }

            val port = value.getInt("Port")
            require(port !in registeredPorts) { "Multiplexer ID '$keyString' uses an already registered port!" }

            val multiplexer = AuthMultiplex(port, value.get("AllowedUuids"))

            registeredPorts.add(multiplexer.port)
            authMultiplex[key] = multiplexer
        }

        this.authMultiplex = authMultiplex

        val serversConfig = config.get<CommentedConfig>("ManagedServers")
        if (serversConfig != null) {
            for (serverConf in serversConfig.entrySet()) {
                val key = serverConf.key

                val value = serverConf.getValue<Any?>()
                require(value is CommentedConfig) { "Managed server '$key' has an invalid configuration!" }

                val displayNameAny = value.get<Any?>("DisplayName")
                val displayName = displayNameAny as? String ?: key

                val managedServer = ManagedServer(
                    key,
                    displayName,
                    value.get("Online") as? CommentedConfig,
                    value.get("Offline") as? CommentedConfig,
                    value.get("Start") as? CommentedConfig
                )

                managedServers[key] = managedServer
            }
        }

        this.managedServers = managedServers

        busyBar = BusyBarConfig.from(config.get<CommentedConfig>("BusyBar"))
    }
}
