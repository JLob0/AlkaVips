package com.alkacode.vips.hook;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Soft-dependency para o ItemsAdder (dev.lone.itemsadder.api.CustomStack) via reflection -
 * ver {@link HookReflection}. CustomStack.getInstance(id)/byItemStack(item) sao a API
 * publica estavel do ItemsAdder.
 */
public final class ItemsAdderHook {

    private static final String CUSTOM_STACK_CLASS = "dev.lone.itemsadder.api.CustomStack";

    private final JavaPlugin vipsPlugin;
    private final Plugin plugin;

    public ItemsAdderHook(JavaPlugin vipsPlugin) {
        this.vipsPlugin = vipsPlugin;
        this.plugin = Bukkit.getPluginManager().getPlugin("ItemsAdder");
    }

    public boolean isAvailable() {
        return plugin != null && plugin.isEnabled();
    }

    public ItemStack getItemStack(String namespacedId) {
        if (!isAvailable() || namespacedId == null || namespacedId.isBlank()) {
            return null;
        }
        Object customStack = HookReflection.invokeStatic(vipsPlugin.getLogger(), "ItemsAdder", CUSTOM_STACK_CLASS,
                "getInstance", new Class<?>[]{String.class}, namespacedId);
        Object stack = HookReflection.invokeInstance(vipsPlugin.getLogger(), "ItemsAdder", customStack, "getItemStack",
                new Class<?>[0]);
        return stack instanceof ItemStack item ? item : null;
    }

    public boolean isItemsAdderItem(ItemStack item) {
        return customStackOf(item) != null;
    }

    public String getNamespacedId(ItemStack item) {
        Object customStack = customStackOf(item);
        if (customStack == null) {
            return null;
        }
        Object id = HookReflection.invokeInstance(vipsPlugin.getLogger(), "ItemsAdder", customStack, "getNamespacedID",
                new Class<?>[0]);
        return id instanceof String s ? s : null;
    }

    private Object customStackOf(ItemStack item) {
        if (!isAvailable() || item == null) {
            return null;
        }
        return HookReflection.invokeStatic(vipsPlugin.getLogger(), "ItemsAdder", CUSTOM_STACK_CLASS, "byItemStack",
                new Class<?>[]{ItemStack.class}, item);
    }
}
