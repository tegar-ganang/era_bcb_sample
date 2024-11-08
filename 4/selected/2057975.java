package net.sf.joafip.heapfile.record.service;

import java.io.File;
import java.lang.reflect.Field;
import org.apache.log4j.Logger;
import net.sf.joafip.heapfile.record.entity.HeapFreeNode;
import net.sf.joafip.heapfile.record.entity.HeapHeader;
import net.sf.joafip.heapfile.record.entity.HeapIdNode;
import net.sf.joafip.heapfile.record.entity.HeapRecord;
import net.sf.joafip.heapfile.record.service.FileForStorable;
import net.sf.joafip.heapfile.record.service.HeapElementManager;
import net.sf.joafip.heapfile.service.HeapException;
import net.sf.joafip.redblacktree.entity.IRBTNode;
import net.sf.joafip.redblacktree.service.RBTException;
import junit.framework.TestCase;

public class TestHeapElement extends TestCase {

    private final Logger logger = Logger.getLogger(TestHeapElement.class);

    private FileForStorable fileForStorable;

    private FileForStorable backupFileForStorable;

    private File stateDataFile;

    private File globalStateFile;

    private File stateDataBackupFile;

    private HeapElementManager heapElementManager;

    private long record1pos;

    private long record2pos;

    private HeapRecord heapRecordAppened1;

