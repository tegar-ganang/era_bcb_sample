package org.jmik.asterisk.monitor;

import gov.nist.javax.sip.stack.SIPClientTransaction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.sip.ClientTransaction;
import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.ListeningPoint;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.SipFactory;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.SipStack;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.HeaderFactory;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import org.apache.log4j.Logger;
import org.jmik.asterisk.model.CallListener;
import org.jmik.asterisk.model.ProviderListener;
import org.jmik.asterisk.model.impl.Call;
import org.jmik.asterisk.model.impl.Channel;
import org.jmik.asterisk.model.impl.ConferenceCall;

public class ConferenceCallMonitor implements ProviderListener, CallListener, SipListener {

    protected static Logger logger = Logger.getLogger(ConferenceCallMonitor.class);

    private Map<String, ConferenceCallMonitor.NoteTaker> noteTakers;

    private SipStack sipStack;

    private HeaderFactory headerFactory;

    private AddressFactory addressFactory;

    private MessageFactory messageFactory;

    private SipProvider sipProvider;

    private String myIP;

    private int myPort;

    private String peerIP;

    private int peerPort;

    public ConferenceCallMonitor(String myIP, int myPort, String peerIP, int peerPort) {
        if (myIP == null) {
            logger.error("myIP null");
            throw new IllegalArgumentException("myIP can not be null");
        }
        if (peerIP == null) {
            logger.error("peerIP null");
            throw new IllegalArgumentException("myIP can not be null");
        }
        this.myIP = myIP;
        this.myPort = myPort;
        this.peerIP = peerIP;
        this.peerPort = peerPort;
        this.noteTakers = new HashMap<String, ConferenceCallMonitor.NoteTaker>();
        try {
            init();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
        if (logger.isDebugEnabled()) logger.debug(this);
    }

    public void init() throws Exception {
        SipFactory sipFactory = null;
        sipStack = null;
        sipFactory = SipFactory.getInstance();
        sipFactory.setPathName("gov.nist");
        Properties properties = new Properties();
        properties.setProperty("javax.sip.IP_ADDRESS", myIP);
        properties.setProperty("javax.sip.OUTBOUND_PROXY", peerIP + ":" + peerPort + "/udp");
        properties.setProperty("javax.sip.STACK_NAME", "conferencemonitor");
        properties.setProperty("javax.sip.RETRANSMISSION_FILTER", "on");
        properties.setProperty("gov.nist.javax.sip.CACHE_CLIENT_CONNECTIONS", "false");
        sipStack = sipFactory.createSipStack(properties);
        headerFactory = sipFactory.createHeaderFactory();
        addressFactory = sipFactory.createAddressFactory();
        messageFactory = sipFactory.createMessageFactory();
        ListeningPoint udpListeningPoint = sipStack.createListeningPoint(myPort, "udp");
        sipProvider = sipStack.createSipProvider(udpListeningPoint);
        sipProvider.addSipListener(this);
        if (logger.isDebugEnabled()) logger.debug("init() " + sipProvider);
    }

    public boolean isMonitored(String roomId) {
        synchronized (noteTakers) {
            boolean monitorExists = noteTakers.containsKey(roomId);
            if (logger.isDebugEnabled()) logger.debug("roomId " + roomId + " is monitored " + monitorExists);
            return noteTakers.containsKey(roomId);
        }
    }

    public void monitorConference(String roomId) {
        if (roomId == null) {
            logger.error("monitorConference roomId null");
            throw new IllegalArgumentException("roomId can not be null");
        }
        synchronized (noteTakers) {
            NoteTaker noteTaker = new NoteTaker(roomId);
            noteTaker.joinConference();
            noteTakers.put(roomId, noteTaker);
            if (logger.isDebugEnabled()) logger.debug("add " + noteTakers);
        }
    }

    public void unmonitorConference(String roomId) {
        if (roomId == null) {
            logger.error("unmonitorConference roomId null");
            throw new IllegalArgumentException("roomId can not be null");
        }
        synchronized (noteTakers) {
            NoteTaker noteTaker = (NoteTaker) noteTakers.get(roomId);
            if (noteTaker != null) {
                noteTaker.leaveConference();
                noteTakers.remove(roomId);
            }
        }
    }

    public void processResponse(ResponseEvent responseEvent) {
        if (responseEvent.getResponse().getStatusCode() == 200) {
            SIPClientTransaction sipClientTx = (SIPClientTransaction) responseEvent.getClientTransaction();
            if (sipClientTx != null && Request.INVITE.equals(sipClientTx.getMethod())) {
                try {
                    ClientTransaction inviteTid = sipProvider.getNewClientTransaction(sipClientTx.createAck());
                    inviteTid.sendRequest();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public void processRequest(RequestEvent requestEvent) {
    }

    public void processTimeout(TimeoutEvent timeoutEvent) {
    }

    public class NoteTaker {

        protected Logger log = Logger.getLogger(NoteTaker.class);

        private String roomId;

        private String callId;

        public NoteTaker(String roomId) {
            this.roomId = roomId;
            log.info("NoteTaker " + roomId);
        }

        public void joinConference() {
            log.info("joinConference()");
            try {
                String fromName = "notetaker";
                String fromSipAddress = "jmik.org";
                String fromDisplayName = "Conference Note Taker";
                String toSipAddress = peerIP;
                String toUser = roomId;
                String toDisplayName = roomId;
                SipURI fromAddress = addressFactory.createSipURI(fromName, fromSipAddress);
                Address fromNameAddress = addressFactory.createAddress(fromAddress);
                fromNameAddress.setDisplayName(fromDisplayName);
                FromHeader fromHeader = headerFactory.createFromHeader(fromNameAddress, "12345");
                SipURI toAddress = addressFactory.createSipURI(toUser, toSipAddress);
                Address toNameAddress = addressFactory.createAddress(toAddress);
                toNameAddress.setDisplayName(toDisplayName);
                ToHeader toHeader = headerFactory.createToHeader(toNameAddress, null);
                SipURI requestURI = addressFactory.createSipURI(toUser, peerIP + ":" + peerPort);
                ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
                ViaHeader viaHeader = headerFactory.createViaHeader(myIP, myPort, "udp", null);
                viaHeaders.add(viaHeader);
                ContentTypeHeader contentTypeHeader = headerFactory.createContentTypeHeader("application", "sdp");
                CallIdHeader callIdHeader = sipProvider.getNewCallId();
                callId = callIdHeader.getCallId();
                CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(1, Request.INVITE);
                MaxForwardsHeader maxForwards = headerFactory.createMaxForwardsHeader(70);
                Request request = messageFactory.createRequest(requestURI, Request.INVITE, callIdHeader, cSeqHeader, fromHeader, toHeader, viaHeaders, maxForwards);
                String contactHost = sipStack.getIPAddress();
                SipURI contactUrl = addressFactory.createSipURI(fromName, contactHost);
                contactUrl.setPort(myPort);
                SipURI contactURI = addressFactory.createSipURI(fromName, myIP);
                contactURI.setPort(myPort);
                Address contactAddress = addressFactory.createAddress(contactURI);
                contactAddress.setDisplayName(fromName);
                ContactHeader contactHeader = headerFactory.createContactHeader(contactAddress);
                request.addHeader(contactHeader);
                String sdpData = "v=0\r\n" + "o=username 0 0" + "IN IP4 " + myIP + "\r\n" + "s=The Funky Flow\r\n" + "c=IN IP4 " + myIP + "\r\n" + "t=0 0\r\n" + "m=audio 0 RTP/AVP 0 97 8 3\r\n" + "a=rtpmap:0 PCMU/8000\r\n" + "a=rtpmap:3 GSM/8000\r\n" + "a=rtpmap:8 PCMA/8000\r\n" + "a=rtpmap:97 iLBC/8000\r\n" + "a=fmtp:97 mode=30\r\n";
                request.setContent(sdpData, contentTypeHeader);
                if (logger.isDebugEnabled()) logger.debug("request\n:" + request + "\n");
                ClientTransaction inviteTid = sipProvider.getNewClientTransaction(request);
                inviteTid.sendRequest();
                log.info("joinConference invite sent");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public void leaveConference() {
            log.info("leaveConference()");
            try {
                String fromName = "notetaker";
                String fromSipAddress = "jmik.org";
                String fromDisplayName = "Conference Note Taker";
                String toSipAddress = peerIP;
                String toUser = roomId;
                String toDisplayName = roomId;
                SipURI fromAddress = addressFactory.createSipURI(fromName, fromSipAddress);
                Address fromNameAddress = addressFactory.createAddress(fromAddress);
                fromNameAddress.setDisplayName(fromDisplayName);
                FromHeader fromHeader = headerFactory.createFromHeader(fromNameAddress, "12345");
                SipURI toAddress = addressFactory.createSipURI(toUser, toSipAddress);
                Address toNameAddress = addressFactory.createAddress(toAddress);
                toNameAddress.setDisplayName(toDisplayName);
                ToHeader toHeader = headerFactory.createToHeader(toNameAddress, null);
                SipURI requestURI = addressFactory.createSipURI(toUser, peerIP + ":" + peerPort);
                ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
                ViaHeader viaHeader = headerFactory.createViaHeader(myIP, myPort, "udp", null);
                viaHeaders.add(viaHeader);
                ContentTypeHeader contentTypeHeader = headerFactory.createContentTypeHeader("application", "sdp");
                CallIdHeader callIdHeader = headerFactory.createCallIdHeader(callId);
                CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(3, Request.BYE);
                MaxForwardsHeader maxForwards = headerFactory.createMaxForwardsHeader(70);
                Request request = messageFactory.createRequest(requestURI, Request.BYE, callIdHeader, cSeqHeader, fromHeader, toHeader, viaHeaders, maxForwards);
                String contactHost = sipStack.getIPAddress();
                SipURI contactUrl = addressFactory.createSipURI(fromName, contactHost);
                contactUrl.setPort(myPort);
                SipURI contactURI = addressFactory.createSipURI(fromName, myIP);
                contactURI.setPort(myPort);
                Address contactAddress = addressFactory.createAddress(contactURI);
                contactAddress.setDisplayName(fromName);
                ContactHeader contactHeader = headerFactory.createContactHeader(contactAddress);
                request.addHeader(contactHeader);
                ClientTransaction inviteTid = sipProvider.getNewClientTransaction(request);
                inviteTid.sendRequest();
                log.info("leaveConference invite \n" + request);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void stateChanged(int oldState, Call call) {
        if (logger.isDebugEnabled()) logger.debug("stateChanged " + call);
        System.out.println("stateChanged " + call);
    }

    public void channelRemoved(ConferenceCall conferenceCall, Channel channel) {
        if (logger.isDebugEnabled()) logger.debug("channelRemoved " + channel);
        if (conferenceCall.getChannels().size() == 1) {
            unmonitorConference(conferenceCall.getRoomId());
            System.out.println("unmonitorConference " + conferenceCall.getRoomId());
        }
    }

    public void channelAdded(ConferenceCall conferenceCall, Channel channel) {
        if (logger.isDebugEnabled()) logger.debug("channelAdded " + channel);
        System.out.println("channelAdded " + channel);
    }

    public void callAttached(Call call) {
        if (logger.isDebugEnabled()) logger.debug("callAttached " + call);
        if (call instanceof ConferenceCall) {
            call.addListener(this);
            if (logger.isDebugEnabled()) logger.debug("callAttached " + call + " addListener " + this);
            monitorConference(((ConferenceCall) call).getRoomId());
        }
    }

    public void callDetached(Call call) {
        if (logger.isDebugEnabled()) logger.debug("callDetached " + call);
        if (call instanceof ConferenceCall) {
            call.removeListener(this);
            ConferenceCall confCall = (ConferenceCall) call;
            if (logger.isDebugEnabled()) logger.debug("callDetached " + confCall + " removeListener " + this);
            unmonitorConference(((ConferenceCall) call).getRoomId());
        }
    }

    public void processDialogTerminated(DialogTerminatedEvent arg0) {
        if (logger.isDebugEnabled()) logger.debug("processDialogTerminated");
    }

    public void processIOException(IOExceptionEvent arg0) {
        if (logger.isDebugEnabled()) logger.debug("processIOException");
    }

    public void processTransactionTerminated(TransactionTerminatedEvent arg0) {
        if (logger.isDebugEnabled()) logger.debug("processTransactionTerminated");
    }
}
