package decoder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.io.IOUtils;

public class ExternalDecoder extends Decoder {

    private Process process;

    private InputStream processStdOut;

    private OutputStream processStdIn;

    protected ExternalDecoder(InputStream source, Process process) {
        super(source);
        this.process = process;
        this.processStdOut = process.getInputStream();
        this.processStdIn = process.getOutputStream();
        new Thread() {

            @Override
            public void run() {
                try {
                    IOUtils.copy(getSource(), processStdIn);
                    System.err.println("Copy done.");
                    close();
                } catch (IOException e) {
                    e.printStackTrace();
                    IOUtils.closeQuietly(ExternalDecoder.this);
                }
            }
        }.start();
    }

    @Override
    public int read() throws IOException {
        if (processStdOut == null) return -1;
        return processStdOut.read();
    }

    @Override
    public void close() throws IOException {
        super.close();
        process.destroy();
        process = null;
        processStdOut = null;
        processStdIn = null;
    }
}
