package com.pentagaia.tb.start.testbase;

import java.util.Properties;
import org.junit.Assert;
import org.junit.Test;
import com.pentagaia.tb.start.api.IPdsKernelProvider;
import com.pentagaia.tb.start.testbase.tools.AppContextKernelProvider;
import com.pentagaia.tb.start.testbase.tools.ListenerKernelProvider;
import com.pentagaia.tb.start.testbase.tools.PropertyParserKernelProvider;
import com.pentagaia.tb.start.testbase.tools.SimpleManager;
import com.pentagaia.tb.start.testbase.tools.SimpleService;
import com.pentagaia.tb.start.testbase.tools.TransactionalRunnable;
import com.pentagaia.tb.start.testbase.tools.TransactionalKernelProvider;

/**
 * An abstract kernel test case to test the project start.
 * 
 * You whould not inherit this class. To implement your own tests use the base class {@code AbstractTestCase}
 * 
 * @author mepeisen
 * @version 0.1.0
 * @since 0.1.0
 */
public abstract class AbstractKernelTestCase extends AbstractTestCase {

    /**
     * Tests a normal kernel startup
     */
    @Test
    public void testNormalStartup() {
        this.generateKernel(this.getSimpleConfig());
        assertRunning();
        assertCleanShutdown();
    }

    /**
     * Tests if the kernel detects missing attributes
     */
    @Test(expected = IllegalStateException.class)
    public void testMissingAttributes1() {
        final Properties appProperties = new Properties();
        appProperties.put(APP_NAME, "jUnit-TestCase");
        appProperties.put(APP_PORT, "19990");
        appProperties.put(APP_ROOT, "foo");
        final Properties sysProperties = new Properties(System.getProperties());
        this.generateKernel(new ConfigImpl(appProperties, sysProperties, "", null));
        Assert.assertFalse("The kernel should detectd missing attribute APP_LISTENER", true);
    }

    /**
     * Tests if the kernel detects missing attributes
     */
    @Test(expected = IllegalStateException.class)
    public void testMissingAttributes2() {
        final Properties appProperties = new Properties();
        appProperties.put(APP_NAME, "jUnit-TestCase");
        appProperties.put(APP_PORT, "19990");
        appProperties.put(APP_LISTENER, APP_LISTENER_NONE);
        final Properties sysProperties = new Properties(System.getProperties());
        this.generateKernel(new ConfigImpl(appProperties, sysProperties, "", null));
        Assert.assertFalse("The kernel should detectd missing attribute APP_ROOT", true);
    }

    /**
     * Tests if the kernel detects missing attributes
     */
    @Test(expected = IllegalStateException.class)
    public void testMissingAttributes3() {
        final Properties appProperties = new Properties();
        appProperties.put(APP_NAME, "jUnit-TestCase");
        appProperties.put(APP_ROOT, "foo");
        appProperties.put(APP_LISTENER, APP_LISTENER_NONE);
        final Properties sysProperties = new Properties(System.getProperties());
        this.generateKernel(new ConfigImpl(appProperties, sysProperties, "", null));
        Assert.assertFalse("The kernel should detectd missing attribute APP_PORT", true);
    }

    /**
     * Tests if the kernel detects missing attributes
     */
    @Test(expected = IllegalStateException.class)
    public void testMissingAttributes4() {
        final Properties appProperties = new Properties();
        appProperties.put(APP_PORT, "19990");
        appProperties.put(APP_ROOT, "foo");
        appProperties.put(APP_LISTENER, APP_LISTENER_NONE);
        final Properties sysProperties = new Properties(System.getProperties());
        this.generateKernel(new ConfigImpl(appProperties, sysProperties, "", null));
        Assert.assertFalse("The kernel should detectd missing attribute APP_NAME", true);
    }

    /**
     * Tests a simple kernel listener
     */
    @Test
    public void testKernelListeners() {
        final ListenerKernelProvider provider = new ListenerKernelProvider();
        this.generateKernel(this.getSimpleConfig(), new IPdsKernelProvider[] { provider });
        this.assertRunning();
        this.assertCleanShutdown();
        Assert.assertTrue("The kernel must call onAppStarted", provider.isAppStarted());
        Assert.assertTrue("The kernel must call onStartup", provider.isStartup());
        Assert.assertTrue("The kernel must call onPreShutdown", provider.isPreShutdown());
    }

