package bill.util.csv;

import java.util.*;
import java.io.*;
import java.sql.*;

/**
 * Controls the reading of a CSV file. A CSV file is a comma delimited text
 * data file.
 */
public class CSVParser {

    /** Number of lines to skip before 'really' reading the file. This is to get
       past any lines that may be before the header. */
    protected int _skipLines;

    /** Name of the CSV file we are working with */
    protected String _fileName;

    /** A stream for writing out the CSV data */
    protected OutputStream _writer;

    /** Allows an already opened CSV file */
    protected Reader _reader;

    /** The list of lines read from the CSV file */
    protected Vector _csvLines = null;

    /** The first line of a csv file is a header line that lists what all the
       columns in the subsequent lines represent. */
    protected CSVLineParser _header = null;

    /** Indicates if the CSV has ben modified since it was read or last saved. */
    protected boolean _modified = false;

    /**
    * Main constructor. Opens up the CSV input file and processes its data
    * lines.
    *
    * @param fileName The name of the CSV input file.
    * @throws CSVException If the input file cannot be found, opened, or
    * processed.
    */
    public CSVParser(String fileName) throws CSVException {
        _csvLines = new Vector();
        _skipLines = 0;
        _fileName = fileName;
        parseCSV();
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
    public CSVParser(Reader reader) throws CSVException {
        _csvLines = new Vector();
        _skipLines = 0;
        _reader = reader;
        parseCSV();
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
    public CSVParser(String fileName, int skipLines) throws CSVException {
        _csvLines = new Vector();
        _skipLines = skipLines;
        _fileName = fileName;
        parseCSV();
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
    public CSVParser(Reader reader, int skipLines) throws CSVException {
        _csvLines = new Vector();
        _skipLines = skipLines;
        _reader = reader;
        parseCSV();
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
    public CSVParser(Reader reader, OutputStream writer) throws CSVException {
        this(reader, writer, 0);
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
    public CSVParser(Reader reader, OutputStream writer, int skipLines) throws CSVException {
        _csvLines = new Vector();
        _writer = writer;
        _skipLines = skipLines;
        _reader = reader;
        parseCSV();
    }

    /**
    * Alternate constructor for skipping lines. Reads specified CSV input stream
    * and processes its data lines.
    *
    * @param writer The output stream to write to when saving.
    */
    public CSVParser(OutputStream writer) {
        _csvLines = new Vector();
        _writer = writer;
    }

    /**
    * Constructor for creating a CSV parser using a SQL result set.
    * Note that when this constructor is used the <code>save</code>
    * method may not be used unless the <code>setFileName</code> method is
    * first called. This is because we currently only support saving to a file
    * and we cannot determine a file name from a result set.
    *
    * @see #save()
    * @see #setFileName(String)
    * @param resultSet The SQL result set to use in building the CSV
    * information.
    * @throws CSVException Thrown when the result set cannot be processed due
    * to a SQLException being thrown.
    */
    public CSVParser(ResultSet resultSet) throws CSVException {
        _csvLines = new Vector();
        try {
            ResultSetMetaData meta = resultSet.getMetaData();
            int colCount = meta.getColumnCount();
            _header = new CSVLineParserAssociated(this);
            Vector headerParts = new Vector(colCount);
            for (int i = 1; i <= colCount; i++) {
                String part = meta.getColumnName(i);
                headerParts.add(part);
            }
            _header.setLineParts(headerParts);
            while (resultSet.next()) {
                CSVLineParserAssociated line = new CSVLineParserAssociated(this);
                Vector lineParts = new Vector(colCount);
                for (int i = 1; i <= colCount; i++) {
                    Object part = resultSet.getObject(i);
                    if (part != null) {
                        lineParts.add(part.toString());
                    } else {
                        lineParts.add("");
                    }
                }
                line.setLineParts(lineParts);
                _csvLines.add(line);
            }
        } catch (SQLException sqlEx) {
            throw new CSVException(sqlEx.toString());
        }
    }

    /**
    * Retrieves the list of CSV lines for this parser.
    *
    * @return The list of lines contained in this CSV parser. Note that this
    * is the actual list, not a copy, so changes made to the contents of this
    * list are permanent.
    */
    public Vector getLines() {
        return _csvLines;
    }

    /**
    * Checks if the contents of the parser have been modified since they were
    * read or last saved (whichever is more recent).
    *
    * @return Returns <code>true</code> if the parser's contents have been
    * modified, otherwise returns <code>false</code>.
    */
    public boolean isModified() {
        return _modified;
    }

    /**
    * Sets the modified flag.
    *
    * @param modified Value to set the modified flag to.
    */
    public void setModified(boolean modified) {
        _modified = modified;
    }

    /**
    * Controls the actual reading of the CSV file.
    *
    * @throws CSVException If the input file cannot be found, opened, or
    * processed.
    */
    private void parseCSV() throws CSVException {
        String line = null;
        BufferedReader inFile = null;
        try {
            inFile = getBufferedReader();
        } catch (FileNotFoundException fnfEx) {
            throw new CSVException("Could not find file: " + _fileName + " => " + fnfEx.toString());
        }
        try {
            for (int i = 0; i < _skipLines; i++) {
                inFile.readLine();
            }
            line = inFile.readLine();
            _header = new CSVLineParserAssociated(line, this);
            do {
                line = inFile.readLine();
                if (line == null) continue;
                CSVLineParserAssociated parsed = new CSVLineParserAssociated(line, this);
                if (parsed.getNumberOfLineParts() != _header.getNumberOfLineParts()) {
                    int numHead = _header.getNumberOfLineParts();
                    int numParsed = parsed.getNumberOfLineParts();
                    for (int i = numParsed; i < numHead; i++) {
                        parsed.addLinePart(i, "");
                    }
                }
                _csvLines.addElement(parsed);
            } while (line != null);
            inFile.close();
        } catch (IOException ioEx) {
            throw new CSVException("Exception processing " + _fileName + ": " + ioEx.toString());
        }
    }

    /**
    * Returns a PrintStream derived either from the file name or the output
    * stream specified when creating this class instance.
    *
    * @throws FileNotFoundException If the input file cannot be found, opened,
    * or processed.
    */
    private PrintStream getPrintStream() throws FileNotFoundException, CSVException {
        if (_fileName != null) {
            return new PrintStream(new FileOutputStream(_fileName));
        } else if (_writer != null) {
            if (_writer instanceof PrintStream) {
                return (PrintStream) _writer;
            } else {
                return new PrintStream(_writer);
            }
        }
        throw new CSVException("No output file specified");
    }

    /**
    * Returns a BufferedReader derived either from the file name or the input
    * stream specified when creating this class instance.
    *
    * @throws FileNotFoundException If the input file cannot be found, opened,
    * or processed.
    */
    private BufferedReader getBufferedReader() throws FileNotFoundException {
        if (_fileName != null) {
            return new BufferedReader(new FileReader(_fileName));
        } else if (_reader != null) {
            if (_reader instanceof BufferedReader) {
                return (BufferedReader) _reader;
            } else {
                return new BufferedReader(_reader);
            }
        }
        return null;
    }

    /**
    * Writes the CSV data out to a file.
    *
    * @throws Exception Thrown when the save file could not be opened or a
    * file name is not defined.
    */
    public void save() throws Exception {
        PrintStream output = null;
        output = getPrintStream();
        if (hasHeader()) output.println(_header.getLine());
        for (int i = 0; i < getNumberOfLines(); i++) {
            output.println(getLine(i).getLine());
        }
        output.close();
        _modified = false;
    }

    /**
    * Retrieves the name of the file the CSV information is written to (and
    * possibly read from).
    *
    * @return The CSV file name.
    */
    public String getFileName() {
        return _fileName;
    }

    /**
    * Set the name of the file the CSV information is written to (and possibly
    * read from).
    *
    * @param fileName The name of the CSV file.
    */
    public void setFileName(String fileName) {
        _fileName = fileName;
    }

    /**
    * Return a specific data line's contents.
    *
    * @param lineNum The data line to retrieve. Uses a 0 based counter, so
    * the first data line is considered line 0.
    * @return The data specified line.
    */
    public CSVLineParser getLine(int lineNum) {
        return ((CSVLineParser) _csvLines.elementAt(lineNum));
    }

    /**
    * Retrieves a specific line part from the specified line.
    *
    * @param lineNum The data line to retrieve line part from. Uses a 0 based
    * counter, so the first data line is considered line 0.
    * @param partNum The index of the part to be retrieved, this is a 0
    * (zero) based value, so 0 = first part.
    */
    public String getLineLinePart(int lineNum, int partNum) {
        CSVLineParser line = ((CSVLineParser) _csvLines.elementAt(lineNum));
        return (String) line.getLinePart(partNum);
    }

    /**
    * Retrieves a specific line part from the specified line.
    *
    * @param lineNum The data line to retrieve line part from. Uses a 0 based
    * counter, so the first data line is considered line 0.
    * @param partName The name of the part to be retrieved, based on the part
    * names read from the header line.
    * @throws CSVException Thrown when the part name is not valid.
    */
    public String getLineLinePart(int lineNum, String partName) throws CSVException {
        CSVLineParser line = ((CSVLineParser) _csvLines.elementAt(lineNum));
        int partNum = getPartNumber(partName);
        return (String) line.getLinePart(partNum);
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
        CSVLineParser line = ((CSVLineParser) _csvLines.elementAt(lineNum));
        line.setLinePart(partNum, value);
        _modified = true;
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
        CSVLineParser line = ((CSVLineParser) _csvLines.elementAt(lineNum));
        int partNum = getPartNumber(partName);
        line.setLinePart(partNum, value);
        _modified = true;
    }

    /**
    * Determines the index of a specific line part name.
    *
    * @param partName The name of the part to be retrieved, based on the part
    * names read from the header line.
    * @throws CSVException Thrown when the part name is not valid.
    */
    public int getPartNumber(String partName) throws CSVException {
        int returnVal = _header.getLineParts().indexOf(partName);
        if (returnVal < 0) throw new CSVException("There is no line part named: " + partName);
        return (returnVal);
    }

    /**
    * Appends the specified line to the current list of lines.
    *
    * @param line The line parser information to add.
    */
    public void addLine(CSVLineParser line) {
        if (line instanceof CSVLineParserAssociated) {
            ((CSVLineParserAssociated) line).setParser(this);
            _csvLines.addElement(line);
        } else {
            _csvLines.addElement(new CSVLineParserAssociated(line, this));
        }
        _modified = true;
    }

    /**
    * Inserts the specified line at the specified index. Each line in this
    * object with an index greater or equal to the specified index is
    * shifted upward to have an index one greater than the value it had
    * previously.<br>
    * The index must be a value greater than or equal to 0 and less than or
    * equal to the current number of lines. (If the index is equal to the
    * current number of lines, the new line is appended to the existing lines.)
    *
    * @param line The line parser information to add.
    * @param lineNum The index to insert the new line at.
    */
    public void addLine(CSVLineParser line, int lineNum) {
        _csvLines.insertElementAt(line, lineNum);
        _modified = true;
    }

    /**
    * Deletes the line at the specified index. Each line in this object with an
    * index greater or equal to the specified index is shifted downward to have
    * an index one smaller than the value it had previously.<br>
    * The index must be a value greater than or equal to 0 and less than the
    * current number of lines.
    *
    * @param lineNum The index of the line to remove.
    */
    public void removeLine(int lineNum) {
        _csvLines.removeElementAt(lineNum);
        _modified = true;
    }

    /**
    * Retrieves the number of lines read from the CSV file.
    *
    * @return The number of lines read from the CSV file.
    */
    public int getNumberOfLines() {
        return (_csvLines.size());
    }

    /**
    * Retrieves the header line for this CSV file. The header line defines
    * what all the columns in the data lines represent. It can also be thought
    * of as a table of contents.
    *
    * @return The CSV file's header line.
    */
    public CSVLineParser getHeader() {
        return _header;
    }

    /**
    * Sets the header line for this CSV file. The header line defines
    * what all the columns in the data lines represent. It can also be thought
    * of as a table of contents.
    *
    * @param header The CSV file's new header line.
    */
    public void setHeader(CSVLineParser header) {
        _header = header;
    }

    /**
    * Test for the presence of a header for this CSV file.
    *
    * @return Returns <code>true</code> if a header is set
    */
    public boolean hasHeader() {
        return (_header != null);
    }

    /**
    * Swaps the positions of the specified lines.
    *
    * @param first The line number (0 based) of the first line to swap.
    * @param second The line number (0 based) of the second line to swap.
    */
    public void swapLines(int first, int second) {
        int lower = first;
        int higher = second;
        if (first > second) {
            lower = second;
            higher = first;
        }
        CSVLineParser higherLine = getLine(higher);
        CSVLineParser lowerLine = getLine(lower);
        _csvLines.remove(higher);
        _csvLines.remove(lower);
        _csvLines.insertElementAt(higherLine, lower);
        _csvLines.insertElementAt(lowerLine, higher);
        _modified = true;
    }

    public static void main(String argv[]) {
        if (argv.length < 1) {
            System.out.println("Please enter a CSV file name");
            System.exit(0);
        }
        try {
            CSVParser parser = new CSVParser(argv[0]);
            System.out.println("Files statistics");
            System.out.println("Number of entries: " + parser.getNumberOfLines());
            for (int i = 0; i < parser.getNumberOfLines(); i++) {
                CSVLineParser line = parser.getLine(i);
                System.out.println("Line " + i + " has " + line.getNumberOfLineParts() + " parts");
            }
        } catch (CSVException csvEx) {
            System.out.println("Could not open CSV file '" + argv[0] + "': " + csvEx.toString());
        }
    }
}
