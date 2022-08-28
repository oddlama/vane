package org.oddlama.vane.core.enchantments;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.oddlama.vane.annotation.enchantment.Rarity;
import org.oddlama.vane.annotation.enchantment.VaneEnchantment;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.config.loot.LootTableList;
import org.oddlama.vane.core.config.loot.LootTables;
import org.oddlama.vane.core.config.recipes.RecipeList;
import org.oddlama.vane.core.config.recipes.Recipes;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.util.Nms;
import org.oddlama.vane.util.StorageUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class CustomEnchantment<T extends Module<T>> extends Listener<T> {
	// Track instances
	private static final Map<Class<?>, CustomEnchantment<?>> instances = new HashMap<>();

	private VaneEnchantment annotation = getClass().getAnnotation(VaneEnchantment.class);
	private String name;
	private NamespacedKey key;
	private NativeEnchantmentWrapper native_wrapper;
	private BukkitEnchantmentWrapper bukkit_wrapper;

	private final Set<NamespacedKey> supersedes = new HashSet<>();

	public Recipes<T> recipes;
	public LootTables<T> loot_tables;

	// Language
	@LangMessage
	public TranslatedMessage lang_name;

	public CustomEnchantment(Context<T> context) {
		this(context, true);
	}

	public CustomEnchantment(Context<T> context, boolean default_enabled) {
		super(null);
		// Make namespace
		name = annotation.name();
		context = context.group("enchantment_" + name, "Enable enchantment " + name, default_enabled);
		set_context(context);

		// Create namespaced key
		key = StorageUtil.namespaced_key(get_module().namespace(), name);

		// Check if instance is already exists
		if (instances.get(getClass()) != null) {
			throw new RuntimeException("Cannot create two instances of a custom enchantment!");
		}
		instances.put(getClass(), this);

		// Register and create wrappers
		native_wrapper = new NativeEnchantmentWrapper(this);
		Nms.register_enchantment(key(), native_wrapper);

		// After registering in NMS we can create a wrapper for bukkit
		bukkit_wrapper = new BukkitEnchantmentWrapper(this, native_wrapper);
		Enchantment.registerEnchantment(bukkit_wrapper);

		// Automatic recipes and loot table config and registration
		recipes = new Recipes<T>(get_context(), this.key, this::default_recipes);
		loot_tables = new LootTables<T>(get_context(), this.key, this::default_loot_tables);
	}

	/**
	 * Returns the bukkit wrapper for the given custom enchantment.
	 */
	public static BukkitEnchantmentWrapper bukkit(Class<? extends CustomEnchantment<?>> cls) {
		return instances.get(cls).bukkit();
	}

	/**
	 * Returns the bukkit wrapper for this enchantment.
	 */
	public final BukkitEnchantmentWrapper bukkit() {
		return bukkit_wrapper;
	}

	/**
	 * Returns all enchantments that are superseded by this enchantment.
	 */
	public final Set<NamespacedKey> supersedes() {
		return supersedes;
	}

	/**
	 * Adds a superseded enchantment. Superseded enchantments will be removed
	 * from the item when this enchantment is added.
	 */
	public final void supersedes(NamespacedKey e) {
		supersedes.add(e);
	}

	/**
	 * Adds a superseded enchantment. Superseded enchantments will be removed
	 * from the item when this enchantment is added.
	 */
	public final void supersedes(Enchantment e) {
		supersedes(e.getKey());
	}

	/**
	 * Returns the namespaced key for this enchantment.
	 */
	public final NamespacedKey key() {
		return key;
	}

	/**
	 * Only for internal use.
	 */
	final String get_name() {
		return name;
	}

	/**
	 * Returns the display format for the display name.
	 * By default the color is dependent on the rarity.
	 * COMMON: gray
	 * UNCOMMON: dark blue
	 * RARE: gold
	 * VERY_RARE: bold dark purple
	 */
	public Component apply_display_format(Component component) {
		switch (annotation.rarity()) {
			default:
			case COMMON:
			case UNCOMMON:
				return component.color(NamedTextColor.DARK_AQUA);
			case RARE:
				return component.color(NamedTextColor.GOLD);
			case VERY_RARE:
				return component.color(NamedTextColor.DARK_PURPLE).decorate(TextDecoration.BOLD);
		}
	}

	/**
	 * Determines the display name of the enchantment.
	 * Usually you don't need to override this method, as it already
	 * uses clientside translation keys and supports chat formatting.
	 */
	public Component display_name(int level) {
		var display_name = apply_display_format(lang_name.format().decoration(TextDecoration.ITALIC, false));

		if (level != 1 || max_level() != 1) {
			final var chat_level = apply_display_format(
				Component.translatable("enchantment.level." + level).decoration(TextDecoration.ITALIC, false)
			);
			display_name = display_name.append(Component.text(" ")).append(chat_level);
		}

		return display_name;
	}

	/**
	 * The minimum level this enchantment can have. Always fixed to 1.
	 */
	public final int min_level() {
		return 1;
	}

	/**
	 * The maximum level this enchantment can have.
	 * Always reflects the annotation value {@link VaneEnchantment#max_level()}.
	 */
	public final int max_level() {
		return annotation.max_level();
	}

	/**
	 * Determines the minimum enchanting table level at which this enchantment
	 * can occur at the given level.
	 */
	public int min_cost(int level) {
		return 1 + level * 10;
	}

	/**
	 * Determines the maximum enchanting table level at which this enchantment
	 * can occur at the given level.
	 */
	public int max_cost(int level) {
		return min_cost(level) + 5;
	}

	/**
	 * Determines if this enchantment can be obtained with the enchanting table.
	 * Always reflects the annotation value {@link VaneEnchantment#treasure()}.
	 */
	public final boolean is_treasure() {
		return annotation.treasure();
	}

	/**
	 * Determines if this enchantment is tradeable with villagers.
	 * Always reflects the annotation value {@link VaneEnchantment#tradeable()}.
	 */
	public final boolean is_tradeable() {
		return annotation.tradeable();
	}

	/**
	 * Determines if this enchantment is a curse.
	 * Always reflects the annotation value {@link VaneEnchantment#curse()}.
	 */
	public final boolean is_curse() {
		return annotation.curse();
	}

	/**
	 * Determines if this enchantment generates on treasure items.
	 * Always reflects the annotation value {@link VaneEnchantment#generate_in_treasure()}.
	 */
	public final boolean generate_in_treasure() {
		return annotation.generate_in_treasure();
	}

	/**
	 * Determines which item types this enchantment can be applied to.
	 * {@link #can_enchant(ItemStack)} can be used to further limit the applicable items.
	 * Always reflects the annotation value {@link VaneEnchantment#target()}.
	 */
	public final EnchantmentTarget target() {
		return annotation.target();
	}

	/**
	 * Determines the enchantment rarity.
	 * Always reflects the annotation value {@link VaneEnchantment#rarity()}.
	 */
	public final Rarity rarity() {
		return annotation.rarity();
	}

	/**
	 * Weather custom items are allowed to be enchanted with this enchantment.
	 */
	public final boolean allow_custom() {
		return annotation.allow_custom();
	}

	/**
	 * Determines if this enchantment is compatible with the given enchantment.
	 * By default all enchantments are compatible. Override this if you want
	 * to express conflicting enchantments.
	 */
	public boolean is_compatible(@NotNull Enchantment other) {
		return true;
	}

	/**
	 * Determines if this enchantment can be applied to the given item.
	 * By default this returns true if the {@link #target()} category includes
	 * the given itemstack. Unfortunately this method cannot be used to widen
	 * the allowed items, just to narrow it (limitation due to minecraft server internals).
	 * So for best results, always check super.can_enchant first when overriding.
	 */
	public boolean can_enchant(@NotNull ItemStack item_stack) {
		return annotation.target().includes(item_stack);
	}

	public RecipeList default_recipes() {
		return RecipeList.of();
	}

	public LootTableList default_loot_tables() {
		return LootTableList.of();
	}

	/** Applies this enchant to the given string item definition. */
	protected String on(String item_definition) {
		return on(item_definition, 1);
	}

	protected String on(String item_definition, int level) {
		return item_definition + "#enchants{" + key + "*" + level + "}";
	}
}
