package fi.hip.gb.disk.transport;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import fi.hip.gb.disk.conf.Config;

/**
 * Dummy transport wrapper to save files into local 
 * {@link fi.hip.gb.disk.conf.Config#getSiloDir()} folder.
 * 
 * @author Juho Karppinen
 * @version $Id: LocalTransport.java 1005 2006-04-19 14:03:36Z jkarppin $
 */
public class LocalTransport implements Transport {

    /**
     * Creates an new wrapper into local file.
     */
    public LocalTransport() {
    }

    public void put(String path, File file) throws IOException {
        copy(file, new File(Config.getSiloDir() + "/" + path));
    }

    public void get(String path, File file) throws IOException {
        copy(new File(Config.getSiloDir() + "/" + path), file);
    }

    public void delete(String path) throws IOException {
        if (new File(Config.getSiloDir() + "/" + path).delete() == false) {
            throw new IOException("Failed to remove the local file " + Config.getSiloDir() + "/" + path);
        }
    }

    public int exists(String path) throws IOException {
        File tempFile = new File(Config.getSiloDir() + "/" + path);
        return tempFile.exists() ? 1 : 0;
    }

    /**
     * Copy the file.
     * @param from full path to the source file
     * @param to full path to the target file
     * @throws IOException
     */
    private void copy(File from, File to) throws IOException {
        InputStream in = new FileInputStream(from);
        OutputStream out = new FileOutputStream(to);
        byte[] line = new byte[16384];
        int bytes = -1;
        while ((bytes = in.read(line)) != -1) out.write(line, 0, bytes);
        in.close();
        out.close();
    }
}
