package au.edu.monash.merc.capture.struts2.action.install;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import au.edu.monash.merc.capture.dto.ApplicationProperty;
import au.edu.monash.merc.capture.dto.JdbcProperty;
import au.edu.monash.merc.capture.dto.LdapProperty;
import au.edu.monash.merc.capture.dto.MailProperty;
import au.edu.monash.merc.capture.util.CaptureUtil;
import au.edu.monash.merc.capture.util.Installer;

@Scope("prototype")
@Controller("install.installAction")
public class InstallAction extends InstallBaseAction {

    private String webinfRoot;

    private String installTempConfPath;

    private String destPropConfRoot;

    private boolean accepted;

    private ApplicationProperty appProp;

    private JdbcProperty jdbcProp;

    private MailProperty mailProp;

    private LdapProperty ldapProp;

    private Map<String, String> dbTypeNames = new HashMap<String, String>();

    private Map<String, String> trueFalseMap = new HashMap<String, String>();

    private static String JDBC_PROP_FILE = "jdbc.properties";

    private static String MAIL_PROP_FILE = "mail.properties";

    private static String APP_PROP_FILE = "dataCapture.properties";

    private static String LDAP_PROP_FILE = "ldap.properties";

    private static String WEB_XML_FILE = "web.xml";

    private static String SPRING_CONF_FILE = "applicationContext.xml";

    private static String STRUTS_FILE = "struts.xml";

    private Logger logger = Logger.getLogger(this.getClass());

    public String setup() {
        defaultConf();
        setDefaultMaps();
        return SUCCESS;
    }

    private void setDefaultMaps() {
        dbTypeNames.put("mysql", "MySQL");
        dbTypeNames.put("oracle", "Oracle");
        dbTypeNames.put("postgresql", "PostgreSQL");
        trueFalseMap.put("true", "true");
        trueFalseMap.put("false", "false");
    }

    public String acceptCon() {
        if (!accepted) {
            return INPUT;
        }
        return SUCCESS;
    }

    public String install() {
        try {
            webinfRoot = getAppRoot() + "WEB-INF" + File.separator;
            installTempConfPath = webinfRoot + "install" + File.separator + "conf" + File.separator;
            destPropConfRoot = webinfRoot + "classes" + File.separator;
            String appTempFile = installTempConfPath + APP_PROP_FILE;
            String destAppFile = destPropConfRoot + APP_PROP_FILE;
            appProp.setAuthDomain(getServerQName());
            Installer.writeAppConfig(appProp, appTempFile, destAppFile);
            String jdbcTempFile = installTempConfPath + JDBC_PROP_FILE;
            String destJdbcFile = destPropConfRoot + JDBC_PROP_FILE;
            Installer.writeDbConfig(jdbcTempFile, jdbcProp.getDbType(), jdbcProp.getDbHost(), jdbcProp.getDbPort(), jdbcProp.getDbName(), jdbcProp.getDbUserName(), jdbcProp.getDbPassword(), destJdbcFile);
            String mailTempFile = installTempConfPath + MAIL_PROP_FILE;
            String destMailFile = destPropConfRoot + MAIL_PROP_FILE;
            Installer.writeMailConfig(mailTempFile, mailProp.getMailServer(), mailProp.getMailServerPort(), mailProp.isAuthenticated(), mailProp.isTlsEnabled(), mailProp.getUserName(), mailProp.getPassword(), destMailFile);
            String ldapTempFile = installTempConfPath + LDAP_PROP_FILE;
            String destLdapFile = destPropConfRoot + LDAP_PROP_FILE;
            Installer.writeLdapConfig(ldapProp, ldapTempFile, destLdapFile);
            String springConfFile = installTempConfPath + SPRING_CONF_FILE;
            String destSpringConfFile = destPropConfRoot + SPRING_CONF_FILE;
            String strutsFile = installTempConfPath + STRUTS_FILE;
            String destStrutsFile = destPropConfRoot + STRUTS_FILE;
            String webxmlConfFile = installTempConfPath + WEB_XML_FILE;
            String destWebxmlFile = webinfRoot + WEB_XML_FILE;
            FileUtils.copyFile(new File(springConfFile), new File(destSpringConfFile));
            FileUtils.copyFile(new File(strutsFile), new File(destStrutsFile));
            FileUtils.copyFile(new File(webxmlConfFile), new File(destWebxmlFile));
            System.out.println("Finished");
        } catch (Exception e) {
            addActionError(e.getMessage());
            setDefaultMaps();
            logger.error(e);
            return INPUT;
        }
        return SUCCESS;
    }

