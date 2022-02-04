package org.oddlama.vane.enchantments.items;

import org.oddlama.vane.core.item.CustomItemRegistry;
import org.oddlama.vane.enchantments.Enchantments;

import java.util.function.Function;

public class ItemRegistry extends CustomItemRegistry<Enchantments> {

	// TODO: Lifecycle of Server, plugin, or config reload?
	private AncientTome ancientTome;
	private AncientTomeOfKnowledge ancientTomeOfKnowledge;
	private AncientTomeOfTheGods ancientTomeOfTheGods;

	public ItemRegistry(Enchantments enchantments) {
		ancientTome = this.register(new AncientTome(enchantments));
		ancientTomeOfKnowledge = this.register(new AncientTomeOfKnowledge(enchantments));
		ancientTomeOfTheGods = this.register(new AncientTomeOfTheGods(enchantments));
	}

}
