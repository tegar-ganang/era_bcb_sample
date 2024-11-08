package ch.unibe.id.se.a3ublogin.business;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ch.unibe.a3ubAdmin.exception.ValidationException;
import ch.unibe.a3ubAdmin.model.Application;
import ch.unibe.a3ubAdmin.model.ViewUser;
import ch.unibe.id.se.a3ublogin.beans.A3ubLoginBucketBean_v01;
import ch.unibe.id.se.a3ublogin.business.liquidstorage.LiquidStorage;
import ch.unibe.id.se.a3ublogin.business.security.SecurityEventManager;
import ch.unibe.id.se.a3ublogin.business.utils.ApplAutAttributesUtil;
import ch.unibe.id.se.a3ublogin.business.utils.CustomAttributesUtil;
import ch.unibe.id.se.a3ublogin.exceptions.BusinessException;
import ch.unibe.id.se.a3ublogin.exceptions.LdapException;
import ch.unibe.id.se.a3ublogin.exceptions.LoginException;
import ch.unibe.id.se.a3ublogin.persistence.PersistenceManager;
import ch.unibe.id.se.a3ublogin.utils.JConfigWrapper;
import ch.unibe.id.se.beans.AuthorizationBean_v03;
import ch.unibe.id.se.beans.InternalAuthorizationBean_v03;
import ch.unibe.id.se.beans.LocalAuthorizationBean_v03;

public class BusinessManager {

    private static final String WELCOME_PAGE = "WelcomePage";

    private static final int CAMPUS_ACCOUNT = 0;

    private static final int LOCAL_ACCOUNT = -1;

    private static final int INTERNAL_ACCOUNT = -2;

    private Log log = null;

    private PersistenceManager persMan = null;

    private Constants constants = null;

    private Map<String, Map<String, String>> testaccountMap = null;

    @SuppressWarnings("unused")
    private LiquidStorage storage = null;

    /** instance of the singleton */
    private static BusinessManager instance = null;

    /** returns the singleton */
    public static synchronized BusinessManager getInstance() {
        if (instance == null) instance = new BusinessManager();
        return instance;
    }

    /** privat constructor */
    private BusinessManager() {
        this.log = LogFactory.getLog(getClass());
        if (log.isInfoEnabled()) {
            log.info("log for BusinessManager created: " + getClass());
        }
        this.persMan = PersistenceManager.getInstance();
        this.constants = Constants.getInstance();
        this.storage = LiquidStorage.getInstance();
        this.testaccountMap = this.getAllRegistredTestAccounts();
    }

