package fulmine.model.container.impl;

import static fulmine.util.Utils.EQUALS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;
import org.apache.commons.lang.SystemUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import fulmine.Domain;
import fulmine.IDomain;
import fulmine.IType;
import fulmine.Type;
import fulmine.context.FulmineContext;
import fulmine.context.IFrameworkContext;
import fulmine.event.EventFrameExecution;
import fulmine.event.listener.EventListenerUtils;
import fulmine.event.system.TxEvent;
import fulmine.model.container.ContainerJUnitTest;
import fulmine.model.container.IContainer;
import fulmine.model.field.DoubleField;
import fulmine.model.field.IntegerField;
import fulmine.protocol.specification.FrameReader;
import fulmine.protocol.specification.FrameWriter;
import fulmine.util.collection.CollectionFactory;
import fulmine.util.log.AsyncLog;
import fulmine.util.reference.Value;

/**
 * Test cases for the {@link Record}
 * 
 * @author Ramon Servadei
 */
@SuppressWarnings("all")
public class RecordJUnitTest {

    public static final String FULMINE_CONTEXT_EVENT_MANAGER = "fulmine.context.EventManager";

    private static final AsyncLog LOG = new AsyncLog(RecordJUnitTest.class);

    private static final String REMOTE_CONTEXT_ID = "RecordJUnitTest-remoteContext";

    static final IDomain DOMAIN = Domain.get(69);

    private static final int SIZE = 10;

    Random rnd = new Random();

    private static final int ITERATIONS = 250;

    Record candidate, candidate2;

    IntegerField[] integerFields = new IntegerField[SIZE];

    IntegerField[] integerFields2 = new IntegerField[SIZE];

    DoubleField[] doubleFields = new DoubleField[SIZE];

    DoubleField[] doubleFields2 = new DoubleField[SIZE];

    IContainer result, ref;

    private static final IType RECORD_TYPE = Type.get(99);

    IFrameworkContext context = new FulmineContext(getClass().getSimpleName());

