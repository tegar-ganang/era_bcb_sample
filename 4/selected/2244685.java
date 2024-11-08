package org.monet.docservice.core.library.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.monet.docservice.core.library.LibraryUtils;

public class LibraryUtilsImpl implements LibraryUtils {

    public void copyStream(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[4096];
        int readed = 0;
        while ((readed = input.read(buffer)) > 0) {
            output.write(buffer, 0, readed);
        }
    }

    @Override
    public byte[] copyStreamToByteArray(InputStream input) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[16384];
        while ((nRead = input.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
    }
}
