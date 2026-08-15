package com.alkacode.vips.util;

import com.alkacode.core.hooks.LuckPermsHook;
import com.alkacode.core.util.PermissionLoreUtil;
import com.alkacode.core.util.PermissionNamesStore;
import com.alkacode.vips.config.ConfigManager;
import com.alkacode.vips.model.VipType;

import java.util.List;

/** Linhas de lore "permissoes deste vip" ja resolvidas com nomes amigaveis - usado tanto
 * por VipsServices (menus construidos depois do enable) quanto por KeyManager (constroi o
 * item fisico da key, criado ANTES de VipsServices existir - sem isso teria dependencia
 * circular). Vazio se o tier nao tem permission-group configurado em vips.yml. */
public final class VipPermissionLore {
    private VipPermissionLore() {}

    public static List<String> forType(VipType type, ConfigManager configManager, PermissionNamesStore permissionNames) {
        String group = type.permissionGroup();
        if (group == null || group.isBlank()) {
            return List.of();
        }
        String template = configManager.config().getString("permission-lore.line-format", " <gray>- <white>%permission%");
        List<String> keys = LuckPermsHook.getGroupPermissionKeys(group);
        return PermissionLoreUtil.expand(keys, template, permissionNames::lookup, permissionNames::registerUnknown);
    }
}
