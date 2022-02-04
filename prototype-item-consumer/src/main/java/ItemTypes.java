import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

public enum ItemTypes implements Reference, EnumLike<ItemTypes> {
	Single("single"),
	Double("double");

	private static final String PLUGIN_PREFIX = "Interesting";
	private final NamespacedKey key;

	ItemTypes(String name) {
		this.key = new NamespacedKey(PLUGIN_PREFIX, name);
	}

	@Override
	public @NotNull NamespacedKey getKey() {
		return this.key;
	}
}