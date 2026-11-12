package org.vwtfafa.quicktrash.util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.logging.Level;
import org.bukkit.entity.Player;
import org.vwtfafa.quicktrash.QuickTrash;

public final class UpdateChecker {
    private static final String RELEASES_API = "https://api.github.com/repos/vwtfafa/QuickTrash/releases/latest";
    private final QuickTrash plugin;
    private volatile String latestVersion;

    public UpdateChecker(QuickTrash plugin) { this.plugin = plugin; }

    public void checkAsync() {
        if (!plugin.getConfig().getBoolean("update-checker.enabled", true)) return;
        plugin.getServer().getAsyncScheduler().runNow(plugin, ignored -> {
            try {
                HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
                HttpRequest request = HttpRequest.newBuilder(URI.create(RELEASES_API))
                    .timeout(Duration.ofSeconds(5))
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "QuickTrash-UpdateChecker")
                    .GET()
                    .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) return;
                String tag = extractTag(response.body());
                if (tag == null || tag.isBlank()) return;
                String normalized = tag.startsWith("v") ? tag.substring(1) : tag;
                String current = plugin.getPluginMeta().getVersion();
                if (!normalized.equalsIgnoreCase(current)) {
                    latestVersion = normalized;
                    plugin.getLogger().info("Update available: " + normalized + " (current " + current + ")");
                }
            } catch (Exception exception) {
                plugin.getLogger().log(Level.FINE, "Update check failed", exception);
            }
        });
    }

    public void notifyPlayer(Player player) {
        String latest = latestVersion;
        if (latest == null || !player.hasPermission("quicktrash.admin")) return;
        plugin.messages().send(player, "update-available", Map.of("version", latest));
    }

    private static String extractTag(String json) {
        String key = "\"tag_name\"";
        int index = json.indexOf(key);
        if (index < 0) return null;
        int colon = json.indexOf(':', index + key.length());
        int firstQuote = json.indexOf('"', colon + 1);
        int secondQuote = json.indexOf('"', firstQuote + 1);
        if (firstQuote < 0 || secondQuote < 0) return null;
        return json.substring(firstQuote + 1, secondQuote);
    }
}
