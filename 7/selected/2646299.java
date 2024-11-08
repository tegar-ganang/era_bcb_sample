package ymsg.network;

import ymsg.network.event.*;
import java.awt.Component;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class Session implements StatusConstants, ServiceConstants, NetworkConstants {

    private String primaryID;

    private String loginID;

    private String password;

    private String cookieY, cookieT, cookieC;

    private String imvironment;

    private long status;

    private String customStatusMessage;

    private boolean customStatusBusy;

    private YahooGroup[] groups;

    private YahooIdentity[] identities;

    private int conferenceCount;

    private UserStore userStore;

    private int sessionStatus;

    private long sessionId = 0;

    private Vector listeners;

    private ConnectionHandler network;

    private ThreadGroup ymsgThreads;

    private InputThread ipThread;

    private PingThread pingThread;

    private Hashtable typingNotifiers;

    private boolean loginOver = false;

    private YahooException loginException = null;

    private YMSG9Packet cachePacket;

    private Hashtable conferences;

    private boolean chatConnectOver = false;

    private boolean chatLoginOver = false;

    private int chatSessionStatus;

    private YahooChatLobby currentLobby = null;

    private String chatID;

    public Session(ConnectionHandler ch) {
        network = ch;
        _init();
    }

    public Session() {
        Properties p = System.getProperties();
        if (p.containsKey(SOCKS_HOST)) network = new SOCKSConnectionHandler(); else if (p.containsKey(PROXY_HOST) || p.containsKey(PROXY_HOST_OLD)) network = new HTTPConnectionHandler(); else network = new DirectConnectionHandler();
        _init();
    }

    private void _init() {
        status = STATUS_AVAILABLE;
        sessionId = 0;
        sessionStatus = UNSTARTED;
        ymsgThreads = new ThreadGroup("YMSG Threads");
        groups = null;
        identities = null;
        listeners = new Vector();
        conferences = new Hashtable();
        typingNotifiers = new Hashtable();
        userStore = new UserStore();
        network.install(this, ymsgThreads);
    }

    public void addSessionListener(SessionListener ss) {
        if (listeners.indexOf(ss) < 0) listeners.addElement(ss);
    }

    public void removeSessionListener(SessionListener ss) {
        listeners.removeElement(ss);
    }

    public ConnectionHandler getConnectionHandler() {
        return network;
    }

    public synchronized void login(String u, String p) throws IllegalStateException, IOException, AccountLockedException, LoginRefusedException {
        if (sessionStatus != UNSTARTED) throw new IllegalStateException("Session should be unstarted");
        u = u.toLowerCase();
        resetData();
        loginID = u;
        primaryID = null;
        password = p;
        sessionId = 0;
        imvironment = "0";
        try {
            network.open();
            loginOver = false;
            startThreads();
            transmitAuth();
            long timeout = System.currentTimeMillis() + Util.loginTimeout(LOGIN_TIMEOUT);
            while (!loginOver && !past(timeout)) try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
            }
            if (past(timeout)) {
                sessionStatus = FAILED;
                closeNetwork();
                throw new InterruptedIOException("Login timed out");
            } else if (sessionStatus == FAILED && loginException != null) {
                throw (LoginRefusedException) loginException;
            }
        } finally {
        }
    }

    public synchronized void logout() throws IllegalStateException, IOException {
        checkStatus();
        sessionStatus = UNSTARTED;
        transmitLogoff();
    }

    public synchronized void reset() throws IllegalStateException {
        if (sessionStatus != FAILED && sessionStatus != UNSTARTED) throw new IllegalStateException("Session currently active");
        sessionStatus = UNSTARTED;
        chatSessionStatus = UNSTARTED;
        resetData();
    }

    public void sendMessage(String to, String msg) throws IllegalStateException, IOException {
        checkStatus();
        transmitMessage(to, loginID, msg);
    }

    public void sendMessage(String to, String msg, YahooIdentity yid) throws IllegalStateException, IOException, IllegalIdentityException {
        checkStatus();
        checkIdentity(yid);
        transmitMessage(to, yid.getId(), msg);
    }

    public void sendBuzz(String to) throws IllegalStateException, IOException {
        sendMessage(to, BUZZ);
    }

    public void sendBuzz(String to, YahooIdentity yid) throws IllegalStateException, IOException, IllegalIdentityException {
        sendMessage(to, BUZZ, yid);
    }

    public int getSessionStatus() {
        return sessionStatus;
    }

    public long getStatus() {
        return status;
    }

    public synchronized void setStatus(long s) throws IllegalArgumentException, IOException {
        if (sessionStatus == UNSTARTED && !(s == STATUS_AVAILABLE || s == STATUS_INVISIBLE)) throw new IllegalArgumentException("Unstarted sessions can be available or invisible only");
        if (s == STATUS_CUSTOM) throw new IllegalArgumentException("Cannot set custom state without message");
        status = s;
        customStatusMessage = null;
        if (sessionStatus == MESSAGING) _doStatus();
    }

    public synchronized void setStatus(String m, boolean b) throws IllegalArgumentException, IOException {
        if (sessionStatus == UNSTARTED) throw new IllegalArgumentException("Unstarted sessions can be available or invisible only");
        if (m == null) throw new IllegalArgumentException("Cannot set custom state with null message");
        status = STATUS_CUSTOM;
        customStatusMessage = m;
        customStatusBusy = b;
        _doStatus();
    }

    private void _doStatus() throws IllegalStateException, IOException {
        if (status == STATUS_AVAILABLE) transmitIsBack(); else if (status == STATUS_CUSTOM) transmitIsAway(customStatusMessage, customStatusBusy); else transmitIsAway();
    }

    public String getCustomStatusMessage() {
        return customStatusMessage;
    }

    public boolean isCustomBusy() {
        return customStatusBusy;
    }

    public void refreshStats() throws IllegalStateException, IOException {
        checkStatus();
        transmitUserStat();
    }

    public YahooIdentity[] getIdentities() {
        if (identities == null) return null; else return (YahooIdentity[]) identities.clone();
    }

    public YahooIdentity getPrimaryIdentity() {
        return identityIdToObject(primaryID);
    }

    public YahooIdentity getLoginIdentity() {
        return identityIdToObject(loginID);
    }

    public void activateIdentity(YahooIdentity yid, boolean activate) throws IllegalStateException, IllegalIdentityException, IOException {
        checkStatus();
        checkIdentity(yid);
        if (yid.getId().equals(primaryID)) throw new IllegalIdentityException("Primary identity cannot be de/activated");
        if (activate) transmitIdActivate(yid.getId()); else transmitIdDeactivate(yid.getId());
    }

    public void addTypingNotification(String user, Component com) {
        String syid = primaryID;
        String key = "user" + "\n" + syid;
        synchronized (typingNotifiers) {
            if (typingNotifiers.containsKey(key)) return;
            typingNotifiers.put(key, new TypingNotifier(com, user, syid));
        }
    }

    public void removeTypingNotification(String user) {
        String syid = primaryID;
        String key = "user" + "\n" + syid;
        synchronized (typingNotifiers) {
            TypingNotifier tn = (TypingNotifier) typingNotifiers.get(key);
            if (tn == null) return;
            tn.quit = true;
            tn.interrupt();
            typingNotifiers.remove(key);
        }
    }

    public void keyTyped(String user) {
        String syid = primaryID;
        String key = "user" + "\n" + syid;
        TypingNotifier tn = (TypingNotifier) typingNotifiers.get(key);
        if (tn != null) tn.keyTyped();
    }

    public YahooGroup[] getGroups() {
        return (YahooGroup[]) groups.clone();
    }

    public Hashtable getUsers() {
        return (Hashtable) userStore.getUsers().clone();
    }

    public YahooUser getUser(String id) {
        return userStore.get(id);
    }

    public String getImvironment() {
        return imvironment;
    }

    public String[] getCookies() {
        String[] arr = new String[3];
        arr[COOKIE_Y] = cookieY;
        arr[COOKIE_T] = cookieT;
        arr[COOKIE_C] = cookieC;
        return arr;
    }

    public YahooConference createConference(String[] users, String msg) throws IllegalStateException, IOException, IllegalIdentityException {
        checkIdentityNotOnList(users);
        return createConference(users, msg, identityIdToObject(loginID));
    }

    public YahooConference createConference(String[] users, String msg, YahooIdentity yid) throws IllegalStateException, IOException, IllegalIdentityException {
        checkStatus();
        checkIdentity(yid);
        checkIdentityNotOnList(users);
        String r = getConferenceName(yid.getId());
        transmitConfInvite(users, yid.getId(), r, msg);
        try {
            return getConference(r);
        } catch (NoSuchConferenceException e) {
            return null;
        }
    }

    public void acceptConferenceInvite(YahooConference room) throws IllegalStateException, IOException, NoSuchConferenceException {
        checkStatus();
        transmitConfLogon(room.getName(), room.getIdentity().getId());
    }

    public void declineConferenceInvite(YahooConference room, String msg) throws IllegalStateException, IOException, NoSuchConferenceException {
        checkStatus();
        transmitConfDecline(room.getName(), room.getIdentity().getId(), msg);
    }

    public void extendConference(YahooConference room, String user, String msg) throws IllegalStateException, IOException, NoSuchConferenceException, IllegalIdentityException {
        checkStatus();
        String[] arr = { user };
        checkIdentityNotOnList(arr);
        transmitConfAddInvite(user, room.getName(), room.getIdentity().getId(), msg);
    }

    public void sendConferenceMessage(YahooConference room, String msg) throws IllegalStateException, IOException, NoSuchConferenceException {
        checkStatus();
        transmitConfMsg(room.getName(), room.getIdentity().getId(), msg);
    }

    public void leaveConference(YahooConference room) throws IllegalStateException, IOException, NoSuchConferenceException {
        checkStatus();
        transmitConfLogoff(room.getName(), room.getIdentity().getId());
    }

    public void addFriend(String friend, String group) throws IllegalStateException, IOException {
        checkStatus();
        transmitFriendAdd(friend, group);
    }

    public void removeFriend(String friend, String group) throws IllegalStateException, IOException {
        checkStatus();
        transmitFriendRemove(friend, group);
    }

    public void rejectContact(SessionEvent se, String msg) throws IllegalArgumentException, IllegalStateException, IOException {
        if (se.getFrom() == null || se.getTo() == null) throw new IllegalArgumentException("Missing to or from field in event object.");
        checkStatus();
        transmitContactReject(se.getFrom(), se.getTo(), msg);
    }

    public void ignoreContact(String friend, boolean ignore) throws IllegalStateException, IOException {
        checkStatus();
        transmitContactIgnore(friend, ignore);
    }

    public void refreshFriends() throws IllegalStateException, IOException {
        checkStatus();
        transmitList();
    }

    public void sendFileTransfer(String user, String filename, String msg) throws IllegalStateException, FileTransferFailedException, IOException {
        checkStatus();
        transmitFileTransfer(user, msg, filename);
    }

    public void saveFileTransferAs(SessionFileTransferEvent ev, String filename) throws FileTransferFailedException, IOException {
        saveFT(ev, null, filename);
    }

    public void saveFileTransferTo(SessionFileTransferEvent ev, String dir) throws FileTransferFailedException, IOException {
        if (!dir.endsWith(File.separator)) dir = dir + File.separator;
        saveFT(ev, dir, ev.getFilename());
    }

    private void saveFT(SessionFileTransferEvent ev, String path, String filename) throws FileTransferFailedException, IOException {
        int len;
        byte[] buff = new byte[4096];
        String contDisp = "Content-Disposition: filename=";
        HTTPConnection conn = new HTTPConnection("GET", ev.getLocation());
        conn.println("Host: " + ev.getLocation().getHost());
        conn.println("User-Agent: " + USER_AGENT);
        conn.println("Cookie: " + cookieY + "; " + cookieT);
        conn.println("");
        conn.flush();
        String in = conn.readLine();
        if (in.indexOf(" 200 ") < 0) throw new FileTransferFailedException("Server HTTP error code: " + in);
        do {
            in = conn.readLine();
            if (path != null && in != null && in.startsWith(contDisp)) {
                filename = in.substring(contDisp.length());
                if (filename.charAt(0) == '\"') filename = filename.substring(1, filename.length() - 1);
            }
        } while (in != null && in.trim().length() > 0);
        if (in == null) throw new FileTransferFailedException("Server premature end of reply");
        if (path != null) filename = path + filename;
        DataOutputStream dos = new DataOutputStream(new FileOutputStream(filename));
        do {
            len = conn.read(buff);
            if (len > 0) dos.write(buff, 0, len);
        } while (len >= 0);
        dos.flush();
        dos.close();
        conn.close();
    }

    public synchronized void chatLogin(YahooChatLobby ycl) throws IllegalStateException, IOException, LoginRefusedException {
        chatLogin(ycl, identityIdToObject(loginID));
    }

    public synchronized void chatLogin(YahooChatLobby ycl, YahooIdentity yid) throws IllegalStateException, IOException, LoginRefusedException, IllegalIdentityException {
        checkStatus();
        checkIdentity(yid);
        if (chatSessionStatus != UNSTARTED && chatSessionStatus != MESSAGING) throw new IllegalStateException("Chat session should be unstarted or messaging");
        chatConnectOver = false;
        chatLoginOver = false;
        chatID = yid.getId();
        try {
            long timeout = System.currentTimeMillis() + Util.loginTimeout(LOGIN_TIMEOUT);
            if (currentLobby == null) {
                transmitChatConnect(yid.getId());
                while (!chatConnectOver && !past(timeout)) try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                }
                if (past(timeout)) throw new InterruptedIOException("Chat connect timed out");
            }
            transmitChatLogon(ycl.getNetworkName(), ycl.getParent().getId());
            while (!chatLoginOver && !past(timeout)) try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
            }
            if (past(timeout)) throw new InterruptedIOException("Chat login timed out"); else if (chatSessionStatus == FAILED && loginException != null) throw (LoginRefusedException) loginException;
            if (chatSessionStatus == MESSAGING) currentLobby = ycl; else currentLobby = null;
        } finally {
            if (chatSessionStatus != MESSAGING) {
                chatSessionStatus = FAILED;
                chatID = null;
            }
        }
    }

    public synchronized void chatLogout() throws IllegalStateException, IOException {
        checkStatus();
        checkChatStatus();
        transmitChatDisconnect(currentLobby.getNetworkName());
        currentLobby = null;
    }

    public void sendChatMessage(String msg) throws IllegalStateException, IOException {
        checkStatus();
        checkChatStatus();
        transmitChatMsg(currentLobby.getNetworkName(), msg, false);
    }

    public void sendChatEmote(String emote) throws IllegalStateException, IOException {
        checkStatus();
        checkChatStatus();
        transmitChatMsg(currentLobby.getNetworkName(), emote, true);
    }

    public YahooChatLobby getCurrentChatLobby() {
        return currentLobby;
    }

    public int getChatSessionStatus() {
        return chatSessionStatus;
    }

    public void resetChat() throws IllegalStateException {
        if (chatSessionStatus != FAILED && chatSessionStatus != UNSTARTED) throw new IllegalStateException("Chat session currently active");
        chatSessionStatus = UNSTARTED;
    }

    public void __test1(String a1, String a2) {
        try {
            network.close();
        } catch (IOException e) {
        }
    }

    public void __test2() {
    }

    protected void transmitAuth() throws IOException {
        sessionStatus = AUTH;
        PacketBodyBuffer body = new PacketBodyBuffer();
        body.addElement("0", loginID);
        body.addElement("1", loginID);
        sendPacket(body, SERVICE_AUTH);
    }

    protected void transmitAuthResp(String plp, String crp) throws IOException {
        PacketBodyBuffer body = new PacketBodyBuffer();
        body.addElement("0", loginID);
        body.addElement("6", plp);
        body.addElement("96", crp);
        body.addElement("2", "1");
        body.addElement("1", loginID);
        sendPacket(body, SERVICE_AUTHRESP, status);
    }

    protected void transmitChatConnect(String yid) throws IOException {
        chatSessionStatus = CONNECT;
        PacketBodyBuffer body = new PacketBodyBuffer();
        body.addElement("109", primaryID);
        body.addElement("1", yid);
        body.addElement("6", "abcde");
        sendPacket(body, SERVICE_CHATCONNECT);
    }

    protected void transmitChatDisconnect(String room) throws IOException {
        chatSessionStatus = UNSTARTED;
        currentLobby = null;
        PacketBodyBuffer body = new PacketBodyBuffer();
        body.addElement("104", room);
        body.addElement("109", chatID);
        sendPacket(body, SERVICE_CHATDISCONNECT);
    }

    protected void transmitChatLogon(String netname, long id) throws IOException {
        chatSessionStatus = LOGON;
        PacketBodyBuffer body = new PacketBodyBuffer();
        body.addElement("1", chatID);
        body.addElement("104", netname);
        body.addElement("129", "" + id);
        body.addElement("62", "2");
        sendPacket(body, SERVICE_CHATLOGON);
    }

    protected void transmitChatMsg(String netname, String msg, boolean emote) throws IOException {
        PacketBodyBuffer body = new PacketBodyBuffer();
        body.addElement("1", chatID);
        body.addElement("104", netname);
        body.addElement("117", msg);
        if (emote) body.addElement("124", "2"); else body.addElement("124", "1");
        if (Util.isUtf8(msg)) body.addElement("97", "1");
        sendPacket(body, SERVICE_CHATMSG);
    }

    protected void transmitChatPM(String to, String msg) throws IOException {
        PacketBodyBuffer body = new PacketBodyBuffer();
        body.addElement("5", to);
        body.addElement("14", msg);
        sendPacket(body, SERVICE_CHATPM);
    }

    protected void transmitChatPing() throws IOException {
        PacketBodyBuffer body = new PacketBodyBuffer();
        sendPacket(body, SERVICE_CHATPING);
    }

    protected void transmitConfAddInvite(String user, String room, String yid, String msg) throws IOException, NoSuchConferenceException {
        getConference(room);
        PacketBodyBuffer body = new PacketBodyBuffer();
        body.addElement("1", yid);
        body.addElement("51", user);
        body.addElement("57", room);
        Vector users = getConference(room).getUsers();
        for (int i = 0; i < users.size(); i++) body.addElement("52", ((YahooUser) users.elementAt(i)).getId());
        for (int i = 0; i < users.size(); i++) body.addElement("53", ((YahooUser) users.elementAt(i)).getId());
        body.addElement("58", msg);
        body.addElement("13", "0");
        sendPacket(body, SERVICE_CONFADDINVITE);
    }

    protected void transmitConfDecline(String room, String yid, String msg) throws IOException, NoSuchConferenceException {
        YahooConference yc = getConference(room);
        yc.closeConference();
        Vector users = yc.getUsers();
        PacketBodyBuffer body = new PacketBodyBuffer();
        body.addElement("1", yid);
        for (int i = 0; i < users.size(); i++) body.addElement("3", ((YahooUser) users.elementAt(i)).getId());
        body.addElement("57", room);
        body.addElement("14", msg);
        sendPacket(body, SERVICE_CONFDECLINE);
    }

    protected void transmitConfInvite(String[] users, String yid, String room, String msg) throws IOException {
        conferences.put(room, new YahooConference(userStore, identityIdToObject(yid), room, this, false));
        PacketBodyBuffer body = new PacketBodyBuffer();
        body.addElement("1", yid);
        body.addElement("50", primaryID);
        for (int i = 0; i < users.length; i++) body.addElement("52", users[i]);
        body.addElement("57", room);
        body.addElement("58", msg);
        body.addElement("13", "0");
        sendPacket(body, SERVICE_CONFINVITE);
    }

    protected void transmitConfLogoff(String room, String yid) throws IOException, NoSuchConferenceException {
        YahooConference yc = getConference(room);
        yc.closeConference();
        Vector users = yc.getUsers();
        PacketBodyBuffer body = new PacketBodyBuffer();
        body.addElement("1", yid);
        for (int i = 0; i < users.size(); i++) body.addElement("3", ((YahooUser) users.elementAt(i)).getId());
        body.addElement("57", room);
        sendPacket(body, SERVICE_CONFLOGOFF);
    }

    protected void transmitConfLogon(String room, String yid) throws IOException, NoSuchConferenceException {
        Vector users = getConference(room).getUsers();
        PacketBodyBuffer body = new PacketBodyBuffer();
        body.addElement("1", yid);
        for (int i = 0; i < users.size(); i++) body.addElement("3", ((YahooUser) users.elementAt(i)).getId());
        body.addElement("57", room);
        sendPacket(body, SERVICE_CONFLOGON);
    }

    protected void transmitConfMsg(String room, String yid, String msg) throws IOException, NoSuchConferenceException {
        Vector users = getConference(room).getUsers();
        PacketBodyBuffer body = new PacketBodyBuffer();
        body.addElement("1", yid);
        for (int i = 0; i < users.size(); i++) body.addElement("53", ((YahooUser) users.elementAt(i)).getId());
        body.addElement("57", room);
        body.addElement("14", msg);
        if (Util.isUtf8(msg)) body.addElement("97", "1");
        sendPacket(body, SERVICE_CONFMSG);
    }

    protected void transmitContactIgnore(String friend, boolean ignore) throws IOException {
        PacketBodyBuffer body = new PacketBodyBuffer();
        body.addElement("1", primaryID);
        body.addElement("7", friend);
        if (ignore) body.addElement("13", "1"); else body.addElement("13", "2");
        sendPacket(body, SERVICE_CONTACTIGNORE);
    }

    protected void transmitContactReject(String friend, String yid, String msg) throws IOException {
        PacketBodyBuffer body = new PacketBodyBuffer();
        body.addElement("1", yid);
        body.addElement("7", friend);
        body.addElement("14", msg);
        sendPacket(body, SERVICE_CONTACTREJECT);
    }

    protected void transmitFileTransfer(String to, String message, String filename) throws FileTransferFailedException, IOException {
        String cookie = cookieY + "; " + cookieT;
        int fileSize = -1;
        byte[] packet;
        byte[] marker = { '2', '9', (byte) 0xc0, (byte) 0x80 };
        DataInputStream dis = new DataInputStream(new FileInputStream(filename));
        fileSize = dis.available();
        if (fileSize <= 0) throw new FileTransferFailedException("File transfer: missing or empty file");
        byte[] fileData = new byte[fileSize];
        dis.readFully(fileData);
        dis.close();
        PacketBodyBuffer body = new PacketBodyBuffer();
        body.addElement("0", primaryID);
        body.addElement("5", to);
        body.addElement("28", fileSize + "");
        body.addElement("27", new File(filename).getName());
        body.addElement("14", message);
        packet = body.getBuffer();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.write(MAGIC, 0, 4);
        dos.write(VERSION, 0, 4);
        dos.writeShort((packet.length + 4) & 0xFFFF);
        dos.writeShort(SERVICE_FILETRANSFER & 0xFFFF);
        dos.writeInt((int) (status & 0xFFFFFFFF));
        dos.writeInt((int) (sessionId & 0xFFFFFFFF));
        dos.write(packet, 0, packet.length);
        dos.write(marker, 0, 4);
        packet = baos.toByteArray();
        String ftHost = Util.fileTransferHost();
        String ftURL = "http://" + ftHost + FILE_TF_PORTPATH;
        HTTPConnection conn = new HTTPConnection("POST", new URL(ftURL));
        conn.println("Content-Length: " + (fileSize + packet.length));
        conn.println("User-Agent: " + USER_AGENT);
        conn.println("Host: " + ftHost);
        conn.println("Cookie: " + cookie);
        conn.println("");
        conn.write(packet);
        conn.write(fileData);
        conn.flush();
        String in = conn.readLine(), head = in;
        if (in != null) {
            byte[] buffer = new byte[4096];
            while (conn.read(buffer) > 0) ;
        }
        conn.close();
        if (head.indexOf(" 200 ") < 0) throw new FileTransferFailedException("Server rejected upload");
    }

    protected void transmitFriendAdd(String friend, String group) throws IOException {
        PacketBodyBuffer body = new PacketBodyBuffer();
        body.addElement("1", primaryID);
        body.addElement("7", friend);
        body.addElement("65", group);
        sendPacket(body, SERVICE_FRIENDADD);
    }

    protected void transmitFriendRemove(String friend, String group) throws IOException {
        PacketBodyBuffer body = new PacketBodyBuffer();
        body.addElement("1", primaryID);
        body.addElement("7", friend);
        body.addElement("65", group);
        sendPacket(body, SERVICE_FRIENDREMOVE);
    }

    protected void transmitGroupRename(String oldName, String newName) throws IOException {
        PacketBodyBuffer body = new PacketBodyBuffer();
        body.addElement("1", primaryID);
        body.addElement("65", oldName);
        body.addElement("67", newName);
        sendPacket(body, SERVICE_GROUPRENAME);
    }

    protected void transmitIdActivate(String id) throws IOException {
        PacketBodyBuffer body = new PacketBodyBuffer();
        body.addElement("3", id);
        sendPacket(body, SERVICE_IDACT);
    }

    protected void transmitIdDeactivate(String id) throws IOException {
        PacketBodyBuffer body = new PacketBodyBuffer();
        body.addElement("3", id);
        sendPacket(body, SERVICE_IDDEACT);
    }

    protected void transmitIdle() throws IOException {
        PacketBodyBuffer body = new PacketBodyBuffer();
        body.addElement("1", loginID);
        body.addElement("0", primaryID);
        sendPacket(body, SERVICE_IDLE);
    }

    protected void transmitIsAway() throws IOException {
        PacketBodyBuffer body = new PacketBodyBuffer();
        body.addElement("10", status + "");
        sendPacket(body, SERVICE_ISAWAY, status);
    }

    protected void transmitIsAway(String msg, boolean a) throws IOException {
        PacketBodyBuffer body = new PacketBodyBuffer();
        status = STATUS_CUSTOM;
        body.addElement("10", status + "");
        body.addElement("19", msg);
        if (a) body.addElement("47", "1"); else body.addElement("47", "0");
        sendPacket(body, SERVICE_ISAWAY, status);
    }

    protected void transmitIsBack() throws IOException {
        PacketBodyBuffer body = new PacketBodyBuffer();
        body.addElement("10", status + "");
        sendPacket(body, SERVICE_ISBACK, status);
    }

    protected void transmitList() throws IOException {
        PacketBodyBuffer body = new PacketBodyBuffer();
        body.addElement("1", primaryID);
        sendPacket(body, SERVICE_LIST);
    }

    protected void transmitLogoff() throws IOException {
        PacketBodyBuffer body = new PacketBodyBuffer();
        body.addElement("0", loginID);
        sendPacket(body, SERVICE_LOGOFF);
    }

    protected void transmitMessage(String to, String yid, String msg) throws IOException {
        PacketBodyBuffer body = new PacketBodyBuffer();
        body.addElement("0", primaryID);
        body.addElement("1", yid);
        body.addElement("5", to);
        body.addElement("14", msg);
        if (Util.isUtf8(msg)) body.addElement("97", "1");
        body.addElement("63", ";" + imvironment);
        body.addElement("64", "0");
        sendPacket(body, SERVICE_MESSAGE, STATUS_OFFLINE);
        TypingNotifier tn = (TypingNotifier) typingNotifiers.get(to);
        if (tn != null) tn.stopTyping();
    }

    protected void transmitNotify(String friend, String yid, boolean on, String msg, String mode) throws IOException {
        PacketBodyBuffer body = new PacketBodyBuffer();
        body.addElement("4", yid);
        body.addElement("5", friend);
        body.addElement("14", msg);
        if (on) body.addElement("13", "1"); else body.addElement("13", "0");
        body.addElement("49", mode);
        sendPacket(body, SERVICE_NOTIFY, STATUS_TYPING);
    }

    protected void transmitPing() throws IOException {
        PacketBodyBuffer body = new PacketBodyBuffer();
        sendPacket(body, SERVICE_PING);
    }

    protected void transmitUserStat() throws IOException {
        PacketBodyBuffer body = new PacketBodyBuffer();
        sendPacket(body, SERVICE_USERSTAT);
    }

    protected void receiveAddIgnore(YMSG9Packet pkt) {
    }

    protected void receiveAuth(YMSG9Packet pkt) throws IOException, NoSuchAlgorithmException {
        String v10 = pkt.getValue("13");
        String[] s;
        try {
            if (v10 != null && v10.equals("1")) s = ChallengeResponseV10.getStrings(loginID, password, pkt.getValue("94")); else s = ChallengeResponseV9.getStrings(loginID, password, pkt.getValue("94"));
        } catch (NoSuchAlgorithmException e) {
            throw e;
        } catch (Exception e) {
            throw new YMSG9BadFormatException("auth", false, e);
        }
        transmitAuthResp(s[0], s[1]);
    }

    protected void receiveAuthResp(YMSG9Packet pkt) {
        try {
            if (pkt.exists("66")) {
                long l = Long.parseLong(pkt.getValue("66"));
                if (l == STATUS_LOCKED) {
                    URL u;
                    try {
                        u = new URL(pkt.getValue("20"));
                    } catch (Exception e) {
                        u = null;
                    }
                    loginException = new AccountLockedException("User " + loginID + " has been locked out", u);
                } else if (l == STATUS_BAD) {
                    loginException = new LoginRefusedException("User " + loginID + " refused login", l);
                } else if (l == STATUS_BADUSERNAME) {
                    loginException = new LoginRefusedException("User " + loginID + " unknown", l);
                }
            }
        } catch (NumberFormatException e) {
        }
        ipThread.quit = true;
        sessionStatus = FAILED;
        loginOver = true;
    }

    protected void receiveChatConnect(YMSG9Packet pkt) {
        chatConnectOver = true;
    }

    protected void receiveChatDisconnect(YMSG9Packet pkt) {
        if (chatSessionStatus != UNSTARTED) {
            new FireEvent().fire(new SessionEvent(this), SERVICE_CHATDISCONNECT);
        }
        chatSessionStatus = UNSTARTED;
    }

    protected void receiveChatLogoff(YMSG9Packet pkt) {
        try {
            String netname = pkt.getValue("104");
            String id = pkt.getValue("109");
            YahooChatLobby ycl = YahooChatCategory.getLobby(netname);
            if (ycl == null) throw new NoSuchChatroomException("Chatroom/lobby " + netname + " not found.");
            YahooChatUser ycu = ycl.getUser(id);
            if (ycu != null) ycl.removeUser(ycu); else ycu = createChatUser(pkt, 0);
            SessionChatEvent se = new SessionChatEvent(this, 1, ycl);
            se.setChatUser(0, ycu);
            new FireEvent().fire(se, SERVICE_CHATLOGOFF);
        } catch (Exception e) {
            throw new YMSG9BadFormatException("chat logoff", false, e);
        }
    }

    protected void receiveChatLogon(YMSG9Packet pkt) {
        boolean joining = false;
        try {
            if (pkt.exists("114")) {
                loginException = new LoginRefusedException("User " + chatID + " refused chat login");
                joining = true;
                chatSessionStatus = FAILED;
                return;
            }
            pkt = compoundChatLoginPacket(pkt);
            if (pkt == null) return;
            String netname = pkt.getValue("104");
            YahooChatLobby ycl = YahooChatCategory.getLobby(netname);
            if (ycl == null) throw new NoSuchChatroomException("Chatroom/lobby " + netname + " not found.");
            int cnt = Integer.parseInt(pkt.getValue("108"));
            while (cnt > 0 && pkt.getNthValue("109", cnt - 1) == null) cnt--;
            YahooChatUser ycu = ycl.getUser(pkt.getValue("109"));
            if (cnt == 1 && ycu != null) {
                ycu.update(pkt.getValue("113"), pkt.getValue("141"), pkt.getValue("110"), pkt.getValue("142"));
                SessionChatEvent se = new SessionChatEvent(this, 1, ycl);
                se.setChatUser(0, ycu);
                new FireEvent().fire(se, SERVICE_X_CHATUPDATE);
                return;
            } else {
                joining = pkt.exists("61");
                if (joining) ycl.clearUsers();
                Hashtable ht = new Hashtable();
                for (int i = 0; i < cnt; i++) {
                    ycu = createChatUser(pkt, i);
                    ht.put(ycu.getId(), ycu);
                }
                SessionChatEvent se = new SessionChatEvent(this, cnt, ycl);
                int i = 0;
                for (Enumeration en = ht.elements(); en.hasMoreElements(); ) {
                    ycu = (YahooChatUser) en.nextElement();
                    if (!ycl.exists(ycu)) ycl.addUser(ycu);
                    se.setChatUser(i++, ycu);
                }
                if (!joining) {
                    if (se.getChatUsers().length > 0) new FireEvent().fire(se, SERVICE_CHATLOGON);
                } else {
                    chatSessionStatus = MESSAGING;
                }
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new YMSG9BadFormatException("chat login", false, e);
        } finally {
            if (joining) chatLoginOver = true;
        }
    }

    protected void receiveChatMsg(YMSG9Packet pkt) {
        if (chatSessionStatus != MESSAGING) return;
        try {
            String netname = pkt.getValue("104");
            YahooChatLobby ycl = YahooChatCategory.getLobby(netname);
            if (ycl == null) throw new NoSuchChatroomException("Chatroom/lobby " + netname + " not found.");
            YahooChatUser ycu = ycl.getUser(pkt.getValue("109"));
            if (ycu == null) ycu = createChatUser(pkt, 0);
            SessionChatEvent se = new SessionChatEvent(this, ycu, pkt.getValue("117"), pkt.getValue("124"), ycl);
            new FireEvent().fire(se, SERVICE_CHATMSG);
        } catch (Exception e) {
            throw new YMSG9BadFormatException("chat message", false, e);
        }
    }

    protected void receiveChatPM(YMSG9Packet pkt) {
        if (chatSessionStatus != MESSAGING) return;
        try {
            SessionEvent se = new SessionEvent(this, pkt.getValue("5"), pkt.getValue("4"), pkt.getValue("14"));
            new FireEvent().fire(se, SERVICE_MESSAGE);
        } catch (Exception e) {
            throw new YMSG9BadFormatException("chat PM", false, e);
        }
    }

    protected void receiveConfAddInvite(YMSG9Packet pkt) {
        receiveConfInvite(pkt);
    }

    protected void receiveConfDecline(YMSG9Packet pkt) {
        try {
            YahooConference yc = getOrCreateConference(pkt);
            SessionConferenceEvent se = new SessionConferenceEvent(this, pkt.getValue("1"), pkt.getValue("54"), pkt.getValue("14"), yc, null);
            yc.removeUser(se.getFrom());
            if (!yc.isClosed()) new FireEvent().fire(se, SERVICE_CONFDECLINE);
        } catch (Exception e) {
            throw new YMSG9BadFormatException("conference decline", false, e);
        }
    }

    protected void receiveConfInvite(YMSG9Packet pkt) {
        try {
            YahooConference yc = getOrCreateConference(pkt);
            String[] users = pkt.getValues("52");
            SessionConferenceEvent se = new SessionConferenceEvent(this, pkt.getValue("1"), pkt.getValue("50"), pkt.getValue("58"), yc, userStore.toUserArray(users));
            yc.addUsers(users);
            yc.addUser(se.getFrom());
            if (!yc.isClosed()) new FireEvent().fire(se, SERVICE_CONFINVITE);
            synchronized (yc) {
                Vector v = yc.inviteReceived();
                for (int i = 0; i < v.size(); i++) ipThread.process((YMSG9Packet) v.elementAt(i));
            }
        } catch (Exception e) {
            throw new YMSG9BadFormatException("conference invite", false, e);
        }
    }

    protected void receiveConfLogoff(YMSG9Packet pkt) {
        YahooConference yc = getOrCreateConference(pkt);
        synchronized (yc) {
            if (!yc.isInvited()) {
                yc.addPacket(pkt);
                return;
            }
        }
        try {
            SessionConferenceEvent se = new SessionConferenceEvent(this, pkt.getValue("1"), pkt.getValue("56"), null, yc);
            yc.removeUser(se.getFrom());
            if (!yc.isClosed()) new FireEvent().fire(se, SERVICE_CONFLOGOFF);
        } catch (Exception e) {
            throw new YMSG9BadFormatException("conference logoff", false, e);
        }
    }

    protected void receiveConfLogon(YMSG9Packet pkt) {
        YahooConference yc = getOrCreateConference(pkt);
        synchronized (yc) {
            if (!yc.isInvited()) {
                yc.addPacket(pkt);
                return;
            }
        }
        try {
            SessionConferenceEvent se = new SessionConferenceEvent(this, pkt.getValue("1"), pkt.getValue("53"), null, yc);
            yc.addUser(se.getFrom());
            if (!yc.isClosed()) new FireEvent().fire(se, SERVICE_CONFLOGON);
        } catch (Exception e) {
            throw new YMSG9BadFormatException("conference logon", false, e);
        }
    }

    protected void receiveConfMsg(YMSG9Packet pkt) {
        YahooConference yc = getOrCreateConference(pkt);
        synchronized (yc) {
            if (!yc.isInvited()) {
                yc.addPacket(pkt);
                return;
            }
        }
        try {
            SessionConferenceEvent se = new SessionConferenceEvent(this, pkt.getValue("1"), pkt.getValue("3"), pkt.getValue("14"), yc);
            if (!yc.isClosed()) new FireEvent().fire(se, SERVICE_CONFMSG);
        } catch (Exception e) {
            throw new YMSG9BadFormatException("conference mesg", false, e);
        }
    }

    protected void receiveContactIgnore(YMSG9Packet pkt) {
        try {
            String n = pkt.getValue("0");
            boolean ig = pkt.getValue("13").charAt(0) == '1';
            int st = Integer.parseInt(pkt.getValue("66"));
            if (st == 0) {
                YahooUser yu = userStore.getOrCreate(n);
                yu.setIgnored(ig);
                SessionFriendEvent se = new SessionFriendEvent(this, 1);
                se.setUser(0, yu);
                new FireEvent().fire(se, SERVICE_ISAWAY);
            } else {
                String m = "Contact ignore error: ";
                switch(st) {
                    case 2:
                        m = m + "Already on ignore list";
                        break;
                    case 3:
                        m = m + "Not currently ignored";
                        break;
                    case 12:
                        m = m + "Cannot ignore friend";
                        break;
                    default:
                        m = m + "Unknown error";
                        break;
                }
                errorMessage(pkt, m);
            }
        } catch (Exception e) {
            throw new YMSG9BadFormatException("contact ignore", false, e);
        }
    }

    protected void receiveContactNew(YMSG9Packet pkt) {
        try {
            if (pkt.length <= 0) {
                return;
            } else if (pkt.exists("7")) {
                updateFriendsStatus(pkt);
                return;
            } else if (pkt.status == 0x07) {
                SessionEvent se = new SessionEvent(this, null, pkt.getValue("3"), pkt.getValue("14"));
                new FireEvent().fire(se, SERVICE_CONTACTREJECT);
            } else {
                SessionEvent se = new SessionEvent(this, pkt.getValue("1"), pkt.getValue("3"), pkt.getValue("14"), pkt.getValue("15"));
                se.setStatus(pkt.status);
                new FireEvent().fire(se, SERVICE_CONTACTNEW);
            }
        } catch (Exception e) {
            throw new YMSG9BadFormatException("contact request", false, e);
        }
    }

    protected void receiveFileTransfer(YMSG9Packet pkt) {
        try {
            if (!pkt.exists("38")) {
                SessionEvent se = new SessionEvent(this, pkt.getValue("5"), pkt.getValue("4"), pkt.getValue("14"));
                new FireEvent().fire(se, SERVICE_MESSAGE);
            } else {
                SessionFileTransferEvent se = new SessionFileTransferEvent(this, pkt.getValue("5"), pkt.getValue("4"), pkt.getValue("14"), pkt.getValue("38"), pkt.getValue("20"));
                new FireEvent().fire(se, SERVICE_FILETRANSFER);
            }
        } catch (Exception e) {
            throw new YMSG9BadFormatException("file transfer", false, e);
        }
    }

    protected void receiveFriendAdd(YMSG9Packet pkt) {
        try {
            String n = pkt.getValue("7"), s = pkt.getValue("66"), g = pkt.getValue("65");
            YahooUser yu = userStore.getOrCreate(n);
            insertFriend(yu, g);
            SessionFriendEvent se = new SessionFriendEvent(this, yu, g);
            new FireEvent().fire(se, SERVICE_FRIENDADD);
        } catch (Exception e) {
            throw new YMSG9BadFormatException("friend added", false, e);
        }
    }

    protected void receiveFriendRemove(YMSG9Packet pkt) {
        try {
            String n = pkt.getValue("7"), g = pkt.getValue("65");
            YahooUser yu = userStore.get(n);
            if (yu == null) {
                report("Unknown friend", pkt);
                return;
            }
            deleteFriend(yu, g);
            SessionFriendEvent se = new SessionFriendEvent(this, yu, g);
            new FireEvent().fire(se, SERVICE_FRIENDREMOVE);
        } catch (Exception e) {
            throw new YMSG9BadFormatException("friend removed", false, e);
        }
    }

    protected void receiveIdAct(YMSG9Packet pkt) {
    }

    protected void receiveIdDeact(YMSG9Packet pkt) {
    }

    protected void receiveIsAway(YMSG9Packet pkt) {
        if (pkt.exists("7")) {
            updateFriendsStatus(pkt);
        }
    }

    protected void receiveIsBack(YMSG9Packet pkt) {
        if (pkt.exists("7")) {
            updateFriendsStatus(pkt);
        }
    }

    protected void receiveList(YMSG9Packet pkt) {
        String[] concatFields = { "87", "88", "89" };
        if (cachePacket == null) cachePacket = pkt; else cachePacket.merge(pkt, concatFields);
        if (pkt.exists("59")) _receiveList(cachePacket);
    }

    private void _receiveList(YMSG9Packet pkt) {
        try {
            String s = pkt.getValue("87");
            if (s != null) {
                StringTokenizer st1 = new StringTokenizer(s, "\n");
                groups = new YahooGroup[st1.countTokens()];
                int i = 0;
                while (st1.hasMoreTokens()) {
                    String s1 = st1.nextToken();
                    groups[i] = new YahooGroup(s1.substring(0, s1.indexOf(":")));
                    StringTokenizer st2 = new StringTokenizer(s1.substring(s1.indexOf(":") + 1), ",");
                    while (st2.hasMoreTokens()) {
                        YahooUser yu;
                        String k = st2.nextToken();
                        yu = userStore.getOrCreate(k);
                        groups[i].addUser(yu);
                        yu.adjustGroupCount(+1);
                    }
                    i++;
                }
            }
        } catch (Exception e) {
            throw new YMSG9BadFormatException("friends list in list", false, e);
        }
        try {
            String s = pkt.getValue("88");
            if (s != null) {
                StringTokenizer st = new StringTokenizer(s, ",");
                while (st.hasMoreTokens()) {
                    s = st.nextToken();
                    YahooUser yu = userStore.getOrCreate(s);
                    yu.setIgnored(true);
                }
            }
        } catch (Exception e) {
            throw new YMSG9BadFormatException("ignored list in list", false, e);
        }
        try {
            String s = pkt.getValue("89");
            if (s != null) {
                StringTokenizer st = new StringTokenizer(s, ",");
                int i = 0;
                identities = new YahooIdentity[st.countTokens()];
                while (st.hasMoreTokens()) {
                    identities[i++] = new YahooIdentity(st.nextToken());
                }
            }
        } catch (Exception e) {
            throw new YMSG9BadFormatException("identities list in list", false, e);
        }
        try {
            String[] ck = ConnectionHandler.extractCookies(pkt);
            cookieY = ck[COOKIE_Y];
            cookieT = ck[COOKIE_T];
            cookieC = ck[COOKIE_C];
        } catch (Exception e) {
            throw new YMSG9BadFormatException("cookies in list", false, e);
        }
        try {
            if (pkt.exists("3")) primaryID = pkt.getValue("3").trim(); else primaryID = loginID;
        } catch (Exception e) {
            throw new YMSG9BadFormatException("primary identity in list", false, e);
        }
        identityIdToObject(primaryID).setPrimaryIdentity(true);
        identityIdToObject(loginID).setLoginIdentity(true);
        if (loginOver) new FireEvent().fire(new SessionEvent(this), SERVICE_LIST);
    }

    protected void receiveLogoff(YMSG9Packet pkt) {
        if (!pkt.exists("7")) {
            sessionStatus = UNSTARTED;
            ipThread.quit = true;
        } else {
            try {
                updateFriendsStatus(pkt);
            } catch (Exception e) {
                throw new YMSG9BadFormatException("online friends in logoff", false, e);
            }
        }
    }

    protected void receiveLogon(YMSG9Packet pkt) {
        if (pkt.exists("7")) {
            try {
                updateFriendsStatus(pkt);
            } catch (Exception e) {
                throw new YMSG9BadFormatException("online friends in logon", false, e);
            }
        }
        if (!loginOver) {
            try {
                if (status == STATUS_AVAILABLE) transmitIsBack(); else transmitIsAway();
            } catch (IOException e) {
            }
            sessionStatus = MESSAGING;
            loginOver = true;
        }
    }

    protected void receiveMessage(YMSG9Packet pkt) {
        try {
            if (!pkt.exists("14")) {
                return;
            } else if (pkt.status == STATUS_NOTINOFFICE) {
                int i = 0;
                String s = pkt.getNthValue("31", i);
                while (s != null) {
                    SessionEvent se = new SessionEvent(this, pkt.getNthValue("5", i), pkt.getNthValue("4", i), pkt.getNthValue("14", i), pkt.getNthValue("15", i));
                    new FireEvent().fire(se, SERVICE_X_OFFLINE);
                    i++;
                    s = pkt.getNthValue("31", i);
                }
            } else {
                SessionEvent se = new SessionEvent(this, pkt.getValue("5"), pkt.getValue("4"), pkt.getValue("14"));
                if (se.getMessage().equalsIgnoreCase(BUZZ)) new FireEvent().fire(se, SERVICE_X_BUZZ); else new FireEvent().fire(se, SERVICE_MESSAGE);
            }
        } catch (Exception e) {
            throw new YMSG9BadFormatException("message", false, e);
        }
    }

    protected void receiveNewMail(YMSG9Packet pkt) {
        try {
            SessionNewMailEvent se;
            if (!pkt.exists("43")) {
                se = new SessionNewMailEvent(this, pkt.getValue("9"));
            } else {
                se = new SessionNewMailEvent(this, pkt.getValue("43"), pkt.getValue("42"), pkt.getValue("18"));
            }
            new FireEvent().fire(se, SERVICE_NEWMAIL);
        } catch (Exception e) {
            throw new YMSG9BadFormatException("new mail", false, e);
        }
    }

    protected void receiveNotify(YMSG9Packet pkt) {
        try {
            if (pkt.status == 0x01) {
                SessionNotifyEvent se = new SessionNotifyEvent(this, pkt.getValue("5"), pkt.getValue("4"), pkt.getValue("14"), pkt.getValue("49"), pkt.getValue("13"));
                se.setStatus(pkt.status);
                new FireEvent().fire(se, SERVICE_NOTIFY);
            }
        } catch (Exception e) {
            throw new YMSG9BadFormatException("notify", false, e);
        }
    }

    protected void receivePing(YMSG9Packet pkt) {
    }

    protected void receiveUserStat(YMSG9Packet pkt) {
        status = pkt.status;
    }

    protected void erroneousChatLogin(YMSG9Packet pkt) {
        chatSessionStatus = FAILED;
        chatLoginOver = true;
    }

    protected void sendPacket(PacketBodyBuffer body, int service, long status) throws IOException {
        network.sendPacket(body, service, status, sessionId);
    }

    protected void sendPacket(PacketBodyBuffer body, int service) throws IOException {
        sendPacket(body, service, STATUS_AVAILABLE);
    }

    private void report(String s, YMSG9Packet p) {
        System.err.println(s + "\n" + p.toString() + "\n");
    }

    private boolean past(long time) {
        return (System.currentTimeMillis() > time);
    }

    private void startThreads() {
        ipThread = new InputThread();
        pingThread = new PingThread();
    }

    private void closeNetwork() {
        if (pingThread != null) {
            pingThread.quit = true;
            pingThread.interrupt();
        }
        if (network != null) try {
            network.close();
            network = null;
        } catch (IOException e) {
        }
    }

    private void checkStatus() throws IllegalStateException {
        if (sessionStatus != MESSAGING) throw new IllegalStateException("Not logged in");
    }

    private void checkChatStatus() throws IllegalStateException {
        if (chatSessionStatus != MESSAGING) throw new IllegalStateException("Not logged in to a chatroom");
    }

    private YahooIdentity identityIdToObject(String yid) {
        for (int i = 0; i < identities.length; i++) if (yid.equals(identities[i].getId())) return identities[i];
        return null;
    }

    private void checkIdentity(YahooIdentity yid) throws IllegalIdentityException {
        for (int i = 0; i < identities.length; i++) if (yid == identities[i]) return;
        throw new IllegalIdentityException(yid + " not a valid identity for this session");
    }

    private void checkIdentityNotOnList(String[] yids) {
        for (int i = 0; i < yids.length; i++) {
            if (identityIdToObject(yids[i]) != null) throw new IllegalIdentityException(yids[i] + " is an identity of this session and cannot be used here");
        }
    }

    private void resetData() {
        primaryID = null;
        loginID = null;
        password = null;
        cookieY = null;
        cookieT = null;
        cookieC = null;
        imvironment = null;
        customStatusMessage = null;
        customStatusBusy = false;
        groups = null;
        identities = null;
        clearTypingNotifiers();
        loginOver = false;
        loginException = null;
        chatConnectOver = false;
        chatLoginOver = false;
    }

    private void clearTypingNotifiers() {
        synchronized (typingNotifiers) {
            for (Enumeration e = typingNotifiers.keys(); e.hasMoreElements(); ) {
                String key = (String) e.nextElement();
                TypingNotifier tn = (TypingNotifier) typingNotifiers.get(key);
                tn.quit = true;
                tn.interrupt();
                typingNotifiers.remove(key);
            }
        }
    }

    private void errorMessage(YMSG9Packet pkt, String m) {
        if (m == null) m = pkt.getValue("16");
        SessionErrorEvent se = new SessionErrorEvent(this, m, pkt.service);
        if (pkt.exists("114")) se.setCode(Integer.parseInt(pkt.getValue("114").trim()));
        new FireEvent().fire(se, SERVICE_X_ERROR);
    }

    private YMSG9Packet compoundChatLoginPacket(YMSG9Packet pkt) {
        if (pkt.status == STATUS_INCOMPLETE) {
            if (cachePacket == null) {
                cachePacket = pkt;
            } else {
                cachePacket.append(pkt);
            }
            return null;
        } else if (pkt.status == STATUS_COMPLETE) {
            if (cachePacket != null) {
                cachePacket.append(pkt);
                pkt = cachePacket;
                cachePacket = null;
            }
            return pkt;
        } else {
            return pkt;
        }
    }

    private void updateFriendsStatus(YMSG9Packet pkt) {
        String s = pkt.getValue("8");
        if (s == null && pkt.getValue("7") != null) s = "1";
        boolean logoff = (pkt.service == SERVICE_LOGOFF);
        if (s != null) {
            int cnt = Integer.parseInt(s);
            SessionFriendEvent se = new SessionFriendEvent(this, cnt);
            for (int i = 0; i < cnt; i++) {
                YahooUser yu = userStore.get(pkt.getNthValue("7", i));
                if (yu == null) {
                    String n = pkt.getNthValue("7", i);
                    yu = userStore.getOrCreate(n);
                }
                yu.update(pkt.getNthValue("7", i), logoff ? STATUS_OFFLINE + "" : pkt.getNthValue("10", i), pkt.getNthValue("17", i), pkt.getNthValue("13", i));
                if (pkt.getNthValue("19", i) != null && pkt.getNthValue("47", i) != null) {
                    yu.setCustom(pkt.getNthValue("19", i), pkt.getNthValue("47", i));
                }
                se.setUser(i, yu);
            }
            new FireEvent().fire(se, SERVICE_ISAWAY);
        }
    }

    private void insertFriend(YahooUser yu, String gr) {
        int idx;
        for (idx = 0; idx < groups.length; idx++) if (groups[idx].getName().equalsIgnoreCase(gr)) break;
        if (idx >= groups.length) {
            YahooGroup[] arr = new YahooGroup[groups.length + 1];
            int j = 0, k = 0;
            while (j < groups.length && groups[j].getName().compareTo(gr) < 0) {
                arr[j] = groups[j];
                j++;
            }
            idx = j;
            arr[idx] = new YahooGroup(gr);
            while (j < groups.length) {
                arr[j + 1] = groups[j];
                j++;
            }
            groups = arr;
        }
        if (groups[idx].getIndexOfFriend(yu.getId()) < 0) {
            groups[idx].addUser(yu);
            yu.adjustGroupCount(+1);
        }
    }

    private void deleteFriend(YahooUser yu, String gr) {
        int idx, j;
        for (idx = 0; idx < groups.length; idx++) if (groups[idx].getName().equalsIgnoreCase(gr)) break;
        if (idx >= groups.length) return;
        j = groups[idx].getIndexOfFriend(yu.getId());
        if (j < 0) return;
        groups[idx].removeUserAt(j);
        yu.adjustGroupCount(-1);
        if (groups[idx].isEmpty()) {
            YahooGroup[] arr = new YahooGroup[groups.length - 1];
            for (j = 0; j < idx; j++) arr[j] = groups[j];
            for (j = idx; j < arr.length; j++) arr[j] = groups[j + 1];
            groups = arr;
        }
    }

    private YahooChatUser createChatUser(YMSG9Packet pkt, int i) {
        YahooUser yu = userStore.getOrCreate(pkt.getNthValue("109", i));
        return new YahooChatUser(yu, pkt.getValueFromNthSet("109", "113", i), pkt.getValueFromNthSet("109", "141", i), pkt.getValueFromNthSet("109", "110", i), pkt.getValueFromNthSet("109", "142", i));
    }

    private String getConferenceName(String yid) {
        return yid + "-" + conferenceCount++;
    }

    private YahooConference getConference(String room) throws NoSuchConferenceException {
        YahooConference yc = (YahooConference) conferences.get(room);
        if (yc == null) throw new NoSuchConferenceException("Conference " + room + " not found."); else return yc;
    }

    private YahooConference getOrCreateConference(YMSG9Packet pkt) {
        String room = pkt.getValue("57");
        YahooIdentity yid = identityIdToObject(pkt.getValue("1"));
        YahooConference yc = (YahooConference) conferences.get(room);
        if (yc == null) {
            yc = new YahooConference(userStore, yid, room, this);
            conferences.put(room, yc);
        }
        return yc;
    }

    private class InputThread extends Thread {

        public boolean quit = false;

        public InputThread() {
            super(ymsgThreads, "Network Input");
            this.start();
        }

        public void run() {
            try {
                while (!quit) {
                    try {
                        process(network.receivePacket());
                    } catch (Exception e) {
                        try {
                            SessionExceptionEvent se = new SessionExceptionEvent(Session.this, "Source: InputThread", e);
                            new FireEvent().fire(se, SERVICE_X_EXCEPTION);
                        } catch (Exception e2) {
                            e2.printStackTrace();
                        }
                    }
                }
            } finally {
                closeNetwork();
                new FireEvent().fire(new SessionEvent(this), SERVICE_LOGOFF);
            }
        }

        void process(YMSG9Packet pkt) throws Exception {
            if (pkt == null) {
                quit = true;
                return;
            }
            if (pkt.sessionId != 0) sessionId = pkt.sessionId;
            if (pkt.status == -1 && processError(pkt) == true) return;
            switch(pkt.service) {
                case SERVICE_ADDIGNORE:
                    receiveAddIgnore(pkt);
                    break;
                case SERVICE_AUTH:
                    receiveAuth(pkt);
                    break;
                case SERVICE_AUTHRESP:
                    receiveAuthResp(pkt);
                    break;
                case SERVICE_CHATCONNECT:
                    receiveChatConnect(pkt);
                    break;
                case SERVICE_CHATDISCONNECT:
                    receiveChatDisconnect(pkt);
                    break;
                case SERVICE_CHATLOGOFF:
                    receiveChatLogoff(pkt);
                    break;
                case SERVICE_CHATLOGON:
                    receiveChatLogon(pkt);
                    break;
                case SERVICE_CHATMSG:
                    receiveChatMsg(pkt);
                    break;
                case SERVICE_CHATPM:
                    receiveChatPM(pkt);
                    break;
                case SERVICE_CONFADDINVITE:
                    receiveConfAddInvite(pkt);
                    break;
                case SERVICE_CONFDECLINE:
                    receiveConfDecline(pkt);
                    break;
                case SERVICE_CONFINVITE:
                    receiveConfInvite(pkt);
                    break;
                case SERVICE_CONFLOGOFF:
                    receiveConfLogoff(pkt);
                    break;
                case SERVICE_CONFLOGON:
                    receiveConfLogon(pkt);
                    break;
                case SERVICE_CONFMSG:
                    receiveConfMsg(pkt);
                    break;
                case SERVICE_CONTACTIGNORE:
                    receiveContactIgnore(pkt);
                    break;
                case SERVICE_CONTACTNEW:
                    receiveContactNew(pkt);
                    break;
                case SERVICE_FILETRANSFER:
                    receiveFileTransfer(pkt);
                    break;
                case SERVICE_FRIENDADD:
                    receiveFriendAdd(pkt);
                    break;
                case SERVICE_FRIENDREMOVE:
                    receiveFriendRemove(pkt);
                    break;
                case SERVICE_IDACT:
                    receiveIdAct(pkt);
                    break;
                case SERVICE_IDDEACT:
                    receiveIdDeact(pkt);
                    break;
                case SERVICE_ISAWAY:
                    receiveIsAway(pkt);
                    break;
                case SERVICE_ISBACK:
                    receiveIsBack(pkt);
                    break;
                case SERVICE_LIST:
                    receiveList(pkt);
                    break;
                case SERVICE_LOGOFF:
                    receiveLogoff(pkt);
                    break;
                case SERVICE_LOGON:
                    receiveLogon(pkt);
                    break;
                case SERVICE_MESSAGE:
                    receiveMessage(pkt);
                    break;
                case SERVICE_NEWMAIL:
                    receiveNewMail(pkt);
                    break;
                case SERVICE_NOTIFY:
                    receiveNotify(pkt);
                    break;
                case SERVICE_PING:
                    receivePing(pkt);
                    break;
                case SERVICE_USERSTAT:
                    receiveUserStat(pkt);
                    break;
                default:
                    System.out.println("UNKNOWN: " + pkt.toString());
                    break;
            }
        }

        boolean processError(YMSG9Packet pkt) throws Exception {
            switch(pkt.service) {
                case SERVICE_AUTHRESP:
                    receiveAuthResp(pkt);
                    return true;
                case SERVICE_CHATLOGON:
                    receiveChatLogon(pkt);
                    return true;
                case SERVICE_LOGOFF:
                    receiveLogoff(pkt);
                    return true;
                default:
                    errorMessage(pkt, null);
                    return (pkt.body.length <= 2);
            }
        }
    }

    private class PingThread extends Thread {

        public boolean quit = false;

        public int time = 1000 * 60 * 20;

        public PingThread() {
            super(ymsgThreads, "Ping");
            this.setPriority(Thread.MIN_PRIORITY);
            this.start();
        }

        public void run() {
            try {
                Thread.sleep(time);
            } catch (InterruptedException e) {
            }
            while (!quit) {
                try {
                    transmitPing();
                    if (currentLobby != null) transmitChatPing();
                    try {
                        Thread.sleep(time);
                    } catch (InterruptedException e) {
                    }
                } catch (Exception e) {
                }
            }
        }
    }

    private class TypingNotifier extends java.awt.event.KeyAdapter implements Runnable {

        public boolean quit = false;

        private long lastKey;

        private int timeout = 1000 * 30;

        private boolean typing = false;

        private Thread thread;

        private Component typeSource;

        private int listenerCnt = 0;

        private String target;

        private String identity;

        public TypingNotifier(Component com, String to, String from) {
            typeSource = com;
            target = to;
            identity = from;
            if (typeSource != null) typeSource.addKeyListener(this);
            thread = new Thread(ymsgThreads, this, "Typing Notification: " + from + "->" + to);
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.start();
        }

        public void keyTyped(KeyEvent ev) {
            keyTyped();
        }

        void keyTyped() {
            if (!thread.isAlive() || sessionStatus != MESSAGING) return;
            lastKey = System.currentTimeMillis();
            if (!typing) {
                try {
                    transmitNotify(target, identity, true, " ", NOTIFY_TYPING);
                } catch (IOException e) {
                }
            }
            typing = true;
        }

        public void run() {
            try {
                while (!quit) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                    if (sessionStatus == MESSAGING && typing && System.currentTimeMillis() - lastKey > timeout) {
                        try {
                            transmitNotify(target, identity, false, " ", NOTIFY_TYPING);
                        } catch (IOException e) {
                        }
                        typing = false;
                    }
                }
            } finally {
                if (typeSource != null) typeSource.removeKeyListener(this);
            }
        }

        public void interrupt() {
            thread.interrupt();
        }

        public void stopTyping() {
            if (typing) {
                try {
                    transmitNotify(target, identity, false, " ", NOTIFY_TYPING);
                } catch (IOException e) {
                }
                typing = false;
            }
        }
    }

    private class FireEvent extends Thread {

        int type;

        SessionEvent ev;

        FireEvent() {
            super(ymsgThreads, "Event Fired");
        }

        void fire(SessionEvent ev, int t) {
            this.ev = ev;
            type = t;
            start();
        }

        public void start() {
            if (listeners.size() > 0) super.start();
        }

        public void run() {
            for (int i = 0; i < listeners.size(); i++) {
                SessionListener l = (SessionListener) listeners.elementAt(i);
                switch(type) {
                    case SERVICE_LOGOFF:
                        l.connectionClosed(ev);
                        break;
                    case SERVICE_ISAWAY:
                        l.friendsUpdateReceived((SessionFriendEvent) ev);
                        break;
                    case SERVICE_MESSAGE:
                        l.messageReceived(ev);
                        break;
                    case SERVICE_X_OFFLINE:
                        l.offlineMessageReceived(ev);
                        break;
                    case SERVICE_NEWMAIL:
                        l.newMailReceived((SessionNewMailEvent) ev);
                        break;
                    case SERVICE_CONTACTNEW:
                        l.contactRequestReceived((SessionEvent) ev);
                        break;
                    case SERVICE_CONFDECLINE:
                        l.conferenceInviteDeclinedReceived((SessionConferenceEvent) ev);
                        break;
                    case SERVICE_CONFINVITE:
                        l.conferenceInviteReceived((SessionConferenceEvent) ev);
                        break;
                    case SERVICE_CONFLOGON:
                        l.conferenceLogonReceived((SessionConferenceEvent) ev);
                        break;
                    case SERVICE_CONFLOGOFF:
                        l.conferenceLogoffReceived((SessionConferenceEvent) ev);
                        break;
                    case SERVICE_CONFMSG:
                        l.conferenceMessageReceived((SessionConferenceEvent) ev);
                        break;
                    case SERVICE_FILETRANSFER:
                        l.fileTransferReceived((SessionFileTransferEvent) ev);
                        break;
                    case SERVICE_NOTIFY:
                        l.notifyReceived((SessionNotifyEvent) ev);
                        break;
                    case SERVICE_LIST:
                        l.listReceived(ev);
                        break;
                    case SERVICE_FRIENDADD:
                        l.friendAddedReceived((SessionFriendEvent) ev);
                        break;
                    case SERVICE_FRIENDREMOVE:
                        l.friendRemovedReceived((SessionFriendEvent) ev);
                        break;
                    case SERVICE_CONTACTREJECT:
                        l.contactRejectionReceived((SessionEvent) ev);
                        break;
                    case SERVICE_CHATLOGON:
                        l.chatLogonReceived((SessionChatEvent) ev);
                        break;
                    case SERVICE_CHATLOGOFF:
                        l.chatLogoffReceived((SessionChatEvent) ev);
                        break;
                    case SERVICE_CHATDISCONNECT:
                        l.chatConnectionClosed((SessionEvent) ev);
                        break;
                    case SERVICE_CHATMSG:
                        l.chatMessageReceived((SessionChatEvent) ev);
                        break;
                    case SERVICE_X_CHATUPDATE:
                        l.chatUserUpdateReceived((SessionChatEvent) ev);
                        break;
                    case SERVICE_X_ERROR:
                        l.errorPacketReceived((SessionErrorEvent) ev);
                        break;
                    case SERVICE_X_EXCEPTION:
                        l.inputExceptionThrown((SessionExceptionEvent) ev);
                        break;
                    case SERVICE_X_BUZZ:
                        l.buzzReceived(ev);
                        break;
                    default:
                        System.out.println("UNKNOWN event: " + type);
                        break;
                }
            }
        }
    }

    public static void dump(Session s) {
        YahooGroup[] yg = s.getGroups();
        for (int i = 0; i < yg.length; i++) {
            System.out.print(yg[i].getName() + ": ");
            Vector v = yg[i].getMembers();
            for (int j = 0; j < v.size(); j++) {
                YahooUser yu = (YahooUser) v.elementAt(j);
                System.out.print(yu.getId() + " ");
            }
            System.out.print("\n");
        }
        Hashtable h = s.userStore.getUsers();
        for (Enumeration e = h.keys(); e.hasMoreElements(); ) {
            String k = (String) e.nextElement();
            YahooUser yu = (YahooUser) h.get(k);
            System.out.println(k + " = " + yu.getId());
        }
        YahooIdentity[] ya = s.getIdentities();
        for (int i = 0; i < ya.length; i++) System.out.print(ya[i].getId() + " ");
        System.out.print("\n");
    }
}
