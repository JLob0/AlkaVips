package com.alkacode.vips.gui;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.vips.VipsServices;
import com.alkacode.vips.config.GuiLayout;
import com.alkacode.vips.model.VipKey;
import com.alkacode.vips.model.VipType;
import com.alkacode.vips.service.MarketplaceService;
import org.bukkit.entity.Player;

import java.util.Map;

public final class ConfirmBuyMenu extends BaseGui {

    private final VipsServices services;
    private final GuiLayout layout;
    private final VipKey key;

    public ConfirmBuyMenu(Player viewer, VipsServices services, VipKey key) {
        super(services.plugin, viewer, services.configManager.menus().getString("confirm-buy.title", "&8Confirmar Compra"),
                services.configManager.menus().getInt("confirm-buy.size", 27) / 9, "vip_confirm_buy");
        this.services = services;
        this.layout = services.configManager.layout("confirm-buy");
        this.key = key;
    }

    @Override
    public void render() {
        VipType type = services.vipTypeManager.get(key.vipTypeId());
        setItem(layout.firstSlot('C'), services.configManager.menuItem("confirm-buy.confirmar"), e -> confirm());
        if (type != null) {
            setItem(layout.firstSlot('K'), services.keyManager.buildItem(key, type));
        }
        setItem(layout.firstSlot('X'), services.configManager.menuItem("confirm-buy.cancelar"), e -> player.closeInventory());
        setItem(layout.firstSlot('V'), services.configManager.menuItem("common.voltar"),
                e -> new MarketplaceMenu(player, services).open());
        fill(services.configManager.menuItem("fill-empty"));
    }

    private void confirm() {
        MarketplaceService.BuyResult result = services.marketplaceService.buy(player, key.id());
        String path = switch (result) {
            case SUCCESS -> "market.bought";
            case NOT_FOUND -> "key.not-found";
            case NOT_FOR_SALE -> "market.not-for-sale";
            case BUY_OWN -> "market.buy-own";
            case INSUFFICIENT_FUNDS -> "market.insufficient-funds";
        };
        services.sendMessage(player, path, Map.of(
                "code", key.id(),
                "price", services.economyHook != null ? services.economyHook.format(key.sellPrice()) : "",
                "currency", key.economyProvider() == null ? "" : key.economyProvider()
        ));
        player.closeInventory();
    }
}
