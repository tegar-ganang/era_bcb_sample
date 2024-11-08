package org.tolven.assembler.jboss5;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.security.KeyStore;
import java.util.Collection;
import java.util.Properties;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.java.plugin.registry.Extension;
import org.java.plugin.registry.ExtensionPoint;
import org.java.plugin.registry.PluginDescriptor;
import org.java.plugin.registry.Extension.Parameter;
import org.tolven.plugin.TolvenCommandPlugin;
import org.tolven.security.auth.PasswordStoreImpl;
import org.tolven.security.hash.TolvenMessageDigest;
import org.tolven.tools.ant.TolvenCopy;
import org.tolven.tools.ant.TolvenJar;

/**
 * This plugin assembles all of the tolven specific configuration files for the JBoss appserver
 * 
 * @author Joseph Isaac
 *
 */
public class JBoss5Assembler extends TolvenCommandPlugin {

    public static final String CONFDIR = "server/tolven/conf";

    public static final String LIBDIR = "server/tolven/lib";

    public static final String MESSAGE_DIGEST_ALGORITHM = "md5";

    public static final String REQUIRED_PLUGIN_ASSEMBLER_EAR = "org.tolven.assembler.ear";

    public static final String REQUIRED_PLUGIN_ASSEMBLER_TOMCATSERVER = "org.tolven.assembler.tomcatserver";

    public static final String EXTENSIONPOINT_DB_PLUGIN_COMPONENT = "databasePlugin";

    public static final String EXTENSIONPOINT_TOLVENCOMMON_JAR = "tolvenCommon";

    public static final String EXTENSIONPOINT_CREDENTIALSTORE_RAR = "credentialStore";

    public static final String EXTENSIONPOINT_TOLVENDS_PROVIDER = "tolvenDSProvider";

    public static final String EXTENSIONPOINT_TOLVENJMS_PROVIDER = "tolvenJMSProvider";

    public static final String EXTENSIONPOINT_SERVERSECURITY_PRODUCT = "serverSecurityProduct";

    public static final String EXTENSIONPOINT_USER_LOGIN_CONTEXT = "userLoginContext";

    public static final String EXTENSIONPOINT_LIB_CLASSES = "classes";

    public static final String EXTENSION_LDAPSOURCE = "ldapSource";

    public static final String ATTRIBUTE_DEPLOY_DIR = "deployDir";

    public static final String ATTRIBUTE_STAGE_DIR = "stageDir";

    public static final String ATTRIBUTE_SOURCE_LOGIN_CONFIG = "sourceLoginConfigFile";

    public static final String ATTRIBUTE_DEST_LOGIN_CONFIG = "destLoginConfigFile";

    public static final String ATTRIBUTE_SOURCE_SERVERXML = "sourceServerXML";

    public static final String ATTRIBUTE_DEST_SERVERXML = "destServerXML";

    public static final String ATTRIBUTE_SOURCE_JMSDS = "sourceJMSDS";

    public static final String ATTRIBUTE_DEST_JMSDS = "destJMSDS";

    public static final String ATTRIBUTE_SOURCE_MESSAGING = "sourceMessaging";

    public static final String ATTRIBUTE_DEST_MESSAGING = "destMessaging";

    public static final String ATTRIBUTE_SOURCE_TOLVENINIT = "sourceTolvenInit";

    public static final String ATTRIBUTE_DEST_TOLVENINIT = "destTolvenInit";

    public static final String ATTRIBUTE_SOURCE_TOLVENLDAP = "sourceTolvenLDAP";

    public static final String ATTRIBUTE_DEST_TOLVENLDAP = "destTolvenLDAP";

    public static final String ATTRIBUTE_SOURCE_EARDEPLOYER = "sourceEARDeployer";

    public static final String ATTRIBUTE_DEST_EARDEPLOYER = "destEARDeployer";

    public static final String ATTRIBUTE_DEST_TOLVENDS = "destTolvenDS";

    public static final String ATTRIBUTE_DEST_TOLVENJMS = "destTolvenJMS";

    public static final String CMD_LINE_DEST_EAR_DIR_OPTION = "destEARDir";

    public static final String CMD_LINE_SOURCE_SERVERXML_FILE_OPTION = "source";

    public static final String CMD_LINE_DEST_SERVERXML_FILE_OPTION = "dest";

    private Logger logger = Logger.getLogger(JBoss5Assembler.class);

    @Override
    protected void doStart() throws Exception {
        logger.debug("*** start ***");
    }

    @Override
    public void execute(String[] args) throws Exception {
        logger.debug("*** execute ***");
        File[] tmpFiles = getPluginTmpDir().listFiles();
        if (tmpFiles != null && tmpFiles.length > 0) {
            return;
        }
        executeRequiredPlugins(args);
        collectTolvenDSProduct();
        assembleTolvenLDAPProduct();
        assembleTolvenJMSProduct();
        assembleJMSDSProduct();
        assembleMessagingProduct();
        assembleEARDeployerProduct();
        assembleServerSecurityProduct();
        assembleLoginConfigProduct();
        assembleLibClasses();
        assembleTolvenCommonJar();
        copyToStageDir();
    }

    protected void executeRequiredPlugins(String[] args) throws Exception {
        ExtensionPoint dbPluginExtensionPoint = getDescriptor().getExtensionPoint(EXTENSIONPOINT_DB_PLUGIN_COMPONENT);
        Extension dbPluginExtension = getSingleConnectedExtension(dbPluginExtensionPoint);
        String dbPluginDescriptor = dbPluginExtension.getDeclaringPluginDescriptor().getId();
        execute(dbPluginDescriptor, args);
        String deployDirname = getDescriptor().getAttribute(ATTRIBUTE_DEPLOY_DIR).getValue();
        File deployDir = new File(getPluginTmpDir(), deployDirname);
        if (!deployDir.exists()) {
            deployDir.mkdirs();
        }
        String earAssemblerCommand = "-" + CMD_LINE_DEST_EAR_DIR_OPTION + " " + deployDir.getPath();
        execute(REQUIRED_PLUGIN_ASSEMBLER_EAR, earAssemblerCommand.split(" "));
        String sourceServerXMLFilename = getDescriptor().getAttribute(ATTRIBUTE_SOURCE_SERVERXML).getValue();
        File sourceServerXMLFile = getFilePath(sourceServerXMLFilename);
        String destServerXMLFilename = getDescriptor().getAttribute(ATTRIBUTE_DEST_SERVERXML).getValue();
        File destServerXMLFile = new File(getPluginTmpDir(), destServerXMLFilename);
        String serverXMLAssemblerCommand = "-" + CMD_LINE_SOURCE_SERVERXML_FILE_OPTION + " " + sourceServerXMLFile.getPath() + " -" + CMD_LINE_DEST_SERVERXML_FILE_OPTION + " " + destServerXMLFile.getPath();
        execute(REQUIRED_PLUGIN_ASSEMBLER_TOMCATSERVER, serverXMLAssemblerCommand.split(" "));
    }

