package org.vwtfafa.quicktrash.manager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.papermc.paper.datacomponent.DataComponentType;
import java.util.Set;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ValuableDataComponentsTest {
    @Test
    void detectsOverriddenComponent() {
        ItemStack item = Mockito.mock(ItemStack.class);
        DataComponentType type = Mockito.mock(DataComponentType.class);
        Mockito.when(item.isDataOverridden(type)).thenReturn(true);
        assertTrue(ValuableItemChecker.hasValuableDataComponents(item, Set.of(type)));
    }

    @Test
    void ignoresWhenNoOverrides() {
        ItemStack item = Mockito.mock(ItemStack.class);
        DataComponentType type = Mockito.mock(DataComponentType.class);
        Mockito.when(item.isDataOverridden(type)).thenReturn(false);
        assertFalse(ValuableItemChecker.hasValuableDataComponents(item, Set.of(type)));
    }
}
