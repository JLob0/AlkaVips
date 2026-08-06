package com.alkacode.vips.util;

import java.util.Map;

/**
 * Templates de comando (group-command, activation-commands, upgrade-commands,
 * reward-commands) usam placeholders com chaves ({@code {player}}, {@code {time}}) e
 * nunca passam pelo MiniMessage - por isso a substituicao e separada de
 * {@link TextUtil#replace}, que usa {@code <chave>} para nao colidir com tags de
 * mensagens/lore.
 */
public final class CommandUtil {

    private CommandUtil() {
    }

    public static String substitute(String raw, Map<String, String> placeholders) {
        String result = raw == null ? "" : raw;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }
}