    /**
	 * does an anymous bind, then a user bind to authenticate user and then
	 * reads all accessible ldapattributes for this user and stores them in the
	 * ticket if everything was ok, then the retured userDN contains the
	 * username
	 * 
	 * @param A3ubLoginBucketBean_v01 -
	 *            wich is almost empty
	 * @return A3ubLoginBucketBean_v01 - containig all the informations about
	 *         the user and its login state
	 * @throws BusinessException
	 * @throws LoginException
	 */
    public A3ubLoginBucketBean_v01 userLogin(A3ubLoginBucketBean_v01 loginBucketBean) throws LdapException, BusinessException {
        String logid = loginBucketBean.getTicketID().substring((loginBucketBean.getTicketID().length() - 6), loginBucketBean.getTicketID().length());
        loginBucketBean.getSecurityEventManager().setTicketPart(logid);
        if (log.isDebugEnabled()) {
            log.debug("start new userlogin ");
        }
        loginBucketBean.getSecurityEventManager().startTimeMeasureBusiness();
        loginBucketBean.setCountAsLoginUpdate(true);
        this.storage.setObject(loginBucketBean.getTicketID(), loginBucketBean);
        loginBucketBean.setCountAsLoginUpdate(false);
        loginBucketBean.setLoginbean(this.getAuthorizationBeanFromLoginData(loginBucketBean.getLoginname(), loginBucketBean.getPassword(), loginBucketBean.getAppID(), loginBucketBean.getSecurityEventManager(), logid));
        loginBucketBean.setPassword("");
        loginBucketBean.setRedirectURL(this.getRedirectURL(loginBucketBean, logid));
        if (loginBucketBean.getRedirectURL() == null || loginBucketBean.getRedirectURL().equals("")) {
            throw new BusinessException("error_08", "error_08");
        }
        loginBucketBean.getSecurityEventManager().startTimeMeasureLiquid();
        loginBucketBean.getLoginbean().setServerLoginTimestamp("" + System.currentTimeMillis());
        loginBucketBean.setCountAsLoginUpdate(true);
        this.storage.setObject(loginBucketBean.getTicketID(), loginBucketBean);
        loginBucketBean.setCountAsLoginUpdate(false);
        if (log.isDebugEnabled()) {
            log.debug(("New Bean into the LiquidStore: " + loginBucketBean + " ticketpart: " + logid));
        }
        if ((loginBucketBean.getLoginbean().getEmailAddress() == null) || loginBucketBean.getLoginbean().getEmailAddress().equals("")) {
            try {
                ch.unibe.id.se.a3ublogin.utils.mail.EmailSender.getInstance().sendMail("setest03@id.unibe.ch", "setest03@id.unibe.ch", "Person ohne E-Mail-Adresse im Ldap gefunden", "Die Person: \n\n" + loginBucketBean + "\n\n hat keine gültige E-Mailadresse registriert");
            } catch (Exception e) {
                if (log.isErrorEnabled()) {
                    log.error("error sending email: ", e);
                }
            }
            ch.unibe.a3ubAdmin.control.DatabaseManager dbm = new ch.unibe.a3ubAdmin.control.DatabaseManager();
            ViewUser user = null;
            try {
                user = dbm.loadViewUser(loginBucketBean.getLoginname());
            } catch (Exception e) {
                if (log.isErrorEnabled()) {
                    log.error(e, e);
                }
            }
            if ((user != null) && (user.getCustommap() != null)) {
                String emailsaved = (String) user.getCustommap().get("emailupdated");
                if (emailsaved == null || !emailsaved.equals("ok")) {
                    loginBucketBean.getLoginbean().setEmailAddress(loginBucketBean.getLoginname() + "@ubecx.unibe.ch");
                    loginBucketBean.setGenericUbcxEmail(true);
                } else {
                    String email = (String) user.getCustommap().get("email");
                    loginBucketBean.getLoginbean().setEmailAddress(email);
                    loginBucketBean.setGenericUbcxEmail(false);
                }
            }
        }
        loginBucketBean = checkAccess(loginBucketBean);
        loginBucketBean.getSecurityEventManager().stopTimeMeasureBusiness();
        return loginBucketBean;
    }

