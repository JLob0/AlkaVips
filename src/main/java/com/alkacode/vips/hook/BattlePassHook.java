package com.alkacode.vips.hook;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Soft-dependency para um plugin de BattlePass via reflection - ver {@link HookReflection}.
 * NAO HA CONFIRMACAO do pacote/API real: o pedido original citou
 * "me.hyfe.simplespigot.plugin.SimplePlugin" com a ressalva "verificar pacote real", e o
 * nome de plugin usado em Bukkit.getPluginManager().getPlugin("BattlePass") tambem e uma
 * suposicao. Antes de usar isso em producao, confirme contra o plugin.yml do BattlePass
 * real instalado no servidor (nome do plugin e classe/metodos da API) e ajuste API_CLASS
 * e o nome do plugin abaixo - ate la isAvailable() vai retornar false e o hook fica inerte.
 */
public final class BattlePassHook {

    private static final String API_CLASS = "me.hyfe.simplespigot.plugin.SimplePlugin";

    private final JavaPlugin vipsPlugin;
    private final Plugin plugin;

    public BattlePassHook(JavaPlugin vipsPlugin) {
        this.vipsPlugin = vipsPlugin;
        this.plugin = Bukkit.getPluginManager().getPlugin("BattlePass");
    }

    public boolean isAvailable() {
        return plugin != null && plugin.isEnabled();
    }

    public void addXp(Player player, int xp) {
        if (!isAvailable() || player == null) {
            return;
        }
        HookReflection.invokeStatic(vipsPlugin.getLogger(), "BattlePass", API_CLASS, "addXp",
                new Class<?>[]{Player.class, int.class}, player, xp);
    }

    public int getTier(Player player) {
        if (!isAvailable() || player == null) {
            return 0;
        }
        Object result = HookReflection.invokeStatic(vipsPlugin.getLogger(), "BattlePass", API_CLASS, "getTier",
                new Class<?>[]{Player.class}, player);
        return result instanceof Integer tier ? tier : 0;
    }

    public boolean hasPremium(Player player) {
        if (!isAvailable() || player == null) {
            return false;
        }
        Object result = HookReflection.invokeStatic(vipsPlugin.getLogger(), "BattlePass", API_CLASS, "hasPremium",
                new Class<?>[]{Player.class}, player);
        return result instanceof Boolean b && b;
    }
}