    protected void collectTolvenDSProduct() throws IOException {
        String destTolvenDSFilename = getDescriptor().getAttribute(ATTRIBUTE_DEST_TOLVENDS).getValue();
        File destTolvenDSFile = new File(getPluginTmpDir(), destTolvenDSFilename);
        ExtensionPoint tolvenDSProviderExtensionPoint = getMyExtensionPoint(EXTENSIONPOINT_TOLVENDS_PROVIDER);
        for (Extension tolvenDSProviderExtension : tolvenDSProviderExtensionPoint.getConnectedExtensions()) {
            String sourceTolvenDS = tolvenDSProviderExtension.getParameter("tolvenDS").valueAsString();
            File sourceTolvenDSPluginTmpDir = getPluginTmpDir(tolvenDSProviderExtension.getDeclaringPluginDescriptor());
            File sourceTolvenDSFile = new File(sourceTolvenDSPluginTmpDir, sourceTolvenDS);
            if (!destTolvenDSFile.exists() || sourceTolvenDSFile.lastModified() > destTolvenDSFile.lastModified()) {
                logger.debug(destTolvenDSFile.getPath() + " was replaced since its source files are more recent");
                logger.debug("Copy " + sourceTolvenDSFile.getPath() + " to " + destTolvenDSFile);
                FileUtils.copyFile(sourceTolvenDSFile, destTolvenDSFile);
            } else {
                logger.debug(destTolvenDSFile.getPath() + " is more recent than any of its source file: " + sourceTolvenDSFile.getPath());
            }
        }
    }

    protected void assembleTolvenLDAPProduct() {
        ExtensionPoint ldapSourceExtensionPoint = getDescriptor().getExtensionPoint(EXTENSION_LDAPSOURCE);
        ExtensionPoint parentLDAPSourceExtensionPoint = getParentExtensionPoint(ldapSourceExtensionPoint);
        PluginDescriptor parentLDAPDescriptor = parentLDAPSourceExtensionPoint.getDeclaringPluginDescriptor();
        String ldapProtocol = (String) evaluate(parentLDAPSourceExtensionPoint.getParameterDefinition("ldap.protocol").getDefaultValue(), parentLDAPDescriptor);
        String ldapHostname = (String) evaluate(parentLDAPSourceExtensionPoint.getParameterDefinition("ldap.hostname").getDefaultValue(), parentLDAPDescriptor);
        String ldapPort = (String) evaluate(parentLDAPSourceExtensionPoint.getParameterDefinition("ldap.port").getDefaultValue(), parentLDAPDescriptor);
        String connectionStringValue = ldapProtocol + "://" + ldapHostname + ":" + ldapPort;
        Properties properties = new Properties();
        String connectionString = getDescriptor().getAttribute("ldapConnectionString").getValue();
        properties.setProperty(connectionString, connectionStringValue);
        String sourceTolvenLDAP = getDescriptor().getAttribute(ATTRIBUTE_SOURCE_TOLVENLDAP).getValue();
        File sourceTolvenLDAPFile = getFilePath(sourceTolvenLDAP);
        String destTolvenLDAP = getDescriptor().getAttribute(ATTRIBUTE_DEST_TOLVENLDAP).getValue();
        File destTolvenLDAPFile = new File(getPluginTmpDir(), destTolvenLDAP);
        logger.debug("Copy " + sourceTolvenLDAPFile.getPath() + " to " + destTolvenLDAPFile);
        TolvenCopy.copyFile(sourceTolvenLDAPFile, destTolvenLDAPFile, properties);
    }

    protected void assembleTolvenJMSProduct() throws IOException {
        String destTolvenJMSFilename = getDescriptor().getAttribute(ATTRIBUTE_DEST_TOLVENJMS).getValue();
        File destTolvenJMSFile = new File(getPluginTmpDir(), destTolvenJMSFilename);
        ExtensionPoint tolvenJMSProviderExtensionPoint = getMyExtensionPoint(EXTENSIONPOINT_TOLVENJMS_PROVIDER);
        for (Extension tolvenJMSProviderExtension : tolvenJMSProviderExtensionPoint.getConnectedExtensions()) {
            String sourceTolvenJMS = tolvenJMSProviderExtension.getParameter("tolvenJMS").valueAsString();
            File sourceTolvenJMSPluginTmpDir = getPluginTmpDir(tolvenJMSProviderExtension.getDeclaringPluginDescriptor());
            File sourceTolvenJMSFile = new File(sourceTolvenJMSPluginTmpDir, sourceTolvenJMS);
            if (!destTolvenJMSFile.exists() || sourceTolvenJMSFile.lastModified() > destTolvenJMSFile.lastModified()) {
                logger.debug(destTolvenJMSFile.getPath() + " was replaced since its source files are more recent");
                logger.debug("Copy " + sourceTolvenJMSFile.getPath() + " to " + destTolvenJMSFile);
                FileUtils.copyFile(sourceTolvenJMSFile, destTolvenJMSFile);
            } else {
                logger.debug(destTolvenJMSFile.getPath() + " is more recent than any of its source file: " + sourceTolvenJMSFile.getPath());
            }
        }
    }

    protected void assembleJMSDSProduct() throws IOException {
        String destJMSDSFilename = getDescriptor().getAttribute(ATTRIBUTE_DEST_JMSDS).getValue();
        File destJMSDSFile = new File(getPluginTmpDir(), destJMSDSFilename);
        String sourceJMSDS = getDescriptor().getAttribute(ATTRIBUTE_SOURCE_JMSDS).getValue();
        File sourceJMSDSFile = getFilePath(sourceJMSDS);
        if (!destJMSDSFile.exists() || sourceJMSDSFile.lastModified() > destJMSDSFile.lastModified()) {
            logger.debug(destJMSDSFile.getPath() + " was replaced since its source files are more recent");
            logger.debug("Copy " + sourceJMSDSFile.getPath() + " to " + destJMSDSFile);
            FileUtils.copyFile(sourceJMSDSFile, destJMSDSFile);
        } else {
            logger.debug(destJMSDSFile.getPath() + " is more recent than any of its source file: " + sourceJMSDSFile.getPath());
        }
    }

    protected void assembleMessagingProduct() throws IOException {
        String destMessagingFilename = getDescriptor().getAttribute(ATTRIBUTE_DEST_MESSAGING).getValue();
        File destMessagingFile = new File(getPluginTmpDir(), destMessagingFilename);
        String sourceMessaging = getDescriptor().getAttribute(ATTRIBUTE_SOURCE_MESSAGING).getValue();
        File sourceMessagingFile = getFilePath(sourceMessaging);
        if (!destMessagingFile.exists() || sourceMessagingFile.lastModified() > destMessagingFile.lastModified()) {
            logger.debug(destMessagingFile.getPath() + " was replaced since its source files are more recent");
            logger.debug("Copy " + sourceMessagingFile.getPath() + " to " + destMessagingFile);
            FileUtils.copyFile(sourceMessagingFile, destMessagingFile);
        } else {
            logger.debug(destMessagingFile.getPath() + " is more recent than any of its source file: " + sourceMessagingFile.getPath());
        }
    }

