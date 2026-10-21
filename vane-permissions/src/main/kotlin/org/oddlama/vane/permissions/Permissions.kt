package org.oddlama.vane.permissions

import java.util.*
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerKickEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.server.RemoteServerCommandEvent
import org.bukkit.event.server.ServerCommandEvent
import org.bukkit.permissions.PermissionAttachment
import org.bukkit.permissions.PermissionDefault
import org.oddlama.vane.annotation.VaneModule
import org.oddlama.vane.annotation.config.ConfigBoolean
import org.oddlama.vane.annotation.config.ConfigString
import org.oddlama.vane.annotation.config.ConfigStringListMap
import org.oddlama.vane.annotation.config.ConfigStringListMapEntry
import org.oddlama.vane.annotation.persistent.Persistent
import org.oddlama.vane.core.module.Module
import org.oddlama.vane.permissions.commands.Permission
import org.oddlama.vane.permissions.commands.Vouch

@VaneModule(
    name = "permissions",
    bstats = 8641,
    configVersion = 1,
    langVersion = 1,
    storageVersion = 1,
)
/** Manages permission groups, inheritance flattening, and runtime player attachments. */
class Permissions : Module<Permissions?>() {
    /** Non-null typed accessor for this module instance. */
    private val permissionsModule: Permissions
        get() = requireNotNull(module)

    /**
     * If true, resets all default permissions to false and relies entirely on configured groups.
     */
    @ConfigBoolean(
        def = false,
        desc =
            "Remove all default permissions from ANY SOURCE (including other plugins and minecraft permissions) to start with a clean preset. This will allow you to exactly set which player have which permissions instead of having to resort to volatile stateful changes like negative permissions. This will result in OPed players to lose access to commands, if not explicitly added back via permissions. The wildcard permissions can be viewed using `perms list permissions`. The wildcard permissions `minecraft` and `craftbukkit` may be especially useful.",
    )
    var configRemoveDefaults: Boolean = false

    /** Name of the fallback group applied when a player has no stored assignments. */
    @ConfigString(
        def = "default",
        desc =
            "The permission group that will be given to players that have no other permission group.",
    )
    var configDefaultGroup: String? = null

    /** Raw configured group definitions before inheritance flattening. */
    @ConfigStringListMap(
        def =
            [
                ConfigStringListMapEntry(
                    key = "default",
                    list = ["bukkit.command.help", "bukkit.broadcast", "bukkit.broadcast.user"],
                ),
                ConfigStringListMapEntry(
                    key = "user",
                    list =
                        [
                            "vane.permissions.groups.default",
                            "vane.admin.modify_world",
                            "vane.regions.commands.region",
                            "vane.trifles.commands.heads",
                        ],
                ),
                ConfigStringListMapEntry(
                    key = "verified",
                    list = ["vane.permissions.groups.user", "vane.permissions.commands.vouch"],
                ),
                ConfigStringListMapEntry(
                    key = "admin",
                    list =
                        [
                            "vane.permissions.groups.verified",
                            "vane.admin.bypass_spawn_protection",
                            "vane.portals.admin",
                            "vane.regions.admin",
                            "vane.*.commands.*",
                        ],
                ),
            ],
        desc =
            "The permission groups. A player can have multiple permission groups assigned. Permission groups can inherit other permission groups by specifying vane.permissions.groups.<groupname> as a permission.",
    )
    var configGroups: MutableMap<String, MutableList<String>>? = null

    /** Persistent mapping from player UUID to assigned permission groups. */
    @Persistent var storagePlayerGroups: MutableMap<UUID, MutableSet<String>> = HashMap()

    /** Flattened permissions per group after inheritance resolution. */
    val permissionGroups: MutableMap<String, MutableSet<String>> = HashMap()

    /** Active permission attachments for online players. */
    private val playerAttachments: MutableMap<UUID, PermissionAttachment> = HashMap()

    /** Applies configured default-permission reset on module enable. */
    override fun onModuleEnable() {
        scheduleNextTick {
            if (configRemoveDefaults) {
                for (perm in permissionsModule.server.pluginManager.permissions) {
                    perm.default = PermissionDefault.FALSE
                    permissionsModule.server.pluginManager.recalculatePermissionDefaults(perm)

                    addConsolePermission(perm)
                }
            }
        }
    }

    /** Rebuilds flattened group permissions whenever configuration changes. */
    override fun onConfigChange() {
        flattenGroups()
    }

    /** Registers and recalculates permissions for joining players. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        registerPlayer(event.player)
    }

    /** Unregisters permission attachments when a player is kicked. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerKick(event: PlayerKickEvent) {
        unregisterPlayer(event.player)
    }

    /** Unregisters permission attachments when a player quits. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        unregisterPlayer(event.player)
    }

    /** Command-sender attachments used to mirror console-level permissions. */
    private val senderAttachments = mutableMapOf<CommandSender, PermissionAttachment>()

