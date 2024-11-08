package com.amazon.carbonado.repo.replicated;

import java.lang.reflect.Method;
import junit.framework.TestSuite;
import com.amazon.carbonado.TestUtilities;
import com.amazon.carbonado.gen.StorableInterceptorFactory;
import com.amazon.carbonado.stored.StorableTestBasic;
import com.amazon.carbonado.stored.StorableTestBasicIdMunger;
import com.amazon.carbonado.TestStorables;
import com.amazon.carbonado.Repository;
import com.amazon.carbonado.Storable;
import com.amazon.carbonado.Storage;
import com.amazon.carbonado.SupportException;
import com.amazon.carbonado.FetchException;
import com.amazon.carbonado.TestStorableBase;

public class TestProxiedStorable extends TestStorableBase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public static TestSuite suite() {
        return new TestSuite(TestProxiedStorable.class);
    }

    public TestProxiedStorable() {
        super();
    }

    /**
     * Test copy storable properties interface
     */
    public void test_copyStorableProperties() throws Exception {
        Storage<StorableTestBasic> storage = getRepository().storageFor(StorableTestBasic.class);
        StorableTestBasic storable = storage.prepare();
        TestStorables.InvocationTracker tracker = new TestStorables.InvocationTracker("tracker");
        storable.copyAllProperties(tracker);
        tracker.assertTrack(0);
        storable.setId(1);
        storable.setIntProp(1);
        storable.copyAllProperties(tracker);
        tracker.assertTrack(0x1 + 0x10);
        tracker.clearTracks();
        setBasicProperties(storable);
        storable.copyAllProperties(tracker);
        tracker.assertTrack(TestStorables.ALL_SET_METHODS);
        tracker.clearTracks();
        storable = storage.prepare();
        storable.copyPrimaryKeyProperties(tracker);
        tracker.assertTrack(0);
        setPrimaryKeyProperties(storable);
        storable.copyPrimaryKeyProperties(tracker);
        tracker.assertTrack(TestStorables.ALL_PRIMARY_KEYS);
        tracker.clearTracks();
        storable = storage.prepare();
        storable.copyUnequalProperties(tracker);
        tracker.assertTrack(0);
        storable.setIntProp(0);
        storable.copyUnequalProperties(tracker);
        tracker.assertTrack(0x8);
        storable.setIntProp(1);
        storable.copyUnequalProperties(tracker);
        tracker.assertTrack(0x8 + 0x10);
        storable = storage.prepare();
        storable.setStringProp("hi");
        storable.setId(22);
        storable.copyPrimaryKeyProperties(tracker);
        storable.copyDirtyProperties(tracker);
        tracker.assertTrack(0x1 + 0x4);
    }

    /**
     * Test Interceptor
     */
    public void test_Interceptor() throws Exception {
        Storage<StorableTestBasic> storage = getRepository().storageFor(StorableTestBasic.class);
        StorableTestBasic storable = storage.prepare();
        StorableInterceptorFactory<StorableTestBasic> proxyFactory = StorableInterceptorFactory.getInstance(StorableTestBasicIdMunger.class, StorableTestBasic.class, false);
        StorableTestBasic proxy = proxyFactory.create(storable);
        proxy.setId(1);
        assertEquals(storable.getId(), 2 << 8);
        storable.setId(5 << 8);
        assertEquals(proxy.getId(), 4);
        proxy.setStringProp("passthrough");
        assertEquals("passthrough", storable.getStringProp());
    }

    /**
     * Test replicatingStorable
     */
    public void test_replicatingStorable() throws Exception {
        Repository altRepo = TestUtilities.buildTempRepository("alt");
        final Storage<StorableTestBasic> readage = getRepository().storageFor(StorableTestBasic.class);
        final Storage<StorableTestBasic> writage = altRepo.storageFor(StorableTestBasic.class);
        Storage<StorableTestBasic> wrappage = new ReplicatedStorage<StorableTestBasic>(getRepository(), readage, writage);
        StorableTestBasic replicator = wrappage.prepare();
        replicator.setId(1);
        setBasicProperties(replicator);
        replicator.insert();
        StorableTestBasic reader = load(readage, 1);
        StorableTestBasic writer = load(writage, 1);
        assertTrue(reader.equalProperties(writer));
        assertStorableEquivalenceById(1, readage, writage);
        replicator = wrappage.prepare();
        replicator.setId(1);
        replicator.setStringProp("updated");
        replicator.setLongProp(2342332);
        replicator.update();
        writer = load(writage, 1);
        reader = load(readage, 1);
        assertTrue(reader.equalProperties(writer));
        replicator.delete();
        try {
            reader.load();
            fail("successfully loaded deleted 'read' storable");
        } catch (FetchException e) {
        }
        try {
            writer.load();
            fail("successfully loaded deleted 'write' storable");
        } catch (FetchException e) {
        }
        StorableTestBasic replicator2 = wrappage.prepare();
        replicator2.setId(2);
        setBasicProperties(replicator2);
        replicator2.insert();
        replicator.setId(2);
        replicator.delete();
        try {
            load(readage, 2);
            fail("successfully loaded deleted 'read' storable 2");
        } catch (FetchException e) {
        }
        try {
            load(writage, 2);
            fail("successfully loaded deleted 'write' storable 2");
        } catch (FetchException e) {
        }
        altRepo.close();
        altRepo = null;
    }
}
