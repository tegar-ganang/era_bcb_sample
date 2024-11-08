package rice.scribe.testing;

import rice.pastry.*;
import rice.pastry.join.*;
import rice.pastry.direct.*;
import rice.pastry.messaging.*;
import rice.scribe.*;
import rice.pastry.security.*;
import rice.pastry.standard.*;
import java.util.*;
import java.io.*;
import java.security.*;

/**
 * @(#) BasicScribeRegrTest.java
 *
 * A basic scribe regression test suite for Scribe. It tests if the basic 
 * operations such as create, join, multicast, leave are working fine.
 *
 * @author Romer Gil
 * @author Eric Engineer 
 */
public class BasicScribeRegrTest {

    private DirectPastryNodeFactory m_factory;

    private NetworkSimulator m_simulator;

    private LinkedList m_pastryNodes;

    private LinkedList m_scribeApps;

    private Vector m_topics;

    private MRTracker m_tracker;

    private MRTracker m_tracker2;

    private Random rng;

    public Message m_lastMsg;

    public NodeId.Distance m_lastDist;

    public BasicScribeRegrTest() {
        m_simulator = new EuclideanNetwork();
        m_factory = new DirectPastryNodeFactory(new RandomNodeIdFactory(), m_simulator);
        m_pastryNodes = new LinkedList();
        m_scribeApps = new LinkedList();
        rng = new Random();
        m_tracker = new MRTracker();
        m_tracker2 = new MRTracker();
        m_topics = new Vector();
    }

    public void makeScribeNode(int apps) {
        NodeHandle bootstrap = null;
        try {
            PastryNode lastnode = (PastryNode) m_pastryNodes.getLast();
            bootstrap = lastnode.getLocalHandle();
        } catch (NoSuchElementException e) {
        }
        PastryNode pnode = m_factory.newNode(bootstrap);
        m_pastryNodes.add(pnode);
        Credentials credentials = new PermissiveCredentials();
        Scribe scribe = new Scribe(pnode, credentials);
        for (int i = 0; i < apps; i++) {
            Credentials cred = new PermissiveCredentials();
            BasicScribeRegrTestApp app = new BasicScribeRegrTestApp(pnode, scribe, i, cred);
            m_scribeApps.add(app);
        }
    }

    public boolean simulate() {
        return m_simulator.simulate();
    }

    /**
     * Main entry point for the basic scribe regression test suite.
     *
     * @return true if all the tests PASSED
     */
    public static boolean start() {
        BasicScribeRegrTest st = new BasicScribeRegrTest();
        int n, a, m, t, i;
        System.out.println(" \n\n BasicScribeRegrTest : which tests if the create, join, multicast, leave operations of Scribe work is about to START  \n");
        n = 100;
        a = 5;
        m = 10;
        t = 5;
        for (i = 0; i < n; i++) {
            st.makeScribeNode(a);
            while (st.simulate()) ;
        }
        System.gc();
        boolean ok = st.doTheTesting(m, t);
        System.out.println("\n" + n + " nodes constructed with " + a + " applications per node.\n" + t + " topics created, and " + m + " messages sent per topic.");
        String out = "\nBASIC SCRIBE REGRESSION TEST - ";
        out += ok ? "PASSED" : "FAILED (see output above for details)";
        System.out.println(out);
        return ok;
    }

    public boolean doTheTesting(int msgs, int topics) {
        int i, j, k, subs, topic, app;
        NodeId tid;
        int totalApps = m_scribeApps.size();
        for (i = 0; i < topics; i++) {
            tid = generateTopicId(new String("ScribeTest" + i));
            app = rng.nextInt(totalApps);
            create(app, tid);
            while (simulate()) ;
            for (j = 0; j < totalApps; j++) {
                BasicScribeRegrTestApp a = (BasicScribeRegrTestApp) m_scribeApps.get(j);
                a.putTopic(tid);
            }
            m_topics.add(tid);
            m_tracker.setSubscribed(tid, true);
            m_tracker2.setSubscribed(tid, true);
            subs = rng.nextInt(totalApps);
            for (j = 0; j < subs; j++) {
                join(rng.nextInt(totalApps), tid);
                while (simulate()) ;
            }
        }
        for (i = 0; i < topics; i++) {
            for (j = 0; j < msgs; j++) {
                topic = rng.nextInt(m_topics.size());
                app = rng.nextInt(totalApps);
                tid = (NodeId) m_topics.get(topic);
                multicast(app, tid);
                m_tracker.receivedMessage(tid);
                while (simulate()) ;
            }
        }
        int unsubs = rng.nextInt(totalApps);
        for (i = 0; i < unsubs; i++) {
            leave(rng.nextInt(totalApps));
            while (simulate()) ;
        }
        for (i = 0; i < topics; i++) {
            for (j = 0; j < msgs; j++) {
                topic = rng.nextInt(m_topics.size());
                app = rng.nextInt(totalApps);
                tid = (NodeId) m_topics.get(topic);
                multicast(app, tid);
                m_tracker2.receivedMessage(tid);
                while (simulate()) ;
            }
        }
        boolean ok = true;
        for (i = 0; i < totalApps; i++) {
            BasicScribeRegrTestApp a = (BasicScribeRegrTestApp) m_scribeApps.get(i);
            ok = a.verifyApplication(m_topics, m_tracker, m_tracker2) && ok;
        }
        return ok;
    }

    public NodeId generateTopicId(String topicName) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException e) {
            System.err.println("No SHA support!");
        }
        md.update(topicName.getBytes());
        byte[] digest = md.digest();
        NodeId newId = new NodeId(digest);
        return newId;
    }

    private void multicast(int app, NodeId tid) {
        BasicScribeRegrTestApp a = (BasicScribeRegrTestApp) m_scribeApps.get(app);
        a.multicast(tid);
    }

    private void join(int app, NodeId tid) {
        BasicScribeRegrTestApp a = (BasicScribeRegrTestApp) m_scribeApps.get(app);
        a.join(tid);
    }

    private void leave(int app) {
        BasicScribeRegrTestApp a = (BasicScribeRegrTestApp) m_scribeApps.get(app);
        a.leave(null);
    }

    private void create(int app, NodeId tid) {
        BasicScribeRegrTestApp a = (BasicScribeRegrTestApp) m_scribeApps.get(app);
        a.create(tid);
    }
}
