package com.alkacode.vips.manager;

import com.alkacode.vips.model.PlayerVip;
import com.alkacode.vips.model.VipPerks;
import com.alkacode.vips.model.VipType;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolve os {@link VipPerks} do VIP selecionado de um jogador e guarda o estado de sessao
 * (fly ligado, ultimo local de morte) que os perks mecanicos precisam - nada disso e
 * persistido no banco, e por sessao mesmo (relogou, perdeu fly/ /back, tem que reativar).
 */
public final class PerksManager {

    private final PlayerVipManager playerVipManager;
    private final VipTypeManager vipTypeManager;
    private final Map<UUID, Location> lastDeathLocation = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> flying = new ConcurrentHashMap<>();

    public PerksManager(PlayerVipManager playerVipManager, VipTypeManager vipTypeManager) {
        this.playerVipManager = playerVipManager;
        this.vipTypeManager = vipTypeManager;
    }

    public VipPerks perksOf(UUID uuid) {
        Optional<PlayerVip> selected = playerVipManager.getSelectedVip(uuid);
        if (selected.isEmpty()) {
            return VipPerks.NONE;
        }
        VipType type = vipTypeManager.get(selected.get().vipTypeId());
        return type != null ? type.perks() : VipPerks.NONE;
    }

    public void recordDeath(UUID uuid, Location location) {
        lastDeathLocation.put(uuid, location);
    }

    public Location lastDeathLocation(UUID uuid) {
        return lastDeathLocation.get(uuid);
    }

    public boolean isFlying(UUID uuid) {
        return flying.getOrDefault(uuid, false);
    }

    public void setFlying(UUID uuid, boolean value) {
        flying.put(uuid, value);
    }

    public void clear(UUID uuid) {
        flying.remove(uuid);
        lastDeathLocation.remove(uuid);
    }

    public void disableFlight(Player player) {
        flying.remove(player.getUniqueId());
        if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
            player.setAllowFlight(false);
            player.setFlying(false);
        }
    }
}
