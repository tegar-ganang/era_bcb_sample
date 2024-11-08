package de.tudresden.inf.rn.mobilis.server.agents;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.NodeInformationProvider;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.jivesoftware.smackx.packet.DiscoverItems;
import de.tudresden.inf.rn.mobilis.server.MobilisManager;
import de.tudresden.inf.rn.mobilis.server.services.MobilisService;

/**
 *
 * @author Christopher
 */
public class MobilisAgent implements NodeInformationProvider, ConnectionListener {

    private String mIdentifier;

    private XMPPConnection mConnection = null;

    private String mJid = null;

    private String resource = null;

    private final Set<MobilisService> mServices = Collections.synchronizedSet(new HashSet<MobilisService>());

    private final Map<String, Object> mDefaultSettings = Collections.synchronizedMap(new HashMap<String, Object>());

    public MobilisAgent(String ident) {
        this(ident, true);
    }

    public MobilisAgent(String ident, boolean loadConfig) {
        mIdentifier = ident;
        if (loadConfig) {
            try {
                synchronized (mDefaultSettings) {
                    mDefaultSettings.putAll(MobilisManager.getInstance().getSettings("agents", ident));
                }
            } catch (Exception e) {
                MobilisManager.getLogger().warning("Mobilis Agent (" + getIdent() + ") could not read configuration, using hardcoded settings instead.");
            }
            String hostname = null;
            try {
                hostname = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException uh) {
                hostname = "localhost";
            }
            synchronized (mDefaultSettings) {
                if (!mDefaultSettings.containsKey("host")) {
                    mDefaultSettings.put("host", hostname);
                }
                if (!mDefaultSettings.containsKey("port")) {
                    mDefaultSettings.put("port", "5222");
                }
                if (!mDefaultSettings.containsKey("service")) {
                    mDefaultSettings.put("service", hostname);
                }
                if (!mDefaultSettings.containsKey("resource")) {
                    mDefaultSettings.put("resource", "MobilisServer");
                }
            }
        }
    }

    public MobilisAgent(String ident, boolean loadConfig, String resource) {
        mIdentifier = ident;
        this.resource = resource;
        if (loadConfig) {
            try {
                synchronized (mDefaultSettings) {
                    mDefaultSettings.putAll(MobilisManager.getInstance().getSettings("agents", ident));
                }
            } catch (Exception e) {
                e.printStackTrace();
                MobilisManager.getLogger().warning("Mobilis Agent (" + getIdent() + ") could not read configuration, using hardcoded settings instead.");
            }
            String hostname = null;
            try {
                hostname = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException uh) {
                hostname = "localhost";
            }
            synchronized (mDefaultSettings) {
                if (!mDefaultSettings.containsKey("host")) {
                    mDefaultSettings.put("host", hostname);
                }
                if (!mDefaultSettings.containsKey("port")) {
                    mDefaultSettings.put("port", "5222");
                }
                if (!mDefaultSettings.containsKey("service")) {
                    mDefaultSettings.put("service", hostname);
                }
                mDefaultSettings.put("resource", resource);
            }
        }
    }

    public MobilisAgent(String username, String password, MobilisAgent refAgentForConfig) {
        this(username, false);
        synchronized (mDefaultSettings) {
            mDefaultSettings.putAll(refAgentForConfig.getAllSettings());
            mDefaultSettings.put("username", username);
            mDefaultSettings.put("password", password);
        }
    }

    protected Map<String, Object> getAllSettings() {
        return mDefaultSettings;
    }

    public Object getSettingString(String name) {
        synchronized (mDefaultSettings) {
            Object o = mDefaultSettings.get(name);
            return (o instanceof String ? (String) o : null);
        }
    }

    @SuppressWarnings("unchecked")
    public Object getSettingStrings(String name) {
        synchronized (mDefaultSettings) {
            Object o = mDefaultSettings.get(name);
            return (o instanceof Map<?, ?> ? (Map<String, String>) o : null);
        }
    }

    public void startup() throws XMPPException {
        String host;
        Integer port;
        String service;
        synchronized (mDefaultSettings) {
            host = (String) mDefaultSettings.get("host");
            port = Integer.parseInt((String) mDefaultSettings.get("port"));
            service = (String) mDefaultSettings.get("service");
        }
        ConnectionConfiguration connConfig = new ConnectionConfiguration(host, port, service);
        mConnection = new XMPPConnection(connConfig);
        mConnection.connect();
        String password = null;
        String resource = null;
        synchronized (mDefaultSettings) {
            mJid = mDefaultSettings.get("username") + "@" + mDefaultSettings.get("service");
            password = (String) mDefaultSettings.get("password");
            resource = (String) mDefaultSettings.get("resource");
        }
        mConnection.login(mJid, password, resource);
        mConnection.addConnectionListener(this);
        ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(mConnection);
        try {
            sdm.addFeature(MobilisManager.discoNamespace);
        } catch (Exception e) {
            MobilisManager.getLogger().warning("Problem with ServiceDiscoveryManager: " + e.getMessage());
        }
        synchronized (mServices) {
            for (MobilisService ms : mServices) {
                try {
                    ms.startup();
                    sdm.setNodeInformationProvider(ms.getNode(), ms);
                } catch (Exception e) {
                    MobilisManager.getLogger().warning("Couldn't startup Mobilis Service (" + ms.getIdent() + ") because of " + e.getClass().getName() + ": " + e.getMessage());
                }
            }
        }
        try {
            sdm.setNodeInformationProvider(MobilisManager.discoServicesNode, this);
        } catch (Exception e) {
            MobilisManager.getLogger().warning("Problem with NodeInformationProvider: " + MobilisManager.discoServicesNode + " (" + getIdent() + ") " + e.getMessage());
        }
        MobilisManager.getLogger().info("Mobilis Agent (" + getIdent() + ") started up.");
    }

