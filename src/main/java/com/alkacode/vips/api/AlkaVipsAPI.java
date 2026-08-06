package com.alkacode.vips.api;

import com.alkacode.vips.model.PlayerVip;
import com.alkacode.vips.model.VipKey;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface AlkaVipsAPI {

    CompletableFuture<Boolean> hasVip(UUID player, String vipId);

    CompletableFuture<PlayerVip> getActiveVip(UUID player);

    CompletableFuture<List<PlayerVip>> getPlayerVips(UUID player);

    CompletableFuture<Double> getCredits(UUID player);

    CompletableFuture<Boolean> hasKey(String keyId);

    CompletableFuture<VipKey> getKey(String keyId);

    void addCredits(UUID player, double amount);

    void removeCredits(UUID player, double amount);
}
