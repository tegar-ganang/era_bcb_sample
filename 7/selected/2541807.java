package jasel.gui;

import jasel.actor.HitArea;
import jasel.engine.Kernel;
import jasel.engine.Renderer;
import jasel.gui.FocusManager.FocusEvent;
import jasel.io.KeyInput;
import jasel.io.KeyInput.Key;
import jasel.io.KeyInput.KeyBuffer;
import jasel.kernel.BasicTask;
import jasel.kernel.BasicTaskWrapper;
import jasel.kernel.Priority;
import jasel.ui.gui.TextFieldUI;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.input.Keyboard;

/**
 * 
 */
public class TextField extends Widget implements BasicTask {

    private static TextFieldUI ui;

    /** The buffer holding the text */
    protected char[] text;

    /** 
	 * roughly the number of columsn this TextField has
	 * rough because most fonts are not monospaced
	 */
    private int cols;

    /**
	 * The current spot in text that will receive a character,
	 * the "cursor" essentially
	 */
    protected int textIndex;

    /**
	 * Whether this field currently holds focus. If it has focus,
	 * it will accept input from the keyboard
	 */
    protected boolean focused;

    /**
	 * The source of input
	 */
    private KeyInput input;

    private List<ActionListener> listeners;

    protected void informListeners() {
        ActionEvent e = new ActionEvent(this, getText());
        for (ActionListener al : listeners) {
            al.actionPerformed(e);
        }
    }

    private void pack() {
        width = ui.getWidth(cols);
        height = ui.getHeight();
        hitArea = HitArea.createRect(0, 0, width, height);
    }

    private void addChar(char c, boolean shift) {
        if (textIndex == text.length) {
            return;
        }
        if (shift && Character.isLetter(c)) {
            c = Character.toUpperCase(c);
        }
        text[textIndex] = c;
        ++textIndex;
    }

    private void moveIndex(int delta) {
        textIndex += delta;
        if (textIndex < 0 || textIndex >= text.length) {
            textIndex -= delta;
        }
    }

    private void backspace() {
        if (textIndex == 0) {
            return;
        }
        int nul = textIndex;
        while (text[nul] != 0 && nul < text.length) {
            ++nul;
        }
        for (int i = textIndex - 1; i < nul; ++i) {
            text[i] = text[i + 1];
        }
        text[nul] = 0;
        --textIndex;
    }

    private void delete() {
        ++textIndex;
        backspace();
    }

    public TextField(int cols, int max) {
        this.cols = cols;
        if (cols < 1) {
            throw new IllegalArgumentException("TextField must have at least one column: " + cols);
        }
        text = new char[max];
        for (char c : text) {
            c = 0;
        }
        textIndex = 0;
        if (ui == null) {
            ui = (TextFieldUI) Widget.getUI("TextField");
        }
        pack();
        input = KeyInput.getInstance();
        BasicTaskWrapper btw = new BasicTaskWrapper(this, Priority.Normal);
        Kernel.getKernel().addTask(btw);
        listeners = new ArrayList<ActionListener>();
    }

    public void addActionListener(ActionListener al) {
        listeners.add(al);
    }

    public boolean removeActionListener(ActionListener al) {
        return listeners.remove(al);
    }

    public void removeAllActionListeners() {
        listeners.clear();
    }

    public boolean isFocused() {
        return focused;
    }

    public char[] getRawText() {
        return text;
    }

    public int getIndex() {
        return textIndex;
    }

    public String getText() {
        int i;
        for (i = 0; i < text.length; ++i) {
            if (text[i] == 0) break;
        }
        return new String(text, 0, i);
    }

    public void setText(String text) {
        int i;
        for (i = 0; i < text.length(); ++i) {
            if (i >= this.text.length) {
                return;
            }
            this.text[i] = text.charAt(i);
        }
        textIndex = i;
        for (; i < this.text.length; ++i) {
            this.text[i] = 0;
        }
    }

    public void clearText() {
        setText("");
    }

    public int getNumCols() {
        return cols;
    }

    public void draw(long millis, Renderer r) {
        ui.draw(this, millis, r);
    }

    public void update(long millis) {
        if (!focused) {
            return;
        }
        KeyBuffer buff = input.getBuffer();
        boolean shiftDown = buff.isShiftDown();
        Key k = null;
        while ((k = buff.next()) != null) {
            int code = k.getCode();
            char character = k.getCharacter();
            if (code == Keyboard.KEY_RETURN) {
                focused = false;
                informListeners();
                return;
            } else if (code == Keyboard.KEY_BACK) {
                backspace();
            } else if (code == Keyboard.KEY_LEFT) {
                moveIndex(-1);
            } else if (code == Keyboard.KEY_RIGHT) {
                moveIndex(1);
            } else if (code == Keyboard.KEY_DELETE) {
                delete();
            } else if (character < 256 && character > 0) {
                addChar(character, shiftDown);
            }
        }
    }

    public boolean canKill() {
        return false;
    }

    public void clickedIn(FocusEvent fe) {
        focused = true;
    }

    public void clickedOut() {
        focused = false;
    }
}
