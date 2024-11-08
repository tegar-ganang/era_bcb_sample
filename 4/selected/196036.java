package com.whitebearsolutions.imagine.wbsagnitio.idm.listener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.whitebearsolutions.directory.Entry;
import com.whitebearsolutions.imagine.wbsagnitio.configuration.HAConfiguration;
import com.whitebearsolutions.imagine.wbsagnitio.configuration.WBSAgnitioConfiguration;
import com.whitebearsolutions.imagine.wbsagnitio.directory.AttributeSet;
import com.whitebearsolutions.imagine.wbsagnitio.directory.GroupManager;
import com.whitebearsolutions.imagine.wbsagnitio.directory.UserManager;
import com.whitebearsolutions.imagine.wbsagnitio.idm.ProfileManager;
import com.whitebearsolutions.imagine.wbsagnitio.idm.RoleManager;
import com.whitebearsolutions.imagine.wbsagnitio.idm.RuleEngine;
import com.whitebearsolutions.imagine.wbsagnitio.idm.RuleManager;
import com.whitebearsolutions.imagine.wbsagnitio.idm.repository.ExternalRepository;
import com.whitebearsolutions.imagine.wbsagnitio.idm.repository.ExternalRepositoryDatabase;
import com.whitebearsolutions.imagine.wbsagnitio.transport.CustomTransport;
import com.whitebearsolutions.io.FileLock;
import com.whitebearsolutions.io.FileLockException;
import com.whitebearsolutions.util.Configuration;

public class RepositoryListener {

    private static final String _prefix = "REPOSITORY-LISTENER-";

    private String _repository, _branch;

    private ExternalRepository _er;

    private int _interval;

    private static File _listener_directory;

    private boolean _update;

    private List<String> _report_emails;

    static {
        _listener_directory = new File(WBSAgnitioConfiguration.getIDMDirectory() + "/engine/listeners");
        if (!_listener_directory.exists()) {
            _listener_directory.mkdirs();
        }
    }

    private RepositoryListener(String repository, ExternalRepository er, int interval, String branch, List<String> emails, boolean update) throws Exception {
        if (repository == null || !repository.matches("[0-9a-zA-Z_.-]+")) {
            throw new Exception("invalid repository name");
        }
        if (er == null) {
            throw new Exception("invalid external repository object");
        }
        this._repository = repository;
        this._er = er;
        if (interval <= 0) {
            interval = 1;
        }
        this._interval = interval * 60 * 1000;
        this._branch = branch;
        if (emails != null) {
            this._report_emails = emails;
        } else {
            this._report_emails = new ArrayList<String>();
        }
        this._update = update;
    }

    private static List<Thread> getActiveThreads() {
        ThreadGroup _rootGroup = Thread.currentThread().getThreadGroup();
        for (ThreadGroup _parentGroup; (_parentGroup = _rootGroup.getParent()) != null; ) {
            _rootGroup = _parentGroup;
        }
        Thread[] _threads = new Thread[_rootGroup.activeCount()];
        for (int i = _rootGroup.enumerate(_threads, true); i >= _threads.length; i = _rootGroup.enumerate(_threads, true)) {
            _threads = new Thread[_threads.length + 1];
        }
        List<Thread> _threadList = new ArrayList<Thread>(Arrays.asList(_threads));
        _threadList.removeAll(Collections.singleton(null));
        return _threadList;
    }

