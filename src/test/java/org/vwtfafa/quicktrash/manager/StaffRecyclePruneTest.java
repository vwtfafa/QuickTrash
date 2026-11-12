package org.vwtfafa.quicktrash.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.vwtfafa.quicktrash.model.RecycleEntry;

class StaffRecyclePruneTest {
    @Test
    void keepsNewestWithinLimits() {
        Deque<RecycleEntry> entries = new ArrayDeque<>();
        long now = 1_000_000L;
        entries.add(entry(1, now - 10_000L));
        entries.add(entry(2, now - 5_000L));
        entries.add(entry(3, now - 1_000L));

        prune(entries, now, 8_000L, 2);

        assertEquals(2, entries.size());
        assertEquals(2L, entries.peekFirst().id());
        assertEquals(3L, entries.peekLast().id());
    }

    @Test
    void findReturnsMatchingEntry() {
        RecycleEntry first = entry(9, 100L);
        RecycleEntry second = entry(11, 200L);
        Deque<RecycleEntry> entries = new ArrayDeque<>();
        entries.add(first);
        entries.add(second);
        assertSame(second, find(entries, 11));
    }

    private static void prune(Deque<RecycleEntry> entries, long now, long retentionMs, int maxEntries) {
        long cutoff = now - retentionMs;
        while (!entries.isEmpty() && entries.peekFirst().timestamp() < cutoff) entries.removeFirst();
        while (entries.size() > maxEntries) entries.removeFirst();
    }

    private static RecycleEntry find(Deque<RecycleEntry> entries, long id) {
        for (RecycleEntry entry : entries) {
            if (entry.id() == id) return entry;
        }
        return null;
    }

    private static RecycleEntry entry(long id, long timestamp) {
        return new RecycleEntry(id, UUID.randomUUID(), "player", "DIAMOND", 1, "x", timestamp, "DELETE");
    }
}
