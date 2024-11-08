package com.tegsoft.cc;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.asteriskjava.manager.ManagerConnectionState;
import org.asteriskjava.manager.action.HangupAction;
import org.asteriskjava.manager.action.OriginateAction;
import org.asteriskjava.manager.action.QueueAddAction;
import org.asteriskjava.manager.action.QueueRemoveAction;
import org.asteriskjava.manager.action.RedirectAction;
import org.asteriskjava.manager.action.SetVarAction;
import org.asteriskjava.manager.action.StopMonitorAction;
import org.asteriskjava.manager.event.StatusEvent;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Iframe;
import org.zkoss.zul.Image;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Vbox;
import com.tegsoft.ivr.BaseTegsoftIVR;
import com.tegsoft.pbx.ManagerConnection;
import com.tegsoft.pbx.TegsoftPBX;
import com.tegsoft.tobe.db.Counter;
import com.tegsoft.tobe.db.command.Command;
import com.tegsoft.tobe.db.dataset.DataColumnType;
import com.tegsoft.tobe.db.dataset.DataRow;
import com.tegsoft.tobe.db.dataset.Dataset;
import com.tegsoft.tobe.os.LicenseManager;
import com.tegsoft.tobe.ui.MessageType;
import com.tegsoft.tobe.ui.tags.Import;
import com.tegsoft.tobe.util.Compare;
import com.tegsoft.tobe.util.Converter;
import com.tegsoft.tobe.util.DateUtil;
import com.tegsoft.tobe.util.NullStatus;
import com.tegsoft.tobe.util.StringUtil;
import com.tegsoft.tobe.util.UiUtil;
import com.tegsoft.tobe.util.message.MessageUtil;

public class Agent {

    private String INTERFACE;

    private String REMOTEINTERFACE;

    private String FROMADDRESS;

    private String EXTID;

    private String PBXID;

    private String LOGID;

    private Map<String, ManagerConnection> managerConnections = new HashMap<String, ManagerConnection>();

    private boolean voiceEnabled = false;

    private boolean chatEnabled = false;

    private boolean emailEnabled = false;

    private boolean campEnabled = false;

    private int externalCRMCount = 0;

    private ArrayList<String> externalCRMPOPUPTYPE = new ArrayList<String>();

    private ArrayList<String> externalCRMPOPUPURL = new ArrayList<String>();

    private ArrayList<String> externalCRMAFTERCALLURL = new ArrayList<String>();

    private Dataset TBLCCAGENTLOG;

    private Dataset TBLCCCAMPCONT;

    private Dataset TBLCCCAMPAGENT;

    private String activeCAMPAIGNID;

    private static void fixLOGENDDATE() throws Exception {
        Command command = new Command("UPDATE TBLCCAGENTLOG A SET A.ENDDATE=(SELECT MAX(B.LOGOUT) FROM TBLLOGINS B WHERE A.SESSIONID=B.SESSIONID)");
        command.append("WHERE A.ENDDATE IS NULL AND A.SESSIONID IN (SELECT C.SESSIONID FROM TBLLOGINS C WHERE A.SESSIONID=C.SESSIONID AND C.LOGOUT IS NOT NULL)");
        command.executeNonQuery(true);
    }

    public static void login(Event event) throws Exception {
        login();
    }

    private static void setAgentVoiceInterface(Agent agent, String CTIADDRESS) throws Exception {
        if (NullStatus.isNull(CTIADDRESS)) {
            return;
        }
        Dataset TBLPBXEXT = new Dataset("TBLPBXEXT", "TBLPBXEXT");
        Command command = new Command("SELECT * FROM TBLPBXEXT WHERE UNITUID={UNITUID} AND DISABLED='false' AND PBXID IN (SELECT PBXID FROM TBLPBX WHERE UNITUID={UNITUID}) AND (UPPER(CTIADDRESS)=UPPER(");
        command.bind(CTIADDRESS);
        command.append(") OR UPPER(CTIADDRESS)=UPPER(");
        command.bind("[" + CTIADDRESS + "]");
        command.append("))");
        TBLPBXEXT.fill(command);
        if (TBLPBXEXT.getRowCount() > 0) {
            String localPBXID = TegsoftPBX.getLocalPBXID();
            if (!Compare.equal(localPBXID, TBLPBXEXT.getRow(0).getString("PBXID"))) {
                agent.setINTERFACE("SIP/" + TBLPBXEXT.getRow(0).getString("PBXID") + "/" + TBLPBXEXT.getRow(0).getString("EXTEN"));
            } else {
                agent.setINTERFACE("SIP/" + TBLPBXEXT.getRow(0).getString("EXTEN"));
            }
            agent.setREMOTEINTERFACE("SIP/" + TBLPBXEXT.getRow(0).getString("PBXID") + "/" + TBLPBXEXT.getRow(0).getString("EXTEN"));
            agent.setFROMADDRESS(CTIADDRESS);
            agent.setEXTID(TBLPBXEXT.getRow(0).getString("EXTID"));
            agent.setPBXID(TBLPBXEXT.getRow(0).getString("PBXID"));
        }
        if (NullStatus.isNull(agent.getINTERFACE())) {
            Dataset TBLCCSEATS = new Dataset("TBLCCSEATS", "TBLCCSEATS");
            command = new Command("SELECT * FROM TBLCCSEATS WHERE UNITUID={UNITUID} AND UPPER(CTIADDRESS)=UPPER(");
            command.bind(CTIADDRESS);
            command.append(")");
            TBLCCSEATS.fill(command);
            if (TBLCCSEATS.getRowCount() > 0) {
                agent.setINTERFACE("SIP/" + TBLCCSEATS.getRow(0).getString("TRUNKID") + "/" + TBLCCSEATS.getRow(0).getString("EXTEN"));
                agent.setREMOTEINTERFACE("SIP/" + TBLCCSEATS.getRow(0).getString("TRUNKID") + "/" + TBLCCSEATS.getRow(0).getString("EXTEN"));
                agent.setFROMADDRESS(CTIADDRESS);
                agent.setEXTID(null);
                agent.setPBXID(null);
            }
        }
    }

    private static void findAgentSeat(Agent agent) throws Exception {
        String remoteAddr = UiUtil.getRemoteAddr();
        String remoteHost = UiUtil.getRemoteHost();
        String computerName = UiUtil.getParameter("computername");
        if (NullStatus.isNotNull(computerName)) {
            setAgentVoiceInterface(agent, computerName);
        }
        if (NullStatus.isNull(agent.getINTERFACE())) {
            setAgentVoiceInterface(agent, remoteAddr);
        }
        if (NullStatus.isNull(agent.getINTERFACE())) {
            setAgentVoiceInterface(agent, remoteHost);
        }
    }

