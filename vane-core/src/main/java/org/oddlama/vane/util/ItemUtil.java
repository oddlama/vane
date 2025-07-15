package org.oddlama.vane.util;

import static net.kyori.adventure.text.event.HoverEvent.Action.SHOW_TEXT;
import static org.oddlama.vane.util.Nms.creative_tab_id;
import static org.oddlama.vane.util.Nms.item_handle;
import static org.oddlama.vane.util.Nms.player_handle;

import com.destroystokyo.paper.profile.ProfileProperty;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.world.item.Item;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.oddlama.vane.core.Core;
import org.oddlama.vane.core.material.ExtendedMaterial;

public class ItemUtil {

    private static final UUID SKULL_OWNER = UUID.fromString("00000000-0000-0000-0000-000000000000");

    public static void damage_item(final Player player, final ItemStack item_stack, final int amount) {
        if (player.getGameMode() == GameMode.CREATIVE) { // don't damage the tool if the player is in creative
            return;
        }

        if (amount <= 0) {
            return;
        }

        final var handle = item_handle(item_stack);
        if (handle == null) {
            return;
        }

        handle.hurtAndBreak(amount, Nms.world_handle(player.getWorld()), player_handle(player), item -> {
            player.broadcastSlotBreak(EquipmentSlot.HAND);
            item_stack.subtract();
        });
    }

    public static String name_of(final ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return "";
        }
        final var meta = item.getItemMeta();
        if (!meta.hasDisplayName()) {
            return "";
        }

