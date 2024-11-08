package org.xaware.server.engine.channel.ldap;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.naming.NamingException;
import org.springframework.ldap.core.ContextSource;
import org.xaware.server.engine.IBizDriver;
import org.xaware.server.engine.IChannelKey;
import org.xaware.server.engine.ITransactionContext;
import org.xaware.server.engine.channel.IChannelPoolManager;
import org.xaware.shared.util.XAwareException;
import org.xaware.shared.util.logging.XAwareLogger;

/**
 * This class implements a way to create and retrieve a LDAPTemplate that will
 * have a pooled connection behind it.
 * 
 * @author Vasu Thadaka
 */
public class LDAPTemplateFactory implements ILDAPTemplateFactory, IChannelPoolManager {

    /** Class Name */
    private static final String className = LDAPTemplateFactory.class.getName();

    /** Logger for LDAPTemplateFactory */
    private static final XAwareLogger logger = XAwareLogger.getXAwareLogger(className);

    /**
	 * Currently the idea is that this class is made a singleton by Spring
	 * therefore the contextSourcePool is not a static member.
	 */
    private Map<IChannelKey, ContextSource> contextSourcePool = new HashMap<IChannelKey, ContextSource>();

    public LDAPTemplate getLDAPTemplate(IBizDriver bizDriver, ITransactionContext transactionContext) throws XAwareException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        final IChannelKey key = bizDriver.getChannelSpecificationKey();
        LDAPTemplate ldapTemplate = null;
        ContextSource contextSource = contextSourcePool.get(key);
        if (contextSource == null) {
            contextSource = (ContextSource) bizDriver.createChannelObject();
            contextSourcePool.put(key, contextSource);
        }
        ldapTemplate = new LDAPTemplate(contextSource, null);
        return ldapTemplate;
    }

    public void clearPool(final String poolName) {
        if (getEntryCount() == 0) {
            return;
        }
        final ContextSource contextSource = contextSourcePool.get(poolName);
        if (contextSource != null) {
            try {
                contextSource.getReadWriteContext().close();
            } catch (NamingException e) {
                logger.debug(e);
            }
            contextSourcePool.remove(poolName);
        }
    }

    public void clearPools() {
        if (getEntryCount() == 0) {
            return;
        }
        final String[] keys = getPoolNames();
        for (String key : keys) {
            clearPool(key);
        }
    }

    public int getEntryCount() {
        if (contextSourcePool == null) {
            return 0;
        }
        return contextSourcePool.size();
    }

    public String getPoolManagerName() {
        return "LDAPTemplateFactory";
    }

    public String[] getPoolNames() {
        if (getEntryCount() == 0) {
            return new String[0];
        }
        final Set<IChannelKey> keys = contextSourcePool.keySet();
        final String[] keyArray = new String[keys.size()];
        int i = 0;
        for (IChannelKey key : this.contextSourcePool.keySet()) {
            keyArray[i] = key.toString();
            i++;
        }
        return keyArray;
    }
}
