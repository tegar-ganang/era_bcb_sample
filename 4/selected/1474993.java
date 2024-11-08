package org.tripcom.tsadapter;

import java.util.HashSet;
import java.util.Random;
import net.jini.core.entry.Entry;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.URIImpl;
import org.tripcom.integration.entry.OutSMEntry;
import org.tripcom.integration.entry.RdMetaMEntry;
import org.tripcom.integration.entry.RdTSAdapterEntry;
import org.tripcom.integration.entry.ReadType;
import org.tripcom.integration.entry.SpaceSelection;
import org.tripcom.integration.entry.SpaceURI;
import org.tripcom.integration.entry.TSAdapterEntry;
import org.tripcom.integration.entry.TripleEntry;
import com.ontotext.ordi.tripleset.TConnection;

public class UndertministicBehaviour {

    private static org.tripcom.tsadapter.TSAdapter adapter;

    private static Random random = new Random();

    private static Thread t;

    private static final boolean IS_RANDOM = true;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        adapter = org.tripcom.tsadapter.TSAdapter.getAdapter();
        TConnection connection = adapter.getTSource().getConnection();
        connection.removeStatement(null, null, null, null);
        connection.close();
        t = new Thread(adapter);
        t.start();
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
        Entry e;
        while ((e = adapter.getSpace().take(null, null, 0)) != null) {
            System.out.println("Deleted " + e);
        }
    }

    private final Object lock = new Object();

    @Test
    public void testBehaviour() throws Exception {
        final int max = (int) 10E+2;
        Runnable writeThread = new Runnable() {

            public void run() {
                synchronized (lock) {
                    try {
                        for (int i = 0; i < max; i++) {
                            TSAdapterEntry out = new TSAdapterEntry();
                            out.annotations = new HashSet<TripleEntry>();
                            out.clientInfo = null;
                            out.data = new HashSet<TripleEntry>();
                            out.data.add(getTripleEntry(i));
                            out.operationID = (long) i;
                            out.space = new SpaceURI("urn:test:space:root");
                            out.tripleset = new HashSet<java.net.URI>();
                            out.writeOutSMEntry = true;
                            adapter.getSpace().write(out, null, Long.MAX_VALUE);
                            if (IS_RANDOM) Thread.sleep(random.nextInt(100));
                            if (out.writeOutSMEntry == true) {
                                adapter.getSpace().take(new OutSMEntry(), null, Long.MAX_VALUE);
                                if (IS_RANDOM) Thread.sleep(random.nextInt(100));
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        Runnable readThread = new Runnable() {

            public void run() {
                try {
                    for (int i = 0; i < max; i++) {
                        RdTSAdapterEntry rd = new RdTSAdapterEntry();
                        rd.clientInfo = null;
                        rd.operationID = (long) i;
                        rd.space = new SpaceURI("tsc://example.org:1234/rootspace/subspace/deepersubspace/thisspace");
                        rd.permittedSpaces = new SpaceSelection();
                        rd.kind = ReadType.IN;
                        rd.query = String.format("CONSTRUCT { <%s> <%s> ?o } WHERE { <%s> <%s> ?o }", "http://example.com/book/book" + i, "http://purl.org/dc/elements/1.1/title", "http://example.com/book/book" + i, "http://purl.org/dc/elements/1.1/title");
                        adapter.getSpace().write(rd, null, Long.MAX_VALUE);
                        if (IS_RANDOM) Thread.sleep(random.nextInt(100));
                        RdMetaMEntry result = (RdMetaMEntry) adapter.getSpace().take(new RdMetaMEntry(), null, Long.MAX_VALUE);
                        Assert.assertEquals(result.data.size(), 1);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        Thread t = new Thread(writeThread);
        t.start();
        Thread t2 = new Thread(readThread);
        t2.start();
        Thread.sleep(1000);
        synchronized (lock) {
            System.out.println("OK!");
        }
    }

    private TripleEntry getTripleEntry(int value) {
        TripleEntry entry = new TripleEntry();
        entry.setSubject(new URIImpl("http://example.com/book/book" + value));
        entry.setPredicate(new URIImpl("http://purl.org/dc/elements/1.1/title"));
        entry.setObject(new LiteralImpl("Book" + value));
        return entry;
    }
}
