package com.alkacode.vips.hook;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Ponte com o {@code AlkaItemsAPI} (itens de recompensa de VIP) via ServicesManager +
 * reflexao - mesmo padrao do {@link AlkaFlairHook}. AlkaItems e softdepend puro: sem
 * ele instalado, {@code item-rewards:} em vips.yml simplesmente nao entrega nada, o
 * resto do fluxo de ativacao de VIP continua normal (nunca vira hard-depend).
 */
public final class AlkaItemsHook {

    private static final String API_CLASS = "com.alkacode.items.api.AlkaItemsAPI";

    private final Logger logger;
    private Object api;
    private Method giveItemMethod;
    private Method hasItemMethod;

    public AlkaItemsHook(Logger logger) {
        this.logger = logger;
        resolve();
    }

    private void resolve() {
        try {
            Class<?> apiClass = Class.forName(API_CLASS);
            RegisteredServiceProvider<?> registration = Bukkit.getServicesManager().getRegistration(apiClass);
            if (registration == null) {
                return;
            }
            api = registration.getProvider();
            giveItemMethod = apiClass.getMethod("giveItem", Player.class, String.class, int.class);
            hasItemMethod = apiClass.getMethod("hasItem", Player.class, String.class);
        } catch (Throwable t) {
            logger.log(Level.FINE, "AlkaItems nao encontrado ou API incompativel - item-rewards de VIP ficam no-op.", t);
        }
    }

    public boolean isAvailable() {
        return api != null;
    }

    /** So concede se o jogador AINDA NAO tiver uma instancia desse template (evita duplicar
     * recompensa soulbound em reativacoes/upgrades que passem pelo mesmo tier de novo). */
    public void giveIfMissing(Player player, String templateId) {
        if (!isAvailable() || templateId == null || templateId.isBlank()) {
            return;
        }
        try {
            Object hasItem = hasItemMethod.invoke(api, player, templateId);
            if (hasItem instanceof Boolean already && already) {
                return;
            }
            giveItemMethod.invoke(api, player, templateId, 1);
        } catch (Throwable t) {
            logger.log(Level.FINE, "Falha ao conceder item de recompensa '" + templateId + "' via AlkaItems.", t);
        }
    }
}
