package com.tegsoft.pbx;

import java.io.ObjectOutputStream;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.Enumeration;
import org.apache.log4j.Level;
import org.asteriskjava.fastagi.AgiChannel;
import org.asteriskjava.fastagi.AgiRequest;
import org.asteriskjava.manager.action.CommandAction;
import org.asteriskjava.manager.action.GetVarAction;
import org.asteriskjava.manager.action.SetVarAction;
import org.asteriskjava.manager.event.StatusEvent;
import org.asteriskjava.manager.response.CommandResponse;
import org.asteriskjava.manager.response.GetVarResponse;
import org.asteriskjava.manager.response.ManagerResponse;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import com.tegsoft.cc.CampaignManager;
import com.tegsoft.cc.ContactCenter;
import com.tegsoft.cc.QueueImporter;
import com.tegsoft.pbx.agi.AgiServer;
import com.tegsoft.pbx.agi.IntelligentCallRouting;
import com.tegsoft.tobe.db.command.Command;
import com.tegsoft.tobe.db.connection.Connection;
import com.tegsoft.tobe.db.dataset.DataRow;
import com.tegsoft.tobe.db.dataset.Dataset;
import com.tegsoft.tobe.event.EventType;
import com.tegsoft.tobe.ui.MessageType;
import com.tegsoft.tobe.ui.datasource.DataSource;
import com.tegsoft.tobe.util.Compare;
import com.tegsoft.tobe.util.Converter;
import com.tegsoft.tobe.util.DateUtil;
import com.tegsoft.tobe.util.DecimalUtil;
import com.tegsoft.tobe.util.JobUtil;
import com.tegsoft.tobe.util.NullStatus;
import com.tegsoft.tobe.util.UiUtil;
import com.tegsoft.tobe.util.message.MessageUtil;

public class TegsoftPBX {

