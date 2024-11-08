package org.ikasan.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.ikasan.common.factory.ClassInstantiationUtils;
import org.ikasan.common.security.IkasanSecurityService;
import org.ikasan.common.security.IkasanSecurityServiceImpl;
import org.ikasan.common.security.SecurityNotConfiguredException;
import org.ikasan.common.util.ResourceUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.jndi.JndiTemplate;

/**
 * Singleton for loading common implementation classes behind the interfaces.
 * 
 * @author Ikasan Development Team
 */
public class ResourceLoader {

    /** The logger instance. */
    private static Logger logger = Logger.getLogger(ResourceLoader.class);

    /** The resource name, e.g. location of resource properties file */
    protected static final String FILE_SEPARATOR = System.getProperty("file.separator");

    /** Resource string */
    protected static String RESOURCE_NAME;

    /** instance of the singleton */
    private static ResourceLoader instance = null;

    /** The context implementation class key */
    protected static String CONTEXT_IMPL_CLASS = "contextImpl.class";

    /** The persistence implementation class key */
    protected static String PERSISTENCE_IMPL_CLASS = "persistenceImpl.class";

    /** The XML parser implementation class key */
    protected static String XMLPARSER_IMPL_CLASS = "xmlParserImpl.class";

    /** The XML transformer implementation class key */
    protected static String XMLTRANSFORMER_IMPL_CLASS = "xmlTransformerImpl.class";

    /** The string transformer implementation class key */
    protected static String STRINGTRANSFORMER_IMPL_CLASS = "stringTransformerImpl.class";

    /** The XSL transformer implementation class key */
    protected static String XSLTRANSFORMER_IMPL_CLASS = "xslTransformerImpl.class";

    /** The environment implementation class key */
    protected static String ENVIRONMENT_IMPL_CLASS = "environmentImpl.class";

    /** resource properties */
    protected Properties resources;

    /** Context Implementation Class */
    protected String contextImplClass;

    /** Persistence Implementation Class */
    protected String persistenceImplClass;

    /** XML Parser Implementation Class */
    protected String xmlParserImplClass;

    /** XML Transformer Implementation Class */
    protected String xmlTransformerImplClass;

    /** String Transformer Implementation Class */
    protected String stringTransformerImplClass;

    /** XSL Parser Implementation Class */
    protected String xslTransformerImplClass;

    /** Environment Implementation Class */
    protected String environmentImplClass;

    /** Concrete class for payload */
    protected Class<? extends Payload> payloadClass;

    /** instance of the ikasan platform */
    protected IkasanEnv ikasanEnv = null;

    /** instance of the ikasan platform */
    private IkasanSecurityService ikasanSecurityService = null;

    /** JNDITemplate for accessing JNDI resources from the JMS server */
    private JndiTemplate jmsJndiTemplate;

    /** A commonly used XML parser */
    private CommonXMLParser commonXmlParser;

    /**
     * Singleton constructor
     * 
     * @return ResourceLoader
     */
    public static ResourceLoader getInstance() {
        if (instance != null) {
            return instance;
        }
        synchronized (ResourceLoader.class) {
            if (instance == null) {
                ResourceLoader.RESOURCE_NAME = "commonResource.xml";
                instance = new ResourceLoader();
            }
            return instance;
        }
    }

    /**
     * Private constructor
     */
    private ResourceLoader() {
        this(RESOURCE_NAME);
    }

    /**
     * Default constructor
     * 
     * @param resourceName The resource name so we can get resource properties
     */
    protected ResourceLoader(final String resourceName) {
        try {
            this.resources = ResourceUtils.getAsProperties(resourceName);
            this.loadAllResources(this.resources);
            this.ikasanEnv = IkasanEnvImpl.getInstance(this.newEnvironment());
            if (this.ikasanEnv.getIkasanSecurityResource() != null) {
                ikasanSecurityService = new IkasanSecurityServiceImpl(this.ikasanEnv.getIkasanSecurityResource(), this.newEnvironment());
            }
        } catch (IOException e) {
            String failMsg = "Failed to load [" + resourceName + "]. Nothing will work! ";
            throw new CommonRuntimeException(failMsg, e);
        }
    }

