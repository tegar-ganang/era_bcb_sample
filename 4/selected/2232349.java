package com.quikj.ace.web.client.presenter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Logger;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.i18n.shared.DateTimeFormat;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Widget;
import com.quikj.ace.messages.vo.app.Message;
import com.quikj.ace.messages.vo.app.ResponseMessage;
import com.quikj.ace.messages.vo.talk.CallPartyElement;
import com.quikj.ace.messages.vo.talk.CalledNameElement;
import com.quikj.ace.messages.vo.talk.CallingNameElement;
import com.quikj.ace.messages.vo.talk.CannedMessageSummaryElement;
import com.quikj.ace.messages.vo.talk.ConferenceInformationMessage;
import com.quikj.ace.messages.vo.talk.DisconnectMessage;
import com.quikj.ace.messages.vo.talk.DisconnectReasonElement;
import com.quikj.ace.messages.vo.talk.HtmlElement;
import com.quikj.ace.messages.vo.talk.JoinRequestMessage;
import com.quikj.ace.messages.vo.talk.MailElement;
import com.quikj.ace.messages.vo.talk.MediaElementInterface;
import com.quikj.ace.messages.vo.talk.MediaElements;
import com.quikj.ace.messages.vo.talk.RTPMessage;
import com.quikj.ace.messages.vo.talk.SendMailRequestMessage;
import com.quikj.ace.messages.vo.talk.SetupRequestMessage;
import com.quikj.ace.messages.vo.talk.SetupResponseMessage;
import com.quikj.ace.messages.vo.talk.TypingElement;
import com.quikj.ace.web.client.ApplicationController;
import com.quikj.ace.web.client.AudioUtils;
import com.quikj.ace.web.client.ChatSessionInfo;
import com.quikj.ace.web.client.ChatSessionInfo.ChatStatus;
import com.quikj.ace.web.client.ChatSettings;
import com.quikj.ace.web.client.ClientProperties;
import com.quikj.ace.web.client.EmailTranscriptInfo;
import com.quikj.ace.web.client.Images;
import com.quikj.ace.web.client.Notifier;
import com.quikj.ace.web.client.SessionInfo;
import com.quikj.ace.web.client.comm.CommunicationsFactory;
import com.quikj.ace.web.client.comm.ResponseListener;
import com.quikj.ace.web.client.view.ChatPanel;
import com.quikj.ace.web.client.view.UserContact;
import com.quikj.ace.web.client.view.ViewUtils;
import com.quikj.ace.web.client.view.desktop.DesktopChatPanel;

/**
 * @author amit
 * 
 */
public class ChatSessionPresenter {

    private static final boolean DEFAULT_COOKIE_IN_SUBJECT = false;

    private static final boolean DEFAULT_VERBOSE_MESSAGES = false;

    private static final int TYPING_TIMEOUT = 30000;

    private static final long TYPING_SEND_DELAY = 30000L;

    private ChatPanel view;

    private List<CallPartyElement> otherParties = new ArrayList<CallPartyElement>();

    private Logger logger;

    private ChatSessionInfo chatInfo;

    private Date conversationStart = new Date();

    private Date conversationDisc;

    private int setupRequestId = -1;

    private long joinSessionId = -1L;

    private long xferredFromId = -1L;

    private Date lastTypingTime = null;

    private Timer typingTimer = null;

    private String systemUser = ApplicationController.getMessages().ChatSessionPresenter_systemUser();

    public ChatSessionPresenter() {
        logger = Logger.getLogger(getClass().getName());
    }

    public void showChat() {
        if (ApplicationController.getInstance().isOperator()) {
            ChatSessionPresenter presenter = UserPanelPresenter.getCurrentInstance().getCurrentChatPresenter();
            if (presenter != null) {
                long oldSessionId = presenter.getSessionId();
                if (!UserConversationsPresenter.getCurrentInstance().chatExists(oldSessionId)) {
                    presenter.dispose(DisconnectReasonElement.NORMAL_DISCONNECT, null);
                }
            }
            UserPanelPresenter.getCurrentInstance().showChat((Widget) view);
        } else {
            MainPanelPresenter.getInstance().attachToMainPanel((Widget) view);
        }
    }

    public String getOtherPartyNames() {
        return ViewUtils.formatNames(otherParties);
    }

    public int getNumParties() {
        return otherParties.size();
    }

    public Long getSessionId() {
        return chatInfo == null ? null : chatInfo.getSessionId();
    }

    public void chatClosed() {
        dispose(DisconnectReasonElement.NORMAL_DISCONNECT, null);
        if (ApplicationController.getInstance().isOperator()) {
            UserPanelPresenter.getCurrentInstance().showConversations();
        }
    }

    public void dispose(int reasonCode, String reasonText) {
        if (chatInfo.getStatus() == ChatStatus.CONNECTED) {
            userDisconnected(reasonCode, reasonText);
        }
        Map<?, ?> chats = (Map<?, ?>) SessionInfo.getInstance().get(SessionInfo.CHAT_LIST);
        chats.remove(chatInfo.getSessionId());
        if (typingTimer != null) {
            cancelTyping();
        }
        if (view != null) {
            if (ApplicationController.getInstance().isOperator()) {
                UserPanelPresenter.getCurrentInstance().removeChat((Widget) view);
                UserConversationsPresenter.getCurrentInstance().removeChat(chatInfo.getSessionId());
            }
            view.dispose();
            view = null;
        }
    }

