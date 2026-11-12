package org.vwtfafa.quicktrash.command;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.vwtfafa.quicktrash.QuickTrash;
import org.vwtfafa.quicktrash.util.Sounds;

public final class QuickTrashCommand implements BasicCommand {
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
            default -> plugin.messages().send(sender, "unknown-subcommand");
        }
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission("quicktrash.admin")) {
            plugin.messages().send(sender, "no-permission");
            return;
        }
        plugin.reloadConfig();
        plugin.trash().reloadSettings();
        plugin.valuableItems().invalidate();
        Sounds.invalidate();
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
            if (target.getOpenInventory().getTopInventory().getHolder() instanceof org.vwtfafa.quicktrash.gui.TrashHolder) {
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
        if (player.getOpenInventory().getTopInventory().getHolder() instanceof org.vwtfafa.quicktrash.gui.TrashHolder holder
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

    @Override public java.util.Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (args.length == 1) {
            List<String> options = source.getSender().hasPermission("quicktrash.admin")
                ? List.of("reload", "version", "stats", "clear", "open")
                : List.of("version", "stats", "clear");
            if (source.getSender().hasPermission("quicktrash.admin.others")
                && !options.contains("open")) {
                options = List.of("reload", "version", "stats", "clear", "open");
            }
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return options.stream().filter(option -> option.startsWith(prefix)).toList();
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("clear") || args[0].equalsIgnoreCase("open"))
            && source.getSender().hasPermission("quicktrash.admin.others")) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                .toList();
        }
        return List.of();
    }

    @Override public String permission() { return null; }
}
