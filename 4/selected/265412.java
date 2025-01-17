package org.streams.test.agent.send;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileWriter;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import junit.framework.TestCase;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.GzipCodec;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.util.HashedWheelTimer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.streams.agent.file.FileTrackerMemory;
import org.streams.agent.file.FileTrackingStatus;
import org.streams.agent.mon.status.AgentStatus;
import org.streams.agent.mon.status.impl.AgentStatusImpl;
import org.streams.agent.send.ClientConnectionFactory;
import org.streams.agent.send.ClientResourceFactory;
import org.streams.agent.send.FileSendTask;
import org.streams.agent.send.FileStreamer;
import org.streams.agent.send.FilesToSendQueue;
import org.streams.agent.send.impl.ClientConnectionFactoryImpl;
import org.streams.agent.send.impl.ClientHandlerContext;
import org.streams.agent.send.impl.ClientResourceFactoryImpl;
import org.streams.agent.send.impl.FileLineStreamerImpl;
import org.streams.agent.send.impl.FileSendTaskImpl;
import org.streams.agent.send.impl.FilesSendWorkerImpl;
import org.streams.agent.send.impl.FilesToSendQueueImpl;
import org.streams.agent.send.utils.MapTrackerMemory;
import org.streams.agent.send.utils.MessageEventBag;
import org.streams.agent.send.utils.MessageFrameDecoder;
import org.streams.commons.compression.CompressionPoolFactory;
import org.streams.commons.compression.impl.CompressionPoolFactoryImpl;
import org.streams.commons.file.impl.SimpleFileDateExtractor;
import org.streams.commons.io.Header;
import org.streams.commons.io.impl.ProtocolImpl;
import org.streams.commons.io.net.impl.RandomDistAddressSelector;
import org.streams.commons.metrics.impl.IntegerCounterPerSecondMetric;
import org.streams.commons.status.Status;

/**
 * 
 * Test FilesSendWorker instance using a dummy server.<br/>
 * <p/>
 * Behaviours tested are:<br/>
 * <ul>
 * <li>Normal thread send</li>
 * <li>Agent file pointer is out of sync with the collector, a 409 response is
 * sent.</li>
 * <li>Agent file pointer is out of sync with the collector but collector sends
 * a faulty line pointer, a 409 response is sent.</li>
 * </ul>
 */
public class TestFilesSendWorker extends TestCase {

    private File baseDir;

    private File fileToStream;

    private int testLineCount = 1000;

    private String testString = "This is a line use for testing, each line in the fileToStream will contain this string";

    private int testPort = 5024;

    private final List<MessageEventBag> bagList = new ArrayList<MessageEventBag>();

    private final CompressionPoolFactory pf = new CompressionPoolFactoryImpl(10, 10, new AgentStatusImpl());

    CompressionCodec codec;

    @Test
    public void testThreadFileDeletionDuringRead() throws Exception {
        ServerBootstrap bootstrap = connectServer(false);
        try {
            MapTrackerMemory memory = new MapTrackerMemory();
            FileTrackingStatus fileToSendStatus = createFileTrackingStatus();
            memory.updateFile(fileToSendStatus);
            AgentStatus agentStatus = new AgentStatusImpl();
            FilesSendWorkerImpl worker = createWorker(memory, agentStatus);
            worker.setWaitIfEmpty(1L);
            worker.setWaitOnErrorTime(1L);
            worker.setWaitBetweenFileSends(1L);
            Thread thread = new Thread(worker);
            thread.start();
            int count = 0;
            while (fileToSendStatus.getStatus().equals(FileTrackingStatus.STATUS.READING) || fileToSendStatus.getStatus().equals(FileTrackingStatus.STATUS.READY)) {
                if (count++ == 0) {
                    fileToStream.delete();
                    assertFalse(fileToStream.exists());
                }
                Thread.sleep(1000L);
            }
            FileTrackingStatus status = memory.getFileStatus(fileToStream);
            assertNotNull(status);
            assertTrue(status.getStatus().equals(FileTrackingStatus.STATUS.DELETED) || status.getStatus().equals(FileTrackingStatus.STATUS.READ_ERROR));
        } finally {
            bootstrap.releaseExternalResources();
        }
    }

