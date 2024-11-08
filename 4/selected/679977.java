package gov.sns.apps.sclcavfield.utils;

import java.awt.event.*;
import javax.swing.*;
import java.awt.*;
import gov.sns.ca.*;
import gov.sns.tools.scan.SecondEdition.*;
import gov.sns.tools.plot.*;
import gov.sns.tools.swing.*;
import gov.sns.tools.xml.*;

/**
 *  The object describes one row in the CavFieldsTable instance
 *
 *@author    shishlo
 */
public class CavFieldsValue {

    private static int countId = 0;

    private UpdatingEventController uc = null;

    private String cavNameHTML = "none";

    private String cavName = "none";

    private volatile boolean active = false;

    private volatile boolean fieldsChanged = false;

    private FortranNumberFormat fmt = new FortranNumberFormat("G10.4");

    private FortranNumberFormat ascii_frmt = new FortranNumberFormat("G10.4", true);

    private String[] fieldStrs = new String[6];

    private String[] fieldStrsHTML = new String[6];

    private volatile double[] fields = new double[6];

    String[] powerPVnames = { "", "", "", "", "" };

    private MonitoredPV[] powerMPVs = new MonitoredPV[5];

    double r_QL = 0.;

    volatile double Q_L = 0.;

    double Q_E = 0.;

    double Q_E_HOMA = 0.;

    double Q_E_HOMB = 0.;

    double w = 0.;

    private double oneMV = 1.0e+6;

    private String pulseEndPV_name = "";

    private String ratePV_name = "";

    private String cavVPV_name = "";

    private String lenPV_name = "";

    private String switchHBOPV_name = "";

    private String adcLossPV_name = "";

    String halfBandWidthPV_name = "";

    private String ampPV_name = "";

    private String timPV_name = "";

    private String rawPV_name = "";

    private MonitoredPV pulseEndPV = null;

    private MonitoredPV ratePV = null;

    private MonitoredPV cavVPV = null;

    private MonitoredPV lenPV = null;

    private MonitoredPV switchHBOPV = null;

    private MonitoredPV adcLossPV = null;

    private MonitoredPV halfBandWidthPV = null;

    private BasicGraphData ampVsTimeGD = new BasicGraphData();

    BasicGraphData ampVsTimeLogGD = new BasicGraphData();

    BasicGraphData ampVsTimeLogFitGD = new BasicGraphData();

    CurveData rawCD = new CurveData();

    double integrationStart = 0.;

    JTextArea resultsText = new JTextArea();

    private double stored_energy = 0.;

    private String asciiFileText = "";

    private boolean analysisReady = false;

