package gov.sns.apps.mpsinputtest;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.*;
import javax.swing.*;
import java.util.*;
import gov.sns.apps.mpsinputtest.MMRecord;

/**
* Provides a model for displaying instances of <CODE>Signal</CODE> and thier 
 * data relating to archives in a <CODE>JTable</CODE>.
 * 
 * @author Chris Fowlkes
 * @author Delphy Armstrong
 */
public class MPSmmTableModel extends AbstractTableModel implements CAValueListener, DataKeys, mmTableListener {

    protected static Map _jeriSubSys;

    protected static Map _jeriDev;

    protected static Map _jeriIOC;

    protected static Map _jeriChanNo;

    protected static Map _jeriTested;

    protected static Map _jeriPF;

    protected List _MPSmmTable;

    protected List _mpsRecords;

    protected List _channelWrappers;

    protected int[] _MPSTestRdy = new int[300];

    protected TableModel model;

    /**
		* Creates a new <CODE>MPSmmTableModel</CODE>.
	 */
    public MPSmmTableModel(final Map jeriSubSys, final Map jeriDev, final Map jeriIOC, final Map jeriChanNo, final Map isTestedMap, final Map PFstatusMap) {
        _mpsRecords = new ArrayList();
        _channelWrappers = new ArrayList(14);
        if (jeriIOC != null) {
            setJeriMaps(jeriSubSys, jeriIOC, jeriDev, jeriChanNo, isTestedMap, PFstatusMap);
        } else {
            _jeriSubSys = new HashMap();
            _jeriIOC = new HashMap();
            _jeriDev = new HashMap();
            _jeriChanNo = new HashMap();
            _jeriTested = new HashMap();
            _jeriPF = new HashMap();
            _MPSmmTable = new ArrayList(_jeriSubSys.size());
        }
    }

    public MPSmmTableModel() {
        this(null, null, null, null, null, null);
    }

