package org.vwtfafa.quicktrash.manager;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.io.File;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.vwtfafa.quicktrash.QuickTrash;
import org.vwtfafa.quicktrash.gui.TrashHolder;
import org.vwtfafa.quicktrash.gui.TrashLayout;
import org.vwtfafa.quicktrash.model.TrashSession;
import org.vwtfafa.quicktrash.model.UndoBuffer;
import org.vwtfafa.quicktrash.util.PersistenceFiles;
import org.vwtfafa.quicktrash.util.RowsResolver;
import org.vwtfafa.quicktrash.util.Sounds;

public final class TrashManager {
    static final long FLUSH_DEBOUNCE_MS = 3_000L;

    private final QuickTrash plugin;
    private final Map<UUID, TrashSession> sessions = new HashMap<>();
    private final Map<UUID, Long> lastInfoSeconds = new HashMap<>();
    private final Map<UUID, UndoBuffer> undoBuffers = new HashMap<>();
    private final Map<UUID, Long> openCooldowns = new HashMap<>();
    private final Object ioLock = new Object();
    private volatile boolean dirty;
    private volatile long lastFlushAt;
    private File dataFile;
    private ScheduledTask task;
    private ItemStack cachedFiller;
    private int defaultRows = TrashLayout.DEFAULT_ROWS;

    public TrashManager(QuickTrash plugin) { this.plugin = plugin; }
    public QuickTrash plugin() { return plugin; }
    public TrashLayout layout() { return TrashLayout.fromRows(defaultRows); }
    public boolean isDirty() { return dirty; }
    public long lastFlushAt() { return lastFlushAt; }

    public TrashLayout layoutFor(Player player) {
        int rows = RowsResolver.resolve(player, defaultRows);
        return TrashLayout.fromRows(rows);
    }

    public void reloadSettings() {
        defaultRows = plugin.getConfig().getInt("trash.rows", TrashLayout.DEFAULT_ROWS);
        cachedFiller = null;
        Sounds.invalidate();
    }

