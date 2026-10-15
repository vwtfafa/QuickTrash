package org.vwtfafa.quicktrash.manager;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.io.File;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.vwtfafa.quicktrash.QuickTrash;
import org.vwtfafa.quicktrash.gui.TrashHolder;
import org.vwtfafa.quicktrash.model.TrashSession;
import org.vwtfafa.quicktrash.util.PersistenceFiles;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;

public final class TrashManager {
    private final QuickTrash plugin;
    private final Map<UUID, TrashSession> sessions = new HashMap<>();
    private final Map<UUID, Long> lastInfoSeconds = new HashMap<>();
    private final Object ioLock = new Object();
    private volatile boolean dirty;
    private File dataFile;
    private ScheduledTask task;

    public TrashManager(QuickTrash plugin) { this.plugin = plugin; }
    public QuickTrash plugin() { return plugin; }

    public void load() {
        dataFile = new File(plugin.getDataFolder(), "trash-data.yml");
        if (!dataFile.exists()) return;
        YamlConfiguration config = PersistenceFiles.loadOrBackup(plugin, dataFile);
        if (config == null || !(config.get("sessions") instanceof ConfigurationSection root)) return;
        for (String key : root.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                TrashSession session = new TrashSession(id, root.getLong(key + ".expires-at"));
                ItemStack[] items = readItems(root.get(key + ".items"));
                for (int slot = 0; slot < Math.min(TrashSession.SIZE, items.length); slot++) {
                    if (items[slot] != null && !items[slot].isEmpty()) session.setItem(slot, items[slot]);
                }
                if (session.expiresAt() > System.currentTimeMillis()) sessions.put(id, session);
            } catch (RuntimeException exception) {
                // Broken sessions must never keep the plugin from starting.
                plugin.getLogger().warning("Ignoring invalid trash session " + key + ": " + exception.getMessage());
            }
        }
        ensureTimer();
    }

    /**
     * Reads the NBT blob written by current versions and falls back to the legacy YAML item list.
     * NBT keeps the built-in data converter in the loop when items are loaded on newer server versions.
     */
    private static ItemStack[] readItems(Object stored) {
        if (stored instanceof String encoded) {
            if (encoded.isBlank()) return new ItemStack[TrashSession.SIZE];
            ItemStack[] decoded = ItemStack.deserializeItemsFromBytes(Base64.getDecoder().decode(encoded));
            for (int slot = 0; slot < decoded.length; slot++) {
                if (decoded[slot] != null && decoded[slot].isEmpty()) decoded[slot] = null;
            }
            return decoded;
        }
        ItemStack[] items = new ItemStack[TrashSession.SIZE];
        if (stored instanceof List<?> list) {
            for (int slot = 0; slot < Math.min(TrashSession.SIZE, list.size()); slot++) {
                if (list.get(slot) instanceof ItemStack item) items[slot] = item;
            }
        }
        return items;
    }

    public void open(Player player) {
        TrashSession session = sessions.computeIfAbsent(player.getUniqueId(), id -> new TrashSession(id, expiryFromNow()));
        session.refresh(expiryFromNow());
        markDirty();
        TrashHolder holder = new TrashHolder(player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, TrashHolder.INVENTORY_SIZE, title());
        holder.inventory(inventory);
        for (int slot = 0; slot < TrashSession.SIZE; slot++) inventory.setItem(slot, session.itemAt(slot));
        decorate(inventory, clearSeconds());
        player.openInventory(inventory);
        ensureTimer();
        org.vwtfafa.quicktrash.util.Sounds.play(plugin, player, "gui.sounds.open");
        plugin.messages().send(player, "trash-opened");
    }

    private void decorate(Inventory inventory, long seconds) {
        ItemStack filler = ItemStack.of(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.customName(Component.empty());
        filler.setItemMeta(fillerMeta);
        for (int slot = TrashSession.SIZE; slot < TrashHolder.INVENTORY_SIZE; slot++) inventory.setItem(slot, filler);
        inventory.setItem(TrashHolder.INFO_SLOT, infoItem(seconds));
    }

    private ItemStack infoItem(long seconds) {
        ItemStack info = ItemStack.of(Material.PAPER);
        ItemMeta meta = info.getItemMeta();
        meta.customName(plugin.messages().component(plugin.getConfig().getString("gui.info-name", "QuickTrash")));
        meta.lore(plugin.getConfig().getStringList("gui.info-lore").stream()
            .map(line -> plugin.messages().component(line, Map.of("seconds", String.valueOf(seconds)))).toList());
        info.setItemMeta(meta);
        return info;
    }

    public void snapshot(Player player, Inventory inventory) {
        TrashSession session = sessions.get(player.getUniqueId());
        if (session == null) return;
        for (int slot = 0; slot < TrashSession.SIZE; slot++) session.setItem(slot, inventory.getItem(slot));
        session.refresh(expiryFromNow());
        markDirty();
    }

    public int put(Player player, Inventory top, ItemStack item) {
        if (item == null || item.isEmpty() || !sessions.containsKey(player.getUniqueId())) return -1;
        int remaining = item.getAmount();
        int maxStack = item.getMaxStackSize();
        for (int slot = 0; slot < TrashSession.SIZE && remaining > 0; slot++) {
            ItemStack existing = top.getItem(slot);
            if (existing == null || existing.isEmpty() || !existing.isSimilar(item)) continue;
            int space = maxStack - existing.getAmount();
            if (space <= 0) continue;
            int transfer = Math.min(space, remaining);
            top.setItem(slot, existing.asQuantity(existing.getAmount() + transfer));
            remaining -= transfer;
        }
        for (int slot = 0; slot < TrashSession.SIZE && remaining > 0; slot++) {
            ItemStack existing = top.getItem(slot);
            if (existing != null && !existing.isEmpty()) continue;
            int transfer = Math.min(maxStack, remaining);
            top.setItem(slot, item.asQuantity(transfer));
            remaining -= transfer;
        }
        if (remaining == item.getAmount()) return -1;
        snapshot(player, top);
        return remaining;
    }

    public TrashSession session(UUID id) { return sessions.get(id); }
    public void remove(UUID id) { sessions.remove(id); markDirty(); }
    public int clearSeconds() { return Math.max(1, plugin.getConfig().getInt("trash.auto-clear-seconds", 30)); }
    private long expiryFromNow() { return System.currentTimeMillis() + clearSeconds() * 1000L; }
    private Component title() { return plugin.messages().component(plugin.getConfig().getString("gui.title", "&8QuickTrash")); }

    private void ensureTimer() {
        if (task != null || sessions.isEmpty()) return;
        task = plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, ignored -> tick(), 20L, 20L);
    }

    private void tick() {
        long now = System.currentTimeMillis();
        boolean changed = false;
        for (TrashSession session : List.copyOf(sessions.values())) {
            UUID id = session.playerId();
            Player viewer = Bukkit.getPlayer(id);
            Inventory open = viewer != null && viewer.getOpenInventory().getTopInventory().getHolder() instanceof TrashHolder holder
                && holder.playerId().equals(id)
                ? viewer.getOpenInventory().getTopInventory()
                : null;
            if (session.expiresAt() <= now) {
                if (open != null) syncFromInventory(session, open);
                plugin.stats().add(id, session.itemCount());
                sessions.remove(id);
                lastInfoSeconds.remove(id);
                changed = true;
                if (open != null) {
                    viewer.closeInventory(InventoryCloseEvent.Reason.PLUGIN);
                    plugin.messages().send(viewer, "trash-cleared");
                }
                continue;
            }
            if (open != null) {
                long seconds = Math.max(0, (session.expiresAt() - now + 999) / 1000);
                Long previous = lastInfoSeconds.get(id);
                if (previous == null || previous != seconds) {
                    lastInfoSeconds.put(id, seconds);
                    open.setItem(TrashHolder.INFO_SLOT, infoItem(seconds));
                }
            }
        }
        if (sessions.isEmpty() && task != null) {
            task.cancel();
            task = null;
        }
        if (changed || dirty) flushAsync();
    }

    private static void syncFromInventory(TrashSession session, Inventory inventory) {
        for (int slot = 0; slot < TrashSession.SIZE; slot++) session.setItem(slot, inventory.getItem(slot));
    }

    private void markDirty() { dirty = true; }

    private void flushAsync() {
        // Clear first so marks during the snapshot are not lost; the next tick flushes again.
        dirty = false;
        YamlConfiguration config = snapshotConfig();
        plugin.getServer().getAsyncScheduler().runNow(plugin, ignored -> writeFile(config));
    }

    private YamlConfiguration snapshotConfig() {
        YamlConfiguration config = new YamlConfiguration();
        for (TrashSession session : sessions.values()) {
            String path = "sessions." + session.playerId();
            config.set(path + ".expires-at", session.expiresAt());
            config.set(path + ".items", Base64.getEncoder().encodeToString(ItemStack.serializeItemsAsBytes(session.copyItems())));
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
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof TrashHolder) {
                snapshot(player, player.getOpenInventory().getTopInventory());
            }
        }
        if (task != null) task.cancel();
        task = null;
        writeFile(snapshotConfig());
    }
}