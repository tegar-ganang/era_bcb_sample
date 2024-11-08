package com.ericdaugherty.mail.server.configuration.backEnd;

import java.security.GeneralSecurityException;
import java.sql.*;
import java.util.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.ericdaugherty.mail.server.configuration.cbc.NewRealms;
import com.ericdaugherty.mail.server.configuration.cbc.NewUser;
import com.ericdaugherty.mail.server.configuration.*;
import com.ericdaugherty.mail.server.dbAccess.ExecuteProcessAbstractImpl;
import com.ericdaugherty.mail.server.dbAccess.ProcessEnvelope;
import com.ericdaugherty.mail.server.info.EmailAddress;
import com.ericdaugherty.mail.server.info.Realm;
import com.ericdaugherty.mail.server.info.db.DomainDb;
import com.ericdaugherty.mail.server.info.db.RealmDb;
import java.io.File;

/**
 *
 * @author Andreas Kyrmegalos
 */
public class DbPersistExecutor implements PersistExecutor {

    /** Logger */
    private static Log log = LogFactory.getLog(DbPersistExecutor.class);

    private final ConfigurationManager cm = ConfigurationManager.getInstance();

    private final ConfigurationManagerDb cmDB = (ConfigurationManagerDb) cm.getBackEnd();

    private Connection connection;

    private Properties sqlCommands;

    private Locale locale = Locale.ENGLISH;

