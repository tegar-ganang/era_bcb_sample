package gov.sns.apps.mpscheckbypass;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.*;
import javax.swing.*;
import java.util.*;
import gov.sns.apps.mpscheckbypass.MMRecord;

/**
* Provides a model for displaying instances of <CODE>Signal</CODE> and thier 
 * data relating to archives in a <CODE>JTable</CODE>.
 * 
 * @author Chris Fowlkes
 * @author Delphy Armstrong
 */
public class MPSmmTableModel extends AbstractTableModel implements CAValueListener, DataKeys, mmTableListener {

    protected Map _jeriMM;

    protected List _channels;

    protected List _MPSswMaskWrappers;

    protected List _MPSactiveWrappers;

    protected List _MPSmmTable;

    protected List _mpsRecords;

    protected String _MachMode;

    protected int _categry;

    public static List _MPSFPWrappers;

    protected ChannelWrapper swMaskWrap = null;

    protected ChannelWrapper activeWrap = null;

    public static int numMasked, numActive;

    /**
		* Creates a new <CODE>MPSmmTableModel</CODE>.
	 */
    public MPSmmTableModel(final Map jeriMM, final String MachMode, final int categry) {
        _mpsRecords = new ArrayList();
        _MachMode = MachMode;
        _categry = categry;
        if (jeriMM != null) {
            setJeriMM(jeriMM, categry);
        } else {
            _jeriMM = new HashMap();
            _channels = new ArrayList();
            _MPSswMaskWrappers = new ArrayList();
            _MPSactiveWrappers = new ArrayList();
            _MPSmmTable = new ArrayList();
        }
    }

    public MPSmmTableModel() {
        this(null, null, -1);
    }

    public void setJeriMM(final Map jeriMM, final int catgy) {
        _jeriMM = jeriMM;
        createChannelWrappers();
        createMPSmmTable();
        synchronized (_mpsRecords) {
            _mpsRecords.clear();
        }
        String pv = "";
        String value = "";
        String fullPv = "";
        int JeriVal, caVal;
        String JeriStr = new String();
        _MPSmmTable = new ArrayList(_jeriMM.size());
        final Iterator iter = _jeriMM.keySet().iterator();
        final Iterator iter2 = _jeriMM.values().iterator();
        MMRecord MPSmmRecord = null;
        numMasked = 0;
        numActive = 0;
        Object wrapper = null;
        MMRecord rec;
        while (iter.hasNext()) {
            pv = (String) iter.next();
            String partPV = pv.substring(0, pv.length() - 2);
            value = (String) iter2.next();
            Iterator CWiter = _channels.iterator();
            if (CWiter.hasNext()) wrapper = CWiter.next();
            while (CWiter.hasNext() && partPV.compareTo((String) wrapper) != 0) wrapper = CWiter.next();
            CWiter = _MPSswMaskWrappers.iterator();
            if (CWiter.hasNext()) swMaskWrap = (ChannelWrapper) CWiter.next();
            fullPv = partPV + "swmask";
            while (CWiter.hasNext() && swMaskWrap.getName().compareTo(fullPv) != 0) swMaskWrap = (ChannelWrapper) CWiter.next();
            CWiter = _MPSactiveWrappers.iterator();
            if (CWiter.hasNext()) activeWrap = (ChannelWrapper) CWiter.next();
            fullPv = partPV + "sw_jump_status";
            while (CWiter.hasNext() && activeWrap.getName().compareTo(fullPv) != 0) activeWrap = (ChannelWrapper) CWiter.next();
            if (swMaskWrap.getValue() == 1 || activeWrap.getValue() == 1) {
                MPSmmRecord = new MMRecord(wrapper, swMaskWrap, activeWrap, catgy);
                if (swMaskWrap.getValue() == 1) numMasked++;
                if (activeWrap.getValue() == 1) numActive++;
                if ((catgy == 1 && swMaskWrap.getValue() == 1) || (catgy == 2 && activeWrap.getValue() == 1)) {
                    synchronized (_mpsRecords) {
                        _mpsRecords.add(MPSmmRecord);
                    }
                }
            }
            sortRecords();
        }
    }

    @Override
    public void fireTableDataChanged() {
        sortRecords();
        super.fireTableDataChanged();
    }

    protected void sortRecords() {
        synchronized (_mpsRecords) {
            Collections.sort(_mpsRecords);
        }
        Collections.reverse(_mpsRecords);
    }

    public void clear() {
        if (_channels == null) return;
        Iterator iter = _MPSmmTable.iterator();
        while (iter.hasNext()) {
            final MPSmmTable mmTable = (MPSmmTable) iter.next();
        }
        _channels.clear();
        _jeriMM.clear();
    }

