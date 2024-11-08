package com.peterhix.net.client;

import static com.peterhi.util.LogMacros.*;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Logger;
import javax.swing.JColorChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import org.flexdock.view.Titlebar;
import com.peterhi.exceptions.PeterHiException;
import com.peterhi.exceptions.Results;
import com.peterhi.media.voice.Decoder;
import com.peterhi.media.voice.Encoder;
import com.peterhi.media.voice.Manager;
import com.peterhi.media.voice.Mic;
import com.peterhi.media.voice.Speaker;
import com.peterhi.net.conv.impl.*;
import com.peterhi.net.packet.Packet;
import com.peterhi.net.conv.Converter;
import com.peterhi.net.conv.Convertible;
import com.peterhi.net.conv.Processor;
import com.peterhi.util.AsyncCallbackListener;
import com.peterhix.net.client.event.SGSClientListener;
import com.sun.sgs.client.simple.SimpleClient;

public class Client {

    private static final Logger logger = getLogger("cli");

    /**
	 * Prefix of a drawn shape or element
	 */
    public static final String ELE_PREFIX = "ele";

    /**
	 * The path to look for the convertible registry properties file
	 */
    public static final String CONV_PROPS = "/com/peterhix/net/client/conv.properties";

    public static final String PROC_PROPS = "/com/peterhix/net/client/proc.properties";

    private SimpleClient simpleClient;

    private DatagramSocket udpSocket;

    private InetSocketAddress nserverAddress = new InetSocketAddress("localhost", 8000);

    private NetworkSystem networkSystem;

    private Map<String, Object> properties;

    public static final String KEY_LOCALE = "locale";

    public static final String KEY_LAF = "laf";

    public static final String KEY_SUPPORTED_LAF = "supportedLaf";

    public static final String KEY_ACCOUNT = "account";

    public static final String KEY_KEY = "key";

    public static final String KEY_NSERVER_ID = "nid";

    public static final String KEY_NSERVER_CHANNEL_ID = "ncid";

    public static final String KEY_ELE_COUNTER = "eleCounter";

    public static final String KEY_MIC = "mic";

    public static final String KEY_ENC = "enc";

    public static final String KEY_SGS_LOGGED_IN = "sgsLoggedIn";

    public static final String KEY_UDP_LOGGED_IN = "udpLoggedIn";

    public static final String KEY_IN_CHANNEL = "inChannel";

    public static final String KEY_IS_LOGGING_IN = "isLoggingIn";

    public static final String KEY_LOGIN_CALLBACK = "loginCallback";

    public static final String KEY_IS_QUERYING_CHANNEL = "isQueryingChannel";

    public static final String KEY_QUERY_CHANNEL_CALLBACK = "queryChannelCallback";

    public static final String KEY_IS_ENTERING_CHANNEL = "isEnteringChannel";

    public static final String KEY_ENTER_CHANNEL_CALLBACK = "enterChannelCallback";

    private Set<SGSClientListener> listeners;

    public Client() {
        try {
            listeners = new HashSet<SGSClientListener>();
            properties = new Hashtable<String, Object>();
            setProperty(KEY_LOCALE, Locale.getDefault());
            setProperty(KEY_LAF, UIManager.getCrossPlatformLookAndFeelClassName());
            setProperty(KEY_SUPPORTED_LAF, new String[] { UIManager.getCrossPlatformLookAndFeelClassName(), UIManager.getSystemLookAndFeelClassName(), "com.sun.java.swing.plaf.motif.MotifLookAndFeel" });
            setProperty(KEY_UDP_LOGGED_IN, Boolean.FALSE);
            setProperty(KEY_SGS_LOGGED_IN, Boolean.FALSE);
            setProperty(KEY_IS_LOGGING_IN, Boolean.FALSE);
            setProperty(KEY_IS_QUERYING_CHANNEL, Boolean.FALSE);
            setProperty(KEY_IS_ENTERING_CHANNEL, Boolean.FALSE);
            Properties convs = new Properties();
            convs.load(Client.class.getResourceAsStream(CONV_PROPS));
            Converter.getInstance().load(convs);
            Properties procs = new Properties();
            procs.load(Client.class.getResourceAsStream(PROC_PROPS));
            Processor.getInstance().load(procs);
            udpSocket = new DatagramSocket();
            info(logger, "datagram socket bound", udpSocket.getLocalSocketAddress());
            networkSystem = new NetworkSystem(this);
            simpleClient = new SimpleClient(networkSystem.getSGSListener());
            Manager man = Manager.getInstance();
            properties.put(KEY_MIC, man.getMic("Pcm", man.getAnyTargetDataLine()));
            properties.put(KEY_ENC, man.getEncoder("Speex"));
        } catch (Exception ex) {
            throwing(logger, "SGSClient", "(init)", ex, udpSocket);
        }
    }

