package org.xaware.salesforce.bizcomp.channel;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.xaware.server.engine.context.BizDriverContext;
import org.xaware.server.engine.exceptions.XAwareConfigurationException;
import org.xaware.server.resources.BizDriverFactory;
import org.xaware.server.resources.XAwareBeanFactory;
import org.xaware.shared.util.ExceptionMessageHelper;
import org.xaware.shared.util.XAwareConstants;
import org.xaware.testing.functoids.TestingFunctoids;
import org.xaware.testing.util.BaseTestCase;

/**
 * @author tferguson
 * 
 * This test case is for testing the Salesforce Biz Driver.  In at least one of the tests it will try
 * to make a connection to salesforce as long as the following
 * properties are setup in your buildoptions.properties.
 *      <li>salesforce.user</li>
 *      <li>salesforce.password</li>
 *      <li>salesforce.timeout</li>
 * 
 */
public class SalesForceBizDriverTestCase extends BaseTestCase {

    private BizDriverFactory bdFactory;

    public SalesForceBizDriverTestCase(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        bdFactory = (BizDriverFactory) XAwareBeanFactory.getBean("BizDriverFactory");
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * This test will use the BizDriverFactory to retrieve a Salesforce bizdriver, and then retrieve the channel object
     * and check that it parsed the xml correctly.  It does not make a connection to salesforce
     * 
     */
    public void testSalesForceChannelSpecification() {
        final String user = "user1";
        final String password = "password";
        final String timeout = "6000";
        final String salesForceBizDriverDocument = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "<xa:bizDriver xmlns=\"http://xaware.org/xas/ns1\"" + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + " xmlns:xa=\"http://xaware.org/xas/ns1\"" + " xsi:schemaLocation=\"http://xaware.org/xas/ns1 file:/C:/XAwareSource/XAware51/server/XSD/JDBCBizDriver.xsd\" xa:bizdrivertype=\"SF\" xa:version=\"5.1\">" + "<xa:description>description0</xa:description>" + "<xa:input>" + "<!-- As many params as necessary -->" + "<xa:param xa:name=\"param1\" xa:datatype=\"string\" xa:default=\"value1\" xa:description=\"\" />" + "</xa:input>" + "<xa:connection>" + "<xa:user>" + user + "</xa:user>" + "<xa:pwd>" + password + "</xa:pwd>" + "<xa:timeout>" + timeout + "</xa:timeout>" + "</xa:connection>" + "</xa:bizDriver>";
        try {
            assertNotNull("Failed to create the BizDriverFactory", bdFactory);
            SAXBuilder sb = new SAXBuilder();
            Document jdom = null;
            jdom = sb.build(new ByteArrayInputStream(salesForceBizDriverDocument.getBytes()));
            assertNotNull("Failed to parse and get the JDOM structure", jdom);
            SalesForceBizDriver sfBizDriver = new SalesForceBizDriver();
            sfBizDriver.setChannelSpecification(new SalesForceChannelSpecification());
            sfBizDriver.setBizDriverIdentifier("testSalesForceChannelSpecification");
            sfBizDriver.setJdomDocument(jdom);
            sfBizDriver.setupContext(sfBizDriver.getBizDriverIdentifier(), new HashMap<String, Object>(), null);
            assertNotNull("Failed to get Salesforce biz driver spec", sfBizDriver);
            SalesForceChannelSpecification channel = (SalesForceChannelSpecification) sfBizDriver.getChannelSpecification();
            assertNotNull("Failed to get Salesforce channel spec", channel);
            String url = channel.getProperty(XAwareConstants.BIZDRIVER_URL);
            assertEquals("Url was expected to be blank, but it was " + url, "", url);
            String cTimeout = channel.getProperty(XAwareConstants.BIZDRIVER_TIMEOUT);
            assertEquals("Timeout was expected to be " + timeout + " but it was " + cTimeout, timeout, cTimeout);
            String cUser = channel.getProperty(XAwareConstants.BIZDRIVER_USER);
            assertEquals("User was expected to be " + user + " but it was " + cUser, cUser, user);
            String cPwd = channel.getProperty(XAwareConstants.BIZDRIVER_PWD);
            assertEquals("Password was expected to be " + password + " but it was " + cPwd, cPwd, password);
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail(e);
        }
    }

    /**
     * This test will get a SalesForceTemplate object and attempt to make a connection to the
     * salesforce.com site.  This only will occur if the above mentioned properties are set.
     *
     */
    public void testSalesforceTemplate() {
        final String salesForceBizDriverDocument = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "<xa:bizDriver xmlns=\"http://xaware.org/xas/ns1\"" + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + " xmlns:xa=\"http://xaware.org/xas/ns1\"" + " xsi:schemaLocation=\"http://xaware.org/xas/ns1 file:/C:/XAwareSource/XAware51/server/XSD/JDBCBizDriver.xsd\" xa:bizdrivertype=\"SF\" xa:version=\"5.1\">" + "<xa:description>description0</xa:description>" + "<xa:input>" + "<!-- As many params as necessary -->" + "<xa:param xa:name=\"param1\" xa:datatype=\"string\" xa:default=\"value1\" xa:description=\"\" />" + "</xa:input>" + "<xa:connection>" + "<xa:user>$java:org.xaware.testing.functoids.TestingFunctoids.getProperty(salesforce.user)$</xa:user>" + "<xa:pwd>$java:org.xaware.testing.functoids.TestingFunctoids.getProperty(salesforce.pwd)$</xa:pwd>" + "<xa:timeout>$java:org.xaware.testing.functoids.TestingFunctoids.getProperty(salesforce.timeout)$</xa:timeout>" + "</xa:connection>" + "</xa:bizDriver>";
        try {
            TestingFunctoids.getProperty("salesforce.user");
            TestingFunctoids.getProperty("salesforce.pwd");
            TestingFunctoids.getProperty("salesforce.timeout");
        } catch (IllegalArgumentException e) {
            e.printStackTrace(System.out);
            System.out.println("The above stack trace indicates that you don't have salesforce.user, salesforce.pwd, or salesforce.timeout specified in your buildoptions.properties");
            return;
        }
        assertNotNull("Failed to create the BizDriverFactory", bdFactory);
        SAXBuilder sb = new SAXBuilder();
        Document jdom = null;
        try {
            jdom = sb.build(new ByteArrayInputStream(salesForceBizDriverDocument.getBytes()));
            assertNotNull("Failed to parse and get the JDOM structure", jdom);
            SalesForceBizDriver sfBizDriver = new SalesForceBizDriver();
            sfBizDriver.setChannelSpecification(new SalesForceChannelSpecification());
            sfBizDriver.setBizDriverIdentifier("testSalesForceChannelSpecification");
            sfBizDriver.setJdomDocument(jdom);
            sfBizDriver.setupContext(sfBizDriver.getBizDriverIdentifier(), new HashMap<String, Object>(), null);
            assertNotNull("Failed to get Salesforce biz driver spec", sfBizDriver);
            SalesForceTemplateFactory factory = new SalesForceTemplateFactory();
            assertNotNull("Failed to get SalesForceTemplateFactory", factory);
            ISalesForceTemplate template = factory.getTemplate(sfBizDriver, null);
            assertNotNull("Failed to retrieve the SalesForceTemplate", template);
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail(e);
        }
    }
}
