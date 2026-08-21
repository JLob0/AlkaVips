package com.alkacode.vips.gui;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.vips.VipsServices;
import com.alkacode.vips.config.GuiLayout;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class KeysMenu extends BaseGui {

    private final VipsServices services;
    private final GuiLayout layout;
    private final int[] slots;
    private final int page;

    public KeysMenu(Player viewer, VipsServices services) {
        this(viewer, services, 0);
    }

    public KeysMenu(Player viewer, VipsServices services, int page) {
        super(services.plugin, viewer, services.configManager.menus().getString("keys.title", "&8Minhas Keys"),
                services.configManager.menus().getInt("keys.size", 54) / 9, "vip_keys");
        this.services = services;
        this.layout = services.configManager.layout("keys");
        this.slots = layout.findSlots('0').stream().mapToInt(Integer::intValue).toArray();
        this.page = page;
    }

    @Override
    public void render() {
        List<ItemStack> keyItems = new ArrayList<>();
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && services.keyManager.readKeyId(item) != null) {
                keyItems.add(item);
            }
        }

        int from = page * slots.length;
        for (int i = 0; i < slots.length; i++) {
            int index = from + i;
            if (index < keyItems.size()) {
                setItem(slots[i], keyItems.get(index).clone());
            }
        }

        setItem(layout.firstSlot('V'), services.configManager.menuItem("common.voltar"),
                e -> new MainVipMenu(player, services).open());
        if (page > 0) {
            setItem(layout.firstSlot('P'), services.configManager.menuItem("common.anterior"),
                    e -> new KeysMenu(player, services, page - 1).open());
        }
        if (from + slots.length < keyItems.size()) {
            setItem(layout.firstSlot('N'), services.configManager.menuItem("common.proximo"),
                    e -> new KeysMenu(player, services, page + 1).open());
        }

        fill(services.configManager.menuItem("fill-empty"));
    }
}
