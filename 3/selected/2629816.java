package net.kano.joscardemo;

import net.kano.joscar.ByteBlock;
import net.kano.joscar.OscarTools;
import net.kano.joscar.SeqNum;
import net.kano.joscar.flapcmd.SnacCommand;
import net.kano.joscar.net.ConnDescriptor;
import net.kano.joscar.snac.SnacRequest;
import net.kano.joscar.snac.SnacRequestListener;
import net.kano.joscar.snaccmd.FullRoomInfo;
import net.kano.joscar.snaccmd.FullUserInfo;
import net.kano.joscar.snaccmd.chat.ChatMsg;
import net.kano.joscar.snaccmd.conn.ServiceRequest;
import net.kano.joscar.snaccmd.icbm.OldIconHashInfo;
import net.kano.joscar.snaccmd.icbm.SendImIcbm;
import net.kano.joscar.snaccmd.rooms.JoinRoomCmd;
import net.kano.joscardemo.security.SecureSession;
import net.kano.joscardemo.security.SecureSessionException;
import net.kano.joustsim.Screenname;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JoscarTester implements CmdLineListener, ServiceListener {

    public static byte[] hashIcon(String filename) throws IOException {
        FileInputStream in = new FileInputStream(filename);
        try {
            byte[] block = new byte[1024];
            MessageDigest md;
            try {
                md = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                return null;
            }
            for (; ; ) {
                int count = in.read(block);
                if (count == -1) break;
                md.update(block, 0, count);
            }
            return md.digest();
        } finally {
            in.close();
        }
    }

    private static final int DEFAULT_SERVICE_PORT = 5190;

    private CLHandler clHandler = new CLHandler(this);

    private String sn = null;

    private String pass = null;

    private int uin = -1;

    private SeqNum icqSeqNum = new SeqNum(0, Integer.MAX_VALUE);

    private LoginConn loginConn = null;

    private BosFlapConn bosConn = null;

    private Set<ServiceConn> services = new HashSet<ServiceConn>();

    private Map<String, ChatConn> chats = new HashMap<String, ChatConn>();

    private NumberFormat formatter = NumberFormat.getNumberInstance();

    {
        formatter.setGroupingUsed(true);
        formatter.setMaximumFractionDigits(1);
    }

    private SecureSession secureSession = SecureSession.getInstance();

    private String aimExp = "the60s";

    private String mb(long bytes) {
        float divby;
        String suffix;
        if (bytes > 1000000) {
            divby = 1048576.0f;
            suffix = "MB";
        } else {
            divby = 1024.0f;
            suffix = "KB";
        }
        return formatter.format(bytes / divby) + suffix;
    }

    public JoscarTester(String sn, String pass) {
        new CmdLineReader(this);
        this.sn = sn;
        this.pass = pass;
        try {
            this.uin = Integer.parseInt(this.sn);
        } catch (NumberFormatException ex) {
            this.uin = -1;
        }
    }

    public String getScreenname() {
        return sn;
    }

    public String getPassword() {
        return pass;
    }

    public int getUIN() {
        if (uin == -1) {
            throw new IllegalStateException("Not connected to ICQ");
        }
        return uin;
    }

    public long nextIcqId() {
        return icqSeqNum.next();
    }

    public void connect() {
        ConnDescriptor cd = new ConnDescriptor("login.oscar.aol.com", DEFAULT_SERVICE_PORT);
        loginConn = new LoginConn(cd, this);
        loginConn.connect();
    }

    void loginFailed(String reason) {
        System.err.println("login failed: " + reason);
    }

    void setScreennameFormat(String screenname) {
        sn = screenname;
    }

    void startBosConn(String server, int port, ByteBlock cookie) {
        bosConn = new BosFlapConn(new ConnDescriptor(server, port), this, cookie);
        bosConn.connect();
    }

    void registerSnacFamilies(BasicConn conn) {
        snacMgr.register(conn);
    }

    public void connectToService(int snacFamily, String host, ByteBlock cookie) {
        ConnDescriptor cd = new ConnDescriptor(host, DEFAULT_SERVICE_PORT);
        ServiceConn conn = new ServiceConn(cd, this, cookie, snacFamily);
        conn.connect();
    }

    public void serviceFailed(ServiceConn conn) {
    }

    public void serviceConnected(ServiceConn conn) {
        services.add(conn);
    }

    public void serviceReady(ServiceConn conn) {
        snacMgr.dequeueSnacs(conn);
    }

    public void serviceDied(ServiceConn conn) {
        services.remove(conn);
        snacMgr.unregister(conn);
    }

    public void joinChat(int exchange, String roomname) {
        FullRoomInfo roomInfo = new FullRoomInfo(exchange, roomname, "us-ascii", "en");
        handleRequest(new SnacRequest(new JoinRoomCmd(roomInfo), null));
    }

    void connectToChat(FullRoomInfo roomInfo, String host, ByteBlock cookie) {
        ConnDescriptor cd = new ConnDescriptor(host, DEFAULT_SERVICE_PORT);
        ChatConn conn = new ChatConn(cd, this, cookie, roomInfo);
        conn.addChatListener(new MyChatConnListener());
        conn.connect();
    }

    public ChatConn getChatConn(String name) {
        return chats.get(Screenname.normalize(name));
    }

    private SnacManager snacMgr = new SnacManager(new PendingSnacListener() {

        public void dequeueSnacs(List<SnacRequest> pending) {
            System.out.println("dequeuing " + pending.size() + " snacs");
            for (SnacRequest request : pending) {
                handleRequest(request);
            }
        }
    });

    synchronized void handleRequest(SnacRequest request) {
        int family = request.getCommand().getFamily();
        if (snacMgr.isPending(family)) {
            snacMgr.addRequest(request);
            return;
        }
        BasicConn conn = snacMgr.getConn(family);
        if (conn != null) {
            conn.sendRequest(request);
        } else {
            if (!(request.getCommand() instanceof ServiceRequest)) {
                System.out.println("requesting " + Integer.toHexString(family) + " service.");
                snacMgr.setPending(family, true);
                snacMgr.addRequest(request);
                request(new ServiceRequest(family));
            } else {
                System.err.println("eep! can't find a service redirector " + "server.");
            }
        }
    }

    public SnacRequest request(SnacCommand cmd) {
        return request(cmd, null);
    }

    private SnacRequest request(SnacCommand cmd, SnacRequestListener listener) {
        SnacRequest req = new SnacRequest(cmd, listener);
        handleRequest(req);
        return req;
    }

    private OldIconHashInfo oldIconInfo;

    private File iconFile = null;

    {
        if (false) {
            try {
                ClassLoader classLoader = getClass().getClassLoader();
                URL iconResource = classLoader.getResource("images/beck.gif");
                String ext = iconResource.toExternalForm();
                System.out.println("ext: " + ext);
                URI uri = new URI(ext);
                iconFile = new File(uri);
                oldIconInfo = new OldIconHashInfo(iconFile);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
    }

    public OldIconHashInfo getOldIconInfo() {
        return oldIconInfo;
    }

    private static final Pattern cmdRE = Pattern.compile("\\s*(?:\"(.*?[^\\\\]?)\"|(\\S+))(?:\\s|$)");

    public void processCmd(CmdLineReader reader, String line) {
        Matcher m = cmdRE.matcher(line);
        LinkedList<String> arglist = new LinkedList<String>();
        while (m.find()) {
            String arg = m.group(1);
            if (arg == null) arg = m.group(2);
            if (arg == null) {
                System.err.println("Error: line parser failed to read " + "arguments");
                return;
            }
            arglist.add(arg);
        }
        if (arglist.isEmpty()) return;
        String cmd = arglist.removeFirst();
        CLCommand handler = clHandler.getCommand(cmd);
        if (handler == null) {
            System.err.println("!! There is no command called '" + cmd + "'");
            System.err.println("!! Try typing 'help'");
        } else {
            try {
                handler.handle(this, line, cmd, arglist);
            } catch (Throwable t) {
                System.err.println("!! Error executing command '" + cmd + "'!");
                System.err.println("!! Try typing 'help " + cmd + "'");
                System.err.println("!! Stack trace of error:");
                t.printStackTrace();
                System.err.println("!! Try typing 'help " + cmd + "'");
            }
        }
    }

    public void printMemUsage() {
        Runtime runtime = Runtime.getRuntime();
        long total = runtime.totalMemory();
        long free = runtime.freeMemory();
        System.out.println("Using " + mb(total - free) + " memory of " + mb(total) + " allocated");
    }

    public void sendIM(String nick, String text) {
        request(new SendImIcbm(nick, text));
    }

    public SecureSession getSecureSession() {
        return secureSession;
    }

    public void setAimExp(String aimExp) {
        this.aimExp = aimExp;
    }

    public String getAimExp() {
        return aimExp;
    }

    public BasicConn getBosConn() {
        return bosConn;
    }

    private class MyChatConnListener implements ChatConnListener {

        public void connFailed(ChatConn conn, Object reason) {
        }

        public void connected(ChatConn conn) {
        }

        public void joined(ChatConn conn, List<FullUserInfo> members) {
            String name = conn.getRoomInfo().getName();
            chats.put(Screenname.normalize(name), conn);
            System.out.println("*** Joined " + conn.getRoomInfo().getRoomName() + ", members:");
            for (FullUserInfo member : members) {
                System.out.println("  " + member.getScreenname());
            }
        }

        public void left(ChatConn conn, Object reason) {
            String name = conn.getRoomInfo().getName();
            chats.remove(Screenname.normalize(name));
            System.out.println("*** Left " + conn.getRoomInfo().getRoomName());
        }

        public void usersJoined(ChatConn conn, List<FullUserInfo> members) {
            for (FullUserInfo member : members) {
                System.out.println("*** " + member.getScreenname() + " joined " + conn.getRoomInfo().getRoomName());
            }
        }

        public void usersLeft(ChatConn conn, List<FullUserInfo> members) {
            for (FullUserInfo member : members) {
                System.out.println("*** " + member.getScreenname() + " left " + conn.getRoomInfo().getRoomName());
            }
        }

        public void gotMsg(ChatConn conn, FullUserInfo sender, ChatMsg msg) {
            String msgStr = msg.getMessage();
            String ct = msg.getContentType();
            if (msgStr == null && ct.equals(ChatMsg.CONTENTTYPE_SECURE)) {
                ByteBlock msgData = msg.getMessageData();
                try {
                    msgStr = secureSession.parseChatMessage(conn.getRoomName(), sender.getScreenname(), msgData);
                } catch (SecureSessionException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("<" + sender.getScreenname() + ":#" + conn.getRoomInfo().getRoomName() + "> " + OscarTools.stripHtml(msgStr));
        }
    }

    public static void main(String[] args) throws IOException {
        String screenname;
        String password;
        if (args.length != 2) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            System.out.print("Screenname: ");
            screenname = reader.readLine();
            System.out.print("Password: ");
            password = reader.readLine();
        } else {
            screenname = args[0];
            password = args[1];
        }
        String levelstr = "fine";
        System.out.println("Connecting to AIM as " + screenname + "...");
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new Formatter() {

            private String lineSeparator = System.getProperty("line.separator");

            public String format(LogRecord record) {
                StringBuffer sb = new StringBuffer();
                sb.append("[");
                sb.append(record.getLevel().getLocalizedName());
                sb.append("] ");
                sb.append(record.getMessage());
                sb.append(lineSeparator);
                if (record.getThrown() != null) {
                    try {
                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter(sw);
                        record.getThrown().printStackTrace(pw);
                        pw.close();
                        sb.append(sw.toString());
                    } catch (Exception ex) {
                    }
                }
                return sb.toString();
            }
        });
        Level level = Level.parse(levelstr.toUpperCase());
        handler.setLevel(level);
        Logger logger = Logger.getLogger("net.kano.joscar");
        logger.addHandler(handler);
        logger.setLevel(Level.ALL);
        final JoscarTester tester = new JoscarTester(screenname, password);
        tester.connect();
        new Timer(true).scheduleAtFixedRate(new TimerTask() {

            public void run() {
                tester.printMemUsage();
            }
        }, 30 * 1000, 5 * 60 * 1000);
    }
}
