package net.sourceforge.quexec.packet.chars.producer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import java.io.CharArrayReader;
import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.Reader;
import java.util.concurrent.Executors;
import net.sourceforge.quexec.packet.chars.consumer.StoreCharPacketConsumer;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;

public abstract class AbstractReaderCharPacketProducerTest {

    private static final Log log = LogFactory.getLog(AbstractReaderCharPacketProducerTest.class);

    protected static final int bufSize = 10;

    private static final int dataWaitTime = 10;

    private StoreCharPacketConsumer consumer;

    private AbstractReaderCharPacketProducer producer;

    @Before
    public void runBeforeEveryTest() throws Exception {
        this.consumer = new StoreCharPacketConsumer();
        this.producer = getReaderCharPipeProducer();
        this.producer.setDataWaitTime(dataWaitTime);
        this.producer.setConsumer(this.consumer);
        this.producer.setExecutor(Executors.newCachedThreadPool());
    }

    protected abstract AbstractReaderCharPacketProducer getReaderCharPipeProducer();

    @Test
    public void readMuchMoreDataThanBufferSize() {
        executeTestWithRandomData(bufSize * 10);
    }

    @Test
    public void readSingleChar() {
        executeTestWithRandomData(1);
    }

    @Test
    public void readEmptyStream() {
        executeTestWithRandomData(0);
    }

    @Test
    public void readStutteringStream() throws IOException {
        final String testData = "ab\ncd";
        final PipedReader reader = new PipedReader();
        final PipedWriter writer = new PipedWriter(reader);
        Thread t = new Thread() {

            public void run() {
                try {
                    for (int i = 0; i < testData.length(); i++) {
                        Thread.sleep(10);
                        log.debug("sending data: " + testData.charAt(i));
                        writer.append(testData.charAt(i));
                    }
                    writer.flush();
                } catch (Exception e) {
                    throw new RuntimeException("exception in test data driver thread", e);
                }
            }
        };
        t.start();
        sendAndCheckTestData(testData, reader);
    }

    private void executeTestWithRandomData(int dataSize) {
        String randStr = RandomStringUtils.random(dataSize, 'a', 'z', true, false);
        Reader testDataReader = new CharArrayReader(randStr.toCharArray());
        sendAndCheckTestData(randStr, testDataReader);
    }

    private void sendAndCheckTestData(String testData, Reader testDataReader) {
        this.producer.setReader(testDataReader);
        this.producer.start();
        try {
            Thread.sleep(100);
            this.producer.shutdown();
        } catch (InterruptedException e) {
            fail("unexpected interrupt");
        }
        String consumedChars = this.consumer.retrieveData();
        log.debug("consumer data retrieved: '" + consumedChars + "'");
        assertEquals(testData, consumedChars);
    }
}
