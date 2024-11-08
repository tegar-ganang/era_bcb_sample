package net.sourceforge.tessboxeditor;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.DefaultKeyboardFocusManager;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.KeyEventDispatcher;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.dnd.DropTarget;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.channels.*;
import java.text.*;
import java.util.*;
import java.util.prefs.Preferences;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.*;
import net.sourceforge.tessboxeditor.components.*;
import net.sourceforge.tessboxeditor.datamodel.*;
import net.sourceforge.vietocr.utilities.*;
import net.sourceforge.vietpad.components.*;
import net.sourceforge.vietpad.utilities.LimitedLengthDocument;
import net.sourceforge.vietpad.utilities.TextUtilities;

public class Gui extends javax.swing.JFrame {

    public static final String APP_NAME = "jTessBoxEditor";

    public static final String TO_BE_IMPLEMENTED = "To be implemented in subclass";

    final String[] headers = { "Char", "X", "Y", "Width", "Height" };

    static final boolean MAC_OS_X = System.getProperty("os.name").startsWith("Mac");

    static final boolean WINDOWS = System.getProperty("os.name").toLowerCase().startsWith("windows");

    static final String EOL = System.getProperty("line.separator");

    static final String UTF8 = "UTF-8";

    protected ResourceBundle bundle;

    static final Preferences prefs = Preferences.userRoot().node("/net/sourceforge/tessboxeditor");

    private final Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();

    private int filterIndex;

    private FileFilter[] fileFilters;

    private File boxFile;

    private String currentDirectory, outputDirectory;

    private boolean boxChanged = true;

    protected boolean tableSelectAction;

    private List<TessBoxCollection> boxPages;

    protected TessBoxCollection boxes;

    protected short imageIndex;

    private List<BufferedImage> imageList;

    protected final File baseDir = Utilities.getBaseDir(Gui.this);

    DefaultTableModel tableModel;

    private boolean isTess2_0Format;

    /**
     * Creates new form JTessBoxEditor.
     */
    public Gui() {
        try {
            UIManager.setLookAndFeel(prefs.get("lookAndFeel", UIManager.getSystemLookAndFeelClassName()));
        } catch (Exception e) {
        }
        bundle = ResourceBundle.getBundle("net.sourceforge.tessboxeditor.Gui");
        initComponents();
        if (MAC_OS_X) {
            new MacOSXApplication(Gui.this);
            this.jMenuFile.remove(this.jSeparatorExit);
            this.jMenuFile.remove(this.jMenuItemExit);
            this.jMenuHelp.remove(this.jSeparatorAbout);
            this.jMenuHelp.remove(this.jMenuItemAbout);
        }
        boxPages = new ArrayList<TessBoxCollection>();
        new DropTarget(this.jLabelImage, new FileDropTargetListener(Gui.this));
        new DropTarget(this.jTabbedPaneBoxData, new FileDropTargetListener(Gui.this));
        new DropTarget(this.jTextArea, new FileDropTargetListener(Gui.this));
        this.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                quit();
            }

