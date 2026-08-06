package com.alkacode.vips.gui;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.vips.VipsServices;
import com.alkacode.vips.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class KeysMenu extends BaseGui {

    private static final int[] SLOTS = {11, 12, 13, 14, 15, 20, 21, 22, 23, 24};

    private final VipsServices services;
    private final int page;

    public KeysMenu(Player viewer, VipsServices services) {
        this(viewer, services, 0);
    }

    public KeysMenu(Player viewer, VipsServices services, int page) {
        super(services.plugin, viewer, services.configManager.menus().getString("keys.title", "&8Minhas Keys"),
                services.configManager.menus().getInt("keys.size", 54) / 9, "vip_keys");
        this.services = services;
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

        int from = page * SLOTS.length;
        for (int i = 0; i < SLOTS.length; i++) {
            int index = from + i;
            if (index < keyItems.size()) {
                setItem(SLOTS[i], keyItems.get(index).clone());
            }
        }

        setItem(49, new ItemBuilder(Material.BARRIER).name("<red>Voltar").build(), e -> new MainVipMenu(player, services).open());
        if (page > 0) {
            setItem(18, new ItemBuilder(Material.ARROW).name("<white>Anterior").build(),
                    e -> new KeysMenu(player, services, page - 1).open());
        }
        if (from + SLOTS.length < keyItems.size()) {
            setItem(26, new ItemBuilder(Material.ARROW).name("<white>Proximo").build(),
                    e -> new KeysMenu(player, services, page + 1).open());
        }

        fill(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build());
    }
}
