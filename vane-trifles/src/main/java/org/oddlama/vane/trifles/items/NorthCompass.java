package org.oddlama.vane.trifles.items;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import net.kyori.adventure.key.Key;
import org.oddlama.vane.external.apache.commons.lang3.tuple.Pair;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.oddlama.vane.annotation.item.VaneItem;
import org.oddlama.vane.core.config.recipes.RecipeList;
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition;
import org.oddlama.vane.core.item.CustomItem;
import org.oddlama.vane.core.item.api.InhibitBehavior;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.resourcepack.ResourcePackGenerator;
import org.oddlama.vane.trifles.Trifles;
import org.oddlama.vane.util.StorageUtil;

@VaneItem(name = "north_compass", base = Material.COMPASS, model_data = 0x760013, version = 1)
public class NorthCompass extends CustomItem<Trifles> {

    public NorthCompass(final Context<Trifles> context) {
        super(context);
    }

    @Override
    public RecipeList default_recipes() {
        return RecipeList.of(
            new ShapedRecipeDefinition("generic")
                .shape(" m ", "mrm", " m ")
                .set_ingredient('m', Material.COPPER_INGOT)
                .set_ingredient('r', Material.REDSTONE)
                .result(key().toString())
        );
    }

    @Override
    public ItemStack updateItemStack(final ItemStack item_stack) {
        final var worlds = get_module().getServer().getWorlds();
        if (worlds.size() > 0) {
            final var world = worlds.get(0);
            if (world != null) {
                item_stack.editMeta(CompassMeta.class, meta -> {
                    meta.setLodestone(new Location(world, 0.0, 0.0, -300000000.0));
                    meta.setLodestoneTracked(false);
                });
            }
        }
        return item_stack;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void on_player_click_inventory(final InventoryClickEvent event) {
        final var item = event.getCurrentItem();
        if (item == null || item.getType() != Material.COMPASS) {
            return;
        }

        final var custom_item = get_module().core.item_registry().get(item);
        if (!(custom_item instanceof NorthCompass north_compass) || !north_compass.enabled()) {
            return;
        }

        // FIXME: not very performant to do this on every click, but
        // there aren't many options if we want to support other plugins creating
        // this item. (e.g. to allow giving it to players in kits, shops, ...)
        item.editMeta(CompassMeta.class, meta -> {
            // Only if it isn't already initialized. This allows making different
            // compasses for different worlds. The world in which it is crafted
            // is stored forever.
            if (!meta.hasLodestone()) {
                meta.setLodestoneTracked(false);
                meta.setLodestone(new Location(event.getWhoClicked().getWorld(), 0.0, 0.0, -300000000.0));
            }
        });
    }

    @Override
    public EnumSet<InhibitBehavior> inhibitedBehaviors() {
        return EnumSet.of(InhibitBehavior.USE_IN_VANILLA_RECIPE, InhibitBehavior.USE_OFFHAND);
    }
}
