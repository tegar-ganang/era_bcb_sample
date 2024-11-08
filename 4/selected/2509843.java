package net.sf.copernicus.server.m2.transport;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import org.apache.log4j.Logger;

public abstract class Endpoint implements ConnectionHandler, Runnable {

    protected static Logger mLog = Logger.getLogger(SecureEndpoint.class.getPackage().getName());

    private static final int BUFFER_SIZE = 4096;

    private static final int SELECT_TIMEOUT_MILLIS = 10;

    protected String mHost;

    protected int mPort;

    protected ConnectionListener mListener;

    protected Thread mThread;

    protected StringBuffer mSendStringBuffer = new StringBuffer();

    protected volatile boolean mDisconnect = false;

    private int mIntId;

    private String mStringId;

    protected ByteBuffer mRecvBuf;

    protected ByteBuffer mSendBuf;

    protected SocketChannel mChannel;

    protected Selector mSelector;

    private Object mUserData;

    public Endpoint(String host, int port, ConnectionListener listener) {
        mHost = host;
        mPort = port;
        mListener = listener;
        mRecvBuf = ByteBuffer.allocate(BUFFER_SIZE);
        mSendBuf = ByteBuffer.allocate(BUFFER_SIZE);
        mSendBuf.position(BUFFER_SIZE);
    }

    public String getHostname() {
        return mHost;
    }

    public int getPortNumber() {
        return mPort;
    }

    public ConnectionListener getListener() {
        return mListener;
    }

    public SocketChannel getChannel() {
        return mChannel;
    }

    public Selector getSelector() {
        return mSelector;
    }

    public void disconnect() {
        mDisconnect = true;
    }

    public void interrupt() {
        if (mThread != null) mThread.interrupt();
    }

    public void send(String data) {
        mSendStringBuffer.append(data);
    }

    public void waitForDisconnect() throws InterruptedException {
        mThread.join();
    }

    public void setIntId(int mIntId) {
        this.mIntId = mIntId;
    }

    public int getIntId() {
        return mIntId;
    }

    public void setStringId(String mStringId) {
        this.mStringId = mStringId;
    }

    public String getStringId() {
        return mStringId;
    }

    public void setUserData(Object data) {
        mUserData = data;
    }

    public Object getUserData() {
        return mUserData;
    }

    public void run() {
        try {
            try {
                this.onInitializing();
                while (!Thread.interrupted() && this.onIteration()) {
                    mSelector.select(SELECT_TIMEOUT_MILLIS);
                    Iterator<SelectionKey> it = mSelector.selectedKeys().iterator();
                    while (it.hasNext()) {
                        SelectionKey key = it.next();
                        it.remove();
                        if (key.isValid()) {
                            this.onKey(key);
                            if (key.isReadable()) {
                                int rc = mChannel.read(mRecvBuf);
                                mLog.debug("Got " + rc + " bytes.");
                                if (rc > 0) {
                                    mRecvBuf.flip();
                                    this.onRead(mRecvBuf);
                                    mRecvBuf.clear();
                                } else if (rc < 0) {
                                    mListener.onConnectionBroken(this);
                                    mThread.interrupt();
                                    key.cancel();
                                    break;
                                }
                            }
                            if (key.isWritable()) {
                                if (this.onWritable()) {
                                    if (mSendBuf.hasRemaining()) {
                                        int rc = mChannel.write(mSendBuf);
                                        if (rc < 0) {
                                            mListener.onConnectionBroken(this);
                                            mThread.interrupt();
                                            key.cancel();
                                        }
                                    }
                                }
                            }
                        }
                        Thread.sleep(SELECT_TIMEOUT_MILLIS);
                    }
                }
            } finally {
                this.onTerminating();
            }
        } catch (InterruptedException e) {
        } catch (Exception e) {
            mListener.onError(this, e);
        }
    }

    protected boolean onIteration() {
        return !mDisconnect;
    }

    protected void onRead(ByteBuffer buf) throws Exception {
        mListener.onReadString(this, new String(buf.array(), 0, buf.limit()));
    }

    protected boolean onWritable() throws Exception {
        int ssb_len = mSendStringBuffer.length();
        if (ssb_len > 0 && !mSendBuf.hasRemaining()) {
            mSendBuf.clear();
            if (ssb_len <= BUFFER_SIZE / 2) {
                mSendBuf.put(mSendStringBuffer.toString().getBytes());
                mSendStringBuffer.setLength(0);
            } else {
                mSendBuf.put(mSendStringBuffer.substring(0, BUFFER_SIZE / 2).getBytes());
                mSendStringBuffer.delete(0, BUFFER_SIZE / 2);
            }
            mSendBuf.flip();
        }
        return true;
    }

    protected abstract void onInitializing() throws Exception;

    protected abstract void onKey(SelectionKey key) throws Exception;

    protected abstract void onTerminating() throws Exception;
}
