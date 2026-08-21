package com.alkacode.vips.gui;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.economy.CurrencyType;
import com.alkacode.vips.VipsServices;
import com.alkacode.vips.config.GuiLayout;
import com.alkacode.vips.service.MarketplaceService;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public final class SellKeyMenu extends BaseGui {

    private final VipsServices services;
    private final GuiLayout layout;
    private final String code;
    private final ItemStack physicalItem;
    private String currency = CurrencyType.COINS;
    private double price = -1;

    public SellKeyMenu(Player viewer, VipsServices services, String code, ItemStack physicalItem) {
        super(services.plugin, viewer, services.configManager.menus().getString("sell.title", "&8Vender Key"),
                services.configManager.menus().getInt("sell.size", 27) / 9, "vip_sell");
        this.services = services;
        this.layout = services.configManager.layout("sell");
        this.code = code;
        this.physicalItem = physicalItem;
    }

    @Override
    public void render() {
        setItem(layout.firstSlot('C'), services.configManager.menuItem("sell.moeda",
                Map.of("moeda", currency.toUpperCase())), e -> cycleCurrency());
        setItem(layout.firstSlot('P'), services.configManager.menuItem("sell.preco",
                Map.of("preco", price < 0 ? "Nao definido" : String.valueOf(price))), e -> promptPrice());
        setItem(layout.firstSlot('F'), services.configManager.menuItem("sell.confirmar"), e -> confirm());
        fill(services.configManager.menuItem("fill-empty"));
    }

    private void cycleCurrency() {
        List<String> ids = services.economyHook.currencyIds();
        if (ids.isEmpty()) {
            return;
        }
        int nextIndex = (ids.indexOf(currency) + 1) % ids.size();
        currency = ids.get(nextIndex);
        refresh();
    }

    private void promptPrice() {
        player.closeInventory();
        services.sendMessage(player, "market.invalid-price", Map.of("value", "Digite o preco no chat:"));
        services.chatInputManager.await(player.getUniqueId(), input -> {
            try {
                price = Double.parseDouble(input.trim().replace(",", "."));
            } catch (NumberFormatException e) {
                price = -1;
            }
            open();
        });
    }

    private void confirm() {
        if (price <= 0) {
            services.sendMessage(player, "market.invalid-price", Map.of("value", String.valueOf(price)));
            return;
        }
        MarketplaceService.ListResult result = services.marketplaceService.listForSale(
                player, code, currency, price);
        if (result == MarketplaceService.ListResult.SUCCESS) {
            physicalItem.setAmount(physicalItem.getAmount() - 1);
        }
        String path = switch (result) {
            case SUCCESS -> "market.listed";
            case NOT_FOUND -> "key.not-found";
            case ALREADY_USED -> "key.already-used";
            case NOT_ALLOWED -> "market.not-for-sale";
            case ALREADY_FOR_SALE -> "market.already-for-sale";
            case INVALID_CURRENCY -> "market.invalid-currency";
            case INVALID_PRICE -> "market.invalid-price";
        };
        services.sendMessage(player, path, Map.of(
                "code", code,
                "currency", currency,
                "price", String.valueOf(price),
                "value", String.valueOf(price)
        ));
        player.closeInventory();
    }
}
