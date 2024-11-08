package com.elibera.gateway.app;

import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.httpclient.HttpClient;
import org.hibernate.Session;
import com.elibera.gateway.elements.History;
import com.elibera.gateway.elements.RedirectClient;
import com.elibera.gateway.entity.AllowedServerChecked;
import com.elibera.gateway.entity.ServerEntry;
import com.elibera.gateway.entity.SessionLog;
import com.elibera.gateway.entity.UserData;
import com.elibera.gateway.entity.Werbung;
import com.elibera.gateway.threading.ServerIO;
import com.elibera.util.Base64;
import com.elibera.util.Log;

/**
 * @author meisi
 *
 */
public class MLERedirectProtocol {

    public static final int PROTOCL_VERSION_SERVER_SIDE = 14;

    public static final int CONNECTED = 0;

    public static final int AUTHENTICATED = 4;

    public static final int CLIENT_ERROR = 8;

    /**
    	 * hier laden wir die Daten in den request Buffer<br>
    	 * ist der request voll geben wir ture, zurück, ansonsten lesen wir weiter
    	 */
    public static boolean doRequest(RedirectClient s, SocketChannel socket, Session em) {
        try {
            if (!socket.isOpen() || !socket.isConnected()) {
                doFinalyCloseMethod(s);
                return false;
            }
            if (s.version <= 8) {
                String inputLine = Server.readLineNonBlocking(s);
                if (inputLine != null) {
                    Log.debug("doRequest for:" + s + "," + s.version + ";" + inputLine + ";" + Thread.currentThread());
                    ServerIO.writeLineToBuffer(inputLine, s.requestBuffer);
                    s.lastAccess = System.currentTimeMillis();
                    return true;
                }
            } else {
                if (s.requestBuffer.requestReadDataLen > 0) {
                    if (ServerIO.readDataNonBlockingRequestCollecting(s.requestBuffer.requestReadDataLen, s.socket, s.bb, s.requestBuffer)) {
                        s.requestBuffer.requestReadDataLen = 0;
                    }
                } else {
                    String inputLine = Server.readLineNonBlocking(s);
                    if (inputLine == null) return false;
                    s.requestBuffer.requestReadDataLen = HelperStd.parseInt(inputLine, -2);
                    boolean binary = false;
                    if (s.requestBuffer.requestReadDataLen == -2 && inputLine.length() > 0 && inputLine.charAt(0) == 'b') {
                        s.requestBuffer.requestReadDataLen = HelperStd.parseInt(inputLine.substring(1), -1);
                        binary = true;
                    }
                    if (s.requestBuffer.requestReadDataLen < 0) {
                        s.requestBuffer.requestReadDataLen = 0;
                        return true;
                    }
                    if (!binary) {
                        s.requestBuffer.write(inputLine.getBytes());
                        s.requestBuffer.write(10);
                    }
                }
            }
            return false;
        } catch (ConnectException co) {
            doFinalyCloseMethod(s);
        } catch (IOException e2) {
            Log.error("Did the client close the connection?" + e2.getMessage());
            doFinalyCloseMethod(s);
        } catch (Exception e) {
            Log.error("Protokoll Error Or Database Error: \nProtokoll Version:" + s.version + ",\nstate:" + s.state + ",log:" + s.log + (s.log != null ? s.log.getAppName() + "," + s.log.getAppVersion() + "," + s.log.getUsername() + "," + s.log.getTel() + "," + s.log.getId() : ""), e);
            doFinalyCloseMethod(s);
        }
        return false;
    }

