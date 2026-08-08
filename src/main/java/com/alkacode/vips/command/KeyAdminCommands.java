package com.alkacode.vips.command;

import com.alkacode.vips.VipsServices;
import com.alkacode.vips.model.VipKey;
import com.alkacode.vips.model.VipType;
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

public final class KeyAdminCommands implements CommandExecutor, TabCompleter {

    private static final List<String> DURATION_SUGGESTIONS = List.of("1h", "1d", "7d", "30d", "0");
    private static final List<String> AMOUNT_SUGGESTIONS = List.of("1", "5", "10", "50");
    private static final List<String> EDIT_FIELDS = List.of("vip", "duracao");

    private final VipsServices services;

    public KeyAdminCommands(VipsServices services) {
        this.services = services;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return switch (command.getName().toLowerCase()) {
            case "gerarkey" -> generate(sender, args);
            case "criarkey" -> create(sender, args);
            case "removerkey" -> remove(sender, args);
            case "editarkey" -> edit(sender, args);
            case "verkeys" -> see(sender, args);
            case "darkey" -> giveKey(sender, args);
            default -> false;
        };
    }

    private boolean generate(CommandSender sender, String[] args) {
        if (args.length < 2) {
            usage(sender, "/gerarkey <vip> <duracao> [quantidade]");
            return true;
        }
        VipType vipType = services.vipTypeManager.get(args[0]);
        if (vipType == null) {
            error(sender, "general.unknown-vip", Map.of("value", args[0]));
            return true;
        }
        long duration = TimeUtil.parseDuration(args[1]);
        if (duration < 0) {
            error(sender, "general.invalid-time", Map.of("value", args[1]));
            return true;
        }
        int amount = args.length > 2 ? parseInt(args[2]) : 1;
        if (amount <= 0) {
            error(sender, "general.invalid-amount", Map.of("value", args.length > 2 ? args[2] : "1"));
            return true;
        }
        for (int i = 0; i < amount; i++) {
            services.keyManager.generate(vipType, duration, false).join();
        }
        sender.sendMessage(TextUtil.legacyParse(services.configManager.prefix() + services.configManager.message("key.generated"),
                Map.of("amount", String.valueOf(amount), "vip", TextUtil.plain(vipType.display()),
                        "duration", duration == 0 ? "Permanente" : TimeUtil.formatRemaining(duration))));
        return true;
    }

    private boolean create(CommandSender sender, String[] args) {
        if (args.length < 3) {
            usage(sender, "/criarkey <vip> <duracao> <codigo>");
            return true;
        }
        VipType vipType = services.vipTypeManager.get(args[0]);
        if (vipType == null) {
            error(sender, "general.unknown-vip", Map.of("value", args[0]));
            return true;
        }
        long duration = TimeUtil.parseDuration(args[1]);
        if (duration < 0) {
            error(sender, "general.invalid-time", Map.of("value", args[1]));
            return true;
        }
        String code = args[2].toUpperCase();
        VipKey created = services.keyManager.create(vipType, duration, code, false).join();
        if (created == null) {
            error(sender, "key.code-exists", Map.of("code", code));
            return true;
        }
        sender.sendMessage(TextUtil.legacyParse(services.configManager.prefix() + services.configManager.message("key.created"),
                Map.of("code", code)));
        return true;
    }

    private boolean remove(CommandSender sender, String[] args) {
        if (args.length < 1) {
            usage(sender, "/removerkey <codigo>");
            return true;
        }
        String code = args[0].toUpperCase();
        VipKey key = services.keyManager.findSync(code);
        if (key == null) {
            error(sender, "key.not-found", Map.of("code", code));
            return true;
        }
        services.keyManager.delete(code);
        sender.sendMessage(TextUtil.legacyParse(services.configManager.prefix() + services.configManager.message("key.removed"),
                Map.of("code", code)));
        return true;
    }

    private boolean edit(CommandSender sender, String[] args) {
        if (args.length < 3) {
            usage(sender, "/editarkey <codigo> <vip|duracao> <valor>");
            return true;
        }
        String code = args[0].toUpperCase();
        VipKey key = services.keyManager.findSync(code);
        if (key == null) {
            error(sender, "key.not-found", Map.of("code", code));
            return true;
        }
        switch (args[1].toLowerCase()) {
            case "vip" -> {
                if (!services.vipTypeManager.exists(args[2])) {
                    error(sender, "general.unknown-vip", Map.of("value", args[2]));
                    return true;
                }
                key = new VipKey(key.id(), args[2], key.duration(), key.used(), key.usedBy(), key.usedAt(),
                        key.bonus(), key.forSale(), key.sellerUuid(), key.economyProvider(), key.sellPrice());
                services.keyManager.save(key);
            }
            case "duracao" -> {
                long duration = TimeUtil.parseDuration(args[2]);
                if (duration < 0) {
                    error(sender, "general.invalid-time", Map.of("value", args[2]));
                    return true;
                }
                VipKey updated = new VipKey(key.id(), key.vipTypeId(), duration, key.used(), key.usedBy(), key.usedAt(),
                        key.bonus(), key.forSale(), key.sellerUuid(), key.economyProvider(), key.sellPrice());
                services.keyManager.save(updated);
            }
            default -> {
                usage(sender, "/editarkey <codigo> <vip|duracao> <valor>");
                return true;
            }
        }
        sender.sendMessage(TextUtil.legacyParse(services.configManager.prefix() + "<green>Key " + code + " atualizada."));
        return true;
    }

