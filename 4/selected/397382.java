package org.springframework.jmx.access;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.net.BindException;
import java.util.HashMap;
import java.util.Map;
import javax.management.Descriptor;
import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import org.springframework.core.JdkVersion;
import org.springframework.jmx.AbstractJmxTests;
import org.springframework.jmx.IJmxTestBean;
import org.springframework.jmx.JmxTestBean;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.jmx.export.assembler.AbstractReflectiveMBeanInfoAssembler;

/**
 * @author Rob Harrop
 */
public class MBeanClientInterceptorTests extends AbstractJmxTests {

    protected static final String OBJECT_NAME = "spring:test=proxy";

    protected JmxTestBean target;

    protected boolean runTests = true;

    public void setUp() throws Exception {
        super.setUp();
        target = new JmxTestBean();
        target.setAge(100);
        target.setName("Rob Harrop");
        MBeanExporter adapter = new MBeanExporter();
        Map beans = new HashMap();
        beans.put(OBJECT_NAME, target);
        adapter.setServer(server);
        adapter.setBeans(beans);
        adapter.setAssembler(new ProxyTestAssembler());
        adapter.afterPropertiesSet();
    }

    protected MBeanServerConnection getServerConnection() throws Exception {
        return server;
    }

    protected IJmxTestBean getProxy() throws Exception {
        MBeanProxyFactoryBean factory = new MBeanProxyFactoryBean();
        factory.setServer(getServerConnection());
        factory.setProxyInterface(IJmxTestBean.class);
        factory.setObjectName(OBJECT_NAME);
        factory.afterPropertiesSet();
        return (IJmxTestBean) factory.getObject();
    }

    public void testProxyClassIsDifferent() throws Exception {
        if (!runTests) return;
        IJmxTestBean proxy = getProxy();
        assertTrue("The proxy class should be different than the base class", (proxy.getClass() != IJmxTestBean.class));
    }

    public void testDifferentProxiesSameClass() throws Exception {
        if (!runTests) return;
        IJmxTestBean proxy1 = getProxy();
        IJmxTestBean proxy2 = getProxy();
        assertNotSame("The proxies should NOT be the same", proxy1, proxy2);
        assertSame("The proxy classes should be the same", proxy1.getClass(), proxy2.getClass());
    }

    public void testGetAttributeValue() throws Exception {
        if (!runTests) return;
        IJmxTestBean proxy1 = getProxy();
        int age = proxy1.getAge();
        assertEquals("The age should be 100", 100, age);
    }

    public void testSetAttributeValue() throws Exception {
        if (!runTests) return;
        IJmxTestBean proxy = getProxy();
        proxy.setName("Rob Harrop");
        assertEquals("The name of the bean should have been updated", "Rob Harrop", target.getName());
    }

    public void testSetReadOnlyAttribute() throws Exception {
        if (!runTests) return;
        IJmxTestBean proxy = getProxy();
        try {
            proxy.setAge(900);
            fail("Should not be able to write to a read only attribute");
        } catch (InvalidInvocationException ex) {
        }
    }

    public void testInvokeNoArgs() throws Exception {
        if (!runTests) return;
        IJmxTestBean proxy = getProxy();
        long result = proxy.myOperation();
        assertEquals("The operation should return 1", 1, result);
    }

    public void testInvokeArgs() throws Exception {
        if (!runTests) return;
        IJmxTestBean proxy = getProxy();
        int result = proxy.add(1, 2);
        assertEquals("The operation should return 3", 3, result);
    }

    public void testInvokeUnexposedMethodWithException() throws Exception {
        if (!runTests) return;
        IJmxTestBean bean = getProxy();
        try {
            bean.dontExposeMe();
            fail("Method dontExposeMe should throw an exception");
        } catch (InvalidInvocationException desired) {
        }
    }

    public void testLazyConnectionToRemote() throws Exception {
        if (!runTests) return;
        if (JdkVersion.getMajorJavaVersion() < JdkVersion.JAVA_14) {
            return;
        }
        JMXServiceURL url = new JMXServiceURL("service:jmx:jmxmp://localhost:9876");
        JMXConnectorServer connector = JMXConnectorServerFactory.newJMXConnectorServer(url, null, server);
        MBeanProxyFactoryBean factory = new MBeanProxyFactoryBean();
        factory.setServiceUrl(url.toString());
        factory.setProxyInterface(IJmxTestBean.class);
        factory.setObjectName(OBJECT_NAME);
        factory.setConnectOnStartup(false);
        factory.afterPropertiesSet();
        try {
            connector.start();
        } catch (BindException ex) {
            System.out.println("Skipping JMX LazyConnectionToRemote test because binding to local port 9876 failed: " + ex.getMessage());
            return;
        }
        try {
            IJmxTestBean bean = (IJmxTestBean) factory.getObject();
            assertEquals("Rob Harrop", bean.getName());
            assertEquals(100, bean.getAge());
        } finally {
            connector.stop();
        }
    }

    private static class ProxyTestAssembler extends AbstractReflectiveMBeanInfoAssembler {

        protected boolean includeReadAttribute(Method method, String beanKey) {
            return true;
        }

        protected boolean includeWriteAttribute(Method method, String beanKey) {
            if ("setAge".equals(method.getName())) {
                return false;
            }
            return true;
        }

        protected boolean includeOperation(Method method, String beanKey) {
            if ("dontExposeMe".equals(method.getName())) {
                return false;
            }
            return true;
        }

        protected String getOperationDescription(Method method) {
            return method.getName();
        }

        protected String getAttributeDescription(PropertyDescriptor propertyDescriptor) {
            return propertyDescriptor.getDisplayName();
        }

        protected void populateAttributeDescriptor(Descriptor descriptor, Method getter, Method setter) {
        }

        protected void populateOperationDescriptor(Descriptor descriptor, Method method) {
        }

        protected String getDescription(String beanKey, Class beanClass) {
            return "";
        }

        protected void populateMBeanDescriptor(Descriptor mbeanDescriptor, String beanKey, Class beanClass) {
        }
    }
}
