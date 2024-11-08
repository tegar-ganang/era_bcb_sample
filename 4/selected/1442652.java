package gov.sns.apps.pvbrowser;

import gov.sns.application.Application;
import gov.sns.ca.Channel;
import gov.sns.ca.ChannelFactory;
import gov.sns.ca.ChannelRecord;
import gov.sns.ca.ConnectionException;
import gov.sns.ca.ConnectionListener;
import gov.sns.ca.IEventSinkValue;
import gov.sns.ca.Monitor;
import gov.sns.ca.MonitorException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.table.AbstractTableModel;
import javax.swing.SwingUtilities;

/**
 * Provides a <CODE>TableModel</CODE> for displaying a 
 * <CODE>PVBrowserPVDocument</CODE>.
 * 
 * @author Chris Fowlkes
 */
public class PVBrowserPVDocumentTableModel extends AbstractTableModel implements PropertyChangeListener {

    /**
   * Holds the <CODE>PVBrowserPVDocument</CODE> being displayed.  
   */
    private PVBrowserPVDocument document;

    /**
   * Flag used to determine if the model is to use channel access to obtain 
   * current values.
   */
    private boolean probe = false;

    private HashMap probeValues = new HashMap();

    /**
   * Holds the channels being probed.
   */
    private ArrayList channels = new ArrayList();

    /**
   * Holds the instances of <CODE>Monitor</CODE> add to the channels.
   */
    private ArrayList monitors = new ArrayList();

    /**
   * Holds the <CODE>ConnectionListener</CODE> that listenes to the PVs being 
   * probed.
   */
    private ConnectionListener connectionListener;

    /**
   * Holds the <CODE>IEventSinkValue</CODE> that listenes to the PVs being 
   * probed.
   */
    private IEventSinkValue probeListener;

