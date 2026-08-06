package com.alkacode.vips.listener;

import com.alkacode.vips.manager.KitManager;
import com.alkacode.vips.manager.PlayerVipManager;
import com.alkacode.vips.manager.VipTypeManager;
import com.alkacode.vips.model.PlayerVip;
import com.alkacode.vips.model.VipPlayerData;
import com.alkacode.vips.model.VipType;
import com.alkacode.vips.model.enums.TimeType;
import com.alkacode.vips.service.ExpirationService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public final class PlayerJoinListener implements Listener {

    private final JavaPlugin plugin;
    private final PlayerVipManager playerVipManager;
    private final VipTypeManager vipTypeManager;
    private final ExpirationService expirationService;
    private final KitManager kitManager;

    public PlayerJoinListener(JavaPlugin plugin, PlayerVipManager playerVipManager, VipTypeManager vipTypeManager,
                               ExpirationService expirationService, KitManager kitManager) {
        this.plugin = plugin;
        this.playerVipManager = playerVipManager;
        this.vipTypeManager = vipTypeManager;
        this.expirationService = expirationService;
        this.kitManager = kitManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        handle(event.getPlayer());
    }

    public void handle(Player player) {
        UUID uuid = player.getUniqueId();
        kitManager.loadForJoin(uuid);
        playerVipManager.loadForJoin(uuid).thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
            applyOnlineCompensation(uuid);
            expirationService.checkPlayer(uuid);
        }));
    }

    /**
     * VIPs do tipo ONLINE so descontam enquanto o jogador esta conectado - o gap entre
     * o ultimo quit e este join precisa ser devolvido ao expiresAt, senao o relogio de
     * parede teria descontado esse tempo indevidamente.
     */
    private void applyOnlineCompensation(UUID uuid) {
        VipPlayerData data = playerVipManager.dataOf(uuid);
        long lastQuitAt = data.lastQuitAt();
        if (lastQuitAt <= 0) {
            return;
        }
        long gap = System.currentTimeMillis() - lastQuitAt;
        if (gap > 0) {
            for (PlayerVip vip : playerVipManager.getActiveVips(uuid)) {
                VipType type = vipTypeManager.get(vip.vipTypeId());
                if (type != null && type.timeType() == TimeType.ONLINE && !vip.isPermanent() && !vip.frozen()) {
                    vip.expiresAt(vip.expiresAt() + gap);
                    playerVipManager.update(vip);
                }
            }
        }
        data.lastQuitAt(0);
        playerVipManager.saveData(uuid);
    }
}
