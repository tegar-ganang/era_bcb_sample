package net.hypotenubel.jaicwain.gui.swing;

import java.awt.*;
import java.awt.event.*;
import java.text.DateFormat;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import net.hypotenubel.irc.*;
import net.hypotenubel.irc.net.*;
import net.hypotenubel.jaicwain.*;
import net.hypotenubel.jaicwain.gui.docking.*;
import net.hypotenubel.jaicwain.local.LocalizationEventListener;
import net.hypotenubel.jaicwain.session.*;
import net.hypotenubel.util.swing.*;

/**
 * Jaic Wain's main window class. It's managed by a {@code WindowManager}
 * and features stuff.
 * 
 * @author Christoph Daniel Schulze
 * @version $Id: MainFrame.java 154 2006-10-07 21:34:14Z captainnuss $
 */
public class MainFrame extends ManagedJFrame implements ChangeListener, ComponentListener, IRCConnectionListener, LocalizationEventListener {

    /**
     * Currently active session.
     */
    private AbstractIRCSession currentSession = null;

    /**
     * {@code JPanel} containing the tabbed channel container.
     */
    private JPanel centerPanel = null;

    /**
     * {@code TabbedChannelContainer} that will take care of displaying
     * stuff.
     */
    private TabbedChannelContainer tab = null;

    /**
     * {@code int} indicating this frame's x position. If this frame is
     * maximized, this variable stores the former x position.
     */
    private int frameX = 0;

    /**
     * {@code int} indicating this frame's y position. If this frame is
     * maximized, this variable stores the former y position.
     */
    private int frameY = 0;

    /**
     * {@code int} indicating this frame's height. If this frame is
     * maximized, this variable stores the former height.
     */
    private int frameHeight = 0;

    /**
     * {@code int} indicating this frame's width. If this frame is
     * maximized, this variable stores the former width.
     */
    private int frameWidth = 0;

    private JMenuBar menuBar = null;

    private JMenu serverMenu = null;

    private JMenu channelMenu = null;

    private JMenu extrasMenu = null;

    private JPanel statusBar = null;

    private JScannerBar scannerBar = null;

    private JLabel statusLabel = null;

    private JLabel timeLabel = null;

