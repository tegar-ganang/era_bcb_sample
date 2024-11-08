package gov.sns.apps.diagnostics.blmview;

import gov.sns.ca.*;
import gov.sns.tools.plot.*;
import gov.sns.tools.scan.SecondEdition.*;
import java.io.*;
import java.text.*;

public abstract class BLMdevice {

    private static int counter = 0;

    private int ID = -1;

    private String devName;

    private String devSection;

    private static DecimalFormat expFormat = new DecimalFormat("0.###E0#");

    private static DecimalFormat decFormat = new DecimalFormat("####0");

    private String devMPSChan;

    private double locationZ = 0;

    private double[] timearr;

    public void write(BufferedWriter out) throws IOException {
        String s = "   <BLMdevice name=\"" + devName + "\" devicetype=\"" + getDeviceType() + "\" " + "section=\"" + devSection + "\" mpschan=\"" + devMPSChan + "\"" + " highvoltage=\"" + decFormat.format(getHV()) + "\" mpslimit=\"" + getMPSLimit() + "\" afegain1st=\"" + decFormat.format(getAFEGain()) + "\"" + " locationz=\"" + locationZ + "\" />\n";
        out.write(s);
    }

    private double calaculatedSlowMPSLimit;

    public void cleanUP() {
        aFEpv.removeValueListeners();
        aFEpv.getChannel().disconnect();
        aFEpv.removeMonitoredPV(aFEpv);
        fastPV.removeValueListeners();
        fastPV.removeMonitoredPV(fastPV);
        slowPV.removeValueListeners();
        slowPV.removeMonitoredPV(slowPV);
        slowPulseLoss.removeValueListeners();
        slowPulseLoss.removeMonitoredPV(slowPulseLoss);
        highVoltage.removeValueListeners();
        highVoltage.removeMonitoredPV(highVoltage);
        highVoltageCurrent.removeValueListeners();
        highVoltageCurrent.removeMonitoredPV(highVoltageCurrent);
        mpsLimit.removeValueListeners();
        mpsLimit.removeMonitoredPV(mpsLimit);
        mpsStatus.removeValueListeners();
        mpsStatus.removeMonitoredPV(mpsStatus);
    }

    public abstract double convertToRadiation(double c);

    public abstract String getRadiationUnits();

    public abstract String getDeviceType();

    private String refName;

    private MonitoredPV aFEpv;

    private static final double deltaT = 10.0E-6;

    private static final double resistorIn = 10.0E+3;

    private static final double capacity = 2200.0E-12;

    private MonitoredPV fastPV;

    private MonitoredPV slowPV;

    private double calculatedFastIntegral;

    private double calculatedSlowIntegral;

    private MonitoredPV slowPulseLoss;

    private MonitoredPV highVoltage;

    private MonitoredPV highVoltageCurrent;

    private MonitoredPV mpsLimit;

    private MonitoredPV mpsStatus;

    private double calaculatedMPSLimit;

    public BLMdevice(String name, String section, String mpschan, double locz) {
        devName = name;
        devSection = section;
        devMPSChan = mpschan;
        locationZ = locz;
        if (mpschan == null) devMPSChan = "FPAR_CCL_BS_chan_status";
        initialize(section + ":" + name, section + ":" + name + ":SlowPulseLossRb", section + ":" + name + ":HVBiasRb", section + ":" + name + ":DbgHVCurrentRb", section + ":" + name + ":MPSPulseLossLimitRb", section + ":" + name + ":" + devMPSChan, section + ":" + name + ":AFEFirstStageGainRb");
    }

    public BLMdevice(String name, String section) {
        devName = name;
        devSection = section;
        devMPSChan = "FPAR_CCL_BS_chan_status";
        initialize(section + ":" + name, section + ":" + name + ":SlowPulseLossRb", section + ":" + name + ":HVBiasRb", section + ":" + name + ":DbgHVCurrentRb", section + ":" + name + ":MPSPulseLossLimitRb", section + ":" + name + ":" + devMPSChan, section + ":" + name + ":AFEFirstStageGainRb");
    }

    public String getSection() {
        return devSection;
    }

    public String getName() {
        return devName;
    }

    /**
	 * Method getFastData
	 *
	 * @return   a  CurveData
	 */
    public CurveData getFastData() {
        calculateFastData();
        return fastCurveData;
    }

    /**
	 * Method calculateAll
	 *
	 */
    public void calculateAll() {
        calculateFastIntegral();
        calculateSlowIntegral();
        calculateMPSLimitFromFastData();
        calculateSlowMPSLimit();
    }

