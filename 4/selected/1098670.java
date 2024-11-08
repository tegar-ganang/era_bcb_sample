package com.whitebearsolutions.imagine.wbsagnitio.idm.repository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
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
import com.whitebearsolutions.imagine.wbsagnitio.directory.AttributeSet;
import com.whitebearsolutions.imagine.wbsagnitio.directory.EntryManager;
import com.whitebearsolutions.imagine.wbsagnitio.directory.GroupManager;
import com.whitebearsolutions.imagine.wbsagnitio.directory.UserManager;
import com.whitebearsolutions.imagine.wbsagnitio.util.CharacterEncode;
import com.whitebearsolutions.util.Configuration;

public class ExternalRepositoryMSAD extends ExternalRepository {

    private static final int UF_ACCOUNTDISABLE = 0x0002;

    private static final int UF_PASSWD_NOTREQD = 0x0020;

    @SuppressWarnings("unused")
    private static final int UF_PASSWD_CANT_CHANGE = 0x0040;

    private static final int UF_NORMAL_ACCOUNT = 0x0200;

    @SuppressWarnings("unused")
    private static final int UF_DONT_EXPIRE_PASSWD = 0x10000;

    @SuppressWarnings("unused")
    private static final int UF_PASSWORD_EXPIRED = 0x800000;

    private Configuration _local_c;

    private Configuration _remote_c;

    private SystemConfiguration _sc;

    private NetworkManager _nm;

    private EntryBase _eb;

    private boolean delete = false;

    public ExternalRepositoryMSAD(Configuration _c) throws Exception {
        super();
        this._local_c = new Configuration(new File(WBSAgnitioConfiguration.getConfigurationFile()));
        this._sc = new SystemConfiguration(this._local_c);
        this._nm = new NetworkManager(this._local_c);
        this._remote_c = _c;
        this._sc = new SystemConfiguration(this._local_c);
        this._eb = new EntryBase(this._remote_c);
        if (this._remote_c.checkProperty("ldap.replica.delete", "true")) {
            this.delete = true;
        }
    }

    public void addGroupEntry(String group, AttributeSet attributes) throws Exception {
        storeGroupEntry(group, attributes, STORE_ADD_ONLY);
    }

    public void addUserEntry(String user, AttributeSet attributes) throws Exception {
        storeUserEntry(user, attributes, STORE_ADD_ONLY);
    }

    public void addUserMember(String group, String uid) throws Exception {
        AttributeSet _user = getUserEntry(uid);
        AttributeSet _group = null;
        if (_user == null) {
            throw new Exception("user does not exists");
        }
        try {
            _group = getGroupEntry(group);
        } catch (Exception _ex) {
        }
        if (_group != null) {
            this._eb.addEntryAttribute(_group.getAttributeFirstStringValue("dn"), "member", _user.getAttributeFirstStringValue("dn"));
        }
    }

    public void deleteGroupEntry(String name, AttributeSet attributes) throws Exception {
        AttributeSet _group = null;
        try {
            _group = getGroupEntry(name);
        } catch (Exception _ex) {
        }
        if (_group != null) {
            this._eb.removeEntry(_group.getAttributeFirstStringValue("dn"));
        }
    }

    public void deleteProfile(String user, String profile) throws Exception {
    }

