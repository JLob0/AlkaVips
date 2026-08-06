package com.alkacode.vips.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class TextUtil {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private TextUtil() {
    }

    public static Component parse(String raw) {
        return MINI_MESSAGE.deserialize(raw == null ? "" : raw).decoration(TextDecoration.ITALIC, false);
    }

    public static Component parse(String raw, Map<String, String> placeholders) {
        return parse(replace(raw, placeholders));
    }

    public static List<Component> parseList(List<String> raws, Map<String, String> placeholders) {
        if (raws == null) {
            return List.of();
        }
        return raws.stream().map(raw -> parse(raw, placeholders)).collect(Collectors.toList());
    }

    public static String replace(String raw, Map<String, String> placeholders) {
        String replaced = raw == null ? "" : raw;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            replaced = replaced.replace("<" + entry.getKey() + ">", entry.getValue());
        }
        return replaced;
    }

    public static String plain(String raw) {
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(parse(raw));
    }
}
