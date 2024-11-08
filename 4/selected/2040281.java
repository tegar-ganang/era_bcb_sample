package gnu.saw.runtime;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class SAWRuntimeProcessOutputConsumer implements Runnable {

    private static final int resultBufferSize = 256;

    private volatile boolean managed;

    private volatile boolean running;

    private int readChars;

    private final char[] resultBuffer = new char[resultBufferSize];

    private InputStreamReader in;

    private BufferedWriter out;

    public SAWRuntimeProcessOutputConsumer(InputStream in, BufferedWriter out, boolean managed) {
        this.in = new InputStreamReader(in);
        this.out = out;
        this.managed = managed;
        this.running = true;
    }

    public void stop() {
        running = false;
    }

    public void finalize() {
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
            }
        }
    }

    public void run() {
        while (running) {
            try {
                if (in.ready()) {
                    readChars = in.read(resultBuffer, 0, resultBufferSize);
                    if (readChars > 0 && running) {
                        if (managed) {
                            out.write(resultBuffer, 0, readChars);
                            out.flush();
                        }
                    } else {
                        running = false;
                        break;
                    }
                } else {
                    Thread.sleep(1);
                }
            } catch (Exception e) {
                running = false;
                break;
            }
        }
    }
}
