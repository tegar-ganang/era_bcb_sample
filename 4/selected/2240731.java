package com.whitebearsolutions.imagine.wbsagnitio.replica;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import com.whitebearsolutions.directory.Entry;
import com.whitebearsolutions.directory.EntryBase;
import com.whitebearsolutions.directory.Query;
import com.whitebearsolutions.directory.ldap.LDAPConnection;
import com.whitebearsolutions.directory.ldap.LDAPEntry;
import com.whitebearsolutions.imagine.wbsagnitio.NetworkManager;
import com.whitebearsolutions.imagine.wbsagnitio.configuration.SystemConfiguration;
import com.whitebearsolutions.imagine.wbsagnitio.configuration.WBSAgnitioConfiguration;
import com.whitebearsolutions.imagine.wbsagnitio.directory.EntryManager;
import com.whitebearsolutions.imagine.wbsagnitio.directory.GroupManager;
import com.whitebearsolutions.imagine.wbsagnitio.directory.UserManager;
import com.whitebearsolutions.imagine.wbsagnitio.util.CharacterEncode;
import com.whitebearsolutions.util.Configuration;

public class ExternalDirectoryMSAD implements ExternalDirectory {

    private static final int STORE_FLEXIBLE = 1;

    private static final int STORE_ADD_ONLY = 2;

    private static final int STORE_UPDATE_ONLY = 3;

    private static final int UF_ACCOUNTDISABLE = 0x0002;

    private static final int UF_PASSWD_NOTREQD = 0x0020;

    @SuppressWarnings("unused")
    private static final int UF_PASSWD_CANT_CHANGE = 0x0040;

    private static final int UF_NORMAL_ACCOUNT = 0x0200;

    @SuppressWarnings("unused")
    private static final int UF_DONT_EXPIRE_PASSWD = 0x10000;

    private static final int UF_PASSWORD_EXPIRED = 0x800000;

    private Configuration _local_c;

    private Configuration _remote_c;

    private SystemConfiguration _sc;

    private EntryBase _eb;

    private boolean delete = false;

    public ExternalDirectoryMSAD(Configuration _c) throws Exception {
        this._local_c = new Configuration(new File(WBSAgnitioConfiguration.getConfigurationFile()));
        _sc = new SystemConfiguration(_local_c);
        this._remote_c = _c;
        this._sc = new SystemConfiguration(this._local_c);
        this._eb = new EntryBase(this._remote_c);
        if (this._remote_c.checkProperty("ldap.replica.delete", "true")) {
            this.delete = true;
        }
    }

    public void addUserEntry(String samAccountName, Map<String, String> attributes) throws Exception {
        storeUserEntry(samAccountName, attributes, STORE_ADD_ONLY);
    }

    public void updateUserEntry(String samAccountName, Map<String, String> attributes) throws Exception {
        storeUserEntry(samAccountName, attributes, STORE_UPDATE_ONLY);
    }

