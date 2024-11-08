package com.peterhi.net.server.sgs;

import static com.peterhi.util.LogMacros.*;
import java.io.File;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import com.peterhi.net.conv.impl.*;
import com.peterhi.net.conv.Convertible;
import com.peterhi.net.conv.Processor;
import com.peterhi.net.server.sgs.ref.SGSClientSessionListener;
import com.peterhi.net.server.sgs.ref.SGSNSessionListener;
import com.peterhi.net.conv.Converter;
import com.peterhi.util.Str;
import com.sun.sgs.app.AppContext;
import com.sun.sgs.app.AppListener;
import com.sun.sgs.app.Channel;
import com.sun.sgs.app.ChannelManager;
import com.sun.sgs.app.ClientSession;
import com.sun.sgs.app.ClientSessionListener;
import com.sun.sgs.app.DataManager;
import com.sun.sgs.app.ManagedReference;
import com.sun.sgs.app.NameNotBoundException;
import com.sun.sgs.app.TaskManager;

public class SGSServer implements AppListener, Serializable {

    private static final Logger logger = Logger.getLogger("sgs");

    /**
	 * The path to look for the convertible registry properties file
	 */
    public static final String CONV_PROPS = "/com/peterhi/net/server/sgs/conv.properties";

    public static final String PROC_PROPS = "/com/peterhi/net/server/sgs/proc.properties";

