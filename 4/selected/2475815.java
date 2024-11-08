package org.jmule.core.protocol.donkey;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.logging.Level;
import org.jmule.core.ConnectionManager;
import org.jmule.core.SearchManager;
import org.jmule.core.SearchQuery;
import org.jmule.core.SearchResult;
import org.jmule.core.SharedFile;
import org.jmule.core.SharesManager;
import org.jmule.util.Convert;

/** Connection object which handles ed2k server connections
 * @author casper
 * @version $Revision: 1.1.1.1 $
 * <br>Last changed by $Author: jmartinc $ on $Date: 2005/04/22 21:44:39 $
 */
public class DonkeyServerConnection extends DonkeyConnectionSkeleton implements DonkeyPacketConstants {

    static final Logger log = Logger.getLogger(DonkeyServerConnection.class.getName());

    public DonkeyServerConnection(DonkeyServer ds) {
        super();
        setOutbound(true);
        this.donkeyServer = ds;
        addOutPacket(DonkeyPacketFactory.hello());
        addOutPacket(DonkeyPacketFactory.command(OP_GETSERVERLIST));
        lastServerListrequesttime = lastServerListreceivetime = System.currentTimeMillis();
        dContext.setServerConnection(this);
        state = DonkeyProtocol.SERVER_CONNECTING;
        setConnectTimeOut((int) dContext.getServerConnectTimeOut());
        log.info("Connecting to server (" + ds.getSocketAddress() + ")  " + ds.getName());
        setPeerAddress(ds.getSocketAddress());
        super.packetDecoder = new DonkeyPacketReceiver(this, new DonkeyClientExtension[] { new EmuleDecompressor(this) });
    }

    protected int getTimeOut() {
        return 600;
    }

    public DonkeyServer getServer() {
        return donkeyServer;
    }

    public synchronized void processIncomingPacket() {
        if (hasInput()) {
            try {
                donkeyServer.setAlive();
                state = DonkeyProtocol.SERVER_CONNECTED;
                inPacket = new DonkeyScannedPacket(getNextPacket());
                byte[] fileHash = null;
                SharesManager shares;
                SharedFile sf = null;
                Vector sharedFiles;
                LinkedList blockList = null;
                int command = Convert.byteToInt(inPacket.getCommandId());
                switch(command) {
                    case OP_SERVERMESSAGE:
                        {
                            String message = inPacket.getServerMessage();
                            log.info(getConnectionNumber() + " Server Message: \"" + message + "\"");
                            if (!donkeyServer.isStaticDns()) {
                                donkeyServer.testForDns(message);
                            }
                            break;
                        }
                    case OP_SERVERSTATUS:
                        {
                            log.info(getConnectionNumber() + " Server status: " + inPacket.getStatus()[0] + " Users online, sharing " + inPacket.getStatus()[1] + " files...");
                            donkeyServer.setUsers((int) inPacket.getStatus()[0]);
                            donkeyServer.setFiles((int) inPacket.getStatus()[1]);
                            lastStatusMessage = System.currentTimeMillis();
                            break;
                        }
                    case OP_SERVERLIST:
                        {
                            log.fine(getConnectionNumber() + "  Server list received.");
                            Collection additionalServerList = inPacket.getServerlist();
                            if (additionalServerList != null) {
                                dContext.getServerList().addAll(additionalServerList);
                                lastServerListreceivetime = System.currentTimeMillis();
                                if (dContext.isDeadServerRemovingEnabled()) {
                                    log.fine(getConnectionNumber() + " prune list");
                                    int a = dContext.getServerList().pruneList(3600000L);
                                    log.fine(getConnectionNumber() + " " + a + " server removed from list.");
                                }
                                Iterator it = dContext.getServerList().iterator();
                                while (it.hasNext()) {
                                    DonkeyServer aDonkeyServer = (DonkeyServer) it.next();
                                    dContext.addSeverForStatusUpdate(aDonkeyServer);
                                    if (aDonkeyServer.getName().equals("no name")) {
                                        dContext.addSeverForServerInfo(aDonkeyServer);
                                    }
                                }
                            }
                            break;
                        }
                    case OP_SEARCHRESULT:
                        {
                            log.fine("Search results received.");
                            try {
                                SearchQuery currentsq = dContext.getSearchQuery();
                                String searchid = currentsq.getQuery();
                                LinkedList donkeySearch = inPacket.getSearchResults(new InetSocketAddress(getChannel().socket().getInetAddress(), getChannel().socket().getPort()), currentsq);
                                SearchManager sm = SearchManager.getInstance();
                                while (!donkeySearch.isEmpty()) {
                                    sm.addResult(searchid, (SearchResult) donkeySearch.removeFirst());
                                }
                                lastsearch = currentsq;
                                if (currentsq.getResultCount() >= currentsq.getLimit()) {
                                    sm.setQueryStatus(searchid, SearchManager.QS_End);
                                    dContext.removeSearchQuery(currentsq);
                                }
                            } catch (Exception e) {
                                log.info("trouble: hexdump from " + inPacket.premetapos + " till " + Math.min(inPacket.premetapos + 128, inPacket.packetBuffer.limit()) + " " + Convert.byteBufferToHexString(inPacket.packetBuffer, inPacket.premetapos, Math.min(inPacket.premetapos + 128, inPacket.packetBuffer.limit())));
                                log.log(Level.WARNING, "Error checking results: ", e);
                            }
                            break;
                        }
                    case OP_CALLBACKREQUESTED:
                        {
                            InetSocketAddress address = inPacket.getPushAddress();
                            log.fine("Server requests push connection to: " + address.toString());
                            DonkeyClientConnection dcc = new DonkeyClientConnection();
                            dcc.setPeerAddress(address);
                            ConnectionManager.getInstance().newConnection(dcc);
                            break;
                        }
                    case OP_CALLBACKFAIL:
                        {
                            break;
                        }
                    case OP_FOUNDSOURCES:
                        {
                            log.fine("Received download sources...");
                            inPacket.addDownloadSources();
                            break;
                        }
                    case OP_IDCHANGE:
                        {
                            long id = inPacket.getClientId();
                            log.info("ID: " + Long.toString(id));
                            if (DonkeyProtocol.isLowID(id)) {
                                log.info("We are firewalled.");
                            } else {
                                log.fine("Not firewalled.");
                            }
                            waiting = true;
                            if (dContext.isEmuleEnabled && inPacket.getPacket().getBuffer().remaining() >= 8) {
                                int flags = inPacket.getPacket().getBuffer().getInt(10);
                                donkeyServer.setTCPEmuleCompression((flags & 1) == 1);
                            }
                            dContext.setUserID(id);
                            state = DonkeyProtocol.SERVER_CONNECTED;
                            dContext.setUpdateSharedFiles(false);
                            DonkeyPacket fileOffer = DonkeyPacketFactory.fileOffer();
                            if (fileOffer != null) addOutPacket(fileOffer);
                            break;
                        }
                    case OP_SERVERINFO:
                        {
                            donkeyServer = inPacket.getServerInfo(donkeyServer);
                            log.fine("Servername:" + donkeyServer.getName() + "\ned2k: Description:" + donkeyServer.getDescription());
                            break;
                        }
                    default:
                        {
                            log.fine("Got unknown packet from server: \"" + inPacket.toString() + "\"");
                        }
                }
            } catch (CorruptPacketException cpe) {
                if (cpe.getCause() == null) {
                    log.log(Level.WARNING, getConnectionNumber() + " has problem with corrupt packet: " + cpe.getMessage() + " cause ", cpe.getCause());
                } else {
                    log.severe(getConnectionNumber() + " has problem with packet: " + cpe.getMessage());
                }
                close();
            }
            inPacket.disposePacket();
        }
    }

