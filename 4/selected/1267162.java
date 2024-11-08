package ch.unibe.id.se.a3ublogin.persistence.tools;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.TreeMap;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import ch.unibe.a3ubAdmin.exception.ValidationException;
import ch.unibe.id.se.a3ublogin.exceptions.LdapException;

/**
 * Interface to access the id-aut ldap
 * 
 * @author Daniel Marthaler
 * @version 1.0 / last change: 13.07.2006 Daniel Marthaler
 * @since JDK 1.4.2
 */
public class transferFromIdAut {

    public static int count = 0;

    private String serverUrl = "";

    private String baseDN = "";

    private Log log = LogFactory.getLog(getClass());

    private Map groups = new TreeMap();

    private BufferedWriter out = null;

    private BufferedWriter outSQL = null;

    /** instance of the singleton */
    public static transferFromIdAut instance = null;

    /** returns the singleton */
    public static synchronized transferFromIdAut getInstance() {
        if (instance == null) instance = new transferFromIdAut();
        return instance;
    }

    /** privat constructor */
    private transferFromIdAut() {
        this.serverUrl = ch.unibe.id.se.a3ublogin.persistence.Constants.getInstance().getIdAutAddress();
        this.baseDN = ch.unibe.id.se.a3ublogin.persistence.Constants.getInstance().getIdAutBaseDN();
        try {
            out = new BufferedWriter(new FileWriter("/groups.txt"));
            outSQL = new BufferedWriter(new FileWriter("/sql.txt"));
        } catch (IOException e) {
            if (log.isWarnEnabled()) {
                log.warn("transferFromIdAut: ", e);
            }
        }
    }

    /**
	 * this method checks if a given user is in the id-aut-ldap or not if
	 * soething is wrong with the ldap a NamingException will be thrown
	 * 
	 * @param userName -
	 *            String the name to look in Id-aut for
	 * @return boolean true if the username is know in id-aut-Ldap
	 * @throws LdapException
	 * 
	 * @throws NamingException
	 */
    public boolean isUserNameInContainer(String userName) throws LdapException {
        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
        NamingEnumeration persons;
        try {
            persons = this.getAnonymousBindFromIdAut().search("", "uid=" + userName, sc);
            while (persons.hasMore()) {
                SearchResult sr = (SearchResult) persons.next();
                String tempStr = sr.getName();
                if (tempStr.indexOf(userName) > 0) {
                    return true;
                }
            }
        } catch (NamingException e) {
            if (log.isWarnEnabled()) {
                log.warn("transferFromIdAut: ", e);
            }
            throw new LdapException(e.getMessage(), "error_10");
        }
        return false;
    }

    /**
	 * uses a anonymous bind, and returns the ldap-searchstring to find the
	 * person afterwards in his subtree of the ldap dictionary
	 * 
	 * @param userIdNumber -
	 *            String specifies the user to find the searchstring for
	 * @return String - the searchstring, also known as UserDN
	 * @throws LdapException
	 * @throws NamingException
	 */
    private String getUserDN(String userIdNumber) throws LdapException {
        String userDNreturn = "";
        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
        NamingEnumeration persons = null;
        try {
            persons = this.getAnonymousBindFromIdAut().search("", "uidNumber=" + userIdNumber, sc);
            while (persons.hasMore()) {
                SearchResult sr = (SearchResult) persons.next();
                userDNreturn = sr.getName();
                break;
            }
        } catch (NamingException e) {
            if (log.isWarnEnabled()) {
                log.warn("transferFromIdAut: ", e);
            }
            throw new LdapException(e.getMessage(), "error_10");
        }
        return userDNreturn;
    }

