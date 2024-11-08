package com.quikj.application.web.talk.plugin;

import java.io.IOException;
import java.sql.Connection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Locale;
import com.quikj.ace.messages.vo.app.ResponseMessage;
import com.quikj.ace.messages.vo.talk.CallPartyElement;
import com.quikj.ace.messages.vo.talk.CalledNameElement;
import com.quikj.ace.messages.vo.talk.CallingNameElement;
import com.quikj.ace.messages.vo.talk.ChangePasswordRequestMessage;
import com.quikj.ace.messages.vo.talk.DisconnectMessage;
import com.quikj.ace.messages.vo.talk.DisconnectReasonElement;
import com.quikj.ace.messages.vo.talk.DndRequestMessage;
import com.quikj.ace.messages.vo.talk.DndResponseMessage;
import com.quikj.ace.messages.vo.talk.GroupActivityMessage;
import com.quikj.ace.messages.vo.talk.GroupMemberElement;
import com.quikj.ace.messages.vo.talk.JoinRequestMessage;
import com.quikj.ace.messages.vo.talk.JoinResponseMessage;
import com.quikj.ace.messages.vo.talk.MailElement;
import com.quikj.ace.messages.vo.talk.RegistrationRequestMessage;
import com.quikj.ace.messages.vo.talk.RegistrationResponseMessage;
import com.quikj.ace.messages.vo.talk.SendMailRequestMessage;
import com.quikj.ace.messages.vo.talk.SendMailResponseMessage;
import com.quikj.ace.messages.vo.talk.SetupRequestMessage;
import com.quikj.ace.messages.vo.talk.SetupResponseMessage;
import com.quikj.ace.messages.vo.talk.TalkMessageInterface;
import com.quikj.application.web.talk.plugin.accounting.CDRHandler;
import com.quikj.application.web.talk.plugin.accounting.CDRInterface;
import com.quikj.application.web.talk.plugin.accounting.LogoutCDR;
import com.quikj.application.web.talk.plugin.accounting.SessionDisconnectCDR;
import com.quikj.application.web.talk.plugin.accounting.SessionJoinCDR;
import com.quikj.application.web.talk.plugin.accounting.SessionLeaveCDR;
import com.quikj.application.web.talk.plugin.accounting.SessionSetupCDR;
import com.quikj.application.web.talk.plugin.accounting.SessionSetupResponseCDR;
import com.quikj.application.web.talk.plugin.accounting.SessionTransferCDR;
import com.quikj.application.web.talk.plugin.accounting.UnregisteredUserLoginCDR;
import com.quikj.client.raccess.AceRMIImpl;
import com.quikj.client.raccess.RemoteServiceInterface;
import com.quikj.server.app.EndPointInterface;
import com.quikj.server.framework.AceException;
import com.quikj.server.framework.AceLogger;
import com.quikj.server.framework.AceMailMessage;
import com.quikj.server.framework.AceMailService;
import com.quikj.server.framework.AceMessageInterface;
import com.quikj.server.framework.AceSQL;
import com.quikj.server.framework.AceSQLMessage;
import com.quikj.server.framework.AceSignalMessage;
import com.quikj.server.framework.AceThread;
import com.quikj.server.framework.AceTimer;
import com.quikj.server.framework.AceTimerMessage;

public class ServiceController extends AceThread implements RemoteServiceInterface {

    private Hashtable sessionList = new Hashtable();

    private Object sessionIdLock = new Object();

    private long sessionId = 1L;

    private AceSQL database;

    private Hashtable pendingDbOps = new Hashtable();

    private static ServiceController instance = null;

    private DbPing ping;

    private int pingTimerId = -1;

    private static final long PING_DATABASE_TIMER = 0;

    public ServiceController(Connection connection) throws IOException, AceException {
        super("TalkServiceController");
        FeatureFactory factory = new FeatureFactory();
        if (factory.init() == false) {
            throw new AceException(factory.getErrorMessage());
        }
        database = new AceSQL(connection);
        new GroupList();
        new EndPointList();
        ping = new DbPing(this, database);
        instance = this;
    }

    public static Locale getLocale(String localeString) {
        if (localeString == null) {
            return Locale.getDefault();
        }
        localeString = localeString.trim();
        if (localeString.toLowerCase().equals("default")) {
            return Locale.getDefault();
        }
        int languageIndex = localeString.indexOf('_');
        String language = null;
        if (languageIndex == -1) {
            return new Locale(localeString, "");
        } else {
            language = localeString.substring(0, languageIndex);
        }
        int countryIndex = localeString.indexOf('_', languageIndex + 1);
        String country = null;
        if (countryIndex == -1) {
            country = localeString.substring(languageIndex + 1);
            return new Locale(language, country);
        } else {
            country = localeString.substring(languageIndex + 1, countryIndex);
            String variant = localeString.substring(countryIndex + 1);
            return new Locale(language, country, variant);
        }
    }

    public static ServiceController Instance() {
        return instance;
    }

    private boolean addSession(SessionInfo session) {
        Long session_id = new Long(session.getSessionId());
        if (sessionList.get(session_id) == null) {
            sessionList.put(session_id, session);
            return true;
        } else {
            writeErrorMessage(session_id + " already exists", null);
            return false;
        }
    }

    private void cancelDbOperation(EndPointInterface endpoint) {
        synchronized (pendingDbOps) {
            DbOperationInterface op = (DbOperationInterface) pendingDbOps.get(endpoint);
            if (op != null) {
                pendingDbOps.remove(endpoint);
                op.cancel();
            }
        }
    }

