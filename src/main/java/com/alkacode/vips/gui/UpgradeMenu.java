package com.alkacode.vips.gui;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.vips.VipsServices;
import com.alkacode.vips.config.GuiLayout;
import com.alkacode.vips.model.PlayerVip;
import com.alkacode.vips.model.VipType;
import com.alkacode.vips.service.UpgradeService;
import com.alkacode.vips.util.ItemBuilder;
import com.alkacode.vips.util.TextUtil;
import com.alkacode.vips.util.TimeUtil;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public final class UpgradeMenu extends BaseGui {

    public enum Mode { UPGRADE, EXTEND }

    private final VipsServices services;
    private final GuiLayout layout;
    private final PlayerVip currentVip;
    private final VipType fromType;
    private final Mode mode;

    public UpgradeMenu(Player viewer, VipsServices services, PlayerVip currentVip, VipType fromType, Mode mode) {
        super(services.plugin, viewer, title(services, fromType, mode),
                services.configManager.menus().getInt("upgrade.size", 27) / 9, "vip_upgrade");
        this.services = services;
        this.layout = services.configManager.layout("upgrade");
        this.currentVip = currentVip;
        this.fromType = fromType;
        this.mode = mode;
    }

    private static String title(VipsServices services, VipType fromType, Mode mode) {
        if (mode == Mode.EXTEND) {
            return services.configManager.menus().getString("upgrade.title-extend", "<yellow>Extender {vip}")
                    .replace("{vip}", TextUtil.plain(fromType.display()));
        }
        VipType toType = services.vipTypeManager.get(fromType.upgradeTo());
        String toName = toType != null ? TextUtil.plain(toType.display()) : fromType.upgradeTo();
        return services.configManager.menus().getString("upgrade.title-upgrade", "<yellow>Upar para {vip}")
                .replace("{vip}", toName);
    }

    @Override
    public void render() {
        if (mode == Mode.EXTEND) {
            renderExtend();
        } else {
            renderUpgrade();
        }
        setItem(layout.firstSlot('V'), services.configManager.menuItem("common.voltar"),
                e -> new MainVipMenu(player, services).open());
        fill(services.configManager.menuItem("fill-empty"));
    }

    private void renderUpgrade() {
        VipType toType = services.vipTypeManager.get(fromType.upgradeTo());
        int[] slots = layout.findSlots('0').stream().mapToInt(Integer::intValue).toArray();
        int slot = 0;
        for (Map.Entry<String, Double> entry : fromType.upgradePrices().entrySet()) {
            if (slot >= slots.length) {
                break;
            }
            String currency = entry.getKey();
            double price = services.upgradeService.calculateUpgradePrice(currentVip, fromType, currency);
            boolean canAfford = services.economyHook.has(player.getUniqueId(), currency, price);
            var item = new ItemBuilder(services.configManager.menuItem(canAfford ? "upgrade.disponivel" : "upgrade.bloqueado"))
                    .glow(canAfford)
                    .lore(List.of(
                            "<gray>Pagar com <white>" + currency.toUpperCase(),
                            "<gray>Preco: <white>" + services.economyHook.format(price),
                            "<gray>Upar para: <white>" + (toType != null ? TextUtil.plain(toType.display()) : fromType.upgradeTo())))
                    .build();
            setItem(slots[slot], item, e -> attemptUpgrade(currency));
            slot++;
        }
    }

    private void renderExtend() {
        long additionalDuration = currentVip.totalDuration();
        int[] slots = layout.findSlots('0').stream().mapToInt(Integer::intValue).toArray();
        int slot = 0;
        for (Map.Entry<String, Double> entry : fromType.upgradePrices().entrySet()) {
            if (slot >= slots.length) {
                break;
            }
            String currency = entry.getKey();
            double price = entry.getValue();
            boolean canAfford = services.economyHook.has(player.getUniqueId(), currency, price);
            var item = new ItemBuilder(services.configManager.menuItem(canAfford ? "upgrade.disponivel" : "upgrade.bloqueado"))
                    .glow(canAfford)
                    .lore(List.of(
                            "<gray>Pagar com <white>" + currency.toUpperCase(),
                            "<gray>Adiciona <white>" + TimeUtil.formatRemaining(additionalDuration),
                            "<gray>Preco: <white>" + services.economyHook.format(price)))
                    .build();
            setItem(slots[slot], item, e -> attemptExtend(currency, additionalDuration));
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
