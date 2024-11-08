package net.sourceforge.transmogrify.symtab.printer;

import java.io.File;
import java.io.IOException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;

public class FileUtil {

    public static void copyFile(File fromFile, File toFile) throws IOException {
        FileReader from = new FileReader(fromFile);
        FileWriter to = new FileWriter(toFile);
        char[] buffer = new char[4096];
        int bytes_read;
        while ((bytes_read = from.read(buffer)) != -1) {
            to.write(buffer, 0, bytes_read);
        }
        to.flush();
        to.close();
        from.close();
    }

    public static boolean fileContentsAreEqual(File a, File b) {
        boolean result = false;
        try {
            String astr = getFileContents(a).toString();
            String bstr = getFileContents(b).toString();
            if (astr == null && bstr == null) {
                result = true;
            } else if (astr != null && bstr != null) {
                result = astr.equals(bstr);
            } else {
                result = false;
            }
        } catch (Exception e) {
        }
        return result;
    }

    public static StringBuffer getFileContents(File file) throws Exception {
        StringBuffer result = new StringBuffer();
        BufferedReader reader = null;
        reader = new BufferedReader(new FileReader(file));
        String line = reader.readLine();
        while (line != null) {
            result.append(line);
            result.append("\n");
            line = reader.readLine();
        }
        return result;
    }
}
