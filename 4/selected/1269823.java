package org.dsgt.ui;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import org.dsgt.data.Configuration;
import org.dsgt.event.GameTransferStatusCode;
import org.dsgt.event.GameTransferStatusEvent;
import org.dsgt.event.GameTransferStatusListener;
import org.dsgt.util.FileUtils;
import org.dsgt.util.GUIUtils;
import org.dsgt.util.StringUtils;

@SuppressWarnings("serial")
public class ApplicationFrame extends JFrame implements GameTransferStatusListener {

    private final long BYTES_IN_GIGABYTE = 1024L * 1024L * 1024L;

    private final long SAVE_GAME_SIZE_BYTES = 1024L * 512;

    private final String HANDS_OFF_FILE = "_ds_mshl.nds";

    private JComboBox profileCombo;

    private JList gameSelectionList;

    private JButton transferButton;

    private JButton cancelButton;

    private boolean transferInProgress;

    private double selectedGamesSize;

    private List<JComponent> componentsForStateChange = new ArrayList<JComponent>();

    private JLabel spaceAvailableLabel;

    private JProgressBar progressBar;

    private boolean cancelTransferIssued;

    private JLabel statusLabel;

    public ApplicationFrame() {
        this.init();
        this.addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                endTransfer();
                Rectangle r = ApplicationFrame.this.getBounds();
                Configuration c = Configuration.getInstance();
                c.setValue("org.dsgt.sticky.aframe.bounds.x", r.x);
                c.setValue("org.dsgt.sticky.aframe.bounds.y", r.y);
                c.setValue("org.dsgt.sticky.aframe.bounds.width", r.width);
                c.setValue("org.dsgt.sticky.aframe.bounds.height", r.height);
                c.store();
            }
        });
    }

    private void init() {
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setTitle(GUIUtils.getString("applicationFrame.title"));
        this.setIconImage(GUIUtils.getImage("applicationFrame.icon"));
        Configuration c = Configuration.getInstance();
        if (c.exists("org.dsgt.sticky.aframe.bounds")) {
            int x = c.getInt("org.dsgt.sticky.aframe.bounds.x");
            int y = c.getInt("org.dsgt.sticky.aframe.bounds.y");
            int width = c.getInt("org.dsgt.sticky.aframe.bounds.width");
            int height = c.getInt("org.dsgt.sticky.aframe.bounds.height");
            this.setBounds(x, y, width, height);
        } else {
            this.setBounds(60, 160, 600, 420);
        }
        this.getContentPane().setLayout(new BorderLayout());
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(this.createFileMenu());
        menuBar.add(this.createToolsMenu());
        menuBar.add(this.createViewMenu());
        menuBar.add(this.createHelpMenu());
        this.setJMenuBar(menuBar);
        this.getContentPane().add(this.createProfilePanel(), BorderLayout.NORTH);
        this.getContentPane().add(this.createGameSelectionPanel(), BorderLayout.CENTER);
        this.getContentPane().add(this.createStatusPanel(), BorderLayout.SOUTH);
        new Thread(new CardAvailabilityWatcher(), "CardAvailabilityWatcher").start();
    }

    private JPanel createProfilePanel() {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        JLabel profileLabel = new JLabel(GUIUtils.getString("profileLabel.text"));
        panel.add(profileLabel);
        panel.add(Box.createHorizontalStrut(4));
        this.profileCombo = new JComboBox();
        this.profileCombo.setFocusable(false);
        Dimension defaultPreferredSize = profileCombo.getPreferredSize();
        this.profileCombo.setPreferredSize(new Dimension(100, defaultPreferredSize.height));
        panel.add(this.profileCombo);
        panel.add(Box.createHorizontalStrut(4));
        JButton launchProfileManagerButton = new JButton(GUIUtils.getIcon("launchProfileManagerButton.icon"));
        launchProfileManagerButton.setFocusable(false);
        launchProfileManagerButton.setMaximumSize(new Dimension(launchProfileManagerButton.getPreferredSize().height, launchProfileManagerButton.getPreferredSize().height));
        launchProfileManagerButton.setPreferredSize(new Dimension(launchProfileManagerButton.getPreferredSize().height, launchProfileManagerButton.getPreferredSize().height));
        launchProfileManagerButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                String currentProfile = null;
                if (profileCombo.getSelectedItem() != null) {
                    currentProfile = profileCombo.getSelectedItem().toString();
                }
                ProfileConfigurationFrame pcFrame = new ProfileConfigurationFrame();
                pcFrame.setVisible(true);
                populateProfileCombo();
                profileCombo.setSelectedItem(currentProfile);
                populateGameSelectionList();
            }
        });
        this.populateProfileCombo();
        panel.add(launchProfileManagerButton);
        this.profileCombo.addActionListener(new ProfileComboListener());
        this.componentsForStateChange.add(profileLabel);
        this.componentsForStateChange.add(this.profileCombo);
        this.componentsForStateChange.add(launchProfileManagerButton);
        return panel;
    }

    private JPanel createGameSelectionPanel() {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createEmptyBorder());
        panel.setLayout(new BorderLayout());
        JScrollPane scrollPane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        this.gameSelectionList = new CheckBoxList();
        this.gameSelectionList.setVisibleRowCount(-1);
        this.gameSelectionList.setModel(new DefaultListModel());
        this.gameSelectionList.setLayoutOrientation(JList.VERTICAL_WRAP);
        scrollPane.getViewport().setView(this.gameSelectionList);
        this.populateGameSelectionList();
        panel.add(scrollPane, BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        ActionListener listener = new ButtonActionListener();
        this.transferButton = new JButton(GUIUtils.getString("transferButton.text"));
        this.transferButton.setName("transferButton");
        this.transferButton.addActionListener(listener);
        this.cancelButton = new JButton(GUIUtils.getString("cancelButton.text"));
        this.cancelButton.setName("cancelButton");
        this.cancelButton.addActionListener(listener);
        this.cancelButton.setEnabled(false);
        Dimension d = GUIUtils.getCommonComponentPreferredSize(this.transferButton, this.cancelButton);
        this.transferButton.setPreferredSize(d);
        this.cancelButton.setPreferredSize(d);
        this.statusLabel = new JLabel("");
        buttonPanel.add(this.statusLabel);
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(this.transferButton);
        buttonPanel.add(Box.createHorizontalStrut(2));
        buttonPanel.add(this.cancelButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        this.componentsForStateChange.add(this.gameSelectionList);
        return panel;
    }

    private JPanel createStatusPanel() {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        this.progressBar = new JProgressBar();
        Dimension progressDimension = new Dimension(300, this.progressBar.getPreferredSize().height);
        this.progressBar.setMaximumSize(progressDimension);
        this.progressBar.setPreferredSize(progressDimension);
        this.progressBar.setMinimumSize(progressDimension);
        panel.add(this.progressBar);
        panel.add(Box.createHorizontalGlue());
        this.spaceAvailableLabel = new JLabel("", JLabel.RIGHT);
        Dimension labelDimension = new Dimension(200, this.progressBar.getPreferredSize().height);
        this.spaceAvailableLabel.setMaximumSize(labelDimension);
        this.spaceAvailableLabel.setPreferredSize(labelDimension);
        this.spaceAvailableLabel.setMinimumSize(labelDimension);
        panel.add(this.spaceAvailableLabel);
        this.componentsForStateChange.add(this.spaceAvailableLabel);
        return panel;
    }

    private JMenu createFileMenu() {
        JMenu menu = new JMenu();
        menu.setText(GUIUtils.getString("fileMenu.text"));
        JMenuItem menuItem = new JMenuItem();
        menuItem.setText(GUIUtils.getString("fileMenu.exit.text"));
        menuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                WindowEvent evt = new WindowEvent(ApplicationFrame.this, WindowEvent.WINDOW_CLOSING);
                ApplicationFrame.this.processWindowEvent(evt);
            }
        });
        menu.add(menuItem);
        this.componentsForStateChange.add(menu);
        return menu;
    }

    private JMenu createToolsMenu() {
        JMenu menu = new JMenu();
        menu.setText(GUIUtils.getString("toolsMenu.text"));
        ButtonGroup group = new ButtonGroup();
        JMenuItem profileSubMenu = new JMenuItem(GUIUtils.getString("toolsMenu.profileManager.text"));
        profileSubMenu.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                String currentProfile = null;
                if (profileCombo.getSelectedItem() != null) {
                    currentProfile = profileCombo.getSelectedItem().toString();
                }
                ProfileConfigurationFrame pcFrame = new ProfileConfigurationFrame();
                pcFrame.setVisible(true);
                populateProfileCombo();
                profileCombo.setSelectedItem(currentProfile);
            }
        });
        menu.add(profileSubMenu);
        JMenuItem configurationSubMenu = new JMenuItem(GUIUtils.getString("toolsMenu.configuration.text"));
        configurationSubMenu.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ConfigurationFrame frame = new ConfigurationFrame();
                frame.setVisible(true);
            }
        });
        menu.add(configurationSubMenu);
        JMenu lafSubMenu = new JMenu();
        lafSubMenu.setText(GUIUtils.getString("toolsMenu.skins.text"));
        final LookAndFeelInfo[] lafInfo = UIManager.getInstalledLookAndFeels();
        String currentlafClassname = UIManager.getLookAndFeel().getClass().toString();
        for (int i = 0; i < lafInfo.length; i++) {
            if (lafInfo[i].getClassName().contains("org.jvnet.substance")) {
                JCheckBoxMenuItem cbMenuItem = new JCheckBoxMenuItem();
                cbMenuItem.setText(lafInfo[i].getName());
                group.add(cbMenuItem);
                if (currentlafClassname.equals("class " + lafInfo[i].getClassName())) {
                    cbMenuItem.setState(true);
                } else {
                    cbMenuItem.setState(false);
                }
                cbMenuItem.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        String laf = null;
                        try {
                            for (int j = 0; j < lafInfo.length; j++) {
                                if (lafInfo[j].getName().equals(e.getActionCommand())) {
                                    laf = lafInfo[j].getClassName();
                                    Configuration.getInstance().setValue("org.dsgt.sticky.laf", laf);
                                    break;
                                }
                            }
                            UIManager.setLookAndFeel(laf);
                            SwingUtilities.updateComponentTreeUI(ApplicationFrame.this);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                });
                lafSubMenu.add(cbMenuItem);
            }
        }
        menu.add(lafSubMenu);
        this.componentsForStateChange.add(menu);
        return menu;
    }

    private JMenu createViewMenu() {
        JMenu menu = new JMenu();
        menu.setText(GUIUtils.getString("viewMenu.text"));
        Configuration c = Configuration.getInstance();
        boolean viewAll = true;
        if (c.exists("org.dsgt.sticky.viewoption")) {
            if (c.getString("org.dsgt.sticky.viewoption").equals("selected")) {
                viewAll = false;
            }
        }
        JMenuItem viewAllMenuItem = new JRadioButtonMenuItem(GUIUtils.getString("viewMenu.viewAll.text"));
        viewAllMenuItem.setSelected(viewAll);
        viewAllMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Configuration.getInstance().setValue("org.dsgt.sticky.viewoption", "all");
                populateGameSelectionList();
            }
        });
        menu.add(viewAllMenuItem);
        JMenuItem viewSelectedMenuItem = new JRadioButtonMenuItem(GUIUtils.getString("viewMenu.viewSelected.text"));
        viewSelectedMenuItem.setSelected(!viewAll);
        viewSelectedMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Configuration.getInstance().setValue("org.dsgt.sticky.viewoption", "selected");
                populateGameSelectionList();
            }
        });
        menu.add(viewSelectedMenuItem);
        ButtonGroup group = new ButtonGroup();
        group.add(viewAllMenuItem);
        group.add(viewSelectedMenuItem);
        this.componentsForStateChange.add(menu);
        return menu;
    }

    private JMenu createHelpMenu() {
        JMenu menu = new JMenu();
        menu.setText(GUIUtils.getString("helpMenu.text"));
        JMenuItem helpMenuItem = new JMenuItem(GUIUtils.getString("helpMenu.help.text"));
        helpMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                try {
                    File helpFile = ApplicationFrame.this.findHelpFile();
                    if (helpFile.exists()) {
                        try {
                            Desktop.getDesktop().open(helpFile);
                        } catch (Throwable t) {
                            JOptionPane.showMessageDialog(ApplicationFrame.this, GUIUtils.getString("helpTarget.error"));
                        }
                    } else {
                        JOptionPane.showMessageDialog(ApplicationFrame.this, String.format(GUIUtils.getString("helpTarget.notFound"), helpFile.getAbsolutePath()));
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        menu.add(helpMenuItem);
        JMenuItem aboutMenuItem = new JMenuItem(GUIUtils.getString("helpMenu.about.text"));
        aboutMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                AboutFrame frame = new AboutFrame();
                frame.setVisible(true);
            }
        });
        menu.add(aboutMenuItem);
        this.componentsForStateChange.add(menu);
        return menu;
    }

    private void populateProfileCombo() {
        Configuration c = Configuration.getInstance();
        List<String> profileList = null;
        if (c.exists("org.dsgt.data.profiles")) {
            profileList = StringUtils.splitAsList(c.getString("org.dsgt.data.profiles"), "|");
            Collections.sort(profileList);
        }
        ComboBoxModel model = null;
        if (profileList != null) {
            model = new DefaultComboBoxModel(profileList.toArray());
        } else {
            model = new DefaultComboBoxModel();
        }
        this.profileCombo.setModel(model);
    }

    private void populateGameSelectionList() {
        if (this.profileCombo.getSelectedIndex() != -1) {
            String selectedProfile = this.profileCombo.getSelectedItem().toString();
            Configuration c = Configuration.getInstance();
            List<String> directoryList = null;
            Set<String> fileSet = null;
            if (c.exists("org.dsgt.data." + selectedProfile + ".directories")) {
                directoryList = StringUtils.splitAsList(c.getString("org.dsgt.data." + selectedProfile + ".directories"), "|");
            } else {
                directoryList = new ArrayList<String>();
            }
            if (c.exists("org.dsgt.data." + selectedProfile + ".files")) {
                fileSet = StringUtils.splitAsSet(c.getString("org.dsgt.data." + selectedProfile + ".files"), "|");
            } else {
                fileSet = new HashSet<String>();
            }
            SortedSet<String> fullFileListing = new TreeSet<String>();
            FilenameFilter filter = new FilenameFilter() {

                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".nds");
                }
            };
            for (String directory : directoryList) {
                File dir = new File(directory);
                if (dir.exists()) {
                    String[] fileNames = dir.list(filter);
                    fullFileListing.addAll(Arrays.asList(fileNames));
                }
            }
            DefaultListModel model = (DefaultListModel) this.gameSelectionList.getModel();
            model.removeAllElements();
            ActionListener listener = new GameSelectionListener();
            int tempWidth = 0;
            int width = 0;
            this.selectedGamesSize = 0.0;
            boolean viewSelectedOnly = false;
            if (c.exists("org.dsgt.sticky.viewoption")) {
                if (c.getString("org.dsgt.sticky.viewoption").equals("selected")) {
                    viewSelectedOnly = true;
                }
            }
            JCheckBox checkBox = null;
            for (String fileName : fullFileListing) {
                checkBox = new JCheckBox(fileName.substring(0, fileName.lastIndexOf('.')));
                checkBox.addActionListener(listener);
                if (fileSet.contains(fileName)) {
                    checkBox.setSelected(true);
                    File file = FileUtils.findFirstFileInstance(directoryList, fileName);
                    this.selectedGamesSize += (file.length() + SAVE_GAME_SIZE_BYTES) * 1.0 / BYTES_IN_GIGABYTE;
                }
                if (!viewSelectedOnly || (viewSelectedOnly && checkBox.isSelected())) {
                    model.addElement(checkBox);
                    if ((tempWidth = checkBox.getPreferredSize().width) > width) {
                        width = tempWidth;
                    }
                }
            }
            if (checkBox != null) {
                this.gameSelectionList.setFixedCellHeight(checkBox.getPreferredSize().height);
                this.gameSelectionList.setFixedCellWidth(width);
            }
        } else {
            DefaultListModel model = (DefaultListModel) this.gameSelectionList.getModel();
            model.removeAllElements();
        }
    }

    private File findHelpFile() {
        File helpFile = new File(StringUtils.pathCombine(System.getProperty("user.dir"), GUIUtils.getString("helpTarget")));
        if (!helpFile.exists()) {
            File applicationDirectory = new File(System.getProperty("user.dir"));
            applicationDirectory = applicationDirectory.getParentFile();
            helpFile = new File(applicationDirectory.getAbsolutePath(), GUIUtils.getString("helpTarget"));
        }
        return helpFile;
    }

    private void setGameSelection(String game, boolean selected) {
        if (this.profileCombo.getSelectedIndex() != -1) {
            String selectedProfile = this.profileCombo.getSelectedItem().toString();
            Configuration c = Configuration.getInstance();
            Set<String> fileSet = null;
            if (c.exists("org.dsgt.data." + selectedProfile + ".files")) {
                fileSet = StringUtils.splitAsSet(c.getString("org.dsgt.data." + selectedProfile + ".files"), "|");
                if (fileSet != null) {
                    if (selected) {
                        fileSet.add(game);
                    } else {
                        fileSet.remove(game);
                    }
                    c.setValue("org.dsgt.data." + selectedProfile + ".files", StringUtils.combineAsDelimited(fileSet, "|"));
                }
            } else {
                c.setValue("org.dsgt.data." + selectedProfile + ".files", game);
            }
            List<String> directoryList = null;
            if (c.exists("org.dsgt.data." + selectedProfile + ".directories")) {
                directoryList = StringUtils.splitAsList(c.getString("org.dsgt.data." + selectedProfile + ".directories"), "|");
            } else {
                directoryList = new ArrayList<String>();
            }
            File file = FileUtils.findFirstFileInstance(directoryList, game);
            if (selected) {
                this.selectedGamesSize += ((file.length() + SAVE_GAME_SIZE_BYTES) * 1.0 / BYTES_IN_GIGABYTE);
            } else {
                this.selectedGamesSize -= ((file.length() + SAVE_GAME_SIZE_BYTES) * 1.0 / BYTES_IN_GIGABYTE);
            }
        }
    }

    private void startTransfer() {
        String selectedProfile = this.profileCombo.getSelectedItem().toString();
        GameTransferManager gtm = new GameTransferManager(selectedProfile);
        gtm.addGameTransferStatusListener(this);
        new Thread(gtm, "GameTransferManager").start();
    }

    private void cancelTransfer() {
        this.cancelButton.setEnabled(false);
        this.cancelTransferIssued = true;
    }

    private void endTransfer() {
        this.cancelButton.setEnabled(false);
        for (JComponent component : this.componentsForStateChange) {
            component.setEnabled(true);
        }
        this.transferInProgress = false;
        this.progressBar.setValue(0);
        this.statusLabel.setText("");
    }

    public void transferEnded(final GameTransferStatusEvent event) {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                ApplicationFrame.this.endTransfer();
            }
        });
    }

    public void transferStarted(final GameTransferStatusEvent event) {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                ApplicationFrame.this.progressBar.setValue(0);
                ApplicationFrame.this.progressBar.setMaximum((int) event.getProgressMaximum());
                ApplicationFrame.this.transferInProgress = true;
                ApplicationFrame.this.cancelButton.setEnabled(true);
                ApplicationFrame.this.transferButton.setEnabled(false);
                for (JComponent component : ApplicationFrame.this.componentsForStateChange) {
                    component.setEnabled(false);
                }
            }
        });
    }

    public void transferProgressed(final GameTransferStatusEvent event) {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                ApplicationFrame.this.progressBar.setValue((int) event.getProgressUpdate());
            }
        });
    }

    public void transferFailed(final GameTransferStatusEvent event) {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                String message = null;
                String title = GUIUtils.getString("startTransfer.error.title");
                switch(event.getStatusCode()) {
                    case SOURCE_DIRECTORIES_NOT_SELECTED:
                    case SOURCE_FILES_NOT_SELECTED:
                        message = GUIUtils.getString("startTransfer.error.noGamesSelected");
                        break;
                    case SAVE_DIRECTORY_NOT_SELECTED:
                        message = GUIUtils.getString("startTransfer.error.noSaveDirectory");
                        break;
                    case CARD_DIRECTORY_NOT_SELECTED:
                        message = GUIUtils.getString("startTransfer.error.noCardDirectory");
                        break;
                }
                JOptionPane.showMessageDialog(ApplicationFrame.this, message, title, JOptionPane.OK_OPTION);
                ApplicationFrame.this.endTransfer();
            }
        });
    }

    private class ProfileComboListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            populateGameSelectionList();
        }
    }

    private class GameSelectionListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            JCheckBox checkBox = (JCheckBox) e.getSource();
            setGameSelection(checkBox.getText() + ".nds", checkBox.isSelected());
        }
    }

    private class ButtonActionListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            JButton button = (JButton) e.getSource();
            if (button.getName().equals("transferButton")) {
                startTransfer();
            } else if (button.getName().equals("cancelButton")) {
                cancelTransfer();
            }
        }
    }

    private class CardAvailabilityWatcher implements Runnable {

        final String availableSpaceValidLabel = GUIUtils.getString("availableSpaceLabel.text.valid");

        final String availableSpaceExceededLabel = GUIUtils.getString("availableSpaceLabel.text.exceeded");

        private double sizeOfSelections;

        private double availableSpace;

        public void run() {
            while (true) {
                Configuration c = Configuration.getInstance();
                boolean cardFound = false;
                if (!transferInProgress && c.exists("org.dsgt.data.card.directory")) {
                    File directory = new File(c.getString("org.dsgt.data.card.directory"));
                    if (directory.exists()) {
                        String[] listing = directory.list(new FilenameFilter() {

                            public boolean accept(File dir, String name) {
                                return dir.isDirectory();
                            }
                        });
                        for (String s : listing) {
                            if (s.equalsIgnoreCase("_system_")) {
                                if (ApplicationFrame.this.profileCombo.getSelectedIndex() != -1) {
                                    String profile = ApplicationFrame.this.profileCombo.getSelectedItem().toString();
                                    sizeOfSelections = this.calculateSelectionSize(profile) * 1.0 / BYTES_IN_GIGABYTE;
                                    availableSpace = this.calculateAvailableSpace(directory) * 1.0 / BYTES_IN_GIGABYTE;
                                    cardFound = true;
                                }
                                break;
                            }
                        }
                    }
                }
                if (cardFound) {
                    spaceAvailableLabel.setEnabled(true);
                    if (availableSpace >= sizeOfSelections) {
                        transferButton.setEnabled(true);
                        spaceAvailableLabel.setText(String.format(this.availableSpaceValidLabel, availableSpace - sizeOfSelections, availableSpace));
                    } else {
                        transferButton.setEnabled(false);
                        spaceAvailableLabel.setText(this.availableSpaceExceededLabel);
                    }
                } else {
                    transferButton.setEnabled(false);
                    spaceAvailableLabel.setEnabled(false);
                    spaceAvailableLabel.setText(String.format(this.availableSpaceValidLabel, 0.0, 0.0));
                }
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        private long calculateAvailableSpace(File directory) {
            File[] roms = directory.listFiles(new FilenameFilter() {

                public boolean accept(File dir, String name) {
                    String lowerName = name.toLowerCase();
                    return lowerName.endsWith(".nds") && !lowerName.equals(HANDS_OFF_FILE);
                }
            });
            long spaceOnCard = directory.getFreeSpace();
            for (File rom : roms) {
                spaceOnCard += rom.length();
                String saveFileName = rom.getName().substring(0, rom.getName().lastIndexOf('.')) + ".sav";
                File saveFile = new File(StringUtils.pathCombine(rom.getParent(), saveFileName));
                if (saveFile.exists()) {
                    spaceOnCard += saveFile.length();
                }
            }
            return spaceOnCard;
        }

        private long calculateSelectionSize(String profile) {
            long selectionSize = 0;
            Configuration c = Configuration.getInstance();
            List<String> directoryList = null;
            List<String> fileList = null;
            if (c.exists("org.dsgt.data." + profile + ".directories")) {
                directoryList = StringUtils.splitAsList(c.getString("org.dsgt.data." + profile + ".directories"), "|");
            }
            if (c.exists("org.dsgt.data." + profile + ".files")) {
                fileList = StringUtils.splitAsList(c.getString("org.dsgt.data." + profile + ".files"), "|");
            }
            if (directoryList != null && fileList != null && directoryList.size() > 0 && fileList.size() > 0) {
                List<File> games = FileUtils.findFirstFileInstances(directoryList, fileList);
                for (File game : games) {
                    selectionSize += game.length();
                    selectionSize += SAVE_GAME_SIZE_BYTES;
                }
            }
            return selectionSize;
        }
    }

    private class GameTransferManager implements Runnable {

        private final String profile;

        public GameTransferManager(String profile) {
            this.profile = profile;
        }

        private List<GameTransferStatusListener> transferListeners = new ArrayList<GameTransferStatusListener>();

        public void addGameTransferStatusListener(GameTransferStatusListener listener) {
            this.transferListeners.add(listener);
        }

        public void removeGameTransferStatusListener(GameTransferStatusListener listener) {
            this.transferListeners.remove(listener);
        }

        protected void fireTransferStarted(GameTransferStatusEvent event) {
            for (GameTransferStatusListener listener : this.transferListeners) {
                listener.transferStarted(event);
            }
        }

        protected void fireTransferProgressed(GameTransferStatusEvent event) {
            for (GameTransferStatusListener listener : this.transferListeners) {
                listener.transferProgressed(event);
            }
        }

        protected void fireTransferEnded(GameTransferStatusEvent event) {
            for (GameTransferStatusListener listener : this.transferListeners) {
                listener.transferEnded(event);
            }
        }

        protected void fireTransferFailed(GameTransferStatusEvent event) {
            for (GameTransferStatusListener listener : this.transferListeners) {
                listener.transferFailed(event);
            }
        }

        public void run() {
            Configuration c = Configuration.getInstance();
            List<String> directoryList = null;
            List<String> fileList = null;
            if (c.exists("org.dsgt.data." + this.profile + ".directories")) {
                directoryList = StringUtils.splitAsList(c.getString("org.dsgt.data." + this.profile + ".directories"), "|");
            }
            if (c.exists("org.dsgt.data." + this.profile + ".files")) {
                fileList = StringUtils.splitAsList(c.getString("org.dsgt.data." + this.profile + ".files"), "|");
            }
            File saveDirectory = null;
            if (c.exists("org.dsgt.data.saves.directory")) {
                String location = c.getString("org.dsgt.data.saves.directory");
                File f = new File(location);
                if (f.exists()) {
                    saveDirectory = f;
                }
            }
            File cardDirectory = null;
            if (c.exists("org.dsgt.data.card.directory")) {
                String location = c.getString("org.dsgt.data.card.directory");
                File f = new File(location);
                if (f.exists()) {
                    cardDirectory = f;
                }
            }
            if (directoryList != null && fileList != null && directoryList.size() > 0 && fileList.size() > 0 && saveDirectory != null && cardDirectory != null) {
                List<File> games = FileUtils.findFirstFileInstances(directoryList, fileList);
                long toBeTransferred = 0;
                long totalTransferred = 0;
                for (File game : games) {
                    toBeTransferred += game.length();
                }
                this.fireTransferStarted(new GameTransferStatusEvent(this, toBeTransferred, totalTransferred));
                File[] saveFiles = cardDirectory.listFiles(new FilenameFilter() {

                    public boolean accept(File dir, String name) {
                        return name.toLowerCase().endsWith(".sav");
                    }
                });
                for (File saveFile : saveFiles) {
                    File dest = new File(StringUtils.pathCombine(saveDirectory.getAbsolutePath(), saveFile.getName()));
                    if (dest.exists()) {
                        dest.delete();
                    }
                    saveFile.renameTo(dest);
                }
                File[] oldFiles = cardDirectory.listFiles(new FilenameFilter() {

                    public boolean accept(File dir, String name) {
                        String lowerName = name.toLowerCase();
                        return (lowerName.endsWith(".sav") || lowerName.endsWith(".nds")) && !lowerName.equals(HANDS_OFF_FILE);
                    }
                });
                for (File oldFile : oldFiles) {
                    oldFile.delete();
                }
                for (File game : games) {
                    String sourceFileName = game.getAbsolutePath();
                    String destFileName = StringUtils.pathCombine(cardDirectory.getAbsolutePath(), game.getName());
                    long gameSize = game.length();
                    this.fireTransferProgressed(new GameTransferStatusEvent(this, toBeTransferred, totalTransferred + gameSize / 2));
                    statusLabel.setText(String.format(GUIUtils.getString("statusLabel.text"), game.getName().substring(0, game.getName().lastIndexOf('.'))));
                    FileUtils.copyFile(sourceFileName, destFileName);
                    totalTransferred += gameSize;
                    this.fireTransferProgressed(new GameTransferStatusEvent(this, toBeTransferred, totalTransferred));
                    if (cancelTransferIssued) {
                        cancelTransferIssued = false;
                        this.fireTransferEnded(new GameTransferStatusEvent(this));
                        return;
                    }
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                }
                for (File game : games) {
                    String saveFileName = game.getName().substring(0, game.getName().lastIndexOf('.')) + ".SAV";
                    File saveFile = new File(StringUtils.pathCombine(saveDirectory.getAbsolutePath(), saveFileName));
                    if (saveFile.exists()) {
                        String sourceFileName = saveFile.getAbsolutePath();
                        String destFileName = StringUtils.pathCombine(cardDirectory.getAbsolutePath(), saveFile.getName());
                        FileUtils.copyFile(sourceFileName, destFileName);
                    }
                }
                this.fireTransferEnded(new GameTransferStatusEvent(this));
            } else {
                GameTransferStatusEvent statusEvent = null;
                if (directoryList == null || directoryList.size() == 0) {
                    statusEvent = new GameTransferStatusEvent(this, GameTransferStatusCode.SOURCE_DIRECTORIES_NOT_SELECTED);
                } else if (fileList == null || fileList.size() == 0) {
                    statusEvent = new GameTransferStatusEvent(this, GameTransferStatusCode.SOURCE_FILES_NOT_SELECTED);
                } else if (saveDirectory == null) {
                    statusEvent = new GameTransferStatusEvent(this, GameTransferStatusCode.SAVE_DIRECTORY_NOT_SELECTED);
                } else if (cardDirectory == null) {
                    statusEvent = new GameTransferStatusEvent(this, GameTransferStatusCode.CARD_DIRECTORY_NOT_SELECTED);
                }
                this.fireTransferFailed(statusEvent);
            }
        }
    }
}
