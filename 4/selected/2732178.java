package uk.org.ogsadai.activity.io;

import junit.framework.TestCase;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Unit test for <code>GrowableInMemoryPipe</code>.
 * 
 * TODO: There are no unit tests that check when happens when threads are
 *       terminated and such like.  These ought to be added.
 *
 * @author The OGSA-DAI Project Team
 */
public class GrowableInMemoryPipeTest extends TestCase {

    /** Copyright notice */
    private static final String COPYRIGHT_NOTICE = "Copyright (c) The University of Edinburgh 2002 - 2007.";

    /**
     * Constructor.
     * 
     * @param arg0 not used.
     */
    public GrowableInMemoryPipeTest(String arg0) {
        super(arg0);
    }

    /**
     * Run test for command line.
     * 
     * @param args not used.
     */
    public static void main(String[] args) {
        junit.textui.TestRunner.run(GrowableInMemoryPipeTest.class);
    }

    /**
     * Tests the buffer when the writer and reader are both going fast.
     * 
     * @throws Exception if an unexpected error occurs.
     */
    public void testFastWriterFastReader() throws Exception {
        runProducerAndConsumer(1000, 0, 0, 0, 0);
    }

    /**
     * Tests the buffer when the reader is much slower than the writer.
     * 
     * @throws Exception if an unexpected error occurs.
     */
    public void testFastWriteSlowRead() throws Exception {
        runProducerAndConsumer(200, 0, 0, 100, 10);
    }

    /**
     * Tests the buffer when the writer is much slower than the reader.
     * 
     * @throws Exception if an unexpected error occurs.
     */
    public void testSlowWriteFastRead() throws Exception {
        runProducerAndConsumer(200, 100, 10, 0, 0);
    }

    /**
     * Tests the <code>getNumBlocksReadable</code> method.
     * 
     * @throws Exception if an unexpected error occurs.
     */
    public void testGetNumBlocksReadable() throws Exception {
        Pipe pipe = new GrowableInMemoryPipe("myPipe");
        assertEquals(0, pipe.getNumBlocksReadable());
        pipe.write("Data");
        assertEquals(1, pipe.getNumBlocksReadable());
        pipe.write("Data");
        assertEquals(2, pipe.getNumBlocksReadable());
        pipe.write("Data");
        assertEquals(3, pipe.getNumBlocksReadable());
        pipe.read();
        assertEquals(2, pipe.getNumBlocksReadable());
        pipe.read();
        assertEquals(1, pipe.getNumBlocksReadable());
        pipe.read();
        assertEquals(0, pipe.getNumBlocksReadable());
        pipe.write("Data");
        assertEquals(1, pipe.getNumBlocksReadable());
        pipe.read();
        assertEquals(0, pipe.getNumBlocksReadable());
        pipe.closeForWriting();
        assertEquals(1, pipe.getNumBlocksReadable());
    }

    /**
     * Test closing the buffered pipe with a error.
     * 
     * @throws Exception if an unexpected error occurs.
     */
    public void testClosingWithError() throws Exception {
        GrowableInMemoryPipe pipe = new GrowableInMemoryPipe("myPipe");
        pipe.write("data1");
        pipe.write("data2");
        pipe.write("data3");
        pipe.closeForWritingDueToError();
        assertEquals("data1", pipe.read());
        assertEquals("data2", pipe.read());
        assertEquals("data3", pipe.read());
        try {
            pipe.read();
            fail("DataError expected");
        } catch (DataError e) {
        }
    }

    /**
     * Test closing the buffered pipe with a error when the buffer is full.
     * 
     * @throws Exception if an unexpected error occurs.
     */
    public void testClosingWithErrorWhenBufferFull() throws Exception {
        GrowableInMemoryPipe pipe = new GrowableInMemoryPipe("myPipe");
        pipe.write("data1");
        pipe.write("data2");
        pipe.write("data3");
        pipe.closeForWritingDueToError();
        assertEquals("data1", pipe.read());
        assertEquals("data2", pipe.read());
        assertEquals("data3", pipe.read());
        try {
            pipe.read();
            fail("DataError expected");
        } catch (DataError e) {
        }
    }

    /**
     * Runs an producer and cosummer and checks the consumer gets the data 
     * written by the producer.
     * 
     * @param numBlocks
     *          number of blocks to write
     * @param writerInitialSleepDuration
     *          duration to sleep before producer starts (milliseconds)
     * @param writerIntervalDuration
     *          duration between producing blocks (milliseconds)
     * @param readerInitialSleepDuration
     *          duration to sleep before consumer starts (milliseconds)
     * @param readerIntervalDuration
     *          duration between consuming blocks (milliseconds)
     * @throws Exception
     */
    private void runProducerAndConsumer(int numBlocks, long writerInitialSleepDuration, long writerIntervalDuration, long readerInitialSleepDuration, long readerIntervalDuration) throws Exception {
        Pipe pipe = new GrowableInMemoryPipe("myPipe");
        Object[] blocks = new Object[numBlocks];
        for (int i = 0; i < numBlocks; ++i) {
            blocks[i] = "MyString" + i;
        }
        BlockWriterCallable blockWriterCallable = new BlockWriterCallable(writerInitialSleepDuration, writerIntervalDuration, blocks, pipe);
        ExecutorService executorService = Executors.newCachedThreadPool();
        executorService.submit(blockWriterCallable);
        Thread.sleep(readerInitialSleepDuration);
        for (int i = 0; i < numBlocks; ++i) {
            Thread.sleep(readerIntervalDuration);
            Object block = pipe.read();
            assertTrue("Read block " + (i + 1) + " must be instance of String", block instanceof String);
            String blockAsString = (String) block;
            assertEquals("Block " + (i + 1), blocks[i], blockAsString);
        }
        assertEquals("Final block must be NO_MORE_DATA block", ControlBlock.NO_MORE_DATA, pipe.read());
    }
}
