package org.jlokalize.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu.Separator;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import org.jlokalize.KeyEntry;
import org.jlokalize.KeysTableModel;
import org.jlokalize.KeysTableRenderer;
import org.jlokalize.LanguageProperties;
import org.jlokalize.LanguageTreeManager;
import org.jlokalize.LanguageTreeModel;
import org.jlokalize.LanguageTreeProject;
import org.jlokalize.LanguageTreeRenderer;
import org.jlokalize.Main;
import org.jlokalize.SpellCheckerIntegration;
import org.tools.common.CentralStatic;
import org.tools.common.TreeNode;
import org.tools.common.Utils;
import org.tools.i18n.PropertyWithStats;
import org.tools.io.Resource;
import org.tools.io.ResourceUtils;
import org.tools.ui.NotificationFactory;
import org.tools.ui.UITools;

/**
 * Keys in keys table can be copied to clip board by "ctrl c". A shortcut for the
 * next key, next new key buttons from the text areas is given by "F2" and "F3".
 * 
 * @author Trilarion 2010-2011
 */
public class EditorFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = Logger.getLogger(EditorFrame.class.getName());

    /** Maximal length of last opened projects list */
    private static final int MAX_LAST_OPENED = 10;

    /** Last opened files list */
    private List<String> lastOpened = new LinkedList<String>();

    /** Everything project specific, only one at a time. */
    private LanguageTreeProject project = new LanguageTreeProject();

    /**
     * Initializes a new main editor frame. Populates the frame. Implements the
     * handler for the selection in the keys table. Also sets model and renderer
     * of the keys table. Also sets renderer and model for the languages tree.
     */
    public EditorFrame() {
        initComponents();
        setPropertiesFromOptions();
        updateLocalization();
        languageTree.setCellRenderer(new LanguageTreeRenderer());
        languageTree.setModel(new LanguageTreeModel());
        languageTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        keysTable.setDefaultRenderer(String.class, new KeysTableRenderer());
        keysTable.setModel(new KeysTableModel(statusProgressBar));
        keysTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            /**
             * The selected row has changed in the table.
             */
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting() == false) {
                    saveTextAreas();
                    int row = keysTable.getSelectedRow();
                    if (row != -1) {
                        KeysTableModel keysTableModel = (KeysTableModel) keysTable.getModel();
                        KeyEntry entry = keysTableModel.getEntry(row);
                        valueTextArea.setText(entry.text);
                        defaultValueTextArea.setText(entry.defaultText);
                        commentTextArea.setText(entry.comment);
                        defaultCommentTextArea.setText(entry.defaultComment);
                        valueTextArea.setEnabled(true);
                        commentTextArea.setEnabled(true);
                    } else {
                        clearTextAreas();
                    }
                }
            }
        });
        Resource res = null;
        Properties langFlagMap = new Properties();
        try {
            res = ResourceUtils.asResource(Main.jarPath + "JLokalize.jar/icons/flags/LanguageFlagMap.properties");
            langFlagMap.load(res.getInputStream());
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        CentralStatic.store("LanguageFlagMap", langFlagMap);
        try {
            res = ResourceUtils.asResource(Main.jarPath + "JLokalize.jar/icons/editor/generic_flag.png");
            ImageIcon genericFlag = new ImageIcon(ImageIO.read(res.getInputStream()));
            CentralStatic.store("GenericFlag", genericFlag);
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        spellCheckRegisterUnregister();
        UITools.setButtonKeyStroke(nextKeyButton, KeyStroke.getKeyStroke("F2"));
        UITools.setButtonKeyStroke(nextNewKeyButton, KeyStroke.getKeyStroke("F3"));
    }

    /**
     * Populate the last opened project menu with data from the options.
     */
    private void initLastOpenedMenu() {
        for (int i = 0; i < MAX_LAST_OPENED; i++) {
            String key = String.format("program.open.last.%d", i);
            String filePath = Main.options.get(key);
            if (filePath != null) {
                lastOpened.add(filePath);
            }
        }
        rebuildLastOpenedMenu();
    }

    /**
     * Save the last opened menu list to the options.
     */
    private void saveLastOpenedMenu() {
        int i;
        for (i = 0; i < MAX_LAST_OPENED; i++) {
            String key = String.format("program.open.last.%d", i);
            Main.options.removeKey(key);
        }
        i = 0;
        for (String filePath : lastOpened) {
            String key = String.format("program.open.last.%d", i);
            Main.options.put(key, filePath);
            i++;
        }
    }

    /**
     * Update the last opened menu after a new project is opened.
     * @param file The main file of the opened project.
     */
    private void updateLastOpenedMenu(File file) {
        String filePath = file.getPath();
        lastOpened.remove(filePath);
        lastOpened.add(0, filePath);
        if (lastOpened.size() > MAX_LAST_OPENED) {
            lastOpened.remove(lastOpened.size() - 1);
        }
        rebuildLastOpenedMenu();
    }

    /**
     * If opening a project was not successful, delete the project also from the
     * last opened list.
     * 
     * @param file The main file of the opened project.
     */
    private void deleteFromLastOpenedMenu(File file) {
        String filePath = file.getPath();
        if (lastOpened.contains(filePath)) {
            lastOpened.remove(filePath);
            rebuildLastOpenedMenu();
        }
    }

    /**
     * Something changed in the last opened list. Update the menu entries.
     * We do this by removing all of them completely and adding again.
     * Appropriate action listeners are created.
     */
    private void rebuildLastOpenedMenu() {
        lastOpenedMenu.setEnabled(!lastOpened.isEmpty());
        lastOpenedMenu.removeAll();
        for (String filePath : lastOpened) {
            final File file = new File(filePath);
            JMenuItem item = new JMenuItem(file.getName());
            item.setToolTipText(filePath);
            item.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    EditorFrame.this.closeMenuItemActionPerformed(e);
                    EditorFrame.this.openProjectAction(file);
                }
            });
            lastOpenedMenu.add(item);
        }
        if (lastOpened.isEmpty()) {
            lastOpenedMenu.add(new JMenuItem(""));
        }
    }

    /**
     * Registers or unregisters the text areas with the spell checker.
     * 
     * Is final because it's called in the constructor (otherwise overridable).
     */
    protected final void spellCheckRegisterUnregister() {
        if ("true".equals(Main.options.get("pref.dictionary.use"))) {
            SpellCheckerIntegration.registerComponents(valueTextArea, commentTextArea);
        } else {
            SpellCheckerIntegration.unregisterComponents(valueTextArea, commentTextArea);
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    private void initComponents() {
        toolBar = new JToolBar();
        newButton = new JButton();
        openButton = new JButton();
        toolBarSeparator1 = new JToolBar.Separator();
        nextKeyButton = new JButton();
        nextNewKeyButton = new JButton();
        toolBarSeparator2 = new JToolBar.Separator();
        newLangButton = new JButton();
        removeLangButton = new JButton();
        setMasterButton = new JButton();
        toolBarSeparator3 = new JToolBar.Separator();
        newKeyButton = new JButton();
        removeKeyButton = new JButton();
        renameKeyButton = new JButton();
        toolBarSeparator4 = new JToolBar.Separator();
        revertKeyButton = new JButton();
        horizontalSplitPane = new JSplitPane();
        leftSideSplitPane = new JSplitPane();
        languageTreeScrollPane = new JScrollPane();
        languageTree = new JTree();
        keysTablePanel = new JPanel();
        keysScrollPane = new JScrollPane();
        keysTable = new JTable();
        statusProgressBar = new JProgressBar();
        rightSideSplitPane = new JSplitPane();
        valuePanel = new JPanel();
        defaultValueScrollPane = new JScrollPane();
        defaultValueTextArea = new JTextArea();
        valueLabel = new JLabel();
        valueScrollPane = new JScrollPane();
        valueTextArea = new JTextArea();
        useDefaultTextButton = new JButton();
        commentPanel = new JPanel();
        defaultCommentScrollPane = new JScrollPane();
        defaultCommentTextArea = new JTextArea();
        commentLabel = new JLabel();
        commentScrollPane = new JScrollPane();
        commentTextArea = new JTextArea();
        useDefaultCommentButton = new JButton();
        menuBar = new JMenuBar();
        fileMenu = new JMenu();
        newMenuItem = new JMenuItem();
        openMenuItem = new JMenuItem();
        lastOpenedMenu = new JMenu();
        saveMenuItem = new JMenuItem();
        saveAsMenuItem = new JMenuItem();
        closeMenuItem = new JMenuItem();
        fileMenuSeparator = new Separator();
        exitMenuItem = new JMenuItem();
        helpMenu = new JMenu();
        langMenuItem = new JMenuItem();
        optionsMenuItem = new JMenuItem();
        helpMenuSeparator = new Separator();
        aboutMenuItem = new JMenuItem();
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle(Main.options.get("window.title"));
        setIconImage(new ImageIcon(getClass().getResource("/icons/jlokalize.png")).getImage());
        setMinimumSize(new Dimension(400, 400));
        addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent evt) {
                formWindowClosing(evt);
            }
        });
        toolBar.setFloatable(false);
        toolBar.setRollover(true);
        newButton.setIcon(new ImageIcon(getClass().getResource("/icons/editor/document-new.png")));
        newButton.setToolTipText("");
        newButton.setFocusable(false);
        newButton.setHorizontalTextPosition(SwingConstants.CENTER);
        newButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        newButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                newMenuItemActionPerformed(evt);
            }
        });
        toolBar.add(newButton);
        openButton.setIcon(new ImageIcon(getClass().getResource("/icons/editor/document-open.png")));
        openButton.setToolTipText("");
        openButton.setFocusable(false);
        openButton.setHorizontalTextPosition(SwingConstants.CENTER);
        openButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        openButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                openMenuItemActionPerformed(evt);
            }
        });
        toolBar.add(openButton);
        toolBar.add(toolBarSeparator1);
        nextKeyButton.setIcon(new ImageIcon(getClass().getResource("/icons/editor/go-next.png")));
        nextKeyButton.setToolTipText("");
        nextKeyButton.setFocusable(false);
        nextKeyButton.setHorizontalTextPosition(SwingConstants.CENTER);
        nextKeyButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        nextKeyButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                nextKeyButtonActionPerformed(evt);
            }
        });
        toolBar.add(nextKeyButton);
        nextNewKeyButton.setIcon(new ImageIcon(getClass().getResource("/icons/editor/go-last.png")));
        nextNewKeyButton.setToolTipText("");
        nextNewKeyButton.setFocusable(false);
        nextNewKeyButton.setHorizontalTextPosition(SwingConstants.CENTER);
        nextNewKeyButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        nextNewKeyButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                nextNewKeyButtonActionPerformed(evt);
            }
        });
        toolBar.add(nextNewKeyButton);
        toolBar.add(toolBarSeparator2);
        newLangButton.setIcon(new ImageIcon(getClass().getResource("/icons/editor/window_new.png")));
        newLangButton.setToolTipText("");
        newLangButton.setFocusable(false);
        newLangButton.setHorizontalTextPosition(SwingConstants.CENTER);
        newLangButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        newLangButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                newLangButtonActionPerformed(evt);
            }
        });
        toolBar.add(newLangButton);
        removeLangButton.setIcon(new ImageIcon(getClass().getResource("/icons/editor/window_new_modified_minus.png")));
        removeLangButton.setToolTipText("");
        removeLangButton.setFocusable(false);
        removeLangButton.setHorizontalTextPosition(SwingConstants.CENTER);
        removeLangButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        removeLangButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                removeLangButtonActionPerformed(evt);
            }
        });
        toolBar.add(removeLangButton);
        setMasterButton.setIcon(new ImageIcon(getClass().getResource("/icons/editor/gtk-about.png")));
        setMasterButton.setFocusable(false);
        setMasterButton.setHorizontalTextPosition(SwingConstants.CENTER);
        setMasterButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        setMasterButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                setMasterButtonActionPerformed(evt);
            }
        });
        toolBar.add(setMasterButton);
        toolBar.add(toolBarSeparator3);
        newKeyButton.setIcon(new ImageIcon(getClass().getResource("/icons/editor/button_key_new.png")));
        newKeyButton.setFocusable(false);
        newKeyButton.setHorizontalTextPosition(SwingConstants.CENTER);
        newKeyButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        newKeyButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                newKeyButtonActionPerformed(evt);
            }
        });
        toolBar.add(newKeyButton);
        removeKeyButton.setIcon(new ImageIcon(getClass().getResource("/icons/editor/button_key_remove.png")));
        removeKeyButton.setFocusable(false);
        removeKeyButton.setHorizontalTextPosition(SwingConstants.CENTER);
        removeKeyButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        removeKeyButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                removeKeyButtonActionPerformed(evt);
            }
        });
        toolBar.add(removeKeyButton);
        renameKeyButton.setIcon(new ImageIcon(getClass().getResource("/icons/editor/button_key_rename.png")));
        renameKeyButton.setFocusable(false);
        renameKeyButton.setHorizontalTextPosition(SwingConstants.CENTER);
        renameKeyButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        renameKeyButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                renameKeyButtonActionPerformed(evt);
            }
        });
        toolBar.add(renameKeyButton);
        toolBar.add(toolBarSeparator4);
        revertKeyButton.setIcon(new ImageIcon(getClass().getResource("/icons/editor/revert.png")));
        revertKeyButton.setFocusable(false);
        revertKeyButton.setHorizontalTextPosition(SwingConstants.CENTER);
        revertKeyButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        revertKeyButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                revertKeyButtonActionPerformed(evt);
            }
        });
        toolBar.add(revertKeyButton);
        horizontalSplitPane.setDividerLocation(250);
        horizontalSplitPane.setMinimumSize(new Dimension(300, 300));
        leftSideSplitPane.setDividerLocation(150);
        leftSideSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        leftSideSplitPane.setMinimumSize(new Dimension(50, 100));
        languageTree.setModel(null);
        languageTree.addTreeSelectionListener(new TreeSelectionListener() {

            public void valueChanged(TreeSelectionEvent evt) {
                languageTreeValueChanged(evt);
            }
        });
        languageTreeScrollPane.setViewportView(languageTree);
        leftSideSplitPane.setTopComponent(languageTreeScrollPane);
        keysScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        keysTable.setModel(new DefaultTableModel(new Object[][] {}, new String[] {}));
        keysTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        keysScrollPane.setViewportView(keysTable);
        statusProgressBar.setMinimumSize(new Dimension(1, 16));
        statusProgressBar.setPreferredSize(new Dimension(250, 16));
        statusProgressBar.setString("");
        statusProgressBar.setStringPainted(true);
        GroupLayout keysTablePanelLayout = new GroupLayout(keysTablePanel);
        keysTablePanel.setLayout(keysTablePanelLayout);
        keysTablePanelLayout.setHorizontalGroup(keysTablePanelLayout.createParallelGroup(Alignment.LEADING).addComponent(statusProgressBar, GroupLayout.DEFAULT_SIZE, 247, Short.MAX_VALUE).addComponent(keysScrollPane, GroupLayout.DEFAULT_SIZE, 247, Short.MAX_VALUE));
        keysTablePanelLayout.setVerticalGroup(keysTablePanelLayout.createParallelGroup(Alignment.LEADING).addGroup(Alignment.TRAILING, keysTablePanelLayout.createSequentialGroup().addComponent(keysScrollPane, GroupLayout.DEFAULT_SIZE, 254, Short.MAX_VALUE).addPreferredGap(ComponentPlacement.RELATED).addComponent(statusProgressBar, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)));
        leftSideSplitPane.setRightComponent(keysTablePanel);
        horizontalSplitPane.setLeftComponent(leftSideSplitPane);
        rightSideSplitPane.setDividerLocation(250);
        rightSideSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        rightSideSplitPane.setResizeWeight(0.5);
        rightSideSplitPane.setMinimumSize(new Dimension(200, 100));
        valuePanel.setMinimumSize(new Dimension(200, 200));
        valuePanel.setPreferredSize(new Dimension(542, 300));
        defaultValueScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        defaultValueScrollPane.setPreferredSize(new Dimension(200, 100));
        defaultValueTextArea.setBackground(new Color(240, 240, 240));
        defaultValueTextArea.setColumns(20);
        defaultValueTextArea.setEditable(false);
        defaultValueTextArea.setLineWrap(true);
        defaultValueTextArea.setRows(5);
        defaultValueTextArea.setMinimumSize(new Dimension(120, 30));
        defaultValueScrollPane.setViewportView(defaultValueTextArea);
        valueLabel.setText("Translation:");
        valueScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        valueTextArea.setColumns(20);
        valueTextArea.setLineWrap(true);
        valueTextArea.setRows(5);
        valueTextArea.setMinimumSize(new Dimension(120, 30));
        valueScrollPane.setViewportView(valueTextArea);
        useDefaultTextButton.setText("use");
        useDefaultTextButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                useDefaultTextButtonActionPerformed(evt);
            }
        });
        GroupLayout valuePanelLayout = new GroupLayout(valuePanel);
        valuePanel.setLayout(valuePanelLayout);
        valuePanelLayout.setHorizontalGroup(valuePanelLayout.createParallelGroup(Alignment.LEADING).addGroup(valuePanelLayout.createSequentialGroup().addContainerGap().addComponent(valueLabel, GroupLayout.PREFERRED_SIZE, 130, GroupLayout.PREFERRED_SIZE).addGap(124, 124, 124).addComponent(useDefaultTextButton).addContainerGap(229, Short.MAX_VALUE)).addComponent(defaultValueScrollPane, GroupLayout.DEFAULT_SIZE, 542, Short.MAX_VALUE).addComponent(valueScrollPane, GroupLayout.DEFAULT_SIZE, 542, Short.MAX_VALUE));
        valuePanelLayout.setVerticalGroup(valuePanelLayout.createParallelGroup(Alignment.LEADING).addGroup(valuePanelLayout.createSequentialGroup().addComponent(defaultValueScrollPane, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE).addPreferredGap(ComponentPlacement.RELATED).addGroup(valuePanelLayout.createParallelGroup(Alignment.BASELINE).addComponent(useDefaultTextButton).addComponent(valueLabel)).addPreferredGap(ComponentPlacement.RELATED).addComponent(valueScrollPane, GroupLayout.DEFAULT_SIZE, 114, Short.MAX_VALUE)));
        rightSideSplitPane.setTopComponent(valuePanel);
        commentPanel.setMinimumSize(new Dimension(200, 200));
        commentPanel.setPreferredSize(new Dimension(542, 300));
        defaultCommentScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        defaultCommentTextArea.setBackground(new Color(240, 240, 240));
        defaultCommentTextArea.setColumns(20);
        defaultCommentTextArea.setEditable(false);
        defaultCommentTextArea.setLineWrap(true);
        defaultCommentTextArea.setRows(5);
        defaultCommentTextArea.setMinimumSize(new Dimension(120, 30));
        defaultCommentScrollPane.setViewportView(defaultCommentTextArea);
        commentLabel.setText("Comment:");
        commentScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        commentTextArea.setColumns(20);
        commentTextArea.setLineWrap(true);
        commentTextArea.setRows(5);
        commentTextArea.setMinimumSize(new Dimension(120, 30));
        commentScrollPane.setViewportView(commentTextArea);
        useDefaultCommentButton.setText("use");
        useDefaultCommentButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                useDefaultCommentButtonActionPerformed(evt);
            }
        });
        GroupLayout commentPanelLayout = new GroupLayout(commentPanel);
        commentPanel.setLayout(commentPanelLayout);
        commentPanelLayout.setHorizontalGroup(commentPanelLayout.createParallelGroup(Alignment.LEADING).addGroup(commentPanelLayout.createSequentialGroup().addContainerGap().addComponent(commentLabel, GroupLayout.PREFERRED_SIZE, 136, GroupLayout.PREFERRED_SIZE).addGap(114, 114, 114).addComponent(useDefaultCommentButton).addContainerGap(233, Short.MAX_VALUE)).addComponent(commentScrollPane, GroupLayout.DEFAULT_SIZE, 542, Short.MAX_VALUE).addComponent(defaultCommentScrollPane, GroupLayout.DEFAULT_SIZE, 542, Short.MAX_VALUE));
        commentPanelLayout.setVerticalGroup(commentPanelLayout.createParallelGroup(Alignment.LEADING).addGroup(commentPanelLayout.createSequentialGroup().addComponent(defaultCommentScrollPane, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE).addPreferredGap(ComponentPlacement.RELATED).addGroup(commentPanelLayout.createParallelGroup(Alignment.BASELINE).addComponent(commentLabel).addComponent(useDefaultCommentButton)).addPreferredGap(ComponentPlacement.RELATED).addComponent(commentScrollPane, GroupLayout.DEFAULT_SIZE, 218, Short.MAX_VALUE)));
        rightSideSplitPane.setBottomComponent(commentPanel);
        horizontalSplitPane.setRightComponent(rightSideSplitPane);
        menuBar.setMinimumSize(new Dimension(0, 20));
        fileMenu.setText("File");
        newMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_MASK));
        newMenuItem.setIcon(new ImageIcon(getClass().getResource("/icons/editor/document-new.png")));
        newMenuItem.setText("New");
        newMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                newMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(newMenuItem);
        openMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
        openMenuItem.setIcon(new ImageIcon(getClass().getResource("/icons/editor/document-open.png")));
        openMenuItem.setText("Open");
        openMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                openMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(openMenuItem);
        lastOpenedMenu.setIcon(new ImageIcon(getClass().getResource("/icons/editor/document-open.png")));
        lastOpenedMenu.setText("Last opened");
        fileMenu.add(lastOpenedMenu);
        saveMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK));
        saveMenuItem.setIcon(new ImageIcon(getClass().getResource("/icons/editor/filesave.png")));
        saveMenuItem.setText("Save");
        saveMenuItem.setDisabledIcon(new ImageIcon(getClass().getResource("/icons/editor/filesave.png")));
        saveMenuItem.setEnabled(false);
        saveMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                saveMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(saveMenuItem);
        saveAsMenuItem.setIcon(new ImageIcon(getClass().getResource("/icons/editor/filesaveas.png")));
        saveAsMenuItem.setText("Save As");
        saveAsMenuItem.setDisabledIcon(new ImageIcon(getClass().getResource("/icons/editor/filesaveas.png")));
        saveAsMenuItem.setEnabled(false);
        saveAsMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                saveAsMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(saveAsMenuItem);
        closeMenuItem.setIcon(new ImageIcon(getClass().getResource("/icons/editor/stop.png")));
        closeMenuItem.setText("Close");
        closeMenuItem.setDisabledIcon(new ImageIcon(getClass().getResource("/icons/editor/stop.png")));
        closeMenuItem.setEnabled(false);
        closeMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                closeMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(closeMenuItem);
        fileMenu.add(fileMenuSeparator);
        exitMenuItem.setIcon(new ImageIcon(getClass().getResource("/icons/editor/exit.png")));
        exitMenuItem.setText("Exit");
        exitMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                exitMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(exitMenuItem);
        menuBar.add(fileMenu);
        helpMenu.setText("Help");
        langMenuItem.setIcon(new ImageIcon(getClass().getResource("/icons/editor/bookmarks_list_add.png")));
        langMenuItem.setText("Choose Language");
        langMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                langMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(langMenuItem);
        optionsMenuItem.setIcon(new ImageIcon(getClass().getResource("/icons/editor/gtk-preferences.png")));
        optionsMenuItem.setText("Options");
        optionsMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                optionsMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(optionsMenuItem);
        helpMenu.add(helpMenuSeparator);
        aboutMenuItem.setIcon(new ImageIcon(getClass().getResource("/icons/editor/kfm_home.png")));
        aboutMenuItem.setText("About");
        aboutMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                aboutMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(aboutMenuItem);
        menuBar.add(helpMenu);
        setJMenuBar(menuBar);
        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(Alignment.LEADING).addComponent(toolBar, GroupLayout.DEFAULT_SIZE, 800, Short.MAX_VALUE).addComponent(horizontalSplitPane, GroupLayout.DEFAULT_SIZE, 800, Short.MAX_VALUE));
        layout.setVerticalGroup(layout.createParallelGroup(Alignment.LEADING).addGroup(layout.createSequentialGroup().addComponent(toolBar, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE).addPreferredGap(ComponentPlacement.RELATED).addComponent(horizontalSplitPane, GroupLayout.DEFAULT_SIZE, 607, Short.MAX_VALUE)));
    }

    /**
     * The exit menu item has been clicked. We save options and dispose of the
     * frame.
     * 
     * @param evt The event.
     */
    private void exitMenuItemActionPerformed(ActionEvent evt) {
        closeMenuItemActionPerformed(evt);
        savePropertiesToOptions();
        dispose();
        Main.Shutdown();
    }

    /**
     * The new project menu item (or button) has been clicked.
     * 
     * @param evt The event.
     */
    private void newMenuItemActionPerformed(ActionEvent evt) {
        PropertyWithStats lang = CentralStatic.retrieve("lang-prop");
        String base = JOptionPane.showInputDialog(this, lang.get("button.new.dialog"));
        if (base != null && base.length() > 0 && !base.contains("_") && !base.contains(".")) {
            closeMenuItemActionPerformed(null);
            TreeNode<LanguageProperties> root = project.createNew(base);
            LanguageTreeModel langTreeModel = (LanguageTreeModel) languageTree.getModel();
            langTreeModel.setCurrentNode(root);
            langTreeModel.structureChanged();
            setTitle(Main.options.get("window.title") + " - " + project.getBase());
            saveMenuItem.setEnabled(true);
            saveAsMenuItem.setEnabled(true);
            closeMenuItem.setEnabled(true);
        } else {
            JOptionPane.showMessageDialog(this, lang.get("button.new.invalid"), lang.get("error"), JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * The open from file menu item (or button) has been clicked.
     * 
     * @param evt The event.
     */
    private void openMenuItemActionPerformed(ActionEvent evt) {
        closeMenuItemActionPerformed(evt);
        JFileChooser chooser = new JFileChooser(Main.options.get("program.open.default.directory"));
        chooser.setFileFilter(LanguageTreeProject.FFilter);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            Main.options.put("program.open.default.directory", chooser.getCurrentDirectory().getAbsolutePath());
            openProjectAction(chooser.getSelectedFile());
        }
    }

    /**
     * Called from the 'Open' menu action or from the last opened action.
     * 
     * @param file The file that defines a project.
     */
    private void openProjectAction(File file) {
        PropertyWithStats lang = CentralStatic.retrieve("lang-prop");
        if (!project.open(file)) {
            deleteFromLastOpenedMenu(file);
            NotificationFactory.createInfoPane(this, String.format(lang.get("menu.file.open.error"), project.getBase()));
            return;
        }
        NotificationFactory.createInfoPane(this, String.format(lang.get("menu.file.open.confirm"), project.getBase()));
        updateLastOpenedMenu(file);
        LanguageTreeModel langTreeModel = (LanguageTreeModel) languageTree.getModel();
        langTreeModel.setCurrentNode(project.getRoot());
        langTreeModel.structureChanged();
        setTitle(Main.options.get("window.title") + " - " + project.getBase());
        saveMenuItem.setEnabled(true);
        saveAsMenuItem.setEnabled(true);
        closeMenuItem.setEnabled(true);
    }

    /**
     * The save menu item has been clicked. We redirect to "Save as", if no
     * project directory is given (i.e. a new project was created), otherwise
     * we call directly the appropriate function.
     * 
     * @param evt The event.
     */
    private void saveMenuItemActionPerformed(ActionEvent evt) {
        if (project.getDir() == null) {
            saveAsMenuItemActionPerformed(evt);
        } else {
            saveTextAreas();
            project.save();
            KeysTableModel keysTableModel = (KeysTableModel) keysTable.getModel();
            int row = keysTable.getSelectedRow();
            String oldKey = null;
            if (row != -1) {
                oldKey = keysTableModel.getEntry(row).key;
            }
            LanguageTreeModel langTreeModel = (LanguageTreeModel) languageTree.getModel();
            keysTableModel.update(langTreeModel.getCurrentNode());
            if (row != -1) {
                row = keysTableModel.getRow(oldKey);
                if (row != -1) {
                    keysTable.setRowSelectionInterval(row, row);
                    keysTable.scrollRectToVisible(new Rectangle(keysTable.getCellRect(row, 0, true)));
                }
            }
            PropertyWithStats lang = CentralStatic.retrieve("lang-prop");
            NotificationFactory.createInfoPane(this, String.format(lang.get("menu.file.save.confirm"), project.getDir().getPath()));
        }
    }

    /**
     * The save as menu item has been clicked. Displays a File Save dialog with
     * an appropriate file filter. The obtained file name is basically just stripped
     * down and used as base name (e.g. the "JLokalize" in "JLokalize_en.properties").
     * 
     * @param evt The event.
     */
    private void saveAsMenuItemActionPerformed(ActionEvent evt) {
        JFileChooser chooser = new JFileChooser(Main.options.get("program.open.default.directory"));
        chooser.setFileFilter(LanguageTreeProject.FFilter);
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            project.rebase(file);
            saveMenuItemActionPerformed(null);
        }
    }

    /**
     * The close menu item has been clicked. Do a simple reset.
     * 
     * @param evt The event.
     */
    private void closeMenuItemActionPerformed(ActionEvent evt) {
        if (project.getRoot() != null) {
            PropertyWithStats lang = CentralStatic.retrieve("lang-prop");
            if (LanguageTreeManager.anyNodeContainsModifiedKeys(project.getRoot())) {
                int confirm = JOptionPane.showConfirmDialog(this, lang.get("menu.file.close.confirm"), lang.get("menu.file.close.name"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (confirm == JOptionPane.YES_OPTION) {
                    saveMenuItemActionPerformed(evt);
                }
            }
            project.reset();
            LanguageTreeModel langTreeModel = (LanguageTreeModel) languageTree.getModel();
            langTreeModel.setCurrentNode(null);
            langTreeModel.structureChanged();
            KeysTableModel keysTableModel = (KeysTableModel) keysTable.getModel();
            keysTableModel.clear();
            clearTextAreas();
            NotificationFactory.createInfoPane(this, lang.get("menu.file.close.closed"));
        }
    }

    /**
     * The about menu item has been clicked. Display the modal about dialog.
     * 
     * @param evt The event.
     */
    private void aboutMenuItemActionPerformed(ActionEvent evt) {
        AboutDlg.start(this);
    }

    /**
     * The next new key button has been clicked. Search for it and change the selection
     * in the keys table.
     * 
     * @param evt The event.
     */
    private void nextNewKeyButtonActionPerformed(ActionEvent evt) {
        KeysTableModel keysTableModel = (KeysTableModel) keysTable.getModel();
        int row = keysTable.getSelectedRow();
        int nrow = keysTableModel.getNextNotHereKey(row);
        if (nrow > 0) {
            keysTable.setRowSelectionInterval(nrow, nrow);
            keysTable.scrollRectToVisible(new Rectangle(keysTable.getCellRect(nrow, 0, true)));
        }
    }

    /**
     * The next key in table button has been clicked. Search for it and change
     * the selection in the keys table accordingly.
     * 
     * @param evt The event.
     */
    private void nextKeyButtonActionPerformed(ActionEvent evt) {
        int nrow = keysTable.getRowCount();
        if (nrow > 0) {
            int row = keysTable.getSelectedRow();
            row = (row + 1) % nrow;
            keysTable.setRowSelectionInterval(row, row);
            keysTable.scrollRectToVisible(new Rectangle(keysTable.getCellRect(row, 0, true)));
        }
    }

    /**
     * The change language menu item has been clicked. Follow the choose Language
     * script, which amongst others display the "choose language" dialog and set
     * new clear names (because they are in the locale language) and update the files tree.
     * 
     * @param evt The event.
     */
    private void langMenuItemActionPerformed(ActionEvent evt) {
        try {
            PropertyWithStats lang = CentralStatic.retrieve("lang-prop");
            lang.saveStatsOnly();
            if (Main.setupLanguage("none") == true) {
                LanguageTreeModel langTreeModel = (LanguageTreeModel) languageTree.getModel();
                TreeNode<LanguageProperties> current = langTreeModel.getCurrentNode();
                int row = keysTable.getSelectedRow();
                LanguageTreeManager.setClearNamesInTree(project.getRoot());
                LanguageTreeManager.sortTreeForClearNames(project.getRoot());
                langTreeModel.structureChanged();
                if (current != null) {
                    languageTree.setSelectionPath(TreeNode.getPathFor(current));
                    if (row != -1) {
                        keysTable.setRowSelectionInterval(row, row);
                        keysTable.scrollRectToVisible(new Rectangle(keysTable.getCellRect(row, 0, true)));
                    }
                }
                updateLocalization();
            }
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Selection of languages in the language tree has changed.
     * Need to rebuild the table model.
     * This is called by an ActionListener of the JTree filesTree.
     * 
     * @param evt The event.
     */
    @SuppressWarnings("unchecked")
    private void languageTreeValueChanged(TreeSelectionEvent evt) {
        saveTextAreas();
        clearTextAreas();
        KeysTableModel keysTableModel = (KeysTableModel) keysTable.getModel();
        int row = keysTable.getSelectedRow();
        String oldKey = null;
        if (row != -1) {
            oldKey = keysTableModel.getEntry(row).key;
        }
        LanguageTreeModel langTreeModel = (LanguageTreeModel) languageTree.getModel();
        TreePath path = evt.getPath();
        langTreeModel.setCurrentNode((TreeNode<LanguageProperties>) path.getLastPathComponent());
        keysTableModel.update(langTreeModel.getCurrentNode());
        if (row != -1) {
            row = keysTableModel.getRow(oldKey);
            if (row != -1) {
                keysTable.setRowSelectionInterval(row, row);
                keysTable.scrollRectToVisible(new Rectangle(keysTable.getCellRect(row, 0, true)));
            }
        }
    }

    /**
     * The insert new language in project button has been clicked. Display the
     * choose new language dialog (mainly the language, country, variant two-letter
     * codes) and insert it in the language tree. Update the tree.
     * 
     * @param evt The event.
     */
    private void newLangButtonActionPerformed(ActionEvent evt) {
        TreeNode<LanguageProperties> root = project.getRoot();
        if (root != null) {
            NewLanguageDlg dialog = new NewLanguageDlg(this);
            String[] languageCodes = dialog.execute();
            if (languageCodes != null) {
                String country = languageCodes[0];
                if (country == null || country.length() != 2 || !Utils.isLower(country)) {
                    NotificationFactory.createInfoPane(this, "Country field is not given or not 2 two small letter term.");
                    return;
                }
                LanguageProperties language = new LanguageProperties();
                language.setLanguageCodes(languageCodes);
                language.setClearName();
                boolean do_it = true;
                if (LanguageTreeManager.contains(root, language)) {
                    int ans = JOptionPane.showConfirmDialog(this, "Language already existing! Overwrite?", "New Language", JOptionPane.YES_NO_OPTION);
                    if (ans == JOptionPane.NO_OPTION) {
                        do_it = false;
                    }
                }
                if (do_it == true) {
                    LanguageTreeManager.insertLangPropInTree(root, language);
                    LanguageTreeManager.sortTreeForClearNames(root);
                    languageTree.setSelectionRow(0);
                    LanguageTreeModel langTreeModel = (LanguageTreeModel) languageTree.getModel();
                    langTreeModel.structureChanged();
                }
            }
        }
    }

    /**
     * The add new key in currently selected language button has been clicked.
     * Display a small input dialog and insert the key, then update.
     * 
     * @param evt The event.
     */
    private void newKeyButtonActionPerformed(ActionEvent evt) {
        if (project.getRoot() == null) {
            return;
        }
        String defaultKey = null;
        int row = keysTable.getSelectedRow();
        if (row != -1) {
            KeysTableModel keysTableModel = (KeysTableModel) keysTable.getModel();
            defaultKey = keysTableModel.getEntry(row).key;
        }
        PropertyWithStats lang = CentralStatic.retrieve("lang-prop");
        String newKey = JOptionPane.showInputDialog(this, lang.get("button.newkey.newkey"), defaultKey);
        if (newKey != null) {
            KeysTableModel keysTableModel = (KeysTableModel) keysTable.getModel();
            if (keysTableModel.containsKey(newKey)) {
                JOptionPane.showMessageDialog(this, lang.get("button.newkey.existing"), lang.get("error"), JOptionPane.ERROR_MESSAGE);
            } else {
                saveTextAreas();
                keysTableModel.insertKey(newKey);
                row = keysTableModel.getRow(newKey);
                keysTable.getSelectionModel().setSelectionInterval(row, row);
                keysTable.scrollRectToVisible(new Rectangle(keysTable.getCellRect(row, 0, true)));
            }
        }
    }

    /**
     * The remove key from currently selected language button has been clicked.
     * Find the key, display a short confirmation dialog, delete it and update.
     *
     * @param evt The event.
     */
    private void removeKeyButtonActionPerformed(ActionEvent evt) {
        if (project.getRoot() == null) {
            return;
        }
        int row = keysTable.getSelectedRow();
        if (row != -1) {
            PropertyWithStats lang = CentralStatic.retrieve("lang-prop");
            int confirm = JOptionPane.showConfirmDialog(this, lang.get("sure"), lang.get("button.removekey"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                KeysTableModel keysTableModel = (KeysTableModel) keysTable.getModel();
                keysTableModel.removeKey();
                valueTextArea.setText(null);
                defaultValueTextArea.setText(null);
            }
        }
    }

    /**
     * The options menu item has been clicked. Display the modal options dialog.
     * 
     * @param evt The event.
     */
    private void optionsMenuItemActionPerformed(ActionEvent evt) {
        OptionsDlg dialog = new OptionsDlg(this);
        dialog.setVisible(true);
    }

    /**
     * The set currently selected language as master language (if it is one below
     * the root or root) button has been clicked. It tells both key table and the
     * tree renderer (who displays a (Master) behind the language in question) about
     * it.
     * 
     * @param evt The event.
     */
    private void setMasterButtonActionPerformed(ActionEvent evt) {
        LanguageTreeModel langTreeModel = (LanguageTreeModel) languageTree.getModel();
        TreeNode<LanguageProperties> current = langTreeModel.getCurrentNode();
        TreeNode<LanguageProperties> root = project.getRoot();
        if (root == null) {
            return;
        }
        if (current.getParent() == root || current == root) {
            LanguageTreeManager.setMasterNode(root, current);
            langTreeModel.structureChanged();
            int row = keysTable.getSelectedRow();
            KeysTableModel keysTableModel = (KeysTableModel) keysTable.getModel();
            keysTableModel.update(current);
            if (current != null) {
                languageTree.setSelectionPath(TreeNode.getPathFor(current));
                if (row != -1) {
                    keysTable.setRowSelectionInterval(row, row);
                    keysTable.scrollRectToVisible(new Rectangle(keysTable.getCellRect(row, 0, true)));
                }
            }
        } else {
            PropertyWithStats lang = CentralStatic.retrieve("lang-prop");
            JOptionPane.showMessageDialog(this, lang.get("button.setmaster.noteligible"), lang.get("button.setmaster"), JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * The rename a key in the currently selected language button has been clicked.
     * Display a short input dialog for the new name and perform the rename, then
     * update.
     * 
     * Renaming a key to another key that is already existing (even if it was
     * removed) is not allowed.
     * 
     * @param evt The event.
     */
    private void renameKeyButtonActionPerformed(ActionEvent evt) {
        if (project.getRoot() == null) {
            return;
        }
        int row = keysTable.getSelectedRow();
        if (row != -1) {
            KeysTableModel keysTableModel = (KeysTableModel) keysTable.getModel();
            String oldKey = keysTableModel.getEntry(row).key;
            PropertyWithStats lang = CentralStatic.retrieve("lang-prop");
            String newKey = JOptionPane.showInputDialog(this, lang.get("button.newkey.newkey"), oldKey);
            if (newKey != null) {
                saveTextAreas();
                row = keysTableModel.renameKey(newKey);
                if (row != -1) {
                    keysTable.setRowSelectionInterval(row, row);
                    keysTable.scrollRectToVisible(new Rectangle(keysTable.getCellRect(row, 0, true)));
                }
            }
        }
    }

    /**
     * The revert key button has been clicked.
     * 
     * @param evt The event.
     */
    private void revertKeyButtonActionPerformed(ActionEvent evt) {
        if (project.getRoot() == null) {
            return;
        }
        int row = keysTable.getSelectedRow();
        if (row != -1) {
            KeysTableModel keysTableModel = (KeysTableModel) keysTable.getModel();
            String oldKey = keysTableModel.getEntry(row).key;
            keysTableModel.restore();
            row = keysTableModel.getRow(oldKey);
            if (row != -1) {
                keysTable.setRowSelectionInterval(row, row);
                keysTable.scrollRectToVisible(new Rectangle(keysTable.getCellRect(row, 0, true)));
            }
        }
    }

    /**
     * The remove currently selected language from project button has been clicked.
     * Display a short confirmation dialog, if so remove the language node from
     * the language tree and tell it that is has changed.
     * 
     * @param evt The event.
     */
    private void removeLangButtonActionPerformed(ActionEvent evt) {
        PropertyWithStats lang = CentralStatic.retrieve("lang-prop");
        TreePath path = languageTree.getSelectionPath();
        if (path != null) {
            @SuppressWarnings("unchecked") TreeNode<LanguageProperties> node = (TreeNode<LanguageProperties>) path.getLastPathComponent();
            String langName = node.getData().getClearName();
            int ans = JOptionPane.showConfirmDialog(this, String.format(lang.get("button.removelanguage.really"), langName), lang.get("button.removelanguage"), JOptionPane.YES_NO_OPTION);
            if (ans == JOptionPane.YES_OPTION) {
                if (LanguageTreeManager.removeNodeFromTree(node) == true) {
                    languageTree.setSelectionRow(0);
                    LanguageTreeModel langTreeModel = (LanguageTreeModel) languageTree.getModel();
                    langTreeModel.structureChanged();
                    JOptionPane.showMessageDialog(this, String.format(lang.get("button.removelanguage.failed"), langName), lang.get("button.removelanguage"), JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, String.format(lang.get("button.removelanguage.failed"), langName), lang.get("button.removelanguage"), JOptionPane.INFORMATION_MESSAGE);
                }
            }
        } else {
            JOptionPane.showMessageDialog(this, lang.get("button.removelanguage.choose"), lang.get("button.removelanguage"), JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * The window closing button has been clicked. Re-directing to the corresponding
     * menu item action.
     * 
     * @param evt The event.
     */
    private void formWindowClosing(WindowEvent evt) {
        exitMenuItemActionPerformed(null);
    }

    /**
     * Just copy the text from the default text box to the text box.
     * 
     * @param evt The event.
     */
    private void useDefaultTextButtonActionPerformed(ActionEvent evt) {
        valueTextArea.setText(defaultValueTextArea.getText());
    }

    /**
     * Just copy the text from the default comment text box to the comment text box.
     * @param evt
     */
    private void useDefaultCommentButtonActionPerformed(ActionEvent evt) {
        commentTextArea.setText(defaultCommentTextArea.getText());
    }

    /**
     * Internal function:
     * Either before the row selection of the keys table changes, or before the
     * languages tree selection changes or before anything is saved, we read
     * from the actual text fields and store the new text in the properties.
     * However, empty strings are not stored!
     */
    private void saveTextAreas() {
        String text = valueTextArea.getText();
        String comment = commentTextArea.getText();
        KeysTableModel keysTableModel = (KeysTableModel) keysTable.getModel();
        keysTableModel.updateLastKey(text, comment);
        keysTable.getColumnModel().getColumn(0).setHeaderValue(keysTableModel.getColumnName(0));
        keysTable.getTableHeader().resizeAndRepaint();
    }

    /**
     * Internal function:
     * If either a key is removed, renamed or added, nothing is selected initially
     * in the keys table (row == -1). Clear all text areas then.
     */
    private void clearTextAreas() {
        defaultValueTextArea.setText(null);
        valueTextArea.setText(null);
        valueTextArea.setEnabled(false);
        defaultCommentTextArea.setText(null);
        commentTextArea.setText(null);
        commentTextArea.setEnabled(false);
    }

    /**
     * Internal function!
     * During initialization restore some window properties (mainly window sizes) from
     * global options.
     */
    private void setPropertiesFromOptions() {
        int x, y;
        initLastOpenedMenu();
        x = Main.options.getInt("window.size.x");
        y = Main.options.getInt("window.size.y");
        setSize(x, y);
        x = Main.options.getInt("window.location.x");
        y = Main.options.getInt("window.location.y");
        setLocation(x, y);
        x = Main.options.getInt("split.pane.divide.editing");
        horizontalSplitPane.setDividerLocation(x);
        x = Main.options.getInt("split.pane.divide.text");
        rightSideSplitPane.setDividerLocation(x);
        x = Main.options.getInt("split.pane.divide.navigation");
        leftSideSplitPane.setDividerLocation(x);
    }

    /**
     * Internal function!
     * Upon exit, store some window properties (main sizes) into global options.
     */
    private void savePropertiesToOptions() {
        saveLastOpenedMenu();
        Dimension d = getSize();
        Main.options.putInt("window.size.x", d.width);
        Main.options.putInt("window.size.y", d.height);
        Point p = getLocation();
        Main.options.putInt("window.location.x", p.x);
        Main.options.putInt("window.location.y", p.y);
        Main.options.putInt("split.pane.divide.editing", horizontalSplitPane.getDividerLocation());
        Main.options.putInt("split.pane.divide.text", rightSideSplitPane.getDividerLocation());
        Main.options.putInt("split.pane.divide.navigation", leftSideSplitPane.getDividerLocation());
    }

    /**
     * Sets all texts and tool tips in the frame from the language property, i.e.
     * localizes the dialog.
     */
    private void updateLocalization() {
        PropertyWithStats lang = CentralStatic.retrieve("lang-prop");
        fileMenu.setText(lang.get("menu.file.name"));
        newMenuItem.setText(lang.get("menu.file.new.name"));
        openMenuItem.setText(lang.get("menu.file.open.name"));
        lastOpenedMenu.setText(lang.get("menu.file.lastopened.name"));
        saveMenuItem.setText(lang.get("menu.file.save.name"));
        saveAsMenuItem.setText(lang.get("menu.file.saveas.name"));
        closeMenuItem.setText(lang.get("menu.file.close.name"));
        exitMenuItem.setText(lang.get("menu.file.exit.name"));
        helpMenu.setText(lang.get("menu.help.name"));
        langMenuItem.setText(lang.get("menu.help.language.name"));
        optionsMenuItem.setText(lang.get("menu.help.options.name"));
        aboutMenuItem.setText(lang.get("menu.help.about.name"));
        newButton.setToolTipText(lang.get("menu.file.new.name"));
        openButton.setToolTipText(lang.get("menu.file.open.name"));
        nextKeyButton.setToolTipText(lang.get("button.nextkey"));
        nextNewKeyButton.setToolTipText(lang.get("button.nextnewkey"));
        newLangButton.setToolTipText(lang.get("button.newlanguage"));
        removeLangButton.setToolTipText(lang.get("button.removelanguage"));
        setMasterButton.setToolTipText(lang.get("button.setmaster"));
        newKeyButton.setToolTipText(lang.get("button.newkey"));
        removeKeyButton.setToolTipText(lang.get("button.removekey"));
        renameKeyButton.setToolTipText(lang.get("button.renamekey"));
        revertKeyButton.setToolTipText(lang.get("button.revertkey"));
        useDefaultTextButton.setText(lang.get("button.usedefault"));
        useDefaultTextButton.setToolTipText(lang.get("button.usedefault.tooltip"));
        useDefaultCommentButton.setText(lang.get("button.usedefault"));
        useDefaultCommentButton.setToolTipText(lang.get("button.usedefault.tooltip"));
        valueLabel.setText(lang.get("editor.translation"));
        commentLabel.setText(lang.get("editor.comments"));
        defaultValueTextArea.setToolTipText(lang.get("editor.textarea.value.tooltip"));
        defaultCommentTextArea.setToolTipText(lang.get("editor.textarea.comment.tooltip"));
    }

    private JMenuItem aboutMenuItem;

    private JMenuItem closeMenuItem;

    private JLabel commentLabel;

    private JPanel commentPanel;

    private JScrollPane commentScrollPane;

    private JTextArea commentTextArea;

    private JScrollPane defaultCommentScrollPane;

    private JTextArea defaultCommentTextArea;

    private JScrollPane defaultValueScrollPane;

    private JTextArea defaultValueTextArea;

    private JMenuItem exitMenuItem;

    private JMenu fileMenu;

    private Separator fileMenuSeparator;

    private JMenu helpMenu;

    private Separator helpMenuSeparator;

    private JSplitPane horizontalSplitPane;

    private JScrollPane keysScrollPane;

    private JTable keysTable;

    private JPanel keysTablePanel;

    private JMenuItem langMenuItem;

    private JTree languageTree;

    private JScrollPane languageTreeScrollPane;

    private JMenu lastOpenedMenu;

    private JSplitPane leftSideSplitPane;

    private JMenuBar menuBar;

    private JButton newButton;

    private JButton newKeyButton;

    private JButton newLangButton;

    private JMenuItem newMenuItem;

    private JButton nextKeyButton;

    private JButton nextNewKeyButton;

    private JButton openButton;

    private JMenuItem openMenuItem;

    private JMenuItem optionsMenuItem;

    private JButton removeKeyButton;

    private JButton removeLangButton;

    private JButton renameKeyButton;

    private JButton revertKeyButton;

    private JSplitPane rightSideSplitPane;

    private JMenuItem saveAsMenuItem;

    private JMenuItem saveMenuItem;

    private JButton setMasterButton;

    private JProgressBar statusProgressBar;

    private JToolBar toolBar;

    private JToolBar.Separator toolBarSeparator1;

    private JToolBar.Separator toolBarSeparator2;

    private JToolBar.Separator toolBarSeparator3;

    private JToolBar.Separator toolBarSeparator4;

    private JButton useDefaultCommentButton;

    private JButton useDefaultTextButton;

    private JLabel valueLabel;

    private JPanel valuePanel;

    private JScrollPane valueScrollPane;

    private JTextArea valueTextArea;
}
