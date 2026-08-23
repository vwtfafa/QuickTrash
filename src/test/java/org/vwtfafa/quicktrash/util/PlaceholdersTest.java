package org.vwtfafa.quicktrash.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PlaceholdersTest {
    @Test
    void appliesSingleReplacement() {
        Map<String, String> replacements = new HashMap<>();
        replacements.put("amount", "5");
        assertEquals("5x deleted", Placeholders.apply("{amount}x deleted", replacements));
    }

    @Test
    void appliesMultipleReplacements() {
        Map<String, String> replacements = new HashMap<>();
        replacements.put("player", "10");
        replacements.put("total", "99");
        assertEquals("you: 10 total: 99", Placeholders.apply("you: {player} total: {total}", replacements));
    }

    @Test
    void leavesUnknownPlaceholdersUntouched() {
        assertEquals("{unknown} value", Placeholders.apply("{unknown} value", Map.of()));
    }
}
