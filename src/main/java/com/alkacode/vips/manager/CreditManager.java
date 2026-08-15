package com.alkacode.vips.manager;

import com.alkacode.vips.hook.AlkaEconomyHook;
import com.alkacode.vips.model.VipPlayerData;
import com.alkacode.vips.storage.VipsRepository;

import java.util.List;
import java.util.UUID;

/**
 * Creditos VIP como moeda de verdade do AlkaEconomy (id {@link #CURRENCY_ID}) - saldo
 * mora em {@code alkaeconomy_balances}, nao mais em {@code player_data.credits} (a
 * coluna continua existindo no schema, so nao e mais lida/escrita por este manager).
 * {@code spentCredits}/{@code totalActivations} continuam locais em {@code player_data}
 * - sao estatisticas do AlkaVips, nao saldo, entao nao fazem sentido no AlkaEconomy.
 */
public final class CreditManager {

    public static final String CURRENCY_ID = "prisma";
    private static final String CURRENCY_NAME = "Prisma";
    private static final String CURRENCY_SYMBOL = "◈";

    private final PlayerVipManager playerVipManager;
    private final VipsRepository database;
    private final AlkaEconomyHook economyHook;

    public CreditManager(PlayerVipManager playerVipManager, VipsRepository database, AlkaEconomyHook economyHook) {
        this.playerVipManager = playerVipManager;
        this.database = database;
        this.economyHook = economyHook;
        economyHook.ensureCurrency(CURRENCY_ID, CURRENCY_NAME, CURRENCY_SYMBOL);
    }

    public double getCredits(UUID uuid) {
        return economyHook.getBalance(uuid, CURRENCY_ID);
    }

    public boolean has(UUID uuid, double amount) {
        return economyHook.has(uuid, CURRENCY_ID, amount);
    }

    public void add(UUID uuid, double amount) {
        economyHook.deposit(uuid, CURRENCY_ID, amount);
    }

    public boolean remove(UUID uuid, double amount) {
        if (!economyHook.withdraw(uuid, CURRENCY_ID, amount)) {
            return false;
        }
        VipPlayerData data = playerVipManager.dataOf(uuid);
        data.spentCredits(data.spentCredits() + amount);
        database.savePlayerData(data);
        return true;
    }

    public void set(UUID uuid, double amount) {
        economyHook.setBalance(uuid, CURRENCY_ID, Math.max(0, amount));
    }

    public void incrementActivations(UUID uuid) {
        VipPlayerData data = playerVipManager.dataOf(uuid);
        data.totalActivations(data.totalActivations() + 1);
        database.savePlayerData(data);
    }

    /** Top N jogadores por creditos - le direto do AlkaEconomy, inclui offline. */
    public List<com.alkacode.economy.storage.EconomyRepository.TopBalanceEntry> getTop(int limit) {
        return economyHook.getTopBalances(CURRENCY_ID, limit);
    }
}
