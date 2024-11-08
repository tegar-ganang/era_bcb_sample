package org.jwebsocket.config;

import static org.jwebsocket.config.JWebSocketCommonConstants.WS_SUBPROT_DEFAULT;
import static org.jwebsocket.config.JWebSocketServerConstants.CATALINA_HOME;
import static org.jwebsocket.config.JWebSocketServerConstants.DEFAULT_INSTALLATION;
import static org.jwebsocket.config.JWebSocketServerConstants.DEFAULT_NODE_ID;
import static org.jwebsocket.config.JWebSocketServerConstants.JWEBSOCKET_HOME;
import static org.jwebsocket.config.JWebSocketServerConstants.JWEBSOCKET_XML;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import org.apache.log4j.Logger;
import org.jwebsocket.config.xml.ChannelConfig;
import org.jwebsocket.config.xml.EngineConfig;
import org.jwebsocket.config.xml.FilterConfig;
import org.jwebsocket.config.xml.PluginConfig;
import org.jwebsocket.config.xml.RightConfig;
import org.jwebsocket.config.xml.RoleConfig;
import org.jwebsocket.config.xml.ServerConfig;
import org.jwebsocket.config.xml.UserConfig;
import org.jwebsocket.kit.WebSocketRuntimeException;
import org.jwebsocket.logging.Logging;

/**
 * Represents the jWebSocket configuration. This class is immutable and should
 * not be overridden.
 * 
 * @author puran
 * @version $Id: JWebSocketConfig.java 345 2010-04-10 20:03:48Z fivefeetfurther$
 */
public final class JWebSocketConfig implements Config {

    private static Logger mLog = null;

    private final String mInstallation;

    private final String mNodeId;

    private final String mProtocol;

    private final String jWebSocketHome;

    private final String mLibraryFolder;

    private final String mInitializer;

    private final List<EngineConfig> mEngines;

    private final List<ServerConfig> mServers;

    private final List<UserConfig> mUsers;

    private final List<PluginConfig> mPlugins;

    private final List<FilterConfig> mFilters;

    private final LoggingConfig mLoggingConfig;

    private final List<RightConfig> mGlobalRights;

    private final List<RoleConfig> mGlobalRoles;

    private final List<ChannelConfig> mChannels;

    private static JWebSocketConfig mConfig = null;

    /**
	 * @return the installation
	 */
    public String getInstallation() {
        if (mInstallation == null || mInstallation.length() == 0) {
            return DEFAULT_INSTALLATION;
        }
        return mInstallation;
    }

    /**
	 * @return the protocol
	 */
    public String getProtocol() {
        if (mProtocol == null || mProtocol.length() == 0) {
            return WS_SUBPROT_DEFAULT;
        }
        return mProtocol;
    }

    /**
	 * @return the node-id
	 */
    public String getNodeId() {
        if (mNodeId == null || mNodeId.length() == 0) {
            return DEFAULT_NODE_ID;
        }
        return mNodeId;
    }

    /**
	 * @return the jWebSocketHome
	 */
    public String getjWebSocketHome() {
        return jWebSocketHome;
    }

    /**
	 * @return the libraryFolder
	 */
    public String getLibraryFolder() {
        return mLibraryFolder;
    }

    /**
	 * @return the initializer
	 */
    public String getInitializer() {
        return mInitializer;
    }

    /**
	 * @return the config
	 */
    public static JWebSocketConfig getConfig() {
        return mConfig;
    }

    /**
	 * private constructor used by the builder
	 */
    private JWebSocketConfig(Builder aBuilder) {
        if (aBuilder.mEngines == null || aBuilder.mServers == null || aBuilder.mPlugins == null || aBuilder.mUsers == null || aBuilder.mGlobalRights == null || aBuilder.mGlobalRoles == null || aBuilder.getFilters() == null || aBuilder.mLoggingConfig == null) {
            throw new WebSocketRuntimeException("Configuration is not loaded completely.");
        }
        mInstallation = aBuilder.mInstallation;
        mProtocol = aBuilder.mProtocol;
        mNodeId = aBuilder.mNodeId;
        jWebSocketHome = aBuilder.jWebSocketHome;
        mLibraryFolder = aBuilder.mLibraryFolder;
        mInitializer = aBuilder.mInitializer;
        mEngines = aBuilder.mEngines;
        mServers = aBuilder.mServers;
        mUsers = aBuilder.mUsers;
        mPlugins = aBuilder.mPlugins;
        mFilters = aBuilder.getFilters();
        mLoggingConfig = aBuilder.mLoggingConfig;
        mGlobalRights = aBuilder.mGlobalRights;
        mGlobalRoles = aBuilder.mGlobalRoles;
        mChannels = aBuilder.mChannels;
        validate();
    }

