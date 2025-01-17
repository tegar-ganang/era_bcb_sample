package com.hardcode.gdbms.driver.dbf_Fernando;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import com.hardcode.gdbms.driver.exceptions.FileNotFoundDriverException;

/**
 * Class to read and write data to a dbase III format file. Creation date:
 * (5/15/2001 5:15:13 PM)
 */
public class DbaseFile {

    private DbaseFileHeader myHeader;

    private FileInputStream fin;

    private FileChannel channel;

    private ByteBuffer buffer;

    public int getRecordCount() {
        return myHeader.getNumRecords();
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public int getFieldCount() {
        return myHeader.getNumFields();
    }

    /**
     * DOCUMENT ME!
     *
     * @param rowIndex DOCUMENT ME!
     * @param fieldId DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public boolean getBooleanFieldValue(int rowIndex, int fieldId) {
        int recordOffset = (myHeader.getRecordLength() * rowIndex) + myHeader.getHeaderLength() + 1;
        int fieldOffset = 0;
        for (int i = 0; i < (fieldId - 1); i++) {
            fieldOffset += myHeader.getFieldLength(i);
        }
        buffer.position(recordOffset + fieldOffset);
        char bool = (char) buffer.get();
        return ((bool == 't') || (bool == 'T') || (bool == 'Y') || (bool == 'y'));
    }

    /**
     * DOCUMENT ME!
     *
     * @param rowIndex DOCUMENT ME!
     * @param fieldId DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public String getStringFieldValue(int rowIndex, int fieldId) {
        int recordOffset = (myHeader.getRecordLength() * rowIndex) + myHeader.getHeaderLength() + 1;
        int fieldOffset = 0;
        for (int i = 0; i < fieldId; i++) {
            fieldOffset += myHeader.getFieldLength(i);
        }
        buffer.position(recordOffset + fieldOffset);
        byte[] data = new byte[myHeader.getFieldLength(fieldId)];
        buffer.get(data);
        return new String(data);
    }

    /**
     * Retrieve the name of the given column.
     *
     * @param inIndex DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public String getFieldName(int inIndex) {
        return myHeader.getFieldName(inIndex).trim();
    }

    /**
     * Retrieve the type of the given column.
     *
     * @param inIndex DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public char getFieldType(int inIndex) {
        return myHeader.getFieldType(inIndex);
    }

    /**
     * Retrieve the length of the given column.
     *
     * @param inIndex DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public int getFieldLength(int inIndex) {
        return myHeader.getFieldLength(inIndex);
    }

    /**
     * Retrieve the location of the decimal point.
     *
     * @param inIndex DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public int getFieldDecimalLength(int inIndex) {
        return myHeader.getFieldDecimalCount(inIndex);
    }

    /**
     * read the DBF file into memory.
     *
     * @param file DOCUMENT ME!
     *
     * @throws IOException DOCUMENT ME!
     */
    public void open(File file) throws FileNotFoundDriverException {
        try {
            fin = new FileInputStream(file);
            channel = fin.getChannel();
            buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
            myHeader = new DbaseFileHeader();
            myHeader.readHeader(buffer);
        } catch (IOException e) {
            throw new FileNotFoundDriverException(file.getName(), e, file.getAbsolutePath());
        }
    }

    /**
     * Removes all data from the dataset
     *
     * @throws IOException DOCUMENT ME!
     */
    public void close() throws IOException {
        fin.close();
        channel.close();
        buffer = null;
    }
}
