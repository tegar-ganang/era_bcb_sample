package eu.davidgamez.mas.agents.pitchinverter.midi;

import java.util.ArrayList;
import java.util.TreeMap;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;
import org.w3c.dom.Document;
import eu.davidgamez.mas.Constants;
import eu.davidgamez.mas.Util;
import eu.davidgamez.mas.exception.MASAgentException;
import eu.davidgamez.mas.exception.MASXmlException;
import eu.davidgamez.mas.gui.MsgHandler;
import eu.davidgamez.mas.midi.Agent;
import eu.davidgamez.mas.midi.Track;

public class PitchInverter extends Agent implements Constants {

    /** Note that serves as the inversion point */
    int inversionPitch = 60;

    /** Constructor */
    public PitchInverter() {
        super("Pitch Inverter", "Pitch Inverter", "PitchInverter");
    }

    @Override
    public String getXML(String indent) {
        String tmpStr = indent + "<midi_agent>";
        tmpStr += super.getXML(indent + "\t");
        tmpStr += indent + "\t<inversion_pitch>" + inversionPitch + "</inversion_pitch>";
        tmpStr += indent + "</midi_agent>";
        return tmpStr;
    }

    @Override
    public void loadFromXML(String xmlStr) throws MASXmlException {
        super.loadFromXML(xmlStr);
        try {
            Document xmlDoc = Util.getXMLDocument(xmlStr);
            inversionPitch = Util.getIntParameter("inversion_pitch", xmlDoc);
        } catch (Exception ex) {
            System.out.println(xmlStr);
            ex.printStackTrace();
            MsgHandler.error(ex.getMessage());
        }
    }

    @Override
    public boolean updateTracks(long bufferStart_ppq, long bufferEnd_ppq) throws InvalidMidiDataException {
        for (Track midiTrack : trackMap.values()) {
            TreeMap<Long, ArrayList<ShortMessage>> tmpMsgMap = midiTrack.getMidiMessages();
            for (Long key : tmpMsgMap.keySet()) {
                for (ShortMessage msg : tmpMsgMap.get(key)) {
                    if (msg.getCommand() == ShortMessage.NOTE_ON || msg.getCommand() == ShortMessage.NOTE_OFF) {
                        int oldPitch = msg.getData1(), newPitch;
                        if (oldPitch > inversionPitch) newPitch = inversionPitch - (oldPitch - inversionPitch); else newPitch = inversionPitch + (inversionPitch - oldPitch);
                        if (newPitch < 0) newPitch = 0;
                        if (newPitch > 127) newPitch = 127;
                        msg.setMessage(msg.getCommand(), msg.getChannel(), newPitch, msg.getData2());
                    }
                }
            }
        }
        return true;
    }

    @Override
    public void connectionStatusChanged() {
    }

    @Override
    public void enabledStatusChanged() {
    }

    @Override
    protected void reset() {
    }

    public int getInversionPitch() {
        return inversionPitch;
    }

    public void setInversionPitch(int inversionPitch) throws MASAgentException {
        if (inversionPitch < 0 || inversionPitch > 127) throw new MASAgentException("Inverter Agent: Inversion pitch out of range: " + inversionPitch);
        this.inversionPitch = inversionPitch;
    }
}
