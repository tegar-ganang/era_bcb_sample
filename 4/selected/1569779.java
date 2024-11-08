package com.ericdaugherty.mail.server.configuration;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.ericdaugherty.mail.server.Mail;
import com.ericdaugherty.mail.server.configuration.ConnectionBasedConfigurator.AddUsersPLL1;
import com.ericdaugherty.mail.server.configuration.cbc.CBCExecutor;
import com.ericdaugherty.mail.server.configuration.cbc.NewUser;
import com.ericdaugherty.mail.server.errors.InvalidAddressException;
import com.ericdaugherty.mail.server.info.*;
import com.ericdaugherty.mail.server.utils.*;

/**
 *
 * @author Andreas Kyrmegalos
 */
public final class ConfigurationManagerBackEndFile implements ConfigurationManagerBackEndIntf, ConfigurationParameterConstants {

    /** Logger */
    private static Log log = LogFactory.getLog(ConfigurationManagerBackEndFile.class);

    private ConfigurationManager cm;

    /** The file reference to the user.conf configuration file */
    private File userConfigurationFile;

    /** The timestamp for the user.conf file when it was last loaded */
    private long userConfigurationFileTimestamp;

    /** The file reference to the realms.conf configuration file */
    private File realmsConfigurationFile;

    /** The file reference to the realms password file */
    private File realmsPasswordFile;

    private boolean realmsPassModified;

    private Properties userProperties;

    /** A Map of Users keyed by their (lowercase) full username */
    private Map<String, UserFile> users;

    /** A Map of Realms keyed by their (lowercase) full realm name */
    private Map<String, Realm> realms;

    /**
    * A Map of the hex hash of (username:realm:pass) keyed by their
    * (lowercase) full realm name and the (lowercase) username
    *
    */
    private Map<String, Map<String, String>> realmsPass;

    /**
    * Array of domains with the default username that the SMTP server
    * should accept mail for local delivery
    */
    private Map<Domain, EmailAddress> localDomainsWithDefaultMailbox = new ConcurrentHashMap<Domain, EmailAddress>();

    private Domain defaultDomain;

    public ConfigurationManagerBackEndFile(ConfigurationManager cm) {
        this.cm = cm;
        String rootDirectory = cm.getRootDirectory();
        String userConfigFilename = "user.conf";
        String realmsConfigFilename = "realms.conf";
        File userConfigFile = new File(rootDirectory + File.separator + "conf", userConfigFilename);
        if (!userConfigFile.isFile() || !userConfigFile.exists()) {
            throw new RuntimeException("Invalid user.conf ConfigurationFile! " + userConfigFile.getAbsolutePath());
        }
        File realmsConfigFile = new File(rootDirectory + File.separator + "conf", realmsConfigFilename);
        if (!realmsConfigFile.isFile() || !realmsConfigFile.exists()) {
            throw new RuntimeException("Invalid realms.conf ConfigurationFile! " + userConfigFile.getAbsolutePath());
        }
        this.userConfigurationFile = userConfigFile;
        this.realmsConfigurationFile = realmsConfigFile;
    }

    /** Not implemented in a file scope **/
    public void shutdown() {
    }

    public void restore(String backupDirectory) throws IOException {
        FileUtils.copyFile(new File(backupDirectory, "user.bak"), userConfigurationFile);
        FileUtils.copyFile(new File(backupDirectory, "realms.bak"), realmsConfigurationFile);
        FileUtils.copyFile(new File(backupDirectory, "realmpwd.bak"), new File(cm.getSecurityDirectory(), "realms.dat"));
    }

    public void doBackup(String backupDirectory) throws IOException {
        try {
            FileUtils.copyFile(new File(backupDirectory, "user.bak"), new File(backupDirectory, "user.ba2"));
            FileUtils.copyFile(new File(backupDirectory, "realms.bak"), new File(backupDirectory, "realms.ba2"));
            FileUtils.copyFile(new File(backupDirectory, "realmpwd.bak"), new File(backupDirectory, "realmpwd.ba2"));
        } catch (IOException e) {
            if (log.isDebugEnabled()) {
                log.debug(e.getMessage());
            }
        }
        FileUtils.copyFile(userConfigurationFile, new File(backupDirectory, "user.bak"));
        FileUtils.copyFile(realmsConfigurationFile, new File(backupDirectory, "realms.bak"));
        FileUtils.copyFile(new File(cm.getSecurityDirectory(), "realms.dat"), new File(backupDirectory, "realmpwd.bak"));
    }

