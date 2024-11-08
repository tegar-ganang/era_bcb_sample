package ircam.jmax.editors.explode;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.undo.*;
import ircam.jmax.toolkit.*;

/**
 * A table model used to represent the content of an explode object
 * in a JTable. ExplodeTableModel is a swing TableModel
 * built on top of an ExplodeDataModel object. Note that the number of columns
 * is fixed and depends on the number of fields of a generic explode event 
 * in FTS.
 */
class ExplodeTableModel extends AbstractTableModel {

    /**
   * An explodeTableModel is created starting from an ExplodeDataModel (es. an
   * ExplodeRemoteData) */
    ExplodeTableModel(ExplodeDataModel explode) {
        super();
        this.explode = explode;
    }

    /**
   * The number of columns in this model */
    public int getColumnCount() {
        return NO_OF_FIELDS;
    }

    /**
   * The class representing a generic entry in (a column of) the table */
    public Class getColumnClass(int col) {
        return Integer.class;
    }

    /**
   * SetValue method: invoked by the cellEditor, sets the given value
   * in the Explode. Row is the event number, column is the field to change. 
   * @see WholeNumberField*/
    public void setValueAt(java.lang.Object aValue, int rowIndex, int columnIndex) {
        int value = ((Integer) aValue).intValue();
        ScrEvent event = explode.getEventAt(rowIndex);
        if (explode instanceof UndoableData) ((UndoableData) explode).beginUpdate();
        switch(columnIndex) {
            case EVTNO_COLUMN:
                return;
            case TIME_COLUMN:
                event.move(value);
                break;
            case PITCH_COLUMN:
                event.setPitch(value);
                break;
            case DURATION_COLUMN:
                event.setDuration(value);
                break;
            case VELOCITY_COLUMN:
                event.setVelocity(value);
                break;
            case CHANNEL_COLUMN:
                event.setChannel(value);
                break;
            default:
                break;
        }
        if (explode instanceof UndoableData) ((UndoableData) explode).endUpdate();
    }

    /**
   * Every field in an explode is editable, except the event number */
    public boolean isCellEditable(int row, int col) {
        return col != EVTNO_COLUMN;
    }

    /**
   * Returns the Name of the given column */
    public String getColumnName(int col) {
        switch(col) {
            case EVTNO_COLUMN:
                return "Evt. no";
            case TIME_COLUMN:
                return "Start time";
            case PITCH_COLUMN:
                return "Pitch";
            case DURATION_COLUMN:
                return "Duration";
            case VELOCITY_COLUMN:
                return "Velocity";
            case CHANNEL_COLUMN:
                return "Channel";
        }
        return "";
    }

    /**
   * How many events in the database? */
    public int getRowCount() {
        return explode.length();
    }

    /**
   * Returns the value of the given field of the given ScrEvent */
    public Object getValueAt(int row, int col) {
        if (col == EVTNO_COLUMN) return new Integer(row); else {
            ScrEvent temp = explode.getEventAt(row);
            int value;
            switch(col) {
                case TIME_COLUMN:
                    value = temp.getTime();
                    break;
                case PITCH_COLUMN:
                    value = temp.getPitch();
                    break;
                case DURATION_COLUMN:
                    value = temp.getDuration();
                    break;
                case VELOCITY_COLUMN:
                    value = temp.getVelocity();
                    break;
                case CHANNEL_COLUMN:
                    value = temp.getChannel();
                    break;
                default:
                    value = 0;
                    break;
            }
            return new Integer(value);
        }
    }

    /**
   * Method to access the ExplodeData this table refers to */
    public ExplodeDataModel getExplodeDataModel() {
        return explode;
    }

    ExplodeDataModel explode;

    public static final int NO_OF_FIELDS = 6;

    public static final int EVTNO_COLUMN = 0;

    public static final int TIME_COLUMN = 1;

    public static final int PITCH_COLUMN = 2;

    public static final int DURATION_COLUMN = 3;

    public static final int VELOCITY_COLUMN = 4;

    public static final int CHANNEL_COLUMN = 5;
}
