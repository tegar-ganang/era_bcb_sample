package com.tegsoft.pbx.agi;

import java.io.File;
import java.math.BigDecimal;
import java.net.NetworkInterface;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import org.apache.log4j.Level;
import org.asteriskjava.fastagi.AgiChannel;
import org.asteriskjava.fastagi.AgiException;
import org.asteriskjava.fastagi.AgiRequest;
import org.asteriskjava.fastagi.BaseAgiScript;
import org.asteriskjava.manager.action.CommandAction;
import org.asteriskjava.manager.action.GetVarAction;
import org.asteriskjava.manager.event.StatusEvent;
import org.asteriskjava.manager.response.CommandResponse;
import org.asteriskjava.manager.response.GetVarResponse;
import com.tegsoft.cc.DialerThread;
import com.tegsoft.ivr.BaseTegsoftIVR;
import com.tegsoft.ivr.ProgressiveDialer;
import com.tegsoft.pbx.ManagerConnection;
import com.tegsoft.pbx.TegsoftPBX;
import com.tegsoft.tobe.db.command.Command;
import com.tegsoft.tobe.db.connection.Connection;
import com.tegsoft.tobe.db.dataset.DataRow;
import com.tegsoft.tobe.db.dataset.Dataset;
import com.tegsoft.tobe.os.LicenseManager;
import com.tegsoft.tobe.util.Compare;
import com.tegsoft.tobe.util.Converter;
import com.tegsoft.tobe.util.DateUtil;
import com.tegsoft.tobe.util.JobUtil;
import com.tegsoft.tobe.util.NullStatus;
import com.tegsoft.tobe.util.StringUtil;
import com.tegsoft.tobe.util.message.MessageUtil;

public class IntelligentCallRouting extends BaseAgiScript {

    public String PBXID;

    private String callerIdNumber;

    private String callerIdName;

    private String DID;

    private Dataset TBLPBXBLOCK;

    private Dataset TBLPBXINROUTE;

    private Dataset TBLCCSKILLS;

    private Dataset TBLPBXCF;

    private Dataset TBLPBXGRP;

    private Dataset TBLPBXSHORTNUM;

    private Dataset TBLPBXEXT;

    private Dataset TBLPBXFAX;

    private Dataset TBLCCAGENTLOG;

    private Dataset TBLPBXCONF;

    private Dataset TBLPBXGRPMEM;

    private Dataset TBLPBXCUSTDEST;

    private Dataset TBLPBX;

    private DataRow rowTBLPBX;

    public String asteriskCommandSeperator = "|";

    private String tegsoftLOGID;

    private static final int maxCLID = 22;

    public IntelligentCallRouting() throws Exception {
        TBLPBXBLOCK = new Dataset("TBLPBXBLOCK", "TBLPBXBLOCK");
        TBLPBXINROUTE = new Dataset("TBLPBXINROUTE", "TBLPBXINROUTE");
        TBLCCSKILLS = new Dataset("TBLCCSKILLS", "TBLCCSKILLS");
        TBLPBXCF = new Dataset("TBLPBXCF", "TBLPBXCF");
        TBLPBXGRP = new Dataset("TBLPBXGRP", "TBLPBXGRP");
        TBLPBXSHORTNUM = new Dataset("TBLPBXSHORTNUM", "TBLPBXSHORTNUM");
        TBLPBXEXT = new Dataset("TBLPBXEXT", "TBLPBXEXT");
        TBLPBXFAX = new Dataset("TBLPBXFAX", "TBLPBXFAX");
        TBLCCAGENTLOG = new Dataset("TBLCCAGENTLOG", "TBLCCAGENTLOG");
        TBLPBXCONF = new Dataset("TBLPBXCONF", "TBLPBXCONF");
        TBLPBXGRPMEM = new Dataset("TBLPBXGRPMEM", "TBLPBXGRPMEM");
        TBLPBXCUSTDEST = new Dataset("TBLPBXCUSTDEST", "TBLPBXCUSTDEST");
        TBLPBX = new Dataset("TBLPBX", "TBLPBX");
    }

    public void service(AgiRequest request, AgiChannel channel) throws AgiException {
        try {
            TegsoftPBX.setLogLevel(channel, Level.INFO);
            TegsoftPBX.logMessage(channel, Level.INFO, "Tegsoft ICR incharge");
            JobUtil.prepareThread();
            Connection.initNonJ2EE();
            Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
            if (nets != null) {
                while (nets.hasMoreElements()) {
                    NetworkInterface netint = nets.nextElement();
                    String macKey = "";
                    if (netint.getHardwareAddress() == null) {
                        continue;
                    }
                    for (int i = 0; i < netint.getHardwareAddress().length; i++) {
                        macKey += Converter.asHexString(netint.getHardwareAddress()[i]);
                        if (i < netint.getHardwareAddress().length - 1) {
                            macKey += ":";
                        }
                    }
                    TBLPBX.fill(new Command("SELECT * FROM TBLPBX WHERE UPPER(MACADDRESS)=UPPER('" + macKey + "')"));
                    if (TBLPBX.getRowCount() > 0) {
                        rowTBLPBX = TBLPBX.getRow(0);
                        PBXID = rowTBLPBX.getString("PBXID");
                        break;
                    }
                }
            }
            if (NullStatus.isNull(PBXID)) {
                TegsoftPBX.logMessage(channel, Level.FATAL, "Invalid PBXID returning -- Please check PBX definitions and MAC of the defined PBX");
                return;
            }
            TegsoftPBX.logMessage(channel, Level.DEBUG, "We got the PBXID " + PBXID);
            if (LicenseManager.isValid("TegsoftPBX") == 0) {
                TegsoftPBX.logMessage(channel, Level.FATAL, "License invalid please check tegsoft_icr.log for details");
                channel.exec("Answer");
                channel.exec("Playback", "goodbye");
                channel.exec("Hangup");
                return;
            }
            TegsoftPBX.logMessage(channel, Level.INFO, "License check OK");
            final ManagerConnection managerConnection = TegsoftPBX.createManagerConnection(PBXID);
            CommandAction commandAction = new CommandAction("core show version");
            CommandResponse response = (CommandResponse) managerConnection.sendAction(commandAction, 10000);
            for (String line : response.getResult()) {
                if (line.indexOf("Asterisk 1.8") >= 0) {
                    asteriskCommandSeperator = ",";
                }
            }
            managerConnection.disconnect();
            String context = request.getParameter("context");
            if (NullStatus.isNotNull(channel.getVariable("EXTID"))) {
                Command command = new Command("SELECT EXTEN FROM TBLPBXEXT WHERE UNITUID={UNITUID} AND PBXID=");
                command.bind(PBXID);
                command.append("AND EXTID=");
                command.bind(channel.getVariable("EXTID"));
                callerIdNumber = command.executeScalarAsString();
                TegsoftPBX.logMessage(channel, Level.DEBUG, "CallerId Number changed from " + request.getCallerIdNumber() + " to " + callerIdNumber);
            } else {
                callerIdNumber = request.getCallerIdNumber();
                callerIdName = request.getCallerIdName();
                TegsoftPBX.logMessage(channel, Level.DEBUG, "CallerId Number kept as " + callerIdNumber + " callerIdName:" + callerIdName);
            }
            String CONTEXTID = channel.getVariable("CONTEXTID");
            if (NullStatus.isNull(CONTEXTID)) {
                Command command = new Command("SELECT CONTEXTID FROM TBLPBXEXT WHERE UNITUID={UNITUID} AND PBXID=");
                command.bind(PBXID);
                command.append("AND EXTEN=");
                command.bind(callerIdNumber);
                CONTEXTID = command.executeScalarAsString();
                if (NullStatus.isNotNull(CONTEXTID)) {
                    channel.setVariable("CONTEXTID", CONTEXTID);
                }
            }
            if (NullStatus.isNull(CONTEXTID)) {
                Command command = new Command("SELECT CONTEXTID FROM TBLPBXFAX WHERE UNITUID={UNITUID} AND PBXID=");
                command.bind(PBXID);
                command.append("AND EXTEN=");
                command.bind(callerIdNumber);
                CONTEXTID = command.executeScalarAsString();
                if (NullStatus.isNotNull(CONTEXTID)) {
                    channel.setVariable("CONTEXTID", CONTEXTID);
                }
            }
            String extension = request.getExtension();
            if (NullStatus.isNotNull(extension)) {
                if (extension.indexOf(";") >= 0) {
                    extension = extension.substring(0, extension.indexOf(";"));
                }
            }
            DID = request.getDnid();
            if (NullStatus.isNotNull(DID)) {
                if (DID.indexOf(";") >= 0) {
                    DID = DID.substring(0, DID.indexOf(";"));
                }
            }
            tegsoftLOGID = channel.getVariable("tegsoftLOGID");
            if ("tegsoft-INTERNAL".equals(context)) {
                channel.setVariable("DIRECTION", "INTERNAL");
                channel.exec("Set", "CDR(DIRECTION)=INTERNAL");
                TegsoftPBX.logMessage(channel, Level.DEBUG, "CONTEXTID set as " + CONTEXTID + " in tegsoft-INTERNAL");
                routeINTERNAL(extension, request, channel);
            } else if ("tegsoft-INCOMMING".equals(context)) {
                channel.setVariable("DIRECTION", "INBOUND");
                channel.exec("Set", "CDR(DIRECTION)=INBOUND");
                TegsoftPBX.logMessage(channel, Level.DEBUG, "CONTEXTID set as " + CONTEXTID + " in tegsoft-INCOMMING");
                routeINCOMMING(extension, request, channel);
            } else if ("tegsoft-TBLPBX".equals(context)) {
                channel.setVariable("DIRECTION", "INTERNAL");
                channel.exec("Set", "CDR(DIRECTION)=INTERNAL");
                TegsoftPBX.logMessage(channel, Level.DEBUG, "CONTEXTID set as " + CONTEXTID + " in tegsoft-TBLPBX");
                routeTBLPBX(extension, request, channel);
            }
            globalCleanup();
        } catch (Exception ex) {
            MessageUtil.logMessage(IntelligentCallRouting.class, Level.FATAL, ex);
        }
        Connection.closeActive();
    }

    private void globalCleanup() throws Exception {
        Command command = new Command("UPDATE TBLCCAGENTLOG SET ENDDATE={NOW} WHERE LOGID=");
        command.bind(tegsoftLOGID);
        command.executeNonQuery();
    }

    public static boolean checkTimeCondition(AgiChannel channel, String TIMETYPE, String TIMECONDID) throws Exception {
        if (NullStatus.isNull(TIMETYPE)) {
            return true;
        }
        if (NullStatus.isNull(TIMECONDID)) {
            return true;
        }
        Dataset TBLPBXTIMEGRP = new Dataset("TBLPBXTIMEGRP", "TBLPBXTIMEGRP");
        Command command = new Command("SELECT * FROM TBLPBXTIMEGRP WHERE UNITUID={UNITUID} AND TIMEGRPID IN (SELECT TIMEGRPID FROM TBLPBXTIMEMEM WHERE UNITUID={UNITUID} AND TIMECONDID=");
        command.bind(TIMECONDID);
        command.append(")");
        TBLPBXTIMEGRP.fill(command);
        boolean result = false;
        for (int i = 0; i < TBLPBXTIMEGRP.getRowCount(); i++) {
            DataRow rowTBLPBXTIMEGRP = TBLPBXTIMEGRP.getRow(i);
            if (NullStatus.isNotNull(rowTBLPBXTIMEGRP.getString("BEGINTIME"))) {
                int BEGINTIME = Integer.parseInt(rowTBLPBXTIMEGRP.getString("BEGINTIME"));
                int CURRTIME = Integer.parseInt(Converter.asNotNullFormattedString(DateUtil.now(), "HHmm"));
                if (channel != null) {
                    TegsoftPBX.logMessage(channel, Level.DEBUG, "Checking BEGINTIME->" + BEGINTIME + " CURRTIME->" + CURRTIME + " TIMETYPE->" + TIMETYPE);
                }
                if (BEGINTIME > CURRTIME) {
                    result = false;
                    continue;
                }
            }
            if (NullStatus.isNotNull(rowTBLPBXTIMEGRP.getString("ENDTIME"))) {
                int ENDTIME = Integer.parseInt(rowTBLPBXTIMEGRP.getString("ENDTIME"));
                int CURRTIME = Integer.parseInt(Converter.asNotNullFormattedString(DateUtil.now(), "HHmm"));
                if (channel != null) {
                    TegsoftPBX.logMessage(channel, Level.DEBUG, "Checking ENDTIME->" + ENDTIME + " CURRTIME->" + CURRTIME + " TIMETYPE->" + TIMETYPE);
                }
                if (ENDTIME < CURRTIME) {
                    result = false;
                    continue;
                }
            }
            if (NullStatus.isNotNull(rowTBLPBXTIMEGRP.getString("BEGINDAYOFM"))) {
                int BEGINDAYOFM = Integer.parseInt(rowTBLPBXTIMEGRP.getString("BEGINDAYOFM"));
                int CURRDAYOFM = Integer.parseInt(Converter.asNotNullFormattedString(DateUtil.now(), "dd"));
                if (channel != null) {
                    TegsoftPBX.logMessage(channel, Level.DEBUG, "Checking BEGINDAYOFM->" + BEGINDAYOFM + " CURRDAYOFM->" + CURRDAYOFM + " TIMETYPE->" + TIMETYPE);
                }
                if (BEGINDAYOFM > CURRDAYOFM) {
                    result = false;
                    continue;
                }
            }
            if (NullStatus.isNotNull(rowTBLPBXTIMEGRP.getString("ENDDAYOFM"))) {
                int ENDDAYOFM = Integer.parseInt(rowTBLPBXTIMEGRP.getString("ENDDAYOFM"));
                int CURRDAYOFM = Integer.parseInt(Converter.asNotNullFormattedString(DateUtil.now(), "dd"));
                if (channel != null) {
                    TegsoftPBX.logMessage(channel, Level.DEBUG, "Checking ENDDAYOFM->" + ENDDAYOFM + " CURRDAYOFM->" + CURRDAYOFM + " TIMETYPE->" + TIMETYPE);
                }
                if (ENDDAYOFM < CURRDAYOFM) {
                    result = false;
                    continue;
                }
            }
            if (NullStatus.isNotNull(rowTBLPBXTIMEGRP.getString("BEGINDAYOFW"))) {
                int BEGINDAYOFW = Integer.parseInt(rowTBLPBXTIMEGRP.getString("BEGINDAYOFW"));
                int CURRDAYOFW = DateUtil.getCurrentWeekDay();
                if (channel != null) {
                    TegsoftPBX.logMessage(channel, Level.DEBUG, "Checking BEGINDAYOFW->" + BEGINDAYOFW + " CURRDAYOFW->" + CURRDAYOFW + " TIMETYPE->" + TIMETYPE);
                }
                if (BEGINDAYOFW > CURRDAYOFW) {
                    result = false;
                    continue;
                }
            }
            if (NullStatus.isNotNull(rowTBLPBXTIMEGRP.getString("ENDDAYOFW"))) {
                int ENDDAYOFW = Integer.parseInt(rowTBLPBXTIMEGRP.getString("ENDDAYOFW"));
                int CURRDAYOFW = DateUtil.getCurrentWeekDay();
                if (channel != null) {
                    TegsoftPBX.logMessage(channel, Level.DEBUG, "Checking ENDDAYOFW->" + ENDDAYOFW + " CURRDAYOFW->" + CURRDAYOFW + " TIMETYPE->" + TIMETYPE);
                }
                if (ENDDAYOFW < CURRDAYOFW) {
                    result = false;
                    continue;
                }
            }
            if (NullStatus.isNotNull(rowTBLPBXTIMEGRP.getString("BEGINDATE"))) {
                long BEGINDATE = rowTBLPBXTIMEGRP.getDate("BEGINDATE").getTime();
                long CURRDATE = DateUtil.now().getTime();
                if (channel != null) {
                    TegsoftPBX.logMessage(channel, Level.DEBUG, "Checking BEGINDATE->" + rowTBLPBXTIMEGRP.getDate("BEGINDATE") + " CURRDATE->" + DateUtil.now() + " TIMETYPE->" + TIMETYPE);
                }
                if (BEGINDATE > CURRDATE) {
                    result = false;
                    continue;
                }
            }
            if (NullStatus.isNotNull(rowTBLPBXTIMEGRP.getString("ENDDATE"))) {
                long ENDDATE = rowTBLPBXTIMEGRP.getDate("ENDDATE").getTime();
                long CURRDATE = DateUtil.now().getTime();
                if (channel != null) {
                    TegsoftPBX.logMessage(channel, Level.DEBUG, "Checking ENDDATE->" + rowTBLPBXTIMEGRP.getDate("ENDDATE") + " CURRDATE->" + DateUtil.now() + " TIMETYPE->" + TIMETYPE);
                }
                if (ENDDATE < CURRDATE) {
                    result = false;
                    continue;
                }
            }
            result = true;
            break;
        }
        if ("INTIME".equals(TIMETYPE)) {
            if (channel != null) {
                TegsoftPBX.logMessage(channel, Level.DEBUG, "Returning " + result + " for time check");
            }
            return result;
        } else {
            if (channel != null) {
                TegsoftPBX.logMessage(channel, Level.DEBUG, "Returning " + !result + " for time check");
            }
            return !result;
        }
    }

