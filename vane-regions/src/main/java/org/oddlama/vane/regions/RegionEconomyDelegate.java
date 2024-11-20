package org.oddlama.vane.regions;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

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
        RegisteredServiceProvider<Economy> rsp = get_module()
            .getServer()
            .getServicesManager()
            .getRegistration(Economy.class);
        if (rsp == null) {
            get_module()
                .log.severe(
                    "Economy was selected as the currency provider, but no Economy service provider is registered via VaultAPI! Falling back to material currency."
                );
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
