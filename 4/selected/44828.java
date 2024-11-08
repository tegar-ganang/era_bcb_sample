package com.flazr;

import org.apache.mina.common.ByteBuffer;

public class DummyWriter implements OutputWriter {

    private WriterStatus status;

    public DummyWriter(int seekTime) {
        status = new WriterStatus(seekTime);
    }

    public void close() {
        status.logFinalVideoDuration();
    }

    public synchronized void write(Packet packet) {
        status.getChannelAbsoluteTime(packet.getHeader());
    }

    public synchronized void writeFlvData(ByteBuffer data) {
        while (data.hasRemaining()) {
            data.get();
            int size = Utils.readInt24(data);
            int timestamp = Utils.readInt24(data);
            status.updateVideoChannelTime(timestamp);
            data.position(data.position() + 4 + size + 4);
        }
    }
}
