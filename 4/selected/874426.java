package intf.net.item;

import intf.net.item.thread.TcpServerThread;

public class TcpServer extends Tcp {

    private TcpServerThread serverThread;

    public TcpServer(Integer parseInt) {
        serverThread = new TcpServerThread(1234);
        serverThread.start();
    }

    @Override
    public void close() {
        try {
            serverThread.kill();
            serverThread.join();
            serverThread = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean exists() {
        return serverThread.isConnected();
    }

    @Override
    public boolean isInputShutdown() {
        return serverThread.isInputShutdown();
    }

    @Override
    public boolean isOutputShutdown() {
        return serverThread.isOutputShutdown();
    }

    @Override
    public void writeByte(Byte b) {
        serverThread.writeByte(b);
    }

    @Override
    public Byte readByte() {
        return serverThread.readByte();
    }

    @Override
    public void setOption(String name, String value) {
        serverThread.setOption(name, value);
    }

    @Override
    public String getOption(String name) {
        return serverThread.getOption(name);
    }
}
