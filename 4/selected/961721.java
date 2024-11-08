package org.xaware.server.engine.channel.http;

import org.xaware.server.engine.IScopedChannel;
import org.xaware.shared.util.XAHttpClient;
import org.xaware.shared.util.XAwareConstants;
import org.xaware.shared.util.XAwareException;

/**
 * This class implements IScopedChannel for HTTP gets and posts. It allows us to
 * keep a connection open as long as necessary, providing aaccessto the HTTP
 * client.
 * 
 * @author jtarnowski
 */
public class HttpTemplate implements IScopedChannel {

    /** Our Channel object holds properties */
    HttpChannelSpecification channelObject = null;

    /** Don't initialize more than once */
    boolean initialized = false;

    /** The connector we will give out to whoever wants it */
    XAHttpClient connector;

    /**
	 * Constructor you can't call
	 */
    private HttpTemplate() {
    }

    /**
	 * Constructor you must call
	 */
    public HttpTemplate(HttpBizDriver driver) throws XAwareException {
        super();
        channelObject = (HttpChannelSpecification) driver.createChannelObject();
    }

    /**
	 * implement the interface's getScopedChannelType() Get the scoped channel
	 * type for this scoped channel instance.
	 * 
	 * @return the IScopedChannel.Type for this IScopedChannel instance.
	 */
    public Type getScopedChannelType() {
        return Type.HTTP;
    }

    /**
	 * implement the interface's close() - close open stream
	 * 
	 */
    public void close(boolean success) {
        if (connector != null) {
            connector.close();
        }
        initialized = false;
    }

    /**
	 * Initialize ourself
	 * 
	 * @throws XAwareException
	 */
    public void initResources() throws XAwareException {
        if (!initialized) {
            String url = channelObject.getProperty(XAwareConstants.BIZDRIVER_URL);
            if (url == null) {
                url = channelObject.getProperty(XAwareConstants.BIZCOMPONENT_ATTR_BASE_URL);
            }
            if (url == null) {
                throw new XAwareException("xa:url or xa:base_url must be specified");
            }
            connector = new XAHttpClient(url);
            connector.setUrl(url);
            String uid = channelObject.getProperty(XAwareConstants.BIZDRIVER_USER);
            if (uid == null) {
                uid = "";
            }
            String pwd = channelObject.getProperty(XAwareConstants.BIZDRIVER_PWD);
            if (pwd == null) {
                pwd = "";
            }
            connector.init(uid, pwd);
            try {
                connector.setConnectionHost(url);
            } catch (Exception e) {
                throw new XAwareException(e.getLocalizedMessage());
            }
            String proxyHost = channelObject.getProperty(XAwareConstants.BIZCOMPONENT_ATTR_PROXYHOST);
            if (proxyHost != null && proxyHost.length() > 0) {
                String proxyPort = channelObject.getProperty(XAwareConstants.BIZCOMPONENT_ATTR_PROXYPORT);
                if (proxyPort != null && proxyPort.length() > 0) {
                    connector.setProxySet(true);
                    connector.setProxyHost(proxyHost);
                    connector.setProxyPort(proxyPort);
                }
            }
            String proxyUser = channelObject.getProperty(XAwareConstants.BIZCOMPONENT_ATTR_PROXYUSER);
            if (proxyUser != null && proxyUser.length() > 0) {
                String proxyPwd = channelObject.getProperty(XAwareConstants.BIZCOMPONENT_ATTR_PROXYPASSWORD);
                if (proxyPwd != null && proxyPwd.length() > 0) {
                    connector.setProxyAuthentication(true);
                    connector.setProxyUser(proxyUser);
                    connector.setProxyPwd(proxyPwd);
                }
            }
            initialized = true;
        }
    }

    /**
     * @return the channelObject
     */
    public HttpChannelSpecification getChannelObject() {
        return channelObject;
    }

    /**
     * @return the connector
     */
    public XAHttpClient getConnector() throws XAwareException {
        if (!initialized) {
            initResources();
        }
        return connector;
    }
}
