package org.vwtfafa.quicktrash.util;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class Sounds {
    private static final Map<String, Sound> CACHE = new ConcurrentHashMap<>();

    private Sounds() { }

    public static void invalidate() { CACHE.clear(); }

    public static void play(JavaPlugin plugin, Player player, String path) {
        String name = plugin.getConfig().getString(path, "");
        if (name == null || name.isBlank()) return;
        Sound sound = resolve(plugin, name);
        if (sound == null) return;
        float volume = (float) plugin.getConfig().getDouble("gui.sounds.volume", 1.0D);
        float pitch = (float) plugin.getConfig().getDouble("gui.sounds.pitch", 1.0D);
        player.playSound(player, sound, volume, pitch);
    }

    private static Sound resolve(JavaPlugin plugin, String name) {
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        Sound cached = CACHE.get(normalized);
        if (cached != null) return cached;

        NamespacedKey key = normalized.indexOf(':') >= 0
            ? NamespacedKey.fromString(normalized)
            : NamespacedKey.minecraft(normalized);
        if (key == null) {
            plugin.getLogger().warning("Invalid sound key in config: " + name);
            return null;
        }
        Sound sound = RegistryAccess.registryAccess().getRegistry(RegistryKey.SOUND_EVENT).get(key);
        if (sound == null) {
            plugin.getLogger().warning("Unknown sound in config: " + name);
            return null;
        }
        CACHE.put(normalized, sound);
        return sound;
    }
}
