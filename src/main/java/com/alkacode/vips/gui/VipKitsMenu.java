package com.alkacode.vips.gui;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.vips.VipsServices;
import com.alkacode.vips.config.GuiLayout;
import com.alkacode.vips.model.VipType;
import com.alkacode.vips.util.ItemBuilder;
import com.alkacode.vips.util.TextUtil;
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
    private final GuiLayout layout;
    private final int[] slots;

    public VipKitsMenu(Player viewer, VipsServices services) {
        super(services.plugin, viewer, services.configManager.menus().getString("vipkits.title", "<dark_gray>Kits de Ativacao"),
                services.configManager.menus().getInt("vipkits.size", 27) / 9, "vip_kits");
        this.services = services;
        this.layout = services.configManager.layout("vipkits");
        this.slots = layout.findSlots('0').stream().mapToInt(Integer::intValue).toArray();
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
            setItem(slots[13], services.configManager.menuItem("vipkits.empty"));
        } else {
            int slot = 0;
            for (VipType type : availableTypes) {
                if (slot >= slots.length) {
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
                var item = new ItemBuilder(services.configManager.menuItem("vipkits.item"))
                        .name("<#FFD700><bold>Kit <white>" + TextUtil.plain(type.display()))
                        .lore(lore)
                        .build();
                setItem(slots[slot], item, e -> {
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

        setItem(layout.firstSlot('V'), services.configManager.menuItem("common.voltar"),
                e -> new MainVipMenu(player, services).open());
        fill(services.configManager.menuItem("fill-empty"));
    }
}
