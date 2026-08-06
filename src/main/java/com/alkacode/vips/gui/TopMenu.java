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

        if (top.isEmpty()) {
            setItem(13, new ItemBuilder(Material.BARRIER).name("<red>Nenhum dado disponivel").build());
        } else {
            int slot = 0;
            for (EconomyRepository.TopBalanceEntry entry : top) {
                if (slot >= 7) break;
                setItem(11 + slot, medal(slot + 1, entry));
                slot++;
            }
        }
        setItem(22, new ItemBuilder(Material.BARRIER).name("<red>Voltar").build(),
                e -> new MainVipMenu(player, services).open());
        fill(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build());
    }

    private org.bukkit.inventory.ItemStack medal(int position, EconomyRepository.TopBalanceEntry entry) {
        String name = "Desconhecido";
        if (entry.uuid() != null) {
            org.bukkit.OfflinePlayer offline = Bukkit.getOfflinePlayer(entry.uuid());
            if (offline != null && offline.getName() != null) {
                name = offline.getName();
            }
        }
        int activations = entry.uuid() != null ? services.playerVipManager.dataOf(entry.uuid()).totalActivations() : 0;

        Material material;
        String title;
        boolean glow;
        switch (position) {
            case 1 -> {
                material = Material.GOLD_BLOCK;
                title = "<#FFD700><bold>🥇 " + name;
                glow = true;
            }
            case 2 -> {
                material = Material.IRON_BLOCK;
                title = "<#AAAAAA><bold>🥈 " + name;
                glow = true;
            }
            case 3 -> {
                material = Material.COPPER_BLOCK;
                title = "<#FFAA55><bold>🥉 " + name;
                glow = true;
            }
            default -> {
                material = Material.PLAYER_HEAD;
                title = "<#55AAFF>" + position + "º Lugar <white>- " + name;
                glow = false;
            }
        }

        return new ItemBuilder(material)
                .glow(glow)
                .name(title)
                .lore(List.of(
                        "<gray>─────────────────",
                        "<#FFD700>✦ Essencia Alka: <white>" + services.economyHook.format(entry.balance()),
                        "<#55AAFF>Ativacoes: <white>" + activations,
                        "<gray>─────────────────"
                ))
                .build();
    }
}
