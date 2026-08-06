package com.alkacode.vips.gui;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.vips.VipsServices;
import com.alkacode.vips.model.PlayerVip;
import com.alkacode.vips.model.VipType;
import com.alkacode.vips.model.enums.VipStatus;
import com.alkacode.vips.util.ItemBuilder;
import com.alkacode.vips.util.TimeUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class HistoryMenu extends BaseGui {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            .withZone(ZoneId.systemDefault());

    private final VipsServices services;

    public HistoryMenu(Player viewer, VipsServices services) {
        super(services.plugin, viewer, services.configManager.menus().getString("history.title", "&8Historico de VIPs"),
                services.configManager.menus().getInt("history.size", 54) / 9, "vip_history");
        this.services = services;
    }

    @Override
    public void render() {
        List<PlayerVip> vips = services.playerVipManager.getVips(player.getUniqueId());
        int slot = 0;
        for (PlayerVip vip : vips) {
            if (slot >= getInventory().getSize() - 9) {
                break;
            }
            VipType type = services.vipTypeManager.get(vip.vipTypeId());
            String title = type != null ? tierBadge(type) + " " + type.prefix() : "<white>" + vip.vipTypeId();
            Material material = vip.status() == VipStatus.EXPIRED ? Material.GRAY_DYE
                    : vip.status() == VipStatus.PENDING ? Material.CLOCK : Material.LIME_DYE;
            var item = new ItemBuilder(material)
                    .glow(vip.status() != VipStatus.EXPIRED)
                    .name(title)
                    .lore(List.of(
                            "<gray>─────────────────",
                            "<gray>Ativado: <white>" + DATE_FORMAT.format(java.time.Instant.ofEpochMilli(vip.activatedAt())),
                            "<gray>Duracao: <white>" + durationText(vip),
                            "<gray>Status: " + statusLine(vip),
                            "<gray>─────────────────"
                    ))
                    .build();
            setItem(slot, item);
            slot++;
        }
        setItem(getInventory().getSize() - 5, new ItemBuilder(Material.BARRIER).name("<red>Voltar").build(),
                e -> new MainVipMenu(player, services).open());
        fill(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build());
    }

    /** Distintivo de rank do tier a partir de {@link VipType#getOrder()} - data-driven em vez de
     * uma lista fixa de nomes, pra nao quebrar quando vips.yml ganhar/perder tiers. Tag-permanent
     * (hoje so DRAKKAR) ganha coroa em vez de estrelas, ja que fica no topo da cadeia. */
    private String tierBadge(VipType type) {
        if (type.isTagPermanent()) {
            return "👑";
        }
        return "★".repeat(Math.max(1, type.getOrder()));
    }

    private String statusLine(PlayerVip vip) {
        return switch (vip.status()) {
            case ACTIVE -> vip.isPermanent() ? "<green>Ativo (Permanente)"
                    : "<green>Ativo, expira em <white>" + TimeUtil.formatRemaining(vip.remainingMillis());
            case EXPIRED -> "<red>Expirou em <white>" + DATE_FORMAT.format(java.time.Instant.ofEpochMilli(vip.expiresAt()));
            case PENDING -> "<yellow>Pendente de ativacao";
        };
    }

    private String durationText(PlayerVip vip) {
        return vip.totalDuration() <= 0 ? "Permanente" : TimeUtil.formatRemaining(vip.totalDuration());
    }
}
