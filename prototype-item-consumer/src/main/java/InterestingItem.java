import org.bukkit.Material;
import org.bukkit.event.player.PlayerInteractEvent;

public class InterestingItem extends CustomItem<ItemTypes> implements Modellable {
	private final Modellable.Model model;

	public InterestingItem(ItemTypes reference) {
		super(reference);
		model = new Model(Material.DIAMOND_HOE, reference.ordinal());
	}

	@Override
	public Model model() {
		return this.model;
	}

	public void handle(PlayerInteractEvent event) {
		event.setCancelled(true);
	}

	public ItemTypes reference() {
		return super.reference;
	}
}