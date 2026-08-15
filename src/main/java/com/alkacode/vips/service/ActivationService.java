package com.alkacode.vips.service;

import com.alkacode.vips.config.ConfigManager;
import com.alkacode.vips.event.VipActivateEvent;
import com.alkacode.vips.hook.DiscordWebhook;
import com.alkacode.vips.hook.HookManager;
import com.alkacode.vips.manager.CreditManager;
import com.alkacode.vips.manager.PartyVipManager;
import com.alkacode.vips.manager.PlayerVipManager;
import com.alkacode.vips.manager.VipTypeManager;
import com.alkacode.vips.model.PlayerVip;
import com.alkacode.vips.model.VipItem;
import com.alkacode.vips.model.VipType;
import com.alkacode.vips.model.enums.VipStatus;
import com.alkacode.vips.storage.VipsRepository;
import com.alkacode.vips.util.CommandUtil;
import com.alkacode.vips.util.TextUtil;
import com.alkacode.vips.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

public final class ActivationService {

    private final PlayerVipManager playerVipManager;
    private final CreditManager creditManager;
    private final PartyVipManager partyVipManager;
    private final ConfigManager configManager;
    private final DiscordWebhook discordWebhook;
    private final VipTypeManager vipTypeManager;
    private final HookManager hooks;
    private final VipsRepository database;
    private final Random random = new Random();

    public ActivationService(PlayerVipManager playerVipManager, CreditManager creditManager,
                              PartyVipManager partyVipManager, ConfigManager configManager, DiscordWebhook discordWebhook,
                              VipTypeManager vipTypeManager, HookManager hooks, VipsRepository database) {
        this.playerVipManager = playerVipManager;
        this.creditManager = creditManager;
        this.partyVipManager = partyVipManager;
        this.configManager = configManager;
        this.discordWebhook = discordWebhook;
        this.vipTypeManager = vipTypeManager;
        this.hooks = hooks;
        this.database = database;
    }

    /**
     * Ponto de entrada unico para dar um VIP a um jogador - key, /darvip, /setvip (sem
     * anuncios) e a colheita de uma ativacao pendente passam por aqui.
     */
    public void activate(Player player, VipType vipType, long duration, String keyId, boolean silent) {
        if (vipType.activationMenu() && !silent) {
            createPending(player, vipType, duration, keyId);
            return;
        }
        activateDirect(player, vipType, duration, keyId, silent);
    }

    private void createPending(Player player, VipType vipType, long duration, String keyId) {
        PlayerVip pending = new PlayerVip(-1, player.getUniqueId(), vipType.id(), keyId, VipStatus.PENDING,
                0, duration, duration, false, 0);
        playerVipManager.addVip(pending);
        send(player, "key.pending-added", Map.of());
    }

    public void collectPending(Player player, PlayerVip pending) {
        long duration = pending.totalDuration();
        playerVipManager.remove(pending);
        VipType vipType = vipTypeManager.get(pending.vipTypeId());
        if (vipType == null) {
            return;
        }
        activateDirect(player, vipType, duration, pending.keyId(), false);
    }

    private void activateDirect(Player player, VipType vipType, long duration, String keyId, boolean silent) {
        UUID uuid = player.getUniqueId();
        Optional<PlayerVip> existing = playerVipManager.getActiveVipsOfType(uuid, vipType.id()).stream().findFirst();

        boolean accumulated = existing.isPresent();
        PlayerVip playerVip;
        if (accumulated) {
            playerVip = existing.get();
            if (!playerVip.isPermanent() && duration != 0) {
                playerVip.expiresAt(playerVip.expiresAt() + duration);
                playerVip.totalDuration(playerVip.totalDuration() + duration);
            } else if (duration == 0) {
                playerVip.expiresAt(0);
            }
            playerVipManager.update(playerVip);
        } else {
            long expiresAt = duration == 0 ? 0 : System.currentTimeMillis() + duration;
            PlayerVip created = new PlayerVip(-1, uuid, vipType.id(), keyId, VipStatus.ACTIVE,
                    System.currentTimeMillis(), expiresAt, duration, false, 0);
            playerVip = playerVipManager.addVip(created).join();
        }

        applyGroupCommands(player, vipType, duration, accumulated);
        applyActivationRewards(player, vipType);
        applyHookRewards(player, vipType);
        deliverActivationBonus(player, vipType, duration);
        creditManager.add(uuid, vipType.credit());
        creditManager.incrementActivations(uuid);
        partyVipManager.addProgress(vipType.partyVipValue());

        if (!silent) {
            sendAnnounces(player, vipType);
            if (vipType.discordWebhookEnabled()) {
                discordWebhook.sendEmbed(vipType.discordEmbedId(), Map.of(
                        "player", player.getName(),
                        "vip_display", TextUtil.plain(vipType.display())
                ));
            }
        }

        Bukkit.getPluginManager().callEvent(new VipActivateEvent(player, vipType, playerVip, accumulated));

        String key = accumulated ? "vip.accumulated" : "vip.activated";
        send(player, key, Map.of("vip", vipType.display()));
    }

