package org.atricore.idbus.capabilities.josso.test;

import org.apache.camel.ContextTestSupport;
import org.apache.camel.Endpoint;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atricore.idbus.kernel.main.mediation.camel.AbstractCamelEndpoint;

/**
 *
 * @author <a href="mailto:gbrigand@josso.org">Gianluca Brigandi</a>
 * @version $Id: JOSSO11WebSSOComponentTest.java 1182 2009-05-05 20:28:51Z ajadzinsky $
 */
public class JOSSO11WebSSOComponentTest extends ContextTestSupport {

    private static Log log = LogFactory.getLog(JOSSO11WebSSORouteTest.class);

    public void testJOSSO11WebSSOEndpointsAreConfiguredProperly() throws Exception {
        AbstractCamelEndpoint endpoint = resolveMandatoryEndpoint("josso-binding:JOSSO11AuthnRequestToSAMLR2?channelRef=ABC");
        assertEquals("getChannelRef", "ABC", endpoint.getChannelRef());
    }

    @Override
    protected AbstractCamelEndpoint resolveMandatoryEndpoint(String uri) {
        Endpoint endpoint = super.resolveMandatoryEndpoint(uri);
        return assertIsInstanceOf(AbstractCamelEndpoint.class, endpoint);
    }
}
