package org.vwtfafa.quicktrash.model;

import java.util.Arrays;
import java.util.UUID;
import org.bukkit.inventory.ItemStack;

public final class TrashSession {
    public static final int SIZE = 18;
    private final UUID playerId;
    private final ItemStack[] items = new ItemStack[SIZE];
    private long expiresAt;

    public TrashSession(UUID playerId, long expiresAt) {
        this.playerId = playerId;
        this.expiresAt = expiresAt;
    }

    public UUID playerId() { return playerId; }
    public ItemStack itemAt(int slot) { return items[slot]; }
    public void setItem(int slot, ItemStack item) { items[slot] = item; }
    public ItemStack[] copyItems() { return Arrays.copyOf(items, SIZE); }
    public long expiresAt() { return expiresAt; }

    public int itemCount() {
        int count = 0;
        for (ItemStack item : items) {
            if (item != null && !item.getType().isAir()) count += item.getAmount();
        }
        return count;
    }

    public void refresh(long expiresAt) { this.expiresAt = expiresAt; }
    public void clear() { Arrays.fill(items, null); }
}