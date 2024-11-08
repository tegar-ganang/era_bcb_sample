package vavi.net.xmpp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import javax.net.ssl.SSLSocketFactory;
import org.xml.sax.SAXException;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.digester.Digester;
import org.apache.commons.digester.xmlrules.DigesterLoader;
import vavi.util.Debug;
import vavi.util.StringUtil;
import vavi.util.event.GenericEvent;
import vavi.util.event.GenericListener;
import vavi.util.event.GenericSupport;

/**
 * JabberConnection.
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 040117 nsano initial version <br>
 */
public class JabberConnection implements Runnable {

    /** */
    private String server;

    /** */
    public String getServer() {
        return server;
    }

    /** */
    public void setServer(String s) {
        server = s;
    }

    /** */
    private int port;

    /** */
    public int getPort() {
        return port;
    }

    /** */
    public void setPort(int s) {
        port = s;
    }

    /** */
    private int state = 0;

    /** */
    private static final int INITSTATE = 0;

    /** */
    private static final int SASL = 1;

    /** */
    private static final int NOTSASL = 2;

    /** */
    private static final int QUERIEDAUTH = 3;

    /** */
    private static final int SENTLOGINDATA = 4;

    /** */
    private static final int LOGGEDIN = 5;

    /** */
    private static final int NEWA = 10;

    /** */
    public Pinger pinger;

    /** */
    private InputStream in;

    /** */
    private Writer out;

    /** */
    boolean newA = false;

    Roster roster;

    String version;

    /** */
    public void setNew(boolean b) {
        newA = b;
    }

    /** */
    String email = "support@die-horde.de";

    /** */
    public JabberConnection(String resource, String password, String userName, boolean useSSL, boolean usePlainPasswd) {
        this.resource = resource;
        this.password = password;
        this.userName = userName;
        this.useSSL = useSSL;
        this.usePlainPasswd = usePlainPasswd;
    }

    /** */
    public void connect() throws IOException, SAXException {
        state = 0;
        Socket socket = null;
        if (!useSSL) {
            socket = new Socket(server, port);
        } else {
            socket = SSLSocketFactory.getDefault().createSocket(server, port);
        }
        in = socket.getInputStream();
        OutputStream os = socket.getOutputStream();
        out = new BufferedWriter(new OutputStreamWriter(os, "UTF8"));
        pinger = new Pinger(socket);
        pinger.start();
        Digester digester = DigesterLoader.createDigester(getClass().getResource("xmpp.xml"));
        digester.setNamespaceAware(true);
        startStream();
        while (true) {
            Xmpp xmpp = (Xmpp) digester.parse(in);
            parse(xmpp);
        }
    }

    /** */
    public void disconnect() throws IOException {
        fireEvent(this, "disconnected");
    }

    /** */
    public void run() {
        try {
            connect();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            try {
                disconnect();
            } catch (IOException f) {
                System.err.println(e);
            }
        }
    }

    /** */
    public void writeLine(String s) throws IOException {
        if (s.equals("\n")) {
            out.write(s);
            out.flush();
        } else {
            out.write(s + "\n");
            out.flush();
            System.out.println("****** OUT:" + s);
        }
    }

    /** */
    public void startStream() throws IOException {
        fireEvent(this, "connected");
        String s = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>";
        writeLine(s);
        Stream stream = new Stream();
        stream.setTo(server);
        stream.setXmlns("jabber:client");
        stream.setXmlnsStream("http://etherx.jabber.org/streams");
        writeLine(stream.toString());
    }

    /** */
    private String streamId = "";

    /** */
    private String userName = "";

    /** */
    String password = "";

    /** */
    private boolean usePlainPasswd = false;

    /** */
    private boolean useSSL = false;

    /** */
    private String resource = "handheld";

    /**
     * @return Returns the resource.
     */
    public String getResource() {
        return resource;
    }

    /**
     * @return Returns the userName.
     */
    public String getUserName() {
        return userName;
    }

    /** */
    private String getJid() {
        return userName + "@" + this.server + "/" + resource;
    }

