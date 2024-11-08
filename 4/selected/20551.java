package de.iritgo.aktera.aktario.gui;

import de.iritgo.aktario.client.gui.AktarioGUI;
import de.iritgo.aktario.client.gui.UserLoginHelper;
import de.iritgo.aktario.core.Engine;
import de.iritgo.aktario.core.application.ApplicationGlassPane;
import de.iritgo.aktario.core.application.ApplicationPane;
import de.iritgo.aktario.core.application.WhiteBoardAction;
import de.iritgo.aktario.core.application.WhiteBoardServerAction;
import de.iritgo.aktario.core.base.SystemProperties;
import de.iritgo.aktario.core.command.Command;
import de.iritgo.aktario.core.filebrowser.FileBrowser;
import de.iritgo.aktario.core.gui.GUIPane;
import de.iritgo.aktario.core.gui.IAction;
import de.iritgo.aktario.core.gui.IDesktopLayouter;
import de.iritgo.aktario.core.gui.IDesktopManager;
import de.iritgo.aktario.core.gui.IDesktopPane;
import de.iritgo.aktario.core.gui.IDialog;
import de.iritgo.aktario.core.gui.IDisplay;
import de.iritgo.aktario.core.gui.IMenuBar;
import de.iritgo.aktario.core.gui.IOverlayIcon;
import de.iritgo.aktario.core.gui.IToolBar;
import de.iritgo.aktario.core.gui.SwingDesktopFrame;
import de.iritgo.aktario.core.gui.SwingDesktopManager;
import de.iritgo.aktario.core.gui.SwingDialogFrame;
import de.iritgo.aktario.core.gui.SwingGUIFactory;
import de.iritgo.aktario.core.gui.SwingGUIPane;
import de.iritgo.aktario.core.gui.SwingWindowFrame;
import de.iritgo.aktario.core.logger.Log;
import de.iritgo.aktario.core.network.ClientTransceiver;
import de.iritgo.aktario.framework.IritgoEngine;
import de.iritgo.aktario.framework.action.ActionTools;
import de.iritgo.aktario.framework.appcontext.AppContext;
import de.iritgo.aktario.framework.base.InitIritgoException;
import de.iritgo.aktario.framework.client.Client;
import de.iritgo.aktario.framework.client.command.ShowDialog;
import de.iritgo.aktario.framework.client.gui.ClientGUI;
import de.iritgo.aktario.framework.command.CommandTools;
import de.iritgo.aktario.framework.user.User;
import de.iritgo.aktera.aktario.AkteraAktarioClientManager;
import de.iritgo.simplelife.math.NumberTools;
import de.iritgo.simplelife.string.StringTools;
import org.swixml.SwingEngine;
import org.swixml.SwingTagLibrary;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRootPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import java.awt.AWTEvent;
import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Point;
import java.awt.SystemTray;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 *
 */
@SuppressWarnings("serial")
public class AkteraAktarioGUI extends AktarioGUI implements ClientGUI {

    class MyDesktopLayouter extends IDesktopLayouter {

        private int width;

        private int height;

        @Override
        public void setBounds(int newX, int newY, int newWidth, int newHeight) {
            width = newWidth;
            height = newHeight;
            layout();
        }

        @Override
        public void setBoundsForFrame(JComponent f, int newX, int newY, int newWidth, int newHeight) {
            layout();
        }

        private void layout() {
            for (JInternalFrame frame : desktopPane.getAllFrames()) {
                String id = (((SwingWindowFrame) frame).getWindow().getTypeId());
                if ("CallManagerInstantCallPane".equals(id)) {
                    frame.setBounds(0, height - 40, width, 40);
                } else {
                    frame.setBounds(0, 0, width, height - 40);
                }
            }
        }
    }

    /** The system tray. */
    protected static SystemTray tray;

    /** The try icon. */
    protected static AkteraTrayIcon trayIcon;

    /** The participant indicator icon. */
    protected static IOverlayIcon participantIndicator;

    /** The system tray popup menu. */
    protected static JPopupMenu systemTrayMenu;

    /** Desktop manager. */
    private SwingDesktopManager desktopManager;

