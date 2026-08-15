package com.alkacode.vips.model;

/**
 * Conquista da Carteira VIP (wallet.yml / config.yml) - regra + recompensa. Tipos de
 * requirement suportados por {@link com.alkacode.vips.manager.WalletManager}:
 * FIRST_PURCHASE, TOTAL_DAYS, TOTAL_SPENT (precisa de currency), RENEW_SAME_TIER.
 */
public record Achievement(
        String id,
        String name,
        String requirementType,
        String requirementCurrency,
        long requirementValue,
        String rewardTagId,
        String rewardMedalId
) {
}
