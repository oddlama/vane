package org.oddlama.vane.regions;

import static org.oddlama.vane.util.PlayerUtil.take_items;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import net.minecraft.server.v1_16_R3.BlockPosition;

import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle.DustOptions;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;

import org.oddlama.vane.annotation.VaneModule;
import org.oddlama.vane.annotation.config.ConfigBoolean;
import org.oddlama.vane.annotation.config.ConfigDouble;
import org.oddlama.vane.annotation.config.ConfigInt;
import org.oddlama.vane.annotation.config.ConfigMaterial;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.annotation.persistent.Persistent;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.core.persistent.PersistentSerializer;
import org.oddlama.vane.portals.Portals;
import org.oddlama.vane.regions.menu.RegionGroupMenuTag;
import org.oddlama.vane.regions.menu.RegionMenuGroup;
import org.oddlama.vane.regions.menu.RegionMenuTag;
import org.oddlama.vane.regions.region.EnvironmentSetting;
import org.oddlama.vane.regions.region.Region;
import org.oddlama.vane.regions.region.RegionExtent;
import org.oddlama.vane.regions.region.RegionGroup;
import org.oddlama.vane.regions.region.RegionSelection;
import org.oddlama.vane.regions.region.Role;
import org.oddlama.vane.regions.region.RoleSetting;

public class RegionEconomyDelegate {
	public Regions parent;
	public Economy economy;

	public RegionEconomyDelegate(final Regions parent) {
		this.parent = parent;
	}

	public Regions get_module() {
		return parent;
	}

	public boolean setup(final Plugin plugin) {
		RegisteredServiceProvider<Economy> rsp = get_module().getServer().getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			get_module().log.severe("Economy was selected as the currency provider, but no Economy service provider is registered via VaultAPI! Falling back to material currency.");
			return false;
		}

		economy = rsp.getProvider();
		return true;
	}

	public boolean has(final OfflinePlayer player, double amount) {
		return economy.has(player, amount);
	}

	public EconomyResponse withdraw(final OfflinePlayer player, double amount) {
		return economy.withdrawPlayer(player, amount);
	}

	public EconomyResponse deposit(final OfflinePlayer player, double amount) {
		return economy.depositPlayer(player, amount);
	}

	public String currency_name_plural() {
		return economy.currencyNamePlural();
	}

	public int fractionalDigits() {
		return economy.fractionalDigits();
	}
}
