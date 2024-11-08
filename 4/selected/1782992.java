package org.homedns.dade.yajal;

import java.io.*;
import java.net.*;
import net.jxta.peer.*;
import net.jxta.peergroup.*;
import net.jxta.protocol.*;
import net.jxta.document.*;

/**
 * This class include all information about the configuration of YaJal.
 * @author  david
 */
public class yjConfiguration {

    /** Maximum number of result accepted from a single peer in a peer search.
     * <BR/>
     * <B>NOTE:</B> change this value only if you have deep knowledge of the YaJal
     * sources.
     */
    public static int MAX_REASULT_IN_PEER_SEARCH = 32;

    /** Maximum number of result accepted from a single peer in a group search.
     * <BR/>
     * <B>NOTE:</B> change this value only if you have deep knowledge of the YaJal
     * sources.
     */
    public static int MAX_REASULT_IN_GROUP_SEARCH = 32;

    /** Maximum number of result accepted from a single peer in a pipe search.
     * <BR/>
     * <B>NOTE:</B> change this value only if you have deep knowledge of the YaJal
     * sources.
     */
    public static int MAX_REASULT_IN_PIPE_SEARCH = 32;

    /** Maximum number of result accepted from a single peer in a node search.
     * <BR/>
     * <B>NOTE:</B> change this value only if you have deep knowledge of the YaJal
     * sources.
     */
    public static int MAX_REASULT_IN_NODE_SEARCH = 32;

    /** Connection timeout with JXTA Rendevouz server.
     * <BR/>
     * <B>NOTE:</B> change this value only if you have deep knowledge of the YaJal
     * sources.
     */
    public static int RENDEVOUZ_CONNECTION_TIMEOUT = 15 * 1000;

    /** Autostart timeout of JXTA Rendevouz server.
     * <BR/>
     * <B>NOTE:</B> change this value only if you have deep knowledge of the YaJal
     * sources.
     */
    public static int RENDEVOUZ_AUTOSTART_CHECK_TIMEOUT = 60 * 1000;

    /** Remote node client connection timeout.
     * <BR/>
     * <B>NOTE:</B> change this value only if you have deep knowledge of the YaJal
     * sources.
     */
    public static int REMOTE_NODE_CLIENT_CONNECT_TIMEOUT = 15 * 1000;

    /** Remote node client ping timeout.
     * <BR/>
     * <B>NOTE:</B> change this value only if you have deep knowledge of the YaJal
     * sources.
     */
    public static int REMOTE_NODE_CLIENT_PING_TIMEOUT = 5 * 1000;

    /** Remote node client search sleep small time.
     * <BR/>
     * <B>NOTE:</B> change this value only if you have deep knowledge of the YaJal
     * sources.
     */
    public static int REMOTE_NODE_CLIENT_SEARCH_SMALL_SLEEP = 15 * 1000;

    /** Remote node client search sleep big time.
     * <BR/>
     * <B>NOTE:</B> change this value only if you have deep knowledge of the YaJal
     * sources.
     */
    public static int REMOTE_NODE_CLIENT_SEARCH_BIG_SLEEP = 5 * 60 * 1000;

    protected static String ADV_NAME_SEPARATOR = "-";

    protected static String ADV_PIPE_NAME_PREFIX = "pipe";

    protected static String ADV_SYS_NAME_PREFIX = "sys";

    /** The path where the directory "yajal" is created. The default value is
     * "./yajal". If the env. variable YAJAL_HOME is defined, it is value will
     * be used to inizialize this field.
     */
    public static String YAJAL_HOME;

    private static File yajalHome;

    private static File yajalAdvCacheHome;

    static {
        YAJAL_HOME = System.getProperty("YAJAL_HOME", "./yajal");
        yajalHome = new File(YAJAL_HOME);
        if (!yajalHome.exists()) yajalHome.mkdirs();
        yajalAdvCacheHome = new File(YAJAL_HOME + "/adv");
        if (!yajalAdvCacheHome.exists()) yajalAdvCacheHome.mkdirs();
    }

    /** Creates a new instance of yjConfiguration.
     * This class can never be instantiated.
     */
    private yjConfiguration() {
    }

    /** Set the JXTA user name in order to avoid login dialog popup.
     * @param name The user name.
     */
    public static void setUserName(String name) {
        System.setProperty("net.jxta.tls.principal", name);
    }

    /** Set the JXTA user password in order to avoid login dialog popup.
     * @param password The user password.
     */
    public static void setUserPassword(String password) {
        System.setProperty("net.jxta.tls.password", password);
    }

    protected static void writeAdvertisement(String name, Advertisement adv) throws IOException, FileNotFoundException {
        File f = new File(name);
        yjConfiguration.writeAdvertisement(f, adv);
    }

    protected static Advertisement readAdvertisement(String name) throws IOException, FileNotFoundException {
        File f = new File(name);
        return yjConfiguration.readAdvertisement(f);
    }

    protected static void writeAdvertisement(File f, Advertisement adv) throws IOException, FileNotFoundException {
        InputStream in = adv.getDocument(new MimeMediaType("text/xml")).getStream();
        FileOutputStream out = new FileOutputStream(f);
        byte[] buf = new byte[8192];
        int r;
        while ((r = in.read(buf)) > 0) out.write(buf, 0, r);
        out.close();
    }

    protected static Advertisement readAdvertisement(File f) throws IOException, FileNotFoundException {
        if (f.exists()) {
            FileInputStream is = new FileInputStream(f);
            return AdvertisementFactory.newAdvertisement(MimeMediaType.XMLUTF8, is);
        } else return null;
    }

    protected static Advertisement readAdvertisement(InputStream is) throws IOException, FileNotFoundException {
        return AdvertisementFactory.newAdvertisement(MimeMediaType.XMLUTF8, is);
    }

    protected static void writeAdvertisementCache(String name, Advertisement adv) throws IOException, FileNotFoundException {
        File f = new File(yajalAdvCacheHome.getAbsolutePath() + "/" + name);
        yjConfiguration.writeAdvertisement(f, adv);
    }

    protected static Advertisement readAdvertisementCache(String name) throws IOException, FileNotFoundException {
        File f = new File(yajalAdvCacheHome.getAbsolutePath() + "/" + name);
        return yjConfiguration.readAdvertisement(f);
    }
}
