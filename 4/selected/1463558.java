package com.pbxworkbench.campaign.svc.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.pbxworkbench.campaign.domain.Callee;
import com.pbxworkbench.campaign.domain.Campaign;
import com.pbxworkbench.campaign.domain.CampaignId;
import com.pbxworkbench.campaign.domain.ICallStatus;
import com.pbxworkbench.campaign.domain.ICalleeGroupHome;
import com.pbxworkbench.campaign.domain.ICampaignHome;
import com.pbxworkbench.campaign.domain.IMessageHome;
import com.pbxworkbench.campaign.domain.IPbxProfileHome;
import com.pbxworkbench.campaign.domain.Message;
import com.pbxworkbench.campaign.domain.PbxProfile;
import com.pbxworkbench.campaign.domain.PbxProfileId;
import com.pbxworkbench.campaign.svc.ICampaignListener;
import com.pbxworkbench.campaign.svc.ICampaignService;
import com.pbxworkbench.commons.CampaignRuntimeException;
import com.pbxworkbench.commons.ui.ProgressListener;
import com.pbxworkbench.pbx.CallOriginationAbortedCause;
import com.pbxworkbench.pbx.CallParameters;
import com.pbxworkbench.pbx.IChannelAddress;
import com.pbxworkbench.pbx.IChannelApplet;
import com.pbxworkbench.pbx.IPbxCall;
import com.pbxworkbench.pbx.IPbxCallObserver;
import com.pbxworkbench.pbx.IPbxProvider;
import com.pbxworkbench.pbx.PbxProviderException;
import com.pbxworkbench.pbx.PbxProviderFactory;
import com.pbxworkbench.pbx.applet.PlayMessageChannelApplet;
import com.pbxworkbench.pbx.applet.RecordMessageChannelApplet;
import com.pbxworkbench.pbx.applet.SendMessageChannelApplet;

public class CampaignServiceImpl implements ICampaignService {

    private static final Log LOG = LogFactory.getLog(CampaignServiceImpl.class);

    private ICampaignRequestProcessor requestProcessor;

    private ICampaignNotificationTopic notificationTopic;

    private ICampaignDao dao = new CampaignDaoImpl();

    public CampaignServiceImpl(ICampaignRequestProcessor requestProcessor, ICampaignDao campaignDao, ICampaignNotificationTopic notificationTopic) {
        this.requestProcessor = requestProcessor;
        this.dao = campaignDao;
        this.notificationTopic = notificationTopic;
    }

    public void startCampaign(CampaignId id) {
        AbstractCampaignRequest request = new CampaignStartRequest(id);
        requestProcessor.process(request);
    }

    public void stopCampaign(CampaignId id) {
        AbstractCampaignRequest request = new CampaignStopRequest(id);
        requestProcessor.process(request);
    }

    public void pauseCampaign(CampaignId id) {
        AbstractCampaignRequest request = new CampaignPauseRequest(id);
        requestProcessor.process(request);
    }

    public void addCampaignListener(CampaignId campaignId, ICampaignListener listener) {
        notificationTopic.subscribe(campaignId, listener);
    }

    public void callUserToRecordMessage(PbxProfileId pbxId, Message message, ProgressListener listener) {
        PbxProfile profile = dao.getPbxProfileHome().read(pbxId);
        LOG.debug("Calling user to Record Mesage " + profile + ", " + message);
        if (message.getPbxProfile() == null) {
            message.setPbxProfile(profile);
        } else {
            if (!message.getPbxProfile().getId().equals(profile.getId())) {
                throw new CampaignRuntimeException("messages current pbx profile does not equal given profile", null);
            }
        }
        dao.getMessageHome().save(message);
        IPbxProvider provider = createPbxProvider(profile);
        IChannelAddress address = provider.getAdministratorAddress();
        RecordMessageChannelApplet recorderApplet = new RecordMessageChannelApplet(message);
        CallParameters callParameters = new CallParameters(address, recorderApplet, "pbxworkbench", "0000");
        IPbxCall call = provider.createCall(callParameters);
        IPbxCallObserver observer = new MyMessageRecorderCallObserver(address, recorderApplet, listener);
        call.addObserver(observer);
        call.connect();
    }

    public void placeVerificationCallToAdmin(PbxProfile pbxProfile, final ProgressListener listener) {
        LOG.debug("place verification call to admin " + pbxProfile);
        IPbxProvider provider = createPbxProvider(pbxProfile);
        IPbxCall call = provider.createTestCall();
        IPbxCallObserver observer = new MyTestCallProgressObserver(provider.getAdministratorAddress(), listener);
        call.addObserver(observer);
        call.connect();
    }

