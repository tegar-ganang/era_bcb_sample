package gov.sns.tools.scan;

import gov.sns.ca.*;
import java.util.*;
import javax.swing.*;

public class Valuator {

    protected Channel ch_ = null;

    protected double value = 0.0;

    protected volatile double currValueMonitor = 0.0;

    protected double lowLim;

    protected double uppLim;

    protected ValuatorLimitsManager limManager = null;

    protected Vector<Valuator> valuatorsV = new Vector<Valuator>();

    private IEventSinkValue callBack = null;

    private static HashMap<String, Void> badChannelNames = new HashMap<String, Void>();

    public Valuator() {
        this(null, -Double.MAX_VALUE, Double.MAX_VALUE);
    }

    public Valuator(String chanName) {
        this(chanName, -Double.MAX_VALUE, Double.MAX_VALUE);
    }

    public Valuator(String chanName, double lowLimIn, double uppLimIn) {
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
        lowLim = lowLimIn;
        uppLim = uppLimIn;
    }

    public void setChannelName(String chanName) {
        ch_ = null;
        if (chanName == null) {
            return;
        }
        if (badChannelNames.containsKey(chanName) == false) {
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
    }

    public String getChannelName() {
        if (ch_ == null) {
            return null;
        } else {
            return ch_.channelName();
        }
    }

    public void setLimitsManager(ValuatorLimitsManager limManagerIn) {
        limManager = limManagerIn;
    }

    public void setLowLim(double lowLim) {
        this.lowLim = lowLim;
    }

    public void setUppLim(double uppLim) {
        this.uppLim = uppLim;
    }

    public double getLowLim() {
        if (limManager != null) {
            return limManager.getLowLim();
        }
        return lowLim;
    }

    public double getUppLim() {
        if (limManager != null) {
            return limManager.getUppLim();
        }
        return uppLim;
    }

    public boolean validate() {
        value = currValueMonitor;
        if (ch_ != null) {
            if (value < getLowLim() || value > getUppLim()) {
                return false;
            }
        }
        for (int i = 0, nVl = valuatorsV.size(); i < nVl; i++) {
            if (!valuatorsV.get(i).validate()) {
                return false;
            }
        }
        return true;
    }

    public double getValue() {
        return currValueMonitor;
    }

    public void addExternalValuator(Valuator vl) {
        if (!valuatorsV.contains(vl)) {
            valuatorsV.add(vl);
        }
    }

    public void removeExternalValuator(Valuator vl) {
        valuatorsV.removeElement(vl);
        ;
    }

    public void removeExternalValuators() {
        valuatorsV.clear();
    }

    private void stopMeasurement(String chName) {
        String title = "Valuator MESSAGE";
        String lineSeparator = System.getProperty("line.separator");
        String errMsg = "EPICS Channel Access Error" + lineSeparator;
        errMsg = errMsg + "Can not create channel for PV name=" + chName + lineSeparator;
        errMsg = errMsg + "The subsequent measurements will be unreliable.";
        JOptionPane.showMessageDialog(null, errMsg, title, JOptionPane.ERROR_MESSAGE);
    }
}
