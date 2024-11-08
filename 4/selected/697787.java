package wand.channelControl;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Font;
import javax.swing.*;
import wand.ChannelFrame;

public class DelayPanel extends JPanel implements SwingConstants {

    private JRadioButton b1, b2, b3, b4, b5, b6, b7, b8, b9, b10, b11, b12;

    public int channelID;

    private JRadioButton[] gearButton;

    private int modifier = 16;

    private int index = 0;

    private Double[] gearValue;

    private int tightBPC_CutOff = 5;

    public void setChannelID(int chID) {
        channelID = chID;
        loadValue();
    }

    public void loadValue() {
        double beatGear = ChannelFrame.channelGridPanel.channels[channelID].getChannelBeat().getGear();
        if (beatGear == 0.015625) b1.setSelected(true);
        if (beatGear == 0.03125) b2.setSelected(true);
        if (beatGear == 0.0625) b3.setSelected(true);
        if (beatGear == 0.125) b4.setSelected(true);
        if (beatGear == 0.25) b5.setSelected(true);
        if (beatGear == 0.5) b6.setSelected(true);
        if (beatGear == 1.0) b7.setSelected(true);
        if (beatGear == 2.0) b8.setSelected(true);
        if (beatGear == 4.0) b9.setSelected(true);
        if (beatGear == 8.0) b10.setSelected(true);
        if (beatGear == 16.0) b11.setSelected(true);
        if (beatGear == 32.0) b12.setSelected(true);
        gearCheckFromButtons();
    }

    public int getChannelID() {
        return channelID;
    }

    public DelayPanel() {
        setBackground(Color.white);
        makeButtons();
        setLayout(new GridLayout(2, 6));
        add(b7);
        add(b8);
        add(b9);
        add(b10);
        add(b11);
        add(b12);
        add(b1);
        add(b2);
        add(b3);
        add(b4);
        add(b5);
        add(b6);
        b7.setSelected(true);
        gearCheckFromButtons();
    }

    private void setDelay(int index, boolean arm, String direction) {
        ChannelFrame.channelGridPanel.channels[channelID].getChannelBeat().setGear(gearValue[index], arm);
        gearButton[index].setSelected(true);
        if (ChannelFrame.channelGridPanel.channels[channelID].getChannelType().equals("clip")) {
            if (ChannelFrame.channelGridPanel.channels[channelID].getStretchMode().equals("discrete") && index > tightBPC_CutOff) {
                if (!ChannelFrame.controlPanel.clipParametersPanel.tightCheck.isSelected()) {
                    ChannelFrame.controlPanel.clipParametersPanel.tightCheck.doClick();
                }
            }
            if (ChannelFrame.channelGridPanel.channels[channelID].getStretchMode().equals("discrete") && index <= tightBPC_CutOff) {
                if (ChannelFrame.controlPanel.clipParametersPanel.tightCheck.isSelected()) {
                    ChannelFrame.controlPanel.clipParametersPanel.tightCheck.doClick();
                }
            }
            if (index == tightBPC_CutOff && direction.equals("up")) {
                index++;
                setDelay(index, arm, direction);
            }
            if (index == tightBPC_CutOff && direction.equals("down")) {
                index--;
                setDelay(index, arm, direction);
            }
        }
    }

    public void setIndex(int indexToPlace, boolean arm) {
        index = indexToPlace;
        setDelay(index, arm, "down");
    }

    public void gearUpArm() {
        if (index < gearButton.length - 1) index++;
        setDelay(index, true, "up");
    }

    public void gearDownArm() {
        if (index > 0) index--;
        setDelay(index, true, "down");
    }

    public void gearUpPunch() {
        if (index < gearButton.length - 1) index++;
        setDelay(index, false, "up");
    }

    public void gearDownPunch() {
        if (index > 0) index--;
        setDelay(index, false, "down");
    }

    private int gearCheckFromButtons() {
        if (b1.isSelected()) index = 0;
        ;
        if (b2.isSelected()) index = 1;
        if (b3.isSelected()) index = 2;
        if (b4.isSelected()) index = 3;
        if (b5.isSelected()) index = 4;
        if (b6.isSelected()) index = 5;
        if (b7.isSelected()) index = 6;
        if (b8.isSelected()) index = 7;
        if (b9.isSelected()) index = 8;
        if (b10.isSelected()) index = 9;
        if (b11.isSelected()) index = 10;
        if (b12.isSelected()) index = 11;
        return index;
    }

