package com.whitebearsolutions.imagine.wbsagnitio.idm.repository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TimeZone;
import com.whitebearsolutions.directory.Entry;
import com.whitebearsolutions.directory.EntryBase;
import com.whitebearsolutions.directory.Query;
import com.whitebearsolutions.directory.ldap.LDAPConnection;
import com.whitebearsolutions.directory.ldap.LDAPEntry;
import com.whitebearsolutions.imagine.wbsagnitio.configuration.SystemConfiguration;
import com.whitebearsolutions.imagine.wbsagnitio.configuration.WBSAgnitioConfiguration;
import com.whitebearsolutions.imagine.wbsagnitio.directory.AttributeSet;
import com.whitebearsolutions.imagine.wbsagnitio.directory.EntryManager;
import com.whitebearsolutions.imagine.wbsagnitio.directory.GroupManager;
import com.whitebearsolutions.imagine.wbsagnitio.directory.UserManager;
import com.whitebearsolutions.util.Configuration;

public class ExternalRepositoryLDAP extends ExternalRepository {

    private static final int STORE_FLEXIBLE = 1;

    private static final int STORE_ADD_ONLY = 2;

    private static final int STORE_UPDATE_ONLY = 3;

    private Configuration _local_c;

    private Configuration _remote_c;

    private SystemConfiguration _sc;

    private EntryBase _eb;

    private boolean delete = false;

    private String _user_suffix;

    private String _group_suffix;

    private String _member_attribute;

    private List<String> _user_objectclasses;

    private List<String> _group_objectclasses;

    public ExternalRepositoryLDAP(Configuration _c) throws Exception {
        super();
        this._local_c = new Configuration(new File(WBSAgnitioConfiguration.getConfigurationFile()));
        this._remote_c = _c;
        this._sc = new SystemConfiguration(this._local_c);
        this._eb = new EntryBase(this._remote_c);
        if (this._remote_c.checkProperty("ldap.replica.delete", "true")) {
            this.delete = true;
        }
        if (_c.getProperty("ldap.sufix.user") == null || _c.getProperty("ldap.sufix.user").isEmpty()) {
            this._user_suffix = ",ou=" + this._sc.getBranchName(SystemConfiguration.BRANCH_USERS) + "," + this._remote_c.getProperty("ldap.basedn");
        } else {
            this._user_suffix = _c.getProperty("ldap.sufix.user");
        }
        if (_c.getProperty("ldap.sufix.group") == null || _c.getProperty("ldap.sufix.group").isEmpty()) {
            this._group_suffix = ",ou=" + this._sc.getBranchName(SystemConfiguration.BRANCH_GROUPS) + "," + this._remote_c.getProperty("ldap.basedn");
        } else {
            this._group_suffix = _c.getProperty("ldap.sufix.group");
        }
        if (_c.getProperty("ldap.group.member") == null || _c.getProperty("ldap.group.member").isEmpty()) {
            this._member_attribute = "member";
        } else {
            this._member_attribute = _c.getProperty("ldap.group.member");
        }
        this._user_objectclasses = new ArrayList<String>();
        this._group_objectclasses = new ArrayList<String>();
        if (_c.getProperty("ldap.objectclasses.user") != null && !_c.getProperty("ldap.objectclasses.user").isEmpty()) {
            StringTokenizer _st = new StringTokenizer(_c.getProperty("ldap.objectclasses.user"), ",");
            while (_st.hasMoreTokens()) {
                this._user_objectclasses.add(_st.nextToken());
            }
        }
        if (_c.getProperty("ldap.objectclasses.group") != null && !_c.getProperty("ldap.objectclasses.group").isEmpty()) {
            StringTokenizer _st = new StringTokenizer(_c.getProperty("ldap.objectclasses.group"), ",");
            while (_st.hasMoreTokens()) {
                this._group_objectclasses.add(_st.nextToken());
            }
        }
    }

    public void addGroupEntry(String group, AttributeSet attributes) throws Exception {
        storeGroupEntry(group, attributes, STORE_ADD_ONLY);
    }

    public void addUserEntry(String user, AttributeSet attributes) throws Exception {
        storeUserEntry(user, attributes, STORE_ADD_ONLY);
    }

    public void addUserMember(String group, String user) throws Exception {
        AttributeSet _group = getGroupEntry(group);
        AttributeSet _user = getUserEntry(user);
        if (_group == null) {
            throw new Exception("group does not exists");
        }
        if (_user == null) {
            throw new Exception("user does not exists");
        }
        this._eb.addEntryAttribute(_group.getAttributeFirstStringValue("dn"), "member", _user.getAttributeFirstStringValue("dn"));
    }

    public void deleteGroupEntry(String group, AttributeSet attributes) throws Exception {
        AttributeSet _group = getGroupEntry(group);
        if (_group == null) {
            throw new Exception("group does not exists");
        }
        this._eb.removeEntry(_group.getAttributeFirstStringValue("dn"));
    }

    public void deleteProfile(String user, String profile) {
    }

    public void deleteUserEntry(String user, AttributeSet attributes) throws Exception {
        AttributeSet _user = getUserEntry(user);
        if (_user == null) {
            throw new Exception("user does not exists");
        }
        this._eb.removeEntry(_user.getAttributeFirstStringValue("dn"));
    }

    public void exportAllEntries(File log) {
        FileOutputStream _log = null;
        try {
            _log = new FileOutputStream(log);
            _log.write("".getBytes());
        } catch (IOException _ex) {
        } finally {
            try {
                if (_log != null) {
                    _log.close();
                }
            } catch (IOException _ex) {
            }
        }
        exportUserEntries(log);
        exportGroupEntries(log);
    }

