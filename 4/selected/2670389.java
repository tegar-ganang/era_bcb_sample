package gov.sns.ca.samples;

import gov.sns.ca.Channel;
import gov.sns.ca.ChannelFactory;
import gov.sns.ca.ChannelTimeRecord;
import gov.sns.ca.ConnectionException;
import gov.sns.ca.IEventSinkValTime;
import gov.sns.ca.Monitor;
import gov.sns.ca.MonitorException;
import java.util.ArrayList;
import java.util.Date;

public class MonitorListenerTest2 implements IEventSinkValTime, Runnable {

    String ch;

    ArrayList<Date> queue;

    double timeRange;

    int nqueue = 0;

    public MonitorListenerTest2(String chname, double tr) {
        queue = new ArrayList<Date>();
        ch = chname;
        System.out.println("ch = " + ch);
        timeRange = tr;
        Thread th = new Thread(this);
        th.start();
    }

    public void eventValue(ChannelTimeRecord record, Channel chan) {
        double[] d = record.doubleArray();
        System.out.println("d = ");
        if (d != null) {
            for (int i = 0; i < d.length; i++) {
                System.out.print(d[i] + " ");
            }
        }
        System.out.println();
    }

    protected synchronized void setUpdateNumber(int nq) {
        nqueue = nq;
    }

    public double getTimeRange() {
        return timeRange;
    }

    public synchronized int getUpdateNumber() {
        return nqueue;
    }

    public void run() {
        Channel channel = ChannelFactory.defaultFactory().getChannel(ch);
        channel.connectAndWait();
        final int count = 1024;
        System.out.println("count = " + count);
        try {
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
        new MonitorListenerTest2(chname, 10);
    }
}