    public void routeINCOMMING(String extension, AgiRequest request, AgiChannel channel) throws Exception {
        Command command = new Command("SELECT COUNT(*) FROM TBLPBXBLOCK WHERE UNITUID={UNITUID} AND BLOCKTYPE='IN' AND EXTEN=");
        command.bind(extension);
        if (command.executeScalarAsDecimal().intValue() > 0) {
            TegsoftPBX.logMessage(channel, Level.WARN, "Blocked number IN");
            channel.exec("Playback", "all-circuits-busy-now&pls-try-call-later" + asteriskCommandSeperator + "noanswer");
            return;
        }
        command = new Command("SELECT * FROM TBLPBXBLOCK WHERE UNITUID={UNITUID} AND BLOCKTYPE='IN' AND EXTEN IS NOT NULL");
        TBLPBXBLOCK.fill(command);
        for (int i = 0; i < TBLPBXBLOCK.getRowCount(); i++) {
            DataRow rowTBLPBXBLOCK = TBLPBXBLOCK.getRow(i);
            if (Compare.checkPattern(rowTBLPBXBLOCK.getString("EXTEN"), extension)) {
                TegsoftPBX.logMessage(channel, Level.WARN, "Blocked number IN");
                channel.exec("Playback", "all-circuits-busy-now&pls-try-call-later" + asteriskCommandSeperator + " noanswer");
                return;
            }
        }
        String FAXROUTE = " AND BLOCKNOCLID='false' ";
        if (Compare.equal("fax", extension)) {
            FAXROUTE = " AND BLOCKNOCLID='true' ";
        }
        TegsoftPBX.logMessage(channel, Level.INFO, "Executing callerid services");
        CRMCallerID crmCallerID = new CRMCallerID();
        crmCallerID.service(request, channel);
        TegsoftPBX.logMessage(channel, Level.INFO, "Checking Incomming Routes Trunk :" + request.getParameter("trunkid"));
        command = new Command("SELECT * FROM TBLPBXINROUTE WHERE UNITUID={UNITUID} " + FAXROUTE + " AND TRUNKID=");
        command.bind(request.getParameter("trunkid"));
        TBLPBXINROUTE.fill(command);
        for (int i = 0; i < TBLPBXINROUTE.getRowCount(); i++) {
            DataRow rowTBLPBXINROUTE = TBLPBXINROUTE.getRow(i);
            if (checkTimeCondition(channel, rowTBLPBXINROUTE.getString("TIMETYPE"), rowTBLPBXINROUTE.getString("TIMECONDID"))) {
                channel.setVariable("CONTEXTID", rowTBLPBXINROUTE.getString("CONTEXTID"));
                if (NullStatus.isNotNull(rowTBLPBXINROUTE.getString("CIDPREFIX"))) {
                    TegsoftPBX.logMessage(channel, Level.INFO, "Adding callerid prefix :" + rowTBLPBXINROUTE.getString("CIDPREFIX"));
                    String name = rowTBLPBXINROUTE.getString("CIDPREFIX") + " " + Converter.asNotNullString(channel.getVariable("CALLERIDNAME")) + " " + Converter.asNotNullString(request.getCallerIdName());
                    String number = request.getCallerIdNumber();
                    String allCallerId = "\"" + StringUtil.convertToEnglishOnlyLetters(StringUtil.left(Converter.asNotNullString(name), maxCLID)) + "\" <" + Converter.asNotNullString(number) + ">";
                    channel.setCallerId(allCallerId);
                }
                if (NullStatus.isNotNull(rowTBLPBXINROUTE.getString("MOHID"))) {
                    channel.exec("Set", "CHANNEL(musicclass)=" + rowTBLPBXINROUTE.getString("MOHID"));
                }
                dialDESTPARAM(extension, request, channel, rowTBLPBXINROUTE.getString("DESTTYPE"), rowTBLPBXINROUTE.getString("DESTPARAM"));
                return;
            }
        }
        TegsoftPBX.logMessage(channel, Level.INFO, "Checking Incomming Routes callerIdNumber:" + callerIdNumber + " DID:" + DID);
        command = new Command("SELECT * FROM TBLPBXINROUTE WHERE UNITUID={UNITUID} " + FAXROUTE + " AND TRUNKID IS NULL AND CLID=");
        command.bind(callerIdNumber);
        command.append("AND DID=");
        command.bind(DID);
        TBLPBXINROUTE.fill(command);
        for (int i = 0; i < TBLPBXINROUTE.getRowCount(); i++) {
            DataRow rowTBLPBXINROUTE = TBLPBXINROUTE.getRow(i);
            if (checkTimeCondition(channel, rowTBLPBXINROUTE.getString("TIMETYPE"), rowTBLPBXINROUTE.getString("TIMECONDID"))) {
                channel.setVariable("CONTEXTID", rowTBLPBXINROUTE.getString("CONTEXTID"));
                if (NullStatus.isNotNull(rowTBLPBXINROUTE.getString("CIDPREFIX"))) {
                    TegsoftPBX.logMessage(channel, Level.INFO, "Adding callerid prefix :" + rowTBLPBXINROUTE.getString("CIDPREFIX"));
                    String name = rowTBLPBXINROUTE.getString("CIDPREFIX") + " " + Converter.asNotNullString(channel.getVariable("CALLERIDNAME")) + " " + Converter.asNotNullString(request.getCallerIdName());
                    String number = request.getCallerIdNumber();
                    String allCallerId = "\"" + StringUtil.convertToEnglishOnlyLetters(StringUtil.left(Converter.asNotNullString(name), maxCLID)) + "\" <" + Converter.asNotNullString(number) + ">";
                    channel.setCallerId(allCallerId);
                }
                if (NullStatus.isNotNull(rowTBLPBXINROUTE.getString("MOHID"))) {
                    channel.exec("Set", "CHANNEL(musicclass)=" + rowTBLPBXINROUTE.getString("MOHID"));
                }
                dialDESTPARAM(extension, request, channel, rowTBLPBXINROUTE.getString("DESTTYPE"), rowTBLPBXINROUTE.getString("DESTPARAM"));
                return;
            }
        }
        TegsoftPBX.logMessage(channel, Level.INFO, "Checking Incomming Routes callerIdNumber:" + callerIdNumber + " DID IS ANY");
        command = new Command("SELECT * FROM TBLPBXINROUTE WHERE UNITUID={UNITUID} " + FAXROUTE + " AND TRUNKID IS NULL AND CLID=");
        command.bind(callerIdNumber);
        command.append("AND DID IS NULL");
        TBLPBXINROUTE.fill(command);
        for (int i = 0; i < TBLPBXINROUTE.getRowCount(); i++) {
            DataRow rowTBLPBXINROUTE = TBLPBXINROUTE.getRow(i);
            if (checkTimeCondition(channel, rowTBLPBXINROUTE.getString("TIMETYPE"), rowTBLPBXINROUTE.getString("TIMECONDID"))) {
                channel.setVariable("CONTEXTID", rowTBLPBXINROUTE.getString("CONTEXTID"));
                if (NullStatus.isNotNull(rowTBLPBXINROUTE.getString("CIDPREFIX"))) {
                    TegsoftPBX.logMessage(channel, Level.INFO, "Adding callerid prefix :" + rowTBLPBXINROUTE.getString("CIDPREFIX"));
                    String name = rowTBLPBXINROUTE.getString("CIDPREFIX") + " " + Converter.asNotNullString(channel.getVariable("CALLERIDNAME")) + " " + Converter.asNotNullString(request.getCallerIdName());
                    String number = request.getCallerIdNumber();
                    String allCallerId = "\"" + StringUtil.convertToEnglishOnlyLetters(StringUtil.left(Converter.asNotNullString(name), maxCLID)) + "\" <" + Converter.asNotNullString(number) + ">";
                    channel.setCallerId(allCallerId);
                }
                if (NullStatus.isNotNull(rowTBLPBXINROUTE.getString("MOHID"))) {
                    channel.exec("Set", "CHANNEL(musicclass)=" + rowTBLPBXINROUTE.getString("MOHID"));
                }
                dialDESTPARAM(extension, request, channel, rowTBLPBXINROUTE.getString("DESTTYPE"), rowTBLPBXINROUTE.getString("DESTPARAM"));
                return;
            }
        }
        TegsoftPBX.logMessage(channel, Level.INFO, "Checking Incomming Routes callerIdNumber IS PATTERN DID:" + DID);
        command = new Command("SELECT * FROM TBLPBXINROUTE WHERE UNITUID={UNITUID} " + FAXROUTE + " AND TRUNKID IS NULL AND CLID IS NOT NULL AND DID=");
        command.bind(DID);
        command.append("ORDER BY LENGTH(CLID) DESC ");
        TBLPBXINROUTE.fill(command);
        for (int i = 0; i < TBLPBXINROUTE.getRowCount(); i++) {
            DataRow rowTBLPBXINROUTE = TBLPBXINROUTE.getRow(i);
            if ((!Compare.checkPattern(rowTBLPBXINROUTE.getString("CLID"), callerIdNumber)) && (!Compare.checkPattern(rowTBLPBXINROUTE.getString("CLID"), callerIdName))) {
                continue;
            } else {
                TegsoftPBX.logMessage(channel, Level.DEBUG, "Checking Incomming Routes callerIdNumber PATTERN MATCH :" + rowTBLPBXINROUTE.getString("CLID") + " for " + callerIdNumber + " DID:" + DID);
            }
            if (checkTimeCondition(channel, rowTBLPBXINROUTE.getString("TIMETYPE"), rowTBLPBXINROUTE.getString("TIMECONDID"))) {
                channel.setVariable("CONTEXTID", rowTBLPBXINROUTE.getString("CONTEXTID"));
                if (NullStatus.isNotNull(rowTBLPBXINROUTE.getString("CIDPREFIX"))) {
                    TegsoftPBX.logMessage(channel, Level.INFO, "Adding callerid prefix :" + rowTBLPBXINROUTE.getString("CIDPREFIX"));
                    String name = rowTBLPBXINROUTE.getString("CIDPREFIX") + " " + Converter.asNotNullString(channel.getVariable("CALLERIDNAME")) + " " + Converter.asNotNullString(request.getCallerIdName());
                    String number = request.getCallerIdNumber();
                    String allCallerId = "\"" + StringUtil.convertToEnglishOnlyLetters(StringUtil.left(Converter.asNotNullString(name), maxCLID)) + "\" <" + Converter.asNotNullString(number) + ">";
                    channel.setCallerId(allCallerId);
                }
                if (NullStatus.isNotNull(rowTBLPBXINROUTE.getString("MOHID"))) {
                    channel.exec("Set", "CHANNEL(musicclass)=" + rowTBLPBXINROUTE.getString("MOHID"));
                }
                dialDESTPARAM(extension, request, channel, rowTBLPBXINROUTE.getString("DESTTYPE"), rowTBLPBXINROUTE.getString("DESTPARAM"));
                return;
            }
        }
        TegsoftPBX.logMessage(channel, Level.INFO, "Checking Incomming Routes callerIdNumber IS PATTERN DID IS ANY");
        command = new Command("SELECT * FROM TBLPBXINROUTE WHERE UNITUID={UNITUID} " + FAXROUTE + " AND TRUNKID IS NULL AND CLID IS NOT NULL AND DID IS NULL");
        command.append("ORDER BY LENGTH(CLID) DESC ");
        TBLPBXINROUTE.fill(command);
        for (int i = 0; i < TBLPBXINROUTE.getRowCount(); i++) {
            DataRow rowTBLPBXINROUTE = TBLPBXINROUTE.getRow(i);
            if ((!Compare.checkPattern(rowTBLPBXINROUTE.getString("CLID"), callerIdNumber)) && (!Compare.checkPattern(rowTBLPBXINROUTE.getString("CLID"), callerIdName))) {
                continue;
            } else {
                TegsoftPBX.logMessage(channel, Level.DEBUG, "Checking Incomming Routes callerIdNumber PATTERN MATCH :" + rowTBLPBXINROUTE.getString("CLID") + " for " + callerIdNumber + " DID IS ANY");
            }
            if (checkTimeCondition(channel, rowTBLPBXINROUTE.getString("TIMETYPE"), rowTBLPBXINROUTE.getString("TIMECONDID"))) {
                channel.setVariable("CONTEXTID", rowTBLPBXINROUTE.getString("CONTEXTID"));
                if (NullStatus.isNotNull(rowTBLPBXINROUTE.getString("CIDPREFIX"))) {
                    TegsoftPBX.logMessage(channel, Level.INFO, "Adding callerid prefix :" + rowTBLPBXINROUTE.getString("CIDPREFIX"));
                    String name = rowTBLPBXINROUTE.getString("CIDPREFIX") + " " + Converter.asNotNullString(channel.getVariable("CALLERIDNAME")) + " " + Converter.asNotNullString(request.getCallerIdName());
                    String number = request.getCallerIdNumber();
                    String allCallerId = "\"" + StringUtil.convertToEnglishOnlyLetters(StringUtil.left(Converter.asNotNullString(name), maxCLID)) + "\" <" + Converter.asNotNullString(number) + ">";
                    channel.setCallerId(allCallerId);
                }
                if (NullStatus.isNotNull(rowTBLPBXINROUTE.getString("MOHID"))) {
                    channel.exec("Set", "CHANNEL(musicclass)=" + rowTBLPBXINROUTE.getString("MOHID"));
                }
                dialDESTPARAM(extension, request, channel, rowTBLPBXINROUTE.getString("DESTTYPE"), rowTBLPBXINROUTE.getString("DESTPARAM"));
                return;
            }
        }
        TegsoftPBX.logMessage(channel, Level.INFO, "Checking Incomming Routes callerIdNumber IS ANY DID:" + DID);
        command = new Command("SELECT * FROM TBLPBXINROUTE WHERE UNITUID={UNITUID} " + FAXROUTE + " AND TRUNKID IS NULL AND CLID IS NULL AND DID=");
        command.bind(DID);
        TBLPBXINROUTE.fill(command);
        for (int i = 0; i < TBLPBXINROUTE.getRowCount(); i++) {
            DataRow rowTBLPBXINROUTE = TBLPBXINROUTE.getRow(i);
            if (checkTimeCondition(channel, rowTBLPBXINROUTE.getString("TIMETYPE"), rowTBLPBXINROUTE.getString("TIMECONDID"))) {
                channel.setVariable("CONTEXTID", rowTBLPBXINROUTE.getString("CONTEXTID"));
                if (NullStatus.isNotNull(rowTBLPBXINROUTE.getString("CIDPREFIX"))) {
                    TegsoftPBX.logMessage(channel, Level.INFO, "Adding callerid prefix :" + rowTBLPBXINROUTE.getString("CIDPREFIX"));
                    String name = rowTBLPBXINROUTE.getString("CIDPREFIX") + " " + Converter.asNotNullString(channel.getVariable("CALLERIDNAME")) + " " + Converter.asNotNullString(request.getCallerIdName());
                    String number = request.getCallerIdNumber();
                    String allCallerId = "\"" + StringUtil.convertToEnglishOnlyLetters(StringUtil.left(Converter.asNotNullString(name), maxCLID)) + "\" <" + Converter.asNotNullString(number) + ">";
                    channel.setCallerId(allCallerId);
                }
                if (NullStatus.isNotNull(rowTBLPBXINROUTE.getString("MOHID"))) {
                    channel.exec("Set", "CHANNEL(musicclass)=" + rowTBLPBXINROUTE.getString("MOHID"));
                }
                dialDESTPARAM(extension, request, channel, rowTBLPBXINROUTE.getString("DESTTYPE"), rowTBLPBXINROUTE.getString("DESTPARAM"));
                return;
            }
        }
        TegsoftPBX.logMessage(channel, Level.INFO, "Checking Incomming Routes callerIdNumber IS ANY DID IS ANY");
        command = new Command("SELECT * FROM TBLPBXINROUTE WHERE UNITUID={UNITUID} " + FAXROUTE + " AND TRUNKID IS NULL AND CLID IS NULL AND DID IS NULL");
        TBLPBXINROUTE.fill(command);
        for (int i = 0; i < TBLPBXINROUTE.getRowCount(); i++) {
            DataRow rowTBLPBXINROUTE = TBLPBXINROUTE.getRow(i);
            if (checkTimeCondition(channel, rowTBLPBXINROUTE.getString("TIMETYPE"), rowTBLPBXINROUTE.getString("TIMECONDID"))) {
                channel.setVariable("CONTEXTID", rowTBLPBXINROUTE.getString("CONTEXTID"));
                if (NullStatus.isNotNull(rowTBLPBXINROUTE.getString("CIDPREFIX"))) {
                    TegsoftPBX.logMessage(channel, Level.INFO, "Adding callerid prefix :" + rowTBLPBXINROUTE.getString("CIDPREFIX"));
                    String name = rowTBLPBXINROUTE.getString("CIDPREFIX") + " " + Converter.asNotNullString(channel.getVariable("CALLERIDNAME")) + " " + Converter.asNotNullString(request.getCallerIdName());
                    String number = request.getCallerIdNumber();
                    String allCallerId = "\"" + StringUtil.convertToEnglishOnlyLetters(StringUtil.left(Converter.asNotNullString(name), maxCLID)) + "\" <" + Converter.asNotNullString(number) + ">";
                    channel.setCallerId(allCallerId);
                }
                if (NullStatus.isNotNull(rowTBLPBXINROUTE.getString("MOHID"))) {
                    channel.exec("Set", "CHANNEL(musicclass)=" + rowTBLPBXINROUTE.getString("MOHID"));
                }
                dialDESTPARAM(extension, request, channel, rowTBLPBXINROUTE.getString("DESTTYPE"), rowTBLPBXINROUTE.getString("DESTPARAM"));
                return;
            }
        }
        channel.exec("Playback", "all-circuits-busy-now&pls-try-call-later" + asteriskCommandSeperator + "noanswer");
    }

