package wand.channelControl;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JPanel;
import wand.ChannelFrame;
import wand.genericChannel.*;

public class ScratchPanel extends JPanel {

    Insets inset = new Insets(1, 1, 1, 1);

    public JButton beatScratchButton1, beatScratchButton2, beatScratchButton3, beatScratchButton4, beatScratchButton5, beatScratchButton6, beatScratchButton7, beatScratchButton8;

    public JButton beatScratchDown, beatScratchUp, recordButton, save;

    private int channelID;

    private GenericChannel channel;

    public void setChannelID(int chID) {
        channelID = chID;
        channel = ChannelFrame.channelGridPanel.channels[channelID];
    }

    public int getChannelID() {
        return channelID;
    }

    public ScratchPanel() {
        Insets inset = new Insets(1, 1, 1, 1);
        setBackground(Color.white);
        makeButtons();
        setLayout(new GridLayout(3, 4));
        recordButton.setBackground(Color.white);
        recordButton.setMargin(inset);
        save.setMargin(inset);
        add(beatScratchDown);
        add(beatScratchUp);
        add(recordButton);
        add(save);
        add(beatScratchButton1);
        add(beatScratchButton2);
        add(beatScratchButton3);
        add(beatScratchButton4);
        add(beatScratchButton5);
        add(beatScratchButton6);
        add(beatScratchButton7);
        add(beatScratchButton8);
    }

    public void makeButtons() {
        beatScratchButton1 = new JButton("1");
        class ButtonListener1 implements ActionListener {

            public void actionPerformed(ActionEvent event) {
                scratchBeatPressed(0);
            }
        }
        beatScratchButton1.addActionListener(new ButtonListener1());
        beatScratchButton2 = new JButton("2");
        class ButtonListener2 implements ActionListener {

            public void actionPerformed(ActionEvent event) {
                scratchBeatPressed(1);
            }
        }
        beatScratchButton2.addActionListener(new ButtonListener2());
        beatScratchButton3 = new JButton("3");
        class ButtonListener3 implements ActionListener {

            public void actionPerformed(ActionEvent event) {
                scratchBeatPressed(2);
            }
        }
        beatScratchButton3.addActionListener(new ButtonListener3());
        beatScratchButton4 = new JButton("4");
        class ButtonListener4 implements ActionListener {

            public void actionPerformed(ActionEvent event) {
                scratchBeatPressed(3);
            }
        }
        beatScratchButton4.addActionListener(new ButtonListener4());
        beatScratchButton5 = new JButton("5");
        class ButtonListener5 implements ActionListener {

            public void actionPerformed(ActionEvent event) {
                scratchBeatPressed(4);
            }
        }
        beatScratchButton5.addActionListener(new ButtonListener5());
        beatScratchButton6 = new JButton("6");
        class ButtonListener6 implements ActionListener {

            public void actionPerformed(ActionEvent event) {
                scratchBeatPressed(5);
            }
        }
        beatScratchButton6.addActionListener(new ButtonListener6());
        beatScratchButton7 = new JButton("7");
        class ButtonListener7 implements ActionListener {

            public void actionPerformed(ActionEvent event) {
                scratchBeatPressed(6);
            }
        }
        beatScratchButton7.addActionListener(new ButtonListener7());
        beatScratchButton8 = new JButton("8");
        class ButtonListener8 implements ActionListener {

            public void actionPerformed(ActionEvent event) {
                scratchBeatPressed(7);
            }
        }
        beatScratchButton8.addActionListener(new ButtonListener8());
        beatScratchUp = new JButton(">");
        class ButtonListener9 implements ActionListener {

            public void actionPerformed(ActionEvent event) {
                scratchBeatUp();
            }
        }
        beatScratchUp.addActionListener(new ButtonListener9());
        beatScratchDown = new JButton("<");
        class ButtonListener10 implements ActionListener {

            public void actionPerformed(ActionEvent event) {
                scratchBeatDown();
            }
        }
        beatScratchDown.addActionListener(new ButtonListener10());
        recordButton = new JButton("Arm");
        class ButtonListener11 implements ActionListener {

            public void actionPerformed(ActionEvent event) {
                recButtonPressed();
            }
        }
        recordButton.addActionListener(new ButtonListener11());
        recordButton.setBackground(Color.white);
        save = new JButton("?");
        class ButtonListener12 implements ActionListener {

            public void actionPerformed(ActionEvent event) {
            }
        }
        save.addActionListener(new ButtonListener12());
        beatScratchButton1.setMargin(inset);
        beatScratchButton2.setMargin(inset);
        beatScratchButton3.setMargin(inset);
        beatScratchButton4.setMargin(inset);
        beatScratchButton5.setMargin(inset);
        beatScratchButton6.setMargin(inset);
        beatScratchButton7.setMargin(inset);
        beatScratchButton8.setMargin(inset);
        beatScratchUp.setMargin(inset);
        beatScratchDown.setMargin(inset);
        recordButton.setMargin(inset);
        save.setMargin(inset);
    }

    public void scratchBeatPressed(int beatSelect) {
        channel.getChannelBeat().scratchBeatPressed(beatSelect);
    }

    public void scratchBeatUp() {
        channel.getChannelBeat().scratchBeatUp();
    }

    public void scratchBeatDown() {
        channel.getChannelBeat().scratchBeatDown();
    }

    public void recButtonPressed() {
        channel.getChannelBeat().recButtonPressed();
    }
}
