package com.alkacode.vips.command;

import com.alkacode.vips.VipsServices;
import com.alkacode.vips.model.VipPerks;
import com.alkacode.vips.util.TextUtil;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PerkCommands implements CommandExecutor {

    private static final long FEED_COOLDOWN_MILLIS = 60_000L;

    private final VipsServices services;
    private final Map<UUID, Long> feedCooldown = new ConcurrentHashMap<>();

    public PerkCommands(VipsServices services) {
        this.services = services;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(TextUtil.parse(services.configManager.prefix() + services.configManager.message("general.player-only")));
            return true;
        }

        VipPerks perks = services.perksManager.perksOf(player.getUniqueId());
        switch (command.getName().toLowerCase()) {
            case "vipfly" -> fly(player, perks);
            case "vipback" -> back(player, perks);
            case "vipfeed" -> feed(player, perks);
            case "vipreparar" -> reparar(player, perks);
            case "vipbigorna" -> bigorna(player, perks);
            case "vipbancada" -> bancada(player, perks);
            default -> {
                return false;
            }
        }
        return true;
    }

    private void fly(Player player, VipPerks perks) {
        if (!perks.fly()) {
            services.sendMessage(player, "perks.no-access", Map.of());
            return;
        }
        boolean enable = !services.perksManager.isFlying(player.getUniqueId());
        if (enable) {
            services.perksManager.setFlying(player.getUniqueId(), true);
            player.setAllowFlight(true);
            player.setFlying(true);
            services.sendMessage(player, "perks.fly-on", Map.of());
        } else {
            services.perksManager.disableFlight(player);
            services.sendMessage(player, "perks.fly-off", Map.of());
        }
    }

    private void back(Player player, VipPerks perks) {
        if (!perks.back()) {
            services.sendMessage(player, "perks.no-access", Map.of());
            return;
        }
        Location location = services.perksManager.lastDeathLocation(player.getUniqueId());
        if (location == null) {
            services.sendMessage(player, "perks.no-death-location", Map.of());
            return;
        }
        player.teleportAsync(location);
        services.sendMessage(player, "perks.back-success", Map.of());
    }

    private void feed(Player player, VipPerks perks) {
        if (!perks.feed()) {
            services.sendMessage(player, "perks.no-access", Map.of());
            return;
        }
        long now = System.currentTimeMillis();
        Long last = feedCooldown.get(player.getUniqueId());
        if (last != null && now - last < FEED_COOLDOWN_MILLIS) {
            services.sendMessage(player, "perks.feed-cooldown", Map.of());
            return;
        }
        feedCooldown.put(player.getUniqueId(), now);
        player.setFoodLevel(20);
        player.setSaturation(20f);
        services.sendMessage(player, "perks.fed", Map.of());
    }

    private void reparar(Player player, VipPerks perks) {
        if (!perks.reparar()) {
            services.sendMessage(player, "perks.no-access", Map.of());
            return;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir() || !(item.getItemMeta() instanceof Damageable damageable)) {
            services.sendMessage(player, "perks.no-item-to-repair", Map.of());
            return;
        }
        damageable.setDamage(0);
        item.setItemMeta((org.bukkit.inventory.meta.ItemMeta) damageable);
        services.sendMessage(player, "perks.repaired", Map.of());
    }

    /** openAnvil/openWorkbench(Location, boolean) are deprecated but have no location-less
     * replacement in this API for a portatil (no real block backing) anvil/bancada. */
    @SuppressWarnings("deprecation")
    private void bigorna(Player player, VipPerks perks) {
        if (!perks.bigorna()) {
            services.sendMessage(player, "perks.no-access", Map.of());
            return;
        }
        player.openAnvil(null, true);
    }

    @SuppressWarnings("deprecation")
    private void bancada(Player player, VipPerks perks) {
        if (!perks.bancada()) {
            services.sendMessage(player, "perks.no-access", Map.of());
            return;
        }
        player.openWorkbench(null, true);
    }
}
