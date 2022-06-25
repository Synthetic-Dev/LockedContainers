package me.syntheticdev.lockedcontainers.events;

import me.syntheticdev.lockedcontainers.LockedContainersManager;
import me.syntheticdev.lockedcontainers.LockedContainersPlugin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import javax.annotation.Nullable;

public class CraftingListener implements Listener {
    private LockedContainersManager manager;
    public CraftingListener() {
        this.manager = LockedContainersPlugin.getManager();
    }

    @EventHandler
    public void onRevertKey(PrepareItemCraftEvent event) {
        Recipe recipe = event.getRecipe();
        if (recipe == null || !recipe.getResult().getType().equals(Material.TRIPWIRE_HOOK)) return;

        CraftingInventory inventory = event.getInventory();

        int count = 0;
        ItemStack key = null;
        for (ItemStack item : inventory.getMatrix()) {
            if (item == null || item.getType().equals(Material.AIR)) continue;

            count++;
            if (count > 1 || !item.getType().equals(Material.TRIPWIRE_HOOK)) return;
            key = item;
        }
        if (key == null) return;

        if (key.hasItemMeta()) {
            ItemMeta meta = key.getItemMeta();
            PersistentDataContainer nbt = meta.getPersistentDataContainer();
            NamespacedKey uuidKey = new NamespacedKey(LockedContainersPlugin.getPlugin(), "key-uuid");
            if (nbt.has(uuidKey, PersistentDataType.STRING)) {
                return;
            }
        }

        inventory.setResult(null);
    }
}
