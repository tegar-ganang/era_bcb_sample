package fiswidgets.fisdesktop;

import java.awt.*;
import java.awt.image.*;
import java.text.*;
import java.net.*;
import java.awt.event.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.util.*;
import java.io.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.lang.reflect.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import fiswidgets.fisgui.*;
import fiswidgets.fisutils.*;
import fiswidgets.fisremote.*;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.ComboBoxUI;
import java.rmi.*;
import java.util.regex.*;

/**
 * A FisDesktop is a meta fiswidget that consists of multiple fiswidgets that can be added and removed
 * from a desktop.  These fiswidgets then can be run in order or one at a time.  The fiswidgets will
 * be run one at a time, the next waiting for the previous to finish before begining.
 */
public class FisDesktop extends JFrame implements ActionListener, WindowListener, InternalFrameListener, MouseListener, MouseMotionListener, PropertyChangeListener {

    public static final String ForeachName = "Foreach";

    public static final String EndForeachName = "EndForeach";

    public static final String LogfileName = "SetLogName";

    public static final String AddComment = "AddLogComment";

    public static final String DefaultLogName = "Desk.log";

    public static final String BREAK = "<<Break>>";

    public static final String NO_BREAK = "<<No Break>>";

    private static final String removeAllName = "Remove All";

    private static final String defaultDescription = "Type your description here ~ ";

    private final String XML_desc = "XML serialized file (*.xml)";

    private final String OLD_desc = "Java serialized file (*.gui)";

    private final String XML_suff = ".xml";

    private final String OLD_suff = ".gui";

    private static final Pattern CONNECTION_TITLE = Pattern.compile("(?:\\s*)(.*)(?:\\s*)@(?:\\s*)(.*)");

    private static final Pattern XML_TITLE = Pattern.compile("(?:\\s*)(.*)(?:\\s*):(?:\\s*)(.*)");

    private static final Pattern CONNECTION_XML_TITLE = Pattern.compile("(?:\\s*)(.*)(?:\\s*)@(?:\\s*)(.*)(?:\\s*):(?:\\s*)(.*)");

    private final URL STOP_ICON = getClass().getResource("icons/Stop.gif");

    private String last_xml_file = null;

    private String last_xml_dir = System.getProperty("user.dir");

    private static final String POSTIT_TITLE = "Add/Remove PostIt notes";

    private DesktopRun runner;

    private FisFrame findReplaceDialog, cdDialog, editPostitsDialog, optionsDialog;

    private FisTextField searchText, replaceText, newCwd;

    private JTextField cwd;

    private FisCheckBox ignoreCase;

    private JTextArea postitText;

    private MouseAdapter mouseAdapter;

    private FisButton epAdd, epRemove;

    private Vector classes = new Vector();

    private JPanel userPanel = new JPanel();

    private JLabel flow;

    private GridBagLayout usergbl = new GridBagLayout();

    private GridBagConstraints usergbc = new GridBagConstraints();

    private JPanel buttonPanel = new JPanel();

    private JPanel deskPanel = new JPanel();

    private JDesktopPane desktop;

    private FlowOptions flowopt;

    private FisToggleOnOff onoff;

    private FisTextField filePath;

    private FisFrame logFrame;

    private JMenuBar menubar = new JMenuBar();

    private JMenu removeMenu, helpMenu, fileMenu, optionsMenu, addMenu;

    private int iconCount = 0;

    private Vector flowPosVec = new Vector();

    private Vector allMenuTops = new Vector();

    private Vector allMenus = new Vector();

    private boolean standalone = true;

    private JButton run;

    private Dimension screen;

    private JMenuItem about, removeAll;

    protected static boolean usingbreaks = false, logging = false, nogood = false, interruptonerror = false;

    protected static boolean runningdesk = false;

    private boolean match;

    protected static String logfilename = DefaultLogName;

    protected boolean loaded = false;

    private GridBagLayout buttongbl;

    private GridBagConstraints buttongbc;

    private Image image;

    private JCheckBoxMenuItem showPostit;

    private FisCheckBox interrupt, startIconified;

    private FisRadioButton singleWinOutput, multiWinOutput, stdOutput, noOutput;

    private String aboutmessage = (new FisBase()).getAboutMessage();

    private Color back = new Color(177, 182, 216);

    private Color front = new Color(71, 73, 84);

    private boolean postitLock = true;

    private final ComboBoxUI comboBoxUI = (new JComboBox()).getUI();

    private FisCheckBox use_cwd_find_replace;

    private FisScriptfileDialog scriptDialog;

    private boolean forLoad = false;

    private JTextArea preview;

    private JCheckBox append;

    private String old_cwd = System.getProperty("user.dir");

    public static FisServerButton serverFrame;

    private KeepAliveDialog keepAliveDialog;

    private CwdFrame cwdFrame;

    private Point p, m;

    private final int buttonWidth = (new JButton("Hi")).getPreferredSize().height + 5;

    /**
     * Constructs the FisDesktop to which the FisWidgets will be added
     */
    public FisDesktop() {
        super("FisWidget Desktop");
        addWindowListener(this);
        setJMenuBar(menubar);
        desktop = new JDesktopPane();
        desktop.setOpaque(false);
        Container pane = getContentPane();
        buttongbl = new GridBagLayout();
        buttongbc = new GridBagConstraints();
        pane.setLayout(new BorderLayout());
        userPanel.setLayout(usergbl);
        buttonPanel.setLayout(buttongbl);
        usergbc.insets = new Insets(3, 3, 2, 2);
        usergbc.gridx = 0;
        usergbc.gridy = 0;
        usergbc.fill = GridBagConstraints.BOTH;
        buttongbc.insets = new Insets(10, 10, 10, 10);
        buttongbc.gridx = 1;
        buttongbc.gridy = 0;
        runningdesk = true;
        desktop.putClientProperty("JDesktopPane.dragMode", "faster");
        BevelBorder border = new BevelBorder(BevelBorder.RAISED);
        removeMenu = new JMenu("Remove");
        ClassStrings cs1 = new ClassStrings("fiswidgets.fisdesktop." + ForeachName, ForeachName);
        classes.addElement(cs1);
        ClassStrings cs2 = new ClassStrings("fiswidgets.fisdesktop." + EndForeachName, EndForeachName);
        classes.addElement(cs2);
        ClassStrings cs3 = new ClassStrings("fiswidgets.fisdesktop." + LogfileName, LogfileName);
        classes.addElement(cs3);
        ClassStrings cs4 = new ClassStrings("fiswidgets.fisdesktop." + AddComment, AddComment);
        classes.addElement(cs4);
        fileMenu = new JMenu("File");
        fileMenu.addSeparator();
        JMenuItem exitmenu = new JMenuItem("Exit");
        exitmenu.addActionListener(this);
        fileMenu.add(exitmenu);
        optionsMenu = new JMenu("Options");
        JMenuItem breaks = new JMenuItem("Set Breakpoints...");
        breaks.setActionCommand("breaks");
        breaks.addActionListener(this);
        optionsMenu.add(breaks);
        JMenuItem keepalive = new JMenuItem("Keep Alive...");
        keepalive.addActionListener(this);
        keepalive.setActionCommand("keepalive");
        optionsMenu.add(keepalive);
        optionsMenu.add(new JSeparator());
        showPostit = new JCheckBoxMenuItem("Show PostIt notes");
        showPostit.addActionListener(this);
        showPostit.setActionCommand("showPostit");
        optionsMenu.add(showPostit);
        JMenuItem editPostit = new JMenuItem(POSTIT_TITLE + " ...");
        editPostit.addActionListener(this);
        editPostit.setActionCommand("editPostit");
        optionsMenu.add(editPostit);
        optionsMenu.add(new JSeparator());
        JMenuItem scripting = new JMenuItem("Scripting...");
        scripting.addActionListener(this);
        scripting.setActionCommand("scripting");
        optionsMenu.add(scripting);
        JMenuItem findreplace = new JMenuItem("Find/Replace...");
        findreplace.addActionListener(this);
        findreplace.setActionCommand("findreplace");
        optionsMenu.add(findreplace);
        JMenuItem cdmenu = new JMenuItem("Change Working Dir...");
        cdmenu.addActionListener(this);
        cdmenu.setActionCommand("cd");
        optionsMenu.add(cdmenu);
        optionsMenu.add(new JSeparator());
        JMenuItem foreach = new JMenuItem("Foreach loop");
        foreach.setActionCommand(ForeachName);
        foreach.addActionListener(this);
        optionsMenu.add(foreach);
        JMenuItem end_foreach = new JMenuItem("End foreach loop");
        end_foreach.setActionCommand(EndForeachName);
        end_foreach.addActionListener(this);
        optionsMenu.add(end_foreach);
        JMenuItem logfile = new JMenuItem("Set Logfile Name");
        logfile.setActionCommand(LogfileName);
        logfile.addActionListener(this);
        optionsMenu.add(logfile);
        JMenuItem addcomment = new JMenuItem("Add comment to logfile");
        addcomment.setActionCommand(AddComment);
        addcomment.addActionListener(this);
        optionsMenu.add(addcomment);
        optionsMenu.add(new JSeparator());
        JMenuItem options = new JMenuItem("Settings ...");
        options.addActionListener(this);
        options.setActionCommand("options");
        optionsMenu.add(options);
        removeAll = new JMenuItem(removeAllName);
        removeAll.setActionCommand(removeAllName);
        removeAll.addActionListener(this);
        removeMenu.add(removeAll);
        addMenu = new JMenu("Add");
        addMenu.setForeground(new Color(100, 106, 130));
        helpMenu = new JMenu("Help");
        JMenuItem helpitem = new JMenuItem("Help");
        helpitem.addActionListener(this);
        helpMenu.add(helpitem);
        about = new JMenuItem("About");
        about.addActionListener(this);
        helpMenu.add(new JSeparator());
        helpMenu.add(about);
        menubar.add(fileMenu);
        menubar.add(optionsMenu);
        menubar.add(addMenu);
        menubar.add(removeMenu);
        JButton stop = new JButton(new ImageIcon(STOP_ICON));
        stop.setToolTipText("Interrupt running flow");
        stop.setActionCommand("stopflow");
        stop.addActionListener(this);
        menubar.add(stop);
        menubar.add(Box.createHorizontalGlue());
        menubar.add(helpMenu);
        run = new JButton("Run");
        run.addActionListener(this);
        buttongbl.setConstraints(run, buttongbc);
        buttonPanel.add(run);
        buttongbc.gridx++;
        JButton log = new JButton("Log");
        log.addActionListener(this);
        buttongbl.setConstraints(log, buttongbc);
        buttonPanel.add(log);
        buttongbc.gridx++;
        serverFrame = new FisServerButton(this);
        JButton server = serverFrame.getServerButton();
        buttongbl.setConstraints(server, buttongbc);
        buttonPanel.add(server);
        buttongbc.gridx++;
        JButton save = new JButton("Save");
        save.addActionListener(this);
        buttongbl.setConstraints(save, buttongbc);
        buttonPanel.add(save);
        buttongbc.gridx++;
        JButton load = new JButton("Load");
        load.addActionListener(this);
        buttongbl.setConstraints(load, buttongbc);
        buttonPanel.add(load);
        buttonPanel.setBorder(border);
        buttongbc.gridx++;
        JButton close = new JButton("Close");
        close.addActionListener(this);
        buttongbl.setConstraints(close, buttongbc);
        buttonPanel.add(close);
        createLoggingDialog();
        usergbc.anchor = GridBagConstraints.CENTER;
        flow = new JLabel("     Flow     ");
        usergbl.setConstraints(flow, usergbc);
        userPanel.add(flow);
        usergbc.gridy++;
        userPanel.setBorder(border);
        JScrollPane scroll = new JScrollPane(userPanel);
        pane.add(scroll, "West");
        deskPanel.setLayout(new BorderLayout());
        deskPanel.add(desktop, "Center");
        pane.add(deskPanel, "Center");
        pane.add(buttonPanel, "South");
        cwdFrame = new CwdFrame(this);
        scriptDialog = new FisScriptfileDialog();
        createFindReplaceDialog();
        createKeepAliveDialog();
        createEditPostitsDialog();
        createOptionsDialog();
        Dimension s = Toolkit.getDefaultToolkit().getScreenSize();
        int width = (int) (s.width * 0.75);
        int height = (int) (s.height * 0.75);
        setSize(width, height);
        setLocation((s.width / 2) - (width / 2), (s.height / 2) - (height / 2));
        actionPerformed(new ActionEvent(new JButton(), 0, "OK"));
        mouseAdapter = new DesktopMouseAdapter();
    }

