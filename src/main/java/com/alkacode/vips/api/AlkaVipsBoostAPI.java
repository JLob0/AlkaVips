package com.alkacode.vips.api;

/**
 * API publica dos boosts de servidor ativos ("VIP Solidario", Ideia 1) - registrada
 * no ServicesManager pra AlkaDrop/AlkaShop/AlkaMines consultarem antes de aplicar
 * drop/venda/chance de bloco raro. Multiplicador 1.0 = sem boost ativo desse tipo
 * (nunca lanca, sempre seguro de multiplicar direto). A integracao do LADO
 * CONSUMIDOR (Drop/Shop/Mines realmente checando isso) NAO foi feita nesta sessao -
 * so o lado que registra/expira/expoe os boosts.
 */
public interface AlkaVipsBoostAPI {

    double getDropMultiplier();

    double getSellMultiplier();

    double getMineRareChanceMultiplier();

    boolean hasActiveBoost();
}
