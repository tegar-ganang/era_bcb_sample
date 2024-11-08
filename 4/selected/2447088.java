package org.opennms.netmgt.asterisk.utils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.log4j.Category;
import org.asteriskjava.manager.AuthenticationFailedException;
import org.asteriskjava.manager.DefaultManagerConnection;
import org.asteriskjava.manager.ManagerConnectionFactory;
import org.asteriskjava.manager.TimeoutException;
import org.asteriskjava.manager.action.OriginateAction;
import org.asteriskjava.manager.response.ManagerResponse;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;
import org.opennms.core.utils.PropertiesUtils;
import org.opennms.core.utils.ThreadCategory;
import org.opennms.netmgt.config.AmiPeerFactory;
import org.opennms.protocols.ami.AmiAgentConfig;

/**
 * Originates a call using the Asterisk Manager API
 * 
 * @author <A HREF="mailto:jeffg@opennms.org">Jeff Gehlbach</A>
 */
public class AsteriskOriginator {

    private static final String DEFAULT_AMI_HOST = "127.0.0.1";

    private static final boolean DEFAULT_ORIGINATOR_DEBUG = true;

    private static final long DEFAULT_RESPONSE_TIMEOUT = 10000;

    private static final String DEFAULT_LEGA_CALLER_ID = "OpenNMS<9195551212>";

    private static final long DEFAULT_LEGA_TIMEOUT = 30000;

    private static final String DEFAULT_LEGA_CHANNEL_PATTERN = "Local/${exten}@default";

    private static final String DEFAULT_LEGB_CONTEXT = "default";

    private static final String DEFAULT_LEGB_EXTENSION = "noc";

    private static final int DEFAULT_LEGB_PRIORITY = 1;

    private DefaultManagerConnection m_managerConnection = null;

    private OriginateAction m_originateAction = null;

    private ManagerResponse m_managerResponse = null;

    private Properties m_amiProps;

    private boolean m_debug;

    private long m_responseTimeout;

    private InetAddress m_amiHost;

    private String m_legAChannelPattern;

    private String m_callerId;

    private long m_dialTimeout;

    private String m_legBContext;

    private int m_legBPriority;

    private String m_legBExtension;

    private boolean m_legBIsApp;

    private String m_legBAppPattern;

    private String m_legBAppDataPattern;

    private String m_legBApp;

    private String m_legBAppData;

    private String m_legAExtension;

    private String m_legAChannel;

    private String m_subject;

    private String m_messageText;

    private Map<String, String> m_channelVars;

    public AsteriskOriginator(Properties amiProps) throws AsteriskOriginatorException {
        try {
            configureProperties(amiProps);
        } catch (IOException e) {
            throw new AsteriskOriginatorException("Failed to construct originator", e);
        }
        try {
            AmiPeerFactory.init();
        } catch (MarshalException me) {
            throw new AsteriskOriginatorException("Failed to unmarshal AMI peer factory configuration", me);
        } catch (ValidationException ve) {
            throw new AsteriskOriginatorException("Failed to validate AMI peer factory configuration", ve);
        } catch (IOException ioe) {
            throw new AsteriskOriginatorException("I/O error initializing AMI peer factory", ioe);
        }
        AmiAgentConfig agentConfig = AmiPeerFactory.getInstance().getAgentConfig(m_amiHost);
        ManagerConnectionFactory mcf = new ManagerConnectionFactory(m_amiHost.getHostAddress(), agentConfig.getPort(), agentConfig.getUsername(), agentConfig.getPassword());
        if (agentConfig.getUseTls()) {
            m_managerConnection = (DefaultManagerConnection) mcf.createSecureManagerConnection();
        } else {
            m_managerConnection = (DefaultManagerConnection) mcf.createManagerConnection();
        }
        m_managerConnection.setDefaultResponseTimeout(m_responseTimeout);
        m_channelVars = new HashMap<String, String>();
    }

    /**
     * Default constructor.  Default properties from asterisk-properties are set into session.
     * @throws AsteriskOriginatorException 
     */
    public AsteriskOriginator() throws AsteriskOriginatorException {
        this(new Properties());
    }

