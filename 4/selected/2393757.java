package com.cameocontrol.cameo.gui;

import javax.swing.table.AbstractTableModel;
import com.cameocontrol.cameo.control.ConsoleFade;
import com.cameocontrol.cameo.control.ConsoleInquiry;
import com.cameocontrol.cameo.control.CameoFade;
import java.util.Iterator;
import java.util.Vector;

public class SSheetTableModel extends AbstractTableModel {

    private ConsoleInquiry _console;

    private boolean editable = false;

    private LevelToString _levelTrans;

    private Vector<String> columnNames;

    SSheetTableModel(ConsoleInquiry c) {
        _console = c;
        columnNames = new Vector<String>();
        _levelTrans = new LevelToString();
        columnNames.add("");
        columnNames.add("Cue #");
        for (int x = 0; x < _console.getTotalChannels(); x++) columnNames.add(Integer.toString(x + 1));
    }

    public String getColumnName(int col) {
        return columnNames.get(col);
    }

    public int getRowCount() {
        return _console.getTotalCues();
    }

    public int getColumnCount() {
        return columnNames.size();
    }

    public boolean isCellEditable(int row, int col) {
        return false;
    }

    private ConsoleFade getCueIndexed(int index) {
        Iterator<ConsoleFade> cues = _console.getCues();
        ConsoleFade fade = cues.next();
        for (int x = 0; x < index && cues.hasNext(); x++) fade = cues.next();
        return fade;
    }

    public Object getValueAt(int row, int col) {
        ConsoleFade cue = getCueIndexed(row);
        switch(col) {
            case 0:
                if (_console.isCurrentCue(cue.getNumber())) return ">"; else return "";
            case 1:
                return Float.toString(cue.getNumber() / (float) 1000);
            default:
                if (cue.getCue().getChannel(col - 2).getLevel() >= 0) return _levelTrans.toString(cue.getCue().getChannel(col - 2).getLevel()); else return "";
        }
    }
}
