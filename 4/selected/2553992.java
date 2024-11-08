package org.jtestcase.plugin.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;

public class FileUtils {

    public FileUtils() {
    }

    public static void write(String file, String text) {
        try {
            FileOutputStream f = new FileOutputStream(file);
            f.write(text.getBytes());
            f.close();
        } catch (FileNotFoundException e) {
            e.fillInStackTrace();
        } catch (SecurityException e) {
            e.fillInStackTrace();
        } catch (IOException e) {
            e.fillInStackTrace();
        }
    }

    public static String read(String file) {
        StringWriter txt = new StringWriter();
        try {
            FileInputStream f = new FileInputStream(file);
            for (int count = f.read(); count != -1; count = f.read()) txt.write((char) count);
            f.close();
        } catch (FileNotFoundException _ex) {
        } catch (SecurityException _ex) {
        } catch (IOException _ex) {
        }
        return txt.toString();
    }

    public static boolean exists(String file) {
        File f = new File(file);
        return f.exists();
    }
}