    /**
	 * Test normal execution of sending file data.
	 * 
	 * @throws Exception
	 */
    @Test
    public void testThread() throws Exception {
        ServerBootstrap bootstrap = connectServer(false);
        try {
            MapTrackerMemory memory = new MapTrackerMemory();
            FileTrackingStatus fileToSendStatus = createFileTrackingStatus();
            memory.updateFile(fileToSendStatus);
            AgentStatus agentStatus = new AgentStatusImpl();
            FilesSendWorkerImpl worker = createWorker(memory, agentStatus);
            worker.setWaitIfEmpty(10L);
            worker.setWaitOnErrorTime(10L);
            worker.setWaitBetweenFileSends(1L);
            Thread thread = new Thread(worker);
            thread.start();
            while (fileToSendStatus.getStatus().equals(FileTrackingStatus.STATUS.READING) || fileToSendStatus.getStatus().equals(FileTrackingStatus.STATUS.READY)) {
                Thread.sleep(1000L);
            }
            FileTrackingStatus status = memory.getFileStatus(fileToStream);
            assertNotNull(status);
            assertEquals(FileTrackingStatus.STATUS.DONE, status.getStatus());
            assertEquals(AgentStatus.STATUS.OK, agentStatus.getStatus());
            worker.destroy();
        } finally {
            bootstrap.releaseExternalResources();
        }
    }

    /**
	 * This method tests the agent's behaviour when the collector sends a 409
	 * conflict message with an invalid file pointer from the collector.<br/>
	 * 
	 * @throws Exception
	 */
    @Test
    public void testThreadAgentConflictCollectorError() throws Exception {
        ServerBootstrap bootstrap = connectServer(true, true);
        try {
            MapTrackerMemory memory = new MapTrackerMemory();
            FileTrackingStatus fileToSendStatus = createFileTrackingStatus();
            memory.updateFile(fileToSendStatus);
            AgentStatus agentStatus = new AgentStatusImpl();
            FilesSendWorkerImpl worker = createWorker(memory, agentStatus);
            worker.setWaitIfEmpty(10L);
            worker.setWaitOnErrorTime(10L);
            worker.setWaitBetweenFileSends(1L);
            Thread thread = new Thread(worker);
            thread.start();
            while (fileToSendStatus.getStatus().equals(FileTrackingStatus.STATUS.READING) || fileToSendStatus.getStatus().equals(FileTrackingStatus.STATUS.READY)) {
                Thread.sleep(1000L);
            }
            FileTrackingStatus status = memory.getFileStatus(fileToStream);
            assertNotNull(status);
            assertEquals(FileTrackingStatus.STATUS.DONE, status.getStatus());
            assertEquals(AgentStatus.STATUS.OK, agentStatus.getStatus());
            worker.destroy();
        } finally {
            bootstrap.releaseExternalResources();
        }
    }

    /**
	 * This method tests the agent's behaviour when the collector sends a 409
	 * conflict message.<br/>
	 * The agent is expected to read the file pointer sent by the collector and
	 * restart reading from this file pointer position.<br/>
	 * 
	 * @param agentStatus2
	 * 
	 * @throws Exception
	 */
    @Test
    public void testThreadAgentConflict() throws Exception {
        ServerBootstrap bootstrap = connectServer(true);
        try {
            MapTrackerMemory memory = new MapTrackerMemory();
            FileTrackingStatus fileToSendStatus = createFileTrackingStatus();
            memory.updateFile(fileToSendStatus);
            AgentStatus agentStatus = new AgentStatusImpl();
            FilesSendWorkerImpl worker = createWorker(memory, agentStatus);
            worker.setWaitIfEmpty(10L);
            worker.setWaitOnErrorTime(10L);
            worker.setWaitBetweenFileSends(1L);
            Thread thread = new Thread(worker);
            thread.start();
            while (fileToSendStatus.getStatus().equals(FileTrackingStatus.STATUS.READING) || fileToSendStatus.getStatus().equals(FileTrackingStatus.STATUS.READY)) {
                Thread.sleep(1000L);
            }
            FileTrackingStatus status = memory.getFileStatus(fileToStream);
            assertNotNull(status);
            assertEquals(FileTrackingStatus.STATUS.DONE, status.getStatus());
            assertEquals(AgentStatus.STATUS.OK, agentStatus.getStatus());
            worker.destroy();
        } finally {
            bootstrap.releaseExternalResources();
        }
    }

    private FilesSendWorkerImpl createWorker(FileTrackerMemory memory, AgentStatus agentStatus) {
        FilesToSendQueue queue = new FilesToSendQueueImpl(memory);
        ExecutorService service = Executors.newCachedThreadPool();
        ClientConnectionFactory ccFact = new ClientConnectionFactoryImpl(new HashedWheelTimer(), new NioClientSocketChannelFactory(service, service), 10000L, 10000L, new ProtocolImpl(pf));
        FileStreamer fileLineStreamer = new FileLineStreamerImpl(codec, pf, 10000L);
        RandomDistAddressSelector selector = new RandomDistAddressSelector(new InetSocketAddress("localhost", testPort));
        ClientResourceFactory clientResourceFactory = new ClientResourceFactoryImpl(ccFact, fileLineStreamer, new SimpleFileDateExtractor());
        FileSendTask fileSendTask = new FileSendTaskImpl(agentStatus, clientResourceFactory, selector, memory, new IntegerCounterPerSecondMetric("TEST", new Status() {

            @Override
            public void setCounter(String status, int counter) {
            }

            @Override
            public STATUS getStatus() {
                return null;
            }

            @Override
            public String getStatusMessage() {
                return null;
            }

            @Override
            public void setStatus(STATUS status, String msg) {
            }

            @Override
            public long getStatusTimestamp() {
                return 0;
            }
        }));
        FilesSendWorkerImpl worker = new FilesSendWorkerImpl(queue, agentStatus, memory, fileSendTask);
        return worker;
    }

