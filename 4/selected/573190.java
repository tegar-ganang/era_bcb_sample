package com.tegsoft.pbx;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.commons.io.IOUtils;
import org.asteriskjava.manager.action.CommandAction;
import com.tegsoft.tobe.db.command.Command;
import com.tegsoft.tobe.db.dataset.DataRow;
import com.tegsoft.tobe.db.dataset.Dataset;
import com.tegsoft.tobe.util.Compare;
import com.tegsoft.tobe.util.FileUtil;
import com.tegsoft.tobe.util.NullStatus;
import com.tegsoft.tobe.util.StringUtil;

public class ConfigWriter {

    private static final String configBase = "/etc/asterisk/";

    public static void applyConfig(String PBXID) throws Exception {
        Dataset TBLPBX = new Dataset("TBLPBX", "TBLPBX");
        Command command = new Command("SELECT * FROM TBLPBX WHERE 1=1 AND UNITUID={UNITUID}");
        command.append("AND PBXID=");
        command.bind(PBXID);
        TBLPBX.fill(command);
        if (TBLPBX.getRowCount() == 0) {
            return;
        }
        exportConfigResources();
        DataRow rowTBLPBX = TBLPBX.getRow(0);
        writeTBLPBX(PBXID, rowTBLPBX.getString("PROVINCEID"));
        writeREGISTER();
        writeTBLPBXTRUNK();
        writeCONTEXT(rowTBLPBX);
        writeTBLPBXEXT(PBXID);
        writeTBLPBXVM(PBXID);
        writeTBLPBXCONF(PBXID);
        writeTBLPBXFILES();
        writeTBLPBXMOHFILES();
        writeTBLCCSKILLS(PBXID);
        writeFEATURES(rowTBLPBX);
        writeFOP(PBXID);
        extractSOUNDFILES();
        performFWOperations(rowTBLPBX);
        writeFAXCONFIG();
        writeSIPGENERAL(rowTBLPBX);
        writeLOGROTATE();
        FileUtil.deleteAllFilesInDir(new File("/etc/iaxmodem/"));
        FileUtil.deleteMatchingFilesInDir(new File("/var/spool/hylafax/etc/"), "config.", null);
        writeTBLPBXFAX(PBXID);
        ManagerConnection managerConnection = TegsoftPBX.createManagerConnection(PBXID);
        managerConnection.sendAction(new CommandAction("sip reload"), 5000);
        managerConnection.sendAction(new CommandAction("dialplan reload"), 5000);
        managerConnection.sendAction(new CommandAction("module reload app_queue.so"), 5000);
        managerConnection.sendAction(new CommandAction("iax2 reload"), 5000);
        managerConnection.sendAction(new CommandAction("voicemail reload"), 5000);
        managerConnection.sendAction(new CommandAction("logger reload"), 5000);
        managerConnection.sendAction(new CommandAction("moh reload"), 5000);
        managerConnection.sendAction(new CommandAction("features reload"), 5000);
        Runtime.getRuntime().exec("/bin/bash -c 'service iaxmodem restart'");
        Runtime.getRuntime().exec("/bin/bash -c 'service hylafax restart'");
        Runtime.getRuntime().exec("/bin/bash -c 'telinit q'");
        Runtime.getRuntime().exec("/bin/bash -c '/bin/chmod +x /root/tegsoft_restartSystem.sh'");
        Runtime.getRuntime().exec("/bin/bash -c '/bin/chmod +x /root/tegsoft_icr.sh'");
        Runtime.getRuntime().exec("/bin/bash -c '/bin/chmod +x /root/tegsoft_BackupDB.sh'");
        Runtime.getRuntime().exec("/bin/bash -c '/bin/chmod +x /root/tegsoft_BackupCDR.sh'");
        Runtime.getRuntime().exec("/bin/bash -c '/bin/chmod +x /root/tegsoft_mp3.sh'");
        Runtime.getRuntime().exec("/bin/bash -c '/bin/chmod +x /root/tegsoft_fixdate.sh'");
        Runtime.getRuntime().exec("/bin/bash -c '/bin/chmod +x /root/tegsoft_resetQueueStats.sh'");
        managerConnection.disconnect();
    }

    private static void performFWOperations(DataRow rowTBLPBX) throws Exception {
        if (!Compare.isTrue(rowTBLPBX.getString("FWENABLED"))) {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            if (classLoader == null) {
                classLoader = ClassLoader.getSystemClassLoader();
            }
            exportConfigResource(classLoader, "com/tegsoft/pbx/conf/root/tegsoft_iptables_disable.sh", "/root/" + "tegsoft_iptables_disable.sh");
            Runtime.getRuntime().exec("/bin/bash -c 'chmod +x /root/tegsoft_iptables_disable.sh'");
            Runtime.getRuntime().exec("/bin/bash -c '/root/tegsoft_iptables_disable.sh'");
            return;
        }
        Dataset TBLPBXFWIPSET = new Dataset("TBLPBXFWIPSET", "TBLPBXFWIPSET");
        Command command = new Command("SELECT * FROM TBLPBXFWIPSET WHERE 1=1 AND UNITUID={UNITUID}");
        command.append("AND PBXID=");
        command.bind(rowTBLPBX.getString("PBXID"));
        command.append("ORDER BY IPADDRESS");
        TBLPBXFWIPSET.fill(command);
        PrintWriter configFile = new PrintWriter(new FileWriter("/root/tegsoft_iptables.sh", false));
        configFile.println("iptables -F INPUT");
        configFile.println("iptables -P INPUT DROP");
        configFile.println("iptables -P OUTPUT ACCEPT");
        configFile.println("iptables -P FORWARD ACCEPT");
        configFile.println("iptables -A INPUT -i lo -j ACCEPT");
        configFile.println("iptables -A INPUT -s 127.0.0.1 -j ACCEPT");
        configFile.println("iptables -A INPUT -s 88.250.121.71 -j ACCEPT");
        configFile.println("iptables -A INPUT -s 95.128.57.45 -j ACCEPT");
        configFile.println("iptables -A INPUT -s 94.73.159.162 -j ACCEPT");
        for (int i = 0; i < TBLPBXFWIPSET.getRowCount(); i++) {
            DataRow rowTBLPBXFWIPSET = TBLPBXFWIPSET.getRow(i);
            configFile.println("iptables -A INPUT -s " + rowTBLPBXFWIPSET.getString("IPADDRESS") + " -j ACCEPT");
        }
        if (Compare.isTrue(rowTBLPBX.getString("FWPUBFTP"))) {
            configFile.println("iptables -A INPUT -p tcp --dport 20 -j ACCEPT");
            configFile.println("iptables -A INPUT -p tcp --dport 21 -j ACCEPT");
        }
        if (Compare.isTrue(rowTBLPBX.getString("FWPUBSSH"))) {
            configFile.println("iptables -A INPUT -p tcp --dport 2222 -j ACCEPT");
        }
        if (Compare.isTrue(rowTBLPBX.getString("FWPUBDNS"))) {
            configFile.println("iptables -A INPUT -p udp --dport 53 -j ACCEPT");
        }
        if (Compare.isTrue(rowTBLPBX.getString("FWPUBHTTP"))) {
            configFile.println("iptables -A INPUT -p tcp --dport 80 -j ACCEPT");
            configFile.println("iptables -A INPUT -p tcp --dport 4447 -j ACCEPT");
        }
        if (Compare.isTrue(rowTBLPBX.getString("FWPUBDB"))) {
            configFile.println("iptables -A INPUT -p tcp --dport 50000 -j ACCEPT");
        }
        configFile.println("iptables -A INPUT -m state --state ESTABLISHED,RELATED -j ACCEPT");
        configFile.println("iptables-save > /etc/sysconfig/iptables");
        configFile.println("service iptables restart");
        configFile.println();
        configFile.println();
        configFile.close();
        Runtime.getRuntime().exec("/bin/bash -c 'chmod +x /root/tegsoft_iptables.sh'");
        Runtime.getRuntime().exec("/bin/bash -c '/root/tegsoft_iptables.sh'");
    }

