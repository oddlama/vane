import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

public class ReloadableMultiton<referenceT extends EnumLike<referenceT> & Keyed>  implements Keyed {
	private final NamespacedKey key;
	protected final referenceT reference;

	public ReloadableMultiton(referenceT reference) {
		this.reference = reference;
		this.key = reference.getKey();
	}

	@Override
	public @NotNull NamespacedKey getKey() {
		return this.key;
	}
}