package org.acegisecurity.securechannel;

import junit.framework.TestCase;
import org.acegisecurity.ConfigAttribute;
import org.acegisecurity.ConfigAttributeDefinition;
import org.acegisecurity.SecurityConfig;
import org.acegisecurity.intercept.web.FilterInvocation;
import org.acegisecurity.intercept.web.FilterInvocationDefinitionSource;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Tests {@link ChannelProcessingFilter}.
 *
 * @author Ben Alex
 * @version $Id: ChannelProcessingFilterTests.java,v 1.4 2005/11/17 00:55:48 benalex Exp $
 */
public class ChannelProcessingFilterTests extends TestCase {

    public final void setUp() throws Exception {
        super.setUp();
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ChannelProcessingFilterTests.class);
    }

    public void testDetectsMissingChannelDecisionManager() throws Exception {
        ChannelProcessingFilter filter = new ChannelProcessingFilter();
        ConfigAttributeDefinition attr = new ConfigAttributeDefinition();
        attr.addConfigAttribute(new SecurityConfig("MOCK"));
        MockFilterInvocationDefinitionMap fids = new MockFilterInvocationDefinitionMap("/path", attr, true);
        filter.setFilterInvocationDefinitionSource(fids);
        try {
            filter.afterPropertiesSet();
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertEquals("channelDecisionManager must be specified", expected.getMessage());
        }
    }

    public void testDetectsMissingFilterInvocationDefinitionSource() throws Exception {
        ChannelProcessingFilter filter = new ChannelProcessingFilter();
        filter.setChannelDecisionManager(new MockChannelDecisionManager(false, "MOCK"));
        try {
            filter.afterPropertiesSet();
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertEquals("filterInvocationDefinitionSource must be specified", expected.getMessage());
        }
    }

    public void testDetectsSupportedConfigAttribute() throws Exception {
        ChannelProcessingFilter filter = new ChannelProcessingFilter();
        filter.setChannelDecisionManager(new MockChannelDecisionManager(false, "SUPPORTS_MOCK_ONLY"));
        ConfigAttributeDefinition attr = new ConfigAttributeDefinition();
        attr.addConfigAttribute(new SecurityConfig("SUPPORTS_MOCK_ONLY"));
        MockFilterInvocationDefinitionMap fids = new MockFilterInvocationDefinitionMap("/path", attr, true);
        filter.setFilterInvocationDefinitionSource(fids);
        filter.afterPropertiesSet();
        assertTrue(true);
    }

    public void testDetectsUnsupportedConfigAttribute() throws Exception {
        ChannelProcessingFilter filter = new ChannelProcessingFilter();
        filter.setChannelDecisionManager(new MockChannelDecisionManager(false, "SUPPORTS_MOCK_ONLY"));
        ConfigAttributeDefinition attr = new ConfigAttributeDefinition();
        attr.addConfigAttribute(new SecurityConfig("SUPPORTS_MOCK_ONLY"));
        attr.addConfigAttribute(new SecurityConfig("INVALID_ATTRIBUTE"));
        MockFilterInvocationDefinitionMap fids = new MockFilterInvocationDefinitionMap("/path", attr, true);
        filter.setFilterInvocationDefinitionSource(fids);
        try {
            filter.afterPropertiesSet();
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().startsWith("Unsupported configuration attributes:"));
        }
    }

    public void testDoFilterWhenManagerDoesCommitResponse() throws Exception {
        ChannelProcessingFilter filter = new ChannelProcessingFilter();
        filter.setChannelDecisionManager(new MockChannelDecisionManager(true, "SOME_ATTRIBUTE"));
        ConfigAttributeDefinition attr = new ConfigAttributeDefinition();
        attr.addConfigAttribute(new SecurityConfig("SOME_ATTRIBUTE"));
        MockFilterInvocationDefinitionMap fids = new MockFilterInvocationDefinitionMap("/path", attr, true);
        filter.setFilterInvocationDefinitionSource(fids);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setQueryString("info=now");
        request.setServletPath("/path");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain(false);
        filter.doFilter(request, response, chain);
        assertTrue(true);
    }

    public void testDoFilterWhenManagerDoesNotCommitResponse() throws Exception {
        ChannelProcessingFilter filter = new ChannelProcessingFilter();
        filter.setChannelDecisionManager(new MockChannelDecisionManager(false, "SOME_ATTRIBUTE"));
        ConfigAttributeDefinition attr = new ConfigAttributeDefinition();
        attr.addConfigAttribute(new SecurityConfig("SOME_ATTRIBUTE"));
        MockFilterInvocationDefinitionMap fids = new MockFilterInvocationDefinitionMap("/path", attr, true);
        filter.setFilterInvocationDefinitionSource(fids);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setQueryString("info=now");
        request.setServletPath("/path");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain(true);
        filter.doFilter(request, response, chain);
        assertTrue(true);
    }

    public void testDoFilterWhenNullConfigAttributeReturned() throws Exception {
        ChannelProcessingFilter filter = new ChannelProcessingFilter();
        filter.setChannelDecisionManager(new MockChannelDecisionManager(false, "NOT_USED"));
        ConfigAttributeDefinition attr = new ConfigAttributeDefinition();
        attr.addConfigAttribute(new SecurityConfig("NOT_USED"));
        MockFilterInvocationDefinitionMap fids = new MockFilterInvocationDefinitionMap("/path", attr, true);
        filter.setFilterInvocationDefinitionSource(fids);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setQueryString("info=now");
        request.setServletPath("/PATH_NOT_MATCHING_CONFIG_ATTRIBUTE");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain(true);
        filter.doFilter(request, response, chain);
        assertTrue(true);
    }

    public void testDoFilterWithNonHttpServletRequestDetected() throws Exception {
        ChannelProcessingFilter filter = new ChannelProcessingFilter();
        try {
            filter.doFilter(null, new MockHttpServletResponse(), new MockFilterChain());
            fail("Should have thrown ServletException");
        } catch (ServletException expected) {
            assertEquals("HttpServletRequest required", expected.getMessage());
        }
    }

    public void testDoFilterWithNonHttpServletResponseDetected() throws Exception {
        ChannelProcessingFilter filter = new ChannelProcessingFilter();
        try {
            filter.doFilter(new MockHttpServletRequest(null, null), null, new MockFilterChain());
            fail("Should have thrown ServletException");
        } catch (ServletException expected) {
            assertEquals("HttpServletResponse required", expected.getMessage());
        }
    }

    public void testGetterSetters() throws Exception {
        ChannelProcessingFilter filter = new ChannelProcessingFilter();
        filter.setChannelDecisionManager(new MockChannelDecisionManager(false, "MOCK"));
        assertTrue(filter.getChannelDecisionManager() != null);
        ConfigAttributeDefinition attr = new ConfigAttributeDefinition();
        attr.addConfigAttribute(new SecurityConfig("MOCK"));
        MockFilterInvocationDefinitionMap fids = new MockFilterInvocationDefinitionMap("/path", attr, false);
        filter.setFilterInvocationDefinitionSource(fids);
        assertTrue(filter.getFilterInvocationDefinitionSource() != null);
        filter.init(null);
        filter.afterPropertiesSet();
        filter.destroy();
    }

    private class MockChannelDecisionManager implements ChannelDecisionManager {

        private String supportAttribute;

        private boolean commitAResponse;

        public MockChannelDecisionManager(boolean commitAResponse, String supportAttribute) {
            this.commitAResponse = commitAResponse;
            this.supportAttribute = supportAttribute;
        }

        private MockChannelDecisionManager() {
            super();
        }

        public void decide(FilterInvocation invocation, ConfigAttributeDefinition config) throws IOException, ServletException {
            if (commitAResponse) {
                invocation.getHttpResponse().sendRedirect("/redirected");
            }
        }

        public boolean supports(ConfigAttribute attribute) {
            if (attribute.getAttribute().equals(supportAttribute)) {
                return true;
            } else {
                return false;
            }
        }
    }

    private class MockFilterChain implements FilterChain {

        private boolean expectToProceed;

        public MockFilterChain(boolean expectToProceed) {
            this.expectToProceed = expectToProceed;
        }

        private MockFilterChain() {
            super();
        }

        public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
            if (expectToProceed) {
                assertTrue(true);
            } else {
                fail("Did not expect filter chain to proceed");
            }
        }
    }

    private class MockFilterInvocationDefinitionMap implements FilterInvocationDefinitionSource {

        private ConfigAttributeDefinition toReturn;

        private String servletPath;

        private boolean provideIterator;

        public MockFilterInvocationDefinitionMap(String servletPath, ConfigAttributeDefinition toReturn, boolean provideIterator) {
            this.servletPath = servletPath;
            this.toReturn = toReturn;
            this.provideIterator = provideIterator;
        }

        private MockFilterInvocationDefinitionMap() {
            super();
        }

        public ConfigAttributeDefinition getAttributes(Object object) throws IllegalArgumentException {
            FilterInvocation fi = (FilterInvocation) object;
            if (servletPath.equals(fi.getHttpRequest().getServletPath())) {
                return toReturn;
            } else {
                return null;
            }
        }

        public Iterator getConfigAttributeDefinitions() {
            if (!provideIterator) {
                return null;
            }
            List list = new Vector();
            list.add(toReturn);
            return list.iterator();
        }

        public boolean supports(Class clazz) {
            return true;
        }
    }
}
