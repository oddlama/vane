package org.oddlama.vane.core;

import static org.oddlama.vane.util.ResourceList.get_resources;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Random;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;

import org.bstats.bukkit.Metrics;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;

import org.oddlama.vane.annotation.VaneModule;
import org.oddlama.vane.annotation.config.ConfigBoolean;
import org.oddlama.vane.annotation.config.ConfigString;
import org.oddlama.vane.annotation.config.ConfigVersion;
import org.oddlama.vane.annotation.lang.LangVersion;
import org.oddlama.vane.annotation.persistent.Persistent;
import org.oddlama.vane.core.Core;
import org.oddlama.vane.core.ResourcePackGenerator;
import org.oddlama.vane.core.functional.Consumer2;
import org.oddlama.vane.core.command.Command;
import org.oddlama.vane.core.config.ConfigManager;
import org.oddlama.vane.core.item.ModelDataEnum;
import org.oddlama.vane.core.lang.LangManager;
import org.oddlama.vane.core.persistent.PersistentStorageManager;

public class LootTable {
	private Random random = new Random();
	private Map<NamespacedKey, LootTableEntry> possible_loot = new HashMap<>();

	public LootTable() {
	}

	public LootTable put(final NamespacedKey key, final LootTableEntry entry) {
		possible_loot.put(key, entry);
		return this;
	}

	public void generate_loot(final List<ItemStack> output) {
		for (final var loot : possible_loot.values()) {
			if (loot.evaluate_chance(random)) {
				loot.add_sample(output, random);
			}
		}
	}

	public static class LootTableEntry {
		private double chance;
		private Consumer2<List<ItemStack>, Random> generator;


		public LootTableEntry(int rarity_expected_chests, final ItemStack item) {
			this(1.0 / rarity_expected_chests, item.clone(), 1, 1);
		}

		public LootTableEntry(int rarity_expected_chests, final ItemStack item, int amount_min, int amount_max) {
			this(1.0 / rarity_expected_chests, item.clone(), amount_min, amount_max);
		}

		private LootTableEntry(double chance, final ItemStack item, int amount_min, int amount_max) {
			this(chance, (list, random) -> {
				final var i = item.clone();
				final var amount = random.nextInt(amount_max - amount_min + 1) + amount_min;
				if (amount < 1) {
					return;
				}

				i.setAmount(amount);
				list.add(i);
			});
		}

		public LootTableEntry(double chance, Consumer2<List<ItemStack>, Random> generator) {
			this.chance = chance;
			this.generator = generator;
		}

		public void add_sample(final List<ItemStack> items, final Random random) {
			generator.apply(items, random);
		}

		public boolean evaluate_chance(Random random) {
			return random.nextDouble() < chance;
		}
	}
}
