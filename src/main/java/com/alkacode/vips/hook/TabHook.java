package com.alkacode.vips.hook;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Soft-dependency para o TAB (me.neznamy.tab.api.TabAPI) via reflection - ver
 * {@link HookReflection}. Property IDs ("tagprefix"/"tagsuffix"/"customtagname") sao os
 * documentados publicamente pelo TAB.
 *
 * <p>IMPORTANTE: o prefixo do jogador ja e gerenciado pelo LuckPerms (group-command do
 * VipType em vips.yml) - este hook existe pra permitir um prefixo DIFERENTE do LuckPerms
 * caso um dia seja necessario, mas NAO e chamado por nenhum fluxo de ativacao/troca de VIP
 * hoje. Nao adicione chamadas daqui em ActivationService/PlayerVipManager sem avaliar
 * antes se isso nao vai duplicar/conflitar com o que o LuckPerms ja faz.</p>
 */
public final class TabHook {

    private static final String API_CLASS = "me.neznamy.tab.api.TabAPI";

    private final JavaPlugin vipsPlugin;
    private final Plugin plugin;

    public TabHook(JavaPlugin vipsPlugin) {
        this.vipsPlugin = vipsPlugin;
        this.plugin = Bukkit.getPluginManager().getPlugin("TAB");
    }

    public boolean isAvailable() {
        return plugin != null && plugin.isEnabled();
    }

    public void setPrefix(Player player, String prefix) {
        setProperty(player, "tagprefix", prefix);
    }

    public void setSuffix(Player player, String suffix) {
        setProperty(player, "tagsuffix", suffix);
    }

    public void setNameTag(Player player, String nametag) {
        setProperty(player, "customtagname", nametag);
    }

    private void setProperty(Player player, String propertyName, String value) {
        if (!isAvailable() || player == null || value == null) {
            return;
        }
        Object api = HookReflection.invokeStatic(vipsPlugin.getLogger(), "TAB", API_CLASS, "getInstance", new Class<?>[0]);
        Object tabPlayer = HookReflection.invokeInstance(vipsPlugin.getLogger(), "TAB", api, "getPlayer",
                new Class<?>[]{java.util.UUID.class}, player.getUniqueId());
        Object property = HookReflection.invokeInstance(vipsPlugin.getLogger(), "TAB", tabPlayer, "getProperty",
                new Class<?>[]{String.class}, propertyName);
        HookReflection.invokeInstance(vipsPlugin.getLogger(), "TAB", property, "setTemporaryValue",
                new Class<?>[]{String.class}, value);
    }
}
