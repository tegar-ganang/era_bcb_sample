import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.text.ChoiceFormat;
import java.text.Collator;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.*;
import ar.com.jkohen.applet.SimpleAppletStub;
import ar.com.jkohen.awt.ChatPanel;
import ar.com.jkohen.awt.ImageCanvas;
import ar.com.jkohen.awt.RFC1459TextField;
import ar.com.jkohen.awt.event.ChatPanelEvent;
import ar.com.jkohen.awt.event.ChatPanelListener;
import ar.com.jkohen.irc.*;
import ar.com.jkohen.net.*;
import ar.com.jkohen.util.CollatedHashtable;
import ar.com.jkohen.util.ConfigurationProperties;
import ar.com.jkohen.util.Resource;

/**
 * This is <b>Eteria IRC Client</b>'s core class. It contains the main
 * control loop, some high level communication methods and topmost-GUI
 * code. In other words, it's a mess.
 *
 * @author <a href="mailto:jkohen@users.sourceforge.net">Javier Kohen</a>
 */
public class EIRC extends Applet implements ClientProcess, Observer, ActionListener, ChatPanelListener, WindowListener {

    /**
     * Program's name.
     * Used as part of "About" info.
     */
    public static final String PACKAGE = "Eteria IRC Client";

    /**
     * Program's release version.
     * Used as part of "About" info.
     * Should be either a "YYYYMMDD" date or a "X.Y.Z" version number.
     */
    public static final String VERSION = "1.0.3";

    /**
     * Program's release extra version.
     * Used as part of "About" info.
     * Should be a String describing in-site's modifications.
     */
    public static final String VERSION_EXTRA = "";

    /**
     * Author's name.
     * Used as part of "About" info.
     */
    public static final String AUTHOR = "Javier Kohen";

    private static Hashtable commands;

    private static ResourceBundle user_commands;

    private Collator collator;

    private Vector channels;

    private CollatedHashtable channel_windows;

    private CollatedHashtable privates;

    private Vector ignore_list;

    private Vector list_reply;

    private boolean missing_default_channel_in_list;

    private Locale locale;

    private ConfigurationProperties properties;

    private ResourceBundle lang;

    private Color mainbg = SystemColor.window;

    private Color mainfg = SystemColor.windowText;

    private Color textbg = SystemColor.text;

    private Color textfg = SystemColor.textText;

    private Color selbg = SystemColor.textHighlight;

    private Color selfg = SystemColor.textHighlightText;

    private TextField nick_entry;

    private ControlPanel control_panel;

    private ChatPanelContainer chat_panel;

    private StatusWindow status;

    private Frame spawned_frame;

    private String current_nick;

    private String new_nick;

    private ServerProcess server;

    private boolean special_services;

    private boolean debug_traffic;

    private String services_password;

    private String password;

    private String quit_message;

    private boolean request_motd;

    private boolean see_everything_from_server;

    private boolean see_join;

    private boolean on_dcc_notify_peer;

    private String service_bots;

    private boolean hideip;

    private boolean focus_opening_privates;

    private String default_channel;

    private String default_keys;

    private boolean connected;

    private boolean logged_in;

    private boolean quit_sent;

    private ChannelTree channel_tree;

    private ChangeFont change_font;

    private NickServCommander nickserv_commander;

    private Configurator configurator;

    private NewChannel new_channel;

    static {
        commands = new Hashtable(50, 1);
        commands.put("ping", new Integer(-1));
        commands.put("nick", new Integer(-2));
        commands.put("join", new Integer(-3));
        commands.put("mode", new Integer(-4));
        commands.put("part", new Integer(-5));
        commands.put("quit", new Integer(-6));
        commands.put("kick", new Integer(-7));
        commands.put("topic", new Integer(-8));
        commands.put("privmsg", new Integer(-9));
        commands.put("notice", new Integer(-10));
        commands.put("error", new Integer(-11));
        commands.put("301", new Integer(301));
        commands.put("305", new Integer(305));
        commands.put("306", new Integer(306));
        commands.put("307", new Integer(307));
        commands.put("310", new Integer(310));
        commands.put("311", new Integer(311));
        commands.put("312", new Integer(312));
        commands.put("313", new Integer(313));
        commands.put("317", new Integer(317));
        commands.put("318", new Integer(318));
        commands.put("319", new Integer(319));
        commands.put("320", new Integer(320));
        commands.put("321", new Integer(321));
        commands.put("322", new Integer(322));
        commands.put("323", new Integer(323));
        commands.put("324", new Integer(324));
        commands.put("328", new Integer(328));
        commands.put("329", new Integer(329));
        commands.put("331", new Integer(331));
        commands.put("332", new Integer(332));
        commands.put("333", new Integer(333));
        commands.put("335", new Integer(335));
        commands.put("353", new Integer(353));
        commands.put("366", new Integer(366));
        commands.put("372", new Integer(372));
        commands.put("375", new Integer(375));
        commands.put("376", new Integer(376));
        commands.put("401", new Integer(401));
        commands.put("421", new Integer(421));
        commands.put("422", new Integer(422));
        commands.put("432", new Integer(432));
        commands.put("433", new Integer(433));
        commands.put("438", new Integer(438));
        commands.put("464", new Integer(464));
        commands.put("471", new Integer(471));
        commands.put("473", new Integer(473));
        commands.put("474", new Integer(474));
        commands.put("475", new Integer(475));
        commands.put("482", new Integer(482));
        user_commands = ResourceBundle.getBundle("Commands");
    }

    /**
     * Execution starts here when the program is run out of Applet context.
     * This method supplies a simple substitute for the Applet viewer.
     *
     * @param args a <code>String[]</code> containing command-line arguments.
     */
    public static void main(String[] args) {
        EIRC eirc = new EIRC();
        eirc.setStub(new SimpleAppletStub(args));
        Frame f = new Frame("Eteria IRC Client");
        f.add(eirc);
        f.addWindowListener(eirc);
        String width, height;
        if (null != (width = eirc.getParameter("width")) && null != (height = eirc.getParameter("height"))) {
            f.setSize(Integer.parseInt(width), Integer.parseInt(height));
        } else {
            f.setSize(620, 400);
        }
        f.show();
        eirc.init();
        eirc.start();
    }

