package phex.connection;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phex.common.Phex;
import phex.common.address.DestAddress;
import phex.common.bandwidth.BandwidthController;
import phex.download.PushHandler;
import phex.event.PhexEventService;
import phex.event.PhexEventTopics;
import phex.host.Host;
import phex.host.HostStatus;
import phex.host.NetworkHostsContainer;
import phex.http.HTTPMessageException;
import phex.http.HTTPProcessor;
import phex.http.HTTPRequest;
import phex.io.buffer.BufferCache;
import phex.msg.GUID;
import phex.net.connection.Connection;
import phex.net.repres.SocketFacade;
import phex.prefs.core.NetworkPrefs;
import phex.servent.Servent;
import phex.share.HttpRequestDispatcher;
import phex.utils.GnutellaInputStream;
import phex.utils.IOUtil;
import phex.utils.URLCodecUtils;

/**
 * If during negotiation it is clear that the remote
 * host has connected to obtain data via a GET request or to deliver data in
 * response to a push, then the worker delegates this on.
 */
public class IncomingConnectionDispatcher implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(IncomingConnectionDispatcher.class);

    public static final String GET_REQUEST_PREFIX = "GET ";

    public static final String HEAD_REQUEST_PREFIX = "HEAD ";

    public static final String GIV_REQUEST_PREFIX = "GIV ";

    public static final String CHAT_REQUEST_PREFIX = "CHAT ";

    public static final String URI_DOWNLOAD_PREFIX = "PHEX_URI ";

    public static final String MAGMA_DOWNLOAD_PREFIX = "PHEX_MAGMA ";

    public static final String RSS_DOWNLOAD_PREFIX = "PHEX_RSS ";

    private final Servent servent;

    private final SocketFacade socket;

    public IncomingConnectionDispatcher(SocketFacade socket, Servent servent) {
        this.socket = socket;
        this.servent = servent;
    }

    public void run() {
        GnutellaInputStream gInStream = null;
        try {
            socket.setSoTimeout(NetworkPrefs.TcpRWTimeout.get().intValue());
            BandwidthController bwController = servent.getBandwidthService().getNetworkBandwidthController();
            Connection connection = new Connection(socket, bwController);
            String requestLine = connection.readLine();
            if (requestLine == null) {
                throw new IOException("Disconnected from remote host during handshake");
            }
            logger.debug("ConnectionRequest " + requestLine);
            DestAddress localAddress = servent.getLocalAddress();
            String greeting = servent.getGnutellaNetwork().getNetworkGreeting();
            if (requestLine.startsWith(greeting + "/")) {
                handleGnutellaRequest(connection);
            } else if (requestLine.startsWith(GET_REQUEST_PREFIX) || requestLine.startsWith(HEAD_REQUEST_PREFIX)) {
                if (!servent.getOnlineStatus().isTransfersOnline() && !socket.getRemoteAddress().isLocalHost(localAddress)) {
                    throw new IOException("Transfers not connected.");
                }
                HTTPRequest httpRequest = HTTPProcessor.parseHTTPRequest(requestLine);
                HTTPProcessor.parseHTTPHeaders(httpRequest, connection);
                logger.debug(httpRequest.getRequestMethod() + " Request: " + httpRequest.buildHTTPRequestString());
                if (httpRequest.isGnutellaRequest()) {
                    servent.getUploadService().handleUploadRequest(connection, httpRequest);
                } else {
                    new HttpRequestDispatcher().httpRequestHandler(connection, httpRequest);
                }
            } else if (requestLine.startsWith(GIV_REQUEST_PREFIX)) {
                if (!servent.getOnlineStatus().isTransfersOnline() && !socket.getRemoteAddress().isLocalHost(localAddress)) {
                    throw new IOException("Transfers not connected.");
                }
                handleIncommingGIV(requestLine);
            } else if (requestLine.startsWith(CHAT_REQUEST_PREFIX)) {
                if (!servent.getOnlineStatus().isNetworkOnline() && !socket.getRemoteAddress().isLocalHost(localAddress)) {
                    throw new IOException("Network not connected.");
                }
                DestAddress address = socket.getRemoteAddress();
                logger.debug("Chat request from: " + address);
                servent.getChatService().acceptChat(connection);
            } else if (requestLine.startsWith(URI_DOWNLOAD_PREFIX)) {
                handleIncommingUriDownload(requestLine);
            } else if (requestLine.startsWith(MAGMA_DOWNLOAD_PREFIX)) {
                handleIncommingMagmaDownload(requestLine);
            } else if (requestLine.startsWith(RSS_DOWNLOAD_PREFIX)) {
                handleIncommingRSSDownload(requestLine);
            } else {
                throw new IOException("Unknown connection request: " + requestLine);
            }
        } catch (HTTPMessageException exp) {
            logger.debug(exp.toString(), exp);
            IOUtil.closeQuietly(gInStream);
            IOUtil.closeQuietly(socket);
        } catch (IOException exp) {
            logger.debug(exp.toString(), exp);
            IOUtil.closeQuietly(gInStream);
            IOUtil.closeQuietly(socket);
        } catch (Exception exp) {
            logger.error(exp.toString(), exp);
            IOUtil.closeQuietly(gInStream);
            IOUtil.closeQuietly(socket);
        }
    }

    private void handleGnutellaRequest(Connection connection) throws IOException {
        DestAddress localAddress = servent.getLocalAddress();
        if (!servent.getOnlineStatus().isNetworkOnline() && !socket.getRemoteAddress().isLocalHost(localAddress)) {
            throw new IOException("Network not connected.");
        }
        DestAddress address = socket.getRemoteAddress();
        NetworkHostsContainer netHostsCont = servent.getHostService().getNetworkHostsContainer();
        Host host = netHostsCont.createIncomingHost(address, connection);
        host.setStatus(HostStatus.ACCEPTING, "");
        try {
            ConnectionEngine engine = new ConnectionEngine(servent, host);
            engine.initHostHandshake();
            engine.processIncomingData();
        } catch (IOException exp) {
            if (host.isConnected()) {
                host.setStatus(HostStatus.ERROR, exp.getMessage());
                host.disconnect();
            }
            throw exp;
        } finally {
            if (host.isConnected()) {
                host.setStatus(HostStatus.DISCONNECTED, "Unknown");
                host.disconnect();
            }
        }
    }

    /**
     * @param requestLine
     * @throws IOException
     */
    private void handleIncommingUriDownload(String requestLine) throws IOException {
        try {
            DestAddress localAddress = servent.getLocalAddress();
            if (!socket.getRemoteAddress().isLocalHost(localAddress)) {
                return;
            }
            socket.getChannel().write(BufferCache.OK_BUFFER);
        } finally {
            IOUtil.closeQuietly(socket);
        }
        String uriToken = requestLine.substring(URI_DOWNLOAD_PREFIX.length() + 1);
        PhexEventService eventService = Phex.getEventService();
        eventService.publish(PhexEventTopics.Incoming_Uri, uriToken);
    }

    /**
     * @param requestLine
     * @throws IOException
     */
    private void handleIncommingMagmaDownload(String requestLine) throws IOException {
        try {
            DestAddress localAddress = servent.getLocalAddress();
            if (!socket.getRemoteAddress().isLocalHost(localAddress)) {
                return;
            }
            socket.getChannel().write(BufferCache.OK_BUFFER);
        } finally {
            IOUtil.closeQuietly(socket);
        }
        String fileNameToken = requestLine.substring(MAGMA_DOWNLOAD_PREFIX.length() + 1);
        PhexEventService eventService = Phex.getEventService();
        eventService.publish(PhexEventTopics.Incoming_Magma, fileNameToken);
    }

    private void handleIncommingRSSDownload(String requestLine) throws IOException {
        try {
            DestAddress localAddress = servent.getLocalAddress();
            if (!socket.getRemoteAddress().isLocalHost(localAddress)) {
                return;
            }
            socket.getChannel().write(BufferCache.OK_BUFFER);
        } finally {
            IOUtil.closeQuietly(socket);
        }
        String fileNameToken = requestLine.substring(RSS_DOWNLOAD_PREFIX.length() + 1);
        PhexEventService eventService = Phex.getEventService();
        eventService.publish(PhexEventTopics.Incoming_Magma, fileNameToken);
    }

    private void handleIncommingGIV(String requestLine) {
        String remainder = requestLine.substring(4);
        try {
            int fileNumIdx = remainder.indexOf(':');
            int guidIdx = remainder.indexOf('/', fileNumIdx);
            String guidStr = remainder.substring(fileNumIdx + 1, guidIdx);
            String givenFileName = remainder.substring(guidIdx + 1);
            givenFileName = URLCodecUtils.decodeURL(givenFileName);
            GUID givenGUID = new GUID(guidStr);
            PushHandler.handleIncommingGIV(socket, givenGUID, givenFileName);
        } catch (IndexOutOfBoundsException exp) {
            logger.error("Failed to parse GIV: " + requestLine, exp);
        }
    }
}
