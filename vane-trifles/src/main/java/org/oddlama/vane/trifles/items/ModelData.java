package org.oddlama.vane.trifles.items;

import org.oddlama.vane.trifles.Trifles;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerItemDamageEvent;

import org.oddlama.vane.annotation.enchantment.Rarity;
import org.oddlama.vane.annotation.item.VaneItem;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.item.ModelDataEnum;
import org.oddlama.vane.core.item.CustomItem;

public enum ModelData implements ModelDataEnum {
	SICKLE(0);

	private int id = 0;
	private ModelData(int id) {
		this.id = id;
	}

	@Override
	public int id() { return id; }
}
