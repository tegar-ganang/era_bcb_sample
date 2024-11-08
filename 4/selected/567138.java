package gov.sns.apps.wirescan;

import java.awt.Dimension;
import java.awt.Component;
import java.awt.BorderLayout;
import java.text.SimpleDateFormat;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.JScrollPane;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JRadioButton;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.JTextField;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.event.TableModelListener;
import javax.swing.event.TableModelEvent;
import gov.sns.application.*;
import gov.sns.ca.*;
import gov.sns.tools.apputils.*;
import gov.sns.tools.pvlogger.*;
import gov.sns.tools.database.*;
import gov.sns.xal.smf.*;
import gov.sns.xal.smf.application.*;
import gov.sns.xal.smf.impl.*;

/**
 * This class creates the main window that is used. A WireWindow contains most
 * methods related to the drawing of the window.
 * 
 * @author S. Bunch
 * @version 1.0
 * @see AcceleratorWindow
 */
public class WireWindow extends AcceleratorWindow {

    private JTable wireTable;

    private JTable postTable;

    private WireDoc theDoc;

    private JTabbedPane theTabbedPane;

    private JButton startButton, selectAll, unselectAll, abortAll, saveWOScan;

    private JComboBox saveData;

    private JPanel mainPanel, controlPanel;

    private JScrollPane scrollPane;

    private JScrollPane postScrollPane;

    private EdgeLayout eLayout1, eLayout2;

    private EdgeConstraints eConstraints1, eConstraints2;

    private WirePanel[] wirePanels;

    private PVUpdaterDbl[] posUpdate;

    private PVUpdaterDblArry[] xDataUpdate, yDataUpdate, zDataUpdate;

    private ArrayList seriesList;

    protected ArrayList panelList;

    private ArrayList selectedRows;

    private SeriesRun seriesRun;

    private ThreadKiller killAllThreads;

    private ButtonGroup group;

    protected JTextField msgField;

    private int[] nSteps;

    /** The table model for the control table */
    protected WireTableModel tableModel;

    /** Progress bar updater */
    protected ProgProdder[] progProdder;

    /** Timestamp when a scan was started */
    protected Date startTime;

    /** Radio button for a series scan */
    protected JRadioButton seriesButton;

    /** Radio button for a parallel scan */
    protected JRadioButton parallelButton;

    protected PostTableModel postTableModel;

    private LoggerSession loggerSession;

    private MachineSnapshot snapshot;

    protected long pvLoggerId;

    protected boolean pvLogged = false;

    protected boolean firstScan = true;

    protected boolean tableChanged = false;

    protected ProfileMonitor[] thePM;

    /**
	 * Creates a new instance of WireWindow
	 * 
	 * @param wiredocument
	 *            The WireDoc that is used
	 */
    public WireWindow(WireDoc wiredocument) {
        super(wiredocument);
        theDoc = wiredocument;
        setSize(600, 800);
        makeContent();
        ConnectionDictionary dict = ConnectionDictionary.defaultDictionary();
        SqlStateStore store;
        if (dict != null) {
            store = new SqlStateStore(dict);
        } else {
            ConnectionPreferenceController.displayPathPreferenceSelector();
            dict = ConnectionDictionary.defaultDictionary();
            store = new SqlStateStore(dict);
        }
        ChannelGroup group = store.fetchGroup("default");
        loggerSession = new LoggerSession(group, store);
    }

