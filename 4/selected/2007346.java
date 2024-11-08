package gov.sns.tools.scan;

import gov.sns.ca.*;
import javax.swing.*;
import java.text.*;
import java.awt.*;

public class IndependentValue {

    private Channel ch_ = null;

    private Channel ch_RB_ = null;

    private String chanName = null;

    private String chanNameRB = null;

    private double currValue = 0.0;

    private double currValue_RB = 0.0;

    private volatile double currValueMonitor = 0.0;

    private volatile double currValueMonitor_RB = 0.0;

    private double memValue = 0.0;

    private double memValue_RB = 0.0;

    private String alias = null;

    private ScanStopper scanStopper = ScanStopper.getScanStopper();

    public JTextField valueTextField = new JTextField(8);

    public JTextField valueTextFieldRB = new JTextField(8);

    public DecimalFormat valueFormat = new DecimalFormat("0.00E0");

    private IEventSinkValue callBack = null;

    private IEventSinkValue callBackRB = null;

    public IndependentValue() {
        setTextFieldsProperty();
    }

    public IndependentValue(String alias) {
        this.alias = alias;
        setTextFieldsProperty();
    }

    public IndependentValue(String alias, String chanName, String chanNameRB) {
        setAlias(alias);
        setChannelName(chanName);
        setChannelNameRB(chanNameRB);
        setTextFieldsProperty();
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

    public String getChannelName() {
        if (ch_ != null) {
            return ch_.channelName();
        }
        return chanName;
    }

    public void setChannelName(String chanName) {
        this.chanName = chanName;
        if (chanName == null) {
            ch_ = null;
            return;
        }
        callBack = new IEventSinkValue() {

            public void eventValue(ChannelRecord record, Channel chan) {
                currValueMonitor = record.doubleValue();
            }
        };
        ch_ = ChannelFactory.defaultFactory().getChannel(chanName);
        if (ch_ == null) {
            stopScanWithReport(null, getChannelName(), "JCA can not create channel.");
            return;
        }
        try {
            ch_.addMonitorValue(callBack, Monitor.VALUE);
        } catch (ConnectionException e) {
            stopScanWithReport(e, chanName, "JCA can not create channel.");
        } catch (MonitorException e) {
            stopScanWithReport(e, chanName, "JCA can not monitor channel.");
        }
    }

    public String getChannelNameRB() {
        if (ch_RB_ != null) {
            return ch_RB_.channelName();
        }
        return chanNameRB;
    }

    public void setChannelNameRB(String chanNameRB) {
        this.chanNameRB = chanNameRB;
        if (chanNameRB == null) {
            ch_RB_ = null;
            return;
        }
        callBackRB = new IEventSinkValue() {

            public void eventValue(ChannelRecord record, Channel chan) {
                currValueMonitor_RB = record.doubleValue();
            }
        };
        ch_RB_ = ChannelFactory.defaultFactory().getChannel(chanNameRB);
        if (ch_RB_ == null) {
            stopScanWithReport(null, getChannelNameRB(), "JCA can not create channel.");
            return;
        }
        try {
            ch_RB_.addMonitorValue(callBackRB, Monitor.VALUE);
        } catch (ConnectionException e) {
            stopScanWithReport(e, chanNameRB, "JCA can not create channel.");
        } catch (MonitorException e) {
            stopScanWithReport(e, chanNameRB, "JCA can not monitor channel.");
        }
    }

    public void memorizeValue() {
        measure();
        memValue = currValue;
        memValue_RB = currValue_RB;
    }

    public void restoreFromMemory() {
        if (ch_ != null) {
            try {
                ch_.putVal(memValue);
            } catch (ConnectionException e) {
                stopScanWithReport(e, getChannelName(), "JCA ConnectionException");
                return;
            } catch (PutException e) {
                stopScanWithReport(e, getChannelName(), "JCA PutException");
                return;
            }
        }
        measure();
    }

    public void measure() {
        if (ch_ != null) {
            currValue = currValueMonitor;
        }
        if (ch_RB_ != null) {
            currValue_RB = currValueMonitor_RB;
        }
        updateTextFields();
    }

    public void setValue(double val) {
        if (ch_ != null) {
            try {
                ch_.putVal(val);
            } catch (ConnectionException e) {
                stopScanWithReport(e, getChannelName(), "JCA ConnectionException");
                return;
            } catch (PutException e) {
                stopScanWithReport(e, getChannelName(), "JCA PutException");
                return;
            }
        }
        measure();
    }

    public double getCurrentValue() {
        return currValueMonitor;
    }

    public double getCurrentValueRB() {
        return currValueMonitor_RB;
    }

    public double getValue() {
        measure();
        return getCurrentValue();
    }

    public double getValueRB() {
        measure();
        return getCurrentValueRB();
    }

    protected void setTextFieldsProperty() {
        valueTextField.setText(null);
        valueTextFieldRB.setText(null);
        valueTextField.setHorizontalAlignment(JTextField.CENTER);
        valueTextFieldRB.setHorizontalAlignment(JTextField.CENTER);
        valueTextField.setEditable(false);
        valueTextFieldRB.setEditable(false);
        valueTextField.setBackground(Color.white);
        valueTextFieldRB.setBackground(Color.white);
    }

    protected void updateTextFields() {
        valueTextField.setText(null);
        valueTextFieldRB.setText(null);
        valueTextField.setText(valueFormat.format(getCurrentValue()));
        valueTextFieldRB.setText(valueFormat.format(getCurrentValueRB()));
    }

    private void stopScanWithReport(Exception e, String chNm, String msgIn) {
        if (chNm == null) chNm = "UNKNOWN";
        String msg = "EPICS channel access problem: IndependentValue class";
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