    private ChatPanel createView(CallPartyElement otherParty) {
        CannedMessageSummaryElement[] cannedMessages = null;
        if (ApplicationController.getInstance().isOperator()) {
            cannedMessages = (CannedMessageSummaryElement[]) SessionInfo.getInstance().get(SessionInfo.CANNED_MESSAGES);
        }
        boolean show = true;
        if (!ApplicationController.getInstance().isOperator()) {
            show = ClientProperties.getInstance().getBooleanValue(ClientProperties.SHOW_OPERATOR_DETAILS, true);
        }
        CallPartyElement me = (CallPartyElement) SessionInfo.getInstance().get(SessionInfo.USER_INFO);
        DesktopChatPanel chat = new DesktopChatPanel(ViewUtils.formatName(me), otherParty, cannedMessages, ApplicationController.getInstance().isOperator(), show);
        chat.setPresenter(this);
        return chat;
    }

    public void setupOutboundChat(String called, String transferId, String transferFrom, String transcript, long joinSessionId, ChatPanel transferView, boolean userTransfer) {
        SetupRequestMessage message = new SetupRequestMessage();
        CallPartyElement cp = (CallPartyElement) SessionInfo.getInstance().get(SessionInfo.USER_INFO);
        CallingNameElement calling = new CallingNameElement();
        calling.setCallParty(cp);
        message.setCallingNameElement(calling);
        CalledNameElement cledElement = new CalledNameElement();
        CallPartyElement cpElement = new CallPartyElement();
        cpElement.setName(called);
        cledElement.setCallParty(cpElement);
        message.setCalledNameElement(cledElement);
        this.joinSessionId = joinSessionId;
        if (transferView != null) {
            view = transferView;
        }
        if (otherParties.size() > 0) {
            otherParties.set(0, cpElement);
        } else {
            otherParties.add(cpElement);
        }
        if (xferredFromId >= 0L) {
            if (transferId != null) {
                message.setTransferId(transferId);
            }
            if (transferFrom != null) {
                message.setTransferFrom(transferFrom);
            }
            message.setUserTransfer(userTransfer);
            if (transcript != null) {
                MediaElements elements = new MediaElements();
                message.setMedia(elements);
                HtmlElement elem = new HtmlElement();
                elements.addMediaElement(elem);
                elem.setHtml("<hr><blockquote>" + transcript + "</blockquote><hr>");
            }
        } else if (joinSessionId >= 0L) {
            view.appendToConveration(systemUser, timestamp(), ApplicationController.getMessages().ChatSessionPresenter_addingUserToConference(called));
            message.setUserConference(true);
        } else {
            if (!ApplicationController.getInstance().isOperator()) {
                systemUser = called;
            }
            MessageBoxPresenter.getInstance().show(ApplicationController.getMessages().ChatSessionPresenter_chatSetup(), ApplicationController.getMessages().ChatSessionPresenter_settingUpChatWithParty(called) + " ... ", (String) Images.USER_CHATTING_MEDIUM, false);
        }
        CommunicationsFactory.getServerCommunications().sendRequest(message, Message.CONTENT_TYPE_XML, true, 100000L, new ResponseListener() {

            @Override
            public void timeoutOccured(int requestId) {
                MessageBoxPresenter.getInstance().hide();
                requestId = -1;
                abortOutboundChat(ApplicationController.getMessages().ChatSessionPresenter_noResponseFromServer());
            }

            @Override
            public void responseReceived(int requestId, String contentType, ResponseMessage message) {
                MessageBoxPresenter.getInstance().hide();
                setupRequestId = requestId;
                SetupResponseMessage rsp = (SetupResponseMessage) message.getMessage();
                if (rsp.getCalledParty() != null) {
                    otherParties.set(0, rsp.getCalledParty().getCallParty());
                }
                switch(message.getStatus()) {
                    case SetupResponseMessage.ACK:
                        handleReceivedAck(rsp);
                        break;
                    case SetupResponseMessage.ALERTING:
                        handleReceivedAlerting(rsp);
                        break;
                    case SetupResponseMessage.PROG:
                        handleReceivedProgress(requestId, rsp);
                        break;
                    case SetupResponseMessage.CONNECT:
                        handleReceivedConnect(requestId, contentType, rsp);
                        break;
                    case SetupResponseMessage.BUSY:
                        handleNotConnected(requestId, ApplicationController.getMessages().ChatSessionPresenter_busyResponse());
                        break;
                    case SetupResponseMessage.NOANS:
                        handleNotConnected(requestId, ApplicationController.getMessages().ChatSessionPresenter_noAnswerResponse());
                        break;
                    case SetupResponseMessage.UNAVAILABLE:
                        handleNotConnected(requestId, ApplicationController.getMessages().ChatSessionPresenter_notAvailableResponse());
                        break;
                    case SetupResponseMessage.UNKNOWN:
                        handleNotConnected(requestId, ApplicationController.getMessages().ChatSessionPresenter_notOnlineResponse());
                        break;
                    default:
                        CommunicationsFactory.getServerCommunications().cancelRequest(requestId);
                        abortOutboundChat(ApplicationController.getMessages().ChatSessionPresenter_chatSetupFailed() + ": " + message.getReason());
                        break;
                }
            }
        });
    }

    private void startNewChat() {
        Map<Long, ChatSessionInfo> chats = (Map<Long, ChatSessionInfo>) SessionInfo.getInstance().get(SessionInfo.CHAT_LIST);
        chats.put(chatInfo.getSessionId(), chatInfo);
        if (xferredFromId >= 0L) {
            if (ApplicationController.getInstance().isOperator()) {
                UserConversationsPresenter.getCurrentInstance().replaceSession(xferredFromId, chatInfo.getSessionId());
                UserConversationsPresenter.getCurrentInstance().chatInformationChanged(chatInfo.getSessionId());
            }
        } else {
            if (ApplicationController.getInstance().isOperator()) {
                UserConversationsPresenter.getCurrentInstance().addNewChat(chatInfo);
            }
            view = createView(otherParties.get(0));
        }
    }

