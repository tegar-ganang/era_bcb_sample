package net.sf.jga.swing.spreadsheet;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import javax.swing.CellRendererPane;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import net.sf.jga.fn.Generator;
import net.sf.jga.fn.UnaryFunctor;
import net.sf.jga.fn.adaptor.Constant;
import net.sf.jga.fn.adaptor.Identity;
import net.sf.jga.parser.FunctorParser;
import net.sf.jga.parser.FunctorRef;
import net.sf.jga.parser.GeneratorRef;
import net.sf.jga.parser.JFXGParser;
import net.sf.jga.parser.ParseException;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

public class Spreadsheet extends JTable {

    static final long serialVersionUID = -4784933072621672138L;

    transient Parser _parser = new Parser();

    private SpreadsheetTableModel _model;

    private RowHeader _rowHeader;

    private UnaryFunctor<String, ?> _statusFn = new Identity<String>();

    private boolean _initialized;

    public Spreadsheet(int rows, int cols) {
        super();
        _model = new SpreadsheetTableModel(rows, cols);
        super.setModel(_model);
        setAutoCreateColumnsFromModel(false);
        setAutoResizeMode(AUTO_RESIZE_OFF);
        setCellSelectionEnabled(true);
        setCellEditor(new Cell.Editor());
        TableColumnModel columns = getColumnModel();
        for (int i = 0; i < cols; ++i) {
            columns.getColumn(i).setHeaderValue(String.valueOf(i));
        }
        getTableHeader().setReorderingAllowed(false);
        _rowHeader = new RowHeader(this);
        _parser.bindThis(this);
        _initialized = true;
    }

    public void setColumnCount(int width) {
        int oldWidth = getColumnCount();
        doSetColumnCount(oldWidth, width);
        if (width > 0 && width != oldWidth) {
            _model.setColumnCount(width);
        }
    }

    private void doSetColumnCount(int oldWidth, int width) {
        if (width > 0 && width != oldWidth) {
            TableColumnModel columns = getColumnModel();
            if (oldWidth < width) {
                for (int i = oldWidth; i < width; ++i) {
                    TableColumn column = new TableColumn(i);
                    column.setHeaderValue(String.valueOf(i));
                    addColumn(column);
                }
            } else {
                for (int i = oldWidth - 1; i >= width; --i) {
                    removeColumn(columns.getColumn(i));
                }
            }
        }
    }

    public void setRowCount(int height) {
        if (height > 0 && height != getRowCount()) {
            _model.setRowCount(height);
            _rowHeader.setRowCount(height);
        }
    }

    /**
     * Returns the parser used by the spreadsheet.
     */
    public FunctorParser getParser() {
        return _parser;
    }

    private Class<?> _defaultType = Integer.class;

    /**
     * Returns the type of cells that have not be initialized.
     */
    public Class<?> getDefaultCellType() {
        return _defaultType;
    }

    /**
     * Sets the type returned by cells that have not been initialized.  By default,
     * this value is java.lang.Integer.  If the default value is non-null and is not
     * an instance of the given type, then the default value will be set to null as
     * a side-effect of setting the type.
     */
    public void setDefaultCellType(Class<?> type) {
        _defaultType = type;
        if (_defaultValue != null && !type.isInstance(_defaultValue)) _defaultValue = null;
    }

    private Object _defaultValue = new Integer(0);

    /**
     * Returns the default value of cells that have not been initialized.
     */
    public Object getDefaultCellValue() {
        return _defaultValue;
    }

    /**
     * Sets the value returned by cells that have not been initialized.  By default,
     * this value is a java.lang.Integer.ZERO.  When called, if the new value is not
     * an instance of the existing default type, then the default type will be changed
     * to the class of the new value as a side-effect.
     */
    public void setDefaultCellValue(Object value) {
        _defaultValue = value;
        if (!_defaultType.isInstance(value)) _defaultType = value.getClass();
    }

    /**
     * Sets the option that determines if empty cells and newly created cells are
     * editable.
     */
    public void setEditableByDefault(boolean b) {
        _model.setEditableByDefault(b);
    }

    /**
     * Returns true if empty and newly created cells are editable.
     */
    public boolean isEditableByDefault() {
        return _model.isEditableByDefault();
    }

