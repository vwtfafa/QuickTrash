package org.vwtfafa.quicktrash.util;

import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class MessageService {
    private final JavaPlugin plugin;
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();

    public MessageService(JavaPlugin plugin) { this.plugin = plugin; }

    public String text(String path, Map<String, String> replacements) {
        String value = plugin.getConfig().getString("messages." + path, path);
        for (var entry : replacements.entrySet()) value = value.replace("{" + entry.getKey() + "}", entry.getValue());
        return color(value);
    }

    public String text(String path) { return text(path, Map.of()); }
    public String color(String value) {
        return serializer.serialize(LegacyComponentSerializer.legacyAmpersand().deserialize(value));
    }
    public Component component(String value) { return serializer.deserialize(color(value)); }
    public void send(CommandSender sender, String path) { sender.sendMessage(text("prefix") + text(path)); }
    public void send(CommandSender sender, String path, Map<String, String> replacements) {
        sender.sendMessage(text("prefix") + text(path, replacements));
    }
    public void send(Player player, String path, Map<String, String> replacements) { player.sendMessage(text("prefix") + text(path, replacements)); }
    public void actionbar(Player player, String path, Map<String, String> replacements) {
        Component component = serializer.deserialize(text(path, replacements));
        player.sendActionBar(component);
    }
}