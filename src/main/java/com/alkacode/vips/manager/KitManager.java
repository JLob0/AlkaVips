package com.alkacode.vips.manager;

import com.alkacode.vips.hook.HookManager;
import com.alkacode.vips.model.VipKit;
import com.alkacode.vips.storage.VipsRepository;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Kits diarios/periodicos por tipo de VIP (kits.yml) - cooldown persistido via
 * {@link VipsRepository#saveKitCooldown}, cache em memoria carregado no join igual ao
 * padrao do {@link PlayerVipManager}.
 */
public final class KitManager {

    private final JavaPlugin plugin;
    private final VipsRepository database;
    private final HookManager hooks;
    private final Map<String, Map<String, VipKit>> kitsByVipType = new LinkedHashMap<>();
    private final Map<UUID, Map<String, Long>> cooldownCache = new ConcurrentHashMap<>();

    public KitManager(JavaPlugin plugin, VipsRepository database, HookManager hooks) {
        this.plugin = plugin;
        this.database = database;
        this.hooks = hooks;
    }

    public void load() {
        kitsByVipType.clear();
        File file = new File(plugin.getDataFolder(), "kits.yml");
        if (!file.exists()) {
            plugin.saveResource("kits.yml", false);
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("kits");
        if (root == null) {
            return;
        }
        for (String vipTypeId : root.getKeys(false)) {
            ConfigurationSection vipSection = root.getConfigurationSection(vipTypeId);
            if (vipSection == null) {
                continue;
            }
            Map<String, VipKit> kits = new LinkedHashMap<>();
            for (String kitId : vipSection.getKeys(false)) {
                ConfigurationSection kitSection = vipSection.getConfigurationSection(kitId);
                if (kitSection == null) {
                    continue;
                }
                try {
                    kits.put(kitId, parse(vipTypeId, kitId, kitSection));
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Falha ao carregar o kit '" + kitId + "' de " + vipTypeId, e);
                }
            }
            kitsByVipType.put(vipTypeId.toLowerCase(), kits);
        }
    }

    public void reload() {
        load();
    }

    @SuppressWarnings("unchecked")
    private VipKit parse(String vipTypeId, String kitId, ConfigurationSection section) {
        String name = section.getString("name", kitId);
        long cooldownMillis = section.getLong("cooldown-seconds", 86_400L) * 1000L;
        List<ItemStack> rawItems = (List<ItemStack>) section.getList("items", List.of());
        List<ItemStack> items = rawItems.stream()
                .filter(i -> i != null && !i.getType().isAir())
                .collect(java.util.stream.Collectors.toList());
        if (items.isEmpty()) {
            plugin.getLogger().warning("Kit '" + kitId + "' do VIP '" + vipTypeId + "' carregado sem itens.");
        }

        ConfigurationSection iconSection = section.getConfigurationSection("icon");
        Material iconMaterial = Material.CHEST;
        String iconItemsAdder = "";
        if (iconSection != null) {
            Material parsed = Material.matchMaterial(iconSection.getString("material", "CHEST"));
            iconMaterial = parsed != null ? parsed : Material.CHEST;
            iconItemsAdder = iconSection.getString("itemsadder", "");
        }

        List<String> itemsAdderItemIds = section.getStringList("itemsadder-items");

        return new VipKit(vipTypeId.toLowerCase(), kitId, name, cooldownMillis, items, iconMaterial, iconItemsAdder,
                itemsAdderItemIds);
    }

    public Map<String, VipKit> getKits(String vipTypeId) {
        return kitsByVipType.getOrDefault(vipTypeId.toLowerCase(), Map.of());
    }

    public CompletableFuture<Void> loadForJoin(UUID uuid) {
        return database.loadKitCooldowns(uuid).thenAccept(map -> cooldownCache.put(uuid, new ConcurrentHashMap<>(map)));
    }

    public void unload(UUID uuid) {
        cooldownCache.remove(uuid);
    }

    private Map<String, Long> cooldownsOf(UUID uuid) {
        return cooldownCache.computeIfAbsent(uuid, database::loadKitCooldownsSync);
    }

    /** Millis restantes de cooldown do kit para o jogador - 0 se ja disponivel. */
    public long remainingCooldown(UUID uuid, VipKit kit) {
        Long claimedAt = cooldownsOf(uuid).get(kit.key());
        if (claimedAt == null) {
            return 0;
        }
        long elapsed = System.currentTimeMillis() - claimedAt;
        return Math.max(0, kit.cooldownMillis() - elapsed);
    }

    /** Entrega os itens do kit e registra o cooldown - assume que a chamada ja verificou {@link #remainingCooldown}. */
    public void claim(Player player, VipKit kit) {
        long now = System.currentTimeMillis();
        cooldownsOf(player.getUniqueId()).put(kit.key(), now);
        database.saveKitCooldown(player.getUniqueId(), kit.key(), now);
        deliver(player, kit);
    }

    /**
     * Entrega os itens do kit SEM registrar cooldown - usado na primeira ativacao de
     * um tier (bonus de boas-vindas), pra nao "gastar" o resgate periodico normal do
     * jogador. Ele ainda pode resgatar esse mesmo kit pelo /vip > Kits depois, no
     * ciclo normal de cooldown.
     */
    public void claimIgnoreCooldown(Player player, VipKit kit) {
        deliver(player, kit);
    }

    private void deliver(Player player, VipKit kit) {
        for (ItemStack item : kit.items()) {
            give(player, item);
        }
        if (!kit.itemsAdderItemIds().isEmpty() && hooks.itemsAdder().isAvailable()) {
            for (String namespacedId : kit.itemsAdderItemIds()) {
                give(player, hooks.itemsAdder().getItemStack(namespacedId));
            }
        }
    }

    private void give(Player player, ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return;
        }
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(item.clone());
        for (ItemStack leftover : overflow.values()) {
            if (leftover == null || leftover.getType().isAir()) continue;
            Item dropped = player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            dropped.setOwner(player.getUniqueId());
        }
    }
}
