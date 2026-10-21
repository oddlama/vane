package org.oddlama.vane.geyserextension

import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomItemsEvent
import org.geysermc.geyser.api.item.custom.v2.CustomItemBedrockOptions
import org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition
import org.geysermc.geyser.api.item.custom.v2.component.java.JavaItemDataComponents
import org.geysermc.geyser.api.predicate.item.ItemConditionPredicate
import org.geysermc.geyser.api.util.Identifier

/**
 * Registers all Vane custom item definitions with the Geyser item translation layer.
 *
 * Each custom item is mapped from its Java Edition base item to a Bedrock-compatible
 * [CustomItemDefinition] that references the corresponding resource pack model and icon. This
 * allows Bedrock clients to see the correct textures and models for vane's custom items.
 *
 * Registered item categories:
 * - **Enchantment Tomes** — Ancient tomes and their enchanted variants (`book` / `enchanted_book`).
 * - **Sickles** — Tiered sickles mapped to their respective hoe base items.
 * - **Containers** — Pouch (`dropper`) and Backpack (`shulker_box`).
 * - **Slime Buckets** — Normal and excited variants (`slime_ball`).
 * - **Reinforced Elytra** — Includes broken/unbroken predicates and armor values (`elytra`).
 * - **Navigation Items** — North compass (`compass`), papyrus scroll (`paper`).
 * - **Scrolls & Tools** — Home, unstable, spawn, lodestone, death scrolls, file, and trowel
 *   (`warped_fungus_on_a_stick`).
 * - **XP Bottles** — Empty (`glass_bottle`) and small/medium/large (`honey_bottle`).
 */
object ItemRegistration {

