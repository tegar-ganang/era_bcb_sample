package org.xaware.server.engine.channel;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;
import org.springframework.jndi.JndiAccessor;
import org.xaware.server.engine.IChannelKey;
import org.xaware.shared.util.logging.XAwareLogger;

/**
 * This class represents the key to uniquely identify a local Channel instance.
 * This implementation simply wraps a String as the backing datastore for the key.
 * 
 * @author Tim Uttormark
 */
public class JndiChannelKey implements IChannelKey {

    private static final XAwareLogger lf = XAwareLogger.getXAwareLogger(JndiChannelKey.class.getName());

    private static final String CLASS_NAME = "JndiChannelKey";

    /**
     * IChannelKey type
     */
    private static final Type TYPE = IChannelKey.Type.JNDI;

    /**
     * The naming context used to access the UserTransaction and the
     * SQL DataSource or JMS ConnectionFactory
     */
    private final JndiAccessor jndiAccessor;

    /**
     * The name (including relative path) of the BizDriver file.  Use of a different
     * bizDriverName indicates that a different channel instance is desired, even
     * when all other channel specification parameters match.
     */
    private final String bizDriverName;

    /**
     * The JNDI name of the SQL DataSource or JMS ConnectionFactory
     */
    private final String channelFactoryName;

    /**
     * The JNDI name of the UserTransaction.  If null, the standard name of
     * "java:comp/UserTransaction" is used.
     */
    private String userTransactionName;

    /**
     * The JNDI name of the JTA TransactionManager.  If null, this list of common
     * names is used: 
     *   -- "java:comp/UserTransaction" (in some app servers, one object implements both
     *                                   UserTransaction and TransactionManager interfaces).
     *   -- "java:comp/TransactionManager"
     *   -- "java:pm/TransactionManager"
     *   -- "java:/TransactionManager"
     *   
     * If null and none of these JNDI names resolve, then processing will continue, but
     * transaction suspension and resumption will not be supported.
     */
    private String transactionManagerName;

    /**
     * The hashCode value for this object, cached to avoid the expense of recalculating it
     */
    private Integer cachedHashCode = null;

    private UserTransaction userTransaction = null;

    private TransactionManager transactionManager = null;

    /**
     * Construct a new instance.
     * 
     * @param jndiAccessor
     * @param bizDriverName
     * @param channelFactoryName
     * @param userTransactionName
     * @param transactionManagerName
     */
    public JndiChannelKey(final JndiAccessor jndiAccessor, final String bizDriverName, final String channelFactoryName, final String userTransactionName, final String transactionManagerName) {
        if (jndiAccessor == null) {
            throw new IllegalArgumentException("jndiAccessor may not be null");
        }
        if (bizDriverName == null) {
            throw new IllegalArgumentException("bizDriverName may not be null");
        }
        this.jndiAccessor = jndiAccessor;
        this.bizDriverName = bizDriverName;
        this.channelFactoryName = (channelFactoryName == null) ? "" : channelFactoryName;
        this.userTransactionName = (userTransactionName == null) ? "" : userTransactionName;
        this.transactionManagerName = (transactionManagerName == null) ? "" : transactionManagerName;
    }

