package com.alkacode.vips.gui;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.vips.VipsServices;
import com.alkacode.vips.config.GuiLayout;
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
    private final GuiLayout layout;

    public MainVipMenu(Player viewer, VipsServices services) {
        super(services.plugin, viewer, services.configManager.menus().getString("main.title", "&8VIPs"),
                services.configManager.menus().getInt("main.size", 36) / 9, "vip_main");
        this.services = services;
        this.layout = services.configManager.layout("main");
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

        setItem(layout.firstSlot('P'), profileHead(activeVips, selected));

        setItem(layout.firstSlot('H'), services.configManager.menuItem("main.history"),
                e -> new HistoryMenu(player, services).open());

        setItem(layout.firstSlot('D'), services.configManager.menuItem("main.pending"),
                e -> new PendingActivationsMenu(player, services).open());

        // Kits de ATIVACAO (bonus unico de 30+ dias por tier) sao uma GUI propria do
        // AlkaVips - ver VipKitsMenu/ActivationService#claimActivationBonus. Os kits
        // RECORRENTES (diario/semanal/mensal) moram no AlkaKits (/kits), gateados por
        // %alkavips_has_vip_<tier>% - o botao aqui NAO abre o /kits do AlkaKits, so a
        // GUI de ativacao.
        setItem(layout.firstSlot('K'), services.configManager.menuItem("main.kits"),
                e -> new VipKitsMenu(player, services).open());

        if (selected.isPresent()) {
            PlayerVip selectedVip = selected.get();
            VipType current = services.vipTypeManager.get(selectedVip.vipTypeId());
            setItem(layout.firstSlot('U'), upgradeItem(current), e -> {
                if (current == null || !current.hasUpgrade() || !services.vipTypeManager.exists(current.upgradeTo())) {
                    services.sendMessage(player, "upgrade.no-upgrade-available", Map.of());
                    return;
                }
                new UpgradeMenu(player, services, selectedVip, current, UpgradeMenu.Mode.UPGRADE).open();
            });
        } else {
            setItem(layout.firstSlot('U'), upgradeItem(null));
        }

        setItem(layout.firstSlot('A'), partyItem(), e -> new PartyVipMenu(player, services).open());

        setItem(layout.firstSlot('Y'), services.configManager.menuItem("main.keys"),
                e -> new KeysMenu(player, services).open());

        setItem(layout.firstSlot('W'), services.configManager.menuItem("main.wallet"),
                e -> new WalletMenu(player, services).open());

        setItem(layout.firstSlot('M'), services.configManager.menuItem("main.p2p-market"),
                e -> new P2PMarketMenu(player, services).open());

        setItem(layout.firstSlot('S'), services.configManager.menuItem("main.shop"),
                e -> new ShopMenu(player, services).open());

        if (activeVips.size() >= 2) {
            setItem(layout.firstSlot('T'), services.configManager.menuItem("main.switch-vip"), e -> {
                services.playerVipManager.cycleSelectedVip(uuid);
                refresh();
            });
        }

        setItem(layout.firstSlot('L'), services.configManager.menuItem("main.marketplace"),
                e -> new MarketplaceMenu(player, services).open());

        setItem(layout.firstSlot('O'), services.configManager.menuItem("main.top"),
                e -> new TopMenu(player, services).open());

        // Arvore de Beneficios (Ideia 3) - desativada por padrao, so aparece se
        // perktree.yml#enabled=true (servidores com pegada de RPG/progression).
        if (services.perkTreeManager.enabled()) {
            setItem(layout.firstSlot('E'), services.configManager.menuItem("main.perk-tree"),
                    e -> new PerkTreeMenu(player, services).open());
        }

        fill(services.configManager.menuItem("fill-empty"));
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
        String path;
        Map<String, String> placeholders = Map.of();
        if (current != null && current.hasUpgrade() && services.vipTypeManager.exists(current.upgradeTo())) {
            VipType toType = services.vipTypeManager.get(current.upgradeTo());
            path = "main.upgrade.lore-has-upgrade";
            placeholders = Map.of("proximo", toType != null ? TextUtil.plain(toType.display()) : current.upgradeTo());
        } else if (current != null) {
            path = "main.upgrade.lore-maxed";
        } else {
            path = "main.upgrade.lore-none";
        }
        var section = services.configManager.menus().getConfigurationSection("main.upgrade");
        return ItemBuilder.fromSection(section)
                .lore(services.configManager.menus().getStringList(path), placeholders)
                .build();
    }

    private org.bukkit.inventory.ItemStack partyItem() {
        double percentage = services.partyVipManager.getPercentage();
        return ItemBuilder.fromSection(services.configManager.menus().getConfigurationSection("main.party"))
                .lore(services.configManager.menus().getStringList("main.party.lore"), Map.of(
                        "percentual", String.valueOf((int) percentage),
                        "progresso", String.valueOf(services.partyVipManager.getProgress()),
                        "meta", String.valueOf(services.partyVipManager.getGoal())
                )).build();
    }
}
