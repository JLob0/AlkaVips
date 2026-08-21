package com.alkacode.vips.gui;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.vips.VipsServices;
import com.alkacode.vips.config.GuiLayout;
import com.alkacode.vips.model.PlayerVip;
import com.alkacode.vips.model.VipType;
import com.alkacode.vips.util.ItemBuilder;
import com.alkacode.vips.util.TextUtil;
import org.bukkit.entity.Player;

import java.util.List;

public final class PendingActivationsMenu extends BaseGui {

    private final VipsServices services;
    private final GuiLayout layout;
    private final int[] slots;

    public PendingActivationsMenu(Player viewer, VipsServices services) {
        super(services.plugin, viewer, services.configManager.menus().getString("pending.title", "&8Ativacoes Pendentes"),
                services.configManager.menus().getInt("pending.size", 27) / 9, "vip_pending");
        this.services = services;
        this.layout = services.configManager.layout("pending");
        this.slots = layout.findSlots('0').stream().mapToInt(Integer::intValue).toArray();
    }

    @Override
    public void render() {
        List<PlayerVip> pending = services.playerVipManager.getPendingVips(player.getUniqueId());
        if (pending.isEmpty()) {
            setItem(slots[13], services.configManager.menuItem("pending.empty"));
        } else {
            int slot = 0;
            for (PlayerVip vip : pending) {
                if (slot >= slots.length) {
                    break;
                }
                VipType type = services.vipTypeManager.get(vip.vipTypeId());
                String display = type != null ? TextUtil.plain(type.display()) : vip.vipTypeId();
                var item = new ItemBuilder(services.configManager.menuItem("pending.item"))
                        .name("<green>" + display)
                        .build();
                setItem(slots[slot], item, e -> {
                    services.activationService.collectPending(player, vip);
                    refresh();
                });
                slot++;
            }
        }
        setItem(layout.firstSlot('V'), services.configManager.menuItem("common.voltar"),
                e -> new MainVipMenu(player, services).open());
        fill(services.configManager.menuItem("fill-empty"));
    }
}
