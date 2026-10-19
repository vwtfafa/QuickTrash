package org.vwtfafa.quicktrash.util;

import java.util.HashMap;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class MessageService {
    private final JavaPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public MessageService(JavaPlugin plugin) { this.plugin = plugin; }

    public Component component(String value) { return component(value, Map.of()); }

    public Component component(String value, Map<String, String> replacements) {
        return component(value, replacements, Map.of());
    }

    /**
     * Renders a configured message. Text replacements are inserted as literal text, component
     * replacements keep their formatting (and stay client-side translated for item names).
     */
    public Component component(String value, Map<String, String> replacements, Map<String, Component> componentReplacements) {
        if (MessageFormats.looksLikeMiniMessage(value)) {
            TagResolver.Builder resolvers = TagResolver.builder();
            String template = value;
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                resolvers.resolver(Placeholder.unparsed(entry.getKey(), entry.getValue()));
                template = template.replace("{" + entry.getKey() + "}", "<" + entry.getKey() + ">");
            }
            for (Map.Entry<String, Component> entry : componentReplacements.entrySet()) {
                resolvers.resolver(Placeholder.component(entry.getKey(), entry.getValue()));
                template = template.replace("{" + entry.getKey() + "}", "<" + entry.getKey() + ">");
            }
            return miniMessage.deserialize(template, resolvers.build());
        }
        Map<String, String> flattened = new HashMap<>(replacements);
        componentReplacements.forEach((key, component) ->
            flattened.put(key, PlainTextComponentSerializer.plainText().serialize(component)));
        return LegacyComponentSerializer.legacyAmpersand().deserialize(Placeholders.apply(value, flattened));
    }

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
        return component(raw("item-deleted"),
            Map.of("amount", String.valueOf(amount)),
            Map.of("item", item.effectiveName()));
    }
}
