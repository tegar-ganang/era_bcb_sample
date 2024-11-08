package abstrasy.consoleui;

import abstrasy.Interpreter;
import abstrasy.Main;
import abstrasy.SimpleFileFilter;
import abstrasy.SourceFile;
import abstrasy.Tools;
import abstrasy.bedesk.ui.BeCheckButton;
import abstrasy.bedesk.ui.BeHiddenTabbedPane;
import abstrasy.bedesk.ui.BeTabButton;
import abstrasy.bedesk.ui.BeTabsGroup;
import abstrasy.bedesk.ui.BeTextField;
import abstrasy.bedesk.ui.HalloButton;
import abstrasy.bedesk.ui.RoundButton;
import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JToolBar;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.Element;

/**
 * Abstrasy Interpreter
 *
 * Copyright : Copyright (c) 2006-2012, Luc Bruninx.
 *
 * Concédée sous licence EUPL, version 1.1 uniquement (la «Licence»).
 *
 * Vous ne pouvez utiliser la présente oeuvre que conformément à la Licence.
 * Vous pouvez obtenir une copie de la Licence à l’adresse suivante:
 *
 *   http://www.osor.eu/eupl
 *
 * Sauf obligation légale ou contractuelle écrite, le logiciel distribué sous
 * la Licence est distribué "en l’état", SANS GARANTIES OU CONDITIONS QUELLES
 * QU’ELLES SOIENT, expresses ou implicites.
 *
 * Consultez la Licence pour les autorisations et les restrictions
 * linguistiques spécifiques relevant de la Licence.
 *
 *
 * @author Luc Bruninx
 * @version 1.0
 */
public class FrameWorkConsole extends JFrame {

    private static final String DEFAULT_FILENAME = "new-script" + SourceFile.EXT_DEFAULT;

    String actualFile = DEFAULT_FILENAME;

    private JPanel mainPanel = new JPanel();

    private BeTabsGroup topPanel = new BeTabsGroup();

    private BorderLayout borderLayout7 = new BorderLayout();

    private JPanel bottomPane = new JPanel();

    private JLabel actualFileName = new JLabel();

    private BorderLayout borderLayout8 = new BorderLayout();

    private JToolBar jToolBar1 = new JToolBar();

    private BorderLayout borderLayout9 = new BorderLayout();

    private FlowLayout flowLayout1 = new FlowLayout(FlowLayout.LEFT, 2, 0);

    private JSeparator jSeparator1 = new JSeparator();

    private JSeparator jSeparator2 = new JSeparator();

    private JSeparator jSeparator3 = new JSeparator();

    private JPanel jPanel8 = new JPanel();

    private GridLayout gridLayout2 = new GridLayout(2, 1, 1, 1);

    private JPopupMenu jPopupMenu1 = new JPopupMenu();

    private JMenuItem jMenuItem1 = new JMenuItem();

    private JMenuItem jMenuItem2 = new JMenuItem();

    private JMenuItem jMenuItem3 = new JMenuItem();

    private JPanel leftPanel = new JPanel();

    private JPanel rightPanel = new JPanel();

    private JPanel jPanel1 = new JPanel();

    private JScrollPane jScrollPane3 = new JScrollPane();

    private BorderLayout borderLayout10 = new BorderLayout();

    private OutputTextArea logArea = new OutputTextArea();

    private JMenuBar jMenuBar1 = new JMenuBar();

    private JMenu jMenu1 = new JMenu();

    private JMenuItem jMenuItem4 = new JMenuItem();

    private JMenuItem jMenuItem5 = new JMenuItem();

    private JMenuItem jMenuItem6 = new JMenuItem();

    private JMenu jMenu2 = new JMenu();

    private JMenuItem jMenuItem7 = new JMenuItem();

    private JPopupMenu jPopupMenu2 = new JPopupMenu();

    private JMenuItem jMenuItem8 = new JMenuItem();

    private JMenuItem jMenuItem9 = new JMenuItem();

    private BeTabButton jButton13 = new BeTabButton();

    private BeTabButton jButton14 = new BeTabButton();

    private BeTabButton jButton15 = new BeTabButton();

    private FlowLayout flowLayout2 = new FlowLayout();

    public void setActualFile(String actualFile) {
        this.actualFile = actualFile;
        actualFileName.setText("Fichier: " + this.actualFile);
    }

    public String getActualFile() {
        return actualFile;
    }

    boolean disposeOnWindowClosing = false;

    ImageIcon i01 = new ImageIcon(Interpreter.class.getResource("resources/icons/open32.png"));

    ImageIcon i02 = new ImageIcon(Interpreter.class.getResource("resources/icons/save32.png"));

    ImageIcon i03 = new ImageIcon(Interpreter.class.getResource("resources/icons/play32.png"));

    ImageIcon i04 = new ImageIcon(Interpreter.class.getResource("resources/icons/clear32.png"));

    ImageIcon i05 = new ImageIcon(Interpreter.class.getResource("resources/icons/new32.png"));

    ImageIcon i06 = new ImageIcon(Interpreter.class.getResource("resources/icons/stop32.png"));

    ImageIcon ia1 = new ImageIcon(Interpreter.class.getResource("resources/icons/copy24.png"));

    ImageIcon ia2 = new ImageIcon(Interpreter.class.getResource("resources/icons/cut24.png"));

    ImageIcon ia3 = new ImageIcon(Interpreter.class.getResource("resources/icons/paste24.png"));