    /**
	 * Deletes the session agent account, frees all resources and disconnects the XMPP connection.
	 * @throws org.jivesoftware.smack.XMPPException
	 */
    public void shutdown() throws XMPPException {
        ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(mConnection);
        try {
            sdm.removeFeature(MobilisManager.discoNamespace);
        } catch (Exception e) {
            MobilisManager.getLogger().warning("Problem with ServiceDiscoveryManager: " + e.getMessage());
        }
        try {
            sdm.removeNodeInformationProvider(MobilisManager.discoServicesNode);
        } catch (Exception e) {
            MobilisManager.getLogger().warning("Problem with NodeInformationProvider: " + MobilisManager.discoServicesNode + " (" + getIdent() + ") " + e.getMessage());
        }
        for (MobilisService service : mServices) {
            try {
                sdm.removeNodeInformationProvider(service.getNode());
                service.shutdown();
            } catch (Exception e) {
            }
        }
        if ((mConnection != null) && mConnection.isConnected()) {
            mConnection.removeConnectionListener(this);
            mConnection.disconnect();
        }
        mConnection = null;
        MobilisManager.getLogger().info("Mobilis Agent (" + getIdent() + ") shut down.");
    }

    public void registerService(MobilisService service) {
        synchronized (mServices) {
            mServices.add(service);
        }
        MobilisManager.getLogger().config("Mobilis Service (" + service.getIdent() + ") registered at Mobilis Agent (" + getIdent() + ").");
    }

    public void unregisterService(MobilisService service) {
        synchronized (mServices) {
            mServices.remove(service);
        }
        MobilisManager.getLogger().config("Mobilis Service (" + service.getIdent() + ") unregistered at Mobilis Agent (" + getIdent() + ").");
    }

    /**
	 * Returns the XMPP connection of the session agent.
	 * @return XMPPConnection
	 */
    public XMPPConnection getConnection() {
        return mConnection;
    }

    /**
	 * Returns the bare jid of the session agent.
	 * @return String
	 */
    public String getJid() {
        return mJid;
    }

    /**
	 * Returns the full jid of the session agent.
	 * @return String
	 */
    public String getFullJid() {
        return getJid() + "/" + getResource();
    }

    /**
	 * Returns the XMPP resource of the session agent.
	 * @return String
	 */
    public String getResource() {
        Object o = mDefaultSettings.get("resource");
        if (o != null && o instanceof String) return (String) o;
        return null;
    }

    /**
	 * Returns the identifier of the session agent.
	 * @return String
	 */
    public String getIdent() {
        return mIdentifier;
    }

    @Override
    public List<DiscoverInfo.Identity> getNodeIdentities() {
        List<DiscoverInfo.Identity> identities = new ArrayList<DiscoverInfo.Identity>();
        if (getNodeItems().size() > 0) {
            identities.add(new DiscoverInfo.Identity("hierarchy", "branch"));
        } else {
            identities.add(new DiscoverInfo.Identity("hierarchy", "leaf"));
        }
        return identities;
    }

    @Override
    public List<String> getNodeFeatures() {
        List<String> features = new ArrayList<String>();
        features.add(MobilisManager.discoNamespace);
        return features;
    }

    @Override
    public List<DiscoverItems.Item> getNodeItems() {
        List<DiscoverItems.Item> items = new ArrayList<DiscoverItems.Item>();
        synchronized (mServices) {
            for (MobilisService service : mServices) {
                try {
                    DiscoverItems.Item item = service.getDiscoverItem();
                    if (item != null) {
                        items.add(item);
                    }
                } catch (Exception e) {
                }
            }
        }
        return items;
    }

    public String servicesToString() {
        StringBuilder sb = new StringBuilder("Services[");
        for (MobilisService service : mServices) {
            sb.append("NS=").append(service.getNamespace()).append(",Version=").append(service.getVersion());
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public void connectionClosed() {
        MobilisManager.getLogger().info("MobilisAgent (" + getIdent() + ") connection was closed normally or the reconnection process has been aborted.");
    }

    @Override
    public void connectionClosedOnError(Exception e) {
        MobilisManager.getLogger().warning("MobilisAgent (" + getIdent() + ") connection was closed due to " + e.getClass().getName() + ": " + e.getMessage());
    }

    @Override
    public void reconnectingIn(int arg0) {
        MobilisManager.getLogger().fine("MobilisAgent (" + getIdent() + ") will retry to reconnect in " + arg0 + " seconds.");
    }

    @Override
    public void reconnectionFailed(Exception arg0) {
        MobilisManager.getLogger().warning("MobilisAgent (" + getIdent() + ") attempt to connect to the server has failed.");
    }

    @Override
    public void reconnectionSuccessful() {
        MobilisManager.getLogger().info("MobilisAgent (" + getIdent() + ") reconnected successfully to the server.");
    }
}
