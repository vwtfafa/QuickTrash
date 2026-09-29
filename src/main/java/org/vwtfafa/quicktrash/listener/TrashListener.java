package org.vwtfafa.quicktrash.listener;

import java.time.Duration;
import java.util.Map;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.vwtfafa.quicktrash.QuickTrash;
import org.vwtfafa.quicktrash.gui.TrashHolder;
import org.vwtfafa.quicktrash.manager.TrashManager;
import org.vwtfafa.quicktrash.model.TrashSession;
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
            || event.getClick() == ClickType.SWAP_OFFHAND || event.getClick().isCreativeAction()
            || event.getClick() == ClickType.CONTROL_DROP) {
            event.setCancelled(true);
            return;
        }
        if (event.getClickedInventory() == top) {
            if (event.getSlot() >= TrashSession.SIZE || event.getClick().isShiftClick()) {
                event.setCancelled(true);
            }
            if (event.getSlot() < TrashSession.SIZE && event.getClick().isShiftClick()) deleteFromTrash(player, top, event.getSlot(), event.getCurrentItem());
            return;
        }
        if (event.getClickedInventory() == event.getView().getBottomInventory() && event.getClick().isShiftClick()) {
            event.setCancelled(true);
            moveFromPlayer(player, event);
        }
    }

    private void deleteFromTrash(Player player, Inventory top, int slot, ItemStack item) {
        if (item == null || item.getType().isAir()) return;
        if (requiresConfirmation(player, PendingDeletion.TRASH_INVENTORY, slot, item)) return;
        deleteTrashItem(player, top, slot, item);
    }

    private void deleteTrashItem(Player player, Inventory top, int slot, ItemStack item) {
        top.setItem(slot, null);
        plugin.stats().add(player.getUniqueId(), item.getAmount());
        sendDeleted(player, item);
        org.vwtfafa.quicktrash.util.Sounds.play(plugin, player, "gui.sounds.delete");
    }

    private void moveFromPlayer(Player player, InventoryClickEvent event) {
        ItemStack item = event.getCurrentItem();
        Inventory bottom = event.getView().getBottomInventory();
        if (event.getClickedInventory() != bottom || item == null || item.getType().isAir()) return;
        int sourceSlot = event.getSlot();
        if (requiresConfirmation(player, PendingDeletion.PLAYER_INVENTORY, sourceSlot, item)) return;
        moveItemIntoTrash(player, event.getView().getTopInventory(), bottom, sourceSlot, item);
    }

    private void moveItemIntoTrash(Player player, Inventory top, Inventory bottom, int sourceSlot, ItemStack item) {
        int expected = item.getAmount();
        ItemStack deposit = item.clone();
        int leftover = manager.put(player, top, deposit);
        if (leftover == -1) {
            plugin.messages().send(player, "no-space");
            return;
        }
        ItemStack current = bottom.getItem(sourceSlot);
        if (current == null || current.getAmount() != expected || !current.isSimilar(item)) return;
        if (leftover == 0) {
            bottom.setItem(sourceSlot, null);
        } else {
            ItemStack remainder = item.clone();
            remainder.setAmount(leftover);
            bottom.setItem(sourceSlot, remainder);
        }
    }

    private boolean requiresConfirmation(Player player, byte inventoryKind, int slot, ItemStack item) {
        if (player.hasPermission("quicktrash.bypass") || !plugin.getConfig().getBoolean("valuable-items.enabled", true)
            || !plugin.getConfig().getBoolean("valuable-items.require-confirmation", true)
            || !plugin.valuableItems().isValuable(item)) return false;
        int timeout = Math.max(1, plugin.getConfig().getInt("valuable-items.confirmation-timeout-seconds", 5));
        PendingDeletion deletion = new PendingDeletion(inventoryKind, slot, item.clone());
        showConfirmation(player, deletion, timeout);
        return true;
    }

    private void showConfirmation(Player player, PendingDeletion deletion, int timeout) {
        Map<String, String> replacements = Map.of(
            "amount", String.valueOf(deletion.item().getAmount()),
            "item", deletion.item().getType().name(),
            "seconds", String.valueOf(timeout)
        );
        String bodyKey = deletion.inventory() == PendingDeletion.TRASH_INVENTORY
            ? "confirmation-delete-body"
            : "confirmation-move-body";
        String bodyFallback = deletion.inventory() == PendingDeletion.TRASH_INVENTORY
            ? "<gray>Delete {amount}x {item} permanently within {seconds}s?"
            : "<gray>Move {amount}x {item} into temporary trash within {seconds}s?";
        Component body = configuredMessage(bodyKey, bodyFallback, replacements);
        Component title = plugin.messages().message("valuable-warning");
        ClickCallback.Options callbackOptions = ClickCallback.Options.builder()
            .uses(1)
            .lifetime(Duration.ofSeconds(timeout))
            .build();
        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(title).body(java.util.List.of(DialogBody.plainMessage(body))).build())
            .type(DialogType.confirmation(
                ActionButton.create(
                    configuredMessage("confirmation-confirm", "<red>Confirm", Map.of()),
                    Component.empty(),
                    150,
                    DialogAction.customClick((response, audience) -> {
                        if (audience instanceof Player confirmingPlayer) {
                            plugin.getServer().getScheduler().runTask(plugin, () -> confirmDeletion(confirmingPlayer, deletion));
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
        String value = plugin.getConfig().getString("messages." + key, fallback);
        return plugin.messages().component(value, replacements);
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
            deleteTrashItem(player, top, deletion.slot(), current);
        } else {
            moveItemIntoTrash(player, top, source, deletion.slot(), current);
        }
    }

    private void sendDeleted(Player player, ItemStack item) {
        plugin.messages().actionbar(player, plugin.messages().deletedItems(item.getAmount(), item));
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof TrashHolder holder)) return;
        if (event.getPlayer() instanceof Player player) manager.snapshot(player, event.getInventory());
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof TrashHolder holder)) return;
        if (!(event.getWhoClicked() instanceof Player player) || !holder.playerId().equals(player.getUniqueId())) return;
        int topSize = event.getView().getTopInventory().getSize();
        boolean touchesFiller = event.getRawSlots().stream().anyMatch(slot -> slot >= TrashSession.SIZE && slot < topSize);
        if (touchesFiller) {
            event.setCancelled(true);
            return;
        }
        boolean touchesTrash = event.getRawSlots().stream().anyMatch(slot -> slot < TrashSession.SIZE);
        if (touchesTrash) {
            plugin.getServer().getScheduler().runTask(plugin, () -> manager.snapshot(player, event.getView().getTopInventory()));
        }
    }

    @EventHandler
    public void onMove(InventoryMoveItemEvent event) {
        if (event.getSource().getHolder() instanceof TrashHolder || event.getDestination().getHolder() instanceof TrashHolder) event.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        closeAndSnapshot(event.getPlayer());
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

    private record PendingDeletion(byte inventory, int slot, ItemStack item) {
        static final byte TRASH_INVENTORY = 0;
        static final byte PLAYER_INVENTORY = 1;
    }
}