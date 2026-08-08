package com.alkacode.vips.command;

import com.alkacode.vips.VipsServices;
import com.alkacode.vips.model.PlayerVip;
import com.alkacode.vips.model.VipType;
import com.alkacode.vips.model.enums.VipStatus;
import com.alkacode.vips.util.TabCompleteUtil;
import com.alkacode.vips.util.TextUtil;
import com.alkacode.vips.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class VipAdminCommands implements CommandExecutor, TabCompleter {

    private static final List<String> DURATION_SUGGESTIONS = List.of("1h", "1d", "7d", "30d", "0");

    private final VipsServices services;

    public VipAdminCommands(VipsServices services) {
        this.services = services;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return switch (command.getName().toLowerCase()) {
            case "darvip" -> giveVip(sender, args, false);
            case "setvip" -> giveVip(sender, args, true);
            case "removervip" -> removeVip(sender, args);
            case "removertempovip" -> removeTime(sender, args);
            case "infovip" -> info(sender, args);
            case "bonusvip" -> bonus(sender, args);
            default -> false;
        };
    }

    private boolean giveVip(CommandSender sender, String[] args, boolean silent) {
        if (args.length < 3) {
            sender.sendMessage(TextUtil.legacyParse(services.configManager.prefix() + "<red>Uso: /" + (silent ? "setvip" : "darvip") + " <jogador> <vip> <duracao>"));
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sendError(sender, "general.unknown-player", Map.of("name", args[0]));
            return true;
        }
        VipType vipType = services.vipTypeManager.get(args[1]);
        if (vipType == null) {
            sendError(sender, "general.unknown-vip", Map.of("value", args[1]));
            return true;
        }
        long duration = TimeUtil.parseDuration(args[2]);
        if (duration < 0) {
            sendError(sender, "general.invalid-time", Map.of("value", args[2]));
            return true;
        }
        services.activationService.activate(target, vipType, duration, null, silent);
        String path = silent ? "vip.set" : "vip.given";
        String durationText = duration == 0 ? "Permanente" : TimeUtil.formatRemaining(duration);
        sender.sendMessage(TextUtil.legacyParse(services.configManager.prefix() + services.configManager.message(path),
                Map.of("vip", TextUtil.plain(vipType.display()), "name", target.getName(), "duration", durationText)));
        return true;
    }

    private boolean removeVip(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(TextUtil.legacyParse(services.configManager.prefix() + "<red>Uso: /removervip <jogador> <vip>"));
            return true;
        }
        var target = Bukkit.getOfflinePlayer(args[0]);
        VipType vipType = services.vipTypeManager.get(args[1]);
        if (vipType == null) {
            sendError(sender, "general.unknown-vip", Map.of("value", args[1]));
            return true;
        }
        List<PlayerVip> matches = services.playerVipManager.getActiveVipsOfType(target.getUniqueId(), vipType.id());
        if (matches.isEmpty()) {
            sender.sendMessage(TextUtil.legacyParse(services.configManager.prefix() + services.configManager.message("vip.no-vip-to-remove"),
                    Map.of("name", args[0], "vip", TextUtil.plain(vipType.display()))));
            return true;
        }
        for (PlayerVip vip : matches) {
            for (String cmd : vipType.groupRemoveCmds()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        com.alkacode.vips.util.CommandUtil.substitute(cmd, Map.of("player", args[0])));
            }
            services.playerVipManager.remove(vip);
        }
        sender.sendMessage(TextUtil.legacyParse(services.configManager.prefix() + services.configManager.message("vip.removed"),
                Map.of("vip", TextUtil.plain(vipType.display()), "name", args[0])));
        return true;
    }

    private boolean removeTime(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(TextUtil.legacyParse(services.configManager.prefix() + "<red>Uso: /removertempovip <jogador> <vip> <tempo>"));
            return true;
        }
        var target = Bukkit.getOfflinePlayer(args[0]);
        VipType vipType = services.vipTypeManager.get(args[1]);
        if (vipType == null) {
            sendError(sender, "general.unknown-vip", Map.of("value", args[1]));
            return true;
        }
        long time = TimeUtil.parseDuration(args[2]);
        if (time <= 0) {
            sendError(sender, "general.invalid-time", Map.of("value", args[2]));
            return true;
        }
        List<PlayerVip> matches = services.playerVipManager.getActiveVipsOfType(target.getUniqueId(), vipType.id());
        if (matches.isEmpty()) {
            sender.sendMessage(TextUtil.legacyParse(services.configManager.prefix() + services.configManager.message("vip.no-vip-to-remove"),
                    Map.of("name", args[0], "vip", TextUtil.plain(vipType.display()))));
            return true;
        }
        PlayerVip vip = matches.get(0);
        if (!vip.isPermanent()) {
            vip.expiresAt(Math.max(System.currentTimeMillis(), vip.expiresAt() - time));
            services.playerVipManager.update(vip);
        }
        sender.sendMessage(TextUtil.legacyParse(services.configManager.prefix() + services.configManager.message("vip.removed-time"),
                Map.of("duration", TimeUtil.formatRemaining(time), "name", args[0], "vip", TextUtil.plain(vipType.display()))));
        return true;
    }

    private boolean info(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(TextUtil.legacyParse(services.configManager.prefix() + "<red>Uso: /infovip <jogador>"));
            return true;
        }
        var target = Bukkit.getOfflinePlayer(args[0]);
        List<PlayerVip> vips = services.playerVipManager.getVips(target.getUniqueId());
        sender.sendMessage(TextUtil.legacyParse(services.configManager.message("info.header"), Map.of("name", args[0])));
        if (vips.isEmpty()) {
            sender.sendMessage(TextUtil.legacyParse(services.configManager.message("info.no-vips"), Map.of("name", args[0])));
            return true;
        }
        for (PlayerVip vip : vips) {
            VipType type = services.vipTypeManager.get(vip.vipTypeId());
            String display = type != null ? TextUtil.plain(type.display()) : vip.vipTypeId();
            String time = vip.status() == VipStatus.ACTIVE
                    ? (vip.isPermanent() ? "Permanente" : TimeUtil.formatRemaining(vip.remainingMillis())) : "-";
            sender.sendMessage(TextUtil.legacyParse(services.configManager.message("info.line"),
                    Map.of("vip", display, "status", vip.status().name(), "time", time)));
        }
        return true;
    }

    private boolean bonus(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(TextUtil.legacyParse(services.configManager.prefix() + "<red>Uso: /bonusvip <vip> <jogador> [quantidade]"));
            return true;
        }
        VipType vipType = services.vipTypeManager.get(args[0]);
        if (vipType == null) {
            sendError(sender, "general.unknown-vip", Map.of("value", args[0]));
            return true;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sendError(sender, "general.unknown-player", Map.of("name", args[1]));
            return true;
        }
        int amount = args.length > 2 ? parseInt(args[2]) : 1;
        if (amount <= 0) {
            sendError(sender, "general.invalid-amount", Map.of("value", args.length > 2 ? args[2] : "1"));
            return true;
        }
        for (int i = 0; i < amount; i++) {
            var key = services.keyManager.generate(vipType, 0, true).join();
            target.getInventory().addItem(services.keyManager.buildItem(key, vipType));
        }
        sender.sendMessage(TextUtil.legacyParse(services.configManager.prefix() + services.configManager.message("key.bonus-given"),
                Map.of("amount", String.valueOf(amount), "vip", TextUtil.plain(vipType.display()), "name", target.getName())));
        return true;
    }

    private int parseInt(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void sendError(CommandSender sender, String path, Map<String, String> placeholders) {
        sender.sendMessage(TextUtil.legacyParse(services.configManager.prefix() + services.configManager.message(path), placeholders));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase();
        int index = args.length - 1;
        String current = args[index];

        return switch (name) {
            case "darvip", "setvip" -> switch (index) {
                case 0 -> TabCompleteUtil.onlinePlayerNames(current);
                case 1 -> TabCompleteUtil.filter(services.vipTypeManager.all().keySet(), current);
                case 2 -> TabCompleteUtil.filter(DURATION_SUGGESTIONS, current);
                default -> Collections.emptyList();
            };
            case "removervip" -> switch (index) {
                case 0 -> TabCompleteUtil.onlinePlayerNames(current);
                case 1 -> TabCompleteUtil.filter(services.vipTypeManager.all().keySet(), current);
                default -> Collections.emptyList();
            };
            case "removertempovip" -> switch (index) {
                case 0 -> TabCompleteUtil.onlinePlayerNames(current);
                case 1 -> TabCompleteUtil.filter(services.vipTypeManager.all().keySet(), current);
                case 2 -> TabCompleteUtil.filter(DURATION_SUGGESTIONS, current);
                default -> Collections.emptyList();
            };
            case "infovip" -> index == 0 ? TabCompleteUtil.onlinePlayerNames(current) : Collections.emptyList();
            case "bonusvip" -> switch (index) {
                case 0 -> TabCompleteUtil.filter(services.vipTypeManager.all().keySet(), current);
                case 1 -> TabCompleteUtil.onlinePlayerNames(current);
                default -> Collections.emptyList();
            };
            default -> Collections.emptyList();
        };
    }
}
