package me.syntheticdev.lockedcontainers;

import org.bukkit.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Locale;

public class LockedContainersRecipes {

    private static ShapedRecipe makeContainerRecipe(Material container) {
        ItemStack result = new ItemStack(container, 1);
        ItemMeta meta = result.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + Utils.toDisplayCase(container.toString()) + " Key");

        PersistentDataContainer nbt = meta.getPersistentDataContainer();
        NamespacedKey namespacedKey = new NamespacedKey(LockedContainersPlugin.getPlugin(), "is-locked-container");
        nbt.set(namespacedKey, PersistentDataType.BYTE, (byte)1);

        result.setItemMeta(meta);

        NamespacedKey recipeKey = new NamespacedKey(LockedContainersPlugin.getPlugin(), "locked_" + container.toString().toLowerCase());
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, result);
        recipe.shape(
                "WDW",
                "WCW",
                "WWW"
        );
        recipe.setIngredient('D', Material.DIAMOND);
        recipe.setIngredient('W', new RecipeChoice.MaterialChoice(Tag.PLANKS));
        recipe.setIngredient('C', new RecipeChoice.MaterialChoice(container));

        return recipe;
    }

    public static void register() {
        Bukkit.addRecipe(makeContainerRecipe(Material.CHEST));
        Bukkit.addRecipe(makeContainerRecipe(Material.BARREL));
    }

}
