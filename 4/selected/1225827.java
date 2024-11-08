package org.tripcom.kerneltests.api;

import java.net.InetAddress;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.tripcom.api.ws.client.TSClient;
import org.tripcom.integration.entry.SpaceURI;
import org.tripcom.integration.entry.Template;
import org.tripcom.kerneltests.TestConfiguration;

@RunWith(Parameterized.class)
public class ConcurrentTest {

    private static Integer THREAD_COUNT = 100;

    private String localhostname;

    private int localport;

    private String remotehostname;

    private int remoteport;

    private TSClient tsclient;

    private SpaceURI rootSpace;

    private Set<Statement> statements;

    private Integer count;

    public ConcurrentTest(String localhostname, String lport, String remotehostname, String rport) throws Exception {
        this.localhostname = localhostname;
        this.localport = Integer.parseInt(lport);
        this.remotehostname = remotehostname;
        this.remoteport = Integer.parseInt(rport);
        this.tsclient = new TSClient("http", InetAddress.getByName(this.localhostname), this.localport);
        this.rootSpace = new SpaceURI("tsc://" + this.localhostname + ":" + this.localport + "/lalalala");
        this.statements = new HashSet<Statement>();
        ValueFactory myFactory = new ValueFactoryImpl();
        for (int i = 0; i <= THREAD_COUNT; i++) {
            URI mySubject = myFactory.createURI("http://example.com/book/book" + i);
            URI myPredicate = myFactory.createURI("http://purl.org/dc/elements/1.1/title");
            LiteralImpl myObject = (LiteralImpl) myFactory.createLiteral("Book " + i);
            statements.add(myFactory.createStatement(mySubject, myPredicate, myObject));
        }
    }

    @Before
    public void setUp() throws Exception {
        this.count = 0;
    }

    @Parameters
    public static Collection<?> regExValues() throws Exception {
        return TestConfiguration.getConfiguration();
    }

    @Test
    public void doManyReads() throws Exception {
        this.rootSpace = this.tsclient.create(this.rootSpace);
        this.tsclient.outSynchrone(this.statements.iterator().next(), this.rootSpace);
        this.readAndCheck();
        Thread[] threads = new Thread[THREAD_COUNT];
        for (int i = 0; i < THREAD_COUNT; i++) {
            threads[i] = new Thread() {

                public void run() {
                    try {
                        readAndCheck();
                        incrementCount();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            };
            threads[i].start();
        }
        for (Thread t : threads) {
            t.join();
        }
        assertEquals(THREAD_COUNT, this.getCount());
        this.tsclient.destroy(this.rootSpace);
    }

    @Test
    public void doConcurrentWriteAndIn() throws Exception {
        this.rootSpace = this.tsclient.create(this.rootSpace);
        Thread writeThread = new Thread() {

            public void run() {
                try {
                    for (Statement t : statements) {
                        tsclient.out(t, rootSpace);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        writeThread.start();
        Thread[] threads = new Thread[THREAD_COUNT];
        for (int i = 0; i < THREAD_COUNT; i++) {
            threads[i] = new Thread() {

                public void run() {
                    try {
                        Set<Statement> result = tsclient.in(new Template("SELECT * WHERE { ?s ?p ?o . }"), rootSpace, 20000);
                        assertNotNull(result);
                        assertEquals(1, result.size());
                        incrementCount();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            };
            threads[i].start();
            Thread.sleep(100);
        }
        for (Thread t : threads) {
            t.join();
        }
        assertEquals(THREAD_COUNT, this.getCount());
        Set<Set<Statement>> result = this.tsclient.rdmultiple(new Template("SELECT * WHERE { ?s ?p ?o . }"), this.rootSpace, 10000);
        assertNotNull(result);
        assertEquals(1, result.size());
        Set<Statement> st = result.iterator().next();
        this.tsclient.destroy(this.rootSpace);
    }

    private void readAndCheck() throws Exception {
        Template t = new Template("CONSTRUCT" + " { ?s <http://purl.org/dc/elements/1.1/title>   ?o }" + " WHERE { ?s <http://purl.org/dc/elements/1.1/title> ?o . }");
        Set<Statement> result = tsclient.rd(t, this.rootSpace, 20000);
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    private void incrementCount() {
        synchronized (this.count) {
            this.count++;
        }
    }

    private Integer getCount() {
        synchronized (this.count) {
            return this.count;
        }
    }
}
