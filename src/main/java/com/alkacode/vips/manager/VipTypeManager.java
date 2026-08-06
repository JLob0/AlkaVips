package com.alkacode.vips.manager;

import com.alkacode.vips.hook.HookManager;
import com.alkacode.vips.model.IconTemplate;
import com.alkacode.vips.model.VipItem;
import com.alkacode.vips.model.VipPerks;
import com.alkacode.vips.model.VipType;
import com.alkacode.vips.model.enums.TimeType;
import com.alkacode.vips.util.ItemBuilder;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public final class VipTypeManager {

    private final JavaPlugin plugin;
    private final HookManager hooks;
    private final Map<String, VipType> vipTypes = new LinkedHashMap<>();

    public VipTypeManager(JavaPlugin plugin, HookManager hooks) {
        this.plugin = plugin;
        this.hooks = hooks;
    }

    public void load() {
        vipTypes.clear();
        File file = new File(plugin.getDataFolder(), "vips.yml");
        if (!file.exists()) {
            plugin.saveResource("vips.yml", false);
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("vips");
        if (root == null) {
            plugin.getLogger().warning("vips.yml nao possui a secao 'vips' - nenhum VIP foi carregado.");
            return;
        }
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) {
                continue;
            }
            try {
                vipTypes.put(id, parse(id, section));
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Falha ao carregar o VIP '" + id + "' de vips.yml", e);
            }
        }
        plugin.getLogger().info("Carregados " + vipTypes.size() + " tipo(s) de VIP.");
    }

    public void reload() {
        load();
    }

    private VipType parse(String id, ConfigurationSection section) {
        VipType.Builder builder = VipType.builder(id)
                .display(section.getString("display", ""))
                .prefix(section.getString("prefix", ""))
                .timeType(parseTimeType(section.getString("time-type", "ONLINE")))
                .credit(section.getDouble("credit", 0.0))
                .partyVipValue(section.getDouble("party-vip-value", 0.0))
                .activationMenu(section.getBoolean("activation-menu", false))
                .allowSell(section.getBoolean("allow-sell", true))
                .order(section.getInt("order", 0))
                .tagPermanent(section.getBoolean("tag-permanent", false))
                .mcmmoXpBoost(section.getDouble("mcmmo-xp-boost", 1.0))
                .mcmmoXpFlat(section.getInt("mcmmo-xp-flat", 0))
                .mythicDrop(section.getString("mythic-drop", ""))
                .battlepassXp(section.getInt("battlepass-xp", 0))
                .pets(section.getStringList("pets"));

        builder.icon(IconTemplate.fromSection(section.getConfigurationSection("icon")))
                .keyPreview(IconTemplate.fromSection(section.getConfigurationSection("key-preview")))
                .activePreview(IconTemplate.fromSection(section.getConfigurationSection("active-preview")))
                .expiredPreview(IconTemplate.fromSection(section.getConfigurationSection("expired-preview")));

        ConfigurationSection announces = section.getConfigurationSection("announces");
        if (announces != null) {
            builder.announceSound(announces.getString("sound", ""))
                    .announceEffect(announces.getString("effect", ""))
                    .announceActionBar(announces.getString("actionbar", ""))
                    .announceTitle(announces.getString("title", ""))
                    .announceChat(announces.getString("chat", ""))
                    .announceChatPrivate(announces.getString("chat-private", ""));
        }

        ConfigurationSection group = section.getConfigurationSection("group-command");
        if (group != null) {
            builder.groupAddCmds(group.getStringList("add"))
                    .groupRemoveCmds(group.getStringList("remove"))
                    .groupAddTempCmds(group.getStringList("addtime"))
                    .groupRemoveTempCmds(group.getStringList("removetime"));
        }

        builder.activationCommands(section.getStringList("activation-commands"));
        builder.activationItems(parseActivationItems(section.getConfigurationSection("activation-items")));

        ConfigurationSection upgrade = section.getConfigurationSection("upgrade");
        if (upgrade != null) {
            Map<String, Double> prices = new LinkedHashMap<>();
            ConfigurationSection pricesSection = upgrade.getConfigurationSection("prices");
            if (pricesSection != null) {
                for (String currency : pricesSection.getKeys(false)) {
                    prices.put(currency.toLowerCase(), pricesSection.getDouble(currency));
                }
            }
            builder.upgradeTo(upgrade.getString("vip-type", ""))
                    .upgradePrices(prices)
                    .upgradeCommands(upgrade.getStringList("commands"))
                    .upgradeMessage(upgrade.getString("message", ""))
                    .upgradeBroadcast(upgrade.getString("message-broadcast", ""));
        }

        ConfigurationSection discord = section.getConfigurationSection("discord");
        if (discord != null) {
            builder.discordWebhookEnabled(discord.getBoolean("webhook-enabled", false))
                    .discordEmbedId(discord.getString("embed-id", ""));
        }

        builder.perks(parsePerks(section.getConfigurationSection("perks")));

        return builder.build();
    }

    private VipPerks parsePerks(ConfigurationSection section) {
        if (section == null) {
            return VipPerks.NONE;
        }
        return new VipPerks(
                section.getBoolean("fly", false),
                section.getBoolean("autopickup", false),
                section.getBoolean("autosmelt", false),
                section.getBoolean("keep-inventory", false),
                section.getBoolean("back", false),
                section.getBoolean("feed", false),
                section.getBoolean("reparar", false),
                section.getBoolean("bigorna", false),
                section.getBoolean("bancada", false),
                section.getBoolean("colored-signs", false)
        );
    }

    private List<VipItem> parseActivationItems(ConfigurationSection section) {
        List<VipItem> items = new ArrayList<>();
        if (section == null) {
            return items;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(key);
            if (entry == null) {
                continue;
            }
            double chance = entry.getDouble("chance", 100.0);
            items.add(new VipItem(chance, buildActivationItem(entry)));
        }
        return items;
    }

    /**
     * Campo "itemsadder" opcional troca o item base por um item custom do ItemsAdder
     * (fallback pro material padrao se o hook nao estiver disponivel); "ae-enchants"
     * opcional aplica encantamentos do AdvancedEnchantments por cima do item resultante.
     * Ambos sao soft-dependency - se o plugin correspondente nao estiver instalado, o
     * item sai identico ao que sairia sem esses campos.
     */
    private ItemStack buildActivationItem(ConfigurationSection entry) {
        ItemStack stack = null;
        String itemsAdderId = entry.getString("itemsadder", "");
        if (itemsAdderId != null && !itemsAdderId.isBlank() && hooks.itemsAdder().isAvailable()) {
            stack = hooks.itemsAdder().getItemStack(itemsAdderId);
        }
        if (stack == null) {
            stack = ItemBuilder.fromSection(entry).build();
        }
        if (hooks.advancedEnchantments().isAvailable()) {
            for (String raw : entry.getStringList("ae-enchants")) {
                String[] parts = raw.split(":", 2);
                if (parts.length != 2) {
                    continue;
                }
                try {
                    hooks.advancedEnchantments().applyEnchant(stack, parts[0], Integer.parseInt(parts[1].trim()));
                } catch (NumberFormatException ignored) {
                    plugin.getLogger().warning("Nivel de ae-enchants invalido: '" + raw + "'");
                }
            }
        }
        return stack;
    }

    private TimeType parseTimeType(String raw) {
        try {
            return TimeType.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            return TimeType.ONLINE;
        }
    }

    public VipType get(String id) {
        return vipTypes.get(id);
    }

    /** Alias de {@link #get(String)} - nome usado pelo contrato pedido em cima do VipConfig do scaffold. */
    public VipType getVip(String id) {
        return get(id);
    }

    public boolean exists(String id) {
        return vipTypes.containsKey(id);
    }

    public Map<String, VipType> all() {
        return vipTypes;
    }

    /** VipTypes ordenados por {@code order} (vips.yml) - menor primeiro. */
    public List<VipType> getOrderedVips() {
        return vipTypes.values().stream()
                .sorted(java.util.Comparator.comparingInt(VipType::getOrder))
                .toList();
    }

    /** Id do proximo tier no upgrade a partir de {@code currentId}, ou null se nao houver (ex: drakkar). */
    public String getUpgradeTarget(String currentId) {
        VipType type = get(currentId);
        if (type == null || !type.hasUpgrade()) {
            return null;
        }
        return type.upgradeTo();
    }

    /** Precos de upgrade (moeda -> preco) a partir de {@code currentId}. Mapa vazio se nao houver upgrade. */
    public Map<String, Double> getUpgradePrice(String currentId) {
        VipType type = get(currentId);
        return type != null ? type.upgradePrices() : Map.of();
    }
}
