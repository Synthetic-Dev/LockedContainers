package me.syntheticdev.lockedcontainers.containers;

import me.syntheticdev.lockedcontainers.LockedContainersPlugin;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LockedContainer implements ConfigurationSerializable {
    private OfflinePlayer owner;
    private Container container;
    private UUID uuid;

    public LockedContainer(Container container, OfflinePlayer owner, UUID uuid) {
        this.container = container;
        this.owner = owner;
        this.uuid = uuid;
    }

    public OfflinePlayer getOwner() {
        return this.owner;
    }

    public UUID getUUID() {
        return this.uuid;
    }

    public Container getContainer() {
        return this.container;
    }

    public boolean isOwner(Player player) {
        return this.owner.getUniqueId().equals(player.getUniqueId());
    }

    public boolean isValidKey(PersistentDataContainer nbt) {
        NamespacedKey uuidKey = new NamespacedKey(LockedContainersPlugin.getPlugin(), "key-uuid");
        return nbt.has(uuidKey, PersistentDataType.STRING) && nbt.get(uuidKey, PersistentDataType.STRING) == this.uuid.toString();
    }

    public ItemStack createKey() {
        ItemStack key = new ItemStack(Material.TRIPWIRE_HOOK, 1);
        ItemMeta meta = key.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + this.container.getType().toString() + " Key");

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

        Chest container = (Chest)((Location)serializedMap.get("container")).getBlock().getState();
        UUID uuid = UUID.fromString((String)serializedMap.get("uuid"));

        LockedContainer deserialized = new LockedContainer(container, owner, uuid);
        return deserialized;
    }
}
