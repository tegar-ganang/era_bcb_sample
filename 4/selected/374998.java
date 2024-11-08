package de.javagimmicks.apps.isync;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Properties;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.TableColumn;
import org.apache.commons.io.FileUtils;
import de.javagimmicks.apps.isync.FolderDiffTableModel.RowInfo;
import de.javagimmicks.io.folderdiff.FolderDiff;
import de.javagimmicks.io.folderdiff.FolderDiffBuilder;
import de.javagimmicks.io.folderdiff.PathInfo;

public class ISyncApplication {

    public static void main(String[] args) {
        setErrorLog();
        setLAF();
        ISyncApplication app = new ISyncApplication();
        app.buildWindow();
        app._frmWindow.setVisible(true);
    }

    private final JFrame _frmWindow = new JFrame("ISync");

    private final JTable _tblSyncItems = new JTable(new FolderDiffTableModel());

    private final JTextField _txtSourceFolder = new JTextField();

    private final JTextField _txtSourceIncludes = new JTextField();

    private final JTextField _txtSourceExcludes = new JTextField();

    private final JTextField _txtTargetFolder = new JTextField();

    private final JTextField _txtTargetIncludes = new JTextField();

    private final JTextField _txtTargetExcludes = new JTextField();

    private final JTextField _txtStatus = new JTextField();

    private final JCheckBox _cbxLastMod = new JCheckBox("Last modified");

    private final JCheckBox _cbxSize = new JCheckBox("Size");

    private final JCheckBox _cbxChecksum = new JCheckBox("Checksum");

    private final JButton _btnScan = new JButton(new ScanAction("Scan", "Stop"));

    private final JButton _btnSync = new JButton(new SyncAction("Sync"));