    static {
        try {
            PropertyConfigurator.configure(new File(new File(System.getProperty("user.dir")), "sgs-log4j.properties").getAbsolutePath());
            logger.info(String.format("NEW LOG4J LOG SESSION (%s) STARTED", logger.getName()));
            getLogger("com.sun.sgs");
            Properties convs = new Properties();
            convs.load(SGSServer.class.getResourceAsStream(CONV_PROPS));
            Converter.getInstance().load(convs);
            Properties procs = new Properties();
            procs.load(SGSServer.class.getResourceAsStream(PROC_PROPS));
            Processor.getInstance().load(procs);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
	 * Serializable ID
	 */
    private static final long serialVersionUID = 853331760031369469L;

    private boolean nAttached;

    public void sendToNServer(byte[] data) {
        try {
            SGSNSessionListener nl = SGSNSessionListener.getInstance();
            if (nl != null) {
                nl.send(data);
            }
        } catch (NameNotBoundException ex) {
        }
    }

    public boolean addClientData(SGSClientData d) {
        DataManager man = AppContext.getDataManager();
        try {
            man.getBinding(d.getBinding(), SGSClientData.class);
            return false;
        } catch (NameNotBoundException ex) {
            man.setBinding(d.getBinding(), d);
            return true;
        }
    }

    public void removeClientData(String account) {
        ChannelManager man = AppContext.getChannelManager();
        DataManager dman = AppContext.getDataManager();
        SGSClientData data = getClientData(account);
        String channelName = data.get(SGSClientData.CURRENT_CHANNEL_NAME, String.class);
        if (!Str.nostr(channelName)) {
            Channel ch = man.getChannel(channelName);
            SGSBusinessProcess.onLeaveChannelNotify(this, data.get(SGSClientData.NSERVER_ID, Integer.class), data.get(SGSClientData.NSERVER_CHANNEL_ID, Integer.class), data.get(SGSClientData.CLIENT_SESSION, ClientSession.class), ch);
        }
        SGSClientData toRemove = null;
        try {
            toRemove = dman.getBinding(SGSClientData.getBinding(account), SGSClientData.class);
            dman.removeBinding(SGSClientData.getBinding(account));
            dman.removeObject(toRemove);
        } catch (NameNotBoundException ex) {
        }
        SGSNSessionListener nl = SGSNSessionListener.getInstance();
        if (nl != null) {
            DestroyClient dc = new DestroyClient();
            dc.account = account;
            try {
                nl.send(Converter.getInstance().convert(dc));
            } catch (Exception ex) {
                logger.warn("unable to notify main server of client removal", ex);
            }
        }
    }

    public SGSClientData getClientData(String account) {
        DataManager dman = AppContext.getDataManager();
        return dman.getBinding(SGSClientData.getBinding(account), SGSClientData.class);
    }

    public ClientSession getClientSession(String account) {
        return getClientData(account).get(SGSClientData.CLIENT_SESSION, ClientSession.class);
    }

    public boolean addChannel(String name, SGSChannelData l) {
        DataManager man = AppContext.getDataManager();
        try {
            man.getBinding(SGSChannelData.getBinding(name), SGSChannelData.class);
            return false;
        } catch (NameNotBoundException ex) {
            man.setBinding(SGSChannelData.getBinding(name), l);
            return true;
        }
    }

    public SGSChannelData getChannelData(String name) {
        DataManager dman = AppContext.getDataManager();
        return dman.getBinding(SGSChannelData.getBinding(name), SGSChannelData.class);
    }

    public void removeChannel(String name) {
        DataManager dman = AppContext.getDataManager();
        dman.removeBinding(SGSChannelData.getBinding(name));
    }

    public Iterator<String> channelIterator() {
        DataManager dman = AppContext.getDataManager();
        HashSet<String> ret = new HashSet<String>();
        String curName = null;
        while ((curName = dman.nextBoundName(curName)) != null) {
            if (SGSChannelData.isBoundAs(curName)) {
                ret.add(curName);
            }
        }
        return ret.iterator();
    }

    public void ndispatch(Convertible conv) {
        SGSNSessionListener nl = SGSNSessionListener.getInstance();
        if (nl != null) {
            try {
                nl.send(Converter.getInstance().convert(conv));
            } catch (Exception ex) {
            }
        }
    }

    /**
	 * Called when NServer notifies a client is trying to log it
	 * This operation will generate a key that is used to verify
	 * that the client actually contacted NServer before visiting here
	 * 
	 * @param acc The account used to login
	 * @param key The key issued by NServer
	 */
    public void putPendingLogin(String acc, String key, int nid) {
        DataManager dman = AppContext.getDataManager();
        dman.setBinding(SGSKey.getBinding(acc), new SGSKey(acc, key, nid, System.currentTimeMillis()));
    }

    /**
	 * User this method to guard duplicated logins
	 * @param acc The account name
	 * @return <c>true</c> if the account was either logged in or
	 * pending for login, otherwise, <c>false</c>
	 */
    public boolean isLoggedInOrPendingLogin(String acc) {
        try {
            DataManager dman = AppContext.getDataManager();
            dman.getBinding(SGSKey.getBinding(acc), SGSKey.class);
            dman.getBinding(SGSClientData.getBinding(acc), SGSClientData.class);
            return true;
        } catch (NameNotBoundException ex) {
            return false;
        }
    }

    /**
	 * Use this method to check whether the account is pending a login
	 * @param acc The account
	 * @return <c>true if pending</c>, <c>false if this account
	 * is either logged in or made no login attempt at all</c>
	 */
    public boolean isPendingLogin(String acc) {
        try {
            DataManager dman = AppContext.getDataManager();
            dman.getBinding(SGSKey.getBinding(acc), SGSKey.class);
            return true;
        } catch (NameNotBoundException ex) {
            return false;
        }
    }

    public SGSKey getPendingLogin(String acc) {
        try {
            DataManager dman = AppContext.getDataManager();
            return dman.getBinding(SGSKey.getBinding(acc), SGSKey.class);
        } catch (NameNotBoundException ex) {
            return null;
        }
    }

    /**
	 * Check whether the pending login exists and whether
	 * the key provided matches with the server's copy
	 * @param acc The account name
	 * @param key The key
	 * @return <c>true</c> if verified successfully,
	 * otherwise <c>false</c>
	 */
    public boolean verifyPendingLogin(String acc, String key) {
        DataManager dman = AppContext.getDataManager();
        boolean b1 = isPendingLogin(acc);
        String k = dman.getBinding(SGSKey.getBinding(acc), SGSKey.class).key;
        boolean b2 = k.equals(key);
        return (b1 && b2);
    }

    /**
	 * Removes an entry from the pending login list. Used
	 * when pending times out or pending successfully transitioned
	 * to logged in state
	 * @param acc
	 */
    public void removePendingLogin(String acc) {
        DataManager dman = AppContext.getDataManager();
        dman.removeBinding(SGSKey.getBinding(acc));
    }

    public Iterator<String> keyIterator() {
        DataManager dman = AppContext.getDataManager();
        HashSet<String> ret = new HashSet<String>();
        String curName = null;
        while ((curName = dman.nextBoundName(curName)) != null) {
            if (SGSKey.isBoundAs(curName)) {
                ret.add(curName);
            }
        }
        return ret.iterator();
    }

    /**
	 * Do not call this method directly. It is used by the system timer
	 * 
	 * @param timeout How long should the expirer treat an entry as expired
	 */
    public void expireKeys(long timeout) {
        DataManager dman = AppContext.getDataManager();
        int i = 0;
        for (Iterator<String> itor = keyIterator(); itor.hasNext(); ) {
            String key = itor.next();
            SGSKey k = dman.getBinding(key, SGSKey.class);
            if ((System.currentTimeMillis() - k.ts) >= timeout) {
                i++;
                itor.remove();
            }
        }
        logger.info(String.format("expire task %d keys expired", i));
    }

    public void detachNServer() {
        SGSNSessionListener.setInstance(null);
        nAttached = false;
    }

    public void initialize(Properties props) {
        DataManager dm = AppContext.getDataManager();
        ManagedReference serv = dm.createReference(this);
        TaskManager tm = AppContext.getTaskManager();
        tm.schedulePeriodicTask(new SGSKeyExpirer(serv), 9000, 9000);
        logger.info("SGSServer initialized");
    }

    public ClientSessionListener loggedIn(ClientSession clientSession) {
        if (SGSBusinessProcess.isNServerSession(clientSession)) {
            if (!nAttached) {
                SGSNSessionListener l = new SGSNSessionListener(AppContext.getDataManager().createReference(this));
                l.setSession(clientSession);
                SGSNSessionListener.setInstance(l);
                nAttached = true;
                return l;
            } else {
                return null;
            }
        } else {
            String account = SGSBusinessProcess.accFromSesName(clientSession.getName());
            String key = SGSBusinessProcess.keyFromSesName(clientSession.getName());
            if (isPendingLogin(account)) {
                if (verifyPendingLogin(account, key)) {
                    SGSKey sgsKey = getPendingLogin(account);
                    SGSClientSessionListener l = new SGSClientSessionListener(AppContext.getDataManager().createReference(this), clientSession);
                    SGSClientData data = new SGSClientData();
                    data.set(SGSClientData.CLIENT_SESSION, (Serializable) clientSession);
                    data.set(SGSClientData.NSERVER_ID, Integer.valueOf(sgsKey.nid));
                    data.set(SGSClientData.TALKING, Boolean.FALSE);
                    if (addClientData(data)) {
                        removePendingLogin(account);
                        return l;
                    } else {
                        logger.warn(String.format("duplicate client object %s", Arrays.toString(clientSession.getSessionId().getBytes())));
                    }
                } else {
                    logger.warn(String.format("client acc/key mismatch %s : %s", account, key));
                }
            } else {
                logger.warn(String.format("acc/key pair not found for %s", account));
            }
            return null;
        }
    }
}