    public void routeTBLPBX(String extension, AgiRequest request, AgiChannel channel) throws Exception {
        routeINTERNAL(extension, request, channel);
    }

    public boolean checkIN(String extension, AgiRequest request, AgiChannel channel) throws Exception {
        TegsoftPBX.logMessage(channel, Level.INFO, "Controlling features for dialed number " + extension);
        if (checkFeatures(extension, request, channel)) {
            hangup(request, channel);
            return true;
        }
        TegsoftPBX.logMessage(channel, Level.INFO, "Controlling conferences for dialed number " + extension);
        if (checkConf(extension, request, channel)) {
            hangup(request, channel);
            return true;
        }
        TegsoftPBX.logMessage(channel, Level.INFO, "Controlling ring groups for dialed number " + extension);
        if (checkRingGroup(extension, request, channel)) {
            hangup(request, channel);
            return true;
        }
        TegsoftPBX.logMessage(channel, Level.INFO, "Controlling custom destinations for dialed number " + extension);
        if (checkCustomDestination(extension, request, channel)) {
            hangup(request, channel);
            return true;
        }
        TegsoftPBX.logMessage(channel, Level.INFO, "Controlling queues for dialed number " + extension);
        if (checkQUEUE(extension, request, channel)) {
            hangup(request, channel);
            return true;
        }
        TegsoftPBX.logMessage(channel, Level.INFO, "Controlling extentions for dialed number " + extension);
        if (checkExtention(extension, request, channel)) {
            hangup(request, channel);
            return true;
        }
        TegsoftPBX.logMessage(channel, Level.INFO, "Controlling faxes for dialed number " + extension);
        if (checkFax(extension, request, channel)) {
            hangup(request, channel);
            return true;
        }
        return false;
    }

    private synchronized void bridgeWithAgent(String extension, AgiRequest request, AgiChannel channel) throws Exception {
        String CAMPAIGNID = channel.getVariable("CAMPAIGNID");
        String CONTID = channel.getVariable("CONTID");
        String ORDERID = channel.getVariable("ORDERID");
        String DIALID = channel.getVariable("DIALID");
        String AGENT = null;
        new Command("UPDATE TBLCCCAMPCALLD SET DIALRESULT='ANSWER' WHERE DIALID='" + DIALID + "'").executeNonQuery(true);
        TegsoftPBX.logMessage(channel, Level.INFO, "Executing BRIDGE for " + CAMPAIGNID);
        StatusEvent targetChannel = null;
        ManagerConnection managerConnection = TegsoftPBX.createManagerConnection(PBXID);
        managerConnection.prepareChannels();
        for (int i = 0; i < managerConnection.getChannels().size(); i++) {
            StatusEvent statusEvent = managerConnection.getChannels().get(i);
            if (Compare.equal("6", statusEvent.getChannelState())) {
                if (NullStatus.isNull(statusEvent.getBridgedChannel())) {
                    String agentCAMPAIGNID = TegsoftPBX.getChannelVariable(managerConnection, statusEvent, "CAMPWAITING");
                    if (Compare.equal(CAMPAIGNID, agentCAMPAIGNID)) {
                        targetChannel = statusEvent;
                        TegsoftPBX.setChannelVariable(managerConnection, statusEvent, "CAMPWAITING", "PROGRESS");
                        AGENT = TegsoftPBX.getChannelVariable(managerConnection, statusEvent, "UID");
                        break;
                    }
                }
            }
        }
        managerConnection.disconnect();
        if (targetChannel == null) {
            Command command = new Command("SELECT COUNT(*) FROM TBLCCCAMPCALLD WHERE UNITUID={UNITUID} AND CAMPAIGNID=");
            command.bind(CAMPAIGNID);
            command.append(" AND CONTID=");
            command.bind(CONTID);
            int callCount = command.executeScalarAsDecimal().intValue();
            if (callCount == 1) {
                command = new Command("DELETE FROM TBLCCCAMPCALLD WHERE UNITUID={UNITUID} AND CAMPAIGNID=");
                command.bind(CAMPAIGNID);
                command.append(" AND CONTID=");
                command.bind(CONTID);
                command.append(" AND ORDERID=");
                command.bind(ORDERID);
                command.executeNonQuery();
                command = new Command("UPDATE TBLCCCAMPCONT SET STATUS=NULL WHERE UNITUID={UNITUID} AND CAMPAIGNID=");
                command.bind(CAMPAIGNID);
                command.append(" AND CONTID=");
                command.bind(CONTID);
                command.executeNonQuery();
            } else {
                command = new Command("SELECT MAX(ORDERID) FROM TBLCCCAMPCALLD WHERE UNITUID={UNITUID} AND CAMPAIGNID=");
                command.bind(CAMPAIGNID);
                command.append(" AND CONTID=");
                command.bind(CONTID);
                command.append("AND ORDERID<");
                command.bind(ORDERID);
                BigDecimal prevORDERID = command.executeScalarAsDecimal();
                command = new Command("DELETE FROM TBLCCCAMPCALLD WHERE UNITUID={UNITUID} AND CAMPAIGNID=");
                command.bind(CAMPAIGNID);
                command.append(" AND CONTID=");
                command.bind(CONTID);
                command.append(" AND ORDERID=");
                command.bind(ORDERID);
                command.executeNonQuery();
                DialerThread.createNextCall(CAMPAIGNID, CONTID, prevORDERID);
            }
        } else if (targetChannel != null) {
            Command command = new Command("UPDATE TBLCCCAMPCONT A SET A.AGENT=");
            command.bind(AGENT);
            command.append(", A.STATUS='PROGRESS'  WHERE A.UNITUID={UNITUID} AND CAMPAIGNID=");
            command.bind(CAMPAIGNID);
            command.append("AND CONTID=");
            command.bind(CONTID);
            command.executeNonQuery(true);
            command = new Command("UPDATE TBLCCAGENTLOG A SET A.ENDDATE={NOW}, A.MODUID=");
            command.bind(AGENT);
            command.append(",A.MODDATE={NOW} ");
            command.append("WHERE A.ENDDATE IS NULL AND A.UID=");
            command.bind(AGENT);
            command.append("AND LOGTYPE='CAMP' ");
            command.executeNonQuery(true);
            managerConnection = TegsoftPBX.createManagerConnection(PBXID);
            GetVarResponse getVarResponse = (GetVarResponse) managerConnection.sendAction(new GetVarAction(targetChannel.getChannel(), "MixMonitorCallID"), 5000);
            String MixMonitorCallID = getVarResponse.getValue();
            managerConnection.disconnect();
            String folderName = "/var/spool/asterisk/monitor/" + new SimpleDateFormat("yyyy/MM/dd").format(new Date()) + "/";
            new File(folderName).mkdirs();
            Runtime.getRuntime().exec("chown asterisk.asterisk " + folderName);
            channel.exec("MixMonitor", "/var/spool/asterisk/monitor/" + new SimpleDateFormat("yyyy/MM/dd").format(new Date()) + "/TOBE-" + new SimpleDateFormat("yyyyMMdd").format(new Date()) + "-" + MixMonitorCallID + ".wav" + asteriskCommandSeperator + "a");
            TegsoftPBX.logMessage(channel, Level.INFO, "Changing id to->" + MixMonitorCallID + " for " + channel.getVariable("DIALID"));
            new Command("UPDATE TBLCCCAMPCALLD SET CALLID='" + MixMonitorCallID + "' WHERE DIALID='" + channel.getVariable("DIALID") + "'").executeNonQuery(true);
            TegsoftPBX.logMessage(channel, Level.INFO, "Before Bridge!! uid->" + channel.getUniqueId());
            String roomName = targetChannel.getChannel();
            if (roomName.indexOf("/") > 0) {
                roomName = roomName.substring(roomName.indexOf("/") + 1);
            }
            if (roomName.indexOf("-") > 0) {
                roomName = roomName.substring(0, roomName.indexOf("-"));
            }
            roomName = "99" + roomName;
            channel.exec("MeetMe", roomName);
            managerConnection = TegsoftPBX.createManagerConnection(PBXID);
            managerConnection.sendAction(new CommandAction("meetme kick " + roomName + " all"), 5000);
            managerConnection.disconnect();
        }
    }