    public void doWeeklyBackup(String backupDirectory) throws IOException {
        FileUtils.copyFile(userConfigurationFile, new File(backupDirectory, "user.wek"));
        FileUtils.copyFile(realmsConfigurationFile, new File(backupDirectory, "realms.wek"));
        FileUtils.copyFile(new File(cm.getSecurityDirectory(), "realms.dat"), new File(backupDirectory, "realmpwd.wek"));
    }

    private Properties loadUsers() {
        DelimitedInputStream dis = null;
        try {
            dis = new DelimitedInputStream(new FileInputStream(userConfigurationFile), 2048);
            JESProperties jesProperties = new JESProperties(dis);
            jesProperties.load();
            return jesProperties.getProperties();
        } catch (IOException e) {
            throw new RuntimeException("Error Loading User Configuration File!  Unable to continue Operation.");
        } finally {
            if (null != dis) {
                try {
                    dis.close();
                } catch (IOException ex) {
                    log.debug(ex.getMessage());
                }
            }
        }
    }

    private Map<String, UserFile> loadUserProperties() {
        userProperties = loadUsers();
        Map<String, UserFile> users = new HashMap<String, UserFile>();
        Enumeration propertyKeys = userProperties.keys();
        String key;
        String fullUsername;
        String correctedUsername;
        while (propertyKeys.hasMoreElements()) {
            key = (String) propertyKeys.nextElement();
            if (key.startsWith(USER_DEF_PREFIX)) {
                fullUsername = key.substring(USER_DEF_PREFIX.length());
                correctedUsername = fullUsername.toLowerCase(cm.englishLocale);
                try {
                    users.put(correctedUsername, loadUser(fullUsername, userProperties));
                } catch (InvalidAddressException e) {
                    throw new RuntimeException("Error Loading user File!  Unable to continue Operation. Improper syntax for " + fullUsername);
                }
            }
        }
        if (users.isEmpty() && !(Mail.testing || cm.isLocalTestingMode())) {
            log.warn("No users registered with any domain!!!");
        } else {
            if (log.isInfoEnabled()) {
                log.info("Loaded " + users.size() + " users from user.conf");
            }
        }
        return users;
    }

    private Map<String, Realm> loadRealmsProperties() {
        Properties properties = null;
        DelimitedInputStream dis = null;
        try {
            dis = new DelimitedInputStream(new FileInputStream(realmsConfigurationFile), 2048);
            JESProperties jesProperties = new JESProperties(dis);
            jesProperties.load();
            properties = jesProperties.getProperties();
        } catch (IOException e) {
            throw new RuntimeException("Error Loading realms File!  Unable to continue Operation.");
        } finally {
            try {
                dis.close();
            } catch (IOException ex) {
                log.debug(ex.getMessage());
            }
        }
        Map<String, Realm> realms = new HashMap<String, Realm>();
        Iterator<Entry<Object, Object>> iter = properties.entrySet().iterator();
        Entry realmDefinition;
        String key;
        String fullRealmName;
        String correctedRealmName;
        String domain;
        while (iter.hasNext()) {
            realmDefinition = iter.next();
            key = (String) realmDefinition.getKey();
            if (key.startsWith(REALM_DEF_PREFIX)) {
                fullRealmName = key.substring(REALM_DEF_PREFIX.length());
                correctedRealmName = fullRealmName.toLowerCase(cm.englishLocale);
                domain = correctedRealmName.substring(correctedRealmName.indexOf('@') + 1);
                if (localDomainsWithDefaultMailbox.containsKey(new Domain(domain))) {
                    realms.put(correctedRealmName, loadRealm(fullRealmName, (String) realmDefinition.getValue()));
                }
            }
        }
        if (log.isInfoEnabled()) {
            log.info("Loaded " + realms.size() + " realms from realms.conf");
        }
        return realms;
    }

