package net.googlecode.kharchenko.helpers.impl;

import net.googlecode.kharchenko.helpers.FileHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;

public class FileHelperImpl implements FileHelper {

    public void copyFile(MultipartFile file, String target) throws IOException {
        InputStream inputStream = file.getInputStream();
        OutputStream outputStream = new FileOutputStream(target);
        int readBytes = 0;
        byte[] buffer = new byte[10000];
        while ((readBytes = inputStream.read(buffer, 0, 10000)) != -1) {
            outputStream.write(buffer, 0, readBytes);
        }
        outputStream.close();
        inputStream.close();
    }
}