    private void defaultConf() {
        appProp = new ApplicationProperty();
        appProp.setAppName("YourApplicationName");
        appProp.setAdminEmail("admin@adminemail.com");
        appProp.setAdminName("admin");
        appProp.setAdminPassword("pass2word!");
        appProp.setSystemServiceEmail("service@servicemail.com");
        appProp.setStoreLocation("/opt/datastore/ands");
        appProp.setCollectionPhysicalLocation("Monash University Clayton Campus Building 26 Clayton 3800 Victoria");
        appProp.setLoginTryTimes(3);
        appProp.setBlockWaitingTimes(15);
        appProp.setSecurityHashSeq("whateveryouwanttomakeitmoresecuritymerc!");
        appProp.setGoogleApiKey("ABQIAAAA-mrDIEKQPrjqNppfCE72fRQtlyttTPx5mPekxQelw9V6C-nC5RQ8Sya-FroqqvlqOHnhCAtW38qDpg");
        appProp.setStageEnabled(false);
        appProp.setStageLocation("/opt/ands_staging");
        appProp.setMdRegEnabled(true);
        appProp.setRifcsStoreLocation("/opt/ands_rifcs");
        appProp.setAndsRegGroupName("Monash University");
        appProp.setResearchFieldCode("960501");
        appProp.setRmWsName("AIRMANDSService");
        appProp.setRmWsEndpointAddress("http://mobs.its.monash.edu.au:7778/orabpel/ResearchMaster/AIRMANDSService/1.0");
        appProp.setRmWsTimeout(60000);
        appProp.setHdlWsEnabled(true);
        appProp.setHdlWsHostName("https://test.ands.org.au");
        appProp.setHdlWsHostPort(8443);
        appProp.setHdlWsPath("pids");
        appProp.setHdlWsMethod("mint");
        appProp.setHdlWsAppId("c4b16dc56797f1dfbf545e2397ac7b6bcc54b8ec");
        appProp.setHdlResolverAddress("http://hdl.handle.net/");
        appProp.setCcLicenseWsAddress("http://api.creativecommons.org/rest/1.5/license/standard");
        jdbcProp = new JdbcProperty();
        jdbcProp.setDbType("postgresql");
        jdbcProp.setDbHost("localhost");
        jdbcProp.setDbPort(5432);
        jdbcProp.setDbName("ands_db");
        jdbcProp.setDbUserName("mercdev");
        jdbcProp.setDbPassword("merc2dev");
        mailProp = new MailProperty();
        mailProp.setMailServer("smtp.monash.edu.au");
        mailProp.setMailServerPort(25);
        mailProp.setAuthenticated(false);
        mailProp.setTlsEnabled(false);
        mailProp.setUserName("mailUser");
        mailProp.setPassword("mailUserPassword");
        ldapProp = new LdapProperty();
        ldapProp.setLdapSupported(true);
        ldapProp.setLdapServer("directory.monash.edu.au");
        ldapProp.setBaseDN("o=Monash University, c=AU");
        ldapProp.setAttUID("uid");
        ldapProp.setAttMail("mail");
        ldapProp.setAttGender("gender");
        ldapProp.setAttCN("cn");
        ldapProp.setAttSn("sn");
        ldapProp.setAttPersonalTitle("personaltitle");
        ldapProp.setAttGivenname("givenname");
    }

