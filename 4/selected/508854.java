package gov.sns.apps.ringmeasurement;

import java.util.*;
import gov.sns.tools.pvlogger.*;
import gov.sns.tools.database.*;

public class RingBPMTBTPVLog {

    MachineSnapshot mss;

    HashMap qPVMap;

    HashMap qPSPVMap;

    ChannelSnapshot[] css;

    /**
	 * @param id  the PV logger ID
	 */
    public RingBPMTBTPVLog(long id) {
        ConnectionDictionary dict = ConnectionDictionary.defaultDictionary();
        SqlStateStore store;
        if (dict != null) {
            store = new SqlStateStore(dict);
        } else {
            ConnectionPreferenceController.displayPathPreferenceSelector();
            dict = ConnectionDictionary.defaultDictionary();
            store = new SqlStateStore(dict);
        }
        mss = store.fetchMachineSnapshot(id);
        css = mss.getChannelSnapshots();
    }

    /**
	 * 
	 * Method getBPMMap.  The method returns a HashMap with BPM IDs as the keys
	 * and BPM turn-by-turn data as the value (2-d Double array [2][TBT_data_size], 
	 * [0][] is for xTBT and [1][] is for yTBT.
	 * 
	 * @return BPM TBT data
	 */
    public HashMap<String, double[][]> getBPMMap() {
        HashMap<String, double[][]> pvMap = new HashMap<String, double[][]>();
        ChannelSnapshot[] css = mss.getChannelSnapshots();
        for (int i = 0; i < css.length; i++) {
            double[] xdata, ydata;
            if (css[i].getPV().indexOf("xTBT") > -1) {
                String BPMId = css[i].getPV().substring(0, 17);
                xdata = css[i].getValue();
                double[][] data = new double[2][xdata.length];
                if (!pvMap.containsKey(BPMId)) {
                    data[0] = xdata;
                    pvMap.put(BPMId, data);
                } else {
                    System.arraycopy(xdata, 0, pvMap.get(BPMId)[0], 0, xdata.length);
                }
            }
            if (css[i].getPV().indexOf("yTBT") > -1) {
                String BPMId = css[i].getPV().substring(0, 17);
                ydata = css[i].getValue();
                double[][] data = new double[2][ydata.length];
                if (!pvMap.containsKey(BPMId)) {
                    data[1] = ydata;
                    pvMap.put(BPMId, data);
                } else {
                    System.arraycopy(ydata, 0, pvMap.get(BPMId)[1], 0, ydata.length);
                }
            }
        }
        System.out.println("Got " + pvMap.size() + " BPMs.");
        return pvMap;
    }
}
