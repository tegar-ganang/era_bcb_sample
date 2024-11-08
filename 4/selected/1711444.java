package com.ewansilver.raindrop.fileio;

import java.io.File;
import java.io.IOException;
import com.ewansilver.raindrop.TaskQueueException;

/**
 * Tests that file write functionality works as expected.
 * 
 * @author ewan.silver AT gmail.com
 */
public class WriteFileTest extends FileTest {

    private File writeFile;

    /**
	 * Constructor.
	 */
    public WriteFileTest() {
        super();
    }

    protected void setUp() throws Exception {
        super.setUp();
        writeFile = new File(xmlDir, "write.txt");
        if (writeFile.exists()) writeFile.delete();
        assertFalse(writeFile.exists());
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        writeFile.delete();
        assertFalse(writeFile.exists());
    }

    /**
	 * Check that writing a file works.
	 */
    public void testWrite() throws IOException, TaskQueueException, InterruptedException {
        File writeFile = new File(xmlDir, "write.txt");
        assertFalse(writeFile.exists());
        assertTrue(writeFile.createNewFile());
        assertTrue(writeFile.exists());
        String string = "this is a test";
        WriteFileTask task = new WriteFileTask(writeFile, null, "test", string.getBytes());
        io.getTaskQueue().enqueue(task);
        WriteFileEvent event = (WriteFileEvent) testChannel.take();
        assertTrue(event.isSuccesful());
        ReadFileTask readTask = new ReadFileTask(writeFile, null, "test");
        io.getTaskQueue().enqueue(readTask);
        ReadFileEvent readEvent = (ReadFileEvent) testChannel.take();
        byte[] data = readEvent.getData();
        assertEquals(14, data.length);
        assertTrue(string.equals(new String(data)));
    }

    /**
	 * Test that an existing file can be appended to.
	 */
    public void testAppendToExistingFile() throws IOException, TaskQueueException, InterruptedException {
        testWrite();
        assertTrue(writeFile.exists());
        final ReadFileTask readTask1 = new ReadFileTask(writeFile, null, "test");
        io.getTaskQueue().enqueue(readTask1);
        final ReadFileEvent readEvent1 = (ReadFileEvent) testChannel.take();
        final byte[] data1 = readEvent1.getData();
        assertEquals(14, data1.length);
        assertTrue("this is a test".equals(new String(data1)));
        String appendString = "append";
        WriteFileTask task = new WriteFileTask(writeFile, null, "test", appendString.getBytes());
        task.setAppend(true);
        io.getTaskQueue().enqueue(task);
        WriteFileEvent event = (WriteFileEvent) testChannel.take();
        assertTrue(event.isSuccesful());
        final ReadFileTask readTask2 = new ReadFileTask(writeFile, null, "test");
        io.getTaskQueue().enqueue(readTask2);
        final ReadFileEvent readEvent2 = (ReadFileEvent) testChannel.take();
        final byte[] data2 = readEvent2.getData();
        assertEquals(20, data2.length);
        assertTrue("this is a testappend".equals(new String(data2)));
    }

    /**
	 * Test that appending to a new file jut writes the data.
	 * 
	 */
    public void testAppendToNewFileFile() throws IOException, TaskQueueException, InterruptedException {
        assertFalse(writeFile.exists());
        String appendString = "append";
        WriteFileTask task = new WriteFileTask(writeFile, null, "test", appendString.getBytes());
        task.setAppend(true);
        io.getTaskQueue().enqueue(task);
        WriteFileEvent event = (WriteFileEvent) testChannel.take();
        assertTrue(event.isSuccesful());
        final ReadFileTask readTask2 = new ReadFileTask(writeFile, null, "test");
        io.getTaskQueue().enqueue(readTask2);
        final ReadFileEvent readEvent2 = (ReadFileEvent) testChannel.take();
        final byte[] data2 = readEvent2.getData();
        assertEquals(6, data2.length);
        assertTrue(appendString.equals(new String(data2)));
    }

    /**
	 * Check that writing to an existing file overwrites what is there.
	 */
    public void testOverWrite() throws IOException, TaskQueueException, InterruptedException {
        File writeFile = new File(xmlDir, "write.txt");
        if (writeFile.exists()) {
            writeFile.delete();
        }
        assertFalse(writeFile.exists());
        assertTrue(writeFile.createNewFile());
        assertTrue(writeFile.exists());
        String writeString = "this is a test";
        WriteFileTask writeTask = new WriteFileTask(writeFile, null, "test", writeString.getBytes());
        io.getTaskQueue().enqueue(writeTask);
        WriteFileEvent writeFileEvent = (WriteFileEvent) testChannel.take();
        assertTrue(writeFileEvent.isSuccesful());
        ReadFileTask readTask1 = new ReadFileTask(writeFile, null, "test");
        io.getTaskQueue().enqueue(readTask1);
        ReadFileEvent readEvent1 = (ReadFileEvent) testChannel.take();
        byte[] data1 = readEvent1.getData();
        assertEquals(14, data1.length);
        assertTrue(writeString.equals(new String(data1)));
        assertTrue(writeFile.exists());
        assertEquals(14, writeFile.length());
        assertEquals(0, testChannel.size());
        String string = "this is an overwrite test";
        WriteFileTask task = new WriteFileTask(writeFile, null, "test", string.getBytes());
        io.getTaskQueue().enqueue(task);
        WriteFileEvent event = (WriteFileEvent) testChannel.take();
        assertTrue(event.isSuccesful());
        ReadFileTask readTask = new ReadFileTask(writeFile, null, "test");
        io.getTaskQueue().enqueue(readTask);
        ReadFileEvent readEvent = (ReadFileEvent) testChannel.take();
        byte[] data = readEvent.getData();
        assertEquals(25, data.length);
        assertTrue(string.equals(new String(data)));
    }
}
