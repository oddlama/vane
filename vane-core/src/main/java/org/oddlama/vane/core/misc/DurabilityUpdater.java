package org.oddlama.vane.core.misc;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.loot.Lootable;
import org.oddlama.vane.annotation.config.ConfigBoolean;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.Core;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;

public class DurabilityUpdater extends Listener<Core> {
	public DurabilityUpdater(Context<Core> context) {
		super(context);
	}

	@Override
	protected void on_enable() {
	}

	@Override
	protected void on_disable() {
	}

	// TODO damage
	// TODO mend
}
