package org.vwtfafa.quicktrash.command;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.vwtfafa.quicktrash.QuickTrash;
import org.vwtfafa.quicktrash.gui.TrashHolder;
import org.vwtfafa.quicktrash.model.RecycleEntry;

public final class QuickTrashCommand implements BasicCommand {
    private static final DateTimeFormatter TIME =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private final QuickTrash plugin;
    public QuickTrashCommand(QuickTrash plugin) { this.plugin = plugin; }

    @Override public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        if (args.length == 0 || args[0].equalsIgnoreCase("version")) {
            plugin.messages().send(sender, "version", Map.of("version", plugin.getPluginMeta().getVersion()));
            return;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload" -> reload(sender);
            case "stats" -> stats(sender);
            case "clear" -> clear(sender, args);
            case "open" -> open(sender, args);
            case "history" -> history(sender, args);
            case "restore" -> restore(sender, args);
            default -> plugin.messages().send(sender, "unknown-subcommand");
        }
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission("quicktrash.admin")) {
            plugin.messages().send(sender, "no-permission");
            return;
        }
        plugin.reloadPluginConfig();
        plugin.messages().send(sender, "reload");
    }

    private void stats(CommandSender sender) {
        long personal = sender instanceof Player player ? plugin.stats().playerTotal(player.getUniqueId()) : 0L;
        plugin.messages().send(sender, "stats", Map.of(
            "player", String.valueOf(personal),
            "total", String.valueOf(plugin.stats().total())));
    }

    private void clear(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            if (!sender.hasPermission("quicktrash.admin.others")) {
                plugin.messages().send(sender, "no-permission");
                return;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                plugin.messages().send(sender, "player-not-found");
                return;
            }
            plugin.trash().clearPlayer(target.getUniqueId());
            if (target.getOpenInventory().getTopInventory().getHolder() instanceof TrashHolder) {
                target.closeInventory();
            }
            plugin.messages().send(sender, "admin-cleared", Map.of("player", target.getName()));
            return;
        }
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "player-only");
            return;
        }
        if (!player.hasPermission("quicktrash.use")) {
            plugin.messages().send(player, "no-permission");
            return;
        }
        if (player.getOpenInventory().getTopInventory().getHolder() instanceof TrashHolder holder
            && holder.playerId().equals(player.getUniqueId())) {
            plugin.trash().clearNow(player, player.getOpenInventory().getTopInventory(), true);
        } else {
            plugin.trash().clearPlayer(player.getUniqueId());
            plugin.messages().send(player, "trash-cleared-manual");
        }
    }

    private void open(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "player-only");
            return;
        }
        if (args.length < 2) {
            plugin.trash().open(player);
            return;
        }
        if (!player.hasPermission("quicktrash.admin.others")) {
            plugin.messages().send(player, "no-permission");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            plugin.messages().send(player, "player-not-found");
            return;
        }
        plugin.trash().openFor(player, target.getUniqueId());
        plugin.messages().send(player, "admin-opened", Map.of("player", target.getName()));
    }

    private void history(CommandSender sender, String[] args) {
        if (!sender.hasPermission("quicktrash.admin.recycle")) {
            plugin.messages().send(sender, "no-permission");
            return;
        }
        UUID filter = null;
        int page = 0;
        if (args.length >= 2) {
            Player online = Bukkit.getPlayerExact(args[1]);
            if (online != null) {
                filter = online.getUniqueId();
                if (args.length >= 3) page = Math.max(0, parseInt(args[2], 1) - 1);
            } else if (args[1].chars().allMatch(Character::isDigit)) {
                page = Math.max(0, parseInt(args[1], 1) - 1);
            } else {
                plugin.messages().send(sender, "player-not-found");
                return;
            }
        }
        List<RecycleEntry> entries = plugin.recycle().history(filter, page, 8);
        if (entries.isEmpty()) {
            plugin.messages().send(sender, "recycle-empty");
            return;
        }
        plugin.messages().send(sender, "recycle-header", Map.of("page", String.valueOf(page + 1)));
        for (RecycleEntry entry : entries) {
            plugin.messages().send(sender, "recycle-line", Map.of(
                "id", String.valueOf(entry.id()),
                "player", entry.playerName(),
                "amount", String.valueOf(entry.amount()),
                "item", entry.material(),
                "reason", entry.reason(),
                "time", TIME.format(Instant.ofEpochMilli(entry.timestamp()))
            ));
        }
    }

    private void restore(CommandSender sender, String[] args) {
        if (!sender.hasPermission("quicktrash.admin.recycle")) {
            plugin.messages().send(sender, "no-permission");
            return;
        }
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "player-only");
            return;
        }
        if (args.length < 2) {
            plugin.messages().send(sender, "recycle-restore-usage");
            return;
        }
        long id = parseLong(args[1], -1);
        RecycleEntry entry = plugin.recycle().find(id);
        if (entry == null) {
            plugin.messages().send(sender, "recycle-not-found");
            return;
        }
        ItemStack item = plugin.recycle().restoreItem(entry);
        if (item == null || item.isEmpty()) {
            plugin.messages().send(sender, "recycle-not-found");
            return;
        }
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        if (!leftover.isEmpty()) {
            leftover.values().forEach(stack -> player.getWorld().dropItemNaturally(player.getLocation(), stack));
        }
        plugin.recycle().remove(id);
        plugin.messages().send(sender, "recycle-restored", Map.of(
            "id", String.valueOf(id),
            "amount", String.valueOf(entry.amount()),
            "item", entry.material()
        ));
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static long parseLong(String value, long fallback) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    @Override public java.util.Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (args.length == 1) {
            List<String> options = new java.util.ArrayList<>(List.of("version", "stats", "clear"));
            if (source.getSender().hasPermission("quicktrash.admin")) options.add("reload");
            if (source.getSender().hasPermission("quicktrash.admin.others")) options.add("open");
            if (source.getSender().hasPermission("quicktrash.admin.recycle")) {
                options.add("history");
                options.add("restore");
            }
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return options.stream().filter(option -> option.startsWith(prefix)).toList();
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            boolean playerArg = ("clear".equals(sub) || "open".equals(sub))
                && source.getSender().hasPermission("quicktrash.admin.others");
            boolean historyArg = "history".equals(sub)
                && source.getSender().hasPermission("quicktrash.admin.recycle");
            if (playerArg || historyArg) {
                String prefix = args[1].toLowerCase(Locale.ROOT);
                return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .toList();
            }
        }
        return List.of();
    }

    @Override public String permission() { return null; }
}
