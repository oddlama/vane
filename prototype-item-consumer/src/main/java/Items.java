import java.util.function.Supplier;

public class Items extends VaneRegistry<InterestingPlugin, ItemTypes, InterestingItem> {
	public static final Supplier<ItemSingle> one = register(ItemTypes.Single, ItemSingle::new);
	public static final Supplier<ItemDouble> two = register(ItemTypes.Double, ItemDouble::new);

	public Items(InterestingPlugin plugin) {
		super(plugin);
	}

	@Override
	void registerReloadHandler(InterestingPlugin plugin, Runnable onReload) {
		plugin.registerReloadHandler(onReload);
	}
}

