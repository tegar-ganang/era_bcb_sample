package wand.filterChannel;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import wand.ChannelFrame;

public class FilterKills extends JPanel {

    public JButton killOpen, killClose, fadeOpen, fadeClose, fadeOpenLong, fadeCloseLong;

    private Insets inset = new Insets(1, 1, 1, 1);

    public FilterKills() {
        setBackground(Color.white);
        makeButtons();
        setLayout(new GridLayout(2, 3));
        add(killOpen);
        add(fadeOpen);
        add(fadeOpenLong);
        add(killClose);
        add(fadeClose);
        add(fadeCloseLong);
    }

    private void makeButtons() {
        killOpen = new JButton("Kill");
        class ButtonListener1 implements ActionListener {

            public void actionPerformed(ActionEvent event) {
                killOpenPressed();
            }
        }
        killOpen.addActionListener(new ButtonListener1());
        killOpen.setMargin(inset);
        killClose = new JButton("Kill");
        class ButtonListener2 implements ActionListener {

            public void actionPerformed(ActionEvent event) {
                killClosePressed();
            }
        }
        killClose.addActionListener(new ButtonListener2());
        killClose.setMargin(inset);
        fadeOpen = new JButton("Fade");
        class ButtonListener3 implements ActionListener {

            public void actionPerformed(ActionEvent event) {
                fadeOpenPressed();
            }
        }
        fadeOpen.addActionListener(new ButtonListener3());
        fadeOpen.setMargin(inset);
        fadeClose = new JButton("Fade");
        class ButtonListener4 implements ActionListener {

            public void actionPerformed(ActionEvent event) {
                fadeClosePressed();
            }
        }
        fadeClose.addActionListener(new ButtonListener4());
        fadeClose.setMargin(inset);
        fadeOpenLong = new JButton("Fade...");
        class ButtonListener9 implements ActionListener {

            public void actionPerformed(ActionEvent event) {
                fadeOpenLongPressed();
            }
        }
        fadeOpenLong.addActionListener(new ButtonListener9());
        fadeOpenLong.setMargin(inset);
        fadeCloseLong = new JButton("Fade...");
        class ButtonListener10 implements ActionListener {

            public void actionPerformed(ActionEvent event) {
                fadeCloseLongPressed();
            }
        }
        fadeCloseLong.addActionListener(new ButtonListener10());
        fadeCloseLong.setMargin(inset);
    }

    public void killOpenPressed() {
        ChannelFrame.filterChooserFrame.choiceMade("shutter");
        ChannelFrame.filterPanel.getChannelDisplay().getPattern().killOpenPressed();
    }

    public void killClosePressed() {
        ChannelFrame.filterChooserFrame.choiceMade("shutter");
        ChannelFrame.filterPanel.getChannelDisplay().getPattern().killClosePressed();
    }

    public int fadeCount = 100;

    public boolean fadeOpenFlag = false;

    public boolean fadeCloseFlag = false;

    public boolean fadeOpenLongFlag = false;

    public boolean fadeCloseLongFlag = false;

    private double pushTime;

    private int shortFadeMillis = 200;

    private int longFadeMillis = 2000;

    public void fadeOpenPressed() {
        pushTime = System.currentTimeMillis();
        ChannelFrame.filterChooserFrame.choiceMade("shutter");
        fadeOpenFlag = true;
        fadeCloseFlag = false;
        fadeOpenLongFlag = false;
        fadeCloseLongFlag = false;
    }

    public void fadeClosePressed() {
        pushTime = System.currentTimeMillis();
        ChannelFrame.filterChooserFrame.choiceMade("shutter");
        fadeCloseFlag = true;
        fadeOpenFlag = false;
        fadeOpenLongFlag = false;
        fadeCloseLongFlag = false;
    }

    public void fadeOpenLongPressed() {
        pushTime = System.currentTimeMillis();
        longFadeMillis = ChannelFrame.enginePanel.engine.getDelayInt() * 8;
        ChannelFrame.filterChooserFrame.choiceMade("shutter");
        fadeOpenLongFlag = true;
        fadeCount = 100;
        fadeOpenFlag = false;
        fadeCloseFlag = false;
        fadeCloseLongFlag = false;
    }

    public void fadeCloseLongPressed() {
        pushTime = System.currentTimeMillis();
        longFadeMillis = ChannelFrame.enginePanel.engine.getDelayInt() * 8;
        ChannelFrame.filterChooserFrame.choiceMade("shutter");
        fadeCloseLongFlag = true;
        fadeCount = 0;
        fadeOpenFlag = false;
        fadeOpenLongFlag = false;
        fadeCloseFlag = false;
    }

    public void clockCheck() {
        if (fadeOpenFlag == true) {
            ChannelFrame.filterPanel.filterFader.transparencySlider.setValue((int) (100 - (100 * (System.currentTimeMillis() - pushTime) / shortFadeMillis)));
            if (System.currentTimeMillis() >= pushTime + shortFadeMillis) {
                fadeOpenFlag = false;
            }
        } else if (fadeOpenLongFlag == true) {
            ChannelFrame.filterPanel.filterFader.transparencySlider.setValue((int) (100 - (100 * (System.currentTimeMillis() - pushTime) / longFadeMillis)));
            if (System.currentTimeMillis() >= pushTime + longFadeMillis) {
                fadeOpenLongFlag = false;
            }
        } else if (fadeCloseFlag == true) {
            ChannelFrame.filterPanel.filterFader.transparencySlider.setValue((int) (100 * (System.currentTimeMillis() - pushTime) / shortFadeMillis));
            if (System.currentTimeMillis() >= pushTime + shortFadeMillis) {
                fadeCloseFlag = false;
            }
        } else if (fadeCloseLongFlag == true) {
            ChannelFrame.filterPanel.filterFader.transparencySlider.setValue((int) (100 * (System.currentTimeMillis() - pushTime) / longFadeMillis));
            if (System.currentTimeMillis() >= pushTime + longFadeMillis) {
                fadeCloseLongFlag = false;
            }
        }
    }
}
