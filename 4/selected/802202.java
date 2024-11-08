package org.jeuron.jlightning.connection.manager;

import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.jeuron.jlightning.connection.Connection;
import org.jeuron.jlightning.connection.commander.Commander;
import org.jeuron.jlightning.connection.commander.Request;
import org.jeuron.jlightning.connection.protocol.factory.ProtocolFactory;
import org.jeuron.jlightning.container.Container;
import org.jeuron.jlightning.event.EventCatagory;
import org.jeuron.jlightning.event.system.SystemEventType;
import org.jeuron.jlightning.event.system.ConnectionManagerEvent;
import org.jeuron.jlightning.message.handler.factory.MessageHandlerFactory;

/**
 * <p>Extended by {@link StandardSocketConnectionManager} and
 * {@link StandardDatagramConnectionManager} this class implements
 * {@link ConnectionManager} operations required for all sub-classes.
 * ConnectionManagers run in the NIO selector thread and are responsible for
 * for all housekeeping associated with the {@link Connection} creation and
 * termination. ConnectionManagers also provide a {@link Commander} interface
 * that is used to send life cycle commands to the connectionManager.
 *
 * @author Mike Karrys
 * @since 1.0
 * @see ConnectionManager
 * @see StandardSocketConnectionManager
 * @see StandardDatagramConnectionManager
 */
public abstract class AbstractConnectionManager implements ConnectionManager {

    private Thread me = null;

    protected Container container = null;

    protected Commander commander = null;

    protected MessageHandlerFactory messageHandlerFactory = null;

    protected ProtocolFactory protocolFactory = null;

    protected BlockingQueue<Request> requestQueue = null;

    protected BlockingQueue<SysOpsChange> sysOpsChangeQueue = null;

    protected Selector selector;

    protected final Object lock = new Object();

    protected volatile boolean running = false;

    /**
     * Sets the associated {@link Container} for the this connectionManager.
     * @param container
     */
    @Override
    public void setContainer(Container container) {
        this.container = container;
    }

    /**
     * Sets the associated {@link Commander} for the this connectionManager.
     * @param commander
     */
    @Override
    public void setCommander(Commander commander) {
        this.commander = commander;
    }

    /**
     * Sets the associated {@link MessageHandlerFactory} for the this connectionManager.
     * @param messageHandlerFactory
     */
    @Override
    public void setMessageHandlerFactory(MessageHandlerFactory messageHandlerFactory) {
        this.messageHandlerFactory = messageHandlerFactory;
    }

    /**
     * Sets the associated {@link ProtocolFactory} for the this connectionManager.
     * @param protocolFactory
     */
    @Override
    public void setProtocolFactory(ProtocolFactory protocolFactory) {
        this.protocolFactory = protocolFactory;
    }

    /**
     * Queue an interestOps change for the specified channel.
     * @param channel channel property
     * @param interestOps interestOps property
     */
    @Override
    public void queueSysOpsChangeForChannel(Channel channel, int interestOps) throws InterruptedException {
        SysOpsChange sysOpsChange = null;
        sysOpsChange = new SysOpsChange(channel, interestOps);
        sysOpsChangeQueue.put(sysOpsChange);
    }

    /**
     * Queues a {@link Request} on the requestQueue.
     * @param request a {@link Request} that contains the operation to perform
     */
    @Override
    public void queueRequest(Request request) throws ConnectionManagerException {
        container.sendEvent(new ConnectionManagerEvent(EventCatagory.DEBUG, SystemEventType.GENERAL, "queueRequest(" + request.getCommand() + ")", container.getName()));
        synchronized (lock) {
            if (isRunning()) {
                try {
                    requestQueue.put(request);
                    wakeupSelector();
                } catch (InterruptedException ex) {
                }
            } else {
                throw new ConnectionManagerException("ConnectionManager is not Running.");
            }
        }
    }