    /**
	 *  Constructor for the CavFieldsValue object
	 */
    public CavFieldsValue() {
        for (int i = 0; i < 6; i++) {
            fields[i] = 0.;
            fieldStrs[i] = " --- ";
            fieldStrsHTML[i] = "<html><body><font color= RED>" + fieldStrs[i] + "</font></body></html>";
        }
        for (int i = 0; i < 5; i++) {
            powerMPVs[i] = MonitoredPV.getMonitoredPV("CFV_" + countId);
            countId++;
        }
        pulseEndPV = MonitoredPV.getMonitoredPV("CFV_" + countId);
        countId++;
        ratePV = MonitoredPV.getMonitoredPV("CFV_" + countId);
        countId++;
        cavVPV = MonitoredPV.getMonitoredPV("CFV_" + countId);
        countId++;
        lenPV = MonitoredPV.getMonitoredPV("CFV_" + countId);
        countId++;
        switchHBOPV = MonitoredPV.getMonitoredPV("CFV_" + countId);
        countId++;
        adcLossPV = MonitoredPV.getMonitoredPV("CFV_" + countId);
        countId++;
        halfBandWidthPV = MonitoredPV.getMonitoredPV("CFV_" + countId);
        countId++;
        ActionListener powerMPVsListener = new ActionListener() {

            public void actionPerformed(ActionEvent evnt) {
                if (!active) {
                    return;
                }
                MonitoredPV mpv = (MonitoredPV) evnt.getSource();
                for (int i = 0; i < 5; i++) {
                    if (powerMPVs[i].equals(mpv)) {
                        if (mpv.isGood()) {
                            double pwr = mpv.getValue();
                            fields[i] = getField(pwr, i);
                            String strVal = fmt.format(fields[i]);
                            fieldStrs[i] = strVal;
                            fieldStrsHTML[i] = "<html><body><font color= green>" + strVal + "</font></body></html>";
                        } else {
                            fields[i] = 0.;
                            fieldStrs[i] = " --- ";
                            fieldStrsHTML[i] = "<html><body><font color= RED>" + fieldStrs[i] + "</font></body></html>";
                        }
                        fieldsChanged = true;
                        uc.update();
                    }
                }
            }
        };
        for (int i = 0; i < 5; i++) {
            powerMPVs[i].addValueListener(powerMPVsListener);
        }
        ActionListener q_l_listener = new ActionListener() {

            public void actionPerformed(ActionEvent evnt) {
                if (!active) {
                    return;
                }
                MonitoredPV mpv = halfBandWidthPV;
                if (mpv.isGood()) {
                    double halfBandWidth = mpv.getValue();
                    if (halfBandWidth != 0.) {
                        Q_L = 4.025e+5 / Math.abs(halfBandWidth);
                    } else {
                        Q_L = 0.;
                    }
                    fieldsChanged = true;
                    uc.update();
                }
            }
        };
        halfBandWidthPV.addValueListener(q_l_listener);
        halfBandWidthPV.addStateListener(q_l_listener);
        ampVsTimeLogGD.setDrawLinesOn(false);
        ampVsTimeLogGD.setDrawPointsOn(true);
        ampVsTimeLogFitGD.setDrawLinesOn(true);
        ampVsTimeLogFitGD.setDrawPointsOn(false);
        ampVsTimeLogGD.setGraphProperty("Legend", "Log of Amplitude Wf  ");
        ampVsTimeLogFitGD.setGraphProperty("Legend", "Fitting");
        ampVsTimeLogGD.setGraphColor(Color.red);
        ampVsTimeLogFitGD.setGraphColor(Color.blue);
        ampVsTimeLogGD.setGraphPointSize(5);
        ampVsTimeLogFitGD.setLineThick(2);
        rawCD.setColor(Color.blue);
        rawCD.setLineWidth(1);
        ampVsTimeLogGD.setImmediateContainerUpdate(false);
        ampVsTimeLogFitGD.setImmediateContainerUpdate(false);
        resultsText.setText("No analysis has been performed.");
    }

    /**
	 *  Updates all fields variables
	 */
    public void updateFields() {
        if (!active) {
            return;
        }
        for (int i = 0; i < 5; i++) {
            MonitoredPV mpv = powerMPVs[i];
            if (mpv.isGood()) {
                double pwr = mpv.getValue();
                fields[i] = getField(pwr, i);
                String strVal = fmt.format(fields[i]);
                fieldStrs[i] = strVal;
                fieldStrsHTML[i] = "<html><body><font color= green>" + strVal + "</font></body></html>";
            } else {
                fields[i] = 0.;
                fieldStrs[i] = " --- ";
                fieldStrsHTML[i] = "<html><body><font color= RED>" + fieldStrs[i] + "</font></body></html>";
            }
        }
        MonitoredPV mpv = halfBandWidthPV;
        if (mpv.isGood()) {
            double halfBandWidth = mpv.getValue();
            if (halfBandWidth != 0.) {
                Q_L = 4.025e+5 / Math.abs(halfBandWidth);
            } else {
                Q_L = 0.;
            }
        }
        fieldsChanged = true;
        uc.update();
    }

    /**
	 *  Sets the monitoring state to the row object
	 */
    public void setMonitoring() {
        if (!active) {
            for (int i = 0; i < 5; i++) {
                MonitoredPV mpv = powerMPVs[i];
                if (mpv.isGood()) {
                    mpv.stopMonitor();
                }
            }
            if (pulseEndPV.isGood()) {
                pulseEndPV.stopMonitor();
            }
            if (ratePV.isGood()) {
                ratePV.stopMonitor();
            }
            if (cavVPV.isGood()) {
                cavVPV.stopMonitor();
            }
            if (lenPV.isGood()) {
                lenPV.stopMonitor();
            }
            if (switchHBOPV.isGood()) {
                switchHBOPV.stopMonitor();
            }
            if (adcLossPV.isGood()) {
                adcLossPV.stopMonitor();
            }
            if (halfBandWidthPV.isGood()) {
                halfBandWidthPV.stopMonitor();
            }
        } else {
            for (int i = 0; i < 5; i++) {
                MonitoredPV mpv = powerMPVs[i];
                mpv.startMonitor();
            }
            pulseEndPV.startMonitor();
            ratePV.startMonitor();
            cavVPV.startMonitor();
            lenPV.startMonitor();
            switchHBOPV.startMonitor();
            adcLossPV.startMonitor();
            halfBandWidthPV.startMonitor();
        }
    }

