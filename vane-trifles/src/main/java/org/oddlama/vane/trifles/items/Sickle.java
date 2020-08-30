package org.oddlama.vane.trifles.items;

import org.bukkit.Material;
import static org.oddlama.vane.util.MaterialUtil.is_seeded_plant;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import org.oddlama.vane.annotation.item.VaneItem;
import org.oddlama.vane.core.item.CustomItem;
import org.oddlama.vane.core.item.CustomItemVariant;
import org.oddlama.vane.core.item.ItemVariantEnum;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.trifles.Trifles;

@VaneItem(name = "sickle")
public class Sickle extends CustomItem<Trifles, Sickle> {
	public static enum Variant implements ItemVariantEnum {
		WOODEN,
		STONE,
		IRON(false),
		GOLDEN,
		DIAMOND,
		NETHERITE;

		private boolean enabled;
		private Variant() { this(true); }
		private Variant(boolean enabled) {
			this.enabled = enabled;
		}

		@Override public String prefix() { return name().toLowerCase(); }
		@Override public boolean enabled() { return enabled; }
	}

	public static class SickleVariant extends CustomItemVariant<Trifles, Sickle, Variant> {
		public SickleVariant(Sickle parent, Variant variant) {
			super(parent, variant);
		}
	}

	public Sickle(Context<Trifles> context) {
		super(context, Variant.class, Variant.values(), SickleVariant::new);

		// TODO attack speed and ....

		//((ShapedRecipe)add_recipe(recipe_key(), new ShapedRecipe(recipe_key(), item())))
		//	.shape(" x ", " x ", " x ")
		//	.setIngredient('x', Material.STICK);
		//add_recipe(recipe);
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void on_player_right_click_plant(final PlayerInteractEvent event) {
		if (!event.hasBlock() || event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}

		// Only seed when right clicking a plant
		final var plant_type = event.getClickedBlock().getType();
		if (!is_seeded_plant(plant_type)) {
			return;
		}

		// Get item variant
		final var player = event.getPlayer();
		final var item = player.getEquipment().getItemInMainHand();
		final var variant = this.<SickleVariant>variant_of(item);
		if (variant == null) {
			return;
		}

		System.out.println("interact with " + variant.lang_name + " " + variant.variant());
	}
}
