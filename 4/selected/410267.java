package org.basegen.plugins.basegen.communication;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * This class adjust Stub file created by Axis, adding support to complexTypes.
 */
public abstract class AxisAdjustAbstract implements AxisAdjust {

    /**
     * Constant buffer size
     */
    private static final int BUFFER_SIZE = 1024 * 1024;

    /**
     * Create bufferedReader
     * 
     * @param file file
     * @return buffered reader
     * @throws IOException io exception
     */
    protected BufferedReader getBufferedReader(File file) throws IOException {
        return new BufferedReader(new FileReader(file), BUFFER_SIZE);
    }

    /**
     * Create printWriter
     * 
     * @return print writer
     * @param file file
     * @throws IOException io exception
     */
    protected PrintWriter getPrintWriter(File file) throws IOException {
        File out = getTempFile(file);
        return new PrintWriter(new BufferedWriter(new FileWriter(out), BUFFER_SIZE));
    }

    /**
     * Create temp file
     * 
     * @param file file
     * @return temp file
     */
    protected File getTempFile(File file) {
        return new File(file.toString() + ".tmp");
    }

    /**
     * Write the rest of the file
     * 
     * @param reader reader
     * @param writer writer
     * @throws IOException io exception
     */
    protected void writeTail(BufferedReader reader, PrintWriter writer) throws IOException {
        String line = null;
        while ((line = reader.readLine()) != null) {
            writer.println(line);
        }
    }

    /**
     * Change skeleton files. Delete original. Rename generated.
     * 
     * @param file file
     * @throws IOException io exception
     */
    protected void changeFiles(File file) throws IOException {
        File out = getTempFile(file);
        if (!file.delete()) {
            throw new IOException("Unable to dele " + file);
        }
        if (!out.renameTo(file)) {
            throw new IOException("Unable to rename " + out + " to " + file);
        }
    }
}