    protected void assembleEARDeployerProduct() throws IOException {
        String destEARDeployerFilename = getDescriptor().getAttribute(ATTRIBUTE_DEST_EARDEPLOYER).getValue();
        File destEARDeployerFile = new File(getPluginTmpDir(), destEARDeployerFilename);
        String sourceEARDeployer = getDescriptor().getAttribute(ATTRIBUTE_SOURCE_EARDEPLOYER).getValue();
        File sourceEARDeployerFile = getFilePath(sourceEARDeployer);
        if (!destEARDeployerFile.exists() || sourceEARDeployerFile.lastModified() > destEARDeployerFile.lastModified()) {
            logger.debug(destEARDeployerFile.getPath() + " was replaced since its source files are more recent");
            logger.debug("Copy " + sourceEARDeployerFile.getPath() + " to " + destEARDeployerFile);
            FileUtils.copyFile(sourceEARDeployerFile, destEARDeployerFile);
        } else {
            logger.debug(destEARDeployerFile.getPath() + " is more recent than any of its source file: " + sourceEARDeployerFile.getPath());
        }
    }

    protected void assembleServerSecurityProduct() throws IOException {
        String sourceTolvenInit = getDescriptor().getAttribute(ATTRIBUTE_SOURCE_TOLVENINIT).getValue();
        File sourceTolvenInitFile = getFilePath(sourceTolvenInit);
        File productDefPluginDataDir = getPluginTmpDir();
        String destTolvenInit = getDescriptor().getAttribute(ATTRIBUTE_DEST_TOLVENINIT).getValue();
        File destTolvenInitFile = new File(productDefPluginDataDir, destTolvenInit);
        String credentialDirname = getDescriptor().getAttribute("credentialDir").getValue();
        String appserverHome = (String) evaluate("#{globalProperty['appserver.home']}", getDescriptor());
        File appserverHomeDir = new File(appserverHome);
        if (!appserverHomeDir.exists()) {
            throw new RuntimeException("Could not find appserver home directory: " + appserverHomeDir.getPath());
        }
        File deployCredentialDir = new File(appserverHomeDir.getParentFile(), credentialDirname);
        Properties properties = new Properties();
        String appServerCredentialDirname = getTolvenConfigWrapper().getPasswordServer().getCredentialDir();
        File appServerCredentialDir = new File(appServerCredentialDirname);
        File passwordStoreFile = generatePasswordStoreFile(appServerCredentialDir);
        properties.setProperty("tolvendev-tolven-passwordStore-property", new File(deployCredentialDir, passwordStoreFile.getName()).toURI().toURL().toExternalForm());
        String passwordStoreCredentialGroupId = getTolvenConfigWrapper().getPasswordServer().getId();
        File passwordKeyStoreFile = getTolvenConfigWrapper().getKeyStoreFile(passwordStoreCredentialGroupId);
        properties.setProperty("tolvendev-tolven-keystore-property", new File(deployCredentialDir, passwordKeyStoreFile.getName()).toURI().toURL().toExternalForm());
        properties.setProperty("tolvendev-tolven-keystore", passwordKeyStoreFile.getName());
        String passwordKeyStoreType = getTolvenConfigWrapper().getKeyStoreType(passwordStoreCredentialGroupId);
        properties.setProperty("tolvendev-tolven-keystore-type-property", passwordKeyStoreType);
        properties.setProperty("tolvendev-tolven-keystore-type", passwordKeyStoreType);
        String appServerCredentialGroupId = getTolvenConfigWrapper().getAppServer().getId();
        File sourceAppServerSSLKeyStoreFile = getTolvenConfigWrapper().getKeyStoreFile(appServerCredentialGroupId);
        properties.setProperty("javax-net-ssl-keyStore", new File(deployCredentialDir, sourceAppServerSSLKeyStoreFile.getName()).getPath().replace("\\", "/"));
        String appServerSSLKeyStoreType = getTolvenConfigWrapper().getKeyStoreType(appServerCredentialGroupId);
        properties.setProperty("javax-net-ssl-keyStoreType", appServerSSLKeyStoreType);
        File sourceAppServerSSLTrustStoreFile = getTolvenConfigWrapper().getTrustStoreFile(appServerCredentialGroupId);
        properties.setProperty("javax-net-ssl-trustStore", new File(deployCredentialDir, sourceAppServerSSLTrustStoreFile.getName()).getPath().replace("\\", "/"));
        String appServerSSLTrustStoreType = getTolvenConfigWrapper().getTrustStoreType(appServerCredentialGroupId);
        properties.setProperty("javax-net-ssl-trustStoreType", appServerSSLTrustStoreType);
        ExtensionPoint serverSecurityExtensionPoint = getMyExtensionPoint(EXTENSIONPOINT_SERVERSECURITY_PRODUCT);
        Collection<Extension> productExtensions = serverSecurityExtensionPoint.getConnectedExtensions();
        String promptForPassword = null;
        String passwordPrompt = null;
        if (productExtensions.isEmpty()) {
            promptForPassword = (String) evaluate(serverSecurityExtensionPoint.getParameterDefinition("promptForPassword").getDefaultValue());
            if (promptForPassword != null) {
                logger.debug("Prompt for password from: " + serverSecurityExtensionPoint.getUniqueId() + " is: " + promptForPassword);
            }
            passwordPrompt = (String) evaluate(serverSecurityExtensionPoint.getParameterDefinition("passwordPrompt").getDefaultValue());
            if (passwordPrompt != null) {
                logger.debug("Password prompt from: " + serverSecurityExtensionPoint.getUniqueId() + " is: " + passwordPrompt);
            }
        } else {
            for (Extension productExtension : productExtensions) {
                if (productExtension.getParameter("passwordPrompt") != null) {
                    passwordPrompt = productExtension.getParameter("passwordPrompt").valueAsString();
                    logger.debug("Password prompt from: " + productExtension.getUniqueId() + " is: " + passwordPrompt);
                }
                promptForPassword = (String) evaluate(productExtension.getParameter("promptForPassword").valueAsString(), productExtension.getDeclaringPluginDescriptor());
                if (promptForPassword != null) {
                    logger.debug("Prompt for password from: " + productExtension.getDeclaringPluginDescriptor() + " is: " + promptForPassword);
                }
                break;
            }
        }
        if (passwordPrompt == null) {
            passwordPrompt = getDescriptor().getAttribute("default-passwordPrompt").getValue();
        }
        if ("false".equals(promptForPassword)) {
            char[] password = getPassword(appServerCredentialGroupId);
            if (password == null) {
                throw new RuntimeException("A passwordPrompt is requested, but no password is associated with credential  group: " + appServerCredentialGroupId);
            }
            properties.setProperty("password-store-entry-prompt", passwordPrompt + ":" + new String(password));
        } else {
            properties.setProperty("password-store-entry-prompt", passwordPrompt);
        }
        logger.debug("Copy " + sourceTolvenInitFile.getPath() + " to " + destTolvenInitFile);
        TolvenCopy.copyFile(sourceTolvenInitFile, destTolvenInitFile, properties);
    }

