package org.vwtfafa.quicktrash;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.util.ArrayList;
import java.util.List;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.vwtfafa.quicktrash.command.QuickTrashCommand;
import org.vwtfafa.quicktrash.command.TrashCommand;
import org.vwtfafa.quicktrash.hook.QuickTrashExpansion;
import org.vwtfafa.quicktrash.listener.TrashListener;
import org.vwtfafa.quicktrash.manager.StatsManager;
import org.vwtfafa.quicktrash.manager.TrashManager;
import org.vwtfafa.quicktrash.manager.ValuableItemChecker;
import org.vwtfafa.quicktrash.util.ConfigMigrator;
import org.vwtfafa.quicktrash.util.MessageService;
import org.vwtfafa.quicktrash.util.MetricsCounters;
import org.vwtfafa.quicktrash.util.UpdateChecker;

/** Main entry point for the QuickTrash Paper plugin. */
public final class QuickTrash extends JavaPlugin {
    private TrashManager trash;
    private ValuableItemChecker valuableItems;
    private StatsManager stats;
    private MessageService messages;
    private MetricsCounters metricsCounters;
    private UpdateChecker updateChecker;

    @Override public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        if (ConfigMigrator.migrate(getConfig(), getConfig().getDefaults())) {
            saveConfig();
            getLogger().info("Migrated config.yml to version " + getConfig().getInt("config-version"));
        }
        messages = new MessageService(this);
        metricsCounters = new MetricsCounters();
        trash = new TrashManager(this);
        valuableItems = new ValuableItemChecker(trash);
        valuableItems.reloadBlocked();
        stats = new StatsManager(this);
        trash.load();
        stats.load();
        stats.startFlushTask();
        getServer().getPluginManager().registerEvents(new TrashListener(this), this);
        getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onJoin(PlayerJoinEvent event) {
                if (updateChecker != null) updateChecker.notifyPlayer(event.getPlayer());
            }
        }, this);

        List<String> aliases = new ArrayList<>(getConfig().getStringList("trash.aliases"));
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register("trash", "Opens your QuickTrash inventory.", aliases, new TrashCommand(this));
            event.registrar().register("quicktrash", "QuickTrash administration commands.", List.of(), new QuickTrashCommand(this));
        });

        if (getConfig().getBoolean("metrics.enabled", true)) {
            Metrics metrics = new Metrics(this, 33565);
            metrics.addCustomChart(new SingleLineChart("deleted_items",
                () -> (int) Math.min(stats.total(), Integer.MAX_VALUE)));
            metrics.addCustomChart(new SingleLineChart("trash_opens", metricsCounters::drainOpens));
            metrics.addCustomChart(new SingleLineChart("confirm_dialogs", metricsCounters::drainConfirms));
            metrics.addCustomChart(new SingleLineChart("active_sessions", () -> trash.activeSessions()));
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new QuickTrashExpansion(this).register();
            getLogger().info("Hooked PlaceholderAPI.");
        }

        updateChecker = new UpdateChecker(this);
        updateChecker.checkAsync();
        getLogger().info("QuickTrash enabled.");
    }

    @Override public void onDisable() {
        if (trash != null) trash.shutdown();
        if (stats != null) stats.shutdown();
    }

    public TrashManager trash() { return trash; }
    public ValuableItemChecker valuableItems() { return valuableItems; }
    public StatsManager stats() { return stats; }
    public MessageService messages() { return messages; }
    public MetricsCounters metricsCounters() { return metricsCounters; }
}
