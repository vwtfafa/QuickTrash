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
        defaults.set("config-version", 3);
        defaults.set("gui.title", "<dark_gray>QuickTrash");
        defaults.set("trash.auto-clear-seconds", 30);
        defaults.set("trash.undo.enabled", true);
        defaults.set("staff-recycle.enabled", true);
        defaults.set("messages.confirmation-confirm", "<red>Confirm");
        config.setDefaults(defaults);

        assertTrue(ConfigMigrator.migrate(config, defaults));
        assertEquals("<gold>My Trash", config.getString("gui.title"));
        assertEquals(12, config.getInt("trash.auto-clear-seconds"));
        assertEquals("<red>Confirm", config.getString("messages.confirmation-confirm"));
        assertTrue(config.getBoolean("trash.undo.enabled"));
        assertTrue(config.getBoolean("staff-recycle.enabled"));
        assertEquals(3, config.getInt("config-version"));
    }

    @Test
    void doesNotRerunMigrationForCurrentConfigVersion() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("config-version", 3);
        YamlConfiguration defaults = new YamlConfiguration();
        defaults.set("config-version", 3);
        defaults.set("messages.new-key", "Default");

        assertFalse(ConfigMigrator.migrate(config, defaults));
        assertFalse(config.contains("messages.new-key", true));
    }
}