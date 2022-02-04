import org.bukkit.Keyed;

public class CustomItem<T extends EnumLike<T> & Keyed> extends ReloadableMultiton<T> {
	public CustomItem(T reference) {
		super(reference);
	}
}