    private String getRedirectURL(A3ubLoginBucketBean_v01 loginBucketBean, String logid) throws BusinessException {
        if (!persMan.isApplicationRegistred(loginBucketBean.getAppID())) {
            throw new BusinessException("error_16", "error_16");
        }
        String locationKey = WELCOME_PAGE;
        if ((loginBucketBean.getLocationID() != null) && !loginBucketBean.getLocationID().equals("")) {
            locationKey = locationKey + loginBucketBean.getLocationID();
        }
        if (log.isInfoEnabled()) {
            log.info("LocationKey: " + locationKey + " ticketpart: " + logid);
        }
        Properties a3ubadminProperties = persMan.getPropertiesForApplicationidentifier("a3ubadmin2");
        String adminUrl = "";
        if (a3ubadminProperties == null) {
            if (log.isWarnEnabled()) {
                log.info("no a3ubadmin2 registred set the adminUrl to http://www.unibe.ch " + locationKey + " ticketpart: " + logid);
            }
            adminUrl = "http://www.unibe.ch";
        }
        Properties applicationProperties = persMan.getPropertiesForApplicationidentifier(loginBucketBean.getAppID());
        String defaultUrl = applicationProperties.getProperty(WELCOME_PAGE);
        String locationUrl = applicationProperties.getProperty(locationKey);
        if (a3ubadminProperties != null) {
            adminUrl = a3ubadminProperties.getProperty(WELCOME_PAGE);
        }
        String tempurl = (String) persMan.getPropertiesForApplicationidentifier(loginBucketBean.getAppID()).get(locationKey);
        if ("".equals(tempurl) || tempurl == null) {
            locationKey = WELCOME_PAGE;
        }
        String resultingURL = "";
        Map<String, List<String>> customAttributesMap = loginBucketBean.getLoginbean().getCustomAttributesMap();
        boolean loginEnabled = false;
        if ((loginBucketBean.getLoginbean() != null) && (customAttributesMap != null)) {
            List<String> localEnabledList = customAttributesMap.get(Constants.getInstance().getLocalaccountLoginEnabledKey());
            if (log.isDebugEnabled()) {
                log.debug("List with true in if it is a login enabled account: " + localEnabledList + " ticketpart: " + logid);
            }
            loginEnabled = ((localEnabledList != null) && (localEnabledList.size() > 0) && (localEnabledList.get(0) != null) && (localEnabledList.get(0).equals("true")));
        }
        if (log.isDebugEnabled()) {
            log.debug("The resulting URL: " + resultingURL + " ticketpart: " + logid);
        }
        if (log.isDebugEnabled()) {
            log.debug("The CustomAttributesMap: " + customAttributesMap + " ticketpart: " + logid);
        }
        List<String> accountFlagList = customAttributesMap.get(Constants.getInstance().getLocalAccountFlagKey());
        boolean isLocal = (accountFlagList != null) && (accountFlagList.size() > 0) && (accountFlagList.get(0) != null) && (accountFlagList.get(0).equals("true"));
        List<String> gidNumberList = customAttributesMap.get(Constants.getInstance().getInternalAccountFlagKey());
        boolean isInternal = (gidNumberList != null) && (gidNumberList.size() > 0) && (gidNumberList.get(0) != null) && (gidNumberList.get(0).equals("true"));
        if (log.isDebugEnabled()) {
            log.debug("islocal: " + isLocal + " isloginenabled: " + loginEnabled + " isinternal: " + isInternal + " ticketpart: " + logid);
        }
        if ((isLocal || isInternal) && (!loginEnabled || loginBucketBean.isSelfadmin())) {
            log.info("we have a not actualized local/internal account or it is a selfadminrequest. will redirect to a3ubaddmin2 for selfadministration" + " ticketpart: " + logid);
            loginBucketBean.getLoginbean().getUnibeApplAuthorizationMap().clear();
            resultingURL = adminUrl;
        } else {
            resultingURL = locationUrl;
            if (resultingURL == null || resultingURL.equals("")) {
                resultingURL = defaultUrl;
                if (resultingURL == null || resultingURL.equals("")) {
                    throw new BusinessException("error_23", "error_23");
                }
            }
        }
        if (log.isInfoEnabled()) {
            log.info("locationKey is: " + resultingURL + " and: locationID is: " + loginBucketBean.getLocationID() + " ticketpart: " + logid);
        }
        return resultingURL;
    }

