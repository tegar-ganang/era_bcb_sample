package com.whitebearsolutions.imagine.wbsagnitio.configuration;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import com.whitebearsolutions.imagine.wbsagnitio.NetworkManager;
import com.whitebearsolutions.imagine.wbsagnitio.ServiceManager;
import com.whitebearsolutions.io.FileLock;
import com.whitebearsolutions.io.FileLockAlreadyLockedException;
import com.whitebearsolutions.security.PasswordHandler;
import com.whitebearsolutions.util.Command;
import com.whitebearsolutions.util.Configuration;

public class SystemConfiguration {

    public static final int BRANCH_USERS = 1;

    public static final int BRANCH_GROUPS = 2;

    public static final int BRANCH_MACHINES = 3;

    public static final int BRANCH_DOMAINS = 4;

    private static final String MAIL_SERVER = "mail.whitebearsolutions.com";

    private static final String SNAPSHOT_FILE = "/var/backups/snapshots/backup.cgz";

    private Configuration _c;

    public SystemConfiguration() throws Exception {
        this._c = new Configuration(new File(WBSAgnitioConfiguration.getConfigurationFile()));
    }

    public SystemConfiguration(Configuration conf) throws Exception {
        this._c = conf;
    }

    public void exportSnapshot() throws Exception {
        FileLock _fl = new FileLock(new File("/tmp/wbsairback-import.lock"));
        try {
            _fl.lock();
            StringBuilder _sb = new StringBuilder();
            _sb.append(WBSAgnitioConfiguration.getConfigurationFile());
            _sb.append("\n");
            _sb.append(WBSAgnitioConfiguration.getOptionalSchemaFile());
            _sb.append("\n");
            if (new File(WBSAgnitioConfiguration.getSchemaObjectFile()).exists()) {
                _sb.append(WBSAgnitioConfiguration.getSchemaObjectFile());
                _sb.append("\n");
            }
            if (new File(WBSAgnitioConfiguration.getHierarchyFile()).exists()) {
                _sb.append(WBSAgnitioConfiguration.getHierarchyFile());
                _sb.append("\n");
            }
            if (new File(WBSAgnitioConfiguration.getIDMDirectory()).exists()) {
                _sb.append(getRecursiveDirectoryList(new File(WBSAgnitioConfiguration.getIDMDirectory())));
            }
            _sb.append("/etc/pki/host.index\n");
            _sb.append("/etc/pki/user.index\n");
            _sb.append("/etc/pki/store\n");
            _sb.append("/etc/pki/certs/wbsagnitio-ca.pem\n");
            _sb.append("/etc/pki/certs/wbsagnitio-ca.der\n");
            _sb.append("/etc/pki/certs/wbsagnitio-ca.crl\n");
            File[] _certs = new File("/etc/pki/certs").listFiles();
            if (_certs != null) {
                for (File _f : _certs) {
                    if (_f.getName().startsWith("wbsagnitio.") && _f.getName().endsWith(".pem")) {
                        _sb.append(_f.getAbsolutePath());
                        _sb.append("\n");
                    }
                }
            }
            _certs = new File("/etc/pki/private").listFiles();
            if (_certs != null) {
                for (File _f : _certs) {
                    if (_f.getName().startsWith("wbsagnitio.") && _f.getName().endsWith(".key")) {
                        _sb.append(_f.getAbsolutePath());
                        _sb.append("\n");
                    }
                }
            }
            _sb.append("/tmp/export.ldif\n");
            FileOutputStream _fos = new FileOutputStream("/var/backups/backup.lst", false);
            _fos.write(_sb.toString().getBytes());
            _fos.close();
            try {
                _sb = new StringBuilder();
                _sb.append("/usr/sbin/slapcat -b ");
                _sb.append(this._c.getProperty("ldap.basedn"));
                _sb.append(" -f /etc/ldap/slapd.conf -l /tmp/export.ldif && /bin/cat /var/backups/backup.lst | /bin/cpio -o | /bin/gzip > ");
                _sb.append(SNAPSHOT_FILE);
                _sb.append(" || rm -f /tmp/export.ldif && rm -f /tmp/export.ldif");
                ServiceManager.fullStop(ServiceManager.LDAP);
                Command.systemCommand(_sb.toString());
            } catch (Exception _ex) {
                throw new Exception("fail to generate snapshot - " + _ex.getMessage());
            } finally {
                ServiceManager.start(ServiceManager.LDAP);
            }
        } catch (Exception _ex) {
            if (_ex instanceof FileLockAlreadyLockedException) {
                throw new Exception("an import process already in progress, please wait to complete before performing any operation");
            } else {
                throw _ex;
            }
        } finally {
            _fl.unlock();
        }
    }