    /**
	 *  Sets the update controller to the CavFieldsValue object
	 *
	 *@param  uc  The new update controller
	 */
    public void setUpdateCntrl(UpdatingEventController uc) {
        this.uc = uc;
    }

    /**
	 *  Returns true if the row changed
	 *
	 *@return    If the row changed - true of false
	 */
    public boolean fieldsChanged() {
        return fieldsChanged;
    }

    /**
	 *  Sets true if the row changed
	 *
	 *@param  fieldsChanged  If the row changed - true of false
	 */
    public void fieldsChanged(boolean fieldsChanged) {
        this.fieldsChanged = fieldsChanged;
    }

    /**
	 *  Returns the field value representation (string) of the CavFieldsValue
	 *  object
	 *
	 *@param  column  The column index
	 *@return         The string as a field value representation
	 */
    public String getField(int column) {
        return fieldStrsHTML[column];
    }

    /**
	 *  Returns the field value
	 *
	 *@param  column  The column inde
	 *@return         The field value
	 */
    public double getFieldValue(int column) {
        return fields[column];
    }

    /**
	 *  Returns true if this cell is hearing the EPICS
	 *
	 *@return    The on or off
	 */
    public boolean isOn() {
        return active;
    }

    /**
	 *  Sets true if this cell is hearing the EPICS
	 *
	 *@param  active  The true or fasle
	 */
    public void isOn(boolean active) {
        this.active = active;
        setMonitoring();
    }

