package org.vwtfafa.quicktrash;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.util.List;
import org.vwtfafa.quicktrash.command.QuickTrashCommand;
import org.vwtfafa.quicktrash.command.TrashCommand;
import org.vwtfafa.quicktrash.listener.TrashListener;
import org.vwtfafa.quicktrash.manager.TrashManager;
import org.vwtfafa.quicktrash.manager.ValuableItemChecker;
import org.vwtfafa.quicktrash.util.MessageService;
import org.bukkit.plugin.java.JavaPlugin;
import org.bstats.bukkit.Metrics;

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
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register("trash", "Opens your QuickTrash inventory.", List.of(), new TrashCommand(this));
            event.registrar().register("quicktrash", "QuickTrash administration commands.", List.of(), new QuickTrashCommand(this));
        });
        // bStats
        int pluginId = 33565;
        new Metrics(this, pluginId);
        getLogger().info("QuickTrash enabled.");
    }

    @Override public void onDisable() { if (trash != null) trash.shutdown(); }
    public TrashManager trash() { return trash; }
    public ValuableItemChecker valuableItems() { return valuableItems; }
    public MessageService messages() { return messages; }
}