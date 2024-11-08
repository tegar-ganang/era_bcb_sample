package sample.zip;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class zipfolder {

    /**
	 * @param args
	 * @throws IOException
	 */
    public static void main(String[] args) throws IOException {
        compress("/home/paresh/Documents/xml/Assignment3/Assignment3/Part1");
    }

    public static void decompress(String fileName, String extractTo) throws IOException {
    }

    public static void compress(String path) throws IOException {
        BufferedOutputStream bf = new BufferedOutputStream(new FileOutputStream(path + ".zip"));
        ZipOutputStream zip = new ZipOutputStream(bf);
        compress(path, "", zip);
        zip.close();
        bf.close();
    }

    public static void compress(String path, String parent, ZipOutputStream zip) throws IOException {
        File[] f = new File(path).listFiles();
        byte[] buffer = new byte[4096];
        int bytes_read;
        for (int i = 0; i < f.length; i++) {
            if (f[i].isFile()) {
                FileInputStream in = new FileInputStream(f[i]);
                ZipEntry entry = new ZipEntry(parent + f[i].getName());
                zip.putNextEntry(entry);
                while ((bytes_read = in.read(buffer)) != -1) zip.write(buffer, 0, bytes_read);
                in.close();
            } else if (f[i].isDirectory()) {
                compress(f[i].getAbsolutePath(), parent + f[i].getName() + File.separator, zip);
            }
        }
    }
}
