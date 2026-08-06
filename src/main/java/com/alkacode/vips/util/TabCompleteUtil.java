package com.alkacode.vips.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public final class TabCompleteUtil {

    private TabCompleteUtil() {
    }

    public static List<String> filter(Iterable<String> options, String prefix) {
        String lower = prefix.toLowerCase();
        return StreamSupport.stream(options.spliterator(), false)
                .filter(option -> option.toLowerCase().startsWith(lower))
                .collect(Collectors.toList());
    }

    public static List<String> onlinePlayerNames(String prefix) {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }
}
