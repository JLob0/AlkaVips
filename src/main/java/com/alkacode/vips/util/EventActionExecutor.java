package com.alkacode.vips.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Dispara acoes tipadas configuraveis em vips.yml (secao {@code events:} por tier -
 * on-activate/on-expire/on-enable/on-disable). Formato de cada linha:
 * {@code [tipo] conteudo}, tipo em maiusculo ou minusculo. Tipos suportados:
 * {@code [console]} (comando via console), {@code [player]} (comando como o proprio
 * jogador, so funciona se ele estiver online), {@code [title]} (titulo, usa
 * {@code <newline>} pra separar subtitulo), {@code [actionbar]}, {@code [chat]}
 * (mensagem direta, nao broadcast), {@code [sound]} (nome do enum Sound).
 * Substitui esse padrao inline de custom-commands por um generico reutilizavel entre
 * ActivationService/ExpirationService/futuras features, em vez de duplicar o mesmo
 * parsing em cada lugar.
 */
public final class EventActionExecutor {

    private EventActionExecutor() {
    }

    /** Roda todas as acoes pro jogador ONLINE - usado quando ha certeza de que esta conectado (ex: ativacao). */
    public static void run(List<String> actions, Player player, Map<String, String> placeholders) {
        if (actions == null || actions.isEmpty()) {
            return;
        }
        for (String raw : actions) {
            runOne(raw, player, placeholders);
        }
    }

    /** Roda so as acoes seguras pra alvo OFFLINE ([console]) - [player]/[title]/[actionbar]/[chat]/[sound] sao ignoradas silenciosamente. */
    public static void runOffline(List<String> actions, UUID uuid, String playerName, Map<String, String> placeholders) {
        if (actions == null || actions.isEmpty()) {
            return;
        }
        for (String raw : actions) {
            String[] parsed = parse(raw);
            if (parsed == null) {
                continue;
            }
            if (parsed[0].equals("console")) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), CommandUtil.substitute(parsed[1], placeholders));
            }
        }
    }

    private static void runOne(String raw, Player player, Map<String, String> placeholders) {
        String[] parsed = parse(raw);
        if (parsed == null) {
            return;
        }
        String type = parsed[0];
        String content = CommandUtil.substitute(parsed[1], placeholders);

        switch (type) {
            case "console" -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), content);
            case "player" -> {
                if (player.isOnline()) {
                    player.performCommand(content);
                }
            }
            case "title" -> {
                String[] lines = content.split("<newline>", 2);
                var title = net.kyori.adventure.title.Title.title(
                        TextUtil.parse(lines[0]),
                        TextUtil.parse(lines.length > 1 ? lines[1] : ""));
                player.showTitle(title);
            }
            case "actionbar" -> player.sendActionBar(TextUtil.parse(content));
            case "chat" -> player.sendMessage(TextUtil.legacyParse(content));
            case "sound" -> {
                try {
                    org.bukkit.Sound sound = org.bukkit.Sound.valueOf(content.toUpperCase(java.util.Locale.ROOT));
                    player.playSound(player.getLocation(), sound, 1f, 1f);
                } catch (IllegalArgumentException ignored) {
                    // som invalido no vips.yml - ignora silenciosamente
                }
            }
            default -> {
                // tipo desconhecido - ignora silenciosamente, nao trava o resto das acoes
            }
        }
    }

    /** Retorna {tipo_minusculo, conteudo} ou null se a linha nao seguir o formato "[tipo] conteudo". */
    private static String[] parse(String raw) {
        if (raw == null || !raw.startsWith("[")) {
            return null;
        }
        int close = raw.indexOf(']');
        if (close < 0) {
            return null;
        }
        String type = raw.substring(1, close).trim().toLowerCase(java.util.Locale.ROOT);
        String content = raw.substring(close + 1).trim();
        return new String[]{type, content};
    }
}