    private void storeUserEntry(String samAccountName, Map<String, String> attributes, int type) throws Exception {
        Entry _e = getUserEntry(samAccountName);
        String cn = samAccountName;
        if (attributes.get("cn") == null) {
            attributes.put("cn", samAccountName);
        } else {
            cn = attributes.get("cn");
        }
        switch(type) {
            case STORE_FLEXIBLE:
                {
                    if (_e == null) {
                        _e = new LDAPEntry("CN=" + cn + ",CN=Users," + this._remote_c.getProperty("ldap.basedn"));
                        _e.setAttribute("samAccountName", samAccountName);
                        _e.setAttribute("userPrincipalName", samAccountName + "@" + NetworkManager.getDomain(this._remote_c.getProperty("ldap.basedn")));
                        loadUserAttributes(_e, attributes);
                        if (attributes.get("accountStatus") != null && "disabled".equalsIgnoreCase(String.valueOf(attributes.get("accountStatus")))) {
                            _e.setAttribute("userAccountControl", Integer.toString(UF_NORMAL_ACCOUNT + UF_PASSWD_NOTREQD + UF_PASSWORD_EXPIRED + UF_ACCOUNTDISABLE));
                        } else {
                            _e.setAttribute("userAccountControl", Integer.toString(UF_NORMAL_ACCOUNT + UF_PASSWD_NOTREQD + UF_PASSWORD_EXPIRED));
                        }
                        if (attributes.containsKey("password")) {
                            _e.setAttribute("unicodePwd", attributes.get("password"));
                        }
                        this._eb.addEntryMSAD(_e);
                    } else {
                        loadUserAttributes(_e, attributes);
                        if (attributes.get("accountStatus") != null && "disabled".equalsIgnoreCase(String.valueOf(attributes.get("accountStatus")))) {
                            _e.setAttribute("userAccountControl", Integer.toString(UF_NORMAL_ACCOUNT + UF_PASSWD_NOTREQD + UF_PASSWORD_EXPIRED + UF_ACCOUNTDISABLE));
                        } else {
                            _e.setAttribute("userAccountControl", Integer.toString(UF_NORMAL_ACCOUNT + UF_PASSWD_NOTREQD + UF_PASSWORD_EXPIRED));
                        }
                        if (attributes.containsKey("password")) {
                            _e.setAttribute("unicodePwd", attributes.get("password"));
                        }
                        this._eb.updateEntryMSAD(_e);
                    }
                }
                break;
            case STORE_ADD_ONLY:
                {
                    if (_e != null) {
                        throw new Exception("user already exists");
                    }
                    _e = new LDAPEntry("CN=" + cn + ",CN=Users," + this._remote_c.getProperty("ldap.basedn"));
                    _e.setAttribute("samAccountName", samAccountName);
                    _e.setAttribute("userPrincipalName", samAccountName + "@" + NetworkManager.getDomain(this._remote_c.getProperty("ldap.basedn")));
                    loadUserAttributes(_e, attributes);
                    if (attributes.get("accountStatus") != null && "disabled".equalsIgnoreCase(String.valueOf(attributes.get("accountStatus")))) {
                        _e.setAttribute("userAccountControl", Integer.toString(UF_NORMAL_ACCOUNT + UF_PASSWD_NOTREQD + UF_PASSWORD_EXPIRED + UF_ACCOUNTDISABLE));
                    } else {
                        _e.setAttribute("userAccountControl", Integer.toString(UF_NORMAL_ACCOUNT + UF_PASSWD_NOTREQD + UF_PASSWORD_EXPIRED));
                    }
                    if (attributes.containsKey("password")) {
                        _e.setAttribute("unicodePwd", attributes.get("password"));
                    }
                    this._eb.addEntryMSAD(_e);
                }
                break;
            case STORE_UPDATE_ONLY:
                {
                    if (_e == null) {
                        throw new Exception("user does not exists");
                    }
                    loadUserAttributes(_e, attributes);
                    if (attributes.get("accountStatus") != null && "disabled".equalsIgnoreCase(String.valueOf(attributes.get("accountStatus")))) {
                        _e.setAttribute("userAccountControl", Integer.toString(UF_NORMAL_ACCOUNT + UF_PASSWD_NOTREQD + UF_PASSWORD_EXPIRED + UF_ACCOUNTDISABLE));
                    } else {
                        _e.setAttribute("userAccountControl", Integer.toString(UF_NORMAL_ACCOUNT + UF_PASSWD_NOTREQD + UF_PASSWORD_EXPIRED));
                    }
                    if (attributes.containsKey("password")) {
                        _e.setAttribute("unicodePwd", attributes.get("password"));
                    }
                    this._eb.updateEntryMSAD(_e);
                }
                break;
        }
    }

    public void storeUserEntry(String samAccountName, Map<String, String> attributes) throws Exception {
        storeUserEntry(samAccountName, attributes, STORE_FLEXIBLE);
    }

