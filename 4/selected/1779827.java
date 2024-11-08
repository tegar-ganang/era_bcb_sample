package com.pbxworkbench.pbx.asterisk;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.commons.logging.Log;
import org.asteriskjava.live.AsteriskChannel;
import org.asteriskjava.live.AsteriskServer;
import org.asteriskjava.live.CallerId;
import org.asteriskjava.live.ChannelState;
import org.asteriskjava.live.DefaultAsteriskServer;
import org.asteriskjava.live.LiveException;
import org.asteriskjava.live.ManagerCommunicationException;
import org.asteriskjava.live.OriginateCallback;
import org.asteriskjava.manager.AuthenticationFailedException;
import org.asteriskjava.manager.ManagerConnection;
import org.asteriskjava.manager.ManagerConnectionFactory;
import org.asteriskjava.manager.TimeoutException;
import com.pbxworkbench.campaign.domain.PbxProfile;
import com.pbxworkbench.pbx.CallOriginationAbortedCause;
import com.pbxworkbench.pbx.CallParameters;
import com.pbxworkbench.pbx.ChannelAppletServer;
import com.pbxworkbench.pbx.IPbxCall;
import com.pbxworkbench.pbx.IPbxCallObserver;
import com.pbxworkbench.pbx.IPbxService;
import com.pbxworkbench.pbx.PbxRuntimeException;

public class AsteriskPbxService implements IPbxService {

    private static final Log LOG = org.apache.commons.logging.LogFactory.getLog(AsteriskPbxService.class);

    public AsteriskServer asteriskManager;

    private String applicationServerRoot;

    private String application;

    private String asteriskHostname;

    private String asteriskUsername;

    private String asteriskPassword;

    private ManagerConnection managerConnection;

    private BlockingQueue<MyCall> callQueue = new LinkedBlockingQueue<MyCall>();

    public AsteriskPbxService(PbxProfile profile) {
        this.asteriskHostname = profile.getAddress();
        this.asteriskUsername = profile.getUsername();
        this.asteriskPassword = profile.getPassword();
        try {
            String hostAddress = profile.getPbxWorkbenchHostname();
            if (hostAddress == null) {
                hostAddress = InetAddress.getLocalHost().getHostAddress();
            }
            this.applicationServerRoot = "agi://" + hostAddress;
        } catch (UnknownHostException e) {
            throw new PbxRuntimeException("unable to deterine local host address", e);
        }
        this.application = "AGI";
        initialize();
    }

    public IPbxCall createCall(CallParameters callParameters) {
        return new MyCall(callParameters);
    }

    private void initialize() {
        try {
            Thread callOriginationServer = new Thread(new CallOriginationServer());
            callOriginationServer.start();
            login();
        } catch (Exception e) {
            throw new AsteriskPbxRuntimeException("could not initialize service", e);
        }
    }

    public void login() throws IllegalStateException, IOException, AuthenticationFailedException, TimeoutException, ManagerCommunicationException {
        ManagerConnectionFactory factory = new ManagerConnectionFactory(getAsteriskHostname(), getAsteriskUsername(), getAsteriskPassword());
        ManagerConnection c = factory.createManagerConnection();
        c.login();
        DefaultAsteriskServer manager = new DefaultAsteriskServer(c);
        manager.initialize();
        this.managerConnection = c;
        this.asteriskManager = manager;
    }

    public void logout() throws IOException, TimeoutException {
        managerConnection.logoff();
    }

    private String getAsteriskPassword() {
        return this.asteriskPassword;
    }

    private String getAsteriskUsername() {
        return this.asteriskUsername;
    }

    private String getAsteriskHostname() {
        return this.asteriskHostname;
    }

    private String getApplicationServerRoot() {
        return this.applicationServerRoot;
    }

    private String getApplication() {
        return this.application;
    }

    private class MyCall implements IPbxCall, PropertyChangeListener, OriginateCallback {

        private IPbxCallObserver observer;

        private CallParameters callParameters;

        public MyCall(CallParameters callParameters) {
            this.callParameters = callParameters;
        }

        public void addObserver(IPbxCallObserver observer) {
            this.observer = observer;
        }

        public void connect() {
            try {
                ChannelAppletServer.getInstance().registerApplet(callParameters.getChannelApplet());
                callQueue.put(this);
            } catch (InterruptedException e) {
                throw new AsteriskPbxRuntimeException("could not queue call for origination", e);
            }
        }

        public void propertyChange(PropertyChangeEvent evt) {
            if (!(evt.getSource() instanceof AsteriskChannel)) {
                return;
            }
            AsteriskChannel channel = (AsteriskChannel) evt.getSource();
            ChannelState state;
            synchronized (channel) {
                state = channel.getState();
            }
            LOG.debug("pbxWorkbenchChannel state change to " + state);
            if (ChannelState.UP.equals(state)) {
                observer.onAnswer();
                return;
            }
            if (ChannelState.HUNGUP.equals(state)) {
                observer.onHangup();
                return;
            }
            return;
        }

        protected void originate() {
            try {
                LOG.debug("originating call from " + getAddress() + " to " + getApplicationData() + " callerId " + getCallerId());
                observer.onCallStart();
                asteriskManager.originateToApplicationAsync(getAddress(), getApplication(), getApplicationData(), getTimeout(), getCallerId(), getCallVariables(), this);
            } catch (ManagerCommunicationException e) {
                observer.onHangup();
            }
        }

        private String getAddress() {
            return callParameters.getChannelAddress().getAddress();
        }

        private Map<String, String> getCallVariables() {
            return new HashMap<String, String>();
        }

        private CallerId getCallerId() {
            return new CallerId(callParameters.getCallingName(), callParameters.getCallingNumber());
        }

        private long getTimeout() {
            return callParameters.getTimeout();
        }

        private String getApplicationData() {
            return getApplicationServerRoot() + callParameters.getChannelApplet().getChannelAppletLocation().getStringRepresentation();
        }

        public void onSuccess(AsteriskChannel channel) {
            LOG.debug("onSuccess " + channel);
            observer.onAnswer();
            channel.addPropertyChangeListener(AsteriskChannel.PROPERTY_STATE, this);
        }

        public void onNoAnswer(AsteriskChannel channel) {
            LOG.debug("onNoAnswer " + channel);
            observer.onOriginationAborted(CallOriginationAbortedCause.NO_ANSWER);
        }

        public void onBusy(AsteriskChannel channel) {
            LOG.debug("onBusy" + channel);
            observer.onOriginationAborted(CallOriginationAbortedCause.BUSY);
        }

        public void onFailure(LiveException liveException) {
            LOG.debug("onFailure " + liveException);
            observer.onOriginationAborted(CallOriginationAbortedCause.FAILURE);
        }
    }

    private class CallOriginationServer implements Runnable {

        public void run() {
            while (true) {
                try {
                    MyCall call = callQueue.take();
                    LOG.debug("take new call");
                    call.originate();
                } catch (Exception e) {
                    LOG.debug(e);
                }
            }
        }
    }
}
