package org.oddlama.vane.trifles.items;

import static org.oddlama.vane.util.PlayerUtil.swing_arm;

import java.util.List;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.oddlama.vane.annotation.item.VaneItem;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.config.recipes.RecipeList;
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.trifles.Trifles;
import org.oddlama.vane.util.StorageUtil;

@VaneItem(
    name = "lodestone_scroll",
    base = Material.WARPED_FUNGUS_ON_A_STICK,
    durability = 15,
    model_data = 0x760011,
    version = 1
)
public class LodestoneScroll extends Scroll {

    public static final NamespacedKey LODESTONE_LOCATION = StorageUtil.namespaced_key("vane", "lodestone_location");

    @LangMessage
    public TranslatedMessage lang_teleport_no_bound_lodestone;

    @LangMessage
    public TranslatedMessage lang_teleport_missing_lodestone;

    @LangMessage
    public TranslatedMessage lang_bound_lore;

    public LodestoneScroll(Context<Trifles> context) {
        super(context, 6000);
    }

    @Override
    public RecipeList default_recipes() {
        return RecipeList.of(
            new ShapedRecipeDefinition("generic")
                .shape("aba", "epe")
                .set_ingredient('p', "vane_trifles:papyrus_scroll")
                .set_ingredient('e', Material.ENDER_PEARL)
                .set_ingredient('a', Material.AMETHYST_SHARD)
                .set_ingredient('b', Material.NETHERITE_INGOT)
                .result(key().toString())
        );
    }

    private Location get_lodestone_location(final ItemStack scroll) {
        if (!scroll.hasItemMeta()) {
            return null;
        }
        return StorageUtil.storage_get_location(
            scroll.getItemMeta().getPersistentDataContainer(),
            LODESTONE_LOCATION,
            null
        );
    }

    @Override
    public Location teleport_location(final ItemStack scroll, Player player, boolean imminent_teleport) {
        // This scroll cannot be used while sneaking to allow re-binding
        if (player.isSneaking()) {
            return null;
        }

        final var lodestone_location = get_lodestone_location(scroll);
        var lodestone = lodestone_location == null ? null : lodestone_location.getBlock();

        if (imminent_teleport) {
            if (lodestone_location == null) {
                lang_teleport_no_bound_lodestone.send_action_bar(player);
            } else if (lodestone.getType() != Material.LODESTONE) {
                lang_teleport_missing_lodestone.send_action_bar(player);
                lodestone = null;
            }
        }

        return lodestone == null ? null : lodestone.getLocation().add(0.5, 1.005, 0.5);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void on_player_interact(final PlayerInteractEvent event) {
        // Skip if no block clicked or the item is allowed to be used (e.g., torches in offhand)
        if (
            !event.hasBlock() ||
            event.getAction() != Action.RIGHT_CLICK_BLOCK ||
            event.useItemInHand() == Event.Result.ALLOW
        ) {
            return;
        }

        final var block = event.getClickedBlock();
        if (block.getType() != Material.LODESTONE) {
            return;
        }

        // Only if player sneak-right-clicks
        final var player = event.getPlayer();
        if (!player.isSneaking()) {
            return;
        }

        // With a lodestone scroll in the main hand
        final var item = player.getEquipment().getItem(EquipmentSlot.HAND);
        final var custom_item = get_module().core.item_registry().get(item);
        if (!(custom_item instanceof LodestoneScroll scroll) || !scroll.enabled()) {
            return;
        }

        // Save lodestone location
        item.editMeta(meta -> {
            StorageUtil.storage_set_location(
                meta.getPersistentDataContainer(),
                LODESTONE_LOCATION,
                block.getLocation().add(0.5, 0.5, 0.5)
            );
            meta.lore(
                List.of(
                    lang_bound_lore
                        .format(
                            "§a" + block.getWorld().getName(),
                            "§b" + block.getX(),
                            "§b" + block.getY(),
                            "§b" + block.getZ()
                        )
                        .decoration(TextDecoration.ITALIC, false)
                )
            );
        });

        // Effects and sound
        swing_arm(player, event.getHand());
        block
            .getWorld()
            .spawnParticle(Particle.ENCHANT, block.getLocation().add(0.5, 2.0, 0.5), 100, 0.1, 0.3, 0.1, 2.0);
        block
            .getWorld()
            .playSound(block.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.BLOCKS, 1.0f, 3.0f);

        // Prevent offhand from triggering (e.g., placing torches)
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);
    }
}
