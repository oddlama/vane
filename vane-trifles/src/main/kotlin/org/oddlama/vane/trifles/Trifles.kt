package org.oddlama.vane.trifles

import java.util.*
import org.bukkit.plugin.Plugin
import org.oddlama.vane.annotation.VaneModule
import org.oddlama.vane.core.module.Module
import org.oddlama.vane.trifles.commands.Finditem
import org.oddlama.vane.trifles.commands.Heads
import org.oddlama.vane.trifles.commands.Setspawn
import org.oddlama.vane.trifles.items.*
import org.oddlama.vane.trifles.items.storage.Backpack
import org.oddlama.vane.trifles.items.storage.Pouch

@VaneModule(name = "trifles", bstats = 8644, configVersion = 4, langVersion = 4, storageVersion = 1)
/** Main entry point for the `vane-trifles` module. */
class Trifles : Module<Trifles?>() {
    /** Tracks the latest XP bottle consume timestamp per player UUID. */
    val lastXpBottleConsumeTime: MutableMap<UUID, Long> = mutableMapOf()

    /** Registry holder for XP bottle custom items. */
    var xpBottles: XpBottles?

    /** Listener and search backend for the item finder feature. */
    var itemFinder: ItemFinder?

    /** Shared storage-item behavior group. */
    var storageGroup: StorageGroup

    /** Indicates whether PacketEvents integration is available and enabled. */
    var packetEventsEnabled: Boolean = false

    init {
        val fastWalkingGroup = FastWalkingGroup(this)
        FastWalkingListener(fastWalkingGroup)
        DoubleDoorListener(this)
        ItemFrameListener(this)
        HarvestListener(this)
        RepairCostLimiter(this)
        RecipeUnlock(this)
        ChestSorter(this)
        itemFinder = ItemFinder(this)

        Heads(this)
        Setspawn(this)
        Finditem(this)

        PapyrusScroll(this)
        Scrolls(this)
        ReinforcedElytra(this)
        File(this)
        Sickles(this)
        EmptyXpBottle(this)
        xpBottles = XpBottles(this)
        Trowel(this)
        NorthCompass(this)
        SlimeBucket(this)

        storageGroup = StorageGroup(this)
        val storageContext = requireNotNull(storageGroup.getContext())
        Pouch(storageContext)
        Backpack(storageContext)
    }

    /** Enables optional integrations after the server has fully initialized plugins. */
    override fun onModuleEnable() {
        val module = module ?: return
        module.scheduleNextTick {
            val packetEventsPlugin: Plugin? = module.server.pluginManager.getPlugin("PacketEvents")
            if (packetEventsPlugin?.isEnabled == true) {
                packetEventsEnabled = true
                module.log.info("Enabling PacketEvents integration")
            }
        }
    }
}