    private boolean _strictType;

    /**
     * Returns true if cells strictly enforce types once a type has been set, or if
     * they allow their types to be changed once set.
     */
    public boolean isStrictlyTyped() {
        return _strictType;
    }

    /**
     * Sets the option that controls whether cells will be strongly or weakly typed.
     * Strongly typed cells will not change their types after being set: a new formula
     * must return the same type as the type that the cell has been assigned.  Weakly
     * typed cells can have their types changed at any time.  By default, cells are
     * weakly typed.
     */
    public void setStrictTyping(boolean b) {
        _strictType = b;
    }

    /**
     */
    public void readSpreadsheet(InputStream is) throws IOException {
        new Reader().readSpreadsheet(is);
    }

    /**
     */
    public void writeSpreadsheet(OutputStream os) throws IOException {
        new Writer().writeSpreadsheet(os);
    }

    /**
     * Returns the component used as a row header
     */
    public JComponent getRowHeader() {
        return _rowHeader;
    }

    /**
     * Sets the functor used by the spreadsheet to display status information.  This
     * allows the spreadsheet's container to (for example) route status information
     * from the spreadsheet to a log file, or to a status bar.
     */
    public void setStatusHandler(UnaryFunctor<String, ?> fn) {
        _statusFn = fn;
    }

    /**
     * Updates the spreadsheet's status message.
     */
    public void setStatus(String status) {
        _statusFn.fn(status);
    }

    private Generator<?> _updateFn;

    public void setUpdateHandler(Generator<?> fn) {
        _updateFn = fn;
    }

    private void fireSpreadsheetUpdated() {
        if (_updateFn != null) _updateFn.fn();
    }

    /**
     * Discards all information about the given cell.
     */
    public void clearCellAt(int row, int col) {
        Cell cell = getCellIfPresent(row, col);
        if (cell != null) {
            cell.clear();
        }
    }

    /**
     * Returns the cell at the given address, creating one if one does not already
     * exist.
     * @throws IndexOutOfBoundsException if the row or column is out of bounds
     */
    public Cell getCellAt(int row, int col) {
        Cell cell = getCellIfPresent(row, col);
        if (cell == null) {
            cell = _model.setCell(new Cell(this, row, col));
        }
        return cell;
    }

    /**
     * Returns the cell at the given address if one exists, or null if it does not
     * @throws IndexOutOfBoundsException if the row or column is out of bounds
     */
    Cell getCellIfPresent(int row, int col) {
        return _model.getCellAt(row, col);
    }

    /**
     * Builds a read-only cell to hold the given value.  If you need the cell
     * to be editable
     * @throws IndexOutOfBoundsException if the row or column is out of bounds
     */
    public <T> Cell setCellAt(Class<T> type, T value, int row, int col) {
        return _model.setCellAt(type, new Constant<T>(value), row, col);
    }

    /**
     * Builds a cell to hold the given value
     * @throws IndexOutOfBoundsException if the row or column is out of bounds
     */
    public <T> Cell setCellAt(Class<T> type, Generator<T> gen, int row, int col) {
        return _model.setCellAt(type, gen, row, col);
    }

    /**
     * Builds a possibly editable cell to hold the given formula.
     * @throws IndexOutOfBoundsException if the row or column is out of bounds
     */
    public Cell setCellAt(String formula, int row, int col) {
        return _model.setCellAt(formula, row, col, _model.isEditableByDefault());
    }

    /**
     * Builds a possibly editable cell to hold the given formula.
     * @throws IndexOutOfBoundsException if the row or column is out of bounds
     */
    public Cell setCellAt(String formula, int row, int col, boolean editable) {
        return _model.setCellAt(formula, row, col, editable);
    }

    /**
     * Replaces the CellRenderer for the given cell such that the given
     * formatting functor is used to present the contents of the cell.
     * NOTE: Use this method cautiously, as passing a formatter of an
     * inappropriate type can lead to a class cast exception or to the
     * Cell reporting '### CLASS ###' condition.
     */
    public <T> void setFormatAt(UnaryFunctor<T, String> formatter, int row, int col) {
        getCellAt(row, col).setFormat(formatter);
    }

    /**
     * Returns the cell with the given name, or null if no cell has the given name.
     */
    public Cell getCellByName(String name) {
        return _model.getCellByName(name);
    }

