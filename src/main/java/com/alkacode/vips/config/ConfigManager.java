package com.alkacode.vips.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public final class ConfigManager {

    private final JavaPlugin plugin;

    private FileConfiguration config;
    private FileConfiguration messages;
    private FileConfiguration menus;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        config = loadResource("config.yml");
        messages = loadResource("messages.yml");
        menus = loadResource("menus.yml");
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
        return config.getString("upgrade.currency", "coins");
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