    /**
     * Convenience method to load all resources from a given properties.
     * 
     * @param props Properties for this resource
     */
    public void loadAllResources(Properties props) {
        this.setContextImplClass(props.getProperty(CONTEXT_IMPL_CLASS));
        this.setPersistenceImplClass(props.getProperty(PERSISTENCE_IMPL_CLASS));
        this.setXmlParserImplClass(props.getProperty(XMLPARSER_IMPL_CLASS));
        this.setXmlTransformerImplClass(props.getProperty(XMLTRANSFORMER_IMPL_CLASS));
        this.setStringTransformerImplClass(props.getProperty(STRINGTRANSFORMER_IMPL_CLASS));
        this.setXslTransformerImplClass(props.getProperty(XSLTRANSFORMER_IMPL_CLASS));
        this.setEnvironmentImplClass(props.getProperty(ENVIRONMENT_IMPL_CLASS));
    }

    /**
     * Provide the caller with an instance of the Ikasan Security Service.
     * 
     * @return IkasanSecurityService
     * @throws SecurityNotConfiguredException Exception if security is not configured
     */
    public IkasanSecurityService getIkasanSecurityService() throws SecurityNotConfiguredException {
        if (ikasanSecurityService == null) {
            throw new SecurityNotConfiguredException("Ikasan security service must be configured before invoking " + "security operations. Check your ikasan.xml entries for " + ikasanEnv.getIkasanSecurityResourceMetaData() + ".");
        }
        return this.ikasanSecurityService;
    }

    /**
     * Get the context concrete implementation class name.
     * 
     * @return the contextImplClass
     */
    public String getContextImplClass() {
        logger.debug("Getting contextImplClass [" + this.contextImplClass + "]");
        return this.contextImplClass;
    }

    /**
     * Set the context concrete implementation class name.
     * 
     * @param contextImplClass the contextImplClass to set
     */
    public void setContextImplClass(final String contextImplClass) {
        this.contextImplClass = contextImplClass;
        logger.debug("Setting contextImplClass [" + this.contextImplClass + "]");
    }

    /**
     * Get the environment concrete implementation class name.
     * 
     * @return the environmentImplClass
     */
    public String getEnvironmentImplClass() {
        logger.debug("Getting environmentImplClass [" + this.environmentImplClass + "]");
        return this.environmentImplClass;
    }

    /**
     * Set the environment concrete implementation class name.
     * 
     * @param environmentImplClass the environmentImplClass to set
     */
    public void setEnvironmentImplClass(final String environmentImplClass) {
        this.environmentImplClass = environmentImplClass;
        logger.debug("Setting environmentImplClass [" + this.environmentImplClass + "]");
    }

    /**
     * Get the persistence concrete implementation class name.
     * 
     * @return the persistenceImplClass
     */
    public String getPersistenceImplClass() {
        logger.debug("Getting persistenceImplClass [" + this.persistenceImplClass + "]");
        return this.persistenceImplClass;
    }

    /**
     * Set the persistence concrete implementation class name.
     * 
     * @param persistenceImplClass the persistenceImplClass to set
     */
    public void setPersistenceImplClass(final String persistenceImplClass) {
        this.persistenceImplClass = persistenceImplClass;
        logger.debug("Setting persistenceImplClass [" + this.persistenceImplClass + "]");
    }

    /**
     * Get the XML parser concrete implementation class name.
     * 
     * @return the xmlParserImplClass
     */
    public String getXmlParserImplClass() {
        logger.debug("Getting xmlParserImplClass [" + this.xmlParserImplClass + "]");
        return this.xmlParserImplClass;
    }

