package org.melati.servlet;

import java.io.File;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.melati.util.DelimitedBufferedInputStream;

/**
 * Store the uploaded data in a byte array in memory.
 */
public class MemoryDataAdaptor implements FormDataAdaptor {

    /** Size for byte buffers */
    protected int BUFSIZE = 512;

    private byte[] data = new byte[0];

    /**
   * Return the data as a byte array.
   * @return the data as a byte array
   */
    public byte[] getData() {
        return data;
    }

    /**
   * Return the size of the data.
   * @return the size of the data
   */
    public long getSize() {
        return data.length;
    }

    /**
   * return a File object pointing to the saved data (if one exists).
   * @return always <code>null</code>
   */
    public File getFile() {
        return null;
    }

    /**
   * Return a url to the object.
   * @return always null
   */
    public String getURL() {
        return null;
    }

    /**
   * read data from in until the delim and save it
   * in an internal buffer for later use
   *
   * @param field   a {@link MultipartFormField} to be read
   * @param in      a {@link DelimitedBufferedInputStream} to read from
   * @param delim   the delimitor to differentiate elements
   * @throws IOException if there is a problem reading the input 
   */
    public void readData(MultipartFormField field, DelimitedBufferedInputStream in, byte[] delim) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buff = new byte[BUFSIZE];
        int count;
        try {
            while ((count = in.readToDelimiter(buff, 0, buff.length, delim)) > 0) out.write(buff, 0, count);
            if (count == -1) throw new IOException("Didn't find boundary whilst reading field data");
            data = out.toByteArray();
        } catch (IOException e) {
            throw e;
        } finally {
            try {
                out.close();
                out = null;
            } catch (Exception e) {
                ;
            }
        }
    }
}
