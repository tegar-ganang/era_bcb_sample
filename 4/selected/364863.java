package jp.jparc.apps.iTuning.Scan2D;

import javax.swing.*;
import java.awt.event.*;
import gov.sns.ca.*;

public class ScanVariable {

    private MonitoredPV mpv = null;

    private MonitoredPV mpvRB = null;

    private int strobeMask = 0;

    private double memValue = 0.0;

    private JTextField messageText = null;

    private ActionListener stopScanListener = null;

    private ActionEvent stopActionEvent = null;

    private Object lockObj = new Object();

    private Channel strobeChan = null;

    private boolean isRestoreFromRB = false;

    public ScanVariable(String alias, String aliasRB) {
        mpv = MonitoredPV.getMonitoredPV(alias);
        mpvRB = MonitoredPV.getMonitoredPV(aliasRB);
        stopActionEvent = new ActionEvent(this, 0, "stop");
    }

    public ScanVariable(String alias, String aliasRB, int strobeMask) {
        mpv = MonitoredPV.getMonitoredPV(alias);
        mpvRB = MonitoredPV.getMonitoredPV(aliasRB);
        this.strobeMask = strobeMask;
        stopActionEvent = new ActionEvent(this, 0, "stop");
    }

    protected void setStopScanListener(ActionListener stopScanListener) {
        this.stopScanListener = stopScanListener;
    }

    public void setMessageTextField(JTextField messageText) {
        this.messageText = messageText;
    }

    protected void setLockObject(Object lockObj) {
        this.lockObj = lockObj;
    }

    public String getChannelName() {
        return mpv.getChannelName();
    }

    public String getChannelNameRB() {
        return mpvRB.getChannelName();
    }

    public String getStrobeChanName() {
        return strobeChan.channelName();
    }

    public Channel getChannel() {
        return mpv.getChannel();
    }

    public Channel getChannelRB() {
        return mpvRB.getChannel();
    }

    public Channel getStrobeChan() {
        return strobeChan;
    }

    public MonitoredPV getMonitoredPV() {
        return mpv;
    }

    public MonitoredPV getMonitoredPV_RB() {
        return mpvRB;
    }

    public void setChannelName(String chanName) {
        synchronized (lockObj) {
            mpv.setChannelName(chanName);
        }
    }

    public void setChannelNameRB(String chanNameRB) {
        synchronized (lockObj) {
            mpvRB.setChannelName(chanNameRB);
        }
    }

    public void setChannel(Channel ch) {
        synchronized (lockObj) {
            mpv.setChannel(ch);
        }
    }

    public void setChannelRB(Channel ch_RB) {
        synchronized (lockObj) {
            mpvRB.setChannel(ch_RB);
            isRestoreFromRB = true;
        }
    }

    public void setStrobeChan(Channel strobeCh) {
        this.strobeChan = strobeCh;
    }

    public void memorizeValue() {
        if (isRestoreFromRB) {
            memValue = mpvRB.getValue();
        } else {
            memValue = mpv.getValue();
        }
    }

    public void restoreFromMemory() {
        setValue(memValue);
    }

    public void setValue(double val) {
        Channel ch = mpv.getChannel();
        String chanName = mpv.getChannelName();
        if (ch != null) {
            try {
                ch.putVal(val);
                if (strobeChan != null) {
                    try {
                        strobeChan.putVal(strobeMask);
                    } catch (ConnectionException e) {
                        stopScanWithMessage("Cannot put value to the channel: " + strobeChan.channelName());
                        return;
                    } catch (PutException e) {
                        stopScanWithMessage("Cannot put value to the channel: " + strobeChan.channelName());
                        return;
                    }
                }
            } catch (ConnectionException e) {
                stopScanWithMessage("Cannot put value to the channel: " + chanName);
                return;
            } catch (PutException e) {
                stopScanWithMessage("Cannot put value to the channel: " + chanName);
                return;
            }
        }
    }

    public double getValue() {
        return mpv.getValue();
    }

    public double getValueRB() {
        return mpvRB.getValue();
    }

    private void stopScanWithMessage(String msg) {
        if (stopScanListener != null) {
            stopScanListener.actionPerformed(stopActionEvent);
        }
        if (messageText != null) {
            messageText.setText(msg);
        } else {
            System.out.println("ScanVariable class:" + msg);
        }
    }
}
