package dk.highflier.airlog.utility;

import java.io.*;

/**
 * Class is for make a easy inputstream from a jar file. 
 */
public class FileInputJarStream extends InputStream {

    ByteArrayInputStream byteInput;

    ByteArrayOutputStream byteOutput;

    private org.log4j.Category log = org.log4j.Category.getInstance("Log.FileInputJarStream");

    /**
     * Method initializes the input stream
     *
     * @param		Filename the name of the file.
     *
     * @exception	FileNotFoundException if the file is not found in any jar-file
     */
    public FileInputJarStream(String filename) throws FileNotFoundException {
        byteOutput = getBytes(filename);
        byteInput = new ByteArrayInputStream(byteOutput.toByteArray());
    }

    public int available() {
        return byteInput.available();
    }

    public void close() throws IOException {
        byteInput.close();
    }

    public void mark(int readlimit) {
        byteInput.mark(readlimit);
    }

    public boolean markSupported() {
        return byteInput.markSupported();
    }

    public int read() {
        return byteInput.read();
    }

    public int read(byte[] b) throws IOException {
        return byteInput.read(b);
    }

    public int read(byte[] b, int off, int len) {
        return byteInput.read(b, off, len);
    }

    public void reset() {
        byteInput.reset();
    }

    public long skip(long n) {
        return byteInput.skip(n);
    }

    private ByteArrayOutputStream getBytes(String filename) throws FileNotFoundException {
        ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
        InputStream inputStream = ClassLoader.getSystemResourceAsStream(filename);
        if (inputStream == null) throw new FileNotFoundException("File " + filename + " not found.");
        byte[] bytes = new byte[1024];
        int read;
        try {
            while ((read = inputStream.read(bytes)) > -1) byteOutput.write(bytes, 0, read);
        } catch (Exception e) {
            log.error(e);
        }
        return byteOutput;
    }

    /**
     * Method reads the bytes from a file in a jar-file. Used for ImageIcons.
     *
     * @param		Filename, the name of the file to read.
     *
     * @remark		If the file is not found, "new byte[0]" is returned.
     *
     * @exception	None
     */
    public byte[] getBytes() {
        try {
            return byteOutput.toByteArray();
        } catch (Exception e) {
            log.error(e);
            return new byte[0];
        }
    }
}