    public static List<RepositoryListener> getActiveListeners() throws Exception {
        List<RepositoryListener> _listeners = new ArrayList<RepositoryListener>();
        RuleManager _rm = new RuleManager();
        List<String> _report_emails = new ArrayList<String>();
        int _interval;
        boolean _update;
        for (Map<String, String> _listener : _rm.getAllListeners()) {
            if (_listener.get("active") != null && "true".equalsIgnoreCase(_listener.get("active"))) {
                _interval = 0;
                try {
                    _interval = Integer.parseInt(_listener.get("interval"));
                } catch (Exception _ex) {
                    throw new Exception("invalid listener interval");
                }
                if (_listener.get("report") != null) {
                    if (_listener.get("report").contains(",")) {
                        for (String _mail : _listener.get("report").split(",")) {
                            _mail = _mail.trim();
                            if (!_mail.isEmpty()) {
                                _report_emails.add(_mail);
                            }
                        }
                    } else {
                        _report_emails.add(_listener.get("report"));
                    }
                }
                try {
                    _update = Boolean.parseBoolean(_listener.get("update"));
                } catch (Exception _ex) {
                    throw new Exception("invalid listener update value");
                }
                _listeners.add(new RepositoryListener(_listener.get("repository"), ExternalRepository.getInstance(_listener.get("repository")), _interval, _listener.get("branch"), _report_emails, _update));
            }
        }
        return _listeners;
    }

    public static RepositoryListener getInstance(String repository) throws Exception {
        if (repository == null || repository.isEmpty()) {
            throw new Exception("invalid listener");
        }
        List<String> _report_emails = new ArrayList<String>();
        int _interval = -1;
        boolean _update;
        RuleManager _rm = new RuleManager();
        Map<String, String> _listener = _rm.getListener(repository);
        try {
            _interval = Integer.parseInt(_listener.get("interval"));
        } catch (Exception _ex) {
            throw new Exception("invalid listener interval");
        }
        if (_listener.get("report") != null) {
            if (_listener.get("report").contains(",")) {
                for (String _mail : _listener.get("report").split(",")) {
                    _mail = _mail.trim();
                    if (!_mail.isEmpty()) {
                        _report_emails.add(_mail);
                    }
                }
            } else {
                _report_emails.add(_listener.get("report"));
            }
        }
        try {
            _update = Boolean.parseBoolean(_listener.get("update"));
        } catch (Exception _ex) {
            throw new Exception("invalid listener update value");
        }
        return new RepositoryListener(repository, ExternalRepository.getInstance(repository), _interval, _listener.get("branch"), _report_emails, _update);
    }

    @SuppressWarnings("unchecked")
    private static List<String> getLastList(File lastListFile) {
        List<String> _users = null;
        if (lastListFile != null && lastListFile.isFile()) {
            ObjectInputStream _ois = null;
            try {
                _ois = new ObjectInputStream(new FileInputStream(lastListFile));
                for (Object _o = _ois.readObject(); _o != null; _o = _ois.readObject()) {
                    if (_o instanceof List) {
                        _users = (List<String>) _o;
                    }
                }
            } catch (Exception _ex) {
            } finally {
                if (_ois != null) {
                    try {
                        _ois.close();
                    } catch (IOException _ex) {
                    }
                }
            }
        }
        return _users;
    }

    private static Calendar getLastModificationCalendar(File lastModificationFile) {
        Calendar _cal = null;
        if (lastModificationFile != null && lastModificationFile.isFile()) {
            ObjectInputStream _ois = null;
            try {
                _ois = new ObjectInputStream(new FileInputStream(lastModificationFile));
                for (Object _o = _ois.readObject(); _o != null; _o = _ois.readObject()) {
                    if (_o instanceof Calendar) {
                        _cal = (Calendar) _o;
                    }
                }
            } catch (Exception _ex) {
            } finally {
                if (_ois != null) {
                    try {
                        _ois.close();
                    } catch (IOException _ex) {
                    }
                }
            }
        }
        return _cal;
    }

    public static File getListenerFile(String repository, String extension) {
        if (repository == null || repository.isEmpty() || extension == null || extension.isEmpty()) {
            return null;
        }
        StringBuilder _sb = new StringBuilder();
        _sb.append(_listener_directory.getAbsolutePath());
        _sb.append("/");
        _sb.append(repository);
        _sb.append(".");
        _sb.append(extension);
        return new File(_sb.toString());
    }

    public List<String> getLogLines() throws IOException {
        List<String> _log = new ArrayList<String>();
        File _logFile = getListenerFile(this._repository, "log");
        if (_logFile.exists()) {
            LineNumberReader _lr = null;
            try {
                _lr = new LineNumberReader(new FileReader(_logFile));
                for (String _line = _lr.readLine(); _line != null; _line = _lr.readLine()) {
                    _log.add(_line);
                }
            } finally {
                if (_lr != null) {
                    _lr.close();
                }
            }
        }
        return _log;
    }

