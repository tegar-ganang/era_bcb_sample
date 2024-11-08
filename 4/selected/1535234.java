package org.remus.infomngmnt.common.core.streams;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

/**
 * @author Tom Seidel <tom.seidel@remus-software.org>
 */
public class StreamUtil {

    public static String convertStreamToString(final InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    public static long stream(final InputStream inputStream, final OutputStream outputStream) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("input stream cannot be null");
        }
        if (outputStream == null) {
            throw new IllegalArgumentException("output stream cannot be null");
        }
        long counter = 0;
        int flushCounter = 0;
        byte[] buffer = new byte[4096];
        int read;
        while ((read = inputStream.read(buffer)) >= 0) {
            outputStream.write(buffer, 0, read);
            counter += read;
            flushCounter += read;
            if (flushCounter >= 8192) {
                outputStream.flush();
                flushCounter = 0;
            }
        }
        outputStream.flush();
        return counter;
    }

    public static byte[] convertStreamToByte(final InputStream is) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        final int BUF_SIZE = 1 << 8;
        byte[] buffer = new byte[BUF_SIZE];
        int bytesRead = -1;
        while ((bytesRead = is.read(buffer)) > -1) {
            out.write(buffer, 0, bytesRead);
        }
        is.close();
        return out.toByteArray();
    }
}
