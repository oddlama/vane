import net.kyori.adventure.text.Component;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;

import java.util.Optional;

public class Lookup {
	public static class ModelLookup {
		public static <T extends CustomItem<? extends ReferenceT>, ReferenceT> Optional<T> lookup(NamespacedKey base, int discriminator) {
			//TODO:
			return Optional.empty();
		}
	}

	public static class LoreLookup {
		public static Optional<NamespacedKey> lookup(Component loreLine) {
			//TODO:
			return Optional.empty();
		}
	}

	public static class NameSpaceKey {
		public static <T extends EnumLike<T> & Keyed> Optional<CustomItem<T>> lookup(NamespacedKey key) {
			//TODO:
			return Optional.empty();
		}
	}
}
