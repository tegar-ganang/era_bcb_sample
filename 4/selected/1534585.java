package net.sf.syracuse.net.impl;

import net.sf.syracuse.threads.ThreadState;
import net.sf.syracuse.threads.ThreadStateManager;
import org.apache.commons.logging.Log;
import org.apache.hivemind.ServiceImplementationFactory;
import org.apache.hivemind.ServiceImplementationFactoryParameters;
import org.apache.hivemind.events.RegistryShutdownListener;
import java.io.IOException;

/**
 * Factory service which creates and starts a {@link net.sf.syracuse.net.NetworkEventThread}.  With the current
 * implementation it really doens't make any sense to create more than one {@code NetworkEventThread} since they all
 * end up with the same set of event handlers anyway.
 *
 * @author Chris Conrad
 * @since 1.0.0
 */
public final class NetworkEventThreadFactory implements ServiceImplementationFactory, RegistryShutdownListener {

    private AcceptEventHandler acceptEventHandler;

    private ReadEventHandler readEventHandler;

    private WriteEventHandler writeEventHandler;

    private ThreadStateManager threadStateManager;

    private Log log;

    private NetworkEventThreadImpl eventThread;

    /**
     * Instantiates and starts a new {@link net.sf.syracuse.net.NetworkEventThread}.
     *
     * @param factoryParameters {@inheritDoc}
     * @return the new {@code NetworkEventThread}
     */
    public Object createCoreServiceImplementation(ServiceImplementationFactoryParameters factoryParameters) {
        log = factoryParameters.getLog();
        log.info("Starting NetworkEventThread");
        try {
            eventThread = new NetworkEventThreadImpl();
        } catch (IOException e) {
            log.error("Error while creating NetworkEvenThread", e);
            return null;
        }
        eventThread.setAcceptEventHandler(acceptEventHandler);
        eventThread.setLog(factoryParameters.getLog());
        eventThread.setName("network-event-thread");
        eventThread.setReadEventHandler(readEventHandler);
        eventThread.setWriteEventHandler(writeEventHandler);
        eventThread.start();
        ThreadState threadState = new ThreadState();
        threadState.setName("RUNNING");
        threadStateManager.setThreadState(eventThread, threadState);
        return eventThread;
    }

    /**
     * Cleanly shuts down the {@code NetworkEventThread}.
     */
    public void registryDidShutdown() {
        log.info("Shutting down NetworkEventThread");
        eventThread.shutdown();
        try {
            eventThread.join();
        } catch (InterruptedException e) {
            log.info("Interrupted while waiting for NetworkEventThread to join", e);
        }
    }

    /**
     * Sets the {@code AcceptHandler} new {@code NetworkEventThreads} should use.
     *
     * @param acceptEventHandler the {@code AcceptHandler}
     */
    public void setAcceptHandler(AcceptEventHandler acceptEventHandler) {
        this.acceptEventHandler = acceptEventHandler;
    }

    /**
     * Sets the {@code ReadEventHandler} new {@code NetworkEventThreads} should use.
     *
     * @param readEventHandler the {@code ReadEventHandler}
     */
    public void setReadHandler(ReadEventHandler readEventHandler) {
        this.readEventHandler = readEventHandler;
    }

    /**
     * Sets the {@code WriteEventHandler} new {@code NetworkEventThreads} should use.
     *
     * @param writeEventHandler the {@code WriteEventHandler}
     */
    public void setWriteHandler(WriteEventHandler writeEventHandler) {
        this.writeEventHandler = writeEventHandler;
    }

    /**
     * Sets the {@code ThreadStateMananger}.
     *
     * @param threadStateManager the {@code ThreadStateManager}
     */
    public void setThreadStateManager(ThreadStateManager threadStateManager) {
        this.threadStateManager = threadStateManager;
    }
}
