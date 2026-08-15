package com.alkacode.vips.hook;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Ponte com o {@code AlkaFlairAPI} (tags/medalhas) via ServicesManager + reflexao -
 * mesmo padrao do {@code TimeHook} do AlkaRankUp pro AlkaTimeAPI. AlkaFlair e
 * softdepend puro: sem ele instalado, toda recompensa de tag/medalha (bonus de
 * boost, conquista da carteira, indicacao) vira no-op silencioso, nunca quebra a
 * feature principal.
 */
public final class AlkaFlairHook {

    private static final String API_CLASS = "com.alkacode.flair.api.AlkaFlairAPI";

    private final Logger logger;
    private Object api;
    private Method addTagMethod;
    private Method addMedalMethod;

    public AlkaFlairHook(Logger logger) {
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
            addTagMethod = apiClass.getMethod("addTag", UUID.class, String.class);
            addMedalMethod = apiClass.getMethod("addMedal", UUID.class, String.class);
        } catch (Throwable t) {
            logger.log(Level.FINE, "AlkaFlair nao encontrado ou API incompativel - hooks de tag/medalha ficam no-op.", t);
        }
    }

    public boolean isAvailable() {
        return api != null;
    }

    public void addTag(UUID uuid, String tagId) {
        if (!isAvailable() || tagId == null || tagId.isBlank()) {
            return;
        }
        try {
            addTagMethod.invoke(api, uuid, tagId);
        } catch (Throwable t) {
            logger.log(Level.FINE, "Falha ao conceder tag '" + tagId + "' via AlkaFlair.", t);
        }
    }

    public void addMedal(UUID uuid, String medalId) {
        if (!isAvailable() || medalId == null || medalId.isBlank()) {
            return;
        }
        try {
            addMedalMethod.invoke(api, uuid, medalId);
        } catch (Throwable t) {
            logger.log(Level.FINE, "Falha ao conceder medalha '" + medalId + "' via AlkaFlair.", t);
        }
    }
}
