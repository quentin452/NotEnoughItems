package codechicken.nei.config;

import static codechicken.lib.gui.GuiDraw.drawRect;
import static codechicken.lib.gui.GuiDraw.drawStringC;

import codechicken.nei.NEIClientUtils;
import java.awt.Rectangle;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.input.Keyboard;

public class OptionKeyBind extends OptionButton {

    private boolean editing = false;
    private boolean useHash = false;
    private int keyboard = Keyboard.CHAR_NONE;

    public OptionKeyBind(String name) {
        this(name, false);
    }

    public OptionKeyBind(final String name, final boolean useHash) {
        super("keys." + name);
        this.useHash = useHash;
    }

    @Override
    public void onMouseClicked(int mousex, int mousey, int button) {
        if (editing) {
            resetChanges();
        }
    }

    @Override
    public void keyTyped(char c, int keycode) {

        if (!editing) {
            return;
        }

        if (keycode == Keyboard.KEY_RETURN) {
            applyChanges();
            return;
        }

        keyboard = useHash ? NEIClientUtils.getKeyHash() : keycode;

        if (keyboard != Keyboard.CHAR_NONE) {
            applyChanges();
        }
    }

    @Override
    public boolean onClick(int btn) {

        if (editing) {
            return true;
        } else if (btn == 0) {
            editing = true;
            return true;
        } else if (btn == 1) {
            keyboard = Keyboard.CHAR_NONE;
            applyChanges();
            return true;
        }

        return false;
    }

    protected void applyChanges() {
        setValue(keyboard);
        resetChanges();
    }

    protected void resetChanges() {
        editing = false;
        keyboard = Keyboard.CHAR_NONE;
    }

    public boolean conflicted() {
        final int keyboard = getValue();

        if (keyboard <= 0) {
            return false;
        }

        for (Option o : slot.options) {
            if (o instanceof OptionKeyBind && o != this && ((OptionKeyBind) o).getValue() == keyboard) {
                return true;
            }
        }

        return false;
    }

    public void setValue(int keycode) {
        getTag().setIntValue(keycode);
    }

    public int getValue() {
        return renderTag().getIntValue();
    }

    @Override
    public String getPrefix() {
        return translateN(name);
    }

    @Override
    public void drawButton(int mx, int my) {

        if (editing) {
            Rectangle b = buttonSize();
            drawRect(b.x, b.y, b.width, b.height, -6250336);
            drawRect(b.x + 1, b.y + 1, b.width - 2, b.height - 2, -16777216);
            drawStringC(getButtonText(), b.x, b.y, b.width, b.height, getTextColour(mx, my));
        } else {
            super.drawButton(mx, my);
        }
    }

    @Override
    public String getButtonText() {
        String keyText = "";

        if (useHash) {
            final int metahash = Math.max(editing ? NEIClientUtils.getMetaHash() : getValue(), 0);
            final String DELIMITER = EnumChatFormatting.GRAY + " + " + EnumChatFormatting.RESET;

            if ((metahash & NEIClientUtils.CTRL_HASH) == NEIClientUtils.CTRL_HASH) {
                keyText += NEIClientUtils.translate(Minecraft.isRunningOnMac ? "key.ctrl.mac" : "key.ctrl") + DELIMITER;
            }

            if ((metahash & NEIClientUtils.SHIFT_HASH) == NEIClientUtils.SHIFT_HASH) {
                keyText += "SHIFT" + DELIMITER;
            }

            if ((metahash & NEIClientUtils.ALT_HASH) == NEIClientUtils.ALT_HASH) {
                keyText += "ALT" + DELIMITER;
            }
        }

        if (!editing) {
            keyText += Keyboard.getKeyName(
                    getValue() & ~(NEIClientUtils.CTRL_HASH | NEIClientUtils.SHIFT_HASH | NEIClientUtils.ALT_HASH));
        }

        return keyText;
    }

    @Override
    public int getTextColour(int mousex, int mousey) {
        return !editing && conflicted() ? 0xFFFF6666 : -1;
    }
}
