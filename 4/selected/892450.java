package wand.genericChannel;

import java.awt.Color;
import java.awt.GridLayout;
import javax.swing.JPanel;
import wand.*;

public class ChannelGridPanel extends JPanel {

    private int numberOfChannels = 12;

    public GenericChannel[] channels = new GenericChannel[numberOfChannels];

    public ChannelGridPanel() {
        setLayout(new GridLayout(3, 4));
        setBackground(Color.black);
        for (int i = 0; i < 12; i++) {
            GenericChannel genericChannel = new GenericChannel(i);
            channels[i] = genericChannel;
        }
        for (int i = 0; i < channels.length; i++) {
            add(channels[i].getChannelDisplay());
        }
    }

    public void setPoints() {
        for (int i = 0; i < channels.length; i++) {
            channels[i].setPoints();
        }
    }

    public void beatRefresh(int beat) {
        for (int i = 0; i < channels.length; i++) {
            channels[i].beatRefresh(beat);
        }
    }

    public void engineBeatHappened(int engineBeat, double delay, double timeOfBeat) {
        for (int i = 0; i < channels.length; i++) {
            channels[i].getChannelBeat().engineBeatHappened(engineBeat, delay, timeOfBeat);
        }
    }

    public void clockCheck(double time) {
        for (int i = 0; i < channels.length; i++) {
            channels[i].getChannelBeat().clockCheck(time);
        }
    }

    public void scratchKill() {
        for (int i = 0; i < channels.length; i++) {
            channels[i].getChannelBeat().kill();
        }
    }

    public boolean outPutChannelWaiting = false;

    public int outputChannelWaitingID;

    public int currentOutputChannel;

    public int currentChannelFocus;

    private boolean swap = false;

    public void setOutputChannel(int channelID, boolean instant) {
        if (channelID >= channels.length) return;
        if (instant) {
            ChannelFrame.enginePanel.previewFrame.lp.forceChannelToFront(channelID);
            ChannelFrame.enginePanel.display.lp.forceChannelToFront(channelID);
            deactivateAllBorders();
            setOutputChannelBorder(channelID);
            currentOutputChannel = channelID;
            outPutChannelWaiting = false;
            if (!swap) setChannelFocus(channelID);
            swap = false;
            outputChannelWaitingID = channelID;
            ChannelFrame.filterPanel.channelTransitions.setChannelsForFadeTransition(this.getFocusChannel(), this.getOutPutChannel());
        } else {
            if (!swap) setChannelFocus(channelID);
            outputChannelWaitingID = channelID;
            outPutChannelWaiting = true;
        }
    }

    public void setChannelFocus(int channelID) {
        setChannelFocusBorder(currentChannelFocus);
        if (channelID >= channels.length) return;
        channels[channelID].getChannelDisplay().requestFocusInWindow();
        currentChannelFocus = channelID;
        deactivateAllBorders();
        setChannelFocusBorder(channelID);
        setOutputChannelBorder(currentOutputChannel);
        ChannelFrame.slideFocusedChannelUnderTop();
        ChannelFrame.controlPanel.setChannelID(channelID);
        ChannelFrame.filterPanel.channelTransitions.setChannelsForFadeTransition(this.getFocusChannel(), this.getOutPutChannel());
    }

    public void armFocusedChannel() {
        swap = true;
        int focusGoTo = currentOutputChannel;
        setOutputChannel(currentChannelFocus, false);
        setChannelFocus(focusGoTo);
    }

    public void punchFocusedChannel() {
        swap = true;
        int focusGoTo = currentOutputChannel;
        setOutputChannel(currentChannelFocus, true);
        setChannelFocus(focusGoTo);
    }

    public void slideFocusedChannelUnderTop() {
        ChannelFrame.enginePanel.previewFrame.lp.forceChannelUnderneath(currentOutputChannel, currentChannelFocus);
        ChannelFrame.enginePanel.display.lp.forceChannelUnderneath(currentOutputChannel, currentChannelFocus);
    }

    public void setOutputChannelBorder(int channelID) {
        if (channelID >= channels.length) return;
        channels[channelID].setBorderActive();
    }

    public void setChannelFocusBorder(int channelID) {
        if (channelID >= channels.length) return;
        channels[channelID].setBorderFocus();
    }

    public void deactivateAllBorders() {
        for (int i = 0; i < channels.length; i++) {
            channels[i].setBorderDeactive();
        }
    }

    public void markButtonPressed() {
        setOutputChannel(outputChannelWaitingID, true);
    }

    public GenericChannel getOutPutChannel() {
        return channels[currentOutputChannel];
    }

    public GenericChannel getFocusChannel() {
        return channels[currentChannelFocus];
    }

    public void restoreAllChannelsToInitDelay() {
        hypeIndex = 0;
        for (GenericChannel ch : channels) {
            ch.getChannelBeat().setGearIndex(ch.getInitDelayIndex() + hypeIndex, false);
        }
        ChannelFrame.controlPanel.delayPanel.loadValue();
    }

    public void shiftAllChannelsDelayDown() {
        hypeIndex--;
        for (GenericChannel ch : channels) {
            ch.getChannelBeat().setGearIndex(ch.getInitDelayIndex() + hypeIndex, false);
        }
        ChannelFrame.controlPanel.delayPanel.loadValue();
    }

    public void shiftAllChannelsDelayUp() {
        hypeIndex++;
        for (GenericChannel ch : channels) {
            ch.getChannelBeat().setGearIndex(ch.getInitDelayIndex() + hypeIndex, false);
        }
        ChannelFrame.controlPanel.delayPanel.loadValue();
    }

    private int hypeIndex = 0;
}
