package com.flazr;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import org.apache.mina.common.ByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.flazr.Packet.Type;

public class FlvWriter implements OutputWriter {

    private static final Logger logger = LoggerFactory.getLogger(FlvWriter.class);

    private ByteBuffer out;

    private FileChannel channel;

    private FileOutputStream fos;

    private WriterStatus status;

    public FlvWriter(int seekTime, String fileName) {
        status = new WriterStatus(seekTime);
        try {
            File file = new File(fileName);
            fos = new FileOutputStream(file);
            channel = fos.getChannel();
            logger.info("opened file for writing: " + file.getAbsolutePath());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        out = ByteBuffer.allocate(1024);
        out.setAutoExpand(true);
        writeHeader();
    }

    public void close() {
        try {
            channel.close();
            fos.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        status.logFinalVideoDuration();
    }

    private void writeHeader() {
        out.put((byte) 0x46);
        out.put((byte) 0x4C);
        out.put((byte) 0x56);
        out.put((byte) 0x01);
        out.put((byte) 0x05);
        out.putInt(0x09);
        out.putInt(0);
        out.flip();
        write(out);
    }

    public synchronized void write(Packet packet) {
        Header header = packet.getHeader();
        int time = status.getChannelAbsoluteTime(header);
        write(header.getPacketType(), packet.getData(), time);
    }

    public synchronized void writeFlvData(ByteBuffer data) {
        while (data.hasRemaining()) {
            Type packetType = Type.parseByte(data.get());
            int size = Utils.readInt24(data);
            int timestamp = Utils.readInt24(data);
            status.updateVideoChannelTime(timestamp);
            data.getInt();
            byte[] bytes = new byte[size];
            data.get(bytes);
            ByteBuffer temp = ByteBuffer.wrap(bytes);
            write(packetType, temp, timestamp);
            data.getInt();
        }
    }

    public synchronized void write(Type packetType, ByteBuffer data, final int time) {
        if (logger.isDebugEnabled()) {
            logger.debug("writing FLV tag {} t{} {}", new Object[] { packetType, time, data });
        }
        out.clear();
        out.put(packetType.byteValue());
        final int size = data.limit();
        Utils.writeInt24(out, size);
        Utils.writeInt24(out, time);
        out.putInt(0);
        out.flip();
        write(out);
        write(data);
        out.clear();
        out.putInt(size + 11);
        out.flip();
        write(out);
    }

    private void write(ByteBuffer buffer) {
        try {
            channel.write(buffer.buf());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
