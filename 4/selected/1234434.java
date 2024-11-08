package de.jlab.ui.modules.panels.analog;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import javax.swing.JLabel;
import javax.swing.JPanel;
import de.jlab.boards.Board;
import de.jlab.lab.Lab;
import de.jlab.lab.SubChannelUpdatedNotification;
import de.jlab.ui.tools.Gauge;

@SuppressWarnings("serial")
public class ComboAdGaugePanel extends JPanel implements Observer {

    int[] subChannels = null;

    Set<String> channelsToWatch = new HashSet<String>();

    Gauge[] gauges = null;

    JLabel[] labels = null;

    double minValue;

    double maxValue;

    double min;

    double max;

    double testSinusStart = 0;

    Map<Integer, Integer> subChannelToIndex = new HashMap<Integer, Integer>();

    DecimalFormat df = new DecimalFormat("#0.00 V");

    Lab theLab = null;

    Board theBoard = null;

    public ComboAdGaugePanel(Lab lab, Board board, double min, double max, Integer... subchannels) {
        theLab = lab;
        theBoard = board;
        minValue = min;
        maxValue = max;
        gauges = new Gauge[subchannels.length];
        labels = new JLabel[subchannels.length];
        subChannels = new int[subchannels.length];
        for (int i = 0; i < subchannels.length; ++i) {
            channelsToWatch.add(board.getCommChannel().getChannelName() + "-" + board.getAddress() + "-" + subchannels[i]);
            subChannels[i] = subchannels[i];
        }
        this.min = min;
        this.max = max;
        initUI();
    }

    private void initUI() {
        this.setLayout(new GridBagLayout());
        for (int i = 0; i < labels.length; ++i) {
            Gauge newGauge = new Gauge(min, max, 30, 5, "#0.0##V", Gauge.ORIENTATION.VERTICAL);
            JLabel newLabel = new JLabel("0 V");
            JLabel newSubChannelLabel = new JLabel(subChannels[i] + "");
            this.add(newSubChannelLabel, new GridBagConstraints(i, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(3, 3, 3, 3), 0, 0));
            this.add(newGauge, new GridBagConstraints(i, 1, 1, 1, 0.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.VERTICAL, new Insets(3, 3, 3, 3), 0, 0));
            this.add(newLabel, new GridBagConstraints(i, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(3, 3, 3, 3), 0, 0));
            subChannelToIndex.put(subChannels[i], i);
            gauges[i] = newGauge;
            labels[i] = newLabel;
        }
    }

    public void update(Observable o, Object arg) {
        if (!(arg instanceof SubChannelUpdatedNotification)) return;
        SubChannelUpdatedNotification notification = (SubChannelUpdatedNotification) arg;
        if (channelsToWatch.contains(notification.getCommChannel() + "-" + notification.getAddress() + "-" + notification.getSubchannel())) {
            int valueIndex = subChannelToIndex.get(notification.getSubchannel());
            gauges[valueIndex].setValue(notification.getDoubleValue());
            labels[valueIndex].setText(df.format(notification.getDoubleValue()));
        }
    }
}