    public String getAdministrativeMail() {
        if (this._c.getProperty("system.mail") != null) {
            return this._c.getProperty("system.mail");
        }
        return null;
    }

    public String getAdministrativeUser() throws Exception {
        if (!this._c.hasProperty("directory.administrator")) {
            this._c.setProperty("directory.administrator", "Administrador");
            this._c.store();
        }
        return this._c.getProperty("directory.administrator");
    }

    public String getAdministrativeGroup(int gidNumber) throws Exception {
        switch(gidNumber) {
            case 1001:
                {
                    if (!this._c.hasProperty("directory.group.users")) {
                        this._c.setProperty("directory.group.users", "Usuarios");
                        this._c.store();
                    }
                    return this._c.getProperty("directory.group.users");
                }
            case 512:
                {
                    if (!this._c.hasProperty("directory.group.domain.administrators")) {
                        this._c.setProperty("directory.group.domain.administrators", "Administradores del dominio");
                        this._c.store();
                    }
                    return this._c.getProperty("directory.group.domain.administrators");
                }
            case 513:
                {
                    if (!this._c.hasProperty("directory.group.domain.users")) {
                        this._c.setProperty("directory.group.domain.users", "Usuarios del dominio");
                        this._c.store();
                    }
                    return this._c.getProperty("directory.group.domain.users");
                }
            case 514:
                {
                    if (!this._c.hasProperty("directory.group.domain.guests")) {
                        this._c.setProperty("directory.group.domain.guests", "Invitados del dominio");
                        this._c.store();
                    }
                    return this._c.getProperty("directory.group.domain.guests");
                }
            case 515:
                {
                    if (!this._c.hasProperty("directory.group.domain.machines")) {
                        this._c.setProperty("directory.group.domain.machines", "Equipos del dominio");
                        this._c.store();
                    }
                    return this._c.getProperty("directory.group.domain.machines");
                }
            default:
                throw new Exception("invalid administrative group number");
        }
    }

    public String getBranchName(int type) throws Exception {
        switch(type) {
            case BRANCH_USERS:
                {
                    if (!this._c.hasProperty("directory.branch.users")) {
                        this._c.setProperty("directory.branch.users", "personas");
                        this._c.store();
                    }
                    return this._c.getProperty("directory.branch.users");
                }
            case BRANCH_GROUPS:
                {
                    if (!this._c.hasProperty("directory.branch.groups")) {
                        this._c.setProperty("directory.branch.groups", "grupos");
                        this._c.store();
                    }
                    return this._c.getProperty("directory.branch.groups");
                }
            case BRANCH_MACHINES:
                {
                    if (!this._c.hasProperty("directory.branch.machines")) {
                        this._c.setProperty("directory.branch.machines", "equipos");
                        this._c.store();
                    }
                    return this._c.getProperty("directory.branch.machines");
                }
            case BRANCH_DOMAINS:
                {
                    if (!this._c.hasProperty("directory.branch.domains")) {
                        this._c.setProperty("directory.branch.domains", "dominios");
                        this._c.store();
                    }
                    return this._c.getProperty("directory.branch.domains");
                }
            default:
                throw new Exception("invalid branch type");
        }
    }

    public String getDomain() throws Exception {
        return new NetworkManager(this._c).getDomain();
    }

