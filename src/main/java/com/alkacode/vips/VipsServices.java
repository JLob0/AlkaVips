package com.alkacode.vips;

import com.alkacode.vips.config.ConfigManager;
import com.alkacode.vips.gui.ChatInputManager;
import com.alkacode.vips.hook.AlkaEconomyHook;
import com.alkacode.vips.hook.DiscordWebhook;
import com.alkacode.vips.hook.HookManager;
import com.alkacode.vips.manager.CreditManager;
import com.alkacode.vips.manager.KeyManager;
import com.alkacode.vips.manager.KitManager;
import com.alkacode.vips.manager.PartyVipManager;
import com.alkacode.vips.manager.PerksManager;
import com.alkacode.vips.manager.PlayerVipManager;
import com.alkacode.vips.manager.VipTypeManager;
import com.alkacode.vips.service.ActivationService;
import com.alkacode.vips.service.ExpirationService;
import com.alkacode.vips.service.KeyUsageService;
import com.alkacode.vips.service.MarketplaceService;
import com.alkacode.vips.service.UpgradeService;
import com.alkacode.vips.storage.VipsRepository;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Agrega todos os managers/services do AlkaVips num unico objeto para simplificar a
 * injecao nos varios comandos/menus/listeners - sem isso cada classe precisaria de 8+
 * parametros de construtor repetidos.
 */
public final class VipsServices {

    public final JavaPlugin plugin;
    public final ConfigManager configManager;
    public final VipTypeManager vipTypeManager;
    public final VipsRepository database;
    public final PlayerVipManager playerVipManager;
    public final CreditManager creditManager;
    public final KeyManager keyManager;
    public final PartyVipManager partyVipManager;
    public final AlkaEconomyHook economyHook;
    public final DiscordWebhook discordWebhook;
    public final ActivationService activationService;
    public final UpgradeService upgradeService;
    public final MarketplaceService marketplaceService;
    public final KeyUsageService keyUsageService;
    public final ExpirationService expirationService;
    public final ChatInputManager chatInputManager;
    public final PerksManager perksManager;
    public final KitManager kitManager;
    public final HookManager hooks;

    public VipsServices(JavaPlugin plugin, ConfigManager configManager, VipTypeManager vipTypeManager,
                         VipsRepository database, PlayerVipManager playerVipManager, CreditManager creditManager,
                         KeyManager keyManager, PartyVipManager partyVipManager, AlkaEconomyHook economyHook,
                         DiscordWebhook discordWebhook, ActivationService activationService,
                         UpgradeService upgradeService, MarketplaceService marketplaceService,
                         KeyUsageService keyUsageService, ExpirationService expirationService,
                         ChatInputManager chatInputManager, PerksManager perksManager, KitManager kitManager,
                         HookManager hooks) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.vipTypeManager = vipTypeManager;
        this.database = database;
        this.playerVipManager = playerVipManager;
        this.creditManager = creditManager;
        this.keyManager = keyManager;
        this.partyVipManager = partyVipManager;
        this.economyHook = economyHook;
        this.discordWebhook = discordWebhook;
        this.activationService = activationService;
        this.upgradeService = upgradeService;
        this.marketplaceService = marketplaceService;
        this.keyUsageService = keyUsageService;
        this.expirationService = expirationService;
        this.chatInputManager = chatInputManager;
        this.perksManager = perksManager;
        this.kitManager = kitManager;
        this.hooks = hooks;
    }

    public void sendMessage(org.bukkit.entity.Player player, String path, java.util.Map<String, String> placeholders) {
        String raw = configManager.prefix() + configManager.message(path);
        player.sendMessage(com.alkacode.vips.util.TextUtil.parse(raw, placeholders));
    }
}