    private static void writeTBLPBXCONF(String PBXID) throws Exception {
        Dataset TBLPBXCONF = new Dataset("TBLPBXCONF", "TBLPBXCONF");
        Command command = new Command("SELECT * FROM TBLPBXCONF WHERE 1=1 AND UNITUID={UNITUID}");
        command.append("AND PBXID=");
        command.bind(PBXID);
        command.append("ORDER BY EXTEN");
        TBLPBXCONF.fill(command);
        PrintWriter configFile = new PrintWriter(new FileWriter(configBase + "tegsoft_TBLPBXCONF.conf", false));
        for (int i = 0; i < TBLPBXCONF.getRowCount(); i++) {
            DataRow rowTBLPBXCONF = TBLPBXCONF.getRow(i);
            String conf = "conf => " + rowTBLPBXCONF.getString("EXTEN");
            if (NullStatus.isNotNull(rowTBLPBXCONF.getString("ENTERPIN"))) {
                conf += "," + rowTBLPBXCONF.getString("ENTERPIN");
                if (NullStatus.isNotNull(rowTBLPBXCONF.getString("ADMINPIN"))) {
                    conf += "," + rowTBLPBXCONF.getString("ADMINPIN");
                }
            }
            configFile.println(conf);
        }
        configFile.println();
        configFile.println();
        configFile.close();
    }

    private static void writeFAXCONFIG() throws Exception {
        PrintWriter configFile = new PrintWriter(new FileWriter(configBase + "tegsoft_FAXDETECT.conf", false));
        if (new Command("SELECT COUNT(*) FROM TBLPBXINROUTE WHERE BLOCKNOCLID='true' AND UNITUID={UNITUID}").executeScalarAsDecimal().intValue() > 0) {
            configFile.println("faxdetect=yes");
            configFile.println();
        } else {
            configFile.println("faxdetect=no");
            configFile.println();
        }
        configFile.println();
        configFile.println();
        configFile.close();
    }

    private static void writeSIPGENERAL(DataRow rowTBLPBX) throws Exception {
        PrintWriter configFile = new PrintWriter(new FileWriter(configBase + "tegsoft_SIPGENERAL.conf", false));
        configFile.println("Language=" + rowTBLPBX.getString("LANGUAGE"));
        if (Compare.isTrue(rowTBLPBX.getString("TCPENABLE"))) {
            configFile.println("tcpenable=yes");
            configFile.println("tcpbindaddr=0.0.0.0");
            configFile.println("transport=udp,tcp");
        } else {
            configFile.println("tcpenable=no");
            configFile.println("transport=udp");
        }
        if (Compare.isTrue(rowTBLPBX.getString("CLIPENABLE"))) {
            configFile.println("sendrpid=yes");
        } else {
            configFile.println("sendrpid=no");
        }
        if (Compare.isTrue(rowTBLPBX.getString("BLFENABLE"))) {
            configFile.println("allowsubscribe=yes");
            configFile.println("notifyringing=yes");
            configFile.println("notifyhold=no");
            configFile.println("notifycid=yes");
            configFile.println("subscribecontext=tegsoft-INTERNAL");
        } else {
            configFile.println("allowsubscribe=no");
            configFile.println("notifyringing=no");
            configFile.println("notifyhold=no");
            configFile.println("notifycid=no");
            configFile.println("subscribecontext=tegsoft-INTERNAL");
        }
        configFile.println();
        configFile.println();
        configFile.close();
    }

    private static void writeFEATURES(DataRow rowTBLPBX) throws Exception {
        PrintWriter configFile = new PrintWriter(new FileWriter(configBase + "features.conf", false));
        configFile.println("[general]");
        configFile.println("parkingtime => 999");
        configFile.println();
        configFile.println("[applicationmap]");
        configFile.println();
        configFile.println();
        configFile.println("[featuremap]");
        configFile.println("blindxfer=##");
        configFile.println("atxfer=*2");
        configFile.println("automon=*1");
        configFile.println("disconnect=**");
        configFile.println();
        configFile.println();
        configFile.close();
        if (NullStatus.isNotNull(rowTBLPBX.getString("MP3RETENSION"))) {
            configFile = new PrintWriter(new FileWriter("/root/tegsoft_mp3.sh", false));
            configFile.println("for i in `find /var/spool/asterisk/monitor/ -mtime +" + rowTBLPBX.getString("MP3RETENSION") + " -name \"*.wav\"`");
            configFile.println("do");
            configFile.println("/usr/local/bin/lame --silent -V9 $i `/usr/bin/dirname $i`/`/bin/basename $i .wav`.mp3 && /bin/rm -rf $i");
            configFile.println("done");
            configFile.println();
            configFile.close();
        }
        if (NullStatus.isNotNull(rowTBLPBX.getString("NTPSRV"))) {
            configFile = new PrintWriter(new FileWriter("/root/tegsoft_fixdate.sh", false));
            configFile.println("/usr/sbin/ntpdate -u " + rowTBLPBX.getString("NTPSRV"));
            configFile.println();
            configFile.close();
        }
    }

    private static void writeTBLCCSKILLS(String PBXID) throws Exception {
        Dataset TBLCCSKILLS = new Dataset("TBLCCSKILLS", "TBLCCSKILLS");
        Command command = new Command("SELECT * FROM TBLCCSKILLS WHERE 1=1 AND UNITUID={UNITUID}");
        command.append("AND PBXID=");
        command.bind(PBXID);
        command.append("ORDER BY NAME");
        TBLCCSKILLS.fill(command);
        PrintWriter configFile = new PrintWriter(new FileWriter(configBase + "tegsoft_TBLCCSKILLS.conf", false));
        for (int i = 0; i < TBLCCSKILLS.getRowCount(); i++) {
            DataRow rowTBLCCSKILLS = TBLCCSKILLS.getRow(i);
            configFile.println(";" + rowTBLCCSKILLS.getString("NAME"));
            configFile.println("[" + rowTBLCCSKILLS.getString("SKILL") + "]");
            configFile.println("music=" + rowTBLCCSKILLS.getString("MOHID"));
            configFile.println("maxlen=" + rowTBLCCSKILLS.getDecimal("MAXLEN"));
            if (Compare.isTrue(rowTBLCCSKILLS.getString("JOINEMPTY"))) {
                configFile.println("joinempty=yes");
            } else {
                configFile.println("joinempty=no");
            }
            if (Compare.isTrue(rowTBLCCSKILLS.getString("LEAVEEMPTY"))) {
                configFile.println("leavewhenempty=yes");
            } else {
                configFile.println("leavewhenempty=no");
            }
            configFile.println("strategy=" + rowTBLCCSKILLS.getString("STRATEGY"));
            configFile.println("retry=" + rowTBLCCSKILLS.getDecimal("RETRY"));
            configFile.println("wrapuptime=" + rowTBLCCSKILLS.getDecimal("WRAPUPTIME"));
            configFile.println("timeout=" + rowTBLCCSKILLS.getDecimal("TIMEOUT"));
            configFile.println("announce-frequency=" + rowTBLCCSKILLS.getDecimal("ANNOUNCEFREQ"));
            configFile.println("announce-holdtime=" + rowTBLCCSKILLS.getString("ANNPOSITION"));
            configFile.println("weight=" + rowTBLCCSKILLS.getDecimal("PRIORITY"));
            if (Compare.isTrue(rowTBLCCSKILLS.getString("RINGINUSE"))) {
                configFile.println("ringinuse=yes");
            } else {
                configFile.println("ringinuse=no");
            }
            if (Compare.isTrue(rowTBLCCSKILLS.getString("AUTOFILL"))) {
                configFile.println("autofill=yes");
            } else {
                configFile.println("autofill=no");
            }
            configFile.println("servicelevel=" + rowTBLCCSKILLS.getDecimal("SERVICELEVEL"));
            if (Compare.isTrue(rowTBLCCSKILLS.getString("RECORDING"))) {
                configFile.println("monitor-format=wav");
            }
            configFile.println("reportholdtime=no");
            configFile.println("memberdelay=0");
            configFile.println("timeoutrestart=yes");
            configFile.println("eventmemberstatus=no");
            configFile.println("eventwhencalled=yes");
            configFile.println("setinterfacevar=yes");
            configFile.println("eventwhencalled=yes");
            configFile.println();
            configFile.println();
        }
        configFile.close();
    }

    private static void writeTBLPBXFILES() throws Exception {
        Dataset TBLPBXFILES = new Dataset("TBLPBXFILES", "TBLPBXFILES");
        Command command = new Command("SELECT * FROM TBLPBXFILES WHERE 1=1 AND UNITUID={UNITUID}");
        TBLPBXFILES.fill(command);
        for (int i = 0; i < TBLPBXFILES.getRowCount(); i++) {
            DataRow rowTBLPBXFILES = TBLPBXFILES.getRow(i);
            FileOutputStream fos = new FileOutputStream(new File("/var/lib/asterisk/sounds/" + rowTBLPBXFILES.getString("FILEID") + ".wav"), false);
            fos.write(rowTBLPBXFILES.getBytes("FILEDATA"));
            fos.flush();
            fos.close();
        }
    }

