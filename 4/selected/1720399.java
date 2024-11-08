package wand.channelControl;

import java.awt.GridLayout;
import javax.swing.JPanel;
import javax.swing.JLabel;

public class BeatCountTestPanel extends JPanel {

    private JLabel beatNumber;

    private int channelID;

    public BeatCountTestPanel() {
        beatNumber = new JLabel("Waiting");
        setLayout(new GridLayout(1, 1));
        add(beatNumber);
    }

    String beatText;

    public void showBeat(int beat) {
        beatText = Integer.toString(beat);
        beatNumber.setText(beatText);
    }

    public void setChannelID(int chID) {
        channelID = chID;
    }

    public int getChannelID() {
        return channelID;
    }
}
