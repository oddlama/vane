package org.oddlama.vane.core.config;

import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.core.module.ModuleComponent;

public class LootTable<T extends Module<T>> extends ModuleComponent<T> {
	public LootTable(Context<T> context) {
		super(context);
	}

	@Override
	public void on_enable() {}

	@Override
	public void on_disable() {}
}