    private void applyGroupCommands(Player player, VipType vipType, long duration, boolean accumulated) {
        List<String> commands = duration == 0 ? vipType.groupAddCmds() : vipType.groupAddTempCmds();
        Map<String, String> placeholders = Map.of(
                "player", player.getName(),
                "time", TimeUtil.toLuckPermsDuration(duration)
        );
        dispatchAll(commands, placeholders);
    }

    private void applyActivationRewards(Player player, VipType vipType) {
        for (String entry : vipType.activationCommands()) {
            String[] parts = entry.split(",", 2);
            if (parts.length != 2) {
                continue;
            }
            double chance = parseDouble(parts[0]);
            if (roll(chance)) {
                dispatch(parts[1], Map.of("player", player.getName()));
            }
        }
        for (VipItem item : vipType.activationItems()) {
            if (roll(item.chance())) {
                player.getInventory().addItem(item.itemStack().clone());
            }
        }
    }

    /**
     * Recompensas dos hooks de terceiros opcionais (com.alkacode.vips.hook) - cada
     * chamada e no-op silencioso se o plugin correspondente nao estiver instalado.
     * XP do mcMMO usa os campos mcmmo-xp-boost/mcmmo-xp-flat do VipType (nao um
     * multiplicador fixo hardcoded), pra ficar ajustavel por tier via vips.yml.
     */
    private void applyHookRewards(Player player, VipType vipType) {
        int mcmmoXp = (int) Math.round(vipType.credit() * vipType.mcmmoXpBoost()) + vipType.mcmmoXpFlat();
        if (mcmmoXp > 0) {
            hooks.mcmmo().addSkillXpAll(player, mcmmoXp);
        }
        if (vipType.battlepassXp() > 0) {
            hooks.battlePass().addXp(player, vipType.battlepassXp());
        }
        if (vipType.mythicDrop() != null && !vipType.mythicDrop().isBlank()) {
            hooks.mythicMobs().giveDrop(player, vipType.mythicDrop());
        }
        for (String petId : vipType.pets()) {
            hooks.mcPets().givePet(player, petId);
        }
    }