    /**
    	 * 2 möglichkeiten:<br>
    	 * 1.) client wurde gerade angenommen, dann machen wir einen basic task zum initialisieren<br>
    	 * 2.) daten sind im buffer und wir verarbeiten sie, wir geben true zurück, wenn der request abgeschlossen wurde<br>
    	 * 
    	 * @param s
    	 * @param socket
    	 * @param em
    	 * @return
    	 */
    public static boolean doProcessing(RedirectClient s, SocketChannel socket, Session em) {
        Log.debug("doProcessing for:" + s + ";" + Thread.currentThread() + ",Buffer-Size:" + s.requestBuffer.size());
        try {
            if (s.state == CLIENT_ERROR) {
                if (System.currentTimeMillis() - s.clientMemoryTotal > 2000) {
                    System.out.println("doProcessing: client error");
                    s.stop();
                    return true;
                } else return false;
            }
            if (!s.init) {
                Socket soc = socket.socket();
                s.isRedirectThread = s.redirectURL != null ? true : false;
                socket.configureBlocking(true);
                s.init = true;
                s.aktiv = true;
                s.client = new HttpClient(RedirectServer.connectionManager);
                Log.debug("client accepted startet:" + soc.getLocalSocketAddress() + "," + soc.getPort() + "," + soc.getInetAddress() + ", time:" + new Date().getTime());
                if (s.redirectURL != null) {
                    doRedirect(s);
                    System.out.println("redirect client to another server:" + s.redirectURL);
                    s.stop();
                    return true;
                }
                try {
                    s.version = Integer.parseInt(Server.readLine(s));
                } catch (Exception ne) {
                }
                s.encoding = Server.readLine(s);
                long key = (System.currentTimeMillis() / 1000) - new Random().nextInt();
                if (key < 0) key = key * -1;
                if (key == 0) key = new Random().nextLong() + 1;
                s.key = key;
                if (s.version >= 15) {
                    write(s, "ver");
                    write(s, "" + PROTOCL_VERSION_SERVER_SIDE);
                }
                write(s, "auth");
                write(s, key + "");
                if (s.version < Server.MINIMUM_CLIENT_VERSION) {
                    System.out.println("status: error min client version");
                    write(s, "error");
                    write(s, "Client out of date! Please upgrade to a new version!");
                    s.state = CLIENT_ERROR;
                    s.clientMemoryTotal = System.currentTimeMillis();
                    return false;
                }
                return true;
            }
            if (s.response.pendingResponse) {
                if (System.currentTimeMillis() - s.lastAccess < 15) {
                    Thread.sleep(15);
                }
                if (s.response.time <= 0) s.response.time = System.currentTimeMillis();
                if (System.currentTimeMillis() - s.response.time > Server.MAX_TIME_PENDING_RESPONSE) {
                    if (s.response.timeouttries > 0) {
                        System.out.println("Error: pending response timeout!");
                        Redirecter.writeError("timeout", s);
                        s.response.reset();
                        return true;
                    } else {
                        s.response.time += 1000 * 5;
                    }
                    s.response.timeouttries++;
                } else if (s.response.convert != null && (RedirectServer.extension != null && RedirectServer.extension.useParsingServer()) && !s.response.convert.isParsed()) {
                    if (s.response.timeConvert < 0) s.response.timeConvert = System.currentTimeMillis();
                    if (System.currentTimeMillis() - s.response.timeConvert > 15 * 1000) {
                        try {
                            System.out.println("still not parsed, doing in Thread Entity refresh:" + new java.util.Date().getTime() + "," + Thread.currentThread());
                            em.refresh(s.response.convert);
                        } catch (Exception e) {
                            Log.printStackTrace(e);
                        }
                        s.response.timeConvert = System.currentTimeMillis();
                    }
                } else if (s.response.isGETRequest) Redirecter.processGETReadResponse(s.response.username, s, s.response.cache, s.response.referer, em); else if (s.response.isPOSTRequest) Redirecter.processPOSTReadResponse(s.response.username, s, s.response.cache, s.response.referer, em); else {
                    System.out.println("Error: pending response STRANGE ERROR!");
                    Redirecter.writeError("unknown error", s);
                    s.response.reset();
                }
                if (!s.response.pendingResponse) return true;
            } else {
                if (processInput(s, em)) return true;
                if (!s.response.pendingResponse) return true;
            }
            return false;
        } catch (ConnectException co) {
            Log.error("ConnectException:" + co.getLocalizedMessage(), co);
            doFinalyCloseMethod(s);
        } catch (IOException e2) {
            Log.error("Did the client close the connection?" + e2.getMessage());
            doFinalyCloseMethod(s);
        } catch (Exception e) {
            Log.error("Protokoll Error Or Database Error: \nProtokoll Version:" + s.version + ",\nstate:" + s.state + ",log:" + s.log + (s.log != null ? s.log.getAppName() + "," + s.log.getAppVersion() + "," + s.log.getUsername() + "," + s.log.getTel() + "," + s.log.getId() : ""), e);
            doFinalyCloseMethod(s);
        } finally {
            s.lastAccess = System.currentTimeMillis();
            if (!socket.isOpen() || !socket.isConnected()) doFinalyCloseMethod(s);
        }
        return false;
    }

    /**
	  * das ist früher in der finaly methode des doServerThread() methode gestanden ...
	  * @param s
	  */
    private static void doFinalyCloseMethod(RedirectClient s) {
        quiteServerThread(s);
        s.log = null;
        s.socket = null;
        s.history = null;
        s.cache = null;
        s.client = null;
        Server.executor.serverThreadHasFinished(s, s.isRedirectThread);
    }

