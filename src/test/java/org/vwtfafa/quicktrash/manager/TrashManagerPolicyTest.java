package org.vwtfafa.quicktrash.manager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TrashManagerPolicyTest {
    @Test
    void deleteMarksNeedFlushAfterDebounce() {
        assertFalse(TrashManager.shouldFlush(true, false, 1_000L, 900L));
        assertTrue(TrashManager.shouldFlush(true, false, 4_000L, 900L));
        assertTrue(TrashManager.shouldFlush(false, true, 1_000L, 900L));
    }

    @Test
    void sessionExpiryUsesInclusiveDeadline() {
        assertFalse(TrashManager.isExpired(2_000L, 1_999L));
        assertTrue(TrashManager.isExpired(2_000L, 2_000L));
        assertTrue(TrashManager.isExpired(2_000L, 2_001L));
    }
}
