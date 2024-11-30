package codechicken.nei.config.preset;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;

import codechicken.lib.gui.GuiDraw;
import codechicken.lib.vec.Rectangle4i;
import codechicken.nei.Button;
import codechicken.nei.ItemList;
import codechicken.nei.ItemPanel.ItemPanelSlot;
import codechicken.nei.ItemSorter;
import codechicken.nei.ItemsGrid;
import codechicken.nei.Label;
import codechicken.nei.RestartableTask;
import codechicken.nei.api.ItemFilter;

public abstract class PresetItemsGrid extends ItemsGrid {

    protected static final int BUTTON_SIZE = 16;

    public Button pagePrev;
    public Label pageLabel;
    public Button pageNext;

    protected ArrayList<ItemStack> newItems;

    protected final RestartableTask updateFilter = new RestartableTask("NEI Presets Item Filtering") {

        @Override
        public void execute() {
            ArrayList<ItemStack> filtered;
            ItemFilter filter = getFilter();

            try {
                filtered = ItemList.forkJoinPool.submit(
                        () -> ItemList.items.parallelStream().filter(filter::matches)
                                .collect(Collectors.toCollection(ArrayList::new)))
                        .get();
            } catch (InterruptedException | ExecutionException e) {
                filtered = new ArrayList<>();
                e.printStackTrace();
                stop();
            }

            if (interrupted()) return;
            ItemSorter.sort(filtered);
            if (interrupted()) return;
            updateItemList(filtered);
        }
    };

    protected PresetItemsGrid() {

        pageLabel = new Label("0/0", true);

        pagePrev = new Button("<") {

            public boolean onButtonPress(boolean rightclick) {

                if (rightclick) {
                    setPage(0);
                } else {
                    shiftPage(-1);
                }

                return true;
            }
        };

        pageNext = new Button(">") {

            public boolean onButtonPress(boolean rightclick) {

                if (rightclick) {
                    setPage(getNumPages() - 1);
                } else {
                    shiftPage(1);
                }

                return true;
            }
        };

    }

    protected String getLabelText() {
        return String.format("%d/%d", getPage(), Math.max(1, getNumPages()));
    }

    @Override
    public void setGridSize(int mleft, int mtop, int w, int h) {
        pageLabel.text = getLabelText();

        pagePrev.w = pageNext.w = BUTTON_SIZE;
        pagePrev.h = pageNext.h = BUTTON_SIZE;
        pagePrev.y = pageNext.y = mtop;

        pagePrev.x = mleft;
        pageNext.x = mleft + w - pageNext.w;

        pageLabel.x = mleft + w / 2;
        pageLabel.y = pagePrev.y + 5;

        super.setGridSize(mleft, mtop + BUTTON_SIZE + 2, w, h - BUTTON_SIZE - 4);
    }

    @Override
    public void draw(int mousex, int mousey) {
        pagePrev.draw(mousex, mousey);
        pageNext.draw(mousex, mousey);
        pageLabel.draw(mousex, mousey);

        super.draw(mousex, mousey);
    }

    public void mouseClicked(int x, int y, int button) {

        if (pagePrev.contains(x, y)) {
            pagePrev.handleClick(x, y, button);
        }

        if (pageNext.contains(x, y)) {
            pageNext.handleClick(x, y, button);
        }
    }

    public void restartFilter() {
        updateFilter.restart();
    }

    protected void updateItemList(List<ItemStack> newItems) {
        this.newItems = new ArrayList<>(newItems);
    }

    @Override
    public void refresh(GuiContainer gui) {

        if (this.newItems != null) {
            this.realItems = this.newItems;
            this.newItems = null;
            onItemsChanged();
        }

        super.refresh(gui);
    }

    @Override
    protected void beforeDrawSlot(@Nullable ItemPanelSlot focused, int slotIdx, Rectangle4i rect) {
        if (isSelected(getItem(slotIdx))) {
            GuiDraw.drawRect(rect.x, rect.y, rect.w, rect.h, 0xee555555);
        } else {
            super.beforeDrawSlot(focused, slotIdx, rect);
        }
    }

    protected abstract boolean isSelected(ItemStack stack);

    protected abstract ItemFilter getFilter();

}
