package net.disy.legato.net.protocol.classpath;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collections;
import java.util.List;
import org.apache.commons.io.IOUtils;

public class ClassPathSocket extends Socket {

    public ClassPathSocket() throws SocketException {
        super();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new InputStream() {

            private ByteArrayInputStream in = null;

            public synchronized ByteArrayInputStream getIn() {
                if (in == null) {
                    in = new ByteArrayInputStream(getResponse());
                }
                return in;
            }

            @Override
            public int read() throws IOException {
                return getIn().read();
            }

            @Override
            public int available() throws IOException {
                return getIn().available();
            }

            @Override
            public void close() throws IOException {
                getIn().close();
            }

            @Override
            public synchronized void mark(int readlimit) {
                getIn().mark(readlimit);
            }

            @Override
            public boolean markSupported() {
                return true;
            }

            @Override
            public int read(byte[] b) throws IOException {
                return getIn().read(b);
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                return getIn().read(b, off, len);
            }

            @Override
            public synchronized void reset() throws IOException {
                getIn().reset();
            }

            @Override
            public long skip(long n) throws IOException {
                return getIn().skip(n);
            }
        };
    }

    private byte[] request;

    public byte[] getResponse() {
        final ByteArrayInputStream bais = new ByteArrayInputStream(request);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        List<String> lines = Collections.emptyList();
        try {
            @SuppressWarnings("unchecked") List<String> dl = IOUtils.readLines(bais);
            lines = dl;
        } catch (IOException ioex) {
            throw new AssertionError(ioex);
        }
        String resource = null;
        for (String line : lines) {
            if (line.startsWith("GET ")) {
                int endIndex = line.lastIndexOf(' ');
                resource = line.substring(4, endIndex);
            }
        }
        final PrintStream printStream = new PrintStream(baos);
        if (resource == null) {
            printStream.println("HTTP/1.1 400 Bad Request");
        } else {
            final InputStream inputStream = getClass().getResourceAsStream(resource);
            if (inputStream == null) {
                printStream.println("HTTP/1.1 404 Not Found");
                printStream.println();
            } else {
                printStream.println("HTTP/1.1 200 OK");
                printStream.println();
                try {
                    IOUtils.copy(inputStream, printStream);
                } catch (IOException ioex) {
                    throw new AssertionError(ioex);
                }
            }
        }
        printStream.flush();
        printStream.close();
        return baos.toByteArray();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return new OutputStream() {

            private ByteArrayOutputStream out = null;

            public synchronized ByteArrayOutputStream getOut() {
                if (out == null) {
                    out = new ByteArrayOutputStream();
                }
                return out;
            }

            @Override
            public void write(int b) throws IOException {
                getOut().write(b);
            }

            @Override
            public void write(byte[] b) throws IOException {
                getOut().write(b);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                getOut().write(b, off, len);
            }

            @Override
            public void flush() throws IOException {
                final byte[] bytes = getOut().toByteArray();
                ClassPathSocket.this.request = bytes;
            }

            @Override
            public void close() throws IOException {
                getOut().close();
            }
        };
    }
}
