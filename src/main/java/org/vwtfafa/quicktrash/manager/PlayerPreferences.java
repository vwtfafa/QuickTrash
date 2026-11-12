package org.vwtfafa.quicktrash.manager;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.vwtfafa.quicktrash.QuickTrash;
import org.vwtfafa.quicktrash.util.PersistenceFiles;

/** Per-player preferences such as valuable-confirm dialogs. */
public final class PlayerPreferences {
    private final QuickTrash plugin;
    private final Map<UUID, Boolean> confirmEnabled = new HashMap<>();
    private final Object ioLock = new Object();
    private File dataFile;
    private volatile boolean dirty;

    public PlayerPreferences(QuickTrash plugin) { this.plugin = plugin; }

    public void load() {
        dataFile = new File(plugin.getDataFolder(), "player-prefs.yml");
        confirmEnabled.clear();
        if (!dataFile.exists()) return;
        YamlConfiguration config = PersistenceFiles.loadOrBackup(plugin, dataFile);
        if (config == null || !(config.get("players") instanceof ConfigurationSection root)) return;
        for (String key : root.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                if (root.contains(key + ".confirm")) {
                    confirmEnabled.put(id, root.getBoolean(key + ".confirm"));
                }
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("Ignoring invalid player preference: " + key);
            }
        }
    }

    /** Defaults to true when unset — valuable dialogs stay on until the player opts out. */
    public boolean isConfirmEnabled(UUID playerId) {
        return confirmEnabled.getOrDefault(playerId, true);
    }

    public void setConfirmEnabled(UUID playerId, boolean enabled) {
        confirmEnabled.put(playerId, enabled);
        dirty = true;
        flush();
    }

    public void flush() {
        if (!dirty || dataFile == null) return;
        dirty = false;
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, Boolean> entry : confirmEnabled.entrySet()) {
            config.set("players." + entry.getKey() + ".confirm", entry.getValue());
        }
        synchronized (ioLock) {
            PersistenceFiles.save(plugin, config, dataFile);
        }
    }

    public void shutdown() { flush(); }
}
