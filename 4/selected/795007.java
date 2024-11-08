package org.microskills.ZIPAnywhere;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.zip.ZipFile;
import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import org.microskills.compress.zip.Zip;

public class ZIPAnywhere extends JFrame implements ActionListener {

    static final int TOTAL_RECENT_FILES = 5;

    File currentArchive;

    File workingDir;

    Vector recentFileNames;

    Parameters param;

    JToolBar tb;

    ZipTablePanel pnlZipTable;

    FileTreePanel fileTree;

    JScrollPane treeScroller;

    JSplitPane splitPane;

    JMenuBar mainJMenuBar;

    JMenu fileMenu;

    JMenuItem newMenuItem;

    JMenuItem openMenuItem;

    JMenuItem closeMenuItem;

    JMenuItem exeMenuItem;

    JMenuItem splitMenuItem;

    JMenuItem wizardMenuItem;

    JMenuItem propertyMenuItem;

    JMenuItem reportMenuItem;

    JMenuItem printMenuItem;

    JMenuItem[] recentFilesMenuItem;

    JMenuItem exitMenuItem;

    JMenu actionMenu;

    JMenuItem addMenuItem;

    JMenuItem delMenuItem;

    JMenuItem extractMenuItem;

    JMenuItem viewMenuItem;

    JMenuItem selAllMenuItem;

    JMenuItem deSelAllMenuItem;

    JMenu sortMenu;

    JRadioButtonMenuItem sortNameMenuItem;

    JRadioButtonMenuItem sortTimeMenuItem;

    JRadioButtonMenuItem sortSizeMenuItem;

    JRadioButtonMenuItem sortPathMenuItem;

    JMenu optionMenu;

    JMenu columnMenu;

    JMenu colorMenu;

    JMenuItem backColorMenuItem;

    JMenuItem foreColorMenuItem;

    JCheckBoxMenuItem[] columnMenuItem;

    JMenu defaultViewerMenu;

    JRadioButtonMenuItem[] viewerMenuItem;

    JCheckBoxMenuItem showFileExplorerMenuItem;

    JCheckBoxMenuItem tipMenuItem;

    JCheckBoxMenuItem saveOnExitMenuItem;

    JMenu helpMenu;

    JMenuItem topicMenuItem;

    JMenuItem aboutMenuItem;

    JButton newButton;

    JButton openButton;

    JButton closeButton;

    JButton addButton;

    JButton extractButton;

    JButton delButton;

    JButton viewButton;

    JButton topicButton;

    JButton aboutButton;

    private Zip zipFile;

    private JFileChooser fileChooser;