    /**
	 * Config builder class.
	 *
	 * @author puran
	 * @version $Id: JWebSocketConfig.java 596 2010-06-22 17:09:54Z
	 *          fivefeetfurther $
	 */
    public static class Builder {

        private String mInstallation;

        private String mProtocol;

        private String mNodeId;

        private String jWebSocketHome;

        private String mLibraryFolder;

        private String mInitializer;

        private List<EngineConfig> mEngines;

        private List<ServerConfig> mServers;

        private List<UserConfig> mUsers;

        private List<PluginConfig> mPlugins;

        private List<FilterConfig> mFilters;

        private LoggingConfig mLoggingConfig;

        private List<RightConfig> mGlobalRights;

        private List<RoleConfig> mGlobalRoles;

        private List<ChannelConfig> mChannels;

        public Builder setInstallation(String aInstallation) {
            mInstallation = aInstallation;
            return this;
        }

        public Builder setProtocol(String aProtocol) {
            mProtocol = aProtocol;
            return this;
        }

        public Builder setNodeId(String aNodeId) {
            mNodeId = aNodeId;
            return this;
        }

        public Builder setJWebSocketHome(String aJWebSocketHome) {
            jWebSocketHome = aJWebSocketHome;
            return this;
        }

        public Builder setInitializer(String aInitializer) {
            mInitializer = aInitializer;
            return this;
        }

        public Builder setLibraryFolder(String aLibraryFolder) {
            mLibraryFolder = aLibraryFolder;
            return this;
        }

        public Builder setEngines(List<EngineConfig> aEngines) {
            mEngines = aEngines;
            return this;
        }

        public Builder setServers(List<ServerConfig> aServers) {
            mServers = aServers;
            return this;
        }

        public Builder setPlugins(List<PluginConfig> aPlugins) {
            mPlugins = aPlugins;
            return this;
        }

        public Builder setFilters(List<FilterConfig> aFilters) {
            mFilters = aFilters;
            return this;
        }

        public Builder setLoggingConfig(List<LoggingConfig> aLoggingConfigs) {
            mLoggingConfig = aLoggingConfigs.get(0);
            return this;
        }

        public Builder setGlobalRights(List<RightConfig> aRights) {
            mGlobalRights = aRights;
            return this;
        }

        public Builder setGlobalRoles(List<RoleConfig> aRoles) {
            mGlobalRoles = aRoles;
            return this;
        }

        public Builder setUsers(List<UserConfig> aUsers) {
            mUsers = aUsers;
            return this;
        }

        public Builder setChannels(List<ChannelConfig> aChannels) {
            mChannels = aChannels;
            return this;
        }

        public synchronized JWebSocketConfig buildConfig() {
            if (mConfig == null) {
                mConfig = new JWebSocketConfig(this);
            }
            return mConfig;
        }

        /**
		 * @return the filters
		 */
        public List<FilterConfig> getFilters() {
            return mFilters;
        }
    }

    /**
	 * @return the engines
	 */
    public List<EngineConfig> getEngines() {
        return Collections.unmodifiableList(mEngines);
    }

    /**
	 * @return the servers
	 */
    public List<ServerConfig> getServers() {
        return Collections.unmodifiableList(mServers);
    }

    /**
	 * @return the users
	 */
    public List<UserConfig> getUsers() {
        return Collections.unmodifiableList(mUsers);
    }

    /**
	 * @return the plugins
	 */
    public List<PluginConfig> getPlugins() {
        return Collections.unmodifiableList(mPlugins);
    }

    /**
	 * @return the filters
	 */
    public List<FilterConfig> getFilters() {
        return Collections.unmodifiableList(mFilters);
    }

    /**
	 * @return the logging config object
	 */
    public LoggingConfig getLoggingConfig() {
        return mLoggingConfig;
    }

    /**
	 * @return the globalRights
	 */
    public List<RightConfig> getGlobalRights() {
        return Collections.unmodifiableList(mGlobalRights);
    }

    /**
	 * @return the globalRoles
	 */
    public List<RoleConfig> getGlobalRoles() {
        return Collections.unmodifiableList(mGlobalRoles);
    }

