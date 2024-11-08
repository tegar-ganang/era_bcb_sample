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

public class SpeechletContextMrcpv2Impl implements SpeechletContext, SpeechletContextMrcpProvider {

    SpeechletService container;

    SessionProcessor speechlet;

    SipSession mrcpSession;

    SipSession pbxSession;

    SpeechClient speechClient;

    TelephonyClient telephonyClient;

    public void init() throws InvalidContextException {
        if (mrcpSession == null) throw new InvalidContextException();
        this.speechClient = new SpeechClientImpl(mrcpSession.getTtsChannel(), mrcpSession.getRecogChannel());
        this.telephonyClient = new TelephonyClientImpl(pbxSession.getChannelName());
    }

    public void cleanup() {
        mrcpSession = null;
        pbxSession = null;
        speechClient = null;
        telephonyClient = null;
    }

    public void dialogCompleted() throws InvalidContextException {
        if (container == null) throw new InvalidContextException();
        try {
            pbxSession.getAgent().sendBye(pbxSession);
            speechClient.stopActiveRecognitionRequests();
            container.StopDialog(pbxSession);
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
    public SipSession getPBXSession() {
        return pbxSession;
    }

    /**
     * @param externalSession the externalSession to set
     */
    public void setPBXSession(SipSession externalSession) {
        this.pbxSession = externalSession;
    }

    /**
     * @return the internalSession
     */
    public SipSession getMRCPv2Session() {
        return mrcpSession;
    }

    /**
     * @param internalSession the internalSession to set
     */
    public void setMRCPSession(SipSession internalSession) {
        this.mrcpSession = internalSession;
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
