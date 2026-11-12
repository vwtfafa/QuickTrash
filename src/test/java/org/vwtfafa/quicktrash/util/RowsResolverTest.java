package org.vwtfafa.quicktrash.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.bukkit.permissions.Permissible;
import org.junit.jupiter.api.Test;

class RowsResolverTest {
    @Test
    void usesHighestRowsPermission() {
        Permissible player = mock(Permissible.class);
        when(player.hasPermission("quicktrash.rows.5")).thenReturn(false);
        when(player.hasPermission("quicktrash.rows.4")).thenReturn(false);
        when(player.hasPermission("quicktrash.rows.3")).thenReturn(true);
        when(player.hasPermission("quicktrash.rows.2")).thenReturn(true);
        when(player.hasPermission("quicktrash.rows.1")).thenReturn(false);
        assertEquals(3, RowsResolver.resolve(player, 2));
    }

    @Test
    void fallsBackToConfiguredRows() {
        Permissible player = mock(Permissible.class);
        assertEquals(2, RowsResolver.resolve(player, 2));
        assertEquals(1, RowsResolver.resolve(player, 0));
        assertEquals(5, RowsResolver.resolve(player, 99));
    }
}