    /**
    * Creates a new User instance for the specified username
    * using the specified properties.
    *
    * @param fullAddress full username (me@mydomain.com)
    * @param properties the properties that contain the user parameters.
    * @return a new User instance.
    */
    private UserFile loadUser(String fullAddress, Properties properties) throws InvalidAddressException {
        EmailAddress address = new EmailAddress(fullAddress);
        UserFile user = new UserFile(address);
        JESVaultControl.getInstance().setUserPassword(user, properties.getProperty(USER_DEF_PREFIX + fullAddress, "").trim());
        String forwardAddressesString = properties.getProperty(USER_PROPERTY_PREFIX + fullAddress + USER_FILE_FORWARDS, "").trim();
        String[] forwardAddresses = new String[0];
        if (forwardAddressesString != null && forwardAddressesString.trim().length() >= 0) {
            forwardAddresses = cm.tokenize(forwardAddressesString, true, ",");
        }
        List<EmailAddress> addressList = new ArrayList<EmailAddress>(forwardAddresses.length);
        for (int index = 0; index < forwardAddresses.length; index++) {
            try {
                addressList.add(new EmailAddress(forwardAddresses[index]));
            } catch (InvalidAddressException e) {
                log.warn("Forward address: " + forwardAddresses[index] + " for user " + user.getFullUsername() + " is invalid and will be ignored.");
            }
        }
        EmailAddress[] emailAddresses = new EmailAddress[addressList.size()];
        emailAddresses = (EmailAddress[]) addressList.toArray(emailAddresses);
        if (log.isDebugEnabled()) {
            log.debug(emailAddresses.length + " forward addresses load for user: " + user.getFullUsername());
        }
        user.setForwardAddresses(emailAddresses);
        return user;
    }

    /**
    * Creates a new Realm instance for the specified Realm name
    * while adding the users associated with the realm
    *
    * @param fullRealmName full Realm name (realmName@mydomain.com)
    * @param userList the String that contains the user name.
    * @return a new Realm instance.
    */
    public Realm loadRealm(String fullRealmName, String userList) {
        Realm realm = new Realm(fullRealmName);
        String domain = fullRealmName.substring(fullRealmName.indexOf('@') + 1).toLowerCase(cm.englishLocale);
        StringTokenizer st = new StringTokenizer(userList, ",");
        String user;
        while (st.hasMoreTokens()) {
            user = st.nextToken().toLowerCase(cm.englishLocale);
            if (user.contains(domain)) {
                if (users.containsKey(user)) {
                    realm.addUser(user);
                }
            }
        }
        return realm;
    }

    public Map<String, Map<String, String>> encryptRealmPasswords() {
        Locale englishLocale = Locale.ENGLISH;
        realmsPassModified = false;
        Map<String, Map<String, String>> realmsPass = null;
        realmsPasswordFile = new File(cm.getSecurityDirectory(), "realms.dat");
        if (realmsPasswordFile.exists()) {
            ObjectInputStream ois = null;
            try {
                ois = new ObjectInputStream(new FileInputStream(realmsPasswordFile));
                realmsPass = (Map<String, Map<String, String>>) ois.readObject();
            } catch (Exception e) {
                throw new RuntimeException("Error loading realms password File!  Unable to continue Operation.");
            } finally {
                if (ois != null) {
                    try {
                        ois.close();
                    } catch (IOException ex) {
                        log.debug(ex.getMessage());
                    }
                    ois = null;
                }
            }
        } else {
            realmsPass = new HashMap<String, Map<String, String>>();
        }
        try {
            String fullUsernameLowerCase;
            if (!realmsPass.containsKey("null")) {
                Map<String, String> nullRealm = new HashMap<String, String>();
                Iterator<UserFile> iter = users.values().iterator();
                UserFile user;
                while (iter.hasNext()) {
                    user = iter.next();
                    fullUsernameLowerCase = user.getFullUsername().toLowerCase(englishLocale);
                    nullRealm.put(fullUsernameLowerCase, user.getHashedRealmPassword(""));
                }
                realmsPassModified = true;
                realmsPass.put("null", nullRealm);
            } else {
                Map<String, String> nullRealm = realmsPass.get("null");
                Iterator<UserFile> iter = users.values().iterator();
                UserFile user;
                while (iter.hasNext()) {
                    user = iter.next();
                    fullUsernameLowerCase = user.getFullUsername().toLowerCase(englishLocale);
                    if (!nullRealm.containsKey(fullUsernameLowerCase) || user.isClearTextPassword()) {
                        nullRealm.put(fullUsernameLowerCase, user.getHashedRealmPassword(""));
                        realmsPassModified = true;
                    }
                }
                realmsPass.put("null", nullRealm);
            }
            Iterator<String> iterOut = realms.keySet().iterator();
            String aRealmName;
            UserFile user;
            while (iterOut.hasNext()) {
                aRealmName = iterOut.next();
                if (!realmsPass.containsKey(aRealmName)) {
                    Map<String, String> aRealm = new HashMap<String, String>();
                    Iterator<String> iter = realms.get(aRealmName).UserIterator();
                    while (iter.hasNext()) {
                        user = users.get(iter.next());
                        fullUsernameLowerCase = user.getFullUsername().toLowerCase(englishLocale);
                        if (user != null) {
                            if (user.isClearTextPassword()) {
                                aRealm.put(fullUsernameLowerCase, user.getHashedRealmPassword(realms.get(aRealmName).getFullRealmName()));
                            }
                        }
                    }
                    realmsPassModified = true;
                    realmsPass.put(aRealmName, aRealm);
                } else {
                    Map<String, String> aRealm = realmsPass.get(aRealmName);
                    Iterator<String> iter = realms.get(aRealmName).UserIterator();
                    while (iter.hasNext()) {
                        user = users.get(iter.next());
                        fullUsernameLowerCase = user.getFullUsername().toLowerCase(englishLocale);
                        if (user != null) {
                            if (!aRealm.containsKey(fullUsernameLowerCase) || user.isClearTextPassword()) {
                                aRealm.put(fullUsernameLowerCase, user.getHashedRealmPassword(realms.get(aRealmName).getFullRealmName()));
                                realmsPassModified = true;
                            }
                        }
                    }
                    realmsPass.put(aRealmName, aRealm);
                }
            }
            iterOut = realmsPass.keySet().iterator();
            Iterator<String> iterIn;
            Map<String, String> aRealm;
            while (iterOut.hasNext()) {
                aRealmName = iterOut.next();
                if (!realms.containsKey(aRealmName)) {
                    iterOut.remove();
                    realmsPassModified = true;
                } else {
                    aRealm = realmsPass.get(aRealmName);
                    iterIn = aRealm.keySet().iterator();
                    while (iterIn.hasNext()) {
                        if (!isUserARealmMember(realms.get(aRealmName), iterIn.next())) {
                            iterIn.remove();
                            realmsPassModified = true;
                        }
                    }
                }
            }
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Error updating Realms. Unable to continue Operation.");
        }
        return realmsPass;
    }

