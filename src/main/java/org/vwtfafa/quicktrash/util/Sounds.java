package org.vwtfafa.quicktrash.util;

import java.util.Locale;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class Sounds {
    private Sounds() { }

    public static void play(JavaPlugin plugin, Player player, String path) {
        String name = plugin.getConfig().getString(path, "");
        if (name == null || name.isBlank()) return;
        Sound sound = resolve(plugin, name);
        if (sound == null) return;
        player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
    }

    private static Sound resolve(JavaPlugin plugin, String name) {
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        NamespacedKey key = normalized.indexOf(':') >= 0
            ? NamespacedKey.fromString(normalized)
            : NamespacedKey.minecraft(normalized);
        if (key == null) {
            plugin.getLogger().warning("Invalid sound key in config: " + name);
            return null;
        }
        Sound sound = Registry.SOUND_EVENT.get(key);
        if (sound == null) plugin.getLogger().warning("Unknown sound in config: " + name);
        return sound;
    }
}
