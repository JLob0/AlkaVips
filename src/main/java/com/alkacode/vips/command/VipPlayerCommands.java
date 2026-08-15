package com.alkacode.vips.command;

import com.alkacode.vips.VipsServices;
import com.alkacode.vips.gui.MainVipMenu;
import com.alkacode.vips.gui.P2PMarketMenu;
import com.alkacode.vips.gui.PartyVipMenu;
import com.alkacode.vips.gui.WalletMenu;
import com.alkacode.vips.manager.AffiliateManager;
import com.alkacode.vips.model.PlayerVip;
import com.alkacode.vips.model.VipType;
import com.alkacode.vips.service.TransferService;
import com.alkacode.vips.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public final class VipPlayerCommands implements CommandExecutor, TabCompleter {

    private final VipsServices services;

    public VipPlayerCommands(VipsServices services) {
        this.services = services;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            services.sendMessage(sender, "general.player-only", Map.of());
            return true;
        }

        switch (command.getName().toLowerCase()) {
            case "vip" -> new MainVipMenu(player, services).open();
            case "vips" -> listVips(player);
            case "tempovip" -> timeRemaining(player);
            case "trocarvip" -> swap(player);
            case "congelarvip" -> freeze(player);
            case "partyvip" -> new PartyVipMenu(player, services).open();
            case "transferirvip" -> transfer(player, args);
            case "carteiravip" -> new WalletMenu(player, services).open();
            case "mercadovip" -> new P2PMarketMenu(player, services).open();
            case "indicarvip" -> refer(player, args);
            case "perksvip" -> {
                if (services.perkTreeManager.enabled()) {
                    new com.alkacode.vips.gui.PerkTreeMenu(player, services).open();
                } else {
                    services.sendMessage(player, "perk-tree.disabled", Map.of());
                }
            }
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
            String display = type != null ? type.display() : vip.vipTypeId();
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
        String display = type != null ? type.display() : vip.vipTypeId();
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
        services.sendMessage(player, "vip.swapped", Map.of("vip", type != null ? type.display() : ""));
    }

    private void freeze(Player player) {
        Optional<PlayerVip> selected = services.playerVipManager.getSelectedVip(player.getUniqueId());
        if (selected.isEmpty()) {
            services.sendMessage(player, "vip.no-vip-to-freeze", Map.of());
            return;
        }
        PlayerVip vip = selected.get();
        VipType type = services.vipTypeManager.get(vip.vipTypeId());
        if (vip.frozen()) {
            long elapsed = System.currentTimeMillis() - vip.frozenAt();
            vip.frozen(false);
            if (!vip.isPermanent()) {
                vip.expiresAt(vip.expiresAt() + elapsed);
            }
            services.playerVipManager.update(vip);
            services.sendMessage(player, "vip.unfrozen", Map.of());
            if (type != null) {
                com.alkacode.vips.util.EventActionExecutor.run(type.eventsOnEnable(), player,
                        Map.of("player", player.getName(), "vip", type.display()));
            }
        } else {
            vip.frozen(true);
            vip.frozenAt(System.currentTimeMillis());
            services.playerVipManager.update(vip);
            services.sendMessage(player, "vip.frozen", Map.of());
            if (type != null) {
                com.alkacode.vips.util.EventActionExecutor.run(type.eventsOnDisable(), player,
                        Map.of("player", player.getName(), "vip", type.display()));
            }
        }
    }

    private void transfer(Player player, String[] args) {
        if (args.length < 1) {
            services.sendMessage(player, "general.invalid-usage", Map.of("usage", "/transferirvip <jogador>"));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            services.sendMessage(player, "general.unknown-player", Map.of("name", args[0]));
            return;
        }
        Optional<PlayerVip> selected = services.playerVipManager.getSelectedVip(player.getUniqueId());
        if (selected.isEmpty()) {
            services.sendMessage(player, "vip.no-active", Map.of());
            return;
        }
        TransferService.Result result = services.transferService.transfer(player.getUniqueId(), target.getUniqueId(), selected.get());
        switch (result) {
            case SUCCESS -> services.sendMessage(player, "transfer.success", Map.of("player", args[0]));
            case TARGET_IS_SELF -> services.sendMessage(player, "transfer.self", Map.of());
            case NO_VIP_SELECTED -> services.sendMessage(player, "vip.no-active", Map.of());
        }
    }

    private void refer(Player player, String[] args) {
        if (args.length < 1) {
            services.sendMessage(player, "general.invalid-usage", Map.of("usage", "/indicarvip <jogador>"));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            services.sendMessage(player, "general.unknown-player", Map.of("name", args[0]));
            return;
        }
        AffiliateManager.ReferResult result = services.affiliateManager.refer(player.getUniqueId(), target.getUniqueId());
        switch (result) {
            case SUCCESS -> services.sendMessage(player, "affiliate.referred", Map.of("player", args[0]));
            case DISABLED -> services.sendMessage(player, "affiliate.disabled", Map.of());
            case SELF -> services.sendMessage(player, "affiliate.self", Map.of());
            case ALREADY_REFERRED -> services.sendMessage(player, "affiliate.already-referred", Map.of());
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String name = command.getName().toLowerCase();
        if (args.length == 1 && (name.equals("transferirvip") || name.equals("indicarvip"))) {
            String lower = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(lower)).collect(Collectors.toList());
        }
        return List.of();
    }
}
