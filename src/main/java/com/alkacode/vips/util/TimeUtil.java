package com.alkacode.vips.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TimeUtil {

    private static final Pattern UNIT_PATTERN = Pattern.compile("(\\d+)([smhdw])", Pattern.CASE_INSENSITIVE);

    private TimeUtil() {
    }

    /**
     * "1d12h30m" -> millis. "0", "perm" e "permanent" retornam 0 (VIP sem expiracao).
     * Retorna -1 se a string nao puder ser interpretada.
     */
    public static long parseDuration(String raw) {
        if (raw == null || raw.isBlank()) {
            return -1;
        }
        String trimmed = raw.trim().toLowerCase();
        if (trimmed.equals("0") || trimmed.equals("perm") || trimmed.equals("permanent")) {
            return 0;
        }
        Matcher matcher = UNIT_PATTERN.matcher(trimmed);
        long totalMillis = 0;
        boolean matchedAny = false;
        while (matcher.find()) {
            matchedAny = true;
            long value = Long.parseLong(matcher.group(1));
            totalMillis += switch (matcher.group(2)) {
                case "s" -> value * 1000L;
                case "m" -> value * 60_000L;
                case "h" -> value * 3_600_000L;
                case "d" -> value * 86_400_000L;
                case "w" -> value * 604_800_000L;
                default -> 0L;
            };
        }
        return matchedAny ? totalMillis : -1;
    }

    public static String formatRemaining(long millis) {
        if (millis <= 0) {
            return "0s";
        }
        long days = millis / 86_400_000L;
        long hours = (millis % 86_400_000L) / 3_600_000L;
        long minutes = (millis % 3_600_000L) / 60_000L;
        long seconds = (millis % 60_000L) / 1000L;

        StringBuilder builder = new StringBuilder();
        if (days > 0) builder.append(days).append("d ");
        if (hours > 0) builder.append(hours).append("h ");
        if (minutes > 0) builder.append(minutes).append("m ");
        if (days == 0 && hours == 0) builder.append(seconds).append("s");
        return builder.toString().trim();
    }

    /**
     * LuckPerms aceita duracao com uma unica unidade em segundos ("864000s") - evita
     * qualquer ambiguidade entre "m" (minuto) e "mo" (mes) na hora de montar o comando
     * addtemp a partir de um valor em millis.
     */
    public static String toLuckPermsDuration(long millis) {
        return Math.max(1, millis / 1000L) + "s";
    }

    public static String formatRemainingShort(long millis) {
        if (millis <= 0) {
            return "0s";
        }
        long days = millis / 86_400_000L;
        long hours = (millis % 86_400_000L) / 3_600_000L;
        if (days > 0) {
            return days + "d " + hours + "h";
        }
        long minutes = (millis % 3_600_000L) / 60_000L;
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        long seconds = (millis % 60_000L) / 1000L;
        return minutes + "m " + seconds + "s";
    }
}
