package com.alkacode.vips.gui;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.economy.CurrencyType;
import com.alkacode.vips.VipsServices;
import com.alkacode.vips.service.MarketplaceService;
import com.alkacode.vips.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public final class SellKeyMenu extends BaseGui {

    private final VipsServices services;
    private final String code;
    private final ItemStack physicalItem;
    private String currency = CurrencyType.COINS;
    private double price = -1;

    public SellKeyMenu(Player viewer, VipsServices services, String code, ItemStack physicalItem) {
        super(services.plugin, viewer, services.configManager.menus().getString("sell.title", "&8Vender Key"),
                services.configManager.menus().getInt("sell.size", 27) / 9, "vip_sell");
        this.services = services;
        this.code = code;
        this.physicalItem = physicalItem;
    }

    @Override
    public void render() {
        setItem(12, new ItemBuilder(Material.GOLD_NUGGET)
                .name("<yellow>Moeda: <white>" + currency.toUpperCase())
                .lore(List.of("<gray>Clique para trocar")).build(), e -> cycleCurrency());
        setItem(14, new ItemBuilder(Material.PAPER)
                .name("<yellow>Preco: <white>" + (price < 0 ? "Nao definido" : price))
                .lore(List.of("<gray>Clique para digitar o preco no chat")).build(), e -> promptPrice());
        setItem(16, new ItemBuilder(Material.LIME_WOOL).name("<green>Confirmar venda").build(), e -> confirm());
        fill(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build());
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