    public void deleteUserEntry(String samAccountName, AttributeSet attributes) throws Exception {
        AttributeSet _user = null;
        try {
            _user = getUserEntry(samAccountName);
        } catch (Exception _ex) {
        }
        if (_user != null) {
            this._eb.removeEntry(_user.getAttributeFirstStringValue("dn"));
        }
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
            Query _q = new Query();
            _q.addCondition("objectclass", "posixAccount", Query.EXACT);
            _q.addCondition("gidNumber", "515", Query.NOT_EXACT);
            write(_log, "Reading groups from local directory .. ");
            List<Entry> _users = _em.getAllEntries(EntryManager.GROUP);
            writeLine(_log, "done");
            for (Entry _g : _users) {
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
                try {
                    AttributeSet _as = new AttributeSet(_u);
                    write(_log, "Writing user [");
                    write(_log, _as.getAttributeFirstStringValue("uid"));
                    write(_log, "]: ");
                    storeUserEntry(_as.getAttributeFirstStringValue("uid"), _as);
                    if (this.delete) {
                        _remove_users.remove(String.valueOf(_u.getAttribute("uid")[0]));
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

    private AttributeSet getGroupAttributes(Entry _g) throws Exception {
        AttributeSet _group = new AttributeSet();
        _group.setAttribute("dn", _g.getID());
        _group.setAttribute("cn", _g.getAttribute("name"));
        if (_g.hasAttribute("displayname")) {
            _group.setAttribute("displayname", _g.getAttribute("displayname"));
        } else {
            _group.setAttribute("displayname", _g.getAttribute("cn"));
        }
        if (_g.hasAttribute("description")) {
            _group.setAttribute("description", _g.getAttribute("description"));
        }
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
                if (_ec.hasAttribute("cn") && "Builtin".equals(String.valueOf(_ec.getAttribute("cn")[0]))) {
                    continue;
                }
                this._eb.setScope(LDAPConnection.SUBTREE_SCOPE);
                _q = new Query();
                _q.addCondition("objectclass", "group", Query.EXACT);
                if (_ec.hasAttribute("ou")) {
                    _q.addCondition("ou", String.valueOf(_ec.getAttribute("ou")[0]), Query.BRANCH);
                } else if (_ec.hasAttribute("cn")) {
                    _q.addCondition("cn", String.valueOf(_ec.getAttribute("cn")[0]), Query.BRANCH);
                }
                List<Entry> _results = this._eb.search(_q);
                for (Entry _g : _results) {
                    if (!_g.hasAttribute("name") || "Users".equals(_g.getAttribute("name")[0]) || "Pre-Windows 2000 Compatible Access".equals(_g.getAttribute("name")[0]) || "DnsUpdateProxy".equals(_g.getAttribute("name")[0])) {
                        continue;
                    }
                    groups.add(String.valueOf(_g.getAttribute("name")[0]));
                    if (cal != null && _g.hasAttribute("whenChanged")) {
                        Calendar _src_cal = getMSADCalendarAttribute(String.valueOf(_g.getAttribute("whenChanged")[0]));
                        if (cal.after(_src_cal)) {
                            continue;
                        }
                    }
                    AttributeSet attributes = new AttributeSet();
                    attributes.setAttribute("cn", _g.getAttribute("name"));
                    if (_g.hasAttribute("displayname")) {
                        attributes.setAttribute("displayname", _g.getAttribute("displayname"));
                    } else {
                        attributes.setAttribute("displayname", _g.getAttribute("cn"));
                    }
                    if (_g.hasAttribute("description")) {
                        attributes.setAttribute("description", _g.getAttribute("description"));
                    }
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
                                if (objectclasses.contains("person") && _e_tmp.hasAttribute("sAMAccountName")) {
                                    _users.add(_e_tmp.getAttribute("sAMAccountName")[0]);
                                } else if (objectclasses.contains("group") && _e_tmp.hasAttribute("name")) {
                                    _groups.add(_e_tmp.getAttribute("name")[0]);
                                }
                            }
                        }
                        attributes.setAttribute("userMember", _users.toArray());
                        attributes.setAttribute("groupMember", _groups.toArray());
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
        _q.addCondition("objectclass", "group", Query.EXACT);
        _q.addCondition("name", group, Query.EXACT);
        List<Entry> _result = this._eb.search(_q);
        if (_result != null && !_result.isEmpty()) {
            Entry _g = _result.get(0);
            return getGroupAttributes(_g);
        }
        return null;
    }

    private static Calendar getMSADCalendarAttribute(String value) throws Exception {
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

    public List<String> getProfiles(String user) throws Exception {
        return new ArrayList<String>();
    }

    private AttributeSet getUserAttributes(Entry _u) throws Exception {
        AttributeSet _user = new AttributeSet();
        if (_u == null) {
            return _user;
        }
        _user.setAttribute("dn", _u.getID());
        List<String> _values = new ArrayList<String>();
        if (_u.hasAttribute("memberOf")) {
            Object[] chain_values = (Object[]) _u.getAttribute("memberOf");
            for (int j = chain_values.length; --j >= 0; ) {
                String chain = String.valueOf(chain_values[j]);
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
            _user.setAttribute("ou", _values.toArray());
        }
        if (_u.hasAttribute("userAccountControl")) {
            try {
                long _UAC = Long.parseLong(String.valueOf(_u.getAttribute("userAccountControl")[0]));
                if ((_UAC & UF_ACCOUNTDISABLE) == UF_ACCOUNTDISABLE) {
                    _user.setAttribute("accountStatus", "disabled");
                }
            } catch (NumberFormatException _ex) {
            }
        }
        _user.setAttribute("uid", _u.getAttribute("sAMAccountName"));
        _user.setAttribute("cn", _u.getAttribute("cn"));
        _user.setAttribute("gecos", CharacterEncode.toASCII(_user.getAttributeFirstStringValue("cn")));
        _user.setAttribute("password", new Object[] { "12345" });
        if (_u.hasAttribute("givenname")) {
            _user.setAttribute("givenname", _u.getAttribute("givenname"));
        } else {
            _user.setAttribute("givenname", _u.getAttribute("cn"));
        }
        if (_u.hasAttribute("sn")) {
            _user.setAttribute("sn", _u.getAttribute("sn"));
        } else {
            _user.setAttribute("sn", _u.getAttribute("cn"));
        }
        if (_u.hasAttribute("title")) {
            _user.setAttribute("title", _u.getAttribute("title"));
        }
        if (_u.hasAttribute("streetaddress")) {
            _user.setAttribute("street", _u.getAttribute("streetaddress"));
        }
        if (_u.hasAttribute("postalcode")) {
            _user.setAttribute("postalcode", _u.getAttribute("postalcode"));
        }
        if (_u.hasAttribute("company")) {
            _user.setAttribute("o", _u.getAttribute("company"));
        }
        if (_u.hasAttribute("employeetype")) {
            _user.setAttribute("employeetype", _u.getAttribute("employeetype"));
        }
        if (_u.hasAttribute("displayname")) {
            _user.setAttribute("displayname", _u.getAttribute("displayname"));
        } else {
            _user.setAttribute("displayname", _u.getAttribute("cn"));
        }
        if (_u.hasAttribute("title")) {
            _user.setAttribute("title", _u.getAttribute("title"));
        }
        if (_u.hasAttribute("company")) {
            _user.setAttribute("o", _u.getAttribute("company"));
        }
        if (_u.hasAttribute("description")) {
            _user.setAttribute("description", _u.getAttribute("description"));
        }
        if (_u.hasAttribute("mail")) {
            _user.setAttribute("maildrop", _u.getAttribute("mail"));
            _user.setAttribute("mail", _u.getAttribute("mail"));
        } else {
            _user.setAttribute("maildrop", _user.getAttributeFirstStringValue("uid") + "@" + this._nm.getDomain());
            _user.setAttribute("mail", _user.getAttributeFirstStringValue("uid") + "@" + this._nm.getDomain());
        }
        if (_u.hasAttribute("telephoneNumber")) {
            _user.setAttribute("telephoneNumber", _u.getAttribute("telephoneNumber"));
        }
        if (_u.hasAttribute("facsimileTelephoneNumber")) {
            _user.setAttribute("facsimileTelephoneNumber", _u.getAttribute("facsimileTelephoneNumber"));
        }
        if (_u.hasAttribute("st")) {
            _user.setAttribute("st", _u.getAttribute("st"));
        }
        if (_u.hasAttribute("l")) {
            _user.setAttribute("l", _u.getAttribute("l"));
        }
        if (_u.hasAttribute("homeDrive")) {
            _user.setAttribute("sambaHomeDrive", _u.getAttribute("homeDrive"));
        } else {
            if (this._local_c.getProperty("samba.home.drive") != null) {
                _user.setAttribute("sambaHomeDrive", new Object[] { this._local_c.getProperty("samba.home.drive") });
            }
        }
        if (_u.hasAttribute("homeDirectory")) {
            _user.setAttribute("sambaHomePath", _u.getAttribute("homeDirectory"));
        } else {
            if (this._local_c.getProperty("samba.home.server") != null) {
                _user.setAttribute("sambaHomePath", new Object[] { "\\\\" + this._local_c.getProperty("samba.home.server") + "\\" + _u.getAttribute("sAMAccountName")[0] });
            }
        }
        if (_u.hasAttribute("scriptPath")) {
            _user.setAttribute("sambaLogonScript", _u.getAttribute("scriptpath"));
        }
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
            NetworkManager _nm = new NetworkManager(this._local_c);
            this._eb.setScope(LDAPConnection.ONE_SCOPE);
            Query _q = new Query();
            _q.addCondition("objectclass", "top", Query.EXACT);
            List<Entry> _containers = this._eb.search(_q);
            for (Entry _ec : _containers) {
                this._eb.setScope(LDAPConnection.SUBTREE_SCOPE);
                _q = new Query();
                _q.addCondition("objectclass", "person", Query.EXACT);
                _q.addCondition("objectclass", "computer", Query.NOT_EXACT);
                if (_ec.hasAttribute("ou")) {
                    _q.addCondition("ou", String.valueOf(_ec.getAttribute("ou")[0]), Query.BRANCH);
                } else if (_ec.hasAttribute("cn")) {
                    _q.addCondition("cn", String.valueOf(_ec.getAttribute("cn")[0]), Query.BRANCH);
                }
                List<Entry> _results = this._eb.search(_q);
                for (Entry _u : _results) {
                    if (!_u.hasAttribute("sAMAccountName") || !_u.hasAttribute("cn") || this._sc.getAdministrativeUser().equals(_u.getAttribute("sAMAccountName")[0])) {
                        continue;
                    }
                    users.add(String.valueOf(_u.getAttribute("sAMAccountName")[0]));
                    if (cal != null && _u.hasAttribute("whenChanged")) {
                        Calendar _src_cal = getMSADCalendarAttribute(String.valueOf(_u.getAttribute("whenChanged")[0]));
                        if (cal.after(_src_cal)) {
                            continue;
                        }
                    }
                    AttributeSet attributes = new AttributeSet();
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
                        attributes.setAttribute("ou", _values.toArray());
                    }
                    if (_u.hasAttribute("userAccountControl")) {
                        try {
                            long _UAC = Long.parseLong(String.valueOf(_u.getAttribute("userAccountControl")[0]));
                            if ((_UAC & UF_ACCOUNTDISABLE) == UF_ACCOUNTDISABLE) {
                                attributes.setAttribute("accountStatus", "disabled");
                            }
                        } catch (NumberFormatException _ex) {
                        }
                    }
                    attributes.setAttribute("uid", _u.getAttribute("sAMAccountName"));
                    attributes.setAttribute("cn", _u.getAttribute("cn"));
                    attributes.setAttribute("gecos", CharacterEncode.toASCII(attributes.getAttributeFirstStringValue("cn")));
                    attributes.setAttribute("password", new Object[] { "12345" });
                    if (_u.hasAttribute("givenname")) {
                        attributes.setAttribute("givenname", _u.getAttribute("givenname"));
                    } else {
                        attributes.setAttribute("givenname", _u.getAttribute("cn"));
                    }
                    if (_u.hasAttribute("sn")) {
                        attributes.setAttribute("sn", _u.getAttribute("sn"));
                    } else {
                        attributes.setAttribute("sn", _u.getAttribute("cn"));
                    }
                    if (_u.hasAttribute("employeetype")) {
                        attributes.setAttribute("employeetype", _u.getAttribute("employeetype"));
                    }
                    if (_u.hasAttribute("displayname")) {
                        attributes.setAttribute("displayname", _u.getAttribute("displayname"));
                    } else {
                        attributes.setAttribute("displayname", _u.getAttribute("cn"));
                    }
                    if (_u.hasAttribute("title")) {
                        attributes.setAttribute("title", _u.getAttribute("title"));
                    }
                    if (_u.hasAttribute("streetaddress")) {
                        attributes.setAttribute("street", _u.getAttribute("streetaddress"));
                    }
                    if (_u.hasAttribute("postalcode")) {
                        attributes.setAttribute("postalcode", _u.getAttribute("postalcode"));
                    }
                    if (_u.hasAttribute("company")) {
                        attributes.setAttribute("o", _u.getAttribute("company"));
                    }
                    if (_u.hasAttribute("description")) {
                        attributes.setAttribute("description", _u.getAttribute("description"));
                    }
                    if (_u.hasAttribute("mail")) {
                        attributes.setAttribute("maildrop", _u.getAttribute("mail"));
                        attributes.setAttribute("mail", _u.getAttribute("mail"));
                    } else {
                        attributes.setAttribute("maildrop", attributes.getAttributeFirstStringValue("uid") + "@" + _nm.getDomain());
                        attributes.setAttribute("mail", attributes.getAttributeFirstStringValue("uid") + "@" + _nm.getDomain());
                    }
                    if (_u.hasAttribute("telephoneNumber")) {
                        attributes.setAttribute("telephoneNumber", _u.getAttribute("telephoneNumber"));
                    }
                    if (_u.hasAttribute("facsimileTelephoneNumber")) {
                        attributes.setAttribute("facsimiletelephonenumber", _u.getAttribute("facsimiletelephonenumber"));
                    }
                    if (_u.hasAttribute("st")) {
                        attributes.setAttribute("st", _u.getAttribute("st"));
                    }
                    if (_u.hasAttribute("l")) {
                        attributes.setAttribute("l", _u.getAttribute("l"));
                    }
                    if (_u.hasAttribute("homeDrive")) {
                        attributes.setAttribute("sambaHomeDrive", _u.getAttribute("homeDrive"));
                    } else {
                        if (this._local_c.getProperty("samba.home.drive") != null) {
                            attributes.setAttribute("sambahomedrive", this._local_c.getProperty("samba.home.drive"));
                        }
                    }
                    if (_u.hasAttribute("homeDirectory")) {
                        attributes.setAttribute("sambaHomePath", _u.getAttribute("homeDirectory"));
                    } else {
                        if (this._local_c.getProperty("samba.home.server") != null) {
                            attributes.setAttribute("sambaHomePath", new Object[] { "\\\\" + this._local_c.getProperty("samba.home.server") + "\\" + _u.getAttribute("sAMAccountName")[0] });
                        }
                    }
                    if (_u.hasAttribute("scriptPath")) {
                        attributes.setAttribute("sambaLogonScript", _u.getAttribute("scriptPath"));
                    }
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

    public AttributeSet getUserEntry(String user) throws Exception {
        if (user == null) {
            return null;
        }
        Query _q = new Query();
        _q.addCondition("objectclass", "person", Query.EXACT);
        _q.addCondition("sAMAccountName", user, Query.EXACT);
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
            throw new Exception("user identifier [" + user + "] not found");
        }
        Entry _e = this._eb.getEntry(_user.getAttributeFirstStringValue("dn"));
        if (_e.hasAttribute("memberOf")) {
            for (Object _oe : _e.getAttribute("memberOf")) {
                Entry _g = this._eb.getEntry(String.valueOf(_oe));
                _groups.add(getGroupAttributes(_g));
            }
        }
        return _groups;
    }

    public List<String> getUserGroupNames(String user) throws Exception {
        List<String> names = new ArrayList<String>();
        for (AttributeSet _g : getUserGroups(user)) {
            if (_g.hasAttribute("cn")) {
                names.add(_g.getAttributeFirstStringValue("cn"));
            } else if (_g.hasAttribute("name")) {
                names.add(_g.getAttributeFirstStringValue("name"));
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
                    AttributeSet attributes = getUserAttributes(_u);
                    try {
                        write(_log, "Importing user [");
                        write(_log, attributes.getAttributeFirstStringValue("uid"));
                        write(_log, "] .. ");
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

    private void loadGroupAttributes(Entry _e, AttributeSet attributes) throws Exception {
        _e.setAttribute("cn", attributes.getAttribute("cn"));
        _e.setAttribute("name", attributes.getAttribute("cn"));
        _e.setAttribute("sAMAccountName", attributes.getAttribute("cn"));
        if (attributes.hasAttribute("description")) {
            _e.setAttribute("description", attributes.getAttribute("description"));
        }
        if (!_e.hasAttribute("objectclass")) {
            _e.setAttribute("objectclass", new String[] { "top", "group" });
        }
        for (String _name : this._groupAttributeMaps.keySet()) {
            Map<String, String> _attribute = this._groupAttributeMaps.get(_name);
            if (_attribute.containsKey("value")) {
                _e.setAttribute(_name, _attribute.get("value"));
            } else if (_attribute.containsKey("attribute")) {
                _e.setAttribute(_name, attributes.getAttribute(_attribute.get("attribute")));
            }
        }
    }

    private void loadUserAttributes(Entry _e, AttributeSet attributes) throws Exception {
        _e.setAttribute("samAccountName", attributes.getAttribute("uid"));
        _e.setAttribute("userPrincipalName", new Object[] { attributes.getAttributeFirstStringValue("uid") + "@" + this._nm.getDomain() });
        _e.setAttribute("givenname", attributes.getAttribute("givenname"));
        _e.setAttribute("sn", attributes.getAttribute("sn"));
        _e.setAttribute("cn", attributes.getAttributeFirstStringValue("givenname") + " " + attributes.getAttributeFirstStringValue("sn"));
        if (!_e.hasAttribute("objectclass")) {
            _e.setAttribute("objectclass", new String[] { "top", "person", "organizationalperson", "user" });
        }
        if (attributes.hasAttribute("cn")) {
            _e.setAttribute("cn", attributes.getAttribute("cn"));
            _e.setAttribute("name", attributes.getAttribute("cn"));
        } else if (!_e.hasAttribute("cn")) {
            _e.setAttribute("cn", attributes.getAttribute("cn"));
            _e.setAttribute("name", attributes.getAttribute("cn"));
        }
        if (attributes.hasAttribute("displayname")) {
            _e.setAttribute("displayname", attributes.getAttribute("displayname"));
        } else if (!_e.hasAttribute("displayname")) {
            _e.setAttribute("displayname", attributes.getAttributeFirstStringValue("givenname") + " " + attributes.getAttributeFirstStringValue("sn"));
        }
        if (attributes.hasAttribute("maildrop")) {
            _e.setAttribute("mail", attributes.getAttribute("maildrop"));
        } else if (attributes.hasAttribute("mail")) {
            _e.setAttribute("mail", attributes.getAttribute("mail"));
        }
        if (attributes.hasAttribute("telephonenumber")) {
            _e.setAttribute("telephonenumber", attributes.getAttribute("telephonenumber"));
        }
        if (attributes.hasAttribute("facsimiletelephonenumber")) {
            _e.setAttribute("facsimiletelephonenumber", attributes.getAttribute("facsimiletelephonenumber"));
        }
        if (attributes.hasAttribute("mobile")) {
            _e.setAttribute("mobile", attributes.getAttribute("mobile"));
        }
        if (attributes.hasAttribute("street")) {
            _e.setAttribute("streetAddress", attributes.getAttribute("street"));
        }
        if (attributes.hasAttribute("postalcode")) {
            _e.setAttribute("postalcode", attributes.getAttribute("postalcode"));
        }
        if (attributes.hasAttribute("st")) {
            _e.setAttribute("st", attributes.getAttribute("st"));
        }
        if (attributes.hasAttribute("title")) {
            _e.setAttribute("title", attributes.getAttribute("title"));
        }
        if (attributes.hasAttribute("employeetype")) {
            _e.setAttribute("employeetype", attributes.getAttribute("employeetype"));
        }
        if (attributes.hasAttribute("sambaprofilepath")) {
            _e.setAttribute("profilePath", attributes.getAttribute("sambaprofilepath"));
        }
        if (attributes.hasAttribute("sambahomepath")) {
            _e.setAttribute("homeDirectory", attributes.getAttribute("sambahomepath"));
        }
        if (attributes.hasAttribute("o")) {
            _e.setAttribute("company", attributes.getAttribute("o"));
        }
        if (attributes.hasAttribute("l")) {
            _e.setAttribute("l", attributes.getAttribute("l"));
        }
        for (String _name : this._userAttributeMaps.keySet()) {
            Map<String, String> _attribute = this._userAttributeMaps.get(_name);
            if (_attribute.containsKey("value")) {
                _e.setAttribute(_name, _attribute.get("value"));
            } else if (_attribute.containsKey("attribute")) {
                _e.setAttribute(_name, attributes.getAttribute(_attribute.get("attribute")));
            }
        }
        if (attributes.checkAttributeFirstValue("accountStatus", "disabled")) {
            _e.setAttribute("userAccountControl", Integer.toString(UF_NORMAL_ACCOUNT + UF_PASSWD_NOTREQD + UF_ACCOUNTDISABLE));
        } else {
            _e.setAttribute("userAccountControl", Integer.toString(UF_NORMAL_ACCOUNT + UF_PASSWD_NOTREQD));
        }
        if (attributes.hasAttribute("password")) {
            _e.setAttribute("unicodePwd", attributes.getAttribute("password"));
        }
    }

    public void removeUserMember(String group, String uid) throws Exception {
        AttributeSet _user = getUserEntry(uid);
        AttributeSet _group = null;
        if (_user == null) {
            throw new Exception("user does not exists");
        }
        try {
            _group = getGroupEntry(group);
        } catch (Exception _ex) {
        }
        if (_group != null) {
            this._eb.removeEntryAttribute(_group.getAttributeFirstStringValue("dn"), "member", _user.getAttributeFirstStringValue("dn"));
        }
    }

    public List<AttributeSet> searchUserEntry(String user) throws Exception {
        List<AttributeSet> _users = new ArrayList<AttributeSet>();
        if (user == null) {
            return _users;
        }
        Query _q = new Query();
        _q.addCondition("objectclass", "person", Query.EXACT);
        _q.addCondition("sAMAccountName", user, Query.CONTAINS);
        for (Entry _u : this._eb.sortedSearch(_q, "cn")) {
            _users.add(getUserAttributes(_u));
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
        String cn = group;
        if (!attributes.hasAttribute("cn")) {
            attributes.setAttribute("cn", group);
        } else {
            cn = attributes.getAttributeFirstStringValue("cn");
        }
        switch(type) {
            case STORE_FLEXIBLE:
                {
                    if (_group == null) {
                        Entry _e = new LDAPEntry("CN=" + cn + ",CN=Users," + this._remote_c.getProperty("ldap.basedn"));
                        loadGroupAttributes(_e, attributes);
                        this._eb.addEntryMSAD(_e);
                    } else {
                        Entry _e = this._eb.getEntry(_group.getAttributeFirstStringValue("dn"));
                        loadGroupAttributes(_e, attributes);
                        this._eb.updateEntryMSAD(_e);
                    }
                }
                break;
            case STORE_ADD_ONLY:
                {
                    if (_group != null) {
                        throw new Exception("group already exists");
                    }
                    Entry _e = new LDAPEntry("CN=" + cn + ",CN=Users," + this._remote_c.getProperty("ldap.basedn"));
                    loadGroupAttributes(_e, attributes);
                    this._eb.addEntryMSAD(_e);
                }
                break;
            case STORE_UPDATE_ONLY:
                {
                    if (_group == null) {
                        throw new Exception("group does not exists");
                    }
                    Entry _e = this._eb.getEntry(_group.getAttributeFirstStringValue("dn"));
                    loadGroupAttributes(_e, attributes);
                    this._eb.updateEntryMSAD(_e);
                }
                break;
        }
    }

    public void storeProfile(String user, String profile) throws Exception {
    }

    public void storeUserEntry(String samAccountName, AttributeSet attributes) throws Exception {
        storeUserEntry(samAccountName, attributes, STORE_FLEXIBLE);
    }

    private void storeUserEntry(String user, AttributeSet attributes, int type) throws Exception {
        AttributeSet _user = getUserEntry(user);
        String cn = user;
        if (!attributes.hasAttribute("cn")) {
            attributes.setAttribute("cn", user);
        } else {
            cn = attributes.getAttributeFirstStringValue("cn");
        }
        switch(type) {
            case STORE_FLEXIBLE:
                {
                    if (_user == null) {
                        Entry _e = new LDAPEntry("CN=" + cn + ",CN=Users," + this._remote_c.getProperty("ldap.basedn"));
                        loadUserAttributes(_e, attributes);
                        this._eb.addEntryMSAD(_e);
                    } else {
                        Entry _e = this._eb.getEntry(_user.getAttributeFirstStringValue("dn"));
                        loadUserAttributes(_e, attributes);
                        this._eb.updateEntryMSAD(_e);
                    }
                }
                break;
            case STORE_ADD_ONLY:
                {
                    if (_user != null) {
                        throw new Exception("user already exists");
                    }
                    Entry _e = new LDAPEntry("CN=" + cn + ",CN=Users," + this._remote_c.getProperty("ldap.basedn"));
                    loadUserAttributes(_e, attributes);
                    this._eb.addEntryMSAD(_e);
                }
                break;
            case STORE_UPDATE_ONLY:
                {
                    if (_user == null) {
                        throw new Exception("user does not exists");
                    }
                    Entry _e = this._eb.getEntry(_user.getAttributeFirstStringValue("dn"));
                    loadUserAttributes(_e, attributes);
                    this._eb.updateEntryMSAD(_e);
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
            } catch (IOException _ex) {
            }
        }
    }
}
