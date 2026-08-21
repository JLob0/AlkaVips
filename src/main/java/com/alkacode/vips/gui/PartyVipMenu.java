package com.alkacode.vips.gui;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.vips.VipsServices;
import com.alkacode.vips.config.GuiLayout;
import org.bukkit.entity.Player;

import java.util.Map;

public final class PartyVipMenu extends BaseGui {

    private final VipsServices services;
    private final GuiLayout layout;
    private final int[] slots;

    public PartyVipMenu(Player viewer, VipsServices services) {
        super(services.plugin, viewer, services.configManager.menus().getString("party.title", "&8Party VIP"),
                services.configManager.menus().getInt("party.size", 27) / 9, "vip_party");
        this.services = services;
        this.layout = services.configManager.layout("party");
        this.slots = layout.findSlots('0').stream().mapToInt(Integer::intValue).toArray();
    }

    @Override
    public void render() {
        double percentage = services.partyVipManager.getPercentage();
        int filled = (int) Math.round(percentage / 100.0 * slots.length);
        for (int i = 0; i < slots.length; i++) {
            String path = i < filled ? "party.cheio" : "party.vazio";
            setItem(slots[i], services.configManager.menuItem(path));
        }
        setItem(layout.firstSlot('S'), services.configManager.menuItem("party.status", Map.of(
                "percentual", String.valueOf((int) percentage),
                "progresso", String.valueOf(services.partyVipManager.getProgress()),
                "meta", String.valueOf(services.partyVipManager.getGoal())
        )));
        fill(services.configManager.menuItem("fill-empty"));
    }
}