    public void persistUsersAndRealms() {
        if (realmsPassModified) {
            persistRealmsPassFile();
        }
        persistUserConfFile();
    }

    private void persistRealmsPassFile() {
        if (!realmsPasswordFile.exists()) {
            try {
                if (!realmsPasswordFile.createNewFile()) {
                    throw new IOException();
                }
            } catch (Exception e) {
                throw new RuntimeException("Error creating realms password File!  Unable to continue Operation.");
            }
        }
        realmsPassModified = false;
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(new FileOutputStream(realmsPasswordFile));
            oos.writeObject(realmsPass);
        } catch (Exception e) {
            throw new RuntimeException("Error storing realms password File!  Unable to continue Operation.");
        } finally {
            if (oos != null) {
                try {
                    oos.close();
                } catch (IOException ex) {
                    log.debug(ex.getMessage());
                }
                oos = null;
            }
        }
    }

    private void persistUserConfFile() {
        boolean userConfModified = false;
        UserFile user;
        Iterator<UserFile> iter = users.values().iterator();
        String username, password;
        while (iter.hasNext()) {
            user = iter.next();
            if (user.isClearTextPassword()) {
                username = user.getFullUsername();
                password = user.getEncryptedPassword();
                if (password == null) {
                    log.error("Error encrypting plaintext password from user.conf for user " + username);
                    throw new RuntimeException("Error encrypting password for user: " + username);
                }
                password = "{ENC}" + password;
                userProperties.setProperty(USER_DEF_PREFIX + username, password);
                userConfModified = true;
            }
        }
        if (userConfModified) {
            try {
                JESProperties.store(userProperties, userConfigurationFile, USER_PROPERTIES_HEADER);
                log.info("Changes to user.conf persisted to disk.");
            } catch (IOException e) {
                log.error("Unable to store changes to user.conf!  Plain text passwords were not hashed!");
                throw new RuntimeException("Error storing changes to user.conf.");
            }
        }
        userProperties.clear();
        userProperties = null;
        userConfigurationFileTimestamp = userConfigurationFile.lastModified();
    }

    public void updateThroughConnection(CBCExecutor cbcExecutor) {
        Thread thread = Thread.currentThread();
        List<NewUser> newUsers = ((AddUsersPLL1) cbcExecutor).getNewUsers();
        NewUser newUser;
        UserFile outputUser;
        String correctedName;
        boolean updateUserFile = false;
        Iterator<NewUser> iter = newUsers.iterator();
        while (iter.hasNext()) {
            newUser = iter.next();
            correctedName = newUser.username.toLowerCase(cm.englishLocale);
            if (!users.containsKey(correctedName)) {
                try {
                    outputUser = new UserFile(new EmailAddress(newUser.username));
                    outputUser.setPassword(newUser.password);
                    if (log.isDebugEnabled()) {
                        log.debug("adding " + correctedName + " to user map");
                    }
                    users.put(correctedName, outputUser);
                    updateUserFile = true;
                } catch (InvalidAddressException e) {
                    log.info("The user " + newUser.username + " was not added to the list of users.", e);
                    iter.remove();
                }
            } else {
                log.info("The user " + newUser.username + " already exists and was not added.");
                iter.remove();
            }
            if (thread.isInterrupted()) {
                return;
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Added " + newUsers.size() + " new users to user map");
        }
        Realm aRealm;
        String domain;
        iter = newUsers.iterator();
        while (iter.hasNext()) {
            newUser = iter.next();
            if (newUser.realm != null && newUser.realm.length() > 0) {
                correctedName = newUser.realm.toLowerCase(cm.englishLocale);
                if (!realms.containsKey(correctedName)) {
                    domain = correctedName.substring(correctedName.indexOf('@') + 1);
                    if (cm.isLocalDomain(domain)) {
                        if (log.isDebugEnabled()) {
                            log.debug("adding realm " + correctedName + " to realm map");
                        }
                        realms.put(correctedName, loadRealm(newUser.realm, newUser.username));
                    }
                } else {
                    aRealm = realms.get(correctedName);
                    correctedName = newUser.username.toLowerCase(cm.englishLocale);
                    if (!isUserARealmMember(aRealm, correctedName)) {
                        if (log.isDebugEnabled()) {
                            log.debug("adding new user " + correctedName + " to realm " + aRealm.getFullRealmName());
                        }
                        aRealm.addUser(correctedName);
                    }
                }
            }
            if (thread.isInterrupted()) {
                return;
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Completed adding new realms (if any) to realm map");
        }
        if (updateUserFile) {
            while (cm.updatingFiles) {
                if (log.isDebugEnabled()) {
                    log.debug("Waiting other process(es) to stop updating files");
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.interrupted();
                }
                if (Mail.getInstance().isShuttingDown()) {
                    return;
                }
            }
            cm.updatingFiles = true;
            Properties realmProperties = new Properties();
            StringBuilder userSB;
            Iterator<String> iter3 = realms.keySet().iterator();
            Iterator<String> iter2;
            while (iter3.hasNext()) {
                userSB = new StringBuilder(30);
                aRealm = realms.get(iter3.next());
                iter2 = aRealm.UserIterator();
                while (iter2.hasNext()) {
                    userSB.append(users.get(iter2.next()).getFullUsername()).append(',');
                }
                correctedName = userSB.toString();
                if (correctedName.length() > 0) {
                    correctedName = correctedName.substring(0, correctedName.length() - 1);
                    if (log.isDebugEnabled()) {
                        log.debug("setting entry " + correctedName + " for realm " + aRealm.getFullRealmName());
                    }
                    realmProperties.put("realm." + aRealm.getFullRealmName(), correctedName);
                }
            }
            cm.disableNotify = true;
            UserFile aUser;
            Properties userProperties = new Properties();
            iter3 = users.keySet().iterator();
            while (iter3.hasNext()) {
                aUser = users.get(iter3.next());
                if (log.isDebugEnabled()) {
                    log.debug("adding user " + aUser.getFullUsername() + " for persistence");
                }
                userProperties.put("user." + aUser.getFullUsername(), aUser.getEncryptedPassword());
            }
            if (Mail.getInstance().isShuttingDown() || thread.isInterrupted()) {
                return;
            }
            try {
                JESProperties.store(realmProperties, realmsConfigurationFile, REALMS_PROPERTIES_HEADER);
                log.info("Changes to realms.conf persisted to disk.");
            } catch (IOException e) {
                restore();
                log.error("Unable to store changes to realms.conf!");
                return;
            }
            try {
                JESProperties.store(userProperties, userConfigurationFile, USER_PROPERTIES_HEADER);
                log.info("Changes to user.conf persisted to disk.");
            } catch (IOException e) {
                restore();
                log.error("Unable to store changes to user.conf!  Plain text passwords were not hashed!");
            }
            cm.updatingFiles = false;
        }
    }

    private void restore() {
        try {
            restore(cm.getBackupDirectory());
        } catch (IOException e) {
        }
    }

    public boolean persistUserUpdate() {
        if (userConfigurationFile.lastModified() != userConfigurationFileTimestamp) {
            log.info("User Configuration File Changed, reloading...");
            return true;
        }
        return false;
    }

    public DomainWithPassword getRealmPassword(String realmName, String username) {
        Map<String, String> realmPass = realmsPass.get(realmName);
        if (log.isInfoEnabled() && realmPass == null) {
            log.info("Tried to load non-existent realmPass: " + realmName);
            return null;
        }
        if (realmName.equals("null") && username.indexOf('@') == -1) {
            if (log.isDebugEnabled()) {
                log.debug("Checking all the available domains");
            }
            User user;
            Domain domain;
            String password;
            Iterator<Domain> domains = localDomainsWithDefaultMailbox.keySet().iterator();
            while (domains.hasNext()) {
                domain = domains.next();
                try {
                    user = getUser(new EmailAddress(username + "@" + domain.getDomainName()));
                    if (user != null) {
                        password = realmPass.get(username + "@" + domain.getDomainName());
                        if (password != null) {
                            return new DomainWithPassword(domain, password.toCharArray());
                        }
                    }
                } catch (InvalidAddressException iae) {
                    continue;
                }
            }
            return null;
        } else {
            char[] password = realmPass.get(username).toCharArray();
            if (password == null) {
                return null;
            }
            return new DomainWithPassword(null, password);
        }
    }

    public void loadUsersAndRealms() {
        users = loadUserProperties();
        if (users.isEmpty() && !(Mail.testing || cm.isLocalTestingMode())) {
            log.error("No users registered, aborting startup. Please consult the documentation.");
            throw new RuntimeException("No users registered, aborting startup. Please consult the documentation.");
        }
        realms = loadRealmsProperties();
        realmsPass = encryptRealmPasswords();
    }

    public void updateUsersAndRealmPasswords() {
        users = loadUserProperties();
        realmsPass = encryptRealmPasswords();
    }

    public String getRealmsForResponse() {
        Iterator iter = realms.values().iterator();
        Realm aRealm;
        StringBuilder challenge = new StringBuilder(100);
        while (iter.hasNext()) {
            aRealm = (Realm) iter.next();
            if (aRealm.getFullRealmName().equals("null")) {
                continue;
            }
            challenge.append("realm=\"").append(aRealm.getFullRealmName()).append("\"");
            challenge.append(",");
        }
        return challenge.toString();
    }

    public void updateDomains(String domains, String defaultMailboxes) {
        String[] result1 = domains.toLowerCase(cm.englishLocale).split(" ");
        String[] result2 = defaultMailboxes.trim().split(" ");
        defaultDomain = new Domain(result1[0].trim());
        localDomainsWithDefaultMailbox = new HashMap<Domain, EmailAddress>(result1.length);
        String domain, dUser;
        for (int i = 0; i < result1.length; i++) {
            domain = result1[i].trim();
            dUser = null;
            for (int j = 0; j < result2.length; j++) {
                if (result2[j].trim().toLowerCase(cm.englishLocale).contains(domain)) {
                    dUser = result2[j].trim();
                    break;
                }
            }
            try {
                localDomainsWithDefaultMailbox.put(new Domain(domain), dUser == null ? new EmailAddress() : new EmailAddress(dUser));
            } catch (InvalidAddressException iae) {
                throw new RuntimeException("Invalid username " + dUser);
            }
        }
    }

    public boolean isLocalDomain(Domain domain) {
        return localDomainsWithDefaultMailbox.keySet().contains(domain);
    }

    public Domain getDefaultDomain() {
        return defaultDomain;
    }

    public void updateDefaultDomain() {
    }

    public EmailAddress getDefaultMailbox(Domain domain) {
        return localDomainsWithDefaultMailbox.get(domain);
    }

    public User getUser(EmailAddress address) {
        return users.get(address.getAddress().toLowerCase(cm.englishLocale));
    }

    /** realmName expected to be lower case **/
    public Realm getRealm(String realmName) {
        return realms.get(realmName);
    }

    public boolean isUserARealmMember(Realm realm, String username_lower_case) {
        return realm.isUserAMember(username_lower_case);
    }
}
