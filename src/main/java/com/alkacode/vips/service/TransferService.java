package com.alkacode.vips.service;

import com.alkacode.vips.manager.PlayerVipManager;
import com.alkacode.vips.model.PlayerVip;
import com.alkacode.vips.model.enums.VipStatus;

import java.util.UUID;

/** "Transferir VIP entre jogadores" - move a assinatura ATIVA (tempo restante incluso) de um jogador pra outro. */
public final class TransferService {

    private final PlayerVipManager playerVipManager;

    public TransferService(PlayerVipManager playerVipManager) {
        this.playerVipManager = playerVipManager;
    }

    public enum Result { SUCCESS, NO_VIP_SELECTED, TARGET_IS_SELF }

    public Result transfer(UUID fromUuid, UUID toUuid, PlayerVip vip) {
        if (fromUuid.equals(toUuid)) {
            return Result.TARGET_IS_SELF;
        }
        if (vip == null || vip.status() != VipStatus.ACTIVE) {
            return Result.NO_VIP_SELECTED;
        }

        playerVipManager.remove(vip);
        PlayerVip transferred = new PlayerVip(-1, toUuid, vip.vipTypeId(), vip.keyId(), VipStatus.ACTIVE,
                System.currentTimeMillis(), vip.expiresAt(), vip.totalDuration(), false, 0);
        playerVipManager.addVip(transferred);
        return Result.SUCCESS;
    }
}
