package com.alkacode.vips.gui;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.vips.VipsServices;
import com.alkacode.vips.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

public final class PartyVipMenu extends BaseGui {

    private final VipsServices services;

    public PartyVipMenu(Player viewer, VipsServices services) {
        super(services.plugin, viewer, services.configManager.menus().getString("party.title", "&8Party VIP"),
                services.configManager.menus().getInt("party.size", 27) / 9, "vip_party");
        this.services = services;
    }

    @Override
    public void render() {
        double percentage = services.partyVipManager.getPercentage();
        int filled = (int) Math.round(percentage / 100.0 * 7);
        for (int i = 0; i < 7; i++) {
            Material material = i < filled ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
            setItem(10 + i, new ItemBuilder(material).name(" ").build());
        }
        setItem(22, new ItemBuilder(Material.NETHER_STAR)
                .name("<light_purple>Progresso: <white>" + (int) percentage + "%")
                .lore(List.of("<gray>" + services.partyVipManager.getProgress() + " / " + services.partyVipManager.getGoal()))
                .build());
        fill(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build());
    }
}