        return PlainTextComponentSerializer.plainText().serialize(meta.displayName());
    }

    public static ItemStack name_item(final ItemStack item, final Component name) {
        return name_item(item, name, (List<Component>) null);
    }

    public static ItemStack name_item(final ItemStack item, final Component name, Component lore) {
        lore = lore.decoration(TextDecoration.ITALIC, false);
        return name_item(item, name, List.of(lore));
    }

    public static ItemStack set_lore(final ItemStack item, final List<Component> lore) {
        item.editMeta(meta -> {
            final var list = lore
                .stream()
                .map(x -> x.decoration(TextDecoration.ITALIC, false))
                .collect(Collectors.toList());
            meta.lore(list);
        });

        return item;
    }

    public static ItemStack name_item(final ItemStack item, Component name, final List<Component> lore) {
        var meta = item.getItemMeta();
		if (meta == null) {
			// Cannot name item without meta (probably air)
			return item;
		}

        name = name.decoration(TextDecoration.ITALIC, false);
        meta.displayName(name);

        if (lore != null) {
            final var list = lore
                .stream()
                .map(x -> x.decoration(TextDecoration.ITALIC, false))
                .collect(Collectors.toList());
            meta.lore(list);
        }

        item.setItemMeta(meta);
        return item;
    }

    public static int compare_enchantments(final ItemStack item_a, final ItemStack item_b) {
        var ae = item_a.getEnchantments();
        var be = item_b.getEnchantments();

        final var a_meta = item_a.getItemMeta();
        if (a_meta instanceof EnchantmentStorageMeta) {
            final var stored = ((EnchantmentStorageMeta) a_meta).getStoredEnchants();
            if (stored.size() > 0) {
                ae = stored;
            }
        }

        final var b_meta = item_b.getItemMeta();
        if (b_meta instanceof EnchantmentStorageMeta) {
            final var stored = ((EnchantmentStorageMeta) b_meta).getStoredEnchants();
            if (stored.size() > 0) {
                be = stored;
            }
        }

        // Unenchanted first
        final var a_count = ae.size();
        final var b_count = be.size();
        if (a_count == 0 && b_count == 0) {
            return 0;
        } else if (a_count == 0) {
            return -1;
        } else if (b_count == 0) {
            return 1;
        }

        // More enchantments before fewer enchantments
        if (a_count != b_count) {
            return b_count - a_count;
        }

        final var a_sorted = ae
            .entrySet()
            .stream()
            .sorted(
                Map.Entry.<Enchantment, Integer>comparingByKey((a, b) ->
                    a.getKey().toString().compareTo(b.getKey().toString())
                ).thenComparing(Map.Entry.comparingByValue())
            )
            .toList();
        final var b_sorted = be
            .entrySet()
            .stream()
            .sorted(
                Map.Entry.<Enchantment, Integer>comparingByKey((a, b) ->
                    a.getKey().toString().compareTo(b.getKey().toString())
                ).thenComparing(Map.Entry.comparingByValue())
            )
            .toList();

        // Lastly, compare names and levels
        final var ait = a_sorted.iterator();
        final var bit = b_sorted.iterator();

        while (ait.hasNext()) {
            final var a_el = ait.next();
            final var b_el = bit.next();

            // Lexicographic name comparison
            final var name_diff = a_el.getKey().getKey().toString().compareTo(b_el.getKey().getKey().toString());
            if (name_diff != 0) {
                return name_diff;
            }

            // Level
            final int level_diff = b_el.getValue() - a_el.getValue();
            if (level_diff != 0) {
                return level_diff;
            }
        }

        return 0;
    }

    public static class ItemStackComparator implements Comparator<ItemStack> {

        @Override
        public int compare(final ItemStack a, final ItemStack b) {
            if (a == null && b == null) {
                return 0;
            } else if (a == null) {
                return 1;
            } else if (b == null) {
                return -1;
            }

            final var na = item_handle(a);
            final var nb = item_handle(b);
            if (na.isEmpty()) {
                return nb.isEmpty() ? 0 : 1;
            } else if (nb.isEmpty()) {
                return -1;
            }

            // By creative mode tab
            final var creative_mode_tab_diff = creative_tab_id(na) - creative_tab_id(nb);
            if (creative_mode_tab_diff != 0) {
                return creative_mode_tab_diff;
            }

            // By id
            final var id_diff = Item.getId(na.getItem()) - Item.getId(nb.getItem());
            if (id_diff != 0) {
                return id_diff;
            }

            // By damage
            final var damage_diff = na.getDamageValue() - nb.getDamageValue();
            if (damage_diff != 0) {
                return damage_diff;
            }

            // By count
            final var count_diff = nb.getCount() - na.getCount();
            if (count_diff != 0) {
                return count_diff;
            }

            // By enchantments
            return compare_enchantments(a, b);
        }
    }

    public static ItemStack skull_for_player(final OfflinePlayer player, final boolean is_for_menu) {
        final var item = new ItemStack(Material.PLAYER_HEAD);
        if (!is_for_menu || Core.instance().config_player_heads_in_menus) {
            item.editMeta(SkullMeta.class, meta -> meta.setOwningPlayer(player));
        }
        return item;
    }

    public static ItemStack skull_with_texture(final String name, final String base64_texture) {
        final var profile = Bukkit.createProfileExact(SKULL_OWNER, "-");
        profile.setProperty(new ProfileProperty("textures", base64_texture));

        final var item = new ItemStack(Material.PLAYER_HEAD);
        final var meta = (SkullMeta) item.getItemMeta();
        final var name_component = Component.text(name)
            .decoration(TextDecoration.ITALIC, false)
            .color(NamedTextColor.YELLOW);
        meta.displayName(name_component);
        meta.setPlayerProfile(profile);
        item.setItemMeta(meta);
        return item;
    }

    /** Returns true if the given component is guarded by the given sentinel. */
    public static boolean has_sentinel(final Component component, final NamespacedKey sentiel) {
        if (component == null) {
            return false;
        }

        final var hover = component.hoverEvent();
        if (hover == null) {
            return false;
        }

        if (hover.value() instanceof final TextComponent hover_text) {
            return hover.action() == SHOW_TEXT && sentiel.toString().equals(hover_text.content());
        } else {
            return false;
        }
    }

    public static Component add_sentinel(final Component component, final NamespacedKey sentinel) {
        return component.hoverEvent(HoverEvent.showText(Component.text(sentinel.toString())));
    }

    /**
     * Applies enchantments to the item given in the form
     * "{<namespace:enchant>[*<level>][,<namespace:enchant>[*<level>]]...}". Throws
     * IllegalArgumentException if an enchantment cannot be found.
     */
    private static ItemStack apply_enchants(final ItemStack item_stack, @Nullable String enchants) {
        if (enchants == null) {
            return item_stack;
        }

        enchants = enchants.trim();
        if (!enchants.startsWith("{") || !enchants.endsWith("}")) {
            throw new IllegalArgumentException(
                "enchantments must be of form {<namespace:enchant>[*<level>][,<namespace:enchant>[*<level>]]...}"
            );
        }

        final var parts = enchants.substring(1, enchants.length() - 1).split(",");
        for (var part : parts) {
            part = part.trim();

            String key = part;
            int level = 1;
            final int level_delim = key.indexOf('*');
            if (level_delim != -1) {
                level = Integer.parseInt(key.substring(level_delim + 1));
                key = key.substring(0, level_delim);
            }

            final var ench = RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.ENCHANTMENT)
                .get(NamespacedKey.fromString(key));
            if (ench == null) {
                throw new IllegalArgumentException(
                    "Cannot apply unknown enchantment '" + key + "' to item '" + item_stack + "'"
                );
            }

            if (item_stack.getType() == Material.ENCHANTED_BOOK) {
                final var flevel = level;
                item_stack.editMeta(EnchantmentStorageMeta.class, meta -> meta.addStoredEnchant(ench, flevel, false));
            } else {
                item_stack.addEnchantment(ench, level);
            }
        }

        if (parts.length > 0) {
            Core.instance().enchantment_manager.update_enchanted_item(item_stack);
        }
        return item_stack;
    }

    /** Returns the itemstack and a boolean indicating whether it was just as simlpe material. */
    public static @NotNull Pair<ItemStack, Boolean> itemstack_from_string(String definition) {
        // namespace:key[[components]][#enchants{}], where the key can reference a
        // material, head material or customitem.
        final var enchants_delim = definition.indexOf("#enchants{");
        String enchants = null;
        if (enchants_delim != -1) {
            enchants = definition.substring(enchants_delim + 9); // Let it start at '{'
            definition = definition.substring(0, enchants_delim);
        }

        final var nbt_delim = definition.indexOf('[');
        NamespacedKey key;
        if (nbt_delim == -1) {
            key = NamespacedKey.fromString(definition);
        } else {
            key = NamespacedKey.fromString(definition.substring(0, nbt_delim));
        }

        final var emat = ExtendedMaterial.from(key);
        if (emat == null) {
            throw new IllegalArgumentException("Invalid extended material definition: " + definition);
        }

        // First, create the itemstack as if we had no NBT information.
        final var item_stack = emat.item();

        // If there is no NBT information, we can return here.
        if (nbt_delim == -1) {
            return Pair.of(apply_enchants(item_stack, enchants), emat.is_simple_material() && enchants == null);
        }

        // Parse the NBT by using minecraft's internal parser with the base material
        // of whatever the extended material gave us.
        final var vanilla_definition = item_stack.getType().key() + definition.substring(nbt_delim);
        try {
            final var parsed_nbt = new ItemParser(Commands.createValidationContext(VanillaRegistries.createLookup()))
                .parse(new StringReader(vanilla_definition))
                .components();

            // Now apply the NBT be parsed by minecraft's internal parser to the itemstack.
            final var nms_item = item_handle(item_stack).copy();
            nms_item.applyComponents(parsed_nbt);

            return Pair.of(apply_enchants(CraftItemStack.asCraftMirror(nms_item), enchants), false);
        } catch (final CommandSyntaxException e) {
            throw new IllegalArgumentException("Could not parse NBT of item definition: " + definition, e);
        }
    }
}
