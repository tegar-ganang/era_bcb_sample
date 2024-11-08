package jather;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jgroups.Address;
import org.jgroups.ChannelException;
import org.jgroups.ExtendedReceiverAdapter;
import org.jgroups.View;
import org.jgroups.blocks.RpcDispatcher;

/**
 * This class provides a client interface to the clustered executives.
 * 
 * @author blueneil
 * 
 */
public class JatherClient extends GroupMember {

    private static final Log log = LogFactory.getLog(JatherClient.class);

    private Map<ProcessId, LockedProcessContext> processMap = Collections.synchronizedMap(new HashMap<ProcessId, LockedProcessContext>());

    private static final AtomicLong idGenerator = new AtomicLong();

    private RpcDispatcher dispatcher;

    private JatherHandlerStub stub;

    private Timer timer;

    private View view;

    private Executive localExec;

    private boolean runLocalExec = true;

    /**
	 * Default constructor.
	 */
    public JatherClient() {
        this(null);
    }

    /**
	 * Named constructor.
	 * 
	 * @param name
	 *            the cluster name to connect to.
	 */
    public JatherClient(String name) {
        this(name, true);
    }

    /**
	 * Named constructor.
	 * 
	 * @param name
	 *            the cluster name to connect to.
	 * @param localExecFlag
	 *            the local executive flag.
	 */
    public JatherClient(String name, boolean localExecFlag) {
        setClusterName(name);
        setRunLocalExec(localExecFlag);
    }

    /**
	 * The map of process ids to process context data.
	 * 
	 * @return the processMap
	 */
    public Map<ProcessId, LockedProcessContext> getProcessMap() {
        return processMap;
    }

    /**
	 * Get the next process id.
	 * 
	 * @return the next process id.
	 * @throws ChannelException
	 *             If the local channel address could not be obtained.
	 */
    public ProcessId nextId() throws ChannelException {
        return new ProcessId(getConnectedChannel().getLocalAddress(), "id-" + getIdGenerator().getAndIncrement());
    }

    /**
	 * The atomic process id generator.
	 * 
	 * @return the idGenerator
	 */
    public AtomicLong getIdGenerator() {
        return idGenerator;
    }

    /**
	 * Close the client and free the resources.
	 */
    public void close() {
        setTimer(null);
        closeChannel();
        getProcessMap().clear();
        setLocalExec(null);
    }

    /**
	 * Add a new callable instance to the process map.
	 * 
	 * @param callable
	 *            the callable instance to add.
	 * @return the new callable context.
	 * @throws ChannelException
	 *             If the process id could not be generated.
	 * @throws IOException
	 *             If the callable could not be serialized.
	 */
    protected LockedProcessContext addCallable(Callable<?> callable) throws ChannelException, IOException {
        LockedProcessContext ctxt = new LockedProcessContext(new ProcessContext(nextId(), callable));
        getProcessMap().put(ctxt.getProcessContext().getProcessId(), ctxt);
        return ctxt;
    }

    /**
	 * Execute the call and wait for a result.
	 * 
	 * @param <V>
	 * 
	 * @param callable
	 *            the callable to execute.
	 * @return the callable result.
	 * @throws Exception
	 *             Any exception thrown by the callable.
	 */
    public <V extends Serializable> V execute(SerializableCallable<V> callable) throws Exception {
        try {
            return (V) submit(callable).get();
        } catch (ExecutionException e) {
            throw (Exception) e.getCause();
        }
    }

    /**
	 * Submit the call returning a future handle.
	 * 
	 * @param <V>
	 * 
	 * @param callable
	 *            the callable to execute
	 * @return the callable future.
	 */
    public <V extends Serializable> Future<V> submit(SerializableCallable<V> callable) throws Exception {
        final LockedProcessContext locked = addCallable(callable);
        getStub().receiveRequest(locked.getProcessDefinition());
        return new Future<V>() {

            private boolean cancelled;

            /**
			 * @see java.util.concurrent.Future#cancel(boolean)
			 */
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                boolean sent = !cancelled;
                try {
                    getStub().receiveCancelRequest(locked.getProcessDefinition(), mayInterruptIfRunning);
                    getProcessMap().remove(locked.getProcessContext().getProcessId());
                    cancelled = true;
                } catch (ChannelException e) {
                    throw new RuntimeException(e);
                } catch (JatherException e) {
                    throw new RuntimeException(e);
                }
                return sent;
            }

            /**
			 * Get the process result.
			 * 
			 * @return the process result.
			 * @throws ExecutionException
			 */
            private Object getResult() throws ExecutionException {
                if (locked.getProcessContext().getCallableException() != null) {
                    throw new ExecutionException(locked.getProcessContext().getCallableException());
                }
                return locked.getProcessContext().getResult();
            }

            /**
			 * @see java.util.concurrent.Future#get()
			 */
            @SuppressWarnings("unchecked")
            @Override
            public V get() throws InterruptedException, ExecutionException {
                locked.waitForLock();
                return (V) getResult();
            }

            /**
			 * @see java.util.concurrent.Future#get(long,
			 *      java.util.concurrent.TimeUnit)
			 */
            @SuppressWarnings("unchecked")
            @Override
            public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                if (!locked.waitForLock(timeout, unit)) {
                    throw new TimeoutException();
                }
                return (V) getResult();
            }