    private static void writeTBLPBXMOHFILES() throws Exception {
        Dataset TBLPBXMOH = new Dataset("TBLPBXMOH", "TBLPBXMOH");
        Dataset TBLPBXFILES = new Dataset("TBLPBXFILES", "TBLPBXFILES");
        TBLPBXFILES.addDataColumn("MOHID");
        TBLPBXFILES.addDataColumn("ORDERID");
        Command command = new Command("SELECT * FROM TBLPBXMOH WHERE 1=1 AND UNITUID={UNITUID}  ");
        TBLPBXMOH.fill(command);
        for (int j = 0; j < TBLPBXMOH.getRowCount(); j++) {
            DataRow rowTBLPBXMOH = TBLPBXMOH.getRow(j);
            command = new Command("SELECT B.MOHID,B.ORDERID,A.* FROM TBLPBXFILES A,TBLPBXMOHFILES B WHERE 1=1 AND A.UNITUID={UNITUID} AND A.UNITUID=B.UNITUID AND A.FILEID=B.FILEID AND MOHID=");
            command.bind(rowTBLPBXMOH.getString("MOHID"));
            TBLPBXFILES.fill(command);
            File classFolder = new File("/var/lib/asterisk/moh/" + rowTBLPBXMOH.getString("MOHID"));
            classFolder.mkdirs();
            File[] children = classFolder.listFiles();
            for (int i = 0; i < children.length; i++) {
                boolean delete = true;
                for (int k = 0; k < TBLPBXFILES.getRowCount(); k++) {
                    DataRow rowTBLPBXFILES = TBLPBXFILES.getRow(k);
                    String fileName = rowTBLPBXFILES.getString("ORDERID") + "_" + rowTBLPBXFILES.getString("FILEID") + ".wav";
                    if (Compare.equal(children[i].getName(), fileName)) {
                        delete = false;
                        break;
                    }
                }
                if (delete) {
                    children[i].delete();
                }
            }
            for (int i = 0; i < TBLPBXFILES.getRowCount(); i++) {
                DataRow rowTBLPBXFILES = TBLPBXFILES.getRow(i);
                FileOutputStream fos = new FileOutputStream(new File("/var/lib/asterisk/moh/" + rowTBLPBXFILES.getString("MOHID") + "/" + rowTBLPBXFILES.getString("ORDERID") + "_" + rowTBLPBXFILES.getString("FILEID") + ".wav"), false);
                fos.write(rowTBLPBXFILES.getBytes("FILEDATA"));
                fos.flush();
                fos.close();
            }
        }
        PrintWriter configFile = new PrintWriter(new FileWriter(configBase + "tegsoft_TBLPBXMOH.conf", false));
        for (int i = 0; i < TBLPBXMOH.getRowCount(); i++) {
            DataRow rowTBLPBXMOH = TBLPBXMOH.getRow(i);
            configFile.println(";" + rowTBLPBXMOH.getString("NAME"));
            configFile.println("[" + rowTBLPBXMOH.getString("MOHID") + "]");
            configFile.println("mode=files");
            configFile.println("directory=/var/lib/asterisk/moh/" + rowTBLPBXMOH.getString("MOHID"));
            if ("rndfiles".equals(rowTBLPBXMOH.getString("MOHMODE"))) {
                configFile.println("sort=random");
            } else {
                configFile.println("sort=alpha");
            }
            configFile.println();
            configFile.println();
        }
        configFile.println("[default]");
        configFile.println("mode=files");
        configFile.println("directory=/var/lib/asterisk/moh");
        configFile.println();
        configFile.println();
        configFile.println("[none]");
        configFile.println("mode=files");
        configFile.println("directory=/var/lib/asterisk/moh/.nomusic_reserved");
        configFile.println();
        configFile.println();
        configFile.close();
    }

    private static void writeTBLPBXEXT(String PBXID) throws Exception {
        Dataset TBLPBXEXT = new Dataset("TBLPBXEXT", "TBLPBXEXT");
        Command command = new Command("SELECT * FROM TBLPBXEXT WHERE 1=1 AND UNITUID={UNITUID}");
        command.append("AND PBXID=");
        command.bind(PBXID);
        command.append("AND DISABLED<>'true' ORDER BY EXTEN");
        TBLPBXEXT.fill(command);
        int count = TBLPBXEXT.getRowCount() / 50;
        if (count * 50 < TBLPBXEXT.getRowCount()) {
            count++;
        }
        PrintWriter configFile = new PrintWriter(new FileWriter(configBase + "tegsoft_TBLPBXEXT.conf", false));
        for (int i = 0; i < count; i++) {
            configFile.println("#include tegsoft_TBLPBXEXT" + i + ".conf");
        }
        configFile.println();
        configFile.println();
        configFile.close();
        for (int k = 0; k < count; k++) {
            configFile = new PrintWriter(new FileWriter(configBase + "tegsoft_TBLPBXEXT" + k + ".conf", false));
            int begin = k * 50;
            int end = begin + 50;
            if (end > TBLPBXEXT.getRowCount()) {
                end = TBLPBXEXT.getRowCount();
            }
            for (int i = begin; i < end; i++) {
                DataRow rowTBLPBXEXT = TBLPBXEXT.getRow(i);
                configFile.println("[" + rowTBLPBXEXT.getString("EXTEN") + "]");
                if (NullStatus.isNotNull(rowTBLPBXEXT.getString("SECRET"))) {
                    configFile.println("secret=" + rowTBLPBXEXT.getString("SECRET"));
                }
                if (NullStatus.isNotNull(rowTBLPBXEXT.getString("CALLERID"))) {
                    configFile.println("callerid=" + rowTBLPBXEXT.getString("CALLERID"));
                }
                if (NullStatus.isNotNull(rowTBLPBXEXT.getString("DTMFMODE"))) {
                    configFile.println("dtmfmode=" + rowTBLPBXEXT.getString("DTMFMODE"));
                }
                if (NullStatus.isNotNull(rowTBLPBXEXT.getString("CANREINVITE"))) {
                    configFile.println("canreinvite=" + rowTBLPBXEXT.getString("CANREINVITE"));
                }
                configFile.println("context=tegsoft-INTERNAL");
                if (NullStatus.isNotNull(rowTBLPBXEXT.getString("HOST"))) {
                    configFile.println("host=" + rowTBLPBXEXT.getString("HOST"));
                }
                if (NullStatus.isNotNull(rowTBLPBXEXT.getString("TYPE"))) {
                    configFile.println("type=" + rowTBLPBXEXT.getString("TYPE"));
                }
                if (NullStatus.isNotNull(rowTBLPBXEXT.getString("NAT"))) {
                    configFile.println("nat=" + rowTBLPBXEXT.getString("NAT"));
                }
                if (NullStatus.isNotNull(rowTBLPBXEXT.getString("PORT"))) {
                    configFile.println("port=" + rowTBLPBXEXT.getString("PORT"));
                }
                if (NullStatus.isNotNull(rowTBLPBXEXT.getString("QUALIFY"))) {
                    configFile.println("qualify=" + rowTBLPBXEXT.getString("QUALIFY"));
                }
                configFile.println("Language=" + rowTBLPBXEXT.getString("LANGUAGE"));
                if (NullStatus.isNotNull(rowTBLPBXEXT.getString("CALLGROUP"))) {
                    configFile.println("callgroup=" + rowTBLPBXEXT.getString("CALLGROUP"));
                }
                if (NullStatus.isNotNull(rowTBLPBXEXT.getString("PICKUPGROUP"))) {
                    configFile.println("pickupgroup=" + rowTBLPBXEXT.getString("PICKUPGROUP"));
                }
                configFile.println("callcounter=yes");
                configFile.println("mailbox=" + rowTBLPBXEXT.getString("EXTEN"));
                configFile.println("faxdetect=no");
                configFile.println("call-limit=50");
                configFile.println();
                configFile.println();
            }
            configFile.close();
        }
    }