    public void routeINTERNAL(String extension, AgiRequest request, AgiChannel channel) throws Exception {
        TegsoftPBX.logMessage(channel, Level.INFO, "Controlling IVR for dialed number " + extension);
        if ("9999".equals(extension)) {
            channel.exec("StopMixMonitor");
            channel.setVariable("MixMonitorCallID", channel.getUniqueId());
            String roomName = channel.getName();
            if (roomName.indexOf("/") > 0) {
                roomName = roomName.substring(roomName.indexOf("/") + 1);
            }
            if (roomName.indexOf("-") > 0) {
                roomName = roomName.substring(0, roomName.indexOf("-"));
            }
            roomName = "99" + roomName;
            channel.exec("MeetMe", roomName + asteriskCommandSeperator + "dM1");
            ManagerConnection managerConnection = TegsoftPBX.createManagerConnection(PBXID);
            managerConnection.sendAction(new CommandAction("meetme kick " + roomName + " all"), 5000);
            managerConnection.disconnect();
            return;
        }
        if ("9996".equals(extension)) {
            new ProgressiveDialer().execute(this, request, channel);
            return;
        }
        if ("9997".equals(extension)) {
            String CAMPAIGNID = channel.getVariable("CAMPAIGNID");
            String CONTID = channel.getVariable("CONTID");
            String ORDERID = channel.getVariable("ORDERID");
            try {
                bridgeWithAgent(extension, request, channel);
            } catch (Exception ex) {
                DialerThread.createNextCall(CAMPAIGNID, CONTID, new BigDecimal(ORDERID));
                return;
            }
            return;
        }
        if ("9998".equals(extension)) {
            TegsoftPBX.logMessage(channel, Level.INFO, "Executing IVR for IVRID " + channel.getVariable("tegsoftIVRID"));
            String CAMPAIGNID = channel.getVariable("CAMPAIGNID");
            String CONTID = channel.getVariable("CONTID");
            String ORDERID = channel.getVariable("ORDERID");
            String DIALID = channel.getVariable("DIALID");
            new Command("UPDATE TBLCCCAMPCALLD SET DIALRESULT='ANSWER' WHERE DIALID='" + DIALID + "'").executeNonQuery(true);
            try {
                BaseTegsoftIVR.initialize(getVariable("tegsoftIVRID")).execute(this, request, channel);
            } catch (Exception ex) {
                DialerThread.createNextCall(CAMPAIGNID, CONTID, new BigDecimal(ORDERID));
                return;
            }
            if (NullStatus.isNotNull(CAMPAIGNID)) {
                Command command = new Command("UPDATE TBLCCCAMPCONT SET STATUS='CLOSED' WHERE UNITUID={UNITUID} AND CAMPAIGNID=");
                command.bind(CAMPAIGNID);
                command.append("AND CONTID=");
                command.bind(CONTID);
                command.executeNonQuery(true);
                command = new Command("UPDATE TBLCCCAMPCALLD  SET STATUS='CLOSED' WHERE UNITUID={UNITUID} AND CAMPAIGNID=");
                command.bind(CAMPAIGNID);
                command.append("AND CONTID=");
                command.bind(CONTID);
                command.executeNonQuery(true);
            }
            String BRIDGEME = channel.getVariable("BRIDGEME");
            if (NullStatus.isNotNull(BRIDGEME)) {
                Command command = new Command("UPDATE TBLCCAGENTLOG SET REASON='DEFAULT',ENDDATE=NULL WHERE LOGID=");
                command.bind(tegsoftLOGID);
                command.executeNonQuery();
                channel.exec("Bridge", BRIDGEME + asteriskCommandSeperator + "p");
                String folderName = "/var/spool/asterisk/monitor/" + new SimpleDateFormat("yyyy/MM/dd").format(new Date()) + "/";
                new File(folderName).mkdirs();
                Runtime.getRuntime().exec("chown asterisk.asterisk " + folderName);
                channel.exec("MixMonitor", "/var/spool/asterisk/monitor/" + new SimpleDateFormat("yyyy/MM/dd").format(new Date()) + "/TOBE-" + new SimpleDateFormat("yyyyMMdd").format(new Date()) + "-" + request.getUniqueId() + ".wav" + asteriskCommandSeperator + "a");
            }
            return;
        }
        TegsoftPBX.logMessage(channel, Level.INFO, "Controlling blocked numbers " + extension);
        Command command = new Command("SELECT COUNT(*) FROM TBLPBXBLOCK WHERE UNITUID={UNITUID} AND BLOCKTYPE='OUT' AND EXTEN=");
        command.bind(extension);
        if (command.executeScalarAsDecimal().intValue() > 0) {
            TegsoftPBX.logMessage(channel, Level.INFO, "Blocked number OUT");
            channel.exec("Playback", "all-circuits-busy-now&pls-try-call-later" + asteriskCommandSeperator + "noanswer");
            return;
        }
        command = new Command("SELECT * FROM TBLPBXBLOCK WHERE UNITUID={UNITUID} AND BLOCKTYPE='OUT' AND EXTEN IS NOT NULL");
        TBLPBXBLOCK.fill(command);
        for (int i = 0; i < TBLPBXBLOCK.getRowCount(); i++) {
            DataRow rowTBLPBXBLOCK = TBLPBXBLOCK.getRow(i);
            if (Compare.checkPattern(rowTBLPBXBLOCK.getString("EXTEN"), extension)) {
                TegsoftPBX.logMessage(channel, Level.INFO, "Blocked number OUT");
                channel.exec("Playback", "all-circuits-busy-now&pls-try-call-later" + asteriskCommandSeperator + "noanswer");
                return;
            }
        }
        TegsoftPBX.logMessage(channel, Level.INFO, "Controlling pickup ");
        if (checkPickup(extension, request, channel)) {
            hangup(request, channel);
            return;
        }
        TegsoftPBX.logMessage(channel, Level.INFO, "Controlling internal ");
        if (checkIN(extension, request, channel)) {
            return;
        }
        TegsoftPBX.logMessage(channel, Level.INFO, "Controlling short numbers ");
        if (checkShortNumber(extension, request, channel)) {
            hangup(request, channel);
            return;
        }
        TegsoftPBX.logMessage(channel, Level.INFO, "Controlling login state for extension -" + extension + " ");
        if (checkLogin(extension, request, channel)) {
            hangup(request, channel);
            return;
        }
        TegsoftPBX.logMessage(channel, Level.INFO, "Trying outbound routes ");
        if (checkTrunk(extension, request, channel)) {
            hangup(request, channel);
            return;
        }
        TegsoftPBX.logMessage(channel, Level.INFO, "We are done nothing to do! ");
        channel.exec("Playback", "all-circuits-busy-now&pls-try-call-later" + asteriskCommandSeperator + "noanswer");
    }

    /**
	 * 
	 * @param request
	 * @param channel
	 * @param type
	 *            can be one of CALLERID,OUTCALLERID,EMERGENCYCID
	 * @throws Exception
	 */
    private void assignCallerID(AgiRequest request, AgiChannel channel, String type) throws Exception {
        Command command = new Command("SELECT * FROM TBLPBXEXT WHERE UNITUID={UNITUID} AND PBXID=");
        command.bind(PBXID);
        command.append("AND EXTEN=");
        command.bind(callerIdNumber);
        TBLPBXEXT.fill(command);
        if (TBLPBXEXT.getRowCount() > 0) {
            String callID = TBLPBXEXT.getRow(0).getString(type);
            if (NullStatus.isNull(callID)) {
                String name = TBLPBXEXT.getRow(0).getString("CALLERID");
                String number = callerIdNumber;
                String allCallerId = "\"" + StringUtil.convertToEnglishOnlyLetters(StringUtil.left(Converter.asNotNullString(name), maxCLID)) + "\" <" + Converter.asNotNullString(number) + ">";
                channel.setCallerId(allCallerId);
            } else {
                if (callID.indexOf("<") >= 0) {
                    channel.setCallerId(StringUtil.convertToEnglishOnlyLetters(callID));
                } else {
                    String name = callID;
                    String number = callerIdNumber;
                    String allCallerId = "\"" + StringUtil.convertToEnglishOnlyLetters(StringUtil.left(Converter.asNotNullString(name), maxCLID)) + "\" <" + Converter.asNotNullString(number) + ">";
                    channel.setCallerId(allCallerId);
                }
            }
        }
    }

    private boolean checkLogin(String extension, AgiRequest request, AgiChannel channel) throws Exception {
        Command command = new Command("SELECT * FROM TBLPBXEXT WHERE UNITUID={UNITUID} AND PBXID=");
        command.bind(PBXID);
        command.append("AND EXTEN=");
        command.bind(callerIdNumber);
        command.append("AND CTIADDRESS IS NOT NULL");
        TBLPBXEXT.fill(command);
        if (TBLPBXEXT.getRowCount() == 0) {
            return false;
        }
        if (TBLPBXEXT.getRow(0).getString("CTIADDRESS").startsWith("[") && TBLPBXEXT.getRow(0).getString("CTIADDRESS").endsWith("]")) {
            return false;
        }
        command = new Command("SELECT * FROM TBLCCAGENTLOG WHERE UNITUID={UNITUID} AND BEGINDATE<{NOW} AND ENDDATE IS NULL AND LOGTYPE='LOGIN' AND INTERFACE='SIP/" + callerIdNumber + "'");
        TBLCCAGENTLOG.fill(command);
        if (TBLCCAGENTLOG.getRowCount() == 0) {
            channel.exec("Answer");
            channel.exec("Playback", "vm-invalidpassword");
            channel.exec("Hangup");
            return true;
        }
        return false;
    }

    private boolean checkFeatures(String extension, AgiRequest request, AgiChannel channel) throws Exception {
        Command command = new Command("SELECT * FROM TBLPBXEXT WHERE UNITUID={UNITUID} AND PBXID=");
        command.bind(PBXID);
        command.append("AND EXTEN=");
        command.bind(callerIdNumber);
        TBLPBXEXT.fill(command);
        if (TBLPBXEXT.getRowCount() == 0) {
            return false;
        }
        if (Compare.equal(rowTBLPBX.getString("FCCF"), extension)) {
            Applications.enableCF(PBXID, request, channel);
            return true;
        }
        if (Compare.equal(rowTBLPBX.getString("FCCFD"), extension)) {
            Applications.disableCF(PBXID, request, channel);
            return true;
        }
        if (Compare.equal(rowTBLPBX.getString("FCCFB"), extension)) {
            Applications.enableCFB(PBXID, request, channel);
            return true;
        }
        if (Compare.equal(rowTBLPBX.getString("FCCFBD"), extension)) {
            Applications.disableCFB(PBXID, request, channel);
            return true;
        }
        if (Compare.equal(rowTBLPBX.getString("FCCFNA"), extension)) {
            Applications.enableCFNA(PBXID, request, channel);
            return true;
        }
        if (Compare.equal(rowTBLPBX.getString("FCCFNAD"), extension)) {
            Applications.disableCFNA(PBXID, request, channel);
            return true;
        }
        if (Compare.equal(rowTBLPBX.getString("FCVM"), extension)) {
            channel.exec("VoiceMailMain", "s" + callerIdNumber);
            return true;
        }
        TegsoftPBX.logMessage(channel, Level.INFO, "Controlling realtime monitoring for dialed number " + extension);
        if (checkRTM(extension, request, channel)) {
            hangup(request, channel);
            return true;
        }
        return false;
    }