    private AuthorizationBean_v03 getAuthorizationBeanFromLoginData(String loginName, String password, String appID, SecurityEventManager securityEventManager, String logid) throws LdapException, BusinessException {
        AuthorizationBean_v03 loginBean = new AuthorizationBean_v03();
        if (log.isDebugEnabled()) {
            log.debug("loginname is: " + loginName + " appid is: " + appID + " ticketpart: " + logid);
        }
        int type = BusinessManager.CAMPUS_ACCOUNT;
        AuthorizationBean_v03 unidentifiedBean = ch.unibe.a3ubAdmin.business.BusinessManager.getInstance().getLocalAccount(appID, loginName);
        if (unidentifiedBean == null) {
            unidentifiedBean = ch.unibe.a3ubAdmin.business.BusinessManager.getInstance().getInternalAccount(loginName);
            if (unidentifiedBean != null) {
                type = BusinessManager.INTERNAL_ACCOUNT;
            }
        } else {
            type = BusinessManager.LOCAL_ACCOUNT;
        }
        ApplAutAttributesUtil autUtil = ApplAutAttributesUtil.getInstance();
        Map<String, Map<String, List<String>>> authorizationMap;
        switch(type) {
            case BusinessManager.CAMPUS_ACCOUNT:
                if (log.isInfoEnabled()) {
                    log.info("before autentication... ticketpart: " + logid);
                }
                securityEventManager.startTimeMeasureLdap();
                HashMap mapFromIdAut = (HashMap) persMan.loginOnIdAut(loginName, password);
                if (log.isDebugEnabled()) {
                    log.debug("after autentication... hashmap from idaut: " + mapFromIdAut.toString().substring(0, 20) + " ticketpart: " + logid);
                }
                securityEventManager.stopTimeMeasureLdap();
                mapFromIdAut = this.isAccountLockedInLdap(mapFromIdAut);
                if (log.isDebugEnabled()) {
                    log.debug("set the server timestamp " + " ticketpart: " + logid);
                }
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                String date = sdf.format(new Date());
                loginBean.setServerLoginTimestamp(date);
                loginBean.setValid(true);
                String[] temp = (String[]) mapFromIdAut.remove(constants.getCn());
                if (temp != null && temp.length == 1) {
                    loginBean.setCommonName(temp[0]);
                }
                temp = (String[]) mapFromIdAut.remove(constants.getGidNumber());
                if (temp != null && temp.length == 1) {
                    loginBean.setUserGroupIdentifierNumber((temp[0]));
                }
                temp = (String[]) mapFromIdAut.remove(constants.getGivenName());
                if (temp != null && temp.length == 1) {
                    loginBean.setGivenName((temp[0]));
                }
                temp = (String[]) mapFromIdAut.remove(constants.getMail());
                if (temp != null && temp.length == 1) {
                    loginBean.setEmailAddress((temp[0]));
                }
                temp = (String[]) mapFromIdAut.remove(constants.getSn());
                if (temp != null && temp.length == 1) {
                    loginBean.setSurName((temp[0]));
                }
                temp = (String[]) mapFromIdAut.remove(constants.getUid());
                if (temp != null && temp.length == 1) {
                    loginBean.setUserIdentifier((temp[0]));
                }
                temp = (String[]) mapFromIdAut.remove(constants.getUidNumber());
                if (temp != null && temp.length == 1) {
                    loginBean.setUserIdentifierNumber((temp[0]));
                }
                String tempSt = appID;
                if (tempSt != null && !tempSt.equals("")) {
                    loginBean.setApplicationIdentifier(tempSt);
                }
                if (log.isDebugEnabled()) {
                    log.debug("after transfer of ldap attributes " + " ticketpart: " + logid);
                }
                securityEventManager.startTimeMeasureLocalAuth();
                authorizationMap = (Map<String, Map<String, List<String>>>) autUtil.getAuthorizationMapFromSeserver01(loginBean.getUserIdentifierNumber(), appID);
                loginBean.setUnibeApplAuthorizationMap(authorizationMap);
                securityEventManager.stopTimeMeasureLocalAuth();
                String[] custArr = this.getCustomAttributesListFromProperties(appID);
                CustomAttributesUtil util = CustomAttributesUtil.getInstance();
                Map customMap = util.transferCustomAttributes(custArr, mapFromIdAut);
                loginBean.setCustomAttributesMap(customMap);
                if (log.isDebugEnabled()) {
                    log.debug("after setting the custom attribute map and the appl-aut-map " + " ticketpart: " + logid);
                }
                if (log.isDebugEnabled()) {
                    log.debug("set the bean into the bucket " + " ticketpart: " + logid);
                }
                break;
            case BusinessManager.LOCAL_ACCOUNT:
                if (log.isDebugEnabled()) {
                    log.debug("the account is a local account: " + " ticketpart: " + logid);
                }
                try {
                    loginBean = persMan.localLogin((LocalAuthorizationBean_v03) unidentifiedBean, password);
                    securityEventManager.stopTimeMeasureLocalAuth();
                    authorizationMap = (Map<String, Map<String, List<String>>>) autUtil.getAuthorizationMapFromSeserver01(loginBean.getUserIdentifierNumber(), appID);
                    loginBean.setUnibeApplAuthorizationMap(authorizationMap);
                } catch (Exception e) {
                    if (log.isInfoEnabled()) {
                        log.info("error login to local account: " + e.getMessage() + " ticketpart: " + logid);
                    }
                    throw new BusinessException("error_02", "error_02");
                }
                if (loginBean == null) {
                    throw new BusinessException("error_02", "error_02");
                }
                break;
            case BusinessManager.INTERNAL_ACCOUNT:
                if (log.isDebugEnabled()) {
                    log.debug("the account is a internal account: " + " ticketpart: " + logid);
                }
                try {
                    loginBean = persMan.internalLogin((InternalAuthorizationBean_v03) unidentifiedBean, password);
                    loginBean.setApplicationIdentifier(appID);
                    authorizationMap = (Map<String, Map<String, List<String>>>) autUtil.getAuthorizationMapFromSeserver01(loginBean.getUserIdentifierNumber(), appID);
                    loginBean.setUnibeApplAuthorizationMap(authorizationMap);
                } catch (Exception e) {
                    if (log.isInfoEnabled()) {
                        log.info("error login to internal account: " + e.getMessage() + " ticketpart: " + logid);
                    }
                    throw new BusinessException("error_02", "error_02");
                }
                if (loginBean == null) {
                    throw new BusinessException("error_02", "error_02");
                }
                break;
            default:
                throw new BusinessException();
        }
        return loginBean;
    }

