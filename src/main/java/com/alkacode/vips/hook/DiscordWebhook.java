package com.alkacode.vips.hook;

import com.alkacode.vips.config.ConfigManager;
import com.alkacode.vips.util.CommandUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.logging.Level;

public final class DiscordWebhook {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;

    public DiscordWebhook(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void sendEmbed(String embedId, Map<String, String> placeholders) {
        String webhookUrl = configManager.config().getString("discord.webhook-url", "");
        if (webhookUrl.isBlank()) {
            return;
        }
        ConfigurationSection embed = configManager.config().getConfigurationSection("discord.embeds." + embedId);
        if (embed == null) {
            return;
        }
        String title = CommandUtil.substitute(embed.getString("title", ""), placeholders);
        String description = CommandUtil.substitute(embed.getString("description", ""), placeholders);
        String color = embed.getString("color", "#FFFFFF");
        int colorDecimal = parseColor(color);

        String payload = """
                {"embeds":[{"title":"%s","description":"%s","color":%d}]}
                """.formatted(escape(title), escape(description), colorDecimal);

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> post(webhookUrl, payload));
    }

    private void post(String webhookUrl, String payload) {
        try {
            HttpURLConnection connection = (HttpURLConnection) URI.create(webhookUrl).toURL().openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            try (OutputStream out = connection.getOutputStream()) {
                out.write(payload.getBytes(StandardCharsets.UTF_8));
            }
            connection.getResponseCode();
            connection.disconnect();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Falha ao enviar webhook do Discord: " + e.getMessage());
        }
    }

    private String escape(String raw) {
        return raw.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private int parseColor(String hex) {
        try {
            return Integer.parseInt(hex.replace("#", ""), 16);
        } catch (NumberFormatException e) {
            return 0xFFFFFF;
        }
    }
}
