package org.oddlama.vane.enchantments

import org.oddlama.vane.annotation.VaneModule
import org.oddlama.vane.core.module.Module
import org.oddlama.vane.enchantments.enchantments.*
import org.oddlama.vane.enchantments.items.Tomes

/** The main class for the Enchantments module. */
@VaneModule(
    name = "enchantments",
    bstats = 8640,
    configVersion = 1,
    langVersion = 4,
    storageVersion = 1,
)
class Enchantments : Module<Enchantments?>() {
    /** Initializes the Enchantments module, registering all enchantments and related items. */
    init {
        Tomes(this)

        Angel(this)
        GrapplingHook(this)
        HellBent(this)
        Leafchopper(this)
        Lightning(this)
        Rake(this)
        Seeding(this)
        Soulbound(this)
        TakeOff(this)
        Unbreakable(this)
        Wings(this)
    }
}
