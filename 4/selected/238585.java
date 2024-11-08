package de.andreavicentini.magicinjection;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Files {

    public static void copy(File inputFile, File target) throws IOException {
        if (!inputFile.exists()) return;
        OutputStream output = new FileOutputStream(target);
        InputStream input = new BufferedInputStream(new FileInputStream(inputFile));
        int b;
        while ((b = input.read()) != -1) output.write(b);
        output.close();
        input.close();
    }

    public static void copy(String name, File source, File target) throws IOException {
        copy(new File(source, name), new File(target, name));
    }
}