    private static void writeTBLPBXVM(String PBXID) throws Exception {
        Dataset TBLPBXEXT = new Dataset("TBLPBXEXT", "TBLPBXEXT");
        Command command = new Command("SELECT * FROM TBLPBXEXT WHERE 1=1 AND UNITUID={UNITUID}");
        command.append("AND PBXID=");
        command.bind(PBXID);
        command.append("AND DISABLED<>'true' AND MAILBOX IS NOT NULL ORDER BY EXTEN");
        TBLPBXEXT.fill(command);
        PrintWriter configFile = new PrintWriter(new FileWriter(configBase + "tegsoft_TBLPBXVM.conf", false));
        for (int i = 0; i < TBLPBXEXT.getRowCount(); i++) {
            DataRow rowTBLPBXEXT = TBLPBXEXT.getRow(i);
            String vmline = rowTBLPBXEXT.getString("EXTEN") + " => " + rowTBLPBXEXT.getString("EXTEN") + "," + rowTBLPBXEXT.getString("EXTEN") + ",";
            vmline += rowTBLPBXEXT.getString("MAILBOX") + ",,attach=yes|saycid=no|envelope=no|delete=no";
            configFile.println(vmline);
        }
        configFile.println();
        configFile.println();
        configFile.close();
    }

    public static void writeLOGROTATE() throws Exception {
        PrintWriter configFile = new PrintWriter(new FileWriter("/etc/logrotate.d/tegsoft_LOGROTATE.conf", false));
        configFile.println("/var/log/asterisk/full {");
        configFile.println("missingok");
        configFile.println("rotate 5");
        configFile.println("daily");
        configFile.println("create 0640 asterisk asterisk");
        configFile.println("postrotate");
        configFile.println("/usr/sbin/asterisk -rx 'logger reload' > /dev/null 2> /dev/null");
        configFile.println("endscript");
        configFile.println("}");
        configFile.println();
        configFile.println();
        configFile.close();
    }

    public static void writeFOP(String PBXID) throws Exception {
        int fopPosition = 1;
        PrintWriter configFileFOP = new PrintWriter(new FileWriter(configBase + "tegsoft_FOP.conf", false));
        Dataset TBLPBXEXT = new Dataset("TBLPBXEXT", "TBLPBXEXT");
        Command command = new Command("SELECT * FROM TBLPBXEXT WHERE 1=1 AND UNITUID={UNITUID}");
        command.append("AND DISABLED<>'true' AND DISPLAYINFOP='true' ");
        command.append("AND PBXID=");
        command.bind(PBXID);
        command.append("ORDER BY EXTEN");
        TBLPBXEXT.fill(command);
        for (int i = 0; i < TBLPBXEXT.getRowCount(); i++) {
            DataRow rowTBLPBXEXT = TBLPBXEXT.getRow(i);
            String callerId = rowTBLPBXEXT.getString("CALLERID");
            command = new Command("SELECT NVL(A.USERNAME,'')||' '||NVL(A.SURNAME,'') FROM TBLUSERS A,TBLCCAGENTLOG B ");
            command.append("WHERE A.UNITUID={UNITUID} AND A.UNITUID=B.UNITUID AND A.UID=B.UID AND B.LOGTYPE='LOGIN' AND B.ENDDATE IS NULL AND B.INTERFACE=");
            command.bind("SIP/" + rowTBLPBXEXT.getString("EXTEN"));
            String agentName = command.executeScalarAsString();
            if (NullStatus.isNotNull(agentName)) {
                callerId = agentName;
            }
            configFileFOP.println("[SIP/" + rowTBLPBXEXT.getString("EXTEN") + "]");
            configFileFOP.println("Position=" + fopPosition++);
            configFileFOP.println("Label=\"" + rowTBLPBXEXT.getString("EXTEN") + ":" + StringUtil.convertToEnglishOnlyLetters(callerId).toUpperCase(Locale.US) + " \"");
            configFileFOP.println("Extension=1");
            configFileFOP.println("Context=tegsoft-INTERNAL");
            configFileFOP.println("Icon=4");
            configFileFOP.println();
            configFileFOP.println();
            if (fopPosition > 60) {
                break;
            }
        }
        boolean shifted = false;
        fopPosition = 61;
        Dataset TBLCCSKILLS = new Dataset("TBLCCSKILLS", "TBLCCSKILLS");
        command = new Command("SELECT * FROM TBLCCSKILLS WHERE 1=1 AND UNITUID={UNITUID}");
        command.append("AND PBXID=");
        command.bind(PBXID);
        command.append("ORDER BY NAME");
        TBLCCSKILLS.fill(command);
        for (int i = 0; i < TBLCCSKILLS.getRowCount(); i++) {
            DataRow rowTBLCCSKILLS = TBLCCSKILLS.getRow(i);
            configFileFOP.println("[QUEUE/" + rowTBLCCSKILLS.getString("SKILL") + "]");
            configFileFOP.println("Position=" + fopPosition++);
            configFileFOP.println("Label=\"" + rowTBLCCSKILLS.getString("SKILL") + ":" + StringUtil.convertToEnglishOnlyLetters(rowTBLCCSKILLS.getString("NAME")) + " \"");
            configFileFOP.println("Extension=-1");
            configFileFOP.println("Context=tegsoft-INTERNAL");
            configFileFOP.println("Icon=5");
            configFileFOP.println();
            configFileFOP.println();
            if ((fopPosition > 68) && (!shifted)) {
                shifted = true;
                fopPosition = 81;
            }
        }
        shifted = false;
        fopPosition = 69;
        Dataset TBLPBXCONF = new Dataset("TBLPBXCONF", "TBLPBXCONF");
        command = new Command("SELECT * FROM TBLPBXCONF WHERE 1=1 AND UNITUID={UNITUID}");
        command.append("AND PBXID=");
        command.bind(PBXID);
        command.append("ORDER BY ROOMNAME");
        TBLPBXCONF.fill(command);
        for (int i = 0; i < TBLPBXCONF.getRowCount(); i++) {
            DataRow rowTBLPBXCONF = TBLPBXCONF.getRow(i);
            configFileFOP.println("[" + rowTBLPBXCONF.getString("EXTEN") + "]");
            configFileFOP.println("Position=" + fopPosition++);
            configFileFOP.println("Label=\"" + rowTBLPBXCONF.getString("EXTEN") + ":" + StringUtil.convertToEnglishOnlyLetters(rowTBLPBXCONF.getString("ROOMNAME")) + " \"");
            configFileFOP.println("Extension=-1");
            configFileFOP.println("Context=tegsoft-INTERNAL");
            configFileFOP.println("Icon=6");
            configFileFOP.println();
            configFileFOP.println();
            if ((fopPosition > 72) && (!shifted)) {
                shifted = true;
                fopPosition = 89;
            }
        }
        shifted = false;
        fopPosition = 73;
        Dataset TBLPBXTRUNK = new Dataset("TBLPBXTRUNK", "TBLPBXTRUNK");
        command = new Command("SELECT * FROM TBLPBXTRUNK WHERE 1=1 AND UNITUID={UNITUID}");
        command.append("AND DISABLED<>'true' ORDER BY TRUNKNAME");
        TBLPBXTRUNK.fill(command);
        for (int i = 0; i < TBLPBXTRUNK.getRowCount(); i++) {
            DataRow rowTBLPBXTRUNK = TBLPBXTRUNK.getRow(i);
            configFileFOP.println("[SIP/" + rowTBLPBXTRUNK.getString("TRUNKID") + "]");
            configFileFOP.println("Position=" + fopPosition++);
            configFileFOP.println("Label=\"" + StringUtil.convertToEnglishOnlyLetters(rowTBLPBXTRUNK.getString("TRUNKNAME")) + " \"");
            configFileFOP.println("Extension=-1");
            configFileFOP.println("Context=tegsoft-INTERNAL");
            configFileFOP.println("Icon=3");
            configFileFOP.println();
            configFileFOP.println();
            if ((fopPosition > 80) && (!shifted)) {
                shifted = true;
                fopPosition = 93;
            }
        }
        configFileFOP.close();
    }

