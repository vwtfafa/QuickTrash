package org.vwtfafa.quicktrash.model;

import java.util.Arrays;
import java.util.UUID;
import org.bukkit.inventory.ItemStack;

public final class TrashSession {
    /** Default content slots (2 rows). Kept for tests and persistence fallbacks. */
    public static final int SIZE = 18;

    private final UUID playerId;
    private final ItemStack[] items;
    private long expiresAt;

    public TrashSession(UUID playerId, long expiresAt) {
        this(playerId, expiresAt, SIZE);
    }

    public TrashSession(UUID playerId, long expiresAt, int size) {
        this.playerId = playerId;
        this.items = new ItemStack[Math.max(1, size)];
        this.expiresAt = expiresAt;
    }

    public UUID playerId() { return playerId; }
    public int size() { return items.length; }
    public ItemStack itemAt(int slot) { return items[slot]; }

    public void setItem(int slot, ItemStack item) {
        if (slot < 0 || slot >= items.length) return;
        items[slot] = item;
    }

    public ItemStack[] copyItems() { return Arrays.copyOf(items, items.length); }
    public long expiresAt() { return expiresAt; }

    public int itemCount() {
        int count = 0;
        for (ItemStack item : items) {
            if (item != null && !item.isEmpty()) count += item.getAmount();
        }
        return count;
    }

    public void refresh(long expiresAt) { this.expiresAt = expiresAt; }
    public void clear() { Arrays.fill(items, null); }
}
