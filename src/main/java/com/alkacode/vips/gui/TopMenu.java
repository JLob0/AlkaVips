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
        setItem(22, new ItemBuilder(Material.ARROW).name("<red>Voltar").build(),
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

        String[] lore = {
                "<gray>─────────────────",
                "<#FFD700>✦ Prisma: <white>" + services.economyHook.format(entry.balance()),
                "<#55AAFF>Ativacoes: <white>" + activations,
                "<gray>─────────────────"
        };

        // Cabeca com o rosto real do jogador em TODAS as posicoes (BaseGui#head resolve via
        // OfflinePlayer) - antes o top 3 usava blocos (ouro/ferro/cobre) sem nenhuma cabeca,
        // e 4+ usava Material.PLAYER_HEAD cru via ItemBuilder local sem dono/textura setado,
        // entao nunca aparecia rosto nenhum em lugar nenhum.
        String title = switch (position) {
            case 1 -> "<#FFD700><bold>🥇 " + name;
            case 2 -> "<#AAAAAA><bold>🥈 " + name;
            case 3 -> "<#FFAA55><bold>🥉 " + name;
            default -> "<#55AAFF>" + position + "º Lugar <white>- " + name;
        };

        org.bukkit.inventory.ItemStack item = head(name, title, lore);
        if (position <= 3) {
            item = glow(item);
        }
        return item;
    }
}