    /**
     * This method uses a properties file reader to pull in opennms styled AMI properties and sets
     * the actual AMi properties.  This is here to preserve the backwards compatibility but configuration
     * will probably change soon.
     * 
     * @throws IOException
     */
    private void configureProperties(Properties amiProps) throws IOException {
        m_amiProps = AsteriskConfig.getProperties();
        m_amiProps.putAll(amiProps);
        m_debug = PropertiesUtils.getProperty(m_amiProps, "org.opennms.asterisk.originate.debug", DEFAULT_ORIGINATOR_DEBUG);
        m_responseTimeout = PropertiesUtils.getProperty(m_amiProps, "org.opennms.asterisk.originate.responsetimeout", DEFAULT_RESPONSE_TIMEOUT);
        m_amiHost = InetAddress.getByName(PropertiesUtils.getProperty(m_amiProps, "org.opennms.asterisk.originate.amiHost", DEFAULT_AMI_HOST));
        m_legAChannelPattern = PropertiesUtils.getProperty(m_amiProps, "org.opennms.asterisk.originate.legachannel", DEFAULT_LEGA_CHANNEL_PATTERN);
        m_callerId = PropertiesUtils.getProperty(m_amiProps, "org.opennms.asterisk.originate.legacallerid", DEFAULT_LEGA_CALLER_ID);
        m_dialTimeout = PropertiesUtils.getProperty(m_amiProps, "org.opennms.asterisk.originate.legadialtimeout", DEFAULT_LEGA_TIMEOUT);
        m_legBContext = PropertiesUtils.getProperty(m_amiProps, "org.opennms.asterisk.originate.legbcontext", DEFAULT_LEGB_CONTEXT);
        m_legBExtension = PropertiesUtils.getProperty(m_amiProps, "org.opennms.asterisk.originate.legbextension", DEFAULT_LEGB_EXTENSION);
        String legBPriorityStr = PropertiesUtils.getProperty(m_amiProps, "org.opennms.asterisk.originate.legbpriority", new Integer(DEFAULT_LEGB_PRIORITY).toString());
        m_legBPriority = Integer.parseInt(legBPriorityStr);
        m_legBAppPattern = PropertiesUtils.getProperty(m_amiProps, "org.opennms.asterisk.originate.legbapp", null);
        m_legBAppDataPattern = PropertiesUtils.getProperty(m_amiProps, "org.opennms.asterisk.originate.legbappdata", null);
        m_legBIsApp = (m_legBAppPattern != null && !"".equals(m_legBAppPattern));
    }

    /**
     * Originates a call based on properties set on this bean.
     */
    public void originateCall() throws AsteriskOriginatorException {
        m_originateAction = buildOriginateAction();
        log().info("Logging in Asterisk manager connection");
        try {
            m_managerConnection.login();
        } catch (IllegalStateException ise) {
            throw new AsteriskOriginatorException("Illegal state logging in Asterisk manager connection", ise);
        } catch (IOException ioe) {
            throw new AsteriskOriginatorException("I/O exception logging in Asterisk manager connection", ioe);
        } catch (AuthenticationFailedException afe) {
            throw new AsteriskOriginatorException("Authentication failure logging in Asterisk manager connection", afe);
        } catch (TimeoutException toe) {
            throw new AsteriskOriginatorException("Timed out logging in Asterisk manager connection", toe);
        }
        log().info("Successfully logged in Asterisk manager connection");
        log().info("Originating a call to extension " + m_legAExtension);
        if (log().isDebugEnabled()) {
            log().debug(createCallLogMsg());
            log().debug("Originate action:\n\n" + m_originateAction.toString());
        }
        try {
            m_managerResponse = m_managerConnection.sendAction(m_originateAction);
        } catch (IllegalArgumentException iae) {
            m_managerConnection.logoff();
            throw new AsteriskOriginatorException("Illegal argument sending originate action", iae);
        } catch (IllegalStateException ise) {
            m_managerConnection.logoff();
            throw new AsteriskOriginatorException("Illegal state sending originate action", ise);
        } catch (IOException ioe) {
            m_managerConnection.logoff();
            throw new AsteriskOriginatorException("I/O exception sending originate action", ioe);
        } catch (TimeoutException toe) {
            m_managerConnection.logoff();
            throw new AsteriskOriginatorException("Timed out sending originate action", toe);
        }
        log().info("Asterisk manager responded: " + m_managerResponse.getResponse());
        log().info("Asterisk manager message: " + m_managerResponse.getMessage());
        if (m_managerResponse.getResponse().toLowerCase().startsWith("error")) {
            m_managerConnection.logoff();
            throw new AsteriskOriginatorException("Got error response sending originate event. Response: " + m_managerResponse.getResponse() + "; Message: " + m_managerResponse.getMessage());
        }
        log().info("Logging off Asterisk manager connection");
        m_managerConnection.logoff();
        log().info("Successfully logged off Asterisk manager connection");
    }

