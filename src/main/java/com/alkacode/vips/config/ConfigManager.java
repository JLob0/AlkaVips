package com.alkacode.vips.config;

import com.alkacode.vips.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ConfigManager {

    private final JavaPlugin plugin;

    private FileConfiguration config;
    private FileConfiguration messages;
    private FileConfiguration menus;
    private FileConfiguration guiLayoutsRaw;
    private final Map<String, GuiLayout> guiLayouts = new HashMap<>();

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        config = loadResource("config.yml");
        messages = loadResource("messages.yml");
        menus = loadResource("menus.yml");
        guiLayoutsRaw = loadResource("gui-layouts.yml");
        loadGuiLayouts();
    }

    private void loadGuiLayouts() {
        guiLayouts.clear();
        for (String key : guiLayoutsRaw.getKeys(false)) {
            int rows = guiLayoutsRaw.getInt(key + ".rows", 3);
            List<String> lines = guiLayoutsRaw.getStringList(key + ".layout");
            guiLayouts.put(key, new GuiLayout(rows, lines.toArray(new String[0])));
        }
    }

    /** Posicoes (grade ASCII) de uma GUI - ver gui-layouts.yml e {@link GuiLayout}. */
    public GuiLayout layout(String key) {
        GuiLayout found = guiLayouts.get(key);
        if (found == null) {
            throw new IllegalStateException("Layout '" + key + "' nao encontrado em gui-layouts.yml");
        }
        return found;
    }

    /** Icone (material/nome/lore) de menus.yml.&lt;path&gt;, sem placeholders. */
    public ItemStack menuItem(String path) {
        return menuItem(path, Map.of());
    }

    /** Icone (material/nome/lore) de menus.yml.&lt;path&gt; com placeholders {chave}
     * substituidos no nome/lore. Fallback BARRIER visivel se o path nao existir, em vez
     * de quebrar silenciosamente - sinaliza uma menus.yml desatualizada/mal editada. */
    public ItemStack menuItem(String path, Map<String, String> placeholders) {
        ConfigurationSection section = menus.getConfigurationSection(path);
        if (section == null) {
            return new ItemBuilder(Material.BARRIER).name("<red>menus.yml: '" + path + "' ausente").build();
        }
        ItemBuilder builder = ItemBuilder.fromSection(section);
        if (section.contains("name")) {
            builder.name(section.getString("name", ""), placeholders);
        }
        if (section.contains("lore")) {
            builder.lore(section.getStringList("lore"), placeholders);
        }
        return builder.build();
    }

    public void reload() {
        load();
    }

    private FileConfiguration loadResource(String name) {
        File file = new File(plugin.getDataFolder(), name);
        if (!file.exists()) {
            plugin.saveResource(name, false);
        }
        FileConfiguration loaded = YamlConfiguration.loadConfiguration(file);
        try (InputStream in = plugin.getResource(name)) {
            if (in != null) {
                loaded.setDefaults(YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)));
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Falha ao carregar defaults de " + name + ": " + e.getMessage());
        }
        return loaded;
    }

    public FileConfiguration config() { return config; }
    public FileConfiguration messages() { return messages; }
    public FileConfiguration menus() { return menus; }

    public String prefix() {
        return messages.getString("prefix", "");
    }

    public String message(String path) {
        return messages.getString(path, path);
    }

    public java.util.List<String> messageList(String path) {
        return messages.getStringList(path);
    }

    public boolean isAllowPartialUpgrade() {
        return config.getBoolean("upgrade.allow-partial", true);
    }

    public String getUpgradeCurrency() {
        return config.getString("upgrade.currency", "gold");
    }

    public java.util.List<Integer> getRenewalOptions() {
        return config.getIntegerList("renewal.options");
    }

    /** URL da loja usada no placeholder &lt;shop-url&gt; dos anuncios de ativacao - vazio por padrao (nao configurado). */
    public String shopUrl() {
        return config.getString("shop-url", "");
    }

    public String getDrakkarSeasonTag() {
        String prefix = config.getString("drakkar.season-prefix", "DRAKKAR");
        int season = config.getInt("drakkar.current-season", 1);
        return prefix + "_S" + season;
    }
}
