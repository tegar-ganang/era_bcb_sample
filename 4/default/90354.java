import java.io.*;
import java.util.*;
import java.util.zip.*;

class Zip {

    public static String StorageDir = System.getProperty("java.io.tmpdir") + "AppEmbed\\";

    public static final void copyInputStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int len;
        while ((len = in.read(buffer)) >= 0) out.write(buffer, 0, len);
        in.close();
        out.close();
    }

    public static String getFileName(String fileName) {
        File tmpFile = new File(fileName);
        tmpFile.getName();
        int whereDot = tmpFile.getName().lastIndexOf('.');
        if (0 < whereDot && whereDot <= tmpFile.getName().length() - 2) {
            return tmpFile.getName().substring(0, whereDot);
        }
        return "";
    }

    public static void Extract(String fileex) {
        Enumeration contents;
        ZipFile zipFile;
        try {
            zipFile = new ZipFile(fileex);
            contents = zipFile.entries();
            (new File(StorageDir + getFileName(fileex) + "\\")).mkdir();
            while (contents.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) contents.nextElement();
                if (entry.isDirectory()) {
                    System.err.println("Extracting directory: " + StorageDir + getFileName(fileex) + "\\" + entry.getName());
                    (new File(StorageDir + getFileName(fileex) + "\\" + entry.getName())).mkdirs();
                    continue;
                }
                System.err.println("Extracting file: " + StorageDir + getFileName(fileex) + "\\" + entry.getName());
                copyInputStream(zipFile.getInputStream(entry), new BufferedOutputStream(new FileOutputStream(StorageDir + getFileName(fileex) + "\\" + entry.getName())));
            }
            zipFile.close();
        } catch (IOException e) {
            System.err.println("Unhandled exception while extracting");
        }
    }
}
