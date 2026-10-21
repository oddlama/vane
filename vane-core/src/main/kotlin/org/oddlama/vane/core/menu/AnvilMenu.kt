package org.oddlama.vane.core.menu

import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Container
import net.minecraft.world.inventory.AnvilMenu
import net.minecraft.world.inventory.ContainerLevelAccess
import org.bukkit.entity.Player
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.util.Nms.playerHandle

/**
 * Menu implementation backed by a native anvil container.
 *
 * @param context menu context.
 * @param player player the anvil container is bound to.
 * @param title menu title.
 */
class AnvilMenu(context: Context<*>, player: Player, private val title: String) : Menu(context) {
    /** Native server player handle bound to this menu. */
    private val entity: ServerPlayer = requireNotNull(playerHandle(player))

    /** Native container instance backing this menu. */
    private val container: AnvilContainer

    /** Unique container id used for open-screen packets. */
    private val containerId: Int = entity.nextContainerCounter()

    init {
        this.container = AnvilContainer(containerId, entity)
        this.container.setTitle(Component.literal(title))
        this.inventory = container.bukkitView.topInventory
    }

    /** Opens the native anvil window for the bound player. */
    override fun openWindow(player: Player) {
        if (tainted) return

        if (playerHandle(player) !== entity) {
            manager.module!!
                .log
                .warning(
                    "AnvilMenu.open() was called with a player for whom this inventory wasn't created!"
                )
        }

        entity.connection.send(
            ClientboundOpenScreenPacket(containerId, container.type, Component.literal(title))
        )
        entity.initMenu(container)
        entity.containerMenu = container
    }

    /** Native anvil container variant with disabled reach checks and zero repair cost. */
    private inner class AnvilContainer(windowId: Int, entity: ServerPlayer) :
        AnvilMenu(
            windowId,
            entity.inventory,
            ContainerLevelAccess.create(entity.level(), BlockPos(0, 0, 0)),
        ) {
        init {
            this.checkReachable = false
        }

        /** Recomputes anvil output while forcing zero cost. */
        override fun createResult() {
            super.createResult()
            this.cost.set(0)
        }

        /** Suppresses default container removal behavior. */
        override fun removed(player: net.minecraft.world.entity.player.Player) {}

        /** Suppresses default clear behavior for this temporary container. */
        override fun clearContainer(
            player: net.minecraft.world.entity.player.Player,
            container: Container,
        ) {}
    }
}
