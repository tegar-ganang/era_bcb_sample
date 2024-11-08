package com.potg.games.sudoku;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Image;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import com.playonthego.client.sudoku.SuDokuGameState;
import com.potg.midlet.Callback;
import com.potg.midlet.ImageStore;
import com.potg.midlet.InitializerSplashScreen;
import com.potg.midlet.MenuScreen;
import com.potg.midlet.OptionsScreen;
import com.potg.midlet.SplashListener;
import com.potg.midlet.TextScreen;

/**
 * @author Rory Graves
 * 
 */
public class SuDokuMidlet extends MIDlet implements SplashListener {

    public static final int COLOR_BACKGROUND = 0xC69C6D;

    public Display display;

    protected String versionString;

    GameScreen gameScreen;

    boolean initialised = false;

    SuDokuManager manager;

    MenuScreen menuScreen;

    public void exitRequest() {
        destroyApp(false);
        notifyDestroyed();
    }

    public MIDlet getMidlet() {
        return this;
    }

    public void initApp() {
        menuScreen = new MenuScreen(this);
        gameScreen = new GameScreen(this);
    }

    public void loadOptions() {
        System.out.println("loadOptions() started");
        try {
            RecordStore store = RecordStore.openRecordStore("STORE", true);
            if (store.getNumRecords() > 0) {
                byte[] data = store.getRecord(1);
                try {
                    ByteArrayInputStream bais = new ByteArrayInputStream(data);
                    DataInputStream dis = new DataInputStream(bais);
                    SuDokuManager.soundOn = dis.readBoolean();
                    readCurrentPuzzle(dis);
                    SuDokuManager.overwriteOn = dis.readBoolean();
                } catch (IOException ioe) {
                    System.out.println("loadOptions() IOException " + ioe);
                    return;
                }
            } else {
                System.out.println("loadOptions() Store empty");
            }
            store.closeRecordStore();
        } catch (RecordStoreException rse) {
            System.out.println("loadOptions() recordStoreException:" + rse);
        }
        System.out.println("loadOptions() complete");
    }

    public void mAddMenuItem(String text, int cmd) {
        menuScreen.addMenuItem(text, cmd);
    }

    public void mClearMenu() {
        menuScreen.clearMenu();
    }

    public void mSetTitles(boolean titleScreen, String title, String prompt) {
        menuScreen.setTitles(titleScreen, title, prompt);
    }

    public int mShowMenu(boolean backVis, int backCmd) {
        return menuScreen.showMenu(backVis, backCmd);
    }

    public void mShowMenu(Callback callback) {
        menuScreen.showMenuNonBlocking(callback);
    }

    public int mShowMenu(int index, boolean backVis, int backCmd) {
        return menuScreen.showMenu(index, backVis, backCmd);
    }

    public void readCurrentPuzzle(DataInputStream dis) throws IOException {
        boolean puzzleStored = dis.readBoolean();
        if (!puzzleStored) {
            manager.state = null;
            return;
        }
        SuDokuGameState state = new SuDokuGameState();
        state.decodeFromStream(dis);
        manager.state = state;
    }

    public void saveOptions() {
        System.out.println("SaveOptions() called");
        try {
            RecordStore store = RecordStore.openRecordStore("STORE", true);
            byte[] data;
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(baos);
                dos.writeBoolean(SuDokuManager.soundOn);
                writeCurrentPuzzle(dos);
                dos.writeBoolean(SuDokuManager.overwriteOn);
                data = baos.toByteArray();
            } catch (IOException ioe) {
                System.out.println("SaveOptions.IOException");
                return;
            }
            if (store.getNumRecords() > 0) {
                store.setRecord(1, data, 0, data.length);
            } else {
                store.addRecord(data, 0, data.length);
            }
            store.closeRecordStore();
        } catch (RecordStoreException rse) {
            System.out.println("SaveOptions() RecordStoreException:" + rse);
        }
        System.out.println("SaveOptions() complete");
    }

    /**
     * @param state
     */
    public void showGameScreen(SuDokuGameState state) {
        gameScreen.show(state);
    }

    public void showOptionsScreen() {
        OptionsScreen os = new OptionsScreen(this);
        os.show();
    }

    public void showTextScreen(String title, String text) {
        TextScreen helpScreen = new TextScreen(this, title, text);
        helpScreen.show();
    }

    public void splashFinished() {
        manager = new SuDokuManager(this);
    }

    /**
     * 
     */
    public void stateChange() {
        saveOptions();
    }

    public void writeCurrentPuzzle(DataOutputStream dos) throws IOException {
        if (manager.state == null) {
            dos.writeBoolean(false);
            return;
        }
        dos.writeBoolean(true);
        manager.state.encodeToStream(dos);
    }

    protected void destroyApp(boolean arg0) {
    }

    protected void pauseApp() {
    }

    protected void startApp() throws MIDletStateChangeException {
        if (!initialised) {
            versionString = this.getAppProperty("MIDlet-Version");
            this.display = Display.getDisplay(this);
            Image splashImage = ImageStore.loadImage("splash");
            InitializerSplashScreen iis = new InitializerSplashScreen(display, splashImage, COLOR_BACKGROUND, this);
            display.setCurrent(iis);
            initialised = true;
        }
    }
}
