package com.alkacode.vips.gui;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.vips.VipsServices;
import com.alkacode.vips.config.GuiLayout;
import com.alkacode.vips.model.VipKey;
import com.alkacode.vips.model.VipType;
import com.alkacode.vips.util.ItemBuilder;
import org.bukkit.entity.Player;

import java.util.List;

public final class MarketplaceMenu extends BaseGui {

    private final VipsServices services;
    private final GuiLayout layout;
    private final int[] slots;
    private final int page;

    public MarketplaceMenu(Player viewer, VipsServices services) {
        this(viewer, services, 0);
    }

    public MarketplaceMenu(Player viewer, VipsServices services, int page) {
        super(services.plugin, viewer, services.configManager.menus().getString("marketplace.title", "&8Loja de Chaves"),
                services.configManager.menus().getInt("marketplace.size", 54) / 9, "vip_marketplace");
        this.services = services;
        this.layout = services.configManager.layout("marketplace");
        this.slots = layout.findSlots('0').stream().mapToInt(Integer::intValue).toArray();
        this.page = page;
    }

    @Override
    public void render() {
        List<VipKey> listings = services.marketplaceService.listings().join();
        if (listings.isEmpty()) {
            setItem(slots[22], services.configManager.menuItem("marketplace.empty"));
        }
        int from = page * slots.length;
        for (int i = 0; i < slots.length; i++) {
            int index = from + i;
            if (index >= listings.size()) {
                break;
            }
            VipKey key = listings.get(index);
            VipType type = services.vipTypeManager.get(key.vipTypeId());
            if (type == null) {
                continue;
            }
            String sellerName = org.bukkit.Bukkit.getOfflinePlayer(key.sellerUuid()).getName();
            var item = new ItemBuilder(services.keyManager.buildItem(key, type))
                    .lore(List.of(
                            "<gray>Vendedor: <white>" + sellerName,
                            "<gray>Preco: <white>" + services.economyHook.format(key.sellPrice()) + " (" + key.economyProvider() + ")",
                            "",
                            "<green>Clique para comprar"
                    ))
                    .build();
            setItem(slots[i], item, e -> new ConfirmBuyMenu(player, services, key).open());
        }
        setItem(layout.firstSlot('V'), services.configManager.menuItem("common.voltar"), e -> new MainVipMenu(player, services).open());
        if (page > 0) {
            setItem(layout.firstSlot('P'), services.configManager.menuItem("common.anterior"),
                    e -> new MarketplaceMenu(player, services, page - 1).open());
        }
        if (from + slots.length < listings.size()) {
            setItem(layout.firstSlot('N'), services.configManager.menuItem("common.proximo"),
                    e -> new MarketplaceMenu(player, services, page + 1).open());
        }
        fill(services.configManager.menuItem("fill-empty"));
    }
}