    /**
     * Center app is the mechanism to display a Dialog in the center of the Desktop.
     */
    private void centerDialog(Component comp) {
        Dimension ds = getSize();
        Dimension dt = comp.getSize();
        Point pt = new Point((ds.width / 2) - (dt.width / 2), (ds.height / 2) - (dt.height / 2));
        SwingUtilities.convertPointToScreen(pt, this);
        comp.setLocation(pt);
    }

    /**
     *  Create Logging Dialog
     */
    private void createLoggingDialog() {
        logFrame = new FisFrame();
        logFrame.setTitle("Logging information");
        filePath = new FisTextField("Logfile", DefaultLogName, logFrame);
        (new FisFileBrowser(logFrame)).attachTo(filePath);
        logFrame.setAppParent(this);
        logFrame.newLine();
        onoff = new FisToggleOnOff("Logging is:", true, logFrame);
        logFrame.newLine();
        logFrame.addBlank();
        FisPanel pane = new FisPanel();
        logFrame.addFisPanel(pane, 1, 1);
        FisButton okay = new FisButton("OK", pane);
        okay.addActionListener(this);
        FisButton logcancel = new FisButton("Cancel", pane);
        logcancel.addActionListener(this);
        logFrame.setStandAlone(false);
        logFrame.pack();
    }

    /**
     *  Create findReplaceDialog
     */
    private void createFindReplaceDialog() {
        findReplaceDialog = new FisFrame();
        searchText = new FisTextField("Search", "", findReplaceDialog);
        (new FisFileBrowser(findReplaceDialog)).attachTo(searchText);
        findReplaceDialog.newLine();
        replaceText = new FisTextField("Replace", "", findReplaceDialog);
        (new FisFileBrowser(findReplaceDialog)).attachTo(replaceText);
        findReplaceDialog.newLine();
        findReplaceDialog.addBlank();
        ignoreCase = new FisCheckBox("Case insensitive", false, findReplaceDialog);
        findReplaceDialog.newLine();
        findReplaceDialog.setAnchor(FisFrame.CENTER);
        findReplaceDialog.addBlank();
        FisPanel pane33 = new FisPanel();
        findReplaceDialog.addFisPanel(pane33, 1, 1);
        FisButton bt2 = new FisButton("Replace All", pane33);
        FisButton bt3 = new FisButton("Cancel", pane33);
        bt2.addActionListener(this);
        bt3.addActionListener(this);
        bt2.setActionCommand("frReplace");
        bt3.setActionCommand("frCancel");
        findReplaceDialog.setTitle("Find/Replace");
        findReplaceDialog.setStandAlone(false);
        findReplaceDialog.pack();
    }

    private void createKeepAliveDialog() {
        keepAliveDialog = new KeepAliveDialog(this);
    }

    /**
     *  Create Edit PostIt(s) Dialog
     */
    private void createEditPostitsDialog() {
        editPostitsDialog = new FisFrame();
        editPostitsDialog.setAnchor(FisFrame.CENTER);
        postitText = new FisTextArea(10, 40, editPostitsDialog);
        editPostitsDialog.newLine();
        FisPanel pane = new FisPanel();
        editPostitsDialog.addFisPanel(pane, 1, 1);
        epAdd = new FisButton("Add", pane);
        epRemove = new FisButton("Remove", pane);
        FisButton bt3 = new FisButton("Done", pane);
        epAdd.addActionListener(this);
        epRemove.addActionListener(this);
        bt3.addActionListener(this);
        epAdd.setActionCommand("epAdd");
        epRemove.setActionCommand("epRemove");
        bt3.setActionCommand("epDone");
        editPostitsDialog.setTitle(POSTIT_TITLE);
        editPostitsDialog.setStandAlone(false);
        editPostitsDialog.pack();
        mouseAdapter = new DesktopMouseAdapter();
    }

    /**
     *  Create Options dialog
     */
    private void createOptionsDialog() {
        optionsDialog = new FisFrame();
        interrupt = new FisCheckBox("Interrupt on errors", true, optionsDialog);
        optionsDialog.newLine();
        FisPanel pane1 = new FisPanel("Output control");
        optionsDialog.addFisPanel(pane1, 1, 1);
        singleWinOutput = new FisRadioButton("Single window output", true, pane1);
        singleWinOutput.addActionListener(this);
        pane1.newLine();
        multiWinOutput = new FisRadioButton("Multiple window output", false, pane1);
        multiWinOutput.addActionListener(this);
        pane1.newLine();
        stdOutput = new FisRadioButton("Output to the console", false, pane1);
        stdOutput.addActionListener(this);
        pane1.newLine();
        noOutput = new FisRadioButton("Do not generate output", false, pane1);
        noOutput.addActionListener(this);
        pane1.newLine();
        startIconified = new FisCheckBox("Start all windows iconified", true, pane1);
        FisRadioButtonGroup g = new FisRadioButtonGroup();
        g.add(singleWinOutput);
        g.add(multiWinOutput);
        g.add(stdOutput);
        g.add(noOutput);
        optionsDialog.newLine();
        FisPanel pane2 = new FisPanel();
        optionsDialog.addFisPanel(pane2, 1, 1);
        FisButton bt1 = new FisButton("OK", pane2);
        FisButton bt2 = new FisButton("Cancel", pane2);
        bt1.addActionListener(this);
        bt2.addActionListener(this);
        bt1.setActionCommand("opOK");
        bt2.setActionCommand("opCancel");
        optionsDialog.setTitle("Settings");
        optionsDialog.setStandAlone(false);
        optionsDialog.pack();
    }

