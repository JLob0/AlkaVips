package com.alkacode.vips.hook;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Agrega os 9 hooks opcionais de plugins de terceiros do AlkaVips - cada um e uma
 * soft-dependency via reflection (ver {@link HookReflection}), nunca lanca excecao se o
 * plugin correspondente estiver ausente/desabilitado.
 */
public final class HookManager {

    private final McMMOHook mcmmo;
    private final MythicMobsHook mythicMobs;
    private final ItemsAdderHook itemsAdder;
    private final AdvancedEnchantmentsHook advancedEnchantments;
    private final BattlePassHook battlePass;
    private final MCPetsHook mcPets;
    private final TabHook tab;
    private final CitizensHook citizens;
    private final NChatHook nChat;

    public HookManager(JavaPlugin plugin) {
        this.mcmmo = new McMMOHook(plugin);
        this.mythicMobs = new MythicMobsHook(plugin);
        this.itemsAdder = new ItemsAdderHook(plugin);
        this.advancedEnchantments = new AdvancedEnchantmentsHook(plugin);
        this.battlePass = new BattlePassHook(plugin);
        this.mcPets = new MCPetsHook(plugin);
        this.tab = new TabHook(plugin);
        this.citizens = new CitizensHook(plugin);
        this.nChat = new NChatHook(plugin);
    }

    public McMMOHook mcmmo() { return mcmmo; }
    public MythicMobsHook mythicMobs() { return mythicMobs; }
    public ItemsAdderHook itemsAdder() { return itemsAdder; }
    public AdvancedEnchantmentsHook advancedEnchantments() { return advancedEnchantments; }
    public BattlePassHook battlePass() { return battlePass; }
    public MCPetsHook mcPets() { return mcPets; }
    public TabHook tab() { return tab; }
    public CitizensHook citizens() { return citizens; }
    public NChatHook nChat() { return nChat; }
}
