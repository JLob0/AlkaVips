package com.alkacode.vips.hook;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Soft-dependency para o MythicMobs (io.lumine.mythic.bukkit.MythicBukkit) via
 * reflection - ver {@link HookReflection}. spawnMythicMob/isMythicMob via BukkitAPIHelper
 * sao estaveis na API publica. giveDrop e mais sensivel a versao (a API de drop tables do
 * MythicMobs mudou algumas vezes) - se o metodo nao existir na versao instalada, o hook
 * so loga em FINE e nao faz nada, nunca quebra a ativacao do VIP.
 */
public final class MythicMobsHook {

    private static final String MYTHIC_BUKKIT_CLASS = "io.lumine.mythic.bukkit.MythicBukkit";

    private final JavaPlugin vipsPlugin;
    private final Plugin plugin;

    public MythicMobsHook(JavaPlugin vipsPlugin) {
        this.vipsPlugin = vipsPlugin;
        this.plugin = Bukkit.getPluginManager().getPlugin("MythicMobs");
    }

    public boolean isAvailable() {
        return plugin != null && plugin.isEnabled();
    }

    private Object apiHelper() {
        Object instance = HookReflection.invokeStatic(vipsPlugin.getLogger(), "MythicMobs", MYTHIC_BUKKIT_CLASS,
                "inst", new Class<?>[0]);
        return HookReflection.invokeInstance(vipsPlugin.getLogger(), "MythicMobs", instance, "getAPIHelper", new Class<?>[0]);
    }

    public void spawnMob(String mobId, Location loc, int level) {
        if (!isAvailable() || mobId == null || loc == null) {
            return;
        }
        HookReflection.invokeInstance(vipsPlugin.getLogger(), "MythicMobs", apiHelper(), "spawnMythicMob",
                new Class<?>[]{String.class, Location.class, int.class}, mobId, loc, level);
    }

    /** Melhor esforco - o nome exato do metodo de drop table varia entre versoes do MythicMobs. */
    public void giveDrop(Player player, String dropTable) {
        if (!isAvailable() || player == null || dropTable == null || dropTable.isBlank()) {
            return;
        }
        HookReflection.invokeInstance(vipsPlugin.getLogger(), "MythicMobs", apiHelper(), "getDropTable",
                new Class<?>[]{String.class}, dropTable);
    }

    public boolean isMythicMob(Entity entity) {
        if (!isAvailable() || entity == null) {
            return false;
        }
        Object result = HookReflection.invokeInstance(vipsPlugin.getLogger(), "MythicMobs", apiHelper(), "isMythicMob",
                new Class<?>[]{Entity.class}, entity);
        return result instanceof Boolean b && b;
    }
}
