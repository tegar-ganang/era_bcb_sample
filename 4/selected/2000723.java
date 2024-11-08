package jmirc;

import java.util.*;
import java.io.*;
import javax.microedition.lcdui.*;
import javax.microedition.rms.*;

public class UIHandler {

    private Display display;

    private Window console;

    private boolean header;

    private boolean timestamp;

    private boolean winlock;

    private boolean usecol;

    private boolean mirccol;

    private int buflines;

    private Hashtable channels, privates;

    private Vector windows;

    private Vector favourites;

    protected int currentwin;

    private String hilight;

    public String nick;

    public boolean keylock;

    protected Window scrabble;

    protected Window boggle;

    protected Window crossword;

    private boolean online;

    protected Vector crosswords, crosswordRSIDs;

    protected String botname;

    public UIHandler(Database db, Display disp, boolean online) {
        nick = db.nick;
        keylock = false;
        header = db.header;
        timestamp = db.timestamp;
        hilight = db.hilight;
        buflines = db.buflines;
        usecol = db.usecolor;
        mirccol = db.usemirccol;
        display = disp;
        winlock = false;
        channels = new Hashtable();
        privates = new Hashtable();
        windows = new Vector();
        loadFavs();
        currentwin = 0;
        console = new Window(this, "Status", Window.TYPE_CONSOLE, hilight, header, timestamp, usecol, mirccol, buflines);
        addWindow(console);
        this.online = online;
        crosswords = new Vector();
        crosswordRSIDs = new Vector();
        getCrosswords();
        botname = db.botname;
    }

    public Window getConsole() {
        return console;
    }

    public Window getChannel(String channel) {
        Window win;
        channel = channel.trim();
        win = (Window) channels.get(channel.toUpperCase());
        if (win == null) {
            win = new Window(this, channel, Window.TYPE_CHANNEL, hilight, header, timestamp, usecol, mirccol, buflines);
            channels.put(channel.toUpperCase(), win);
            addWindow(win);
        }
        return win;
    }

    public Window getPrivate(String priv) {
        Window win;
        priv = priv.trim();
        win = (Window) privates.get(priv.toUpperCase());
        if (win == null) {
            win = new Window(this, priv, Window.TYPE_PRIVATE, hilight, header, timestamp, usecol, mirccol, buflines);
            privates.put(priv.toUpperCase(), win);
            addWindow(win);
            if (keylock) playAlarm(true);
        }
        return win;
    }

    public Window getScrabble() {
        Window win;
        if (this.scrabble == null) {
            win = new Window(this, "Scrabble", Window.TYPE_SCRABBLE, hilight, header, timestamp, usecol, mirccol, buflines);
            this.scrabble = win;
            addWindow(win);
        } else win = this.scrabble;
        return win;
    }

    public Window getBoggle() {
        Window win;
        if (boggle == null) {
            win = new Window(this, "Boggle", Window.TYPE_BOGGLE, hilight, header, timestamp, usecol, mirccol, buflines);
            this.boggle = win;
            addWindow(win);
        } else win = boggle;
        return win;
    }

    public Window getCrossword(int type) {
        Window win;
        if (crossword != null) {
            crossword.saveCrossword(crossword.crossword.createMsg());
            crossword.closeCrossword();
        }
        win = new Window(this, "crossword", type, hilight, header, timestamp, usecol, mirccol, buflines);
        this.crossword = win;
        addWindow(win);
        return win;
    }

    public void addWindow(Window win) {
        windows.addElement(win);
        displayWindow(-1);
    }

    public int[] getIndicators() {
        int[] ret = new int[windows.size()];
        for (int i = 0; i < windows.size(); i++) {
            if (i == currentwin) ret[i] = Window.STATE_SELECTED; else ret[i] = ((Window) windows.elementAt(i)).getState();
        }
        return ret;
    }

    public void setHeader(boolean visible) {
        header = visible;
        for (int i = 0; i < windows.size(); i++) ((Window) windows.elementAt(i)).setHeaderVisible(header);
    }

    public void displayNextWindow() {
        displayWindow(currentwin + 1);
    }

    public void displayPreviousWindow() {
        displayWindow(currentwin - 1);
    }

    public void displayWindow(int num) {
        if (winlock) return;
        if (num >= windows.size()) num = 0;
        if (num < 0) num = windows.size() - 1;
        if (num != currentwin) {
            ((Window) windows.elementAt(currentwin)).setState(Window.STATE_NONE);
            setDisplay((Window) windows.elementAt(num));
            currentwin = num;
        }
        System.gc();
    }

