package com.alkacode.vips.manager;

import com.alkacode.vips.model.PlayerVip;
import com.alkacode.vips.model.VipPlayerData;
import com.alkacode.vips.model.enums.VipStatus;
import com.alkacode.vips.storage.VipsRepository;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Cache em memoria (UUID -> lista de PlayerVip / VipPlayerData) para jogadores online -
 * mesmo padrao do EconomyManager do AlkaEconomy. Toda mutacao atualiza o cache de forma
 * sincrona e persiste no SQLite em background.
 */
public final class PlayerVipManager {

    private final VipsRepository database;
    private final Map<UUID, List<PlayerVip>> vipsCache = new ConcurrentHashMap<>();
    private final Map<UUID, VipPlayerData> dataCache = new ConcurrentHashMap<>();

    public PlayerVipManager(VipsRepository database) {
        this.database = database;
    }

    public CompletableFuture<Void> loadForJoin(Player player) {
        return loadForJoin(player.getUniqueId());
    }

    public CompletableFuture<Void> loadForJoin(UUID uuid) {
        CompletableFuture<Void> vips = database.loadPlayerVips(uuid)
                .thenAccept(list -> vipsCache.put(uuid, new CopyOnWriteArrayList<>(list)));
        CompletableFuture<Void> data = database.loadPlayerData(uuid)
                .thenAccept(d -> dataCache.put(uuid, d));
        return CompletableFuture.allOf(vips, data);
    }

    public void unload(UUID uuid) {
        vipsCache.remove(uuid);
        dataCache.remove(uuid);
    }

    private List<PlayerVip> vipsOf(UUID uuid) {
        return vipsCache.computeIfAbsent(uuid, database::loadPlayerVipsSync);
    }

    public VipPlayerData dataOf(UUID uuid) {
        return dataCache.computeIfAbsent(uuid, database::loadPlayerDataSync);
    }

    public List<PlayerVip> getVips(UUID uuid) {
        return List.copyOf(vipsOf(uuid));
    }

    public List<PlayerVip> getActiveVips(UUID uuid) {
        return vipsOf(uuid).stream().filter(v -> v.status() == VipStatus.ACTIVE).collect(Collectors.toList());
    }

    public List<PlayerVip> getActiveVipsOfType(UUID uuid, String vipTypeId) {
        return getActiveVips(uuid).stream().filter(v -> v.vipTypeId().equals(vipTypeId)).collect(Collectors.toList());
    }

    public List<PlayerVip> getPendingVips(UUID uuid) {
        return vipsOf(uuid).stream().filter(v -> v.status() == VipStatus.PENDING).collect(Collectors.toList());
    }

    public Optional<PlayerVip> getById(UUID uuid, long id) {
        return vipsOf(uuid).stream().filter(v -> v.id() == id).findFirst();
    }

    public Optional<PlayerVip> getSelectedVip(UUID uuid) {
        List<PlayerVip> active = getActiveVips(uuid);
        if (active.isEmpty()) {
            return Optional.empty();
        }
        Long selected = dataOf(uuid).selectedVipId();
        if (selected != null) {
            Optional<PlayerVip> match = active.stream().filter(v -> v.id() == selected).findFirst();
            if (match.isPresent()) {
                return match;
            }
        }
        return active.stream().max(Comparator.comparingLong(PlayerVip::activatedAt));
    }

    public Optional<PlayerVip> cycleSelectedVip(UUID uuid) {
        List<PlayerVip> active = getActiveVips(uuid);
        if (active.size() < 2) {
            return getSelectedVip(uuid);
        }
        active.sort(Comparator.comparingLong(PlayerVip::id));
        Optional<PlayerVip> current = getSelectedVip(uuid);
        int nextIndex = 0;
        if (current.isPresent()) {
            int index = active.indexOf(current.get());
            nextIndex = (index + 1) % active.size();
        }
        PlayerVip next = active.get(nextIndex);
        selectVip(uuid, next.id());
        return Optional.of(next);
    }

    public void selectVip(UUID uuid, long vipId) {
        VipPlayerData data = dataOf(uuid);
        data.selectedVipId(vipId);
        database.savePlayerData(data);
    }

    public void saveData(UUID uuid) {
        database.savePlayerData(dataOf(uuid));
    }

    public CompletableFuture<PlayerVip> addVip(PlayerVip vip) {
        return database.insertPlayerVip(vip).thenApply(id -> {
            PlayerVip stored = new PlayerVip(id, vip.playerUuid(), vip.vipTypeId(), vip.keyId(), vip.status(),
                    vip.activatedAt(), vip.expiresAt(), vip.totalDuration(), vip.frozen(), vip.frozenAt());
            vipsCache.computeIfAbsent(vip.playerUuid(), u -> new CopyOnWriteArrayList<>()).add(stored);
            return stored;
        });
    }

    public void update(PlayerVip vip) {
        database.updatePlayerVip(vip);
    }

    public void remove(PlayerVip vip) {
        vipsOf(vip.playerUuid()).remove(vip);
        database.deletePlayerVip(vip.id());
    }
}
