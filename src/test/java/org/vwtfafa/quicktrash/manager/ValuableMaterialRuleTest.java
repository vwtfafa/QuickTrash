package org.vwtfafa.quicktrash.manager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.bukkit.Material;
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
}
