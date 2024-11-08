package gov.sns.apps.energymaster;

import java.util.ArrayList;
import gov.sns.ca.Channel;
import gov.sns.ca.ChannelFactory;
import gov.sns.ca.ChannelTimeRecord;
import gov.sns.ca.ConnectionException;
import gov.sns.ca.IEventSinkValTime;
import gov.sns.ca.Monitor;
import gov.sns.ca.MonitorException;

public class CaMonitorBuffer implements IEventSinkValTime {

    private String ch;

    public String getChannelName() {
        return ch;
    }

    private ArrayList<ChannelTimeRecord> buffer;

    private int bufferSize;

    private double timeThreshold;

    public synchronized ChannelTimeRecord getRecord() {
        if (buffer.size() > 0) {
            return buffer.get(buffer.size() - 1);
        } else {
            return null;
        }
    }

    public CaMonitorBuffer(String chname, int bs, double dt) {
        buffer = new ArrayList<ChannelTimeRecord>();
        ch = chname;
        bufferSize = bs;
        timeThreshold = dt;
        setup();
    }

    public synchronized void eventValue(ChannelTimeRecord record, Channel chan) {
        buffer.add(record);
        if (buffer.size() > bufferSize) {
            buffer.remove(0);
        }
    }

    public synchronized ArrayList<ChannelTimeRecord> getBuffer() {
        return buffer;
    }

    public synchronized int getBufferSize() {
        return bufferSize;
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
