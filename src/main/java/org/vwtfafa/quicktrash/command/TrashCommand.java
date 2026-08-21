package org.vwtfafa.quicktrash.command;

import org.vwtfafa.quicktrash.QuickTrash;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class TrashCommand implements CommandExecutor {
    private final QuickTrash plugin;
    public TrashCommand(QuickTrash plugin) { this.plugin = plugin; }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { plugin.messages().send(sender, "player-only"); return true; }
        if (!player.hasPermission("quicktrash.use")) { plugin.messages().send(player, "no-permission"); return true; }
        plugin.trash().open(player);
        return true;
    }
}