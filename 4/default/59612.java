import java.io.*;

interface PipeSource extends Runnable {

    abstract PipedOutputStream getPipedOutputStream();

    abstract void connectOutputTo(PipeSink sink) throws IOException;

    abstract PipeSink getSink();

    abstract void start();
}

interface PipeSink extends Runnable {

    abstract PipedInputStream getPipedInputStream();

    abstract void connectInputTo(PipeSource source) throws IOException;
}

interface PipeFilter extends PipeSource, PipeSink {
}

;

class StreamPipeSource implements PipeSource {

    protected PipedOutputStream out = new PipedOutputStream();

    protected InputStream in;

    protected PipeSink sink;

    public StreamPipeSource(InputStream in) {
        this.in = in;
    }

    public PipedOutputStream getPipedOutputStream() {
        return out;
    }

    public PipeSink getSink() {
        return sink;
    }

    public void connectOutputTo(PipeSink sink) throws IOException {
        this.sink = sink;
        out.connect(sink.getPipedInputStream());
        sink.connectInputTo(this);
    }

    public void start() {
        new Thread(this).start();
        if (sink instanceof PipeFilter) ((PipeFilter) sink).start(); else new Thread(sink).start();
    }

    public void run() {
        byte[] buffer = new byte[512];
        int bytes_read;
        try {
            for (; ; ) {
                bytes_read = in.read(buffer);
                if (bytes_read == -1) {
                    return;
                }
                out.write(buffer, 0, bytes_read);
            }
        } catch (IOException e) {
            if (e instanceof EOFException) return; else System.out.println(e);
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                ;
            }
        }
    }
}

class StreamPipeSink implements PipeSink {

    protected PipedInputStream in = new PipedInputStream();

    protected OutputStream out;

    public StreamPipeSink(OutputStream out) {
        this.out = out;
    }

    public PipedInputStream getPipedInputStream() {
        return in;
    }

    public void connectInputTo(PipeSource source) throws IOException {
        in.connect(source.getPipedOutputStream());
    }

    public void run() {
        byte[] buffer = new byte[512];
        int bytes_read;
        try {
            for (; ; ) {
                bytes_read = in.read(buffer);
                if (bytes_read == -1) return;
                out.write(buffer, 0, bytes_read);
            }
        } catch (IOException e) {
            if (e instanceof EOFException) return; else System.out.println(e);
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                ;
            }
        }
    }
}

abstract class BasicPipeFilter implements PipeFilter {

    protected PipedInputStream in = new PipedInputStream();

    protected PipedOutputStream out = new PipedOutputStream();

    protected PipeSink sink;

    public PipedInputStream getPipedInputStream() {
        return in;
    }

    public PipedOutputStream getPipedOutputStream() {
        return out;
    }

    public void connectOutputTo(PipeSink sink) throws IOException {
        this.sink = sink;
        out.connect(sink.getPipedInputStream());
        sink.connectInputTo((PipeSource) this);
    }

    public void start() {
        new Thread(this).start();
        if (sink instanceof PipeFilter) ((PipeFilter) sink).start(); else new Thread(sink).start();
    }

    public PipeSink getSink() {
        return sink;
    }

    public void connectInputTo(PipeSource source) throws IOException {
        in.connect(source.getPipedOutputStream());
    }

    public void run() {
        try {
            filter();
        } catch (IOException e) {
            if (e instanceof EOFException) return; else System.out.println(e);
        } finally {
            try {
                out.close();
                in.close();
            } catch (IOException e) {
                ;
            }
        }
    }

    public abstract void filter() throws IOException;
}

class GrepFilter extends BasicPipeFilter {

    protected GrepInputStream gis;

    protected PrintStream pout = new PrintStream(out);

    public GrepFilter(String pattern) {
        gis = new GrepInputStream(new DataInputStream(in), pattern);
    }

    public void filter() throws IOException {
        String line;
        for (; ; ) {
            line = gis.readLine();
            if (line == null) return;
            pout.println(line);
        }
    }
}

class Rot13Filter extends BasicPipeFilter {

    public void filter() throws IOException {
        byte[] buffer = new byte[512];
        int bytes_read;
        for (; ; ) {
            bytes_read = in.read(buffer);
            if (bytes_read == -1) return;
            for (int i = 0; i < bytes_read; i++) {
                if ((buffer[i] >= 'a') && (buffer[i] <= 'z')) {
                    buffer[i] = (byte) ('a' + ((buffer[i] - 'a') + 13) % 26);
                }
                if ((buffer[i] >= 'A') && (buffer[i] <= 'Z')) {
                    buffer[i] = (byte) ('A' + ((buffer[i] - 'A') + 13) % 26);
                }
            }
            out.write(buffer, 0, bytes_read);
        }
    }
}

public class Pipes {

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println("Usage: java Pipes <pattern> <filename>");
            System.exit(0);
        }
        PipeSource source = new StreamPipeSource(new FileInputStream(args[1]));
        PipeFilter filter = new GrepFilter(args[0]);
        PipeFilter filter2 = new Rot13Filter();
        PipeFilter filter3 = new Rot13Filter();
        PipeSink sink = new StreamPipeSink(System.out);
        source.connectOutputTo(filter);
        filter.connectOutputTo(filter2);
        filter2.connectOutputTo(filter3);
        filter3.connectOutputTo(sink);
        source.start();
    }
}
