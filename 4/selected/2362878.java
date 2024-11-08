package gov.sns.tools.scan;

import java.util.*;
import java.awt.event.*;
import gov.sns.tools.plot.*;
import gov.sns.tools.scan.SecondEdition.BeamTrigger;

public class MeasuredValues implements Measurer {

    protected Vector<MeasuredValue> measuredValueV = new Vector<MeasuredValue>();

    protected Valuator valuator = null;

    private ScanStopper scanStopper = ScanStopper.getScanStopper();

    private AveragingController avgCntrl = null;

    private int invalidCountMax = 10;

    private ActionListener newSetOfMeasurementsListener = null;

    private ActionEvent newSetOfMeasurementsEvent = null;

    BeamTrigger beamTrigger = null;

    public MeasuredValues() {
        newSetOfMeasurementsEvent = new ActionEvent(this, 0, "newSetOfMeasurements");
    }

    public void addMeasuredValueInstance(MeasuredValue mVal) {
        if (!measuredValueV.contains(mVal)) {
            measuredValueV.add(mVal);
        }
    }

    public void removeMeasuredValueInstance(MeasuredValue mVal) {
        measuredValueV.removeElement(mVal);
    }

    public void removeAllMeasuredValues() {
        measuredValueV.clear();
    }

    public int getNumberOfMeasuredValues() {
        return measuredValueV.size();
    }

    public MeasuredValue getMeasuredValueInstance(int index) {
        if (index < 0 && index > measuredValueV.size()) {
            return null;
        }
        return measuredValueV.get(index);
    }

    public MeasuredValue getMeasuredValueInstance(String aliasIn) {
        String alias;
        for (int i = 0, nVals = measuredValueV.size(); i < nVals; i++) {
            alias = measuredValueV.get(i).getAlias();
            if (alias != null && alias.equals(aliasIn)) {
                return measuredValueV.get(i);
            }
        }
        return null;
    }

    public void setValuator(Valuator valuator) {
        this.valuator = valuator;
    }

    public void removeValuator() {
        valuator = null;
    }

    public Valuator getValuator() {
        return valuator;
    }

    public void setBeamTrigger(BeamTrigger beamTriggerIn) {
        beamTrigger = beamTriggerIn;
    }

    public void setAvrgCntrl(AveragingController avgCntrl) {
        this.avgCntrl = avgCntrl;
    }

    public void removeAvrgCntrl() {
        avgCntrl = null;
    }

    public AveragingController getAvrgCntrl() {
        return avgCntrl;
    }

    public void setMaxNumbInvalidMeasurements(int invalidCountMaxIn) {
        invalidCountMax = invalidCountMaxIn;
    }

    public void removeScanStopper() {
        scanStopper = null;
    }

    public void addNewSetOfMeasurementsListener(ActionListener newSetOfMeasurementsListenerIn) {
        newSetOfMeasurementsListener = newSetOfMeasurementsListenerIn;
    }

    public boolean measure() {
        int nAvrg = 1;
        double timeDelay = 0.0;
        if (avgCntrl != null) {
            nAvrg = avgCntrl.getAvgNumber();
            timeDelay = avgCntrl.getTimeDelay();
        }
        if (scanStopper != null) {
            if (!ScanStopper.getScanStopper().isRunning()) {
                return false;
            }
        }
        restoreIniState();
        int counter = 0;
        int invalidCount = 0;
        while (counter < nAvrg) {
            beamTrigger.makePulse();
            if (counter != 0 && !beamTrigger.isOn()) {
                try {
                    Thread.sleep((long) (timeDelay * 1000.0));
                } catch (InterruptedException e) {
                }
            }
            for (int i = 0, nVals = measuredValueV.size(); i < nVals; i++) {
                if (!(measuredValueV.get(i).measure())) {
                    return false;
                }
                if (scanStopper != null) {
                    if (!ScanStopper.getScanStopper().isRunning()) {
                        return false;
                    }
                }
            }
            if (scanStopper != null) {
                if (!ScanStopper.getScanStopper().isRunning()) {
                    return false;
                }
            }
            if (valuator != null) {
                if (valuator.validate()) {
                    counter++;
                    acceptMeasure();
                } else {
                    invalidCount++;
                    if (invalidCount > invalidCountMax) return false;
                }
            } else {
                counter++;
                acceptMeasure();
            }
        }
        return true;
    }

