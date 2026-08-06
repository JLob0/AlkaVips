package com.alkacode.vips.command;

import com.alkacode.vips.VipsServices;
import com.alkacode.vips.util.TabCompleteUtil;
import com.alkacode.vips.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class CreditCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("add", "remove", "set");

    private final VipsServices services;

    public CreditCommand(VipsServices services) {
        this.services = services;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(TextUtil.parse(services.configManager.prefix() + services.configManager.message("general.player-only")));
                return true;
            }
            double credits = services.creditManager.getCredits(player.getUniqueId());
            services.sendMessage(player, "credit.balance", Map.of("amount", String.valueOf(credits)));
            return true;
        }

        if (!sender.hasPermission("alkavips.admin.credit")) {
            sender.sendMessage(TextUtil.parse(services.configManager.prefix() + services.configManager.message("general.no-permission")));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(TextUtil.parse(services.configManager.prefix() + "<red>Uso: /creditovip <add|remove|set> <jogador> <quantia>"));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        double amount = parseDouble(args[2]);
        if (amount < 0) {
            sender.sendMessage(TextUtil.parse(services.configManager.prefix() + services.configManager.message("general.invalid-amount")
                    .replace("<value>", args[2])));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "add" -> {
                services.creditManager.add(target.getUniqueId(), amount);
                sender.sendMessage(TextUtil.parse(services.configManager.prefix() + services.configManager.message("credit.added"),
                        Map.of("amount", String.valueOf(amount), "name", nameOf(target))));
            }
            case "remove" -> {
                services.creditManager.remove(target.getUniqueId(), amount);
                sender.sendMessage(TextUtil.parse(services.configManager.prefix() + services.configManager.message("credit.removed"),
                        Map.of("amount", String.valueOf(amount), "name", nameOf(target))));
            }
            case "set" -> {
                services.creditManager.set(target.getUniqueId(), amount);
                sender.sendMessage(TextUtil.parse(services.configManager.prefix() + services.configManager.message("credit.set"),
                        Map.of("amount", String.valueOf(amount), "name", nameOf(target))));
            }
            default -> sender.sendMessage(TextUtil.parse(services.configManager.prefix() + "<red>Uso: /creditovip <add|remove|set> <jogador> <quantia>"));
        }
        return true;
    }

    private String nameOf(OfflinePlayer player) {
        return player.getName() != null ? player.getName() : player.getUniqueId().toString();
    }

    private double parseDouble(String raw) {
        try {
            return Double.parseDouble(raw.replace(",", "."));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("alkavips.admin.credit")) {
            return Collections.emptyList();
        }
        int index = args.length - 1;
        String current = args[index];
        return switch (index) {
            case 0 -> TabCompleteUtil.filter(SUBCOMMANDS, current);
            case 1 -> TabCompleteUtil.onlinePlayerNames(current);
            default -> Collections.emptyList();
        };
    }
}
