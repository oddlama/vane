package org.oddlama.vane.core.resourcepack

import org.oddlama.vane.annotation.config.ConfigString
import org.oddlama.vane.core.Core
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.ModuleComponent

/**
 * Configuration component for distributing a custom resource pack.
 *
 * @param context the module context.
 */
class CustomResourcePackConfig(context: Context<Core?>) : ModuleComponent<Core?>(
    context.group(
        "CustomResourcePack",
        "If this is not enabled, vane will automatically distribute the official vane resource pack. By enabling this option, you can have vane distribute a custom resource pack (with the given url and sha1) instead of the official vane resource pack. Use this option only if you either want to distribute another resource pack (you will need to merge the vane resources by hand!) or self-host the vane resource pack (generated via `/vane generateResourcePack`). The latter is necessary when you make adjustments to the language files of vane. For more information on this, see the wiki (https://github.com/oddlama/vane/wiki/Creating-a-Translation).",
        false
    )
) {
    /**
     * URL from which clients should download the custom resource pack.
     */
    @ConfigString(
        def = "https://your-server.tld/path/to/pack.zip",
        desc = "URL to an resource pack. Will request players to use the specified resource pack. [as of 1.16.2] Beware that the minecraft client currently has issues with webservers that serve resource packs via https and don't allow ssl3. This protocol is considered insecure and therefore should NOT be used. To workaround this issue, you should host the file in a http context. Using http is not a security issue, as the file will be verified via its sha1 sum by the client."
    )
    /** Configured URL used for resource-pack download. */
    lateinit var configUrl: String

    /**
     * SHA-1 checksum used by clients to verify resource pack integrity.
     */
    @ConfigString(def = "", desc = "Resource pack SHA-1 sum. Required to verify resource pack integrity.")
    lateinit var configSha1: String

    /**
     * UUID of the custom resource pack.
     */
    @ConfigString(def = "", desc = "Resource pack UUID. Leave empty to derive one from the URL.")
    lateinit var configUuid: String

    /**
     * Enables this component.
     */
    override fun onEnable() {}

    /**
     * Disables this component.
     */
    override fun onDisable() {}
}
