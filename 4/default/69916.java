import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.event.*;
import javax.swing.ToolTipManager;
import javax.swing.JOptionPane;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JCheckBox;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.BorderFactory;
import javax.swing.KeyStroke;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import javax.swing.AbstractCellEditor;
import javax.swing.table.TableCellEditor;
import java.awt.Component;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Graphics;
import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.print.*;
import java.awt.Graphics2D;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.KeyEvent;
import java.awt.GridBagLayout;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.Date;
import java.util.Arrays;
import java.util.EventObject;
import java.io.Serializable;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import javax.swing.JFileChooser;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import javax.xml.transform.TransformerException;

public class ReportTable extends JPanel implements ItemListener, Serializable, Printable, ActionListener, ListSelectionListener, Cloneable {

    private static final long serialVersionUID = 1L;

    JTable table;

    Vector<Object> row_v = new Vector<Object>();

    Vector<Object> column_v = new Vector<Object>();

    int col_size = 0;

    private DefaultTableModel tabModel;

    private String FIELD_SEP = ",";

    private JButton menu;

    private JPopupMenu popup;

    private boolean showClass = false;

    private ReportTableSorter sorter = null;

    private boolean _isEditable = false;

    private JScrollPane scrollPane;

    private int[] classType = null;

    public ReportTable(String[] col) {
        addColumns(col);
        createTable(false);
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    public ReportTable(Object[] col) {
        addColumns(col);
        createTable(false);
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    public ReportTable(Object[] col, boolean isEditable) {
        addColumns(col);
        createTable(isEditable);
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    public ReportTable(String[] col, boolean isEditable) {
        addColumns(col);
        createTable(isEditable);
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    public ReportTable(String[] col, boolean isEditable, boolean colClass) {
        addColumns(col);
        createTable(isEditable);
        ToolTipManager.sharedInstance().registerComponent(this);
        showClass = colClass;
    }

    public ReportTable(String[] col, int[] sqlType, boolean isEditable, boolean colClass) {
        addColumns(col);
        createTable(isEditable);
        ToolTipManager.sharedInstance().registerComponent(this);
        showClass = colClass;
        classType = sqlType;
    }

    public ReportTable(Object[] col, boolean isEditable, boolean colClass) {
        addColumns(col);
        createTable(isEditable);
        ToolTipManager.sharedInstance().registerComponent(this);
        showClass = colClass;
    }

    public ReportTable(String less, String more, String b_less1, String b_more1, String b_less2, String b_more2) {
        super(new GridLayout(1, 0));
        String[] columnNames = { "<html><b><i>Values</i></b></html>", "<html><b>Aggregate</i></b></html>", "<html><b> &lt;  <i>" + less + "</i></b></html>", "<html><b> &gt;  <i>" + more + "</i></b></html>", "<html><b><i>" + b_less1 + "</i>&lt;&gt;<i>" + b_more1 + "</i></b></html>", "<html><b><i>" + b_less2 + "</i>&lt;&gt;<i>" + b_more2 + "</i></b></html>" };
        String[][] data = { { "<html><b>COUNT</b></html>", "", "", "", "", "" }, { "<html><b>AVG</b></html>", "", "", "", "", "" }, { "<html><b>MAX</b></html>", "", "", "", "", "" }, { "<html><b>MIN</b></html>", "", "", "", "", "" }, { "<html><b>SUM</b></html>", "", "", "", "", "" }, { "<html><b>DUPLICATE</b></html>", "", "", "", "", "" } };
        addColumns(columnNames);
        addRows(data);
        createTable(false);
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    ;

    private void createTable(final boolean isEditable) {
        _isEditable = isEditable;
        tabModel = new DefaultTableModel() {

            public boolean isCellEditable(int row, int col) {
                String colN = this.getColumnName(col);
                if (isEditable == false && colN.endsWith("Editable") == false) return false; else return true;
            }

            public Class getColumnClass(int col) {
                if (showClass == true) if (classType != null) {
                    return SqlType.getClass(classType[col]);
                } else {
                    for (int i = 0; i < this.getRowCount(); i++) if (getValueAt(i, col) != null) return getValueAt(i, col).getClass();
                    return (new Object()).getClass();
                }
                return (new Object()).getClass();
            }
        };
        tabModel.setDataVector(row_v, column_v);
        sorter = new ReportTableSorter(tabModel);
        table = new JTable(sorter) {

            public Component prepareRenderer(TableCellRenderer renderer, int row, int col) {
                Component c = super.prepareRenderer(renderer, row, col);
                if (c == null) c = new JLabel();
                if (row % 2 == 0) {
                    if (_isEditable == true || this.isCellEditable(row, col)) c.setBackground(new Color(0, 100, 100, 100)); else c.setBackground(new Color(0, 100, 0, 100));
                } else {
                    c.setBackground(getBackground());
                }
                if (this.isRowSelected(row) && this.isColumnSelected(col)) c.setBackground(Color.YELLOW);
                return c;
            }

            public String getToolTipText(MouseEvent e) {
                String tip = null;
                java.awt.Point p = e.getPoint();
                int rowIndex = rowAtPoint(p);
                int colIndex = columnAtPoint(p);
                int col_c = tabModel.getColumnCount();
                int row_c = tabModel.getRowCount();
                int model_col_index = 0;
                String t_total = null, t_avg = null, t_sum = null, t_dup = null;
                String first_c = null;
                if (col_c == 6 && tabModel.getColumnName(0).equals("<html><b><i>Values</i></b></html>")) {
                    String view_c_name = table.getColumnName(colIndex);
                    if (tabModel.getColumnName(0).equals(view_c_name)) return tip;
                    if (tabModel.getColumnName(1).equals(view_c_name)) model_col_index = 1;
                    if (tabModel.getColumnName(2).equals(view_c_name)) model_col_index = 2;
                    if (tabModel.getColumnName(3).equals(view_c_name)) model_col_index = 3;
                    if (tabModel.getColumnName(4).equals(view_c_name)) model_col_index = 4;
                    if (tabModel.getColumnName(5).equals(view_c_name)) model_col_index = 5;
                    t_total = (String) tabModel.getValueAt(0, 1);
                    t_avg = (String) tabModel.getValueAt(1, 1);
                    t_sum = (String) tabModel.getValueAt(4, 1);
                    t_dup = (String) tabModel.getValueAt(5, 1);
                    for (int i = 0; i < col_c; i++) {
                        String index_search = table.getColumnName(i);
                        if (index_search.compareTo("<html><b><i>Values</i></b></html>") == 0) {
                            first_c = (String) table.getValueAt(rowIndex, i);
                            break;
                        }
                    }
                    if (first_c.compareTo("<html><b>COUNT</b></html>") == 0) rowIndex = 0;
                    if (first_c.compareTo("<html><b>AVG</b></html>") == 0) rowIndex = 1;
                    if (first_c.compareTo("<html><b>MAX</b></html>") == 0) rowIndex = 2;
                    if (first_c.compareTo("<html><b>MIN</b></html>") == 0) rowIndex = 3;
                    if (first_c.compareTo("<html><b>SUM</b></html>") == 0) rowIndex = 4;
                    if (first_c.compareTo("<html><b>DUPLICATE</b></html>") == 0) rowIndex = 5;
                    switch(rowIndex) {
                        case 0:
                            if (t_total != null && t_total.equals("") == false) {
                                switch(model_col_index) {
                                    case 2:
                                    case 3:
                                    case 4:
                                    case 5:
                                        String c_val = (String) tabModel.getValueAt(0, model_col_index);
                                        if (c_val != null && c_val.equals("") == false) {
                                            float percent = (Float.parseFloat(c_val) * 100) / Float.parseFloat(t_total);
                                            tip = percent + "% of Count " + t_total;
                                        }
                                    default:
                                }
                            }
                            break;
                        case 1:
                            if (t_avg != null && t_avg.equals("") == false) {
                                switch(model_col_index) {
                                    case 2:
                                    case 3:
                                    case 4:
                                    case 5:
                                        String c_val = (String) tabModel.getValueAt(1, model_col_index);
                                        if (c_val != null && c_val.equals("") == false) {
                                            float percent = (Float.parseFloat(c_val) * 100) / Float.parseFloat(t_avg);
                                            tip = percent + "% of Total Avg. " + t_avg;
                                        }
                                    default:
                                }
                            }
                            break;
                        case 2:
                        case 3:
                            break;
                        case 4:
                            if (t_sum != null && t_sum.equals("") == false) {
                                switch(model_col_index) {
                                    case 2:
                                    case 3:
                                    case 4:
                                    case 5:
                                        String c_val = (String) tabModel.getValueAt(4, model_col_index);
                                        if (c_val != null && c_val.equals("") == false) {
                                            float percent = (Float.parseFloat(c_val) * 100) / Float.parseFloat(t_sum);
                                            tip = percent + "% of Sum " + t_sum;
                                        }
                                    default:
                                }
                            }
                            break;
                        case 5:
                            if (t_dup != null && t_dup.equals("") == false && t_dup.equals("0") == false) {
                                switch(model_col_index) {
                                    case 2:
                                    case 3:
                                    case 4:
                                    case 5:
                                        String c_val = (String) tabModel.getValueAt(5, model_col_index);
                                        if (c_val != null && c_val.equals("") == false) {
                                            float percent = (Float.parseFloat(c_val) * 100) / Float.parseFloat(t_dup);
                                            tip = percent + "% of Total Duplicate " + t_dup;
                                        }
                                    default:
                                }
                            }
                            break;
                        default:
                            break;
                    }
                } else {
                    tip = "<html>Row Index:" + (rowIndex) + "<br> Column Index:" + (colIndex) + "</html>";
                }
                return tip;
            }
        };
        table.addKeyListener(new EventKeyHandler());
        table.setDefaultEditor(Date.class, new ReportTable.DateCellEditor());
        Font font = new Font("Helvetika", Font.BOLD, 12);
        table.setFont(font);
        font = new Font("Helvetika", Font.BOLD | Font.ITALIC, 14);
        table.getTableHeader().setFont(font);
        table.getTableHeader().setBackground(new Color(200, 200, 200));
        sorter.setTableHeader(table.getTableHeader());
        table.setPreferredScrollableViewportSize(new Dimension(600, 400));
        table.setRowSelectionAllowed(true);
        table.setColumnSelectionAllowed(true);
        table.setSelectionForeground(Color.BLUE);
        this.setLayout(new BorderLayout());
        this.setOpaque(true);
        scrollPane = new JScrollPane(table);
        if (_isEditable) {
            TableRowHeader rh = new TableRowHeader(this);
            scrollPane.setRowHeaderView(rh);
            table.getSelectionModel().addListSelectionListener(this);
        }
        add(scrollPane, BorderLayout.CENTER);
        JButton csv_b = new JButton("Save as..");
        csv_b.setMnemonic('S');
        csv_b.addActionListener(this);
        csv_b.addKeyListener(new KeyBoardListener());
        JCheckBox t_sc = new JCheckBox("Horizontal Scroll");
        t_sc.setMnemonic('H');
        t_sc.setHorizontalAlignment(JCheckBox.CENTER);
        t_sc.setToolTipText("Enable Horizontal Scrollbar for Table");
        t_sc.addItemListener(this);
        JCheckBox g_ch = new JCheckBox("No Grid");
        g_ch.setMnemonic('N');
        g_ch.setHorizontalAlignment(JCheckBox.CENTER);
        g_ch.setToolTipText("Enable/Diable Grid for Table");
        g_ch.addItemListener(this);
        JButton printButton = new JButton("Print");
        printButton.setMnemonic('P');
        printButton.addActionListener(this);
        printButton.addKeyListener(new KeyBoardListener());
        menu = new JButton("Menu", TableSorter.getDownArrow());
        menu.setMnemonic('M');
        menu.setHorizontalTextPosition(menu.LEFT);
        menu.addActionListener(this);
        menu.addKeyListener(new KeyBoardListener());
        menu.setIconTextGap(8);
        JPanel page_s = new JPanel();
        GridBagLayout layout = new GridBagLayout();
        page_s.setLayout(layout);
        page_s.add(menu);
        page_s.add(t_sc);
        page_s.add(printButton);
        page_s.add(g_ch);
        page_s.add(csv_b);
        add(page_s, BorderLayout.PAGE_START);
        EmptyBorder emptyBorder = new EmptyBorder(10, 5, 5, 5);
        BevelBorder bevelBorder = new BevelBorder(BevelBorder.LOWERED);
        CompoundBorder compoundBorder = new CompoundBorder(emptyBorder, bevelBorder);
        setBorder(new CompoundBorder(compoundBorder, emptyBorder));
    }

    public void setColValue(int col_index, String[] values) {
        for (int i = 0; i < 5; i++) {
            table.setValueAt(values[i], i, col_index);
        }
    }

    public void setValueAt(String s, int row, int col) {
        if (row < 0 || col < 0) return;
        tabModel.setValueAt(s, row, col);
    }

    public void setValueAt(Object s, int row, int col) {
        if (row < 0 || col < 0) return;
        tabModel.setValueAt(s, row, col);
    }

    public void setTableValueAt(String s, int row, int col) {
        if (row < 0 || col < 0) return;
        table.setValueAt(s, row, col);
    }

    public void setTableValueAt(Object s, int row, int col) {
        if (row < 0 || col < 0) return;
        table.setValueAt(s, row, col);
    }

    public Object getValueAt(int row, int col) {
        return table.getValueAt(row, col);
    }

    public String getTextValueAt(int row, int col) {
        TableCellRenderer cr = table.getCellRenderer(row, col);
        if (cr == null) return null;
        Component c = cr.getTableCellRendererComponent(table, table.getValueAt(row, col), false, false, row, col);
        if (c instanceof JLabel) {
            return ((JLabel) c).getText();
        } else {
            return table.getValueAt(row, col).toString();
        }
    }

    private void addColumns(String[] colName) {
        int i;
        for (i = 0; i < colName.length; i++) column_v.addElement((String) colName[i]);
        col_size = i;
    }

    private void addColumns(Object[] colName) {
        int i;
        for (i = 0; i < colName.length; i++) column_v.addElement((String) colName[i].toString());
        col_size = i;
    }

    private void addRows(String[][] rowData) {
        for (int i = 0; i < rowData.length; i++) {
            Vector<String> newRow = new Vector<String>();
            for (int j = 0; j < rowData[i].length; j++) newRow.addElement((String) rowData[i][j]);
            row_v.addElement(newRow);
        }
    }

    public void addFillRow(String[] rowset) {
        Vector<String> newRow = new Vector<String>();
        for (int j = 0; j < rowset.length; j++) newRow.addElement((String) rowset[j]);
        row_v.addElement(newRow);
        table.addNotify();
    }

    public void addFillRow(Object[] rowset) {
        Vector<Object> newRow = new Vector<Object>();
        for (int j = 0; j < rowset.length; j++) newRow.addElement(rowset[j]);
        row_v.addElement(newRow);
        table.addNotify();
    }

    public void addFillRow(Vector rowset) {
        row_v.addElement(rowset);
        table.addNotify();
    }

    public void addRow() {
        Vector<String> newRow = new Vector<String>();
        for (int j = 0; j < col_size; j++) newRow.addElement((String) "");
        row_v.addElement(newRow);
        table.addNotify();
    }

    public void addNullRow() {
        Vector<String> newRow = new Vector<String>();
        for (int j = 0; j < col_size; j++) newRow.addElement(null);
        row_v.addElement(newRow);
        table.addNotify();
    }

    public void cleanallRow() {
        int i = table.getRowCount();
        removeRows(0, i);
    }

    public void setAutoResizeMode(int a) {
        if (a == 1) table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF); else table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
    }

    public void RTLayout() {
        table.doLayout();
    }

    public void setPreferred(int a, int b) {
        table.setPreferredScrollableViewportSize(new Dimension(a, b));
        super.repaint();
    }

    private void setFieldSep(String sep) {
        FIELD_SEP = sep;
    }

    private String getFieldSep() {
        return FIELD_SEP;
    }

    public void itemStateChanged(ItemEvent e) {
        String ch_name = ((JCheckBox) e.getSource()).getText();
        if (ch_name.compareTo("No Grid") == 0) {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                table.setIntercellSpacing(new Dimension(0, 0));
                table.setShowGrid(false);
            }
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                table.setIntercellSpacing(new Dimension(1, 1));
                table.setShowGrid(true);
            }
        } else {
            if (e.getStateChange() == ItemEvent.SELECTED) setAutoResizeMode(1);
            if (e.getStateChange() == ItemEvent.DESELECTED) setAutoResizeMode(2);
        }
    }

    public void actionPerformed(ActionEvent e) {
        String but_c = ((JButton) e.getSource()).getText();
        if (but_c.compareTo("Menu") == 0) {
            popup = new JPopupMenu();
            popup.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED, Color.CYAN, Color.BLACK));
            JMenuItem sel_a = new JMenuItem("Select All");
            sel_a.addActionListener(new TableMenuListener(table));
            sel_a.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.SHIFT_MASK));
            popup.add(sel_a);
            JMenuItem dsel_a = new JMenuItem("DeSelect All");
            dsel_a.addActionListener(new TableMenuListener(table));
            popup.add(dsel_a);
            popup.addSeparator();
            JMenuItem menu1 = new JMenuItem("Record Count");
            menu1.addActionListener(new TableMenuListener(table));
            popup.add(menu1);
            JMenuItem menu2 = new JMenuItem("Selected Count");
            menu2.addActionListener(new TableMenuListener(table));
            popup.add(menu2);
            popup.addSeparator();
            JMenuItem menu3 = new JMenuItem("Regex Search");
            menu3.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_MASK));
            menu3.addActionListener(new TableMenuListener(table));
            popup.add(menu3);
            popup.addSeparator();
            JMenuItem menu4 = new JMenuItem("Analyse Selected");
            menu4.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_MASK));
            menu4.addActionListener(new TableMenuListener(table));
            popup.add(menu4);
            popup.show(menu, 0, menu.getHeight());
        } else if (but_c.compareTo("Print") == 0) {
            PrinterJob pj = PrinterJob.getPrinterJob();
            pj.setPrintable(ReportTable.this);
            pj.printDialog();
            try {
                pj.print();
            } catch (Exception PrintException) {
                ConsoleFrame.addText("\n Print error: " + PrintException.getMessage());
            }
        } else if (but_c.compareTo("Save as..") == 0) {
            Object[] saveTypes = { "XML", "XLS", "CSV" };
            String saveFormat = (String) JOptionPane.showInputDialog(null, "Save as XML,XLS or CSV", "Save Format", JOptionPane.QUESTION_MESSAGE, null, saveTypes, saveTypes[0]);
            if (saveFormat == null) {
                return;
            } else if (saveFormat.equals("XML")) {
                saveAsXml();
            } else if (saveFormat.equals("XLS")) {
                saveAsXls();
            } else {
                saveAsCsv();
            }
        }
    }

    public int print(Graphics g, PageFormat pageFormat, int pageIndex) throws PrinterException {
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(Color.black);
        int fontHeight = g2.getFontMetrics().getHeight();
        int fontDesent = g2.getFontMetrics().getDescent();
        double pageHeight = pageFormat.getImageableHeight() - fontHeight;
        double pageWidth = pageFormat.getImageableWidth();
        double tableWidth = (double) table.getColumnModel().getTotalColumnWidth();
        double scale = 1;
        if (tableWidth >= pageWidth) {
            scale = pageWidth / tableWidth;
        }
        double headerHeightOnPage = table.getTableHeader().getHeight() * scale;
        double tableWidthOnPage = tableWidth * scale;
        double oneRowHeight = (table.getRowHeight() + table.getRowMargin()) * scale;
        int numRowsOnAPage = (int) ((pageHeight - headerHeightOnPage) / oneRowHeight);
        double pageHeightForTable = oneRowHeight * numRowsOnAPage;
        int totalNumPages = (int) Math.ceil(((double) table.getRowCount()) / numRowsOnAPage);
        if (pageIndex >= totalNumPages) {
            return Printable.NO_SUCH_PAGE;
        }
        g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
        g2.drawString("Page: " + (pageIndex + 1), (int) pageWidth / 2 - 35, (int) (pageHeight + fontHeight - fontDesent));
        g2.translate(0f, headerHeightOnPage);
        g2.translate(0f, -pageIndex * pageHeightForTable);
        if (pageIndex + 1 == totalNumPages) {
            int lastRowPrinted = numRowsOnAPage * pageIndex;
            int numRowsLeft = table.getRowCount() - lastRowPrinted;
            g2.setClip(0, (int) (pageHeightForTable * pageIndex), (int) Math.ceil(tableWidthOnPage), (int) Math.ceil(oneRowHeight * numRowsLeft));
        } else {
            g2.setClip(0, (int) (pageHeightForTable * pageIndex), (int) Math.ceil(tableWidthOnPage), (int) Math.ceil(pageHeightForTable));
        }
        g2.scale(scale, scale);
        table.paint(g2);
        g2.scale(1 / scale, 1 / scale);
        g2.translate(0f, pageIndex * pageHeightForTable);
        g2.translate(0f, -headerHeightOnPage);
        g2.setClip(0, 0, (int) Math.ceil(tableWidthOnPage), (int) Math.ceil(headerHeightOnPage));
        g2.scale(scale, scale);
        table.getTableHeader().paint(g2);
        return Printable.PAGE_EXISTS;
    }

    public void addColumn(String name) {
        TableColumn col = new TableColumn(tabModel.getColumnCount());
        table.setAutoCreateColumnsFromModel(false);
        if (table.getAutoCreateColumnsFromModel()) {
            throw new IllegalStateException();
        }
        col.setHeaderValue(name);
        column_v.addElement(name);
        table.addColumn(col);
        tabModel.addColumn(col);
        tabModel.fireTableStructureChanged();
    }

    public void hideColumn(int index) {
        TableColumn col = table.getColumnModel().getColumn(index);
        table.setAutoCreateColumnsFromModel(false);
        table.getColumnModel().removeColumn(col);
        table.removeColumn(col);
        tabModel.fireTableStructureChanged();
    }

    public void addRows(int startRow, int noOfRows) {
        int col_c = table.getColumnCount();
        Object[] row = new Object[col_c];
        if (isSorting()) {
            JOptionPane.showMessageDialog(null, "Table is in Sorted State \n Removing Sorting state to add Rows \n Rows will be added in the end.", "Information Message", JOptionPane.INFORMATION_MESSAGE);
            cancelSorting();
            startRow = table.getRowCount();
        }
        for (int i = 0; i < noOfRows; i++) tabModel.insertRow(startRow, row);
        tabModel.fireTableRowsInserted(startRow, noOfRows);
        table.scrollRectToVisible(table.getCellRect(startRow, 0, true));
    }

    public void removeRows(int startRow, int noOfRows) {
        if (!isSorting()) {
            for (int i = 0; i < noOfRows; i++) tabModel.removeRow(startRow);
        } else {
            int[] modelI = new int[noOfRows];
            for (int i = 0; i < noOfRows; i++) modelI[i] = sorter.modelIndex(startRow + i);
            Arrays.sort(modelI);
            for (int i = modelI.length - 1; i >= 0; i--) tabModel.removeRow(modelI[i]);
        }
        tabModel.fireTableRowsDeleted(startRow, noOfRows);
    }

    public Object[] copyRow(int startRow) {
        int col_c = table.getColumnCount();
        Object[] row = new Object[col_c];
        for (int i = 0; i < col_c; i++) row[i] = getValueAt(startRow, i);
        return row;
    }

    public void pasteRow(int startRow, Vector<Object[]> row) {
        if (isSorting()) {
            JOptionPane.showMessageDialog(null, " Table is in Sorted State \n Unsort to paste Rows", "Error Message", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int row_c = table.getRowCount();
        int col_c = table.getColumnCount();
        int vci = 0;
        int saveR = row_c - (startRow + row.size());
        if (saveR < 0) {
            JOptionPane.showMessageDialog(null, "Not Enough Rows left to paste " + row.size() + " Rows \n Use \'Insert Clip\' instead", "Error Message", JOptionPane.ERROR_MESSAGE);
            return;
        }
        for (int i = row.size() - 1; i >= 0; i--) {
            Object[] a = row.elementAt(vci++);
            col_c = (col_c > a.length) ? a.length : col_c;
            for (int j = 0; j < col_c; j++) table.setValueAt(a[j], startRow + i, j);
        }
    }

    public void pasteRow(int startRow, Object[] row) {
        if (isSorting()) {
            JOptionPane.showMessageDialog(null, " Table is in Sorted State \n Unsort to paste Rows", "Error Message", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int row_c = table.getRowCount();
        int col_c = table.getColumnCount();
        int vci = 0;
        int saveR = row_c - (startRow + 1);
        if (saveR < 0) {
            JOptionPane.showMessageDialog(null, "Not Enough Rows left to paste " + 1 + " Row \n Use \'Insert Clip\' instead", "Error Message", JOptionPane.ERROR_MESSAGE);
            return;
        }
        Object[] a = row;
        col_c = (col_c > a.length) ? a.length : col_c;
        for (int j = 0; j < col_c; j++) table.setValueAt(a[j], startRow, j);
    }

    public void cancelSorting() {
        sorter.cancelSorting();
    }

    public boolean isSorting() {
        return sorter.isSorting();
    }

    public int modelIndex(int viewIndex) {
        if (sorter.isSorting()) return sorter.modelIndex(viewIndex); else return viewIndex;
    }

    public DefaultTableModel getModel() {
        return tabModel;
    }

    private class DateCellEditor extends AbstractCellEditor implements TableCellEditor {

        private SpinnerDateModel dm = new SpinnerDateModel();

        private JSpinner sp = new JSpinner(dm);

        private JSpinner.DateEditor sd = new JSpinner.DateEditor(sp, "dd/MM/yyyy hh:mm:ss");

        public DateCellEditor() {
        }

        public Object getCellEditorValue() {
            try {
                sp.commitEdit();
                ConsoleFrame.addText("\n New Value:" + dm.getValue().toString());
            } catch (Exception e) {
                ConsoleFrame.addText("\n Date Change Exception ......");
                return (Date) null;
            }
            return sp.getValue();
        }

        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            sp.setEditor(sd);
            if (value != null && value instanceof Date) dm.setValue(value);
            return sp;
        }

        public boolean shouldSelectCell(EventObject anEvent) {
            if (anEvent instanceof MouseEvent) {
                int clickC = ((MouseEvent) anEvent).getClickCount();
                if (clickC == 1) return true;
            }
            return false;
        }

        public boolean isCellEditable(EventObject anEvent) {
            if (anEvent instanceof MouseEvent) {
                int clickC = ((MouseEvent) anEvent).getClickCount();
                if (clickC == 2) return true;
            }
            return false;
        }
    }

    public boolean isRTEditable() {
        return _isEditable;
    }

    public Component getRowHeader() {
        return scrollPane.getRowHeader().getView();
    }

    public void valueChanged(ListSelectionEvent e) {
        boolean changing = ((TableRowHeader) getRowHeader()).isHeaderChanging();
        if (changing) return;
        ((TableRowHeader) getRowHeader()).clearSelection();
    }

    /**
    *  Display the showSaveDialog which allows the user to specify file and
    *  directory
    *
    *@return    file to be saved
    */
    private File promptForFilename() {
        File file = null;
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Arrah Technology Save File");
        chooser.setCurrentDirectory(new File("."));
        int returnVal = chooser.showSaveDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            file = chooser.getSelectedFile();
        } else {
            return null;
        }
        if (file.exists()) {
            int response = JOptionPane.showConfirmDialog(null, "Overwrite existing file?", "Confirm Overwrite", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (response == JOptionPane.CANCEL_OPTION) {
                return null;
            }
        }
        return file;
    }

    /**
    *  save the table as XLS
    */
    private void saveAsXls() {
        File xlsFile = promptForFilename();
        if (xlsFile == null) {
            return;
        }
        if (xlsFile.getName().toLowerCase().endsWith(".xls") == false) {
            File renameF = new File(xlsFile.getAbsolutePath() + ".xls");
            xlsFile = renameF;
        }
        XlsReader xlsreader = new XlsReader();
        xlsreader.write(this, xlsFile);
    }

    /**
    *  save the table as XML
    */
    public void saveAsXml() {
        File xmlFile = promptForFilename();
        if (xmlFile == null) {
            return;
        }
        if (xmlFile.getName().toLowerCase().endsWith(".xml") == false) {
            File renameF = new File(xmlFile.getAbsolutePath() + ".xml");
            xmlFile = renameF;
        }
        final XmlWriter xmlWriter = new XmlWriter();
        try {
            xmlWriter.writeXmlFile(table, xmlFile.getAbsolutePath());
        } catch (IOException exp) {
            ConsoleFrame.addText("\n Error saving XML file:" + exp.getMessage());
        }
        final DTDGenerator dtdGenerator = new DTDGenerator();
        final String dtdFilename = dtdGenerator.getDtdFilename(xmlFile);
        try {
            dtdGenerator.save(xmlFile, new File(dtdFilename));
            ConsoleFrame.addText("\n ReportTable saved SuccessFully at " + xmlFile.getAbsolutePath());
        } catch (TransformerException exp) {
            ConsoleFrame.addText("\n Error saving DTD file:" + exp.getMessage());
        } catch (FileNotFoundException exp) {
            ConsoleFrame.addText("\n Error saving DTD file:" + exp.getMessage());
        }
    }

    /**
    *  save the table as comma separated values (CSV)
    */
    private void saveAsCsv() {
        Object[] possible = { "Comma(,)", "Tab(\\t)", "Colon(:)", "Semi Colon(;)", "Space( )" };
        String des_input = (String) JOptionPane.showInputDialog(null, "Choose the Field Separator", "Field Separation Dialog", JOptionPane.QUESTION_MESSAGE, null, possible, possible[0]);
        if (des_input == null) {
            return;
        }
        if (des_input.compareTo((String) possible[1]) == 0) {
            setFieldSep("\t");
        }
        if (des_input.compareTo((String) possible[2]) == 0) {
            setFieldSep(":");
        }
        if (des_input.compareTo((String) possible[3]) == 0) {
            setFieldSep(";");
        }
        if (des_input.compareTo((String) possible[4]) == 0) {
            setFieldSep(" ");
        }
        File file = promptForFilename();
        if (file == null) {
            return;
        }
        int rowCount = table.getRowCount();
        int columnCount = table.getColumnCount();
        StringBuffer row_v = new StringBuffer("");
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(file));
            for (int j = 0; j < columnCount; j++) {
                if ((columnCount - j) > 1 == true) {
                    row_v.append(table.getColumnName(j) + getFieldSep());
                } else {
                    row_v.append(table.getColumnName(j));
                }
            }
            bw.write(row_v.toString(), 0, row_v.length());
            bw.newLine();
            bw.flush();
            row_v = new StringBuffer("");
            for (int i = 0; i < rowCount; i++) {
                for (int j = 0; j < columnCount; j++) {
                    if ((columnCount - j) > 1 == true) {
                        row_v.append(getValueAt(i, j) + getFieldSep());
                    } else {
                        row_v.append(getValueAt(i, j));
                    }
                }
                bw.write(row_v.toString(), 0, row_v.length());
                bw.newLine();
                bw.flush();
                row_v = new StringBuffer("");
            }
            bw.close();
        } catch (IOException exp) {
            ConsoleFrame.addText("\n SAVE FILE ERROR:" + exp.getMessage());
        }
    }

    private class EventKeyHandler extends KeyAdapter {

        public void keyPressed(KeyEvent e) {
            switch(e.getKeyCode()) {
                case KeyEvent.VK_V:
                    if (e.isControlDown()) {
                        if (!isRTEditable()) {
                            JOptionPane.showMessageDialog(null, " Table is not Editable", "Error Message", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        int sr = table.getSelectedRow();
                        int sc = table.getSelectedColumn();
                        if (sr == -1 || sc == -1) {
                            JOptionPane.showMessageDialog(null, " Select Column to Paste", "Error Message", JOptionPane.ERROR_MESSAGE);
                            return;
                        } else {
                            pasteFromClip(sr, sc);
                        }
                    }
                    break;
            }
        }
    }

    private void pasteFromClip(int startRow, int startCol) {
        try {
            Clipboard system = Toolkit.getDefaultToolkit().getSystemClipboard();
            String trstring = (String) (system.getContents(this).getTransferData(DataFlavor.stringFlavor));
            StringTokenizer st1 = new StringTokenizer(trstring, "\n");
            for (int i = 0; st1.hasMoreTokens(); i++) {
                String rowstring = st1.nextToken();
                StringTokenizer st2 = new StringTokenizer(rowstring, "\t");
                for (int j = 0; st2.hasMoreTokens(); j++) {
                    String value = (String) st2.nextToken();
                    if (startRow + i < table.getRowCount() && startCol + j < table.getColumnCount()) table.setValueAt(value, startRow + i, startCol + j);
                }
            }
        } catch (Exception e) {
            ConsoleFrame.addText("\n Paste Error:" + e.getMessage());
        }
    }

    public static ReportTable copyTable(ReportTable rpt, boolean editable, boolean showClass) {
        if (rpt == null) return null;
        int colC = rpt.table.getColumnCount();
        int rowC = rpt.table.getRowCount();
        String[] colName = new String[colC];
        for (int i = 0; i < colC; i++) colName[i] = rpt.table.getColumnName(i);
        ReportTable newRT = new ReportTable(colName, editable, showClass);
        for (int i = 0; i < rowC; i++) {
            newRT.addRow();
            for (int j = 0; j < colC; j++) newRT.table.setValueAt(rpt.table.getValueAt(i, j), i, j);
        }
        return newRT;
    }

    public Object[] getRow(int rowIndex) {
        int colC = tabModel.getColumnCount();
        Object[] obj = new Object[colC];
        if (rowIndex < 0 || rowIndex >= tabModel.getRowCount()) return obj;
        for (int i = 0; i < colC; i++) {
            obj[i] = this.getValueAt(rowIndex, i);
        }
        return obj;
    }

    public void transposeTable() {
        int rows = table.getRowCount();
        int cols = table.getColumnCount();
        int diff = cols - rows;
        for (int i = 0; i < diff; i++) {
            addRow();
        }
        for (int i = 0; i < rows; i++) {
            String colName = "Column_" + (i + 1);
            addColumn(colName);
        }
        for (int i = 0; i < rows; i++) for (int j = 0; j < cols; j++) {
            Object obj = table.getValueAt(i, j);
            table.setValueAt(obj, j, i + cols);
        }
        for (int i = 0; i < cols; i++) {
            hideColumn(0);
        }
        removeRows(cols, rows - cols);
    }
}
