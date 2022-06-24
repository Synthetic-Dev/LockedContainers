package me.syntheticdev.lockedcontainers;

import me.syntheticdev.lockedcontainers.events.BlockListener;
import me.syntheticdev.lockedcontainers.events.PlayerInteractListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class LockedContainersPlugin extends JavaPlugin {
    private static LockedContainersPlugin plugin;
    private static LockedContainersManager manager;

    @Override
    public void onEnable() {
        plugin = this;
        manager = new LockedContainersManager();

        this.registerEvents();
    }

    @Override
    public void onDisable() {

    }

    public static LockedContainersPlugin getPlugin() {
        return plugin;
    }

    public static LockedContainersManager getManager() {
        return manager;
    }

    private void registerEvents() {
        PluginManager manager = Bukkit.getPluginManager();

        manager.registerEvents(new PlayerInteractListener(), this);
        manager.registerEvents(new BlockListener(), this);
    }
}