    ImageIcon ia4 = new ImageIcon(Interpreter.class.getResource("resources/icons/goto24.png"));

    JPanel contentPane;

    HalloButton jButton1 = new HalloButton();

    HalloButton jButton2 = new HalloButton();

    HalloButton jButton3 = new HalloButton();

    HalloButton jButton4 = new HalloButton();

    RoundButton jButton5 = new RoundButton();

    HalloButton jButton6 = new HalloButton();

    HalloButton jButton7 = new HalloButton();

    HalloButton jButton8 = new HalloButton();

    HalloButton jButton9 = new HalloButton();

    private HalloButton jButton10 = new HalloButton();

    private HalloButton jButton11 = new HalloButton();

    Border border1;

    BeHiddenTabbedPane jSplitPane1 = new BeHiddenTabbedPane();

    BorderLayout borderLayout1 = new BorderLayout();

    JPanel jPanel2 = new JPanel();

    JPanel jPanel3 = new JPanel();

    BorderLayout borderLayout2 = new BorderLayout();

    BorderLayout borderLayout3 = new BorderLayout();

    MyLabel jLabel1 = new MyLabel();

    JScrollPane jScrollPane1 = new JScrollPane();

    EditorTextArea jTextArea1 = new EditorTextArea();

    Border border2;

    JPanel jPanel4 = new JPanel();

    JScrollPane jScrollPane2 = new JScrollPane();

    BorderLayout borderLayout4 = new BorderLayout();

    OutputTextArea jTextArea2 = new OutputTextArea();

    JPanel jPanel5 = new JPanel();

    BeTextField jTextField1 = new BeTextField();

    BorderLayout borderLayout5 = new BorderLayout();

    Interpreter lastInterpreter = null;

    JPanel jPanel6 = new JPanel();

    GridLayout gridLayout1 = new GridLayout();

    JPanel jPanel7 = new JPanel();

    BorderLayout borderLayout6 = new BorderLayout();

    Border border3;

    Border border4;

    Border border6;

    Border border7;

    Border border8;

    private BeCheckButton jCheckBox_LogAll = new BeCheckButton();

