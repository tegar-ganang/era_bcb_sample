package com.markpiper.tvtray;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ResourceBundle;
import java.util.Vector;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import org.jdesktop.jdic.tray.SystemTray;
import org.jdesktop.jdic.tray.TrayIcon;
import com.markpiper.tvtray.gui.*;

/**
 * @author unknown
 */
public class TVTray implements ActionListener, WindowListener, ChannelManagerListener, MouseListener, HyperlinkListener, ProgressListener {

    private final String DATA_FILE = Messages.getString("tvtray.datafile");

    private ChannelManager channelManager;

    private TrayIcon appIcon;

    private JMenuItem nowAndNext;

    private JMenuItem quit;

    private JMenuItem refresh;

    private JMenuItem prefs;

    private boolean channelsLoading = false;

    private boolean overHyperlink = false;

    private Vector parseErrors = new Vector();

    private NowAndNextWindow nwnxWin;

    private ChannelListWindow chListWin;

    private InfoWindow info;

    private PreferencesWindow prefsWin;

    private ProgressBarWindow progress;

    private ErrorWindow err;

    private Toolkit toolkit = Toolkit.getDefaultToolkit();

    private boolean quitting = false;

    public static void main(String[] args) {
        try {
            TVTray app = new TVTray();
        } catch (Throwable e) {
            RuntimeHandler.getInstance().uncaughtException(Thread.currentThread(), e);
        }
    }

    public TVTray() {
        try {
            UIManager.setLookAndFeel(Messages.getString("tvtray.laf"));
            UIManager.getLookAndFeelDefaults().put("ClassLoader", getClass().getClassLoader());
        } catch (Exception e) {
        }
        createJdicTrayIcon();
        channelManager = reinstateChannelManager();
        channelManager.addChannelListener(this);
        channelManager.addProgressListener(this);
        channelManager.update();
        int checkUpdate = 0;
        while (!quitting) {
            try {
                Thread.sleep(100);
                checkUpdate++;
                if (checkUpdate > 1000) {
                    channelManager.update();
                    checkUpdate = 0;
                }
            } catch (InterruptedException e1) {
            }
        }
        System.exit(0);
    }

    public void removeNowAndNextWindow() {
        nwnxWin.setVisible(false);
        nwnxWin = null;
    }

    public void removeChannelListWindow() {
        chListWin.setVisible(false);
        chListWin = null;
    }

    public void removeInfoWindow() {
        info.setVisible(false);
        info = null;
    }

    public void removeProgressWindow() {
        progress.setVisible(false);
        progress = null;
    }

    public void removeErrorWindow() {
        err.setVisible(false);
        err = null;
    }

    /** 
     * Callback method for channelManager thread when all channels have finished loading
     *
     */
    public void channelsLoaded(boolean ok) {
        Image img = (ok == true ? toolkit.createImage(Messages.getString("tvtray.okicon")) : toolkit.createImage(Messages.getString("tvtray.notokicon")));
        Icon i = new ImageIcon(img);
        appIcon.setIcon(i);
        appIcon.setToolTip(Messages.getString("tvtray.appName"));
        nowAndNext.setEnabled(true);
        refresh.setEnabled(true);
        prefs.setEnabled(true);
        channelsLoading = false;
        if (progress != null) {
            removeProgressWindow();
        }
        if (parseErrors.size() != 0 && err == null) {
            err = new ErrorWindow();
            err.setErrors(parseErrors);
            err.addCloseListener(this);
            err.setVisible(true);
        }
        serializeChannelManager();
    }

    /** 
     * Callback method for channelManager thread when fetching commences
     */
    public void fetchingChannels() {
        parseErrors.clear();
        Icon i = new ImageIcon(toolkit.createImage(Messages.getString("tvtray.fetchingicon")));
        appIcon.setIcon(i);
        nowAndNext.setEnabled(false);
        refresh.setEnabled(false);
        prefs.setEnabled(false);
        progress = new ProgressBarWindow();
        progress.addCloseListener(this);
        progress.setVisible(true);
        channelManager.addProgressListener(progress);
        channelsLoading = true;
    }

