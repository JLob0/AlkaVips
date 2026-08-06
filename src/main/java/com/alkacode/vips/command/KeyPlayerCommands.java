package com.alkacode.vips.command;

import com.alkacode.vips.VipsServices;
import com.alkacode.vips.gui.MarketplaceMenu;
import com.alkacode.vips.service.KeyUsageService;
import com.alkacode.vips.service.MarketplaceService;
import com.alkacode.vips.util.TabCompleteUtil;
import com.alkacode.vips.util.TextUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class KeyPlayerCommands implements CommandExecutor, TabCompleter {

    private final VipsServices services;

    public KeyPlayerCommands(VipsServices services) {
        this.services = services;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(TextUtil.parse(services.configManager.prefix() + services.configManager.message("general.player-only")));
            return true;
        }

        return switch (command.getName().toLowerCase()) {
            case "usarkey" -> useKey(player, args);
            case "vendervip" -> sellKey(player, args);
            case "cancelarvenda" -> cancelSale(player, args);
            case "comprarvip" -> buyKey(player, args);
            case "vendasvip" -> {
                new MarketplaceMenu(player, services).open();
                yield true;
            }
            default -> false;
        };
    }

    private boolean useKey(Player player, String[] args) {
        if (args.length < 1) {
            usage(player, "/usarkey <codigo>");
            return true;
        }
        KeyUsageService.Result result = services.keyUsageService.use(player, args[0].toUpperCase(), null);
        String path = switch (result) {
            case SUCCESS -> "key.used-success";
            case NOT_FOUND -> "key.not-found";
            case ALREADY_USED -> "key.already-used";
        };
        services.sendMessage(player, path, Map.of());
        return true;
    }

    /**
     * A key so vale como posse fisica do item (KeysMenu/KeyInteractListener nao tem
     * conceito de "dono" no banco para keys nao vendidas) - listForSale nao checa isso
     * sozinho, entao um jogador digitando o codigo de uma key que NAO esta com ele
     * conseguia listar ela pra venda sem nunca ter tido o item, e o comprador recebia
     * um item novo enquanto o dono original ainda ficava com o seu (duplicacao). Exigir
     * e consumir o item fisico aqui fecha o mesmo buraco que o fluxo via GUI
     * (SellKeyMenu, aberto a partir do proprio item) ja fechava.
     */
    private boolean sellKey(Player player, String[] args) {
        if (args.length < 3) {
            usage(player, "/vendervip <codigo> <moeda> <preco>");
            return true;
        }
        double price = parseDouble(args[2]);
        if (price <= 0) {
            services.sendMessage(player, "market.invalid-price", Map.of("value", args[2]));
            return true;
        }
        String code = args[0].toUpperCase();
        ItemStack physicalItem = findKeyItem(player, code);
        if (physicalItem == null) {
            services.sendMessage(player, "key.not-held", Map.of("code", code));
            return true;
        }
        MarketplaceService.ListResult result = services.marketplaceService.listForSale(player, code, args[1], price);
        if (result == MarketplaceService.ListResult.SUCCESS) {
            physicalItem.setAmount(physicalItem.getAmount() - 1);
        }
        String path = switch (result) {
            case SUCCESS -> "market.listed";
            case NOT_FOUND -> "key.not-found";
            case ALREADY_USED -> "key.already-used";
            case NOT_ALLOWED -> "market.not-for-sale";
            case ALREADY_FOR_SALE -> "market.already-for-sale";
            case INVALID_CURRENCY -> "market.invalid-currency";
            case INVALID_PRICE -> "market.invalid-price";
        };
        services.sendMessage(player, path, Map.of("code", args[0], "currency", args[1], "price", args[2], "value", args[2]));
        return true;
    }

    private ItemStack findKeyItem(Player player, String code) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && code.equals(services.keyManager.readKeyId(item))) {
                return item;
            }
        }
        return null;
    }

    private boolean cancelSale(Player player, String[] args) {
        if (args.length < 1) {
            usage(player, "/cancelarvenda <codigo>");
            return true;
        }
        MarketplaceService.CancelResult result = services.marketplaceService.cancelSale(player, args[0].toUpperCase());
        String path = switch (result) {
            case SUCCESS -> "market.cancelled";
            case NOT_FOUND -> "key.not-found";
            case NOT_FOR_SALE -> "market.not-for-sale";
            case NOT_OWNER -> "market.not-owner";
        };
        services.sendMessage(player, path, Map.of("code", args[0]));
        return true;
    }

    private boolean buyKey(Player player, String[] args) {
        if (args.length < 1) {
            usage(player, "/comprarvip <codigo>");
            return true;
        }
        MarketplaceService.BuyResult result = services.marketplaceService.buy(player, args[0].toUpperCase());
        String path = switch (result) {
            case SUCCESS -> "market.bought";
            case NOT_FOUND -> "key.not-found";
            case NOT_FOR_SALE -> "market.not-for-sale";
            case BUY_OWN -> "market.buy-own";
            case INSUFFICIENT_FUNDS -> "market.insufficient-funds";
        };
        services.sendMessage(player, path, Map.of("code", args[0]));
        return true;
    }

    private double parseDouble(String raw) {
        try {
            return Double.parseDouble(raw.replace(",", "."));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void usage(Player player, String usage) {
        services.sendMessage(player, "general.invalid-usage", Map.of("usage", usage));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            return Collections.emptyList();
        }
        String name = command.getName().toLowerCase();
        int index = args.length - 1;
        String current = args[index];

        return switch (name) {
            case "usarkey", "vendervip", "cancelarvenda" -> index == 0
                    ? TabCompleteUtil.filter(ownKeyCodes(player), current)
                    : (name.equals("vendervip") && index == 1 ? currencySuggestions(current) : Collections.emptyList());
            default -> Collections.emptyList();
        };
    }

    private List<String> ownKeyCodes(Player player) {
        List<String> codes = new ArrayList<>();
        for (ItemStack item : player.getInventory().getContents()) {
            String keyId = services.keyManager.readKeyId(item);
            if (keyId != null) {
                codes.add(keyId);
            }
        }
        return codes;
    }

    private List<String> currencySuggestions(String current) {
        return services.economyHook != null
                ? TabCompleteUtil.filter(services.economyHook.currencyIds(), current)
                : Collections.emptyList();
    }
}