    public void initialize(String refn, String pvf, String hv, String hvc, String mpsl, String mpss, String afe) {
        ID = counter;
        counter++;
        refName = refn;
        slowPulseLoss = MonitoredPV.getMonitoredPV(pvf);
        highVoltage = MonitoredPV.getMonitoredPV(hv);
        highVoltageCurrent = MonitoredPV.getMonitoredPV(hvc);
        mpsLimit = MonitoredPV.getMonitoredPV(mpsl);
        mpsStatus = MonitoredPV.getMonitoredPV(mpss);
        aFEpv = MonitoredPV.getMonitoredPV(afe);
        slowPulseLoss.setChannelName(pvf);
        highVoltage.setChannelName(hv);
        highVoltageCurrent.setChannelName(hvc);
        mpsLimit.setChannelName(mpsl);
        mpsStatus.setChannelName(mpss);
        aFEpv.setChannelName(afe);
        fastPV = MonitoredPV.getMonitoredPV(refName + ":FastDataRb");
        fastPV.setChannelName(refName + ":FastDataRb");
        slowPV = MonitoredPV.getMonitoredPV(refName + ":SlowDataRb");
        slowPV.setChannelName(refName + ":SlowDataRb");
    }

    /**
	 * Method getRefName
	 *
	 * @return   a  K
	 */
    public String getRefName() {
        return refName;
    }

    private CurveData fastCurveData;

    protected void calculateFastData() {
        if (fastCurveData == null) {
            fastCurveData = new CurveData();
            boolean failed = false;
            try {
                double arr[] = fastPV.getChannel().getArrDbl();
                timearr = new double[arr.length];
                for (int i = 0; i < arr.length; i++) {
                    fastCurveData.addPoint(i * deltaT, arr[i]);
                    timearr[i] = i * deltaT;
                }
            } catch (GetException e) {
                failed = true;
            } catch (ConnectionException e2) {
                failed = true;
            }
            if (failed) {
                fastCurveData.addPoint(0, 0);
                fastCurveData.addPoint(1, 1);
                fastCurveData.addPoint(2, 0);
            }
        } else {
            boolean failed = false;
            try {
                double arr[] = fastPV.getChannel().getArrDbl();
                fastCurveData.setPoints(timearr, arr);
            } catch (GetException e) {
                failed = true;
            } catch (ConnectionException e2) {
                failed = true;
            } catch (Exception e) {
                System.out.println("Exception in fast Data: " + e.getMessage());
            }
            if (failed) {
                fastCurveData.addPoint(0, 0);
                fastCurveData.addPoint(1, 1);
                fastCurveData.addPoint(2, 0);
            }
        }
    }

    protected void calculateFastIntegral() {
        boolean failed = false;
        double sum = 0;
        try {
            double arr[] = fastPV.getChannel().getArrDbl();
            for (int i = 0; i < arr.length; i++) {
                sum += arr[i];
            }
        } catch (GetException e) {
            failed = true;
        } catch (ConnectionException e2) {
            failed = true;
        }
        if (failed) {
            sum = 0;
        }
        calculatedFastIntegral = sum * deltaT;
    }

    public double getCalculatedFastIntegral() {
        return calculatedFastIntegral;
    }

    public double getAFEGain() {
        return aFEpv.getValue();
    }

    public double getHV() {
        return highVoltage.getValue();
    }

    public double getHVCurrent() {
        return highVoltageCurrent.getValue();
    }

    protected void calculateMPSLimitFromSlowData() {
        if (getDeviceType().equals("IC")) calaculatedMPSLimit = 620.0 * getAFEGain() * calculatedSlowIntegral / convertToRadiation(1.0) / resistorIn / capacity; else calaculatedMPSLimit = 620.0 * getAFEGain() * calculatedSlowIntegral / convertToRadiation(1.0) / resistorIn / capacity;
    }

    protected void calculateMPSLimitFromFastData() {
        if (getDeviceType().equals("IC")) calaculatedMPSLimit = 620.0 * getAFEGain() * calculatedFastIntegral / convertToRadiation(1.0) / resistorIn / capacity; else calaculatedMPSLimit = 620.0 * getAFEGain() * calculatedFastIntegral / convertToRadiation(1.0) / resistorIn / capacity;
    }

    protected void calculateSlowMPSLimit() {
        if (getDeviceType().equals("IC")) calaculatedSlowMPSLimit = 620.0 * getAFEGain() * getSlowIntegral() / convertToRadiation(1.0) / resistorIn / capacity; else calaculatedSlowMPSLimit = 620.0 * getAFEGain() * getSlowIntegral() / convertToRadiation(1.0) * 1e-12 / resistorIn / capacity;
    }

    public double getCalculatedMPSlimit() {
        return calaculatedMPSLimit;
    }

    public double getCalculatedSlowMPSlimit() {
        return calaculatedSlowMPSLimit;
    }

