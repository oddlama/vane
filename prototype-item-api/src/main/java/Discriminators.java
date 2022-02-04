import com.google.common.collect.Streams;
import net.kyori.adventure.text.Component;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Discriminators {

	@Deprecated
	public static class ComponentLoreDiscriminator<T extends EnumLike<T> & Keyed> implements ItemDiscriminator<ReferenceT, ItemT> {
		private final Function<? super Component, Optional<NamespacedKey>> lookup;

		public ComponentLoreDiscriminator() {
			this(Lookup.LoreLookup::lookup);
		}

		ComponentLoreDiscriminator(Function<? super Component, Optional<NamespacedKey>> lookup) {
			this.lookup = lookup;
		}

		@Override
		public Optional<CustomItem<T>> apply(ItemStack itemStack) {
			return loreStream(itemStack)
					.map(lookup)
					.flatMap(Optional::stream)
					.map(Lookup.NameSpaceKey::lookup)
					.flatMap(Optional::stream)
					.findFirst();
		}

		private static Stream<Component> loreStream(ItemStack itemStack) {
			var lore = itemStack.lore();
			if(lore == null) return Stream.empty();
			return lore.stream();
		}
	}

	/**
	 * Legacy fallback method that vane originally used to discriminate custom items.
	 * @param <ReferenceT>
	 */
	@SuppressWarnings("DeprecatedIsStillUsed")
	@Deprecated()
	public static class MaterialAndCustomModelDiscriminator<ReferenceT extends EnumLike<ReferenceT> & Reference, ItemT extends CustomItem<? extends ReferenceT>> implements ItemDiscriminator<ReferenceT, ItemT> {

		private final Function<Modellable.Model, Optional<ReferenceT>> lookup;
		private final VaneRegistry<?, ReferenceT, ItemT> registry;

		public MaterialAndCustomModelDiscriminator(Function<Modellable.Model, Optional<ReferenceT>> lookup, VaneRegistry<?, ReferenceT, ItemT> registry) {
			this.lookup = lookup;
			this.registry = registry;
		}

		public MaterialAndCustomModelDiscriminator(VaneRegistry<?, ReferenceT, ItemT> items, Function<ItemT, Modellable.Model> asModel, Function<ItemT, ReferenceT> asReference) {
			this(createLookupCacheFrom(items, asModel, asReference), items);
		}

		private static <ReferenceT extends EnumLike<ReferenceT> & Reference, ItemT extends CustomItem<? extends ReferenceT>> Function<Modellable.Model, Optional<ReferenceT>> createLookupCacheFrom(VaneRegistry<?, ReferenceT, ItemT> items, Function<ItemT, Modellable.Model> asModel, Function<ItemT, ReferenceT> asReference) {
			//noinspection UnstableApiUsage
			final Stream<? extends ItemT> stream = Streams.stream(items.iterator());
			//TODO: This naughty lil cache could cause issues when adding / removing ReferenceT instances during reload.
			// Need to incorporate reloading support. Could we add a listener to the Registry for when it gets notified of a reload?
			final Map<Modellable.Model, ReferenceT> cache = stream.collect(Collectors.toMap(asModel, asReference));
			return (Modellable.Model x) -> Optional.ofNullable(cache.get(x));
		}

		@Override
		public Optional<ItemT> apply(ItemStack itemStack) {
			if(itemStack == null) return Optional.empty();
			if(itemStack.getItemMeta() == null) return Optional.empty();
			if(!itemStack.getItemMeta().hasCustomModelData()) return Optional.empty();
			final int customModel = itemStack.getItemMeta().getCustomModelData();
			var ref= lookup.apply(new Modellable.Model(itemStack.getType(), customModel));
			return ref.flatMap(registry::get);

		}
	}

	/**
	 * @param <T>
	 */
	public record CustomBukkitNBTDiscriminator<T extends EnumLike<T> & Keyed>(
			NamespacedKey key,
			PersistentDataType<?, ?> type

	) implements ItemDiscriminator<T> {
		@Override
		public Optional<CustomItem<T>> apply(ItemStack itemStack) {
			final ItemMeta itemMeta = itemStack.getItemMeta();
			if(itemMeta.getPersistentDataContainer().has(this.key)) {
				itemMeta.getPersistentDataContainer().get(this.key, this.type);

			} else return Optional.empty();

		}
	}
}
