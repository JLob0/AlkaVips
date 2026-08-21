package com.alkacode.vips.gui;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.vips.VipsServices;
import com.alkacode.vips.config.GuiLayout;
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
    private final GuiLayout layout;
    private final int[] slots;

    public HistoryMenu(Player viewer, VipsServices services) {
        super(services.plugin, viewer, services.configManager.menus().getString("history.title", "&8Historico de VIPs"),
                services.configManager.menus().getInt("history.size", 54) / 9, "vip_history");
        this.services = services;
        this.layout = services.configManager.layout("history");
        this.slots = layout.findSlots('0').stream().mapToInt(Integer::intValue).toArray();
    }

    @Override
    public void render() {
        List<PlayerVip> vips = services.playerVipManager.getVips(player.getUniqueId());
        int slot = 0;
        for (PlayerVip vip : vips) {
            if (slot >= slots.length) {
                break;
            }
            VipType type = services.vipTypeManager.get(vip.vipTypeId());
            String title = type != null ? type.prefix() : "<white>" + vip.vipTypeId();
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
            setItem(slots[slot], item);
            slot++;
        }
        setItem(layout.firstSlot('V'), services.configManager.menuItem("common.voltar"),
                e -> new MainVipMenu(player, services).open());
        fill(services.configManager.menuItem("fill-empty"));
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