    /**
	 *  Reads parameters and pv-names from xml-data conatiner
	 *
	 *@param  da  The xml-data conatiner
	 */
    public void readParams(XmlDataAdaptor da) {
        resultsText.setText("No analysis has been performed.");
        java.util.Iterator<XmlDataAdaptor> dataIt = da.childAdaptorIterator("POWER_PV");
        while (dataIt.hasNext()) {
            XmlDataAdaptor powerPV_da = dataIt.next();
            int ind = powerPV_da.intValue("index");
            powerPVnames[ind] = powerPV_da.stringValue("name");
            powerMPVs[ind].setChannelNameQuietly(powerPVnames[ind]);
        }
        dataIt = da.childAdaptorIterator("E_FIELD");
        while (dataIt.hasNext()) {
            XmlDataAdaptor eField_da = dataIt.next();
            int ind = eField_da.intValue("index");
            fields[ind] = eField_da.doubleValue("val");
            fieldStrs[ind] = eField_da.stringValue("val_string");
            fieldStrsHTML[ind] = "<html><body><font color= RED>" + fieldStrs[ind] + "</font></body></html>";
        }
        XmlDataAdaptor param_da = da.childAdaptor("Parameter_r_QL");
        r_QL = param_da.doubleValue("value");
        param_da = da.childAdaptor("Parameter_Q_L");
        Q_L = param_da.doubleValue("value");
        param_da = da.childAdaptor("Parameter_Q_E");
        Q_E = param_da.doubleValue("value");
        param_da = da.childAdaptor("Parameter_Q_E_HOMA");
        Q_E_HOMA = param_da.doubleValue("value");
        param_da = da.childAdaptor("Parameter_Q_E_HOMB");
        Q_E_HOMB = param_da.doubleValue("value");
        param_da = da.childAdaptor("Parameter_w");
        w = (805.0 * 1.0E+6) * 2.0 * Math.PI;
        XmlDataAdaptor pulseEndPV_da = da.childAdaptor("pulseEndPV");
        pulseEndPV_name = pulseEndPV_da.stringValue("name");
        pulseEndPV.setChannelNameQuietly(pulseEndPV_name);
        XmlDataAdaptor ratePV_da = da.childAdaptor("ratePV");
        ratePV_name = ratePV_da.stringValue("name");
        ratePV.setChannelNameQuietly(ratePV_name);
        XmlDataAdaptor cavVPV_da = da.childAdaptor("cavVPV");
        cavVPV_name = cavVPV_da.stringValue("name");
        cavVPV.setChannelNameQuietly(cavVPV_name);
        XmlDataAdaptor ampPV_da = da.childAdaptor("ampPV");
        ampPV_name = ampPV_da.stringValue("name");
        XmlDataAdaptor timPV_da = da.childAdaptor("timPV");
        timPV_name = timPV_da.stringValue("name");
        XmlDataAdaptor switchHBOPV_da = da.childAdaptor("switchHBOPV");
        switchHBOPV_name = switchHBOPV_da.stringValue("name");
        switchHBOPV.setChannelNameQuietly(switchHBOPV_name);
        XmlDataAdaptor lenPV_da = da.childAdaptor("lenPV");
        lenPV_name = lenPV_da.stringValue("name");
        lenPV.setChannelNameQuietly(lenPV_name);
        XmlDataAdaptor rawPV_da = da.childAdaptor("rawPV");
        rawPV_name = rawPV_da.stringValue("name");
        XmlDataAdaptor adcLossPV_da = da.childAdaptor("adcLossPV");
        adcLossPV_name = adcLossPV_da.stringValue("name");
        adcLossPV.setChannelNameQuietly(adcLossPV_name);
        XmlDataAdaptor halfBandWidthPV_da = da.childAdaptor("halfBandWidthPV");
        halfBandWidthPV_name = halfBandWidthPV_da.stringValue("name");
        halfBandWidthPV.setChannelNameQuietly(halfBandWidthPV_name);
        XmlDataAdaptor analysisText_da = da.childAdaptor("analysisText");
        if (analysisText_da != null) {
            resultsText.setText(null);
            String txt = analysisText_da.stringValue("txt");
            txt = txt.replaceAll("endOfLine", System.getProperty("line.separator"));
            resultsText.setText(txt);
        }
        stored_energy = 0.;
        XmlDataAdaptor storedEnergy_da = da.childAdaptor("stored_energy");
        if (storedEnergy_da != null) {
            stored_energy = storedEnergy_da.doubleValue("value");
        }
        asciiFileText = cavName;
        XmlDataAdaptor asciiFileText_da = da.childAdaptor("asciiFileText");
        if (asciiFileText_da != null) {
            asciiFileText = asciiFileText_da.stringValue("txt");
        }
        boolean active_old = active;
        XmlDataAdaptor state_da = da.childAdaptor("ACTIVE_STATE");
        if (state_da != null) {
            active = state_da.booleanValue("state");
        } else {
            active = false;
        }
        if (active_old == true) {
            isOn(active);
        }
    }