    private boolean checkConf(String extension, AgiRequest request, AgiChannel channel) throws Exception {
        Command command = new Command("SELECT * FROM TBLPBXCONF WHERE UNITUID={UNITUID} AND PBXID IS NOT NULL ");
        command.append("AND EXTEN=");
        command.bind(extension);
        TBLPBXCONF.fill(command);
        if (TBLPBXCONF.getRowCount() == 0) {
            return false;
        }
        DataRow rowTBLPBXCONF = TBLPBXCONF.getRow(0);
        if (Compare.equal(PBXID, rowTBLPBXCONF.getString("PBXID"))) {
            String options = "";
            if (NullStatus.isNotNull(rowTBLPBXCONF.getDecimal("MAXMEMBER"))) {
                int maxCount = rowTBLPBXCONF.getDecimal("MAXMEMBER").intValue();
                int numberofUsers = channel.exec("MeetMeCount", extension + asteriskCommandSeperator + "COUNT_" + extension);
                if (numberofUsers >= maxCount) {
                    channel.exec("Background", "conf-full");
                    return true;
                }
            }
            if (NullStatus.isNotNull(rowTBLPBXCONF.getString("JOINMSG"))) {
                TegsoftPBX.playBackground(channel, rowTBLPBXCONF.getString("JOINMSG"));
            }
            if (Compare.isTrue(rowTBLPBXCONF.getString("QUIETMODE"))) {
                options += "q";
            }
            if (Compare.isTrue(rowTBLPBXCONF.getString("USERCOUNT"))) {
                options += "c";
            }
            if (Compare.isTrue(rowTBLPBXCONF.getString("JOINLEAVE"))) {
                options += "I";
            }
            if (Compare.isTrue(rowTBLPBXCONF.getString("ALLOWMENU"))) {
                options += "s";
            }
            if (Compare.isTrue(rowTBLPBXCONF.getString("RECORD"))) {
                String folderName = "/var/spool/asterisk/monitor/" + new SimpleDateFormat("yyyy/MM/dd").format(new Date()) + "/";
                new File(folderName).mkdirs();
                Runtime.getRuntime().exec("chown asterisk.asterisk " + folderName);
                channel.setVariable("MEETME_RECORDINGFILE", "/var/spool/asterisk/monitor/" + new SimpleDateFormat("yyyy/MM/dd").format(new Date()) + "/TOBE-" + new SimpleDateFormat("yyyyMMdd").format(new Date()) + "-" + request.getUniqueId() + ".wav" + asteriskCommandSeperator + "a");
                options += "r";
            }
            channel.exec("MeetMe", extension + asteriskCommandSeperator + options + asteriskCommandSeperator);
            return true;
        } else {
            channel.exec("Dial", "SIP/" + rowTBLPBXCONF.getString("PBXID") + "/" + extension + asteriskCommandSeperator + "20" + asteriskCommandSeperator + "t");
            return true;
        }
    }

    private boolean checkPickup(String extension, AgiRequest request, AgiChannel channel) throws Exception {
        if (NullStatus.isNotNull(rowTBLPBX.getString("FCDPICKUP"))) {
            if (extension.startsWith(rowTBLPBX.getString("FCDPICKUP"))) {
                String realExtention = extension.substring(2);
                TegsoftPBX.logMessage(channel, Level.INFO, "Extension starts with directed pickup checking extension list for " + realExtention);
                Command command = new Command("SELECT COUNT(*) FROM TBLPBXEXT WHERE UNITUID={UNITUID} AND EXTEN=");
                command.bind(realExtention);
                if (command.executeScalarAsDecimal().intValue() > 0) {
                    TegsoftPBX.logMessage(channel, Level.DEBUG, "Executing pickup for " + realExtention);
                    TegsoftPBX.logMessage(channel, Level.DEBUG, "Pickup 1 " + channel.exec("Pickup", realExtention));
                    return true;
                }
            }
        }
        if (Compare.equal(rowTBLPBX.getString("FCGPICKUP"), extension)) {
            TegsoftPBX.logMessage(channel, Level.DEBUG, "Executing global pickup");
            channel.exec("Pickup");
            return true;
        }
        return false;
    }

    private boolean checkRTM(String extension, AgiRequest request, AgiChannel channel) throws Exception {
        boolean returnValue = false;
        if (extension.startsWith("556")) {
            String realExtention = extension.substring(3);
            TegsoftPBX.logMessage(channel, Level.INFO, "Extension starts with realtime monitoring checking extension list for " + realExtention);
            Command command = new Command("SELECT COUNT(*) FROM TBLPBXEXT WHERE UNITUID={UNITUID} AND EXTEN=");
            command.bind(realExtention);
            if (command.executeScalarAsDecimal().intValue() > 0) {
                returnValue = true;
                if (LicenseManager.isValid("TegsoftCC") > 0) {
                    TegsoftPBX.logMessage(channel, Level.DEBUG, "Checking login agent on " + realExtention);
                    command = new Command("SELECT * FROM TBLCCAGENTLOG WHERE UNITUID={UNITUID} AND BEGINDATE<{NOW} AND ENDDATE IS NULL AND LOGTYPE='LOGIN' AND INTERFACE='SIP/" + realExtention + "'");
                    TBLCCAGENTLOG.fill(command);
                    if (TBLCCAGENTLOG.getRowCount() == 0) {
                        TegsoftPBX.logMessage(channel, Level.WARN, "No one logged in " + realExtention + " cannot start realtime monitoring");
                        channel.exec("Answer");
                        channel.exec("Playback", "vm-invalidpassword");
                        channel.exec("Hangup");
                        return returnValue;
                    } else {
                        TegsoftPBX.logMessage(channel, Level.INFO, "Checking login supervisor on " + callerIdNumber);
                        String agent = TBLCCAGENTLOG.getRow(0).getString("UID");
                        command = new Command("SELECT * FROM TBLCCAGENTLOG WHERE UNITUID={UNITUID} AND BEGINDATE<{NOW} AND ENDDATE IS NULL AND LOGTYPE='LOGIN' AND INTERFACE='SIP/" + callerIdNumber + "'");
                        TBLCCAGENTLOG.fill(command);
                        if (TBLCCAGENTLOG.getRowCount() == 0) {
                            TegsoftPBX.logMessage(channel, Level.WARN, "No Authorized supervisor is logged in " + callerIdNumber + " cannot start realtime monitoring");
                            channel.exec("Answer");
                            channel.exec("Playback", "vm-invalidpassword");
                            channel.exec("Hangup");
                            return returnValue;
                        } else {
                            TegsoftPBX.logMessage(channel, Level.DEBUG, "Check permissin to monitor " + realExtention + " with supervisor " + TBLCCAGENTLOG.getRow(0).getString("UID"));
                            String supervisor = TBLCCAGENTLOG.getRow(0).getString("UID");
                            command = new Command("SELECT COUNT(*) FROM TBLCCSUPAGENT WHERE UNITUID={UNITUID} AND UID=");
                            command.bind(supervisor);
                            command.append("AND AGENT=");
                            command.bind(agent);
                            if (command.executeScalarAsDecimal().intValue() > 0) {
                                TegsoftPBX.logMessage(channel, Level.INFO, "Starting realtime monitoring");
                                channel.exec("ChanSpy", "SIP/" + realExtention + asteriskCommandSeperator + "dq)");
                                return returnValue;
                            } else {
                                TegsoftPBX.logMessage(channel, Level.WARN, "Supervisor " + supervisor + " has no right to listen " + agent);
                                channel.exec("Answer");
                                channel.exec("Playback", "vm-invalidpassword");
                                channel.exec("Hangup");
                                return returnValue;
                            }
                        }
                    }
                } else {
                    channel.exec("Authenticate", "1234");
                    channel.exec("ChanSpy", "SIP/" + realExtention + asteriskCommandSeperator + "dq)");
                    return returnValue;
                }
            }
        }
        return returnValue;
    }

    public boolean checkShortNumber(String extension, AgiRequest request, AgiChannel channel) throws Exception {
        Command command = new Command("SELECT * FROM TBLPBXSHORTNUM WHERE UNITUID={UNITUID} AND DISABLED='false' ");
        command.append("AND EXTEN=");
        command.bind(extension);
        TBLPBXSHORTNUM.fill(command);
        if (TBLPBXSHORTNUM.getRowCount() == 0) {
            return false;
        }
        DataRow rowTBLPBXSHORTNUM = TBLPBXSHORTNUM.getRow(0);
        routeINTERNAL(rowTBLPBXSHORTNUM.getString("DESTNUMBER"), request, channel);
        return true;
    }

