package codechicken.nei.api;

import static codechicken.lib.gui.GuiDraw.drawRect;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.inventory.Slot;

import org.lwjgl.opengl.GL11;

import codechicken.nei.PositionedStack;
import codechicken.nei.guihook.GuiContainerManager;

public class DefaultOverlayRenderer implements IRecipeOverlayRenderer {

    public DefaultOverlayRenderer(List<PositionedStack> ai, IStackPositioner positioner) {
        positioner = this.positioner = positioner;
        ingreds = new ArrayList<>();
        for (PositionedStack stack : ai) ingreds.add(stack.copy());
        ingreds = positioner.positionStacks(ingreds);
    }

    @Override
    public void renderOverlay(GuiContainerManager gui, Slot slot) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(770, 1);

        for (PositionedStack stack : ingreds) {
            if (stack.relx == slot.xDisplayPosition && stack.rely == slot.yDisplayPosition && !slot.getHasStack()) {
                GuiContainerManager.drawItem(stack.relx, stack.rely, stack.item);
                drawHover(stack.relx, stack.rely);
            }
        }

        GL11.glPopAttrib();
    }

    private static void drawHover(int x, int y) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        drawRect(x, y, 16, 16, 0x66555555);
        GL11.glPopAttrib();
    }

    final IStackPositioner positioner;
    ArrayList<PositionedStack> ingreds;
}
