package org.vwtfafa.quicktrash.util;

import java.util.Map;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class MessageService {
    private static final Pattern MINI_MESSAGE_TAG = Pattern.compile("</?[a-zA-Z#][^<>]*>");
    private final JavaPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public MessageService(JavaPlugin plugin) { this.plugin = plugin; }

    public Component component(String value, Map<String, String> replacements) {
        String result = value;
        for (var entry : replacements.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        if (MINI_MESSAGE_TAG.matcher(result).find()) return miniMessage.deserialize(result);
        return LegacyComponentSerializer.legacyAmpersand().deserialize(result);
    }

    public Component component(String value) { return component(value, Map.of()); }

    public Component message(String path) { return component(raw(path)); }
    public Component message(String path, Map<String, String> replacements) { return component(raw(path), replacements); }
    private String raw(String path) { return plugin.getConfig().getString("messages." + path, path); }

    public void send(CommandSender sender, String path) { send(sender, path, Map.of()); }

    public void send(CommandSender sender, String path, Map<String, String> replacements) {
        sender.sendMessage(component(raw("prefix")).append(component(raw(path), replacements)));
    }

    public void actionbar(Player player, String path, Map<String, String> replacements) {
        player.sendActionBar(component(raw(path), replacements));
    }

    public void actionbar(Player player, Component component) { player.sendActionBar(component); }

    public Component deletedItems(int amount, ItemStack item) {
        String value = raw("item-deleted");
        if (MINI_MESSAGE_TAG.matcher(value).find()) {
            return miniMessage.deserialize(value,
                Placeholder.unparsed("amount", String.valueOf(amount)),
                Placeholder.component("item", Component.translatable(item.getType())));
        }
        return component(value, Map.of("amount", String.valueOf(amount), "item", item.getType().name()));
    }
}
