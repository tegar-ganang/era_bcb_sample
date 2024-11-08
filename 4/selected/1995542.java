package ru.mcfr.oxygen.updater.utils;

import org.apache.log4j.Logger;
import java.io.*;

public class Streamer {

    private static Logger logger = Logger.getLogger(Streamer.class);

    public static void bufferedStreamCopy(InputStream in, OutputStream out) throws IOException {
        int bufSizeHint = 1;
        int read = -1;
        byte[] buf = new byte[bufSizeHint];
        BufferedInputStream from = new BufferedInputStream(in, bufSizeHint);
        BufferedOutputStream to = new BufferedOutputStream(out, bufSizeHint);
        while ((read = from.read(buf, 0, bufSizeHint)) >= 0) {
            to.write(buf, 0, read);
        }
        to.close();
        out.close();
    }

    public static void bufferedStreamCopy_noCloseOut(InputStream in, OutputStream out) throws IOException {
        int bufSizeHint = 1;
        int read = -1;
        byte[] buf = new byte[bufSizeHint];
        BufferedInputStream from = new BufferedInputStream(in, bufSizeHint);
        BufferedOutputStream to = new BufferedOutputStream(out, bufSizeHint);
        while ((read = from.read(buf, 0, bufSizeHint)) >= 0) {
            to.write(buf, 0, read);
        }
    }

    public static boolean downloadFromStream(InputStream in, File to) {
        try {
            if (!to.exists()) {
                if (!to.getParentFile().exists()) to.getParentFile().mkdirs();
                to.createNewFile();
            }
            bufferedStreamCopy(in, new FileOutputStream(to));
        } catch (IOException e) {
            logger.error(e.getLocalizedMessage());
            return false;
        }
        return true;
    }
}
