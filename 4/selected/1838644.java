package org.gudy.azureus2.pluginsimpl.local.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.gudy.azureus2.plugins.messaging.MessageStreamDecoder;
import org.gudy.azureus2.plugins.messaging.MessageStreamEncoder;
import org.gudy.azureus2.plugins.network.Connection;
import org.gudy.azureus2.plugins.network.ConnectionManager;
import org.gudy.azureus2.plugins.network.Transport;
import org.gudy.azureus2.plugins.network.TransportCipher;
import org.gudy.azureus2.plugins.network.TransportException;
import org.gudy.azureus2.plugins.network.TransportFilter;
import org.gudy.azureus2.pluginsimpl.local.messaging.MessageStreamDecoderAdapter;
import org.gudy.azureus2.pluginsimpl.local.messaging.MessageStreamEncoderAdapter;
import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.networkmanager.ConnectionEndpoint;
import com.aelitis.azureus.core.networkmanager.NetworkManager;
import com.aelitis.azureus.core.networkmanager.impl.TransportHelper;
import com.aelitis.azureus.core.networkmanager.impl.TransportHelperFilter;
import com.aelitis.azureus.core.networkmanager.impl.udp.UDPNetworkManager;
import com.aelitis.azureus.core.networkmanager.impl.TransportHelperFilterStreamCipher;
import com.aelitis.azureus.core.networkmanager.impl.tcp.ProtocolEndpointTCP;
import com.aelitis.azureus.core.networkmanager.impl.tcp.TCPTransportHelper;
import com.aelitis.azureus.core.networkmanager.impl.tcp.TCPTransportImpl;
import com.aelitis.azureus.core.networkmanager.impl.udp.UDPTransport;
import com.aelitis.azureus.core.networkmanager.impl.udp.UDPTransportHelper;

/**
 *
 */
public class ConnectionManagerImpl implements ConnectionManager {

    private static ConnectionManagerImpl instance;

    public static synchronized ConnectionManagerImpl getSingleton(AzureusCore core) {
        if (instance == null) {
            instance = new ConnectionManagerImpl(core);
        }
        return (instance);
    }

    private AzureusCore azureus_core;

    private ConnectionManagerImpl(AzureusCore _core) {
        azureus_core = _core;
    }

    public Connection createConnection(InetSocketAddress remote_address, MessageStreamEncoder encoder, MessageStreamDecoder decoder) {
        ConnectionEndpoint connection_endpoint = new ConnectionEndpoint(remote_address);
        connection_endpoint.addProtocol(new ProtocolEndpointTCP(remote_address));
        com.aelitis.azureus.core.networkmanager.NetworkConnection core_conn = NetworkManager.getSingleton().createConnection(connection_endpoint, new MessageStreamEncoderAdapter(encoder), new MessageStreamDecoderAdapter(decoder), false, false, null);
        return new ConnectionImpl(core_conn, false);
    }

    public int getNATStatus() {
        return (azureus_core.getGlobalManager().getNATStatus());
    }

    public TransportCipher createTransportCipher(String algorithm, int mode, SecretKeySpec key_spec, AlgorithmParameterSpec params) throws TransportException {
        try {
            com.aelitis.azureus.core.networkmanager.impl.TransportCipher cipher = new com.aelitis.azureus.core.networkmanager.impl.TransportCipher(algorithm, mode, key_spec, params);
            return new TransportCipherImpl(cipher);
        } catch (Exception e) {
            throw new TransportException(e);
        }
    }

    public TransportFilter createTransportFilter(Connection connection, TransportCipher read_cipher, TransportCipher write_cipher) throws TransportException {
        Transport transport = connection.getTransport();
        com.aelitis.azureus.core.networkmanager.Transport core_transport;
        try {
            core_transport = ((TransportImpl) transport).coreTransport();
        } catch (IOException e) {
            throw new TransportException(e);
        }
        TransportHelper helper;
        if (core_transport instanceof TCPTransportImpl) {
            TransportHelperFilter hfilter = ((TCPTransportImpl) core_transport).getFilter();
            if (hfilter != null) {
                helper = hfilter.getHelper();
            } else {
                helper = new TCPTransportHelper(((TCPTransportImpl) (core_transport)).getSocketChannel());
            }
        } else if (core_transport instanceof UDPTransport) {
            TransportHelperFilter hfilter = ((UDPTransport) core_transport).getFilter();
            if (hfilter != null) {
                helper = hfilter.getHelper();
            } else {
                helper = ((UDPTransport) core_transport).getFilter().getHelper();
                InetSocketAddress addr = core_transport.getTransportEndpoint().getProtocolEndpoint().getConnectionEndpoint().getNotionalAddress();
                if (!connection.isIncoming()) {
                    try {
                        helper = new UDPTransportHelper(UDPNetworkManager.getSingleton().getConnectionManager(), addr, (UDPTransport) core_transport);
                    } catch (IOException ioe) {
                        throw new TransportException(ioe);
                    }
                } else {
                    throw new TransportException("udp incoming transport type not supported - " + core_transport);
                }
            }
        } else {
            throw new TransportException("transport type not supported - " + core_transport);
        }
        TransportHelperFilterStreamCipher core_filter = new TransportHelperFilterStreamCipher(helper, ((TransportCipherImpl) read_cipher).cipher, ((TransportCipherImpl) write_cipher).cipher);
        return new TransportFilterImpl(core_filter);
    }
}
