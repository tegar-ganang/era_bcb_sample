package ch.unibe.id.se.a3ublogin.persistence;

import java.util.Map;
import java.util.Properties;
import javax.naming.NamingException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ch.unibe.a3ubAdmin.control.DatabaseManager;
import ch.unibe.a3ubAdmin.model.Group;
import ch.unibe.a3ubAdmin.model.ViewUser;
import ch.unibe.id.se.a3ublogin.exceptions.LdapException;
import ch.unibe.id.se.a3ublogin.exceptions.PersistenceException;
import ch.unibe.id.se.a3ublogin.persistence.readersandwriters.AutLdapReader;
import ch.unibe.id.se.a3ublogin.persistence.serializeddbtables.SerializedTablesFactory;
import ch.unibe.id.se.a3ublogin.persistence.serializeddbtables.core.IPropertiesStorage;
import ch.unibe.id.se.beans.AuthorizationBean_v03;
import ch.unibe.id.se.beans.InternalAuthorizationBean_v03;
import ch.unibe.id.se.beans.LocalAuthorizationBean_v03;

public class PersistenceManager {

    private Log log = LogFactory.getLog(getClass());

    private AutLdapReader idAutR = null;

    /** instance of the singleton */
    private static PersistenceManager instance = null;

    /** returns the singleton */
    public static synchronized PersistenceManager getInstance() {
        if (instance == null) instance = new PersistenceManager();
        return instance;
    }

    /** privat constructor */
    private PersistenceManager() {
        idAutR = AutLdapReader.getInstance();
    }

    /**
	 * @param login -
	 *            String
	 * @return boolean - true if we find user, false if we don't find the user
	 * @throws LdapException
	 * @throws NamingException -
	 *             if we have a problem with the idaut
	 */
    public boolean isLoginNameInIdAut(String login) throws LdapException {
        return idAutR.isUserNameInContainer(login);
    }

    /**
	 * @param login -
	 *            String
	 * @return boolean - true if we find user, false if we don't find the user
	 */
    public boolean isLoginNameInLocalAccounts(String login) {
        return false;
    }

    /**
	 * @param loginName -
	 *            String with the loginname of the person
	 * @param password -
	 *            String with the password of the person
	 * @return Map with String as key an String[] as values
	 * 
	 * @throws LdapException
	 */
    public Map loginOnIdAut(String loginName, String password) throws LdapException {
        return idAutR.getPersonsValues(loginName, password);
    }

    /**
	 * Check if the application with the given identifier is registred or not
	 * 8.11.2006 adaption to .properties - storage into the
	 * SerializedTableFramework
	 * 
	 * @return boolean - false if the application is not registred jet, true if
	 *         it is there
	 */
    public boolean isApplicationRegistred(String applicationIdentifier) {
        IPropertiesStorage propertiesS = SerializedTablesFactory.getInstance().getIPropertiesStorage();
        return propertiesS.isApplicationRegistred(applicationIdentifier);
    }

    /**
	 * Returns the Properties to an given applicationIdentifier 8.11.2006
	 * adaption to .properties - storage into the SerializedTableFramework
	 * 
	 * @return Properties - corresponding to the given applicationIdentifier
	 */
    public Properties getPropertiesForApplicationidentifier(String applicationIdentifier) {
        IPropertiesStorage propertiesS = SerializedTablesFactory.getInstance().getIPropertiesStorage();
        return propertiesS.readProperties(applicationIdentifier);
    }

    /**
	 * List out Map with the Properties of all the registred applications
	 * 8.11.2006 adaption to .properties - storage into the
	 * SerializedTableFramework
	 * 
	 * @return Map - keys are the applicationsIdentifiers an the values are the
	 *         Properties
	 */
    public Map getMapOfAllProperties() {
        IPropertiesStorage propertiesS = SerializedTablesFactory.getInstance().getIPropertiesStorage();
        return propertiesS.readPropertieMap();
    }

    /**
	 * List out Map with the Properties of all the registred applications
	 * 
	 * @return String - with the emailaddress of the person
	 */
    public String getEmailFromCnOutOfLdap(String cn) {
        return ch.unibe.id.se.a3ublogin.persistence.readersandwriters.UniLdapReader.getInstance().getEmail(cn);
    }

