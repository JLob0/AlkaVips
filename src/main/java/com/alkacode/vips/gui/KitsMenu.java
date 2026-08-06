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
        setItem(getInventory().getSize() - 5, new ItemBuilder(Material.BARRIER).name("<red>Voltar").build(),
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
        setItem(HEADER_SLOTS[column], new ItemBuilder(owned ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE)
                .name(TextUtil.plain(tier.display()))
                .build());

        Map<String, VipKit> kits = services.kitManager.getKits(tier.id());
        for (int row = 0; row < KIT_ROWS.size(); row++) {
            VipKit kit = kits.get(KIT_ROWS.get(row));
            int slot = KIT_SLOTS[row][column];
            if (!owned) {
                setItem(slot, lockedItem("<red>Bloqueado", "<gray>Voce nao possui " + TextUtil.plain(tier.display())));
            } else if (kit == null) {
                setItem(slot, lockedItem("<gray>Sem kit", "<gray>Este tier nao tem kit " + KIT_ROWS.get(row) + "."));
            } else {
                setItem(slot, kitItem(kit), e -> claim(kit));
            }
        }
    }

    private org.bukkit.inventory.ItemStack lockedItem(String name, String lore) {
        return new ItemBuilder(Material.BARRIER).name(name).lore(List.of(lore)).build();
    }

    private org.bukkit.inventory.ItemStack kitItem(VipKit kit) {
        long remaining = services.kitManager.remainingCooldown(player.getUniqueId(), kit);
        boolean available = remaining <= 0;
        ItemBuilder builder = new ItemBuilder(available ? Material.CHEST : Material.BARRIER)
                .name((available ? "<green>" : "<red>") + kit.displayName());
        if (available) {
            builder.lore(List.of("<gray>Clique para resgatar."));
        } else {
            builder.lore(List.of("<gray>Disponivel em: <white>" + TimeUtil.formatRemaining(remaining)));
        }
        return builder.build();
    }

    private void claim(VipKit kit) {
        if (services.kitManager.remainingCooldown(player.getUniqueId(), kit) > 0) {
            return;
        }
        services.kitManager.claim(player, kit);
        services.sendMessage(player, "kit.claimed", Map.of("kit", kit.displayName()));
        refresh();
    }
}
