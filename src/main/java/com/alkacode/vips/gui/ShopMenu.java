package com.alkacode.vips.gui;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.vips.VipsServices;
import com.alkacode.vips.config.GuiLayout;
import com.alkacode.vips.util.ItemBuilder;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Map;

public final class ShopMenu extends BaseGui {

    private final VipsServices services;
    private final GuiLayout layout;
    private final int[] slots;

    public ShopMenu(Player viewer, VipsServices services) {
        super(services.plugin, viewer,
                services.configManager.config().getString("credit-shop.title", "<#00FFAA>✦ Loja Prisma"),
                services.configManager.menus().getInt("shop.size", 54) / 9, "vip_shop");
        this.services = services;
        this.layout = services.configManager.layout("shop");
        this.slots = layout.findSlots('0').stream().mapToInt(Integer::intValue).toArray();
    }

    @Override
    public void render() {
        ConfigurationSection items = services.configManager.config().getConfigurationSection("credit-shop.items");
        int slot = 0;
        if (items != null) {
            for (String key : items.getKeys(false)) {
                if (slot >= slots.length) {
                    break;
                }
                ConfigurationSection entry = items.getConfigurationSection(key);
                if (entry == null) {
                    continue;
                }
                double cost = entry.getDouble("cost", 0);
                var item = ItemBuilder.fromSection(entry)
                        .name(entry.getString("name", ""), Map.of("cost", String.valueOf(cost)))
                        .lore(entry.getStringList("lore"), Map.of("cost", String.valueOf(cost)))
                        .build();
                setItem(slots[slot], item, e -> buy(key, cost, entry));
                slot++;
            }
        }
        setItem(layout.firstSlot('V'), services.configManager.menuItem("common.voltar"),
                e -> new MainVipMenu(player, services).open());
        fill(services.configManager.menuItem("fill-empty"));
    }

    private void buy(String key, double cost, ConfigurationSection entry) {
        if (!services.creditManager.has(player.getUniqueId(), cost)) {
            services.sendMessage(player, "credit.insufficient", Map.of());
            return;
        }
        services.creditManager.remove(player.getUniqueId(), cost);
        player.getInventory().addItem(ItemBuilder.fromSection(entry).build());
        refresh();
    }
}