    private File generatePasswordStoreFile(File localCredentialDir) throws IOException {
        String groupId = getTolvenConfigWrapper().getPasswordServerId();
        if (groupId == null) {
            throw new RuntimeException("The password server groupId is null");
        }
        char[] passwordStorePassword = getPassword(groupId);
        if (passwordStorePassword == null) {
            throw new RuntimeException("Password for password server group Id " + groupId + " is null");
        }
        KeyStore keyStore = getTolvenConfigWrapper().getKeyStore(groupId, passwordStorePassword);
        if (keyStore == null) {
            throw new RuntimeException("Could not find password server keystore with group Id: " + groupId);
        }
        PasswordStoreImpl passwordStore = new PasswordStoreImpl(keyStore, passwordStorePassword);
        String appserverId = getTolvenConfigWrapper().getAppServerId();
        if (appserverId == null) {
            throw new RuntimeException("The appserver Id is null for group Id: " + groupId);
        }
        char[] appserverIdPassword = getPassword(appserverId);
        if (appserverIdPassword == null) {
            throw new RuntimeException("The password is null when retrieved with appserver password Id: '" + appserverId + "'");
        }
        passwordStore.setPassword(appserverId, appserverIdPassword);
        String ldapRootPasswordId = getTolvenConfigWrapper().getLDAPServerRootPasswordId();
        if (ldapRootPasswordId == null) {
            throw new RuntimeException("The ldap root password Id is null");
        }
        char[] ldapRootPassword = getPassword(ldapRootPasswordId);
        if (ldapRootPassword == null) {
            throw new RuntimeException("The ldap root password is null when retrieved with password Id: '" + ldapRootPasswordId + "'");
        }
        passwordStore.setPassword(ldapRootPasswordId, ldapRootPassword);
        String dbRootPasswordId = getTolvenConfigWrapper().getDBServerRootPasswordId();
        if (dbRootPasswordId == null) {
            throw new RuntimeException("The db root password Id is null");
        }
        char[] dbRootPassword = getPassword(dbRootPasswordId);
        if (dbRootPassword == null) {
            throw new RuntimeException("The db root password is null when retrieved with password Id: '" + dbRootPasswordId + "'");
        }
        passwordStore.setPassword(dbRootPasswordId, dbRootPassword);
        String mdbuserPasswordId = getTolvenConfigWrapper().getMDBUserId();
        if (mdbuserPasswordId == null) {
            throw new RuntimeException("The mdbuser Id is null");
        }
        char[] mdbuserPassword = getPassword(mdbuserPasswordId);
        if (mdbuserPassword == null) {
            throw new RuntimeException("The password is null when retrieved with mdbuser password Id: '" + mdbuserPasswordId + "'");
        }
        passwordStore.setPassword(mdbuserPasswordId, mdbuserPassword);
        File passwordStoreFile = new File(localCredentialDir, "passwordStore.properties");
        FileOutputStream out = null;
        Properties encryptedPasswords = passwordStore.getEncryptedPasswords();
        try {
            passwordStoreFile.getParentFile().mkdirs();
            out = new FileOutputStream(passwordStoreFile);
            logger.debug("Store encrypted passwords in " + passwordStoreFile.getPath());
            encryptedPasswords.store(out, null);
        } finally {
            if (out != null) {
                out.close();
            }
        }
        return passwordStoreFile;
    }

    protected void assembleLoginConfigProduct() {
        String templateFilename = getDescriptor().getAttribute(ATTRIBUTE_SOURCE_LOGIN_CONFIG).getValue();
        File templateFile = getFilePath(templateFilename);
        StringBuffer originalXML = new StringBuffer();
        try {
            originalXML.append(FileUtils.readFileToString(templateFile));
        } catch (IOException ex) {
            throw new RuntimeException("Could not read the login-config template file: " + templateFile.getPath(), ex);
        }
        String xslt = null;
        try {
            xslt = getXSLT();
        } catch (XMLStreamException ex) {
            throw new RuntimeException("Could not generate the XSLT for login-config the file: " + templateFile.getPath(), ex);
        }
        File xsltFile = new File(getPluginTmpDir(), "login-config-xslt.xml");
        logger.debug("Write xslt file " + xsltFile.getPath());
        try {
            FileUtils.writeStringToFile(xsltFile, xslt);
        } catch (IOException ex) {
            throw new RuntimeException("Could not write the login-config xslt file: " + xsltFile.getPath() + " as\n" + xslt, ex);
        }
        String translatedXMLString = getTranslatedXML(originalXML.toString(), xslt);
        String destLoginConfigFilename = getDescriptor().getAttribute(ATTRIBUTE_DEST_LOGIN_CONFIG).getValue();
        File destLoginConfigFile = new File(getPluginTmpDir(), destLoginConfigFilename);
        destLoginConfigFile.getParentFile().mkdirs();
        logger.debug("Write translated server.xml file to " + destLoginConfigFile);
        try {
            FileUtils.writeStringToFile(destLoginConfigFile, translatedXMLString);
        } catch (IOException ex) {
            throw new RuntimeException("Could not write the login-config to file: " + destLoginConfigFile.getPath() + " as\n" + translatedXMLString, ex);
        }
    }

    protected String getXSLT() throws XMLStreamException {
        StringWriter xslt = new StringWriter();
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        XMLStreamWriter xmlStreamWriter = null;
        try {
            xmlStreamWriter = factory.createXMLStreamWriter(xslt);
            xmlStreamWriter.writeStartDocument("UTF-8", "1.0");
            xmlStreamWriter.writeCharacters("\n");
            xmlStreamWriter.writeStartElement("xsl:stylesheet");
            xmlStreamWriter.writeAttribute("version", "2.0");
            xmlStreamWriter.writeNamespace("xsl", "http://www.w3.org/1999/XSL/Transform");
            xmlStreamWriter.writeCharacters("\n");
            xmlStreamWriter.writeStartElement("xsl:output");
            xmlStreamWriter.writeAttribute("method", "xml");
            xmlStreamWriter.writeAttribute("indent", "yes");
            xmlStreamWriter.writeAttribute("encoding", "UTF-8");
            xmlStreamWriter.writeAttribute("omit-xml-declaration", "no");
            xmlStreamWriter.writeEndElement();
            xmlStreamWriter.writeCharacters("\n");
            addMainTemplate(xmlStreamWriter);
            addPolicyTemplate(xmlStreamWriter);
            xmlStreamWriter.writeEndDocument();
            xmlStreamWriter.writeEndDocument();
        } finally {
            if (xmlStreamWriter != null) {
                xmlStreamWriter.close();
            }
        }
        return xslt.toString();
    }

    protected void addMainTemplate(XMLStreamWriter xmlStreamWriter) throws XMLStreamException {
        xmlStreamWriter.writeStartElement("xsl:template");
        xmlStreamWriter.writeAttribute("match", "/ | * | @* | text() | comment()");
        xmlStreamWriter.writeCharacters("\n");
        xmlStreamWriter.writeStartElement("xsl:copy");
        xmlStreamWriter.writeAttribute("select", ".");
        xmlStreamWriter.writeCharacters("\n");
        xmlStreamWriter.writeStartElement("xsl:apply-templates");
        xmlStreamWriter.writeAttribute("select", "* | @* | text() | comment()");
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeCharacters("\n");
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeCharacters("\n");
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeCharacters("\n");
    }

