package com.alkacode.vips.listener;

import com.alkacode.vips.gui.ChatInputManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class ChatInputListener implements Listener {

    private final JavaPlugin plugin;
    private final ChatInputManager chatInputManager;

    public ChatInputListener(JavaPlugin plugin, ChatInputManager chatInputManager) {
        this.plugin = plugin;
        this.chatInputManager = chatInputManager;
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        var uuid = event.getPlayer().getUniqueId();
        if (!chatInputManager.isAwaiting(uuid)) {
            return;
        }
        event.setCancelled(true);
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        Bukkit.getScheduler().runTask(plugin, () -> chatInputManager.complete(uuid, message));
    }
}
