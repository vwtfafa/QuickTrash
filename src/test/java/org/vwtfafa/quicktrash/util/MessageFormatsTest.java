package org.vwtfafa.quicktrash.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MessageFormatsTest {
    @Test
    void detectsMiniMessageTags() {
        assertTrue(MessageFormats.looksLikeMiniMessage("<red>valuable!</red>"));
        assertTrue(MessageFormats.looksLikeMiniMessage("<dark_gray>[<aqua>QuickTrash<dark_gray>] "));
        assertTrue(MessageFormats.looksLikeMiniMessage("<hover:show_text:'hi'>x</hover>"));
    }

    @Test
    void treatsLegacyCodesAsLegacy() {
        assertFalse(MessageFormats.looksLikeMiniMessage("&8[&bQuickTrash&8] "));
        assertFalse(MessageFormats.looksLikeMiniMessage("&c{amount}x {item} deleted."));
    }

    @Test
    void treatsPlainTextAsLegacy() {
        assertFalse(MessageFormats.looksLikeMiniMessage("plain text without formatting"));
        assertFalse(MessageFormats.looksLikeMiniMessage("math: 3 < 4 but 5 > 2"));
    }
}
