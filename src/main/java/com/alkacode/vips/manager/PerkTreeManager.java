package com.alkacode.vips.manager;

import com.alkacode.vips.model.PerkNode;
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
 * "VIP Perk Tree" (Ideia 3) - DESATIVADA POR PADRAO ({@code enabled: false} em
 * perktree.yml, unico jeito de ligar - nao ha flag por-tier). Util so pra servidores
 * com pegada de RPG/progression, por isso ficou fora do fluxo normal de compra
 * (pontos-por-tier so sao creditados quando {@code enabled=true}, ver ActivationService).
 */
public final class PerkTreeManager {

    private final JavaPlugin plugin;
    private final VipsRepository database;
    private boolean enabled;
    private final Map<String, Integer> pointsPerTier = new LinkedHashMap<>();
    private final Map<String, PerkNode> perks = new LinkedHashMap<>();
    private final Map<String, String> branchNames = new LinkedHashMap<>();

    public PerkTreeManager(JavaPlugin plugin, VipsRepository database) {
        this.plugin = plugin;
        this.database = database;
        load();
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), "perktree.yml");
        if (!file.exists()) {
            try (InputStream in = plugin.getResource("perktree.yml")) {
                if (in != null) {
                    Files.copy(in, file.toPath());
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Nao foi possivel criar perktree.yml: " + e.getMessage());
            }
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        this.enabled = yaml.getBoolean("enabled", false);

        pointsPerTier.clear();
        ConfigurationSection pointsSection = yaml.getConfigurationSection("points-per-tier");
        if (pointsSection != null) {
            for (String tierId : pointsSection.getKeys(false)) {
                pointsPerTier.put(tierId.toLowerCase(), pointsSection.getInt(tierId));
            }
        }

        Map<String, PerkNode> loaded = new LinkedHashMap<>();
        Map<String, String> loadedBranchNames = new LinkedHashMap<>();
        ConfigurationSection branches = yaml.getConfigurationSection("branches");
        if (branches != null) {
            for (String branchId : branches.getKeys(false)) {
                ConfigurationSection branch = branches.getConfigurationSection(branchId);
                if (branch != null) {
                    loadedBranchNames.put(branchId, branch.getString("name", branchId));
                }
                ConfigurationSection perksSection = branch != null ? branch.getConfigurationSection("perks") : null;
                if (perksSection == null) continue;
                for (String perkId : perksSection.getKeys(false)) {
                    ConfigurationSection perk = perksSection.getConfigurationSection(perkId);
                    if (perk == null) continue;
                    ConfigurationSection effect = perk.getConfigurationSection("effect");
                    loaded.put(perkId, new PerkNode(perkId, branchId, perk.getString("name", perkId),
                            perk.getInt("cost", 0), perk.getString("requires", ""),
                            effect != null ? effect.getString("type", "") : "",
                            effect != null ? effect.getDouble("value", 0) : 0));
                }
            }
        }
        perks.clear();
        perks.putAll(loaded);
        branchNames.clear();
        branchNames.putAll(loadedBranchNames);
    }

    public boolean enabled() {
        return enabled;
    }

    public Set<String> branchIds() {
        return branchNames.keySet();
    }

    public String branchName(String branchId) {
        return branchNames.getOrDefault(branchId, branchId);
    }

    public int pointsForTier(String tierId) {
        return pointsPerTier.getOrDefault(tierId.toLowerCase(), 0);
    }

    public void grantPoints(UUID uuid, String tierId) {
        if (!enabled) {
            return;
        }
        int points = pointsForTier(tierId);
        if (points > 0) {
            database.addPerkPointsSync(uuid, points);
        }
    }

    public VipsRepository.PerkPoints pointsOf(UUID uuid) {
        return database.loadPerkPointsSync(uuid);
    }

    public List<PerkNode> all() {
        return List.copyOf(perks.values());
    }

    public List<PerkNode> inBranch(String branchId) {
        List<PerkNode> result = new ArrayList<>();
        for (PerkNode perk : perks.values()) {
            if (perk.branchId().equalsIgnoreCase(branchId)) {
                result.add(perk);
            }
        }
        return result;
    }

    public Set<String> unlockedPerks(UUID uuid) {
        return database.loadUnlockedPerksSync(uuid);
    }

    /** Maior effectValue entre os perks desbloqueados desse effectType (nao soma - perks
     * da mesma branch sao progressivos/prerequisito em cadeia, ex: sell-boost-10 exige
     * sell-boost-5, entao so o maior deve valer). 0 se nenhum perk desse tipo desbloqueado
     * ou perk tree desativada. */
    public double effectValueMax(UUID uuid, String effectType) {
        if (!enabled) return 0;
        Set<String> unlocked = unlockedPerks(uuid);
        double max = 0;
        for (PerkNode perk : perks.values()) {
            if (perk.effectType().equalsIgnoreCase(effectType) && unlocked.contains(perk.id())) {
                max = Math.max(max, perk.effectValue());
            }
        }
        return max;
    }

    /** Soma do effectValue de todos os perks desbloqueados desse effectType (perks
     * independentes que empilham, ex: cada "+1 Mina Particular"). 0 se nenhum/desativada. */
    public double effectValueSum(UUID uuid, String effectType) {
        if (!enabled) return 0;
        Set<String> unlocked = unlockedPerks(uuid);
        double sum = 0;
        for (PerkNode perk : perks.values()) {
            if (perk.effectType().equalsIgnoreCase(effectType) && unlocked.contains(perk.id())) {
                sum += perk.effectValue();
            }
        }
        return sum;
    }

    public enum UnlockResult { SUCCESS, ALREADY_UNLOCKED, NOT_ENOUGH_POINTS, MISSING_PREREQUISITE, NOT_FOUND, DISABLED }

    public UnlockResult unlock(UUID uuid, String perkId) {
        if (!enabled) {
            return UnlockResult.DISABLED;
        }
        PerkNode perk = perks.get(perkId);
        if (perk == null) {
            return UnlockResult.NOT_FOUND;
        }
        Set<String> unlocked = database.loadUnlockedPerksSync(uuid);
        if (unlocked.contains(perkId)) {
            return UnlockResult.ALREADY_UNLOCKED;
        }
        if (!perk.requiresPerkId().isBlank() && !unlocked.contains(perk.requiresPerkId())) {
            return UnlockResult.MISSING_PREREQUISITE;
        }
        VipsRepository.PerkPoints points = database.loadPerkPointsSync(uuid);
        if (points.available() < perk.cost()) {
            return UnlockResult.NOT_ENOUGH_POINTS;
        }
        database.spendPerkPointsSync(uuid, perk.cost());
        database.unlockPerkSync(uuid, perkId);
        return UnlockResult.SUCCESS;
    }
}
