package wand.genericChannel;

import java.awt.Color;
import javax.swing.BorderFactory;
import javax.swing.border.Border;
import wand.*;
import wand.graphicsChooser.themeSets.Theme;
import wand.patterns.*;

public class GenericChannel {

    public ChannelBeat channelBeat;

    private int channelID;

    private DisplayPanel channelDisplay;

    private DisplayPanel outputPreviewPanel;

    private DisplayPanel outputDisplayPanel;

    private int borderWidth = 4;

    private Border deadBorder = BorderFactory.createLineBorder(Color.black, borderWidth);

    private Border liveBorder = BorderFactory.createLineBorder(Color.red, borderWidth);

    private Border focusBorder = BorderFactory.createLineBorder(Color.cyan, borderWidth);

    private static final int NO_MODIFIER = 16;

    private static final int SHIFT_MODIFIER = 17;

    private static final int CTRL_MODIFIER = 18;

    public Border getDeadBorder() {
        return deadBorder;
    }

    public GenericChannel(int id) {
        channelID = id;
        channelDisplay = new DisplayPanel(false);
        outputPreviewPanel = new DisplayPanel(false);
        outputDisplayPanel = new DisplayPanel(true);
        channelDisplay.setBackground(Color.green);
        outputPreviewPanel.setBackground(Color.black);
        outputDisplayPanel.setBackground(Color.black);
        channelBeat = new ChannelBeat(channelID);
        channelDisplay.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mousePressed(java.awt.event.MouseEvent evt) {
                channelDisplayClicked(evt);
            }
        });
    }

    public int getChannelID() {
        return channelID;
    }

    public int getChannelDisplayBorderWidth() {
        return borderWidth;
    }

    private void channelDisplayClicked(java.awt.event.MouseEvent evt) {
        int modifier = evt.getModifiers();
        if (modifier == NO_MODIFIER) ChannelFrame.channelGridPanel.setOutputChannel(channelID, true); else if (modifier == SHIFT_MODIFIER) ; else if (modifier == CTRL_MODIFIER) ChannelFrame.channelGridPanel.setChannelFocus(channelID);
    }

    public void setPoints() {
        channelDisplay.setPoints();
        outputPreviewPanel.setPoints();
        outputDisplayPanel.setPoints();
    }

    public void beatRefresh(int channelBeat) {
        channelDisplay.beatRefresh(channelBeat);
        outputPreviewPanel.beatRefresh(channelBeat);
        outputDisplayPanel.beatRefresh(channelBeat);
        if (channelID == ChannelFrame.controlPanel.getChannelID()) {
            ChannelFrame.controlPanel.indicatorPanel.beatRefresh(channelBeat);
        }
    }

    public void setBorderActive() {
        channelDisplay.setBorder(liveBorder);
    }

    public void setBorderFocus() {
        channelDisplay.setBorder(focusBorder);
    }

    public void setBorderDeactive() {
        channelDisplay.setBorder(deadBorder);
    }

    public void reset() {
    }

    public ChannelBeat getChannelBeat() {
        return channelBeat;
    }

    public DisplayPanel getChannelDisplay() {
        return channelDisplay;
    }

    public DisplayPanel getOutputPreviewPanel() {
        return outputPreviewPanel;
    }

    public DisplayPanel getOutputDisplayPanel() {
        return outputDisplayPanel;
    }

    public void setPatternType(PatternInterface p0, PatternInterface p1, PatternInterface p2) {
        channelDisplay.setPatternType(p0);
        outputPreviewPanel.setPatternType(p1);
        outputDisplayPanel.setPatternType(p2);
    }

    public void setPatternType(PatternInterface pattern) {
        channelDisplay.setPatternType(pattern);
        outputPreviewPanel.setPatternType(pattern);
        outputDisplayPanel.setPatternType(pattern);
    }

    public void updateInputString() {
        ChannelFrame.controlPanel.textInputPanel.updateInputString();
    }

    public void setTextAreaInput(String text) {
        ChannelFrame.controlPanel.textInputPanel.setTextAreaInput(text);
    }

    public String getChannelType() {
        return channelDisplay.getPattern().getPatternType();
    }

    public String[] getStringArray() {
        return stringArray;
    }

    public void setStringArray(String[] s) {
        stringArray = s;
    }

    private String[] stringArray = new String[999];

    public void setText(String[] s) {
        channelDisplay.getPattern().setText(s);
        outputPreviewPanel.getPattern().setText(s);
        outputDisplayPanel.getPattern().setText(s);
    }

    public boolean getRecoil() {
        return channelDisplay.getPattern().getRecoil();
    }

    public void setRecoil(boolean flag) {
        channelDisplay.getPattern().setRecoil(flag);
        outputPreviewPanel.getPattern().setRecoil(flag);
        outputDisplayPanel.getPattern().setRecoil(flag);
    }

    public boolean getTightBPC() {
        return channelDisplay.getPattern().getTightBPC();
    }

    public void setTightBPC(boolean flag) {
        channelDisplay.getPattern().setTightBPC(flag);
        outputPreviewPanel.getPattern().setTightBPC(flag);
        outputDisplayPanel.getPattern().setTightBPC(flag);
    }

    public int getDatumShift() {
        return channelDisplay.getPattern().getDatumShift();
    }

    public void setDatumShift(int value) {
        channelDisplay.getPattern().setDatumShift(value);
        outputPreviewPanel.getPattern().setDatumShift(value);
        outputDisplayPanel.getPattern().setDatumShift(value);
    }

    public void saveParametersFile() {
        channelDisplay.getPattern().saveParametersFile();
    }

    public void setStretchMode(String mode) {
        channelDisplay.getPattern().setStretchMode(mode);
        outputPreviewPanel.getPattern().setStretchMode(mode);
        outputDisplayPanel.getPattern().setStretchMode(mode);
    }

    public String getStretchMode() {
        return channelDisplay.getPattern().getStretchMode();
    }

    public void setAlpha(float alpha) {
        channelDisplay.setAlpha(alpha, false);
        outputPreviewPanel.setAlpha(alpha, true);
        outputDisplayPanel.setAlpha(alpha, true);
    }

    public void setOpaque(boolean isOpaque) {
        channelDisplay.setOpaque(isOpaque);
        outputPreviewPanel.setOpaque(isOpaque);
        outputDisplayPanel.setOpaque(isOpaque);
    }

    public void refreshAlpha(boolean displayOnly) {
        channelDisplay.refreshAlpha(false);
        outputPreviewPanel.refreshAlpha(true);
        outputDisplayPanel.refreshAlpha(true);
    }

    public int getInitDelayIndex() {
        return channelDisplay.getPattern().getInitDelayIndex();
    }

    public void setInitDelayIndex(int index) {
        channelDisplay.getPattern().setInitDelayIndex(index);
    }
}