    public static String getLocalPBXID() throws Exception {
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
                String PBXID = new Command("SELECT PBXID FROM TBLPBX WHERE UNITUID={UNITUID} AND UPPER(MACADDRESS)=UPPER('" + macKey + "')").executeScalarAsString();
                if (NullStatus.isNotNull(PBXID)) {
                    return PBXID;
                }
            }
        }
        return null;
    }

    public static void init() throws Exception {
        JobUtil.prepareThread();
        Connection.initNonJ2EE();
        boolean autoStartTegsoftPBX = false;
        boolean autoStartTegsoftCC = false;
        boolean autoStartTegsoftCC_Progressive = false;
        CdrImporter cdrImporter = new CdrImporter();
        QueueImporter queueImporter = new QueueImporter();
        RemotePBX remotePBX = new RemotePBX();
        AgiServer agiServer = new AgiServer();
        PhoneConfigurator phoneConfigurator = new PhoneConfigurator();
        QueueStats queueStats = new QueueStats();
        cdrImporter.start();
        queueImporter.start();
        remotePBX.start();
        phoneConfigurator.start();
        agiServer.start();
        autoStartTegsoftPBX = true;
        ManagerEventListener managerEventListener = new ManagerEventListener();
        managerEventListener.start();
        queueStats.start();
        autoStartTegsoftCC = true;
        CampaignManager campaignManager = new CampaignManager();
        campaignManager.start();
        autoStartTegsoftCC_Progressive = true;
        String PBXID = TegsoftPBX.getLocalPBXID();
        if (NullStatus.isNotNull(PBXID)) {
            applyConfig(PBXID);
        }
        while (true) {
            try {
                if (autoStartTegsoftCC) {
                    if (!managerEventListener.isAlive()) {
                        MessageUtil.logMessage(ContactCenter.class, Level.ERROR, "ManagerEventListener is down restarting");
                        managerEventListener = new ManagerEventListener();
                        managerEventListener.start();
                    }
                    if (!queueStats.isAlive()) {
                        MessageUtil.logMessage(ContactCenter.class, Level.ERROR, "QueueStats is down restarting");
                        queueStats = new QueueStats();
                        queueStats.start();
                    }
                }
                if (autoStartTegsoftCC_Progressive) {
                    if (!campaignManager.isAlive()) {
                        MessageUtil.logMessage(ContactCenter.class, Level.ERROR, "CampaignManager is down restarting");
                        campaignManager = new CampaignManager();
                        campaignManager.start();
                    }
                }
                if (autoStartTegsoftPBX) {
                    if (!cdrImporter.isAlive()) {
                        MessageUtil.logMessage(ContactCenter.class, Level.ERROR, "CdrImporter is down restarting");
                        cdrImporter = new CdrImporter();
                        cdrImporter.start();
                    }
                    if (!phoneConfigurator.isAlive()) {
                        MessageUtil.logMessage(ContactCenter.class, Level.ERROR, "PhoneConfigurator is down restarting");
                        phoneConfigurator = new PhoneConfigurator();
                        phoneConfigurator.start();
                    }
                    if (!queueImporter.isAlive()) {
                        MessageUtil.logMessage(ContactCenter.class, Level.ERROR, "QueueImporter is down restarting");
                        queueImporter = new QueueImporter();
                        queueImporter.start();
                    }
                    if (!remotePBX.isAlive()) {
                        MessageUtil.logMessage(ContactCenter.class, Level.ERROR, "RemotePBX is down restarting");
                        remotePBX = new RemotePBX();
                        remotePBX.start();
                    }
                    if (!agiServer.isAlive()) {
                        MessageUtil.logMessage(ContactCenter.class, Level.ERROR, "AgiServer is down restarting");
                        agiServer = new AgiServer();
                        agiServer.start();
                    }
                }
                Thread.sleep(1000 * 60 * 5);
            } catch (Exception ex) {
                MessageUtil.logMessage(ContactCenter.class, Level.FATAL, ex);
            }
        }
    }

    public static ManagerConnection createManagerConnection(String PBXID, String eventMask) throws Exception {
        try {
            String IPADDRESS = "127.0.0.1";
            if (NullStatus.isNotNull(PBXID)) {
                Command command = new Command("SELECT IPADDRESS FROM TBLPBX WHERE 1=1 AND UNITUID={UNITUID}");
                command.append("AND PBXID=");
                command.bind(PBXID);
                IPADDRESS = command.executeScalarAsString();
            }
            ManagerConnection managerConnection = new ManagerConnection();
            managerConnection.setHostname(IPADDRESS);
            managerConnection.setUsername("tobe");
            managerConnection.setPassword("t1be");
            if (NullStatus.isNull(eventMask)) {
                managerConnection.login();
            } else {
                managerConnection.login(eventMask);
            }
            return managerConnection;
        } catch (Exception ex) {
            MessageUtil.logMessage(ContactCenter.class, Level.FATAL, ex);
        }
        return null;
    }

    public static ManagerConnection createManagerConnection(String PBXID) throws Exception {
        return createManagerConnection(PBXID, null);
    }

    public static void applyConfig(Event event) throws Exception {
        String IPADDRESS = Converter.asString(UiUtil.getDataSourceValue("TBLPBX", "IPADDRESS", null, null));
        if (NullStatus.isNotNull(IPADDRESS)) {
            Socket socket = new Socket(IPADDRESS, 11001);
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            oos.writeObject("APPLYCONFIG\r\n");
            oos.flush();
            oos.close();
            socket.close();
            UiUtil.showMessage(MessageType.ERROR, MessageUtil.getMessage(TegsoftPBX.class, Messages.tegsoftPBX_1));
        }
    }

    public static void applyConfig(String PBXID) throws Exception {
        ConfigWriter.applyConfig(PBXID);
    }

    private static void prepareDESTPARAMDataset(Dataset DESTPARAM, String DESTTYPE) throws Exception {
        DESTPARAM.addDataColumn("TYPE");
        DESTPARAM.addDataColumn("CODE");
        DESTPARAM.addDataColumn("VALUE");
        if ("TERM".equals(DESTTYPE)) {
            DataRow dataRow = DESTPARAM.addNewDataRow();
            dataRow.set("TYPE", "TERM");
            dataRow.set("CODE", "CONG");
            dataRow.set("VALUE", MessageUtil.getMessage(TegsoftPBX.class, Messages.tegsoftPBX_2));
            dataRow = DESTPARAM.addNewDataRow();
            dataRow.set("TYPE", "TERM");
            dataRow.set("CODE", "BUSY");
            dataRow.set("VALUE", MessageUtil.getMessage(TegsoftPBX.class, Messages.tegsoftPBX_3));
            dataRow = DESTPARAM.addNewDataRow();
            dataRow.set("TYPE", "TERM");
            dataRow.set("CODE", "HANGUP");
            dataRow.set("VALUE", MessageUtil.getMessage(TegsoftPBX.class, Messages.tegsoftPBX_4));
            return;
        }
        if ("TRSTSRC".equals(DESTTYPE)) {
            DataRow dataRow = DESTPARAM.addNewDataRow();
            dataRow.set("TYPE", "TRSTSRC");
            dataRow.set("CODE", "EXT");
            dataRow.set("VALUE", MessageUtil.getMessage(TegsoftPBX.class, Messages.tegsoftPBX_5));
            dataRow = DESTPARAM.addNewDataRow();
            dataRow.set("TYPE", "TRSTSRC");
            dataRow.set("CODE", "ALLIN");
            dataRow.set("VALUE", MessageUtil.getMessage(TegsoftPBX.class, Messages.tegsoftPBX_6));
            dataRow = DESTPARAM.addNewDataRow();
            dataRow.set("TYPE", "TRSTSRC");
            dataRow.set("CODE", "ALL");
            dataRow.set("VALUE", MessageUtil.getMessage(TegsoftPBX.class, Messages.tegsoftPBX_7));
            return;
        }
        Command command = new Command("SELECT TYPE,CODE,VALUE FROM (");
        command.append("SELECT 'CONF' AS TYPE,CONFID AS CODE, ROOMNAME AS VALUE FROM TBLPBXCONF WHERE UNITUID={UNITUID} UNION ALL ");
        command.append("SELECT 'EXT' AS TYPE,EXTID AS CODE, NVL(EXTEN,' ')||' '||NVL(CALLERID,' ') AS VALUE FROM TBLPBXEXT WHERE UNITUID={UNITUID} UNION ALL ");
        command.append("SELECT 'VMQ' AS TYPE,EXTEN AS CODE, NVL(EXTEN,' ')||' '||NVL(CALLERID,' ')||' '||NVL(MAILBOX,' ') AS VALUE FROM TBLPBXEXT WHERE UNITUID={UNITUID} AND MAILBOX IS NOT NULL UNION ALL ");
        command.append("SELECT 'VM' AS TYPE,EXTEN AS CODE, NVL(EXTEN,' ')||' '||NVL(CALLERID,' ')||' '||NVL(MAILBOX,' ') AS VALUE FROM TBLPBXEXT WHERE UNITUID={UNITUID} AND MAILBOX IS NOT NULL UNION ALL ");
        command.append("SELECT 'VMB' AS TYPE,EXTEN AS CODE, NVL(EXTEN,' ')||' '||NVL(CALLERID,' ')||' '||NVL(MAILBOX,' ') AS VALUE FROM TBLPBXEXT WHERE UNITUID={UNITUID} AND MAILBOX IS NOT NULL UNION ALL ");
        command.append("SELECT 'VMU' AS TYPE,EXTEN AS CODE, NVL(EXTEN,' ')||' '||NVL(CALLERID,' ')||' '||NVL(MAILBOX,' ') AS VALUE FROM TBLPBXEXT WHERE UNITUID={UNITUID} AND MAILBOX IS NOT NULL UNION ALL ");
        command.append("SELECT 'FAX' AS TYPE,FAXID AS CODE, NVL(EXTEN,' ')||' '||NVL(FAXNAME,' ')||' '||NVL(EMAIL,' ') AS VALUE FROM TBLPBXFAX WHERE UNITUID={UNITUID} UNION ALL ");
        command.append("SELECT 'IVR' AS TYPE,IVRID AS CODE, IVRNAME AS VALUE FROM TBLPBXIVR WHERE UNITUID={UNITUID} UNION ALL ");
        command.append("SELECT 'QUEUE' AS TYPE,SKILL AS CODE, NAME AS VALUE FROM TBLCCSKILLS WHERE UNITUID={UNITUID} UNION ALL ");
        command.append("SELECT 'SHRT' AS TYPE,NUMID AS CODE,NVL(EXTEN,' ')||' '||NVL(DESTNUMBER,' ')||' '||NVL(NOTES,' ') AS VALUE FROM TBLPBXSHORTNUM WHERE UNITUID={UNITUID} UNION ALL ");
        command.append("SELECT 'RINGGRP' AS TYPE,GROUPID AS CODE, NAME AS VALUE FROM TBLPBXGRP WHERE UNITUID={UNITUID} UNION ALL ");
        command.append("SELECT 'TRUNK' AS TYPE,TRUNKID AS CODE, TRUNKNAME AS VALUE FROM TBLPBXTRUNK WHERE UNITUID={UNITUID}) ");
        command.append("WHERE TYPE=");
        command.bind(DESTTYPE);
        command.append("ORDER BY TYPE,VALUE");
        DESTPARAM.fill(command);
    }

    private static EventListener DESTPARAMEventListener = new EventListener() {

        public void onEvent(Event event) throws Exception {
            Combobox component = (Combobox) event.getTarget();
            if (component.getId().startsWith("IVR_")) {
                int rowIndex = Integer.parseInt(component.getId().substring(13));
                Dataset DESTPARAM = new Dataset("DESTPARAM");
                prepareDESTPARAMDataset(DESTPARAM, (String) UiUtil.getDataSourceValueAt("TBLPBXIVROPT", "DESTTYPE", rowIndex, "", ""));
                if (component.getSelectedIndex() > 0) {
                    UiUtil.getDataSource("TBLPBXIVROPT").setValueAt("DESTPARAM", rowIndex, DESTPARAM.getRow(component.getSelectedIndex() - 1).getString("CODE"));
                } else {
                    UiUtil.getDataSource("TBLPBXIVROPT").setValueAt("DESTPARAM", rowIndex, null);
                }
            } else if (component.getId().startsWith("INV")) {
                Dataset DESTPARAM = new Dataset("DESTPARAM");
                prepareDESTPARAMDataset(DESTPARAM, (String) UiUtil.getDataSourceValue("TBLPBXIVR", "INVDESTTYPE", "", ""));
                if (component.getSelectedIndex() > 0) {
                    UiUtil.getDataSource("TBLPBXIVR").setValue("INVDESTPARAM", DESTPARAM.getRow(component.getSelectedIndex() - 1).getString("CODE"));
                } else {
                    UiUtil.getDataSource("TBLPBXIVR").setValue("INVDESTPARAM", null);
                }
            } else if (component.getId().startsWith("OPDESTPARAM")) {
                Dataset DESTPARAM = new Dataset("DESTPARAM");
                prepareDESTPARAMDataset(DESTPARAM, (String) UiUtil.getDataSourceValue("TBLPBX", "DESTTYPE", "", ""));
                if (component.getSelectedIndex() > 0) {
                    UiUtil.getDataSource("TBLPBX").setValue("DESTPARAM", DESTPARAM.getRow(component.getSelectedIndex() - 1).getString("CODE"));
                } else {
                    UiUtil.getDataSource("TBLPBX").setValue("DESTPARAM", null);
                }
            } else if (component.getId().startsWith("TO")) {
                Dataset DESTPARAM = new Dataset("DESTPARAM");
                prepareDESTPARAMDataset(DESTPARAM, (String) UiUtil.getDataSourceValue("TBLPBXIVR", "TODESTTYPE", "", ""));
                if (component.getSelectedIndex() > 0) {
                    UiUtil.getDataSource("TBLPBXIVR").setValue("TODESTPARAM", DESTPARAM.getRow(component.getSelectedIndex() - 1).getString("CODE"));
                } else {
                    UiUtil.getDataSource("TBLPBXIVR").setValue("TODESTPARAM", null);
                }
            } else if (component.getId().startsWith("CUST_")) {
                int rowIndex = Integer.parseInt(component.getId().substring(14));
                Dataset DESTPARAM = new Dataset("DESTPARAM");
                prepareDESTPARAMDataset(DESTPARAM, (String) UiUtil.getDataSourceValueAt("TBLPBXCUSTDEST", "DESTTYPE", rowIndex, "", ""));
                if (component.getSelectedIndex() > 0) {
                    UiUtil.getDataSource("TBLPBXCUSTDEST").setValueAt("DESTPARAM", rowIndex, DESTPARAM.getRow(component.getSelectedIndex() - 1).getString("CODE"));
                } else {
                    UiUtil.getDataSource("TBLPBXCUSTDEST").setValueAt("DESTPARAM", rowIndex, null);
                }
            } else if (component.getId().startsWith("GRPMEM")) {
                int rowIndex = Integer.parseInt(component.getId().substring(16));
                Dataset DESTPARAM = new Dataset("DESTPARAM");
                prepareDESTPARAMDataset(DESTPARAM, (String) UiUtil.getDataSourceValueAt("TBLPBXGRPMEM", "DESTTYPE", rowIndex, "", ""));
                if (component.getSelectedIndex() > 0) {
                    UiUtil.getDataSource("TBLPBXGRPMEM").setValueAt("DESTPARAM", rowIndex, DESTPARAM.getRow(component.getSelectedIndex() - 1).getString("CODE"));
                } else {
                    UiUtil.getDataSource("TBLPBXGRPMEM").setValueAt("DESTPARAM", rowIndex, null);
                }
            } else if (component.getId().startsWith("GRP")) {
                Dataset DESTPARAM = new Dataset("DESTPARAM");
                prepareDESTPARAMDataset(DESTPARAM, (String) UiUtil.getDataSourceValue("TBLPBXGRP", "DESTTYPE", "", ""));
                if (component.getSelectedIndex() > 0) {
                    UiUtil.getDataSource("TBLPBXGRP").setValue("DESTPARAM", DESTPARAM.getRow(component.getSelectedIndex() - 1).getString("CODE"));
                } else {
                    UiUtil.getDataSource("TBLPBXGRP").setValue("DESTPARAM", null);
                }
            } else if (component.getId().startsWith("SKILL_")) {
                Dataset DESTPARAM = new Dataset("DESTPARAM");
                prepareDESTPARAMDataset(DESTPARAM, (String) UiUtil.getDataSourceValue("TBLCCSKILLS", "DESTTYPE", "", ""));
                if (component.getSelectedIndex() > 0) {
                    UiUtil.getDataSource("TBLCCSKILLS").setValue("DESTPARAM", DESTPARAM.getRow(component.getSelectedIndex() - 1).getString("CODE"));
                } else {
                    UiUtil.getDataSource("TBLCCSKILLS").setValue("DESTPARAM", null);
                }
            } else {
                Dataset DESTPARAM = new Dataset("DESTPARAM");
                prepareDESTPARAMDataset(DESTPARAM, (String) UiUtil.getDataSourceValue("TBLPBXINROUTE", "DESTTYPE", "", ""));
                if (component.getSelectedIndex() > 0) {
                    UiUtil.getDataSource("TBLPBXINROUTE").setValue("DESTPARAM", DESTPARAM.getRow(component.getSelectedIndex() - 1).getString("CODE"));
                } else {
                    UiUtil.getDataSource("TBLPBXINROUTE").setValue("DESTPARAM", null);
                }
            }
        }

        ;
    };

    private static void prepareDestinations(String componentId, DataSource dataSource, int dataRowIndex) throws Exception {
        Combobox component = (Combobox) UiUtil.findComponent(componentId + dataRowIndex);
        Dataset DESTPARAM = new Dataset("DESTPARAM");
        prepareDESTPARAMDataset(DESTPARAM, (String) dataSource.getValueAt("DESTTYPE", dataRowIndex));
        component.getItems().clear();
        Comboitem item = new Comboitem();
        UiUtil.addComponent(component, item);
        item.setLabel("---");
        component.setText("");
        for (int i = 0; i < DESTPARAM.getRowCount(); i++) {
            item = new Comboitem();
            UiUtil.addComponent(component, item);
            item.setLabel(DESTPARAM.getRow(i).getString("VALUE"));
            if (Compare.equal(DESTPARAM.getRow(i).getString("CODE"), dataSource.getValueAt("DESTPARAM", dataRowIndex))) {
                component.setSelectedItem(item);
            }
        }
        component.addEventListener(Events.ON_CHANGE, DESTPARAMEventListener);
    }

    private static void prepareDestinations(String componentId, String columnNameDESTTYPE, String columnNameDESTPARAM, DataSource dataSource) throws Exception {
        Combobox component = (Combobox) UiUtil.findComponent(componentId);
        if (component == null) {
            throw new Exception("Unable to locate component ->" + componentId + " dataSource->" + dataSource.getName());
        }
        Dataset DESTPARAM = new Dataset("DESTPARAM");
        prepareDESTPARAMDataset(DESTPARAM, (String) dataSource.getValue(columnNameDESTTYPE));
        component.getItems().clear();
        Comboitem item = new Comboitem();
        UiUtil.addComponent(component, item);
        item.setLabel("---");
        component.setText("");
        for (int i = 0; i < DESTPARAM.getRowCount(); i++) {
            item = new Comboitem();
            UiUtil.addComponent(component, item);
            item.setLabel(DESTPARAM.getRow(i).getString("VALUE"));
            if (Compare.equal(DESTPARAM.getRow(i).getString("CODE"), dataSource.getValue(columnNameDESTPARAM))) {
                component.setSelectedItem(item);
            }
        }
        component.addEventListener(Events.ON_CHANGE, DESTPARAMEventListener);
    }

    public static void initPrepareDestinations(final String dataSourceName, final String componentId, final String columnNameDESTTYPE, final String columnNameDESTPARAM) throws Exception {
        DataSource dataSource = UiUtil.getDataSource(dataSourceName);
        dataSource.addListener(EventType.all, new com.tegsoft.tobe.event.EventListener() {

            @Override
            public void onEvent(com.tegsoft.tobe.event.Event event) throws Exception {
                DataSource dataSource = UiUtil.getDataSource(dataSourceName);
                prepareDestinations(componentId, columnNameDESTTYPE, columnNameDESTPARAM, dataSource);
            }

            @Override
            public Object getTarget() {
                return null;
            }
        });
    }

    public static void initPrepareDestinationsMulti(final String dataSourceName, final String componentId) throws Exception {
        DataSource dataSource = UiUtil.getDataSource(dataSourceName);
        dataSource.addListener(EventType.all, new com.tegsoft.tobe.event.EventListener() {

            @Override
            public void onEvent(com.tegsoft.tobe.event.Event event) throws Exception {
                DataSource dataSource = UiUtil.getDataSource(dataSourceName);
                for (int i = 0; i < dataSource.getRowCount(); i++) {
                    prepareDestinations(componentId, dataSource, i);
                }
            }

            @Override
            public Object getTarget() {
                return null;
            }
        });
    }

    public static void playBackground(AgiChannel channel, String announce) throws Exception {
        if (NullStatus.isNotNull(channel.getVariable("CACHED_DTMF"))) {
            return;
        }
        if (NullStatus.isNotNull(announce)) {
            String announces[] = announce.split(",");
            for (int i = 0; i < announces.length; i++) {
                if (NullStatus.isNotNull(announces[i])) {
                    char dtmfchar = channel.streamFile(announces[i], "0123456789#*");
                    if (dtmfchar != 0x0) {
                        channel.setVariable("CACHED_DTMF", "" + dtmfchar);
                    }
                }
            }
        }
    }

    public static void playBack(AgiChannel channel, String announce) throws Exception {
        channel.setVariable("CACHED_DTMF", "");
        if (NullStatus.isNotNull(announce)) {
            String announces[] = announce.split(",");
            for (int i = 0; i < announces.length; i++) {
                if (NullStatus.isNotNull(announces[i])) {
                    channel.streamFile(announces[i]);
                }
            }
        }
    }

    private static String prepareAnnounceX(String prefix, String digits, boolean sayOne, boolean sayZero) {
        if (NullStatus.isNull(digits)) {
            return "";
        }
        if (!DecimalUtil.isNumeric(digits)) {
            return "";
        }
        String digit = digits.substring(0, 1);
        if (Integer.parseInt(digit) == 0) {
            if (sayZero) {
                return "digits/" + prefix + digit;
            }
            return "";
        }
        if (Integer.parseInt(digit) == 1) {
            if (sayOne) {
                return "digits/" + prefix + digit;
            }
            return "";
        }
        return "digits/" + prefix + digit;
    }

    private static String prepareAnnounceXX(String prefix, String digits) {
        if (NullStatus.isNull(digits)) {
            return "";
        }
        if (!DecimalUtil.isNumeric(digits)) {
            return "";
        }
        if (digits.length() == 2) {
            String digit = digits.substring(0, 1);
            if (Integer.parseInt(digit) > 0) {
                return "digits/" + prefix + digit + "0," + prepareAnnounceX(prefix, digits.substring(1), true, false);
            } else {
                return prepareAnnounceX(prefix, digits.substring(1), true, false);
            }
        }
        return "";
    }

    private static String prepareAnnounceXXX(String prefix, String digits) {
        if (NullStatus.isNull(digits)) {
            return "";
        }
        if (!DecimalUtil.isNumeric(digits)) {
            return "";
        }
        if (digits.length() == 3) {
            String digit = digits.substring(0, 1);
            if (Integer.parseInt(digit) > 1) {
                return "digits/" + prefix + digit + ",digits/" + prefix + "hundred," + prepareAnnounceXX(prefix, digits.substring(1));
            } else if (Integer.parseInt(digit) > 0) {
                return "digits/" + prefix + "hundred," + prepareAnnounceXX(prefix, digits.substring(1));
            } else if (Integer.parseInt(digit) == 0) {
                return prepareAnnounceXX(prefix, digits.substring(1));
            }
        }
        return "";
    }

    private static String prepareAnnounceList(String prefix, String digits) {
        if (NullStatus.isNull(digits)) {
            return "";
        }
        if (!DecimalUtil.isNumeric(digits)) {
            return "";
        }
        digits = "" + Integer.parseInt(digits);
        switch(digits.length()) {
            case 1:
                return prepareAnnounceX(prefix, digits, true, true);
            case 2:
                return prepareAnnounceXX(prefix, digits);
            case 3:
                return prepareAnnounceXXX(prefix, digits);
            case 4:
                String digit = digits.substring(0, 1);
                if (Integer.parseInt(digit) > 1) {
                    return "digits/" + prefix + digit + ",digits/" + prefix + "thousand," + prepareAnnounceXXX(prefix, digits.substring(1));
                } else if (Integer.parseInt(digit) > 0) {
                    return "digits/" + prefix + "thousand," + prepareAnnounceXXX(prefix, digits.substring(1));
                }
                return "";
            case 5:
                return prepareAnnounceXX(prefix, digits.substring(0, 2)) + ",digits/" + prefix + "thousand," + prepareAnnounceXXX(prefix, digits.substring(2));
            case 6:
                return prepareAnnounceXXX(prefix, digits.substring(0, 3)) + ",digits/" + prefix + "thousand," + prepareAnnounceXXX(prefix, digits.substring(3));
            case 7:
                String result = "";
                digit = digits.substring(0, 1);
                if (Integer.parseInt(digit) > 1) {
                    result = "digits/" + prefix + digit + ",digits/" + prefix + "million,";
                } else if (Integer.parseInt(digit) > 0) {
                    result = "digits/" + prefix + "million,";
                }
                if (Integer.parseInt(digits.substring(1, 4)) > 0) {
                    digits = digits.substring(1);
                    return result + prepareAnnounceXXX(prefix, digits.substring(0, 3)) + ",digits/" + prefix + "thousand," + prepareAnnounceXXX(prefix, digits.substring(3));
                } else {
                    digits = digits.substring(1);
                    return result + "," + prepareAnnounceXXX(prefix, digits.substring(3));
                }
            case 8:
                result = prepareAnnounceXX(prefix, digits.substring(0, 2)) + ",digits/" + prefix + "million,";
                if (Integer.parseInt(digits.substring(2, 5)) > 0) {
                    digits = digits.substring(2);
                    return result + prepareAnnounceXXX(prefix, digits.substring(0, 3)) + ",digits/" + prefix + "thousand," + prepareAnnounceXXX(prefix, digits.substring(3));
                } else {
                    digits = digits.substring(2);
                    return result + "," + prepareAnnounceXXX(prefix, digits.substring(3));
                }
            case 9:
                result = prepareAnnounceXXX(prefix, digits.substring(0, 3)) + ",digits/" + prefix + "million,";
                if (Integer.parseInt(digits.substring(3, 6)) > 0) {
                    digits = digits.substring(3);
                    return result + prepareAnnounceXXX(prefix, digits.substring(0, 3)) + ",digits/" + prefix + "thousand," + prepareAnnounceXXX(prefix, digits.substring(3));
                } else {
                    digits = digits.substring(2);
                    return result + "," + prepareAnnounceXXX(prefix, digits.substring(3));
                }
            default:
                return "";
        }
    }

    public static void sayNumber(AgiChannel channel, String number) throws Exception {
        String part1 = number;
        String part2 = null;
        String prefix = channel.getVariable("tegsoftDIGITPREFIX");
        if (prefix == null) {
            prefix = "";
        }
        if (number.indexOf(".") > 0) {
            part1 = number.substring(0, number.indexOf("."));
            part2 = number.substring(number.indexOf(".") + 1);
        }
        String announceList = prepareAnnounceList(prefix, part1);
        if (NullStatus.isNotNull(part2)) {
            announceList += "," + "digits/" + prefix + "and" + "," + prepareAnnounceList(prefix, part2);
        }
        playBackground(channel, announceList);
    }

    public static void sayMoney(AgiChannel channel, String number, String dolars, String cents, String dolarsOnly) throws Exception {
        String part1 = number;
        String part2 = null;
        String prefix = channel.getVariable("tegsoftDIGITPREFIX");
        if (prefix == null) {
            prefix = "";
        }
        if (number.indexOf(".") > 0) {
            part1 = number.substring(0, number.indexOf("."));
            part2 = number.substring(number.indexOf(".") + 1);
        }
        String announceList = prepareAnnounceList(prefix, part1);
        if (NullStatus.isNotNull(part2)) {
            announceList += "," + dolars + "," + prepareAnnounceList(prefix, part2) + "," + cents;
        } else {
            announceList += "," + dolarsOnly;
        }
        playBackground(channel, announceList);
    }

    public static void sayDigits(AgiChannel channel, String digits) throws Exception {
        if (NullStatus.isNull(digits)) {
            return;
        }
        if (!DecimalUtil.isNumeric(digits)) {
            return;
        }
        String prefix = channel.getVariable("tegsoftDIGITPREFIX");
        if (prefix == null) {
            prefix = "";
        }
        String announceList = "";
        for (int i = 0; i < digits.length(); i++) {
            announceList += "digits/" + prefix + digits.charAt(i) + ",";
        }
        playBackground(channel, announceList);
    }

    public static void sayDate(AgiChannel channel, String day, String month, String year) throws Exception {
        String prefix = channel.getVariable("tegsoftDIGITPREFIX");
        if (prefix == null) {
            prefix = "";
        }
        sayNumber(channel, "" + Integer.parseInt(day));
        playBackground(channel, "digits/" + prefix + "mon-" + (Integer.parseInt(month) - 1));
        sayNumber(channel, "" + Integer.parseInt(year));
    }

    public static String getChannelVariable(ManagerConnection managerConnection, StatusEvent statusEvent, String varName) throws Exception {
        if (statusEvent == null) {
            return null;
        }
        if (varName == null) {
            return null;
        }
        String varValue = null;
        ManagerResponse managerResponse = managerConnection.sendAction(new GetVarAction(statusEvent.getChannel(), varName), 10000);
        if (managerResponse instanceof GetVarResponse) {
            GetVarResponse getVarResponse = (GetVarResponse) managerResponse;
            varValue = getVarResponse.getValue();
            if (NullStatus.isNull(varValue)) {
                if (NullStatus.isNotNull(statusEvent.getBridgedChannel())) {
                    managerResponse = managerConnection.sendAction(new GetVarAction(statusEvent.getBridgedChannel(), varName), 10000);
                    if (managerResponse instanceof GetVarResponse) {
                        getVarResponse = (GetVarResponse) managerResponse;
                        varValue = getVarResponse.getValue();
                    }
                }
            }
        }
        return varValue;
    }

    public static void setChannelVariable(ManagerConnection managerConnection, StatusEvent statusEvent, String varName, String value) throws Exception {
        if (statusEvent == null) {
            return;
        }
        if (varName == null) {
            return;
        }
        managerConnection.sendAction(new SetVarAction(statusEvent.getChannel(), varName, value), 10000);
    }

    public static void logMessage(AgiChannel channel, Level level, String message) throws Exception {
        String logLEVEL = channel.getVariable("tegsoftLOGLEVEL");
        Level currentLevel = Level.ERROR;
        if (NullStatus.isNull(logLEVEL)) {
            channel.setVariable("tegsoftLOGLEVEL", logLEVEL);
        } else {
            currentLevel = Level.toLevel(logLEVEL);
        }
        if (currentLevel.isGreaterOrEqual(currentLevel)) {
            channel.exec("NoOp", DateUtil.now() + " " + channel.getName() + " " + channel.getUniqueId() + " " + message);
        }
    }

    public static void setVariable(AgiChannel channel, String name, String value) throws Exception {
        logMessage(channel, Level.DEBUG, "Setting variable " + name + " as " + value);
        channel.setVariable(name, value);
    }

    public static String getVariable(AgiChannel channel, String name) throws Exception {
        String value = channel.getVariable(name);
        logMessage(channel, Level.DEBUG, "Reading variable " + name + " as " + value);
        return value;
    }

    public static void setLogLevel(AgiChannel channel, Level level) throws Exception {
        channel.setVariable("tegsoftLOGLEVEL", level.toString());
    }

    public static String readInput(AgiChannel channel, String announce, boolean interruptible, int initial_timeout, int interdigit_timeout, int maxdigits) throws Exception {
        logMessage(channel, Level.DEBUG, "Getting data for " + announce);
        String dtmf = "";
        if (NullStatus.isNotNull(announce)) {
            String announces[] = announce.split(",");
            for (int i = 0; i < announces.length; i++) {
                if (interruptible) {
                    char dtmfchar = 0x0;
                    if (NullStatus.isNotNull(channel.getVariable("CACHED_DTMF"))) {
                        dtmfchar = channel.getVariable("CACHED_DTMF").charAt(0);
                        channel.setVariable("CACHED_DTMF", "");
                    } else {
                        dtmfchar = channel.streamFile(announces[i], "0123456789#*");
                    }
                    if (dtmfchar != 0x0) {
                        if (dtmfchar == '#') {
                            logMessage(channel, Level.DEBUG, "Returning data for " + announce + " " + dtmf);
                            return dtmf;
                        }
                        dtmf += dtmfchar;
                        if (maxdigits > 0) {
                            if (dtmf.length() >= maxdigits) {
                                logMessage(channel, Level.DEBUG, "Returning data for " + announce + " " + dtmf);
                                return dtmf;
                            }
                        }
                        dtmf = getDigits(channel, dtmf, interdigit_timeout, interdigit_timeout, maxdigits);
                        logMessage(channel, Level.DEBUG, "Returning data for " + announce + " " + dtmf);
                        return dtmf;
                    }
                } else {
                    channel.setVariable("CACHED_DTMF", "");
                    channel.streamFile(announces[i]);
                }
            }
        }
        dtmf = getDigits(channel, dtmf, initial_timeout, interdigit_timeout, maxdigits);
        logMessage(channel, Level.DEBUG, "Returning data for " + announce + " " + dtmf);
        return dtmf;
    }

    private static String getDigits(AgiChannel channel, String dtmf, int initial_timeout, int interdigit_timeout, int maxdigits) throws Exception {
        char dtmfchar = channel.waitForDigit(initial_timeout * 1000);
        if (dtmfchar == 0x0) {
            return dtmf;
        }
        if (dtmfchar == '#') {
            return dtmf;
        }
        dtmf += dtmfchar;
        if (maxdigits > 0) {
            if (dtmf.length() >= maxdigits) {
                return dtmf;
            }
        }
        return getDigits(channel, dtmf, interdigit_timeout, interdigit_timeout, maxdigits);
    }

    public static boolean transfer(String extension, IntelligentCallRouting activeICR, AgiRequest request, AgiChannel channel) throws Exception {
        return activeICR.checkIN(extension, request, channel);
    }

    public static boolean isExtensionBusy(String PBXID, String EXTEN) throws Exception {
        boolean busy = false;
        final ManagerConnection managerConnection = createManagerConnection(PBXID);
        CommandAction commandAction = new CommandAction("sip show inuse");
        CommandResponse response = (CommandResponse) managerConnection.sendAction(commandAction, 10000);
        for (String line : response.getResult()) {
            if (line.startsWith(EXTEN + " ")) {
                if (line.length() > 26) {
                    if (line.charAt(26) != '0') {
                        busy = true;
                    }
                }
                break;
            }
        }
        managerConnection.disconnect();
        return busy;
    }

    public enum Messages {

        /**
		 * Configuration applyed
		 * 
		 */
        tegsoftPBX_1, /**
		 * Congestion
		 * 
		 */
        tegsoftPBX_2, /**
		 * Busy
		 * 
		 */
        tegsoftPBX_3, /**
		 * Hangup
		 * 
		 */
        tegsoftPBX_4, /**
		 * Extensions
		 * 
		 */
        tegsoftPBX_5, /**
		 * All Internal Destinations
		 * 
		 */
        tegsoftPBX_6, /**
		 * All Destinations
		 * 
		 */
        tegsoftPBX_7
    }
}
