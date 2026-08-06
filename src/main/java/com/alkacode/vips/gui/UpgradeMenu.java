package com.alkacode.vips.gui;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.vips.VipsServices;
import com.alkacode.vips.model.PlayerVip;
import com.alkacode.vips.model.VipType;
import com.alkacode.vips.service.UpgradeService;
import com.alkacode.vips.util.ItemBuilder;
import com.alkacode.vips.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public final class UpgradeMenu extends BaseGui {

    private final VipsServices services;
    private final PlayerVip currentVip;
    private final VipType fromType;

    public UpgradeMenu(Player viewer, VipsServices services, PlayerVip currentVip, VipType fromType) {
        super(services.plugin, viewer, "<dark_gray>Upar VIP", 3, "vip_upgrade");
        this.services = services;
        this.currentVip = currentVip;
        this.fromType = fromType;
    }

    @Override
    public void render() {
        VipType toType = services.vipTypeManager.get(fromType.upgradeTo());
        int slot = 10;
        for (Map.Entry<String, Double> entry : fromType.upgradePrices().entrySet()) {
            if (slot > 16) {
                break;
            }
            String currency = entry.getKey();
            double price = entry.getValue();
            var item = new ItemBuilder(Material.GOLD_NUGGET)
                    .name("<yellow>Pagar com " + currency.toUpperCase())
                    .lore(List.of("<gray>Preco: <white>" + services.economyHook.format(price),
                            "<gray>Upar para: <white>" + (toType != null ? TextUtil.plain(toType.display()) : fromType.upgradeTo())))
                    .build();
            setItem(slot, item, e -> attempt(currency));
            slot++;
        }
        fill(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build());
    }

    private void attempt(String currency) {
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
}
