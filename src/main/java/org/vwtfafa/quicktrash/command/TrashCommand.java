package org.vwtfafa.quicktrash.command;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.vwtfafa.quicktrash.QuickTrash;

public final class TrashCommand implements BasicCommand {
    private final QuickTrash plugin;
    public TrashCommand(QuickTrash plugin) { this.plugin = plugin; }

    @Override public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "player-only");
            return;
        }
        if (!player.hasPermission("quicktrash.use")) {
            plugin.messages().send(player, "no-permission");
            return;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("confirm")) {
            handleConfirm(player, args);
            return;
        }
        plugin.trash().open(player);
    }

    private void handleConfirm(Player player, String[] args) {
        if (!player.hasPermission("quicktrash.confirm.toggle")) {
            plugin.messages().send(player, "no-permission");
            return;
        }
        if (args.length == 1) {
            boolean enabled = plugin.preferences().isConfirmEnabled(player.getUniqueId());
            plugin.messages().send(player, enabled ? "confirm-status-on" : "confirm-status-off");
            return;
        }
        String mode = args[1].toLowerCase(Locale.ROOT);
        if (mode.equals("on") || mode.equals("true") || mode.equals("enable")) {
            plugin.preferences().setConfirmEnabled(player.getUniqueId(), true);
            plugin.messages().send(player, "confirm-enabled");
            return;
        }
        if (mode.equals("off") || mode.equals("false") || mode.equals("disable")) {
            plugin.preferences().setConfirmEnabled(player.getUniqueId(), false);
            plugin.messages().send(player, "confirm-disabled");
            return;
        }
        plugin.messages().send(player, "confirm-usage");
    }

    @Override public java.util.Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return List.of("confirm").stream().filter(option -> option.startsWith(prefix)).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("confirm")) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return List.of("on", "off").stream().filter(option -> option.startsWith(prefix)).toList();
        }
        return List.of();
    }

    @Override public String permission() { return "quicktrash.use"; }
}
