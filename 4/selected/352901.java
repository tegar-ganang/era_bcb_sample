package jbomberman.net.pipe;

import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;

public class Pipe {

    private PipedReader reader_;

    private PipedWriter writer_;

    public Pipe() throws IOException {
        writer_ = new PipedWriter();
        reader_ = new PipedReader();
        writer_.connect(reader_);
    }

    public void write(int c) throws IOException {
        writer_.write(c);
    }

    public int read() throws IOException {
        return reader_.read();
    }

    public void closeWriter() throws IOException {
        writer_.flush();
        writer_.close();
    }

    public void closeReader() throws IOException {
        reader_.close();
    }
}