    private int getWindowMode() {
        int mode = 0;
        if (singleWinOutput.isSelected()) mode = FisRunManager.DESKTOP_WINDOW; else if (multiWinOutput.isSelected()) mode = FisRunManager.SINGLE_WINDOW; else if (stdOutput.isSelected()) mode = FisRunManager.STD_OUTPUT; else if (noOutput.isSelected()) mode = FisRunManager.NO_OUTPUT;
        return mode;
    }

    private void setWindowMode(int mode) {
        if (mode > 3 || mode < 0) mode = FisRunManager.DESKTOP_WINDOW;
        multiWinOutput.setSelected(false);
        singleWinOutput.setSelected(false);
        stdOutput.setSelected(false);
        noOutput.setSelected(false);
        switch(mode) {
            case (FisRunManager.SINGLE_WINDOW):
                multiWinOutput.setSelected(true);
                break;
            case (FisRunManager.DESKTOP_WINDOW):
                singleWinOutput.setSelected(true);
                break;
            case (FisRunManager.STD_OUTPUT):
                stdOutput.setSelected(true);
                break;
            case (FisRunManager.NO_OUTPUT):
                noOutput.setSelected(true);
                break;
        }
    }

    /**
     *  This is to allow access to the desktop for making changes to the L&F
     */
    public JDesktopPane getDesktop() {
        return desktop;
    }

    /**
     *  Adds the given message to the about dialog.
     *  @param message the added information to be added to the about.
     */
    public void addToAbout(String message) {
        aboutmessage = message + "\n\n" + aboutmessage;
    }

    /**
     *  This allows the programmer to add an icon that will be shown to the left of the buttons
     *  on the button panel.
     * @param image This is an Image that will be used on the button panel. It will be pushed into a 
     *  32x32 area while preserving the aspect ratio.
     */
    public void addImageIcon(Image image) {
        this.image = image;
        ImageIcon tmp = new ImageIcon(image);
        ImageObserver io = tmp.getImageObserver();
        Image im;
        if (image.getHeight(io) > image.getWidth(io)) {
            im = createImage(image.getSource()).getScaledInstance(-32, 32, Image.SCALE_FAST);
        } else {
            im = createImage(image.getSource()).getScaledInstance(32, -32, Image.SCALE_FAST);
        }
        JLabel il = new JLabel(new ImageIcon(im));
        buttongbc.gridx = 0;
        buttongbc.anchor = GridBagConstraints.WEST;
        buttongbl.setConstraints(il, buttongbc);
        buttonPanel.add(il);
    }

    /**
     *  This allows the programmer to add an icon that will be shown to the left of the buttons
     *  on the button panel.
     * @param image This is an string of what image will be used on the button panel, it will be 
     *  pushed into a 32x32 area while preserving the aspect ratio.
     */
    public void addImageIcon(String imageString) {
        ImageIcon ic = new ImageIcon(imageString);
        addImageIcon(ic.getImage());
    }

    /**
     *  This is used by the about message dialog to get the about message for the fisapp
     */
    public String getAboutMessage() {
        return aboutmessage;
    }

    /**
     * This can be used to place your own menuitem into the help menu.
     * @param helpItem a JMenuItem to be used as the Help menu item.
     */
    public void setHelpItem(JMenuItem helpItem) {
        helpMenu.removeAll();
        helpMenu.add(helpItem);
        helpMenu.add(new JSeparator());
        helpMenu.add(about);
    }

    /**
     * addPulloverMenu adds a pull over menu to the Add menu of the FisDesktop, by default the menu items
     * are added top to buttom.
     * @param menu The menu to be added as a pullover menu.
     */
    public void addPulloverMenu(String menu) {
        JMenu newmenu = new JMenu(menu);
        allMenus.addElement(newmenu);
        addMenu.add(newmenu);
    }

    /**
     * addWidget adds a FisWidget to the FisDesktop.  This will put the menuTitle into the Add menu of
     * the FisDesktop.
     * @param className this is the class name of the FisWidget to be added.  This FisWidget must exist in your CLASSPATH.
     * @param menuTitle this is the Sring that will appear in the menu of the FisDesktop.
     */
    public void addWidget(String className, String menuTitle) {
        addWidget(className, menuTitle, null);
    }

    /**
    * addWidget adds a FisWidget to the FisDesktop.  This will put the menuTitle into the Add menu of
    * the FisDesktop. This also adds a tool tip to the widget.
    * @param className this is the class name of the FisWidget to be added.  This FisWidget must exist in your CLASSPATH.
    * @param menuTitle this is the Sring that will appear in the menu of the FisDesktop.
    * @param tooltip is the String that will be used as a tool tip for the widget
    */
    public void addWidget(String className, String menuTitle, String tooltip) {
        JMenuItem prog = new JMenuItem(menuTitle);
        if (null != tooltip) {
            prog.setToolTipText(tooltip);
        }
        ClassStrings cs = new ClassStrings(className, menuTitle);
        classes.addElement(cs);
        prog.setActionCommand("add");
        prog.addActionListener(this);
        addMenu.add(prog);
    }

    /**
     * This is a special case addWidget which will add the FisWidget to the FisDesktop and add the 
     * menuTitle to the pull over menu specified by whichmenu.  This also adds a tool tip to the widget.
     * @param className this is the class name of the FisWidget to be added.  This FisWidget must exist in your CLASSPATH.
     * @param menuTitle this is the Sring that will appear in the menu of the FisDesktop.
     * @param whichMenuItem this is the pull over menu that you want the menuTitle to be added to under the Add menu on the FisDesktop.
     * @param tooltip is the String that will be used as a tool tip for the widget
     */
    public void addWidget(String className, String menuTitle, String whichMenuItem, String tooltip) {
        JMenuItem prog = new JMenuItem(menuTitle);
        prog.setToolTipText(tooltip);
        ClassStrings cs = new ClassStrings(className, menuTitle);
        classes.addElement(cs);
        prog.setActionCommand("add");
        prog.addActionListener(this);
        boolean gotIt = false;
        JMenu test;
        for (int i = 0; i < allMenus.size(); i++) {
            test = (JMenu) allMenus.elementAt(i);
            if ((test.getText()).equals(whichMenuItem)) {
                gotIt = true;
                test.add(prog);
                break;
            }
        }
        if (!gotIt) addMenu.add(prog);
    }

    public void addWidgetMenu(String menuName) {
        addWidgetMenu(menuName, null);
    }

    public void addWidgetMenu(String menuName, String tooltip) {
        menubar.removeAll();
        JMenu menu = new JMenu(menuName);
        if (null != tooltip) {
            menu.setToolTipText(tooltip);
        }
        allMenuTops.addElement(menu);
        menubar.add(fileMenu);
        menubar.add(optionsMenu);
        for (int i = 0; i < allMenuTops.size(); i++) menubar.add((JMenu) allMenuTops.elementAt(i));
        menubar.add(removeMenu);
        JButton stop = new JButton(new ImageIcon(STOP_ICON));
        stop.setToolTipText("Interrupt running flow");
        stop.setActionCommand("stopflow");
        stop.addActionListener(this);
        menubar.add(stop);
        menubar.add(Box.createHorizontalGlue());
        menubar.add(helpMenu);
        addMenu = menu;
    }

    /**
     * this method is to get userPanel
     */
    public Vector getFlowPositionVector() {
        return flowPosVec;
    }

    /**
     *  this method is to get an array of FisBases
     */
    public FisBase[] getFisBases() {
        FisBase[] bases = new FisBase[flowPosVec.size()];
        for (int i = 0; i < flowPosVec.size(); i++) bases[i] = ((FlowPosition) flowPosVec.elementAt(i)).desktopBase.base;
        return bases;
    }

    /**
     *  this method is to get an array of Titles
     */
    public String[] getTitles() {
        String[] titles = new String[flowPosVec.size()];
        for (int i = 0; i < flowPosVec.size(); i++) titles[i] = ((FlowPosition) flowPosVec.elementAt(i)).title;
        return titles;
    }

