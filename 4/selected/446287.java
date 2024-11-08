package org.proteomecommons.mzml.zip;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 *
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class MZMLCompressionUtil {

    public static final int IO_BUFFER = 1024;

    /**
     * 
     * @param mzml
     * @param zipDestination
     * @throws java.lang.Exception
     */
    public static void zip(File mzml, File zipDestination) throws Exception {
        File preCompressionTmp = null;
        CompressionHandler comp = null;
        try {
            preCompressionTmp = new File(mzml.getName() + ".tmp");
            preCompressionTmp.createNewFile();
            if (!preCompressionTmp.canWrite()) {
                throw new Exception("Cannot write to temp file: " + preCompressionTmp.getAbsolutePath());
            }
            comp = new CompressionHandler();
            comp.compress(mzml, preCompressionTmp);
            comp.close();
            BufferedOutputStream bos = null;
            try {
                bos = new BufferedOutputStream(new FileOutputStream(zipDestination));
                bos.write(Util.convertIntToBytes(Util.getVersion()));
            } finally {
                try {
                    bos.flush();
                } catch (Exception nope) {
                }
                try {
                    bos.close();
                } catch (Exception nope) {
                }
            }
            GZIPOutputStream gos = null;
            BufferedInputStream bis = null;
            try {
                bis = new BufferedInputStream(new FileInputStream(preCompressionTmp));
                gos = new GZIPOutputStream(new FileOutputStream(zipDestination, true));
                final byte[] buffer = new byte[IO_BUFFER];
                int read = -1;
                while ((read = bis.read(buffer)) != -1) {
                    gos.write(buffer, 0, read);
                }
            } finally {
                try {
                    bis.close();
                } catch (Exception nope) {
                }
                try {
                    gos.flush();
                } catch (Exception nope) {
                }
                try {
                    gos.close();
                } catch (Exception nope) {
                }
            }
        } finally {
            try {
                comp.close();
            } catch (Exception nope) {
            }
            try {
                preCompressionTmp.delete();
            } catch (Exception nope) {
            }
        }
    }

    /**
     * 
     * @param mzmlZipped
     * @param unzipDestination
     * @throws java.lang.Exception
     */
    public static void unzip(File mzmlZipped, File unzipDestination) throws Exception {
        File unzipTmp = null;
        DecompressionHandler deh = null;
        try {
            int inputVersion = Util.getVersion(mzmlZipped);
            unzipTmp = new File(unzipDestination.getName() + ".tmp");
            unzipTmp.createNewFile();
            if (!unzipTmp.canWrite()) {
                throw new Exception("Cannot write to temp file: " + unzipTmp.getAbsolutePath());
            }
            Util.decompressGZIP(mzmlZipped, unzipTmp, 4);
            deh = new DecompressionHandler();
            deh.decompress(unzipTmp, unzipDestination);
            deh.close();
        } finally {
            try {
                deh.close();
            } catch (Exception nope) {
            }
            try {
                unzipTmp.delete();
            } catch (Exception nope) {
            }
        }
    }
}
