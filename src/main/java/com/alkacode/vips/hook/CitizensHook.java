package com.alkacode.vips.hook;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Soft-dependency para o Citizens (net.citizensnpcs.api.CitizensAPI) via reflection - ver
 * {@link HookReflection}. createNPC/getNPCRegistry sao API publica estavel; skin via
 * SkinTrait e trait generico via TraitFactory tambem sao publicos, mas mais sensiveis a
 * versao - se algo mudar de nome entre versoes do Citizens, o metodo correspondente so
 * vira no-op.
 */
public final class CitizensHook {

    private static final String CITIZENS_API_CLASS = "net.citizensnpcs.api.CitizensAPI";
    private static final String SKIN_TRAIT_CLASS = "net.citizensnpcs.trait.SkinTrait";

    private final JavaPlugin vipsPlugin;
    private final Plugin plugin;

    public CitizensHook(JavaPlugin vipsPlugin) {
        this.vipsPlugin = vipsPlugin;
        this.plugin = Bukkit.getPluginManager().getPlugin("Citizens");
    }

    public boolean isAvailable() {
        return plugin != null && plugin.isEnabled();
    }

    private Object registry() {
        return HookReflection.invokeStatic(vipsPlugin.getLogger(), "Citizens", CITIZENS_API_CLASS,
                "getNPCRegistry", new Class<?>[0]);
    }

    /** Retorna o id do NPC criado, ou -1 se o Citizens nao estiver disponivel/a chamada falhar. */
    public int createNPC(String name, Location loc, String skin) {
        if (!isAvailable() || name == null || loc == null) {
            return -1;
        }
        Object registry = registry();
        Object npc = HookReflection.invokeInstance(vipsPlugin.getLogger(), "Citizens", registry, "createNPC",
                new Class<?>[]{EntityType.class, String.class}, EntityType.PLAYER, name);
        if (npc == null) {
            return -1;
        }
        HookReflection.invokeInstance(vipsPlugin.getLogger(), "Citizens", npc, "spawn", new Class<?>[]{Location.class}, loc);
        Object id = HookReflection.invokeInstance(vipsPlugin.getLogger(), "Citizens", npc, "getId", new Class<?>[0]);
        if (skin != null && !skin.isBlank()) {
            applySkin(npc, skin);
        }
        return id instanceof Integer i ? i : -1;
    }

    public void setNPCSkin(int npcId, String skin) {
        Object npc = npcById(npcId);
        if (npc != null) {
            applySkin(npc, skin);
        }
    }

    private void applySkin(Object npc, String skin) {
        if (skin == null || skin.isBlank()) {
            return;
        }
        // getOrAddTrait espera a propria Class<? extends Trait> como classe literal, entao
        // isto so busca a classe - nao e uma chamada reflexiva de metodo (HookReflection
        // nao se aplica aqui).
        Class<?> skinTraitClass;
        try {
            skinTraitClass = Class.forName(SKIN_TRAIT_CLASS);
        } catch (Throwable t) {
            vipsPlugin.getLogger().log(java.util.logging.Level.FINE, "Hook Citizens falhou (SkinTrait ausente): " + t, t);
            return;
        }
        Object trait = HookReflection.invokeInstance(vipsPlugin.getLogger(), "Citizens", npc, "getOrAddTrait",
                new Class<?>[]{Class.class}, skinTraitClass);
        HookReflection.invokeInstance(vipsPlugin.getLogger(), "Citizens", trait, "setSkinName",
                new Class<?>[]{String.class}, skin);
    }

    public void addTrait(int npcId, String trait) {
        if (!isAvailable() || trait == null || trait.isBlank()) {
            return;
        }
        Object npc = npcById(npcId);
        if (npc == null) {
            return;
        }
        Object traitFactory = HookReflection.invokeStatic(vipsPlugin.getLogger(), "Citizens", CITIZENS_API_CLASS,
                "getTraitFactory", new Class<?>[0]);
        Object traitClass = HookReflection.invokeInstance(vipsPlugin.getLogger(), "Citizens", traitFactory,
                "getTraitClass", new Class<?>[]{String.class}, trait);
        if (traitClass instanceof Class<?> clazz) {
            HookReflection.invokeInstance(vipsPlugin.getLogger(), "Citizens", npc, "addTrait", new Class<?>[]{Class.class}, clazz);
        }
    }

    public void removeNPC(int npcId) {
        Object npc = npcById(npcId);
        if (npc != null) {
            HookReflection.invokeInstance(vipsPlugin.getLogger(), "Citizens", npc, "destroy", new Class<?>[0]);
        }
    }

    private Object npcById(int npcId) {
        if (!isAvailable()) {
            return null;
        }
        return HookReflection.invokeInstance(vipsPlugin.getLogger(), "Citizens", registry(), "getById",
                new Class<?>[]{int.class}, npcId);
    }
}
