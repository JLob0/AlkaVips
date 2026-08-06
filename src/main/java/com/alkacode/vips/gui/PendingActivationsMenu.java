package com.alkacode.vips.gui;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.vips.VipsServices;
import com.alkacode.vips.model.PlayerVip;
import com.alkacode.vips.model.VipType;
import com.alkacode.vips.util.ItemBuilder;
import com.alkacode.vips.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

public final class PendingActivationsMenu extends BaseGui {

    private final VipsServices services;

    public PendingActivationsMenu(Player viewer, VipsServices services) {
        super(services.plugin, viewer, services.configManager.menus().getString("pending.title", "&8Ativacoes Pendentes"),
                services.configManager.menus().getInt("pending.size", 27) / 9, "vip_pending");
        this.services = services;
    }

    @Override
    public void render() {
        List<PlayerVip> pending = services.playerVipManager.getPendingVips(player.getUniqueId());
        int slot = 0;
        for (PlayerVip vip : pending) {
            if (slot >= getInventory().getSize()) {
                break;
            }
            VipType type = services.vipTypeManager.get(vip.vipTypeId());
            String display = type != null ? TextUtil.plain(type.display()) : vip.vipTypeId();
            var item = new ItemBuilder(Material.CHEST_MINECART)
                    .name("<green>" + display)
                    .lore(List.of("<gray>Clique para coletar!"))
                    .build();
            setItem(slot, item, e -> {
                services.activationService.collectPending(player, vip);
                refresh();
            });
            slot++;
        }
        fill(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build());
    }
}