    protected void addPolicyTemplate(XMLStreamWriter xmlStreamWriter) throws XMLStreamException {
        xmlStreamWriter.writeStartElement("xsl:template");
        xmlStreamWriter.writeAttribute("match", "policy");
        xmlStreamWriter.writeCharacters("\n");
        xmlStreamWriter.writeStartElement("xsl:element");
        xmlStreamWriter.writeAttribute("name", "{name()}");
        xmlStreamWriter.writeCharacters("\n");
        xmlStreamWriter.writeStartElement("xsl:for-each");
        xmlStreamWriter.writeAttribute("select", "@*");
        xmlStreamWriter.writeCharacters("\n");
        xmlStreamWriter.writeStartElement("xsl:attribute");
        xmlStreamWriter.writeAttribute("name", "{name()}");
        xmlStreamWriter.writeStartElement("xsl:value-of");
        xmlStreamWriter.writeAttribute("select", ".");
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeCharacters("\n");
        xmlStreamWriter.writeStartElement("xsl:value-of");
        xmlStreamWriter.writeAttribute("select", "text()");
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeCharacters("\n");
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeCharacters("\n");
        addDefaultLDAPManagerLoginContext(xmlStreamWriter);
        addDefaultDBLoginContext(xmlStreamWriter);
        addDefaultRuleQueueLoginContext(xmlStreamWriter);
        ExtensionPoint userLoginContextExtensionPoint = getDescriptor().getExtensionPoint(EXTENSIONPOINT_USER_LOGIN_CONTEXT);
        int nConnectedExtensions = userLoginContextExtensionPoint.getConnectedExtensions().size();
        if (nConnectedExtensions > 1) {
            throw new RuntimeException("Only one or no extensions are allowed to be connected to: " + userLoginContextExtensionPoint.getUniqueId());
        }
        if (nConnectedExtensions == 0) {
            addDefaultUserLoginContext(xmlStreamWriter);
            addJBossBugUserLoginContext(xmlStreamWriter);
        } else {
            Extension userLoginContextExtension = getSingleConnectedExtension(userLoginContextExtensionPoint);
            addCustomUserLoginContext(userLoginContextExtension, xmlStreamWriter);
        }
        addMessagingLoginContext(xmlStreamWriter);
        addJMSLoginContext(xmlStreamWriter);
        addDefaultJMXConsoleLoginContext(xmlStreamWriter);
        xmlStreamWriter.writeStartElement("xsl:copy-of");
        xmlStreamWriter.writeAttribute("select", "*");
        xmlStreamWriter.writeCharacters("\n");
        xmlStreamWriter.writeStartElement("xsl:apply-templates");
        xmlStreamWriter.writeAttribute("select", "* | @* | text() | comment()");
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeCharacters("\n");
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeCharacters("\n");
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeCharacters("\n");
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeCharacters("\n");
    }

    protected void addDefaultLDAPManagerLoginContext(XMLStreamWriter xmlStreamWriter) throws XMLStreamException {
        xmlStreamWriter.writeStartElement("application-policy");
        xmlStreamWriter.writeAttribute("name", "tolvenLDAPManager");
        xmlStreamWriter.writeStartElement("authentication");
        xmlStreamWriter.writeStartElement("login-module");
        xmlStreamWriter.writeAttribute("code", "org.tolven.security.auth.PasswordStoreLoginModule");
        xmlStreamWriter.writeAttribute("flag", "required");
        xmlStreamWriter.writeStartElement("module-option");
        xmlStreamWriter.writeAttribute("name", "username");
        xmlStreamWriter.writeCharacters(getTolvenConfigWrapper().getLDAPServerRootUser());
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeStartElement("module-option");
        xmlStreamWriter.writeAttribute("name", "passwordStoreAlias");
        xmlStreamWriter.writeCharacters(getTolvenConfigWrapper().getLDAPServerRootPasswordId());
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeEndElement();
    }

    protected void addDefaultDBLoginContext(XMLStreamWriter xmlStreamWriter) throws XMLStreamException {
        xmlStreamWriter.writeStartElement("application-policy");
        xmlStreamWriter.writeAttribute("name", "tolvenDB");
        xmlStreamWriter.writeStartElement("authentication");
        xmlStreamWriter.writeStartElement("login-module");
        xmlStreamWriter.writeAttribute("code", "org.tolven.security.auth.DBPasswordStoreLoginModule");
        xmlStreamWriter.writeAttribute("flag", "required");
        xmlStreamWriter.writeStartElement("module-option");
        xmlStreamWriter.writeAttribute("name", "username");
        xmlStreamWriter.writeCharacters(getTolvenConfigWrapper().getDBServer().getUser());
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeStartElement("module-option");
        xmlStreamWriter.writeAttribute("name", "passwordStoreAlias");
        xmlStreamWriter.writeCharacters(getTolvenConfigWrapper().getDBServerRootPasswordId());
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeStartElement("login-module");
        xmlStreamWriter.writeAttribute("code", "org.tolven.security.auth.ManagedConnectionFactoryLoginModule");
        xmlStreamWriter.writeAttribute("flag", "required");
        xmlStreamWriter.writeStartElement("module-option");
        xmlStreamWriter.writeAttribute("name", "password-stacking");
        xmlStreamWriter.writeCharacters("useFirstPass");
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeStartElement("module-option");
        xmlStreamWriter.writeAttribute("name", "managedConnectionFactoryKey");
        xmlStreamWriter.writeCharacters("ManagedConnectionFactory");
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeStartElement("module-option");
        xmlStreamWriter.writeAttribute("name", "managedConnectionFactoryName");
        xmlStreamWriter.writeCharacters("jboss.jca:service=XATxCM,name=DefaultDS");
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeStartElement("module-option");
        xmlStreamWriter.writeAttribute("name", "serverId");
        xmlStreamWriter.writeCharacters("jboss");
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeEndElement();
    }

    protected void addDefaultRuleQueueLoginContext(XMLStreamWriter xmlStreamWriter) throws XMLStreamException {
        xmlStreamWriter.writeStartElement("application-policy");
        xmlStreamWriter.writeAttribute("name", "tolvenRuleQueue");
        xmlStreamWriter.writeStartElement("authentication");
        xmlStreamWriter.writeStartElement("login-module");
        xmlStreamWriter.writeAttribute("code", "org.tolven.security.auth.PasswordStoreLoginModule");
        xmlStreamWriter.writeAttribute("flag", "required");
        xmlStreamWriter.writeStartElement("module-option");
        xmlStreamWriter.writeAttribute("name", "username");
        xmlStreamWriter.writeCharacters(getTolvenConfigWrapper().getMDBUserId());
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeStartElement("module-option");
        xmlStreamWriter.writeAttribute("name", "passwordStoreAlias");
        xmlStreamWriter.writeCharacters(getTolvenConfigWrapper().getMDBUserId());
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeEndElement();
    }