    /** Swing desktop frame. */
    private SwingDesktopFrame desktopFrame;

    /** Desktop pane. */
    private JDesktopPane desktopPane;

    /** Status user field. */
    public JTextField statusUser;

    /** Status text. */
    public JTextField statusText;

    /** Current x position of the pointer. */
    protected int pointerX;

    /** Current y position of the pointer. */
    protected int pointerY;

    /** Init the GUI-Factory **/
    private SwingGUIFactory swingGUIFactory = new SwingGUIFactory();

    /** The current color scheme. */
    protected String colorScheme;

    /** The menu containing administration functions. */
    public JMenu adminMenu;

    /** The menu containing application functions. */
    public JMenu applicationMenu;

    /** Our tool bar. */
    protected IToolBar toolbar;

    /** Login dialog background. */
    protected ImageIcon loginBackground;

    /** About dialog background. */
    protected ImageIcon aboutBackground;

    /** Our tool panel. */
    protected JPanel toolPanel;

    /** Application icons. */
    protected Map icons;

    /** If this flag is set, a manual disconnect was performed. */
    protected boolean manualDisconnect;

    /** Modules can add options to this menu */
    public JMenu extrasMenu;

    /**
	 * Terminate the client.
	 */
    public Action quitAction = new AbstractAction() {

        public void actionPerformed(ActionEvent e) {
            IritgoEngine.instance().shutdown();
        }
    };

    /**
	 * Display the preferences dialog.
	 */
    public Action preferencesAction = new AbstractAction() {

        public void actionPerformed(ActionEvent e) {
            try {
                Properties props = new Properties();
                props.put("category", ((Component) e.getSource()).getName());
                CommandTools.performAsync(new ShowDialog("PreferencesGUIPane", "PreferencesGUIPane", AppContext.instance().getUser().getUniqueId(), "AktarioUserPreferences"), props);
            } catch (Exception x) {
                Log.logError("client", "AktarioGUI.preferencesAction", "Unable to find preferences object");
            }
        }
    };

    /**
	 * Display the user administration dialog.
	 */
    public Action manageUsers = new AbstractAction() {

        public void actionPerformed(ActionEvent e) {
            try {
                CommandTools.performAsync(new ShowDialog("UserListGUIPane", "UserListGUIPane", 11000, "AkteraUserRegistry"));
            } catch (Exception x) {
            }
        }
    };

    /**
	 * Display the about dialog.
	 */
    public Action aboutAction = new AbstractAction() {

        public void actionPerformed(ActionEvent e) {
            CommandTools.performAsync(new ShowDialog("AboutGUIPane"));
        }
    };

    /**
	 * Display the system info dialog.
	 */
    public Action systemAction = new AbstractAction() {

        public void actionPerformed(ActionEvent e) {
            CommandTools.performAsync("ShowSystemMonitor");
        }
    };

    /**
	 */
    public Action phoneAction = new AbstractAction() {

        public void actionPerformed(ActionEvent e) {
            getDesktopManager().getDisplay("SipPhonePane").bringToFront();
        }
    };

    /**
	 */
    public Action disconnectAction = new AbstractAction() {

        public void actionPerformed(ActionEvent e) {
            manualDisconnect = true;
            Client.instance().getNetworkService().closeAllChannels();
        }
    };

    /**
	 * Create the main aktario gui.
	 */
    public AkteraAktarioGUI() {
        icons = new HashMap();
        icons.put("loginBackground", new ImageIcon(getClass().getResource("/resources/app-login.png")));
        icons.put("aboutBackground", new ImageIcon(getClass().getResource("/resources/app-splash.png")));
        icons.put("icon16", new ImageIcon(getClass().getResource("/resources/app-icon-16.png")));
        icons.put("icon24", new ImageIcon(getClass().getResource("/resources/app-logo-24.png")));
    }

