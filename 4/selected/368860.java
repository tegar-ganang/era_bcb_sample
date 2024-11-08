package wand.channelControl;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import wand.ChannelFrame;
import wand.Main;

public class TextFormatPanel extends JPanel {

    private JComboBox textList;

    private int channelID;

    private String preText;

    private String[] preTexts = { "Select pre-text...", "Choose life. Choose a job. Choose a career. Choose a family. Choose a fucking big television, choose washing machines, cars, compact disc players and electrical tin openers. Choose good health, low cholesterol and dental insurance. Choose fixed-interest mortgage repayments. Choose a starter home. Choose your friends. Choose leisurewear and matching luggage. Choose a three-piece suite on hire purchase in a range of fucking fabrics. Choose DIY and wondering who the fuck you are on a Sunday morning. Choose sitting on that couch watching mind-numbing, spirit-crushing game shows, stuffing fucking junk food into your mouth. Choose rotting away at the end of it all, pishing your last in a miserable home, nothing more than an embarrassment to the selfish, fucked-up brats you have spawned to replace yourself. Choose your future. Choose-life.", "It's your choice", "Blessed be the cracked,\nfor they let in the light\n\n(19th century asylum graffiti)" };

    public TextFormatPanel() {
        setBackground(Color.gray);
        makeButtons();
        setLayout(new GridLayout(4, 1));
        add(textList);
    }

    public void setChannelID(int chID) {
        channelID = chID;
    }

    public int getChannelID() {
        return channelID;
    }

    private void makeButtons() {
        textList = new JComboBox(preTexts);
        class ButtonListener2 implements ActionListener {

            public void actionPerformed(ActionEvent event) {
                preText = preTexts[textList.getSelectedIndex()];
                loadText(preText);
                restoreFocus();
            }
        }
        textList.addActionListener(new ButtonListener2());
    }

    private void loadText(String text) {
        ChannelFrame.controlPanel.textInputPanel.setTextAreaInput(text);
    }

    public void restoreFocus() {
        Main.channelFrame.requestFocus();
    }
}
