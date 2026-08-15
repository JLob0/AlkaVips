package com.alkacode.vips.manager;

import com.alkacode.vips.hook.AlkaEconomyHook;
import com.alkacode.vips.hook.AlkaFlairHook;
import com.alkacode.vips.model.AffiliateConfig;
import com.alkacode.vips.model.PlayerVip;
import com.alkacode.vips.storage.VipsRepository;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * "VIP Affiliate" (Ideia 4) - indicacao em cadeia. {@code /vip indicar} so registra a
 * indicacao como PENDENTE; a cascata de recompensa so dispara quando o indicado
 * ativa o PRIMEIRO VIP da conta dele (checado pelo chamador via WalletManager antes
 * de chamar {@link #onFirstActivation}), subindo ate {@code max-referral-depth}
 * niveis (indicador do indicador do indicado, etc).
 */
public final class AffiliateManager {

    private final JavaPlugin plugin;
    private final VipsRepository database;
    private final PlayerVipManager playerVipManager;
    private final AlkaEconomyHook economyHook;
    private AffiliateConfig config;

    public AffiliateManager(JavaPlugin plugin, VipsRepository database, PlayerVipManager playerVipManager, AlkaEconomyHook economyHook) {
        this.plugin = plugin;
        this.database = database;
        this.playerVipManager = playerVipManager;
        this.economyHook = economyHook;
        load();
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), "affiliate.yml");
        if (!file.exists()) {
            try (InputStream in = plugin.getResource("affiliate.yml")) {
                if (in != null) {
                    Files.copy(in, file.toPath());
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Nao foi possivel criar affiliate.yml: " + e.getMessage());
            }
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        boolean enabled = yaml.getBoolean("enabled", true);
        int maxDepth = yaml.getInt("max-referral-depth", 3);
        List<AffiliateConfig.Level> levels = new ArrayList<>();
        for (int i = 1; i <= maxDepth; i++) {
            var section = yaml.getConfigurationSection("rewards.level-" + i);
            if (section == null) {
                levels.add(new AffiliateConfig.Level("coins", 0, 0));
                continue;
            }
            long bonusDays = section.getInt("bonus-days", 0);
            levels.add(new AffiliateConfig.Level(section.getString("currency", "coins"),
                    section.getDouble("amount", 0), bonusDays * 86_400_000L));
        }
        var leaderboard = yaml.getConfigurationSection("leaderboard");
        this.config = new AffiliateConfig(enabled, maxDepth, levels,
                leaderboard != null ? leaderboard.getString("top-tag", "") : "",
                leaderboard != null ? leaderboard.getString("top-medal", "") : "",
                leaderboard != null ? leaderboard.getInt("top-medal-min-count", 3) : 3);
    }

    public AffiliateConfig config() {
        return config;
    }

    public enum ReferResult { SUCCESS, DISABLED, SELF, ALREADY_REFERRED }

    public ReferResult refer(UUID referrer, UUID referred) {
        if (!config.enabled()) {
            return ReferResult.DISABLED;
        }
        if (referrer.equals(referred)) {
            return ReferResult.SELF;
        }
        if (database.hasAnyReferralSync(referred)) {
            return ReferResult.ALREADY_REFERRED;
        }
        database.insertReferralSync(referrer, referred);
        return ReferResult.SUCCESS;
    }

    /** Chamado quando {@code referredUuid} ativa o primeiro VIP da conta - sobe a cadeia de indicadores concedendo recompensa por nivel. */
    public void onFirstActivation(UUID referredUuid, AlkaFlairHook flairHook) {
        if (!config.enabled()) {
            return;
        }
        UUID current = referredUuid;
        for (int depth = 1; depth <= config.maxDepth(); depth++) {
            VipsRepository.ReferralRow row = database.findPendingReferralSync(current);
            if (row == null) {
                return;
            }
            database.completeReferralSync(row.id());
            AffiliateConfig.Level level = config.levelOrNull(depth);
            if (level != null) {
                if (level.amount() > 0 && economyHook != null) {
                    economyHook.deposit(row.referrerUuid(), level.currency(), level.amount());
                }
                if (level.bonusDaysMillis() > 0) {
                    extendActiveVip(row.referrerUuid(), level.bonusDaysMillis());
                }
            }
            current = row.referrerUuid();
        }
    }

    private void extendActiveVip(UUID uuid, long bonusMillis) {
        Optional<PlayerVip> selected = playerVipManager.getSelectedVip(uuid);
        if (selected.isEmpty() || selected.get().isPermanent()) {
            return;
        }
        PlayerVip vip = selected.get();
        vip.expiresAt(vip.expiresAt() + bonusMillis);
        vip.totalDuration(vip.totalDuration() + bonusMillis);
        playerVipManager.update(vip);
    }

    public List<VipsRepository.TopReferrer> topReferrers(int limit) {
        return database.topReferrersSync(limit);
    }
}
