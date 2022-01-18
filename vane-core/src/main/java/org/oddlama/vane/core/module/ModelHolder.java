package org.oddlama.vane.core.module;

import org.oddlama.vane.core.item.ModelDataEnum;

/**
 * An aspect of a module capable of holding models.
 */
public interface ModelHolder {
	Class<? extends ModelDataEnum> model_data_enum();

	int model_data(int item_id, int variant_id);
}