    private void exportGroupEntries(File log) {
        FileOutputStream _log = null;
        try {
            _log = new FileOutputStream(log, true);
            EntryManager _em = new EntryManager(new Configuration(new File(WBSAgnitioConfiguration.getConfigurationFile())));
            write(_log, "Verifying organizational unit .. ");
            if (!_em.checkEntry("ou=" + this._sc.getBranchName(SystemConfiguration.BRANCH_GROUPS) + "," + this._remote_c.getProperty("ldap.basedn"))) {
                Entry _ou_p = new LDAPEntry("ou=" + this._sc.getBranchName(SystemConfiguration.BRANCH_GROUPS) + "," + this._remote_c.getProperty("ldap.basedn"));
                _ou_p.setAttribute("objectclass", "organizationalUnit");
                _ou_p.setAttribute("ou", this._sc.getBranchName(SystemConfiguration.BRANCH_USERS));
                this._eb.addEntry(_ou_p);
            }
            writeLine(_log, "done");
            write(_log, "Reading groups from local directory .. ");
            List<Entry> _groups = _em.getAllEntries(EntryManager.GROUP);
            writeLine(_log, "done");
            for (Entry _g : _groups) {
                try {
                    AttributeSet _as = new AttributeSet(_g);
                    write(_log, "Writing group [");
                    write(_log, _as.getAttributeFirstStringValue("cn"));
                    write(_log, "]: ");
                    storeGroupEntry(_as.getAttributeFirstStringValue("cn"), _as);
                    writeLine(_log, "done");
                } catch (Exception _ex) {
                    write(_log, "error - ");
                    writeLine(_log, _ex.getMessage());
                }
            }
        } catch (Exception _ex) {
            writeLine(_log, "error - " + _ex.getMessage());
        } finally {
            if (_log != null) {
                try {
                    _log.close();
                } catch (IOException _ex2) {
                }
            }
        }
    }

    private void exportUserEntries(File log) {
        FileOutputStream _log = null;
        try {
            _log = new FileOutputStream(log, true);
            EntryManager _em = new EntryManager(new Configuration(new File(WBSAgnitioConfiguration.getConfigurationFile())));
            write(_log, "Verifying organizational unit .. ");
            if (!_em.checkEntry("ou=" + this._sc.getBranchName(SystemConfiguration.BRANCH_USERS) + "," + this._remote_c.getProperty("ldap.basedn"))) {
                Entry _ou_p = new LDAPEntry("ou=" + this._sc.getBranchName(SystemConfiguration.BRANCH_USERS) + "," + this._remote_c.getProperty("ldap.basedn"));
                _ou_p.setAttribute("objectclass", "organizationalUnit");
                _ou_p.setAttribute("ou", this._sc.getBranchName(SystemConfiguration.BRANCH_USERS));
                this._eb.addEntry(_ou_p);
            }
            writeLine(_log, "done");
            List<String> _remove_users = new ArrayList<String>();
            if (this.delete) {
                Query _q = new Query();
                _q.addCondition("objectclass", "posixAccount", Query.EXACT);
                write(_log, "Reading users from remote directory .. ");
                for (Entry _u : this._eb.search(_q)) {
                    if (_u.hasAttribute("uid")) {
                        String _uid = String.valueOf(_u.getAttribute("uid")[0]);
                        if (!_uid.toLowerCase().equals("Manager") && !_uid.toLowerCase().equals("admin")) {
                            _remove_users.add(_uid);
                        }
                    }
                }
                writeLine(_log, "done");
            }
            write(_log, "Reading users from local directory .. ");
            List<Entry> _users = _em.getAllEntries(EntryManager.USER);
            writeLine(_log, "done");
            for (Entry _u : _users) {
                if (this._sc.getAdministrativeUser().equals(String.valueOf(_u.getAttribute("uid")[0]))) {
                    continue;
                }
                try {
                    AttributeSet _as = new AttributeSet(_u);
                    write(_log, "Writing user [");
                    write(_log, _as.getAttributeFirstStringValue("uid"));
                    write(_log, "]: ");
                    storeUserEntry(_as.getAttributeFirstStringValue("uid"), _as);
                    if (this.delete) {
                        _remove_users.remove(_u.getAttribute("uid"));
                    }
                    writeLine(_log, "done");
                } catch (Exception _ex) {
                    write(_log, "error - ");
                    writeLine(_log, _ex.getMessage());
                }
            }
            if (this.delete) {
                for (String uid : _remove_users) {
                    write(_log, "Deleting user entry [");
                    write(_log, uid);
                    write(_log, "]: ");
                    try {
                        AttributeSet _user = getUserEntry(uid);
                        this._eb.removeEntry(_user.getAttributeFirstStringValue("dn"));
                        writeLine(_log, "done");
                    } catch (Exception _ex) {
                        write(_log, "error - ");
                        writeLine(_log, _ex.getMessage());
                    }
                }
            }
        } catch (Exception _ex) {
            writeLine(_log, "error - " + _ex.getMessage());
        } finally {
            if (_log != null) {
                try {
                    _log.close();
                } catch (IOException _ex2) {
                }
            }
        }
    }

