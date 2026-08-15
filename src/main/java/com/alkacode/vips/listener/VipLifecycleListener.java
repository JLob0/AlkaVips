package com.alkacode.vips.listener;

import com.alkacode.vips.event.VipActivateEvent;
import com.alkacode.vips.event.VipExpireEvent;
import com.alkacode.vips.manager.AffiliateManager;
import com.alkacode.vips.manager.BoostManager;
import com.alkacode.vips.manager.LegacyManager;
import com.alkacode.vips.manager.PerkTreeManager;
import com.alkacode.vips.manager.VipTypeManager;
import com.alkacode.vips.manager.WalletManager;
import com.alkacode.vips.hook.AlkaFlairHook;
import com.alkacode.vips.hook.AlkaItemsHook;
import com.alkacode.vips.model.VipType;
import com.alkacode.vips.util.EventActionExecutor;
import com.alkacode.vips.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Map;

/**
 * Centraliza os efeitos colaterais das novas features (Wallet/Legacy/Boost/
 * Affiliate/PerkTree/acoes-por-evento) escutando os eventos que o AlkaVips ja
 * disparava (VipActivateEvent/VipExpireEvent) - evita inflar ainda mais o
 * construtor de ActivationService/ExpirationService, que ja tinham bastante
 * dependencia. Roda em prioridade NORMAL (padrao), depois do que o
 * ActivationService/ExpirationService ja fizeram de essencial.
 */
public final class VipLifecycleListener implements Listener {

    private final VipTypeManager vipTypeManager;
    private final WalletManager walletManager;
    private final LegacyManager legacyManager;
    private final BoostManager boostManager;
    private final AffiliateManager affiliateManager;
    private final PerkTreeManager perkTreeManager;
    private final AlkaFlairHook flairHook;
    private final AlkaItemsHook itemsHook;

    public VipLifecycleListener(VipTypeManager vipTypeManager, WalletManager walletManager, LegacyManager legacyManager,
                                 BoostManager boostManager, AffiliateManager affiliateManager,
                                 PerkTreeManager perkTreeManager, AlkaFlairHook flairHook, AlkaItemsHook itemsHook) {
        this.vipTypeManager = vipTypeManager;
        this.walletManager = walletManager;
        this.legacyManager = legacyManager;
        this.boostManager = boostManager;
        this.affiliateManager = affiliateManager;
        this.perkTreeManager = perkTreeManager;
        this.flairHook = flairHook;
        this.itemsHook = itemsHook;
    }

    @EventHandler
    public void onActivate(VipActivateEvent event) {
        Player player = event.getPlayer();
        VipType vipType = event.getVipType();
        boolean isFirstEverVip = !event.isAccumulated() && walletManager.history(player.getUniqueId()).isEmpty();

        EventActionExecutor.run(vipType.eventsOnActivate(), player,
                Map.of("player", player.getName(), "vip", TextUtil.plain(vipType.display())));

        if (!event.isAccumulated()) {
            walletManager.onActivate(player.getUniqueId(), vipType.id(), event.getPlayerVip().activatedAt());
            legacyManager.clearGraceOnRenewal(player.getUniqueId(), vipType.id());
            boostManager.trigger(player, vipType);
            perkTreeManager.grantPoints(player.getUniqueId(), vipType.id());
            vipType.itemRewards().forEach(templateId -> itemsHook.giveIfMissing(player, templateId));
        }

        if (isFirstEverVip) {
            affiliateManager.onFirstActivation(player.getUniqueId(), flairHook);
        }
    }

    @EventHandler
    public void onExpire(VipExpireEvent event) {
        VipType vipType = vipTypeManager.get(event.getVipTypeId());
        if (vipType == null) {
            return;
        }
        Player player = Bukkit.getPlayer(event.getPlayerUuid());
        Map<String, String> placeholders = Map.of(
                "player", player != null ? player.getName() : Bukkit.getOfflinePlayer(event.getPlayerUuid()).getName(),
                "vip", TextUtil.plain(vipType.display()));

        if (player != null) {
            EventActionExecutor.run(vipType.eventsOnExpire(), player, placeholders);
        } else {
            EventActionExecutor.runOffline(vipType.eventsOnExpire(), event.getPlayerUuid(),
                    Bukkit.getOfflinePlayer(event.getPlayerUuid()).getName(), placeholders);
        }

        walletManager.onExpire(event.getPlayerUuid(), vipType.id());
        legacyManager.grantGraceIfConfigured(event.getPlayerUuid(), vipType);
    }
}
