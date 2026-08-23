package org.vwtfafa.quicktrash.manager;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public final class ValuableItemChecker {
    private final TrashManager manager;
    private final AtomicReference<MaterialRule> cachedRule = new AtomicReference<>();

    public ValuableItemChecker(TrashManager manager) { this.manager = manager; }

    public boolean isValuable(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        if (cachedRule().matches(item.getType())) return true;
        var meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.hasCustomName()
            || meta.hasEnchants()
            || meta.hasCustomModelDataComponent()
            || !meta.getPersistentDataContainer().getKeys().isEmpty()
            || item.getType() == Material.ENCHANTED_GOLDEN_APPLE;
    }

    public void invalidate() { cachedRule.set(null); }

    private MaterialRule cachedRule() {
        MaterialRule cached = cachedRule.get();
        if (cached != null) return cached;
        List<String> configured = manager.plugin().getConfig().getStringList("valuable-items.materials");
        Set<Material> materials = configured.stream()
            .map(this::parseMaterial)
            .filter(Objects::nonNull)
            .collect(Collectors.toUnmodifiableSet());
        String mode = manager.plugin().getConfig().getString("valuable-items.mode", "WHITELIST");
        boolean blacklist = "BLACKLIST".equalsIgnoreCase(mode);
        if (blacklist && materials.isEmpty()) {
            manager.plugin().getLogger().warning(
                "valuable-items.mode is BLACKLIST but valuable-items.materials is empty; every item would require confirmation. Treating as disabled.");
            materials = Set.of(Material.AIR);
            blacklist = false;
        }
        MaterialRule rule = new MaterialRule(materials, blacklist);
        cachedRule.compareAndSet(null, rule);
        return rule;
    }

    private Material parseMaterial(String value) {
        Material material = Material.matchMaterial(value.toUpperCase(Locale.ROOT));
        if (material == null) {
            manager.plugin().getLogger().warning("Unknown material in valuable-items.materials: " + value);
            return null;
        }
        return material;
    }

    record MaterialRule(Set<Material> materials, boolean blacklist) {
        boolean matches(Material material) { return blacklist != materials.contains(material); }
    }
}
