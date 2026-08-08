package com.alkacode.vips.hook;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Soft-dependency para o nChat - so deteccao de presenca por enquanto, sem chamada de
 * API nenhuma. nChat le prefixo/sufixo direto do LuckPerms (mesmo grupo que o
 * group-command do VipType em vips.yml ja aplica), entao nao ha nada a fazer aqui
 * hoje - a tag do jogador no chat ja reflete o VIP ativo sem nenhum codigo adicional,
 * desde que o prefixo do grupo no LuckPerms esteja formatado com MiniMessage/hex
 * (configuracao do LuckPerms, nao deste plugin). Existe como classe pra ja ter um
 * lugar certo pra crescer se o nChat expuser uma API de tags/badges dinamicas no
 * futuro - nChat e infraestrutura permanente da rede, nao um plugin ocasional.
 */
public final class NChatHook {

    private final Plugin plugin;

    public NChatHook(JavaPlugin vipsPlugin) {
        this.plugin = Bukkit.getPluginManager().getPlugin("nChat");
    }

    public boolean isAvailable() {
        return plugin != null && plugin.isEnabled();
    }
}
