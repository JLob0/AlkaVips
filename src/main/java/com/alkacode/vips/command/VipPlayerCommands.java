package com.alkacode.vips.command;

import com.alkacode.vips.VipsServices;
import com.alkacode.vips.gui.MainVipMenu;
import com.alkacode.vips.gui.PartyVipMenu;
import com.alkacode.vips.model.PlayerVip;
import com.alkacode.vips.model.VipType;
import com.alkacode.vips.util.TextUtil;
import com.alkacode.vips.util.TimeUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class VipPlayerCommands implements CommandExecutor {

    private final VipsServices services;

    public VipPlayerCommands(VipsServices services) {
        this.services = services;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(TextUtil.legacyParse(services.configManager.prefix() + services.configManager.message("general.player-only")));
            return true;
        }

        switch (command.getName().toLowerCase()) {
            case "vip" -> new MainVipMenu(player, services).open();
            case "vips" -> listVips(player);
            case "tempovip" -> timeRemaining(player);
            case "trocarvip" -> swap(player);
            case "congelarvip" -> freeze(player);
            case "partyvip" -> new PartyVipMenu(player, services).open();
            default -> {
                return false;
            }
        }
        return true;
    }

    private void listVips(Player player) {
        List<PlayerVip> vips = services.playerVipManager.getActiveVips(player.getUniqueId());
        if (vips.isEmpty()) {
            services.sendMessage(player, "vip.no-active", Map.of());
            return;
        }
        for (PlayerVip vip : vips) {
            VipType type = services.vipTypeManager.get(vip.vipTypeId());
            String display = type != null ? TextUtil.plain(type.display()) : vip.vipTypeId();
            String remaining = vip.isPermanent() ? "Permanente" : TimeUtil.formatRemaining(vip.remainingMillis());
            services.sendMessage(player, "info.line", Map.of(
                    "vip", display, "status", vip.status().name(), "time", remaining));
        }
    }

    private void timeRemaining(Player player) {
        Optional<PlayerVip> selected = services.playerVipManager.getSelectedVip(player.getUniqueId());
        if (selected.isEmpty()) {
            services.sendMessage(player, "vip.no-active", Map.of());
            return;
        }
        PlayerVip vip = selected.get();
        VipType type = services.vipTypeManager.get(vip.vipTypeId());
        String display = type != null ? TextUtil.plain(type.display()) : vip.vipTypeId();
        String remaining = vip.isPermanent() ? "Permanente" : TimeUtil.formatRemaining(vip.remainingMillis());
        services.sendMessage(player, "vip.time-remaining", Map.of("vip", display, "time", remaining));
    }

    private void swap(Player player) {
        Optional<PlayerVip> next = services.playerVipManager.cycleSelectedVip(player.getUniqueId());
        if (next.isEmpty() || services.playerVipManager.getActiveVips(player.getUniqueId()).size() < 2) {
            services.sendMessage(player, "vip.no-vips-to-swap", Map.of());
            return;
        }
        VipType type = services.vipTypeManager.get(next.get().vipTypeId());
        services.sendMessage(player, "vip.swapped", Map.of("vip", type != null ? TextUtil.plain(type.display()) : ""));
    }

    private void freeze(Player player) {
        Optional<PlayerVip> selected = services.playerVipManager.getSelectedVip(player.getUniqueId());
        if (selected.isEmpty()) {
            services.sendMessage(player, "vip.no-vip-to-freeze", Map.of());
            return;
        }
        PlayerVip vip = selected.get();
        if (vip.frozen()) {
            long elapsed = System.currentTimeMillis() - vip.frozenAt();
            vip.frozen(false);
            if (!vip.isPermanent()) {
                vip.expiresAt(vip.expiresAt() + elapsed);
            }
            services.playerVipManager.update(vip);
            services.sendMessage(player, "vip.unfrozen", Map.of());
        } else {
            vip.frozen(true);
            vip.frozenAt(System.currentTimeMillis());
            services.playerVipManager.update(vip);
            services.sendMessage(player, "vip.frozen", Map.of());
        }
    }
}
