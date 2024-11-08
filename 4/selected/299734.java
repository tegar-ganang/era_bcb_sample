package org.melati.poem;

import java.util.Vector;
import java.io.File;
import java.io.Writer;
import java.io.IOException;
import org.melati.poem.csv.CSVTable;
import org.melati.poem.csv.CSVParseException;
import org.melati.poem.csv.CSVWriteDownException;
import org.melati.poem.csv.NoPrimaryKeyInCSVTableException;

/**
 * A class to define a sequence of {@link CSVTable}s and  process them 
 * by parsing the files and writing the data to the database.
 *
 * @author MylesC@paneris.org
 *
 */
public class CSVFilesProcessor {

    protected Vector tables = new Vector();

    Database db = null;

    /**
   * Constructor.
   * 
   * @param db the target database
   */
    public CSVFilesProcessor(Database db) {
        this.db = db;
    }

    /**
   * Convenience method.
   *  
   * @param tablename the name of a POEM table
   * @param file a CSV file, with first line containing field names
   * @return a new CSVTable
   */
    public CSVTable addTable(String tablename, File file) {
        return addTable(db.getTable(tablename), file);
    }

    /**
   * Add a table to this processor.
   * 
   * @param tab a POEM table
   * @param file a CSV file, with first line containing field names
   * @return a new CSVTable
   */
    public CSVTable addTable(Table tab, File file) {
        CSVTable table = new CSVTable(tab, file);
        tables.addElement(table);
        return table;
    }

    /**
   * Load all the data from the files, empty the tables if
   * necessary and then write the new data into the tables.
   * <p>
   * Write a report of the progress to the Writer.
   *
   * @param emptyTables flag whether to remove remains from last run
   * @param recordDetails flag passed in to table.report 
   * @param fieldDetails flag passed in to table.report
   * @param output tio write report to
   * @throws IOException if file stuff goes wrong
   * @throws CSVParseException if csv file has an error
   * @throws NoPrimaryKeyInCSVTableException not thrown
   * @throws CSVWriteDownException thrown when a persistent cannot be created
   */
    public void process(boolean emptyTables, boolean recordDetails, boolean fieldDetails, Writer output) throws IOException, CSVParseException, NoPrimaryKeyInCSVTableException, CSVWriteDownException {
        for (int i = 0; i < tables.size(); i++) ((CSVTable) tables.elementAt(i)).load();
        output.write("Loaded files\n");
        output.write("Trying to get exclusive lock on the database\n");
        db.beginExclusiveLock();
        output.write("Got exclusive lock on the database!!!\n");
        if (emptyTables) {
            for (int i = 0; i < tables.size(); i++) ((CSVTable) tables.elementAt(i)).emptyTable();
            PoemThread.writeDown();
        }
        output.write("Emptied all tables\n");
        System.err.println("Emptied all tables");
        writeData(output);
        output.write("Written records\n");
        db.endExclusiveLock();
        output.write("Ended exclusive lock on the database!!!\n");
        output.write("***** REPORT ******\n");
        for (int i = 0; i < tables.size(); i++) ((CSVTable) tables.elementAt(i)).report(recordDetails, fieldDetails, output);
    }

    /**
   * @throws NoPrimaryKeyInCSVTableException
   */
    protected void writeData(Writer o) throws NoPrimaryKeyInCSVTableException, CSVWriteDownException {
        for (int i = 0; i < tables.size(); i++) ((CSVTable) tables.elementAt(i)).writeRecords();
    }
}
