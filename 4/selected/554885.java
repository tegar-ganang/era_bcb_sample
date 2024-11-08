package com.jkgh.remedium.tools;

import com.jkgh.asin.ChannelHandler;

public abstract class BlockingChannelHandler implements ChannelHandler {

    private SocketThread socketThread;

    public void registerSocketThread(SocketThread socketThread) {
        this.socketThread = socketThread;
    }

    @Override
    public void disconnectAfterAllWrites() {
        disconnect();
    }

    @Override
    public void disconnect() {
        socketThread.disconnect();
    }

    @Override
    public void write(byte[] data) {
        socketThread.write(data);
    }
}
