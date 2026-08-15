package com.alkacode.vips.model;

/**
 * Um no da arvore de perks (Ideia 3, desativada por padrao) - {@code effectType}/
 * {@code effectValue} sao registrados mas a APLICACAO do efeito no jogo nao foi
 * feita nesta sessao (mesma limitacao documentada do BoostManager: o lado que
 * "sabe" quais perks o jogador tem existe e e consultavel, os hooks em
 * Mines/Shop/EnderChest/Kits pra realmente aplicar cada efeito ficam pra depois).
 */
public record PerkNode(String id, String branchId, String name, int cost, String requiresPerkId,
                        String effectType, double effectValue) {
}