    /**
	     * gibt true zurück, wenn die Verbindung geschlossen werden soll
	     * @param theInput
	     * @return
	     * @throws IOException
	     */
    public static boolean processInput(RedirectClient s, Session em) throws IOException {
        boolean protocolSplitVersion = false;
        if (s.version >= 9) protocolSplitVersion = true;
        if (s.state == CONNECTED) {
            s.log.setUsername(Server.readEncodedLine(s));
            String pass = "", plattform = "";
            s.log.setAppName(Server.readEncodedLine(s));
            s.log.setDeviceName(Server.readEncodedLine(s));
            s.log.setDeviceInstanceID(Server.readEncodedLine(s));
            if (protocolSplitVersion) s.log.setInstallDate(HelperStd.parseLong(Server.readEncodedLine(s), -1)); else s.log.setInstallDate(HelperStd.parseLong(Server.readLine(s), -1));
            if (protocolSplitVersion) s.log.setLanguage(Server.readEncodedLine(s)); else s.log.setLanguage(Server.readLine(s));
            s.locale = new Locale(s.log.getLanguage());
            s.log.setTel(Server.readEncodedLine(s));
            if (protocolSplitVersion) s.displayWidth = Integer.parseInt(Server.readEncodedLine(s)); else s.displayWidth = Integer.parseInt(Server.readLine(s));
            if (protocolSplitVersion) s.displayHeight = Integer.parseInt(Server.readEncodedLine(s)); else s.displayHeight = Integer.parseInt(Server.readLine(s));
            if (s.version >= 7) {
                plattform = Server.readEncodedLine(s);
                pass = Server.readEncodedLine(s);
                s.formatImg = Server.readEncodedLine(s);
                s.formatAudio = Server.readEncodedLine(s);
                s.formatVideo = Server.readEncodedLine(s);
                s.msgID = new String[] { s.log.getUsername(), pass, plattform };
            }
            if (s.version >= 2) {
                if (protocolSplitVersion) s.log.setAppVersion(Server.readEncodedLine(s)); else s.log.setAppVersion(Server.readLine(s));
                if (protocolSplitVersion) s.clientMemoryTotal = Long.parseLong(Server.readEncodedLine(s)); else s.clientMemoryTotal = Long.parseLong(Server.readLine(s));
                if (s.version >= 3) s.plugins = Server.readEncodedLine(s);
                if (s.version >= 10) {
                    int numValues = HelperStd.parseInt(Server.readEncodedLine(s), 0);
                    for (int i = 0; i < numValues; i++) {
                        String v = Server.readEncodedLine(s);
                        if (i == 0) s.fontMetrics = v; else if (i == 1) s.contentSize = v;
                    }
                }
            }
            if (Server.USE_DB_ADVERTISING) {
                s.werbungen.username = s.log.getUsername();
                s.werbungen.appVersion = s.log.getAppVersion();
                s.werbungen.appName = s.log.getAppName();
                s.werbungen.deviceName = s.log.getDeviceName();
                if (!Queues.offerElementToWerbungsQueue(s.werbungen)) {
                    Log.error("advertising queue is too small!!!");
                }
            }
            s.log.setTimeLoggedIn(new Date().getTime());
            if (checkIFUserIsBlocked(s)) {
                System.out.println("user is blocked!!!");
                doQuite(s);
                return true;
            }
            if (Server.USE_DB_LOGGING) Server.executor.saveDBObject(s.log);
            s.state = AUTHENTICATED;
            Werbung ad = null;
            if (s.version >= 16) ad = Helper.getStdWerbungImageAuth(s, true);
            if (ad == null) write(s, "ready"); else {
                writeProgressAd(s, ad);
            }
            String userDataId = s.log.getUsername() + pass + plattform + s.log.getDeviceInstanceID() + s.log.getAppName();
            try {
                s.userData = (UserData) em.get(UserData.class, userDataId);
                if (s.userData != null && s.userData.getHistory() != null && System.currentTimeMillis() - s.userData.getLastAccess() < 1200 * 1000) {
                    s.history = s.userData.getHistory();
                }
            } catch (org.hibernate.type.SerializationException se) {
                try {
                    em.createQuery("DELETE FROM UserData WHERE id=:id").setParameter("id", userDataId).executeUpdate();
                } catch (Exception ee) {
                }
            } catch (Exception e) {
            }
            if (s.userData == null) {
                s.userData = new UserData(userDataId);
                s.userData.isNew = true;
            }
        } else if (s.state == AUTHENTICATED) {
            String theInput = protocolSplitVersion ? Server.readEncodedLine(s) : Server.readLine(s);
            if (theInput.compareTo("0") == 0) {
                if (s.requestBuffer.isAvaliable()) theInput = protocolSplitVersion ? Server.readEncodedLine(s) : Server.readLine(s); else return false;
            } else if (theInput.compareTo("1") == 0) {
                write(s, "1");
                if (s.requestBuffer.isAvaliable()) theInput = protocolSplitVersion ? Server.readEncodedLine(s) : Server.readLine(s); else return false;
            }
            if (theInput.compareTo("exit") == 0) {
                doQuite(s);
                return true;
            } else if (theInput.compareTo("url") == 0) {
                processRedirect(s, false, em);
            } else if (theInput.compareTo("cache") == 0) {
                processRedirect(s, true, em);
            }
        }
        return false;
    }

