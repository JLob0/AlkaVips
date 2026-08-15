package com.alkacode.vips.manager;

import com.alkacode.vips.api.AlkaVipsBoostAPI;
import com.alkacode.vips.hook.AlkaFlairHook;
import com.alkacode.vips.model.ServerBoostConfig;
import com.alkacode.vips.model.VipType;
import com.alkacode.vips.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * "VIP Solidario" (Ideia 1) - ao ativar um tier com {@code server-boost} configurado,
 * registra um multiplicador GLOBAL e temporario (expira sozinho, checado por
 * {@link #activeEffects()} a cada consulta - sem task de limpeza, os boosts vencidos
 * simplesmente param de contar na proxima leitura). Multiplos boosts do mesmo tipo
 * ativos ao mesmo tempo (2 jogadores compraram VIP perto um do outro) se somam-se
 * multiplicativamente, nao substituem um ao outro.
 */
public final class BoostManager implements AlkaVipsBoostAPI {

    private record ActiveBoost(List<ServerBoostConfig.BoostEffect> effects, long expiresAt) {
    }

    private final List<ActiveBoost> active = new CopyOnWriteArrayList<>();
    private final AlkaFlairHook flairHook;

    public BoostManager(AlkaFlairHook flairHook) {
        this.flairHook = flairHook;
    }

    public void trigger(Player buyer, VipType vipType) {
        ServerBoostConfig config = vipType.serverBoost();
        if (config == null || !config.enabled()) {
            return;
        }
        long expiresAt = System.currentTimeMillis() + config.durationMinutes() * 60_000L;
        active.add(new ActiveBoost(config.effects(), expiresAt));

        if (config.broadcast()) {
            String msg = "<gold>" + buyer.getName() + " <yellow>ativou <white>" + TextUtil.plain(vipType.display())
                    + " <yellow>! Servidor ganhou bonus temporario por " + config.durationMinutes() + " minuto(s)!";
            Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(TextUtil.legacyParse(msg, Map.of())));
        }

        flairHook.addTag(buyer.getUniqueId(), config.rewardTagId());
        flairHook.addMedal(buyer.getUniqueId(), config.rewardMedalId());
    }

    private List<ServerBoostConfig.BoostEffect> activeEffects() {
        long now = System.currentTimeMillis();
        active.removeIf(boost -> boost.expiresAt() < now);
        List<ServerBoostConfig.BoostEffect> result = new ArrayList<>();
        for (ActiveBoost boost : active) {
            result.addAll(boost.effects());
        }
        return result;
    }

    private double multiplierFor(String type) {
        double multiplier = 1.0;
        for (ServerBoostConfig.BoostEffect effect : activeEffects()) {
            if (effect.type().equalsIgnoreCase(type)) {
                multiplier *= effect.value();
            }
        }
        return multiplier;
    }

    @Override
    public double getDropMultiplier() {
        return multiplierFor("DROP_MULTIPLIER");
    }

    @Override
    public double getSellMultiplier() {
        return multiplierFor("SELL_MULTIPLIER");
    }

    @Override
    public double getMineRareChanceMultiplier() {
        return multiplierFor("MINE_RARE_CHANCE");
    }

    @Override
    public boolean hasActiveBoost() {
        return !activeEffects().isEmpty();
    }
}