    public void validateInstall() {
        boolean hasError = false;
        if (StringUtils.isBlank(appProp.getAppName())) {
            addFieldError("appName", "The application name must be provided");
            hasError = true;
        }
        if (StringUtils.isBlank(appProp.getCollectionPhysicalLocation())) {
            addFieldError("physicalAddress", "The data physical location must be provided");
            hasError = true;
        }
        if (StringUtils.isBlank(appProp.getAdminName())) {
            addFieldError("adminName", "The system admin name must be provided");
            hasError = true;
        }
        if (StringUtils.isBlank(appProp.getAdminEmail())) {
            addFieldError("adminEmail", "The system admin email must be provided");
            hasError = true;
        }
        if (StringUtils.isNotBlank(appProp.getAdminEmail()) && (!CaptureUtil.validateEmail(appProp.getAdminEmail()))) {
            addFieldError("adminEmailInvalid", "The system admin email is invalid");
            hasError = true;
        }
        if (StringUtils.isBlank(appProp.getAdminPassword())) {
            addFieldError("adminPasswd", "The system admin password must be provided");
            hasError = true;
        }
        if (StringUtils.isBlank(appProp.getSystemServiceEmail())) {
            addFieldError("systemserviceEmail", "The system service email must be provided");
            hasError = true;
        }
        if (StringUtils.isNotBlank(appProp.getSystemServiceEmail()) && (!CaptureUtil.validateEmail(appProp.getSystemServiceEmail()))) {
            addFieldError("sysEmailInvalid", "The system service email is invalid");
            hasError = true;
        }
        if (appProp.getLoginTryTimes() == 0) {
            addFieldError("logintry", "Login try times must be provided");
            hasError = true;
        }
        if (appProp.getBlockWaitingTimes() == 0) {
            addFieldError("blockwaittimes", "Login re-try waiting times must be provided");
            hasError = true;
        }
        if (StringUtils.isBlank(appProp.getSecurityHashSeq())) {
            addFieldError("securityHash", "The security hash sequence must be provided");
            hasError = true;
        }
        if (StringUtils.isBlank(appProp.getGoogleApiKey())) {
            addFieldError("googlemapkey", "The Google Map API Key must be provided");
            hasError = true;
        }
        boolean stageEnabled = appProp.isStageEnabled();
        if (stageEnabled) {
            if (StringUtils.isBlank(appProp.getStageLocation())) {
                addFieldError("stagelocation", "The staging location must be provided");
                hasError = true;
            }
        }
        boolean publishEnabled = appProp.isMdRegEnabled();
        if (publishEnabled) {
            if (StringUtils.isBlank(appProp.getRifcsStoreLocation())) {
                addFieldError("rifcslocation", "The rif-cs store location must be provided");
                hasError = true;
            }
            if (StringUtils.isBlank(appProp.getAndsRegGroupName())) {
                addFieldError("groupname", "The group name in the rif-cs must be provided");
                hasError = true;
            }
            if (StringUtils.isBlank(appProp.getResearchFieldCode())) {
                addFieldError("anzsrcode", "The research field code must be provided");
                hasError = true;
            }
            if (StringUtils.isBlank(appProp.getRmWsName())) {
                addFieldError("rmwsname", "The researcher master web service name must be provided");
                hasError = true;
            }
            if (StringUtils.isBlank(appProp.getRmWsEndpointAddress())) {
                addFieldError("rmwsaddress", "The researcher master web service endpoint address must be provided");
                hasError = true;
            }
            if (appProp.getRmWsTimeout() == 0) {
                addFieldError("rmwsatimeout", "The researcher master web service timeout value must be provided");
                hasError = true;
            }
            boolean handleWsEnabled = appProp.isHdlWsEnabled();
            if (handleWsEnabled) {
                if (StringUtils.isBlank(appProp.getHdlWsHostName())) {
                    addFieldError("hdlwshost", "The handle web service host must be provided");
                    hasError = true;
                } else {
                    if (!StringUtils.startsWith(appProp.getHdlWsHostName(), "https://") && (!StringUtils.startsWith(appProp.getHdlWsHostName(), "http://"))) {
                        addFieldError("hdlwshost", "The protocol (https or http) must be included in the handle web service host");
                        hasError = true;
                    }
                }
                if (appProp.getHdlWsHostPort() == 0) {
                    addFieldError("hdlwshostport", "The handle web service host port must be provided");
                    hasError = true;
                }
                if (StringUtils.isBlank(appProp.getHdlWsPath())) {
                    addFieldError("hdlwsapath", "The handle web service path must be provided");
                    hasError = true;
                }
                if (StringUtils.isBlank(appProp.getHdlWsMethod())) {
                    addFieldError("hdlwsmintmethod", "The handle web service mint method must be provided");
                    hasError = true;
                }
                if (StringUtils.isBlank(appProp.getHdlWsAppId())) {
                    addFieldError("hdlwsappid", "The handle web service application id must be provided");
                    hasError = true;
                }
                if (StringUtils.isBlank(appProp.getHdlResolverAddress())) {
                    addFieldError("hdlresolver", "The handle resolver server must be provided");
                    hasError = true;
                }
            }
            if (StringUtils.isBlank(appProp.getCcLicenseWsAddress())) {
                addFieldError("cclicense", "The creative commons license web service address must be provided");
                hasError = true;
            }
        }
        if (StringUtils.isBlank(jdbcProp.getDbHost())) {
            addFieldError("dbhost", "The Database server must be provided ");
            hasError = true;
        }
        if (jdbcProp.getDbPort() == 0) {
            addFieldError("dbport", "The Database server port must be provided");
            hasError = true;
        }
        if (StringUtils.isBlank(jdbcProp.getDbName())) {
            addFieldError("dbname", "The Database name must be provided ");
            hasError = true;
        }
        if (StringUtils.isBlank(jdbcProp.getDbUserName())) {
            addFieldError("dbuser", "The Database user name must be provided ");
            hasError = true;
        }
        if (StringUtils.isBlank(jdbcProp.getDbPassword())) {
            addFieldError("dbpassword", "The Database user password must be provided ");
            hasError = true;
        }
        if (StringUtils.isBlank(mailProp.getMailServer())) {
            addFieldError("mailserver", "The mail server must be provided ");
            hasError = true;
        }
        if (mailProp.getMailServerPort() == 0) {
            addFieldError("mailport", "The mail server port must be provided ");
            hasError = true;
        }
        if (mailProp.isAuthenticated()) {
            if (StringUtils.isBlank(mailProp.getUserName())) {
                addFieldError("mailuser", "The mail user name must be provided");
                hasError = true;
            }
            if (StringUtils.isBlank(mailProp.getPassword())) {
                addFieldError("mailuserpassword", "The mail user password must be provided");
                hasError = true;
            }
        }
        if (ldapProp.isLdapSupported()) {
            if (StringUtils.isBlank(ldapProp.getLdapServer())) {
                addFieldError("ldapserver", "The ldap server must be provided");
                hasError = true;
            }
            if (StringUtils.isBlank(ldapProp.getBaseDN())) {
                addFieldError("basedn", "The ldap server base dn must be provided");
                hasError = true;
            }
            if (StringUtils.isBlank(ldapProp.getAttUID())) {
                addFieldError("attuid", "The attribute uid name must be provided");
                hasError = true;
            }
            if (StringUtils.isBlank(ldapProp.getAttMail())) {
                addFieldError("attmail", "The attribute mail name must be provided");
                hasError = true;
            }
            if (StringUtils.isBlank(ldapProp.getAttGender())) {
                addFieldError("attgender", "The attribute gender name must be provided");
                hasError = true;
            }
            if (StringUtils.isBlank(ldapProp.getAttCN())) {
                addFieldError("attcn", "The attribute cn name must be provided");
                hasError = true;
            }
            if (StringUtils.isBlank(ldapProp.getAttGivenname())) {
                addFieldError("attgivenname", "The attribute givenname name must be provided");
                hasError = true;
            }
            if (StringUtils.isBlank(ldapProp.getAttSn())) {
                addFieldError("attsn", "The attribute sn name must be provided");
                hasError = true;
            }
            if (StringUtils.isBlank(ldapProp.getAttPersonalTitle())) {
                addFieldError("attptitle", "The attribute personaltitle name must be provided");
                hasError = true;
            }
        }
        if (hasError) {
            setDefaultMaps();
        }
    }

