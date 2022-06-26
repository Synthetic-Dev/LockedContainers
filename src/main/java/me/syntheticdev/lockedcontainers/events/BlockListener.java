package me.syntheticdev.lockedcontainers.events;

import me.syntheticdev.lockedcontainers.LockedContainer;
import me.syntheticdev.lockedcontainers.LockedContainersManager;
import me.syntheticdev.lockedcontainers.LockedContainersPlugin;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Logger;

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
        if (manager.isLockableContainer(item)) {
            event.setCancelled(manager.handleContainerPlace(event));
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!manager.isLockedContainer(block)) return;
        event.setCancelled(true);
        Logger logger = LockedContainersPlugin.getPlugin().getLogger();
        logger.info("is event cancelled: " + event.isCancelled());

        LockedContainer lockedContainer = manager.getLockedContainer((Container)block.getState());
        logger.info("Broke: " + lockedContainer);
        logger.info("Is null: " + (lockedContainer == null));

        try {
            boolean shouldBreak = manager.destroyLockedContainer(event, lockedContainer);
            logger.info("Should break: " + shouldBreak);
            if (lockedContainer == null || shouldBreak) {
                logger.info("Breaking block");
                Container container = (Container)block.getState();
                ItemStack[] contents;
                if (block.getType().equals(Material.CHEST)) {
                    contents = ((Chest)container).getBlockInventory().getContents();
                } else {
                    contents = container.getInventory().getContents();
                }

                Material type = block.getType();
                block.setType(Material.AIR);

                World world = block.getWorld();
                Location location = block.getLocation();

                Player player = event.getPlayer();
                if (!player.getGameMode().equals(GameMode.CREATIVE)) {
                    world.dropItemNaturally(location, manager.createLockedContainerItem(type));
                }

                for (ItemStack item : contents) {
                    if (item == null) continue;
                    world.dropItemNaturally(location, item);
                }
            }
        } catch (IOException err) {
            err.printStackTrace();
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
