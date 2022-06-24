package me.syntheticdev.lockedcontainers;

import com.sun.tools.javac.util.StringUtils;
import me.syntheticdev.lockedcontainers.containers.LockedContainer;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.block.DoubleChest;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.permissions.Permission;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class LockedContainersManager {
    private ArrayList<LockedContainer> containers;
    private ArrayList<Material> containerTypes;

    public LockedContainersManager() {
        this.containerTypes = new ArrayList<Material>();
        this.containerTypes.add(Material.CHEST);
        this.containerTypes.add(Material.BARREL);

        this.containers = this.load();
    }

    private boolean isLockableContainer(Block block) {
        return containerTypes.stream().anyMatch((material) -> material.equals(block.getType()));
    }

    private boolean isLockableContainer(ItemStack item) {
        return containerTypes.stream().anyMatch((material) -> material.equals(item.getType()));
    }

    private boolean isLockedContainerRaw(Block block) {
        return this.containers.stream().anyMatch((lockedContainer) -> lockedContainer.getContainer().getLocation().equals(block.getLocation()));
    }

    public boolean isLockedContainer(Block block) {
        if (!this.isLockableContainer(block)) return false;

        boolean is = this.isLockedContainerRaw(block);
        // Handle double chests
        if (!is && block.getType() == Material.CHEST && ((Chest)block).getInventory() instanceof DoubleChestInventory) {
            DoubleChest doubleChest = (DoubleChest)((Chest)block).getInventory().getHolder();
            is = this.isLockedContainerRaw((Block) doubleChest.getLeftSide());
            if (!is) this.isLockedContainerRaw((Block) doubleChest.getRightSide());
        }
        return is;
    }

    public boolean isLockedContainerItem(ItemStack item) {
        if (!this.isLockableContainer(item) || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer nbt = meta.getPersistentDataContainer();
        NamespacedKey namespacedKey = new NamespacedKey(LockedContainersPlugin.getPlugin(), "is-locked-container");

        return nbt.has(namespacedKey, PersistentDataType.BYTE);
    }

    public boolean isKey(ItemStack item) {
        if (item.getType() != Material.TRIPWIRE_HOOK || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer nbt = meta.getPersistentDataContainer();
        NamespacedKey uuidKey = new NamespacedKey(LockedContainersPlugin.getPlugin(), "key-uuid");
        return nbt.has(uuidKey, PersistentDataType.STRING);
    }

    public ArrayList<LockedContainer> getLockedContainers() {
        return this.containers;
    }

    @Nullable
    private LockedContainer getLockedContainerRaw(Container container) {
        return this.containers.stream().filter((lockedContainer) -> lockedContainer.getContainer().getLocation().equals(container.getLocation())).findFirst().orElse(null);
    }

    @Nullable
    public LockedContainer getLockedContainer(Container container) {
        LockedContainer lockedContainer = this.getLockedContainerRaw(container);
        // Handle double chests
        if (lockedContainer == null && container.getType() == Material.CHEST && container.getInventory() instanceof DoubleChestInventory) {
            DoubleChest doubleChest = (DoubleChest)container.getInventory().getHolder();
            lockedContainer = this.getLockedContainerRaw((Chest)doubleChest.getLeftSide());
            if (lockedContainer == null) this.getLockedContainerRaw((Chest)doubleChest.getRightSide());
        }
        return lockedContainer;
    }

    public ArrayList<LockedContainer> load() {
        File file = new File(LockedContainersPlugin.getPlugin().getDataFolder().getAbsolutePath(), "containers.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (file.length() != 0L) {
            List<?> containers = config.getList("containers");
            if (containers instanceof ArrayList) {
                return (ArrayList<LockedContainer>)containers;
            }
        }
        return new ArrayList();
    }

    private LockedContainer createLockedContainer(Container container) {
        //create locked container
        return (LockedContainer)null;
    }

    public void destroyLockedContainer(LockedContainer lockedContainer) throws IOException {
        File file = new File(LockedContainersPlugin.getPlugin().getDataFolder().getAbsolutePath(), "containers.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        this.containers.remove(lockedContainer);
        config.set("containers", this.containers);
        config.save(file);
    }

    public boolean handleContainerPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Container container = (Container)event.getBlock();
        Location location = event.getBlock().getLocation();
        int lockedChestCount = 0;

        if (container instanceof Chest) {
            for (double x = -1; x <= 1; x = x + 2) {
                for (double z = -1; z <= 1; z = z + 2) {
                    Location neighbour = new Location(location.getWorld(), location.getX() + x, location.getY(), location.getZ() + z);
                    Block block = neighbour.getBlock();

                    if (block.getType() != Material.CHEST) continue;
                    if (!(((Chest)block).getInventory() instanceof DoubleChestInventory)) {
                        if (this.isLockedContainer(block)) {
                            lockedChestCount++;
                            if (lockedChestCount > 1) {
                                player.sendMessage(ChatColor.RED + "Locked Double Chest cannot be determined!");
                                return true;
                            }
                        } else {
                            player.sendMessage(ChatColor.RED + "Cannot attach Locked Chest to Chest!");
                            return true;
                        }
                    }
                }
            }
        }

        if (lockedChestCount > 0) return true;

        this.createLockedContainer(container);
        return false;
    }

    public void handleContainerOpen(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Container container = (Container)event.getClickedBlock();
        LockedContainer lockedContainer = this.getLockedContainer(container);

        if (lockedContainer == null) return;
        if (!lockedContainer.isOwner(player) && !player.hasPermission("lockedcontainers.admin")) {
            player.sendMessage(ChatColor.RED + "This " + Utils.toDisplayCase(container.getType().toString()) + " is locked.");
        } else {
            player.openInventory(container.getInventory());
        }
    }
}