    private static void extractSOUNDFILES() throws Exception {
        ArrayList<File> soundFiles = new ArrayList<File>();
        File soundsFolder = new File("/var/lib/asterisk/sounds/");
        if (soundsFolder.exists()) {
            File soundFiles1[] = soundsFolder.listFiles(new FilenameFilter() {

                @Override
                public boolean accept(File dir, String name) {
                    if (NullStatus.isNull(name)) {
                        return false;
                    }
                    if (name.endsWith("-sounds.jar")) {
                        return true;
                    }
                    return false;
                }
            });
            for (int i = 0; i < soundFiles1.length; i++) {
                soundFiles.add(soundFiles1[i]);
            }
        }
        soundsFolder = new File("/opt/jboss/server/default/deploy/Tobe.war/WEB-INF/lib/");
        if (soundsFolder.exists()) {
            File soundFiles1[] = soundsFolder.listFiles(new FilenameFilter() {

                @Override
                public boolean accept(File dir, String name) {
                    if (NullStatus.isNull(name)) {
                        return false;
                    }
                    if (name.endsWith("-sounds.jar")) {
                        return true;
                    }
                    return false;
                }
            });
            for (int i = 0; i < soundFiles1.length; i++) {
                soundFiles.add(soundFiles1[i]);
            }
        }
        for (int i = 0; i < soundFiles.size(); i++) {
            ZipFile zipFile = new ZipFile(soundFiles.get(i));
            InputStream inputStream = null;
            Enumeration<? extends ZipEntry> oEnum = zipFile.entries();
            while (oEnum.hasMoreElements()) {
                ZipEntry zipEntry = oEnum.nextElement();
                File file = new File("/var/lib/asterisk/sounds/", zipEntry.getName());
                if (zipEntry.isDirectory()) {
                    file.mkdirs();
                } else {
                    inputStream = zipFile.getInputStream(zipEntry);
                    FileOutputStream fos = new FileOutputStream(file);
                    IOUtils.copy(inputStream, fos);
                    fos.close();
                }
            }
        }
    }

    private static void writeTBLPBX(String PBXID, String PROVINCEID) throws Exception {
        Dataset TBLPBX = new Dataset("TBLPBX", "TBLPBX");
        Command command = new Command("SELECT * FROM TBLPBX WHERE 1=1 AND UNITUID={UNITUID}");
        command.append("AND PBXID<>");
        command.bind(PBXID);
        command.append("ORDER BY PBXNAME");
        TBLPBX.fill(command);
        PrintWriter configFile = new PrintWriter(new FileWriter(configBase + "tegsoft_TBLPBX.conf", false));
        for (int i = 0; i < TBLPBX.getRowCount(); i++) {
            DataRow rowTBLPBX = TBLPBX.getRow(i);
            configFile.println(";" + rowTBLPBX.getString("PBXNAME"));
            configFile.println("[" + rowTBLPBX.getString("PBXID") + "]");
            configFile.println("dtmfmode=RFC2833");
            configFile.println("canreinvite=yes");
            configFile.println("context=tegsoft-TBLPBX");
            configFile.println("host=" + rowTBLPBX.getString("IPADDRESS"));
            configFile.println("type=friend");
            configFile.println("qualify=yes");
            configFile.println("insecure=port,invite");
            configFile.println("transport=UDP");
            configFile.println("nat=yes");
            configFile.println("videosupport=yes");
            configFile.println("disallow=all");
            if (Compare.equal(PROVINCEID, rowTBLPBX.getString("PROVINCEID"))) {
                configFile.println("allow=ulaw");
            } else {
                configFile.println("allow=g729");
            }
            configFile.println();
            configFile.println();
        }
        configFile.close();
    }

    private static void writeREGISTER() throws Exception {
        Dataset TBLPBXTRUNK = new Dataset("TBLPBXTRUNK", "TBLPBXTRUNK");
        Command command = new Command("SELECT * FROM TBLPBXTRUNK WHERE 1=1 AND UNITUID={UNITUID}");
        command.append("AND DISABLED<>'true' ORDER BY TRUNKNAME");
        TBLPBXTRUNK.fill(command);
        PrintWriter configFile = new PrintWriter(new FileWriter(configBase + "tegsoft_REGISTER.conf", false));
        for (int i = 0; i < TBLPBXTRUNK.getRowCount(); i++) {
            DataRow rowTBLPBXTRUNK = TBLPBXTRUNK.getRow(i);
            if (NullStatus.isNull(rowTBLPBXTRUNK.getString("REGISTER"))) {
                continue;
            }
            configFile.println("register => " + rowTBLPBXTRUNK.getString("REGISTER"));
            configFile.println();
            configFile.println();
        }
        configFile.close();
    }

    private static void writeTBLPBXFAX(String PBXID) throws Exception {
        Dataset TBLPBXFAX = new Dataset("TBLPBXFAX", "TBLPBXFAX");
        Command command = new Command("SELECT * FROM TBLPBXFAX WHERE 1=1 AND UNITUID={UNITUID}");
        command.append("AND PBXID=");
        command.bind(PBXID);
        command.append("ORDER BY EXTEN");
        TBLPBXFAX.fill(command);
        PrintWriter configFile = new PrintWriter(new FileWriter(configBase + "tegsoft_TBLPBXFAX.conf", false));
        for (int i = 0; i < TBLPBXFAX.getRowCount(); i++) {
            DataRow rowTBLPBXFAX = TBLPBXFAX.getRow(i);
            configFile.println(";" + rowTBLPBXFAX.getString("FAXNAME"));
            configFile.println("[" + rowTBLPBXFAX.getString("FAXID") + "]");
            configFile.println("deny=0.0.0.0/0.0.0.0");
            configFile.println("permit=127.0.0.1/255.255.255.255");
            configFile.println("transfer=no");
            configFile.println("context=tegsoft-INTERNAL");
            configFile.println("host=dynamic");
            configFile.println("type=friend");
            configFile.println("qualify=yes");
            configFile.println("port=4569");
            configFile.println("secret=" + rowTBLPBXFAX.getString("FAXID"));
            configFile.println("callerid=" + rowTBLPBXFAX.getString("FAXNAME") + " <" + rowTBLPBXFAX.getString("EXTEN") + ">");
            configFile.println();
            configFile.println();
        }
        configFile.close();
        for (int i = 0; i < TBLPBXFAX.getRowCount(); i++) {
            DataRow rowTBLPBXFAX = TBLPBXFAX.getRow(i);
            configFile = new PrintWriter(new FileWriter("/etc/iaxmodem/" + rowTBLPBXFAX.getString("FAXID"), false));
            configFile.println("device          /dev/" + rowTBLPBXFAX.getString("FAXID"));
            configFile.println("owner           uucp:uucp");
            configFile.println("mode            660");
            configFile.println("port            4000" + (i + 1));
            configFile.println("refresh         300");
            configFile.println("server          127.0.0.1");
            configFile.println("peername        " + rowTBLPBXFAX.getString("FAXID"));
            configFile.println("secret          " + rowTBLPBXFAX.getString("FAXID"));
            configFile.println("cidname         " + rowTBLPBXFAX.getString("FAXNAME"));
            configFile.println("cidnumber       " + rowTBLPBXFAX.getString("EXTEN"));
            configFile.println("codec           ulaw");
            configFile.println();
            configFile.println();
            configFile.close();
        }
        for (int i = 0; i < TBLPBXFAX.getRowCount(); i++) {
            DataRow rowTBLPBXFAX = TBLPBXFAX.getRow(i);
            configFile = new PrintWriter(new FileWriter("/var/spool/hylafax/etc/config." + rowTBLPBXFAX.getString("FAXID"), false));
            configFile.println("CountryCode:		" + rowTBLPBXFAX.getString("COUNTRYCODE"));
            configFile.println("AreaCode:		" + rowTBLPBXFAX.getString("AREACODE"));
            configFile.println("FAXNumber:		" + rowTBLPBXFAX.getString("PHONE"));
            configFile.println("LongDistancePrefix:		" + rowTBLPBXFAX.getString("LONGPREFIX"));
            configFile.println("InternationalPrefix:		" + rowTBLPBXFAX.getString("INTPREFIX"));
            configFile.println("DialStringRules:	etc/dialrules");
            configFile.println("ServerTracing:		1");
            configFile.println("SessionTracing:		11");
            configFile.println("RecvFileMode:		0600");
            configFile.println("LogFileMode:		0600");
            configFile.println("DeviceMode:		0600");
            configFile.println("RingsBeforeAnswer:	1");
            configFile.println("SpeakerVolume:		off");
            configFile.println("GettyArgs:		\"-h %l dx_%s\"");
            configFile.println("LocalIdentifier:	\"NothingSetup\"");
            configFile.println("TagLineFont:		etc/lutRS18.pcf");
            configFile.println("TagLineFormat:		\"From %%l|%c|Page %%P of %%T\"");
            configFile.println("MaxRecvPages:		50");
            configFile.println();
            configFile.println("ModemType:		Class1");
            configFile.println("Class1Cmd:		AT+FCLASS=1.0");
            configFile.println("Class1PPMWaitCmd:	AT+FTS=7");
            configFile.println("Class1TCFWaitCmd:	AT+FTS=7");
            configFile.println("Class1EOPWaitCmd:	AT+FTS=9");
            configFile.println("Class1SwitchingCmd:	AT+FRS=7");
            configFile.println("Class1RecvAbortOK:	200");
            configFile.println("Class1FrameOverhead:	4");
            configFile.println("Class1RecvIdentTimer:	40000");
            configFile.println("Class1TCFMaxNonZero:	10");
            configFile.println("Class1TCFMinRun:	1000");
            configFile.println();
            configFile.println();
            configFile.close();
        }
        configFile = new PrintWriter(new FileWriter("/var/spool/hylafax/etc/FaxDispatch", false));
        configFile.println("SENDTO=root;");
        configFile.println("FILETYPE=pdf;");
        configFile.println();
        configFile.println();
        if (TBLPBXFAX.getRowCount() > 0) {
            configFile.println("case \"$DEVICE\" in");
        }
        for (int i = 0; i < TBLPBXFAX.getRowCount(); i++) {
            DataRow rowTBLPBXFAX = TBLPBXFAX.getRow(i);
            configFile.println(" " + rowTBLPBXFAX.getString("FAXID") + ")   SENDTO=" + rowTBLPBXFAX.getString("EMAIL") + ";;");
        }
        if (TBLPBXFAX.getRowCount() > 0) {
            configFile.println("esac");
            configFile.println();
            configFile.println();
        }
        configFile.close();
        configFile = new PrintWriter(new FileWriter("/etc/inittab", false));
        configFile.println("# tegsoft auto generated");
        configFile.println("id:3:initdefault:");
        configFile.println("");
        configFile.println("");
        configFile.println("si::sysinit:/etc/rc.d/rc.sysinit");
        configFile.println("");
        configFile.println("");
        configFile.println("l0:0:wait:/etc/rc.d/rc 0");
        configFile.println("l1:1:wait:/etc/rc.d/rc 1");
        configFile.println("l2:2:wait:/etc/rc.d/rc 2");
        configFile.println("l3:3:wait:/etc/rc.d/rc 3");
        configFile.println("l4:4:wait:/etc/rc.d/rc 4");
        configFile.println("l5:5:wait:/etc/rc.d/rc 5");
        configFile.println("l6:6:wait:/etc/rc.d/rc 6");
        configFile.println("");
        configFile.println("ca::ctrlaltdel:/sbin/shutdown -t3 -r now");
        configFile.println("");
        configFile.println("pf::powerfail:/sbin/shutdown -f -h +2 \"Power Failure; System Shutting Down\"");
        configFile.println("");
        configFile.println("pr:12345:powerokwait:/sbin/shutdown -c \"Power Restored; Shutdown Cancelled\"");
        configFile.println("");
        configFile.println("1:2345:respawn:/sbin/mingetty tty1");
        configFile.println("2:2345:respawn:/sbin/mingetty tty2");
        configFile.println("3:2345:respawn:/sbin/mingetty tty3");
        configFile.println("4:2345:respawn:/sbin/mingetty tty4");
        configFile.println("5:2345:respawn:/sbin/mingetty tty5");
        configFile.println("6:2345:respawn:/sbin/mingetty tty6");
        configFile.println("");
        configFile.println("x:5:respawn:/etc/X11/prefdm -nodaemon");
        configFile.println("fmc:2345:respawn:/opt/ibm/db2/V9.7/bin/db2fmcd #DB2 Fault Monitor Coordinator");
        configFile.println("");
        for (int i = 0; i < TBLPBXFAX.getRowCount(); i++) {
            DataRow rowTBLPBXFAX = TBLPBXFAX.getRow(i);
            configFile.println("fx" + (i + 1) + ":2345:respawn:/usr/sbin/faxgetty " + rowTBLPBXFAX.getString("FAXID") + " # tegsoft auto generated");
        }
        if (TBLPBXFAX.getRowCount() > 0) {
            configFile.println("");
            configFile.println("");
        }
        configFile.close();
    }

