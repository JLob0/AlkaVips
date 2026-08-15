package com.alkacode.vips.gui;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.vips.VipsServices;
import com.alkacode.vips.model.VipType;
import com.alkacode.vips.util.ItemBuilder;
import com.alkacode.vips.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI de kits VIP do AlkaVips - mostra somente os kits de ATIVACAO (bonus unico de
 * 30+ dias) que o jogador desbloqueou mas ainda nao pegou. Ao clicar, entrega o kit
 * fisicamente (ver ActivationService#claimActivationBonus). Os kits RECORRENTES
 * (diario/semanal/mensal) nao aparecem aqui - moram no AlkaKits (/kits), gateados
 * por %alkavips_has_vip_<tier>%.
 */
public final class VipKitsMenu extends BaseGui {

    private final VipsServices services;

    public VipKitsMenu(Player viewer, VipsServices services) {
        super(services.plugin, viewer, services.configManager.menus().getString("vipkits.title", "<dark_gray>Kits de Ativacao"),
                services.configManager.menus().getInt("vipkits.size", 27) / 9, "vip_kits");
        this.services = services;
    }

    @Override
    public void render() {
        List<String> available = services.database.loadAvailableActivationBonusTiersSync(player.getUniqueId());
        List<VipType> availableTypes = new ArrayList<>();
        for (String tierId : available) {
            VipType type = services.vipTypeManager.get(tierId);
            if (type != null && type.hasActivationBonus()) {
                availableTypes.add(type);
            }
        }

        if (availableTypes.isEmpty()) {
            setItem(13, new ItemBuilder(Material.CHEST).name("<yellow>Nenhum kit de ativacao")
                    .lore(List.of(
                            "<gray>Kits de ativacao sao desbloqueados",
                            "<gray>ao ativar um VIP por 30+ dias.",
                            "",
                            "<gray>Os kits diarios/semanais/mensais",
                            "<gray>recorrentes ficam em <yellow>/kits<gray>."
                    )).build());
        } else {
            int slot = 0;
            for (VipType type : availableTypes) {
                if (slot >= 18) {
                    break;
                }
                List<String> lore = new ArrayList<>();
                lore.add("<gray>─────────────────");
                lore.add("<#FFD700><bold>✦ KIT DE ATIVACAO");
                lore.add("<gray>─────────────────");
                lore.add("<gray>Presente unico por ativar");
                lore.add("<gray>" + type.activationBonusMinDurationDays() + "+ dias de <white>" + TextUtil.plain(type.display()));
                lore.add("");
                lore.add("<#55FF55>Clique para pegar o kit!");
                var item = new ItemBuilder(Material.ENDER_CHEST)
                        .name("<#FFD700><bold>Kit <white>" + TextUtil.plain(type.display()))
                        .lore(lore)
                        .build();
                setItem(slot, item, e -> {
                    boolean claimed = services.activationService.claimActivationBonus(player, type);
                    if (claimed) {
                        services.sendMessage(player, "vipkits.claimed", java.util.Map.of("vip", type.display()));
                    } else {
                        services.sendMessage(player, "vipkits.already-claimed", java.util.Map.of("vip", type.display()));
                    }
                    refresh();
                });
                slot++;
            }
        }

        setItem(22, new ItemBuilder(Material.ARROW).name("<red>Voltar").build(),
                e -> new MainVipMenu(player, services).open());
        fill(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build());
    }
}