    private boolean checkRingGroup(String extension, AgiRequest request, AgiChannel channel) throws Exception {
        Command command = new Command("SELECT * FROM TBLPBXGRP WHERE UNITUID={UNITUID} ");
        command.append("AND GRPNUMBER=");
        command.bind(extension);
        TBLPBXGRP.fill(command);
        if (TBLPBXGRP.getRowCount() == 0) {
            return false;
        }
        DataRow rowTBLPBXGRP = TBLPBXGRP.getRow(0);
        command = new Command("SELECT * FROM TBLPBXGRPMEM WHERE UNITUID={UNITUID} ");
        command.append("AND GROUPID=");
        command.bind(rowTBLPBXGRP.getString("GROUPID"));
        command.append("ORDER BY ORDERID");
        TBLPBXGRPMEM.fill(command);
        if (NullStatus.isNotNull(rowTBLPBXGRP.getString("JOINANNOUNCE"))) {
            TegsoftPBX.playBackground(channel, rowTBLPBXGRP.getString("JOINANNOUNCE"));
        }
        if (Compare.isTrue(rowTBLPBXGRP.getString("RECORDING"))) {
            String folderName = "/var/spool/asterisk/monitor/" + new SimpleDateFormat("yyyy/MM/dd").format(new Date()) + "/";
            new File(folderName).mkdirs();
            Runtime.getRuntime().exec("chown asterisk.asterisk " + folderName);
            channel.exec("MixMonitor", "/var/spool/asterisk/monitor/" + new SimpleDateFormat("yyyy/MM/dd").format(new Date()) + "/TOBE-" + new SimpleDateFormat("yyyyMMdd").format(new Date()) + "-" + request.getUniqueId() + ".wav" + asteriskCommandSeperator + "a");
        }
        if (NullStatus.isNotNull(rowTBLPBXGRP.getString("MOHID"))) {
            channel.exec("Set", "CHANNEL(musicclass)=" + rowTBLPBXGRP.getString("MOHID"));
        }
        String options = "t";
        if ("RING".equals(rowTBLPBXGRP.getString("MOHTYPE"))) {
            options = "rt";
        } else {
            options = "mt";
        }
        channel.setVariable("CONTEXTID", rowTBLPBXGRP.getString("CONTEXTID"));
        if ("RINGALL".equals(rowTBLPBXGRP.getString("STRATEGY"))) {
            String membersToDial = "";
            for (int i = 0; i < TBLPBXGRPMEM.getRowCount(); i++) {
                DataRow rowTBLPBXGRPMEM = TBLPBXGRPMEM.getRow(i);
                if ("EXT".equals(rowTBLPBXGRPMEM.getString("DESTTYPE"))) {
                    command = new Command("SELECT * FROM TBLPBXEXT WHERE UNITUID={UNITUID} AND EXTID=");
                    command.bind(rowTBLPBXGRPMEM.getString("DESTPARAM"));
                    TBLPBXEXT.fill(command);
                    if (TBLPBXEXT.getRowCount() == 0) {
                        continue;
                    }
                    DataRow rowTBLPBXEXT = TBLPBXEXT.getRow(0);
                    if (TegsoftPBX.isExtensionBusy(rowTBLPBXEXT.getString("PBXID"), rowTBLPBXEXT.getString("EXTEN"))) {
                        continue;
                    }
                    if (Compare.equal(PBXID, rowTBLPBXEXT.getString("PBXID"))) {
                        if (NullStatus.isNotNull(membersToDial)) {
                            membersToDial += "&";
                        }
                        membersToDial += "SIP/" + rowTBLPBXEXT.getString("EXTEN");
                    } else {
                        if (NullStatus.isNotNull(membersToDial)) {
                            membersToDial += "&";
                        }
                        membersToDial += "SIP/" + rowTBLPBXEXT.getString("PBXID") + "/" + rowTBLPBXEXT.getString("EXTEN");
                    }
                } else if ("FAX".equals(rowTBLPBXGRPMEM.getString("DESTTYPE"))) {
                    command = new Command("SELECT * FROM TBLPBXFAX WHERE UNITUID={UNITUID} AND FAXID=");
                    command.bind(rowTBLPBXGRPMEM.getString("DESTPARAM"));
                    TBLPBXFAX.fill(command);
                    if (TBLPBXFAX.getRowCount() == 0) {
                        continue;
                    }
                    DataRow rowTBLPBXFAX = TBLPBXFAX.getRow(0);
                    if (TegsoftPBX.isExtensionBusy(rowTBLPBXFAX.getString("PBXID"), rowTBLPBXFAX.getString("EXTEN"))) {
                        continue;
                    }
                    if (Compare.equal(PBXID, rowTBLPBXFAX.getString("PBXID"))) {
                        if (NullStatus.isNotNull(membersToDial)) {
                            membersToDial += "&";
                        }
                        membersToDial += "IAX2/" + rowTBLPBXFAX.getString("FAXID");
                    } else {
                        if (NullStatus.isNotNull(membersToDial)) {
                            membersToDial += "&";
                        }
                        membersToDial += "SIP/" + rowTBLPBXFAX.getString("PBXID") + "/" + rowTBLPBXFAX.getString("EXTEN");
                    }
                } else if ("SHRT".equals(rowTBLPBXGRPMEM.getString("DESTTYPE"))) {
                    command = new Command("SELECT DESTNUMBER FROM TBLPBXSHORTNUM WHERE UNITUID={UNITUID} AND NUMID=");
                    command.bind(rowTBLPBXGRPMEM.getString("DESTPARAM"));
                    String realExtension = command.executeScalarAsString();
                    ArrayList<DialPlanResult> dialPlanResults = createDialplan(rowTBLPBXGRP.getString("CONTEXTID"), realExtension);
                    if (dialPlanResults.size() > 0) {
                        if (NullStatus.isNotNull(membersToDial)) {
                            membersToDial += "&";
                        }
                        membersToDial += dialPlanResults.get(0).getInfoDial();
                    }
                }
            }
            if (NullStatus.isNotNull(membersToDial)) {
                channel.exec("Dial", membersToDial + asteriskCommandSeperator + rowTBLPBXGRP.getString("RINGTIME") + asteriskCommandSeperator + options);
                String DIALSTATUS = channel.getVariable("DIALSTATUS");
                if (!"ANSWER".equalsIgnoreCase(DIALSTATUS)) {
                    if (!"CANCEL".equalsIgnoreCase(DIALSTATUS)) {
                        dialDESTPARAM(extension, request, channel, rowTBLPBXGRP.getString("DESTTYPE"), rowTBLPBXGRP.getString("DESTPARAM"));
                        return true;
                    } else {
                        return true;
                    }
                }
            }
            dialDESTPARAM(extension, request, channel, rowTBLPBXGRP.getString("DESTTYPE"), rowTBLPBXGRP.getString("DESTPARAM"));
            return true;
        } else if ("HUNT".equals(rowTBLPBXGRP.getString("STRATEGY"))) {
            for (int i = 0; i < TBLPBXGRPMEM.getRowCount(); i++) {
                DataRow rowTBLPBXGRPMEM = TBLPBXGRPMEM.getRow(i);
                if ("EXT".equals(rowTBLPBXGRPMEM.getString("DESTTYPE"))) {
                    command = new Command("SELECT * FROM TBLPBXEXT WHERE UNITUID={UNITUID} AND EXTID=");
                    command.bind(rowTBLPBXGRPMEM.getString("DESTPARAM"));
                    TBLPBXEXT.fill(command);
                    if (TBLPBXEXT.getRowCount() == 0) {
                        continue;
                    }
                    String dialString = "";
                    DataRow rowTBLPBXEXT = TBLPBXEXT.getRow(0);
                    if (Compare.equal(PBXID, rowTBLPBXEXT.getString("PBXID"))) {
                        dialString = "SIP/" + rowTBLPBXEXT.getString("EXTEN");
                    } else {
                        dialString = "SIP/" + rowTBLPBXEXT.getString("PBXID") + "/" + rowTBLPBXEXT.getString("EXTEN");
                    }
                    if (TegsoftPBX.isExtensionBusy(rowTBLPBXEXT.getString("PBXID"), rowTBLPBXEXT.getString("EXTEN"))) {
                        continue;
                    }
                    channel.exec("Dial", dialString + asteriskCommandSeperator + rowTBLPBXGRP.getString("RINGTIME") + asteriskCommandSeperator + options);
                    String DIALSTATUS = channel.getVariable("DIALSTATUS");
                    if (("ANSWER".equalsIgnoreCase(DIALSTATUS)) || ("CANCEL".equalsIgnoreCase(DIALSTATUS))) {
                        return true;
                    }
                } else if ("FAX".equals(rowTBLPBXGRPMEM.getString("DESTTYPE"))) {
                    command = new Command("SELECT * FROM TBLPBXFAX WHERE UNITUID={UNITUID} AND FAXID=");
                    command.bind(rowTBLPBXGRPMEM.getString("DESTPARAM"));
                    TBLPBXFAX.fill(command);
                    if (TBLPBXFAX.getRowCount() == 0) {
                        continue;
                    }
                    String dialString = "";
                    DataRow rowTBLPBXFAX = TBLPBXFAX.getRow(0);
                    if (Compare.equal(PBXID, rowTBLPBXFAX.getString("PBXID"))) {
                        dialString = "IAX2/" + rowTBLPBXFAX.getString("FAXID");
                    } else {
                        dialString = "SIP/" + rowTBLPBXFAX.getString("PBXID") + "/" + rowTBLPBXFAX.getString("EXTEN");
                    }
                    if (TegsoftPBX.isExtensionBusy(rowTBLPBXFAX.getString("PBXID"), rowTBLPBXFAX.getString("EXTEN"))) {
                        continue;
                    }
                    channel.exec("Dial", dialString + asteriskCommandSeperator + rowTBLPBXGRP.getString("RINGTIME") + asteriskCommandSeperator + options);
                    String DIALSTATUS = channel.getVariable("DIALSTATUS");
                    if (("ANSWER".equalsIgnoreCase(DIALSTATUS)) || ("CANCEL".equalsIgnoreCase(DIALSTATUS))) {
                        return true;
                    }
                } else if ("SHRT".equals(rowTBLPBXGRPMEM.getString("DESTTYPE"))) {
                    command = new Command("SELECT DESTNUMBER FROM TBLPBXSHORTNUM WHERE UNITUID={UNITUID} AND NUMID=");
                    command.bind(rowTBLPBXGRPMEM.getString("DESTPARAM"));
                    String realExtension = command.executeScalarAsString();
                    ArrayList<DialPlanResult> dialPlanResults = createDialplan(rowTBLPBXGRP.getString("CONTEXTID"), realExtension);
                    if (dialPlanResults.size() > 0) {
                        channel.exec("Dial", dialPlanResults.get(0).getInfoDial() + asteriskCommandSeperator + rowTBLPBXGRP.getString("RINGTIME") + asteriskCommandSeperator + options);
                        String DIALSTATUS = channel.getVariable("DIALSTATUS");
                        if (("ANSWER".equalsIgnoreCase(DIALSTATUS)) || ("CANCEL".equalsIgnoreCase(DIALSTATUS))) {
                            return true;
                        }
                    }
                }
            }
            dialDESTPARAM(extension, request, channel, rowTBLPBXGRP.getString("DESTTYPE"), rowTBLPBXGRP.getString("DESTPARAM"));
            return true;
        } else if ("RAND".equals(rowTBLPBXGRP.getString("STRATEGY"))) {
            ArrayList<String> membersToDial = new ArrayList<String>();
            ArrayList<String> membersExtension = new ArrayList<String>();
            ArrayList<String> membersPBXID = new ArrayList<String>();
            for (int i = 0; i < TBLPBXGRPMEM.getRowCount(); i++) {
                DataRow rowTBLPBXGRPMEM = TBLPBXGRPMEM.getRow(i);
                if ("EXT".equals(rowTBLPBXGRPMEM.getString("DESTTYPE"))) {
                    command = new Command("SELECT * FROM TBLPBXEXT WHERE UNITUID={UNITUID} AND EXTID=");
                    command.bind(rowTBLPBXGRPMEM.getString("DESTPARAM"));
                    TBLPBXEXT.fill(command);
                    if (TBLPBXEXT.getRowCount() == 0) {
                        continue;
                    }
                    DataRow rowTBLPBXEXT = TBLPBXEXT.getRow(0);
                    if (Compare.equal(PBXID, rowTBLPBXEXT.getString("PBXID"))) {
                        membersToDial.add("SIP/" + rowTBLPBXEXT.getString("EXTEN"));
                        membersPBXID.add(rowTBLPBXEXT.getString("PBXID"));
                        membersExtension.add(rowTBLPBXEXT.getString("EXTEN"));
                    } else {
                        membersToDial.add("SIP/" + rowTBLPBXEXT.getString("PBXID") + "/" + rowTBLPBXEXT.getString("EXTEN"));
                        membersPBXID.add(rowTBLPBXEXT.getString("PBXID"));
                        membersExtension.add(rowTBLPBXEXT.getString("EXTEN"));
                    }
                } else if ("FAX".equals(rowTBLPBXGRPMEM.getString("DESTTYPE"))) {
                    command = new Command("SELECT * FROM TBLPBXFAX WHERE UNITUID={UNITUID} AND FAXID=");
                    command.bind(rowTBLPBXGRPMEM.getString("DESTPARAM"));
                    TBLPBXFAX.fill(command);
                    if (TBLPBXFAX.getRowCount() == 0) {
                        continue;
                    }
                    DataRow rowTBLPBXFAX = TBLPBXFAX.getRow(0);
                    if (Compare.equal(PBXID, rowTBLPBXFAX.getString("PBXID"))) {
                        membersToDial.add("IAX2/" + rowTBLPBXFAX.getString("FAXID"));
                        membersPBXID.add("");
                        membersExtension.add("");
                    } else {
                        membersToDial.add("SIP/" + rowTBLPBXFAX.getString("PBXID") + "/" + rowTBLPBXFAX.getString("EXTEN"));
                        membersPBXID.add("");
                        membersExtension.add("");
                    }
                } else if ("SHRT".equals(rowTBLPBXGRPMEM.getString("DESTTYPE"))) {
                    command = new Command("SELECT DESTNUMBER FROM TBLPBXSHORTNUM WHERE UNITUID={UNITUID} AND NUMID=");
                    command.bind(rowTBLPBXGRPMEM.getString("DESTPARAM"));
                    String realExtension = command.executeScalarAsString();
                    ArrayList<DialPlanResult> dialPlanResults = createDialplan(rowTBLPBXGRP.getString("CONTEXTID"), realExtension);
                    if (dialPlanResults.size() > 0) {
                        membersToDial.add(dialPlanResults.get(0).getInfoDial());
                        membersPBXID.add("");
                        membersExtension.add("");
                    }
                }
            }
            int dialCount = membersToDial.size();
            for (int i = 0; i < dialCount; i++) {
                int randomMember = (int) (Math.random() * membersToDial.size());
                if (randomMember >= membersToDial.size()) {
                    randomMember--;
                }
                if (TegsoftPBX.isExtensionBusy(membersPBXID.get(randomMember), membersExtension.get(randomMember))) {
                    membersToDial.remove(randomMember);
                    membersPBXID.remove(randomMember);
                    membersExtension.remove(randomMember);
                    continue;
                }
                channel.exec("Dial", membersToDial.get(randomMember) + asteriskCommandSeperator + rowTBLPBXGRP.getString("RINGTIME") + asteriskCommandSeperator + options);
                String DIALSTATUS = channel.getVariable("DIALSTATUS");
                if (("ANSWER".equalsIgnoreCase(DIALSTATUS)) || ("CANCEL".equalsIgnoreCase(DIALSTATUS))) {
                    return true;
                }
                membersToDial.remove(randomMember);
                membersPBXID.remove(randomMember);
                membersExtension.remove(randomMember);
            }
            dialDESTPARAM(extension, request, channel, rowTBLPBXGRP.getString("DESTTYPE"), rowTBLPBXGRP.getString("DESTPARAM"));
            return true;
        }
        dialDESTPARAM(extension, request, channel, rowTBLPBXGRP.getString("DESTTYPE"), rowTBLPBXGRP.getString("DESTPARAM"));
        return true;
    }

    public void dialDESTPARAM(String extension, AgiRequest request, AgiChannel channel, String DESTTYPE, String DESTPARAM) throws Exception {
        if ("CONF".equals(DESTTYPE)) {
            TegsoftPBX.logMessage(channel, Level.INFO, "Executing Conferance Application");
            Command command = new Command("SELECT EXTEN FROM TBLPBXCONF WHERE UNITUID={UNITUID} AND CONFID=");
            command.bind(DESTPARAM);
            String confExtension = command.executeScalarAsString();
            if (!checkConf(confExtension, request, channel)) {
                channel.exec("Playback", "all-circuits-busy-now&pls-try-call-later" + asteriskCommandSeperator + "noanswer");
            }
        } else if ("EXT".equals(DESTTYPE)) {
            Command command = new Command("SELECT EXTEN FROM TBLPBXEXT WHERE UNITUID={UNITUID} AND EXTID=");
            command.bind(DESTPARAM);
            String realExtension = command.executeScalarAsString();
            TegsoftPBX.logMessage(channel, Level.INFO, "Dialing extension - " + realExtension);
            if (!checkExtention(realExtension, request, channel)) {
                channel.exec("Playback", "all-circuits-busy-now&pls-try-call-later" + asteriskCommandSeperator + "noanswer");
            }
        } else if ("FAX".equals(DESTTYPE)) {
            TegsoftPBX.logMessage(channel, Level.INFO, "Dialing Fax");
            Command command = new Command("SELECT EXTEN FROM TBLPBXFAX WHERE UNITUID={UNITUID} AND FAXID=");
            command.bind(DESTPARAM);
            String realExtension = command.executeScalarAsString();
            if (!checkFax(realExtension, request, channel)) {
                channel.exec("Playback", "all-circuits-busy-now&pls-try-call-later" + asteriskCommandSeperator + "noanswer");
            }
        } else if ("QUEUE".equals(DESTTYPE)) {
            TegsoftPBX.logMessage(channel, Level.INFO, "Executing Queue Application");
            if (!checkQUEUE(DESTPARAM, request, channel)) {
                channel.exec("Playback", "all-circuits-busy-now&pls-try-call-later" + asteriskCommandSeperator + "noanswer");
            }
        } else if ("TRSTSRC".equals(DESTTYPE)) {
            if ("EXT".equals(DESTPARAM)) {
                TegsoftPBX.logMessage(channel, Level.INFO, "Checking Extensions");
                checkExtention(extension, request, channel);
            } else if ("ALLIN".equals(DESTPARAM)) {
                TegsoftPBX.logMessage(channel, Level.INFO, "Checking internal");
                checkIN(extension, request, channel);
            } else if ("ALL".equals(DESTPARAM)) {
                TegsoftPBX.logMessage(channel, Level.INFO, "Checking all");
                routeINTERNAL(extension, request, channel);
                return;
            }
        } else if ("VMQ".equals(DESTTYPE)) {
            TegsoftPBX.logMessage(channel, Level.INFO, "Executing VM Application");
            channel.exec("VoiceMail", DESTPARAM + asteriskCommandSeperator + "s");
        } else if ("VM".equals(DESTTYPE)) {
            TegsoftPBX.logMessage(channel, Level.INFO, "Executing VM Application");
            channel.exec("VoiceMail", DESTPARAM + asteriskCommandSeperator);
        } else if ("VMB".equals(DESTTYPE)) {
            TegsoftPBX.logMessage(channel, Level.INFO, "Executing VM Application");
            channel.exec("VoiceMail", DESTPARAM + asteriskCommandSeperator + "b");
        } else if ("VMU".equals(DESTTYPE)) {
            TegsoftPBX.logMessage(channel, Level.INFO, "Executing VM Application");
            channel.exec("VoiceMail", DESTPARAM + asteriskCommandSeperator + "u");
        } else if ("SHRT".equals(DESTTYPE)) {
            TegsoftPBX.logMessage(channel, Level.INFO, "Executing Short Numbers Application");
            Command command = new Command("SELECT EXTEN FROM TBLPBXSHORTNUM WHERE UNITUID={UNITUID} AND NUMID=");
            command.bind(DESTPARAM);
            String realExtension = command.executeScalarAsString();
            if (!checkShortNumber(realExtension, request, channel)) {
                channel.exec("Playback", "all-circuits-busy-now&pls-try-call-later" + asteriskCommandSeperator + "noanswer");
            }
        } else if ("CUST".equals(DESTTYPE)) {
            TegsoftPBX.logMessage(channel, Level.INFO, "Executing Custom Destination");
            Command command = new Command("SELECT EXTEN FROM TBLPBXCUSTDEST WHERE UNITUID={UNITUID} AND DESTID=");
            command.bind(DESTPARAM);
            String realExtension = command.executeScalarAsString();
            if (!checkCustomDestination(realExtension, request, channel)) {
                channel.exec("Playback", "all-circuits-busy-now&pls-try-call-later" + asteriskCommandSeperator + "noanswer");
            }
        } else if ("RINGGRP".equals(DESTTYPE)) {
            TegsoftPBX.logMessage(channel, Level.INFO, "Executing RingGroup Application");
            Command command = new Command("SELECT GRPNUMBER FROM TBLPBXGRP WHERE UNITUID={UNITUID} AND GROUPID=");
            command.bind(DESTPARAM);
            String grpExtension = command.executeScalarAsString();
            if (!checkRingGroup(grpExtension, request, channel)) {
                channel.exec("Playback", "all-circuits-busy-now&pls-try-call-later" + asteriskCommandSeperator + "noanswer");
            }
        } else if ("IVR".equals(DESTTYPE)) {
            TegsoftPBX.logMessage(channel, Level.INFO, "Executing IVR Application");
            BaseTegsoftIVR.initialize(DESTPARAM).execute(this, request, channel);
        } else if ("TRUNK".equals(DESTTYPE)) {
            TegsoftPBX.logMessage(channel, Level.INFO, "Dialing to trunk");
            channel.exec("Dial", "SIP/" + DESTPARAM + "/" + extension + asteriskCommandSeperator + "300" + asteriskCommandSeperator);
        } else if ("TERM".equals(DESTTYPE)) {
            TegsoftPBX.logMessage(channel, Level.INFO, "Terminating call");
            if ("HANGUP".equals(DESTPARAM)) {
                channel.exec("Hangup");
            } else if ("CONG".equals(DESTPARAM)) {
                channel.exec("Answer");
                channel.exec("Playtones", "congestion");
                channel.exec("Congestion", "20");
                channel.exec("Hangup");
            } else if ("BUSY".equals(DESTPARAM)) {
                channel.exec("Busy", "20");
                channel.exec("Hangup");
            }
        }
    }

