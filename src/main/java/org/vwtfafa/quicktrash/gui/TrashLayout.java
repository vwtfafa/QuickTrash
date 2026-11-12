package org.vwtfafa.quicktrash.gui;

/** Fixed geometry for the trash GUI: content rows plus one control row. */
public final class TrashLayout {
    public static final int MIN_ROWS = 1;
    public static final int MAX_ROWS = 5;
    public static final int DEFAULT_ROWS = 2;

    private final int rows;
    private final int contentSize;
    private final int inventorySize;
    private final int infoSlot;
    private final int clearSlot;
    private final int undoSlot;

    private TrashLayout(int rows, int contentSize, int inventorySize, int undoSlot, int infoSlot, int clearSlot) {
        this.rows = rows;
        this.contentSize = contentSize;
        this.inventorySize = inventorySize;
        this.undoSlot = undoSlot;
        this.infoSlot = infoSlot;
        this.clearSlot = clearSlot;
    }

    public static TrashLayout fromRows(int rows) {
        int clamped = Math.max(MIN_ROWS, Math.min(MAX_ROWS, rows));
        int contentSize = clamped * 9;
        int inventorySize = contentSize + 9;
        int controlStart = contentSize;
        int infoSlot = controlStart + 4;
        return new TrashLayout(clamped, contentSize, inventorySize, infoSlot - 1, infoSlot, infoSlot + 1);
    }

    public int rows() { return rows; }
    public int contentSize() { return contentSize; }
    public int inventorySize() { return inventorySize; }
    public int infoSlot() { return infoSlot; }
    public int clearSlot() { return clearSlot; }
    public int undoSlot() { return undoSlot; }

    public boolean isContentSlot(int slot) {
        return slot >= 0 && slot < contentSize;
    }

    public boolean isControlSlot(int slot) {
        return slot == infoSlot || slot == clearSlot || slot == undoSlot;
    }
}