    @SuppressWarnings("unchecked")
    public static void login() throws Exception {
        Agent agent = (Agent) UiUtil.getSessionAttribute("agent");
        if (agent != null) {
            ((Component) UiUtil.findComponent("topElement")).setVisible(false);
            UiUtil.showMessage(MessageType.ERROR, MessageUtil.getMessage(Agent.class, Messages.agent_5));
            return;
        }
        UiUtil.addLogoutMethod("com.tegsoft.cc.Agent.logout");
        agent = new Agent();
        agent.TBLCCAGENTLOG = new Dataset("TBLCCAGENTLOG", "TBLCCAGENTLOG");
        agent.TBLCCCAMPCONT = new Dataset("TBLCCCAMPCONT", "TBLCCCAMPCONT");
        agent.TBLCCCAMPAGENT = new Dataset("TBLCCCAMPAGENT", "TBLCCCAMPAGENT");
        agent.TBLCCCAMPCONT.addDataColumn("PHONE1");
        agent.TBLCCCAMPCONT.addDataColumn("PHONE2");
        agent.TBLCCCAMPCONT.addDataColumn("PHONE3");
        agent.TBLCCCAMPCONT.addDataColumn("PHONE4");
        UiUtil.setSessionAttribute("agent", agent);
        Dataset TBLCCAGENT = new Dataset("TBLCCAGENT", "TBLCCAGENT");
        TBLCCAGENT.fill(new Command("SELECT * FROM TBLCCAGENT WHERE UNITUID={UNITUID} AND UID={UID}"));
        if (TBLCCAGENT.getRowCount() == 0) {
            ((Component) UiUtil.findComponent("topElement")).setVisible(false);
            UiUtil.showMessage(MessageType.ERROR, MessageUtil.getMessage(Agent.class, Messages.agent_1));
            return;
        }
        findAgentSeat(agent);
        if (NullStatus.isNotNull(agent.getINTERFACE())) {
            ((Textbox) UiUtil.findComponent("agentEXTENTION")).setValue(agent.getINTERFACE());
            ((Textbox) UiUtil.findComponent("agentIPADDRESS")).setValue(agent.getFROMADDRESS());
            agent.voiceEnabled = true;
            if (!"false".equalsIgnoreCase(TBLCCAGENT.getRow(0).getString("CAMP"))) {
                if (LicenseManager.isValid("TegsoftCC_Progressive") > 0) {
                    agent.campEnabled = true;
                    ((Checkbox) UiUtil.findComponent("CAMP")).setChecked(true);
                }
            }
        } else {
            agent.voiceEnabled = false;
            ((Component) UiUtil.findComponent("topElement")).setVisible(false);
            UiUtil.showMessage(MessageType.ERROR, MessageUtil.getMessage(Agent.class, Messages.agent_4));
            return;
        }
        if (!"false".equalsIgnoreCase(TBLCCAGENT.getRow(0).getString("CHAT"))) {
            if (LicenseManager.isValid("TegsoftCC_Chat") > 0) {
                agent.chatEnabled = true;
                ((Checkbox) UiUtil.findComponent("CHAT")).setChecked(true);
            }
        }
        if (!"false".equalsIgnoreCase(TBLCCAGENT.getRow(0).getString("EMAIL"))) {
            if (LicenseManager.isValid("TegsoftCC_EMail") > 0) {
                agent.emailEnabled = true;
                ((Checkbox) UiUtil.findComponent("EMAIL")).setChecked(true);
            }
        }
        fixLOGENDDATE();
        int loginCount = new Command("SELECT COUNT(*) FROM TBLCCAGENTLOG WHERE 1=1 AND UNITUID={UNITUID} AND LOGTYPE='LOGIN' AND ENDDATE IS NULL").executeScalarAsDecimal().intValue();
        int allowedCount = LicenseManager.isValid("TegsoftCC");
        if (loginCount > allowedCount) {
            ((Component) UiUtil.findComponent("topElement")).setVisible(false);
            UiUtil.showMessage(MessageType.ERROR, MessageUtil.getMessage(Agent.class, Messages.agent_3, loginCount, allowedCount));
            return;
        }
        DataRow rowTBLCCAGENTLOG = agent.TBLCCAGENTLOG.addNewDataRow();
        rowTBLCCAGENTLOG.setString("UID", UiUtil.getUID());
        rowTBLCCAGENTLOG.setString("LOGTYPE", "LOGIN");
        rowTBLCCAGENTLOG.setString("STATUS", "LOGIN");
        rowTBLCCAGENTLOG.setTimestamp("BEGINDATE", DateUtil.now());
        rowTBLCCAGENTLOG.setString("REASON", "DEFAULT");
        rowTBLCCAGENTLOG.setString("INTERFACE", agent.getINTERFACE());
        rowTBLCCAGENTLOG.setString("RINTERFACE", agent.getREMOTEINTERFACE());
        agent.TBLCCAGENTLOG.save();
        agent.createManagerConnections();
        notReady();
        Tabbox crmTabbox = (Tabbox) UiUtil.findComponent("crmTabbox");
        Dataset TBLCCPOPUP = new Dataset("TBLCCPOPUP", "TBLCCPOPUP");
        TBLCCPOPUP.fill(new Command("SELECT * FROM TBLCCPOPUP WHERE 1=1 AND UNITUID={UNITUID} AND POPUPID IN (SELECT POPUPID FROM TBLCCPOPUPAGENT WHERE UID={UID} AND UNITUID={UNITUID})"));
        agent.externalCRMCount = TBLCCPOPUP.getRowCount();
        for (int i = 0; i < TBLCCPOPUP.getRowCount(); i++) {
            Tab tab = new Tab(TBLCCPOPUP.getRow(i).getString("NAME"));
            crmTabbox.getTabs().getChildren().add(0, tab);
            String type = "html";
            String LOGINURL = TBLCCPOPUP.getRow(i).getString("LOGINURL");
            if (NullStatus.isNotNull(LOGINURL)) {
                if (LOGINURL.startsWith("form://")) {
                    type = "form";
                }
            }
            Component component;
            if ("html".equals(type)) {
                component = new Iframe();
                Iframe iframe = (Iframe) component;
                iframe.setWidth("100%");
                iframe.setHeight("100%");
                iframe.setId("POPUPURL" + i);
                iframe.setSrc(TBLCCPOPUP.getRow(i).getString("LOGINURL"));
            } else {
                component = new Vbox();
                Vbox vbox = (Vbox) component;
                vbox.setId("POPUPURL" + i);
                Import.importToParent(vbox, TBLCCPOPUP.getRow(i).getString("LOGINURL").substring("form://".length()), null);
            }
            agent.externalCRMPOPUPTYPE.add(type);
            agent.externalCRMPOPUPURL.add(TBLCCPOPUP.getRow(i).getString("POPUPURL"));
            agent.externalCRMAFTERCALLURL.add(TBLCCPOPUP.getRow(i).getString("AFTERCALLURL"));
            Tabpanel tabpanel = new Tabpanel();
            UiUtil.addComponent(tabpanel, component);
            crmTabbox.getTabpanels().getChildren().add(0, tabpanel);
        }
        crmTabbox.setSelectedIndex(0);
        Timer agentTimer = new Timer();
        agentTimer.setDelay(1000);
        agentTimer.setRepeats(true);
        agentTimer.addEventListener(Events.ON_TIMER, new EventListener() {

            public void onEvent(Event event) throws Exception {
                boolean agentNotReady = false;
                boolean agentBusy = false;
                Component parent = event.getTarget().getParent();
                if (UiUtil.getActiveForm().getDataset("TBLCRMCONTACTS") == null) {
                    return;
                }
                Agent agent = (Agent) UiUtil.getSessionAttribute("agent");
                if (agent == null) {
                    return;
                }
                if (!"background:green".equals(((Image) UiUtil.findComponent(parent, "stLINE")).getStyle())) {
                    ((Image) UiUtil.findComponent(parent, "stLINE")).setStyle("background:green");
                }
                String status = Converter.asNullableString(new Command("SELECT STATUS FROM TBLCCAGENTLOG WHERE 1=1 AND UNITUID={UNITUID} AND LOGTYPE='READY' AND UID={UID} AND ENDDATE IS NULL").executeScalar(DataColumnType.STRING));
                if ("NOTREADY".equals(status)) {
                    agentNotReady = true;
                    if (!"background:red".equals(((Image) UiUtil.findComponent(parent, "stREADY")).getStyle())) {
                        ((Image) UiUtil.findComponent(parent, "stREADY")).setStyle("background:red");
                    }
                    return;
                } else {
                    if (!"background:green".equals(((Image) UiUtil.findComponent(parent, "stREADY")).getStyle())) {
                        ((Image) UiUtil.findComponent(parent, "stREADY")).setStyle("background:green");
                    }
                }
                agentBusy = isAgentBusy(parent, agent);
                if ((!agentBusy) && (!agentNotReady) && (agent.chatEnabled)) {
                    Command command = new Command("UPDATE TBLCCCHAT SET AGENT={UID} WHERE CHATID IN (");
                    command.append("SELECT A.CHATID FROM TBLCCCHAT A WHERE A.UNITUID={UNITUID} AND A.BEGINDATE IN (");
                    command.append("SELECT MIN(B.BEGINDATE) FROM TBLCCCHAT B WHERE B.UNITUID={UNITUID} AND B.ENDDATE IS NULL AND B.AGENT IS NULL))");
                    Timestamp lastChatENDDATE = new Command("SELECT MAX(ENDDATE) FROM TBLCCCHAT WHERE UNITUID={UNITUID} AND ENDDATE IS NOT NULL AND AGENT={UID}").executeScalarAsDate();
                    if (lastChatENDDATE != null) {
                        double milliseconds = DateUtil.now().getTime() - lastChatENDDATE.getTime();
                        double seconds = milliseconds / 1000;
                        if (seconds >= 3) {
                            command.executeNonQuery();
                        }
                    } else {
                        command.executeNonQuery();
                    }
                }
                agentBusy = isAgentBusy(parent, agent);
                if ((!agentBusy) && (!agentNotReady) && (agent.emailEnabled)) {
                    Command command = new Command("UPDATE TBLCRMEMAIL SET AGENT={UID} WHERE EMAILID IN (");
                    command.append("SELECT A.EMAILID FROM TBLCRMEMAIL A WHERE A.UNITUID={UNITUID} AND A.RECEIVED IN (");
                    command.append("SELECT MIN(B.RECEIVED) FROM TBLCRMEMAIL B WHERE B.UNITUID={UNITUID} AND B.ENDDATE IS NULL AND B.AGENT IS NULL))");
                    Timestamp lastEmailENDDATE = new Command("SELECT MAX(ENDDATE) FROM TBLCRMEMAIL WHERE UNITUID={UNITUID} AND ENDDATE IS NOT NULL AND AGENT={UID}").executeScalarAsDate();
                    if (lastEmailENDDATE != null) {
                        double milliseconds = DateUtil.now().getTime() - lastEmailENDDATE.getTime();
                        double seconds = milliseconds / 1000;
                        if (seconds >= 3) {
                            command.executeNonQuery();
                        }
                    } else {
                        command.executeNonQuery();
                    }
                }
                agentBusy = isAgentBusy(parent, agent);
                if ((!agentBusy) && (!agentNotReady) && (agent.campEnabled)) {
                    int wrapuptime = Converter.asDecimal(ContactCenter.getParameter("cc_camp.wrapup")).intValue();
                    boolean execute = false;
                    Timestamp lastCampENDDATE = new Command("SELECT MAX(MODDATE) FROM TBLCCCAMPCONT WHERE UNITUID={UNITUID} AND AGENT={UID}").executeScalarAsDate();
                    if (lastCampENDDATE != null) {
                        double milliseconds = DateUtil.now().getTime() - lastCampENDDATE.getTime();
                        double seconds = milliseconds / 1000;
                        if (seconds >= wrapuptime) {
                            execute = true;
                        }
                    } else {
                        execute = true;
                    }
                    if (execute) {
                        execute = false;
                        lastCampENDDATE = new Command("SELECT MAX(MODDATE) FROM TBLCCAGENTLOG WHERE UNITUID={UNITUID} AND UID={UID} AND LOGTYPE='CAMP' ").executeScalarAsDate();
                        if (lastCampENDDATE != null) {
                            double milliseconds = DateUtil.now().getTime() - lastCampENDDATE.getTime();
                            double seconds = milliseconds / 1000;
                            if (seconds >= wrapuptime) {
                                execute = true;
                            }
                        } else {
                            execute = true;
                        }
                    }
                    Command command = new Command("UPDATE TBLCCCAMPCONT A SET A.AGENT={UID}, A.STATUS='PROGRESS' WHERE A.UNITUID={UNITUID} AND A.STATUS='SCHME' AND A.AGENT={UID} AND A.SCHEDULE<={NOW}");
                    command.append("AND A.CAMPAIGNID IN (SELECT B.CAMPAIGNID FROM TBLCCCAMPAIGN B WHERE B.UNITUID={UNITUID} AND B.CAMPAIGNID IN (SELECT C.CAMPAIGNID FROM TBLCCCAMPAGENT C WHERE C.UID={UID}) ");
                    command.append("AND B.STARTDATE< {NOW} AND B.ENDDATE>{NOW}");
                    command.append("AND B.STARTTIME<'" + Converter.asNotNullFormattedString(DateUtil.now(), "HHmm") + "'");
                    command.append("AND B.ENDTIME>'" + Converter.asNotNullFormattedString(DateUtil.now(), "HHmm") + "'");
                    command.append("AND B.ISACTIVE='true' AND B.CAMPTYPE='ATTENDED')");
                    command.append("AND A.CONTID=(SELECT MAX(D.CONTID) FROM TBLCCCAMPCONT D WHERE D.CAMPAIGNID=A.CAMPAIGNID AND D.STATUS='SCHME' AND D.SCHEDULE<={NOW})");
                    int update = 0;
                    if (execute) {
                        update = command.executeNonQuery();
                    }
                    if (update == 0) {
                        command = new Command("UPDATE TBLCCCAMPCONT A SET A.AGENT={UID}, A.STATUS='PROGRESS' WHERE A.UNITUID={UNITUID} AND A.STATUS='SCHMEORALL' AND A.SCHEDULE<={NOW}");
                        command.append("AND A.AGENT NOT IN (SELECT L.UID FROM TBLCCAGENTLOG L WHERE 1=1 AND L.UNITUID={UNITUID} AND L.LOGTYPE='LOGIN' AND L.ENDDATE IS NULL) ");
                        command.append("AND A.CAMPAIGNID IN (SELECT B.CAMPAIGNID FROM TBLCCCAMPAIGN B WHERE B.UNITUID={UNITUID} AND B.CAMPAIGNID IN (SELECT C.CAMPAIGNID FROM TBLCCCAMPAGENT C WHERE C.UID={UID}) ");
                        command.append("AND B.STARTDATE< {NOW} AND B.ENDDATE>{NOW}");
                        command.append("AND B.STARTTIME<'" + Converter.asNotNullFormattedString(DateUtil.now(), "HHmm") + "'");
                        command.append("AND B.ENDTIME>'" + Converter.asNotNullFormattedString(DateUtil.now(), "HHmm") + "'");
                        command.append("AND B.ISACTIVE='true' AND B.CAMPTYPE='ATTENDED')");
                        command.append("AND A.CONTID=(SELECT MAX(D.CONTID) FROM TBLCCCAMPCONT D WHERE D.CAMPAIGNID=A.CAMPAIGNID AND D.STATUS='SCHMEORALL' AND D.SCHEDULE<={NOW})");
                        update = 0;
                        if (execute) {
                            update = command.executeNonQuery();
                        }
                        if (update == 0) {
                            command = new Command("UPDATE TBLCCCAMPCONT A SET A.AGENT={UID}, A.STATUS='PROGRESS' WHERE A.UNITUID={UNITUID} AND A.STATUS='SCHEDULED' AND A.SCHEDULE<={NOW}");
                            command.append("AND A.CAMPAIGNID IN (SELECT B.CAMPAIGNID FROM TBLCCCAMPAIGN B WHERE B.UNITUID={UNITUID} AND B.CAMPAIGNID IN (SELECT C.CAMPAIGNID FROM TBLCCCAMPAGENT C WHERE C.UID={UID}) ");
                            command.append("AND B.STARTDATE< {NOW} AND B.ENDDATE>{NOW}");
                            command.append("AND B.STARTTIME<'" + Converter.asNotNullFormattedString(DateUtil.now(), "HHmm") + "'");
                            command.append("AND B.ENDTIME>'" + Converter.asNotNullFormattedString(DateUtil.now(), "HHmm") + "'");
                            command.append("AND B.ISACTIVE='true' AND B.CAMPTYPE='ATTENDED')");
                            command.append("AND A.CONTID=(SELECT MAX(D.CONTID) FROM TBLCCCAMPCONT D WHERE D.CAMPAIGNID=A.CAMPAIGNID AND D.STATUS='SCHEDULED' AND D.SCHEDULE<={NOW})");
                            update = 0;
                            if (execute) {
                                update = command.executeNonQuery();
                            }
                            if (update == 0) {
                                command = new Command("SELECT A.* FROM TBLCCCAMPAGENT A WHERE A.UNITUID={UNITUID} AND A.UID={UID} ");
                                command.append("AND A.CAMPAIGNID IN (SELECT B.CAMPAIGNID FROM TBLCCCAMPAIGN B WHERE B.UNITUID={UNITUID}  ");
                                command.append("AND B.STARTDATE< {NOW} AND B.ENDDATE>{NOW}");
                                command.append("AND B.STARTTIME<'" + Converter.asNotNullFormattedString(DateUtil.now(), "HHmm") + "'");
                                command.append("AND B.ENDTIME>'" + Converter.asNotNullFormattedString(DateUtil.now(), "HHmm") + "'");
                                command.append("AND B.ISACTIVE='true' AND B.CAMPTYPE='ATTENDED') ");
                                command.append("AND A.CAMPAIGNID IN (SELECT C.CAMPAIGNID FROM TBLCCCAMPCONT C WHERE C.UNITUID={UNITUID}");
                                command.append("AND C.CAMPAIGNID=A.CAMPAIGNID AND ((C.STATUS IS NULL) OR  (C.STATUS='IVRSCHEDULED')OR  (C.STATUS='PROGRESS'))");
                                command.append(")");
                                agent.TBLCCCAMPAGENT.clear();
                                if (execute) {
                                    agent.TBLCCCAMPAGENT.fill(command);
                                }
                                if (agent.TBLCCCAMPAGENT.getRowCount() > 0) {
                                    DataRow rowTBLCCAGENTLOG = agent.TBLCCAGENTLOG.addNewDataRow();
                                    rowTBLCCAGENTLOG.setString("UID", UiUtil.getUID());
                                    rowTBLCCAGENTLOG.setString("LOGTYPE", "CAMP");
                                    rowTBLCCAGENTLOG.setString("STATUS", "READY");
                                    rowTBLCCAGENTLOG.setTimestamp("BEGINDATE", DateUtil.now());
                                    rowTBLCCAGENTLOG.setString("REASON", agent.TBLCCCAMPAGENT.getRow(0).getString("CAMPAIGNID"));
                                    rowTBLCCAGENTLOG.setString("INTERFACE", agent.getINTERFACE());
                                    rowTBLCCAGENTLOG.setString("RINTERFACE", agent.getREMOTEINTERFACE());
                                    agent.TBLCCAGENTLOG.save();
                                    agent.activeCAMPAIGNID = agent.TBLCCCAMPAGENT.getRow(0).getString("CAMPAIGNID");
                                    OriginateAction originateAction = new OriginateAction();
                                    originateAction.setChannel(agent.getINTERFACE());
                                    originateAction.setContext("tegsoft-INTERNAL");
                                    originateAction.setExten("9999");
                                    originateAction.setPriority(new Integer("1"));
                                    originateAction.setVariable("CAMPAIGNID", agent.TBLCCCAMPAGENT.getRow(0).getString("CAMPAIGNID"));
                                    originateAction.setVariable("CAMPWAITING", agent.TBLCCCAMPAGENT.getRow(0).getString("CAMPAIGNID"));
                                    originateAction.setVariable("UID", UiUtil.getUID());
                                    agent.getManagerConnection(agent.getPBXID()).sendAction(originateAction, 20000);
                                }
                            }
                        }
                    }
                    if (update > 0) {
                        command = new Command("SELECT A.UNITUID,A.CAMPAIGNID,A.CONTID,A.STATUS,A.RESULT,A.CALLRESULT,A.SCHEDULE,A.MODUID,A.MODDATE,B.PHONE1,B.PHONE2,B.PHONE3,B.PHONE4");
                        command.append("FROM TBLCCCAMPCONT A,TBLCRMCONTACTS B WHERE 1=1 AND A.UNITUID={UNITUID} AND A.UNITUID=B.UNITUID AND A.CONTID=B.CONTID AND A.AGENT={UID} AND A.STATUS='PROGRESS' ");
                        agent.TBLCCCAMPCONT.fill(command);
                        if (agent.TBLCCCAMPCONT.getRowCount() > 0) {
                            String PHONE = agent.TBLCCCAMPCONT.getRow(0).getString("PHONE1");
                            if (NullStatus.isNull(PHONE)) {
                                PHONE = agent.TBLCCCAMPCONT.getRow(0).getString("PHONE2");
                            }
                            if (NullStatus.isNull(PHONE)) {
                                PHONE = agent.TBLCCCAMPCONT.getRow(0).getString("PHONE3");
                            }
                            if (NullStatus.isNull(PHONE)) {
                                PHONE = agent.TBLCCCAMPCONT.getRow(0).getString("PHONE4");
                            }
                            if (NullStatus.isNotNull(PHONE)) {
                                PHONE = StringUtil.replaceString(PHONE, " ", "");
                                PHONE = StringUtil.replaceString(PHONE, "(", "");
                                PHONE = StringUtil.replaceString(PHONE, ")", "");
                                PHONE = StringUtil.replaceString(PHONE, "+9", "");
                                agent.TBLCCCAMPCONT.getRow(0).setString("PHONE", PHONE);
                                agent.TBLCCCAMPCONT.save(true);
                                if (!"SKIPPED".equals(agent.TBLCCCAMPCONT.getRow(0).getString("RESULT"))) {
                                    agent.pause(true);
                                    OriginateAction originateAction = new OriginateAction();
                                    originateAction.setChannel(agent.getINTERFACE());
                                    originateAction.setContext("tegsoft-INTERNAL");
                                    String prefix = new Command("SELECT CALLCHANNEL FROM TBLCCCAMPAIGN WHERE UNITUID={UNITUID} AND CAMPAIGNID='" + agent.TBLCCCAMPCONT.getRow(0).getString("CAMPAIGNID") + "'").executeScalarAsString();
                                    if (NullStatus.isNull(prefix)) {
                                        originateAction.setExten(PHONE);
                                    } else {
                                        originateAction.setExten(prefix + PHONE);
                                    }
                                    originateAction.setPriority(new Integer("1"));
                                    originateAction.setVariable("CAMPAIGNID", agent.TBLCCCAMPCONT.getRow(0).getString("CAMPAIGNID"));
                                    originateAction.setVariable("CONTID", agent.TBLCCCAMPCONT.getRow(0).getString("CONTID"));
                                    originateAction.setVariable("UID", UiUtil.getUID());
                                    originateAction.setVariable("EXTID", agent.getEXTID());
                                    originateAction.setVariable("PHONE", PHONE);
                                    agent.getManagerConnection(agent.getPBXID()).sendAction(originateAction, 20000);
                                }
                            }
                        }
                    }
                }
                agentBusy = isAgentBusy(parent, agent);
                if (agentBusy) {
                    if (agent.chatEnabled) {
                        Dataset TBLCCCHAT = new Dataset("TBLCCCHAT", "TBLCCCHAT");
                        TBLCCCHAT.fill(new Command("SELECT * FROM TBLCCCHAT WHERE UNITUID={UNITUID} AND ENDDATE IS NULL AND AGENT={UID}"));
                        if (TBLCCCHAT.getRowCount() > 0) {
                            UiUtil.setSessionAttribute("CHATID", TBLCCCHAT.getRow(0).getString("CHATID"));
                            ((Textbox) UiUtil.findComponent(parent, "SUBJECT")).setValue(TBLCCCHAT.getRow(0).getString("SUBJECT"));
                            ((Textbox) UiUtil.findComponent(parent, "chatMSG")).setValue(TBLCCCHAT.getRow(0).getString("MESSAGE"));
                            if (!((Component) UiUtil.findComponent(parent, "webChatWindow")).isVisible()) {
                                ((Textbox) UiUtil.findComponent(parent, "srcENTRY")).setText(TBLCCCHAT.getRow(0).getString("EMAIL"));
                                UiUtil.getDataset("TBLCRMCONTACTS").reFill();
                                agent.pause(true);
                                popupExternal(parent, agent, "WEBCHAT", TBLCCCHAT.getRow(0).getString("EMAIL"));
                                ((Component) UiUtil.findComponent(parent, "idleWindow")).setVisible(false);
                                ((Component) UiUtil.findComponent(parent, "campWaitWindow")).setVisible(false);
                                ((Component) UiUtil.findComponent(parent, "campWindow")).setVisible(false);
                                ((Component) UiUtil.findComponent(parent, "emailWindow")).setVisible(false);
                                ((Component) UiUtil.findComponent(parent, "voiceWindow")).setVisible(false);
                                ((Component) UiUtil.findComponent(parent, "webChatWindow")).setVisible(true);
                                return;
                            }
                            return;
                        }
                    }
                    if (agent.emailEnabled) {
                        Dataset TBLCRMEMAIL = new Dataset("TBLCRMEMAIL", "TBLCRMEMAIL");
                        TBLCRMEMAIL.fill(new Command("SELECT * FROM TBLCRMEMAIL WHERE UNITUID={UNITUID} AND ENDDATE IS NULL AND AGENT={UID}"));
                        if (TBLCRMEMAIL.getRowCount() > 0) {
                            ((Textbox) UiUtil.findComponent(parent, "SUBJECT")).setText(null);
                            ((Textbox) UiUtil.findComponent(parent, "chatMSG")).setText(null);
                            UiUtil.setSessionAttribute("EMAILID", TBLCRMEMAIL.getRow(0).getString("EMAIL"));
                            ((Textbox) UiUtil.findComponent(parent, "EMAILSUBJECT")).setValue(TBLCRMEMAIL.getRow(0).getString("SUBJECT"));
                            ((Textbox) UiUtil.findComponent(parent, "EMAILCONTENT")).setValue(TBLCRMEMAIL.getRow(0).getString("CONTENT"));
                            if (!((Component) UiUtil.findComponent(parent, "emailWindow")).isVisible()) {
                                ((Textbox) UiUtil.findComponent(parent, "srcENTRY")).setText(TBLCRMEMAIL.getRow(0).getString("EMAIL"));
                                UiUtil.getDataset("TBLCRMCONTACTS").reFill();
                                agent.pause(true);
                                popupExternal(parent, agent, "EMAIL", TBLCRMEMAIL.getRow(0).getString("EMAIL"));
                                ((Component) UiUtil.findComponent(parent, "idleWindow")).setVisible(false);
                                ((Component) UiUtil.findComponent(parent, "campWaitWindow")).setVisible(false);
                                ((Component) UiUtil.findComponent(parent, "campWindow")).setVisible(false);
                                ((Component) UiUtil.findComponent(parent, "voiceWindow")).setVisible(false);
                                ((Component) UiUtil.findComponent(parent, "webChatWindow")).setVisible(false);
                                ((Component) UiUtil.findComponent(parent, "emailWindow")).setVisible(true);
                                return;
                            }
                            return;
                        }
                    }
                    if (agent.campEnabled) {
                        agent.TBLCCCAMPCONT.fill(new Command("SELECT * FROM TBLCCCAMPCONT WHERE UNITUID={UNITUID} AND AGENT={UID} AND STATUS='PROGRESS'"));
                        if (agent.TBLCCCAMPCONT.getRowCount() > 0) {
                            if (!((Component) UiUtil.findComponent(parent, "campWindow")).isVisible()) {
                                ((Textbox) UiUtil.findComponent(parent, "CAMPAIGNID")).setValue(agent.TBLCCCAMPCONT.getRow(0).getString("CAMPAIGNID"));
                                ((Textbox) UiUtil.findComponent(parent, "CONTID")).setValue(agent.TBLCCCAMPCONT.getRow(0).getString("CONTID"));
                                UiUtil.getDbCommand("TBLCCCAMPCONT").getReservedWordList().put("CAMPAIGNID", agent.TBLCCCAMPCONT.getRow(0).getString("CAMPAIGNID"));
                                UiUtil.getDbCommand("TBLCCCAMPCONT").getReservedWordList().put("CONTID", agent.TBLCCCAMPCONT.getRow(0).getString("CONTID"));
                                UiUtil.getDataset("TBLCCCAMPCONT").fill(UiUtil.getDbCommand("TBLCCCAMPCONT"));
                                UiUtil.getDataset("TBLCCCAMPAIGN").fill(UiUtil.getDbCommand("TBLCCCAMPAIGN"));
                                ((Textbox) UiUtil.findComponent(parent, "srcENTRY")).setText(agent.TBLCCCAMPCONT.getRow(0).getString("CONTID"));
                                UiUtil.getDataset("TBLCRMCONTACTS").reFill();
                                agent.pause(true);
                                popupExternal(parent, agent, "CAMP", agent.TBLCCCAMPCONT.getRow(0).getString("CONTID"));
                                agent.activeCAMPAIGNID = agent.TBLCCCAMPCONT.getRow(0).getString("CAMPAIGNID");
                                Command command = new Command("SELECT B.NAME,C.FIRSTNAME,C.LASTNAME,A.PHONE,A.CALLDATE,A.DIALRESULT,A.HANGUP FROM TBLCCCAMPCALLD A,TBLCCCAMPCONT D,TBLCCCAMPAIGN B,TBLCRMCONTACTS C");
                                command.append("WHERE A.UNITUID={UNITUID} AND A.UNITUID=B.UNITUID AND A.CAMPAIGNID=B.CAMPAIGNID ");
                                command.append("AND A.UNITUID=C.UNITUID AND A.CONTID=C.CONTID AND A.STATUS='PROGRESS' ");
                                command.append("AND A.UNITUID=D.UNITUID AND A.CAMPAIGNID=D.CAMPAIGNID AND A.CONTID=D.CONTID AND D.STATUS='PROGRESS' ");
                                command.append("AND A.CAMPAIGNID=");
                                command.bind(agent.activeCAMPAIGNID);
                                command.append("ORDER BY A.CALLDATE");
                                UiUtil.getDataset("TBLCCCAMPCALLD2").fill(command);
                                ((Component) UiUtil.findComponent(parent, "idleWindow")).setVisible(false);
                                ((Component) UiUtil.findComponent(parent, "campWaitWindow")).setVisible(false);
                                ((Component) UiUtil.findComponent(parent, "webChatWindow")).setVisible(false);
                                ((Component) UiUtil.findComponent(parent, "emailWindow")).setVisible(false);
                                ((Component) UiUtil.findComponent(parent, "voiceWindow")).setVisible(false);
                                ((Component) UiUtil.findComponent(parent, "campWindow")).setVisible(true);
                                return;
                            }
                            return;
                        }
                        BigDecimal activeCampReg = new Command("SELECT COUNT(*) FROM TBLCCAGENTLOG WHERE UNITUID={UNITUID} AND UID={UID} AND LOGTYPE='CAMP' AND ENDDATE IS NULL").executeScalarAsDecimal();
                        if (activeCampReg.intValue() > 0) {
                            Command command = new Command("SELECT B.NAME,C.FIRSTNAME,C.LASTNAME,A.PHONE,A.CALLDATE,A.DIALRESULT,A.HANGUP FROM TBLCCCAMPCALLD A,TBLCCCAMPAIGN B,TBLCRMCONTACTS C");
                            command.append("WHERE A.UNITUID={UNITUID} AND A.UNITUID=B.UNITUID AND A.CAMPAIGNID=B.CAMPAIGNID ");
                            command.append("AND A.UNITUID=C.UNITUID AND A.CONTID=C.CONTID AND A.STATUS='PROGRESS' ");
                            command.append("AND A.CAMPAIGNID=");
                            command.bind(agent.activeCAMPAIGNID);
                            command.append("ORDER BY A.CALLDATE");
                            UiUtil.getDataset("TBLCCCAMPCALLD2").fill(command);
                            if (!((Component) UiUtil.findComponent(parent, "campWaitWindow")).isVisible()) {
                                agent.pause(true);
                                ((Component) UiUtil.findComponent(parent, "idleWindow")).setVisible(false);
                                ((Component) UiUtil.findComponent(parent, "webChatWindow")).setVisible(false);
                                ((Component) UiUtil.findComponent(parent, "emailWindow")).setVisible(false);
                                ((Component) UiUtil.findComponent(parent, "voiceWindow")).setVisible(false);
                                ((Component) UiUtil.findComponent(parent, "campWindow")).setVisible(false);
                                ((Component) UiUtil.findComponent(parent, "campWaitWindow")).setVisible(true);
                                return;
                            }
                            return;
                        }
                    }
                    if (agent.voiceEnabled) {
                        agent.TBLCCAGENTLOG.fill(new Command("SELECT * FROM TBLCCAGENTLOG WHERE UNITUID={UNITUID} AND UID={UID} AND LOGTYPE='BUSY' AND CALLID IS NOT NULL AND ENDDATE IS NULL AND REASON='DEFAULT'"));
                        if (agent.TBLCCAGENTLOG.getRowCount() > 0) {
                            agent.LOGID = agent.TBLCCAGENTLOG.getRow(0).getString("LOGID");
                            agent.TBLCCAGENTLOG.getRow(0).setString("SESSIONID", UiUtil.getSessionId());
                            agent.TBLCCAGENTLOG.getRow(0).setString("REASON", "POPUP");
                            agent.TBLCCAGENTLOG.getRow(0).setString("INTERFACE", agent.getINTERFACE());
                            agent.TBLCCAGENTLOG.getRow(0).setString("RINTERFACE", agent.getREMOTEINTERFACE());
                            agent.TBLCCAGENTLOG.save();
                            String PHONE = agent.TBLCCAGENTLOG.getRow(0).getString("PHONE");
                            ((Textbox) UiUtil.findComponent(parent, "srcENTRY")).setText(PHONE);
                            UiUtil.getDataset("TBLCRMCONTACTS").reFill();
                            agent.displayChannelVariables();
                            popupExternal(parent, agent, "VOICE", PHONE);
                            Tabbox crmTabbox = (Tabbox) UiUtil.findComponent("crmTabbox");
                            crmTabbox.setSelectedIndex(0);
                            ((Component) UiUtil.findComponent(parent, "idleWindow")).setVisible(false);
                            ((Component) UiUtil.findComponent(parent, "campWaitWindow")).setVisible(false);
                            ((Component) UiUtil.findComponent(parent, "campWindow")).setVisible(false);
                            ((Component) UiUtil.findComponent(parent, "webChatWindow")).setVisible(false);
                            ((Component) UiUtil.findComponent(parent, "emailWindow")).setVisible(false);
                            ((Component) UiUtil.findComponent(parent, "voiceWindow")).setVisible(true);
                            return;
                        }
                    }
                } else {
                    if (!((Component) UiUtil.findComponent(parent, "idleWindow")).isVisible()) {
                        UiUtil.getDataset("CHANNELVARS").clear();
                        ((Textbox) UiUtil.findComponent(parent, "srcENTRY")).setText(null);
                        UiUtil.getDataset("TBLCRMCONTACTS").reFill();
                        ((Textbox) UiUtil.findComponent(parent, "SUBJECT")).setText(null);
                        ((Textbox) UiUtil.findComponent(parent, "chatMSG")).setText(null);
                        popupExternalAfterCall(parent, agent);
                        Tabbox crmTabbox = (Tabbox) UiUtil.findComponent("crmTabbox");
                        crmTabbox.setSelectedIndex(0);
                        ((Component) UiUtil.findComponent(parent, "campWindow")).setVisible(false);
                        ((Component) UiUtil.findComponent(parent, "campWaitWindow")).setVisible(false);
                        ((Component) UiUtil.findComponent(parent, "webChatWindow")).setVisible(false);
                        ((Component) UiUtil.findComponent(parent, "emailWindow")).setVisible(false);
                        ((Component) UiUtil.findComponent(parent, "voiceWindow")).setVisible(true);
                        ((Component) UiUtil.findComponent(parent, "idleWindow")).setVisible(true);
                        if (!agentNotReady) {
                            agent.pause(false);
                        }
                    }
                }
            }
        });
        UiUtil.addComponent(UiUtil.getActiveForm(), agentTimer);
        agentTimer.start();
    }

