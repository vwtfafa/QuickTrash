package org.vwtfafa.quicktrash.util;

import org.bukkit.configuration.ConfigurationSection;

public final class ConfigMigrator {
    private static final String VERSION_PATH = "config-version";

    private ConfigMigrator() { }

    public static boolean migrate(ConfigurationSection config, ConfigurationSection defaults) {
        if (defaults == null) return false;
        int currentVersion = config.getInt(VERSION_PATH, 0);
        int targetVersion = defaults.getInt(VERSION_PATH, 0);
        if (currentVersion >= targetVersion) return false;

        for (String path : defaults.getKeys(true)) {
            if (VERSION_PATH.equals(path) || defaults.isConfigurationSection(path)) continue;
            if (!config.contains(path, true)) config.set(path, defaults.get(path));
        }
        config.set(VERSION_PATH, targetVersion);
        return true;
    }
}