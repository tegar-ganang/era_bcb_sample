package edu.asu.commons.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.AccessControlException;

/**
 * $Id: ResourceLoader.java 72 2009-01-28 21:09:57Z alllee $
 * 
 * @author <a href='mailto:Allen.Lee@asu.edu'>Allen Lee</a>
 * @version $Revision: 72 $
 */
public class ResourceLoader {

    public static String getString(String path) {
        String content = "";
        BufferedReader reader = ResourceLoader.getBufferedReader(path);
        char[] buf = new char[1024];
        int numRead = 0;
        try {
            while ((numRead = reader.read(buf)) != -1) {
                String readData = String.valueOf(buf, 0, numRead);
                content += readData;
                buf = new char[1024];
            }
            reader.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new RuntimeException("Couldn't load resource: " + path);
        }
        return content;
    }

    public static BufferedReader getBufferedReader(String path) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(path));
        } catch (AccessControlException e) {
            InputStream stream = toInputStream(path);
            reader = new BufferedReader(new InputStreamReader(stream));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return reader;
    }

    public static InputStream toInputStream(String path) {
        InputStream stream = null;
        try {
            File file = new File(path);
            if (file.isFile()) {
                stream = new FileInputStream(file);
            } else {
                stream = getResourceAsStream(path);
            }
        } catch (AccessControlException e) {
            stream = getResourceAsStream(path);
        } catch (FileNotFoundException e) {
            stream = getResourceAsStream(path);
        }
        return stream;
    }

    public static InputStream getResourceAsStream(String path) {
        InputStream stream = ResourceLoader.class.getResourceAsStream(path);
        if (stream == null) {
            stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        }
        if (stream == null) {
            stream = ClassLoader.getSystemResourceAsStream(path);
        }
        if (stream == null) {
            throw new RuntimeException("Oh no - couldn't load resource: " + path);
        }
        return stream;
    }

    public static ByteArrayOutputStream getByteArrayOutputStream(String fileName) throws IOException {
        InputStream stream = ResourceLoader.toInputStream(fileName);
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try {
            while (stream.available() > 0) {
                byteStream.write(stream.read());
            }
            return byteStream;
        } finally {
            stream.close();
        }
    }

    public static URL getResourceAsUrl(String resource) {
        URL url = ResourceLoader.class.getResource(resource);
        if (url == null) {
            url = Thread.currentThread().getContextClassLoader().getResource(resource);
        }
        if (url == null) {
            throw new RuntimeException("Could not load resource via ClassLoader: " + resource);
        }
        return url;
    }
}
