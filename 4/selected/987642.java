package com.ibm.wala.cast.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class TemporaryFile {

    private static final String outputDir;

    static {
        String dir = System.getProperty("java.io.tmpdir");
        while (dir.endsWith(File.separator)) dir = dir.substring(0, dir.length() - 1);
        dir = dir + File.separator;
        outputDir = dir;
    }

    public static File urlToFile(String fileName, URL input) throws IOException {
        return streamToFile(fileName, input.openStream());
    }

    public static File streamToFile(String fileName, InputStream input) throws IOException {
        File F = new File(outputDir + File.separator + fileName);
        FileOutputStream output = new FileOutputStream(F);
        int read;
        byte[] buffer = new byte[1024];
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        output.close();
        input.close();
        return F;
    }
}
