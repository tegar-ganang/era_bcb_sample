package org.hibernate.cache;

import junit.framework.Test;
import org.hibernate.cfg.Environment;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.test.cache.BaseCacheProviderTestCase;

/**
 * @author Emmanuel Bernard
 */
public class EhCacheTest extends BaseCacheProviderTestCase {

    public EhCacheTest(String x) {
        super(x);
    }

    public static Test suite() {
        return new FunctionalTestClassTestSuite(EhCacheTest.class);
    }

    public String getCacheConcurrencyStrategy() {
        return "read-write";
    }

    protected Class getCacheProvider() {
        return EhCacheProvider.class;
    }

    protected String getConfigResourceKey() {
        return Environment.CACHE_PROVIDER_CONFIG;
    }

    protected String getConfigResourceLocation() {
        return "ehcache.xml";
    }

    protected boolean useTransactionManager() {
        return false;
    }
}
