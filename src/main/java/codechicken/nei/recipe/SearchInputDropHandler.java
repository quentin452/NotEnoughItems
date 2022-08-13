package codechicken.nei.recipe;

import static codechicken.nei.LayoutManager.searchField;

import codechicken.nei.api.INEIGuiAdapter;
import java.util.regex.Pattern;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fluids.FluidStack;

public class SearchInputDropHandler extends INEIGuiAdapter {

    protected static final Pattern SPECIAL_REGEX_CHARS = Pattern.compile("[{}()\\[\\].+*?^$\\\\|]");

    @Override
    public boolean handleDragNDrop(GuiContainer gui, int mouseX, int mouseY, ItemStack draggedStack, int button) {

        if (searchField.isVisible() && searchField.contains(mouseX, mouseY)) {
            final FluidStack fluidStack = StackInfo.getFluid(draggedStack);

            if (fluidStack != null) {
                searchField.setText(formattingText(fluidStack.getLocalizedName()));
            } else {
                searchField.setText(formattingText(draggedStack.getDisplayName()));
            }

            return true;
        }

        return false;
    }

    protected String formattingText(String displayName) {
        return SPECIAL_REGEX_CHARS
                .matcher(EnumChatFormatting.getTextWithoutFormattingCodes(displayName))
                .replaceAll("\\\\$0");
    }
}