    /**
	 *  Writes parameters and pv-names to the xml-data conatiner
	 *
	 *@param  da  The xml-data conatiner
	 */
    public void writeParams(XmlDataAdaptor da) {
        XmlDataAdaptor state_da = da.createChild("ACTIVE_STATE");
        state_da.setValue("state", active);
        XmlDataAdaptor powerPV_da = null;
        for (int ind = 0; ind < 5; ind++) {
            powerPV_da = da.createChild("POWER_PV");
            powerPV_da.setValue("index", ind);
            powerPV_da.setValue("name", powerPVnames[ind]);
        }
        XmlDataAdaptor eField_da = null;
        for (int ind = 0; ind < 6; ind++) {
            eField_da = da.createChild("E_FIELD");
            eField_da.setValue("index", ind);
            eField_da.setValue("val", fields[ind]);
            eField_da.setValue("val_string", fieldStrs[ind]);
        }
        XmlDataAdaptor param_da = da.createChild("Parameter_r_QL");
        param_da.setValue("value", r_QL);
        param_da = da.createChild("Parameter_Q_L");
        param_da.setValue("value", Q_L);
        param_da = da.createChild("Parameter_Q_E");
        param_da.setValue("value", Q_E);
        param_da = da.createChild("Parameter_Q_E_HOMA");
        param_da.setValue("value", Q_E_HOMA);
        param_da = da.createChild("Parameter_Q_E_HOMB");
        param_da.setValue("value", Q_E_HOMB);
        param_da = da.createChild("Parameter_w");
        param_da.setValue("value", w);
        XmlDataAdaptor pulseEndPV_da = da.createChild("pulseEndPV");
        pulseEndPV_da.setValue("name", pulseEndPV_name);
        XmlDataAdaptor ratePV_da = da.createChild("ratePV");
        ratePV_da.setValue("name", ratePV_name);
        XmlDataAdaptor cavVPV_da = da.createChild("cavVPV");
        cavVPV_da.setValue("name", cavVPV_name);
        XmlDataAdaptor ampPV_da = da.createChild("ampPV");
        ampPV_da.setValue("name", ampPV_name);
        XmlDataAdaptor timPV_da = da.createChild("timPV");
        timPV_da.setValue("name", timPV_name);
        XmlDataAdaptor switchHBOPV_da = da.createChild("switchHBOPV");
        switchHBOPV_da.setValue("name", switchHBOPV_name);
        XmlDataAdaptor lenPV_da = da.createChild("lenPV");
        lenPV_da.setValue("name", lenPV_name);
        XmlDataAdaptor rawPV_da = da.createChild("rawPV");
        rawPV_da.setValue("name", rawPV_name);
        XmlDataAdaptor adcLossPV_da = da.createChild("adcLossPV");
        adcLossPV_da.setValue("name", adcLossPV_name);
        XmlDataAdaptor halfBandWidthPV_da = da.createChild("halfBandWidthPV");
        halfBandWidthPV_da.setValue("name", halfBandWidthPV_name);
        XmlDataAdaptor analysisText_da = da.createChild("analysisText");
        String txt = resultsText.getText();
        txt = txt.replaceAll(System.getProperty("line.separator"), "endOfLine");
        analysisText_da.setValue("txt", txt);
        if (isAnalysisReady()) {
            XmlDataAdaptor storedEnergy_da = da.createChild("stored_energy");
            storedEnergy_da.setValue("value", stored_energy);
            XmlDataAdaptor asciiFileText_da = da.createChild("asciiFileText");
            asciiFileText_da.setValue("txt", asciiFileText);
        }
    }

    /**
	 *  Sets the name of the cavity (e.g. "01a")
	 *
	 *@param  cavName  The new name
	 */
    public void setName(String cavName) {
        this.cavName = cavName;
        cavNameHTML = "<html><body><font color= blue>" + cavName + "</font></body></html>";
    }

    /**
	 *  Returns the name of the cavity (e.g. "01a")
	 *
	 *@return    The name
	 */
    public String getName() {
        return cavName;
    }

    /**
	 *  Returns the name of the cavity as a HTML string
	 *
	 *@return    The name as a HTML string
	 */
    public String getNameHTML() {
        return cavNameHTML;
    }

    /**
	 *  Updates the Q_L parameter from the halfBandWidthPV
	 */
    public void updateQ_L() {
    }

    /**
	 *  Returns the field calculated with different formulas
	 *
	 *@param  pwr   The power
	 *@param  type  The formula index
	 *@return       The field value
	 */
    private double getField(double pwr, int type) {
        double res = 0.;
        switch(type) {
            case 0:
                res = Math.sqrt(4.0 * r_QL * Q_L * pwr * 1000.0) / 1.0e6;
                break;
            case 1:
                res = Math.sqrt(4.0 * r_QL * Q_L * pwr * 1000.0) / 1.0e6;
                break;
            case 2:
                res = Math.sqrt(r_QL * Q_E * pwr) / 1.0e6;
                break;
            case 3:
                res = Math.sqrt(r_QL * Q_E_HOMA * pwr) / 1.0e6;
                break;
            case 4:
                res = Math.sqrt(r_QL * Q_E_HOMB * pwr) / 1.0e6;
                break;
            case 5:
                res = Math.sqrt(r_QL * w * pwr) / 1.0e6;
                break;
            default:
                res = 0.;
                break;
        }
        return res;
    }

    /**
	 *  Removes the monitored PV.
	 */
    @Override
    protected void finalize() {
        for (int i = 0; i < 5; i++) {
            MonitoredPV.removeMonitoredPV(powerMPVs[i]);
        }
        MonitoredPV.removeMonitoredPV(pulseEndPV);
        MonitoredPV.removeMonitoredPV(ratePV);
        MonitoredPV.removeMonitoredPV(cavVPV);
        MonitoredPV.removeMonitoredPV(lenPV);
        MonitoredPV.removeMonitoredPV(switchHBOPV);
        MonitoredPV.removeMonitoredPV(adcLossPV);
        MonitoredPV.removeMonitoredPV(halfBandWidthPV);
    }

