package org.oddlama.vane.proxycore.config

import org.oddlama.vane.proxycore.VaneProxyPlugin
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.logging.Level

/**
 * Loads, validates, and provides access to proxy-core configuration.
 *
 * @property plugin owning plugin context.
 */
class ConfigManager(private val plugin: VaneProxyPlugin) {
    // name → managed server
    /** Managed server definitions keyed by backend name. */
    @JvmField
    val managedServers: MutableMap<String?, ManagedServer?> = HashMap()

    // port → alias id (starts at 1)
    /** Multiplexer definitions keyed by configured multiplexer id. */
    @JvmField
    val multiplexerById: MutableMap<Int?, AuthMultiplex?> = HashMap()

    /** BUSY Bar settings, or `null` when not configured. */
    var busyBar: BusyBarConfig? = null
        private set

    /**
     * Returns the location of `config.toml` in the plugin data folder.
     *
     * @return configuration file path.
     */
    private fun file(): File = File(plugin.dataFolder, "config.toml")

    /**
     * Loads configuration from disk and populates in-memory maps.
     *
     * @return `true` when loading and validation succeeds.
     */
    fun load(): Boolean {
        val file = file()
        if (!file.exists() && !saveDefault(file)) {
            plugin.getLogger().log(Level.SEVERE, "Unable to create default config! Bailing.")
            return false
        }

        val parsedConfig = try {
            Config(file)
        } catch (e: Exception) {
            plugin.getLogger().log(Level.SEVERE, "Error while loading config file '$file'", e)
            return false
        }
        parsedConfig.legacyBackup?.let {
            plugin.getLogger().log(Level.INFO, "Migrated legacy keys in '${file.name}' (backup: '${it.name}')")
        }

        val authMultiplex = parsedConfig.authMultiplex
            ?: run {
                plugin.getLogger().log(Level.SEVERE, "Config did not contain auth multiplexer definitions")
                return false
            }
        val managedServers = parsedConfig.managedServers
            ?: run {
                plugin.getLogger().log(Level.SEVERE, "Config did not contain managed server definitions")
                return false
            }

        if (authMultiplex.containsKey(0)) {
            plugin.getLogger().log(Level.SEVERE, "Attempted to register a multiplexer with id 0!")
            return false
        }

        // Make sure there are no duplicate ports
        val registeredPorts = mutableSetOf<Int?>()
        for (multiplexer in authMultiplex.values) {
            registeredPorts.add(multiplexer?.port)
        }

        if (authMultiplex.size != registeredPorts.size) {
            plugin.getLogger().log(Level.SEVERE, "Attempted to register multiple multiplexers on the same port!")
            return false
        }

        multiplexerById.putAll(authMultiplex)
        this.managedServers.putAll(managedServers)
        busyBar = parsedConfig.busyBar

        return true
    }

    /**
     * Writes the bundled default config to [file].
     *
     * @param file destination file.
     * @return `true` when the file was written successfully.
     */
    fun saveDefault(file: File): Boolean {
        try {
            file.parentFile.mkdirs()
            Files.copy(
                requireNotNull(javaClass.classLoader.getResourceAsStream("config.toml")) {
                    "Missing bundled default config.toml"
                },
                file.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
            return true
        } catch (e: Exception) {
            plugin.getLogger().log(Level.SEVERE, "Error while writing config file '$file'", e)
            return false
        }
    }

    /**
     * Finds the multiplexer mapping entry for a given listening [port].
     *
     * @param port incoming connection port.
     * @return matching entry or `null` when no multiplexer is registered for the port.
     */
    fun getMultiplexerForPort(port: Int?): MutableMap.MutableEntry<Int?, AuthMultiplex?>? {
        for (multiplexer in multiplexerById.entries) {
            // We already checked there are no duplicate ports when parsing
            if (multiplexer.value?.port == port) {
                return multiplexer
            }
        }

        return null
    }
}
