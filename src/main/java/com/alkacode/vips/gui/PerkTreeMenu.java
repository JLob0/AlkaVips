package com.alkacode.vips.gui;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.vips.VipsServices;
import com.alkacode.vips.manager.PerkTreeManager;
import com.alkacode.vips.model.PerkNode;
import com.alkacode.vips.storage.VipsRepository;
import com.alkacode.vips.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * "Arvore de Beneficios do VIP" (Ideia 3) - DESATIVADA POR PADRAO, so acessivel se
 * {@code perktree.yml#enabled} for true (o comando/item que abre esse menu ja
 * verifica antes de chamar {@code open()}). Layout simples de grade unica (nao
 * pagina por branch) porque o numero de perks tende a ser pequeno - ver
 * PerkTreeManager pra limitacao de escopo (efeito so REGISTRADO, nao aplicado).
 */
public final class PerkTreeMenu extends BaseGui {

    private static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34};

    private final VipsServices services;

    public PerkTreeMenu(Player viewer, VipsServices services) {
        super(services.plugin, viewer, services.configManager.menus().getString("perk-tree.title", "&8Arvore de Beneficios"),
                services.configManager.menus().getInt("perk-tree.size", 54) / 9, "vip_perk_tree");
        this.services = services;
    }

    @Override
    public void render() {
        fill(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build());

        PerkTreeManager perkTree = services.perkTreeManager;
        VipsRepository.PerkPoints points = perkTree.pointsOf(player.getUniqueId());
        Set<String> unlocked = perkTree.unlockedPerks(player.getUniqueId());

        setItem(4, new ItemBuilder(Material.EXPERIENCE_BOTTLE).name("<#FFD700><bold>✦ Pontos de Perk")
                .lore(List.of(
                        "<gray>Disponiveis: <white>" + points.available(),
                        "<gray>Total ganho: <white>" + points.earned(),
                        "",
                        "<gray>Ganhe pontos ativando VIPs",
                        "<gray>(ver points-per-tier em perktree.yml)"
                )).build());

        List<PerkNode> all = perkTree.all();
        for (int i = 0; i < SLOTS.length && i < all.size(); i++) {
            PerkNode perk = all.get(i);
            boolean isUnlocked = unlocked.contains(perk.id());
            boolean hasPrereq = perk.requiresPerkId().isBlank() || unlocked.contains(perk.requiresPerkId());

            List<String> lore = new ArrayList<>();
            lore.add("<gray>Ramo: <white>" + perkTree.branchName(perk.branchId()));
            lore.add("<gray>Custo: <white>" + perk.cost() + " ponto(s)");
            if (!perk.requiresPerkId().isBlank()) {
                lore.add("<gray>Requer: <white>" + perk.requiresPerkId());
            }
            lore.add("");
            if (isUnlocked) {
                lore.add("<#55FF55><bold>✔ Desbloqueado</bold>");
            } else if (!hasPrereq) {
                lore.add("<#FF5555>Requisito nao atendido");
            } else if (points.available() < perk.cost()) {
                lore.add("<#FFAA00>Pontos insuficientes");
            } else {
                lore.add("<#55FF55>Clique para desbloquear");
            }

            var item = new ItemBuilder(isUnlocked ? Material.ENCHANTED_BOOK : Material.BOOK)
                    .name((isUnlocked ? "<#55FF55>" : "<white>") + perk.name())
                    .glow(isUnlocked)
                    .lore(lore).build();
            setItem(SLOTS[i], item, e -> unlock(perk));
        }

        setItem(49, new ItemBuilder(Material.ARROW).name("<red>Voltar").build(), e -> new MainVipMenu(player, services).open());
    }

    private void unlock(PerkNode perk) {
        PerkTreeManager.UnlockResult result = services.perkTreeManager.unlock(player.getUniqueId(), perk.id());
        String path = switch (result) {
            case SUCCESS -> "perk-tree.unlocked";
            case ALREADY_UNLOCKED -> "perk-tree.already-unlocked";
            case NOT_ENOUGH_POINTS -> "perk-tree.not-enough-points";
            case MISSING_PREREQUISITE -> "perk-tree.missing-prerequisite";
            case NOT_FOUND -> "perk-tree.not-found";
            case DISABLED -> "perk-tree.disabled";
        };
        services.sendMessage(player, path, Map.of("perk", perk.name()));
        refresh();
    }
}
