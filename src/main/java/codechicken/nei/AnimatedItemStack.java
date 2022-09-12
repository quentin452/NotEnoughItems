package codechicken.nei;

import codechicken.nei.guihook.GuiContainerManager;
import net.minecraft.item.ItemStack;

public class AnimatedItemStack {

    private static final int steep = 5;

    private int lastPosX;
    private int lastPosY;

    public ItemStack itemStack;

    public AnimatedItemStack(ItemStack itemStack, int startPosX, int startPosY) {
        this.itemStack = itemStack;
        lastPosX = startPosX;
        lastPosY = startPosY;
    }

    public void drawItem(int x, int y) {
        drawItem(x, y, false, null);
    }

    public void drawItem(int x, int y, boolean smallAmount, String stackSize) {
        if (x != lastPosX || y != lastPosY) {
            drawMovingItem(x, y, smallAmount, stackSize);
        } else {
            GuiContainerManager.drawItem(x, y, itemStack, smallAmount, stackSize);
        }
    }

    private void drawMovingItem(int x, int y, boolean smallAmount, String stackSize) {
        int xDiff = x - lastPosX;
        int yDiff = y - lastPosY;
        int moveX = xDiff / steep + ((xDiff % steep == 0) ? 0 : (xDiff > 0 ? 1 : -1));
        int moveY = yDiff / steep + ((yDiff % steep == 0) ? 0 : (yDiff > 0 ? 1 : -1));
        GuiContainerManager.drawItem(lastPosX + moveX, lastPosY + moveY, itemStack, smallAmount, stackSize);
        lastPosX = lastPosX + moveX;
        lastPosY = lastPosY + moveY;
    }
}
