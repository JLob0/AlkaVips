package com.alkacode.vips.model;

import java.util.List;

/**
 * "VIP Solidario" - ao ativar este tier, todo o servidor ganha um multiplicador
 * temporario (ver {@link com.alkacode.vips.manager.BoostManager}). Os multiplicadores
 * em si sao so REGISTRADOS aqui - AlkaDrop/AlkaShop/AlkaMines precisam consultar
 * {@code AlkaVipsBoostAPI} (exposta no ServicesManager) pra de fato aplicar o efeito;
 * essa integracao do lado consumidor NAO foi feita nesta sessao, so o lado que
 * registra/expira os boosts.
 */
public record ServerBoostConfig(
        boolean enabled,
        int durationMinutes,
        boolean broadcast,
        List<BoostEffect> effects,
        String rewardMedalId,
        String rewardTagId,
        int rewardTagDurationHours
) {
    public record BoostEffect(String type, double value) {
    }
}
