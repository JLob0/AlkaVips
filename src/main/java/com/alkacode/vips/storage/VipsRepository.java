package com.alkacode.vips.storage;

import com.alkacode.core.api.DatabaseProvider;
import com.alkacode.core.database.AbstractRepository;
import com.alkacode.vips.model.PlayerVip;
import com.alkacode.vips.model.VipKey;
import com.alkacode.vips.model.VipPlayerData;
import com.alkacode.vips.model.enums.VipStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Persistencia do AlkaVips sobre o {@link DatabaseProvider} do AlkaCore (HikariCP,
 * SQLite ou MySQL conforme o config.yml do Core) - substitui o antigo DatabaseManager,
 * que abria sua propria Connection JDBC via DriverManager. O executor single-thread
 * proprio e mantido so para servir de fila de CompletableFuture (os call-sites em
 * PlayerVipManager/CreditManager/KeyManager/PartyVipManager encadeiam .thenApply/
 * .thenCompose sobre essas futures) - nao gerencia mais a conexao em si, que agora
 * vem do pool do Core (tamanho 1 para SQLite, ja serializado la).
 */
public final class VipsRepository extends AbstractRepository {

    private final Logger logger;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "AlkaVips-DB");
        thread.setDaemon(true);
        return thread;
    });

    public VipsRepository(DatabaseProvider db, Logger logger) {
        super(db);
        this.logger = logger;
        createTables();
    }

    private void createTables() {
        String autoIncrement = db.isMySQL() ? "INT AUTO_INCREMENT PRIMARY KEY" : "INTEGER PRIMARY KEY AUTOINCREMENT";
        try (Connection conn = db.getConnection(); Statement statement = conn.createStatement()) {
            statement.execute("""
                CREATE TABLE IF NOT EXISTS player_vips (
                    id %s,
                    player_uuid VARCHAR(36) NOT NULL,
                    vip_type_id VARCHAR(64) NOT NULL,
                    key_id VARCHAR(32),
                    status VARCHAR(16) NOT NULL,
                    activated_at BIGINT NOT NULL,
                    expires_at BIGINT NOT NULL,
                    total_duration BIGINT NOT NULL,
                    frozen INTEGER NOT NULL DEFAULT 0,
                    frozen_at BIGINT NOT NULL DEFAULT 0
                )
                """.formatted(autoIncrement));
            statement.execute("CREATE INDEX IF NOT EXISTS idx_player_vips_uuid ON player_vips(player_uuid)");
            statement.execute("""
                CREATE TABLE IF NOT EXISTS vip_keys (
                    id VARCHAR(32) PRIMARY KEY,
                    vip_type_id VARCHAR(64) NOT NULL,
                    duration BIGINT NOT NULL,
                    used INTEGER NOT NULL DEFAULT 0,
                    used_by VARCHAR(36),
                    used_at BIGINT NOT NULL DEFAULT 0,
                    bonus INTEGER NOT NULL DEFAULT 0,
                    for_sale INTEGER NOT NULL DEFAULT 0,
                    seller_uuid VARCHAR(36),
                    economy_provider VARCHAR(32),
                    sell_price DOUBLE NOT NULL DEFAULT 0
                )
                """);
            statement.execute("""
                CREATE TABLE IF NOT EXISTS player_data (
                    uuid VARCHAR(36) PRIMARY KEY,
                    credits DOUBLE NOT NULL DEFAULT 0,
                    spent_credits DOUBLE NOT NULL DEFAULT 0,
                    total_activations INTEGER NOT NULL DEFAULT 0,
                    auto_sell_enabled INTEGER NOT NULL DEFAULT 0,
                    selected_vip_id BIGINT,
                    last_quit_at BIGINT NOT NULL DEFAULT 0
                )
                """);
            statement.execute("""
                CREATE TABLE IF NOT EXISTS party_vip_state (
                    id INTEGER PRIMARY KEY CHECK (id = 1),
                    progress DOUBLE NOT NULL DEFAULT 0
                )
                """);
            statement.execute("""
                CREATE TABLE IF NOT EXISTS vip_activation_bonus_claimed (
                    player_uuid VARCHAR(36) NOT NULL,
                    vip_type_id VARCHAR(32) NOT NULL,
                    claimed_at BIGINT NOT NULL,
                    PRIMARY KEY (player_uuid, vip_type_id)
                )
                """);
            statement.execute("""
                CREATE TABLE IF NOT EXISTS vip_activation_bonus_available (
                    player_uuid VARCHAR(36) NOT NULL,
                    vip_type_id VARCHAR(32) NOT NULL,
                    available_at BIGINT NOT NULL,
                    PRIMARY KEY (player_uuid, vip_type_id)
                )
                """);
            statement.execute("""
                CREATE TABLE IF NOT EXISTS vips_state (
                    id INTEGER PRIMARY KEY CHECK (id = 1),
                    last_shutdown_at BIGINT NOT NULL DEFAULT 0
                )
                """);
            statement.execute("""
                CREATE TABLE IF NOT EXISTS vip_legacy_grants (
                    player_uuid VARCHAR(36) NOT NULL,
                    original_tier_id VARCHAR(64) NOT NULL,
                    fall_tier_id VARCHAR(64) NOT NULL,
                    player_vip_id BIGINT NOT NULL,
                    grace_until BIGINT NOT NULL,
                    discount_percent INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY (player_uuid, original_tier_id)
                )
                """);
            statement.execute("""
                CREATE TABLE IF NOT EXISTS vip_history (
                    id %s,
                    player_uuid VARCHAR(36) NOT NULL,
                    tier_id VARCHAR(64) NOT NULL,
                    activated_at BIGINT NOT NULL,
                    ended_at BIGINT NOT NULL DEFAULT 0,
                    duration_days INTEGER NOT NULL DEFAULT 0
                )
                """.formatted(autoIncrement));
            statement.execute("CREATE INDEX IF NOT EXISTS idx_vip_history_uuid ON vip_history(player_uuid)");
            statement.execute("""
                CREATE TABLE IF NOT EXISTS vip_transactions (
                    id %s,
                    player_uuid VARCHAR(36) NOT NULL,
                    type VARCHAR(32) NOT NULL,
                    currency VARCHAR(32) NOT NULL,
                    amount DOUBLE NOT NULL,
                    created_at BIGINT NOT NULL
                )
                """.formatted(autoIncrement));
            statement.execute("CREATE INDEX IF NOT EXISTS idx_vip_transactions_uuid ON vip_transactions(player_uuid)");
            statement.execute("""
                CREATE TABLE IF NOT EXISTS vip_achievements_claimed (
                    player_uuid VARCHAR(36) NOT NULL,
                    achievement_id VARCHAR(64) NOT NULL,
                    claimed_at BIGINT NOT NULL,
                    PRIMARY KEY (player_uuid, achievement_id)
                )
                """);
            statement.execute("""
                CREATE TABLE IF NOT EXISTS vip_referrals (
                    id %s,
                    referrer_uuid VARCHAR(36) NOT NULL,
                    referred_uuid VARCHAR(36) NOT NULL,
                    created_at BIGINT NOT NULL,
                    completed INTEGER NOT NULL DEFAULT 0,
                    completed_at BIGINT NOT NULL DEFAULT 0
                )
                """.formatted(autoIncrement));
            statement.execute("CREATE INDEX IF NOT EXISTS idx_vip_referrals_referred ON vip_referrals(referred_uuid)");
            statement.execute("""
                CREATE TABLE IF NOT EXISTS vip_p2p_listings (
                    id %s,
                    seller_uuid VARCHAR(36) NOT NULL,
                    player_vip_id BIGINT NOT NULL,
                    tier_id VARCHAR(64) NOT NULL,
                    remaining_millis BIGINT NOT NULL,
                    price DOUBLE NOT NULL,
                    currency VARCHAR(32) NOT NULL,
                    listed_at BIGINT NOT NULL
                )
                """.formatted(autoIncrement));
            statement.execute("""
                CREATE TABLE IF NOT EXISTS vip_perk_points (
                    player_uuid VARCHAR(36) PRIMARY KEY,
                    points_available INTEGER NOT NULL DEFAULT 0,
                    points_earned INTEGER NOT NULL DEFAULT 0
                )
                """);
            statement.execute("""
                CREATE TABLE IF NOT EXISTS vip_perk_unlocks (
                    player_uuid VARCHAR(36) NOT NULL,
                    perk_id VARCHAR(96) NOT NULL,
                    PRIMARY KEY (player_uuid, perk_id)
                )
                """);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao criar tabelas do AlkaVips", e);
        }
    }

    /** Encerra a fila de CompletableFuture do AlkaVips - a conexao em si e do AlkaCore, fechada por ele. */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.log(Level.WARNING, "Timeout aguardando saves pendentes do AlkaVips.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public <T> CompletableFuture<T> async(java.util.function.Supplier<T> task) {
        return CompletableFuture.supplyAsync(task, executor);
    }

    public CompletableFuture<Void> asyncRun(Runnable task) {
        return CompletableFuture.runAsync(task, executor);
    }

    // ---------------------------------------------------------------- player_vips

    public List<PlayerVip> loadPlayerVipsSync(UUID uuid) {
        List<PlayerVip> result = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement statement = conn.prepareStatement(
                     "SELECT * FROM player_vips WHERE player_uuid = ?")) {
            statement.setString(1, uuid.toString());
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    result.add(readPlayerVip(rs));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao carregar VIPs de " + uuid, e);
        }
        return result;
    }

    public CompletableFuture<List<PlayerVip>> loadPlayerVips(UUID uuid) {
        return async(() -> loadPlayerVipsSync(uuid));
    }

    private PlayerVip readPlayerVip(ResultSet rs) throws SQLException {
        return new PlayerVip(
                rs.getLong("id"),
                UUID.fromString(rs.getString("player_uuid")),
                rs.getString("vip_type_id"),
                rs.getString("key_id"),
                VipStatus.valueOf(rs.getString("status")),
                rs.getLong("activated_at"),
                rs.getLong("expires_at"),
                rs.getLong("total_duration"),
                rs.getInt("frozen") != 0,
                rs.getLong("frozen_at")
        );
    }

    public long insertPlayerVipSync(PlayerVip vip) {
        try (Connection conn = db.getConnection();
             PreparedStatement statement = conn.prepareStatement("""
                INSERT INTO player_vips
                    (player_uuid, vip_type_id, key_id, status, activated_at, expires_at, total_duration, frozen, frozen_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, vip.playerUuid().toString());
            statement.setString(2, vip.vipTypeId());
            statement.setString(3, vip.keyId());
            statement.setString(4, vip.status().name());
            statement.setLong(5, vip.activatedAt());
            statement.setLong(6, vip.expiresAt());
            statement.setLong(7, vip.totalDuration());
            statement.setInt(8, vip.frozen() ? 1 : 0);
            statement.setLong(9, vip.frozenAt());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao inserir PlayerVip", e);
        }
        return -1;
    }

    public CompletableFuture<Long> insertPlayerVip(PlayerVip vip) {
        return async(() -> insertPlayerVipSync(vip));
    }

    public void updatePlayerVipSync(PlayerVip vip) {
        try (Connection conn = db.getConnection();
             PreparedStatement statement = conn.prepareStatement("""
                UPDATE player_vips SET status = ?, activated_at = ?, expires_at = ?, total_duration = ?,
                    frozen = ?, frozen_at = ? WHERE id = ?
                """)) {
            statement.setString(1, vip.status().name());
            statement.setLong(2, vip.activatedAt());
            statement.setLong(3, vip.expiresAt());
            statement.setLong(4, vip.totalDuration());
            statement.setInt(5, vip.frozen() ? 1 : 0);
            statement.setLong(6, vip.frozenAt());
            statement.setLong(7, vip.id());
            statement.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao atualizar PlayerVip " + vip.id(), e);
        }
    }

    public CompletableFuture<Void> updatePlayerVip(PlayerVip vip) {
        return asyncRun(() -> updatePlayerVipSync(vip));
    }

    public void deletePlayerVipSync(long id) {
        try (Connection conn = db.getConnection();
             PreparedStatement statement = conn.prepareStatement("DELETE FROM player_vips WHERE id = ?")) {
            statement.setLong(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao remover PlayerVip " + id, e);
        }
    }

    public CompletableFuture<Void> deletePlayerVip(long id) {
        return asyncRun(() -> deletePlayerVipSync(id));
    }

    // ---------------------------------------------------------------- vip_keys

    private VipKey readKey(ResultSet rs) throws SQLException {
        String usedByRaw = rs.getString("used_by");
        String sellerRaw = rs.getString("seller_uuid");
        return new VipKey(
                rs.getString("id"),
                rs.getString("vip_type_id"),
                rs.getLong("duration"),
                rs.getInt("used") != 0,
                usedByRaw != null ? UUID.fromString(usedByRaw) : null,
                rs.getLong("used_at"),
                rs.getInt("bonus") != 0,
                rs.getInt("for_sale") != 0,
                sellerRaw != null ? UUID.fromString(sellerRaw) : null,
                rs.getString("economy_provider"),
                rs.getDouble("sell_price")
        );
    }

    public VipKey loadKeySync(String id) {
        try (Connection conn = db.getConnection();
             PreparedStatement statement = conn.prepareStatement("SELECT * FROM vip_keys WHERE id = ?")) {
            statement.setString(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return readKey(rs);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao carregar key " + id, e);
        }
        return null;
    }

    public CompletableFuture<VipKey> loadKey(String id) {
        return async(() -> loadKeySync(id));
    }

    public void insertKeySync(VipKey key) {
        try (Connection conn = db.getConnection();
             PreparedStatement statement = conn.prepareStatement("""
                INSERT INTO vip_keys
                    (id, vip_type_id, duration, used, used_by, used_at, bonus, for_sale, seller_uuid, economy_provider, sell_price)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, key.id());
            statement.setString(2, key.vipTypeId());
            statement.setLong(3, key.duration());
            statement.setInt(4, key.used() ? 1 : 0);
            statement.setString(5, key.usedBy() != null ? key.usedBy().toString() : null);
            statement.setLong(6, key.usedAt());
            statement.setInt(7, key.bonus() ? 1 : 0);
            statement.setInt(8, key.forSale() ? 1 : 0);
            statement.setString(9, key.sellerUuid() != null ? key.sellerUuid().toString() : null);
            statement.setString(10, key.economyProvider());
            statement.setDouble(11, key.sellPrice());
            statement.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao inserir key " + key.id(), e);
        }
    }

    public CompletableFuture<Void> insertKey(VipKey key) {
        return asyncRun(() -> insertKeySync(key));
    }

    public void updateKeySync(VipKey key) {
        try (Connection conn = db.getConnection();
             PreparedStatement statement = conn.prepareStatement("""
                UPDATE vip_keys SET used = ?, used_by = ?, used_at = ?, for_sale = ?, seller_uuid = ?,
                    economy_provider = ?, sell_price = ? WHERE id = ?
                """)) {
            statement.setInt(1, key.used() ? 1 : 0);
            statement.setString(2, key.usedBy() != null ? key.usedBy().toString() : null);
            statement.setLong(3, key.usedAt());
            statement.setInt(4, key.forSale() ? 1 : 0);
            statement.setString(5, key.sellerUuid() != null ? key.sellerUuid().toString() : null);
            statement.setString(6, key.economyProvider());
            statement.setDouble(7, key.sellPrice());
            statement.setString(8, key.id());
            statement.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao atualizar key " + key.id(), e);
        }
    }

    public CompletableFuture<Void> updateKey(VipKey key) {
        return asyncRun(() -> updateKeySync(key));
    }

    public void deleteKeySync(String id) {
        try (Connection conn = db.getConnection();
             PreparedStatement statement = conn.prepareStatement("DELETE FROM vip_keys WHERE id = ?")) {
            statement.setString(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao remover key " + id, e);
        }
    }

    public CompletableFuture<Void> deleteKey(String id) {
        return asyncRun(() -> deleteKeySync(id));
    }

    public List<VipKey> loadForSaleKeysSync() {
        List<VipKey> result = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement statement = conn.prepareStatement(
                     "SELECT * FROM vip_keys WHERE for_sale = 1")) {
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    result.add(readKey(rs));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao carregar keys a venda", e);
        }
        return result;
    }

    public CompletableFuture<List<VipKey>> loadForSaleKeys() {
        return async(this::loadForSaleKeysSync);
    }

    // ---------------------------------------------------------------- player_data

    public VipPlayerData loadPlayerDataSync(UUID uuid) {
        try (Connection conn = db.getConnection();
             PreparedStatement statement = conn.prepareStatement(
                     "SELECT * FROM player_data WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    long selectedRaw = rs.getLong("selected_vip_id");
                    Long selected = rs.wasNull() ? null : selectedRaw;
                    return new VipPlayerData(uuid, rs.getDouble("credits"), rs.getDouble("spent_credits"),
                            rs.getInt("total_activations"), rs.getInt("auto_sell_enabled") != 0, selected,
                            rs.getLong("last_quit_at"));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao carregar dados de " + uuid, e);
        }
        return new VipPlayerData(uuid, 0, 0, 0, false, null, 0);
    }

    public CompletableFuture<VipPlayerData> loadPlayerData(UUID uuid) {
        return async(() -> loadPlayerDataSync(uuid));
    }

    public void savePlayerDataSync(VipPlayerData data) {
        String sql = upsert("player_data",
                new String[]{"uuid", "credits", "spent_credits", "total_activations", "auto_sell_enabled", "selected_vip_id", "last_quit_at"},
                new String[]{"uuid"});
        try {
            execute(sql, ps -> {
                ps.setString(1, data.uuid().toString());
                ps.setDouble(2, data.credits());
                ps.setDouble(3, data.spentCredits());
                ps.setInt(4, data.totalActivations());
                ps.setInt(5, data.autoSellEnabled() ? 1 : 0);
                if (data.selectedVipId() != null) {
                    ps.setLong(6, data.selectedVipId());
                } else {
                    ps.setNull(6, java.sql.Types.BIGINT);
                }
                ps.setLong(7, data.lastQuitAt());
            });
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao salvar dados de " + data.uuid(), e);
        }
    }

    public CompletableFuture<Void> savePlayerData(VipPlayerData data) {
        return asyncRun(() -> savePlayerDataSync(data));
    }


    // ---------------------------------------------------------------- party vip

    public double loadPartyProgressSync() {
        try (Connection conn = db.getConnection(); Statement statement = conn.createStatement();
             ResultSet rs = statement.executeQuery("SELECT progress FROM party_vip_state WHERE id = 1")) {
            if (rs.next()) {
                return rs.getDouble("progress");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao carregar progresso do Party VIP", e);
        }
        return 0.0;
    }

    public CompletableFuture<Double> loadPartyProgress() {
        return async(this::loadPartyProgressSync);
    }

    public void savePartyProgressSync(double progress) {
        String sql = upsert("party_vip_state", new String[]{"id", "progress"}, new String[]{"id"});
        try {
            execute(sql, ps -> {
                ps.setInt(1, 1);
                ps.setDouble(2, progress);
            });
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao salvar progresso do Party VIP", e);
        }
    }

    public CompletableFuture<Void> savePartyProgress(double progress) {
        return asyncRun(() -> savePartyProgressSync(progress));
    }

    // ---------------------------------------------------------------- vips_state (uptime tracking)

    public long loadLastShutdownSync() {
        try (Connection conn = db.getConnection(); Statement statement = conn.createStatement();
             ResultSet rs = statement.executeQuery("SELECT last_shutdown_at FROM vips_state WHERE id = 1")) {
            if (rs.next()) {
                return rs.getLong("last_shutdown_at");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao carregar last_shutdown_at", e);
        }
        return 0L;
    }

    public void saveLastShutdownSync(long timestamp) {
        String sql = upsert("vips_state", new String[]{"id", "last_shutdown_at"}, new String[]{"id"});
        try {
            execute(sql, ps -> {
                ps.setInt(1, 1);
                ps.setLong(2, timestamp);
            });
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao salvar last_shutdown_at", e);
        }
    }

    // ------------------------------------------------- vip_activation_bonus_claimed

    /**
     * true se esse jogador ja recebeu o presente de ativacao (unico por conta, pra
     * sempre) desse tier - independente de quantas vezes ele reativar o mesmo tier
     * depois. E o que impede comprar VIP de 1h repetidas vezes pra repetir o bonus:
     * combinado com a exigencia de duracao minima em {@link com.alkacode.vips.service.ActivationService},
     * so conta na primeira vez que uma ativacao daquele tier bater a duracao minima.
     */
    public boolean hasClaimedActivationBonusSync(UUID uuid, String vipTypeId) {
        String sql = "SELECT 1 FROM vip_activation_bonus_claimed WHERE player_uuid = ? AND vip_type_id = ?";
        try (Connection conn = db.getConnection(); PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, vipTypeId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao verificar bonus de ativacao de " + uuid, e);
            return false;
        }
    }

    public void markActivationBonusClaimedSync(UUID uuid, String vipTypeId) {
        String sql = upsert("vip_activation_bonus_claimed",
                new String[]{"player_uuid", "vip_type_id", "claimed_at"},
                new String[]{"player_uuid", "vip_type_id"});
        try {
            execute(sql, ps -> {
                ps.setString(1, uuid.toString());
                ps.setString(2, vipTypeId);
                ps.setLong(3, System.currentTimeMillis());
            });
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao registrar bonus de ativacao de " + uuid, e);
        }
    }

    // ------------------------------------------------- vip_activation_bonus_available

    /**
     * Kit de ativacao "disponivel para pegar na GUI" - o jogador desbloqueou o bonus
     * (ativou 30+ dias) mas ainda nao pegou o kit fisicamente. Ver
     * com.alkacode.vips.gui.VipKitsMenu - a entrega de itens so acontece quando o
     * jogador clica no kit, nao mais automaticamente na ativacao.
     */
    public boolean isActivationBonusAvailableSync(UUID uuid, String vipTypeId) {
        String sql = "SELECT 1 FROM vip_activation_bonus_available WHERE player_uuid = ? AND vip_type_id = ?";
        try (Connection conn = db.getConnection(); PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, vipTypeId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao verificar bonus de ativacao disponivel de " + uuid, e);
            return false;
        }
    }

    public void grantActivationBonusAvailableSync(UUID uuid, String vipTypeId) {
        String sql = upsert("vip_activation_bonus_available",
                new String[]{"player_uuid", "vip_type_id", "available_at"},
                new String[]{"player_uuid", "vip_type_id"});
        try {
            execute(sql, ps -> {
                ps.setString(1, uuid.toString());
                ps.setString(2, vipTypeId);
                ps.setLong(3, System.currentTimeMillis());
            });
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao marcar bonus de ativacao disponivel de " + uuid, e);
        }
    }

    public void revokeActivationBonusAvailableSync(UUID uuid, String vipTypeId) {
        String sql = "DELETE FROM vip_activation_bonus_available WHERE player_uuid = ? AND vip_type_id = ?";
        try (Connection conn = db.getConnection(); PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, vipTypeId);
            statement.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao revogar bonus de ativacao disponivel de " + uuid, e);
        }
    }

    public java.util.List<String> loadAvailableActivationBonusTiersSync(UUID uuid) {
        java.util.List<String> result = new ArrayList<>();
        String sql = "SELECT vip_type_id FROM vip_activation_bonus_available WHERE player_uuid = ?";
        try (Connection conn = db.getConnection(); PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    result.add(rs.getString("vip_type_id"));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao carregar bonus de ativacao disponiveis de " + uuid, e);
        }
        return result;
    }

    // ---------------------------------------------------------------- vip_legacy_grants

    public record LegacyGrant(String originalTierId, String fallTierId, long playerVipId, long graceUntil, int discountPercent) {
    }

    public LegacyGrant loadLegacyGrantSync(UUID uuid, String originalTierId) {
        String sql = "SELECT * FROM vip_legacy_grants WHERE player_uuid = ? AND original_tier_id = ?";
        try (Connection conn = db.getConnection(); PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, originalTierId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return new LegacyGrant(originalTierId, rs.getString("fall_tier_id"), rs.getLong("player_vip_id"),
                            rs.getLong("grace_until"), rs.getInt("discount_percent"));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao carregar legacy grant de " + uuid, e);
        }
        return null;
    }

    public List<LegacyGrant> loadLegacyGrantsForPlayerSync(UUID uuid) {
        List<LegacyGrant> result = new ArrayList<>();
        String sql = "SELECT * FROM vip_legacy_grants WHERE player_uuid = ?";
        try (Connection conn = db.getConnection(); PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    result.add(new LegacyGrant(rs.getString("original_tier_id"), rs.getString("fall_tier_id"),
                            rs.getLong("player_vip_id"), rs.getLong("grace_until"), rs.getInt("discount_percent")));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao carregar legacy grants de " + uuid, e);
        }
        return result;
    }

    public List<LegacyGrant> loadAllLegacyGrantsSync() {
        List<LegacyGrant> result = new ArrayList<>();
        try (Connection conn = db.getConnection(); Statement statement = conn.createStatement();
             ResultSet rs = statement.executeQuery("SELECT * FROM vip_legacy_grants")) {
            while (rs.next()) {
                result.add(new LegacyGrant(rs.getString("original_tier_id"), rs.getString("fall_tier_id"),
                        rs.getLong("player_vip_id"), rs.getLong("grace_until"), rs.getInt("discount_percent")));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao carregar todos os legacy grants", e);
        }
        return result;
    }

    public void saveLegacyGrantSync(UUID uuid, LegacyGrant grant) {
        String sql = upsert("vip_legacy_grants",
                new String[]{"player_uuid", "original_tier_id", "fall_tier_id", "player_vip_id", "grace_until", "discount_percent"},
                new String[]{"player_uuid", "original_tier_id"});
        try {
            execute(sql, ps -> {
                ps.setString(1, uuid.toString());
                ps.setString(2, grant.originalTierId());
                ps.setString(3, grant.fallTierId());
                ps.setLong(4, grant.playerVipId());
                ps.setLong(5, grant.graceUntil());
                ps.setInt(6, grant.discountPercent());
            });
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao salvar legacy grant de " + uuid, e);
        }
    }

    public void deleteLegacyGrantSync(UUID uuid, String originalTierId) {
        String sql = "DELETE FROM vip_legacy_grants WHERE player_uuid = ? AND original_tier_id = ?";
        try (Connection conn = db.getConnection(); PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, originalTierId);
            statement.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao remover legacy grant de " + uuid, e);
        }
    }

    // ---------------------------------------------------------------- vip_history

    public record HistoryEntry(long id, String tierId, long activatedAt, long endedAt, int durationDays) {
        public boolean ongoing() { return endedAt == 0; }
    }

    public long insertHistorySync(UUID uuid, String tierId, long activatedAt) {
        try (Connection conn = db.getConnection();
             PreparedStatement statement = conn.prepareStatement(
                     "INSERT INTO vip_history (player_uuid, tier_id, activated_at) VALUES (?, ?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, tierId);
            statement.setLong(3, activatedAt);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao inserir historico de VIP de " + uuid, e);
        }
        return -1;
    }

    public void closeHistorySync(long id, long endedAt, int durationDays) {
        try (Connection conn = db.getConnection();
             PreparedStatement statement = conn.prepareStatement(
                     "UPDATE vip_history SET ended_at = ?, duration_days = ? WHERE id = ?")) {
            statement.setLong(1, endedAt);
            statement.setInt(2, durationDays);
            statement.setLong(3, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao fechar historico de VIP " + id, e);
        }
    }

    /** Ultima entrada ABERTA (ended_at = 0) desse tier pro jogador - usada pra fechar no expire. */
    public HistoryEntry loadOpenHistorySync(UUID uuid, String tierId) {
        String sql = "SELECT * FROM vip_history WHERE player_uuid = ? AND tier_id = ? AND ended_at = 0 ORDER BY id DESC LIMIT 1";
        try (Connection conn = db.getConnection(); PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, tierId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return new HistoryEntry(rs.getLong("id"), tierId, rs.getLong("activated_at"), rs.getLong("ended_at"), rs.getInt("duration_days"));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao carregar historico aberto de " + uuid, e);
        }
        return null;
    }

    public List<HistoryEntry> loadHistorySync(UUID uuid) {
        List<HistoryEntry> result = new ArrayList<>();
        String sql = "SELECT * FROM vip_history WHERE player_uuid = ? ORDER BY activated_at DESC";
        try (Connection conn = db.getConnection(); PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    result.add(new HistoryEntry(rs.getLong("id"), rs.getString("tier_id"), rs.getLong("activated_at"),
                            rs.getLong("ended_at"), rs.getInt("duration_days")));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao carregar historico de " + uuid, e);
        }
        return result;
    }

    public int totalHistoryDaysSync(UUID uuid) {
        String sql = "SELECT COALESCE(SUM(duration_days), 0) AS total FROM vip_history WHERE player_uuid = ?";
        try (Connection conn = db.getConnection(); PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao somar dias de historico de " + uuid, e);
        }
        return 0;
    }

    public int countHistoryByTierSync(UUID uuid, String tierId) {
        String sql = "SELECT COUNT(*) AS total FROM vip_history WHERE player_uuid = ? AND tier_id = ?";
        try (Connection conn = db.getConnection(); PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, tierId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao contar historico por tier de " + uuid, e);
        }
        return 0;
    }

    // ---------------------------------------------------------------- vip_transactions

    public void insertTransactionSync(UUID uuid, String type, String currency, double amount) {
        try (Connection conn = db.getConnection();
             PreparedStatement statement = conn.prepareStatement(
                     "INSERT INTO vip_transactions (player_uuid, type, currency, amount, created_at) VALUES (?, ?, ?, ?, ?)")) {
            statement.setString(1, uuid.toString());
            statement.setString(2, type);
            statement.setString(3, currency);
            statement.setDouble(4, amount);
            statement.setLong(5, System.currentTimeMillis());
            statement.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao inserir transacao de " + uuid, e);
        }
    }

    public Map<String, Double> totalSpentByCurrencySync(UUID uuid) {
        Map<String, Double> result = new HashMap<>();
        String sql = "SELECT currency, SUM(amount) AS total FROM vip_transactions WHERE player_uuid = ? AND type = 'PURCHASE' GROUP BY currency";
        try (Connection conn = db.getConnection(); PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString("currency"), rs.getDouble("total"));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao somar gastos de " + uuid, e);
        }
        return result;
    }

    // ---------------------------------------------------------------- vip_achievements_claimed

    public java.util.Set<String> loadClaimedAchievementsSync(UUID uuid) {
        java.util.Set<String> result = new java.util.HashSet<>();
        String sql = "SELECT achievement_id FROM vip_achievements_claimed WHERE player_uuid = ?";
        try (Connection conn = db.getConnection(); PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    result.add(rs.getString("achievement_id"));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao carregar conquistas de " + uuid, e);
        }
        return result;
    }

    public void markAchievementClaimedSync(UUID uuid, String achievementId) {
        String sql = upsert("vip_achievements_claimed", new String[]{"player_uuid", "achievement_id", "claimed_at"},
                new String[]{"player_uuid", "achievement_id"});
        try {
            execute(sql, ps -> {
                ps.setString(1, uuid.toString());
                ps.setString(2, achievementId);
                ps.setLong(3, System.currentTimeMillis());
            });
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao registrar conquista de " + uuid, e);
        }
    }

    // ---------------------------------------------------------------- vip_referrals

    public record ReferralRow(long id, UUID referrerUuid, UUID referredUuid, long createdAt, boolean completed, long completedAt) {
    }

    public long insertReferralSync(UUID referrer, UUID referred) {
        try (Connection conn = db.getConnection();
             PreparedStatement statement = conn.prepareStatement(
                     "INSERT INTO vip_referrals (referrer_uuid, referred_uuid, created_at) VALUES (?, ?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, referrer.toString());
            statement.setString(2, referred.toString());
            statement.setLong(3, System.currentTimeMillis());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao inserir indicacao " + referrer + " -> " + referred, e);
        }
        return -1;
    }

    public boolean hasAnyReferralSync(UUID referred) {
        String sql = "SELECT 1 FROM vip_referrals WHERE referred_uuid = ?";
        try (Connection conn = db.getConnection(); PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, referred.toString());
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao checar indicacao de " + referred, e);
            return false;
        }
    }

    public ReferralRow findPendingReferralSync(UUID referred) {
        String sql = "SELECT * FROM vip_referrals WHERE referred_uuid = ? AND completed = 0 LIMIT 1";
        try (Connection conn = db.getConnection(); PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, referred.toString());
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return readReferral(rs);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao buscar indicacao pendente de " + referred, e);
        }
        return null;
    }

    private ReferralRow readReferral(ResultSet rs) throws SQLException {
        return new ReferralRow(rs.getLong("id"), UUID.fromString(rs.getString("referrer_uuid")),
                UUID.fromString(rs.getString("referred_uuid")), rs.getLong("created_at"),
                rs.getInt("completed") != 0, rs.getLong("completed_at"));
    }

    public void completeReferralSync(long id) {
        try (Connection conn = db.getConnection();
             PreparedStatement statement = conn.prepareStatement(
                     "UPDATE vip_referrals SET completed = 1, completed_at = ? WHERE id = ?")) {
            statement.setLong(1, System.currentTimeMillis());
            statement.setLong(2, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao completar indicacao " + id, e);
        }
    }

    public record TopReferrer(UUID uuid, int completedCount) {
    }

    public List<TopReferrer> topReferrersSync(int limit) {
        List<TopReferrer> result = new ArrayList<>();
        String sql = "SELECT referrer_uuid, COUNT(*) AS total FROM vip_referrals WHERE completed = 1 " +
                "GROUP BY referrer_uuid ORDER BY total DESC LIMIT ?";
        try (Connection conn = db.getConnection(); PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    result.add(new TopReferrer(UUID.fromString(rs.getString("referrer_uuid")), rs.getInt("total")));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao carregar top indicadores", e);
        }
        return result;
    }

    // ---------------------------------------------------------------- vip_p2p_listings

    public record P2PListing(long id, UUID sellerUuid, long playerVipId, String tierId, long remainingMillis,
                              double price, String currency, long listedAt) {
    }

    public long insertListingSync(P2PListing listing) {
        try (Connection conn = db.getConnection();
             PreparedStatement statement = conn.prepareStatement("""
                INSERT INTO vip_p2p_listings (seller_uuid, player_vip_id, tier_id, remaining_millis, price, currency, listed_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, listing.sellerUuid().toString());
            statement.setLong(2, listing.playerVipId());
            statement.setString(3, listing.tierId());
            statement.setLong(4, listing.remainingMillis());
            statement.setDouble(5, listing.price());
            statement.setString(6, listing.currency());
            statement.setLong(7, listing.listedAt());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao criar anuncio P2P de " + listing.sellerUuid(), e);
        }
        return -1;
    }

    private P2PListing readListing(ResultSet rs) throws SQLException {
        return new P2PListing(rs.getLong("id"), UUID.fromString(rs.getString("seller_uuid")), rs.getLong("player_vip_id"),
                rs.getString("tier_id"), rs.getLong("remaining_millis"), rs.getDouble("price"),
                rs.getString("currency"), rs.getLong("listed_at"));
    }

    public P2PListing loadListingSync(long id) {
        try (Connection conn = db.getConnection();
             PreparedStatement statement = conn.prepareStatement("SELECT * FROM vip_p2p_listings WHERE id = ?")) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return readListing(rs);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao carregar anuncio P2P " + id, e);
        }
        return null;
    }

    public List<P2PListing> loadAllListingsSync() {
        List<P2PListing> result = new ArrayList<>();
        try (Connection conn = db.getConnection(); Statement statement = conn.createStatement();
             ResultSet rs = statement.executeQuery("SELECT * FROM vip_p2p_listings ORDER BY listed_at DESC")) {
            while (rs.next()) {
                result.add(readListing(rs));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao carregar anuncios P2P", e);
        }
        return result;
    }

    public void deleteListingSync(long id) {
        try (Connection conn = db.getConnection();
             PreparedStatement statement = conn.prepareStatement("DELETE FROM vip_p2p_listings WHERE id = ?")) {
            statement.setLong(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao remover anuncio P2P " + id, e);
        }
    }

    // ---------------------------------------------------------------- vip_perk_points / vip_perk_unlocks

    public record PerkPoints(int available, int earned) {
    }

    public PerkPoints loadPerkPointsSync(UUID uuid) {
        String sql = "SELECT points_available, points_earned FROM vip_perk_points WHERE player_uuid = ?";
        try (Connection conn = db.getConnection(); PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return new PerkPoints(rs.getInt("points_available"), rs.getInt("points_earned"));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao carregar pontos de perk de " + uuid, e);
        }
        return new PerkPoints(0, 0);
    }

    public void addPerkPointsSync(UUID uuid, int amount) {
        PerkPoints current = loadPerkPointsSync(uuid);
        String sql = upsert("vip_perk_points", new String[]{"player_uuid", "points_available", "points_earned"},
                new String[]{"player_uuid"});
        try {
            execute(sql, ps -> {
                ps.setString(1, uuid.toString());
                ps.setInt(2, current.available() + amount);
                ps.setInt(3, current.earned() + amount);
            });
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao adicionar pontos de perk de " + uuid, e);
        }
    }

    public void spendPerkPointsSync(UUID uuid, int amount) {
        PerkPoints current = loadPerkPointsSync(uuid);
        String sql = upsert("vip_perk_points", new String[]{"player_uuid", "points_available", "points_earned"},
                new String[]{"player_uuid"});
        try {
            execute(sql, ps -> {
                ps.setString(1, uuid.toString());
                ps.setInt(2, Math.max(0, current.available() - amount));
                ps.setInt(3, current.earned());
            });
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao gastar pontos de perk de " + uuid, e);
        }
    }

    public java.util.Set<String> loadUnlockedPerksSync(UUID uuid) {
        java.util.Set<String> result = new java.util.HashSet<>();
        String sql = "SELECT perk_id FROM vip_perk_unlocks WHERE player_uuid = ?";
        try (Connection conn = db.getConnection(); PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    result.add(rs.getString("perk_id"));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao carregar perks desbloqueados de " + uuid, e);
        }
        return result;
    }

    public void unlockPerkSync(UUID uuid, String perkId) {
        String sql = upsert("vip_perk_unlocks", new String[]{"player_uuid", "perk_id"}, new String[]{"player_uuid", "perk_id"});
        try {
            execute(sql, ps -> {
                ps.setString(1, uuid.toString());
                ps.setString(2, perkId);
            });
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao desbloquear perk de " + uuid, e);
        }
    }

    /**
     * Varre a tabela inteira (nao so o cache de online) - usado uma unica vez no
     * startup para compensar o tempo de downtime dos VIPs do tipo SERVER, que devem
     * descontar independente de quem estava online.
     */
    // ---------------------------------------------------------------- misc

    public List<PlayerVip> loadAllActivePlayerVipsSync() {
        List<PlayerVip> result = new ArrayList<>();
        try (Connection conn = db.getConnection(); Statement statement = conn.createStatement();
             ResultSet rs = statement.executeQuery("SELECT * FROM player_vips WHERE status = 'ACTIVE'")) {
            while (rs.next()) {
                result.add(readPlayerVip(rs));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao carregar todos os PlayerVips ativos", e);
        }
        return result;
    }
}
