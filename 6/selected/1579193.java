package de.frewert.vboxj.gui.swing;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileOutputStream;
import java.net.PasswordAuthentication;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.ResourceBundle;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.frewert.vboxj.MessagePlayer;
import de.frewert.vboxj.PlayableMessage;
import de.frewert.vboxj.Preferences;
import de.frewert.vboxj.event.PlaybackMonitorEvent;
import de.frewert.vboxj.event.PlaybackMonitorListener;
import de.frewert.vboxj.event.PlayerEvent;
import de.frewert.vboxj.event.PlayerListener;
import de.frewert.vboxj.gui.PluggableGUI;
import de.frewert.vboxj.gui.swing.third_party.SimpleInternalFrame;
import de.frewert.vboxj.vbox.MessageInfo;
import de.frewert.vboxj.vbox.Proxy;
import de.frewert.vboxj.vbox.VBoxException;
import de.frewert.vboxj.vbox.VboxConstants;

/**
 * A GUI for VBox/J using Swing. 
 * <pre>
 * Copyright (C) 2000, 2001, 2003, 2005 Carsten Frewert. All Rights Reserved.
 * 
 * The VBox/J package (de.frewert.vboxj.*) is distributed under
 * the terms of the Artistic license.
 * </pre>
 * @author Carsten Frewert
 * &lt;<a href="mailto:frewert@users.sourceforge.net">
 * frewert@users.sourceforge.net</a>&gt;
 */
public class VBoxGUI extends JFrame implements PluggableGUI, VboxConstants {

    private static final long serialVersionUID = 3544957670672249904L;

    private static ResourceBundle messageBundle = ResourceBundle.getBundle("de.frewert.vboxj.gui.swing.messages");

    static {
        Preferences.setDefault(Preferences.ICON_SIZE, Preferences.ICON_SIZE_BIG);
    }

    private Proxy proxy = new Proxy();

    /** user preferences */
    private Preferences prefs = new Preferences();

    private JMenuBar menubar;

    /** The main toolbar */
    private JToolBar toolbar;

    private JProgressBar progressBar;

    private JSlider volumeSlider;

    /** The table containing all messages */
    private MessageTable msgTable;

    /** The model for msgTable */
    private MessageTableModel tableModel;

    private MessageTableSorter tableSorter;

    /** A flag indicating the connection state */
    private boolean connected = false;

    private static final String NOT_CONNECTED = messageBundle.getString("not_connected");

    private SimpleInternalFrame msgFrame;

    private JLabel selectedMsgsLabel;

    private JLabel totalMsgsLabel;

    /** Popup menu for use with message table */
    private JPopupMenu messagePopup;

    /**
     * Popup menu for use with table columns.
     * @see #getColumnpopup
     */
    private JPopupMenu columnPopup;

    /** The message currently played */
    private MessagePlayer player = new MessagePlayer(proxy);

    /** Initialization, called from the class using this GUI */
    public void init() {
        initGUI();
        propagatePreferences(prefs);
        if (Boolean.TRUE.equals(loginAction.getValue(Preferences.AUTOLOGON))) {
            loginAction.actionPerformed(null);
        }
        player.addPlayerListener(playerListener);
    }

    /**
     * Called from the GUI loading class if the user closes the
     * application.
     */
    public void exit() {
        if (VBoxGUI.this.connected) {
            VBoxGUI.this.proxy.disconnect();
            VBoxGUI.this.connected = false;
            loginAction.setEnabled(true);
            logoutAction.setEnabled(false);
        }
        collectPreferences(prefs);
        player.removePlayerListener(playerListener);
        dispose();
    }

    /**
     * Get all preferences.
     * @return clone of the Preferences object
     * @see #setPreferences
     */
    public Preferences getPreferences() {
        return (Preferences) this.prefs.clone();
    }

    /**
     * Set the preferences to use.
     * If called with a <code>null</code> value,
     * a new Preferences instance will be used.
     * @param prefs the preferences to use
     * @see #getPreferences
     */
    public void setPreferences(final Preferences prefs) {
        this.prefs = (prefs == null) ? new Preferences() : prefs;
    }

    private void setIcons() {
        String sizeName = prefs.get(Preferences.ICON_SIZE);
        IconSet icons = Preferences.ICON_SIZE_SMALL.equalsIgnoreCase(sizeName) ? new IconSet("iconset_small.properties") : new IconSet("iconset_large.properties");
        loginAction.setIcon(icons.getIcon("connect"));
        logoutAction.setIcon(icons.getIcon("disconnect"));
        preferencesAction.setIcon(icons.getIcon("preferences"));
        quitAction.setIcon(icons.getIcon("quit"));
        listAction.setIcon(icons.getIcon("list"));
        stepBackAction.setIcon(icons.getIcon("stepBack"));
        playAction.setIcon(icons.getIcon("play"));
        pauseAction.setIcon(icons.getIcon("pause"));
        stopAction.setIcon(icons.getIcon("stop"));
        stepForwardAction.setIcon(icons.getIcon("stepForward"));
        saveAsAction.setIcon(icons.getIcon("saveAs"));
        toggleAction.setIcon(icons.getIcon("toggle"));
        copyCallerIdToClipboard.setIcon(icons.getIcon("clipboard"));
        deleteAction.setIcon(icons.getIcon("delete"));
        helpAction.setIcon(icons.getIcon("help"));
        aboutAction.setIcon(icons.getIcon("about"));
    }

