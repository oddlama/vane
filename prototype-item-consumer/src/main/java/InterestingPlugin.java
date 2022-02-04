import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class InterestingPlugin extends JavaPlugin implements Listener {

	private final List<Runnable> onReload = new LinkedList<>();
	private Items items;
	@SuppressWarnings("deprecation")
	private Discriminators.MaterialAndCustomModelDiscriminator<ItemTypes, InterestingItem> lookup;

	@Override
	public void onEnable() {
		super.onEnable();
		onReload.forEach(Runnable::run);
		this.items = new Items(this);

		//noinspection deprecation
		lookup = new Discriminators.MaterialAndCustomModelDiscriminator<>(items, InterestingItem::model, InterestingItem::reference);
	}

	public void registerReloadHandler(Runnable onReload) {
		this.onReload.add(onReload);
	}

	@EventHandler
	public void onInteract(PlayerInteractEvent event) {
		final Optional<InterestingItem> customItem = lookup.apply(event.getItem());
		customItem.ifPresent(item -> item.handle(event));
	}
}