    public void init() {
        this.collator = RFC1459.getCollator();
        this.channels = new Vector();
        this.channel_windows = new CollatedHashtable(collator);
        this.privates = new CollatedHashtable(collator);
        this.ignore_list = new Vector();
        this.locale = Locale.getDefault();
        String locale_params[] = new String[2];
        if (null != (locale_params[0] = getParameter("language")) && null != (locale_params[1] = getParameter("country"))) {
            this.locale = new Locale(locale_params[0], locale_params[1]);
        }
        Properties default_props = new Properties();
        try {
            default_props.load(getClass().getResourceAsStream("configuration.properties"));
            this.properties = new ConfigurationProperties(default_props);
        } catch (IOException ex) {
            System.err.println(ex);
            this.properties = new ConfigurationProperties();
        }
        properties.addObserver(this);
        update(properties, null);
        this.lang = ResourceBundle.getBundle("eirc", locale);
        this.default_channel = getParameter("channel");
        this.default_keys = getParameter("channel_keys");
        String t;
        if (null != (t = getParameter("mainbg"))) {
            this.mainbg = Color.decode(t);
        }
        if (null != (t = getParameter("mainfg"))) {
            this.mainfg = Color.decode(t);
        }
        if (null != (t = getParameter("textbg"))) {
            this.textbg = Color.decode(t);
        }
        if (null != (t = getParameter("textfg"))) {
            this.textfg = Color.decode(t);
        }
        if (null != (t = getParameter("selbg"))) {
            this.selbg = Color.decode(t);
        }
        if (null != (t = getParameter("selfg"))) {
            this.selfg = Color.decode(t);
        }
        if (null == (t = getParameter("servPassword"))) {
            t = "";
        }
        properties.setString("services_password", t);
        if (null == (t = getParameter("servEmail"))) {
            t = "";
        }
        properties.setString("services_email", t);
        com.ms.security.PolicyEngine.assertPermission(com.ms.security.PermissionID.UI);
        if (null != (t = getParameter("spawn_frame")) && !t.equals("0")) {
            this.spawned_frame = new Frame(makeWindowTitle("Eteria IRC Client"));
            initGUI(spawned_frame);
            int width = 620;
            int height = 400;
            try {
                width = Integer.parseInt(getParameter("frame_width"));
                height = Integer.parseInt(getParameter("frame_height"));
            } catch (Exception ex) {
            }
            spawned_frame.addWindowListener(new WindowAdapter() {

                public void windowClosing(WindowEvent e) {
                    stop();
                }
            });
            spawned_frame.setSize(width, height);
            spawned_frame.show();
        } else {
            initGUI(this);
            validate();
        }
        setTextBackground(textbg);
        setTextForeground(textfg);
        nick_entry.addActionListener(this);
        properties.addObserver(control_panel);
        control_panel.update(properties, null);
        if (null != (t = getParameter("nickname"))) {
            char[] str = t.toCharArray();
            for (int i = 0; i < str.length; i++) {
                if ('?' == str[i]) {
                    str[i] = Character.forDigit((int) Math.floor(Math.random() * 10.0), 10);
                }
            }
            nick_entry.setText(String.valueOf(str));
        }
        if (null != (t = getParameter("password"))) {
            properties.setString("password", t);
        }
    }

    public void start() {
        if (null != spawned_frame) {
            spawned_frame.show();
        }
        String t;
        if (null != (t = getParameter("login")) && !t.equals("0")) {
            if (null == (t = getParameter("nickname")) || 0 == t.length()) {
                status.printError(lang.getString("eirc.s26"));
            } else {
                connect();
            }
        }
    }

    public void stop() {
        disconnect();
        for (Enumeration e = privates.elements(); e.hasMoreElements(); ) {
            PrivateWindow w = (PrivateWindow) e.nextElement();
            w.dispose();
        }
        privates.clear();
        if (null != spawned_frame) {
            spawned_frame.dispose();
        }
    }

    public void destroy() {
        removeAll();
    }

    private void initGUI(Container cont) {
        GridBagLayout gb = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();
        cont.setLayout(gb);
        Label l = new Label(lang.getString("eirc.enter_nick"), Label.RIGHT);
        gbc.anchor = GridBagConstraints.EAST;
        gb.setConstraints(l, gbc);
        cont.add(l);
        gbc.anchor = GridBagConstraints.CENTER;
        this.nick_entry = new RFC1459TextField(9);
        gbc.anchor = GridBagConstraints.WEST;
        gb.setConstraints(nick_entry, gbc);
        cont.add(nick_entry);
        gbc.anchor = GridBagConstraints.CENTER;
        this.control_panel = new ControlPanel(this, locale);
        gbc.anchor = GridBagConstraints.EAST;
        gb.setConstraints(control_panel, gbc);
        cont.add(control_panel);
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.gridy = 1;
        this.chat_panel = new ChatPanelContainer(this, locale);
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.BOTH;
        gb.setConstraints(chat_panel, gbc);
        cont.add(chat_panel);
        String status_tag = lang.getString("eirc.status");
        this.status = new StatusWindow(this, status_tag, locale);
        status.setBackground(mainbg);
        status.setForeground(mainfg);
        status.setTextBackground(textbg);
        status.setTextForeground(textfg);
        status.requestFocus();
        chat_panel.add(status, status_tag, true);
        chat_panel.show(status_tag);
        cont.setBackground(mainbg);
        cont.setForeground(mainfg);
    }

    public String getAppletInfo() {
        return (PACKAGE + " " + VERSION.concat(VERSION_EXTRA) + ", an RFC 1459 compliant client program written in Java.\n" + "Copyright 2005 Javier Kohen <jkohen at users.sourceforge.net>");
    }

    public String[][] getParameterInfo() {
        return param_info;
    }

    private String getServerName() {
        String server_name = getParameter("server");
        if (null == server_name) {
            server_name = getDocumentBase().getHost();
            if (0 == server_name.length()) {
                server_name = "localhost";
            }
        }
        return server_name;
    }

    private int getDefaultPort() {
        int port = 6667;
        try {
            String portStr = getParameter("port");
            if (null != portStr) port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
        }
        return port;
    }

    void connect() {
        connect(getServerName(), getDefaultPort());
    }

    synchronized void connect(final String server_name, final int port) {
        if (0 == nick_entry.getText().length()) {
            status.printError(lang.getString("eirc.s1"));
            return;
        }
        try {
            netscape.security.PrivilegeManager.enablePrivilege("UniversalConnect");
            com.ms.security.PolicyEngine.checkPermission(com.ms.security.PermissionID.NETIO);
        } catch (Exception ex) {
        }
        com.ms.security.PolicyEngine.assertPermission(com.ms.security.PermissionID.NETIO);
        if (connected) {
            disconnect();
        }
        this.quit_sent = false;
        InetAddress server_addr = null;
        try {
            InetAddress[] addresses = InetAddress.getAllByName(server_name);
            server_addr = addresses[(int) Math.floor(Math.random() * addresses.length)];
        } catch (IOException ex) {
        } catch (SecurityException ex) {
        }
        if (null == server_addr) {
            Object a[] = { server_name };
            String ptn = lang.getString("eirc.s2");
            status.printError(MessageFormat.format(ptn, a));
            return;
        }
        Socket s = null;
        try {
            s = new Socket(server_addr, port);
        } catch (IOException e) {
        } catch (SecurityException e) {
        }
        if (null == s) {
            Object a[] = { server_addr.getHostName(), new Integer(port) };
            String ptn = lang.getString("eirc.s3");
            status.printError(MessageFormat.format(ptn, a));
            return;
        }
        try {
            ServerThread st = new ServerThread(this, s);
            this.server = st;
            st.start();
        } catch (IOException e) {
            Object a[] = { server_addr.getHostName(), new Integer(port) };
            String ptn = lang.getString("eirc.s3");
            status.printError(MessageFormat.format(ptn, a));
            return;
        }
        this.connected = true;
        login();
        control_panel.setConnected(true);
    }