    private ManagerConnection getManagerConnection(String PBXID) throws Exception {
        if (NullStatus.isNull(PBXID)) {
            PBXID = TegsoftPBX.getLocalPBXID();
        }
        ManagerConnection managerConnection = managerConnections.get(PBXID);
        if (managerConnection != null) {
            if (managerConnection.getState() != ManagerConnectionState.CONNECTED) {
                managerConnection.disconnect();
                managerConnections.put(PBXID, TegsoftPBX.createManagerConnection(managerConnection.getPBXID()));
            }
            return managerConnection;
        }
        return null;
    }

    private void createManagerConnections() throws Exception {
        boolean interfaceConnectionCreated = false;
        String interfacePBXID = getPBXID();
        if (NullStatus.isNull(interfacePBXID)) {
            interfacePBXID = TegsoftPBX.getLocalPBXID();
        }
        Dataset TBLCCSKILLS = new Dataset("TBLCCSKILLS", "TBLCCSKILLS");
        TBLCCSKILLS.fill(new Command("SELECT * FROM TBLCCSKILLS WHERE UNITUID={UNITUID} AND PBXID IN (SELECT PBXID FROM TBLPBX WHERE UNITUID={UNITUID})"));
        for (int i = 0; i < TBLCCSKILLS.getRowCount(); i++) {
            if (getManagerConnection(TBLCCSKILLS.getRow(i).getString("PBXID")) == null) {
                ManagerConnection managerConnection = TegsoftPBX.createManagerConnection(TBLCCSKILLS.getRow(i).getString("PBXID"));
                managerConnection.setPBXID(TBLCCSKILLS.getRow(i).getString("PBXID"));
                managerConnections.put(TBLCCSKILLS.getRow(i).getString("PBXID"), managerConnection);
                if (Compare.equal(TBLCCSKILLS.getRow(i).getString("PBXID"), interfacePBXID)) {
                    interfaceConnectionCreated = true;
                }
            }
        }
        if (!interfaceConnectionCreated) {
            if (getManagerConnection(interfacePBXID) == null) {
                ManagerConnection managerConnection = TegsoftPBX.createManagerConnection(interfacePBXID);
                managerConnection.setPBXID(interfacePBXID);
                managerConnections.put(interfacePBXID, managerConnection);
            }
        }
    }

