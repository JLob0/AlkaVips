package com.alkacode.vips;

import com.alkacode.core.api.AlkaAPI;
import com.alkacode.core.plugin.AlkaPlugin;
import com.alkacode.vips.command.AlkaVipsCommand;
import com.alkacode.vips.command.CreditCommand;
import com.alkacode.vips.command.KeyAdminCommands;
import com.alkacode.vips.command.KeyPlayerCommands;
import com.alkacode.vips.command.PerkCommands;
import com.alkacode.vips.command.VipAdminCommands;
import com.alkacode.vips.command.VipPlayerCommands;
import com.alkacode.vips.config.ConfigManager;
import com.alkacode.vips.gui.ChatInputManager;
import com.alkacode.vips.hook.AlkaEconomyHook;
import com.alkacode.vips.hook.DiscordWebhook;
import com.alkacode.vips.hook.HookManager;
import com.alkacode.vips.hook.PlaceholderAPIHook;
import com.alkacode.vips.listener.ChatInputListener;
import com.alkacode.vips.listener.KeyInteractListener;
import com.alkacode.vips.listener.PerksListener;
import com.alkacode.vips.listener.PlayerJoinListener;
import com.alkacode.vips.listener.PlayerQuitListener;
import com.alkacode.vips.manager.CreditManager;
import com.alkacode.vips.manager.KeyManager;
import com.alkacode.vips.manager.KitManager;
import com.alkacode.vips.manager.PartyVipManager;
import com.alkacode.vips.manager.PerksManager;
import com.alkacode.vips.manager.PlayerVipManager;
import com.alkacode.vips.manager.VipTypeManager;
import com.alkacode.vips.model.PlayerVip;
import com.alkacode.vips.model.VipType;
import com.alkacode.vips.model.enums.TimeType;
import com.alkacode.vips.api.AlkaVipsAPI;
import com.alkacode.vips.api.AlkaVipsAPIProvider;
import com.alkacode.vips.service.ActivationService;
import com.alkacode.vips.service.ExpirationService;
import com.alkacode.vips.service.KeyUsageService;
import com.alkacode.vips.service.MarketplaceService;
import com.alkacode.vips.service.UpgradeService;
import com.alkacode.vips.storage.VipsRepository;
import com.alkacode.vips.task.VipExpirationTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;

public final class AlkaVipsPlugin extends AlkaPlugin {

    private VipsRepository database;
    private VipsServices services;
    private PlayerJoinListener playerJoinListener;

    @Override
    protected void onPluginEnable() {
        AlkaAPI api = getAlkaAPI();

        ConfigManager configManager = new ConfigManager(this);
        configManager.load();

        database = new VipsRepository(api.getDatabase(), getLogger());

        HookManager hooks = new HookManager(this);
        VipTypeManager vipTypeManager = new VipTypeManager(this, hooks);
        vipTypeManager.load();

        AlkaEconomyHook economyHook = AlkaEconomyHook.resolve();
        if (economyHook == null) {
            getLogger().warning("AlkaEconomy nao encontrado - upgrades/marketplace ficarao indisponiveis.");
        }

        PlayerVipManager playerVipManager = new PlayerVipManager(database);
        CreditManager creditManager = new CreditManager(playerVipManager, database, economyHook);
        PartyVipManager partyVipManager = new PartyVipManager(database, configManager);
        partyVipManager.load();
        KeyManager keyManager = new KeyManager(this, database);
        DiscordWebhook discordWebhook = new DiscordWebhook(this, configManager);
        ChatInputManager chatInputManager = new ChatInputManager();
        PerksManager perksManager = new PerksManager(playerVipManager, vipTypeManager);
        KitManager kitManager = new KitManager(this, database);
        kitManager.load();

        ExpirationService expirationService = new ExpirationService(playerVipManager, vipTypeManager, configManager);
        ActivationService activationService = new ActivationService(playerVipManager, creditManager, partyVipManager,
                configManager, discordWebhook, vipTypeManager, hooks);
        UpgradeService upgradeService = new UpgradeService(playerVipManager, vipTypeManager, configManager, economyHook);
        MarketplaceService marketplaceService = new MarketplaceService(keyManager, vipTypeManager, configManager, economyHook);
        KeyUsageService keyUsageService = new KeyUsageService(keyManager, vipTypeManager, activationService);

        services = new VipsServices(this, configManager, vipTypeManager, database, playerVipManager, creditManager,
                keyManager, partyVipManager, economyHook, discordWebhook, activationService, upgradeService,
                marketplaceService, keyUsageService, expirationService, chatInputManager, perksManager, kitManager,
                hooks);

        compensateServerDowntime(vipTypeManager, database);

        registerCommands();
        registerListeners();

        for (Player player : Bukkit.getOnlinePlayers()) {
            playerJoinListener.handle(player);
        }

        long interval = configManager.config().getLong("expiration-check-interval", 1200);
        new VipExpirationTask(expirationService).runTaskTimer(this, interval, interval);

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PlaceholderAPIHook(services).register();
            getLogger().info("Expansao do PlaceholderAPI registrada.");
        }

