package com.meidusa.amoeba.net;

import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * ���ConnectoinManager�������������������������������.
 * 
 * @author struct
 *
 */
public abstract class AbstractConnectionFactory implements ConnectionFactory {

    private int sendBufferSize = 64;

    private int receiveBufferSize = 64;

    private boolean tcpNoDelay = true;

    private boolean keepAlive = true;

    /**
	 * ����һ������,��ʼ������,ע�ᵽ���ӹ�����,
	 * 
	 * @return Connection ���ظ�����ʵ��
	 */
    public Connection createConnection(SocketChannel channel, long createStamp) throws IOException {
        Connection connection = (Connection) newConnectionInstance(channel, System.currentTimeMillis());
        initConnection(connection);
        return connection;
    }

    /**
	 * �����Ժ�,�������´�����������һЩ��ʼ��
	 * @param connection
	 */
    protected void initConnection(Connection connection) throws IOException {
        connection.getChannel().socket().setSendBufferSize(sendBufferSize * 1024);
        connection.getChannel().socket().setReceiveBufferSize(receiveBufferSize * 1024);
        connection.getChannel().socket().setTcpNoDelay(tcpNoDelay);
        connection.getChannel().socket().setKeepAlive(keepAlive);
    }

    public int getSendBufferSize() {
        return sendBufferSize;
    }

    public void setSendBufferSize(int sendBufferSize) {
        this.sendBufferSize = sendBufferSize;
    }

    public int getReceiveBufferSize() {
        return receiveBufferSize;
    }

    public void setReceiveBufferSize(int receiveBufferSize) {
        this.receiveBufferSize = receiveBufferSize;
    }

    public boolean isTcpNoDelay() {
        return tcpNoDelay;
    }

    public void setTcpNoDelay(boolean tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    /**
	 * ��������ʵ��
	 * @param channel
	 * @param createStamp
	 * @return
	 */
    protected abstract Connection newConnectionInstance(SocketChannel channel, long createStamp);
}
