package org.vwtfafa.quicktrash.util;

import org.bukkit.permissions.Permissible;
import org.vwtfafa.quicktrash.gui.TrashLayout;

/** Resolves trash content rows from permissions, falling back to the configured default. */
public final class RowsResolver {
    private RowsResolver() { }

    public static int resolve(Permissible permissible, int configuredRows) {
        for (int rows = TrashLayout.MAX_ROWS; rows >= TrashLayout.MIN_ROWS; rows--) {
            if (permissible.hasPermission("quicktrash.rows." + rows)) return rows;
        }
        return Math.max(TrashLayout.MIN_ROWS, Math.min(TrashLayout.MAX_ROWS, configuredRows));
    }
}