    private void closeManagerConnections() throws Exception {
        for (String pbxid : managerConnections.keySet()) {
            try {
                if (managerConnections.get(pbxid) != null) {
                    managerConnections.get(pbxid).disconnect();
                }
            } catch (Exception ignored) {
                ignored.printStackTrace();
            }
        }
    }

    private void displayChannelVariables() throws Exception {
        UiUtil.getDataset("CHANNELVARS").clear();
        ManagerConnection managerConnection = getManagerConnection(getPBXID());
        managerConnection.prepareChannels();
        for (int i = 0; i < managerConnection.getChannels().size(); i++) {
            StatusEvent statusEvent = managerConnection.getChannels().get(i);
            String MEMBERINTERFACE = TegsoftPBX.getChannelVariable(managerConnection, statusEvent, "MEMBERINTERFACE");
            if (NullStatus.isNull(MEMBERINTERFACE)) {
                continue;
            }
            if (Compare.equal(MEMBERINTERFACE, getINTERFACE())) {
                String CCRESULT = TegsoftPBX.getChannelVariable(managerConnection, statusEvent, "CCRESULT");
                if (NullStatus.isNotNull(CCRESULT)) {
                    DataRow dataRow = UiUtil.getDataset("CHANNELVARS").addNewDataRow();
                    dataRow.set("KEYNAME", MessageUtil.getMessage(Agent.class, Messages.CCRESULT));
                    if ("ERROR".equals(CCRESULT)) {
                        dataRow.set("KEYVALUE", MessageUtil.getMessage(Agent.class, Messages.ERROR));
                    } else if ("SUCCESS".equals(CCRESULT)) {
                        dataRow.set("KEYVALUE", MessageUtil.getMessage(Agent.class, Messages.SUCCESS));
                    }
                }
                String QUEUENAME = TegsoftPBX.getChannelVariable(managerConnection, statusEvent, "QUEUENAME");
                if (NullStatus.isNotNull(QUEUENAME)) {
                    DataRow dataRow = UiUtil.getDataset("CHANNELVARS").addNewDataRow();
                    dataRow.set("KEYNAME", MessageUtil.getMessage(Agent.class, Messages.QUEUENAME));
                    if (NullStatus.isNotNull(QUEUENAME)) {
                        Command command = new Command("SELECT NAME FROM TBLCCSKILLS WHERE UNITUID={UNITUID} AND PBXID IN (SELECT PBXID FROM TBLPBX WHERE UNITUID={UNITUID}) AND SKILL=");
                        command.bind(QUEUENAME);
                        QUEUENAME += " " + command.executeScalarAsString();
                        dataRow.set("KEYVALUE", QUEUENAME);
                    }
                }
                String QEHOLDTIME = TegsoftPBX.getChannelVariable(managerConnection, statusEvent, "QEHOLDTIME");
                if (NullStatus.isNotNull(QEHOLDTIME)) {
                    DataRow dataRow = UiUtil.getDataset("CHANNELVARS").addNewDataRow();
                    dataRow.set("KEYNAME", MessageUtil.getMessage(Agent.class, Messages.QEHOLDTIME));
                    dataRow.set("KEYVALUE", QEHOLDTIME);
                }
                String QEORIGINALPOS = TegsoftPBX.getChannelVariable(managerConnection, statusEvent, "QEORIGINALPOS");
                if (NullStatus.isNotNull(QEORIGINALPOS)) {
                    DataRow dataRow = UiUtil.getDataset("CHANNELVARS").addNewDataRow();
                    dataRow.set("KEYNAME", MessageUtil.getMessage(Agent.class, Messages.QEORIGINALPOS));
                    dataRow.set("KEYVALUE", QEORIGINALPOS);
                }
                String QUEUECALLS = TegsoftPBX.getChannelVariable(managerConnection, statusEvent, "QUEUECALLS");
                if (NullStatus.isNotNull(QUEUECALLS)) {
                    DataRow dataRow = UiUtil.getDataset("CHANNELVARS").addNewDataRow();
                    dataRow.set("KEYNAME", MessageUtil.getMessage(Agent.class, Messages.QUEUECALLS));
                    dataRow.set("KEYVALUE", QUEUECALLS);
                }
                ArrayList<String> ctiVariables = new ArrayList<String>();
                for (int k = 0; k < 100; k++) {
                    String index = "" + k;
                    if (index.length() == 1) {
                        index = "0" + index;
                    }
                    String ctiVariable = TegsoftPBX.getChannelVariable(managerConnection, statusEvent, "tegsoftCTI" + index);
                    if (NullStatus.isNull(ctiVariable)) {
                        break;
                    }
                    ctiVariables.add(ctiVariable);
                }
                for (int k = 0; k < ctiVariables.size(); k++) {
                    String ctiVariableValue = TegsoftPBX.getChannelVariable(managerConnection, statusEvent, ctiVariables.get(k));
                    if (NullStatus.isNotNull(ctiVariableValue)) {
                        DataRow dataRow = UiUtil.getDataset("CHANNELVARS").addNewDataRow();
                        dataRow.set("KEYNAME", ctiVariables.get(k));
                        dataRow.set("KEYVALUE", ctiVariableValue);
                    }
                }
                return;
            }
        }
    }