    /**
     * Creates a new {@code MainFrame} object and initializes it.
     * 
     * @param actions {@code ActionMap} to use.
     * @param inputs {@code InputMap} to use.
     */
    public MainFrame(ActionMap actions, InputMap inputs) {
        super();
        this.addComponentListener(this);
        App.localization.addLocalizationEventListener(this);
        this.addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                App.options.beginUpdate("gui");
                App.options.setIntOption("gui", "mainframe.x", frameX);
                App.options.setIntOption("gui", "mainframe.y", frameY);
                App.options.setIntOption("gui", "mainframe.height", frameHeight);
                App.options.setIntOption("gui", "mainframe.width", frameWidth);
                App.options.setIntOption("gui", "mainframe.state", getExtendedState());
                App.options.endUpdate("gui", false);
                closing();
            }
        });
        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        this.setTitle(App.APP_SHORT_NAME + " " + App.APP_VERSION);
        this.createUI();
        this.adaptUI();
        this.setIconImage(Utils.loadImage("icon-app.gif"));
        this.setVisible(true);
        this.setSize(App.options.getIntOption("gui", "mainframe.width", 800), App.options.getIntOption("gui", "mainframe.height", 600));
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        screen.setSize((screen.getWidth() / 2 - getWidth() / 2), (screen.getHeight() / 2 - getHeight() / 2));
        this.setLocation(App.options.getIntOption("gui", "mainframe.x", (int) screen.getWidth()), App.options.getIntOption("gui", "mainframe.y", (int) screen.getHeight()));
        this.setExtendedState(App.options.getIntOption("gui", "mainframe.state", Frame.NORMAL));
    }

    /**
     * Initializes the GUI stuff.
     */
    private void createUI() {
        menuBar = new JMenuBar();
        serverMenu = new JMenu();
        menuBar.add(serverMenu);
        channelMenu = new JMenu();
        menuBar.add(channelMenu);
        extrasMenu = new JMenu();
        menuBar.add(extrasMenu);
        ArrayList<ArrayList<Action>> menus = App.gui.getMenuItems();
        for (int i = 0; i < menus.size(); i++) for (int j = 0; j < menus.get(i).size(); j++) addMenuItem(menus.get(i).get(j), i);
        setJMenuBar(menuBar);
        centerPanel = new JPanel();
        centerPanel.setLayout(new BorderLayout());
        getWindowPanel().add(centerPanel, DesktopLayout.CENTER);
        tab = new TabbedChannelContainer();
        tab.getTabbedPane().addChangeListener(this);
        centerPanel.add(tab, BorderLayout.CENTER);
        Border border = new EtchedBorder();
        scannerBar = new JScannerBar(Color.white, new Color(116, 122, 140), 10, Orientation.HORIZONTAL, 0, 0);
        GridBagLayout g = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        JPanel p = new JPanel();
        p.setBorder(border);
        p.setMinimumSize(new Dimension(100, 5));
        p.setMaximumSize(new Dimension(100, 30000));
        p.setPreferredSize(new Dimension(100, 10));
        p.setLayout(g);
        c = new GridBagConstraints();
        c.weightx = 1.0f;
        c.weighty = 1.0f;
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        g.setConstraints(scannerBar, c);
        p.add(scannerBar);
        statusLabel = new JLabel();
        statusLabel.setBorder(border);
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN));
        statusLabel.setMaximumSize(new Dimension(30000, 30000));
        timeLabel = new JLabel(DateFormat.getTimeInstance(DateFormat.MEDIUM).format(new Date()));
        timeLabel.setBorder(border);
        timeLabel.setFont(timeLabel.getFont().deriveFont(Font.PLAIN));
        ActionListener timerUpdater = new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                timeLabel.setText(DateFormat.getTimeInstance(DateFormat.MEDIUM).format(new Date()));
            }
        };
        javax.swing.Timer timer = new javax.swing.Timer(1000, timerUpdater);
        timer.start();
        statusBar = new JPanel();
        statusBar.setMinimumSize(new Dimension(5, 20));
        statusBar.setLayout(new BoxLayout(statusBar, BoxLayout.X_AXIS));
        statusBar.add(p);
        statusBar.add(Box.createHorizontalStrut(3));
        statusBar.add(statusLabel);
        statusBar.add(Box.createHorizontalStrut(3));
        statusBar.add(timeLabel);
        getContentPane().add(statusBar, BorderLayout.SOUTH);
    }

    /**
     * Changes the menu bar to reflect language changes.
     */
    private void adaptUI() {
        serverMenu.setText(App.localization.localize("app", "mainframe.servermenu", "Server"));
        channelMenu.setText(App.localization.localize("app", "mainframe.channelmenu", "Channel"));
        extrasMenu.setText(App.localization.localize("app", "mainframe.extrasmenu", "Extras"));
    }

    /**
     * Updates the frame's title.
     * 
     * @param nick {@code String} containing the nick the user has after
     *             logging on or {@code null}.
     */
    private void adaptTitle(String nick) {
        String title = App.APP_SHORT_NAME + " " + App.APP_VERSION;
        if (tab.getTabbedPane().getTabCount() == 0) {
            setTitle(title);
            scannerBar.stop();
            return;
        }
        AbstractIRCSession s = null;
        Component c = tab.getTabbedPane().getSelectedComponent();
        if (c instanceof IRCChatPanel) s = ((IRCChatPanel) c).getChannel().getParentSession(); else if (c instanceof IRCSessionPanel) s = ((IRCSessionPanel) c).getSession(); else {
            resetSession();
            setTitle(title);
            scannerBar.stop();
            return;
        }
        if (s != currentSession) {
            resetSession();
            currentSession = s;
            currentSession.getConnection().addIRCConnectionListener(this);
            if (currentSession.getConnection().getStatus() == SessionStatus.CONNECTING || currentSession.getConnection().getStatus() == SessionStatus.AUTHENTICATING) {
                scannerBar.start();
            } else scannerBar.stop();
        }
        if (currentSession != null) {
        }
        setTitle(title);
    }

    /**
     * Removes us from the listener list of the formerly active session and sets
     * our session reference to {@code null}.
     */
    private void resetSession() {
        if (currentSession != null) currentSession.getConnection().removeIRCConnectionListener(this);
        currentSession = null;
    }

    /**
     * Adds a new menu item to the specified menu.
     * 
     * @param action {@code Action} to initialize the menu item with. If
     *               this is {@code null}, a new seperator will be added to
     *               the specified menu.
     * @param menu {@code int} specifying where to add the menu item.
     */
    public void addMenuItem(Action action, int menu) {
        if (action == null) {
            menuBar.getMenu(menu).addSeparator();
        } else {
            menuBar.getMenu(menu).add(new JMenuItem(action));
        }
    }

    /**
     * Removes a menu item from the specified menu.
     * 
     * @param action {@code Action} to remove. This can't be null as
     *               separators will automatically be removed if no menu item
     *               follows them.
     * @param menu {@code int} specifying where to remove the menu item
     *             from.
     */
    public void removeMenuItem(Action action, int menu) {
        JMenu m = menuBar.getMenu(menu);
        for (int i = 0; i < m.getItemCount(); i++) {
            if (m.getItem(i) != null) if (m.getItem(i).getAction() == action) {
                m.remove(i);
                break;
            }
        }
        boolean separator = false;
        for (int i = 0; i < m.getItemCount(); i++) {
            if (m.getMenuComponent(i) instanceof JSeparator) {
                if (separator) {
                    m.remove(i);
                    i--;
                }
                separator = true;
            } else separator = false;
        }
    }

    /**
     * Removes this {@code MainFrame} from the collection of frames.
     */
    private void closing() {
        tab.getTabbedPane().removeChangeListener(this);
        Component c;
        IRCSessionPanel s;
        for (int i = 0; i < tab.getTabbedPane().getTabCount(); i++) {
            c = tab.getTabbedPane().getComponentAt(i);
            if (c instanceof IRCSessionPanel) {
                s = (IRCSessionPanel) c;
                if (s.getSession().isActive()) s.getSession().disconnect();
            }
        }
        App.gui.removeMainFrame(this);
    }

    /**
     * Returns the {@code TabbedChannelContainer} instance used by this
     * {@code MainFrame}.
     * 
     * @return {@code TabbedChannelContainer} instance used by this
     *         {@code MainFrame}.
     */
    public TabbedChannelContainer getTabbedChannelContainer() {
        return tab;
    }

    public void stateChanged(ChangeEvent e) {
        adaptTitle(null);
        App.gui.tabStateChanged(this);
    }

    public void componentResized(ComponentEvent e) {
        if (getExtendedState() != MAXIMIZED_BOTH) {
            frameHeight = getHeight();
            frameWidth = getWidth();
        }
    }

    public void componentMoved(ComponentEvent e) {
        if (getExtendedState() != MAXIMIZED_BOTH) {
            frameX = getX();
            frameY = getY();
        }
    }

    public void componentShown(ComponentEvent e) {
    }

    public void componentHidden(ComponentEvent e) {
    }

    public void statusChanged(IRCConnection conn, SessionStatus oldStatus) {
        if (!conn.isActive()) adaptTitle(null);
        if (conn.getStatus() == SessionStatus.CONNECTING || conn.getStatus() == SessionStatus.AUTHENTICATING) {
            scannerBar.start();
        } else scannerBar.stop();
    }

    public void messageSendable(IRCMessageEvent e) {
    }

    public void messageSent(IRCMessageEvent e) {
    }

    public void messageReceived(IRCMessageEvent e) {
        IRCMessage msg = e.getMessage();
        if (msg.getType().equals(IRCMessageTypes.MSG_NICK)) {
            adaptTitle(null);
        }
    }

    public void languageChanged() {
        adaptUI();
    }
}