    private synchronized void login() {
        if (!connected) {
            return;
        }
        if (password.length() > 0) {
            String p[] = { password };
            sendMessage("pass", p);
        }
        {
            this.new_nick = nick_entry.getText();
            String p[] = { new_nick };
            sendMessage("nick", p);
        }
        String t;
        String username = "eirc";
        String realname = "";
        if (null != (t = getParameter("username")) && t.length() > 0) {
            username = t;
        }
        if (null != (t = getParameter("realname"))) {
            realname = t;
        }
        {
            String p[] = { username, "0", "0", realname };
            sendMessage("user", p);
        }
    }

    public void disconnect(ServerProcess sp) {
        synchronized (this) {
            if (server != sp) {
                return;
            }
        }
        disconnect();
    }

    public synchronized void disconnect() {
        if (!connected) {
            return;
        }
        control_panel.setConnected(false);
        logout();
        Socket s = server.getSocket();
        server.disconnect();
        try {
            s.close();
        } catch (IOException e) {
        }
        this.connected = false;
        if (null != channel_tree) {
            channel_tree.dispose();
        }
        for (Enumeration e = channel_windows.elements(); e.hasMoreElements(); ) {
            ChannelWindow w = (ChannelWindow) e.nextElement();
            w.dispose();
        }
        channel_windows.clear();
        channels.removeAllElements();
        String status_tag = lang.getString("eirc.status");
        chat_panel.show(status_tag);
        Object[] a = { getServerName() };
        String ptn = lang.getString("eirc.disconnected");
        status.printWarning(MessageFormat.format(ptn, a));
    }

    private synchronized void logout() {
        if (!logged_in) {
            return;
        }
        if (!quit_sent) {
            String p[] = { quit_message };
            sendMessage("quit", p);
        }
        this.logged_in = false;
    }

    private synchronized void pos_login() {
        if (!connected) {
            return;
        }
        this.current_nick = new_nick;
        status.requestFocus();
        if (request_motd) {
            String p[] = {};
            sendMessage("motd", p);
        }
        if (null != default_channel && 0 != default_channel.length()) {
            if (null != default_keys && 0 != default_keys.length()) {
                String p[] = { default_channel, default_keys };
                sendMessage("join", p);
            } else {
                String p[] = { default_channel };
                sendMessage("join", p);
            }
        }
        if (0 != services_password.length()) {
            String p[] = { "nickserv", "identify " + services_password };
            sendMessage("privmsg", p);
        }
    }

