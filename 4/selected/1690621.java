package gov.sns.apps.energymaster;

import gov.sns.ca.Channel;
import gov.sns.ca.ChannelFactory;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * 
 * @author sako
 * This class manages one FCT monitor. This monitors signal and related records, and identify 
 * it is used for RF scanning, and identify if the signal range is out of range and a phase switching
 * is necessary. It has two sets of FCT calibration parameters of phase value calculation
 * for 0-degree mode and 180-degree mode. It calculates phase when it is ordered by a FCTPairManager
 */
public class FCTManager {

    private long timeOutmsec = 1000;

    private String FCTId;

    public String getId() {
        return FCTId;
    }

    private int phaseSwitchLast;

    private double voltLast;

    private double phaseLast;

    private boolean isScanned;

    private boolean isPhaseSwitchGood;

    private CaUpdateCounter phaseSwitchRB;

    private CaUpdateCounter voltMon;

    private static double timeRangePhaseSwitchRB;

    private static final double timeRangeVoltMon = 10;

    private Channel phaseSwitchSet;

    private static int phaseSwitchScanned;

    private static final double voltHighThreshold = 4.5;

    private static final double voltLowThreshold = 0.5;

    private static HashMap<String, CaUpdateCounter> monitorPoolPhaseSwitch;

    private static HashMap<String, CaUpdateCounter> monitorPoolFCTVolt;

    private static EnergyMasterProperties prop;

    static {
        prop = new EnergyMasterProperties();
        monitorPoolPhaseSwitch = new HashMap<String, CaUpdateCounter>();
        monitorPoolFCTVolt = new HashMap<String, CaUpdateCounter>();
    }

    public FCTManager(HashMap<String, String> FCTCalibData) {
        calib = new FCTCalib(FCTCalibData);
        FCTId = calib.getShortId();
        phaseSwitchLast = 0;
        isScanned = false;
        isPhaseSwitchGood = true;
        phaseSwitchSet = null;
        phaseSwitchScanned = prop.getFCTPhaseSwitchScanThreshold();
        timeRangePhaseSwitchRB = prop.getFCTScanCheckRange();
        setupRecords();
    }

    private FCTCalib calib;

    public double getPosition() {
        if (calib == null) {
            return 0;
        }
        return calib.getPositionS();
    }

    public boolean getIsScanned() {
        return isScanned;
    }

    public boolean getIsPhaseSwitchGood() {
        return isPhaseSwitchGood;
    }

    public void swapPhaseSwitch() throws Exception {
        int currentSwitch = 0;
        if (!Double.isNaN(phaseSwitchRB.getValue())) {
            currentSwitch = (int) (phaseSwitchRB.getValue());
            if (phaseSwitchSet == null) {
                phaseSwitchSet = ChannelFactory.defaultFactory().getChannel(calib.getPhaseSwitchSetRecord());
            }
            int swapSwitch = (currentSwitch == 0) ? 1 : 0;
            System.out.println("swapping phaseswitch rec " + calib.getPhaseSwitchSetRecord());
            System.out.println("for FCT = " + FCTId);
            CaMonitorScalar.setChannel(phaseSwitchSet, swapSwitch);
        }
    }

    public boolean checkScanned() {
        int phaseSwitchVaried = phaseSwitchRB.getUpdateNumber();
        if (phaseSwitchVaried >= phaseSwitchScanned) {
            isScanned = true;
        } else {
            isScanned = false;
        }
        return isScanned;
    }

    public boolean checkVolt() {
        ArrayList<Double> buffer = voltMon.getVals();
        int lowCount = 0;
        int highCount = 0;
        for (int ib = 0; ib < buffer.size(); ib++) {
            double volt = buffer.get(ib).doubleValue();
            if (volt < voltLowThreshold) {
                lowCount++;
            }
            if (volt > voltHighThreshold) {
                highCount++;
            }
        }
        if ((lowCount > 0) && (highCount > 0)) {
            System.out.println("voltMon = " + voltMon.getChannelName());
            System.out.println("lowCount, highCount = " + lowCount + " " + highCount);
            isPhaseSwitchGood = false;
        } else {
            isPhaseSwitchGood = true;
        }
        return isPhaseSwitchGood;
    }

    public boolean adjustSwitchIfNecessary() {
        checkVolt();
        if (!isPhaseSwitchGood) {
            System.out.println("isPhaseSwitchGood = false, swapping phaseSwitch");
            try {
                swapPhaseSwitch();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    public double calcPhase() {
        if (!Double.isNaN(voltMon.getValue())) {
            voltLast = voltMon.getValue();
        } else {
            phaseLast = 0;
            return phaseLast;
        }
        if (!Double.isNaN(phaseSwitchRB.getValue())) {
            phaseSwitchLast = (int) phaseSwitchRB.getValue();
        } else {
            phaseLast = 0;
            return phaseLast;
        }
        phaseLast = calib.calcPhase(voltLast, phaseSwitchLast);
        return phaseLast;
    }

    private void setupRecords() {
        String phaseSwitchRBRec = calib.getPhaseSwitchRBRecord();
        phaseSwitchRB = monitorPoolPhaseSwitch.get(phaseSwitchRBRec);
        if (phaseSwitchRB == null) {
            phaseSwitchRB = new CaUpdateCounter(calib.getPhaseSwitchRBRecord(), timeRangePhaseSwitchRB);
            monitorPoolPhaseSwitch.put(phaseSwitchRBRec, phaseSwitchRB);
        }
        String voltMonRec = calib.getVoltRecord();
        voltMon = monitorPoolFCTVolt.get(voltMonRec);
        if (voltMon == null) {
            voltMon = new CaUpdateCounter(calib.getVoltRecord(), timeRangeVoltMon);
            monitorPoolFCTVolt.put(voltMonRec, voltMon);
        }
    }
}
