package org.jsresources.apps.jsinfo;

import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;

public class SynthesizerTableModel extends MidiDeviceTableModel {

    private static final String[] sm_astrColumnNames = { "Name", "Vendor", "Description", "Version", "Class", "max.Receivers", "max. Transmitters", "Position (ms)", "Channels", "Voices", "Latency (us)" };

    public SynthesizerTableModel() {
        super();
    }

    protected boolean useIt(MidiDevice device) {
        return (device instanceof Synthesizer);
    }

    protected String[] getColumnNames() {
        return sm_astrColumnNames;
    }

    public Object getValueAt(int nRow, int nColumn) {
        if (nColumn < 8) {
            return super.getValueAt(nRow, nColumn);
        }
        Synthesizer device = (Synthesizer) m_devices.get(nRow);
        switch(nColumn) {
            case 8:
                return "" + device.getChannels().length;
            case 9:
                return "" + device.getMaxPolyphony();
            case 10:
                return "" + device.getLatency();
            default:
                return null;
        }
    }

    public static String getDefaultSynthesizerName() {
        String strSynthesizerName = null;
        try {
            Synthesizer defaultSynthesizer = MidiSystem.getSynthesizer();
            strSynthesizerName = defaultSynthesizer.getDeviceInfo().getName();
        } catch (MidiUnavailableException e) {
            if (Debug.getTraceAllExceptions()) {
                Debug.out(e);
            }
        }
        return strSynthesizerName;
    }
}
