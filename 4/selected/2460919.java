package gov.sns.apps.xio;

import gov.sns.xal.smf.AcceleratorNode;
import gov.sns.xal.smf.NoSuchChannelException;
import gov.sns.ca.*;
import gov.sns.jclass.*;
import javax.swing.*;
import javax.swing.table.TableColumn;
import java.util.List;
import java.util.Iterator;
import java.util.Vector;
import java.util.HashMap;

/**
 * Generate data tables
 * 
 * @version 0.1 03 Dec 2002
 * @author C.M. Chu
 */
public class IODiagTableFactory implements ConnectionListener {

    /** the document this object belongs to */
    private MyDocument theDoc;

    /** Flag indicating whether this plot has been created yet */
    private boolean tableShowing;

    /** List of the monitors */
    final Vector mons = new Vector();

    /**
	 * map relating channels to all the Table cells that monitor these. This is
	 * incase there is > 1 table per device. It adds complexity, I (jdg) think
	 * we should remove it and only allow 1 table/(device type) per document.
	 * Open a new document if you want a different view of the device.
	 */
    private HashMap monitorQueues = new HashMap();

    private Channel[][] theChannels;

    private int pvs = 0;

    private int row = 0;

    /** the constructor */
    public IODiagTableFactory(MyDocument md) {
        theDoc = md;
        tableShowing = false;
    }

    /** get the list of table cells monitoring the prescibed channel */
    private Vector getChannelVec(Channel p_chan) {
        if (!monitorQueues.containsKey(p_chan.channelName())) monitorQueues.put(p_chan.channelName(), new Vector());
        return ((Vector) monitorQueues.get(p_chan.channelName()));
    }

    /**
	 * This method actually creates the table, fires up the monitors, sets the
	 * associated buttons etc.
	 */
    public JScrollPane createTable(List theNodes, String[] inputPVs) {
        AcceleratorNode tempNode = (AcceleratorNode) (theNodes.get(0));
        final String typeID = new String(tempNode.getType());
        JInternalFrame theFrame = new JInternalFrame(typeID + "s", true, true, true);
        int nCols = 2;
        int nRows = theNodes.size() + 1;
        InputPVTableCell pvCell;
        XioTableCellEditor cellEditor = new XioTableCellEditor(nRows);
        if (inputPVs != null) nCols += inputPVs.length;
        String[] colNames = new String[nCols];
        colNames[0] = typeID + "s";
        colNames[1] = "s";
        int cnt = 2;
        if (inputPVs != null) for (int i = 0; i < inputPVs.length; i++, cnt++) {
            colNames[cnt] = inputPVs[i];
        }
        IODiagTableModel theTableModel = new IODiagTableModel(colNames, nRows);
        theDoc.tableModelMap.put(typeID, theTableModel);
        JTable theTable = new JTable(theTableModel);
        theTable.setRowSelectionAllowed(false);
        theTable.setColumnSelectionAllowed(false);
        theTable.setCellSelectionEnabled(true);
        theTable.setCellEditor(cellEditor);
        for (int i = 0; i < theTable.getColumnCount(); i++) {
            TableColumn tc = theTable.getColumnModel().getColumn(i);
            XioTableCellEditor colCellEditor = new XioTableCellEditor(nRows);
            tc.setCellRenderer(colCellEditor);
        }
        Iterator it = theNodes.iterator();
        AcceleratorNode theNode;
        double[] xGrid = new double[theNodes.size()];
        theChannels = new Channel[theNodes.size()][inputPVs.length];
        while (it.hasNext()) {
            theNode = ((AcceleratorNode) (it.next()));
            xGrid[row] = theDoc.getSelectedSequence().getPosition(theNode);
            theTableModel.addRowName(theNode.getId(), row);
            java.text.NumberFormat nf = java.text.NumberFormat.getNumberInstance();
            nf.setMaximumFractionDigits(3);
            theTableModel.setValueAt(nf.format(xGrid[row]), row, 1);
            int numINs = 0;
            if (inputPVs != null) {
                numINs += inputPVs.length;
                pvs = inputPVs.length;
                for (int ci = 0; ci < inputPVs.length; ci++) {
                    try {
                        theChannels[row][ci] = theNode.getChannel(inputPVs[ci]);
                        if (theChannels[row] != null) {
                            pvCell = new InputPVTableCell(theChannels[row][ci], row, ci + 2);
                            theTableModel.addPVCell(pvCell, row, ci + 2);
                            getChannelVec(theChannels[row][ci]).add(pvCell);
                        }
                    } catch (NoSuchChannelException e) {
                    }
                }
            }
            row++;
        }
        IODiagPlotter thePlotter = new IODiagPlotter(theNodes, theTable, theDoc, typeID);
        theDoc.tableXYPlotMap.put(typeID, thePlotter);
        JButton newBtn = new JButton("X-Y Plot");
        newBtn.addActionListener(thePlotter);
        theTableModel.addJButton(0, newBtn);
        JButton[] plotButtons = new JButton[inputPVs.length];
        final WaterfallPlot[] wfps = new WaterfallPlot[inputPVs.length];
        for (int ci = 0; ci < inputPVs.length; ci++) {
            String pName = ((AcceleratorNode) theNodes.get(0)).getType() + colNames[ci + 1];
            final WaterfallPlot theWaterfallPlot = new WaterfallPlot(pName, xGrid, theTable, ci + 2, theDoc.getMainWindow());
            wfps[ci] = theWaterfallPlot;
            plotButtons[ci] = new JButton("Water Fall");
            theTableModel.h2OPlots.put(colNames[ci + 1], wfps[ci]);
            theTableModel.addJButton(ci + 1, plotButtons[ci]);
            plotButtons[ci].addActionListener(wfps[ci]);
        }
        theTable.getColumnModel().getColumn(0).setPreferredWidth(125);
        JScrollPane theScrollPane = new JScrollPane(theTable);
        theTable.setPreferredScrollableViewportSize(theTable.getPreferredSize());
        final TableProdder prodder = new TableProdder(theTableModel);
        prodder.start();
        return theScrollPane;
    }

    public void connectChannels() {
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < pvs; j++) {
                if (theChannels[i][j] != null) {
                    theChannels[i][j].addConnectionListener(this);
                    theChannels[i][j].connectAndWait();
                    if (theChannels[i][j].isConnected()) connectMons(theChannels[i][j]);
                }
            }
        }
    }

    /** ConnectionListener interface */
    public void connectionMade(Channel aChannel) {
        connectMons(aChannel);
    }

    /** ConnectionListener interface */
    public void connectionDropped(Channel aChannel) {
    }

    /** internal method to connect the monitors */
    private void connectMons(Channel p_chan) {
        Vector chanVec;
        try {
            chanVec = getChannelVec(p_chan);
            for (int i = 0; i < chanVec.size(); i++) {
                mons.add(p_chan.addMonitorValue((InputPVTableCell) chanVec.elementAt(i), Monitor.VALUE));
            }
            chanVec.removeAllElements();
        } catch (ConnectionException e) {
            System.out.println("Connection Exception");
        } catch (MonitorException e) {
            System.out.println("Monitor Exception");
        }
    }
}
