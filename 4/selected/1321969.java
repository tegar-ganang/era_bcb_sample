package intf.net.item;

import intf.net.item.thread.TcpClientThread;

public class TcpClient extends Tcp {

    private TcpClientThread clientThread;

    public TcpClient(String address, int parseInt) {
        clientThread = new TcpClientThread(address, 1234);
        clientThread.start();
    }

    @Override
    public void close() {
        try {
            clientThread.kill();
            clientThread.join();
            clientThread = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean exists() {
        return clientThread.isConnected();
    }

    @Override
    public boolean isInputShutdown() {
        return clientThread.isInputShutdown();
    }

    @Override
    public boolean isOutputShutdown() {
        return clientThread.isOutputShutdown();
    }

    @Override
    public void writeByte(Byte b) {
        clientThread.writeByte(b);
    }

    @Override
    public Byte readByte() {
        return clientThread.readByte();
    }

    @Override
    public void setOption(String name, String value) {
        clientThread.setOption(name, value);
    }

    @Override
    public String getOption(String name) {
        return clientThread.getOption(name);
    }
}
