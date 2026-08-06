package com.alkacode.vips.hook;

import com.alkacode.vips.VipsServices;
import com.alkacode.vips.model.PlayerVip;
import com.alkacode.vips.model.VipType;
import com.alkacode.vips.util.TextUtil;
import com.alkacode.vips.util.TimeUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public final class PlaceholderAPIHook extends PlaceholderExpansion {

    private final VipsServices services;

    public PlaceholderAPIHook(VipsServices services) {
        this.services = services;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "alkavips";
    }

    @Override
    public @NotNull String getAuthor() {
        return "MestreDEV";
    }

    @Override
    public @NotNull String getVersion() {
        return services.plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }
        Optional<PlayerVip> selected = services.playerVipManager.getSelectedVip(player.getUniqueId());
        VipType type = selected.map(v -> services.vipTypeManager.get(v.vipTypeId())).orElse(null);

        return switch (params) {
            case "vip" -> type != null ? TextUtil.plain(type.display()) : "";
            case "vip_id" -> selected.map(PlayerVip::vipTypeId).orElse("");
            case "vip_prefix" -> type != null ? TextUtil.plain(type.prefix()) : "";
            case "vip_time_remaining" -> selected.map(v -> v.isPermanent() ? "Permanente" : TimeUtil.formatRemaining(v.remainingMillis())).orElse("");
            case "vip_time_remaining_short" -> selected.map(v -> v.isPermanent() ? "Permanente" : TimeUtil.formatRemainingShort(v.remainingMillis())).orElse("");
            case "vip_expires_at" -> selected.map(v -> v.isPermanent() ? "Nunca" : String.valueOf(v.expiresAt())).orElse("");
            case "vip_activated_at" -> selected.map(v -> String.valueOf(v.activatedAt())).orElse("");
            case "credits" -> String.valueOf(services.creditManager.getCredits(player.getUniqueId()));
            case "credits_formatted" -> services.economyHook != null ? services.economyHook.format(services.creditManager.getCredits(player.getUniqueId())) : String.valueOf(services.creditManager.getCredits(player.getUniqueId()));
            case "keys_amount" -> String.valueOf(countKeys(player));
            case "has_vip" -> String.valueOf(selected.isPresent());
            case "party_progress" -> String.valueOf(services.partyVipManager.getProgress());
            case "party_goal" -> String.valueOf(services.partyVipManager.getGoal());
            case "party_percentage" -> String.valueOf((int) services.partyVipManager.getPercentage());
            default -> null;
        };
    }

    private int countKeys(OfflinePlayer player) {
        if (!(player.getPlayer() instanceof org.bukkit.entity.Player online)) {
            return 0;
        }
        int count = 0;
        for (var item : online.getInventory().getContents()) {
            if (services.keyManager.readKeyId(item) != null) {
                count++;
            }
        }
        return count;
    }
}
