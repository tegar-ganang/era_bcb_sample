package ch.unifr.nio.framework;

import ch.unifr.nio.framework.mockups.BlockingSocketChannel;
import ch.unifr.nio.framework.mockups.TestChannelHandler;
import ch.unifr.nio.framework.mockups.TestTarget;
import ch.unifr.nio.framework.transform.ChannelWriter;
import ch.unifr.nio.framework.transform.StringToByteBufferTransformer;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.TestCase;

/**
 * Checks that after caching the interestOps other threads can access the channel
 * @author Ronny Standtke <Ronny.Standtke@gmx.net>
 */
public class ChannelLockTest extends TestCase {

    private static String testString = "testString";

    /**
     * tests, if the channel is correctly locking 
     * @throws java.lang.Exception if an exception occurs
     */
    public void testChannelLock() throws Exception {
        Logger logger = Logger.getLogger("CachedOpsTest");
        logger.setLevel(Level.FINEST);
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.FINEST);
        logger.addHandler(consoleHandler);
        CacheStopDispatcher cacheStopDispatcher = new CacheStopDispatcher();
        cacheStopDispatcher.start();
        TestTarget testTarget = new TestTarget(12345);
        testTarget.start();
        SocketChannel channel = testTarget.getSocketChannel();
        BlockingSocketChannel blockingChannel = new BlockingSocketChannel(channel);
        blockingChannel.configureBlocking(false);
        final TestChannelHandler testChannelHandler = new TestChannelHandler();
        cacheStopDispatcher.registerChannel(blockingChannel, testChannelHandler);
        testTarget.write(testString.getBytes());
        cacheStopDispatcher.waitForHandlerAdapter();
        final ChannelWriter channelWriter = testChannelHandler.getChannelWriter();
        Thread testThread = new Thread() {

            @Override
            public void run() {
                try {
                    StringToByteBufferTransformer transformer = new StringToByteBufferTransformer();
                    transformer.setNextForwarder(channelWriter);
                    transformer.forward("this MUST BLOCK right now!");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
        testThread.start();
        testThread.join(1000);
        assertFalse(testThread.isAlive());
        Field bufferField = ChannelWriter.class.getDeclaredField("buffer");
        bufferField.setAccessible(true);
        ByteBuffer buffer = (ByteBuffer) bufferField.get(channelWriter);
        assertTrue(buffer.hasRemaining());
    }

    private class CacheStopDispatcher extends Dispatcher {

        private final Semaphore semaphore;

        private final Lock lock = new ReentrantLock();

        private final Condition adapterCached = lock.newCondition();

        private boolean cached;

        private Selector selector;

        private Logger logger;

        private Executor executor;

        public CacheStopDispatcher() throws IOException {
            semaphore = new Semaphore(0);
            try {
                Field selectorField = Dispatcher.class.getDeclaredField("selector");
                selectorField.setAccessible(true);
                selector = (Selector) selectorField.get((Dispatcher) this);
                Field loggerField = Dispatcher.class.getDeclaredField("LOGGER");
                loggerField.setAccessible(true);
                logger = (Logger) loggerField.get((Dispatcher) this);
                Field executorField = Dispatcher.class.getDeclaredField("executor");
                executorField.setAccessible(true);
                executor = (Executor) executorField.get((Dispatcher) this);
            } catch (IllegalArgumentException ex) {
                logger.log(Level.WARNING, null, ex);
            } catch (SecurityException ex) {
                logger.log(Level.WARNING, null, ex);
            } catch (IllegalAccessException ex) {
                logger.log(Level.WARNING, null, ex);
            } catch (NoSuchFieldException ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }

        @Override
        @SuppressWarnings("empty-statement")
        public void run() {
            try {
                while (true) {
                    synchronized (this) {
                    }
                    selector.select();
                    Set<SelectionKey> keys = selector.selectedKeys();
                    if ((logger != null) && logger.isLoggable(Level.FINEST)) {
                        logger.log(Level.FINEST, "{0} keys in selector''s selected key set", keys.size());
                    }
                    for (SelectionKey key : keys) {
                        HandlerAdapter adapter = (HandlerAdapter) key.attachment();
                        adapter.cacheOps();
                        lock.lock();
                        try {
                            cached = true;
                            adapterCached.signal();
                        } finally {
                            lock.unlock();
                        }
                        semaphore.acquire();
                        key.interestOps(0);
                        executor.execute(adapter);
                    }
                    keys.clear();
                }
            } catch (InterruptedException ex) {
                logger.log(Level.WARNING, null, ex);
            } catch (IOException ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }

        public void continueDispatcher() {
            semaphore.release();
        }

        public void waitForHandlerAdapter() {
            lock.lock();
            try {
                while (!cached) {
                    adapterCached.await();
                }
            } catch (Exception e) {
                lock.unlock();
            }
        }
    }
}
