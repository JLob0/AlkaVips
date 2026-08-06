package com.alkacode.vips.hook;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Chamadas reflexivas compartilhadas pelos hooks deste pacote - nenhum dos 8 plugins de
 * terceiros (mcMMO, MythicMobs, ItemsAdder, AdvancedEnchantments, BattlePass, MCPets,
 * TAB, Citizens) e compileOnly aqui. Varios deles (AdvancedEnchantments, BattlePass,
 * MCPets) nao tem artefato Maven publico confiavel, e travar o build do AlkaVips inteiro
 * por causa de um repo indisponivel e pior do que perder tipagem estatica num hook
 * opcional. Toda falha (classe/metodo ausente, versao incompativel, etc) cai no catch e
 * vira log FINE - nunca propaga.
 */
final class HookReflection {

    private HookReflection() {
    }

    static Object invokeStatic(Logger logger, String hookName, String className, String methodName,
                                Class<?>[] paramTypes, Object... args) {
        try {
            Class<?> clazz = Class.forName(className);
            Method method = clazz.getMethod(methodName, paramTypes);
            return method.invoke(null, args);
        } catch (Throwable t) {
            logger.log(Level.FINE, "Hook " + hookName + " falhou (" + className + "#" + methodName + "): " + t, t);
            return null;
        }
    }

    static Object invokeInstance(Logger logger, String hookName, Object target, String methodName,
                                  Class<?>[] paramTypes, Object... args) {
        if (target == null) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(methodName, paramTypes);
            return method.invoke(target, args);
        } catch (Throwable t) {
            logger.log(Level.FINE, "Hook " + hookName + " falhou (" + methodName + "): " + t, t);
            return null;
        }
    }
}
