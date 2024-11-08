package com.tegsoft.pbx;

import java.util.ArrayList;
import org.apache.log4j.Level;
import org.asteriskjava.manager.action.PingAction;
import org.asteriskjava.manager.event.AgentCalledEvent;
import org.asteriskjava.manager.event.AgentConnectEvent;
import org.asteriskjava.manager.event.DtmfEvent;
import org.asteriskjava.manager.event.HangupEvent;
import org.asteriskjava.manager.event.ManagerEvent;
import org.asteriskjava.manager.event.NewCallerIdEvent;
import org.asteriskjava.manager.event.NewStateEvent;
import org.asteriskjava.manager.event.QueueMemberAddedEvent;
import org.asteriskjava.manager.event.QueueMemberRemovedEvent;
import org.asteriskjava.manager.event.VarSetEvent;
import com.tegsoft.tobe.db.Counter;
import com.tegsoft.tobe.db.command.Command;
import com.tegsoft.tobe.db.connection.Connection;
import com.tegsoft.tobe.os.LicenseManager;
import com.tegsoft.tobe.util.Compare;
import com.tegsoft.tobe.util.DateUtil;
import com.tegsoft.tobe.util.JobUtil;
import com.tegsoft.tobe.util.NullStatus;
import com.tegsoft.tobe.util.message.MessageUtil;

public class ManagerEventListener extends Thread implements org.asteriskjava.manager.ManagerEventListener {

    private ManagerConnection managerConnection;

    private ArrayList<NewStateEvent> activeCalls = new ArrayList<NewStateEvent>();

    private String PBXID;

    private boolean checkConnection() throws Exception {
        PBXID = TegsoftPBX.getLocalPBXID();
        if (NullStatus.isNull(PBXID)) {
            MessageUtil.logMessage(ManagerEventListener.class, Level.FATAL, "Unable to locate PBXID exiting");
            return false;
        }
        if (LicenseManager.isValid("TegsoftCC") <= 0) {
            MessageUtil.logMessage(ManagerEventListener.class, Level.FATAL, "Unable validate License exiting");
            return false;
        }
        if (managerConnection == null) {
            managerConnection = TegsoftPBX.createManagerConnection(PBXID);
            managerConnection.addEventListener(this);
            return managerConnection != null;
        } else {
            managerConnection.sendAction(new PingAction(), 10000);
        }
        return managerConnection != null;
    }

    @Override
    public void run() {
        try {
            JobUtil.prepareThread();
            Connection.initNonJ2EE();
            while (true) {
                if (!checkConnection()) {
                    Thread.sleep(60 * 1000);
                    continue;
                }
                Thread.sleep(1000 * 60);
            }
        } catch (Exception ex) {
            MessageUtil.logMessage(ManagerEventListener.class, Level.FATAL, ex);
        }
    }

    private static void updateState(String uniqueId, String state) throws Exception {
    }

    private static void notifyAgent(String uniqueId, String agentUID, String callerId, String INTERFACE) throws Exception {
        if (new Command("SELECT COUNT(*) FROM TBLCCAGENTLOG WHERE CALLID='" + uniqueId + "' ").executeScalarAsDecimal().intValue() > 0) {
            return;
        }
        Command command = new Command("INSERT INTO TBLCCAGENTLOG (UNITUID,LOGID,UID,LOGTYPE,STATUS,BEGINDATE,REASON,CALLID,PHONE,INTERFACE,MODUID,MODDATE) VALUES({UNITUID},");
        command.bind(Counter.getUUID());
        command.append(",");
        command.bind(agentUID);
        command.append(",");
        command.append("'BUSY'");
        command.append(",");
        command.append("'BUSY'");
        command.append(",");
        command.bind(DateUtil.now());
        command.append(",");
        command.append("'DEFAULT'");
        command.append(",");
        command.bind(uniqueId);
        command.append(",");
        command.bind(callerId);
        command.append(",");
        command.bind(INTERFACE);
        command.append(",");
        command.append("'setup-admin-uuid'");
        command.append(",");
        command.bind(DateUtil.now());
        command.append(")");
        command.executeNonQuery(true);
        Connection.getActive().commit();
    }

