package org.vwtfafa.quicktrash.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class UndoBufferTest {
    @Test
    void countsAndExpires() {
        ItemStack item = Mockito.mock(ItemStack.class);
        Mockito.when(item.isEmpty()).thenReturn(false);
        Mockito.when(item.getAmount()).thenReturn(12);
        UndoBuffer buffer = new UndoBuffer(new ItemStack[] {item, null}, 1000L);
        assertEquals(12, buffer.itemCount());
        assertFalse(buffer.expired(999L));
        assertTrue(buffer.expired(1001L));
    }
}
