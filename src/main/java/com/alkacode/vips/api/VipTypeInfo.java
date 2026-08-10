package com.alkacode.vips.api;

/**
 * Resumo publico de um VipType (vips.yml) pra consumidores externos - id/display/order,
 * sem expor toda a definicao interna (perks, comandos de grupo, mensagens, etc). order
 * e o mesmo usado por {@link com.alkacode.vips.manager.VipTypeManager#getOrderedVips()}
 * (menor -> maior): um plugin consumidor pode comparar dois vipId sem hardcodar nenhum
 * nome de tier - criar um VIP novo com order maior automaticamente vira "o melhor" pra
 * quem consulta a API, sem precisar mudar codigo em nenhum outro plugin.
 */
public record VipTypeInfo(String id, String display, int order) {
}
