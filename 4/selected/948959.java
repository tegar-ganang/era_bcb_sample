package org.eugenes.lusearch.fileIO;

import java.io.*;
import java.util.Arrays;
import org.eugenes.lusearch.settings.*;
import org.eugenes.lusearch.sort.*;
import org.eugenes.lusearch.util.ArgosUtils;

/**
 * This class is a specialized form of ArgosFile that allows a sort on the file by column.  
 * This class expects that the file is formatted using the ArgosDefaults delimiters, but
 * there is currently nothing in place to enforce this.  So... 
 * <br>
 * TODO: Add capability to ensure a properly formatted file...
 * <p>
 * The format should follow these guidelines:
 * <br>1. All field data are delimited by ArgosDefaults.DATA_SEPARATOR
 * <br>2. Field name and its value is delimited by ArgosDefaults.FIELD_DELIM
 * <br>3. The last field must be an int that represents the original index number, which
 *              is the number that refers to the line where the data can be located in the ArgosDataFile.
 * <br>example: SYM|At1g62580\tNAM|flavin-containing monooxygenase (FMO) family\t5
 * 
 * TODO: Efficiency is hurt here, especially with larger files.  
 * We need to address the ArgosFieldData[] situation.  The field
 * data is acquired from the file everytime a sort is called, but
 * if we hold the data in memory, then we need to use a map to 
 * track the fields we have data for and we could end up with 
 * a lot of memory being used (if data is held for multiple fields).
 * 
 * 
 *  @author Paul Poole
 */
public class ArgosIndexFile extends ArgosFile {

    /**
	 * Constructor for ArgosIndexFile.
	 * @param filename The filename to store the tab-delimited data in.
	 */
    public ArgosIndexFile(String filename) throws IOException {
        super(filename);
    }

    /**
	 * Constructor for ArgosIndexFile.
	 * @param prefix            Prefix to be used for determining filename.
	 * @param suffix            Suffix to be used for determining filename.
	 * @param directory       Directory to create the file in.
	 */
    public ArgosIndexFile(String prefix, String suffix, File directory) throws IOException {
        super(prefix, suffix, directory);
    }

    /**
	 * Sorts the field requested in the index file and returns an 
	 *   ArgosFieldData array containing the field, its value, and the
	 *   original index (which will correspond to the index in
	 *   the data file).
	 * @param field String representation of the field to be sorted.
	 * @param ascending True for an ascending sort and false for a 
	 * 				    descending sort.
	 * @return an array of ArgosFieldData objects in the sorted order that
	 *                 can be used for mapping the new order to the original order.
	 */
    public ArgosFieldData[] sort(String field, boolean ascending) throws IOException {
        ArgosFieldData[] fieldData = getFieldData(field);
        Arrays.sort(fieldData, new ArgosCompareFieldData(ascending));
        return fieldData;
    }

    private ArgosFieldData[] getFieldData(String field) throws IOException {
        String fileData = this.getFileData();
        String[] lines = fileData.split(ArgosDefaults.NEWLINE);
        ArgosFieldData[] fd = new ArgosFieldData[lines.length];
        field = field.trim();
        int fieldCol = getFieldColumn(field, lines[0]);
        for (int i = 0; i < lines.length; i++) {
            String[] tokens = this.getFieldsFromLine(lines[i]);
            fd[i] = new ArgosFieldData(field, this.getData(tokens[fieldCol]), Integer.parseInt(tokens[tokens.length - 1]));
        }
        return fd;
    }

    private String getData(String fieldData) {
        String value = fieldData.substring(fieldData.indexOf(ArgosDefaults.FIELD_DELIM) + 1);
        value = ArgosUtils.controlUnescape(value, ArgosDefaults.FIELD_DELIM);
        return value;
    }

    private int getFieldColumn(String field, String line) throws IOException {
        String[] fields = this.getFieldsFromLine(line);
        for (int i = 0; i < fields.length; i++) if (fields[i].startsWith(field)) return i;
        throw new IOException("Requested field " + field + " is not present in index field.");
    }

    private String[] getFieldsFromLine(String line) {
        return line.split("[" + ArgosDefaults.DATA_SEPARATOR + "]");
    }

    private String getFileData() throws IOException {
        super.openFile("r");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        while (in.available() != 0) bos.write(in.readByte());
        super.closeFile();
        return bos.toString();
    }
}
