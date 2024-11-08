package com.exadel.flamingo.flex.messaging.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Franck WOLFF
 */
public class StreamGobbler extends Thread {

    private final InputStream is;

    private final OutputStream os;

    public StreamGobbler(InputStream is, OutputStream os) {
        this.is = is;
        this.os = os;
    }

    @Override
    public void run() {
        try {
            for (int b = is.read(); b != -1; b = is.read()) os.write(b & 0xFF);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                os.flush();
                os.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
