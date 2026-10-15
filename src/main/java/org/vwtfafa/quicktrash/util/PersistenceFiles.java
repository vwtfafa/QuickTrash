package org.vwtfafa.quicktrash.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/** Shared helpers for reading and writing the plugin's YAML data files. */
public final class PersistenceFiles {
    private PersistenceFiles() { }

    public static YamlConfiguration loadOrBackup(JavaPlugin plugin, File file) {
        YamlConfiguration config = new YamlConfiguration();
        try {
            // load(File) reports parse errors, unlike loadConfiguration(File), which only logs them.
            config.load(file);
            return config;
        } catch (IOException | InvalidConfigurationException exception) {
            plugin.getLogger().log(Level.SEVERE, "Could not read " + file.getName() + "; starting with empty data", exception);
            moveAside(plugin, file);
            return null;
        }
    }

    public static void save(JavaPlugin plugin, YamlConfiguration config, File file) {
        File temp = new File(file.getParentFile(), file.getName() + ".tmp");
        try {
            config.save(temp);
            Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Could not save " + file.getName(), exception);
        }
    }

    private static void moveAside(JavaPlugin plugin, File file) {
        File backup = new File(file.getParentFile(), file.getName() + ".corrupt");
        try {
            Files.move(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            plugin.getLogger().warning("Moved unreadable " + file.getName() + " to " + backup.getName());
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Could not move aside " + file.getName(), exception);
        }
    }
}