    public static void write(RedirectClient s, String theOutput) throws IOException {
        ServerIO.write(s.socket, theOutput);
    }

    public static void write(RedirectClient s, byte[] data) throws IOException {
        ServerIO.write(s.socket, data);
    }

    /**
	     * führt das weiterleiten aus
	     * wenn der client eine msg "redirect" schickt
	     * @param s
	     */
    private static void processRedirect(RedirectClient s, boolean cache, Session em) throws IOException {
        boolean protocolSplitVersion = false;
        if (s.version >= 9) protocolSplitVersion = true;
        String url = Server.readEncodedLine(s);
        String username = Server.readEncodedLine(s);
        String passwort = Server.readEncodedLine(s);
        String referer = "";
        if (s.version >= 2) {
            s.clientMemoryUsed = Long.parseLong(protocolSplitVersion ? Server.readEncodedLine(s) : Server.readLine(s));
            s.clientDiscMemoryFree = Long.parseLong(protocolSplitVersion ? Server.readEncodedLine(s) : Server.readLine(s));
            if (s.version >= 5) {
                referer = Server.readEncodedLine(s);
                if (s.version >= 7) {
                    int numOfInfos = Integer.parseInt(protocolSplitVersion ? Server.readEncodedLine(s) : Server.readLine(s));
                    for (int i = 0; i < numOfInfos; i++) {
                        String v = Server.readEncodedLine(s);
                        switch(v.charAt(0)) {
                            case 'l':
                                s.locationInfo = v.substring(1);
                                break;
                        }
                    }
                }
            }
        }
        int paramParts = Integer.parseInt(protocolSplitVersion ? Server.readEncodedLine(s) : Server.readLine(s));
        int fileParts = Integer.parseInt(protocolSplitVersion ? Server.readEncodedLine(s) : Server.readLine(s));
        if (paramParts > 0 || fileParts > 0) {
            String[] paramNames = new String[paramParts];
            String[] paramValues = new String[paramParts];
            for (int i = 0; i < paramParts; i++) {
                paramNames[i] = Server.readEncodedLine(s);
                paramValues[i] = Server.readEncodedLine(s);
            }
            String[] fileNames = new String[fileParts];
            String[] fileContentTypes = new String[fileParts];
            String[] fileParamNames = new String[fileParts];
            byte[][] fileData = new byte[fileParts][];
            for (int i = 0; i < fileParts; i++) {
                fileParamNames[i] = Server.readEncodedLine(s);
                if (s.version >= 4) fileNames[i] = Server.readEncodedLine(s);
                if (fileNames[i] == null) fileNames[i] = fileParamNames[i];
                fileContentTypes[i] = Server.readEncodedLine(s);
                int length = Integer.parseInt(protocolSplitVersion ? Server.readEncodedLine(s) : Server.readLine(s));
                fileData[i] = Server.readData(length, s);
            }
            s.requestBuffer.reset();
            Redirecter.processPOST(url, username, passwort, paramNames, paramValues, fileParamNames, fileNames, fileContentTypes, fileData, s, referer, em);
        } else {
            s.requestBuffer.reset();
            Redirecter.processGET(url, username, passwort, s, cache, referer, em);
        }
    }

    /**
	    * überprüft, ob der Benutzer geblockt ist
	    * @param s
	    * @return
	    */
    private static boolean checkIFUserIsBlocked(RedirectClient s) throws IOException {
        if (!Server.USE_DB || !Server.USE_CHECK_FOR_BLOCKED_USERS) return false;
        String blockMsg = BlockingTasks.isUserBlocked(s);
        if (Helper.isEmpty(blockMsg)) return false;
        write(s, "error");
        write(s, blockMsg.replace('\n', ';').replace('\r', ';'));
        s.log.setUserWasBlocked(true);
        return true;
    }