    /** */
    private void parse(Xmpp xmpp) throws IOException {
        switch(state) {
            case INITSTATE:
                if (xmpp instanceof Stream) {
                    System.err.println("Stream tag detected.");
                    System.err.println("Checking if server supports SASL");
                    if (((Stream) xmpp).getSasl() != null) {
                        System.err.println("Server supports SASL");
                        state = SASL;
                    } else {
                        System.err.println("Server doesn't support SASL");
                        state = NOTSASL;
                    }
                    System.err.println("Extracting stream id.");
                    streamId = ((Stream) xmpp).getId();
                    parse(xmpp);
                }
                break;
            case SASL:
                if (xmpp instanceof Sasl) {
                    System.err.println("SASL mechanisms arrived.");
                }
                break;
            case NOTSASL:
                System.err.println("*** Querying auth");
                if (newA) {
                    state = NEWA;
                    System.err.println("Querying new account.");
                    writeLine(Iq.getQueryRegistrationString(server));
                } else {
                    state = QUERIEDAUTH;
                    queryAuth();
                }
                break;
            case QUERIEDAUTH:
                System.err.println("*** Reply received:" + xmpp);
                if (xmpp instanceof Iq) {
                    parseAuthRequestReply((Iq) xmpp);
                }
                break;
        }
        switch(state) {
            case QUERIEDAUTH:
                System.err.println("*** Reply received:" + xmpp);
                if (xmpp instanceof Iq) {
                    parseAuthRequestReply((Iq) xmpp);
                }
                break;
            case SENTLOGINDATA:
                if (xmpp instanceof Iq) {
                    if (((Iq) xmpp).getChild() == null) {
                        requestRoster();
                        state = LOGGEDIN;
                        sendPresence();
                        loadPrivateParameters();
                        System.err.println("Positive authentification.");
                    } else {
                        System.err.println("Negative authentification.");
                    }
                }
                break;
            case LOGGEDIN:
                xmpp.handle(this);
                break;
            case NEWA:
                xmpp.handle(this);
                break;
        }
    }

    /** */
    private void loadPrivateParameters() {
    }

    /** */
    private void sendPresence(String to) throws IOException {
        Presence presence = new Presence();
        presence.setTo(to);
        writeLine(presence.toString());
    }

    /** */
    private void parseAuthRequestReply(Iq iq) throws IOException {
        System.err.println("Parsing Auth Request Reply");
        if (iq.getType().equals("result")) {
            Xmpp query = iq.getChild();
            if (query instanceof Query) {
                String p = ((Query) query).getPassword();
                if (p != null) {
                    System.err.println("using plain password for simplicities sake.");
                    state = SENTLOGINDATA;
                    System.err.println("Sending login data.");
                    if (usePlainPasswd) {
                        writeLine(Iq.getLoginWithPlainPasswordString(streamId, userName, resource, password));
                    } else {
                        writeLine(Iq.getLoginWithDigestPasswordString(streamId, userName, resource, password));
                    }
                }
            }
        } else if (iq.getType().equals("error")) {
            System.err.println("ERROR During auth: " + iq);
            String error = iq.getError();
            if (error != null) {
                System.err.println(error);
            }
        }
    }

    /** */
    private void queryAuth() throws IOException {
        Iq iq = new Iq();
        iq.setId(streamId);
        iq.setType("get");
        Query query = new Query();
        query.setXmlns("jabber:iq:auth");
        query.setUserName(userName);
        iq.setChild(query);
        System.err.println("Querying auth.");
        writeLine(iq.toString());
    }

    /** */
    private void sendChat(String to, String text) {
    }

    /** */
    String exN(String f) {
        int i = f.indexOf("/");
        if (!(i < 0)) {
            f = f.substring(i + 1);
        }
        return f;
    }

    /** */
    String exCN(String f) {
        int i = f.indexOf("/");
        if (i != -1) {
            f = f.substring(0, i);
        }
        return f;
    }

    /** */
    List<Chat> groupChats = new ArrayList<Chat>();

    /** */
    List<Chat> privateChats = new ArrayList<Chat>();

