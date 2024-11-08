package com.whitebearsolutions.imagine.wbsagnitio.tool;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.whitebearsolutions.directory.Entry;
import com.whitebearsolutions.imagine.wbsagnitio.NetworkManager;
import com.whitebearsolutions.imagine.wbsagnitio.ServiceManager;
import com.whitebearsolutions.imagine.wbsagnitio.configuration.HAConfiguration;
import com.whitebearsolutions.imagine.wbsagnitio.configuration.SystemConfiguration;
import com.whitebearsolutions.imagine.wbsagnitio.configuration.WBSAgnitioConfiguration;
import com.whitebearsolutions.imagine.wbsagnitio.directory.AttributeSet;
import com.whitebearsolutions.imagine.wbsagnitio.directory.GroupManager;
import com.whitebearsolutions.imagine.wbsagnitio.directory.UserManager;
import com.whitebearsolutions.imagine.wbsagnitio.idm.ProfileManager;
import com.whitebearsolutions.imagine.wbsagnitio.idm.RuleEngine;
import com.whitebearsolutions.imagine.wbsagnitio.idm.RuleManager;
import com.whitebearsolutions.imagine.wbsagnitio.idm.listener.RepositoryListener;
import com.whitebearsolutions.imagine.wbsagnitio.idm.repository.ExternalRepository;
import com.whitebearsolutions.imagine.wbsagnitio.idm.repository.RepositoryTask;
import com.whitebearsolutions.imagine.wbsagnitio.net.WatchdogComm;
import com.whitebearsolutions.imagine.wbsagnitio.service.LicenseManager;
import com.whitebearsolutions.imagine.wbsagnitio.transport.CustomTransport;
import com.whitebearsolutions.io.FileSystemFile;
import com.whitebearsolutions.io.FileUtils;
import com.whitebearsolutions.security.FileSummation;
import com.whitebearsolutions.util.Command;
import com.whitebearsolutions.util.Configuration;

public class Watchdog {

    private static final int RECOVERY_GENERAL = 2569835;

    private static final int RECOVERY_SYSTEM = 4284716;

    private static final int RECOVERY_SERVICE = 2290873;

    private static final int ERROR_GENERAL = 4234457;

    private static final int ERROR_SYSTEM = 9864124;

    private static final int ERROR_SERVICE = 5738633;

    private static final int SYSTEM_MEMORY = 8917389;

    private static final int SYSTEM_DISK = 9163191;

    private static final int SYSTEM_SNAPSHOT = 4761831;

    private static final int SYSTEM_HA_BALANCER = 4908274;

    private static final int SYSTEM_HA_SYNCHRONIZATION = 4876131;

    private static final int SYSTEM_IDM_LISTENER = 6876283;

    private static final int SYSTEM_IDM_LINK = 6876284;

    private static final int SYSTEM_LICENSE = 98790257;

    private static final int REPORT_MINIMUM_PERIOD = 43200000;

    protected static final String SUPPORT_MAIL = "soporte@whitebearsolutions.com";

    private Map<Integer, String> services;

    private Map<Integer, Calendar> service_alerts;

    private Map<Integer, Calendar> system_alerts;

    private SystemConfiguration _sc;

    private Configuration _c;

