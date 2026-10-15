package org.vwtfafa.quicktrash.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Files;
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PersistenceFilesTest {
    @TempDir
    File directory;

    private JavaPlugin plugin() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        return plugin;
    }

    @Test
    void saveWritesFileWithoutLeftoverTemp() {
        File target = new File(directory, "data.yml");
        YamlConfiguration config = new YamlConfiguration();
        config.set("total-deleted", 5);

        PersistenceFiles.save(plugin(), config, target);

        assertTrue(target.isFile());
        assertFalse(new File(directory, "data.yml.tmp").exists());
        assertEquals(5, YamlConfiguration.loadConfiguration(target).getInt("total-deleted"));
    }

    @Test
    void saveReplacesExistingContent() throws Exception {
        File target = new File(directory, "data.yml");
        Files.writeString(target.toPath(), "total-deleted: 1\n");
        YamlConfiguration config = new YamlConfiguration();
        config.set("total-deleted", 9);

        PersistenceFiles.save(plugin(), config, target);

        assertEquals(9, YamlConfiguration.loadConfiguration(target).getInt("total-deleted"));
    }

    @Test
    void loadOrBackupReturnsNullAndBacksUpBrokenFile() throws Exception {
        File target = new File(directory, "data.yml");
        Files.writeString(target.toPath(), "sessions: [unclosed\n");

        YamlConfiguration config = PersistenceFiles.loadOrBackup(plugin(), target);

        assertNull(config);
        assertFalse(target.exists());
        assertTrue(new File(directory, "data.yml.corrupt").isFile());
    }

    @Test
    void loadOrBackupReadsValidFile() {
        File target = new File(directory, "data.yml");
        YamlConfiguration written = new YamlConfiguration();
        written.set("total-deleted", 3);
        PersistenceFiles.save(plugin(), written, target);

        YamlConfiguration loaded = PersistenceFiles.loadOrBackup(plugin(), target);

        assertNotNull(loaded);
        assertEquals(3, loaded.getInt("total-deleted"));
    }
}
