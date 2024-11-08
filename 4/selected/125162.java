package gov.sns.tools.scan;

import gov.sns.ca.*;
import javax.swing.*;
import java.text.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import gov.sns.tools.plot.*;

public class MeasuredValue {

    protected Channel ch_ = null;

    private ScanStopper scanStopper = ScanStopper.getScanStopper();

    protected double sigma = 0.0;

    protected double currValue = 0.0;

    protected volatile double currValueMonitor = 0.0;

    protected double sumValues = 0.0;

    protected double sumValues2 = 0.0;

    protected String alias = null;

    protected int nMeasurements = 0;

    protected Vector<ActionListener> actionListenersV = new Vector<ActionListener>();

    protected TransformationFunction transFunc = null;

    protected Valuator valuator = null;

    protected MeasuredValue offSetVal = null;

    public JTextField currValueTextField = new JTextField(5);

    public JTextField meanValueTextField = new JTextField(5);

    public DecimalFormat valueFormat = new DecimalFormat("0.00E0");

    protected HashMap<Object, Object> propertyMap = new HashMap<Object, Object>();

    protected Vector<BasicGraphData> graphDataV = new Vector<BasicGraphData>();

    protected Vector<BasicGraphData> graphDataRBV = new Vector<BasicGraphData>();

    protected Vector<BasicGraphData> graphDataUnwrappedV = new Vector<BasicGraphData>();

    protected Vector<BasicGraphData> graphDataUnwrappedRBV = new Vector<BasicGraphData>();

    protected boolean generateUnwrap = false;

    protected BasicGraphData extGraphData = null;

    protected BasicGraphData extGraphDataRB = null;

    private IEventSinkValue callBack = null;

    private static HashMap<String, Void> badChannelNames = new HashMap<String, Void>();

    public MeasuredValue() {
        this(null);
    }

    public MeasuredValue(String chanName) {
        if (chanName != null && badChannelNames.containsKey(chanName) == false) {
            callBack = new IEventSinkValue() {

                public void eventValue(ChannelRecord record, Channel chan) {
                    currValueMonitor = record.doubleValue();
                }
            };
            ch_ = ChannelFactory.defaultFactory().getChannel(chanName);
            try {
                ch_.addMonitorValue(callBack, Monitor.VALUE);
            } catch (ConnectionException e) {
                badChannelNames.put(chanName, null);
                ch_ = null;
            } catch (MonitorException e) {
                badChannelNames.put(chanName, null);
                ch_ = null;
            }
        }
        currValueTextField.setBackground(Color.white);
        meanValueTextField.setBackground(Color.white);
        currValueTextField.setEditable(false);
        meanValueTextField.setEditable(false);
        setFontForAll(new Font(meanValueTextField.getFont().getFamily(), Font.BOLD, 10));
        currValueTextField.setHorizontalAlignment(JTextField.CENTER);
        meanValueTextField.setHorizontalAlignment(JTextField.CENTER);
        currValueTextField.setText(null);
        meanValueTextField.setText(null);
    }

    public void setChannelName(String chanName) {
        if (chanName != null && badChannelNames.containsKey(chanName) == false) {
            ch_ = ChannelFactory.defaultFactory().getChannel(chanName);
            callBack = new IEventSinkValue() {

                public void eventValue(ChannelRecord record, Channel chan) {
                    currValueMonitor = record.doubleValue();
                }
            };
            try {
                ch_.addMonitorValue(callBack, Monitor.VALUE);
            } catch (MonitorException e) {
                badChannelNames.put(chanName, null);
                ch_ = null;
            } catch (ConnectionException e) {
                badChannelNames.put(chanName, null);
                ch_ = null;
            }
        } else {
            ch_ = null;
        }
    }

    public String getChannelName() {
        if (ch_ != null) {
            return ch_.channelName();
        }
        return null;
    }

