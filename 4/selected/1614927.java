package com.luxoft.fitpro.plugin.wizards.importexport;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;

public class FitHtmlConverter implements ExportConverter {

    private static final int DEFAULT_BUFFER_SIZE = 16 * 1024;

    public String getExtension() {
        return "html";
    }

    public void write(BufferedInputStream istream, BufferedOutputStream ostream) throws IOException {
        int available = (istream.available()) <= 0 ? DEFAULT_BUFFER_SIZE : istream.available();
        int chunkSize = Math.min(DEFAULT_BUFFER_SIZE, available);
        byte[] readBuffer = new byte[chunkSize];
        int n = istream.read(readBuffer);
        while (n > 0) {
            ostream.write(readBuffer, 0, n);
            n = istream.read(readBuffer);
        }
    }
}
