package annone.util;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class TranslatedInputStream extends InputStream {

    private static final int CHUNK_SIZE = 8;

    private static class Buffer {

        private Buffer next;

        private byte[] bytes;

        private int writeIndex;

        private int readIndex;

        public Buffer() {
            this.bytes = new byte[CHUNK_SIZE];
        }

        public boolean isReadAvailable() {
            return (readIndex < writeIndex);
        }

        public int read() throws IOException {
            if (readIndex < writeIndex) return bytes[readIndex++]; else throw new EOFException("Read buffer not available.");
        }

        public boolean isWriteAvailable() {
            return (writeIndex < bytes.length);
        }

        public void write(int b) throws IOException {
            if (writeIndex < bytes.length) bytes[writeIndex++] = (byte) b; else throw new EOFException("Write buffer not available.");
        }

        @Override
        public String toString() {
            return "R[" + readIndex + "] W[" + writeIndex + "] L[" + bytes.length + "]";
        }
    }

    private class BufferOutputStream extends OutputStream {

        @Override
        public void write(int b) throws IOException {
            if (!writeBuffer.isWriteAvailable()) {
                Buffer newBuffer = new Buffer();
                writeBuffer.next = newBuffer;
                writeBuffer = newBuffer;
            }
            writeBuffer.write(b);
        }
    }

    private Buffer firstBuffer;

    private Buffer writeBuffer;

    private Buffer readBuffer;

    private final OutputStream out;

    public TranslatedInputStream() {
        firstBuffer = new Buffer();
        writeBuffer = firstBuffer;
        readBuffer = firstBuffer;
        out = new BufferOutputStream();
    }

    @Override
    public int read() throws IOException {
        if (!readBuffer.isReadAvailable()) if (writeBuffer == null) return -1; else if (!readBuffer.isWriteAvailable()) readBuffer = readBuffer.next; else {
            if (!nextChunk(out)) writeBuffer = null;
            return read();
        }
        return readBuffer.bytes[readBuffer.readIndex++];
    }

    public OutputStream getOutputStream() {
        return out;
    }

    protected abstract boolean nextChunk(OutputStream out) throws IOException;
}
