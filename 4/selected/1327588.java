package main;

import gamebot.GameBot;
import gamebot.IrcCallback;
import games.Game;
import games.GameLoader;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import logging.DateTimeFormatter;
import logging.PlainTextFormatter;
import util.IniFile;

public class GameManager {

    private static final Logger logger = Logger.getLogger("system");

    private static final File LOG_DIRECTORY = new File("log");

    private static final String GAMES_DIRECTORY = "games";

    private static final String GAME_INFO_FILE = "gameinfo.ini";

    private static final String GAME_LOADER_PROPERTY_NAME = "gameloader";

    private static final String DEFAULT_GAME_LOADER = "games.defaultgame.DefaultGameLoader";

    public void initSystemLogging() throws Throwable {
        try {
            if (!LOG_DIRECTORY.exists()) LOG_DIRECTORY.mkdirs();
            String logFileName = "log.txt";
            FileHandler fileLogger = new FileHandler(LOG_DIRECTORY + "/" + logFileName, false);
            fileLogger.setFormatter(new DateTimeFormatter());
            fileLogger.setLevel(Level.ALL);
            Handler consoleLogger = new Handler() {

                @Override
                public void close() throws SecurityException {
                }

                @Override
                public void flush() {
                    System.out.flush();
                }

                @Override
                public void publish(LogRecord record) {
                    if (this.getLevel().intValue() >= record.getLevel().intValue()) System.out.print(new PlainTextFormatter().format(record));
                }
            };
            consoleLogger.setLevel(Level.OFF);
            logger.addHandler(consoleLogger);
            logger.addHandler(fileLogger);
            logger.setUseParentHandlers(false);
            logger.setLevel(Level.FINE);
            logger.info("Logging initiated");
        } catch (Throwable t) {
            logger.info("Error during initialization of logging: " + t);
            throw t;
        }
    }

    public void createGame(GameSetup setup) {
        logger.info("Creating game");
        String gameName = setup.getGameName();
        File gameDir = new File(GAMES_DIRECTORY + File.separator + gameName);
        String hostname = setup.getHostName();
        String botName = setup.getBotName();
        String botClassName = setup.getBotClassName();
        String botMode = setup.getBotMode();
        String hostPassword = setup.getPassword();
        String identify = setup.getIdentify();
        int port = setup.getPort();
        Map<String, Object> setupProperties = setup.getProperties();
        try {
            GameBot bot = parseBotClass(botClassName);
            bot.setLogDirectory(LOG_DIRECTORY);
            bot.setBotName(botName);
            Game game = parseGame(gameDir, bot.getIrcCallback(), setupProperties);
            bot.setGame(game);
            if (!hostPassword.isEmpty()) bot.connectToHost(hostname, port, hostPassword); else bot.connectToHost(hostname, port);
            if (!identify.isEmpty()) bot.identify(identify);
            if (botMode != null && !botMode.isEmpty()) {
                logger.info("Setting mode " + botMode);
                bot.setMode(botMode);
            }
            for (Channel channel : setup.getChannels()) {
                String channelName = channel.getName();
                String channelPassword = channel.getPassword();
                if (!channelName.startsWith("#")) channelName = "#" + channelName;
                logger.info("Joining channel: " + channelName);
                if (channelPassword.isEmpty()) bot.connectToChannel(channelName); else bot.connectToChannel(channelName, channelPassword);
                String channelMode = channel.getMode();
                if (channelMode != null && !channelMode.isEmpty()) {
                    logger.info("Setting mode to " + channelMode);
                    bot.setMode(channelName, channelMode);
                }
                String topic = channel.getTopic();
                if (topic != null && !topic.isEmpty()) {
                    logger.info("Setting topic to '" + topic + "'");
                    bot.setTopic(channelName, topic);
                }
            }
        } catch (Throwable e) {
            logger.log(Level.SEVERE, "Error while creating game", e);
            e.printStackTrace();
        }
    }

    private static GameBot parseBotClass(String botClassName) throws Throwable {
        GameBot bot = null;
        try {
            Class<GameBot> botClass = getClass(botClassName);
            bot = botClass.getConstructor().newInstance();
        } catch (ClassCastException e) {
            logger.severe("Error: bot class " + botClassName + " does not implement interface gamebot.GameBot");
            throw e;
        } catch (InstantiationException e) {
            logger.severe("Error: bot class: " + botClassName + " is an abstract class or interface");
        } catch (NoSuchMethodException e) {
            logger.severe("Error: bot class " + botClassName + " does not have a default constructor");
            throw e;
        } catch (ClassNotFoundException e) {
            logger.severe("Error: bot class " + botClassName + " not found");
            throw e;
        } catch (Throwable t) {
            logError("parsing bot class", t);
            throw t;
        }
        return bot;
    }

    private static Game parseGame(File gameDirectory, IrcCallback ircCallback, Map<String, Object> setupProperties) throws Throwable {
        Game game = null;
        File gameInfoFile = new File(gameDirectory + File.separator + GAME_INFO_FILE);
        String gameLoaderClassName = DEFAULT_GAME_LOADER;
        try {
            IniFile gameIniFile = new IniFile(gameInfoFile);
            Map<String, String> gameOptions = gameIniFile.read(new String[] { GAME_LOADER_PROPERTY_NAME });
            if (gameOptions.containsKey(GAME_LOADER_PROPERTY_NAME)) gameLoaderClassName = gameOptions.get(GAME_LOADER_PROPERTY_NAME);
        } catch (FileNotFoundException e) {
            logger.severe("Game file " + gameInfoFile + " not found");
            throw e;
        }
        logger.info("Gameloader class: " + gameLoaderClassName);
        GameLoader loader = null;
        try {
            Class<GameLoader> gameLoaderClass = getClass(gameLoaderClassName);
            loader = gameLoaderClass.getConstructor().newInstance();
        } catch (ClassCastException e) {
            logger.severe("Error: game loader class " + gameLoaderClassName + " does not implement interface gamebot.GameLoader");
            throw e;
        } catch (ClassNotFoundException e) {
            logger.severe("Error: game loader class " + gameLoaderClassName + " not found");
            throw e;
        } catch (IllegalArgumentException e) {
            logger.severe("Error: game loader class " + gameLoaderClassName + " does not have a default constructor");
            throw e;
        } catch (Exception e) {
            logError("loading game", e);
            throw e;
        }
        if (loader != null) game = loader.load(gameInfoFile, ircCallback, setupProperties);
        return game;
    }

    private static void logError(String activity, Throwable t) {
        logger.severe("Error while " + activity + ": " + t);
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<T> getClass(String className) throws ClassNotFoundException {
        return (Class<T>) Class.forName(className);
    }
}