    /** */
    Chat getChannelFor(String jid) {
        String channel = exCN(jid);
        for (int i = 0; i < groupChats.size(); i++) {
            Chat gc = groupChats.get(i);
            if (gc.getName().equals(channel)) {
                return gc;
            }
        }
        throw new NoSuchElementException(jid);
    }

    /** */
    Chat getChat(String name) {
        System.err.println("###### displaying in groupchat");
        Chat p = null;
        for (int i = 0; i < groupChats.size(); i++) {
            Chat gc = groupChats.get(i);
            if (gc.getName().equals(name)) {
                p = gc;
                break;
            }
        }
        if (p == null) {
            System.err.println("Creating new GCP");
            p = new Chat(name, "groupchat");
            System.err.println("GCP instantiated");
            groupChats.add(p);
            System.out.println("##### GCP created.");
            fireEvent(this, "displayChat", "name");
        }
        return p;
    }

    /** */
    Chat getPrivateChat(String name, boolean priv, String thread) {
        System.err.println("###### displaying in privatechat");
        Chat p = null;
        for (int i = 0; i < privateChats.size(); i++) {
            Chat gc = privateChats.get(i);
            if (gc.getName().equals(name)) {
                p = gc;
                break;
            }
        }
        if (p == null) {
            System.err.println("Creating new PCP");
            if (priv) {
                p = new Chat(name, "chat");
            } else {
                p = new Chat(name, "groupChat");
            }
            System.err.println("PCP instantiated");
            privateChats.add(p);
            System.out.println("##### GCP created.");
            fireEvent(this, "displayPrivateChat", "name");
        }
        return p;
    }

    /** */
    boolean isChat(String jid) {
        for (int i = 0; i < groupChats.size(); i++) {
            Chat pc = groupChats.get(i);
            if (pc.getName().equals(jid) & pc.getType().equals("groupChat")) {
                return true;
            }
        }
        return false;
    }

    /** */
    boolean isPrivateChat(String jid) {
        for (int i = 0; i < privateChats.size(); i++) {
            Chat pc = privateChats.get(i);
            if (pc.getName().equals(jid) & pc.getType().equals("chat")) {
                return true;
            }
        }
        return false;
    }

    /** */
    private List<Contact> contacts = new ArrayList<Contact>();

    /** */
    private List<Contact> getContactsFor(String jid) {
        List<Contact> v1 = new ArrayList<Contact>();
        Contact c2;
        for (int i = 0; i < contacts.size(); i++) {
            c2 = contacts.get(i);
            if (c2.getJid().startsWith(jid)) {
                v1.add(c2);
            }
        }
        return v1;
    }

    /** */
    List<Message> messageList = new ArrayList<Message>();

    /** */
    private void sendPresence() throws IOException {
        Presence presence = new Presence();
        writeLine(presence.toString());
    }

    /** */
    private void requestRoster() throws IOException {
        Iq iq = new Iq();
        iq.setId("roster");
        iq.setType("get");
        Query query = new Query();
        query.setXmlns("jabber:iq:roster");
        iq.setChild(query);
        writeLine(iq.toString());
    }

    /** */
    private void sendPresence(String to, String status, String show) throws IOException {
        String result = null;
        Presence presence = new Presence();
        presence.setTo(to);
        presence.setStatus(status);
        presence.setShow(show);
        result = presence.toString();
        if (to.equals("")) {
            Presence presence2 = new Presence();
            presence.setStatus(status);
            presence.setShow(show);
            result += presence2;
        }
        writeLine(result);
    }

    /** */
    private void acceptS(String s) throws IOException {
        Presence presence = new Presence();
        presence.setTo(s);
        presence.setType("subscribed");
        writeLine(presence.toString());
    }

    /** */
    private GenericListener defaultListener = new GenericListener() {

        public void eventHappened(GenericEvent ev) {
            String name = ev.getName();
            if ("updateContact".equals(name)) {
                Contact c = (Contact) ev.getArguments()[0];
                List<Contact> v1 = getContactsFor(c.getJid());
                for (int i = 0; i < v1.size(); i++) {
                    Contact c1 = v1.get(i);
                    c1.setName(c.getName());
                    c1.setFoto(c.getFoto());
                }
            }
        }
    };