    public void removeLogLines() throws IOException {
        File _logFile = getListenerFile(this._repository, "log");
        if (_logFile.exists()) {
            _logFile.delete();
        }
    }

    private static String getStringCalendar(Calendar _cal) {
        StringBuilder _sb = new StringBuilder();
        if (_cal != null) {
            if (_cal.get(Calendar.DAY_OF_MONTH) <= 9) {
                _sb.append("0");
            }
            _sb.append(_cal.get(Calendar.DAY_OF_MONTH));
            _sb.append("/");
            if ((_cal.get(Calendar.MONTH) + 1) <= 9) {
                _sb.append("0");
            }
            _sb.append(_cal.get(Calendar.MONTH) + 1);
            _sb.append("/");
            _sb.append(_cal.get(Calendar.YEAR));
            _sb.append(" ");
            if (_cal.get(Calendar.HOUR_OF_DAY) <= 9) {
                _sb.append("0");
            }
            _sb.append(_cal.get(Calendar.HOUR_OF_DAY));
            _sb.append(":");
            if (_cal.get(Calendar.MINUTE) <= 9) {
                _sb.append("0");
            }
            _sb.append(_cal.get(Calendar.MINUTE));
            _sb.append(":");
            if (_cal.get(Calendar.SECOND) <= 9) {
                _sb.append("0");
            }
            _sb.append(_cal.get(Calendar.SECOND));
        } else {
            _sb.append("Never");
        }
        return _sb.toString();
    }

