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

    /** Info publica (id/display/order) de um vipId - null se o vipId nao existir. Use order pra comparar tiers sem hardcode. */
    CompletableFuture<VipTypeInfo> getVipTypeInfo(String vipId);

    /** Todos os VipTypes conhecidos, ordenados por order crescente (o ultimo da lista e o de maior tier). */
    CompletableFuture<List<VipTypeInfo>> getVipTypesOrdered();

    /** true se o jogador tem o perk desbloqueado na VIP Perk Tree (perktree.yml). Sempre
     * false se a perk tree estiver desativada (enabled: false, padrão) ou o perkId não existir. */
    CompletableFuture<Boolean> hasPerk(UUID player, String perkId);

    /** Multiplicador de venda dos perks SELL_MULTIPLIER desbloqueados na Perk Tree
     * (perktree.yml). 1.0 = nenhum perk desse tipo desbloqueado/perk tree desativada
     * (nunca lanca, sempre seguro de multiplicar direto no preco). */
    CompletableFuture<Double> getPerkSellMultiplier(UUID player);

    /** Soma de slots extra de mina particular concedidos pelos perks EXTRA_MINE_SLOT
     * desbloqueados na Perk Tree. 0 = nenhum/desativada. */
    CompletableFuture<Integer> getExtraMineSlots(UUID player);
}
