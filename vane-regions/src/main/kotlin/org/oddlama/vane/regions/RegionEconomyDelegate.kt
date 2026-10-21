package org.oddlama.vane.regions

import java.util.Locale
import net.thenextlvl.service.api.economy.EconomyController
import org.bukkit.OfflinePlayer
import org.bukkit.plugin.Plugin

/** Thin wrapper around ServiceIO economy APIs used by the regions module. */
class RegionEconomyDelegate(private val module: Regions) {
    /** Economy service provider resolved from ServiceIO. */
    private var economy: EconomyController? = null

    /** Non-null accessor for the resolved economy service. */
    private val activeEconomy: EconomyController
        get() = requireNotNull(economy) { "Economy delegate used before setup() completed" }

    /**
     * Resolves and stores the active ServiceIO economy provider.
     *
     * @return `true` when setup succeeds.
     */
    fun setup(plugin: Plugin?): Boolean {
        if (plugin == null) {
            module.log.severe(
                "Economy was selected as the currency provider, but the ServiceIO plugin wasn't found! Falling back to material currency."
            )
            return false
        }
        val rsp = module.server.servicesManager.getRegistration(EconomyController::class.java)
        if (rsp == null) {
            module.log.severe(
                "Economy was selected as the currency provider, but no Economy service provider is registered via ServiceIO! Falling back to material currency."
            )
            return false
        }

        economy = rsp.provider
        return true
    }

    /** Returns whether the player has at least the requested balance. */
    fun has(player: OfflinePlayer?, amount: Double): Boolean {
        if (player == null) return false
        val account = activeEconomy.tryGetAccount(player).join().orElse(null) ?: return false
        return account.balance.toDouble() >= amount
    }

    /** Withdraws the requested amount from a player account. */
    fun withdraw(player: OfflinePlayer?, amount: Double): Boolean {
        if (player == null) return false
        val account = activeEconomy.tryGetAccount(player).join().orElse(null) ?: return false
        if (account.balance.toDouble() < amount) return false
        account.withdraw(amount)
        return true
    }

    /** Deposits the requested amount into a player account. */
    fun deposit(player: OfflinePlayer?, amount: Double): Boolean {
        if (player == null) return false
        val account = activeEconomy.tryGetAccount(player).join().orElse(null) ?: return false
        account.deposit(amount)
        return true
    }

    /** Returns the pluralized currency label from the active provider. */
    fun currencyNamePlural(): String = activeEconomy.getCurrencyNamePlural(Locale.US)

    /** Returns the number of supported fractional digits in this economy. */
    fun fractionalDigits(): Int = activeEconomy.fractionalDigits()
}
