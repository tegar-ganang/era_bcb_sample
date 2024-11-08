package gov.sns.apps.energymaster;

import gov.sns.ca.Channel;
import gov.sns.ca.ChannelFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class EnergyMeasurer {

    private double eneBest = 0;

    public static final int noBeam = -1;

    public static final int beamToNOWHERE = 0;

    public static final int beamToLEBT = 1;

    public static final int beamToMEBT1 = 2;

    public static final int beamTo0Dump = 3;

    public static final int beamTo30Dump = 4;

    public static final int beamTo100Dump = 9;

    public static final int beamTo90Dump = 10;

    public static final int beamToH0Dump = 11;

    public static final int beamTo3NDumpDC = 12;

    public static final int beamTo3NDumpAC = 13;

    public static final int beamToMLFTarget = 17;

    public static final int beamToMRInjDump = 33;

    public static final int beamToMRExtDump = 34;

    public static final int beamToKTarget = 65;

    public static final int beamToNTarget = 66;

    public static final int beamToKNTarget = 67;

    public static final int beamToMLF_MRInj = 49;

    public static final int beamToMLF_MRExt = 50;

    public static final int beamToMLF_K = 81;

    public static final int beamToMLF_N = 82;

    public static final int beamToMLF_KN = 83;

    public static final String beamModeNOWHERE = "IS";

    public static final String beamModeLEBT = "LEBT";

    public static final String beamModeMEBT1 = "MEBT1";

    public static final String beamMode0Dump = "0-dump";

    public static final String beamMode30Dump = "30-dump";

    public static final String beamMode90Dump = "90-dump";

    public static final String beamMode100Dump = "100-dump";

    public static final String beamModeH0Dump = "H0-dump";

    public static final String beamMode3NDumpDC = "3N-dump-DC";

    public static final String beamMode3NDumpAC = "3N-dump-AC";

    public static final String beamModeMLFTarget = "MLF-target";

    public static final String beamModeNOBEAM = "NO BEAM";

    public static final String beamModeMRInjDump = "MR-inj-dump";

    public static final String beamModeMRExtDump = "MR-ext-dump";

    public static final String beamModeKTarget = "K-target";

    public static final String beamModeNTarget = "N-target";

    public static final String beamModeKNTarget = "K+N-target";

    public static final String beamModeMLF_MRInj = "MLF+MR-inj";

    public static final String beamModeMLF_MRExt = "MLF+MR-ext";

    public static final String beamModeMLF_K = "MLF+K-target";

    public static final String beamModeMLF_N = "MLF+N-target";

    public static final String beamModeMLF_KN = "MLF+K+N-target";

    private double designEnergyRaw;

    private boolean caputFlag;

    private Channel energyChannel;

    private Channel energyBeforeDB1Channel;

    private Channel energyBeforeDB1ChannelLong;

    private Channel energyBeforeDB1ChannelRaw;

    private Channel energyDB1DB2Channel;

    private Channel energyDB1DB2ChannelLong;

    private Channel energyDB1DB2ChannelRaw;

    private Channel energyAfterDB2Channel;

    private Channel energyAfterDB2ChannelRaw;

    private Channel FCTPairChannel;

    private Channel RFScanChannel;

    private Channel RFScanChannelR1;

    private Channel RFScanChannelR2;

    private Channel eneRCSRFInjChannel;

    private Channel eneRCSRFExtChannel;

    private Channel chopperFreqChannel;

    public static final String[] beamDestinationTags = { "No Beam", "LEBT", "MEBT1", "0-deg dump", "30-deg dump", "90-deg dump", "100-deg dump", "analyzer dump", "L3BT" };

    private int beamDestination;

    public int getBeamDestination() {
        return beamDestination;
    }

    public String getBeamDestinationTag() {
        switch(beamDestination) {
            case beamToNOWHERE:
                return beamModeNOWHERE;
            case beamToLEBT:
                return beamModeLEBT;
            case beamToMEBT1:
                return beamModeMEBT1;
            case beamTo0Dump:
                return beamMode0Dump;
            case beamTo30Dump:
                return beamMode30Dump;
            case beamTo90Dump:
                return beamMode90Dump;
            case beamTo100Dump:
                return beamMode100Dump;
            case beamToH0Dump:
                return beamModeH0Dump;
            case beamTo3NDumpDC:
                return beamMode3NDumpDC;
            case beamTo3NDumpAC:
                return beamMode3NDumpAC;
            case beamToMLFTarget:
                return beamModeMLFTarget;
            case beamToMRInjDump:
                return beamModeMRInjDump;
            case beamToMRExtDump:
                return beamModeMRExtDump;
            case beamToKTarget:
                return beamModeKTarget;
            case beamToNTarget:
                return beamModeNTarget;
            case beamToKNTarget:
                return beamModeKNTarget;
            case beamToMLF_MRInj:
                return beamModeMLF_MRInj;
            case beamToMLF_MRExt:
                return beamModeMLF_MRExt;
            case beamToMLF_K:
                return beamModeMLF_K;
            case beamToMLF_N:
                return beamModeMLF_N;
            case beamToMLF_KN:
                return beamModeMLF_KN;
            default:
                return beamModeNOBEAM;
        }
    }

    private String currentBeamMode;

    public String getBeamMode() {
        return currentBeamMode;
    }

    private boolean isBeamOn;

    public boolean getIsBeamOn() {
        return isBeamOn;
    }

    private ArrayList<RFManager> currentRFManagers;

    private RFManager downstreamAccelerationRF;

    private RFManager nextdownstreamAccelerationRF;

    public RFManager getAccRF() {
        return downstreamAccelerationRF;
    }

    private RFManager scanRF;

    public RFManager getScanRF() {
        return scanRF;
    }

    private ArrayList<FCTPairManager> currentFCTPairs;

    public ArrayList<FCTPairManager> getCurrentFCTPair() {
        return currentFCTPairs;
    }

    private FCTPairManager refFCTPairBeforeDB1;

    private FCTPairManager refFCTPairBeforeDB1Long;

    private FCTPairManager refFCTPairBetweenDB1DB2;

    private FCTPairManager refFCTPairBetweenDB1DB2Long;

    private FCTPairManager refFCTPairAfterDB2;

    private double eneRefFCTPairBeforeDB1;

    private double eneRefFCTPairBeforeDB1Long;

    private double eneRefFCTPairBeforeDB1Raw;

    private double eneRefFCTPairBetweenDB1DB2;

    private double eneRefFCTPairBetweenDB1DB2Long;

    private double eneRefFCTPairBetweenDB1DB2Raw;

    private double eneRefFCTPairAfterDB2;

    private double eneRefFCTPairAfterDB2Raw;

    private double eneRCSRFInj;

    private double eneRCSRFExt;

    private double freqRCSRFInj;

    private double freqRCSRFExt;

    private int nRefFCTPairBeforeDB1;

    private int nRefFCTPairBeforeDB1Long;

    private int nRefFCTPairBeforeDB1Raw;

    private int nRefFCTPairBetweenDB1DB2;

    private int nRefFCTPairBetweenDB1DB2Long;

    private int nRefFCTPairBetweenDB1DB2Raw;

    private int nRefFCTPairAfterDB2;

    private int nRefFCTPairAfterDB2Raw;

    public double getEnergyRefBeforeDB1() {
        return eneRefFCTPairBeforeDB1;
    }

    public double getEnergyRefBeforeDB1Long() {
        return eneRefFCTPairBeforeDB1Long;
    }

    public double getEnergyRefBetweenDB1DB2() {
        return eneRefFCTPairBetweenDB1DB2;
    }

    public double getEnergyRefBetweenDB1DB2Long() {
        return eneRefFCTPairBetweenDB1DB2Long;
    }

    public double getEnergyRefAfterDB2() {
        return eneRefFCTPairAfterDB2;
    }

    public double getEnergyRCSRFInj() {
        return eneRCSRFInj;
    }

    public double getEnergyRCSRFExt() {
        return eneRCSRFExt;
    }

    public int getNRefBeforeDB1() {
        return nRefFCTPairBeforeDB1;
    }

    public int getNRefBeforeDB1Long() {
        return nRefFCTPairBeforeDB1Long;
    }

    public int getNRefBetweenDB1DB2() {
        return nRefFCTPairBetweenDB1DB2;
    }

    public int getNRefBetweenDB1DB2Long() {
        return nRefFCTPairBetweenDB1DB2Long;
    }

    public int getNRefAfterDB2() {
        return nRefFCTPairAfterDB2;
    }

    private CaMonitorScalar beamMode;

    private CaMonitorScalar SCTLEBT;

    private CaMonitorScalar SCTOnMEBT1;

    private CaMonitorScalar SCTOffMEBT1;

    private CaMonitorScalar SCT0Dump;

    private CaMonitorScalar SCT30Dump;

    private CaMonitorScalar SCT90Dump;

    private CaMonitorScalar SCT100Dump;

    private CaMonitorScalar SCTL3BT;

    private CaMonitorIntArray RCSRF;

    private double curCurrent;

    public double getCurCurrent() {
        return curCurrent;
    }

    private String curSCT;

    public String getCurSCT() {
        return curSCT;
    }

    private double designEnergy;

    public double getDesignEnergy() {
        return designEnergy;
    }

    static final int nFCTPairs = 5;

    private int indexFCT;

    public int getFCTPairIndex() {
        return indexFCT;
    }

    public double getEnergy() {
        if ((0 <= indexFCT) && (indexFCT <= nFCTPairs)) {
            return energies[indexFCT];
        } else {
            return 0;
        }
    }

    public int getN() {
        if (0 <= indexFCT && indexFCT <= nFCTPairs) {
            return Ns[indexFCT];
        } else {
            return -1;
        }
    }

    public String getFCTUp() {
        if (0 <= indexFCT && indexFCT <= nFCTPairs) {
            return FCTUps[indexFCT];
        } else {
            return "";
        }
    }

    public String getFCTDown() {
        if (0 <= indexFCT && indexFCT <= nFCTPairs) {
            return FCTDowns[indexFCT];
        } else {
            return "";
        }
    }

    private double energies[];

    private int Ns[];

    private String FCTUps[];

    private String FCTDowns[];

    private boolean FCTUpScans[];

    private boolean FCTDownScans[];

    private boolean FCTUpOks[];

    private boolean FCTDownOks[];

    public double[] getEnergies() {
        return energies;
    }

    public int[] getNs() {
        return Ns;
    }

    public String[] getFCTUps() {
        return FCTUps;
    }

    public String[] getFCTDowns() {
        return FCTDowns;
    }

    public boolean[] getFCTUpScans() {
        return FCTUpScans;
    }

    public boolean[] getFCTDownScans() {
        return FCTDownScans;
    }

    public boolean[] getFCTUpOks() {
        return FCTUpOks;
    }

    public boolean[] getFCTDownOks() {
        return FCTDownOks;
    }

    private double curLEBT;

    public double getCurOnLEBT() {
        return curLEBT;
    }

    private double curOnMEBT1;

    public double getCurOnMEBT1() {
        return curOnMEBT1;
    }

    private double curOffMEBT1;

    public double getCurOffMEBT1() {
        return curOffMEBT1;
    }

    private double cur0Dump;

    public double getCur0Dump() {
        return cur0Dump;
    }

    private double cur30Dump;

    public double getCur30Dump() {
        return cur30Dump;
    }

    private double cur90Dump;

    public double getCur90Dump() {
        return cur90Dump;
    }

    private double cur100Dump;

    public double getCur100Dump() {
        return cur100Dump;
    }

    private double curL3BT;

    public double getCurL3BT() {
        return curL3BT;
    }

    private EnergyMasterProperties prop;

    private CSVParser RFCalibParser;

    private CSVParser FCTPairCalibParser;

    private CSVParser FCTCalibParser;

    private CSVParser SCTCalibParser;

    private ArrayList<RFManager> RFManagersMEBT1;

    private ArrayList<RFManager> RFManagersL3BT;

    private boolean adjustSwitch;

    public ArrayList<RFManager> getRFManagersMEBT1() {
        return RFManagersMEBT1;
    }

    public ArrayList<RFManager> getRFManagersL3BT() {
        return RFManagersL3BT;
    }

    public EnergyMeasurer(boolean caputF) {
        caputFlag = caputF;
        currentFCTPairs = null;
        energies = new double[nFCTPairs];
        Ns = new int[nFCTPairs];
        FCTUps = new String[nFCTPairs];
        FCTDowns = new String[nFCTPairs];
        FCTUpScans = new boolean[nFCTPairs];
        FCTDownScans = new boolean[nFCTPairs];
        FCTUpOks = new boolean[nFCTPairs];
        FCTDownOks = new boolean[nFCTPairs];
        curSCT = "";
        beamDestination = beamToNOWHERE;
        curCurrent = 0;
        curLEBT = 0;
        curOnMEBT1 = 0;
        curOffMEBT1 = 0;
        cur0Dump = 0;
        cur30Dump = 0;
        cur90Dump = 0;
        cur100Dump = 0;
        curL3BT = 0;
        prop = new EnergyMasterProperties();
        adjustSwitch = false;
        if (prop.getAdjustPhaseSwitch() == 1) {
            adjustSwitch = true;
        }
        InputStream inRFCalib = EnergyMeasurer.class.getResourceAsStream("resources/" + prop.getRFCalib());
        InputStream inFCTPairCalib = EnergyMeasurer.class.getResourceAsStream("resources/" + prop.getFCTPairCalib());
        InputStream inFCTCalib = EnergyMeasurer.class.getResourceAsStream("resources/" + prop.getFCTCalib());
        InputStream inSCTCalib = EnergyMeasurer.class.getResourceAsStream("resources/" + prop.getSCTCalib());
        RFCalibParser = new CSVParser(inRFCalib);
        FCTPairCalibParser = new CSVParser(inFCTPairCalib);
        FCTCalibParser = new CSVParser(inFCTCalib);
        SCTCalibParser = new CSVParser(inSCTCalib);
        designEnergyRaw = 181;
        RFManagersMEBT1 = new ArrayList<RFManager>();
        RFManagersL3BT = new ArrayList<RFManager>();
        ArrayList<HashMap<String, String>> RFCalibTable = RFCalibParser.getData();
        Iterator<HashMap<String, String>> iter = RFCalibTable.iterator();
        while (iter.hasNext()) {
            HashMap<String, String> RFLine = iter.next();
            String id = RFLine.get(RFCalib.RFKey);
            RFManager rfm = new RFManager(id, RFLine, FCTPairCalibParser, FCTCalibParser, SCTCalibParser);
            if (rfm.getId().equals("LI_S15")) {
                designEnergyRaw = rfm.getDesignEnergy();
                ArrayList<FCTPairManager> pairList = rfm.getFCTPairs();
                for (int ip = 0; ip < pairList.size(); ip++) {
                    FCTPairManager pair = pairList.get(ip);
                    if (pair.getFCTUpstream().getId().equals("A03BF0") && pair.getFCTDownstream().getId().equals("A05BF1")) {
                        refFCTPairBetweenDB1DB2 = pair;
                    } else if (pair.getFCTUpstream().getId().equals("A03BF0") && pair.getFCTDownstream().getId().equals("L3F12")) {
                        refFCTPairBetweenDB1DB2Long = pair;
                    } else if (pair.getFCTUpstream().getId().equals("S15BF0") && pair.getFCTDownstream().getId().equals("M2F2")) {
                        refFCTPairBeforeDB1 = pair;
                    } else if (pair.getFCTUpstream().getId().equals("S15BF0") && pair.getFCTDownstream().getId().equals("A03AF0")) {
                        refFCTPairBeforeDB1Long = pair;
                    } else if (pair.getFCTUpstream().getId().equals("L3F50") && pair.getFCTDownstream().getId().equals("L3F63")) {
                        refFCTPairAfterDB2 = pair;
                    }
                }
            }
            if (RFLine.get(RFCalib.beamModeKey).equals(beamModeMEBT1)) {
                RFManagersMEBT1.add(rfm);
            } else {
                RFManagersL3BT.add(rfm);
            }
        }
        prepareRecords();
    }

    private void prepareRecords() {
        beamMode = new CaMonitorScalar(prop.getBeamModeRecord());
        SCTLEBT = new CaMonitorScalar(prop.getSCTLEBT() + ":MON:CUR");
        SCTOnMEBT1 = new CaMonitorScalar(prop.getSCTOnMEBT1() + ":MON:CUR");
        SCTOffMEBT1 = new CaMonitorScalar(prop.getSCTOffMEBT1() + ":MON:CUR");
        SCT0Dump = new CaMonitorScalar(prop.getSCT0Dump() + ":MON:CUR");
        SCT30Dump = new CaMonitorScalar(prop.getSCT30Dump() + ":MON:CUR");
        SCT90Dump = new CaMonitorScalar(prop.getSCT90Dump() + ":MON:CUR");
        SCT100Dump = new CaMonitorScalar(prop.getSCT100Dump() + ":MON:CUR");
        SCTL3BT = new CaMonitorScalar(prop.getSCTL3BT() + ":MON:CUR");
        RCSRF = new CaMonitorIntArray(prop.getRCSRFFreqRecord());
    }

    private void identifyBeam() {
        beamDestination = (int) beamMode.getValue();
        System.out.println("beamDestination = " + beamDestination);
        curLEBT = SCTLEBT.getValue();
        curOnMEBT1 = SCTOnMEBT1.getValue();
        curOffMEBT1 = SCTOffMEBT1.getValue();
        cur0Dump = SCT0Dump.getValue();
        cur30Dump = SCT30Dump.getValue();
        cur90Dump = SCT90Dump.getValue();
        cur100Dump = SCT100Dump.getValue();
        curL3BT = SCTL3BT.getValue();
        double curTh = prop.getSCTCurThreshold();
        currentBeamMode = getBeamDestinationTag();
        switch(beamDestination) {
            case beamToNOWHERE:
            case beamToLEBT:
                currentRFManagers = RFManagersMEBT1;
                curCurrent = 0;
                curSCT = null;
                isBeamOn = false;
                break;
            case beamToMEBT1:
                currentRFManagers = RFManagersMEBT1;
                curCurrent = curOnMEBT1;
                curSCT = SCTOnMEBT1.getChannelName();
                if (curCurrent >= curTh) {
                    isBeamOn = true;
                } else {
                    isBeamOn = false;
                }
                break;
            case beamTo0Dump:
            case beamTo30Dump:
            case beamTo90Dump:
            case beamTo100Dump:
            case beamToH0Dump:
            case beamTo3NDumpDC:
            case beamTo3NDumpAC:
            case beamToMLFTarget:
            case beamToMRInjDump:
            case beamToMRExtDump:
            case beamToKTarget:
            case beamToNTarget:
            case beamToKNTarget:
            case beamToMLF_MRInj:
            case beamToMLF_MRExt:
            case beamToMLF_K:
            case beamToMLF_N:
            case beamToMLF_KN:
                currentRFManagers = RFManagersL3BT;
                curCurrent = curOnMEBT1;
                curSCT = SCTOnMEBT1.getChannelName();
                if (curCurrent >= curTh) {
                    isBeamOn = true;
                } else {
                    isBeamOn = false;
                }
                break;
            default:
                currentRFManagers = RFManagersL3BT;
                curCurrent = curOnMEBT1;
                curSCT = SCTOnMEBT1.getChannelName();
                if (curCurrent >= curTh) {
                    isBeamOn = true;
                } else {
                    isBeamOn = false;
                }
                break;
        }
        calculateRCSRFEnergy(RCSRF.getValue());
        if (caputFlag) {
            if (eneRCSRFInjChannel == null) {
                eneRCSRFInjChannel = ChannelFactory.defaultFactory().getChannel(prop.getRCSEnergyInjRecord());
            }
            CaMonitorScalar.setChannel(eneRCSRFInjChannel, eneRCSRFInj);
            if (eneRCSRFExtChannel == null) {
                eneRCSRFExtChannel = ChannelFactory.defaultFactory().getChannel(prop.getRCSEnergyExtRecord());
            }
            CaMonitorScalar.setChannel(eneRCSRFExtChannel, eneRCSRFExt);
            if (chopperFreqChannel == null) {
                chopperFreqChannel = ChannelFactory.defaultFactory().getChannel(prop.getChopperFreqRecord());
            }
            CaMonitorScalar.setChannel(chopperFreqChannel, freqRCSRFInj);
        }
    }

    private void calculateRCSRFEnergy(int[] rfarray) {
        eneRCSRFInj = 0;
        eneRCSRFExt = 0;
        freqRCSRFInj = 0;
        freqRCSRFExt = 0;
        if ((rfarray == null) || (rfarray.length < 40000)) {
            return;
        }
        final int indinj = prop.getRCSRFIndInj();
        final int indext = prop.getRCSRFIndExt();
        final double lRCS = prop.getRCSCircumference();
        final double c = prop.getCLight();
        final double mp = prop.getMProton();
        final double harmonic = prop.getRCSRFFreqHarmonic();
        final double convFactor = prop.getRCSRFFreqFactor();
        final double power = prop.getRCSRFFreqPower();
        if ((indinj - 1 >= 0) && (indinj - 1 < 40000)) {
            int rfrawin = rfarray[indinj - 1];
            freqRCSRFInj = rfrawin * convFactor / (Math.pow(2, power) * harmonic);
            double betain = freqRCSRFInj * 1.e+6 * lRCS / c;
            if ((betain >= 0) && (betain <= 1)) {
                double gammain = 1 / Math.sqrt(1 - betain * betain);
                eneRCSRFInj = (gammain - 1) * mp / 1000.;
            }
        }
        if ((indext - 1 >= 0) && (indext - 1 < 40000)) {
            int rfrawext = rfarray[indext - 1];
            freqRCSRFExt = rfrawext * convFactor / (Math.pow(2, power) * harmonic);
            double betaext = freqRCSRFExt * 1.e+6 * lRCS / c;
            if ((betaext >= 0) && (betaext <= 1)) {
                double gammaext = 1 / Math.sqrt(1 - betaext * betaext);
                eneRCSRFExt = (gammaext - 1) * mp / 1000.;
            }
        }
    }

    private void identifyDownstreamAccelerationRF() {
        currentFCTPairs = null;
        downstreamAccelerationRF = null;
        nextdownstreamAccelerationRF = null;
        for (int irf = 0; irf < currentRFManagers.size(); irf++) {
            RFManager RF = currentRFManagers.get(irf);
            if (!RF.getAcceleration()) {
                continue;
            }
            boolean RFon = RF.checkRFOn();
            boolean RFampok = RF.checkProperAmplitude();
            boolean RFsync = RF.checkBeamSynchronized();
            if (RFon && RFampok && RFsync) {
                nextdownstreamAccelerationRF = downstreamAccelerationRF;
                downstreamAccelerationRF = RF;
                currentFCTPairs = RF.getFCTPairs();
            } else {
            }
        }
    }

    private void identifyScanRF() {
        scanRF = null;
        String scanRec;
        String scanRecReset1;
        String scanRecReset2;
        int bit = 0;
        if (!isBeamOn) {
            scanRec = prop.getRFDTLScanRecord();
            scanRecReset1 = prop.getRFSDTLScanRecord();
            scanRecReset2 = prop.getRFBCQDScanRecord();
        } else {
            scanRec = prop.getRFDTLScanRecord();
            scanRecReset1 = prop.getRFSDTLScanRecord();
            scanRecReset2 = prop.getRFBCQDScanRecord();
            for (int irf = currentRFManagers.size() - 1; irf >= 0; irf--) {
                RFManager RF = currentRFManagers.get(irf);
                boolean RFampok = RF.checkProperAmplitude();
                boolean RFon = RF.checkRFOn();
                boolean RFsync = RF.checkBeamSynchronized();
                boolean RFscan = RF.checkScanned();
                if (RFampok & RFon && RFsync && RFscan) {
                    scanRF = RF;
                    RFCalib calib = RF.getCalib();
                    int bitMode = calib.getTimingRecordBit();
                    bit = bitMode % 100;
                    if (bitMode < 100) {
                        scanRec = prop.getRFDTLScanRecord();
                        scanRecReset1 = prop.getRFSDTLScanRecord();
                        scanRecReset2 = prop.getRFBCQDScanRecord();
                    } else if (bitMode < 200) {
                        scanRec = prop.getRFSDTLScanRecord();
                        scanRecReset1 = prop.getRFDTLScanRecord();
                        scanRecReset2 = prop.getRFBCQDScanRecord();
                    } else {
                        scanRec = prop.getRFBCQDScanRecord();
                        scanRecReset1 = prop.getRFDTLScanRecord();
                        scanRecReset2 = prop.getRFSDTLScanRecord();
                    }
                    break;
                }
            }
            if (RFScanChannel == null) {
                RFScanChannel = ChannelFactory.defaultFactory().getChannel(scanRec);
            }
            if (RFScanChannelR1 == null) {
                RFScanChannelR1 = ChannelFactory.defaultFactory().getChannel(scanRecReset1);
            }
            if (RFScanChannelR2 == null) {
                RFScanChannelR2 = ChannelFactory.defaultFactory().getChannel(scanRecReset2);
            }
            if (caputFlag) {
                CaMonitorScalar.setChannel(RFScanChannel, bit);
                CaMonitorScalar.setChannel(RFScanChannelR1, 0);
                CaMonitorScalar.setChannel(RFScanChannelR2, 0);
            }
        }
    }

    private void calculateEnergy() {
        for (int i = 0; i < nFCTPairs; i++) {
            energies[i] = 0;
            Ns[i] = 0;
            FCTUps[i] = "";
            FCTDowns[i] = "";
            FCTUpScans[i] = false;
            FCTDownScans[i] = false;
            FCTUpOks[i] = false;
            FCTDownOks[i] = false;
        }
        indexFCT = -1;
        eneRefFCTPairBeforeDB1Raw = calcRefEnergyRaw(refFCTPairBeforeDB1, designEnergyRaw);
        nRefFCTPairBeforeDB1Raw = calcRefNWaveRaw(refFCTPairBeforeDB1, designEnergyRaw);
        eneRefFCTPairBetweenDB1DB2Raw = calcRefEnergyRaw(refFCTPairBetweenDB1DB2, designEnergyRaw);
        nRefFCTPairBetweenDB1DB2Raw = calcRefNWaveRaw(refFCTPairBetweenDB1DB2, designEnergyRaw);
        eneRefFCTPairAfterDB2Raw = calcRefEnergyRaw(refFCTPairAfterDB2, designEnergyRaw);
        nRefFCTPairAfterDB2Raw = calcRefNWaveRaw(refFCTPairAfterDB2, designEnergyRaw);
        if (caputFlag) {
            if (energyBeforeDB1ChannelRaw == null) {
                energyBeforeDB1ChannelRaw = ChannelFactory.defaultFactory().getChannel(prop.getEnergyBeforeDB1RecordRaw());
            }
            CaMonitorScalar.setChannel(energyBeforeDB1ChannelRaw, eneRefFCTPairBeforeDB1Raw);
            if (energyDB1DB2ChannelRaw == null) {
                energyDB1DB2ChannelRaw = ChannelFactory.defaultFactory().getChannel(prop.getEnergyDB1DB2RecordRaw());
            }
            CaMonitorScalar.setChannel(energyDB1DB2ChannelRaw, eneRefFCTPairBetweenDB1DB2Raw);
            if (energyAfterDB2ChannelRaw == null) {
                energyAfterDB2ChannelRaw = ChannelFactory.defaultFactory().getChannel(prop.getEnergyAfterDB2RecordRaw());
            }
            CaMonitorScalar.setChannel(energyAfterDB2ChannelRaw, eneRefFCTPairAfterDB2Raw);
        }
        if ((!isBeamOn) || (downstreamAccelerationRF == null)) {
            designEnergy = 0;
            if (caputFlag) {
                if (energyChannel == null) {
                    energyChannel = ChannelFactory.defaultFactory().getChannel(prop.getEnergyRecord());
                }
                eneBest = 0;
                CaMonitorScalar.setChannel(energyChannel, eneBest);
                if (energyBeforeDB1Channel == null) {
                    energyBeforeDB1Channel = ChannelFactory.defaultFactory().getChannel(prop.getEnergyBeforeDB1Record());
                }
                CaMonitorScalar.setChannel(energyBeforeDB1Channel, eneBest);
                if (energyBeforeDB1ChannelLong == null) {
                    energyBeforeDB1ChannelLong = ChannelFactory.defaultFactory().getChannel(prop.getEnergyBeforeDB1RecordLong());
                }
                CaMonitorScalar.setChannel(energyBeforeDB1Channel, eneBest);
                if (energyDB1DB2Channel == null) {
                    energyDB1DB2Channel = ChannelFactory.defaultFactory().getChannel(prop.getEnergyDB1DB2Record());
                }
                CaMonitorScalar.setChannel(energyDB1DB2Channel, eneBest);
                if (energyDB1DB2ChannelLong == null) {
                    energyDB1DB2ChannelLong = ChannelFactory.defaultFactory().getChannel(prop.getEnergyDB1DB2RecordLong());
                }
                CaMonitorScalar.setChannel(energyDB1DB2ChannelLong, eneBest);
                if (energyAfterDB2Channel == null) {
                    energyAfterDB2Channel = ChannelFactory.defaultFactory().getChannel(prop.getEnergyAfterDB2Record());
                }
                CaMonitorScalar.setChannel(energyAfterDB2Channel, eneBest);
                if (FCTPairChannel == null) {
                    FCTPairChannel = ChannelFactory.defaultFactory().getChannel(prop.getFCTPairRecord());
                }
                CaMonitorScalar.setChannel(FCTPairChannel, -1.);
            }
            return;
        }
        double sRF = downstreamAccelerationRF.getPosition();
        boolean energySet = false;
        FCTPairManager pairBest = null;
        if (scanRF == null) {
            designEnergy = downstreamAccelerationRF.getDesignEnergy();
            ArrayList<FCTPairManager> pairs = downstreamAccelerationRF.getFCTPairs();
            for (int ip = 0; ip < pairs.size(); ip++) {
                FCTPairManager pair = pairs.get(ip);
                FCTManager up = pair.getFCTUpstream();
                FCTManager down = pair.getFCTDownstream();
                FCTUps[ip] = up.getId();
                FCTDowns[ip] = down.getId();
                double sFCT = pair.getFCTUpstream().getPosition();
                pair.checkStatus();
                FCTUpScans[ip] = up.getIsScanned();
                FCTDownScans[ip] = down.getIsScanned();
                if (!up.getIsScanned() && up.getIsPhaseSwitchGood() && sFCT >= sRF && pair.getIsBeamOn()) {
                    FCTUpOks[ip] = true;
                }
                if (!down.getIsScanned() && down.getIsPhaseSwitchGood() && sFCT >= sRF && pair.getIsBeamOn()) {
                    FCTDownOks[ip] = true;
                }
                double ene = pair.calculateEnergy(adjustSwitch, designEnergy);
                energies[ip] = ene;
                Ns[ip] = pair.getN0();
                if ((ene > 0) && (!energySet)) {
                    pairBest = pair;
                    eneBest = ene;
                    energySet = true;
                    indexFCT = ip;
                }
            }
            boolean adjustRef = false;
            eneRefFCTPairBeforeDB1 = calcRefEnergy(refFCTPairBeforeDB1, sRF, adjustRef, designEnergy);
            nRefFCTPairBeforeDB1 = calcRefNWave(refFCTPairBeforeDB1, sRF, adjustRef, designEnergy);
            eneRefFCTPairBeforeDB1Long = calcRefEnergy(refFCTPairBeforeDB1Long, sRF, adjustRef, designEnergy);
            nRefFCTPairBeforeDB1Long = calcRefNWave(refFCTPairBeforeDB1Long, sRF, adjustRef, designEnergy);
            eneRefFCTPairBetweenDB1DB2 = calcRefEnergy(refFCTPairBetweenDB1DB2, sRF, adjustRef, designEnergy);
            nRefFCTPairBetweenDB1DB2 = calcRefNWave(refFCTPairBetweenDB1DB2, sRF, adjustRef, designEnergy);
            eneRefFCTPairBetweenDB1DB2Long = calcRefEnergy(refFCTPairBetweenDB1DB2Long, sRF, adjustRef, designEnergy);
            nRefFCTPairBetweenDB1DB2Long = calcRefNWave(refFCTPairBetweenDB1DB2Long, sRF, adjustRef, designEnergy);
            eneRefFCTPairAfterDB2 = calcRefEnergy(refFCTPairAfterDB2, sRF, adjustRef, designEnergy);
            nRefFCTPairAfterDB2 = calcRefNWave(refFCTPairAfterDB2, sRF, adjustRef, designEnergy);
            if (caputFlag) {
                if (energyChannel == null) {
                    energyChannel = ChannelFactory.defaultFactory().getChannel(prop.getEnergyRecord());
                }
                CaMonitorScalar.setChannel(energyChannel, eneBest);
                if (energyBeforeDB1Channel == null) {
                    energyBeforeDB1Channel = ChannelFactory.defaultFactory().getChannel(prop.getEnergyBeforeDB1Record());
                }
                CaMonitorScalar.setChannel(energyBeforeDB1Channel, eneRefFCTPairBeforeDB1);
                if (energyBeforeDB1ChannelLong == null) {
                    energyBeforeDB1ChannelLong = ChannelFactory.defaultFactory().getChannel(prop.getEnergyBeforeDB1RecordLong());
                }
                CaMonitorScalar.setChannel(energyBeforeDB1ChannelLong, eneRefFCTPairBeforeDB1Long);
                if (energyDB1DB2Channel == null) {
                    energyDB1DB2Channel = ChannelFactory.defaultFactory().getChannel(prop.getEnergyDB1DB2Record());
                }
                CaMonitorScalar.setChannel(energyDB1DB2Channel, eneRefFCTPairBetweenDB1DB2);
                if (energyDB1DB2ChannelLong == null) {
                    energyDB1DB2ChannelLong = ChannelFactory.defaultFactory().getChannel(prop.getEnergyDB1DB2RecordLong());
                }
                CaMonitorScalar.setChannel(energyDB1DB2ChannelLong, eneRefFCTPairBetweenDB1DB2Long);
                if (energyAfterDB2Channel == null) {
                    energyAfterDB2Channel = ChannelFactory.defaultFactory().getChannel(prop.getEnergyAfterDB2Record());
                }
                CaMonitorScalar.setChannel(energyAfterDB2Channel, eneRefFCTPairAfterDB2);
                if (FCTPairChannel == null) {
                    FCTPairChannel = ChannelFactory.defaultFactory().getChannel(prop.getFCTPairRecord());
                }
                int code = -1;
                if (pairBest != null) {
                    code = pairBest.getPairCode();
                }
                CaMonitorScalar.setChannel(FCTPairChannel, code);
            }
        } else {
            designEnergy = nextdownstreamAccelerationRF.getDesignEnergy();
            double sRFScan = scanRF.getPosition();
            ArrayList<FCTPairManager> pairs = downstreamAccelerationRF.getFCTPairs();
            for (int ip = 0; ip < pairs.size(); ip++) {
                FCTPairManager pair = pairs.get(ip);
                FCTManager up = pair.getFCTUpstream();
                FCTManager down = pair.getFCTDownstream();
                FCTUps[ip] = up.getId();
                FCTDowns[ip] = down.getId();
                FCTUpScans[ip] = up.getIsScanned();
                FCTDownScans[ip] = down.getIsScanned();
                double sFCT = pair.getFCTUpstream().getPosition();
                pair.checkStatus();
                if (!up.getIsScanned() && up.getIsPhaseSwitchGood() && sFCT >= sRFScan) {
                    FCTUpOks[ip] = true;
                }
                if (!down.getIsScanned() && down.getIsPhaseSwitchGood() && sFCT >= sRFScan) {
                    FCTDownOks[ip] = true;
                }
                double ene = pair.calculateEnergy(false, designEnergy);
                energies[ip] = ene;
                Ns[ip] = pair.getN0();
                if ((ene > 0) && (!energySet) && (sFCT > sRFScan)) {
                    pairBest = pair;
                    energySet = true;
                    indexFCT = ip;
                    eneBest = ene;
                }
            }
            boolean adjustRef = false;
            eneRefFCTPairBeforeDB1 = calcRefEnergy(refFCTPairBeforeDB1, sRFScan, adjustRef, designEnergy);
            nRefFCTPairBeforeDB1 = calcRefNWave(refFCTPairBeforeDB1, sRFScan, adjustRef, designEnergy);
            eneRefFCTPairBeforeDB1Long = calcRefEnergy(refFCTPairBeforeDB1Long, sRFScan, adjustRef, designEnergy);
            nRefFCTPairBeforeDB1Long = calcRefNWave(refFCTPairBeforeDB1Long, sRFScan, adjustRef, designEnergy);
            eneRefFCTPairBetweenDB1DB2 = calcRefEnergy(refFCTPairBetweenDB1DB2, sRFScan, adjustRef, designEnergy);
            nRefFCTPairBetweenDB1DB2 = calcRefNWave(refFCTPairBetweenDB1DB2, sRFScan, adjustRef, designEnergy);
            eneRefFCTPairBetweenDB1DB2Long = calcRefEnergy(refFCTPairBetweenDB1DB2Long, sRFScan, adjustRef, designEnergy);
            nRefFCTPairBetweenDB1DB2Long = calcRefNWave(refFCTPairBetweenDB1DB2Long, sRFScan, adjustRef, designEnergy);
            eneRefFCTPairAfterDB2 = calcRefEnergy(refFCTPairAfterDB2, sRFScan, adjustRef, designEnergy);
            nRefFCTPairAfterDB2 = calcRefNWave(refFCTPairAfterDB2, sRFScan, adjustRef, designEnergy);
            if (caputFlag) {
                if (energyChannel == null) {
                    energyChannel = ChannelFactory.defaultFactory().getChannel(prop.getEnergyRecord());
                }
                CaMonitorScalar.setChannel(energyChannel, eneBest);
                if (energyBeforeDB1Channel == null) {
                    energyBeforeDB1Channel = ChannelFactory.defaultFactory().getChannel(prop.getEnergyBeforeDB1Record());
                }
                CaMonitorScalar.setChannel(energyBeforeDB1Channel, eneRefFCTPairBeforeDB1);
                if (energyBeforeDB1ChannelLong == null) {
                    energyBeforeDB1ChannelLong = ChannelFactory.defaultFactory().getChannel(prop.getEnergyBeforeDB1RecordLong());
                }
                CaMonitorScalar.setChannel(energyBeforeDB1ChannelLong, eneRefFCTPairBeforeDB1Long);
                if (energyDB1DB2Channel == null) {
                    energyDB1DB2Channel = ChannelFactory.defaultFactory().getChannel(prop.getEnergyDB1DB2Record());
                }
                CaMonitorScalar.setChannel(energyDB1DB2Channel, eneRefFCTPairBetweenDB1DB2);
                if (energyDB1DB2ChannelLong == null) {
                    energyDB1DB2ChannelLong = ChannelFactory.defaultFactory().getChannel(prop.getEnergyDB1DB2RecordLong());
                }
                CaMonitorScalar.setChannel(energyDB1DB2Channel, eneRefFCTPairBetweenDB1DB2Long);
                if (energyAfterDB2Channel == null) {
                    energyAfterDB2Channel = ChannelFactory.defaultFactory().getChannel(prop.getEnergyAfterDB2Record());
                }
                CaMonitorScalar.setChannel(energyAfterDB2Channel, eneRefFCTPairAfterDB2);
                if (FCTPairChannel == null) {
                    FCTPairChannel = ChannelFactory.defaultFactory().getChannel(prop.getFCTPairRecord());
                }
                int code = -1;
                if (pairBest != null) {
                    code = pairBest.getPairCode();
                }
                CaMonitorScalar.setChannel(FCTPairChannel, code);
            }
        }
    }

    public void run() {
        identifyBeam();
        identifyDownstreamAccelerationRF();
        identifyScanRF();
        calculateEnergy();
        printStatus();
    }

    public void printStatus() {
        System.out.println("isBeamOn = " + getIsBeamOn());
        System.out.println("beamDestination = " + getBeamDestination());
        System.out.println("beamMode = " + getBeamMode());
        System.out.println("current = " + curOnMEBT1);
        System.out.println("energy = " + eneBest + " MeV");
        for (int i = 0; i < energies.length; i++) {
            double ene = energies[i];
            String pair = FCTUps[i] + "-" + FCTDowns[i];
            System.out.println(i + ": FCTpair = " + pair + " : " + ene + " MeV");
        }
    }

    private double calcRefEnergy(FCTPairManager pair, double sRF, boolean adjustRef, double designEne) {
        double energy = 0;
        if (pair != null) {
            FCTManager up = pair.getFCTUpstream();
            FCTManager down = pair.getFCTDownstream();
            double sFCT = pair.getFCTUpstream().getPosition();
            pair.checkStatus();
            boolean fctupok = false;
            boolean fctdownok = false;
            if (!up.getIsScanned() && up.getIsPhaseSwitchGood() && sFCT >= sRF && pair.getIsBeamOn()) {
                fctupok = true;
            }
            if (!down.getIsScanned() && down.getIsPhaseSwitchGood() && sFCT >= sRF && pair.getIsBeamOn()) {
                fctdownok = true;
            }
            double ene = pair.calculateEnergy(adjustRef, designEne);
            if ((ene > 0) && (fctupok) && (fctdownok)) {
                energy = ene;
            }
        }
        return energy;
    }

    private int calcRefNWave(FCTPairManager pair, double sRF, boolean adjustRef, double designEne) {
        int nwave = -1;
        if (pair != null) {
            FCTManager up = pair.getFCTUpstream();
            FCTManager down = pair.getFCTDownstream();
            double sFCT = pair.getFCTUpstream().getPosition();
            pair.checkStatus();
            boolean fctupok = false;
            boolean fctdownok = false;
            if (!up.getIsScanned() && up.getIsPhaseSwitchGood() && sFCT >= sRF && pair.getIsBeamOn()) {
                fctupok = true;
            }
            if (!down.getIsScanned() && down.getIsPhaseSwitchGood() && sFCT >= sRF && pair.getIsBeamOn()) {
                fctdownok = true;
            }
            double ene = pair.calculateEnergy(adjustRef, designEne);
            int n = pair.getN0();
            if ((ene > 0) && (fctupok) && (fctdownok)) {
                nwave = n;
            }
        }
        return nwave;
    }

    private double calcRefEnergyRaw(FCTPairManager pair, double designEne) {
        boolean adjustRef = false;
        double energy = 0;
        if (pair != null) {
            energy = pair.calculateEnergy(adjustRef, designEne);
        }
        return energy;
    }

    private int calcRefNWaveRaw(FCTPairManager pair, double designEne) {
        boolean adjustRef = false;
        int nwave = -1;
        if (pair != null) {
            double ene = pair.calculateEnergy(adjustRef, designEne);
            nwave = pair.getN0();
        }
        return nwave;
    }
}
