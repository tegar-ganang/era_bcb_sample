package com.ibm.tuningfork.infra.feed;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.util.Date;
import com.ibm.tuningfork.infra.Logging;
import com.ibm.tuningfork.infra.Version;
import com.ibm.tuningfork.infra.chunk.Chunk;
import com.ibm.tuningfork.infra.sharing.ISharingConvertibleCallback;
import com.ibm.tuningfork.infra.sharing.UnimplementedConversionException;
import com.ibm.tuningfork.infra.util.FileUtility;
import com.ibm.tuningfork.infra.util.MiscUtils;

/**
 * A trace source coming from a socket.
 */
public final class SocketTraceSource extends InfiniteTraceSource {

    private InetSocketAddress isa;

    private SocketChannel socketChannel;

    private File traceFile;

    private FileChannel outChannel;

    private FileChannel inChannel;

    private long bytesRead = 0;

    private static final int NO_DATA_SLEEP_DURATION_MS = 50;

    public SocketTraceSource(InetSocketAddress isa) throws IOException {
        if (!FileUtility.cacheAvailable()) {
            throw new IOException("File Cache Not Available: Cannot create socket trace source.");
        }
        String dateStr = makeDateForTemporaryFilename();
        traceFile = FileUtility.getSocketFile("socket-" + dateStr + ".trace");
        FileOutputStream outStream = new FileOutputStream(traceFile);
        outChannel = outStream.getChannel();
        this.isa = isa;
    }

    public void collectReconstructionArguments(ISharingConvertibleCallback cb) throws Exception {
        throw new UnimplementedConversionException("Cannot convert: " + this.getClass().getSimpleName());
    }

    public static String makeDateForTemporaryFilename() {
        Date date = new Date(System.currentTimeMillis());
        String dateStr = date.toString().replace(' ', '_');
        dateStr = dateStr.toString().replace(':', '_');
        return dateStr;
    }

    @Override
    public void open() throws IOException {
        socketChannel = SocketChannel.open();
        socketChannel.connect(isa);
        Logging.msgln("Info: SocketTraceSource connected");
        FileInputStream inStream = new FileInputStream(traceFile);
        inChannel = inStream.getChannel();
        new Thread("Socket Receiver") {

            public void run() {
                listenAndWrite();
            }
        }.start();
        FeedGroupRegistry.addFeed(feed);
    }

    public void close() {
        if (socketChannel != null) {
            try {
                socketChannel.close();
            } catch (IOException e) {
                Logging.errorln("IOException closing socketChannel; continuing");
            }
            socketChannel = null;
        }
        if (outChannel != null) {
            try {
                outChannel.close();
            } catch (IOException e) {
                Logging.errorln("IOException closing outChannel; continuing");
            }
            outChannel = null;
        }
        if (inChannel != null) {
            try {
                inChannel.close();
            } catch (IOException e) {
                Logging.errorln("IOException closing outChannel; continuing");
            }
            inChannel = null;
        }
    }

    public int readSomeBytes(Chunk chunk) throws IOException {
        return chunk.read(inChannel);
    }

    public long getLength() {
        return -1;
    }

    public long getCurrentLength() {
        return bytesRead;
    }

    public long getEstimatedLength() {
        return getCurrentLength();
    }

    public long position() throws IOException {
        return inChannel.position();
    }

    public void seek(long position) throws IOException {
        inChannel.position(position);
    }

    public String getDisplayName() {
        return isa.toString();
    }

    public void listenAndWrite() {
        try {
            socketChannel.configureBlocking(false);
            ByteBuffer buffer = ByteBuffer.allocate(FeedConstants.MAX_CHUNK_BODY_SIZE);
            for (int i = 0; socketChannel.isConnected(); i++) {
                int remainingSize = FeedConstants.FEED_HEADER_SIZE;
                buffer.clear();
                if (i > 0) {
                    buffer.limit(FeedConstants.CHUNK_HEADER_SIZE);
                    while (buffer.remaining() > 0) {
                        socketChannel.read(buffer);
                        MiscUtils.milliSleep(NO_DATA_SLEEP_DURATION_MS);
                    }
                    remainingSize = buffer.getInt(FeedConstants.CHUNK_HEADER_BODY_LENGTH_POSITION);
                } else {
                    buffer.limit(0);
                }
                buffer.limit(buffer.limit() + remainingSize);
                while (buffer.remaining() > 0) {
                    socketChannel.read(buffer);
                    MiscUtils.milliSleep(NO_DATA_SLEEP_DURATION_MS);
                }
                buffer.position(0);
                while (buffer.remaining() > 0) {
                    outChannel.write(buffer);
                }
                bytesRead += buffer.limit();
                if (i == 0) {
                    while (inChannel.size() < bytesRead) {
                        MiscUtils.milliSleep(NO_DATA_SLEEP_DURATION_MS);
                    }
                    Version v = readHeader(feed);
                    feed.initialize(v);
                }
                MiscUtils.milliSleep(NO_DATA_SLEEP_DURATION_MS);
            }
        } catch (Exception exn) {
            Logging.errorln("SocketTraceSource: " + exn.getMessage());
        }
    }
}