    init {
        Permission(this)
        Vouch(this)
    }

    /** Ensures a sender has mirrored console permissions attached. */
    private fun addConsolePermissions(sender: CommandSender) {
        if (!senderAttachments.containsKey(sender)) {
            val attachment = sender.addAttachment(this)
            senderAttachments[sender] = attachment

            val attachedPerms = permissionsModule.consoleAttachment?.permissions.orEmpty()
            attachedPerms.forEach { (permission, value) ->
                attachment.setPermission(permission, value)
            }
        }
    }

    /** Mirrors console attachment permissions for operator command senders. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onServerCommandEvent(event: ServerCommandEvent) {
        val sender = event.sender
        if (sender is Player && sender.isOp) {
            addConsolePermissions(sender)
        }
    }

    /** Mirrors console attachment permissions for remote operator command senders. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onRemoteServerCommandEvent(event: RemoteServerCommandEvent) {
        val sender = event.sender
        if (sender.isOp) {
            addConsolePermissions(sender)
        }
    }

    /** Resolve references to other permission groups in the hierarchy. */
    private fun flattenGroups() {
        val inheritancePrefix = "vane.permissions.groups."
        permissionGroups.clear()
        configGroups?.forEach { (groupName, permissions) ->
            permissionGroups[groupName] =
                permissions.filterNot { it.startsWith(inheritancePrefix) }.toMutableSet()
        }

        var modified = true
        while (modified) {
            modified = false
            configGroups?.forEach { (groupName, permissions) ->
                val resolvedPermissions = permissionGroups[groupName] ?: return@forEach
                for (permission in permissions) {
                    if (permission.startsWith(inheritancePrefix)) {
                        val group = permission.substring(inheritancePrefix.length)
                        val groupPerms = permissionGroups[group]
                        if (groupPerms == null) {
                            log.severe(
                                "Nonexistent permission group '$group' referenced by group '$groupName'; Ignoring statement!"
                            )
                            continue
                        }
                        if (resolvedPermissions.addAll(groupPerms)) {
                            modified = true
                        }
                    }
                }
            }
        }
    }

    /** Creates and stores a permission attachment for a joining player. */
    private fun registerPlayer(player: Player) {
        val attachment = player.addAttachment(this)
        playerAttachments[player.uniqueId] = attachment

        recalculatePlayerPermissions(player)
    }

    /** Rebuilds all effective attachment permissions for the given player. */
    fun recalculatePlayerPermissions(player: Player) {
        val attachment = playerAttachments[player.uniqueId] ?: return
        val attachedPerms = attachment.permissions
        attachedPerms.keys.toList().forEach(attachment::unsetPermission)

        var groups = storagePlayerGroups[player.uniqueId]
        if (groups.isNullOrEmpty()) {
            val defaultGroup = configDefaultGroup ?: "default"
            groups = mutableSetOf(defaultGroup)
        }

        for (group in groups) {
            for (permission in permissionGroups[group].orEmpty()) {
                val perm = permissionsModule.server.pluginManager.getPermission(permission)
                if (perm == null) {
                    log.warning(
                        "Use of unregistered permission '$permission' might have unintended effects."
                    )
                }
                attachment.setPermission(permission, true)
            }
        }

        player.updateCommands()
    }

    /** Removes and detaches stored attachment for an offline player. */
    private fun unregisterPlayer(player: Player) {
        val attachment = playerAttachments.remove(player.uniqueId)
        if (attachment != null) {
            player.removeAttachment(attachment)
        }
    }

    /** Persists storage changes and recalculates permissions if the player is online. */
    fun saveAndRecalculate(player: OfflinePlayer) {
        markPersistentStorageDirty()

        player.player?.let(::recalculatePlayerPermissions)
    }

    /** Adds a player to a group and triggers persistence + recalculation on change. */
    fun addPlayerToGroup(player: OfflinePlayer, group: String?): Boolean {
        val set = storagePlayerGroups.getOrPut(player.uniqueId) { mutableSetOf() }

        val added = set.add(group ?: return false)
        if (added) {
            log.info("[audit] Group $group assigned to ${player.uniqueId} (${player.name})")
            saveAndRecalculate(player)
        }

        return added
    }

    /** Removes a player from a group and triggers persistence + recalculation on change. */
    fun removePlayerFromGroup(player: OfflinePlayer, group: String?): Boolean {
        val set = storagePlayerGroups[player.uniqueId]
        var removed = false
        if (set != null) {
            removed = set.remove(group)
        }

        if (removed) {
            log.info("[audit] Group $group removed from ${player.uniqueId} (${player.name})")
            saveAndRecalculate(player)
        }

        return removed
    }
}
