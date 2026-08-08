package com.alkacode.vips.gui;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.vips.VipsServices;
import com.alkacode.vips.model.VipKey;
import com.alkacode.vips.model.VipType;
import com.alkacode.vips.service.MarketplaceService;
import com.alkacode.vips.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Map;

public final class ConfirmBuyMenu extends BaseGui {

    private final VipsServices services;
    private final VipKey key;

    public ConfirmBuyMenu(Player viewer, VipsServices services, VipKey key) {
        super(services.plugin, viewer, services.configManager.menus().getString("confirm-buy.title", "&8Confirmar Compra"),
                services.configManager.menus().getInt("confirm-buy.size", 27) / 9, "vip_confirm_buy");
        this.services = services;
        this.key = key;
    }

    @Override
    public void render() {
        VipType type = services.vipTypeManager.get(key.vipTypeId());
        setItem(11, new ItemBuilder(Material.LIME_WOOL).name("<green>Confirmar").build(), e -> confirm());
        if (type != null) {
            setItem(13, services.keyManager.buildItem(key, type));
        }
        setItem(15, new ItemBuilder(Material.RED_WOOL).name("<red>Cancelar").build(), e -> player.closeInventory());
        setItem(22, new ItemBuilder(Material.ARROW).name("<red>Voltar").build(),
                e -> new MarketplaceMenu(player, services).open());
        fill(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build());
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
