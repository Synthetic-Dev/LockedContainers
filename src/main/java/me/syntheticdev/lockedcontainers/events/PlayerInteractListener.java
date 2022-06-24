package me.syntheticdev.lockedcontainers.events;

import me.syntheticdev.lockedcontainers.LockedContainersPlugin;
import me.syntheticdev.lockedcontainers.LockedContainersManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerInteractListener implements Listener {
    private LockedContainersManager manager;
    public PlayerInteractListener() {
        this.manager = LockedContainersPlugin.getManager();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!(event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_BLOCK)) return;

        Block block = event.getClickedBlock();
        if (!manager.isLockedContainer(block)) return;

        Player player = event.getPlayer();
        if (player.isSneaking()) {
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item.getType() == Material.TRIPWIRE_HOOK && !item.hasItemMeta()) {
                manager.handleCreateKey(event, item);
                event.setCancelled(true);
                return;
            }
        }

        manager.handleContainerOpen(event);
        event.setCancelled(true);
    }
}