    public void addNewDataPoint(double xValue, Vector<IndependentValue> independVariablesV) {
        if (independVariablesV == null || independVariablesV.size() == 0) {
            addNewDataPoint(xValue);
            return;
        }
        double xValueRB = independVariablesV.lastElement().getValueRB();
        double yValue = 0.0;
        double sigma = 0.0;
        for (int i = 0, nVals = measuredValueV.size(); i < nVals; i++) {
            MeasuredValue mv = (measuredValueV.get(i));
            BasicGraphData gd = mv.getDataContainer(mv.getNumberOfDataContainers() - 1);
            if (gd != null) {
                yValue = mv.getMeasurement();
                sigma = mv.getMeasurementSigma();
                gd.addPoint(xValue, yValue, sigma);
                if (independVariablesV.lastElement().getChannelNameRB() != null) {
                    BasicGraphData gdRB = mv.getDataContainerRB(mv.getNumberOfDataContainersRB() - 1);
                    gdRB.addPoint(xValueRB, yValue, sigma);
                }
            }
        }
    }

    public void addNewDataPoint(double xValue) {
        double yValue = 0.0;
        double sigma = 0.0;
        for (int i = 0, nVals = measuredValueV.size(); i < nVals; i++) {
            MeasuredValue mv = (measuredValueV.get(i));
            if (mv.getNumberOfDataContainers() == 0) continue;
            BasicGraphData gd = mv.getDataContainer(mv.getNumberOfDataContainers() - 1);
            if (gd != null) {
                yValue = mv.getMeasurement();
                sigma = mv.getMeasurementSigma();
                gd.addPoint(xValue, yValue, sigma);
            }
        }
    }

    public void makeNewSetOfMeasurements(Vector<IndependentValue> independVariablesV) {
        IndependentValue indVal = null;
        if (independVariablesV != null && independVariablesV.size() > 0) {
            indVal = independVariablesV.lastElement();
        }
        for (int i = 0, nVals = measuredValueV.size(); i < nVals; i++) {
            MeasuredValue mv = (measuredValueV.get(i));
            mv.createNewDataContainer();
            BasicGraphData gd = mv.getDataContainer(mv.getNumberOfDataContainers() - 1);
            BasicGraphData gdRB = null;
            if (indVal != null) {
                if (indVal.getChannelNameRB() != null) {
                    mv.createNewDataContainerRB();
                    gdRB = mv.getDataContainerRB(mv.getNumberOfDataContainersRB() - 1);
                }
            }
            if (independVariablesV == null) continue;
            for (int j = 0, nPar = independVariablesV.size(); j < nPar; j++) {
                IndependentValue indVal_tmp = (independVariablesV.get(j));
                if (indVal_tmp != null && indVal_tmp.getChannelName() != null) {
                    gd.setGraphProperty(indVal_tmp.getChannelName(), new Double(indVal_tmp.getCurrentValue()));
                }
                if (indVal_tmp != null && indVal_tmp.getChannelNameRB() != null) {
                    gd.setGraphProperty(indVal_tmp.getChannelNameRB(), new Double(indVal_tmp.getCurrentValueRB()));
                }
                if (gdRB != null) {
                    if (indVal_tmp != null && indVal_tmp.getChannelName() != null) {
                        gdRB.setGraphProperty(indVal_tmp.getChannelName(), new Double(indVal_tmp.getCurrentValue()));
                    }
                    if (indVal_tmp != null && indVal_tmp.getChannelNameRB() != null) {
                        gdRB.setGraphProperty(indVal_tmp.getChannelNameRB(), new Double(indVal_tmp.getCurrentValueRB()));
                    }
                }
            }
        }
        if (newSetOfMeasurementsListener != null) {
            newSetOfMeasurementsListener.actionPerformed(newSetOfMeasurementsEvent);
        }
    }

    protected void restoreIniState() {
        for (int i = 0, nVals = measuredValueV.size(); i < nVals; i++) {
            measuredValueV.get(i).restoreIniState();
        }
    }

    protected void acceptMeasure() {
        for (int i = 0, nVals = measuredValueV.size(); i < nVals; i++) {
            measuredValueV.get(i).acceptMeasure();
        }
    }
}
