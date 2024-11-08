package com.curlap.orb.util;

import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import com.curlap.orb.io.SerializableStreamWriter;
import com.curlap.orb.io.SerializerException;

/**
 * TextFileStreamWriter
 */
public class TextFileStreamWriter {

    private static String DEFAULT_CHARSET = "UTF8";

    public static void write(String url, String charset, SerializableStreamWriter writer) throws IOException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(url), charset));
            String line = null;
            while ((line = reader.readLine()) != null) writer.write(line);
        } catch (FileNotFoundException e) {
            throw new IOException(e.getMessage());
        } catch (SerializerException e) {
            throw new IOException(e.getMessage());
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (SerializerException e) {
                    throw new IOException(e.getMessage());
                }
            }
            if (reader != null) reader.close();
        }
    }

    public static void write(String url, SerializableStreamWriter writer) throws IOException {
        write(url, DEFAULT_CHARSET, writer);
    }
}
