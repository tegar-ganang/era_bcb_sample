package net.jawe.scriptbot.impl;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import org.jibble.pircbot.Colors;
import net.jawe.scriptbot.Config;
import net.jawe.scriptbot.MessageType;
import net.jawe.scriptbot.Server;
import net.jawe.scriptbot.factory.Factory;

public class ConfigImpl implements Config {

    private static final String DEFAULT_INIT_SCRIPT_NAME = "scripts/config.js";

    private static final String DEFAULT_DATA_DIR = "data";

    private static final int DEFAULT_MAX_CONNECTION_ATTEMPTS = 10;

    private static final long DEFAULT_CONNECTION_RETRY_DELAY = 15000L;

    private static final boolean DEFAULT_HIDE_PINGPONG = true;

    private static final boolean DEFAULT_START_IDENT_SERVER = false;

    private static final boolean DEFAULT_AUTO_NICK_CHANGE = false;

    private static final String DEFAULT_CHANNEL_COMMAND_PREFIX = "!";

    private static final String DEFAULT_PRIVATE_COMMAND_PREFIX = "";

    private static final String DEFAULT_NOTICE_COMMAND_PREFIX = "";

    private static final String DEFAULT_ACTION_COMMAND_PREFIX = "!";

    private static final String DEFAULT_DCC_COMMAND_PREFIX = ".";

    private static final boolean DEFAULT_AUTO_ADD_HOSTMASKS = false;

    private static final String DEFAULT_HILITE_COLOR = Colors.DARK_BLUE;

    private static final boolean DEFAULT_AUDIT_COMMANDS = true;

    private final LinkedHashSet<String> _nicks;

    private final LinkedHashSet<Server> _servers;

    private final LinkedHashSet<String> _performScripts;

    private final LinkedHashSet<String> _modules;

    private final Set<Integer> _dccPorts;

    private String _id;

    private String _dccInetAddress;

    private String _initScriptName;

    private String _dataDir;

    private int _maxConnectionAttempts;

    private long _connectionRetryDelay;

    private boolean _hidePingPong;

    private String _login;

    private String _finger;

    private boolean _autoNickChange;

    private boolean _startIdentServer;

    private String _encoding;

    private String _channelCommandPrefix;

    private String _privateCommandPrefix;

    private String _noticeCommandPrefix;

    private String _actionCommandPrefix;

    private String _dccCommandPrefix;

    private boolean _autoAddHostmasks;

    private String _hiliteColor;

    private boolean _auditCommands;

    /**
     * 
     */
    public ConfigImpl() {
        _nicks = new LinkedHashSet<String>();
        _servers = new LinkedHashSet<Server>();
        _modules = new LinkedHashSet<String>();
        _performScripts = new LinkedHashSet<String>();
        _dccPorts = new HashSet<Integer>();
        _id = null;
        _dccInetAddress = null;
        _initScriptName = DEFAULT_INIT_SCRIPT_NAME;
        _dataDir = DEFAULT_DATA_DIR;
        _maxConnectionAttempts = DEFAULT_MAX_CONNECTION_ATTEMPTS;
        _connectionRetryDelay = DEFAULT_CONNECTION_RETRY_DELAY;
        _hidePingPong = DEFAULT_HIDE_PINGPONG;
        _login = null;
        _finger = null;
        _autoNickChange = DEFAULT_AUTO_NICK_CHANGE;
        _startIdentServer = DEFAULT_START_IDENT_SERVER;
        _encoding = null;
        _channelCommandPrefix = DEFAULT_CHANNEL_COMMAND_PREFIX;
        _privateCommandPrefix = DEFAULT_PRIVATE_COMMAND_PREFIX;
        _noticeCommandPrefix = DEFAULT_NOTICE_COMMAND_PREFIX;
        _actionCommandPrefix = DEFAULT_ACTION_COMMAND_PREFIX;
        _dccCommandPrefix = DEFAULT_DCC_COMMAND_PREFIX;
        _autoAddHostmasks = DEFAULT_AUTO_ADD_HOSTMASKS;
        _hiliteColor = DEFAULT_HILITE_COLOR;
        _auditCommands = DEFAULT_AUDIT_COMMANDS;
    }

    public String getId() {
        return _id;
    }

    public void setId(String id) {
        _id = id;
    }

    public String getInitScriptName() {
        return _initScriptName;
    }

    public void setInitScriptName(String initScriptName) {
        _initScriptName = initScriptName;
    }

    public String getDataDir() {
        return _dataDir;
    }

    public void setDataDir(String dataDir) {
        _dataDir = dataDir;
    }

    public Set<String> getNicks() {
        return _nicks;
    }

    public String[] getNicksArray() {
        return _nicks.toArray(new String[_nicks.size()]);
    }

    public Set<String> getPerformScripts() {
        return _performScripts;
    }

    public String[] getPerformScriptsArray() {
        return _performScripts.toArray(new String[_performScripts.size()]);
    }

    public Set<String> getModules() {
        return _modules;
    }

    public String[] getModulesArray() {
        return _modules.toArray(new String[_modules.size()]);
    }

    public Set<Server> getServers() {
        return _servers;
    }

    public String getDccInetAddress() {
        return _dccInetAddress;
    }