    private void abortOutboundChat(String msg) {
        if (view != null) {
            view.appendToConveration(systemUser, timestamp(), msg);
        } else {
            MessageBoxPresenter.getInstance().show(ApplicationController.getMessages().ChatSessionPresenter_chatSetup(), msg, (String) Images.DISCONNECTED_MEDIUM, true);
        }
        conversationDisc = new Date();
        if (joinSessionId >= 0L) {
            disposeJoinSession();
        } else {
            chatInfo.setStatus(ChatStatus.DISCONNECTED);
            String url = ClientProperties.getInstance().getStringValue(ClientProperties.VISITOR_CHAT_DECLINED_URL, null);
            if (!ApplicationController.getInstance().isOperator() && url != null) {
                ApplicationController.getInstance().disconnectExpected();
                Window.Location.assign(url);
                return;
            }
            Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {

                @Override
                public void execute() {
                    if (view != null) {
                        view.makeReadOnly();
                    }
                }
            });
            if (ApplicationController.getInstance().isOperator()) {
                UserConversationsPresenter.getCurrentInstance().chatDisconnected(chatInfo.getSessionId(), true);
            } else {
                ApplicationController.getInstance().disconnectExpected();
            }
        }
    }

    private long timestamp() {
        return new Date().getTime() + ApplicationController.getInstance().getTimeAdjustment();
    }

    public void processChatRequest(int reqId, String contentType, SetupRequestMessage msg) {
        chatInfo = new ChatSessionInfo(ChatSessionInfo.ChatStatus.SETUP_IN_PROGRESS);
        chatInfo.setChat(this);
        chatInfo.setSessionId(msg.getSessionId());
        otherParties.add(msg.getCallingNameElement().getCallParty());
        ChatSettings chatSettings = (ChatSettings) SessionInfo.getInstance().get(SessionInfo.CHAT_SETTINGS);
        if (chatSettings.isAutoAnswer()) {
            AudioUtils.getInstance().play(AudioUtils.RING);
            answerChat(reqId, contentType, msg);
        } else {
            AcceptTimer acceptTimer = new AcceptTimer(reqId, contentType);
            acceptTimer.schedule(60000);
            ConfirmationDialogPresenter.getInstance().show(ApplicationController.getMessages().ChatSessionPresenter_incomingChat(), ApplicationController.getMessages().ChatSessionPresenter_incomingChatFromParty(ViewUtils.formatUserInfo(otherParties.get(0))) + "<p>" + ApplicationController.getMessages().ChatSessionPresenter_doYouWantToAnswer(), (String) Images.USER_CHATTING_MEDIUM, new AcceptCallListener(chatInfo, reqId, contentType, msg, acceptTimer), false);
            Notifier.alert(ApplicationController.getMessages().ChatSessionPresenter_incomingChat());
        }
    }

    class AcceptTimer extends Timer {

        private int requestId;

        private String contentType;

        public AcceptTimer(int requestId, String contentType) {
            this.requestId = requestId;
            this.contentType = contentType;
        }

        @Override
        public void run() {
            ConfirmationDialogPresenter.getInstance().hide();
            Notifier.cancelAlert();
            SetupResponseMessage response = new SetupResponseMessage();
            response.setSessionId(chatInfo.getSessionId());
            CommunicationsFactory.getServerCommunications().sendResponse(requestId, SetupResponseMessage.NOANS, ApplicationController.getMessages().ChatSessionPresenter_noResponseFromUser(), contentType, response);
        }
    }

    class AcceptCallListener implements ConfirmationListener {

        private ChatSessionInfo chatInfo;

        private int reqId;

        private String contentType;

        private SetupRequestMessage msg;

        private AcceptTimer acceptTimer;

        public AcceptCallListener(ChatSessionInfo chatInfo, int reqId, String contentType, SetupRequestMessage msg, AcceptTimer acceptTimer) {
            this.chatInfo = chatInfo;
            this.reqId = reqId;
            this.contentType = contentType;
            this.msg = msg;
            this.acceptTimer = acceptTimer;
        }

        @Override
        public void yes() {
            Notifier.cancelAlert();
            acceptTimer.cancel();
            answerChat(reqId, contentType, msg);
        }

        @Override
        public void no() {
            Notifier.cancelAlert();
            acceptTimer.cancel();
            SetupResponseMessage response = new SetupResponseMessage();
            response.setSessionId(chatInfo.getSessionId());
            CommunicationsFactory.getServerCommunications().sendResponse(reqId, SetupResponseMessage.BUSY, ApplicationController.getMessages().ChatSessionPresenter_callerBusy(), contentType, response);
        }

        @Override
        public void cancel() {
        }
    }

    public void processRTPMessage(RTPMessage msg) {
        MediaElements media = msg.getMediaElements();
        String from = msg.getFrom();
        if (ClientProperties.getInstance().getBooleanValue(ClientProperties.HIDE_LOGIN_IDS, false)) {
            from = ViewUtils.parseFullName(from);
        }
        processMedia(media, from);
    }

    private void processMedia(MediaElements media, String from) {
        if (ApplicationController.getInstance().isOperator()) {
            UserPanelPresenter.getCurrentInstance().highlightChatEvent(chatInfo.getSessionId(), "rtp");
        }
        boolean playChime = false;
        int size = media.numMediaElements();
        for (int i = 0; i < size; i++) {
            MediaElementInterface element = media.elementAt(i);
            if (element instanceof HtmlElement) {
                HtmlElement text = (HtmlElement) element;
                cancelTyping();
                view.appendToConveration(from, timestamp(), text.getHtml());
                playChime = true;
            } else if (element instanceof TypingElement) {
                if (typingTimer != null) {
                    cancelTyping();
                }
                view.showTyping(from, timestamp());
                typingTimer = new Timer() {

                    @Override
                    public void run() {
                        cancelTyping();
                    }
                };
                typingTimer.schedule(TYPING_TIMEOUT);
            } else {
                logger.warning("Media element of type " + element.getClass().getName() + " is not supported");
            }
        }
        if (playChime) {
            AudioUtils.getInstance().play(AudioUtils.CHIME);
        }
    }

    public static void rtpReceived(RTPMessage msg) {
        Map<?, ?> chats = (Map<?, ?>) SessionInfo.getInstance().get(SessionInfo.CHAT_LIST);
        ChatSessionInfo chat = (ChatSessionInfo) chats.get(msg.getSessionId());
        if (chat == null) {
            Logger.getLogger(ChatSessionPresenter.class.getName()).warning("Received a RTP message for a session that does not exist");
            return;
        }
        ChatSessionPresenter presenter = chat.getChat();
        presenter.processRTPMessage(msg);
    }

    public void sendTextMessage(String text) {
        view.appendToConveration(null, timestamp(), text);
        lastTypingTime = null;
        RTPMessage rtp = new RTPMessage();
        rtp.setSessionId(chatInfo.getSessionId());
        MediaElements elements = new MediaElements();
        rtp.setMediaElements(elements);
        HtmlElement element = new HtmlElement();
        elements.addMediaElement(element);
        element.setHtml(text);
        CallPartyElement cp = (CallPartyElement) SessionInfo.getInstance().get(SessionInfo.USER_INFO);
        rtp.setFrom(ViewUtils.formatName(cp));
        CommunicationsFactory.getServerCommunications().sendRequest(rtp, Message.CONTENT_TYPE_XML, false, 0L, null);
    }

    public void userDisconnected(int reasonCode, String reasonText) {
        view.appendToConveration(systemUser, timestamp(), ApplicationController.getMessages().ChatSessionPresenter_chatHasEnded());
        view.makeReadOnly();
        disconnectOrTransfer(null, false, reasonCode, reasonText);
    }

    private void disconnectOrTransfer(String transferTo, boolean transferTranscript, int reasonCode, String reasonText) {
        conversationDisc = new Date();
        if (ApplicationController.getInstance().isOperator()) {
            EmailTranscriptInfo emailTrInfo = (EmailTranscriptInfo) SessionInfo.getInstance().get(SessionInfo.EMAIL_TRANSCRIPT_INFO);
            if (emailTrInfo != null && emailTrInfo.isEmailTranscript()) {
                emailTranscript(reasonCode, reasonText);
            }
        }
        DisconnectMessage disc = new DisconnectMessage();
        disc.setSessionId(chatInfo.getSessionId());
        CallPartyElement cp = (CallPartyElement) SessionInfo.getInstance().get(SessionInfo.USER_INFO);
        disc.setFrom(ViewUtils.formatName(cp));
        DisconnectReasonElement reason = new DisconnectReasonElement();
        disc.setDisconnectReason(reason);
        reason.setReasonCode(reasonCode);
        reason.setReasonText(reasonText);
        if (transferTo != null) {
            CalledNameElement called = new CalledNameElement();
            disc.setCalledInfo(called);
            CallPartyElement cparty = new CallPartyElement();
            called.setCallParty(cparty);
            cparty.setName(transferTo);
            if (transferTranscript) {
                disc.setTranscript(transferTranscript);
            }
        }
        CommunicationsFactory.getServerCommunications().sendRequest(disc, Message.CONTENT_TYPE_XML, false, 0L, null);
        chatInfo.setStatus(ChatStatus.DISCONNECTED);
        Map<?, ?> chats = (Map<?, ?>) SessionInfo.getInstance().get(SessionInfo.CHAT_LIST);
        ChatSessionInfo chat = (ChatSessionInfo) chats.get(chatInfo.getSessionId());
        chat.setStatus(ChatStatus.DISCONNECTED);
        if (ApplicationController.getInstance().isOperator()) {
            UserConversationsPresenter.getCurrentInstance().chatDisconnected(chatInfo.getSessionId(), false);
        } else {
            ApplicationController.getInstance().disconnectExpected();
        }
    }

    private void emailTranscript(int reasonCode, String reasonText) {
        EmailTranscriptInfo info = (EmailTranscriptInfo) SessionInfo.getInstance().get(SessionInfo.EMAIL_TRANSCRIPT_INFO);
        SendMailRequestMessage message = new SendMailRequestMessage();
        message.setReplyRequired(false);
        MailElement melement = new MailElement();
        message.setMailElement(melement);
        CallPartyElement cpElement = (CallPartyElement) SessionInfo.getInstance().get(SessionInfo.USER_INFO);
        Vector<String> replyToList = null;
        if (info.isFrom()) {
            replyToList = new Vector<String>();
            int count = 0;
            if (info.isFromSelf() && cpElement.getEmail() != null) {
                if (count++ == 0) {
                    melement.setFrom(cpElement.getEmail());
                }
                replyToList.addElement(cpElement.getEmail());
            }
            String[] cfglist = info.getFromList();
            for (String cfg : cfglist) {
                if (count++ == 0) {
                    melement.setFrom(cfg);
                }
                replyToList.addElement(cfg);
            }
            if (replyToList.size() > 0) {
                melement.setReplyTo(replyToList);
            }
        }
        if (info.isSendSelf() && cpElement.getEmail() != null) {
            melement.addTo(cpElement.getEmail());
        }
        if (info.isSendOthers()) {
            for (CallPartyElement other : otherParties) {
                if (other.getEmail() != null) {
                    melement.addTo(other.getEmail());
                }
            }
        }
        String[] toList = info.getToList();
        for (String to : toList) {
            melement.addTo(to);
        }
        melement.setSubype("html");
        if (ClientProperties.getInstance().getBooleanValue(ClientProperties.COOKIE_IN_TRANSCRIPT_SUBJECT, DEFAULT_COOKIE_IN_SUBJECT)) {
            String visitorId = null;
            for (CallPartyElement other : otherParties) {
                if (other.getUserName() == null) {
                    visitorId = other.getEndUserCookie();
                    if (!other.isCookiesEnabled()) {
                        visitorId = other.getIpAddress();
                    }
                    break;
                }
            }
            melement.setSubject(visitorId + " - " + ApplicationController.getMessages().ChatSessionPresenter_aceOperatorChatTranscript());
        } else {
            melement.setSubject(ApplicationController.getMessages().ChatSessionPresenter_aceOperatorChatTranscript());
        }
        StringBuffer buffer = new StringBuffer();
        buffer.append(ApplicationController.getMessages().ChatSessionPresenter_conversationStartTime() + " " + DateTimeFormat.getFormat(ClientProperties.getInstance().getStringValue(ClientProperties.DATE_TIME_FORMAT, ClientProperties.DEFAULT_DATE_TIME_FORMAT)).format(conversationStart));
        buffer.append("<br>");
        buffer.append(ApplicationController.getMessages().ChatSessionPresenter_conversationEndTime() + " " + (conversationDisc == null ? "--" : DateTimeFormat.getFormat(ClientProperties.getInstance().getStringValue(ClientProperties.DATE_TIME_FORMAT, ClientProperties.DEFAULT_DATE_TIME_FORMAT)).format(conversationDisc)));
        buffer.append("<br>");
        String thisParty = ViewUtils.formatName(cpElement);
        StringBuffer otherParty = new StringBuffer();
        int count = 0;
        for (CallPartyElement other : otherParties) {
            if (count++ > 0) {
                otherParty.append(", ");
            }
            otherParty.append(ViewUtils.formatName(other));
        }
        buffer.append(ApplicationController.getMessages().ChatSessionPresenter_conversationUsers(thisParty, otherParty.toString()));
        buffer.append("<br>");
        buffer.append("Disconnect status: ");
        if (reasonText != null && reasonText.length() > 0) {
            buffer.append(reasonText);
            buffer.append(" - ");
        }
        buffer.append(DisconnectReasonElement.DISCONNECT_CODE_DESCRIPTIONS[reasonCode]);
        buffer.append("<hr>");
        buffer.append(ApplicationController.getMessages().ChatSessionPresenter_chatTranscript());
        buffer.append(":<br>");
        buffer.append(view.getTranscript());
        melement.setBody(buffer.toString());
        CommunicationsFactory.getServerCommunications().sendRequest(message, Message.CONTENT_TYPE_XML, false, 0L, null);
    }

    public void serverDisconnected() {
        if (chatInfo.getStatus() == ChatSessionInfo.ChatStatus.SETUP_IN_PROGRESS) {
            CommunicationsFactory.getServerCommunications().cancelRequest(setupRequestId);
        }
        conversationDisc = new Date();
        if (view != null) {
            view.appendToConveration(systemUser, timestamp(), ApplicationController.getMessages().ChatSessionPresenter_chatEnded());
            view.makeReadOnly();
        }
        chatInfo.setStatus(ChatStatus.DISCONNECTED);
        if (typingTimer != null) {
            cancelTyping();
        }
    }

    public static void disconnectReceived(DisconnectMessage msg) {
        Map<?, ?> chats = (Map<?, ?>) SessionInfo.getInstance().get(SessionInfo.CHAT_LIST);
        ChatSessionInfo chat = (ChatSessionInfo) chats.get(msg.getSessionId());
        if (chat == null) {
            Logger.getLogger(ChatSessionPresenter.class.getName()).warning("Received a disconnect/transfer message for a session that does not exist");
            return;
        }
        ChatSessionPresenter presenter = chat.getChat();
        if (chat.getStatus() == ChatSessionInfo.ChatStatus.SETUP_IN_PROGRESS) {
            CommunicationsFactory.getServerCommunications().cancelRequest(presenter.getSetupRequestId());
        }
        String from = msg.getFrom();
        if (ClientProperties.getInstance().getBooleanValue(ClientProperties.HIDE_LOGIN_IDS, false)) {
            from = ViewUtils.parseFullName(from);
        }
        if (msg.getCalledInfo() != null) {
            presenter.processTransfer(msg, from);
        } else {
            presenter.processDisconnect(msg, from, chat);
            if (ApplicationController.getInstance().isOperator()) {
                UserConversationsPresenter.getCurrentInstance().chatDisconnected(msg.getSessionId(), true);
            }
        }
    }

    private void processTransfer(DisconnectMessage msg, String from) {
        boolean userTransfer = true;
        if (msg.getFrom().equals(ClientProperties.getInstance().getStringValue(ClientProperties.GROUP, ""))) {
            userTransfer = false;
        }
        if (userTransfer) {
            view.appendToConveration(systemUser, timestamp(), ApplicationController.getMessages().ChatSessionPresenter_chatTransferred(from));
        }
        view.appendToConveration(systemUser, timestamp(), ApplicationController.getMessages().ChatSessionPresenter_transferringToParty(ViewUtils.formatName(msg.getCalledInfo().getCallParty())));
        view.chatDisabled();
        String transcript = null;
        if (msg.isTranscript()) {
            transcript = view.getTranscript();
        }
        if (typingTimer != null) {
            cancelTyping();
        }
        Map<?, ?> chats = (Map<?, ?>) SessionInfo.getInstance().get(SessionInfo.CHAT_LIST);
        chats.remove(chatInfo.getSessionId());
        xferredFromId = chatInfo.getSessionId();
        setupOutboundChat(msg.getCalledInfo().getCallParty().getUserName(), msg.getTransferId(), msg.getFrom(), transcript, -1, null, userTransfer);
    }

    public void cancelTyping() {
        if (typingTimer != null) {
            typingTimer.cancel();
            typingTimer = null;
        }
        if (view != null) {
            view.hideTyping();
        }
    }

    private void processDisconnect(DisconnectMessage msg, String from, ChatSessionInfo chatSession) {
        conversationDisc = new Date();
        view.appendToConveration(systemUser, timestamp(), ApplicationController.getMessages().ChatSessionPresenter_chatDisconnected(from == null ? "Server" : from));
        view.makeReadOnly();
        chatInfo.setStatus(ChatStatus.DISCONNECTED);
        chatSession.setStatus(ChatStatus.DISCONNECTED);
        if (ApplicationController.getInstance().isOperator()) {
            EmailTranscriptInfo emailTrInfo = (EmailTranscriptInfo) SessionInfo.getInstance().get(SessionInfo.EMAIL_TRANSCRIPT_INFO);
            if (emailTrInfo != null && emailTrInfo.isEmailTranscript()) {
                emailTranscript(msg.getDisconnectReason().getReasonCode(), msg.getDisconnectReason().getReasonText());
            }
        } else {
            ApplicationController.getInstance().disconnectExpected();
        }
        if (typingTimer != null) {
            cancelTyping();
        }
    }

    public void cannedMessageSelected(int i, String message) {
        view.appendToConveration(null, timestamp(), ApplicationController.getMessages().ChatSessionPresenter_sentCannedMessage() + " - " + message);
        CannedMessageSummaryElement[] clist = (CannedMessageSummaryElement[]) SessionInfo.getInstance().get(SessionInfo.CANNED_MESSAGES);
        RTPMessage rtp = new RTPMessage();
        rtp.setSessionId(chatInfo.getSessionId());
        MediaElements elements = new MediaElements();
        rtp.setMediaElements(elements);
        elements.addMediaElement(clist[i]);
        CallPartyElement cp = (CallPartyElement) SessionInfo.getInstance().get(SessionInfo.USER_INFO);
        rtp.setFrom(ViewUtils.formatName(cp));
        CommunicationsFactory.getServerCommunications().sendRequest(rtp, Message.CONTENT_TYPE_XML, false, 0L, null);
    }

    private void answerChat(int reqId, String contentType, SetupRequestMessage msg) {
        SetupResponseMessage response = new SetupResponseMessage();
        response.setSessionId(chatInfo.getSessionId());
        CommunicationsFactory.getServerCommunications().sendResponse(reqId, SetupResponseMessage.CONNECT, ApplicationController.getMessages().ChatSessionPresenter_answered(), contentType, response);
        ChatSessionPresenter.this.chatInfo.setStatus(ChatSessionInfo.ChatStatus.CONNECTED);
        ChatSessionPresenter.this.startNewChat();
        view.appendToConveration(systemUser, timestamp(), ApplicationController.getMessages().ChatSessionPresenter_chatInformation() + ":<br/>" + ViewUtils.formatUserInfo(otherParties.get(0)));
        view.appendToConveration(systemUser, timestamp(), ApplicationController.getMessages().ChatSessionPresenter_connectedToParty(ViewUtils.formatName(otherParties.get(0))));
        if (msg.isUserTransfer()) {
            String fromParty = msg.getTransferFrom();
            if (fromParty == null) {
                fromParty = ApplicationController.getMessages().ChatSessionPresenter_privateParty();
            } else {
                if (ClientProperties.getInstance().getBooleanValue(ClientProperties.HIDE_LOGIN_IDS, false)) {
                    fromParty = ViewUtils.parseFullName(fromParty);
                }
            }
            view.appendToConveration(systemUser, timestamp(), ApplicationController.getMessages().ChatSessionPresenter_transferringFrom(fromParty) + ". ");
            if (msg.getMedia() != null) {
                view.appendToConveration(systemUser, timestamp(), ApplicationController.getMessages().ChatSessionPresenter_transcriptFollows() + ": ");
            }
        } else if (msg.isUserConference()) {
            view.appendToConveration(systemUser, timestamp(), ApplicationController.getMessages().ChatSessionPresenter_addingYouToConference());
        }
        if (msg.getMedia() != null) {
            processMedia(msg.getMedia(), systemUser);
        }
        view.chatEnabled();
        if (UserConversationsPresenter.getCurrentInstance().getActiveChatCount() == 1) {
            showChat();
        } else {
            UserPanelPresenter.getCurrentInstance().highlightChatEvent(chatInfo.getSessionId(), "new");
        }
    }

    public int getSetupRequestId() {
        return setupRequestId;
    }

    public List<String> getContactsList() {
        List<UserContact> contacts = UserContactsPresenter.getCurrentInstance().getOnlineContacts();
        List<String> ret = new ArrayList<String>();
        for (UserContact contact : contacts) {
            ret.add(ViewUtils.formatName(contact.getUser(), contact.getFullName()));
        }
        return ret;
    }

    public void addToConference(String user) {
        new ChatSessionPresenter().setupOutboundChat(ViewUtils.parseName(user), null, null, null, chatInfo.getSessionId(), view, false);
    }

    class TransferListener implements ConfirmationListener {

        private String transferTo;

        public TransferListener(String transferTo) {
            this.transferTo = transferTo;
        }

        @Override
        public void yes() {
            view.appendToConveration(systemUser, new Date().getTime(), ApplicationController.getMessages().ChatSessionPresenter_transferringChatTo(transferTo));
            view.makeReadOnly();
            disconnectOrTransfer(ViewUtils.parseName(transferTo), true, DisconnectReasonElement.NORMAL_DISCONNECT, null);
        }

        @Override
        public void no() {
            view.appendToConveration(systemUser, new Date().getTime(), ApplicationController.getMessages().ChatSessionPresenter_transferringChatTo(transferTo));
            view.makeReadOnly();
            disconnectOrTransfer(ViewUtils.parseName(transferTo), false, DisconnectReasonElement.NORMAL_DISCONNECT, null);
        }

        @Override
        public void cancel() {
        }
    }

    public void transferTo(String user) {
        ConfirmationDialogPresenter.getInstance().show(ApplicationController.getMessages().ChatSessionPresenter_chatTransfer(), ApplicationController.getMessages().ChatSessionPresenter_shouldIncludeTranscript(), Images.INFO_MEDIUM, new TransferListener(user), true);
    }

    private void joinSessions(String contentType) {
        JoinRequestMessage join = new JoinRequestMessage();
        join.addSession(joinSessionId);
        join.addSession(getSessionId());
        CommunicationsFactory.getServerCommunications().sendRequest(join, contentType, false, 100000L, new ResponseListener() {

            @Override
            public void timeoutOccured(int requestId) {
                MessageBoxPresenter.getInstance().show(ApplicationController.getMessages().ChatSessionPresenter_error(), ApplicationController.getMessages().ChatSessionPresenter_failedToAddUser(), MessageBoxPresenter.Severity.SEVERE, true);
                disposeJoinSession();
            }

            @Override
            public void responseReceived(int requestId, String contentType, ResponseMessage message) {
                disposeJoinSession();
            }
        });
    }

    public void replaceSession(long oldSessionId, long newSessionId) {
        Map<Long, ChatSessionInfo> chats = (Map<Long, ChatSessionInfo>) SessionInfo.getInstance().get(SessionInfo.CHAT_LIST);
        ChatSessionInfo session = chats.get(oldSessionId);
        session.setSessionId(newSessionId);
        chats.remove(oldSessionId);
        chats.put(newSessionId, session);
        if (ApplicationController.getInstance().isOperator()) {
            UserConversationsPresenter.getCurrentInstance().replaceSession(oldSessionId, newSessionId);
        }
    }

    private void handleReceivedAck(SetupResponseMessage rsp) {
        chatInfo = new ChatSessionInfo(ChatSessionInfo.ChatStatus.SETUP_IN_PROGRESS);
        chatInfo.setChat(ChatSessionPresenter.this);
        chatInfo.setSessionId(rsp.getSessionId());
        if (ChatSessionPresenter.this.joinSessionId < 0L) {
            startNewChat();
            showChat();
        }
        if (ClientProperties.getInstance().getBooleanValue(ClientProperties.VERBOSE_MESSAGES, DEFAULT_VERBOSE_MESSAGES)) {
            view.appendToConveration(systemUser, timestamp(), ApplicationController.getMessages().ChatSessionPresenter_initiatedChatWith(ViewUtils.formatName(otherParties.get(0))) + "...");
        }
        if (rsp.getMediaElements() != null) {
            processMedia(rsp.getMediaElements(), systemUser);
        }
        xferredFromId = -1L;
    }

    private void handleReceivedAlerting(SetupResponseMessage rsp) {
        if (ClientProperties.getInstance().getBooleanValue(ClientProperties.VERBOSE_MESSAGES, DEFAULT_VERBOSE_MESSAGES)) {
            view.appendToConveration(systemUser, timestamp(), ApplicationController.getMessages().ChatSessionPresenter_notifyingUser() + " " + ViewUtils.formatName(otherParties.get(0)));
        }
        if (joinSessionId < 0L) {
            view.setOtherPartyInfo(otherParties);
        }
        if (ChatSessionPresenter.this.joinSessionId < 0L && ApplicationController.getInstance().isOperator()) {
            UserConversationsPresenter.getCurrentInstance().chatInformationChanged(chatInfo.getSessionId());
        }
        if (rsp.getMediaElements() != null) {
            processMedia(rsp.getMediaElements(), systemUser);
        }
    }

    private void handleReceivedProgress(int requestId, SetupResponseMessage rsp) {
        CommunicationsFactory.getServerCommunications().changeTimeout(requestId, 120000L);
        if (rsp.getMediaElements() != null) {
            processMedia(rsp.getMediaElements(), systemUser);
        } else {
            view.appendToConveration(systemUser, timestamp(), ApplicationController.getMessages().ChatSessionPresenter_setupInProgress() + " ... ");
        }
    }

    private void handleReceivedConnect(int requestId, String contentType, SetupResponseMessage rsp) {
        chatInfo.setStatus(ChatStatus.CONNECTED);
        view.appendToConveration(systemUser, timestamp(), ApplicationController.getMessages().ChatSessionPresenter_connectedToParty(ViewUtils.formatName(otherParties.get(0))));
        if (joinSessionId < 0L) {
            view.chatEnabled();
        }
        CommunicationsFactory.getServerCommunications().cancelRequest(requestId);
        if (ApplicationController.getInstance().isOperator()) {
            UserConversationsPresenter.getCurrentInstance().chatConnected(rsp.getSessionId(), true);
        }
        if (rsp.getMediaElements() != null) {
            processMedia(rsp.getMediaElements(), systemUser);
        }
        if (ChatSessionPresenter.this.joinSessionId >= 0L) {
            joinSessions(contentType);
        }
    }

    private void handleNotConnected(int requestId, String msg) {
        CommunicationsFactory.getServerCommunications().cancelRequest(requestId);
        abortOutboundChat(msg);
    }

    public void changeUserInfo(ConferenceInformationMessage conf) {
        List<CallPartyElement> newOtherParties = new ArrayList<CallPartyElement>();
        int num = conf.numEndPointsInConference();
        for (int i = 0; i < num; i++) {
            CallPartyElement other = conf.elementAt(i);
            CallPartyElement me = (CallPartyElement) SessionInfo.getInstance().get(SessionInfo.USER_INFO);
            if (ApplicationController.getInstance().isOperator()) {
                if (other.getUserName() != null && me.getUserName().equals(other.getUserName())) {
                    continue;
                }
            } else {
                if (other.getUserName() == null && me.getFullName().equals(other.getFullName())) {
                    continue;
                }
            }
            newOtherParties.add(other);
        }
        List<CallPartyElement> added = partiesAdded(newOtherParties, otherParties);
        if (added.size() > 0) {
            partiesChanged(added, ApplicationController.getMessages().ChatSessionPresenter_joinedChat());
        }
        List<CallPartyElement> removed = partiesAdded(otherParties, newOtherParties);
        if (removed.size() > 0) {
            partiesChanged(removed, ApplicationController.getMessages().ChatSessionPresenter_leftChat());
        }
        otherParties = newOtherParties;
        if (otherParties.size() > 1) {
            view.transferSetEnabled(false);
        } else {
            view.transferSetEnabled(true);
        }
        view.setOtherPartyInfo(otherParties);
        if (ApplicationController.getInstance().isOperator()) {
            UserConversationsPresenter.getCurrentInstance().chatInformationChanged(chatInfo.getSessionId());
        }
    }

    private void partiesChanged(List<CallPartyElement> elements, String message) {
        StringBuffer buffer = new StringBuffer();
        if (elements.size() == 1) {
            buffer.append(ApplicationController.getMessages().ChatSessionPresenter_user());
        } else {
            buffer.append(ApplicationController.getMessages().ChatSessionPresenter_users());
        }
        buffer.append(" ");
        int count = 0;
        for (CallPartyElement e : elements) {
            if (count++ > 0) {
                buffer.append(", ");
            }
            buffer.append(ViewUtils.formatName(e));
        }
        buffer.append(" ");
        buffer.append(message);
        view.appendToConveration(systemUser, timestamp(), buffer.toString());
    }

    private List<CallPartyElement> partiesAdded(List<CallPartyElement> p1, List<CallPartyElement> p2) {
        List<CallPartyElement> added = new ArrayList<CallPartyElement>();
        for (CallPartyElement p1e : p1) {
            boolean found = false;
            for (CallPartyElement p2e : p2) {
                if (isSame(p1e, p2e)) {
                    found = true;
                }
            }
            if (!found) {
                added.add(p1e);
            }
        }
        return added;
    }

    private boolean isSame(CallPartyElement p1, CallPartyElement p2) {
        if (p1.getUserName() != null && p2.getUserName() != null) {
            return p1.getUserName().equals(p2.getUserName());
        } else if (p1.getFullName() != null && p2.getFullName() != null) {
            return p1.getFullName().equals(p2.getFullName());
        } else {
            return false;
        }
    }

    private void disposeJoinSession() {
        view = null;
        otherParties = null;
        chatInfo = null;
    }

    public Date getConversationDisc() {
        return conversationDisc;
    }

    public void typing() {
        boolean sendTyping = false;
        if (lastTypingTime == null) {
            sendTyping = true;
        } else if (new Date().getTime() - TYPING_SEND_DELAY > lastTypingTime.getTime()) {
            sendTyping = true;
        }
        if (sendTyping) {
            lastTypingTime = new Date();
            RTPMessage message = new RTPMessage();
            message.setSessionId(chatInfo.getSessionId());
            MediaElements elements = new MediaElements();
            TypingElement tyelem = new TypingElement();
            elements.addMediaElement(tyelem);
            message.setMediaElements(elements);
            CallPartyElement cp = (CallPartyElement) SessionInfo.getInstance().get(SessionInfo.USER_INFO);
            message.setFrom(ViewUtils.formatName(cp));
            CommunicationsFactory.getServerCommunications().sendRequest(message, Message.CONTENT_TYPE_XML, false, 0L, null);
        }
    }

    public void chatTerminated(String reasonText) {
        if (ApplicationController.getInstance().isOperator()) {
            EmailTranscriptInfo emailTrInfo = (EmailTranscriptInfo) SessionInfo.getInstance().get(SessionInfo.EMAIL_TRANSCRIPT_INFO);
            if (emailTrInfo != null && emailTrInfo.isEmailTranscript()) {
                int reasonCode = DisconnectReasonElement.NORMAL_DISCONNECT;
                if (reasonText != null) {
                    reasonCode = DisconnectReasonElement.CLIENT_EXIT;
                }
                emailTranscript(reasonCode, reasonText);
            }
        }
        DisconnectMessage disc = new DisconnectMessage();
        disc.setSessionId(chatInfo.getSessionId());
        CallPartyElement cp = (CallPartyElement) SessionInfo.getInstance().get(SessionInfo.USER_INFO);
        disc.setFrom(ViewUtils.formatName(cp));
        DisconnectReasonElement reason = new DisconnectReasonElement();
        disc.setDisconnectReason(reason);
        reason.setReasonCode(DisconnectReasonElement.NORMAL_DISCONNECT);
        if (reasonText != null) {
            reason.setReasonCode(DisconnectReasonElement.CLIENT_EXIT);
            reason.setReasonText(reasonText);
        }
        CommunicationsFactory.getServerCommunications().sendRequest(disc, Message.CONTENT_TYPE_XML, false, 0L, null);
        if (!ApplicationController.getInstance().isOperator()) {
            ApplicationController.getInstance().disconnectExpected();
        }
    }
}
