package org.oddlama.vane.trifles.items;

import org.oddlama.vane.trifles.Trifles;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerItemDamageEvent;

import org.oddlama.vane.annotation.enchantment.Rarity;
import org.oddlama.vane.annotation.item.VaneItem;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.item.CustomItem;

@VaneItem(name = "scythe")
public class Scythe extends CustomItem<Trifles> {
	public Scythe(Context<Trifles> context) {
		super(context);
	}
}