    public Application getApp(String appid) {
        ch.unibe.a3ubAdmin.control.ApplRightManager rman = new ch.unibe.a3ubAdmin.control.ApplRightManager();
        Application app = null;
        try {
            app = rman.loadApplication(appid);
        } catch (ValidationException e) {
            if (log.isErrorEnabled()) {
                log.error(e.getMessage(), e);
            }
        }
        return app;
    }

    public A3ubLoginBucketBean_v01 checkAccess(A3ubLoginBucketBean_v01 bean) {
        return AccessManager.getInstance().checkAccess(bean);
    }

    /**
	 * New method to take out the treatment of the redirect-URL out of the userLogin Method
	 * 
	 * TODO -> methode ganz auslagern
	 * 
	 * @param bean
	 * @param logid
	 */
    public A3ubLoginBucketBean_v01 setRedirecturl(A3ubLoginBucketBean_v01 bean) {
        String logid = bean.getTicketID().substring((bean.getTicketID().length() - 6), bean.getTicketID().length());
        String locationKey = WELCOME_PAGE;
        if (log.isInfoEnabled()) {
            log.info("locationKey is: " + locationKey + " and: locationID is: " + bean.getLocationID() + " ticketpart: " + logid);
        }
        if (bean.getLocationID() != null && !bean.getLocationID().equals("")) {
            locationKey = locationKey + bean.getLocationID();
            if (log.isInfoEnabled()) {
                log.info("the new locationKey now is: " + locationKey + " ticketpart: " + logid);
            }
        }
        Properties tempP = persMan.getPropertiesForApplicationidentifier(bean.getAppID());
        Properties tempAdmin = persMan.getPropertiesForApplicationidentifier("a3ubadmin2");
        String adminUrl = "";
        if (tempAdmin == null) {
            if (log.isWarnEnabled()) {
                log.info("no a3ubadmin2 registred set the adminUrl to http://www.unibe.ch " + locationKey + " ticketpart: " + logid);
            }
            adminUrl = "http://www.unibe.ch";
        }
        String defaultUrl = tempP.getProperty(WELCOME_PAGE);
        String locationUrl = tempP.getProperty(locationKey);
        if (tempAdmin != null) {
            adminUrl = tempAdmin.getProperty(WELCOME_PAGE);
        }
        String tempurl = (String) persMan.getPropertiesForApplicationidentifier(bean.getAppID()).get(locationKey);
        if ("".equals(tempurl) || tempurl == null) {
            locationKey = WELCOME_PAGE;
        }
        bean.setRedirectURL(tempurl);
        return bean;
    }

