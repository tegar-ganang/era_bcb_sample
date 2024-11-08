package gov.sns.apps.istuner;

import gov.sns.ca.Channel;
import gov.sns.ca.ChannelFactory;
import gov.sns.ca.ChannelTimeRecord;
import gov.sns.ca.ConnectionException;
import gov.sns.ca.GetException;
import gov.sns.ca.IEventSinkValTime;
import gov.sns.ca.Monitor;
import gov.sns.ca.MonitorException;
import gov.sns.ca.Timestamp;
import java.awt.Color;
import java.awt.event.ComponentEvent;
import java.text.DecimalFormat;
import java.util.Date;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;

public class MonitorLabel extends JLabel implements IEventSinkValTime {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    private String ch;

    private Channel channel;

    private ChannelTimeRecord snapshot;

    protected double val;

    private double valRange;

    private double valRef;

    protected DecimalFormat df;

    private boolean checkRange;

    public MonitorLabel(String chname, String title) {
        checkRange = false;
        ch = chname;
        valRange = Double.NaN;
        valRef = Double.NaN;
        val = 0;
        df = new DecimalFormat("####.00");
        TitledBorder tborder = new TitledBorder(null, title, TitledBorder.CENTER, TitledBorder.LEFT);
        this.setBorder(tborder);
        this.setHorizontalAlignment(SwingConstants.CENTER);
        init();
    }

    public String getChannelName() {
        return ch;
    }

    public void setValRange(double d) {
        valRange = d;
    }

    public void setValRef(double d) {
        valRef = d;
        setCheckRange(checkRange);
    }

    public void setCheckRange(boolean check) {
        checkRange = check;
        try {
            checkValRange(channel.getTimeRecord());
        } catch (ConnectionException e) {
            e.printStackTrace();
        } catch (GetException e) {
            e.printStackTrace();
        }
    }

    public synchronized ChannelTimeRecord takeSnapShot() {
        try {
            snapshot = channel.getTimeRecord();
            valRef = snapshot.doubleValue();
        } catch (ConnectionException e) {
            e.printStackTrace();
        } catch (GetException e) {
            e.printStackTrace();
        }
        return snapshot;
    }

    public double getSnapShotValue() {
        if (snapshot != null) {
            double snapval = snapshot.doubleValue();
            return snapval;
        }
        return Double.NaN;
    }

    public Date getSnapShotDate() {
        if (snapshot != null) {
            Timestamp snapts = snapshot.getTimestamp();
            Date snapdate = snapts.getDate();
            return snapdate;
        }
        return null;
    }

    public synchronized double getVal() {
        return val;
    }

    private void checkValRange(ChannelTimeRecord record) {
        if (record != null) {
            val = record.doubleValue();
            setText(df.format(val));
            this.setForeground(Color.BLACK);
            if ((checkRange) && (valRange != Double.NaN) && (valRef != Double.NaN)) {
                double dv = val - valRef;
                if (Math.abs(dv) > valRange) {
                    this.setForeground(Color.RED);
                    processComponentEvent(new ComponentEvent(this, ComponentEvent.COMPONENT_MOVED));
                }
            }
        } else {
            this.setForeground(Color.BLACK);
            setText("no connection");
        }
        repaint();
    }

    public synchronized void eventValue(ChannelTimeRecord record, Channel chan) {
        checkValRange(record);
    }

    public void init() {
        channel = ChannelFactory.defaultFactory().getChannel(ch);
        channel.connectAndWait(1000);
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
        JFrame frame = new JFrame();
        frame.setSize(200, 100);
        JLabel label = new MonitorLabel(chname, "test");
        frame.add(label);
        frame.setVisible(true);
        frame.pack();
    }
}
