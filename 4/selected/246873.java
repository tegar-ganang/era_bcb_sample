package gnu.saw.stream;

import java.io.IOException;
import java.io.OutputStream;

public class SAWMultiplexingOutputStream {

    public class SAWMultiplexedOutputStream extends OutputStream {

        private volatile boolean closed;

        private boolean autoFlushPackets;

        private boolean writeClosePackets;

        private boolean writeOpenPackets;

        private int number;

        private int packetSize;

        private SAWMultiplexingOutputStream multiplexedOutputStream;

        private SAWMultiplexedOutputStream(SAWMultiplexingOutputStream multiplexedOutputStream, int number, int packetSize, boolean autoFlushPackets, boolean writeClosePackets, boolean writeOpenPackets) {
            this.multiplexedOutputStream = multiplexedOutputStream;
            this.number = number;
            this.packetSize = packetSize;
            this.autoFlushPackets = autoFlushPackets;
            this.writeClosePackets = writeClosePackets;
            this.writeOpenPackets = writeOpenPackets;
            this.closed = false;
        }

        public void write(byte[] data, int offset, int length) throws IOException {
            int written = 0;
            int position = offset;
            int remaining = length;
            if (autoFlushPackets) {
                while (remaining > 0) {
                    written = Math.min(remaining, packetSize);
                    synchronized (this) {
                        if (closed) {
                            throw new IOException("OutputStream closed");
                        }
                        multiplexedOutputStream.writePacketFlushing(data, position, written, number);
                    }
                    position += written;
                    remaining -= written;
                }
            } else {
                while (remaining > 0) {
                    written = Math.min(remaining, packetSize);
                    synchronized (this) {
                        if (closed) {
                            throw new IOException("OutputStream closed");
                        }
                        multiplexedOutputStream.writePacket(data, position, written, number);
                    }
                    position += written;
                    remaining -= written;
                }
            }
        }

        public void write(byte[] data) throws IOException {
            write(data, 0, data.length);
        }

        public void write(int data) throws IOException {
            if (autoFlushPackets) {
                synchronized (this) {
                    if (closed) {
                        throw new IOException("OutputStream closed");
                    }
                    multiplexedOutputStream.writePacketFlushing(data, number);
                }
            } else {
                synchronized (this) {
                    if (closed) {
                        throw new IOException("OutputStream closed");
                    }
                    multiplexedOutputStream.writePacket(data, number);
                }
            }
        }

        public void flush() throws IOException {
            if (!autoFlushPackets) {
                synchronized (this) {
                    if (closed) {
                        throw new IOException("OutputStream closed");
                    }
                    multiplexedOutputStream.flush();
                }
            }
        }

        public void close() throws IOException {
            synchronized (this) {
                if (!closed && writeClosePackets) {
                    closed = true;
                    multiplexedOutputStream.writeClosePacketFlushing(number);
                }
                closed = true;
            }
        }

        public void open() throws IOException {
            synchronized (this) {
                if (closed && writeOpenPackets) {
                    closed = false;
                    multiplexedOutputStream.writeOpenPacketFlushing(number);
                }
                closed = false;
            }
        }
    }

    private volatile boolean writing;

    private boolean autoFlushPackets;

    private int packetSize;

    private OutputStream out;

    private SAWMultiplexedOutputStream[] channels;

    private SAWByteArrayOutputStream packetBuffer;

    private SAWLittleEndianOutputStream packetStream;

    public SAWMultiplexingOutputStream(OutputStream out, int channelsNumber, int packetSize, boolean autoFlushPackets, boolean writeClosePackets, boolean writeOpenPackets) {
        this.out = out;
        this.channels = new SAWMultiplexedOutputStream[channelsNumber];
        this.packetSize = packetSize;
        this.autoFlushPackets = autoFlushPackets;
        for (int i = 0; i < channelsNumber; i++) {
            this.channels[i] = new SAWMultiplexedOutputStream(this, i, packetSize, autoFlushPackets, writeClosePackets, writeOpenPackets);
        }
        this.packetBuffer = new SAWByteArrayOutputStream(packetSize + 4);
        this.packetStream = new SAWLittleEndianOutputStream(packetBuffer);
        this.writing = false;
    }

