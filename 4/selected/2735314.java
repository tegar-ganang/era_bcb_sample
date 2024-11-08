package com.clanwts.bots.epicwar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import org.apache.commons.lang.StringUtils;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.Tag;
import org.htmlparser.filters.NodeClassFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import com.clanwts.bncs.bot.BattleNetChatBot;
import com.clanwts.bncs.bot.SimpleBattleNetChatBot;
import com.clanwts.bncs.client.BattleNetChatClientFactory;
import com.clanwts.bncs.codec.standard.messages.Platform;
import com.clanwts.bncs.codec.standard.messages.Product;

public class EpicWarDownloaderBot extends SimpleBattleNetChatBot {

    private static final String TRIGGER = "!";

    private static final int ROLL_MAX_DFL = 100;

    private static final File CFG_DIR = new File("/home/nathan/ghost-1.23/bots/common/maps/custom/cfg/");

    private static final File MAP_DIR = new File("/home/nathan/ghost-1.23/bots/common/maps/custom/files/");

    private static final String GHOST_DB_HOST = "localhost";

    private static final int GHOST_DB_PORT = 3306;

    private static final String GHOST_DB_SCHEMA = "ghost-elite_hbot";

    private static final String GHOST_DB_URL = String.format("jdbc:mysql://%s:%d/%s", GHOST_DB_HOST, GHOST_DB_PORT, GHOST_DB_SCHEMA);

    private static final String GHOST_DB_USER = "root";

    private static final String GHOST_DB_PASS = "186000";

    private static final String SYSADMIN_ACCT = "RichardNixon";

    public EpicWarDownloaderBot(BattleNetChatClientFactory fact, String host, int port, String user, String pass, boolean pvpgn, String channel) {
        super(fact, host, port, user, pass, pvpgn, channel);
    }

    private static String getMapName(int number) throws ParserException, URISyntaxException {
        return getMapName(getMapURI(number));
    }

    private static String getMapName(URI mapURI) {
        return new File(mapURI.getPath()).getName();
    }

    private static URI getMapURI(int number) throws URISyntaxException, ParserException {
        Parser parser;
        NodeFilter filter;
        NodeList list;
        URI epicwar = new URI("http://www.epicwar.com/");
        URI epicwarmapinfo = epicwar.resolve("/maps/" + number);
        URI epicwarmapdl = null;
        filter = new NodeClassFilter(LinkTag.class);
        parser = new Parser(epicwarmapinfo.toString());
        list = parser.extractAllNodesThatMatch(filter);
        for (int i = 0; i < list.size(); i++) {
            String href = ((Tag) list.elementAt(i)).getAttribute("href");
            if (href.endsWith(".w3x") || href.endsWith(".w3m")) {
                epicwarmapdl = epicwar.resolve(href);
                break;
            }
        }
        return epicwarmapdl;
    }

