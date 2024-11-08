package eu.davidgamez.mas.gui.model;

import java.util.ArrayList;
import javax.swing.table.AbstractTableModel;
import eu.davidgamez.mas.Globals;
import eu.davidgamez.mas.midi.MIDIEvent;

public class MIDIEventsModel extends AbstractTableModel {

    /** Holds the midi events that are displayed by the model */
    private ArrayList<MIDIEvent> midiEventList = new ArrayList<MIDIEvent>();

    /** Time at which play started */
    long startTime_ms;

    /** Constructor */
    public MIDIEventsModel() {
    }

    /** Returns the number of columns */
    public int getColumnCount() {
        return 6;
    }

    /** Returns the headers for the columns */
    public String getColumnName(int columnIndex) {
        switch(columnIndex) {
            case 0:
                return "Time(ms)";
            case 1:
                return "Time(ticks)";
            case 2:
                return "Command";
            case 3:
                return "Data 1";
            case 4:
                return "Data 2";
            case 5:
                return "Channel";
            default:
                return "";
        }
    }

    /** Returns the number of rows */
    public int getRowCount() {
        return midiEventList.size();
    }

    /** Returns the data at the specified position */
    public Object getValueAt(int row, int col) {
        switch(col) {
            case 0:
                return new Long(midiEventList.get(row).getTimeStamp());
            case 1:
                return new Long(Math.round((midiEventList.get(row).getTimeStamp() * 1000000) / Globals.getNanoSecPerTick()));
            case 2:
                return new Integer(midiEventList.get(row).getCommand());
            case 3:
                return new Integer(midiEventList.get(row).getData1());
            case 4:
                return new Integer(midiEventList.get(row).getData2());
            case 5:
                return new Integer(midiEventList.get(row).getChannel());
            default:
                return new Object();
        }
    }

    public void add(MIDIEvent event) {
        event.setTimeStamp(System.currentTimeMillis() - startTime_ms);
        midiEventList.add(event);
        fireTableDataChanged();
    }

    public void initialize() {
        midiEventList.clear();
        startTime_ms = System.currentTimeMillis();
        fireTableDataChanged();
    }
}