    /**
     * Set the XML parser concrete implementation class name.
     * 
     * @param xmlParserImplClass the xmlParserImplClass to set
     */
    public void setXmlParserImplClass(final String xmlParserImplClass) {
        this.xmlParserImplClass = xmlParserImplClass;
        logger.debug("Setting xmlParserImplClass [" + this.xmlParserImplClass + "]");
    }

    /**
     * Get the XML transformer concrete implementation class name.
     * 
     * @return the xmlTransformerImplClass
     */
    public String getXmlTransformerImplClass() {
        logger.debug("Getting xmlTransformerImplClass [" + this.xmlTransformerImplClass + "]");
        return this.xmlTransformerImplClass;
    }

    /**
     * Set the XML transformer concrete implementation class name.
     * 
     * @param xmlTransformerImplClass the xmlTransformerImplClass to set
     */
    public void setXmlTransformerImplClass(final String xmlTransformerImplClass) {
        this.xmlTransformerImplClass = xmlTransformerImplClass;
        logger.debug("Setting xmlTransformerImplClass [" + this.xmlTransformerImplClass + "]");
    }

    /**
     * Get the String transformer concrete implementation class name.
     * 
     * @return the stringTransformerImplClass
     */
    public String getStringTransformerImplClass() {
        logger.debug("Getting stringTransformerImplClass [" + this.stringTransformerImplClass + "]");
        return this.stringTransformerImplClass;
    }

    /**
     * Set the String transformer concrete implementation class name.
     * 
     * @param stringTransformerImplClass the xmlTransformerImplClass to set
     */
    public void setStringTransformerImplClass(final String stringTransformerImplClass) {
        this.stringTransformerImplClass = stringTransformerImplClass;
        logger.debug("Setting stringTransformerImplClass [" + this.stringTransformerImplClass + "]");
    }

    /**
     * Get the XSL transformer concrete implementation class name.
     * 
     * @return the xslTransformerImplClass
     */
    public String getXslTransformerImplClass() {
        logger.debug("Getting xslTransformerImplClass [" + this.xslTransformerImplClass + "]");
        return this.xslTransformerImplClass;
    }

    /**
     * Set the XSL transformer concrete implementation class name.
     * 
     * @param xslTransformerImplClass the xslTransformerImplClass to set
     */
    public void setXslTransformerImplClass(final String xslTransformerImplClass) {
        this.xslTransformerImplClass = xslTransformerImplClass;
        logger.debug("Setting xslTransformerImplClass [" + this.xslTransformerImplClass + "]");
    }

    /**
     * Get the specified property from the underlying resources.
     * 
     * @param propertyName The name of the property to retrieve
     * @return the contextImplClass
     */
    public String getProperty(final String propertyName) {
        logger.debug("Getting property [" + propertyName + "]");
        return this.resources.getProperty(propertyName);
    }