    private boolean see(CommandSender sender, String[] args) {
        String targetName = args.length > 0 ? args[0] : (sender instanceof Player p ? p.getName() : null);
        if (targetName == null) {
            usage(sender, "/verkeys [jogador]");
            return true;
        }
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            error(sender, "general.unknown-player", Map.of("name", targetName));
            return true;
        }
        int found = 0;
        for (var item : target.getInventory().getContents()) {
            String keyId = services.keyManager.readKeyId(item);
            if (keyId != null) {
                VipKey key = services.keyManager.findSync(keyId);
                if (key != null) {
                    sender.sendMessage(TextUtil.legacyParse("<gray>- <white>" + key.id() + " <gray>(" + key.vipTypeId() + ")"));
                    found++;
                }
            }
        }
        if (found == 0) {
            sender.sendMessage(TextUtil.legacyParse(services.configManager.prefix() + "<yellow>Nenhuma key encontrada no inventario de " + targetName + "."));
        }
        return true;
    }

    private boolean giveKey(CommandSender sender, String[] args) {
        if (args.length < 3) {
            usage(sender, "/darkey <jogador> <vip> <duracao>");
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            error(sender, "general.unknown-player", Map.of("name", args[0]));
            return true;
        }
        VipType vipType = services.vipTypeManager.get(args[1]);
        if (vipType == null) {
            error(sender, "general.unknown-vip", Map.of("value", args[1]));
            return true;
        }
        long duration = TimeUtil.parseDuration(args[2]);
        if (duration < 0) {
            error(sender, "general.invalid-time", Map.of("value", args[2]));
            return true;
        }
        VipKey key = services.keyManager.generate(vipType, duration, false).join();
        target.getInventory().addItem(services.keyManager.buildItem(key, vipType));
        sender.sendMessage(TextUtil.legacyParse(services.configManager.prefix() + services.configManager.message("key.given"),
                Map.of("code", key.id(), "vip", TextUtil.plain(vipType.display()), "name", target.getName())));
        target.sendMessage(TextUtil.legacyParse(services.configManager.prefix() + services.configManager.message("key.received"),
                Map.of("vip", TextUtil.plain(vipType.display()), "code", key.id())));
        return true;
    }

    private int parseInt(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void usage(CommandSender sender, String usage) {
        sender.sendMessage(TextUtil.legacyParse(services.configManager.prefix() + "<red>Uso: " + usage));
    }

    private void error(CommandSender sender, String path, Map<String, String> placeholders) {
        sender.sendMessage(TextUtil.legacyParse(services.configManager.prefix() + services.configManager.message(path), placeholders));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase();
        int index = args.length - 1;
        String current = args[index];

        return switch (name) {
            case "gerarkey" -> switch (index) {
                case 0 -> TabCompleteUtil.filter(services.vipTypeManager.all().keySet(), current);
                case 1 -> TabCompleteUtil.filter(DURATION_SUGGESTIONS, current);
                case 2 -> TabCompleteUtil.filter(AMOUNT_SUGGESTIONS, current);
                default -> Collections.emptyList();
            };
            case "criarkey" -> switch (index) {
                case 0 -> TabCompleteUtil.filter(services.vipTypeManager.all().keySet(), current);
                case 1 -> TabCompleteUtil.filter(DURATION_SUGGESTIONS, current);
                default -> Collections.emptyList();
            };
            case "editarkey" -> switch (index) {
                case 1 -> TabCompleteUtil.filter(EDIT_FIELDS, current);
                case 2 -> args[1].equalsIgnoreCase("vip")
                        ? TabCompleteUtil.filter(services.vipTypeManager.all().keySet(), current)
                        : (args[1].equalsIgnoreCase("duracao") ? TabCompleteUtil.filter(DURATION_SUGGESTIONS, current) : Collections.emptyList());
                default -> Collections.emptyList();
            };
            case "verkeys" -> index == 0 ? TabCompleteUtil.onlinePlayerNames(current) : Collections.emptyList();
            case "darkey" -> switch (index) {
                case 0 -> TabCompleteUtil.onlinePlayerNames(current);
                case 1 -> TabCompleteUtil.filter(services.vipTypeManager.all().keySet(), current);
                case 2 -> TabCompleteUtil.filter(DURATION_SUGGESTIONS, current);
                default -> Collections.emptyList();
            };
            default -> Collections.emptyList();
        };
    }
}
