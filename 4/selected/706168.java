package com.gestioni.adoc.aps.system.services.pec.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import com.agiletec.aps.system.exception.ApsSystemException;

public final class FileUtil {

    /**
	 * @param stream
	 * @param bos
	 * @throws IOException
	 */
    public static void writeToStream(InputStream stream, OutputStream bos) throws IOException {
        int bytesRead = 0;
        byte buffer[] = new byte[BUFFER_SIZE];
        while ((bytesRead = stream.read(buffer)) != -1) {
            bos.write(buffer, 0, bytesRead);
            bos.flush();
        }
    }

    /**
     * Utility per la scrittura dell file sul filesystem
     *
     * @param os	lo stream di output
     * @param filePath	il path del file in output
     * @return	true se il file è stato scritto correttamente false altrimenti
     * @throws IOException
     */
    public static boolean writeFile(OutputStream os, String filePath) throws IOException {
        InputStream is = null;
        try {
            is = new BufferedInputStream(new FileInputStream(filePath), BUFFER_SIZE);
            writeToStream(is, os);
        } catch (Throwable e) {
            return false;
        } finally {
            if (os != null) {
                os.close();
            }
            if (is != null) {
                is.close();
            }
        }
        return true;
    }

    public static boolean writeFile(InputStream is, String destPath) throws Throwable {
        OutputStream os = null;
        try {
            os = new BufferedOutputStream(new FileOutputStream(destPath), BUFFER_SIZE);
            writeToStream(is, os);
        } catch (Throwable e) {
            throw new ApsSystemException("Error while writting file");
        } finally {
            if (os != null) {
                os.close();
            }
        }
        return true;
    }

    public static boolean writeFile(InputStream is, OutputStream os) throws ApsSystemException {
        try {
            writeToStream(is, os);
        } catch (Throwable e) {
            throw new ApsSystemException("Error while writting file");
        }
        return true;
    }

    /**
     * Copia un file da source a destination utilizzando le nuove librerie java
     * "nio".
     *
     * @param source
     *            il file origine
     * @param destination
     *            il file destinazione
     */
    public static void copyFile(File source, File destination) {
        if (!source.exists()) {
            return;
        }
        if ((destination.getParentFile() != null) && (!destination.getParentFile().exists())) {
            destination.getParentFile().mkdirs();
        }
        try {
            FileChannel srcChannel = new FileInputStream(source).getChannel();
            FileChannel dstChannel = new FileOutputStream(destination).getChannel();
            dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
            srcChannel.close();
            dstChannel.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private static int BUFFER_SIZE = 1024;
}