    /**
   * Creates a new <CODE>PVBrowserPVDocumentTableModel</CODE>.
   */
    public PVBrowserPVDocumentTableModel() {
        probeListener = new IEventSinkValue() {

            public void eventValue(final ChannelRecord record, final Channel chan) {
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        String channelName = chan.channelName();
                        BrowserPV pv = getDocument().getPV();
                        String pvID = pv.getID();
                        if (channelName.equals(pvID)) {
                            probeValues.put("VAL", record.stringValue());
                            fireTableCellUpdated(0, 2);
                        } else if (!channelName.startsWith(pvID + ".")) System.out.println("Recieved event for '" + channelName + "' when monitoring '" + pvID + "'."); else {
                            String fieldName = convertChannelName(channelName);
                            probeValues.put(fieldName, record.stringValue());
                            fireTableCellUpdated(pv.getIndexOfField(fieldName) + 1, 2);
                        }
                    }
                });
            }
        };
        connectionListener = new ConnectionListener() {

            public void connectionMade(Channel channel) {
                try {
                    synchronized (monitors) {
                        monitors.add(channel.addMonitorValue(probeListener, Monitor.VALUE));
                    }
                } catch (ConnectionException e) {
                    Application.displayError(e);
                    e.printStackTrace();
                } catch (MonitorException e) {
                    Application.displayError(e);
                    e.printStackTrace();
                }
            }

            public void connectionDropped(Channel channel) {
            }
        };
    }

    /**
   * Converts the channel access channel name to the field name.
   * 
   * @param channelName The channel access channel name.
   * @return The field name.
   */
    private String convertChannelName(String channelName) {
        return channelName.substring(getDocument().getPV().getID().length() + 1);
    }

    /**
   * Sets the <CODE>PVBrowserPVDocument</CODE> to display. This method also 
   * clears the current values that have been added.
   * 
   * @param document The <CODE>PVBrowserPVDocument</CODE> to display in the table.
   */
    public void setDocument(PVBrowserPVDocument document) {
        if (this.document != null) {
            removePVListeners(this.document.getPV());
            clearChannels();
        }
        this.document = document;
        if (isProbe()) addProbeToExistingFields();
        BrowserPV pv = document.getPV();
        pv.addPropertyChangeListener(this);
        fireTableStructureChanged();
    }

    /**
   * gets the <CODE>PVBrowserPVDocument</CODE> displayed in the table.
   * 
   * @return The <CODE>PVBrowserPVDocument</CODE> displayed in the table.
   */
    public PVBrowserPVDocument getDocument() {
        return document;
    }

    /**
   * Gets the number of rows in the table. There is one row in the table for 
   * each field in the PV displayed.
   * 
   * @return The number of rows in the table.
   */
    public int getRowCount() {
        PVBrowserPVDocument document = getDocument();
        int fieldCount;
        if (document == null) fieldCount = 0; else {
            fieldCount = document.getPV().getFieldCount();
            if (isProbe()) fieldCount++;
        }
        return fieldCount;
    }

    /**
   * gets the number of columns in the table. There are two columns in the 
   * table, the field name and the field value.
   * 
   * @return The number of columns in the table.
   */
    public int getColumnCount() {
        if (isProbe()) return 3; else return 2;
    }

    /**
   * Gets the value for the given cell.
   * 
   * @param rowIndex The row index of the cell for which to return the value.
   * @param columnIndex The column index of the cell for which to return the value.
   * @return The value of the cell.
   */
    public Object getValueAt(int rowIndex, int columnIndex) {
        BrowserPVField field;
        if (isProbe()) if (rowIndex == 0) field = new BrowserPVField("VAL", "", null); else field = getDocument().getPV().getFieldAt(rowIndex - 1); else field = getDocument().getPV().getFieldAt(rowIndex);
        if (columnIndex == 0) return field.getName(); else if (columnIndex == 1) return field; else return probeValues.get(field.getName());
    }

    /**
   * gets the name of the given column.
   * 
   * @param column The column for which to return the name.
   * @return The name of the given column.
   */
    @Override
    public String getColumnName(int column) {
        if (column == 0) return "Field ID"; else if (column == 1) return "Field Value"; else return "Probe";
    }

    /**
   * Gets the <CODE>Class</CODE> for the type of data in the given column.
   * 
   * @param columnIndex The index of column for which to return the <CODE>Class</CODE>.
   * @return The <CODE>Class</CODE> for the type of data in the column.
   */
    @Override
    public Class getColumnClass(int columnIndex) {
        if (columnIndex == 1) return BrowserPVField.class;
        return super.getColumnClass(columnIndex);
    }

    /**
   * Called when a property of the <CODE>PVBrowserPVDocument</CODE> changes.
   * 
   * @param evt The <CODE>PropertyChangeEvent</CODE> that caused the invocation of this method.
   */
    public void propertyChange(PropertyChangeEvent evt) {
        String propertyName = evt.getPropertyName();
        BrowserPV rdbPV = getDocument().getPV();
        if (propertyName.equals("fieldCount")) {
            int oldValue = ((Integer) evt.getOldValue()).intValue();
            int newValue = ((Integer) evt.getNewValue()).intValue();
            if (isProbe()) {
                String pvID = rdbPV.getID();
                for (int i = oldValue; i < newValue; i++) {
                    addProbe(pvID, rdbPV.getFieldAt(i).getName());
                }
            }
            fireTableRowsInserted(oldValue, newValue - 1);
        } else if (propertyName.equals("value")) {
            BrowserPVField changedField = (BrowserPVField) evt.getSource();
            String fieldName = changedField.getName();
            int rowIndex = rdbPV.getIndexOfField(fieldName);
            fireTableCellUpdated(rowIndex, 2);
        }
    }

    /**
   * Makes the model display current values of the PV fields.
   * 
   * @param probe Pass as <CODE>true</CODE> to display the current PV values. <CODE>false</CODE> by default.
   */
    public void setProbe(boolean probe) {
        this.probe = probe;
        if (probe) addProbeToExistingFields(); else clearChannels();
        fireTableStructureChanged();
    }

    /**
   * Adds a probe to all fields in the PV.
   */
    private void addProbeToExistingFields() {
        BrowserPV rdbPV = getDocument().getPV();
        String pvID = rdbPV.getID();
        addProbe(pvID);
        int fieldCount = rdbPV.getFieldCount();
        for (int i = 0; i < fieldCount; i++) {
            String fieldName = rdbPV.getFieldAt(i).getName();
            StringBuffer channelName = new StringBuffer(pvID);
            channelName.append(".");
            channelName.append(fieldName);
            addProbe(channelName.toString());
        }
    }

    /**
   * Determines if the given channel is being monitored.
   * 
   * @param channel The channel to find.
   * @return <CODE>true</CODE> if the channel is being monitored, <CODE>false</CODE> otherwise.
   */
    private boolean findChannel(String channel) {
        synchronized (channels) {
            int channelCount = channels.size();
            for (int i = 0; i < channelCount; i++) if (((Channel) channels.get(i)).channelName().equals(channel)) return true;
            return false;
        }
    }

    /**
   * Adds a probe for the given field.
   * 
   * @param pv The <CODE>BrowserPV</CODE> containing the field to probe.
   * @param fieldIndex The index of the field to probe.
   */
    private void addProbe(BrowserPV pv, int fieldIndex) {
        addProbe(pv.getID(), pv.getFieldAt(fieldIndex).getName());
    }

    /**
   * Adds a probe for the given field.
   * 
   * @param pvID The ID of the <CODE>BrowserPV</CODE> containing the field to probe.
   * @param fieldName The name of the field to probe.
   */
    private void addProbe(String pvID, String fieldName) {
        StringBuffer channel = new StringBuffer(pvID);
        channel.append(".");
        channel.append(fieldName);
        addProbe(channel.toString());
    }

    /**
   * Adds the probe listener to the given channel.
   * 
   * @param channelName The name of the channel to probe.
   */
    private void addProbe(String channelName) {
        synchronized (channels) {
            if (!findChannel(channelName)) {
                final Channel channel = ChannelFactory.defaultFactory().getChannel(channelName);
                channel.addConnectionListener(connectionListener);
                channel.requestConnection();
                Channel.flushIO();
                channels.add(channel);
            }
        }
    }

    /**
   * Determines if current values are being displayed in the model.
   * @return
   */
    public boolean isProbe() {
        return probe;
    }

    /**
   * Frees the custom resources allocated by the window and the components in 
   * it.
   */
    public void freeCustomResources() {
        removePVListeners(getDocument().getPV());
        clearChannels();
    }

    /**
   * Clears the channel listeners.
   */
    private void clearChannels() {
        synchronized (channels) {
            int channelCount = channels.size();
            for (int i = 0; i < channelCount; i++) {
                Channel channel = (Channel) channels.get(i);
                channel.removeConnectionListener(connectionListener);
            }
            channels.clear();
        }
        synchronized (monitors) {
            int monitorCount = monitors.size();
            for (int i = 0; i < monitorCount; i++) ((Monitor) monitors.get(i)).clear();
            monitors.clear();
        }
    }

    /**
   * Removes the listeners ffrom the given PV and it's fields.
   * 
   * @param pv The <CODE>BrowserPV</CODE> from which to remove the listeners.
   */
    private void removePVListeners(BrowserPV pv) {
        if (pv != null) {
            pv.removePropertyChangeListener(this);
            int fieldCount = pv.getFieldCount();
            for (int i = 0; i < fieldCount; i++) pv.getFieldAt(i).removePropertyChangeListener(this);
        }
    }
}