    public void fetchingProgress(int prog) {
        appIcon.setToolTip("Fetching..." + String.valueOf(prog) + "%");
        if (progress != null) progress.setProgress(prog);
    }

    public void errorOccurred(Channel c, Exception e) {
        String errMessage = Messages.getString(e.getClass().getSimpleName()) + " : " + e.getMessage();
        parseErrors.add("<b>" + c.getAlias() + "</b><br>" + errMessage);
    }

    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        if (((command.equals("Now & Next")) || (command.equals("PressAction"))) && nwnxWin == null) {
            if (channelsLoading == false) {
                nwnxWin = new NowAndNextWindow(channelManager);
                nwnxWin.addChannelListListener(this, this);
                nwnxWin.addCloseListener(this);
                nwnxWin.setVisible(true);
            } else {
                if (progress == null) {
                    progress = new ProgressBarWindow();
                    progress.addCloseListener(this);
                    progress.setVisible(true);
                }
            }
            return;
        }
        if (command.equals("Quit")) {
            serializeChannelManager();
            quitting = true;
            return;
        }
        if ((nwnxWin != null) && (command.equals("close " + nwnxWin.getClass()))) {
            removeNowAndNextWindow();
            return;
        }
        if ((chListWin != null) && (command.equals("close " + chListWin.getClass()))) {
            removeChannelListWindow();
            return;
        }
        if ((info != null) && (command.equals("close " + info.getClass()))) {
            removeInfoWindow();
            return;
        }
        if ((progress != null) && (command.equals("close " + progress.getClass()))) {
            removeProgressWindow();
            return;
        }
        if ((err != null) && (command.equals("close " + err.getClass()))) {
            removeErrorWindow();
            return;
        }
        if (command.equals("Preferences...")) {
            if (prefsWin == null) {
                prefsWin = new PreferencesWindow(channelManager);
                prefsWin.addButtonListener(this);
            }
            return;
        }
        if (command.equals("Refresh")) {
            channelManager.update();
            return;
        }
        if (command.equals("Prefs Cancel")) {
            prefsWin.setVisible(false);
            prefsWin = null;
        }
        if (command.equals("Prefs OK")) {
            prefsWin.savePreferences();
            channelManager = prefsWin.getListModel().getUpdatedManager();
            channelManager.setBaseURL(prefsWin.getBaseURL());
            channelManager.hideNotOnAir(prefsWin.getHideNotOnAir());
            channelManager.writeProperties();
            prefsWin.setVisible(false);
            prefsWin = null;
            serializeChannelManager();
        }
    }

    public void windowClosing(WindowEvent arg0) {
        System.exit(0);
    }

    public void windowClosed(WindowEvent arg0) {
        System.exit(0);
    }

    public void windowActivated(WindowEvent arg0) {
    }

    public void windowDeactivated(WindowEvent arg0) {
    }

    public void windowDeiconified(WindowEvent arg0) {
    }

    public void windowIconified(WindowEvent arg0) {
    }

    public void windowOpened(WindowEvent arg0) {
    }

    public void hyperlinkUpdate(HyperlinkEvent evt) {
        if (evt.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            info = new InfoWindow(evt.getURL());
            info.setVisible(true);
        } else if (evt.getEventType() == HyperlinkEvent.EventType.ENTERED) {
            nwnxWin.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            overHyperlink = true;
        } else if (evt.getEventType() == HyperlinkEvent.EventType.EXITED) {
            nwnxWin.setCursor(Cursor.getDefaultCursor());
            overHyperlink = false;
        }
    }

    public void mouseClicked(MouseEvent evt) {
        if (overHyperlink) return;
        NowAndNextLabel srcPane;
        if (evt.getButton() != MouseEvent.BUTTON1) {
            return;
        }
        try {
            srcPane = (NowAndNextLabel) evt.getSource();
        } catch (ClassCastException e) {
            return;
        }
        int colonPos = srcPane.getName().indexOf(":");
        if (srcPane.getName().substring(0, 7).equals("channel")) {
            String channelName = srcPane.getName().substring(colonPos + 1, srcPane.getName().length());
            Channel ch = channelManager.getChannel(channelName);
            if (chListWin == null) {
                chListWin = new ChannelListWindow(ch);
                chListWin.addCloseListener(this);
                chListWin.setScroll(true);
                chListWin.setVisible(true);
                chListWin.scrollToReference(srcPane.getAnchor());
            } else {
                chListWin.reloadWindow(ch);
                chListWin.setTitle("Today's Listing for " + ch.getAlias());
                chListWin.scrollToReference(srcPane.getAnchor());
            }
        }
    }

    public void mouseEntered(MouseEvent evt) {
        JEditorPane srcPane;
        try {
            srcPane = (JEditorPane) evt.getSource();
            srcPane.setBackground(new Color(200, 200, 250));
        } catch (ClassCastException e) {
            return;
        }
    }

    public void mouseExited(MouseEvent evt) {
        JEditorPane srcPane;
        try {
            srcPane = (JEditorPane) evt.getSource();
            srcPane.setBackground(Color.WHITE);
        } catch (ClassCastException e) {
            return;
        }
    }

    public void mousePressed(MouseEvent arg0) {
    }

    public void mouseReleased(MouseEvent arg0) {
    }

    /**
     * Serializes the channel manager and all it's data to a file tvtray.dat
     *
     */
    private void serializeChannelManager() {
        FileOutputStream fos = null;
        ObjectOutputStream out = null;
        try {
            fos = new FileOutputStream(DATA_FILE);
            out = new ObjectOutputStream(fos);
            out.writeObject(channelManager);
            out.close();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "Data could not be written", "tvtray Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Attempts to reinstate a ChannelManager object from a file and return it.  If not possible
     * a new ChannelManager is created and returned.
     * 
     * @return a ChannelManager object
     */
    private ChannelManager reinstateChannelManager() {
        FileInputStream fis = null;
        ObjectInputStream in = null;
        ChannelManager retval;
        try {
            fis = new FileInputStream(DATA_FILE);
            in = new ObjectInputStream(fis);
            retval = (ChannelManager) in.readObject();
            in.close();
        } catch (IOException e) {
            retval = new ChannelManager();
        } catch (ClassNotFoundException e) {
            retval = new ChannelManager();
        } catch (Exception e) {
            e.printStackTrace();
            retval = null;
        }
        return retval;
    }

    /**
     * Fetches channel data from url
     */
    private void updateChannelManager() {
        Thread th = new Thread(channelManager);
        th.start();
    }

    private void createJdicTrayIcon() {
        Icon i = new ImageIcon(toolkit.createImage(Messages.getString("tvtray.notokicon")));
        appIcon = new TrayIcon(i);
        SystemTray.getDefaultSystemTray().addTrayIcon(appIcon);
        appIcon.addActionListener(this);
        nowAndNext = new JMenuItem("Now & Next");
        nowAndNext.addActionListener(this);
        nowAndNext.setEnabled(false);
        quit = new JMenuItem("Quit");
        quit.addActionListener(this);
        refresh = new JMenuItem("Refresh");
        refresh.addActionListener(this);
        refresh.setEnabled(false);
        prefs = new JMenuItem("Preferences...");
        prefs.addActionListener(this);
        prefs.setEnabled(false);
        JSeparator sep = new JSeparator();
        JPopupMenu popUpMenu = new JPopupMenu();
        popUpMenu.add(nowAndNext);
        popUpMenu.add(sep);
        popUpMenu.add(prefs);
        popUpMenu.add(refresh);
        popUpMenu.add(quit);
        appIcon.setPopupMenu(popUpMenu);
        channelsLoading = true;
    }
}