    private static void internalRun(ExternalRepository _er, int _interval, String branch, File lastModificationFile, File lastGroupListFile, File lastUserListFile, File log, List<String> report_emails, boolean update) throws Exception {
        Configuration _c = new Configuration(new File(WBSAgnitioConfiguration.getConfigurationFile()));
        RuleEngine _re = new RuleEngine(null);
        RoleManager _rm = new RoleManager();
        UserManager _um = new UserManager(_c);
        GroupManager _gm = new GroupManager(_c);
        ProfileManager _pm = new ProfileManager();
        try {
            writeLine(log, "Listener started");
            for (Thread.sleep(_interval); true; Thread.sleep(_interval)) {
                try {
                    List<String> _new_users = new ArrayList<String>();
                    List<String> _modified_users = new ArrayList<String>();
                    List<String> _removed_users = new ArrayList<String>();
                    Map<String, String> _added_profiles = new HashMap<String, String>();
                    Map<String, String> _removed_profiles = new HashMap<String, String>();
                    List<String> _new_groups = new ArrayList<String>();
                    List<String> _modified_groups = new ArrayList<String>();
                    List<String> _removed_groups = new ArrayList<String>();
                    Calendar _lastModification = Calendar.getInstance();
                    Calendar _cal = getLastModificationCalendar(lastModificationFile);
                    List<String> _users = new ArrayList<String>();
                    List<String> _groups = new ArrayList<String>();
                    List<String> _old_users = getLastList(lastUserListFile);
                    List<String> _old_groups = getLastList(lastGroupListFile);
                    writeLine(log, "Searching users modified since: " + getStringCalendar(_cal));
                    List<AttributeSet> _entries = _er.getUserEntries(_cal, _users);
                    for (AttributeSet _attributes : _entries) {
                        if (!_um.userExists(_attributes.getAttributeFirstStringValue("uid"))) {
                            writeLine(log, "Found a new user: " + _attributes.getAttributeFirstStringValue("uid"));
                            if (!report_emails.isEmpty()) {
                                _new_users.add(_attributes.getAttributeFirstStringValue("uid"));
                            }
                            if (update) {
                                try {
                                    _attributes.setAttribute("branch", branch);
                                    _re.writeEntry(RuleEngine.USER_ADD, _attributes);
                                    writeLine(log, "Added user: " + _attributes.getAttributeFirstStringValue("uid"));
                                } catch (Exception _ex) {
                                    writeLine(log, "Add user error: " + _ex.getMessage());
                                }
                            }
                        } else {
                            if (!report_emails.isEmpty()) {
                                try {
                                    AttributeSet _user_attributes = new AttributeSet(_um.getUserEntry(_attributes.getAttributeFirstStringValue("uid")));
                                    List<String> keys = _attributes.getAttributeNames();
                                    for (String entry : keys) {
                                        if (!"password".equals(entry) && !"userpassword".equals(entry) && _attributes.getAttribute(entry) != null && _attributes.getAttribute(entry)[0] != null && (!_attributes.getAttribute(entry)[0].equals(_user_attributes.getAttribute(entry)[0]))) {
                                            writeLine(log, "Found attribute differences for this user " + _attributes.getAttributeFirstStringValue("uid"));
                                            _modified_users.add(_attributes.getAttributeFirstStringValue("uid"));
                                            break;
                                        }
                                    }
                                } catch (Exception _ex) {
                                    writeLine(log, "Attribute consistency check error: " + _ex.getMessage());
                                }
                            }
                            writeLine(log, "Found an existing user: " + _attributes.getAttributeFirstStringValue("uid"));
                            if (update) {
                                try {
                                    _attributes.setAttribute("branch", branch);
                                    _re.writeEntry(RuleEngine.USER_UPDATE, _attributes);
                                    writeLine(log, "Updated user: " + _attributes.getAttributeFirstStringValue("uid"));
                                } catch (Exception _ex) {
                                    writeLine(log, "Update user error: " + _ex.getMessage());
                                }
                            }
                        }
                        writeLine(log, "Searching user profiles: ");
                        try {
                            Entry _e_user = _um.getUserEntry(_attributes.getAttributeFirstStringValue("uid"));
                            List<String> _activeProfiles = _er.getProfiles(_attributes.getAttributeFirstStringValue("uid"));
                            for (Map<String, String> _profileRepository : _pm.getProfiles(_er.getRepositoryName())) {
                                if (_activeProfiles.contains(_profileRepository.get("name"))) {
                                    for (String _role : _pm.getProfileRoles(_profileRepository.get("name"))) {
                                        writeLine(log, "Profile is associated with the role: " + _role);
                                        for (String _group : _rm.getGroups(_role)) {
                                            Entry _e_group = _gm.getGroupEntry(_group);
                                            if (!_gm.isGroupMember(_e_group.getID(), _e_user.getID())) {
                                                if (!report_emails.isEmpty()) {
                                                    _added_profiles.put(_role, _attributes.getAttributeFirstStringValue("uid"));
                                                }
                                                if (update) {
                                                    writeLine(log, "Adding user to role group: " + _group);
                                                    _re.writeMembershipEntry(RuleEngine.GROUP_USER_ADD, _e_group, _e_user);
                                                }
                                            } else {
                                                writeLine(log, "User is already member of role group: " + _group);
                                            }
                                        }
                                    }
                                } else {
                                    for (String _role : _pm.getProfileRoles(_profileRepository.get("name"))) {
                                        writeLine(log, "Profile is not associated with the role: " + _role);
                                        for (String _group : _rm.getGroups(_role)) {
                                            Entry _e_group = _gm.getGroupEntry(_group);
                                            if (_gm.isGroupMember(_e_group.getID(), _e_user.getID())) {
                                                if (!report_emails.isEmpty()) {
                                                    _removed_profiles.put(_role, _attributes.getAttributeFirstStringValue("uid"));
                                                }
                                                if (update) {
                                                    writeLine(log, "Removing user to role group: " + _group);
                                                    _re.writeMembershipEntry(RuleEngine.GROUP_USER_REMOVE, _e_group, _e_user);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (Exception _ex) {
                            writeLine(log, "Profile search error: " + _ex.toString());
                        }
                    }
                    if (_old_users != null) {
                        _old_users.removeAll(_users);
                        for (String _user : _old_users) {
                            AttributeSet _attributes = new AttributeSet();
                            _attributes.setAttribute("uid", _user);
                            writeLine(log, "Found a removed user: " + _attributes.getAttributeFirstStringValue("uid"));
                            if (!report_emails.isEmpty()) {
                                _removed_users.add(_user);
                            }
                            if (update) {
                                try {
                                    _re.writeEntry(RuleEngine.USER_REMOVE, _attributes);
                                    writeLine(log, "Removed user: " + _attributes.getAttributeFirstStringValue("uid"));
                                } catch (Exception _ex) {
                                    writeLine(log, "Remove user error: " + _ex.getMessage());
                                }
                            }
                        }
                    }
                    writeLine(log, "Searching groups modified since: " + getStringCalendar(_cal));
                    _entries = _er.getGroupEntries(_cal, _groups);
                    for (AttributeSet _attributes : _entries) {
                        if (!_gm.groupExists(_attributes.getAttributeFirstStringValue("cn"))) {
                            writeLine(log, "Found a new group: " + _attributes.getAttributeFirstStringValue("cn"));
                            if (!report_emails.isEmpty()) {
                                _new_groups.add(_attributes.getAttributeFirstStringValue("cn"));
                            }
                            if (update) {
                                try {
                                    _re.writeEntry(RuleEngine.GROUP_ADD, _attributes);
                                    writeLine(log, "Added group: " + _attributes.getAttributeFirstStringValue("cn"));
                                } catch (Exception _ex) {
                                    writeLine(log, "Add group error: " + _ex.getMessage());
                                }
                            }
                        } else {
                            writeLine(log, "Found an existing group: " + _attributes.getAttributeFirstStringValue("cn"));
                            if (!report_emails.isEmpty()) {
                                AttributeSet _group_attributes = new AttributeSet(_gm.getGroupEntry(_attributes.getAttributeFirstStringValue("cn")));
                                if (!_attributes.equals(_group_attributes)) {
                                    writeLine(log, "Found attribute differences for this group");
                                    _modified_groups.add(_attributes.getAttributeFirstStringValue("cn"));
                                }
                            }
                            if (update) {
                                try {
                                    _re.writeEntry(RuleEngine.GROUP_UPDATE, _attributes);
                                    writeLine(log, "Updated group: " + _attributes.getAttributeFirstStringValue("cn"));
                                } catch (Exception _ex) {
                                    writeLine(log, "Update group error: " + _ex.getMessage());
                                }
                            }
                        }
                    }
                    if (_old_groups != null) {
                        _old_groups.removeAll(_groups);
                        for (String _group : _old_groups) {
                            AttributeSet _attributes = new AttributeSet();
                            _attributes.setAttribute("cn", _group);
                            writeLine(log, "Found a removed group: " + _attributes.getAttributeFirstStringValue("cn"));
                            if (!report_emails.isEmpty()) {
                                _removed_groups.add(_group);
                            }
                            if (update) {
                                try {
                                    _re.writeEntry(RuleEngine.GROUP_REMOVE, _attributes);
                                    writeLine(log, "Removed group: " + _attributes.getAttributeFirstStringValue("cn"));
                                } catch (Exception _ex) {
                                    writeLine(log, "Remove group error: " + _ex.getMessage());
                                }
                            }
                        }
                    }
                    writeLastModification(lastModificationFile, _lastModification);
                    writeLastList(lastGroupListFile, _groups);
                    writeLastList(lastUserListFile, _users);
                    if (!report_emails.isEmpty()) {
                        Map<String, String> _fields = new HashMap<String, String>();
                        _fields.put("Repository", _er.getRepositoryName());
                        if (!_new_users.isEmpty()) {
                            StringBuilder _sb = new StringBuilder();
                            _sb.append("The following users have been created in the repository and are not integrated into the management database:<br/>\n");
                            for (String _user : _new_users) {
                                _sb.append("&nbsp;<strong>");
                                _sb.append(_user);
                                _sb.append("</strong><br/>\n");
                            }
                            _fields.put("Added users", _sb.toString());
                        }
                        if (!_modified_users.isEmpty()) {
                            StringBuilder _sb = new StringBuilder();
                            _sb.append("The following users have been modified in the repository and are not modified into the management database:<br/>\n");
                            for (String _user : _modified_users) {
                                _sb.append("&nbsp;<strong>");
                                _sb.append(_user);
                                _sb.append("</strong><br/>\n");
                            }
                            _fields.put("Modified users", _sb.toString());
                        }
                        if (!_removed_users.isEmpty()) {
                            StringBuilder _sb = new StringBuilder();
                            _sb.append("The following users have been removed in the repository and are not removed into the management database:<br/>\n");
                            for (String _user : _removed_users) {
                                _sb.append("&nbsp;<strong>");
                                _sb.append(_user);
                                _sb.append("</strong><br/>\n");
                            }
                            _fields.put("Removed users", _sb.toString());
                        }
                        if (!_added_profiles.isEmpty()) {
                            StringBuilder _sb = new StringBuilder();
                            _sb.append("The following profiles have been added in the repository and are not modified into the management database:<br/>\n");
                            for (String _profile : _added_profiles.keySet()) {
                                _sb.append("&nbsp;<strong>");
                                _sb.append(_profile);
                                _sb.append("</strong> / <strong>");
                                _sb.append(_added_profiles.get(_profile));
                                _sb.append("</strong><br/>\n");
                            }
                            _fields.put("Added profiles", _sb.toString());
                        }
                        if (!_removed_profiles.isEmpty()) {
                            StringBuilder _sb = new StringBuilder();
                            _sb.append("The following profiles have been removed in the repository and are not modified into the management database:<br/>\n");
                            for (String _profile : _removed_profiles.keySet()) {
                                _sb.append("&nbsp;<strong>");
                                _sb.append(_profile);
                                _sb.append("</strong> / <strong>");
                                _sb.append(_removed_profiles.get(_profile));
                                _sb.append("</strong><br/>\n");
                            }
                            _fields.put("Removed profiles", _sb.toString());
                        }
                        if (!_new_groups.isEmpty()) {
                            StringBuilder _sb = new StringBuilder();
                            _sb.append("The following groups have been created in the repository and are not integrated into the management database:<br/>\n");
                            for (String _group : _new_groups) {
                                _sb.append("&nbsp;<strong>");
                                _sb.append(_group);
                                _sb.append("</strong><br/>\n");
                            }
                            _fields.put("Added groups", _sb.toString());
                        }
                        if (!_modified_groups.isEmpty()) {
                            StringBuilder _sb = new StringBuilder();
                            _sb.append("The following groups have been modified in the repository and are not modified into the management database:<br/>\n");
                            for (String _group : _modified_groups) {
                                _sb.append("&nbsp;<strong>");
                                _sb.append(_group);
                                _sb.append("</strong><br/>\n");
                            }
                            _fields.put("Modified groups", _sb.toString());
                        }
                        if (!_removed_groups.isEmpty()) {
                            StringBuilder _sb = new StringBuilder();
                            _sb.append("The following groups have been removed in the repository and are not integrated into the management database:<br/>\n");
                            for (String _group : _removed_groups) {
                                _sb.append("&nbsp;<strong>");
                                _sb.append(_group);
                                _sb.append("</strong><br/>\n");
                            }
                            _fields.put("Removed groups", _sb.toString());
                        }
                        if (!_fields.isEmpty()) {
                            CustomTransport _mt = new CustomTransport();
                            _mt.setMailFields(_fields);
                            _mt.sendMail(report_emails, "WBSAGNITIO ENFORCEMENT COMPLIANCE REPORT");
                        }
                    }
                } catch (IllegalStateException _ex) {
                    writeLine(log, "Listener invalid application state: exiting ... (maybe the application has been updated?)");
                    Thread.currentThread().interrupt();
                } catch (NullPointerException _ex) {
                    writeLine(log, "Listener invalid application state: exiting ... (maybe the application has been updated?)");
                    Thread.currentThread().interrupt();
                } catch (Exception _ex) {
                    writeLine(log, "Listener error: " + _ex.getMessage());
                }
            }
        } catch (InterruptedException _ex) {
        } catch (IllegalStateException _ex) {
            writeLine(log, "Listener invalid application state: exiting ... (maybe the application has been updated?)");
        }
        writeLine(log, "Listener stopped");
    }

    public static void interrupt(String repository) throws Exception {
        String _threadName = _prefix.concat(repository);
        for (Thread _t : getActiveThreads()) {
            String _name = _t.getName();
            if (_name != null && _name.toLowerCase().equalsIgnoreCase(_threadName)) {
                RuleManager _rm = new RuleManager();
                _rm.setActiveListener(repository, false);
                _t.interrupt();
            }
        }
    }

    public static boolean isRunning(String repository) {
        String _threadName = _prefix.concat(repository);
        for (Thread _t : getActiveThreads()) {
            String _name = _t.getName();
            if (_name != null && _name.toLowerCase().equalsIgnoreCase(_threadName)) {
                return true;
            }
        }
        return false;
    }

    public void run() throws Exception {
        if (!"slave".equals(HAConfiguration.getStatus())) {
            final ExternalRepository _er = this._er;
            final int interval = this._interval;
            final String branch = this._branch;
            final File _lastModificationFile = getListenerFile(this._repository, "listener");
            final File _lastGroupListFile = getListenerFile(this._repository, "groups");
            final File _lastUserListFile = getListenerFile(this._repository, "users");
            final File _log = getListenerFile(this._repository, "log");
            final List<String> _report_emails = this._report_emails;
            final boolean _update = this._update;
            if (!isRunning(this._repository)) {
                RuleManager _rm = new RuleManager();
                _rm.setActiveListener(this._repository, true);
                Runnable r = new Runnable() {

                    public void run() {
                        try {
                            internalRun(_er, interval, branch, _lastModificationFile, _lastGroupListFile, _lastUserListFile, _log, _report_emails, _update);
                        } catch (Exception _ex) {
                            _ex.printStackTrace();
                        }
                    }
                };
                Thread internalThread = new Thread(r);
                internalThread.setName(_prefix.concat(this._repository));
                internalThread.start();
            }
        }
    }

    private static void writeLastList(File lastUserListFile, List<String> list) {
        ObjectOutputStream _oos = null;
        FileLock _fl = new FileLock(lastUserListFile);
        try {
            _fl.lock();
            _oos = new ObjectOutputStream(new FileOutputStream(lastUserListFile));
            _oos.writeObject(list);
        } catch (Exception _ex) {
        } finally {
            try {
                _fl.unlock();
            } catch (FileLockException _ex) {
            }
            if (_oos != null) {
                try {
                    _oos.close();
                } catch (IOException _ex) {
                }
            }
        }
    }

    private static void writeLastModification(File lastModificationFile, Calendar cal) {
        ObjectOutputStream _oos = null;
        FileLock _fl = new FileLock(lastModificationFile);
        try {
            _fl.lock();
            _oos = new ObjectOutputStream(new FileOutputStream(lastModificationFile));
            _oos.writeObject(cal);
        } catch (Exception _ex) {
        } finally {
            try {
                _fl.unlock();
            } catch (FileLockException _ex) {
            }
            if (_oos != null) {
                try {
                    _oos.close();
                } catch (IOException _ex) {
                }
            }
        }
    }

    private static void writeLine(File log, String message) {
        if (log != null && message != null) {
            Calendar _cal = Calendar.getInstance();
            StringBuilder _sb = new StringBuilder();
            FileOutputStream _fos = null;
            FileLock _fl = new FileLock(log);
            _sb.append(getStringCalendar(_cal));
            _sb.append(" ");
            _sb.append(message);
            _sb.append("\n");
            try {
                _fl.lock();
                _fos = new FileOutputStream(log, true);
                _fos.write(_sb.toString().getBytes());
            } catch (Exception _ex) {
            } finally {
                try {
                    _fl.unlock();
                } catch (FileLockException _ex) {
                }
                if (_fos != null) {
                    try {
                        _fos.close();
                    } catch (IOException _ex) {
                    }
                }
            }
        }
    }
}
