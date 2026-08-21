package org.example.quicktrash;

import org.example.quicktrash.command.QuickTrashCommand;
import org.example.quicktrash.command.TrashCommand;
import org.example.quicktrash.listener.TrashListener;
import org.example.quicktrash.manager.TrashManager;
import org.example.quicktrash.manager.ValuableItemChecker;
import org.example.quicktrash.util.MessageService;
import org.bukkit.plugin.java.JavaPlugin;

/** Main entry point for the QuickTrash Paper plugin. */
public final class QuickTrash extends JavaPlugin {
    private TrashManager trash;
    private ValuableItemChecker valuableItems;
    private MessageService messages;

    @Override public void onEnable() {
        saveDefaultConfig();
        messages = new MessageService(this);
        trash = new TrashManager(this);
        valuableItems = new ValuableItemChecker(trash);
        trash.load();
        getServer().getPluginManager().registerEvents(new TrashListener(this), this);
        getCommand("trash").setExecutor(new TrashCommand(this));
        QuickTrashCommand admin = new QuickTrashCommand(this);
        getCommand("quicktrash").setExecutor(admin);
        getCommand("quicktrash").setTabCompleter(admin);
        getLogger().info("QuickTrash enabled.");
    }

    @Override public void onDisable() { if (trash != null) trash.shutdown(); }
    public TrashManager trash() { return trash; }
    public ValuableItemChecker valuableItems() { return valuableItems; }
    public MessageService messages() { return messages; }
}
