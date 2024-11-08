package com.faunos.skwish.sys.mgr;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Random;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import com.faunos.skwish.Segment;
import com.faunos.skwish.SegmentStore;
import com.faunos.skwish.SkwishException;
import com.faunos.skwish.TxnSegment;
import com.faunos.skwish.sys.SegmentTestHelper;
import com.faunos.util.Version;
import com.faunos.util.io.WorkAroundFileChannel;
import com.faunos.util.test.Helper;
import junit.framework.TestCase;

/**
 * TODO: fix serendipitous naming error?
 * 
 * @author Babak Farhang
 */
public class SystemMangerTest extends TestCase {

    private static final Helper helper = new Helper(SystemMangerTest.class);

    private static final File ROOT_DIR = helper.getTestCaseDirectory(SystemMangerTest.class);

    @Override
    protected void setUp() throws Exception {
        final Level level = Level.ALL;
        Logger rootLogger = LogManager.getLogManager().getLogger("");
        Handler[] handlers = rootLogger.getHandlers();
        for (Handler h : handlers) h.setLevel(level);
        rootLogger.setLevel(Level.WARNING);
    }

    public void testEmpty() throws IOException {
        File segmentRootDir = new File(ROOT_DIR, getName());
        SegmentStore store = SegmentStore.writeNewInstance(segmentRootDir.getPath());
        Segment readOnlySegment = store.getReadOnlySegment();
        assertTrue(readOnlySegment.isReadOnly());
        assertTrue(readOnlySegment.getEntryCount() == 0);
        assertTrue(readOnlySegment.getBaseId() == 0);
        try {
            readOnlySegment.killNext();
            fail();
        } catch (UnsupportedOperationException x) {
        }
        store.close();
        store = SegmentStore.loadInstance(segmentRootDir.getPath());
        readOnlySegment = store.getReadOnlySegment();
        assertTrue(readOnlySegment.isReadOnly());
        assertTrue(readOnlySegment.getEntryCount() == 0);
        assertTrue(readOnlySegment.getBaseId() == 0);
        store.close();
        try {
            store = SegmentStore.writeNewInstance(segmentRootDir.getPath());
            fail();
        } catch (SkwishException x) {
        }
    }

    public void testVersion() throws IOException {
        File segmentRootDir = new File(ROOT_DIR, getName());
        {
            Store store = (Store) SegmentStore.writeNewInstance(segmentRootDir.getPath());
            Version version = store.getFileVersion();
            assertEquals(Store.VERSION, version);
            store.close();
        }
        {
            Store store = (Store) SegmentStore.loadInstance(segmentRootDir.getPath());
            Version version = store.getFileVersion();
            assertEquals(Store.VERSION, version);
            store.close();
        }
    }

    public void testTxnWithOneEntry() throws IOException {
        File segmentRootDir = new File(ROOT_DIR, getName());
        SegmentStore store = SegmentStore.writeNewInstance(segmentRootDir.getPath());
        Segment readOnlySegment = store.getReadOnlySegment();
        TxnSegment txnSegment = store.newTransaction();
        assertEquals(0, txnSegment.getBaseId());
        assertEquals(0, txnSegment.getTxnBaseId());
        assertEquals(-1, txnSegment.getTxnCommitId());
        assertTrue(txnSegment.isAlive());
        final String sentence = "Jack and Jill went up the hill.";
        ByteBuffer buffer = ByteBuffer.wrap(sentence.getBytes());
        long id = txnSegment.insertEntry(buffer);
        buffer = ByteBuffer.allocate(buffer.capacity());
        txnSegment.getEntry(id, buffer);
        buffer.flip();
        String outSentence = new String(buffer.array());
        assertEquals(sentence, outSentence);
        assertEquals(0, readOnlySegment.getEntryCount());
        txnSegment.commit();
        assertTrue(txnSegment.isCommitted());
        assertFalse(txnSegment.isAlive());
        assertEquals(0, txnSegment.getBaseId());
        assertEquals(0, txnSegment.getTxnCommitId());
        assertEquals(1, readOnlySegment.getEntryCount());
        buffer = ByteBuffer.allocate(buffer.capacity());
        readOnlySegment.getEntry(id + txnSegment.getTxnCommitIdGap(), buffer);
        buffer.flip();
        outSentence = new String(buffer.array());
        assertEquals(sentence, outSentence);
        store.close();
        store = SegmentStore.loadInstance(segmentRootDir.getPath());
        readOnlySegment = store.getReadOnlySegment();
        buffer = ByteBuffer.allocate(buffer.capacity());
        readOnlySegment.getEntry(id, buffer);
        buffer.flip();
        outSentence = new String(buffer.array());
        assertEquals(sentence, outSentence);
        store.close();
    }