    public void removeScanStopper() {
        scanStopper = null;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public void setValuator(Valuator valuator) {
        this.valuator = valuator;
    }

    public Valuator getValuator() {
        return valuator;
    }

    public void setOffSetPV(MeasuredValue offSetValIn) {
        offSetVal = offSetValIn;
    }

    public void addActionListener(ActionListener actionListener) {
        actionListenersV.add(actionListener);
    }

    public int getNumberOfActionListeners() {
        return actionListenersV.size();
    }

    public ActionListener getActionListener(int index) {
        if (index < 0 || index >= actionListenersV.size()) return null;
        return actionListenersV.get(index);
    }

    public void removeActionListeners() {
        actionListenersV.clear();
    }

    protected void fireActionEvent() {
        ActionEvent e = new ActionEvent(this, 0, "changed");
        for (int i = 0, nL = actionListenersV.size(); i < nL; i++) {
            actionListenersV.get(i).actionPerformed(e);
        }
    }

    public void setTransformationFunction(TransformationFunction transFuncIn) {
        transFunc = transFuncIn;
    }

    public void setProperty(Object keyObj, Object propObj) {
        propertyMap.put(keyObj, propObj);
    }

    public Object getProperty(Object keyObj) {
        return propertyMap.get(keyObj);
    }

    public int getPropertySize() {
        return propertyMap.size();
    }

    public Set<Object> getPropertyKeys() {
        return propertyMap.keySet();
    }

    public void setFontForAll(Font fnt) {
        currValueTextField.setFont(fnt);
        meanValueTextField.setFont(fnt);
    }

    public void restoreIniState() {
        sigma = 0.0;
        currValue = 0.;
        sumValues = 0.0;
        sumValues2 = 0.0;
        nMeasurements = 0;
    }

    public boolean measure() {
        currValue = currValueMonitor;
        if (scanStopper != null) {
            if (!ScanStopper.getScanStopper().isRunning()) {
                return false;
            }
        }
        if (ch_ != null) {
            if (offSetVal != null) {
                currValue = currValueMonitor - offSetVal.getValue();
            }
            if (transFunc != null) {
                currValue = transFunc.transform(this, currValueMonitor);
            }
        }
        if (valuator != null) {
            if (!valuator.validate()) return false;
        }
        fireActionEvent();
        currValueTextField.setText(null);
        currValueTextField.setText(valueFormat.format(currValue));
        return true;
    }

    public void acceptMeasure() {
        nMeasurements++;
        sumValues = sumValues + currValue;
        sumValues2 = sumValues2 + currValue * currValue;
        fireActionEvent();
        meanValueTextField.setText(null);
        meanValueTextField.setText(valueFormat.format(getMeasurement()));
    }

    public double getCurrentMeasurement() {
        return currValue;
    }

    public double getMeasurement() {
        if (nMeasurements > 0) {
            return sumValues / nMeasurements;
        }
        return 0.0;
    }

    public double getMeasurementSigma() {
        if (nMeasurements > 0) {
            double mean = sumValues / nMeasurements;
            double sigma = Math.sqrt(Math.abs(sumValues2 - nMeasurements * mean * mean) / nMeasurements);
            return sigma;
        }
        return 0.0;
    }

    public int getNumberOfAveraging() {
        return nMeasurements;
    }

    public double getValue() {
        restoreIniState();
        boolean iRez = measure();
        if (!iRez) {
            return 0.0;
        }
        acceptMeasure();
        return getMeasurement();
    }

    public double getInstantValue() {
        double instVal = currValueMonitor;
        if (ch_ != null) {
            if (transFunc != null) {
                instVal = transFunc.transform(this, currValueMonitor);
            }
        }
        return instVal;
    }

    public void createNewDataContainer() {
        if (extGraphData != null) return;
        if (generateUnwrap) {
            UnwrappedGeneratorGraphData unwrD = new UnwrappedGeneratorGraphData();
            BasicGraphData gd = new CubicSplineGraphData();
            unwrD.setExtUnwrappedContainer(gd);
            graphDataV.add(unwrD);
            graphDataUnwrappedV.add(gd);
        } else {
            graphDataV.add(new CubicSplineGraphData());
        }
    }

    public void createNewDataContainerRB() {
        if (extGraphDataRB != null) return;
        if (generateUnwrap) {
            UnwrappedGeneratorGraphData unwrD = new UnwrappedGeneratorGraphData();
            BasicGraphData gd = new CubicSplineGraphData();
            unwrD.setExtUnwrappedContainer(gd);
            graphDataRBV.add(unwrD);
            graphDataUnwrappedRBV.add(gd);
        } else {
            graphDataRBV.add(new CubicSplineGraphData());
        }
    }

    public int getNumberOfDataContainers() {
        return graphDataV.size();
    }

    public int getNumberOfDataContainersRB() {
        return graphDataRBV.size();
    }

    public void generateUnwrappedData(boolean generateUnwrapIn) {
        generateUnwrap = generateUnwrapIn;
    }

    public boolean generateUnwrappedDataOn() {
        return generateUnwrap;
    }

    public BasicGraphData getDataContainer(int index) {
        if (index >= 0 && index < graphDataV.size()) {
            return graphDataV.get(index);
        }
        return null;
    }

    public BasicGraphData getDataContainerRB(int index) {
        if (index >= 0 && index < graphDataRBV.size()) {
            return graphDataRBV.get(index);
        }
        return null;
    }

    public BasicGraphData getUnwrappedDataContainer(int index) {
        if (index >= 0 && index < graphDataUnwrappedV.size()) {
            return graphDataUnwrappedV.get(index);
        }
        return null;
    }

    public BasicGraphData getUnwrappedDataContainerRB(int index) {
        if (index >= 0 && index < graphDataUnwrappedRBV.size()) {
            return graphDataUnwrappedRBV.get(index);
        }
        return null;
    }

    public Vector<BasicGraphData> getDataContainers() {
        return new Vector<BasicGraphData>(graphDataV);
    }

    public Vector<BasicGraphData> getDataContainersRB() {
        return new Vector<BasicGraphData>(graphDataRBV);
    }

    public Vector<BasicGraphData> getUnwrappedDataContainers() {
        return new Vector<BasicGraphData>(graphDataUnwrappedV);
    }

    public Vector<BasicGraphData> getUnwrappedDataContainersRB() {
        return new Vector<BasicGraphData>(graphDataUnwrappedRBV);
    }

    public void removeAllDataContainers() {
        graphDataV.clear();
        graphDataRBV.clear();
        graphDataUnwrappedV.clear();
        graphDataUnwrappedRBV.clear();
    }

    public void removeDataContainer(int index) {
        if (index >= 0 && index < graphDataV.size()) {
            graphDataV.remove(index);
        }
        if (index >= 0 && index < graphDataUnwrappedV.size()) {
            graphDataUnwrappedV.remove(index);
        }
    }

    public void removeDataContainerRB(int index) {
        if (index >= 0 && index < graphDataRBV.size()) {
            graphDataRBV.remove(index);
        }
        if (index >= 0 && index < graphDataUnwrappedRBV.size()) {
            graphDataUnwrappedRBV.remove(index);
        }
    }

    public void setExternalDataContainer(BasicGraphData extGraphDataIn) {
        extGraphData = extGraphDataIn;
        graphDataV.clear();
        graphDataV.add(extGraphData);
    }

    public void setExternalDataContainerRB(BasicGraphData extGraphDataIn) {
        extGraphDataRB = extGraphDataIn;
        graphDataRBV.clear();
        graphDataRBV.add(extGraphDataRB);
    }

    private void stopScanWithReport(String chNm, String msgIn) {
        if (chNm == null) chNm = "UNKNOWN";
        String msg = "EPICS channel access problem: MeasuredValue class";
        msg = msg + System.getProperty("line.separator");
        msg = msg + "Channel name:" + chNm;
        msg = msg + System.getProperty("line.separator");
        msg = msg + msgIn;
        msg = msg + System.getProperty("line.separator");
        msg = msg + "STOP measurements.";
        if (scanStopper != null) {
            ScanStopper.getScanStopper().stop(msg);
        } else {
            System.out.println(msg);
        }
    }
}
