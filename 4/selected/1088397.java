package de.lema.appender.net;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

public abstract class AbstractObjectWriter {

    protected static final int PREFIX_SIZE = 4;

    protected static class HeaderElement {

        private static final int ELEMENT_LENGTH = 1 + 8 + 4;

        private static final byte STATUS_BELEGT = 1;

        private static final byte STATUS_DELETE = 2;

        private static final byte STATUS_FREI = 0;

        private final int lengthImBody;

        private final int elementNummerImHeader;

        private final long startPosImBody;

        private final byte status;

        private HeaderElement(byte status, long startPos, int length, int position) {
            super();
            this.status = status;
            this.startPosImBody = startPos;
            this.lengthImBody = length;
            this.elementNummerImHeader = position;
        }

        public int getStartPosInHeader() {
            return PREFIX_SIZE + this.getElementNummerImHeader() * HeaderElement.ELEMENT_LENGTH;
        }

        public HeaderElement(ByteBuffer buffer, int position) {
            this(buffer.get(), buffer.getLong(), buffer.getInt(), position);
        }

        public HeaderElement(long startPos, int length, int position) {
            this(STATUS_BELEGT, startPos, length, position);
        }

        /**
		 * @return the length
		 */
        public int getLengthImBody() {
            return lengthImBody;
        }

        /**
		 * @return the position
		 */
        public int getElementNummerImHeader() {
            return elementNummerImHeader;
        }

        /**
		 * @return the startPos
		 */
        public long getStartPosImBody() {
            return startPosImBody;
        }

        public boolean istBelegt() {
            return status == STATUS_BELEGT;
        }

        public boolean istFrei() {
            return status == STATUS_FREI;
        }

        public boolean istGeloescht() {
            return status == STATUS_DELETE;
        }

        public HeaderElement setDeleted() {
            return new HeaderElement(STATUS_DELETE, this.startPosImBody, this.lengthImBody, this.elementNummerImHeader);
        }

        public String toString() {
            return "pos=" + elementNummerImHeader + ", status=" + status + ",  start=" + startPosImBody + ", Laenge=" + lengthImBody;
        }

        public void writeToBuffer(ByteBuffer headerBuffer) {
            headerBuffer.put(status);
            headerBuffer.putLong(startPosImBody);
            headerBuffer.putInt(lengthImBody);
        }

        public long getLengthImHeader() {
            return ELEMENT_LENGTH;
        }
    }

    private FileChannel channel;

    private ByteBuffer headerBuffer;

    protected static final int ELEMENT_LENGTH = HeaderElement.ELEMENT_LENGTH;

    private RandomAccessFile randomAccessFile;

    protected AbstractObjectWriter() {
    }

    public void close() throws IOException {
        if (isFileOpen()) {
            randomAccessFile.close();
            randomAccessFile = null;
            channel = null;
            headerBuffer = null;
        }
    }

    protected void createHeaderbuffer(int maxElementeImHeader) {
        if (headerBuffer == null) {
            headerBuffer = ByteBuffer.allocate(berechneErsteSchreibposition(maxElementeImHeader));
        }
    }

    protected Object readAndUpdateHeader(int maxElementeImHeader) throws IOException, ClassNotFoundException {
        FileLock lock = null;
        Object back = null;
        HeaderElement found = findBelegt();
        if (found != null) {
            try {
                lock = channel.lock(0, maxElementeImHeader, false);
                final byte[] in = readObject(found);
                back = ObjectSerializer.deserialize(in);
                HeaderElement deleted = found.setDeleted();
                writeHeaderElement(deleted, false);
            } finally {
                if (lock != null) {
                    lock.release();
                }
            }
        }
        return back;
    }

    protected void writeAndUpdateHeader(HeaderElement headerElementAkt, byte[] serializeObject, int maxElementeImHeader) throws IOException {
        FileLock lock = null;
        try {
            lock = channel.lock(0, maxElementeImHeader, false);
            writeObject(serializeObject, headerElementAkt.getStartPosImBody());
            writeHeaderElement(headerElementAkt, true);
        } finally {
            if (lock != null) {
                lock.release();
            }
        }
    }

    protected int initHeaderBufferWennNotwendig() throws IOException {
        int maxElementeImHeader = -1;
        FileLock lock = null;
        if (headerBuffer == null) {
            try {
                lock = channel.lock(0, 4L, false);
                ByteBuffer laenge = ByteBuffer.allocate(4);
                channel.position(0);
                while (laenge.hasRemaining()) {
                    channel.read(laenge);
                }
                maxElementeImHeader = laenge.getInt(0);
                if (maxElementeImHeader < 1) {
                    throw new IllegalStateException("Der Header in der Datei ist fehlerhaft");
                }
                createHeaderbuffer(maxElementeImHeader);
            } finally {
                if (lock != null) {
                    lock.release();
                }
            }
        }
        return maxElementeImHeader;
    }

    private byte[] readObject(HeaderElement found) throws IOException {
        byte[] in = new byte[found.getLengthImBody()];
        ByteBuffer buf = ByteBuffer.wrap(in);
        channel.position(found.getStartPosImBody());
        while (buf.hasRemaining()) {
            channel.read(buf);
        }
        return in;
    }

    private HeaderElement erstelleElementWennPosition0(HeaderElement heOld, int ersteSchreibposition) {
        if (heOld == null) {
            heOld = new HeaderElement(ersteSchreibposition, 0, -1);
        }
        return heOld;
    }

    private HeaderElement findBelegt() throws IOException {
        int anzahlElemente = readHeader();
        HeaderElement found = null;
        for (int i = 0; i < anzahlElemente; i++) {
            HeaderElement he = new HeaderElement(headerBuffer, i);
            if (he.istBelegt()) {
                found = he;
                break;
            }
        }
        return found;
    }

    protected boolean isFileOpen() {
        return channel != null;
    }

    protected void openFile(File file) throws FileNotFoundException {
        if (!file.exists()) {
            throw new IllegalArgumentException("Die Datei " + file + " existiert nicht.");
        }
        randomAccessFile = new RandomAccessFile(file, "rw");
        channel = randomAccessFile.getChannel();
    }

    /**
	 * @return max. Anzahl Elemente im Header.
	 * @throws IOException
	 */
    private int readHeader() throws IOException {
        headerBuffer.clear();
        channel.position(0L);
        while (headerBuffer.hasRemaining()) {
            channel.read(headerBuffer);
        }
        headerBuffer.position(0);
        return headerBuffer.getInt();
    }

    protected int berechneErsteSchreibposition(int maxElementeImHeader) {
        return PREFIX_SIZE + maxElementeImHeader * ELEMENT_LENGTH;
    }

    protected void writeEmptyHeader(int maxElementeImHeader) throws IOException {
        createHeaderbuffer(berechneErsteSchreibposition(maxElementeImHeader));
        headerBuffer.putInt(0, maxElementeImHeader);
        channel.position(0L);
        while (headerBuffer.hasRemaining()) {
            channel.write(headerBuffer);
        }
    }

    protected void writeHeaderElement(HeaderElement heNeu, boolean mitLock) throws IOException {
        headerBuffer.clear();
        heNeu.writeToBuffer(headerBuffer);
        headerBuffer.flip();
        channel.position(heNeu.getStartPosInHeader());
        while (headerBuffer.hasRemaining()) {
            channel.write(headerBuffer);
        }
    }

    protected void writeObject(final byte[] serializeObject, long startPos) throws IOException {
        final ByteBuffer out = ByteBuffer.wrap(serializeObject);
        channel.position(startPos);
        while (out.hasRemaining()) {
            channel.write(out);
        }
    }
}
