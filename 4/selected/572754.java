package org.rt.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.channels.FileChannel;

public class FileUtil {

    /**
     * Copis source into destination file.
     * @param sourceFile the source file
     * @param destinationFile the destination file
     * @throws java.io.IOException
     */
    public static void copyFile(File sourceFile, File destinationFile) throws IOException {
        if (!destinationFile.exists()) {
            destinationFile.createNewFile();
        }
        FileChannel source = null;
        FileChannel destination = null;
        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destinationFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
        }
    }

    public static String getFileAsString(File file) throws IOException {
        StringBuffer buff = new StringBuffer();
        BufferedReader in = new BufferedReader(new FileReader(file));
        String line;
        while ((line = in.readLine()) != null) {
            buff.append(line);
            buff.append('\n');
        }
        buff.deleteCharAt(buff.length() - 1);
        return buff.toString();
    }
}