    public void setJeriMaps(final Map jeriSubSys, final Map jeriIOC, final Map jeriDev, final Map jeriChanNo, final Map isTestedMap, final Map PFstatusMap) {
        _jeriSubSys = jeriSubSys;
        _jeriIOC = jeriIOC;
        _jeriDev = jeriDev;
        _jeriChanNo = jeriChanNo;
        _jeriTested = isTestedMap;
        _jeriPF = PFstatusMap;
        String pv = "";
        String value = "";
        String ioc = "";
        String dev = "";
        String chanNo = "";
        String isTested = "";
        String testRdy = "";
        String pfStat = "";
        String JeriStr = new String();
        Iterator iter = _jeriSubSys.keySet().iterator();
        final Iterator iter2 = _jeriSubSys.values().iterator();
        final Iterator iter3 = _jeriIOC.values().iterator();
        final Iterator iter4 = _jeriDev.values().iterator();
        final Iterator iter5 = _jeriChanNo.values().iterator();
        final Iterator iter6 = _jeriTested.values().iterator();
        final Iterator iter7 = _jeriPF.values().iterator();
        MMRecord MPSmmRecord;
        int i = 0;
        int isRdy = 0;
        createChannelWrappers();
        String chainSel = getMainWindow().getChainSel();
        while (iter.hasNext()) {
            pv = (String) iter.next();
            if (chainSel.equals("MEBT_BS") && (getMainWindow().getSubSysSel().equals("Mag") || getMainWindow().getDeviceSel().equals("PS"))) {
                TestMEBTMagPSPanel newPanel = new TestMEBTMagPSPanel();
                newPanel.createChannelWrappers(pv);
                isRdy = getMainWindow().MEBTMagTestRdy(pv);
                if (isRdy == 0) testRdy = "No"; else if (isRdy < 0) testRdy = "<html><body><font COLOR=#ff0000> ? </font></body></html>"; else testRdy = "Yes";
            } else if ((chainSel.equals("CCL_BS") || chainSel.equals("LDmp")) && (getMainWindow().getSubSysSel().equals("Mag") || getMainWindow().getDeviceSel().equals("PS"))) {
                TestMagPSPanel newPanel = new TestMagPSPanel();
                newPanel.createChannelWrappers(pv);
                testRdy = "Yes";
            } else if ((chainSel.equals("CCL_BS") || chainSel.equals("LDmp")) && getMainWindow().getDeviceSel().equals("BLM")) {
                TestBLMPanel newPanel = new TestBLMPanel();
                newPanel.createChannelWrappers(pv);
                _MPSTestRdy = getMainWindow().getMPSTestRdy(pv);
                isRdy = _MPSTestRdy[0];
                if (isRdy < 0) testRdy = "<html><body><font COLOR=#ff0000> ? </font></body></html>"; else if (isRdy != 0) testRdy = "No"; else testRdy = "Yes";
            } else if ((chainSel.equals("MEBT_BS") || chainSel.equals("CCL_BS") || chainSel.equals("LDmp")) && (getMainWindow().getSubSysSel().equals("LLRF") || getMainWindow().getDeviceSel().equals("HPM"))) {
                int vals[] = getMainWindow().getMPSllrfTestRdy(pv);
                isRdy = vals[i];
                if (isRdy < 0) testRdy = "<html><body><font COLOR=#ff0000> ? </font></body></html>"; else if (isRdy == 0) testRdy = "No"; else testRdy = "Yes";
            } else if (chainSel.equals("MEBT_BS") && (getMainWindow().getSubSysSel().equals("RF") || getMainWindow().getDeviceSel().equals("Bnch"))) {
                ContMPSHPRFPanel newPanel = new ContMPSHPRFPanel();
                newPanel.createChannelWrappers(pv);
                int vals[] = getMainWindow().getRdyToTestHPRF(pv);
                isRdy = vals[i];
                if (isRdy == 0) testRdy = "No"; else if (isRdy < 0) testRdy = "<html><body><font COLOR=#ff0000> ? </font></body></html>"; else testRdy = "Yes";
            }
            i++;
            value = (String) iter2.next();
            ioc = (String) iter3.next();
            dev = (String) iter4.next();
            chanNo = iter5.next().toString();
            isTested = iter6.next().toString();
            pfStat = iter7.next().toString();
            if (pfStat.equals("N")) isTested = "<html><body><font COLOR=#ff0000> " + isTested + " </font></body></html>";
            MPSmmRecord = new MMRecord(pv, value, dev, ioc, chanNo, isTested, testRdy, pfStat, isRdy);
            createMPSmmTable(pv, value, dev, ioc, chanNo, isTested, testRdy, i);
            synchronized (_mpsRecords) {
                _mpsRecords.add(MPSmmRecord);
            }
        }
        MMRecord rec;
        ListIterator typeItr = _mpsRecords.listIterator();
        while (typeItr.hasNext()) {
            rec = (MMRecord) typeItr.next();
        }
        sortRecords();
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
    }

    public void clear() {
        if (_MPSmmTable == null) return;
        final Iterator iter = _MPSmmTable.iterator();
        while (iter.hasNext()) {
            final MPSmmTable mmTable = (MPSmmTable) iter.next();
            mmTable.removeMPSmmListener(this);
        }
        _jeriSubSys.clear();
        _jeriIOC.clear();
        _jeriDev.clear();
        _jeriChanNo.clear();
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
        if (_jeriSubSys == null) return 0;
        return _jeriSubSys.size();
    }

    /**
		* Gets the number of columns in the model. This model contains three columns:
	 * signal ID, archive frequency, and archive type.
	 * 
	 * @return The number of columns in the model.
	 */
    public int getColumnCount() {
        return 7;
    }

    /**
		* Gets the value for the given cell.
	 * 
	 * @param rowIndex The index of the row for the cell of which to return the value.
	 * @param columnIndex The index of the column for the cell of which to return the value.
	 * @return The value of the given cell.
	 */
    public Object getValueAt(int rowIndex, int columnIndex) {
        final MMRecord record;
        synchronized (_mpsRecords) {
            record = (MMRecord) _mpsRecords.get(rowIndex);
        }
        switch(columnIndex) {
            case 0:
                return record.getPV();
            case 1:
                return record.getSubSystem();
            case 2:
                return record.getDevice();
            case 3:
                return record.getIOC();
            case 4:
                return record.getChannelNo();
            case 5:
                return record.getTestedValue();
            case 6:
                return record.getTestRdyValue();
            default:
                return "";
        }
    }

    public void SetValueAt(Object value, int row, int col) {
        JTable mpsTable = getMainWindow().getMPSTable();
        mpsTable.setValueAt(value, row, col);
        setValueAt(value, row, col);
        MPSmmTable mmTable = new MPSmmTable(this, value, row, col);
        fireTableCellUpdated(row, col);
        fireTableDataChanged();
    }