    public SimpleClient getSimpleClient() {
        return simpleClient;
    }

    public DatagramSocket getDatagramSocket() {
        return udpSocket;
    }

    public InetSocketAddress getNServerAddress() {
        return nserverAddress;
    }

    public Object getProperty(String key) {
        return properties.get(key);
    }

    public void setProperty(String key, Object value) {
        if (value == null) {
            properties.remove(key);
        } else {
            properties.put(key, value);
        }
    }

    public boolean isAlreadyLoggedIn() {
        Boolean b1 = (Boolean) getProperty(KEY_UDP_LOGGED_IN);
        Boolean b2 = (Boolean) getProperty(KEY_SGS_LOGGED_IN);
        return (b1.booleanValue() && b2.booleanValue());
    }

    public String getURL(String name) {
        return getClass().getResource("/com/peterhix/net/client/resource/" + name).toExternalForm();
    }

    public String getString(Locale locale, String key) {
        return ResourceBundle.getBundle("com/peterhix/net/client/resource", locale).getString(key);
    }

    public Font getFontByLocale(Locale l) {
        if (l.equals(Locale.CHINA)) {
            return new Font("SimSun", Font.PLAIN, 14);
        } else {
            return new Font("Dialog", Font.PLAIN, 14);
        }
    }

    public FontUIResource getFontUIResourceByLocale(Locale l) {
        if (l.equals(Locale.CHINA)) {
            return new FontUIResource("SimSun", Font.PLAIN, 14);
        } else {
            return new FontUIResource("Dialog", Font.PLAIN, 14);
        }
    }

    public void updateLocale(Component c, Locale l) {
        Locale.setDefault(l);
        JOptionPane.setDefaultLocale(l);
        JColorChooser.setDefaultLocale(l);
        Font f = getFontByLocale(l);
        FontUIResource resource = getFontUIResourceByLocale(l);
        if (resource != null) {
            for (Enumeration<Object> keys = UIManager.getDefaults().keys(); keys.hasMoreElements(); ) {
                Object key = keys.nextElement();
                Object value = UIManager.get(key);
                if (value instanceof FontUIResource) {
                    UIManager.put(key, resource);
                }
            }
        }
        setProperty(KEY_LOCALE, l);
        PeterHiException.setLocale(l);
        if (c != null) {
            setAllFonts(c, f);
        }
    }

    private void setAllFonts(Component c, Font f) {
        c.setFont(new Font(f.getName(), f.getStyle(), f.getSize()));
        if (c instanceof Container) {
            Container cn = (Container) c;
            for (int i = 0; i < cn.getComponentCount(); i++) {
                setAllFonts(cn.getComponent(i), f);
            }
        }
        if (c instanceof JMenuBar) {
            JMenuBar menuBar = (JMenuBar) c;
            for (int i = 0; i < menuBar.getMenuCount(); i++) {
                if (menuBar.getMenu(i) != null) {
                    setAllFonts(menuBar.getMenu(i), f);
                }
            }
        } else if (c instanceof JMenu) {
            JMenu menu = (JMenu) c;
            for (int i = 0; i < menu.getItemCount(); i++) {
                if (menu.getItem(i) != null) {
                    setAllFonts(menu.getItem(i), f);
                }
            }
        } else if (c instanceof Titlebar) {
            Titlebar bar = (Titlebar) c;
            bar.setFont(f);
        } else if (c instanceof JTabbedPane) {
            JTabbedPane pane = (JTabbedPane) c;
            for (int i = 0; i < pane.getTabCount(); i++) {
                if (pane.getTabComponentAt(i) != null) {
                    setAllFonts(pane.getTabComponentAt(i), f);
                }
            }
        } else if (c instanceof JFrame) {
            JFrame frame = (JFrame) c;
            setAllFonts(frame.getJMenuBar(), f);
        }
    }

