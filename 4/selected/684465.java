package org.speechforge.zanzibar.telephony;

import java.io.IOException;
import org.apache.log4j.Logger;
import org.asteriskjava.manager.ManagerEventListener;
import org.asteriskjava.manager.TimeoutException;
import org.asteriskjava.manager.event.HangupEvent;
import org.asteriskjava.manager.event.ManagerEvent;
import org.speechforge.zanzibar.asterisk.CallControl;
import org.speechforge.zanzibar.server.SpeechletServerMain;

/**
 * Implementation of a telephony client.
 * 
 * @author Spencer Lord {@literal <}<a href="mailto:salord@users.sourceforge.net">salord@users.sourceforge.net</a>{@literal >}
 */
public class TelephonyClientImpl implements TelephonyClient, ManagerEventListener {

    /**
     * The _logger.
     */
    private static Logger _logger = Logger.getLogger(TelephonyClientImpl.class);

    /**
     * The _channel.
     */
    private String _channel;

    /**
     * The _call completed.
     */
    private boolean _callCompleted = false;

    /**
     * The cc.
     */
    CallControl cc = null;

    /**
     * Instantiates a new telephony client impl.
     * 
     * @param channel the channel
     */
    public TelephonyClientImpl(String channel) {
        super();
        if (channel != null) _channel = channel.trim();
        cc = (CallControl) SpeechletServerMain.context.getBean("callControl");
    }

    public void redirectBlocking(String channel, String connectContext, String connectTo) throws IOException, TimeoutException {
        cc.addEventListener(this);
        _callCompleted = false;
        cc.addEventListener(this);
        cc.amiRedirect(channel, connectContext, connectTo);
        while (!_callCompleted) {
            synchronized (this) {
                try {
                    this.wait(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        cc.removeEventListener(this);
        _logger.info("Returning from the redirect blocking call");
    }

    public void redirect(String channel, String connectContext, String connectTo) throws IOException, TimeoutException {
        cc.amiRedirect(channel, connectContext, connectTo);
    }

    public void onManagerEvent(ManagerEvent event) {
        _logger.debug(event.toString());
        _logger.info("Asterisk Event: " + event.getClass().getCanonicalName());
        if (event instanceof org.asteriskjava.manager.event.HangupEvent) {
            HangupEvent e = (HangupEvent) event;
            String channel = e.getChannel().trim();
            _logger.info("got a hangup on the channel: " + e.getChannel() + "|" + _channel);
            if (channel.equals(_channel)) {
                synchronized (this) {
                    _callCompleted = true;
                    this.notifyAll();
                }
            }
        }
    }
}
