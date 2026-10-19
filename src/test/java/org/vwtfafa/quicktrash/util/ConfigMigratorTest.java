package org.vwtfafa.quicktrash.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class ConfigMigratorTest {
    @Test
    void addsMissingDefaultsAndPreservesUserValues() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("gui.title", "<gold>My Trash");
        config.set("trash.auto-clear-seconds", 12);
        YamlConfiguration defaults = new YamlConfiguration();
        defaults.set("config-version", 1);
        defaults.set("gui.title", "<dark_gray>QuickTrash");
        defaults.set("trash.auto-clear-seconds", 30);
        defaults.set("messages.confirmation-confirm", "<red>Confirm");
        config.setDefaults(defaults);

        assertTrue(ConfigMigrator.migrate(config, defaults));
        assertEquals("<gold>My Trash", config.getString("gui.title"));
        assertEquals(12, config.getInt("trash.auto-clear-seconds"));
        assertEquals("<red>Confirm", config.getString("messages.confirmation-confirm"));
        assertEquals(1, config.getInt("config-version"));
    }

    @Test
    void doesNotRerunMigrationForCurrentConfigVersion() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("config-version", 1);
        YamlConfiguration defaults = new YamlConfiguration();
        defaults.set("config-version", 1);
        defaults.set("messages.new-key", "Default");

        assertFalse(ConfigMigrator.migrate(config, defaults));
        assertFalse(config.contains("messages.new-key", true));
    }
}