    private void storeGroupEntry(String group, Map<String, String> attributes, int type) throws Exception {
        Entry _e = getGroupEntry(group);
        String cn = group;
        if (attributes.get("cn") == null) {
            attributes.put("cn", group);
        } else {
            cn = attributes.get("cn");
        }
        switch(type) {
            case STORE_FLEXIBLE:
                {
                    if (_e == null) {
                        _e = new LDAPEntry("CN=" + cn + ",CN=Users," + this._remote_c.getProperty("ldap.basedn"));
                        _e.setAttribute("name", group);
                        loadGroupAttributes(_e, attributes);
                        this._eb.addEntryMSAD(_e);
                    } else {
                        loadGroupAttributes(_e, attributes);
                        this._eb.updateEntryMSAD(_e);
                    }
                }
                break;
            case STORE_ADD_ONLY:
                {
                    if (_e != null) {
                        throw new Exception("group already exists");
                    }
                    _e = new LDAPEntry("CN=" + cn + ",CN=Users," + this._remote_c.getProperty("ldap.basedn"));
                    _e.setAttribute("name", group);
                    loadGroupAttributes(_e, attributes);
                    this._eb.addEntryMSAD(_e);
                }
                break;
            case STORE_UPDATE_ONLY:
                {
                    if (_e == null) {
                        throw new Exception("group does not exists");
                    }
                    loadGroupAttributes(_e, attributes);
                    this._eb.updateEntryMSAD(_e);
                }
                break;
        }
    }

    public void storeGroupEntry(String group, Map<String, String> attributes) throws Exception {
        storeGroupEntry(group, attributes, STORE_FLEXIBLE);
    }

    public Entry getUserEntry(String samAccountName) throws Exception {
        if (samAccountName == null) {
            return null;
        }
        Query _q = new Query();
        _q.addCondition("objectclass", "person", Query.EXACT);
        _q.addCondition("sAMAccountName", samAccountName, Query.EXACT);
        List<Entry> _result = this._eb.search(_q);
        if (_result != null && !_result.isEmpty()) {
            return _result.get(0);
        }
        return null;
    }

    public Entry getGroupEntry(String group) throws Exception {
        if (group == null) {
            return null;
        }
        Query _q = new Query();
        _q.addCondition("objectclass", "group", Query.EXACT);
        _q.addCondition("name", group, Query.EXACT);
        List<Entry> _result = this._eb.search(_q);
        if (_result != null && !_result.isEmpty()) {
            return _result.get(0);
        }
        return null;
    }

    public List<Entry> getUserGroups(String samAccountName) throws Exception {
        List<Entry> groups = new ArrayList<Entry>();
        Entry _e = getUserEntry(samAccountName);
        if (_e == null) {
            throw new Exception("user identifier [" + samAccountName + "] not found");
        }
        if (_e.hasAttribute("memberOf")) {
            for (Object _oe : _e.getAttribute("memberOf")) {
                groups.add(this._eb.getEntry(String.valueOf(_oe)));
            }
        }
        return groups;
    }

    public List<String> getUserGroupNames(String samAccountName) throws Exception {
        List<String> names = new ArrayList<String>();
        for (Entry g : getUserGroups(samAccountName)) {
            if (g.hasAttribute("cn")) {
                names.add(String.valueOf(g.getAttribute("cn")[0]));
            } else if (g.hasAttribute("name")) {
                names.add(String.valueOf(g.getAttribute("name")[0]));
            }
        }
        return names;
    }

    public void deleteUserEntry(String samAccountName) throws Exception {
        Entry _e = getUserEntry(samAccountName);
        if (_e == null) {
            throw new Exception("user does not exists");
        }
        this._eb.removeEntry(_e.getID());
    }

    public void deleteGroupEntry(String name) throws Exception {
        Entry _e = getGroupEntry(name);
        if (_e == null) {
            throw new Exception("group does not exists");
        }
        this._eb.removeEntry(_e.getID());
    }

