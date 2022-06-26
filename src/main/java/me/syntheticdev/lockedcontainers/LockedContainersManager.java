package me.syntheticdev.lockedcontainers;

import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public class LockedContainersManager {
    private ArrayList<LockedContainer> containers;
    private Material[] containerTypes = new Material[]{Material.CHEST, Material.BARREL};

    public LockedContainersManager() {
        this.containers = new ArrayList<>();
    }

    public boolean isLockableContainer(Block block) {
        return Arrays.stream(containerTypes).anyMatch((material) -> material.equals(block.getType()));
    }

    public boolean isLockableContainer(ItemStack item) {
        return Arrays.stream(containerTypes).anyMatch((material) -> material.equals(item.getType()));
    }

    public boolean isLockedContainerItem(ItemStack item) {
        if (!this.isLockableContainer(item) || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer nbt = meta.getPersistentDataContainer();
        NamespacedKey namespacedKey = new NamespacedKey(LockedContainersPlugin.getPlugin(), "is-locked-container");

        return nbt.has(namespacedKey, PersistentDataType.BYTE);
    }

    private boolean isLockedContainerRaw(Block block) {
        return this.containers.stream().anyMatch((lockedContainer) -> lockedContainer.getContainer().getLocation().equals(block.getLocation()));
    }

    public boolean isLockedContainer(Block block) {
        if (!this.isLockableContainer(block)) return false;

        boolean isLocked = this.isLockedContainerRaw(block);
        // Handle double chests
        if (!isLocked && block.getType().equals(Material.CHEST) && ((Chest)block.getState()).getInventory() instanceof DoubleChestInventory) {
            DoubleChest doubleChest = (DoubleChest)((Chest)block.getState()).getInventory().getHolder();
            isLocked = this.isLockedContainerRaw(((Chest)doubleChest.getLeftSide()).getBlock());
            if (!isLocked) isLocked = this.isLockedContainerRaw(((Chest)doubleChest.getRightSide()).getBlock());
        }
        return isLocked;
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
        if (lockedContainer == null && container.getType().equals(Material.CHEST) && container.getInventory() instanceof DoubleChestInventory) {
            DoubleChest doubleChest = (DoubleChest)container.getInventory().getHolder();
            lockedContainer = this.getLockedContainerRaw((Chest)doubleChest.getLeftSide());
            if (lockedContainer == null) lockedContainer = this.getLockedContainerRaw((Chest)doubleChest.getRightSide());
        }
        return lockedContainer;
    }

    public void load() {
        File file = new File(LockedContainersPlugin.getPlugin().getDataFolder().getAbsolutePath(), "containers.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (file.length() != 0L) {
            List<?> list = config.getList("containers");
            if (list instanceof ArrayList) {
                ArrayList<LockedContainer> containers = (ArrayList<LockedContainer>)list;
                boolean removed = containers.removeIf(lockedContainer -> lockedContainer == null || lockedContainer.getContainer() == null);
                this.containers = containers;

                if (!removed) return;
                try {
                    config.set("containers", this.containers);
                    config.save(file);
                } catch (IOException err) {
                    err.printStackTrace();
                }
            }
        }
    }

    public ItemStack createLockedContainerItem(Material container) {
        ItemStack item = new ItemStack(container, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Locked " + Utils.toDisplayCase(container.toString()));

        PersistentDataContainer nbt = meta.getPersistentDataContainer();
        NamespacedKey namespacedKey = new NamespacedKey(LockedContainersPlugin.getPlugin(), "is-locked-container");
        nbt.set(namespacedKey, PersistentDataType.BYTE, (byte)1);

        item.setItemMeta(meta);
        return item;
    }

    private LockedContainer createLockedContainer(BlockPlaceEvent event, Container container) {
        Player player = event.getPlayer();
        LockedContainer lockedContainer = new LockedContainer(container, player, UUID.randomUUID());

        try {
            File file = new File(LockedContainersPlugin.getPlugin().getDataFolder().getAbsolutePath(), "containers.yml");
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);

            this.containers.add(lockedContainer);
            config.set("containers", this.containers);
            config.save(file);
        } catch (IOException err) {
            err.printStackTrace();
        }

        return lockedContainer;
    }

    public boolean destroyLockedContainer(BlockBreakEvent event, LockedContainer lockedContainer) throws IOException {
        Player player = event.getPlayer();
        if (!lockedContainer.isOwner(player) && !player.hasPermission("lockedcontainers.admin")) {
            player.sendMessage(ChatColor.RED + "This is not your Locked " + Utils.toDisplayCase(event.getBlock().getType().toString()) + ".");
            return false;
        }

        File file = new File(LockedContainersPlugin.getPlugin().getDataFolder().getAbsolutePath(), "containers.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        Logger logger = LockedContainersPlugin.getPlugin().getLogger();
        Container container = lockedContainer.getContainer();

        if (!container.getLocation().equals(event.getBlock().getLocation())) {
            //logger.info("Broke non-primary container");
            return true;
        }

        //logger.info("Broke container: " + container.getType());
        if (container.getType().equals(Material.CHEST) && ((Chest)container.getBlock().getState()).getInventory() instanceof DoubleChestInventory) {
            //logger.info("Is double chest");
            DoubleChest doubleChest = (DoubleChest)((Chest)container.getBlock().getState()).getInventory().getHolder();

            Chest left = (Chest)doubleChest.getLeftSide();
            Chest right = (Chest)doubleChest.getRightSide();

            Chest otherChest = left.getLocation().equals(container.getLocation()) ? right : left;
            lockedContainer.setContainer(otherChest);
        } else {
            this.containers.remove(lockedContainer);
        }

        config.set("containers", this.containers);
        config.save(file);
        return true;
    }

    public boolean handleContainerPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Container container = (Container)event.getBlock().getState();

        int lockedChestCount = 0;
        boolean isLockedContainer = this.isLockedContainerItem(event.getItemInHand());

        if (!player.isSneaking() && container.getType().equals(Material.CHEST)) {
            BlockFace containerFacing = ((org.bukkit.block.data.type.Chest)container.getBlockData()).getFacing();
            Logger logger = LockedContainersPlugin.getPlugin().getLogger();

            BlockFace[] sides = (containerFacing == BlockFace.NORTH || containerFacing == BlockFace.SOUTH)
                    ? new BlockFace[]{BlockFace.EAST, BlockFace.WEST}
                    : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH};

            for (BlockFace face : sides) {
                Block block = container.getBlock().getRelative(face);
                //logger.info("Checking pos: " + block.getLocation().toVector() + "; Type: " + block.getType());

                if (block.getType() != Material.CHEST) continue;
                if (((org.bukkit.block.data.type.Chest)block.getBlockData()).getFacing() != containerFacing) continue;

                Chest chest = (Chest)block.getState();
                boolean isDouble = chest.getInventory() instanceof DoubleChestInventory;
                //logger.info("Is double chest: " + isDouble);
                if (isDouble) continue;

                boolean isLockedChest = this.isLockedContainer(block);

                if (isLockedContainer && isLockedChest) {
                    LockedContainer lockedChest = this.getLockedContainer(chest);
                    if (!lockedChest.isOwner(player)) {
                        player.sendMessage(ChatColor.RED + "Cannot attach to another player's Locked Chest!");
                        return true;
                    }

                    lockedChestCount++;
                    if (lockedChestCount > 1) {
                        player.sendMessage(ChatColor.RED + "Locked Double Chest cannot be determined!");
                        return true;
                    }
                } else if (isLockedContainer != isLockedChest) {
                    player.sendMessage(ChatColor.RED + "Cannot attach " + (isLockedContainer ? "Locked " : "") + "Chest to "  + (isLockedChest ? "Locked " : "") +  "Chest!");
                    return true;
                }
            }
            //logger.info("Placing chest, locked chests: " + lockedChestCount);
        }

        if (isLockedContainer && lockedChestCount == 0) {
            this.createLockedContainer(event, container);
        }
        return false;
    }

    public void handleContainerOpen(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Container container = (Container)event.getClickedBlock().getState();
        LockedContainer lockedContainer = this.getLockedContainer(container);

        if (lockedContainer == null) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        boolean hasKey = item.getType() == Material.TRIPWIRE_HOOK && this.isKey(item);

        if (hasKey && !lockedContainer.isValidKey(item)) {
            player.sendMessage(ChatColor.RED + "That key is not for this " + Utils.toDisplayCase(container.getType().toString()) + ".");
            player.playSound(container.getLocation(), Sound.BLOCK_ANVIL_LAND, 1f, 1f);
            return;
        }

        if (!hasKey && !lockedContainer.isOwner(player) && !player.hasPermission("lockedcontainers.admin")) {
            player.sendMessage(ChatColor.RED + "This " + Utils.toDisplayCase(container.getType().toString()) + " is locked.");
            player.playSound(container.getLocation(), Sound.BLOCK_IRON_TRAPDOOR_CLOSE, 1f, 1f);
            return;
        }

        player.openInventory(container.getInventory());
    }

    public void handleCreateKey(PlayerInteractEvent event, ItemStack item) {
        if (item.getAmount() == 0) return;

        Player player = event.getPlayer();
        Container container = (Container)event.getClickedBlock().getState();
        LockedContainer lockedContainer = this.getLockedContainer(container);

        if (lockedContainer == null) return;
        if (!lockedContainer.isOwner(player) && !player.hasPermission("lockedcontainers.admin")) {
            player.sendMessage(ChatColor.RED + "This is not your Locked " + Utils.toDisplayCase(container.getType().toString()) + ".");
            return;
        }

        ItemStack key = lockedContainer.createKey();
        int amount = item.getAmount() - 1;
        item.setAmount(amount);

        PlayerInventory inventory = player.getInventory();
        if (amount == 0) {
            inventory.setItemInMainHand(key);
        } else {
            inventory.setItemInMainHand(item);
            inventory.addItem(key);
        }
        player.sendMessage(ChatColor.GREEN + "Key created for Locked " + Utils.toDisplayCase(container.getType().toString()) + " at " + lockedContainer.getContainer().getLocation().toVector());
    }
}
