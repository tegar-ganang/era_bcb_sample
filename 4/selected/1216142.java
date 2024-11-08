package UserInterface;

import java.awt.*;

/**
 * Graphical settings holds settings who are important for LCPDChart. Which channel should be displayed, which color has which channel
 * 
 * @author kurmt1@bfh.ch, woodr1@bfh.ch, eichs2@bfh.ch
 * @version 1.0
 */
public class LCPDGraphicSettings {

    private final int NumberOfChannel = 2;

    private final int NumberOfMathChannel = 2;

    private float[] OffsetVoltage;

    private boolean[] DisplayChannel;

    private Color[] ChannelColor;

    private boolean[] DisplayMath;

    private Color[] MathColor;

    private boolean NewData;

    /**
	 * Initialize Graphic Settings
	 */
    public LCPDGraphicSettings() {
        DisplayChannel = new boolean[NumberOfChannel];
        ChannelColor = new Color[NumberOfChannel];
        DisplayMath = new boolean[NumberOfMathChannel];
        MathColor = new Color[NumberOfMathChannel];
        OffsetVoltage = new float[NumberOfChannel];
        for (int i = 0; i < NumberOfChannel; i++) {
            DisplayChannel[i] = false;
            ChannelColor[i] = new Color(255, 0, 0);
            OffsetVoltage[i] = 0;
        }
    }

    public boolean getDisplayChannel(int ChannelNumber) {
        if (ChannelNumber < NumberOfChannel) return DisplayChannel[ChannelNumber]; else return false;
    }

    public void setDisplayChannel(int ChannelNumber, boolean value) {
        if (ChannelNumber < NumberOfChannel) DisplayChannel[ChannelNumber] = value;
    }

    public final Color getChannelColor(int ChannelNumber) {
        if (ChannelNumber < NumberOfChannel) return ChannelColor[ChannelNumber]; else return null;
    }

    public void setChannelColor(int ChannelNumber, Color value) {
        if (ChannelNumber < NumberOfChannel) ChannelColor[ChannelNumber] = value;
    }

    public final Color getMathColor(int ChannelNumber) {
        if (ChannelNumber < NumberOfMathChannel) return MathColor[ChannelNumber]; else return null;
    }

    public void setMathColor(int ChannelNumber, Color value) {
        if (ChannelNumber < NumberOfMathChannel) MathColor[ChannelNumber] = value;
    }

    public final boolean getDisplayMath(int ChannelNumber) {
        if (ChannelNumber < NumberOfMathChannel) return DisplayMath[ChannelNumber]; else return false;
    }

    public void setDisplayMath(int ChannelNumber, boolean value) {
        if (ChannelNumber < NumberOfMathChannel) DisplayMath[ChannelNumber] = value;
    }

    public final boolean getNewData() {
        return NewData;
    }

    public void setNewData(boolean value) {
        NewData = value;
    }

    public float getOffsetVoltage(int Channel) {
        if (Channel < NumberOfChannel) {
            return OffsetVoltage[Channel];
        } else return 0;
    }

    public void setOffsetVoltage(int Channel, float offsetVoltage) {
        if (Channel < NumberOfChannel) {
            OffsetVoltage[Channel] = offsetVoltage;
        }
    }
}
