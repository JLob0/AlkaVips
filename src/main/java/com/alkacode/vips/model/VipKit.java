package com.alkacode.vips.model;

import org.bukkit.inventory.ItemStack;

import java.util.List;

public record VipKit(String vipTypeId, String id, String displayName, long cooldownMillis, List<ItemStack> items) {

    public String key() {
        return vipTypeId + ":" + id;
    }
}
