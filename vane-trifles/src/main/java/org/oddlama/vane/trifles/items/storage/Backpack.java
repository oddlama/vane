package org.oddlama.vane.trifles.items.storage;

import static org.oddlama.vane.util.PlayerUtil.swing_arm;

import java.util.EnumSet;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.oddlama.vane.annotation.item.VaneItem;
import org.oddlama.vane.core.config.recipes.RecipeList;
import org.oddlama.vane.core.config.recipes.SmithingRecipeDefinition;
import org.oddlama.vane.core.item.CustomItem;
import org.oddlama.vane.core.item.api.InhibitBehavior;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.trifles.Trifles;

@VaneItem(name = "backpack", base = Material.SHULKER_BOX, model_data = 0x760017, version = 1)
public class Backpack extends CustomItem<Trifles> {

    public Backpack(Context<Trifles> context) {
        super(context);
    }

    @Override
    public RecipeList default_recipes() {
        return RecipeList.of(
            new SmithingRecipeDefinition("from_shulker_box")
                .base(Tag.SHULKER_BOXES)
                .addition(Material.LEATHER_CHESTPLATE)
                .copy_nbt(true)
                .result(key().toString())
        );
    }

    // ignoreCancelled = false to catch right-click-air events
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void on_player_right_click(final PlayerInteractEvent event) {
        if (!event.hasItem() || event.useItemInHand() == Event.Result.DENY) {
            return;
        }

        // Any right click to open
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }

        // Assert this is a matching custom item
        final var player = event.getPlayer();
        final var item = player.getEquipment().getItem(event.getHand());
        final var custom_item = get_module().core.item_registry().get(item);
        if (!(custom_item instanceof Backpack backpack) || !backpack.enabled()) {
            return;
        }

        // Never use anything else (e.g., offhand)
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);

        if (get_module().storage_group.open_block_state_inventory(player, item)) {
            player.getWorld().playSound(player, Sound.ITEM_BUNDLE_DROP_CONTENTS, 1.0f, 1.2f);
            swing_arm(player, event.getHand());
        }
    }

    @Override
    public EnumSet<InhibitBehavior> inhibitedBehaviors() {
        return EnumSet.of(InhibitBehavior.USE_IN_VANILLA_RECIPE, InhibitBehavior.DISPENSE);
    }
}