    /**
     * Build a complete OriginateAction ready for dispatching.
     * 
     * @return completed OriginateAction, ready to be passed to ManagerConnection.sendAction
     * @throws AsteriskOriginatorException if any of the underlying operations fail
     */
    public OriginateAction buildOriginateAction() throws AsteriskOriginatorException {
        OriginateAction action = new OriginateAction();
        action.setCallerId(m_callerId);
        setLegAChannel(expandPattern(m_legAChannelPattern));
        action.setChannel(getLegAChannel());
        action.setTimeout(m_dialTimeout);
        action.setCallerId(m_callerId);
        if (m_legBIsApp) {
            action.setApplication(expandPattern(m_legBAppPattern));
            action.setData(expandPattern(m_legBAppDataPattern));
        } else {
            action.setContext(m_legBContext);
            action.setExten(m_legBExtension);
            action.setPriority(m_legBPriority);
        }
        action.setVariables(m_channelVars);
        return action;
    }

    private String expandPattern(String pattern) {
        log().debug("Expanding pattern " + pattern);
        String expanded = AsteriskUtils.expandPattern(pattern);
        Properties props = new Properties();
        props.put("exten", getLegAExtension());
        expanded = PropertiesUtils.substitute(expanded, props);
        log().debug("Expanded pattern is: " + expanded);
        return expanded;
    }

    /**
     * @return
     */
    private String createCallLogMsg() {
        StringBuffer sb = new StringBuffer();
        sb.append("\n\tChannel: ");
        sb.append(getLegAChannel());
        sb.append("\n\tFrom Caller-ID: ");
        sb.append(getCallerId());
        if (m_legBIsApp) {
            sb.append("\n\tConnect to application: ");
            sb.append(m_legBApp);
            sb.append("\n\tApplication Data: ");
            sb.append(m_legBAppData);
        } else {
            sb.append("\n\tConnect to extension: ");
            sb.append(m_legBExtension);
            sb.append("\n\tIn context: ");
            sb.append(m_legBContext);
            sb.append("\n\tStarting at priority: ");
            sb.append(m_legBPriority);
        }
        sb.append("\n\tSubject is: ");
        sb.append(getSubject());
        sb.append("\n\n");
        sb.append(getMessageText());
        sb.append("\n");
        return sb.toString();
    }

    /**
     * @return Returns the Caller ID
     */
    public String getCallerId() {
        return m_callerId;
    }

    /**
     * @param from
     *            The from address to set.
     */
    public void setCallerId(String cid) {
        m_callerId = cid;
    }

    /**
     * @return Returns the AMI host.
     */
    public String getAmiHost() {
        return m_amiHost.getHostAddress();
    }

    /**
     * @param amiHost
     *            Sets the mail host.
     * @throws UnknownHostException 
     */
    public void setAmiHost(String amiHost) throws UnknownHostException {
        m_amiHost = InetAddress.getByName(amiHost);
    }

    /**
     * @return Returns the message text.
     */
    public String getMessageText() {
        return m_messageText;
    }

    /**
     * @param messageText
     *            Sets the message text.
     */
    public void setMessageText(String messageText) {
        m_messageText = messageText;
    }

    /**
     * @return Returns the message Subject.
     */
    public String getSubject() {
        return m_subject;
    }

    /**
     * @param subject
     *            Sets the message Subject.
     */
    public void setSubject(String subject) {
        m_subject = subject;
    }

    /**
     * @return Returns the extension for Leg A
     */
    public String getLegAExtension() {
        return m_legAExtension;
    }

    /**
     * @param exten
     *            Sets the extension for Leg A
     */
    public void setLegAExtension(String exten) {
        m_legAExtension = exten;
    }

    /**
     * @return Returns the channel for Leg A
     */
    public String getLegAChannel() {
        return m_legAChannel;
    }

    /**
     * @param exten
     *            Sets the channelfor Leg A
     */
    public void setLegAChannel(String chan) {
        m_legAChannel = chan;
    }

    public boolean isDebug() {
        return m_debug;
    }

    public void setDebug(boolean debug) {
        m_debug = debug;
    }

    /**
     * @return log4j Category
     */
    private Category log() {
        return ThreadCategory.getInstance(getClass());
    }

    /**
     * This returns the properties configured in the asterisk-configuration.properties file.
     * @return
     */
    public Properties getAmiProps() {
        return m_amiProps;
    }

    /**
     * Sets a variable on the channel used for the originated call
     * @param name
     *      Name of variable to set
     * @param value
     *      Value to set for variable
     */
    public void setChannelVariable(String name, String value) {
        m_channelVars.put(name, value);
    }

    /**
     * Retrieves a Map of channel variables for the originated call
     * @return A Map of channel variable names and values
     */
    public Map<String, String> getChannelVariables() {
        return m_channelVars;
    }

    /**
     * Retrieves a named channel variable for the originated call
     * @param name
     *      Name of variable to retrieve
     * @return Value of named variable
     */
    public String getChannelVariable(String name) {
        if (name == null || "".equals(name)) {
            return null;
        }
        return m_channelVars.get(name);
    }
}
