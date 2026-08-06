package com.alkacode.vips.task;

import com.alkacode.vips.service.ExpirationService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public final class VipExpirationTask extends BukkitRunnable {

    private final ExpirationService expirationService;

    public VipExpirationTask(ExpirationService expirationService) {
        this.expirationService = expirationService;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            expirationService.checkPlayer(player.getUniqueId());
        }
    }
}
