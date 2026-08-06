package com.alkacode.vips.api;

import com.alkacode.vips.manager.CreditManager;
import com.alkacode.vips.manager.KeyManager;
import com.alkacode.vips.manager.PlayerVipManager;
import com.alkacode.vips.model.PlayerVip;
import com.alkacode.vips.model.VipKey;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class AlkaVipsAPIProvider implements AlkaVipsAPI {

    private final PlayerVipManager playerVipManager;
    private final CreditManager creditManager;
    private final KeyManager keyManager;

    public AlkaVipsAPIProvider(PlayerVipManager playerVipManager, CreditManager creditManager, KeyManager keyManager) {
        this.playerVipManager = playerVipManager;
        this.creditManager = creditManager;
        this.keyManager = keyManager;
    }

    @Override
    public CompletableFuture<Boolean> hasVip(UUID player, String vipId) {
        return CompletableFuture.completedFuture(
                playerVipManager.getActiveVips(player).stream().anyMatch(v -> v.vipTypeId().equals(vipId)));
    }

    @Override
    public CompletableFuture<PlayerVip> getActiveVip(UUID player) {
        return CompletableFuture.completedFuture(playerVipManager.getSelectedVip(player).orElse(null));
    }

    @Override
    public CompletableFuture<List<PlayerVip>> getPlayerVips(UUID player) {
        return CompletableFuture.completedFuture(playerVipManager.getVips(player));
    }

    @Override
    public CompletableFuture<Double> getCredits(UUID player) {
        return CompletableFuture.completedFuture(creditManager.getCredits(player));
    }

    @Override
    public CompletableFuture<Boolean> hasKey(String keyId) {
        return keyManager.find(keyId).thenApply(key -> key != null && !key.used());
    }

    @Override
    public CompletableFuture<VipKey> getKey(String keyId) {
        return keyManager.find(keyId);
    }

    @Override
    public void addCredits(UUID player, double amount) {
        creditManager.add(player, amount);
    }

    @Override
    public void removeCredits(UUID player, double amount) {
        creditManager.remove(player, amount);
    }
}
