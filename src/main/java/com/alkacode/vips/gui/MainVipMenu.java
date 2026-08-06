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

import java.util.List;
import java.util.Optional;

public final class MainVipMenu extends BaseGui {

    private final VipsServices services;

    public MainVipMenu(Player viewer, VipsServices services) {
        super(services.plugin, viewer, services.configManager.menus().getString("main.title", "&8VIPs"),
                services.configManager.menus().getInt("main.size", 36) / 9, "vip_main");
        this.services = services;
    }

    @Override
    public void render() {
        Optional<PlayerVip> selected = services.playerVipManager.getSelectedVip(player.getUniqueId());
        setItem(10, profileHead(selected));
        setItem(12, new ItemBuilder(Material.BOOK).name("<white>Historico de VIPs").build(),
                e -> new HistoryMenu(player, services).open());
        setItem(13, new ItemBuilder(Material.TRIPWIRE_HOOK).name("<white>Minhas Keys").build(),
                e -> new KeysMenu(player, services).open());
        setItem(14, new ItemBuilder(Material.EMERALD).name("<white>Loja de Creditos").build(),
                e -> new ShopMenu(player, services).open());
        setItem(15, new ItemBuilder(Material.CHEST).name("<white>Loja de Chaves").build(),
                e -> new MarketplaceMenu(player, services).open());
        setItem(16, new ItemBuilder(Material.GOLDEN_HELMET).name("<white>TOP VIPs").build(),
                e -> new TopMenu(player, services).open());
        setItem(21, new ItemBuilder(Material.ENDER_CHEST).name("<white>Ativacoes Pendentes").build(),
                e -> new PendingActivationsMenu(player, services).open());

        if (selected.isPresent()) {
            VipType current = services.vipTypeManager.get(selected.get().vipTypeId());
            if (current != null && current.hasUpgrade() && services.vipTypeManager.exists(current.upgradeTo())) {
                setItem(22, new ItemBuilder(Material.NETHER_STAR).name("<yellow>Upar VIP").build(),
                        e -> new UpgradeMenu(player, services, selected.get(), current).open());
            }
        }

        setItem(30, new ItemBuilder(Material.CHEST).name("<white>Kits VIP").build(),
                e -> new KitsMenu(player, services).open());
        setItem(31, partyItem(), e -> new PartyVipMenu(player, services).open());

        fill(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build());
    }

    private org.bukkit.inventory.ItemStack profileHead(Optional<PlayerVip> selected) {
        ItemBuilder builder = new ItemBuilder(Material.PLAYER_HEAD).name("<white>" + player.getName());
        if (selected.isEmpty()) {
            return builder.lore(services.configManager.messageList("info.no-vips")).build();
        }
        PlayerVip vip = selected.get();
        VipType type = services.vipTypeManager.get(vip.vipTypeId());
        String display = type != null ? TextUtil.plain(type.display()) : vip.vipTypeId();
        String remaining = vip.isPermanent() ? "Permanente" : TimeUtil.formatRemaining(vip.remainingMillis());
        return builder.lore(List.of(
                "<gray>VIP: <white>" + display,
                "<gray>Creditos: <white>" + services.creditManager.getCredits(player.getUniqueId()),
                "<gray>Expira em: <white>" + remaining
        )).build();
    }

    private org.bukkit.inventory.ItemStack partyItem() {
        double percentage = services.partyVipManager.getPercentage();
        return new ItemBuilder(Material.NETHER_STAR)
                .name("<light_purple>Party VIP")
                .lore(List.of(
                        "<gray>Progresso: <white>" + (int) percentage + "%",
                        "<gray>" + services.partyVipManager.getProgress() + "/" + services.partyVipManager.getGoal()
                )).build();
    }
}
