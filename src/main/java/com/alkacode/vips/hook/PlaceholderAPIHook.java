package com.alkacode.vips.hook;

import com.alkacode.vips.VipsServices;
import com.alkacode.vips.model.PlayerVip;
import com.alkacode.vips.model.VipType;
import com.alkacode.vips.util.TextUtil;
import com.alkacode.vips.util.TimeUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public final class PlaceholderAPIHook extends PlaceholderExpansion {

    private final VipsServices services;

    public PlaceholderAPIHook(VipsServices services) {
        this.services = services;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "alkavips";
    }

    @Override
    public @NotNull String getAuthor() {
        return "MestreDEV";
    }

    @Override
    public @NotNull String getVersion() {
        return services.plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }
        Optional<PlayerVip> selected = services.playerVipManager.getSelectedVip(player.getUniqueId());
        VipType type = selected.map(v -> services.vipTypeManager.get(v.vipTypeId())).orElse(null);

        // has_vip_<tier>/vip_duration_days_<tier> checam TODOS os VIPs ativos do jogador
        // (getActiveVipsOfType), nao so o "selecionado" (vip_id/vip acima) - um jogador
        // pode ter mais de um tier ativo ao mesmo tempo (multi-VIP), e gatear kit por
        // "selecionado" bloquearia injustamente o kit de um tier que ele tem mas nao
        // esta com o prefixo/display escolhido no momento. Consumido pelo AlkaKits
        // (kits.yml -> requisitos tipo PLACEHOLDER) pra portar os kits por tier de VIP.
        if (params.startsWith("has_vip_")) {
            String tier = params.substring("has_vip_".length());
            boolean active = !services.playerVipManager.getActiveVipsOfType(player.getUniqueId(), tier).isEmpty();
            return String.valueOf(active);
        }
        if (params.startsWith("vip_duration_days_")) {
            String tier = params.substring("vip_duration_days_".length());
            return services.playerVipManager.getActiveVipsOfType(player.getUniqueId(), tier).stream()
                    .findFirst()
                    .map(v -> v.isPermanent() ? "999999" : String.valueOf(v.totalDuration() / 86_400_000L))
                    .orElse("0");
        }

        return switch (params) {
            case "vip" -> type != null ? TextUtil.plain(type.display()) : "";
            case "vip_id" -> selected.map(PlayerVip::vipTypeId).orElse("");
            case "vip_prefix" -> type != null ? TextUtil.plain(type.prefix()) : "";
            // _colored: mesmo dado, mas serializado pra § real (TAB/nChat/scoreboard so
            // entendem codigo legado real, nunca MiniMessage cru) - vip/vip_prefix acima
            // ficam plain de proposito (uso ja estabelecido em GUI/lore), esses sao novos,
            // sem consumidor existente pra quebrar.
            case "vip_colored" -> type != null ? toLegacy(type.display()) : "";
            case "vip_prefix_colored" -> type != null ? toLegacy(type.prefix()) : "";
            case "vip_time_remaining" -> selected.map(v -> v.isPermanent() ? "Permanente" : TimeUtil.formatRemaining(v.remainingMillis())).orElse("");
            case "vip_time_remaining_short" -> selected.map(v -> v.isPermanent() ? "Permanente" : TimeUtil.formatRemainingShort(v.remainingMillis())).orElse("");
            case "vip_expires_at" -> selected.map(v -> v.isPermanent() ? "Nunca" : String.valueOf(v.expiresAt())).orElse("");
            case "vip_activated_at" -> selected.map(v -> String.valueOf(v.activatedAt())).orElse("");
            case "credits" -> String.valueOf(services.creditManager.getCredits(player.getUniqueId()));
            case "credits_formatted" -> services.economyHook != null ? services.economyHook.format(services.creditManager.getCredits(player.getUniqueId())) : String.valueOf(services.creditManager.getCredits(player.getUniqueId()));
            case "keys_amount" -> String.valueOf(countKeys(player));
            case "has_vip" -> String.valueOf(selected.isPresent());
            case "party_progress" -> String.valueOf(services.partyVipManager.getProgress());
            case "party_goal" -> String.valueOf(services.partyVipManager.getGoal());
            case "party_percentage" -> String.valueOf((int) services.partyVipManager.getPercentage());
            default -> null;
        };
    }

    // character(SECTION_CHAR) (nao legacyAmpersand()) + useUnusualXRepeatedCharacterHexFormat():
    // mesmo padrao fixado no ecossistema (AlkaFlair/AlkaMines) - TAB/nChat so entendem
    // codigo real "§", nunca texto "&" cru.
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character(LegacyComponentSerializer.SECTION_CHAR)
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private String toLegacy(String miniMessage) {
        if (miniMessage == null || miniMessage.isBlank()) {
            return "";
        }
        return LEGACY.serialize(MiniMessage.miniMessage().deserialize("<!i>" + miniMessage)) + "§r";
    }

    private int countKeys(OfflinePlayer player) {
        if (!(player.getPlayer() instanceof org.bukkit.entity.Player online)) {
            return 0;
        }
        int count = 0;
        for (var item : online.getInventory().getContents()) {
            if (services.keyManager.readKeyId(item) != null) {
                count++;
            }
        }
        return count;
    }
}
