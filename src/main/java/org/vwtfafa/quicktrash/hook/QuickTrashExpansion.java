package org.vwtfafa.quicktrash.hook;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vwtfafa.quicktrash.QuickTrash;

public final class QuickTrashExpansion extends PlaceholderExpansion {
    private final QuickTrash plugin;

    public QuickTrashExpansion(QuickTrash plugin) { this.plugin = plugin; }

    @Override public @NotNull String getIdentifier() { return "quicktrash"; }
    @Override public @NotNull String getAuthor() { return "QuickTrash"; }
    @Override public @NotNull String getVersion() { return plugin.getPluginMeta().getVersion(); }
    @Override public boolean persist() { return true; }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (params.equalsIgnoreCase("total")) return String.valueOf(plugin.stats().total());
        if (player == null) return "";
        if (params.equalsIgnoreCase("player") || params.equalsIgnoreCase("deleted")) {
            return String.valueOf(plugin.stats().playerTotal(player.getUniqueId()));
        }
        if (params.equalsIgnoreCase("sessions")) return String.valueOf(plugin.trash().activeSessions());
        return null;
    }
}
