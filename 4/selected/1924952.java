package org.opennfc.service.communication;

import org.opennfc.HelperForNfc;
import org.opennfc.NfcException;
import org.opennfc.NfcManager;
import org.opennfc.p2p.P2PConnectionLess;
import org.opennfc.p2p.P2PManager;
import android.nfc.ErrorCodes;
import android.nfc.ILlcpConnectionlessSocket;
import android.nfc.LlcpPacket;
import android.os.RemoteException;
import android.util.Log;
import android.util.SparseArray;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Connection less socket LLCP manager implementation
 */
public class ConnectionlessSocketLLCP extends ILlcpConnectionlessSocket.Stub {

    /** Connection less socket LLCP manager singleton */
    public static final ConnectionlessSocketLLCP CONNECTION;

    /** Enable/disable debug */
    private static final boolean DEBUG = true;

    /** Linked P2P manager */
    private static P2PManager p2pManager;

    /** Tag use in debug */
    private static final String TAG = ConnectionlessSocketLLCP.class.getSimpleName();

    static {
        CONNECTION = new ConnectionlessSocketLLCP();
    }

    /**
     * Obtain linked P2P manager
     * 
     * @return P2P manager
     * @throws NfcException On obtain issue
     */
    private static P2PManager getP2PManager() throws NfcException {
        if (ConnectionlessSocketLLCP.p2pManager == null) {
            ConnectionlessSocketLLCP.p2pManager = NfcManager.getInstance().getP2PManager();
        }
        return ConnectionlessSocketLLCP.p2pManager;
    }

    /** Connections cache */
    private final SparseArray<P2PConnectionLess> connections;

    /** Next handle to use */
    private int nextHandle;

    /**
     * Create the connection less socket LLCP manager singleton
     */
    private ConnectionlessSocketLLCP() {
        this.connections = new SparseArray<P2PConnectionLess>(16);
    }

    /**
     * Close a connection less socket <br>
     * <br>
     * <u><b>Documentation from parent :</b></u><br> {@inheritDoc}
     * 
     * @param nativeHandle Connection less socket handle to close
     * @throws RemoteException On AIDL link broken
     * @see android.nfc.ILlcpConnectionlessSocket#close(int)
     */
    @Override
    public void close(final int nativeHandle) throws RemoteException {
        final P2PConnectionLess connectionLess = this.connections.get(nativeHandle);
        if (connectionLess == null) {
            return;
        }
        try {
            connectionLess.close();
        } catch (final IOException exception) {
            if (ConnectionlessSocketLLCP.DEBUG) {
                Log.d(ConnectionlessSocketLLCP.TAG, "Closing", exception);
            }
        }
    }

    /**
     * Create a connection less socket
     * 
     * @param uri URI to connect with
     * @param sap SAP to use
     * @return Connection less socket handle
     * @throws NfcException On creating issue
     */
    public int createConnection(final String uri, final int sap) throws NfcException {
        if (uri == null && sap == 0) {
            if (ConnectionlessSocketLLCP.DEBUG) {
                Log.d(ConnectionlessSocketLLCP.TAG, "URI is null and SAP 0 in same time !");
            }
            return ErrorCodes.ERROR_INVALID_PARAM;
        }
        int handle = -1;
        synchronized (this.connections) {
            handle = this.nextHandle++;
            this.connections.put(handle, ConnectionlessSocketLLCP.getP2PManager().createP2PConnectionLess(uri, (byte) (sap & 0xFF)));
        }
        return handle;
    }

