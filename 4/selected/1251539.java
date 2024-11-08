package org.jpos.apps.qsp;

import java.io.IOException;
import org.jpos.core.Configurable;
import org.jpos.core.Configuration;
import org.jpos.core.ConfigurationException;
import org.jpos.iso.BaseChannel;
import org.jpos.iso.ISOChannel;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMUX;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISORequest;
import org.jpos.iso.ISORequestListener;
import org.jpos.iso.ISOSource;
import org.jpos.util.LogEvent;
import org.jpos.util.LogSource;
import org.jpos.util.Logger;
import org.jpos.util.ThreadPool;
import org.jpos.util.NameRegistrar.NotFoundException;

/**
 * QSP Connector implements ISORequestListener
 * and forward all incoming messages to a given
 * destination MUX, handling back responses
 *
 * @author <a href="mailto:apr@cs.com.uy">Alejandro P. Revilla</a>
 * @version $Revision: 1745 $ $Date: 2003-10-13 07:04:20 -0400 (Mon, 13 Oct 2003) $
 * @see org.jpos.iso.ISORequestListener
 */
public class Connector implements ISORequestListener, LogSource, Configurable {

    Logger logger;

    String realm;

    ISOMUX destMux;

    ISOChannel destChannel;

    int timeout = 0;

    boolean bounce = false;

    static ThreadPool pool;

    static {
        pool = new ThreadPool(1, 100);
    }

    public Connector() {
        super();
        destMux = null;
        destChannel = null;
    }

    public void setLogger(Logger logger, String realm) {
        this.logger = logger;
        this.realm = realm;
    }

    public String getRealm() {
        return realm;
    }

    public Logger getLogger() {
        return logger;
    }

    /**
    * Destination can be a Channel or a MUX. If Destination is a Channel
    * then timeout applies (used on ISORequest to get a Response).
    * <ul>
    * <li>destination-mux
    * <li>destination-channel
    * <li>timeout
    * <li>bounce
    * </ul>
    * @param cfg Configuration
    */
    public void setConfiguration(Configuration cfg) throws ConfigurationException {
        timeout = cfg.getInt("timeout");
        bounce = cfg.getBoolean("bounce");
        String muxName = cfg.get("destination-mux", null);
        String channelName = cfg.get("destination-channel", null);
        try {
            if (muxName != null) destMux = ISOMUX.getMUX(muxName); else if (channelName != null) destChannel = BaseChannel.getChannel(channelName);
        } catch (NotFoundException e) {
            throw new ConfigurationException(e);
        }
    }

    /**
     * hook used to optional bounce an unanswered message 
     * to its source channel
     * @param s message source
     * @param m unanswered message
     * @exception ISOException
     * @exception IOException
     */
    protected void processNullResponse(ISOSource s, ISOMsg m, LogEvent evt) throws ISOException, IOException {
        if (bounce) {
            ISOMsg c = (ISOMsg) m.clone();
            c.setResponseMTI();
            if (c.hasField(39)) c.unset(39);
            s.send(c);
            evt.addMessage("<bounced/>");
        } else evt.addMessage("<null-response/>");
    }

    protected class Process implements Runnable {

        ISOSource source;

        ISOMsg m;

        Process(ISOSource source, ISOMsg m) {
            super();
            this.source = source;
            this.m = m;
        }

        public void run() {
            LogEvent evt = new LogEvent(Connector.this, "connector-request-listener");
            try {
                ISOMsg c = (ISOMsg) m.clone();
                evt.addMessage(c);
                if (destMux != null) {
                    if (timeout > 0) {
                        ISOMsg response = null;
                        if (destMux.isConnected()) {
                            ISORequest req = new ISORequest(c);
                            destMux.queue(req);
                            evt.addMessage("<queued/>");
                            response = req.getResponse(timeout);
                        } else evt.addMessage("<mux-not-connected/>");
                        if (response != null) {
                            evt.addMessage("<got-response/>");
                            evt.addMessage(response);
                            response.setHeader(c.getISOHeader());
                            source.send(response);
                        } else {
                            processNullResponse(source, m, evt);
                        }
                    } else {
                        evt.addMessage("<sent-through-mux/>");
                        destMux.send(c);
                    }
                } else if (destChannel != null) {
                    evt.addMessage("<sent-to-channel/>");
                    destChannel.send(c);
                }
            } catch (ISOException e) {
                evt.addMessage(e);
            } catch (IOException e) {
                evt.addMessage(e);
            }
            Logger.log(evt);
        }
    }

    public boolean process(ISOSource source, ISOMsg m) {
        pool.execute(new Process(source, m));
        return true;
    }
}