    private boolean checkCustomDestination(String extension, AgiRequest request, AgiChannel channel) throws Exception {
        Command command = new Command("SELECT * FROM TBLPBXCUSTDEST WHERE UNITUID={UNITUID} AND DISABLED='false' ");
        command.append("AND EXTEN=");
        command.bind(extension);
        TBLPBXCUSTDEST.fill(command);
        if (TBLPBXCUSTDEST.getRowCount() == 0) {
            return false;
        }
        DataRow rowTBLPBXCUSTDEST = TBLPBXCUSTDEST.getRow(0);
        dialDESTPARAM(extension, request, channel, rowTBLPBXCUSTDEST.getString("DESTTYPE"), rowTBLPBXCUSTDEST.getString("DESTPARAM"));
        return true;
    }

    public boolean checkExtention(String extension, AgiRequest request, AgiChannel channel) throws Exception {
        Command command = new Command("SELECT A.* FROM TBLPBXEXT A,TBLPBX B ");
        command.append("WHERE A.UNITUID={UNITUID} AND A.UNITUID=B.UNITUID");
        command.append("AND A.PBXID=B.PBXID AND ( (A.PBXID=");
        command.bind(PBXID);
        command.append("AND A.EXTEN=");
        command.bind(extension);
        command.append(") OR (A.PBXID<>");
        command.bind(PBXID);
        command.append(") AND A.EXTEN=NVL(B.OUTBOUNDPREFIX,'')|| ");
        command.bind(extension);
        command.append(")");
        TBLPBXEXT.fill(command);
        if (TBLPBXEXT.getRowCount() == 0) {
            return false;
        }
        DataRow rowTBLPBXEXT = TBLPBXEXT.getRow(0);
        String CALLSCREEN = rowTBLPBXEXT.getString("CALLSCREEN");
        if ("memory".equalsIgnoreCase(CALLSCREEN)) {
            if (NullStatus.isNull(callerIdNumber)) {
                CALLSCREEN = "nomemory";
            }
        }
        if ("nomemory".equalsIgnoreCase(CALLSCREEN)) {
        } else if ("memory".equalsIgnoreCase(CALLSCREEN)) {
        }
        boolean busy = false;
        final ManagerConnection managerConnection = TegsoftPBX.createManagerConnection(rowTBLPBXEXT.getString("PBXID"));
        CommandAction commandAction = new CommandAction("sip show inuse");
        CommandResponse response = (CommandResponse) managerConnection.sendAction(commandAction, 10000);
        for (String line : response.getResult()) {
            if (line.indexOf("/") > 0) {
                if (line.startsWith(extension + " ")) {
                    if (line.length() > 26) {
                        if (line.charAt(26) != '0') {
                            busy = true;
                        }
                    }
                    break;
                }
            }
        }
        managerConnection.disconnect();
        TegsoftPBX.logMessage(channel, Level.INFO, "Extension " + extension + " current state " + (busy ? "is busy" : "is idle"));
        command = new Command("SELECT * FROM TBLPBXCF WHERE UNITUID={UNITUID} ");
        command.append("AND EXTID=");
        command.bind(rowTBLPBXEXT.getString("EXTID"));
        TBLPBXCF.fill(command);
        String CFAlways = "";
        String CFBusy = "";
        String CFNoAnswer = "";
        for (int i = 0; i < TBLPBXCF.getRowCount(); i++) {
            if (Compare.equal("Always", TBLPBXCF.getRow(i).getString("CFTYPE"))) {
                CFAlways = TBLPBXCF.getRow(i).getString("DESTINATION");
                if (NullStatus.isNotNull(CFAlways)) {
                    routeINTERNAL(CFAlways, request, channel);
                    return true;
                }
            } else if (Compare.equal("Busy", TBLPBXCF.getRow(i).getString("CFTYPE"))) {
                CFBusy = TBLPBXCF.getRow(i).getString("DESTINATION");
                if (busy) {
                    routeINTERNAL(CFBusy, request, channel);
                    return true;
                }
            } else if (Compare.equal("NoAnswer", TBLPBXCF.getRow(i).getString("CFTYPE"))) {
                CFNoAnswer = TBLPBXCF.getRow(i).getString("DESTINATION");
            }
        }
        if (busy) {
            if (!Compare.isTrue(rowTBLPBXEXT.getString("CALLWAIT"))) {
                String context = request.getParameter("context");
                if ("tegsoft-INCOMMING".equals(context)) {
                    TegsoftPBX.logMessage(channel, Level.INFO, "Call wait disabled. Extension " + extension + " is busy going for message");
                    channel.answer();
                    TegsoftPBX.playBack(channel, "IncommingBusy");
                    dialDESTPARAM(extension, request, channel, rowTBLPBX.getString("DESTTYPE"), rowTBLPBX.getString("DESTPARAM"));
                    return true;
                } else {
                    TegsoftPBX.logMessage(channel, Level.INFO, "Call wait disabled. Extension " + extension + " is busy going for hangup");
                    channel.exec("Busy", "20");
                    channel.exec("Hangup");
                    return true;
                }
            } else {
                TegsoftPBX.logMessage(channel, Level.INFO, "Call wait enabled for extension " + extension + ". Adding new call to extension");
            }
        }
        if ("Always".equals(rowTBLPBXEXT.getString("RECORDIN"))) {
            String folderName = "/var/spool/asterisk/monitor/" + new SimpleDateFormat("yyyy/MM/dd").format(new Date()) + "/";
            new File(folderName).mkdirs();
            Runtime.getRuntime().exec("chown asterisk.asterisk " + folderName);
            channel.exec("MixMonitor", "/var/spool/asterisk/monitor/" + new SimpleDateFormat("yyyy/MM/dd").format(new Date()) + "/TOBE-" + new SimpleDateFormat("yyyyMMdd").format(new Date()) + "-" + request.getUniqueId() + ".wav" + asteriskCommandSeperator + "a");
        } else {
            command = new Command("SELECT RECORDOUT FROM TBLPBXEXT WHERE UNITUID={UNITUID} ");
            command.append("AND EXTEN=");
            command.bind(callerIdNumber);
            String RECORDOUT = command.executeScalarAsString();
            if ("Always".equals(RECORDOUT)) {
                String folderName = "/var/spool/asterisk/monitor/" + new SimpleDateFormat("yyyy/MM/dd").format(new Date()) + "/";
                new File(folderName).mkdirs();
                Runtime.getRuntime().exec("chown asterisk.asterisk " + folderName);
                channel.exec("MixMonitor", "/var/spool/asterisk/monitor/" + new SimpleDateFormat("yyyy/MM/dd").format(new Date()) + "/TOBE-" + new SimpleDateFormat("yyyyMMdd").format(new Date()) + "-" + request.getUniqueId() + ".wav" + asteriskCommandSeperator + "a");
            }
        }
        String RINGTIME = rowTBLPBXEXT.getString("RINGTIME");
        int dialResult = 0;
        assignCallerID(request, channel, "CALLERID");
        if (!"DND".equals(rowTBLPBXEXT.getString("EXTSTATUS"))) {
            if (Compare.equal(PBXID, rowTBLPBXEXT.getString("PBXID"))) {
                dialResult = channel.exec("Dial", "SIP/" + extension + asteriskCommandSeperator + RINGTIME + asteriskCommandSeperator + "t");
            } else {
                dialResult = channel.exec("Dial", "SIP/" + rowTBLPBXEXT.getString("PBXID") + "/" + extension + asteriskCommandSeperator + RINGTIME + asteriskCommandSeperator + "t");
            }
        }
        if (dialResult == 0) {
            if (NullStatus.isNotNull(CFNoAnswer)) {
                routeINTERNAL(CFNoAnswer, request, channel);
                return true;
            }
            String context = request.getParameter("context");
            if ("tegsoft-INCOMMING".equals(context)) {
                TegsoftPBX.logMessage(channel, Level.INFO, "Extension unavailable, going for message");
                channel.answer();
                TegsoftPBX.playBack(channel, "IncommingUnavail");
                dialDESTPARAM(extension, request, channel, rowTBLPBX.getString("DESTTYPE"), rowTBLPBX.getString("DESTPARAM"));
                return true;
            }
        }
        return true;
    }

    public boolean checkFax(String extension, AgiRequest request, AgiChannel channel) throws Exception {
        Command command = new Command("SELECT A.* FROM TBLPBXFAX A,TBLPBX B ");
        command.append("WHERE A.UNITUID={UNITUID} AND A.UNITUID=B.UNITUID");
        command.append("AND A.PBXID=B.PBXID AND ( (A.PBXID=");
        command.bind(PBXID);
        command.append("AND A.EXTEN=");
        command.bind(extension);
        command.append(") OR (A.PBXID<>");
        command.bind(PBXID);
        command.append(") AND A.EXTEN=NVL(B.OUTBOUNDPREFIX,'')|| ");
        command.bind(extension);
        command.append(")");
        TBLPBXFAX.fill(command);
        if (TBLPBXFAX.getRowCount() == 0) {
            return false;
        }
        DataRow rowTBLPBXFAX = TBLPBXFAX.getRow(0);
        if (Compare.equal(PBXID, rowTBLPBXFAX.getString("PBXID"))) {
            channel.exec("Dial", "IAX2/" + rowTBLPBXFAX.getString("FAXID") + asteriskCommandSeperator + "20" + asteriskCommandSeperator + "t");
        } else {
            channel.exec("Dial", "SIP/" + rowTBLPBXFAX.getString("PBXID") + "/" + extension + asteriskCommandSeperator + "20" + asteriskCommandSeperator + "t");
        }
        return true;
    }

    public boolean checkQUEUE(String extension, AgiRequest request, AgiChannel channel) throws Exception {
        Command command = new Command("SELECT * FROM TBLCCSKILLS WHERE UNITUID={UNITUID} ");
        command.append("AND SKILL=");
        command.bind(extension);
        TBLCCSKILLS.fill(command);
        if (TBLCCSKILLS.getRowCount() == 0) {
            return false;
        }
        DataRow rowTBLCCSKILLS = TBLCCSKILLS.getRow(0);
        assignCallerID(request, channel, "CALLERID");
        if (NullStatus.isNotNull(rowTBLCCSKILLS.getString("JOINANNOUNCE"))) {
            TegsoftPBX.playBackground(channel, rowTBLCCSKILLS.getString("JOINANNOUNCE"));
        }
        if (Compare.isTrue(rowTBLCCSKILLS.getString("RECORDING"))) {
            String folderName = "/var/spool/asterisk/monitor/" + new SimpleDateFormat("yyyy/MM/dd").format(new Date()) + "/";
            new File(folderName).mkdirs();
            Runtime.getRuntime().exec("chown asterisk.asterisk " + folderName);
            channel.setVariable("MONITOR_FILENAME", "/var/spool/asterisk/monitor/" + new SimpleDateFormat("yyyy/MM/dd").format(new Date()) + "/TOBE-" + new SimpleDateFormat("yyyyMMdd").format(new Date()) + "-" + request.getUniqueId());
        }
        String options = "t";
        String url = "";
        String announce = "";
        String timeout = "";
        if ("RING".equals(rowTBLCCSKILLS.getString("MOHTYPE"))) {
            options += "r";
        }
        if (NullStatus.isNotNull(rowTBLCCSKILLS.getDecimal("MAXWAITTIME"))) {
            timeout = "" + rowTBLCCSKILLS.getDecimal("MAXWAITTIME");
        }
        if (NullStatus.isNotNull(rowTBLCCSKILLS.getString("MOHID"))) {
            channel.exec("Set", "CHANNEL(musicclass)=" + rowTBLCCSKILLS.getString("MOHID"));
        }
        channel.setVariable("QUEUENAME", extension);
        int rc = channel.exec("Queue", extension + asteriskCommandSeperator + options + asteriskCommandSeperator + url + asteriskCommandSeperator + announce + asteriskCommandSeperator + timeout);
        if (rc == 0) {
            channel.setVariable("CONTEXTID", rowTBLCCSKILLS.getString("CONTEXTID"));
            dialDESTPARAM(extension, request, channel, rowTBLCCSKILLS.getString("DESTTYPE"), rowTBLCCSKILLS.getString("DESTPARAM"));
            return true;
        }
        return true;
    }

