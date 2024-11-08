package jp.jparc.apps.iTuning.Scan2D;

import java.util.*;
import java.awt.*;
import gov.sns.ca.*;
import gov.sns.tools.plot.*;

public class MeasuredValue {

    private MonitoredPV mpv = null;

    private String alias = null;

    private double sigma = 0.0;

    private double currValue = 0.0;

    private double sumValues = 0.0;

    private double sumValues2 = 0.0;

    private int nMeasurements = 0;

    private TransformationFunction transFunc = null;

    private MeasuredValue offSetVal = null;

    private HashMap propertyMap = new HashMap();

    private Vector graphDataV = new Vector();

    private Vector graphDataRBV = new Vector();

    private boolean drawLinesOn = true;

    private boolean generateUnwrap = false;

    private boolean immediateContainerUpdate = false;

    private BasicGraphData extGraphData = null;

    private BasicGraphData extGraphDataRB = null;

    private Color graphColor = null;

    private Channel rangeSwitchChan;

    private Channel rangeSwitchChanRB;

    private int rangeSwitchVal = 0;

    private boolean rangeSwitch = false;

    public MeasuredValue(String alias) {
        this.alias = alias;
        mpv = MonitoredPV.getMonitoredPV(alias);
    }

    public void setChannel(Channel ch) {
        mpv.setChannel(ch);
    }

    public void setSwitchChan(boolean switchRange, Channel chan, Channel chanRB, int val) {
        this.rangeSwitch = switchRange;
        this.rangeSwitchChan = chan;
        this.rangeSwitchChanRB = chanRB;
        this.rangeSwitchVal = val;
    }

    public Channel getChannel() {
        return mpv.getChannel();
    }

    public void setChannelName(String chanName) {
        mpv.setChannelName(chanName);
    }

    public String getChannelName() {
        return mpv.getChannelName();
    }

    public String getAlias() {
        return alias;
    }

    public MonitoredPV getMonitoredPV() {
        return mpv;
    }

    public void setOffSetPV(MeasuredValue offSetVal) {
        this.offSetVal = offSetVal;
    }

