package com.bix.util.blizfiles.dbc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import com.bix.util.blizfiles.BufferUtils;

/**
 * This class encapsulates the functionality of Blizzard's DBC (Database
 * Client) files.
 *  
 *	@author		squid
 *
 *	@version	1.0.0
 */
public class DBCFile<T extends DBCRecord> {

    /**
	 * This class represents the header of a DBC file.
	 * 
	 *	@author squid
	 *
	 *	@version	1.0.0
	 */
    public class DBCHeader {

        private byte[] magic = new byte[4];

        private int numRecords;

        private int numFieldsPerRecord;

        private int recordSize;

        private int stringBlockSize;

        /**
		 * Instantiate the header from a ByteBuffer.
		 * 
		 * @param bb	The ByteBuffer to instantiate the class from.
		 */
        public DBCHeader(ByteBuffer bb) {
            read(bb);
        }

        /**
		 * Read the DBC Header from a ByteBuffer.
		 * 
		 * @param bb	The buffer to read the header from.
		 */
        public void read(ByteBuffer bb) {
            this.magic = BufferUtils.getByteArray(bb, 4);
            if (new String(this.magic).equals("WDBC") == false) {
                throw new RuntimeException("Invalid DBC file.");
            }
            this.numRecords = bb.getInt();
            this.numFieldsPerRecord = bb.getInt();
            this.recordSize = bb.getInt();
            this.stringBlockSize = bb.getInt();
        }

        /**
		 * @return the magic
		 */
        public byte[] getMagic() {
            return magic;
        }

        /**
		 * @return the numRecords
		 */
        public int getNumRecords() {
            return numRecords;
        }

        /**
		 * @return the numFieldsPerRecord
		 */
        public int getNumFieldsPerRecord() {
            return numFieldsPerRecord;
        }

        /**
		 * @return the recordSize
		 */
        public int getRecordSize() {
            return recordSize;
        }

        /**
		 * @return the blockSize
		 */
        public int getStringBlockSize() {
            return stringBlockSize;
        }
    }

    private Class<T> recordClass;

    private Constructor<T> constructor;

    private DBCHeader header;

    private int stringBlockStart;

    private ByteBuffer data;

    public DBCFile(Class<T> c) throws SecurityException, NoSuchMethodException {
        this.recordClass = c;
        this.constructor = this.recordClass.getConstructor(DBCFile.class);
    }

    public DBCFile<? extends DBCRecord> initialize(String pathAndFilename) throws IOException {
        if (this.data == null) {
            File f = new File(pathAndFilename);
            FileInputStream fis = new FileInputStream(f);
            FileChannel fc = fis.getChannel();
            this.data = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            this.data.order(ByteOrder.LITTLE_ENDIAN);
            read(this.data);
        }
        return this;
    }

    protected void read(ByteBuffer bb) throws IOException {
        this.header = new DBCHeader(bb);
        this.stringBlockStart = this.header.numRecords * this.header.recordSize + 20;
    }

    /**
	 * @return the header
	 */
    public DBCHeader getHeader() {
        return header;
    }

    protected ByteBuffer getData() {
        return this.data;
    }

    /**
	 * Helper method to provide string reading functionality.  After reading the
	 * string, the buffer's position is reset to its original location.
	 * 
	 * @param stringIndex	The string index to read.
	 * 
	 * @return
	 */
    protected String getString(int stringIndex) {
        int position = this.data.position();
        this.data.position(stringBlockStart + stringIndex);
        String s = BufferUtils.getString(this.data);
        this.data.position(position);
        return s;
    }

    /**
	 * This is a helper method that seeks to a particular record number in the
	 * buffer.
	 * 
	 * @param recNum	Record number to seek to.
	 */
    protected void seekToRecord(int recNum) {
        this.data.position(recNum * this.header.recordSize + 20);
    }

    public void getRecord(int recNum, T t) {
        seekToRecord(recNum);
        t.read(this);
    }

    public T getRecord(int recNum) {
        seekToRecord(recNum);
        try {
            return (T) (this.constructor.newInstance(this));
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }
}
