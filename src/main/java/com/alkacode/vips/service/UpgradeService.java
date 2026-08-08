package com.alkacode.vips.service;

import com.alkacode.vips.config.ConfigManager;
import com.alkacode.vips.event.VipUpgradeEvent;
import com.alkacode.vips.hook.AlkaEconomyHook;
import com.alkacode.vips.manager.PlayerVipManager;
import com.alkacode.vips.manager.VipTypeManager;
import com.alkacode.vips.model.PlayerVip;
import com.alkacode.vips.model.VipType;
import com.alkacode.vips.model.enums.VipStatus;
import com.alkacode.vips.util.CommandUtil;
import com.alkacode.vips.util.TextUtil;
import com.alkacode.vips.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public final class UpgradeService {

    private final PlayerVipManager playerVipManager;
    private final VipTypeManager vipTypeManager;
    private final ConfigManager configManager;
    private final AlkaEconomyHook economyHook;

    public UpgradeService(PlayerVipManager playerVipManager, VipTypeManager vipTypeManager,
                           ConfigManager configManager, AlkaEconomyHook economyHook) {
        this.playerVipManager = playerVipManager;
        this.vipTypeManager = vipTypeManager;
        this.configManager = configManager;
        this.economyHook = economyHook;
    }

    public enum Result {
        SUCCESS, NO_UPGRADE, INVALID_CURRENCY, INSUFFICIENT_FUNDS
    }

    /**
     * Credita ao jogador o tempo ainda nao consumido do VIP atual: quanto mais tempo
     * restante, maior o desconto sobre o preco base do upgrade.
     */
    public double calculateUpgradePrice(PlayerVip currentVip, VipType fromType, String currency) {
        Double price = fromType.upgradePrices().get(currency.toLowerCase());
        if (price == null) {
            return 0;
        }
        if (!currentVip.isPermanent() && currentVip.remainingMillis() > 0 && currentVip.totalDuration() > 0) {
            double discount = price * ((double) currentVip.remainingMillis() / currentVip.totalDuration());
            return Math.max(0, price - discount);
        }
        return price;
    }

    public Result upgrade(Player player, PlayerVip currentVip, VipType fromType, String currency) {
        if (!fromType.hasUpgrade() || !vipTypeManager.exists(fromType.upgradeTo())) {
            return Result.NO_UPGRADE;
        }
        VipType toType = vipTypeManager.get(fromType.upgradeTo());
        if (currency == null) {
            return Result.INVALID_CURRENCY;
        }
        Double basePrice = fromType.upgradePrices().get(currency.toLowerCase());
        if (basePrice == null || economyHook == null || !economyHook.isValidCurrency(currency)) {
            return Result.INVALID_CURRENCY;
        }
        double price = calculateUpgradePrice(currentVip, fromType, currency);
        if (!economyHook.withdraw(player.getUniqueId(), currency, price)) {
            return Result.INSUFFICIENT_FUNDS;
        }

        boolean permanent = currentVip.isPermanent();
        long remaining = permanent ? 0 : currentVip.remainingMillis();

        dispatchAll(fromType.groupRemoveCmds(), Map.of("player", player.getName()));
        playerVipManager.remove(currentVip);

        long expiresAt = permanent ? 0 : System.currentTimeMillis() + remaining;
        PlayerVip newVip = new PlayerVip(-1, player.getUniqueId(), toType.id(), null, VipStatus.ACTIVE,
                System.currentTimeMillis(), expiresAt, remaining, false, 0);
        PlayerVip stored = playerVipManager.addVip(newVip).join();

        List<String> groupCommands = permanent ? toType.groupAddCmds() : toType.groupAddTempCmds();
        dispatchAll(groupCommands, Map.of("player", player.getName(), "time", TimeUtil.toLuckPermsDuration(remaining)));
        dispatchAll(toType.upgradeCommands(), Map.of("player", player.getName()));

        Map<String, String> placeholders = Map.of(
                "vip_from", TextUtil.plain(fromType.display()),
                "vip_to", TextUtil.plain(toType.display()),
                "player", player.getName()
        );
        if (!fromType.upgradeMessage().isBlank()) {
            player.sendMessage(TextUtil.legacyParse(configManager.prefix() + fromType.upgradeMessage(), placeholders));
        }
        if (!fromType.upgradeBroadcast().isBlank()) {
            Bukkit.broadcast(TextUtil.parse(fromType.upgradeBroadcast(), placeholders));
        }

        Bukkit.getPluginManager().callEvent(new VipUpgradeEvent(player, fromType, toType));
        return Result.SUCCESS;
    }

    /**
     * Compra mais tempo do mesmo tier ja ativo - sem trocar de VIP, so acumula duracao
     * (diferente de upgrade(), que troca para o tier superior).
     */
    public Result extend(Player player, PlayerVip currentVip, VipType type, String currency, long additionalDuration) {
        if (currency == null) {
            return Result.INVALID_CURRENCY;
        }
        Double price = type.upgradePrices().get(currency.toLowerCase());
        if (price == null || economyHook == null || !economyHook.isValidCurrency(currency)) {
            return Result.INVALID_CURRENCY;
        }
        if (!economyHook.withdraw(player.getUniqueId(), currency, price)) {
            return Result.INSUFFICIENT_FUNDS;
        }

        if (!currentVip.isPermanent()) {
            currentVip.expiresAt(currentVip.expiresAt() + additionalDuration);
            currentVip.totalDuration(currentVip.totalDuration() + additionalDuration);
        }
        playerVipManager.update(currentVip);
        return Result.SUCCESS;
    }

    private void dispatchAll(List<String> commands, Map<String, String> placeholders) {
        for (String command : commands) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), CommandUtil.substitute(command, placeholders));
        }
    }
}
