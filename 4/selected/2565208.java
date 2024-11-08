package it.unibo.mortemale.cracker.john;

import java.io.*;
import java.util.zip.*;

public class DictUnZipper {

    static int BUFFER = 2048;

    /**
	 * Check if the file specified could be a zip archive
	 * @param fpath
	 * @return
	 */
    public static boolean seems_a_zip_archive(String fpath) {
        byte[] b = new byte[2];
        try {
            FileInputStream is = new FileInputStream(fpath);
            is.read(b);
            is.close();
            if (b[0] == 0x50 && b[1] == 0x4B) return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
	 * Unzip a zipped archive, merge all entries contained in the archive in a
	 * single file. Useful in case of separated wordlist into archive.
	 * 
	 * @param infile
	 *            archive file path
	 * @param outfile
	 * @return
	 */
    public static boolean unzip_and_merge(String infile, String outfile) {
        try {
            BufferedOutputStream dest = null;
            FileInputStream fis = new FileInputStream(infile);
            ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
            FileOutputStream fos = new FileOutputStream(outfile);
            dest = new BufferedOutputStream(fos, BUFFER);
            while (zis.getNextEntry() != null) {
                int count;
                byte data[] = new byte[BUFFER];
                while ((count = zis.read(data, 0, BUFFER)) != -1) dest.write(data, 0, count);
                dest.flush();
            }
            dest.close();
            zis.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
