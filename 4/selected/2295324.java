package net.sf.fileexchange.util.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class Util {

    public static byte[] readAll(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int readedBytes = inputStream.read(buffer);
        while (readedBytes != -1) {
            outputStream.write(buffer, 0, readedBytes);
            readedBytes = inputStream.read(buffer);
        }
        return outputStream.toByteArray();
    }
}
