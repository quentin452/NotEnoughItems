package codechicken.nei.recipe;

import java.util.ArrayList;
import java.util.Arrays;

import net.minecraft.client.Minecraft;

import codechicken.nei.NEIClientConfig;
import codechicken.nei.NEIClientUtils;

public class GuiUsageRecipe extends GuiRecipe<IUsageHandler> {

    public static ArrayList<IUsageHandler> usagehandlers = new ArrayList<>();
    public static ArrayList<IUsageHandler> serialUsageHandlers = new ArrayList<>();

    public static boolean openRecipeGui(String inputId, Object... ingredients) {
        try {
            ArrayList<IUsageHandler> handlers = getiUsageHandlers(inputId, ingredients);

            if (handlers.isEmpty()) {
                NEIClientConfig.logger.warn("No handlers found for the given input and ingredients.");
                return false;
            }

            Minecraft mc = NEIClientUtils.mc();
            BookmarkRecipeId recipeId = getCurrentRecipe(mc.currentScreen);
            GuiUsageRecipe gui = new GuiUsageRecipe(handlers, recipeId);

            mc.displayGuiScreen(gui);
            gui.openTargetRecipe(gui.recipeId);

            return true;
        } catch (Exception e) {
            NEIClientConfig.logger.error("Error opening recipe GUI: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private static ArrayList<IUsageHandler> getiUsageHandlers(String inputId, Object[] ingredients) {
        RecipeHandlerQuery<IUsageHandler> recipeQuery = new RecipeHandlerQuery<>(
                h -> getUsageOrCatalystHandler(h, inputId, ingredients),
                usagehandlers,
                serialUsageHandlers,
                "Error while looking up usage recipe",
                "inputId: " + inputId,
                "ingredients: " + Arrays.toString(ingredients));
        ArrayList<IUsageHandler> handlers = recipeQuery.runWithProfiling("recipe.concurrent.usage");
        return handlers;
    }

    private GuiUsageRecipe(ArrayList<IUsageHandler> handlers, BookmarkRecipeId recipeId) {
        super(NEIClientUtils.mc().currentScreen);
        this.currenthandlers = handlers;
        this.recipeId = recipeId;
    }

    public static void registerUsageHandler(IUsageHandler handler) {
        final String handlerId = handler.getHandlerId();
        if (usagehandlers.stream().anyMatch(h -> h.getHandlerId().equals(handlerId))
                || serialUsageHandlers.stream().anyMatch(h -> h.getHandlerId().equals(handlerId)))
            return;

        if (NEIClientConfig.serialHandlers.contains(handlerId)) serialUsageHandlers.add(handler);
        else usagehandlers.add(handler);
    }

    private static IUsageHandler getUsageOrCatalystHandler(IUsageHandler handler, String inputId,
            Object... ingredients) {
        boolean skipCatalyst = NEIClientUtils.controlKey();
        if (NEIClientConfig.areJEIStyleRecipeCatalystsVisible() && !skipCatalyst) {
            return handler.getUsageAndCatalystHandler(inputId, ingredients);
        } else {
            return handler.getUsageHandler(inputId, ingredients);
        }
    }

    @Override
    public ArrayList<IUsageHandler> getCurrentRecipeHandlers() {
        return currenthandlers;
    }
}
