package net.sf.joafip.store.service;

import java.util.Arrays;
import net.sf.joafip.store.service.objectfortest.Bob1;
import net.sf.joafip.store.service.objectfortest.Bob2;
import net.sf.joafip.store.service.objectfortest.BobArray;
import org.apache.log4j.Logger;

public class TestStore extends AbstractStoreTestCase {

    private static final Logger logger = Logger.getLogger(TestStore.class);

    private Store store;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        store = new Store(path);
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            store.close();
        } catch (Throwable throwable) {
        }
        super.tearDown();
    }

    /**
	 * test for root object management:<br>
	 * get root on empty file must failed<br>
	 * check set root on empty file<br>
	 * set root on not empty file must failed<br>
	 * 
	 * @throws StoreException
	 * 
	 */
    public void testRoot() throws StoreException {
        logger.info("testRoot");
        Object rootObject = store.getRoot(Bob1.class);
        assertNull("read root object must failed", rootObject);
        final Bob1 bob1 = new Bob1();
        store.setRoot(bob1);
        store.save();
        store.checkIntegrity();
        rootObject = store.getRoot(Bob1.class);
        assertNotNull("must have root", rootObject);
        logger.info("root object class " + rootObject.getClass());
        assertTrue("root object must be instance of Bob1", rootObject instanceof Bob1);
        assertNotSame("writed and readed objects must not be same object in memory", bob1, rootObject);
        final Object object = new Object();
        try {
            store.setRoot(object);
            fail("set root again must failed");
        } catch (StoreException exception) {
        }
    }

    /**
	 * test if reading object set data record
	 * 
	 * @throws StoreException
	 * 
	 * 
	 */
    public void testReadObjectSetDataRecord() throws StoreException {
        logger.info("testReadObjectSetDataRecord");
        final Bob1 bob1 = new Bob1();
        Bob2 bob2 = new Bob2();
        bob1.setBob2(bob2);
        bob2.setVal(0);
        store.setRoot(bob1);
        store.save();
        store.checkIntegrity();
        Bob1 rootObject = (Bob1) store.getRoot(Bob1.class);
        assertNotNull("must have root", rootObject);
        bob2 = rootObject.getBob2();
        assertNotNull("root object must have bob2 set", bob2);
        bob2.getVal();
        assertNotNull("bob2 object must have data record associated to it", store.getDataRecord(bob2));
    }

    /**
	 * test writing object having null array field
	 * 
	 * @throws StoreException
	 * 
	 */
    public void testEmptyArray() throws StoreException {
        logger.info("testEmptyArray");
        final BobArray bobArray = new BobArray();
        store.setRoot(bobArray);
        store.save();
        store.checkIntegrity();
        store.getRoot(BobArray.class);
    }

    /**
	 * test write and read of array
	 * 
	 * @throws StoreException
	 * 
	 */
    public void testNotEmptyArray() throws StoreException {
        logger.info("testNotEmptyArray2");
        final BobArray bobArray = new BobArray();
        final int[] originalValues = new int[] { 1, 2, 3 };
        bobArray.setValues(originalValues);
        store.setRoot(bobArray);
        store.save();
        BobArray readed = (BobArray) store.getRoot(BobArray.class);
        assertNotSame("must be a new instance", bobArray, readed);
        assertNotNull("must have root", readed);
        final int[] values = readed.getValues();
        assertTrue("arrays fiels must have same values", Arrays.equals(originalValues, values));
        logSize("after set root");
        readed.setValues(null);
        store.save();
        store.checkIntegrity();
        readed = (BobArray) store.getRoot(BobArray.class);
        assertNotNull("must have root", readed);
        assertNull("values must be null", readed.getValues());
        logSize("after modification");
    }

    /**
	 * NOT USE: see garbage<br>
	 * test for memory freeing<br>
	 * 
	 * @throws StoreException
	 * 
	 */
    public void xxxtestNotEmptyArrayFreeRecord() throws StoreException {
        logger.info("testNotEmptyArray3");
        final BobArray bobArray = new BobArray();
        store.setRoot(bobArray);
        final long baseUsed = store.usedSize();
        final int[] originalValues = new int[] { 1, 2, 3 };
        bobArray.setValues(originalValues);
        store.save();
        store.checkIntegrity();
        BobArray readed = (BobArray) store.getRoot(BobArray.class);
        assertNotNull("must have root", readed);
        final int[] values = readed.getValues();
        assertTrue("arrays fiels must have same values", Arrays.equals(originalValues, values));
        assertTrue("must have allocated byte", store.usedSize() > baseUsed);
        readed.setValues(null);
        store.save();
        store.checkIntegrity();
        assertEquals("must free in file deleted object", baseUsed, store.usedSize());
        readed = (BobArray) store.getRoot(BobArray.class);
        assertNotNull("must have root", readed);
        assertNull("values must be null", readed.getValues());
    }

    /**
	 * test for array size change
	 * 
	 * @throws StoreException
	 * 
	 * 
	 */
    public void testArraySizeChange() throws StoreException {
        logger.info("testNotEmptyArray3");
        final BobArray bobArray = new BobArray();
        store.setRoot(bobArray);
        final int[] originalValues = new int[] { 1, 2 };
        bobArray.setValues(originalValues);
        store.save();
        store.checkIntegrity();
        final long sizeFor2 = store.usedSize();
        BobArray readed = (BobArray) store.getRoot(BobArray.class);
        final int[] newValues = new int[] { 1, 2, 3, 4, 5, 6 };
        readed.setValues(newValues);
        store.save();
        store.checkIntegrity();
        final long sizeForALot = store.usedSize();
        readed = (BobArray) store.getRoot(BobArray.class);
        assertTrue("arrays must be equals", Arrays.equals(newValues, readed.getValues()));
        assertTrue("memory used must increase", sizeForALot > sizeFor2);
    }

    public void testCancelModification() throws StoreException {
        Bob1 bob1 = new Bob1();
        bob1.setVal(0);
        Bob2 bob2 = new Bob2();
        bob1.setBob2(bob2);
        bob2.setVal(0);
        store.setRoot(bob1);
        store.save();
        bob1 = (Bob1) store.getRoot(Bob1.class);
        bob1.setVal(1);
        bob2.setVal(1);
        store.doNotSave();
        bob1 = (Bob1) store.getRoot(Bob1.class);
        assertEquals("bob1 value must be unchanged", 0, bob1.getVal());
        bob2 = bob1.getBob2();
        assertEquals("bob2 value must be unchanged", 0, bob2.getVal());
    }

    /**
	 * @throws StoreException
	 */
    private void logSize(final String message) throws StoreException {
        final long totalSize = store.totalSize();
        final long freeSize = store.freeSize();
        final long usedSize = store.usedSize();
        logger.info(message + ": total=" + totalSize + " used=" + usedSize + " free=" + freeSize);
    }
}
