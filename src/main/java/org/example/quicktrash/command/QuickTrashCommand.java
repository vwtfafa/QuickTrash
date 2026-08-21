package org.example.quicktrash.command;

import java.util.List;
import org.example.quicktrash.QuickTrash;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

public final class QuickTrashCommand implements CommandExecutor, TabCompleter {
    private final QuickTrash plugin;
    public QuickTrashCommand(QuickTrash plugin) { this.plugin = plugin; }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("version")) {
            plugin.messages().send(sender, "version", java.util.Map.of("version", plugin.getPluginMeta().getVersion()));
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("quicktrash.admin")) { plugin.messages().send(sender, "no-permission"); return true; }
            plugin.reloadConfig();
            plugin.messages().send(sender, "reload");
            return true;
        }
        return false;
    }

    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("quicktrash.admin")) return List.of("reload", "version");
        return List.of();
    }
}
