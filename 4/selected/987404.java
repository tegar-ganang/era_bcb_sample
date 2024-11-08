package windowsserver.fileexplorer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Compress {

    ZipOutputStream out;

    FileInputStream in;

    public void compress(String directoryPath) {
        try {
            String zipfile = directoryPath + ".zip";
            out = new ZipOutputStream(new FileOutputStream(zipfile));
            this.compressDirectory(directoryPath);
        } catch (Exception c) {
            c.printStackTrace();
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void compressFile(String filePath) {
        String outPut = filePath + ".zip";
        try {
            FileInputStream in = new FileInputStream(filePath);
            GZIPOutputStream out = new GZIPOutputStream(new FileOutputStream(outPut));
            byte[] buffer = new byte[4096];
            int bytes_read;
            while ((bytes_read = in.read(buffer)) != -1) out.write(buffer, 0, bytes_read);
            in.close();
            out.close();
        } catch (Exception c) {
            c.printStackTrace();
        }
    }

    private void compressDirectory(String directoryPath) {
        byte[] buffer = new byte[4096];
        byte[] extra = new byte[0];
        File dir = new File(directoryPath);
        int bytes_read;
        try {
            if (dir.isDirectory()) {
                String[] entries = dir.list();
                if (entries.length == 0) {
                    ZipEntry entry = new ZipEntry(dir.getPath() + "/");
                    out.putNextEntry(entry);
                }
                for (int i = 0; i < entries.length; i++) {
                    File f = new File(dir, entries[i]);
                    compressDirectory(f.getAbsolutePath());
                }
            } else {
                in = new FileInputStream(dir);
                ZipEntry entry = new ZipEntry(dir.getPath());
                out.putNextEntry(entry);
                while ((bytes_read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytes_read);
                }
                in.close();
            }
        } catch (Exception c) {
            c.printStackTrace();
        }
    }
}