    private void setLookAndFeel() {
        String[] lookAndFeelNames = { "com.jgoodies.plaf.plastic.Plastic3DLookAndFeel", "com.jgoodies.looks.plastic.Plastic3DLookAndFeel" };
        for (int i = 0; i < lookAndFeelNames.length; i++) {
            try {
                Class lookAndFeel = Class.forName(lookAndFeelNames[i]);
                UIManager.setLookAndFeel((LookAndFeel) lookAndFeel.newInstance());
                break;
            } catch (Exception e) {
            }
        }
        UIManager.put("Application.useSystemFontSettings", Boolean.TRUE);
    }

    /**
     * Use Plastic L&F specific options if possible.
     * Does nothing if another Look&Feel is in use.
     */
    private void setPlasticOptions() {
        if (UIManager.getLookAndFeel().getName().toLowerCase().indexOf("plastic") == -1) {
            return;
        }
        toolbar.putClientProperty(com.jgoodies.plaf.Options.HEADER_STYLE_KEY, com.jgoodies.plaf.HeaderStyle.BOTH);
    }

    /**
     * Build all panels, disable some Actions, set some listeners...
     */
    private void initGUI() {
        setLookAndFeel();
        menubar = new JMenuBar();
        toolbar = new JToolBar();
        progressBar = new JProgressBar();
        setPlasticOptions();
        FormLayout layout = new FormLayout("3dlu, pref:grow, right:pref, pref, pref, pref, 3dlu", "pref, 9dlu, pref, 3dlu, fill:min:grow, 3dlu, pref, 3dlu");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        buildMenus(menubar);
        logoutAction.setEnabled(false);
        listAction.setEnabled(false);
        stepBackAction.setEnabled(false);
        playAction.setEnabled(false);
        pauseAction.setEnabled(false);
        stopAction.setEnabled(false);
        stepForwardAction.setEnabled(false);
        saveAsAction.setEnabled(false);
        toggleAction.setEnabled(false);
        copyCallerIdToClipboard.setEnabled(false);
        deleteAction.setEnabled(false);
        helpAction.setEnabled(false);
        this.setJMenuBar(menubar);
        builder.add(buildHeader(), cc.xywh(1, 1, 6, 1));
        selectedMsgsLabel = new JLabel("0");
        selectedMsgsLabel.setHorizontalAlignment(JLabel.RIGHT);
        JPanel selectionInfo = new JPanel();
        selectionInfo.add(selectedMsgsLabel);
        JLabel sep = new JLabel(" / ");
        selectionInfo.add(sep);
        totalMsgsLabel = new JLabel("0");
        selectionInfo.add(totalMsgsLabel);
        JLabel selLabel = new JLabel(" " + Utils.getStringFromBundle(messageBundle, "selected"));
        selectionInfo.add(selLabel);
        msgFrame = new SimpleInternalFrame(NOT_CONNECTED, selectionInfo, buildMessagePane());
        builder.add(msgFrame, cc.xywh(2, 5, 5, 1));
        builder.add(progressBar, cc.xywh(2, 7, 5, 1));
        setContentPane(builder.getPanel());
        progressBar.setOpaque(false);
        copyCallerIdToClipboard.setMsgTable(msgTable);
        ClipboardSupport.addClipboardSupport(msgTable, copyCallerIdToClipboard);
    }

