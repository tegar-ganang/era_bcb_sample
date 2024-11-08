package bill.util.csv;

import java.util.*;
import java.io.*;

/**
 * Controls the reading of a CSV file. A CSV file is a comma delimited text
 * data file. This class extends the standard CSV parser and implements
 * sorting of the CSV data. The user must specify the sort columns using the
 * setSortOrder method to enable the sorting functionality.
 */
public class CSVParserSorted extends CSVParser {

    private int[] _sortOrder = {};

    private boolean _reverse = false;

    /**
    * Main constructor. Opens up the CSV input file and processes its data
    * lines.
    *
    * @param fileName The name of the CSV input file.
    * @throws CSVException If the input file cannot be found, opened, or
    * processed.
    */
    public CSVParserSorted(String fileName) throws CSVException {
        super(fileName);
    }

    /**
    * Main constructor. Reads specified CSV input stream and processes its data
    * lines. Note that when this constructor is used the <code>save</code>
    * method may not be used unless the <code>setFileName</code> method is first
    * called. This is because we cannot determine a file name from just the
    * stream.
    *
    * @see #save()
    * @see #setFileName(String)
    * @param reader The CSV input stream.
    * @throws CSVException If the input file cannot be found, opened, or
    * processed.
    */
    public CSVParserSorted(Reader reader) throws CSVException {
        super(reader);
    }

    /**
    * Alternate constructor for skipping lines. Opens up the CSV input file and
    * processes its data lines. Use this constructor when there are a set number
    * of lines of data before the CSV header line. The extra lines are
    * ignored.
    *
    * @param fileName The name of the CSV input file.
    * @param skipLines Number of lines to skip before 'really' reading the file.
    * This is to get past any lines that may be before the header.
    * @throws CSVException If the input file cannot be found, opened, or
    * processed.
    */
    public CSVParserSorted(String fileName, int skipLines) throws CSVException {
        super(fileName, skipLines);
    }

    /**
    * Alternate constructor for skipping lines. Reads specified CSV input stream
    * and processes its data lines. Note that when this constructor is used the
    * <code>save</code> method may not be used unless the <code>setFileName
    * </code> method is first called. This is because we cannot determine a
    * file name from just the stream.
    *
    * @see #save()
    * @see #setFileName(String)
    * @param reader The CSV input stream.
    * @param skipLines Number of lines to skip before 'really' reading the file.
    * This is to get past any lines that may be before the header.
    * @throws CSVException If the input file cannot be found, opened, or
    * processed.
    */
    public CSVParserSorted(Reader reader, int skipLines) throws CSVException {
        super(reader, skipLines);
    }

    /**
    * Alternate constructor. Reads specified CSV input stream and processes its
    * data lines.
    *
    * @param reader The CSV input stream.
    * @param writer The output stream to write to when saving.
    * @throws CSVException If the input file cannot be found, opened, or
    * processed.
    */
    public CSVParserSorted(Reader reader, OutputStream writer) throws CSVException {
        super(reader, writer);
    }

    /**
    * Alternate constructor for skipping lines. Reads specified CSV input stream
    * and processes its data lines.
    *
    * @param reader The CSV input stream.
    * @param writer The output stream to write to when saving.
    * @param skipLines Number of lines to skip before 'really' reading the file.
    * This is to get past any lines that may be before the header.
    * @throws CSVException If the input file cannot be found, opened, or
    * processed.
    */
    public CSVParserSorted(Reader reader, OutputStream writer, int skipLines) throws CSVException {
        super(reader, writer, skipLines);
    }

    /**
    * Alternate constructor for skipping lines. Reads specified CSV input stream
    * and processes its data lines.
    *
    * @param writer The output stream to write to when saving.
    */
    public CSVParserSorted(OutputStream writer) {
        super(writer);
    }

    /**
    * Sets the reverse sort flag. When set to <code>true</code> this causes all
    * the sort order columns to be sorted in reverse order rather than regular
    * order.
    *
    * @param reverse Value to set the reverse flag to.
    */
    public void setReverse(boolean reverse) {
        _reverse = reverse;
    }

    /**
    * Sets the sort order array. This is an array of integers representing the
    * column numbers to sort on. The first array element is the primary sort
    * column, the second is the secondary, etc. Column numbers are 0 based.
    *
    * @param order The sort order array.
    */
    public void setSortOrder(int[] order) {
        _sortOrder = order;
    }

    /**
    * Sorts the list of CSV lines using the previously defined sort order.
    */
    public void sort() {
        if (_sortOrder.length == 0) return;
        int numLines = getNumberOfLines();
        if (numLines == 0) return;
        for (int i = 0; i < numLines; i++) {
            boolean swappedOne = false;
            CSVLineParser prev = getLine(numLines - 1);
            for (int j = numLines - 2; j >= i; j--) {
                CSVLineParser curr = getLine(j);
                int where = firstBeforeSecond(curr, prev);
                if ((where < 0) && (!_reverse)) {
                    swapLines(j, j + 1);
                    swappedOne = true;
                    _modified = true;
                } else if ((where > 0) && (_reverse)) {
                    swapLines(j, j + 1);
                    swappedOne = true;
                    _modified = true;
                } else {
                    prev = curr;
                }
            }
            if (!swappedOne) break;
        }
    }

    /**
    * Using the sort order, checks if the first CSV line should be positioned
    * before the second one.
    *
    * @param first The first CSV line to check the positioning of.
    * @param second The second CSV line to check the positioning of.
    * @return Returns <code>-1</code> if the first line should be positioned
    * before the second one, <code>1</code> if the second goes before the
    * first, and <code>0</code> if they are the same.
    */
    private int firstBeforeSecond(CSVLineParser first, CSVLineParser second) {
        for (int i = 0; i < _sortOrder.length; i++) {
            String firstField = first.getLinePart(_sortOrder[i]);
            String secondField = second.getLinePart(_sortOrder[i]);
            int compare = firstField.compareTo(secondField);
            if (compare < 0) return compare; else if (compare > 0) return compare;
        }
        return 0;
    }

    /**
    * Sets the specified line part from the specified line.
    *
    * @param lineNum The data line to retrieve line part from. Uses a 0 based
    * counter, so the first data line is considered line 0.
    * @param partName The name of the part to be retrieved, based on the part
    * names read from the header line.
    */
    public void setLineLinePart(int lineNum, int partNum, String value) {
        super.setLineLinePart(lineNum, partNum, value);
        sort();
    }

    /**
    * Sets the specified line part from the specified line.
    *
    * @param lineNum The data line to retrieve line part from. Uses a 0 based
    * counter, so the first data line is considered line 0.
    * @param partName The name of the part to be retrieved, based on the part
    * names read from the header line.
    * @throws CSVException Thrown when the part name is not valid.
    */
    public void setLineLinePart(int lineNum, String partName, String value) throws CSVException {
        super.setLineLinePart(lineNum, partName, value);
        sort();
    }

    /**
    * Appends the specified line to the current list of lines.
    *
    * @param line The line parser information to add.
    */
    public void addLine(CSVLineParser line) {
        super.addLine(line);
        sort();
    }
}
