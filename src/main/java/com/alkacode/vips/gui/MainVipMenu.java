package com.alkacode.vips.gui;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.vips.VipsServices;
import com.alkacode.vips.model.PlayerVip;
import com.alkacode.vips.model.VipType;
import com.alkacode.vips.util.ItemBuilder;
import com.alkacode.vips.util.TextUtil;
import com.alkacode.vips.util.TimeUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class MainVipMenu extends BaseGui {

    private final VipsServices services;

    public MainVipMenu(Player viewer, VipsServices services) {
        super(services.plugin, viewer, services.configManager.menus().getString("main.title", "&8VIPs"),
                services.configManager.menus().getInt("main.size", 36) / 9, "vip_main");
        this.services = services;
    }

    @Override
    public void render() {
        UUID uuid = player.getUniqueId();
        List<PlayerVip> activeVips = services.playerVipManager.getActiveVips(uuid);

        if (!activeVips.isEmpty() && services.playerVipManager.dataOf(uuid).selectedVipId() == null) {
            services.playerVipManager.getSelectedVip(uuid)
                    .ifPresent(v -> services.playerVipManager.selectVip(uuid, v.id()));
        }
        Optional<PlayerVip> selected = services.playerVipManager.getSelectedVip(uuid);

        setItem(10, profileHead(activeVips, selected));

        setItem(11, new ItemBuilder(Material.WRITTEN_BOOK).name("<#AA55FF><bold>📜 Historico de VIPs").lore(List.of(
                "<gray>Visualize todo o historico de",
                "<gray>ativacoes, upgrades e expiracoes.",
                "",
                "<#AA55FF>Clique para abrir"
        )).build(), e -> new HistoryMenu(player, services).open());

        setItem(12, new ItemBuilder(Material.REDSTONE_TORCH).name("<#FFAA00><bold>🔥 Ativacoes Pendentes").lore(List.of(
                "<gray>VIPs comprados aguardando",
                "<gray>ativacao manual. Colete-os aqui!",
                "",
                "<#FFAA00>Clique para abrir"
        )).build(), e -> new PendingActivationsMenu(player, services).open());

        // Kits de ATIVACAO (bonus unico de 30+ dias por tier) sao uma GUI propria do
        // AlkaVips - ver VipKitsMenu/ActivationService#claimActivationBonus. Os kits
        // RECORRENTES (diario/semanal/mensal) moram no AlkaKits (/kits), gateados por
        // %alkavips_has_vip_<tier>% - o botao aqui NAO abre o /kits do AlkaKits, so a
        // GUI de ativacao.
        setItem(13, new ItemBuilder(Material.CHEST).name("<#55FF55><bold>🎁 Kits de Ativacao").lore(List.of(
                "<gray>Pegue os kits de ativacao",
                "<gray>desbloqueados pelos seus VIPs",
                "<gray>de 30+ dias.",
                "",
                "<gray>Kits diarios/semanais/mensais",
                "<gray>recorrentes ficam em <yellow>/kits<gray>.",
                "",
                "<#55FF55>Clique para abrir"
        )).build(), e -> new VipKitsMenu(player, services).open());

        if (selected.isPresent()) {
            PlayerVip selectedVip = selected.get();
            VipType current = services.vipTypeManager.get(selectedVip.vipTypeId());
            setItem(14, upgradeItem(current), e -> {
                if (current == null || !current.hasUpgrade() || !services.vipTypeManager.exists(current.upgradeTo())) {
                    services.sendMessage(player, "upgrade.no-upgrade-available", Map.of());
                    return;
                }
                new UpgradeMenu(player, services, selectedVip, current, UpgradeMenu.Mode.UPGRADE).open();
            });
        } else {
            setItem(14, upgradeItem(null));
        }

        setItem(15, partyItem(), e -> new PartyVipMenu(player, services).open());

        setItem(16, new ItemBuilder(Material.TRIPWIRE_HOOK).name("<#55AAFF><bold>🔑 Minhas Keys").lore(List.of(
                "<gray>Suas chaves de VIP guardadas.",
                "<gray>Ative ou venda conforme desejar.",
                "",
                "<#55AAFF>Clique para abrir"
        )).build(), e -> new KeysMenu(player, services).open());

        setItem(19, new ItemBuilder(Material.BOOK).name("<#FFD700><bold>💰 Carteira VIP").lore(List.of(
                "<gray>Veja seu historico, gastos",
                "<gray>e conquistas de VIP.",
                "",
                "<#FFD700>Clique para abrir"
        )).build(), e -> new WalletMenu(player, services).open());

        setItem(20, new ItemBuilder(Material.EMERALD_BLOCK).name("<#00AAFF><bold>🛒 Mercado de VIPs").lore(List.of(
                "<gray>Compre e venda assinaturas",
                "<gray>de VIP ativas com outros jogadores.",
                "",
                "<#00AAFF>Clique para abrir"
        )).build(), e -> new P2PMarketMenu(player, services).open());

        setItem(21, new ItemBuilder(Material.EMERALD).name("<#00FFAA><bold>✦ Loja Prisma").lore(List.of(
                "<gray>Troque seu Prisma por",
                "<gray>itens raros e valiosos!",
                "",
                "<#00FFAA>Clique para abrir"
        )).build(), e -> new ShopMenu(player, services).open());

        if (activeVips.size() >= 2) {
            setItem(22, new ItemBuilder(Material.COMPASS).name("<#FFAA00><bold>🔄 Trocar VIP").lore(List.of(
                    "<gray>Alterne entre seus VIPs ativos",
                    "<gray>para mudar prefixo e perks ativos.",
                    "",
                    "<#FFAA00>Clique para trocar"
            )).build(), e -> {
                services.playerVipManager.cycleSelectedVip(uuid);
                refresh();
            });
        }

        setItem(23, new ItemBuilder(Material.CHEST).name("<#FF5555><bold>🏪 Loja de Chaves").lore(List.of(
                "<gray>Compre chaves de VIP de outros",
                "<gray>jogadores no marketplace!",
                "",
                "<#FF5555>Clique para abrir"
        )).build(), e -> new MarketplaceMenu(player, services).open());

        setItem(24, new ItemBuilder(Material.GOLD_BLOCK).name("<#FFD700><bold>🏆 TOP VIP").lore(List.of(
                "<gray>Os jogadores mais dedicados da",
                "<gray>rede Alka. Suba no ranking!",
                "",
                "<#FFD700>Clique para visualizar"
        )).build(), e -> new TopMenu(player, services).open());

        // Arvore de Beneficios (Ideia 3) - desativada por padrao, so aparece se
        // perktree.yml#enabled=true (servidores com pegada de RPG/progression).
        if (services.perkTreeManager.enabled()) {
            setItem(25, new ItemBuilder(Material.ENCHANTED_BOOK).name("<#AA55FF><bold>🌳 Arvore de Beneficios").lore(List.of(
                    "<gray>Gaste pontos de perk ganhos",
                    "<gray>ativando VIP em beneficios extras.",
                    "",
                    "<#AA55FF>Clique para abrir"
            )).build(), e -> new PerkTreeMenu(player, services).open());
        }

        fill(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build());
    }

    private org.bukkit.inventory.ItemStack profileHead(List<PlayerVip> activeVips, Optional<PlayerVip> selected) {
        ItemBuilder builder = new ItemBuilder(Material.PLAYER_HEAD)
                .name("<#55AAFF><bold>✦ " + player.getName())
                .glow(!activeVips.isEmpty());
        if (activeVips.isEmpty()) {
            return builder.lore(List.of(
                    "<gray>─────────────────",
                    "<red>Voce ainda nao possui nenhum VIP ativo.",
                    "<gray>Adquira um em <yellow>/vip <gray>-> Loja de Chaves",
                    "<gray>─────────────────"
            )).build();
        }
        Long selectedId = selected.map(PlayerVip::id).orElse(null);
        List<String> lore = new ArrayList<>();
        lore.add("<gray>─────────────────");
        lore.add("<#FFD700><bold>👑 SEUS VIPS ATIVOS");
        lore.add("<gray>─────────────────");
        for (PlayerVip vip : activeVips) {
            VipType type = services.vipTypeManager.get(vip.vipTypeId());
            String display = type != null ? TextUtil.plain(type.display()) : vip.vipTypeId();
            String status = vip.isPermanent()
                    ? "Ativo (Permanente)"
                    : "Ativo, expira em " + TimeUtil.formatRemaining(vip.remainingMillis());
            boolean isSelected = selectedId != null && vip.id() == selectedId;
            if (isSelected) {
                lore.add("<#FFD700>⭐ <green><bold>[" + display + "] <gray>— " + status);
            } else {
                lore.add("<#555555>★ <gray>[" + display + "] <gray>— " + status);
            }
        }
        lore.add("<gray>─────────────────");
        lore.add("<#55AAFF>✦ Prisma: <white>" + services.creditManager.getCredits(player.getUniqueId()));
        lore.add("<gray>─────────────────");
        return builder.lore(lore).build();
    }

    private org.bukkit.inventory.ItemStack upgradeItem(VipType current) {
        List<String> lore = new ArrayList<>();
        lore.add("<gray>Evolua seu VIP atual para o");
        lore.add("<gray>proximo tier com preco proporcional!");
        lore.add("");
        if (current != null && current.hasUpgrade() && services.vipTypeManager.exists(current.upgradeTo())) {
            VipType toType = services.vipTypeManager.get(current.upgradeTo());
            lore.add("<#FF55FF>Proximo tier: <white>" + (toType != null ? TextUtil.plain(toType.display()) : current.upgradeTo()));
            lore.add("");
            lore.add("<#FF55FF>Clique para upar");
        } else if (current != null) {
            lore.add("<gray>Seu VIP ja esta no tier maximo.");
        } else {
            lore.add("<gray>Voce precisa de um VIP ativo");
            lore.add("<gray>selecionado para upar.");
        }
        return new ItemBuilder(Material.ENDER_CHEST).name("<#FF55FF><bold>⬆ Upar VIP").lore(lore).build();
    }

    private org.bukkit.inventory.ItemStack partyItem() {
        double percentage = services.partyVipManager.getPercentage();
        return new ItemBuilder(Material.COMPASS)
                .name("<#FFD700><bold>🎉 Party VIP")
                .lore(List.of(
                        "<gray>Contribua com Prisma para",
                        "<gray>desbloquear recompensas em grupo!",
                        "",
                        "<#FFD700>Progresso: <white>" + (int) percentage + "%",
                        "<gray>" + services.partyVipManager.getProgress() + "/" + services.partyVipManager.getGoal() + " Prisma",
                        "",
                        "<#FFD700>Clique para visualizar"
                )).build();
    }
}
