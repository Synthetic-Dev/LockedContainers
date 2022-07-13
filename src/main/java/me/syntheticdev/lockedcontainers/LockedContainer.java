package me.syntheticdev.lockedcontainers;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class LockedContainer implements ConfigurationSerializable {
    private OfflinePlayer owner;
    private @Nullable Container container;
    private UUID uuid;

    public LockedContainer(Container container, OfflinePlayer owner, UUID uuid) {
        this.container = container;
        this.owner = owner;
        this.uuid = uuid;
    }

    public LockedContainer(Location location, OfflinePlayer owner, UUID uuid) {
        Block block = location.getBlock();
        if (LockedContainersPlugin.getManager().isLockableContainer(block)) {
            this.container = (Container)block.getState();
        }
        this.owner = owner;
        this.uuid = uuid;
    }

//    public OfflinePlayer getOwner() {
//        return this.owner;
//    }
//    public UUID getUUID() {
//        return this.uuid;
//    }

    @Nullable
    public Container getContainer() {
        return (Container)this.container.getLocation().getBlock().getState();
    }

    public void setContainer(Container container) {
        this.container = container;
    }

    public boolean isOwner(Player player) {
        return this.owner.getUniqueId().equals(player.getUniqueId());
    }

    public boolean isValidKey(ItemStack item) {
        if (!item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer nbt = meta.getPersistentDataContainer();

        NamespacedKey uuidKey = new NamespacedKey(LockedContainersPlugin.getPlugin(), "key-uuid");
        if (!nbt.has(uuidKey, PersistentDataType.STRING)) return false;

        Logger logger = LockedContainersPlugin.getPlugin().getLogger();

        String keyUUID = nbt.get(uuidKey, PersistentDataType.STRING);
        String uuid = this.uuid.toString();
        boolean isValid = keyUUID.equals(uuid);
        //logger.info("Key UUID: " + keyUUID + ", Container UUID: " + uuid + ", " + isValid);
        return isValid;
    }

    public ItemStack createKey() {
        ItemStack key = new ItemStack(Material.TRIPWIRE_HOOK, 1);
        ItemMeta meta = key.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + Utils.toDisplayCase(this.container.getType().toString()) + " Key");
        meta.addEnchant(Enchantment.ARROW_INFINITE, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        PersistentDataContainer nbt = meta.getPersistentDataContainer();
        NamespacedKey uuidKey = new NamespacedKey(LockedContainersPlugin.getPlugin(), "key-uuid");
        nbt.set(uuidKey, PersistentDataType.STRING, this.uuid.toString());

        key.setItemMeta(meta);
        return key;
    }

    @Override
    public Map<String, Object> serialize() {
        HashMap<String, Object> serializedMap = new HashMap();
        serializedMap.put("owner", this.owner.getUniqueId().toString());
        serializedMap.put("uuid", this.uuid.toString());
        serializedMap.put("container", this.container.getLocation());
        return serializedMap;
    }

    public static LockedContainer deserialize(Map<String, Object> serializedMap) {
        String ownerUUID = (String)serializedMap.get("owner");
        OfflinePlayer owner = Bukkit.getOfflinePlayer(UUID.fromString(ownerUUID));

        Location location = (Location)serializedMap.get("container");
        UUID uuid = UUID.fromString((String)serializedMap.get("uuid"));

        LockedContainer deserialized = new LockedContainer(location, owner, uuid);
        return deserialized;
    }
}