    private DonkeyScannedPacket inPacket;

    private boolean hasSearch = false;

    private DonkeyServer donkeyServer;

    private long lastStatusMessage = 0;

    private long lastServerListrequesttime;

    private long lastServerListreceivetime;

    private static long listcounter = 0;

    private long locallistcounter = 0;

    private SearchQuery lastsearch;

    public boolean isMoreLastsearch(SearchQuery search) {
        if (lastsearch != null && lastsearch == search) {
            return search.getHint("no_more_ed2k") == null;
        }
        return false;
    }

    public boolean check(int count) {
        boolean result = super.check(count);
        if (state == DonkeyProtocol.SERVER_CONNECTED) {
            if ((System.currentTimeMillis() - lastServerListreceivetime) > 1800000L && (System.currentTimeMillis() - lastServerListrequesttime) > 360000L) {
                listcounter++;
                locallistcounter++;
                log.fine("serverlist from server request time since last call:" + (System.currentTimeMillis() - lastServerListreceivetime) + " " + listcounter + " " + locallistcounter);
                addOutPacket(DonkeyPacketFactory.command(OP_GETSERVERLIST));
                lastServerListrequesttime = System.currentTimeMillis();
            }
            if (dContext.doUpdateSharedFiles() && dContext.getFilesToHash().isEmpty()) {
                dContext.setUpdateSharedFiles(false);
                DonkeyPacket packet = DonkeyPacketFactory.fileOffer();
                if (packet == null) {
                    log.fine("No files to share.");
                } else {
                    log.fine("Sending shared file list to server.");
                    addOutPacket(packet);
                }
            }
        }
        return result;
    }

    /** 
    * Used to handle connection rejects.
    */
    public void setConnected(boolean connected) {
        if (state == STATE_CONNECTING) {
            if (!connected) {
                if (!donkeyServer.isImmortal()) {
                    dContext.getServerList().remove(donkeyServer);
                    log.fine(getConnectionNumber() + " remove server because of reject (server: " + donkeyServer + ")");
                } else {
                    log.info(getConnectionNumber() + " problem with connect to " + donkeyServer);
                }
            }
        }
        super.setConnected(connected);
    }

    /**
	  * Close the connection to the edonkey2000 server.
	  */
    public void close() {
        super.close();
        SearchQuery currentsq = dContext.getSearchQuery();
        while (currentsq != null) {
            String searchid = currentsq.getQuery();
            SearchManager sm = SearchManager.getInstance();
            if (sm.getQueryStatus(searchid) != SearchManager.QS_Quered) {
                sm.setQueryStatus(searchid, SearchManager.QS_Quered);
            }
            currentsq = dContext.getSearchQuery();
        }
    }
}
