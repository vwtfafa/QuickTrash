package org.vwtfafa.quicktrash.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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

    private ItemStack item(Component effectiveName) {
        ItemStack item = mock(ItemStack.class);
        when(item.effectiveName()).thenReturn(effectiveName);
        return item;
    }

    @Test
    void resolvesMiniMessageWithComponentPlaceholder() {
        MessageService service = serviceWith("<red>{amount}x {item} deleted.");
        Component component = service.deletedItems(5, item(Component.text("Diamond")));
        String serialized = MiniMessage.miniMessage().serialize(component);
        assertTrue(serialized.contains("5"), "expected amount in: " + serialized);
        assertTrue(serialized.contains("Diamond"), "expected item in: " + serialized);
        assertFalse(serialized.contains("{amount}"), "unresolved placeholder in: " + serialized);
        assertFalse(serialized.contains("{item}"), "unresolved placeholder in: " + serialized);
    }

    @Test
    void resolvesLegacyFormat() {
        MessageService service = serviceWith("&c{amount}x {item} deleted.");
        Component component = service.deletedItems(2, item(Component.text("DIRT")));
        String plain = PlainTextComponentSerializer.plainText().serialize(component);
        assertTrue(plain.contains("2"), "expected amount in: " + plain);
        assertTrue(plain.contains("DIRT"), "expected item in: " + plain);
        assertFalse(plain.contains("{amount}"), "unresolved placeholder in: " + plain);
        assertFalse(plain.contains("{item}"), "unresolved placeholder in: " + plain);
    }
}