    public void processMessage(ServerProcess sp, Message m) {
        String command_key = m.getCommand().toLowerCase();
        String prefix = m.getPrefix();
        String params[] = m.getParameters();
        Integer t_cmd = (Integer) commands.get(command_key);
        {
            int endOff = prefix.indexOf('!');
            if (-1 != endOff) {
                prefix = prefix.substring(0, endOff);
            }
        }
        if (debug_traffic || null == t_cmd) {
            String t = m.toString();
            t = t.substring(0, t.length() - 2);
            if (debug_traffic) {
                System.out.println("< ".concat(t));
            }
            if (null == t_cmd) {
                if (see_everything_from_server) {
                    try {
                        Integer.parseInt(command_key);
                        StringBuffer buf = new StringBuffer(params[1]);
                        for (int i = 2; i < params.length; i++) {
                            buf.append(' ');
                            buf.append(params[i]);
                        }
                        status.printUnmangled(buf.toString());
                    } catch (NumberFormatException e) {
                        status.printUnmangled(t);
                    }
                }
                return;
            }
        }
        int command = t_cmd.intValue();
        boolean is_prefix_ignored = ignore_list.contains(prefix);
        switch(command) {
            case -1:
                {
                    String p[] = { params[0] };
                    sendMessage("pong", p);
                    break;
                }
            case -2:
                {
                    if (prefix.equals(current_nick)) {
                        current_nick = params[0];
                        nick_entry.setText(current_nick);
                        Object a[] = { current_nick };
                        String ptn = lang.getString("eirc.s4");
                        getCurrentPanel().printInfo(MessageFormat.format(ptn, a));
                    } else {
                        Object a[] = { prefix, params[0] };
                        String ptn = lang.getString("eirc.s5");
                        String msg_text = MessageFormat.format(ptn, a);
                        OutputWindow[] ow = getUserPanels(prefix);
                        for (int i = 0; i < ow.length; i++) {
                            ow[i].printInfo(msg_text);
                        }
                    }
                    String new_nick = params[0];
                    if (privates.containsKey(prefix)) {
                        if (privates.containsKey(new_nick)) {
                            ((PrivateWindow) privates.get(new_nick)).dispose();
                        }
                        PrivateWindow pw = (PrivateWindow) privates.get(prefix);
                        pw.setUser(new_nick);
                        chat_panel.rename(prefix, new_nick);
                        privates.remove(prefix);
                        privates.put(new_nick, pw);
                    }
                    for (Enumeration e = channels.elements(); e.hasMoreElements(); ) {
                        Channel channel = (Channel) e.nextElement();
                        if (channel.contains(prefix)) {
                            channel.rename(prefix, new_nick);
                        }
                    }
                    break;
                }
            case -3:
                {
                    if (current_nick.equals(prefix)) {
                        openChannel(params[0]);
                    } else {
                        Channel channel = getChannel(params[0]);
                        channel.add(prefix);
                        if (see_join) {
                            ChannelWindow cw = getChannelWindow(params[0]);
                            Object a[] = { prefix };
                            String ptn = lang.getString("eirc.s6");
                            cw.printInfo(MessageFormat.format(ptn, a));
                        }
                    }
                    break;
                }
            case -4:
                {
                    if (!Channel.isChannel(params[0])) {
                        break;
                    }
                    String[] modes_params = new String[params.length - 2];
                    for (int i = 0; i < modes_params.length; i++) {
                        modes_params[i] = params[i + 2];
                    }
                    Channel channel = getChannel(params[0]);
                    channel.setModes(params[1], modes_params);
                    ChannelWindow cw = getChannelWindow(params[0]);
                    boolean sign = false;
                    char modes[] = params[1].toCharArray();
                    int j = 0;
                    for (int i = 0; i < modes.length; i++) {
                        String msg_tag = null;
                        switch(modes[i]) {
                            case '+':
                                sign = true;
                                break;
                            case '-':
                                sign = false;
                                break;
                            case 'v':
                                msg_tag = sign ? "eirc.voice" : "eirc.unvoice";
                                break;
                            case 'o':
                                msg_tag = sign ? "eirc.op" : "eirc.deop";
                                break;
                            case 'h':
                                msg_tag = sign ? "eirc.hop" : "eirc.dehop";
                                break;
                            case 'b':
                                msg_tag = sign ? "eirc.ban" : "eirc.unban";
                                break;
                            case 'k':
                            case 'l':
                            case 'e':
                            case 'I':
                                j++;
                                break;
                        }
                        if (null != msg_tag) {
                            Object a[] = { prefix, modes_params[j] };
                            String ptn = lang.getString(msg_tag);
                            cw.printInfo(MessageFormat.format(ptn, a));
                            j++;
                        }
                    }
                    break;
                }
            case -5:
                {
                    if (current_nick.equals(prefix)) {
                        channels.removeElement(params[0]);
                        ChannelWindow cw = getChannelWindow(params[0]);
                        cw.dispose();
                    } else {
                        Channel channel = getChannel(params[0]);
                        channel.remove(prefix);
                        if (see_join) {
                            ChannelWindow cw = getChannelWindow(params[0]);
                            Object a[] = { prefix };
                            String ptn = lang.getString("eirc.s8");
                            cw.printInfo(MessageFormat.format(ptn, a));
                        }
                    }
                    break;
                }
            case -6:
                {
                    Object a[] = { prefix, params[0] };
                    String ptn = lang.getString("eirc.s9");
                    String msg_text = MessageFormat.format(ptn, a);
                    OutputWindow[] ow = getUserPanels(prefix);
                    for (int i = 0; i < ow.length; i++) {
                        ow[i].printInfo(msg_text);
                    }
                    for (Enumeration e = channels.elements(); e.hasMoreElements(); ) {
                        Channel channel = (Channel) e.nextElement();
                        channel.remove(prefix);
                    }
                    break;
                }
            case -7:
                {
                    String reason = null != params[2] ? params[2] : "";
                    if (current_nick.equals(params[1])) {
                        channels.removeElement(params[0]);
                        ChannelWindow cw = getChannelWindow(params[0]);
                        cw.dispose();
                        Object a[] = { params[0], prefix, reason };
                        String ptn = lang.getString("eirc.s10");
                        status.printInfo(MessageFormat.format(ptn, a));
                    } else {
                        Channel channel = getChannel(params[0]);
                        channel.remove(params[1]);
                        ChannelWindow cw = getChannelWindow(params[0]);
                        Object a[] = { params[1], prefix, reason };
                        String ptn = lang.getString("eirc.s11");
                        cw.printInfo(MessageFormat.format(ptn, a));
                    }
                    break;
                }
            case -8:
                {
                    Channel channel = getChannel(params[0]);
                    channel.setTopic(MircMessage.filterMircAttributes(params[1]));
                    ChannelWindow cw = getChannelWindow(params[0]);
                    Object a[] = { prefix, params[1] };
                    String ptn = lang.getString("eirc.s36");
                    cw.printInfo(MessageFormat.format(ptn, a));
                    break;
                }
            case -9:
                {
                    if (is_prefix_ignored) {
                        break;
                    }
                    boolean isChannel = Channel.isChannel(params[0]);
                    if (CTCPMessage.isCTCPMessage(params[1])) {
                        CTCPMessage ctcp = new CTCPMessage(params[1]);
                        boolean report_user = true;
                        switch(ctcp.getCommand()) {
                            case CTCPMessage.ACTION:
                                {
                                    report_user = false;
                                    if (!ctcp.hasParameter()) {
                                        break;
                                    }
                                    ChatWindow target = isChannel ? (ChatWindow) getChannelWindow(params[0]) : (ChatWindow) openPrivate(prefix);
                                    target.printAction(ctcp.getParameter(), prefix);
                                    break;
                                }
                            case CTCPMessage.PING:
                                {
                                    String param = ctcp.hasParameter() ? ctcp.getParameter() : "";
                                    CTCPMessage reply = new CTCPMessage("PING", param);
                                    String p[] = { prefix, reply.toString() };
                                    sendMessage("notice", p);
                                    break;
                                }
                            case CTCPMessage.DCC:
                                {
                                    Object a[] = { prefix };
                                    String ptn = lang.getString("eirc.dcc_not_supported");
                                    OutputWindow target = getCurrentPanel();
                                    target.printInfo(MessageFormat.format(ptn, a));
                                    if (on_dcc_notify_peer) {
                                        String p[] = { prefix, lang.getString("eirc.dcc_notify.remote") };
                                        sendMessage("notice", p);
                                        ptn = lang.getString("eirc.dcc_notify.local");
                                        target.printInfo(MessageFormat.format(ptn, a));
                                    }
                                    break;
                                }
                            case CTCPMessage.VERSION:
                                {
                                    CTCPMessage reply = new CTCPMessage("VERSION", PACKAGE.concat("-").concat(VERSION).concat(VERSION_EXTRA).concat(" ").concat(AUTHOR));
                                    String p[] = { prefix, reply.toString() };
                                    sendMessage("notice", p);
                                    break;
                                }
                        }
                        if (report_user) {
                            Object[] a = { ctcp.getCommandString(), prefix };
                            String ptn = lang.getString("eirc.ctcp_received");
                            status.printWarning(MessageFormat.format(ptn, a));
                        }
                    } else {
                        ChatWindow target = isChannel ? (ChatWindow) getChannelWindow(params[0]) : (ChatWindow) openPrivate(prefix);
                        target.printPrivmsg(params[1], prefix);
                    }
                    break;
                }
            case -10:
                {
                    if (is_prefix_ignored) {
                        break;
                    }
                    if (prefix.length() > 0) {
                        int endOff = prefix.indexOf('!');
                        if (-1 != endOff) {
                            prefix = prefix.substring(0, endOff);
                        }
                    }
                    if (Channel.isChannel(params[0])) {
                        getChannelWindow(params[0]).printNotice(params[1], prefix);
                    } else if (special_services && 0 != prefix.length() && -1 != service_bots.indexOf(prefix)) {
                        openPrivate(prefix).printNotice(params[1], prefix);
                    } else if (params[0].equals(current_nick)) {
                        if (!CTCPMessage.isCTCPMessage(params[1])) {
                            getCurrentPanel().printNotice(params[1], prefix);
                        } else {
                            CTCPMessage ctcp = new CTCPMessage(params[1]);
                            switch(ctcp.getCommand()) {
                                case CTCPMessage.PING:
                                    {
                                        double diff = Double.NEGATIVE_INFINITY;
                                        if (ctcp.hasParameter()) {
                                            try {
                                                long launch = Long.parseLong(ctcp.getParameter());
                                                long arrive = (new Date()).getTime();
                                                diff = (arrive - launch) / 1000.0;
                                            } catch (NumberFormatException e) {
                                            }
                                        }
                                        {
                                            Object a[] = { prefix, new Double(diff) };
                                            MessageFormat mf = new MessageFormat(lang.getString("eirc.s12"));
                                            double limits[] = { ChoiceFormat.previousDouble(0.0), 0.0, 1.0, ChoiceFormat.nextDouble(1.0) };
                                            String times[] = { lang.getString("eirc.s12.0"), lang.getString("eirc.s12.1"), lang.getString("eirc.s12.2"), lang.getString("eirc.s12.3") };
                                            mf.setFormat(1, new ChoiceFormat(limits, times));
                                            getCurrentPanel().printInfo(mf.format(a));
                                        }
                                        break;
                                    }
                                default:
                                    {
                                        Object a[] = { ctcp.getCommandString(), ctcp.getParameter() };
                                        String ptn = lang.getString("eirc.ctcp_reply");
                                        getCurrentPanel().printNotice(MessageFormat.format(ptn, a), prefix);
                                        break;
                                    }
                            }
                        }
                    } else {
                        status.printNotice(params[0] + " " + params[1], prefix);
                    }
                    break;
                }
            case -11:
                {
                    status.printError(params[0]);
                    break;
                }
            case 301:
                {
                    Object a[] = { params[1], params[2] };
                    String ptn = lang.getString("eirc.301");
                    getUserPanel(params[1]).printInfo(MessageFormat.format(ptn, a));
                    break;
                }
            case 305:
            case 306:
                {
                    getCurrentPanel().printInfo(lang.getString("eirc." + command));
                    break;
                }
            case 307:
                {
                    Object a[] = { params[1] };
                    String ptn = lang.getString("eirc.s27");
                    getCurrentPanel().printInfo(MessageFormat.format(ptn, a));
                    break;
                }
            case 310:
                {
                    Object a[] = { params[1], params[2] };
                    String ptn = lang.getString("eirc.310");
                    getCurrentPanel().printInfo(MessageFormat.format(ptn, a));
                    break;
                }
            case 311:
                {
                    String t;
                    if (hideip) {
                        params[3] = "xxx";
                    }
                    {
                        Object a[] = { params[1], params[2], params[3] };
                        String ptn = lang.getString("eirc.s13");
                        getCurrentPanel().printInfo(MessageFormat.format(ptn, a), false);
                    }
                    {
                        Object a[] = { params[1], params[5] };
                        String ptn = lang.getString("eirc.s14");
                        getCurrentPanel().printInfo(MessageFormat.format(ptn, a));
                    }
                    break;
                }
            case 312:
                {
                    Object a[] = { params[1], params[2], params[3] };
                    String ptn = lang.getString("eirc.s15");
                    getCurrentPanel().printInfo(MessageFormat.format(ptn, a));
                    break;
                }
            case 313:
                {
                    Object a[] = { params[1] };
                    String ptn = lang.getString("eirc.s16");
                    getCurrentPanel().printInfo(MessageFormat.format(ptn, a));
                    break;
                }
            case 317:
                {
                    Object a[] = { params[1], new Integer(params[2]) };
                    MessageFormat mf = new MessageFormat(lang.getString("eirc.s17"));
                    double limits[] = { 0, 1, 2 };
                    String times[] = { lang.getString("eirc.s17.0"), lang.getString("eirc.s17.1"), lang.getString("eirc.s17.2") };
                    mf.setFormat(1, new ChoiceFormat(limits, times));
                    getCurrentPanel().printInfo(mf.format(a));
                    break;
                }
            case 319:
                {
                    Object a[] = { params[1], params[2] };
                    String ptn = lang.getString("eirc.s18");
                    getCurrentPanel().printInfo(MessageFormat.format(ptn, a));
                    break;
                }
            case 320:
                {
                    Object a[] = { params[1], params[2] };
                    String ptn = lang.getString("eirc.320");
                    getCurrentPanel().printInfo(MessageFormat.format(ptn, a));
                    break;
                }
            case 321:
                {
                    replyListStart();
                    break;
                }
            case 322:
                {
                    if (params[1].equals("*")) {
                        break;
                    }
                    int users = 0;
                    try {
                        users = Integer.parseInt(params[2]);
                    } catch (NumberFormatException ex) {
                    }
                    String topic = MircMessage.filterMircAttributes(params[3]);
                    replyListAdd(params[1], users, topic);
                    break;
                }
            case 323:
                {
                    replyListEnd();
                    break;
                }
            case 324:
                {
                    String[] modes_params = new String[params.length - 3];
                    for (int i = 0; i < modes_params.length; i++) {
                        modes_params[i] = params[i + 3];
                    }
                    Channel channel = getChannel(params[1]);
                    channel.setModes(params[2], modes_params);
                    break;
                }
            case 328:
                {
                    Channel channel = getChannel(params[1]);
                    channel.setUrl(params[1]);
                    ChannelWindow cw = getChannelWindow(params[1]);
                    Object a[] = { params[0], params[2] };
                    String ptn = lang.getString("eirc.328");
                    cw.printInfo(MessageFormat.format(ptn, a));
                    break;
                }
            case 331:
                {
                    Channel channel = getChannel(params[1]);
                    channel.setTopic("");
                    ChannelWindow cw = getChannelWindow(params[1]);
                    Object a[] = { params[1] };
                    String ptn = lang.getString("eirc.s37");
                    cw.printInfo(MessageFormat.format(ptn, a));
                    break;
                }
            case 332:
                {
                    Channel channel = getChannel(params[1]);
                    channel.setTopic(MircMessage.filterMircAttributes(params[2]));
                    ChannelWindow cw = getChannelWindow(params[1]);
                    Object a[] = { params[1], params[2] };
                    String ptn = lang.getString("eirc.s38");
                    cw.printInfo(MessageFormat.format(ptn, a));
                    break;
                }
            case 333:
                {
                    Date topic_date = new Date(Long.parseLong(params[3]) * 1000);
                    OutputWindow cw = getChannelWindow(params[1]);
                    Object a[] = { params[2], topic_date, topic_date };
                    String ptn = lang.getString("eirc.s35");
                    cw.printInfo(MessageFormat.format(ptn, a));
                    break;
                }
            case 335:
                {
                    if (params.length < 3) {
                        break;
                    }
                    Object a[] = { params[1], params[2] };
                    String ptn = lang.getString("eirc.335");
                    getCurrentPanel().printInfo(MessageFormat.format(ptn, a));
                    break;
                }
            case 353:
                {
                    Channel channel = getChannel(params[2]);
                    if (null == channel || channel.contains(getNick())) {
                        break;
                    }
                    StringTokenizer st = new StringTokenizer(params[3], " ");
                    int tokens = st.countTokens();
                    for (int i = 0; i < tokens; i++) {
                        channel.add(st.nextToken());
                    }
                    break;
                }
            case 372:
                {
                    status.printInfo(params[1]);
                    break;
                }
            case 376:
            case 422:
                {
                    if (!logged_in) {
                        this.logged_in = true;
                        if (!new_nick.equals(params[0])) {
                            this.new_nick = params[0];
                            nick_entry.setText(new_nick);
                            Object a[] = { new_nick };
                            String ptn = lang.getString("eirc.s4");
                            getCurrentPanel().printInfo(MessageFormat.format(ptn, a));
                        }
                        pos_login();
                    }
                    break;
                }
            case 421:
                {
                    Object[] a = { params[1] };
                    String ptn = lang.getString("eirc.421");
                    getCurrentPanel().printError(MessageFormat.format(ptn, a));
                    break;
                }
            case 432:
            case 433:
                {
                    getCurrentPanel().printError(lang.getString("eirc." + command));
                    break;
                }
            case 438:
                {
                    getCurrentPanel().printError(params[2]);
                    break;
                }
            case 464:
                {
                    status.printError(params[1]);
                    disconnect();
                    break;
                }
            case 401:
            case 471:
            case 473:
            case 474:
            case 475:
                {
                    Object a[] = { params[1] };
                    String ptn = lang.getString("eirc." + command);
                    status.printError(MessageFormat.format(ptn, a));
                    break;
                }
            case 482:
                {
                    getChannelWindow(params[1]).printError(lang.getString("eirc." + 482));
                    break;
                }
        }
    }