    private static void writeTBLPBXTRUNK() throws Exception {
        Dataset TBLPBXTRUNK = new Dataset("TBLPBXTRUNK", "TBLPBXTRUNK");
        Command command = new Command("SELECT * FROM TBLPBXTRUNK WHERE 1=1 AND UNITUID={UNITUID}");
        command.append("AND DISABLED<>'true' ORDER BY TYPE DESC");
        TBLPBXTRUNK.fill(command);
        PrintWriter configFile = new PrintWriter(new FileWriter(configBase + "tegsoft_TBLPBXTRUNK.conf", false));
        for (int i = 0; i < TBLPBXTRUNK.getRowCount(); i++) {
            DataRow rowTBLPBXTRUNK = TBLPBXTRUNK.getRow(i);
            configFile.println(";" + rowTBLPBXTRUNK.getString("TRUNKNAME"));
            configFile.println("[" + rowTBLPBXTRUNK.getString("TRUNKID") + "]");
            if (NullStatus.isNotNull(rowTBLPBXTRUNK.getString("USERNAME"))) {
                configFile.println("username=" + rowTBLPBXTRUNK.getString("USERNAME"));
            }
            if (NullStatus.isNotNull(rowTBLPBXTRUNK.getString("SECRET"))) {
                configFile.println("secret=" + rowTBLPBXTRUNK.getString("SECRET"));
            }
            if (NullStatus.isNotNull(rowTBLPBXTRUNK.getString("DTMFMODE"))) {
                configFile.println("dtmfmode=" + rowTBLPBXTRUNK.getString("DTMFMODE"));
            }
            if (NullStatus.isNotNull(rowTBLPBXTRUNK.getString("CANREINVITE"))) {
                configFile.println("canreinvite=" + rowTBLPBXTRUNK.getString("CANREINVITE"));
            }
            configFile.println("callcounter=yes");
            configFile.println("call-limit=1000");
            if (NullStatus.isNotNull(rowTBLPBXTRUNK.getString("HOST"))) {
                configFile.println("host=" + rowTBLPBXTRUNK.getString("HOST"));
            }
            configFile.println("type=peer");
            configFile.println("context=from" + rowTBLPBXTRUNK.getString("TRUNKID"));
            if (NullStatus.isNotNull(rowTBLPBXTRUNK.getString("NAT"))) {
                configFile.println("nat=" + rowTBLPBXTRUNK.getString("NAT"));
            }
            if (NullStatus.isNotNull(rowTBLPBXTRUNK.getString("PORT"))) {
                configFile.println("port=" + rowTBLPBXTRUNK.getString("PORT"));
            }
            if (NullStatus.isNotNull(rowTBLPBXTRUNK.getString("QUALIFY"))) {
                configFile.println("qualify=" + rowTBLPBXTRUNK.getString("QUALIFY"));
            }
            if (NullStatus.isNotNull(rowTBLPBXTRUNK.getString("INSECURE"))) {
                configFile.println("insecure=" + rowTBLPBXTRUNK.getString("INSECURE"));
            }
            if (NullStatus.isNotNull(rowTBLPBXTRUNK.getString("FROMUSER"))) {
                configFile.println("fromuser=" + rowTBLPBXTRUNK.getString("FROMUSER"));
            }
            if (NullStatus.isNotNull(rowTBLPBXTRUNK.getString("FROMDOMAIN"))) {
                configFile.println("fromdomain=" + rowTBLPBXTRUNK.getString("FROMDOMAIN"));
            }
            if (NullStatus.isNotNull(rowTBLPBXTRUNK.getString("AUTOCREATEPEER"))) {
                configFile.println("autocreatepeer=" + rowTBLPBXTRUNK.getString("AUTOCREATEPEER"));
            }
            if (NullStatus.isNotNull(rowTBLPBXTRUNK.getString("TRANSPORT"))) {
                configFile.println("transport=" + rowTBLPBXTRUNK.getString("TRANSPORT"));
            }
            if (NullStatus.isNotNull(rowTBLPBXTRUNK.getString("VIDEOSUPPORT"))) {
                configFile.println("videosupport=" + rowTBLPBXTRUNK.getString("VIDEOSUPPORT"));
            }
            configFile.println("disallow=all");
            String ALLOW = rowTBLPBXTRUNK.getString("ALLOW");
            if (NullStatus.isNotNull(ALLOW)) {
                String ALLOWs[] = ALLOW.split("&");
                for (int j = 0; j < ALLOWs.length; j++) {
                    configFile.println("allow=" + ALLOWs[j]);
                }
            }
            configFile.println();
            configFile.println();
        }
        configFile.close();
    }