    /**
	 * List out Map with the Properties of all the registred applications
	 * 
	 * @return String - with the emailaddress of the person
	 * @throws PersistenceException
	 */
    public AuthorizationBean_v03 localLogin(LocalAuthorizationBean_v03 bean, String password) throws PersistenceException {
        AuthorizationBean_v03 beanR = null;
        try {
            beanR = ch.unibe.id.se.a3ublogin.persistence.serializeddbtables.core.LocalPersistenceManager.getInstance().doLocalLogin(bean, password);
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("error getting local-accounts: " + e);
            }
            throw new PersistenceException("error_24");
        }
        if (beanR == null) {
            if (log.isInfoEnabled()) {
                log.info("login with local account failed: loginName; " + beanR.getUserIdentifier() + " appid: " + beanR.getApplicationIdentifier());
            }
            return null;
        }
        if (log.isInfoEnabled()) {
            log.info("login with local account ok; " + beanR.getUserIdentifier() + " appid: " + beanR.getApplicationIdentifier());
        }
        return bean;
    }

    public AuthorizationBean_v03 internalLogin(InternalAuthorizationBean_v03 bean, String password) throws PersistenceException {
        AuthorizationBean_v03 beanR = null;
        try {
            beanR = ch.unibe.id.se.a3ublogin.persistence.serializeddbtables.core.LocalPersistenceManager.getInstance().doInternalLogin(bean, password);
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("error getting internal-accounts: " + e);
            }
            throw new PersistenceException("error_24");
        }
        if (beanR == null) {
            if (log.isInfoEnabled()) {
                log.info("login with internal account failed: loginName; " + beanR.getUserIdentifier() + " appid: " + beanR.getApplicationIdentifier());
            }
            return null;
        }
        if (log.isInfoEnabled()) {
            log.info("login with internal account ok; " + beanR.getUserIdentifier() + " appid: " + beanR.getApplicationIdentifier());
        }
        return bean;
    }

    /**
	 * Load a LocalAuthorizationBean_v03 null if not there
	 * 
	 */
    public LocalAuthorizationBean_v03 getLocalLocalAccountBean(String applicationIdentifier, String loginName) throws PersistenceException {
        LocalAuthorizationBean_v03 bean = null;
        bean = ch.unibe.a3ubAdmin.business.BusinessManager.getInstance().getLocalAccount(applicationIdentifier, loginName);
        return bean;
    }

    /**
	 * Load a ViewUser from the old Hibernate Layer
	 * 
	 * @return String - with the emailaddress of the person
	 */
    public ViewUser getHibernateViewUser(int uidNumber) {
        DatabaseManager man = new DatabaseManager();
        ViewUser user = null;
        try {
            user = man.loadViewUser(uidNumber);
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error(e);
            }
        }
        return user;
    }

    /**
	 * Save a ViewUser into the old Hibernate Layer
	 * 
	 * @return String - with the emailaddress of the person
	 */
    public void saveHibernateViewUser(ViewUser user) {
        DatabaseManager man = new DatabaseManager();
        try {
            man.saveUser(user);
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error(e);
            }
        }
    }

    /**
	 * Load a Group from the old Hibernate Layer
	 * 
	 * @return String - with the emailaddress of the person
	 */
    public Group getHibernateGroup(int gIdNumber) {
        DatabaseManager man = new DatabaseManager();
        Group group = null;
        try {
            group = man.loadGroup(gIdNumber);
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error(e);
            }
        }
        return group;
    }

    /**
	 * Save a Group into the old Hibernate Layer
	 * 
	 * @return String - with the emailaddress of the person
	 */
    public void saveHibernateGroup(Group group) {
        DatabaseManager man = new DatabaseManager();
        try {
            man.saveGroup(group);
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error(e);
            }
        }
    }

    /**
	 * Delete a User into the old Hibernate Layer
	 * 
	 */
    public void deleteHibernateUser(ViewUser user) {
        DatabaseManager man = new DatabaseManager();
        try {
            man.deleteUser(user);
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error(e);
            }
        }
    }

    public static void main(String[] args) {
        PersistenceManager p = new PersistenceManager();
        System.out.println(p.getMapOfAllProperties());
    }
}