    private boolean replyListInitialized() {
        return (null != list_reply);
    }

    private void replyListStart() {
        this.list_reply = new Vector(20, 10);
        if (null == default_channel || default_channel.length() <= 0) {
            missing_default_channel_in_list = false;
        } else {
            missing_default_channel_in_list = true;
        }
    }

    private void replyListAdd(String tag, int users, String topic) {
        if (!replyListInitialized()) {
            replyListStart();
        }
        if (tag.equalsIgnoreCase(default_channel)) {
            missing_default_channel_in_list = false;
        }
        list_reply.addElement(new ChannelItem(tag, users, topic));
    }

    private void replyListEnd() {
        if (!replyListInitialized()) {
            replyListStart();
        }
        if (missing_default_channel_in_list) {
            list_reply.addElement(new ChannelItem(default_channel, 0, ""));
        }
        if (list_reply.size() > 0) {
            openChannelList();
        } else {
            status.printInfo(lang.getString("eirc.s19"));
        }
        this.list_reply = null;
    }

    public void sendCommand(String text, OutputWindow target) {
        if (text.trim().length() <= 0) {
            throw new IllegalArgumentException("empty command");
        }
        String[] result = null;
        try {
            result = Commands.parseCommand(user_commands, text);
        } catch (MissingResourceException ex) {
            String[] a = new String[0];
            sendMessage(text, a);
            return;
        } catch (IllegalArgumentException ex) {
            String name = new StringTokenizer(text).nextToken();
            Command c = Commands.getCommand(user_commands, name);
            String action = c.getTag();
            int required = c.getRequiredParameters();
            try {
                ResourceBundle help = ResourceBundle.getBundle("Help", locale);
                Object[] a = { help.getString(action.concat(".short").toLowerCase()) };
                String ptn = lang.getString("eirc.s22");
                target.printError(MessageFormat.format(ptn, a));
            } catch (MissingResourceException ex2) {
                MessageFormat mf = new MessageFormat(lang.getString("eirc.bad_invocation"));
                Object[] a = { action, new Integer(required) };
                double[] limits = { 1.0, ChoiceFormat.nextDouble(1.0) };
                String[] fragments = { lang.getString("eirc.bad_invocation.0"), lang.getString("eirc.bad_invocation.1") };
                mf.setFormat(1, new ChoiceFormat(limits, fragments));
                target.printError(mf.format(a));
            }
            return;
        }
        String name = result[0];
        Vector parameters = new Vector(result.length - 1);
        for (int i = 1; i < result.length; i++) {
            parameters.addElement(result[i]);
        }
        invokeCommand(name, parameters, target);
    }

