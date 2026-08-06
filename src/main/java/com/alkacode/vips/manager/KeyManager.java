package com.alkacode.vips.manager;

import com.alkacode.vips.model.VipKey;
import com.alkacode.vips.model.VipType;
import com.alkacode.vips.storage.VipsRepository;
import com.alkacode.vips.util.KeyGenerator;
import com.alkacode.vips.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class KeyManager {

    private final VipsRepository database;
    private final NamespacedKey keyIdKey;

    public KeyManager(JavaPlugin plugin, VipsRepository database) {
        this.database = database;
        this.keyIdKey = new NamespacedKey(plugin, "vip_key_id");
    }

    public CompletableFuture<VipKey> generate(VipType vipType, long duration, boolean bonus) {
        String code = KeyGenerator.generate();
        VipKey key = new VipKey(code, vipType.id(), duration, false, null, 0, bonus, false, null, null, 0);
        return database.insertKey(key).thenApply(v -> key);
    }

    public CompletableFuture<VipKey> create(VipType vipType, long duration, String code, boolean bonus) {
        return database.loadKey(code).thenCompose(existing -> {
            if (existing != null) {
                return CompletableFuture.completedFuture(null);
            }
            VipKey key = new VipKey(code, vipType.id(), duration, false, null, 0, bonus, false, null, null, 0);
            return database.insertKey(key).thenApply(v -> key);
        });
    }

    public CompletableFuture<VipKey> find(String code) {
        return database.loadKey(code);
    }

    public VipKey findSync(String code) {
        return database.loadKeySync(code);
    }

    public CompletableFuture<Void> save(VipKey key) {
        return database.updateKey(key);
    }

    public CompletableFuture<Void> delete(String code) {
        return database.deleteKey(code);
    }

    public CompletableFuture<java.util.List<VipKey>> loadForSale() {
        return database.loadForSaleKeys();
    }

    public ItemStack buildItem(VipKey key, VipType vipType) {
        long expiresIn = key.duration();
        Map<String, String> placeholders = Map.of(
                "id", key.id(),
                "expire_remaining", key.duration() == 0 ? "Permanente" : TimeUtil.formatRemaining(expiresIn)
        );
        ItemStack item = vipType.keyPreview().build(placeholders);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(keyIdKey, PersistentDataType.STRING, key.id());
        item.setItemMeta(meta);
        return item;
    }

    public String readKeyId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(keyIdKey, PersistentDataType.STRING);
    }

    public void giveKeyItem(UUID playerUuid, ItemStack item) {
        var player = Bukkit.getPlayer(playerUuid);
        if (player != null) {
            player.getInventory().addItem(item);
        }
    }
}
