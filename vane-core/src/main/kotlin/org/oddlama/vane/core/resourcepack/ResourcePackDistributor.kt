package org.oddlama.vane.core.resourcepack

import com.destroystokyo.paper.event.player.PlayerConnectionCloseEvent
import io.papermc.paper.connection.PlayerConfigurationConnection
import io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent
import io.papermc.paper.event.connection.configuration.PlayerConnectionReconfigureEvent
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.resource.ResourcePackInfo
import net.kyori.adventure.resource.ResourcePackRequest
import net.kyori.adventure.resource.ResourcePackStatus
import net.kyori.adventure.text.Component
import org.bukkit.event.EventHandler
import org.oddlama.vane.annotation.config.ConfigBoolean
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.core.Core
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.ModuleGroup
import org.oddlama.vane.util.Nms.serverHandle
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.logging.Level

/**
 * Distributes the configured resource pack to connecting players.
 *
 * @param context the module context.
 */
class ResourcePackDistributor(context: Context<Core?>) :
    Listener<Core?>(context.group("ResourcePack", "Enable resource pack distribution.")) {

    /**
     * If enabled, players must accept the resource pack.
     */
    @ConfigBoolean(def = true, desc = "Kick players if they deny to use the specified resource pack (if set).")
    var configForce: Boolean = false

    /**
     * Prompt message used when the pack is required.
     */
    @LangMessage
    var langPackRequired: TranslatedMessage? = null

    /**
     * Prompt message used when the pack is optional.
     */
    @LangMessage
    var langPackSuggested: TranslatedMessage? = null

    /**
     * URL of the resource pack to distribute.
     */
    var packUrl: String? = null

    /**
     * SHA-1 checksum of the resource pack.
     */
    var packSha1: String? = null

    /**
     * UUID used for the distributed resource pack.
     */
    var packUuid: UUID = UUID.fromString("fbba121a-8f87-4e97-922d-2059777311bf")

    /**
     * Cache-busting counter used in local development mode.
     */
    @JvmField
    var counter: Int = 0

    /**
     * Optional custom resource pack configuration.
     */
    var customResourcePackConfig: CustomResourcePackConfig = CustomResourcePackConfig(requireNotNull(getContext()))

    /**
     * Watches the pack file and triggers refreshes in local development mode.
     */
    private var fileWatcher: ResourcePackFileWatcher? = null

    /**
     * Local HTTP server used in local development mode.
     */
    private var devServer: ResourcePackDevServer? = null

    /**
     * Synchronization latches used to block player configuration until pack status is known.
     */
    private val latches = ConcurrentHashMap<UUID, CountDownLatch>()

    /**
     * Initializes the active resource pack source and enables distribution listeners.
     */
    public override fun onEnable() {
        val mod = requireNotNull(module)
        when {
            localDev -> {
                try {
                    val packOutput: File? = File("VaneResourcePack.zip").takeIf { it.exists() }
                        ?: mod.generateResourcePack().also {
                            if (it == null) {
                                mod.log.severe("Failed to generate resource pack; dev server and file watcher will not be started")
                            } else {
                                mod.log.info("Resource Pack Missing, first run? Generating resource pack.")
                            }
                        }

                    packOutput?.let { file ->
                        fileWatcher = ResourcePackFileWatcher(this, file)
                        devServer = ResourcePackDevServer(this, file).also { it.serve() }
                        fileWatcher?.watchForChanges()
                    }
                } catch (e: Exception) {
                    if (e is IOException || e is InterruptedException) {
                        mod.log.log(Level.SEVERE, "Failed to initialize resource pack dev server or file watcher", e)
                    } else throw e
                }
                mod.log.info("Setting up dev lazy server")
            }

            (customResourcePackConfig.getContext() as? ModuleGroup<*>)?.configEnabled == true -> {
                mod.log.info("Serving custom resource pack")
                packUrl = customResourcePackConfig.configUrl
                packSha1 = customResourcePackConfig.configSha1
                // The UUID defaults to empty, so a missing or malformed one must not fail enabling.
                val configUuid = customResourcePackConfig.configUuid
                packUuid = ResourcePackUuid.parse(configUuid)
                    ?: ResourcePackUuid.fromUrl(customResourcePackConfig.configUrl).also {
                        if (configUuid.isNotBlank()) {
                            mod.log.warning("Invalid resource pack UUID '$configUuid', using $it derived from the URL")
                        }
                    }
            }

            else -> {
                mod.log.info("Serving official vane resource pack")
                try {
                    val properties = Properties().also { props ->
                        Core::class.java.getResourceAsStream("/vane-core.properties")!!.use { props.load(it) }
                    }
                    packUrl = properties.getProperty("resourcePackUrl")
                    packSha1 = properties.getProperty("resourcePackSha1")
                    packUuid = UUID.fromString(properties.getProperty("resourcePackUuid"))
                } catch (_: IOException) {
                    mod.log.severe("Could not load official resource pack sha1 from included properties file")
                    packUrl = ""
                    packSha1 = ""
                    packUuid = UUID.randomUUID()
                }
            }
        }

        val sha1 = requireNotNull(packSha1)
        // Validate SHA-1 format before enabling distribution.
        if (sha1.length != 40) {
            mod.log.warning("Invalid resource pack SHA-1 sum '$sha1', should be 40 characters long but has ${sha1.length} characters")
            mod.log.warning("Disabling resource pack serving and message delaying")
            packUrl = ""
        }

        // Clients can only download from an absolute http(s) URL; refuse anything else up front.
        val url = requireNotNull(packUrl)
        if (url.isNotEmpty() && ResourcePackUrl.parse(url) == null) {
            mod.log.warning("Invalid resource pack URL '$url', must be an absolute http or https URL")
            mod.log.warning("Disabling resource pack serving and message delaying")
            packUrl = ""
        }

        // Enable listeners only after the active distribution mode has been resolved.
        super.onEnable()

        packSha1 = sha1.lowercase()
        if (packUrl!!.isNotEmpty()) {
            serverHandle().settings.properties.serverResourcePackInfo.ifPresent { rpInfo ->
                if (rpInfo.url().trim().isNotEmpty()) {
                    mod.log.warning(
                        "You have manually configured a resource pack in your server.properties. " +
                                "This cannot be used together with vane, as servers only allow serving a single resource pack."
                    )
                }
            }
            mod.log.info("Distributing resource pack from '$packUrl' with sha1 $packSha1")
        }
    }

    /**
     * Stops all pending waits and shuts down local development helpers.
     */
    public override fun onDisable() {
        latches.values.forEach { it.countDown() }
        latches.clear()

        fileWatcher?.stop()
        fileWatcher = null
        devServer?.stop()
        devServer = null

        super.onDisable()
    }

    /**
     * Sends the resource pack during async player configuration and waits for completion.
     *
     * @param event the async configure event.
     */
    @EventHandler
    fun onPlayerAsyncConnectionConfigure(event: AsyncPlayerConnectionConfigureEvent) {
        val profileUuid = event.connection.profile.id ?: return

        val latch = CountDownLatch(1)
        latches.put(profileUuid, latch)?.countDown()

        // Only wait for an answer if a request was actually sent.
        if (sendResourcePackDuringConfiguration(event.connection)) {
            try {
                latch.await()
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                module?.log?.warning("Resource pack wait interrupted for player $profileUuid")
            }
        } else {
            latches.remove(profileUuid, latch)
        }

        event.connection.completeReconfiguration()
    }

    /**
     * Re-sends the resource pack after a player reconfiguration request.
     *
     * @param event the reconfigure event.
     */
    @EventHandler
    fun onPlayerConnectionReconfigure(event: PlayerConnectionReconfigureEvent) {
        sendResourcePackDuringConfiguration(event.connection)
    }

    /**
     * Releases pending waits when a player connection closes.
     *
     * @param event the close event.
     */
    @EventHandler
    fun onPlayerConnectionClose(event: PlayerConnectionCloseEvent) {
        latches.remove(event.playerUniqueId)?.countDown()
    }

    /**
     * Returns the pack info for [url], or `null` (with a warning) if no pack can be sent from it.
     */
    private fun packInfo(url: String?): ResourcePackInfo? {
        if (url.isNullOrEmpty()) return null
        val uri = ResourcePackUrl.parse(url) ?: run {
            module?.log?.warning("The provided resource pack URL is incorrect: $url")
            return null
        }
        return ResourcePackInfo.resourcePackInfo(packUuid, uri, requireNotNull(packSha1))
    }

    /**
     * Sends the resource pack request on the configuration connection.
     *
     * @param connection the player configuration connection.
     * @return whether a request was sent.
     */
    fun sendResourcePackDuringConfiguration(connection: PlayerConfigurationConnection): Boolean {
        val info = packInfo(packUrl) ?: return false
        val promptLang = requireNotNull(if (configForce) langPackRequired else langPackSuggested)
        val prompt: Component? = if (promptLang.str().isEmpty()) null else promptLang.strComponent()
        val request = ResourcePackRequest.resourcePackRequest()
            .required(configForce).replace(true)
            .packs(info)
            .callback { _: UUID?, status: ResourcePackStatus?, _: Audience? ->
                if (status?.intermediate() == false) {
                    latches.remove(connection.profile.id)?.countDown()
                }
            }
            .prompt(prompt)
            .build()

        connection.audience.sendResourcePacks(request)
        return true
    }

    /**
     * Sends the configured resource pack request to an audience.
     *
     * @param audience the target audience.
     */
    fun sendResourcePack(audience: Audience) {
        var url = packUrl
        if (localDev) {
            url = "$packUrl?$counter"
            audience.sendMessage(Component.text("$url $packSha1"))
        }
        val info = packInfo(url) ?: return
        audience.sendResourcePacks(ResourcePackRequest.resourcePackRequest().packs(info).asResourcePackRequest())
    }

    /**
     * Recomputes and updates [packSha1] from the given file in local development mode.
     *
     * @param file the resource pack file.
     */
    fun updateSha1(file: File) {
        if (!localDev) return
        try {
            val hashBytes = MessageDigest.getInstance("SHA-1").digest(file.readBytes())
            packSha1 = hashBytes.joinToString("") { "%02x".format(it) }
        } catch (_: IOException) {
        }
    }

    /**
     * Holds static helpers for development-mode detection.
     */
    companion object {
        /**
         * Whether the server appears to run in a local development environment.
         *
         * This relies on current `run-paper` flags until upstream provides a dedicated signal:
         * https://github.com/jpenilla/run-paper/issues/14
         */
        private val localDev =
            serverHandle().options.hasArgument("add-plugin") && System.getProperty("disable.watchdog") != null
    }
}
