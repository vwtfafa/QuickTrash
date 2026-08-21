package org.vwtfafa.quicktrash.manager;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.vwtfafa.quicktrash.QuickTrash;
import org.vwtfafa.quicktrash.gui.TrashHolder;
import org.vwtfafa.quicktrash.model.TrashSession;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;
import net.kyori.adventure.text.Component;

public final class TrashManager {
    private final QuickTrash plugin;
    private final Map<UUID, TrashSession> sessions = new HashMap<>();
    private File dataFile;
    private BukkitTask task;

    public TrashManager(QuickTrash plugin) { this.plugin = plugin; }
    public QuickTrash plugin() { return plugin; }

    public void load() {
        dataFile = new File(plugin.getDataFolder(), "trash-data.yml");
        if (!dataFile.exists()) return;
        ConfigurationSection root = YamlConfiguration.loadConfiguration(dataFile).getConfigurationSection("sessions");
        if (root == null) return;
        for (String key : root.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                TrashSession session = new TrashSession(id, root.getLong(key + ".expires-at"));
                var list = root.getList(key + ".items", java.util.List.of());
                for (int slot = 0; slot < Math.min(18, list.size()); slot++) {
                    if (list.get(slot) instanceof ItemStack item) session.items()[slot] = item;
                }
                if (session.expiresAt() > System.currentTimeMillis()) sessions.put(id, session);
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("Ignoring invalid trash session: " + key);
            }
        }
        ensureTimer();
    }

    public void open(Player player) {
        TrashSession session = sessions.computeIfAbsent(player.getUniqueId(), id -> new TrashSession(id, expiryFromNow()));
        TrashHolder holder = new TrashHolder(player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, 27, title());
        holder.inventory(inventory);
        for (int slot = 0; slot < 18; slot++) inventory.setItem(slot, session.items()[slot]);
        decorate(inventory);
        player.openInventory(inventory);
        ensureTimer();
        plugin.messages().send(player, "trash-opened");
    }

    private void decorate(Inventory inventory) {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.customName(Component.empty());
        filler.setItemMeta(fillerMeta);
        for (int slot = 18; slot < 27; slot++) inventory.setItem(slot, filler);
        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta meta = info.getItemMeta();
        meta.customName(plugin.messages().component(plugin.getConfig().getString("gui.info-name", "QuickTrash")));
        meta.lore(plugin.getConfig().getStringList("gui.info-lore").stream()
            .map(line -> plugin.messages().component(line.replace("{seconds}", String.valueOf(clearSeconds())))).toList());
        info.setItemMeta(meta);
        inventory.setItem(22, info);
    }

    public void snapshot(Player player, Inventory inventory) {
        TrashSession session = sessions.get(player.getUniqueId());
        if (session == null) return;
        for (int slot = 0; slot < 18; slot++) session.items()[slot] = inventory.getItem(slot);
        save();
    }

    public int put(Player player, ItemStack item) {
        TrashSession session = sessions.get(player.getUniqueId());
        if (session == null) return -1;
        for (int slot = 0; slot < 18; slot++) {
            if (session.items()[slot] == null || session.items()[slot].getType().isAir()) {
                session.items()[slot] = item.clone();
                return slot;
            }
        }
        return -1;
    }

    public TrashSession session(UUID id) { return sessions.get(id); }
    public void remove(UUID id) { sessions.remove(id); save(); }
    public int clearSeconds() { return Math.max(1, plugin.getConfig().getInt("trash.auto-clear-seconds", 30)); }
    private long expiryFromNow() { return System.currentTimeMillis() + clearSeconds() * 1000L; }
    private Component title() { return plugin.messages().component(plugin.getConfig().getString("gui.title", "&8QuickTrash")); }

    private void ensureTimer() {
        if (task != null || sessions.isEmpty()) return;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    private void tick() {
        long now = System.currentTimeMillis();
        for (TrashSession session : java.util.List.copyOf(sessions.values())) {
            if (session.expiresAt() > now) continue;
            UUID id = session.playerId();
            Player player = Bukkit.getPlayer(id);
            if (player != null && player.getOpenInventory().getTopInventory().getHolder() instanceof TrashHolder holder
                && holder.playerId().equals(id)) {
                player.closeInventory();
                plugin.messages().send(player, "trash-cleared");
            }
            sessions.remove(id);
        }
        if (sessions.isEmpty() && task != null) {
            task.cancel();
            task = null;
        }
        save();
    }

    public void save() {
        if (dataFile == null) return;
        YamlConfiguration config = new YamlConfiguration();
        for (TrashSession session : sessions.values()) {
            String path = "sessions." + session.playerId();
            config.set(path + ".expires-at", session.expiresAt());
            config.set(path + ".items", java.util.Arrays.asList(session.items()));
        }
        try { config.save(dataFile); }
        catch (IOException exception) { plugin.getLogger().log(java.util.logging.Level.SEVERE, "Could not save trash data", exception); }
    }

    public void shutdown() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof TrashHolder) {
                snapshot(player, player.getOpenInventory().getTopInventory());
            }
        }
        if (task != null) task.cancel();
        task = null;
        save();
    }
}