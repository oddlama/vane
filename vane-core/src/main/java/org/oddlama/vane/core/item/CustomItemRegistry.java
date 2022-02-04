package org.oddlama.vane.core.item;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.oddlama.vane.core.module.Module;

import java.util.Iterator;

public class CustomItemRegistry<ModuleT extends Module<ModuleT>> implements Registry<CustomItem<ModuleT,? extends CustomItem<ModuleT, ?>>> {

	protected  <R extends CustomItem<ModuleT, R>> R register(R item) {
	}

	@Override
	public @Nullable CustomItem<M,I> get(@NotNull NamespacedKey key) {
		return null;
	}

	@NotNull
	@Override
	public Iterator<CustomItemRegistry> iterator() {
		return null;
	}
}
