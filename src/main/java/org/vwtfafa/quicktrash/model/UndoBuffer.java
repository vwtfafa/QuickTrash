package org.vwtfafa.quicktrash.model;

import java.util.Arrays;
import org.bukkit.inventory.ItemStack;

/** Snapshot of items that can be restored after a clear. */
public final class UndoBuffer {
    private final ItemStack[] items;
    private final long expiresAt;

    public UndoBuffer(ItemStack[] items, long expiresAt) {
        this.items = Arrays.copyOf(items, items.length);
        this.expiresAt = expiresAt;
    }

    public ItemStack[] items() { return Arrays.copyOf(items, items.length); }
    public long expiresAt() { return expiresAt; }
    public boolean expired(long now) { return now > expiresAt; }

    public int itemCount() {
        int count = 0;
        for (ItemStack item : items) {
            if (item != null && !item.isEmpty()) count += item.getAmount();
        }
        return count;
    }
}
