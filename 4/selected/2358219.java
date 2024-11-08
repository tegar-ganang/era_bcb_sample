package gov.sns.apps.mpsblmlimits;

import javax.swing.table.*;
import javax.swing.JTable;
import javax.swing.event.*;
import java.util.*;
import gov.sns.apps.mpsblmlimits.MMRecord;

/**
* Provides a model for displaying instances of <CODE>Signal</CODE> and thier 
 * data relating to archives in a <CODE>JTable</CODE>.
 * 
 * @author Chris Fowlkes
 * @author Delphy Armstrong
 */
public class MPSmmTableModel extends DefaultTableModel implements CAValueListener, DataKeys, mmTableListener {

    MPSmmTable mmTable;

    List _MPSmmTable;

    static List _mpsRecords;

    TableModel model;

    int delay;

    static String _pv = null;

    static List _MPSpll = null;

    static List _MPSpll600 = null;

    static List _hv = null;

    static List _NomTrip = null;

    static List _UserTrip = null;

    static List _TuneTrip = null;

    static List _hvcur = null;

    static List _afe = null;

    static List _hvrb = null;

    static List _pll = null;

    static Object[][] recs = new Object[200][11];

    static ChannelWrapper[][] MPSpll600Wrapper = new ChannelWrapper[200][4];

    static ChannelWrapper[][] hvWrapper = new ChannelWrapper[200][8];

    static ChannelWrapper[][] hvcurWrapper = new ChannelWrapper[200][6];

    static ChannelWrapper[][] afeWrapper = new ChannelWrapper[200][4];

    static ChannelWrapper[][] hvrbWrapper = new ChannelWrapper[200][6];

    static ChannelWrapper[][] UserTripWrap = new ChannelWrapper[200][4];

    static ChannelWrapper[][] TuneTripWrap = new ChannelWrapper[200][4];

    static ChannelWrapper[][] NomTripWrap = new ChannelWrapper[200][4];

    static ChannelWrapper[][] pllWrapper = new ChannelWrapper[200][6];

    static ChannelWrapper[][] MPSpllWrapper = new ChannelWrapper[200][4];

    ChannelWrapper wrapper = null;

    private static List pvPartList;

    int index;

    static int tableLength;

    static JTable mpsTable = null;

    int len, retI;

    public String chName, chName2, chName3, chName4, chName5, chName6, chName7, chName8;

    static MMRecord[] MPSmmRecords = new MMRecord[200];

    static String HeaderStr[] = { "Signal", "DbgHVBias", "DbgHVBiasRb", "DbgHVCurrentRb", "DbgAFEFirstStageGainRb", "DbgSlowPulseLossRb", "DbgMPSPulseLossLimit", "DbgMPS600PulsesLossLimit", "nom_trip", "tune_trip", "user_trip" };

    /**
		* Creates a new <CODE>MPSmmTableModel</CODE>.
	 */
    public MPSmmTableModel(Map pvS_SS_Dev) {
        int index = 0;
        len = pvS_SS_Dev.size();
        tableLength = len;
        setRowCount(len);
        setColumnCount(HeaderStr.length);
        addTableModelListener(new TableModelListener() {

            public void tableChanged(TableModelEvent e) {
            }
        });
        _mpsRecords = new ArrayList(len);
        _MPSpll = new ArrayList(len * 4);
        _MPSpll600 = new ArrayList(len * 4);
        _hv = new ArrayList(len * 8);
        _NomTrip = new ArrayList(len * 4);
        _UserTrip = new ArrayList(len * 4);
        _TuneTrip = new ArrayList(len * 4);
        _hvcur = new ArrayList(len * 6);
        _afe = new ArrayList(len * 4);
        _hvrb = new ArrayList(len * 6);
        _pll = new ArrayList(len * 6);
    }

    public void setJeriMaps(Iterator keyValue, int j) {
        Object key = keyValue.next();
        _pv = key.toString();
        createChannelWrappers(_pv, j);
        String hv = "";
        hv = getHVBias(_pv);
        String hvcur = "";
        hvcur = getHVCurRb(_pv);
        String hvrb = "";
        hvrb = getHVrb(_pv);
        String afe = "";
        afe = getAFE(_pv);
        String pll = "";
        pll = getPLL(_pv);
        String MPSpll = "";
        MPSpll = getMPSpll(_pv);
        String MPSpll600 = "";
        MPSpll600 = getMPS600pll(_pv);
        String NomTrip = "";
        NomTrip = getNomTrip(_pv);
        String TuneTrip = "";
        TuneTrip = getTuneTrip(_pv);
        String UserTrip = "";
        UserTrip = getUserTrip(_pv);
        int index = _mpsRecords.size();
        MPSmmRecords[index] = new MMRecord(_pv, hv, hvcur, hvrb, afe, pll, MPSpll, MPSpll600, NomTrip, TuneTrip, UserTrip);
        createMPSmmTable(_pv, hv, hvcur, hvrb, afe, pll, MPSpll, MPSpll600, NomTrip, TuneTrip, UserTrip);
        addRow(MPSmmRecords[index]);
        fireTableDataChanged();
    }

