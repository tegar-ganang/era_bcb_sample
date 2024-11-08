package it.newinstance.test.watchdog.security.monitors;

import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.Reader;
import junit.framework.TestCase;
import it.newinstance.test.watchdog.security.stub.StringListenerStub;
import it.newinstance.watchdog.engine.DefaultServer;
import it.newinstance.watchdog.engine.Server;
import it.newinstance.watchdog.security.monitors.TailMonitor;

/**
 * @author Luigi R. Viggiano
 * @version $Revision: 46 $
 * @since 27-nov-2005
 */
public class TestTailMonitor extends TestCase {

    public void testTail() throws Exception {
        PipedWriter writer = new PipedWriter();
        Reader reader = new PipedReader(writer);
        writer.write("test\n");
        TailMonitor tail = new TailMonitor("TailMonitor", reader);
        StringListenerStub listener = new StringListenerStub();
        tail.subscribe(listener);
        final Server server = new DefaultServer();
        server.register(tail);
        server.start();
        synchronized (listener) {
            if (listener.getMessage() == null) listener.wait();
        }
        assertEquals("test", listener.getMessage());
        server.stop();
    }
}
