package fulmine.model.component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import fulmine.Domain;
import fulmine.IDomain;
import fulmine.IType;
import fulmine.Type;
import fulmine.event.listener.IEventListener;
import fulmine.protocol.wire.IWireIdentity;
import fulmine.protocol.wire.WireIdentity;
import fulmine.protocol.wire.operation.IOperationScope;
import fulmine.util.reference.Value;

/**
 * Test cases for the {@link AbstractComponent}
 * 
 * @author Ramon Servadei
 */
@SuppressWarnings("all")
public class ComponentJUnitTest {

    byte[][] headerBuffer = new byte[1][1];

    byte[][] writerDataBuffer = new byte[1][1];

    int[] headerBufferPosition = new int[1];

    int[] dataBufferPosition = new int[1];

    byte[] dataBuffer = new byte[1];

    int start = 0;

    int numberOfBytes = 1;

    Mockery context = new Mockery();

    final IOperationScope scope = context.mock(IOperationScope.class);

    JUnitComponent candidate;

    private String testIdentity;

    Mockery mocks = new JUnit4Mockery();

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        testIdentity = "JUnitComponent-" + System.nanoTime();
        candidate = new JUnitComponent((testIdentity));
    }

    @After
    public void tearDown() {
        context.assertIsSatisfied();
    }

    /**
     * Test method for
     * {@link fulmine.model.component.AbstractComponent#hashCode()}.
     */
    @Test
    public void testHashCode() {
        AbstractComponent other = new JUnitComponent(candidate.getIdentity());
        assertEquals(candidate.hashCode(), other.hashCode());
        other = new JUnitComponent(candidate.getIdentity());
        assertEquals(candidate.hashCode(), other.hashCode());
    }

    /**
     * Test method for
     * {@link fulmine.model.component.AbstractComponent#AbstractComponent(fulmine.model.key.String, IType, IDomain)}
     * .
     */
    @Test
    public void testConstructorString() {
        final String identity = (testIdentity);
        AbstractComponent testComponent = new JUnitComponent(identity);
        assertEquals(identity, testComponent.getIdentity());
    }

    /**
     * Test method for
     * {@link fulmine.model.component.AbstractComponent#readState(fulmine.protocol.wire.operation.IOperationScope, byte[], int, int)}
     * .
     */
    @Test
    public void testReadStateNoException() {
        candidate.readState = true;
        context.checking(new Expectations() {

            {
                one(scope).include(candidate);
                will(returnValue(true));
                one(scope).exiting(candidate, true);
            }
        });
        candidate.readState(scope, dataBuffer, start, numberOfBytes);
    }

    /**
     * Test method for
     * {@link fulmine.model.component.AbstractComponent#readState(fulmine.protocol.wire.operation.IOperationScope, byte[], int, int)}
     * .
     */
    @Test
    public void testReadStateWithException() {
        final Exception exception = new Exception();
        candidate.exception = exception;
        context.checking(new Expectations() {

            {
                one(scope).include(candidate);
                will(returnValue(true));
                one(scope).exception(candidate, exception);
            }
        });
        candidate.readState(scope, dataBuffer, start, numberOfBytes);
    }

    /**
     * Test method for
     * {@link fulmine.model.component.AbstractComponent#readState(fulmine.protocol.wire.operation.IOperationScope, byte[], int, int)}
     * .
     */
    @Test
    public void testReadStateIgnore() {
        context.checking(new Expectations() {

            {
                one(scope).include(candidate);
                will(returnValue(false));
            }
        });
        candidate.readState(scope, dataBuffer, start, numberOfBytes);
    }

    /**
     * Test method for
     * {@link fulmine.model.component.AbstractComponent#writeState(fulmine.protocol.wire.operation.IOperationScope, byte[][], int[], byte[][], int[])}
     * .
     */
    @Test
    public void testWriteStateNoException() {
        candidate.writeState = true;
        context.checking(new Expectations() {

            {
                one(scope).include(candidate);
                will(returnValue(true));
                one(scope).exiting(candidate, true);
            }
        });
        candidate.writeState(scope, WireIdentity.get((int) System.nanoTime()), headerBuffer, headerBufferPosition, writerDataBuffer, dataBufferPosition, false);
    }

    /**
     * Test method for
     * {@link fulmine.model.component.AbstractComponent#writeState(fulmine.protocol.wire.operation.IOperationScope, byte[][], int[], byte[][], int[])}
     * .
     */
    @Test
    public void testWriteStateException() {
        final Exception exception = new Exception();
        candidate.exception = exception;
        context.checking(new Expectations() {

            {
                one(scope).include(candidate);
                will(returnValue(true));
                one(scope).exception(candidate, exception);
            }
        });
        candidate.writeState(scope, WireIdentity.get((int) System.nanoTime()), headerBuffer, headerBufferPosition, writerDataBuffer, dataBufferPosition, false);
    }

    /**
     * Test method for
     * {@link fulmine.model.component.AbstractComponent#writeState(fulmine.protocol.wire.operation.IOperationScope, byte[][], int[], byte[][], int[])}
     * .
     */
    @Test
    public void testWriteStateIgnore() {
        context.checking(new Expectations() {

            {
                one(scope).include(candidate);
                will(returnValue(false));
            }
        });
        candidate.writeState(scope, WireIdentity.get((int) System.nanoTime()), headerBuffer, headerBufferPosition, writerDataBuffer, dataBufferPosition, false);
    }

    /**
     * Test method for
     * {@link fulmine.model.component.AbstractComponent#equals(java.lang.Object)}
     * .
     */
    @Test
    public void testEqualsObject() {
        AbstractComponent other = new JUnitComponent(candidate.getIdentity());
        assertEquals(candidate, other);
    }

    /**
     * Test method for
     * {@link fulmine.model.component.AbstractComponent#getIdentity()}.
     */
    @Test
    public void testGetIdentity() {
        assertEquals("context identity", this.testIdentity, candidate.getIdentity());
    }

    @Test
    public void testAddListener() {
        doAddListener(mocks.mock(IEventListener.class));
    }

    private void doAddListener(final IEventListener listener) {
        mocks.checking(new Expectations() {

            {
                one(listener).addedAsListenerFor(candidate);
            }
        });
        candidate.addListener(listener);
    }

    @Test
    public void testAddListener_concurrency() throws Exception {
        final IEventListener listener1 = mocks.mock(IEventListener.class);
        final IEventListener listener2 = mocks.mock(IEventListener.class);
        mocks.checking(new Expectations() {

            {
                one(listener1).addedAsListenerFor(candidate);
                one(listener2).addedAsListenerFor(candidate);
            }
        });
        candidate.addListener(listener1);
        final CountDownLatch iteratorLatch = new CountDownLatch(1);
        final CountDownLatch writerLatch = new CountDownLatch(1);
        final Value<Exception> failed = new Value<Exception>();
        Thread iterator = new Thread(new Runnable() {

            public void run() {
                final Iterator<IEventListener> iter = candidate.getListeners().iterator();
                writerLatch.countDown();
                try {
                    while (iter.hasNext()) {
                        iteratorLatch.await();
                        iter.next();
                    }
                } catch (Exception e) {
                    failed.set(e);
                }
            }
        });
        Thread writer = new Thread(new Runnable() {

            public void run() {
                try {
                    writerLatch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                candidate.addListener(listener2);
                iteratorLatch.countDown();
            }
        });
        iterator.start();
        writer.start();
        iterator.join();
        writer.join();
        if (failed.get() != null) {
            throw failed.get();
        }
    }

    @Test
    public void testUnregisterListener() {
        final IEventListener listener = mocks.mock(IEventListener.class);
        mocks.checking(new Expectations() {

            {
                exactly(1).of(listener).removedAsListenerFrom(candidate);
            }
        });
        doAddListener(listener);
        candidate.removeListener(listener);
        assertFalse("null listener", candidate.removeListener(null));
    }

    @Test
    public void testUnregisterListeners() {
        candidate.start();
        final IEventListener listener = mocks.mock(IEventListener.class);
        mocks.checking(new Expectations() {

            {
                exactly(1).of(listener).removedAsListenerFrom(candidate);
            }
        });
        doAddListener(listener);
        final List<IEventListener> result = candidate.removeListeners();
        assertEquals("size", 1, result.size());
        assertEquals("listener", listener, result.get(0));
        assertEquals("Did not remove listeners", Collections.emptyList(), candidate.removeListeners());
    }

    private class JUnitComponent extends AbstractComponent {

        public Exception exception;

        boolean readState, writeState;

        public JUnitComponent(String identity) {
            super(identity, Type.get(0), Domain.get(73));
        }

        @Override
        protected boolean doReadState(IOperationScope scope, byte[] buffer, int start, int numberOfBytes) throws Exception {
            if (exception != null) {
                throw exception;
            }
            return readState;
        }

        @Override
        protected boolean doWriteState(IOperationScope scope, IWireIdentity wireId, byte[][] headerBuffer, int[] headerBufferPosition, byte[][] dataBuffer, int[] dataBufferPosition, boolean completeState) throws Exception {
            if (exception != null) {
                throw exception;
            }
            return writeState;
        }
    }
}