    /**
     * Sets the name of the given cell.
     * @throws IllegalArgumentException if the name is already in use.
     */
    public Cell setCellName(String name, int row, int col) {
        Cell cell = _model.setCellName(name, row, col);
        setStatus(cell.toString());
        return cell;
    }

    /**
     * Returns a reference to the contents of a given cell
     */
    public <T> Generator<T> getReference(Class<T> type, int row, int col) {
        return _model.getReference(type, row, col);
    }

    public void clear() {
        _model.clear();
        setRowSelectionInterval(0, 0);
        setColumnSelectionInterval(0, 0);
    }

    public TableCellEditor getCellEditor(int row, int col) {
        Cell cell = getCellIfPresent(row, col);
        if (cell == null) {
            return super.getCellEditor(row, col);
        }
        TableCellEditor editor = cell.getEditor();
        return (editor != null) ? editor : super.getCellEditor(row, col);
    }

    private TableCellRenderer _defaultRenderer = new Cell.Renderer<Object>();

    public TableCellRenderer getCellRenderer(int row, int col) {
        Cell cell = getCellIfPresent(row, col);
        if (cell == null) return _defaultRenderer;
        TableCellRenderer renderer = cell.getRenderer();
        return (renderer != null) ? renderer : _defaultRenderer;
    }

    public void setModel(TableModel model) {
        if (!_initialized) super.setModel(model); else if (model instanceof SpreadsheetTableModel) {
            super.setModel(model);
            SpreadsheetTableModel oldModel = _model;
            _model = (SpreadsheetTableModel) model;
            _rowHeader.setRowCount(_model.getRowCount());
            doSetColumnCount(oldModel.getColumnCount(), _model.getColumnCount());
        } else {
            String msg = "Spreadsheet requires SpreadsheetTableModel";
            throw new IllegalArgumentException(msg);
        }
    }

    /**
     * In addition to the JTable behaviour of this method (which takes care to add the
     * table's columnheader to an enclosing scrollpane's viewport), add a rowheader to
     * the enclosing scrollpane's viewport as well.
     */
    protected void configureEnclosingScrollPane() {
        super.configureEnclosingScrollPane();
        Container p = getParent();
        if (p instanceof JViewport) {
            Container gp = p.getParent();
            if (gp instanceof JScrollPane) {
                JScrollPane scrollPane = (JScrollPane) gp;
                JViewport viewport = scrollPane.getViewport();
                if (viewport == null || viewport.getView() != this) {
                    return;
                }
                scrollPane.setRowHeaderView(getRowHeader());
            }
        }
    }

