package eu.davidgamez.mas.agents.copy.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.sound.midi.ControllerEventListener;
import javax.sound.midi.ShortMessage;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import eu.davidgamez.mas.Constants;
import eu.davidgamez.mas.Util;
import eu.davidgamez.mas.agents.copy.midi.CopyAgent;
import eu.davidgamez.mas.event.ConnectionListener;
import eu.davidgamez.mas.event.KnobDoubleClickListener;
import eu.davidgamez.mas.exception.MASAgentException;
import eu.davidgamez.mas.exception.MASXmlException;
import eu.davidgamez.mas.gui.AgentPropertiesPanel;
import eu.davidgamez.mas.gui.DKnob;
import eu.davidgamez.mas.gui.MsgHandler;
import eu.davidgamez.mas.gui.dialog.ControllerDialog;
import eu.davidgamez.mas.midi.Agent;
import eu.davidgamez.mas.midi.MIDIDeviceManager;
import eu.davidgamez.mas.midi.Track;

public class CopyPanel extends AgentPropertiesPanel implements ConnectionListener, Constants, KnobDoubleClickListener, ControllerEventListener, ChangeListener {

    /** Knob controlling the probability that a note will be produced. */
    private DKnob delayKnob = new DKnob();

    /** Label for delay knob */
    private JLabel delayLabel = new JLabel();

    /** List of track IDs, used for mapping from a combo box index to a track id */
    private ArrayList<String> trackIDList = new ArrayList<String>();

    /** Holds a list of tracks to connect from */
    private JComboBox fromCombo = new JComboBox();

    /** Holds a list of tracks to connect to */
    private JComboBox toCombo = new JComboBox();

    public CopyPanel() {
        super("Copy");
        delayKnob.setValue(0f);
        delayLabel.setText("Delay: 0");
        delayKnob.setRange(0, CopyAgent.MAX_DELAY_BEATS);
        delayKnob.addDoubleClickListener(this);
        delayKnob.addChangeListener(this);
        Box mainVBox = Box.createVerticalBox();
        JPanel tempPanel = new JPanel(new BorderLayout(3, 3));
        tempPanel.add(delayKnob, BorderLayout.NORTH);
        tempPanel.add(delayLabel, BorderLayout.CENTER);
        Box knobBox = Box.createHorizontalBox();
        knobBox.add(Box.createHorizontalStrut(50));
        knobBox.add(tempPanel);
        knobBox.add(Box.createHorizontalGlue());
        mainVBox.add(knobBox);
        mainVBox.add(Box.createVerticalStrut(10));
        Box connectionBox = Box.createHorizontalBox();
        connectionBox.add(new JLabel("From: "));
        connectionBox.add(fromCombo);
        connectionBox.add(new JLabel(" To: "));
        connectionBox.add(toCombo);
        mainVBox.add(connectionBox);
        this.add(mainVBox, BorderLayout.CENTER);
    }

    @Override
    public boolean applyButtonPressed() {
        setCopyConnection();
        return true;
    }

    @Override
    public boolean cancelButtonPressed() {
        return true;
    }

    @Override
    public void loadAgentProperties() throws Exception {
        loadTrackNames();
        delayKnob.setValue((float) getCopyAgent().getDelay_ppq() / (float) (CopyAgent.MAX_DELAY_BEATS * PPQ_RESOLUTION));
    }

    @Override
    public void loadFromXML(String arg0) throws MASXmlException {
    }

    @Override
    public boolean okButtonPressed() {
        setCopyConnection();
        return true;
    }

    @Override
    public void knobDoubleClicked(int arg0, int arg1) {
        ControllerDialog contDialog = new ControllerDialog(this);
        contDialog.showDialog(400, 400);
        if (contDialog.midiControllerEnabled()) {
            delayKnob.setControllerText(String.valueOf(contDialog.getMidiController()));
            int densityControllerNumber = contDialog.getMidiController();
            MIDIDeviceManager.getMidiInputHandler().addControllerEventListener(this, new int[] { densityControllerNumber });
        } else {
            delayKnob.setControllerText("");
        }
    }

    @Override
    public void connectionChangeOccurred() {
        loadTrackNames();
    }

    @Override
    public void controlChange(ShortMessage shortMsg) {
        delayKnob.setValue((float) shortMsg.getData2() / 127f);
    }

    @Override
    public String getXML(String indent) {
        String panelStr = indent + "<agent_panel>";
        panelStr += super.getXML(indent + "\t");
        panelStr += indent + "</agent_panel>";
        return panelStr;
    }

    @Override
    public void setAgent(Agent ag) throws MASAgentException {
        super.setAgent(ag);
        getCopyAgent().addConnectionListener(this);
    }

    @Override
    public void stateChanged(ChangeEvent ev) {
        if (ev.getSource() == delayKnob) {
            int delayValue = Math.round(delayKnob.getValue() * CopyAgent.MAX_DELAY_BEATS);
            delayLabel.setText("Delay: " + delayValue);
            try {
                getCopyAgent().setDelay_beats(delayValue);
            } catch (Exception ex) {
                MsgHandler.error(ex);
            }
        }
    }

    /** Returns an appropriately cast reference to the MIDI agent associated with this interface */
    private eu.davidgamez.mas.agents.copy.midi.CopyAgent getCopyAgent() {
        return (eu.davidgamez.mas.agents.copy.midi.CopyAgent) agent;
    }

    /** Fills the combo boxes with the names of the tracks */
    private void loadTrackNames() {
        trackIDList.clear();
        fromCombo.removeAllItems();
        toCombo.removeAllItems();
        HashMap<String, Track> tmpTrkMap = getCopyAgent().getTrackMap();
        int cntr = 0, fromSelIndx = -1, toSelIndx = -1;
        for (String trkID : tmpTrkMap.keySet()) {
            trackIDList.add(trkID);
            fromCombo.addItem(tmpTrkMap.get(trkID).getName() + " (" + String.valueOf(tmpTrkMap.get(trkID).getChannel() + 1) + ")");
            toCombo.addItem(tmpTrkMap.get(trkID).getName() + " (" + String.valueOf(tmpTrkMap.get(trkID).getChannel() + 1) + ")");
            if (trkID.equals(getCopyAgent().getFromTrackID())) fromSelIndx = cntr;
            if (trkID.equals(getCopyAgent().getToTrackID())) toSelIndx = cntr;
            ++cntr;
        }
        if (fromSelIndx >= 0) fromCombo.setSelectedIndex(fromSelIndx);
        if (toSelIndx >= 0) toCombo.setSelectedIndex(toSelIndx);
    }

    /** Sets the from and to connection of the agent */
    private void setCopyConnection() {
        if (fromCombo.getItemCount() == 0) return;
        if (fromCombo.getItemCount() != trackIDList.size() || toCombo.getItemCount() != trackIDList.size()) {
            MsgHandler.error("CopyPanel: Combo does not match list of ids.");
            return;
        }
        try {
            getCopyAgent().setCopyConnection(trackIDList.get(fromCombo.getSelectedIndex()), trackIDList.get(toCombo.getSelectedIndex()));
        } catch (Exception ex) {
            MsgHandler.error(ex);
        }
    }
}
