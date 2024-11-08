package edu.uah.elearning.qti.service.impl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import edu.uah.elearning.qti.service.ZipService;

@Service(value = "zipService")
public class ZipFileImpl implements ZipService {

    Logger logger = LoggerFactory.getLogger(this.getClass());

    List<File> files = new ArrayList<File>();

    public int unzipFile(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
            ZipEntry entry;
            String path = file.getParent();
            while ((entry = zis.getNextEntry()) != null) {
                System.out.println("Unzipping: " + entry.getName());
                if (entry.isDirectory()) {
                    System.err.println("Extracting directory: " + entry.getName());
                    File dir = new File(path + File.separator + entry.getName());
                    dir.mkdir();
                    logger.info("dir:" + path);
                    continue;
                }
                int size;
                byte[] buffer = new byte[2048];
                File fileEntry = new File(path + File.separator + entry.getName());
                FileOutputStream fos = new FileOutputStream(fileEntry);
                BufferedOutputStream bos = new BufferedOutputStream(fos, buffer.length);
                while ((size = zis.read(buffer, 0, buffer.length)) != -1) {
                    bos.write(buffer, 0, size);
                }
                bos.flush();
                bos.close();
                files.add(fileEntry);
            }
            zis.close();
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private final void copyInputStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int len;
        while ((len = in.read(buffer)) >= 0) out.write(buffer, 0, len);
        in.close();
        out.close();
    }

    public static void main(String[] args) {
        ZipService zipService = new ZipFileImpl();
        zipService.unzipFile(new File("/home/emoriana/Desktop/Mathematics.zip"));
    }

    public List<File> getFiles() {
        return files;
    }
}
