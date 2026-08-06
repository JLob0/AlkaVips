package com.alkacode.vips.hook;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Soft-dependency para o mcMMO (com.gmail.nossr50.api.ExperienceAPI) via reflection -
 * ver {@link HookReflection}. addXP(Player, String, int) e getLevel(Player, String) sao
 * a parte da API publica do mcMMO estavel ha varias major versions; a lista de skills e
 * fixa desde sempre no mcMMO, entao fica hardcoded aqui (nao e dado de config nosso).
 */
public final class McMMOHook {

    private static final String API_CLASS = "com.gmail.nossr50.api.ExperienceAPI";
    private static final String[] SKILLS = {
            "ACROBATICS", "ALCHEMY", "ARCHERY", "AXES", "EXCAVATION", "FISHING",
            "HERBALISM", "MINING", "REPAIR", "SALVAGE", "SMELTING", "SWORDS",
            "TAMING", "UNARMED", "WOODCUTTING"
    };

    private final JavaPlugin vipsPlugin;
    private final Plugin plugin;

    public McMMOHook(JavaPlugin vipsPlugin) {
        this.vipsPlugin = vipsPlugin;
        this.plugin = Bukkit.getPluginManager().getPlugin("mcMMO");
    }

    public boolean isAvailable() {
        return plugin != null && plugin.isEnabled();
    }

    public void addSkillXp(Player player, String skill, int xp) {
        if (!isAvailable() || player == null || skill == null) {
            return;
        }
        HookReflection.invokeStatic(vipsPlugin.getLogger(), "mcMMO", API_CLASS, "addXP",
                new Class<?>[]{Player.class, String.class, int.class}, player, skill, xp);
    }

    public void addSkillXpAll(Player player, int xp) {
        if (!isAvailable()) {
            return;
        }
        for (String skill : SKILLS) {
            addSkillXp(player, skill, xp);
        }
    }

    public int getSkillLevel(Player player, String skill) {
        if (!isAvailable() || player == null || skill == null) {
            return 0;
        }
        Object result = HookReflection.invokeStatic(vipsPlugin.getLogger(), "mcMMO", API_CLASS, "getLevel",
                new Class<?>[]{Player.class, String.class}, player, skill);
        return result instanceof Integer level ? level : 0;
    }

    public boolean hasReachedLevel(Player player, String skill, int level) {
        return getSkillLevel(player, skill) >= level;
    }
}
