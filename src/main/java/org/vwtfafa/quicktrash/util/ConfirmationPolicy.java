package org.vwtfafa.quicktrash.util;

/** Pure rules for whether a trash action needs a confirmation dialog. */
public final class ConfirmationPolicy {
    private ConfirmationPolicy() { }

    public static boolean requires(
        boolean bypassPermission,
        boolean playerConfirmEnabled,
        boolean confirmAllDeletes,
        boolean valuableProtectionEnabled,
        boolean requireConfirmation,
        boolean itemIsValuable
    ) {
        if (bypassPermission || !playerConfirmEnabled) return false;
        if (confirmAllDeletes) return true;
        return valuableProtectionEnabled && requireConfirmation && itemIsValuable;
    }
}
