package org.oddlama.vane.proxycore.commands

import java.util.*
import org.oddlama.vane.proxycore.VaneProxyPlugin

/**
 * Base abstraction for proxy commands.
 *
 * @property permission permission node required to execute this command, or `null` for unrestricted
 *   access.
 * @property plugin owning plugin context.
 */
abstract class ProxyCommand(val permission: String?, @JvmField val plugin: VaneProxyPlugin) {
    /**
     * Executes the command.
     *
     * @param sender command sender.
     * @param args command arguments.
     */
    abstract fun execute(sender: ProxyCommandSender?, args: Array<String?>?)

    /**
     * Checks whether [uuid] may execute this command.
     *
     * @param uuid player UUID.
     * @return `true` when unrestricted or permission check succeeds.
     */
    fun hasPermission(uuid: UUID?): Boolean =
        permission == null || plugin.proxy.hasPermission(uuid, permission)
}