    /**
     * Connected SAP of a connection less socket <br>
     * <br>
     * <u><b>Documentation from parent :</b></u><br> {@inheritDoc}
     * 
     * @param nativeHandle Connection less socket handle
     * @return Associated SAP OR {@link ErrorCodes#ERROR_SOCKET_NOT_CONNECTED}
     *         if socket not connected OR {@link ErrorCodes#ERROR_IO} on
     *         Input/Output issue
     * @throws RemoteException On AIDL link broken
     * @see android.nfc.ILlcpConnectionlessSocket#getSap(int)
     */
    @Override
    public int getSap(final int nativeHandle) throws RemoteException {
        final P2PConnectionLess connectionLess = this.connections.get(nativeHandle);
        if (connectionLess == null) {
            return ErrorCodes.ERROR_SOCKET_NOT_CONNECTED;
        }
        try {
            return connectionLess.getLocalSap() & 0xFF;
        } catch (final IOException exception) {
            if (ConnectionlessSocketLLCP.DEBUG) {
                Log.d(ConnectionlessSocketLLCP.TAG, "get SAP", exception);
            }
            return ErrorCodes.ERROR_IO;
        } catch (final NfcException exception) {
            if (ConnectionlessSocketLLCP.DEBUG) {
                Log.d(ConnectionlessSocketLLCP.TAG, "get SAP", exception);
            }
            return HelperForNfc.obtainErrorCode(exception, ErrorCodes.ERROR_SOCKET_NOT_CONNECTED);
        }
    }

    /**
     * Read packet from a connection less socket.<br>
     * It block until a packet read or an issue happen <br>
     * <br>
     * <u><b>Documentation from parent :</b></u><br> {@inheritDoc}
     * 
     * @param nativeHandle Connection less socket handle
     * @return Read packet or {@code null} on reading issue
     * @throws RemoteException On AIDL broken link
     * @see android.nfc.ILlcpConnectionlessSocket#receiveFrom(int)
     */
    @Override
    public LlcpPacket receiveFrom(final int nativeHandle) throws RemoteException {
        final P2PConnectionLess connectionLess = this.connections.get(nativeHandle);
        if (connectionLess == null) {
            return null;
        }
        final byte[] buffer = new byte[1024];
        final ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream(1024);
        try {
            int read = connectionLess.recvFrom(buffer);
            int last = read;
            while (read >= 0) {
                last = read;
                arrayOutputStream.write(buffer, 0, read);
                read = connectionLess.recvFrom(buffer);
            }
            return new LlcpPacket(buffer[last] & 0xFF, arrayOutputStream.toByteArray());
        } catch (final IOException exception) {
            if (ConnectionlessSocketLLCP.DEBUG) {
                Log.d(ConnectionlessSocketLLCP.TAG, "receive", exception);
            }
            return null;
        }
    }

    /**
     * Send a packet to a connection less socket <br>
     * <br>
     * <u><b>Documentation from parent :</b></u><br> {@inheritDoc}
     * 
     * @param nativeHandle Connection less socket handle to send packet
     * @param packet Packet to send
     * @return {@link ErrorCodes#SUCCESS} on sending success OR
     *         {@link ErrorCodes#ERROR_SOCKET_NOT_CONNECTED} if connection less
     *         socket not connected OR {@link ErrorCodes#ERROR_IO} on writing
     *         issue
     * @throws RemoteException On AIDL broken link
     * @see android.nfc.ILlcpConnectionlessSocket#sendTo(int,
     *      android.nfc.LlcpPacket)
     */
    @Override
    public int sendTo(final int nativeHandle, final LlcpPacket packet) throws RemoteException {
        final P2PConnectionLess connectionLess = this.connections.get(nativeHandle);
        if (connectionLess == null) {
            return ErrorCodes.ERROR_SOCKET_NOT_CONNECTED;
        }
        try {
            connectionLess.sendTo((byte) (packet.getRemoteSap() & 0xFF), packet.getDataBuffer());
        } catch (final IOException exception) {
            if (ConnectionlessSocketLLCP.DEBUG) {
                Log.d(ConnectionlessSocketLLCP.TAG, "send", exception);
            }
            return ErrorCodes.ERROR_IO;
        }
        return ErrorCodes.SUCCESS;
    }
}
