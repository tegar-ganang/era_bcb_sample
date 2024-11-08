package org.exist.xmldb.test.concurrent;

import junit.framework.TestCase;
import junit.textui.TestRunner;
import org.exist.storage.DBBroker;
import org.exist.xmldb.DatabaseInstanceManager;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Database;
import org.xmldb.api.modules.XMLResource;

public class DeadlockTest extends TestCase {

    public static final String DOCUMENT_CONTENT = "<document>\n" + "  <element1>value1</element1>\n" + "  <element2>value2</element2>\n" + "  <element3>value3</element3>\n" + "  <element4>value4</element4>\n" + "</document>\n";

    private String rootCollection = "xmldb:exist://" + DBBroker.ROOT_COLLECTION;

    private Collection root;

    public void testDeadlock() {
        int threads = 20;
        int resources = 200;
        try {
            Thread[] writerThreads = new Thread[threads];
            for (int i = 0; i < threads; i++) {
                writerThreads[i] = new WriterThread(rootCollection, resources);
                writerThreads[i].setName("T" + i);
                writerThreads[i].start();
            }
            for (int i = 0; i < threads; i++) {
                writerThreads[i].join();
            }
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    protected void setUp() {
        try {
            String driver = "org.exist.xmldb.DatabaseImpl";
            Class cl = Class.forName(driver);
            Database database = (Database) cl.newInstance();
            assertNotNull(database);
            database.setProperty("create-database", "true");
            DatabaseManager.registerDatabase(database);
            root = DatabaseManager.getCollection(rootCollection, "admin", "");
            assertNotNull(root);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    protected void tearDown() {
        try {
            DatabaseInstanceManager manager = (DatabaseInstanceManager) root.getService("DatabaseInstanceManager", "1.0");
            assertNotNull(manager);
            manager.shutdown();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    public static void main(String args[]) {
        TestRunner.run(DeadlockTest.class);
    }

    public static class WriterThread extends Thread {

        protected Collection collection = null;

        protected int resources = 0;

        public WriterThread(String collectionURI, int resources) throws Exception {
            this.collection = DatabaseManager.getCollection(collectionURI);
            this.resources = resources;
        }

        public void run() {
            try {
                for (int i = 0; i < resources; i++) {
                    XMLResource document = (XMLResource) collection.createResource(Thread.currentThread().getName() + "_" + i, "XMLResource");
                    document.setContent(DOCUMENT_CONTENT);
                    System.out.print("storing document " + document.getId() + "\n");
                    collection.storeResource(document);
                }
            } catch (Exception e) {
                System.err.println("Writer " + Thread.currentThread().getName() + " failed: " + e);
                e.printStackTrace();
            }
        }
    }
}