    /**
     * Returns <b>true</b> if the connectionManager is running.
     * @return boolean state of running connectionManager
     */
    @Override
    public void waitForThreadStart() {
        while (getThreadState() != java.lang.Thread.State.RUNNABLE) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
            }
        }
    }

    /**
     * Returns <b>true</b> if the connectionManager is running.
     * @return boolean state of running connectionManager
     */
    @Override
    public Thread.State getThreadState() {
        if (me != null) {
            return me.getState();
        } else {
            return Thread.State.NEW;
        }
    }

    /**
     * Returns <b>true</b> if the connectionManager is running.
     * @return boolean state of running connectionManager
     */
    @Override
    public boolean isRunning() {
        return running;
    }

    /**
     * Initialize connectionManager by opening the selector.
     */
    @Override
    public void init() throws IOException {
        requestQueue = new LinkedBlockingQueue<Request>();
        sysOpsChangeQueue = new LinkedBlockingQueue<SysOpsChange>();
        selector = Selector.open();
    }

    /**
     * Wakes up the Selector.
     */
    @Override
    public void wakeupSelector() {
        selector.wakeup();
    }

    /**
     * Initiates the shutdown process of the connectionManager.
     */
    protected void startTermination() {
        running = false;
        wakeupSelector();
    }

    /**
     * Called my sub-classes to finish the shutdown of the connectionManager.
     */
    protected void finishTermination() {
        try {
            selector.close();
        } catch (IOException e) {
            container.sendEvent(new ConnectionManagerEvent(EventCatagory.WARN, SystemEventType.EXCEPTION, "EXCEPTION: selector.close(" + e + ")", container.getName()));
        }
        container = null;
        commander = null;
        messageHandlerFactory = null;
        protocolFactory = null;
        requestQueue = null;
        sysOpsChangeQueue = null;
        selector = null;
        running = false;
    }

    /**
     * This abstract protected method allows sub-classes to initiate the shutdown
     * process.  Sub-classes must call {@link #finishShutdown()} to complete
     * the shutdown process.
     */
    protected abstract void doTerminate();

    /**
     * This abstract method requires all sub-classes to specify a set of
     * operations to control the connectionManager.
     * @param request a {@link Request} that contains the operation to perform
     */
    protected abstract void execute(Request request);

    /**
     * This abstract method requires all sub-classes to specify a set of validOps
     * key processing operations.
     * @param key {@link SelectionKey} key to operate on
     */
    protected abstract void processKey(SelectionKey key) throws ConnectionManagerException, InterruptedException;

    /**
     * Processes queued question to change interestOps on channels.
     */
    protected void processSysOpsChanges() throws InterruptedException {
        SelectionKey key = null;
        SysOpsChange sysOpsChange = null;
        while (!sysOpsChangeQueue.isEmpty()) {
            int size = sysOpsChangeQueue.size();
            int loop = size > container.getQueueSize() ? container.getQueueSize() : size;
            for (int i = 0; i < loop; i++) {
                sysOpsChange = (SysOpsChange) sysOpsChangeQueue.take();
                if (sysOpsChange.getChannel() instanceof SocketChannel) {
                    key = ((SocketChannel) sysOpsChange.getChannel()).keyFor(selector);
                    container.sendEvent(new ConnectionManagerEvent(EventCatagory.TRACE, SystemEventType.GENERAL, "processSysOpsChanges() - interestOps(" + sysOpsChange.getInterestOps() + ") socketChannel(" + ((Channel) sysOpsChange.getChannel()).toString() + ")", container.getName()));
                } else if (sysOpsChange.getChannel() instanceof DatagramChannel) {
                    key = ((DatagramChannel) sysOpsChange.getChannel()).keyFor(selector);
                } else {
                    container.sendEvent(new ConnectionManagerEvent(EventCatagory.WARN, SystemEventType.GENERAL, "processSysOpsChanges()- channel not socket or datagram.", container.getName()));
                }
                if (key.isValid()) {
                    key.interestOps(key.interestOps() | sysOpsChange.getInterestOps());
                } else {
                    container.sendEvent(new ConnectionManagerEvent(EventCatagory.TRACE, SystemEventType.GENERAL, "processSysOpsChanges().keyIsInvalid - interestOps(" + sysOpsChange.getInterestOps() + ") socketChannel(" + ((Channel) sysOpsChange.getChannel()).toString() + ")", container.getName()));
                }
            }
        }
    }

    /**
     * Selector thread blocks on the selector.select() until a connection is
     * received or write operation has been queued.
     */
    protected void task() {
        SelectionKey key = null;
        int readyChannels = 0;
        running = true;
        do {
            try {
                Iterator requestList = requestQueue.iterator();
                while (requestList.hasNext()) {
                    Request request = (Request) requestList.next();
                    execute(request);
                }
                requestQueue.clear();
                if (running) {
                    processSysOpsChanges();
                    readyChannels = selector.select();
                    Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
                    while (selectedKeys.hasNext()) {
                        key = (SelectionKey) selectedKeys.next();
                        selectedKeys.remove();
                        if (!key.isValid()) {
                            continue;
                        }
                        if (!key.isAcceptable()) {
                            int readyOps = key.readyOps();
                            key.interestOps(key.interestOps() & ~readyOps);
                        }
                        container.sendEvent(new ConnectionManagerEvent(EventCatagory.TRACE, SystemEventType.GENERAL, "task() - Before processKey(interestOps=" + key.interestOps() + ", readyOps=" + key.readyOps() + ")", container.getName()));
                        processKey(key);
                    }
                }
            } catch (InterruptedException ex) {
                running = false;
            } catch (IOException ex) {
                container.sendEvent(new ConnectionManagerEvent(EventCatagory.WARN, SystemEventType.EXCEPTION, "task() IOException(" + ex + ")", container.getName()));
            } catch (ConnectionManagerException ex) {
                container.sendEvent(new ConnectionManagerEvent(EventCatagory.WARN, SystemEventType.EXCEPTION, "task() ConnectionManagerException(" + ex + ")", container.getName()));
            }
        } while (running);
        finishTermination();
    }

    /**
     * Runs the Selector thread.
     *
     */
    @Override
    public void run() {
        me = Thread.currentThread();
        task();
    }
}
