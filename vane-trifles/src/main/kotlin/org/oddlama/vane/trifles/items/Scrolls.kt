package org.oddlama.vane.trifles.items

import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.oddlama.vane.annotation.config.ConfigInt
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.item.api.CustomItem
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.trifles.Trifles
import org.oddlama.vane.trifles.event.PlayerTeleportScrollEvent
import org.oddlama.vane.util.ItemUtil
import org.oddlama.vane.util.PlayerUtil
import org.oddlama.vane.util.StorageUtil
import org.oddlama.vane.util.msToTicks

/** Registers concrete scroll items and handles shared scroll interaction behavior. */
class Scrolls(context: Context<Trifles?>) :
    Listener<Trifles?>(
        context.group(
            "Scrolls",
            "Several scrolls that allow player teleportation, and related behavior.",
        )
    ) {
    /** Active scroll item implementations managed by this listener. */
    private val scrolls: MutableSet<Scroll> = HashSet()

    /** Base materials that receive shared cooldown updates. */
    private val baseMaterials: MutableSet<Material> = HashSet()

    /** Cooldown applied after taking damage to discourage combat logging. */
    @ConfigInt(
        def = 15000,
        min = 0,
        desc =
            "A cooldown in milliseconds that is applied when the player takes damage (prevents combat logging). Set to 0 to allow combat logging.",
    )
    /** Cooldown applied after taking damage to discourage combat logging. */
    private val configDamageCooldown = 0

    /** Creates all scroll variants and records their base materials. */
    init {
        val context = requireNotNull(getContext())
        scrolls += HomeScroll(context)
        scrolls += UnstableScroll(context)
        scrolls += SpawnScroll(context)
        scrolls += LodestoneScroll(context)
        scrolls += DeathScroll(context)

        // Track base materials so shared cooldown applies across all scroll variants.
        baseMaterials += scrolls.map { it.baseMaterial() }
    }

    @EventHandler(
        priority = EventPriority.LOW,
        ignoreCancelled = false,
    ) // Keep disabled-cancel filtering to include right-click-air handling.
    /** Handles right-click scroll usage and executes teleport flow when valid. */
    fun onPlayerRightClick(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK && event.action != Action.RIGHT_CLICK_AIR) {
            return
        }

        if (event.useItemInHand() == Event.Result.DENY) {
            return
        }

        // Require a matching enabled custom scroll item.
        val player = event.player
        val hand = event.hand ?: return
        val item = player.equipment.getItem(hand)
        val customItem: CustomItem? = module?.core?.itemRegistry()?.get(item)
        if (customItem !is Scroll || !customItem.enabled()) {
            return
        }

        // Prevent vanilla use of the underlying base item.
        event.setUseItemInHand(Event.Result.DENY)

        when (event.action) {
            Action.RIGHT_CLICK_AIR -> {}
            Action.RIGHT_CLICK_BLOCK ->
                // Trigger only when block interaction has been denied by earlier handlers.
                if (event.useInteractedBlock() != Event.Result.DENY) {
                    return
                }

            else -> return
        }

        val toLocation = customItem.teleportLocation(item, player, true) ?: return

        // Respect per-material cooldown.
        if (player.getCooldown(customItem.baseMaterial()) > 0) {
            return
        }

        val currentLocation = player.location
        if (teleportFromScroll(player, currentLocation, toLocation)) {
            // Apply cooldown to all scroll materials.
            cooldownAll(player, customItem.configCooldown)

            // Consume durability on successful use.
            ItemUtil.damageItem(player, item, 1)
            PlayerUtil.swingArm(player, hand)
        }
    }

    /**
     * Performs a teleport initiated by a scroll and emits associated effects.
     *
     * @return `true` if teleport completed and was not cancelled by listeners.
     */
    fun teleportFromScroll(player: Player, from: Location, to: Location): Boolean {
        val module = module ?: return false

        // Allow external plugins to veto or react to scroll teleports.
        val teleportScrollEvent = PlayerTeleportScrollEvent(player, from, to)
        module.server.pluginManager.callEvent(teleportScrollEvent)
        if (teleportScrollEvent.isCancelled) {
            return false
        }

        // Execute teleport.
        player.teleport(to, PlayerTeleportEvent.TeleportCause.PLUGIN)

        // Store origin for unstable-scroll backtracking.
        StorageUtil.storageSetLocation(
            player.persistentDataContainer,
            UnstableScroll.LAST_SCROLL_TELEPORT_LOCATION,
            from.clone(),
        )

        // Play source and destination teleport sounds.
        from.world.playSound(
            from,
            Sound.ITEM_CHORUS_FRUIT_TELEPORT,
            SoundCategory.PLAYERS,
            1.0f,
            1.1f,
        )
        to.world.playSound(to, Sound.ITEM_CHORUS_FRUIT_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.1f)
        from.world.playSound(
            from,
            Sound.BLOCK_END_PORTAL_FRAME_FILL,
            SoundCategory.PLAYERS,
            1.0f,
            1.0f,
        )
        to.world.playSound(to, Sound.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.PLAYERS, 1.0f, 1.0f)

        // Spawn source and destination particles.
        from.world.spawnParticle(
            Particle.PORTAL,
            from.clone().add(0.0, 1.0, 0.0),
            200,
            1.0,
            2.0,
            1.0,
            1.0,
        )
        to.world.spawnParticle(
            Particle.END_ROD,
            to.clone().add(0.0, 1.0, 0.0),
            100,
            1.0,
            2.0,
            1.0,
            1.0,
        )
        return true
    }

    /** Applies (or increases) cooldown across every tracked scroll base material. */
    fun cooldownAll(player: Player, cooldownMs: Int) {
        val cooldownTicks = msToTicks(cooldownMs.toLong()).toInt()
        for (mat in baseMaterials) {
            // Never reduce an existing longer cooldown.
            if (player.getCooldown(mat) < cooldownTicks) {
                player.setCooldown(mat, cooldownTicks)
            }
        }
    }

    /** Applies combat cooldown when players take damage. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerTakeDamage(event: EntityDamageEvent) {
        val entity = event.entity
        if (entity is Player) {
            cooldownAll(entity, configDamageCooldown)
        }
    }
}
