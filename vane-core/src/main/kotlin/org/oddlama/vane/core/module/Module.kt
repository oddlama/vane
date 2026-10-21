package org.oddlama.vane.core.module

import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import java.util.regex.Pattern
import kotlin.math.max
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import org.bstats.bukkit.Metrics
import org.bukkit.NamespacedKey
import org.bukkit.OfflinePlayer
import org.bukkit.attribute.Attribute
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Item
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.event.world.LootGenerateEvent
import org.bukkit.loot.LootTables
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionAttachment
import org.bukkit.permissions.PermissionDefault
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin
import org.oddlama.vane.annotation.VaneModule
import org.oddlama.vane.annotation.config.ConfigBoolean
import org.oddlama.vane.annotation.config.ConfigString
import org.oddlama.vane.annotation.config.ConfigVersion
import org.oddlama.vane.annotation.lang.LangVersion
import org.oddlama.vane.annotation.persistent.Persistent
import org.oddlama.vane.core.Core
import org.oddlama.vane.core.LootTable
import org.oddlama.vane.core.command.Command
import org.oddlama.vane.core.config.ConfigManager
import org.oddlama.vane.core.functional.Consumer1
import org.oddlama.vane.core.lang.LangManager
import org.oddlama.vane.core.persistent.PersistentStorageDirtyState
import org.oddlama.vane.core.persistent.PersistentStorageManager
import org.oddlama.vane.core.resourcepack.ResourcePackGenerator
import org.oddlama.vane.util.ResourceList

/** Matches the per-language files a module ships and regenerates: `lang-<code>.yml`. */
private val LANG_FILE_PATTERN: Pattern = Pattern.compile("lang-.*\\.yml")

/**
 * Base class for all vane Bukkit modules.
 *
 * @param T concrete module type.
 */
abstract class Module<T : Module<T?>?> : JavaPlugin(), Context<T?>, Listener {
    /** Module annotation metadata. */
    val annotation: VaneModule = javaClass.getAnnotation(VaneModule::class.java)!!

    /** Reference to vane-core module. */
    var core: Core? = null

    /** Java util logger for this module. */
    var log: Logger = logger

    /** Adventure component logger for this module. */
    var clog: ComponentLogger = componentLogger

    /** Resource-pack namespace of this module. */
    private val namespace = "vane_" + annotation.name.replace("[^a-zA-Z0-9_]".toRegex(), "_")

    /** Configuration manager. */
    var configManager: ConfigManager = ConfigManager(this)

    /** Localization manager. */
    var langManager: LangManager = LangManager(this)

    /** Persistent storage manager. */
    var persistentStorageManager: PersistentStorageManager = PersistentStorageManager(this)

    /** Whether persistent storage should be saved at next flush interval. */
    private val persistentStorageState = PersistentStorageDirtyState()

    /** Per-module catch-all command permission. */
    var permissionCommandCatchallModule: Permission?

    /** Module-scoped random source. */
    var random: Random = Random()

    /** Console permissions queued until attachment creation. */
    private val pendingConsolePermissions: MutableList<String> = mutableListOf()

    /** Console permission attachment. */
    var consoleAttachment: PermissionAttachment? = null

    /** Config file version. */
    @ConfigVersion var configVersion: Long = 0

    /** Localization file version. */
    @LangVersion var langVersion: Long = 0

    /** Persistent storage version. */
    @Persistent var storageVersion: Long = 0

    /** Active language code for this module. */
    @ConfigString(
        def = "inherit",
        desc =
            "The language for this module. The corresponding language file must be named lang-{lang}.yml. Specifying 'inherit' will load the value set for vane-core.",
        metrics = true,
    )
    /** Configured language code for this module. */
    var configLang: String? = null

    /** Whether bStats metrics are enabled for this module. */
    @ConfigBoolean(
        def = true,
        desc =
            "Enable plugin metrics via bStats. You can opt-out here or via the global bStats configuration. All collected information is completely anonymous and publicly available.",
    )
    /** Whether bStats metrics are enabled. */
    var configMetricsEnabled: Boolean = false

    /** Root context group for this module. */
    private val contextGroup: ModuleGroup<T?> =
        ModuleGroup(
            this,
            "",
            "The module will only add functionality if this is set to true.",
            false,
        )

    /** Compiles a component into the root context. */
    override fun compile(component: ModuleComponent<T?>?) {
        contextGroup.compile(component)
    }

