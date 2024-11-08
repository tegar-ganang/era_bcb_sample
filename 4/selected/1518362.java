package gov.sns.apps.energymaster;

import java.util.ArrayList;
import java.util.Date;
import gov.sns.ca.Channel;
import gov.sns.ca.ChannelFactory;
import gov.sns.ca.ChannelTimeRecord;
import gov.sns.ca.ConnectionException;
import gov.sns.ca.IEventSinkValTime;
import gov.sns.ca.Monitor;
import gov.sns.ca.MonitorException;

public class CaUpdateCounter implements IEventSinkValTime {

    private String ch;

    public String getChannelName() {
        return ch;
    }

    private double val;

    private Date date;

    ArrayList<Double> Vals;

    ArrayList<Date> queue;

    double timeRange;

    int nqueue = 0;

    Channel channel;

    public CaUpdateCounter(String chname, double tr) {
        val = Double.NaN;
        Vals = new ArrayList<Double>();
        queue = new ArrayList<Date>();
        ch = chname;
        timeRange = tr;
        setup();
    }

    public synchronized ArrayList<Double> getVals() {
        return Vals;
    }

    public synchronized double getValue() {
        return val;
    }

    public synchronized void eventValue(ChannelTimeRecord record, Channel chan) {
        val = record.doubleValue();
        Vals.add(new Double(val));
        Date now = new Date();
        queue.add(now);
        boolean flags[] = new boolean[queue.size()];
        boolean debug = false;
        if (debug) {
            System.out.println("ch = " + ch);
            System.out.println("queue.size()(before) = " + queue.size());
        }
        for (int i = 0; i < queue.size(); i++) {
            Date date = queue.get(i);
            if (date.getTime() < (now.getTime() - 1000. * timeRange)) {
                flags[i] = false;
            } else {
                flags[i] = true;
            }
        }
        for (int i = queue.size() - 1; i >= 0; i--) {
            if (!flags[i]) {
                Vals.remove(i);
                queue.remove(i);
            }
        }
        if (debug) {
            System.out.println("queue.size()(after) = " + queue.size());
        }
        setUpdateNumber(queue.size());
    }

    private synchronized void updateQueue() {
        Date now = new Date();
        boolean flags[] = new boolean[queue.size()];
        for (int i = 0; i < queue.size(); i++) {
            Date date = queue.get(i);
            if (date.getTime() < (now.getTime() - 1000. * timeRange)) {
                flags[i] = false;
            } else {
                flags[i] = true;
            }
        }
        for (int i = queue.size() - 1; i >= 0; i--) {
            if (!flags[i]) {
                Vals.remove(i);
                queue.remove(i);
            }
        }
        setUpdateNumber(queue.size());
    }

    protected synchronized void setUpdateNumber(int nq) {
        nqueue = nq;
    }

    public double getTimeRange() {
        return timeRange;
    }

    public synchronized int getUpdateNumber() {
        if (queue.size() == 0) {
            return 0;
        }
        Date newest = queue.get(queue.size() - 1);
        Date now = new Date();
        if (newest.getTime() < (now.getTime() - 1000. * timeRange)) {
            System.out.println("no update for ch " + ch + ", fake polling....");
            System.out.println("t(newest), t(now), dt, dt(max) = " + newest.getTime() + " " + now.getTime() + " " + (now.getTime() - newest.getTime()) + " " + 1000 * timeRange);
            updateQueue();
        }
        return nqueue;
    }

    private void setup() {
        channel = ChannelFactory.defaultFactory().getChannel(ch);
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

    public static void main(String[] args) {
        if (args == null) {
            System.out.println("usage MonitorListerTest channel");
            System.exit(0);
        } else if (args.length == 0) {
            System.out.println("usage MonitorListerTest channel");
            System.exit(0);
        }
        String chname = args[0];
        new CaUpdateCounter(chname, 10);
    }
}
