package com.alkacode.vips.hook;

import com.alkacode.economy.AlkaEconomyPlugin;
import com.alkacode.economy.CurrencyRegistry;
import com.alkacode.economy.EconomyManager;
import com.alkacode.economy.storage.EconomyRepository;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Ponte para o AlkaEconomy - AlkaVips nao suporta Vault/outros provedores, so o
 * EconomyManager direto (mesmo padrao usado no AlkaEnderChest/AlkaGates), com o nome
 * da moeda resolvido por string (config do marketplace/upgrade usa nomes como
 * "coins", "escarion"). Tambem usado pelo CreditManager pra registrar/mover os
 * creditos VIP como uma moeda de verdade do AlkaEconomy (id "credits_vip").
 */
public final class AlkaEconomyHook {

    private final EconomyManager economyManager;
    private final CurrencyRegistry currencyRegistry;

    public AlkaEconomyHook(EconomyManager economyManager, CurrencyRegistry currencyRegistry) {
        this.economyManager = economyManager;
        this.currencyRegistry = currencyRegistry;
    }

    public static AlkaEconomyHook resolve() {
        AlkaEconomyPlugin plugin = (AlkaEconomyPlugin) Bukkit.getPluginManager().getPlugin("AlkaEconomy");
        if (plugin == null) {
            return null;
        }
        return new AlkaEconomyHook(plugin.getEconomyManager(), plugin.getCurrencyRegistry());
    }

    /** Cria a moeda no AlkaEconomy se ainda nao existir - idempotente, seguro de chamar todo enable. */
    public void ensureCurrency(String id, String name, String symbol) {
        if (!currencyRegistry.isValid(id)) {
            currencyRegistry.create(id, name, symbol);
        }
    }

    public void setBalance(UUID uuid, String currency, double amount) {
        String id = resolveCurrency(currency);
        if (id != null) {
            economyManager.setBalance(uuid, id, amount);
        }
    }

    /** Top N saldos de uma moeda, direto do banco do AlkaEconomy (inclui offline). */
    public List<EconomyRepository.TopBalanceEntry> getTopBalances(String currency, int limit) {
        String id = resolveCurrency(currency);
        return id != null ? economyManager.getTopBalances(id, limit) : List.of();
    }

    public boolean isValidCurrency(String name) {
        return resolveCurrency(name) != null;
    }

    private String resolveCurrency(String name) {
        if (name == null) {
            return null;
        }
        String id = name.toLowerCase(Locale.ROOT);
        return economyManager.isValidCurrency(id) ? id : null;
    }

    /**
     * Ids das moedas negociaveis, na ordem do config.yml do AlkaEconomy - usado pelo
     * SellKeyMenu para ciclar entre elas. Exclui "credits_vip": e uma moeda de
     * recompensa interna do AlkaVips (ver CreditManager), nao faz sentido vender uma
     * key de VIP por ela.
     */
    public List<String> currencyIds() {
        return economyManager.getCurrencyIds().stream()
                .filter(id -> !id.equals(com.alkacode.vips.manager.CreditManager.CURRENCY_ID))
                .toList();
    }

    public double getBalance(UUID uuid, String currency) {
        String id = resolveCurrency(currency);
        return id != null ? economyManager.getBalance(uuid, id) : 0.0;
    }

    public boolean has(UUID uuid, String currency, double amount) {
        String id = resolveCurrency(currency);
        return id != null && economyManager.has(uuid, id, amount);
    }

    public boolean withdraw(UUID uuid, String currency, double amount) {
        String id = resolveCurrency(currency);
        if (id == null || !economyManager.has(uuid, id, amount)) {
            return false;
        }
        economyManager.removeBalance(uuid, id, amount);
        return true;
    }

    public void deposit(UUID uuid, String currency, double amount) {
        String id = resolveCurrency(currency);
        if (id != null) {
            economyManager.addBalance(uuid, id, amount);
        }
    }

    public String format(double amount) {
        return EconomyManager.formatValue(amount);
    }
}
