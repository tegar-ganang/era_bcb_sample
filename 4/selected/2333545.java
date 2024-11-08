package mnemosyne.core;

import mnemosyne.archiver.nonPersistentTestObjects.MockPersistentObjectFactory;
import mnemosyne.core.testObjects.NoDefaultConstructorTestObj;
import mnemosyne.core.testObjects.TestObj;
import mnemosyne.core.testObjects.TestObj2;
import mnemosyne.guid.Guid;
import mnemosyne.guid.GuidGenerator;
import mnemosyne.guid.RmiUidGuidGenerator;
import mnemosyne.lock.Lock;
import mnemosyne.lock.MockTransaction;
import mnemosyne.util.PersistenceRuntimeException;
import org.jmock.Mock;
import org.jmock.MockObjectTestCase;
import java.io.*;

/**
 * Unit test for class PersistentObjectImpl
 * 
 * @version $Id: PersistentObjectImplTest.java,v 1.3 2004/10/09 07:52:17 charlesblaxland Exp $
 */
public class PersistentObjectImplTest extends MockObjectTestCase {

    private PersistentObject persistentObject;

    private GuidGenerator guidGenerator;

    private VersionManager versionManager;

    private Enhancer enhancer;

    private Mock enhancerMock;

    private Lock lock;

    private Mock lockMock;

    private Guid guid;

    private PersistentObjectFactory persistentObjectFactory;

    protected void setUp() throws Exception {
        guidGenerator = new RmiUidGuidGenerator();
        versionManager = new VersionManager(new LongVersion(0));
        PersistentContext.reset(versionManager);
        enhancerMock = new Mock(Enhancer.class);
        enhancer = (Enhancer) enhancerMock.proxy();
        lockMock = new Mock(Lock.class);
        lock = (Lock) lockMock.proxy();
        guid = guidGenerator.generateGUID();
        persistentObject = new PersistentObjectImpl(guid, versionManager, enhancer, lock);
        persistentObjectFactory = new MockPersistentObjectFactory();
        AggregatedTransaction aggregatedTransaction = new AggregatedTransaction();
        ArchiveContext context = ArchiveContext.get();
        context.setAggregatedTransaction(aggregatedTransaction);
        context.setPersistentObjectFactory(persistentObjectFactory);
    }

    protected void tearDown() throws Exception {
        leaveTransaction();
    }

    public void testInitialize() {
        try {
            persistentObject.findTargetObject();
            fail("Exception expected");
        } catch (Exception e) {
        }
        TestObj2 obj = new TestObj2();
        persistentObject.initialize(obj);
        assertSame(obj, persistentObject.findTargetObject());
    }

    public void testInitializeAsNew() {
        Transaction transaction = enterTransaction();
        lockMock.expects(once()).method("acquireWriteLock").with(same(transaction));
        TestObj2 obj = new TestObj2();
        persistentObject.initializeAsNewObject(obj);
        lockMock.verify();
        lockMock.stubs().method("acquireWriteLock").with(same(transaction));
        assertSame(obj, persistentObject.findTargetObject());
    }

    public void testNonTransactionalFindObject() {
        setVersion(new LongVersion(10));
        TestObj obj = new TestObj();
        persistentObject.initialize(obj);
        ((PersistentObjectImpl) persistentObject).setMutableMethodsWorkaroundEnabled(false);
        Object targetObject = persistentObject.findTargetObject();
        assertSame(obj, targetObject);
        setVersion(new LongVersion(14));
        targetObject = persistentObject.findTargetObject();
        assertSame(obj, targetObject);
    }

    public void testNonTransactionMutatingMethodFindObject() {
        TestObj obj = new TestObj();
        persistentObject.initialize(obj);
        ((PersistentObjectImpl) persistentObject).setMutableMethodsWorkaroundEnabled(false);
        try {
            persistentObject.findWritableTargetObject();
            fail("Exception expected");
        } catch (NoTransactionException e) {
            assertTrue(true);
        }
    }

    public void testTransactionalFindObject() {
        Transaction transaction = enterTransaction();
        setVersion(new LongVersion(10));
        TestObj obj = new TestObj();
        persistentObject.initialize(obj);
        lockMock.expects(atLeastOnce()).method("acquireReadLock").with(same(transaction));
        ((PersistentObjectImpl) persistentObject).setMutableMethodsWorkaroundEnabled(false);
        Object targetObject = persistentObject.findTargetObject();
        assertSame(obj, targetObject);
        setVersion(new LongVersion(14));
        targetObject = persistentObject.findTargetObject();
        assertSame(obj, targetObject);
        lockMock.verify();
    }

    public void testTransactionalMutatingMethodFindObject() {
        Transaction transaction = enterTransaction();
        setVersion(new LongVersion(10));
        TestObj obj = new TestObj();
        obj.setI(324);
        persistentObject.initialize(obj);
        lockMock.expects(atLeastOnce()).method("acquireWriteLock").with(same(transaction));
        lockMock.expects(once()).method("acquireReadLock").with(same(transaction));
        ((PersistentObjectImpl) persistentObject).setMutableMethodsWorkaroundEnabled(false);
        TestObj targetObject = (TestObj) persistentObject.findWritableTargetObject();
        assertNotSame(obj, targetObject);
        assertEquals(obj.getI(), targetObject.getI());
        assertNotSame(obj.getObj2Array(), targetObject.getObj2Array());
        assertTrue(transaction.hasModifiedOrAddedObject(persistentObject));
        setVersion(new LongVersion(14));
        TestObj targetObject2 = (TestObj) persistentObject.findWritableTargetObject();
        assertNotSame(obj, targetObject2);
        assertSame(targetObject, targetObject2);
        assertEquals(obj.getI(), targetObject.getI());
        TestObj targetObj3 = (TestObj) persistentObject.findTargetObject();
        assertNotSame(obj, targetObj3);
        assertSame(targetObject, targetObj3);
        assertEquals(obj.getI(), targetObject.getI());
        lockMock.verify();
    }