    private static void popupExternal(Component parent, Agent agent, String CONTTYPE, String PHONE) throws Exception {
        for (int i = 0; i < agent.externalCRMCount; i++) {
            String popup_url = agent.externalCRMPOPUPURL.get(i);
            popup_url = StringUtil.replaceString(popup_url, "{CONTTYPE}", CONTTYPE);
            popup_url = StringUtil.replaceString(popup_url, "{PHONE}", PHONE);
            Dataset CHANNELVARS = UiUtil.getDataset("CHANNELVARS");
            for (int k = 0; k < CHANNELVARS.getRowCount(); k++) {
                DataRow rowCHANNELVARS = CHANNELVARS.getRow(k);
                popup_url = StringUtil.replaceString(popup_url, "{" + rowCHANNELVARS.getString("KEYNAME") + "}", rowCHANNELVARS.getString("KEYVALUE"));
            }
            if ("html".equals(agent.externalCRMPOPUPTYPE.get(i))) {
                ((Iframe) UiUtil.findComponent(parent, "POPUPURL" + i)).setSrc(popup_url);
            } else {
                if (NullStatus.isNotNull(popup_url)) {
                    if (popup_url.startsWith("form://")) {
                        Vbox vbox = ((Vbox) UiUtil.findComponent(parent, "POPUPURL" + i));
                        vbox.getChildren().clear();
                        Import.importToParent(vbox, popup_url.substring("form://".length()), null);
                    }
                }
            }
        }
    }

