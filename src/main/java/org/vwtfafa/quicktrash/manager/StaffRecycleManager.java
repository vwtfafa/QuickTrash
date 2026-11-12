package org.vwtfafa.quicktrash.manager;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.vwtfafa.quicktrash.QuickTrash;
import org.vwtfafa.quicktrash.model.RecycleEntry;
import org.vwtfafa.quicktrash.util.PersistenceFiles;

/** Longer-lived staff history of valuable deletes for inspect/restore. */
public final class StaffRecycleManager {
    private final QuickTrash plugin;
    private final Deque<RecycleEntry> entries = new ArrayDeque<>();
    private final Object ioLock = new Object();
    private File dataFile;
    private long nextId = 1L;
    private volatile boolean dirty;

    public StaffRecycleManager(QuickTrash plugin) { this.plugin = plugin; }

    public void load() {
        dataFile = new File(plugin.getDataFolder(), "staff-recycle.yml");
        entries.clear();
        nextId = 1L;
        if (!dataFile.exists()) return;
        YamlConfiguration config = PersistenceFiles.loadOrBackup(plugin, dataFile);
        if (config == null) return;
        nextId = Math.max(1L, config.getLong("next-id", 1L));
        if (!(config.get("entries") instanceof ConfigurationSection root)) return;
        List<String> keys = new ArrayList<>(root.getKeys(false));
        keys.sort(String::compareTo);
        for (String key : keys) {
            try {
                long id = Long.parseLong(key);
                UUID playerId = UUID.fromString(root.getString(key + ".player-id"));
                RecycleEntry entry = new RecycleEntry(
                    id,
                    playerId,
                    root.getString(key + ".player-name", "unknown"),
                    root.getString(key + ".material", "AIR"),
                    root.getInt(key + ".amount", 1),
                    root.getString(key + ".item-data", ""),
                    root.getLong(key + ".timestamp"),
                    root.getString(key + ".reason", "DELETE")
                );
                entries.addLast(entry);
                nextId = Math.max(nextId, id + 1);
            } catch (RuntimeException exception) {
                plugin.getLogger().warning("Ignoring invalid recycle entry " + key + ": " + exception.getMessage());
            }
        }
        prune();
    }

    public void record(UUID playerId, String playerName, ItemStack item, String reason) {
        if (!plugin.getConfig().getBoolean("staff-recycle.enabled", true)) return;
        if (item == null || item.isEmpty()) return;
        String encoded = Base64.getEncoder().encodeToString(item.serializeAsBytes());
        RecycleEntry entry = new RecycleEntry(
            nextId++,
            playerId,
            playerName == null ? "unknown" : playerName,
            item.getType().name(),
            item.getAmount(),
            encoded,
            System.currentTimeMillis(),
            reason
        );
        entries.addLast(entry);
        prune();
        dirty = true;
        flushAsync();
    }

    public List<RecycleEntry> history(UUID filterPlayer, int page, int pageSize) {
        prune();
        List<RecycleEntry> filtered = new ArrayList<>();
        Iterator<RecycleEntry> iterator = entries.descendingIterator();
        while (iterator.hasNext()) {
            RecycleEntry entry = iterator.next();
            if (filterPlayer != null && !filterPlayer.equals(entry.playerId())) continue;
            filtered.add(entry);
        }
        int from = Math.max(0, page * pageSize);
        if (from >= filtered.size()) return List.of();
        int to = Math.min(filtered.size(), from + pageSize);
        return List.copyOf(filtered.subList(from, to));
    }

    public RecycleEntry find(long id) {
        for (RecycleEntry entry : entries) {
            if (entry.id() == id) return entry;
        }
        return null;
    }

    public ItemStack restoreItem(RecycleEntry entry) {
        if (entry.itemData() == null || entry.itemData().isBlank()) return null;
        try {
            return ItemStack.deserializeBytes(Base64.getDecoder().decode(entry.itemData()));
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("Failed to restore recycle entry " + entry.id() + ": " + exception.getMessage());
            return null;
        }
    }

    public boolean remove(long id) {
        boolean removed = entries.removeIf(entry -> entry.id() == id);
        if (removed) {
            dirty = true;
            flushAsync();
        }
        return removed;
    }

    private void prune() {
        long retentionMs = Math.max(1, plugin.getConfig().getInt("staff-recycle.retention-hours", 72)) * 3_600_000L;
        int maxEntries = Math.max(1, plugin.getConfig().getInt("staff-recycle.max-entries", 200));
        long cutoff = System.currentTimeMillis() - retentionMs;
        while (!entries.isEmpty() && entries.peekFirst().timestamp() < cutoff) {
            entries.removeFirst();
            dirty = true;
        }
        while (entries.size() > maxEntries) {
            entries.removeFirst();
            dirty = true;
        }
    }

    private void flushAsync() {
        if (!dirty || dataFile == null) return;
        dirty = false;
        YamlConfiguration config = snapshot();
        plugin.getServer().getAsyncScheduler().runNow(plugin, ignored -> {
            synchronized (ioLock) {
                PersistenceFiles.save(plugin, config, dataFile);
            }
        });
    }

    private YamlConfiguration snapshot() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("next-id", nextId);
        for (RecycleEntry entry : entries) {
            String path = "entries." + entry.id();
            config.set(path + ".player-id", entry.playerId().toString());
            config.set(path + ".player-name", entry.playerName());
            config.set(path + ".material", entry.material());
            config.set(path + ".amount", entry.amount());
            config.set(path + ".item-data", entry.itemData());
            config.set(path + ".timestamp", entry.timestamp());
            config.set(path + ".reason", entry.reason());
        }
        return config;
    }

    public void shutdown() {
        if (dataFile == null) return;
        dirty = false;
        YamlConfiguration config = snapshot();
        synchronized (ioLock) {
            PersistenceFiles.save(plugin, config, dataFile);
        }
    }

    public int size() { return entries.size(); }
}