    void invokeCommand(String name, Vector parameters, OutputWindow target) {
        if (name.equalsIgnoreCase("help")) {
            cmd_help(parameters, target);
        } else if (name.equalsIgnoreCase("quote")) {
            cmd_quote(parameters, target);
        } else if (name.equalsIgnoreCase("join")) {
            cmd_join(parameters, target);
        } else if (name.equalsIgnoreCase("part")) {
            cmd_part(parameters, target);
        } else if (name.equalsIgnoreCase("ping")) {
            cmd_ping(parameters, target);
        } else if (name.equalsIgnoreCase("quit")) {
            cmd_quit(parameters, target);
        } else if (name.equalsIgnoreCase("nick")) {
            cmd_nick(parameters, target);
        } else if (name.equalsIgnoreCase("me")) {
            cmd_me(parameters, target);
        } else if (name.equalsIgnoreCase("query")) {
            cmd_query(parameters, target);
        } else if (name.equalsIgnoreCase("ctcp")) {
            cmd_ctcp(parameters, target);
        } else if (name.equalsIgnoreCase("eirc")) {
            cmd_eirc(parameters, target);
        } else if (name.equalsIgnoreCase("kban")) {
            cmd_kban(parameters, target);
        } else if (name.equalsIgnoreCase("ignore")) {
            cmd_ignore(parameters, target);
        } else if (name.equalsIgnoreCase("unignore")) {
            cmd_unignore(parameters, target);
        } else if (name.equalsIgnoreCase("server")) {
            cmd_server(parameters, target);
        } else {
            String[] a = new String[parameters.size()];
            parameters.copyInto(a);
            sendMessage(name, a);
        }
    }

    void cmd_help(Vector params, OutputWindow target) {
        if (0 == params.size()) {
            for (Enumeration e = user_commands.getKeys(); e.hasMoreElements(); ) {
                target.printInfo((String) e.nextElement());
            }
            return;
        }
        String action = (String) params.elementAt(0);
        try {
            action = Commands.getCommand(user_commands, action).getTag();
        } catch (MissingResourceException e) {
        }
        try {
            ResourceBundle help = ResourceBundle.getBundle("Help", locale);
            String help_on = action.toLowerCase();
            target.printInfo(help.getString(help_on.concat(".short")));
            target.printInfo(help.getString(help_on.concat(".long")));
        } catch (MissingResourceException e) {
            Object a[] = { action };
            String ptn = lang.getString("eirc.no_help");
            target.printError(MessageFormat.format(ptn, a));
        }
    }

    void cmd_quote(Vector params, OutputWindow target) {
        String a[] = new String[0];
        sendMessage((String) params.elementAt(0), a);
    }

    void cmd_nick(Vector params, OutputWindow target) {
        String a[] = new String[params.size()];
        params.copyInto(a);
        a[0] = RFC1459.filterString(a[0]);
        sendMessage("nick", a);
    }

    void cmd_me(Vector params, OutputWindow target) {
        if (!(target instanceof ChatWindow)) {
            target.printError(lang.getString("eirc.s23"));
            return;
        }
        String to = ((ChatWindow) target).getPanelTag();
        params.insertElementAt(to, 0);
        String a[] = new String[params.size()];
        params.copyInto(a);
        ((ChatWindow) target).printMyAction(a[1]);
        a[1] = "\001ACTION " + a[1] + "\001";
        sendMessage("privmsg", a);
    }

    void cmd_query(Vector params, OutputWindow target) {
        String a[] = new String[params.size()];
        params.copyInto(a);
        openPrivate(a[0]);
        if (2 == params.size()) {
            sendMessage("privmsg", a);
            openPrivate(a[0]).printMyPrivmsg(a[1]);
        }
    }

    void cmd_join(Vector params, OutputWindow target) {
        String a[] = new String[params.size()];
        params.copyInto(a);
        if (!Channel.isChannel(a[0])) {
            a[0] = '#' + a[0];
        }
        sendMessage("join", a);
    }

    void cmd_part(Vector params, OutputWindow target) {
        if (0 == params.size()) {
            if (!(target instanceof ChannelWindow)) {
                target.printError(lang.getString("eirc.s24"));
                return;
            }
            params.insertElementAt(((ChannelWindow) target).getPanelTag(), 0);
        }
        String a[] = new String[params.size()];
        params.copyInto(a);
        if (!Channel.isChannel(a[0])) {
            a[0] = '#' + a[0];
        }
        sendMessage("part", a);
    }

