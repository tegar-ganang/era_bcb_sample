package org.hardtokenmgmt.autoenroll.requesterrepositories;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import org.apache.log4j.Logger;
import org.hardtokenmgmt.autoenroll.AutoEnrollContext;
import org.hardtokenmgmt.autoenroll.BadConfigurationException;
import org.hardtokenmgmt.autoenroll.Constants;
import org.hardtokenmgmt.autoenroll.InValidRequestException;
import org.hardtokenmgmt.autoenroll.getwinowner.GetWinFileOwner;

/**
 * @author Philip Vendil 20090829
 *
 */
public class ADComputerRequesterRepository extends BaseRequesterRepository {

    public static final int UAC_DISABLE = 2;

    private Logger log = null;

    /**
	 * Method that checks the following.
	 * 
	 * 
	 * 2. Check that the owner corresponds to the filename
	 * 3. Reads the contents of the file and returns it
	 * 
	 * @see org.hardtokenmgmt.autoenroll.requesterrepositories.IRequesterRepository#isValidComputer(org.hardtokenmgmt.web.autoenroll.AutoEnrollContext, java.lang.String)
	 */
    @Override
    public String isValidRequester(AutoEnrollContext context) throws IOException, InValidRequestException, BadConfigurationException {
        String retval = null;
        String[] allowedDomains = getProperty(Constants.CONFIG_AD_DOMAIN).split(",");
        File f = new File(context.getFullFilePath());
        if (!f.exists()) {
            throw new InValidRequestException("Error: file with path " + context.getFullFilePath().replaceAll("/", "\\") + " doesn't exist.");
        }
        String[] owner = GetWinFileOwner.getOwner(f).split("\\\\");
        String fileDomain = owner[0];
        String fileUsername = owner[1];
        boolean domainVerifies = false;
        for (String allowedDomain : allowedDomains) {
            if (allowedDomain.equalsIgnoreCase(fileDomain)) {
                domainVerifies = true;
                break;
            }
        }
        getLogger().debug("Comparing owner : " + fileUsername + ", domain :" + fileDomain + " with requestId : " + context.getRequesterId());
        boolean userVerifies = false;
        if (domainVerifies && fileUsername != null) {
            if (fileUsername.equalsIgnoreCase(context.getRequesterId()) || fileUsername.equalsIgnoreCase(context.getRequesterId() + "$")) {
                userVerifies = true;
            }
            if (fileUsername.equalsIgnoreCase("Administrators")) {
                userVerifies = true;
            }
        }
        if (userVerifies) {
            for (int attempts = 0; attempts < 10; attempts++) {
                try {
                    retval = readRequest(f);
                    if (retval != null && retval.contains("----END")) {
                        break;
                    } else {
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            log.debug("Sleep interrupted exception when reading request : " + e.getMessage(), e);
                        }
                    }
                } catch (IOException e) {
                    log.debug("Error reading file due to concurrent access : " + e.getMessage(), e);
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e1) {
                        log.debug("Sleep interrupted exception when reading request : " + e.getMessage(), e);
                    }
                }
            }
        }
        return retval;
    }

    private String readRequest(File f) throws IOException {
        String retval = null;
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(f));
            StringWriter sr = new StringWriter();
            String nextLine = null;
            while ((nextLine = br.readLine()) != null) {
                sr.write(nextLine + "\n");
            }
            retval = sr.toString();
        } finally {
            if (br != null) {
                br.close();
            }
        }
        return retval;
    }

    /**
	 * Test that the AD is up by sending a test query.
	 * 
	 * 
	 * 
	 * @see org.hardtokenmgmt.autoenroll.requesterrepositories.IRequesterRepository#testConnection()
	 */
    @Override
    public String testConnection() {
        String retval = null;
        try {
            String shareLocation = getProperty(Constants.CONFIG_SHARELOCATION);
            File f = new File(shareLocation);
            if (!f.exists()) {
                return "Bad Configuration : share location " + shareLocation + " doesn't exists.";
            }
            if (!f.canWrite()) {
                return "Bad Configuration : no write access to share location " + shareLocation;
            }
            String testRequestId = getProperty(Constants.CONFIG_AD_TEST_REQUESTID);
            fetchEntityData(new AutoEnrollContext("c:/" + testRequestId + ".p10"));
        } catch (BadConfigurationException e) {
            return "Bad Configuration : " + e.getMessage();
        } catch (IOException e) {
            return "IO Exception : " + e.getMessage();
        } catch (InValidRequestException e) {
            return "Invalid Request : " + e.getMessage();
        }
        return retval;
    }

    @Override
    public Entity fetchEntityData(AutoEnrollContext context) throws IOException, InValidRequestException, BadConfigurationException {
        Entity retval = null;
        String connectURL = getProperty(Constants.CONFIG_AD_LDAPURL);
        String baseDN = getProperty(Constants.CONFIG_AD_BASEDN);
        String loginDN = getProperty(Constants.CONFIG_AD_LOGINDN);
        String loginPwd = getProperty(Constants.CONFIG_AD_LOGINPWD);
        String referral = props.getProperty(Constants.CONFIG_AD_REFERRAL, Constants.DEFAULT_AD_REFERRAL);
        String authType = props.getProperty(Constants.CONFIG_AD_AUTHENTICATIONTYPE);
        String requestId = context.getRequesterId();
        requestId = requestId.replaceAll("\\$", "");
        String searchFilter = getProperty(Constants.CONFIG_AD_SEARCHFILTER);
        searchFilter = searchFilter.replaceAll(Constants.REQUESTERIDNAME_REPLACE_PATTERN, requestId);
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, connectURL);
        env.put(Context.REFERRAL, referral);
        if (authType != null) {
            env.put(Context.SECURITY_AUTHENTICATION, authType);
        }
        env.put(Context.SECURITY_PRINCIPAL, loginDN);
        env.put(Context.SECURITY_CREDENTIALS, loginPwd);
        DirContext ctx;
        try {
            ctx = new InitialDirContext(env);
            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            sc.setReturningAttributes(new String[] { "sAMAccountName", "useraccountcontrol", "cn", "dNSHostName", "objectClass", "userPrincipalName" });
            NamingEnumeration<SearchResult> result = ctx.search(baseDN, searchFilter, sc);
            while (result.hasMoreElements()) {
                SearchResult sr = result.nextElement();
                String userAccountControlString = sr.getAttributes().get("useraccountcontrol").get().toString();
                long uacValue = Long.parseLong(userAccountControlString);
                if ((uacValue & UAC_DISABLE) == 0) {
                    Attribute objClasses = sr.getAttributes().get("objectClass");
                    boolean isUser = !objClasses.contains("computer");
                    String fullUniqueId = "";
                    String commonName = "";
                    Entity.Type entityType = Entity.Type.COMPUTER;
                    if (isUser) {
                        entityType = Entity.Type.USER;
                        fullUniqueId = sr.getAttributes().get("userPrincipalName").get().toString();
                        commonName = sr.getAttributes().get("cn").get().toString();
                    } else {
                        fullUniqueId = sr.getAttributes().get("dNSHostName").get().toString();
                        commonName = fullUniqueId;
                    }
                    String uniqueId = sr.getAttributes().get("sAMAccountName").get().toString();
                    uniqueId = stripLastDollarSign(uniqueId);
                    retval = new Entity(entityType, uniqueId, commonName, fullUniqueId);
                } else {
                    throw new InValidRequestException("Error: Requester with id: " + context.getRequesterId() + " is disabled in Active Directory");
                }
            }
            if (retval == null) {
                throw new InValidRequestException("Error: Requester with id: " + context.getRequesterId() + " could be found in Active Directory");
            }
        } catch (NamingException e) {
            throw new IOException("Error performing LDAP search : " + e.getMessage(), e);
        }
        return retval;
    }

    private String stripLastDollarSign(String uniqueId) {
        if (uniqueId != null && uniqueId.endsWith("$")) {
            return uniqueId.substring(0, uniqueId.length() - 1);
        }
        return uniqueId;
    }

    private String getProperty(String key) throws BadConfigurationException {
        String retval = props.getProperty(key);
        if (retval == null) {
            throw new BadConfigurationException("Error in organization configuration, setting " + key + " must be set.");
        }
        return retval;
    }

    private Logger getLogger() {
        if (log == null) {
            log = Logger.getLogger(ADComputerRequesterRepository.class);
        }
        return log;
    }
}
