package com.google.code.ar;

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

/**
 * AR format was not designed for streaming data, so it is big overhead keeping file bytes in memory. 
 * In comparison to <code>com.google.code.ar.ArOutputStream</code> this implementation uses java.io.RandomAccessFile to write data. ArEntry.data is not used.
 * <p>NOTE: that the long file names which are longer than 16 bytes will be truncated to the first 16.</p>
 * Normal scenario: 
 * 
 * <p><blockquote><pre>
 * ArEntry[] entries = ...
 * ArFileOutputStream afo = null;
 * try {
 *      afo = new ArFileOutputStream("archive.a");
 *      for( int i=0;i&lt;entries.lenght;i++) {
 *      	afo.putNextEntry(entries[i]);
 *      	//write data using afo.write();
 *      	afo.closeEntry();
 *      }
 * } catch(Exception e) {
 *  //do logging. handle exception
 * } finally {
 *      if( afo != null ) {
 *          try {
 *              afo.close();
 *          } catch(IOException e) {
 *              //do logging
 *          }
 *      }
 * }
 * </pre></blockquote></p>
 * 
 * @see com.google.code.ar.ArFileOutputStreamTest
 * @author dernasherbrezon
 *
 */
public class ArFileOutputStream extends OutputStream {

    private final RandomAccessFile raf;

    private boolean isClosed = false;

    private boolean isHeaderPresent = false;

    private ArEntry curEntry = null;

    private long curFileLenghtPointer;

    private long curWroteBytes = 0;

    public ArFileOutputStream(String fileName) throws IOException {
        this.raf = new RandomAccessFile(fileName, "rw");
    }

    /**
	 * Closes previous entry if it wasnt closed and starts new entry. File size will be filled later on closeEntry()
	 * @param entry
	 * @throws IOException if stream has been closed or unable to write to file.
     * @throws IllegalArgumentException if provided entry contains invalid data.
	 */
    public void putNextEntry(ArEntry entry) throws IOException {
        if (isClosed) {
            throw new IOException("stream closed");
        }
        if (curEntry != null) {
            closeEntry();
        }
        ArEntryValidator.validate(entry);
        if (!isHeaderPresent) {
            raf.write(ArOutputStream.HEADER);
            isHeaderPresent = true;
        }
        if (entry == null) {
            return;
        }
        curEntry = entry;
        String time = ArOutputStream.getCurTime();
        if (curEntry.getFilename().length() > 16) {
            write(curEntry.getFilename().substring(0, 15), 16);
        } else {
            write(curEntry.getFilename(), 16);
        }
        write(time, 12);
        write(String.valueOf(curEntry.getOwnerId()), 6);
        write(String.valueOf(curEntry.getGroupId()), 6);
        write(String.valueOf(curEntry.getFileMode()), 8);
        curFileLenghtPointer = raf.getFilePointer();
        write("0", 10);
        raf.write(ArOutputStream.MAGIC);
    }

    private void write(String str, int maxLenght) throws IOException {
        byte[] data = str.getBytes(ArOutputStream.ASCII);
        if (data.length > maxLenght) {
            throw new IOException("invalid string data");
        }
        raf.write(data);
        for (int i = data.length; i < maxLenght; i++) {
            raf.write((byte) 32);
        }
    }

    /**
     * close current entry and fill file size
     * @throws IOException
     */
    public void closeEntry() throws IOException {
        if (curEntry == null) {
            return;
        }
        if (curWroteBytes % 2 != 0) {
            raf.write('\n');
        }
        long curEntryDataEnd = raf.getFilePointer();
        raf.seek(curFileLenghtPointer);
        write(String.valueOf(curWroteBytes), 10);
        raf.seek(curEntryDataEnd);
        curWroteBytes = 0;
        curEntry = null;
    }

    @Override
    public void write(int b) throws IOException {
        raf.write(b);
        curWroteBytes++;
    }

    @Override
    public void write(byte[] b) throws IOException {
        raf.write(b);
        curWroteBytes += b.length;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        raf.write(b, off, len);
        curWroteBytes += len;
    }

    @Override
    public void close() throws IOException {
        if (curEntry != null) {
            closeEntry();
        }
        raf.getFD().sync();
        raf.close();
        isClosed = true;
    }

    @Override
    public void flush() throws IOException {
        raf.getChannel().force(false);
    }
}