    void cmd_ping(Vector params, OutputWindow target) {
        params.addElement("\001PING " + (new Date()).getTime() + "\001");
        String a[] = new String[params.size()];
        params.copyInto(a);
        sendMessage("privmsg", a);
    }

    void cmd_quit(Vector params, OutputWindow target) {
        String a[] = new String[1];
        a[0] = 0 == params.size() ? quit_message : (String) params.elementAt(0);
        this.quit_sent = true;
        sendMessage("quit", a);
    }

    void cmd_ctcp(Vector params, OutputWindow target) {
        String a[] = new String[params.size()];
        params.copyInto(a);
        a[1] = "\001" + a[1] + "\001";
        sendMessage("privmsg", a);
    }

    void cmd_eirc(Vector params, OutputWindow target) {
        Object a[] = { PACKAGE, VERSION.concat(VERSION_EXTRA), lang.getString("author"), lang.getString("update") };
        String ptn = lang.getString("info");
        target.printInfo(MessageFormat.format(ptn, a));
    }

    void cmd_kban(Vector params, OutputWindow target) {
        if (1 == params.size()) {
            if (!(target instanceof ChannelWindow)) {
                target.printError(lang.getString("eirc.s24"));
                return;
            }
            params.insertElementAt(((ChannelWindow) target).getPanelTag(), 0);
        }
        String channel = (String) params.elementAt(0);
        if (!Channel.isChannel(channel)) {
            channel = '#' + channel;
        }
        String nick = (String) params.elementAt(1);
        sendCommand("mode " + channel + " +b " + nick, target);
        sendCommand("kick " + channel + " " + nick, target);
    }

    void cmd_ignore(Vector params, OutputWindow target) {
        if (0 == params.size()) {
            if (0 == ignore_list.size()) {
                target.printWarning(lang.getString("eirc.no_ignored_users"));
                return;
            }
            StringBuffer line = new StringBuffer();
            for (Enumeration e = ignore_list.elements(); e.hasMoreElements(); ) {
                line.append((String) e.nextElement()).append(' ');
            }
            String[] a = { line.toString() };
            String ptn = lang.getString("eirc.ignored_users");
            target.printInfo(MessageFormat.format(ptn, a));
            return;
        }
        String nick = (String) params.elementAt(0);
        if (!ignore_list.contains(nick)) {
            ignore_list.addElement(nick);
            Object a[] = { nick };
            String ptn = lang.getString("eirc.ignore");
            target.printInfo(MessageFormat.format(ptn, a));
        }
    }

    void cmd_unignore(Vector params, OutputWindow target) {
        String nick = (String) params.elementAt(0);
        if (ignore_list.removeElement(nick)) {
            Object a[] = { nick };
            String ptn = lang.getString("eirc.unignore");
            target.printInfo(MessageFormat.format(ptn, a));
        }
    }

    void cmd_server(Vector params, OutputWindow target) {
        boolean is_promisc = false;
        try {
            netscape.security.PrivilegeManager.enablePrivilege("UniversalConnect");
            netscape.security.PrivilegeManager.revertPrivilege("UniversalConnect");
            com.ms.security.PolicyEngine.checkPermission(com.ms.security.PermissionID.NETIO);
            is_promisc = true;
        } catch (Exception e) {
        }
        if (!is_promisc) {
            Object[] a = { "SERVER" };
            String ptn = lang.getString("eirc.not_in_applet");
            target.printError(MessageFormat.format(ptn, a));
            return;
        }
        disconnect();
        String server_name = (String) params.elementAt(0);
        int port = getDefaultPort();
        if (params.size() > 1) {
            try {
                String portStr = (String) params.elementAt(1);
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
            }
        }
        connect(server_name, port);
    }

    public void sendMessage(String command, String parameters[]) {
        if (connected) {
            Message m = new MircMessage(command, parameters);
            if (debug_traffic) {
                String t = m.toString();
                t = t.substring(0, t.length() - 2);
                System.err.println("> ".concat(t));
            }
            try {
                server.enqueueMessage(m);
            } catch (IOException e) {
            }
        } else {
        }
    }

    public OutputWindow getCurrentPanel() {
        return (OutputWindow) chat_panel.getVisible();
    }

    public OutputWindow getUserPanel(String user) {
        OutputWindow ow = getPrivate(user);
        return null != ow ? ow : status;
    }

    public OutputWindow[] getUserPanels(String user) {
        Vector v = new Vector();
        for (Enumeration e = channel_windows.elements(); e.hasMoreElements(); ) {
            ChannelWindow cw = (ChannelWindow) e.nextElement();
            Channel c = cw.getChannel();
            if (c.contains(user)) {
                v.addElement(cw);
            }
        }
        OutputWindow pw = getPrivate(user);
        if (null != pw) {
            v.addElement(pw);
        }
        OutputWindow[] a = new OutputWindow[v.size()];
        v.copyInto(a);
        return a;
    }

    public String getNick() {
        return current_nick;
    }

    public Channel getChannel(String tag) {
        ChannelWindow cw = getChannelWindow(tag);
        return null != cw ? cw.getChannel() : null;
    }

    public ChannelWindow getChannelWindow(String tag) {
        return (ChannelWindow) channel_windows.get(tag);
    }

    public Channel openChannel(String tag) {
        Channel channel = getChannel(tag);
        if (null != channel) {
            return channel;
        }
        channel = new Channel(tag);
        channels.addElement(channel);
        String[] p = { tag };
        sendMessage("mode", p);
        ChannelWindow cw = new ChannelWindow(this, channel, locale);
        properties.addObserver(cw);
        cw.update(properties, null);
        cw.setFont(getFont());
        cw.setForeground(mainfg);
        cw.setBackground(mainbg);
        cw.setTextForeground(textfg);
        cw.setTextBackground(textbg);
        cw.setSelectedForeground(selfg);
        cw.setSelectedBackground(selbg);
        channel_windows.put(tag, cw);
        chat_panel.add(cw, tag);
        cw.validate();
        chat_panel.show(tag);
        cw.requestFocus();
        cw.addChatPanelListener(this);
        return channel;
    }

    public PrivateWindow getPrivate(String target) {
        return (PrivateWindow) privates.get(target);
    }

    public PrivateWindow openPrivate(String target) {
        PrivateWindow pw = getPrivate(target);
        if (null != pw) {
            return pw;
        }
        pw = new PrivateWindow(this, target, target, locale);
        properties.addObserver(pw);
        pw.update(properties, null);
        pw.setFont(getFont());
        pw.setForeground(mainfg);
        pw.setBackground(mainbg);
        pw.setTextForeground(textfg);
        pw.setTextBackground(textbg);
        privates.put(target, pw);
        chat_panel.add(pw, target);
        pw.validate();
        if (focus_opening_privates) {
            chat_panel.show(target);
            pw.requestFocus();
        }
        pw.addChatPanelListener(this);
        return pw;
    }

    public void showPanel(String name) {
        chat_panel.show(name);
    }

