package com.ibm.tuningfork.infra.feed;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.FileChannel;
import com.ibm.tuningfork.infra.Logging;
import com.ibm.tuningfork.infra.Version;
import com.ibm.tuningfork.infra.chunk.Chunk;
import com.ibm.tuningfork.infra.chunk.ChunkUtils;
import com.ibm.tuningfork.infra.sharing.ClassRegistry;
import com.ibm.tuningfork.infra.sharing.ISharingConvertibleCallback;
import com.ibm.tuningfork.infra.util.FileUtility;

/**
 * A trace source coming from a file.
 */
public final class FileTraceSource extends FiniteTraceSource {

    private File file;

    private FileInputStream fs;

    private FileChannel fc;

    private long crc = 0;

    private boolean crcReady = false;

    private long fileSize = 0;

    private String displayName = null;

    public FileTraceSource(File f) throws FileNotFoundException {
        this.file = f;
        fs = new FileInputStream(file);
        fc = fs.getChannel();
        try {
            fileSize = fc.size();
        } catch (IOException ioe) {
            Logging.errorln("Warning: Could not determine size of file " + f);
        }
    }

    public static FileTraceSource fromWorkspaceRelativeFileName(String s) throws FileNotFoundException {
        return new FileTraceSource(new File(FileUtility.fromUserIndependentIfPossible(s)));
    }

    public static FileTraceSource fromWorkspaceRelativeFileNameForSharing(String fileName, long sampledCRC, long pid, String user, long fileSize) throws FileNotFoundException {
        FileTraceSource src = FileTraceSource.fromWorkspaceRelativeFileName(fileName);
        if (src.sampledCRC() != sampledCRC) throw new FileNotFoundException("CRC mismatch: " + fileName);
        return src;
    }

    public void collectReconstructionArguments(ISharingConvertibleCallback cb) throws Exception {
        Class<?> interactiveOpener = ClassRegistry.get("InteractiveFileTraceSourceOpener");
        final boolean interactiveOpenerAvailable = interactiveOpener != null;
        if (!interactiveOpenerAvailable) {
            cb.setStaticMethod(FileTraceSource.class, "fromWorkspaceRelativeFileNameForSharing");
            cb.convert(getWorkspaceRelativeFilename());
        } else {
            cb.setStaticMethod(interactiveOpener, "fromWorkspaceRelativeFileNameForSharing");
            cb.convert(getWorkspaceRelativeFilename());
            cb.convert(sampledCRC());
            cb.convert((Long) interactiveOpener.getMethod("getThisPID").invoke(interactiveOpener));
            cb.convert((String) interactiveOpener.getMethod("getThisUser").invoke(interactiveOpener));
            cb.convert(getLength());
        }
    }

    public long sampledCRC() throws FileNotFoundException {
        if (!crcReady) {
            crc = FileUtility.sampledCRC(file);
            crcReady = true;
        }
        return crc;
    }

    public String getFilename() {
        return file.getName();
    }

    public String getCompleteFilename() {
        return file.getAbsolutePath();
    }

    public String getWorkspaceRelativeFilename() {
        return FileUtility.toUserIndependentIfPossible(getCompleteFilename());
    }

    public synchronized void open() {
        Logging.msgln("Connected to file: " + file);
        try {
            Version v = readHeader(feed);
            feed.initialize(v);
            if (!feed.readIndex()) {
                feed.createIndex();
                FeedGroupRegistry.addFeed(feed);
                feed.writeIndex();
            }
        } catch (final IOException ex) {
            Logging.msgln("Reading trace file: IOException (" + ex.getMessage() + ").");
        } catch (final FeedFormatException ex) {
            Logging.errorln("***** Malformed trace file *****");
            ex.printStackTrace();
            Logging.errorln("Chunk Header:");
            ChunkUtils.dumpBuffer(chunkHeaderChunk);
            if (ex.getChunkBody() != null) {
                Logging.errorln("Chunk Body:");
                ChunkUtils.dumpBuffer(ex.getChunkBody());
            }
            feed.setMode(Feed.Mode.CORRUPT);
        }
    }

    public synchronized void close() {
        Logging.msgln("FileTraceSource.close()");
        if (fc != null) {
            try {
                fc.close();
            } catch (final IOException ex) {
                ex.printStackTrace();
            }
            fc = null;
            fs = null;
        }
    }

    public synchronized int readSomeBytes(Chunk chunk) throws IOException {
        return chunk.read(fc);
    }

    public boolean hasSeek() {
        return true;
    }

    public long getEstimatedLength() {
        return fileSize;
    }

    public synchronized long getLength() throws IOException {
        return fc.size();
    }

    public synchronized long position() throws IOException {
        return fc.position();
    }

    public synchronized void seek(long pos) throws IOException {
        fc.position(pos);
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        if (displayName != null) {
            return displayName;
        }
        String name = file.getName();
        if (name.endsWith(".trace")) {
            return name.substring(0, name.length() - 6);
        } else {
            return name;
        }
    }
}
