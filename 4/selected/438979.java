package admin.astor.statistics;

import admin.astor.AstorUtil;
import admin.astor.tools.PopupText;
import fr.esrf.Tango.DevFailed;
import fr.esrf.tangoatk.widget.util.ATKGraphicsUtils;
import fr.esrf.tangoatk.widget.util.ErrorPane;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Vector;

public class StatisticsPanel extends JFrame {

    private static JFileChooser chooser = null;

    private static final StatisticsFileFilter fileFilter = new StatisticsFileFilter("xml", "Statistics Files");

    private JFrame parent = null;

    private GlobalStatistics globalStatistics;

    private JScrollPane tableScrollPane = null;

    private GlobalStatisticsTable statisticsTable;

    public StatisticsPanel(JFrame parent, String fileName) throws DevFailed {
        this.parent = parent;
        initComponents();
        customizeMenus();
        globalStatistics = new GlobalStatistics(fileName);
        displayGlobalStatistics();
        pack();
        ATKGraphicsUtils.centerFrameOnScreen(this);
    }

    public StatisticsPanel() {
        this(null);
    }

    public StatisticsPanel(JFrame parent) {
        this.parent = parent;
        AstorUtil.startSplash("Statistics ");
        AstorUtil.increaseSplashProgress(5, "Initializing....");
        initComponents();
        customizeMenus();
        titleLabel.setText("No Statistics Read");
        pack();
        ATKGraphicsUtils.centerFrameOnScreen(this);
        AstorUtil.stopSplash();
    }

    public void readAndDisplayStatistics(Vector<String> hostList) {
        titleLabel.setText("Reading and Computing Statistics");
        new ReadThread(hostList).start();
    }

    private class ReadThread extends Thread {

        private Vector<String> hostList;

        private ReadThread(Vector<String> hostList) {
            this.hostList = hostList;
        }

        public void run() {
            AstorUtil.startSplash("Statistics ");
            AstorUtil.increaseSplashProgress(5, "Reading....");
            setCursor(new Cursor(Cursor.WAIT_CURSOR));
            Vector<StarterStat> starterStatistics = Utils.readHostStatistics(hostList);
            globalStatistics = new GlobalStatistics(starterStatistics);
            displayGlobalStatistics();
            setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            AstorUtil.stopSplash();
        }
    }

    private void displayGlobalStatistics() {
        Vector<ServerStat> failedServers = getServerFailedList(globalStatistics.getStarterStatistics());
        statisticsTable = new GlobalStatisticsTable(this);
        statisticsTable.setStatistics(failedServers);
        globalStatTextArea.setText(globalStatistics.toString());
        if (tableScrollPane != null) getContentPane().remove(tableScrollPane);
        tableScrollPane = new JScrollPane();
        tableScrollPane.setPreferredSize(new Dimension(statisticsTable.getDefaultWidth(), statisticsTable.getDefaultHeight()));
        tableScrollPane.setViewportView(statisticsTable);
        getContentPane().add(tableScrollPane, BorderLayout.CENTER);
        String title = "During  " + Utils.formatDuration(globalStatistics.getDuration()) + "      " + failedServers.size();
        if (failedServers.size() <= 1) title += "  server has failed"; else title += "  servers have failed";
        titleLabel.setText(title);
        pack();
    }

    private void customizeMenus() {
        fileMenu.setMnemonic('F');
        readItem.setMnemonic('R');
        readItem.setAccelerator(KeyStroke.getKeyStroke('R', Event.CTRL_MASK));
        openItem.setMnemonic('O');
        openItem.setAccelerator(KeyStroke.getKeyStroke('O', Event.CTRL_MASK));
        saveItem.setMnemonic('S');
        saveItem.setAccelerator(KeyStroke.getKeyStroke('S', Event.CTRL_MASK));
        String superTango = System.getenv("SUPER_TANGO");
        if (superTango != null && superTango.toLowerCase().equals("true")) {
            resetItem.setMnemonic('R');
            resetItem.setAccelerator(KeyStroke.getKeyStroke('R', Event.ALT_MASK));
        } else resetItem.setVisible(false);
        exitItem.setMnemonic('E');
        exitItem.setAccelerator(KeyStroke.getKeyStroke('Q', Event.CTRL_MASK));
        editMenu.setMnemonic('E');
        filterItem.setMnemonic('F');
        filterItem.setAccelerator(KeyStroke.getKeyStroke('F', Event.CTRL_MASK));
        errorItem.setMnemonic('E');
        errorItem.setAccelerator(KeyStroke.getKeyStroke('E', Event.CTRL_MASK));
        bottomPanel.setVisible(false);
    }