    private static boolean isAdmin(String user) {
        try {
            Connection dbConn = DriverManager.getConnection(GHOST_DB_URL, GHOST_DB_USER, GHOST_DB_PASS);
            Statement stmt = dbConn.createStatement();
            ResultSet rs = stmt.executeQuery("select * from admins where name=\"" + user.toLowerCase() + "\"");
            boolean result = rs.next();
            rs.close();
            dbConn.close();
            return result;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    protected void onChannelChatReceived(String user, String message) {
        if (message.startsWith(TRIGGER)) {
            String commandLine = message.substring(1);
            String[] tokens = StringUtils.split(commandLine);
            if (tokens.length == 0) {
                return;
            }
            String command = tokens[0];
            String[] args = Arrays.copyOfRange(tokens, 1, tokens.length);
            if (command.equalsIgnoreCase("version")) {
                sendChannelChat("EpicWar Installer Bot 1.0.0-elite_hbot by RichardNixon (shout out to Yodyood/Tenth for forcing me to fix the admins)");
            } else if (command.equalsIgnoreCase("wersion")) {
                sendChannelChat("xaxaxa");
            } else if (command.equalsIgnoreCase("roll")) {
                long max = ROLL_MAX_DFL;
                if (args.length > 0) {
                    try {
                        max = Long.parseLong(args[0]);
                    } catch (NumberFormatException e) {
                        sendChannelChat("Try typing an actual number, dumbass.");
                        return;
                    }
                }
                int roll = (int) (Math.random() * max);
                sendChannelChat(user + " rolled " + roll + " out of " + max + ".");
            } else if (command.equalsIgnoreCase("mapinfo")) {
                for (int i = 0; i < args.length; i++) {
                    try {
                        int mapnum = Integer.parseInt(args[i]);
                        String map = getMapName(mapnum);
                        sendChannelChat("EpicWar map #" + mapnum + " is " + map + ".");
                    } catch (Exception ex) {
                        sendChannelChat("Could not get map info for EpicWar map #" + args[i] + ".");
                        continue;
                    }
                }
            } else if (command.equalsIgnoreCase("install")) {
                if (!isAdmin(user)) {
                    sendChannelChat("You do not have access to this command.");
                    return;
                }
                int number;
                try {
                    number = Integer.parseInt(args[0]);
                } catch (Exception ex) {
                    sendChannelChat("USAGE: " + TRIGGER + "install <map number>");
                    return;
                }
                if (number < 1) {
                    sendChannelChat("Map number must be positive.");
                    return;
                }
                if (!CFG_DIR.exists() || !CFG_DIR.isDirectory()) {
                    sendChannelChat("The configuration file directory does not exist.");
                    sendChannelChat("Please notify the system administrator (" + SYSADMIN_ACCT + ") immediately.");
                    return;
                }
                if (!MAP_DIR.exists() || !MAP_DIR.isDirectory()) {
                    sendChannelChat("The map file directory does not exist.");
                    sendChannelChat("Please notify the system administrator (" + SYSADMIN_ACCT + ") immediately.");
                    return;
                }
                URI mapURI;
                String mapName;
                try {
                    mapURI = getMapURI(number);
                    mapName = getMapName(mapURI);
                } catch (Exception ex) {
                    sendChannelChat("Unable to get file information for EpicWar map #" + number + ".");
                    return;
                }
                File mapCfg;
                File mapCfgObs;
                File mapDloadDir;
                File mapFile;
                try {
                    mapCfg = new File(CFG_DIR.getCanonicalPath() + File.separator + number + ".cfg").getCanonicalFile();
                    mapCfgObs = new File(CFG_DIR.getCanonicalPath() + File.separator + number + "-obs.cfg").getCanonicalFile();
                    mapDloadDir = new File(MAP_DIR.getCanonicalPath() + File.separator + number + File.separator).getCanonicalFile();
                    mapFile = new File(mapDloadDir.getCanonicalPath() + File.separator + mapName).getCanonicalFile();
                } catch (IOException ex) {
                    sendChannelChat("An error occurred while creating paths:");
                    sendChannelChat("" + ex.toString());
                    sendChannelChat("Please notify the system administrator (" + SYSADMIN_ACCT + ") immediately.");
                    return;
                }
                if (mapCfg.exists()) {
                    sendChannelChat("This map is already installed!");
                    sendChannelChat("You may load this map by typing " + TRIGGER + "load " + number + ".");
                    return;
                }
                if (mapDloadDir.exists()) {
                    sendChannelChat("The map directory exists, but no configuration file found.");
                    sendChannelChat("Please notify the system administrator (" + SYSADMIN_ACCT + ") immediately.");
                    return;
                }
                try {
                    if (!mapCfg.createNewFile()) {
                        throw new IOException("Unable to create map configuration file.");
                    }
                    if (!mapDloadDir.mkdir()) {
                        throw new IOException("Unable to create map directory.");
                    }
                    if (!mapFile.createNewFile()) {
                        throw new IOException("Unable to create map file.");
                    }
                    FileChannel outChannel = new FileOutputStream(mapFile).getChannel();
                    URLConnection conn = mapURI.toURL().openConnection();
                    ReadableByteChannel inChannel = Channels.newChannel(conn.getInputStream());
                    int clen = conn.getContentLength();
                    sendChannelChat("Installing EpicWar map #" + number + ": " + mapName + " (" + (int) (clen / 1024.0) + " KB), please wait...");
                    outChannel.transferFrom(inChannel, 0, clen);
                    outChannel.close();
                    inChannel.close();
                    PrintWriter cfgWriter;
                    cfgWriter = new PrintWriter(mapCfg);
                    cfgWriter.println("map_path = Maps\\Download\\" + mapName);
                    cfgWriter.println("map_speed = 3");
                    cfgWriter.println("map_visibility = 4");
                    cfgWriter.println("map_observers = 1");
                    cfgWriter.println("map_flags = 0");
                    cfgWriter.println("map_gametype = 1");
                    cfgWriter.println("map_localpath = " + number + File.separator + mapName);
                    cfgWriter.flush();
                    cfgWriter.close();
                    cfgWriter = new PrintWriter(mapCfgObs);
                    cfgWriter.println("map_path = Maps\\Download\\" + mapName);
                    cfgWriter.println("map_speed = 3");
                    cfgWriter.println("map_visibility = 4");
                    cfgWriter.println("map_observers = 4");
                    cfgWriter.println("map_flags = 0");
                    cfgWriter.println("map_gametype = 1");
                    cfgWriter.println("map_localpath = " + number + File.separator + mapName);
                    cfgWriter.flush();
                    cfgWriter.close();
                    sendChannelChat("EpicWar map #" + number + " installed successfully!");
                    sendChannelChat("Use " + TRIGGER + "load " + number + " to load this map.");
                } catch (Exception ex) {
                    sendChannelChat("Unable to install map #" + number + " due to I/O error:");
                    sendChannelChat("" + ex.toString());
                    sendChannelChat("Please notify the system administrator (" + SYSADMIN_ACCT + ") immediately.");
                    mapCfgObs.delete();
                    mapCfg.delete();
                    mapFile.delete();
                    mapDloadDir.delete();
                }
            }
        }
    }

    /**
   * @param args
   * @throws ClassNotFoundException 
   * @throws IllegalAccessException 
   * @throws InstantiationException 
   */
    public static void main(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        Class.forName("org.sqlite.JDBC").newInstance();
        Class.forName("com.mysql.jdbc.Driver").newInstance();
        BattleNetChatClientFactory fact = new BattleNetChatClientFactory();
        fact.setPlatform(Platform.X86);
        fact.setProduct(Product.W3TFT_1_23A);
        fact.setKeys("ABCDEFGHIJKLMNOPQRSTUVWXYZ", "ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        fact.setKeyOwner("Richard Milhous Nixon");
        BattleNetChatBot bot = new EpicWarDownloaderBot(fact, args[0], Integer.valueOf(args[1]), args[2], args[3], Boolean.valueOf(args[4]), args[5]);
        bot.start();
    }
}
