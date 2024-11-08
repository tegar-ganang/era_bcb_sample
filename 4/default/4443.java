import java.io.*;
import java.util.zip.*;

/**
 * This class defines two static methods for zipping and unzipping files.
 **/
public class Compress {

    /** Zip the contents of the from file and save in the to file. */
    public static void zipFile(String file, String entry) throws IOException {
        FileInputStream in = new FileInputStream(file);
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file + ".zip"));
        out.putNextEntry(new ZipEntry(entry));
        byte[] buffer = new byte[4096];
        int bytes_read;
        while ((bytes_read = in.read(buffer)) != -1) out.write(buffer, 0, bytes_read);
        in.close();
        out.closeEntry();
        out.close();
        File fin = new File(file);
        fin.delete();
    }
}
