package org.vwtfafa.quicktrash.manager;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.vwtfafa.quicktrash.QuickTrash;

public final class StatsManager {
    private final QuickTrash plugin;
    private final Map<UUID, Long> players = new HashMap<>();
    private final Object ioLock = new Object();
    private volatile boolean dirty;
    private long totalDeleted;
    private File dataFile;

    public StatsManager(QuickTrash plugin) { this.plugin = plugin; }

    public void load() {
        dataFile = new File(plugin.getDataFolder(), "stats.yml");
        if (!dataFile.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        totalDeleted = config.getLong("total-deleted", 0);
        ConfigurationSection section = config.getConfigurationSection("players");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            try {
                players.put(UUID.fromString(key), section.getLong(key));
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("Ignoring invalid stats entry: " + key);
            }
        }
    }

    public void add(UUID playerId, int amount) {
        if (amount <= 0) return;
        players.merge(playerId, (long) amount, Long::sum);
        totalDeleted += amount;
        dirty = true;
    }

    public long playerTotal(UUID playerId) { return players.getOrDefault(playerId, 0L); }
    public long total() { return totalDeleted; }

    public void flushIfDirty() { if (dirty) flushAsync(); }

    private void flushAsync() {
        YamlConfiguration config = snapshotConfig();
        dirty = false;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> writeFile(config));
    }

    private YamlConfiguration snapshotConfig() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("total-deleted", totalDeleted);
        for (Map.Entry<UUID, Long> entry : players.entrySet()) {
            config.set("players." + entry.getKey(), entry.getValue());
        }
        return config;
    }

    private void writeFile(YamlConfiguration config) {
        if (dataFile == null) return;
        synchronized (ioLock) {
            try { config.save(dataFile); }
            catch (IOException exception) { plugin.getLogger().log(java.util.logging.Level.SEVERE, "Could not save stats", exception); }
        }
    }

    public void shutdown() { writeFile(snapshotConfig()); }
}
