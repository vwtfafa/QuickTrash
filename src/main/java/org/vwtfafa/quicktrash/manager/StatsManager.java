package org.vwtfafa.quicktrash.manager;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.vwtfafa.quicktrash.QuickTrash;
import org.vwtfafa.quicktrash.util.PersistenceFiles;

public final class StatsManager {
    private static final long FLUSH_INTERVAL_TICKS = 20L * 30L;
    private static final long PRUNE_AFTER_MS = 1000L * 60L * 60L * 24L * 30L;

    private final QuickTrash plugin;
    private final Map<UUID, Long> players = new HashMap<>();
    private final Map<UUID, Long> lastSeen = new HashMap<>();
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
                UUID id = UUID.fromString(key);
                players.put(id, section.getLong(key));
                lastSeen.put(id, config.getLong("last-seen." + key, System.currentTimeMillis()));
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("Ignoring invalid stats entry: " + key);
            }
        }
    }

    public void add(UUID playerId, int amount) {
        if (amount <= 0) return;
        players.merge(playerId, (long) amount, Long::sum);
        lastSeen.put(playerId, System.currentTimeMillis());
        totalDeleted += amount;
        dirty = true;
    }

    public long playerTotal(UUID playerId) { return players.getOrDefault(playerId, 0L); }
    public long total() { return totalDeleted; }

    public void flushIfDirty() {
        if (!dirty) return;
        prune();
        flushAsync();
    }

    public void startFlushTask() {
        if (flushTask == null) {
            flushTask = plugin.getServer().getGlobalRegionScheduler()
                .runAtFixedRate(plugin, ignored -> flushIfDirty(), FLUSH_INTERVAL_TICKS, FLUSH_INTERVAL_TICKS);
        }
    }

    private void prune() {
        long cutoff = System.currentTimeMillis() - PRUNE_AFTER_MS;
        Iterator<Map.Entry<UUID, Long>> iterator = lastSeen.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            if (entry.getValue() >= cutoff) continue;
            if (Bukkit.getPlayer(entry.getKey()) != null) continue;
            players.remove(entry.getKey());
            iterator.remove();
            dirty = true;
        }
    }

    private void flushAsync() {
        dirty = false;
        YamlConfiguration config = snapshotConfig();
        plugin.getServer().getAsyncScheduler().runNow(plugin, ignored -> writeFile(config));
    }

    private YamlConfiguration snapshotConfig() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("total-deleted", totalDeleted);
        for (Map.Entry<UUID, Long> entry : players.entrySet()) {
            config.set("players." + entry.getKey(), entry.getValue());
            Long seen = lastSeen.get(entry.getKey());
            if (seen != null) config.set("last-seen." + entry.getKey(), seen);
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
