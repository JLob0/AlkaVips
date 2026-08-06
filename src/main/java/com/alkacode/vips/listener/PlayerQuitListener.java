package com.alkacode.vips.listener;

import com.alkacode.vips.manager.KitManager;
import com.alkacode.vips.manager.PlayerVipManager;
import com.alkacode.vips.model.VipPlayerData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerQuitListener implements Listener {

    private final PlayerVipManager playerVipManager;
    private final KitManager kitManager;

    public PlayerQuitListener(PlayerVipManager playerVipManager, KitManager kitManager) {
        this.playerVipManager = playerVipManager;
        this.kitManager = kitManager;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        var uuid = event.getPlayer().getUniqueId();
        VipPlayerData data = playerVipManager.dataOf(uuid);
        data.lastQuitAt(System.currentTimeMillis());
        playerVipManager.saveData(uuid);
        playerVipManager.unload(uuid);
        kitManager.unload(uuid);
    }
}