    public void setDccInetAddress(String dccInetAddress) {
        _dccInetAddress = dccInetAddress;
    }

    public Set<Integer> getDccPorts() {
        return _dccPorts;
    }

    public void addDccPortRange(int from, int to) {
        for (int i = from; i <= to; i++) {
            _dccPorts.add(i);
        }
    }

    public void removeDccPortRange(int from, int to) {
        for (int i = from; i <= to; i++) {
            _dccPorts.remove(i);
        }
    }

    public int getMaxConnectionAttempts() {
        return _maxConnectionAttempts;
    }

    public void setMaxConnectionAttempts(int maxConnectionAttempts) {
        _maxConnectionAttempts = maxConnectionAttempts;
    }

    public long getConnectionRetryDelay() {
        return _connectionRetryDelay;
    }

    public void setConnectionRetryDelay(long connectionRetryDelay) {
        _connectionRetryDelay = connectionRetryDelay;
    }

    public boolean isHidePingPong() {
        return _hidePingPong;
    }

    public void setHidePingPong(boolean hidePingPong) {
        _hidePingPong = hidePingPong;
    }

    public boolean addNick(String nick) {
        return _nicks.add(nick);
    }

    public Server addServer(String hostname) {
        Server server = (Server) Factory.getInstance().getBean("server");
        server.setHostname(hostname);
        _servers.add(server);
        return server;
    }

    public Server addServer(String hostname, int port) {
        Server server = (Server) Factory.getInstance().getBean("server");
        server.setHostname(hostname);
        server.setPort(port);
        _servers.add(server);
        return server;
    }

    public Server addServer(String hostname, int port, String password) {
        Server server = (Server) Factory.getInstance().getBean("server");
        server.setHostname(hostname);
        server.setPort(port);
        server.setPassword(password);
        _servers.add(server);
        return server;
    }

    public boolean addModule(String name) {
        return _modules.add(name);
    }

    public boolean addPerformScript(String filename) {
        return _performScripts.add(filename);
    }

    public String getLogin() {
        return _login;
    }

    public void setLogin(String login) {
        _login = login;
    }

    public boolean isAutoNickChange() {
        return _autoNickChange;
    }

    public void setAutoNickChange(boolean autoNickChange) {
        _autoNickChange = autoNickChange;
    }

    public String getFinger() {
        return _finger;
    }

    public void setFinger(String finger) {
        _finger = finger;
    }

    public boolean isStartIdentServer() {
        return _startIdentServer;
    }

    public void setStartIdentServer(boolean startIdentServer) {
        _startIdentServer = startIdentServer;
    }

    public String getEncoding() {
        return _encoding;
    }

    public void setEncoding(String encoding) {
        _encoding = encoding;
    }

    public String getChannelCommandPrefix() {
        return _channelCommandPrefix;
    }

    public void setChannelCommandPrefix(String channelCommandPrefix) {
        _channelCommandPrefix = channelCommandPrefix;
    }

    public String getActionCommandPrefix() {
        return _actionCommandPrefix;
    }

    public void setActionCommandPrefix(String actionCommandPrefix) {
        _actionCommandPrefix = actionCommandPrefix;
    }

    public String getDccCommandPrefix() {
        return _dccCommandPrefix;
    }

    public void setDccCommandPrefix(String dccCommandPrefix) {
        _dccCommandPrefix = dccCommandPrefix;
    }

    public String getNoticeCommandPrefix() {
        return _noticeCommandPrefix;
    }

    public void setNoticeCommandPrefix(String noticeCommandPrefix) {
        _noticeCommandPrefix = noticeCommandPrefix;
    }

    public String getPrivateCommandPrefix() {
        return _privateCommandPrefix;
    }

    public void setPrivateCommandPrefix(String privateCommandPrefix) {
        _privateCommandPrefix = privateCommandPrefix;
    }

    public String getPrefix(MessageType inputType) {
        String prefix = "";
        if (inputType.equals(MessageType.CHANNEL)) {
            prefix = _channelCommandPrefix;
        } else if (inputType.equals(MessageType.PRIVATE)) {
            prefix = _privateCommandPrefix;
        } else if (inputType.equals(MessageType.NOTICE)) {
            prefix = _noticeCommandPrefix;
        } else if (inputType.equals(MessageType.ACTION)) {
            prefix = _actionCommandPrefix;
        } else if (inputType.equals(MessageType.DCC)) {
            prefix = _dccCommandPrefix;
        }
        return prefix;
    }

    public boolean isAutoAddHostmasks() {
        return _autoAddHostmasks;
    }

    public void setAutoAddHostmasks(boolean autoAddHostmasks) {
        _autoAddHostmasks = autoAddHostmasks;
    }

    public String getHiliteColor() {
        return _hiliteColor;
    }

    public void setHiliteColor(String hiliteColor) {
        _hiliteColor = hiliteColor;
    }

    public boolean isAuditCommands() {
        return _auditCommands;
    }

    public void setAuditCommands(boolean auditCommands) {
        _auditCommands = auditCommands;
    }

    @Override
    public String toString() {
        return super.toString() + " [id=" + _id + ",initScript=" + _initScriptName + "]";
    }
}