    /**
	 * @return the channels
	 */
    public List<ChannelConfig> getChannels() {
        return Collections.unmodifiableList(mChannels);
    }

    /**
	 * {@inheritDoc}
	 */
    @Override
    public void validate() {
        if ((mEngines == null || mEngines.isEmpty()) || (mServers == null || mServers.isEmpty()) || (mUsers == null || mUsers.isEmpty()) || (mPlugins == null || mPlugins.isEmpty()) || (mFilters == null) || (mLoggingConfig == null) || (mGlobalRights == null || mGlobalRights.isEmpty()) || (mGlobalRoles == null || mGlobalRoles.isEmpty())) {
            throw new WebSocketRuntimeException("Missing one of the server configuration, please check your configuration file");
        }
    }

    private static void checkLogs() {
        if (mLog == null) {
            mLog = Logging.getLogger(JWebSocketConfig.class);
        }
    }

    /**
	 * private method that checks the path of the jWebSocket.xml file
	 * @return the path to jWebSocket.xml
	 */
    public static String getConfigurationPath() {
        String lWebSocketXML = null;
        String lWebSocketHome = null;
        String lFileSep = System.getProperty("file.separator");
        File lFile;
        lWebSocketHome = System.getenv(JWEBSOCKET_HOME);
        if (lWebSocketHome != null) {
            if (!lWebSocketHome.endsWith(lFileSep)) {
                lWebSocketHome += lFileSep;
            }
            lWebSocketXML = lWebSocketHome + "conf" + lFileSep + JWEBSOCKET_XML;
            lFile = new File(lWebSocketXML);
            if (lFile.exists()) {
                return lWebSocketXML;
            }
        }
        lWebSocketHome = System.getenv(CATALINA_HOME);
        if (lWebSocketHome != null) {
            if (!lWebSocketHome.endsWith(lFileSep)) {
                lWebSocketHome += lFileSep;
            }
            lWebSocketXML = lWebSocketHome + "conf" + lFileSep + JWEBSOCKET_XML;
            lFile = new File(lWebSocketXML);
            if (lFile.exists()) {
                return lWebSocketXML;
            }
        }
        URL lURL = Thread.currentThread().getContextClassLoader().getResource("conf/" + JWEBSOCKET_XML);
        if (lURL != null) {
            try {
                URI lFilename = lURL.toURI();
                lFile = new File(lFilename);
                if (lFile.exists()) {
                    lWebSocketXML = lFile.getPath();
                    return lWebSocketXML;
                }
            } catch (Exception ex) {
            }
        }
        return null;
    }

    /**
	 * private method that checks the path of the jWebSocket.xml file
	 *
	 * @return the path to jWebSocket.xml
	 */
    public static String getLibraryFolderPath(String fileName) {
        String lWebSocketLib = null;
        String lWebSocketHome = null;
        String lFileSep = null;
        File lFile = null;
        checkLogs();
        lWebSocketHome = System.getenv(JWEBSOCKET_HOME);
        lFileSep = System.getProperty("file.separator");
        if (lWebSocketHome != null) {
            if (!lWebSocketHome.endsWith(lFileSep)) {
                lWebSocketHome += lFileSep;
            }
            lWebSocketLib = lWebSocketHome + "libs" + lFileSep + fileName;
            lFile = new File(lWebSocketLib);
            if (lFile.exists()) {
                if (mLog.isDebugEnabled()) {
                    mLog.debug("Found lib at " + lWebSocketLib + "...");
                }
                return lWebSocketLib;
            } else {
                if (mLog.isDebugEnabled()) {
                    mLog.debug(fileName + " not found at %" + JWEBSOCKET_HOME + "%/libs.");
                }
            }
        }
        lWebSocketHome = System.getenv(CATALINA_HOME);
        lFileSep = System.getProperty("file.separator");
        if (lWebSocketHome != null) {
            if (!lWebSocketHome.endsWith(lFileSep)) {
                lWebSocketHome += lFileSep;
            }
            lWebSocketLib = lWebSocketHome + "lib" + lFileSep + fileName;
            lFile = new File(lWebSocketLib);
            if (lFile.exists()) {
                if (mLog.isDebugEnabled()) {
                    mLog.debug("Found lib at " + lWebSocketLib + "...");
                }
                return lWebSocketLib;
            } else {
                if (mLog.isDebugEnabled()) {
                    mLog.debug(fileName + " not found at %" + CATALINA_HOME + "/lib%.");
                }
            }
        }
        return null;
    }
}
