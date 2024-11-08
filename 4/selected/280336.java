package de.andreavicentini.magicphoto.batch.problems;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.commons.io.IOUtils;

public class FileUtils {

    public static void copy(File source, File dest) throws FileNotFoundException, IOException {
        FileInputStream input = new FileInputStream(source);
        FileOutputStream output = new FileOutputStream(dest);
        System.out.println("Copying " + source + " to " + dest);
        IOUtils.copy(input, output);
        output.close();
        input.close();
        dest.setLastModified(source.lastModified());
    }
}
