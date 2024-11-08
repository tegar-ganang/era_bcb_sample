package org.apache.hadoop.net;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.LinkedList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.util.StringUtils;

/**
 * This supports input and output streams for a socket channels. 
 * These streams can have a timeout.
 */
abstract class SocketIOWithTimeout {

    static final Log LOG = LogFactory.getLog(SocketIOWithTimeout.class);

    private SelectableChannel channel;

    private long timeout;

    private boolean closed = false;

    private static SelectorPool selector = new SelectorPool();

    SocketIOWithTimeout(SelectableChannel channel, long timeout) throws IOException {
        checkChannelValidity(channel);
        this.channel = channel;
        this.timeout = timeout;
        channel.configureBlocking(false);
    }

    void close() {
        closed = true;
    }

    boolean isOpen() {
        return !closed && channel.isOpen();
    }

    SelectableChannel getChannel() {
        return channel;
    }

    /** 
   * Utility function to check if channel is ok.
   * Mainly to throw IOException instead of runtime exception
   * in case of mismatch. This mismatch can occur for many runtime
   * reasons.
   */
    static void checkChannelValidity(Object channel) throws IOException {
        if (channel == null) {
            throw new IOException("Channel is null. Check " + "how the channel or socket is created.");
        }
        if (!(channel instanceof SelectableChannel)) {
            throw new IOException("Channel should be a SelectableChannel");
        }
    }

    /**
   * Performs actual IO operations. This is not expected to block.
   *  
   * @param buf
   * @return number of bytes (or some equivalent). 0 implies underlying
   *         channel is drained completely. We will wait if more IO is 
   *         required.
   * @throws IOException
   */
    abstract int performIO(ByteBuffer buf) throws IOException;

    /**
   * Performs one IO and returns number of bytes read or written.
   * It waits up to the specified timeout. If the channel is 
   * not read before the timeout, SocketTimeoutException is thrown.
   * 
   * @param buf buffer for IO
   * @param ops Selection Ops used for waiting. Suggested values: 
   *        SelectionKey.OP_READ while reading and SelectionKey.OP_WRITE while
   *        writing. 
   *        
   * @return number of bytes read or written. negative implies end of stream.
   * @throws IOException
   */
    int doIO(ByteBuffer buf, int ops) throws IOException {
        if (!buf.hasRemaining()) {
            throw new IllegalArgumentException("Buffer has no data left.");
        }
        while (buf.hasRemaining()) {
            if (closed) {
                return -1;
            }
            try {
                int n = performIO(buf);
                if (n != 0) {
                    return n;
                }
            } catch (IOException e) {
                if (!channel.isOpen()) {
                    closed = true;
                }
                throw e;
            }
            int count = 0;
            try {
                count = selector.select(channel, ops, timeout);
            } catch (IOException e) {
                closed = true;
                throw e;
            }
            if (count == 0) {
                throw new SocketTimeoutException(timeoutExceptionString(ops));
            }
        }
        return 0;
    }

    /**
   * This is similar to {@link #doIO(ByteBuffer, int)} except that it
   * does not perform any I/O. It just waits for the channel to be ready
   * for I/O as specified in ops.
   * 
   * @param ops Selection Ops used for waiting
   * 
   * @throws SocketTimeoutException 
   *         if select on the channel times out.
   * @throws IOException
   *         if any other I/O error occurs. 
   */
    void waitForIO(int ops) throws IOException {
        if (selector.select(channel, ops, timeout) == 0) {
            throw new SocketTimeoutException(timeoutExceptionString(ops));
        }
    }

    private String timeoutExceptionString(int ops) {
        String waitingFor = "" + ops;
        if (ops == SelectionKey.OP_READ) {
            waitingFor = "read";
        } else if (ops == SelectionKey.OP_WRITE) {
            waitingFor = "write";
        }
        return timeout + " millis timeout while " + "waiting for channel to be ready for " + waitingFor + ". ch : " + channel;
    }

    /**
   * This maintains a pool of selectors. These selectors are closed
   * once they are idle (unused) for a few seconds.
   */
    private static class SelectorPool {

        private static class SelectorInfo {

            Selector selector;