    public void updateLaf(Component c, String laf) {
        try {
            setProperty(KEY_LAF, laf);
            UIManager.setLookAndFeel(laf);
            SwingUtilities.updateComponentTreeUI(c);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void addSGSClientListener(SGSClientListener l) {
        if (l == null) {
            throw new NullPointerException();
        }
        listeners.add(l);
    }

    public void removeSGSClientListener(SGSClientListener l) {
        if (l == null) {
            throw new NullPointerException();
        }
        listeners.remove(l);
    }

    public void startMic() {
        try {
            final Mic mic = (Mic) getProperty(KEY_MIC);
            final Encoder enc = (Encoder) getProperty(KEY_ENC);
            final int nid = (Integer) getProperty(KEY_NSERVER_ID);
            mic.start(Manager.getInstance().getDefaultVoiceAudioFormat());
            OpenMic om = new OpenMic();
            om.nid = nid;
            getSimpleClient().send(Converter.getInstance().convert(om));
            Thread th = new Thread() {

                public void run() {
                    try {
                        Thread.sleep(500);
                    } catch (Exception ex) {
                    }
                    while (mic.isStarted()) {
                        try {
                            byte[] data = mic.read(null);
                            int len = 0;
                            if (data != null) {
                                len = enc.encode(data);
                                byte[] toSend = new byte[len];
                                System.arraycopy(data, 0, toSend, 0, len);
                                Voice v = new Voice();
                                v.nid = nid;
                                v.data = toSend;
                                Packet[] packets = Converter.getInstance().convertAndSplit(new Packet(), v);
                                networkSystem.udpPutAll(packets, nserverAddress);
                            }
                        } catch (Exception ex) {
                        }
                    }
                }
            };
            th.start();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void stopMic() {
        try {
            Mic mic = (Mic) getProperty(KEY_MIC);
            int nid = (Integer) getProperty(KEY_NSERVER_ID);
            mic.stop();
            CloseMic cm = new CloseMic();
            cm.nid = nid;
            getSimpleClient().send(Converter.getInstance().convert(cm));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void startSpeaker(final Speaker speaker) {
        try {
            if (speaker != null) {
                speaker.start(Manager.getInstance().getDefaultVoiceAudioFormat());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void stopSpeaker(final Speaker speaker) {
        if (speaker != null) {
            speaker.stop();
        }
    }

    public void play(final Decoder decoder, final Speaker speaker, final byte[] data) {
        if ((decoder == null) || (speaker == null)) {
            return;
        } else {
            byte[] buffer = new byte[320];
            System.arraycopy(data, 0, buffer, 0, data.length);
            decoder.decode(buffer, data.length);
            speaker.write(buffer);
        }
    }

    public void fireLoggingIn() {
        setProperty(KEY_IS_LOGGING_IN, Boolean.TRUE);
        for (Iterator<SGSClientListener> itor = listeners.iterator(); itor.hasNext(); ) {
            SGSClientListener cur = itor.next();
            cur.loggingIn();
        }
    }

    public void fireLoggedIn(Integer result, String name, int nid) {
        setProperty(KEY_IS_LOGGING_IN, Boolean.FALSE);
        for (Iterator<SGSClientListener> itor = listeners.iterator(); itor.hasNext(); ) {
            SGSClientListener cur = itor.next();
            cur.loggedIn(result, name, nid);
        }
    }

    public void fireLoginFailed(String message) {
        setProperty(KEY_UDP_LOGGED_IN, Boolean.FALSE);
        setProperty(KEY_SGS_LOGGED_IN, Boolean.FALSE);
        setProperty(KEY_IS_LOGGING_IN, Boolean.FALSE);
        setProperty(KEY_ACCOUNT, "");
        for (Iterator<SGSClientListener> itor = listeners.iterator(); itor.hasNext(); ) {
            SGSClientListener cur = itor.next();
            cur.loginFailed(message);
        }
    }

    public void fireLoggedOut() {
        setProperty(KEY_UDP_LOGGED_IN, Boolean.FALSE);
        setProperty(KEY_SGS_LOGGED_IN, Boolean.FALSE);
        setProperty(KEY_IS_LOGGING_IN, Boolean.FALSE);
        setProperty(KEY_ACCOUNT, "");
        for (Iterator<SGSClientListener> itor = listeners.iterator(); itor.hasNext(); ) {
            SGSClientListener cur = itor.next();
            cur.loggedOut();
        }
    }

    public void fireQueryingChannel() {
        setProperty(KEY_IS_QUERYING_CHANNEL, Boolean.TRUE);
        for (Iterator<SGSClientListener> itor = listeners.iterator(); itor.hasNext(); ) {
            SGSClientListener cur = itor.next();
            cur.queryingChannel();
        }
    }

    public void fireQueriedChannel(Integer result, Object channels) {
        setProperty(KEY_IS_QUERYING_CHANNEL, Boolean.FALSE);
        for (Iterator<SGSClientListener> itor = listeners.iterator(); itor.hasNext(); ) {
            SGSClientListener cur = itor.next();
            cur.queriedChannel(result, channels);
        }
    }

    public void fireQueryChannelFailed(String message) {
        setProperty(KEY_IS_QUERYING_CHANNEL, Boolean.FALSE);
        for (Iterator<SGSClientListener> itor = listeners.iterator(); itor.hasNext(); ) {
            SGSClientListener cur = itor.next();
            cur.queryChannelFailed(message);
        }
    }

    public void fireEnteringChannel() {
        setProperty(KEY_IS_ENTERING_CHANNEL, Boolean.TRUE);
    }

    public void fireEnteredChannel(Integer result, String name, int ncid) {
        setProperty(KEY_IS_ENTERING_CHANNEL, Boolean.FALSE);
    }

    public void fireEnterChannelFailed(String message) {
        setProperty(KEY_IS_ENTERING_CHANNEL, Boolean.FALSE);
    }

    public void fireLeftChannel(String name) {
        for (Iterator<SGSClientListener> itor = listeners.iterator(); itor.hasNext(); ) {
            SGSClientListener cur = itor.next();
            cur.leftChannel(name);
        }
    }

    public void fireDisconnected(String message) {
        setProperty(KEY_UDP_LOGGED_IN, Boolean.FALSE);
        setProperty(KEY_SGS_LOGGED_IN, Boolean.FALSE);
        setProperty(KEY_IS_LOGGING_IN, Boolean.FALSE);
        setProperty(KEY_ACCOUNT, "");
        for (Iterator<SGSClientListener> itor = listeners.iterator(); itor.hasNext(); ) {
            SGSClientListener cur = itor.next();
            cur.disconnected(message);
        }
    }

    public void fireReceived(Object source, Object client, Object channel, Convertible message, Object state) {
        for (Iterator<SGSClientListener> itor = listeners.iterator(); itor.hasNext(); ) {
            SGSClientListener cur = itor.next();
            cur.received(source, client, channel, message, state);
        }
    }

    public void fireReceived(Object source, Object client, Object channel, Packet packet, Object state) {
        for (Iterator<SGSClientListener> itor = listeners.iterator(); itor.hasNext(); ) {
            SGSClientListener cur = itor.next();
            cur.received(source, client, channel, packet, state);
        }
    }

    public String getAuthenticateCode() {
        return "" + getProperty(KEY_KEY) + getProperty(KEY_ACCOUNT);
    }

    public String nextEleID() {
        int nid = (Integer) getProperty(KEY_NSERVER_ID);
        int eleCounter = (Integer) getProperty(KEY_ELE_COUNTER);
        setProperty(KEY_ELE_COUNTER, Integer.valueOf(eleCounter + 1));
        return ELE_PREFIX + "-" + nid + "-" + eleCounter;
    }

    public void login(InetSocketAddress loginServerAddr, String account, char[] password, AsyncCallbackListener callback) {
        try {
            Boolean isLoggingIn = (Boolean) getProperty(KEY_IS_LOGGING_IN);
            if (!isLoggingIn.booleanValue()) {
                setProperty(KEY_IS_LOGGING_IN, Boolean.TRUE);
                setProperty(KEY_LOGIN_CALLBACK, callback);
                fireLoggingIn();
                networkSystem.getDatagramReceiver().setRunning(true);
                networkSystem.getDatagramSender().setRunning(true);
                setProperty(KEY_ACCOUNT, account);
                Socket temp = new Socket();
                temp.bind(new InetSocketAddress(0));
                temp.connect(loginServerAddr);
                Login login = new Login();
                login.account = account;
                login.password = password;
                Packet[] array = Converter.getInstance().convertAndSplit(new Packet(), login);
                for (int i = 0; i < array.length; i++) {
                    temp.getOutputStream().write(array[i].toByteArray());
                }
                temp.getOutputStream().flush();
                byte[] data = new byte[Packet.PACKET_SIZE];
                temp.getInputStream().read(data);
                SessionKey rsp = (SessionKey) Converter.getInstance().revert(new Packet[] { Packet.fromByteArray(data) });
                if (!Results.succeeded(rsp.result)) {
                    throw new PeterHiException(rsp.result);
                }
                setProperty(KEY_KEY, rsp.key);
                UdpContact uc = new UdpContact();
                uc.account = (String) getProperty(KEY_ACCOUNT);
                uc.key = (String) getProperty(KEY_KEY);
                networkSystem.udpPutAll(Converter.getInstance().convertAndSplit(new Packet(), uc), loginServerAddr);
                Properties props = new Properties();
                props.put("host", "localhost");
                props.put("port", "8001");
                simpleClient.login(props);
            }
        } catch (Exception ex) {
            setProperty(KEY_SGS_LOGGED_IN, Boolean.FALSE);
            setProperty(KEY_UDP_LOGGED_IN, Boolean.FALSE);
            setProperty(KEY_IS_LOGGING_IN, Boolean.FALSE);
            setProperty(KEY_ACCOUNT, "");
            fireLoginFailed(ex.getMessage());
            throw new RuntimeException(ex);
        }
    }

    public void callbackLogin(Integer result) {
        try {
            if (isAlreadyLoggedIn()) {
                setProperty(KEY_IS_LOGGING_IN, Boolean.FALSE);
                if (Results.succeeded(result)) {
                    String name = (String) getProperty(KEY_ACCOUNT);
                    int nid = (Integer) getProperty(KEY_NSERVER_ID);
                    fireLoggedIn(result, name, nid);
                } else {
                    Locale locale = (Locale) getProperty(KEY_LOCALE);
                    fireLoginFailed(Results.toString(locale, result));
                }
                AsyncCallbackListener l = (AsyncCallbackListener) getProperty(KEY_LOGIN_CALLBACK);
                if (l != null) {
                    l.callback(result);
                }
            }
        } catch (Exception ex) {
            fireLoginFailed(ex.getMessage());
            throw new RuntimeException(ex);
        }
    }

    public void logout() {
        try {
            setProperty(KEY_SGS_LOGGED_IN, Boolean.FALSE);
            setProperty(KEY_UDP_LOGGED_IN, Boolean.FALSE);
            setProperty(KEY_ACCOUNT, "");
            simpleClient.send(Converter.getInstance().convert(new Logout()));
            fireLoggedOut();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public void queryChannel(AsyncCallbackListener callback) {
        try {
            Boolean isQueryingChannel = (Boolean) getProperty(KEY_IS_QUERYING_CHANNEL);
            if (!isQueryingChannel.booleanValue()) {
                setProperty(KEY_QUERY_CHANNEL_CALLBACK, callback);
                fireQueryingChannel();
                simpleClient.send(Converter.getInstance().convert(new QueryChannel()));
            }
        } catch (Exception ex) {
            setProperty(KEY_IS_QUERYING_CHANNEL, Boolean.FALSE);
            fireQueryChannelFailed(ex.getMessage());
            throw new RuntimeException(ex);
        }
    }

    public void callbackQueryChannel(Integer result, Object channels) {
        fireQueriedChannel(result, channels);
        AsyncCallbackListener l = (AsyncCallbackListener) getProperty(KEY_QUERY_CHANNEL_CALLBACK);
        if (l != null) {
            l.callback(channels);
        }
    }

    public void enterChannel(String name, String password, AsyncCallbackListener callback) {
        try {
            Boolean isEnteringChannel = (Boolean) getProperty(KEY_IS_ENTERING_CHANNEL);
            if (!isEnteringChannel.booleanValue()) {
                setProperty(KEY_ENTER_CHANNEL_CALLBACK, callback);
                fireEnteringChannel();
                EnterChannel ec = new EnterChannel();
                ec.name = name;
                ec.password = password;
                simpleClient.send(Converter.getInstance().convert(ec));
            }
        } catch (Exception ex) {
            setProperty(KEY_IS_ENTERING_CHANNEL, Boolean.FALSE);
            fireEnterChannelFailed(ex.getMessage());
            throw new RuntimeException(ex);
        }
    }

    public void callbackEnterChannel(Integer result, String name, int ncid) {
        fireEnteredChannel(result, name, ncid);
        AsyncCallbackListener l = (AsyncCallbackListener) getProperty(KEY_ENTER_CHANNEL_CALLBACK);
        if (l != null) {
            l.callback(name);
        }
    }

    public void leaveChannel() {
        try {
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
	 * 
	 * @param args
	 */
    public static void main(String[] args) {
        doRun(null);
    }

    public static void doRun(ClassLoader scriptLoader) {
        Client sgsClient = new Client();
        mainLoop(sgsClient, scriptLoader);
    }

    private static void mainLoop(Client sgsClient, ClassLoader scriptLoader) {
        String url = "/com/peterhix/net/client/__main.fx";
        GUIRunner runner = new GUIRunner(url, scriptLoader);
        runner.getBindings().put("C:com.peterhix.net.client.Client", sgsClient);
        SwingUtilities.invokeLater(runner);
        logger.info("fx script at " + url);
    }
}
