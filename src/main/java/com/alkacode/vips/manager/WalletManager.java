package com.alkacode.vips.manager;

import com.alkacode.vips.hook.AlkaFlairHook;
import com.alkacode.vips.model.Achievement;
import com.alkacode.vips.storage.VipsRepository;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * "Carteira VIP" (Ideia 5) - historico de ativacoes ({@code vip_history}), gastos
 * registrados por qualquer fluxo de compra real ({@code vip_transactions}, hoje so
 * o mercado P2P de assinaturas escreve nela - a compra por key nao passa por
 * currency dentro do AlkaVips) e conquistas (achievements.yml).
 */
public final class WalletManager {

    private final JavaPlugin plugin;
    private final VipsRepository database;
    private final AlkaFlairHook flairHook;
    private final Map<String, Achievement> achievements = new LinkedHashMap<>();

    public WalletManager(JavaPlugin plugin, VipsRepository database, AlkaFlairHook flairHook) {
        this.plugin = plugin;
        this.database = database;
        this.flairHook = flairHook;
        load();
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), "achievements.yml");
        if (!file.exists()) {
            try (InputStream in = plugin.getResource("achievements.yml")) {
                if (in != null) {
                    Files.copy(in, file.toPath());
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Nao foi possivel criar achievements.yml: " + e.getMessage());
            }
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        Map<String, Achievement> loaded = new LinkedHashMap<>();
        ConfigurationSection root = yaml.getConfigurationSection("achievements");
        if (root != null) {
            for (String id : root.getKeys(false)) {
                ConfigurationSection section = root.getConfigurationSection(id);
                if (section == null) continue;
                ConfigurationSection requirement = section.getConfigurationSection("requirement");
                loaded.put(id, new Achievement(
                        id,
                        section.getString("name", id),
                        requirement != null ? requirement.getString("type", "") : "",
                        requirement != null ? requirement.getString("currency", "") : "",
                        requirement != null ? requirement.getLong("value", 0) : 0,
                        section.getString("reward-tag", ""),
                        section.getString("reward-medal", "")));
            }
        }
        achievements.clear();
        achievements.putAll(loaded);
    }

    public void onActivate(UUID uuid, String tierId, long activatedAtMillis) {
        long id = database.insertHistorySync(uuid, tierId, activatedAtMillis);
        if (id < 0) {
            return;
        }
        checkAchievements(uuid);
    }

    /** Fecha a entrada de historico ABERTA desse tier (se houver) - chamado no expire, nunca em accumulate (o VIP continua ativo). */
    public void onExpire(UUID uuid, String tierId) {
        VipsRepository.HistoryEntry open = database.loadOpenHistorySync(uuid, tierId);
        if (open == null) {
            return;
        }
        long now = System.currentTimeMillis();
        int days = (int) Math.max(0, (now - open.activatedAt()) / 86_400_000L);
        database.closeHistorySync(open.id(), now, days);
        checkAchievements(uuid);
    }

    public void recordTransaction(UUID uuid, String type, String currency, double amount) {
        database.insertTransactionSync(uuid, type, currency, amount);
        checkAchievements(uuid);
    }

    public void checkAchievements(UUID uuid) {
        Set<String> claimed = database.loadClaimedAchievementsSync(uuid);
        for (Achievement achievement : achievements.values()) {
            if (claimed.contains(achievement.id())) {
                continue;
            }
            if (meets(uuid, achievement)) {
                database.markAchievementClaimedSync(uuid, achievement.id());
                flairHook.addTag(uuid, achievement.rewardTagId());
                flairHook.addMedal(uuid, achievement.rewardMedalId());
            }
        }
    }

    private boolean meets(UUID uuid, Achievement achievement) {
        return switch (achievement.requirementType().toUpperCase(java.util.Locale.ROOT)) {
            case "FIRST_PURCHASE" -> !database.loadHistorySync(uuid).isEmpty();
            case "TOTAL_DAYS" -> database.totalHistoryDaysSync(uuid) >= achievement.requirementValue();
            case "TOTAL_SPENT" -> database.totalSpentByCurrencySync(uuid)
                    .getOrDefault(achievement.requirementCurrency(), 0.0) >= achievement.requirementValue();
            case "RENEW_SAME_TIER" -> database.loadHistorySync(uuid).stream()
                    .collect(java.util.stream.Collectors.groupingBy(VipsRepository.HistoryEntry::tierId, java.util.stream.Collectors.counting()))
                    .values().stream().anyMatch(count -> count >= achievement.requirementValue());
            default -> false;
        };
    }

    public List<Achievement> all() {
        return List.copyOf(achievements.values());
    }

    public Map<String, Double> totalSpent(UUID uuid) {
        return database.totalSpentByCurrencySync(uuid);
    }

    public int totalDays(UUID uuid) {
        return database.totalHistoryDaysSync(uuid);
    }

    public List<VipsRepository.HistoryEntry> history(UUID uuid) {
        return database.loadHistorySync(uuid);
    }

    public Set<String> claimedAchievements(UUID uuid) {
        return database.loadClaimedAchievementsSync(uuid);
    }
}
