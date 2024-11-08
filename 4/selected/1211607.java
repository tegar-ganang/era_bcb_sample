package net.diet_rich.jabak.core.datafile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import net.diet_rich.util.io.ExDataInputStream;
import net.diet_rich.util.io.GZipCountStream;

/**
 * read data chunks from compressed data files
 * 
 * @author Georg Dietrich
 * @version 1.0
 * 
 * minor change
 * keeping data file open for next read access
 */
public class DataFileReader {

    /** the length in bytes of the read/write buffer */
    private final int BUFLEN = 8192;

    /** the read/write buffer for data input/output */
    private final byte[] bytes = new byte[BUFLEN];

    /** the data file directory */
    private final File dir;

    /** the data file input stream */
    private ExDataInputStream input = null;

    /** index of the last data chunk read */
    private long lastIndex = -1;

    /** source file of the last data chunk read */
    private int lastFile = -1;

    /** size entry in the management data of an entry */
    private long c_size;

    /** index entry in the management data of an entry */
    private long c_index;

    /** ancillary data string in the management data of an entry */
    @SuppressWarnings("unused")
    private String c_ancString;

    /** the base name of the data files of the repository */
    private final String basename;

    /**
	 * initialize a new data file reader
	 * 
	 * @param directory  the data file directory
	 */
    public DataFileReader(File directory, String basename) {
        if (!directory.isDirectory() || !directory.canRead()) throw new RuntimeException("problem with data file directory " + directory);
        dir = directory;
        this.basename = basename;
    }

    /**
	 * read a data chunk from the backup data files
	 * 
	 * @param destination the destination stream to write the data to
	 * @param index the data chunk index to seek
	 * @param filenumber the backup data file to look in
	 * 
	 * @return false if the file didn't contain the data chunk specified
	 * @throws IOException
	 */
    public boolean read(OutputStream destination, long index, int filenumber) throws IOException {
        if (index < 1) throw new IndexOutOfBoundsException();
        if (filenumber < 0 || filenumber > 99999999) throw new IndexOutOfBoundsException();
        if (filenumber != lastFile || index <= lastIndex) open(filenumber);
        lastIndex = index;
        while (true) {
            readManagementData();
            if (c_index == index) break;
            long remaining = c_size;
            if (remaining < 0) throw new IOException();
            while (remaining > 0) {
                int read = (int) Math.min(BUFLEN, remaining);
                read = input.read(bytes, 0, read);
                if (read < 0) return false;
                remaining -= read;
            }
        }
        long length = c_size;
        while (length > 0) {
            int read = (int) Math.min(BUFLEN, length);
            read = input.read(bytes, 0, read);
            if (read == -1) {
                input.close();
                open(++filenumber);
                readManagementData();
                if (c_index != 0) throw new IOException();
                if (c_size != length) throw new IOException();
                read = 0;
            }
            destination.write(bytes, 0, read);
            length -= read;
        }
        return true;
    }

    private void readManagementData() throws IOException {
        c_size = input.readPosNum();
        c_index = input.readPosNum();
        c_ancString = input.readUTFstd();
    }

    /**
	 * open the backup data file specified by its file number
	 */
    private void open(int filenumber) throws IOException {
        String filename = "0000000" + filenumber;
        filename = filename.substring(filename.length() - 8) + GZipCountStream.fileext;
        filename = dir.getPath() + File.separator + basename + "_" + filename;
        if (input != null) input.close();
        input = new ExDataInputStream(new GZIPInputStream(new FileInputStream(filename)));
        @SuppressWarnings("unused") String header = input.readUTFstd();
        lastFile = filenumber;
    }
}
