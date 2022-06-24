package me.syntheticdev.lockedcontainers.events;

import me.syntheticdev.lockedcontainers.LockedContainer;
import me.syntheticdev.lockedcontainers.LockedContainersManager;
import me.syntheticdev.lockedcontainers.LockedContainersPlugin;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.Iterator;

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
            manager.destroyLockedContainer(event, lockedContainer);
            event.setCancelled(false);
        } catch (IOException err) {
            err.printStackTrace();
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        Iterator<Block> blocks = event.blockList().iterator();

        while (blocks.hasNext()) {
            Block block = blocks.next();

            if (manager.isLockedContainer(block)) {
                blocks.remove();
            }
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        Iterator<Block> blocks = event.blockList().iterator();

        while (blocks.hasNext()) {
            Block block = blocks.next();

            if (manager.isLockedContainer(block)) {
                blocks.remove();
            }
        }
    }
}
