package eu.davidgamez.mas.gui.dialog;

import java.awt.BorderLayout;
import javax.swing.JTextField;
import javax.swing.Box;
import javax.swing.JComboBox;
import java.awt.Insets;
import javax.swing.JLabel;
import javax.swing.JToggleButton;
import java.awt.event.*;
import java.awt.Color;
import javax.swing.*;
import eu.davidgamez.mas.gui.MainFrame;
import eu.davidgamez.mas.midi.Track;
import java.awt.Dimension;

public class TrackDialog extends JDialog implements ActionListener {

    private Track midiTrack;

    private JTextField nameTextField;

    private JComboBox midiChannelList;

    protected boolean panelSelected = false;

    private JButton okButton, cancelButton;

    private JPanel holdingPanel = new JPanel();

    public TrackDialog(MainFrame mainFrame, Track mTrack, int xPos, int yPos) {
        super(mainFrame, "Track Properties", true);
        this.midiTrack = mTrack;
        holdingPanel.setLayout(new BorderLayout());
        String[] channelNumbers = new String[16];
        for (int i = 0; i < 16; i++) channelNumbers[i] = Integer.toString(i + 1);
        midiChannelList = new JComboBox(channelNumbers);
        midiChannelList.setBackground(Color.white);
        midiChannelList.setSelectedIndex(midiTrack.getChannel());
        nameTextField = new JTextField(midiTrack.getName(), 12);
        JPanel midiPanel = new JPanel();
        Box verticalBox = Box.createVerticalBox();
        Box nameBox = Box.createHorizontalBox();
        nameBox.add(new JLabel("Name"));
        nameBox.add(Box.createHorizontalStrut(5));
        nameBox.add(nameTextField);
        nameBox.add(Box.createHorizontalGlue());
        verticalBox.add(nameBox);
        Box channelBox = Box.createHorizontalBox();
        channelBox.add(new JLabel("Channel"));
        channelBox.add(Box.createHorizontalStrut(5));
        channelBox.add(midiChannelList);
        channelBox.add(Box.createHorizontalGlue());
        verticalBox.add(Box.createVerticalStrut(10));
        verticalBox.add(channelBox);
        midiPanel.add(verticalBox);
        JPanel buttonPane = new JPanel();
        buttonPane.add(okButton = createButton("Ok"));
        okButton.addActionListener(this);
        buttonPane.add(cancelButton = createButton("Cancel"));
        cancelButton.addActionListener(this);
        holdingPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        holdingPanel.add(midiPanel, BorderLayout.NORTH);
        holdingPanel.add(buttonPane, BorderLayout.SOUTH);
        getContentPane().add(holdingPanel, BorderLayout.CENTER);
        this.pack();
        this.setLocation(xPos, yPos);
        this.setVisible(true);
    }

    public void actionPerformed(ActionEvent ev) {
        if (ev.getSource() == okButton) {
            midiTrack.setChannel(midiChannelList.getSelectedIndex());
            if (nameTextField.getText().equals("")) midiTrack.setName("Untitled"); else midiTrack.setName(nameTextField.getText());
            this.setVisible(false);
        } else {
            this.setVisible(false);
        }
    }

    private JButton createButton(String label) {
        JButton button = new JButton(label);
        button.setPreferredSize(new Dimension(80, 20));
        return button;
    }
}