    /**
	 * Set the right "zugang" from a person and app to "anfrage" and sends an email to the administrator.
	 * 
	 * @throws BusinessException
	 */
    public void setAccessRequest(A3ubLoginBucketBean_v01 bean) {
        AccessManager.getInstance().setRequest(bean);
    }

    /**
	 * this method checks if the ldap entry is valid or not in an idaut point of
	 * view!!!! ldapattribute ShadowExpire (day since 1.1.1970) > now or
	 * ShadowExpire <0 and ldapAttribute ShadowFlag > 0 means account is
	 * activated and initial password is changed by the user
	 * 
	 * @throws BusinessException
	 */
    private HashMap isAccountLockedInLdap(HashMap mapFromIdAut) throws BusinessException {
        String shadowExpire = "";
        String[] tempS = (String[]) mapFromIdAut.remove(constants.getShadowExpire());
        if (tempS != null && tempS.length == 1) {
            shadowExpire = tempS[0];
        }
        String shadowFlag = "";
        tempS = (String[]) mapFromIdAut.remove(constants.getShadowFlag());
        if (tempS != null && tempS.length == 1) {
            shadowFlag = tempS[0];
        }
        String shadowMin = "";
        tempS = (String[]) mapFromIdAut.remove(constants.getShadowMin());
        if (tempS != null && tempS.length == 1) {
            shadowMin = tempS[0];
        }
        String shadowInactive = "";
        tempS = (String[]) mapFromIdAut.remove(constants.getShadowInactive());
        if (tempS != null && tempS.length == 1) {
            shadowInactive = tempS[0];
        }
        String shadowWarning = "";
        tempS = (String[]) mapFromIdAut.remove(constants.getShadowWarning());
        if (tempS != null && tempS.length == 1) {
        }
        String shadowMax = "";
        tempS = (String[]) mapFromIdAut.remove(constants.getShadowMax());
        if (tempS != null && tempS.length == 1) {
            shadowMax = tempS[0];
        }
        if (shadowFlag != null && shadowFlag.equals("1")) {
            return mapFromIdAut;
        }
        if (shadowFlag != null && shadowFlag.equals("0")) {
            throw new BusinessException("error_05", "error_05");
        }
        if (shadowFlag != null && shadowFlag.equals("2")) {
            throw new BusinessException("error_04", "error_04");
        }
        return mapFromIdAut;
    }

    /**
	 * Generates a unique ticketIdentifier and adds it to the
	 * A3ubLoginBucketBean_v01
	 * 
	 * @return A3ubLoginBucketBean_v01 - with the added ticketIdentifier
	 */
    public A3ubLoginBucketBean_v01 addTicketIdentifier(A3ubLoginBucketBean_v01 bean) {
        Random generator = new Random(System.currentTimeMillis());
        bean.setTicketID("" + Math.abs(generator.nextLong()) + Math.abs(generator.nextLong()) + Math.abs(generator.nextLong()));
        return bean;
    }

    /**
	 * Check if the application with the given identifier is registred or not
	 * 
	 * @return boolean - false if the application is not registred jet, true if
	 *         it is there
	 */
    public boolean isApplicationRegistred(String applicationIdentifier) {
        return PersistenceManager.getInstance().isApplicationRegistred(applicationIdentifier);
    }

