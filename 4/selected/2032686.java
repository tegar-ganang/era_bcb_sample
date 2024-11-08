package com.jot.user;

import java.nio.ByteBuffer;

/**
 * for testing
 * 
 * @author alanwootton
 * 
 */
public class MockSocket extends SocketIntf {

    public ByteBuffer writeBuffer;

    static final int buffSize = 4000;

    public MockSocket() {
        readBuffer = ByteBuffer.allocate(buffSize);
        writeBuffer = ByteBuffer.allocate(buffSize);
    }

    public MockSocket(Globals g) {
        this();
        this.g = g;
    }

    @Override
    public String getName() {
        return "mock";
    }

    @Override
    public void write(byte[] bytes) throws Exception {
        writeBuffer.put(bytes);
    }

    @Override
    public void write(ByteBuffer buf) throws Exception {
        buf.flip();
        writeBuffer.put(buf);
    }

    public void transmit() {
        if (writeBuffer.limit() == buffSize) writeBuffer.flip();
        if (readBuffer.limit() != buffSize) readBuffer.compact();
        readBuffer.put(writeBuffer);
        writeBuffer.clear();
        readBuffer.flip();
    }

    @Override
    public void stopRunning() {
    }

    @Override
    public boolean isRunning() {
        return true;
    }

    @Override
    public String getIpAddress() {
        return "mock";
    }
}