    /**
	 * Initialize the main gui.
	 */
    public void init() throws InitIritgoException {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {

                public void run() {
                    try {
                        initGui();
                    } catch (Exception x) {
                        Log.logError("client", "AkteraAktarioGUI.initDesktop", x.toString());
                        x.printStackTrace();
                    }
                }
            });
        } catch (InterruptedException x) {
        } catch (InvocationTargetException x) {
        }
    }

    /**
	 * Show the client gui.
	 */
    public void show() {
        Properties props = Engine.instance().getSystemProperties();
        int sizeX = NumberTools.toInt(props.getProperty("iritgoConnectSizeX"), 320);
        int sizeY = NumberTools.toInt(props.getProperty("iritgoConnectSizeY"), 480);
        int posX = NumberTools.toInt(props.getProperty("iritgoConnectPosX"), 48);
        int posY = NumberTools.toInt(props.getProperty("iritgoConnectPosY"), 48);
        desktopFrame.setSize(sizeX, sizeY);
        desktopFrame.setLocation(posX, posY);
        if (NumberTools.toBool(props.getProperty("startMinimized"), false)) {
            desktopFrame.setExtendedState(JFrame.ICONIFIED);
        }
        desktopFrame.setVisible();
        if (NumberTools.toBool(props.getProperty("startMinimized"), false)) {
            desktopFrame.setVisible(false);
            desktopFrame.setExtendedState(JFrame.NORMAL);
        }
        if (checkSystemOSOfWindowsOrLinux()) {
            trayIcon = new AkteraTrayIcon(getIcon("icon24").getImage(), getAppTitle(), systemTrayMenu);
            if (trayIcon != null) {
                trayIcon.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        if (desktopFrame.isVisible() && !desktopFrame.isActive()) {
                            desktopFrame.setVisible(false);
                            desktopFrame.setExtendedState(JFrame.NORMAL);
                            desktopFrame.setVisible(true);
                            desktopFrame.toFront();
                        } else if ((desktopFrame.getExtendedState() & JFrame.ICONIFIED) != 0) {
                            desktopFrame.setVisible(false);
                            desktopFrame.setExtendedState(JFrame.NORMAL);
                            desktopFrame.setVisible(true);
                            desktopFrame.toFront();
                        } else {
                            desktopFrame.setVisible(!desktopFrame.isVisible());
                            if (desktopFrame.isVisible()) {
                                desktopFrame.toFront();
                            }
                        }
                    }
                });
                if (tray != null) {
                    try {
                        tray.add(trayIcon);
                    } catch (AWTException x) {
                        System.out.println(x);
                    }
                }
            }
        }
    }

    /**
	 * Retrieve the desktop manager.
	 *
	 * @return The desktop manager.
	 */
    public IDesktopManager getDesktopManager() {
        return desktopManager;
    }

    /**
	 * Start the client gui.
	 */
    public void startGUI() {
        final SystemProperties sysProperties = Engine.instance().getSystemProperties();
        if (sysProperties.getBool("autoLogin", false)) {
            CommandTools.performAsync(new Command() {

                public void perform() {
                    UserLoginHelper.login(null, sysProperties.getString("autoLoginServer", null), sysProperties.getString("autoLoginUser", null), StringTools.decode(sysProperties.getString("autoLoginPassword", null)), false, false);
                }
            });
        } else {
            CommandTools.performAsync(new ShowDialog("AktarioUserLoginDialog"));
        }
    }

    /**
	 * Stop the client gui.
	 */
    public void stopGUI() {
        Properties props = Engine.instance().getSystemProperties();
        props.setProperty("iritgoConnectSizeX", "" + (int) desktopFrame.getBounds().getWidth());
        props.setProperty("iritgoConnectSizeY", "" + (int) desktopFrame.getBounds().getHeight());
        props.setProperty("iritgoConnectPosX", "" + (int) desktopFrame.getBounds().getX());
        props.setProperty("iritgoConnectPosY", "" + (int) desktopFrame.getBounds().getY());
        if (tray != null && trayIcon != null) {
            tray.remove(trayIcon);
        }
        desktopFrame.close();
    }

    /**
	 * Start the client application.
	 */
    public void startApplication() {
    }

    /**
	 * Stop the client application.
	 */
    public void stopApplication() {
    }

    /**
	 * Set the user status text.
	 *
	 * @param userName The user name text to set.
	 */
    public void setStatusUser(String userName) {
        statusUser.setText(userName);
    }

    /**
	 * Set the status text.
	 *
	 * @param text The status text to set.
	 */
    public void setStatusText(String text) {
        statusText.setText(text);
    }

    /**
	 * Retrieve the desktop pane.
	 *
	 * @return The desktop pane.
	 */
    public JDesktopPane getDesktopPane() {
        return desktopPane;
    }

    /**
	 * If a mouse or key event occurred in an enabled application pane,
	 * this method is called.
	 *
	 * @param e The event.
	 */
    public void processApplicationPaneEvent(AWTEvent e) {
        WhiteBoardServerAction action = new WhiteBoardServerAction(AppContext.instance().getUser());
        GUIPane guiPane = (GUIPane) AppContext.instance().getObject("applicationPane");
        if (!(guiPane instanceof ApplicationPane)) {
            return;
        }
        ApplicationPane appPane = (ApplicationPane) guiPane;
        switch(e.getID()) {
            case MouseEvent.MOUSE_MOVED:
                {
                    MouseEvent event = (MouseEvent) e;
                    Point glassPanePos = ((SwingGUIPane) appPane).getPanel().getLocationOnScreen();
                    Point compPos = ((JComponent) e.getSource()).getLocationOnScreen();
                    pointerX = (int) (compPos.getX() - glassPanePos.getX() + event.getX());
                    pointerY = (int) (compPos.getY() - glassPanePos.getY() + event.getY());
                    action.sendMouseMove(pointerX, pointerY);
                    break;
                }
            case KeyEvent.KEY_RELEASED:
                {
                    KeyEvent event = (KeyEvent) e;
                    switch(event.getKeyCode()) {
                        case KeyEvent.VK_F1:
                            appPane.contextHelp();
                            break;
                        case KeyEvent.VK_F5:
                            action.sendPaint(pointerX, pointerY, WhiteBoardAction.PAINT_EXCLAMATION);
                            break;
                        case KeyEvent.VK_F6:
                            action.sendPaint(pointerX, pointerY, WhiteBoardAction.PAINT_INFO);
                            break;
                        case KeyEvent.VK_F7:
                            action.sendPaint(pointerX, pointerY, WhiteBoardAction.PAINT_OK);
                            break;
                        case KeyEvent.VK_F8:
                            action.sendPaint(pointerX, pointerY, WhiteBoardAction.PAINT_QUESTION);
                            break;
                        case KeyEvent.VK_F9:
                            action.sendPaint(pointerX, pointerY, WhiteBoardAction.PAINT_ERASE);
                            break;
                    }
                }
        }
        ClientTransceiver transceiver = new ClientTransceiver(AppContext.instance().getChannelNumber());
        transceiver.addReceiver(AppContext.instance().getChannelNumber());
        action.setTransceiver(transceiver);
        ActionTools.sendToServer(action);
    }

    /**
	 * Get the name of the current color scheme.
	 *
	 * @retrurn The color scheme name.
	 */
    public String getColorScheme() {
        return colorScheme;
    }

    /**
	 * Change the color scheme.
	 *
	 * @param colorScheme The new color scheme.
	 */
    public void setColorScheme(String colorScheme) {
        try {
            this.colorScheme = colorScheme;
            com.jgoodies.looks.plastic.PlasticXPLookAndFeelIritgo.setCurrentTheme((com.jgoodies.looks.plastic.PlasticTheme) Class.forName(colorScheme).newInstance());
            com.jgoodies.looks.Options.setPopupDropShadowEnabled(true);
            UIManager.put("jgoodies.popupDropShadowEnabled", Boolean.TRUE);
            LookAndFeel lnf = (LookAndFeel) getClass().getClassLoader().loadClass("com.jgoodies.looks.plastic.PlasticXPLookAndFeelIritgo").newInstance();
            UIManager.setLookAndFeel(lnf);
            UIManager.getLookAndFeelDefaults().put("ClassLoader", getClass().getClassLoader());
            UIManager.put("TaskPane.useGradient", Boolean.TRUE);
            UIManager.put("TaskPane.backgroundGradientStart", UIManager.getColor("Panel.background").brighter());
            UIManager.put("TaskPane.backgroundGradientEnd", UIManager.getColor("Panel.background").darker());
            UIManager.put("TaskPaneGroup.titleOver", UIManager.getColor("Label.foreground"));
            UIManager.put("TaskPaneGroup.specialTitleOver", UIManager.getColor("Label.foreground"));
            if (desktopFrame != null) {
                SwingUtilities.updateComponentTreeUI(desktopFrame);
                for (Iterator i = desktopManager.getDisplayIterator(); i.hasNext(); ) {
                    IDisplay display = (IDisplay) i.next();
                    if (display instanceof IDialog) {
                        SwingUtilities.updateComponentTreeUI(((SwingDialogFrame) ((IDialog) display).getDialogFrame()));
                    }
                }
            }
        } catch (Exception x) {
            final Exception error = x;
            new Thread(new Runnable() {

                public void run() {
                    JOptionPane.showMessageDialog(desktopFrame.getJFrame(), error.toString(), getAppTitle(), JOptionPane.OK_OPTION);
                }
            }).start();
            x.printStackTrace();
            Log.logError("client", "AktarioGUI.setColorScheme", x.toString());
        }
    }

    /**
	 * Reload the menu bar.
	 * This method is called after a change to the language to reload
	 * the menu labels.
	 */
    public void reloadMenuBar() {
        JFrame frame = desktopFrame.getJFrame();
        ((IMenuBar) frame.getJMenuBar()).reloadText();
        try {
            SwingEngine swingEngine = new SwingEngine(this);
            if (checkSystemOSOfWindowsOrLinux()) {
                systemTrayMenu = (JPopupMenu) swingEngine.render(getClass().getResource("/swixml/TrayMenu.xml"));
                if (trayIcon != null) {
                }
            }
        } catch (Exception x) {
        }
    }

    /**
	 * Reload the tool bar.
	 * This method is called after a change to the language to reload
	 * the menu labels.
	 */
    public void reloadToolBar() {
        toolbar.reloadText();
    }

    /**
	 * Show/hide the administration menu.
	 *
	 * @param visible If true the admin menu is visible.
	 */
    public void setAdminMenuVisible(boolean visible) {
    }

    /**
	 * Enable/disable the application menu.
	 *
	 * @param enabled If true the application menu is enabled.
	 */
    public void setApplicationMenuEnabled(boolean enabled) {
    }

    /**
	 * Called when the server connection was lost.
	 * This method shuts down the client and redisplays the login dialog.
	 */
    public void lostNetworkConnection() {
        try {
            Thread.sleep(4000);
        } catch (InterruptedException x) {
        }
        User user = AppContext.instance().getUser();
        if (user.isOnline()) {
            manualDisconnect = false;
            getDesktopManager().closeAllDisplays();
            desktopFrame.setVisible(false);
            if (tray != null) {
                tray.remove(trayIcon);
            }
            String password = AppContext.instance().getUserPassword();
            UserLoginHelper.login(null, AppContext.instance().getServerIP(), user.getName(), password, false, false);
        }
    }

    /**
	 * Remove all desktops.
	 */
    public void removeAllDesktops() {
        desktopManager.removeAllDesktopPanes();
    }

    /**
	 * Get the login dialog background image.
	 *
	 * @return The background image.
	 */
    public ImageIcon getLoginBackground() {
        return (ImageIcon) icons.get("loginBackground");
    }

    /**
	 * Get the about dialog background image.
	 *
	 * @return The background image.
	 */
    public ImageIcon getAboutBackground() {
        return (ImageIcon) icons.get("aboutBackground");
    }

    /**
	 * Get the participant indicator icon.
	 *
	 * @return The participant indicator icon.
	 */
    public IOverlayIcon getParticipantIndicator() {
        return participantIndicator;
    }

    /**
	 * Get the system tray menu.
	 *
	 * @return The system tray popup menu.
	 */
    public JPopupMenu getSystemTrayMenu() {
        return systemTrayMenu;
    }

    /**
	 * Update the system tray.
	 */
    public void updateSystemTray() {
        if (trayIcon != null) {
            trayIcon.setImage(getIcon("icon24").getImage());
        }
    }

    public JPanel getToolPanel() {
        return toolPanel;
    }

    /**
	 * Helper method for creating gridbag constraints.
	 *
	 * @param x The grid column.
	 * @param y The grid row.
	 * @param width The number of occupied columns.
	 * @param height The number of occupied rows.
	 * @param fill The fill method.
	 * @param wx The horizontal stretch factor.
	 * @param wy The vertical stretch factor.
	 * @param insets The cell insets.
	 * @return The gridbag constraints.
	 */
    protected GridBagConstraints createConstraints(int x, int y, int width, int height, int fill, int wx, int wy, Insets insets) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = width;
        gbc.gridheight = height;
        gbc.fill = fill;
        gbc.weightx = wx;
        gbc.weighty = wy;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        if (insets == null) {
            gbc.insets = new Insets(0, 0, 0, 0);
        } else {
            gbc.insets = insets;
        }
        return gbc;
    }

    /**
	 * Get the position of the systray icon.
	 *
	 * @return The systray icon position.
	 */
    public Point getSystemTrayIconPosition() {
        if (trayIcon != null) {
            return new Point(0, 0);
        }
        return null;
    }

    /**
	 * Get the size of the systray icon.
	 *
	 * @return The systray icon size.
	 */
    public Dimension getSystemTrayIconSize() {
        if (trayIcon != null) {
            return new Dimension(participantIndicator.getIconWidth(), participantIndicator.getIconHeight());
        }
        return null;
    }

    /**
	 * Get the application title.
	 *
	 * @return The application title.
	 */
    public String getAppTitle() {
        return Engine.instance().getResourceService().getString("app.title");
    }

    /**
	 * Set an application icon.
	 *
	 * @param key The icon key.
	 * @param icon The icon to set.
	 */
    public void setIcon(String key, ImageIcon icon) {
        icons.put(key, icon);
    }

    /**
	 * Get an application icon.
	 *
	 * @param key The icon key.
	 * @return The icon.
	 */
    public ImageIcon getIcon(String key) {
        return (ImageIcon) icons.get(key);
    }

    private boolean checkSystemOSOfWindowsOrLinux() {
        return (System.getProperty("os.name").indexOf("Windows") != -1) || (System.getProperty("os.name").indexOf("Linux") != -1);
    }

    /**
	 * Make the client gui the foreground window.
	 */
    public void bringToFront() {
        if (desktopFrame.isVisible() && !desktopFrame.isActive()) {
            desktopFrame.setVisible(false);
            desktopFrame.setExtendedState(JFrame.NORMAL);
            desktopFrame.setVisible(true);
            desktopFrame.toFront();
        } else if ((desktopFrame.getExtendedState() & JFrame.ICONIFIED) != 0) {
            desktopFrame.setVisible(false);
            desktopFrame.setExtendedState(JFrame.NORMAL);
            desktopFrame.setVisible(true);
            desktopFrame.toFront();
        } else {
            desktopFrame.setVisible(true);
            desktopFrame.toFront();
        }
    }

    /**
	 * Get the main application window.
	 *
	 * @return The application window.
	 */
    public Window getMainWindow() {
        return desktopFrame;
    }

    private void initGui() throws Exception {
        Engine.instance().setGUIFactory(swingGUIFactory);
        SwingEngine swingEngine = new SwingEngine(this);
        if (checkSystemOSOfWindowsOrLinux()) {
            if (tray == null) {
                try {
                    tray = SystemTray.getSystemTray();
                    participantIndicator = new IOverlayIcon(getIcon("icon24"));
                    participantIndicator.addOverlay("message", 0, 12);
                    participantIndicator.addIcon("message", "new", new ImageIcon(getClass().getResource("/resources/emblem-message-12.png")));
                    systemTrayMenu = (JPopupMenu) swingEngine.render(getClass().getResource("/swixml/TrayMenu.xml"));
                } catch (Exception ignored) {
                }
            }
        }
        SwingTagLibrary.getInstance().registerTag("filebrowser", FileBrowser.class);
        swingEngine.setClassLoader(AkteraAktarioGUI.class.getClassLoader());
        desktopFrame = new SwingDesktopFrame();
        desktopFrame.setTitle(getAppTitle());
        desktopFrame.setIconImage(getIcon("icon16").getImage());
        desktopFrame.init();
        desktopFrame.addCloseListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (tray == null) {
                    quitAction.actionPerformed(e);
                } else {
                    desktopFrame.setVisible(false);
                }
            }
        });
        desktopFrame.addWindowListener(new WindowAdapter() {

            @Override
            public void windowIconified(WindowEvent e) {
                desktopFrame.setExtendedState(JFrame.NORMAL);
                desktopFrame.setVisible(true);
                desktopFrame.setVisible(false);
            }
        });
        desktopManager = new SwingDesktopManager();
        desktopManager.setDesktopFrame(desktopFrame);
        JFrame jframe = desktopFrame.getJFrame();
        jframe.getContentPane().setLayout(new BorderLayout());
        IMenuBar menubar = (IMenuBar) swingEngine.render(getClass().getResource("/swixml/MenuBar.xml"));
        jframe.setJMenuBar(menubar);
        toolbar = (IToolBar) swingEngine.render(getClass().getResource("/swixml/ToolBar.xml"));
        desktopPane = new IDesktopPane();
        desktopPane.setDesktopManager(new MyDesktopLayouter());
        desktopManager.addDesktopPane(desktopPane);
        jframe.getContentPane().add(desktopPane, BorderLayout.CENTER);
        toolPanel = new JPanel();
        toolPanel.setLayout(new FlowLayout());
        addToolBarItems();
        toolPanel.revalidate();
        jframe.getContentPane().add(toolPanel, BorderLayout.NORTH);
        JPanel statusBar = (JPanel) swingEngine.render(getClass().getResource("/swixml/StatusBar.xml"));
        jframe.getContentPane().add(statusBar, BorderLayout.SOUTH);
        jframe.getToolkit().addAWTEventListener(new AWTEventListener() {

            public void eventDispatched(AWTEvent e) {
                if (e.getSource() instanceof JComponent) {
                    JRootPane root = ((JComponent) e.getSource()).getRootPane();
                    if (root != null && root.getGlassPane() instanceof ApplicationGlassPane) {
                        if (((ApplicationGlassPane) root.getGlassPane()).isEnabled()) {
                            processApplicationPaneEvent(e);
                        }
                    }
                }
            }
        }, AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.KEY_EVENT_MASK);
        addExtrasMenuItems();
        addSystemTrayMenuItems();
        AppContext.instance().put("aktarioGui", this);
        setColorScheme("com.jgoodies.looks.plastic.theme.KDE");
    }

    /**
	 * Describe method addToolMenuItems() here.
	 *
	 */
    private void addExtrasMenuItems() {
        AkteraAktarioClientManager aacm = (AkteraAktarioClientManager) Engine.instance().getManager(AkteraAktarioClientManager.ID);
        for (IAction item : aacm.getExtrasMenuItems()) {
            extrasMenu.add(item);
        }
    }

    /**
	 * Describe method addSystemTrayMenuItems() here.
	 *
	 */
    private void addSystemTrayMenuItems() {
        AkteraAktarioClientManager aacm = (AkteraAktarioClientManager) Engine.instance().getManager(AkteraAktarioClientManager.ID);
        if (aacm.getSystemTrayMenuItems().size() > 0) {
            systemTrayMenu.add(new JSeparator(JSeparator.HORIZONTAL), 0);
            for (JMenuItem item : aacm.getSystemTrayMenuItems()) {
                systemTrayMenu.add(item, 0);
            }
        }
    }

    /**
	 * Describe method addToolBarItems() here.
	 *
	 */
    private void addToolBarItems() {
        AkteraAktarioClientManager aacm = (AkteraAktarioClientManager) Engine.instance().getManager(AkteraAktarioClientManager.ID);
        for (IAction action : aacm.getToolBarItems()) {
            JButton button = new JButton(action);
            button.setBorderPainted(false);
            button.setBorder(new EmptyBorder(new Insets(0, 4, 0, 4)));
            toolPanel.add(button);
        }
    }
}
