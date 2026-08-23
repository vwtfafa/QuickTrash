package org.vwtfafa.quicktrash.command;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.List;
import org.vwtfafa.quicktrash.QuickTrash;

public final class QuickTrashCommand implements BasicCommand {
    private final QuickTrash plugin;
    public QuickTrashCommand(QuickTrash plugin) { this.plugin = plugin; }

    @Override public void execute(CommandSourceStack source, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("version")) {
            plugin.messages().send(source.getSender(), "version", java.util.Map.of("version", plugin.getPluginMeta().getVersion()));
            return;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            if (!source.getSender().hasPermission("quicktrash.admin")) {
                plugin.messages().send(source.getSender(), "no-permission");
                return;
            }
            plugin.reloadConfig();
            plugin.valuableItems().invalidate();
            plugin.messages().send(source.getSender(), "reload");
            return;
        }
        if (args[0].equalsIgnoreCase("stats")) {
            var sender = source.getSender();
            long personal = sender instanceof org.bukkit.entity.Player player ? plugin.stats().playerTotal(player.getUniqueId()) : 0L;
            plugin.messages().send(sender, "stats", java.util.Map.of(
                "player", String.valueOf(personal),
                "total", String.valueOf(plugin.stats().total())));
            return;
        }
        plugin.messages().send(source.getSender(), "unknown-subcommand");
    }

    @Override public java.util.Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (args.length != 1) return List.of();
        List<String> options = source.getSender().hasPermission("quicktrash.admin")
            ? List.of("reload", "version", "stats")
            : List.of("stats");
        return options.stream().filter(option -> option.startsWith(args[0].toLowerCase())).toList();
    }

    @Override public String permission() { return "quicktrash.use"; }
}