    /**
     * Get the channel key type for this channel key instance.
     * 
     * @return the IChannelKey.Type for this IChannelKey instance.
     */
    public Type getChannelKeyType() {
        return TYPE;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof JndiChannelKey)) {
            return false;
        }
        JndiChannelKey that = (JndiChannelKey) obj;
        if (!this.bizDriverName.equals(that.bizDriverName) || !this.channelFactoryName.equals(that.channelFactoryName) || !equalsOrBothNull(this.userTransactionName, that.userTransactionName) || !equalsOrBothNull(this.transactionManagerName, that.transactionManagerName)) {
            lf.debug("JndiChannelKeys not equal", CLASS_NAME, "equals");
            return false;
        }
        boolean result = providerUrlsEqual(this, that);
        if (result) {
            lf.debug("JndiChannelKeys equal", CLASS_NAME, "equals");
        } else {
            lf.debug("JndiChannelKeys not equal", CLASS_NAME, "equals");
        }
        return result;
    }

    /**
     * Gets the string form of the ProviderURL of the destination of the JNDI
     * naming Context.
     * 
     * @return the ProviderURL String
     */
    private String getProviderUrl() {
        Properties jndiEnvironment = this.jndiAccessor.getJndiEnvironment();
        if (jndiEnvironment == null) {
            return "";
        }
        return (String) jndiEnvironment.get(Context.PROVIDER_URL);
    }

    /**
     * Determines whether the two provider URLs are considered equal for
     * purposes of determining whether the same destination server is used.
     * 
     * @param key1
     *            the first JndiChannelKey to be compared
     * @param key2
     *            the second JndiChannelKey to be compared
     * @return a boolean indicationg whether the the same destination server is
     *         used in the two keys provided.
     */
    public static boolean providerUrlsEqual(JndiChannelKey key1, JndiChannelKey key2) {
        String key1ProviderUrlStr = key1.getProviderUrl();
        String key2ProviderUrlStr = key2.getProviderUrl();
        URL key1ProviderUrl = null;
        URL key2ProviderUrl = null;
        try {
            key1ProviderUrl = getproviderUrlAsUrl(key1ProviderUrlStr);
            key2ProviderUrl = getproviderUrlAsUrl(key2ProviderUrlStr);
            return equalsOrBothNull(key1ProviderUrl, key2ProviderUrl);
        } catch (MalformedURLException e) {
            if (key1ProviderUrl != null) {
                return false;
            }
            try {
                key2ProviderUrl = getproviderUrlAsUrl(key2ProviderUrlStr);
                return false;
            } catch (MalformedURLException e1) {
                return equalsOrBothNull(key1ProviderUrlStr, key2ProviderUrlStr);
            }
        }
    }

    private static URL getproviderUrlAsUrl(String providerUrlAsString) throws MalformedURLException {
        if (providerUrlAsString == null) {
            return null;
        }
        try {
            return new URL(providerUrlAsString);
        } catch (MalformedURLException e) {
            int index = providerUrlAsString.indexOf("://");
            String providerUrlMinusProtocol = (index == -1) ? providerUrlAsString : providerUrlAsString.substring(index + 3);
            return new URL("http://" + providerUrlMinusProtocol);
        }
    }

    /**
     * Determines whether the Object provided are equal, allowing for either to be null.
     * 
     * @param obj1
     *            the first Object to compare
     * @param obj2
     *            the second Object to compare
     * @return True if both Object are null, or both are non-null and have
     *         equal values.
     */
    private static boolean equalsOrBothNull(Object obj1, Object obj2) {
        if (obj1 == null) {
            return (obj2 == null);
        }
        if (obj2 == null) {
            return false;
        }
        return obj1.equals(obj2);
    }

    /**
     * Returns a hash code value for the object.
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        if (cachedHashCode == null) {
            int userTransactionNameHashCode = (userTransactionName == null) ? 0 : userTransactionName.hashCode();
            int transactionManagerNameHashCode = (transactionManagerName == null) ? 0 : transactionManagerName.hashCode();
            int jndiNamingContextHashCode = 1;
            try {
                jndiNamingContextHashCode = this.getProviderUrl().hashCode();
            } catch (Exception e) {
            }
            cachedHashCode = new Integer(jndiNamingContextHashCode + 37 * (bizDriverName.hashCode() + 13 * (channelFactoryName.hashCode() + 31 * (userTransactionNameHashCode + 17 * transactionManagerNameHashCode))));
        }
        return cachedHashCode.intValue();
    }

    /**
     * Returns a String representation for the object
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(super.toString()).append(" [").append("jndiAccessor = ").append(jndiAccessor).append("; bizDriverName = ").append(bizDriverName).append("; channelFactoryName = ").append(channelFactoryName).append("; userTransactionName = ").append(userTransactionName).append("; transactionManagerName = ").append(transactionManagerName).append("]");
        return buf.toString();
    }

    /**
     * @return the jndiNamingContext
     */
    public JndiAccessor getJndiAccessor() {
        return this.jndiAccessor;
    }

    /**
     * @return the bizDriverName
     */
    public String getBizDriverName() {
        return this.bizDriverName;
    }

    /**
     * @return the channelFactoryName
     */
    public String getChannelFactoryName() {
        return this.channelFactoryName;
    }

    /**
     * @return the userTransactionName
     */
    public String getUserTransactionName() {
        return this.userTransactionName;
    }

    /**
     * @param userTransactionName the userTransactionName to set
     */
    public void setUserTransactionName(String userTransactionName) {
        this.userTransactionName = userTransactionName;
        this.cachedHashCode = null;
    }

    /**
     * @return the transactionManagerName
     */
    public String getTransactionManagerName() {
        return this.transactionManagerName;
    }

    /**
     * @param transactionManagerName the transactionManagerName to set
     */
    public void setTransactionManagerName(String transactionManagerName) {
        this.transactionManagerName = transactionManagerName;
        this.cachedHashCode = null;
    }

    /**
     * @return the userTransaction
     */
    public UserTransaction getUserTransaction() {
        return this.userTransaction;
    }

    /**
     * @param userTransaction the userTransaction to set
     */
    public void setUserTransaction(UserTransaction userTransaction) {
        this.userTransaction = userTransaction;
    }

    /**
     * @return the transactionManager
     */
    public TransactionManager getTransactionManager() {
        return this.transactionManager;
    }

    /**
     * @param transactionManager the transactionManager to set
     */
    public void setTransactionManager(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    private static final String SPACES = "                                                                 ";

    public static String dumpJndiTree(JndiAccessor jndiAccessor) {
        try {
            InitialContext ic = new InitialContext(jndiAccessor.getJndiEnvironment());
            return dumpJndiTree(ic);
        } catch (NamingException e) {
            return "dumpJndiTree failed: " + e;
        }
    }

    public static String dumpJndiTree(Context context) {
        int indent = 0;
        String rootContextName = "";
        StringBuffer buf = new StringBuffer();
        printSubtree(context, rootContextName, indent, buf);
        return buf.toString();
    }

    private static void printSubtree(Context context, String contextName, int indent, StringBuffer buf) {
        try {
            printOneLevel(context, contextName, indent, buf);
        } catch (NamingException e) {
        } catch (RuntimeException e) {
        }
    }

    private static void printOneLevel(Context context, String contextName, int indent, StringBuffer buf) throws NamingException {
        NamingEnumeration<NameClassPair> namingEnumeration = context.list(contextName);
        while (namingEnumeration.hasMoreElements()) {
            NameClassPair ncp = namingEnumeration.nextElement();
            buf.append(SPACES.substring(0, indent)).append(ncp).append('\n');
            indent += 4;
            String subcontextName = ("".equals(contextName)) ? ncp.getName() : contextName + "/" + ncp.getName();
            printSubtree(context, subcontextName, indent, buf);
            indent -= 4;
        }
    }
}
