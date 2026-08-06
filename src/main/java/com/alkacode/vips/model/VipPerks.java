package com.alkacode.vips.model;

/**
 * Perks mecanicos que o proprio AlkaVips implementa (sem depender de outro plugin lendo
 * permissao do LuckPerms) - diferente de perks numericos como homes/pets/claim-blocks, que
 * continuam sendo so permissao concedida via group-command (outro plugin externo os le).
 */
public record VipPerks(
        boolean fly,
        boolean autoPickup,
        boolean autoSmelt,
        boolean keepInventory,
        boolean back,
        boolean feed,
        boolean reparar,
        boolean bigorna,
        boolean bancada,
        boolean coloredSigns
) {

    public static final VipPerks NONE = new VipPerks(false, false, false, false, false, false, false, false, false, false);
}
