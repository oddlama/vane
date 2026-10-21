package org.oddlama.vane.proxycore.config

import com.electronwill.nightconfig.core.CommentedConfig
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.*
import javax.imageio.ImageIO
import kotlin.random.Random

/**
 * Represents a managed backend server and state-dependent presentation/start settings.
 *
 * @property id stable server identifier.
 * @constructor Creates a managed server from parsed online/offline and start sections.
 */
class ManagedServer(
    private val id: String,
    displayName: String,
    onlineConfigSection: CommentedConfig?,
    offlineConfigSection: CommentedConfig?,
    start: CommentedConfig?,
) {
    /** Start behavior configuration for this server. */
    @JvmField var start: ServerStart = ServerStart(id, displayName, start)

    /** Presentation config used while backend is online. */
    private val onlineConfig: StatefulConfiguration =
        StatefulConfiguration(id, displayName, onlineConfigSection)

    /** Presentation config used while backend is offline. */
    private val offlineConfig: StatefulConfiguration =
        StatefulConfiguration(id, displayName, offlineConfigSection)

    /**
     * Returns the server identifier.
     *
     * @return configured id.
     */
    fun id(): String = id

    /**
     * Returns the encoded favicon for the selected [source] state.
     *
     * @param source online/offline state selector.
     * @return base64 data URL favicon or `null`.
     */
    fun favicon(source: ConfigItemSource): String? {
        return when (source) {
            ConfigItemSource.ONLINE -> {
                onlineConfig.encodedFavicon
            }

            ConfigItemSource.OFFLINE -> {
                offlineConfig.encodedFavicon
            }
        }
    }

    /**
     * Returns the configured start command tokens.
     *
     * @return command array or `null` when no command is configured.
     */
    fun startCmd(): Array<String?>? = start.cmd

    /**
     * Returns the kick message used after issuing a start command.
     *
     * @return configured kick message or `null`.
     */
    fun startKickMsg(): String? = start.kickMsg

    /**
     * Picks a random quote for the selected [source] state.
     *
     * @param source online/offline state selector.
     * @return selected quote or an empty string when none are available.
     */
    private fun randomQuote(source: ConfigItemSource): String? {
        val quoteSet =
            when (source) {
                ConfigItemSource.ONLINE -> onlineConfig.quotes
                ConfigItemSource.OFFLINE -> offlineConfig.quotes
            }

        if (quoteSet.isNullOrEmpty()) {
            return ""
        }
        return quoteSet[Random.nextInt(quoteSet.size)]
    }

    /**
     * Returns the state-dependent MOTD with quote placeholders resolved.
     *
     * @param source online/offline state selector.
     * @return formatted MOTD or an empty string.
     */
    fun motd(source: ConfigItemSource): String {
        val (sourcedMotd, quoteSource) =
            when (source) {
                ConfigItemSource.ONLINE -> onlineConfig.motd to ConfigItemSource.ONLINE
                ConfigItemSource.OFFLINE -> offlineConfig.motd to ConfigItemSource.OFFLINE
            }

        return sourcedMotd?.replace("{QUOTE}", randomQuote(quoteSource).orEmpty()) ?: ""
    }

    /**
     * Returns the timeout for the start command.
     *
     * @return timeout in seconds.
     */
    fun commandTimeout(): Int? = start.timeout

    /** Selects which state-specific configuration should be used. */
    enum class ConfigItemSource {
        /** Use presentation values for online backend state. */
        ONLINE,

        /** Use presentation values for offline backend state. */
        OFFLINE,
    }

    /**
     * Holds state-specific display values for MOTD, quote set, and favicon.
     *
     * @constructor Parses a state section and resolves placeholders.
     */
    private class StatefulConfiguration(id: String, displayName: String, config: CommentedConfig?) {
        /** Candidate quote lines used by `{QUOTE}` placeholder replacement. */
        var quotes: Array<String?>? = null

        /** MOTD template for this state. */
        var motd: String? = null

        /** Encoded `data:image/png;base64,...` favicon value. */
        var encodedFavicon: String? = null

        init {
            // [managedServers.MyServer.state]
            if (config != null) {
                // quotes = ["", ...]
                val quotesList = config.get<MutableList<String?>?>("Quotes")

                if (quotesList != null) {
                    quotes =
                        quotesList
                            .mapNotNull { s -> s?.takeUnless { it.isBlank() } }
                            .map { s ->
                                s.replace("{SERVER}", id)
                                    .replace("{SERVER_DISPLAY_NAME}", displayName)
                            }
                            .toTypedArray()
                }

                // motd = "..."
                val motdVal = config.get<Any?>("MOTD")

                require(motdVal == null || motdVal is String) {
                    "Managed server '$id' has a non-string MOTD!"
                }

                if (motdVal is String && motdVal.isNotEmpty())
                    motd =
                        motdVal.replace(
                            "{SERVER_DISPLAY_NAME}",
                            displayName,
                        )

                // favicon = "..."
                val faviconPath = config.get<Any?>("Favicon")

                require(faviconPath == null || faviconPath is String) {
                    "Managed server '$id' has a non-string favicon path!"
                }

                if (faviconPath is String && faviconPath.isNotEmpty())
                    encodedFavicon =
                        encodeFavicon(
                            id,
                            faviconPath,
                        )
            }
        }

        companion object {
            /**
             * Loads and encodes a favicon image into a PNG data URL.
             *
             * @param id managed server id used for placeholder substitution.
             * @param faviconPath path template to favicon file.
             * @return encoded favicon data URL.
             * @throws IOException when the image cannot be read or encoded.
             */
            @Throws(IOException::class)
            private fun encodeFavicon(id: String, faviconPath: String): String {
                val faviconFile = File(faviconPath.replace("{SERVER}", id))
                val image: BufferedImage
                try {
                    image = ImageIO.read(faviconFile)
                } catch (e: IOException) {
                    throw IOException("Failed to read favicon file: " + e.message)
                }

                require(!(image.width != 64 || image.height != 64)) {
                    "Favicon has invalid dimensions (must be 64x64)"
                }

                val stream = ByteArrayOutputStream()
                ImageIO.write(image, "PNG", stream)
                val faviconBytes = stream.toByteArray()

                val encodedFavicon =
                    "data:image/png;base64,${Base64.getEncoder().encodeToString(faviconBytes)}"

                require(encodedFavicon.length <= Short.MAX_VALUE) {
                    "Favicon file too large for server to process"
                }

                return encodedFavicon
            }
        }
    }

    /**
     * Configures how a managed server should be started.
     *
     * @constructor Parses start command settings and validation values.
     */
    class ServerStart(id: String, displayName: String, config: CommentedConfig?) {
        /** Command tokens used to start the backend process. */
        var cmd: Array<String?>?

        /** Maximum allowed command runtime in seconds. */
        var timeout: Int?

        /** Kick message shown to connecting users while startup is in progress. */
        var kickMsg: String? = null

        /** Whether any player may trigger startup even without dedicated permissions. */
        @JvmField var allowAnyone: Boolean = false

        init {
            val cmdList = config?.get<MutableList<String?>?>("CMD")
            val timeoutVal = config?.get<Any?>("Timeout")
            val kickMsgVal = config?.get<Any?>("KickMsg")
            val allowAnyoneVal = config?.get<Any?>("AllowAnyone")

            cmd = cmdList?.map { it?.replace("{SERVER}", id) }?.toTypedArray()

            require(kickMsgVal == null || kickMsgVal is String) {
                "Managed server '$id' has an invalid kick message!"
            }

            kickMsg =
                if (kickMsgVal is String)
                    kickMsgVal
                        .replace("{SERVER}", id)
                        .replace(
                            "{SERVER_DISPLAY_NAME}",
                            displayName,
                        )
                else null

            allowAnyone = allowAnyoneVal as? Boolean ?: false

            if (timeoutVal == null) {
                timeout = DEFAULT_TIMEOUT_SECONDS
            } else {
                require(timeoutVal is Int) {
                    "Managed server '$id' has an invalid command timeout!"
                }

                timeout = timeoutVal
            }
        }

        companion object {
            /** Default command timeout in seconds when not explicitly configured. */
            private const val DEFAULT_TIMEOUT_SECONDS = 10
        }
    }
}