    public void setTransformationFunction(TransformationFunction transFunc) {
        this.transFunc = transFunc;
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

    public Set getPropertyKeys() {
        return propertyMap.keySet();
    }

    public void restoreIniState() {
        sigma = 0.0;
        sumValues = 0.0;
        sumValues2 = 0.0;
        nMeasurements = 0;
    }

    public void measure() {
        if (rangeSwitch) {
            if (!rangeSwitchChan.isConnected()) rangeSwitchChan.requestConnection();
            if (!rangeSwitchChanRB.isConnected()) rangeSwitchChanRB.requestConnection();
            try {
                if (rangeSwitchChanRB.getValInt() != rangeSwitchVal) {
                    rangeSwitchChan.putVal(rangeSwitchVal);
                    Thread.sleep(1000);
                }
                int badCounter = 0;
                while (rangeSwitchChanRB.getValInt() != rangeSwitchVal) {
                    rangeSwitchChan.putVal(rangeSwitchVal);
                    Thread.sleep(2);
                    badCounter++;
                    if (badCounter == 5) {
                        System.out.println("Can not switch the phase detector...");
                        break;
                    }
                }
            } catch (ConnectionException e) {
                e.printStackTrace();
            } catch (PutException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (GetException e) {
                e.printStackTrace();
            }
        }
        currValue = mpv.getValue();
        if (offSetVal != null) {
            currValue -= offSetVal.getValue();
        }
        if (transFunc != null) {
            currValue = transFunc.transform(this, currValue);
        }
        nMeasurements++;
        sumValues = sumValues + currValue;
        sumValues2 = sumValues2 + currValue * currValue;
        if (rangeSwitch && rangeSwitchVal == 1) {
            if (!rangeSwitchChan.isConnected()) rangeSwitchChan.requestConnection();
            if (!rangeSwitchChanRB.isConnected()) rangeSwitchChanRB.requestConnection();
            try {
                rangeSwitchChan.putVal(0);
                Thread.sleep(2);
                int badCounter = 0;
                while (rangeSwitchChanRB.getValInt() != 0) {
                    rangeSwitchChan.putVal(0);
                    Thread.sleep(2);
                    badCounter++;
                    if (badCounter == 5) {
                        System.out.println("Can not switch the phase detector...");
                        break;
                    }
                }
            } catch (ConnectionException e) {
                e.printStackTrace();
            } catch (PutException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (GetException e) {
                e.printStackTrace();
            }
        }
    }

    public double getMeasurement() {
        if (nMeasurements > 0) {
            return sumValues / nMeasurements;
        }
        return currValue;
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
        currValue = mpv.getValue();
        if (offSetVal != null) {
            currValue -= offSetVal.getValue();
        }
        if (transFunc != null) {
            currValue = transFunc.transform(this, currValue);
        }
        return currValue;
    }

    public void addNewDataConatainer(BasicGraphData gd) {
        graphDataV.add(gd);
        gd.setImmediateContainerUpdate(false);
        gd.setDrawLinesOn(drawLinesOn);
        if (graphColor != null) {
            gd.setGraphColor(graphColor);
        }
    }

    public void addNewDataConatainerRB(BasicGraphData gd) {
        graphDataRBV.add(gd);
        gd.setImmediateContainerUpdate(false);
        gd.setDrawLinesOn(drawLinesOn);
        if (graphColor != null) {
            gd.setGraphColor(graphColor);
        }
    }

    protected void createNewDataContainer() {
        if (extGraphData != null) return;
        if (mpv.getChannel() == null) return;
        if (graphDataV.size() > 0 && ((BasicGraphData) graphDataV.lastElement()).getNumbOfPoints() == 0) return;
        BasicGraphData gd = new BasicGraphData();
        graphDataV.add(gd);
        gd.setImmediateContainerUpdate(false);
        gd.setDrawLinesOn(drawLinesOn);
        if (graphColor != null) {
            gd.setGraphColor(graphColor);
        }
    }

    protected void createNewDataContainerRB() {
        if (extGraphDataRB != null) return;
        if (mpv.getChannel() == null) return;
        if (graphDataRBV.size() > 0 && ((BasicGraphData) graphDataRBV.lastElement()).getNumbOfPoints() == 0) return;
        BasicGraphData gd = new BasicGraphData();
        graphDataRBV.add(gd);
        gd.setImmediateContainerUpdate(false);
        gd.setDrawLinesOn(drawLinesOn);
        if (graphColor != null) {
            gd.setGraphColor(graphColor);
        }
    }

    public void setDrawLinesOn(boolean drawLinesOn) {
        this.drawLinesOn = drawLinesOn;
        for (int i = 0, n = graphDataV.size(); i < n; i++) {
            ((BasicGraphData) graphDataV.get(i)).setDrawLinesOn(drawLinesOn);
        }
        for (int i = 0, n = graphDataRBV.size(); i < n; i++) {
            ((BasicGraphData) graphDataRBV.get(i)).setDrawLinesOn(drawLinesOn);
        }
    }

    public void setImmediateGraphUpdate(boolean immediateContainerUpdate) {
        this.immediateContainerUpdate = immediateContainerUpdate;
        for (int i = 0, n = graphDataV.size(); i < n; i++) {
            ((BasicGraphData) graphDataV.get(i)).setImmediateContainerUpdate(immediateContainerUpdate);
        }
        for (int i = 0, n = graphDataRBV.size(); i < n; i++) {
            ((BasicGraphData) graphDataRBV.get(i)).setImmediateContainerUpdate(immediateContainerUpdate);
        }
    }

    public void setColor(Color color) {
        graphColor = color;
        for (int i = 0, n = graphDataV.size(); i < n; i++) {
            ((BasicGraphData) graphDataV.get(i)).setGraphColor(graphColor);
        }
        for (int i = 0, n = graphDataRBV.size(); i < n; i++) {
            ((BasicGraphData) graphDataRBV.get(i)).setGraphColor(graphColor);
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
            return (BasicGraphData) graphDataV.get(index);
        }
        return null;
    }

    public BasicGraphData getDataContainerRB(int index) {
        if (index >= 0 && index < graphDataRBV.size()) {
            return (BasicGraphData) graphDataRBV.get(index);
        }
        return null;
    }

    public BasicGraphData getDataContainer() {
        if (graphDataV.size() > 0) {
            return (BasicGraphData) graphDataV.get(graphDataV.size() - 1);
        }
        return null;
    }

    public BasicGraphData getDataContainerRB() {
        if (graphDataRBV.size() > 0) {
            return (BasicGraphData) graphDataRBV.get(graphDataRBV.size() - 1);
        }
        return null;
    }

    public Vector getDataContainers() {
        return new Vector(graphDataV);
    }

    public Vector getDataContainersRB() {
        return new Vector(graphDataRBV);
    }

    public void removeAllDataContainers() {
        graphDataV.clear();
        graphDataRBV.clear();
    }

    public void removeAllDataContainersNonRB() {
        graphDataV.clear();
    }

    public void removeAllDataContainersRB() {
        graphDataRBV.clear();
    }

    public void removeDataContainer(BasicGraphData gd) {
        graphDataV.remove(gd);
        graphDataRBV.remove(gd);
    }

    public void removeDataContainer(int index) {
        if (index >= 0 && index < graphDataV.size()) {
            graphDataV.remove(index);
        }
    }

    public void removeDataContainerRB(int index) {
        if (index >= 0 && index < graphDataRBV.size()) {
            graphDataRBV.remove(index);
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
        extGraphDataIn.setImmediateContainerUpdate(false);
    }

    protected void consumeData(double x) {
        BasicGraphData gd = getDataContainer();
        if (gd != null && mpv.getChannel() != null) {
            if (generateUnwrap == false) {
                gd.addPoint(x, getMeasurement(), getMeasurementSigma());
            } else {
                int np = gd.getNumbOfPoints();
                double y_last = 0.;
                if (np != 0) {
                    y_last = gd.getY(np - 1);
                }
                double y_new = getMeasurement();
                y_new = unwrap(y_new, y_last);
                gd.addPoint(x, y_new, getMeasurementSigma());
            }
        }
    }

    protected void consumeDataRB(double xRB) {
        BasicGraphData gd = getDataContainerRB();
        if (gd != null && mpv.getChannel() != null) {
            if (generateUnwrap == false) {
                gd.addPoint(xRB, getMeasurement(), getMeasurementSigma());
            } else {
                int np = gd.getNumbOfPoints();
                double y_last = 0.;
                if (np != 0) {
                    y_last = gd.getY(np - 1);
                }
                double y_new = getMeasurement();
                y_new = unwrap(y_new, y_last);
                gd.addPoint(xRB, y_new, getMeasurementSigma());
            }
        }
    }

    /**
     * this method finds +-2*PI to produce the nearest points
     */
    private double unwrap(double y, double yIn) {
        if (y == yIn) return y;
        int n = 0;
        double diff_min = Math.abs(yIn - y);
        double diff = diff_min;
        int n_max = 1 + ((int) (diff_min / 360.));
        for (int i = -n_max; i <= n_max; i++) {
            diff = Math.abs(y + i * 360. - yIn);
            if (diff < diff_min) {
                diff_min = diff;
                n = i;
            }
        }
        return (y + n * 360.);
    }
}
