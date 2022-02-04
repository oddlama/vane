import org.bukkit.Keyed;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.function.Function;

/**
 * Functional interface capable of determining the CustomItem instance for a given itemstack.
 * @param <ReferenceT>
 */
public interface ItemDiscriminator<
			ReferenceT extends EnumLike<ReferenceT> & Reference,
			ItemT extends CustomItem<? extends ReferenceT>
		> extends Function<ItemStack, Optional<? extends ItemT>> {
	@Override
	Optional<? extends ItemT> apply(ItemStack itemStack);
}