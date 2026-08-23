package org.vwtfafa.quicktrash.gui;

import java.util.UUID;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class TrashHolder implements InventoryHolder {
    public static final int INVENTORY_SIZE = 27;
    public static final int INFO_SLOT = 22;
    private final UUID playerId;
    private Inventory inventory;

    public TrashHolder(UUID playerId) { this.playerId = playerId; }
    public UUID playerId() { return playerId; }
    public void inventory(Inventory inventory) { this.inventory = inventory; }
    @Override public Inventory getInventory() { return inventory; }
}