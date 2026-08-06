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
    private final Random random = new Random();

    public ActivationService(PlayerVipManager playerVipManager, CreditManager creditManager,
                              PartyVipManager partyVipManager, ConfigManager configManager, DiscordWebhook discordWebhook,
                              VipTypeManager vipTypeManager, HookManager hooks) {
        this.playerVipManager = playerVipManager;
        this.creditManager = creditManager;
        this.partyVipManager = partyVipManager;
        this.configManager = configManager;
        this.discordWebhook = discordWebhook;
        this.vipTypeManager = vipTypeManager;
        this.hooks = hooks;
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
        send(player, key, Map.of("vip", TextUtil.plain(vipType.display())));
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

    private void sendAnnounces(Player player, VipType vipType) {
        Map<String, String> placeholders = Map.of("player", player.getName());
        if (!vipType.announceActionBar().isBlank()) {
            player.sendActionBar(TextUtil.parse(vipType.announceActionBar(), placeholders));
        }
        if (!vipType.announceTitle().isBlank()) {
            String[] lines = vipType.announceTitle().split("<newline>", 2);
            player.showTitle(net.kyori.adventure.title.Title.title(
                    TextUtil.parse(lines[0], placeholders),
                    TextUtil.parse(lines.length > 1 ? lines[1] : "", placeholders)));
        }
        if (!vipType.announceChat().isBlank()) {
            Bukkit.broadcast(TextUtil.parse(vipType.announceChat(), placeholders));
        }
        if (!vipType.announceChatPrivate().isBlank()) {
            player.sendMessage(TextUtil.parse(vipType.announceChatPrivate(), placeholders));
        }
        if (!vipType.announceSound().isBlank()) {
            org.bukkit.Sound sound = org.bukkit.Registry.SOUNDS.get(
                    org.bukkit.NamespacedKey.minecraft(vipType.announceSound().toLowerCase()));
            if (sound != null) {
                player.playSound(player.getLocation(), sound, 1f, 1f);
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
        player.sendMessage(TextUtil.parse(raw, placeholders));
    }
}