    public void load() {
        reloadSettings();
        dataFile = new File(plugin.getDataFolder(), "trash-data.yml");
        if (!dataFile.exists()) return;
        YamlConfiguration config = PersistenceFiles.loadOrBackup(plugin, dataFile);
        if (config == null || !(config.get("sessions") instanceof ConfigurationSection root)) return;
        for (String key : root.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                int size = TrashLayout.fromRows(defaultRows).contentSize();
                TrashSession session = new TrashSession(id, root.getLong(key + ".expires-at"), size);
                ItemStack[] items = readItems(root.get(key + ".items"), size);
                for (int slot = 0; slot < Math.min(session.size(), items.length); slot++) {
                    if (items[slot] != null && !items[slot].isEmpty()) session.setItem(slot, items[slot]);
                }
                if (session.expiresAt() > System.currentTimeMillis()) sessions.put(id, session);
            } catch (RuntimeException exception) {
                plugin.getLogger().warning("Ignoring invalid trash session " + key + ": " + exception.getMessage());
            }
        }
        ensureTimer();
    }

    private static ItemStack[] readItems(Object stored, int size) {
        if (stored instanceof String encoded) {
            if (encoded.isBlank()) return new ItemStack[size];
            ItemStack[] decoded = ItemStack.deserializeItemsFromBytes(Base64.getDecoder().decode(encoded));
            ItemStack[] items = new ItemStack[size];
            for (int slot = 0; slot < Math.min(size, decoded.length); slot++) {
                if (decoded[slot] != null && !decoded[slot].isEmpty()) items[slot] = decoded[slot];
            }
            return items;
        }
        ItemStack[] items = new ItemStack[size];
        if (stored instanceof List<?> list) {
            for (int slot = 0; slot < Math.min(size, list.size()); slot++) {
                if (list.get(slot) instanceof ItemStack item) items[slot] = item;
            }
        }
        return items;
    }

    public boolean canOpen(Player player) {
        String world = player.getWorld().getName().toLowerCase(Locale.ROOT);
        for (String disabled : plugin.getConfig().getStringList("trash.disabled-worlds")) {
            if (world.equals(disabled.toLowerCase(Locale.ROOT))) {
                plugin.messages().send(player, "world-disabled");
                return false;
            }
        }
        int cooldown = Math.max(0, plugin.getConfig().getInt("trash.open-cooldown-seconds", 0));
        if (cooldown > 0) {
            long now = System.currentTimeMillis();
            Long until = openCooldowns.get(player.getUniqueId());
            if (until != null && until > now) {
                long seconds = Math.max(1, (until - now + 999) / 1000);
                plugin.messages().send(player, "cooldown", Map.of("seconds", String.valueOf(seconds)));
                return false;
            }
            openCooldowns.put(player.getUniqueId(), now + cooldown * 1000L);
        }
        return true;
    }

    public void open(Player player) {
        if (!canOpen(player)) return;
        TrashLayout playerLayout = layoutFor(player);
        TrashSession session = sessions.computeIfAbsent(
            player.getUniqueId(), id -> new TrashSession(id, expiryFromNow(), playerLayout.contentSize()));
        if (session.size() != playerLayout.contentSize()) {
            TrashSession resized = new TrashSession(player.getUniqueId(), expiryFromNow(), playerLayout.contentSize());
            ItemStack[] old = session.copyItems();
            for (int slot = 0; slot < Math.min(old.length, resized.size()); slot++) resized.setItem(slot, old[slot]);
            sessions.put(player.getUniqueId(), resized);
            session = resized;
        }
        session.refresh(expiryFromNow());
        markDirty();
        TrashHolder holder = new TrashHolder(player.getUniqueId(), playerLayout);
        Inventory inventory = Bukkit.createInventory(holder, playerLayout.inventorySize(), title());
        holder.inventory(inventory);
        for (int slot = 0; slot < session.size(); slot++) inventory.setItem(slot, session.itemAt(slot));
        decorate(inventory, playerLayout, clearSeconds());
        player.openInventory(inventory);
        ensureTimer();
        Sounds.play(plugin, player, "gui.sounds.open");
        plugin.messages().send(player, "trash-opened");
        plugin.metricsCounters().opened();
    }

    public void openFor(Player viewer, UUID targetId) {
        Player target = Bukkit.getPlayer(targetId);
        TrashLayout targetLayout = target != null ? layoutFor(target) : TrashLayout.fromRows(defaultRows);
        TrashSession session = sessions.computeIfAbsent(
            targetId, id -> new TrashSession(id, expiryFromNow(), targetLayout.contentSize()));
        session.refresh(expiryFromNow());
        markDirty();
        TrashHolder holder = new TrashHolder(targetId, targetLayout);
        Inventory inventory = Bukkit.createInventory(holder, targetLayout.inventorySize(), title());
        holder.inventory(inventory);
        for (int slot = 0; slot < session.size(); slot++) inventory.setItem(slot, session.itemAt(slot));
        decorate(inventory, targetLayout, clearSeconds());
        viewer.openInventory(inventory);
        ensureTimer();
    }

    private void decorate(Inventory inventory, TrashLayout layout, long seconds) {
        ItemStack filler = fillerItem();
        for (int slot = layout.contentSize(); slot < layout.inventorySize(); slot++) {
            inventory.setItem(slot, filler);
        }
        inventory.setItem(layout.infoSlot(), infoItem(seconds));
        inventory.setItem(layout.clearSlot(), buttonItem(
            Material.LAVA_BUCKET,
            "gui.clear-name",
            "gui.clear-lore",
            Map.of()));
        if (plugin.getConfig().getBoolean("trash.undo.enabled", true)) {
            inventory.setItem(layout.undoSlot(), buttonItem(
                Material.CLOCK,
                "gui.undo-name",
                "gui.undo-lore",
                Map.of()));
        }
    }

    private ItemStack fillerItem() {
        if (cachedFiller != null) return cachedFiller.clone();
        ItemStack filler = ItemStack.of(Material.GRAY_STAINED_GLASS_PANE);
        filler.setData(DataComponentTypes.ITEM_NAME, Component.empty());
        cachedFiller = filler;
        return filler.clone();
    }

    private ItemStack infoItem(long seconds) {
        ItemStack info = ItemStack.of(Material.PAPER);
        info.setData(DataComponentTypes.ITEM_NAME,
            plugin.messages().component(plugin.getConfig().getString("gui.info-name", "QuickTrash")));
        List<Component> lore = plugin.getConfig().getStringList("gui.info-lore").stream()
            .map(line -> plugin.messages().component(line, Map.of("seconds", String.valueOf(seconds))))
            .toList();
        info.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
        return info;
    }

    private ItemStack buttonItem(Material material, String namePath, String lorePath, Map<String, String> replacements) {
        ItemStack item = ItemStack.of(material);
        item.setData(DataComponentTypes.ITEM_NAME,
            plugin.messages().component(plugin.getConfig().getString(namePath, namePath), replacements));
        List<Component> lore = plugin.getConfig().getStringList(lorePath).stream()
            .map(line -> plugin.messages().component(line, replacements))
            .toList();
        if (!lore.isEmpty()) item.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
        return item;
    }

    public void snapshot(Player player, Inventory inventory) {
        TrashSession session = sessions.get(player.getUniqueId());
        if (session == null) return;
        TrashLayout openLayout = inventory.getHolder() instanceof TrashHolder holder
            ? holder.layout() : TrashLayout.fromRows(defaultRows);
        for (int slot = 0; slot < Math.min(session.size(), openLayout.contentSize()); slot++) {
            session.setItem(slot, inventory.getItem(slot));
        }
        session.refresh(expiryFromNow());
        markDirty();
    }

    public void markInventoryChanged(Player player, Inventory inventory) {
        snapshot(player, inventory);
    }

    public int put(Player player, Inventory top, ItemStack item) {
        if (item == null || item.isEmpty() || !sessions.containsKey(player.getUniqueId())) return -1;
        if (plugin.valuableItems().isBlocked(item)) {
            plugin.messages().send(player, "blocked-item");
            return -1;
        }
        int contentSize = top.getHolder() instanceof TrashHolder holder
            ? holder.layout().contentSize() : TrashLayout.fromRows(defaultRows).contentSize();
        int remaining = item.getAmount();
        int maxStack = item.getMaxStackSize();
        for (int slot = 0; slot < contentSize && remaining > 0; slot++) {
            ItemStack existing = top.getItem(slot);
            if (existing == null || existing.isEmpty() || !existing.isSimilar(item)) continue;
            int space = maxStack - existing.getAmount();
            if (space <= 0) continue;
            int transfer = Math.min(space, remaining);
            top.setItem(slot, existing.asQuantity(existing.getAmount() + transfer));
            remaining -= transfer;
        }
        for (int slot = 0; slot < contentSize && remaining > 0; slot++) {
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

    public void deleteItem(Player player, Inventory top, int slot, ItemStack item) {
        top.setItem(slot, null);
        recordPermanentDelete(player, item, "DELETE");
        snapshot(player, top);
        Sounds.play(plugin, player, "gui.sounds.delete");
    }

    /**
     * Bookkeeping for a permanent delete: stats, optional staff recycle, dirty flag.
     * Extracted so persistence side effects can be verified without a live inventory click.
     */
    public void recordPermanentDelete(Player player, ItemStack item, String reason) {
        if (item == null || item.isEmpty()) return;
        plugin.stats().add(player.getUniqueId(), item.getAmount());
        if (plugin.valuableItems().isValuable(item)) {
            plugin.recycle().record(player.getUniqueId(), player.getName(), item, reason);
        }
        markDirty();
    }

    private void recordValuableContents(UUID playerId, String playerName, ItemStack[] items, String reason) {
        for (ItemStack item : items) {
            if (item != null && !item.isEmpty() && plugin.valuableItems().isValuable(item)) {
                plugin.recycle().record(playerId, playerName, item, reason);
            }
        }
    }

    public void clearNow(Player player, Inventory top, boolean manual) {
        TrashSession session = sessions.get(player.getUniqueId());
        if (session == null) return;
        TrashLayout openLayout = top.getHolder() instanceof TrashHolder holder
            ? holder.layout() : TrashLayout.fromRows(defaultRows);
        syncFromInventory(session, top);
        ItemStack[] snapshotItems = session.copyItems();
        storeUndo(player.getUniqueId(), snapshotItems);
        recordValuableContents(player.getUniqueId(), player.getName(), snapshotItems, manual ? "CLEAR" : "EXPIRE");
        int count = session.itemCount();
        if (count > 0) plugin.stats().add(player.getUniqueId(), count);
        session.clear();
        for (int slot = 0; slot < openLayout.contentSize() && slot < top.getSize(); slot++) top.setItem(slot, null);
        sessions.remove(player.getUniqueId());
        lastInfoSeconds.remove(player.getUniqueId());
        markDirty();
        player.closeInventory(InventoryCloseEvent.Reason.PLUGIN);
        plugin.messages().send(player, manual ? "trash-cleared-manual" : "trash-cleared");
    }

    public boolean undo(Player player, Inventory top) {
        UndoBuffer buffer = undoBuffers.get(player.getUniqueId());
        long now = System.currentTimeMillis();
        if (buffer == null || buffer.expired(now)) {
            undoBuffers.remove(player.getUniqueId());
            plugin.messages().send(player, "undo-empty");
            return false;
        }
        TrashLayout playerLayout = top.getHolder() instanceof TrashHolder holder
            ? holder.layout() : layoutFor(player);
        TrashSession session = sessions.computeIfAbsent(
            player.getUniqueId(), id -> new TrashSession(id, expiryFromNow(), playerLayout.contentSize()));
        ItemStack[] items = buffer.items();
        for (int slot = 0; slot < Math.min(session.size(), items.length); slot++) {
            session.setItem(slot, items[slot]);
            if (slot < top.getSize()) top.setItem(slot, items[slot]);
        }
        session.refresh(expiryFromNow());
        undoBuffers.remove(player.getUniqueId());
        markDirty();
        plugin.messages().send(player, "undo-success", Map.of("amount", String.valueOf(buffer.itemCount())));
        ensureTimer();
        return true;
    }

    private void storeUndo(UUID playerId, ItemStack[] items) {
        if (!plugin.getConfig().getBoolean("trash.undo.enabled", true)) return;
        boolean any = false;
        for (ItemStack item : items) {
            if (item != null && !item.isEmpty()) {
                any = true;
                break;
            }
        }
        if (!any) return;
        int seconds = Math.max(1, plugin.getConfig().getInt("trash.undo.seconds", 15));
        undoBuffers.put(playerId, new UndoBuffer(items, System.currentTimeMillis() + seconds * 1000L));
    }

    public TrashSession session(UUID id) { return sessions.get(id); }
    public void remove(UUID id) {
        sessions.remove(id);
        lastInfoSeconds.remove(id);
        markDirty();
    }
    public void clearPlayer(UUID id) {
        TrashSession session = sessions.remove(id);
        if (session != null && session.itemCount() > 0) {
            storeUndo(id, session.copyItems());
            plugin.stats().add(id, session.itemCount());
        }
        lastInfoSeconds.remove(id);
        markDirty();
    }
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
            if (session.expiresAt() <= now) {
                expireSession(session, viewer);
                changed = true;
                continue;
            }
            if (viewer != null) {
                long seconds = Math.max(0, (session.expiresAt() - now + 999) / 1000);
                Long previous = lastInfoSeconds.get(id);
                if (previous == null || previous != seconds) {
                    lastInfoSeconds.put(id, seconds);
                    final long secondsFinal = seconds;
                    viewer.getScheduler().run(plugin, ignored -> updateInfoIfOpen(viewer, id, secondsFinal), null);
                }
            }
        }
        undoBuffers.entrySet().removeIf(entry -> entry.getValue().expired(now));
        if (sessions.isEmpty() && task != null) {
            task.cancel();
            task = null;
        }
        if (changed) {
            markDirty();
            flushAsync(true);
        } else if (dirty) {
            flushAsync(false);
        }
    }

    private void expireSession(TrashSession session, Player viewer) {
        UUID id = session.playerId();
        sessions.remove(id);
        lastInfoSeconds.remove(id);
        if (viewer != null) {
            viewer.getScheduler().run(plugin, ignored -> {
                Inventory open = openTrash(viewer, id);
                if (open != null) syncFromInventory(session, open);
                recordExpiredContents(viewer, session);
                if (open != null) {
                    viewer.closeInventory(InventoryCloseEvent.Reason.PLUGIN);
                    plugin.messages().send(viewer, "trash-cleared");
                }
            }, null);
            return;
        }
        ItemStack[] items = session.copyItems();
        storeUndo(id, items);
        recordValuableContents(id, "offline", items, "EXPIRE");
        plugin.stats().add(id, session.itemCount());
        markDirty();
    }

    private void recordExpiredContents(Player viewer, TrashSession session) {
        ItemStack[] items = session.copyItems();
        storeUndo(session.playerId(), items);
        recordValuableContents(viewer.getUniqueId(), viewer.getName(), items, "EXPIRE");
        plugin.stats().add(session.playerId(), session.itemCount());
        markDirty();
    }

    private void updateInfoIfOpen(Player viewer, UUID id, long seconds) {
        Inventory open = openTrash(viewer, id);
        if (open == null) return;
        TrashLayout openLayout = open.getHolder() instanceof TrashHolder holder
            ? holder.layout() : TrashLayout.fromRows(defaultRows);
        open.setItem(openLayout.infoSlot(), infoItem(seconds));
    }

    /** Visible for tests: whether a flush should run for the current dirty/debounce state. */
    public static boolean shouldFlush(boolean dirty, boolean force, long now, long lastFlushAt) {
        if (force) return true;
        return dirty && now - lastFlushAt >= FLUSH_DEBOUNCE_MS;
    }

    /** Visible for tests: whether a session should expire at {@code now}. */
    public static boolean isExpired(long expiresAt, long now) {
        return expiresAt <= now;
    }

    private static Inventory openTrash(Player viewer, UUID id) {
        if (viewer.getOpenInventory().getTopInventory().getHolder() instanceof TrashHolder holder
            && holder.playerId().equals(id)) {
            return viewer.getOpenInventory().getTopInventory();
        }
        return null;
    }

    private static void syncFromInventory(TrashSession session, Inventory inventory) {
        int size = Math.min(session.size(),
            inventory.getHolder() instanceof TrashHolder holder ? holder.layout().contentSize() : session.size());
        for (int slot = 0; slot < size; slot++) session.setItem(slot, inventory.getItem(slot));
    }

    private void markDirty() { dirty = true; }

    private void flushAsync(boolean force) {
        long now = System.currentTimeMillis();
        if (!force && now - lastFlushAt < FLUSH_DEBOUNCE_MS) return;
        dirty = false;
        lastFlushAt = now;
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

    public int activeSessions() { return sessions.size(); }

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
