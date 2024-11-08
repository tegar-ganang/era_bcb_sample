package com.amazon.carbonado;

import junit.framework.TestCase;
import com.amazon.carbonado.stored.StorableTestBasic;

/**
 * @author Don Schneider
 */
public abstract class TestStorableBase extends TestCase {

    private Repository mRepository;

    public TestStorableBase() {
    }

    public TestStorableBase(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
        if (mRepository != null) {
            mRepository.close();
            mRepository = null;
        }
    }

    protected Repository getRepository() {
        if (mRepository == null) {
            mRepository = TestUtilities.buildTempRepository();
        }
        return mRepository;
    }

    protected void setPrimaryKeyProperties(StorableTestBasic proxy) {
        proxy.setId(10);
    }

    protected void setBasicProperties(StorableTestBasic proxy) {
        proxy.setStringProp("foo");
        proxy.setIntProp(10);
        proxy.setLongProp(120);
        proxy.setDoubleProp(1.2);
    }

    protected void assertStorableEquivalenceById(int id, Storage<StorableTestBasic> s1, Storage<StorableTestBasic> s2) throws FetchException {
        StorableTestBasic reader = s1.prepare();
        reader.setId(id);
        reader.load();
        StorableTestBasic writer = s2.prepare();
        writer.setId(id);
        writer.load();
        assertTrue(reader.equalProperties(writer));
    }

    protected StorableTestBasic load(Storage<StorableTestBasic> readage, int id) throws FetchException {
        StorableTestBasic item = readage.prepare();
        item.setId(id);
        item.load();
        return item;
    }
}
