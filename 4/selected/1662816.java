package com.knowgate.dfs;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import com.knowgate.debug.DebugFile;

/** This class links input and output streams so that data taken from input
 *  stream is transfered to the output stream. This class can be used to
 *  connect standard input/ouput stream of Java application with
 *  output/input streams of spawned child process, so that all user's input is
 *  redirected to the child and all it's output is visible for the user.<P>
 *
 *  This class starts a thread, which transfers data from input stream to
 *  output stream until End Of File is reached or IOException caused by IO
 *  error is catched.
 * @author Konstantin Knizhnik
 * @version 1.04
 */
public class StreamPipe {

    /** Default size of buffer used to transfer data from the input
     *  stream to the output stream.
     */
    final int defaultBufferSize = 8000;

    boolean bSynchronous;

    /**
     * <b>Create synchronous stream conector.</b>
     * between() methods do not return until end of input stream is reached and
     * written into ouput stream.
     */
    public StreamPipe() {
        bSynchronous = true;
    }

    /**
     * <b>Create synchronous or asynchronous stream conector.</b>
     * between() methods do not return until end of input stream is reached and
     * written into ouput stream.
     */
    public StreamPipe(boolean bSync) {
        bSynchronous = bSync;
    }

    private static void pipe(InputStream in, OutputStream out, int bufferSize, boolean autoFlush) throws IOException {
        if (DebugFile.trace) {
            DebugFile.writeln("Begin StreamPipe.pipe()");
        }
        int total = 0;
        byte[] buffer = new byte[bufferSize];
        int length;
        while ((length = in.read(buffer)) > 0) {
            if (DebugFile.trace) DebugFile.writeln("OutputStream.write(byte[], 0, " + String.valueOf(length) + ")");
            out.write(buffer, 0, length);
            if (autoFlush) out.flush();
            total += length;
        }
        if (DebugFile.trace) {
            DebugFile.writeln("End StreamPipe.pipe() : " + String.valueOf(total));
        }
    }

    /** Establish connection between input and output streams with specified size of buffer used for data transfer.
     * @param in  input stream
     * @param out output stream
     * @param bufferSize size of buffer used to transfer data from the input stream to the output stream
     * @param  autoFlush if set to <b>true</b> OutputStream.flush() method will be called each time bufferSize bytes are written into output stream
     */
    public void between(InputStream in, OutputStream out, int bufferSize, boolean autoFlush) throws IOException {
        if (bSynchronous) pipe(in, out, bufferSize, autoFlush); else (new PipeThread(in, out, bufferSize, autoFlush)).start();
    }

    /** Establish connection between input and output streams with specified size of buffer used for data transfer and no auto-flush.
     * @param in  input stream
     * @param out output stream
     */
    public void between(InputStream in, OutputStream out, int bufferSize) throws IOException {
        if (bSynchronous) pipe(in, out, bufferSize, false); else (new PipeThread(in, out, bufferSize, false)).start();
    }

    /** Establish connection between input and output streams with default buffer size and no auto-flush.
     * @param in  input stream
     * @param out output stream
     */
    public void between(InputStream in, OutputStream out) throws IOException {
        if (bSynchronous) pipe(in, out, defaultBufferSize, false); else (new PipeThread(in, out, defaultBufferSize, false)).start();
    }
}

final class PipeThread extends Thread {

    InputStream in;

    OutputStream out;

    byte[] buffer;

    boolean flush;

    PipeThread(InputStream in, OutputStream out, int bufferSize, boolean autoFlush) {
        this.in = in;
        this.out = out;
        buffer = new byte[bufferSize];
        flush = autoFlush;
    }

    public void run() {
        if (DebugFile.trace) {
            DebugFile.writeln("Begin StreamPipe.PipeThread.run()");
        }
        int total = 0;
        try {
            int length;
            while ((length = in.read(buffer)) > 0) {
                if (DebugFile.trace) DebugFile.writeln("OutputStream.write(byte[], 0, " + String.valueOf(length) + ")");
                out.write(buffer, 0, length);
                if (flush) out.flush();
                total += length;
            }
        } catch (IOException ex) {
        }
        if (DebugFile.trace) {
            DebugFile.writeln("End StreamPipe.PipeThread.run() : " + String.valueOf(total));
        }
    }
}