    public ZIPAnywhere() throws IOException {
        zipFile = new Zip();
        workingDir = new File(".");
        mainJMenuBar = new JMenuBar();
        fileMenu = new JMenu("File");
        newMenuItem = new JMenuItem("New Archive...");
        openMenuItem = new JMenuItem("Open Archive...");
        closeMenuItem = new JMenuItem("Close Archive");
        exeMenuItem = new JMenuItem("Make Self Extractable Jar");
        splitMenuItem = new JMenuItem("Split/Merge Files...");
        wizardMenuItem = new JMenuItem("Wizard...");
        propertyMenuItem = new JMenuItem("Archive Property...");
        reportMenuItem = new JMenuItem("Summary Report...");
        printMenuItem = new JMenuItem("Print...");
        recentFilesMenuItem = new JMenuItem[5];
        exitMenuItem = new JMenuItem("Exit");
        actionMenu = new JMenu("Actions");
        addMenuItem = new JMenuItem("Add...");
        delMenuItem = new JMenuItem("Delete...");
        extractMenuItem = new JMenuItem("Extract...");
        viewMenuItem = new JMenuItem("View...");
        selAllMenuItem = new JMenuItem("Select All");
        deSelAllMenuItem = new JMenuItem("DeSelect All");
        sortMenu = new JMenu("Sort");
        sortNameMenuItem = new JRadioButtonMenuItem("By File Name");
        sortTimeMenuItem = new JRadioButtonMenuItem("By File Modified Time");
        sortSizeMenuItem = new JRadioButtonMenuItem("By File Size");
        sortPathMenuItem = new JRadioButtonMenuItem("By Directory Path");
        optionMenu = new JMenu("Option");
        columnMenu = new JMenu("Show/Hide Columns");
        colorMenu = new JMenu("Colors");
        backColorMenuItem = new JMenuItem("Background Color");
        foreColorMenuItem = new JMenuItem("Foreground Color");
        columnMenuItem = new JCheckBoxMenuItem[ZipTableModel.colInfos.length];
        defaultViewerMenu = new JMenu("Default Viewer");
        viewerMenuItem = new JRadioButtonMenuItem[ZipTablePanel.viewerNames.length];
        showFileExplorerMenuItem = new JCheckBoxMenuItem("Show File Explorer");
        tipMenuItem = new JCheckBoxMenuItem("Show Tooltips");
        saveOnExitMenuItem = new JCheckBoxMenuItem("Save On Exits");
        helpMenu = new JMenu("Help");
        topicMenuItem = new JMenuItem("Help Topics...");
        aboutMenuItem = new JMenuItem("About ZipAnywhere...");
        newButton = new JButton(JarIcon.getImageIcon("/images/new24.gif"));
        openButton = new JButton(JarIcon.getImageIcon("/images/open24.gif"));
        closeButton = new JButton(JarIcon.getImageIcon("/images/close24.gif"));
        addButton = new JButton(JarIcon.getImageIcon("/images/import24.gif"));
        extractButton = new JButton(JarIcon.getImageIcon("/images/export24.gif"));
        delButton = new JButton(JarIcon.getImageIcon("/images/delete24.gif"));
        viewButton = new JButton(JarIcon.getImageIcon("/images/find24.gif"));
        topicButton = new JButton(JarIcon.getImageIcon("/images/topic24.gif"));
        aboutButton = new JButton(JarIcon.getImageIcon("/images/about24.gif"));
        setTitle("ZipAnywhere");
        setIconImage(JarIcon.getImageIcon("/images/archive16.gif").getImage());
        setDefaultCloseOperation(0);
        addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent windowevent) {
                exitApplication();
            }
        });
        getContentPane().setLayout(new BorderLayout(0, 0));
        pnlZipTable = new ZipTablePanel();
        param = new Parameters("zipany.cfg");
        param.loadParameters();
        pnlZipTable.setupRenderers();
        pnlZipTable.setDefaultViewer(param.defaultViewerType);
        pnlZipTable.getTable().setBackground(param.tableBackgroundColor);
        pnlZipTable.getTable().setForeground(param.tableForegroundColor);
        pnlZipTable.setBackground(param.tableBackgroundColor);
        pnlZipTable.setDisplayColumns(param.tableColumns);
        fileTree = new FileTreePanel();
        fileTree.getTree().addTreeSelectionListener(new TreeListener());
        JScrollPane jscrollpane = new JScrollPane(fileTree);
        jscrollpane.setMinimumSize(new Dimension(0, 0));
        splitPane = new JSplitPane(1, jscrollpane, pnlZipTable);
        splitPane.setContinuousLayout(true);
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerLocation(175);
        getContentPane().add(splitPane, "Center");
        getContentPane().add(createToolBar(), "North");
        setupMenu();
        updateMenuToolbarState();
        Dimension dimension = getToolkit().getScreenSize();
        if (dimension.width >= 800 && dimension.height >= 600) {
            setSize(new Dimension(800, 600));
        } else {
            setSize(dimension);
        }
        Dimension dimension1 = getToolkit().getScreenSize();
        Dimension dimension2 = getSize();
        setLocation((dimension1.width - dimension2.width) / 2, (dimension1.height - dimension2.height) / 2);
        setVisible(true);
    }

    protected class TreeListener implements TreeSelectionListener {

        public void valueChanged(TreeSelectionEvent treeselectionevent) {
            File file = fileTree.getSelectedFile();
            if (file == null) {
                return;
            }
            if (!file.isDirectory()) {
                openArchive(file);
            } else {
                closeArchive();
            }
        }

        public TreeListener() {
        }
    }

    public void actionPerformed(ActionEvent actionevent) {
        String s = actionevent.getActionCommand();
        if (s.equals("new")) {
            fileTree.getTree().clearSelection();
            newArchive();
        } else if (s.equals("open")) {
            fileTree.getTree().clearSelection();
            openArchive();
        } else if (s.equals("add")) {
            addFiles();
        } else if (s.equals("extract")) {
            extractFiles();
        } else if (s.equals("close")) {
            closeArchive();
        } else if (s.equals("exe")) {
            Thread thread = new Thread() {

                public void run() {
                    makeExeArchive();
                }
            };
            thread.start();
        } else if (s.equals("split")) {
            new FileSplitterMerger(this, currentArchive);
        } else if (s.equals("property")) {
            showPropDialog();
        } else if (s.equals("report")) {
            pnlZipTable.getSummaryReport();
        } else if (s.equals("print")) {
            Thread thread1 = new Thread() {

                public void run() {
                    pnlZipTable.printZipTable();
                }
            };
            thread1.start();
        } else if (s.equals("recentFiles")) {
            fileTree.getTree().clearSelection();
            JMenuItem jmenuitem = (JMenuItem) actionevent.getSource();
            String s2 = jmenuitem.getText().substring(3);
            String s3 = s2.substring(s2.lastIndexOf(File.separator) + 1);
            String s4 = s2.substring(0, s2.lastIndexOf(File.separator) + 1);
            openArchive(new File(s4, s3));
        } else if (s.equals("exit")) {
            exitApplication();
        } else if (s.equals("view")) {
            Color color = JColorChooser.showDialog(this, "Color Chooser", Color.red);
            if (color != null) {
                tb.setBackground(color);
                tb.repaint();
            }
        } else if (s.equals("column")) {
            for (int i = 0; i < ZipTableModel.colInfos.length; i++) {
                if (columnMenuItem[i] == actionevent.getSource()) {
                    pnlZipTable.setColumnVisible(i, columnMenuItem[i].isSelected());
                }
            }
        } else if (s.equals("viewer")) {
            for (int j = 0; j < ZipTablePanel.viewerNames.length; j++) {
                if (viewerMenuItem[j].isSelected()) {
                    pnlZipTable.setDefaultViewer(j);
                }
            }
        } else if (s.equals("about")) {
            String s1 = "ZIPAnywhere Version 0.1 A modified version by Christopher Lim.\n" + "Based on the application of the same name by Zunhe Steve JIN";
            JOptionPane.showMessageDialog(this, s1, "ZipAnywhere --- About", -1);
        } else if (s.equals("topic")) {
            File file = new File("zipuser.rtf");
            if (!file.exists()) {
                JarIcon.copy("zipuser.rtf", file);
            }
            new HelpViewer(new File("ZipUser.rtf"));
        }
    }

    public void closeArchive() {
        pnlZipTable.closeArchive();
        setTitle("ZipAnywhere --- No archive opened");
        updateMenuToolbarState();
    }

    public JToolBar createToolBar() {
        tb = new JToolBar();
        tb.setFloatable(true);
        newButton.setToolTipText("Setup A New Archive File");
        newButton.setActionCommand("new");
        newButton.addActionListener(this);
        newButton.setRolloverEnabled(true);
        tb.add(newButton);
        openButton.setToolTipText("Open An Existing Archive File");
        openButton.setActionCommand("open");
        openButton.addActionListener(this);
        openButton.setRolloverEnabled(true);
        tb.add(openButton);
        closeButton.setToolTipText("Close Current Archive File");
        closeButton.setActionCommand("close");
        closeButton.addActionListener(this);
        closeButton.setRolloverEnabled(true);
        tb.add(closeButton);
        tb.addSeparator();
        addButton.setToolTipText("Add Files into Current Archive");
        addButton.setActionCommand("add");
        addButton.addActionListener(this);
        addButton.setRolloverEnabled(true);
        tb.add(addButton);
        extractButton.setToolTipText("Extract Files from Current Archive");
        extractButton.setActionCommand("extract");
        extractButton.addActionListener(this);
        extractButton.setRolloverEnabled(true);
        tb.add(extractButton);
        delButton.setToolTipText("Delete Selected Files from Current Archinve");
        delButton.setActionCommand("del");
        delButton.addActionListener(pnlZipTable);
        delButton.setRolloverEnabled(true);
        tb.add(delButton);
        viewButton.setToolTipText("View Current File");
        viewButton.setActionCommand("view");
        viewButton.addActionListener(pnlZipTable);
        viewButton.setRolloverEnabled(true);
        tb.add(viewButton);
        tb.addSeparator();
        topicButton.setActionCommand("topic");
        topicButton.setToolTipText("Help Topics");
        topicButton.setRolloverEnabled(true);
        topicButton.addActionListener(this);
        tb.add(topicButton);
        aboutButton.setActionCommand("about");
        aboutButton.setToolTipText("Product Information About ZipAnywhere");
        aboutButton.addActionListener(this);
        aboutButton.setRolloverEnabled(true);
        tb.add(aboutButton);
        return tb;
    }

    void exitApplication() {
        setVisible(false);
        param.defaultViewerType = pnlZipTable.getDefaultViewer();
        param.tableColumns = pnlZipTable.getDisplayColumns();
        param.recentFileNames = recentFileNames;
        param.tableBackgroundColor = pnlZipTable.getTable().getBackground();
        param.tableForegroundColor = pnlZipTable.getTable().getForeground();
        param.saveParameters();
        dispose();
        System.exit(0);
    }

    public static void main(String[] args) throws IOException {
        ZIPAnywhere jzip = new ZIPAnywhere();
        if (args.length == 1) {
            jzip.openArchive(new File(args[0]));
        }
    }

    public void makeExeArchive() {
        int i = JOptionPane.showConfirmDialog(this, "ZipAnywhere will create a Jar file with same file name in the same directory.\nIf current opened is a Jar file, the self-extractable will insert 1 just before \".\" \nThis might change the manifest file of original executable Jar file.\nThis program will overwrite existing Jar file without notice.\nDo you want to contiue?", "Warning", 0, 2, null);
        if (i == 1) {
            return;
        }
        setCursor(Cursor.getPredefinedCursor(3));
        fileTree.setCursor(Cursor.getPredefinedCursor(3));
        File file = new File("ZipSelfExtractor.class");
        File file1 = new File("META-INF/MANIFEST.MF");
        JarIcon.copy("ZipSelfExtractor.class", file);
        JarIcon.copy("jarmanifest", file1);
        String s = null;
        String s1 = null;
        try {
            s = currentArchive.getCanonicalPath();
        } catch (Exception _ex) {
        }
        int j = s.lastIndexOf(".zip");
        if (j != -1) {
            s1 = s.substring(0, j) + ".jar";
        } else {
            s1 = s.substring(0, s.length() - 4) + "1.jar";
        }
        JarIcon.copy(s, s1);
        File file2 = new File(".");
        File[] afile = { file, file1 };
        pnlZipTable.makeExeArchive(new File(s1), afile, file2);
        setCursor(Cursor.getPredefinedCursor(0));
        fileTree.setCursor(Cursor.getPredefinedCursor(0));
        JOptionPane.showMessageDialog(this, "Self extractable Jar file:\n" + s1 + "\n has been created!", "ZipAnywhere Self Extractor Maker", 1);
    }

    public void newArchive() {
        fileChooser = new JFileChooser();
        fileChooser.addChoosableFileFilter(new SimpleFileFilter("zip", "ZIP Files"));
        fileChooser.addChoosableFileFilter(new SimpleFileFilter("jar", "JAR Files"));
        fileChooser.setDialogTitle("Create a New Archive");
        fileChooser.addChoosableFileFilter(new ArchiveFileFilter());
        fileChooser.setMultiSelectionEnabled(false);
        fileChooser.setFileSelectionMode(0);
        fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
        fileChooser.setCurrentDirectory(workingDir);
        if (fileChooser.showDialog(this, "Create a new archive") == JFileChooser.CANCEL_OPTION) {
            return;
        }
        zipFile.setCurrentArchive(fileChooser.getSelectedFile().getAbsolutePath());
        workingDir = fileChooser.getSelectedFile().getParentFile();
        if (zipFile.fileExists()) {
            int j = JOptionPane.showConfirmDialog(this, "This file exists already. \nDo you want to overwrite existing one?", "Warning", 0, 2, null);
            if (j == JOptionPane.OK_OPTION) {
                zipFile.fileDelete();
            }
        }
        this.addFiles();
        updateMenuToolbarState();
    }

    public void openArchive() {
        fileChooser = new JFileChooser();
        fileChooser.addChoosableFileFilter(new SimpleFileFilter("zip", "ZIP Files"));
        fileChooser.addChoosableFileFilter(new SimpleFileFilter("jar", "JAR Files"));
        fileChooser.setDialogTitle("Open Archive");
        javax.swing.filechooser.FileFilter filefilter = fileChooser.getAcceptAllFileFilter();
        fileChooser.removeChoosableFileFilter(filefilter);
        fileChooser.setFileFilter(new ArchiveFileFilter());
        fileChooser.setMultiSelectionEnabled(false);
        fileChooser.setFileSelectionMode(0);
        fileChooser.setDialogType(0);
        fileChooser.setCurrentDirectory(workingDir);
        int i = fileChooser.showDialog(this, "Open");
        if (i == 0) {
            zipFile.setCurrentArchive(fileChooser.getSelectedFile().getAbsolutePath());
            setTitle("ZipAnywhere - " + fileChooser.getSelectedFile().getName());
            zipFile.setCurrentArchive(fileChooser.getSelectedFile().getAbsolutePath());
            pnlZipTable.getTableModel().setData(zipFile.getZipFileData());
            updateMenuToolbarState();
        }
    }

    public void openArchive(File file) {
        currentArchive = file;
        try {
            pnlZipTable.openArchive(currentArchive);
            setTitle("ZipAnywhere --- " + currentArchive.getName());
        } catch (Exception exception) {
            System.out.println(exception);
        }
        updateMenuToolbarState();
    }

    public void setupMenu() {
        mainJMenuBar.setOpaque(true);
        mainJMenuBar.add(fileMenu);
        fileMenu.setMnemonic('F');
        fileMenu.add(newMenuItem);
        newMenuItem.setAccelerator(KeyStroke.getKeyStroke(78, 2));
        fileMenu.add(openMenuItem);
        openMenuItem.setAccelerator(KeyStroke.getKeyStroke(79, 2));
        fileMenu.add(closeMenuItem);
        closeMenuItem.setAccelerator(KeyStroke.getKeyStroke(67, 2));
        fileMenu.addSeparator();
        fileMenu.add(exeMenuItem);
        fileMenu.add(splitMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(propertyMenuItem);
        fileMenu.add(reportMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(printMenuItem);
        fileMenu.addSeparator();
        recentFileNames = param.recentFileNames;
        for (int i = 0; i < 5; i++) {
            recentFilesMenuItem[i] = new JMenuItem("");
            recentFilesMenuItem[i].setActionCommand("recentFiles");
            recentFilesMenuItem[i].addActionListener(this);
        }
        for (int j = 0; j < recentFileNames.size(); j++) {
            recentFilesMenuItem[j].setText((j + 1) + ". " + (String) recentFileNames.elementAt(j));
            fileMenu.add(recentFilesMenuItem[j]);
        }
        fileMenu.addSeparator();
        fileMenu.add(exitMenuItem);
        newMenuItem.setActionCommand("new");
        newMenuItem.addActionListener(this);
        openMenuItem.setActionCommand("open");
        openMenuItem.addActionListener(this);
        closeMenuItem.setActionCommand("close");
        closeMenuItem.addActionListener(this);
        exeMenuItem.setActionCommand("exe");
        exeMenuItem.addActionListener(this);
        splitMenuItem.setActionCommand("split");
        splitMenuItem.addActionListener(this);
        propertyMenuItem.setActionCommand("property");
        reportMenuItem.setActionCommand("report");
        printMenuItem.setActionCommand("print");
        printMenuItem.addActionListener(this);
        propertyMenuItem.addActionListener(this);
        reportMenuItem.addActionListener(this);
        exitMenuItem.setActionCommand("exit");
        exitMenuItem.addActionListener(this);
        mainJMenuBar.add(actionMenu);
        actionMenu.setMnemonic('A');
        actionMenu.add(addMenuItem);
        actionMenu.add(delMenuItem);
        actionMenu.add(extractMenuItem);
        actionMenu.add(viewMenuItem);
        actionMenu.addSeparator();
        actionMenu.add(selAllMenuItem);
        actionMenu.add(deSelAllMenuItem);
        actionMenu.addSeparator();
        actionMenu.add(sortMenu);
        ButtonGroup buttongroup = new ButtonGroup();
        sortMenu.add(sortNameMenuItem);
        sortNameMenuItem.setSelected(true);
        buttongroup.add(sortNameMenuItem);
        sortMenu.add(sortSizeMenuItem);
        buttongroup.add(sortSizeMenuItem);
        sortMenu.add(sortTimeMenuItem);
        buttongroup.add(sortTimeMenuItem);
        sortMenu.add(sortPathMenuItem);
        buttongroup.add(sortPathMenuItem);
        sortNameMenuItem.setActionCommand("sortName");
        sortNameMenuItem.addActionListener(pnlZipTable);
        sortSizeMenuItem.setActionCommand("sortSize");
        sortSizeMenuItem.addActionListener(pnlZipTable);
        sortTimeMenuItem.setActionCommand("sortTime");
        sortTimeMenuItem.addActionListener(pnlZipTable);
        sortPathMenuItem.setActionCommand("sortPath");
        sortPathMenuItem.addActionListener(pnlZipTable);
        addMenuItem.setActionCommand("add");
        addMenuItem.addActionListener(pnlZipTable);
        delMenuItem.setActionCommand("del");
        delMenuItem.addActionListener(pnlZipTable);
        extractMenuItem.setActionCommand("extract");
        extractMenuItem.addActionListener(this);
        viewMenuItem.setActionCommand("view");
        viewMenuItem.addActionListener(pnlZipTable);
        selAllMenuItem.setActionCommand("selAll");
        selAllMenuItem.addActionListener(pnlZipTable);
        deSelAllMenuItem.setActionCommand("deSelAll");
        deSelAllMenuItem.addActionListener(pnlZipTable);
        optionMenu.setMnemonic('p');
        mainJMenuBar.add(optionMenu);
        optionMenu.add(columnMenu);
        for (int k = 0; k < columnMenuItem.length; k++) {
            columnMenuItem[k] = new JCheckBoxMenuItem(pnlZipTable.getTableModel().getColumnName(k));
            columnMenu.add(columnMenuItem[k]);
            columnMenuItem[k].setActionCommand("column");
            columnMenuItem[k].addActionListener(this);
            if (ZipTableModel.colInfos[k].visible) {
                columnMenuItem[k].setSelected(true);
            }
        }
        optionMenu.add(defaultViewerMenu);
        ButtonGroup buttongroup1 = new ButtonGroup();
        for (int l = 0; l < ZipTablePanel.viewerNames.length; l++) {
            viewerMenuItem[l] = new JRadioButtonMenuItem(ZipTablePanel.viewerNames[l]);
            defaultViewerMenu.add(viewerMenuItem[l]);
            buttongroup1.add(viewerMenuItem[l]);
            viewerMenuItem[l].setActionCommand("viewer");
            viewerMenuItem[l].addActionListener(this);
        }
        viewerMenuItem[pnlZipTable.getDefaultViewer()].setSelected(true);
        optionMenu.add(colorMenu);
        colorMenu.add(backColorMenuItem);
        colorMenu.add(foreColorMenuItem);
        backColorMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent actionevent) {
                Color color = JColorChooser.showDialog(ZIPAnywhere.this, "Choose color for backgroud", Color.white);
                if (color != null) {
                    pnlZipTable.setBackground(color);
                    pnlZipTable.getTable().setBackground(color);
                }
            }
        });
        foreColorMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent actionevent) {
                Color color = JColorChooser.showDialog(ZIPAnywhere.this, "Choose color for backgroud", Color.black);
                if (color != null) {
                    pnlZipTable.getTable().setForeground(color);
                }
            }
        });
        optionMenu.addSeparator();
        optionMenu.add(tipMenuItem);
        tipMenuItem.setSelected(true);
        optionMenu.add(saveOnExitMenuItem);
        optionMenu.addSeparator();
        saveOnExitMenuItem.setSelected(true);
        tipMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent actionevent) {
                JCheckBoxMenuItem jcheckboxmenuitem = (JCheckBoxMenuItem) actionevent.getSource();
                if (jcheckboxmenuitem.isSelected()) {
                    ToolTipManager.sharedInstance().setEnabled(true);
                } else {
                    ToolTipManager.sharedInstance().setEnabled(false);
                }
            }
        });
        helpMenu.setMnemonic('H');
        mainJMenuBar.add(helpMenu);
        topicMenuItem.setActionCommand("topic");
        helpMenu.add(topicMenuItem);
        aboutMenuItem.setActionCommand("about");
        helpMenu.add(aboutMenuItem);
        aboutMenuItem.addActionListener(this);
        topicMenuItem.addActionListener(this);
        setJMenuBar(mainJMenuBar);
    }

    public void showPropDialog() {
        try {
            if (currentArchive == null) {
                return;
            }
            ZipFile zipfile = new ZipFile(currentArchive);
            int i = 0;
            for (Enumeration enumeration = zipfile.entries(); enumeration.hasMoreElements(); ) {
                enumeration.nextElement();
                i++;
            }
            String s = "Archive File Name:  " + zipfile.getName() + "\n" + "Archive File Size:  " + currentArchive.length() + " bytes\n" + "Last Modified:      " + new Date(currentArchive.lastModified()) + "\n" + "Total Files Inside: " + i;
            JOptionPane.showMessageDialog(this, s, "ZipAnywhere --- Archive Property", -1);
        } catch (Exception _ex) {
        }
    }

    public void updateMenuToolbarState() {
        if (zipFile.getCurrentArchive() == null) {
            closeMenuItem.setEnabled(false);
            exeMenuItem.setEnabled(false);
            closeButton.setEnabled(false);
            propertyMenuItem.setEnabled(false);
            reportMenuItem.setEnabled(false);
            printMenuItem.setEnabled(false);
            actionMenu.setEnabled(false);
            addButton.setEnabled(false);
            extractButton.setEnabled(false);
            delButton.setEnabled(false);
            viewButton.setEnabled(false);
        } else {
            closeMenuItem.setEnabled(true);
            exeMenuItem.setEnabled(true);
            closeButton.setEnabled(true);
            addButton.setEnabled(true);
            extractButton.setEnabled(true);
            delButton.setEnabled(true);
            if (pnlZipTable.getSelectedRowCount() == 1) {
                viewButton.setEnabled(true);
                viewMenuItem.setEnabled(true);
            } else {
                viewButton.setEnabled(true);
                viewMenuItem.setEnabled(true);
            }
            propertyMenuItem.setEnabled(true);
            reportMenuItem.setEnabled(true);
            printMenuItem.setEnabled(true);
            actionMenu.setEnabled(true);
            try {
                String s = currentArchive.getAbsolutePath();
                int i = fileMenu.getItemCount();
                int j = recentFileNames.size();
                if (recentFileNames.contains(s)) {
                    recentFileNames.removeElement(s);
                } else if (j == 5) {
                    recentFileNames.removeElementAt(recentFileNames.size() - 1);
                } else {
                    fileMenu.insert(recentFilesMenuItem[j], i - 2);
                }
                recentFileNames.insertElementAt(s, 0);
                for (int k = 0; k < recentFileNames.size(); k++) {
                    recentFilesMenuItem[k].setText((k + 1) + ". " + (String) recentFileNames.elementAt(k));
                }
            } catch (Exception exception) {
                System.out.println("UpdateMenuBarState" + exception);
            }
        }
    }

    public void addFiles() {
        File[] fileSelection = null;
        fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File("."));
        fileChooser.setDialogType(0);
        fileChooser.setDialogTitle("Select Files/Directories to Add to " + currentArchive.getName());
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileSelectionMode(2);
        if (fileChooser.showDialog(this, "Add...") == JFileChooser.CANCEL_OPTION) {
            return;
        }
        File directory = fileChooser.getCurrentDirectory();
        fileSelection = fileChooser.getSelectedFiles();
        try {
            setTitle("ZipAnywhere - " + currentArchive.getName());
            zipFile.createZipFile(currentArchive, directory, fileSelection);
            pnlZipTable.getTableModel().setData(zipFile.getZipFileData());
        } catch (Exception exception) {
            System.out.println(exception);
        }
        updateMenuToolbarState();
    }

    private File[] getSelectedFiles(JFileChooser jfilechooser) {
        Container container = (Container) jfilechooser.getComponent(3);
        JList jlist = null;
        Container container1;
        for (; container != null; container = container1) {
            container1 = (Container) container.getComponent(0);
            if (!(container1 instanceof JList)) {
                continue;
            }
            jlist = (JList) container1;
            break;
        }
        Object[] aobj = jlist.getSelectedValues();
        File[] afile = new File[aobj.length];
        for (int i = 0; i < aobj.length; i++) {
            if (aobj[i] instanceof File) {
                afile[i] = (File) aobj[i];
            }
        }
        return afile;
    }

    public void extractFiles() {
        fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(workingDir);
        fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogTitle("Select destination directory for extracting " + zipFile.getCurrentArchive().substring(zipFile.getCurrentArchive().lastIndexOf(File.separatorChar) + 1));
        fileChooser.setMultiSelectionEnabled(false);
        if (fileChooser.showDialog(this, "Select") == JFileChooser.APPROVE_OPTION) {
            zipFile.extractZipFile(fileChooser.getSelectedFile(), pnlZipTable.getSelectedFiles());
            zipFile.extractZipFile(fileChooser.getCurrentDirectory(), pnlZipTable.getSelectedFiles());
        }
    }
}
