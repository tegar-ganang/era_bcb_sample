package jp.jparc.apps.monsampler;

import java.util.Date;
import gov.sns.ca.Channel;
import gov.sns.ca.ChannelFactory;
import gov.sns.ca.ChannelTimeRecord;
import gov.sns.ca.ConnectionException;
import gov.sns.ca.PutException;
import gov.sns.ca.IEventSinkValTime;
import gov.sns.ca.Monitor;
import gov.sns.ca.MonitorException;

public class CaMonitorScalar implements IEventSinkValTime {

    static final double caTimeOut = 0.01;

    static final int caputExtraTimeOut = 400;

    private String ch;

    public String getChannelName() {
        return ch;
    }

    private double val;

    private Date date;

    static void setChannel(Channel ch, int val) {
        try {
            if (!ch.isConnected()) {
                ch.requestConnection();
            }
            ch.putVal(val);
            Thread.sleep(CaMonitorScalar.caputExtraTimeOut);
        } catch (ConnectionException e) {
            e.printStackTrace();
        } catch (PutException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    static void setChannel(Channel ch, double val) {
        try {
            if (!ch.isConnected()) {
                ch.requestConnection();
            }
            ch.putVal(val);
            Thread.sleep(CaMonitorScalar.caputExtraTimeOut);
        } catch (ConnectionException e) {
            e.printStackTrace();
        } catch (PutException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public double getValue() {
        return val;
    }

    public Date getDate() {
        return date;
    }

    public CaMonitorScalar(String chname) {
        val = 0;
        ch = chname;
        setup();
    }

    public synchronized void eventValue(ChannelTimeRecord record, Channel chan) {
        val = record.doubleValue();
        date = record.getTimestamp().getDate();
    }

    private synchronized void setup() {
        Channel channel = ChannelFactory.defaultFactory().getChannel(ch);
        channel.connectAndWait(caTimeOut);
        try {
            channel.addMonitorValTime(this, Monitor.VALUE);
        } catch (ConnectionException e) {
            e.printStackTrace();
        } catch (MonitorException e) {
            e.printStackTrace();
        }
    }
}