    /**
	 *  Performs analysis of the stored energy in the SC RF cavity
	 *
	 *@return    The success of the analysis
	 */
    protected boolean makeStoredEnergyAnalysis() {
        analysisReady = false;
        ampVsTimeGD.removeAllPoints();
        ampVsTimeLogGD.removeAllPoints();
        ampVsTimeLogFitGD.removeAllPoints();
        rawCD.clear();
        integrationStart = 0.;
        resultsText.setText(null);
        fields[5] = 0.;
        fieldStrs[5] = " --- ";
        fieldStrsHTML[5] = "<html><body><font color= RED>" + fieldStrs[5] + "</font></body></html>";
        fieldsChanged = true;
        uc.update();
        Channel ampPV = ChannelFactory.defaultFactory().getChannel(ampPV_name);
        Channel timPV = ChannelFactory.defaultFactory().getChannel(timPV_name);
        Channel rawPV = ChannelFactory.defaultFactory().getChannel(rawPV_name);
        double pulseEnd = pulseEndPV.getValue();
        double rate = ratePV.getValue();
        double cavV = cavVPV.getValue();
        double len = lenPV.getValue();
        double loss = adcLossPV.getValue();
        try {
            double[] ampArr = ampPV.getArrDbl();
            double[] timArr = timPV.getArrDbl();
            int nP = Math.min(ampArr.length, timArr.length);
            if (nP < 3) {
                resultsText.append("Unable to connect channels:");
                resultsText.append(System.getProperty("line.separator"));
                resultsText.append(ampPV_name);
                resultsText.append(System.getProperty("line.separator"));
                resultsText.append(timPV_name);
                resultsText.append(System.getProperty("line.separator"));
                return analysisReady;
            }
            double st = timArr[2] - timArr[1];
            for (int i = 0; i < nP; i++) {
                if (timArr[i] > (pulseEnd + 1.0) && i < Math.floor(pulseEnd + 1.0 + len / st)) {
                    double amp = ampArr[i];
                    double t = timArr[i] - (pulseEnd + 1.0);
                    ampVsTimeGD.addPoint(t, amp);
                }
            }
        } catch (ConnectionException exp) {
            resultsText.append("Unable to connect channels:");
            resultsText.append(System.getProperty("line.separator"));
            resultsText.append(ampPV_name);
            resultsText.append(System.getProperty("line.separator"));
            resultsText.append(timPV_name);
            resultsText.append(System.getProperty("line.separator"));
            return analysisReady;
        } catch (GetException exp) {
            resultsText.append("Unable to connect channels:");
            resultsText.append(System.getProperty("line.separator"));
            resultsText.append(ampPV_name);
            resultsText.append(System.getProperty("line.separator"));
            resultsText.append(timPV_name);
            resultsText.append(System.getProperty("line.separator"));
            return analysisReady;
        }
        for (int i = 0; i < ampVsTimeGD.getNumbOfPoints(); i++) {
            double val = ampVsTimeGD.getY(i);
            val = Math.abs(val);
            if (val != 0.) {
                val = Math.log(val);
                ampVsTimeLogGD.addPoint(ampVsTimeGD.getX(i), val);
            }
        }
        int order = 1;
        double[][] fitCoeff = GraphDataOperations.polynomialFit(ampVsTimeLogGD, ampVsTimeLogGD.getMinX(), ampVsTimeLogGD.getMaxX(), order);
        for (int i = 0; i < ampVsTimeLogGD.getNumbOfPoints(); i++) {
            double x = ampVsTimeLogGD.getX(i);
            double val = fitCoeff[0][0] + x * fitCoeff[0][1];
            ampVsTimeLogFitGD.addPoint(x, val);
        }
        double tau = -1 / fitCoeff[0][1];
        double tau_err = Math.abs(tau * fitCoeff[1][1] / fitCoeff[0][1]);
        double Q = 2 * 3.1415926 * 805.0 * tau / 2;
        double bw_kHz = (805 / Q) * 1000.0 / 2;
        fitCoeff[0][0] = Math.exp(fitCoeff[0][0]);
        fitCoeff[1][0] = fitCoeff[0][0] * fitCoeff[1][0];
        resultsText.append("===Field_WfA Fitting===");
        resultsText.append(System.getProperty("line.separator"));
        resultsText.append("cavV= " + fmt.format(cavV) + " [MV/m]");
        resultsText.append(System.getProperty("line.separator"));
        resultsText.append("Q= " + fmt.format(Q));
        resultsText.append(System.getProperty("line.separator"));
        resultsText.append("BW= " + fmt.format(bw_kHz) + " [kHz]");
        resultsText.append(System.getProperty("line.separator"));
        resultsText.append("wf(t) = A0*exp(-t/tau) ");
        resultsText.append(System.getProperty("line.separator"));
        resultsText.append("A0=" + fmt.format(fitCoeff[0][0]) + " +- " + fmt.format(fitCoeff[1][0]));
        resultsText.append(System.getProperty("line.separator"));
        resultsText.append("tau[us]=" + fmt.format(tau) + " +- " + fmt.format(tau_err));
        resultsText.append(System.getProperty("line.separator"));
        resultsText.append("=======================");
        resultsText.append(System.getProperty("line.separator"));
        double bw = 2 * bw_kHz / 1000.0;
        double Ql = 805 / bw;
        tau = 1.0 / (2 * Math.PI * bw);
        try {
            switchHBOPV.getChannel().putVal(3);
        } catch (ConnectionException exp) {
            resultsText.append("Unable to set PV:" + switchHBOPV.getChannelName());
            return analysisReady;
        } catch (PutException exp) {
            resultsText.append("Unable to set PV:" + switchHBOPV.getChannelName());
            return analysisReady;
        }
        double[] rawArr = null;
        try {
            rawArr = rawPV.getArrDbl();
            for (int i = 0; i < rawArr.length; i++) {
                rawCD.addPoint(2.0 * i, rawArr[i]);
            }
        } catch (ConnectionException exp) {
            resultsText.append("Unable to connect channel:");
            resultsText.append(System.getProperty("line.separator"));
            resultsText.append(rawPV_name);
            resultsText.append(System.getProperty("line.separator"));
            return analysisReady;
        } catch (GetException exp) {
            resultsText.append("Unable to connect channel:");
            resultsText.append(System.getProperty("line.separator"));
            resultsText.append(rawPV_name);
            resultsText.append(System.getProperty("line.separator"));
            return analysisReady;
        }
        int nIter = 2;
        for (int iii = 0; iii < nIter; iii++) {
            try {
                Thread.sleep(600);
            } catch (InterruptedException exp) {
                return analysisReady;
            }
            try {
                rawArr = rawPV.getArrDbl();
                for (int i = 0; i < rawArr.length; i++) {
                    double val = rawCD.getY(i);
                    rawCD.setPoint(i, rawCD.getX(i), val + rawArr[i]);
                }
            } catch (ConnectionException exp) {
                resultsText.append("Unable to connect channel:");
                resultsText.append(System.getProperty("line.separator"));
                resultsText.append(rawPV_name);
                resultsText.append(System.getProperty("line.separator"));
                return analysisReady;
            } catch (GetException exp) {
                resultsText.append("Unable to connect channel:");
                resultsText.append(System.getProperty("line.separator"));
                resultsText.append(rawPV_name);
                resultsText.append(System.getProperty("line.separator"));
                return analysisReady;
            }
        }
        for (int i = 0; i < rawArr.length; i++) {
            double val = rawCD.getY(i) / (nIter + 1);
            val = val * 0.0625 - 52.2;
            val = Math.pow(10., (val - 60 + loss) / 10.);
            rawCD.setPoint(i, rawCD.getX(i), val);
        }
        double storedEnergy = 0.;
        int max_index = (int) Math.floor(0.9 * pulseEnd / 2.0);
        int nZero = Math.min(max_index + 1, rawCD.getSize());
        for (int i = 0; i < nZero; i++) {
            rawCD.setPoint(i, rawCD.getX(i), 0.);
        }
        double p0 = -Double.MAX_VALUE;
        int nP = rawCD.getSize();
        int time_0_ind = 0;
        for (int i = 0; i < nP; i++) {
            if (p0 <= rawCD.getY(i)) {
                p0 = rawCD.getY(i);
            }
        }
        for (int i = 0; i < nP; i++) {
            if (p0 == rawCD.getY(i)) {
                time_0_ind = i;
                break;
            }
        }
        rawCD.findMinMax();
        integrationStart = time_0_ind * 2.0;
        double U = p0 * tau;
        U = U / 1000.;
        double Un = 0.;
        int i_start = time_0_ind;
        i_start = Math.max(0, i_start);
        int i_stop = nP - 1;
        for (int i = i_start; i <= i_stop; i++) {
            Un += rawCD.getY(i);
        }
        if (i_start < i_stop) {
            Un -= 0.5 * rawCD.getY(i_start);
        }
        Un -= 0.5 * rawCD.getY(i_stop);
        Un *= (2.0 / 1000.0);
        stored_energy = Un;
        resultsText.append("Un[J] = " + fmt.format(Un));
        resultsText.append(System.getProperty("line.separator"));
        resultsText.append("U[J] = " + fmt.format(U));
        resultsText.append(System.getProperty("line.separator"));
        resultsText.append("=======================");
        resultsText.append(System.getProperty("line.separator"));
        resultsText.append("ADCSnap0_Pwr = " + fmt.format(powerMPVs[2].getValue()));
        resultsText.append(System.getProperty("line.separator"));
        resultsText.append("ADCSnap1_Pwr = " + fmt.format(powerMPVs[0].getValue()));
        resultsText.append(System.getProperty("line.separator"));
        resultsText.append("ADCSnap2_Pwr = " + fmt.format(powerMPVs[1].getValue()));
        resultsText.append(System.getProperty("line.separator"));
        resultsText.append("ADCSnap3_Pwr = " + fmt.format(powerMPVs[3].getValue()));
        resultsText.append(System.getProperty("line.separator"));
        resultsText.append("ADCSnap4_Pwr = " + fmt.format(powerMPVs[4].getValue()));
        resultsText.append(System.getProperty("line.separator"));
        resultsText.append("=======================");
        resultsText.append(System.getProperty("line.separator"));
        storedEnergy = Un;
        fields[5] = getField(storedEnergy, 5);
        String strVal = fmt.format(fields[5]);
        fieldStrs[5] = strVal;
        fieldStrsHTML[5] = "<html><body><font color= green>" + strVal + "</font></body></html>";
        fieldsChanged = true;
        uc.update();
        asciiFileText = makeASCIIFileLine();
        analysisReady = true;
        return analysisReady;
    }