    private static void writeCONTEXT(DataRow rowTBLPBX) throws Exception {
        PrintWriter configFile = new PrintWriter(new FileWriter(configBase + "tegsoft_CONTEXT.conf", false));
        configFile.println("[tegsoft-INTERNAL]");
        configFile.println("exten => _X.,1,NoOp(Tegsoft macro running)");
        configFile.println("exten => _X.,n,Agi(agi://localhost/IntelligentCallRouting?context=tegsoft-INTERNAL)");
        configFile.println("exten => _X.,n,Hangup");
        configFile.println("exten => 1,1,NoOp(Tegsoft macro running)");
        configFile.println("exten => 1,n,Agi(agi://localhost/IntelligentCallRouting?context=tegsoft-INTERNAL)");
        configFile.println("exten => 1,n,Hangup");
        configFile.println("exten => 2,1,NoOp(Tegsoft macro running)");
        configFile.println("exten => 2,n,Agi(agi://localhost/IntelligentCallRouting?context=tegsoft-INTERNAL)");
        configFile.println("exten => 2,n,Hangup");
        configFile.println("exten => 3,1,NoOp(Tegsoft macro running)");
        configFile.println("exten => 3,n,Agi(agi://localhost/IntelligentCallRouting?context=tegsoft-INTERNAL)");
        configFile.println("exten => 3,n,Hangup");
        configFile.println("exten => 4,1,NoOp(Tegsoft macro running)");
        configFile.println("exten => 4,n,Agi(agi://localhost/IntelligentCallRouting?context=tegsoft-INTERNAL)");
        configFile.println("exten => 4,n,Hangup");
        configFile.println("exten => 5,1,NoOp(Tegsoft macro running)");
        configFile.println("exten => 5,n,Agi(agi://localhost/IntelligentCallRouting?context=tegsoft-INTERNAL)");
        configFile.println("exten => 5,n,Hangup");
        configFile.println("exten => 6,1,NoOp(Tegsoft macro running)");
        configFile.println("exten => 6,n,Agi(agi://localhost/IntelligentCallRouting?context=tegsoft-INTERNAL)");
        configFile.println("exten => 6,n,Hangup");
        configFile.println("exten => 7,1,NoOp(Tegsoft macro running)");
        configFile.println("exten => 7,n,Agi(agi://localhost/IntelligentCallRouting?context=tegsoft-INTERNAL)");
        configFile.println("exten => 7,n,Hangup");
        configFile.println("exten => 8,1,NoOp(Tegsoft macro running)");
        configFile.println("exten => 8,n,Agi(agi://localhost/IntelligentCallRouting?context=tegsoft-INTERNAL)");
        configFile.println("exten => 8,n,Hangup");
        configFile.println("exten => 9,1,NoOp(Tegsoft macro running)");
        configFile.println("exten => 9,n,Agi(agi://localhost/IntelligentCallRouting?context=tegsoft-INTERNAL)");
        configFile.println("exten => 9,n,Hangup");
        configFile.println("exten => 0,1,NoOp(Tegsoft macro running)");
        configFile.println("exten => 0,n,Agi(agi://localhost/IntelligentCallRouting?context=tegsoft-INTERNAL)");
        configFile.println("exten => 0,n,Hangup");
        configFile.println("exten => " + rowTBLPBX.getString("FCCF") + ",1,NoOp(Tegsoft macro running)");
        configFile.println("exten => " + rowTBLPBX.getString("FCCF") + ",n,Agi(agi://localhost/IntelligentCallRouting?context=tegsoft-INTERNAL)");
        configFile.println("exten => " + rowTBLPBX.getString("FCCF") + ",n,Hangup");
        configFile.println("exten => " + rowTBLPBX.getString("FCCFD") + ",1,NoOp(Tegsoft macro running)");
        configFile.println("exten => " + rowTBLPBX.getString("FCCFD") + ",n,Agi(agi://localhost/IntelligentCallRouting?context=tegsoft-INTERNAL)");
        configFile.println("exten => " + rowTBLPBX.getString("FCCFD") + ",n,Hangup");
        configFile.println("exten => " + rowTBLPBX.getString("FCCFB") + ",1,NoOp(Tegsoft macro running)");
        configFile.println("exten => " + rowTBLPBX.getString("FCCFB") + ",n,Agi(agi://localhost/IntelligentCallRouting?context=tegsoft-INTERNAL)");
        configFile.println("exten => " + rowTBLPBX.getString("FCCFB") + ",n,Hangup");
        configFile.println("exten => " + rowTBLPBX.getString("FCCFBD") + ",1,NoOp(Tegsoft macro running)");
        configFile.println("exten => " + rowTBLPBX.getString("FCCFBD") + ",n,Agi(agi://localhost/IntelligentCallRouting?context=tegsoft-INTERNAL)");
        configFile.println("exten => " + rowTBLPBX.getString("FCCFBD") + ",n,Hangup");
        configFile.println("exten => " + rowTBLPBX.getString("FCCFNA") + ",1,NoOp(Tegsoft macro running)");
        configFile.println("exten => " + rowTBLPBX.getString("FCCFNA") + ",n,Agi(agi://localhost/IntelligentCallRouting?context=tegsoft-INTERNAL)");
        configFile.println("exten => " + rowTBLPBX.getString("FCCFNA") + ",n,Hangup");
        configFile.println("exten => " + rowTBLPBX.getString("FCCFNAD") + ",1,NoOp(Tegsoft macro running)");
        configFile.println("exten => " + rowTBLPBX.getString("FCCFNAD") + ",n,Agi(agi://localhost/IntelligentCallRouting?context=tegsoft-INTERNAL)");
        configFile.println("exten => " + rowTBLPBX.getString("FCCFNAD") + ",n,Hangup");
        configFile.println("exten => " + rowTBLPBX.getString("FCVM") + ",1,NoOp(Tegsoft macro running)");
        configFile.println("exten => " + rowTBLPBX.getString("FCVM") + ",n,Agi(agi://localhost/IntelligentCallRouting?context=tegsoft-INTERNAL)");
        configFile.println("exten => " + rowTBLPBX.getString("FCVM") + ",n,Hangup");
        configFile.println("exten => " + rowTBLPBX.getString("FCGPICKUP") + ",1,NoOp(Tegsoft macro running)");
        configFile.println("exten => " + rowTBLPBX.getString("FCGPICKUP") + ",n,Agi(agi://localhost/IntelligentCallRouting?context=tegsoft-INTERNAL)");
        configFile.println("exten => " + rowTBLPBX.getString("FCGPICKUP") + ",n,Hangup");
        configFile.println("exten => " + rowTBLPBX.getString("FCDPICKUP") + ".,1,NoOp(Tegsoft macro running)");
        configFile.println("exten => " + rowTBLPBX.getString("FCDPICKUP") + ".,n,Agi(agi://localhost/IntelligentCallRouting?context=tegsoft-INTERNAL)");
        configFile.println("exten => " + rowTBLPBX.getString("FCDPICKUP") + ".,n,Hangup");
        configFile.println("exten => s,1,Hangup");
        configFile.println("exten => h,1,Hangup");
        Dataset TBLPBXEXT = new Dataset("TBLPBXEXT", "TBLPBXEXT");
        Command command = new Command("SELECT * FROM TBLPBXEXT WHERE 1=1 AND UNITUID={UNITUID}");
        command.append("AND PBXID=");
        command.bind(rowTBLPBX.getString("PBXID"));
        command.append("AND DISABLED<>'true' ORDER BY EXTEN");
        TBLPBXEXT.fill(command);
        for (int i = 0; i < TBLPBXEXT.getRowCount(); i++) {
            DataRow rowTBLPBXEXT = TBLPBXEXT.getRow(i);
            configFile.println("exten => " + rowTBLPBXEXT.getString("EXTEN") + ",hint,SIP/" + rowTBLPBXEXT.getString("EXTEN"));
        }
        configFile.println();
        configFile.println();
        configFile.println("[tegsoft-TBLPBX]");
        configFile.println("exten => _X.,1,NoOp(Tegsoft macro running)");
        configFile.println("exten => _X.,n,Agi(agi://localhost/IntelligentCallRouting?context=tegsoft-TBLPBX)");
        configFile.println("exten => _X.,n,Hangup");
        configFile.println("exten => s,1,Hangup");
        configFile.println("exten => h,1,Hangup");
        configFile.println();
        configFile.println();
        configFile.println("[default]");
        configFile.println("exten => _X.,1,NoOp(Tegsoft macro running)");
        configFile.println("exten => _X.,n,Agi(agi://localhost/IntelligentCallRouting?context=tegsoft-INTERNAL)");
        configFile.println("exten => _X.,n,Hangup");
        configFile.println("exten => s,1,Hangup");
        configFile.println("exten => h,1,Hangup");
        configFile.println();
        configFile.println();
        Dataset TBLPBXTRUNK = new Dataset("TBLPBXTRUNK", "TBLPBXTRUNK");
        command = new Command("SELECT * FROM TBLPBXTRUNK WHERE 1=1 AND UNITUID={UNITUID}");
        command.append("AND DISABLED<>'true' ORDER BY TRUNKNAME");
        TBLPBXTRUNK.fill(command);
        for (int i = 0; i < TBLPBXTRUNK.getRowCount(); i++) {
            DataRow rowTBLPBXTRUNK = TBLPBXTRUNK.getRow(i);
            configFile.println(";" + rowTBLPBXTRUNK.getString("TRUNKNAME"));
            configFile.println("[from" + rowTBLPBXTRUNK.getString("TRUNKID") + "]");
            configFile.println("exten => _X.,1,NoOp(Tegsoft macro running)");
            configFile.println("exten => _X.,n,Agi(agi://localhost/IntelligentCallRouting?context=tegsoft-INCOMMING&trunkid=" + rowTBLPBXTRUNK.getString("TRUNKID") + ")");
            configFile.println("exten => _X.,n,Set(CDR(userfield)=${MEMBERINTERFACE})");
            configFile.println("exten => _X.,n,Hangup");
            configFile.println("exten => fax,1,NoOp(Tegsoft macro running)");
            configFile.println("exten => fax,n,Agi(agi://localhost/IntelligentCallRouting?context=tegsoft-INCOMMING&trunkid=" + rowTBLPBXTRUNK.getString("TRUNKID") + ")");
            configFile.println("exten => h,1,Set(CDR(userfield)=${MEMBERINTERFACE})");
            configFile.println("exten => h,n,Hangup");
            configFile.println();
            configFile.println();
        }
        configFile.close();
    }