        AlkaVipsAPI vipsApi = new AlkaVipsAPIProvider(playerVipManager, creditManager, keyManager);
        getServer().getServicesManager().register(AlkaVipsAPI.class, vipsApi, this, ServicePriority.Normal);

        getLogger().info("AlkaVips habilitado.");
    }

    @Override
    protected void onPluginDisable() {
        if (database != null) {
            database.saveLastShutdownSync(System.currentTimeMillis());
            database.shutdown();
        }
    }

    private void registerCommands() {
        VipPlayerCommands vipPlayerCommands = new VipPlayerCommands(services);
        setExecutor("vip", vipPlayerCommands);
        setExecutor("vips", vipPlayerCommands);
        setExecutor("tempovip", vipPlayerCommands);
        setExecutor("trocarvip", vipPlayerCommands);
        setExecutor("congelarvip", vipPlayerCommands);
        setExecutor("partyvip", vipPlayerCommands);

        KeyPlayerCommands keyPlayerCommands = new KeyPlayerCommands(services);
        setExecutor("usarkey", keyPlayerCommands);
        setExecutor("vendervip", keyPlayerCommands);
        setExecutor("cancelarvenda", keyPlayerCommands);
        setExecutor("comprarvip", keyPlayerCommands);
        setExecutor("vendasvip", keyPlayerCommands);

        CreditCommand creditCommand = new CreditCommand(services);
        setExecutor("creditovip", creditCommand);

        VipAdminCommands vipAdminCommands = new VipAdminCommands(services);
        setExecutor("darvip", vipAdminCommands);
        setExecutor("removervip", vipAdminCommands);
        setExecutor("setvip", vipAdminCommands);
        setExecutor("removertempovip", vipAdminCommands);
        setExecutor("infovip", vipAdminCommands);
        setExecutor("bonusvip", vipAdminCommands);

        KeyAdminCommands keyAdminCommands = new KeyAdminCommands(services);
        setExecutor("gerarkey", keyAdminCommands);
        setExecutor("criarkey", keyAdminCommands);
        setExecutor("removerkey", keyAdminCommands);
        setExecutor("editarkey", keyAdminCommands);
        setExecutor("verkeys", keyAdminCommands);
        setExecutor("darkey", keyAdminCommands);

        setExecutor("alkavips", new AlkaVipsCommand(services));

        PerkCommands perkCommands = new PerkCommands(services);
        setExecutor("vipfly", perkCommands);
        setExecutor("vipback", perkCommands);
        setExecutor("vipfeed", perkCommands);
        setExecutor("vipreparar", perkCommands);
        setExecutor("vipbigorna", perkCommands);
        setExecutor("vipbancada", perkCommands);
    }

    private void setExecutor(String name, org.bukkit.command.CommandExecutor executor) {
        var command = getCommand(name);
        if (command != null) {
            command.setExecutor(executor);
            if (executor instanceof org.bukkit.command.TabCompleter tabCompleter) {
                command.setTabCompleter(tabCompleter);
            }
        }
    }

    private void registerListeners() {
        playerJoinListener = new PlayerJoinListener(this, services.playerVipManager, services.vipTypeManager,
                services.expirationService, services.kitManager);
        getServer().getPluginManager().registerEvents(playerJoinListener, this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(services.playerVipManager, services.kitManager), this);
        getServer().getPluginManager().registerEvents(new KeyInteractListener(services), this);
        getServer().getPluginManager().registerEvents(new ChatInputListener(this, services.chatInputManager), this);
        getServer().getPluginManager().registerEvents(new PerksListener(services.perksManager), this);
    }

    /**
     * VIPs do tipo SERVER descontam com o servidor ligado, independente de quem
     * estava online - o gap entre o ultimo shutdown e este startup precisa ser
     * devolvido a todos, nao so a quem estiver conectado agora.
     */
    private void compensateServerDowntime(VipTypeManager vipTypeManager, VipsRepository database) {
        long lastShutdown = database.loadLastShutdownSync();
        if (lastShutdown <= 0) {
            return;
        }
        long gap = System.currentTimeMillis() - lastShutdown;
        if (gap <= 0) {
            return;
        }
        for (PlayerVip vip : database.loadAllActivePlayerVipsSync()) {
            VipType type = vipTypeManager.get(vip.vipTypeId());
            if (type != null && type.timeType() == TimeType.SERVER && !vip.isPermanent() && !vip.frozen()) {
                vip.expiresAt(vip.expiresAt() + gap);
                database.updatePlayerVip(vip);
            }
        }
    }
}
