package me.syntheticdev.lockedcontainers;

import org.bukkit.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Locale;

public class LockedContainersRecipes {

    private static ShapedRecipe makeContainerRecipe(Material container) {
        ItemStack result = LockedContainersPlugin.getManager().createLockedContainerItem(container);

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

        {
            ItemStack result = new ItemStack(Material.TRIPWIRE_HOOK, 1);

            NamespacedKey recipeKey = new NamespacedKey(LockedContainersPlugin.getPlugin(), "revert_key");
            ShapelessRecipe recipe = new ShapelessRecipe(recipeKey, result);
            recipe.addIngredient(Material.TRIPWIRE_HOOK);
            Bukkit.addRecipe(recipe);
        }
    }

}