    public static int getCPULoad() {
        int _percent = -1;
        List<Integer> _old_stat, _new_stat;
        try {
            _old_stat = getCPUStats();
            Thread.sleep(1000);
            _new_stat = getCPUStats();
            for (int i = 0; i < _old_stat.size(); i++) {
                _old_stat.set(i, _new_stat.get(i) - _old_stat.get(i));
                _percent += _old_stat.get(i);
            }
            _percent = 100 - ((_old_stat.get(3) * 100) / _percent);
        } catch (InterruptedException _ex) {
        }
        return _percent;
    }

    public static String getDatabaseInfo() {
        try {
            return Command.systemCommand("/usr/bin/db4.2_stat -m -h /var/lib/ldap | head -n 16");
        } catch (Exception _ex) {
            return _ex.getMessage();
        }
    }

    public static Map<String, Integer> getDiskLoad() throws Exception {
        Map<String, Integer> partitions = new HashMap<String, Integer>();
        try {
            String _output = Command.systemCommand("df");
            if (_output != null && !_output.isEmpty()) {
                List<String> devices = new ArrayList<String>();
                File _f = new File("/proc/partitions");
                if (_f.exists()) {
                    FileInputStream _fis = null;
                    try {
                        _fis = new FileInputStream(_f);
                        for (String _line = readLine(_fis); _line != null; _line = readLine(_fis)) {
                            if (_line.matches("\\ +[0-9]+\\ +[0-9]+\\ +[0-9]+\\ +[a-zA-Z0-9]+")) {
                                StringTokenizer _st = new StringTokenizer(_line.trim());
                                for (int i = 3; i > 0; i--) {
                                    _st.nextToken();
                                }
                                devices.add(_st.nextToken());
                            }
                        }
                    } finally {
                        if (_fis != null) {
                            _fis.close();
                        }
                    }
                }
                for (String _line : _output.split("\n")) {
                    for (String device : devices) {
                        if (_line.contains(device)) {
                            StringTokenizer _st = new StringTokenizer(_line, " ");
                            String _partition = _st.nextToken();
                            for (int i = 3; i > 0; i--) {
                                _st.nextToken();
                            }
                            try {
                                partitions.put(_partition, Integer.parseInt(_st.nextToken().replace("%", "")));
                            } catch (NumberFormatException _ex) {
                                partitions.put(_partition, 0);
                            }
                        }
                    }
                }
            }
        } catch (Exception _ex) {
            if (_ex.getMessage() == null || _ex.getMessage().trim().isEmpty()) {
                throw new Exception("unknown error while reading system partitions");
            } else {
                throw new Exception("read partition error - " + _ex.getMessage());
            }
        }
        return partitions;
    }

    public String getMailServer() {
        if (this._c.getProperty("mail.host") != null) {
            return this._c.getProperty("mail.host");
        }
        return MAIL_SERVER;
    }

    public static int getMemoryLoad() {
        int memory_total = 0, memory_free = 0;
        File _proc = new File("/proc/meminfo");
        if (_proc.exists()) {
            FileInputStream _fis = null;
            try {
                _fis = new FileInputStream(_proc);
                for (String _line = readLine(_fis); _line != null; _line = readLine(_fis)) {
                    if (_line.contains("MemTotal")) {
                        memory_total = Integer.parseInt(_line.substring(9, _line.lastIndexOf(" ")).trim());
                    } else if (_line.contains("MemFree")) {
                        memory_free = Integer.parseInt(_line.substring(8, _line.lastIndexOf(" ")).trim());
                    }
                }
                return 100 - ((memory_free * 100) / memory_total);
            } catch (Exception _ex) {
            } finally {
                if (_fis != null) {
                    try {
                        _fis.close();
                    } catch (IOException _ex) {
                    }
                }
            }
        }
        return -1;
    }

    public byte[] getSnapshot() throws Exception {
        File _f = new File(SNAPSHOT_FILE);
        if (!_f.exists()) {
            return new byte[0];
        }
        ByteArrayOutputStream _baos = new ByteArrayOutputStream();
        FileInputStream _fis = new FileInputStream(_f);
        try {
            while (_fis.available() > 0) {
                _baos.write(_fis.read());
            }
        } finally {
            _fis.close();
        }
        return _baos.toByteArray();
    }

    public static String getVersion() {
        try {
            return Command.systemCommand("/usr/bin/dpkg -l | grep wbsagnitio-admin | awk '{print $3}'");
        } catch (Exception _ex) {
            return "unknown";
        }
    }