    public void valueChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) showSelectionStatus();
        super.valueChanged(e);
    }

    public void columnSelectionChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) showSelectionStatus();
        super.columnSelectionChanged(e);
    }

    private int _lastRow = -1, _lastCol = -1;

    private void showSelectionStatus() {
        int row = getSelectedRow();
        int col = getSelectedColumn();
        if (row < 0 || col < 0) return;
        if (row == _lastRow && col == _lastCol) return;
        _lastRow = row;
        _lastCol = col;
        Cell cell = getCellIfPresent(row, col);
        if (cell != null) setStatus(cell.toString()); else setStatus("cell(" + row + "," + col + ")");
    }

    private class SpreadsheetTableModel extends AbstractTableModel implements Observer {

        static final long serialVersionUID = -6455541616661139146L;

        private Map<Point, Cell> _cellmap = new HashMap<Point, Cell>();

        private Map<String, Cell> _namemap = new HashMap<String, Cell>();

        private int _numRows;

        private int _numCols;

        /**
         * Builds a SpreadsheetTableModel of the default size (16x16)
         */
        public SpreadsheetTableModel() {
            this(16, 16);
        }

        /**
         * Builds a SpreadsheetTableModel of the given size
         */
        public SpreadsheetTableModel(int rows, int cols) {
            _numRows = rows;
            _numCols = cols;
        }

        /**
         * Removes all information from the model
         */
        public void clear() {
            _cellmap = new HashMap<Point, Cell>();
            _namemap = new HashMap<String, Cell>();
            fireTableDataChanged();
        }

        /**
         * Returns the cell at the given address, or null if no such cell
         * exists.
         * @throws IndexOutOfBoundsException if the row or column is out of
         * bounds
         */
        public Cell getCellAt(int row, int col) throws IndexOutOfBoundsException {
            if (checkCellAddress(row, col)) return _cellmap.get(new Point(row, col));
            if (row < 0 || row >= _numRows) {
                String msg = "Row " + row + " out of range: 0.." + (_numRows - 1);
                throw new IndexOutOfBoundsException(msg);
            }
            String msg = "Col " + col + " out of range: 0.." + (_numCols - 1);
            throw new IndexOutOfBoundsException(msg);
        }

        /**
         * Builds a read-only cell to hold the given value
         * @throws IndexOutOfBoundsException if the row or column is out of
         * bounds
         * @throws IllegalArgumentException if the cell has already been set
         */
        private <T> Cell setCellAt(Class<T> type, Generator<T> gen, int row, int col) {
            Cell cell = getCellAt(row, col);
            if (cell != null) {
                throw new IllegalArgumentException(cell + " has already been set");
            }
            if (gen == null) return null;
            return setCell(new Cell(Spreadsheet.this, type, new Point(row, col), gen));
        }

        /**
         * Builds a possibly editable cell to hold the given formula
         * @throws IndexOutOfBoundsException if the row or column is out of
         * bounds
         * @throws IllegalArgumentException if the cell has already been set
         */
        private Cell setCellAt(String formula, int row, int col, boolean editable) {
            Cell cell = getCellAt(row, col);
            if (cell != null) {
                throw new IllegalArgumentException(cell + " has already been set");
            }
            if (formula == null || "".equals(formula)) return null;
            return setCell(new Cell(Spreadsheet.this, new Point(row, col), formula, editable));
        }

        /**
         */
        private Cell setCell(Cell cell) {
            String name = cell.getName();
            if (name != null) {
                if (_namemap.get(name) != null) {
                    String err = "Duplicate cell name " + name;
                    throw new IllegalArgumentException(err);
                }
                _namemap.put(name, cell);
            }
            cell.addObserver(this);
            Point p = cell.getAddress();
            _cellmap.put(p, cell);
            fireTableCellUpdated(p.x, p.y);
            return cell;
        }

        /**
         * Returns a (possibly null) cell whose name is given
         */
        private Cell getCellByName(String name) {
            return _namemap.get(name);
        }

        /**
         * Sets the name of a cell
         */
        private Cell setCellName(String name, int row, int col) {
            if (name != null) {
                Cell cell = _namemap.get(name);
                if (cell != null) {
                    String err = "Duplicate cell name " + name;
                    throw new IllegalArgumentException(err);
                }
            }
            Cell cell = Spreadsheet.this.getCellAt(row, col);
            String oldname = cell.getName();
            if (oldname != null) _namemap.remove(oldname);
            if (name != null) _namemap.put(name, cell);
            cell.setName(name);
            return cell;
        }

        /**
         * Returns a reference to the contents of a given cell
         */
        @SuppressWarnings("unchecked")
        public <T> Generator<T> getReference(Class<T> type, int row, int col) {
            Cell cell = getCellAt(row, col);
            if (cell == null) {
                String msg = "Cell({0},{1}) is not yet defined";
                throw new IllegalArgumentException(MessageFormat.format(msg, new Object[] { row, col }));
            }
            if (type.isAssignableFrom(cell.getType())) {
                return (Generator<T>) cell.getReference();
            }
            String err = "Cannot return reference of type {0} from {1}, whose type is {2}";
            String msg = MessageFormat.format(err, new Object[] { type, cell, cell.getType() });
            throw new ClassCastException(msg);
        }

        private boolean _editableByDefault;

        private void setEditableByDefault(boolean b) {
            _editableByDefault = b;
        }

        private boolean isEditableByDefault() {
            return _editableByDefault;
        }

        public void setRowCount(int height) {
            int oldHeight = _numRows;
            _numRows = height;
            if (oldHeight < height) {
                fireTableRowsInserted(oldHeight, height - 1);
            } else {
                removeCells();
                fireTableRowsDeleted(height, oldHeight - 1);
            }
        }

        public void setColumnCount(int width) {
            int oldWidth = _numCols;
            _numCols = width;
            if (width < oldWidth) {
                removeCells();
            }
            fireTableStructureChanged();
        }

        private void removeCells() {
            for (Iterator<Cell> iter = _cellmap.values().iterator(); iter.hasNext(); ) {
                Cell cell = iter.next();
                Point p = cell.getAddress();
                if (p.x >= _numRows || p.y >= _numCols) {
                    iter.remove();
                    String name = cell.getName();
                    if (name != null) _namemap.remove(cell.getName());
                    cell.unlink();
                }
            }
        }

        public int getRowCount() {
            return _numRows;
        }

        public int getColumnCount() {
            return _numCols;
        }

        public Object getValueAt(int row, int col) {
            if (!checkCellAddress(row, col)) return Cell.REFERENCE_ERR;
            Cell cell = _cellmap.get(new Point(row, col));
            if (cell == null) return null;
            Object obj = cell.getValue();
            return cell.isUndefined() ? "" : cell.isValid() ? obj : cell.getErrorMsg();
        }

        public void setValueAt(Object value, int row, int col) {
            Cell cell = getCellAt(row, col);
            if (cell != null) {
                cell.setValue(value);
            } else if (value != null) {
                setCellAt(value.toString(), row, col, true);
            }
            fireSpreadsheetUpdated();
        }

        public boolean isCellEditable(int row, int col) {
            Cell cell = getCellAt(row, col);
            if (cell != null) return cell.isEditable();
            return _editableByDefault;
        }

        public void update(Observable observable, Object object) {
            Cell cell = (Cell) observable;
            Point addr = cell.getAddress();
            fireTableCellUpdated(addr.x, addr.y);
        }

        private boolean checkCellAddress(int row, int col) {
            return row >= 0 && row < _numRows && col >= 0 && col < _numCols;
        }
    }

    class Parser extends JFXGParser {

        private Cell _crntCell;

        private Parser() {
            super();
        }

        public Generator<?> parseGenerator(Cell cell, String str) throws ParseException {
            _crntCell = cell;
            try {
                return super.parseGenerator(str);
            } finally {
                _crntCell = null;
            }
        }

        /**
         * Suppresses the binding method: inside a spreadsheet, 'this' always refers to
         * the spreadsheet itself.
         */
        public void bindThis(Object thisBinding) {
            super.bindThis(Spreadsheet.this);
        }

        /**
         * Overrides the base parser class mechanism for resolving methods, in order to
         * protect certain methods from abuse.  
         */
        @SuppressWarnings("unchecked")
        protected FunctorRef<?, ?> resolveMethodName(FunctorRef<?, ?> prefix, String name, FunctorRef<?, ?>[] args) throws ParseException {
            if (!prefix.isConstant()) return super.resolveMethodName(prefix, name, args);
            if (((GeneratorRef) prefix).getFunctor().fn() != Spreadsheet.this) return super.resolveMethodName(prefix, name, args);
            return super.resolveMethodName(prefix, name, args);
        }

        /**
         * Implements the <i>row</i>, and <i>col</i> keywords.
         */
        protected FunctorRef<?, ?> reservedWord(String word) throws ParseException {
            if (word.equals("row")) {
                return new GeneratorRef<Integer>(new Constant<Integer>(_crntCell.getAddress().x), Integer.class);
            }
            if (word.equals("col")) {
                return new GeneratorRef<Integer>(new Constant<Integer>(_crntCell.getAddress().y), Integer.class);
            }
            return super.reservedWord(word);
        }

        /**
         * Implements the <i>cell</i>  keyword.
         */
        @SuppressWarnings("unchecked")
        protected FunctorRef<?, ?> reservedFunction(String name, FunctorRef<?, ?>[] args) throws ParseException {
            if (name.equals("cell")) {
                if (args.length == 1 && args[0] instanceof GeneratorRef && args[0].getReturnType().equals(String.class)) {
                    String refname = (String) ((GeneratorRef<?>) args[0]).getFunctor().fn();
                    Cell cell = getCellByName(refname);
                    if (cell == null) {
                        throw new ParseException("Unknown Cell Name: " + refname);
                    }
                    return new GeneratorRef(cell.getReference(), cell.getType());
                }
                if (args.length != 2 || !(args[0] instanceof GeneratorRef) || !(args[1] instanceof GeneratorRef) || !(args[0].getReturnType().equals(Integer.class)) || !(args[1].getReturnType().equals(Integer.class))) throw new ParseException("Cell Reference requires row, col arguments");
                int row = ((Integer) ((GeneratorRef) args[0]).getFunctor().fn()).intValue();
                int col = ((Integer) ((GeneratorRef) args[1]).getFunctor().fn()).intValue();
                Cell cell = getCellAt(row, col);
                return new GeneratorRef(cell.getReference(), cell.getType());
            }
            return super.reservedFunction(name, args);
        }
    }

    public class Writer {

        private TransformerHandler _handler;

        private Set<Cell> _cellsWritten;

        public Writer() throws IOException {
            try {
                SAXTransformerFactory tf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
                _handler = tf.newTransformerHandler();
                Transformer xformer = _handler.getTransformer();
                xformer.setOutputProperty(OutputKeys.ENCODING, "ISO-8859-1");
                xformer.setOutputProperty(OutputKeys.INDENT, "yes");
            } catch (TransformerConfigurationException x) {
                IOException iox = new IOException(x.getMessage());
                iox.initCause(x);
                throw iox;
            }
        }

        public void writeSpreadsheet(OutputStream os) throws IOException {
            try {
                StreamResult stream = new StreamResult(os);
                _handler.setResult(stream);
                _handler.startDocument();
                AttributesImpl atts = new AttributesImpl();
                int rows = getRowCount();
                int cols = getColumnCount();
                atts.clear();
                atts.addAttribute("", "", "vers", "", "0.1.0");
                atts.addAttribute("", "", "rows", "", String.valueOf(rows));
                atts.addAttribute("", "", "cols", "", String.valueOf(cols));
                Class<?> defaultType = getDefaultCellType();
                if (!defaultType.equals(Integer.class)) {
                    atts.addAttribute("", "", "defaultType", "", defaultType.getName());
                }
                Object defaultValue = getDefaultCellValue();
                if (!defaultValue.equals(0)) {
                    atts.addAttribute("", "", "defaultValue", "", getDefaultCellValue().toString());
                }
                _handler.startElement("", "", "hacksheet", atts);
                _cellsWritten = new HashSet<Cell>(_model._cellmap.values().size() * 4 / 3);
                writeCells(_model._cellmap.values().iterator());
                _handler.endElement("", "", "hacksheet");
                _handler.endDocument();
            } catch (SAXException x) {
                IOException iox = new IOException(x.getMessage());
                iox.initCause(x);
                throw iox;
            }
        }

        private void writeCells(Iterator<Cell> cells) throws SAXException {
            while (cells.hasNext()) {
                Cell cell = cells.next();
                if (!_cellsWritten.contains(cell)) {
                    writeCells(cell.dependsOn());
                    writeCell(cell);
                }
            }
        }

        private void writeCell(Cell cell) throws SAXException {
            AttributesImpl atts = new AttributesImpl();
            String name = cell.getName();
            if (name != null && name.trim().length() != 0) atts.addAttribute("", "", "id", "", cell.getName());
            Point p = cell.getAddress();
            atts.addAttribute("", "", "row", "", String.valueOf(p.x));
            atts.addAttribute("", "", "col", "", String.valueOf(p.y));
            atts.addAttribute("", "", "type", "", cell.getType().getName());
            atts.addAttribute("", "", "editable", "", String.valueOf(cell.isEditable()));
            String formula = cell.getFormula();
            _handler.startElement("", "", "cell", atts);
            atts.clear();
            _handler.startElement("", "", "formula", atts);
            _handler.characters(formula.toCharArray(), 0, formula.length());
            _handler.endElement("", "", "formula");
            _handler.endElement("", "", "cell");
            _cellsWritten.add(cell);
        }
    }

    public class Reader extends DefaultHandler {

        StringBuffer buf = new StringBuffer();

        public void readSpreadsheet(InputStream is) throws IOException {
            try {
                createParser().parse(new InputSource(is));
            } catch (SAXException x) {
                IOException iox = new IOException(x.getMessage());
                iox.initCause(x);
                throw iox;
            } catch (ParserConfigurationException x) {
                IOException iox = new IOException(x.getMessage());
                iox.initCause(x);
                throw iox;
            }
        }

        private Cell _crntCell;

        public void startElement(String nsURI, String localname, String qname, Attributes attr) throws SAXException {
            if (qname.equals("hacksheet")) {
                attr.getValue("vers");
                int rows = Integer.parseInt(attr.getValue("rows"));
                int cols = Integer.parseInt(attr.getValue("cols"));
                SpreadsheetTableModel model = new SpreadsheetTableModel(rows, cols);
                setModel(model);
            } else if (qname.equals("cell")) {
                int row = Integer.parseInt(attr.getValue("row"));
                int col = Integer.parseInt(attr.getValue("col"));
                _crntCell = new Cell(Spreadsheet.this, row, col);
                _crntCell.setEditable(Boolean.valueOf(attr.getValue("editable")));
                String name = attr.getValue("id");
                if (name != null) {
                    _crntCell.setName(name);
                }
            } else if (qname.equals("formula")) {
                buf.delete(0, buf.length());
            } else throw new SAXException("unknown tag \"" + qname + "\"");
        }

        public void endElement(String nsURI, String localname, String qname) throws SAXException {
            if (qname.equals("hacksheet")) {
            } else if (qname.equals("cell")) {
                _model.setCell(_crntCell);
            } else if (qname.equals("formula")) {
                _crntCell.setFormula(buf.toString());
                buf.delete(0, buf.length());
            } else throw new SAXException("unknown tag \"" + qname + "\"");
        }

        public void characters(char[] ch, int start, int ln) throws SAXException {
            for (int i = 0; i < ln; ++i) {
                buf.append(ch[start + i]);
            }
        }

        public XMLReader createParser() throws SAXException, ParserConfigurationException {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setValidating(false);
            SAXParser saxParser = spf.newSAXParser();
            XMLReader xmlrd = saxParser.getXMLReader();
            xmlrd.setContentHandler(this);
            xmlrd.setErrorHandler(this);
            return xmlrd;
        }
    }
}

