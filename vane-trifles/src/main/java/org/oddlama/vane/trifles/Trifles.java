package org.oddlama.vane.trifles;

import java.util.HashMap;
import java.util.UUID;

import org.oddlama.vane.annotation.VaneModule;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.trifles.items.XpBottles;

@VaneModule(name = "trifles", bstats = 8644, config_version = 4, lang_version = 3, storage_version = 1)
public class Trifles extends Module<Trifles> {
	public final HashMap<UUID, Long> last_xp_bottle_consume_time = new HashMap<>();
	public XpBottles xp_bottles;

	public Trifles() {
		var fast_walking_group = new FastWalkingGroup(this);
		new FastWalkingListener(fast_walking_group);
		new DoubleDoorListener(this);
		new HarvestListener(this);
		new RepairCostLimiter(this);
		new RecipeUnlock(this);
		new ChestSorter(this);

		new org.oddlama.vane.trifles.commands.Heads(this);

		new org.oddlama.vane.trifles.items.Scrolls(this);
		new org.oddlama.vane.trifles.items.ReinforcedElytra(this);
		new org.oddlama.vane.trifles.items.File(this);
		new org.oddlama.vane.trifles.items.Sickles(this);
		new org.oddlama.vane.trifles.items.EmptyXpBottle(this);
		xp_bottles = new org.oddlama.vane.trifles.items.XpBottles(this);
		//TODO new org.oddlama.vane.trifles.items.Trowel(this);
	}
}
