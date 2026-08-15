package com.alkacode.vips.hook;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

/**
 * Soft-dependency para o AdvancedEnchantments (net.advancedplugins.ae.api.AEAPI) via
 * reflection - ver {@link HookReflection}. Assinaturas confirmadas em 2026-08-13 via
 * javap direto no jar real (AdvancedEnchantments-9.24.1.jar, instalado no servidor de
 * dev) - a versao anterior deste hook ("addEnchant"/"hasEnchant"/"getEnchants") era uma
 * suposicao nunca validada e sempre caia no catch(Throwable), virando no-op silencioso.
 * A API real e 100% estatica (sem singleton/instancia): AEAPI#applyEnchant(String, int,
 * ItemStack) retorna um ItemStack NOVO (nao muta o parametro in-place), por isso todo
 * metodo aqui tambem retorna o resultado em vez de assumir mutacao.
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

    /**
     * Formato esperado no vips.yml: "nome:nivel" (ex: "vampiric:3") ja separado pelo
     * chamador. Retorna o ItemStack resultante (a API do AE nao muta in-place) - o
     * chamador PRECISA usar o retorno, nao o item original, se quiser refletir o
     * encantamento aplicado.
     */
    public ItemStack applyEnchant(ItemStack item, String enchant, int level) {
        if (!isAvailable() || item == null || enchant == null) {
            return item;
        }
        Object result = HookReflection.invokeStatic(vipsPlugin.getLogger(), "AdvancedEnchantments", API_CLASS,
                "applyEnchant", new Class<?>[]{String.class, int.class, ItemStack.class}, enchant, level, item);
        return result instanceof ItemStack stack ? stack : item;
    }

    public boolean hasEnchant(ItemStack item, String enchant) {
        if (!isAvailable() || item == null || enchant == null) {
            return false;
        }
        Object result = HookReflection.invokeStatic(vipsPlugin.getLogger(), "AdvancedEnchantments", API_CLASS,
                "getEnchantLevel", new Class<?>[]{String.class, ItemStack.class}, enchant, item);
        return result instanceof Integer level && level > 0;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Integer> getEnchantments(ItemStack item) {
        if (!isAvailable() || item == null) {
            return Map.of();
        }
        Object result = HookReflection.invokeStatic(vipsPlugin.getLogger(), "AdvancedEnchantments", API_CLASS,
                "getEnchantmentsOnItem", new Class<?>[]{ItemStack.class}, item);
        return result instanceof Map<?, ?> map ? (Map<String, Integer>) map : Map.of();
    }
}
