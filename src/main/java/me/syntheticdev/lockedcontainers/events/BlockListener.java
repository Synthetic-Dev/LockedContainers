package me.syntheticdev.lockedcontainers.events;

import me.syntheticdev.lockedcontainers.Utils;
import me.syntheticdev.lockedcontainers.containers.LockedContainer;
import me.syntheticdev.lockedcontainers.LockedContainersManager;
import me.syntheticdev.lockedcontainers.LockedContainersPlugin;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;

public class BlockListener implements Listener {
    private LockedContainersManager manager;
    public BlockListener() {
        this.manager = LockedContainersPlugin.getManager();
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (manager.isKey(item)) {
            event.setCancelled(true);
        }
        if (manager.isLockedContainerItem(item)) {
            event.setCancelled(manager.handleContainerPlace(event));
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!manager.isLockedContainer(block)) return;

        LockedContainer lockedContainer = manager.getLockedContainer((Container)block);

        try {
            manager.destroyLockedContainer(lockedContainer);
            event.setCancelled(false);
        } catch (IOException err) {
            err.printStackTrace();
            event.setCancelled(true);
        }
    }
}
