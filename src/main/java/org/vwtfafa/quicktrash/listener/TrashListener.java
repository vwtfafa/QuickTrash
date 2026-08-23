package org.vwtfafa.quicktrash.listener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.vwtfafa.quicktrash.QuickTrash;
import org.vwtfafa.quicktrash.gui.TrashHolder;
import org.vwtfafa.quicktrash.manager.TrashManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class TrashListener implements Listener {
    private final QuickTrash plugin;
    private final TrashManager manager;
    private final Map<UUID, PendingDeletion> pending = new HashMap<>();

    public TrashListener(QuickTrash plugin) {
        this.plugin = plugin;
        this.manager = plugin.trash();
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof TrashHolder holder)) return;
        if (!(event.getWhoClicked() instanceof Player player) || !holder.playerId().equals(player.getUniqueId())) return;
        if (event.getClick().isKeyboardClick() || event.getClick() == ClickType.DOUBLE_CLICK
            || event.getClick() == ClickType.SWAP_OFFHAND || event.getClick().isCreativeAction()) {
            event.setCancelled(true);
            return;
        }
        if (event.getClickedInventory() == top) {
            if (event.getSlot() >= 18 || event.getClick().isShiftClick()) {
                event.setCancelled(true);
            }
            if (event.getSlot() < 18 && event.getClick().isShiftClick()) deleteFromTrash(player, top, event.getSlot(), event.getCurrentItem());
            return;
        }
        if (event.getClickedInventory() == event.getView().getBottomInventory() && event.getClick().isShiftClick()) {
            event.setCancelled(true);
            moveFromPlayer(player, event);
        }
    }

    private void deleteFromTrash(Player player, Inventory top, int slot, ItemStack item) {
        if (item == null || item.getType().isAir()) return;
        if (requiresConfirmation(player, slot, item)) return;
        top.setItem(slot, null);
        sendDeleted(player, item);
        org.vwtfafa.quicktrash.util.Sounds.play(plugin, player, "gui.sounds.delete");
    }

    private void moveFromPlayer(Player player, InventoryClickEvent event) {
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType().isAir()) return;
        int sourceSlot = event.getSlot();
        if (requiresConfirmation(player, sourceSlot, item)) return;
        ItemStack deposit = item.clone();
        int leftover = manager.put(player, event.getView().getTopInventory(), deposit);
        if (leftover == -1) {
            plugin.messages().send(player, "no-space");
            return;
        }
        if (leftover == 0) {
            event.getClickedInventory().setItem(sourceSlot, null);
        } else {
            deposit.setAmount(leftover);
            event.getClickedInventory().setItem(sourceSlot, deposit);
        }
    }

    private boolean requiresConfirmation(Player player, int slot, ItemStack item) {
        if (player.hasPermission("quicktrash.bypass") || !plugin.getConfig().getBoolean("valuable-items.enabled", true)
            || !plugin.getConfig().getBoolean("valuable-items.require-confirmation", true)
            || !plugin.valuableItems().isValuable(item)) return false;
        long now = System.currentTimeMillis();
        PendingDeletion previous = pending.get(player.getUniqueId());
        int timeout = Math.max(1, plugin.getConfig().getInt("valuable-items.confirmation-timeout-seconds", 5));
        if (previous != null && previous.slot() == slot && previous.item().isSimilar(item) && now - previous.createdAt() <= timeout * 1000L) {
            pending.remove(player.getUniqueId());
            return false;
        }
        pending.put(player.getUniqueId(), new PendingDeletion(slot, item.clone(), now));
        plugin.messages().send(player, "valuable-warning");
        plugin.messages().send(player, "confirmation-required");
        return true;
    }

    private void sendDeleted(Player player, ItemStack item) {
        plugin.messages().actionbar(player, plugin.messages().deletedItems(item.getAmount(), item));
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof TrashHolder holder)) return;
        if (event.getPlayer() instanceof Player player) manager.snapshot(player, event.getInventory());
        pending.remove(holder.playerId());
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof TrashHolder)) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onMove(InventoryMoveItemEvent event) {
        if (event.getSource().getHolder() instanceof TrashHolder || event.getDestination().getHolder() instanceof TrashHolder) event.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        closeAndSnapshot(event.getPlayer());
        pending.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) { closeAndSnapshot(event.getEntity()); }
    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) { closeAndSnapshot(event.getPlayer()); }

    private void closeAndSnapshot(Player player) {
        if (player.getOpenInventory().getTopInventory().getHolder() instanceof TrashHolder) {
            manager.snapshot(player, player.getOpenInventory().getTopInventory());
            plugin.getServer().getScheduler().runTask(plugin, () -> player.closeInventory());
        }
    }

    private record PendingDeletion(int slot, ItemStack item, long createdAt) { }
}