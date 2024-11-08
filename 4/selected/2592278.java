package org.columba.ristretto.log;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * InputStream that logs the data that flows through it.
 * 
 * @author tstich
 *
 */
public class LogInputStream extends FilterInputStream {

    private static final byte[] PREFIX_STRING = { 'S', ':', ' ' };

    private static final int LINEEND = 0;

    private static final int IN_LINE = 1;

    private static final int MAX_LENGTH = 100 - PREFIX_STRING.length;

    private int state;

    private int line_length;

    private OutputStream logOutputStream;

    /**
     * Constructs the LogInputStream.java.
     * 
     * @param arg0
     * @param log
     *            The LogOutputStream
     */
    public LogInputStream(InputStream arg0, OutputStream log) {
        super(arg0);
        this.logOutputStream = log;
        state = LINEEND;
        line_length = 0;
    }

    /**
     * Constructs the LogInputStream.java. This constructor sets the
     * LogOutputStream to System.out.
     * 
     * @param arg0
     */
    public LogInputStream(InputStream arg0) {
        this(arg0, System.out);
    }

    /**
     * @see java.io.InputStream#read()
     */
    public int read() throws IOException {
        int read = in.read();
        if (read == -1) return -1;
        switch(state) {
            case (LINEEND):
                {
                    line_length = 0;
                    state = IN_LINE;
                    logOutputStream.write(PREFIX_STRING);
                    break;
                }
            case (IN_LINE):
                {
                    line_length++;
                    if (read == '\n') {
                        state = LINEEND;
                    } else if (line_length == MAX_LENGTH) {
                        line_length = 0;
                        logOutputStream.write('\\');
                        logOutputStream.write('\n');
                        logOutputStream.write(PREFIX_STRING);
                    }
                    break;
                }
        }
        logOutputStream.write(read);
        return read;
    }

    /**
     * @see java.io.InputStream#read(byte[], int, int)
     */
    public int read(byte[] arg0, int arg1, int arg2) throws IOException {
        int next;
        int i = 0;
        for (; i < arg2; i++) {
            next = read();
            if (next == -1) {
                break;
            }
            arg0[arg1 + i] = (byte) next;
        }
        if (i == 0) return -1; else return i;
    }

    /**
     * @return Returns the logOutputStream.
     */
    public OutputStream getLogOutputStream() {
        return logOutputStream;
    }

    /**
     * @param logOutputStream
     *            The logOutputStream to set.
     */
    public void setLogOutputStream(OutputStream logOutputStream) {
        this.logOutputStream = logOutputStream;
    }
}
