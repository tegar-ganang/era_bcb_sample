package org.hibernate.transaction;

import java.util.Properties;
import javax.transaction.TransactionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hibernate.HibernateException;
import org.hibernate.cfg.Environment;
import org.hibernate.util.ReflectHelper;

/**
 * Helper for generating {@link TransactionManagerLookup} instances.
 *
 * @author Gavin King
 */
public final class TransactionManagerLookupFactory {

    private static final Logger log = LoggerFactory.getLogger(TransactionManagerLookupFactory.class);

    /**
	 * Disallow instantiation
	 */
    private TransactionManagerLookupFactory() {
    }

    /**
	 * Convenience method for locating the JTA {@link TransactionManager} from the
	 * given platform config.
	 * <p/>
	 * Same as calling {@link #getTransactionManager}.getTransactionManager( props )
	 *
	 * @param props The properties representing the platform config
	 * @return The located {@link TransactionManager}
	 * @throws HibernateException Indicates a problem either (a) generatng the
	 * {@link TransactionManagerLookup} or (b) asking it to locate the {@link TransactionManager}.
	 */
    public static TransactionManager getTransactionManager(Properties props) throws HibernateException {
        log.info("obtaining TransactionManager");
        return getTransactionManagerLookup(props).getTransactionManager(props);
    }

    /**
	 * Generate the appropriate {@link TransactionManagerLookup} given the
	 * config settings being passed.
	 *
	 * @param props The config settings
	 * @return The appropriate {@link TransactionManagerLookup}
	 * @throws HibernateException Indicates problem generating {@link TransactionManagerLookup}
	 */
    public static TransactionManagerLookup getTransactionManagerLookup(Properties props) throws HibernateException {
        String tmLookupClass = props.getProperty(Environment.TRANSACTION_MANAGER_STRATEGY);
        if (tmLookupClass == null) {
            log.info("No TransactionManagerLookup configured (in JTA environment, use of read-write or transactional second-level cache is not recommended)");
            return null;
        } else {
            log.info("instantiating TransactionManagerLookup: " + tmLookupClass);
            try {
                TransactionManagerLookup lookup = (TransactionManagerLookup) ReflectHelper.classForName(tmLookupClass).newInstance();
                log.info("instantiated TransactionManagerLookup");
                return lookup;
            } catch (Exception e) {
                log.error("Could not instantiate TransactionManagerLookup", e);
                throw new HibernateException("Could not instantiate TransactionManagerLookup '" + tmLookupClass + "'");
            }
        }
    }
}