    protected void addDefaultUserLoginContext(XMLStreamWriter xmlStreamWriter) throws XMLStreamException {
        xmlStreamWriter.writeStartElement("application-policy");
        xmlStreamWriter.writeAttribute("name", "tolvenLDAP");
        xmlStreamWriter.writeStartElement("authentication");
        xmlStreamWriter.writeStartElement("login-module");
        xmlStreamWriter.writeAttribute("code", "org.tolven.security.auth.KeyLoginModule");
        xmlStreamWriter.writeAttribute("flag", "required");
        xmlStreamWriter.writeStartElement("module-option");
        xmlStreamWriter.writeAttribute("name", "jaasSecurityDomain");
        xmlStreamWriter.writeCharacters("tolven/ldap");
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeStartElement("module-option");
        xmlStreamWriter.writeAttribute("name", "principalDNPrefix");
        xmlStreamWriter.writeCharacters("uid");
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeStartElement("module-option");
        xmlStreamWriter.writeAttribute("name", "principalDNSuffix");
        ExtensionPoint ldapSourceExtensionPoint = getDescriptor().getExtensionPoint(EXTENSION_LDAPSOURCE);
        ExtensionPoint parentLDAPSourceExtensionPoint = getParentExtensionPoint(ldapSourceExtensionPoint);
        PluginDescriptor parentLDAPDescriptor = parentLDAPSourceExtensionPoint.getDeclaringPluginDescriptor();
        String ldapPeople = parentLDAPSourceExtensionPoint.getParameterDefinition("ldap.people").getDefaultValue();
        String eval_ldapPeople = (String) evaluate(ldapPeople, parentLDAPDescriptor);
        if (eval_ldapPeople == null) {
            throw new RuntimeException("plugin property: ldapPeople '" + ldapPeople + "'evaluated to: null for: " + ldapSourceExtensionPoint.getUniqueId());
        }
        String ldapSuffix = parentLDAPSourceExtensionPoint.getParameterDefinition("ldap.suffix").getDefaultValue();
        String eval_ldapSuffix = (String) evaluate(ldapSuffix, parentLDAPDescriptor);
        if (eval_ldapSuffix == null) {
            throw new RuntimeException("plugin property: ldapSuffix '" + ldapSuffix + "'evaluated to: null for: " + ldapSourceExtensionPoint.getUniqueId());
        }
        xmlStreamWriter.writeCharacters(eval_ldapPeople + "," + eval_ldapSuffix);
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeStartElement("module-option");
        xmlStreamWriter.writeAttribute("name", "rolesCtxDN");
        String ldapGroups = parentLDAPSourceExtensionPoint.getParameterDefinition("ldap.groups").getDefaultValue();
        String eval_ldapGroups = (String) evaluate(ldapGroups, parentLDAPDescriptor);
        if (eval_ldapGroups == null) {
            throw new RuntimeException("plugin property: ldapGroups '" + ldapGroups + "'evaluated to: null for: " + ldapSourceExtensionPoint.getUniqueId());
        }
        xmlStreamWriter.writeCharacters(eval_ldapGroups + "," + eval_ldapSuffix);
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeStartElement("module-option");
        xmlStreamWriter.writeAttribute("name", "roleAttributeID");
        xmlStreamWriter.writeCharacters("cn");
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeStartElement("module-option");
        xmlStreamWriter.writeAttribute("name", "guestPrincipalName");
        xmlStreamWriter.writeCharacters("tolvenGuest");
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeStartElement("module-option");
        xmlStreamWriter.writeAttribute("name", "guestPassword");
        xmlStreamWriter.writeCharacters("tolvenGuest");
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeStartElement("login-module");
        xmlStreamWriter.writeAttribute("code", "org.jboss.security.ClientLoginModule");
        xmlStreamWriter.writeAttribute("flag", "required");
        xmlStreamWriter.writeStartElement("module-option");
        xmlStreamWriter.writeAttribute("name", "restore-login-identity");
        xmlStreamWriter.writeCharacters("true");
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeEndElement();
    }

    protected void addJBossBugUserLoginContext(XMLStreamWriter xmlStreamWriter) throws XMLStreamException {
        xmlStreamWriter.writeStartElement("application-policy");
        xmlStreamWriter.writeAttribute("name", "CLIENT_LOGIN_MODULE");
        xmlStreamWriter.writeStartElement("authentication");
        xmlStreamWriter.writeStartElement("login-module");
        xmlStreamWriter.writeAttribute("code", "org.tolven.security.auth.KeyLoginModule");
        xmlStreamWriter.writeAttribute("flag", "required");
        xmlStreamWriter.writeStartElement("module-option");
        xmlStreamWriter.writeAttribute("name", "jaasSecurityDomain");
        xmlStreamWriter.writeCharacters("tolven/ldap");
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeStartElement("module-option");
        xmlStreamWriter.writeAttribute("name", "principalDNPrefix");
        xmlStreamWriter.writeCharacters("uid");
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeStartElement("module-option");
        xmlStreamWriter.writeAttribute("name", "principalDNSuffix");
        ExtensionPoint ldapSourceExtensionPoint = getDescriptor().getExtensionPoint(EXTENSION_LDAPSOURCE);
        ExtensionPoint parentLDAPSourceExtensionPoint = getParentExtensionPoint(ldapSourceExtensionPoint);
        PluginDescriptor parentLDAPDescriptor = parentLDAPSourceExtensionPoint.getDeclaringPluginDescriptor();
        String ldapPeople = parentLDAPSourceExtensionPoint.getParameterDefinition("ldap.people").getDefaultValue();
        String eval_ldapPeople = (String) evaluate(ldapPeople, parentLDAPDescriptor);
        if (eval_ldapPeople == null) {
            throw new RuntimeException("plugin property: ldapPeople '" + ldapPeople + "'evaluated to: null for: " + ldapSourceExtensionPoint.getUniqueId());
        }
        String ldapSuffix = parentLDAPSourceExtensionPoint.getParameterDefinition("ldap.suffix").getDefaultValue();
        String eval_ldapSuffix = (String) evaluate(ldapSuffix, parentLDAPDescriptor);
        if (eval_ldapSuffix == null) {
            throw new RuntimeException("plugin property: ldapSuffix '" + ldapSuffix + "'evaluated to: null for: " + ldapSourceExtensionPoint.getUniqueId());
        }
        xmlStreamWriter.writeCharacters(eval_ldapPeople + "," + eval_ldapSuffix);
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeStartElement("module-option");
        xmlStreamWriter.writeAttribute("name", "rolesCtxDN");
        String ldapGroups = parentLDAPSourceExtensionPoint.getParameterDefinition("ldap.groups").getDefaultValue();
        String eval_ldapGroups = (String) evaluate(ldapGroups, parentLDAPDescriptor);
        if (eval_ldapGroups == null) {
            throw new RuntimeException("plugin property: ldapGroups '" + ldapGroups + "'evaluated to: null for: " + ldapSourceExtensionPoint.getUniqueId());
        }
        xmlStreamWriter.writeCharacters(eval_ldapGroups + "," + eval_ldapSuffix);
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeStartElement("module-option");
        xmlStreamWriter.writeAttribute("name", "roleAttributeID");
        xmlStreamWriter.writeCharacters("cn");
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeStartElement("module-option");
        xmlStreamWriter.writeAttribute("name", "guestPrincipalName");
        xmlStreamWriter.writeCharacters("tolvenGuest");
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeStartElement("module-option");
        xmlStreamWriter.writeAttribute("name", "guestPassword");
        xmlStreamWriter.writeCharacters("tolvenGuest");
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeStartElement("login-module");
        xmlStreamWriter.writeAttribute("code", "org.jboss.security.ClientLoginModule");
        xmlStreamWriter.writeAttribute("flag", "required");
        xmlStreamWriter.writeStartElement("module-option");
        xmlStreamWriter.writeAttribute("name", "restore-login-identity");
        xmlStreamWriter.writeCharacters("true");
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeEndElement();
    }

