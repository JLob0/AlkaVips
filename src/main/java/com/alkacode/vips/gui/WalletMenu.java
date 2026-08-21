package com.alkacode.vips.gui;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.vips.VipsServices;
import com.alkacode.vips.config.GuiLayout;
import com.alkacode.vips.model.Achievement;
import com.alkacode.vips.util.ItemBuilder;
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

    private final VipsServices services;
    private final GuiLayout layout;
    private final int[] achievementSlots;

    public WalletMenu(Player viewer, VipsServices services) {
        super(services.plugin, viewer, services.configManager.menus().getString("wallet.title", "&8Carteira VIP"),
                services.configManager.menus().getInt("wallet.size", 54) / 9, "vip_wallet");
        this.services = services;
        this.layout = services.configManager.layout("wallet");
        this.achievementSlots = layout.findSlots('0').stream().mapToInt(Integer::intValue).toArray();
    }

    @Override
    public void render() {
        fill(services.configManager.menuItem("fill-empty"));

        int totalDays = services.walletManager.totalDays(player.getUniqueId());
        Map<String, Double> spent = services.walletManager.totalSpent(player.getUniqueId());
        List<String> spentLore = new ArrayList<>();
        if (spent.isEmpty()) {
            spentLore.add("<gray>Nenhuma compra registrada ainda.");
        } else {
            spent.forEach((currency, amount) -> spentLore.add("<gray>" + currency + ": <white>" + amount));
        }

        setItem(layout.firstSlot('D'), new ItemBuilder(services.configManager.menuItem("wallet.tempo-total"))
                .lore(List.of("<white>" + totalDays + " dia(s) acumulados")).build());
        setItem(layout.firstSlot('I'), new ItemBuilder(services.configManager.menuItem("wallet.total-investido")).lore(spentLore).build());
        setItem(layout.firstSlot('H'), new ItemBuilder(services.configManager.menuItem("wallet.historico"))
                .lore(List.of("<white>" + services.walletManager.history(player.getUniqueId()).size() + " registro(s)")).build());

        Set<String> claimed = services.walletManager.claimedAchievements(player.getUniqueId());
        List<Achievement> achievements = services.walletManager.all();
        for (int i = 0; i < achievementSlots.length && i < achievements.size(); i++) {
            Achievement achievement = achievements.get(i);
            boolean unlocked = claimed.contains(achievement.id());
            setItem(achievementSlots[i], unlocked
                    ? new ItemBuilder(services.configManager.menuItem("wallet.conquista-desbloqueada")).name(achievement.name()).build()
                    : services.configManager.menuItem("wallet.conquista-bloqueada"));
        }

        setItem(layout.firstSlot('V'), services.configManager.menuItem("common.voltar"), e -> new MainVipMenu(player, services).open());
    }
}