    private static void popupExternalAfterCall(Component parent, Agent agent) throws Exception {
        for (int i = 0; i < agent.externalCRMCount; i++) {
            String popup_url = agent.externalCRMAFTERCALLURL.get(i);
            if ("html".equals(agent.externalCRMPOPUPTYPE.get(i))) {
                ((Iframe) UiUtil.findComponent(parent, "POPUPURL" + i)).setSrc(popup_url);
            } else {
                if (NullStatus.isNotNull(popup_url)) {
                    if (popup_url.startsWith("form://")) {
                        Vbox vbox = ((Vbox) UiUtil.findComponent(parent, "POPUPURL" + i));
                        vbox.getChildren().clear();
                        Import.importToParent(vbox, popup_url.substring("form://".length()), null);
                    }
                }
            }
        }
    }

    private void pause(boolean pause) throws Exception {
        if (!voiceEnabled) {
            return;
        }
        if (!pause) {
            Dataset TBLCCSKILLAGENT = new Dataset("TBLCCSKILLAGENT", "TBLCCSKILLAGENT");
            TBLCCSKILLAGENT.addDataColumn("PBXID");
            TBLCCSKILLAGENT.fill(new Command("SELECT B.PBXID,A.SKILL,A.PRIORITY FROM TBLCCSKILLAGENT A,TBLCCSKILLS B WHERE A.UNITUID=B.UNITUID AND A.SKILL=B.SKILL AND A.UNITUID={UNITUID} AND A.UID={UID} AND B.PBXID IN (SELECT PBXID FROM TBLPBX WHERE UNITUID={UNITUID})"));
            for (int i = 0; i < TBLCCSKILLAGENT.getRowCount(); i++) {
                String INTERFACE = getINTERFACE();
                if (NullStatus.isNotNull(getPBXID())) {
                    if (Compare.equal(getPBXID(), TBLCCSKILLAGENT.getRow(i).getString("PBXID"))) {
                        INTERFACE = "SIP/" + getINTERFACE().substring(getINTERFACE().lastIndexOf("/") + 1);
                    }
                }
                getManagerConnection(TBLCCSKILLAGENT.getRow(i).getString("PBXID")).sendAction(new QueueAddAction(TBLCCSKILLAGENT.getRow(i).getString("SKILL"), INTERFACE, TBLCCSKILLAGENT.getRow(i).getDecimal("PRIORITY").intValue()), 10000);
            }
        } else {
            Dataset TBLCCSKILLAGENT = new Dataset("TBLCCSKILLAGENT", "TBLCCSKILLAGENT");
            TBLCCSKILLAGENT.addDataColumn("PBXID");
            TBLCCSKILLAGENT.fill(new Command("SELECT B.PBXID,A.SKILL,A.PRIORITY FROM TBLCCSKILLAGENT A,TBLCCSKILLS B WHERE A.UNITUID=B.UNITUID AND A.SKILL=B.SKILL AND A.UNITUID={UNITUID} AND B.PBXID IN (SELECT PBXID FROM TBLPBX WHERE UNITUID={UNITUID})"));
            for (int i = 0; i < TBLCCSKILLAGENT.getRowCount(); i++) {
                String INTERFACE = getINTERFACE();
                if (NullStatus.isNotNull(getPBXID())) {
                    if (Compare.equal(getPBXID(), TBLCCSKILLAGENT.getRow(i).getString("PBXID"))) {
                        INTERFACE = "SIP/" + getINTERFACE().substring(getINTERFACE().lastIndexOf("/") + 1);
                    }
                }
                getManagerConnection(TBLCCSKILLAGENT.getRow(i).getString("PBXID")).sendAction(new QueueRemoveAction(TBLCCSKILLAGENT.getRow(i).getString("SKILL"), INTERFACE), 10000);
            }
        }
    }

    private static boolean isAgentBusy(Component parent, Agent agent) throws Exception {
        boolean agentBusy = false;
        if (agent.voiceEnabled) {
            BigDecimal activeVoice = new Command("SELECT COUNT(*) FROM TBLCCAGENTLOG WHERE UNITUID={UNITUID} AND UID={UID} AND LOGTYPE='BUSY' AND CALLID IS NOT NULL AND ENDDATE IS NULL").executeScalarAsDecimal();
            if (activeVoice.intValue() > 0) {
                agentBusy = true;
                if (!"background:red".equals(((Image) UiUtil.findComponent(parent, "stLINE")).getStyle())) {
                    ((Image) UiUtil.findComponent(parent, "stLINE")).setStyle("background:red");
                }
                return agentBusy;
            }
        }
        if (agent.chatEnabled) {
            BigDecimal activeChat = new Command("SELECT COUNT(*) FROM TBLCCCHAT WHERE UNITUID={UNITUID} AND ENDDATE IS NULL AND AGENT={UID}").executeScalarAsDecimal();
            if (activeChat.intValue() > 0) {
                agentBusy = true;
                if (!"background:red".equals(((Image) UiUtil.findComponent(parent, "stLINE")).getStyle())) {
                    ((Image) UiUtil.findComponent(parent, "stLINE")).setStyle("background:red");
                }
                return agentBusy;
            }
        }
        if (agent.emailEnabled) {
            BigDecimal activeEmail = new Command("SELECT COUNT(*) FROM TBLCRMEMAIL WHERE UNITUID={UNITUID} AND ENDDATE IS NULL AND AGENT={UID}").executeScalarAsDecimal();
            if (activeEmail.intValue() > 0) {
                agentBusy = true;
                if (!"background:red".equals(((Image) UiUtil.findComponent(parent, "stLINE")).getStyle())) {
                    ((Image) UiUtil.findComponent(parent, "stLINE")).setStyle("background:red");
                }
                return agentBusy;
            }
        }
        if (agent.campEnabled) {
            BigDecimal activeCamp = new Command("SELECT COUNT(*) FROM TBLCCCAMPCONT WHERE UNITUID={UNITUID} AND STATUS='PROGRESS' AND AGENT={UID}").executeScalarAsDecimal();
            if (activeCamp.intValue() > 0) {
                agentBusy = true;
                if (!"background:red".equals(((Image) UiUtil.findComponent(parent, "stLINE")).getStyle())) {
                    ((Image) UiUtil.findComponent(parent, "stLINE")).setStyle("background:red");
                }
                return agentBusy;
            }
            BigDecimal activeCampReg = new Command("SELECT COUNT(*) FROM TBLCCAGENTLOG WHERE UNITUID={UNITUID} AND UID={UID} AND LOGTYPE='CAMP' AND ENDDATE IS NULL").executeScalarAsDecimal();
            if (activeCampReg.intValue() > 0) {
                agentBusy = true;
                if (!"background:red".equals(((Image) UiUtil.findComponent(parent, "stLINE")).getStyle())) {
                    ((Image) UiUtil.findComponent(parent, "stLINE")).setStyle("background:red");
                }
                return agentBusy;
            }
        }
        return agentBusy;
    }

    public static void logout() throws Exception {
        logout(null);
    }

    public static void logout(Event event) throws Exception {
        Agent agent = (Agent) UiUtil.getSessionAttribute("agent");
        if (agent == null) {
            return;
        }
        agent.pause(true);
        agent.closeManagerConnections();
        UiUtil.setSessionAttribute("agent", null);
        agent.TBLCCAGENTLOG.fill(new Command("SELECT * FROM TBLCCAGENTLOG WHERE 1=1 AND UNITUID={UNITUID} AND LOGTYPE='LOGIN' AND UID={UID} AND STATUS='LOGIN' AND ENDDATE IS NULL"));
        if (agent.TBLCCAGENTLOG.getRowCount() > 0) {
            agent.TBLCCAGENTLOG.getRow(0).setTimestamp("ENDDATE", DateUtil.now());
            agent.TBLCCAGENTLOG.getRow(0).setString("STATUS", "LOGOUT");
            agent.TBLCCAGENTLOG.save();
        }
        agent.TBLCCAGENTLOG.fill(new Command("SELECT * FROM TBLCCAGENTLOG WHERE 1=1 AND UNITUID={UNITUID} AND LOGTYPE='READY' AND UID={UID} AND STATUS='NOTREADY' AND ENDDATE IS NULL"));
        for (int i = 0; i < agent.TBLCCAGENTLOG.getRowCount(); i++) {
            agent.TBLCCAGENTLOG.getRow(0).setTimestamp("ENDDATE", DateUtil.now());
        }
        agent.TBLCCAGENTLOG.save();
    }

    public static void ready() throws Exception {
        ready(null);
    }