    private void createMPSmmTable(String pv, String hv, String hvcur, String hvrb, String afe, String pll, String MPSpll, String MPSpll600, String NomTrip, String TuneTrip, String UserTrip) {
        int r = _mpsRecords.size();
        mmTable = new MPSmmTable(this, pv, hv, hvcur, hvrb, afe, pll, MPSpll, MPSpll600, NomTrip, TuneTrip, UserTrip, r);
        if (_MPSmmTable == null) _MPSmmTable = new ArrayList(r);
        _MPSmmTable.add(mmTable);
    }

    void sortRecords() {
        synchronized (_mpsRecords) {
            Collections.sort(_mpsRecords);
        }
    }

    @Override
    public void fireTableDataChanged() {
        super.fireTableDataChanged();
    }

    public void clear() {
        if (_MPSmmTable == null) return;
        final Iterator iter = _MPSmmTable.iterator();
        while (iter.hasNext()) {
            mmTable = (MPSmmTable) iter.next();
            mmTable.removeMPSmmListener(this);
        }
    }

    public void newValueAt(Object value, int row, int col) {
        MMRecord record = null;
        int len = _mpsRecords.size();
        for (int i = 0; i < len; i++) {
            record = (MMRecord) _mpsRecords.get(i);
            if (mpsTable == null) mpsTable = getMainWindow().getMPSmmTable(this);
            if (record.getPV().equals(mpsTable.getValueAt(row, 0))) break;
        }
        switch(col) {
            case 0:
                record.setPV((String) value);
                break;
            case 1:
                record.setHV((String) value);
                break;
            case 2:
                record.setHVrb((String) value);
                break;
            case 3:
                record.setHVcur((String) value);
                break;
            case 4:
                record.setAFE((String) value);
                break;
            case 5:
                record.setPLL((String) value);
                break;
            case 6:
                record.setMPSpll((String) value);
                break;
            case 7:
                record.setMPSpll600((String) value);
                break;
            case 8:
                record.setNomTrip((String) value);
                break;
            case 9:
                record.setTuneTrip((String) value);
                break;
            case 10:
                record.setUserTrip((String) value);
                break;
        }
        fireTableCellUpdated(row, col);
        fireTableDataChanged();
    }

