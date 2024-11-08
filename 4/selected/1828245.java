package org.apache.harmony.nio.tests.java.nio.channels;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.OverlappingFileLockException;
import junit.framework.TestCase;

/**
 * API tests for the NIO FileChannel locking APIs
 */
public class FileChannelLockingTest extends TestCase {

    private FileChannel readOnlyChannel;

    private FileChannel writeOnlyChannel;

    private FileChannel readWriteChannel;

    private final String CONTENT = "The best things in life are nearest: Breath in your nostrils, light in your eyes, " + "flowers at your feet, duties at your hand, the path of right just before you. Then do not grasp at the stars, " + "but do life's plain, common work as it comes, certain that daily duties and daily bread are the sweetest " + " things in life.--Robert Louis Stevenson";

    protected void setUp() throws Exception {
        super.setUp();
        File[] tempFiles = new File[3];
        for (int i = 0; i < tempFiles.length; i++) {
            tempFiles[i] = File.createTempFile("testing", "tmp");
            tempFiles[i].deleteOnExit();
            FileWriter writer = new FileWriter(tempFiles[i]);
            writer.write(CONTENT);
            writer.close();
        }
        FileInputStream fileInputStream = new FileInputStream(tempFiles[0]);
        readOnlyChannel = fileInputStream.getChannel();
        FileOutputStream fileOutputStream = new FileOutputStream(tempFiles[1]);
        writeOnlyChannel = fileOutputStream.getChannel();
        RandomAccessFile randomAccessFile = new RandomAccessFile(tempFiles[2], "rw");
        readWriteChannel = randomAccessFile.getChannel();
    }

    protected void tearDown() throws IOException {
        if (readOnlyChannel != null) {
            readOnlyChannel.close();
        }
        if (writeOnlyChannel != null) {
            writeOnlyChannel.close();
        }
        if (readWriteChannel != null) {
            readWriteChannel.close();
        }
    }

    public void test_illegalLocks() throws IOException {
        try {
            readOnlyChannel.lock();
            fail("Acquiring a full exclusive lock on a read only channel should fail.");
        } catch (NonWritableChannelException ex) {
        }
        try {
            writeOnlyChannel.lock(1, 10, true);
            fail("Acquiring a shared lock on a write-only channel should fail.");
        } catch (NonReadableChannelException ex) {
        }
    }

    public void test_lockReadWrite() throws IOException {
        FileLock flock = readWriteChannel.lock();
        if (flock != null) {
            flock.release();
        }
    }

    public void test_illegalLockParameters() throws IOException {
        try {
            readOnlyChannel.lock(-1, 10, true);
            fail("Passing illegal args to lock should fail.");
        } catch (IllegalArgumentException ex) {
        }
        try {
            writeOnlyChannel.lock(-1, 10, false);
            fail("Passing illegal args to lock should fail.");
        } catch (IllegalArgumentException ex) {
        }
        try {
            readWriteChannel.lock(-1, 10, false);
            fail("Passing illegal args to lock should fail.");
        } catch (IllegalArgumentException ex) {
        }
        FileLock flock1 = readWriteChannel.lock(22, 110, true);
        try {
            readWriteChannel.lock(75, 210, true);
        } catch (OverlappingFileLockException exception) {
            flock1.release();
        }
    }

    public void test_lockLLZ() throws IOException {
        FileLock flock1 = readWriteChannel.lock(0, 10, false);
        FileLock flock2 = readWriteChannel.lock(22, 100, true);
        flock1.release();
        flock2.release();
    }

    public void test_tryLock() throws IOException {
        try {
            readOnlyChannel.tryLock();
            fail("Acquiring a full exclusive lock on a read channel should have thrown an exception.");
        } catch (NonWritableChannelException ex) {
        }
    }

    public void test_tryLockLLZ() throws IOException {
        try {
            readOnlyChannel.tryLock(0, 99, false);
            fail("Acquiring exclusive lock on read-only channel should fail");
        } catch (NonWritableChannelException ex) {
        }
        try {
            readOnlyChannel.tryLock(-99, 0, true);
            fail("Acquiring an illegal lock value should fail.");
        } catch (IllegalArgumentException ex) {
        }
        FileLock tmpLock = readOnlyChannel.tryLock(0, 10, true);
        assertTrue(tmpLock.isValid());
        tmpLock.release();
        FileLock lock = readOnlyChannel.tryLock(10, 788, true);
        assertTrue(lock.isValid());
        try {
            readOnlyChannel.tryLock(1, 23, true);
            fail("Acquiring an overlapping lock should fail.");
        } catch (OverlappingFileLockException ex) {
        }
        FileLock adjacentLock = readOnlyChannel.tryLock(1, 3, true);
        assertTrue(adjacentLock.isValid());
        adjacentLock.release();
        lock.release();
    }
}
