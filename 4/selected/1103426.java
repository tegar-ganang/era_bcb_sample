package net.sf.joafip.heapfile.record.service;

import java.io.File;
import net.sf.joafip.AbstractDeleteFileTestCase;
import net.sf.joafip.TestConstant;
import net.sf.joafip.file.service.FileForStorable;
import net.sf.joafip.heapfile.record.entity.DataRecordIdentifier;
import net.sf.joafip.heapfile.record.entity.HeapFreeNode;
import net.sf.joafip.heapfile.record.entity.HeapHeader;
import net.sf.joafip.heapfile.record.entity.HeapIdNode;
import net.sf.joafip.heapfile.record.entity.HeapRecord;
import net.sf.joafip.heapfile.service.HeapException;
import net.sf.joafip.redblacktree.entity.IRBTNode;
import net.sf.joafip.redblacktree.service.RBTException;

public class TestHeapElementManager extends AbstractDeleteFileTestCase {

    private static final String RUNTIME = TestConstant.RUNTIME_DIR;

    private FileForStorable fileForStorable;

    private FileForStorable backupFileForStorable;

    private HeapElementManager heapElementManager;

    /** first record position */
    private long record1pos;

    /** second record position */
    private long record2pos;

    /** first record appened */
    private HeapRecord heapRecordAppened1;

