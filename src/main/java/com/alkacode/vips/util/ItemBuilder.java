package com.alkacode.vips.util;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;

public final class ItemBuilder {

    private final ItemStack stack;

    public ItemBuilder(Material material) {
        this(new ItemStack(material));
    }

    public ItemBuilder(ItemStack base) {
        this.stack = base.clone();
    }

    public static ItemBuilder fromSection(ConfigurationSection section) {
        Material material = Material.matchMaterial(section.getString("material", "STONE"));
        ItemBuilder builder = new ItemBuilder(material != null ? material : Material.STONE);
        if (section.contains("amount")) {
            builder.amount(section.getInt("amount", 1));
        }
        if (section.contains("name")) {
            builder.name(section.getString("name"));
        }
        if (section.contains("lore")) {
            builder.lore(section.getStringList("lore"));
        }
        return builder;
    }

    public ItemBuilder amount(int amount) {
        stack.setAmount(Math.max(1, amount));
        return this;
    }

    public ItemBuilder name(String raw) {
        return name(raw, Map.of());
    }

    public ItemBuilder name(String raw, Map<String, String> placeholders) {
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(TextUtil.parse(raw, placeholders));
        stack.setItemMeta(meta);
        return this;
    }

    public ItemBuilder lore(List<String> raw) {
        return lore(raw, Map.of());
    }

    public ItemBuilder lore(List<String> raw, Map<String, String> placeholders) {
        ItemMeta meta = stack.getItemMeta();
        meta.lore(TextUtil.parseList(raw, placeholders));
        stack.setItemMeta(meta);
        return this;
    }

    public ItemBuilder glow(boolean glow) {
        if (glow) {
            ItemMeta meta = stack.getItemMeta();
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
            stack.setItemMeta(meta);
        }
        return this;
    }

    public ItemStack build() {
        return stack;
    }
}
