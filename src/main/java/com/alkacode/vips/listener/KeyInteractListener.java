package com.alkacode.vips.listener;

import com.alkacode.vips.VipsServices;
import com.alkacode.vips.gui.SellKeyMenu;
import com.alkacode.vips.service.KeyUsageService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public final class KeyInteractListener implements Listener {

    private final VipsServices services;

    public KeyInteractListener(VipsServices services) {
        this.services = services;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        ItemStack item = event.getItem();
        String keyId = services.keyManager.readKeyId(item);
        if (keyId == null) {
            return;
        }
        event.setCancelled(true);

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            new SellKeyMenu(event.getPlayer(), services, keyId, item).open();
        } else if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            KeyUsageService.Result result = services.keyUsageService.use(event.getPlayer(), keyId, item);
            switch (result) {
                case NOT_FOUND -> services.sendMessage(event.getPlayer(), "key.not-found", Map.of());
                case ALREADY_USED -> services.sendMessage(event.getPlayer(), "key.already-used", Map.of());
                case SUCCESS -> {
                    // ActivationService.activateDirect ja mandou vip.activated/vip.accumulated -
                    // mandar key.used-success aqui tambem duplicava a mensagem de sucesso no chat.
                }
            }
        }
    }
}