    final Level level = Logger.getLogger(FULMINE_CONTEXT_EVENT_MANAGER).getLevel();

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        context.start();
        Logger.getLogger(FULMINE_CONTEXT_EVENT_MANAGER).setLevel(Level.WARN);
        candidate = new Record(context.getIdentity(), "A", RECORD_TYPE, DOMAIN, context, true);
        candidate2 = new Record(context.getIdentity(), "B", RECORD_TYPE, DOMAIN, context, true);
        populate(candidate, integerFields, doubleFields);
        populate(candidate2, integerFields2, doubleFields2);
    }

    private void populate(Record record, IntegerField[] intFields, DoubleField[] dblFields) {
        record.start();
        for (int i = 0; i < intFields.length; i++) {
            intFields[i] = new IntegerField("intField" + i);
            record.add(intFields[i]);
        }
        for (int i = 0; i < dblFields.length; i++) {
            dblFields[i] = new DoubleField("doubleField" + i);
            record.add(dblFields[i]);
        }
        triggerFieldValues(1, intFields, dblFields);
    }

    private void triggerFieldValues(int override, IntegerField[] intFields, DoubleField[] dblFields) {
        for (int i = 0; i < intFields.length; i++) {
            intFields[i].set(intFields[i].get() + 1);
        }
        for (int i = 0; i < dblFields.length; i++) {
            dblFields[i].set(dblFields[i].get() + 1);
        }
    }

    private void randomTriggerFieldValues(IntegerField[] intFields, DoubleField[] dblFields) {
        for (int i = 0; i < intFields.length; i++) {
            if (rnd.nextBoolean()) {
                intFields[i].set(rnd.nextInt());
            }
        }
        for (int i = 0; i < dblFields.length; i++) {
            if (rnd.nextBoolean()) {
                dblFields[i].set(rnd.nextDouble());
            }
        }
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        context.destroy();
        Logger.getLogger(FULMINE_CONTEXT_EVENT_MANAGER).setLevel(level);
    }

    @Test
    public void testFrameworkSerialisation() throws InterruptedException {
        ContainerJUnitTest.JUnitListener listener = new ContainerJUnitTest.JUnitListener("FrameworkSerialisation1", EventListenerUtils.createFilter(Record.class, TxEvent.class));
        candidate.addListener(listener);
        candidate.markForRemoteSubscription();
        ContainerJUnitTest.JUnitListener remoteListener = new ContainerJUnitTest.JUnitListener("FrameworkSerialisation2", EventListenerUtils.createFilter(Record.class));
        final IContainer remoteContainer = context.getRemoteContainer(REMOTE_CONTEXT_ID, candidate.getIdentity(), candidate.getType(), DOMAIN);
        remoteContainer.addListener(remoteListener);
        final Value<IContainer> result = new Value<IContainer>();
        for (int i = 0; i < 20; i++) {
            listener.reset();
            listener.setExpectedUpdateCount(2);
            remoteListener.reset();
            triggerFieldValues(i, integerFields, doubleFields);
            candidate.flushFrame();
            listener.waitForUpdate();
            assertEquals("Event ", candidate, listener.container);
            final byte[] frame = listener.data.getBuffer();
            Runnable task = new Runnable() {

                public void run() {
                    result.set(new FrameReader().read(frame, REMOTE_CONTEXT_ID, context));
                }
            };
            Thread t = new Thread(task);
            t.start();
            t.join();
            remoteListener.waitForUpdate();
            assertNotSame("Failed to write/read container", candidate, result.get());
            assertEquals("Failed to write/read container", candidate, result.get());
        }
    }

    private void doFrameworkSerialisationTest(final Stats stats, boolean deltas, Record record, IntegerField[] intFields, DoubleField[] dblFields) throws Exception {
        ContainerJUnitTest.JUnitListener listener = new ContainerJUnitTest.JUnitListener("FrameworkSerialisation3", EventListenerUtils.createFilter(Record.class, TxEvent.class));
        record.addListener(listener);
        record.markForRemoteSubscription();
        ContainerJUnitTest.JUnitListener remoteListener = new ContainerJUnitTest.JUnitListener("FrameworkSerialisation4", EventListenerUtils.createFilter(Record.class));
        final IContainer remoteContainer = context.getRemoteContainer(REMOTE_CONTEXT_ID, record.getIdentity(), record.getType(), DOMAIN);
        remoteContainer.addListener(remoteListener);
        final Value<IContainer> result = new Value<IContainer>();
        for (int i = 0; i < ITERATIONS; i++) {
            listener.reset();
            listener.setExpectedUpdateCount(2);
            remoteListener.reset();
            if (deltas) {
                randomTriggerFieldValues(intFields, dblFields);
            } else {
                triggerFieldValues(i, intFields, dblFields);
            }
            record.flushFrame();
            listener.waitForUpdate();
            assertEquals("Event ", record, listener.container);
            final byte[] frame = listener.data.getBuffer();
            stats.start("write-framework", 0);
            stats.stop("write-framework", listener.data.getBufferCreateTime());
            stats.dataSize(frame.length);
            Runnable task = new Runnable() {

                public void run() {
                    stats.start("read-framework", System.nanoTime());
                    result.set(new FrameReader().read(frame, REMOTE_CONTEXT_ID, context));
                    stats.stop("read-framework", System.nanoTime());
                }
            };
            Thread t = new Thread(task);
            t.start();
            t.join();
            remoteListener.waitForUpdate();
            assertNotSame("Failed to write/read container", record, result.get());
            assertEquals("Failed to write/read container", record, result.get());
        }
        stats.setCount(ITERATIONS);
    }

    @SuppressWarnings("unchecked")
    private void doJavaSerializationTest(Stats stats, boolean deltas) throws Exception {
        Map<String, Object> map = CollectionFactory.newMap();
        Map<String, Object> resultMap = null;
        for (int i = 0; i < integerFields.length; i++) {
            map.put(integerFields[i].getIdentity(), integerFields[i].get());
        }
        for (int i = 0; i < doubleFields.length; i++) {
            map.put(doubleFields[i].getIdentity(), doubleFields[i].get());
        }
        for (int i = 0; i < ITERATIONS; i++) {
            ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
            ObjectOutputStream outStream = new ObjectOutputStream(byteOutput);
            stats.start("write-java", System.nanoTime());
            outStream.writeObject(map);
            stats.stop("write-java", System.nanoTime());
            final byte[] byteArray = byteOutput.toByteArray();
            ObjectInputStream inStream = new ObjectInputStream(new ByteArrayInputStream(byteArray));
            stats.start("read-java", System.nanoTime());
            resultMap = (Map<String, Object>) inStream.readObject();
            stats.stop("read-java", System.nanoTime());
            stats.dataSize(byteArray.length);
        }
        stats.setCount(ITERATIONS);
        assertNotSame("Failed to write/read resultMap", map, resultMap);
        assertEquals("Failed to write/read resultMap", map, resultMap);
    }

    @Test
    public void testGet() throws Exception {
    }

    @Test
    public void testSerialisePerformanceImage() throws Exception {
        final Stats javaStats = new Stats();
        doJavaSerializationTest(javaStats, false);
        final Stats frameworkStats = new Stats();
        LOG.info("RecordJUnitTest.testSerialisePerformanceImage: IMAGE javaStats=" + javaStats);
        final Value<Exception> exception = new Value<Exception>();
        Thread t1 = new Thread(new Runnable() {

            public void run() {
                try {
                    doFrameworkSerialisationTest(frameworkStats, false, candidate, integerFields, doubleFields);
                    LOG.info("RecordJUnitTest.testSerialisePerformanceImage: IMAGE frameworkStats=" + frameworkStats);
                } catch (Exception e) {
                    exception.set(e);
                }
            }
        });
        final Stats frameworkStats2 = new Stats();
        Thread t2 = new Thread(new Runnable() {

            public void run() {
                try {
                    doFrameworkSerialisationTest(frameworkStats2, false, candidate2, integerFields2, doubleFields2);
                } catch (Exception e) {
                    exception.set(e);
                }
            }
        });
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        if (exception.get() != null) {
            throw exception.get();
        }
        doComparison(javaStats, frameworkStats);
    }

    @Test
    public void testSerialisePerformanceDeltas() throws Exception {
        final Stats javaStats = new Stats();
        doJavaSerializationTest(javaStats, true);
        LOG.info("RecordJUnitTest.testSerialisePerformanceDeltas: DELTAS javaStats=" + javaStats);
        final Stats frameworkStats = new Stats();
        final Value<Exception> exception = new Value<Exception>();
        Thread t1 = new Thread(new Runnable() {

            public void run() {
                try {
                    doFrameworkSerialisationTest(frameworkStats, true, candidate, integerFields, doubleFields);
                    LOG.info("RecordJUnitTest.testSerialisePerformanceDeltas: DELTAS frameworkStats=" + frameworkStats);
                } catch (Exception e) {
                    exception.set(e);
                }
            }
        });
        final Stats frameworkStats2 = new Stats();
        Thread t2 = new Thread(new Runnable() {

            public void run() {
                try {
                    doFrameworkSerialisationTest(frameworkStats2, true, candidate2, integerFields2, doubleFields2);
                } catch (Exception e) {
                    exception.set(e);
                }
            }
        });
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        if (exception.get() != null) {
            throw exception.get();
        }
        doComparison(javaStats, frameworkStats);
    }

    private void doComparison(Stats javaStats, Stats frameworkStats) {
        double readGain = (javaStats.avg("read-java") - frameworkStats.avg("read-framework")) / javaStats.avg("read-java");
        readGain = ((long) (readGain * 10000)) / 100d;
        double writeGain = (javaStats.avg("write-java") - frameworkStats.avg("write-framework")) / javaStats.avg("write-java");
        writeGain = ((long) (writeGain * 10000)) / 100d;
        double byteSaving = (double) (javaStats.length - frameworkStats.length) / javaStats.length;
        byteSaving = ((long) (byteSaving * 10000)) / 100d;
        LOG.info("RecordJUnitTest.doComparison: readGain=" + readGain + "%" + " writeGain=" + writeGain + "%" + " byteSaving=" + byteSaving + "%");
    }

    @Test
    public void testSerialiseValidate() throws InterruptedException {
        doWriteReadTest();
    }

    @Test
    public void testSerialiseChangedDefinition() throws InterruptedException {
        candidate = (Record) context.getLocalContainer("testSerialiseChangedDefinition", RECORD_TYPE, DOMAIN);
        ref = candidate;
        candidate.start();
        candidate.beginFrame(new EventFrameExecution());
        final IntegerField field1 = new IntegerField("first");
        candidate.add(field1);
        doWriteReadTest(true);
        candidate.beginFrame(new EventFrameExecution());
        final IntegerField integerField = new IntegerField("added");
        candidate.add(integerField);
        integerField.set(133);
        doWriteReadTest(true);
        LOG.info("RecordJUnitTest record.toString()        =" + candidate);
        LOG.info("RecordJUnitTest record.toIdentityString()=" + candidate.toIdentityString());
        LOG.info("RecordJUnitTest record.toDetailedString()=" + candidate.toDetailedString());
    }

    @Test
    public void testInvalidUserCode() {
        try {
            new Record(context.getIdentity(), "invalid", Type.get(1), DOMAIN, context, true);
            fail("Should throw exception for invalid type code");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testNonExistingField() {
        assertNull(candidate.get("this is null"));
    }

    private void doWriteReadTest() throws InterruptedException {
        doWriteReadTest(false);
    }

    private void doWriteReadTest(boolean hasLock) throws InterruptedException {
        doTestWithCompleteState(false, hasLock);
        doTestWithCompleteState(true, hasLock);
    }

    private void doTestWithCompleteState(boolean completeState, boolean hasLock) throws InterruptedException {
        final byte[] frame = completeState ? new FrameWriter().writeComplete(candidate) : ContainerJUnitTest.getFrame(context, candidate, hasLock, true);
        if (completeState) {
            context.removeContainer((IContainer) result);
            assertFalse("remote reference was removed again", context.removeContainer((IContainer) result));
        }
        result = ContainerJUnitTest.readFrame(context, candidate, frame);
        assertFalse("Not remote!", result.isLocal());
        assertNotSame("Failed to write/read container", candidate, result);
        assertEquals("Failed to write/read container", candidate, result);
    }

    private class Stats {

        private Map<String, Long> start = CollectionFactory.newMap();

        private Map<String, Long> avg = CollectionFactory.newMap();

        private int length;

        private int iterations;

        public void start(String id, long start) {
            this.start.put(id, start);
        }

        public void setCount(int iterations) {
            this.iterations = iterations;
        }

        public void dataSize(int length) {
            this.length = length;
        }

        public void stop(String id, long stop) {
            long diff = stop - start.get(id);
            if (this.avg.containsKey(id)) {
                this.avg.put(id, diff + this.avg.get(id));
            } else {
                this.avg.put(id, diff);
            }
        }

        public double avg(String id) {
            return (double) this.avg.get(id) / this.iterations;
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            final Set<Entry<String, Long>> entrySet = avg.entrySet();
            for (Entry<String, Long> entry : entrySet) {
                sb.append(SystemUtils.LINE_SEPARATOR);
                sb.append(entry.getKey()).append(EQUALS).append((long) ((double) entry.getValue()) / (this.iterations * 1000)).append("us");
            }
            sb.append(SystemUtils.LINE_SEPARATOR);
            sb.append("size=").append(this.length).append("b");
            return sb.toString();
        }
    }
}
