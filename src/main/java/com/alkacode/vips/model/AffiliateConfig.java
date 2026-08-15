package com.alkacode.vips.model;

import java.util.List;

/**
 * Config global (nao por tier) do "VIP Affiliate" (Ideia 4) - affiliate.yml.
 * Diferente do documento original (que previa "% do valor gasto"), aqui a
 * recompensa por nivel e um valor FIXO em moeda + dias de bonus, porque o AlkaVips
 * nao tem um preco de compra direto associado a ativacao (VIPs vem de keys, que nao
 * carregam preco embutido) - percentual "de que valor" nao teria uma resposta
 * confiavel. bonus-days estende o VIP ATIVO do indicador (se ele tiver um).
 */
public record AffiliateConfig(boolean enabled, int maxDepth, List<Level> levels, String leaderboardTopTag,
                               String leaderboardTopMedal, int leaderboardTopMedalMinCount) {
    public record Level(String currency, double amount, long bonusDaysMillis) {
    }

    public Level levelOrNull(int depth) {
        return depth >= 1 && depth <= levels.size() ? levels.get(depth - 1) : null;
    }
}