    /**
	 * creates a new, named ldap bind on id-aut
	 * 
	 * @param userName -
	 *            the campusAccount user
	 * @param password -
	 *            the campusAccount password
	 * 
	 * @return DirContext - the ldap object
	 * 
	 * @throws NamingException
	 * @throws LdapException
	 */
    @SuppressWarnings("unused")
    private DirContext getNamedBindFromIdAut(String userName, String password) throws LdapException {
        String serverUrl = this.serverUrl + "/" + baseDN;
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, serverUrl);
        env.put("java.naming.ldap.version", "3");
        env.put(Context.SECURITY_PROTOCOL, "ssl");
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, getUserDN(userName) + "," + baseDN);
        env.put(Context.SECURITY_CREDENTIALS, password);
        try {
            return new InitialDirContext(env);
        } catch (NamingException e) {
            if (log.isWarnEnabled()) {
                log.warn("transferFromIdAut: ", e);
            }
            throw new LdapException(e.getMessage(), "error_10");
        }
    }

    /**
	 * creates a new, anonymos ldap bind on id-aut
	 * 
	 * @return DirContext - the ldap object
	 * @throws LdapException
	 * 
	 * @throws NamingException
	 */
    private DirContext getAnonymousBindFromIdAut() throws LdapException {
        String serverUrl = this.serverUrl + "/" + baseDN;
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, serverUrl);
        env.put("java.naming.ldap.version", "3");
        env.put(Context.SECURITY_PROTOCOL, "ssl");
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        try {
            return new InitialDirContext(env);
        } catch (NamingException e) {
            if (log.isWarnEnabled()) {
                log.warn("transferFromIdAut: ", e);
            }
            throw new LdapException(e.getMessage(), "error_10");
        }
    }

    /**
	 * returns a HashMap with all the idaut keys as keys and a String[] as
	 * values
	 * 
	 * @param userName -
	 *            the campusAccount user
	 * @param password -
	 *            the campusAccount password
	 * @return Map - with all the attributes for the given person
	 * @throws LdapException
	 * 
	 * @throws NamingException
	 */
    public Map<String, String[]> getPersonsValues(String userDN) throws LdapException {
        Map<String, String[]> retMap = new HashMap<String, String[]>();
        DirContext context;
        try {
            context = this.getAnonymousBindFromIdAut();
            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            NamingEnumeration persons = context.search(userDN, "(objectclass=*)", sc);
            while (persons != null && persons.hasMore()) {
                SearchResult sr = (SearchResult) persons.next();
                Attributes attributes = sr.getAttributes();
                NamingEnumeration enu = attributes.getAll();
                while (enu.hasMore()) {
                    Attribute attr = (Attribute) enu.next();
                    String[] arr = new String[attr.size()];
                    for (int i = 0; i < arr.length; i++) {
                        arr[i] = (String) attr.get(i);
                    }
                    retMap.put(attr.getID().toLowerCase(), arr);
                }
            }
            if (log.isDebugEnabled()) {
                log.debug("found " + retMap.size() + " attributes from idauth for: " + userDN);
            }
        } catch (NamingException e) {
            if (log.isWarnEnabled()) {
                log.warn("transferFromIdAut: ", e);
            }
            if (e.getMessage().equals("[LDAP: error code 49 - Invalid Credentials]")) {
                throw new LdapException(e.getMessage(), "error_01");
            }
            if (e.getMessage().equals("[LDAP: error code 53 - unauthenticated bind (DN with no password) disallowed]")) {
                throw new LdapException(e.getMessage(), "error_02");
            }
            if (e.getMessage().equals("[LDAP: error code 34 - invalid DN]")) {
                throw new LdapException("wrong username");
            }
        }
        return retMap;
    }

    private void SearchPersons(int start) {
        transferFromIdAut ldap = transferFromIdAut.getInstance();
        ch.unibe.a3ubAdmin.control.DatabaseManager man = new ch.unibe.a3ubAdmin.control.DatabaseManager();
        for (int i = start; i < 150000; i++) {
            if (i % 500 == 0) {
                Date d = new Date();
            }
            String userDN = "";
            try {
                userDN = ldap.getUserDN("" + i);
            } catch (LdapException e) {
            }
            if (!"".equals(userDN)) {
                Map map = null;
                try {
                    map = ldap.getPersonsValues(userDN);
                } catch (LdapException e) {
                }
                if (map != null && map.keySet().size() > 3) {
                    String[] uidNumberArr = (String[]) map.get("uidnumber");
                    String[] uidArr = (String[]) map.get("uid");
                    String[] cnArr = (String[]) map.get("cn");
                    String[] gidArr = (String[]) map.get("gidnumber");
                    ch.unibe.a3ubAdmin.model.ViewUser user = null;
                    try {
                        user = man.loadViewUser(uidArr[0]);
                    } catch (HibernateException e) {
                        if (log.isWarnEnabled()) {
                            log.warn("transferFromIdAut: ", e);
                        }
                    } catch (NumberFormatException e) {
                        if (log.isWarnEnabled()) {
                            log.warn("transferFromIdAut: ", e);
                        }
                    } catch (ValidationException e) {
                        if (log.isWarnEnabled()) {
                            log.warn("transferFromIdAut: ", e);
                        }
                    }
                    String snArr[] = { "-" };
                    if (user.getUidnumber() != null) {
                    } else {
                        writeSqlFile("INSERT INTO ub_a3ubadmin2_dbm.vw_user VALUES(" + uidNumberArr[0] + ",'" + uidArr[0] + "','" + snArr[0] + "','" + cnArr[0] + "','" + gidArr[0] + "');");
                    }
                    writeGroupfile(gidArr[0]);
                }
            }
        }
    }

    private void writeGroupfile(String g) {
        if (!groups.containsKey(g)) {
            groups.put(g, g);
            try {
                out.write(g + ",");
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void writeSqlFile(String g) {
        try {
            outSQL.write(g + "\n");
            outSQL.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * returns a HashMap with all the idaut keys as keys and a String[] as
	 * values
	 * 
	 * @param userName -
	 *            the campusAccount user
	 * @param password -
	 *            the campusAccount password
	 * @return Map - with all the attributes for the given person
	 * @throws LdapException
	 * 
	 * @throws NamingException
	 */
    public Map<String, String[]> getGroup(String groupId) throws LdapException {
        Map<String, String[]> retMap = new HashMap<String, String[]>();
        DirContext context;
        int in = 0;
        try {
            context = this.getAnonymousBindFromIdAut();
            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            NamingEnumeration persons = context.search("", "gidNumber=" + groupId, sc);
            int i = 0;
            while (persons != null && persons.hasMore()) {
                SearchResult sr = (SearchResult) persons.next();
                Attributes attributes = sr.getAttributes();
                NamingEnumeration enu = attributes.getAll();
                while (enu.hasMore()) {
                    Attribute attr = (Attribute) enu.next();
                    String[] arr = new String[attr.size()];
                    for (int k = 0; k < arr.length; k++) {
                        arr[k] = (String) attr.get(k);
                    }
                    retMap.put(attr.getID().toLowerCase(), arr);
                }
                if (retMap.size() > 5) {
                    String uidnumber = retMap.get("uidnumber")[0];
                    String gidnumber = retMap.get("gidnumber")[0];
                    String uid = retMap.get("uid")[0];
                    String cn = retMap.get("cn")[0];
                    String shurname = cn.split(" ")[(cn.split(" ")).length - 1];
                    String str = "INSERT INTO ub_a3ubadmin2_dbm.vw_user VALUES(" + uidnumber + ",'" + uid + "','" + shurname + "','" + cn + "','" + gidnumber + "');";
                    if (!"".equals(str)) {
                        this.writeSqlFile(str);
                        in++;
                        count++;
                    }
                }
            }
            if (log.isDebugEnabled()) {
                log.debug("found " + retMap.size() + " attributes from idauth for: ");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return retMap;
    }

    /**
	 * for testing and developpin purpouse only
	 */
    public static void main(String[] args) {
        transferFromIdAut.getInstance().writeSqlFile("Delete from ub_a3ubadmin2_dbm.vw_user;");
        int gr[] = { 1002, 1013, 1017, 1030, 1183, 1048, 1058, 1060, 1084, 1089, 1092, 1096, 1097, 1100, 1101, 1127, 1146, 1159, 1169, 1173, 1180, 1085, 1014, 1070, 1165, 1072, 1098, 1099, 1153, 1004, 1016, 1110, 1147, 1005, 1043, 1157, 1063, 1062, 1160, 1061, 1059, 1028, 1125, 1020, 1033, 1032, 1077, 1163, 1078, 1102, 1091, 1175, 1094, 1067, 1103, 1148, 1027, 1038, 1142, 1124, 1080, 1050, 1001, 1168, 1122, 1095, 1024, 1174, 1179, 1129, 1126, 1151, 1008, 1182, 1141, 1082, 1081, 1023, 1107, 1152, 1083, 1144, 1074, 1039, 1171, 1117, 1066, 1003, 1131, 1022, 1138, 1029, 1010, 1035, 1040, 1076, 1037, 1051, 1086, 1031, 1162, 1136, 1046, 1128, 1156, 1135, 1178, 1075, 1036, 1139, 1140, 1134, 1090, 1119, 1007, 1132, 1049, 1167, 1121, 1019, 1130, 1047, 1106, 1181, 1052, 1166, 1006, 1120, 1065, 1064, 1155, 1088, 1123, 1108, 1068, 1018, 1105, 1012, 1093, 1015, 1172, 1053, 1069, 1054, 1045, 1041, 1071, 1111, 1112, 1115, 1113, 1026, 1009, 1055, 1021, 15000, 15001, 15002, 15005, 15006, 15010, 1154, 1149, 1158, 1042, 1073, 1034, 1025, 1150, 1011, 1056, 1116, 1118, 1087, 1044, 1104, 1079 };
        long t = System.currentTimeMillis();
        for (int i = 0; i < gr.length; i++) {
            int temp = gr[i];
            if (temp < 10000) {
                try {
                    transferFromIdAut.getInstance().getGroup("" + temp);
                } catch (LdapException e) {
                    e.printStackTrace();
                }
            }
            i++;
        }
    }
}