    /**
	 *  Returns true if analysis is ready and false otherwise
	 *
	 *@return    The analysis ready value
	 */
    public boolean isAnalysisReady() {
        return analysisReady;
    }

    /**
	 *  Sets true if analysis is ready and false otherwise
	 *
	 *@param  analysisReady  The analysis ready value
	 */
    public void analysisReady(boolean analysisReady) {
        this.analysisReady = analysisReady;
    }

    public static String asciiFileHeader() {
        String txt = "";
        txt = txt + "Cav. Name ";
        txt = txt + "   Q_L    ";
        txt = txt + "  Cavity Field  ";
        txt = txt + "   Store E [J]  ";
        txt = txt + "   Fwrd. Pwr.     ";
        txt = txt + "   Refl. Pwr.     ";
        txt = txt + " Cav. Field Prb ";
        txt = txt + "   Field HOMA   ";
        txt = txt + "   Field HOMB   ";
        return txt;
    }

    public String getASCIIFileLine() {
        return asciiFileText;
    }

    private String makeASCIIFileLine() {
        String txt = cavName;
        txt = txt + " ";
        txt = txt + ascii_frmt.format(Q_L);
        txt = txt + "     ";
        txt = txt + ascii_frmt.format(cavVPV.getValue());
        txt = txt + "       ";
        txt = txt + ascii_frmt.format(stored_energy);
        txt = txt + "       ";
        txt = txt + ascii_frmt.format(powerMPVs[0].getValue());
        txt = txt + "       ";
        txt = txt + ascii_frmt.format(powerMPVs[1].getValue());
        txt = txt + "       ";
        txt = txt + ascii_frmt.format(fields[2]);
        txt = txt + "       ";
        txt = txt + ascii_frmt.format(fields[3]);
        txt = txt + "       ";
        txt = txt + ascii_frmt.format(fields[4]);
        return txt;
    }
}
