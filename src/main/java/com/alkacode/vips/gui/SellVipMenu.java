package com.alkacode.vips.gui;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.economy.CurrencyType;
import com.alkacode.vips.VipsServices;
import com.alkacode.vips.config.GuiLayout;
import com.alkacode.vips.model.PlayerVip;
import com.alkacode.vips.model.VipType;
import com.alkacode.vips.service.P2PMarketService;
import com.alkacode.vips.util.ItemBuilder;
import com.alkacode.vips.util.TimeUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Anuncia o VIP ATIVO selecionado do jogador no mercado P2P - ver {@link P2PMarketMenu}. */
public final class SellVipMenu extends BaseGui {

    private final VipsServices services;
    private final GuiLayout layout;
    private String currency = CurrencyType.COINS;
    private double price = -1;

    public SellVipMenu(Player viewer, VipsServices services) {
        super(services.plugin, viewer, services.configManager.menus().getString("sell-vip.title", "&8Vender meu VIP"),
                services.configManager.menus().getInt("sell-vip.size", 27) / 9, "vip_sell_vip");
        this.services = services;
        this.layout = services.configManager.layout("sell-vip");
    }

    @Override
    public void render() {
        Optional<PlayerVip> selected = services.playerVipManager.getSelectedVip(player.getUniqueId());
        if (selected.isEmpty()) {
            setItem(layout.firstSlot('E'), services.configManager.menuItem("sell-vip.sem-selecionado"));
            setItem(layout.firstSlot('V'), services.configManager.menuItem("common.voltar"), e -> new P2PMarketMenu(player, services).open());
            fill(services.configManager.menuItem("fill-empty"));
            return;
        }
        PlayerVip vip = selected.get();
        VipType type = services.vipTypeManager.get(vip.vipTypeId());
        String display = type != null ? type.display() : vip.vipTypeId();
        String remaining = vip.isPermanent() ? "Permanente (nao pode ser vendido)" : TimeUtil.formatRemaining(vip.remainingMillis());

        setItem(layout.firstSlot('H'), new ItemBuilder(Material.PLAYER_HEAD).name("<white>" + display)
                .lore(List.of("<gray>Tempo restante: <white>" + remaining)).build());
        setItem(layout.firstSlot('C'), services.configManager.menuItem("sell-vip.moeda",
                Map.of("moeda", currency.toUpperCase())), e -> cycleCurrency());
        setItem(layout.firstSlot('P'), services.configManager.menuItem("sell-vip.preco",
                Map.of("preco", price < 0 ? "Nao definido" : String.valueOf(price))), e -> promptPrice(vip));
        setItem(layout.firstSlot('F'), services.configManager.menuItem("sell-vip.confirmar"), e -> confirm(vip));
        setItem(layout.firstSlot('V'), services.configManager.menuItem("common.voltar"), e -> new P2PMarketMenu(player, services).open());
        fill(services.configManager.menuItem("fill-empty"));
    }

    private void cycleCurrency() {
        List<String> ids = services.economyHook.currencyIds();
        if (ids.isEmpty()) return;
        int nextIndex = (ids.indexOf(currency) + 1) % ids.size();
        currency = ids.get(nextIndex);
        refresh();
    }

    private void promptPrice(PlayerVip vip) {
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

    private void confirm(PlayerVip vip) {
        if (price <= 0) {
            services.sendMessage(player, "market.invalid-price", Map.of("value", String.valueOf(price)));
            return;
        }
        P2PMarketService.ListResult result = services.p2pMarketService.list(player, vip, price, currency);
        String path = switch (result) {
            case SUCCESS -> "p2p.listed";
            case NO_VIP_SELECTED -> "p2p.no-vip-selected";
            case PERMANENT_NOT_SELLABLE -> "p2p.permanent-not-sellable";
            case ALREADY_LISTED -> "p2p.already-listed";
            case NOT_SELLABLE_TIER -> "p2p.not-sellable-tier";
        };
        services.sendMessage(player, path, Map.of());
        player.closeInventory();
    }
}
