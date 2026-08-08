package com.alkacode.vips.model;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * iconMaterial/iconItemsAdder sao so o icone do kit na KitsMenu (nunca entregues ao
 * jogador) - independentes dos items() de verdade que o resgate da. itemsAdderItemIds
 * sao namespaces do ItemsAdder resolvidos em tempo de entrega (ver
 * KitManager#deliver) - ficam separados de items() porque esse continua sendo a
 * lista de ItemStack nativos do Bukkit (serializacao ==: org.bukkit.inventory.ItemStack
 * do kits.yml), e um namespace ausente/plugin fora do ar nao pode virar um ItemStack
 * "congelado" no load sem o ItemsAdder disponivel naquele momento.
 */
public record VipKit(String vipTypeId, String id, String displayName, long cooldownMillis, List<ItemStack> items,
                      Material iconMaterial, String iconItemsAdder, List<String> itemsAdderItemIds) {

    public String key() {
        return vipTypeId + ":" + id;
    }
}