    public void insertDomain(final List<String> domains) {
        try {
            connection.setAutoCommit(false);
            new ProcessEnvelope().executeNull(new ExecuteProcessAbstractImpl(connection, false, false, true, true) {

                @Override
                public void executeProcessReturnNull() throws SQLException {
                    psImpl = connImpl.prepareStatement(sqlCommands.getProperty("domain.add"));
                    Iterator<String> iter = domains.iterator();
                    String domain;
                    while (iter.hasNext()) {
                        domain = iter.next();
                        psImpl.setString(1, domain);
                        psImpl.setString(2, domain.toLowerCase(locale));
                        psImpl.executeUpdate();
                    }
                }
            });
            connection.commit();
            cmDB.updateDomains(null, null);
        } catch (SQLException sqle) {
            log.error(sqle);
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ex) {
                }
            }
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                } catch (SQLException ex) {
                    log.error(ex);
                }
            }
        }
    }

    public void deleteDomain(final List<Integer> domainIds) {
        try {
            connection.setAutoCommit(false);
            final int defaultDomainId = ((DomainDb) cmDB.getDefaultDomain()).getDomainId();
            boolean defaultDomainDeleted = (Boolean) new ProcessEnvelope().executeObject(new ExecuteProcessAbstractImpl(connection, false, false, true, true) {

                @Override
                public Object executeProcessReturnObject() throws SQLException {
                    psImpl = connImpl.prepareStatement(sqlCommands.getProperty("domain.delete"));
                    Iterator<Integer> iter = domainIds.iterator();
                    int domainId;
                    boolean defaultDomainDeleted = false;
                    while (iter.hasNext()) {
                        domainId = iter.next();
                        if (!defaultDomainDeleted) defaultDomainDeleted = defaultDomainId == domainId;
                        psImpl.setInt(1, domainId);
                        psImpl.executeUpdate();
                    }
                    return defaultDomainDeleted;
                }
            });
            if (defaultDomainDeleted) {
                new ProcessEnvelope().executeNull(new ExecuteProcessAbstractImpl(connection, false, false, true, true) {

                    @Override
                    public void executeProcessReturnNull() throws SQLException {
                        psImpl = connImpl.prepareStatement(sqlCommands.getProperty("domain.setDefaultDomainId"));
                        psImpl.setInt(1, -1);
                        psImpl.executeUpdate();
                    }
                });
            }
            connection.commit();
            cmDB.updateDomains(null, null);
            if (defaultDomainDeleted) {
                cm.updateDefaultDomain();
            }
        } catch (SQLException sqle) {
            log.error(sqle);
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ex) {
                }
            }
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                } catch (SQLException ex) {
                }
            }
        }
    }

    public void setDefaultDomain(final int domainId) {
        try {
            connection.setAutoCommit(false);
            new ProcessEnvelope().executeNull(new ExecuteProcessAbstractImpl(connection, false, false, true, true) {

                @Override
                public void executeProcessReturnNull() throws SQLException {
                    psImpl = connImpl.prepareStatement(sqlCommands.getProperty("domain.setDefaultDomainId"));
                    psImpl.setInt(1, domainId);
                    psImpl.executeUpdate();
                }
            });
            connection.commit();
            cm.updateDefaultDomain();
        } catch (SQLException sqle) {
            log.error(sqle);
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ex) {
                }
            }
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                } catch (SQLException ex) {
                }
            }
        }
    }

    private final class RealmWithEncryptedPass {

        private final Realm realm;

        private final String password;

        public RealmWithEncryptedPass(Realm realm, String password) {
            this.realm = realm;
            this.password = password;
        }
    }

    private class PasswordAndSalt {

        private String password;

        private String salt;

        public PasswordAndSalt(String password, String salt) {
            this.password = password;
            this.salt = salt;
        }
    }

    public void insertUser(final List<NewUser> newUsers) {
        try {
            connection.setAutoCommit(false);
            final Map<String, PasswordAndSalt> pass = new HashMap<String, PasswordAndSalt>();
            final Map<String, List<RealmWithEncryptedPass>> realmPass = new HashMap<String, List<RealmWithEncryptedPass>>();
            final List<String> userDirs = new ArrayList<String>();
            Iterator<NewUser> iter = newUsers.iterator();
            NewUser user;
            Realm realm;
            String username;
            PasswordHasher ph;
            while (iter.hasNext()) {
                user = iter.next();
                username = user.username.toLowerCase(locale);
                ph = PasswordFactory.getInstance().getPasswordHasher();
                pass.put(user.username, new PasswordAndSalt(ph.hashPassword(user.password), ph.getSalt()));
                realmPass.put(user.username, new ArrayList<RealmWithEncryptedPass>());
                realmPass.get(user.username).add(new RealmWithEncryptedPass(cm.getRealm("null"), PasswordFactory.getInstance().getPasswordHasher().hashRealmPassword(username, "", user.password)));
                if (user.realms != null) {
                    for (String realmName : user.realms) {
                        realm = cm.getRealm(realmName);
                        realmPass.get(user.username).add(new RealmWithEncryptedPass(realm, PasswordFactory.getInstance().getPasswordHasher().hashRealmPassword(username, realm.getFullRealmName(), user.password)));
                    }
                    user.realms = null;
                }
            }
            new ProcessEnvelope().executeNull(new ExecuteProcessAbstractImpl(connection, false, false, true, true) {

                @Override
                public void executeProcessReturnNull() throws SQLException {
                    psImpl = connImpl.prepareStatement(sqlCommands.getProperty("user.add"), Statement.RETURN_GENERATED_KEYS);
                    Iterator<NewUser> iter = newUsers.iterator();
                    NewUser user;
                    DomainDb domain = null;
                    while (iter.hasNext()) {
                        user = iter.next();
                        psImpl.setString(1, user.username);
                        psImpl.setString(2, user.username.toLowerCase(locale));
                        if (domain == null || (domain.getDomainId() != user.domainId)) {
                            domain = (DomainDb) cmDB.getDomain(user.domainId);
                        }
                        userDirs.add(user.username + '@' + domain.getDomainName());
                        psImpl.setInt(3, user.domainId);
                        psImpl.setString(4, pass.get(user.username).password);
                        psImpl.setString(5, pass.get(user.username).salt);
                        psImpl.executeUpdate();
                        rsImpl = psImpl.getGeneratedKeys();
                        if (rsImpl.next()) {
                            user.userId = rsImpl.getInt(1);
                            rsImpl.close();
                        } else {
                            throw new SQLException("Need to have a user id generated.");
                        }
                    }
                }
            });
            new ProcessEnvelope().executeNull(new ExecuteProcessAbstractImpl(connection, false, false, true, true) {

                @Override
                public void executeProcessReturnNull() throws SQLException {
                    psImpl = connImpl.prepareStatement(sqlCommands.getProperty("realm.addUser"));
                    Iterator<NewUser> iter = newUsers.iterator();
                    NewUser user;
                    List<RealmWithEncryptedPass> list;
                    RealmWithEncryptedPass rwep;
                    RealmDb realm;
                    while (iter.hasNext()) {
                        user = iter.next();
                        list = realmPass.get(user.username);
                        if (list != null) {
                            Iterator<RealmWithEncryptedPass> iter1 = list.iterator();
                            while (iter1.hasNext()) {
                                rwep = iter1.next();
                                realm = (RealmDb) rwep.realm;
                                psImpl.setInt(1, realm.getRealmId());
                                psImpl.setInt(2, user.userId);
                                psImpl.setInt(3, user.domainId);
                                psImpl.setString(4, rwep.password);
                                psImpl.executeUpdate();
                            }
                        }
                    }
                }
            });
            connection.commit();
            Iterator<String> iterator = userDirs.iterator();
            while (iterator.hasNext()) {
                cm.requestDirCreation(new File(cm.getUsersDirectory(), iterator.next()).getPath());
            }
            cm.createDirectories();
        } catch (GeneralSecurityException e) {
            log.error(e);
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ex) {
                }
            }
            throw new RuntimeException("Error updating Realms. Unable to continue Operation.");
        } catch (SQLException sqle) {
            log.error(sqle);
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ex) {
                }
            }
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                } catch (SQLException ex) {
                }
            }
        }
    }

    public void deleteUser(final List<Integer> userIds) {
        try {
            connection.setAutoCommit(false);
            new ProcessEnvelope().executeNull(new ExecuteProcessAbstractImpl(connection, false, false, true, true) {

                @Override
                public void executeProcessReturnNull() throws SQLException {
                    psImpl = connImpl.prepareStatement(sqlCommands.getProperty("user.delete"));
                    Iterator<Integer> iter = userIds.iterator();
                    int userId;
                    while (iter.hasNext()) {
                        userId = iter.next();
                        psImpl.setInt(1, userId);
                        psImpl.executeUpdate();
                    }
                }
            });
            connection.commit();
            cmDB.removeUsers(userIds);
        } catch (SQLException sqle) {
            log.error(sqle);
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ex) {
                }
            }
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                } catch (SQLException ex) {
                }
            }
        }
    }

    private class JESRealmUser {

        private int userId, realmId, domainId;

        private String username, password, realm;

        public JESRealmUser(String username, int userId, int realmId, int domainId, String password, String realm) {
            this.username = username;
            this.userId = userId;
            this.realmId = realmId;
            this.domainId = domainId;
            this.password = password;
            this.realm = realm;
        }
    }

    public void setUserPassword(final List<NewUser> users) {
        try {
            final List<Integer> usersToRemoveFromCache = new ArrayList<Integer>();
            connection.setAutoCommit(false);
            new ProcessEnvelope().executeNull(new ExecuteProcessAbstractImpl(connection, false, false, true, true) {

                @Override
                public void executeProcessReturnNull() throws SQLException {
                    psImpl = connImpl.prepareStatement(sqlCommands.getProperty("user.updatePassword"));
                    Iterator<NewUser> iter = users.iterator();
                    NewUser user;
                    PasswordHasher ph;
                    while (iter.hasNext()) {
                        user = iter.next();
                        ph = PasswordFactory.getInstance().getPasswordHasher();
                        psImpl.setString(1, ph.hashPassword(user.password));
                        psImpl.setString(2, ph.getSalt());
                        psImpl.setInt(3, user.userId);
                        psImpl.executeUpdate();
                        usersToRemoveFromCache.add(user.userId);
                    }
                }
            });
            List<JESRealmUser> list = (List<JESRealmUser>) new ProcessEnvelope().executeObject(new ExecuteProcessAbstractImpl(connection, false, false, true, true) {

                @Override
                public Object executeProcessReturnObject() throws SQLException {
                    List<JESRealmUser> list = new ArrayList<JESRealmUser>(users.size() + 10);
                    psImpl = connImpl.prepareStatement(sqlCommands.getProperty("realms.user.load"));
                    Iterator<NewUser> iter = users.iterator();
                    NewUser user;
                    while (iter.hasNext()) {
                        user = iter.next();
                        psImpl.setInt(1, user.userId);
                        rsImpl = psImpl.executeQuery();
                        while (rsImpl.next()) {
                            list.add(new JESRealmUser(user.username, user.userId, rsImpl.getInt("realm_id"), rsImpl.getInt("domain_id"), user.password, rsImpl.getString("realm_name_lower_case")));
                        }
                    }
                    return list;
                }
            });
            final List<JESRealmUser> encrypted = new ArrayList<JESRealmUser>(list.size());
            Iterator<JESRealmUser> iter = list.iterator();
            JESRealmUser jesRealmUser;
            Realm realm;
            while (iter.hasNext()) {
                jesRealmUser = iter.next();
                realm = cm.getRealm(jesRealmUser.realm);
                encrypted.add(new JESRealmUser(null, jesRealmUser.userId, jesRealmUser.realmId, jesRealmUser.domainId, PasswordFactory.getInstance().getPasswordHasher().hashRealmPassword(jesRealmUser.username.toLowerCase(locale), realm.getFullRealmName().equals("null") ? "" : realm.getFullRealmName(), jesRealmUser.password), null));
            }
            new ProcessEnvelope().executeNull(new ExecuteProcessAbstractImpl(connection, false, false, true, true) {

                @Override
                public void executeProcessReturnNull() throws SQLException {
                    psImpl = connImpl.prepareStatement(sqlCommands.getProperty("realms.user.update"));
                    Iterator<JESRealmUser> iter = encrypted.iterator();
                    JESRealmUser jesRealmUser;
                    while (iter.hasNext()) {
                        jesRealmUser = iter.next();
                        psImpl.setString(1, jesRealmUser.password);
                        psImpl.setInt(2, jesRealmUser.realmId);
                        psImpl.setInt(3, jesRealmUser.userId);
                        psImpl.setInt(4, jesRealmUser.domainId);
                        psImpl.executeUpdate();
                    }
                }
            });
            connection.commit();
            cmDB.removeUsers(usersToRemoveFromCache);
        } catch (GeneralSecurityException e) {
            log.error(e);
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ex) {
                }
            }
            throw new RuntimeException("Error updating Realms. Unable to continue Operation.");
        } catch (SQLException sqle) {
            log.error(sqle);
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ex) {
                }
            }
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                } catch (SQLException ex) {
                }
            }
        }
    }

    public void addForwardAddress(final List<NewUser> forwardAddresses) {
        try {
            final List<Integer> usersToRemoveFromCache = new ArrayList<Integer>();
            connection.setAutoCommit(false);
            new ProcessEnvelope().executeNull(new ExecuteProcessAbstractImpl(connection, false, false, true, true) {

                @Override
                public void executeProcessReturnNull() throws SQLException {
                    psImpl = connImpl.prepareStatement(sqlCommands.getProperty("userForwardAddresses.add"));
                    Iterator<NewUser> iter = forwardAddresses.iterator();
                    Iterator<String> iter2;
                    NewUser newUser;
                    while (iter.hasNext()) {
                        newUser = iter.next();
                        psImpl.setInt(1, newUser.userId);
                        iter2 = newUser.forwardAddresses.iterator();
                        while (iter2.hasNext()) {
                            psImpl.setString(2, iter2.next());
                            psImpl.executeUpdate();
                        }
                        usersToRemoveFromCache.add(newUser.userId);
                    }
                }
            });
            connection.commit();
            cmDB.removeUsers(usersToRemoveFromCache);
        } catch (SQLException sqle) {
            log.error(sqle);
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ex) {
                }
            }
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                } catch (SQLException ex) {
                }
            }
        }
    }

    public void removeForwardAddress(final List<NewUser> forwardAddresses) {
        try {
            final List<Integer> usersToRemoveFromCache = new ArrayList<Integer>();
            connection.setAutoCommit(false);
            new ProcessEnvelope().executeNull(new ExecuteProcessAbstractImpl(connection, false, false, true, true) {

                @Override
                public void executeProcessReturnNull() throws SQLException {
                    psImpl = connImpl.prepareStatement(sqlCommands.getProperty("userForwardAddresses.delete"));
                    Iterator<NewUser> iter = forwardAddresses.iterator();
                    Iterator<Integer> iter2;
                    NewUser newUser;
                    while (iter.hasNext()) {
                        newUser = iter.next();
                        iter2 = newUser.forwardAddressIds.iterator();
                        while (iter2.hasNext()) {
                            psImpl.setInt(1, iter2.next());
                            psImpl.executeUpdate();
                        }
                        usersToRemoveFromCache.add(newUser.userId);
                    }
                }
            });
            connection.commit();
            cmDB.removeUsers(usersToRemoveFromCache);
        } catch (SQLException sqle) {
            log.error(sqle);
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ex) {
                }
            }
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                } catch (SQLException ex) {
                }
            }
        }
    }

    public void setDefaultMailBox(final int domainId, final int userId) {
        final EmailAddress defaultMailbox = cmDB.getDefaultMailbox(domainId);
        try {
            connection.setAutoCommit(false);
            new ProcessEnvelope().executeNull(new ExecuteProcessAbstractImpl(connection, false, false, true, true) {

                @Override
                public void executeProcessReturnNull() throws SQLException {
                    psImpl = connImpl.prepareStatement(sqlCommands.getProperty(defaultMailbox == null ? "domain.setDefaultMailbox" : "domain.updateDefaultMailbox"));
                    if (defaultMailbox == null) {
                        psImpl.setInt(1, domainId);
                        psImpl.setInt(2, userId);
                    } else {
                        psImpl.setInt(1, userId);
                        psImpl.setInt(2, domainId);
                    }
                    psImpl.executeUpdate();
                }
            });
            connection.commit();
            cmDB.updateDomains(null, null);
        } catch (SQLException sqle) {
            log.error(sqle);
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ex) {
                }
            }
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                } catch (SQLException ex) {
                }
            }
        }
    }

    public void insertRealm(final List<NewRealms> newRealms) {
        try {
            connection.setAutoCommit(false);
            new ProcessEnvelope().executeNull(new ExecuteProcessAbstractImpl(connection, false, false, true, true) {

                @Override
                public void executeProcessReturnNull() throws SQLException {
                    psImpl = connImpl.prepareStatement(sqlCommands.getProperty("realm.add"));
                    Iterator<NewRealms> iter = newRealms.iterator();
                    NewRealms newRealm;
                    String realm;
                    Iterator<String> iter2;
                    while (iter.hasNext()) {
                        newRealm = iter.next();
                        psImpl.setInt(3, newRealm.domainId);
                        iter2 = newRealm.realms.iterator();
                        while (iter2.hasNext()) {
                            realm = iter2.next();
                            psImpl.setString(1, realm);
                            psImpl.setString(2, realm.toLowerCase(locale));
                            psImpl.executeUpdate();
                        }
                    }
                }
            });
            connection.commit();
        } catch (SQLException sqle) {
            log.error(sqle);
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ex) {
                }
            }
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                } catch (SQLException ex) {
                }
            }
        }
    }

    public void removeRealm(final List<Integer> realmIds) {
        try {
            connection.setAutoCommit(false);
            new ProcessEnvelope().executeNull(new ExecuteProcessAbstractImpl(connection, false, false, true, true) {

                @Override
                public void executeProcessReturnNull() throws SQLException {
                    psImpl = connImpl.prepareStatement(sqlCommands.getProperty("realm.remove"));
                    Iterator<Integer> iter = realmIds.iterator();
                    int realmId;
                    while (iter.hasNext()) {
                        realmId = iter.next();
                        psImpl.setInt(1, realmId);
                        psImpl.executeUpdate();
                        cmDB.removeRealm(realmId);
                    }
                }
            });
            connection.commit();
        } catch (SQLException sqle) {
            log.error(sqle);
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ex) {
                }
            }
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                } catch (SQLException ex) {
                }
            }
        }
    }

    public void addUserToRealm(final NewUser user) {
        try {
            connection.setAutoCommit(false);
            final String pass, salt;
            final List<RealmWithEncryptedPass> realmPass = new ArrayList<RealmWithEncryptedPass>();
            Realm realm;
            String username;
            username = user.username.toLowerCase(locale);
            PasswordHasher ph = PasswordFactory.getInstance().getPasswordHasher();
            pass = ph.hashPassword(user.password);
            salt = ph.getSalt();
            realmPass.add(new RealmWithEncryptedPass(cm.getRealm("null"), PasswordFactory.getInstance().getPasswordHasher().hashRealmPassword(username, "", user.password)));
            if (user.realms != null) {
                for (String realmName : user.realms) {
                    realm = cm.getRealm(realmName);
                    realmPass.add(new RealmWithEncryptedPass(realm, PasswordFactory.getInstance().getPasswordHasher().hashRealmPassword(username, realm.getFullRealmName(), user.password)));
                }
                user.realms = null;
            }
            new ProcessEnvelope().executeNull(new ExecuteProcessAbstractImpl(connection, false, false, true, true) {

                @Override
                public void executeProcessReturnNull() throws SQLException {
                    psImpl = connImpl.prepareStatement(sqlCommands.getProperty("user.updatePassword"));
                    psImpl.setString(1, pass);
                    psImpl.setString(2, salt);
                    psImpl.setInt(3, user.userId);
                    psImpl.executeUpdate();
                }
            });
            new ProcessEnvelope().executeNull(new ExecuteProcessAbstractImpl(connection, false, false, true, true) {

                @Override
                public void executeProcessReturnNull() throws SQLException {
                    psImpl = connImpl.prepareStatement(sqlCommands.getProperty("realm.addUser"));
                    RealmWithEncryptedPass rwep;
                    RealmDb realm;
                    Iterator<RealmWithEncryptedPass> iter1 = realmPass.iterator();
                    while (iter1.hasNext()) {
                        rwep = iter1.next();
                        realm = (RealmDb) rwep.realm;
                        psImpl.setInt(1, realm.getRealmId());
                        psImpl.setInt(2, user.userId);
                        psImpl.setInt(3, user.domainId);
                        psImpl.setString(4, rwep.password);
                        psImpl.executeUpdate();
                    }
                }
            });
            connection.commit();
            cmDB.removeUser(user.userId);
        } catch (GeneralSecurityException e) {
            log.error(e);
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ex) {
                }
            }
            throw new RuntimeException("Error updating Realms. Unable to continue Operation.");
        } catch (SQLException sqle) {
            log.error(sqle);
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ex) {
                }
            }
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                } catch (SQLException ex) {
                }
            }
        }
    }

    public void removeUserFromRealm(final List<NewUser> users) {
        try {
            connection.setAutoCommit(false);
            final List<Integer> removeFromNullRealm = (List<Integer>) new ProcessEnvelope().executeObject(new ExecuteProcessAbstractImpl(connection, false, false, true, true) {

                @Override
                public Object executeProcessReturnObject() throws SQLException {
                    psImpl = connImpl.prepareStatement(sqlCommands.getProperty("realm.removeUser"));
                    Iterator<NewUser> iter = users.iterator();
                    NewUser user;
                    int realmId;
                    Iterator<Integer> iter2;
                    List<Integer> removeFromNullRealm = new ArrayList<Integer>();
                    while (iter.hasNext()) {
                        user = iter.next();
                        psImpl.setInt(1, user.userId);
                        iter2 = user.realmIds.iterator();
                        while (iter2.hasNext()) {
                            realmId = iter2.next();
                            if (realmId == 0) {
                                removeFromNullRealm.add(user.userId);
                                continue;
                            }
                            psImpl.setInt(2, realmId);
                            psImpl.executeUpdate();
                        }
                        cmDB.removeUser(user.userId);
                    }
                    return removeFromNullRealm;
                }
            });
            if (!removeFromNullRealm.isEmpty()) {
                new ProcessEnvelope().executeNull(new ExecuteProcessAbstractImpl(connection, false, false, true, true) {

                    @Override
                    public void executeProcessReturnNull() throws SQLException {
                        psImpl = connImpl.prepareStatement(sqlCommands.getProperty("realm.removeUserFromNullRealm"));
                        Iterator<Integer> iter2 = removeFromNullRealm.iterator();
                        while (iter2.hasNext()) {
                            psImpl.setInt(1, iter2.next());
                            psImpl.executeUpdate();
                        }
                    }
                });
            }
            connection.commit();
        } catch (SQLException sqle) {
            log.error(sqle);
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ex) {
                }
            }
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                } catch (SQLException ex) {
                }
            }
        }
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public void setSqlCommands(Properties sqlCommands) {
        this.sqlCommands = sqlCommands;
    }
}
