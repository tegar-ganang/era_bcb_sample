package gov.sns.tools.pvlogger.query;

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
        try {
            PVLogger pvLogger = null;
            final ConnectionDictionary defaultDictionary = PVLogger.newBrowsingConnectionDictionary();
            if (defaultDictionary != null && defaultDictionary.hasRequiredInfo()) {
                pvLogger = new PVLogger(defaultDictionary);
            } else {
                ConnectionPreferenceController.displayPathPreferenceSelector();
                final ConnectionDictionary dictionary = PVLogger.newBrowsingConnectionDictionary();
                if (dictionary != null && dictionary.hasRequiredInfo()) {
                    pvLogger = new PVLogger(dictionary);
                }
            }
            if (pvLogger != null) {
                mss = pvLogger.fetchMachineSnapshot(id);
                css = mss.getChannelSnapshots();
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
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