    /**
	 * Creates a new table based on WireTableModel and adds it to the display.
	 * 
	 * @see WireTableModel
	 */
    protected void makeTable() {
        tableModel = new WireTableModel(theDoc);
        tableModel.addTableModelListener(new TableModelListener() {

            public void tableChanged(TableModelEvent evt) {
                tableChanged = true;
                firstScan = true;
            }
        });
        wireTable = new JTable(tableModel);
        wireTable.getColumn("Relative Wire Position").setCellRenderer(new ProgressRenderer());
        scrollPane = new JScrollPane(wireTable);
        scrollPane.setPreferredSize(new Dimension(550, 300));
        scrollPane.setVisible(true);
        eLayout2.setConstraints(scrollPane, 27, 0, 0, 0, EdgeLayout.TOP_LEFT_RIGHT, EdgeLayout.NO_GROWTH);
        controlPanel.add(scrollPane);
        theTabbedPane.add("Control", controlPanel);
        theTabbedPane.setVisible(true);
    }

    protected void doPostSigmaTable() {
        postTableModel = new PostTableModel(theDoc);
        postTable = new JTable(postTableModel);
        postScrollPane = new JScrollPane(postTable);
        postScrollPane.setPreferredSize(new Dimension(550, 225));
        postScrollPane.setVisible(true);
        eLayout2.setConstraints(postScrollPane, 350, 0, 0, 0, EdgeLayout.TOP_LEFT_RIGHT, EdgeLayout.NO_GROWTH);
        controlPanel.add(postScrollPane);
    }

    public void updatePostSigmaTable() {
        for (int i = 0; i < theDoc.selectedWires.size(); i++) {
            WireData wd = (WireData) theDoc.wireDataMap.get(thePM[i].getId());
            postTableModel.setValueAt(new Double(wd.xsigmaf), i, 1);
            postTableModel.setValueAt(new Double(wd.ysigmaf), i, 2);
            postTableModel.setValueAt(new Double(wd.zsigmaf), i, 3);
        }
    }

