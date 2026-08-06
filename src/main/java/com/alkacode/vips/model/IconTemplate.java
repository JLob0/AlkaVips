package com.alkacode.vips.model;

import com.alkacode.vips.util.ItemBuilder;
import com.alkacode.vips.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

/**
 * Guarda o texto cru (name/lore) do icone em vez de um ItemStack ja construido, para
 * que placeholders como {@code <id>}/{@code <expire_remaining>} sejam substituidos por
 * instancia (cada key/PlayerVip tem seu proprio valor) em vez de "congelados" no
 * carregamento do vips.yml.
 */
public record IconTemplate(Material material, int amount, String name, List<String> lore, boolean glow) {

    public static IconTemplate fromSection(ConfigurationSection section) {
        if (section == null) {
            return new IconTemplate(Material.STONE, 1, "", List.of(), false);
        }
        Material material = Material.matchMaterial(section.getString("material", "STONE"));
        return new IconTemplate(
                material != null ? material : Material.STONE,
                section.getInt("amount", 1),
                section.getString("name", ""),
                section.getStringList("lore"),
                section.getBoolean("glow", false)
        );
    }

    public ItemStack build(Map<String, String> placeholders) {
        return new ItemBuilder(material)
                .amount(amount)
                .name(name, placeholders)
                .lore(lore, placeholders)
                .glow(glow)
                .build();
    }

    public ItemStack build() {
        return build(Map.of());
    }

    public String plainName(Map<String, String> placeholders) {
        return TextUtil.plain(TextUtil.replace(name, placeholders));
    }
}