    public static void ready(Event event) throws Exception {
        Agent agent = (Agent) UiUtil.getSessionAttribute("agent");
        if (agent == null) {
            ((Component) UiUtil.findComponent("topElement")).setVisible(false);
            UiUtil.showMessage(MessageType.ERROR, MessageUtil.getMessage(Agent.class, Messages.agent_6));
            return;
        }
        agent.TBLCCAGENTLOG.fill(new Command("SELECT * FROM TBLCCAGENTLOG WHERE 1=1 AND UNITUID={UNITUID} AND LOGTYPE='READY' AND UID={UID} AND STATUS='NOTREADY' AND ENDDATE IS NULL"));
        if (agent.TBLCCAGENTLOG.getRowCount() > 0) {
            agent.TBLCCAGENTLOG.getRow(0).setTimestamp("ENDDATE", DateUtil.now());
            agent.TBLCCAGENTLOG.save();
        }
        agent.pause(false);
    }

    public static void notReady() throws Exception {
        notReady("AFTERLOGIN");
    }

    public static void notReady(Event event) throws Exception {
        String REASON = (String) UiUtil.getValue("NotReadyREASON", "getValue", null, null);
        if (NullStatus.isNotNull(REASON)) {
            notReady(REASON);
        }
    }

    private static void notReady(String REASON) throws Exception {
        Agent agent = (Agent) UiUtil.getSessionAttribute("agent");
        if (agent == null) {
            ((Component) UiUtil.findComponent("topElement")).setVisible(false);
            UiUtil.showMessage(MessageType.ERROR, MessageUtil.getMessage(Agent.class, Messages.agent_6));
            return;
        }
        agent.TBLCCAGENTLOG.fill(new Command("SELECT * FROM TBLCCAGENTLOG WHERE 1=1 AND UNITUID={UNITUID} AND LOGTYPE='READY' AND UID={UID} AND STATUS='NOTREADY' AND ENDDATE IS NULL"));
        if (agent.TBLCCAGENTLOG.getRowCount() > 0) {
            agent.TBLCCAGENTLOG.getRow(0).setTimestamp("ENDDATE", DateUtil.now());
            agent.TBLCCAGENTLOG.save();
        }
        DataRow rowTBLCCAGENTLOG = agent.TBLCCAGENTLOG.addNewDataRow();
        rowTBLCCAGENTLOG.setString("UID", UiUtil.getUID());
        rowTBLCCAGENTLOG.setString("LOGTYPE", "READY");
        rowTBLCCAGENTLOG.setString("STATUS", "NOTREADY");
        rowTBLCCAGENTLOG.setTimestamp("BEGINDATE", DateUtil.now());
        rowTBLCCAGENTLOG.setString("REASON", REASON);
        rowTBLCCAGENTLOG.setString("INTERFACE", agent.getINTERFACE());
        rowTBLCCAGENTLOG.setString("RINTERFACE", agent.getREMOTEINTERFACE());
        agent.TBLCCAGENTLOG.save();
        agent.pause(true);
    }

    public synchronized void hangupExtention() throws Exception {
        UiUtil.getDataset("CHANNELVARS").clear();
        ManagerConnection managerConnection = getManagerConnection(getPBXID());
        managerConnection.prepareChannels();
        for (int i = 0; i < managerConnection.getChannels().size(); i++) {
            StatusEvent statusEvent = managerConnection.getChannels().get(i);
            if (statusEvent.getChannel().startsWith(getINTERFACE())) {
                managerConnection.sendAction(new HangupAction(statusEvent.getChannel()), 5000);
            }
        }
    }

    public static void originate() throws Exception {
        originate(null);
    }

    public static void originate(Event event) throws Exception {
        Agent agent = (Agent) UiUtil.getSessionAttribute("agent");
        if (agent == null) {
            ((Component) UiUtil.findComponent("topElement")).setVisible(false);
            UiUtil.showMessage(MessageType.ERROR, MessageUtil.getMessage(Agent.class, Messages.agent_6));
            return;
        }
        if (agent != null) {
            if (agent.voiceEnabled) {
                if (((Component) UiUtil.findComponent("campWindow")).isVisible()) {
                    OriginateAction originateAction = new OriginateAction();
                    originateAction.setChannel(agent.getINTERFACE());
                    originateAction.setContext("tegsoft-INTERNAL");
                    originateAction.setPriority(new Integer("1"));
                    String prefix = new Command("SELECT CALLCHANNEL FROM TBLCCCAMPAIGN WHERE UNITUID={UNITUID} AND CAMPAIGNID='" + ((Textbox) UiUtil.findComponent("CAMPAIGNID")).getValue() + "'").executeScalarAsString();
                    if (NullStatus.isNull(prefix)) {
                        originateAction.setExten(((Textbox) UiUtil.findComponent("DialNumber")).getValue());
                    } else {
                        originateAction.setExten(prefix + ((Textbox) UiUtil.findComponent("DialNumber")).getValue());
                    }
                    originateAction.setVariable("CAMPAIGNID", ((Textbox) UiUtil.findComponent("CAMPAIGNID")).getValue());
                    originateAction.setVariable("CONTID", Converter.asNotNullString(UiUtil.getDataSourceValue("TBLCRMCONTACTS", "CONTID", null, null)));
                    originateAction.setVariable("UID", UiUtil.getUID());
                    originateAction.setVariable("EXTID", agent.getEXTID());
                    originateAction.setVariable("PHONE", ((Textbox) UiUtil.findComponent("DialNumber")).getValue());
                    agent.getManagerConnection(agent.getPBXID()).sendAction(originateAction, 20000);
                } else {
                    OriginateAction originateAction = new OriginateAction();
                    originateAction.setChannel(agent.getINTERFACE());
                    originateAction.setContext("tegsoft-INTERNAL");
                    originateAction.setExten(((Textbox) UiUtil.findComponent("DialNumber")).getValue());
                    originateAction.setPriority(new Integer("1"));
                    originateAction.setVariable("UID", UiUtil.getUID());
                    originateAction.setVariable("EXTID", agent.getEXTID());
                    originateAction.setVariable("PHONE", ((Textbox) UiUtil.findComponent("DialNumber")).getValue());
                    agent.getManagerConnection(agent.getPBXID()).sendAction(originateAction, 20000);
                }
            }
        }
    }

    public static void hangup() throws Exception {
        hangup(null);
    }

    public static void hangup(Event event) throws Exception {
        if (((Component) UiUtil.findComponent("campWindow")).isVisible()) {
            String RESULT = Converter.asNotNullString(UiUtil.getValue(UiUtil.findComponent("RESULT"), "ignored"));
            if (NullStatus.isNotNull(RESULT)) {
                Command command = new Command("UPDATE TBLCCCAMPCONT SET STATUS='CLOSED',AGENT={UID},MODUID={UID},MODDATE={NOW},RESULT=");
                command.bind(RESULT);
                command.append(",NOTES=");
                command.bind(((Textbox) UiUtil.findComponent("NOTES")).getValue());
                command.append("WHERE UNITUID={UNITUID} AND CAMPAIGNID=");
                command.bind(((Textbox) UiUtil.findComponent("CAMPAIGNID")).getValue());
                command.append("AND CONTID=");
                command.bind(((Textbox) UiUtil.findComponent("CONTID")).getValue());
                command.executeNonQuery();
                command = new Command("UPDATE TBLCCCAMPCALLD  SET STATUS='CLOSED' WHERE UNITUID={UNITUID} AND CAMPAIGNID=");
                command.bind(((Textbox) UiUtil.findComponent("CAMPAIGNID")).getValue());
                command.append("AND CONTID=");
                command.bind(((Textbox) UiUtil.findComponent("CONTID")).getValue());
                command.append("AND STATUS='PROGRESS' ");
                command.executeNonQuery(true);
                ((Datebox) UiUtil.findComponent("SCHEDULE")).setValue(null);
                ((Textbox) UiUtil.findComponent("NOTES")).setValue(null);
                ((Textbox) UiUtil.findComponent("CAMPAIGNID")).setValue(null);
                ((Textbox) UiUtil.findComponent("CONTID")).setValue(null);
                ((Combobox) UiUtil.findComponent("RESULT")).setSelectedIndex(0);
            } else {
                if (NullStatus.isNotNull(((Datebox) UiUtil.findComponent("SCHEDULE")).getValue())) {
                    String STATUS = (String) UiUtil.getValue("STATUS", "ignored");
                    if (NullStatus.isNull(STATUS)) {
                        STATUS = "SCHEDULED";
                    }
                    Command command = new Command("UPDATE TBLCCCAMPCONT SET MODUID={UID},MODDATE={NOW},STATUS='" + STATUS + "'");
                    command.append(",NOTES=");
                    command.bind(((Textbox) UiUtil.findComponent("NOTES")).getValue());
                    command.append(",SCHEDULE=");
                    command.bind(((Datebox) UiUtil.findComponent("SCHEDULE")).getValue());
                    command.append("WHERE UNITUID={UNITUID} AND CAMPAIGNID=");
                    command.bind(((Textbox) UiUtil.findComponent("CAMPAIGNID")).getValue());
                    command.append("AND CONTID=");
                    command.bind(((Textbox) UiUtil.findComponent("CONTID")).getValue());
                    command.executeNonQuery();
                    command = new Command("UPDATE TBLCCCAMPCALLD  SET STATUS='CLOSED' WHERE UNITUID={UNITUID} AND CAMPAIGNID=");
                    command.bind(((Textbox) UiUtil.findComponent("CAMPAIGNID")).getValue());
                    command.append("AND CONTID=");
                    command.bind(((Textbox) UiUtil.findComponent("CONTID")).getValue());
                    command.append("AND STATUS='PROGRESS' ");
                    command.executeNonQuery(true);
                    ((Datebox) UiUtil.findComponent("SCHEDULE")).setValue(null);
                    ((Textbox) UiUtil.findComponent("NOTES")).setValue(null);
                    ((Textbox) UiUtil.findComponent("CAMPAIGNID")).setValue(null);
                    ((Textbox) UiUtil.findComponent("CONTID")).setValue(null);
                    ((Combobox) UiUtil.findComponent("RESULT")).setSelectedIndex(0);
                } else if (NullStatus.isNull(RESULT)) {
                    UiUtil.showMessage(MessageType.ERROR, MessageUtil.getMessage(Agent.class, Messages.agent_2));
                    return;
                }
            }
            Agent agent = (Agent) UiUtil.getSessionAttribute("agent");
            if (agent == null) {
                ((Component) UiUtil.findComponent("topElement")).setVisible(false);
                UiUtil.showMessage(MessageType.ERROR, MessageUtil.getMessage(Agent.class, Messages.agent_6));
                return;
            }
            if (agent != null) {
                if (agent.voiceEnabled) {
                    agent.hangupExtention();
                }
            }
            if (!((Component) UiUtil.findComponent("idleWindow")).isVisible()) {
                UiUtil.getDataset("CHANNELVARS").clear();
                ((Textbox) UiUtil.findComponent("srcENTRY")).setText(null);
                UiUtil.getDataset("TBLCRMCONTACTS").reFill();
                ((Textbox) UiUtil.findComponent("SUBJECT")).setText(null);
                ((Textbox) UiUtil.findComponent("chatMSG")).setText(null);
                ((Component) UiUtil.findComponent("campWindow")).setVisible(false);
                ((Component) UiUtil.findComponent("campWaitWindow")).setVisible(false);
                ((Component) UiUtil.findComponent("webChatWindow")).setVisible(false);
                ((Component) UiUtil.findComponent("emailWindow")).setVisible(false);
                ((Component) UiUtil.findComponent("voiceWindow")).setVisible(true);
                ((Component) UiUtil.findComponent("idleWindow")).setVisible(true);
            }
        } else if (((Component) UiUtil.findComponent("campWaitWindow")).isVisible()) {
            Agent agent = (Agent) UiUtil.getSessionAttribute("agent");
            if (agent == null) {
                ((Component) UiUtil.findComponent("topElement")).setVisible(false);
                UiUtil.showMessage(MessageType.ERROR, MessageUtil.getMessage(Agent.class, Messages.agent_6));
                return;
            }
            if (agent != null) {
                if (agent.voiceEnabled) {
                    agent.hangupExtention();
                }
            }
            Command command = new Command("UPDATE TBLCCAGENTLOG A SET A.ENDDATE={NOW}, A.MODUID={UID},A.MODDATE={NOW} ");
            command.append("WHERE A.ENDDATE IS NULL AND A.UID={UID} AND LOGTYPE='CAMP' ");
            command.executeNonQuery(true);
            if (!((Component) UiUtil.findComponent("idleWindow")).isVisible()) {
                UiUtil.getDataset("CHANNELVARS").clear();
                ((Textbox) UiUtil.findComponent("srcENTRY")).setText(null);
                UiUtil.getDataset("TBLCRMCONTACTS").reFill();
                ((Textbox) UiUtil.findComponent("SUBJECT")).setText(null);
                ((Textbox) UiUtil.findComponent("chatMSG")).setText(null);
                ((Component) UiUtil.findComponent("campWindow")).setVisible(false);
                ((Component) UiUtil.findComponent("campWaitWindow")).setVisible(false);
                ((Component) UiUtil.findComponent("webChatWindow")).setVisible(false);
                ((Component) UiUtil.findComponent("emailWindow")).setVisible(false);
                ((Component) UiUtil.findComponent("voiceWindow")).setVisible(true);
                ((Component) UiUtil.findComponent("idleWindow")).setVisible(true);
            }
        } else if (((Component) UiUtil.findComponent("webChatWindow")).isVisible()) {
            Chat.endChat(event);
        } else {
            Agent agent = (Agent) UiUtil.getSessionAttribute("agent");
            if (agent == null) {
                ((Component) UiUtil.findComponent("topElement")).setVisible(false);
                UiUtil.showMessage(MessageType.ERROR, MessageUtil.getMessage(Agent.class, Messages.agent_6));
                return;
            }
            if (agent != null) {
                if (agent.voiceEnabled) {
                    agent.hangupExtention();
                }
            }
        }
    }

