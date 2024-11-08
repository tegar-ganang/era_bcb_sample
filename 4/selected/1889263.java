package org.rdv.ui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.filechooser.FileFilter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdesktop.application.Application;
import org.jdesktop.application.ApplicationContext;
import org.jdesktop.application.ResourceMap;
import org.rdv.ConfigurationManager;
import org.rdv.DataPanelManager;
import org.rdv.DataViewer;
import org.rdv.Extension;
import org.rdv.RDV;
import org.rdv.action.ActionFactory;
import org.rdv.action.DataViewerAction;
import org.rdv.auth.AuthenticationManager;
import org.rdv.rbnb.Channel;
import org.rdv.rbnb.ConnectionListener;
import org.rdv.rbnb.LocalServer;
import org.rdv.rbnb.MessageListener;
import org.rdv.rbnb.Player;
import org.rdv.rbnb.RBNBController;
import org.rdv.rbnb.RBNBHelper;
import org.rdv.rbnb.StateListener;
import org.rdv.rbnb.TimeRange;
import com.jgoodies.uif_lite.component.Factory;

/**
 * Main frame for the application
 * 
 * @author  Jason P. Hanley
 * @author  Lawrence J. Miller
 * @since   1.2
 */
public class ApplicationFrame extends JPanel implements MessageListener, ConnectionListener, StateListener {

    /** serialization version identifier */
    private static final long serialVersionUID = -4692978463068122918L;

    static Log log = org.rdv.LogFactory.getLog(ApplicationFrame.class.getName());

    private RBNBController rbnb;

    private DataPanelManager dataPanelManager;

    private BusyDialog busyDialog;

    private LoadingDialog loadingDialog;

    private LoginDialog loginDialog;

    private JFrame frame;

    private GridBagConstraints c;

    private JMenuBar menuBar;

    private ChannelListPanel channelListPanel;

    private MetadataPanel metadataPanel;

    private JSplitPane leftPanel;

    private JPanel rightPanel;

    private ControlPanel controlPanel;

    private AudioPlayerPanel audioPlayerPanel;

    private MarkerSubmitPanel markerSubmitPanel;

    private DataPanelContainer dataPanelContainer;

    private JSplitPane splitPane;

    private AboutDialog aboutDialog;

    private RBNBConnectionDialog rbnbConnectionDialog;

    private Action fileAction;

    private Action connectAction;

    private Action disconnectAction;

    private Action loginAction;

    private Action logoutAction;

    private Action loadAction;

    private Action saveAction;

    private Action importAction;

    private Action exportAction;

    private Action exitAction;

    private Action exportVideoAction;

    private Action controlAction;

    private DataViewerAction realTimeAction;

    private DataViewerAction playAction;

    private DataViewerAction pauseAction;

    private Action beginningAction;

    private Action endAction;

    private Action gotoTimeAction;

    private Action updateChannelListAction;

    private Action dropDataAction;

    private Action viewAction;

    private Action showChannelListAction;

    private Action showMetadataPanelAction;

    private Action showControlPanelAction;

    private Action showAudioPlayerPanelAction;

    private Action showMarkerPanelAction;

    private Action dataPanelAction;

    private Action dataPanelHorizontalLayoutAction;

    private Action dataPanelVerticalLayoutAction;

    private Action showHiddenChannelsAction;

    private Action hideEmptyTimeAction;

    private Action fullScreenAction;

    private Action windowAction;

    private Action closeAllDataPanelsAction;

    private Action helpAction;

    private Action usersGuideAction;

    private Action supportAction;

    private Action releaseNotesAction;

    private Action aboutAction;

    private JLabel throbber;

    private Icon throbberStop;

    private Icon throbberAnim;

    private final Object loadingMonitor = new Object();

    /** the key mask used for menus shortucts */
    private final int menuShortcutKeyMask;

    public ApplicationFrame() {
        super();
        this.rbnb = RBNBController.getInstance();
        this.dataPanelManager = DataPanelManager.getInstance();
        busyDialog = null;
        loadingDialog = null;
        menuShortcutKeyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
        initFrame();
    }

    private void initFrame() {
        frame = Application.getInstance(RDV.class).getMainFrame();
        setLayout(new BorderLayout());
        c = new GridBagConstraints();
        initActions();
        initMenuBar();
        initChannelListPanel();
        initMetadataPanel();
        initLeftPanel();
        initRightPanel();
        initControls();
        initDataPanelContainer();
        initAudioPlayerPanel();
        initMarkerSubmitPanel();
        initSplitPane();
        channelListPanel.addChannelSelectionListener(metadataPanel);
        rbnb.addSubscriptionListener(controlPanel);
        rbnb.addTimeListener(controlPanel);
        rbnb.addStateListener(channelListPanel);
        rbnb.addStateListener(controlPanel);
        rbnb.addStateListener(this);
        rbnb.getMetadataManager().addMetadataListener(channelListPanel);
        rbnb.getMetadataManager().addMetadataListener(metadataPanel);
        rbnb.getMetadataManager().addMetadataListener(controlPanel);
        rbnb.addPlaybackRateListener(controlPanel);
        rbnb.addTimeScaleListener(controlPanel);
        rbnb.addMessageListener(this);
        rbnb.addConnectionListener(this);
    }

