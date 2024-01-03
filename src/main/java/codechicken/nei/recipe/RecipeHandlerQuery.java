package codechicken.nei.recipe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import codechicken.core.TaskProfiler;
import codechicken.nei.ItemList;
import codechicken.nei.NEIClientConfig;

class RecipeHandlerQuery<T extends IRecipeHandler> {

    private final Function<T, T> recipeHandlerFunction;
    private final List<T> recipeHandlers;
    private final List<T> serialRecipeHandlers;
    private final String[] errorMessage;
    private boolean error = false;

    public RecipeHandlerQuery(
            Function<T, T> recipeHandlerFunction,
            List<T> recipeHandlers,
            List<T> serialRecipeHandlers,
            String... errorMessage
    ) {
        this.recipeHandlerFunction = recipeHandlerFunction;
        this.recipeHandlers = recipeHandlers;
        this.serialRecipeHandlers = serialRecipeHandlers;
        this.errorMessage = errorMessage;
    }

     ArrayList<T> runWithProfiling(String profilerSection) {
        TaskProfiler profiler = ProfilerRecipeHandler.getProfiler();
        profiler.start(profilerSection);

        try {
            ArrayList<T> handlers = getRecipeHandlersParallel();
            if (error) {
                displayRecipeLookupError();
            }
            return handlers;
        } catch (InterruptedException | ExecutionException e) {
            handleExecutionException(e);
            return new ArrayList<>();
        } finally {
            profiler.end();
        }
    }

    private ArrayList<T> getRecipeHandlersParallel() throws InterruptedException, ExecutionException {
        FuelRecipeHandler.findFuelsOnceParallel();
        ArrayList<T> handlers = new ArrayList<>();
        handlers.addAll(getValidHandlers(serialRecipeHandlers));
        handlers.addAll(getValidHandlers(recipeHandlers));
        handlers.sort(NEIClientConfig.HANDLER_COMPARATOR);
        return handlers;
    }

    private ArrayList<T> getValidHandlers(List<T> handlers) {
        return handlers.parallelStream()
                .map(handler -> {
                    try {
                        return recipeHandlerFunction.apply(handler);
                    } catch (Throwable t) {
                        handleThrowable(t);
                        return null;
                    }
                })
                .filter(h -> h != null && h.numRecipes() > 0)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private void handleExecutionException(Exception e) {
        printLog(e);
        displayRecipeLookupError();
    }

    private void handleThrowable(Throwable t) {
        printLog(t);
        error = true;
    }

    private void printLog(Throwable t) {
        Arrays.stream(errorMessage).forEach(msg -> NEIClientConfig.logger.error(msg));
        t.printStackTrace();
    }

    private void displayRecipeLookupError() {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (player != null) {
            IChatComponent chat = new ChatComponentTranslation("nei.chat.recipe.error");
            chat.getChatStyle().setColor(EnumChatFormatting.RED);
            player.addChatComponentMessage(chat);
        }
    }
}