    /** Adds a child context to the root context while preventing self-cycles. */
    override fun addChild(subcontext: Context<T?>?) {
        // Prevent adding the module's root group as a child of itself which would create
        // a self-referential cycle leading to infinite recursion on enable/disable.
        if (subcontext === contextGroup) return
        contextGroup.addChild(subcontext)
    }

    /** Parent context of the module root context. */
    override val context: Context<T?>?
        get() = this

    /** Module instance as generic type. */
    @Suppress("UNCHECKED_CAST")
    override val module: T?
        get() = this as T

    /** Root YAML path of this module context. */
    override fun yamlPath(): String? = ""

    /** Resolves variable YAML paths for the root context. */
    override fun variableYamlPath(variable: String?): String? = variable

    /** Whether this module is logically enabled. */
    override fun enabled(): Boolean = contextGroup.enabled()

    /** Hook called during plugin load phase. */
    protected fun onModuleLoad() {}

    /** Hook called when the module is enabled. */
    override fun onModuleEnable() {}

    /** Hook called when the module is disabled. */
    override fun onModuleDisable() {}

    /** Hook called after config reload. */
    override fun onConfigChange() {}

    /** Hook called during resource-pack generation. */
    @Throws(IOException::class) fun onGenerateResourcePack() {}

    /** Iterates over all compiled module components. */
    override fun forEachModuleComponent(f: Consumer1<ModuleComponent<*>?>?) {
        contextGroup.forEachModuleComponent(f)
    }

    /** Additional loot-table injections keyed by target loot table key. */
    private val additionalLootTables: MutableMap<NamespacedKey?, LootTable?> = mutableMapOf()

    /** bStats metrics handle, if enabled. */
    var metrics: Metrics? = null

    init {
        core =
            if (this.name == "vane-core") this as Core
            else server.pluginManager.getPlugin("vane-core") as Core?

        permissionCommandCatchallModule =
            Permission(
                    "vane.${annotationName}.commands.*",
                    "Allow access to all vane-$annotationName commands",
                    PermissionDefault.FALSE,
                )
                .also { registerPermission(it) }

        contextGroup.compileSelf()
    }

    /** Returns the namespace used in generated resource packs. */
    override fun namespace(): String = namespace

    /** Bukkit plugin load entrypoint. */
    override fun onLoad() {
        if (!dataFolder.exists()) dataFolder.mkdirs()
        onModuleLoad()
    }

    /** Bukkit plugin enable entrypoint. */
    override fun onEnable() {
        consoleAttachment =
            server.consoleSender.addAttachment(this).also { attachment ->
                pendingConsolePermissions.forEach { attachment.setPermission(it, true) }
                pendingConsolePermissions.clear()
            }

        core!!.registerModule(this)
        loadPersistentStorage()
        // shutdown() is deferred; do not enable components with rejected/default storage meanwhile.
        if (!persistentStorageManager.isLoaded) return
        reloadConfiguration()

        scheduleTaskTimer(
            {
                if (persistentStorageState.isDirty) savePersistentStorage()
            },
            (60 * 20).toLong(),
            (60 * 20).toLong(),
        )
    }

    /** Bukkit plugin disable entrypoint. */
    override fun onDisable() {
        try {
            disable()
        } finally {
            // A failing component or world write must not also discard pending module storage.
            savePersistentStorage()
            core!!.unregisterModule(this)
        }
    }

    /** Enables module internals and component tree. */
    override fun enable() {
        if (configMetricsEnabled) {
            val id = annotation.bstats
            if (id != -1) {
                metrics = Metrics(this, id)
                configManager.registerMetrics(metrics)
            }
        }
        onModuleEnable()
        contextGroup.enable()
        registerListener(this)
    }

    /** Disables module internals and component tree. */
    override fun disable() {
        unregisterListener(this)
        contextGroup.disable()
        onModuleDisable()
        metrics = null
    }

    /** Dispatches configuration-change callbacks. */
    override fun configChange() {
        onConfigChange()
        contextGroup.configChange()
    }

