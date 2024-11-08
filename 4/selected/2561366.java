package com.tegsoft.pbx;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import org.asteriskjava.manager.action.CommandAction;
import org.asteriskjava.manager.action.SipShowPeerAction;
import org.asteriskjava.manager.event.StatusEvent;
import org.asteriskjava.manager.response.CommandResponse;
import org.asteriskjava.manager.response.ManagerResponse;
import org.asteriskjava.manager.response.SipShowPeerResponse;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.Progressmeter;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;
import com.tegsoft.tobe.db.command.Command;
import com.tegsoft.tobe.db.dataset.DataRow;
import com.tegsoft.tobe.db.dataset.Dataset;
import com.tegsoft.tobe.os.LicenseManager;
import com.tegsoft.tobe.util.Converter;
import com.tegsoft.tobe.util.NullStatus;
import com.tegsoft.tobe.util.StringUtil;
import com.tegsoft.tobe.util.UiUtil;
import com.tegsoft.tobe.util.message.MessageUtil;

public class Monitoring {

    public static void refreshInformation() {
        refreshInformation(null);
    }

    public static void refreshInformation(Event event) {
        try {
            Object parent = null;
            if (event != null) {
                parent = event.getTarget().getParent();
            } else {
                parent = UiUtil.findComponent("parentForm");
            }
            clearAll(parent);
            String PBXID = TegsoftPBX.getLocalPBXID();
            Process process = Runtime.getRuntime().exec("/root/tegsoft_systemstatus.sh");
            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = null;
            while ((line = in.readLine()) != null) {
                if (NullStatus.isNull(line)) {
                    continue;
                }
                line = line.trim();
                if (line.indexOf("load average:") > 0) {
                    String loadPart = line.substring(line.indexOf("load average:") + "load average:".length());
                    String loadAvarages[] = loadPart.split(",");
                    BigDecimal loadAvarage = BigDecimal.ZERO;
                    for (int i = 0; i < loadAvarages.length; i++) {
                        loadAvarage = loadAvarage.add(new BigDecimal(loadAvarages[i].trim()));
                    }
                    if (loadAvarages.length > 0) {
                        loadAvarage = loadAvarage.divide(new BigDecimal(loadAvarages.length), 2, BigDecimal.ROUND_HALF_DOWN);
                    }
                    ((Progressmeter) UiUtil.findComponent(parent, "LoadAvarage")).setValue(loadAvarage.intValue() % 100);
                    ((Label) UiUtil.findComponent(parent, "LoadAvarageValue")).setValue(Converter.asNotNullFormattedString(loadAvarage, "#,##0.00") + "%");
                    continue;
                }
                if (line.endsWith("active call") || line.endsWith("active calls")) {
                    if (line.indexOf(" ") > 0) {
                        String items[] = line.split(" ");
                        if (items.length > 0) {
                            String item = items[0];
                            ((Label) UiUtil.findComponent(parent, "TotalActiveCalls")).setValue(item.trim());
                            continue;
                        }
                    }
                }
                if (line.endsWith("active channel") || line.endsWith("active channels")) {
                    if (line.indexOf(" ") > 0) {
                        String items[] = line.split(" ");
                        if (items.length > 0) {
                            String item = items[0];
                            ((Label) UiUtil.findComponent(parent, "ActiveChannels")).setValue(item.trim());
                            continue;
                        }
                    }
                }
                if (line.startsWith("PHONES:")) {
                    String item = line.substring(line.indexOf("PHONES:") + "PHONES:".length());
                    Command command = new Command("SELECT COUNT(*) FROM TBLPBXEXT WHERE UNITUID={UNITUID} AND PBXID=");
                    command.bind(PBXID);
                    BigDecimal phoneCount = command.executeScalarAsDecimal();
                    BigDecimal onlinePhoneCount = new BigDecimal(item.trim());
                    if (phoneCount.doubleValue() > 0) {
                        BigDecimal active = onlinePhoneCount.divide(phoneCount, 2, BigDecimal.ROUND_HALF_DOWN).multiply(new BigDecimal("100"));
                        ((Progressmeter) UiUtil.findComponent(parent, "OnlinePhones")).setValue(active.intValue() % 100);
                        ((Label) UiUtil.findComponent(parent, "OnlinePhonesValue")).setValue("" + onlinePhoneCount.intValue() + "/" + phoneCount.intValue());
                    } else {
                        ((Progressmeter) UiUtil.findComponent(parent, "OnlinePhones")).setValue(0);
                        ((Label) UiUtil.findComponent(parent, "OnlinePhonesValue")).setValue("0");
                    }
                    continue;
                }
                if (line.startsWith("/dev/sda")) {
                    String items[] = line.split(" ");
                    for (int i = 0; i < items.length; i++) {
                        if (NullStatus.isNotNull(items[i])) {
                            if (items[i].indexOf("%") > 0) {
                                String item = items[i].trim();
                                int usage = Integer.parseInt(item.substring(0, item.indexOf("%")));
                                ((Progressmeter) UiUtil.findComponent(parent, "TotalSpace")).setValue(usage % 100);
                                ((Label) UiUtil.findComponent(parent, "TotalSpaceValue")).setValue("" + usage + "%");
                            }
                        }
                    }
                    continue;
                }
            }
            in.close();
            BigDecimal maxMemory = new BigDecimal(Runtime.getRuntime().maxMemory()).divide(new BigDecimal("1000000")).setScale(0, BigDecimal.ROUND_DOWN);
            BigDecimal usedMemory = maxMemory.subtract(new BigDecimal(Runtime.getRuntime().freeMemory()).divide(new BigDecimal("1000000")).setScale(0, BigDecimal.ROUND_DOWN));
            BigDecimal usageMem = usedMemory.divide(maxMemory, 2, BigDecimal.ROUND_HALF_DOWN).multiply(new BigDecimal("100"));
            ((Progressmeter) UiUtil.findComponent(parent, "MemUsage")).setValue(usageMem.intValue() % 100);
            ((Label) UiUtil.findComponent(parent, "MemUsageValue")).setValue("" + usedMemory + "MB /" + maxMemory + "MB " + usageMem + "%");
            BigDecimal allAgentsCount = new Command("SELECT COUNT(*) FROM TBLCCAGENT WHERE UNITUID={UNITUID}").executeScalarAsDecimal();
            BigDecimal onlineAgentsCount = new Command("SELECT COUNT(*) FROM TBLCCAGENTLOG WHERE UNITUID={UNITUID} AND LOGTYPE='LOGIN' AND ENDDATE IS NULL").executeScalarAsDecimal();
            if (allAgentsCount.doubleValue() > 0) {
                BigDecimal active = onlineAgentsCount.divide(allAgentsCount, 2, BigDecimal.ROUND_HALF_DOWN).multiply(new BigDecimal("100"));
                ((Progressmeter) UiUtil.findComponent(parent, "LoginAgent")).setValue(active.intValue() % 100);
                ((Label) UiUtil.findComponent(parent, "LoginAgentValue")).setValue("" + onlineAgentsCount.intValue() + "/" + allAgentsCount.intValue());
                BigDecimal readyAgentsCount = new Command("SELECT COUNT(*) FROM TBLCCAGENTLOG WHERE UNITUID={UNITUID} AND LOGTYPE='LOGIN' AND ENDDATE IS NULL AND UID NOT IN (SELECT UID FROM TBLCCAGENTLOG WHERE UNITUID={UNITUID} AND LOGTYPE='READY' AND ENDDATE IS NULL)").executeScalarAsDecimal();
                active = readyAgentsCount.divide(allAgentsCount, 2, BigDecimal.ROUND_HALF_DOWN).multiply(new BigDecimal("100"));
                ((Progressmeter) UiUtil.findComponent(parent, "ReadyAgent")).setValue(active.intValue() % 100);
                ((Label) UiUtil.findComponent(parent, "ReadyAgentValue")).setValue("" + readyAgentsCount.intValue() + "/" + allAgentsCount.intValue());
            } else {
                ((Progressmeter) UiUtil.findComponent(parent, "LoginAgent")).setValue(0);
                ((Label) UiUtil.findComponent(parent, "LoginAgentValue")).setValue("0");
                ((Progressmeter) UiUtil.findComponent(parent, "ReadyAgent")).setValue(0);
                ((Label) UiUtil.findComponent(parent, "ReadyAgentValue")).setValue("0");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void refreshPanel(Event event) {
        try {
            if (UiUtil.findComponent("TRUNKS0") == null) {
                return;
            }
            ManagerConnection managerConnection = TegsoftPBX.createManagerConnection(TegsoftPBX.getLocalPBXID());
            managerConnection.prepareChannels();
            CommandAction commandAction = new CommandAction("sip show inuse");
            CommandResponse sipInUseResponse = (CommandResponse) managerConnection.sendAction(commandAction, 10000);
            List<String> sipInUseLines = sipInUseResponse.getResult();
            refreshTrunks(managerConnection, sipInUseLines);
            refreshSKILLS(managerConnection);
            refreshExtensions(managerConnection, sipInUseLines);
            refreshAgents(managerConnection, sipInUseLines);
            managerConnection.disconnect();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static void refreshTrunks(ManagerConnection managerConnection, List<String> sipInUseLines) {
        try {
            Dataset TBLPBXTRUNK = UiUtil.getDataset("TBLPBXTRUNK");
            TBLPBXTRUNK.fill(UiUtil.getDbCommand("TBLPBXTRUNK"));
            for (int j = 0; j < 3; j++) {
                Vbox target = (Vbox) UiUtil.findComponent("TRUNKS" + j);
                target.getChildren().clear();
                for (int i = j * 3; i < TBLPBXTRUNK.getRowCount(); i++) {
                    DataRow rowTBLPBXTRUNK = TBLPBXTRUNK.getRow(i);
                    Window window = new Window();
                    window.setWidth("120px");
                    window.setBorder("normal");
                    window.setTitle(StringUtil.left(StringUtil.convertToEnglishOnlyLetters(Converter.asNotNullString(rowTBLPBXTRUNK.getString("TRUNKNAME"))).toUpperCase(Locale.ENGLISH), 17));
                    Image image1 = new Image("/image/buttons/ok_unsaved.png");
                    image1.setWidth("15px");
                    image1.setHeight("15px");
                    image1.setVisible(false);
                    UiUtil.addComponent(window, image1);
                    Image image2 = new Image("/image/crm/skype/Error_16x16.png");
                    image2.setWidth("15px");
                    image2.setHeight("15px");
                    image2.setVisible(false);
                    UiUtil.addComponent(window, image2);
                    Image image3 = new Image("/image/crm/skype/CallPhones_20x16.png");
                    image3.setWidth("15px");
                    image3.setHeight("15px");
                    image3.setVisible(false);
                    UiUtil.addComponent(window, image3);
                    Image image4 = new Image("/image/crm/skype/Sound_16x16.png");
                    image4.setWidth("15px");
                    image4.setHeight("15px");
                    image4.setVisible(false);
                    UiUtil.addComponent(window, image4);
                    Label label1 = new Label();
                    label1.setVisible(false);
                    UiUtil.addComponent(window, label1);
                    ManagerResponse managerResponse = managerConnection.sendAction(new SipShowPeerAction(rowTBLPBXTRUNK.getString("TRUNKID")), 1000);
                    if (managerResponse != null) {
                        if (SipShowPeerResponse.class.isAssignableFrom(managerResponse.getClass())) {
                            SipShowPeerResponse peerDetails = (SipShowPeerResponse) managerResponse;
                            if (peerDetails != null) {
                                if (peerDetails.getAttributes() != null) {
                                    if (peerDetails.getAttributes().keySet() != null) {
                                        Iterator<String> keys = peerDetails.getAttributes().keySet().iterator();
                                        while (keys.hasNext()) {
                                            String key = keys.next();
                                            String value = Converter.asNotNullString(peerDetails.getAttributes().get(key));
                                            if ("status".equals(key)) {
                                                if (value.startsWith("OK")) {
                                                    image1.setVisible(true);
                                                } else {
                                                    image2.setVisible(true);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    for (String line : sipInUseLines) {
                        if (line.indexOf("/") > 0) {
                            if (line.indexOf(" ") > 0) {
                                String peerName = line.substring(0, line.indexOf(" "));
                                if (rowTBLPBXTRUNK.getString("TRUNKID").startsWith(peerName)) {
                                    String count = Converter.asNotNullString(line.substring(line.indexOf(" ") + 1, line.indexOf("/", line.indexOf(" ")))).trim();
                                    if (Integer.parseInt(count) > 0) {
                                        image4.setVisible(true);
                                        label1.setVisible(true);
                                        label1.setValue(count);
                                    } else {
                                        image3.setVisible(true);
                                    }
                                    break;
                                }
                            }
                        }
                    }
                    UiUtil.addComponent(target, window);
                    if ((i + 1) % 3 == 0) {
                        break;
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static void refreshExtensions(ManagerConnection managerConnection, List<String> sipInUseLines) {
        try {
            Command command = new Command("SELECT A.* FROM TBLPBXEXT A WHERE A.UNITUID={UNITUID}");
            if (LicenseManager.isValid("TegsoftCC") > 0) {
                command.append("AND A.EXTID IN (SELECT B.EXTID FROM TBLPBXEXTUID B WHERE B.UNITUID={UNITUID} AND B.UID={UID})");
            }
            command.append("AND A.PBXID=");
            command.bind(TegsoftPBX.getLocalPBXID());
            command.append("ORDER BY A.EXTEN");
            Dataset TBLPBXEXT = UiUtil.getDataset("TBLPBXEXT");
            TBLPBXEXT.fill(command);
            for (int j = 0; j < 7; j++) {
                Vbox target = (Vbox) UiUtil.findComponent("EXTENSIONS" + j);
                target.getChildren().clear();
                for (int i = j * 10; i < TBLPBXEXT.getRowCount(); i++) {
                    DataRow rowTBLPBXEXT = TBLPBXEXT.getRow(i);
                    Window window = new Window();
                    window.setWidth("120px");
                    window.setBorder("normal");
                    window.setTitle(StringUtil.left(StringUtil.convertToEnglishOnlyLetters(Converter.asNotNullString(rowTBLPBXEXT.getString("EXTEN")) + ":" + Converter.asNotNullString(rowTBLPBXEXT.getString("CALLERID"))).toUpperCase(Locale.ENGLISH), 17));
                    Image image1 = new Image("/image/buttons/ok_unsaved.png");
                    image1.setWidth("15px");
                    image1.setHeight("15px");
                    image1.setVisible(false);
                    UiUtil.addComponent(window, image1);
                    Image image2 = new Image("/image/crm/skype/Error_16x16.png");
                    image2.setWidth("15px");
                    image2.setHeight("15px");
                    image2.setVisible(false);
                    UiUtil.addComponent(window, image2);
                    Image image3 = new Image("/image/crm/skype/CallPhones_20x16.png");
                    image3.setWidth("15px");
                    image3.setHeight("15px");
                    image3.setVisible(false);
                    UiUtil.addComponent(window, image3);
                    Image image4 = new Image("/image/crm/skype/Sound_16x16.png");
                    image4.setWidth("15px");
                    image4.setHeight("15px");
                    image4.setVisible(false);
                    UiUtil.addComponent(window, image4);
                    Label label1 = new Label();
                    label1.setVisible(false);
                    UiUtil.addComponent(window, label1);
                    ManagerResponse managerResponse = managerConnection.sendAction(new SipShowPeerAction(rowTBLPBXEXT.getString("EXTEN")), 1000);
                    if (managerResponse != null) {
                        if (SipShowPeerResponse.class.isAssignableFrom(managerResponse.getClass())) {
                            SipShowPeerResponse peerDetails = (SipShowPeerResponse) managerResponse;
                            if (peerDetails != null) {
                                if (peerDetails.getAttributes() != null) {
                                    if (peerDetails.getAttributes().keySet() != null) {
                                        Iterator<String> keys = peerDetails.getAttributes().keySet().iterator();
                                        while (keys.hasNext()) {
                                            String key = keys.next();
                                            String value = Converter.asNotNullString(peerDetails.getAttributes().get(key));
                                            if ("status".equals(key)) {
                                                if (value.startsWith("OK")) {
                                                    image1.setVisible(true);
                                                } else {
                                                    image2.setVisible(true);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    for (String line : sipInUseLines) {
                        if (line.indexOf("/") > 0) {
                            if (line.indexOf(" ") > 0) {
                                String peerName = line.substring(0, line.indexOf(" "));
                                if (rowTBLPBXEXT.getString("EXTEN").startsWith(peerName)) {
                                    String count = Converter.asNotNullString(line.substring(line.indexOf(" ") + 1, line.indexOf("/", line.indexOf(" ")))).trim();
                                    if (Integer.parseInt(count) > 0) {
                                        image4.setVisible(true);
                                        label1.setVisible(true);
                                        label1.setValue(count);
                                    } else {
                                        image3.setVisible(true);
                                    }
                                    break;
                                }
                            }
                        }
                    }
                    UiUtil.addComponent(target, window);
                    if ((i + 1) % 10 == 0) {
                        break;
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static void refreshAgents(ManagerConnection managerConnection, List<String> sipInUseLines) {
        try {
            if (LicenseManager.isValid("TegsoftCC") <= 0) {
                return;
            }
            Dataset TBLUSERS = UiUtil.getDataset("TBLUSERS");
            TBLUSERS.fill(UiUtil.getDbCommand("TBLUSERS"));
            for (int j = 0; j < 7; j++) {
                Vbox target = (Vbox) UiUtil.findComponent("AGENTS" + j);
                target.getChildren().clear();
                for (int i = j * 10; i < TBLUSERS.getRowCount(); i++) {
                    DataRow rowTBLUSERS = TBLUSERS.getRow(i);
                    Window window = new Window();
                    window.setWidth("120px");
                    window.setBorder("normal");
                    Image image1 = new Image("/image/buttons/ok_unsaved.png");
                    image1.setWidth("15px");
                    image1.setHeight("15px");
                    image1.setVisible(false);
                    UiUtil.addComponent(window, image1);
                    Image image2 = new Image("/image/crm/skype/Error_16x16.png");
                    image2.setWidth("15px");
                    image2.setHeight("15px");
                    image2.setVisible(false);
                    UiUtil.addComponent(window, image2);
                    Image image3 = new Image("/image/crm/skype/ContactsOkay_16x16.png");
                    image3.setWidth("15px");
                    image3.setHeight("15px");
                    image3.setVisible(false);
                    UiUtil.addComponent(window, image3);
                    Image image4 = new Image("/image/crm/skype/BlockContact_16x16.png");
                    image4.setWidth("15px");
                    image4.setHeight("15px");
                    image4.setVisible(false);
                    UiUtil.addComponent(window, image4);
                    Image image5 = new Image("/image/crm/skype/CallPhones_20x16.png");
                    image5.setWidth("15px");
                    image5.setHeight("15px");
                    image5.setVisible(false);
                    UiUtil.addComponent(window, image5);
                    Image image6 = new Image("/image/crm/skype/Sound_16x16.png");
                    image6.setWidth("15px");
                    image6.setHeight("15px");
                    image6.setVisible(false);
                    UiUtil.addComponent(window, image6);
                    Label label1 = new Label();
                    label1.setVisible(false);
                    UiUtil.addComponent(window, label1);
                    Command command = new Command("SELECT INTERFACE FROM TBLCCAGENTLOG WHERE UNITUID={UNITUID} AND LOGTYPE='LOGIN' AND ENDDATE IS NULL");
                    command.append(" AND UID=");
                    command.bind(rowTBLUSERS.getString("UID"));
                    String agentINTERFACE = command.executeScalarAsString();
                    boolean isAgentLogin = NullStatus.isNotNull(agentINTERFACE);
                    command = new Command("SELECT A.REASON FROM TBLCCAGENTLOG A WHERE A.UNITUID={UNITUID} AND A.LOGTYPE='READY' AND A.ENDDATE IS NULL");
                    command.append(" AND UID=");
                    command.bind(rowTBLUSERS.getString("UID"));
                    String REASON = command.executeScalarAsString();
                    boolean isAgentReady = NullStatus.isNull(REASON);
                    if (isAgentLogin) {
                        image1.setVisible(true);
                        String count = "";
                        String callerId = "";
                        String INTERFACE = agentINTERFACE;
                        if (INTERFACE.indexOf("/") >= 0) {
                            INTERFACE = INTERFACE.substring(INTERFACE.indexOf("/") + 1);
                            for (String line : sipInUseLines) {
                                if (line.indexOf("/") > 0) {
                                    if (line.indexOf(" ") > 0) {
                                        String peerName = line.substring(0, line.indexOf(" "));
                                        if (INTERFACE.startsWith(peerName)) {
                                            count = Converter.asNotNullString(line.substring(line.indexOf(" ") + 1, line.indexOf("/", line.indexOf(" ")))).trim();
                                            if (Integer.parseInt(count) > 0) {
                                                image6.setVisible(true);
                                            } else {
                                                image5.setVisible(true);
                                            }
                                            break;
                                        }
                                    }
                                }
                            }
                            for (int k = 0; k < managerConnection.getChannels().size(); k++) {
                                StatusEvent statusEvent = managerConnection.getChannels().get(k);
                                String channel = statusEvent.getChannel();
                                if (NullStatus.isNotNull(channel)) {
                                    if (channel.startsWith(agentINTERFACE)) {
                                        callerId = statusEvent.getCallerIdNum();
                                    }
                                }
                            }
                        }
                        if (isAgentReady) {
                            image3.setVisible(true);
                            label1.setVisible(true);
                            label1.setValue(MessageUtil.getMessage(Monitoring.class, Messages.readyText, callerId));
                        } else {
                            image4.setVisible(true);
                            label1.setVisible(true);
                            label1.setValue(MessageUtil.getMessage(Monitoring.class, Messages.notReadyText, callerId, MessageUtil.getMessage(Monitoring.class, REASON)));
                        }
                        String name = StringUtil.left(StringUtil.convertToEnglishOnlyLetters(Converter.asNotNullString(rowTBLUSERS.getString("USERNAME")) + " " + Converter.asNotNullString(rowTBLUSERS.getString("SURNAME"))).toUpperCase(Locale.ENGLISH), 12);
                        String title = name + " " + INTERFACE;
                        window.setTitle(title);
                    } else {
                        image2.setVisible(true);
                        window.setTitle(StringUtil.left(StringUtil.convertToEnglishOnlyLetters(Converter.asNotNullString(rowTBLUSERS.getString("USERNAME")) + " " + Converter.asNotNullString(rowTBLUSERS.getString("SURNAME"))).toUpperCase(Locale.ENGLISH), 17));
                    }
                    UiUtil.addComponent(target, window);
                    if ((i + 1) % 10 == 0) {
                        break;
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static void refreshSKILLS(ManagerConnection managerConnection) {
        try {
            Dataset TBLCCSKILLS = UiUtil.getDataset("TBLCCSKILLS");
            TBLCCSKILLS.fill(UiUtil.getDbCommand("TBLCCSKILLS"));
            CommandAction commandAction = new CommandAction("queue show");
            CommandResponse queueShowResponse = (CommandResponse) managerConnection.sendAction(commandAction, 2000);
            if (queueShowResponse == null) {
                return;
            }
            List<String> queueShowLines = queueShowResponse.getResult();
            for (int j = 0; j < 3; j++) {
                Vbox target = (Vbox) UiUtil.findComponent("SKILLS" + j);
                target.getChildren().clear();
                for (int i = j * 3; i < TBLCCSKILLS.getRowCount(); i++) {
                    DataRow rowTBLCCSKILLS = TBLCCSKILLS.getRow(i);
                    Window window = new Window();
                    window.setWidth("120px");
                    window.setBorder("normal");
                    String name = StringUtil.left(StringUtil.convertToEnglishOnlyLetters(Converter.asNotNullString(rowTBLCCSKILLS.getString("NAME")).toUpperCase(Locale.ENGLISH)), 12);
                    String title = name + " " + Converter.asNotNullString(rowTBLCCSKILLS.getString("SKILL"));
                    window.setTitle(title);
                    Image image1 = new Image("/image/buttons/ok_unsaved.png");
                    image1.setWidth("15px");
                    image1.setHeight("15px");
                    image1.setVisible(false);
                    UiUtil.addComponent(window, image1);
                    Image image2 = new Image("/image/crm/skype/Error_16x16.png");
                    image2.setWidth("15px");
                    image2.setHeight("15px");
                    image2.setVisible(false);
                    UiUtil.addComponent(window, image2);
                    Image image3 = new Image("/image/crm/skype/Warning_16x16.png");
                    image3.setWidth("15px");
                    image3.setHeight("15px");
                    image3.setVisible(false);
                    UiUtil.addComponent(window, image3);
                    Label label1 = new Label();
                    label1.setVisible(false);
                    UiUtil.addComponent(window, label1);
                    int waitingCalls = 0;
                    int begin = 0;
                    for (int k = 0; k < queueShowLines.size(); k++) {
                        String line = queueShowLines.get(k);
                        if (NullStatus.isNull(line)) {
                            begin++;
                            continue;
                        }
                        if (line.startsWith(rowTBLCCSKILLS.getString("SKILL") + " ")) {
                            waitingCalls = Integer.parseInt(Converter.asNotNullString(line.substring(line.indexOf("has ") + 4, line.indexOf("calls ") - 1)));
                            break;
                        }
                        begin++;
                    }
                    int members = 0;
                    boolean countMembers = false;
                    if (begin < queueShowLines.size()) {
                        for (int k = begin; k < queueShowLines.size(); k++) {
                            String line = queueShowLines.get(k);
                            if (NullStatus.isNull(line)) {
                                break;
                            }
                            if (line.indexOf("Members:") >= 0) {
                                countMembers = true;
                                begin++;
                                continue;
                            }
                            if (line.indexOf("Callers:") >= 0) {
                                break;
                            }
                            if (line.indexOf("No Callers") >= 0) {
                                break;
                            }
                            if (countMembers) {
                                members++;
                            }
                        }
                    }
                    if (members == 0) {
                        image2.setVisible(true);
                    } else {
                        image1.setVisible(true);
                    }
                    if (waitingCalls > 0) {
                        image3.setVisible(true);
                    } else {
                        image3.setVisible(false);
                    }
                    label1.setValue(MessageUtil.getMessage(Monitoring.class, Messages.skillText, "" + waitingCalls, "" + members));
                    label1.setVisible(true);
                    UiUtil.addComponent(target, window);
                    if ((i + 1) % 3 == 0) {
                        break;
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void refreshPanel() {
        refreshPanel(null);
    }

    public static void initPanel() throws Exception {
        if (LicenseManager.isValid("TegsoftCC") <= 0) {
            Tabbox tabBox1 = (Tabbox) UiUtil.findComponent("tabBox1");
            Tab tabAGENTS = (Tab) UiUtil.findComponent("tabAGENTS");
            Tabpanel tabPanelAGENTS = (Tabpanel) UiUtil.findComponent("tabPanelAGENTS");
            Tabpanel tabPanelEXTENSIONS = (Tabpanel) UiUtil.findComponent("tabPanelEXTENSIONS");
            tabAGENTS.setVisible(false);
            tabPanelAGENTS.setVisible(false);
            tabBox1.setSelectedPanel(tabPanelEXTENSIONS);
            ((Groupbox) UiUtil.findComponent("groupSKILLS")).setVisible(false);
            return;
        }
    }

    private static void clearAll(Object parent) throws Exception {
        ((Label) UiUtil.findComponent(parent, "TotalActiveCalls")).setValue("0");
        ((Label) UiUtil.findComponent(parent, "ActiveChannels")).setValue("0");
        ((Progressmeter) UiUtil.findComponent(parent, "LoginAgent")).setValue(0);
        ((Progressmeter) UiUtil.findComponent(parent, "ReadyAgent")).setValue(0);
        ((Progressmeter) UiUtil.findComponent(parent, "OnlinePhones")).setValue(0);
        ((Progressmeter) UiUtil.findComponent(parent, "MemUsage")).setValue(0);
        ((Progressmeter) UiUtil.findComponent(parent, "LoadAvarage")).setValue(0);
        ((Label) UiUtil.findComponent(parent, "MemUsageValue")).setValue("0");
        ((Label) UiUtil.findComponent(parent, "LoadAvarageValue")).setValue("0.0");
        ((Label) UiUtil.findComponent(parent, "OnlinePhonesValue")).setValue("0");
        ((Label) UiUtil.findComponent(parent, "LoginAgentValue")).setValue("0");
        ((Label) UiUtil.findComponent(parent, "ReadyAgentValue")).setValue("0");
    }

    public enum Messages {

        /**
		 * days
		 * 
		 */
        days, /**
		 * hours
		 */
        hours, /**
		 * W:{0} M:{1}
		 */
        skillText, /**
		 * Active Calls:{0}
		 */
        trunkText, /**
		 * {0}
		 */
        readyText, /**
		 * {1} {0}
		 */
        notReadyText
    }
}
