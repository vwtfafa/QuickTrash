package org.vwtfafa.quicktrash.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class TrashSessionTest {
    private final TrashSession session = new TrashSession(UUID.randomUUID(), 1000L);

    @Test
    void refreshMovesExpiryForward() {
        session.refresh(5000L);
        assertEquals(5000L, session.expiresAt());
    }

    @Test
    void itemCountCountsOnlyRealItems() {
        session.setItem(0, stack(32, false));
        session.setItem(1, stack(1, true));
        session.setItem(5, stack(8, false));
        assertEquals(40, session.itemCount());
    }

    @Test
    void clearRemovesAllItems() {
        session.setItem(0, stack(64, false));
        session.clear();
        assertEquals(0, session.itemCount());
    }

    private ItemStack stack(int amount, boolean air) {
        Material material = Mockito.mock(Material.class);
        Mockito.when(material.isAir()).thenReturn(air);
        ItemStack item = Mockito.mock(ItemStack.class);
        Mockito.when(item.getAmount()).thenReturn(amount);
        Mockito.when(item.getType()).thenReturn(material);
        return item;
    }
}