    public SAWMultiplexedOutputStream getOutputStream(int number) {
        return channels[number];
    }

    public int getChannelsNumber() {
        return channels.length;
    }

    public int getPacketSize() {
        return packetSize;
    }

    public boolean isAutoFlushPackets() {
        return autoFlushPackets;
    }

    public void flush() throws IOException {
        try {
            synchronized (this) {
                while (writing) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                    }
                }
                writing = true;
            }
            out.flush();
        } finally {
            synchronized (this) {
                writing = false;
                notify();
            }
        }
    }

    public void close() throws IOException {
        try {
            synchronized (this) {
                while (writing) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                    }
                }
                writing = true;
            }
            out.close();
        } finally {
            synchronized (this) {
                writing = false;
                notify();
            }
        }
    }

    public void open(int number) throws IOException {
        channels[number].open();
    }

    public void close(int number) throws IOException {
        channels[number].close();
    }

    private void writePacket(int data, int number) throws IOException {
        try {
            synchronized (this) {
                while (writing) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                    }
                }
                writing = true;
            }
            packetBuffer.reset();
            packetStream.writeUnsignedShort(number);
            packetStream.writeShort((short) 1);
            packetStream.write(data);
            out.write(packetBuffer.buf(), 0, packetBuffer.count());
        } finally {
            synchronized (this) {
                writing = false;
                notify();
            }
        }
    }

    private void writePacket(byte[] data, int offset, int length, int number) throws IOException {
        try {
            synchronized (this) {
                while (writing) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                    }
                }
                writing = true;
            }
            packetBuffer.reset();
            packetStream.writeUnsignedShort(number);
            packetStream.writeShort((short) length);
            packetStream.write(data, offset, length);
            out.write(packetBuffer.buf(), 0, packetBuffer.count());
        } finally {
            synchronized (this) {
                writing = false;
                notify();
            }
        }
    }

    private void writePacketFlushing(int data, int number) throws IOException {
        try {
            synchronized (this) {
                while (writing) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                    }
                }
                writing = true;
            }
            packetBuffer.reset();
            packetStream.writeUnsignedShort(number);
            packetStream.writeShort((short) 1);
            packetStream.write(data);
            out.write(packetBuffer.buf(), 0, packetBuffer.count());
            out.flush();
        } finally {
            synchronized (this) {
                writing = false;
                notify();
            }
        }
    }

    private void writePacketFlushing(byte[] data, int offset, int length, int number) throws IOException {
        try {
            synchronized (this) {
                while (writing) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                    }
                }
                writing = true;
            }
            packetBuffer.reset();
            packetStream.writeUnsignedShort(number);
            packetStream.writeShort((short) length);
            packetStream.write(data, offset, length);
            out.write(packetBuffer.buf(), 0, packetBuffer.count());
            out.flush();
        } finally {
            synchronized (this) {
                writing = false;
                notify();
            }
        }
    }

    private void writeClosePacketFlushing(int number) throws IOException {
        try {
            synchronized (this) {
                while (writing) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                    }
                }
                writing = true;
            }
            packetBuffer.reset();
            packetStream.writeUnsignedShort(number);
            packetStream.writeShort((short) -2);
            out.write(packetBuffer.buf(), 0, packetBuffer.count());
            out.flush();
        } finally {
            synchronized (this) {
                writing = false;
                notify();
            }
        }
    }

    private void writeOpenPacketFlushing(int number) throws IOException {
        try {
            synchronized (this) {
                while (writing) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                    }
                }
                writing = true;
            }
            packetBuffer.reset();
            packetStream.writeUnsignedShort(number);
            packetStream.writeShort((short) -3);
            out.write(packetBuffer.buf(), 0, packetBuffer.count());
            out.flush();
        } finally {
            synchronized (this) {
                writing = false;
                notify();
            }
        }
    }
}
