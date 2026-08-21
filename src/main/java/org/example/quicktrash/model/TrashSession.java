package org.example.quicktrash.model;

import java.util.UUID;
import org.bukkit.inventory.ItemStack;

public final class TrashSession {
    private final UUID playerId;
    private final ItemStack[] items = new ItemStack[18];
    private long expiresAt;

    public TrashSession(UUID playerId, long expiresAt) {
        this.playerId = playerId;
        this.expiresAt = expiresAt;
    }

    public UUID playerId() { return playerId; }
    public ItemStack[] items() { return items; }
    public long expiresAt() { return expiresAt; }
    public void refresh(long expiresAt) { this.expiresAt = expiresAt; }
    public void clear() { java.util.Arrays.fill(items, null); }
}
