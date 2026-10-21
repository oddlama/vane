package org.oddlama.vane.geyserextension

import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import org.geysermc.event.subscribe.Subscribe
import org.geysermc.geyser.api.event.lifecycle.*
import org.geysermc.geyser.api.extension.Extension
import org.geysermc.geyser.api.pack.PackCodec
import org.geysermc.geyser.api.pack.ResourcePack

/**
 * Main entry point for the Vane Geyser extension.
 *
 * This class implements [Extension] and acts as the lifecycle manager for the vane-geyser-extension
 * module. It subscribes to Geyser lifecycle events to:
 * - Log extension metadata during pre-initialization.
 * - Track loaded resource packs.
 * - Delegate command and custom item registration to [CommandsRegistration] and [ItemRegistration].
 *
 * The extension is declared in `extension.yml` and instantiated by the Geyser extension loader.
 */
class VaneGeyser : Extension {
    /**
     * Handles the [GeyserPreInitializeEvent] to print a startup banner.
     *
     * Logs the extension name, version, authors, and a brief description to the Geyser console
     * before the server finishes initialization.
     */
    @Subscribe
    fun onGeyserPreInitializeEvent(event: GeyserPreInitializeEvent) {
        val desc = description()
        logger().info("")
        logger().info("##############################################")
        logger().info("Extension: ${desc.name()}")
        logger().info("Version: ${desc.version()}")
        logger().info("Authors: ${desc.authors().joinToString(", ")}")
        logger()
            .info(
                "Description: A plugin-suite that provides many immersive and lore-friendly additions to vanilla Minecraft."
            )
        logger().info("##############################################")
        logger().info("")

        // Must run before Geyser loads any locale, and pre-initialize is the only event that does.
        try {
            LocaleOverrides.deploy(this)
        } catch (e: Exception) {
            // Never break Geyser's startup over translations; Bedrock players then see raw keys.
            logger()
                .error(
                    "Could not deploy vane translations to Geyser's locales/overrides directory",
                    e,
                )
        }
    }

    /**
     * Handles the [GeyserDefineResourcePacksEvent] to register vane's Bedrock resource pack.
     *
     * The pack is generated from vane's Java resource pack at build time and bundled in this jar
     * (see `generateBedrockPack` in the build script). It is extracted to the data folder on every
     * start, so an updated extension always serves its matching pack.
     */
    @Subscribe
    fun onGeyserDefineResourcePacksEvent(event: GeyserDefineResourcePacksEvent) {
        try {
            val pack = dataFolder().resolve(BEDROCK_PACK_FILE)
            Files.createDirectories(pack.parent)
            val bundled =
                javaClass.getResourceAsStream(BEDROCK_PACK_RESOURCE)
                    ?: throw IOException("$BEDROCK_PACK_RESOURCE is missing from the extension jar")
            bundled.use { Files.copy(it, pack, StandardCopyOption.REPLACE_EXISTING) }
            val resourcePack = ResourcePack.create(PackCodec.path(pack))
            event.register(resourcePack)
            logger().info("Registered vane Bedrock resource pack ${resourcePack.uuid()}.")
        } catch (e: Exception) {
            // Never break Geyser's startup over the pack; Bedrock players then just lack vane's
            // textures.
            logger()
                .error(
                    "Could not register the vane Bedrock resource pack. If a manually installed copy is in " +
                        "Geyser's packs folder, remove it: this extension now provides the pack itself.",
                    e,
                )
        }
        logger().info("Loading: ${event.resourcePacks().size} resource packs.")
    }

    /**
     * Handles the [GeyserPostInitializeEvent] fired after Geyser is fully initialized.
     *
     * Logs the extension name and its data folder path once Geyser is ready to accept Bedrock
     * player connections.
     */
    @Subscribe
    fun onPostInitialize(event: GeyserPostInitializeEvent?) {
        with(logger()) {
            info("Loading ${description().name()}...")
            info("${dataFolder()}")
        }
    }

    /**
     * Handles the [GeyserPreReloadEvent] to support extension reloading.
     *
     * Logs a reload message when the Geyser reload cycle is triggered. Extension configuration
     * could be re-read here if needed.
     *
     * Note: Geyser's extension template declares this handler with a [GeyserPreInitializeEvent]
     * parameter while its documentation points at the reload event. Following the parameter rather
     * than the docs subscribes a second handler to pre-initialize, so every startup logs a
     * "Reloading" line, and no reload is ever observed.
     */
    @Subscribe
    fun onGeyserReload(event: GeyserPreReloadEvent) {
        logger().info("Reloading ${description().name()}!")
    }

    /**
     * Handles the [GeyserDefineCommandsEvent] to register Bedrock-specific commands.
     *
     * Delegates to [CommandsRegistration] which registers the `/vanegeyser menu` command and all
     * associated Bedrock form-based UI menus.
     */
    @Subscribe
    fun onGeyserDefineCommands(event: GeyserDefineCommandsEvent) {
        CommandsRegistration.onGeyserDefineCommands(event, this)
    }

    /**
     * Handles the [GeyserDefineCustomItemsEvent] to register custom item definitions.
     *
     * Delegates to [ItemRegistration] which maps vane's custom Java items (tomes, sickles, scrolls,
     * etc.) to their Bedrock resource pack counterparts.
     */
    @Subscribe
    fun onGeyserDefineCustomItems(event: GeyserDefineCustomItemsEvent) {
        ItemRegistration.onGeyserDefineCustomItems(event)
    }

    private companion object {
        /** Classpath location of the pack produced by the `generateBedrockPack` build task. */
        const val BEDROCK_PACK_RESOURCE = "/bedrock/vane.mcpack"

        /** File name of the extracted pack inside the extension's data folder. */
        const val BEDROCK_PACK_FILE = "vane.mcpack"
    }
}
