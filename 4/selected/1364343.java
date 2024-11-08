package org.xaware.server.engine.channel.jms;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.jms.JMSException;
import org.springframework.jms.connection.SingleConnectionFactory;
import org.springframework.jms.connection.SingleConnectionFactory102;
import org.springframework.jms.connection.UserCredentialsConnectionFactoryAdapter;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.JmsTemplate102;
import org.springframework.jms.support.destination.DestinationResolver;
import org.xaware.server.engine.IBizDriver;
import org.xaware.server.engine.IChannelKey;
import org.xaware.server.engine.ITransactionContext;
import org.xaware.server.engine.ITransactionalChannel;
import org.xaware.server.engine.channel.IChannelPoolManager;
import org.xaware.shared.util.XAwareException;
import org.xaware.shared.util.logging.XAwareLogger;

/**
 * This class implements a way to create and retrieve a JmsTemplate that will have a pooled jms connection behind it.
 * 
 * @author Tim Ferguson
 */
public class JmsTemplateFactory implements IJmsTemplateFactory, IChannelPoolManager {

    protected XAwareLogger lf = null;

    protected String className = this.getClass().getName();

    protected Map<IChannelKey, JmsConnectionFactoryHolder> dynamicConnectionFactories = new HashMap<IChannelKey, JmsConnectionFactoryHolder>();

    /**
     * The constructor initializes the Template factory
     */
    public JmsTemplateFactory() {
        lf = XAwareLogger.getXAwareLogger(className);
    }

    public JmsTemplateHolder getJmsTemplate(final IBizDriver bizDriver, final ITransactionContext transactionContext) throws JMSException, InstantiationException, IllegalAccessException, ClassNotFoundException, XAwareException {
        final IChannelKey key = bizDriver.getChannelSpecificationKey();
        JmsTemplateHolder jmsTemplateHolder = (JmsTemplateHolder) transactionContext.getTransactionalChannel(key);
        if (jmsTemplateHolder != null) {
            return jmsTemplateHolder;
        }
        JmsConnectionFactoryHolder cfh = this.getDynamicConnectionFactory(key);
        if (cfh == null) {
            cfh = (JmsConnectionFactoryHolder) bizDriver.createChannelObject();
            this.dynamicConnectionFactories.put(key, cfh);
        }
        UserCredentialsConnectionFactoryAdapter ucConnectionFactoryAdapter = new UserCredentialsConnectionFactoryAdapter();
        ucConnectionFactoryAdapter.setTargetConnectionFactory(cfh.getConnectionFactory());
        ucConnectionFactoryAdapter.setUsername(cfh.getJmsUser());
        ucConnectionFactoryAdapter.setPassword(cfh.getJmsPassword());
        JmsTemplate embeddedJmsTemplate = null;
        if (cfh.isJms102()) {
            final SingleConnectionFactory102 scf = new SingleConnectionFactory102(ucConnectionFactoryAdapter, false);
            embeddedJmsTemplate = new JmsTemplate102(scf, false);
        } else {
            final SingleConnectionFactory scf = new SingleConnectionFactory(ucConnectionFactoryAdapter.createConnection());
            embeddedJmsTemplate = new JmsTemplate(scf);
        }
        DestinationResolver dr = cfh.getDestionationResolver();
        if (dr != null) {
            embeddedJmsTemplate.setDestinationResolver(dr);
        }
        jmsTemplateHolder = new JmsTemplateHolder(embeddedJmsTemplate);
        transactionContext.setTransactionalChannel(key, jmsTemplateHolder);
        return jmsTemplateHolder;
    }

    private JmsConnectionFactoryHolder getDynamicConnectionFactory(final IChannelKey key) {
        if (this.dynamicConnectionFactories != null) {
            return this.dynamicConnectionFactories.get(key);
        }
        return null;
    }

    public void clearPool(final String poolName) {
        if (getEntryCount() == 0) {
            return;
        }
        final JmsConnectionFactoryHolder cFactory = this.dynamicConnectionFactories.get(poolName);
        if (cFactory != null) {
            cFactory.getConnectionFactory();
            this.dynamicConnectionFactories.remove(poolName);
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
        if (dynamicConnectionFactories == null) {
            return 0;
        }
        return this.dynamicConnectionFactories.size();
    }

    public String getPoolManagerName() {
        return "JmsTemplateFactory";
    }

    public String[] getPoolNames() {
        if (getEntryCount() == 0) {
            return new String[0];
        }
        final Set<IChannelKey> keys = this.dynamicConnectionFactories.keySet();
        final String[] keyArray = new String[keys.size()];
        int i = 0;
        for (IChannelKey key : keys) {
            keyArray[i] = key.toString();
            i++;
        }
        return keyArray;
    }
}