    /**
	     * beenden der Verbindung
	     * @param s
	     */
    public static void doQuite(RedirectClient s) {
        if (Server.USE_DB && s.userData != null && s.userData.getId() != null) {
            s.userData.setLastAccess(System.currentTimeMillis());
            s.userData.setHistory(s.history);
            System.out.println("doQuite:" + s.userData.isNew);
            if (s.userData.isNew) Server.executor.saveDBObject(s.userData); else Server.executor.mergeDBObject(s.userData);
        }
        if (s == null || s.log == null) return;
        Log.debug("doQuite:" + s.log.getUsername() + "," + s.log.getAppName() + "," + s.log.getId() + ",time:" + new Date().getTime());
        if (!Server.USE_DB_LOGGING) {
            s.log = null;
            return;
        }
        s.log.setTimeLoggedOut(new Date().getTime());
        Server.executor.mergeDBObject(s.log);
        s.log = null;
    }

    /**
	     * gibt die URL zurück
	     * oder NULL wenn die URL nicht erlaubt ist
	     * http oder https wird hinzugefügt
	     * @param url
	     * @return
	     */
    public static AllowedServerChecked getAllowedServerUrl(String url, RedirectClient s) {
        AllowedServerChecked server = new AllowedServerChecked();
        server.newUrl = url;
        if (url.indexOf("http") < 0) server.newUrl = "http://" + url;
        if (!Server.USE_CHECK_FOR_DB_SERVER_ENTRIES) return server;
        String host = url.toLowerCase();
        if (url.indexOf("http://") == 0) host = url.substring("http://".length()); else if (url.indexOf("https://") == 0) host = url.substring("https://".length());
        if (host.indexOf('/') > 0) host = host.substring(0, host.indexOf('/'));
        ServerEntry se = null;
        boolean firsttime = true;
        while (firsttime || (se == null && host.indexOf('.') > 0)) {
            se = BlockingTasks.serverEntries.get(host);
            if (host.indexOf('.') > 0) host = host.substring(host.indexOf('.') + 1);
            firsttime = false;
        }
        if (se == null) return server;
        server.server = se;
        if (se.isLocked()) {
            server.newUrl = null;
            return server;
        }
        if (url.indexOf("http") < 0) {
            if (se.isHttps()) server.newUrl = "https://" + url;
            server.newUrl = "http://" + url;
        } else server.newUrl = url;
        return server;
    }

    public static void doRedirect(RedirectClient s) throws IOException {
        write(s, "redirect");
        write(s, s.redirectURL);
        String ok = Server.readLine(s);
    }

    private static boolean isEmpty(String test) {
        if (test == null || test.length() <= 0) return true;
        return false;
    }

    public static void quiteServerThread(RedirectClient s) {
        if (!s.aktiv) return;
        doQuite(s);
        try {
            s.socket.close();
        } catch (Exception e) {
        }
        s.aktiv = false;
    }

    public static void resetRedirectClientObject(RedirectClient s) {
        s.readLine.reset();
        s.resetRequestBuffer();
        s.log = new SessionLog();
        s.userData = null;
        s.werbungen.reset();
        s.cache = null;
        s.state = MLERedirectProtocol.CONNECTED;
        s.cookie = "";
        s.encoding = "UTF-8";
        s.version = 1;
        s.client = null;
        s.clientDiscMemoryFree = -1;
        s.clientMemoryTotal = -1;
        s.clientMemoryUsed = -1;
        s.displayHeight = 0;
        s.displayWidth = 0;
        s.formatAudio = null;
        s.formatImg = null;
        s.formatVideo = null;
        s.history = new History[0];
        s.plugins = "";
        s.redirectURL = null;
        s.init = false;
        s.isRedirectThread = false;
        s.lastPreparser = null;
        s.msgID = null;
        s.locationInfo = null;
        s.sessionId = null;
        s.socket = null;
        s.fontMetrics = null;
        s.contentSize = null;
        s.adSentLastTime = false;
        s.response.reset();
    }

    /**
	     * schickt eine Progress-Bar Ad Werbugn an den Client
	     * @param s
	     * @param ad
	     * @throws IOException
	     */
    public static void writeProgressAd(RedirectClient s, Werbung ad) throws IOException {
        if (s.version < 16) return;
        write(s, "ad2");
        byte[] img = ad.getImage();
        if (img == null) img = new byte[0];
        write(s, "" + img.length);
        if (img.length > 0) write(s, img);
        write(s, "" + ad.getProgressForceDisplayTime());
        String text = ad.getXml();
        if (text == null) text = "";
        img = text.getBytes();
        write(s, "" + img.length);
        if (img.length > 0) write(s, img);
    }
}
