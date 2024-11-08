package wand.graphicsChooser;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Dimension;
import javax.swing.JFrame;
import wand.ChannelFrame;
import wand.patterns.PatternInterface;

public class ClipChooserFrame extends JFrame {

    public void choiceMade(PatternInterface clip0, PatternInterface clip1, PatternInterface clip2) {
        ChannelFrame.channelGridPanel.channels[channelID].setPatternType(clip0);
        ChannelFrame.controlPanel.clipParametersPanel.loadParameters();
        ChannelFrame.channelGridPanel.channels[channelID].getChannelBeat().setGear(0.125, false);
        ChannelFrame.controlPanel.delayPanel.loadValue();
    }

    private FlowLayout layout = new FlowLayout();

    private PatternInterface p0, p1, p2;

    private int channelID = 0;

    public ClipChooserFrame() {
        this.setTitle("Clip Chooser");
        this.setPreferredSize(new Dimension(850, 750));
        this.setAlwaysOnTop(true);
        this.setVisible(false);
        setLocationByPlatform(true);
        this.setBackground(Color.yellow);
        setLayout(layout);
        getContentPane().add(new ClipBeatButtons());
        pack();
    }

    public void setChannel(int id) {
        channelID = id;
    }
}
