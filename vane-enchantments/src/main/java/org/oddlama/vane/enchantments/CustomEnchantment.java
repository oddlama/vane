package org.oddlama.vane.enchantments;

import static org.oddlama.vane.util.Util.namespaced_key;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;

import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import org.jetbrains.annotations.NotNull;

import org.oddlama.vane.annotation.enchantment.Rarity;
import org.oddlama.vane.annotation.enchantment.VaneEnchantment;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.util.Nms;

public class CustomEnchantment<T extends Module<T>> extends Listener<T> {
	// Track instances
	private static final Map<Class<?>, CustomEnchantment<?>> instances = new HashMap<>();

	private VaneEnchantment annotation = getClass().getAnnotation(VaneEnchantment.class);
	private String name;
	private NamespacedKey key;
	private NativeEnchantmentWrapper native_wrapper;
	private BukkitEnchantmentWrapper bukkit_wrapper;

	private final Set<Enchantment> supersedes = new HashSet<>();

	// Language
	@LangMessage public TranslatedMessage lang_name;

	// All associated recipes
	private Map<NamespacedKey, Recipe> recipes = new HashMap<>();

	public CustomEnchantment(Context<T> context) {
		super(null);

		// Make namespace
		name = annotation.name();
		context = context.group("enchantment_" + name, "Enable enchantment " + name);
		set_context(context);

		// Create namespaced key
		key = namespaced_key(get_module().namespace(), name);

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
	}

	/**
	 * May be overridden to reigster superseding enchantments.
	 */
	public void register_superseding() { }

	/**
	 * Calls register_superseding() on all custom enchantment instances.
	 * This allows them to add superseding enchantments while having access
	 * to every other custom enchantment instance.
	 */
	public static void call_register_superseding() {
		instances.values().forEach(CustomEnchantment::register_superseding);
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
	public final Set<Enchantment> supersedes() {
		return supersedes;
	}

	/**
	 * Adds a superseded enchantment. Superseded enchantments will be removed
	 * from the item when this enchantment is added.
	 */
	public final void supersedes(Enchantment e) {
		supersedes.add(e);
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
	public void apply_display_format(BaseComponent component) {
		switch (annotation.rarity()) {
			default:
			case COMMON:
				component.setColor(ChatColor.DARK_AQUA);
				break;

			case UNCOMMON:
				component.setColor(ChatColor.DARK_AQUA);
				break;

			case RARE:
				component.setColor(ChatColor.GOLD);
				break;

			case VERY_RARE:
				component.setColor(ChatColor.DARK_PURPLE);
				component.setBold(true);
				break;
		}
	}

	/**
	 * Determines the display name of the enchantment.
	 * Usually you don't need to override this method, as it already
	 * uses clientside translation keys and supports chat formatting.
	 */
	public BaseComponent display_name(int level) {
        final var display_name = lang_name.format();
		display_name.setItalic(false);
		apply_display_format(display_name);

		if (level != 1 || max_level() != 1) {
			final var chat_level = new TranslatableComponent("enchantment.level." + level);
			chat_level.setItalic(false);
			apply_display_format(chat_level);
			display_name.addExtra(" ");
			display_name.addExtra(chat_level);
        }

        return display_name;
	}

	/**
	 * The minimum level this enchantment can have. Always fixed to 1.
	 */
	public final int start_level() {
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
	public int min_enchanting_level(int level) {
		return 1 + level * 10;
	}

	/**
	 * Determines the maximum enchanting table level at which this enchantment
	 * can occur at the given level.
	 */
	public int max_enchanting_level(int level) {
		return min_enchanting_level(level) + 5;
	}

	/**
	 * Determines if this enchantment can be obtained with the enchanting table.
	 * Always reflects the annotation value {@link VaneEnchantment#treasure()}.
	 */
	public final boolean is_treasure() {
		return annotation.treasure();
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

	/**
	 * Override this and add your related recipes here.
	 */
	public void register_recipes() {}

	/** Returns the main recipe key */
	public final NamespacedKey recipe_key() { return recipe_key(""); }
	/** Returns a named recipe key */
	public final NamespacedKey recipe_key(String recipe_name) {
		if (recipe_name.equals("")) {
			return namespaced_key(get_module().namespace(), "enchantment_" + name + "_recipe");
		}
		return namespaced_key(get_module().namespace(), "enchantment_" + name + "_recipe_" + recipe_name);
	}

	private final void add_recipe_or_throw(NamespacedKey recipe_key, Recipe recipe) {
		if (recipes.containsKey(recipe_key)) {
			throw new RuntimeException("A recipe with the same key ('" + recipe_key + "') is already defined!");
		}
		recipes.put(recipe_key, recipe);
	}

	/**
	 * Adds a related recipe to this item.
	 * Useful if you need non-standard recipes.
	 */
	public final Recipe add_recipe(NamespacedKey recipe_key, Recipe recipe) {
		add_recipe_or_throw(recipe_key, recipe);
		return recipe;
	}

	public final<R extends Recipe & Keyed> Recipe add_recipe(R recipe) {
		add_recipe_or_throw(((Keyed)recipe).getKey(), recipe);
		return recipe;
	}

	@Override
	public void on_config_change() {
		recipes.keySet().forEach(get_module().getServer()::removeRecipe);
		recipes.clear();

		register_recipes();
		recipes.values().forEach(get_module().getServer()::addRecipe);

		super.on_config_change();
	}
}
