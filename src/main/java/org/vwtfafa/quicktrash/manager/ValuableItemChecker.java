package org.vwtfafa.quicktrash.manager;

import java.util.List;
import java.util.Locale;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public final class ValuableItemChecker {
    private final TrashManager manager;

    public ValuableItemChecker(TrashManager manager) { this.manager = manager; }

    public boolean isValuable(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        List<String> configured = manager.plugin().getConfig().getStringList("valuable-items.materials");
        if (configured.stream().map(value -> value.toUpperCase(Locale.ROOT)).anyMatch(value -> value.equals(item.getType().name()))) return true;
        var meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.hasCustomName()
            || meta.hasEnchants()
            || meta.hasCustomModelDataComponent()
            || !meta.getPersistentDataContainer().getKeys().isEmpty()
            || item.getType() == Material.ENCHANTED_GOLDEN_APPLE;
    }
}