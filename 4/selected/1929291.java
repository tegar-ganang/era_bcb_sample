package fildiv.jremcntl.tools.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PipeInputStreamToOutputStream implements Runnable {

    private final InputStream is;

    private final OutputStream os;

    public PipeInputStreamToOutputStream(InputStream is, OutputStream os) {
        this.is = is;
        this.os = os;
    }

    public void run() {
        byte[] buffer = new byte[1024];
        try {
            for (int count = 0; (count = is.read(buffer)) >= 0; ) os.write(buffer, 0, count);
        } catch (IOException e) {
            throw new ToolsRuntimeException(e);
        }
    }
}
