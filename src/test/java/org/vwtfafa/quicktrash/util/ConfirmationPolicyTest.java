package org.vwtfafa.quicktrash.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ConfirmationPolicyTest {
    @Test
    void bypassOrPlayerOptOutSkipsDialog() {
        assertFalse(ConfirmationPolicy.requires(true, true, false, true, true, true));
        assertFalse(ConfirmationPolicy.requires(false, false, true, true, true, true));
    }

    @Test
    void confirmAllOrValuableRequiresDialog() {
        assertTrue(ConfirmationPolicy.requires(false, true, true, false, false, false));
        assertTrue(ConfirmationPolicy.requires(false, true, false, true, true, true));
        assertFalse(ConfirmationPolicy.requires(false, true, false, true, true, false));
    }
}
