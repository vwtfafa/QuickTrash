package org.vwtfafa.quicktrash.manager;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.vwtfafa.quicktrash.QuickTrash;
import org.vwtfafa.quicktrash.util.PersistenceFiles;

public final class StatsManager {
    private final QuickTrash plugin;
    private final Map<UUID, Long> players = new HashMap<>();
    private final Object ioLock = new Object();
    private volatile boolean dirty;
    private volatile long totalDeleted;
    private File dataFile;
    private ScheduledTask flushTask;

    public StatsManager(QuickTrash plugin) { this.plugin = plugin; }

    public void load() {
        dataFile = new File(plugin.getDataFolder(), "stats.yml");
        if (!dataFile.exists()) return;
        YamlConfiguration config = PersistenceFiles.loadOrBackup(plugin, dataFile);
        if (config == null) return;
        Object total = config.get("total-deleted");
        if (total instanceof Number number) totalDeleted = number.longValue();
        if (!(config.get("players") instanceof ConfigurationSection section)) return;
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

    public void startFlushTask() {
        if (flushTask == null) {
            flushTask = plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, ignored -> flushIfDirty(), 20L, 20L);
        }
    }

    private void flushAsync() {
        // Clear first so marks during the snapshot are not lost; the next tick flushes again.
        dirty = false;
        YamlConfiguration config = snapshotConfig();
        plugin.getServer().getAsyncScheduler().runNow(plugin, ignored -> writeFile(config));
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
            PersistenceFiles.save(plugin, config, dataFile);
        }
    }

    public void shutdown() {
        if (flushTask != null) {
            flushTask.cancel();
            flushTask = null;
        }
        writeFile(snapshotConfig());
    }
}
