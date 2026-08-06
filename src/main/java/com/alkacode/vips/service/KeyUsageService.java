package com.alkacode.vips.service;

import com.alkacode.vips.manager.KeyManager;
import com.alkacode.vips.manager.VipTypeManager;
import com.alkacode.vips.model.VipKey;
import com.alkacode.vips.model.VipType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class KeyUsageService {

    private final KeyManager keyManager;
    private final VipTypeManager vipTypeManager;
    private final ActivationService activationService;

    public KeyUsageService(KeyManager keyManager, VipTypeManager vipTypeManager, ActivationService activationService) {
        this.keyManager = keyManager;
        this.vipTypeManager = vipTypeManager;
        this.activationService = activationService;
    }

    public enum Result {
        SUCCESS, NOT_FOUND, ALREADY_USED
    }

    /**
     * So chamado a partir de contextos que ja rodam na thread principal (comando,
     * clique de inventario, PlayerInteractEvent) - usa a leitura sincrona do
     * KeyManager (mesmo padrao de fallback bloqueante do EconomyManager) para poder
     * tocar a API do Bukkit (inventario, mensagens, comandos) sem trocar de thread.
     */
    public Result use(Player player, String code, ItemStack physicalItem) {
        VipKey key = keyManager.findSync(code);
        if (key == null) {
            return Result.NOT_FOUND;
        }
        if (key.used()) {
            return Result.ALREADY_USED;
        }
        VipType vipType = vipTypeManager.get(key.vipTypeId());
        if (vipType == null) {
            return Result.NOT_FOUND;
        }

        key.used(true);
        key.usedBy(player.getUniqueId());
        key.usedAt(System.currentTimeMillis());
        key.forSale(false);
        keyManager.save(key);

        if (physicalItem != null) {
            physicalItem.setAmount(physicalItem.getAmount() - 1);
        }

        activationService.activate(player, vipType, key.duration(), key.id(), false);
        return Result.SUCCESS;
    }
}