    public FrameWorkConsole() {
        enableEvents(AWTEvent.WINDOW_EVENT_MASK);
        try {
            jbInit();
            jbInit2();
            this.pack();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void jbInit() throws Exception {
        contentPane = (JPanel) this.getContentPane();
        border1 = BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(Color.white, new Color(142, 142, 142)), BorderFactory.createEmptyBorder(3, 3, 3, 3));
        border2 = BorderFactory.createEmptyBorder(4, 4, 4, 4);
        border3 = BorderFactory.createEmptyBorder(0, 4, 0, 4);
        border6 = BorderFactory.createLineBorder(UIManager.getColor("TextField.darkShadow"), 1);
        border7 = BorderFactory.createEmptyBorder(2, 2, 2, 2);
        border8 = BorderFactory.createEmptyBorder(2, 2, 2, 2);
        contentPane.setLayout(borderLayout1);
        this.setTitle(Interpreter.TITLE_APP);
        this.setSize(new Dimension(930, 636));
        this.setJMenuBar(jMenuBar1);
        this.addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                this_windowClosing(e);
            }
        });
        jButton1.setMaximumSize(new Dimension(84, 36));
        jButton1.setMinimumSize(new Dimension(84, 36));
        jButton1.setPreferredSize(new Dimension(84, 36));
        jButton1.setToolTipText("");
        jButton1.setIcon(i03);
        jButton1.setText("Run");
        jButton1.setFont(new Font("Dialog", 0, 10));
        jButton1.addActionListener(new Cadre1_jButton1_actionAdapter(this));
        contentPane.setMinimumSize(new Dimension(700, 538));
        contentPane.setPreferredSize(new Dimension(1024, 640));
        contentPane.setSize(new Dimension(1024, 640));
        jSplitPane1.setPreferredSize(new Dimension(1000, 680));
        jSplitPane1.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        jButton2.setMaximumSize(new Dimension(84, 36));
        jButton2.setMinimumSize(new Dimension(84, 36));
        jButton2.setPreferredSize(new Dimension(84, 36));
        jButton2.setIcon(i02);
        jButton2.setText("Save");
        jButton2.setFont(new Font("Dialog", 0, 10));
        jButton2.addActionListener(new Cadre1_jButton2_actionAdapter(this));
        jButton3.setMaximumSize(new Dimension(84, 36));
        jButton3.setMinimumSize(new Dimension(84, 36));
        jButton3.setPreferredSize(new Dimension(84, 36));
        jButton3.setIcon(i01);
        jButton3.setText("Load");
        jButton3.setFont(new Font("Dialog", 0, 10));
        jButton3.addActionListener(new Cadre1_jButton3_actionAdapter(this));
        jButton4.setMaximumSize(new Dimension(128, 36));
        jButton4.setMinimumSize(new Dimension(128, 36));
        jButton4.setPreferredSize(new Dimension(128, 36));
        jButton4.setIcon(i04);
        jButton4.setText("Clear Console");
        jButton4.setFont(new Font("Dialog", 0, 10));
        jButton4.addActionListener(new Cadre1_jButton4_actionAdapter(this));
        jPanel3.setLayout(borderLayout2);
        jPanel2.setLayout(borderLayout3);
        jPanel2.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        jPanel2.addComponentListener(new ComponentAdapter() {

            public void componentShown(ComponentEvent e) {
                jPanel2_componentShown(e);
            }
        });
        jPanel3.setBorder(border7);
        jPanel3.setMinimumSize(new Dimension(220, 36));
        jPanel3.setPreferredSize(new Dimension(220, 36));
        jLabel1.setFont(new Font("Dialog", 0, 10));
        jLabel1.setText("jLabel1");
        jScrollPane1.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        jScrollPane1.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        jTextArea1.addCaretListener(new Cadre1_jTextArea1_caretAdapter(this));
        jTextArea1.setFont(new Font("Monospaced", 0, 12));
        jTextArea1.setBorder(border2);
        jTextArea1.setText("");
        jTextArea1.setTabSize(2);
        jTextArea1.addPropertyChangeListener(new Cadre1_jTextArea1_propertyChangeAdapter(this));
        jTextArea1.addMouseListener(new MouseAdapter() {

            public void mouseClicked(MouseEvent e) {
                jTextArea1_mouseClicked(e);
            }
        });
        jTextArea1.comTimer.start();
        jPanel4.setLayout(borderLayout4);
        jPanel4.addComponentListener(new ComponentAdapter() {

            public void componentShown(ComponentEvent e) {
                jPanel4_componentShown(e);
            }
        });
        jScrollPane2.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        jButton5.setFont(new Font("Dialog", 0, 10));
        jButton5.setMaximumSize(new Dimension(72, 23));
        jButton5.setMinimumSize(new Dimension(72, 23));
        jButton5.setPreferredSize(new Dimension(72, 23));
        jButton5.setText("Do");
        jButton5.addActionListener(new Cadre1_jButton5_actionAdapter(this));
        jPanel5.setLayout(borderLayout5);
        jTextField1.setFont(new Font("Dialog", 0, 10));
        jTextField1.setText("");
        jTextField1.addActionListener(new Cadre1_jTextField1_actionAdapter(this));
        jPanel6.setLayout(gridLayout1);
        jPanel6.setMinimumSize(new Dimension(416, 32));
        jPanel6.setPreferredSize(new Dimension(416, 32));
        gridLayout1.setColumns(5);
        jButton6.setFont(new Font("Dialog", 0, 10));
        jButton6.setMaximumSize(new Dimension(43, 33));
        jButton6.setMinimumSize(new Dimension(43, 33));
        jButton6.setIcon(ia1);
        jButton6.setText("Copy");
        jButton6.addActionListener(new Console_jButton6_actionAdapter(this));
        jButton7.setFont(new Font("Dialog", 0, 10));
        jButton7.setIcon(ia2);
        jButton7.setText("Cut");
        jButton7.addActionListener(new Console_jButton7_actionAdapter(this));
        jButton8.setFont(new Font("Dialog", 0, 10));
        jButton8.setIcon(ia3);
        jButton8.setText("Paste");
        jButton8.addActionListener(new Console_jButton8_actionAdapter(this));
        jPanel7.setBorder(border3);
        jPanel7.setMinimumSize(new Dimension(128, 23));
        jPanel7.setPreferredSize(new Dimension(128, 23));
        jPanel7.setLayout(borderLayout6);
        borderLayout5.setHgap(2);
        borderLayout5.setVgap(1);
        borderLayout2.setHgap(0);
        borderLayout2.setVgap(0);
        jButton9.setText("New");
        jButton9.addActionListener(new Cadre1_jButton9_actionAdapter(this));
        jButton9.setIcon(i05);
        jButton9.setPreferredSize(new Dimension(84, 36));
        jButton9.setMinimumSize(new Dimension(84, 36));
        jButton9.setMaximumSize(new Dimension(84, 36));
        jButton9.setSize(new Dimension(884, 36));
        jButton9.setFont(new Font("Dialog", 0, 10));
        jButton9.addActionListener(new Cadre1_jButton9_actionAdapter(this));
        jPanel5.setBorder(border8);
        jButton10.setText("Break");
        jButton10.addActionListener(new Console_jButton10_actionAdapter(this));
        jButton10.setIcon(i06);
        jButton10.setToolTipText("");
        jButton10.setPreferredSize(new Dimension(84, 36));
        jButton10.setMinimumSize(new Dimension(84, 36));
        jButton10.setMaximumSize(new Dimension(84, 36));
        jButton10.setFont(new Font("Dialog", 0, 10));
        jCheckBox_LogAll.setText("Log all");
        jCheckBox_LogAll.setFont(new Font("Dialog", 0, 10));
        jCheckBox_LogAll.setSelected(Interpreter.isDebugMode());
        jCheckBox_LogAll.setPreferredSize(new Dimension(57, 14));
        jCheckBox_LogAll.setOpaque(false);
        jCheckBox_LogAll.setFocusPainted(false);
        jCheckBox_LogAll.setHorizontalAlignment(SwingConstants.LEFT);
        jCheckBox_LogAll.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                jCheckBox_LogAll_actionPerformed(e);
            }
        });
        jButton15.setText("Logs");
        jButton15.setMaximumSize(new Dimension(55, 28));
        jButton15.setMinimumSize(new Dimension(55, 28));
        jButton15.setPreferredSize(new Dimension(55, 28));
        jButton15.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                jButton15_actionPerformed(e);
            }
        });
        flowLayout2.setAlignment(0);
        flowLayout2.setVgap(0);
        flowLayout2.setAlignOnBaseline(true);
        jButton14.setText("Console");
        jButton14.setMaximumSize(new Dimension(74, 28));
        jButton14.setMinimumSize(new Dimension(74, 28));
        jButton14.setPreferredSize(new Dimension(74, 28));
        jButton14.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                jButton14_actionPerformed(e);
            }
        });
        jButton13.setText("Editor");
        jButton13.setMaximumSize(new Dimension(61, 28));
        jButton13.setMinimumSize(new Dimension(61, 28));
        jButton13.setPreferredSize(new Dimension(61, 28));
        jButton13.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                jButton13_actionPerformed(e);
            }
        });
        jMenuItem9.setText("Find next...");
        jMenuItem9.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                jMenuItem9_actionPerformed(e);
            }
        });
        jPopupMenu2.setLabel("jPopupMenu2");
        jMenuItem8.setText("Go to line number...");
        jMenuItem8.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                jMenuItem8_actionPerformed(e);
            }
        });
        jButton11.setText("Go to...");
        jButton11.setFont(new Font("Dialog", 0, 10));
        jButton11.setIcon(ia4);
        jButton11.addActionListener(new FrameWorkConsole_jButton11_actionAdapter(this));
        jMenuItem7.setText("About");
        jMenuItem7.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                jMenuItem7_actionPerformed(e);
            }
        });
        jMenu2.setText("Help");
        jMenuItem6.setText("Save...");
        jMenuItem6.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                jMenuItem6_actionPerformed(e);
            }
        });
        jMenuItem5.setText("Open...");
        jMenuItem5.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                jMenuItem5_actionPerformed(e);
            }
        });
        jMenuItem4.setText("New file...");
        jMenuItem4.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                jMenuItem4_actionPerformed(e);
            }
        });
        jMenu1.setText("File");
        jMenuItem3.setText("Paste");
        jMenuItem3.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                jMenuItem3_actionPerformed(e);
            }
        });
        leftPanel.setMinimumSize(new Dimension(1, 1));
        leftPanel.setPreferredSize(new Dimension(1, 1));
        rightPanel.setMinimumSize(new Dimension(1, 1));
        rightPanel.setPreferredSize(new Dimension(1, 1));
        jPanel1.setLayout(borderLayout10);
        jPanel1.addComponentListener(new ComponentAdapter() {

            public void componentShown(ComponentEvent e) {
                jPanel1_componentShown(e);
            }
        });
        logArea.setBackground(new Color(231, 231, 231));
        logArea.setForeground(new Color(0, 74, 115));
        logArea.setCurrentFG(new Color(0, 82, 82));
        logArea.setCurrentBG(new Color(231, 231, 231));
        jMenuItem2.setText("Copy");
        jMenuItem2.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                jMenuItem2_actionPerformed(e);
            }
        });
        jPopupMenu1.setLabel("jPopupMenu1");
        jMenuItem1.setText("Cut");
        jMenuItem1.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                jMenuItem1_actionPerformed(e);
            }
        });
        jPanel8.setPreferredSize(new Dimension(100, 32));
        jPanel8.setLayout(gridLayout2);
        jPanel8.setOpaque(false);
        jSeparator3.setOrientation(SwingConstants.VERTICAL);
        jSeparator3.setPreferredSize(new Dimension(2, 28));
        jSeparator2.setOrientation(SwingConstants.VERTICAL);
        jSeparator2.setPreferredSize(new Dimension(4, 28));
        jSeparator1.setPreferredSize(new Dimension(4, 30));
        jSeparator1.setSize(new Dimension(2, 30));
        jSeparator1.setMinimumSize(new Dimension(1, 30));
        jSeparator1.setOrientation(SwingConstants.VERTICAL);
        jSeparator1.setBounds(new Rectangle(0, -6, 2, 32));
        actualFileName.setText("FileName");
        actualFileName.setFont(new Font("Dialog", 0, 10));
        jToolBar1.setMinimumSize(new Dimension(200, 40));
        jToolBar1.setSize(new Dimension(4935, 34));
        jToolBar1.setPreferredSize(new Dimension(800, 40));
        jToolBar1.setLayout(flowLayout1);
        jToolBar1.setMargin(new Insets(0, 10, 0, 0));
        mainPanel.setLayout(borderLayout7);
        topPanel.setMaximumSize(new Dimension(32767, 32));
        topPanel.setMinimumSize(new Dimension(10, 32));
        topPanel.setPreferredSize(new Dimension(10, 32));
        topPanel.setRightOffset(-3);
        topPanel.setLeftOffset(-3);
        topPanel.setRoundDiameter(0);
        topPanel.setLayout(flowLayout2);
        topPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 1, 4));
        topPanel.setBottomConnected(true);
        bottomPane.setLayout(borderLayout8);
        bottomPane.setMinimumSize(new Dimension(38, 26));
        bottomPane.setPreferredSize(new Dimension(38, 26));
        bottomPane.setBorder(border3);
        jPanel2.add(jPanel3, BorderLayout.SOUTH);
        jPanel3.add(jPanel6, BorderLayout.EAST);
        jPanel6.add(jButton11, null);
        jPanel6.add(jButton6, null);
        jPanel6.add(jButton7, null);
        jPanel6.add(jButton8, null);
        jPanel3.add(jPanel7, BorderLayout.CENTER);
        jPanel7.add(jLabel1, BorderLayout.CENTER);
        jPanel2.add(jScrollPane1, BorderLayout.CENTER);
        jPanel4.add(jScrollPane2, BorderLayout.CENTER);
        jPanel4.add(jPanel5, BorderLayout.SOUTH);
        jPanel5.add(jTextField1, BorderLayout.CENTER);
        jPanel5.add(jButton5, BorderLayout.EAST);
        jScrollPane2.getViewport().add(jTextArea2, null);
        jScrollPane1.getViewport().add(jTextArea1, null);
        topPanel.add(jButton13, null);
        topPanel.add(jButton14, null);
        topPanel.add(jButton15, null);
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(jSplitPane1, BorderLayout.CENTER);
        contentPane.add(mainPanel, BorderLayout.CENTER);
        jToolBar1.add(jButton9, null);
        jToolBar1.add(jSeparator1, null);
        jToolBar1.add(jButton3, null);
        jToolBar1.add(jButton2, null);
        jToolBar1.add(jSeparator2, null);
        jToolBar1.add(jButton1, null);
        jToolBar1.add(jButton10, null);
        jToolBar1.add(jButton4, null);
        jToolBar1.add(jSeparator3, null);
        jPanel8.add(jCheckBox_LogAll, null);
        jToolBar1.add(jPanel8, null);
        contentPane.add(jToolBar1, BorderLayout.NORTH);
        bottomPane.add(actualFileName, BorderLayout.CENTER);
        contentPane.add(bottomPane, BorderLayout.SOUTH);
        contentPane.add(leftPanel, BorderLayout.WEST);
        contentPane.add(rightPanel, BorderLayout.EAST);
        jSplitPane1.add(jPanel2, "Editor");
        jSplitPane1.add(jPanel4, "Console");
        jScrollPane3.getViewport().add(logArea, null);
        jPanel1.add(jScrollPane3, BorderLayout.CENTER);
        jSplitPane1.add(jPanel1, "Logs");
        jPopupMenu1.add(jMenuItem2);
        jPopupMenu1.add(jMenuItem1);
        jPopupMenu1.add(jMenuItem3);
        jMenu1.add(jMenuItem4);
        jMenu1.add(jMenuItem5);
        jMenu1.add(jMenuItem6);
        jMenuBar1.add(jMenu1);
        jMenu2.add(jMenuItem7);
        jMenuBar1.add(jMenu2);
        jPopupMenu2.add(jMenuItem8);
        jPopupMenu2.add(jMenuItem9);
    }

    /**
     * adaptation automatique de la taille de la fenêtre (surtout pour les minis pc).
     */
    private static boolean OnOSX = System.getProperty("os.name").toUpperCase().startsWith("MAC OS X");

    private static int MINIMUM_WINDOW_HEIGHT = 648 + (OnOSX ? 64 : 0);

    private static Dimension getScreenSize() {
        return Toolkit.getDefaultToolkit().getScreenSize();
    }

    private static int getScreenHeight() {
        return getScreenSize().height;
    }

    private static int getScreenWidth() {
        return getScreenSize().width;
    }

    private static void maximizeFrame(JFrame frame) {
        GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
        frame.setLocation(0, 0);
        frame.setMaximizedBounds(env.getMaximumWindowBounds());
        frame.setExtendedState(frame.getExtendedState() | frame.MAXIMIZED_BOTH);
    }

    private void jbInit2() throws Exception {
        if (getScreenHeight() < MINIMUM_WINDOW_HEIGHT) {
            maximizeFrame(this);
        }
        jButton13.setBackground(topPanel.getBackground());
        jButton14.setBackground(topPanel.getBackground());
        jButton15.setBackground(topPanel.getBackground());
        jButton14.setSelected(true);
        jSplitPane1.setSelectedComponent(jPanel4);
        jTextArea2.write(Interpreter.TITLE_APP + "\n");
        Interpreter.makeDefaultReserved();
        this.setActualFile(DEFAULT_FILENAME);
        if (Main.arg_file != null) {
            _loadSource(new File(Main.arg_file));
        }
        jTextArea2.write("Ready...\n");
    }

    private String referenceSource_ = "";

    private final void _loadSource(File fln) {
        String fname = fln.getAbsolutePath();
        System.out.println("Load " + fname);
        try {
            SourceFile sfile = new SourceFile(fname);
            sfile.load();
            jTextArea1.setText(sfile.getSource());
            referenceSource_ = jTextArea1.getText();
            jTextArea1.setCaretPosition(0);
            jTextArea1.forceColorize();
            jTextArea2.write(fln + " loaded...\n");
            this.setActualFile(fname);
            jSplitPane1.setSelectedComponent(jPanel2);
        } catch (Exception ex) {
            ex.printStackTrace();
            try {
                jTextArea2.write("Warning : IOException error... Loading abstrasy File.\n");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void setDisposeOnWindowClosing(boolean disposeOnWindowClosing) {
        this.disposeOnWindowClosing = disposeOnWindowClosing;
    }

    public boolean isDisposeOnWindowClosing() {
        return this.disposeOnWindowClosing;
    }

    protected void processWindowEvent(WindowEvent e) {
        super.processWindowEvent(e);
        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
            if (disposeOnWindowClosing) {
                this.dispose();
            } else {
                System.exit(0);
            }
        }
    }

    /**
     * Active ou désactive les boutons du FreeIDE. Cette méthode est utilisée lors
     * de l'exécution des scripts.
     *
     * @param status
     */
    private void onRunChangeButtons(boolean status) {
        jButton1.setEnabled(status);
        jButton9.setEnabled(status);
        jButton3.setEnabled(status);
        jButton2.setEnabled(status);
        jButton6.setEnabled(status);
        jButton7.setEnabled(status);
        jButton8.setEnabled(status);
        jTextArea1.setEnabled(status);
        jTextField1.setEnabled(status);
        jButton5.setEnabled(status);
    }

    /**
     * Evènement (tâche plannifiée) permet de détecter automatiquement si l'interpréteur s'est
     * arrêté et ainsi permet de réactiver les boutons du FreeIDE.
     */
    private Action stoppingAction = new AbstractAction() {

        public void actionPerformed(ActionEvent e) {
            if (lastInterpreter != null && (!lastInterpreter.isAlive() || lastInterpreter.isInterrupted())) {
                onRunChangeButtons(true);
                stoppingActionTimer.stop();
            }
        }
    };

    private Timer stoppingActionTimer = new Timer(1000, stoppingAction);

    void jButton1_actionPerformed(ActionEvent e) {
        this.jSplitPane1.setSelectedComponent(jPanel4);
        try {
            onRunChangeButtons(false);
            stoppingActionTimer.start();
            lastInterpreter = Interpreter.interpr_getSuperInterpreter(Interpreter.GETSUPERINTERPRETER_HARDMODE);
            Interpreter.setDebugMode(jCheckBox_LogAll.isSelected());
            Interpreter.registerSourceAbsolutePath(this.getActualFile());
            lastInterpreter.setSource(jTextArea1.getText());
            lastInterpreter.setOutputTextArea(jTextArea2);
            lastInterpreter.setLogTextArea(logArea);
            lastInterpreter.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            onRunChangeButtons(true);
            stoppingActionTimer.stop();
        }
    }

    void jButton2_actionPerformed(ActionEvent e) {
        String fname;
        fname = "";
        JFileChooser fdial = new JFileChooser();
        fdial.setDialogTitle("Save as...");
        fdial.setCurrentDirectory(new File(Tools.directoryOfFile(this.getActualFile())));
        fdial.setSelectedFile(new File(this.getActualFile()));
        fdial.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fdial.setDialogType(JFileChooser.SAVE_DIALOG);
        fdial.addChoosableFileFilter(SimpleFileFilter.DEFAULT);
        fdial.addChoosableFileFilter(SimpleFileFilter.COMPRESSED);
        fdial.setFileFilter(SimpleFileFilter.DEFAULT);
        int uresp = fdial.showSaveDialog(this);
        File fln = fdial.getSelectedFile();
        if (uresp == JFileChooser.APPROVE_OPTION && fln != null) {
            fname = fln.getAbsolutePath();
            try {
                boolean cont = true;
                if (new File(fname).exists()) {
                    cont = (JOptionPane.showConfirmDialog(this, "The file " + fname + "\nalready exists...\nOverwrite it?", "Existing file...", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION);
                }
                if (cont) {
                    SourceFile sfile = new SourceFile(fname);
                    sfile.setSource(jTextArea1.getText());
                    sfile.save();
                    referenceSource_ = jTextArea1.getText();
                    jTextArea2.write(fln + " saved...\n");
                    this.setActualFile(fname);
                } else {
                    jTextArea2.write("cancelled...\n");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                try {
                    jTextArea2.write("Warning : IOException error... Saving abstrasy File.\n");
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        }
    }

    void jButton3_actionPerformed(ActionEvent e) {
        String fname;
        fname = "";
        JFileChooser fdial = new JFileChooser();
        fdial.setDialogTitle("Ouvrir...");
        fdial.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fdial.setDialogType(JFileChooser.OPEN_DIALOG);
        fdial.addChoosableFileFilter(SimpleFileFilter.DEFAULT);
        fdial.addChoosableFileFilter(SimpleFileFilter.COMPRESSED);
        fdial.setFileFilter(SimpleFileFilter.DEFAULT);
        fdial.setCurrentDirectory(new File(Tools.directoryOfFile(this.getActualFile())));
        fdial.setSelectedFile(new File(this.getActualFile()));
        int uresp = fdial.showOpenDialog(this);
        File fln = fdial.getSelectedFile();
        if (uresp == JFileChooser.APPROVE_OPTION && fln != null) {
            _loadSource(fln);
        }
    }

    void jButton4_actionPerformed(ActionEvent e) {
        jTextArea2.clr();
        logArea.clr();
    }

    void caretPosition() {
        try {
            int caretpos = jTextArea1.getCaretPosition();
            Element root = jTextArea1.getDocument().getDefaultRootElement();
            int dot = jTextArea1.getCaret().getDot();
            int line = root.getElementIndex(dot);
            int col = dot - root.getElement(line).getStartOffset();
            String cs = "L : " + (line + 1) + "  /  C : " + (col + 1);
            jLabel1.setText(cs);
            ((MyDocument) jTextArea1.getDocument()).actionDone(caretpos);
        } catch (Exception exx) {
        }
    }

    void jTextArea1_propertyChange(PropertyChangeEvent e) {
        caretPosition();
    }

    void jTextArea1_caretUpdate(CaretEvent e) {
        caretPosition();
    }

    void jButton5_actionPerformed(ActionEvent e) {
        try {
            onRunChangeButtons(false);
            stoppingActionTimer.start();
            if (lastInterpreter == null) {
                lastInterpreter = Interpreter.interpr_getSuperInterpreter(Interpreter.GETSUPERINTERPRETER_HARDMODE);
            } else {
                lastInterpreter = Interpreter.interpr_cloneInterpreter(lastInterpreter);
            }
            lastInterpreter.setDebugMode(jCheckBox_LogAll.isSelected());
            lastInterpreter.setOutputTextArea(jTextArea2);
            lastInterpreter.setLogTextArea(logArea);
            lastInterpreter.setSource(this.jTextField1.getText());
            lastInterpreter.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            onRunChangeButtons(true);
            stoppingActionTimer.stop();
        }
    }

    void jTextField1_actionPerformed(ActionEvent e) {
        jButton5_actionPerformed(null);
    }

    void jButton6_actionPerformed(ActionEvent e) {
        jTextArea1.copy();
    }

    void jButton7_actionPerformed(ActionEvent e) {
        jTextArea1.cut();
    }

    void jButton8_actionPerformed(ActionEvent e) {
        jTextArea1.paste();
    }

    void jButton9_actionPerformed(ActionEvent e) {
        jTextArea1.setText("");
        jSplitPane1.setSelectedComponent(jPanel2);
    }

    void jButton10_actionPerformed(ActionEvent e) {
        if (lastInterpreter != null) {
            if (lastInterpreter.isAlive()) {
                lastInterpreter.setBreakingFromSEMAPHORE(true);
                lastInterpreter = null;
                onRunChangeButtons(true);
                stoppingActionTimer.stop();
            }
        }
    }

    private void jCheckBox_LogAll_actionPerformed(ActionEvent e) {
        Interpreter.setDebugMode(jCheckBox_LogAll.isSelected());
    }

    private void jMenuItem1_actionPerformed(ActionEvent e) {
        this.jTextArea1.cut();
        this.jTextArea1.grabFocus();
    }

    private void jMenuItem2_actionPerformed(ActionEvent e) {
        this.jTextArea1.copy();
        this.jTextArea1.grabFocus();
    }

    private void jMenuItem3_actionPerformed(ActionEvent e) {
        this.jTextArea1.paste();
        this.jTextArea1.grabFocus();
    }

    private void jTextArea1_mouseClicked(MouseEvent e) {
        boolean isRightButton = (SwingUtilities.isRightMouseButton(e)) || (SwingUtilities.isLeftMouseButton(e) && e.isControlDown());
        if (isRightButton) {
            jPopupMenu1.show(jTextArea1, e.getX(), e.getY());
        }
    }

    private void jMenuItem4_actionPerformed(ActionEvent e) {
        jButton9_actionPerformed(null);
    }

    private void jMenuItem5_actionPerformed(ActionEvent e) {
        jButton3_actionPerformed(null);
    }

    private void jMenuItem6_actionPerformed(ActionEvent e) {
        jButton2_actionPerformed(null);
    }

    private void this_windowClosing(WindowEvent e) {
        if (!referenceSource_.equals(jTextArea1.getText())) {
            if (JOptionPane.showConfirmDialog(this, "The file has been modified...\nWould you save the file before exiting?", "Save before exit...", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                jButton2_actionPerformed(null);
            }
        }
    }

    private void jMenuItem7_actionPerformed(ActionEvent e) {
        AboutDialog.createAbout(this);
    }

    void jButton11_actionPerformed(ActionEvent e) {
        jPopupMenu2.show(jPanel6, jButton11.getX() + (jButton11.getWidth() / 3) * 2, jButton11.getY() + (jButton11.getHeight() / 3) * 2);
    }

    private void jMenuItem8_actionPerformed(ActionEvent e) {
        String l = JOptionPane.showInputDialog(this, "Enter valid line number...", "Go to line number...", JOptionPane.PLAIN_MESSAGE);
        if (l != null) {
            int n = -1;
            try {
                n = Integer.parseInt(l);
            } catch (Exception ex) {
                n = -1;
            }
            if (n >= 1) {
                int carpos = this.jTextArea1.getLineStartOffset(n - 1);
                if (carpos >= 0) {
                    this.jTextArea1.setCaretPosition(carpos);
                    this.jTextArea1.setSelectionStart(carpos);
                    this.jTextArea1.setSelectionEnd(carpos);
                    this.jTextArea1.grabFocus();
                    return;
                }
            }
            JOptionPane.showMessageDialog(this, "Invalid line number.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void jMenuItem9_actionPerformed(ActionEvent e) {
        String l = this.jTextArea1.getSelectedText();
        if (l == null || (l != null && l.trim().length() == 0)) {
            l = JOptionPane.showInputDialog(this, "Enter string to find...", "Find nex...", JOptionPane.PLAIN_MESSAGE);
        }
        if (l != null) {
            if (this.jTextArea1.findNext(l)) {
                this.jTextArea1.grabFocus();
                return;
            }
            JOptionPane.showMessageDialog(this, "Content string not found.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void jButton13_actionPerformed(ActionEvent e) {
        jSplitPane1.setSelectedComponent(jPanel2);
    }

    private void jButton14_actionPerformed(ActionEvent e) {
        jSplitPane1.setSelectedComponent(jPanel4);
    }

    private void jButton15_actionPerformed(ActionEvent e) {
        jSplitPane1.setSelectedComponent(jPanel1);
    }

    private void jPanel2_componentShown(ComponentEvent e) {
        jButton13.setSelected(true);
        jButton14.setSelected(false);
        jButton15.setSelected(false);
    }

    private void jPanel4_componentShown(ComponentEvent e) {
        jButton13.setSelected(false);
        jButton14.setSelected(true);
        jButton15.setSelected(false);
    }

    private void jPanel1_componentShown(ComponentEvent e) {
        jButton13.setSelected(false);
        jButton14.setSelected(false);
        jButton15.setSelected(true);
    }

    private class Cadre1_jButton1_actionAdapter implements ActionListener {

        FrameWorkConsole adaptee;

        Cadre1_jButton1_actionAdapter(FrameWorkConsole adaptee) {
            this.adaptee = adaptee;
        }

        public void actionPerformed(ActionEvent e) {
            adaptee.jButton1_actionPerformed(e);
        }
    }

    private class Cadre1_jButton2_actionAdapter implements ActionListener {

        FrameWorkConsole adaptee;

        Cadre1_jButton2_actionAdapter(FrameWorkConsole adaptee) {
            this.adaptee = adaptee;
        }

        public void actionPerformed(ActionEvent e) {
            adaptee.jButton2_actionPerformed(e);
        }
    }

    private class Cadre1_jButton3_actionAdapter implements ActionListener {

        FrameWorkConsole adaptee;

        Cadre1_jButton3_actionAdapter(FrameWorkConsole adaptee) {
            this.adaptee = adaptee;
        }

        public void actionPerformed(ActionEvent e) {
            adaptee.jButton3_actionPerformed(e);
        }
    }

    private class Cadre1_jButton4_actionAdapter implements ActionListener {

        FrameWorkConsole adaptee;

        Cadre1_jButton4_actionAdapter(FrameWorkConsole adaptee) {
            this.adaptee = adaptee;
        }

        public void actionPerformed(ActionEvent e) {
            adaptee.jButton4_actionPerformed(e);
        }
    }

    private class Cadre1_jTextArea1_caretAdapter implements CaretListener {

        FrameWorkConsole adaptee;

        Cadre1_jTextArea1_caretAdapter(FrameWorkConsole adaptee) {
            this.adaptee = adaptee;
        }

        public void caretUpdate(CaretEvent e) {
            adaptee.jTextArea1_caretUpdate(e);
        }
    }

    private class Cadre1_jTextArea1_propertyChangeAdapter implements PropertyChangeListener {

        FrameWorkConsole adaptee;

        Cadre1_jTextArea1_propertyChangeAdapter(FrameWorkConsole adaptee) {
            this.adaptee = adaptee;
        }

        public void propertyChange(PropertyChangeEvent e) {
            adaptee.jTextArea1_propertyChange(e);
        }
    }

    private class Cadre1_jButton5_actionAdapter implements ActionListener {

        FrameWorkConsole adaptee;

        Cadre1_jButton5_actionAdapter(FrameWorkConsole adaptee) {
            this.adaptee = adaptee;
        }

        public void actionPerformed(ActionEvent e) {
            adaptee.jButton5_actionPerformed(e);
        }
    }

    private class Cadre1_jTextField1_actionAdapter implements ActionListener {

        FrameWorkConsole adaptee;

        Cadre1_jTextField1_actionAdapter(FrameWorkConsole adaptee) {
            this.adaptee = adaptee;
        }

        public void actionPerformed(ActionEvent e) {
            adaptee.jTextField1_actionPerformed(e);
        }
    }

    class Console_jButton6_actionAdapter implements ActionListener {

        FrameWorkConsole adaptee;

        Console_jButton6_actionAdapter(FrameWorkConsole adaptee) {
            this.adaptee = adaptee;
        }

        public void actionPerformed(ActionEvent e) {
            adaptee.jButton6_actionPerformed(e);
        }
    }

    private class Console_jButton7_actionAdapter implements ActionListener {

        FrameWorkConsole adaptee;

        Console_jButton7_actionAdapter(FrameWorkConsole adaptee) {
            this.adaptee = adaptee;
        }

        public void actionPerformed(ActionEvent e) {
            adaptee.jButton7_actionPerformed(e);
        }
    }

    private class Console_jButton8_actionAdapter implements ActionListener {

        FrameWorkConsole adaptee;

        Console_jButton8_actionAdapter(FrameWorkConsole adaptee) {
            this.adaptee = adaptee;
        }

        public void actionPerformed(ActionEvent e) {
            adaptee.jButton8_actionPerformed(e);
        }
    }

    private class Cadre1_jButton9_actionAdapter implements ActionListener {

        FrameWorkConsole adaptee;

        Cadre1_jButton9_actionAdapter(FrameWorkConsole adaptee) {
            this.adaptee = adaptee;
        }

        public void actionPerformed(ActionEvent e) {
            adaptee.jButton9_actionPerformed(e);
        }
    }

    private class Console_jButton10_actionAdapter implements ActionListener {

        FrameWorkConsole adaptee;

        Console_jButton10_actionAdapter(FrameWorkConsole adaptee) {
            this.adaptee = adaptee;
        }

        public void actionPerformed(ActionEvent e) {
            adaptee.jButton10_actionPerformed(e);
        }
    }

    private class FrameWorkConsole_jButton11_actionAdapter implements ActionListener {

        private FrameWorkConsole adaptee;

        FrameWorkConsole_jButton11_actionAdapter(FrameWorkConsole adaptee) {
            this.adaptee = adaptee;
        }

        public void actionPerformed(ActionEvent e) {
            adaptee.jButton11_actionPerformed(e);
        }
    }
}
