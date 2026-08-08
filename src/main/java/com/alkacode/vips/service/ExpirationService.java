package com.alkacode.vips.service;

import com.alkacode.vips.event.VipExpireEvent;
import com.alkacode.vips.manager.PlayerVipManager;
import com.alkacode.vips.manager.VipTypeManager;
import com.alkacode.vips.model.PlayerVip;
import com.alkacode.vips.model.VipType;
import com.alkacode.vips.model.enums.VipStatus;
import com.alkacode.vips.util.CommandUtil;
import com.alkacode.vips.util.TextUtil;
import com.alkacode.vips.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * VIPs congelados nao expiram enquanto {@code frozen} estiver ativo - o relogio de
 * parede continua passando, mas simplesmente ignoramos a checagem ate o
 * descongelamento, que entao estende {@code expiresAt} pelo tempo que ficou
 * congelado (ver secao 4 da especificacao).
 */
public final class ExpirationService {

    private final PlayerVipManager playerVipManager;
    private final VipTypeManager vipTypeManager;
    private final ConfigManager configManager;

    public ExpirationService(PlayerVipManager playerVipManager, VipTypeManager vipTypeManager, ConfigManager configManager) {
        this.playerVipManager = playerVipManager;
        this.vipTypeManager = vipTypeManager;
        this.configManager = configManager;
    }

    public void checkPlayer(UUID uuid) {
        List<PlayerVip> vips = playerVipManager.getActiveVips(uuid);
        for (PlayerVip vip : vips) {
            checkVip(uuid, vip);
        }
    }

    private void checkVip(UUID uuid, PlayerVip vip) {
        if (vip.frozen() || vip.isPermanent() || !vip.isExpired()) {
            return;
        }
        VipType vipType = vipTypeManager.get(vip.vipTypeId());
        vip.status(VipStatus.EXPIRED);
        playerVipManager.update(vip);

        if (vipType != null) {
            Map<String, String> placeholders = Map.of("player", nameOf(uuid));
            for (String command : vipType.groupRemoveCmds()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), CommandUtil.substitute(command, placeholders));
            }
        }

        Bukkit.getPluginManager().callEvent(new VipExpireEvent(uuid, vip.vipTypeId()));

        Player player = Bukkit.getPlayer(uuid);
        if (player != null && vipType != null) {
            String raw = configManager.prefix() + "<yellow>Seu VIP <white><vip></white> expirou.";
            player.sendMessage(TextUtil.legacyParse(raw, Map.of("vip", TextUtil.plain(vipType.display()))));
        }
    }

    private String nameOf(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        return player != null ? player.getName() : Bukkit.getOfflinePlayer(uuid).getName();
    }
}