    @Override
    public void onManagerEvent(ManagerEvent managerEvent) {
        try {
            JobUtil.prepareThread();
            Connection.initNonJ2EE();
            if (NewStateEvent.class.isAssignableFrom(managerEvent.getClass())) {
                NewStateEvent newStateEvent = (NewStateEvent) managerEvent;
                addActiveCall(newStateEvent);
                Connection.initNonJ2EE();
                updateState(newStateEvent.getUniqueId(), newStateEvent.getChannelStateDesc());
                Connection.closeActive();
            } else if (QueueMemberAddedEvent.class.isAssignableFrom(managerEvent.getClass())) {
                ConfigWriter.writeFOP(PBXID);
                Runtime.getRuntime().exec("service tegsoft_panel reload");
            } else if (QueueMemberRemovedEvent.class.isAssignableFrom(managerEvent.getClass())) {
                Thread.sleep(2000);
                ConfigWriter.writeFOP(PBXID);
                Runtime.getRuntime().exec("service tegsoft_panel reload");
            } else if (AgentCalledEvent.class.isAssignableFrom(managerEvent.getClass())) {
                AgentCalledEvent agentCalledEvent = (AgentCalledEvent) managerEvent;
                Connection.initNonJ2EE();
                Command command = new Command("SELECT UID FROM TBLCCAGENTLOG WHERE UNITUID={UNITUID} AND LOGTYPE='LOGIN' AND INTERFACE=");
                command.bind(agentCalledEvent.getAgentCalled());
                command.append("AND ENDDATE IS NULL");
                String agentUID = command.executeScalarAsString();
                if (NullStatus.isNull(agentUID)) {
                    Connection.closeActive();
                    return;
                }
                String uniqueId = agentCalledEvent.getUniqueId();
                if (NullStatus.isNull(uniqueId)) {
                    if (NullStatus.isNotNull(agentCalledEvent.getChannelCalling())) {
                        NewStateEvent callingCahnnel = getActiveCall(agentCalledEvent.getChannelCalling());
                        if (callingCahnnel != null) {
                            uniqueId = callingCahnnel.getUniqueId();
                        }
                    }
                }
                notifyAgent(uniqueId, agentUID, agentCalledEvent.getCallerIdNum(), agentCalledEvent.getAgentCalled());
                Connection.closeActive();
            } else if (NewCallerIdEvent.class.isAssignableFrom(managerEvent.getClass())) {
                JobUtil.prepareThread();
                Connection.initNonJ2EE();
                NewCallerIdEvent newCallerIdEvent = (NewCallerIdEvent) managerEvent;
                Command command = new Command("UPDATE TBLCCAGENTLOG SET REASON='DEFAULT',PHONE=");
                command.bind(newCallerIdEvent.getCallerIdNum());
                command.append("WHERE UNITUID={UNITUID} AND CALLID=");
                command.bind(newCallerIdEvent.getUniqueId());
                command.executeNonQuery(true);
                Connection.getActive().commit();
                Connection.closeActive();
            } else if (AgentConnectEvent.class.isAssignableFrom(managerEvent.getClass())) {
                AgentConnectEvent agentConnectEvent = (AgentConnectEvent) managerEvent;
                new Command("UPDATE TBLCCAGENTLOG SET REASON='DEFAULT' WHERE CALLID='" + agentConnectEvent.getUniqueId() + "'").executeNonQuery(true);
            } else if (VarSetEvent.class.isAssignableFrom(managerEvent.getClass())) {
                JobUtil.prepareThread();
                Connection.initNonJ2EE();
                VarSetEvent varSetEvent = (VarSetEvent) managerEvent;
                if ("QEHOLDTIME".equals(varSetEvent.getVariable())) {
                    new Command("UPDATE TBLCCAGENTLOG SET REASON='DEFAULT' WHERE CALLID='" + varSetEvent.getUniqueId() + "'").executeNonQuery(true);
                }
            } else if (HangupEvent.class.isAssignableFrom(managerEvent.getClass())) {
                JobUtil.prepareThread();
                Connection.initNonJ2EE();
                HangupEvent hangupEvent = (HangupEvent) managerEvent;
                new Command("UPDATE TBLCCCAMPCALLD A SET A.HANGUP={NOW}  WHERE A.CALLID='" + hangupEvent.getUniqueId() + "' ").executeNonQuery(true);
                new Command("UPDATE TBLCCAGENTLOG SET ENDDATE={NOW} WHERE CALLID='" + hangupEvent.getUniqueId() + "'").executeNonQuery(true);
                removeActiveCall(hangupEvent.getUniqueId());
                Connection.closeActive();
            } else if (DtmfEvent.class.isAssignableFrom(managerEvent.getClass())) {
                JobUtil.prepareThread();
                Connection.initNonJ2EE();
                DtmfEvent dtmfEvent = (DtmfEvent) managerEvent;
                String CAMPAIGNID = new Command("SELECT CAMPAIGNID FROM TBLCCCAMPCALLD WHERE CALLID='" + dtmfEvent.getUniqueId() + "' ").executeScalarAsString();
                String CONTID = new Command("SELECT CONTID FROM TBLCCCAMPCALLD WHERE CALLID='" + dtmfEvent.getUniqueId() + "' ").executeScalarAsString();
                Command command = new Command("UPDATE TBLCCCAMPCONT SET DTMFARRAY=' ' WHERE CAMPAIGNID=");
                command.bind(CAMPAIGNID);
                command.append("AND CONTID=");
                command.bind(CONTID);
                command.append("AND DTMFARRAY IS NULL");
                command.executeNonQuery(true);
                command = new Command("UPDATE TBLCCCAMPCONT SET DTMFARRAY=DTMFARRAY||'" + dtmfEvent.getDigit() + "' WHERE CAMPAIGNID=");
                command.bind(CAMPAIGNID);
                command.append("AND CONTID=");
                command.bind(CONTID);
                command.executeNonQuery(true);
                Connection.closeActive();
            }
        } catch (Exception ex) {
            MessageUtil.logMessage(ManagerEventListener.class, Level.FATAL, ex);
        }
        Connection.closeActive();
    }

    private synchronized void addActiveCall(NewStateEvent newStateEvent) {
        if (newStateEvent == null) {
            return;
        }
        if (NullStatus.isNull(newStateEvent.getChannel())) {
            return;
        }
        boolean add = true;
        for (NewStateEvent activeCall : activeCalls) {
            if (Compare.equal(activeCall.getChannel(), newStateEvent.getChannel())) {
                add = false;
                break;
            }
        }
        if (add) {
            activeCalls.add(newStateEvent);
        }
    }

    private synchronized NewStateEvent getActiveCall(String channel) {
        if (channel == null) {
            return null;
        }
        for (NewStateEvent activeCall : activeCalls) {
            if (Compare.equal(activeCall.getChannel(), channel)) {
                return activeCall;
            }
        }
        return null;
    }

    private synchronized void removeActiveCall(String channel) {
        if (channel == null) {
            return;
        }
        NewStateEvent removeCall = null;
        for (NewStateEvent activeCall : activeCalls) {
            if (Compare.equal(activeCall.getChannel(), channel)) {
                removeCall = activeCall;
                break;
            }
        }
        if (removeCall != null) {
            activeCalls.remove(removeCall);
        }
    }
}