    protected void addCustomUserLoginContext(Extension userLoginContextExtension, XMLStreamWriter xmlStreamWriter) throws XMLStreamException {
        PluginDescriptor pluginDescriptor = userLoginContextExtension.getDeclaringPluginDescriptor();
        xmlStreamWriter.writeStartElement("application-policy");
        xmlStreamWriter.writeAttribute("name", "tolvenLDAP");
        xmlStreamWriter.writeStartElement("authentication");
        for (Parameter loginModuleParameter : userLoginContextExtension.getParameters("login-module")) {
            xmlStreamWriter.writeStartElement("login-module");
            String code = loginModuleParameter.getSubParameter("code").valueAsString();
            String eval_code = (String) evaluate(code, pluginDescriptor);
            if (eval_code == null) {
                throw new RuntimeException("plugin property: code '" + code + "'evaluated to: null");
            }
            xmlStreamWriter.writeAttribute("code", eval_code);
            String flag = loginModuleParameter.getSubParameter("flag").valueAsString();
            String eval_flag = (String) evaluate(code, pluginDescriptor);
            if (eval_flag == null) {
                throw new RuntimeException("plugin property: flag '" + flag + "'evaluated to: null");
            }
            xmlStreamWriter.writeAttribute("flag", flag);
            for (Parameter moduleOptionParameter : loginModuleParameter.getSubParameters("module-option")) {
                xmlStreamWriter.writeStartElement("module-option");
                String name = moduleOptionParameter.getSubParameter("name").valueAsString();
                String eval_name = (String) evaluate(name, pluginDescriptor);
                if (eval_name == null) {
                    throw new RuntimeException("plugin property: name '" + name + "'evaluated to: null");
                }
                xmlStreamWriter.writeAttribute("name", name);
                String value = moduleOptionParameter.getSubParameter("value").valueAsString();
                String eval_value = (String) evaluate(value, pluginDescriptor);
                if (eval_value == null) {
                    throw new RuntimeException("plugin property: value '" + value + "'evaluated to: null");
                }
                xmlStreamWriter.writeCharacters(value);
                xmlStreamWriter.writeEndElement();
            }
            xmlStreamWriter.writeEndElement();
        }
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeEndElement();
    }

    protected void addMessagingLoginContext(XMLStreamWriter xmlStreamWriter) throws XMLStreamException {
        xmlStreamWriter.writeStartElement("application-policy");
        xmlStreamWriter.writeAttribute("name", "tolvenMessaging");
        xmlStreamWriter.writeStartElement("authentication");
        xmlStreamWriter.writeStartElement("login-module");
        xmlStreamWriter.writeAttribute("code", "org.tolven.security.auth.MessagingLoginModule");
        xmlStreamWriter.writeAttribute("flag", "required");
        xmlStreamWriter.writeStartElement("module-option");
        xmlStreamWriter.writeAttribute("name", "jaasSecurityDomain");
        xmlStreamWriter.writeCharacters("tolven/ldap");
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeStartElement("module-option");
        xmlStreamWriter.writeAttribute("name", "principalDNPrefix");
        xmlStreamWriter.writeCharacters("uid");
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeStartElement("module-option");
        xmlStreamWriter.writeAttribute("name", "principalDNSuffix");
        ExtensionPoint ldapSourceExtensionPoint = getDescriptor().getExtensionPoint(EXTENSION_LDAPSOURCE);
        ExtensionPoint parentLDAPSourceExtensionPoint = getParentExtensionPoint(ldapSourceExtensionPoint);
        PluginDescriptor parentLDAPDescriptor = parentLDAPSourceExtensionPoint.getDeclaringPluginDescriptor();
        String ldapPeople = parentLDAPSourceExtensionPoint.getParameterDefinition("ldap.people").getDefaultValue();
        String eval_ldapPeople = (String) evaluate(ldapPeople, parentLDAPDescriptor);
        if (eval_ldapPeople == null) {
            throw new RuntimeException("plugin property: ldapPeople '" + ldapPeople + "'evaluated to: null for: " + ldapSourceExtensionPoint.getUniqueId());
        }
        String ldapSuffix = parentLDAPSourceExtensionPoint.getParameterDefinition("ldap.suffix").getDefaultValue();
        String eval_ldapSuffix = (String) evaluate(ldapSuffix, parentLDAPDescriptor);
        if (eval_ldapSuffix == null) {
            throw new RuntimeException("plugin property: ldapSuffix '" + ldapSuffix + "'evaluated to: null for: " + ldapSourceExtensionPoint.getUniqueId());
        }
        xmlStreamWriter.writeCharacters(eval_ldapPeople + "," + eval_ldapSuffix);
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeStartElement("module-option");
        xmlStreamWriter.writeAttribute("name", "rolesCtxDN");
        String ldapGroups = parentLDAPSourceExtensionPoint.getParameterDefinition("ldap.groups").getDefaultValue();
        String eval_ldapGroups = (String) evaluate(ldapGroups, parentLDAPDescriptor);
        if (eval_ldapGroups == null) {
            throw new RuntimeException("plugin property: ldapGroups '" + ldapGroups + "'evaluated to: null for: " + ldapSourceExtensionPoint.getUniqueId());
        }
        xmlStreamWriter.writeCharacters(eval_ldapGroups + "," + eval_ldapSuffix);
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeStartElement("module-option");
        xmlStreamWriter.writeAttribute("name", "roleAttributeID");
        xmlStreamWriter.writeCharacters("cn");
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeStartElement("module-option");
        xmlStreamWriter.writeAttribute("name", "guestPrincipalName");
        xmlStreamWriter.writeCharacters("tolvenGuest");
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeStartElement("module-option");
        xmlStreamWriter.writeAttribute("name", "guestPassword");
        xmlStreamWriter.writeCharacters("tolvenGuest");
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeEndElement();
    }

    protected void addJMSLoginContext(XMLStreamWriter xmlStreamWriter) throws XMLStreamException {
        xmlStreamWriter.writeStartElement("application-policy");
        xmlStreamWriter.writeAttribute("name", "tolvenJMS");
        xmlStreamWriter.writeStartElement("authentication");
        xmlStreamWriter.writeStartElement("login-module");
        xmlStreamWriter.writeAttribute("code", "org.tolven.security.auth.ManagedConnectionFactoryLoginModule");
        xmlStreamWriter.writeAttribute("flag", "required");
        xmlStreamWriter.writeStartElement("module-option");
        xmlStreamWriter.writeAttribute("name", "managedConnectionFactoryKey");
        xmlStreamWriter.writeCharacters("ManagedConnectionFactory");
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeStartElement("module-option");
        xmlStreamWriter.writeAttribute("name", "managedConnectionFactoryName");
        xmlStreamWriter.writeCharacters("jboss.jca:service=TxCM,name=JmsXA");
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeStartElement("module-option");
        xmlStreamWriter.writeAttribute("name", "serverId");
        xmlStreamWriter.writeCharacters("jboss");
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeEndElement();
    }

