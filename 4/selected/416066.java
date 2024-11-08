package org.junit.internal.runners;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

/**
 * A very simple PrintStream that looks at the current thread-local and retrieves the JUnitEventCycleRecorder,s printStream from there.
 * @author <a href="mailto:kristian@zenior*dot*no">Kristian Rosenvold</a>
 */
public class ThreadBoundPrintStream extends PrintStream {

    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

    PrintStream streamWhenNoThreadLocalStream = new PrintStream(byteArrayOutputStream);

    PrintStream systemStream;

    ThreadLocal<JUnitEventCycleRecorder> cycleRecorder;

    public ThreadBoundPrintStream(PrintStream systemOutStream, ThreadLocal<JUnitEventCycleRecorder> cycleRecorder) {
        super(systemOutStream);
        this.systemStream = systemOutStream;
        this.cycleRecorder = cycleRecorder;
    }

    private PrintStream getOutputStreamForCurrentThread() {
        JUnitEventCycleRecorder eventCycleRecorder = cycleRecorder.get();
        if (eventCycleRecorder != null) return eventCycleRecorder.getPrintStreamForThread();
        systemStream.println("No JUnitEventCycleRecorder on thread");
        return streamWhenNoThreadLocalStream;
    }

    @Override
    public void println() {
        getOutputStreamForCurrentThread().println();
    }

    @Override
    public void print(char c) {
        getOutputStreamForCurrentThread().print(c);
    }

    @Override
    public void println(char x) {
        getOutputStreamForCurrentThread().println(x);
    }

    @Override
    public void print(double d) {
        getOutputStreamForCurrentThread().print(d);
    }

    @Override
    public void println(double x) {
        getOutputStreamForCurrentThread().println(x);
    }

    @Override
    public void print(float f) {
        getOutputStreamForCurrentThread().print(f);
    }

    @Override
    public void println(float x) {
        getOutputStreamForCurrentThread().println(x);
    }

    @Override
    public void print(int i) {
        getOutputStreamForCurrentThread().print(i);
    }

    @Override
    public void println(int x) {
        getOutputStreamForCurrentThread().println(x);
    }

    @Override
    public void print(long l) {
        getOutputStreamForCurrentThread().print(l);
    }

    @Override
    public void println(long x) {
        getOutputStreamForCurrentThread().println(x);
    }

    @Override
    public void print(boolean b) {
        getOutputStreamForCurrentThread().print(b);
    }

    @Override
    public void println(boolean x) {
        getOutputStreamForCurrentThread().println(x);
    }

    @Override
    public void print(char s[]) {
        getOutputStreamForCurrentThread().print(s);
    }

    @Override
    public void println(char x[]) {
        getOutputStreamForCurrentThread().println(x);
    }

    @Override
    public void print(Object obj) {
        getOutputStreamForCurrentThread().print(obj);
    }

    @Override
    public void println(Object x) {
        getOutputStreamForCurrentThread().println(x);
    }

    @Override
    public void print(String s) {
        getOutputStreamForCurrentThread().print(s);
    }

    @Override
    public void println(String x) {
        getOutputStreamForCurrentThread().println(x);
    }

    @Override
    public void write(byte b[], int off, int len) {
        getOutputStreamForCurrentThread().write(b, off, len);
    }

    @Override
    public void close() {
        getOutputStreamForCurrentThread().close();
    }

    @Override
    public void flush() {
        getOutputStreamForCurrentThread().flush();
    }

    @Override
    public void write(int b) {
        getOutputStreamForCurrentThread().write(b);
    }

    @Override
    public void write(byte b[]) throws IOException {
        getOutputStreamForCurrentThread().write(b);
    }
}