    public void placeTestCall(Campaign campaign, String dialString, ProgressListener callListener) {
        LOG.debug("place test campaign call " + campaign);
        IPbxProvider provider = createPbxProvider(campaign.getPbxProfile());
        IChannelAddress channelAddress = provider.createChannelAddress(campaign.getExtContext(), dialString);
        IChannelApplet channelApplet = new SendMessageChannelApplet(campaign.getMessage());
        CallParameters callParameters = new CallParameters(channelAddress, channelApplet, "", campaign.getCallerIdString());
        IPbxCall pbxCall = provider.createCall(callParameters);
        IPbxCallObserver observer = new MyTestCallProgressObserver(channelAddress, callListener);
        pbxCall.addObserver(observer);
        pbxCall.connect();
    }

    public String testConnection(PbxProfile profile) {
        LOG.debug("test connection " + profile);
        IPbxProvider provider = createPbxProvider(profile);
        try {
            provider.testConnection();
            return "successfully connected";
        } catch (PbxProviderException e) {
            return e.getProviderExceptionCause().toString();
        }
    }

    public void placePlaybackCall(Message message, final ProgressListener callListener) {
        LOG.debug("place playback call " + message);
        IPbxProvider provider = createPbxProvider(message.getPbxProfile());
        CallParameters callParameters = new CallParameters(provider.getAdministratorAddress(), new PlayMessageChannelApplet(message), "", "");
        IPbxCall call = provider.createCall(callParameters);
        IPbxCallObserver observer = new MyTestCallProgressObserver(callParameters.getChannelAddress(), callListener);
        call.addObserver(observer);
        call.connect();
    }

    private IPbxProvider createPbxProvider(PbxProfile pbxProfile) {
        return PbxProviderFactory.newInstance(pbxProfile);
    }

    private class MyMessageRecorderCallObserver implements IPbxCallObserver {

        private ProgressListener recordingListener;

        private IChannelAddress address;

        private RecordMessageChannelApplet recorderApplet;

        public MyMessageRecorderCallObserver(IChannelAddress address, RecordMessageChannelApplet recorderApplet, ProgressListener recordingListener) {
            this.address = address;
            this.recordingListener = recordingListener;
            this.recorderApplet = recorderApplet;
        }

        public void onHangup() {
            if (recorderApplet.isRecorded()) {
                recordingListener.onCompleted("message recorded by " + address);
            } else {
                recordingListener.onAborted("no message recorded by " + address);
            }
        }

        public void onAnswer() {
            recordingListener.onStatusChanged("answered " + address);
        }

        public void onOriginationAborted(CallOriginationAbortedCause cause) {
            recordingListener.onAborted("origination failure address " + address + ", cause " + cause);
        }

        public void onCallStart() {
            recordingListener.onStarted("calling " + address);
        }
    }

    public ICallStatus getCallStatus(CampaignId campaignId, Callee callee) {
        return requestProcessor.getCallStatus(campaignId, callee);
    }

    public String getCampaignStatus(CampaignId campaignId) {
        return requestProcessor.getCampaignStatus(campaignId);
    }

    private class MyTestCallProgressObserver implements IPbxCallObserver {

        private ProgressListener testCallListener;

        private IChannelAddress address;

        public MyTestCallProgressObserver(IChannelAddress address, ProgressListener testCallListener) {
            this.testCallListener = testCallListener;
            this.address = address;
        }

        public void onHangup() {
            testCallListener.onCompleted("hungup " + address);
        }

        public void onAnswer() {
            testCallListener.onStatusChanged("answered " + address);
        }

        public void onOriginationAborted(CallOriginationAbortedCause cause) {
            testCallListener.onAborted("origination failure " + address + ", cause " + cause);
        }

        public void onCallStart() {
            testCallListener.onStarted("calling " + address);
        }
    }

    public ICampaignHome getCampaignHome() {
        return dao.getCampaignHome();
    }

    public IMessageHome getMessageHome() {
        return dao.getMessageHome();
    }

    public IPbxProfileHome getPbxProfileHome() {
        return dao.getPbxProfileHome();
    }

    public ICalleeGroupHome getCalleeGroupHome() {
        return dao.getCalleeGroupHome();
    }
}
