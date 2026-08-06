package com.alkacode.vips.gui;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.vips.VipsServices;
import com.alkacode.vips.model.VipKey;
import com.alkacode.vips.model.VipType;
import com.alkacode.vips.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

public final class MarketplaceMenu extends BaseGui {

    private static final int[] SLOTS = buildSlots();

    private final VipsServices services;
    private final int page;

    public MarketplaceMenu(Player viewer, VipsServices services) {
        this(viewer, services, 0);
    }

    public MarketplaceMenu(Player viewer, VipsServices services, int page) {
        super(services.plugin, viewer, services.configManager.menus().getString("marketplace.title", "&8Loja de Chaves"),
                services.configManager.menus().getInt("marketplace.size", 54) / 9, "vip_marketplace");
        this.services = services;
        this.page = page;
    }

    private static int[] buildSlots() {
        int[] slots = new int[45];
        for (int i = 0; i < 45; i++) {
            slots[i] = i;
        }
        return slots;
    }

    @Override
    public void render() {
        List<VipKey> listings = services.marketplaceService.listings().join();
        if (listings.isEmpty()) {
            setItem(22, new ItemBuilder(Material.BARRIER).name("<yellow>Nenhuma key a venda").build());
        }
        int from = page * SLOTS.length;
        for (int i = 0; i < SLOTS.length; i++) {
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
            setItem(SLOTS[i], item, e -> new ConfirmBuyMenu(player, services, key).open());
        }
        setItem(49, new ItemBuilder(Material.BARRIER).name("<red>Voltar").build(), e -> new MainVipMenu(player, services).open());
        if (page > 0) {
            setItem(45, new ItemBuilder(Material.ARROW).name("<white>Anterior").build(),
                    e -> new MarketplaceMenu(player, services, page - 1).open());
        }
        if (from + SLOTS.length < listings.size()) {
            setItem(53, new ItemBuilder(Material.ARROW).name("<white>Proximo").build(),
                    e -> new MarketplaceMenu(player, services, page + 1).open());
        }
        fill(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build());
    }
}
