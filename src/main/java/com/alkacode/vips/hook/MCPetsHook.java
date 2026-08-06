package com.alkacode.vips.hook;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Soft-dependency para o MCPets (br.com.devpaulo.mcpets.api.MCPetsAPI) via reflection -
 * ver {@link HookReflection}. Sem artefato Maven publico confirmado - se os nomes de
 * metodo abaixo nao baterem com a versao instalada, o hook fica inerte silenciosamente.
 */
public final class MCPetsHook {

    private static final String API_CLASS = "br.com.devpaulo.mcpets.api.MCPetsAPI";

    private final JavaPlugin vipsPlugin;
    private final Plugin plugin;

    public MCPetsHook(JavaPlugin vipsPlugin) {
        this.vipsPlugin = vipsPlugin;
        this.plugin = Bukkit.getPluginManager().getPlugin("MCPets");
    }

    public boolean isAvailable() {
        return plugin != null && plugin.isEnabled();
    }

    public void givePet(Player player, String petId) {
        if (!isAvailable() || player == null || petId == null || petId.isBlank()) {
            return;
        }
        HookReflection.invokeStatic(vipsPlugin.getLogger(), "MCPets", API_CLASS, "givePet",
                new Class<?>[]{Player.class, String.class}, player, petId);
    }

    public boolean hasPet(Player player, String petId) {
        if (!isAvailable() || player == null || petId == null) {
            return false;
        }
        Object result = HookReflection.invokeStatic(vipsPlugin.getLogger(), "MCPets", API_CLASS, "hasPet",
                new Class<?>[]{Player.class, String.class}, player, petId);
        return result instanceof Boolean b && b;
    }

    public String getActivePet(Player player) {
        if (!isAvailable() || player == null) {
            return null;
        }
        Object result = HookReflection.invokeStatic(vipsPlugin.getLogger(), "MCPets", API_CLASS, "getActivePet",
                new Class<?>[]{Player.class}, player);
        return result instanceof String s ? s : null;
    }
}