    private void createChannelWrappers() {
        _channels = new ArrayList(_jeriMM.size());
        _MPSswMaskWrappers = new ArrayList(_jeriMM.size());
        _MPSactiveWrappers = new ArrayList(_jeriMM.size());
        final Iterator iter = _jeriMM.keySet().iterator();
        int i = 0;
        String swMaskPv = "";
        String activePv = "";
        while (iter.hasNext()) {
            String pv = (String) iter.next();
            String partPV = pv.substring(0, pv.length() - 2);
            _channels.add(partPV);
            swMaskPv = partPV + "swmask";
            swMaskWrap = new ChannelWrapper(swMaskPv);
            _MPSswMaskWrappers.add(swMaskWrap);
            swMaskWrap.addCAValueListener(this);
            activePv = partPV + "sw_jump_status";
            activeWrap = new ChannelWrapper(activePv);
            _MPSactiveWrappers.add(activeWrap);
            activeWrap.addCAValueListener(this);
        }
    }

    private void createMPSmmTable() {
        _MPSmmTable = new ArrayList(_jeriMM.size());
        Iterator iter = _jeriMM.keySet().iterator();
        while (iter.hasNext()) {
            final String pv = (String) iter.next();
            final MPSmmTable mmTable = new MPSmmTable(this, pv);
            _MPSmmTable.add(mmTable);
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int vColIndex) {
        return false;
    }

    /**
		* Returns the number of rows in the model.
	 * 
	 * @return The number of rows in the model.
	 */
    public int getRowCount() {
        if (_channels == null) return 0;
        return _mpsRecords.size();
    }

    /**
		* Gets the number of columns in the model. This model contains three columns:
	 * signal ID, archive frequency, and archive type.
	 * 
	 * @return The number of columns in the model.
	 */
    public int getColumnCount() {
        return 1;
    }

    /**
		* Gets the value for the given cell.
	 * 
	 * @param rowIndex The index of the row for the cell of which to return the value.
	 * @param columnIndex The index of the column for the cell of which to return the value.
	 * @return The value of the given cell.

	This is the routine that updates the JTable through the wrappers CAListeners

	 */
    public Object getValueAt(int rowIndex, int columnIndex) {
        final MMRecord record;
        synchronized (_mpsRecords) {
            record = (MMRecord) _mpsRecords.get(rowIndex);
        }
        switch(columnIndex) {
            case 0:
                if (record.getSwMaskValue() == 1 && record.getActiveValue() == 1) {
                    record.setBoth();
                    return record.getRedDisplayString(record.getPV());
                } else return record.getPV();
            default:
                return "";
        }
    }

    /** Indicates a new channel access value has been found for this wrapped channel. */
    public void newValue(final ChannelWrapper wrapper, int value) {
        JTable mpsTable = getMainWindow().getMPSmmTable();
        if (mpsTable == null) return;
        MMRecord MPSmmRecord = null;
        int len = mpsTable.getRowCount();
        String pv = wrapper.getName();
        value = wrapper.getValue();
        int i;
        if (pv.indexOf("sw_jump_status") > 0 && value == 1) {
            numActive++;
            getMainWindow().updateBtn(numMasked, numActive);
            for (i = 0; i < len; i++) {
                if (pv.equals(mpsTable.getValueAt(i, 0))) break;
            }
            if (i == len) {
                MPSmmRecord = new MMRecord(pv, wrapper, _categry);
                if (_categry == 2) {
                    synchronized (_mpsRecords) {
                        _mpsRecords.add(MPSmmRecord);
                    }
                }
            }
        }
        if (pv.indexOf("swmask") > 0 && value == 1) {
            numMasked++;
            getMainWindow().updateBtn(numMasked, numActive);
            len = mpsTable.getRowCount();
            for (i = 0; i < len; i++) {
                if (pv.equals(mpsTable.getValueAt(i, 0))) break;
            }
            if (i == len) {
                MPSmmRecord = new MMRecord(pv, wrapper, _categry);
                if (_categry == 1) {
                    synchronized (_mpsRecords) {
                        _mpsRecords.add(MPSmmRecord);
                    }
                }
            }
        }
        fireTableDataChanged();
        wrapper.getChannel().flushIO();
    }

    public void newValue(final ChannelWrapper wrapper, boolean value) {
        MPSWindow mainWindow = getMainWindow();
        value = (wrapper.getValue() != 0);
        fireTableDataChanged();
    }

    /** Indicates a new channel access value has been found for this wrapped channel. */
    public void newValue(TableModel mmTable, int row, int column, Object value) {
        value = mmTable.getValueAt(row, column);
        fireTableDataChanged();
    }

    public int getNumActive() {
        return numActive;
    }

    public int getNumMasked() {
        return numMasked;
    }

    public MPSWindow getMainWindow() {
        MPSDocument document = new MPSDocument();
        return document.getWindow();
    }

    public void createFPChannelWrappers() {
        _MPSFPWrappers = new ArrayList(14);
        String pv = "ICS_MPS:FPAR_MEBT_BS:FPAR_MEBT_BS_chan_status";
        ChannelWrapper wrapper = new ChannelWrapper(pv);
        _MPSFPWrappers.add(wrapper);
        wrapper.addCAValueListener(this);
        pv = "ICS_MPS:FPL_MEBT_BS:FPL_MEBT_BS_chan_status";
        wrapper = new ChannelWrapper(pv);
        _MPSFPWrappers.add(wrapper);
        wrapper.addCAValueListener(this);
        pv = "ICS_MPS:FPAR_CCL_BS:FPAR_MEBT_BS_chan_status";
        wrapper = new ChannelWrapper(pv);
        _MPSFPWrappers.add(wrapper);
        wrapper.addCAValueListener(this);
        pv = "ICS_MPS:FPL_CCL_BS:FPL_MEBT_BS_chan_status";
        wrapper = new ChannelWrapper(pv);
        _MPSFPWrappers.add(wrapper);
        wrapper.addCAValueListener(this);
        pv = "ICS_MPS:FPAR_LDmp:FPAR_MEBT_BS_chan_status";
        wrapper = new ChannelWrapper(pv);
        _MPSFPWrappers.add(wrapper);
        wrapper.addCAValueListener(this);
        pv = "ICS_MPS:FPL_LDmp:FPL_MEBT_BS_chan_status";
        wrapper = new ChannelWrapper(pv);
        _MPSFPWrappers.add(wrapper);
        wrapper.addCAValueListener(this);
        pv = "ICS_MPS:Switch_MachMd:MEBT_BS";
        wrapper = new ChannelWrapper(pv);
        _MPSFPWrappers.add(wrapper);
        wrapper.addCAValueListener(this);
        pv = "ICS_MPS:Switch_MachMd:CCL_BS";
        wrapper = new ChannelWrapper(pv);
        _MPSFPWrappers.add(wrapper);
        wrapper.addCAValueListener(this);
        pv = "ICS_MPS:Switch_MachMd:LinDmp";
        wrapper = new ChannelWrapper(pv);
        _MPSFPWrappers.add(wrapper);
        wrapper.addCAValueListener(this);
        pv = "ICS_MPS:Switch_MachMd:InjDmp";
        wrapper = new ChannelWrapper(pv);
        _MPSFPWrappers.add(wrapper);
        wrapper.addCAValueListener(this);
        pv = "ICS_MPS:Switch_MachMd:Ring";
        wrapper = new ChannelWrapper(pv);
        _MPSFPWrappers.add(wrapper);
        wrapper.addCAValueListener(this);
        pv = "ICS_MPS:Switch_MachMd:ExtDmp";
        wrapper = new ChannelWrapper(pv);
        _MPSFPWrappers.add(wrapper);
        wrapper.addCAValueListener(this);
        pv = "ICS_MPS:Switch_MachMd:Tgt";
        wrapper = new ChannelWrapper(pv);
        _MPSFPWrappers.add(wrapper);
        wrapper.addCAValueListener(this);
    }

    public String getMEBTFPARlabel() {
        int value;
        String htmlStr;
        ChannelWrapper wrapper = (ChannelWrapper) _MPSFPWrappers.get(0);
        value = wrapper.getValue();
        if (value == 0) htmlStr = "<html><body><font COLOR=#ff0000> 0  </font></body></html>"; else htmlStr = "1";
        return htmlStr;
    }

    public String getMEBTFPLlabel() {
        int value;
        String htmlStr;
        ChannelWrapper wrapper = (ChannelWrapper) _MPSFPWrappers.get(1);
        value = wrapper.getValue();
        if (value == 0) htmlStr = "<html><body><font COLOR=#ff0000> 0  </font></body></html>"; else htmlStr = "1";
        return htmlStr;
    }

    public String getCCLFPARlabel() {
        int value;
        String htmlStr;
        ChannelWrapper wrapper = (ChannelWrapper) _MPSFPWrappers.get(2);
        value = wrapper.getValue();
        if (value == 0) htmlStr = "<html><body><font COLOR=#ff0000> 0  </font></body></html>"; else htmlStr = "1";
        return htmlStr;
    }

    public String getCCLFPLlabel() {
        int value;
        String htmlStr;
        ChannelWrapper wrapper = (ChannelWrapper) _MPSFPWrappers.get(3);
        value = wrapper.getValue();
        if (value == 0) htmlStr = "<html><body><font COLOR=#ff0000> 0  </font></body></html>"; else htmlStr = "1";
        return htmlStr;
    }

    public String getLDmpFPARlabel() {
        int value;
        String htmlStr;
        ChannelWrapper wrapper = (ChannelWrapper) _MPSFPWrappers.get(4);
        value = wrapper.getValue();
        if (value == 0) htmlStr = "<html><body><font COLOR=#ff0000> 0  </font></body></html>"; else htmlStr = "1";
        return htmlStr;
    }

    public String getLDmpFPLlabel() {
        int value;
        String htmlStr;
        ChannelWrapper wrapper = (ChannelWrapper) _MPSFPWrappers.get(5);
        value = wrapper.getValue();
        if (value == 0) htmlStr = "<html><body><font COLOR=#ff0000> 0  </font></body></html>"; else htmlStr = "1";
        return htmlStr;
    }
}