    private void buildWindow() {
        JMenu settingsMenu = new JMenu("File");
        settingsMenu.add(new SaveAction("Save settings..."));
        settingsMenu.add(new LoadAction("Load settings..."));
        settingsMenu.addSeparator();
        settingsMenu.add(new AbstractAction("Exit") {

            private static final long serialVersionUID = 4935061869116047107L;

            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(settingsMenu);
        _txtStatus.setEditable(false);
        _txtStatus.setBackground(UIManager.getColor("control"));
        _frmWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        _frmWindow.setJMenuBar(menuBar);
        _frmWindow.getContentPane().add(buildMainPanel());
        _frmWindow.pack();
    }

    private JPanel buildMainPanel() {
        _tblSyncItems.addMouseListener(new CheckMenuMouseListener());
        _tblSyncItems.addMouseListener(new ShowCompareMouseListener());
        JPanel boxPanel = new JPanel(new GridLayout(1, 3, 5, 5));
        boxPanel.add(_cbxLastMod);
        boxPanel.add(_cbxSize);
        boxPanel.add(_cbxChecksum);
        boxPanel.setBorder(BorderFactory.createTitledBorder("Check these properties"));
        JPanel buttonPanel = new JPanel(new GridLayout(1, 3, 5, 5));
        buttonPanel.add(boxPanel);
        buttonPanel.add(_btnScan);
        buttonPanel.add(_btnSync);
        JPanel settingsPanel = buildSettingPanel();
        JPanel settingsAndActionPanel = new JPanel(new BorderLayout(5, 5));
        settingsAndActionPanel.add(settingsPanel, BorderLayout.CENTER);
        settingsAndActionPanel.add(buttonPanel, BorderLayout.SOUTH);
        JPanel result = new JPanel(new BorderLayout(10, 10));
        result.add(new JScrollPane(_tblSyncItems), BorderLayout.CENTER);
        result.add(settingsAndActionPanel, BorderLayout.NORTH);
        result.add(_txtStatus, BorderLayout.SOUTH);
        return result;
    }

    private JPanel buildSettingPanel() {
        JPanel sourcePanel = buildFolderPanel("source", _txtSourceFolder, _txtSourceIncludes, _txtSourceExcludes);
        JPanel targetPanel = buildFolderPanel("target", _txtTargetFolder, _txtTargetIncludes, _txtTargetExcludes);
        JPanel result = new JPanel(new GridLayout(1, 2, 10, 10));
        result.add(sourcePanel);
        result.add(targetPanel);
        return result;
    }

    private static void setMaxToPrefferred(JTable table, int columnIndex) {
        TableColumn column = table.getColumnModel().getColumn(columnIndex);
        column.setMaxWidth(column.getPreferredWidth());
    }

    private static List<String> getPatterns(JTextField textField) {
        String[] fragments = textField.getText().split(",");
        ArrayList<String> result = new ArrayList<String>(fragments.length);
        for (String fragment : fragments) {
            fragment = fragment.trim();
            if (fragment.length() > 0) {
                result.add(fragment);
            }
        }
        return result;
    }

    private static JPanel buildFolderPanel(String type, JTextField folderField, JTextField includeField, JTextField excludeField) {
        JPanel folderPanel = buildFolderChoosePanel(folderField);
        folderPanel.setBorder(BorderFactory.createTitledBorder("Choose " + type + " folder"));
        JPanel includeExcludePanel = buildIncludeExcludePanel(includeField, excludeField);
        includeExcludePanel.setBorder(BorderFactory.createTitledBorder("Specify " + type + " include/exclude patterns"));
        JPanel result = new JPanel(new GridLayout(2, 1, 10, 10));
        result.add(folderPanel);
        result.add(includeExcludePanel);
        return result;
    }

    private static JPanel buildFolderChoosePanel(final JTextField textField) {
        final JButton chooseButton = new JButton("...");
        chooseButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser();
                String path = textField.getText();
                if (path != null && path.length() > 0) {
                    File file = new File(path);
                    if (file.exists()) {
                        chooser.setCurrentDirectory(file.isDirectory() ? file : file.getParentFile());
                    }
                }
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int returnCode = chooser.showOpenDialog(chooseButton);
                if (returnCode == JFileChooser.APPROVE_OPTION) {
                    textField.setText(chooser.getSelectedFile().getAbsolutePath());
                }
            }
        });
        JPanel result = new JPanel(new BorderLayout(5, 5));
        result.add(textField, BorderLayout.CENTER);
        result.add(chooseButton, BorderLayout.EAST);
        return result;
    }

    private static JPanel buildIncludeExcludePanel(JTextField includeField, JTextField excludeField) {
        JPanel result = new JPanel(new GridLayout(1, 2, 5, 5));
        result.add(buildLabeledComponent("Include patterns", includeField));
        result.add(buildLabeledComponent("Exclude patterns", excludeField));
        return result;
    }

    private static JPanel buildLabeledComponent(final String caption, final Component component) {
        JPanel result = new JPanel(new BorderLayout(5, 5));
        result.add(new JLabel(caption), BorderLayout.WEST);
        result.add(component, BorderLayout.CENTER);
        return result;
    }

    private static void setLAF() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
        }
    }

    private static void setErrorLog() {
        File logDir = new File("log");
        logDir.mkdirs();
        try {
            System.setErr(new PrintStream(new FileOutputStream(new File(logDir, "error.log"))));
        } catch (FileNotFoundException ex) {
        }
    }

    private final class ScanAction extends AbstractAction {

        private static final long serialVersionUID = -7107936758055774035L;

        private final StatusFolderDiffListener _listener = new StatusFolderDiffListener(_txtStatus);

        private final String _name;

        private final String _nameStop;

        private Thread _scanThread;

        private ScanAction(String name, String nameStop) {
            super(name);
            _name = name;
            _nameStop = nameStop;
        }

        public void actionPerformed(ActionEvent e) {
            synchronized (this) {
                if (_scanThread == null) {
                    startScan();
                } else {
                    stopScan();
                }
            }
        }

        private void startScan() {
            _btnSync.setEnabled(false);
            _frmWindow.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            _scanThread = new Thread("Scan thread") {

                public void run() {
                    List<String> sourceIncludeList = getPatterns(_txtSourceIncludes);
                    List<String> sourceExcludeList = getPatterns(_txtSourceExcludes);
                    List<String> targetIncludeList = getPatterns(_txtTargetIncludes);
                    List<String> targetExcludeList = getPatterns(_txtTargetExcludes);
                    File sourceFolderFile = new File(_txtSourceFolder.getText());
                    File targetFolderFile = new File(_txtTargetFolder.getText());
                    try {
                        final FolderDiff folderDiff = new FolderDiffBuilder(sourceFolderFile, targetFolderFile).addSourceIncludes(sourceIncludeList).addSourceExcludes(sourceExcludeList).addTargetIncludes(targetIncludeList).addTargetExcludes(targetExcludeList).setCompareChecksum(_cbxChecksum.isSelected()).setCompareSize(_cbxSize.isSelected()).setCompareLastModified(_cbxLastMod.isSelected()).setFolderDiffListener(_listener).buildFolderDiff();
                        _listener.reset();
                        SwingUtilities.invokeLater(new Runnable() {

                            public void run() {
                                _tblSyncItems.setModel(new FolderDiffTableModel(folderDiff));
                                setMaxToPrefferred(_tblSyncItems, 0);
                                setMaxToPrefferred(_tblSyncItems, 2);
                                _tblSyncItems.getColumnModel().getColumn(1).setCellRenderer(new PathInfoCellRenderer());
                                _tblSyncItems.getColumnModel().getColumn(2).setCellRenderer(new DiffTypeCellRenderer());
                                _tblSyncItems.getColumnModel().getColumn(3).setCellRenderer(new PathInfoCellRenderer());
                            }
                        });
                    } catch (Throwable t) {
                        if (!(t instanceof ThreadDeath)) {
                            JOptionPane.showMessageDialog(_frmWindow, t.toString(), "Error occured during scan", JOptionPane.ERROR_MESSAGE);
                        }
                    } finally {
                        SwingUtilities.invokeLater(new Runnable() {

                            public void run() {
                                cleanUp();
                            }
                        });
                    }
                }
            };
            putValue(Action.NAME, _nameStop);
            _scanThread.start();
        }

        @SuppressWarnings("deprecation")
        private void stopScan() {
            if (_scanThread.isAlive()) {
                _scanThread.stop();
            }
            cleanUp();
        }

        private void cleanUp() {
            _scanThread = null;
            putValue(Action.NAME, _name);
            _txtStatus.setText("");
            _frmWindow.setCursor(Cursor.getDefaultCursor());
            _btnSync.setEnabled(true);
        }
    }

    private final class SyncAction extends AbstractAction {

        private static final long serialVersionUID = -1156665279980438962L;

        private SyncAction(String name) {
            super(name);
        }

        public void actionPerformed(ActionEvent e) {
            _btnScan.setEnabled(false);
            _btnSync.setEnabled(false);
            final FolderDiffTableModel model = (FolderDiffTableModel) _tblSyncItems.getModel();
            new Thread() {

                @Override
                public void run() {
                    for (ListIterator<RowInfo> iterator = model.getRowInfos().listIterator(); iterator.hasNext(); ) {
                        RowInfo rowInfo = iterator.next();
                        if (!rowInfo.isChecked()) {
                            continue;
                        }
                        switch(rowInfo.getType()) {
                            case SOURCE_ONLY:
                                {
                                    PathInfo pathInfo = rowInfo.getSourcePathInfo();
                                    File sourceFile = pathInfo.applyToFolder(model.getFolderDiff().getSourceFolder());
                                    File targetFile = pathInfo.applyToFolder(model.getFolderDiff().getTargetFolder());
                                    try {
                                        if (sourceFile.isFile()) {
                                            FileUtils.copyFile(sourceFile, targetFile);
                                        } else if (sourceFile.isDirectory()) {
                                            FileUtils.copyDirectory(sourceFile, targetFile);
                                        }
                                        iterator.remove();
                                    } catch (IOException ex) {
                                        ex.printStackTrace();
                                    }
                                    break;
                                }
                            case DIFFERENT:
                                {
                                    PathInfo pathInfo = rowInfo.getSourcePathInfo();
                                    File sourceFile = pathInfo.applyToFolder(model.getFolderDiff().getSourceFolder());
                                    File targetFile = pathInfo.applyToFolder(model.getFolderDiff().getTargetFolder());
                                    try {
                                        if (sourceFile.isFile()) {
                                            targetFile.delete();
                                            FileUtils.copyFile(sourceFile, targetFile);
                                        } else if (sourceFile.isDirectory()) {
                                            FileUtils.deleteDirectory(targetFile);
                                            FileUtils.copyDirectory(sourceFile, targetFile);
                                        }
                                        iterator.remove();
                                    } catch (IOException ex) {
                                        ex.printStackTrace();
                                    }
                                    break;
                                }
                            case TARGET_ONLY:
                                {
                                    PathInfo pathInfo = rowInfo.getTargetPathInfo();
                                    File targetFile = pathInfo.applyToFolder(model.getFolderDiff().getTargetFolder());
                                    try {
                                        if (targetFile.isFile()) {
                                            targetFile.delete();
                                        } else if (targetFile.isDirectory()) {
                                            FileUtils.deleteDirectory(targetFile);
                                        }
                                        iterator.remove();
                                    } catch (IOException ex) {
                                        ex.printStackTrace();
                                    }
                                    break;
                                }
                        }
                    }
                    model.fireTableDataChanged();
                    _btnScan.setEnabled(true);
                    _btnSync.setEnabled(true);
                }
            }.start();
        }
    }

    private static final String SETTINGS_EXTENSION = ".iss";

    private static final String PROP_SOURCE_DIR = "source.dir";

    private static final String PROP_SOURCE_INCLUDES = "source.includes";

    private static final String PROP_SOURCE_EXCLUDES = "source.excludes";

    private static final String PROP_TARGET_DIR = "target.dir";

    private static final String PROP_TARGET_INCLUDES = "target.includes";

    private static final String PROP_TARGET_EXCLUDES = "target.excludes";

    private static final String PROP_COMPARE_LASTMODIFIED = "compare.lastmodified";

    private static final String PROP_COMPARE_SIZE = "compare.size";

    private static final String PROP_COMPARE_CHECKSUM = "compare.checksum";

    private final class SaveAction extends AbstractAction {

        private static final long serialVersionUID = 7002695997612544471L;

        public SaveAction(String name) {
            super(name);
        }

        public void actionPerformed(ActionEvent e) {
            JFileChooser fileChooser = new JFileChooser(System.getProperty("user.dir"));
            fileChooser.setFileFilter(ISS_FILTER);
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            int userAction = fileChooser.showSaveDialog(_frmWindow);
            if (userAction == JFileChooser.CANCEL_OPTION) {
                return;
            }
            File file = fileChooser.getSelectedFile();
            Properties properties = new Properties();
            properties.setProperty(PROP_SOURCE_DIR, _txtSourceFolder.getText());
            properties.setProperty(PROP_SOURCE_INCLUDES, _txtSourceIncludes.getText());
            properties.setProperty(PROP_SOURCE_EXCLUDES, _txtSourceExcludes.getText());
            properties.setProperty(PROP_TARGET_DIR, _txtTargetFolder.getText());
            properties.setProperty(PROP_TARGET_INCLUDES, _txtTargetIncludes.getText());
            properties.setProperty(PROP_TARGET_EXCLUDES, _txtTargetExcludes.getText());
            properties.setProperty(PROP_COMPARE_LASTMODIFIED, String.valueOf(_cbxLastMod.isSelected()));
            properties.setProperty(PROP_COMPARE_SIZE, String.valueOf(_cbxSize.isSelected()));
            properties.setProperty(PROP_COMPARE_CHECKSUM, String.valueOf(_cbxChecksum.isSelected()));
            String fileName = file.getAbsolutePath();
            if (!fileName.endsWith(SETTINGS_EXTENSION)) {
                fileName += SETTINGS_EXTENSION;
            }
            try {
                properties.storeToXML(new FileOutputStream(fileName), "ISync settings file");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(_frmWindow, ex.toString(), "Cannot save settings", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private final class LoadAction extends AbstractAction {

        private static final long serialVersionUID = -2079950049055168166L;

        public LoadAction(String name) {
            super(name);
        }

        public void actionPerformed(ActionEvent e) {
            JFileChooser fileChooser = new JFileChooser(System.getProperty("user.dir"));
            fileChooser.setFileFilter(ISS_FILTER);
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            int userAction = fileChooser.showSaveDialog(_frmWindow);
            if (userAction == JFileChooser.CANCEL_OPTION) {
                return;
            }
            File file = fileChooser.getSelectedFile();
            Properties properties = new Properties();
            try {
                properties.loadFromXML(new FileInputStream(file));
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(_frmWindow, ex.toString(), "Cannot load settings", JOptionPane.ERROR_MESSAGE);
                return;
            }
            _txtSourceFolder.setText(properties.getProperty(PROP_SOURCE_DIR, ""));
            _txtSourceIncludes.setText(properties.getProperty(PROP_SOURCE_INCLUDES, ""));
            _txtSourceExcludes.setText(properties.getProperty(PROP_SOURCE_EXCLUDES, ""));
            _txtTargetFolder.setText(properties.getProperty(PROP_TARGET_DIR, ""));
            _txtTargetIncludes.setText(properties.getProperty(PROP_TARGET_INCLUDES, ""));
            _txtTargetExcludes.setText(properties.getProperty(PROP_TARGET_EXCLUDES, ""));
            _cbxLastMod.setSelected(Boolean.parseBoolean(properties.getProperty(PROP_COMPARE_LASTMODIFIED)));
            _cbxSize.setSelected(Boolean.parseBoolean(properties.getProperty(PROP_COMPARE_SIZE)));
            _cbxChecksum.setSelected(Boolean.parseBoolean(properties.getProperty(PROP_COMPARE_CHECKSUM)));
        }
    }

    private static final FileFilter ISS_FILTER = new FileFilter() {

        public boolean accept(File pathname) {
            return pathname.isDirectory() || pathname.getName().endsWith(SETTINGS_EXTENSION);
        }

        @Override
        public String getDescription() {
            return "ISync setting files";
        }
    };
}
