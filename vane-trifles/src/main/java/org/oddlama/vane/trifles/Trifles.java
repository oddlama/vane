package org.oddlama.vane.trifles;

import org.oddlama.vane.annotation.VaneModule;
import org.oddlama.vane.core.Core;
import org.oddlama.vane.core.item.ModelDataEnum;
import org.oddlama.vane.core.module.Module;

@VaneModule(name = "trifles", bstats = 8644, config_version = 1, lang_version = 1, storage_version = 1)
public class Trifles extends Module<Trifles> {
	public Trifles() {
		var fast_walking_group = new FastWalkingGroup(this);
		new FastWalkingListener(fast_walking_group);
		new DoubleDoorListener(this);
		new HarvestListener(this);
		new RepairCostLimiter(this);
		new RecipeUnlock(this);
		new ChestSorter(this);

		new org.oddlama.vane.trifles.items.Sickle(this);
		new org.oddlama.vane.trifles.items.File(this);
		new org.oddlama.vane.trifles.items.EmptyXpBottle(this);
		new org.oddlama.vane.trifles.items.XpBottle(this);
	}

	@Override
	public Class<? extends ModelDataEnum> model_data_enum() {
		return org.oddlama.vane.trifles.items.ModelData.class;
	}

	@Override
	public int model_data(int item_id, int variant_id) {
		return Core.model_data(0, item_id, variant_id);
	}
}