    public void testTxnWithTwoEntries() throws IOException {
        File segmentRootDir = new File(ROOT_DIR, getName());
        SegmentStore store = SegmentStore.writeNewInstance(segmentRootDir.getPath());
        Segment readOnlySegment = store.getReadOnlySegment();
        TxnSegment txnSegment = store.newTransaction();
        assertEquals(0, txnSegment.getBaseId());
        assertEquals(0, txnSegment.getTxnBaseId());
        assertEquals(-1, txnSegment.getTxnCommitId());
        assertTrue(txnSegment.isAlive());
        SegmentTestHelper.insertEntries(txnSegment, 2);
        assertEquals(2, txnSegment.getEntryCount());
        SegmentTestHelper.assertEntries(txnSegment, 0, 2);
        assertEquals(0, readOnlySegment.getEntryCount());
        txnSegment.commit();
        assertTrue(txnSegment.isCommitted());
        assertFalse(txnSegment.isAlive());
        assertEquals(0, txnSegment.getBaseId());
        assertEquals(0, txnSegment.getTxnCommitId());
        assertEquals(2, readOnlySegment.getEntryCount());
        SegmentTestHelper.assertEntries(readOnlySegment, 0, 2);
        store.close();
        store = SegmentStore.loadInstance(segmentRootDir.getPath());
        readOnlySegment = store.getReadOnlySegment();
        SegmentTestHelper.assertEntries(readOnlySegment, 0, 2);
        store.close();
    }

    public void testTwoTxnsOneEntryEach() throws IOException {
        File segmentRootDir = new File(ROOT_DIR, getName());
        SegmentStore store = SegmentStore.writeNewInstance(segmentRootDir.getPath());
        Segment readOnlySegment = store.getReadOnlySegment();
        TxnSegment[] txns = { store.newTransaction(), store.newTransaction() };
        for (TxnSegment txn : txns) {
            assertEquals(0, txn.getEntryCount());
            assertEquals(0, txn.getBaseId());
            assertEquals(0, txn.getNextId());
            assertEquals(0, txn.getTxnBaseId());
            assertEquals(-1, txn.getTxnCommitId());
            assertEquals(-1, txn.getTxnCommitIdGap());
            assertTrue(txn.isAlive());
        }
        for (int i = 0; i < txns.length; ++i) SegmentTestHelper.insertEntries(txns[i], i, 1);
        assertEquals(0, readOnlySegment.getEntryCount());
        int i = 0;
        long expectedGap = 0;
        long expectedCommitId = 0;
        long txnCommitId = txns[i].commit();
        assertEquals(expectedCommitId, txnCommitId);
        assertEquals(expectedCommitId, txns[i].getTxnCommitId());
        assertEquals(0, txns[i].getTxnBaseId());
        assertEquals(expectedGap, txns[i].getTxnCommitIdGap());
        assertTrue(txns[i].isCommitted());
        assertFalse(txns[i].isAlive());
        assertFalse(txns[i].isDiscarded());
        assertEquals(1, readOnlySegment.getEntryCount());
        SegmentTestHelper.assertEntries(readOnlySegment, 0, 1);
        i = 1;
        expectedGap = 1;
        expectedCommitId = 1;
        txnCommitId = txns[i].commit();
        assertEquals(expectedCommitId, txnCommitId);
        assertEquals(expectedCommitId, txns[i].getTxnCommitId());
        assertEquals(0, txns[i].getTxnBaseId());
        assertEquals(expectedGap, txns[i].getTxnCommitIdGap());
        assertTrue(txns[i].isCommitted());
        assertFalse(txns[i].isAlive());
        assertFalse(txns[i].isDiscarded());
        assertEquals(2, readOnlySegment.getEntryCount());
        SegmentTestHelper.assertEntries(readOnlySegment, 0, 2);
        store.close();
        store = SegmentStore.loadInstance(segmentRootDir.getPath());
        readOnlySegment = store.getReadOnlySegment();
        assertEquals(2, readOnlySegment.getEntryCount());
        SegmentTestHelper.assertEntries(readOnlySegment, 0, 2);
        store.close();
    }

    public static void main(String[] arg) throws Exception {
        SystemMangerTest test = new SystemMangerTest();
        test.setName("testDeleteSetDelete");
        test.setUp();
        test.testDeleteSetDelete();
    }

