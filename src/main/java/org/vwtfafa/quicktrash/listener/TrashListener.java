package org.vwtfafa.quicktrash.listener;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.vwtfafa.quicktrash.QuickTrash;
import org.vwtfafa.quicktrash.gui.TrashHolder;
import org.vwtfafa.quicktrash.gui.TrashLayout;
import org.vwtfafa.quicktrash.manager.TrashManager;

public final class TrashListener implements Listener {
    private final QuickTrash plugin;
    private final TrashManager manager;

    public TrashListener(QuickTrash plugin) {
        this.plugin = plugin;
        this.manager = plugin.trash();
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof TrashHolder holder)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!holder.playerId().equals(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        TrashLayout layout = holder.layout();

        if (event.getClick().isKeyboardClick() || event.getClick() == ClickType.DOUBLE_CLICK
            || event.getClick() == ClickType.SWAP_OFFHAND || event.getClick().isCreativeAction()
            || event.getClick() == ClickType.CONTROL_DROP) {
            event.setCancelled(true);
            return;
        }

        if (event.getClickedInventory() == top) {
            int slot = event.getSlot();
            if (!layout.isContentSlot(slot)) {
                event.setCancelled(true);
                if (slot == layout.clearSlot()) {
                    manager.clearNow(player, top, true);
                } else if (slot == layout.undoSlot()) {
                    manager.undo(player, top);
                }
                return;
            }
            if (event.getClick().isShiftClick()) {
                event.setCancelled(true);
                deleteFromTrash(player, top, slot, event.getCurrentItem());
            } else {
                player.getScheduler().run(plugin,
                    ignored -> manager.markInventoryChanged(player, top), null);
            }
            return;
        }

        if (event.getClickedInventory() == event.getView().getBottomInventory() && event.getClick().isShiftClick()) {
            event.setCancelled(true);
            moveFromPlayer(player, event);
            return;
        }

        if (event.getClickedInventory() == event.getView().getBottomInventory()) {
            // Cursor deposit into trash happens via vanilla; refresh after the click.
            player.getScheduler().run(plugin,
                ignored -> manager.markInventoryChanged(player, top), null);
        }
    }

    private void deleteFromTrash(Player player, Inventory top, int slot, ItemStack item) {
        if (item == null || item.isEmpty()) return;
        if (requiresConfirmation(player, PendingDeletion.TRASH_INVENTORY, slot, item)) return;
        finishDelete(player, top, slot, item);
    }

    private void finishDelete(Player player, Inventory top, int slot, ItemStack item) {
        manager.deleteItem(player, top, slot, item);
        plugin.messages().actionbar(player, plugin.messages().deletedItems(item.getAmount(), item));
    }

    private void moveFromPlayer(Player player, InventoryClickEvent event) {
        ItemStack item = event.getCurrentItem();
        Inventory bottom = event.getView().getBottomInventory();
        if (event.getClickedInventory() != bottom || item == null || item.isEmpty()) return;
        int sourceSlot = event.getSlot();
        if (plugin.valuableItems().isBlocked(item)) {
            plugin.messages().send(player, "blocked-item");
            return;
        }
        if (requiresConfirmation(player, PendingDeletion.PLAYER_INVENTORY, sourceSlot, item)) return;
        moveItemIntoTrash(player, event.getView().getTopInventory(), bottom, sourceSlot, item);
    }

    private void moveItemIntoTrash(Player player, Inventory top, Inventory bottom, int sourceSlot, ItemStack item) {
        int expected = item.getAmount();
        ItemStack deposit = item.clone();
        int leftover = manager.put(player, top, deposit);
        if (leftover == -1) {
            if (!plugin.valuableItems().isBlocked(item)) plugin.messages().send(player, "no-space");
            return;
        }
        ItemStack current = bottom.getItem(sourceSlot);
        if (current == null || current.getAmount() != expected || !current.isSimilar(item)) return;
        if (leftover == 0) {
            bottom.setItem(sourceSlot, null);
        } else {
            bottom.setItem(sourceSlot, item.asQuantity(leftover));
        }
    }

    private boolean requiresConfirmation(Player player, byte inventoryKind, int slot, ItemStack item) {
        boolean needsConfirm = org.vwtfafa.quicktrash.util.ConfirmationPolicy.requires(
            player.hasPermission("quicktrash.bypass"),
            plugin.preferences().isConfirmEnabled(player.getUniqueId()),
            plugin.getConfig().getBoolean("trash.confirm-all-deletes", false),
            plugin.getConfig().getBoolean("valuable-items.enabled", true),
            plugin.getConfig().getBoolean("valuable-items.require-confirmation", true),
            plugin.valuableItems().isValuable(item)
        );
        if (!needsConfirm) return false;
        int timeout = Math.max(1, plugin.getConfig().getInt("valuable-items.confirmation-timeout-seconds", 5));
        PendingDeletion deletion = new PendingDeletion(inventoryKind, slot, item.clone());
        showConfirmation(player, deletion, timeout);
        plugin.metricsCounters().confirmShown();
        return true;
    }

