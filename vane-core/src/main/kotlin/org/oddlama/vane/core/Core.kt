package org.oddlama.vane.core

import java.io.File
import java.io.IOException
import java.net.URISyntaxException
import java.util.*
import java.util.logging.Level
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.core.Holder
import net.minecraft.core.MappedRegistry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionDefault
import org.json.JSONException
import org.oddlama.vane.annotation.VaneModule
import org.oddlama.vane.annotation.config.ConfigBoolean
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.core.commands.CustomItem
import org.oddlama.vane.core.commands.Enchant
import org.oddlama.vane.core.commands.Vane
import org.oddlama.vane.core.enchantments.EnchantmentManager
import org.oddlama.vane.core.functional.Consumer1
import org.oddlama.vane.core.item.*
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.menu.MenuManager
import org.oddlama.vane.core.misc.AuthMultiplexer
import org.oddlama.vane.core.misc.CommandHider
import org.oddlama.vane.core.misc.HeadLibrary
import org.oddlama.vane.core.misc.LootChestProtector
import org.oddlama.vane.core.module.Module
import org.oddlama.vane.core.module.ModuleComponent
import org.oddlama.vane.core.resourcepack.ResourcePackDistributor
import org.oddlama.vane.core.resourcepack.ResourcePackGenerator
import org.oddlama.vane.util.IOUtil.readJsonFromUrl
import org.oddlama.vane.util.VersionUtil
import org.oddlama.vane.util.msToTicks

/** Core vane module that initializes shared managers, registries, and base commands. */
@VaneModule(name = "core", bstats = 8637, configVersion = 6, langVersion = 5, storageVersion = 1)
class Core : Module<Core?>() {
    /** Enchantment manager. */
    var enchantmentManager: EnchantmentManager?

    /** Registry of reserved custom model data ranges. */
    private val modelDataRegistry: CustomModelDataRegistry?

    /** Registry of custom items. */
    private val itemRegistry: CustomItemRegistry?

    /** Whether player heads should render full skins in menus. */
    @ConfigBoolean(
        def = true,
        desc =
            "Allow loading of player heads in relevant menus. Disabling this will show all player heads using the Steve skin, which may perform better on low-performance servers and clients.",
    )
    /** Whether player heads should load profile textures in menus. */
    var configPlayerHeadsInMenus: Boolean = false

    /** Message used when a command requires a player sender. */
    @LangMessage var langCommandNotAPlayer: TranslatedMessage? = null

    /** Message used when command permission checks fail. */
    @LangMessage var langCommandPermissionDenied: TranslatedMessage? = null

    /** Message used for invalid time format parsing. */
    @LangMessage var langInvalidTimeFormat: TranslatedMessage? = null

    /** Loaded vane modules ordered by annotation name. */
    private val vaneModules: SortedSet<Module<*>> = TreeSet(compareBy { it.annotationName })

    /** Resource-pack distribution manager. */
    val resourcePackDistributor: ResourcePackDistributor

    /** Registers a module with the core module set. */
    fun registerModule(module: Module<*>) {
        vaneModules.add(module)
    }

    /** Unregisters a module from the core module set. */
    fun unregisterModule(module: Module<*>) {
        vaneModules.remove(module)
    }

    /** Immutable view of loaded modules. */
    val modules: SortedSet<Module<*>?>
        get() = Collections.unmodifiableSortedSet(vaneModules)

    /** Global vane command catch-all permission. */
    var permissionCommandCatchall: Permission =
        Permission(
            "vane.*.commands.*",
            "Allow access to all vane commands (ONLY FOR ADMINS!)",
            PermissionDefault.FALSE,
        )

    /** Menu manager shared by menu systems. */
    @JvmField var menuManager: MenuManager?

    /** Whether client-side translation via resource pack is enabled. */
    @ConfigBoolean(
        def = true,
        desc =
            "Let the client translate messages using the generated resource pack. This allows every player to select their preferred language, and all plugin messages will also be translated. Disabling this won't allow you to skip generating the resource pack, as it will be needed for custom item textures.",
    )
    /** Whether client-side translation keys should be used in outgoing messages. */
    var configClientSideTranslations: Boolean = false

    /** Whether update notices are sent to operators. */
    @ConfigBoolean(
        def = true,
        desc = "Send update notices to OPed player when a new version of vane is available.",
    )
    var configUpdateNotices: Boolean = false

    /** Current running vane version string. */
    @Volatile var currentVersion: String? = null

    /** Latest version string fetched from GitHub releases. */
    @Volatile var latestVersion: String? = null

    init {
        check(INSTANCE == null) { "Cannot instanciate Core twice." }
        INSTANCE = this

        // Create global command catch-all permission
        registerPermission(permissionCommandCatchall)

        // Allow registration of new enchantments and entities
        unfreezeRegistries()

        // Components
        enchantmentManager = EnchantmentManager(this)
        HeadLibrary(this)
        AuthMultiplexer(this)
        LootChestProtector(this)
        VanillaFunctionalityInhibitor(this)
        DurabilityManager(this)
        Vane(this)
        CustomItem(this)
        Enchant(this)
        menuManager = MenuManager(this)
        resourcePackDistributor = ResourcePackDistributor(this)
        CommandHider(this)
        modelDataRegistry = CustomModelDataRegistry()
        itemRegistry = CustomItemRegistry()
        ExistingItemConverter(this)
    }

