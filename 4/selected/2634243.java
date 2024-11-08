package it.newinstance.test.watchdog.monitors;

import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.Reader;
import junit.framework.TestCase;
import it.newinstance.test.watchdog.mock.MockStringListener;
import it.newinstance.watchdog.DefaultServer;
import it.newinstance.watchdog.Server;
import it.newinstance.watchdog.monitors.TailMonitor;

/**
 * @author Luigi R. Viggiano
 * @version $Revision: 53 $
 * @since 27-nov-2005
 */
public class TestTailMonitor extends TestCase {

    public void testTail() throws Exception {
        PipedWriter writer = new PipedWriter();
        Reader reader = new PipedReader(writer);
        writer.write("test\n");
        TailMonitor tail = new TailMonitor("TailMonitor", reader);
        MockStringListener listener = new MockStringListener();
        tail.subscribe(listener);
        final Server server = new DefaultServer();
        server.register(tail);
        server.start();
        synchronized (listener) {
            if (listener.getMessage() == null) listener.wait();
        }
        assertEquals("test", listener.getMessage());
        new Thread("New") {

            @Override
            public void run() {
                server.stop();
            }
        }.start();
    }
}