    private JPanel buildHeader() {
        FormLayout layout = new FormLayout("pref, 9dlu, pref:grow", "pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        buildToolbar(this.toolbar);
        builder.add(this.toolbar, cc.xy(1, 1, "left, top"));
        volumeSlider = createVolumeSlider();
        builder.add(this.volumeSlider, cc.xy(3, 1));
        return builder.getPanel();
    }

    private JScrollPane buildMessagePane() {
        tableModel = new MessageTableModel();
        tableSorter = new MessageTableSorter(tableModel);
        tableSorter.sortByColumn(MessageTableModel.COL_CTIME);
        msgTable = new MessageTable(tableSorter);
        msgTable.setEnabled(false);
        final JTableHeader tableHeader = msgTable.getTableHeader();
        TableCellRenderer headerRenderer = tableHeader.getDefaultRenderer();
        TableColumnModel columnModel = msgTable.getColumnModel();
        TableColumn col = columnModel.getColumn(0);
        Component comp = headerRenderer.getTableCellRendererComponent(null, col.getHeaderValue(), false, false, 0, 0);
        int headerWidth = comp.getPreferredSize().width;
        col.setPreferredWidth(headerWidth + 8);
        tableSorter.addMouseListenerToHeaderInTable(msgTable);
        tableSorter.addTableModelListener(tableModelListener);
        int lengthColumnIndex = tableSorter.findColumn(VBoxGUI.getMsgBundle().getString("length"));
        msgTable.getColumnModel().getColumn(msgTable.convertColumnIndexToView(lengthColumnIndex)).setCellRenderer(new DurationCellRenderer());
        msgTable.setPreferredScrollableViewportSize(new Dimension((int) msgTable.getPreferredSize().getWidth(), 300));
        msgTable.getSelectionModel().addListSelectionListener(msgTableSelectionListener);
        JScrollPane tableScrollPane = new JScrollPane(msgTable);
        tableScrollPane.setOpaque(false);
        tableScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        msgTable.addMouseListener(mouseListener);
        tableHeader.addMouseListener(mouseListener);
        tableHeader.setToolTipText(messageBundle.getString("tableHeaderTip"));
        return tableScrollPane;
    }

    private TableModelListener tableModelListener = new TableModelListener() {

        public void tableChanged(TableModelEvent e) {
        }
    };

    /**
     * SelectionListener for the message table.
     */
    private ListSelectionListener msgTableSelectionListener = new ListSelectionListener() {

        public void valueChanged(final ListSelectionEvent e) {
            selectedMsgsLabel.setText(String.valueOf(msgTable.getSelectedRows().length));
            if (e.getValueIsAdjusting()) {
                return;
            }
            ListSelectionModel lsm = (ListSelectionModel) e.getSource();
            boolean selEmpty = lsm.isSelectionEmpty();
            if (VBoxGUI.this.player.getCurrentMessage() == null) {
                playAction.setEnabled(!selEmpty);
                stepBackAction.setEnabled(!selEmpty);
                stepForwardAction.setEnabled(!selEmpty);
            }
            saveAsAction.setEnabled(!selEmpty);
            toggleAction.setEnabled(!selEmpty);
            copyCallerIdToClipboard.setEnabled(!selEmpty);
            deleteAction.setEnabled(!selEmpty);
        }
    };

    /**
     * Build all menus from Action objects.
     * @param menubar the menu bar to add the menus to
     */
    private void buildMenus(JMenuBar menubar) {
        menubar.add(buildServerMenu());
        menubar.add(buildMessageMenu());
        messagePopup = buildMessagePopup();
        menubar.add(Box.createHorizontalGlue());
        menubar.add(buildHelpMenu());
    }

    private JMenu buildServerMenu() {
        JMenu menu = Utils.createMenu(messageBundle.getString("ServerMenu"));
        menu.add(new JMenuItem(loginAction));
        menu.add(new JMenuItem(logoutAction));
        menu.addSeparator();
        menu.add(new JMenuItem(preferencesAction));
        menu.addSeparator();
        menu.add(new JMenuItem(quitAction));
        return menu;
    }

    private JMenu buildMessageMenu() {
        JMenu menu = Utils.createMenu(messageBundle.getString("MessageMenu"));
        menu.add(new JMenuItem(listAction));
        menu.addSeparator();
        menu.add(new JMenuItem(stepBackAction));
        menu.add(new JMenuItem(playAction));
        menu.add(new JMenuItem(pauseAction));
        menu.add(new JMenuItem(stopAction));
        menu.add(new JMenuItem(stepForwardAction));
        menu.addSeparator();
        menu.add(new JMenuItem(toggleAction));
        menu.add(new JMenuItem(saveAsAction));
        menu.add(new JMenuItem(copyCallerIdToClipboard));
        menu.addSeparator();
        menu.add(new JMenuItem(deleteAction));
        return menu;
    }

    private JMenu buildHelpMenu() {
        JMenu menu = Utils.createMenu(Utils.getStringFromBundle(messageBundle, "HelpMenu"));
        menu.add(new JMenuItem(helpAction));
        menu.addSeparator();
        menu.add(new JMenuItem(aboutAction));
        return menu;
    }

    /**
     * Build message popup menu
     */
    private JPopupMenu buildMessagePopup() {
        JPopupMenu popup = new JPopupMenu("message popup");
        popup.add(new JMenuItem(listAction));
        popup.addSeparator();
        popup.add(new JMenuItem(stepBackAction));
        popup.add(new JMenuItem(playAction));
        popup.add(new JMenuItem(pauseAction));
        popup.add(new JMenuItem(stopAction));
        popup.add(new JMenuItem(stepForwardAction));
        popup.addSeparator();
        popup.add(new JMenuItem(toggleAction));
        popup.add(new JMenuItem(saveAsAction));
        popup.add(new JMenuItem(copyCallerIdToClipboard));
        popup.addSeparator();
        popup.add(new JMenuItem(deleteAction));
        return popup;
    }

    /**
     * Get the context menu for column headers.
     */
    private JPopupMenu getColumnPopup() {
        if (columnPopup == null) {
            columnPopup = new JPopupMenu("column popup");
            final TableModel model = msgTable.getModel();
            ItemListener columnItemListener = new ItemListener() {

                TableColumnModel columnModel = msgTable.getColumnModel();

                TableColumn[] columns = new TableColumn[model.getColumnCount()];

                public void itemStateChanged(ItemEvent e) {
                    int modelIndex = columnPopup.getComponentIndex((Component) e.getItem());
                    int viewIndex = msgTable.convertColumnIndexToView(modelIndex);
                    switch(e.getStateChange()) {
                        case ItemEvent.DESELECTED:
                            if (viewIndex >= 0) {
                                TableColumn col = columnModel.getColumn(viewIndex);
                                if (columns[modelIndex] == null) {
                                    columns[modelIndex] = col;
                                }
                                columnModel.removeColumn(col);
                            }
                            break;
                        case ItemEvent.SELECTED:
                            columnModel.addColumn(columns[modelIndex]);
                            break;
                    }
                }
            };
            int columns = model.getColumnCount();
            for (int i = 0; i < columns; i++) {
                JCheckBoxMenuItem mi = new JCheckBoxMenuItem(model.getColumnName(i));
                mi.setSelected(true);
                columnPopup.add(mi);
                mi.addItemListener(columnItemListener);
            }
        }
        return columnPopup;
    }

    /**
     * Create a JButton <em>not</em> displaying any text.
     */
    private JButton createToolbarButton(Action action) {
        JButton b = new JButton();
        if (System.getProperty("java.version").compareTo("1.4") >= 0) {
            b.setFocusable(false);
        }
        b.putClientProperty("hideActionText", Boolean.TRUE);
        b.setAction(action);
        return b;
    }

    /**
     * Build a toolbar using some Actions.
     * @param toolbar the toolbar to fill with buttons
     * @return the filled toolbar.
     */
    private JToolBar buildToolbar(JToolBar toolbar) {
        toolbar.setFloatable(false);
        toolbar.add(createToolbarButton(loginAction));
        toolbar.add(createToolbarButton(logoutAction));
        toolbar.addSeparator();
        toolbar.add(createToolbarButton(listAction));
        toolbar.addSeparator();
        toolbar.add(createToolbarButton(stepBackAction));
        toolbar.add(createToolbarButton(playAction));
        toolbar.add(createToolbarButton(pauseAction));
        toolbar.add(createToolbarButton(stopAction));
        toolbar.add(createToolbarButton(stepForwardAction));
        toolbar.addSeparator();
        toolbar.add(createToolbarButton(toggleAction));
        toolbar.add(createToolbarButton(saveAsAction));
        toolbar.add(createToolbarButton(copyCallerIdToClipboard));
        toolbar.addSeparator();
        toolbar.add(createToolbarButton(deleteAction));
        return toolbar;
    }

    /**
     * Create a JSlider used for adjusting the output volume.
     * @return volume slider
     */
    private JSlider createVolumeSlider() {
        JSlider slider = new JSlider(JSlider.HORIZONTAL, 0, 100, 60);
        slider.addChangeListener(volumeChangeListener);
        slider.setValue(90);
        slider.setToolTipText(messageBundle.getString("VolumeTip"));
        Hashtable labels = slider.createStandardLabels(20);
        Iterator iterator = labels.values().iterator();
        while (iterator.hasNext()) {
            Object obj = iterator.next();
            if (!(obj instanceof JLabel)) {
                continue;
            }
            JLabel label = (JLabel) obj;
            Font font = label.getFont();
            label.setFont(font.deriveFont(font.getSize() - 2F));
        }
        slider.setLabelTable(labels);
        slider.setMajorTickSpacing(20);
        slider.setMinorTickSpacing(10);
        slider.setPaintLabels(true);
        slider.setPaintTicks(true);
        return slider;
    }

    /** ChangeListener for volume slider in toolbar. */
    private ChangeListener volumeChangeListener = new ChangeListener() {

        public void stateChanged(final ChangeEvent e) {
            JSlider source = (JSlider) e.getSource();
            double vol = source.getValue();
            PlayableMessage.setDefaultGain(vol);
            try {
                player.setVolume(vol);
            } catch (Exception ex) {
                System.err.println("Illegal value for gain: " + vol);
            }
        }
    };

    private Preferences collectPreferences(Preferences preferences) {
        if (preferences == null) {
            preferences = new Preferences();
        }
        preferences.set(Preferences.HOST, (String) loginAction.getValue(Preferences.HOST));
        preferences.set(Preferences.PORT, (String) loginAction.getValue(Preferences.PORT));
        preferences.set(Preferences.USER, (String) loginAction.getValue(Preferences.USER));
        preferences.set(Preferences.PASSWORD, (String) loginAction.getValue(Preferences.PASSWORD));
        TableColumnModel columnModel = msgTable.getColumnModel();
        StringBuffer modelIndices = new StringBuffer();
        int numCols = msgTable.getColumnCount();
        for (int i = 0; i < numCols; i++) {
            modelIndices.append(msgTable.convertColumnIndexToModel(i)).append(":").append(columnModel.getColumn(i).getWidth()).append(" ");
        }
        preferences.set(Preferences.COLUMNS, modelIndices.toString());
        preferences.set(Preferences.VOLUME, String.valueOf(volumeSlider.getValue()));
        preferences.set(Preferences.FILE_FORMAT, (String) saveAsAction.getValue(Preferences.FILE_FORMAT));
        Frame frame = (Frame) SwingUtilities.getRoot(this);
        if (frame != null) {
            preferences.set(Preferences.DIMENSION, frame.getWidth() + "x" + frame.getHeight());
        }
        return preferences;
    }

    /**
     * @see #setPreferences
     */
    private void propagatePreferences(final Preferences preferences) {
        if (preferences == null) {
            return;
        }
        setIcons();
        loginAction.putValue(Preferences.HOST, prefs.get(Preferences.HOST));
        loginAction.putValue(Preferences.PORT, prefs.get(Preferences.PORT));
        loginAction.putValue(Preferences.USER, prefs.get(Preferences.USER));
        loginAction.putValue(Preferences.PASSWORD, prefs.get(Preferences.PASSWORD));
        String value = prefs.get(Preferences.VOLUME);
        if (value != null) {
            try {
                volumeSlider.setValue(Integer.parseInt(value));
            } catch (NumberFormatException nfe) {
                nfe.printStackTrace();
            }
        }
        value = prefs.get(Preferences.COLUMNS);
        if (value != null) {
            JPopupMenu popup = getColumnPopup();
            int[] colMap = new int[msgTable.getModel().getColumnCount()];
            java.util.Arrays.fill(colMap, -1);
            int[] widthMap = new int[colMap.length];
            java.util.StringTokenizer st = new java.util.StringTokenizer(value);
            for (int i = 0; st.hasMoreTokens(); i++) {
                String col = st.nextToken();
                String width = null;
                int pos = col.indexOf(':');
                if (pos != -1) {
                    width = col.substring(pos + 1);
                    col = col.substring(0, pos);
                }
                try {
                    int index = Integer.parseInt(col);
                    colMap[index] = i;
                    if (width != null) {
                        widthMap[index] = Integer.parseInt(width);
                    }
                } catch (NumberFormatException nfe) {
                    nfe.printStackTrace();
                }
            }
            for (int i = 0; i < colMap.length; i++) {
                if (colMap[i] == -1) {
                    Component menuItem = popup.getComponent(i);
                    if (menuItem instanceof javax.swing.AbstractButton) {
                        ((javax.swing.AbstractButton) menuItem).setSelected(false);
                    }
                }
            }
            TableColumnModel columnModel = msgTable.getColumnModel();
            for (int i = 0; i < colMap.length; i++) {
                int newIndex = colMap[i];
                if (newIndex == -1) {
                    continue;
                }
                int viewIndex = msgTable.convertColumnIndexToView(i);
                if (widthMap[i] > 0) {
                    columnModel.getColumn(viewIndex).setPreferredWidth(widthMap[i]);
                }
                if (newIndex != viewIndex) {
                    columnModel.moveColumn(viewIndex, newIndex);
                }
            }
        }
        value = prefs.get(Preferences.AUTOLOGON);
        loginAction.putValue(Preferences.AUTOLOGON, Utils.text2Boolean(value));
        value = prefs.get(Preferences.SAVE_DIR);
        if (value != null) {
            saveAsAction.putValue(Preferences.SAVE_DIR, value);
        }
        value = prefs.get(Preferences.FILE_FORMAT);
        if (value != null) {
            saveAsAction.putValue(Preferences.FILE_FORMAT, value);
        }
    }

    /** React on double- and right-clicks on message table */
    private MouseListener mouseListener = new MouseAdapter() {

        public void mousePressed(final MouseEvent e) {
            processEvent(e);
        }

        public void mouseReleased(final MouseEvent e) {
            if (VBoxGUI.this.playAction.isEnabled() && (e.getClickCount() == 2)) {
                msgTable.setEnabled(false);
                playSelectedMessages();
            } else {
                processEvent(e);
            }
        }

        private void processEvent(final MouseEvent e) {
            if (e.isPopupTrigger()) {
                Component source = e.getComponent();
                if (source instanceof JTable) {
                    final int height = messagePopup.getHeight();
                    messagePopup.show(source, e.getX(), e.getY() - height / 2);
                } else if (source instanceof JTableHeader) {
                    JTableHeader header = (JTableHeader) source;
                    getColumnPopup().show(header, e.getX(), e.getY());
                }
            }
        }
    };

    private ImageIcon connectIcon;

    private ImageIcon disconnectIcon;

    private ImageIcon blankIcon;

    {
        connectIcon = Utils.loadIcon("/org/javalobby/icons/20x20png/Plug.png");
        disconnectIcon = Utils.loadIcon("/org/javalobby/icons/20x20png/UnPlug.png");
        blankIcon = Utils.loadIcon("/org/javalobby/icons/20x20png/blank-20.png");
    }

    /**
     * @return true if the compression type is supported, false otherwise
     */
    private boolean isSupportedFormat(MessageInfo msgInfo) {
        return (msgInfo.getCompression() == MessageInfo.ULAW);
    }

    private XAction loginAction = new XAction(messageBundle.getString("LoginAction"), messageBundle.getString("LoginTip")) {

        private static final long serialVersionUID = -5638104744982898952L;

        private LoginDialog loginDialog;

        private AbstractButton loadListButton;

        private boolean isFirstLogin = true;

        private AbstractButton getLoadListButton() {
            if (VBoxGUI.this.toolbar == null) {
                return null;
            }
            AbstractButton result = null;
            Component[] components = VBoxGUI.this.toolbar.getComponents();
            for (int i = 0; i < components.length; i++) {
                if (components[i] instanceof AbstractButton) {
                    AbstractButton button = (AbstractButton) components[i];
                    if (button.getAction() == VBoxGUI.this.listAction) {
                        result = button;
                        break;
                    }
                }
            }
            return result;
        }

        public void actionPerformed(final ActionEvent e) {
            if (loginDialog == null) {
                loginDialog = new LoginDialog(VBoxGUI.this, (String) getValue(Preferences.HOST), (String) getValue(Preferences.PORT), (String) getValue(Preferences.USER), (String) getValue(Preferences.PASSWORD));
            } else {
                loginDialog.reset();
            }
            Object val = getValue(Preferences.AUTOLOGON);
            if ((val instanceof Boolean) && (Boolean.FALSE.equals((Boolean) val) || !isFirstLogin)) {
                loginDialog.show();
            } else {
                loginDialog.doClickOK();
            }
            isFirstLogin = false;
            if (!loginDialog.hasBeenCanceled()) {
                String host = loginDialog.getHost();
                PasswordAuthentication authInfo = loginDialog.getAuthInfo();
                int port = loginDialog.getPort();
                try {
                    VBoxGUI.this.proxy.connect(host, port);
                    VBoxGUI.this.connected = true;
                    String user = authInfo.getUserName();
                    String pass = new String(authInfo.getPassword());
                    VBoxGUI.this.proxy.login(user, pass);
                    msgFrame.setTitle(messageBundle.getString("connected") + " " + user + "@" + host + ":" + port);
                    this.setEnabled(false);
                    VBoxGUI.this.logoutAction.setEnabled(true);
                    VBoxGUI.this.msgTable.setEnabled(true);
                    VBoxGUI.this.listAction.setEnabled(true);
                    if (loadListButton == null) {
                        loadListButton = getLoadListButton();
                    }
                    if (loadListButton != null) {
                        loadListButton.doClick();
                    }
                    msgTable.revalidate();
                    putValue(Preferences.HOST, host);
                    putValue(Preferences.PORT, String.valueOf(port));
                    putValue(Preferences.USER, user);
                    if (getValue(Preferences.PASSWORD) != null) {
                        putValue(Preferences.PASSWORD, pass);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    showErrorDialog("loginError.title", null, "loginError.message", ex);
                }
            }
        }
    };

    private XAction logoutAction = new XAction(messageBundle.getString("LogoutAction"), messageBundle.getString("LogoutTip")) {

        private static final long serialVersionUID = -5349461593837941029L;

        public void actionPerformed(final ActionEvent e) {
            VBoxGUI.this.proxy.disconnect();
            VBoxGUI.this.connected = false;
            VBoxGUI.this.msgFrame.setTitle(VBoxGUI.NOT_CONNECTED);
            VBoxGUI.this.selectedMsgsLabel.setText("0");
            VBoxGUI.this.totalMsgsLabel.setText("0");
            VBoxGUI.this.msgTable.setEnabled(false);
            this.setEnabled(false);
            VBoxGUI.this.loginAction.setEnabled(true);
            VBoxGUI.this.listAction.setEnabled(false);
            tableModel.clear();
        }
    };

    private XAction preferencesAction = new XAction(messageBundle.getString("PreferencesAction")) {

        private static final long serialVersionUID = 1167197649064034967L;

        private PreferencesDialog dialog;

        public void actionPerformed(final ActionEvent e) {
            synchronized (this) {
                if (dialog == null) {
                    dialog = new PreferencesDialog(VBoxGUI.this, VBoxGUI.this.collectPreferences(VBoxGUI.this.prefs));
                } else {
                    dialog.setPreferences(VBoxGUI.this.collectPreferences(VBoxGUI.this.prefs));
                }
            }
            dialog.show();
            if (!dialog.hasBeenCanceled()) {
                Preferences newPrefs = dialog.getPreferences();
                VBoxGUI.this.setPreferences(newPrefs);
                VBoxGUI.this.propagatePreferences(newPrefs);
            }
            dialog.dispose();
        }
    };

    private XAction quitAction = new XAction(messageBundle.getString("QuitAction"), blankIcon, messageBundle.getString("QuitTip")) {

        private static final long serialVersionUID = 7746100317080321972L;

        public void actionPerformed(final ActionEvent e) {
            VBoxGUI.this.exit();
        }
    };

    private XAction listAction = new XAction(messageBundle.getString("ListAction"), messageBundle.getString("ListTip")) {

        private static final long serialVersionUID = -6049426080730698754L;

        void selectUnreadMessages(final JTable table) {
            int msgCount = table.getRowCount();
            for (int i = 0; i < msgCount; i++) {
                MessageInfo msg = tableSorter.getMessageInfoAt(i);
                if (msg.isUnread()) {
                    table.addRowSelectionInterval(i, i);
                }
            }
            ListSelectionModel lsm = table.getSelectionModel();
            int first = lsm.getMinSelectionIndex();
            if (first != -1) {
                int last = lsm.getMaxSelectionIndex();
                Rectangle rect = table.getCellRect(first, 0, true);
                rect.add(table.getCellRect(last, 0, true));
                table.scrollRectToVisible(rect);
            }
        }

        public void actionPerformed(final ActionEvent e) {
            Thread listThread = new Thread("List update") {

                public void run() {
                    try {
                        java.util.List ml = proxy.listMessages();
                        VBoxGUI.this.tableModel.addAllMessageInfos(ml);
                        VBoxGUI.this.totalMsgsLabel.setText(String.valueOf(msgTable.getRowCount()));
                        SwingUtilities.invokeLater(new Runnable() {

                            public void run() {
                                selectUnreadMessages(VBoxGUI.this.msgTable);
                            }
                        });
                    } catch (VBoxException vbe) {
                        vbe.printStackTrace();
                        showErrorDialog("listError.title", null, "listError.message", vbe);
                    }
                    VBoxGUI.this.setProgressVisible(false);
                    VBoxGUI.this.msgTable.setEnabled(true);
                    listAction.setEnabled(true);
                }
            };
            VBoxGUI.this.msgTable.setEnabled(false);
            this.setEnabled(false);
            VBoxGUI.this.setProgressVisible(true);
            listThread.start();
        }
    };

    private void markMessageAsRead(final int rowIndex) throws VBoxException {
        markMessageAsRead(tableSorter.getMessageInfoAt(rowIndex));
    }

    private void markMessageAsRead(final MessageInfo msgInfo) throws VBoxException {
        if ((msgInfo == null) || !msgInfo.isUnread()) {
            return;
        }
        long newMtime = proxy.toggleMessage(msgInfo.getFilename());
        msgInfo.setMtime(newMtime);
        tableSorter.fireTableCellUpdated(tableSorter.getRow(msgInfo), MessageTableModel.COL_STATUS);
    }

    private void setProgressVisible(final boolean visible) {
        if (System.getProperty("java.version").compareTo("1.4") >= 0) {
            this.progressBar.setIndeterminate(visible);
        }
    }

    /**
     * Play all selected messages from {@link #msgTable}.
     * @see de.fewert.vboxj.MessagePlayer
     */
    private void playSelectedMessages() {
        playAction.setEnabled(false);
        pauseAction.setEnabled(true);
        stopAction.setEnabled(true);
        player.clear();
        int[] index = msgTable.getSelectedRows();
        boolean unsupportedFormatFound = false;
        for (int i = 0; i < index.length; i++) {
            try {
                MessageInfo msgInfo = tableSorter.getMessageInfoAt(index[i]);
                unsupportedFormatFound = !isSupportedFormat(msgInfo);
                if (unsupportedFormatFound) {
                    continue;
                }
                player.add(msgInfo);
                player.debug();
            } catch (Exception e) {
                e.printStackTrace();
                showErrorDialog("playError.title", null, "playError.message", e);
            }
        }
        if (unsupportedFormatFound) {
            showErrorDialog("formatError.title", null, "formatError.message", null);
        }
        if (player.size() == 0) {
            playAction.setEnabled(true);
            pauseAction.setEnabled(false);
            stopAction.setEnabled(false);
            return;
        }
        player.start();
    }

    public void showErrorDialog(final String titleKey, final Object titleArgs, final String messageKey, final Object messageArgs) {
        String title = Utils.getStringFromBundle(messageBundle, titleKey);
        String message = Utils.getStringFromBundle(messageBundle, messageKey);
        title = Utils.safeMessageFormat(title, titleArgs);
        message = Utils.safeMessageFormat(message, messageArgs);
        JOptionPane.showMessageDialog(VBoxGUI.this, message, title, JOptionPane.ERROR_MESSAGE);
    }

    private PlayerListener playerListener = new PlayerListener() {

        private PlaybackMonitorListener pml = new PlaybackMonitorListener() {

            public void update(final PlaybackMonitorEvent pme) {
                int length = pme.getLength();
                if (VBoxGUI.this.progressBar.getMaximum() != length) {
                    VBoxGUI.this.progressBar.setMaximum(length);
                }
                VBoxGUI.this.progressBar.setValue(pme.getPosition());
            }
        };

        public void replayStarted(final PlayerEvent pe) {
            int rowIndex = tableSorter.getRow(pe.getMessageInfo());
            msgTable.setPlayingIndex(rowIndex);
            Rectangle rect = msgTable.getCellRect(rowIndex, 0, true);
            msgTable.scrollRectToVisible(rect);
            pe.getPlayer().addPlaybackMonitorListener(pml);
        }

        public void replayFinished(final PlayerEvent pe) {
            System.out.println("replayFinished()");
            pe.getPlayer().removePlaybackMonitorListener(pml);
            VBoxGUI.this.progressBar.setValue(0);
            msgTable.setPlayingIndex(-1);
            if (pe != null) {
                try {
                    markMessageAsRead(pe.getMessageInfo());
                } catch (VBoxException vbe) {
                    vbe.printStackTrace();
                    showErrorDialog("toggleError.title", null, "toggleError.message", vbe);
                }
                System.out.println("Finished index " + pe.getIndex() + ", size is " + pe.getPlayer().size());
                if (pe.getIndex() + 1 >= pe.getPlayer().size()) {
                    playAction.setEnabled(true);
                    pauseAction.setEnabled(false);
                    msgTable.setEnabled(true);
                }
            }
        }

        public void replayCanceled(final PlayerEvent pe) {
            System.out.println("Canceled!");
        }
    };

    private XAction stepBackAction = new XAction(messageBundle.getString("StepBackAction"), messageBundle.getString("StepBackTip")) {

        private static final long serialVersionUID = -6786556428609916001L;

        public void actionPerformed(final ActionEvent e) {
            player.playPrevious();
            player.debug();
        }
    };

    private XAction playAction = new XAction(messageBundle.getString("PlayAction"), messageBundle.getString("PlayTip")) {

        private static final long serialVersionUID = 8545759864470537273L;

        public void actionPerformed(final ActionEvent e) {
            if (VBoxGUI.this.player.getCurrentMessage() != null) {
                return;
            }
            msgTable.setEnabled(false);
            playSelectedMessages();
        }
    };

    private XAction pauseAction = new XAction(messageBundle.getString("PauseAction"), messageBundle.getString("PauseTip")) {

        private static final long serialVersionUID = -7621472535980167165L;

        private volatile boolean paused;

        public void actionPerformed(final ActionEvent e) {
            if (VBoxGUI.this.player.getCurrentMessage() == null) {
                return;
            }
            if (!paused) {
                player.pause();
            } else {
                player.resume();
            }
            paused = !paused;
        }

        public synchronized void setEnabled(final boolean enabled) {
            if (enabled != isEnabled()) {
                paused = false;
            }
            super.setEnabled(enabled);
        }
    };

    private XAction stopAction = new XAction(messageBundle.getString("StopAction"), messageBundle.getString("StopTip")) {

        private static final long serialVersionUID = -7557093583203757965L;

        public void actionPerformed(final ActionEvent e) {
            player.stop();
            pauseAction.setEnabled(false);
            msgTable.setEnabled(true);
        }
    };

    private XAction stepForwardAction = new XAction(messageBundle.getString("StepForwardAction"), messageBundle.getString("StepForwardTip")) {

        private static final long serialVersionUID = 8665865345730272691L;

        public void actionPerformed(final ActionEvent e) {
            player.playNext();
            player.debug();
        }
    };

    private XAction saveAsAction = new XAction(messageBundle.getString("SaveAsAction"), messageBundle.getString("SaveAsTip")) {

        private static final long serialVersionUID = 4530443473056517325L;

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss");

        /** texts for buttons in overwrite dialog */
        final String[] overwriteOptions = new String[] { messageBundle.getString("overwriteButtonText"), messageBundle.getString("cancel") };

        /**
         * Build a filename for saving a message.
         */
        String buildFilename(MessageInfo msg) {
            if ((msg == null)) {
                return "";
            }
            StringBuffer name = new StringBuffer();
            name.append(dateFormat.format(msg.getCtime()));
            String caller = msg.getName();
            if (caller.length() > 0) {
                name.append(" ").append(caller);
            } else {
                String callerID = msg.getCaller();
                if (!callerID.equals("0")) {
                    name.append(" ").append(callerID);
                }
            }
            return name.toString();
        }

        public void actionPerformed(final ActionEvent e) {
            final int[] rows = msgTable.getSelectedRows();
            if (rows.length == 0) {
                return;
            }
            boolean containsUnsupportedFormat = false;
            ArrayList validMessages = new ArrayList(rows.length);
            for (int i = 0; i < rows.length; i++) {
                MessageInfo mi = tableSorter.getMessageInfoAt(rows[i]);
                containsUnsupportedFormat = !isSupportedFormat(mi);
                if (containsUnsupportedFormat) {
                    continue;
                }
                validMessages.add(mi);
            }
            if (containsUnsupportedFormat) {
                showErrorDialog("formatError.title", null, "formatError.message", null);
            }
            if (validMessages.size() == 0) {
                return;
            }
            MessageInfo[] msgInfo = (MessageInfo[]) validMessages.toArray(new MessageInfo[validMessages.size()]);
            String cwd = (String) getValue(Preferences.SAVE_DIR);
            SoundFileChooser chooser = new SoundFileChooser(cwd);
            Object ext = getValue(Preferences.FILE_FORMAT);
            if (ext instanceof String) {
                AudioFileFilter extFilter = chooser.getFilterByExtension((String) ext);
                if (extFilter != null) {
                    chooser.setFileFilter(extFilter);
                }
            }
            if (rows.length > 1) {
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                chooser.setSelectedFile(new File(cwd));
            } else {
                MagicFileFilter filter = (MagicFileFilter) chooser.getFileFilter();
                chooser.setSelectedFile(new File(buildFilename(msgInfo[0]) + filter.getExtension()));
            }
            int state = chooser.showSaveDialog(VBoxGUI.this);
            if (state != JFileChooser.APPROVE_OPTION) {
                return;
            }
            File target = chooser.getSelectedFile();
            if (rows.length > 1) {
                if (!target.isDirectory()) {
                    showErrorDialog("saveError.title", null, "saveError.nodir", target);
                    return;
                } else if (!target.canWrite()) {
                    showErrorDialog("saveError.title", null, "saveError.write", target);
                    return;
                }
            }
            AudioFileFilter filter = (AudioFileFilter) chooser.getFileFilter();
            putValue(Preferences.FILE_FORMAT, filter.getExtension());
            for (int i = 0; i < msgInfo.length; i++) {
                try {
                    File file = null;
                    if (target.isDirectory()) {
                        file = new File(target, buildFilename(msgInfo[i]) + filter.getExtension());
                    } else {
                        file = target;
                    }
                    if (file.exists()) {
                        String text = Utils.safeMessageFormat(messageBundle.getString("overwriteConfirm.message"), file);
                        int decision = JOptionPane.showOptionDialog(VBoxGUI.this, text, messageBundle.getString("overwriteConfirm.title"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, overwriteOptions, overwriteOptions[1]);
                        if (decision != 0) {
                            continue;
                        }
                    }
                    PlayableMessage msg = new PlayableMessage(msgInfo[i], proxy);
                    msg.writeAudioData(new FileOutputStream(file), filter.getAudioFileType());
                } catch (Exception ex) {
                    ex.printStackTrace();
                    showErrorDialog("saveError.title", null, "saveError.message", ex);
                }
            }
        }
    };

    private XAction toggleAction = new XAction(messageBundle.getString("ToggleAction"), messageBundle.getString("ToggleTip")) {

        private static final long serialVersionUID = -7467622577709031304L;

        public void actionPerformed(final ActionEvent e) {
            int[] index = msgTable.getSelectedRows();
            for (int i = 0; i < index.length; i++) {
                try {
                    markMessageAsRead(index[i]);
                } catch (VBoxException ex) {
                    ex.printStackTrace();
                    showErrorDialog("toggleError.title", null, "toggleError.message", ex);
                }
            }
        }
    };

    private CopyCallerIdToClipboardAction copyCallerIdToClipboard = new CopyCallerIdToClipboardAction(Utils.getStringFromBundle(messageBundle, "CopyToClipboardAction"), Utils.getStringFromBundle(messageBundle, "CopyToClipboardTip"));

    private XAction deleteAction = new XAction(messageBundle.getString("DeleteAction"), messageBundle.getString("DeleteTip")) {

        private static final long serialVersionUID = -536218288758195114L;

        /** texts for buttons in delete dialog */
        final String[] deleteOptions = new String[] { messageBundle.getString("deleteButtonText"), messageBundle.getString("cancel") };

        public void actionPerformed(final ActionEvent e) {
            int[] index = msgTable.getSelectedRows();
            String text = Utils.safeMessageFormat(messageBundle.getString("deleteConfirm.message"), new Integer(index.length));
            int decision = JOptionPane.showOptionDialog(VBoxGUI.this, text, messageBundle.getString("deleteConfirm.title"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, deleteOptions, deleteOptions[1]);
            if (decision != 0) {
                return;
            }
            for (int i = index.length - 1; i >= 0; i--) {
                try {
                    MessageInfo msg = tableSorter.getMessageInfoAt(index[i]);
                    proxy.deleteMessage(msg.getFilename());
                    tableModel.removeMessageInfo(index[i]);
                } catch (VBoxException vbe) {
                    vbe.printStackTrace();
                    showErrorDialog("deleteError.title", null, "deleteError.message", vbe);
                }
            }
        }
    };

    private XAction helpAction = new XAction(messageBundle.getString("HelpAction"), messageBundle.getString("HelpTip")) {

        private static final long serialVersionUID = -7897741528067405364L;

        public void actionPerformed(final ActionEvent e) {
        }
    };

    private XAction aboutAction = new XAction(messageBundle.getString("AboutAction"), messageBundle.getString("AboutTip")) {

        private static final long serialVersionUID = -1795351683160310421L;

        private AboutBox aboutBox = null;

        public void actionPerformed(final ActionEvent e) {
            if (aboutBox == null) {
                aboutBox = new AboutBox((Frame) SwingUtilities.getRoot(VBoxGUI.this));
                aboutBox.pack();
                aboutBox.setLocationRelativeTo(VBoxGUI.this);
            }
            aboutBox.show();
        }
    };

    protected static ResourceBundle getMsgBundle() {
        return VBoxGUI.messageBundle;
    }
}