    /**
     * String presentation of the properties settings
     * 
     * @return String
     */
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("\n");
        sb.append("To Be Implemented\n");
        sb.append("\n");
        return sb.toString();
    }

    /**
     * Load resource from the given name. Try loading this resource in the following order, (2) load from the classpath;
     * (4) load from the file system; If all above fail then throw IOException.
     * 
     * @param name - resource name
     * @return byte[]
     * @throws IOException Exception if reading from the File System fails
     */
    public byte[] getAsByteArray(final String name) throws IOException {
        byte[] buffer;
        URL url = ResourceUtils.getAsUrl(name);
        if (url != null) {
            InputStream is = url.openStream();
            buffer = readInputStream(is);
            is.close();
            return buffer;
        }
        FileInputStream fis = new FileInputStream(name);
        buffer = readInputStream(fis);
        fis.close();
        return buffer;
    }

    /**
     * Read the input stream and return the content as a byte array.
     * 
     * @param is The InputStream to read from
     * @return byte[]
     * @throws IOException Exception if we fail to read from the InputStream
     */
    private byte[] readInputStream(final InputStream is) throws IOException {
        int c;
        StringBuffer sb = new StringBuffer();
        while ((c = is.read()) != -1) sb.append((char) c);
        return sb.toString().getBytes();
    }

    /**
     * Get a new instance of the CommonContext
     * 
     * @return CommonContext
     */
    public CommonContext newContext() {
        logger.debug("Instantiating context based on class [" + this.contextImplClass + "]");
        return (CommonContext) ClassInstantiationUtils.instantiate(this.contextImplClass);
    }

    /**
     * Create a new CommonContext based on the incoming properties.
     * 
     * @param properties The properties for this context
     * @return CommonContext
     */
    public CommonContext newContext(final Properties properties) {
        logger.debug("Instantiating context based on class [" + this.contextImplClass + "]");
        Class<?>[] paramTypes = { Properties.class };
        Object[] params = { properties };
        return (CommonContext) ClassInstantiationUtils.instantiate(this.contextImplClass, paramTypes, params);
    }

    /**
     * Get a new instance of the CommonXMLParser
     * 
     * @return CommonXMLParser - this maybe 'null' if it failed to create.
     */
    public CommonXMLParser newXMLParser() {
        logger.debug("Instantiating xmlParser based on class [" + this.xmlParserImplClass + "]");
        return (CommonXMLParser) ClassInstantiationUtils.instantiate(this.xmlParserImplClass);
    }

    /**
     * Get a new instance of the CommonXMLTransformer
     * 
     * @return CommonXMLTransformer - this maybe 'null' if it failed to create.
     */
    public CommonXMLTransformer newXMLTransformer() {
        logger.debug("Instantiating xmlTransformer based on class [" + this.xmlTransformerImplClass + "]");
        return (CommonXMLTransformer) ClassInstantiationUtils.instantiate(this.xmlTransformerImplClass);
    }

    /**
     * Get a new instance of the CommonXSLTransformer
     * 
     * @return CommonXSLTransformer - this maybe 'null' if it failed to create.
     */
    public CommonXSLTransformer newXSLTransformer() {
        logger.debug("Instantiating xslTransformer based on class [" + this.xslTransformerImplClass + "]");
        return (CommonXSLTransformer) ClassInstantiationUtils.instantiate(this.xslTransformerImplClass);
    }

    /**
     * Get a new instance of the CommonStringTransformer
     * 
     * @return CommonStringTransformer - this maybe 'null' if it failed to create.
     */
    public CommonStringTransformer newStringTransformer() {
        logger.debug("Instantiating stringTransformer based on class [" + this.stringTransformerImplClass + "]");
        return (CommonStringTransformer) ClassInstantiationUtils.instantiate(this.stringTransformerImplClass);
    }

    /**
     * Get a new instance of the CommonEnvironment
     * 
     * @return CommonEnvironment - this maybe 'null' if it failed to create.
     */
    public CommonEnvironment newEnvironment() {
        logger.debug("Instantiating environment based on class [" + this.environmentImplClass + "]");
        return (CommonEnvironment) ClassInstantiationUtils.instantiate(this.environmentImplClass);
    }

    public CommonXMLParser getCommonXmlParser() {
        if (commonXmlParser == null) {
            commonXmlParser = (CommonXMLParser) ClassInstantiationUtils.instantiate(this.xmlParserImplClass);
        }
        return commonXmlParser;
    }

    public JndiTemplate getJMSJndiTemplate() {
        if (jmsJndiTemplate == null) {
            ApplicationContext context = new FileSystemXmlApplicationContext(FILE_SEPARATOR + ikasanEnv.getIkasanConfDir() + FILE_SEPARATOR + "jmsJndiContext.xml");
            jmsJndiTemplate = (JndiTemplate) context.getBean("jmsJndiTemplate");
        }
        return jmsJndiTemplate;
    }

    public File getIkasanConfigurationDirectory() {
        return new File(ikasanEnv.getIkasanConfDir());
    }
}