    public void testPepareForCommit() {
        enhancerMock.expects(once()).method("enhance").withAnyArguments();
        lockMock.stubs().method("acquireWriteLock").withAnyArguments();
        enterTransaction();
        TestObj2 obj = new TestObj2();
        persistentObject.initializeAsNewObject(obj);
        persistentObject.prepareForCommit();
        enhancerMock.verify();
    }

    public void testCommit() {
        lockMock.stubs().method("acquireWriteLock").withAnyArguments();
        Transaction transaction = enterTransaction();
        lockMock.expects(once()).method("releaseLock").with(same(transaction));
        TestObj2 obj = new TestObj2();
        persistentObject.initializeAsNewObject(obj);
        Version prevVersion = new LongVersion(9);
        Version version = prevVersion.getNext();
        persistentObject.commit(version);
        leaveTransaction();
        setVersion(prevVersion);
        try {
            persistentObject.findTargetObject();
            fail("Exception expected");
        } catch (Exception e) {
        }
        setVersion(version);
        Object newObj = persistentObject.findTargetObject();
        assertSame(obj, newObj);
        lockMock.verify();
    }

    public void testRollback() {
        lockMock.stubs().method("acquireWriteLock").withAnyArguments();
        Transaction transaction = enterTransaction();
        lockMock.expects(once()).method("releaseLock").with(same(transaction));
        TestObj2 obj = new TestObj2();
        persistentObject.initializeAsNewObject(obj);
        Persistable persistable = (Persistable) obj;
        persistable.MN__setPersistentObject(persistentObject);
        persistentObject.rollback();
        assertNull(persistable.MN__getPersistentObject());
        lockMock.verify();
    }

    public void testSimpleWriteAndReadObject() throws Exception {
        TestObj obj = new TestObj();
        obj.setI(324);
        persistentObject.initialize(obj);
        AggregatedTransaction aggregatedTransaction = ArchiveContext.get().getAggregatedTransaction();
        aggregatedTransaction.setComplete(true);
        PersistentObject readPersistentObject = writeAndReadPersistentObject(persistentObject);
        TestObj readObj = (TestObj) readPersistentObject.findTargetObject();
        assertNotNull(readObj);
        assertEquals(obj.getI(), readObj.getI());
        assertSame(persistentObject, aggregatedTransaction.resolveObject(persistentObject));
    }

    public void testTransactionalWriteAndReadObject() throws Exception {
        TestObj obj = new TestObj();
        obj.setI(324);
        persistentObject.initialize(obj);
        AggregatedTransaction aggregatedTransaction = ArchiveContext.get().getAggregatedTransaction();
        aggregatedTransaction.setComplete(false);
        PersistentObject readPersistentObject = writeAndReadPersistentObject(persistentObject);
        TestObj readObj = (TestObj) readPersistentObject.findTargetObject();
        assertNotNull(readObj);
        assertEquals(obj.getI(), readObj.getI());
        assertNotSame(persistentObject, aggregatedTransaction.resolveObject(persistentObject));
    }

    public void testUnresolvedReferenceWriteAndReadObject() throws Exception {
        TestObj obj = new TestObj();
        obj.setI(324);
        persistentObject.initialize(obj);
        AggregatedTransaction aggregatedTransaction = ArchiveContext.get().getAggregatedTransaction();
        lockMock.stubs().method("acquireWriteLock").withAnyArguments();
        enterTransaction();
        PersistentObject readPersistentObject = writeAndReadPersistentObject(persistentObject);
        leaveTransaction();
        TestObj readObj = (TestObj) readPersistentObject.findTargetObject();
        assertNotNull(readObj);
        assertEquals(10, readObj.getI());
        assertNotSame(persistentObject, aggregatedTransaction.resolveObject(persistentObject));
        lockMock.verify();
    }

    public void testUnresolvedReferenceWriteAndReadObjectWithoutDefaultConstructorFails() throws Exception {
        NoDefaultConstructorTestObj obj = new NoDefaultConstructorTestObj(324);
        persistentObject.initialize(obj);
        lockMock.stubs().method("acquireWriteLock").withAnyArguments();
        enterTransaction();
        try {
            writeAndReadPersistentObject(persistentObject);
            fail("Exception expected");
        } catch (PersistenceRuntimeException e) {
            assertTrue(true);
        }
    }

    public void testHashCode() {
        int hash = persistentObject.hashCode();
        assertEquals(guid.hashCode(), hash);
    }

    public void testEquals() {
        assertTrue(persistentObject.equals(persistentObject));
        assertFalse(persistentObject.equals(new TestObj2()));
        PersistentObject persistentObject2 = new PersistentObjectImpl(guidGenerator.generateGUID(), versionManager, enhancer, lock);
        assertFalse(persistentObject.equals(persistentObject2));
    }

    private Transaction enterTransaction() {
        PersistentContext context = PersistentContext.get(versionManager);
        Transaction transaction = new MockTransaction();
        context.setTransaction(transaction);
        return transaction;
    }

    private void leaveTransaction() {
        PersistentContext context = PersistentContext.get(versionManager);
        context.setTransaction(null);
    }

    private void setVersion(Version version) {
        PersistentContext context = PersistentContext.get(versionManager);
        context.setVersion(version);
    }

    private PersistentObject writeAndReadPersistentObject(PersistentObject obj) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(obj);
        byte[] bytes = baos.toByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = new ObjectInputStream(bais);
        PersistentObject readPersistentObject = (PersistentObject) ois.readObject();
        return readPersistentObject;
    }
}