    /**
     *  Return DesktopInfo object that describes the stuff
     *  that normally would not serialize
     */
    public DesktopInfo getDesktopInfo() {
        DesktopInfo info = new DesktopInfo();
        info.description = getComment(preview.getText());
        info.logging = logging;
        info.usingBreaks = usingbreaks;
        info.interruptOnErrors = interruptonerror;
        info.logFile = logfilename;
        info.winMode = getWindowMode();
        info.minimized = startIconified.isChecked();
        info.use_remote = serverFrame.getServerState();
        info.remote_server = serverFrame.getServer();
        info.remote_user = serverFrame.getUsername();
        info.breakOn = new boolean[flowPosVec.size()];
        info.visible = new boolean[flowPosVec.size()];
        info.apps = new AppInfo[flowPosVec.size()];
        for (int i = 0; i < info.breakOn.length; i++) {
            info.breakOn[i] = ((FlowPosition) flowPosVec.elementAt(i)).breakon;
            info.visible[i] = ((FlowPosition) flowPosVec.elementAt(i)).internalFrame.isVisible();
            info.apps[i] = ((FlowPosition) flowPosVec.elementAt(i)).desktopBase.base.getAppInfo();
        }
        if (info.remote_server == null) info.remote_server = "localhost";
        if (info.remote_user == null) info.remote_user = System.getProperty("user.dir");
        info.script_info = scriptDialog.getInfo();
        return info;
    }

    /**
    *  set DesktopInfo object that describes the stuff
    *  that normally would not serialize
    */
    public void setDesktopInfo(DesktopInfo info) {
        logging = info.logging;
        usingbreaks = info.usingBreaks;
        interruptonerror = info.interruptOnErrors;
        setWindowMode(info.winMode);
        logfilename = info.logFile;
        startIconified.setSelected(info.minimized);
        serverFrame.setServerState(info.use_remote);
        serverFrame.setServer(info.remote_server);
        serverFrame.setUsername(info.remote_user);
        int oic = iconCount - info.breakOn.length;
        for (int i = 0; i < info.breakOn.length; i++) {
            ((FlowPosition) flowPosVec.elementAt(i + oic)).breakon = info.breakOn[i];
            ((FlowPosition) flowPosVec.elementAt(i + oic)).desktopBase.base.setAppInfo(info.apps[i]);
            if (info.visible[i]) ((FlowPosition) flowPosVec.elementAt(i + oic)).button.doClick();
        }
        filePath.setText(logfilename);
        onoff.setSelected(logging);
        interrupt.setSelected(interruptonerror);
        scriptDialog.setInfo(info.script_info);
    }

    public void setStandAlone(boolean alone) {
        standalone = alone;
    }

    protected void setRunEnabled(boolean value) {
        run.setEnabled(value);
    }

    /**
     * this method goes through open widgets and finds/replaces
     * relevant fields
     */
    private void findReplace(String find, String replace, boolean ignoreCase) {
        for (int i = 0; i < flowPosVec.size(); i++) {
            FisBase b = ((FlowPosition) flowPosVec.elementAt(i)).desktopBase.base;
            doReplace(find, replace, b.getComponentPanel().getComponents(), null, null, ignoreCase);
        }
    }

    /**
     * do replace 
     */
    protected void doReplace(String find, String replace, Component[] comps, Vector comp, Vector text, boolean ignoreCase) {
        for (int i = 0; i < comps.length; i++) {
            if (comps[i] instanceof JPanel) {
                JPanel pane = (JPanel) comps[i];
                doReplace(find, replace, pane.getComponents(), comp, text, ignoreCase);
            } else if (comps[i] instanceof JScrollPane) {
                JScrollPane pane = (JScrollPane) comps[i];
                doReplace(find, replace, pane.getComponents(), comp, text, ignoreCase);
            } else if (comps[i] instanceof JViewport) {
                JViewport port = (JViewport) comps[i];
                doReplace(find, replace, port.getComponents(), comp, text, ignoreCase);
            } else if (comps[i] instanceof FisAdvancedButton) {
                FisAdvancedButton ab = (FisAdvancedButton) comps[i];
                FisFrame ff = ab.getFisFrame();
                JPanel cp = ff.getComponentPanel();
                doReplace(find, replace, cp.getComponents(), comp, text, ignoreCase);
            } else if (comps[i] instanceof JTabbedPane) {
                JTabbedPane tab = (JTabbedPane) comps[i];
                int tabcount = tab.getTabCount();
                for (int j = 0; j < tabcount; j++) {
                    JPanel pane = (JPanel) tab.getComponentAt(j);
                    doReplace(find, replace, pane.getComponents(), comp, text, ignoreCase);
                }
            } else if (comps[i] instanceof JTextField) {
                JTextField f = (JTextField) comps[i];
                String orig = f.getText();
                if (comp != null) comp.addElement(f);
                if (text != null) text.addElement(orig);
                if (find.length() > 0) {
                    for (int offset = 0; offset < orig.length(); offset++) if (orig.regionMatches(ignoreCase, offset, find, 0, find.length())) {
                        orig = orig.substring(0, offset) + replace + orig.substring(offset + find.length());
                        offset += replace.length() - 1;
                    }
                    f.setText(orig);
                }
            } else if (comps[i] instanceof JComboBox && ((JComboBox) comps[i]).isEditable()) {
                JComboBox f = (JComboBox) comps[i];
                DefaultComboBoxModel oldmodel = (DefaultComboBoxModel) f.getModel();
                DefaultComboBoxModel model = new DefaultComboBoxModel();
                if (f.isEditable() && oldmodel.getSize() < 2) {
                    Object sel = f.getEditor().getItem();
                    oldmodel.removeAllElements();
                    oldmodel.addElement(sel);
                }
                if (comp != null) comp.addElement(f);
                if (text != null) text.addElement(oldmodel);
                if (find.length() > 0) {
                    for (int j = 0; j < oldmodel.getSize(); j++) {
                        String orig = oldmodel.getElementAt(j).toString();
                        for (int offset = 0; offset < orig.length(); offset++) if (orig.regionMatches(ignoreCase, offset, find, 0, find.length())) {
                            orig = orig.substring(0, offset) + replace + orig.substring(offset + find.length());
                            offset += replace.length() - 1;
                        }
                        model.addElement(orig);
                    }
                    f.setModel(model);
                }
            } else if (comps[i] instanceof FisListBox && ((FisListBox) comps[i]).isEditable()) {
                FisListBox list = (FisListBox) comps[i];
                DefaultListModel oldmodel = (DefaultListModel) list.getModel();
                DefaultListModel model = new DefaultListModel();
                if (comp != null) comp.addElement(list);
                if (text != null) text.addElement(oldmodel);
                if (find.length() > 0) {
                    for (int j = 0; j < oldmodel.getSize(); j++) {
                        String orig = oldmodel.getElementAt(j).toString();
                        for (int offset = 0; offset < orig.length(); offset++) if (orig.regionMatches(ignoreCase, offset, find, 0, find.length())) {
                            orig = orig.substring(0, offset) + replace + orig.substring(offset + find.length());
                            offset += replace.length() - 1;
                        }
                        model.addElement(orig);
                    }
                    list.setModel(model);
                }
            } else if (comps[i] instanceof JTextArea && ((JTextArea) comps[i]).isEditable()) {
                JTextArea f = (JTextArea) comps[i];
                String orig = f.getText();
                if (comp != null) comp.addElement(f);
                if (text != null) text.addElement(orig);
                if (find.length() > 0) {
                    for (int offset = 0; offset < orig.length(); offset++) if (orig.regionMatches(ignoreCase, offset, find, 0, find.length())) {
                        orig = orig.substring(0, offset) + replace + orig.substring(offset + find.length());
                        offset += replace.length() - 1;
                    }
                    f.setText(orig);
                }
            }
        }
    }

