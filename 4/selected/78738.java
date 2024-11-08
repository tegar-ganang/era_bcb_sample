package org.smapcore.smap.core.client;

import java.awt.Toolkit;
import java.io.InputStream;
import java.io.IOException;
import java.util.HashMap;
import org.smapcore.smap.core.*;
import org.smapcore.smap.transport.SMAPChannel;
import org.smapcore.smap.transport.SMAPSession;
import org.smapcore.smap.transport.SMAPTransportLoader;
import org.smapcore.smap.transport.SMAPTransportManager;

public class SMAP {

    public static final String VERSION = "SMAP/1.0";

    public static final String URL = "http://www.ontapsolutions.com";

    private SMAPTransportManager mgr;

    private SMAPSession session = null;

    private SMAPChannel channel = null;

    private String server;

    private int port;

    private int bufferSize = 0;

    private boolean beep = false;

    public SMAP(String impl, String server, int port, boolean privacy) throws SMAPException {
        this.server = server;
        this.port = port;
        this.mgr = SMAPTransportLoader.getTransportManager(impl);
        this.session = mgr.getSession(server, port, privacy);
    }

    public void connect() throws SMAPException {
        if (this.session.isOpen() == false) {
            try {
                this.session.open();
            } catch (SMAPException se) {
                throw new SMAPException("unable to open connection to " + this.server + ":" + this.port, se);
            }
        }
        if (this.channel != null) {
            try {
                close();
            } catch (Exception e) {
            }
        }
        try {
            this.channel = this.session.getChannel();
        } catch (SMAPException se) {
            throw new SMAPException("unable to smap " + this.server + ":" + this.port, se);
        }
        if (this.bufferSize == 0) {
            this.bufferSize = getWindowSize();
        } else {
            try {
                setWindowSize(this.bufferSize);
            } catch (SMAPException se) {
            }
        }
        return;
    }

    public SMAPReplyEnvelope sendRequest(String method, HashMap args) throws SMAPException {
        return (sendRequest(method, args, null, null));
    }

    public SMAPReplyEnvelope sendRequest(final String method, final HashMap args, final String mime, InputStream data) throws SMAPException {
        return (sendRequest(method, args, mime, data, -1));
    }

    public SMAPReplyEnvelope sendRequest(final String method, final HashMap args, final String mime, InputStream data, final int size) throws SMAPException {
        if (method == null) {
            throw new SMAPException("no method in request");
        }
        SMAPRequestEnvelope envelope = new SMAPRequestEnvelope();
        envelope.setProc(method, args);
        if (data != null) {
            envelope.setNumberMessages(1);
            SMAPMessage message = new SMAPMessage(mime, data, size);
            envelope.addMessage(message);
            envelope.endMessages();
        }
        return (sendRequest(envelope));
    }

    public SMAPReplyEnvelope sendRequest(SMAPRequestEnvelope envelope) throws SMAPException {
        SMAPReplyEnvelope reply = null;
        try {
            reply = this.channel.sendRequest(envelope);
        } catch (SMAPException se) {
            try {
                connect();
                reply = this.channel.sendRequest(envelope);
            } catch (SMAPException se2) {
                throw new SMAPException("unable to make request", se2);
            }
            if (reply == null) {
                throw new SMAPException("unable to receive reply", se);
            }
        }
        if (this.beep == true) {
            Toolkit.getDefaultToolkit().beep();
        }
        return (reply);
    }

    public void close() throws SMAPException {
        try {
            this.channel.close();
        } catch (SMAPException se) {
            throw new SMAPException("unable to close smap to " + this.server + ":" + this.port, se);
        } finally {
            this.channel = null;
        }
        return;
    }

    public int getWindowSize() {
        return (this.channel.getWindowSize());
    }

    public void setWindowSize(int size) throws SMAPException {
        try {
            this.channel.setWindowSize(size);
            this.bufferSize = size;
        } catch (SMAPException se) {
            throw new SMAPException("cannot set receive buffer size", se);
        }
        return;
    }

    public String getServer() {
        return (this.server);
    }

    public int getPort() {
        return (this.port);
    }

    public void beep() {
        this.beep = true;
        return;
    }
}