    private static Calendar getCalendarAttribute(String value) throws Exception {
        if (value == null || !value.matches("[0-9.Z]+")) {
            throw new Exception("invalid attribute date format");
        }
        Calendar _c = Calendar.getInstance();
        if (value.endsWith("Z")) {
            _c.setTimeZone(TimeZone.getTimeZone("UTC"));
        }
        _c.set(Calendar.YEAR, Integer.parseInt(value.substring(0, 4)));
        _c.set(Calendar.MONTH, Integer.parseInt(value.substring(4, 6)) - 1);
        _c.set(Calendar.DAY_OF_MONTH, Integer.parseInt(value.substring(6, 8)));
        _c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(value.substring(8, 10)));
        _c.set(Calendar.MINUTE, Integer.parseInt(value.substring(10, 12)));
        if (value.length() > 13) {
            _c.set(Calendar.SECOND, Integer.parseInt(value.substring(12, 14)));
        } else {
            _c.set(Calendar.SECOND, 0);
        }
        _c.set(Calendar.MILLISECOND, 0);
        return _c;
    }

    private AttributeSet getGroupAttributes(Entry _g) throws Exception {
        AttributeSet _group = new AttributeSet();
        for (String _name : this._groupAttributeMaps.keySet()) {
            Map<String, String> _attribute = this._groupAttributeMaps.get(_name);
            if (_attribute.containsKey("attribute")) {
                if (_g.hasAttribute(_name)) {
                    _group.setAttribute(_attribute.get("attribute"), _g.getAttribute(_name));
                }
            }
        }
        return _group;
    }

    public List<AttributeSet> getGroupEntries(Calendar cal, List<String> groups) throws Exception {
        List<AttributeSet> _entries = new ArrayList<AttributeSet>();
        try {
            this._eb.setScope(LDAPConnection.ONE_SCOPE);
            Query _q = new Query();
            _q.addCondition("objectclass", "top", Query.EXACT);
            List<Entry> _containers = this._eb.search(_q);
            for (Entry _ec : _containers) {
                this._eb.setScope(LDAPConnection.SUBTREE_SCOPE);
                _q = new Query();
                for (String _oc : this._group_objectclasses) {
                    _q.addCondition("objectclass", _oc, Query.EXACT);
                }
                if (_ec.hasAttribute("dc")) {
                    _q.addCondition("dc", String.valueOf(_ec.getAttribute("dc")[0]), Query.BRANCH);
                } else if (_ec.hasAttribute("ou")) {
                    _q.addCondition("ou", String.valueOf(_ec.getAttribute("ou")[0]), Query.BRANCH);
                } else if (_ec.hasAttribute("cn")) {
                    _q.addCondition("cn", String.valueOf(_ec.getAttribute("cn")[0]), Query.BRANCH);
                }
                List<Entry> _results = this._eb.search(_q);
                for (Entry _g : _results) {
                    groups.add(String.valueOf(_g.getAttribute("cn")[0]));
                    if (cal != null) {
                        if (_g.hasAttribute("modifyTimestamp")) {
                            Calendar _src_cal = getCalendarAttribute(String.valueOf(_g.getAttribute("modifyTimestamp")[0]));
                            if (cal.after(_src_cal)) {
                                continue;
                            }
                        }
                    }
                    AttributeSet attributes = new AttributeSet();
                    for (String _name : this._groupAttributeMaps.keySet()) {
                        Map<String, String> _attribute = this._groupAttributeMaps.get(_name);
                        if (_attribute.containsKey("attribute")) {
                            if (_g.hasAttribute(_name)) {
                                attributes.setAttribute(_attribute.get("attribute"), _g.getAttribute(_name));
                            }
                        }
                    }
                    if (_g.hasAttribute("member")) {
                        List<Object> _users = new ArrayList<Object>();
                        List<Object> _groups = new ArrayList<Object>();
                        for (Object chain_value : _g.getAttribute("member")) {
                            Entry _e_tmp = null;
                            try {
                                _e_tmp = this._eb.getEntry(String.valueOf(chain_value));
                            } catch (Exception _ex) {
                                continue;
                            }
                            if (_e_tmp != null) {
                                List<Object> objectclasses = Arrays.asList(_e_tmp.getAttribute("objectclass"));
                                if (objectclasses.contains("posixAccount") && _e_tmp.hasAttribute("uid")) {
                                    _users.add(_e_tmp.getAttribute("uid")[0]);
                                } else if (objectclasses.contains("posixGroup") && _e_tmp.hasAttribute("name")) {
                                    _groups.add(_e_tmp.getAttribute("cn")[0]);
                                }
                            }
                        }
                        attributes.setAttribute("userMember", _users.toArray());
                        attributes.setAttribute("groupMember", _groups.toArray());
                    } else if (_g.hasAttribute("uniqueMember")) {
                        int _users = 0, _groups = 0;
                        for (Object chain_value : _g.getAttribute("uniqueMember")) {
                            Entry _e_tmp = null;
                            try {
                                _e_tmp = this._eb.getEntry(String.valueOf(chain_value));
                            } catch (Exception _ex) {
                                continue;
                            }
                            if (_e_tmp != null) {
                                List<Object> objectclasses = Arrays.asList(_e_tmp.getAttribute("objectclass"));
                                if (objectclasses.contains("posixAccount") && _e_tmp.hasAttribute("uid")) {
                                    attributes.setAttribute("userMember" + _users, _e_tmp.getAttribute("uid"));
                                    _users++;
                                } else if (objectclasses.contains("posixGroup") && _e_tmp.hasAttribute("name")) {
                                    attributes.setAttribute("groupMember" + _groups, _e_tmp.getAttribute("name"));
                                    _groups++;
                                }
                            }
                        }
                    } else if (_g.hasAttribute("memberUid")) {
                        int _users = 0;
                        for (Object _uid : _g.getAttribute("memberUid")) {
                            attributes.setAttribute("userMember" + _users, new Object[] { String.valueOf(_uid) });
                            _users++;
                        }
                    }
                    _entries.add(attributes);
                }
            }
        } catch (Exception _ex) {
        }
        return _entries;
    }

    public AttributeSet getGroupEntry(String group) throws Exception {
        if (group == null) {
            return null;
        }
        Query _q = new Query();
        for (String _oc : this._group_objectclasses) {
            _q.addCondition("objectclass", _oc, Query.EXACT);
        }
        List<Entry> _result = this._eb.search(_q);
        if (_result != null && !_result.isEmpty()) {
            Entry _g = _result.get(0);
            return getGroupAttributes(_g);
        }
        return null;
    }

    public List<String> getProfiles(String user) throws Exception {
        return new ArrayList<String>();
    }

    private AttributeSet getUserAttributes(Entry _u) throws Exception {
        AttributeSet _user = new AttributeSet();
        for (String _name : this._userAttributeMaps.keySet()) {
            Map<String, String> _attribute = this._userAttributeMaps.get(_name);
            if (_attribute.containsKey("attribute")) {
                if (_u.hasAttribute(_name)) {
                    _user.setAttribute(_attribute.get("attribute"), _u.getAttribute(_name));
                }
            }
        }
        return _user;
    }

    public List<AttributeSet> getUserEntries(Calendar cal, List<String> users) throws Exception {
        List<AttributeSet> _entries = new ArrayList<AttributeSet>();
        try {
            this._eb.setScope(LDAPConnection.ONE_SCOPE);
            Query _q = new Query();
            _q.addCondition("objectclass", "top", Query.EXACT);
            List<Entry> _containers = this._eb.search(_q);
            for (Entry _ec : _containers) {
                this._eb.setScope(LDAPConnection.SUBTREE_SCOPE);
                _q = new Query();
                for (String _oc : this._user_objectclasses) {
                    _q.addCondition("objectclass", _oc, Query.EXACT);
                }
                if (_ec.hasAttribute("dc")) {
                    _q.addCondition("dc", String.valueOf(_ec.getAttribute("dc")[0]), Query.BRANCH);
                } else if (_ec.hasAttribute("ou")) {
                    _q.addCondition("ou", String.valueOf(_ec.getAttribute("ou")[0]), Query.BRANCH);
                } else if (_ec.hasAttribute("cn")) {
                    _q.addCondition("cn", String.valueOf(_ec.getAttribute("cn")[0]), Query.BRANCH);
                }
                List<Entry> _results = this._eb.search(_q);
                for (Entry _u : _results) {
                    if (cal != null) {
                        if (_u.hasAttribute("modifyTimestamp")) {
                            Calendar _src_cal = getCalendarAttribute(String.valueOf(_u.getAttribute("modifyTimestamp")[0]));
                            if (cal.after(_src_cal)) {
                                continue;
                            }
                        }
                    }
                    AttributeSet attributes = new AttributeSet();
                    for (String _name : this._userAttributeMaps.keySet()) {
                        Map<String, String> _attribute = this._userAttributeMaps.get(_name);
                        if (_attribute.containsKey("attribute")) {
                            if (_u.hasAttribute(_name)) {
                                attributes.setAttribute(_attribute.get("attribute"), _u.getAttribute(_name));
                            }
                        }
                    }
                    _entries.add(attributes);
                }
            }
        } catch (Exception _ex) {
        }
        return _entries;
    }

    public AttributeSet getUserEntry(String uid) throws Exception {
        if (uid == null) {
            return null;
        }
        Query _q = new Query();
        for (String _oc : this._user_objectclasses) {
            _q.addCondition("objectclass", _oc, Query.EXACT);
        }
        List<Entry> _result = this._eb.search(_q);
        if (_result != null && !_result.isEmpty()) {
            Entry _u = _result.get(0);
            return getUserAttributes(_u);
        }
        return null;
    }

    public List<AttributeSet> getUserGroups(String user) throws Exception {
        List<AttributeSet> _groups = new ArrayList<AttributeSet>();
        AttributeSet _user = getUserEntry(user);
        if (_user == null) {
            throw new Exception("user does not exists");
        }
        Query _q = new Query();
        for (String _oc : this._group_objectclasses) {
            _q.addCondition("objectclass", _oc, Query.EXACT);
        }
        _q.addCondition(this._member_attribute, _user.getAttributeFirstStringValue("dn"), Query.EXACT);
        for (Entry _g : this._eb.search(_q)) {
            _groups.add(getGroupAttributes(_g));
        }
        return _groups;
    }

    public List<String> getUserGroupNames(String user) throws Exception {
        String _attribute = null;
        List<String> names = new ArrayList<String>();
        for (Map<String, String> _map : this._groupAttributeMaps.values()) {
            if (_map.get("attribute") != null && "cn".equals(_map.get("attribute"))) {
                _attribute = _map.get("name");
            }
        }
        if (_attribute == null) {
            throw new Exception("there is no map to group [cn] attribute");
        }
        for (AttributeSet _g : getUserGroups(user)) {
            if (_g.hasAttribute(_attribute)) {
                names.add(_g.getAttributeFirstStringValue(_attribute));
            }
        }
        return names;
    }

    public void importAllEntries(String branch, File log) {
        FileOutputStream _log = null;
        try {
            _log = new FileOutputStream(log);
            _log.write("".getBytes());
        } catch (IOException _ex) {
        } finally {
            try {
                if (_log != null) {
                    _log.close();
                }
            } catch (IOException _ex) {
            }
        }
        importUserEntries(branch, log);
        importGroupEntries(branch, log);
        importGroupMembership(log);
    }

    private void importUserEntries(String branch, File log) {
        FileOutputStream _log = null;
        try {
            EntryManager _em = new EntryManager(this._local_c);
            UserManager _um = new UserManager(this._local_c);
            _log = new FileOutputStream(log, true);
            List<String> _remove_users = new ArrayList<String>();
            if (this.delete) {
                write(_log, "Reading users from local directory .. ");
                for (Entry _u : _em.getAllEntries(EntryManager.USER)) {
                    if (_u.hasAttribute("uid")) {
                        String _uid = String.valueOf(_u.getAttribute("uid")[0]);
                        if (!_uid.toLowerCase().equals("administrator") && !_uid.toLowerCase().equals("administrador")) {
                            _remove_users.add(_uid);
                        }
                    }
                }
                writeLine(_log, "done");
            }
            this._eb.setScope(LDAPConnection.ONE_SCOPE);
            Query _q = new Query();
            _q.addCondition("objectclass", "top", Query.EXACT);
            write(_log, "Reading remote base entries .. ");
            List<Entry> _containers = this._eb.search(_q);
            writeLine(_log, "done");
            for (Entry _ec : _containers) {
                writeLine(_log, "Processing container: " + _ec.getID());
                this._eb.setScope(LDAPConnection.SUBTREE_SCOPE);
                _q = new Query();
                _q.addCondition("objectclass", "account", Query.EXACT);
                _q.addCondition("uid", "*", Query.NOT_EXACT);
                if (_ec.hasAttribute("ou")) {
                    _q.addCondition("ou", String.valueOf(_ec.getAttribute("ou")[0]), Query.BRANCH);
                } else if (_ec.hasAttribute("cn") && "users".equals((String.valueOf(_ec.getAttribute("cn")[0])).toLowerCase())) {
                    _q.addCondition("cn", String.valueOf(_ec.getAttribute("cn")[0]), Query.BRANCH);
                }
                write(_log, "Reading entries from remote directory .. ");
                List<Entry> _results = this._eb.search(_q);
                writeLine(_log, "done");
                for (Entry _u : _results) {
                    if (!_u.hasAttribute("uid") || this._sc.getAdministrativeUser().equals(_u.getAttribute("uid")[0])) {
                        continue;
                    }
                    AttributeSet attributes = getUserAttributes(_u);
                    try {
                        write(_log, "Importing user [");
                        write(_log, attributes.getAttributeFirstStringValue("uid"));
                        write(_log, "  ] .. ");
                        _um.addUserEntry(attributes, branch);
                        writeLine(_log, "done");
                        if (this.delete) {
                            _remove_users.remove(attributes.getAttributeFirstStringValue("uid"));
                        }
                    } catch (Exception _ex) {
                        writeLine(_log, "error - " + _ex.getMessage());
                    }
                }
            }
            if (this.delete) {
                for (String uid : _remove_users) {
                    write(_log, "Deleting user entry [");
                    write(_log, uid);
                    write(_log, "]: ");
                    try {
                        _um.deleteUserEntry(uid);
                        writeLine(_log, "done");
                    } catch (Exception _ex) {
                        write(_log, "error - ");
                        writeLine(_log, _ex.getMessage());
                    }
                }
            }
        } catch (Exception _ex) {
            writeLine(_log, "error - " + _ex.getMessage());
        } finally {
            if (_log != null) {
                try {
                    _log.close();
                } catch (IOException _ex2) {
                }
            }
        }
    }

    private void importGroupEntries(String branch, File log) {
        FileOutputStream _log = null;
        try {
            EntryManager _em = new EntryManager(this._local_c);
            GroupManager _gm = new GroupManager(this._local_c);
            _log = new FileOutputStream(log, true);
            List<String> _remove_groups = new ArrayList<String>();
            if (this.delete) {
                write(_log, "Reading groups from local directory .. ");
                for (Entry _g : _em.getAllEntries(EntryManager.USER)) {
                    if (_g.hasAttribute("cn")) {
                        String _cn = String.valueOf(_g.getAttribute("cn")[0]);
                        if (!_cn.toLowerCase().equals(this._sc.getAdministrativeGroup(1001)) && !_cn.toLowerCase().equals(this._sc.getAdministrativeGroup(512)) && !_cn.toLowerCase().equals(this._sc.getAdministrativeGroup(513)) && !_cn.toLowerCase().equals(this._sc.getAdministrativeGroup(514)) && !_cn.toLowerCase().equals(this._sc.getAdministrativeGroup(515))) {
                            _remove_groups.add(_cn);
                        }
                    }
                }
                writeLine(_log, "done");
            }
            this._eb.setScope(LDAPConnection.ONE_SCOPE);
            Query _q = new Query();
            _q.addCondition("objectclass", "top", Query.EXACT);
            write(_log, "Reading remote base entries .. ");
            List<Entry> _containers = this._eb.search(_q);
            writeLine(_log, "done");
            for (Entry _ec : _containers) {
                writeLine(_log, "Processing container: " + _ec.getID());
                this._eb.setScope(LDAPConnection.SUBTREE_SCOPE);
                _q = new Query();
                _q.addCondition("objectclass", "posixGroup", Query.EXACT);
                if (_ec.hasAttribute("dc")) {
                    _q.addCondition("dc", String.valueOf(_ec.getAttribute("dc")[0]), Query.BRANCH);
                } else if (_ec.hasAttribute("ou")) {
                    _q.addCondition("ou", String.valueOf(_ec.getAttribute("ou")[0]), Query.BRANCH);
                } else if (_ec.hasAttribute("cn")) {
                    _q.addCondition("cn", String.valueOf(_ec.getAttribute("cn")[0]), Query.BRANCH);
                }
                write(_log, "Reading entries from remote directory .. ");
                List<Entry> _results = this._eb.search(_q);
                writeLine(_log, "done");
                for (Entry _g : _results) {
                    AttributeSet attributes = getGroupAttributes(_g);
                    try {
                        write(_log, "Importing group [");
                        write(_log, attributes.getAttributeFirstStringValue("cn"));
                        write(_log, "] .. ");
                        _gm.addGroupEntry(attributes, branch);
                        writeLine(_log, "done");
                        if (this.delete) {
                            _remove_groups.remove(attributes.getAttributeFirstStringValue("cn"));
                        }
                    } catch (Exception _ex) {
                        writeLine(_log, "error - " + _ex.getMessage());
                    }
                }
            }
            if (this.delete) {
                for (String cn : _remove_groups) {
                    write(_log, "Deleting user entry [");
                    write(_log, cn);
                    write(_log, "]: ");
                    try {
                        _gm.deleteGroupEntry(cn);
                        writeLine(_log, "done");
                    } catch (Exception _ex) {
                        write(_log, "error - ");
                        writeLine(_log, _ex.getMessage());
                    }
                }
            }
        } catch (Exception _ex) {
            writeLine(_log, "error - " + _ex.getMessage());
        } finally {
            if (_log != null) {
                try {
                    _log.close();
                } catch (IOException _ex2) {
                }
            }
        }
    }

    private void importGroupMembership(File log) {
        FileOutputStream _log = null;
        try {
            EntryManager _em = new EntryManager(this._local_c);
            GroupManager _gm = new GroupManager(this._local_c);
            UserManager _um = new UserManager(this._local_c);
            _log = new FileOutputStream(log, true);
            this._eb.setScope(LDAPConnection.ONE_SCOPE);
            Query _q = new Query();
            _q.addCondition("objectclass", "top", Query.EXACT);
            write(_log, "Reading remote base entries .. ");
            List<Entry> _containers = this._eb.search(_q);
            writeLine(_log, "done");
            for (Entry _ec : _containers) {
                writeLine(_log, "Processing container: " + _ec.getID());
                this._eb.setScope(LDAPConnection.SUBTREE_SCOPE);
                _q = new Query();
                _q.addCondition("objectclass", "group", Query.EXACT);
                if (_ec.hasAttribute("dc")) {
                    _q.addCondition("dc", String.valueOf(_ec.getAttribute("dc")[0]), Query.BRANCH);
                } else if (_ec.hasAttribute("ou")) {
                    _q.addCondition("ou", String.valueOf(_ec.getAttribute("ou")[0]), Query.BRANCH);
                } else if (_ec.hasAttribute("cn")) {
                    _q.addCondition("cn", String.valueOf(_ec.getAttribute("cn")[0]), Query.BRANCH);
                }
                write(_log, "Reading entries from remote directory .. ");
                List<Entry> _results = this._eb.search(_q);
                writeLine(_log, "done");
                for (Entry _g : _results) {
                    Entry _g_tmp = null;
                    List<String> _values_dn = new ArrayList<String>();
                    List<String> _values_uid = new ArrayList<String>();
                    List<String> _values_sid = new ArrayList<String>();
                    try {
                        _g_tmp = _gm.getGroupEntry(String.valueOf(_g.getAttribute("name")[0]));
                    } catch (Exception _ex) {
                        continue;
                    }
                    writeLine(_log, "Checking group: " + _g.getAttribute("name")[0]);
                    if (_g_tmp.hasAttribute("uniqueMember") && _g_tmp.hasAttribute("memberUid") && _g_tmp.hasAttribute("sambaSIDList")) {
                        write(_log, "Verifying members .. ");
                        for (Object _dn : _g_tmp.getAttribute("uniqueMember")) {
                            Entry _e_tmp = null;
                            try {
                                _e_tmp = _em.getEntry(String.valueOf(_dn));
                            } catch (Exception _ex) {
                                continue;
                            }
                            if (_e_tmp != null) {
                                _values_dn.add(String.valueOf(_dn));
                                if (_e_tmp.hasAttribute("uid")) {
                                    _values_uid.add(String.valueOf(_e_tmp.getAttribute("uid")[0]));
                                }
                                if (_e_tmp.hasAttribute("sambaSID")) {
                                    _values_sid.add(String.valueOf(_e_tmp.getAttribute("sambaSID")[0]));
                                }
                            }
                        }
                        writeLine(_log, "done");
                    }
                    if (_g.hasAttribute("member")) {
                        for (Object chain_value : _g.getAttribute("member")) {
                            Entry _e_tmp = null;
                            try {
                                _e_tmp = this._eb.getEntry(String.valueOf(chain_value));
                            } catch (Exception _ex) {
                                continue;
                            }
                            if (_e_tmp != null) {
                                List<Object> objectclasses = Arrays.asList(_e_tmp.getAttribute("objectclass"));
                                try {
                                    if (objectclasses.contains("person") && _e_tmp.hasAttribute("sAMAccountName")) {
                                        Entry _tmp_u = _um.getUserEntry(String.valueOf(_e_tmp.getAttribute("sAMAccountName")[0]));
                                        write(_log, "Adding user member [");
                                        write(_log, String.valueOf(_e_tmp.getAttribute("sAMAccountName")[0]));
                                        write(_log, "] .. ");
                                        if (!_values_dn.contains(_tmp_u.getID())) {
                                            _values_dn.add(_tmp_u.getID());
                                            if (_tmp_u.hasAttribute("uid")) {
                                                _values_uid.add(String.valueOf(_tmp_u.getAttribute("uid")[0]));
                                            }
                                            if (_tmp_u.hasAttribute("sambaSID")) {
                                                _values_sid.add(String.valueOf(_tmp_u.getAttribute("sambaSID")[0]));
                                            }
                                            writeLine(_log, "done");
                                        } else {
                                            writeLine(_log, "already exists");
                                        }
                                    } else if (objectclasses.contains("group") && _e_tmp.hasAttribute("name")) {
                                        write(_log, "Adding group member [");
                                        write(_log, String.valueOf(_e_tmp.getAttribute("name")[0]));
                                        write(_log, "] .. ");
                                        Entry _tmp_g = _gm.getGroupEntry(String.valueOf(_e_tmp.getAttribute("name")[0]));
                                        if (!_values_dn.contains(_tmp_g.getID())) {
                                            _values_dn.add(_tmp_g.getID());
                                            writeLine(_log, "done");
                                        } else {
                                            writeLine(_log, "already exists");
                                        }
                                    }
                                } catch (Exception _ex) {
                                    writeLine(_log, "error");
                                    continue;
                                }
                            }
                        }
                    } else if (_g.hasAttribute("uniqueMember")) {
                        for (Object chain_value : _g.getAttribute("uniqueMember")) {
                            Entry _e_tmp = null;
                            try {
                                _e_tmp = this._eb.getEntry(String.valueOf(chain_value));
                            } catch (Exception _ex) {
                                continue;
                            }
                            if (_e_tmp != null) {
                                List<Object> objectclasses = Arrays.asList(_e_tmp.getAttribute("objectclass"));
                                try {
                                    if (objectclasses.contains("posixAccount") && _e_tmp.hasAttribute("uid")) {
                                        Entry _tmp_u = _um.getUserEntry(String.valueOf(_e_tmp.getAttribute("uid")[0]));
                                        write(_log, "Adding user member [");
                                        write(_log, String.valueOf(_e_tmp.getAttribute("uid")[0]));
                                        write(_log, "] .. ");
                                        if (!_values_dn.contains(_tmp_u.getID())) {
                                            _values_dn.add(_tmp_u.getID());
                                            if (_tmp_u.hasAttribute("uid")) {
                                                _values_uid.add(String.valueOf(_tmp_u.getAttribute("uid")[0]));
                                            }
                                            if (_tmp_u.hasAttribute("sambaSID")) {
                                                _values_sid.add(String.valueOf(_tmp_u.getAttribute("sambaSID")[0]));
                                            }
                                            writeLine(_log, "done");
                                        } else {
                                            writeLine(_log, "already exists");
                                        }
                                    } else if (objectclasses.contains("posixGroup") && _e_tmp.hasAttribute("cn")) {
                                        write(_log, "Adding group member [");
                                        write(_log, String.valueOf(_e_tmp.getAttribute("cn")[0]));
                                        write(_log, "] .. ");
                                        Entry _tmp_g = _gm.getGroupEntry(String.valueOf(_e_tmp.getAttribute("cn")[0]));
                                        if (!_values_dn.contains(_tmp_g.getID())) {
                                            _values_dn.add(_tmp_g.getID());
                                            writeLine(_log, "done");
                                        } else {
                                            writeLine(_log, "already exists");
                                        }
                                    }
                                } catch (Exception _ex) {
                                    writeLine(_log, "error");
                                    continue;
                                }
                            }
                        }
                    } else if (_g.hasAttribute("memberUid")) {
                        for (Object _value : _g.getAttribute("memberUid")) {
                            try {
                                Entry _tmp_u = _um.getUserEntry(String.valueOf(_value));
                                write(_log, "Adding user member [");
                                write(_log, String.valueOf(_value));
                                write(_log, "] .. ");
                                if (!_values_dn.contains(_tmp_u.getID())) {
                                    _values_dn.add(_tmp_u.getID());
                                    if (_tmp_u.hasAttribute("uid")) {
                                        _values_uid.add(String.valueOf(_tmp_u.getAttribute("uid")[0]));
                                    }
                                    if (_tmp_u.hasAttribute("sambaSID")) {
                                        _values_sid.add(String.valueOf(_tmp_u.getAttribute("sambaSID")[0]));
                                    }
                                    writeLine(_log, "done");
                                } else {
                                    writeLine(_log, "already exists");
                                }
                            } catch (Exception _ex) {
                            }
                        }
                    }
                    if (_values_dn.isEmpty() && _values_uid.isEmpty() && _values_sid.isEmpty()) {
                        continue;
                    }
                    if (!_values_dn.isEmpty()) {
                        _g_tmp.setAttribute("uniqueMember", _values_dn.toArray());
                    }
                    if (!_values_uid.isEmpty()) {
                        _g_tmp.setAttribute("memberUid", _values_uid.toArray());
                    }
                    if (_values_sid.size() > 0) {
                        _g_tmp.setAttribute("sambaSIDList", _values_sid.toArray());
                    }
                    try {
                        write(_log, "Updating group members .. ");
                        _em.updateEntry(_g_tmp);
                        writeLine(_log, "done");
                    } catch (Exception _ex) {
                        writeLine(_log, "error - " + _ex.getMessage());
                    }
                }
            }
        } catch (Exception _ex) {
            writeLine(_log, "error - " + _ex.getMessage());
        } finally {
            if (_log != null) {
                try {
                    _log.close();
                } catch (IOException _ex2) {
                }
            }
        }
    }

    private void loadUserAttributes(Entry _e, AttributeSet attributes) throws Exception {
        _e.setAttribute("objectclass", this._user_objectclasses.toArray());
        for (String _name : this._userAttributeMaps.keySet()) {
            Map<String, String> _attribute = this._userAttributeMaps.get(_name);
            if (_attribute.containsKey("value")) {
                _e.setAttribute(_name, _attribute.get("value"));
            } else if (_attribute.containsKey("attribute")) {
                if ("password".equals(_attribute.get("attribute"))) {
                    _e.setAttribute(_name, getEncriptedPassword(attributes.getAttributeFirstStringValue("password"), true));
                } else {
                    _e.setAttribute(_name, attributes.getAttribute(_attribute.get("attribute")));
                }
            }
        }
    }

    private void loadGroupAttributes(Entry _e, AttributeSet attributes) throws Exception {
        _e.setAttribute("objectclass", this._group_objectclasses.toArray());
        for (String _name : this._groupAttributeMaps.keySet()) {
            Map<String, String> _attribute = this._groupAttributeMaps.get(_name);
            if (_attribute.containsKey("value")) {
                _e.setAttribute(_name, _attribute.get("value"));
            } else if (_attribute.containsKey("attribute")) {
                _e.setAttribute(_name, attributes.getAttribute(_attribute.get("attribute")));
            }
        }
    }

    public void removeUserMember(String group, String user) throws Exception {
        AttributeSet _group = getGroupEntry(group);
        AttributeSet _user = getUserEntry(user);
        if (_group == null) {
            throw new Exception("group does not exists");
        }
        if (_user == null) {
            throw new Exception("user does not exists");
        }
        this._eb.removeEntryAttribute(_group.getAttributeFirstStringValue("dn"), "member", _user.getAttributeFirstStringValue("dn"));
    }

    public List<AttributeSet> searchUserEntry(String user) throws Exception {
        List<AttributeSet> _users = new ArrayList<AttributeSet>();
        if (user == null) {
            return _users;
        }
        Query _q = new Query();
        for (String _oc : this._user_objectclasses) {
            _q.addCondition("objectclass", _oc, Query.EXACT);
        }
        _q.addCondition("uid", user, Query.CONTAINS);
        for (Entry _u : this._eb.sortedSearch(_q, "cn")) {
            AttributeSet _user = new AttributeSet();
            _user.setAttribute("dn", _u.getID());
            for (String _name : _u.getAttributeNames()) {
                _user.setAttribute(_name, _u.getAttribute(_name));
            }
            _users.add(_user);
        }
        return _users;
    }

    public void setRepository(RepositoryManager rm, String repository) throws Exception {
        this._rm = rm;
        this._repositoryName = repository;
        this._attributes = this._rm.getRepositoryAttributes(this._repositoryName);
        this._groupAttributeMaps = this._rm.getRepositoryGroupAttributeMap(this._repositoryName);
        this._userAttributeMaps = this._rm.getRepositoryUserAttributeMap(this._repositoryName);
    }

    public void storeGroupEntry(String group, AttributeSet attributes) throws Exception {
        storeGroupEntry(group, attributes, STORE_FLEXIBLE);
    }

    private void storeGroupEntry(String group, AttributeSet attributes, int type) throws Exception {
        AttributeSet _group = getGroupEntry(group);
        switch(type) {
            case STORE_FLEXIBLE:
                {
                    if (_group == null) {
                        Entry _e = null;
                        for (Map<String, String> _map : this._groupAttributeMaps.values()) {
                            if (_map.get("attribute") != null && "cn".equals(_map.get("attribute"))) {
                                _e = new LDAPEntry(_map.get("name") + "=" + group + this._group_suffix);
                            }
                        }
                        if (_e == null) {
                            throw new Exception("there is no map to group [cn] attribute");
                        }
                        loadGroupAttributes(_e, attributes);
                        this._eb.addEntry(_e);
                    } else {
                        Entry _e = this._eb.getEntry(_group.getAttributeFirstStringValue("dn"));
                        loadGroupAttributes(_e, attributes);
                        this._eb.updateEntry(_e);
                    }
                }
                break;
            case STORE_ADD_ONLY:
                {
                    if (_group != null) {
                        throw new Exception("group already exists");
                    }
                    Entry _e = null;
                    for (Map<String, String> _map : this._groupAttributeMaps.values()) {
                        if (_map.get("attribute") != null && "cn".equals(_map.get("attribute"))) {
                            _e = new LDAPEntry(_map.get("name") + "=" + group + this._group_suffix);
                        }
                    }
                    if (_e == null) {
                        throw new Exception("there is no map to group [cn] attribute");
                    }
                    loadGroupAttributes(_e, attributes);
                    this._eb.addEntry(_e);
                }
                break;
            case STORE_UPDATE_ONLY:
                {
                    if (_group == null) {
                        throw new Exception("group does not exists");
                    }
                    Entry _e = this._eb.getEntry(_group.getAttributeFirstStringValue("dn"));
                    loadGroupAttributes(_e, attributes);
                    this._eb.updateEntry(_e);
                }
                break;
        }
    }

    public void storeProfile(String user, String profile) throws Exception {
    }

    public void storeUserEntry(String uid, AttributeSet attributes) throws Exception {
        storeUserEntry(uid, attributes, STORE_FLEXIBLE);
    }

    private void storeUserEntry(String user, AttributeSet attributes, int type) throws Exception {
        AttributeSet _user = getUserEntry(user);
        switch(type) {
            case STORE_FLEXIBLE:
                {
                    if (_user == null) {
                        Entry _e = null;
                        for (Map<String, String> _map : this._userAttributeMaps.values()) {
                            if (_map.get("attribute") != null && "uid".equals(_map.get("attribute"))) {
                                _e = new LDAPEntry(_map.get("name") + "=" + user + this._user_suffix);
                            }
                        }
                        if (_e == null) {
                            throw new Exception("there is no map to user [uid] attribute");
                        }
                        loadUserAttributes(_e, attributes);
                        this._eb.addEntry(_e);
                    } else {
                        Entry _e = this._eb.getEntry(_user.getAttributeFirstStringValue("dn"));
                        loadUserAttributes(_e, attributes);
                        this._eb.updateEntry(_e);
                    }
                }
                break;
            case STORE_ADD_ONLY:
                {
                    if (_user != null) {
                        throw new Exception("user already exists");
                    }
                    Entry _e = null;
                    for (Map<String, String> _map : this._userAttributeMaps.values()) {
                        if (_map.get("attribute") != null && "uid".equals(_map.get("attribute"))) {
                            _e = new LDAPEntry(_map.get("name") + "=" + user + this._user_suffix);
                        }
                    }
                    if (_e == null) {
                        throw new Exception("there is no map to user [uid] attribute");
                    }
                    loadUserAttributes(_e, attributes);
                    this._eb.addEntry(_e);
                }
                break;
            case STORE_UPDATE_ONLY:
                {
                    if (_user == null) {
                        throw new Exception("user does not exists");
                    }
                    Entry _e = this._eb.getEntry(_user.getAttributeFirstStringValue("dn"));
                    loadUserAttributes(_e, attributes);
                    this._eb.updateEntry(_e);
                }
                break;
        }
    }

    private static void writeLine(FileOutputStream _fos, String message) {
        write(_fos, message.concat("\n"));
    }

    private static void write(FileOutputStream _fos, String message) {
        if (_fos != null) {
            try {
                _fos.write(message.getBytes());
                _fos.write("\n".getBytes());
            } catch (IOException _ex) {
            }
        }
    }
}