    private void checkUnavailableUserTransfer(SessionInfo session, String unavail_username, EndPointInterface active_called_endpoint, SetupResponseMessage original_setupresp_message, int original_setupresp_status, String original_setupresp_reason) {
        DbUnavailableUserTransfer db_op = new DbUnavailableUserTransfer(unavail_username, session, active_called_endpoint, original_setupresp_message, original_setupresp_status, original_setupresp_reason, this, database);
        if (db_op.checkForTransfer() == false) {
            AceLogger.Instance().log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, "ServiceController.checkUnavailableUserTransfer() (TALK) -- Failure initiating DB check for unavailable user " + unavail_username + ", error : " + db_op.getLastError());
            if (session.getCallingEndPoint().sendEvent(new MessageEvent(MessageEvent.SETUP_RESPONSE, null, original_setupresp_status, original_setupresp_reason, null, null)) == false) {
                AceLogger.Instance().log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, "ServiceController.checkUnavailableUserTransfer() (TALK) -- Could not send setup response message to the endpoint " + session.getCallingEndPoint());
            }
            removeSession(original_setupresp_message.getSessionId());
            return;
        }
        pendingDbOps.put(session.getCallingEndPoint(), db_op);
        return;
    }

    public void dispose() {
        interruptWait(AceSignalMessage.SIGNAL_TERM, "disposed");
    }

    private void cleanup() {
        synchronized (pendingDbOps) {
            Enumeration ops = pendingDbOps.elements();
            while (ops.hasMoreElements() == true) {
                ((DbOperationInterface) (ops.nextElement())).cancel();
            }
            pendingDbOps.clear();
        }
        Enumeration e = sessionList.elements();
        while (e.hasMoreElements() == true) {
            SessionInfo session = (SessionInfo) e.nextElement();
            sendCDR(new SessionDisconnectCDR(session.getBillingId(), 0));
            int num = session.numEndPoints();
            for (int i = 0; i < num; i++) {
                EndPointInterface ep = session.elementAt(i);
                if (EndPointList.Instance().findRegisteredEndPointInfo(ep) == null) {
                    sendCDR(new LogoutCDR(ep.getIdentifier()));
                }
            }
        }
        EndPointList.Instance().clearEndpointList();
        EndPointList.Instance().dispose();
        GroupList.Instance().dispose();
        ping.cancel();
        if (pingTimerId != -1) {
            AceTimer.Instance().cancelTimer(pingTimerId);
            pingTimerId = -1;
        }
        database.dispose();
        FeatureFactory.getInstance().dispose();
        AceRMIImpl rs = AceRMIImpl.getInstance();
        if (rs != null) {
            rs.unregisterService("com.quikj.application.web.talk.plugin.ServiceController");
        }
        super.dispose();
        instance = null;
    }

    private SessionInfo findSession(long session) {
        return (SessionInfo) sessionList.get(new Long(session));
    }

    public void finishSetupResponse(SetupResponseMessage message, int status, String reason, EndPointInterface from) {
        SessionInfo session = findSession(message.getSessionId());
        if (session == null) {
            AceLogger.Instance().log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, "ServiceController.finishSetupResponse() (TALK) -- Could not find session with id " + message.getSessionId());
            return;
        }
        if (session.getCallingEndPoint().sendEvent(new MessageEvent(MessageEvent.SETUP_RESPONSE, from, status, reason, message, null)) == false) {
            AceLogger.Instance().log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, "ServiceController.finishSetupResponse() (TALK) -- Could not send setup response message to the endpoint " + session.getCallingEndPoint());
            removeSession(message.getSessionId());
            return;
        }
        generateSetupResponseCDR(message, status, from, session);
        if (status == SetupResponseMessage.CONNECT) {
            session.setConnected(true);
        } else {
            removeSession(message.getSessionId());
        }
    }

    public void generateSetupResponseCDR(SetupResponseMessage message, int status, EndPointInterface from, SessionInfo session) {
        String called_name = "unspecified";
        if (from != null) {
            UserElement info = EndPointList.Instance().findRegisteredUserData(from);
            called_name = from.getIdentifier();
        } else {
            if (message.getCalledParty() != null) {
                if (message.getCalledParty().getCallParty() != null) {
                    called_name = message.getCalledParty().getCallParty().getUserName();
                }
            }
        }
        SessionSetupResponseCDR cdr = new SessionSetupResponseCDR(session.getBillingId(), called_name, status);
        sendCDR(cdr);
    }

    private String generateRandomKey() {
        return "not so random";
    }

    public AceSQL getDatabase() {
        return database;
    }

    public long getNewSessionId() {
        long sess;
        synchronized (sessionIdLock) {
            sess = sessionId++;
        }
        return sess;
    }

    public String getRMIParam(String param) {
        String str = "logged-in:";
        if (param.startsWith(str) == true) {
            String name = param.substring(str.length());
            if (name.length() > 0) {
                EndPointInterface ep = EndPointList.Instance().findRegisteredEndPoint(name);
                if (ep == null) {
                    return "no";
                } else {
                    return "yes";
                }
            }
        }
        return null;
    }

    public void groupNotifyOfAvailabilityChange(EndPointInfo info, boolean available) {
        String username = info.getName();
        String[] notify_endpoints = EndPointList.Instance().notifyOfLoginLogout(username);
        if (notify_endpoints != null) {
            GroupActivityMessage ga = new GroupActivityMessage();
            com.quikj.ace.messages.vo.talk.GroupElement ge = new com.quikj.ace.messages.vo.talk.GroupElement();
            GroupMemberElement gm = new GroupMemberElement();
            gm.setUser(username);
            if (available == true) {
                gm.setOperation(GroupMemberElement.OPERATION_ADD_LIST);
                gm.setFullName(info.getUserData().getFullName());
                gm.setCallCount(info.getCallCount());
            } else {
                gm.setOperation(GroupMemberElement.OPERATION_REM_LIST);
            }
            ge.addElement(gm);
            ga.setGroup(ge);
            for (int i = 0; i < notify_endpoints.length; i++) {
                EndPointInterface endpoint = EndPointList.Instance().findRegisteredEndPoint(notify_endpoints[i]);
                if (endpoint != null) {
                    if (endpoint.sendEvent(new MessageEvent(MessageEvent.CLIENT_REQUEST_MESSAGE, null, ga, null)) == false) {
                        AceLogger.Instance().log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, "ServiceController.groupNotifyOfAvailabilityChange (TALK) -- Could not send group activity message to the endpoint " + endpoint);
                    }
                }
            }
        }
    }

    public void groupNotifyOfAvailabilityChange(EndPointInterface endpoint, boolean available) {
        EndPointInfo info = EndPointList.Instance().findRegisteredEndPointInfo(endpoint);
        if (info == null) {
            AceLogger.Instance().log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, getName() + "- ServiceController.groupNotifyOfAvailabilityChange() -- EndPoint not registered " + endpoint);
            return;
        }
        groupNotifyOfAvailabilityChange(info, available);
    }

    public void groupNotifyOfAvailabilityChange(String username, boolean available) {
        EndPointInfo info = EndPointList.Instance().findRegisteredEndPointInfo(username);
        if (info == null) {
            AceLogger.Instance().log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, getName() + "- ServiceController.groupNotifyOfAvailabilityChange() -- Username not registered " + username);
            return;
        }
        groupNotifyOfAvailabilityChange(info, available);
    }

    public void groupNotifyOfCallCountChange(EndPointInfo info) {
        String username = info.getName();
        String[] notify_endpoints = EndPointList.Instance().notifyOfBusyIdle(username);
        int call_count = info.getCallCount();
        if (call_count == -1) {
            AceLogger.Instance().log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, getName() + "- ServiceController.groupNotifyOfCallCountChange() -- Could not get new call count for user " + username + ", the user is not in the EndPointList");
            return;
        }
        if (notify_endpoints != null) {
            GroupActivityMessage ga = new GroupActivityMessage();
            com.quikj.ace.messages.vo.talk.GroupElement ge = new com.quikj.ace.messages.vo.talk.GroupElement();
            GroupMemberElement gm = new GroupMemberElement();
            gm.setUser(username);
            gm.setOperation(GroupMemberElement.OPERATION_MOD_LIST);
            gm.setCallCount(info.getCallCount());
            ge.addElement(gm);
            ga.setGroup(ge);
            for (int i = 0; i < notify_endpoints.length; i++) {
                EndPointInterface endpoint = EndPointList.Instance().findRegisteredEndPoint(notify_endpoints[i]);
                if (endpoint != null) {
                    if (endpoint.sendEvent(new MessageEvent(MessageEvent.CLIENT_REQUEST_MESSAGE, null, ga, null)) == false) {
                        AceLogger.Instance().log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, "ServiceController.groupNotifyOfCallCountChange(TALK) -- Could not send group activity message to the endpoint " + endpoint);
                    }
                }
            }
        }
    }

    public void groupNotifyOfCallCountChange(EndPointInterface endpoint) {
        EndPointInfo info = EndPointList.Instance().findRegisteredEndPointInfo(endpoint);
        if (info == null) {
            AceLogger.Instance().log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, getName() + "- ServiceController.groupNotifyOfCallCountChange() -- EndPoint not registered " + endpoint);
            return;
        }
        groupNotifyOfCallCountChange(info);
    }

    public void groupNotifyOfCallCountChange(String username) {
        EndPointInfo info = EndPointList.Instance().findRegisteredEndPointInfo(username);
        if (info == null) {
            AceLogger.Instance().log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, getName() + "- ServiceController.groupNotifyOfCallCountChange() -- Username not registered " + username);
            return;
        }
        groupNotifyOfCallCountChange(info);
    }

    private void pingDatabase() {
        ping.ping();
        pingTimerId = AceTimer.Instance().startTimer(1 * 3600 * 1000, PING_DATABASE_TIMER);
    }

    private void processChangePasswordRequest(ChangePasswordRequestMessage message, EndPointInterface from, int req_id) {
        String enc_old_password = message.getOldPassword();
        String old_password = null;
        if (enc_old_password != null) {
            old_password = enc_old_password;
        }
        String enc_new_password = message.getNewPassword();
        String new_password = null;
        if (enc_new_password != null) {
            new_password = enc_new_password;
        }
        DbChangeUserPassword cp = new DbChangeUserPassword(message.getUserName(), old_password, new_password, from, this, database, req_id, null);
        if (cp.initiate() == false) {
            AceLogger.Instance().log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, "ServiceController.processChangePasswordRequest() (TALK) -- Failure initiating change password for " + message.getUserName() + ", error : " + cp.getLastError());
            if (from.sendEvent(new MessageEvent(MessageEvent.CLIENT_RESPONSE_MESSAGE, null, ResponseMessage.INTERNAL_ERROR, java.util.ResourceBundle.getBundle("com.quikj.application.web.talk.plugin.language", getLocale(from.getParam("language"))).getString("Database_error_occured_while_trying_to_change_password"), new RegistrationResponseMessage(), null)) == false) {
                AceLogger.Instance().log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, "ServiceController.processChangePasswordRequest() (TALK) -- Could not send change password response message to the endpoint " + from);
            }
        }
    }

    private void processClientRequestMessage(TalkMessageInterface message, EndPointInterface from, int req_id) {
        if ((message instanceof JoinRequestMessage) == true) {
            processJoinRequest((JoinRequestMessage) message, from, req_id);
        } else if ((message instanceof ChangePasswordRequestMessage) == true) {
            processChangePasswordRequest((ChangePasswordRequestMessage) message, from, req_id);
        } else if ((message instanceof SendMailRequestMessage) == true) {
            processSendMailRequest((SendMailRequestMessage) message, from, req_id);
        } else if ((message instanceof DndRequestMessage) == true) {
            processDndRequest((DndRequestMessage) message, from, req_id);
        } else {
            AceLogger.Instance().log(AceLogger.WARNING, AceLogger.SYSTEM_LOG, "ServiceController.processClientRequestMessage() (TALK) -- Unknown message of type " + message.getClass().getName() + " received");
        }
    }

    private void processDisconnectRequest(DisconnectMessage message, EndPointInterface from) {
        if (from != null) {
            cancelDbOperation(from);
        }
        long session_id = message.getSessionId();
        SessionInfo session = findSession(session_id);
        if (session == null) {
            AceLogger.Instance().log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, "ServiceController.processDisconnectRequest() (TALK) -- Could not find session with id " + session_id);
            return;
        }
        if (from != null) {
            session.removeEndPoint(from);
        }
        SessionTransferCDR transfer_cdr = null;
        int num_ep = session.numEndPoints();
        if (message.getCalledInfo() != null) {
            if (num_ep != 1) {
                message.setCalledInfo(null);
                DisconnectReasonElement disc_reason = message.getDisconnectReason();
                if (disc_reason == null) {
                    disc_reason = new DisconnectReasonElement();
                    disc_reason.setReasonCode(DisconnectReasonElement.SERVER_DISCONNECT);
                }
                disc_reason.setReasonText(java.util.ResourceBundle.getBundle("com.quikj.application.web.talk.plugin.language", getLocale(session.elementAt(0).getParam("language"))).getString("Transfer_failed_-_cannot_transfer_conference_call"));
                message.setDisconnectReason(disc_reason);
            } else {
                transfer_cdr = new SessionTransferCDR(session.getBillingId(), message.getCalledInfo().getCallParty().getUserName());
                message.setTransferId(transfer_cdr.getIdentifier());
                if (from != null) {
                    if (message.getFrom() == null) {
                        UserElement udata = EndPointList.Instance().findRegisteredUserData(from);
                        if (udata != null) {
                            message.setFrom(udata.getName());
                        }
                    }
                }
            }
        }
        int code = 0;
        if (message.getDisconnectReason() != null) {
            code = message.getDisconnectReason().getReasonCode();
        }
        if (num_ep == 0) {
            removeSession(session_id);
            if (transfer_cdr == null) {
                sendCDR(new SessionDisconnectCDR(session.getBillingId(), code));
            } else {
                sendCDR(transfer_cdr);
            }
            if (EndPointList.Instance().findRegisteredEndPointInfo(from) == null) {
                sendCDR(new LogoutCDR(from.getIdentifier()));
            }
        } else if (num_ep == 1) {
            removeSession(session_id);
            if (transfer_cdr == null) {
                sendCDR(new SessionDisconnectCDR(session.getBillingId(), code));
            } else {
                sendCDR(transfer_cdr);
            }
            EndPointInterface party = session.elementAt(0);
            if (from != null) {
                if (EndPointList.Instance().findRegisteredEndPointInfo(from) == null) {
                    sendCDR(new LogoutCDR(from.getIdentifier()));
                }
            }
            if (EndPointList.Instance().findRegisteredEndPointInfo(party) == null) {
                sendCDR(new LogoutCDR(party.getIdentifier()));
            }
            if (party.sendEvent(new MessageEvent(MessageEvent.DISCONNECT_MESSAGE, from, message, null)) == false) {
                AceLogger.Instance().log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, "ServiceController.processDisconnectRequest() (TALK) -- Could not send disconnect message to the endpoint " + party);
                return;
            }
        } else {
            ConferenceBridge bridge = session.getConferenceBridge();
            if (bridge == null) {
                AceLogger.Instance().log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, "ServiceController.processDisconnectRequest() (TALK) -- Could not find conference bridge for a multi-party session");
                return;
            }
            bridge.removeEndPoint(from);
            sendCDR(new SessionLeaveCDR(session.getBillingId(), from.getIdentifier()));
            if (EndPointList.Instance().findRegisteredEndPointInfo(from) == null) {
                sendCDR(new LogoutCDR(from.getIdentifier()));
            }
            if (num_ep == 2) {
                EndPointInterface ep0 = session.elementAt(0);
                EndPointInterface ep1 = session.elementAt(1);
                ActionEvent event = new ActionEvent(null);
                ChangeEndPointAction change = new ChangeEndPointAction(session_id, ep1);
                event.addAction(change);
                if (ep0.sendEvent(event) == false) {
                    AceLogger.Instance().log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, "ServiceController.processDisconnectRequest() (TALK) -- Could not send action event to an endpoint: " + ep0);
                }
                event = new ActionEvent(null);
                change = new ChangeEndPointAction(session_id, ep0);
                event.addAction(change);
                if (ep1.sendEvent(event) == false) {
                    AceLogger.Instance().log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, "ServiceController.processDisconnectRequest() (TALK) -- Could not send action event to an endpoint: " + ep1);
                }
                bridge.interruptWait(AceSignalMessage.SIGNAL_TERM, "disposed");
                session.setConferenceBridge(null);
            }
        }
    }

    private void processDndRequest(DndRequestMessage message, EndPointInterface from, int req_id) {
        boolean enable = message.isEnable();
        EndPointInfo info = EndPointList.Instance().findRegisteredEndPointInfo(from);
        if (info == null) {
            AceLogger.Instance().log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, "ServiceController.processDndRequest() (TALK) -- Could not find the endpoint info " + from);
            if (from.sendEvent(new MessageEvent(MessageEvent.CLIENT_RESPONSE_MESSAGE, null, ResponseMessage.INTERNAL_ERROR, "Internal error", new DndResponseMessage(), null, req_id)) == false) {
                AceLogger.Instance().log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, "ServiceController.processDndRequest() (TALK) -- Could not send DndResponse to  " + from);
            }
            return;
        }
        info.setDnd(enable);
        if (enable == true) {
            groupNotifyOfAvailabilityChange(from, false);
        } else {
            groupNotifyOfAvailabilityChange(from, true);
            if (info.getCallCount() > 0) {
                groupNotifyOfCallCountChange(info);
            }
        }
        if (from.sendEvent(new MessageEvent(MessageEvent.CLIENT_RESPONSE_MESSAGE, null, ResponseMessage.OK, "OK", new DndResponseMessage(), null, req_id)) == false) {
            AceLogger.Instance().log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, "ServiceController.processDndRequest() (TALK) -- Could not send ok response message to the endpoint " + from);
        }
    }

    private void processJoinRequest(JoinRequestMessage message, EndPointInterface from, int req_id) {
        int num_sessions = message.getSessionListSize();
        if (num_sessions <= 1) {
            AceLogger.Instance().log(AceLogger.WARNING, AceLogger.SYSTEM_LOG, "ServiceController.processJoinRequest() (TALK) -- Received join request with only one session");
            if (from.sendEvent(new MessageEvent(MessageEvent.CLIENT_RESPONSE_MESSAGE, null, ResponseMessage.BAD_REQUEST, java.util.ResourceBundle.getBundle("com.quikj.application.web.talk.plugin.language", getLocale(from.getParam("language"))).getString("At_least_two_sessions_must_be_specified"), null, null, req_id)) == false) {
                AceLogger.Instance().log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, "ServiceController.processJoinRequest() (TALK) -- Could not send join response message to the endpoint " + from);
            }
            return;
        }
        long first_session_id = message.getSessionIdAt(0);
        SessionInfo first_session = findSession(first_session_id);
        if (first_session == null) {
            AceLogger.Instance().log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, "ServiceController.processJoinRequest() (TALK) -- Could not find session for a join request");
            if (from.sendEvent(new MessageEvent(MessageEvent.CLIENT_RESPONSE_MESSAGE, null, ResponseMessage.BAD_REQUEST, java.util.ResourceBundle.getBundle("com.quikj.application.web.talk.plugin.language", getLocale(from.getParam("language"))).getString("Could_not_get_information_on_the_first_session"), null, null, req_id)) == false) {
                AceLogger.Instance().log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, "ServiceController.processJoinRequest() (TALK) -- Could not send join response message to the endpoint " + from);
            }
            return;
        }
        ConferenceBridge bridge = null;
        SessionJoinCDR cdr = new SessionJoinCDR();
        for (int i = 0; i < num_sessions; i++) {
            long session = message.getSessionIdAt(i);
            SessionInfo session_info = findSession(session);
            if (session_info == null) {
                AceLogger.Instance().log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, "ServiceController.processJoinRequest() (TALK) -- Could not find session for a join request");
                if (from.sendEvent(new MessageEvent(MessageEvent.CLIENT_RESPONSE_MESSAGE, null, ResponseMessage.INTERNAL_ERROR, java.util.ResourceBundle.getBundle("com.quikj.application.web.talk.plugin.language", getLocale(from.getParam("language"))).getString("Could_not_find_session"), null, null, req_id)) == false) {
                    AceLogger.Instance().log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, "ServiceController.processJoinRequest() (TALK) -- Could not send join response message to the endpoint " + from);
                }
                return;
            }
            cdr.addSession(session_info.getBillingId());
            if (session_info.isConnected() == false) {
                AceLogger.Instance().log(AceLogger.WARNING, AceLogger.SYSTEM_LOG, "ServiceController.processJoinRequest() (TALK) -- All specified session in a join request are not connected");
                if (from.sendEvent(new MessageEvent(MessageEvent.CLIENT_RESPONSE_MESSAGE, null, ResponseMessage.BAD_REQUEST, java.util.ResourceBundle.getBundle("com.quikj.application.web.talk.plugin.language", getLocale(from.getParam("language"))).getString("All_sessions_must_be_in_connected_state"), null, null, req_id)) == false) {
                    AceLogger.Instance().log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, "ServiceController.processJoinRequest() (TALK) -- Could not send join response message to the endpoint " + from);
                }
                return;
            }
            if (session_info.indexOf(from) == -1) {
                AceLogger.Instance().log(AceLogger.WARNING, AceLogger.SYSTEM_LOG, "ServiceController.processJoinRequest() (TALK) --  All specified sessions for a join request do not belong to the same end point");
                if (from.sendEvent(new MessageEvent(MessageEvent.CLIENT_RESPONSE_MESSAGE, null, ResponseMessage.BAD_REQUEST, java.util.ResourceBundle.getBundle("com.quikj.application.web.talk.plugin.language", ServiceController.getLocale(from.getParam("language"))).getString("The_end_point_does_not_have_all_the_specified_sessions"), null, null, req_id)) == false) {
                    AceLogger.Instance().log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, "ServiceController.processJoinRequest() (TALK) -- Could not send join response message to the endpoint " + from);
                }
                return;
            }
            if (bridge != null) {
                bridge = session_info.getConferenceBridge();
            } else {
                ConferenceBridge conf = session_info.getConferenceBridge();
                if (conf != null) {
                    conf.interruptWait(AceSignalMessage.SIGNAL_TERM);
                    session_info.setConferenceBridge(null);
                }
            }
        }
        try {
            String bridge_name = "Bridge_" + first_session_id;
            if (bridge == null) {
                bridge = new ConferenceBridge(bridge_name);
                bridge.setSessionId(first_session_id);
                bridge.start();
            } else {
                bridge.setName(bridge_name);
                bridge.setSessionId(first_session_id);
            }
        } catch (IOException ex) {
            AceLogger.Instance().log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, "ServiceController.processJoinRequest() (TALK) -- IO Error occured while creating a conference bridge", ex);
            if (from.sendEvent(new MessageEvent(MessageEvent.CLIENT_RESPONSE_MESSAGE, null, ResponseMessage.INTERNAL_ERROR, java.util.ResourceBundle.getBundle("com.quikj.application.web.talk.plugin.language", getLocale(from.getParam("language"))).getString("Could_not_obtain_a_conference_bridge"), null, null, req_id)) == false) {
                AceLogger.Instance().log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, "ServiceController.processJoinRequest() (TALK) -- Could not send join response message to the endpoint " + from);
            }
            return;
        }
        first_session.setConferenceBridge(bridge);
        for (int i = 0; i < num_sessions; i++) {
            long session = message.getSessionIdAt(i);
            SessionInfo session_info = findSession(session);
            if (session_info == null) {
                AceLogger.Instance().log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, "ServiceController.processJoinRequest() (TALK) -- Could not find session for a join request");
                if (from.sendEvent(new MessageEvent(MessageEvent.CLIENT_RESPONSE_MESSAGE, null, ResponseMessage.INTERNAL_ERROR, java.util.ResourceBundle.getBundle("com.quikj.application.web.talk.plugin.language", getLocale(from.getParam("language"))).getString("Could_not_find_session"), null, null, req_id)) == false) {
                    AceLogger.Instance().log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, "ServiceController.processJoinRequest() (TALK) -- Could not send join response message to the endpoint " + from);
                }
                return;
            }
            int num_ep = session_info.numEndPoints();
            for (int j = 0; j < num_ep; j++) {
                EndPointInterface ep = session_info.elementAt(j);
                if (bridge.containsEndPoint(ep) == false) {
                    bridge.addEndPoint(ep);
                }
                if (first_session.indexOf(ep) == -1) {
                    first_session.addEndPoint(ep);
                }
            }
            if (i > 0) {
                removeSession(session);
            }
        }
        ActionEvent action = new ActionEvent(null);
        for (int j = 1; j < num_sessions; j++) {
            ReplaceSessionAction replace = new ReplaceSessionAction(message.getSessionIdAt(j), message.getSessionIdAt(0));
            action.addAction(replace);
        }
        ChangeEndPointAction change = new ChangeEndPointAction(first_session_id, bridge);
        action.addAction(change);
        int num_end_points = first_session.numEndPoints();
        for (int i = 0; i < num_end_points; i++) {
            EndPointInterface ep = first_session.elementAt(i);
            if (ep.sendEvent(action) == false) {
                AceLogger.Instance().log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, "ServiceController.processJoinRequest() (TALK) -- Could not send action event to an endpoint: " + ep);
            }
        }
        JoinResponseMessage response = new JoinResponseMessage();
        for (int i = 0; i < num_sessions; i++) {
            response.addSession(message.getSessionIdAt(i));
        }
        if (from.sendEvent(new MessageEvent(MessageEvent.CLIENT_RESPONSE_MESSAGE, null, ResponseMessage.OK, "OK", response, null, req_id)) == false) {
            AceLogger.Instance().log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, "ServiceController.processJoinRequest() (TALK) -- Could not send join response message to the endpoint " + from);
        } else {
            sendCDR(cdr);
        }
    }

    private void processMessageEvent(MessageEvent event) {
        int type = event.getEventType();
        switch(type) {
            case MessageEvent.REGISTRATION_REQUEST:
                processRegistrationRequest((RegistrationRequestMessage) event.getMessage(), event.getFrom());
                break;
            case MessageEvent.SETUP_REQUEST:
                processSetupRequest((SetupRequestMessage) event.getMessage(), event.getFrom());
                break;
            case MessageEvent.SETUP_RESPONSE:
                processSetupResponse((SetupResponseMessage) event.getMessage(), event.getResponseStatus(), event.getReason(), event.getFrom());
                break;
            case MessageEvent.DISCONNECT_MESSAGE:
                processDisconnectRequest((DisconnectMessage) event.getMessage(), event.getFrom());
                break;
            case MessageEvent.CLIENT_REQUEST_MESSAGE:
                processClientRequestMessage(event.getMessage(), event.getFrom(), event.getRequestId());
                break;
            default:
                AceLogger.Instance().log(AceLogger.WARNING, AceLogger.SYSTEM_LOG, "ServiceController.processMessageEvent() (TALK)-- Unknown event : " + event.messageType() + " received");
                break;
        }
    }

    private void processRegistrationRequest(RegistrationRequestMessage message, EndPointInterface from) {
        if (message.getLanguage() != null) {
            from.setParam("language", message.getLanguage());
        }
        if (EndPointList.Instance().findRegisteredEndPoint(message.getUserName()) != null) {
            String[] features = FeatureFactory.getInstance().getFeatureNames();
            int num_features = features.length;
            for (int i = 0; i < num_features; i++) {
                if (features[i].equals(message.getUserName()) == true) {
                    if (from.sendEvent(new MessageEvent(MessageEvent.REGISTRATION_RESPONSE, null, ResponseMessage.FORBIDDEN, java.util.ResourceBundle.getBundle("com.quikj.application.web.talk.plugin.language", ServiceController.getLocale(from.getParam("language"))).getString("User_already_registered"), null, null)) == false) {
                        AceLogger.Instance().log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, "ServiceController.processRegistrationRequest() (TALK) -- Could not send registration response message to the endpoint " + from);
                    }
                    return;
                }
            }
        }
        String password = null;
        String enc_password = message.getPassword();
        if (enc_password != null) {
            password = enc_password;
        }
        DbEndPointRegistration reg_request = new DbEndPointRegistration(from, message.getUserName(), password, this, database);
        if (reg_request.registerEndPoint() == false) {
            AceLogger.Instance().log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, "ServiceController.processRegistrationRequest() (TALK) -- Failure authenticating user " + message.getUserName() + ", error : " + reg_request.getLastError());
            if (from.sendEvent(new MessageEvent(MessageEvent.REGISTRATION_RESPONSE, null, ResponseMessage.INTERNAL_ERROR, java.util.ResourceBundle.getBundle("com.quikj.application.web.talk.plugin.language", ServiceController.getLocale(from.getParam("language"))).getString("Failure_authenticating_user"), null, null)) == false) {
                AceLogger.Instance().log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, "ServiceController.processRegistrationRequest() (TALK) -- Could not send registration response message to the endpoint " + from);
            }
            return;
        }
        pendingDbOps.put(from, reg_request);
        return;
    }

    private void processSendMailRequest(SendMailRequestMessage message, EndPointInterface from, int req_id) {
        if (EndPointList.Instance().findRegisteredEndPointInfo(from) == null) {
            AceLogger.Instance().log(AceLogger.WARNING, AceLogger.SYSTEM_LOG, "ServiceController.processSendMailRequest() (TALK) -- Send mail request message received from unregistered endpoint " + from);
            if (message.isReplyRequired() == true) {
                if (from.sendEvent(new MessageEvent(MessageEvent.CLIENT_RESPONSE_MESSAGE, null, ResponseMessage.FORBIDDEN, java.util.ResourceBundle.getBundle("com.quikj.application.web.talk.plugin.language", ServiceController.getLocale(from.getParam("language"))).getString("Unauthorized_send_mail_attempt_rejected"), new SendMailResponseMessage(), null, req_id)) == false) {
                    AceLogger.Instance().log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, "ServiceController.processSendMailRequest() (TALK) -- Could not send unauthorized send-mail response message to the endpoint " + from);
                }
            }
            return;
        }
        if (AceMailService.getInstance() == null) {
            if (message.isReplyRequired() == true) {
                if (from.sendEvent(new MessageEvent(MessageEvent.CLIENT_RESPONSE_MESSAGE, null, ResponseMessage.SERVICE_UNAVAILABLE, java.util.ResourceBundle.getBundle("com.quikj.application.web.talk.plugin.language", ServiceController.getLocale(from.getParam("language"))).getString("Mail_Service_not_active"), new SendMailResponseMessage(), null, req_id)) == false) {
                    AceLogger.Instance().log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, "ServiceController.processSendMailRequest() (TALK) -- Could not send service unavailable response message to the endpoint " + from);
                }
            }
            return;
        }
        MailElement rcv_mail = message.getMailElement();
        AceMailMessage out_mail = new AceMailMessage();
        out_mail.setSubType(rcv_mail.getSubype());
        int num_items = rcv_mail.numBcc();
        for (int i = 0; i < num_items; i++) {
            out_mail.addBcc(rcv_mail.getBccAt(i));
        }
        num_items = rcv_mail.numCc();
        for (int i = 0; i < num_items; i++) {
            out_mail.addCc(rcv_mail.getCcAt(i));
        }
        num_items = rcv_mail.numTo();
        for (int i = 0; i < num_items; i++) {
            out_mail.addTo(rcv_mail.getToAt(i));
        }
        num_items = rcv_mail.numReplyTo();
        for (int i = 0; i < num_items; i++) {
            out_mail.addReplyTo(rcv_mail.getReplyToAt(i));
        }
        String rcv_body = rcv_mail.getBody();
        if (rcv_body != null) {
            out_mail.setBody(rcv_body);
        }
        String rcv_from = rcv_mail.getFrom();
        if (rcv_from != null) {
            out_mail.setFrom(rcv_from);
        }
        String rcv_subject = rcv_mail.getSubject();
        if (rcv_subject != null) {
            out_mail.setSubject(rcv_subject);
        }
        if (AceMailService.getInstance().addToMailQueue(out_mail) == true) {
            if (message.isReplyRequired() == true) {
                if (from.sendEvent(new MessageEvent(MessageEvent.CLIENT_RESPONSE_MESSAGE, null, ResponseMessage.OK, "OK", new SendMailResponseMessage(), null, req_id)) == false) {
                    AceLogger.Instance().log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, "ServiceController.processSendMailRequest() (TALK) -- Could not send OK send-mail response message to the endpoint " + from);
                }
            }
        } else {
            if (message.isReplyRequired() == true) {
                if (from.sendEvent(new MessageEvent(MessageEvent.CLIENT_RESPONSE_MESSAGE, null, ResponseMessage.INTERNAL_ERROR, java.util.ResourceBundle.getBundle("com.quikj.application.web.talk.plugin.language", ServiceController.getLocale(from.getParam("language"))).getString("Send_mail_attempt_failed"), new SendMailResponseMessage(), null, req_id)) == false) {
                    AceLogger.Instance().log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, "ServiceController.processSendMailRequest() (TALK) -- Could not send fail send-mail response message to the endpoint " + from);
                }
            }
        }
    }

    private void processSetupRequest(SetupRequestMessage message, EndPointInterface from) {
        if (from.getParam("language") == null) {
            if (message.getCallingNameElement() != null) {
                String language = message.getCallingNameElement().getCallParty().getLanguage();
                if (language != null) {
                    from.setParam("language", language);
                }
            }
        }
        long session_id = message.getSessionId();
        if (findSession(session_id) != null) {
            SetupResponseMessage response = new SetupResponseMessage();
            response.setSessionId(session_id);
            if (from.sendEvent(new MessageEvent(MessageEvent.SETUP_RESPONSE, null, ResponseMessage.INTERNAL_ERROR, java.util.ResourceBundle.getBundle("com.quikj.application.web.talk.plugin.language", getLocale(from.getParam("language"))).getString("Duplicate_session_id"), response, null)) == false) {
                AceLogger.Instance().log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, "ServiceController.processSetupRequest() (TALK) -- Could not send setup response message to the endpoint " + from);
            }
            return;
        }
        if (message.getTransferId() == null) {
            if (EndPointList.Instance().findRegisteredEndPointInfo(from) == null) {
                String name = null;
                String email = null;
                String additional = null;
                String environment = null;
                String ip = null;
                String cookie = null;
                CallingNameElement calling = message.getCallingNameElement();
                if (calling != null) {
                    name = calling.getCallParty().getFullName();
                    email = calling.getCallParty().getEmail();
                    additional = calling.getCallParty().getComment();
                    environment = calling.getCallParty().getEnvironment();
                    ip = calling.getCallParty().getIpAddress();
                    cookie = calling.getCallParty().getEndUserCookie();
                }
                UnregisteredUserLoginCDR cdr = new UnregisteredUserLoginCDR(from.getIdentifier(), name, email, additional, environment, ip, cookie);
                sendCDR(cdr);
            }
        }
        String called_name = message.getCalledNameElement().getCallParty().getUserName();
        EndPointInterface called_endpoint = EndPointList.Instance().findRegisteredEndPoint(called_name);
        if (called_endpoint == null) {
            SessionSetupCDR cdr = new SessionSetupCDR(from.getIdentifier(), called_name, message.getTransferId());
            sendCDR(cdr);
            SessionInfo session = new SessionInfo(session_id, from);
            session.setBillingId(cdr.getIdentifier());
            addSession(session);
            SetupResponseMessage response = new SetupResponseMessage();
            response.setSessionId(session_id);
            response.setCalledParty(message.getCalledNameElement());
            checkUnavailableUserTransfer(session, called_name, null, response, SetupResponseMessage.UNAVAILABLE, java.util.ResourceBundle.getBundle("com.quikj.application.web.talk.plugin.language", ServiceController.getLocale(from.getParam("language"))).getString("User_not_found"));
            return;
        }
        EndPointInfo info = EndPointList.Instance().findRegisteredEndPointInfo(called_endpoint);
        if (info == null) {
            AceLogger.Instance().log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, "ServiceController.processSetupRequest() (TALK) -- Could not find the end-point info for " + called_endpoint);
            SetupResponseMessage response = new SetupResponseMessage();
            response.setSessionId(session_id);
            if (from.sendEvent(new MessageEvent(MessageEvent.SETUP_RESPONSE, null, ResponseMessage.INTERNAL_ERROR, java.util.ResourceBundle.getBundle("com.quikj.application.web.talk.plugin.language", getLocale(from.getParam("language"))).getString("Failed_to_send_message_to_the_called_party"), response, null)) == false) {
                AceLogger.Instance().log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, "ServiceController.processSetupRequest() (TALK) -- Could not send setup response message to the endpoint " + from);
            }
            return;
        }
        if (info.isDnd() == true) {
            SetupResponseMessage response = new SetupResponseMessage();
            response.setSessionId(session_id);
            if (from.sendEvent(new MessageEvent(MessageEvent.SETUP_RESPONSE, null, SetupResponseMessage.BUSY, java.util.ResourceBundle.getBundle("com.quikj.application.web.talk.plugin.language", getLocale(from.getParam("language"))).getString("The_user_has_enabled_do_not_disturb"), response, null)) == false) {
                AceLogger.Instance().log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, "ServiceController.processSetupRequest() (TALK) -- Could not send setup response message to the endpoint " + from);
            }
            return;
        }
        if (called_endpoint.sendEvent(new MessageEvent(MessageEvent.SETUP_REQUEST, from, message, null)) == false) {
            AceLogger.Instance().log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, "ServiceController.processSetupRequest() (TALK) -- Could not send setup message to the called endpoint " + called_endpoint);
            SetupResponseMessage response = new SetupResponseMessage();
            response.setSessionId(session_id);
            if (from.sendEvent(new MessageEvent(MessageEvent.SETUP_RESPONSE, null, ResponseMessage.INTERNAL_ERROR, java.util.ResourceBundle.getBundle("com.quikj.application.web.talk.plugin.language", getLocale(from.getParam("language"))).getString("Failed_to_send_message_to_the_called_party"), response, null)) == false) {
                AceLogger.Instance().log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, "ServiceController.processSetupRequest() (TALK) -- Could not send setup response message to the endpoint " + from);
            }
            return;
        }
        SessionSetupCDR cdr = new SessionSetupCDR(from.getIdentifier(), called_endpoint.getIdentifier(), message.getTransferId());
        sendCDR(cdr);
        SessionInfo session = new SessionInfo(session_id, from);
        session.setBillingId(cdr.getIdentifier());
        session.addEndPoint(called_endpoint);
        addSession(session);
    }

    public void processSetupResponse(SetupResponseMessage message, int status, String reason, EndPointInterface from) {
        SessionInfo session = findSession(message.getSessionId());
        if (session == null) {
            AceLogger.Instance().log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, "ServiceController.processSetupResponse() (TALK) -- Could not find session with id " + message.getSessionId());
            return;
        }
        if ((status == SetupResponseMessage.NOANS) || (status == SetupResponseMessage.BUSY)) {
            UserElement unavail_user = EndPointList.Instance().findRegisteredUserData(from);
            String xferto_name = unavail_user.getUnavailXferTo();
            if (xferto_name != null) {
                if (xferto_name.length() > 0) {
                    generateSetupResponseCDR(message, status, from, session);
                    UserElement xferto_user = EndPointList.Instance().findRegisteredUserData(xferto_name);
                    if (xferto_user != null) {
                        transferUserUnavailableCall(session, xferto_user, from, unavail_user.getName());
                        return;
                    } else {
                        checkUnavailableUserTransfer(session, unavail_user.getName(), from, message, status, reason);
                        return;
                    }
                }
            }
        }
        finishSetupResponse(message, status, reason, from);
    }

    private void processUnregistrationEvent(UnregistrationEvent message) {
        unregisterUser(message.getUser());
    }

    private boolean removeSession(long session) {
        Long session_id = new Long(session);
        if (sessionList.get(session_id) == null) {
            writeErrorMessage(session + " does not exist", null);
            return false;
        }
        sessionList.remove(session_id);
        return true;
    }

    public void run() {
        FeatureFactory.getInstance().startUp();
        pingDatabase();
        AceRMIImpl rs = AceRMIImpl.getInstance();
        if (rs != null) {
            rs.registerService("com.quikj.application.web.talk.plugin.ServiceController", this);
        }
        while (true) {
            AceMessageInterface message = waitMessage();
            if (message == null) {
                AceLogger.Instance().log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, getName() + "- ServiceController.run() -- A null message was received while waiting for a message - " + getErrorMessage());
                break;
            }
            if ((message instanceof AceSignalMessage) == true) {
                AceLogger.Instance().log(AceLogger.INFORMATIONAL, AceLogger.SYSTEM_LOG, getName() + " - ServiceController.run() --  A signal " + ((AceSignalMessage) message).getSignalId() + " is received : " + ((AceSignalMessage) message).getMessage());
                break;
            } else if ((message instanceof MessageEvent) == true) {
                processMessageEvent((MessageEvent) message);
            } else if ((message instanceof UnregistrationEvent) == true) {
                processUnregistrationEvent((UnregistrationEvent) message);
            } else if ((message instanceof AceSQLMessage) == true) {
                DbOperationInterface op = (DbOperationInterface) ((AceSQLMessage) message).getUserParm();
                if (op != null) {
                    synchronized (pendingDbOps) {
                        if (pendingDbOps.contains(op) == true) {
                            if (op.processResponse((AceSQLMessage) message) == true) {
                                pendingDbOps.remove(op.getEndPoint());
                            }
                        } else {
                            op.processResponse((AceSQLMessage) message);
                        }
                    }
                } else {
                    AceLogger.Instance().log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, getName() + "- ServiceController.run() -- No database handler for database event.");
                }
            } else if ((message instanceof AceTimerMessage) == true) {
                AceTimerMessage timer_msg = (AceTimerMessage) message;
                if (timer_msg.getUserSpecifiedParm() == PING_DATABASE_TIMER) {
                    pingDatabase();
                }
            } else {
                AceLogger.Instance().log(AceLogger.WARNING, AceLogger.SYSTEM_LOG, getName() + "- ServiceController.run() -- An unexpected event is received : " + message.messageType());
            }
        }
        cleanup();
    }

    protected void sendCDR(CDRInterface cdr) {
        CDRHandler handler = CDRHandler.getInstance();
        if (handler != null) {
            if (handler.sendCDR(cdr) == false) {
                AceLogger.Instance().log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, "ServiceController.sendCDR() (TALK) -- Could not send CDR" + getErrorMessage());
            }
        }
    }

    public boolean sendEvent(AceMessageInterface message) {
        return sendMessage(message);
    }

    public boolean sendRegistrationResponse(EndPointInterface endpoint, int response_status, String reason, TalkMessageInterface message, Object user_parm) {
        if (endpoint.sendEvent(new MessageEvent(MessageEvent.REGISTRATION_RESPONSE, null, response_status, reason, message, user_parm)) == false) {
            AceLogger.Instance().log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, getName() + "- ServiceController.sendRegistrationResponse() -- Could not send registration response message to the endpoint " + endpoint);
            return false;
        }
        return true;
    }

    public boolean setRMIParam(String param, String value) {
        return false;
    }

    public void transferUserUnavailableCall(SessionInfo session, UserElement xferto, EndPointInterface from, String transfer_from_name) {
        DisconnectMessage message = new DisconnectMessage();
        message.setSessionId(session.getSessionId());
        DisconnectReasonElement reason = new DisconnectReasonElement();
        reason.setReasonCode(DisconnectReasonElement.NORMAL_DISCONNECT);
        String user = null;
        if (xferto.getFullName() != null) {
            user = xferto.getFullName();
        } else {
            user = xferto.getName();
        }
        reason.setReasonText(java.util.ResourceBundle.getBundle("com.quikj.application.web.talk.plugin.language", ServiceController.getLocale(session.getCallingEndPoint().getParam("language"))).getString("Called_party_unavailable_-_your_session_is_being_transferred_to_") + ' ' + user);
        message.setDisconnectReason(reason);
        CalledNameElement called = new CalledNameElement();
        CallPartyElement party = new CallPartyElement();
        party.setName(xferto.getName());
        party.setFullName(xferto.getFullName());
        called.setCallParty(party);
        message.setCalledInfo(called);
        message.setFrom(transfer_from_name);
        processDisconnectRequest(message, from);
    }

    public void unregisterUser(String user_name) {
        EndPointInfo info = EndPointList.Instance().findRegisteredEndPointInfo(user_name);
        if (info == null) {
            AceLogger.Instance().log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, "ServiceController.processUnregistrationEvent() (TALK) -- Could not find endpoint info " + user_name + " in the registered end-point list");
            return;
        }
        EndPointInterface endpoint = info.getEndPoint();
        cancelDbOperation(endpoint);
        if (info.isDnd() == false) {
            groupNotifyOfAvailabilityChange(user_name, false);
        }
        LogoutCDR cdr = new LogoutCDR(endpoint.getIdentifier());
        sendCDR(cdr);
        if (EndPointList.Instance().removeRegisteredEndPoint(user_name) == false) {
            AceLogger.Instance().log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, "ServiceController.processUnregistrationEvent() (TALK) -- Could not remove endpoint " + user_name + " from the registered end-point list");
            return;
        }
    }
}