    /** Generates module resource-pack contributions. */
    @Throws(IOException::class)
    override fun generateResourcePack(pack: ResourcePackGenerator?) {
        if (pack == null) return

        // Generate language files
        val langFiles =
            dataFolder.listFiles { _, name ->
                name != null && LANG_FILE_PATTERN.matcher(name).matches()
            } ?: emptyArray()
        langFiles.sorted().forEach { langFile ->
            val yaml = YamlConfiguration.loadConfiguration(langFile)
            try {
                langManager.generateResourcePack(pack, yaml, langFile)
            } catch (e: Exception) {
                throw RuntimeException(
                    "Error while generating language for '$langFile' of module $annotationName",
                    e,
                )
            }
        }

        // Add resource pack files listed in the embedded index
        getResource("resource_pack/index")?.let { index ->
            try {
                BufferedReader(InputStreamReader(index)).use { reader ->
                    reader.lineSequence().forEach { filePath ->
                        getResource("resource_pack/$filePath")?.let { content ->
                            pack.addFile(filePath, content)
                        }
                            ?: log.log(
                                Level.WARNING,
                                "Missing resource 'resource_pack/$filePath' in module $annotationName",
                            )
                    }
                }
            } catch (e: IOException) {
                log.log(
                    Level.SEVERE,
                    "Could not load resource pack index file of module $annotationName",
                    e,
                )
            }
        }

        onGenerateResourcePack(pack)
        contextGroup.generateResourcePack(pack)
    }

    /** Tries to reload module configuration from disk. */
    private fun tryReloadConfiguration(): Boolean {
        val file = configManager.standardFile()
        return !(!file.exists() && !configManager.generateFile(file, null)) &&
            configManager.reload(file)
    }

    /** Updates a localized language file from embedded resources when newer. */
    private fun updateLangFile(langFile: String) {
        val file = File(dataFolder, langFile)
        val fileVersion = YamlConfiguration.loadConfiguration(file).getLong("Version", -1)
        var resourceVersion = -1L

        getResource(langFile)?.let { res ->
            try {
                InputStreamReader(res).use { reader ->
                    resourceVersion =
                        YamlConfiguration.loadConfiguration(reader).getLong("Version", -1)
                }
            } catch (e: IOException) {
                log.log(
                    Level.SEVERE,
                    "Error while updating lang file '$file' of module $annotationName",
                    e,
                )
            }
        }

        if (resourceVersion > fileVersion) {
            try {
                getResource(langFile)?.let { res ->
                    Files.copy(res, file.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }
                    ?: log.log(
                        Level.WARNING,
                        "Embedded lang resource '$langFile' missing for module $annotationName",
                    )
            } catch (e: IOException) {
                log.log(
                    Level.SEVERE,
                    "Error while copying lang file '$file' of module $annotationName",
                    e,
                )
            }
        }
    }

    /** Tries to reload localization files for this module. */
    private fun tryReloadLocalization(): Boolean {
        ResourceList.getResources(javaClass, LANG_FILE_PATTERN).forEach { updateLangFile(it) }

        var langCode = configLang
        if (langCode == "inherit") {
            langCode = core!!.configLang ?: "en"
            if (langCode == "inherit") langCode = "en"
        }

        val file = File(dataFolder, "lang-$langCode.yml")
        if (!file.exists()) {
            log.severe("Missing language file '${file.name}' for module $annotationName")
            return false
        }
        return langManager.reload(file)
    }

    /** Reloads module configuration and localization and reapplies enable state. */
    fun reloadConfiguration(): Boolean {
        val wasEnabled = enabled()

        if (!tryReloadConfiguration()) {
            // Force stop server, we encountered an invalid config file
            log.severe("Invalid plugin configuration. Shutting down.")
            server.shutdown()
            return false
        }

        // Reload localization
        if (!tryReloadLocalization()) {
            // Force stop server, we encountered an invalid lang file
            log.severe("Invalid localization file. Shutting down.")
            server.shutdown()
            return false
        }

        when {
            wasEnabled && !enabled() -> disable()
            !wasEnabled && enabled() -> enable()
        }

        configChange()
        return true
    }

    /** Returns the persistent storage file path. */
    val persistentStorageFile: File
        get() = File(dataFolder, "storage.json")

    /** Loads persistent storage from disk. */
    fun loadPersistentStorage() {
        val file: File = this.persistentStorageFile
        if (!persistentStorageManager.load(file)) {
            log.severe("Invalid persistent storage. Shutting down to prevent further corruption.")
            server.shutdown()
        }
    }

    /** Marks persistent storage as dirty for deferred save. */
    override fun markPersistentStorageDirty() {
        persistentStorageState.markDirty()
    }

    /** Saves persistent storage to disk. */
    fun savePersistentStorage() {
        val file: File = this.persistentStorageFile
        persistentStorageState.save { persistentStorageManager.trySave(file) }
    }

    /** Registers a Bukkit listener with this module plugin instance. */
    fun registerListener(listener: Listener) = server.pluginManager.registerEvents(listener, this)

    /** Unregisters a Bukkit listener. */
    fun unregisterListener(listener: Listener) = HandlerList.unregisterAll(listener)

    /** Returns module annotation short name. */
    val annotationName: String
        get() = annotation.name

    /** Command names whose lifecycle handlers have already been installed. */
    private val registeredCommands = mutableSetOf<String>()

    /** Registers a brigadier-backed command during lifecycle command registration. */
    fun registerCommand(command: Command<*>) {
        // Guard: only register the lifecycle handler once per command to prevent
        // handler accumulation during module reload cycles. The brigadier node
        // persists in the dispatcher regardless of Bukkit command map changes.
        if (!registeredCommands.add(command.name)) return
        val manager: LifecycleEventManager<Plugin> = lifecycleManager
        manager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            event
                .registrar()
                .register(command.command, command.langDescription.str(), command.getAliases())
        }
    }

