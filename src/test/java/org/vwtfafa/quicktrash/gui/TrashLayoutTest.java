package org.vwtfafa.quicktrash.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TrashLayoutTest {
    @Test
    void defaultRowsMatchLegacyGeometry() {
        TrashLayout layout = TrashLayout.fromRows(2);
        assertEquals(18, layout.contentSize());
        assertEquals(27, layout.inventorySize());
        assertEquals(21, layout.undoSlot());
        assertEquals(22, layout.infoSlot());
        assertEquals(23, layout.clearSlot());
        assertTrue(layout.isContentSlot(17));
        assertFalse(layout.isContentSlot(18));
        assertTrue(layout.isControlSlot(22));
    }

    @Test
    void clampsRowsToSupportedRange() {
        assertEquals(1, TrashLayout.fromRows(0).rows());
        assertEquals(5, TrashLayout.fromRows(99).rows());
        assertEquals(45, TrashLayout.fromRows(5).contentSize());
        assertEquals(54, TrashLayout.fromRows(5).inventorySize());
    }
}