    public Watchdog() throws Exception {
        this._c = new Configuration(new File(WBSAgnitioConfiguration.getConfigurationFile()));
        this._sc = new SystemConfiguration();
        this.service_alerts = new HashMap<Integer, Calendar>();
        this.system_alerts = new HashMap<Integer, Calendar>();
        this.services = new HashMap<Integer, String>();
        this.services.put(ServiceManager.LDAP, "LDAP");
        this.services.put(ServiceManager.DNS, "DNS");
        this.services.put(ServiceManager.KERBEROS, "KERBEROS");
        this.services.put(ServiceManager.RADIUS, "RADIUS");
        this.services.put(ServiceManager.NTP, "NTP");
        this.services.put(ServiceManager.WGUI, "ADMIN-WEB-GUI");
        this.services.put(ServiceManager.SSH, "SSH");
        this.services.put(ServiceManager.SNMP, "SNMP");
        for (Thread.sleep(60000); true; Thread.sleep(120000)) {
            Calendar _cal = Calendar.getInstance();
            if ((_cal.get(Calendar.MINUTE) == 15 || _cal.get(Calendar.MINUTE) == 16) && _cal.get(Calendar.HOUR_OF_DAY) == 3) {
                try {
                    ServiceManager.fullStop(ServiceManager.LDAP, 200);
                    this._sc.exportSnapshot();
                    unregisterError(ERROR_SYSTEM, SYSTEM_SNAPSHOT);
                } catch (Exception _ex) {
                    registerError(ERROR_SYSTEM, SYSTEM_SNAPSHOT, _ex.getMessage());
                }
                logRotate();
                monitorLicenses();
            } else if (_cal.get(Calendar.MINUTE) == 5 || _cal.get(Calendar.MINUTE) == 6 || _cal.get(Calendar.MINUTE) == 15 || _cal.get(Calendar.MINUTE) == 16 || _cal.get(Calendar.MINUTE) == 25 || _cal.get(Calendar.MINUTE) == 26 || _cal.get(Calendar.MINUTE) == 35 || _cal.get(Calendar.MINUTE) == 36 || _cal.get(Calendar.MINUTE) == 45 || _cal.get(Calendar.MINUTE) == 46 || _cal.get(Calendar.MINUTE) == 55 || _cal.get(Calendar.MINUTE) == 56) {
                replicate();
            }
            monitorSystem();
            monitorServices();
            monitorListeners();
            executeBackgroundtasks();
        }
    }

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        try {
            new Watchdog();
        } catch (Exception _ex) {
            System.out.println("Error: " + _ex.getMessage());
        }
    }

    private void replicate() {
        try {
            if ("master".equals(HAConfiguration.getStatus())) {
                if (!this._c.hasProperty("directory.remote")) {
                    throw new Exception("remote slave directory is not in configuration");
                }
                StringBuilder _sb = new StringBuilder();
                _sb.append("/usr/bin/scp -i /var/replica/.ssh/id_dsa -o StrictHostKeyChecking=no -r /etc/pki ");
                if (new File(WBSAgnitioConfiguration.getBindConfigurationFile()).exists()) {
                    _sb.append(WBSAgnitioConfiguration.getBindConfigurationFile());
                    _sb.append(" ");
                }
                if (new File(WBSAgnitioConfiguration.getOptionalSchemaFile()).exists()) {
                    _sb.append(WBSAgnitioConfiguration.getOptionalSchemaFile());
                    _sb.append(" ");
                }
                if (new File(WBSAgnitioConfiguration.getSchemaObjectFile()).exists()) {
                    _sb.append(WBSAgnitioConfiguration.getSchemaObjectFile());
                    _sb.append(" ");
                }
                if (new File(WBSAgnitioConfiguration.getHierarchyFile()).exists()) {
                    _sb.append(WBSAgnitioConfiguration.getHierarchyFile());
                    _sb.append(" ");
                }
                if (new File(WBSAgnitioConfiguration.getIDMDirectory()).exists()) {
                    _sb.append(WBSAgnitioConfiguration.getIDMDirectory());
                    _sb.append(" ");
                }
                _sb.append("replica@");
                _sb.append(this._c.getProperty("directory.remote"));
                _sb.append(":/var/replica/");
                Command.systemCommand(_sb.toString());
            } else if ("slave".equals(HAConfiguration.getStatus())) {
                File _f = new File("/var/replica/pki");
                if (_f.exists()) {
                    Command.systemCommand("/bin/cp -fr /var/replica/pki /etc/");
                }
                _f = new File("/var/replica/idm");
                if (_f.exists()) {
                    Command.systemCommand("/bin/cp -fr /var/replica/idm /etc/wbsagnitio-admin/");
                }
                _f = new File("/var/replica/hierarchy.xml");
                if (_f.exists()) {
                    Command.systemCommand("/bin/mv -f /var/replica/hierarchy.xml /etc/wbsagnitio-admin/");
                }
                Command.systemCommand("/bin/mv -f /var/replica/*.xml /opt/imagine/");
                _f = new File("/var/replica/optional.schema");
                if (_f.exists()) {
                    if (!FileSummation.compareMD5Summation(_f, new File("/etc/ldap/schema/optional.schema"))) {
                        Command.systemCommand("/bin/mv /var/replica/optional.schema /etc/ldap/schema/");
                        ServiceManager.restart(ServiceManager.LDAP);
                    }
                }
                _f = new File("/var/replica/named.conf");
                if (_f.exists()) {
                    if (!FileSummation.compareMD5Summation(_f, new File("/etc/bind/named.conf"))) {
                        Command.systemCommand("/bin/mv /var/replica/named.conf /etc/bind/");
                        ServiceManager.restart(ServiceManager.DNS);
                    }
                }
            }
            unregisterError(ERROR_SYSTEM, SYSTEM_HA_SYNCHRONIZATION);
        } catch (Exception _ex) {
            String _message = "unknown HA synchronization error";
            if (_ex.getMessage() != null) {
                _message = _ex.getMessage();
            }
            registerError(ERROR_SYSTEM, SYSTEM_HA_SYNCHRONIZATION, _message);
        }
    }

    private void executeBackgroundtasks() throws Exception {
        if ("slave".equals(HAConfiguration.getStatus())) {
            return;
        }
        boolean _background_error = false;
        RuleManager _rm = new RuleManager();
        ProfileManager _pm = new ProfileManager();
        StringBuilder _error_text = new StringBuilder();
        for (String _uuid : RepositoryTask.getRepositoryTasks()) {
            boolean _task_error = false;
            if (_error_text.length() > 0) {
                _error_text.append("\n\n");
            }
            _error_text.append("Background task [");
            _error_text.append(_uuid);
            _error_text.append("] failed");
            try {
                RepositoryTask _rt = new RepositoryTask(_uuid);
                List<String> _unaffected_repositories = _rm.getBackgroundRepositoryLinks(_rt.getAffectedBranches(), _rt.getUnaffectedRoles());
                for (String _repository : _rt.getRepositories()) {
                    try {
                        ExternalRepository _er = ExternalRepository.getInstance(_repository);
                        switch(_rt.getModificationType()) {
                            case RuleEngine.USER_ADD:
                                {
                                    AttributeSet _attributes = _rt.getAttributes();
                                    if (_attributes != null && _attributes.hasAttribute("uid")) {
                                        _er.addUserEntry(_attributes.getAttributeFirstStringValue("uid"), _attributes);
                                        for (String _role : _rt.getAffectedRoles()) {
                                            for (String _profile : _pm.getProfiles(_repository, _role)) {
                                                _er.storeProfile(_attributes.getAttributeFirstStringValue("uid"), _profile);
                                            }
                                        }
                                    }
                                }
                                break;
                            case RuleEngine.USER_UPDATE:
                                {
                                    AttributeSet _attributes = _rt.getAttributes();
                                    if (_attributes != null && _attributes.hasAttribute("uid")) {
                                        _er.storeUserEntry(_attributes.getAttributeFirstStringValue("uid"), _attributes);
                                        for (String _role : _rt.getAffectedRoles()) {
                                            for (String _profile : _pm.getProfiles(_repository, _role)) {
                                                _er.storeProfile(_attributes.getAttributeFirstStringValue("uid"), _profile);
                                            }
                                        }
                                    }
                                }
                                break;
                            case RuleEngine.USER_REMOVE:
                                {
                                    AttributeSet _attributes = _rt.getAttributes();
                                    if (_attributes != null && _attributes.hasAttribute("uid")) {
                                        for (String _role : _rt.getAffectedRoles()) {
                                            for (String _profile : _pm.getProfiles(_repository, _role)) {
                                                _er.deleteProfile(_attributes.getAttributeFirstStringValue("uid"), _profile);
                                            }
                                        }
                                        _er.deleteUserEntry(_attributes.getAttributeFirstStringValue("uid"), _attributes);
                                    }
                                }
                                break;
                            case RuleEngine.GROUP_ADD:
                                {
                                    AttributeSet _attributes = _rt.getAttributes();
                                    if (_attributes != null && _attributes.hasAttribute("cn")) {
                                        _er.addGroupEntry(_attributes.getAttributeFirstStringValue("cn"), _attributes);
                                    }
                                }
                                break;
                            case RuleEngine.GROUP_UPDATE:
                                {
                                    AttributeSet _attributes = _rt.getAttributes();
                                    if (_attributes != null && _attributes.hasAttribute("cn")) {
                                        _er.storeGroupEntry(_attributes.getAttributeFirstStringValue("cn"), _attributes);
                                    }
                                }
                                break;
                            case RuleEngine.GROUP_REMOVE:
                                {
                                    AttributeSet _attributes = _rt.getAttributes();
                                    if (_attributes != null && _attributes.hasAttribute("cn")) {
                                        _er.deleteGroupEntry(_attributes.getAttributeFirstStringValue("cn"), _attributes);
                                    }
                                }
                                break;
                            case RuleEngine.GROUP_USER_ADD:
                                {
                                    Map<String, Entry> _membership = _rt.getMembership();
                                    Entry _group = _membership.get("group");
                                    Entry _member = _membership.get("member");
                                    AttributeSet _attributes = new AttributeSet(_member);
                                    if (GroupManager.isGroup(_group) && UserManager.isUser(_member)) {
                                        _er.storeUserEntry(_attributes.getAttributeFirstStringValue("uid"), _attributes);
                                        List<String> _unaffected_profiles = _pm.getProfiles(_repository, _rt.getUnaffectedRoles());
                                        for (String _role : _rt.getAffectedRoles()) {
                                            for (String _profile : _pm.getProfiles(_repository, _role)) {
                                                if (!_unaffected_profiles.contains(_profile)) {
                                                    _er.storeProfile(_attributes.getAttributeFirstStringValue("uid"), _profile);
                                                }
                                            }
                                        }
                                    }
                                }
                                break;
                            case RuleEngine.GROUP_USER_REMOVE:
                                {
                                    Map<String, Entry> _membership = _rt.getMembership();
                                    Entry _group = _membership.get("group");
                                    Entry _member = _membership.get("member");
                                    AttributeSet _attributes = new AttributeSet(_member);
                                    if (GroupManager.isGroup(_group) && UserManager.isUser(_member)) {
                                        List<String> _unaffected_profiles = _pm.getProfiles(_repository, _rt.getUnaffectedRoles());
                                        for (String _role : _rt.getAffectedRoles()) {
                                            for (String _profile : _pm.getProfiles(_repository, _role)) {
                                                if (!_unaffected_profiles.contains(_profile)) {
                                                    _er.deleteProfile(_attributes.getAttributeFirstStringValue("uid"), _profile);
                                                }
                                            }
                                        }
                                        _er.removeUserMember(String.valueOf(_group.getAttribute("cn")[0]), _attributes.getAttributeFirstStringValue("uid"));
                                        if (!_unaffected_repositories.contains(_repository)) {
                                            _er.deleteUserEntry(_attributes.getAttributeFirstStringValue("uid"), _attributes);
                                        }
                                    }
                                }
                                break;
                            case RuleEngine.GROUP_GROUP_ADD:
                                {
                                    Map<String, Entry> _membership = _rt.getMembership();
                                    Entry _group = _membership.get("group");
                                    Entry _member = _membership.get("member");
                                    if (GroupManager.isGroup(_group) && GroupManager.isGroup(_member)) {
                                    }
                                }
                                break;
                            case RuleEngine.GROUP_GROUP_REMOVE:
                                {
                                    Map<String, Entry> _membership = _rt.getMembership();
                                    Entry _group = _membership.get("group");
                                    Entry _member = _membership.get("member");
                                    if (GroupManager.isGroup(_group) && GroupManager.isGroup(_member)) {
                                    }
                                }
                                break;
                        }
                        _rt.removeRepository(_repository);
                    } catch (Exception _ex) {
                        _background_error = true;
                        _task_error = true;
                        _error_text.append("\n Repository link [");
                        _error_text.append(_repository);
                        _error_text.append("] error - ");
                        if (_ex.getMessage() != null) {
                            _error_text.append(_ex.getMessage());
                        } else if (!(_ex instanceof NullPointerException)) {
                            _error_text.append(_ex.getClass().toString().substring(_ex.getClass().toString().lastIndexOf(".") + 1));
                        } else {
                            _error_text.append("Unknown error (please check connection)");
                        }
                    }
                }
                if (!_task_error) {
                    _rt.remove();
                }
            } catch (Exception _ex) {
                _background_error = true;
                _error_text.append(" - ");
                if (_ex.getMessage() != null) {
                    _error_text.append(_ex.getMessage());
                } else if (!(_ex instanceof NullPointerException)) {
                    _error_text.append(_ex.getClass().toString());
                } else {
                    _error_text.append("Unknown error");
                }
            }
        }
        if (_background_error) {
            registerError(ERROR_SYSTEM, SYSTEM_IDM_LINK, _error_text.toString());
        } else {
            unregisterError(ERROR_SYSTEM, SYSTEM_IDM_LINK);
        }
    }

    private void logRotate() {
        try {
            if ("slave".equals(HAConfiguration.getStatus())) {
                return;
            }
            RuleManager _rm = new RuleManager();
            for (Map<String, String> _listener : _rm.getAllListeners()) {
                File _log = RepositoryListener.getListenerFile(_listener.get("repository"), "log");
                if (_log.exists()) {
                    if (new FileSystemFile(_log).getSize() > 10000) {
                        for (int i = 1; i <= 5; i++) {
                            File _f = new File(_log.getAbsolutePath() + "." + i + ".gz");
                            if (_f.exists()) {
                                FileUtils.copyFile(_f, new File(_log.getAbsolutePath() + "." + (i + 1) + ".gz"));
                            }
                        }
                        FileUtils.copyFile(_log, new File(_log.getAbsolutePath() + ".1"));
                        FileUtils.gzip(new File(_log.getAbsolutePath() + ".1"));
                        FileUtils.empty(_log);
                    }
                }
            }
        } catch (Exception _ex) {
        }
    }

    private void monitorLicenses() {
        boolean _error = false;
        try {
            Calendar _now = Calendar.getInstance();
            LicenseManager _lm = new LicenseManager();
            for (Map<String, String> _license : _lm.getLicenses()) {
                Calendar _expiration = Calendar.getInstance();
                _expiration.setTime(LicenseManager.expirationToDate(_license.get("expiration")));
                if ((_expiration.getTimeInMillis() - _now.getTimeInMillis()) < (60 * 24 * 60 * 60 * 1000L)) {
                    StringBuilder _sb = new StringBuilder();
                    _sb.append("service code ");
                    _sb.append(_license.get("code"));
                    _sb.append(" is about to expire on ");
                    _sb.append(_license.get("expiration"));
                    registerError(ERROR_SYSTEM, SYSTEM_LICENSE, _sb.toString());
                    _error = true;
                }
            }
            if (!_error) {
                unregisterError(ERROR_SYSTEM, SYSTEM_LICENSE);
            }
        } catch (Exception _ex) {
        }
    }

    private void monitorListeners() {
        WatchdogComm _wdc = new WatchdogComm();
        try {
            boolean _error = false;
            if (!"slave".equals(HAConfiguration.getStatus())) {
                for (Map<String, String> _listener : _wdc.getListeners()) {
                    if ("true".equalsIgnoreCase(_listener.get("active")) && !"true".equalsIgnoreCase(_listener.get("running"))) {
                        try {
                            _wdc.runRemoteListener(_listener.get("repository"));
                        } catch (Exception _ex) {
                        }
                        StringBuilder _sb = new StringBuilder();
                        _sb.append("listener [");
                        _sb.append(_listener.get("repository"));
                        _sb.append("] is marked as active but is not running");
                        registerError(ERROR_SYSTEM, SYSTEM_IDM_LISTENER, _sb.toString());
                        _error = true;
                    }
                }
            }
            if (!_error) {
                unregisterError(ERROR_SYSTEM, SYSTEM_IDM_LISTENER);
            }
        } catch (Exception _ex) {
            registerError(ERROR_SYSTEM, SYSTEM_IDM_LISTENER, "cannot check listeners - " + _ex.getMessage());
        }
    }

    private void monitorSystem() {
        int _value = SystemConfiguration.getMemoryLoad();
        if (_value > 90) {
            StringBuilder _sb = new StringBuilder();
            _sb.append("Maximum memory rate achieved (");
            _sb.append(_value);
            _sb.append(" %)");
            registerError(ERROR_SYSTEM, SYSTEM_MEMORY, _sb.toString());
        } else {
            unregisterError(ERROR_SYSTEM, SYSTEM_MEMORY);
        }
        try {
            Map<String, Integer> partitions = SystemConfiguration.getDiskLoad();
            for (String partition : partitions.keySet()) {
                if (partitions.get(partition) > 90) {
                    StringBuilder _sb = new StringBuilder();
                    _sb.append("Maximum disk rate achieved (");
                    _sb.append(partition);
                    _sb.append(" - ");
                    _sb.append(partitions.get(partition));
                    _sb.append(" %)");
                    registerError(ERROR_SYSTEM, SYSTEM_DISK, _sb.toString());
                } else {
                    unregisterError(ERROR_SYSTEM, SYSTEM_DISK);
                }
            }
        } catch (Exception _ex) {
            registerError(ERROR_GENERAL, SYSTEM_DISK, _ex.getMessage());
        }
        try {
            if ("slave".equals(HAConfiguration.getStatus())) {
                if (!"enabled".equals(Command.systemCommand("/usr/share/wbsagnitio/tools/check_iptables"))) {
                    Command.systemCommand("/etc/ha.d/resource.d/LVS-DR stop");
                    registerError(ERROR_SYSTEM, SYSTEM_HA_BALANCER, "slave ARP rules disabled");
                } else {
                    unregisterError(ERROR_SYSTEM, SYSTEM_HA_BALANCER);
                }
            }
        } catch (Exception _ex) {
            registerError(ERROR_GENERAL, SYSTEM_HA_BALANCER, _ex.getMessage());
        }
    }

    private void monitorServices() {
        try {
            String _status = HAConfiguration.getStatus();
            if (_status.equals("master") || _status.equals("slave")) {
                this.services.put(ServiceManager.CLUSTER, "CLUSTER");
            }
        } catch (Exception _ex) {
        }
        for (int service : this.services.keySet()) {
            try {
                if (ServiceManager.isRunning(service)) {
                    unregisterError(ERROR_SERVICE, service);
                } else {
                    registerError(ERROR_SERVICE, service, null);
                    try {
                        ServiceManager.restart(service);
                    } catch (Exception _ex) {
                    }
                }
            } catch (Exception _ex) {
                registerError(ERROR_GENERAL, service, _ex.getMessage());
            }
        }
    }

    private void registerError(int type, int subtype, String message) {
        if (type == ERROR_SERVICE) {
            if (this.service_alerts.containsKey(subtype)) {
                Calendar _cal = this.service_alerts.get(subtype);
                if ((Calendar.getInstance().getTimeInMillis() - _cal.getTimeInMillis()) < REPORT_MINIMUM_PERIOD) {
                    return;
                } else {
                    this.service_alerts.remove(subtype);
                    this.service_alerts.put(subtype, Calendar.getInstance());
                }
            } else {
                this.service_alerts.put(subtype, Calendar.getInstance());
            }
        } else if (type == ERROR_SYSTEM) {
            if (this.system_alerts.containsKey(subtype)) {
                Calendar _cal = this.system_alerts.get(subtype);
                if ((Calendar.getInstance().getTimeInMillis() - _cal.getTimeInMillis()) < REPORT_MINIMUM_PERIOD) {
                    return;
                } else {
                    this.system_alerts.remove(subtype);
                    this.system_alerts.put(subtype, Calendar.getInstance());
                }
            } else {
                this.system_alerts.put(subtype, Calendar.getInstance());
            }
        }
        StringBuilder _sb = new StringBuilder();
        _sb.append("/tmp/");
        _sb.append((type * 1000) + subtype);
        _sb.append(".wderror");
        File _f = new File(_sb.toString());
        try {
            if (!_f.exists() && _f.createNewFile()) {
                try {
                    sendMail(type, subtype, message);
                } catch (Exception _ex) {
                    _f.delete();
                }
            }
        } catch (Exception _ex) {
        }
    }

    private void unregisterError(int type, int subtype) {
        StringBuilder _sb = new StringBuilder();
        _sb.append("/tmp/");
        _sb.append((type * 1000) + subtype);
        _sb.append(".wderror");
        File _f = new File(_sb.toString());
        try {
            switch(type) {
                case ERROR_GENERAL:
                    type = RECOVERY_GENERAL;
                    break;
                case ERROR_SERVICE:
                    type = RECOVERY_SERVICE;
                    break;
                case ERROR_SYSTEM:
                    type = RECOVERY_SYSTEM;
                    break;
            }
            if (_f.exists() && _f.delete()) {
                try {
                    sendMail(type, subtype, null);
                } catch (Exception _ex) {
                }
            }
        } catch (Exception _ex) {
        }
    }

    private void sendMail(int type, int subtype, String text) throws Exception {
        LicenseManager _lm = new LicenseManager();
        if (this._sc.getAdministrativeMail() == null && !_lm.hasLicenseType("SUPPORT")) {
            return;
        }
        CustomTransport _mt = new CustomTransport();
        Map<String, String> _fields = new HashMap<String, String>();
        StringBuilder _title = new StringBuilder();
        _title.append("WATCHDOG ");
        if (type == ERROR_GENERAL || type == ERROR_SERVICE || type == ERROR_SYSTEM) {
            _title.append("ERROR");
        } else {
            _title.append("RECOVERY");
        }
        _title.append(" REPORT");
        if (type == ERROR_SERVICE || type == RECOVERY_SERVICE) {
            if (this.services.containsKey(subtype)) {
                _title.append(this.services.get(subtype));
                _title.append(" ");
            } else {
                _title.append("UNKNOWN ");
            }
            _title.append("service ");
            if (type == ERROR_SERVICE) {
                _title.append("error");
            } else {
                _title.append("recovery");
            }
        } else if (type == ERROR_SYSTEM || type == RECOVERY_SYSTEM) {
            switch(subtype) {
                case SYSTEM_MEMORY:
                    {
                        _title.append("MEMORY ");
                        _title.append("use percent ");
                    }
                    break;
                case SYSTEM_DISK:
                    {
                        _title.append("DISK ");
                        _title.append("use percent ");
                    }
                    break;
                case SYSTEM_SNAPSHOT:
                    {
                        _title.append("BACKUP-SNAPSHOT ");
                    }
                    break;
                case SYSTEM_LICENSE:
                    {
                        _title.append("LICENSE ");
                    }
                    break;
                case SYSTEM_HA_BALANCER:
                    {
                        _title.append("HA-BALANCER ");
                    }
                    break;
                case SYSTEM_HA_SYNCHRONIZATION:
                    {
                        _title.append("HA-SYNCHRONIZATION ");
                    }
                    break;
                case SYSTEM_IDM_LISTENER:
                    {
                        _title.append("IDM-LISTENER ");
                    }
                    break;
                case SYSTEM_IDM_LINK:
                    {
                        _title.append("IDM-LINK ");
                    }
                    break;
                default:
                    {
                        _title.append("UNKNOWN(");
                        _title.append(subtype);
                        _title.append(") ");
                    }
                    break;
            }
            if (type == ERROR_SYSTEM) {
                _title.append("error");
            } else {
                _title.append("recovery");
            }
        } else {
            _title.append("general ");
            if (type == ERROR_GENERAL) {
                _title.append("error");
            } else {
                _title.append("recovery");
            }
        }
        if (!_lm.isRegistered()) {
            _fields.put("UUID", _lm.getUnitUUID() + " (unregistered)");
        } else {
            _fields.put("UUID", _lm.getUnitUUID());
        }
        _fields.put("Directory base", this._c.getProperty("ldap.basedn"));
        _fields.put("Error", _title.toString());
        _fields.put("Error description", text.replace("\n", "<br/>"));
        _fields.put("Memory", SystemConfiguration.getMemoryLoad() + " %");
        try {
            StringBuilder _sb = new StringBuilder();
            Map<String, Integer> partitions = SystemConfiguration.getDiskLoad();
            for (String partition : partitions.keySet()) {
                _sb.append(partition);
                _sb.append(" (");
                _sb.append(partitions.get(partition));
                _sb.append(" %)<br/>");
            }
            _fields.put("Partitions", _sb.toString());
        } catch (Exception _ex) {
        }
        StringBuilder _sb = new StringBuilder();
        NetworkManager _nm = new NetworkManager(this._c);
        for (String iface : _nm.getSystemInterfaces()) {
            _sb.append(iface);
            _sb.append(" (");
            _sb.append(NetworkManager.addressToString(_nm.getAddress(iface)));
            _sb.append("/");
            _sb.append(NetworkManager.addressToString(_nm.getNetmask(iface)));
            _sb.append(")<br/>");
            String _value;
            try {
                _value = Command.systemCommand("/sbin/mii-tool " + iface);
            } catch (Exception _ex) {
                _value = _ex.getMessage();
            }
            if (_value != null && !_value.isEmpty()) {
                _sb.append("&nbsp;&nbsp;<span style=\"color: #c3c3c3\">");
                _sb.append(_value.substring(_value.indexOf(":") + 1).trim());
                _sb.append("</span><br/>");
            }
        }
        _fields.put("Network interfaces", _sb.toString());
        _sb = new StringBuilder();
        try {
            String _list = Command.systemCommand("dpkg -l | awk '{ print $2, $3 }'");
            if (_list == null || _list.isEmpty()) {
                throw new Exception();
            }
            for (String _line : _list.split("\n")) {
                if (_line.matches("^[a-zA-Z0-9-._+]+\\ [0-9][a-zA-Z0-9-._+:~]+")) {
                    _sb.append(_line);
                    _sb.append("<br/>");
                }
            }
        } catch (Exception _ex) {
            _sb.append("packages cannot be displayed");
        }
        _fields.put("Packages", _sb.toString());
        _mt.setMailFields(_fields);
        List<String> _mail_to = new ArrayList<String>();
        List<String> _mail_bcc = new ArrayList<String>();
        if (this._sc.getAdministrativeMail() != null) {
            _mail_to.add(this._sc.getAdministrativeMail());
            if (_lm.hasLicenseType("SUPPORT")) {
                _mail_bcc.add(SUPPORT_MAIL);
            }
        } else if (_lm.hasLicenseType("SUPPORT")) {
            _mail_to.add(SUPPORT_MAIL);
        }
        _mt.sendMail(_mail_to, _mail_bcc, _title.toString());
    }
}
