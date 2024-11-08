package gov.sns.apps.score;

import gov.sns.ca.*;
import gov.sns.tools.data.*;
import gov.sns.tools.logbook.ElogUtility;
import java.util.*;
import javax.swing.*;

public class Restorer implements Runnable, PutListener {

    /** the document this resorer belongs to */
    private ScoreDocument theDoc;

    /** the thread this works in */
    private Thread thread;

    /** List of channels that have been sent out for restoring */
    private ArrayList sentChannels;

    /** List of channels that have been restored successfully */
    private ArrayList receivedChannels;

    /** the time to check connections before givving up */
    private double timeOut;

    public static final double dwellTime = 0.5;

    /** the number of loops to make to see if the connections are OK
     * calculated from the input timeOut period 
     */
    private int nTrys;

    /** A flag indicating some channels are not connected */
    private boolean allOK = false;

    /** Flag indicating something did not go perfectly */
    private boolean glitchOccurred = false;

    /** holder for PVs that we cannot connect to */
    private ArrayList badPVs = new ArrayList();

    /** holder for bad value PVs */
    private ArrayList badValuePVs = new ArrayList();

    /** holder for selected records to restore */
    private Collection selectedRecords;

    /** The number of selected records to restore */
    private int nRecords;

    /** time the restore action started */
    private Date date;

    private DataTable dataTable;

    /** the constructor
     * @param aDoc - the Document this reatorer acts on
     * @param to - the time out period to wait before giving up on restores (sec)
     */
    public Restorer(ScoreDocument aDoc, double to) {
        theDoc = aDoc;
        timeOut = to;
        dataTable = theDoc.theSnapshot.getData().getDataTable();
        selectedRecords = getSelectedRecords();
        nRecords = selectedRecords.size();
        if (nRecords == 0) {
            theDoc.dumpErr("You first must select some systems + types");
        } else {
            thread = new Thread(this, "ConnectChecker");
            sentChannels = new ArrayList();
            receivedChannels = new ArrayList();
            nTrys = (int) (timeOut / dwellTime);
            thread.start();
        }
    }

    /** get a collection of PVData records that have the selected rows in the selected table tab, that also have setpoints in them */
    private Collection getSelectedRecords() {
        String name;
        JTable table;
        String spName, systemName;
        Double spVal;
        Map bindings = new HashMap();
        ScoreDouble spValue;
        JTabbedPane ttp = theDoc.myWindow().theTabbedPane;
        ScoreTableModel tableModel;
        ArrayList records = new ArrayList();
        Collection temp;
        int tab = ttp.getSelectedIndex();
        systemName = ttp.getTitleAt(tab);
        table = (JTable) theDoc.theTables.get(systemName);
        if (table != null) {
            tableModel = (ScoreTableModel) table.getModel();
            for (int j = 0; j < table.getRowCount(); j++) {
                if (table.isRowSelected(j)) {
                    ScoreRecord rec = (ScoreRecord) tableModel.records.get(j);
                    if (rec != null) {
                        spName = rec.stringValueForKey(PVData.spNameKey);
                        spVal = (Double) rec.valueForKey(PVData.spSavedValKey);
                        if (spName != null && spVal != null) {
                            records.add(rec);
                        }
                    }
                }
            }
        }
        return records;
    }

    public void run() {
        System.err.println("Starting a restore at " + new Date());
        String warningMsg = "Do you Really want to restore the " + (new Integer(nRecords)).toString() + " selected records?";
        int cont = JOptionPane.showConfirmDialog(theDoc.myWindow(), warningMsg, "Just Checking", JOptionPane.YES_NO_OPTION);
        if (cont == JOptionPane.NO_OPTION) return;
        theDoc.myWindow().errorText.setText("Attempting a restore");
        date = new Date();
        receivedChannels.clear();
        fireRestores();
        int i = 0;
        while (i < nTrys) {
            if (allOK) {
                break;
            } else {
                try {
                    Thread.sleep((int) (1000 * dwellTime));
                    checkRestoreStatus();
                } catch (InterruptedException e) {
                }
                i++;
                boolean blink = theDoc.myWindow().errorText.isVisible();
                theDoc.myWindow().errorText.setVisible(!blink);
            }
        }
        reportStatus();
        postElogResoreEntry();
    }

