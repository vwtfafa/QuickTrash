package org.vwtfafa.quicktrash.command;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.vwtfafa.quicktrash.QuickTrash;

public final class TrashCommand implements BasicCommand {
    private final QuickTrash plugin;
    public TrashCommand(QuickTrash plugin) { this.plugin = plugin; }

    @Override public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        if (!(sender instanceof Player player)) { plugin.messages().send(sender, "player-only"); return; }
        if (!player.hasPermission("quicktrash.use")) { plugin.messages().send(player, "no-permission"); return; }
        plugin.trash().open(player);
    }

    @Override public String permission() { return "quicktrash.use"; }
}
