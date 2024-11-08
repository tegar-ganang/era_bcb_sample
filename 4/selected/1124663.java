package org.speechforge.zanzibar.speechlet;

import java.io.IOException;
import javax.sip.SipException;
import org.mrcp4j.client.MrcpInvocationException;
import org.speechforge.cairo.sip.SipSession;
import org.speechforge.cairo.client.NoMediaControlChannelException;
import org.speechforge.cairo.client.SpeechClient;
import org.speechforge.cairo.client.SpeechClientImpl;
import org.speechforge.zanzibar.telephony.TelephonyClient;
import org.speechforge.zanzibar.telephony.TelephonyClientImpl;

public class SpeechletContextImpl implements SpeechletContext {

    private SpeechletService container;

    private SessionProcessor speechlet;

    SipSession internalSession;

    SipSession externalSession;

    SpeechClient speechClient;

    TelephonyClient telephonyClient;

    public void init() throws InvalidContextException {
        if (internalSession == null) throw new InvalidContextException();
        this.speechClient = new SpeechClientImpl(internalSession.getTtsChannel(), internalSession.getRecogChannel());
        this.telephonyClient = new TelephonyClientImpl(externalSession.getChannelName());
    }

    public void cleanup() {
        internalSession = null;
        externalSession = null;
        speechClient = null;
        telephonyClient = null;
    }

    public void dialogCompleted() throws InvalidContextException {
        if (container == null) throw new InvalidContextException();
        try {
            externalSession.getAgent().sendBye(externalSession);
            speechClient.stopActiveRecognitionRequests();
            container.StopDialog(externalSession);
        } catch (SipException e) {
            e.printStackTrace();
        } catch (MrcpInvocationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (NoMediaControlChannelException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return the container
     */
    public SpeechletService getContainer() {
        return container;
    }

    /**
     * @param container the container to set
     */
    public void setContainer(SpeechletService container) {
        this.container = container;
    }

    /**
     * @return the speechlet
     */
    public SessionProcessor getSpeechlet() {
        return speechlet;
    }

    /**
     * @param speechlet the speechlet to set
     */
    public void setSpeechlet(SessionProcessor speechlet) {
        this.speechlet = speechlet;
    }

    /**
     * @return the externalSession
     */
    public SipSession getExternalSession() {
        return externalSession;
    }

    /**
     * @param externalSession the externalSession to set
     */
    public void setExternalSession(SipSession externalSession) {
        this.externalSession = externalSession;
    }

    /**
     * @return the internalSession
     */
    public SipSession getInternalSession() {
        return internalSession;
    }

    /**
     * @param internalSession the internalSession to set
     */
    public void setInternalSession(SipSession internalSession) {
        this.internalSession = internalSession;
    }

    /**
     * @return the speechClient
     */
    public SpeechClient getSpeechClient() {
        return speechClient;
    }

    /**
     * @return the telephonyClient
     */
    public TelephonyClient getTelephonyClient() {
        return telephonyClient;
    }
}
