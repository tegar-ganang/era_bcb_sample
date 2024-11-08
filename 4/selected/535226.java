package org.cleartk.classifier.jar;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import org.apache.commons.io.IOUtils;

/**
 * Utility class for working with {@link JarInputStream} and {@link JarOutputStream} objects.
 * 
 * <br>
 * Copyright (c) 2011, Regents of the University of Colorado <br>
 * All rights reserved.
 * 
 * @author Steven Bethard
 */
public class JarStreams {

    public static JarEntry getNextJarEntry(JarInputStream modelStream, String expectedName) throws IOException {
        JarEntry entry = modelStream.getNextJarEntry();
        if (entry == null) {
            throw new IOException(String.format("expected next jar entry to be %s, found nothing", expectedName));
        }
        if (!entry.getName().equals(expectedName)) {
            throw new IOException(String.format("expected next jar entry to be %s, found %s", expectedName, entry.getName()));
        }
        return entry;
    }

    public static void putNextJarEntry(JarOutputStream modelStream, String name, File file) throws IOException {
        JarEntry entry = new JarEntry(name);
        entry.setSize(file.length());
        modelStream.putNextEntry(entry);
        InputStream fileStream = new BufferedInputStream(new FileInputStream(file));
        IOUtils.copy(fileStream, modelStream);
        fileStream.close();
    }
}
