package jather;

import static org.junit.Assert.*;
import static org.junit.Assert.assertNotNull;
import jather.JatherClient.LockedProcessContext;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class JatherClientTest {

    List<Executive> execs;

    JatherClient client;

    MockCallable callable;

    MockBadCallable badCallable;

    MockBlockingCallable blockingCallable;

    static int counter;

    @Before
    public void before() {
        counter++;
        client = new JatherClient();
        client.setClusterName("jather" + counter);
        callable = new MockCallable();
        badCallable = new MockBadCallable();
        blockingCallable = new MockBlockingCallable();
        MockBlockingCallable.counter = 0;
        MockBlockingCallable.cancelled = false;
        execs = new ArrayList<Executive>();
        for (int i = 0; i < 5; i++) {
            Executive e = new Executive();
            e.setClusterName("jather" + counter);
            execs.add(e);
        }
    }

    @After
    public void after() {
        MockBlockingCallable.counter = -100;
        client.close();
        for (Executive e : execs) {
            e.closeChannel();
        }
    }

    @Test
    public void testId() throws Exception {
        assertNotNull("Make sure there is an id.", client.nextId());
        assertEquals("Make sure there is an address", client.getChannel().getLocalAddress(), client.nextId().getSource());
    }

    @Test
    public void testAddToMap() throws Exception {
        LockedProcessContext ctxt = client.addCallable(callable);
        assertEquals("Make sure the context is added", ctxt, client.getProcessMap().get(ctxt.getProcessContext().getProcessId()));
    }

    @Test
    public void testExecute() throws Exception {
        for (Executive e : execs) {
            e.start();
        }
        Object obj = client.execute(callable);
        assertEquals("Make sure the result was collected", MockCallable.class.toString(), obj);
    }

    @Test(expected = ArithmeticException.class)
    public void testBadExecute() throws Exception {
        for (Executive e : execs) {
            e.start();
        }
        client.execute(badCallable);
    }

    @Test
    public void testSubmit() throws Exception {
        for (Executive e : execs) {
            e.start();
        }
        Future<?> f = client.submit(callable);
        assertEquals("Make sure the result was collected", MockCallable.class.toString(), f.get());
    }

    @Test(expected = TimeoutException.class)
    public void testSubmitTimeout() throws Exception {
        for (Executive e : execs) {
            e.start();
        }
        Future<?> f = client.submit(callable);
        f.get(1, TimeUnit.NANOSECONDS);
    }

    @Test(timeout = 60 * 1000)
    public void testSubmitDone() throws Exception {
        for (Executive e : execs) {
            e.start();
        }
        Future<?> f = client.submit(callable);
        while (!f.isDone()) {
            Thread.yield();
        }
        assertEquals("Make sure the result was collected", MockCallable.class.toString(), f.get());
    }

    @Test(timeout = 60 * 1000)
    public void testSubmitCancel() throws Exception {
        for (Executive e : execs) {
            e.start();
        }
        Future<?> f = client.submit(blockingCallable);
        while (MockBlockingCallable.counter == 0) {
            Thread.yield();
        }
        f.cancel(true);
        assertTrue("Make sure the future was cancelled", f.isCancelled());
    }

    @Test(timeout = 60 * 1000)
    public void testSubmitLate() throws Exception {
        Future<?> f = client.submit(callable);
        System.err.println("starting....");
        for (Executive e : execs) {
            e.start();
        }
        assertEquals("Make sure the result was collected", MockCallable.class.toString(), f.get());
    }

    @Test(timeout = 60 * 1000)
    public void testReSubmit() throws Exception {
        for (Executive e : execs) {
            e.start();
        }
        MockBlockingCallable.counter = 0;
        Future<?> f = client.submit(blockingCallable);
        while (MockBlockingCallable.counter == 0) {
            Thread.yield();
        }
        for (Executive e : execs) {
            e.getChannel().disconnect();
        }
        Executive e = new Executive();
        e.setClusterName(client.getClusterName());
        execs.add(e);
        e.start();
        MockBlockingCallable.counter = -2;
        assertEquals("Make sure the result was collected", MockBlockingCallable.class.toString(), f.get());
    }
}
