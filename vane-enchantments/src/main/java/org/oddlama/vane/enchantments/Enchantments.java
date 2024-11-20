package org.oddlama.vane.enchantments;

import org.oddlama.vane.annotation.VaneModule;
import org.oddlama.vane.core.module.Module;

@VaneModule(name = "enchantments", bstats = 8640, config_version = 1, lang_version = 4, storage_version = 1)
public class Enchantments extends Module<Enchantments> {

    public Enchantments() {
        new org.oddlama.vane.enchantments.items.Tomes(this);

        new org.oddlama.vane.enchantments.enchantments.Angel(this);
        new org.oddlama.vane.enchantments.enchantments.GrapplingHook(this);
        new org.oddlama.vane.enchantments.enchantments.HellBent(this);
        new org.oddlama.vane.enchantments.enchantments.Leafchopper(this);
        new org.oddlama.vane.enchantments.enchantments.Lightning(this);
        new org.oddlama.vane.enchantments.enchantments.Rake(this);
        new org.oddlama.vane.enchantments.enchantments.Seeding(this);
        new org.oddlama.vane.enchantments.enchantments.Soulbound(this);
        new org.oddlama.vane.enchantments.enchantments.TakeOff(this);
        new org.oddlama.vane.enchantments.enchantments.Unbreakable(this);
        new org.oddlama.vane.enchantments.enchantments.Wings(this);
    }
}
