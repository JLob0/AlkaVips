package com.alkacode.vips.gui;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.vips.VipsServices;
import com.alkacode.vips.model.PlayerVip;
import com.alkacode.vips.model.VipKit;
import com.alkacode.vips.model.VipType;
import com.alkacode.vips.util.ItemBuilder;
import com.alkacode.vips.util.TextUtil;
import com.alkacode.vips.util.TimeUtil;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mostra os kits de TODOS os VIPs ativos do jogador (nao so o selecionado) - um
 * jogador com VIP+MVP+ ativos ao mesmo tempo ve os kits dos dois, senao perde o
 * incentivo de acumular tiers em vez de so trocar pro mais alto. Layout em grade:
 * uma coluna por tier (ordem de {@link VipType#getOrder()}), uma linha por "nivel"
 * de kit (diario/semanal/mensal). Coluna inteira fica bloqueada se o jogador nao
 * tiver aquele tier ativo agora; celula fica bloqueada se aquele tier nao tiver
 * esse nivel de kit configurado em kits.yml.
 */
public final class KitsMenu extends BaseGui {

    private static final List<String> KIT_ROWS = List.of("diario", "semanal", "mensal");
    private static final int[] HEADER_SLOTS = {10, 11, 12, 13, 14, 15};
    private static final int[][] KIT_SLOTS = {
            {19, 20, 21, 22, 23, 24},
            {28, 29, 30, 31, 32, 33},
            {37, 38, 39, 40, 41, 42}
    };

    private final VipsServices services;

    public KitsMenu(Player viewer, VipsServices services) {
        super(services.plugin, viewer, services.configManager.menus().getString("kits.title", "&8Kits VIP"),
                services.configManager.menus().getInt("kits.size", 54) / 9, "vip_kits");
        this.services = services;
    }

    @Override
    public void render() {
        fillBorder(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build());
        setItem(getInventory().getSize() - 5, new ItemBuilder(Material.ARROW).name("<red>Voltar").build(),
                e -> new MainVipMenu(player, services).open());

        List<VipType> tiers = services.vipTypeManager.getOrderedVips();
        Set<String> activeTierIds = services.playerVipManager.getActiveVips(player.getUniqueId()).stream()
                .map(PlayerVip::vipTypeId)
                .collect(Collectors.toSet());

        int column = 0;
        for (VipType tier : tiers) {
            if (column >= HEADER_SLOTS.length) break;
            boolean owned = activeTierIds.contains(tier.id());
            renderColumn(column, tier, owned);
            column++;
        }
    }

    private void renderColumn(int column, VipType tier, boolean owned) {
        setItem(HEADER_SLOTS[column], new ItemBuilder(tier.icon().build())
                .glow(owned)
                .name(TextUtil.plain(tier.display()))
                .build());

        Map<String, VipKit> kits = services.kitManager.getKits(tier.id());
        for (int row = 0; row < KIT_ROWS.size(); row++) {
            VipKit kit = kits.get(KIT_ROWS.get(row));
            int slot = KIT_SLOTS[row][column];
            if (!owned) {
                setItem(slot, lockedTierItem(tier));
            } else if (kit == null) {
                setItem(slot, noKitItem(KIT_ROWS.get(row)));
            } else {
                setItem(slot, kitItem(kit), e -> claim(kit));
            }
        }
    }

    private org.bukkit.inventory.ItemStack lockedTierItem(VipType tier) {
        return new ItemBuilder(Material.BARRIER)
                .name("<dark_red><bold>Bloqueado")
                .lore(List.of("<#FF5555>Voce nao possui " + TextUtil.plain(tier.display())))
                .build();
    }

    private org.bukkit.inventory.ItemStack noKitItem(String row) {
        return new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .name("<gray><bold>Sem Kit")
                .lore(List.of("<gray>Este tier nao tem kit " + row + "."))
                .build();
    }

    /** Icone custom do ItemsAdder tem prioridade sobre o Material configurado no kit - cai pro Material se ausente/indisponivel. */
    private org.bukkit.inventory.ItemStack resolveKitIcon(VipKit kit) {
        if (!kit.iconItemsAdder().isBlank() && services.hooks.itemsAdder().isAvailable()) {
            org.bukkit.inventory.ItemStack custom = services.hooks.itemsAdder().getItemStack(kit.iconItemsAdder());
            if (custom != null) {
                return custom;
            }
        }
        return new org.bukkit.inventory.ItemStack(kit.iconMaterial());
    }

    private org.bukkit.inventory.ItemStack kitItem(VipKit kit) {
        long remaining = services.kitManager.remainingCooldown(player.getUniqueId(), kit);
        boolean available = remaining <= 0;
        ItemBuilder builder = new ItemBuilder(resolveKitIcon(kit))
                .glow(available)
                .name(available ? "<green><bold>" + kit.displayName() : "<red><bold>Em Recarga");
        if (available) {
            builder.lore(List.of(
                    "<dark_gray>─────────────────",
                    "<#55FF55>Status: <bold>DISPONÍVEL</bold>",
                    "<#AAAAAA>Clique para resgatar",
                    "<dark_gray>─────────────────"));
        } else {
            builder.lore(List.of(
                    "<dark_gray>─────────────────",
                    "<#FF5555>Status: <bold>EM RECARGA</bold>",
                    "<#AAAAAA>Disponível em: <#FFD700>" + TimeUtil.formatRemaining(remaining),
                    "<dark_gray>─────────────────"));
        }
        return builder.build();
    }

    private void claim(VipKit kit) {
        if (services.kitManager.remainingCooldown(player.getUniqueId(), kit) > 0) {
            return;
        }
        services.kitManager.claim(player, kit);
        services.sendMessage(player, "kit.claimed", Map.of("kit", kit.displayName()));
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.2f, 1f);
        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 35, 0.5, 0.6, 0.5, 0.05);
        refresh();
    }
}