    /**
     * Registers all Vane custom items for Bedrock clients.
     *
     * Called during the [GeyserDefineCustomItemsEvent] lifecycle event. Each registration maps a
     * Java base item [Identifier] to a [CustomItemDefinition] containing:
     * - A model identifier referencing the resource pack model.
     * - An item identifier for the custom vane item.
     * - Bedrock-specific options such as icon paths, protection values, and max damage.
     *
     * @param event the Geyser event used to register custom item definitions.
     */
    fun onGeyserDefineCustomItems(event: GeyserDefineCustomItemsEvent) {
        // --- Enchantment Tomes ---
        // Non-enchanted tomes use the normal book item, enchanted variants use enchanted_book.

        // Ancient Tome (normal book)
        event.register(
            Identifier.of("book"),
            CustomItemDefinition.builder(
                    Identifier.of("vane_enchantments:item/ancient_tome"),
                    Identifier.of("vane_enchantments:ancient_tome"),
                )
                .bedrockOptions(
                    CustomItemBedrockOptions.builder().icon("vane_enchantments.item_ancient_tome")
                )
                .build(),
        )

        // Enchanted Ancient Tome (enchanted_book)
        event.register(
            Identifier.of("enchanted_book"),
            CustomItemDefinition.builder(
                    Identifier.of("vane_enchantments:item/enchanted_ancient_tome"),
                    Identifier.of("vane_enchantments:enchanted_ancient_tome"),
                )
                .bedrockOptions(
                    CustomItemBedrockOptions.builder()
                        .icon("vane_enchantments.item_enchanted_ancient_tome")
                )
                .build(),
        )

        // Ancient Tome of Knowledge (normal book)
        event.register(
            Identifier.of("book"),
            CustomItemDefinition.builder(
                    Identifier.of("vane_enchantments:item/ancient_tome_of_knowledge"),
                    Identifier.of("vane_enchantments:ancient_tome_of_knowledge"),
                )
                .bedrockOptions(
                    CustomItemBedrockOptions.builder()
                        .icon("vane_enchantments.item_ancient_tome_of_knowledge")
                )
                .build(),
        )

        // Enchanted Ancient Tome of Knowledge (enchanted_book)
        event.register(
            Identifier.of("enchanted_book"),
            CustomItemDefinition.builder(
                    Identifier.of("vane_enchantments:item/enchanted_ancient_tome_of_knowledge"),
                    Identifier.of("vane_enchantments:enchanted_ancient_tome_of_knowledge"),
                )
                .bedrockOptions(
                    CustomItemBedrockOptions.builder()
                        .icon("vane_enchantments.item_enchanted_ancient_tome_of_knowledge")
                )
                .build(),
        )

        // Ancient Tome of the Gods (normal book)
        event.register(
            Identifier.of("book"),
            CustomItemDefinition.builder(
                    Identifier.of("vane_enchantments:item/ancient_tome_of_the_gods"),
                    Identifier.of("vane_enchantments:ancient_tome_of_the_gods"),
                )
                .bedrockOptions(
                    CustomItemBedrockOptions.builder()
                        .icon("vane_enchantments.item_ancient_tome_of_the_gods")
                )
                .build(),
        )

        // Enchanted Ancient Tome of the Gods (enchanted_book)
        event.register(
            Identifier.of("enchanted_book"),
            CustomItemDefinition.builder(
                    Identifier.of("vane_enchantments:item/enchanted_ancient_tome_of_the_gods"),
                    Identifier.of("vane_enchantments:enchanted_ancient_tome_of_the_gods"),
                )
                .bedrockOptions(
                    CustomItemBedrockOptions.builder()
                        .icon("vane_enchantments.item_enchanted_ancient_tome_of_the_gods")
                )
                .build(),
        )

        // ----- Vane Trifles items -----
        // Sickles (mapped to the corresponding hoe base item)
        event.register(
            Identifier.of("wooden_hoe"),
            CustomItemDefinition.builder(
                    Identifier.of("vane_trifles:item/wooden_sickle"),
                    Identifier.of("vane_trifles:wooden_sickle"),
                )
                .bedrockOptions(
                    CustomItemBedrockOptions.builder().icon("vane_trifles.item_wooden_sickle")
                )
                .build(),
        )

        event.register(
            Identifier.of("stone_hoe"),
            CustomItemDefinition.builder(
                    Identifier.of("vane_trifles:item/stone_sickle"),
                    Identifier.of("vane_trifles:stone_sickle"),
                )
                .bedrockOptions(
                    CustomItemBedrockOptions.builder().icon("vane_trifles.item_stone_sickle")
                )
                .build(),
        )

        event.register(
            Identifier.of("iron_hoe"),
            CustomItemDefinition.builder(
                    Identifier.of("vane_trifles:item/iron_sickle"),
                    Identifier.of("vane_trifles:iron_sickle"),
                )
                .bedrockOptions(
                    CustomItemBedrockOptions.builder().icon("vane_trifles.item_iron_sickle")
                )
                .build(),
        )

        event.register(
            Identifier.of("golden_hoe"),
            CustomItemDefinition.builder(
                    Identifier.of("vane_trifles:item/golden_sickle"),
                    Identifier.of("vane_trifles:golden_sickle"),
                )
                .bedrockOptions(
                    CustomItemBedrockOptions.builder().icon("vane_trifles.item_golden_sickle")
                )
                .build(),
        )

        event.register(
            Identifier.of("diamond_hoe"),
            CustomItemDefinition.builder(
                    Identifier.of("vane_trifles:item/diamond_sickle"),
                    Identifier.of("vane_trifles:diamond_sickle"),
                )
                .bedrockOptions(
                    CustomItemBedrockOptions.builder().icon("vane_trifles.item_diamond_sickle")
                )
                .build(),
        )

        event.register(
            Identifier.of("netherite_hoe"),
            CustomItemDefinition.builder(
                    Identifier.of("vane_trifles:item/netherite_sickle"),
                    Identifier.of("vane_trifles:netherite_sickle"),
                )
                .bedrockOptions(
                    CustomItemBedrockOptions.builder().icon("vane_trifles.item_netherite_sickle")
                )
                .build(),
        )

        // Pouch (mapped to dropper)
        event.register(
            Identifier.of("dropper"),
            CustomItemDefinition.builder(
                    Identifier.of("vane_trifles:item/pouch"),
                    Identifier.of("vane_trifles:pouch"),
                )
                .bedrockOptions(CustomItemBedrockOptions.builder().icon("vane_trifles.item_pouch"))
                .build(),
        )

        // Backpack (mapped to shulker_box)
        event.register(
            Identifier.of("shulker_box"),
            CustomItemDefinition.builder(
                    Identifier.of("vane_trifles:item/backpack"),
                    Identifier.of("vane_trifles:backpack"),
                )
                .bedrockOptions(
                    CustomItemBedrockOptions.builder().icon("vane_trifles.item_backpack")
                )
                .build(),
        )

        // Slime buckets (mapped to slime_ball)
        event.register(
            Identifier.of("slime_ball"),
            CustomItemDefinition.builder(
                    Identifier.of("vane_trifles:item/slime_bucket"),
                    Identifier.of("vane_trifles:slime_bucket"),
                )
                .bedrockOptions(
                    CustomItemBedrockOptions.builder().icon("vane_trifles.item_slime_bucket")
                )
                .build(),
        )

        event.register(
            Identifier.of("slime_ball"),
            CustomItemDefinition.builder(
                    Identifier.of("vane_trifles:item/slime_bucket_excited"),
                    Identifier.of("vane_trifles:slime_bucket_excited"),
                )
                .bedrockOptions(
                    CustomItemBedrockOptions.builder()
                        .icon("vane_trifles.item_slime_bucket_excited")
                )
                .build(),
        )

        // Reinforced Elytra (mapped to elytra)
        event.register(
            Identifier.of("elytra"),
            CustomItemDefinition.builder(
                    Identifier.of("vane_trifles:item/reinforced_elytra"),
                    Identifier.of("vane_trifles:reinforced_elytra"),
                )
                .bedrockOptions(
                    CustomItemBedrockOptions.builder()
                        .icon("vane_trifles.item_reinforced_elytra")
                        .protectionValue(6)
                )
                .component(JavaItemDataComponents.MAX_DAMAGE, 864)
                .predicate(ItemConditionPredicate.BROKEN.negate())
                .build(),
        )

        event.register(
            Identifier.of("elytra"),
            CustomItemDefinition.builder(
                    Identifier.of("vane_trifles:item/reinforced_elytra.broken"),
                    Identifier.of("vane_trifles:reinforced_elytra"),
                )
                .bedrockOptions(
                    CustomItemBedrockOptions.builder()
                        .icon("vane_trifles.item_reinforced_elytra.broken")
                        .protectionValue(6)
                )
                .component(JavaItemDataComponents.MAX_DAMAGE, 864)
                .predicate(ItemConditionPredicate.BROKEN)
                .build(),
        )

        // Compass variant (north compass)
        event.register(
            Identifier.of("compass"),
            CustomItemDefinition.builder(
                    Identifier.of("vane_trifles:item/north_compass_16"),
                    Identifier.of("vane_trifles:north_compass"),
                )
                .bedrockOptions(
                    CustomItemBedrockOptions.builder().icon("vane_trifles.item_north_compass_16")
                )
                .build(),
        )

        // Papyrus scroll (paper)
        event.register(
            Identifier.of("paper"),
            CustomItemDefinition.builder(
                    Identifier.of("vane_trifles:item/papyrus_scroll"),
                    Identifier.of("vane_trifles:papyrus_scroll"),
                )
                .bedrockOptions(
                    CustomItemBedrockOptions.builder().icon("vane_trifles.item_papyrus_scroll")
                )
                .build(),
        )

        // Parchment / home/unstable/file/trowel/spawn/lodestone/death scrolls
        // (warped_fungus_on_a_stick)
        event.register(
            Identifier.of("warped_fungus_on_a_stick"),
            CustomItemDefinition.builder(
                    Identifier.of("vane_trifles:item/home_scroll"),
                    Identifier.of("vane_trifles:home_scroll"),
                )
                .bedrockOptions(
                    CustomItemBedrockOptions.builder().icon("vane_trifles.item_home_scroll")
                )
                .component(JavaItemDataComponents.MAX_DAMAGE, 25)
                .build(),
        )
        event.register(
            Identifier.of("warped_fungus_on_a_stick"),
            CustomItemDefinition.builder(
                    Identifier.of("vane_trifles:item/unstable_scroll"),
                    Identifier.of("vane_trifles:unstable_scroll"),
                )
                .bedrockOptions(
                    CustomItemBedrockOptions.builder().icon("vane_trifles.item_unstable_scroll")
                )
                .component(JavaItemDataComponents.MAX_DAMAGE, 25)
                .build(),
        )
        event.register(
            Identifier.of("warped_fungus_on_a_stick"),
            CustomItemDefinition.builder(
                    Identifier.of("vane_trifles:item/file"),
                    Identifier.of("vane_trifles:file"),
                )
                .bedrockOptions(CustomItemBedrockOptions.builder().icon("vane_trifles.item_file"))
                .component(JavaItemDataComponents.MAX_DAMAGE, 4000)
                .build(),
        )
        event.register(
            Identifier.of("warped_fungus_on_a_stick"),
            CustomItemDefinition.builder(
                    Identifier.of("vane_trifles:item/trowel"),
                    Identifier.of("vane_trifles:trowel"),
                )
                .bedrockOptions(CustomItemBedrockOptions.builder().icon("vane_trifles.item_trowel"))
                .component(JavaItemDataComponents.MAX_DAMAGE, 800)
                .build(),
        )
        event.register(
            Identifier.of("warped_fungus_on_a_stick"),
            CustomItemDefinition.builder(
                    Identifier.of("vane_trifles:item/spawn_scroll"),
                    Identifier.of("vane_trifles:spawn_scroll"),
                )
                .bedrockOptions(
                    CustomItemBedrockOptions.builder().icon("vane_trifles.item_spawn_scroll")
                )
                .component(JavaItemDataComponents.MAX_DAMAGE, 40)
                .build(),
        )
        event.register(
            Identifier.of("warped_fungus_on_a_stick"),
            CustomItemDefinition.builder(
                    Identifier.of("vane_trifles:item/lodestone_scroll"),
                    Identifier.of("vane_trifles:lodestone_scroll"),
                )
                .bedrockOptions(
                    CustomItemBedrockOptions.builder().icon("vane_trifles.item_lodestone_scroll")
                )
                .component(JavaItemDataComponents.MAX_DAMAGE, 15)
                .build(),
        )
        event.register(
            Identifier.of("warped_fungus_on_a_stick"),
            CustomItemDefinition.builder(
                    Identifier.of("vane_trifles:item/death_scroll"),
                    Identifier.of("vane_trifles:death_scroll"),
                )
                .bedrockOptions(
                    CustomItemBedrockOptions.builder().icon("vane_trifles.item_death_scroll")
                )
                .component(JavaItemDataComponents.MAX_DAMAGE, 2)
                .build(),
        )

        // Parchment bag (paper already mapped above) and other items
        event.register(
            Identifier.of("glass_bottle"),
            CustomItemDefinition.builder(
                    Identifier.of("vane_trifles:item/empty_xp_bottle"),
                    Identifier.of("vane_trifles:empty_xp_bottle"),
                )
                .bedrockOptions(
                    CustomItemBedrockOptions.builder().icon("vane_trifles.item_empty_xp_bottle")
                )
                .build(),
        )

        // XP bottle sizes (honey_bottle)
        event.register(
            Identifier.of("honey_bottle"),
            CustomItemDefinition.builder(
                    Identifier.of("vane_trifles:item/small_xp_bottle"),
                    Identifier.of("vane_trifles:small_xp_bottle"),
                )
                .bedrockOptions(
                    CustomItemBedrockOptions.builder().icon("vane_trifles.item_small_xp_bottle")
                )
                .build(),
        )
        event.register(
            Identifier.of("honey_bottle"),
            CustomItemDefinition.builder(
                    Identifier.of("vane_trifles:item/medium_xp_bottle"),
                    Identifier.of("vane_trifles:medium_xp_bottle"),
                )
                .bedrockOptions(
                    CustomItemBedrockOptions.builder().icon("vane_trifles.item_medium_xp_bottle")
                )
                .build(),
        )
        event.register(
            Identifier.of("honey_bottle"),
            CustomItemDefinition.builder(
                    Identifier.of("vane_trifles:item/large_xp_bottle"),
                    Identifier.of("vane_trifles:large_xp_bottle"),
                )
                .bedrockOptions(
                    CustomItemBedrockOptions.builder().icon("vane_trifles.item_large_xp_bottle")
                )
                .build(),
        )
    }
}
