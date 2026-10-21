package org.oddlama.vane.permissions.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import java.util.*
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.bukkit.permissions.PermissionDefault
import org.oddlama.vane.annotation.command.Aliases
import org.oddlama.vane.annotation.command.Name
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.core.command.argumentType.OfflinePlayerArgumentType
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.permissions.Permissions
import org.oddlama.vane.permissions.argumentTypes.PermissionGroupArgumentType

@Name("permission")
@Aliases("perm")
/**
 * Command suite for listing permission data and assigning/removing permission groups.
 *
 * @param context command context bound to the permissions module.
 */
class Permission(context: Context<Permissions?>) :
    org.oddlama.vane.core.command.Command<Permissions?>(context) {
    /** Non-null typed accessor for the backing permissions module. */
    private val permissionsModule: Permissions
        get() = requireNotNull(module)

    /** Message shown when no entries are available for a list command. */
    @LangMessage private val langListEmpty: TranslatedMessage? = null

    /** Header shown when listing all configured groups. */
    @LangMessage private val langListHeaderGroups: TranslatedMessage? = null

    /** Header shown when listing all registered permissions. */
    @LangMessage private val langListHeaderPermissions: TranslatedMessage? = null

    /** Header shown when listing groups assigned to a specific player. */
    @LangMessage private val langListHeaderPlayerGroups: TranslatedMessage? = null

    /** Header shown when listing effective permissions for a specific player. */
    @LangMessage private val langListHeaderPlayerPermissions: TranslatedMessage? = null

    /** Header shown when listing permissions contained in a specific group. */
    @LangMessage private val langListHeaderGroupPermissions: TranslatedMessage? = null

    /** Notice shown when player-specific permission details are unavailable offline. */
    @LangMessage private val langListPlayerOffline: TranslatedMessage? = null

    /** Row format for a listed permission group. */
    @LangMessage private val langListGroup: TranslatedMessage? = null

    /** Row format for a listed permission and its metadata. */
    @LangMessage private val langListPermission: TranslatedMessage? = null

    /** Confirmation message for successful group assignment. */
    @LangMessage private val langGroupAssigned: TranslatedMessage? = null

    /** Confirmation message for successful group removal. */
    @LangMessage private val langGroupRemoved: TranslatedMessage? = null

    /** Error message when assigning a group already held by the player. */
    @LangMessage private val langGroupAlreadyAssigned: TranslatedMessage? = null

    /** Error message when removing a group not held by the player. */
    @LangMessage private val langGroupNotAssigned: TranslatedMessage? = null

    /** Builds the command tree for permission inspection and group management. */
    override fun getCommandBase(): LiteralArgumentBuilder<CommandSourceStack> {
        return super.getCommandBase()
            .then(help())
            .then(
                Commands.literal("list")
                    .then(
                        Commands.literal("groups")
                            .executes { ctx ->
                                listGroups(ctx.source.sender)
                                Command.SINGLE_SUCCESS
                            }
                            .then(
                                Commands.argument<OfflinePlayer>(
                                        "offlinePlayer",
                                        OfflinePlayerArgumentType.offlinePlayer(),
                                    )
                                    .executes { ctx ->
                                        listGroupsForPlayer(ctx.source.sender, offlinePlayer(ctx))
                                        Command.SINGLE_SUCCESS
                                    }
                            )
                    )
                    .then(
                        Commands.literal("permissions")
                            .then(
                                Commands.argument<String>(
                                        "permissionGroup",
                                        PermissionGroupArgumentType.permissionGroup(
                                            permissionsModule.permissionGroups
                                        ),
                                    )
                                    .executes { ctx ->
                                        listPermissionsForGroup(
                                            ctx.source.sender,
                                            permissionGroup(ctx),
                                        )
                                        Command.SINGLE_SUCCESS
                                    }
                            )
                            .then(
                                Commands.argument<OfflinePlayer>(
                                        "offlinePlayer",
                                        OfflinePlayerArgumentType.offlinePlayer(),
                                    )
                                    .executes { ctx ->
                                        listPermissionsForPlayer(
                                            ctx.source.sender,
                                            offlinePlayer(ctx),
                                        )
                                        Command.SINGLE_SUCCESS
                                    }
                            )
                            .executes { ctx ->
                                listPermissions(ctx.source.sender)
                                Command.SINGLE_SUCCESS
                            }
                    )
            )
            .then(
                Commands.literal("add")
                    .then(
                        Commands.argument<OfflinePlayer>(
                                "offlinePlayer",
                                OfflinePlayerArgumentType.offlinePlayer(),
                            )
                            .then(
                                Commands.argument<String>(
                                        "permissionGroup",
                                        PermissionGroupArgumentType.permissionGroup(
                                            permissionsModule.permissionGroups
                                        ),
                                    )
                                    .executes { ctx ->
                                        addPlayerToGroup(
                                            ctx.source.sender,
                                            offlinePlayer(ctx),
                                            permissionGroup(ctx),
                                        )
                                        Command.SINGLE_SUCCESS
                                    }
                            )
                    )
            )
            .then(
                Commands.literal("remove")
                    .then(
                        Commands.argument<OfflinePlayer>(
                                "offlinePlayer",
                                OfflinePlayerArgumentType.offlinePlayer(),
                            )
                            .then(
                                Commands.argument<String>(
                                        "permissionGroup",
                                        PermissionGroupArgumentType.permissionGroup(
                                            permissionsModule.permissionGroups
                                        ),
                                    )
                                    .executes { ctx ->
                                        removePlayerFromGroup(
                                            ctx.source.sender,
                                            offlinePlayer(ctx),
                                            permissionGroup(ctx),
                                        )
                                        Command.SINGLE_SUCCESS
                                    }
                            )
                    )
            )
    }

    /** Returns the resolved permission-group argument. */
    private fun permissionGroup(ctx: CommandContext<CommandSourceStack>): String =
        ctx.getArgument("permissionGroup", String::class.java)

    /** Returns the resolved offline-player argument. */
    private fun offlinePlayer(ctx: CommandContext<CommandSourceStack>): OfflinePlayer =
        ctx.getArgument("offlinePlayer", OfflinePlayer::class.java)

    /** Returns color prefix for the configured default value. */
    private fun permissionDefaultValueColorCode(def: PermissionDefault): String {
        return when (def) {
            PermissionDefault.FALSE -> "§c"
            PermissionDefault.NOT_OP -> "§5"
            PermissionDefault.OP -> "§b"
            PermissionDefault.TRUE -> "§a"
        }
    }

    /** Returns color prefix for a boolean permission state. */
    private fun permissionValueColorCode(value: Boolean): String {
        return permissionDefaultValueColorCode(
            if (value) PermissionDefault.TRUE else PermissionDefault.FALSE
        )
    }

    /** Lists configured permission groups. */
    private fun listGroups(sender: CommandSender?) {
        langListHeaderGroups!!.send(sender)
        permissionsModule.permissionGroups.keys.sorted().forEach { group ->
            langListGroup!!.send(sender, "§b$group")
        }
    }

    /** Lists all registered permissions sorted by name. */
    private fun listPermissions(sender: CommandSender?) {
        langListHeaderPermissions!!.send(sender)
        permissionsModule.server.pluginManager.permissions
            .sortedBy { it.name }
            .forEach { perm ->
                langListPermission!!.send(
                    sender,
                    "§d${perm.name}",
                    permissionDefaultValueColorCode(perm.default) +
                        perm.default.toString().lowercase(Locale.getDefault()),
                    perm.description,
                )
            }
    }

    /** Lists effective permissions for an offline or online player. */
    private fun listPermissionsForPlayer(sender: CommandSender?, offlinePlayer: OfflinePlayer) {
        langListHeaderPlayerPermissions!!.send(sender, "§b${offlinePlayer.name}")
        val player = offlinePlayer.player
        if (player == null) {
            langListPlayerOffline!!.send(sender)
            val groups = permissionsModule.storagePlayerGroups[offlinePlayer.uniqueId]
            if (groups == null) {
                langListEmpty!!.send(sender)
            } else {
                for (group in groups) {
                    listPermissionsForGroupNoHeader(sender, group)
                }
            }
        } else {
            val effectivePermissions = player.effectivePermissions
            if (effectivePermissions.isEmpty()) {
                langListEmpty!!.send(sender)
            } else {
                player.effectivePermissions
                    .sortedBy { it.permission }
                    .forEach { att ->
                        val perm =
                            permissionsModule.server.pluginManager.getPermission(att.permission)
                        if (perm == null) {
                            permissionsModule.log.warning(
                                "Encountered unregistered permission '${att.permission}'"
                            )
                            return@forEach
                        }
                        langListPermission!!.send(
                            sender,
                            "§d${perm.name}",
                            permissionValueColorCode(att.value) + att.value,
                            perm.description,
                        )
                    }
            }
        }
    }

    /** Lists permissions contained in a group without a header message. */
    private fun listPermissionsForGroupNoHeader(sender: CommandSender?, group: String) {
        for (permission in permissionsModule.permissionGroups[group].orEmpty()) {
            val perm = permissionsModule.server.pluginManager.getPermission(permission)
            if (perm == null) {
                permissionsModule.log.warning(
                    "Use of unregistered permission '$permission' might have unintended effects."
                )
                langListPermission!!.send(
                    sender,
                    "§d$permission",
                    permissionValueColorCode(true) + true,
                    "",
                )
            } else {
                langListPermission!!.send(
                    sender,
                    "§d${perm.name}",
                    permissionValueColorCode(true) + true,
                    perm.description,
                )
            }
        }
    }

    /** Lists permissions contained in a specific group. */
    private fun listPermissionsForGroup(sender: CommandSender?, group: String) {
        langListHeaderGroupPermissions!!.send(sender, "§b$group")
        listPermissionsForGroupNoHeader(sender, group)
    }

    /** Lists configured groups for a player. */
    private fun listGroupsForPlayer(sender: CommandSender?, offlinePlayer: OfflinePlayer) {
        val set = permissionsModule.storagePlayerGroups[offlinePlayer.uniqueId]
        if (set == null) {
            langListEmpty!!.send(sender)
        } else {
            langListHeaderPlayerGroups!!.send(sender, "§b${offlinePlayer.name}")
            for (group in set) {
                langListGroup!!.send(sender, group)
            }
        }
    }

    /** Adds a player to the specified group and sends localized feedback. */
    private fun addPlayerToGroup(sender: CommandSender?, player: OfflinePlayer, group: String) {
        if (permissionsModule.addPlayerToGroup(player, group)) {
            langGroupAssigned!!.send(sender, "§b${player.name}", "§a$group")
        } else {
            langGroupAlreadyAssigned!!.send(sender, "§b${player.name}", "§a$group")
        }
    }

    /** Removes a player from the specified group and sends localized feedback. */
    private fun removePlayerFromGroup(
        sender: CommandSender?,
        player: OfflinePlayer,
        group: String,
    ) {
        if (permissionsModule.removePlayerFromGroup(player, group)) {
            langGroupRemoved!!.send(sender, "§b${player.name}", "§a$group")
        } else {
            langGroupNotAssigned!!.send(sender, "§b${player.name}", "§a$group")
        }
    }
}