    /** loop through the setpoint PVs and send out
     * values associated with setpoint PVs  in the table
     */
    private void fireRestores() {
        ScoreRecord record;
        Double spVal;
        ChannelWrapper channel;
        String name;
        Boolean useRBVal;
        badPVs.clear();
        badValuePVs.clear();
        glitchOccurred = false;
        Iterator itr = selectedRecords.iterator();
        while (itr.hasNext()) {
            record = (ScoreRecord) itr.next();
            channel = record.getSPChannel();
            if (channel != null && channel.isConnected()) {
                name = channel.getId();
                spVal = (Double) record.valueForKey(PVData.spSavedValKey);
                useRBVal = (Boolean) record.valueForKey(PVData.restoreRBValKey);
                if (useRBVal != null) if (useRBVal.booleanValue()) spVal = (Double) record.valueForKey(PVData.rbSavedValKey);
                if (spVal.isNaN()) {
                    glitchOccurred = true;
                    badValuePVs.add(name);
                }
                if (spVal != null && !spVal.isNaN()) {
                    try {
                        if (channel.getChannel().writeAccess() == false) {
                            System.err.println("No WRITE access to " + name);
                            glitchOccurred = true;
                        } else {
                            try {
                                synchronized (sentChannels) {
                                    if (sentChannels.contains(name)) {
                                        glitchOccurred = true;
                                        System.err.println("Tried to set " + name + " more than once! only will do the first attempt");
                                    } else {
                                        channel.getChannel().putValCallback(spVal.doubleValue(), this);
                                        sentChannels.add(name);
                                    }
                                }
                            } catch (Exception evt) {
                                evt.printStackTrace();
                            }
                        }
                    } catch (ConnectionException eyah) {
                        System.err.println("PV " + name + " is not connected, will ignore this restore request");
                        glitchOccurred = true;
                    }
                }
            } else if (channel != null) badPVs.add(channel.getId());
        }
        Channel.flushIO();
        if (badPVs.size() > 0) {
            System.err.println(" The following setpoint PVs cannot be connected to and will not be restored:");
            glitchOccurred = true;
            Iterator itrBad = badPVs.iterator();
            while (itrBad.hasNext()) {
                name = (String) itrBad.next();
                System.err.println(name);
            }
        }
    }

    protected void postElogResoreEntry() {
        String msg = "Score restore done at " + date.toString() + "\n";
        msg += "The following PVs were set:\n";
        Iterator itr = receivedChannels.iterator();
        while (itr.hasNext()) msg += (String) itr.next() + "\n";
        if (badPVs.size() > 0) {
            msg += "The following were not connected (nor attempted):\n";
            itr = badPVs.iterator();
            while (itr.hasNext()) msg += (String) itr.next() + "\n";
        }
        if (msg.length() > 3999) msg = msg.substring(0, 4000);
        try {
            ElogUtility.defaultUtility().postEntry("Operations", "Score restore done", msg);
            System.out.println(msg);
        } catch (Exception exc) {
            System.err.println("Failed elog post on save " + exc.getMessage());
        }
    }

    /** callback routine for channel puts.
     * keep track of how many requested puts made it */
    public void putCompleted(Channel chan) {
        synchronized (receivedChannels) {
            receivedChannels.add(chan.getId());
        }
    }

    /** report status of the put attempts */
    private void reportStatus() {
        String key;
        theDoc.myWindow().errorText.setVisible(true);
        System.err.println("Restore status at " + new Date());
        if (allOK && !glitchOccurred) {
            System.err.println("All PV restore trys were OK");
        } else {
            theDoc.dumpErr("Dang! some restore attempts failed. See console for details.");
            if (sentChannels.size() > 0) {
                System.err.println("The following PV restores did not succeed");
                Iterator itr = sentChannels.iterator();
                while (itr.hasNext()) {
                    key = (String) itr.next();
                    System.err.println(key);
                }
            }
        }
    }

    /** check on the status of the PVs that were sent for restoration */
    private void checkRestoreStatus() {
        int i;
        synchronized (receivedChannels) {
            Iterator itr = receivedChannels.iterator();
            while (itr.hasNext()) {
                i = sentChannels.indexOf(itr.next());
                if (i != -1) sentChannels.remove(i);
            }
        }
        if (sentChannels.size() == 0) allOK = true;
    }
}
