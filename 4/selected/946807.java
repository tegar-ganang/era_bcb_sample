package android.TICO;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class readProject {

    private static final int ZIP_BUFFER_SIZE = 8192;

    public static void loadZip(File zipFile, File tempDirectory) throws IOException {
        unzip(zipFile, tempDirectory);
    }

    public static void borrarTmp(File fich) {
        if (fich.isDirectory()) {
            File[] contenido = fich.listFiles();
            for (int i = 0; i < contenido.length; i++) {
                borrarTmp(contenido[i]);
            }
        }
        fich.delete();
    }

    private static void unzip(File srcFile, File dstDir) throws IOException {
        byte[] buffer = new byte[ZIP_BUFFER_SIZE];
        int bytes;
        ZipFile zipFile = new ZipFile(srcFile);
        Enumeration entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            if (entry.isDirectory()) {
                File newDirectory = new File(entry.getName());
                if (!newDirectory.exists()) newDirectory.mkdirs();
                continue;
            }
            File newFile = new File(dstDir, entry.getName().replace('\\', '/'));
            File newFileDir = newFile.getParentFile();
            if (!newFileDir.exists()) newFileDir.mkdirs();
            InputStream in = zipFile.getInputStream(entry);
            OutputStream out = new BufferedOutputStream(new FileOutputStream(newFile));
            while ((bytes = in.read(buffer)) >= 0) out.write(buffer, 0, bytes);
            in.close();
            out.close();
        }
        zipFile.close();
    }
}
