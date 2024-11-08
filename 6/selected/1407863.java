package net.bioclipse.xws.client.xmpp;

import java.util.Locale;
import org.jabberstudio.jso.JID;
import org.jabberstudio.jso.JSOImplementation;
import org.jabberstudio.jso.Stream;
import org.jabberstudio.jso.StreamDataFactory;
import org.jabberstudio.jso.tls.StartTLSSocketStreamSource;
import org.jabberstudio.jso.util.Utilities;
import net.bioclipse.xws.client.xmpp.modules.IqAdHocCommands;
import net.bioclipse.xws.client.xmpp.modules.IqDefault;
import net.bioclipse.xws.client.xmpp.modules.IqDisco;
import net.bioclipse.xws.client.xmpp.modules.StreamEventLogger;
import net.bioclipse.xws.client.xmpp.modules.MessageDefault;
import net.bioclipse.xws.client.xmpp.modules.MessageAdHocCommands;
import net.bioclipse.xws.client.xmpp.modules.PresenceDefault;
import net.bioclipse.xws.client.IExecutionPipe;
import net.bioclipse.xws.client.listeners.IConnectionListener;
import net.bioclipse.xws.client.xmpp.adhoc.AdHocCommand;
import net.bioclipse.xws.client.xmpp.disco.Disco;
import net.bioclipse.xws.client.xmpp.connection.ConManager;
import net.bioclipse.xws.exceptions.XmppException;
import net.bioclipse.xws.XwsLogger;
import net.bioclipse.xws.xmpp.StreamUtils;

public class ClientSession {

    private StreamUtils streamUtils = null;

    private int timeout = 1000;

    private String clientJid, pwd, host;

    private int port;

    private boolean stream_was_used = false;

    private AdHocCommand adhoc;

    private Disco disco;

    private ConManager conmgr;

    private IExecutionPipe executionPipe;

    public ClientSession(String clientJid, String pwd, String host, int port, IExecutionPipe executionPipe) throws XmppException {
        if (executionPipe == null) {
            XwsLogger.error("xmpp: IExecutionPipe is null. An execution pipe must be defined!");
            throw new XmppException("xmpp: IExecutionPipe is null. An execution pipe must be defined!");
        }
        XwsLogger.info("xmpp: Creating ClientSession.");
        adhoc = new AdHocCommand(this);
        disco = new Disco(this);
        conmgr = new ConManager();
        this.clientJid = clientJid;
        this.host = host;
        this.pwd = pwd;
        this.port = port;
        this.executionPipe = executionPipe;
        init(clientJid, pwd, host, port);
    }

    private void init(String clientJid, String pwd, String host, int port) throws XmppException {
        XwsLogger.info("xmpp: initializing ClientSession...");
        JSOImplementation jso = JSOImplementation.getInstance();
        Stream stream = jso.createStream(Utilities.CLIENT_NAMESPACE);
        StreamDataFactory sdf = stream.getDataFactory();
        XwsLogger.info("xmpp: verifying JIDs...");
        JID client = sdf.createJID(clientJid);
        if (!Utilities.isValidString(client.getNode()) || !Utilities.isValidString(client.getResource())) throw new XmppException("Client's JID does not contain a node and/or resource");
        JID server = sdf.createJID(host);
        if (Utilities.isValidString(server.getNode()) || Utilities.isValidString(server.getResource())) throw new XmppException("Host's JID cannot include a node or resource");
        XwsLogger.info("xmpp: preparing the stream...");
        stream.getOutboundContext().setDeclaredLocale(Locale.getDefault());
        stream.getOutboundContext().setVersion("1.0");
        streamUtils = new StreamUtils(stream, client);
        XwsLogger.info("xmpp: loading xmpp modules...");
        new StreamEventLogger(this);
        new PresenceDefault(streamUtils);
        new MessageAdHocCommands(this);
        new MessageDefault(streamUtils);
        new IqAdHocCommands(this);
        new IqDisco(this);
        new IqDefault(streamUtils);
        XwsLogger.info("xmpp: ClientSession successfully initialized.");
    }

    private void reset() throws XmppException {
        disconnect();
        init(clientJid, pwd, host, port);
    }

    private boolean isUsed() {
        return stream_was_used;
    }

    private void setUsed() {
        this.stream_was_used = true;
    }

    public void connect() throws XmppException {
        StartTLSSocketStreamSource tls;
        if (isUsed() == true) {
            reset();
        }
        setUsed();
        Stream stream = streamUtils.getStream();
        try {
            tls = new StartTLSSocketStreamSource(host, port);
            stream.connect(tls);
            stream.open(timeout);
            LoginManager lm = new LoginManager(streamUtils, pwd, tls);
            lm.login();
        } catch (Exception e) {
            throw new XmppException(e);
        }
        streamUtils.startSessionLoop(false);
    }

    public void disconnect() throws XmppException {
        Stream stream = streamUtils.getStream();
        try {
            stream.close(timeout);
            stream.disconnect();
        } catch (Exception e) {
            throw new XmppException(e);
        }
    }

    public boolean isConnected() {
        if (streamUtils.getStream().getCurrentStatus().isOpened()) return true;
        return false;
    }

    public void addConnectionListener(IConnectionListener listener) {
        conmgr.addListener(listener);
    }

    public boolean removeConnectionListener(IConnectionListener listener) {
        return conmgr.removeListener(listener);
    }

    public AdHocCommand getAdHoc() {
        return adhoc;
    }

    public Disco getDisco() {
        return disco;
    }

    public ConManager getConManager() {
        return conmgr;
    }

    public StreamUtils getStreamUtils() {
        return streamUtils;
    }

    public IExecutionPipe getDefaultExecutionPipe() {
        return executionPipe;
    }
}
