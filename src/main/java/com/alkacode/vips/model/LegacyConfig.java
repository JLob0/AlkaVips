package com.alkacode.vips.model;

/**
 * "VIP Legacy" - quando o VIP deste tier expira, o jogador cai automaticamente pro
 * {@code fallTier} por {@code graceDays} dias, com desconto de renovacao (aplicado
 * no preco de upgrade, ja que o AlkaVips nao tem compra direta por moeda - so
 * upgrade tem preco em currency). {@code null} (nao {@code LegacyConfig.NONE}) em
 * {@link VipType#legacy()} significa "sem legacy configurado pra esse tier".
 */
public record LegacyConfig(String fallTierId, int graceDays, int renewalDiscountPercent, boolean notifyDaily) {
}