    public static ArrayList<DialPlanResult> createDialplan(String CONTEXTID, String extension) throws Exception {
        ArrayList<DialPlanResult> dialPlanResults = new ArrayList<DialPlanResult>();
        Dataset TBLPBXTRUNKRULE = new Dataset("TBLPBXTRUNKRULE", "TBLPBXTRUNKRULE");
        Dataset TBLPBXTRUNK = new Dataset("TBLPBXTRUNK", "TBLPBXTRUNK");
        Dataset TBLPBXOUTRTPTNR = new Dataset("TBLPBXOUTRTPTNR", "TBLPBXOUTRTPTNR");
        Dataset TBLPBXOUTROUTE = new Dataset("TBLPBXOUTROUTE", "TBLPBXOUTROUTE");
        Dataset TBLPBXCONTEXTROUTE = new Dataset("TBLPBXCONTEXTROUTE", "TBLPBXCONTEXTROUTE");
        Command command = new Command("SELECT * FROM TBLPBXCONTEXTROUTE WHERE UNITUID={UNITUID} AND  CONTEXTID IN (SELECT CONTEXTID FROM TBLPBXCONTEXT WHERE UNITUID={UNITUID} AND DISABLED='false' AND CONTEXTID=");
        command.bind(CONTEXTID);
        command.append(")");
        command.append("ORDER BY ORDERID");
        TBLPBXCONTEXTROUTE.fill(command);
        for (int i = 0; i < TBLPBXCONTEXTROUTE.getRowCount(); i++) {
            DataRow rowTBLPBXCONTEXTROUTE = TBLPBXCONTEXTROUTE.getRow(i);
            command = new Command("SELECT * FROM TBLPBXOUTROUTE WHERE UNITUID={UNITUID} AND DISABLED='false' AND ROUTEID=");
            command.bind(rowTBLPBXCONTEXTROUTE.getString("ROUTEID"));
            TBLPBXOUTROUTE.fill(command);
            for (int j = 0; j < TBLPBXOUTROUTE.getRowCount(); j++) {
                DataRow rowTBLPBXOUTROUTE = TBLPBXOUTROUTE.getRow(j);
                if (!checkTimeCondition(null, rowTBLPBXOUTROUTE.getString("TIMETYPE"), rowTBLPBXOUTROUTE.getString("TIMECONDID"))) {
                    continue;
                }
                command = new Command("SELECT * FROM TBLPBXOUTRTPTNR WHERE UNITUID={UNITUID} AND ROUTEID=");
                command.bind(rowTBLPBXOUTROUTE.getString("ROUTEID"));
                TBLPBXOUTRTPTNR.fill(command);
                for (int k = 0; k < TBLPBXOUTRTPTNR.getRowCount(); k++) {
                    String numberToDialRoute = extension;
                    DataRow rowTBLPBXOUTRTPTNR = TBLPBXOUTRTPTNR.getRow(k);
                    if (Compare.checkPattern(rowTBLPBXOUTRTPTNR.getString("PATTERN"), numberToDialRoute)) {
                        if (NullStatus.isNotNull(rowTBLPBXOUTRTPTNR.getString("PREFIX"))) {
                            numberToDialRoute = numberToDialRoute.substring(Integer.parseInt(rowTBLPBXOUTRTPTNR.getString("PREFIX")));
                        }
                        if (NullStatus.isNotNull(rowTBLPBXOUTRTPTNR.getString("PREPEND"))) {
                            numberToDialRoute = rowTBLPBXOUTRTPTNR.getString("PREPEND") + numberToDialRoute;
                        }
                        command = new Command("SELECT A.* FROM TBLPBXTRUNK A,TBLPBXOUTRTTRNK B WHERE A.UNITUID={UNITUID} AND A.UNITUID=B.UNITUID AND A.TRUNKID=B.TRUNKID AND B.ROUTEID=");
                        command.bind(rowTBLPBXOUTRTPTNR.getString("ROUTEID"));
                        command.append("ORDER BY B.ORDERID");
                        TBLPBXTRUNK.fill(command);
                        for (int p = 0; p < TBLPBXTRUNK.getRowCount(); p++) {
                            String numberToDialTrunk = numberToDialRoute;
                            DataRow rowTBLPBXTRUNK = TBLPBXTRUNK.getRow(p);
                            command = new Command("SELECT * FROM TBLPBXTRUNKRULE WHERE UNITUID={UNITUID} AND TRUNKID=");
                            command.bind(rowTBLPBXTRUNK.getString("TRUNKID"));
                            TBLPBXTRUNKRULE.fill(command);
                            for (int t = 0; t < TBLPBXTRUNKRULE.getRowCount(); t++) {
                                DataRow rowTBLPBXTRUNKRULE = TBLPBXTRUNKRULE.getRow(t);
                                if (Compare.checkPattern(rowTBLPBXTRUNKRULE.getString("PATTERN"), numberToDialTrunk)) {
                                    if (NullStatus.isNotNull(rowTBLPBXTRUNKRULE.getString("PREFIX"))) {
                                        numberToDialTrunk = numberToDialTrunk.substring(Integer.parseInt(rowTBLPBXTRUNKRULE.getString("PREFIX")));
                                    }
                                    if (NullStatus.isNotNull(rowTBLPBXTRUNKRULE.getString("PREPEND"))) {
                                        numberToDialTrunk = rowTBLPBXTRUNKRULE.getString("PREPEND") + numberToDialTrunk;
                                    }
                                }
                            }
                            DialPlanResult dialPlanResult = new DialPlanResult();
                            dialPlanResult.setInfoTBLPBXOUTROUTE("TBLPBXOUTROUTE " + rowTBLPBXOUTROUTE.getString("ROUTENAME") + " " + rowTBLPBXOUTROUTE.getString("ROUTEID"));
                            dialPlanResult.setInfoTBLPBXOUTRTPTNR("TBLPBXOUTRTPTNR " + rowTBLPBXOUTRTPTNR.getString("PATTERN") + " " + rowTBLPBXOUTRTPTNR.getString("NOTES"));
                            dialPlanResult.setInfoTBLPBXTRUNK("TBLPBXTRUNK " + rowTBLPBXTRUNK.getString("TRUNKNAME") + " " + rowTBLPBXTRUNK.getString("TRUNKID"));
                            dialPlanResult.setInfoDial("SIP/" + rowTBLPBXTRUNK.getString("TRUNKID") + "/" + numberToDialTrunk);
                            dialPlanResult.setRCLIDTYPE(rowTBLPBXOUTROUTE.getString("CLIDTYPE"));
                            dialPlanResult.setROUTECID(rowTBLPBXOUTROUTE.getString("ROUTECID"));
                            dialPlanResult.setCLIDTYPE(rowTBLPBXTRUNK.getString("CLIDTYPE"));
                            dialPlanResult.setTRUNKCID(rowTBLPBXTRUNK.getString("TRUNKCID"));
                            dialPlanResult.setTrunkid(rowTBLPBXTRUNK.getString("TRUNKID"));
                            dialPlanResult.setRouteid(rowTBLPBXOUTROUTE.getString("ROUTEID"));
                            dialPlanResult.setMOHID(rowTBLPBXOUTROUTE.getString("MOHID"));
                            dialPlanResult.setPatternid(rowTBLPBXOUTRTPTNR.getString("PATTERNID"));
                            dialPlanResults.add(dialPlanResult);
                        }
                        return dialPlanResults;
                    }
                }
            }
        }
        return dialPlanResults;
    }

    private boolean checkTrunk(String extension, AgiRequest request, AgiChannel channel) throws Exception {
        if ("INBOUND".equals(channel.getVariable("DIRECTION"))) {
            channel.setVariable("DIRECTION", "TRANSIT");
            channel.exec("Set", "CDR(DIRECTION)=TRANSIT");
        } else {
            channel.setVariable("DIRECTION", "OUTBOUND");
            channel.exec("Set", "CDR(DIRECTION)=OUTBOUND");
        }
        String CONTEXTID = channel.getVariable("CONTEXTID");
        boolean enableRecording = false;
        Command command = new Command("SELECT * FROM TBLPBXEXT WHERE UNITUID={UNITUID} AND PBXID=");
        command.bind(PBXID);
        command.append("AND EXTEN=");
        command.bind(callerIdNumber);
        TBLPBXEXT.fill(command);
        if (TBLPBXEXT.getRowCount() > 0) {
            DataRow rowTBLPBXEXT = TBLPBXEXT.getRow(0);
            if (NullStatus.isNull(CONTEXTID)) {
                CONTEXTID = rowTBLPBXEXT.getString("CONTEXTID");
            }
            if (!"Never".equals(rowTBLPBXEXT.getString("RECORDOUT"))) {
                enableRecording = true;
            }
        }
        if (NullStatus.isNull(CONTEXTID)) {
            command = new Command("SELECT * FROM TBLPBXFAX WHERE UNITUID={UNITUID} AND PBXID=");
            command.bind(PBXID);
            command.append("AND EXTEN=");
            command.bind(callerIdNumber);
            TBLPBXFAX.fill(command);
            if (TBLPBXFAX.getRowCount() > 0) {
                CONTEXTID = TBLPBXFAX.getRow(0).getString("CONTEXTID");
            }
        }
        if (NullStatus.isNull(CONTEXTID)) {
            TegsoftPBX.logMessage(channel, Level.INFO, "No profile set cannot dial out");
            return false;
        }
        ArrayList<DialPlanResult> dialPlanResults = createDialplan(CONTEXTID, extension);
        for (int i = 0; i < dialPlanResults.size(); i++) {
            DialPlanResult dialPlanResult = dialPlanResults.get(i);
            if (NullStatus.isNotNull(dialPlanResult.getROUTECID())) {
                channel.setCallerId(StringUtil.convertToEnglishOnlyLetters(dialPlanResult.getROUTECID()));
            } else {
                if (NullStatus.isNull(dialPlanResult.getRCLIDTYPE())) {
                    assignCallerID(request, channel, "OUTCALLERID");
                } else if ("Internal".equals(dialPlanResult.getRCLIDTYPE())) {
                    assignCallerID(request, channel, "CALLERID");
                } else if ("Outbound".equals(dialPlanResult.getRCLIDTYPE())) {
                    assignCallerID(request, channel, "OUTCALLERID");
                }
            }
            if (NullStatus.isNotNull(dialPlanResult.getTRUNKCID())) {
                channel.setCallerId(StringUtil.convertToEnglishOnlyLetters(dialPlanResult.getTRUNKCID()));
            } else {
                if (NullStatus.isNull(dialPlanResult.getCLIDTYPE())) {
                    assignCallerID(request, channel, "OUTCALLERID");
                } else if ("Internal".equals(dialPlanResult.getCLIDTYPE())) {
                    assignCallerID(request, channel, "CALLERID");
                } else if ("Outbound".equals(dialPlanResult.getCLIDTYPE())) {
                    assignCallerID(request, channel, "OUTCALLERID");
                }
            }
            if (enableRecording) {
                String UNIQUEID = channel.getUniqueId();
                String yyyyMMdd = new SimpleDateFormat("yyyyMMdd").format(DateUtil.now());
                String folderName = "/var/spool/asterisk/monitor/" + new SimpleDateFormat("yyyy/MM/dd").format(new Date()) + "/";
                new File(folderName).mkdirs();
                Runtime.getRuntime().exec("chown asterisk.asterisk " + folderName);
                channel.exec("MixMonitor", "/var/spool/asterisk/monitor/" + new SimpleDateFormat("yyyy/MM/dd").format(new Date()) + "/TOBE-" + yyyyMMdd + "-" + UNIQUEID + ".wav" + asteriskCommandSeperator + "a");
            }
            TegsoftPBX.logMessage(channel, Level.INFO, dialPlanResult.getInfoTBLPBXOUTROUTE());
            TegsoftPBX.logMessage(channel, Level.INFO, dialPlanResult.getInfoTBLPBXOUTRTPTNR());
            TegsoftPBX.logMessage(channel, Level.INFO, dialPlanResult.getInfoTBLPBXTRUNK());
            if (NullStatus.isNotNull(dialPlanResult.getMOHID())) {
                channel.exec("Set", "CHANNEL(musicclass)=" + dialPlanResult.getMOHID());
            }
            channel.exec("Dial", dialPlanResult.getInfoDial() + asteriskCommandSeperator + "300" + asteriskCommandSeperator + "t");
            String DIALSTATUS = channel.getVariable("DIALSTATUS");
            dialPlanResult.setDialStatus(DIALSTATUS);
            if ("ANSWER".equalsIgnoreCase(DIALSTATUS) || "BUSY".equalsIgnoreCase(DIALSTATUS) || "NOANSWER".equalsIgnoreCase(DIALSTATUS) || "CANCEL".equalsIgnoreCase(DIALSTATUS) || "DONTCALL".equalsIgnoreCase(DIALSTATUS) || "TORTURE".equalsIgnoreCase(DIALSTATUS) || "INVALIDARGS".equalsIgnoreCase(DIALSTATUS)) {
                return true;
            }
        }
        if (dialPlanResults.size() > 0) {
            return true;
        }
        return false;
    }

    private void hangup(AgiRequest request, AgiChannel channel) throws Exception {
        channel.exec("Hangup");
    }
}