    private HeapRecord heapRecordAppened2;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        final File dataFile = new File("runtime" + File.separator + "test.bin");
        dataFile.delete();
        final File backupFile = new File("runtime" + File.separator + "backup.bin");
        backupFile.delete();
        stateDataBackupFile = new File("runtime" + File.separator + "backup.flag");
        stateDataFile = new File("runtime" + File.separator + "data.flag");
        fileForStorable = new FileForStorable(dataFile);
        backupFileForStorable = new FileForStorable(backupFile);
        globalStateFile = new File("runtime" + File.separator + "global.flag");
        heapElementManager = new HeapElementManager(fileForStorable, stateDataFile, backupFileForStorable, stateDataBackupFile, globalStateFile);
    }

    @Override
    protected void tearDown() throws Exception {
        if (fileForStorable.isOpened()) {
            fileForStorable.close();
        }
        super.tearDown();
    }

    /**
	 * test creation of the heap header in heap file
	 * 
	 * @throws HeapException
	 * 
	 */
    public void testEmptyHeap() throws HeapException {
        final HeapHeader heapHeader1 = appendHeader();
        heapElementManager.open();
        heapElementManager.openTransaction();
        getHeapHeader(heapHeader1);
        heapElementManager.closeTransaction();
        heapElementManager.close();
    }

    /**
	 * test header modification saved in heap file
	 * 
	 * @throws HeapException
	 * 
	 */
    public void testChangeHeader() throws HeapException {
        final HeapHeader heapHeader1 = appendHeader();
        heapElementManager.open();
        heapElementManager.openTransaction();
        final HeapHeader heapHeader2 = getHeapHeader(heapHeader1);
        HeapRecord heapRecord = new HeapRecord(heapElementManager, HeapHeader.HEAP_HEADER_SIZE, false);
        heapHeader2.setHeapFreeRootNode((HeapFreeNode) heapRecord.getFreeNode());
        heapHeader2.setHeapIdRootNode((HeapIdNode) heapRecord.getIdNode());
        heapElementManager.closeTransaction();
        heapElementManager.openTransaction();
        final HeapHeader heapHeader3 = getHeapHeader(heapHeader2);
        assertEquals("FreeRootNodeFilePosition must be " + HeapHeader.HEAP_HEADER_SIZE, HeapHeader.HEAP_HEADER_SIZE, heapHeader3.getFreeRootNodeFilePosition());
        assertEquals("IdRootNodeFilePosition must be " + HeapHeader.HEAP_HEADER_SIZE, HeapHeader.HEAP_HEADER_SIZE, heapHeader3.getIdRootNodeFilePosition());
        heapElementManager.closeTransaction();
        heapElementManager.close();
    }

    /**
	 * test append done correctly
	 * 
	 * @throws HeapException
	 * 
	 */
    public void testAppendRecord() throws HeapException {
        appendRecord();
        heapElementManager.open();
        heapElementManager.openTransaction();
        int dataSize = 100;
        HeapRecord heapRecord1 = heapElementManager.readHeapFileRecord(record1pos, true);
        assertEquals("bad record position", record1pos, heapRecord1.getPositionInFile());
        assertEquals("bad record data size", dataSize, heapRecord1.getDataAssociatedSize());
        byte[] data1 = heapRecord1.getDataAssociated();
        for (int index = 0; index < dataSize; index++) {
            assertEquals("bad record data", index, data1[index]);
        }
        logger.debug("heap record1\n" + heapRecord1.toString());
        assertEquals("readed must be equals appened", heapRecordAppened1, heapRecord1);
        HeapRecord heapRecord2 = heapElementManager.readHeapFileRecord(record2pos, true);
        assertEquals("bad record position", record2pos, heapRecord2.getPositionInFile());
        assertEquals("bad record data size", dataSize, heapRecord2.getDataAssociatedSize());
        byte[] data2 = heapRecord2.getDataAssociated();
        assertFalse("heap record 1 and 2 must not have the same data", data1 == data2);
        for (int index = 0; index < 100; index++) {
            assertEquals("bad record data", index, data2[index]);
        }
        logger.debug("heap record2\n" + heapRecord2.toString());
        assertEquals("readed must be equals appened", heapRecordAppened2, heapRecord2);
        heapElementManager.closeTransaction();
        heapElementManager.close();
    }

    public void testUpdateDataRecord() throws HeapException {
        appendRecord();
        long pos = record1pos;
        heapElementManager.open();
        heapElementManager.openTransaction();
        HeapRecord heapRecord1 = heapElementManager.readHeapFileRecord(pos, true);
        byte[] data1 = heapRecord1.getDataAssociated();
        for (int index = 0; index < data1.length; index++) {
            data1[index] = (byte) (data1.length - index);
        }
        heapRecord1.setValueIsChanged();
        heapElementManager.closeTransaction();
        heapElementManager.openTransaction();
        heapRecord1 = heapElementManager.readHeapFileRecord(pos, true);
        data1 = heapRecord1.getDataAssociated();
        for (int index = 0; index < data1.length; index++) {
            assertEquals("bad record data", data1.length - index, data1[index]);
        }
        heapElementManager.closeTransaction();
        heapElementManager.close();
    }

    public void testUpdateNodeRecord() throws HeapException, RBTException {
        appendRecord();
        long pos = record1pos;
        heapElementManager.open();
        heapElementManager.openTransaction();
        HeapRecord heapRecord1 = heapElementManager.readHeapFileRecord(pos, true);
        IRBTNode node = heapRecord1.getIdNode();
        boolean color = node.getColor();
        node.setColor(!color);
        heapElementManager.closeTransaction();
        heapElementManager.openTransaction();
        heapRecord1 = heapElementManager.readHeapFileRecord(pos, true);
        node = heapRecord1.getIdNode();
        assertEquals("color must have changed", !color, node.getColor());
        heapElementManager.closeTransaction();
        heapElementManager.openTransaction();
        heapRecord1 = heapElementManager.readHeapFileRecord(pos, false);
        heapRecord1.freeRecord();
        node = heapRecord1.getFreeNode();
        color = node.getColor();
        node.setColor(!color);
        heapElementManager.closeTransaction();
        heapElementManager.openTransaction();
        heapRecord1 = heapElementManager.readHeapFileRecord(pos, false);
        node = heapRecord1.getFreeNode();
        assertEquals("color must have changed", !color, node.getColor());
        heapElementManager.closeTransaction();
        heapElementManager.close();
    }

    /**
	 * append heap header in heap empty file
	 * 
	 * @return appened heap header
	 * @throws HeapException
	 * 
	 */
    private HeapHeader appendHeader() throws HeapException {
        heapElementManager.open();
        heapElementManager.openTransaction();
        HeapHeader heapHeader1 = getHeapHeader(null);
        assertEquals("FreeRootNodeFilePosition must be -1", -1, heapHeader1.getFreeRootNodeFilePosition());
        assertEquals("IdRootNodeFilePosition must be -1", -1, heapHeader1.getIdRootNodeFilePosition());
        assertFalse("at creation there is no value change for saving", heapHeader1.isValueChanged());
        heapHeader1.setValueIsChanged();
        heapElementManager.closeTransaction();
        heapElementManager.close();
        assertFalse("saving implies no more value changed", heapHeader1.isValueChanged());
        assertFalse("just created must be false", heapHeader1.isJustCreated());
        return heapHeader1;
    }

    /**
	 * append two records
	 * 
	 * @throws HeapException
	 */
    private void appendRecord() throws HeapException {
        appendHeader();
        heapElementManager.open();
        heapElementManager.openTransaction();
        record1pos = HeapHeader.HEAP_HEADER_SIZE;
        int dataSize = 100;
        heapRecordAppened1 = heapElementManager.newHeapFileRecord(record1pos, 0L, 0L, false, true, dataSize, dataSize + HeapRecord.RECORD_HEADER_SIZE + 4);
        byte[] data = heapRecordAppened1.getDataAssociated();
        assertNull("there is no data associated to record", data);
        byte[] testData = new byte[dataSize];
        for (int index = 0; index < dataSize; index++) {
            testData[index] = (byte) index;
        }
        heapRecordAppened1.setDataAssociated(testData);
        heapElementManager.closeTransaction();
        logger.debug("appened heap record1\n" + heapRecordAppened1.toString());
        heapElementManager.openTransaction();
        record2pos = record1pos + heapRecordAppened1.getAreaSize();
        heapRecordAppened2 = heapElementManager.newHeapFileRecord(record2pos, 0L, 0L, false, true, dataSize, dataSize + HeapRecord.RECORD_HEADER_SIZE + 4);
        heapRecordAppened2.setDataAssociated(testData);
        heapElementManager.closeTransaction();
        heapElementManager.close();
        logger.debug("appened heap record2\n" + heapRecordAppened2.toString());
    }

    /**
	 * check good heapHeader state after reading or writing in heap file
	 * 
	 * @param heapHeader
	 * @throws HeapException
	 */
    private void checkHeapHeaderJustReadWriteState(HeapHeader heapHeader) throws HeapException {
        assertFalse("reading implies no value change for saving", heapHeader.isValueChanged());
        assertTrue("just created must be false", heapHeader.isJustCreated());
    }

    /**
	 * check good heapHeader state after changing value
	 * 
	 * @param heapHeader
	 * @throws HeapException
	 */
    private void checkHeapHeaderValueChangeState(HeapHeader heapHeader) throws HeapException {
        assertTrue("modification implies value change for saving", heapHeader.isValueChanged());
        assertFalse("just created must be false", heapHeader.isJustCreated());
    }

    /**
	 * get heap header and check its position
	 * 
	 * @param heapHeaderReference
	 * 
	 * @return heap header
	 * @throws HeapException
	 */
    private HeapHeader getHeapHeader(final HeapHeader heapHeaderReference) throws HeapException {
        HeapHeader heapHeader = heapElementManager.getHeapHeader();
        assertEquals("header must be at file beginning", 0, heapHeader.getPositionInFile());
        if (heapHeaderReference != null) {
            assertNotSame("new header must not be same of creation one", heapHeaderReference, heapHeader);
            assertEquals("writed and readed must be equals", heapHeaderReference, heapHeader);
        }
        return heapHeader;
    }
}
