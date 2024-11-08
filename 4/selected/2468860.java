package net.sf.beezle.sushi.launcher;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class InputPumpStream extends Thread {

    private final InputStream in;

    private final OutputStream out;

    private IOException exception;

    private volatile boolean finishing;

    public InputPumpStream(InputStream in, OutputStream out) {
        this.in = in;
        this.out = out;
        this.exception = null;
        this.finishing = false;
    }

    public void run() {
        try {
            while (true) {
                try {
                    while (!finishing && in.available() == 0) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            throw new Interrupted(e);
                        }
                    }
                    if (finishing) {
                        return;
                    }
                } catch (IOException e) {
                    if (in instanceof BufferedInputStream && "Stream closed".equals(e.getMessage())) {
                        return;
                    }
                }
                out.write(in.read());
                out.flush();
            }
        } catch (IOException e) {
            exception = e;
        }
    }

    public void finish(Launcher launcher) throws Failure {
        finishing = true;
        try {
            join();
        } catch (InterruptedException e) {
            throw new Interrupted(e);
        }
        if (exception != null) {
            throw new Failure(launcher, exception);
        }
    }
}
