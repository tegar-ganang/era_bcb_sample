package org.homeunix.drummer.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class FileFunctions {

    private FileFunctions() {
    }

    public static void copyFile(File source, File dest) throws IOException {
        FileChannel in = null, out = null;
        try {
            in = new FileInputStream(source).getChannel();
            out = new FileOutputStream(dest).getChannel();
            in.transferTo(0, in.size(), out);
        } catch (FileNotFoundException fnfe) {
            Log.debug(fnfe);
        } finally {
            if (in != null) in.close();
            if (out != null) out.close();
        }
    }
}