    public double getMPSLimit() {
        return mpsLimit.getValue();
    }

    public Boolean getMPSstatus() {
        if (mpsStatus.getValue() == 1) {
            return new Boolean(true);
        } else return new Boolean(false);
    }

    protected void calculateSlowIntegral() {
        boolean failed = false;
        double sum = 0;
        try {
            double arr[] = slowPV.getChannel().getArrDbl();
            double baseline = 0;
            for (int i = 0; i < arr.length; i++) {
                sum += arr[i];
            }
        } catch (GetException e) {
            failed = true;
        } catch (ConnectionException e2) {
            failed = true;
        }
        if (failed) {
            sum = 0;
        }
        calculatedSlowIntegral = sum * deltaT;
    }

    public double getCalculatedSlowIntegral() {
        return calculatedSlowIntegral;
    }

    public double getSlowIntegral() {
        if (getDeviceType().equals("IC")) return slowPulseLoss.getValue(); else return slowPulseLoss.getValue();
    }

    public double getLocationZ() {
        return locationZ;
    }

    public boolean setParameters(int par) {
        if (parametersSetChannel == null) parametersSetChannel = ChannelFactory.defaultFactory().getChannel(getRefName() + ":DbgCmdSetParameters");
        parametersSetChannel.connect();
        int newpar = 0;
        try {
            if (parametersSetChannel.writeAccess()) parametersSetChannel.putVal(par); else System.out.println("setParameters() " + getRefName() + " has no permission to write.");
            newpar = parametersSetChannel.getValInt();
        } catch (ConnectionException e) {
            newpar = 100;
        } catch (GetException e) {
            newpar = 200;
        } catch (PutException e) {
            System.out.println(getRefName() + " got PutException " + e.getMessage());
        }
        return false;
    }

    public boolean setHV(double hv) {
        if (hv > 0) hv = -hv;
        if (hvSetChannel == null) hvSetChannel = ChannelFactory.defaultFactory().getChannel(getRefName() + ":DbgHVBias");
        hvSetChannel.connect();
        double newhv = 0;
        try {
            if (hvSetChannel.writeAccess()) hvSetChannel.putVal(hv); else System.out.println("setHV() " + getRefName() + " has no permission to write.");
            newhv = hvSetChannel.getValDbl();
        } catch (ConnectionException e) {
            newhv = 100;
        } catch (GetException e) {
            newhv = 200;
        } catch (PutException e) {
            System.out.println(getRefName() + " got PutException " + e.getMessage());
        }
        return false;
    }

    public void decreaseHV(double deltahv) {
        alterHV(deltahv, -1.0);
    }

    public void increaseHV(double deltahv) {
        alterHV(deltahv, 1.0);
    }

    public boolean alterHV(double deltahv, double sign) {
        if (deltahv < 0) deltahv = -deltahv;
        if (hvSetChannel == null) hvSetChannel = ChannelFactory.defaultFactory().getChannel(getRefName() + ":DbgHVBias");
        hvSetChannel.connect();
        double newhv = 0, oldhv = 0, resulthv = 0;
        try {
            oldhv = hvSetChannel.getValDbl();
            newhv = oldhv - sign * deltahv;
            if (newhv > 0) newhv = 0;
            if (hvSetChannel.writeAccess()) hvSetChannel.putVal(newhv); else System.out.println("alterHV() " + getRefName() + " has no permission to write.");
            resulthv = hvSetChannel.getValDbl();
        } catch (ConnectionException e) {
            newhv = 100;
        } catch (GetException e) {
            newhv = 200;
        } catch (PutException e) {
            System.out.println(getRefName() + " got PutException " + e.getMessage());
        }
        System.out.println("alterHV() " + getRefName() + " resulthv " + resulthv + " newhv " + newhv + " oldhv " + oldhv);
        return false;
    }

    public boolean setMPS(double mps) {
        if (mpsSetChannel == null) mpsSetChannel = ChannelFactory.defaultFactory().getChannel(getRefName() + ":DbgMPSPulseLossLimit");
        mpsSetChannel.connect();
        double newmps = 0;
        try {
            if (mpsSetChannel.writeAccess()) mpsSetChannel.putVal(mps); else System.out.println("setMPS() " + getRefName() + " has no permission to write.");
            newmps = mpsSetChannel.getValDbl();
        } catch (ConnectionException e) {
            newmps = -100;
        } catch (GetException e) {
            newmps = -200;
        } catch (PutException e) {
            System.out.println(getRefName() + " got PutException " + e.getMessage());
        }
        return false;
    }

    private Channel mpsSetChannel;

    private Channel hvSetChannel;

    private Channel parametersSetChannel;
}
