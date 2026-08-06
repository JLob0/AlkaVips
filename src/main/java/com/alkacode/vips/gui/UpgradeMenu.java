package com.alkacode.vips.gui;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.vips.VipsServices;
import com.alkacode.vips.model.PlayerVip;
import com.alkacode.vips.model.VipType;
import com.alkacode.vips.service.UpgradeService;
import com.alkacode.vips.util.ItemBuilder;
import com.alkacode.vips.util.TextUtil;
import com.alkacode.vips.util.TimeUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public final class UpgradeMenu extends BaseGui {

    public enum Mode { UPGRADE, EXTEND }

    private final VipsServices services;
    private final PlayerVip currentVip;
    private final VipType fromType;
    private final Mode mode;

    public UpgradeMenu(Player viewer, VipsServices services, PlayerVip currentVip, VipType fromType, Mode mode) {
        super(services.plugin, viewer, title(services, fromType, mode), 3, "vip_upgrade");
        this.services = services;
        this.currentVip = currentVip;
        this.fromType = fromType;
        this.mode = mode;
    }

    private static String title(VipsServices services, VipType fromType, Mode mode) {
        if (mode == Mode.EXTEND) {
            return "<yellow>Extender " + TextUtil.plain(fromType.display());
        }
        VipType toType = services.vipTypeManager.get(fromType.upgradeTo());
        String toName = toType != null ? TextUtil.plain(toType.display()) : fromType.upgradeTo();
        return "<yellow>Upar para " + toName;
    }

    @Override
    public void render() {
        if (mode == Mode.EXTEND) {
            renderExtend();
        } else {
            renderUpgrade();
        }
        fill(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build());
    }

    private void renderUpgrade() {
        VipType toType = services.vipTypeManager.get(fromType.upgradeTo());
        int slot = 10;
        for (Map.Entry<String, Double> entry : fromType.upgradePrices().entrySet()) {
            if (slot > 16) {
                break;
            }
            String currency = entry.getKey();
            double price = services.upgradeService.calculateUpgradePrice(currentVip, fromType, currency);
            var item = new ItemBuilder(Material.GOLD_NUGGET)
                    .name("<yellow>Pagar com " + currency.toUpperCase())
                    .lore(List.of("<gray>Preco: <white>" + services.economyHook.format(price),
                            "<gray>Upar para: <white>" + (toType != null ? TextUtil.plain(toType.display()) : fromType.upgradeTo())))
                    .build();
            setItem(slot, item, e -> attemptUpgrade(currency));
            slot++;
        }
    }

    private void renderExtend() {
        long additionalDuration = currentVip.totalDuration();
        int slot = 10;
        for (Map.Entry<String, Double> entry : fromType.upgradePrices().entrySet()) {
            if (slot > 16) {
                break;
            }
            String currency = entry.getKey();
            double price = entry.getValue();
            var item = new ItemBuilder(Material.GOLD_NUGGET)
                    .name("<yellow>Pagar com " + currency.toUpperCase())
                    .lore(List.of("<gray>Adiciona <white>" + TimeUtil.formatRemaining(additionalDuration),
                            "<gray>Preco: <white>" + services.economyHook.format(price)))
                    .build();
            setItem(slot, item, e -> attemptExtend(currency, additionalDuration));
            slot++;
        }
    }

    private void attemptUpgrade(String currency) {
        UpgradeService.Result result = services.upgradeService.upgrade(player, currentVip, fromType, currency);
        String path = switch (result) {
            case SUCCESS -> "upgrade.success-private";
            case NO_UPGRADE -> "upgrade.no-upgrade-available";
            case INVALID_CURRENCY -> "market.invalid-currency";
            case INSUFFICIENT_FUNDS -> "upgrade.insufficient-funds";
        };
        VipType toType = services.vipTypeManager.get(fromType.upgradeTo());
        services.sendMessage(player, path, Map.of(
                "vip", toType != null ? TextUtil.plain(toType.display()) : "",
                "currency", currency
        ));
        player.closeInventory();
    }

    private void attemptExtend(String currency, long additionalDuration) {
        UpgradeService.Result result = services.upgradeService.extend(player, currentVip, fromType, currency, additionalDuration);
        String path = switch (result) {
            case SUCCESS -> "upgrade.extended";
            case NO_UPGRADE -> "upgrade.no-upgrade-available";
            case INVALID_CURRENCY -> "market.invalid-currency";
            case INSUFFICIENT_FUNDS -> "upgrade.insufficient-funds";
        };
        services.sendMessage(player, path, Map.of(
                "vip", TextUtil.plain(fromType.display()),
                "currency", currency,
                "time", TimeUtil.formatRemaining(additionalDuration)
        ));
        player.closeInventory();
    }
}
