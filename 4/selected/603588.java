package com.google.code.ebmlviewer.io;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

/** The {@code EbmlFile} class allows read-only access to the EBML data. */
public final class EbmlFile implements Closeable {

    private final FileInputStream stream;

    private final boolean manageStream;

    private final FileChannel channel;

    private final boolean manageChannel;

    private final EbmlFileReader reader;

    /** The cached list of entries contained in the file. */
    private List<EbmlFileEntry> entries;

    /**
     * Creates a new {@code EbmlFile} from the specified file.
     *
     * @param file the file to be opened for reading
     *
     * @throws FileNotFoundException if the file does not exist
     */
    public EbmlFile(String file) throws FileNotFoundException {
        this(new File(file));
    }

    /**
     * Creates a new {@code EbmlFile} from the specified file.
     *
     * @param file the file to be opened for reading
     *
     * @throws FileNotFoundException if the file does not exist
     */
    public EbmlFile(File file) throws FileNotFoundException {
        this(new FileInputStream(file), true);
    }

    /**
     * Creates a new {@code EbmlFile} from the specified file input stream.
     * <p/>
     * The stream will not be closed when this {@code EbmlFile} is closed.
     *
     * @param stream the file stream to be used for reading
     *
     * @throws IllegalArgumentException if {@code stream} is {@code null}
     */
    public EbmlFile(FileInputStream stream) {
        this(stream, false);
    }

    private EbmlFile(FileInputStream stream, boolean manageStream) {
        if (stream == null) {
            throw new IllegalArgumentException("stream is null");
        }
        this.stream = stream;
        this.manageStream = manageStream;
        channel = stream.getChannel();
        manageChannel = true;
        reader = new EbmlFileReader(channel);
    }

    /**
     * Creates a new {@code EbmlFile} from the specified file channel.
     * <p/>
     * The channel will not be closed when this {@code EbmlFile} is closed.
     *
     * @param channel the file channel to be used for reading
     *
     * @throws IllegalArgumentException if {@code channel} is {@code null}
     */
    public EbmlFile(FileChannel channel) {
        this(channel, false);
    }

    private EbmlFile(FileChannel channel, boolean manageChannel) {
        if (channel == null) {
            throw new IllegalArgumentException("channel is null");
        }
        stream = null;
        manageStream = false;
        this.channel = channel;
        this.manageChannel = manageChannel;
        reader = new EbmlFileReader(channel);
    }

    @Override
    public void close() throws IOException {
        if (manageChannel) {
            channel.close();
        }
        if (manageStream && stream != null) {
            stream.close();
        }
    }

    /**
     * Returns whether the {@link #getEntries()} method will require blocking I/O operations.
     *
     * @return {@code true} if the {@link #getEntries()} method will require blocking I/O operations; {@code false}
     *         otherwise
     */
    public boolean getEntriesWillBlock() {
        return entries == null;
    }

    /**
     * Returns a list of entries contained in this file.
     *
     * @return a list of entries contained in this file
     *
     * @throws IOException if an I/O error has occurred
     */
    public List<EbmlFileEntry> getEntries() throws IOException {
        if (entries == null) {
            entries = reader.readEntries(0L, channel.size());
        }
        return new ArrayList<EbmlFileEntry>(entries);
    }
}
