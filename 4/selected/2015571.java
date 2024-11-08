package org.coury.jfilehelpers.tests.callbacks;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.coury.jfilehelpers.engines.EngineBase;
import org.coury.jfilehelpers.engines.FileHelperEngine;
import junit.framework.TestCase;

/**
 * @author Robert Eccardt
 *
 */
public abstract class CallbacksBase extends TestCase {

    static FileHelperEngine<Customer> engine;

    static final String customerFile = "customers_for_callbacks.txt";

    static List<Customer> customers;

    static int readCount;

    static int beforeReadCount;

    static int afterReadCount;

    static int writeCount;

    static int beforeWriteCount;

    static int afterWriteCount;

    static String eventLine;

    static String notifyLine;

    @Override
    protected void setUp() {
        engine = new FileHelperEngine<Customer>(Customer.class);
        customers = new ArrayList<Customer>();
        readCount = writeCount = 0;
        beforeReadCount = beforeWriteCount = 0;
        afterReadCount = afterWriteCount = 0;
        eventLine = "";
        notifyLine = "";
        Customer c = new Customer();
        c.custId = 1;
        c.name = "John Doe";
        c.rating = 1;
        customers.add(c);
        c = new Customer();
        c.custId = 2;
        c.name = "Jane Rowe";
        c.rating = 2;
        customers.add(c);
        c = new Customer();
        c.custId = 3;
        c.name = "Santa Claus";
        c.rating = 3;
        customers.add(c);
        c = new Customer();
        c.custId = 4;
        c.name = "Homer Simpson";
        c.rating = 4;
        customers.add(c);
    }

    @Override
    protected void tearDown() {
        new File(customerFile).delete();
    }

    public static void engineTester(EngineBase<Customer> e) {
        assertEquals(e, engine);
    }

    public static void incrementReadCount() {
        ++readCount;
    }

    public static void incrementBeforeReadCount() {
        ++beforeReadCount;
    }

    public static void incrementAfterReadCount() {
        ++afterReadCount;
    }

    public static void incrementWriteCount() {
        ++writeCount;
    }

    public static void incrementBeforeWriteCount() {
        ++beforeWriteCount;
    }

    public static void incrementAfterWriteCount() {
        ++afterWriteCount;
    }

    public static void setEventLine(String s) {
        eventLine = s;
    }

    public static void setNotifyLine(String s) {
        notifyLine = s;
    }
}
