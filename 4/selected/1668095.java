package de.jlab.ui.modules.panels.analog;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.DecimalFormat;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import de.jlab.boards.Board;
import de.jlab.lab.Lab;
import de.jlab.lab.SubChannelUpdatedNotification;

@SuppressWarnings("serial")
public class ComboDaSliderPanel extends JPanel implements Observer {

    int[] subChannels = null;

    Set<String> channelsToWatch = new HashSet<String>();

    Map<Integer, JLabel> labelsForSubChannel = new HashMap<Integer, JLabel>();

    Map<Integer, JSlider> slidersForSubChannel = new HashMap<Integer, JSlider>();

    double minValue;

    double maxValue;

    int min;

    int max;

    Dictionary<Integer, JLabel> lableTable = new Hashtable<Integer, JLabel>();

    private static int STEPPING = 3000;

    Lab theLab = null;

    DecimalFormat df = new DecimalFormat("#0.0V");

    boolean loopback = false;

    Timer suspendChangeTimer = new Timer();

    SuspendTimerTask suspendTask = null;

    Board theBoard = null;

    /**
    * 
    * @param lab
    *           reference to the Lab (where the commands got to)
    * @param address
    *           ( address of the Board )
    * @param min
    *           (minimum Value for the sliders (in volts)
    * @param max
    *           ( maximum Values for the Sliders (in volts)
    * @param loopback
    *           shall the Label under the slider show the Value acknowledged by
    *           the Board or simply show set set Value ?
    * @param subchannels
    *           ( Subchannels to control )
    */
    public ComboDaSliderPanel(Lab lab, Board aBoard, double min, double max, boolean loopback, Integer... subchannels) {
        theLab = lab;
        this.theBoard = aBoard;
        subChannels = new int[subchannels.length];
        for (int i = 0; i < subchannels.length; ++i) {
            channelsToWatch.add(theBoard.getCommChannel().getChannelName() + "-" + theBoard.getAddress() + "-" + subchannels[i]);
            subChannels[i] = subchannels[i];
        }
        this.min = (int) min * 1000;
        this.max = (int) max * 1000;
        if (min < 0 && max > 0) {
            for (int i = 0; i <= this.max; i = i + STEPPING) {
                lableTable.put(i, new JLabel(((double) i) / 1000 + "V"));
            }
            for (int i = 0; i >= this.min; i = i - STEPPING) {
                lableTable.put(i, new JLabel(((double) i) / 1000 + "V"));
            }
        } else {
            for (int i = this.min; i < this.max; i = i + STEPPING) {
                lableTable.put(i, new JLabel(((double) i) / 1000 + "V"));
            }
        }
        initUI();
    }

    private void initUI() {
        this.setLayout(new GridBagLayout());
        for (int i = 0; i < subChannels.length; ++i) {
            JSlider newSlider = new JSlider(JSlider.VERTICAL, min, max, 0);
            JLabel newLabel = new JLabel();
            JLabel newChannelLabel = new JLabel("" + subChannels[i]);
            this.add(newChannelLabel, new GridBagConstraints(i, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(3, 3, 3, 3), 0, 0));
            this.add(newSlider, new GridBagConstraints(i, 1, 1, 1, 0.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.VERTICAL, new Insets(3, 3, 3, 3), 0, 0));
            this.add(newLabel, new GridBagConstraints(i, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(3, 3, 3, 3), 0, 0));
            newSlider.setPaintLabels(true);
            newSlider.setPaintTicks(true);
            newSlider.setPaintTrack(true);
            newSlider.setMajorTickSpacing(STEPPING);
            newSlider.setMinorTickSpacing(STEPPING / 5);
            newSlider.setLabelTable(lableTable);
            double boardValue = theBoard.queryDoubleValue(subChannels[i]);
            newSlider.setValue((int) (boardValue * 1000));
            newLabel.setText(df.format(boardValue));
            slidersForSubChannel.put(subChannels[i], newSlider);
            labelsForSubChannel.put(subChannels[i], newLabel);
            newSlider.addChangeListener(new ChannelChangeListener(this, subChannels[i]));
        }
    }

    public void setOutputToVolts(int subChannel, double volts) {
        theBoard.sendCommand(subChannel, volts);
        if (true) theBoard.queryValueAsynchronously(subChannel); else {
            JLabel voltageLabel = labelsForSubChannel.get(subChannel);
            if (voltageLabel != null) voltageLabel.setText(df.format(volts));
        }
        suspendTask = null;
    }

    public void update(Observable o, Object arg) {
        if (!(arg instanceof SubChannelUpdatedNotification)) return;
        SubChannelUpdatedNotification notification = (SubChannelUpdatedNotification) arg;
        if (channelsToWatch.contains(notification.getAddress() + "-" + notification.getSubchannel())) {
            JSlider slider = slidersForSubChannel.get(notification.getSubchannel());
            if (slider != null) slider.setValue((int) (notification.getDoubleValue() * 1000));
            JLabel voltageLabel = labelsForSubChannel.get(notification.getSubchannel());
            if (voltageLabel != null) voltageLabel.setText(df.format(notification.getDoubleValue()));
        }
    }

    public void createSuspendTimer(int channel, double volts) {
        if (this.suspendTask != null) {
            suspendTask.cancel();
        }
        suspendTask = new SuspendTimerTask(channel, volts, this);
        this.suspendChangeTimer.schedule(suspendTask, 300);
    }
}

class ChannelChangeListener implements ChangeListener {

    ComboDaSliderPanel panel = null;

    int subchannel = 0;

    public ChannelChangeListener(ComboDaSliderPanel panel, int subChannel) {
        super();
        this.panel = panel;
        this.subchannel = subChannel;
    }

    public void stateChanged(ChangeEvent e) {
        JSlider slider = (JSlider) e.getSource();
        double wantedVolts = ((double) slider.getValue()) / 1000;
        panel.createSuspendTimer(subchannel, wantedVolts);
    }
}

class SuspendTimerTask extends TimerTask {

    int subchannel;

    double volts;

    ComboDaSliderPanel panel = null;

    public void run() {
        panel.setOutputToVolts(subchannel, volts);
    }

    public SuspendTimerTask(int subchannel, double volts, ComboDaSliderPanel panel) {
        super();
        this.subchannel = subchannel;
        this.volts = volts;
        this.panel = panel;
    }
}
