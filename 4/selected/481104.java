package net.sf.alc.connection.tcp;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import net.sf.alc.connection.ConnectionException;
import net.sf.alc.connection.tcp.protocolencoder.ProtocolEncoderFactory;

public class TcpConnection extends AbstractSocketConnection {

    private static final int DEFULT_CAPACITY = 1024;

    /**
    * The socket where read and write operations are performed.
    */
    private final SocketChannel mSocketChannel;

    private final InetAddress mRemoteAddress;

    private final int mRemotePort;

    private final InetAddress mLocalAddress;

    private final int mLocalPort;

    /**
    * Used for reading from the socket.
    */
    private final ByteBuffer mInBuffer;

    /**
    * Current dataHandler whose message is being sent.
    */
    private DataHandler mDataHandler = null;

    private final SelectorHandler mSelectorHandler;

    /**
    * Creates and initializes a new instance.
    * 
    * @param aContext
    *           Socket to be wrapped.
    * @param aDecoderFactory
    * @param aEncoderFactory
    * @throws IOException
    */
    public TcpConnection(TcpConnectionContext aContext, ProtocolDecoderFactory aDecoderFactory, ProtocolEncoderFactory aEncoderFactory) throws IOException {
        super(aContext, aDecoderFactory, aEncoderFactory);
        mSelectorHandler = new Handler();
        mSocketChannel = aContext.getChannel();
        if (DEFULT_CAPACITY > 0) {
            mInBuffer = ByteBuffer.allocate(DEFULT_CAPACITY);
        } else {
            mInBuffer = ByteBuffer.allocate(mSocketChannel.socket().getReceiveBufferSize());
        }
        final Socket sock = mSocketChannel.socket();
        mRemoteAddress = sock.getInetAddress();
        mRemotePort = sock.getPort();
        mLocalAddress = sock.getLocalAddress();
        mLocalPort = sock.getLocalPort();
    }

    protected final Socket getSocket() {
        return mSocketChannel.socket();
    }

    protected final InetAddress getLocalAddress() {
        return mLocalAddress;
    }

    protected final int getLocalPort() {
        return mLocalPort;
    }

    protected final InetAddress getRemoteAddress() {
        return mRemoteAddress;
    }

    protected final int getRemotePort() {
        return mRemotePort;
    }

    /**
    * Registers the connection with its Reactor. Does not activate reading.
    * <code>activateReading</code> must be invoked explicitely.
    * 
    * @throws ConnectionException
    */
    public final void open() throws ConnectionException {
        try {
            getReactor().registerChannelNow(mSocketChannel, 0, mSelectorHandler);
        } catch (IOException e) {
            throw new ConnectionException(e);
        }
    }

    public final void close() {
        getReactor().closeChannelNow(mSocketChannel);
    }

    /**
    * Enables interest in reading.
    */
    public final void activateReading() {
        getReactor().addChannelInterestLater(mSocketChannel, SelectionKey.OP_READ, null);
    }

    protected final void triggerWrite(DataHandler aDataHandler) {
        mDataHandler = aDataHandler;
        requestWrite();
    }

    protected void fireEndOfInputData() {
        try {
            getReactor().removeChannelInterestLater(mSocketChannel, SelectionKey.OP_READ, null);
        } catch (CancelledKeyException ex) {
        }
        super.fireEndOfInputData();
    }

    /**
    * Activates interest in writing.
    */
    private void requestWrite() {
        getReactor().addChannelInterestLater(mSocketChannel, SelectionKey.OP_WRITE, null);
    }

    public void setSecure(boolean aIsClient) throws ConnectionException {
        throw new UnsupportedOperationException("Cannot be made secure.");
    }

    public boolean isSecure() {
        return false;
    }

    private class Handler extends SelectorHandlerAdapter {

        /**
       * Writes to the underlying channel. Non-blocking.
       */
        public void handleWrite() {
            try {
                final ByteBuffer buffer = mDataHandler.buffer();
                mSocketChannel.write(buffer);
                if (buffer.hasRemaining()) {
                    requestWrite();
                } else {
                    final DataHandler nextData = doneWriting(mDataHandler);
                    if (nextData != null) {
                        mDataHandler = nextData;
                        requestWrite();
                    }
                }
            } catch (RuntimeException e) {
                writeException(e);
            } catch (InterruptedIOException e) {
                writeException(e);
                Thread.currentThread().interrupt();
            } catch (ClosedChannelException ex) {
            } catch (IOException e) {
                writeException(e);
            }
        }

        /**
       * Reads from the socket into the internal buffer.
       */
        public void handleRead() {
            try {
                final int readBytes = mSocketChannel.read(mInBuffer);
                if (readBytes == -1) {
                    processNewData(null);
                    return;
                }
                if (readBytes == 0) {
                    activateReading();
                    return;
                }
                mInBuffer.flip();
                processNewData(mInBuffer);
            } catch (RuntimeException e) {
                readException(e);
            } catch (InterruptedIOException e) {
                readException(e);
                Thread.currentThread().interrupt();
            } catch (ClosedChannelException ex) {
            } catch (IOException e) {
                readException(e);
            }
        }

        /**
       * Called when the associated socket is closed.
       */
        public void handleClosed() {
            fireConnectionClosed();
        }
    }
}
