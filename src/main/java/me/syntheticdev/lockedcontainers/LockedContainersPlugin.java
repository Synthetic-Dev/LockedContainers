package me.syntheticdev.lockedcontainers;

import me.syntheticdev.lockedcontainers.events.BlockListener;
import me.syntheticdev.lockedcontainers.events.CraftingListener;
import me.syntheticdev.lockedcontainers.events.PlayerInteractListener;
import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.locks.Lock;

public final class LockedContainersPlugin extends JavaPlugin {
    private static LockedContainersPlugin plugin;
    private static LockedContainersManager manager;

    @Override
    public void onEnable() {
        ConfigurationSerialization.registerClass(LockedContainer.class);

        plugin = this;
        manager = new LockedContainersManager();
        manager.load();

        this.registerEvents();
        LockedContainersRecipes.register();
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
        manager.registerEvents(new CraftingListener(), this);
    }
}