    /** Schedules periodic update checks when configured. */
    override fun onModuleEnable() {
        if (configUpdateNotices) {
            // Async: this does blocking network I/O and must never stall the main thread.
            server.scheduler.runTaskTimerAsynchronously(
                this,
                ::checkForUpdate,
                1L,
                msToTicks(2 * 60L * 60L * 1000L),
            )
        }
    }

    /** Unfreezes relevant registries to allow custom enchantment/entity registration. */
    fun unfreezeRegistries() {
        try {
            val frozen =
                MappedRegistry::class.java.getDeclaredField("frozen" /* frozen */).also {
                    it.isAccessible = true
                }
            val intrusiveHolderCache =
                MappedRegistry::class
                    .java
                    .getDeclaredField(
                        "unregisteredIntrusiveHolders" /* unregisteredIntrusiveHolders (1.19.3+), intrusiveHolderCache (until 1.19.2) */
                    )
                    .also { it.isAccessible = true }

            frozen.set(BuiltInRegistries.ENTITY_TYPE, false)
            intrusiveHolderCache.set(
                BuiltInRegistries.ENTITY_TYPE,
                IdentityHashMap<EntityType<*>, Holder.Reference<EntityType<*>>>(),
            )
        } catch (e: Exception) {
            when (e) {
                is NoSuchFieldException,
                is SecurityException,
                is IllegalArgumentException,
                is IllegalAccessException ->
                    log.log(Level.SEVERE, "Failed to unfreeze registries", e)

                else -> throw e
            }
        }
    }

    /** Core has no additional disable hook behavior. */
    override fun onModuleDisable() = Unit

    /** Generates the combined vane resource pack zip file. */
    fun generateResourcePack(): File? =
        runCatching {
                File("VaneResourcePack.zip").also { file ->
                    ResourcePackGenerator().also { pack ->
                        vaneModules.forEach { it.generateResourcePack(pack) }
                        pack.write(file)
                    }
                }
            }
            .onFailure { log.log(Level.SEVERE, "Error while generating resourcepack", it) }
            .getOrNull()

    /** Iterates over all module components across loaded modules. */
    fun forAllModuleComponents(f: Consumer1<ModuleComponent<*>?>?) {
        vaneModules.forEach { it.forEachModuleComponent(f) }
    }

    /** Returns the custom item registry. */
    fun itemRegistry(): CustomItemRegistry? = itemRegistry

    /** Returns the custom model data registry. */
    fun modelDataRegistry(): CustomModelDataRegistry? = modelDataRegistry

    /**
     * Checks GitHub for newer releases and updates cached version state. Runs off the main thread.
     */
    fun checkForUpdate() {
        if (currentVersion == null) {
            try {
                val stream =
                    Core::class.java.getResourceAsStream("/vane-core.properties")
                        ?: throw IOException("vane-core.properties is missing from the plugin jar")
                currentVersion =
                    "v" + stream.use { Properties().apply { load(it) } }.getProperty("version")
            } catch (e: IOException) {
                log.severe("Could not load current version from included properties file: $e")
                return
            }
        }

        try {
            val json = readJsonFromUrl("https://api.github.com/repos/oddlama/vane/releases/latest")
            latestVersion = json.getString("tag_name")
            if (isUpdateAvailable()) {
                log.warning(
                    "A newer version of vane is available online! (current=$currentVersion, new=$latestVersion)"
                )
                log.warning(
                    "Please update as soon as possible to get the latest features and fixes."
                )
                log.warning(
                    "Get the latest release here: https://github.com/oddlama/vane/releases/latest"
                )
            }
        } catch (e: Exception) {
            when (e) {
                is IOException,
                is JSONException,
                is URISyntaxException -> log.warning("Could not check for updates: $e")

                else -> throw e
            }
        }
    }

    /**
     * Whether the latest GitHub release is newer than the running version. Compared by semver
     * rather than inequality: `releases/latest` never returns pre-releases, so a pre-release build
     * would otherwise be told to "update" to the previous stable release.
     */
    private fun isUpdateAvailable(): Boolean {
        val latest = latestVersion ?: return false
        val current = currentVersion ?: return false
        return VersionUtil.isNewer(latest, current)
    }

    /** Sends update notices to operators when a newer version is known. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    fun onPlayerJoinSendUpdateNotice(event: PlayerJoinEvent) {
        if (!configUpdateNotices) return

        val player = event.player
        if (player.isOp && isUpdateAvailable()) {
            player.sendMessage(
                Component.text("A new version of vane ", NamedTextColor.GREEN)
                    .append(Component.text("($latestVersion)", NamedTextColor.AQUA))
                    .append(Component.text(" is available!", NamedTextColor.GREEN))
            )
            player.sendMessage(
                Component.text(
                    "Please update soon to get the latest features.",
                    NamedTextColor.GREEN,
                )
            )
            player.sendMessage(
                Component.text("Click here to go to the download page", NamedTextColor.AQUA)
                    .clickEvent(
                        ClickEvent.openUrl("https://github.com/oddlama/vane/releases/latest")
                    )
            )
        }
    }

    /** Static core instance access. */
    companion object {
        /** Use sparingly. */
        private var INSTANCE: Core? = null

        /** Returns the active core instance, if initialized. */
        fun instance(): Core? = INSTANCE
    }
}