    /**
     * Tests a simple app kernel app context
     */
    @Test
    public void testAppKernelAppContext() {
        final AppContextKernelProvider provider = new AppContextKernelProvider();
        this.generateKernel(this.getSimpleConfig(), new IPdsKernelProvider[] { provider });
        this.assertRunning();
        Assert.assertNotNull("Data Manager must not be null", this.testKernel.getAppKernelAppContext().getDataManager());
        Assert.assertNotNull("Task Manager must not be null", this.testKernel.getAppKernelAppContext().getTaskManager());
        Assert.assertNotNull("Channel Manager must not be null", this.testKernel.getAppKernelAppContext().getChannelManager());
        Assert.assertNull("Random manager must be null", this.testKernel.getAppKernelAppContext().getManager(SimpleManager.class));
        Assert.assertNull("Random service must be null", this.testKernel.getAppKernelAppContext().getService(SimpleService.class));
        this.assertCleanShutdown();
        Assert.assertTrue("get channel manager must be called", provider.getChannelManagerCalled);
        Assert.assertTrue("get data manager must be called", provider.getDataManagerCalled);
        Assert.assertTrue("get task manager must be called", provider.getTaskManagerCalled);
        Assert.assertTrue("get manager must be called", provider.getManagerCalled);
        Assert.assertTrue("get service must be called", provider.getServiceCalled);
        Assert.assertTrue("shutdown services manager must be called", provider.shutdownCalled);
    }

    /**
     * Tests a class loader extension
     */
    @Test
    @org.junit.Ignore
    public void testClassLoaderExtension() {
        Assert.assertFalse("not implemented", true);
    }

    /**
     * Tests a simple property parser
     */
    @Test
    public void testPropertyParsing() {
        final PropertyParserKernelProvider provider = new PropertyParserKernelProvider();
        this.generateKernel(this.getSimpleConfig(), new IPdsKernelProvider[] { provider });
        this.assertRunning();
        boolean managerActive = false;
        boolean serviceActive = false;
        try {
            final Class<?> managerClazz = this.testKernel.getClass().getClassLoader().loadClass(SimpleManager.class.getName());
            final Class<?> serviceClazz = this.testKernel.getClass().getClassLoader().loadClass(SimpleService.class.getName());
            managerActive = this.testKernel.getAppKernelAppContext().getManager(managerClazz) != null;
            serviceActive = this.testKernel.getAppKernelAppContext().getService(serviceClazz) != null;
        } catch (Exception e) {
            Assert.fail("Receiving manager/service failed: " + e.getClass().getName() + " / " + e.getMessage());
        }
        this.assertCleanShutdown();
        Assert.assertTrue("The manager must not be null", managerActive);
        Assert.assertTrue("The service must not be null", serviceActive);
    }

    /**
     * Tests a simple transactional test case
     */
    @Test
    public void testTransactionally() {
        final TransactionalKernelProvider provider = new TransactionalKernelProvider();
        this.generateKernel(this.getSimpleConfig(), new IPdsKernelProvider[] { provider });
        this.assertRunning();
        TransactionalRunnable.CALLED = false;
        this.runTest(TransactionalRunnable.class, 500);
        final boolean testCalled = TransactionalRunnable.CALLED;
        TransactionalRunnable.CALLED = false;
        this.runTestTransactionally(TransactionalRunnable.class, 500);
        final boolean testTransactionallyCalled = TransactionalRunnable.CALLED;
        this.assertCleanShutdown();
        Assert.assertTrue("The test method must invoke the runnable", testCalled);
        Assert.assertTrue("The transactional test method must invoke the runnable", testTransactionallyCalled);
    }

    /**
     * Tests if the kernel respects the kernel provider white list
     */
    @Test
    @org.junit.Ignore
    public void testKernelProviderWhitelist() {
        Assert.assertFalse("not implemented", true);
    }
}
