package org.oddlama.vane.waterfall;

import net.md_5.bungee.api.plugin.Plugin;

public final class WaterfallBasePlugin extends Plugin {

	Waterfall waterfall;

	@Override
	public void onEnable() {
		waterfall = new Waterfall(this);
		waterfall.enable();
	}

	@Override
	public void onDisable() {
		waterfall.disable();
	}

}