    public void displayWindow(Window win) {
        displayWindow(windows.indexOf(win));
    }

    public void deleteWindow(Window win) {
        if (win.getType() == Window.TYPE_PRIVATE) privates.remove(win.getName().toUpperCase());
        if (win.getType() == Window.TYPE_CHANNEL) channels.remove(win.getName().toUpperCase());
        if (win.getType() == Window.TYPE_SCRABBLE) {
            scrabble = null;
        } else if (win.getType() == Window.TYPE_BOGGLE) {
            boggle = null;
        } else if (win.getType() == Window.TYPE_CROSSWORD || win.getType() == Window.TYPE_CROSSWORD_OFFLINE || win.getType() == Window.TYPE_CROSSWORD_DOWNLOAD) {
            crossword = null;
        }
        if (windows.indexOf(win) <= currentwin) {
            currentwin--;
            windows.removeElement(win);
            setDisplay((Window) windows.elementAt(currentwin));
        } else {
            windows.removeElement(win);
            repaint();
        }
        if (win.getType() == Window.TYPE_CHANNEL) {
            jmIrc.writeLine("PART " + win.getName());
        }
    }

    public void setDisplay(Displayable disp) {
        display.setCurrent(disp);
        repaint();
    }

    public Hashtable getChannels() {
        return channels;
    }

    public void clearChanPriv() {
        console.enterExitMode();
        if (currentwin >= 0) {
            setDisplay(console);
            currentwin = 0;
        }
        for (int i = windows.size() - 1; i >= 1; i--) {
            ((Window) windows.elementAt(i)).enterExitMode();
        }
        console.repaint();
    }

    public void cleanup() {
        windows.removeAllElements();
        channels.clear();
        privates.clear();
        currentwin = -1;
        System.gc();
        setDisplay(jmIrc.mainform);
    }

    public void repaint() {
        if (windows.size() > 0) ((Window) windows.elementAt(currentwin)).repaint();
    }

    public void setWinlock(boolean lock) {
        winlock = lock;
    }

    public boolean playAlarm(boolean louder) {
        if (louder) return AlertType.ALARM.playSound(display); else return AlertType.INFO.playSound(display);
    }

    public void loadFavs() {
        try {
            RecordStore rs = RecordStore.openRecordStore("jmircfav", true);
            favourites = new Vector();
            favourites.addElement("/msg elsie hangman");
            favourites.addElement("highscores");
            if (rs.getNumRecords() > 0) {
                byte[] record = rs.enumerateRecords(null, null, false).nextRecord();
                DataInputStream dis = new DataInputStream(new ByteArrayInputStream(record));
                int count = dis.readInt();
                for (int i = 0; i < count; i++) favourites.addElement(dis.readUTF());
                dis.close();
            }
            rs.closeRecordStore();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveFavs() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        try {
            dos.writeInt(favourites.size());
            for (int i = 0; i < favourites.size(); i++) dos.writeUTF((String) favourites.elementAt(i));
            dos.close();
            baos.close();
            RecordStore.deleteRecordStore("jmircfav");
            RecordStore rs = RecordStore.openRecordStore("jmircfav", true);
            byte[] bytes = baos.toByteArray();
            rs.addRecord(bytes, 0, bytes.length);
            rs.closeRecordStore();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addFav(String input) {
        for (int i = 0; i < favourites.size(); i++) {
            if (input.compareTo((String) favourites.elementAt(i)) < 0) {
                favourites.insertElementAt(input, i);
                return;
            }
        }
        favourites.addElement(input);
    }

    public void removeFav(String input) {
        favourites.removeElement(input);
    }

    public Vector getFavs() {
        return favourites;
    }

    public boolean isOnline() {
        return online;
    }

    public void getCrosswords() {
        try {
            crosswordRSIDs.removeAllElements();
            crosswords.removeAllElements();
            RecordStore rs = RecordStore.openRecordStore(Window.STORE_CROSSWORD, true);
            int i = 1, j = 0;
            while (j < rs.getNumRecords()) {
                try {
                    String crosswordMsg = new DataInputStream(new ByteArrayInputStream(rs.getRecord(i))).readUTF();
                    crosswords.addElement(crosswordMsg);
                    crosswordRSIDs.addElement(new Integer(i));
                    i++;
                    j++;
                } catch (InvalidRecordIDException iride) {
                    i++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
