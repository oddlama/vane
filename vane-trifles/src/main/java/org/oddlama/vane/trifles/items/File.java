package org.oddlama.vane.trifles.items;

import org.bukkit.Material;
import org.bukkit.inventory.ShapedRecipe;

import org.oddlama.vane.annotation.item.VaneItem;
import org.oddlama.vane.core.item.CustomItem;
import org.oddlama.vane.core.item.CustomItemVariant;
import org.oddlama.vane.core.item.ItemVariantEnum;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.trifles.Trifles;

@VaneItem(name = "file")
public class File extends CustomItem<Trifles, File> {
	public File(Context<Trifles> context) {
		super(context);
	}
}
