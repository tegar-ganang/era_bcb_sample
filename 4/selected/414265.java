package net.sf.k_automaton;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;

/**
 * This code derived from java.util.Scanner
 *
 * @author Dmitri
 */
public class Tape implements Readable {

    private Readable source;

    private String sourceName;

    public Tape(Readable source) {
        this(source, null);
    }

    public Tape(Readable source, String sourceName) {
        if (source == null) throw new NullPointerException("source");
        this.source = source;
        if (sourceName == null) sourceName = "<anonymous source>";
        this.sourceName = sourceName;
    }

    public Tape(File source) throws IOException {
        this(source, Charset.defaultCharset().name());
    }

    public Tape(File source, String encodingName) throws IOException {
        if (source == null) throw new NullPointerException("source");
        FileInputStream stream = new FileInputStream(source);
        ReadableByteChannel channel = (ReadableByteChannel) (stream.getChannel());
        Reader reader = Channels.newReader(channel, encodingName);
        this.source = reader;
        this.sourceName = source.getName();
    }

    public int read(CharBuffer chb) throws IOException {
        return source.read(chb);
    }

    public String getSourceName() {
        return sourceName;
    }
}
