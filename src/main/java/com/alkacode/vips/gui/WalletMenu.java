package com.alkacode.vips.gui;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.vips.VipsServices;
import com.alkacode.vips.model.Achievement;
import com.alkacode.vips.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * "Carteira VIP / Hall da Fama" (Ideia 5) - resumo agregado (dias totais, gasto por
 * moeda) + grade de conquistas. Nao pagina o historico completo de ativacoes
 * (vip_history) nesta versao - so os agregados, pra manter o escopo razoavel; dado
 * ja fica disponivel via {@code services.walletManager.history(uuid)} pra uma GUI
 * de historico detalhado futura.
 */
public final class WalletMenu extends BaseGui {

    private static final int[] ACHIEVEMENT_SLOTS = {19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};

    private final VipsServices services;

    public WalletMenu(Player viewer, VipsServices services) {
        super(services.plugin, viewer, services.configManager.menus().getString("wallet.title", "&8Carteira VIP"),
                services.configManager.menus().getInt("wallet.size", 54) / 9, "vip_wallet");
        this.services = services;
    }

    @Override
    public void render() {
        fill(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build());

        int totalDays = services.walletManager.totalDays(player.getUniqueId());
        Map<String, Double> spent = services.walletManager.totalSpent(player.getUniqueId());
        List<String> spentLore = new ArrayList<>();
        if (spent.isEmpty()) {
            spentLore.add("<gray>Nenhuma compra registrada ainda.");
        } else {
            spent.forEach((currency, amount) -> spentLore.add("<gray>" + currency + ": <white>" + amount));
        }

        setItem(10, new ItemBuilder(Material.CLOCK).name("<gold>Tempo Total de VIP")
                .lore(List.of("<white>" + totalDays + " dia(s) acumulados")).build());
        setItem(12, new ItemBuilder(Material.GOLD_INGOT).name("<gold>Total Investido").lore(spentLore).build());
        setItem(14, new ItemBuilder(Material.BOOK).name("<gold>Historico de Ativacoes")
                .lore(List.of("<white>" + services.walletManager.history(player.getUniqueId()).size() + " registro(s)")).build());

        Set<String> claimed = services.walletManager.claimedAchievements(player.getUniqueId());
        List<Achievement> achievements = services.walletManager.all();
        for (int i = 0; i < ACHIEVEMENT_SLOTS.length && i < achievements.size(); i++) {
            Achievement achievement = achievements.get(i);
            boolean unlocked = claimed.contains(achievement.id());
            setItem(ACHIEVEMENT_SLOTS[i], unlocked
                    ? new ItemBuilder(Material.NETHER_STAR).name(achievement.name())
                        .lore(List.of("<green>Conquistada")).build()
                    : new ItemBuilder(Material.GRAY_DYE).name("<gray>???")
                        .lore(List.of("<gray>Conquista bloqueada")).build());
        }

        setItem(49, new ItemBuilder(Material.ARROW).name("<red>Voltar").build(), e -> new MainVipMenu(player, services).open());
    }
}