    public void openChannelList() {
        if (null == channel_tree) {
            this.channel_tree = new ChannelTree(this, locale);
            properties.addObserver(channel_tree);
            channel_tree.update(properties, null);
            channel_tree.setTitle(makeWindowTitle(channel_tree.getTitle()));
            channel_tree.setFont(getFont());
            channel_tree.setForeground(mainfg);
            channel_tree.setBackground(mainbg);
            channel_tree.setSelectedForeground(selfg);
            channel_tree.setSelectedBackground(selbg);
        }
        channel_tree.addWindowListener(new WindowAdapter() {

            public void windowClosed(WindowEvent e) {
                properties.deleteObserver(channel_tree);
                channel_tree = null;
            }
        });
        channel_tree.loadChannels(list_reply);
        channel_tree.pack();
        channel_tree.show();
    }

    public void openNickServ() {
        if (null == nickserv_commander) {
            this.nickserv_commander = new NickServCommander(this, properties, locale);
            properties.addObserver(nickserv_commander);
            nickserv_commander.update(properties, null);
            nickserv_commander.setTitle(makeWindowTitle(nickserv_commander.getTitle()));
            nickserv_commander.setFont(getFont());
            nickserv_commander.setBackground(mainbg);
            nickserv_commander.setForeground(mainfg);
            nickserv_commander.setTextBackground(textbg);
            nickserv_commander.setTextForeground(textfg);
            nickserv_commander.setResizable(false);
        }
        nickserv_commander.pack();
        nickserv_commander.show();
        nickserv_commander.requestFocus();
    }

    public void openConfigurator() {
        if (null == configurator) {
            this.configurator = new Configurator(properties, locale);
            configurator.setTitle(makeWindowTitle(configurator.getTitle()));
            configurator.setFont(getFont());
            configurator.setBackground(mainbg);
            configurator.setForeground(mainfg);
            configurator.setResizable(false);
        }
        configurator.pack();
        configurator.show();
        configurator.requestFocus();
    }

    public void openNewChannel() {
        if (null == new_channel) {
            this.new_channel = new NewChannel(this, locale);
            new_channel.setTitle(makeWindowTitle(new_channel.getTitle()));
            new_channel.setFont(getFont());
            new_channel.setForeground(mainfg);
            new_channel.setBackground(mainbg);
            new_channel.setTextForeground(textfg);
            new_channel.setTextBackground(textbg);
            new_channel.setResizable(false);
        }
        new_channel.pack();
        new_channel.show();
        new_channel.requestFocus();
    }

    String makeWindowTitle(String t) {
        String te;
        if (null != (te = getParameter("titleExtra"))) {
            if (' ' != te.charAt(0)) {
                te = ' ' + te;
            }
            t = t.concat(te);
        }
        return t;
    }

    public void update(Observable o, Object arg) {
        ConfigurationProperties props = (ConfigurationProperties) o;
        if (null == arg || arg.equals("special_services")) {
            this.special_services = props.getBoolean("special_services");
        }
        if (null == arg || arg.equals("debug_traffic")) {
            this.debug_traffic = props.getBoolean("debug_traffic");
        }
        if (null == arg || arg.equals("services_password")) {
            this.services_password = props.getString("services_password");
        }
        if (null == arg || arg.equals("password")) {
            this.password = props.getString("password");
        }
        if (null == arg || arg.equals("quit_message")) {
            this.quit_message = props.getString("quit_message");
        }
        if (null == arg || arg.equals("request_motd")) {
            this.request_motd = props.getBoolean("request_motd");
        }
        if (null == arg || arg.equals("see_everything_from_server")) {
            this.see_everything_from_server = props.getBoolean("see_everything_from_server");
        }
        if (null == arg || arg.equals("see_join")) {
            this.see_join = props.getBoolean("see_join");
        }
        if (null == arg || arg.equals("on_dcc_notify_peer")) {
            this.on_dcc_notify_peer = props.getBoolean("on_dcc_notify_peer");
        }
        if (null == arg || arg.equals("service_bots")) {
            this.service_bots = props.getString("service_bots");
        }
        if (null == arg || arg.equals("hideip")) {
            this.hideip = props.getBoolean("hideip");
        }
        if (null == arg || arg.equals("focus_opening_privates")) {
            this.focus_opening_privates = props.getBoolean("focus_opening_privates");
        }
    }

    public void requestFocus() {
        if (connected) {
            status.requestFocus();
        } else {
            nick_entry.requestFocus();
        }
    }

    public void setBackground(Color c) {
        super.setBackground(c);
        control_panel.setBackground(mainbg);
        chat_panel.setBackground(mainbg);
    }

    public void setForeground(Color c) {
        super.setForeground(c);
        control_panel.setForeground(mainfg);
        chat_panel.setForeground(mainfg);
    }

    public void setTextBackground(Color c) {
        nick_entry.setBackground(c);
    }

    public void setTextForeground(Color c) {
        nick_entry.setForeground(c);
    }

    public void visitURL(URL url) {
        visitURL(url, "_blank");
    }

    public void visitURL(URL url, String target) {
        getAppletContext().showDocument(url, target);
    }

    public void actionPerformed(ActionEvent ev) {
        this.new_nick = nick_entry.getText();
        if (0 == new_nick.length()) {
            return;
        }
        if (!connected) {
            connect();
        } else {
            String p[] = { new_nick };
            sendMessage("nick", p);
        }
    }

    public void chatPanelClosing(ChatPanelEvent ev) {
        ChatPanel source = ev.getChatPanel();
        String name = source.getPanelTag();
        chat_panel.remove(name);
        if (source instanceof PrivateWindow) {
            properties.deleteObserver((PrivateWindow) source);
            privates.remove(name);
        } else if (source instanceof ChannelWindow) {
            properties.deleteObserver((ChannelWindow) source);
            channels.removeElement(getChannel(name));
            channel_windows.remove(name);
        }
    }

    public void windowOpened(WindowEvent e) {
    }

    public void windowClosing(WindowEvent e) {
        stop();
        destroy();
        System.exit(0);
    }

    public void windowClosed(WindowEvent e) {
    }

    public void windowIconified(WindowEvent e) {
    }

    public void windowDeiconified(WindowEvent e) {
    }

    public void windowActivated(WindowEvent e) {
    }

    public void windowDeactivated(WindowEvent e) {
    }

    private static final String[][] param_info = { { "server", "string", "IRC server's address" }, { "port", "0-65535", "IRC server's port" }, { "mainbg", "color", "general background color" }, { "mainfg", "color", "general foreground color" }, { "textbg", "color", "text background color" }, { "textfg", "color", "text foreground color" }, { "selbg", "color", "selected background color" }, { "selfg", "color", "selected foreground color" }, { "channel", "string", "channel to auto-join" }, { "titleExtra", "string", "extra text for windows' titles" }, { "username", "string", "username part of hostmask" }, { "realname", "string", "user's real name" }, { "nickname", "RFC 1459 string", "user's nick name" }, { "password", "string", "user's password to server" }, { "login", "0/1", "whether to try auto-login" }, { "language", "two-letter ISO-639 Language Code", "user's language" }, { "country", "two-letter ISO-3166 Country Code", "user's country" }, { "spawn_frame", "0/1", "spawn separate frame" }, { "frame_width", "int", "frame width" }, { "frame_height", "int", "frame height" } };
}