    @Override
    public void setValueAt(Object value, int row, int col) {
        fireTableCellUpdated(row, col);
        fireTableDataChanged();
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
    @Override
    public int getRowCount() {
        return tableLength;
    }

    public Object GetValueAt(int rowIndex, int columnIndex) {
        return ((java.util.List) _mpsRecords.get(rowIndex)).get(columnIndex);
    }

    @Override
    public String getColumnName(int col) {
        return HeaderStr[col];
    }

    /**
		* Gets the number of columns in the model. This model contains three columns:
	 * signal ID, archive frequency, and archive type.
	 * 
	 * @return The number of columns in the model.
	 */
    @Override
    public int getColumnCount() {
        return HeaderStr.length;
    }

    /**
		* Gets the value for the given cell.
	 * 
	 * @param rowIndex The index of the row for the cell of which to return the value.
	 * @param columnIndex The index of the column for the cell of which to return the value.
	 * @return The value of the given cell.
	 */
    @Override
    public String getValueAt(int rowIndex, int columnIndex) {
        MMRecord record;
        if (_mpsRecords.size() == 0) return "";
        if (rowIndex >= _mpsRecords.size()) return "";
        synchronized (_mpsRecords) {
            record = (MMRecord) _mpsRecords.get(rowIndex);
        }
        switch(columnIndex) {
            case 0:
                return record.getPV();
            case 1:
                return record.getHV();
            case 2:
                return record.getHVrb();
            case 3:
                return record.getHVcur();
            case 4:
                return record.getAFE();
            case 5:
                return record.getPLL();
            case 6:
                return record.getMPSpll();
            case 7:
                return record.getMPSpll600();
            case 8:
                return record.getNomTrip();
            case 9:
                return record.getTuneTrip();
            case 10:
                return record.getUserTrip();
            default:
                return "";
        }
    }

    public String getSVvalue(String pv, int columnIndex) {
        switch(columnIndex) {
            case 1:
                return getHVBiasSv(pv);
            case 2:
                return getHVrbSv(pv);
            case 3:
                return getHVCurRbSv(pv);
            case 4:
                return getAFEsv(pv);
            case 5:
                return getPLLsv(pv);
            case 6:
                return getMPSpllSv(pv);
            case 7:
                return getMPS600pllSv(pv);
            case 8:
                return getNomTripSv(pv);
            case 9:
                return getTuneTripSv(pv);
            case 10:
                return getUserTripSv(pv);
            default:
                return "";
        }
    }

    public Object[][] getRecords() {
        return recs;
    }

    public void addRow(MMRecord mpsRecs) {
        int index = _mpsRecords.size();
        recs[index][0] = mpsRecs.getPV();
        recs[index][1] = mpsRecs.getHV();
        recs[index][2] = mpsRecs.getHVrb();
        recs[index][3] = mpsRecs.getHVcur();
        recs[index][4] = mpsRecs.getAFE();
        recs[index][5] = mpsRecs.getPLL();
        recs[index][6] = mpsRecs.getMPSpll();
        recs[index][7] = mpsRecs.getMPSpll600();
        recs[index][8] = mpsRecs.getNomTrip();
        recs[index][9] = mpsRecs.getTuneTrip();
        recs[index][10] = mpsRecs.getUserTrip();
        synchronized (_mpsRecords) {
            _mpsRecords.add(mpsRecs);
        }
        fireTableDataChanged();
    }

    public MPSWindow getMainWindow() {
        MPSDocument document = new MPSDocument();
        return document.getWindow();
    }

    public void createChannelWrappers(String pv, final int j) {
        final CAValueListener listener = this;
        chName = pv + ":DbgMPSPulseLossLimit";
        MPSpllWrapper[j][0] = new ChannelWrapper(chName);
        _MPSpll.add(MPSpllWrapper[j][0]);
        MPSpllWrapper[j][0].addCAValueListener(listener);
        chName2 = chName + ".HIHI";
        MPSpllWrapper[j][1] = new ChannelWrapper(chName2);
        _MPSpll.add(MPSpllWrapper[j][1]);
        MPSpllWrapper[j][1].addCAValueListener(listener);
        chName3 = chName + ".LOLO";
        MPSpllWrapper[j][2] = new ChannelWrapper(chName3);
        _MPSpll.add(MPSpllWrapper[j][2]);
        MPSpllWrapper[j][2].addCAValueListener(listener);
        chName4 = chName + ".SEVR";
        MPSpllWrapper[j][3] = new ChannelWrapper(chName4);
        _MPSpll.add(MPSpllWrapper[j][3]);
        MPSpllWrapper[j][3].addCAValueListener(listener);
        chName = pv + ":DbgAFEFirstStageGainRb";
        afeWrapper[j][0] = new ChannelWrapper(chName);
        _afe.add(afeWrapper[j][0]);
        afeWrapper[j][0].addCAValueListener(listener);
        chName2 = chName + ".HIHI";
        afeWrapper[j][1] = new ChannelWrapper(chName2);
        _afe.add(afeWrapper[j][1]);
        afeWrapper[j][1].addCAValueListener(listener);
        chName3 = chName + ".LOLO";
        afeWrapper[j][2] = new ChannelWrapper(chName3);
        _afe.add(afeWrapper[j][2]);
        afeWrapper[j][2].addCAValueListener(listener);
        chName4 = chName + ".SEVR";
        afeWrapper[j][3] = new ChannelWrapper(chName4);
        _afe.add(afeWrapper[j][3]);
        afeWrapper[j][3].addCAValueListener(listener);
        chName = pv + ":DbgHVBias";
        hvWrapper[j][0] = new ChannelWrapper(chName);
        _hv.add(hvWrapper[j][0]);
        hvWrapper[j][0].addCAValueListener(listener);
        chName2 = chName + ".DRVL";
        hvWrapper[j][1] = new ChannelWrapper(chName2);
        _hv.add(hvWrapper[j][1]);
        hvWrapper[j][1].addCAValueListener(listener);
        chName3 = chName + ".DRVH";
        hvWrapper[j][2] = new ChannelWrapper(chName3);
        _hv.add(hvWrapper[j][2]);
        hvWrapper[j][2].addCAValueListener(listener);
        chName4 = chName + ".HIHI";
        hvWrapper[j][3] = new ChannelWrapper(chName4);
        _hv.add(hvWrapper[j][3]);
        hvWrapper[j][3].addCAValueListener(listener);
        chName5 = chName + ".LOLO";
        hvWrapper[j][4] = new ChannelWrapper(chName5);
        _hv.add(hvWrapper[j][4]);
        hvWrapper[j][4].addCAValueListener(listener);
        chName6 = chName + ".HIGH";
        hvWrapper[j][5] = new ChannelWrapper(chName6);
        _hv.add(hvWrapper[j][5]);
        hvWrapper[j][5].addCAValueListener(listener);
        chName7 = chName + ".LOW";
        hvWrapper[j][6] = new ChannelWrapper(chName7);
        _hv.add(hvWrapper[j][6]);
        hvWrapper[j][6].addCAValueListener(listener);
        chName8 = chName + ".SEVR";
        hvWrapper[j][7] = new ChannelWrapper(chName8);
        _hv.add(hvWrapper[j][7]);
        hvWrapper[j][7].addCAValueListener(listener);
        chName = pv + ":DbgHVBiasRb";
        hvrbWrapper[j][0] = new ChannelWrapper(chName);
        _hvrb.add(hvrbWrapper[j][0]);
        hvrbWrapper[j][0].addCAValueListener(listener);
        chName2 = chName + ".HIHI";
        hvrbWrapper[j][1] = new ChannelWrapper(chName2);
        _hvrb.add(hvrbWrapper[j][1]);
        hvrbWrapper[j][1].addCAValueListener(listener);
        chName3 = chName + ".LOLO";
        hvrbWrapper[j][2] = new ChannelWrapper(chName3);
        _hvrb.add(hvrbWrapper[j][2]);
        hvrbWrapper[j][2].addCAValueListener(listener);
        chName4 = chName + ".HIGH";
        hvrbWrapper[j][3] = new ChannelWrapper(chName4);
        _hvrb.add(hvrbWrapper[j][3]);
        hvrbWrapper[j][3].addCAValueListener(listener);
        chName5 = chName + ".LOW";
        hvrbWrapper[j][4] = new ChannelWrapper(chName5);
        _hvrb.add(hvrbWrapper[j][4]);
        hvrbWrapper[j][4].addCAValueListener(listener);
        chName6 = chName + ".SEVR";
        hvrbWrapper[j][5] = new ChannelWrapper(chName6);
        _hvrb.add(hvrbWrapper[j][5]);
        hvrbWrapper[j][5].addCAValueListener(listener);
        chName = pv + ":DbgSlowPulseLossRb";
        pllWrapper[j][0] = new ChannelWrapper(chName);
        _pll.add(pllWrapper[j][0]);
        pllWrapper[j][0].addCAValueListener(listener);
        chName2 = chName + ".HIHI";
        pllWrapper[j][1] = new ChannelWrapper(chName2);
        _pll.add(pllWrapper[j][1]);
        pllWrapper[j][1].addCAValueListener(listener);
        chName3 = chName + ".LOLO";
        pllWrapper[j][2] = new ChannelWrapper(chName3);
        _pll.add(pllWrapper[j][2]);
        pllWrapper[j][2].addCAValueListener(listener);
        chName4 = chName + ".HIGH";
        pllWrapper[j][3] = new ChannelWrapper(chName4);
        _pll.add(pllWrapper[j][3]);
        pllWrapper[j][3].addCAValueListener(listener);
        chName5 = chName + ".LOW";
        pllWrapper[j][4] = new ChannelWrapper(chName5);
        _pll.add(pllWrapper[j][4]);
        pllWrapper[j][4].addCAValueListener(listener);
        chName6 = chName + ".SEVR";
        pllWrapper[j][5] = new ChannelWrapper(chName6);
        _pll.add(pllWrapper[j][5]);
        pllWrapper[j][5].addCAValueListener(listener);
        chName = pv + ":nom_trip";
        NomTripWrap[j][0] = new ChannelWrapper(chName);
        _NomTrip.add(NomTripWrap[j][0]);
        NomTripWrap[j][0].addCAValueListener(listener);
        chName2 = chName + ".DRVL";
        NomTripWrap[j][1] = new ChannelWrapper(chName2);
        _NomTrip.add(NomTripWrap[j][1]);
        NomTripWrap[j][1].addCAValueListener(listener);
        chName3 = chName + ".DRVH";
        NomTripWrap[j][2] = new ChannelWrapper(chName3);
        _NomTrip.add(NomTripWrap[j][2]);
        NomTripWrap[j][2].addCAValueListener(listener);
        chName4 = chName + ".SEVR";
        NomTripWrap[j][3] = new ChannelWrapper(chName4);
        _NomTrip.add(NomTripWrap[j][3]);
        NomTripWrap[j][3].addCAValueListener(listener);
        chName = pv + ":tune_trip";
        TuneTripWrap[j][0] = new ChannelWrapper(chName);
        _TuneTrip.add(TuneTripWrap[j][0]);
        TuneTripWrap[j][0].addCAValueListener(listener);
        chName2 = chName + ".DRVL";
        TuneTripWrap[j][1] = new ChannelWrapper(chName2);
        _TuneTrip.add(TuneTripWrap[j][1]);
        TuneTripWrap[j][1].addCAValueListener(listener);
        chName3 = chName + ".DRVH";
        TuneTripWrap[j][2] = new ChannelWrapper(chName3);
        _TuneTrip.add(TuneTripWrap[j][2]);
        TuneTripWrap[j][2].addCAValueListener(listener);
        chName4 = chName + ".SEVR";
        TuneTripWrap[j][3] = new ChannelWrapper(chName4);
        _TuneTrip.add(TuneTripWrap[j][3]);
        TuneTripWrap[j][3].addCAValueListener(listener);
        chName = pv + ":user_trip";
        UserTripWrap[j][0] = new ChannelWrapper(chName);
        _UserTrip.add(UserTripWrap[j][0]);
        UserTripWrap[j][0].addCAValueListener(listener);
        chName2 = chName + ".DRVL";
        UserTripWrap[j][1] = new ChannelWrapper(chName2);
        _UserTrip.add(UserTripWrap[j][1]);
        UserTripWrap[j][1].addCAValueListener(listener);
        chName3 = chName + ".DRVH";
        UserTripWrap[j][2] = new ChannelWrapper(chName3);
        _UserTrip.add(UserTripWrap[j][2]);
        UserTripWrap[j][2].addCAValueListener(listener);
        chName4 = chName + ".SEVR";
        UserTripWrap[j][3] = new ChannelWrapper(chName4);
        _UserTrip.add(UserTripWrap[j][3]);
        UserTripWrap[j][3].addCAValueListener(listener);
        chName = pv + ":DbgHVCurrentRb";
        hvcurWrapper[j][0] = new ChannelWrapper(chName);
        _hvcur.add(hvcurWrapper[j][0]);
        hvcurWrapper[j][0].addCAValueListener(listener);
        chName2 = chName + ".HIHI";
        hvcurWrapper[j][1] = new ChannelWrapper(chName2);
        _hvcur.add(hvcurWrapper[j][1]);
        hvcurWrapper[j][1].addCAValueListener(listener);
        chName3 = chName + ".LOLO";
        hvcurWrapper[j][2] = new ChannelWrapper(chName3);
        _hvcur.add(hvcurWrapper[j][2]);
        hvcurWrapper[j][2].addCAValueListener(listener);
        chName4 = chName + ".HIGH";
        hvcurWrapper[j][3] = new ChannelWrapper(chName4);
        _hvcur.add(hvcurWrapper[j][3]);
        hvcurWrapper[j][3].addCAValueListener(listener);
        chName5 = chName + ".LOW";
        hvcurWrapper[j][4] = new ChannelWrapper(chName5);
        _hvcur.add(hvcurWrapper[j][4]);
        hvcurWrapper[j][4].addCAValueListener(listener);
        chName6 = chName + ".SEVR";
        hvcurWrapper[j][5] = new ChannelWrapper(chName6);
        _hvcur.add(hvcurWrapper[j][5]);
        hvcurWrapper[j][5].addCAValueListener(listener);
        chName = pv + ":DbgMPS600PulsesLossLimit";
        MPSpll600Wrapper[j][0] = new ChannelWrapper(chName);
        _MPSpll600.add(MPSpll600Wrapper[j][0]);
        MPSpll600Wrapper[j][0].addCAValueListener(listener);
        chName2 = chName + ".HIHI";
        MPSpll600Wrapper[j][1] = new ChannelWrapper(chName2);
        _MPSpll600.add(MPSpll600Wrapper[j][1]);
        MPSpll600Wrapper[j][1].addCAValueListener(listener);
        chName3 = chName + ".LOLO";
        MPSpll600Wrapper[j][2] = new ChannelWrapper(chName3);
        _MPSpll600.add(MPSpll600Wrapper[j][2]);
        MPSpll600Wrapper[j][2].addCAValueListener(listener);
        chName4 = chName + ".SEVR";
        MPSpll600Wrapper[j][3] = new ChannelWrapper(chName4);
        _MPSpll600.add(MPSpll600Wrapper[j][3]);
        MPSpll600Wrapper[j][3].addCAValueListener(listener);
    }

    public void removeChannelWrappers(Map pvPartList) {
        final CAValueListener listener = this;
        Iterator keyValue = pvPartList.keySet().iterator();
        int j = 0;
        while (keyValue.hasNext()) {
            Object key = keyValue.next();
            _pv = key.toString();
            MPSpllWrapper[j][0].removeCAValueListener(listener);
            MPSpllWrapper[j][1].removeCAValueListener(listener);
            MPSpllWrapper[j][2].removeCAValueListener(listener);
            MPSpllWrapper[j][3].removeCAValueListener(listener);
            afeWrapper[j][0].removeCAValueListener(listener);
            afeWrapper[j][1].removeCAValueListener(listener);
            afeWrapper[j][2].removeCAValueListener(listener);
            afeWrapper[j][3].removeCAValueListener(listener);
            hvWrapper[j][0].removeCAValueListener(listener);
            hvWrapper[j][1].removeCAValueListener(listener);
            hvWrapper[j][2].removeCAValueListener(listener);
            hvWrapper[j][3].removeCAValueListener(listener);
            hvWrapper[j][4].removeCAValueListener(listener);
            hvWrapper[j][5].removeCAValueListener(listener);
            hvWrapper[j][6].removeCAValueListener(listener);
            hvWrapper[j][7].removeCAValueListener(listener);
            hvrbWrapper[j][0].removeCAValueListener(listener);
            hvrbWrapper[j][1].removeCAValueListener(listener);
            hvrbWrapper[j][2].removeCAValueListener(listener);
            hvrbWrapper[j][3].removeCAValueListener(listener);
            hvrbWrapper[j][4].removeCAValueListener(listener);
            hvrbWrapper[j][5].removeCAValueListener(listener);
            pllWrapper[j][0].removeCAValueListener(listener);
            pllWrapper[j][1].removeCAValueListener(listener);
            pllWrapper[j][2].removeCAValueListener(listener);
            pllWrapper[j][3].removeCAValueListener(listener);
            pllWrapper[j][4].removeCAValueListener(listener);
            pllWrapper[j][5].removeCAValueListener(listener);
            NomTripWrap[j][0].removeCAValueListener(listener);
            NomTripWrap[j][1].removeCAValueListener(listener);
            NomTripWrap[j][2].removeCAValueListener(listener);
            NomTripWrap[j][3].removeCAValueListener(listener);
            TuneTripWrap[j][0].removeCAValueListener(listener);
            TuneTripWrap[j][1].removeCAValueListener(listener);
            TuneTripWrap[j][2].removeCAValueListener(listener);
            TuneTripWrap[j][3].removeCAValueListener(listener);
            UserTripWrap[j][0].removeCAValueListener(listener);
            UserTripWrap[j][1].removeCAValueListener(listener);
            UserTripWrap[j][2].removeCAValueListener(listener);
            UserTripWrap[j][3].removeCAValueListener(listener);
            hvcurWrapper[j][0].removeCAValueListener(listener);
            hvcurWrapper[j][1].removeCAValueListener(listener);
            hvcurWrapper[j][2].removeCAValueListener(listener);
            hvcurWrapper[j][3].removeCAValueListener(listener);
            hvcurWrapper[j][4].removeCAValueListener(listener);
            hvcurWrapper[j][5].removeCAValueListener(listener);
            MPSpll600Wrapper[j][0].removeCAValueListener(listener);
            MPSpll600Wrapper[j][1].removeCAValueListener(listener);
            MPSpll600Wrapper[j][2].removeCAValueListener(listener);
            MPSpll600Wrapper[j][3].removeCAValueListener(listener);
            j++;
        }
    }

    public void setDelay(int seconds) {
        delay = seconds * 1000;
    }

    public String getAlarmVal(float pvVal, int alrm) {
        String value = "" + pvVal;
        if (alrm == 1) value = "<html><body><font COLOR=#ffff00>" + pvVal + "</font></body></html>"; else if (alrm == 2) value = "<html><body><font COLOR=#ff0000>" + pvVal + "</font></body></html>"; else if (alrm == 3) value = "<html><body><font COLOR=#ffffff>" + pvVal + "</font></body></html>";
        return value;
    }

    public String iterVal(Iterator CWiter, String pv, int showColor) {
        String val = "?";
        Iterator cw = CWiter;
        ChannelWrapper svWrap = null;
        try {
            while (CWiter.hasNext()) {
                wrapper = (ChannelWrapper) CWiter.next();
                if (wrapper.getName().compareTo(pv) == 0) break;
            }
            if (wrapper == null) {
                System.out.println(wrapper.getName() + "is null");
                return "-1";
            }
            if (wrapper.getName().compareTo(pv) == 0) {
                if (pv.indexOf(".SEVR") == -1 && showColor == 1) {
                    if (pv.indexOf('.') == -1) {
                        String svPV = pv + ".SEVR";
                        while (cw.hasNext()) {
                            svWrap = (ChannelWrapper) cw.next();
                            if (svWrap.getName().compareTo(svPV) == 0) break;
                        }
                        if (svWrap.getName().compareTo(svPV) == 0) val = getAlarmVal(wrapper.getFloatValue(), svWrap.getValue()); else val = "" + wrapper.getFloatValue();
                    } else val = "" + wrapper.getFloatValue();
                } else if (pv.indexOf(".SEVR") > -1) val = "" + wrapper.getValue(); else val = "" + wrapper.getFloatValue();
            }
        } catch (ConcurrentModificationException ignored) {
        }
        return val;
    }

    public String getHVBias(String pv) {
        String hvPv = pv + ":DbgHVBias";
        return iterVal(_hv.iterator(), hvPv, 1);
    }

    public String getHVhvlo(String pv) {
        String hvPv = pv;
        return iterVal(_hv.iterator(), hvPv, 0);
    }

    public String getHVhvhi(String pv) {
        String hvPv = pv;
        return iterVal(_hv.iterator(), hvPv, 0);
    }

    public String getHVlolo(String pv) {
        String hvPv = pv;
        return iterVal(_hv.iterator(), hvPv, 0);
    }

    public String getHVhihi(String pv) {
        String hvPv = pv;
        return iterVal(_hv.iterator(), hvPv, 0);
    }

    public String getHVlow(String pv) {
        String hvPv = pv;
        return iterVal(_hv.iterator(), hvPv, 0);
    }

    public String getHVhigh(String pv) {
        String hvPv = pv;
        return iterVal(_hv.iterator(), hvPv, 0);
    }

    public String getHVBiasSv(String pv) {
        String hvPv = pv + ".SEVR";
        return iterVal(_hv.iterator(), hvPv, 0);
    }

    public String getHVrb(String pv) {
        String hvPv = pv + ":DbgHVBiasRb";
        return iterVal(_hvrb.iterator(), hvPv, 1);
    }

    public String getHVrbHIHI(String pv) {
        String hvPv = pv;
        return iterVal(_hvrb.iterator(), hvPv, 0);
    }

    public String getHVrbHIGH(String pv) {
        String hvPv = pv;
        return iterVal(_hvrb.iterator(), hvPv, 0);
    }

    public String getHVrbLOLO(String pv) {
        String hvPv = pv;
        return iterVal(_hvrb.iterator(), hvPv, 0);
    }

    public String getHVrbLOW(String pv) {
        String hvPv = pv;
        return iterVal(_hvrb.iterator(), hvPv, 0);
    }

    public String getHVrbSv(String pv) {
        String hvPv = pv + ".SEVR";
        return iterVal(_hvrb.iterator(), hvPv, 0);
    }

    public String getHVCurRb(String pv) {
        String hvPv = pv + ":DbgHVCurrentRb";
        return iterVal(_hvcur.iterator(), hvPv, 1);
    }

    public String getHVCurHIGH(String pv) {
        String hvPv = pv;
        return iterVal(_hvcur.iterator(), hvPv, 0);
    }

    public String getHVCurHIHI(String pv) {
        String hvPv = pv;
        return iterVal(_hvcur.iterator(), hvPv, 0);
    }

    public String getHVCurLOW(String pv) {
        String hvPv = pv;
        return iterVal(_hvcur.iterator(), hvPv, 0);
    }

    public String getHVCurLOLO(String pv) {
        String hvPv = pv;
        return iterVal(_hvcur.iterator(), hvPv, 0);
    }

    public String getHVCurRbSv(String pv) {
        String hvPv = pv + ".SEVR";
        return iterVal(_hvcur.iterator(), hvPv, 0);
    }

    public String getAFE(String pv) {
        String hvPv = pv + ":DbgAFEFirstStageGainRb";
        return iterVal(_afe.iterator(), hvPv, 1);
    }

    public String getAFElgain(String pv) {
        String hvPv = pv;
        return iterVal(_afe.iterator(), hvPv, 0);
    }

    public String getAFEhgain(String pv) {
        String hvPv = pv;
        return iterVal(_afe.iterator(), hvPv, 0);
    }

    public String getAFEsv(String pv) {
        String hvPv = pv + ".SEVR";
        return iterVal(_afe.iterator(), hvPv, 0);
    }

    public String getPLL(String pv) {
        String hvPv = pv + ":DbgSlowPulseLossRb";
        return iterVal(_pll.iterator(), hvPv, 1);
    }

    public String getPllLOW(String pv) {
        String hvPv = pv;
        return iterVal(_pll.iterator(), hvPv, 0);
    }

    public String getPllHIGH(String pv) {
        String hvPv = pv;
        return iterVal(_pll.iterator(), hvPv, 0);
    }

    public String getPllLOLO(String pv) {
        String hvPv = pv;
        return iterVal(_pll.iterator(), hvPv, 0);
    }

    public String getPllHIHI(String pv) {
        String hvPv = pv;
        return iterVal(_pll.iterator(), hvPv, 0);
    }

    public String getPLLsv(String pv) {
        String hvPv = pv + ".SEVR";
        return iterVal(_pll.iterator(), hvPv, 0);
    }

    public String getMPSpll(String pv) {
        String hvPv = pv + ":DbgMPSPulseLossLimit";
        return iterVal(_MPSpll.iterator(), hvPv, 1);
    }

    public String getMPSpllHIHI(String pv) {
        String hvPv = pv;
        return iterVal(_MPSpll.iterator(), hvPv, 0);
    }

    public String getMPSpllLOLO(String pv) {
        String hvPv = pv;
        return iterVal(_MPSpll.iterator(), hvPv, 0);
    }

    public String getMPSpllSv(String pv) {
        String hvPv = pv + ".SEVR";
        return iterVal(_MPSpll.iterator(), hvPv, 0);
    }

    public String getMPS600pll(String pv) {
        String hvPv = pv + ":DbgMPS600PulsesLossLimit";
        return iterVal(_MPSpll600.iterator(), hvPv, 1);
    }

    public String getMPS600pllLOLO(String pv) {
        String hvPv = pv;
        return iterVal(_MPSpll600.iterator(), hvPv, 0);
    }

    public String getMPS600pllHIHI(String pv) {
        String hvPv = pv;
        return iterVal(_MPSpll600.iterator(), hvPv, 0);
    }

    public String getMPS600pllSv(String pv) {
        String hvPv = pv + ".SEVR";
        return iterVal(_MPSpll600.iterator(), hvPv, 0);
    }

    public String getNomTrip(String pv) {
        String hvPv = pv + ":nom_trip";
        return iterVal(_NomTrip.iterator(), hvPv, 1);
    }

    public String getNomTripHI(String pv) {
        String hvPv = pv;
        return iterVal(_NomTrip.iterator(), hvPv, 0);
    }

    public String getNomTripLO(String pv) {
        String hvPv = pv;
        return iterVal(_NomTrip.iterator(), hvPv, 0);
    }

    public String getNomTripSv(String pv) {
        String hvPv = pv + ".SEVR";
        return iterVal(_NomTrip.iterator(), hvPv, 0);
    }

    public String getTuneTrip(String pv) {
        String hvPv = pv + ":tune_trip";
        return iterVal(_TuneTrip.iterator(), hvPv, 1);
    }

    public String getTuneTripHI(String pv) {
        String hvPv = pv;
        return iterVal(_TuneTrip.iterator(), hvPv, 0);
    }

    public String getTuneTripLO(String pv) {
        String hvPv = pv;
        return iterVal(_TuneTrip.iterator(), hvPv, 0);
    }

    public String getTuneTripSv(String pv) {
        String hvPv = pv + ".SEVR";
        return iterVal(_TuneTrip.iterator(), hvPv, 0);
    }

    public String getUserTrip(String pv) {
        String hvPv = pv + ":user_trip";
        return iterVal(_UserTrip.iterator(), hvPv, 1);
    }

    public String getUserTripLO(String pv) {
        String hvPv = pv;
        return iterVal(_UserTrip.iterator(), hvPv, 0);
    }

    public String getUserTripHI(String pv) {
        String hvPv = pv;
        return iterVal(_UserTrip.iterator(), hvPv, 0);
    }

    public String getUserTripSv(String pv) {
        String hvPv = pv + ".SEVR";
        return iterVal(_UserTrip.iterator(), hvPv, 0);
    }

    public String getPVvalue(String pvName, int showColor) {
        if (pvName.indexOf(":DbgMPSPulseLossLimit") != -1) {
            return iterVal(_MPSpll.iterator(), pvName, showColor);
        } else if (pvName.indexOf(":DbgMPS600PulsesLossLimit") != -1) {
            return iterVal(_MPSpll600.iterator(), pvName, showColor);
        } else if (pvName.indexOf(":DbgHVBiasRb") != -1) {
            return iterVal(_hvrb.iterator(), pvName, showColor);
        } else if (pvName.indexOf(":DbgHVBias") != -1) {
            return iterVal(_hv.iterator(), pvName, showColor);
        } else if (pvName.indexOf(":DbgHVCurrentRb") != -1) {
            return iterVal(_hvcur.iterator(), pvName, showColor);
        } else if (pvName.indexOf(":DbgAFEFirstStageGainRb") != -1) {
            return iterVal(_afe.iterator(), pvName, showColor);
        } else if (pvName.indexOf(":DbgSlowPulseLossRb") != -1) {
            return iterVal(_pll.iterator(), pvName, showColor);
        } else if (pvName.indexOf(":nom_trip") != -1) {
            return iterVal(_NomTrip.iterator(), pvName, showColor);
        } else if (pvName.indexOf(":tune_trip") != -1) {
            return iterVal(_TuneTrip.iterator(), pvName, showColor);
        } else if (pvName.indexOf(":user_trip") != -1) {
            return iterVal(_UserTrip.iterator(), pvName, showColor);
        }
        return "?";
    }

    /** Indicates a new channel access value has been found for this wrapped channel. */
    public void newValue(final ChannelWrapper wrapper, int avalue) {
        String pvName = wrapper.getName();
        JTable mpsTable = getMainWindow().getMPSmmTable();
        if (mpsTable == null) return;
        int len = mpsTable.getRowCount();
        int last = pvName.lastIndexOf(':');
        int j = 0;
        String SignalName = pvName.substring(0, last);
        int i;
        for (i = 0; i < len; i++) {
            if (mpsTable.getValueAt(i, 0) == null) return;
            if (SignalName.equals(mpsTable.getValueAt(i, 0))) break;
        }
        if (i >= len) return;
        String value = "";
        int dot = pvName.lastIndexOf('.');
        String colHdr = "";
        if (dot > -1 && pvName.indexOf("SEVR") == -1) return;
        if (dot > -1) colHdr = pvName.substring(last + 1, dot); else colHdr = pvName.substring(last + 1);
        int numCol = mpsTable.getColumnCount();
        for (j = numCol - 1; j >= 0; j--) {
            if (colHdr.equals(HeaderStr[j])) break;
        }
        if (j < 0) return;
        int alrm = -1;
        String pvStr = "";
        String fval = "";
        if (pvName.indexOf("SEVR") > -1 && wrapper.getValue() <= 0) return;
        if (pvName.indexOf("SEVR") > -1 && wrapper.getValue() > 0) {
            alrm = wrapper.getValue();
            pvStr = SignalName + ":" + colHdr;
            fval = getPVvalue(pvStr, 1);
        } else {
            pvStr = pvName + ".SEVR";
            String sv = getSVvalue(pvName, j);
            try {
                alrm = Integer.parseInt(sv);
            } catch (NumberFormatException e) {
            }
            fval = "" + wrapper.getFloatValue();
        }
        value = "?";
        if (alrm == 1) value = "<html><body><font COLOR=#ffff00>" + fval + "</font></body></html>"; else if (alrm == 2) value = "<html><body><font COLOR=#ff0000>" + fval + "</font></body></html>"; else if (alrm == 3) value = "<html><body><font COLOR=#ffffff>" + fval + "</font></body></html>"; else value = "" + wrapper.getFloatValue();
        setValueAt(value, i, j);
        newValueAt(value, i, j);
        mpsTable.setValueAt(value, i, j);
        mpsTable.validate();
        fireTableCellUpdated(i, j);
        fireTableDataChanged();
        wrapper.getChannel().flushIO();
    }

    public synchronized void newValue(final ChannelWrapper wrapper) {
        if (wrapper == null) return;
        String pvName = wrapper.getName();
        int dot = pvName.lastIndexOf('.');
        if (dot > -1) return;
        JTable mpsTable = getMainWindow().getMPSmmTable();
        if (mpsTable == null) return;
        int len = mpsTable.getRowCount();
        if (len <= 0) return;
        int last = pvName.lastIndexOf(':');
        String SignalName = pvName.substring(0, last);
        String value = "" + wrapper.getFloatValue();
        String ivalue = "" + wrapper.getValue();
        int i;
        for (i = 0; i < len; i++) {
            if (mpsTable.getValueAt(i, 0) == null) return;
            if (SignalName.equals(mpsTable.getValueAt(i, 0))) break;
        }
        if (i >= len) return;
        String hdr = pvName.substring(last + 1);
        int numCol = mpsTable.getColumnCount();
        int j;
        for (j = numCol - 1; j >= 0; j--) {
            if (hdr.equals(HeaderStr[j])) break;
        }
        if (j < 0) return;
        setValueAt(value, i, j);
        if (mmTable != null) {
            mmTable.setValueAt(value, i, j);
        }
        mpsTable.setValueAt(value, i, j);
        mpsTable.validate();
        fireTableDataChanged();
        wrapper.getChannel().flushIO();
    }

    public synchronized void refreshTable() {
        for (int j = 0; j < _mpsRecords.size(); j++) {
            newValue(hvWrapper[j][0]);
            newValue(MPSpllWrapper[j][0]);
            newValue(MPSpll600Wrapper[j][0]);
            newValue(afeWrapper[j][0]);
            newValue(hvrbWrapper[j][0]);
            newValue(pllWrapper[j][0]);
            newValue(NomTripWrap[j][0]);
            newValue(TuneTripWrap[j][0]);
            newValue(UserTripWrap[j][0]);
            newValue(hvcurWrapper[j][0]);
        }
        fireTableDataChanged();
    }

    /** Indicates a new channel access value has been found for this wrapped channel. */
    public void newValue(TableModel mmTable, int row, int column, Object value) {
        mmTable.setValueAt(value, row, column);
        fireTableDataChanged();
    }
}
