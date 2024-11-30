package codechicken.nei.api;

import static codechicken.lib.gui.GuiDraw.getMousePosition;

import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.List;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import org.lwjgl.input.Mouse;

import codechicken.nei.ItemPanel.ItemPanelSlot;
import codechicken.nei.ItemPanels;
import codechicken.nei.LayoutManager;
import codechicken.nei.NEIClientConfig;
import codechicken.nei.NEIClientUtils;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.BookmarkRecipeId;
import codechicken.nei.recipe.GuiCraftingRecipe;
import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.GuiUsageRecipe;

public abstract class ShortcutInputHandler {

    public static boolean handleKeyEvent(ItemStack stackover) {

        if (!NEIClientConfig.isLoaded()) {
            return false;
        }

        if (NEIClientConfig.isKeyHashDown("gui.overlay_hide")) {
            return hideOverlayRecipe();
        }

        if (NEIClientConfig.isKeyHashDown("gui.bookmark_pull_items")) {
            return ItemPanels.bookmarkPanel.pullBookmarkItems(ItemPanels.bookmarkPanel.getHoveredGroupId(true), false);
        }

        if (NEIClientConfig.isKeyHashDown("gui.bookmark_pull_items_ingredients")) {
            return ItemPanels.bookmarkPanel.pullBookmarkItems(ItemPanels.bookmarkPanel.getHoveredGroupId(true), true);
        }

        if (stackover == null && NEIClientConfig.isKeyHashDown("gui.remove_recipe")) {
            final int groupId = ItemPanels.bookmarkPanel.getHoveredGroupId(true);

            if (groupId != -1) {
                ItemPanels.bookmarkPanel.removeGroup(groupId);
                return true;
            }
        }

        if (stackover == null) {
            return false;
        }

        stackover = stackover.copy();

        if (NEIClientConfig.isKeyHashDown("gui.overlay")) {
            return openOverlayRecipe(stackover, false);
        }

        if (NEIClientConfig.isKeyHashDown("gui.copy_name")) {
            return copyItemStackName(stackover);
        }

        if (NEIClientConfig.isKeyHashDown("gui.copy_oredict")) {
            return copyItemStackOreDictionary(stackover);
        }

        if (NEIClientConfig.isKeyHashDown("gui.overlay_use")) {
            return openOverlayRecipe(stackover, true);
        }

        if (NEIClientConfig.isKeyHashDown("gui.recipe")) {
            return GuiCraftingRecipe.openRecipeGui("item", stackover);
        }

        if (NEIClientConfig.isKeyHashDown("gui.usage")) {
            return GuiUsageRecipe.openRecipeGui("item", stackover);
        }

        if (NEIClientConfig.isKeyHashDown("gui.bookmark")) {
            return saveRecipeInBookmark(stackover, false, false);
        }

        if (NEIClientConfig.isKeyHashDown("gui.bookmark_recipe")) {
            return saveRecipeInBookmark(stackover, true, false);
        }

        if (NEIClientConfig.isKeyHashDown("gui.bookmark_count")) {
            return saveRecipeInBookmark(stackover, false, true);
        }

        if (NEIClientConfig.isKeyHashDown("gui.bookmark_recipe_count")) {
            return saveRecipeInBookmark(stackover, true, true);
        }

        return false;
    }

    public static boolean handleMouseClick(ItemStack stackover) {

        if (!NEIClientConfig.isLoaded()) {
            return false;
        }

        if (stackover != null) {
            final int button = Mouse.getEventButton();

            if (button == 0) {
                return GuiCraftingRecipe.openRecipeGui("item", stackover);
            } else if (button == 1) {
                return GuiUsageRecipe.openRecipeGui("item", stackover);
            }
        }

        return false;
    }

    private static boolean hideOverlayRecipe() {

        if (LayoutManager.overlayRenderer != null) {
            LayoutManager.overlayRenderer = null;
            return true;
        }

        return false;
    }

    private static boolean copyItemStackName(ItemStack stackover) {
        Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(stackover.getDisplayName()), null);
        return true;
    }

    private static boolean copyItemStackOreDictionary(ItemStack stackover) {
        StringBuilder builder = new StringBuilder();

        for (int id : OreDictionary.getOreIDs(stackover)) {
            String oreDictionaryName = OreDictionary.getOreName(id);
            if (!"Unknown".equals(oreDictionaryName)) {
                builder.append(oreDictionaryName).append(",");
            }
        }

        if (builder.length() > 0) {
            builder.deleteCharAt(builder.length() - 1);
        }

        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(builder.toString()), null);
        return true;
    }

    private static boolean openOverlayRecipe(ItemStack stackover, boolean shift) {
        final GuiContainer gui = NEIClientUtils.getGuiContainer();

        if (gui == null || gui instanceof GuiRecipe) {
            return false;
        }

        final Point mouseover = getMousePosition();
        ItemPanelSlot panelSlot = ItemPanels.bookmarkPanel.getSlotMouseOver(mouseover.x, mouseover.y);
        BookmarkRecipeId recipeId = null;

        if (panelSlot != null) {
            recipeId = ItemPanels.bookmarkPanel.getBookmarkRecipeId(panelSlot.slotIndex);
        } else {
            recipeId = ItemPanels.bookmarkPanel.getBookmarkRecipeId(stackover);
        }

        if (recipeId != null) {
            return GuiCraftingRecipe.overlayRecipe(stackover, recipeId, shift);
        }

        return false;
    }

    private static boolean saveRecipeInBookmark(ItemStack stack, boolean saveIngredients, boolean saveStackSize) {

        if (stack != null) {
            final GuiContainer gui = NEIClientUtils.getGuiContainer();
            List<PositionedStack> ingredients = null;
            String handlerName = "";

            if (gui instanceof GuiRecipe) {
                ingredients = ((GuiRecipe<?>) gui).getFocusedRecipeIngredients();
                handlerName = ((GuiRecipe<?>) gui).getHandlerName();
                stack.stackSize = ((GuiRecipe<?>) gui).prepareFocusedRecipeResultStackSize(stack);
            }

            ItemPanels.bookmarkPanel.addOrRemoveItem(stack, handlerName, ingredients, saveIngredients, saveStackSize);
            return true;
        }

        return false;
    }

}
