package se.antimon.colourcontrols;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;
import javax.sound.midi.MidiDevice;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import se.antimon.colourcontrols.ColourControl.ColorSet;

public class ColourControlPanel extends JPanel implements ColorEventListener, ActionListener, ChangeListener, ListSelectionListener {

    private static String deviceTooltip = null;

    private ControlValuePanel controlValuePanel = new ControlValuePanel();

    private ColourControl control;

    private ColorPanel colorPanel = new ColorPanel(ColorSet.COLOR, Color.white);

    private ColorPanel baseColorPanel = new ColorPanel(ColorSet.BASE_COLOR, Color.white);

    private JComboBox channelCB;

    private JComboBox deviceCB;

    private JSpinner controlSpinner = new JSpinner();

    private JList sweeperList = new JList();

    private boolean initialized = false;

    private static synchronized String getTooltip() {
        if (deviceTooltip == null) {
            StringBuffer buffer = new StringBuffer();
            int index = 1;
            buffer.append("<html><h3>MIDI Devices:</h3>");
            for (MidiDevice.Info deviceInfo : MidiManager.getMidiDevices()) {
                buffer.append("<i>Dev " + (index++) + ":</i> " + deviceInfo);
                if (index < MidiManager.getMidiDevices().size()) {
                    buffer.append("<br>");
                }
            }
            deviceTooltip = buffer.toString();
        }
        return deviceTooltip;
    }

    public void valueChanged(ListSelectionEvent e) {
        if (e.getSource().equals(sweeperList)) {
            int selectedIndex = sweeperList.getMinSelectionIndex();
            Sweeper selectedSweeper = ColourControlsMain.getGUI().getImageGUIs().get(selectedIndex).getSweeper();
            control.setSweeper(selectedSweeper);
        }
    }

    public ColourControl getControl() {
        return control;
    }

    public void stateChanged(ChangeEvent arg0) {
        if (arg0.getSource().equals(controlSpinner)) {
            control.setControl(((Integer) controlSpinner.getValue()).intValue());
        }
    }

    public void actionPerformed(ActionEvent arg0) {
        if (arg0.getSource().equals(channelCB)) {
            control.setChannel(channelCB.getSelectedIndex() + 1);
        } else if (arg0.getSource().equals(deviceCB)) {
            if (deviceCB.getSelectedIndex() > 0) {
                control.setDevice(MidiManager.getMidiDevices().get(deviceCB.getSelectedIndex() - 1));
            } else {
                control.setDevice(null);
            }
        }
    }

    public void newColor(ColorSet colorSet, Color color) {
        switch(colorSet) {
            case BASE_COLOR:
                control.setBaseColor(color);
                break;
            case COLOR:
                control.setColor(color);
        }
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (initialized == false) {
            initialized = true;
            channelCB.setPreferredSize(channelCB.getPreferredSize());
            deviceCB.setPreferredSize(deviceCB.getPreferredSize());
            controlSpinner.setPreferredSize(controlSpinner.getPreferredSize());
            updateInfo();
            repaint();
        }
    }

    public void updateInfo() {
        channelCB.getModel().setSelectedItem("#" + control.getChannel());
        deviceCB.setSelectedIndex(MidiManager.getMidiDevices().indexOf(control.getDevice()) + 1);
        controlSpinner.setValue(control.getControl());
        colorPanel.setColor(control.getColor());
        baseColorPanel.setColor(control.getBaseColor());
        String[] newSweeperList = new String[(ColourControlsMain.getGUI().getImageGUIs().size())];
        int index = 0;
        for (ImageGUI imageGUI : ColourControlsMain.getGUI().getImageGUIs()) {
            newSweeperList[index++] = imageGUI.getSweeper().getName();
            MyLogger.debug("List: " + imageGUI.getSweeper().getName());
        }
        sweeperList.setListData(newSweeperList);
        if (control.getSweeper() != null) {
            sweeperList.setSelectedValue(control.getSweeper().getName(), true);
        }
        updateControlValue();
        repaint();
    }

    public void updateControlValue() {
        controlValuePanel.setValue(control.getFinalResult());
    }

    public ColourControlPanel(ColourControl control) {
        this.control = control;
        controlValuePanel.setPreferredSize(new Dimension(20, 30));
        controlValuePanel.setBorder(BorderFactory.createLineBorder(Color.black));
        colorPanel.setPreferredSize(new Dimension(30, 30));
        colorPanel.setBorder(BorderFactory.createLineBorder(Color.black));
        colorPanel.addColorEventListener(this);
        baseColorPanel.setPreferredSize(new Dimension(30, 30));
        baseColorPanel.setBorder(BorderFactory.createLineBorder(Color.black));
        baseColorPanel.addColorEventListener(this);
        String[] channelStrings = new String[16];
        for (int i = 0; i < 16; i++) {
            channelStrings[i] = "#" + (i + 1);
        }
        channelCB = new JComboBox(channelStrings);
        channelCB.addActionListener(this);
        Vector<String> devicesIndexes = new Vector<String>();
        devicesIndexes.add("Dev -");
        for (int i = 0; i < MidiManager.getMidiDevices().size(); i++) {
            devicesIndexes.add("Dev " + (i + 1));
        }
        deviceCB = new JComboBox(devicesIndexes);
        deviceCB.addActionListener(this);
        deviceCB.setToolTipText(getTooltip());
        controlSpinner.setValue(new Integer(100));
        controlSpinner.addChangeListener(this);
        sweeperList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        sweeperList.setVisibleRowCount(2);
        JScrollPane listScroller = new JScrollPane(sweeperList);
        listScroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        listScroller.setPreferredSize(new Dimension(180, 30));
        sweeperList.addListSelectionListener(this);
        this.add(controlValuePanel);
        this.add(colorPanel);
        this.add(baseColorPanel);
        this.add(channelCB);
        this.add(deviceCB);
        this.add(new JSeparator());
        this.add(controlSpinner);
        this.add(listScroller);
    }
}
