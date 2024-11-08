package org.smapcore.smap.transport.beep;

import org.beepcore.beep.core.BEEPError;
import org.beepcore.beep.core.BEEPException;
import org.beepcore.beep.core.Channel;
import org.beepcore.beep.core.Session;
import org.beepcore.beep.profile.tls.TLSProfile;
import org.beepcore.beep.transport.tcp.TCPSessionCreator;
import org.beepcore.beep.transport.tcp.TCPSession;
import org.smapcore.smap.core.SMAPException;

public class SMAPSession extends Object implements org.smapcore.smap.transport.SMAPSession {

    private String server;

    private int port;

    private Session session = null;

    private boolean privacy = true;

    protected SMAPSession(String server, int port, boolean privacy) {
        this.server = server;
        this.port = port;
        this.privacy = privacy;
        return;
    }

    public synchronized void open() throws SMAPException {
        Session session;
        if (this.isOpen() == true) {
            return;
        }
        try {
            this.session = TCPSessionCreator.initiate(this.server, this.port);
        } catch (BEEPException be) {
            this.session = null;
            throw new SMAPException("unable to initiate session to " + this.server + ":" + this.port, be);
        }
        if (privacy == true) {
            try {
                this.session = TLSProfile.getDefaultInstance().startTLS((TCPSession) this.session);
            } catch (BEEPException be) {
                this.session = null;
                throw new SMAPException("unable to start TLS", be);
            }
        }
    }

    public synchronized void close() throws SMAPException {
        try {
            this.session.close();
        } catch (BEEPException be) {
            throw new SMAPException("unable to close session", be);
        } finally {
            this.session = null;
        }
        return;
    }

    public org.smapcore.smap.transport.SMAPChannel getChannel() throws SMAPException {
        return (getChannel(false));
    }

    public boolean isOpen() {
        if (this.session == null) {
            return (false);
        } else {
            return (true);
        }
    }

    public boolean isPrivate() {
        return (this.session.getTuningProperties().getEncrypted());
    }

    private org.smapcore.smap.transport.SMAPChannel getChannel(boolean retry) throws SMAPException {
        Channel channel;
        if (isOpen() == false) {
            open();
        }
        System.out.println(this.session.toString());
        try {
            channel = this.session.startChannel(BEEPProfile.URI);
        } catch (BEEPError be) {
            if (be.getCode() == 550) {
                throw new SMAPException(this.server + ":" + this.port + " does not support " + BEEPProfile.URI, be);
            } else {
                if (retry == true) {
                    throw new SMAPException("lost connection to " + this.server + ":" + this.port, be);
                } else {
                    try {
                        close();
                    } catch (SMAPException se) {
                    }
                    try {
                        open();
                    } catch (SMAPException se) {
                        throw new SMAPException("cannot connect to " + this.server + ":" + this.port, se);
                    }
                    return (getChannel(true));
                }
            }
        } catch (BEEPException be) {
            throw new SMAPException("cannot connect to " + this.server + ":" + this.port, be);
        }
        return (new SMAPChannel(channel));
    }
}
