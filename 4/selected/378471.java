package org.atricore.idbus.capabilities.spnego.jaas;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.io.IOUtils;
import org.ini4j.Ini;
import org.ini4j.Wini;
import org.osgi.framework.BundleContext;
import org.atricore.idbus.kernel.common.support.osgi.OsgiBundleClassLoader;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import java.io.*;

public class KerberosServiceInit {

    private static final Log logger = LogFactory.getLog(KerberosServiceInit.class);

    public static final String KEYTAB_RESOURCE_BASE = "META-INF/krb5";

    private String realm;

    private String kerberosRealm;

    private String keyDistributionCenter;

    private String principal;

    private String keyTabResource;

    private boolean configureKerberos;

    private String keyTabRepository;

    private String defaultKrb5Config;

    private boolean authenticatOnInit;

    private BundleContext bundleContext;

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public String getPrincipal() {
        return principal;
    }

    public void setPrincipal(String principal) {
        this.principal = principal;
    }

    /**
     * Name of the JAAS Realm that configures the Kerberos Login Module
     * @return
     */
    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    /**
     * Name of the Windows Domain (TODO : Remove 'windows' semantic from this module and call this Kerberos Realm ? )
     * @return
     */
    public String getKerberosRealm() {
        return kerberosRealm;
    }

    public void setKerberosRealm(String kerberosRealm) {
        this.kerberosRealm = kerberosRealm;
    }

    /**
     * Domain Controller server name
     * @return
     */
    public String getKeyDistributionCenter() {
        return keyDistributionCenter;
    }

    public void setKeyDistributionCenter(String keyDistributionCenter) {
        this.keyDistributionCenter = keyDistributionCenter;
    }

    /**
     * Name of the embedded resource to be found in the classpath that is actually the keytab file
     * @return
     */
    public String getKeyTabResource() {
        return keyTabResource;
    }

    public void setKeyTabResource(String keyTabResource) {
        this.keyTabResource = keyTabResource;
    }

    /**
     * Path to deploy the embedded keytab
     * @return
     */
    public String getKeyTabRepository() {
        return keyTabRepository;
    }

    public void setKeyTabRepository(String keyTabRepository) {
        this.keyTabRepository = keyTabRepository;
    }

    /**
     * If true, the service will configure kerberos
     * @return
     */
    public boolean isConfigureKerberos() {
        return configureKerberos;
    }

    public void setConfigureKerberos(boolean configureKerberos) {
        this.configureKerberos = configureKerberos;
    }

    /**
     * Kerberos 5 confgiruation file
     * @return
     */
    public String getDefaultKrb5Config() {
        return defaultKrb5Config;
    }

    public void setDefaultKrb5Config(String defaultKrb5Config) {
        this.defaultKrb5Config = defaultKrb5Config;
    }

    public boolean isAuthenticatOnInit() {
        return authenticatOnInit;
    }

    public void setAuthenticatOnInit(boolean authenticatOnInit) {
        this.authenticatOnInit = authenticatOnInit;
    }

    /**
     * Initialize Kerberos
     * @throws Exception
     */
    public void init() throws Exception {
        try {
            if (configureKerberos) configureKerberos(true);
        } catch (Exception e) {
            logger.error("Cannot perform Kerberos configuration :" + e.getMessage(), e);
            throw e;
        }
        try {
            if (authenticatOnInit) authenticate(new String[] { principal });
        } catch (Exception e) {
            logger.error("Cannot perform Kerberos Sign-On:" + e.getMessage(), e);
            throw e;
        }
    }