    private void initComponents() {
        javax.swing.JPanel topPanel = new javax.swing.JPanel();
        titleLabel = new javax.swing.JLabel();
        javax.swing.JScrollPane globalStatScrollPane = new javax.swing.JScrollPane();
        globalStatTextArea = new javax.swing.JTextArea();
        bottomPanel = new javax.swing.JPanel();
        javax.swing.JLabel filterLabel = new javax.swing.JLabel();
        filterText = new javax.swing.JTextField();
        javax.swing.JMenuBar jMenuBar1 = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        readItem = new javax.swing.JMenuItem();
        openItem = new javax.swing.JMenuItem();
        saveItem = new javax.swing.JMenuItem();
        resetItem = new javax.swing.JMenuItem();
        exitItem = new javax.swing.JMenuItem();
        editMenu = new javax.swing.JMenu();
        filterItem = new javax.swing.JMenuItem();
        errorItem = new javax.swing.JMenuItem();
        addWindowListener(new java.awt.event.WindowAdapter() {

            public void windowClosing(java.awt.event.WindowEvent evt) {
                exitForm(evt);
            }
        });
        topPanel.setLayout(new java.awt.BorderLayout());
        titleLabel.setFont(new java.awt.Font("Times New Roman", 1, 14));
        titleLabel.setText("Title");
        topPanel.add(titleLabel, java.awt.BorderLayout.PAGE_END);
        globalStatScrollPane.setPreferredSize(new java.awt.Dimension(250, 110));
        globalStatTextArea.setColumns(20);
        globalStatTextArea.setEditable(false);
        globalStatTextArea.setFont(new java.awt.Font("Monospaced", 1, 12));
        globalStatTextArea.setRows(5);
        globalStatScrollPane.setViewportView(globalStatTextArea);
        topPanel.add(globalStatScrollPane, java.awt.BorderLayout.CENTER);
        getContentPane().add(topPanel, java.awt.BorderLayout.NORTH);
        filterLabel.setText("Filter :  ");
        bottomPanel.add(filterLabel);
        filterText.setColumns(20);
        filterText.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyPressed(java.awt.event.KeyEvent evt) {
                filterTextKeyPressed(evt);
            }
        });
        bottomPanel.add(filterText);
        getContentPane().add(bottomPanel, java.awt.BorderLayout.SOUTH);
        fileMenu.setText("File");
        readItem.setText("Read Whole Statistics");
        readItem.setActionCommand("read");
        readItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                readItemActionPerformed(evt);
            }
        });
        fileMenu.add(readItem);
        openItem.setText("Open");
        openItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openItemActionPerformed(evt);
            }
        });
        fileMenu.add(openItem);
        saveItem.setText("Save");
        saveItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveItemActionPerformed(evt);
            }
        });
        fileMenu.add(saveItem);
        resetItem.setText("Reset Statistics");
        resetItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetItemActionPerformed(evt);
            }
        });
        fileMenu.add(resetItem);
        exitItem.setText("Exit");
        exitItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitItemActionPerformed(evt);
            }
        });
        fileMenu.add(exitItem);
        jMenuBar1.add(fileMenu);
        editMenu.setText("Edit");
        filterItem.setText("Find Server");
        filterItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                filterItemActionPerformed(evt);
            }
        });
        editMenu.add(filterItem);
        errorItem.setText("Show Errors");
        errorItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                errorItemActionPerformed(evt);
            }
        });
        editMenu.add(errorItem);
        jMenuBar1.add(editMenu);
        setJMenuBar(jMenuBar1);
        pack();
    }

    @SuppressWarnings({ "UnusedDeclaration" })
    private void openItemActionPerformed(java.awt.event.ActionEvent evt) {
        initChooser("Open");
        int retval = chooser.showOpenDialog(this);
        if (retval == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (file != null) {
                if (!file.isDirectory()) {
                    try {
                        String filename = file.getAbsolutePath();
                        globalStatistics = new GlobalStatistics(filename);
                        displayGlobalStatistics();
                    } catch (DevFailed e) {
                        ErrorPane.showErrorMessage(this, null, e);
                    }
                }
            }
        }
    }

    private void initChooser(String str) {
        if (chooser == null) {
            String path = System.getProperty("FILES");
            if (path == null) path = "";
            chooser = new JFileChooser(new File(path).getAbsolutePath());
            chooser.setFileFilter(fileFilter);
        }
        chooser.setApproveButtonText(str);
    }

    @SuppressWarnings({ "UnusedDeclaration" })
    private void saveItemActionPerformed(java.awt.event.ActionEvent evt) {
        if (globalStatistics == null) return;
        initChooser("Save");
        int retval = chooser.showOpenDialog(this);
        if (retval == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (file != null) {
                if (!file.isDirectory()) {
                    String fileName = file.getAbsolutePath();
                    if (!fileName.endsWith(".xml")) fileName += ".xml";
                    if (new File(fileName).exists()) {
                        if (JOptionPane.showConfirmDialog(this, fileName + "   File already exists\n\n     Overwrite it ?", "Confirm Dialog", JOptionPane.YES_NO_OPTION) != JOptionPane.OK_OPTION) {
                            return;
                        }
                    }
                    try {
                        globalStatistics.saveStatistics(fileName);
                    } catch (DevFailed e) {
                        ErrorPane.showErrorMessage(this, null, e);
                    }
                }
            }
        }
    }

    @SuppressWarnings({ "UnusedDeclaration" })
    private void exitItemActionPerformed(java.awt.event.ActionEvent evt) {
        if (parent == null) System.exit(0); else setVisible(false);
    }

    @SuppressWarnings({ "UnusedDeclaration" })
    private void exitForm(java.awt.event.WindowEvent evt) {
        if (parent == null) System.exit(0); else setVisible(false);
    }

    @SuppressWarnings({ "UnusedDeclaration" })
    private void readItemActionPerformed(java.awt.event.ActionEvent evt) {
        readAndDisplayStatistics(null);
    }

    @SuppressWarnings({ "UnusedDeclaration" })
    private void errorItemActionPerformed(java.awt.event.ActionEvent evt) {
        StringBuffer sb = new StringBuffer();
        if (globalStatistics != null) {
            for (StarterStat starterStat : globalStatistics.getStarterStatistics()) {
                if (!starterStat.readOK) {
                    sb.append(starterStat.name).append(":\t").append(starterStat.error).append("\n");
                }
            }
        }
        if (sb.length() == 0) sb.append("No Eror.");
        new PopupText(this, true).show(sb.toString());
    }

    private void filterTextKeyPressed(java.awt.event.KeyEvent evt) {
        if (evt.getKeyChar() == 27) {
            resetFilter();
        } else {
            new DelayedDisplay(evt).start();
        }
    }

    @SuppressWarnings({ "UnusedDeclaration" })
    private void filterItemActionPerformed(java.awt.event.ActionEvent evt) {
        if (statisticsTable != null) {
            bottomPanel.setVisible(true);
            pack();
            new DelayedDisplay().start();
        }
    }

    @SuppressWarnings({ "UnusedDeclaration" })
    private void resetItemActionPerformed(java.awt.event.ActionEvent evt) {
        new ResetStatistics(this);
    }

    private void resetFilter() {
        if (statisticsTable != null) {
            statisticsTable.resetFilter();
            filterText.setText("");
            bottomPanel.setVisible(false);
            pack();
        }
    }

    private Vector<ServerStat> getServerFailedList(Vector<StarterStat> starterStats) {
        Vector<ServerStat> serverStats = new Vector<ServerStat>();
        for (StarterStat starterStat : starterStats) {
            for (ServerStat server : starterStat) {
                if (server.nbFailures > 0) {
                    serverStats.add(server);
                }
            }
        }
        return serverStats;
    }

    public static void main(String args[]) {
        try {
            if (args.length > 0) new StatisticsPanel(null, args[0]).setVisible(true); else new StatisticsPanel().setVisible(true);
        } catch (DevFailed e) {
            ErrorPane.showErrorMessage(new JFrame(), null, e);
        }
    }

    private javax.swing.JPanel bottomPanel;

    private javax.swing.JMenu editMenu;

    private javax.swing.JMenuItem errorItem;

    private javax.swing.JMenuItem exitItem;

    private javax.swing.JMenu fileMenu;

    private javax.swing.JMenuItem filterItem;

    private javax.swing.JTextField filterText;

    private javax.swing.JTextArea globalStatTextArea;

    private javax.swing.JMenuItem openItem;

    private javax.swing.JMenuItem readItem;

    private javax.swing.JMenuItem resetItem;

    private javax.swing.JMenuItem saveItem;

    private javax.swing.JLabel titleLabel;

    private String filter = "";

    private class DelayedDisplay extends Thread {

        private KeyEvent evt = null;

        private DelayedDisplay() {
        }

        private DelayedDisplay(KeyEvent evt) {
            this.evt = evt;
        }

        public void run() {
            try {
                sleep(10);
            } catch (InterruptedException e) {
            }
            if (evt == null) {
                filterText.requestFocus();
            } else {
                char c = evt.getKeyChar();
                if ((c & 0x8000) == 0) {
                    String s = filterText.getText();
                    if (!filter.equals(s)) {
                        if (s.length() > 0) {
                            statisticsTable.setFilter(s);
                        } else statisticsTable.resetFilter();
                    }
                    filter = s;
                }
            }
        }
    }
}