    /** second record appened */
    private HeapRecord heapRecordAppened2;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        final File dataFile = new File(RUNTIME + File.separator + "test.dat");
        final File backupFile = new File(RUNTIME + File.separator + "backup.dat");
        final File stateDataBackupFile;
        stateDataBackupFile = new File(RUNTIME + File.separator + "backup.flag");
        final File stateDataFile;
        stateDataFile = new File(RUNTIME + File.separator + "data.flag");
        fileForStorable = new FileForStorable(dataFile);
        backupFileForStorable = new FileForStorable(backupFile);
        final File globalStateFile;
        globalStateFile = new File(RUNTIME + File.separator + "global.flag");
        heapElementManager = new HeapElementManager(fileForStorable, stateDataFile, backupFileForStorable, stateDataBackupFile, globalStateFile, false);
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            if (fileForStorable.isOpened()) {
                fileForStorable.close();
            }
        } catch (Throwable throwable) {
        }
        try {
            if (backupFileForStorable.isOpened()) {
                backupFileForStorable.close();
            }
        } catch (Throwable throwable) {
        }
        super.tearDown();
    }

    /**
	 * test creation of the heap header in empty heap file
	 * 
	 * @throws HeapException
	 * 
	 */
    public void testEmptyHeap() throws HeapException {
        final HeapHeader heapHeader1 = appendHeaderInEmptyFile();
        heapElementManager.open();
        heapElementManager.openTransaction();
        getHeapHeaderAndCheckPosition(heapHeader1);
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
        final HeapHeader heapHeader1 = appendHeaderInEmptyFile();
        heapElementManager.open();
        heapElementManager.openTransaction();
        final HeapHeader heapHeader2 = getHeapHeaderAndCheckPosition(heapHeader1);
        final HeapRecord heapRecord = new HeapRecord(heapElementManager, HeapHeader.HEAP_HEADER_SIZE, false);
        heapHeader2.setHeapFreeRootNode(new HeapFreeNode(heapElementManager, heapRecord));
        heapHeader2.setHeapIdRootNode(new HeapIdNode(heapElementManager, heapRecord));
        heapElementManager.closeTransaction();
        heapElementManager.openTransaction();
        final HeapHeader heapHeader3 = getHeapHeaderAndCheckPosition(heapHeader2);
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
        final int dataSize = 100;
        final HeapRecord heapRecord1 = heapElementManager.readHeapFileDataRecord(record1pos, true);
        assertEquals("bad record position", record1pos, heapRecord1.getPositionInFile());
        assertEquals("bad record data size", dataSize, heapRecord1.getDataAssociatedSize().intValue());
        final byte[] data1 = heapRecord1.getDataAssociated();
        for (int index = 0; index < dataSize; index++) {
            assertEquals("bad record data", index, data1[index]);
        }
        _log.debug("heap record1\n" + heapRecord1.toString());
        assertEquals("readed must be equals appened", heapRecordAppened1, heapRecord1);
        final HeapRecord heapRecord2 = heapElementManager.readHeapFileDataRecord(record2pos, true);
        assertEquals("bad record position", record2pos, heapRecord2.getPositionInFile());
        assertEquals("bad record data size", dataSize, heapRecord2.getDataAssociatedSize().intValue());
        final byte[] data2 = heapRecord2.getDataAssociated();
        assertNotSame("heap record 1 and 2 must not have the same data", data1, data2);
        for (int index = 0; index < 100; index++) {
            assertEquals("bad record data", index, data2[index]);
        }
        _log.debug("heap record2\n" + heapRecord2.toString());
        assertEquals("readed must be equals appened", heapRecordAppened2, heapRecord2);
        heapElementManager.closeTransaction();
        heapElementManager.close();
    }

    public void testUpdateDataRecord() throws HeapException {
        appendRecord();
        final long pos = record1pos;
        heapElementManager.open();
        heapElementManager.openTransaction();
        HeapRecord heapRecord1 = heapElementManager.readHeapFileDataRecord(pos, true);
        byte[] data1 = heapRecord1.getDataAssociated();
        for (int index = 0; index < data1.length; index++) {
            data1[index] = (byte) (data1.length - index);
        }
        heapRecord1.setValueIsChanged();
        heapElementManager.closeTransaction();
        heapElementManager.openTransaction();
        heapRecord1 = heapElementManager.readHeapFileDataRecord(pos, true);
        data1 = heapRecord1.getDataAssociated();
        for (int index = 0; index < data1.length; index++) {
            assertEquals("bad record data", data1.length - index, data1[index]);
        }
        heapElementManager.closeTransaction();
        heapElementManager.close();
    }

    public void testUpdateNodeRecord() throws HeapException, RBTException {
        appendRecord();
        final long pos = record1pos;
        heapElementManager.open();
        heapElementManager.openTransaction();
        HeapRecord heapRecord1 = heapElementManager.readHeapFileDataRecord(pos, true);
        try {
            heapRecord1.getFreeNode();
            fail("must not obtain free node");
        } catch (HeapException exception) {
        }
        IRBTNode node = heapRecord1.getIdNode();
        boolean color;
        if (node.isColorSetted()) {
            color = node.getColor();
        } else {
            color = true;
        }
        node.setColor(!color);
        assertTrue("value must have changed", heapRecord1.isValueChanged());
        heapElementManager.closeTransaction();
        heapElementManager.openTransaction();
        heapRecord1 = heapElementManager.readHeapFileDataRecord(pos, true);
        node = heapRecord1.getIdNode();
        assertEquals("color must have changed", !color, node.getColor());
        assertFalse("not modified record", heapRecord1.isValueChanged());
        heapElementManager.closeTransaction();
        heapElementManager.openTransaction();
        heapRecord1 = heapElementManager.readHeapFileDataRecord(pos, false);
        heapRecord1.freeRecord();
        try {
            heapRecord1.getIdNode();
            fail("must not obtain id node");
        } catch (HeapException exception) {
        }
        node = heapRecord1.getFreeNode();
        if (node.isColorSetted()) {
            color = node.getColor();
        } else {
            color = true;
        }
        node.setColor(!color);
        heapElementManager.closeTransaction();
        heapElementManager.openTransaction();
        heapRecord1 = heapElementManager.readHeapFileDataRecord(pos, false);
        node = heapRecord1.getFreeNode();
        assertEquals("color must have changed", !color, node.getColor());
        heapElementManager.closeTransaction();
        heapElementManager.close();
    }

    public void testChangeManageDataMode() throws HeapException {
        appendRecord();
        final long pos = record1pos;
        heapElementManager.open();
        heapElementManager.openTransaction();
        HeapRecord heapRecord1 = heapElementManager.readHeapFileDataRecord(pos, false);
        assertFalse("record1 must be data record", heapRecord1.isFreeRecord());
        assertFalse("record1 must be in state just created", heapRecord1.isJustCreated());
        assertFalse("record1 do not manage data", heapRecord1.isManageData());
        assertTrue("record1 has been readed", heapRecord1.isReaded());
        assertFalse("record1 value must not changed state", heapRecord1.isValueChanged());
        heapRecord1.setValueIsChanged();
        assertNotNull("must be in cache", heapElementManager.getHeapFileRecordInCache(pos));
        heapRecord1 = heapElementManager.readHeapFileDataRecord(pos, true);
        assertFalse("record1 must be data record", heapRecord1.isFreeRecord());
        assertFalse("must not be just created", heapRecord1.isJustCreated());
        assertTrue("must manage data", heapRecord1.isManageData());
        assertTrue("must has been readed", heapRecord1.isReaded());
        assertTrue("mark as value changed", heapRecord1.isValueChanged());
        assertNotNull("must be in cache", heapElementManager.getHeapFileRecordInCache(pos));
    }

    /**
	 * append heap header in heap empty file
	 * 
	 * @return appened heap header
	 * @throws HeapException
	 * 
	 */
    private HeapHeader appendHeaderInEmptyFile() throws HeapException {
        heapElementManager.open();
        heapElementManager.openTransaction();
        final HeapHeader heapHeader1 = getHeapHeaderAndCheckPosition(null);
        assertEquals("FreeRootNodeFilePosition must be -1", -1, heapHeader1.getFreeRootNodeFilePosition());
        assertEquals("IdRootNodeFilePosition must be -1", -1, heapHeader1.getIdRootNodeFilePosition());
        assertTrue("created header must be to save", heapHeader1.isValueChanged());
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
        appendHeaderInEmptyFile();
        heapElementManager.open();
        heapElementManager.openTransaction();
        record1pos = HeapHeader.HEAP_HEADER_SIZE;
        final int dataSize = 100;
        final DataRecordIdentifier dataRecordIdentifier = new DataRecordIdentifier();
        heapRecordAppened1 = heapElementManager.newHeapFileRecord(record1pos, -1L, dataRecordIdentifier, false, true, dataSize, dataSize + HeapRecord.MAX_RECORD_HEADER_SIZE + 4);
        final byte[] data = heapRecordAppened1.getDataAssociated();
        assertNull("there is no data associated to record", data);
        byte[] testData = new byte[dataSize];
        for (int index = 0; index < dataSize; index++) {
            testData[index] = (byte) index;
        }
        heapRecordAppened1.setDataAssociated(testData);
        heapElementManager.closeTransaction();
        _log.debug("appened heap record1\n" + heapRecordAppened1.toString());
        heapElementManager.openTransaction();
        record2pos = record1pos + heapRecordAppened1.getAreaSize();
        heapRecordAppened2 = heapElementManager.newHeapFileRecord(record2pos, record1pos, dataRecordIdentifier, false, true, dataSize, dataSize + HeapRecord.MAX_RECORD_HEADER_SIZE + 4);
        heapRecordAppened2.setDataAssociated(testData);
        heapElementManager.closeTransaction();
        heapElementManager.close();
        _log.debug("appened heap record2\n" + heapRecordAppened2.toString());
    }

    /**
	 * get heap header and check its position
	 * 
	 * @param heapHeaderReference
	 * 
	 * @return heap header
	 * @throws HeapException
	 */
    private HeapHeader getHeapHeaderAndCheckPosition(final HeapHeader heapHeaderReference) throws HeapException {
        final HeapHeader heapHeader = heapElementManager.getHeapHeader();
        assertEquals("header must be at file beginning", 0, heapHeader.getPositionInFile());
        if (heapHeaderReference != null) {
            assertEquals("writed and readed must be equals", heapHeaderReference, heapHeader);
        }
        return heapHeader;
    }
}