            /**
			 * @see java.util.concurrent.Future#isCancelled()
			 */
            @Override
            public boolean isCancelled() {
                return cancelled;
            }

            /**
			 * @see java.util.concurrent.Future#isDone()
			 */
            @Override
            public boolean isDone() {
                return (locked.getProcessContext().getCallableException() != null) || (locked.getProcessContext().getResult() != null);
            }
        };
    }

    /**
	 * The local callable context.
	 * 
	 * @author blueneil
	 * 
	 */
    class LockedProcessContext {

        private ProcessContext processContext;

        private ProcessDefinition processDefinition;

        private Semaphore semaphore = new Semaphore(1);

        private Address executive;

        private Lock execLock = new ReentrantLock();

        /**
		 * Callable constructor.
		 * 
		 * @param processContext
		 *            the processContext to store.
		 */
        public LockedProcessContext(ProcessContext processContext) {
            this.processContext = processContext;
            try {
                semaphore.acquire();
            } catch (InterruptedException e) {
                log.error("Unable to create a locked context.", e);
            }
        }

        /**
		 * @return the execLock
		 */
        public Lock getExecLock() {
            return execLock;
        }

        /**
		 * @return the executive
		 */
        public Address getExecutive() {
            return executive;
        }

        /**
		 * @param executive
		 *            the executive to set
		 */
        public void setExecutive(Address executive) {
            this.executive = executive;
        }

        /**
		 * @param processContext
		 *            the processContext to set
		 */
        public void setProcessContext(ProcessContext processContext) {
            this.processContext = processContext;
        }

        /**
		 * @return the processContext
		 */
        public ProcessContext getProcessContext() {
            processContext.setExecutiveAddress(getExecutive());
            return processContext;
        }

        /**
		 * Wait for the lock to open.
		 * 
		 * 
		 * @throws InterruptedException
		 *             If the wait was interrupted.
		 */
        public void waitForLock() throws InterruptedException {
            semaphore.acquire();
            semaphore.release();
        }

        /**
		 * Wait for the lock to open.
		 * 
		 * @param timeout
		 *            the maximum time to wait for a permit
		 * @param unit
		 *            the time unit of the timeout argument
		 * @return true if lock aquired else false.
		 * @throws InterruptedException
		 *             If the wait was interrupted.
		 */
        public boolean waitForLock(long timeout, TimeUnit unit) throws InterruptedException {
            boolean aquired = semaphore.tryAcquire(timeout, unit);
            if (aquired) {
                semaphore.release();
            }
            return aquired;
        }

        /**
		 * Unlock the process.
		 */
        public void unlock() {
            semaphore.release();
        }

        /**
		 * @return the processDefinition
		 */
        public ProcessDefinition getProcessDefinition() {
            if (processDefinition == null) {
                processDefinition = new ProcessDefinition(getProcessContext().getProcessId());
            }
            return processDefinition;
        }

        /**
		 * @param processDefinition
		 *            the processDefinition to set
		 */
        public void setProcessDefinition(ProcessDefinition processDefinition) {
            this.processDefinition = processDefinition;
        }
    }

    /**
	 * Get or create a new {@link JatherHandlerStub}.
	 * 
	 * @return the stub
	 * @throws ChannelException
	 *             if the dispatcher could not be connected.
	 */
    public JatherHandlerStub getStub() throws ChannelException {
        if (stub == null) {
            stub = new JatherHandlerStub(getDispatcher());
        }
        return stub;
    }

    /**
	 * The handler stub to call through.
	 * 
	 * @param stub
	 *            the stub to set
	 */
    public void setStub(JatherHandlerStub stub) {
        this.stub = stub;
    }

    /**
	 * Get the rpc dispatcher, creating a new one if it is not set.
	 * 
	 * @return the dispatcher
	 * @throws ChannelException
	 *             if the dispatcher could not be connected.
	 */
    public RpcDispatcher getDispatcher() throws ChannelException {
        if (dispatcher == null) {
            dispatcher = new RpcDispatcher(getConnectedChannel(), null, null, new ClientHandler());
            setView(dispatcher.getChannel().getView());
            if (isRunLocalExec()) {
                Executive exec = new Executive();
                exec.setClusterName(getClusterName());
                exec.setMaxGroupSize(0);
                setLocalExec(exec);
                exec.start();
            }
            timer = new Timer(true);
            timer.schedule(new ChannelTimer(), 0, 5000);
        }
        return dispatcher;
    }

    class JatherListener extends ExtendedReceiverAdapter {

        /**
		 * @see org.jgroups.ExtendedReceiverAdapter#viewAccepted(org.jgroups.View)
		 */
        @Override
        public void viewAccepted(final View newView) {
            new Thread() {

                /**
				 * @see java.lang.Thread#run()
				 */
                @Override
                public void run() {
                    updateProcesses(newView);
                }
            }.start();
        }
    }

    /**
	 * Update the status of the processes in the map.
	 * 
	 * @param newView
	 *            the new channel view.
	 */
    public void updateProcesses(View newView) {
        try {
            if (!getView().getMembers().containsAll(newView.getMembers())) {
                for (LockedProcessContext c : getProcessMap().values()) {
                    if (c.getExecutive() == null) {
                        try {
                            getStub().receiveRequest(c.getProcessDefinition());
                        } catch (JatherException e) {
                            log.error(e);
                        }
                    }
                }
            }
            for (LockedProcessContext c : getProcessMap().values()) {
                if (c.getExecutive() != null) {
                    try {
                        c.getExecLock().lock();
                        if (!newView.containsMember(c.getExecutive())) {
                            log.debug("Looks like executive is gone, resubmitting process:" + c);
                            c.setExecutive(null);
                            getStub().receiveRequest(c.getProcessDefinition());
                        }
                    } catch (JatherException e) {
                        log.error(e);
                    } finally {
                        c.getExecLock().unlock();
                    }
                }
            }
            setView(newView);
        } catch (ChannelException e) {
            log.error(e);
            throw new RuntimeException(e);
        }
    }

    /**
	 * The process map update timer.
	 * 
	 * @author blueneil
	 * 
	 */
    class ChannelTimer extends TimerTask {

        /**
		 * @see java.util.TimerTask#run()
		 */
        @Override
        public void run() {
            try {
                View newView = getDispatcher().getChannel().getView();
                updateProcesses(newView);
            } catch (ChannelException e) {
                log.error(e);
                throw new RuntimeException(e);
            }
        }
    }

    /**
	 * The RpcDispatcher for message passing.
	 * 
	 * @param dispatcher
	 *            the dispatcher to set
	 */
    public void setDispatcher(RpcDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    /**
	 * Local handler of jather methods.
	 * 
	 * @author blueneil
	 * 
	 */
    class ClientHandler extends JatherHandlerAdapter {

        /**
		 * @see jather.JatherHandlerAdapter#processResult(jather.ProcessContext)
		 */
        @Override
        public void processResult(ProcessContext context) throws JatherException {
            LockedProcessContext locked = getProcessMap().get(context.getProcessId());
            if (locked != null && locked.getExecutive().equals(context.getExecutiveAddress())) {
                log.debug("Got result:" + context.getProcessId());
                if (locked != null) {
                    locked.setProcessContext(context);
                    locked.unlock();
                }
            }
        }

        /**
		 * @see jather.JatherHandler#processRequest(jather.RequestContext)
		 */
        @Override
        public ProcessContext processRequest(RequestContext requestContext) throws JatherException {
            log.debug("Process request:" + requestContext.getProcessId());
            try {
                ProcessContext ctxt;
                ctxt = new ProcessContext(requestContext.getProcessId(), null);
                LockedProcessContext locked = getProcessMap().get(requestContext.getProcessId());
                if (locked != null) {
                    try {
                        locked.getExecLock().lock();
                        if (locked.getExecutive() == null && requestContext.getExecutiveAddress() != null) {
                            locked.setExecutive(requestContext.getExecutiveAddress());
                            ctxt = locked.getProcessContext();
                        }
                    } finally {
                        locked.getExecLock().unlock();
                    }
                }
                return ctxt;
            } catch (IOException e) {
                throw new JatherException(e);
            }
        }
    }

    /**
	 * Start the client process if it is not already running.
	 * 
	 * @throws ChannelException
	 *             If the channel could not be started.
	 */
    public void start() throws ChannelException {
        getDispatcher();
    }

    /**
	 * @return the timer
	 */
    public Timer getTimer() {
        return timer;
    }

    /**
	 * @param timer
	 *            the timer to set
	 */
    public void setTimer(Timer timer) {
        if (this.timer != null) {
            this.timer.cancel();
        }
        this.timer = timer;
    }

    /**
	 * The last channel view.
	 * 
	 * @return the view
	 */
    public View getView() {
        return view;
    }

    /**
	 * The last channel view.
	 * 
	 * @param view
	 *            the view to set
	 */
    public void setView(View view) {
        this.view = view;
    }

    /**
	 * @return the localExec
	 */
    public Executive getLocalExec() {
        return localExec;
    }

    /**
	 * @param localExec
	 *            the localExec to set
	 */
    public void setLocalExec(Executive localExec) {
        if (this.localExec != null) {
            this.localExec.close();
        }
        this.localExec = localExec;
    }

    /**
	 * @return the runLocalExec
	 */
    protected boolean isRunLocalExec() {
        return runLocalExec;
    }

    /**
	 * @param runLocalExec
	 *            the runLocalExec to set
	 */
    protected void setRunLocalExec(boolean runLocalExec) {
        this.runLocalExec = runLocalExec;
    }
}
