package csheets.core;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import csheets.core.formula.compiler.FormulaCompilationException;
import csheets.ext.Extension;
import csheets.ext.ExtensionManager;
import csheets.ext.SpreadsheetExtension;

/**
 * The implementation of the <code>Spreadsheet</code> interface.
 * @author Einar Pehrson
 */
public class SpreadsheetImpl implements Spreadsheet {

    /** The unique version identifier used for serialization */
    private static final long serialVersionUID = 7010464744129096272L;

    /** The base of the titles of new spreadsheets */
    public static final String BASE_TITLE = "Sheet ";

    /** The workbook to which the spreadsheet belongs */
    private Workbook workbook;

    /** The cells that have been instantiated */
    private Map<Address, Cell> cells = new HashMap<Address, Cell>();

    /** The title of the spreadsheet */
    private String title;

    /** The number of columns in the spreadsheet */
    private int columns = 0;

    /** The number of rows in the spreadsheet */
    private int rows = 0;

    /** The cell listeners that have been registered on the cell */
    private transient List<CellListener> cellListeners = new ArrayList<CellListener>();

    /** The cell listener that forwards events from all cells */
    private transient CellListener eventForwarder = new EventForwarder();

    /** The spreadsheet extensions that have been instantiated */
    private transient Map<String, SpreadsheetExtension> extensions = new HashMap<String, SpreadsheetExtension>();

    /**
	 * Creates a new spreadsheet.
	 * @param workbook the workbook to which the spreadsheet belongs
	 * @param title the title of the spreadsheet
	 */
    SpreadsheetImpl(Workbook workbook, String title) {
        this.workbook = workbook;
        this.title = title;
    }

    /**
	 * Creates a new spreadsheet, in which cells are initialized with data from
	 * the given content matrix.
	 * @param workbook the workbook to which the spreadsheet belongs
	 * @param title the title of the spreadsheet
	 * @param content the contents of the cells in the spreadsheet
	 */
    SpreadsheetImpl(Workbook workbook, String title, String[][] content) {
        this(workbook, title);
        rows = content.length;
        for (int row = 0; row < content.length; row++) {
            int columns = content[row].length;
            if (this.columns < columns) this.columns = columns;
            for (int column = 0; column < columns; column++) {
                try {
                    Address address = new Address(column, row);
                    Cell cell = new CellImpl(this, address, content[row][column]);
                    cell.addCellListener(eventForwarder);
                    cells.put(address, cell);
                } catch (FormulaCompilationException e) {
                }
            }
        }
    }

    public Workbook getWorkbook() {
        return workbook;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getColumnCount() {
        return columns;
    }

    public int getRowCount() {
        return rows;
    }

    public Cell getCell(Address address) {
        if (address.getRow() > rows) rows = address.getRow();
        if (address.getColumn() > columns) columns = address.getColumn();
        Cell cell = cells.get(address);
        if (cell == null) {
            cell = new CellImpl(this, address);
            cell.addCellListener(eventForwarder);
            cells.put(address, cell);
        }
        return cell;
    }

    public Cell getCell(int column, int row) {
        return getCell(new Address(column, row));
    }

    public SortedSet<Cell> getCells(Address address1, Address address2) {
        if (address1.compareTo(address2) > 0) {
            Address tempAddress = address1;
            address1 = address2;
            address2 = tempAddress;
        }
        SortedSet<Cell> cells = new TreeSet<Cell>();
        for (int column = address1.getColumn(); column <= address2.getColumn(); column++) for (int row = address1.getRow(); row <= address2.getRow(); row++) cells.add(getCell(new Address(column, row)));
        return cells;
    }

    public Cell[] getColumn(int index) {
        Cell[] column = new Cell[rows];
        for (int row = 0; row < row; row++) column[row] = getCell(new Address(index, row));
        return column;
    }

    public Cell[] getRow(int index) {
        Cell[] row = new Cell[columns];
        for (int column = 0; column < columns; column++) row[column] = getCell(new Address(column, index));
        return row;
    }

    public Iterator<Cell> iterator() {
        return cells.values().iterator();
    }

    public void addCellListener(CellListener listener) {
        cellListeners.add(listener);
    }

    public void removeCellListener(CellListener listener) {
        cellListeners.remove(listener);
    }

    public CellListener[] getCellListeners() {
        return cellListeners.toArray(new CellListener[cellListeners.size()]);
    }

    /**
	 * A cell listener that forwards events from all cells to registered listeners.
	 */
    private class EventForwarder implements CellListener {

        /**
		 * Creates a new event forwarder.
		 */
        public EventForwarder() {
        }

        public void valueChanged(Cell cell) {
            for (CellListener listener : cellListeners) listener.valueChanged(cell);
        }

        public void contentChanged(Cell cell) {
            for (CellListener listener : cellListeners) listener.contentChanged(cell);
        }

        public void dependentsChanged(Cell cell) {
            for (CellListener listener : cellListeners) listener.dependentsChanged(cell);
        }

        public void cellCleared(Cell cell) {
            for (CellListener listener : cellListeners) listener.cellCleared(cell);
        }

        public void cellCopied(Cell cell, Cell source) {
            for (CellListener listener : cellListeners) listener.cellCopied(cell, source);
        }
    }

    public Spreadsheet getExtension(String name) {
        SpreadsheetExtension extension = extensions.get(name);
        if (extension == null) {
            Extension x = ExtensionManager.getInstance().getExtension(name);
            if (x != null) {
                extension = x.extend(this);
                if (extension != null) extensions.put(name, extension);
            }
        }
        return extension;
    }

    /**
	 * Customizes deserialization by catching exceptions when extensions
	 * are not found.
	 * @param stream the object input stream from which the object is to be read
	 * @throws IOException If any of the usual Input/Output related exceptions occur
	 * @throws ClassNotFoundException If the class of a serialized object cannot be found.
	 */
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        eventForwarder = new EventForwarder();
        for (Cell cell : cells.values()) cell.addCellListener(eventForwarder);
        cellListeners = new ArrayList<CellListener>();
        extensions = new HashMap<String, SpreadsheetExtension>();
        int extCount = stream.readInt();
        for (int i = 0; i < extCount; i++) {
            try {
                SpreadsheetExtension extension = (SpreadsheetExtension) stream.readObject();
                extensions.put(extension.getName(), extension);
            } catch (ClassNotFoundException e) {
                System.err.println(e);
            }
        }
    }

    /**
	 * Customizes serialization, by writing extensions separately.
	 * @param stream the object output stream to which the object is to be written
	 * @throws IOException If any of the usual Input/Output related exceptions occur
	 */
    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        stream.writeInt(extensions.size());
        for (SpreadsheetExtension extension : extensions.values()) stream.writeObject(extension);
    }
}
