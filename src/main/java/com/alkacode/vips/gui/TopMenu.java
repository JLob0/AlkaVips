package com.alkacode.vips.gui;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.economy.storage.EconomyRepository;
import com.alkacode.vips.VipsServices;
import com.alkacode.vips.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

public final class TopMenu extends BaseGui {

    private final VipsServices services;

    public TopMenu(Player viewer, VipsServices services) {
        super(services.plugin, viewer, services.configManager.menus().getString("top.title", "&8TOP VIP"),
                services.configManager.menus().getInt("top.size", 27) / 9, "vip_top");
        this.services = services;
    }

    @Override
    public void render() {
        List<EconomyRepository.TopBalanceEntry> top = services.creditManager.getTop(7);

        int slot = 0;
        for (EconomyRepository.TopBalanceEntry entry : top) {
            if (slot >= 7) break;
            setItem(11 + slot, medal(entry));
            slot++;
        }
        fill(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build());
    }

    private org.bukkit.inventory.ItemStack medal(EconomyRepository.TopBalanceEntry entry) {
        String name = Bukkit.getOfflinePlayer(entry.uuid()).getName();
        int activations = services.playerVipManager.dataOf(entry.uuid()).totalActivations();
        return new ItemBuilder(Material.PLAYER_HEAD)
                .name("<white>" + (name != null ? name : entry.uuid()))
                .lore(List.of(
                        "<gray>Creditos: <white>" + services.economyHook.format(entry.balance()),
                        "<gray>Ativacoes: <white>" + activations
                ))
                .build();
    }
}
