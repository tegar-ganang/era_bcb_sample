package gov.sns.apps.sclmonitor;

import java.util.*;
import gov.sns.tools.pvlogger.*;
import gov.sns.tools.database.*;

public class SCLCavPVLog {

    MachineSnapshot mss;

    ChannelSnapshot[] css;

    /**
	 * @param id
	 *            the PV logger ID
	 */
    public SCLCavPVLog(long id) {
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
	 * Method getCavMap. The method returns a HashMap with HOM IDs as the keys
	 * and HOM HB0/HB1 data as the value.
	 * 
	 * @return HOM data
	 */
    public HashMap<String, double[][]> getCavMap() {
        HashMap<String, double[][]> pvMap = new HashMap<String, double[][]>();
        ChannelSnapshot[] css = mss.getChannelSnapshots();
        for (int i = 0; i < css.length; i++) {
            double[] hb0Data, hb1Data;
            if (css[i].getPV().indexOf("HB0") > -1) {
                String hpmId = css[i].getPV().substring(0, 15);
                hb0Data = css[i].getValue();
                double[][] data = new double[2][hb0Data.length];
                if (!pvMap.containsKey(hpmId)) {
                    data[0] = hb0Data;
                    pvMap.put(hpmId, data);
                } else {
                    System.arraycopy(hb0Data, 0, pvMap.get(hpmId)[0], 0, hb0Data.length);
                }
            }
            if (css[i].getPV().indexOf("HB1") > -1) {
                String hpmId = css[i].getPV().substring(0, 15);
                hb1Data = css[i].getValue();
                double[][] data = new double[2][hb1Data.length];
                if (!pvMap.containsKey(hpmId)) {
                    data[1] = hb1Data;
                    pvMap.put(hpmId, data);
                } else {
                    System.arraycopy(hb1Data, 0, pvMap.get(hpmId)[1], 0, hb1Data.length);
                }
            }
        }
        System.out.println("Got " + pvMap.size() + " Cavities.");
        return pvMap;
    }

    public HashMap<String, Double> getCavMap1() {
        HashMap<String, Double> pvMap = new HashMap<String, Double>();
        ChannelSnapshot[] css = mss.getChannelSnapshots();
        for (int i = 0; i < css.length; i++) {
            double rep;
            if (css[i].getPV().indexOf("EventSelect") > -1) {
                String hpmId = css[i].getPV().substring(0, 16);
                rep = css[i].getValue()[0];
                if (!pvMap.containsKey(hpmId)) {
                    pvMap.put(hpmId, new Double(rep));
                }
            }
        }
        return pvMap;
    }

    public HashMap<String, Double> getCavMap2() {
        HashMap<String, Double> pvMap = new HashMap<String, Double>();
        ChannelSnapshot[] css = mss.getChannelSnapshots();
        for (int i = 0; i < css.length; i++) {
            double rep;
            if (css[i].getPV().indexOf("cavV") > -1) {
                String hpmId = css[i].getPV().substring(0, 15);
                rep = css[i].getValue()[0];
                if (!pvMap.containsKey(hpmId)) {
                    pvMap.put(hpmId, new Double(rep));
                }
            }
        }
        return pvMap;
    }
}
