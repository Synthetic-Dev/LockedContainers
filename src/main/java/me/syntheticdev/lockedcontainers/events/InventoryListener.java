package me.syntheticdev.lockedcontainers.events;

import me.syntheticdev.lockedcontainers.LockedContainer;
import me.syntheticdev.lockedcontainers.LockedContainersManager;
import me.syntheticdev.lockedcontainers.LockedContainersPlugin;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class InventoryListener implements Listener {
    private LockedContainersManager manager;
    public InventoryListener() {
        this.manager = LockedContainersPlugin.getManager();
    }

    @EventHandler
    public void onHopperTake(InventoryMoveItemEvent event) {
        Block source = event.getSource().getLocation().getBlock();
        if (!manager.isLockedContainer(source)) return;

        InventoryHolder destination = event.getDestination().getHolder();
        if (destination instanceof Hopper
            || destination instanceof HopperMinecart) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onOpen(InventoryOpenEvent event) {
        Inventory inventory = event.getInventory();
        if (inventory == null || inventory.getLocation() == null) return;
        Block block = inventory.getLocation().getBlock();

        if (!manager.isLockedContainer(block)) return;

        Container container = (Container)block.getState();
        LockedContainer lockedContainer = manager.getLockedContainer(container);
        if (lockedContainer == null) return;

        Player player = (Player)event.getPlayer();
        boolean shouldOpen = manager.tryOpenContainer(lockedContainer, player);
        if (!shouldOpen) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDisenchant(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        if (event.getRawSlot() == 2 && inventory.getType() == InventoryType.GRINDSTONE) {
            ItemStack item1 = inventory.getItem(0);
            ItemStack item2 = inventory.getItem(1);

            if ((item1 != null && manager.isKey(item1)) || (item2 != null && manager.isKey(item2))) {
                event.setResult(Event.Result.DENY);
                event.setCancelled(true);
                inventory.setItem(2, null);
            }
        }
    }
}
