package org.melati.servlet;

import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.File;
import java.io.IOException;
import org.melati.util.DelimitedBufferedInputStream;

/**
 * Interface for a file uploaded from a HTML form.
 *
 * We store the data uploaded from a multipart form by saving it to
 * a file on disk and, optionally, give it an associated URL.
 */
public abstract class BaseFileDataAdaptor implements FormDataAdaptor {

    /** Size for byte buffers. */
    protected int BUFSIZE = 2048;

    /** The file in which to save the data. */
    protected File file = null;

    /** A URL to the data. */
    protected String url = null;

    /** Information about the uploaded file. */
    public MultipartFormField field = null;

    /**
   * @return The file in which to save the data
   */
    protected abstract File calculateLocalFile();

    /**
   * Return a URL to the saved file, null if not appropriate.
   * @return a URL to the saved file, null if not appropriate
   */
    protected abstract String calculateURL();

    /**
   * Return the data in the file as a byte array.
   * @return the data in the file as a byte array
   */
    public byte[] getData() {
        File file = getFile();
        if (file == null) return new byte[0];
        InputStream in = null;
        ByteArrayOutputStream out = null;
        try {
            in = new BufferedInputStream(new FileInputStream(file));
            out = new ByteArrayOutputStream();
            byte[] buff = new byte[BUFSIZE];
            int count;
            while ((count = in.read(buff, 0, buff.length)) > 0) out.write(buff, 0, count);
            return out.toByteArray();
        } catch (IOException e) {
            throw new FormDataAdaptorException("Couldn't retreive the data from the file", e);
        } finally {
            try {
                if (in != null) {
                    in.close();
                    in = null;
                }
                if (out != null) {
                    out.close();
                    out = null;
                }
            } catch (Exception e) {
                ;
            }
        }
    }

    /**
   * Return the size of the data.
   * @return the size of the data as a <code>long</code>
   */
    public long getSize() {
        return (getFile() != null) ? getFile().length() : 0;
    }

    /**
   * Return a File object pointing to the saved data.
   * 
   * @return the {@link #file}
   */
    public File getFile() {
        if (file == null) file = calculateLocalFile();
        return file;
    }

    /**
   * @return Url to the data, null if there isn't an appropriate one
   */
    public String getURL() {
        if (url == null) url = calculateURL();
        return url;
    }

    /**
   * Read data from in until the delim, work out which file to
   * save it in, and save it.
   * 
   * @param field  a {@link MultipartFormField}
   * @param in     a {@link DelimitedBufferedInputStream}
   * @param delim  the delimiter used to denote elements
   * @throws IOException if there is a problem reading the input 
   */
    public void readData(MultipartFormField field, DelimitedBufferedInputStream in, byte[] delim) throws IOException {
        this.field = field;
        OutputStream out = null;
        byte[] buff = new byte[BUFSIZE];
        int count;
        try {
            out = new BufferedOutputStream(new FileOutputStream(getFile()));
            while ((count = in.readToDelimiter(buff, 0, buff.length, delim)) > 0) out.write(buff, 0, count);
            if (count == -1) throw new IOException("Didn't find boundary whilst reading field data");
        } catch (IOException e) {
            throw e;
        } finally {
            try {
                if (out != null) {
                    out.close();
                    out = null;
                }
            } catch (Exception e) {
                ;
            }
        }
    }
}
