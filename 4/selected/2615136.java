package org.speechforge.zanzibar.speechlet;

import java.io.IOException;
import javax.sip.SipException;
import org.mrcp4j.client.MrcpInvocationException;
import org.speechforge.cairo.client.NoMediaControlChannelException;
import org.speechforge.cairo.client.SpeechClient;
import org.speechforge.cairo.client.cloudimpl.SpeechCloudClient;
import org.speechforge.cairo.rtp.server.RTPStreamReplicator;
import org.speechforge.cairo.sip.SipSession;
import org.speechforge.zanzibar.telephony.TelephonyClient;
import org.speechforge.zanzibar.telephony.TelephonyClientImpl;
import com.spokentech.speechdown.client.rtp.RtpTransmitter;

public class SpeechletContextCloudImpl implements SpeechletContext, SpeechletContextCloudProvider {

    SpeechletService container;

    SessionProcessor speechlet;

    SpeechClient speechClient;

    TelephonyClient telephonyClient;

    RTPStreamReplicator rtpReplicator = null;

    RtpTransmitter rtpTransmitter = null;

    SipSession pbxSession;

    private String url = null;

    public void init() throws InvalidContextException {
        if ((rtpReplicator == null) && (rtpTransmitter == null)) throw new InvalidContextException();
        this.speechClient = new SpeechCloudClient(rtpReplicator, rtpTransmitter, url);
        this.telephonyClient = new TelephonyClientImpl(pbxSession.getChannelName());
    }

    /**
     * @return the rtpReplicator
     */
    public RTPStreamReplicator getRtpReplicator() {
        return rtpReplicator;
    }

    /**
     * @param rtpReplicator the rtpReplicator to set
     */
    public void setRtpReplicator(RTPStreamReplicator rtpReplicator) {
        this.rtpReplicator = rtpReplicator;
    }

    /**
     * @return the rtpTransmitter
     */
    public RtpTransmitter getRtpTransmitter() {
        return rtpTransmitter;
    }

    /**
     * @param rtpTransmitter the rtpTransmitter to set
     */
    public void setRtpTransmitter(RtpTransmitter rtpTransmitter) {
        this.rtpTransmitter = rtpTransmitter;
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

    public SpeechClient getSpeechClient() {
        return speechClient;
    }

    public TelephonyClient getTelephonyClient() {
        return telephonyClient;
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

    public void setUrl(String url) {
        this.url = url;
        if (speechClient != null) {
            ((SpeechCloudClient) this.speechClient).setServiceUrl(url);
        }
    }
}
