package com.georgeandabe.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StreamUtils {

    public static final String readInput(InputStream input) throws IOException {
        StringBuffer results = new StringBuffer();
        byte[] buffer = new byte[2048];
        int read = -1;
        while ((read = input.read(buffer)) > 0) {
            results.append(new String(buffer, 0, read));
        }
        input.close();
        return results.toString();
    }

    public static final void write(String input, OutputStream output) throws IOException {
        write(new ByteArrayInputStream(input.getBytes()), output);
    }

    public static void write(InputStream input, OutputStream output) throws IOException {
        write(input, output, -1);
    }

    public static void write(InputStream input, OutputStream output, long maxLength) throws IOException {
        byte[] buffer = new byte[2048];
        int read = -1;
        int totalRead = 0;
        while ((read = input.read(buffer)) > 0) {
            totalRead += read;
            if (maxLength > -1 && totalRead > maxLength) {
                throw new IOException("Exceded max length (" + maxLength + "): " + totalRead);
            }
            output.write(buffer, 0, read);
        }
        input.close();
        output.flush();
        output.close();
    }

    public static InputStream getResourceStream(String resourceName) throws IOException {
        return StreamUtils.class.getClassLoader().getResourceAsStream(resourceName);
    }

    public static String readResource(String resourceName) throws IOException {
        InputStream resourceStream = getResourceStream(resourceName);
        if (resourceStream == null) {
            return null;
        }
        return readInput(resourceStream);
    }

    public static void copyResource(String resourceName, File destinationDir) throws IOException {
        InputStream resourceStream = StreamUtils.class.getClassLoader().getResourceAsStream(resourceName);
        if (resourceStream == null) {
            throw new IOException("No such resource: " + resourceName);
        }
        write(resourceStream, new FileOutputStream(destinationDir), -1);
    }

    public static String readInput(InputStream input, int maxLength) throws IOException {
        StringBuffer results = new StringBuffer();
        byte[] buffer = new byte[2048];
        int read = -1;
        int totalRead = 0;
        while ((read = input.read(buffer)) > 0) {
            totalRead += read;
            if (maxLength > -1 && totalRead > maxLength) {
                throw new IOException("Exceded max length (" + maxLength + "): " + totalRead);
            }
            results.append(new String(buffer, 0, read));
        }
        input.close();
        return results.toString();
    }

    public static void discardInput(InputStream input) throws IOException {
        StringBuffer results = new StringBuffer();
        byte[] buffer = new byte[4096];
        int read = -1;
        while ((read = input.read(buffer)) > 0) {
            results.append(new String(buffer, 0, read));
        }
        input.close();
    }
}
