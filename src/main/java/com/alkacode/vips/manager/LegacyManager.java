package com.alkacode.vips.manager;

import com.alkacode.vips.model.LegacyConfig;
import com.alkacode.vips.model.PlayerVip;
import com.alkacode.vips.model.VipType;
import com.alkacode.vips.model.enums.VipStatus;
import com.alkacode.vips.storage.VipsRepository;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * "VIP Legacy" (Ideia 2) - quando um tier com {@code legacy:} configurado expira, o
 * jogador cai automaticamente pro {@code fall-tier} por {@code grace-days}, com
 * desconto de renovacao (consultado pelo UpgradeService na hora de calcular preco -
 * o AlkaVips nao tem compra direta por moeda, so upgrade tem preco em currency, entao
 * o desconto se aplica la). O grace em si e um {@link PlayerVip} ACTIVE de verdade
 * (nao um estado paralelo) - o jogador realmente usa o fall-tier normalmente durante
 * o grace, so perde ele (e o registro de desconto) se nao renovar a tempo.
 */
public final class LegacyManager {

    private final VipsRepository database;
    private final PlayerVipManager playerVipManager;
    private final VipTypeManager vipTypeManager;

    public LegacyManager(VipsRepository database, PlayerVipManager playerVipManager, VipTypeManager vipTypeManager) {
        this.database = database;
        this.playerVipManager = playerVipManager;
        this.vipTypeManager = vipTypeManager;
    }

    /** Chamado pelo ExpirationService quando um PlayerVip expira de verdade (nao em accumulate). */
    public void grantGraceIfConfigured(UUID uuid, VipType expiredType) {
        LegacyConfig legacy = expiredType.legacy();
        if (legacy == null || legacy.fallTierId().isBlank()) {
            return;
        }
        VipType fallType = vipTypeManager.get(legacy.fallTierId());
        if (fallType == null) {
            return;
        }
        long graceMillis = legacy.graceDays() * 86_400_000L;
        long graceUntil = System.currentTimeMillis() + graceMillis;

        PlayerVip grantedVip = new PlayerVip(-1, uuid, fallType.id(), "legacy", VipStatus.ACTIVE,
                System.currentTimeMillis(), graceUntil, graceMillis, false, 0);
        PlayerVip stored = playerVipManager.addVip(grantedVip).join();

        database.saveLegacyGrantSync(uuid, new VipsRepository.LegacyGrant(
                expiredType.id(), fallType.id(), stored.id(), graceUntil, legacy.renewalDiscountPercent()));
    }

    /** Desconto de renovacao ativo pro jogador nesse tier original, ou 0 se nao houver grace ativo. */
    public int getRenewalDiscountPercent(UUID uuid, String originalTierId) {
        VipsRepository.LegacyGrant grant = database.loadLegacyGrantSync(uuid, originalTierId);
        if (grant == null || grant.graceUntil() < System.currentTimeMillis()) {
            return 0;
        }
        return grant.discountPercent();
    }

    /** Limpa o registro de grace quando o jogador renova o tier original de verdade (chamado pela ativacao). */
    public void clearGraceOnRenewal(UUID uuid, String originalTierId) {
        database.deleteLegacyGrantSync(uuid, originalTierId);
    }

    /** Lembrete de renovacao no login - so pros grants com notify-daily=true e ainda dentro do grace. */
    public void notifyIfInGrace(Player player) {
        UUID uuid = player.getUniqueId();
        for (VipsRepository.LegacyGrant grant : database.loadLegacyGrantsForPlayerSync(uuid)) {
            if (grant.graceUntil() < System.currentTimeMillis()) {
                continue;
            }
            VipType originalType = vipTypeManager.get(grant.originalTierId());
            VipType fallType = vipTypeManager.get(grant.fallTierId());
            LegacyConfig legacy = originalType != null ? originalType.legacy() : null;
            if (originalType == null || fallType == null || legacy == null || !legacy.notifyDaily()) {
                continue;
            }
            long daysLeft = Math.max(0, (grant.graceUntil() - System.currentTimeMillis()) / 86_400_000L);
            player.sendMessage(com.alkacode.vips.util.TextUtil.legacyParse(
                    "<yellow>Voce esta no periodo de carencia do <white><original></white>! "
                            + "Renove em ate <white><days> dia(s) <yellow>e ganhe <white><discount>%</white> de desconto.",
                    java.util.Map.of(
                            "original", com.alkacode.vips.util.TextUtil.plain(originalType.display()),
                            "days", String.valueOf(daysLeft),
                            "discount", String.valueOf(grant.discountPercent()))));
        }
    }
}