    /**
     * Title, actionbar, som e chat vao pra TODO MUNDO online (celebra a ativacao pro
     * servidor inteiro, nao so pra quem ativou). Chat usa sendMessage direto por
     * player em vez de Bukkit.broadcast() pra nao depender da permissao
     * bukkit.broadcast.user - garante entrega mesmo se algum grupo do LuckPerms
     * nao tiver essa node. chat-private e a particula continuam so pro jogador que
     * ativou (mensagem pessoal / efeito no proprio corpo dele).
     */
    private void sendAnnounces(Player player, VipType vipType) {
        Map<String, String> placeholders = Map.of(
                "player", player.getName(),
                "shop-url", configManager.shopUrl());

        if (!vipType.announceActionBar().isBlank()) {
            var actionBar = TextUtil.parse(vipType.announceActionBar(), placeholders);
            Bukkit.getOnlinePlayers().forEach(p -> p.sendActionBar(actionBar));
        }
        if (!vipType.announceTitle().isBlank()) {
            String[] lines = vipType.announceTitle().split("<newline>", 2);
            var title = net.kyori.adventure.title.Title.title(
                    TextUtil.parse(lines[0], placeholders),
                    TextUtil.parse(lines.length > 1 ? lines[1] : "", placeholders));
            Bukkit.getOnlinePlayers().forEach(p -> p.showTitle(title));
        }
        if (!vipType.announceChat().isBlank()) {
            String chatMessage = TextUtil.legacyParse(vipType.announceChat(), placeholders);
            Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(chatMessage));
        }
        if (!vipType.announceChatPrivate().isBlank()) {
            player.sendMessage(TextUtil.legacyParse(vipType.announceChatPrivate(), placeholders));
        }
        if (!vipType.announceSound().isBlank()) {
            org.bukkit.Sound sound = org.bukkit.Registry.SOUNDS.get(
                    org.bukkit.NamespacedKey.minecraft(vipType.announceSound().toLowerCase()));
            if (sound != null) {
                Bukkit.getOnlinePlayers().forEach(p -> p.playSound(p.getLocation(), sound, 0.8f, 1f));
            }
        }
        if (!vipType.announceEffect().isBlank()) {
            try {
                player.getWorld().spawnParticle(org.bukkit.Particle.valueOf(vipType.announceEffect()),
                        player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    /**
     * Presente de ativacao - uma vez por conta, pra sempre, so libera se ESTA
     * ativacao especifica valer >= activation-bonus.min-duration-days (nao a soma
     * acumulada de compras menores - ver vip_activation_bonus_claimed). Roda em
     * TODA ativacao (accumulated ou nao) porque o que importa e a duracao da compra
     * que acabou de entrar, nao se e a primeira vez que o jogador pega esse tier.
     *
     * <p>Nao entrega itens automaticamente - so marca o bonus como DISPONIVEL pra
     * pegar na GUI de kits VIP (ver gui/VipKitsMenu). A entrega fisica acontece
     * quando o jogador clica no kit, evitando que o inventario entupa e deixando o
     * jogador escolher a hora de pegar.
     */
    private void deliverActivationBonus(Player player, VipType vipType, long duration) {
        if (!vipType.hasActivationBonus()) {
            return;
        }
        long durationDays = duration == 0 ? Long.MAX_VALUE : duration / 86_400_000L;
        if (durationDays < vipType.activationBonusMinDurationDays()) {
            return;
        }
        UUID uuid = player.getUniqueId();
        if (database.hasClaimedActivationBonusSync(uuid, vipType.id())) {
            return;
        }
        database.grantActivationBonusAvailableSync(uuid, vipType.id());
        sendActivationBonusAnnounce(player, vipType);
    }

    /**
     * Entrega o kit de ativacao que ficou disponivel (ver gui/VipKitsMenu) - da os
     * itens fisicamente, marca como reivindicado e limpa o estado disponivel. Se o
     * jogador ainda nao tem o bonus disponivel ou ja reivindicou, retorna false.
     */
    public boolean claimActivationBonus(Player player, VipType vipType) {
        if (vipType == null || !vipType.hasActivationBonus()) {
            return false;
        }
        UUID uuid = player.getUniqueId();
        if (database.hasClaimedActivationBonusSync(uuid, vipType.id())) {
            return false;
        }
        if (!database.isActivationBonusAvailableSync(uuid, vipType.id())) {
            return false;
        }
        for (VipItem item : vipType.activationBonusItems()) {
            if (roll(item.chance())) {
                player.getInventory().addItem(item.itemStack().clone());
            }
        }
        database.revokeActivationBonusAvailableSync(uuid, vipType.id());
        database.markActivationBonusClaimedSync(uuid, vipType.id());
        sendActivationBonusAnnounce(player, vipType);
        return true;
    }

    /**
     * Mesmo racional do sendAnnounces: chat vai por sendMessage direto (nao
     * Bukkit.broadcast) pra nao depender de bukkit.broadcast.user; title/actionbar/
     * som tambem vao pro servidor inteiro (e um marco raro - primeira vez que
     * alguem bate a duracao minima de um tier), particula fica so no jogador.
     */
    private void sendActivationBonusAnnounce(Player player, VipType vipType) {
        Map<String, String> placeholders = Map.of("player", player.getName(), "vip", TextUtil.plain(vipType.display()));

        if (!vipType.activationBonusActionBar().isBlank()) {
            var actionBar = TextUtil.parse(vipType.activationBonusActionBar(), placeholders);
            Bukkit.getOnlinePlayers().forEach(p -> p.sendActionBar(actionBar));
        }
        if (!vipType.activationBonusTitle().isBlank()) {
            String[] lines = vipType.activationBonusTitle().split("<newline>", 2);
            var title = net.kyori.adventure.title.Title.title(
                    TextUtil.parse(lines[0], placeholders),
                    TextUtil.parse(lines.length > 1 ? lines[1] : "", placeholders));
            Bukkit.getOnlinePlayers().forEach(p -> p.showTitle(title));
        }
        if (!vipType.activationBonusChat().isBlank()) {
            String chatMessage = TextUtil.legacyParse(vipType.activationBonusChat(), placeholders);
            Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(chatMessage));
        }
        if (!vipType.activationBonusSound().isBlank()) {
            org.bukkit.Sound sound = org.bukkit.Registry.SOUNDS.get(
                    org.bukkit.NamespacedKey.minecraft(vipType.activationBonusSound().toLowerCase()));
            if (sound != null) {
                Bukkit.getOnlinePlayers().forEach(p -> p.playSound(p.getLocation(), sound, 0.8f, 1f));
            }
        }
        if (!vipType.activationBonusEffect().isBlank()) {
            try {
                player.getWorld().spawnParticle(org.bukkit.Particle.valueOf(vipType.activationBonusEffect()),
                        player.getLocation().add(0, 1, 0), 40, 0.5, 0.5, 0.5);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private void dispatchAll(List<String> commands, Map<String, String> placeholders) {
        for (String command : commands) {
            dispatch(command, placeholders);
        }
    }

    private void dispatch(String command, Map<String, String> placeholders) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), CommandUtil.substitute(command, placeholders));
    }

    private boolean roll(double chancePercent) {
        return random.nextDouble() * 100.0 < chancePercent;
    }

    private double parseDouble(String raw) {
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void send(Player player, String path, Map<String, String> placeholders) {
        String raw = configManager.prefix() + configManager.message(path);
        player.sendMessage(TextUtil.legacyParse(raw, placeholders));
    }
}
