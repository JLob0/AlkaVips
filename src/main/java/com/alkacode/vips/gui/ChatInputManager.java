package com.alkacode.vips.gui;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Fila simples de "proxima mensagem de chat vira input" para prompts como o preco de
 * venda no {@link SellKeyMenu} - evita puxar uma lib de anvil-gui/conversationsAPI so
 * para isso.
 */
public final class ChatInputManager {

    private final Map<UUID, Consumer<String>> pending = new ConcurrentHashMap<>();

    public void await(UUID uuid, Consumer<String> callback) {
        pending.put(uuid, callback);
    }

    public boolean isAwaiting(UUID uuid) {
        return pending.containsKey(uuid);
    }

    public void complete(UUID uuid, String input) {
        Consumer<String> callback = pending.remove(uuid);
        if (callback != null) {
            callback.accept(input);
        }
    }

    public void cancel(UUID uuid) {
        pending.remove(uuid);
    }
}