    private static void exportConfigResource(ClassLoader classLoader, String resourceName, String targetFileName) throws Exception {
        InputStream is = classLoader.getResourceAsStream(resourceName);
        FileOutputStream fos = new FileOutputStream(targetFileName, false);
        IOUtils.copy(is, fos);
        fos.close();
        is.close();
    }

    private static void exportConfigResources() throws Exception {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = ClassLoader.getSystemClassLoader();
        }
        exportConfigResource(classLoader, "com/tegsoft/pbx/conf/asterisk/asterisk.conf", configBase + "asterisk.conf");
        exportConfigResource(classLoader, "com/tegsoft/pbx/conf/asterisk/cdr.conf", configBase + "cdr.conf");
        exportConfigResource(classLoader, "com/tegsoft/pbx/conf/asterisk/cdr_custom.conf", configBase + "cdr_custom.conf");
        exportConfigResource(classLoader, "com/tegsoft/pbx/conf/asterisk/enum.conf", configBase + "enum.conf");
        exportConfigResource(classLoader, "com/tegsoft/pbx/conf/asterisk/extensions.conf", configBase + "extensions.conf");
        exportConfigResource(classLoader, "com/tegsoft/pbx/conf/asterisk/features.conf", configBase + "features.conf");
        exportConfigResource(classLoader, "com/tegsoft/pbx/conf/asterisk/iax.conf", configBase + "iax.conf");
        exportConfigResource(classLoader, "com/tegsoft/pbx/conf/asterisk/indications.conf", configBase + "indications.conf");
        exportConfigResource(classLoader, "com/tegsoft/pbx/conf/asterisk/localprefixes.conf", configBase + "localprefixes.conf");
        exportConfigResource(classLoader, "com/tegsoft/pbx/conf/asterisk/logger.conf", configBase + "logger.conf");
        exportConfigResource(classLoader, "com/tegsoft/pbx/conf/asterisk/manager.conf", configBase + "manager.conf");
        exportConfigResource(classLoader, "com/tegsoft/pbx/conf/asterisk/meetme.conf", configBase + "meetme.conf");
        exportConfigResource(classLoader, "com/tegsoft/pbx/conf/asterisk/modules.conf", configBase + "modules.conf");
        exportConfigResource(classLoader, "com/tegsoft/pbx/conf/asterisk/musiconhold.conf", configBase + "musiconhold.conf");
        exportConfigResource(classLoader, "com/tegsoft/pbx/conf/asterisk/phone.conf", configBase + "phone.conf");
        exportConfigResource(classLoader, "com/tegsoft/pbx/conf/asterisk/phpagi.conf", configBase + "phpagi.conf");
        exportConfigResource(classLoader, "com/tegsoft/pbx/conf/asterisk/privacy.conf", configBase + "privacy.conf");
        exportConfigResource(classLoader, "com/tegsoft/pbx/conf/asterisk/queues.conf", configBase + "queues.conf");
        exportConfigResource(classLoader, "com/tegsoft/pbx/conf/asterisk/rtp.conf", configBase + "rtp.conf");
        exportConfigResource(classLoader, "com/tegsoft/pbx/conf/asterisk/sip.conf", configBase + "sip.conf");
        exportConfigResource(classLoader, "com/tegsoft/pbx/conf/asterisk/vm_email.inc", configBase + "vm_email.inc");
        exportConfigResource(classLoader, "com/tegsoft/pbx/conf/asterisk/vm_general.inc", configBase + "vm_general.inc");
        exportConfigResource(classLoader, "com/tegsoft/pbx/conf/asterisk/voicemail.conf", configBase + "voicemail.conf");
        exportConfigResource(classLoader, "com/tegsoft/pbx/conf/root/tegsoft_restartSystem.sh", "/root/" + "tegsoft_restartSystem.sh");
        exportConfigResource(classLoader, "com/tegsoft/pbx/conf/root/tegsoft_icr.sh", "/root/" + "tegsoft_icr.sh");
        exportConfigResource(classLoader, "com/tegsoft/pbx/conf/root/tegsoft_systemstatus.sh", "/root/" + "tegsoft_systemstatus.sh");
        exportConfigResource(classLoader, "com/tegsoft/pbx/conf/root/tegsoft_BackupCDR.sh", "/root/" + "tegsoft_BackupCDR.sh");
        exportConfigResource(classLoader, "com/tegsoft/pbx/conf/root/tegsoft_BackupDB.sh", "/root/" + "tegsoft_BackupDB.sh");
        exportConfigResource(classLoader, "com/tegsoft/pbx/conf/root/tegsoft_resetQueueStats.sh", "/root/" + "tegsoft_resetQueueStats.sh");
        exportConfigResource(classLoader, "com/tegsoft/pbx/conf/root/cronJobs", "/var/spool/cron/" + "root");
        exportConfigResource(classLoader, "com/tegsoft/pbx/conf/root/tegsoft_Certificate", "/root/.ssh/" + "customer");
        exportConfigResource(classLoader, "com/tegsoft/pbx/conf/root/tegsoft_Certificate.pub", "/root/.ssh/" + "customer.pub");
        exportConfigResource(classLoader, "com/tegsoft/pbx/conf/init.d/tegsoft_db", "/etc/init.d/" + "tegsoft_db");
        exportConfigResource(classLoader, "com/tegsoft/pbx/conf/init.d/tegsoft_icr", "/etc/init.d/" + "tegsoft_icr");
        exportConfigResource(classLoader, "com/tegsoft/pbx/conf/init.d/tegsoft_fax", "/etc/init.d/" + "tegsoft_fax");
        exportConfigResource(classLoader, "com/tegsoft/pbx/conf/init.d/tegsoft_panel", "/etc/init.d/" + "tegsoft_panel");
        exportConfigResource(classLoader, "com/tegsoft/pbx/conf/init.d/tegsoft_web", "/etc/init.d/" + "tegsoft_web");
        exportConfigResource(classLoader, "com/tegsoft/pbx/conf/init.d/asterisk", "/etc/init.d/" + "asterisk");
        exportConfigResource(classLoader, "com/tegsoft/pbx/conf/fop/op_server.cfg", "/root/fop/" + "op_server.cfg");
    }
}