    /**
	 * Returns the Properties to an given applicationIdentifier
	 * 
	 * @return Properties - corresponding to the given applicationIdentifier
	 */
    public Properties getPropertiesForApplicationidentifier(String applicationIdentifier) {
        return PersistenceManager.getInstance().getPropertiesForApplicationidentifier(applicationIdentifier);
    }

    /**
	 * List out Map with the Properties of all the registred applications
	 * 
	 * @return Map - keys are the applicationsIdentifiers an the values are the
	 *         Properties
	 */
    public Map getMapOfAllProperties() {
        return PersistenceManager.getInstance().getMapOfAllProperties();
    }

    /**
	 * Returns all the custom attributes for an application
	 * 
	 * @return String[] - with the custom Attributes
	 * @throws BusinessException
	 */
    public String[] getCustomAttributesListFromProperties(String applicationIdentifier) throws BusinessException {
        if (!this.isApplicationRegistred(applicationIdentifier)) {
            throw new BusinessException("error_06", "error_06");
        }
        Properties prop = this.getPropertiesForApplicationidentifier(applicationIdentifier);
        String temp = (String) prop.get(this.constants.getCustomAttributesKey());
        if (temp != null && !temp.equals("")) {
            String[] arr = temp.split(",");
            return arr;
        }
        return new String[0];
    }

    /**
	 * returns a Map with the registred (in appConfig.xml) testaccounts
	 * 
	 * @return Map<String,Map<String,String>
	 */
    public Map<String, Map<String, String>> getAllRegistredTestAccounts() {
        if (this.testaccountMap == null) {
            JConfigWrapper jConfig = null;
            Map<String, Map<String, String>> returnMap = new HashMap<String, Map<String, String>>();
            try {
                jConfig = JConfigWrapper.getInstance();
                String numbers = jConfig.getProperty("numbers", "testaccounts");
                if (numbers == null) {
                    this.testaccountMap = new HashMap<String, Map<String, String>>();
                    numbers = "";
                }
                String[] numberArray = numbers.split(",");
                for (int i = 0; i < numberArray.length; i++) {
                    String nu = numberArray[i];
                    String login = jConfig.getProperty("login", "testaccount" + nu);
                    String password = jConfig.getProperty("password", "testaccount" + nu);
                    String appid = jConfig.getProperty("appid", "testaccount" + nu);
                    HashMap<String, String> temp = new HashMap<String, String>();
                    temp.put("login", login);
                    temp.put("password", password);
                    temp.put("appid", appid);
                    returnMap.put(login, temp);
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (log.isErrorEnabled()) {
                    log.error("error getting Jconfig: " + e.getMessage());
                }
            }
            this.testaccountMap = returnMap;
        }
        return this.testaccountMap;
    }

    /**
	 * returns the A3ubLoginBucketBean_v01 from the LiquidStorage
	 * 
	 * @return A3ubLoginBucketBean_v01 - with the custom Attributes
	 * @param String -
	 *            identifier
	 * @return A3ubLoginBucketBean_v01 - with the custom Attributes
	 */
    public A3ubLoginBucketBean_v01 getObjectfromLiquidStorage(String identifier) {
        LiquidStorage storage = LiquidStorage.getInstance();
        return (A3ubLoginBucketBean_v01) storage.getObject(identifier);
    }

    /**
	 * Get an Email Proposal for a CN out of the Unitel LDAP
	 * 
	 * @param cn
	 * @return
	 */
    public String getEmailProposal(String cn) {
        String result = "";
        result = ch.unibe.id.se.a3ublogin.persistence.readersandwriters.UniLdapReader.getInstance().getEmail(cn);
        if (result == null) {
            result = "";
        }
        return result;
    }

    public A3ubLoginBucketBean_v01 saveEmailForPerson(String email, A3ubLoginBucketBean_v01 bean) throws Exception {
        if (!org.apache.commons.validator.EmailValidator.getInstance().isValid(email)) {
            throw new Exception("email invalide");
        }
        bean.getLoginbean().setEmailAddress(email);
        bean.setNewEmailFromUser(true);
        return bean;
    }
}