    private void showConfirmation(Player player, PendingDeletion deletion, int timeout) {
        Map<String, String> replacements = Map.of(
            "amount", String.valueOf(deletion.item().getAmount()),
            "seconds", String.valueOf(timeout)
        );
        Map<String, Component> itemName = Map.of("item", deletion.item().effectiveName());
        String bodyKey = deletion.inventory() == PendingDeletion.TRASH_INVENTORY
            ? "confirmation-delete-body"
            : "confirmation-move-body";
        String bodyFallback = deletion.inventory() == PendingDeletion.TRASH_INVENTORY
            ? "<gray>Delete {amount}x {item} permanently within {seconds}s?"
            : "<gray>Move {amount}x {item} into temporary trash within {seconds}s?";
        Component body = configuredMessage(bodyKey, bodyFallback, replacements, itemName);
        Component title = plugin.messages().message("valuable-warning");
        ClickCallback.Options callbackOptions = ClickCallback.Options.builder()
            .uses(1)
            .lifetime(Duration.ofSeconds(timeout))
            .build();
        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(title).body(List.of(DialogBody.plainMessage(body))).build())
            .type(DialogType.confirmation(
                ActionButton.create(
                    configuredMessage("confirmation-confirm", "<red>Confirm", Map.of()),
                    Component.empty(),
                    150,
                    DialogAction.customClick((response, audience) -> {
                        if (audience instanceof Player confirmingPlayer) {
                            confirmingPlayer.getScheduler().run(plugin,
                                ignored -> confirmDeletion(confirmingPlayer, deletion), null);
                        }
                    }, callbackOptions)
                ),
                ActionButton.create(
                    configuredMessage("confirmation-cancel", "<gray>Cancel", Map.of()),
                    Component.empty(),
                    150,
                    null
                )
            )));
        player.showDialog(dialog);
    }

    private Component configuredMessage(String key, String fallback, Map<String, String> replacements) {
        return configuredMessage(key, fallback, replacements, Map.of());
    }

    private Component configuredMessage(String key, String fallback, Map<String, String> replacements,
                                        Map<String, Component> componentReplacements) {
        String value = plugin.getConfig().getString("messages." + key, fallback);
        return plugin.messages().component(value, replacements, componentReplacements);
    }

    private void confirmDeletion(Player player, PendingDeletion deletion) {
        if (!player.isOnline()) return;
        Inventory top = player.getOpenInventory().getTopInventory();
        if (!(top.getHolder() instanceof TrashHolder holder) || !holder.playerId().equals(player.getUniqueId())) return;
        Inventory source = deletion.inventory() == PendingDeletion.TRASH_INVENTORY
            ? top
            : player.getOpenInventory().getBottomInventory();
        if (deletion.slot() < 0 || deletion.slot() >= source.getSize()) return;
        ItemStack current = source.getItem(deletion.slot());
        if (current == null || current.getAmount() != deletion.item().getAmount() || !current.isSimilar(deletion.item())) return;
        if (deletion.inventory() == PendingDeletion.TRASH_INVENTORY) {
            finishDelete(player, top, deletion.slot(), current);
        } else {
            moveItemIntoTrash(player, top, source, deletion.slot(), current);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof TrashHolder holder)) return;
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!holder.playerId().equals(player.getUniqueId())) return;
        manager.snapshot(player, event.getInventory());
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof TrashHolder holder)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!holder.playerId().equals(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        TrashLayout layout = holder.layout();
        int topSize = event.getView().getTopInventory().getSize();
        boolean touchesControl = false;
        boolean touchesTrash = false;
        for (int slot : event.getRawSlots()) {
            if (slot >= layout.contentSize() && slot < topSize) touchesControl = true;
            if (slot < layout.contentSize()) touchesTrash = true;
        }
        if (touchesControl) {
            event.setCancelled(true);
            return;
        }
        if (touchesTrash) {
            player.getScheduler().run(plugin,
                ignored -> manager.snapshot(player, event.getView().getTopInventory()), null);
        }
    }

    @EventHandler
    public void onMove(InventoryMoveItemEvent event) {
        if (event.getSource().getHolder() instanceof TrashHolder || event.getDestination().getHolder() instanceof TrashHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        snapshotOpen(event.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) { closeAndSnapshot(event.getEntity()); }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) { closeAndSnapshot(event.getPlayer()); }

    private void snapshotOpen(Player player) {
        if (player.getOpenInventory().getTopInventory().getHolder() instanceof TrashHolder holder
            && holder.playerId().equals(player.getUniqueId())) {
            manager.snapshot(player, player.getOpenInventory().getTopInventory());
        }
    }

    private void closeAndSnapshot(Player player) {
        snapshotOpen(player);
        player.getScheduler().run(plugin, ignored -> {
            if (player.isOnline() && player.getOpenInventory().getTopInventory().getHolder() instanceof TrashHolder) {
                player.closeInventory(InventoryCloseEvent.Reason.PLUGIN);
            }
        }, null);
    }

    private record PendingDeletion(byte inventory, int slot, ItemStack item) {
        static final byte TRASH_INVENTORY = 0;
        static final byte PLAYER_INVENTORY = 1;
    }
}
