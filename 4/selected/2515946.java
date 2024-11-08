package gov.sns.apps.energymaster;

import java.util.ArrayList;
import java.util.Date;
import gov.sns.ca.Channel;
import gov.sns.ca.ChannelFactory;
import gov.sns.ca.ChannelTimeRecord;
import gov.sns.ca.ConnectionException;
import gov.sns.ca.IEventSinkValTime;
import gov.sns.ca.PutException;
import gov.sns.ca.Monitor;
import gov.sns.ca.MonitorException;

public class CaMonitorIntArray implements IEventSinkValTime {

    static final int caputExtraTimeOut = 400;

    private String ch;

    public String getChannelName() {
        return ch;
    }

    private int[] val;

    private Date date;

    private ChannelTimeRecord newestRec;

    static void setChannel(Channel ch, double val) {
        try {
            if (!ch.isConnected()) {
                ch.requestConnection();
            }
            ch.putVal(val);
            Thread.sleep(CaMonitorIntArray.caputExtraTimeOut);
        } catch (ConnectionException e) {
            e.printStackTrace();
        } catch (PutException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public ChannelTimeRecord getRecord() {
        return newestRec;
    }

    public int[] getValue() {
        return val;
    }

    public Date getDate() {
        return date;
    }

    public CaMonitorIntArray(String chname) {
        val = new int[40000];
        newestRec = null;
        ch = chname;
        setup();
    }

    public synchronized void eventValue(ChannelTimeRecord record, Channel chan) {
        newestRec = record;
        val = record.intArray();
        date = record.getTimestamp().getDate();
    }

    public synchronized ArrayList<ChannelTimeRecord> getBuffer() {
        ArrayList<ChannelTimeRecord> list = new ArrayList<ChannelTimeRecord>();
        list.add(newestRec);
        return list;
    }

    private synchronized void setup() {
        Channel channel = ChannelFactory.defaultFactory().getChannel(ch);
        try {
            if (!channel.isConnected()) {
                channel.requestConnection();
            }
            channel.addMonitorValTime(this, Monitor.VALUE);
        } catch (ConnectionException e) {
            e.printStackTrace();
        } catch (MonitorException e) {
            e.printStackTrace();
        }
    }
}