    public static void transfer() throws Exception {
        transfer(null);
    }

    public static void transfer(Event event) throws Exception {
        Agent agent = (Agent) UiUtil.getSessionAttribute("agent");
        if (agent == null) {
            ((Component) UiUtil.findComponent("topElement")).setVisible(false);
            UiUtil.showMessage(MessageType.ERROR, MessageUtil.getMessage(Agent.class, Messages.agent_6));
            return;
        }
        if (agent != null) {
            if (!agent.voiceEnabled) {
                return;
            }
            String targetType = "extension";
            String target = Converter.asString(UiUtil.getValue("DialNumber", "getValue"));
            if (NullStatus.isNull(target)) {
                target = Converter.asString(UiUtil.getValue("IVRID", "getValue"));
                if (NullStatus.isNotNull(target)) {
                    targetType = "IVR";
                }
            }
            if (NullStatus.isNull(target)) {
                return;
            }
            UiUtil.getDataset("CHANNELVARS").clear();
            ManagerConnection managerConnection = agent.getManagerConnection(agent.getPBXID());
            managerConnection.prepareChannels();
            StatusEvent activeChannel = null;
            StatusEvent customerChannel = null;
            String tegsoftTRANSFERID = Counter.getUUID().toString();
            for (int i = 0; i < managerConnection.getChannels().size(); i++) {
                StatusEvent statusEvent = managerConnection.getChannels().get(i);
                String UID = TegsoftPBX.getChannelVariable(managerConnection, statusEvent, "UID");
                if (Compare.equal(UID, UiUtil.getUID())) {
                    activeChannel = statusEvent;
                } else if (statusEvent.getChannel().startsWith(agent.getINTERFACE())) {
                    activeChannel = statusEvent;
                }
            }
            if (activeChannel == null) {
                return;
            }
            if ("extension".equals(targetType)) {
                customerChannel = managerConnection.getChannel(activeChannel.getBridgedChannel());
                managerConnection.sendAction(new RedirectAction(customerChannel.getChannel(), "tegsoft-INTERNAL", target, 1), 5000);
                return;
            } else {
                managerConnection.sendAction(new SetVarAction(activeChannel.getChannel(), "tegsoftTRANSFERID", tegsoftTRANSFERID), 5000);
                managerConnection.sendAction(new SetVarAction(activeChannel.getChannel(), "tegsoftPARTY", "AGENT"), 5000);
                managerConnection.sendAction(new SetVarAction(activeChannel.getChannel(), "tegsoftLOGID", agent.LOGID), 5000);
                customerChannel = managerConnection.getChannel(activeChannel.getBridgedChannel());
                if (customerChannel == null) {
                    return;
                }
                BaseTegsoftIVR baseTegsoftIVR = BaseTegsoftIVR.initialize(target);
                managerConnection.sendAction(new SetVarAction(customerChannel.getChannel(), "CONTID", Converter.asNotNullString(UiUtil.getDataSourceValue("TBLCRMCONTACTS", "CONTID", null, null))), 5000);
                managerConnection.sendAction(new SetVarAction(customerChannel.getChannel(), "tegsoftTRANSFERID", tegsoftTRANSFERID), 5000);
                managerConnection.sendAction(new SetVarAction(customerChannel.getChannel(), "tegsoftIVRID", target), 5000);
                managerConnection.sendAction(new SetVarAction(customerChannel.getChannel(), "tegsoftLOGID", agent.LOGID), 5000);
                if (baseTegsoftIVR.isConferenceIVR()) {
                    managerConnection.sendAction(new SetVarAction(customerChannel.getChannel(), "tegsoftPARTY", "CUSTOMER"), 5000);
                    managerConnection.sendAction(new StopMonitorAction(customerChannel.getChannel()), 5000);
                    managerConnection.sendAction(new RedirectAction(activeChannel.getChannel(), activeChannel.getBridgedChannel(), "tegsoft-INTERNAL", "9999", 1, "tegsoft-INTERNAL", "9999", 1), 5000);
                    Thread.sleep(2000);
                    managerConnection.sendAction(new RedirectAction(customerChannel.getChannel(), "tegsoft-INTERNAL", "9998", 1), 5000);
                } else {
                    managerConnection.sendAction(new RedirectAction(activeChannel.getChannel(), "tegsoft-INTERNAL", "9999", 1), 5000);
                    return;
                }
            }
        }
    }

    public String getINTERFACE() {
        return INTERFACE;
    }

    public void setINTERFACE(String iNTERFACE) {
        INTERFACE = iNTERFACE;
    }

    public String getFROMADDRESS() {
        return FROMADDRESS;
    }

    public void setFROMADDRESS(String fROMADDRESS) {
        FROMADDRESS = fROMADDRESS;
    }

    public String getEXTID() {
        return EXTID;
    }

    public void setEXTID(String eXTID) {
        EXTID = eXTID;
    }

    public String getPBXID() {
        return PBXID;
    }

    public void setPBXID(String pBXID) {
        PBXID = pBXID;
    }

    public enum Messages {

        /**
		 * No agent definition found! Please check Contact Center Settings.
		 * 
		 */
        agent_1, /**
		 * You have to select a campaign result.
		 */
        agent_2, /**
		 * License state error. (Login Count: {0}, Allowed Count: {0})
		 */
        agent_3, /**
		 * No seat definition found! Please check Contact Center Settings.
		 * 
		 */
        agent_4, /**
		 * Agent is already login. Cannot relogin. Please switch to open
		 * application or close all open browsers and relogin.
		 * 
		 */
        agent_5, /**
		 * Agent login corrupted. Please close application and reopen.
		 * 
		 */
        agent_6, /**
		 * Credit card result
		 * 
		 */
        CCRESULT, /**
		 * Error
		 * 
		 */
        ERROR, /**
		 * Success
		 * 
		 */
        SUCCESS, /**
		 * Caller hold time
		 * 
		 */
        QEHOLDTIME, /**
		 * Caller position
		 * 
		 */
        QEORIGINALPOS, /**
		 * Skill
		 * 
		 */
        QUEUENAME, /**
		 * Calls waiting
		 * 
		 */
        QUEUECALLS
    }

    public String getREMOTEINTERFACE() {
        return REMOTEINTERFACE;
    }

    public void setREMOTEINTERFACE(String rEMOTEINTERFACE) {
        REMOTEINTERFACE = rEMOTEINTERFACE;
    }
}