    /** Indicates a new channel access value has been found for this wrapped channel. */
    public void newValue(final ChannelWrapper wrapper, int value) {
        int[] isMPSTestRdy;
        String name = "";
        String pv = "";
        String testRdy = "";
        int index, j;
        int isRdy = 1;
        int[] RdyArr = new int[500];
        JTable mpsTable = getMainWindow().getMPSTable();
        Iterator iter = _jeriSubSys.keySet().iterator();
        MPSWindow mainWindow = getMainWindow();
        name = wrapper.getName();
        value = wrapper.getValue();
        if (mainWindow != null && value == 0 && (wrapper.getName().compareTo("ICS_MPS:Switch_MachMd:MEBT_BS") == 0 || wrapper.getName().compareTo("ICS_MPS:Switch_MachMd:CCL_BS") == 0 || wrapper.getName().compareTo("ICS_MPS:Switch_MachMd:LinDmp") == 0 || wrapper.getName().compareTo("ICS_MPS:Switch_MachMd:InjDmp") == 0 || wrapper.getName().compareTo("ICS_MPS:Switch_MachMd:Ring") == 0 || wrapper.getName().compareTo("ICS_MPS:Switch_MachMd:ExtDmp") == 0 || wrapper.getName().compareTo("ICS_MPS:Switch_MachMd:Tgt") == 0)) {
            mainWindow.updateViews();
        } else {
            index = name.lastIndexOf("_LLRF:");
            if (index > -1) {
                if (name.lastIndexOf(":Flt10") > -1 || name.lastIndexOf(":FPL_MEBT_BS_cable_status") > -1) {
                    j = 0;
                    String[] testReady = { "1", "1" };
                    while (iter.hasNext()) {
                        pv = (String) iter.next();
                        if (name.equals(pv)) break;
                        j++;
                    }
                    isMPSTestRdy = getMainWindow().getMPSllrfTestRdy(pv);
                    for (int i = 0; i < isMPSTestRdy.length && isRdy == 1; i++) {
                        if (isMPSTestRdy[i] == -1) isRdy = -1; else if (testReady[i] != String.valueOf(isMPSTestRdy[i])) isRdy = 0;
                    }
                    if (isRdy == 0) testRdy = "No"; else if (isRdy < 0) testRdy = "<html><body><font COLOR=#ff0000> ? </font></body></html>"; else testRdy = "Yes";
                    mpsTable.setValueAt(testRdy, j, 6);
                }
            } else {
                index = name.lastIndexOf("_Mag:");
                if (index > -1) {
                    if (name.lastIndexOf(":FltS") > -1) {
                        j = 0;
                        String[] testReady = { "0" };
                        while (iter.hasNext()) {
                            pv = (String) iter.next();
                            if (name.equals(pv)) break;
                            j++;
                        }
                        isMPSTestRdy = getMainWindow().getMPSllrfTestRdy(pv);
                        for (int i = 0; i < isMPSTestRdy.length && isRdy == 1; i++) {
                            if (isMPSTestRdy[i] == -1) isRdy = -1; else if (testReady[i] != String.valueOf(isMPSTestRdy[i])) isRdy = 0;
                        }
                        if (isRdy == 0) testRdy = "No"; else if (isRdy < 0) testRdy = "<html><body><font COLOR=#ff0000> ? </font></body></html>"; else testRdy = "Yes";
                        mpsTable.setValueAt(testRdy, j, 6);
                    }
                } else {
                    index = name.lastIndexOf("MEBT_Mag:");
                    if (index > -1) {
                        if (name.lastIndexOf(":Sts_AC") > -1 || name.lastIndexOf(":Fault") > -1 || name.lastIndexOf(":Ilk") > -1 || name.lastIndexOf(":Sts_OVR") > -1 || name.lastIndexOf(":Sts_Rdy") > -1 || name.lastIndexOf(":Sts_Rem") > -1 || name.lastIndexOf(":Sts_H2O") > -1 || name.lastIndexOf(":Enable") > -1 || name.lastIndexOf(":FPL_MEBT_BS_cable_status") > -1) {
                            j = 0;
                            String[] testReady = { "0", "0", "0", "0", "1", "1", "1", "1", "1" };
                            while (iter.hasNext()) {
                                pv = (String) iter.next();
                                if (name.equals(pv)) break;
                                j++;
                            }
                            isMPSTestRdy = getMainWindow().getMPSTestRdy(pv);
                            for (int i = 0; i < isMPSTestRdy.length && isRdy == 1; i++) {
                                if (isMPSTestRdy[i] == -1) isRdy = -1; else if (testReady[i] != String.valueOf(isMPSTestRdy[i])) isRdy = 0;
                            }
                            if (isRdy == 0) testRdy = "No"; else if (isRdy < 0) testRdy = "<html><body><font COLOR=#ff0000> ? </font></body></html>"; else testRdy = "Yes";
                            mpsTable.setValueAt(testRdy, j, 6);
                        }
                    } else {
                        index = name.lastIndexOf("_RF:");
                        if (index > -1) {
                            if (name.lastIndexOf(":Sts_AC") > -1 || name.lastIndexOf(":Sts_Cool") > -1 || name.lastIndexOf(":Enable") > -1 || name.lastIndexOf(":Lcl") > -1 || name.lastIndexOf(":FPL_MEBT_BS_cable_status") > -1) {
                                j = 0;
                                String[] testReady = { "0", "0", "0", "1", "1" };
                                while (iter.hasNext()) {
                                    pv = (String) iter.next();
                                    if (name.equals(pv)) break;
                                    j++;
                                }
                                isMPSTestRdy = getMainWindow().getRdyToTestHPRF(pv);
                                for (int i = 0; i < isMPSTestRdy.length && isRdy == 1; i++) {
                                    if (isMPSTestRdy[i] == -1) isRdy = -1; else if (testReady[i] != String.valueOf(isMPSTestRdy[i])) isRdy = 0;
                                }
                                if (isRdy == 0) testRdy = "No"; else if (isRdy < 0) testRdy = "<html><body><font COLOR=#ff0000> ? </font></body></html>"; else testRdy = "Yes";
                                mpsTable.setValueAt(testRdy, j, 6);
                            }
                        }
                    }
                }
            }
        }
        fireTableDataChanged();
    }