    public Subject authenticate(Object credentials) throws SecurityException {
        if (logger.isDebugEnabled()) logger.debug("Performing automatic authentication using credentials " + credentials);
        if (!(credentials instanceof String[])) {
            throw new IllegalArgumentException("Expected String[1], got " + (credentials != null ? credentials.getClass().getName() : null));
        }
        final String[] params = (String[]) credentials;
        if (params.length != 1) {
            throw new IllegalArgumentException("Expected String[1] but length was " + params.length);
        }
        try {
            if (logger.isDebugEnabled()) logger.debug("Performing automatic authentication using principal " + params[0]);
            LoginContext loginContext = new LoginContext(realm, new CallbackHandler() {

                public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                    for (int i = 0; i < callbacks.length; i++) {
                        if (callbacks[i] instanceof NameCallback) {
                            ((NameCallback) callbacks[i]).setName(params[0]);
                        } else {
                            throw new UnsupportedCallbackException(callbacks[i]);
                        }
                    }
                }
            });
            loginContext.login();
            return loginContext.getSubject();
        } catch (LoginException e) {
            logger.error("Login failure : " + e.getMessage());
            throw new SecurityException("Authentication failed", e);
        }
    }

    public void configureKerberos(boolean overwriteExistingSetup) throws Exception {
        OutputStream keyTabOut = null;
        InputStream keyTabIn = null;
        OutputStream krb5ConfOut = null;
        try {
            keyTabIn = loadKeyTabResource(keyTabResource);
            File file = new File(keyTabRepository + keyTabResource);
            if (!file.exists() || overwriteExistingSetup) {
                keyTabOut = new FileOutputStream(file, false);
                if (logger.isDebugEnabled()) logger.debug("Installing keytab file to : " + file.getAbsolutePath());
                IOUtils.copy(keyTabIn, keyTabOut);
            }
            File krb5ConfFile = new File(System.getProperty("java.security.krb5.conf", defaultKrb5Config));
            if (logger.isDebugEnabled()) logger.debug("Using Kerberos config file : " + krb5ConfFile.getAbsolutePath());
            if (!krb5ConfFile.exists()) throw new Exception("Kerberos config file not found : " + krb5ConfFile.getAbsolutePath());
            FileInputStream fis = new FileInputStream(krb5ConfFile);
            Wini krb5Conf = new Wini(KerberosConfigUtil.toIni(fis));
            Ini.Section krb5Realms = krb5Conf.get("realms");
            String windowsDomainSetup = krb5Realms.get(kerberosRealm);
            if (kerberosRealm == null || overwriteExistingSetup) {
                windowsDomainSetup = "{  kdc = " + keyDistributionCenter + ":88 admin_server = " + keyDistributionCenter + ":749  default_domain = " + kerberosRealm.toLowerCase() + "  }";
                krb5Realms.put(kerberosRealm, windowsDomainSetup);
            }
            Ini.Section krb5DomainRealms = krb5Conf.get("domain_realm");
            String domainRealmSetup = krb5DomainRealms.get(kerberosRealm.toLowerCase());
            if (domainRealmSetup == null || overwriteExistingSetup) {
                krb5DomainRealms.put(kerberosRealm.toLowerCase(), kerberosRealm);
                krb5DomainRealms.put("." + kerberosRealm.toLowerCase(), kerberosRealm);
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            krb5Conf.store(baos);
            InputStream bios = new ByteArrayInputStream(baos.toByteArray());
            bios = KerberosConfigUtil.toKrb5(bios);
            krb5ConfOut = new FileOutputStream(krb5ConfFile, false);
            IOUtils.copy(bios, krb5ConfOut);
        } catch (Exception e) {
            logger.error("Error while configuring Kerberos :" + e.getMessage(), e);
            throw e;
        } finally {
            IOUtils.closeQuietly(keyTabOut);
            IOUtils.closeQuietly(keyTabIn);
            IOUtils.closeQuietly(krb5ConfOut);
        }
    }

    /**
     * This will try the default location and the resource name as is.
     */
    public InputStream loadKeyTabResource(String keyTabResource) throws Exception {
        String resourcePath = KEYTAB_RESOURCE_BASE + "/" + keyTabResource;
        if (logger.isTraceEnabled()) logger.trace("Loading resource : " + resourcePath);
        ClassLoader cl = new OsgiBundleClassLoader(bundleContext.getBundle());
        InputStream in = cl.getResourceAsStream(resourcePath);
        if (in == null) {
            logger.warn("Cannot load keytab resource from bundle classpath using : " + resourcePath);
            in = cl.getResourceAsStream(keyTabResource);
            if (in == null) {
                logger.warn("Cannot load keytab resource from bundle classpath using : " + keyTabResource);
            }
        }
        if (in == null) throw new Exception("Cannot load keytab resource from bundle classpath (check log for details)");
        return in;
    }
}