    /**
	 * The routine used to draw the window and it's contents. Each window
	 * contains a control panel with a control table diplayed, a button to begin
	 * a scan, radio buttons to choose a series or parallel scan, an abort all
	 * button, and an export button.
	 */
    protected void makeContent() {
        mainPanel = new JPanel();
        mainPanel.setVisible(true);
        this.getContentPane().add(mainPanel, BorderLayout.CENTER);
        eLayout1 = new EdgeLayout();
        eConstraints1 = new EdgeConstraints();
        eLayout2 = new EdgeLayout();
        eConstraints2 = new EdgeConstraints();
        mainPanel.setLayout(eLayout1);
        theTabbedPane = new JTabbedPane();
        eLayout1.setConstraints(theTabbedPane, 0, 0, 0, 0, EdgeLayout.ALL_SIDES, EdgeLayout.GROW_BOTH);
        theTabbedPane.setVisible(true);
        mainPanel.add(theTabbedPane);
        controlPanel = new JPanel();
        controlPanel.setLayout(eLayout2);
        startButton = new JButton("Start");
        startButton.setText("Start Scan");
        eLayout2.setConstraints(startButton, 0, 5, 50, 0, EdgeLayout.LEFT_BOTTOM, EdgeLayout.NO_GROWTH);
        controlPanel.add(startButton);
        startButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startButton.setEnabled(false);
                startAction();
                snapshot = loggerSession.takeSnapshot();
                Channel.flushIO();
            }
        });
        saveWOScan = new JButton("Save W/O Scan");
        eLayout2.setConstraints(saveWOScan, 0, 5, 15, 0, EdgeLayout.LEFT_BOTTOM, EdgeLayout.NO_GROWTH);
        controlPanel.add(saveWOScan);
        saveWOScan.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                snapshot = loggerSession.takeSnapshot();
                saveWOScanAction();
                saveToPVLogger();
                exportAction();
                Channel.flushIO();
            }
        });
        selectAll = new JButton();
        selectAll.setText("Select All");
        eLayout2.setConstraints(selectAll, 0, 200, 0, 0, EdgeLayout.TOP_LEFT, EdgeLayout.NO_GROWTH);
        controlPanel.add(selectAll);
        selectAll.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectAllAction();
            }
        });
        unselectAll = new JButton("Unselect All");
        eLayout2.setConstraints(unselectAll, 0, 300, 0, 0, EdgeLayout.TOP_LEFT, EdgeLayout.NO_GROWTH);
        controlPanel.add(unselectAll);
        unselectAll.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                unselectAllAction();
            }
        });
        String[] saveDataOptions = { "Export Data & PVLogger", "Export Data only" };
        saveData = new JComboBox(saveDataOptions);
        eLayout2.setConstraints(saveData, 0, 285, 25, 0, EdgeLayout.LEFT_BOTTOM, EdgeLayout.NO_GROWTH);
        controlPanel.add(saveData);
        saveData.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                if (((String) (((JComboBox) evt.getSource()).getSelectedItem())).equals("Export Data Only")) {
                    exportAction();
                } else if (((String) (((JComboBox) evt.getSource()).getSelectedItem())).equals("Export Data & PVLogger")) {
                    saveToPVLogger();
                    exportAction();
                }
            }
        });
        abortAll = new JButton("Abort All");
        eLayout2.setConstraints(abortAll, 0, 470, 25, 0, EdgeLayout.LEFT_BOTTOM, EdgeLayout.NO_GROWTH);
        controlPanel.add(abortAll);
        abortAll.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                abortAllAction();
            }
        });
        seriesButton = new JRadioButton("Series Scan");
        eLayout2.setConstraints(seriesButton, 0, 140, 55, 0, EdgeLayout.LEFT_BOTTOM, EdgeLayout.NO_GROWTH);
        controlPanel.add(seriesButton);
        seriesButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                theDoc.series = Boolean.TRUE;
            }
        });
        parallelButton = new JRadioButton("Parallel Scan");
        parallelButton.setSelected(true);
        theDoc.series = new Boolean(false);
        theDoc.series = Boolean.FALSE;
        eLayout2.setConstraints(parallelButton, 0, 140, 25, 0, EdgeLayout.LEFT_BOTTOM, EdgeLayout.NO_GROWTH);
        controlPanel.add(parallelButton);
        parallelButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                theDoc.series = Boolean.FALSE;
            }
        });
        group = new ButtonGroup();
        group.add(seriesButton);
        group.add(parallelButton);
        msgField = new JTextField();
        msgField.setEditable(false);
        getContentPane().add(msgField, "South");
        seriesList = new ArrayList();
        panelList = new ArrayList();
        selectedRows = new ArrayList();
    }

    /**
	 * Action performed when the start scan button is clicked.
	 * <p>
	 * This is a quite intensive routine that starts up everything and sets
	 * everything up to do a scan.
	 */
    private void startAction() {
        clearAll();
        if (tableChanged) {
            tableChanged = false;
        }
        theDoc.resetWireDataMap();
        selectWires();
        String[] theId = new String[theDoc.selectedWires.size()];
        if (firstScan) {
            thePM = new ProfileMonitor[theDoc.selectedWires.size()];
            nSteps = new int[theDoc.selectedWires.size()];
            posUpdate = new PVUpdaterDbl[theDoc.selectedWires.size()];
            progProdder = new ProgProdder[theDoc.selectedWires.size()];
            wirePanels = new WirePanel[theDoc.selectedWires.size()];
            for (int i = 0; i < theDoc.selectedWires.size(); i++) {
                System.out.println("selectedwires size " + theDoc.selectedWires.size());
                theId[i] = ((AcceleratorNode) theDoc.selectedWires.get(i)).getId();
                thePM[i] = (ProfileMonitor) theDoc.selectedWires.get(i);
                connectArrays(thePM[i]);
                wirePanels[i] = new WirePanel(thePM[i], theDoc, this);
                theDoc.wireDataMap.put(theId[i], new WireData(wirePanels[i]));
                panelList.add(wirePanels[i]);
            }
            doPostSigmaTable();
            firstScan = false;
        } else {
        }
        killAllThreads = new ThreadKiller(this);
        xDataUpdate = new PVUpdaterDblArry[theDoc.selectedWires.size()];
        yDataUpdate = new PVUpdaterDblArry[theDoc.selectedWires.size()];
        zDataUpdate = new PVUpdaterDblArry[theDoc.selectedWires.size()];
        for (int i = 0; i < theDoc.selectedWires.size(); i++) {
            try {
                nSteps[i] = thePM[i].getNSteps();
            } catch (ConnectionException ce) {
                System.err.println(ce.getMessage());
                ce.printStackTrace();
            } catch (GetException ge) {
                System.err.println(ge.getMessage());
                ge.printStackTrace();
            }
            theTabbedPane.add(theId[i], wirePanels[i]);
            try {
                tableModel.setMaxValueAt(thePM[i].getScanLength(), ((Integer) selectedRows.get(i)).intValue());
            } catch (ConnectionException ce) {
                System.err.println(ce.getMessage());
                ce.printStackTrace();
            } catch (GetException ge) {
                System.err.println(ge.getMessage());
                ge.printStackTrace();
            }
            xDataUpdate[i] = new PVUpdaterDblArry(wirePanels[i].pm.getChannel(ProfileMonitor.V_REAL_DATA_HANDLE), theId[i], theDoc, 0, wirePanels[i].pm.PosC, wirePanels[i]);
            yDataUpdate[i] = new PVUpdaterDblArry(wirePanels[i].pm.getChannel(ProfileMonitor.D_REAL_DATA_HANDLE), theId[i], theDoc, 1, wirePanels[i].pm.PosC, wirePanels[i]);
            zDataUpdate[i] = new PVUpdaterDblArry(wirePanels[i].pm.getChannel(ProfileMonitor.H_REAL_DATA_HANDLE), theId[i], theDoc, 2, wirePanels[i].pm.PosC, wirePanels[i]);
            posUpdate[i] = new PVUpdaterDbl(thePM[i].PosC);
            progProdder[i] = new ProgProdder((JProgressBar) tableModel.getValueAt(((Integer) selectedRows.get(i)).intValue(), 2), posUpdate[i], tableModel);
            if (theDoc.series == Boolean.TRUE) {
                seriesList.add(theDoc.selectedWires.get(i));
            }
            if (theDoc.series == Boolean.FALSE) {
                int timeout = 0;
                while (timeout <= 10) {
                    if (wirePanels[i].status.getText().startsWith("Ready")) {
                        break;
                    }
                    if (wirePanels[i].status.getText().startsWith("Scan")) {
                        break;
                    }
                    if (wirePanels[i].status.getText().startsWith("Found")) {
                        break;
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        System.out.println("Sleep interrupted for wirescanner " + theId[i] + " while checking if ready");
                        System.err.println(ie.getMessage());
                        ie.printStackTrace();
                    }
                    timeout++;
                }
                if (timeout <= 10) {
                    try {
                        msgField.setText("Scanning...");
                        thePM[i].doScan();
                    } catch (ConnectionException ce) {
                        System.err.println(ce.getMessage());
                        ce.printStackTrace();
                    } catch (PutException pe) {
                        System.err.println(pe.getMessage());
                        pe.printStackTrace();
                    }
                    startTime = new Date();
                } else {
                    System.out.println("Timeout expired for " + theId[i] + " while waiting for ready");
                    JOptionPane.showMessageDialog(this, "Timeout expired while waiting for ready", theId[i], JOptionPane.ERROR_MESSAGE);
                }
            }
            progProdder[i].start();
            wirePanels[i].chartProdder.start();
            theTabbedPane.setVisible(true);
        }
        if (theDoc.series == Boolean.TRUE) {
            seriesRun = new SeriesRun(seriesList, panelList);
            seriesRun.start();
            startTime = new Date();
        }
        killAllThreads.start();
        Channel.flushIO();
    }

    private void saveWOScanAction() {
        clearAll();
        if (tableChanged) {
            tableChanged = false;
        }
        theDoc.resetWireDataMap();
        startTime = new Date();
        selectWires();
        String[] theId = new String[theDoc.selectedWires.size()];
        wirePanels = new WirePanel[theDoc.selectedWires.size()];
        thePM = new ProfileMonitor[theDoc.selectedWires.size()];
        nSteps = new int[theDoc.selectedWires.size()];
        for (int i = 0; i < theDoc.selectedWires.size(); i++) {
            System.out.println("selectedwires size " + theDoc.selectedWires.size());
            theId[i] = ((AcceleratorNode) theDoc.selectedWires.get(i)).getId();
            thePM[i] = (ProfileMonitor) theDoc.selectedWires.get(i);
            connectArrays(thePM[i]);
            wirePanels[i] = new WirePanel(thePM[i], theDoc, this);
            theDoc.wireDataMap.put(theId[i], new WireData(wirePanels[i]));
            panelList.add(wirePanels[i]);
            wirePanels[i].getAmpls();
            wirePanels[i].getAreas();
            wirePanels[i].getMeans();
            wirePanels[i].getOffsets();
            wirePanels[i].getSigmas();
            wirePanels[i].getSlopes();
            wirePanels[i].doPostGraph();
        }
        doPostSigmaTable();
        Channel.flushIO();
    }

    private void connectArrays(ProfileMonitor dummyPM) {
        String theId = ((AcceleratorNode) dummyPM).getId();
        try {
            dummyPM.connectVData();
        } catch (ConnectionException ce) {
            System.out.println("Could not connect to " + theId + " XData");
            System.err.println(ce.getMessage());
            ce.printStackTrace();
        }
        try {
            dummyPM.connectDData();
        } catch (ConnectionException ce) {
            System.out.println("Could not connect to " + theId + " YData");
            System.err.println(ce.getMessage());
            ce.printStackTrace();
        }
        try {
            dummyPM.connectHData();
        } catch (ConnectionException ce) {
            System.out.println("Could not connect to " + theId + " ZData");
            System.err.println(ce.getMessage());
            ce.printStackTrace();
        }
        try {
            dummyPM.connectVDataArray();
        } catch (ConnectionException ce) {
            System.out.println("Could not connect to " + theId + " XDataArray");
            System.err.println(ce.getMessage());
            ce.printStackTrace();
        }
        try {
            dummyPM.connectDDataArray();
        } catch (ConnectionException ce) {
            System.out.println("Could not connect to " + theId + " YDataArray");
            System.err.println(ce.getMessage());
            ce.printStackTrace();
        }
        try {
            dummyPM.connectHDataArray();
        } catch (ConnectionException ce) {
            System.out.println("Could not connect to " + theId + " ZDataArray");
            System.err.println(ce.getMessage());
            ce.printStackTrace();
        }
        try {
            dummyPM.connectPosArray();
        } catch (ConnectionException ce) {
            System.out.println("Could not connect to " + theId + " PosArray");
            System.err.println(ce.getMessage());
            ce.printStackTrace();
        }
        try {
            dummyPM.connectStatArray();
        } catch (ConnectionException ce) {
            System.out.println("Could not connect to " + theId + " StatArray");
            System.err.println(ce.getMessage());
            ce.printStackTrace();
        }
        try {
            dummyPM.connectPos();
        } catch (ConnectionException ce) {
            System.out.println("Could not connect to " + theId + " Pos");
            System.err.println(ce.getMessage());
            ce.printStackTrace();
        }
        try {
            dummyPM.connectVFitArray();
        } catch (ConnectionException ce) {
            System.out.println("Could not connect to " + theId + " VFitArray");
            System.err.println(ce.getMessage());
            ce.printStackTrace();
        }
        try {
            dummyPM.connectDFitArray();
        } catch (ConnectionException ce) {
            System.out.println("Could not connect to " + theId + " DFitArray");
            System.err.println(ce.getMessage());
            ce.printStackTrace();
        }
        try {
            dummyPM.connectHFitArray();
        } catch (ConnectionException ce) {
            System.out.println("Could not connect to " + theId + " HFitArray");
            System.err.println(ce.getMessage());
            ce.printStackTrace();
        }
        Channel.flushIO();
    }

    private void clearAll() {
        if (tableChanged) {
            theDoc.selectedWires = new ArrayList();
            seriesList = new ArrayList();
            panelList = new ArrayList();
            selectedRows = new ArrayList();
            theDoc.wireDataMap = new HashMap();
            if (theTabbedPane.getTabCount() > 1) {
                controlPanel.remove(postScrollPane);
            }
            while (theTabbedPane.getTabCount() > 1) {
                theTabbedPane.remove(theTabbedPane.getTabCount() - 1);
            }
        }
        for (int i = 0; i < panelList.size(); i++) {
            ((WirePanel) panelList.get(i)).clear();
        }
    }

    public void clearPVUpdaters() {
        for (int i = 0; i < theDoc.selectedWires.size(); i++) {
            xDataUpdate[i].reset();
            yDataUpdate[i].reset();
            zDataUpdate[i].reset();
        }
    }

    /**
	 * Adds the selected wires to an ArrayList in the WireDoc class for use
	 * later.
	 */
    protected void selectWires() {
        theDoc.selectedWires.clear();
        selectedRows.clear();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (((Boolean) tableModel.getValueAt(i, 1)).booleanValue()) {
                theDoc.selectedWires.add(theDoc.wirescanners.get(i));
                selectedRows.add(new Integer(i));
            }
        }
    }

    private void selectAllAction() {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            tableModel.setValueAt(Boolean.TRUE, i, 1);
        }
    }

    private void unselectAllAction() {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            tableModel.setValueAt(Boolean.FALSE, i, 1);
        }
        clearAll();
    }

    private void abortAllAction() {
        for (int i = 0; i < panelList.size(); i++) {
            ((WirePanel) panelList.get(i)).abortAction();
        }
    }

    /**
	 * Action taken when the export button is clicked.
	 * <p>
	 * It basically sets up the timestamp filename and sends it to the
	 * saveToFile() routine in the WireDoc class.
	 */
    protected void exportAction() {
        if (startTime != null) {
            SimpleDateFormat df = new SimpleDateFormat("yyyy.MM.dd");
            String datePart = df.format(startTime);
            datePart = datePart.replaceAll(" ", "");
            datePart = datePart.replace('/', '.');
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            String timePart = sdf.format(startTime);
            timePart = timePart.replaceAll(" ", "");
            timePart = timePart.replace(':', '.');
            String fileString = datePart + "." + timePart + ".txt";
            File filePath = Application.getApp().getDefaultDocumentFolder();
            File file = new File(filePath, fileString);
            try {
                theDoc.saveToFile(file);
            } catch (Exception e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error Saving File", "Save Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Nothing to export!\nPlease run a scan first!", "No Data", JOptionPane.ERROR_MESSAGE);
        }
    }

    protected void saveToPVLogger() {
        String comments = startTime.toString();
        comments = comments + "\n" + "For Wire Scanner Application with WSs:\n";
        for (int i = 0; i < theDoc.selectedWires.size(); i++) {
            comments = comments + " " + ((AcceleratorNode) theDoc.selectedWires.get(i)).getId();
        }
        snapshot.setComment(comments);
        loggerSession.publishSnapshot(snapshot);
        pvLoggerId = snapshot.getId();
        pvLogged = true;
    }

    public void enableStartButton() {
        startButton.setEnabled(true);
    }

    public void stopProgProdders() {
        for (int i = 0; i < theDoc.selectedWires.size(); i++) {
            progProdder[i].stopT();
        }
    }
}
