package org.oddlama.vane.enchantments;

import static org.oddlama.vane.util.Util.namespaced_key;
import org.bukkit.enchantments.Enchantment;

import org.oddlama.vane.annotation.VaneModule;
import org.jetbrains.annotations.NotNull;
import org.oddlama.vane.core.module.Module;

@VaneModule(name = "enchantments", bstats = 8640, config_version = 1, lang_version = 1, storage_version = 1)
public class Enchantments extends Module<Enchantments> {
	public Enchantments() {
		try {
			final var accepting = Enchantment.class.getDeclaredField("acceptingNew");
			accepting.setAccessible(true);
			accepting.set(null, true);
		} catch (NoSuchFieldException |	IllegalAccessException e) {
			log.severe("Could not re-enable enchantment registration! Shutting down.");
			getServer().shutdown();
		}

		new org.oddlama.vane.enchantments.enchantments.Rake(this);
	}
}