    public void testDeleteSetDelete() throws IOException {
        Logger.getLogger("").setLevel(Level.ALL);
        File segmentRootDir = new File(ROOT_DIR, getName());
        SegmentStore store = SegmentStore.writeNewInstance(segmentRootDir.getPath());
        Segment readOnlySegment = store.getReadOnlySegment();
        println("T1");
        TxnSegment txnSeg = store.newTransaction();
        final int count = 7;
        SegmentTestHelper.insertEntries(txnSeg, count);
        txnSeg.commit();
        SegmentTestHelper.assertEntries(readOnlySegment, 0, count);
        println("T2");
        txnSeg = store.newTransaction();
        txnSeg.delete(0);
        assertTrue(txnSeg.isDeleted(0));
        assertFalse(readOnlySegment.isDeleted(0));
        txnSeg.commit();
        assertTrue(readOnlySegment.isDeleted(0));
        assertEquals(count, readOnlySegment.getEntryCount());
        store.close();
        println("reloading..");
        store = SegmentStore.loadInstance(segmentRootDir.getPath());
        readOnlySegment = store.getReadOnlySegment();
        assertTrue(readOnlySegment.isDeleted(0));
        assertEquals(count, readOnlySegment.getEntryCount());
        println("T3");
        txnSeg = store.newTransaction();
        final int moreCount = 3000;
        SegmentTestHelper.insertEntries(txnSeg, moreCount);
        txnSeg.commit();
        store.close();
        println("reloading..");
        store = SegmentStore.loadInstance(segmentRootDir.getPath());
        readOnlySegment = store.getReadOnlySegment();
        println("T4");
        txnSeg = store.newTransaction();
        final int deleteStartId = 4;
        final int deleteCount = 1000;
        txnSeg.delete(deleteStartId, deleteCount);
        txnSeg.commit();
        SegmentTestHelper.assertEntries(readOnlySegment, 1, deleteStartId - 1);
        final int deleteIdEndX = deleteStartId + deleteCount;
        for (int i = deleteStartId; i < deleteIdEndX; ++i) assertTrue(readOnlySegment.isDeleted(i));
        SegmentTestHelper.assertEntries(readOnlySegment, deleteIdEndX, (int) readOnlySegment.getNextId() - deleteIdEndX);
        store.close();
        println("reloading..");
        store = SegmentStore.loadInstance(segmentRootDir.getPath());
        readOnlySegment = store.getReadOnlySegment();
        assertTrue(readOnlySegment.isDeleted(0));
        SegmentTestHelper.assertEntries(readOnlySegment, 1, deleteStartId - 1);
        for (int i = deleteStartId; i < deleteIdEndX; ++i) assertTrue(readOnlySegment.isDeleted(i));
        SegmentTestHelper.assertEntries(readOnlySegment, deleteIdEndX, (int) readOnlySegment.getNextId() - deleteIdEndX);
        store.close();
    }

    public void testFileChannelTransfer() throws IOException {
        File entryFile = helper.getTestCaseFile(this);
        {
            FileWriter writer = new FileWriter(entryFile);
            String[] junk = new String[] { "cows jumping over the moon ", "quick foxes and lazy dogs ", "cdos, cmos, sivs, and cdss ", "print code instead of money ", "this time it's different ", "the next one will be even better " };
            Random rnd = new Random(0);
            for (int count = 500; count-- > 0; ) {
                int i = rnd.nextInt(junk.length);
                writer.write(junk[i]);
                if (count % 3 == 0) writer.write('\n');
            }
            writer.close();
        }
        FileChannel src = new FileInputStream(entryFile).getChannel();
        File copy = helper.getTestCaseFile(this + "-copy");
        FileChannel copyChannel = new RandomAccessFile(copy, "rw").getChannel();
        copyChannel = new WorkAroundFileChannel(copyChannel);
        long pos = 0;
        while (true) {
            long amtTransfered = copyChannel.transferFrom(src, pos, 100000);
            if (amtTransfered == 0) break;
            pos += amtTransfered;
        }
        copyChannel.close();
        src.close();
    }

    public void testInsertEntryChannel() throws IOException {
        File entryFile = helper.getTestCaseFile(this);
        {
            FileWriter writer = new FileWriter(entryFile);
            String[] junk = new String[] { "cows jumping over the moon ", "quick foxes and lazy dogs ", "cdos, cmos, sivs, and cdss ", "print code instead of money ", "this time it's different ", "the next one will be even better " };
            Random rnd = new Random(0);
            for (int count = 500; count-- > 0; ) {
                int i = rnd.nextInt(junk.length);
                writer.write(junk[i]);
                if (count % 3 == 0) writer.write('\n');
            }
            writer.close();
        }
        File segmentRootDir = new File(ROOT_DIR, getName());
        SegmentStore store = SegmentStore.writeNewInstance(segmentRootDir.getPath());
        Segment readOnlySegment = store.getReadOnlySegment();
        TxnSegment txn = store.newTransaction();
        FileChannel entry = new FileInputStream(entryFile).getChannel();
        txn.insertEntry(entry);
        txn.commit();
        FileChannel insertedEntry = readOnlySegment.getEntryChannel(0);
        entry.position(0);
        long count = entry.size();
        ByteBuffer ob = ByteBuffer.allocate(1);
        ByteBuffer ib = ByteBuffer.allocate(1);
        while (count-- > 0) {
            ob.clear();
            ib.clear();
            entry.read(ob);
            insertedEntry.read(ib);
            assertEquals(ob.get(0), ib.get(0));
        }
        entry.close();
        store.close();
    }

    private void println(String message) {
        System.out.println();
        System.out.println(message);
        System.out.println();
    }
}
