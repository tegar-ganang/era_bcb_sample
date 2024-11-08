package consciouscode.bonsai.components;

import consciouscode.bonsai.channels.BasicChannel;
import consciouscode.bonsai.channels.Channel;
import consciouscode.bonsai.channels.ChannelEvent;
import consciouscode.bonsai.channels.ChannelListener;
import consciouscode.logging.PrintLog;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import org.apache.commons.logging.Log;

/**
   A Swing {@link javax.swing.table.TableModel TableModel} that uses a modeled
   list to store its rows.

   Subclasses will generally override {@link #getColumnCount},
   {@link #getColumnName}, and {@link #getColumnValue}.  This allows the
   subclass to determine how each element is presented across its row.
*/
public class BListTableModel extends AbstractTableModel implements ChannelListener {

    /**
       Identifies the {@link Channel} holding the current list elements.
    */
    public static final String CHANNEL_ELEMENTS = "elements";

    public BListTableModel() {
        this(new BasicChannel());
    }

    public BListTableModel(Channel elements) {
        myElementsChannel = elements;
        prepareChannels();
    }

    public Log getLog() {
        if (myLog == null) {
            myLog = new PrintLog();
        }
        return myLog;
    }

    public void setLog(Log log) {
        myLog = log;
    }

    public void addColumn(BListTableColumn column) {
        if (myColumns == null) {
            myColumns = new ArrayList<BListTableColumn>(1);
        }
        myColumns.add(column);
    }

    public Channel getElementsChannel() {
        return myElementsChannel;
    }

    public Channel getChannel(String name) {
        return (name.equals(CHANNEL_ELEMENTS) ? myElementsChannel : null);
    }

    public int getSize() {
        Object listObject = myElementsChannel.getValue();
        if (listObject instanceof List<?>) {
            List<?> list = (List<?>) listObject;
            return list.size();
        } else if (listObject instanceof Object[]) {
            Object[] array = (Object[]) listObject;
            return array.length;
        }
        return 0;
    }

    public Object getElementAt(int i) {
        List<?> list = getElementsAsList();
        if (list != null) {
            return list.get(i);
        }
        return null;
    }

    public int indexOf(Object obj) {
        List<?> list = getElementsAsList();
        if (list != null) {
            return list.indexOf(obj);
        }
        return -1;
    }

    public final int getRowCount() {
        return getSize();
    }

    public int getColumnCount() {
        return (myColumns == null ? 1 : myColumns.size());
    }

    @Override
    public String getColumnName(int columnIndex) {
        if (myColumns == null) {
            return super.getColumnName(columnIndex);
        }
        BListTableColumn col = myColumns.get(columnIndex);
        return col.getHeader();
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (myColumns == null) {
            return super.getColumnClass(columnIndex);
        }
        BListTableColumn col = myColumns.get(columnIndex);
        return col.getColumnClass();
    }

    public final Object getValueAt(int rowIndex, int columnIndex) {
        return getColumnValue(getElementAt(rowIndex), columnIndex);
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (myColumns != null) {
            BListTableColumn column = myColumns.get(columnIndex);
            Object rowObject = getElementAt(rowIndex);
            column.setColumnValue(rowObject, aValue);
            saveRow(rowIndex, rowObject);
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        if (myColumns == null) {
            return false;
        }
        BListTableColumn col = myColumns.get(columnIndex);
        return col.isEditable(getElementAt(rowIndex));
    }

    public void channelUpdate(ChannelEvent event) {
        Object source = event.getSource();
        if (source == myElementsChannel) {
            List<?> oldList = (List<?>) event.getOldValue();
            List<?> newList = (List<?>) event.getNewValue();
            int oldSize = (oldList != null ? oldList.size() : 0);
            int newSize = (newList != null ? newList.size() : 0);
            if (newSize < oldSize) {
                fireTableRowsDeleted(newSize, oldSize - 1);
                if (newSize != 0) {
                    fireTableRowsUpdated(0, newSize - 1);
                }
            } else if (oldSize < newSize) {
                fireTableRowsInserted(oldSize, newSize - 1);
                if (oldSize != 0) {
                    fireTableRowsUpdated(0, oldSize - 1);
                }
            } else {
                if (newSize != 0) {
                    fireTableRowsUpdated(0, newSize - 1);
                }
            }
        }
    }

    protected Object getColumnValue(Object element, int columnIndex) {
        if (myColumns == null) {
            return element;
        }
        BListTableColumn column = myColumns.get(columnIndex);
        return column.getColumnValue(element);
    }

    protected void prepareChannels() {
        myElementsChannel.addChannelListener(this);
    }

    protected List<?> getElementsAsList() {
        Object listObject = myElementsChannel.getValue();
        if (listObject instanceof List<?>) {
            return (List<?>) listObject;
        } else if (listObject instanceof Object[]) {
            return Arrays.asList((Object[]) listObject);
        }
        return null;
    }

    /**
       Saves a new value for a row. This method is called whenever a cell
       editing session is completed.
       <p>
       This implementation does nothing; subclasses should override it to
       provide appropriate behavior.
    */
    protected void saveRow(int rowIndex, Object rowValue) {
    }

    private Log myLog;

    private Channel myElementsChannel;

    /**
       TODO: Should be BListTableColumn[] for space/speed.
    */
    private ArrayList<BListTableColumn> myColumns;
}
