package gov.sns.ca.samples;

import gov.sns.ca.Channel;
import gov.sns.ca.ChannelFactory;
import gov.sns.ca.ChannelTimeRecord;
import gov.sns.ca.ConnectionException;
import gov.sns.ca.IEventSinkValTime;
import gov.sns.ca.Monitor;
import gov.sns.ca.MonitorException;
import java.util.Date;

public class MonitorListenerTest implements IEventSinkValTime {

    /**
     * @param args
     */
    public static void main(String[] args) {
        if (args == null) {
            System.out.println("usage MonitorListerTest channel");
            System.exit(0);
        } else if (args.length == 0) {
            System.out.println("usage MonitorListerTest channel");
            System.exit(0);
        }
        String chname = args[0];
        System.out.println("chname = " + chname);
        Channel channel = ChannelFactory.defaultFactory().getChannel(chname);
        channel.connectAndWait();
        try {
            channel.addMonitorValTime(new MonitorListenerTest(), Monitor.VALUE);
        } catch (ConnectionException e) {
            e.printStackTrace();
        } catch (MonitorException e) {
            e.printStackTrace();
        }
        try {
            Date d = channel.getTimeRecord().getTimestamp().getDate();
            System.out.println("d = " + d);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            Thread.sleep(500);
            channel.disconnect();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("time-out");
        System.exit(1);
    }

    public void eventValue(ChannelTimeRecord record, Channel chan) {
        System.out.println(record);
    }
}