    public void memberUserAdd(String group, String uid) throws Exception {
        Entry _e = getGroupEntry(group);
        Entry _u = getUserEntry(uid);
        if (_e == null) {
            throw new Exception("group does not exists");
        }
        if (_u == null) {
            throw new Exception("user does not exists");
        }
        this._eb.addEntryAttribute(_e.getID(), "member", _u.getID());
    }

    public void memberUserRemove(String group, String uid) throws Exception {
        Entry _e = getGroupEntry(group);
        Entry _u = getUserEntry(uid);
        if (_e == null) {
            throw new Exception("group does not exists");
        }
        if (_u == null) {
            throw new Exception("user does not exists");
        }
        this._eb.removeEntryAttribute(_e.getID(), "member", _u.getID());
    }

    public void importEntries(File log) {
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
        importUserEntries(log);
        importGroupEntries(log);
        importGroupMembership(log);
    }

    private void importUserEntries(File log) {
        FileOutputStream _log = null;
        try {
            NetworkManager _nm = new NetworkManager(this._local_c);
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
                _q.addCondition("objectclass", "person", Query.EXACT);
                _q.addCondition("objectclass", "computer", Query.NOT_EXACT);
                if (_ec.hasAttribute("ou")) {
                    _q.addCondition("ou", String.valueOf(_ec.getAttribute("ou")[0]), Query.BRANCH);
                } else if (_ec.hasAttribute("cn")) {
                    _q.addCondition("cn", String.valueOf(_ec.getAttribute("cn")[0]), Query.BRANCH);
                }
                write(_log, "Reading entries from remote directory .. ");
                List<Entry> _results = this._eb.search(_q);
                writeLine(_log, "done");
                for (Entry _u : _results) {
                    if (!_u.hasAttribute("sAMAccountName") || !_u.hasAttribute("cn") || this._sc.getAdministrativeUser().equals(_u.getAttribute("sAMAccountName")[0])) {
                        continue;
                    }
                    if (_um.userExists(String.valueOf(_u.getAttribute("sAMAccountName")[0]))) {
                        write(_log, "User already exists [");
                        write(_log, String.valueOf(_u.getAttribute("sAMAccountName")[0]));
                        writeLine(_log, "] .. ignored");
                        continue;
                    }
                    Map<String, Object> attributes = new HashMap<String, Object>();
                    String chain = null;
                    List<String> _values = new ArrayList<String>();
                    if (_u.hasAttribute("department")) {
                        _values.add(String.valueOf(_u.getAttribute("department")[0]));
                    }
                    if (_u.hasAttribute("memberOf")) {
                        Object[] chain_values = (Object[]) _u.getAttribute("memberOf");
                        for (int j = chain_values.length; --j >= 0; ) {
                            chain = (String) chain_values[j];
                            if (chain.toLowerCase().indexOf("ou=groups") == -1) {
                                while (chain.toLowerCase().startsWith("ou=") || chain.toLowerCase().startsWith("cn=")) {
                                    if (_values.indexOf(chain.substring(0, chain.indexOf(",")).substring(chain.indexOf("=") + 1)) == -1) {
                                        String val_tmp = chain.substring(0, chain.indexOf(",")).substring(chain.indexOf("=") + 1);
                                        _values.add(val_tmp.toLowerCase());
                                    }
                                    chain = chain.substring(chain.indexOf(",") + 1).trim();
                                }
                            }
                        }
                    }
                    if (_values.size() > 0) {
                        attributes.put("ou", _values.toArray());
                    }
                    try {
                        long _UAC = Long.parseLong(String.valueOf(_u.getAttribute("userAccountControl")[0]));
                        if ((_UAC & UF_ACCOUNTDISABLE) == UF_ACCOUNTDISABLE) {
                            attributes.put("accountStatus", "disabled");
                        }
                    } catch (NumberFormatException _ex) {
                    }
                    attributes.put("uid", _u.getAttribute("sAMAccountName")[0]);
                    attributes.put("cn", _u.getAttribute("cn")[0]);
                    attributes.put("gecos", CharacterEncode.toASCII(String.valueOf(_u.getAttribute("cn")[0])));
                    attributes.put("password", "12345");
                    if (_u.hasAttribute("givenName")) {
                        attributes.put("givenName", _u.getAttribute("givenName")[0]);
                    } else {
                        attributes.put("givenName", _u.getAttribute("cn")[0]);
                    }
                    if (_u.hasAttribute("sn")) {
                        attributes.put("sn", _u.getAttribute("sn")[0]);
                    } else {
                        attributes.put("sn", _u.getAttribute("cn")[0]);
                    }
                    if (_u.hasAttribute("employeetype")) {
                        attributes.put("employeeType", _u.getAttribute("employeetype")[0]);
                    }
                    if (_u.hasAttribute("displayName")) {
                        attributes.put("displayName", _u.getAttribute("displayName")[0]);
                    } else {
                        attributes.put("displayName", _u.getAttribute("cn")[0]);
                    }
                    if (_u.hasAttribute("title")) {
                        attributes.put("title", _u.getAttribute("title")[0]);
                    }
                    if (_u.hasAttribute("company")) {
                        attributes.put("o", _u.getAttribute("company")[0]);
                    }
                    if (_u.hasAttribute("description")) {
                        attributes.put("description", _u.getAttribute("description")[0]);
                    }
                    if (_u.hasAttribute("mail")) {
                        attributes.put("maildrop", _u.getAttribute("mail")[0]);
                        attributes.put("mail", _u.getAttribute("mail")[0]);
                    } else {
                        attributes.put("maildrop", attributes.get("uid") + "@" + _nm.getDomain());
                        attributes.put("mail", attributes.get("uid") + "@" + _nm.getDomain());
                    }
                    if (_u.hasAttribute("telephoneNumber")) {
                        attributes.put("telephoneNumber", _u.getAttribute("telephoneNumber")[0]);
                    }
                    if (_u.hasAttribute("facsimileTelephoneNumber")) {
                        attributes.put("facsimileTelephoneNumber", _u.getAttribute("facsimileTelephoneNumber")[0]);
                    }
                    if (_u.hasAttribute("st")) {
                        attributes.put("st", _u.getAttribute("st")[0]);
                    }
                    if (_u.hasAttribute("l")) {
                        attributes.put("l", _u.getAttribute("l")[0]);
                    }
                    if (_u.hasAttribute("homeDrive")) {
                        attributes.put("sambaHomeDrive", _u.getAttribute("homeDrive")[0]);
                    } else {
                        if (this._local_c.getProperty("samba.home.drive") != null) {
                            attributes.put("sambaHomeDrive", this._local_c.getProperty("samba.home.drive"));
                        }
                    }
                    if (_u.hasAttribute("homeDirectory")) {
                        attributes.put("sambaHomePath", _u.getAttribute("homeDirectory")[0]);
                    } else {
                        if (this._local_c.getProperty("samba.home.server") != null) {
                            attributes.put("sambaHomePath", "\\\\" + this._local_c.getProperty("samba.home.server") + "\\" + _u.getAttribute("sAMAccountName")[0]);
                        }
                    }
                    if (_u.hasAttribute("scriptPath")) {
                        attributes.put("sambaLogonScript", _u.getAttribute("scriptPath")[0]);
                    }
                    String _branch = null;
                    if (this._remote_c.getProperty("ldap.replica.container") != null && !this._remote_c.getProperty("ldap.replica.container").isEmpty()) {
                        _branch = this._remote_c.getProperty("ldap.replica.container");
                    }
                    try {
                        write(_log, "Importing user [");
                        write(_log, String.valueOf(attributes.get("uid")));
                        write(_log, "] .. ");
                        _um.addUserEntry(attributes, _branch);
                        writeLine(_log, "done");
                        if (this.delete) {
                            _remove_users.remove(attributes.get("uid"));
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

    private void importGroupEntries(File log) {
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
                if (_ec.hasAttribute("cn") && "Builtin".equals(String.valueOf(_ec.getAttribute("cn")[0]))) {
                    continue;
                }
                writeLine(_log, "Processing container: " + _ec.getID());
                this._eb.setScope(LDAPConnection.SUBTREE_SCOPE);
                _q = new Query();
                _q.addCondition("objectclass", "group", Query.EXACT);
                if (_ec.hasAttribute("ou")) {
                    _q.addCondition("ou", String.valueOf(_ec.getAttribute("ou")[0]), Query.BRANCH);
                } else if (_ec.hasAttribute("cn")) {
                    _q.addCondition("cn", String.valueOf(_ec.getAttribute("cn")[0]), Query.BRANCH);
                }
                write(_log, "Reading entries from remote directory .. ");
                List<Entry> _results = this._eb.search(_q);
                writeLine(_log, "done");
                for (Entry _g : _results) {
                    if (!_g.hasAttribute("name") || "Users".equals(_g.getAttribute("name")[0]) || "Pre-Windows 2000 Compatible Access".equals(_g.getAttribute("name")[0]) || "DnsUpdateProxy".equals(_g.getAttribute("name")[0])) {
                        continue;
                    }
                    Map<String, Object> attributes = new HashMap<String, Object>();
                    attributes.put("cn", _g.getAttribute("name")[0]);
                    if (_g.hasAttribute("displayName")) {
                        attributes.put("displayName", _g.getAttribute("displayName")[0]);
                    } else {
                        attributes.put("displayName", _g.getAttribute("cn")[0]);
                    }
                    if (_g.hasAttribute("description")) {
                        attributes.put("description", _g.getAttribute("description")[0]);
                    }
                    String _branch = null;
                    if (this._remote_c.getProperty("ldap.replica.container") != null && !this._remote_c.getProperty("ldap.replica.container").isEmpty()) {
                        _branch = this._remote_c.getProperty("ldap.replica.container");
                    }
                    try {
                        write(_log, "Importing group [");
                        write(_log, String.valueOf(attributes.get("cn")));
                        write(_log, "] .. ");
                        _gm.addGroupEntry(attributes, _branch);
                        writeLine(_log, "done");
                        if (this.delete) {
                            _remove_groups.remove(attributes.get("cn"));
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
                if (_ec.hasAttribute("cn") && "Builtin".equals(String.valueOf(_ec.getAttribute("cn")[0]))) {
                    continue;
                }
                writeLine(_log, "Processing container: " + _ec.getID());
                this._eb.setScope(LDAPConnection.SUBTREE_SCOPE);
                _q = new Query();
                _q.addCondition("objectclass", "group", Query.EXACT);
                if (_ec.hasAttribute("ou")) {
                    _q.addCondition("ou", String.valueOf(_ec.getAttribute("ou")[0]), Query.BRANCH);
                } else if (_ec.hasAttribute("cn")) {
                    _q.addCondition("cn", String.valueOf(_ec.getAttribute("cn")[0]), Query.BRANCH);
                }
                write(_log, "Reading entries from remote directory .. ");
                List<Entry> _results = this._eb.search(_q);
                writeLine(_log, "done");
                for (Entry _g : _results) {
                    if (!_g.hasAttribute("name") || "Users".equals(_g.getAttribute("name")[0]) || "Pre-Windows 2000 Compatible Access".equals(_g.getAttribute("name")[0]) || "DnsUpdateProxy".equals(_g.getAttribute("name")[0])) {
                        continue;
                    }
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
                            if (String.valueOf(chain_value).startsWith("CN=S-")) {
                                continue;
                            }
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

    public void exportEntries(File log) {
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
    }

    private void exportUserEntries(File log) {
        FileOutputStream _log = null;
        try {
            _log = new FileOutputStream(log, true);
            List<String> _remove_users = new ArrayList<String>();
            if (this.delete) {
                write(_log, "Reading users from remote directory .. ");
                Query _q = new Query();
                _q.addCondition("objectclass", "user", Query.EXACT);
                for (Entry _u : this._eb.search(_q)) {
                    if (_u.hasAttribute("sAMAccountName")) {
                        String _uid = String.valueOf(_u.getAttribute("sAMAccountName")[0]);
                        if (!_uid.toLowerCase().equals("administrator") && !_uid.toLowerCase().equals("administrador")) {
                            _remove_users.add(_uid);
                        }
                    }
                }
                writeLine(_log, "done");
            }
            EntryManager _em = new EntryManager(this._local_c);
            write(_log, "Reading users from local directory .. ");
            List<Entry> _users = _em.getAllEntries(EntryManager.USER);
            writeLine(_log, "done");
            for (Entry _u : _users) {
                if (this._sc.getAdministrativeUser().equals(String.valueOf(_u.getAttribute("uid")[0]))) {
                    continue;
                }
                Map<String, String> attributes = new HashMap<String, String>();
                try {
                    attributes.put("cn", String.valueOf(_u.getAttribute("cn")[0]));
                    attributes.put("sAMAccountName", String.valueOf(_u.getAttribute("uid")[0]));
                    attributes.put("givenName", String.valueOf(_u.getAttribute("givenName")[0]));
                    attributes.put("sn", String.valueOf(_u.getAttribute("sn")[0]));
                    if (_u.hasAttribute("maildrop")) {
                        attributes.put("mail", String.valueOf(_u.getAttribute("maildrop")[0]));
                    }
                    if (_u.hasAttribute("o")) {
                        attributes.put("organization", String.valueOf(_u.getAttribute("o")[0]));
                    }
                    if (_u.hasAttribute("ou")) {
                        attributes.put("department", String.valueOf(_u.getAttribute("ou")[0]));
                    }
                    if (_u.hasAttribute("telephoneNumber")) {
                        attributes.put("telephoneNumber", String.valueOf(_u.getAttribute("telephoneNumber")[0]));
                    }
                    if (_u.hasAttribute("facsimileTelephoneNumber")) {
                        attributes.put("facsimileTelephoneNumber", String.valueOf(_u.getAttribute("facsimileTelephoneNumber")[0]));
                    }
                    if (_u.hasAttribute("st")) {
                        attributes.put("st", String.valueOf(_u.getAttribute("st")[0]));
                    }
                    if (_u.hasAttribute("l")) {
                        attributes.put("l", String.valueOf(_u.getAttribute("l")[0]));
                    }
                    if (_u.hasAttribute("accountEnableStatus") && "disabled".equalsIgnoreCase(String.valueOf(_u.getAttribute("accountEnableStatus")[0]))) {
                        attributes.put("accountStatus", "disabled");
                    }
                    write(_log, "Writing user [");
                    write(_log, attributes.get("sAMAccountName"));
                    write(_log, "]: ");
                    storeUserEntry(attributes.get("sAMAccountName"), attributes);
                    if (this.delete) {
                        _remove_users.remove(attributes.get("sAMAccountName"));
                    }
                    writeLine(_log, "done");
                } catch (Exception _ex) {
                    write(_log, "error - ");
                    writeLine(_log, _ex.getMessage());
                }
            }
            if (this.delete) {
                for (String uid : _remove_users) {
                    write(_log, " -Deleting user entry [");
                    write(_log, uid);
                    write(_log, "]: ");
                    try {
                        Entry _e = getUserEntry(uid);
                        this._eb.removeEntry(_e.getID());
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

    public List<Entry> searchUserEntry(String samAccountName) throws Exception {
        if (samAccountName == null) {
            return new ArrayList<Entry>();
        }
        Query _q = new Query();
        _q.addCondition("objectclass", "person", Query.EXACT);
        _q.addCondition("sAMAccountName", samAccountName, Query.CONTAINS);
        return this._eb.sortedSearch(_q, "cn");
    }

    private static void loadUserAttributes(Entry _e, Map<String, String> attributes) throws Exception {
        _e.setAttribute("givenName", attributes.get("givenName"));
        _e.setAttribute("sn", attributes.get("sn"));
        if (!_e.hasAttribute("objectclass")) {
            _e.setAttribute("objectclass", new String[] { "top", "person", "organizationalPerson", "user" });
        }
        if (attributes.containsKey("cn")) {
            _e.setAttribute("cn", attributes.get("cn"));
            _e.setAttribute("name", attributes.get("cn"));
        } else if (!_e.hasAttribute("cn")) {
            _e.setAttribute("cn", attributes.get("givenName") + " " + attributes.get("sn"));
            _e.setAttribute("name", attributes.get("givenName") + " " + attributes.get("sn"));
        }
        if (attributes.containsKey("displayName")) {
            _e.setAttribute("displayName", attributes.get("displayName"));
        } else if (!_e.hasAttribute("displayName")) {
            _e.setAttribute("displayName", attributes.get("givenName") + " " + attributes.get("sn"));
        }
        if (attributes.containsKey("maildrop")) {
            _e.setAttribute("mail", attributes.get("maildrop"));
        } else if (attributes.containsKey("mail")) {
            _e.setAttribute("mail", attributes.get("mail"));
        }
        if (attributes.containsKey("telephoneNumber")) {
            _e.setAttribute("telephoneNumber", attributes.get("telephoneNumber"));
        }
        if (attributes.containsKey("facsimileTelephoneNumber")) {
            _e.setAttribute("facsimileTelephoneNumber", attributes.get("facsimileTelephoneNumber"));
        }
        if (attributes.containsKey("mobile")) {
            _e.setAttribute("mobile", attributes.get("mobile"));
        }
        if (attributes.containsKey("street")) {
            _e.setAttribute("streetAddress", attributes.get("street"));
        }
        if (attributes.containsKey("postalCode")) {
            _e.setAttribute("postalCode", attributes.get("postalCode"));
        }
        if (attributes.containsKey("st")) {
            _e.setAttribute("st", attributes.get("st"));
        }
        if (attributes.containsKey("title")) {
            _e.setAttribute("title", attributes.get("title"));
        }
        if (attributes.containsKey("employeeType")) {
            _e.setAttribute("employeetype", attributes.get("employeeType"));
        }
        if (attributes.containsKey("sambaProfilePath")) {
            _e.setAttribute("profilePath", attributes.get("sambaProfilePath"));
        }
        if (attributes.containsKey("sambaHomePath")) {
            _e.setAttribute("homeDirectory", attributes.get("sambaHomePath"));
        }
        if (attributes.containsKey("o")) {
            _e.setAttribute("company", attributes.get("o"));
        }
        if (attributes.containsKey("l")) {
            _e.setAttribute("l", attributes.get("l"));
        }
    }

    private static void loadGroupAttributes(Entry _e, Map<String, String> attributes) throws Exception {
        _e.setAttribute("cn", attributes.get("cn"));
        _e.setAttribute("name", attributes.get("cn"));
        _e.setAttribute("sAMAccountName", attributes.get("cn"));
        if (attributes.containsKey("description")) {
            _e.setAttribute("description", attributes.get("description"));
        }
        if (!_e.hasAttribute("objectclass")) {
            _e.setAttribute("objectclass", new String[] { "top", "group" });
        }
    }

    @SuppressWarnings("unused")
    private static Calendar getMSADCalendarAttribute(String value) throws Exception {
        if (value == null || !value.matches("[0-9]+")) {
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
        if (value.length() > 4) {
            _c.set(Calendar.SECOND, Integer.parseInt(value.substring(12, 14)));
        } else {
            _c.set(Calendar.SECOND, 0);
        }
        _c.set(Calendar.MILLISECOND, 0);
        return _c;
    }

    private static void writeLine(FileOutputStream _fos, String message) {
        write(_fos, message.concat("\n"));
    }

    private static void write(FileOutputStream _fos, String message) {
        if (_fos != null) {
            try {
                _fos.write(message.getBytes());
            } catch (IOException _ex) {
            }
        }
    }
}