    /**
	 * Ajax call for checking store permission
	 * 
	 * @return a String represents SUCCESS or ERROR.
	 */
    public String checkDatastore() {
        return SUCCESS;
    }

    /**
	 * Ajax call for checking the database connection
	 * 
	 * @return a String represents SUCCESS or ERROR.
	 */
    public String checkDbConn() {
        return SUCCESS;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }

    public Map<String, String> getDbTypeNames() {
        return dbTypeNames;
    }

    public void setDbTypeNames(Map<String, String> dbTypeNames) {
        this.dbTypeNames = dbTypeNames;
    }

    public Map<String, String> getTrueFalseMap() {
        return trueFalseMap;
    }

    public void setTrueFalseMap(Map<String, String> trueFalseMap) {
        this.trueFalseMap = trueFalseMap;
    }

    public String getWebinfRoot() {
        return webinfRoot;
    }

    public void setWebinfRoot(String webinfRoot) {
        this.webinfRoot = webinfRoot;
    }

    public String getInstallTempConfPath() {
        return installTempConfPath;
    }

    public void setInstallTempConfPath(String installTempConfPath) {
        this.installTempConfPath = installTempConfPath;
    }

    public String getDestPropConfRoot() {
        return destPropConfRoot;
    }

    public void setDestPropConfRoot(String destPropConfRoot) {
        this.destPropConfRoot = destPropConfRoot;
    }

    public ApplicationProperty getAppProp() {
        return appProp;
    }

    public void setAppProp(ApplicationProperty appProp) {
        this.appProp = appProp;
    }

    public JdbcProperty getJdbcProp() {
        return jdbcProp;
    }

    public void setJdbcProp(JdbcProperty jdbcProp) {
        this.jdbcProp = jdbcProp;
    }

    public MailProperty getMailProp() {
        return mailProp;
    }

    public void setMailProp(MailProperty mailProp) {
        this.mailProp = mailProp;
    }

    public LdapProperty getLdapProp() {
        return ldapProp;
    }

    public void setLdapProp(LdapProperty ldapProp) {
        this.ldapProp = ldapProp;
    }
}