            @Override
            public void windowOpened(WindowEvent e) {
                updateSave(false);
                setExtendedState(prefs.getInt("windowState", Frame.NORMAL));
                populateMRUList();
            }
        });
        setSize(snap(prefs.getInt("frameWidth", 500), 300, screen.width), snap(prefs.getInt("frameHeight", 360), 150, screen.height));
        setLocation(snap(prefs.getInt("frameX", (screen.width - getWidth()) / 2), screen.x, screen.x + screen.width - getWidth()), snap(prefs.getInt("frameY", screen.y + (screen.height - getHeight()) / 3), screen.y, screen.y + screen.height - getHeight()));
        KeyEventDispatcher dispatcher = new KeyEventDispatcher() {

            @Override
            public boolean dispatchKeyEvent(KeyEvent e) {
                if (e.getID() == KeyEvent.KEY_PRESSED) {
                    if (e.getKeyCode() == KeyEvent.VK_F3) {
                        jButtonFind.doClick();
                    }
                }
                return false;
            }
        };
        DefaultKeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(dispatcher);
    }

    private int snap(final int ideal, final int min, final int max) {
        final int TOLERANCE = 0;
        return ideal < min + TOLERANCE ? min : (ideal > max - TOLERANCE ? max : ideal);
    }

    /**
     * Populates MRU List.
     */
    protected void populateMRUList() {
        JOptionPane.showMessageDialog(this, TO_BE_IMPLEMENTED);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    private void initComponents() {
        jFileChooser = new javax.swing.JFileChooser();
        jToolBar1 = new javax.swing.JToolBar();
        jButtonOpen = new javax.swing.JButton();
        jButtonSave = new javax.swing.JButton();
        jButtonReload = new javax.swing.JButton();
        jButtonMerge = new javax.swing.JButton();
        jButtonSplit = new javax.swing.JButton();
        jButtonInsert = new javax.swing.JButton();
        jButtonDelete = new javax.swing.JButton();
        jPanelSpinner = new javax.swing.JPanel();
        jLabelCharacter = new javax.swing.JLabel();
        jTextFieldChar = new javax.swing.JTextField();
        jTextFieldChar.setDocument(new LimitedLengthDocument(8));
        jButtonConvert = new javax.swing.JButton();
        jLabelX = new javax.swing.JLabel();
        jSpinnerX = new javax.swing.JSpinner();
        jLabelY = new javax.swing.JLabel();
        jSpinnerY = new javax.swing.JSpinner();
        jLabelW = new javax.swing.JLabel();
        jSpinnerW = new javax.swing.JSpinner();
        jLabelH = new javax.swing.JLabel();
        jSpinnerH = new javax.swing.JSpinner();
        jPanelStatus = new javax.swing.JPanel();
        jLabelStatus = new javax.swing.JLabel();
        jLabelPageNbr = new javax.swing.JLabel();
        jButtonPrevPage = new javax.swing.JButton();
        jButtonNextPage = new javax.swing.JButton();
        jTabbedPaneBoxData = new javax.swing.JTabbedPane();
        jPanelCoord = new javax.swing.JPanel();
        jScrollPaneCoord = new javax.swing.JScrollPane();
        jTable = new javax.swing.JTable();
        jPanelFind = new javax.swing.JPanel();
        jTextFieldFind = new javax.swing.JTextField();
        jButtonFind = new javax.swing.JButton();
        jScrollPaneBoxData = new javax.swing.JScrollPane();
        jTextArea = new javax.swing.JTextArea();
        jPanelBoxView = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jLabelCodepoint = new javax.swing.JLabel();
        jLabelCodepoint.setFont(jLabelCodepoint.getFont().deriveFont(14.0f));
        jLabelChar = new javax.swing.JLabel();
        jLabelChar.setFont(jLabelChar.getFont().deriveFont(14.0f));
        jLabelCodepointValue = new javax.swing.JLabel();
        jLabelCodepointValue.setFont(jLabelCodepointValue.getFont().deriveFont(14.0f));
        jLabelSubimage = new javax.swing.JLabel();
        jScrollPaneImage = new javax.swing.JScrollPane();
        jScrollPaneImage.getVerticalScrollBar().setUnitIncrement(20);
        jScrollPaneImage.getHorizontalScrollBar().setUnitIncrement(20);
        jLabelImage = new JImageLabel();
        jMenuBar = new javax.swing.JMenuBar();
        jMenuFile = new javax.swing.JMenu();
        jMenuItemOpen = new javax.swing.JMenuItem();
        jMenuItemSave = new javax.swing.JMenuItem();
        jMenuItemSaveAs = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        jMenuRecentFiles = new javax.swing.JMenu();
        jSeparatorExit = new javax.swing.JPopupMenu.Separator();
        jMenuItemExit = new javax.swing.JMenuItem();
        jMenuEdit = new javax.swing.JMenu();
        jMenuItemMerge = new javax.swing.JMenuItem();
        jMenuItemSplit = new javax.swing.JMenuItem();
        jMenuItemInsert = new javax.swing.JMenuItem();
        jMenuItemDelete = new javax.swing.JMenuItem();
        jMenuSettings = new javax.swing.JMenu();
        jMenuItemFont = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        jMenuLookAndFeel = new javax.swing.JMenu();
        jMenuTools = new javax.swing.JMenu();
        jMenuItemMergeTiff = new javax.swing.JMenuItem();
        jMenuItemGenerateTiffBox = new javax.swing.JMenuItem();
        jMenuHelp = new javax.swing.JMenu();
        jMenuItemHelp = new javax.swing.JMenuItem();
        jSeparatorAbout = new javax.swing.JPopupMenu.Separator();
        jMenuItemAbout = new javax.swing.JMenuItem();
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("net/sourceforge/tessboxeditor/Gui");
        jFileChooser.setDialogTitle(bundle.getString("jButtonOpen.ToolTipText"));
        currentDirectory = prefs.get("currentDirectory", null);
        outputDirectory = prefs.get("outputDirectory", null);
        jFileChooser.setCurrentDirectory(currentDirectory == null ? null : new File(currentDirectory));
        filterIndex = prefs.getInt("filterIndex", 0);
        FileFilter allImageFilter = new SimpleFilter("bmp;jpg;jpeg;png;tif;tiff", bundle.getString("All_Image_Files"));
        FileFilter pngFilter = new SimpleFilter("png", "PNG");
        FileFilter tiffFilter = new SimpleFilter("tif;tiff", "TIFF");
        jFileChooser.setAcceptAllFileFilterUsed(false);
        jFileChooser.addChoosableFileFilter(allImageFilter);
        jFileChooser.addChoosableFileFilter(pngFilter);
        jFileChooser.addChoosableFileFilter(tiffFilter);
        fileFilters = jFileChooser.getChoosableFileFilters();
        if (filterIndex < fileFilters.length) {
            jFileChooser.setFileFilter(fileFilters[filterIndex]);
        }
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("jTessBoxEditor");
        jToolBar1.setRollover(true);
        jButtonOpen.setText(bundle.getString("jButtonOpen.Text"));
        jButtonOpen.setToolTipText(bundle.getString("jButtonOpen.ToolTipText"));
        jButtonOpen.setFocusable(false);
        jButtonOpen.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButtonOpen.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButtonOpen.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonOpenActionPerformed(evt);
            }
        });
        jToolBar1.add(jButtonOpen);
        jButtonSave.setText(bundle.getString("jButtonSave.Text"));
        jButtonSave.setToolTipText(bundle.getString("jButtonSave.ToolTipText"));
        jButtonSave.setFocusable(false);
        jButtonSave.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButtonSave.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButtonSave.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSaveActionPerformed(evt);
            }
        });
        jToolBar1.add(jButtonSave);
        jButtonReload.setText("Reload");
        jButtonReload.setToolTipText("Reload Box File");
        jButtonReload.setFocusable(false);
        jButtonReload.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButtonReload.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButtonReload.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonReloadActionPerformed(evt);
            }
        });
        jToolBar1.add(jButtonReload);
        jToolBar1.add(Box.createHorizontalGlue());
        jButtonMerge.setText(bundle.getString("jButtonMerge.Text"));
        jButtonMerge.setToolTipText(bundle.getString("jButtonMerge.ToolTipText"));
        jButtonMerge.setFocusable(false);
        jButtonMerge.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButtonMerge.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButtonMerge.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonMergeActionPerformed(evt);
            }
        });
        jToolBar1.add(jButtonMerge);
        jButtonSplit.setText(bundle.getString("jButtonSplit.Text"));
        jButtonSplit.setToolTipText(bundle.getString("jButtonSplit.ToolTipText"));
        jButtonSplit.setFocusable(false);
        jButtonSplit.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButtonSplit.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButtonSplit.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSplitActionPerformed(evt);
            }
        });
        jToolBar1.add(jButtonSplit);
        jButtonInsert.setText(bundle.getString("jButtonInsert.Text"));
        jButtonInsert.setToolTipText(bundle.getString("jButtonInsert.ToolTipText"));
        jButtonInsert.setFocusable(false);
        jButtonInsert.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButtonInsert.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButtonInsert.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonInsertActionPerformed(evt);
            }
        });
        jToolBar1.add(jButtonInsert);
        jButtonDelete.setText(bundle.getString("jButtonDelete.Text"));
        jButtonDelete.setToolTipText(bundle.getString("jButtonDelete.ToolTipText"));
        jButtonDelete.setFocusable(false);
        jButtonDelete.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButtonDelete.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButtonDelete.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonDeleteActionPerformed(evt);
            }
        });
        jToolBar1.add(jButtonDelete);
        jToolBar1.add(Box.createHorizontalGlue());
        jLabelCharacter.setLabelFor(jTextFieldChar);
        jLabelCharacter.setText("Character");
        jPanelSpinner.add(jLabelCharacter);
        jTextFieldChar.setColumns(4);
        jTextFieldChar.setEnabled(false);
        jTextFieldChar.setMargin(new java.awt.Insets(0, 2, 0, 2));
        jTextFieldChar.setPreferredSize(new java.awt.Dimension(38, 24));
        jTextFieldChar.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldCharActionPerformed(evt);
            }
        });
        jPanelSpinner.add(jTextFieldChar);
        jButtonConvert.setIcon(new javax.swing.ImageIcon(getClass().getResource("/net/sourceforge/tessboxeditor/icons/tools.png")));
        jButtonConvert.setToolTipText("<html>Convert NCR and Escape<br/>Sequence to Unicode</html>");
        jButtonConvert.setPreferredSize(new java.awt.Dimension(20, 20));
        jButtonConvert.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonConvertActionPerformed(evt);
            }
        });
        jPanelSpinner.add(jButtonConvert);
        jPanelSpinner.add(Box.createHorizontalStrut(10));
        jLabelX.setLabelFor(jSpinnerX);
        jLabelX.setText("X");
        jPanelSpinner.add(jLabelX);
        jSpinnerX.setEditor(new javax.swing.JSpinner.NumberEditor(jSpinnerX, "#"));
        jSpinnerX.setEnabled(false);
        jSpinnerX.setPreferredSize(new java.awt.Dimension(63, 22));
        jSpinnerX.addChangeListener(new javax.swing.event.ChangeListener() {

            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSpinnerXStateChanged(evt);
            }
        });
        jPanelSpinner.add(jSpinnerX);
        jLabelY.setLabelFor(jSpinnerY);
        jLabelY.setText("Y");
        jPanelSpinner.add(jLabelY);
        jSpinnerY.setEditor(new javax.swing.JSpinner.NumberEditor(jSpinnerY, "#"));
        jSpinnerY.setEnabled(false);
        jSpinnerY.setPreferredSize(new java.awt.Dimension(63, 22));
        jSpinnerY.addChangeListener(new javax.swing.event.ChangeListener() {

            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSpinnerYStateChanged(evt);
            }
        });
        jPanelSpinner.add(jSpinnerY);
        jLabelW.setLabelFor(jSpinnerW);
        jLabelW.setText("W");
        jPanelSpinner.add(jLabelW);
        jSpinnerW.setModel(new javax.swing.SpinnerNumberModel());
        jSpinnerW.setEditor(new javax.swing.JSpinner.NumberEditor(jSpinnerW, "#"));
        jSpinnerW.setEnabled(false);
        jSpinnerW.setPreferredSize(new java.awt.Dimension(48, 22));
        jSpinnerW.addChangeListener(new javax.swing.event.ChangeListener() {

            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSpinnerWStateChanged(evt);
            }
        });
        jPanelSpinner.add(jSpinnerW);
        jLabelH.setLabelFor(jSpinnerH);
        jLabelH.setText("H");
        jPanelSpinner.add(jLabelH);
        jSpinnerH.setModel(new javax.swing.SpinnerNumberModel());
        jSpinnerH.setEditor(new javax.swing.JSpinner.NumberEditor(jSpinnerH, "#"));
        jSpinnerH.setEnabled(false);
        jSpinnerH.setPreferredSize(new java.awt.Dimension(48, 22));
        jSpinnerH.addChangeListener(new javax.swing.event.ChangeListener() {

            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSpinnerHStateChanged(evt);
            }
        });
        jPanelSpinner.add(jSpinnerH);
        jToolBar1.add(jPanelSpinner);
        jToolBar1.add(Box.createHorizontalGlue());
        getContentPane().add(jToolBar1, java.awt.BorderLayout.PAGE_START);
        jPanelStatus.add(jLabelStatus);
        jPanelStatus.add(jLabelPageNbr);
        this.jPanelStatus.add(Box.createHorizontalStrut(10));
        jButtonPrevPage.setIcon(new javax.swing.ImageIcon(getClass().getResource("/net/sourceforge/tessboxeditor/icons/PrevPage.gif")));
        jButtonPrevPage.setToolTipText(bundle.getString("jButtonPrevPage.ToolTipText"));
        jButtonPrevPage.setFocusable(false);
        jButtonPrevPage.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButtonPrevPage.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButtonPrevPage.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonPrevPageActionPerformed(evt);
            }
        });
        jPanelStatus.add(jButtonPrevPage);
        jButtonNextPage.setIcon(new javax.swing.ImageIcon(getClass().getResource("/net/sourceforge/tessboxeditor/icons/NextPage.gif")));
        jButtonNextPage.setToolTipText(bundle.getString("jButtonNextPage.ToolTipText"));
        jButtonNextPage.setFocusable(false);
        jButtonNextPage.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButtonNextPage.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButtonNextPage.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonNextPageActionPerformed(evt);
            }
        });
        jPanelStatus.add(jButtonNextPage);
        getContentPane().add(jPanelStatus, java.awt.BorderLayout.SOUTH);
        jPanelCoord.setLayout(new java.awt.BorderLayout());
        jScrollPaneCoord.setPreferredSize(new java.awt.Dimension(200, 275));
        jTable.setModel(new javax.swing.table.DefaultTableModel(new Object[][] {}, new String[] { "Char", "X", "Y", "Width", "Height" }) {

            boolean[] canEdit = new boolean[] { false, false, false, false, false };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        });
        jTable.setFillsViewportHeight(true);
        jTable.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        jScrollPaneCoord.setViewportView(jTable);
        tableModel = (DefaultTableModel) this.jTable.getModel();
        ListSelectionModel cellSelectionModel = jTable.getSelectionModel();
        cellSelectionModel.addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    int selectedIndex = jTable.getSelectedRow();
                    if (selectedIndex != -1) {
                        if (!((JImageLabel) jLabelImage).isBoxClickAction()) {
                            boxes.deselectAll();
                        }
                        List<TessBox> boxesOfCurPage = boxes.toList();
                        for (int index : jTable.getSelectedRows()) {
                            TessBox box = boxesOfCurPage.get(index);
                            box.setSelected(true);
                            jLabelImage.scrollRectToVisible(box.getRect());
                        }
                        jLabelImage.repaint();
                        if (jTable.getSelectedRows().length == 1) {
                            enableReadout(true);
                            jTextFieldChar.setText((String) tableModel.getValueAt(selectedIndex, 0));
                            jLabelChar.setText(jTextFieldChar.getText());
                            jLabelCodepointValue.setText(Utilities.toHex(jTextFieldChar.getText()));
                            Icon icon = jLabelImage.getIcon();
                            TessBox curBox = boxesOfCurPage.get(selectedIndex);
                            Rectangle rect = curBox.getRect();
                            try {
                                Image subImage = ((BufferedImage) ((ImageIcon) icon).getImage()).getSubimage(rect.x, rect.y, rect.width, rect.height);
                                ImageIconScalable subIcon = new ImageIconScalable(subImage);
                                subIcon.setScaledFactor(4);
                                jLabelSubimage.setIcon(subIcon);
                            } catch (Exception exc) {
                            }
                            tableSelectAction = true;
                            jSpinnerX.setValue(rect.x);
                            jSpinnerY.setValue(rect.y);
                            jSpinnerH.setValue(rect.height);
                            jSpinnerW.setValue(rect.width);
                            tableSelectAction = false;
                        } else {
                            enableReadout(false);
                        }
                    } else {
                        boxes.deselectAll();
                        jLabelImage.repaint();
                        enableReadout(false);
                        tableSelectAction = true;
                        resetReadout();
                        tableSelectAction = false;
                    }
                }
            }
        });
        TableCellRenderer tcr = this.jTable.getDefaultRenderer(String.class);
        DefaultTableCellRenderer dtcr = (DefaultTableCellRenderer) tcr;
        dtcr.setHorizontalAlignment(DefaultTableCellRenderer.CENTER);
        ((JLabel) jTable.getTableHeader().getDefaultRenderer()).setHorizontalAlignment(SwingConstants.CENTER);
        ((JImageLabel) this.jLabelImage).setTable(jTable);
        jTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("control C"), "none");
        jTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("control X"), "none");
        jTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("control V"), "none");
        jPanelCoord.add(jScrollPaneCoord, java.awt.BorderLayout.CENTER);
        jTextFieldFind.setPreferredSize(new java.awt.Dimension(200, 20));
        jTextFieldFind.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldFindActionPerformed(evt);
            }
        });
        jPanelFind.add(jTextFieldFind);
        jButtonFind.setText(bundle.getString("jButtonFind.Text"));
        jButtonFind.setToolTipText(bundle.getString("jButtonFind.ToolTipText"));
        jButtonFind.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonFindActionPerformed(evt);
            }
        });
        jPanelFind.add(jButtonFind);
        jPanelCoord.add(jPanelFind, java.awt.BorderLayout.SOUTH);
        jTabbedPaneBoxData.addTab("Box Coordinates", jPanelCoord);
        jTextArea.setColumns(20);
        jTextArea.setEditable(false);
        jTextArea.setRows(5);
        jTextArea.setMargin(new java.awt.Insets(8, 8, 2, 2));
        jScrollPaneBoxData.setViewportView(jTextArea);
        jTabbedPaneBoxData.addTab("Box Data", jScrollPaneBoxData);
        jPanelBoxView.setBackground(java.awt.Color.lightGray);
        jPanelBoxView.setLayout(new java.awt.BorderLayout());
        jPanel1.setBackground(java.awt.Color.lightGray);
        jPanel1.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        jLabelCodepoint.setText("Char/Codepoint:");
        jPanel1.add(jLabelCodepoint);
        jPanel1.add(jLabelChar);
        jLabelChar.getAccessibleContext().setAccessibleName("Char");
        jPanel1.add(jLabelCodepointValue);
        jLabelCodepointValue.getAccessibleContext().setAccessibleName("Codepoint");
        jPanelBoxView.add(jPanel1, java.awt.BorderLayout.NORTH);
        jLabelSubimage.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jPanelBoxView.add(jLabelSubimage, java.awt.BorderLayout.CENTER);
        jTabbedPaneBoxData.addTab("Box View", jPanelBoxView);
        getContentPane().add(jTabbedPaneBoxData, java.awt.BorderLayout.WEST);
        jLabelImage.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jScrollPaneImage.setViewportView(jLabelImage);
        getContentPane().add(jScrollPaneImage, java.awt.BorderLayout.CENTER);
        jMenuFile.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/tessboxeditor/Gui").getString("jMenuFile.Mnemonic").charAt(0));
        jMenuFile.setText(bundle.getString("jMenuFile.Text"));
        jMenuItemOpen.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItemOpen.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/tessboxeditor/Gui").getString("jMenuItemOpen.Mnemonic").charAt(0));
        jMenuItemOpen.setText(bundle.getString("jMenuItemOpen.Text"));
        jMenuItemOpen.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemOpenActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemOpen);
        jMenuItemSave.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItemSave.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/tessboxeditor/Gui").getString("jMenuItemSave.Mnemonic").charAt(0));
        jMenuItemSave.setText(bundle.getString("jMenuItemSave.Text"));
        jMenuItemSave.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSaveActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemSave);
        jMenuItemSaveAs.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        jMenuItemSaveAs.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/tessboxeditor/Gui").getString("jMenuItemSaveAs.Mnemonic").charAt(0));
        jMenuItemSaveAs.setText(bundle.getString("jMenuItemSaveAs.Text"));
        jMenuItemSaveAs.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSaveAsActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemSaveAs);
        jMenuFile.add(jSeparator1);
        jMenuRecentFiles.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/tessboxeditor/Gui").getString("jMenuRecentFiles.Mnemonic").charAt(0));
        jMenuRecentFiles.setText(bundle.getString("jMenuRecentFiles.Text"));
        jMenuFile.add(jMenuRecentFiles);
        jMenuFile.add(jSeparatorExit);
        jMenuItemExit.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/tessboxeditor/Gui").getString("jMenuItemExit.Mnemonic").charAt(0));
        jMenuItemExit.setText(bundle.getString("jMenuItemExit.Text"));
        jMenuItemExit.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemExitActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemExit);
        jMenuBar.add(jMenuFile);
        jMenuEdit.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/tessboxeditor/Gui").getString("jMenuEdit.Mnemonic").charAt(0));
        jMenuEdit.setText(bundle.getString("jMenuEdit.Text"));
        jMenuItemMerge.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItemMerge.setText("Merge");
        jMenuItemMerge.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemMergeActionPerformed(evt);
            }
        });
        jMenuEdit.add(jMenuItemMerge);
        jMenuItemSplit.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_X, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItemSplit.setText("Split");
        jMenuItemSplit.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSplitActionPerformed(evt);
            }
        });
        jMenuEdit.add(jMenuItemSplit);
        jMenuItemInsert.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_V, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItemInsert.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/tessboxeditor/Gui").getString("jMenuItemInsert.Mnemonic").charAt(0));
        jMenuItemInsert.setText(bundle.getString("jMenuItemInsert.Text"));
        jMenuItemInsert.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemInsertActionPerformed(evt);
            }
        });
        jMenuEdit.add(jMenuItemInsert);
        jMenuItemDelete.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DELETE, 0));
        jMenuItemDelete.setText("Delete");
        jMenuItemDelete.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemDeleteActionPerformed(evt);
            }
        });
        jMenuEdit.add(jMenuItemDelete);
        jMenuBar.add(jMenuEdit);
        jMenuSettings.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/tessboxeditor/Gui").getString("jMenuSettings.Mnemonic").charAt(0));
        jMenuSettings.setText("Settings");
        jMenuItemFont.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_T, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItemFont.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/tessboxeditor/Gui").getString("jMenuItemFont.Mnemonic").charAt(0));
        jMenuItemFont.setText(bundle.getString("jMenuItemFont.Text"));
        jMenuItemFont.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemFontActionPerformed(evt);
            }
        });
        jMenuSettings.add(jMenuItemFont);
        jMenuSettings.add(jSeparator3);
        jMenuLookAndFeel.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/tessboxeditor/Gui").getString("jMenuLookAndFeel.Mnemonic").charAt(0));
        jMenuLookAndFeel.setText(bundle.getString("jMenuLookAndFeel.Text"));
        jMenuSettings.add(jMenuLookAndFeel);
        jMenuBar.add(jMenuSettings);
        jMenuTools.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/tessboxeditor/Gui").getString("jMenuTools.Mnemonic").charAt(0));
        jMenuTools.setText(bundle.getString("jMenuTools.Text"));
        jMenuItemMergeTiff.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_M, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItemMergeTiff.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/tessboxeditor/Gui").getString("jMenuItemMergeTiff.Mnemonic").charAt(0));
        jMenuItemMergeTiff.setText(bundle.getString("jMenuItemMergeTiff.Text"));
        jMenuItemMergeTiff.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemMergeTiffActionPerformed(evt);
            }
        });
        jMenuTools.add(jMenuItemMergeTiff);
        jMenuItemGenerateTiffBox.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_G, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItemGenerateTiffBox.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/tessboxeditor/Gui").getString("jMenuItemGenerateTiffBox.Mnemonic").charAt(0));
        jMenuItemGenerateTiffBox.setText(bundle.getString("jMenuItemGenerateTiffBox.Text"));
        jMenuItemGenerateTiffBox.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemGenerateTiffBoxActionPerformed(evt);
            }
        });
        jMenuTools.add(jMenuItemGenerateTiffBox);
        jMenuBar.add(jMenuTools);
        jMenuHelp.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/tessboxeditor/Gui").getString("jMenuHelp.Mnemonic").charAt(0));
        jMenuHelp.setText(bundle.getString("jMenuHelp.Text"));
        jMenuItemHelp.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/tessboxeditor/Gui").getString("jMenuItemHelp.Mnemonic").charAt(0));
        jMenuItemHelp.setText(bundle.getString("jMenuItemHelp.Text"));
        jMenuItemHelp.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemHelpActionPerformed(evt);
            }
        });
        jMenuHelp.add(jMenuItemHelp);
        jMenuHelp.add(jSeparatorAbout);
        jMenuItemAbout.setMnemonic(java.util.ResourceBundle.getBundle("net/sourceforge/tessboxeditor/Gui").getString("jMenuItemAbout.Mnemonic").charAt(0));
        jMenuItemAbout.setText(bundle.getString("jMenuItemAbout.Text"));
        jMenuItemAbout.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemAboutActionPerformed(evt);
            }
        });
        jMenuHelp.add(jMenuItemAbout);
        jMenuBar.add(jMenuHelp);
        setJMenuBar(jMenuBar);
        pack();
    }

    private void jMenuItemOpenActionPerformed(java.awt.event.ActionEvent evt) {
        if (jFileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            currentDirectory = jFileChooser.getCurrentDirectory().getPath();
            openFile(jFileChooser.getSelectedFile());
            for (int i = 0; i < fileFilters.length; i++) {
                if (fileFilters[i] == jFileChooser.getFileFilter()) {
                    filterIndex = i;
                    break;
                }
            }
        }
    }

    public void openFile(final File selectedFile) {
        if (!selectedFile.exists()) {
            JOptionPane.showMessageDialog(this, bundle.getString("File_not_exist"), APP_NAME, JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!promptToSave()) {
            return;
        }
        getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        getGlassPane().setVisible(true);
        SwingWorker loadWorker = new SwingWorker<Void, Void>() {

            @Override
            protected Void doInBackground() throws Exception {
                readImageFile(selectedFile);
                updateMRUList(selectedFile.getPath());
                int lastDot = selectedFile.getName().lastIndexOf(".");
                boxFile = new File(selectedFile.getParentFile(), selectedFile.getName().substring(0, lastDot) + ".box");
                readBoxFile(boxFile);
                return null;
            }

            @Override
            protected void done() {
                getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                getGlassPane().setVisible(false);
            }
        };
        loadWorker.execute();
    }

    void readImageFile(File selectedFile) {
        try {
            imageList = ImageIOHelper.getImageList(selectedFile);
            if (imageList == null) {
                JOptionPane.showMessageDialog(this, bundle.getString("Cannotloadimage"), APP_NAME, JOptionPane.ERROR_MESSAGE);
                return;
            }
            imageIndex = 0;
            loadImage();
            this.jScrollPaneImage.getViewport().setViewPosition(new Point(0, 0));
            this.setTitle(APP_NAME + " - " + selectedFile.getName());
        } catch (OutOfMemoryError oome) {
            JOptionPane.showMessageDialog(this, oome.getMessage(), "Out-Of-Memory Exception", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), APP_NAME, JOptionPane.ERROR_MESSAGE);
        }
    }

    void readBoxFile(final File boxFile) {
        if (boxFile.exists()) {
            try {
                boxPages.clear();
                BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(boxFile), "UTF8"));
                this.jTextArea.read(in, null);
                in.close();
                String[] boxdata = this.jTextArea.getText().split("\\n");
                if (boxdata.length > 0) {
                    isTess2_0Format = boxdata[0].split("\\s+").length == 5;
                }
                int startBoxIndex = 0;
                for (int curPage = 0; curPage < imageList.size(); curPage++) {
                    TessBoxCollection boxCol = new TessBoxCollection();
                    int pageHeight = imageList.get(curPage).getHeight();
                    for (int i = startBoxIndex; i < boxdata.length; i++) {
                        String[] items = boxdata[i].split("\\s+");
                        if (items.length < 5 || items.length > 6) {
                            continue;
                        }
                        String chrs = items[0];
                        int x = Integer.parseInt(items[1]);
                        int y = Integer.parseInt(items[2]);
                        int w = Integer.parseInt(items[3]) - x;
                        int h = Integer.parseInt(items[4]) - y;
                        y = pageHeight - y - h;
                        short page;
                        if (items.length == 6) {
                            page = Short.parseShort(items[5]);
                        } else {
                            page = 0;
                        }
                        if (page > curPage) {
                            startBoxIndex = i;
                            break;
                        }
                        boxCol.add(new TessBox(chrs, new Rectangle(x, y, w, h), page));
                    }
                    boxPages.add(boxCol);
                }
                loadTable();
                updateSave(false);
            } catch (OutOfMemoryError oome) {
                JOptionPane.showMessageDialog(this, oome.getMessage(), "Out-Of-Memory Exception", JOptionPane.ERROR_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), APP_NAME, JOptionPane.ERROR_MESSAGE);
            }
        } else {
            tableModel.setDataVector((Object[][]) null, (Object[]) null);
            ((JImageLabel) this.jLabelImage).setBoxes(null);
            jTextArea.setText(null);
        }
    }

    void loadTable() {
        if (!this.boxPages.isEmpty()) {
            boxes = this.boxPages.get(imageIndex);
            tableModel.setDataVector(boxes.getTableDataList().toArray(new String[0][5]), headers);
            ((JImageLabel) this.jLabelImage).setBoxes(boxes);
        }
    }

    /**
     * Displays a dialog to save changes.
     *
     * @return false if user canceled, true else
     */
    protected boolean promptToSave() {
        if (!boxChanged) {
            return true;
        }
        switch(JOptionPane.showConfirmDialog(this, bundle.getString("Do_you_want_to_save_the_changes_to_") + (boxFile == null ? bundle.getString("Untitled") : boxFile.getName()) + "?", APP_NAME, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE)) {
            case JOptionPane.YES_OPTION:
                return saveAction();
            case JOptionPane.NO_OPTION:
                return true;
            default:
                return false;
        }
    }

    boolean saveAction() {
        if (boxFile == null || !boxFile.exists()) {
            return saveFileDlg();
        } else {
            return saveBoxFile();
        }
    }

    boolean saveFileDlg() {
        JFileChooser saveChooser = new JFileChooser(outputDirectory);
        FileFilter textFilter = new SimpleFilter("box;txt", "Box Files");
        saveChooser.addChoosableFileFilter(textFilter);
        saveChooser.setDialogTitle(bundle.getString("Save_As"));
        if (boxFile != null) {
            saveChooser.setSelectedFile(boxFile);
        }
        if (saveChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            outputDirectory = saveChooser.getCurrentDirectory().getPath();
            File f = saveChooser.getSelectedFile();
            if (saveChooser.getFileFilter() == textFilter) {
                if (!f.getName().endsWith(".box")) {
                    f = new File(f.getPath() + ".box");
                }
                if (boxFile != null && boxFile.getPath().equals(f.getPath())) {
                    if (JOptionPane.NO_OPTION == JOptionPane.showConfirmDialog(Gui.this, boxFile.getName() + bundle.getString("file_already_exist"), bundle.getString("Confirm_Save_As"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE)) {
                        return false;
                    }
                } else {
                    boxFile = f;
                }
            }
            return saveBoxFile();
        } else {
            return false;
        }
    }

    boolean saveBoxFile() {
        getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        getGlassPane().setVisible(true);
        try {
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(boxFile), UTF8));
            out.write(formatOutputString());
            out.close();
            updateSave(false);
        } catch (OutOfMemoryError oome) {
            JOptionPane.showMessageDialog(this, oome.getMessage(), bundle.getString("OutOfMemoryError"), JOptionPane.ERROR_MESSAGE);
        } catch (FileNotFoundException fnfe) {
        } catch (Exception ex) {
        } finally {
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    getGlassPane().setVisible(false);
                }
            });
        }
        return true;
    }

    String formatOutputString() {
        StringBuilder sb = new StringBuilder();
        for (short i = 0; i < imageList.size(); i++) {
            int pageHeight = ((BufferedImage) imageList.get(i)).getHeight();
            for (TessBox box : boxPages.get(i).toList()) {
                Rectangle rect = box.getRect();
                sb.append(String.format("%s %d %d %d %d %d", box.getChrs(), rect.x, pageHeight - rect.y - rect.height, rect.x + rect.width, pageHeight - rect.y, i)).append(EOL);
            }
        }
        if (isTess2_0Format) {
            return sb.toString().replace(" 0" + EOL, EOL);
        }
        return sb.toString();
    }

    /**
     * Update MRU List.
     *
     * @param fileName
     */
    protected void updateMRUList(String fileName) {
    }

    private void jMenuItemSaveActionPerformed(java.awt.event.ActionEvent evt) {
        saveAction();
    }

    private void jMenuItemExitActionPerformed(java.awt.event.ActionEvent evt) {
        quit();
    }

    void quit() {
        if (!promptToSave()) {
            return;
        }
        if (currentDirectory != null) {
            prefs.put("currentDirectory", currentDirectory);
        }
        if (outputDirectory != null) {
            prefs.put("outputDirectory", outputDirectory);
        }
        prefs.putInt("windowState", getExtendedState());
        if (getExtendedState() == NORMAL) {
            prefs.putInt("frameHeight", getHeight());
            prefs.putInt("frameWidth", getWidth());
            prefs.putInt("frameX", getX());
            prefs.putInt("frameY", getY());
        }
        prefs.putInt("filterIndex", filterIndex);
        System.exit(0);
    }

    private void jMenuItemAboutActionPerformed(java.awt.event.ActionEvent evt) {
        about();
    }

    void about() {
        try {
            Properties config = new Properties();
            config.loadFromXML(getClass().getResourceAsStream("config.xml"));
            String version = config.getProperty("Version");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
            Date releaseDate = sdf.parse(config.getProperty("ReleaseDate"));
            JOptionPane.showMessageDialog(this, APP_NAME + " " + version + " © 2011\n" + "Box Editor for Tesseract OCR Data\n" + DateFormat.getDateInstance(DateFormat.LONG).format(releaseDate) + "\nhttp://vietocr.sourceforge.net", jMenuItemAbout.getText(), JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private void jButtonPrevPageActionPerformed(java.awt.event.ActionEvent evt) {
        if (imageList != null && imageIndex > 0) {
            --imageIndex;
            loadImage();
            loadTable();
        }
    }

    private void jButtonNextPageActionPerformed(java.awt.event.ActionEvent evt) {
        if (imageList != null && imageIndex < imageList.size() - 1) {
            ++imageIndex;
            loadImage();
            loadTable();
        }
    }

    void loadImage() {
        this.jLabelImage.setIcon(new ImageIcon(imageList.get(imageIndex)));
        if (boxes != null) {
            boxes.deselectAll();
        }
        this.jLabelImage.repaint();
        this.jLabelPageNbr.setText(String.format("Page: %d of %d", imageIndex + 1, imageList.size()));
        setButton();
        tableSelectAction = true;
        resetReadout();
        tableSelectAction = false;
    }

    void setButton() {
        if (imageIndex == 0) {
            this.jButtonPrevPage.setEnabled(false);
        } else {
            this.jButtonPrevPage.setEnabled(true);
        }
        if (imageIndex == imageList.size() - 1) {
            this.jButtonNextPage.setEnabled(false);
        } else {
            this.jButtonNextPage.setEnabled(true);
        }
    }

    void resetReadout() {
        jTextFieldChar.setText(null);
        jLabelChar.setText(null);
        jLabelCodepointValue.setText(null);
        jSpinnerH.setValue(0);
        jSpinnerW.setValue(0);
        jSpinnerX.setValue(0);
        jSpinnerY.setValue(0);
        jLabelSubimage.setIcon(null);
    }

    void enableReadout(boolean enabled) {
        jTextFieldChar.setEnabled(enabled);
        jSpinnerX.setEnabled(enabled);
        jSpinnerY.setEnabled(enabled);
        jSpinnerH.setEnabled(enabled);
        jSpinnerW.setEnabled(enabled);
    }

    /**
     * Updates the Save action.
     *
     * @param modified whether file has been modified
     */
    void updateSave(boolean modified) {
        if (boxChanged != modified) {
            boxChanged = modified;
            this.jButtonSave.setEnabled(modified);
            this.jMenuItemSave.setEnabled(modified);
            rootPane.putClientProperty("windowModified", Boolean.valueOf(modified));
        }
    }

    private void jButtonOpenActionPerformed(java.awt.event.ActionEvent evt) {
        jMenuItemOpenActionPerformed(evt);
    }

    private void jButtonSaveActionPerformed(java.awt.event.ActionEvent evt) {
        jMenuItemSaveActionPerformed(evt);
    }

    private void jButtonReloadActionPerformed(java.awt.event.ActionEvent evt) {
        if (boxFile != null) {
            jButtonReload.setEnabled(false);
            getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            getGlassPane().setVisible(true);
            SwingWorker loadWorker = new SwingWorker<Void, Void>() {

                @Override
                protected Void doInBackground() throws Exception {
                    readBoxFile(boxFile);
                    return null;
                }

                @Override
                protected void done() {
                    jButtonReload.setEnabled(true);
                    getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    getGlassPane().setVisible(false);
                }
            };
            loadWorker.execute();
        }
    }

    void jMenuItemFontActionPerformed(java.awt.event.ActionEvent evt) {
        JOptionPane.showMessageDialog(this, TO_BE_IMPLEMENTED);
    }

    private void jMenuItemHelpActionPerformed(java.awt.event.ActionEvent evt) {
        final String readme = bundle.getString("readme");
        if (MAC_OS_X) {
            try {
                final File supportDir = new File(System.getProperty("user.home") + "/Library/Application Support/" + APP_NAME);
                if (!supportDir.exists()) {
                    supportDir.mkdirs();
                }
                File helpFile = new File(supportDir, "readme.html");
                copyFileFromJarToSupportDir(helpFile);
                Runtime.getRuntime().exec(new String[] { "open", "-b", "com.apple.helpviewer", readme }, null, supportDir);
            } catch (IOException x) {
                x.printStackTrace();
            }
        } else {
            if (helptopicsFrame == null) {
                helptopicsFrame = new JFrame(jMenuItemHelp.getText());
                helptopicsFrame.getContentPane().setLayout(new BorderLayout());
                HtmlPane helpPane = new HtmlPane(readme);
                helptopicsFrame.getContentPane().add(helpPane, BorderLayout.CENTER);
                helptopicsFrame.getContentPane().add(helpPane.getStatusBar(), BorderLayout.SOUTH);
                helptopicsFrame.pack();
                helptopicsFrame.setLocation((screen.width - helptopicsFrame.getWidth()) / 2, 40);
            }
            helptopicsFrame.setVisible(true);
        }
    }

    private void copyFileFromJarToSupportDir(File helpFile) throws IOException {
        if (!helpFile.exists()) {
            final ReadableByteChannel input = Channels.newChannel(ClassLoader.getSystemResourceAsStream(helpFile.getName()));
            final FileChannel output = new FileOutputStream(helpFile).getChannel();
            output.transferFrom(input, 0, 1000000L);
            output.close();
            input.close();
        }
    }

    private void jMenuItemSaveAsActionPerformed(java.awt.event.ActionEvent evt) {
        saveFileDlg();
    }

    private void jSpinnerXStateChanged(javax.swing.event.ChangeEvent evt) {
        stateChanged(evt);
    }

    private void jSpinnerYStateChanged(javax.swing.event.ChangeEvent evt) {
        stateChanged(evt);
    }

    private void jSpinnerWStateChanged(javax.swing.event.ChangeEvent evt) {
        stateChanged(evt);
    }

    private void jSpinnerHStateChanged(javax.swing.event.ChangeEvent evt) {
        stateChanged(evt);
    }

    void stateChanged(javax.swing.event.ChangeEvent evt) {
        JOptionPane.showMessageDialog(this, TO_BE_IMPLEMENTED);
    }

    private void jTextFieldCharActionPerformed(java.awt.event.ActionEvent evt) {
        if (boxes == null) {
            return;
        }
        List<TessBox> selected = this.boxes.getSelectedBoxes();
        if (selected.size() <= 0) {
            return;
        } else if (selected.size() > 1) {
            JOptionPane.showMessageDialog(this, "Please select only one box to apply the change.");
            return;
        }
        TessBox box = selected.get(0);
        int index = this.boxes.toList().indexOf(box);
        if (!box.getChrs().equals(this.jTextFieldChar.getText())) {
            box.setChrs(this.jTextFieldChar.getText());
            tableModel.setValueAt(box.getChrs(), index, 0);
            jLabelChar.setText(this.jTextFieldChar.getText());
            jLabelCodepointValue.setText(Utilities.toHex(this.jTextFieldChar.getText()));
            updateSave(true);
        }
    }

    private void jButtonConvertActionPerformed(java.awt.event.ActionEvent evt) {
        String curChar = this.jTextFieldChar.getText();
        if (curChar.trim().length() == 0) {
            return;
        }
        this.jTextFieldChar.setText(TextUtilities.convertNCR(this.jTextFieldChar.getText()));
        if (curChar.equals(this.jTextFieldChar.getText())) {
            jTextFieldCharActionPerformed(evt);
        }
    }

    void jMenuItemMergeActionPerformed(java.awt.event.ActionEvent evt) {
        JOptionPane.showMessageDialog(this, TO_BE_IMPLEMENTED);
    }

    void jMenuItemSplitActionPerformed(java.awt.event.ActionEvent evt) {
        JOptionPane.showMessageDialog(this, TO_BE_IMPLEMENTED);
    }

    void jMenuItemDeleteActionPerformed(java.awt.event.ActionEvent evt) {
        JOptionPane.showMessageDialog(this, TO_BE_IMPLEMENTED);
    }

    void jMenuItemInsertActionPerformed(java.awt.event.ActionEvent evt) {
        JOptionPane.showMessageDialog(this, TO_BE_IMPLEMENTED);
    }

    private void jButtonMergeActionPerformed(java.awt.event.ActionEvent evt) {
        jMenuItemMergeActionPerformed(evt);
    }

    private void jButtonDeleteActionPerformed(java.awt.event.ActionEvent evt) {
        jMenuItemDeleteActionPerformed(evt);
    }

    private void jButtonSplitActionPerformed(java.awt.event.ActionEvent evt) {
        jMenuItemSplitActionPerformed(evt);
    }

    private void jButtonInsertActionPerformed(java.awt.event.ActionEvent evt) {
        jMenuItemInsertActionPerformed(evt);
    }

    private void jButtonFindActionPerformed(java.awt.event.ActionEvent evt) {
        if (imageList == null) {
            return;
        }
        int pageHeight = imageList.get(imageIndex).getHeight();
        String[] items = this.jTextFieldFind.getText().split("\\s+");
        try {
            TessBox findBox;
            if (items.length == 1) {
                String chrs = items[0];
                if (chrs.length() == 0) {
                    throw new Exception("Empty search values.");
                }
                findBox = new TessBox(chrs, new Rectangle(), imageIndex);
                findBox = boxes.selectByChars(findBox);
            } else {
                int x = Integer.parseInt(items[0]);
                int y = Integer.parseInt(items[1]);
                int w = Integer.parseInt(items[2]) - x;
                int h = Integer.parseInt(items[3]) - y;
                y = pageHeight - y - h;
                findBox = new TessBox("", new Rectangle(x, y, w, h), imageIndex);
                findBox = boxes.select(findBox);
            }
            if (findBox != null) {
                int index = boxes.toList().indexOf(findBox);
                this.jTable.setRowSelectionInterval(index, index);
                Rectangle rect = this.jTable.getCellRect(index, 0, true);
                this.jTable.scrollRectToVisible(rect);
            } else {
                this.jTable.clearSelection();
                String msg = String.format("No box with the specified %s was found.", items.length == 1 ? "character(s)" : "coordinates");
                JOptionPane.showMessageDialog(this, msg);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Please enter box character(s) or coordinates (x1 y1 x2 y2).");
        }
    }

    private void jTextFieldFindActionPerformed(java.awt.event.ActionEvent evt) {
        jButtonFindActionPerformed(evt);
    }

    void jMenuItemMergeTiffActionPerformed(java.awt.event.ActionEvent evt) {
        JOptionPane.showMessageDialog(this, TO_BE_IMPLEMENTED);
    }

    void jMenuItemGenerateTiffBoxActionPerformed(java.awt.event.ActionEvent evt) {
        JOptionPane.showMessageDialog(this, TO_BE_IMPLEMENTED);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                new Gui().setVisible(true);
            }
        });
    }

    private javax.swing.JButton jButtonConvert;

    private javax.swing.JButton jButtonDelete;

    private javax.swing.JButton jButtonFind;

    private javax.swing.JButton jButtonInsert;

    private javax.swing.JButton jButtonMerge;

    private javax.swing.JButton jButtonNextPage;

    private javax.swing.JButton jButtonOpen;

    private javax.swing.JButton jButtonPrevPage;

    private javax.swing.JButton jButtonReload;

    private javax.swing.JButton jButtonSave;

    private javax.swing.JButton jButtonSplit;

    private javax.swing.JFileChooser jFileChooser;

    protected javax.swing.JLabel jLabelChar;

    private javax.swing.JLabel jLabelCharacter;

    private javax.swing.JLabel jLabelCodepoint;

    private javax.swing.JLabel jLabelCodepointValue;

    private javax.swing.JLabel jLabelH;

    protected javax.swing.JLabel jLabelImage;

    private javax.swing.JLabel jLabelPageNbr;

    protected javax.swing.JLabel jLabelStatus;

    protected javax.swing.JLabel jLabelSubimage;

    private javax.swing.JLabel jLabelW;

    private javax.swing.JLabel jLabelX;

    private javax.swing.JLabel jLabelY;

    private javax.swing.JMenuBar jMenuBar;

    private javax.swing.JMenu jMenuEdit;

    private javax.swing.JMenu jMenuFile;

    private javax.swing.JMenu jMenuHelp;

    private javax.swing.JMenuItem jMenuItemAbout;

    private javax.swing.JMenuItem jMenuItemDelete;

    private javax.swing.JMenuItem jMenuItemExit;

    private javax.swing.JMenuItem jMenuItemFont;

    private javax.swing.JMenuItem jMenuItemGenerateTiffBox;

    private javax.swing.JMenuItem jMenuItemHelp;

    private javax.swing.JMenuItem jMenuItemInsert;

    private javax.swing.JMenuItem jMenuItemMerge;

    private javax.swing.JMenuItem jMenuItemMergeTiff;

    private javax.swing.JMenuItem jMenuItemOpen;

    private javax.swing.JMenuItem jMenuItemSave;

    private javax.swing.JMenuItem jMenuItemSaveAs;

    private javax.swing.JMenuItem jMenuItemSplit;

    protected javax.swing.JMenu jMenuLookAndFeel;

    protected javax.swing.JMenu jMenuRecentFiles;

    private javax.swing.JMenu jMenuSettings;

    private javax.swing.JMenu jMenuTools;

    private javax.swing.JPanel jPanel1;

    private javax.swing.JPanel jPanelBoxView;

    protected javax.swing.JPanel jPanelCoord;

    private javax.swing.JPanel jPanelFind;

    private javax.swing.JPanel jPanelSpinner;

    private javax.swing.JPanel jPanelStatus;

    private javax.swing.JScrollPane jScrollPaneBoxData;

    private javax.swing.JScrollPane jScrollPaneCoord;

    private javax.swing.JScrollPane jScrollPaneImage;

    private javax.swing.JPopupMenu.Separator jSeparator1;

    private javax.swing.JPopupMenu.Separator jSeparator3;

    private javax.swing.JPopupMenu.Separator jSeparatorAbout;

    private javax.swing.JPopupMenu.Separator jSeparatorExit;

    protected javax.swing.JSpinner jSpinnerH;

    protected javax.swing.JSpinner jSpinnerW;

    protected javax.swing.JSpinner jSpinnerX;

    protected javax.swing.JSpinner jSpinnerY;

    private javax.swing.JTabbedPane jTabbedPaneBoxData;

    protected javax.swing.JTable jTable;

    protected javax.swing.JTextArea jTextArea;

    protected javax.swing.JTextField jTextFieldChar;

    private javax.swing.JTextField jTextFieldFind;

    private javax.swing.JToolBar jToolBar1;

    private JFrame helptopicsFrame;
}
