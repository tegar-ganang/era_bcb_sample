package net.sourceforge.cridmanager;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import net.sourceforge.cridmanager.services.ServiceProvider;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.log4j.Logger;

public class FtpPool {

    /**
	 * Logger for this class
	 */
    private static final Logger logger = Logger.getLogger(FtpPool.class);

    private Map Hosts = Collections.synchronizedMap(new HashMap());

    private class Host {

        /**
		 * Logger for this class
		 */
        private final Logger logger = Logger.getLogger(Host.class);

        private String Host;

        private int counter;

        private int maxClients;

        private HashMap Clients;

        public Host(String Host) {
            this.Host = Host;
            counter = 0;
            maxClients = ServiceProvider.instance().getSettings().read(ISettings.FTP_MAXCLIENTS, 20);
            if (maxClients < 3) maxClients = 3;
        }

        /**
		 * @return Returns the maxClients.
		 */
        public int getMaxClients() {
            return maxClients;
        }

        /**
		 * @param maxClients The maxClients to set.
		 */
        public void setMaxClients(int maxClients) {
            this.maxClients = maxClients;
        }

        /**
		 * @return Returns the counter.
		 */
        public int getCounter() {
            return counter;
        }

        /**
		 * @return Returns the host.
		 */
        public String getHost() {
            return Host;
        }

        public synchronized FTPClient getFTPClient(String User, String Password) throws IOException {
            if (logger.isDebugEnabled()) {
                logger.debug("getFTPClient(String, String) - start");
            }
            while ((counter >= maxClients)) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    logger.error("getFTPClient(String, String)", e);
                    e.printStackTrace();
                }
            }
            FTPClient result = null;
            String key = User.concat(Password);
            logger.debug("versuche vorhandenen FTPClient aus Liste zu lesen");
            if (Clients != null) {
                if (Clients.containsKey(key)) {
                    LinkedList ClientList = (LinkedList) Clients.get(key);
                    if (!ClientList.isEmpty()) do {
                        result = (FTPClient) ClientList.getLast();
                        logger.debug("-- hole einen Client aus der Liste: " + result.toString());
                        ClientList.removeLast();
                        if (!result.isConnected()) {
                            logger.debug("---- nicht mehr verbunden.");
                            result = null;
                        } else {
                            try {
                                result.changeWorkingDirectory("/");
                            } catch (IOException e) {
                                logger.debug("---- schmei�t Exception bei Zugriff.");
                                result = null;
                            }
                        }
                    } while (result == null && !ClientList.isEmpty());
                    if (ClientList.isEmpty()) {
                        Clients.remove(key);
                    }
                } else {
                }
            } else logger.debug("-- keine Liste vorhanden.");
            if (result == null) {
                logger.debug("Kein FTPCLient verf�gbar, erstelle einen neuen.");
                result = new FTPClient();
                logger.debug("-- Versuche Connect");
                result.connect(Host);
                logger.debug("-- Versuche Login");
                result.login(User, Password);
                result.setFileType(FTPClient.BINARY_FILE_TYPE);
                if (counter == maxClients - 1) {
                    RemoveBufferedClient();
                }
            }
            logger.debug("OK: neuer FTPClient ist " + result.toString());
            ;
            counter++;
            if (logger.isDebugEnabled()) {
                logger.debug("getFTPClient(String, String) - end");
            }
            return result;
        }

        public synchronized void releaseFtpClient(FTPClient aClient, String user, String Password) {
            if (logger.isDebugEnabled()) {
                logger.debug("releaseFtpClient(FTPClient, String, String) - start");
            }
            String key = user.concat(Password);
            if (aClient != null) {
                logger.debug("Gib FTPClient zur�ck: " + aClient.toString());
                if (Clients == null) {
                    Clients = new HashMap();
                }
                ;
                LinkedList ClientList;
                if (!Clients.containsKey(key)) {
                    ClientList = new LinkedList();
                    Clients.put(key, ClientList);
                } else ClientList = (LinkedList) Clients.get(key);
                ClientList.addFirst(aClient);
            }
            ;
            counter--;
            notifyAll();
            if (logger.isDebugEnabled()) {
                logger.debug("releaseFtpClient(FTPClient, String, String) - end");
            }
        }

        private synchronized void RemoveBufferedClient() {
            if (logger.isDebugEnabled()) {
                logger.debug("RemoveBufferedClient() - start");
            }
            LinkedList MaxList = null;
            if (Clients == null) {
                Clients = new HashMap();
            }
            for (Iterator iter = Clients.values().iterator(); iter.hasNext(); ) {
                LinkedList CurrentList = (LinkedList) iter.next();
                if (MaxList == null) {
                    MaxList = CurrentList;
                } else if (CurrentList.size() > MaxList.size()) {
                    MaxList = CurrentList;
                }
            }
            if (MaxList != null) {
                FTPClient oldClient = (FTPClient) MaxList.removeLast();
                try {
                    oldClient.logout();
                    oldClient.disconnect();
                } catch (IOException e) {
                    logger.warn("RemoveBufferedClient() - exception ignored", e);
                }
            }
            if (logger.isDebugEnabled()) {
                logger.debug("RemoveBufferedClient() - end");
            }
        }
    }

    public FTPClient getFtpClient(String hostName, String user, String password) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("getFtpClient(String, String, String) - start");
        }
        FTPClient result = null;
        if (Hosts != null) {
            Host myHost = null;
            if (Hosts.containsKey(hostName)) {
                myHost = (Host) Hosts.get(hostName);
            } else {
                myHost = new Host(hostName);
                Hosts.put(hostName, myHost);
            }
            result = myHost.getFTPClient(user, password);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("getFtpClient(String, String, String) - end");
        }
        return result;
    }

    public void releaseFtpClient(FTPClient aClient, String hostName, String user, String Password) {
        if (logger.isDebugEnabled()) {
            logger.debug("releaseFtpClient(FTPClient, String, String, String) - start");
        }
        Host myHost = (Host) Hosts.get(hostName);
        if (myHost != null) {
            myHost.releaseFtpClient(aClient, user, Password);
        } else {
            logger.warn("myHost null bei R�ckgabe des Clients!");
        }
        if (logger.isDebugEnabled()) {
            logger.debug("releaseFtpClient(FTPClient, String, String, String) - end");
        }
    }
}
