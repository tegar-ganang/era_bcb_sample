package org.objectstyle.cayenne.remote;

import org.objectstyle.cayenne.DataChannel;
import org.objectstyle.cayenne.MockDataChannel;
import org.objectstyle.cayenne.remote.service.LocalConnection;
import junit.framework.TestCase;

/**
 * @author Andrus Adamchik
 */
public class LocalConnectionTst extends TestCase {

    public void testConstructors() {
        DataChannel handler1 = new MockDataChannel();
        LocalConnection connector1 = new LocalConnection(handler1);
        assertFalse(connector1.isSerializingMessages());
        assertSame(handler1, connector1.getChannel());
        DataChannel handler2 = new MockDataChannel();
        LocalConnection connector2 = new LocalConnection(handler2, LocalConnection.JAVA_SERIALIZATION);
        assertTrue(connector2.isSerializingMessages());
        assertSame(handler2, connector2.getChannel());
    }
}