    public void actionPerformed(ActionEvent e) {
        String action = e.getActionCommand();
        if (e.getSource() instanceof FisRadioButton) {
            startIconified.setEnabled(singleWinOutput.isSelected());
        } else if (action.equals("Close") || action.equals("Exit")) {
            if (standalone) System.exit(0); else dispose();
        } else if (action.equals("cd")) {
            centerDialog(cwdFrame);
            cwdFrame.setVisible(true);
        } else if (action.equals("findreplace")) {
            centerDialog(findReplaceDialog);
            findReplaceDialog.setVisible(true);
        } else if (action.equals("frCancel")) {
            findReplaceDialog.setVisible(false);
        } else if (action.equals("frReplace")) {
            findReplace(searchText.getText(), replaceText.getText(), ignoreCase.isChecked());
            findReplaceDialog.setVisible(false);
        } else if (action.equals("breaks")) {
            flowopt = new FlowOptions(this);
        } else if (action.equals("stopflow")) {
            if (runner != null && runner.isRunning()) {
                runner.stopFlow();
                Dialogs.ShowNonModalMessageDialog(this, "The flow will stop its execution as soon as currently running application terminates", "Pausing running flow...");
            } else Dialogs.ShowErrorDialog(this, "Flow is not currently running");
        } else if (action.equals("options")) {
            centerDialog(optionsDialog);
            optionsDialog.setVisible(true);
        } else if (action.equals("opOK")) {
            optionsDialog.setVisible(false);
        } else if (action.equals("opCancel")) {
            optionsDialog.setVisible(false);
        } else if (action.equals("showPostit")) {
            showPostits();
        } else if (action.equals("editPostit")) {
            editPostitsDialog.setVisible(true);
            showPostit.setSelected(true);
            showPostits();
        } else if (action.equals("epDone")) {
            editPostitsDialog.setVisible(false);
            epAdd.setEnabled(true);
            epRemove.setEnabled(true);
            if (!postitLock) {
                JInternalFrame[] frames = desktop.getAllFrames();
                for (int i = 0; i < frames.length; i++) {
                    frames[i].getJMenuBar().setCursor(Cursor.getDefaultCursor());
                    frames[i].getContentPane().setCursor(Cursor.getDefaultCursor());
                    frames[i].getJMenuBar().removeMouseListener(mouseAdapter);
                    registerComponents(frames[i].getContentPane().getComponents(), false);
                }
                FisPostit fake = new FisPostit(null, "Fake");
                mouseAdapter.mouseClicked(new MouseEvent(fake, 0, 0, 0, 0, 0, 1, false));
                fake = null;
                postitLock = true;
            }
        } else if (action.equals("epAdd")) {
            if (postitLock) {
                JInternalFrame[] frames = desktop.getAllFrames();
                for (int i = 0; i < frames.length; i++) {
                    frames[i].getContentPane().setCursor(new Cursor(Cursor.HAND_CURSOR));
                    frames[i].getJMenuBar().setCursor(new Cursor(Cursor.HAND_CURSOR));
                    frames[i].getJMenuBar().addMouseListener(mouseAdapter);
                    registerComponents(frames[i].getContentPane().getComponents(), true);
                }
                postitLock = false;
                epRemove.setEnabled(false);
            }
        } else if (action.equals("epRemove")) {
            if (postitLock) {
                for (int i = 0; i < flowPosVec.size(); i++) {
                    FlowPosition app = (FlowPosition) flowPosVec.elementAt(i);
                    if (app.internalFrame.isVisible()) {
                        Vector postits = app.desktopBase.base.getPostitVector();
                        for (int j = 0; j < postits.size(); j++) {
                            FisPostit post = (FisPostit) postits.elementAt(j);
                            post.addMouseListener(mouseAdapter);
                            post.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
                        }
                    }
                }
                postitLock = false;
                epAdd.setEnabled(false);
            }
        } else if (action.equals("Help")) {
            FisProperties props = new FisProperties();
            try {
                props.loadProperties();
            } catch (Exception ex) {
                return;
            }
            if (!props.hasProperty("BROWSER") || !props.hasProperty("FISDOC_PATH")) return;
            String browser = System.getProperty("BROWSER");
            String docpath = System.getProperty("FISDOC_PATH");
            try {
                Runtime.getRuntime().exec(browser + " " + docpath + "/desktop.html");
            } catch (Exception ex) {
                return;
            }
        } else if (action.equals("Run")) {
            run.setEnabled(false);
            interruptonerror = interrupt.isSelected();
            runner = new DesktopRun(this, getWindowMode(), startIconified.isChecked());
        } else if (action.equals("Load")) {
            loadFlow();
        } else if (action.equals("Save")) {
            saveFlow();
        } else if (action.equals("Cancel")) {
            logFrame.dispose();
        } else if (action.equals("OK")) {
            logging = onoff.getSelected();
            logfilename = filePath.getText();
            logFrame.dispose();
        } else if (action.equals("Log")) {
            centerDialog(logFrame);
            logFrame.setVisible(true);
        } else if (action.equals("add")) {
            String temp = ((JMenuItem) e.getSource()).getText();
            addInternalFrame(temp);
        } else if (action.equals(ForeachName)) {
            addInternalFrame(ForeachName);
        } else if (action.equals(EndForeachName)) {
            addInternalFrame(EndForeachName);
        } else if (action.equals(LogfileName)) {
            addInternalFrame(LogfileName);
        } else if (action.equals(AddComment)) {
            addInternalFrame(AddComment);
        } else if (action.equals("remove")) {
            String temp = ((JMenuItem) e.getSource()).getText();
            removeInternalFrame(temp);
        } else if (action.equals(removeAllName)) {
            desktop.removeAll();
            removeMenu.removeAll();
            flowPosVec.removeAllElements();
            userPanel.removeAll();
            iconCount = 0;
            updateButtonsAndFrames();
            String[] title_strings = getTitleElements(getTitle());
            if (title_strings[1] == null) {
                setTitle(title_strings[0]);
            } else {
                setTitle(title_strings[0] + " @ " + title_strings[1]);
            }
        } else if (action.equals("panelButton")) {
            String temp = ((JButton) e.getSource()).getText();
            FlowPosition app;
            int index;
            try {
                index = Integer.parseInt((new StringTokenizer(temp, " ")).nextToken()) - 1;
                app = (FlowPosition) flowPosVec.elementAt(index);
            } catch (Exception ex) {
                System.out.println("Couldn't get an app index\nYou shouldn't see this");
                return;
            }
            if (app.internalFrame.isVisible()) {
                app.internalFrame.setVisible(false);
                ((JButton) e.getSource()).setForeground(Color.black);
                ((JButton) e.getSource()).setBackground((app.running) ? Color.green.darker() : back);
            } else {
                app.internalFrame.setVisible(true);
                ((JButton) e.getSource()).setForeground(Color.white);
                ((JButton) e.getSource()).setBackground(front);
            }
            userPanel.repaint();
            if (showPostit.isSelected()) app.desktopBase.base.displayPostits(true);
        } else if (action.equals("About")) {
            AboutDialog ad = new AboutDialog(this);
        } else if (action.equals("scripting")) {
            centerDialog(scriptDialog);
            scriptDialog.setVisible(true);
        } else if (action.equals("keepalive")) {
            centerDialog(keepAliveDialog);
            keepAliveDialog.setVisible(true);
        }
    }

    /**
     * Saves the flow from file
     */
    private void saveFlow() {
        boolean overwrite = true;
        forLoad = false;
        JFileChooser chooser = new JFileChooser();
        chooser.addChoosableFileFilter(new FisFileFilter(OLD_suff, OLD_desc));
        chooser.addChoosableFileFilter(new FisFileFilter(XML_suff, XML_desc));
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        chooser.setCurrentDirectory(new File(last_xml_dir));
        chooser.setAccessory(buildPreview());
        chooser.addPropertyChangeListener(this);
        if (last_xml_file != null) {
            File tmp_file = new File(last_xml_file);
            chooser.setSelectedFile(tmp_file);
            setupPreview(tmp_file);
        }
        int r = chooser.showSaveDialog(this);
        if (r == JFileChooser.APPROVE_OPTION) {
            String t = chooser.getSelectedFile().getAbsolutePath();
            setXMLDirectory(t);
            if (!chooser.getFileFilter().getDescription().equals(OLD_desc)) {
                if (!t.endsWith(XML_suff)) t = t + XML_suff;
                if ((new File(t)).exists()) {
                    File file = new File(t);
                    if (file != null) {
                        Document document;
                        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                        try {
                            DocumentBuilder builder = factory.newDocumentBuilder();
                            builder.setErrorHandler(new XmlErrorHandler());
                            builder.setEntityResolver(new XmlEntityResolver());
                            document = builder.parse(file);
                            Element element = document.getDocumentElement();
                            if (!(getClass().getName().equals(element.getAttribute("name").trim()))) {
                                JOptionPane.showMessageDialog(this, "This XML file is not from GlobalDesktop", "Warning", JOptionPane.INFORMATION_MESSAGE);
                            }
                        } catch (Exception ex) {
                        }
                        document = null;
                        factory = null;
                        int ans = JOptionPane.showConfirmDialog(this, "The file already exists, overwrite?", "Are you sure?", JOptionPane.YES_NO_OPTION);
                        if (ans == JOptionPane.YES_OPTION) {
                            overwrite = true;
                            try {
                                copyFile(t, t + "~");
                            } catch (IOException e) {
                                int err_ans = JOptionPane.showConfirmDialog(this, "Can't create backup file " + t + "~" + "\nDo you wish to continue anyways?", "Warning", JOptionPane.YES_NO_OPTION);
                                if (err_ans != JOptionPane.YES_OPTION) {
                                    overwrite = false;
                                }
                            }
                        } else {
                            overwrite = false;
                        }
                    }
                }
                if (overwrite) {
                    XmlSerializationFactory sf = new XmlSerializationFactory(this, t);
                    try {
                        sf.saveDesktop();
                        sf = null;
                    } catch (Exception ex) {
                        Dialogs.ShowErrorDialog(this, "Error saving file!\n" + ex.getMessage());
                        if (System.getProperty("DEBUG") != null) ex.printStackTrace();
                        return;
                    }
                    Dialogs.ShowMessageDialog(this, "Desktop saved successfully", "Done");
                    last_xml_file = t;
                    String[] title_strings = getTitleElements(getTitle());
                    if (title_strings[1] == null) {
                        setTitle(title_strings[0] + " : " + t);
                    } else {
                        setTitle(title_strings[0] + " @ " + title_strings[1] + " : " + t);
                    }
                }
            } else {
                if (!t.endsWith(OLD_suff)) t = t + OLD_suff;
                if ((new File(t)).exists()) {
                    int ans = JOptionPane.showConfirmDialog(this, "The file already exists, overwrite?", "Are you sure?", JOptionPane.YES_NO_OPTION);
                    if (ans == JOptionPane.YES_OPTION) overwrite = true; else overwrite = false;
                }
                if (overwrite) {
                    Vector bases = new Vector(flowPosVec.size());
                    Vector titles = new Vector(flowPosVec.size());
                    for (int i = 0; i < flowPosVec.size(); i++) {
                        bases.addElement(((FlowPosition) flowPosVec.elementAt(i)).desktopBase.base);
                        titles.addElement(((FlowPosition) flowPosVec.elementAt(i)).title);
                    }
                    SerializationFactory sf = new SerializationFactory(t);
                    try {
                        sf.saveDesktop(bases, titles);
                        sf = null;
                    } catch (Exception ex) {
                        Dialogs.ShowErrorDialog(this, "Error saving file!");
                        return;
                    }
                    Dialogs.ShowMessageDialog(this, "Desktop saved successfully", "Done");
                }
            }
        }
    }

