package wand.channelControl;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.*;

public class ControlPanel extends JPanel {

    private int channelID;

    public IndicatorPanel indicatorPanel = new IndicatorPanel();

    public DelayPanel delayPanel = new DelayPanel();

    public ScratchPanel scratchPanel = new ScratchPanel();

    public ChannelOutTriggerPanel channelOutTriggerPanel = new ChannelOutTriggerPanel();

    public ClipParametersPanel clipParametersPanel = new ClipParametersPanel();

    public TextFormatPanel textFormatPanel = new TextFormatPanel();

    public TextInput textInputPanel = new TextInput();

    public ControlPanel() {
        setLayout(new GridLayout(7, 1));
        setPreferredSize(new Dimension(200, 700));
        setBorder(BorderFactory.createLineBorder(Color.gray, 0));
        setBackground(Color.lightGray);
        add(indicatorPanel);
        add(delayPanel);
        add(scratchPanel);
        add(clipParametersPanel);
        add(textFormatPanel);
        add(textInputPanel);
        add(channelOutTriggerPanel);
    }

    public int getChannelID() {
        return channelID;
    }

    public void setChannelID(int chID) {
        channelID = chID;
        channelOutTriggerPanel.setChannelID(channelID);
        clipParametersPanel.setChannelID(channelID);
        delayPanel.setChannelID(channelID);
        indicatorPanel.setChannelID(channelID);
        scratchPanel.setChannelID(channelID);
        textInputPanel.setChannelID(channelID);
        textFormatPanel.setChannelID(channelID);
    }

    public void setPoints() {
        indicatorPanel.setPoints();
    }
}
