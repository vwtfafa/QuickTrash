package org.vwtfafa.quicktrash.util;

import java.util.regex.Pattern;

public final class MessageFormats {
    private static final Pattern MINI_MESSAGE_TAG = Pattern.compile("</?[a-zA-Z#][^<>]*>");

    private MessageFormats() { }

    public static boolean looksLikeMiniMessage(String value) { return MINI_MESSAGE_TAG.matcher(value).find(); }
}