    /** Unregisters a Bukkit command from the command map. */
    fun unregisterCommand(command: Command<*>) {
        val bukkitCommand = command.getBukkitCommand()
        server.commandMap.knownCommands.values.remove(bukkitCommand)
        bukkitCommand.unregister(server.commandMap)
    }

    /** Grants a permission to the console attachment. */
    fun addConsolePermission(permission: Permission) = addConsolePermission(permission.name)

    /** Grants a permission node to console, deferred until attachment exists if needed. */
    fun addConsolePermission(permission: String) {
        consoleAttachment?.setPermission(permission, true)
            ?: pendingConsolePermissions.add(permission)
    }

    /** Registers a permission with Bukkit's plugin manager. */
    fun registerPermission(permission: Permission) {
        try {
            server.pluginManager.addPermission(permission)
        } catch (e: IllegalArgumentException) {
            log.log(Level.SEVERE, "Permission '${permission.name}' was already defined", e)
        }
    }

    /** Unregisters a permission from Bukkit's plugin manager. */
    fun unregisterPermission(permission: Permission) =
        server.pluginManager.removePermission(permission)

    /** Returns loot-table injector for a Bukkit [LootTables] enum value. */
    fun lootTable(table: LootTables): LootTable = lootTable(table.key)

    /** Returns offline players that have a non-null name. */
    val offlinePlayersWithValidName: List<OfflinePlayer>
        get() = server.offlinePlayers.filter { it.name != null }

    /** Returns or creates additional loot table data for a key. */
    fun lootTable(key: NamespacedKey?): LootTable =
        additionalLootTables.getOrPut(key) { LootTable() }!!

    /** Injects additional module loot into generated loot events. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onModuleLootGenerate(event: LootGenerateEvent) {
        val additionalLootTable = additionalLootTables[event.lootTable.key] ?: return
        val loc = event.lootContext.location
        val localRandom =
            Random(
                (random.nextInt() +
                        (loc.blockX and (0xffff shl 16)) +
                        (loc.blockY and (0xffff shl 32)) +
                        (loc.blockZ and (0xffff shl 48)))
                    .toLong()
            )
        additionalLootTable.generateLoot(event.loot, localRandom)
    }

    /** Overrides caught fishing loot with module-injected loot pools. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onModulePlayerCaughtFish(event: PlayerFishEvent) {
        if (event.state != PlayerFishEvent.State.CAUGHT_FISH) return
        val caughtItem = event.caught as? Item ?: return

        val player = event.player
        val playerLuck = player.getAttribute(Attribute.LUCK)!!.value
        val rodLuck =
            player.inventory
                .getItem(event.hand!!)
                .getEnchantmentLevel(Enchantment.LUCK_OF_THE_SEA)
                .toDouble()
        val totalLuck = playerLuck + rodLuck

        val weightFish = max(0.0, 85 + totalLuck * -1)
        val weightJunk = max(0.0, 10 + totalLuck * -2)
        val weightTreasure = if (event.hook.isInOpenWater) max(0.0, 5 + totalLuck * 2) else 0.0

        val roll = random.nextDouble() * (weightFish + weightJunk + weightTreasure)
        val key =
            when {
                roll < weightFish -> LootTables.FISHING_FISH.key
                roll < weightFish + weightJunk -> LootTables.FISHING_JUNK.key
                else -> LootTables.FISHING_TREASURE.key
            }

        val additionalLootTable = additionalLootTables[key] ?: return
        val newItem =
            additionalLootTable.generateOverride(Random(random.nextInt().toLong())) ?: return
        caughtItem.itemStack = newItem
    }
}
