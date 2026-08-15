package com.alkacode.vips.command;

import com.alkacode.vips.VipsServices;
import com.alkacode.vips.util.TextUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class AlkaVipsCommand implements CommandExecutor, TabCompleter {

    private final VipsServices services;

    public AlkaVipsCommand(VipsServices services) {
        this.services = services;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            services.configManager.reload();
            services.vipTypeManager.reload();
            services.permissionNames.reload();
            services.sendMessage(sender, "general.reloaded", Map.of());
            return true;
        }
        sender.sendMessage(TextUtil.parse(services.configManager.prefix() + "<red>Uso: /alkavips reload"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return "reload".startsWith(args[0].toLowerCase()) ? List.of("reload") : Collections.emptyList();
        }
        return Collections.emptyList();
    }
}