    private void initActions() {
        fileAction = new DataViewerAction("File", "File Menu", KeyEvent.VK_F);
        connectAction = new DataViewerAction("Connect", "Connect to RBNB server", KeyEvent.VK_C, KeyStroke.getKeyStroke(KeyEvent.VK_C, menuShortcutKeyMask | ActionEvent.SHIFT_MASK)) {

            /** serialization version identifier */
            private static final long serialVersionUID = 5038790506859429244L;

            public void actionPerformed(ActionEvent ae) {
                if (rbnbConnectionDialog == null) {
                    rbnbConnectionDialog = new RBNBConnectionDialog(frame, rbnb, dataPanelManager);
                } else {
                    rbnbConnectionDialog.setVisible(true);
                }
            }
        };
        disconnectAction = new DataViewerAction("Disconnect", "Disconnect from RBNB server", KeyEvent.VK_D, KeyStroke.getKeyStroke(KeyEvent.VK_D, menuShortcutKeyMask | ActionEvent.SHIFT_MASK)) {

            /** serialization version identifier */
            private static final long serialVersionUID = -1871076535376405181L;

            public void actionPerformed(ActionEvent ae) {
                dataPanelManager.closeAllDataPanels();
                rbnb.disconnect();
            }
        };
        loginAction = new DataViewerAction("Login", "Login as a NEES user") {

            /** serialization version identifier */
            private static final long serialVersionUID = 6105503896620555072L;

            public void actionPerformed(ActionEvent ae) {
                if (loginDialog == null) {
                    loginDialog = new LoginDialog(frame);
                } else {
                    loginDialog.setVisible(true);
                }
            }
        };
        logoutAction = new DataViewerAction("Logout", "Logout as a NEES user") {

            /** serialization version identifier */
            private static final long serialVersionUID = -2517567766044673777L;

            public void actionPerformed(ActionEvent ae) {
                AuthenticationManager.getInstance().setAuthentication(null);
            }
        };
        loadAction = new DataViewerAction("Load Setup", "Load data viewer setup from file") {

            /** serialization version identifier */
            private static final long serialVersionUID = 7197815395398039821L;

            public void actionPerformed(ActionEvent ae) {
                JFileChooser chooser = new JFileChooser();
                chooser.setFileFilter(new RDVFileFilter());
                chooser.setApproveButtonText("Load");
                chooser.setApproveButtonToolTipText("Load selected file");
                int returnVal = chooser.showOpenDialog(frame);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File configFile = chooser.getSelectedFile();
                    try {
                        URL configURL = configFile.toURI().toURL();
                        ConfigurationManager.loadConfiguration(configURL);
                    } catch (MalformedURLException e) {
                        DataViewer.alertError("\"" + configFile + "\" is not a valid configuration file URL.");
                    }
                }
            }
        };
        saveAction = new DataViewerAction("Save Setup", "Save data viewer setup to file") {

            /** serialization version identifier */
            private static final long serialVersionUID = -8259994975940624038L;

            public void actionPerformed(ActionEvent ae) {
                JFileChooser chooser = new JFileChooser();
                chooser.setFileFilter(new RDVFileFilter());
                int returnVal = chooser.showSaveDialog(frame);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = chooser.getSelectedFile();
                    if (file.getName().indexOf(".") == -1) {
                        file = new File(file.getAbsolutePath() + ".rdv");
                    }
                    if (file.exists()) {
                        int overwriteReturn = JOptionPane.showConfirmDialog(null, file.getName() + " already exists. Do you want to overwrite it?", "Overwrite file?", JOptionPane.YES_NO_OPTION);
                        if (overwriteReturn == JOptionPane.NO_OPTION) {
                            return;
                        }
                    }
                    ConfigurationManager.saveConfiguration(file);
                }
            }
        };
        importAction = new DataViewerAction("Import", "Import Menu", KeyEvent.VK_I, "icons/import.gif");
        exportAction = new DataViewerAction("Export", "Export Menu", KeyEvent.VK_E, "icons/export.gif");
        exportVideoAction = new DataViewerAction("Export video channels", "Export video on the server to the local computer") {

            /** serialization version identifier */
            private static final long serialVersionUID = -6420430928972633313L;

            public void actionPerformed(ActionEvent ae) {
                showExportVideoDialog();
            }
        };
        exitAction = new DataViewerAction("Exit", "Exit RDV", KeyEvent.VK_X) {

            /** serialization version identifier */
            private static final long serialVersionUID = 3137490972014710133L;

            public void actionPerformed(ActionEvent ae) {
                Application.getInstance().exit(ae);
            }
        };
        controlAction = new DataViewerAction("Control", "Control Menu", KeyEvent.VK_C);
        realTimeAction = new DataViewerAction("Real Time", "View data in real time", KeyEvent.VK_R, KeyStroke.getKeyStroke(KeyEvent.VK_R, menuShortcutKeyMask), "icons/rt.gif") {

            /** serialization version identifier */
            private static final long serialVersionUID = -7564783609370910512L;

            public void actionPerformed(ActionEvent ae) {
                rbnb.monitor();
            }
        };
        playAction = new DataViewerAction("Play", "Playback data", KeyEvent.VK_P, KeyStroke.getKeyStroke(KeyEvent.VK_P, menuShortcutKeyMask), "icons/play.gif") {

            /** serialization version identifier */
            private static final long serialVersionUID = 5974457444931142938L;

            public void actionPerformed(ActionEvent ae) {
                rbnb.play();
            }
        };
        pauseAction = new DataViewerAction("Pause", "Pause data display", KeyEvent.VK_A, KeyStroke.getKeyStroke(KeyEvent.VK_S, menuShortcutKeyMask), "icons/pause.gif") {

            /** serialization version identifier */
            private static final long serialVersionUID = -5297742186923194460L;

            public void actionPerformed(ActionEvent ae) {
                rbnb.pause();
            }
        };
        beginningAction = new DataViewerAction("Go to beginning", "Move the location to the start of the data", KeyEvent.VK_B, KeyStroke.getKeyStroke(KeyEvent.VK_B, menuShortcutKeyMask), "icons/begin.gif") {

            /** serialization version identifier */
            private static final long serialVersionUID = 9171304956895497898L;

            public void actionPerformed(ActionEvent ae) {
                controlPanel.setLocationBegin();
            }
        };
        endAction = new DataViewerAction("Go to end", "Move the location to the end of the data", KeyEvent.VK_E, KeyStroke.getKeyStroke(KeyEvent.VK_E, menuShortcutKeyMask), "icons/end.gif") {

            /** serialization version identifier */
            private static final long serialVersionUID = 1798579248452726211L;

            public void actionPerformed(ActionEvent ae) {
                controlPanel.setLocationEnd();
            }
        };
        gotoTimeAction = new DataViewerAction("Go to time", "Move the location to specific date time of the data", KeyEvent.VK_T, KeyStroke.getKeyStroke(KeyEvent.VK_T, menuShortcutKeyMask)) {

            /** serialization version identifier */
            private static final long serialVersionUID = -6411442297488926326L;

            public void actionPerformed(ActionEvent ae) {
                TimeRange timeRange = RBNBHelper.getChannelsTimeRange();
                double time = DateTimeDialog.showDialog(frame, rbnb.getLocation(), timeRange.start, timeRange.end);
                if (time >= 0) {
                    rbnb.setLocation(time);
                }
            }
        };
        updateChannelListAction = new DataViewerAction("Update Channel List", "Update the channel list", KeyEvent.VK_U, KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "icons/refresh.gif") {

            /** serialization version identifier */
            private static final long serialVersionUID = -170096772973697277L;

            public void actionPerformed(ActionEvent ae) {
                rbnb.updateMetadata();
            }
        };
        dropDataAction = new DataViewerAction("Drop Data", "Drop data if plaback can't keep up with data rate", KeyEvent.VK_D, "icons/drop_data.gif") {

            /** serialization version identifier */
            private static final long serialVersionUID = 7079791364881120134L;

            public void actionPerformed(ActionEvent ae) {
                JCheckBoxMenuItem menuItem = (JCheckBoxMenuItem) ae.getSource();
                rbnb.dropData(menuItem.isSelected());
            }
        };
        viewAction = new DataViewerAction("View", "View Menu", KeyEvent.VK_V);
        showChannelListAction = new DataViewerAction("Show Channels", "", KeyEvent.VK_L, "icons/channels.gif") {

            /** serialization version identifier */
            private static final long serialVersionUID = 4982129759386009112L;

            public void actionPerformed(ActionEvent ae) {
                JCheckBoxMenuItem menuItem = (JCheckBoxMenuItem) ae.getSource();
                channelListPanel.setVisible(menuItem.isSelected());
                layoutSplitPane();
                leftPanel.resetToPreferredSizes();
            }
        };
        showMetadataPanelAction = new DataViewerAction("Show Properties", "", KeyEvent.VK_P, "icons/properties.gif") {

            /** serialization version identifier */
            private static final long serialVersionUID = 430106771704397810L;

            public void actionPerformed(ActionEvent ae) {
                JCheckBoxMenuItem menuItem = (JCheckBoxMenuItem) ae.getSource();
                metadataPanel.setVisible(menuItem.isSelected());
                layoutSplitPane();
                leftPanel.resetToPreferredSizes();
            }
        };
        showControlPanelAction = new DataViewerAction("Show Control Panel", "", KeyEvent.VK_C, "icons/control.gif") {

            /** serialization version identifier */
            private static final long serialVersionUID = 6401715717710735485L;

            public void actionPerformed(ActionEvent ae) {
                JCheckBoxMenuItem menuItem = (JCheckBoxMenuItem) ae.getSource();
                controlPanel.setVisible(menuItem.isSelected());
            }
        };
        showAudioPlayerPanelAction = new DataViewerAction("Show Audio Player", "", KeyEvent.VK_A, "icons/audio.gif") {

            /** serialization version identifier */
            private static final long serialVersionUID = -4248275698973916287L;

            public void actionPerformed(ActionEvent ae) {
                JCheckBoxMenuItem menuItem = (JCheckBoxMenuItem) ae.getSource();
                audioPlayerPanel.setVisible(menuItem.isSelected());
            }
        };
        showMarkerPanelAction = new DataViewerAction("Show Marker Panel", "", KeyEvent.VK_M, "icons/info.gif") {

            /** serialization version identifier */
            private static final long serialVersionUID = -5253555511660929640L;

            public void actionPerformed(ActionEvent ae) {
                JCheckBoxMenuItem menuItem = (JCheckBoxMenuItem) ae.getSource();
                markerSubmitPanel.setVisible(menuItem.isSelected());
            }
        };
        dataPanelAction = new DataViewerAction("Arrange", "Arrange Data Panel Orientation", KeyEvent.VK_D);
        dataPanelHorizontalLayoutAction = new DataViewerAction("Horizontal Data Panel Orientation", "", -1, "icons/vertical.gif") {

            /** serialization version identifier */
            private static final long serialVersionUID = 3356151813557187908L;

            public void actionPerformed(ActionEvent ae) {
                dataPanelContainer.setLayout(DataPanelContainer.VERTICAL_LAYOUT);
            }
        };
        dataPanelVerticalLayoutAction = new DataViewerAction("Vertical Data Panel Orientation", "", -1, "icons/horizontal.gif") {

            /** serialization version identifier */
            private static final long serialVersionUID = -4629920180285927138L;

            public void actionPerformed(ActionEvent ae) {
                dataPanelContainer.setLayout(DataPanelContainer.HORIZONTAL_LAYOUT);
            }
        };
        showHiddenChannelsAction = new DataViewerAction("Show Hidden Channels", "", KeyEvent.VK_H, KeyStroke.getKeyStroke(KeyEvent.VK_H, menuShortcutKeyMask), "icons/hidden.gif") {

            /** serialization version identifier */
            private static final long serialVersionUID = -2723464261568074033L;

            public void actionPerformed(ActionEvent ae) {
                JCheckBoxMenuItem menuItem = (JCheckBoxMenuItem) ae.getSource();
                boolean selected = menuItem.isSelected();
                channelListPanel.showHiddenChannels(selected);
            }
        };
        hideEmptyTimeAction = new DataViewerAction("Hide time with no data", "", KeyEvent.VK_D) {

            /** serialization version identifier */
            private static final long serialVersionUID = -3123608144249355642L;

            public void actionPerformed(ActionEvent ae) {
                JCheckBoxMenuItem menuItem = (JCheckBoxMenuItem) ae.getSource();
                boolean selected = menuItem.isSelected();
                controlPanel.hideEmptyTime(selected);
            }
        };
        fullScreenAction = new DataViewerAction("Full Screen", "", KeyEvent.VK_F, KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)) {

            /** serialization version identifier */
            private static final long serialVersionUID = -6882310862616235602L;

            public void actionPerformed(ActionEvent ae) {
                JCheckBoxMenuItem menuItem = (JCheckBoxMenuItem) ae.getSource();
                if (menuItem.isSelected()) {
                    if (enterFullScreenMode()) {
                        menuItem.setSelected(true);
                    } else {
                        menuItem.setSelected(false);
                    }
                } else {
                    leaveFullScreenMode();
                    menuItem.setSelected(false);
                }
            }
        };
        windowAction = new DataViewerAction("Window", "Window Menu", KeyEvent.VK_W);
        closeAllDataPanelsAction = new DataViewerAction("Close all data panels", "", KeyEvent.VK_C, "icons/closeall.gif") {

            /** serialization version identifier */
            private static final long serialVersionUID = -8104876009869238037L;

            public void actionPerformed(ActionEvent ae) {
                dataPanelManager.closeAllDataPanels();
            }
        };
        helpAction = new DataViewerAction("Help", "Help Menu", KeyEvent.VK_H);
        usersGuideAction = new DataViewerAction("RDV Help", "Open the RDV User's Guide", KeyEvent.VK_H, KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0)) {

            /** serialization version identifier */
            private static final long serialVersionUID = -2837190869008153291L;

            public void actionPerformed(ActionEvent ae) {
                try {
                    URL usersGuideURL = new URL("http://it.nees.org/library/telepresence/rdv-19-users-guide.php");
                    DataViewer.browse(usersGuideURL);
                } catch (Exception e) {
                }
            }
        };
        supportAction = new DataViewerAction("RDV Support", "Get support from NEESit", KeyEvent.VK_S) {

            /** serialization version identifier */
            private static final long serialVersionUID = -6855670513381679226L;

            public void actionPerformed(ActionEvent ae) {
                try {
                    URL supportURL = new URL("http://it.nees.org/support/");
                    DataViewer.browse(supportURL);
                } catch (Exception e) {
                }
            }
        };
        releaseNotesAction = new DataViewerAction("Release Notes", "Open the RDV Release Notes", KeyEvent.VK_R) {

            /** serialization version identifier */
            private static final long serialVersionUID = 7223639998298692494L;

            public void actionPerformed(ActionEvent ae) {
                try {
                    URL releaseNotesURL = new URL("http://it.nees.org/library/rdv/rdv-release-notes.php");
                    DataViewer.browse(releaseNotesURL);
                } catch (Exception e) {
                }
            }
        };
        aboutAction = new DataViewerAction("About RDV", "", KeyEvent.VK_A) {

            /** serialization version identifier */
            private static final long serialVersionUID = 3978467903181198979L;

            public void actionPerformed(ActionEvent ae) {
                showAboutDialog();
            }
        };
    }

    private void initMenuBar() {
        Application application = RDV.getInstance();
        ApplicationContext context = application.getContext();
        ResourceMap resourceMap = context.getResourceMap();
        String platform = resourceMap.getString("platform");
        boolean isMac = (platform != null && platform.equals("osx"));
        ActionFactory actionFactory = ActionFactory.getInstance();
        menuBar = new JMenuBar();
        JMenuItem menuItem;
        JMenu fileMenu = new JMenu(fileAction);
        menuItem = new JMenuItem(connectAction);
        fileMenu.add(menuItem);
        menuItem = new JMenuItem(disconnectAction);
        fileMenu.add(menuItem);
        fileMenu.addSeparator();
        menuItem = new JMenuItem(loginAction);
        fileMenu.add(menuItem);
        menuItem = new JMenuItem(logoutAction);
        fileMenu.add(menuItem);
        fileMenu.addMenuListener(new MenuListener() {

            public void menuCanceled(MenuEvent arg0) {
            }

            public void menuDeselected(MenuEvent arg0) {
            }

            public void menuSelected(MenuEvent arg0) {
                if (AuthenticationManager.getInstance().getAuthentication() == null) {
                    loginAction.setEnabled(true);
                    logoutAction.setEnabled(false);
                } else {
                    loginAction.setEnabled(false);
                    logoutAction.setEnabled(true);
                }
            }
        });
        fileMenu.addSeparator();
        menuItem = new JMenuItem(loadAction);
        fileMenu.add(menuItem);
        menuItem = new JMenuItem(saveAction);
        fileMenu.add(menuItem);
        fileMenu.addSeparator();
        JMenu importSubMenu = new JMenu(importAction);
        menuItem = new JMenuItem(actionFactory.getDataImportAction());
        importSubMenu.add(menuItem);
        menuItem = new JMenuItem(actionFactory.getOpenSeesDataImportAction());
        importSubMenu.add(menuItem);
        menuItem = new JMenuItem(actionFactory.getJPEGImportAction());
        importSubMenu.add(menuItem);
        importSubMenu.addSeparator();
        menuItem = new JMenuItem(actionFactory.getCentralImportAction());
        importSubMenu.add(menuItem);
        fileMenu.add(importSubMenu);
        JMenu exportSubMenu = new JMenu(exportAction);
        menuItem = new JMenuItem(actionFactory.getDataExportAction());
        exportSubMenu.add(menuItem);
        menuItem = new JMenuItem(exportVideoAction);
        exportSubMenu.add(menuItem);
        fileMenu.add(exportSubMenu);
        fileMenu.addSeparator();
        menuItem = new DataViewerCheckBoxMenuItem(actionFactory.getOfflineAction());
        fileMenu.add(menuItem);
        if (!isMac) {
            menuItem = new JMenuItem(exitAction);
            fileMenu.add(menuItem);
        }
        menuBar.add(fileMenu);
        JMenu controlMenu = new JMenu(controlAction);
        menuItem = new SelectedCheckBoxMenuItem(realTimeAction);
        controlMenu.add(menuItem);
        menuItem = new SelectedCheckBoxMenuItem(playAction);
        controlMenu.add(menuItem);
        menuItem = new SelectedCheckBoxMenuItem(pauseAction);
        controlMenu.add(menuItem);
        controlMenu.addMenuListener(new MenuListener() {

            public void menuCanceled(MenuEvent arg0) {
            }

            public void menuDeselected(MenuEvent arg0) {
            }

            public void menuSelected(MenuEvent arg0) {
                int state = rbnb.getState();
                realTimeAction.setSelected(state == Player.STATE_MONITORING);
                playAction.setSelected(state == Player.STATE_PLAYING);
                pauseAction.setSelected(state == Player.STATE_STOPPED);
            }
        });
        controlMenu.addSeparator();
        menuItem = new JMenuItem(beginningAction);
        controlMenu.add(menuItem);
        menuItem = new JMenuItem(endAction);
        controlMenu.add(menuItem);
        menuItem = new JMenuItem(gotoTimeAction);
        controlMenu.add(menuItem);
        menuBar.add(controlMenu);
        controlMenu.addSeparator();
        menuItem = new JMenuItem(updateChannelListAction);
        controlMenu.add(menuItem);
        controlMenu.addSeparator();
        menuItem = new JCheckBoxMenuItem(dropDataAction);
        menuItem.setSelected(true);
        controlMenu.add(menuItem);
        JMenu viewMenu = new JMenu(viewAction);
        menuItem = new JCheckBoxMenuItem(showChannelListAction);
        menuItem.setSelected(true);
        viewMenu.add(menuItem);
        menuItem = new JCheckBoxMenuItem(showMetadataPanelAction);
        menuItem.setSelected(true);
        viewMenu.add(menuItem);
        menuItem = new JCheckBoxMenuItem(showControlPanelAction);
        menuItem.setSelected(true);
        viewMenu.add(menuItem);
        menuItem = new JCheckBoxMenuItem(showMarkerPanelAction);
        menuItem.setSelected(true);
        viewMenu.add(menuItem);
        viewMenu.addSeparator();
        menuItem = new JCheckBoxMenuItem(showHiddenChannelsAction);
        menuItem.setSelected(false);
        viewMenu.add(menuItem);
        menuItem = new JCheckBoxMenuItem(hideEmptyTimeAction);
        menuItem.setSelected(false);
        viewMenu.add(menuItem);
        viewMenu.addSeparator();
        menuItem = new JCheckBoxMenuItem(fullScreenAction);
        menuItem.setSelected(false);
        viewMenu.add(menuItem);
        menuBar.add(viewMenu);
        JMenu windowMenu = new JMenu(windowAction);
        List<Extension> extensions = dataPanelManager.getExtensions();
        for (final Extension extension : extensions) {
            Action action = new DataViewerAction("Add " + extension.getName(), "", KeyEvent.VK_J) {

                private static final long serialVersionUID = 36998228704476723L;

                public void actionPerformed(ActionEvent ae) {
                    try {
                        dataPanelManager.createDataPanel(extension);
                    } catch (Exception e) {
                        log.error("Unable to open data panel provided by extension " + extension.getName() + " (" + extension.getID() + ").");
                        e.printStackTrace();
                    }
                }
            };
            menuItem = new JMenuItem(action);
            windowMenu.add(menuItem);
        }
        windowMenu.addSeparator();
        menuItem = new JMenuItem(closeAllDataPanelsAction);
        windowMenu.add(menuItem);
        windowMenu.addSeparator();
        JMenu dataPanelSubMenu = new JMenu(dataPanelAction);
        ButtonGroup dataPanelLayoutGroup = new ButtonGroup();
        menuItem = new JRadioButtonMenuItem(dataPanelHorizontalLayoutAction);
        dataPanelSubMenu.add(menuItem);
        dataPanelLayoutGroup.add(menuItem);
        menuItem = new JRadioButtonMenuItem(dataPanelVerticalLayoutAction);
        menuItem.setSelected(true);
        dataPanelSubMenu.add(menuItem);
        dataPanelLayoutGroup.add(menuItem);
        windowMenu.add(dataPanelSubMenu);
        menuBar.add(windowMenu);
        JMenu helpMenu = new JMenu(helpAction);
        menuItem = new JMenuItem(usersGuideAction);
        helpMenu.add(menuItem);
        menuItem = new JMenuItem(supportAction);
        helpMenu.add(menuItem);
        menuItem = new JMenuItem(releaseNotesAction);
        helpMenu.add(menuItem);
        if (!isMac) {
            helpMenu.addSeparator();
            menuItem = new JMenuItem(aboutAction);
            helpMenu.add(menuItem);
        }
        menuBar.add(helpMenu);
        menuBar.add(Box.createHorizontalGlue());
        throbberStop = DataViewer.getIcon("icons/throbber.png");
        throbberAnim = DataViewer.getIcon("icons/throbber_anim.gif");
        throbber = new JLabel(throbberStop);
        throbber.setBorder(new EmptyBorder(0, 0, 0, 4));
        menuBar.add(throbber, BorderLayout.EAST);
        if (isMac) {
            registerMacOSXEvents();
        }
        frame.setJMenuBar(menuBar);
    }

    private void initChannelListPanel() {
        channelListPanel = new ChannelListPanel(dataPanelManager, rbnb);
        channelListPanel.setMinimumSize(new Dimension(0, 0));
        log.info("Created channel list panel.");
    }

    private void initMetadataPanel() {
        metadataPanel = new MetadataPanel(rbnb);
        log.info("Created metadata panel");
    }

    private void initLeftPanel() {
        leftPanel = Factory.createStrippedSplitPane(JSplitPane.VERTICAL_SPLIT, channelListPanel, metadataPanel, 0.65f);
        leftPanel.setContinuousLayout(true);
        leftPanel.setBorder(new EmptyBorder(8, 8, 8, 0));
        log.info("Created left panel");
    }

    private void initRightPanel() {
        rightPanel = new JPanel();
        rightPanel.setMinimumSize(new Dimension(0, 0));
        rightPanel.setLayout(new GridBagLayout());
    }

    private void initControls() {
        controlPanel = new ControlPanel();
        add(controlPanel, BorderLayout.NORTH);
        log.info("Added control panel.");
    }

    private void initDataPanelContainer() {
        dataPanelContainer = dataPanelManager.getDataPanelContainer();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.ipadx = 0;
        c.ipady = 0;
        c.insets = new java.awt.Insets(8, 0, 8, 6);
        c.anchor = GridBagConstraints.NORTHWEST;
        rightPanel.add(dataPanelContainer, c);
        log.info("Added data panel container.");
    }

    private void initAudioPlayerPanel() {
        audioPlayerPanel = new AudioPlayerPanel();
        audioPlayerPanel.setVisible(false);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 2;
        c.gridheight = 1;
        c.ipadx = 0;
        c.ipady = 0;
        c.insets = new java.awt.Insets(0, 0, 8, 6);
        c.anchor = GridBagConstraints.SOUTHWEST;
        rightPanel.add(audioPlayerPanel, c);
        log.info("Added Audio Player Panel.");
    }

    private void initMarkerSubmitPanel() {
        markerSubmitPanel = new MarkerSubmitPanel(rbnb);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 2;
        c.gridheight = 1;
        c.ipadx = 0;
        c.ipady = 0;
        c.insets = new java.awt.Insets(0, 0, 8, 6);
        c.anchor = GridBagConstraints.SOUTHWEST;
        rightPanel.add(markerSubmitPanel, c);
        log.info("Added Marker Submission Panel.");
    }

    private void initSplitPane() {
        splitPane = Factory.createStrippedSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel, 0.2f);
        splitPane.setContinuousLayout(true);
        add(splitPane, BorderLayout.CENTER);
    }

    /**
	 * Register event handlers for Mac OS X specific menu items.
	 */
    private void registerMacOSXEvents() {
        try {
            Application rdv = RDV.getInstance();
            OSXAdapter.setQuitHandler(rdv, Application.class.getDeclaredMethod("exit", (Class[]) null));
            OSXAdapter.setAboutHandler(this, getClass().getDeclaredMethod("showAboutDialog", (Class[]) null));
        } catch (Exception e) {
            log.warn("Unable to register Mac OS X events.");
            e.printStackTrace();
            return;
        }
        log.info("Registered Mac OS X events.");
    }

    /**
   * Hide the left part of the main split pane if both it's components are
   * visible. If either of them are visible, show it.
   */
    private void layoutSplitPane() {
        boolean visible = channelListPanel.isVisible() || metadataPanel.isVisible();
        if (leftPanel.isVisible() != visible) {
            leftPanel.setVisible(visible);
            splitPane.resetToPreferredSizes();
        }
    }

    private boolean enterFullScreenMode() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] devices = ge.getScreenDevices();
        for (int i = 0; i < devices.length; i++) {
            GraphicsDevice device = devices[i];
            if (device.isFullScreenSupported() && device.getFullScreenWindow() == null) {
                log.info("Switching to full screen mode.");
                frame.setVisible(false);
                try {
                    device.setFullScreenWindow(frame);
                } catch (InternalError e) {
                    log.error("Failed to switch to full screen exclusive mode.");
                    e.printStackTrace();
                    frame.setVisible(true);
                    return false;
                }
                frame.dispose();
                frame.setUndecorated(true);
                frame.setVisible(true);
                frame.requestFocusInWindow();
                return true;
            }
        }
        log.warn("No screens available or full screen exclusive mode is unsupported on your platform.");
        postError("Full screen mode is not supported on your platform.");
        return false;
    }

    private void leaveFullScreenMode() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] devices = ge.getScreenDevices();
        for (int i = 0; i < devices.length; i++) {
            GraphicsDevice device = devices[i];
            if (device.isFullScreenSupported() && device.getFullScreenWindow() == frame) {
                log.info("Leaving full screen mode.");
                frame.setVisible(false);
                device.setFullScreenWindow(null);
                frame.dispose();
                frame.setUndecorated(false);
                frame.setVisible(true);
                break;
            }
        }
    }

    public void postError(String errorMessage) {
        JOptionPane.showMessageDialog(this, errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public void postStatus(String statusMessage) {
        JOptionPane.showMessageDialog(this, statusMessage, "Status", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
   * Displays the about dialog
   */
    public void showAboutDialog() {
        if (aboutDialog == null) {
            aboutDialog = new AboutDialog(frame);
        } else {
            aboutDialog.setVisible(true);
        }
    }

    public void showExportVideoDialog() {
        List<String> channels = channelListPanel.getSelectedChannels();
        for (int i = channels.size() - 1; i >= 0; i--) {
            Channel channel = RBNBController.getInstance().getChannel(channels.get(i));
            String mime = channel.getMetadata("mime");
            if (!mime.equals("image/jpeg")) {
                channels.remove(i);
            }
        }
        if (channels.isEmpty()) {
            JOptionPane.showMessageDialog(null, "There are no video channels selected.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        new ExportVideoDialog(frame, rbnb, channels);
    }

    /**
   * Gets a list of channels selected in the channel list.
   * 
   * @return  a list of selected channels
   */
    public List<String> getSelectedChannels() {
        return channelListPanel.getSelectedChannels();
    }

    /**
   * A check box menu item that uses the "selected" property from it's action.
   */
    class SelectedCheckBoxMenuItem extends JCheckBoxMenuItem {

        /** serialization version identifier */
        private static final long serialVersionUID = 831834301317733433L;

        /**
     * Create a check box menu item from the action.
     * 
     * @param a  the action for this menu item
     */
        public SelectedCheckBoxMenuItem(Action a) {
            super(a);
        }

        /**
     * Configure from the action properties. This looks for the selected
     * property.
     * 
     * @param a  the action
     */
        protected void configurePropertiesFromAction(Action a) {
            super.configurePropertiesFromAction(a);
            Boolean selected = (Boolean) a.getValue("selected");
            if (selected != null && selected != isSelected()) {
                setSelected(selected);
            }
        }

        /**
     * Create an action  property change listener. This listens for the 
     * "selected" property.
     * 
     * @param a  the action to create the listener for
     */
        protected PropertyChangeListener createActionPropertyChangeListener(Action a) {
            final PropertyChangeListener listener = super.createActionPropertyChangeListener(a);
            PropertyChangeListener myListener = new PropertyChangeListener() {

                public void propertyChange(PropertyChangeEvent evt) {
                    listener.propertyChange(evt);
                    if (evt.getPropertyName().equals("selected")) {
                        Boolean selected = (Boolean) evt.getNewValue();
                        if (selected == null) {
                            setSelected(false);
                        } else if (selected != isSelected()) {
                            setSelected(selected);
                        }
                    }
                }
            };
            return myListener;
        }
    }

    public void connecting() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        if (busyDialog != null) {
            busyDialog.close();
            busyDialog = null;
        }
        busyDialog = new BusyDialog(frame);
        busyDialog.setCancelActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                rbnb.cancelConnect();
            }
        });
        busyDialog.start();
        startThrobber();
    }

    public void connected() {
        setCursor(null);
        if (busyDialog != null) {
            busyDialog.close();
            busyDialog = null;
        }
        stopThrobber();
    }

    public void connectionFailed() {
        setCursor(null);
        if (busyDialog != null) {
            busyDialog.close();
            busyDialog = null;
        }
        stopThrobber();
    }

    public void postState(int newState, int oldState) {
        if (newState == Player.STATE_DISCONNECTED) {
            frame.setTitle("RDV");
            controlAction.setEnabled(false);
            disconnectAction.setEnabled(false);
            saveAction.setEnabled(false);
            importAction.setEnabled(false);
            exportAction.setEnabled(false);
            controlPanel.setEnabled(false);
            markerSubmitPanel.setEnabled(false);
            ActionFactory.getInstance().getOfflineAction().setSelected(false);
        } else if (oldState == Player.STATE_DISCONNECTED) {
            frame.setTitle(rbnb.getServerName() + " - RDV");
            controlAction.setEnabled(true);
            disconnectAction.setEnabled(true);
            boolean offline = rbnb.getRBNBHostName().equals("localhost") && rbnb.getRBNBPortNumber() == LocalServer.getInstance().getPort();
            saveAction.setEnabled(!offline);
            importAction.setEnabled(offline);
            exportAction.setEnabled(true);
            controlPanel.setEnabled(true);
            markerSubmitPanel.setEnabled(true);
        }
        if (newState == Player.STATE_LOADING || newState == Player.STATE_PLAYING || newState == Player.STATE_MONITORING) {
            startThrobber();
        } else if (oldState == Player.STATE_LOADING || oldState == Player.STATE_PLAYING || oldState == Player.STATE_MONITORING) {
            stopThrobber();
        }
        if (newState == Player.STATE_LOADING) {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            loadingDialog = new LoadingDialog(frame);
            new Thread() {

                public void run() {
                    synchronized (loadingMonitor) {
                        if (loadingDialog == null) {
                            return;
                        }
                        try {
                            loadingMonitor.wait(1000);
                        } catch (InterruptedException e) {
                        }
                        if (loadingDialog != null) {
                            loadingDialog.setVisible(true);
                            loadingDialog.start();
                        }
                    }
                }
            }.start();
        } else if (oldState == Player.STATE_LOADING) {
            setCursor(null);
            synchronized (loadingMonitor) {
                loadingMonitor.notify();
                if (loadingDialog != null) {
                    loadingDialog.close();
                    loadingDialog = null;
                }
            }
        }
    }

    private void startThrobber() {
        throbber.setIcon(throbberAnim);
    }

    private void stopThrobber() {
        throbber.setIcon(throbberStop);
    }

    public class RDVFileFilter extends FileFilter {

        public boolean accept(File f) {
            return !f.isFile() || f.getName().endsWith(".rdv");
        }

        public String getDescription() {
            return "RDV Configuration Files (*.rdv)";
        }
    }

    ;
}