    private void copyFile(String input_file, String output_file) throws IOException {
        BufferedReader lr = new BufferedReader(new FileReader(input_file));
        BufferedWriter lw = new BufferedWriter(new FileWriter(output_file));
        String line;
        while ((line = lr.readLine()) != null) {
            lw.write(line, 0, line.length());
            lw.newLine();
        }
        lr.close();
        lw.flush();
        lw.close();
    }

    /**
     * Sets the last_xml_dir so that the JFileChooser will start
     * correct directory.
     */
    private void setXMLDirectory(String file) {
        File tmp = new File(file);
        String tmp_parent = tmp.getParent();
        last_xml_dir = (tmp_parent != null) ? tmp_parent : System.getProperty("user.dir");
    }

    /**
     * Loads the flow from file
     */
    private void loadFlow() {
        forLoad = true;
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File(last_xml_dir));
        if (last_xml_file != null) {
            File tmp_file = new File(last_xml_file);
            chooser.setSelectedFile(tmp_file);
            setupPreview(tmp_file);
        }
        chooser.addChoosableFileFilter(new FisFileFilter(OLD_suff, OLD_desc));
        chooser.addChoosableFileFilter(new FisFileFilter(XML_suff, XML_desc));
        chooser.setAccessory(buildPreview());
        chooser.addPropertyChangeListener(this);
        int r = chooser.showOpenDialog(this);
        if (r == JFileChooser.APPROVE_OPTION) {
            loadFlow(chooser.getSelectedFile().getAbsolutePath(), chooser.getFileFilter().getDescription(), append.isSelected());
        } else loaded = false;
    }

    /**
     * This method can be used by GlobalDesktop to load a flow as an argument
     */
    public void loadFlow(String file) {
        String description;
        if (file.endsWith(OLD_suff)) description = OLD_desc; else description = XML_desc;
        match = true;
        loadFlow(file, description, false);
    }

    private void loadFlow(String file, String description, boolean append) {
        if (!append) {
            desktop.removeAll();
            removeMenu.removeAll();
            flowPosVec.removeAllElements();
            userPanel.removeAll();
            iconCount = 0;
            updateButtonsAndFrames();
        }
        int oic = iconCount;
        if (!description.equals(OLD_desc)) {
            if (!match && (JOptionPane.showConfirmDialog(this, "The XML file you selected is from different application\n" + "Are you sure you want to continue?", "Are you sure?", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION)) {
                return;
            }
            setXMLDirectory(file);
            XmlSerializationFactory sf = new XmlSerializationFactory(this, file);
            try {
                String[] titles = sf.loadTitles();
                for (int i = 0; i < titles.length; i++) {
                    addInternalFrame(titles[i]);
                }
                FisBase[] bases = new FisBase[titles.length];
                for (int i = 0, j = oic; i < bases.length; i++) {
                    FlowPosition pos = (FlowPosition) flowPosVec.elementAt(j);
                    if (pos.title.equals(titles[i])) {
                        bases[i] = pos.desktopBase.base;
                        j++;
                    }
                }
                sf.loadDesktop(bases);
                setDesktopInfo(sf.loadDesktopInfo());
                sf = null;
                loaded = true;
                actionPerformed(new ActionEvent(new JButton(), 0, "OK"));
                String[] title_strings = getTitleElements(getTitle());
                if (title_strings[1] == null) {
                    setTitle(title_strings[0] + " : " + file);
                } else {
                    setTitle(title_strings[0] + " @ " + title_strings[1] + " : " + file);
                }
                last_xml_file = file;
            } catch (NullPointerException ex) {
                Dialogs.ShowErrorDialog(this, "Error loading file " + file + "!\nApplication version mismatch or corrupt XML file");
                if (System.getProperty("DEBUG") != null) ex.printStackTrace();
                loaded = false;
            } catch (Exception ex) {
                Dialogs.ShowErrorDialog(this, "Error loading file " + file + "!\n" + ex.getMessage());
                if (System.getProperty("DEBUG") != null) ex.printStackTrace();
                loaded = false;
            }
        } else {
            if (!(new File(file)).canRead()) {
                Dialogs.ShowErrorDialog(this, "Error loading file " + file + "!\nFile either does not exist or cannot be read.");
                return;
            }
            SerializationFactory sf = new SerializationFactory(file);
            try {
                Vector titles = sf.loadTitles();
                String temp;
                for (int i = 0; i < titles.size(); i++) addInternalFrame(titles.elementAt(i).toString());
                Vector bases = new Vector();
                for (int i = oic; i < flowPosVec.size(); i++) {
                    bases.addElement(((FlowPosition) flowPosVec.elementAt(i)).desktopBase.base);
                }
                sf.loadDesktop(bases);
                sf = null;
                loaded = true;
                actionPerformed(new ActionEvent(new JButton(), 0, "OK"));
            } catch (Exception ex) {
                Dialogs.ShowErrorDialog(this, "Error loading file" + file + "!\nApplication version mismatch or corrupt GUI file");
                loaded = false;
            }
        }
    }

    private String[] getTitleElements(String title) {
        Matcher tmp1 = CONNECTION_XML_TITLE.matcher(getTitle());
        Matcher tmp2 = XML_TITLE.matcher(getTitle());
        Matcher tmp3 = CONNECTION_TITLE.matcher(getTitle());
        String[] tmp_strings = new String[3];
        if (tmp1.matches()) {
            tmp_strings[0] = tmp1.group(1);
            tmp_strings[1] = tmp1.group(2);
            tmp_strings[2] = tmp1.group(3);
        } else if (tmp2.matches()) {
            tmp_strings[0] = tmp2.group(1);
            tmp_strings[1] = null;
            tmp_strings[2] = tmp2.group(2);
        } else if (tmp3.matches()) {
            tmp_strings[0] = tmp3.group(1);
            tmp_strings[1] = tmp3.group(2);
            tmp_strings[2] = null;
        } else {
            tmp_strings[0] = title;
            tmp_strings[1] = null;
            tmp_strings[2] = null;
        }
        return tmp_strings;
    }

    /**
     *  This is the preview panel for the Desktop
     */
    private JPanel buildPreview() {
        JPanel pane = new JPanel();
        pane.setLayout(new BorderLayout());
        pane.setAlignmentX(Container.CENTER_ALIGNMENT);
        pane.setBorder(new BevelBorder(BevelBorder.LOWERED));
        preview = new JTextArea(9, 12);
        preview.setLineWrap(true);
        preview.setFont(new Font(getFont().getName(), Font.PLAIN, getFont().getSize() - 1));
        pane.add(preview, BorderLayout.CENTER);
        if (forLoad) {
            append = new JCheckBox("append to flow");
            pane.add(append, BorderLayout.SOUTH);
        } else {
            preview.setText(defaultDescription);
            preview.addMouseListener(new MouseAdapter() {

                public void mouseClicked(MouseEvent e) {
                    preview.setText(getComment(preview.getText()));
                }
            });
        }
        return pane;
    }

    /**
    * This method removes the header and returns the comment section of the string.
    */
    private String getComment(String comment) {
        StringTokenizer tok = new StringTokenizer(comment, "~");
        if (tok.countTokens() < 2) {
            return comment;
        }
        tok.nextToken();
        String text;
        if (tok.hasMoreTokens()) text = tok.nextToken().trim(); else text = "";
        return text;
    }

    /**
     * Property listener for the JFileChooser save/load preview
     */
    private void setupPreview(File file) {
        if (file != null) {
            if (forLoad) preview.setText("loading...");
            Document document;
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            try {
                DocumentBuilder builder = factory.newDocumentBuilder();
                builder.setErrorHandler(new XmlErrorHandler());
                builder.setEntityResolver(new XmlEntityResolver());
                document = builder.parse(file);
                preview.setText("");
                Element element = document.getDocumentElement();
                preview.append(element.getAttribute("name") + "\n");
                preview.append(element.getAttribute("date") + "\n");
                preview.append(element.getAttribute("user") + "\n");
                preview.append(element.getAttribute("platform") + "\n~\n");
                preview.append(element.getAttribute("postit"));
                match = getClass().getName().equals(element.getAttribute("name").trim());
            } catch (Exception ex) {
                if (forLoad) preview.setText("no preview available");
            }
            document = null;
            factory = null;
        } else if (forLoad) preview.setText("no preview available");
    }

    public void propertyChange(PropertyChangeEvent e) {
        String prop = e.getPropertyName();
        if (prop.equals(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY)) {
            setupPreview((File) e.getNewValue());
        }
    }

    /**
     * Display PostIts Notes
     */
    private void showPostits() {
        int numberOfPostits = 0;
        for (int i = 0; i < flowPosVec.size(); i++) {
            FlowPosition app = (FlowPosition) flowPosVec.elementAt(i);
            app.desktopBase.base.displayPostits(showPostit.isSelected());
            numberOfPostits += app.desktopBase.base.getPostitVector().size();
        }
        if (showPostit.isSelected() && !editPostitsDialog.isVisible() && numberOfPostits == 0) Dialogs.ShowMessageDialog(this, "<html><b>No PostIt notes were found!</b>\n" + "<html>This usually happens when the flow has been created, but\n" + "<html>PostIt notes have not been added yet. To add PostIt notes\n" + "<html>select <i>Edit PostIt notes</i> from the <i>Options menu</i>.\n" + "<html>After adding PostIt notes, save the state of the Desktop\n" + "<html>as an XML file. PostIt notes will be displayed whenever\n" + "<html>that file is loaded in the future", "Note!");
    }

    /**
     *  Add/Remove Mouse listener to each component depending on
     *  the add boolean.
     */
    private void registerComponents(Component[] comps, boolean add) {
        for (int i = 0; i < comps.length; i++) {
            if (comps[i] instanceof JPanel) {
                JPanel pane = (JPanel) comps[i];
                registerComponents(pane.getComponents(), add);
            } else if (comps[i] instanceof JTabbedPane) {
                JTabbedPane tab = (JTabbedPane) comps[i];
                int tabcount = tab.getTabCount();
                for (int j = 0; j < tabcount; j++) {
                    JPanel pane = (JPanel) tab.getComponentAt(j);
                    registerComponents(pane.getComponents(), add);
                }
            } else if (comps[i] instanceof JScrollPane) {
                JScrollPane pane = (JScrollPane) comps[i];
                registerComponents(pane.getComponents(), add);
            } else if (comps[i] instanceof JViewport) {
                JViewport port = (JViewport) comps[i];
                registerComponents(port.getComponents(), add);
            } else if (comps[i] instanceof AbstractButton) {
                AbstractButton ab = (AbstractButton) comps[i];
                if (add) {
                    ab.addMouseListener(mouseAdapter);
                    ab.setDoubleBuffered(ab.isEnabled());
                    ab.setEnabled(false);
                } else {
                    ab.removeMouseListener(mouseAdapter);
                    ab.setEnabled(ab.isDoubleBuffered());
                    ab.setDoubleBuffered(true);
                }
                if (comps[i] instanceof FisAdvancedButton) {
                    FisAdvancedButton fab = (FisAdvancedButton) comps[i];
                    registerComponents(fab.getFisFrame().getComponentPanel().getComponents(), add);
                }
            } else if (comps[i] instanceof JComboBox) {
                JComboBox cb = (JComboBox) comps[i];
                if (add) {
                    cb.setUI(new DesktopComboBoxUI(cb));
                    cb.setEnabled(false);
                    ((DesktopComboBoxUI) cb.getUI()).disable(true);
                    comps[i].addMouseListener(mouseAdapter);
                } else {
                    ComboBoxUI ui = cb.getUI();
                    if (ui instanceof DesktopComboBoxUI) {
                        ((DesktopComboBoxUI) ui).disable(false);
                        cb.setEnabled(((DesktopComboBoxUI) ui).wasEnabled());
                        comps[i].removeMouseListener(mouseAdapter);
                        try {
                            cb.setUI((ComboBoxUI) comboBoxUI.getClass().newInstance());
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            } else if (comps[i] instanceof JComponent) {
                if (add) {
                    comps[i].addMouseListener(mouseAdapter);
                    ((JComponent) comps[i]).setDoubleBuffered(comps[i].isEnabled());
                    comps[i].setEnabled(false);
                } else {
                    comps[i].removeMouseListener(mouseAdapter);
                    comps[i].setEnabled(((JComponent) comps[i]).isDoubleBuffered());
                }
            }
        }
    }

    private void removeInternalFrame(String title) {
        StringTokenizer st = new StringTokenizer(title, " ");
        int thenum = 0;
        try {
            thenum = (new Integer(st.nextToken())).intValue();
        } catch (Exception e) {
        }
        if (thenum > 0) thenum--;
        FlowPosition tempFlow = (FlowPosition) flowPosVec.elementAt(thenum);
        flowPosVec.removeElementAt(thenum);
        desktop.remove(desktop.getIndexOf(tempFlow.internalFrame));
        tempFlow.internalFrame.dispose();
        tempFlow.desktopBase.base.disposeAll();
        tempFlow.desktopBase.base = null;
        tempFlow.desktopBase = null;
        tempFlow.internalFrame = null;
        tempFlow = null;
        desktop.repaint();
        userPanel.repaint();
        iconCount--;
        updateButtonsAndFrames();
    }

    private void addInternalFrame(String title) {
        ClassStrings tempClassStrings;
        DesktopBase tempDesktopBase;
        FisBase tempBase;
        try {
            for (int y = 0; y < classes.size(); y++) {
                tempClassStrings = (ClassStrings) classes.elementAt(y);
                if (title.equals(tempClassStrings.title)) {
                    tempBase = (FisBase) ((Class.forName(tempClassStrings.className)).newInstance());
                    tempBase.getServerButton().changeCWDLocal();
                    tempBase.dispose();
                    tempDesktopBase = new DesktopBase(tempBase, title, iconCount + 1);
                    JInternalFrame internal = new JInternalFrame(iconCount + 1 + " " + title, true, false, false, false);
                    internal.addInternalFrameListener(this);
                    Container tempcont = internal.getContentPane();
                    JPanel panel = new JPanel();
                    BorderLayout bl = new BorderLayout();
                    bl.setVgap(5);
                    bl.setHgap(5);
                    panel.setLayout(bl);
                    panel.add(tempBase.getComponentPanel(), BorderLayout.NORTH);
                    tempBase.setLogButtonActive(false);
                    tempBase.setCloseButtonActive(false);
                    panel.add(tempBase.getButtonPanel(), BorderLayout.SOUTH);
                    JScrollPane scroll = new JScrollPane(panel);
                    tempcont.add(scroll);
                    internal.setJMenuBar(tempBase.getJMenuBar());
                    internal.pack();
                    Dimension id = internal.getSize();
                    Dimension dd = desktop.getSize();
                    if (id.width > dd.width) internal.setSize(dd.width - 10, id.height);
                    if (id.height > dd.height) internal.setSize(id.width, dd.height - 10);
                    internal.addInternalFrameListener(this);
                    desktop.add(internal);
                    internal.setVisible(false);
                    JButton button = new JButton(iconCount + 1 + " " + title);
                    button.setBackground(back);
                    button.setActionCommand("panelButton");
                    button.addActionListener(this);
                    button.addMouseMotionListener(this);
                    button.addMouseListener(this);
                    usergbl.setConstraints(button, usergbc);
                    userPanel.add(button);
                    usergbc.gridy++;
                    JMenuItem removeMenuItem = new JMenuItem(iconCount + 1 + " " + title);
                    removeMenuItem.setActionCommand("remove");
                    removeMenuItem.addActionListener(this);
                    removeMenu.add(removeMenuItem);
                    removeMenu.add(removeAll);
                    removeMenu.repaint();
                    desktop.repaint();
                    userPanel.repaint();
                    FlowPosition tempPos = new FlowPosition(iconCount, title, new Point(0, iconCount * 35), false);
                    tempPos.internalFrame = internal;
                    tempPos.removeMenuItem = removeMenuItem;
                    tempPos.desktopBase = tempDesktopBase;
                    tempPos.button = button;
                    flowPosVec.addElement(tempPos);
                    iconCount++;
                    show();
                }
            }
        } catch (ClassNotFoundException ex) {
            JOptionPane.showMessageDialog(this, "Could not find the " + title + " application\nPlease check your CLASSPATH", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "There was an error creating the application " + title + ":\n\n" + ex.toString(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateButtonsAndFrames() {
        JButton button;
        String title;
        FlowPosition tempPos;
        userPanel.removeAll();
        removeMenu.removeAll();
        usergbl.setConstraints(flow, usergbc);
        userPanel.add(flow, 0);
        usergbc.gridy++;
        for (int i = 0; i < flowPosVec.size(); i++) {
            tempPos = (FlowPosition) flowPosVec.elementAt(i);
            title = new String(tempPos.title);
            button = new JButton((i + 1) + " " + title);
            button.setBackground(back);
            button.setActionCommand("panelButton");
            button.addActionListener(this);
            button.addMouseMotionListener(this);
            button.addMouseListener(this);
            usergbl.setConstraints(button, usergbc);
            userPanel.add(button, i + 1);
            usergbc.gridy++;
            if (tempPos.internalFrame.isVisible()) {
                button.setForeground(Color.white);
                button.setBackground(front);
            }
            tempPos.place = i;
            tempPos.location = new Point(0, i * 35);
            tempPos.internalFrame.setTitle((i + 1) + " " + title);
            tempPos.removeMenuItem.setText((i + 1) + " " + title);
            tempPos.desktopBase.num = i + 1;
            tempPos.button = button;
            removeMenu.add(tempPos.removeMenuItem);
        }
        removeMenu.add(removeAll);
        userPanel.validate();
        userPanel.repaint();
        desktop.repaint();
        removeMenu.repaint();
    }

    public void mouseDragged(MouseEvent e) {
        if (e.getSource() instanceof JButton) {
            JButton b = (JButton) e.getSource();
            Point pt = SwingUtilities.convertPoint(b, e.getPoint(), userPanel);
            pt.x = pt.x - m.x;
            pt.y = pt.y - m.y;
            b.setLocation(pt);
        }
    }

    public void mousePressed(MouseEvent e) {
        m = e.getPoint();
        if (e.getSource() instanceof JButton) {
            JButton b = (JButton) e.getSource();
            p = b.getLocation();
            GridBagConstraints gbc = usergbl.getConstraints(b);
            userPanel.remove(b);
            usergbl.setConstraints(b, gbc);
            userPanel.add(b, 0);
        }
    }

    private int indexOf(String buttonTitle) {
        FlowPosition tempPos;
        for (int i = 0; i < flowPosVec.size(); i++) {
            tempPos = (FlowPosition) flowPosVec.elementAt(i);
            if (buttonTitle.equals((tempPos.place + 1) + " " + tempPos.title)) return i;
        }
        return -1;
    }

    public void mouseReleased(MouseEvent e) {
        if (e.getSource() instanceof JButton) {
            JButton b = (JButton) e.getSource();
            Point n = b.getLocation();
            int old_indx = indexOf(b.getText());
            int new_indx = old_indx + (n.y - p.y) / buttonWidth;
            if (old_indx == new_indx) {
                return;
            }
            if (new_indx >= flowPosVec.size()) new_indx = flowPosVec.size() - 1; else if (new_indx < 0) new_indx = 0;
            FlowPosition tempPos = (FlowPosition) flowPosVec.elementAt(old_indx);
            flowPosVec.removeElementAt(old_indx);
            flowPosVec.insertElementAt(tempPos, new_indx);
            tempPos.internalFrame.setVisible(false);
            updateButtonsAndFrames();
        }
    }

    public void mouseMoved(MouseEvent e) {
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void windowActivated(WindowEvent e) {
    }

    public void windowClosed(WindowEvent e) {
    }

    public void windowClosing(WindowEvent e) {
        if (standalone) System.exit(0); else dispose();
    }

    public void windowDeactivated(WindowEvent e) {
    }

    public void windowDeiconified(WindowEvent e) {
    }

    public void windowIconified(WindowEvent e) {
    }

    public void windowOpened(WindowEvent e) {
    }

    public void internalFrameActivated(InternalFrameEvent e) {
    }

    public void internalFrameClosed(InternalFrameEvent e) {
    }

    public void internalFrameClosing(InternalFrameEvent e) {
    }

    public void internalFrameDeactivated(InternalFrameEvent e) {
    }

    public void internalFrameDeiconified(InternalFrameEvent e) {
    }

    public void internalFrameIconified(InternalFrameEvent e) {
    }

    public void internalFrameOpened(InternalFrameEvent e) {
    }

    /**
     * Returns the boolean value of the logging option
     */
    public static boolean doLog() {
        if (logging && !nogood) return true; else return false;
    }

    /**
     * Returns the log file for logging
     */
    public static String getLogFile() {
        return logfilename;
    }

    /**
     * Set the log file for logging used by Logfile
     * Note that this will not check the filename at all
     */
    public static void setLogFile(String filename) {
        logfilename = filename;
    }

    /**
     * Returns true if a desktop has been constructed
     * so programs calling static class methods knwo if there's a desktop
     */
    public static boolean getRunningDesk() {
        if (runningdesk) return true; else return false;
    }

    /**
     *  This is a dummy class to get control of protected fields in BasicComboBoxUI
     */
    class DesktopComboBoxUI extends BasicComboBoxUI {

        private JComboBox parent;

        private boolean enabled;

        private boolean editable;

        public DesktopComboBoxUI(JComboBox parent) {
            this.parent = parent;
            this.enabled = parent.isEnabled();
            this.editable = parent.isEditable();
        }

        public void disable(boolean on) {
            if (on) {
                uninstallListeners();
                if (editable && editor != null) editor.addMouseListener(mouseAdapter);
            } else {
                installListeners();
                if (editable && editor != null) editor.removeMouseListener(mouseAdapter);
            }
        }

        public JComboBox getComboBox() {
            return parent;
        }

        public boolean wasEnabled() {
            return enabled;
        }

        public boolean wasEditable() {
            return editable;
        }
    }

    /**
     *  This Mouse adapter class is used for PostIts
     */
    private class DesktopMouseAdapter extends MouseAdapter {

        /**
         *  Find a FisBase that is a parent of the component.
         */
        private FisBase getFisBase(Component comp) {
            if (comp == null) {
                return null;
            }
            if (comp instanceof FisBase) {
                return (FisBase) comp;
            }
            if (comp instanceof FisFrame) {
                return getFisBase(((FisFrame) comp).getAppParent());
            }
            if (comp instanceof JInternalFrame) {
                for (int i = 0; i < flowPosVec.size(); i++) {
                    FlowPosition app = (FlowPosition) flowPosVec.elementAt(i);
                    if (app.internalFrame == (JInternalFrame) comp) {
                        return app.desktopBase.base;
                    }
                }
            }
            return getFisBase(comp.getParent());
        }

        public void mousePressed(MouseEvent e) {
            mouseClicked(e);
        }

        public void mouseClicked(MouseEvent e) {
            postitLock = true;
            if (e.getSource() instanceof FisPostit) {
                FisPostit sel_post = (FisPostit) e.getSource();
                sel_post.setVisible(false);
                for (int i = 0; i < flowPosVec.size(); i++) {
                    FlowPosition app = (FlowPosition) flowPosVec.elementAt(i);
                    if (app.internalFrame.isVisible()) {
                        if (sel_post.getComponent() instanceof JMenuBar) app.desktopBase.base.getAppInfo().description = "";
                        Vector postits = app.desktopBase.base.getPostitVector();
                        postits.removeElement(sel_post);
                        for (int j = 0; j < postits.size(); j++) {
                            FisPostit post = (FisPostit) postits.elementAt(j);
                            post.removeMouseListener(mouseAdapter);
                            post.setCursor(Cursor.getDefaultCursor());
                        }
                    }
                }
                epAdd.setEnabled(true);
            } else {
                String text = postitText.getText().trim();
                JComponent comp = (JComponent) e.getSource();
                if ((comp instanceof JTextField) && (comp.getParent() instanceof JComboBox)) comp = (JComponent) comp.getParent();
                FisBase base = getFisBase(comp);
                if (text.length() > 0 && base != null) {
                    if (base.getPostit(comp) == null) {
                        base.addPostit(new FisPostit(comp, text));
                        if (comp instanceof JMenuBar) base.getAppInfo().description = text;
                    } else Dialogs.ShowErrorDialog(desktop, "Each component can only have one PostIt note attached to it.\n" + "Please remove the existing PostIt note before adding a new one.");
                }
                base.displayPostits(true);
                JInternalFrame[] frames = desktop.getAllFrames();
                for (int i = 0; i < frames.length; i++) {
                    frames[i].getJMenuBar().setCursor(Cursor.getDefaultCursor());
                    frames[i].getContentPane().setCursor(Cursor.getDefaultCursor());
                    frames[i].getJMenuBar().removeMouseListener(mouseAdapter);
                    registerComponents(frames[i].getContentPane().getComponents(), false);
                }
                epRemove.setEnabled(true);
            }
        }
    }
}
