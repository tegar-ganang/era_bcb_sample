package com.tirsen.hanoi.test.engine;

import com.tirsen.hanoi.engine.*;
import com.tirsen.hanoi.builder.DefinitionBuilder;
import junit.framework.TestCase;
import java.util.*;

/**
 * TODO: this is no longer supported.
 *
 * <!-- $Id: TestConnector.java,v 1.3 2002/08/08 09:58:28 tirsen Exp $ -->
 * <!-- $Author: tirsen $ -->
 *
 * @author Jon Tirs&acute;n (tirsen@users.sourceforge.net)
 * @version $Revision: 1.3 $
 */
public class TestConnector extends TestCase {

    private static final String REQUEST = "Hello from activity!";

    private static final String RESPONSE = "Hello from connector!";

    public static class TestConnectorImpl extends AbstractConnector {

        protected Class getChannelClass() {
            return TestChannel.class;
        }

        protected Object processRequest(Object request) {
            assertEquals(request, REQUEST);
            String response = RESPONSE;
            return response;
        }
    }

    private TestConnectorImpl connector = new TestConnectorImpl();

    public static class TestChannel extends Channel {

        private String request;

        private String response;

        public String getRequest() {
            return request;
        }

        public void setRequest(String request) {
            this.request = request;
        }

        public String getResponse() {
            return response;
        }

        public void setResponse(String response) {
            this.response = response;
        }

        protected Object marshal() {
            return request;
        }

        protected void demarshal(Object response) {
            this.response = (String) response;
        }
    }

    public static class PrintValue extends Activity {

        private String value;

        public void setValue(String value) {
            this.value = value;
        }

        public void assertValue(String expected) {
            assertEquals(expected, value);
        }

        public int run() {
            System.out.println("value = " + value);
            return CONTINUE;
        }
    }

    public TestConnector(String name) {
        super(name);
    }

    public void testNothing() {
    }
}
