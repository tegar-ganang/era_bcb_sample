package com.peterhi.classroom.bootstrap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StdStreamRedirector implements Runnable {

    private final InputStream inputStream;

    private final OutputStream outputStream;

    public StdStreamRedirector(InputStream inputStream, OutputStream outputStream) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    public void run() {
        try {
            byte[] buffer = new byte[100];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }
    }
}
