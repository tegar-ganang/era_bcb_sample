package jcfs.core.serverside;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import jcfs.core.client.JCFS;
import jcfs.core.fs.RFile;
import jcfs.core.fs.RServerInfo;
import jcfs.core.fs.WriteMode;
import org.apache.commons.io.IOUtils;

/**
 * Base server management
 * @author enrico
 */
public class JCFSFileServer {

    private static final Logger logger = Logger.getLogger("jcfsfileserver");

    private CommandServer server;

    private ReplicaManager replicaManager;

    private JCFSServerSideFileManager fileManager;

    private DiscoveryService discovery;

    private List<RServerInfo> peers;

    private JCFS localClient;

    private RServerInfo localServerInfo;

    private DirectoryConfiguration directoryDefaultConfiguration = DirectoryConfiguration.DEFAULT;

    private Map<String, DirectoryConfiguration> directoryDefaults = new HashMap<String, DirectoryConfiguration>();

    private ServerSideFileCache cache;

    private BackgroundDistributor distributor;

    public JCFS getLocalClient() {
        return localClient;
    }

    public ServerSideFileCache getCache() {
        return cache;
    }

    public void defineDirectoryDefault(DirectoryConfiguration directoryConfig) {
        directoryDefaults.put(directoryConfig.getPath(), directoryConfig);
    }

    public DirectoryConfiguration getDirectoryDefaultConfiguration(String path) {
        DirectoryConfiguration config = directoryDefaults.get(path);
        if (config == null) {
            return directoryDefaultConfiguration;
        }
        return config;
    }

    public RServerInfo getLocalServerInfo() {
        return localServerInfo;
    }

    void updatePeerInfo() throws IOException {
        List<RServerInfo> newPeerList = localClient.discoverServers();
        List<RServerInfo> list = new ArrayList<RServerInfo>();
        for (RServerInfo info : newPeerList) {
            if (!info.equals(localServerInfo)) {
                list.add(info);
                logger.log(Level.INFO, "{0}:found peer {1}", new Object[] { localServerInfo, info });
            }
        }
        this.peers = list;
    }

    public List<RServerInfo> getPeers() {
        return peers;
    }

    public JCFSServerSideFileManager getFileManager() {
        return fileManager;
    }

    public CommandServer getServer() {
        return server;
    }

    void copyFileOnPeer(String path, RServerInfo peerserver, boolean allowoverwrite) throws IOException {
        RFile file = new RFile(path);
        OutputStream out = null;
        FileInputStream in = null;
        try {
            in = fileManager.openFileRead(path);
            out = localClient.openWrite(file, false, WriteMode.TRANSACTED, 1, peerserver, !allowoverwrite);
            IOUtils.copyLarge(in, out);
            out.close();
            out = null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Throwable t) {
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (Throwable t) {
                }
            }
        }
    }

    public JCFSFileServer(int tcpPort, String tcpAddress, int udpPort, String udpAddress, File storageDir, int cacheCapacity, int distributorPeriod) {
        this.fileManager = new JCFSServerSideFileManager(storageDir);
        this.server = new CommandServer(tcpPort, tcpAddress, this);
        this.discovery = new DiscoveryService(udpPort, udpAddress, this);
        this.localClient = new JCFS();
        this.localClient.setUdpAddress(udpAddress);
        this.localClient.setUdpPort(udpPort);
        this.localServerInfo = new RServerInfo(tcpAddress, tcpPort);
        this.peers = new ArrayList<RServerInfo>();
        this.replicaManager = new ReplicaManager(this);
        this.cache = new ServerSideFileCache(cacheCapacity);
        this.distributor = new BackgroundDistributor(this, distributorPeriod);
    }

    public ReplicaManager getReplicaManager() {
        return replicaManager;
    }

    public boolean isRunning() {
        return server.isRunning();
    }

    public void start() throws IOException {
        server.start();
        if (discovery.getUdpPort() > 0) {
            discovery.start();
        }
        distributor.start();
        updatePeerInfo();
    }

    public void stop() {
        distributor.stop();
        server.stop();
        discovery.stop();
        localClient.stop();
    }

    public void forcePeerDiscovery() throws IOException {
        updatePeerInfo();
    }

    public DirectoryConfiguration getDirectoryDefaultConfiguration() {
        return directoryDefaultConfiguration;
    }

    public void setDirectoryDefaultConfiguration(DirectoryConfiguration directoryConfiguration) {
        this.directoryDefaultConfiguration = directoryConfiguration;
    }
}
