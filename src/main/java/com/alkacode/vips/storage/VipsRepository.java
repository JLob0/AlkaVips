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
                CREATE TABLE IF NOT EXISTS vips_state (
                    id INTEGER PRIMARY KEY CHECK (id = 1),
                    last_shutdown_at BIGINT NOT NULL DEFAULT 0
                )
                """);
            statement.execute("""
                CREATE TABLE IF NOT EXISTS vip_kit_cooldowns (
                    player_uuid VARCHAR(36) NOT NULL,
                    kit_key VARCHAR(96) NOT NULL,
                    claimed_at BIGINT NOT NULL,
                    PRIMARY KEY (player_uuid, kit_key)
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

    /**
     * Varre a tabela inteira (nao so o cache de online) - usado uma unica vez no
     * startup para compensar o tempo de downtime dos VIPs do tipo SERVER, que devem
     * descontar independente de quem estava online.
     */
    // ---------------------------------------------------------------- vip_kit_cooldowns

    public Map<String, Long> loadKitCooldownsSync(UUID uuid) {
        Map<String, Long> result = new HashMap<>();
        try (Connection conn = db.getConnection();
             PreparedStatement statement = conn.prepareStatement(
                     "SELECT kit_key, claimed_at FROM vip_kit_cooldowns WHERE player_uuid = ?")) {
            statement.setString(1, uuid.toString());
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString("kit_key"), rs.getLong("claimed_at"));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao carregar cooldowns de kits de " + uuid, e);
        }
        return result;
    }

    public CompletableFuture<Map<String, Long>> loadKitCooldowns(UUID uuid) {
        return async(() -> loadKitCooldownsSync(uuid));
    }

    public void saveKitCooldownSync(UUID uuid, String kitKey, long claimedAt) {
        String sql = upsert("vip_kit_cooldowns", new String[]{"player_uuid", "kit_key", "claimed_at"},
                new String[]{"player_uuid", "kit_key"});
        try {
            execute(sql, ps -> {
                ps.setString(1, uuid.toString());
                ps.setString(2, kitKey);
                ps.setLong(3, claimedAt);
            });
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao salvar cooldown do kit " + kitKey + " de " + uuid, e);
        }
    }

    public CompletableFuture<Void> saveKitCooldown(UUID uuid, String kitKey, long claimedAt) {
        return asyncRun(() -> saveKitCooldownSync(uuid, kitKey, claimedAt));
    }

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
