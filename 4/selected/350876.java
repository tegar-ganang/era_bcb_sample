package gov.sns.apps.scope;

import javax.swing.*;
import java.awt.*;
import javax.swing.table.*;
import java.text.DecimalFormat;

/**
 * SummaryView displays a summary of the scope setup.
 *
 * @author  tap
 */
public class SummaryView extends Box implements SwingConstants {

    protected ScopeModel scopeModel;

    protected ChannelTableModel channelTableModel;

    /** Creates a new instance of SummaryView */
    public SummaryView(ScopeModel aModel) {
        super(VERTICAL);
        scopeModel = aModel;
        initComponents();
    }

    /**
     * Add components to the view.
     */
    protected void initComponents() {
        addChannelTable();
    }

    /**
     * Build the channel table and add it to the summary view.
     */
    private void addChannelTable() {
        channelTableModel = new ChannelTableModel(scopeModel);
        JTable channelTable = new JTable(channelTableModel);
        channelTable.setColumnSelectionAllowed(true);
        TableColumnModel columnModel = channelTable.getColumnModel();
        columnModel.getColumn(channelTableModel.SCALE_COLUMN).setCellRenderer(getNumericCellRenderer());
        columnModel.getColumn(channelTableModel.OFFSET_COLUMN).setCellRenderer(getNumericCellRenderer());
        add(channelTable.getTableHeader());
        add(channelTable);
    }

    /**
     * Right justify text associated with numeric values.
     * @return A renderer for numeric values.
     */
    private TableCellRenderer getNumericCellRenderer() {
        return new DefaultTableCellRenderer() {

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                label.setHorizontalAlignment(RIGHT);
                return label;
            }
        };
    }
}

/**
 * ChannelTableModel is the table model for displaying the channel settings in a table.
 */
class ChannelTableModel extends AbstractTableModel implements SettingListener {

    protected final DecimalFormat offsetFormat = new DecimalFormat("#,##0.0");

    protected final DecimalFormat scaleFormat = new DecimalFormat("#,##0.0####");

    protected final int INDEX_COLUMN = 0;

    protected final int NAME_COLUMN = 1;

    protected final int ENABLE_COLUMN = 2;

    protected final int SCALE_COLUMN = 3;

    protected final int OFFSET_COLUMN = 4;

    protected ScopeModel scopeModel;

    /**
     * Constructor of the channel table model.
     * @param aModel The scope model that supplies the setup.
     */
    public ChannelTableModel(ScopeModel aModel) {
        scopeModel = aModel;
        scopeModel.addSettingListener(this);
    }

    /**
     * Get the number of table rows.
     * @return The number of table rows.
     */
    public int getRowCount() {
        return scopeModel.numChannels();
    }

    /**
     * Get the number of table columns.
     * @return The number of table columns.
     */
    public int getColumnCount() {
        return 5;
    }

    /**
     * Get the value to display for the table cell at the specified row and column.
     * @param row The table cell row.
     * @param column The table cell column.
     * @return The value to display for the specified table cell.
     */
    public Object getValueAt(int row, int column) {
        switch(column) {
            case INDEX_COLUMN:
                return new Integer(row + 1);
            case NAME_COLUMN:
                return scopeModel.getChannelModel(row).getChannelName();
            case ENABLE_COLUMN:
                return new Boolean(scopeModel.getChannelModel(row).isEnabled());
            case SCALE_COLUMN:
                return scaleFormat.format(scopeModel.getChannelModel(row).getSignalScale());
            case OFFSET_COLUMN:
                return offsetFormat.format(scopeModel.getChannelModel(row).getSignalOffset());
            default:
                return "";
        }
    }

    /** 
     * Get the title of the specified column.
     * @param column The index of the column.
     * @return The title for the specified column.
     */
    @Override
    public String getColumnName(int column) {
        switch(column) {
            case INDEX_COLUMN:
                return "Channel";
            case NAME_COLUMN:
                return "Label";
            case ENABLE_COLUMN:
                return "Enabled";
            case SCALE_COLUMN:
                return "Scale";
            case OFFSET_COLUMN:
                return "Offset";
            default:
                return "";
        }
    }

    /**
     * A setting from the sender has changed.
     * @param source The object whose setting changed.
     */
    public void settingChanged(Object source) {
        fireTableDataChanged();
    }
}
