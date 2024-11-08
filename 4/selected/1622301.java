package gnu.saw.stream;

import java.io.IOException;
import java.io.InputStream;

public class SAWMultiplexingInputStream {

    private class SAWMultiplexingInputStreamPacketReader implements Runnable {

        private volatile boolean running;

        private SAWMultiplexingInputStream multiplexedInputStream;

        private SAWMultiplexingInputStreamPacketReader(SAWMultiplexingInputStream multiplexedInputStream) {
            this.multiplexedInputStream = multiplexedInputStream;
            this.running = true;
        }

        private void setRunning(boolean running) {
            this.running = running;
        }

        public void run() {
            while (running) {
                try {
                    multiplexedInputStream.readPacket();
                } catch (Exception e) {
                    running = false;
                }
            }
            try {
                multiplexedInputStream.close();
            } catch (IOException e1) {
            }
        }
    }

    public class SAWMultiplexedInputStream extends InputStream {

        private SAWMultiplexingInputStream multiplexedInputStream;

        private int number;

        private SAWMultiplexedInputStream(SAWMultiplexingInputStream multiplexedInputStream, int number) {
            this.multiplexedInputStream = multiplexedInputStream;
            this.number = number;
        }

        public int available() throws IOException {
            return multiplexedInputStream.available(number);
        }

        public int read() throws IOException {
            return multiplexedInputStream.readData(number);
        }

        public int read(byte[] data) throws IOException {
            return multiplexedInputStream.readData(data, number);
        }

        public int read(byte[] data, int offset, int length) throws IOException {
            return multiplexedInputStream.readData(data, offset, length, number);
        }

        public long skip(long count) throws IOException {
            return multiplexedInputStream.skip(count, number);
        }

        public void open() {
            multiplexedInputStream.open(number);
        }

        public void close() throws IOException {
            multiplexedInputStream.close(number);
        }
    }

    private int number;

    private int length;

    private int copied;

    private int readed;

    private int remaining;

    private Thread packetReaderThread;

    private SAWLittleEndianInputStream in;

    private SAWMultiplexingInputStreamPacketReader packetReader;

    private SAWMultiplexedInputStream[] channels;

    private SAWPipedInputStream[] pipedInputStreams;

    private SAWPipedOutputStream[] pipedOutputStreams;

    private byte[] packetBuffer;

    public SAWMultiplexingInputStream(InputStream in, int channelsNumber, int packetSize, int channelBufferSize, boolean startPacketReader) throws IOException {
        this.packetBuffer = new byte[packetSize];
        this.in = new SAWLittleEndianInputStream(in);
        this.channels = new SAWMultiplexedInputStream[channelsNumber];
        this.pipedInputStreams = new SAWPipedInputStream[channelsNumber];
        this.pipedOutputStreams = new SAWPipedOutputStream[channelsNumber];
        this.packetReader = new SAWMultiplexingInputStreamPacketReader(this);
        this.packetReaderThread = new Thread(packetReader, "SAWMultiplexingInputStreamPacketReader");
        for (int i = 0; i < channelsNumber; i++) {
            this.channels[i] = new SAWMultiplexedInputStream(this, i);
            this.pipedInputStreams[i] = new SAWPipedInputStream(channelBufferSize);
            this.pipedOutputStreams[i] = new SAWPipedOutputStream();
            this.pipedInputStreams[i].connect(pipedOutputStreams[i]);
        }
        this.packetReaderThread.setDaemon(true);
        this.packetReaderThread.setPriority(Thread.NORM_PRIORITY);
        if (startPacketReader) {
            this.packetReaderThread.start();
        }
    }

    public SAWMultiplexedInputStream getInputStream(int number) {
        return channels[number];
    }

    public void startPacketReader() {
        if (!packetReaderThread.isAlive()) {
            packetReaderThread.start();
        }
    }

    public void stopPacketReader() throws IOException, InterruptedException {
        close();
        packetReaderThread.join();
    }

    public int getChannelsNumber() {
        return channels.length;
    }

    private int readData(int number) throws IOException {
        return pipedInputStreams[number].read();
    }

    private int readData(byte[] data, int number) throws IOException {
        return pipedInputStreams[number].read(data);
    }

    private int readData(byte[] data, int offset, int length, int number) throws IOException {
        return pipedInputStreams[number].read(data, offset, length);
    }

    private int available(int number) throws IOException {
        return pipedInputStreams[number].available();
    }

    private long skip(long count, int number) throws IOException {
        return pipedInputStreams[number].skip(count);
    }

    public void open(int number) {
        pipedInputStreams[number].open();
    }

    public void close(int number) throws IOException {
        pipedOutputStreams[number].close();
    }

    public void close() throws IOException {
        packetReader.setRunning(false);
        for (int i = 0; i < channels.length; i++) {
            pipedOutputStreams[i].close();
            pipedInputStreams[i].close();
        }
        in.close();
    }

    private void readPacket() throws IOException {
        number = in.readUnsignedShort();
        if (number != -1) {
            length = in.readShort();
            if (length > 0) {
                copied = 0;
                readed = 0;
                remaining = length;
                while (copied < length) {
                    readed = in.read(packetBuffer, copied, remaining);
                    if (readed != -1) {
                        pipedOutputStreams[number].write(packetBuffer, copied, readed);
                        pipedOutputStreams[number].flush();
                        copied += readed;
                        remaining -= readed;
                    } else {
                        close();
                        break;
                    }
                }
            } else if (length == -1) {
                close();
            } else if (length == -2) {
                close(number);
            } else if (length == -3) {
                open(number);
            }
        } else {
            close();
        }
    }
}
