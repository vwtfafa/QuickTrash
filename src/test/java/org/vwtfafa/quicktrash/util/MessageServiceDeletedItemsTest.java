package org.vwtfafa.quicktrash.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;

class MessageServiceDeletedItemsTest {
    private MessageService serviceWith(String itemDeletedFormat) {
        JavaPlugin plugin = mock(JavaPlugin.class);
        YamlConfiguration config = new YamlConfiguration();
        config.set("messages.item-deleted", itemDeletedFormat);
        when(plugin.getConfig()).thenReturn(config);
        return new MessageService(plugin);
    }

    @Test
    void normalizesCurlyPlaceholdersToMiniMessageTags() {
        assertEquals("<red><amount>x <item> deleted.",
            MessageService.normalizeDeletedFormat("<red>{amount}x {item} deleted."));
    }

    @Test
    void keepsExistingMiniMessageTagsUntouched() {
        assertEquals("<red><amount>x <item> deleted.",
            MessageService.normalizeDeletedFormat("<red><amount>x <item> deleted."));
    }

    @Test
    void normalizedFormatResolvesWithoutLiterals() {
        String normalized = MessageService.normalizeDeletedFormat("<red>{amount}x {item} deleted.");
        Component component = MiniMessage.miniMessage().deserialize(normalized,
            Placeholder.unparsed("amount", "5"),
            Placeholder.component("item", Component.text("DIRT")));
        String serialized = MiniMessage.miniMessage().serialize(component);
        assertTrue(serialized.contains("5"), "expected amount in: " + serialized);
        assertTrue(serialized.contains("DIRT"), "expected item in: " + serialized);
        assertFalse(serialized.contains("{amount}"), "unresolved placeholder in: " + serialized);
        assertFalse(serialized.contains("{item}"), "unresolved placeholder in: " + serialized);
    }

    @Test
    void resolvesLegacyFormat() {
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.DIRT);
        MessageService service = serviceWith("&c{amount}x {item} deleted.");
        Component component = service.deletedItems(2, item);
        String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
            .serialize(component);
        assertTrue(plain.contains("2"), "expected amount in: " + plain);
        assertFalse(plain.contains("{amount}"), "unresolved placeholder in: " + plain);
        assertFalse(plain.contains("{item}"), "unresolved placeholder in: " + plain);
    }
}