    private FileTrackingStatus createFileTrackingStatus() {
        FileTrackingStatus fileToSendStatus = new FileTrackingStatus();
        fileToSendStatus.setPath(fileToStream.getAbsolutePath());
        fileToSendStatus.setFileSize(fileToStream.length());
        fileToSendStatus.setLogType("Test");
        fileToSendStatus.setStatus(FileTrackingStatus.STATUS.READY);
        fileToSendStatus.setLastModificationTime(fileToStream.lastModified());
        return fileToSendStatus;
    }

    @Before
    public void setUp() throws Exception {
        Configuration conf = new Configuration();
        GzipCodec gzipCodec = new GzipCodec();
        gzipCodec.setConf(conf);
        codec = gzipCodec;
        baseDir = new File(".", "target/testSendClientFiles/");
        baseDir.mkdirs();
        fileToStream = new File(baseDir, "test.txt");
        if (fileToStream.exists()) fileToStream.delete();
        fileToStream.createNewFile();
        FileWriter writer = new FileWriter(fileToStream);
        BufferedWriter buffWriter = new BufferedWriter(writer);
        try {
            for (int i = 0; i < testLineCount; i++) {
                buffWriter.write(testString);
                buffWriter.write('\n');
            }
        } finally {
            buffWriter.close();
            writer.close();
        }
        while (!fileToStream.exists()) ;
    }

    @After
    public void tearDown() throws Exception {
        FileUtils.deleteDirectory(baseDir);
    }

    private ServerBootstrap connectServer(boolean simulateConflict) {
        return connectServer(simulateConflict, false);
    }

    private ServerBootstrap connectServer(boolean simulateConflict, boolean simulateConflictErrorPointer) {
        ServerBootstrap bootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool()));
        final MessageEventBagHandler messagEventBagHandler = new MessageEventBagHandler(bagList, simulateConflict, simulateConflictErrorPointer);
        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {

            @Override
            public ChannelPipeline getPipeline() throws Exception {
                return Channels.pipeline(new MessageFrameDecoder(), messagEventBagHandler);
            }
        });
        bootstrap.bind(new InetSocketAddress(testPort));
        return bootstrap;
    }

    private class MessageEventBagHandler extends SimpleChannelUpstreamHandler {

        List<MessageEventBag> bagList = null;

        boolean simulateConflict = false;

        boolean simulateConflictErrorPointer = false;

        int counter = 0;

        long filePointer = 0;

        /**
		 * 
		 * @param bagList
		 * @param simulateConflict
		 *            if true every 10 iterations this class will send a 409
		 *            error to the client.
		 * @param simulateConflictErrorPointer
		 *            if true the file pointer for the above 409 error will be
		 *            -1
		 */
        public MessageEventBagHandler(List<MessageEventBag> bagList, boolean simulateConflict, boolean simulateConflictErrorPointer) {
            this.bagList = bagList;
            this.simulateConflict = simulateConflict;
        }

        @Override
        public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
            System.out.println("-------- Channel connected " + System.currentTimeMillis());
            super.channelConnected(ctx, e);
        }

        @Override
        public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
            System.out.println("-------- Channel closed " + System.currentTimeMillis());
            super.channelClosed(ctx, e);
        }

        @Override
        public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
            super.messageReceived(ctx, e);
            System.out.println("-------- Channel messageRecieved " + System.currentTimeMillis());
            MessageEventBag bag = new MessageEventBag();
            bag.setBytes(e);
            ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
            if (simulateConflict) {
                Header header = new ProtocolImpl(pf).read(new PropertiesConfiguration(), new DataInputStream(new ByteArrayInputStream(bag.getBytes())));
                if (counter++ == 10) {
                    counter = 0;
                    buffer.writeInt(ClientHandlerContext.STATUS_CONFLICT);
                    if (simulateConflictErrorPointer) {
                        buffer.writeLong(-1);
                    } else {
                        buffer.writeLong(filePointer);
                    }
                } else {
                    buffer.writeInt(ClientHandlerContext.STATUS_OK);
                    bagList.add(bag);
                }
                filePointer = header.getFilePointer();
            } else {
                buffer.writeInt(ClientHandlerContext.STATUS_OK);
                bagList.add(bag);
            }
            ChannelFuture future = e.getChannel().write(buffer);
            future.addListener(ChannelFutureListener.CLOSE);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
            Throwable t = e.getCause();
            if (t != null) {
                t.printStackTrace();
            }
            e.getChannel().close();
        }
    }
}
