package com.alkacode.vips;

import com.alkacode.vips.config.ConfigManager;
import com.alkacode.vips.gui.ChatInputManager;
import com.alkacode.vips.hook.AlkaEconomyHook;
import com.alkacode.vips.hook.AlkaFlairHook;
import com.alkacode.vips.hook.DiscordWebhook;
import com.alkacode.vips.hook.HookManager;
import com.alkacode.vips.manager.AffiliateManager;
import com.alkacode.vips.manager.BoostManager;
import com.alkacode.vips.manager.CreditManager;
import com.alkacode.vips.manager.KeyManager;
import com.alkacode.vips.manager.LegacyManager;
import com.alkacode.vips.manager.PartyVipManager;
import com.alkacode.vips.manager.PerkTreeManager;
import com.alkacode.vips.manager.PerksManager;
import com.alkacode.vips.manager.PlayerVipManager;
import com.alkacode.vips.manager.VipTypeManager;
import com.alkacode.vips.manager.WalletManager;
import com.alkacode.vips.service.ActivationService;
import com.alkacode.vips.service.ExpirationService;
import com.alkacode.vips.service.KeyUsageService;
import com.alkacode.vips.service.MarketplaceService;
import com.alkacode.vips.service.P2PMarketService;
import com.alkacode.vips.service.TransferService;
import com.alkacode.vips.service.UpgradeService;
import com.alkacode.vips.storage.VipsRepository;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Agrega todos os managers/services do AlkaVips num unico objeto para simplificar a
 * injecao nos varios comandos/menus/listeners - sem isso cada classe precisaria de
 * dezenas de parametros de construtor repetidos.
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
    public final HookManager hooks;
    public final AlkaFlairHook flairHook;
    public final TransferService transferService;
    public final LegacyManager legacyManager;
    public final WalletManager walletManager;
    public final AffiliateManager affiliateManager;
    public final BoostManager boostManager;
    public final P2PMarketService p2pMarketService;
    public final PerkTreeManager perkTreeManager;
    public final com.alkacode.vips.hook.AlkaItemsHook itemsHook;
    public final com.alkacode.core.util.PermissionNamesStore permissionNames;

    public VipsServices(JavaPlugin plugin, ConfigManager configManager, VipTypeManager vipTypeManager,
                         VipsRepository database, PlayerVipManager playerVipManager, CreditManager creditManager,
                         KeyManager keyManager, PartyVipManager partyVipManager, AlkaEconomyHook economyHook,
                         DiscordWebhook discordWebhook, ActivationService activationService,
                         UpgradeService upgradeService, MarketplaceService marketplaceService,
                         KeyUsageService keyUsageService, ExpirationService expirationService,
                         ChatInputManager chatInputManager, PerksManager perksManager,
                         HookManager hooks, AlkaFlairHook flairHook, TransferService transferService,
                         LegacyManager legacyManager, WalletManager walletManager, AffiliateManager affiliateManager,
                         BoostManager boostManager, P2PMarketService p2pMarketService, PerkTreeManager perkTreeManager,
                         com.alkacode.vips.hook.AlkaItemsHook itemsHook,
                         com.alkacode.core.util.PermissionNamesStore permissionNames) {
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
        this.hooks = hooks;
        this.flairHook = flairHook;
        this.transferService = transferService;
        this.legacyManager = legacyManager;
        this.walletManager = walletManager;
        this.affiliateManager = affiliateManager;
        this.boostManager = boostManager;
        this.p2pMarketService = p2pMarketService;
        this.perkTreeManager = perkTreeManager;
        this.itemsHook = itemsHook;
        this.permissionNames = permissionNames;
    }

    /** Linhas de lore "permissoes deste vip" pra esse tier, ja resolvidas com nomes
     * amigaveis (ver PermissionNamesStore/PermissionLoreUtil) - vazio se o tier nao tem
     * permission-group configurado em vips.yml. Usar como o parametro permissionLines de
     * IconTemplate#build quando a lore do tier tiver a linha marcadora "%permissions%". */
    public java.util.List<String> permissionLoreLines(com.alkacode.vips.model.VipType type) {
        return com.alkacode.vips.util.VipPermissionLore.forType(type, configManager, permissionNames);
    }

    public void sendMessage(org.bukkit.command.CommandSender sender, String path, java.util.Map<String, String> placeholders) {
        String raw = configManager.prefix() + configManager.message(path);
        sender.sendMessage(com.alkacode.vips.util.TextUtil.parse(raw, placeholders));
    }
}