    private void makeButtons() {
        b1 = new JRadioButton("1/64");
        b2 = new JRadioButton("1/32");
        b3 = new JRadioButton("1/16");
        b4 = new JRadioButton("1/8");
        b5 = new JRadioButton("1/4");
        b6 = new JRadioButton("1/2");
        b7 = new JRadioButton("1");
        b8 = new JRadioButton("2");
        b9 = new JRadioButton("4");
        b10 = new JRadioButton("8");
        b11 = new JRadioButton("16");
        b12 = new JRadioButton("32");
        gearButton = new JRadioButton[] { b1, b2, b3, b4, b5, b6, b7, b8, b9, b10, b11, b12 };
        gearValue = new Double[] { 0.015625, 0.03125, 0.0625, 0.125, 0.25, 0.5, 1.0, 2.0, 4.0, 8.0, 16.0, 32.0 };
        ActionListener actionListener = new ActionListener() {

            public void actionPerformed(ActionEvent actionEvent) {
                modifier = actionEvent.getModifiers();
                if (modifier == 16) setDelay(gearCheckFromButtons(), false, "down"); else if (modifier == 17) setDelay(gearCheckFromButtons(), true, "down"); else System.out.println("Modifier error on delay panel");
            }
        };
        b1.addActionListener(actionListener);
        b2.addActionListener(actionListener);
        b3.addActionListener(actionListener);
        b4.addActionListener(actionListener);
        b5.addActionListener(actionListener);
        b6.addActionListener(actionListener);
        b7.addActionListener(actionListener);
        b8.addActionListener(actionListener);
        b9.addActionListener(actionListener);
        b10.addActionListener(actionListener);
        b11.addActionListener(actionListener);
        b12.addActionListener(actionListener);
        Font labelFont = new Font("sansserif", Font.PLAIN, 8);
        b1.setFont(labelFont);
        b2.setFont(labelFont);
        b3.setFont(labelFont);
        b4.setFont(labelFont);
        b5.setFont(labelFont);
        b6.setFont(labelFont);
        b7.setFont(labelFont);
        b8.setFont(labelFont);
        b9.setFont(labelFont);
        b10.setFont(labelFont);
        b11.setFont(labelFont);
        b12.setFont(labelFont);
        b1.setHorizontalTextPosition(SwingConstants.CENTER);
        b1.setVerticalTextPosition(SwingConstants.BOTTOM);
        b2.setHorizontalTextPosition(SwingConstants.CENTER);
        b2.setVerticalTextPosition(SwingConstants.BOTTOM);
        b3.setHorizontalTextPosition(SwingConstants.CENTER);
        b3.setVerticalTextPosition(SwingConstants.BOTTOM);
        b4.setHorizontalTextPosition(SwingConstants.CENTER);
        b4.setVerticalTextPosition(SwingConstants.BOTTOM);
        b5.setHorizontalTextPosition(SwingConstants.CENTER);
        b5.setVerticalTextPosition(SwingConstants.BOTTOM);
        b6.setHorizontalTextPosition(SwingConstants.CENTER);
        b6.setVerticalTextPosition(SwingConstants.BOTTOM);
        b7.setHorizontalTextPosition(SwingConstants.CENTER);
        b7.setVerticalTextPosition(SwingConstants.BOTTOM);
        b8.setHorizontalTextPosition(SwingConstants.CENTER);
        b8.setVerticalTextPosition(SwingConstants.BOTTOM);
        b9.setHorizontalTextPosition(SwingConstants.CENTER);
        b9.setVerticalTextPosition(SwingConstants.BOTTOM);
        b10.setHorizontalTextPosition(SwingConstants.CENTER);
        b10.setVerticalTextPosition(SwingConstants.BOTTOM);
        b11.setHorizontalTextPosition(SwingConstants.CENTER);
        b11.setVerticalTextPosition(SwingConstants.BOTTOM);
        b12.setHorizontalTextPosition(SwingConstants.CENTER);
        b12.setVerticalTextPosition(SwingConstants.BOTTOM);
        ButtonGroup group = new ButtonGroup();
        group.add(b1);
        group.add(b2);
        group.add(b3);
        group.add(b4);
        group.add(b5);
        group.add(b6);
        group.add(b7);
        group.add(b8);
        group.add(b9);
        group.add(b10);
        group.add(b11);
        group.add(b12);
    }
}