    public void importSnapshot(byte[] data) throws Exception {
        if (data == null || data.length == 0) {
            return;
        }
        FileOutputStream _fos = new FileOutputStream(SNAPSHOT_FILE, false);
        try {
            _fos.write(data);
        } finally {
            _fos.close();
        }
        StringBuilder _sb = new StringBuilder();
        _sb.append("/bin/zcat ");
        _sb.append(SNAPSHOT_FILE);
        _sb.append(" | /bin/cpio -idu");
        Command.systemCommand(_sb.toString());
        ServiceManager.fullStop(ServiceManager.LDAP);
        Command.systemCommand("/bin/rm -fr /var/lib/ldap/*");
        Command.systemCommand("/bin/mkdir /var/lib/ldap/accesslog && chown openldap:openldap /var/lib/ldap/accesslog");
        Command.systemCommand("/bin/su -s /bin/bash -c \"/usr/sbin/slapadd -f /etc/ldap/slapd.conf -l /tmp/export.ldif\" -l openldap && rm -f /tmp/export.ldif");
        HAConfiguration.update();
        ServiceManager.restart(ServiceManager.KERBEROS);
        ServiceManager.restart(ServiceManager.RADIUS);
        if (ServiceManager.isRunning(ServiceManager.SMB)) {
            ServiceManager.restart(ServiceManager.SMB);
        }
    }

    public void setAdministrativeMail(String mail) throws Exception {
        if (mail != null && !mail.isEmpty()) {
            this._c.setProperty("system.mail", mail);
        } else if (this._c.getProperty("system.mail") != null) {
            this._c.removeProperty("system.mail");
        }
        this._c.store();
    }

    public void setMailServer(String server) throws Exception {
        if (server != null && !server.isEmpty()) {
            this._c.setProperty("mail.host", server);
        } else if (this._c.getProperty("mail.host") != null) {
            this._c.removeProperty("mail.host");
        }
        this._c.store();
    }

    public void setRootPassword(String password) throws Exception {
        if (password != null && !password.isEmpty()) {
            this._c.setProperty("system.password", PasswordHandler.generateDigest(password, null, "MD5"));
            this._c.store();
        }
    }

    private static List<Integer> getCPUStats() {
        File _proc = new File("/proc/stat");
        if (_proc.exists()) {
            FileInputStream _fis = null;
            try {
                _fis = new FileInputStream(_proc);
                for (String _line = readLine(_fis); _line != null; _line = readLine(_fis)) {
                    if (_line.contains("cpu ")) {
                        List<Integer> cpu_stat = new ArrayList<Integer>();
                        StringTokenizer _st = new StringTokenizer(_line.replace("cpu ", ""));
                        for (int i = 0; i < 4; i++) {
                            cpu_stat.add(Integer.parseInt(_st.nextToken()));
                        }
                        return cpu_stat;
                    }
                }
            } catch (Exception _ex) {
            } finally {
                if (_fis != null) {
                    try {
                        _fis.close();
                    } catch (IOException _ex) {
                    }
                }
            }
        }
        return new ArrayList<Integer>();
    }

    private String getRecursiveDirectoryList(File _directory) {
        StringBuilder _sb = new StringBuilder();
        if (_directory != null && _directory.isDirectory()) {
            File[] _files = _directory.listFiles();
            for (File _f : _files) {
                if (_f.isFile()) {
                    _sb.append(_f.getAbsolutePath());
                    _sb.append("\n");
                } else if (_f.isDirectory()) {
                    _sb.append(getRecursiveDirectoryList(_f));
                }
            }
        }
        return _sb.toString();
    }

    private static String readLine(InputStream is) throws IOException {
        if (is == null) {
            return null;
        }
        int i;
        StringBuilder _sb = new StringBuilder();
        for (i = is.read(); i != -1; i = is.read()) {
            if (i == 13) {
                continue;
            }
            if (i == 10) {
                break;
            }
            _sb.append((char) i);
        }
        if (i == -1) {
            return null;
        }
        return _sb.toString();
    }
}