class RowHeader extends JComponent {

    static final long serialVersionUID = -1375303876648436931L;

    private JTable _table;

    private TableCellRenderer _renderer;

    private JTableHeader _header;

    private CellRendererPane _rendererPane;

    private Font _headerFont;

    public RowHeader(JTable table) {
        _table = table;
        _header = table.getTableHeader();
        _renderer = _header.getDefaultRenderer();
        _rendererPane = new CellRendererPane();
        add(_rendererPane);
        Component rendererComponent = _renderer.getTableCellRendererComponent(_table, "0", false, false, 0, -1);
        _headerFont = rendererComponent.getFont();
        setFont(_headerFont);
        setBackground(rendererComponent.getBackground());
        setForeground(rendererComponent.getForeground());
    }

    public TableCellRenderer getRenderer() {
        return _renderer;
    }

    public void setRenderer(TableCellRenderer renderer) {
        _renderer = renderer;
    }

    public void setRowCount(int count) {
        setSize(getPreferredSize());
    }

    public Dimension getPreferredSize() {
        Border border = (Border) UIManager.getDefaults().get("TableHeader.cellBorder");
        Insets insets = border.getBorderInsets(_header);
        FontMetrics metrics = getFontMetrics(_headerFont);
        Dimension dim = new Dimension(metrics.stringWidth("99999") + insets.right + insets.left, _table.getRowHeight() * _table.getRowCount());
        return dim;
    }

    protected void paintComponent(Graphics g) {
        Rectangle cellRect = new Rectangle(0, 0, getWidth(), _table.getRowHeight(0));
        int rowMargin = _header.getColumnModel().getColumnMargin() - 1;
        for (int i = 0; i < _table.getRowCount(); ++i) {
            int rowHeight = _table.getRowHeight(i);
            cellRect.height = rowHeight - rowMargin;
            paintCell(g, cellRect, i);
            cellRect.y += rowHeight;
        }
    }

    private void paintCell(Graphics g, Rectangle cellRect, int rowIndex) {
        Component component = _renderer.getTableCellRendererComponent(_table, rowIndex, false, false, rowIndex, -1);
        _rendererPane.paintComponent(g, component, this, cellRect.x, cellRect.y, cellRect.width, cellRect.height, true);
    }
}
