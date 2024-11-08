package org.jnerve;

import java.util.*;
import java.io.*;
import org.jnerve.util.*;

/** Stores run-time configuraton data for the server
  */
public class JNerveConfiguration {

    private static final String SERVER_PORT = "jnerve.server_port";

    private static final String SERVER_MAX_CONNECTIONS = "jnerve.max_connections";

    private static final String SERVER_USER_PERSISTENCE_STORE = "jnerve.user_persistence_store.classname";

    private static final String SERVER_MOTD_FILE = "jnerve.motd.path";

    private static final String SERVER_SESSION_TIMEOUT = "jnerve.session.timeout";

    private static final String SERVER_CHANNELS_FILE = "jnerve.channelsfile.path";

    private static final String SERVER_POLICY_OPENDOOR = "jnerve.policy.opendoor";

    private int serverPort = 8888;

    private int maxConnections = 5000;

    private int sessionTimeout = 600000;

    private String userPersistenceStoreClassname = "org.jnerve.MySQLUserPersistence";

    private String channelsFilePath = null;

    private boolean policyOpenDoor = false;

    private String[] motd = null;

    private Properties rawProperties;

    public JNerveConfiguration(Properties p) {
        rawProperties = p;
        serverPort = new Integer(p.getProperty(SERVER_PORT, String.valueOf(serverPort)).trim()).intValue();
        maxConnections = new Integer(p.getProperty(SERVER_MAX_CONNECTIONS, String.valueOf(maxConnections)).trim()).intValue();
        sessionTimeout = new Integer(p.getProperty(SERVER_SESSION_TIMEOUT, String.valueOf(sessionTimeout)).trim()).intValue();
        userPersistenceStoreClassname = p.getProperty(SERVER_USER_PERSISTENCE_STORE, userPersistenceStoreClassname).trim();
        channelsFilePath = p.getProperty(SERVER_CHANNELS_FILE, channelsFilePath).trim();
        channelsFilePath = p.getProperty(SERVER_CHANNELS_FILE, channelsFilePath).trim();
        policyOpenDoor = new Boolean(p.getProperty(SERVER_POLICY_OPENDOOR, String.valueOf(policyOpenDoor))).booleanValue();
        readMotdFile(p.getProperty(SERVER_MOTD_FILE, "motd.txt").trim());
    }

    private void readMotdFile(String filename) {
        FileReader fReader = null;
        BufferedReader reader = null;
        try {
            fReader = new FileReader(filename);
            reader = new BufferedReader(fReader);
        } catch (FileNotFoundException fnfe) {
            Logger.getInstance().log(Logger.WARNING, "Message of the day file [" + filename + "] could not be loaded.");
        }
        if (reader != null) {
            Vector lines = new Vector(30);
            try {
                String line = reader.readLine();
                while (line != null) {
                    lines.addElement(line);
                    line = reader.readLine();
                }
                int numLines = lines.size();
                motd = new String[numLines];
                for (int x = 0; x < numLines; x++) {
                    motd[x] = (String) lines.elementAt(x);
                }
            } catch (IOException ioe) {
                Logger.getInstance().log(Logger.WARNING, "I/O Error reading message of the day file. " + ioe.toString());
            }
            try {
                reader.close();
                fReader.close();
            } catch (Exception e) {
            }
        }
        if (motd == null) {
            motd = new String[1];
            motd[0] = "Welcome, this is a jnerve server.";
        }
    }

    public int getServerPort() {
        return serverPort;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public int getSessionTimeout() {
        return sessionTimeout;
    }

    public String getUserPersistenceStoreClassname() {
        return userPersistenceStoreClassname;
    }

    public Properties getProperties() {
        return rawProperties;
    }

    public String[] getMessageOfTheDay() {
        return motd;
    }

    public String getChannelsFilePath() {
        return channelsFilePath;
    }

    public boolean isPolicyOpenDoor() {
        return policyOpenDoor;
    }
}
