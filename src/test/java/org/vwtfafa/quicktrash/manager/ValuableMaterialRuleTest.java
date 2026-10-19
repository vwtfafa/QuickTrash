package org.vwtfafa.quicktrash.manager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;
import io.papermc.paper.datacomponent.DataComponentType;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

class ValuableMaterialRuleTest {
    @Test
    void whitelistMatchesOnlyConfiguredMaterials() {
        ValuableItemChecker.MaterialRule rule = new ValuableItemChecker.MaterialRule(Set.of(Material.DIAMOND), false);
        assertTrue(rule.matches(Material.DIAMOND));
        assertFalse(rule.matches(Material.DIRT));
    }

    @Test
    void blacklistMatchesUnconfiguredMaterials() {
        ValuableItemChecker.MaterialRule rule = new ValuableItemChecker.MaterialRule(Set.of(Material.DIAMOND), true);
        assertFalse(rule.matches(Material.DIAMOND));
        assertTrue(rule.matches(Material.DIRT));
    }

    @Test
    void customContainerContentsAreValuable() {
        ItemStack item = mock(ItemStack.class);
        DataComponentType component = mock(DataComponentType.class);
        when(item.isDataOverridden(component)).thenReturn(true);

        assertTrue(ValuableItemChecker.hasValuableDataComponents(item, Set.of(component)));
    }

    @Test
    void ordinaryItemsWithoutValuableOverridesAreNotValuable() {
        ItemStack item = mock(ItemStack.class);

        assertFalse(ValuableItemChecker.hasValuableDataComponents(item, Set.of(mock(DataComponentType.class))));
    }
}
