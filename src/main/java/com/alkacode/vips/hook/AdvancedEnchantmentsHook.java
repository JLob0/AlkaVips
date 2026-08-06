package com.alkacode.vips.hook;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

/**
 * Soft-dependency para o AdvancedEnchantments (net.advancedplugins.ae.api.AEAPI) via
 * reflection - ver {@link HookReflection}. AE e um plugin premium (Polymart/BuiltByBit)
 * sem artefato Maven publico e sem documentacao de API aberta, entao os nomes de metodo
 * abaixo (addEnchant/hasEnchant/getEnchants) sao a melhor suposicao com base no padrao
 * usual desse tipo de plugin - CONFIRME contra a versao do AE instalada no servidor antes
 * de depender disso em producao. Se o nome nao bater, o hook so vira no-op silencioso.
 */
public final class AdvancedEnchantmentsHook {

    private static final String API_CLASS = "net.advancedplugins.ae.api.AEAPI";

    private final JavaPlugin vipsPlugin;
    private final Plugin plugin;

    public AdvancedEnchantmentsHook(JavaPlugin vipsPlugin) {
        this.vipsPlugin = vipsPlugin;
        this.plugin = Bukkit.getPluginManager().getPlugin("AdvancedEnchantments");
    }

    public boolean isAvailable() {
        return plugin != null && plugin.isEnabled();
    }

    /** Formato esperado no vips.yml: "nome:nivel" (ex: "vampiric:3") ja separado pelo chamador. */
    public void applyEnchant(ItemStack item, String enchant, int level) {
        if (!isAvailable() || item == null || enchant == null) {
            return;
        }
        HookReflection.invokeStatic(vipsPlugin.getLogger(), "AdvancedEnchantments", API_CLASS, "addEnchant",
                new Class<?>[]{ItemStack.class, String.class, int.class}, item, enchant, level);
    }

    public boolean hasEnchant(ItemStack item, String enchant) {
        if (!isAvailable() || item == null || enchant == null) {
            return false;
        }
        Object result = HookReflection.invokeStatic(vipsPlugin.getLogger(), "AdvancedEnchantments", API_CLASS,
                "hasEnchant", new Class<?>[]{ItemStack.class, String.class}, item, enchant);
        return result instanceof Boolean b && b;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Integer> getEnchantments(ItemStack item) {
        if (!isAvailable() || item == null) {
            return Map.of();
        }
        Object result = HookReflection.invokeStatic(vipsPlugin.getLogger(), "AdvancedEnchantments", API_CLASS,
                "getEnchants", new Class<?>[]{ItemStack.class}, item);
        return result instanceof Map<?, ?> map ? (Map<String, Integer>) map : Map.of();
    }
}
