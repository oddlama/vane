package org.oddlama.vane.busybar.hooks

import java.util.*
import org.bukkit.permissions.Permission
import org.bukkit.plugin.Plugin
import org.oddlama.vane.permissions.Permissions

/**
 * Evaluates permissions of offline players from vane-permissions' stored groups.
 *
 * Without this, an offline player's grants are only known from when they were last online.
 * vane-permissions keeps every player's groups, so a group change applies to their bridge without
 * waiting for them to rejoin.
 *
 * @param permissions the vane-permissions module.
 */
class PermissionsHook private constructor(private val permissions: Permissions) {
    /**
     * Whether offline [player] holds [permission]. Mirrors how vane-permissions attaches groups on
     * join: every permission of every group is set to true, and a permission not set falls back to
     * its default. Call from the main thread.
     */
    fun has(player: UUID, isOp: Boolean, permission: Permission): Boolean {
        val groups =
            permissions.storagePlayerGroups[player]?.takeIf { it.isNotEmpty() }
                ?: setOf(permissions.configDefaultGroup ?: "default")
        val granted = groups.flatMap { permissions.permissionGroups[it].orEmpty() }
        return permission.name in expand(granted) || permission.default.getValue(isOp)
    }

    /** Adds every child a granted permission switches on, the way Bukkit resolves attachments. */
    private fun expand(granted: Collection<String>): Set<String> {
        val pluginManager = permissions.server.pluginManager
        val result = mutableSetOf<String>()
        val pending = ArrayDeque(granted)
        while (pending.isNotEmpty()) {
            val name = pending.removeFirst()
            if (!result.add(name)) continue
            pluginManager.getPermission(name)?.children?.forEach { (child, value) ->
                if (value) pending += child
            }
        }
        return result
    }

    companion object {
        /** Creates the hook when [plugin] is vane-permissions. */
        fun create(plugin: Plugin): PermissionsHook? =
            (plugin as? Permissions)?.let(::PermissionsHook)
    }
}