    /** */
    private GenericSupport gs = new GenericSupport();

    /** GenericListener ��ǉ����܂��D */
    public void addGenericListener(GenericListener l) {
        gs.addGenericListener(l);
    }

    /** GenericListener ���폜���܂��D */
    public void removeGenericListener(GenericListener l) {
        gs.removeGenericListener(l);
    }

    /** �ėp�C�x���g�𔭍s���܂��D */
    public void fireEvent(Object source, String name) {
        gs.fireEventHappened(new GenericEvent(source, name));
    }

    /** �ėp�C�x���g�𔭍s���܂��D */
    public void fireEvent(Object source, String name, Object arg) {
        gs.fireEventHappened(new GenericEvent(source, name, arg));
    }

    /** */
    private BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

    /** */
    @SuppressWarnings("static-access")
    public JabberConnection() throws IOException {
        Options options = new Options();
        options.addOption(OptionBuilder.withArgName("resource").hasArg().withDescription("resource").create("r"));
        options.addOption(OptionBuilder.withArgName("password").hasArg().withDescription("password").create("p"));
        options.addOption(OptionBuilder.withArgName("username").hasArg().withDescription("username").create("u"));
        options.addOption(OptionBuilder.withArgName("use ssl").hasArg().withDescription("use ssl").create("c"));
        options.addOption(OptionBuilder.withArgName("digest password").hasArg().withDescription("digest password").create("d"));
        options.addOption(OptionBuilder.withArgName("jabber id to").hasArg().withDescription("to").create("to"));
        options.addOption(OptionBuilder.withArgName("message").hasArg().withDescription("message").create("m"));
        options.addOption(OptionBuilder.withArgName("server").hasArg().withDescription("server").create("s"));
        options.addOption(OptionBuilder.withArgName("url").hasArg().withDescription("url").create("url"));
        CommandLineParser parser = new BasicParser();
        String url = null;
        String to = null;
        String message = null;
        boolean connected = false;
        while (true) {
            try {
                System.out.print(">");
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                if (line.length() == 0) {
                    HelpFormatter formatter = new HelpFormatter();
                    formatter.printHelp("jabber", options, true);
                    continue;
                }
                String[] args = StringUtil.splitCommandLine(line);
                CommandLine cl = parser.parse(options, args);
                if (cl.hasOption("r")) {
                    this.resource = cl.getOptionValue("r");
                    Debug.println("resource: " + resource);
                }
                if (cl.hasOption("p")) {
                    this.password = cl.getOptionValue("p");
                    Debug.println("password: " + password);
                }
                if (cl.hasOption("u")) {
                    this.userName = cl.getOptionValue("u");
                    Debug.println("userName: " + userName);
                }
                if (cl.hasOption("c")) {
                    String value = cl.getOptionValue("c");
                    this.useSSL = new Boolean(value).booleanValue();
                    Debug.println("useSSL: " + useSSL);
                }
                if (cl.hasOption("d")) {
                    String value = cl.getOptionValue("d");
                    this.usePlainPasswd = new Boolean(value).booleanValue();
                    Debug.println("usePlainPasswd: " + usePlainPasswd);
                }
                if (cl.hasOption("to")) {
                    to = cl.getOptionValue("to");
                }
                if (cl.hasOption("url")) {
                    url = cl.getOptionValue("url");
                }
                if (cl.hasOption("m")) {
                    message = cl.getOptionValue("m");
                }
                if (cl.hasOption("s")) {
                    String sa = cl.getOptionValue("s");
                    StringTokenizer st = new StringTokenizer(sa, ":");
                    this.server = st.nextToken();
                    this.port = Integer.parseInt(st.nextToken());
                    Debug.println("server: " + this.server + ":" + this.port);
                }
                String[] commands = cl.getArgs();
                for (int i = 0; i < commands.length; i++) {
                    if ("connect".equals(commands[i])) {
                        if (server == null || port == 0) {
                            throw new IllegalStateException("not connected");
                        }
                        Thread thread = new Thread(this);
                        thread.start();
                        connected = true;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }
    }

    /** */
    public static void main(String[] args) throws IOException {
        new JabberConnection();
    }
}