            long lastActivityTime;

            LinkedList<SelectorInfo> queue;

            void close() {
                if (selector != null) {
                    try {
                        selector.close();
                    } catch (IOException e) {
                        LOG.warn("Unexpected exception while closing selector : " + StringUtils.stringifyException(e));
                    }
                }
            }
        }

        private static class ProviderInfo {

            SelectorProvider provider;

            LinkedList<SelectorInfo> queue;

            ProviderInfo next;
        }

        private static final long IDLE_TIMEOUT = 10 * 1000;

        private ProviderInfo providerList = null;

        /**
     * Waits on the channel with the given timeout using one of the 
     * cached selectors. It also removes any cached selectors that are
     * idle for a few seconds.
     * 
     * @param channel
     * @param ops
     * @param timeout
     * @return
     * @throws IOException
     */
        int select(SelectableChannel channel, int ops, long timeout) throws IOException {
            SelectorInfo info = get(channel);
            SelectionKey key = null;
            int ret = 0;
            try {
                while (true) {
                    long start = (timeout == 0) ? 0 : System.currentTimeMillis();
                    key = channel.register(info.selector, ops);
                    ret = info.selector.select(timeout);
                    if (ret != 0) {
                        return ret;
                    }
                    if (timeout > 0) {
                        timeout -= System.currentTimeMillis() - start;
                        if (timeout <= 0) {
                            return 0;
                        }
                    }
                    if (Thread.currentThread().isInterrupted()) {
                        throw new InterruptedIOException("Interruped while waiting for " + "IO on channel " + channel + ". " + timeout + " millis timeout left.");
                    }
                }
            } finally {
                if (key != null) {
                    key.cancel();
                }
                try {
                    info.selector.selectNow();
                } catch (IOException e) {
                    LOG.info("Unexpected Exception while clearing selector : " + StringUtils.stringifyException(e));
                    info.close();
                    return ret;
                }
                release(info);
            }
        }

        /**
     * Takes one selector from end of LRU list of free selectors.
     * If there are no selectors awailable, it creates a new selector.
     * Also invokes trimIdleSelectors(). 
     * 
     * @param channel
     * @return 
     * @throws IOException
     */
        private synchronized SelectorInfo get(SelectableChannel channel) throws IOException {
            SelectorInfo selInfo = null;
            SelectorProvider provider = channel.provider();
            ProviderInfo pList = providerList;
            while (pList != null && pList.provider != provider) {
                pList = pList.next;
            }
            if (pList == null) {
                pList = new ProviderInfo();
                pList.provider = provider;
                pList.queue = new LinkedList<SelectorInfo>();
                pList.next = providerList;
                providerList = pList;
            }
            LinkedList<SelectorInfo> queue = pList.queue;
            if (queue.isEmpty()) {
                Selector selector = provider.openSelector();
                selInfo = new SelectorInfo();
                selInfo.selector = selector;
                selInfo.queue = queue;
            } else {
                selInfo = queue.removeLast();
            }
            trimIdleSelectors(System.currentTimeMillis());
            return selInfo;
        }

        /**
     * puts selector back at the end of LRU list of free selectos.
     * Also invokes trimIdleSelectors().
     * 
     * @param info
     */
        private synchronized void release(SelectorInfo info) {
            long now = System.currentTimeMillis();
            trimIdleSelectors(now);
            info.lastActivityTime = now;
            info.queue.addLast(info);
        }

        /**
     * Closes selectors that are idle for IDLE_TIMEOUT (10 sec). It does not
     * traverse the whole list, just over the one that have crossed 
     * the timeout.
     */
        private void trimIdleSelectors(long now) {
            long cutoff = now - IDLE_TIMEOUT;
            for (ProviderInfo pList = providerList; pList != null; pList = pList.next) {
                if (pList.queue.isEmpty()) {
                    continue;
                }
                for (Iterator<SelectorInfo> it = pList.queue.iterator(); it.hasNext(); ) {
                    SelectorInfo info = it.next();
                    if (info.lastActivityTime > cutoff) {
                        break;
                    }
                    it.remove();
                    info.close();
                }
            }
        }
    }
}
