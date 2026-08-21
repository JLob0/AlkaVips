package com.alkacode.vips.gui;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.vips.VipsServices;
import com.alkacode.vips.config.GuiLayout;
import com.alkacode.vips.model.VipType;
import com.alkacode.vips.service.P2PMarketService;
import com.alkacode.vips.storage.VipsRepository;
import com.alkacode.vips.util.ItemBuilder;
import com.alkacode.vips.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

/** Mercado P2P de assinaturas ATIVAS (nao confundir com {@link MarketplaceMenu}, que vende keys nao usadas). */
public final class P2PMarketMenu extends BaseGui {

    private final VipsServices services;
    private final GuiLayout layout;
    private final int[] slots;
    private final int page;

    public P2PMarketMenu(Player viewer, VipsServices services) {
        this(viewer, services, 0);
    }

    public P2PMarketMenu(Player viewer, VipsServices services, int page) {
        super(services.plugin, viewer, services.configManager.menus().getString("p2p-market.title", "&8Mercado de VIPs"),
                services.configManager.menus().getInt("p2p-market.size", 54) / 9, "vip_p2p_market");
        this.services = services;
        this.layout = services.configManager.layout("p2p-market");
        this.slots = layout.findSlots('0').stream().mapToInt(Integer::intValue).toArray();
        this.page = page;
    }

    @Override
    public void render() {
        List<VipsRepository.P2PListing> listings = services.p2pMarketService.allListings();
        if (listings.isEmpty()) {
            setItem(slots[22], services.configManager.menuItem("p2p-market.empty"));
        }
        int from = page * slots.length;
        for (int i = 0; i < slots.length; i++) {
            int index = from + i;
            if (index >= listings.size()) break;
            VipsRepository.P2PListing listing = listings.get(index);
            VipType type = services.vipTypeManager.get(listing.tierId());
            if (type == null) continue;
            String sellerName = Bukkit.getOfflinePlayer(listing.sellerUuid()).getName();
            List<String> lore = new java.util.ArrayList<>(List.of(
                    "<gray>Vendedor: <white>" + sellerName,
                    "<gray>Tempo restante: <white>" + TimeUtil.formatRemaining(listing.remainingMillis()),
                    "<gray>Preco: <white>" + services.economyHook.format(listing.price()) + " (" + listing.currency() + ")"
            ));
            List<String> permissionLore = services.permissionLoreLines(type);
            if (!permissionLore.isEmpty()) {
                lore.add("");
                lore.add("<#00AAFF>ᴘᴇʀᴍɪssões");
                lore.addAll(permissionLore);
            }
            lore.add("");
            lore.add("<green>Clique para comprar");
            var item = new ItemBuilder(type.icon().build())
                    .name(type.display())
                    .lore(lore).build();
            setItem(slots[i], item, e -> buy(listing));
        }

        setItem(layout.firstSlot('V'), services.configManager.menuItem("common.voltar"), e -> new MainVipMenu(player, services).open());
        setItem(layout.firstSlot('S'), services.configManager.menuItem("p2p-market.vender-meu-vip"),
                e -> new SellVipMenu(player, services).open());
        if (page > 0) {
            setItem(layout.firstSlot('P'), services.configManager.menuItem("common.anterior"),
                    e -> new P2PMarketMenu(player, services, page - 1).open());
        }
        if (from + slots.length < listings.size()) {
            setItem(layout.firstSlot('N'), services.configManager.menuItem("common.proximo"),
                    e -> new P2PMarketMenu(player, services, page + 1).open());
        }
        fill(services.configManager.menuItem("fill-empty"));
    }

    private void buy(VipsRepository.P2PListing listing) {
        P2PMarketService.BuyResult result = services.p2pMarketService.buy(player, listing.id());
        String path = switch (result) {
            case SUCCESS -> "p2p.buy-success";
            case NOT_FOUND -> "p2p.not-found";
            case NOT_ENOUGH_CURRENCY -> "market.invalid-price";
            case SELLER_NO_LONGER_HAS_VIP -> "p2p.seller-lost-vip";
            case CANNOT_BUY_OWN -> "p2p.cannot-buy-own";
        };
        services.sendMessage(player, path, Map.of());
        refresh();
    }
}