    protected void addDefaultJMXConsoleLoginContext(XMLStreamWriter xmlStreamWriter) throws XMLStreamException {
        xmlStreamWriter.writeStartElement("application-policy");
        xmlStreamWriter.writeAttribute("name", "jmx-console");
        xmlStreamWriter.writeStartElement("authentication");
        xmlStreamWriter.writeStartElement("login-module");
        xmlStreamWriter.writeAttribute("code", "org.tolven.security.auth.KeyLoginModule");
        xmlStreamWriter.writeAttribute("flag", "required");
        xmlStreamWriter.writeStartElement("module-option");
        xmlStreamWriter.writeAttribute("name", "jaasSecurityDomain");
        xmlStreamWriter.writeCharacters("tolven/ldap");
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeStartElement("module-option");
        xmlStreamWriter.writeAttribute("name", "principalDNPrefix");
        xmlStreamWriter.writeCharacters("uid");
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeStartElement("module-option");
        xmlStreamWriter.writeAttribute("name", "principalDNSuffix");
        ExtensionPoint ldapSourceExtensionPoint = getDescriptor().getExtensionPoint(EXTENSION_LDAPSOURCE);
        ExtensionPoint parentLDAPSourceExtensionPoint = getParentExtensionPoint(ldapSourceExtensionPoint);
        PluginDescriptor parentLDAPDescriptor = parentLDAPSourceExtensionPoint.getDeclaringPluginDescriptor();
        String ldapPeople = parentLDAPSourceExtensionPoint.getParameterDefinition("ldap.people").getDefaultValue();
        String eval_ldapPeople = (String) evaluate(ldapPeople, parentLDAPDescriptor);
        if (eval_ldapPeople == null) {
            throw new RuntimeException("plugin property: ldapPeople '" + ldapPeople + "'evaluated to: null for: " + ldapSourceExtensionPoint.getUniqueId());
        }
        String ldapSuffix = parentLDAPSourceExtensionPoint.getParameterDefinition("ldap.suffix").getDefaultValue();
        String eval_ldapSuffix = (String) evaluate(ldapSuffix, parentLDAPDescriptor);
        if (eval_ldapSuffix == null) {
            throw new RuntimeException("plugin property: ldapSuffix '" + ldapSuffix + "'evaluated to: null for: " + ldapSourceExtensionPoint.getUniqueId());
        }
        xmlStreamWriter.writeCharacters(eval_ldapPeople + "," + eval_ldapSuffix);
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeStartElement("module-option");
        xmlStreamWriter.writeAttribute("name", "rolesCtxDN");
        String ldapGroups = parentLDAPSourceExtensionPoint.getParameterDefinition("ldap.groups").getDefaultValue();
        String eval_ldapGroups = (String) evaluate(ldapGroups, parentLDAPDescriptor);
        if (eval_ldapGroups == null) {
            throw new RuntimeException("plugin property: ldapGroups '" + ldapGroups + "'evaluated to: null for: " + ldapSourceExtensionPoint.getUniqueId());
        }
        xmlStreamWriter.writeCharacters(eval_ldapGroups + "," + eval_ldapSuffix);
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeStartElement("module-option");
        xmlStreamWriter.writeAttribute("name", "roleAttributeID");
        xmlStreamWriter.writeCharacters("cn");
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeStartElement("module-option");
        xmlStreamWriter.writeAttribute("name", "guestPrincipalName");
        xmlStreamWriter.writeCharacters("tolvenGuest");
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeStartElement("module-option");
        xmlStreamWriter.writeAttribute("name", "guestPassword");
        xmlStreamWriter.writeCharacters("tolvenGuest");
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeStartElement("login-module");
        xmlStreamWriter.writeAttribute("code", "org.jboss.security.ClientLoginModule");
        xmlStreamWriter.writeAttribute("flag", "required");
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeEndElement();
    }

    protected void assembleLibClasses() throws IOException {
        File jar = new File(getPluginTmpDir(), getDescriptor().getId() + ".jar");
        jar.delete();
        ExtensionPoint classesExtensionPoint = getDescriptor().getExtensionPoint(EXTENSIONPOINT_LIB_CLASSES);
        for (Extension classesExtension : classesExtensionPoint.getConnectedExtensions()) {
            PluginDescriptor pluginDescriptor = classesExtension.getDeclaringPluginDescriptor();
            String dirname = classesExtension.getParameter("dir").valueAsString();
            String eval_dirname = (String) evaluate(dirname, pluginDescriptor);
            if (eval_dirname == null) {
                throw new RuntimeException("plugin property: dir '" + dirname + "'evaluated to: null for: " + pluginDescriptor);
            }
            File dir = getFilePath(pluginDescriptor, dirname);
            TolvenJar.jar(dir, jar);
        }
        if (jar.exists()) {
            String appserverHome = (String) evaluate("#{globalProperty['appserver.home']}", getDescriptor());
            File appserverHomeDir = new File(appserverHome);
            if (!appserverHomeDir.exists()) {
                throw new RuntimeException("Could not find appserver home directory: " + appserverHomeDir.getPath());
            }
            File stageDirAppserverHomeDir = new File(getStageDir(), appserverHomeDir.getName());
            File stageAppserverLibDir = new File(stageDirAppserverHomeDir, LIBDIR);
            logger.debug("Copy " + jar.getPath() + " to " + stageAppserverLibDir.getPath());
            FileUtils.copyFileToDirectory(jar, stageAppserverLibDir, true);
        }
    }

    protected void assembleTolvenCommonJar() throws IOException {
        ExtensionPoint tolvenCommonJarExtensionPoint = getDescriptor().getExtensionPoint(EXTENSIONPOINT_TOLVENCOMMON_JAR);
        ExtensionPoint tolvenCommonJARParentExtensionPoint = getParentExtensionPoint(tolvenCommonJarExtensionPoint);
        PluginDescriptor tolvenCommonJarPluginDescritor = tolvenCommonJARParentExtensionPoint.getDeclaringPluginDescriptor();
        String tolvenCommonJarname = tolvenCommonJARParentExtensionPoint.getParameterDefinition("tolvenCommon").getDefaultValue();
        File sourceTolvenCommonJar = getFilePath(tolvenCommonJarPluginDescritor, tolvenCommonJarname);
        String sourceDigest = TolvenMessageDigest.checksum(sourceTolvenCommonJar.toURI().toURL(), MESSAGE_DIGEST_ALGORITHM);
        String appserverHome = (String) evaluate("#{globalProperty['appserver.home']}", getDescriptor());
        File appserverHomeDir = new File(appserverHome);
        if (!appserverHomeDir.exists()) {
            throw new RuntimeException("Could not find appserver home directory: " + appserverHomeDir.getPath());
        }
        File stageDirAppserverHomeDir = new File(getStageDir(), appserverHomeDir.getName());
        File stageAppserverLibDir = new File(stageDirAppserverHomeDir, LIBDIR);
        File destTolvenCommonJar = new File(stageAppserverLibDir, sourceTolvenCommonJar.getName());
        String destDigest = null;
        if (destTolvenCommonJar.exists()) {
            destDigest = TolvenMessageDigest.checksum(destTolvenCommonJar.toURI().toURL(), MESSAGE_DIGEST_ALGORITHM);
        }
        if (destDigest == null || !destDigest.equals(sourceDigest)) {
            logger.debug("Copy " + sourceTolvenCommonJar.getPath() + " to " + destTolvenCommonJar.getPath());
            FileUtils.copyFile(sourceTolvenCommonJar, destTolvenCommonJar);
        }
    }

    protected void copyToStageDir() throws IOException {
        String deployDirname = getDescriptor().getAttribute(ATTRIBUTE_STAGE_DIR).getValue();
        File jbossTmpDir = new File(getPluginTmpDir(), deployDirname);
        File jbossStageDir = new File(getStageDir(), deployDirname);
        logger.debug("Copy " + jbossTmpDir.getPath() + " to " + jbossStageDir.getPath());
        jbossStageDir.mkdirs();
        FileUtils.copyDirectory(jbossTmpDir, jbossStageDir);
    }

    @Override
    protected void doStop() throws Exception {
        logger.debug("*** stop ***");
    }
}
