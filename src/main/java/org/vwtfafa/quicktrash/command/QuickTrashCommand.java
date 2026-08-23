package org.vwtfafa.quicktrash.command;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.List;
import org.vwtfafa.quicktrash.QuickTrash;

public final class QuickTrashCommand implements BasicCommand {
    private static final List<String> SUB_COMMANDS = List.of("reload", "version");
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
    }

    @Override public java.util.Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (args.length == 1 && source.getSender().hasPermission("quicktrash.admin")) {
            return SUB_COMMANDS.stream().filter(option -> option.startsWith(args[0].toLowerCase())).toList();
        }
        return List.of();
    }

    @Override public String permission() { return "quicktrash.use"; }
}
