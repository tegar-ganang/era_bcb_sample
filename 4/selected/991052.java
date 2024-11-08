package de.gee.erep.server.util.unzip;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;

/**
 * @author Matthew Gee created: 14.03.2011
 */
public class Unzipper {

    public static void unzipAndRemove(final String file) {
        String destination = file.substring(0, file.length() - 3);
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new GZIPInputStream(new FileInputStream(file));
            os = new FileOutputStream(destination);
            byte[] buffer = new byte[8192];
            for (int length; (length = is.read(buffer)) != -1; ) os.write(buffer, 0, length);
        } catch (IOException e) {
            System.err.println("Fehler: Kann nicht entpacken " + file);
        } finally {
            if (os != null) try {
                os.close();
            } catch (IOException e) {
            }
            if (is != null) try {
                is.close();
            } catch (IOException e) {
            }
        }
        deleteFile(file);
    }

    /**
	 * @param file
	 *            f
	 */
    private static void deleteFile(final String file) {
        new File(file).delete();
    }

    public static void main(String[] args) {
        String source, destination;
        if (args[0].toLowerCase().endsWith(".gz")) {
            source = args[0];
            destination = source.substring(0, source.length() - 3);
        } else {
            source = args[0] + ".gz";
            destination = args[0];
        }
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new GZIPInputStream(new FileInputStream(source));
            os = new FileOutputStream(destination);
            byte[] buffer = new byte[8192];
            for (int length; (length = is.read(buffer)) != -1; ) os.write(buffer, 0, length);
        } catch (IOException e) {
            System.err.println("Fehler: Kann nicht entpacken " + args[0]);
        } finally {
            if (os != null) try {
                os.close();
            } catch (IOException e) {
            }
            if (is != null) try {
                is.close();
            } catch (IOException e) {
            }
        }
    }
}