    /** Indicates a new channel access value has been found for this wrapped channel. */
    public void newValue(TableModel mmTable, int row, int column, Object value) {
    }

    public MPSWindow getMainWindow() {
        MPSDocument document = new MPSDocument();
        return document.getWindow();
    }

    public void createChannelWrappers() {
        String pv = "ICS_MPS:Switch_MachMd:MEBT_BS";
        ChannelWrapper wrapper = new ChannelWrapper(pv);
        _channelWrappers.add(wrapper);
        wrapper.addCAValueListener(this);
        pv = "ICS_MPS:Switch_MachMd:CCL_BS";
        wrapper = new ChannelWrapper(pv);
        _channelWrappers.add(wrapper);
        wrapper.addCAValueListener(this);
        pv = "ICS_MPS:Switch_MachMd:LinDmp";
        wrapper = new ChannelWrapper(pv);
        _channelWrappers.add(wrapper);
        wrapper.addCAValueListener(this);
        pv = "ICS_MPS:Switch_MachMd:InjDmp";
        wrapper = new ChannelWrapper(pv);
        _channelWrappers.add(wrapper);
        wrapper.addCAValueListener(this);
        pv = "ICS_MPS:Switch_MachMd:Ring";
        wrapper = new ChannelWrapper(pv);
        _channelWrappers.add(wrapper);
        wrapper.addCAValueListener(this);
        pv = "ICS_MPS:Switch_MachMd:ExtDmp";
        wrapper = new ChannelWrapper(pv);
        _channelWrappers.add(wrapper);
        wrapper.addCAValueListener(this);
        pv = "ICS_MPS:Switch_MachMd:Tgt";
        wrapper = new ChannelWrapper(pv);
        _channelWrappers.add(wrapper);
        wrapper.addCAValueListener(this);
    }

    private void createMPSmmTable(final String pv, final String subsys, final String dev, final String ioc, final String chanNo, final String tested, final String testRdy, final int r) {
        MPSmmTable mmTable = new MPSmmTable(this, pv, subsys, dev, ioc, chanNo, tested, testRdy, r);
        if (_MPSmmTable == null) _MPSmmTable = new ArrayList(_jeriSubSys.size());
        _MPSmmTable.add(mmTable);
    }
}
