package com.usoog.hextd;

import com.usoog.commons.gamecore.UserInfo;
import com.usoog.commons.gamecore.map.MapInfo;
import com.usoog.commons.gamecore.message.GameInfo;
import com.usoog.commons.gamecore.message.MessageError;
import com.usoog.commons.gamecore.message.MessageFetch;
import com.usoog.commons.gamecore.message.MessageGameList;
import com.usoog.commons.gamecore.message.MessageMapData;
import com.usoog.commons.gamecore.message.MessageMapList;
import com.usoog.commons.gamecore.message.MessageReplay;
import com.usoog.commons.network.ConnectionListener;
import com.usoog.commons.network.NetworkServer;
import com.usoog.commons.network.NetworkServerConnection;
import com.usoog.commons.network.message.Message;
import com.usoog.hextd.Constants.FetchType;
import com.usoog.hextd.server.MapLoaderServer;
import com.usoog.hextd.server.Channel;
import com.usoog.hextd.server.PersistantReplay;
import com.usoog.hextd.server.PersistantUserInfo;
import com.usoog.hextd.server.ReplayLogChecker;
import com.usoog.hextd.server.ServerUser;
import com.usoog.tdcore.message.TDFactoryMessage;
import java.io.BufferedReader;
import java.io.Console;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import javax.jdo.Extent;
import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import javax.jdo.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Server implements ConnectionListener {

    /**
	 * The logger for this class.
	 */
    private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);

    public static TDFactoryMessage messageFactory = new TDFactoryMessage();

    private List<ServerUser> users;

    private List<ServerUser> usersNew;

    private List<String> userNames;

    private Map<Integer, ServerUser> usersByUserId;

    private Map<Integer, Channel> channels;

    private Channel lobby;

    private int nextChannelId = 0;

    private int port = 4567;

    private URL replayFetchUrl;

    private NetworkServer serverCore;

    private MapLoaderServer mapLoader;

    private List<Integer> logsToCheck;

    private ReplayLogChecker logChecker;

    private Properties settings;

    private long cachCleanupPeriod = 1l * 30 * 60 * 1000;

    private long channelResendPeriod = 1l * 30 * 1000;

    private Timer maintenanceTimer, countdownTimer;

    private PersistenceManagerFactory pmf;

    public Server(Properties config) {
        settings = config;
        pmf = JDOHelper.getPersistenceManagerFactory("datanucleus.properties");
        port = new Integer(settings.getProperty("port"));
        System.out.println("Starting server on port: " + port);
        maintenanceTimer = new Timer("maintenanceTimer", true);
        countdownTimer = new Timer("countdownTimer", true);
        users = new ArrayList<ServerUser>();
        usersNew = new ArrayList<ServerUser>();
        userNames = new ArrayList<String>();
        usersByUserId = new HashMap<Integer, ServerUser>();
        channels = new HashMap<Integer, Channel>();
        Integer channelId = new Integer(nextChannelId);
        nextChannelId++;
        lobby = new Channel(this, "The Lobby", channelId, countdownTimer);
        channels.put(channelId, lobby);
        mapLoader = new MapLoaderServer(pmf, "maps/levels.txt", getClass().getResource("/resources/styles.css"));
        mapLoader.fetchRemoteIndex();
        logsToCheck = Collections.synchronizedList(new ArrayList<Integer>());
        logChecker = new ReplayLogChecker(this, pmf, logsToCheck, mapLoader, maintenanceTimer);
        logChecker.startCheckTimer();
        TimerTask maplistChecker = new TimerTask() {

            @Override
            public void run() {
                mapLoader.fetchRemoteIndex();
            }
        };
        maintenanceTimer.schedule(maplistChecker, cachCleanupPeriod, cachCleanupPeriod);
        TimerTask channelListSender = new TimerTask() {

            @Override
            public void run() {
                sendChannelList();
            }
        };
        maintenanceTimer.schedule(channelListSender, channelResendPeriod, channelResendPeriod);
    }

    public void startServer() throws IOException {
        serverCore = new NetworkServer(messageFactory);
        serverCore.setPort(port);
        serverCore.addConnectionListener(this);
        serverCore.start();
    }

    public Map<Integer, Channel> getChannels() {
        return this.channels;
    }

    /**
	 * Load the user info of the user with the given token
	 *
	 * @param token The login token supplied by the user
	 * @return a UserInfo object with the user's info.
	 */
    public UserInfo loadUserInfo(String token) {
        PersistenceManager pm = pmf.getPersistenceManager();
        Transaction tx = pm.currentTransaction();
        UserInfo user = null;
        ServerUser oldUser;
        try {
            tx.begin();
            Extent<PersistantUserInfo> e = pm.getExtent(PersistantUserInfo.class, true);
            Query q = pm.newQuery(e, "token=='" + token + "'");
            Collection<PersistantUserInfo> c = (Collection<PersistantUserInfo>) q.execute();
            for (PersistantUserInfo pUser : c) {
                user = pUser.getUserInfo(user);
                oldUser = usersByUserId.get(user.getUserId());
                if (oldUser != null) {
                    user = oldUser.getUserInfo();
                }
                break;
            }
            tx.commit();
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
        }
        pm.close();
        return user;
    }

    /**
	 * Load the user info of the user with the given ID
	 *
	 * @param userId The login token supplied by the user
	 * @return a UserInfo object with the user's info.
	 */
    public UserInfo loadUserInfo(int userId) {
        PersistenceManager pm = pmf.getPersistenceManager();
        Transaction tx = pm.currentTransaction();
        UserInfo user = null;
        ServerUser oldUser;
        try {
            tx.begin();
            Extent<PersistantUserInfo> e = pm.getExtent(PersistantUserInfo.class, true);
            Query q = pm.newQuery(e, "userId=='" + userId + "'");
            Collection<PersistantUserInfo> c = (Collection<PersistantUserInfo>) q.execute();
            for (PersistantUserInfo pUser : c) {
                user = pUser.getUserInfo(user);
                oldUser = usersByUserId.get(user.getUserId());
                if (oldUser != null) {
                    user = oldUser.getUserInfo();
                }
                break;
            }
            tx.commit();
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
        }
        pm.close();
        return user;
    }

    /**
	 * Updates the user info of one user in the database.
	 * @param userInfo
	 */
    public void storeUserInfo(UserInfo userInfo) {
        PersistenceManager pm = pmf.getPersistenceManager();
        Transaction tx = pm.currentTransaction();
        try {
            tx.begin();
            Extent<PersistantUserInfo> e = pm.getExtent(PersistantUserInfo.class, true);
            Query q = pm.newQuery(e, "userId=='" + userInfo.getUserId() + "'");
            Collection<PersistantUserInfo> c = (Collection<PersistantUserInfo>) q.execute();
            if (c.size() == 1) {
                PersistantUserInfo pUser;
                for (PersistantUserInfo temp : c) {
                    pUser = temp;
                    pUser.setUserInfo(userInfo);
                    break;
                }
            } else {
                LOGGER.error("Fetching users with id: {} returned irregular number of results: {}", new Object[] { userInfo.getUserId(), c.size() });
            }
            tx.commit();
        } finally {
            if (tx.isActive()) {
                LOGGER.error("Failure updating userInfo for user: {}", userInfo.getUserId());
                tx.rollback();
            }
        }
        pm.close();
    }

    public boolean authenticatePlayer(ServerUser user, String token) {
        boolean success = false;
        String reason = "";
        if (token.equalsIgnoreCase("invalid")) {
            reason = " Not logged in.";
        } else {
            UserInfo userInfo = loadUserInfo(token);
            if (userInfo != null) {
                if (userInfo != null && usersByUserId.get(userInfo.getUserId()) != null && token.equals("TestToken")) {
                    userInfo = loadUserInfo(token + "2");
                }
                if (userInfo != null && usersByUserId.get(userInfo.getUserId()) != null && token.equals("TestToken")) {
                    userInfo = loadUserInfo(token + "3");
                }
            }
            if (userInfo != null) {
                user.setUserInfo(userInfo);
                userNames.add(userInfo.getName());
                users.add(user);
                usersNew.remove(user);
                usersByUserId.put(userInfo.getUserId(), user);
                success = true;
            }
            if (!success) {
                reason += "User not found";
            }
        }
        if (!success) {
            System.out.println("Server::playerAuthenticated: Auth failed for token '" + token + "' because: " + reason);
            user.sendMessage(new MessageError(Constants.ErrorType.AuthFailed.name(), reason));
        }
        return success;
    }

    public void playerDisconnected(ServerUser user) {
        UserInfo userInfo = user.getUserInfo();
        if (userInfo != null) {
            System.out.println("Server::playerDisconnected: " + userInfo.getName());
        }
        Channel channel = user.getChannel();
        if (channel != null) {
            channel.playerLeft(user, "nowhere (Disconnected)");
        }
        this.users.remove(user);
        this.usersNew.remove(user);
        if (userInfo != null) {
            this.userNames.remove(userInfo.getName());
            this.usersByUserId.remove(userInfo.getUserId());
        }
    }

    public void playerToChannel(ServerUser player, Integer channelId) {
        if (channels.containsKey(channelId)) {
            Channel target = channels.get(channelId);
            Channel from = player.getChannel();
            if (target.isOpen()) {
                if (from != null) {
                    from.playerLeft(player, target.getName());
                }
                target.playerJoined(player);
                if (target == lobby) {
                    player.sendMessage(new MessageGameList(createGameInfoList()));
                }
            }
        } else if (channelId.intValue() == -1) {
            Channel from = player.getChannel();
            if (from != null) {
                player.setChannel(null);
                from.playerLeft(player, "nowhere (Single Player)");
                System.out.println("Server::playerToChannel: " + player.getUserInfo().getName() + "-> null");
            }
        }
    }

    private List<GameInfo> createGameInfoList() {
        ArrayList<GameInfo> gameList = new ArrayList<GameInfo>();
        for (Channel c : channels.values()) {
            gameList.add(c.getGameInfo());
        }
        return gameList;
    }

    public void channelEmpty(Channel channel) {
        if (channel.getId() > 0) {
            channels.remove(channel.getId());
            lobby.sendMessage(new MessageGameList(createGameInfoList()));
            System.out.println("Server::channelEmpty: removing channel " + channel.getId() + " " + channel.getName());
        }
    }

    public void createChannel(String name, ServerUser player) {
        Integer channelId;
        synchronized (this) {
            channelId = new Integer(nextChannelId);
            nextChannelId++;
        }
        String finalName = name;
        System.out.println("Server::createChannel: creating channel " + channelId + " " + finalName);
        Channel newChannel = new Channel(this, finalName, channelId, countdownTimer);
        channels.put(channelId, newChannel);
        sendChannelList();
        playerToChannel(player, channelId);
    }

    private void sendChannelList() {
        lobby.sendMessage(new MessageGameList(createGameInfoList()));
    }

    @Override
    public void connectionEstablished(NetworkServerConnection connection) {
        System.out.println("Server::connectionEstablished: New connection from " + connection.getRemoteAddress().toString());
        ServerUser newPlayer = new ServerUser(this);
        newPlayer.setConnection(connection);
        this.usersNew.add(newPlayer);
        MessageMapList mapList = mapLoader.getMapListMessage();
        if (mapList != null) {
            System.out.println("Server::connectionEstablished: Sending map list");
            newPlayer.sendMessage(mapList);
        }
    }

    public int getAuthedPlayerCount() {
        return this.users.size();
    }

    public int getUnauthedPlayerCount() {
        return this.usersNew.size();
    }

    public int getChannelCount() {
        return this.channels.size();
    }

    /**
	 * Add a single player replay to the set of replays to check.
	 *
	 * @param logMessage The message containing the replay.
	 */
    public void addSpReplay(MessageReplay logMessage) {
        Map<String, String> options = logMessage.getOptions();
        PersistantReplay pReplay = new PersistantReplay();
        String mapIdString = options.get(Constants.settingKey.mapId.name());
        String publicString = options.get(Constants.settingKey.publicGame.name());
        try {
            pReplay.setMapId(Integer.parseInt(mapIdString));
        } catch (Exception e) {
            System.out.println("Server::addMpReplay: MP replay with no mapId!");
            return;
        }
        try {
            pReplay.setPublic(Boolean.parseBoolean(publicString));
        } catch (Exception e) {
            pReplay.setPublic(true);
        }
        pReplay.setType(PersistantReplay.ReplayType.SINGLE);
        pReplay.addUserId(logMessage.getSenderId());
        pReplay.setWinningUserId(-1);
        pReplay.setReplayLog(logMessage.getReplay());
        PersistenceManager pm = pmf.getPersistenceManager();
        Transaction tx = pm.currentTransaction();
        try {
            tx.begin();
            pReplay = pm.makePersistent(pReplay);
            tx.commit();
            System.out.println("Server::addSpReplay: added SP replay to database: " + pReplay.getReplayId());
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
        }
        pm.close();
        logsToCheck.add(pReplay.getReplayId());
    }

    /**
	 * Add a multi player replay to the set of replays to check.
	 *
	 * @param logMessage The message containing the replay.
	 */
    public void addMpReplay(MessageReplay logMessage) {
        Map<String, String> options = logMessage.getOptions();
        PersistantReplay pReplay = new PersistantReplay();
        String mapIdString = options.get(Constants.settingKey.mapId.name());
        String publicString = options.get(Constants.settingKey.publicGame.name());
        String ladderString = options.get(Constants.settingKey.ladderGame.name());
        try {
            pReplay.setMapId(Integer.parseInt(mapIdString));
        } catch (Exception e) {
            System.out.println("Server::addMpReplay: MP replay with no mapId!");
            return;
        }
        try {
            pReplay.setPublic(Boolean.parseBoolean(publicString));
        } catch (Exception e) {
            pReplay.setPublic(true);
        }
        try {
            pReplay.setLadder(Boolean.parseBoolean(ladderString));
        } catch (Exception e) {
            pReplay.setLadder(true);
        }
        pReplay.setType(PersistantReplay.ReplayType.MULTI);
        pReplay.setWinningUserId(-1);
        pReplay.setReplayLog(logMessage.getReplay());
        PersistenceManager pm = pmf.getPersistenceManager();
        Transaction tx = pm.currentTransaction();
        try {
            tx.begin();
            pReplay = pm.makePersistent(pReplay);
            tx.commit();
            System.out.println("Server::addMpReplay: added MP replay to database: " + pReplay.getReplayId());
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
        }
        pReplay = pm.detachCopy(pReplay);
        pm.close();
        logsToCheck.add(pReplay.getReplayId());
    }

    public MapLoaderServer getMapLoader() {
        return mapLoader;
    }

    public void fetchFromServer(MessageFetch mf, ServerUser from) {
        UserInfo playerInfo = from.getUserInfo();
        if (playerInfo == null) {
            playerInfo = new UserInfo();
            playerInfo.setName(from.getConnection().getRemoteAddress().toString());
        }
        try {
            FetchType type = Constants.FetchType.valueOf(mf.getType());
            String playType;
            switch(type) {
                case maplist:
                    System.out.println("Server::fetchFromServer: Sending maplist to " + playerInfo.getName() + " (" + playerInfo.getUserId() + ")");
                    from.sendMessage(mapLoader.getMapListMessage());
                    return;
                case map:
                    MapInfo info = mapLoader.getMap(mf.getId());
                    if (info != null) {
                        System.out.println("Server::fetchFromServer: Sending map " + mf.getId() + " to " + playerInfo.getName() + " (" + playerInfo.getUserId() + ")");
                        from.sendMessage(new MessageMapData(info.getMapId(), info.getContent()));
                    } else {
                        LOGGER.warn("Request for unknown map: {} from {} ({})", new Object[] { mf.getId(), playerInfo.getName(), playerInfo.getUserId() });
                    }
                    return;
                case mp:
                    playType = "multi";
                    break;
                case sp:
                    playType = "single";
                    break;
                default:
                    System.err.println("Server::fetchFromServer: Unknown fetch action!");
                    return;
            }
            System.err.println("Server::fetchFromServer: Fetching " + mf.getId() + " " + playType + " by " + from.getUserInfo().getUserId());
            URLConnection connection = replayFetchUrl.openConnection();
            connection.setDoOutput(true);
            OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
            out.write(URLEncoder.encode("type", "UTF-8") + "=" + type + "&");
            out.write(URLEncoder.encode("replayId", "UTF-8") + "=" + mf.getId() + "&");
            out.write(URLEncoder.encode("requestingUserId", "UTF-8") + "=" + from.getUserInfo().getUserId() + "&");
            out.close();
            StringBuilder result = new StringBuilder();
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String receivedLine;
            Message replayMsg = null;
            while ((receivedLine = in.readLine()) != null) {
                result.append(receivedLine).append("\n");
                Message message;
                try {
                    message = messageFactory.parseString(receivedLine);
                    if (message.getKey().equals(MessageReplay.KEY)) {
                        replayMsg = message;
                    }
                } catch (Exception ex) {
                    LOGGER.warn("Error!", ex);
                }
            }
            in.close();
            if (replayMsg != null) {
                System.err.println("Server::fetchReplay: Got Replay!");
                try {
                    from.sendMessage(replayMsg);
                } catch (Exception ex) {
                    LOGGER.warn("Error!", ex);
                }
            } else {
                System.err.println("Server::fetchReplay: Got no Replay!");
                System.err.println(result);
                from.sendMessage(new MessageError(Constants.ErrorType.ReplayLoadFailed.name(), "No reason known"));
            }
        } catch (IOException ex) {
            System.err.println("Server::fetchReplay: IOException submitting score!");
        }
    }

    public List<ServerUser> getPlayers() {
        return users;
    }

    public List<ServerUser> getPlayersNew() {
        return usersNew;
    }

    public void checkMaps() {
        mapLoader.fetchRemoteIndex();
    }

    public void stop() {
        serverCore.stop();
    }

    /**
	 * @param args the command line arguments
	 */
    public static void main(String[] args) {
        Map<String, String> versions = new HashMap<String, String>();
        String versionsString = "";
        versionsString = HexTD.getVersions(versions);
        Properties defaults = new Properties();
        defaults.setProperty("port", "4567");
        Properties config = new Properties(defaults);
        try {
            config.load(new FileInputStream("HexTD.conf"));
        } catch (FileNotFoundException ex) {
            try {
                System.out.println("Creating config file, " + defaults.size() + " keys");
                defaults.store(new FileWriter("HexTD.conf"), "Default settings.");
            } catch (IOException ex1) {
                LOGGER.warn("Error!", ex1);
            }
        } catch (IOException ex) {
            LOGGER.warn("Error!", ex);
        }
        Console c = System.console();
        if (c == null) {
            try {
                System.err.println("No console.");
                Server server = new Server(config);
                server.startServer();
            } catch (IOException ex) {
                LOGGER.warn("Error!", ex);
            }
        } else {
            try {
                Server server = new Server(config);
                server.startServer();
                boolean quit = false;
                do {
                    String line = c.readLine(">> ");
                    if (line.matches("^i$")) {
                        for (Entry<String, String> es : versions.entrySet()) {
                            System.out.println(es.getKey() + " " + es.getValue());
                        }
                        System.out.println("Players Auth: " + server.getAuthedPlayerCount() + " Players notAuth: " + server.getUnauthedPlayerCount() + " Channels: " + server.getChannelCount());
                        System.out.println("Unchecked logs: " + server.logsToCheck.size());
                    }
                    if (line.matches("^m$")) {
                        server.checkMaps();
                    }
                    if (line.matches("^p$")) {
                        System.out.println("Authenticated players:");
                        for (ServerUser p : server.getPlayers()) {
                            System.out.println(" * " + p.getUserInfo().getName() + " " + p.getConnection().getRemoteAddress() + " " + p.getChannel());
                        }
                        System.out.println("Unauthenticated players:");
                        for (ServerUser p : server.getPlayersNew()) {
                            System.out.println(" * " + p.getConnection().getRemoteAddress());
                        }
                    }
                    quit = line.matches("^q(uit)?$");
                } while (!quit);
                server.stop();
                System.exit(0);
            } catch (IOException ex) {
                LOGGER.warn("Error!", ex);
            }
        }
    }
}
