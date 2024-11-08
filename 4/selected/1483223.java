package org.otfeed.protocol.connector;

import org.otfeed.OTConnectionSpec;
import org.otfeed.event.IConnectionStateListener;
import org.otfeed.event.OTError;
import org.otfeed.protocol.ErrorEnum;
import org.otfeed.protocol.message.ErrorResponse;
import org.otfeed.protocol.message.HeartbeatRequest;
import org.otfeed.protocol.message.Message;
import org.otfeed.protocol.message.SessionMarshaler;
import org.otfeed.protocol.request.AbstractRequest;
import org.otfeed.protocol.request.CancelRequest;
import org.otfeed.protocol.request.Util;
import org.otfeed.support.IBufferAllocator;
import java.util.Queue;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.io.IOException;
import java.nio.ByteBuffer;
import static org.otfeed.protocol.request.AbstractRequest.JobStatus;
import static org.otfeed.protocol.request.Util.newError;

class OTThreadingEngine {

    private static final int BUFFER_SIZE = 1024;

    private final Queue<AbstractRequest> writeQueue = new ConcurrentLinkedQueue<AbstractRequest>();

    private final Queue<ByteBuffer> readQueue = new ConcurrentLinkedQueue<ByteBuffer>();

    private final Queue<Runnable> taskQueue = new ConcurrentLinkedQueue<Runnable>();

    private final Map<Integer, AbstractRequest> pendingJobs = new ConcurrentHashMap<Integer, AbstractRequest>();

    private final IBufferAllocator allocator;

    private final Executor executor;

    private final ISessionStreamerFactory factory;

    private final OTConnectionSpec spec;

    private final IConnectionStateListener listener;

    private ISessionStreamer connector = null;

    private final AtomicReference<OTError> error = new AtomicReference<OTError>();

    private static void setThreadName(String name) {
        try {
            Thread.currentThread().setName(name);
        } catch (Exception ex) {
        }
    }

    protected OTThreadingEngine(ISessionStreamerFactory f, OTConnectionSpec s, IBufferAllocator a, Executor exec, IConnectionStateListener l) {
        factory = f;
        spec = new OTConnectionSpec(s);
        allocator = a;
        listener = l;
        if (exec == null) {
            executor = Executors.newCachedThreadPool();
        } else {
            executor = exec;
        }
        isControlRunning.set(true);
        executor.execute(new Runnable() {

            public void run() {
                setThreadName("control-thread");
                controlThread();
            }
        });
    }

    private static final long SLEEP_DELAY = 1000;

    private final AtomicBoolean isWriteRunning = new AtomicBoolean(false);

    private void checkWriteInterrupted() throws InterruptedException {
        if (!isWriteRunning.get()) throw new InterruptedException();
    }

    private void interruptWriteThread() {
        isWriteRunning.set(false);
        synchronized (isWriteRunning) {
            isWriteRunning.notifyAll();
        }
    }

    private void writeThread() {
        try {
            writeThreadRunner();
        } catch (InterruptedException ex) {
            error.compareAndSet(null, newError("interrupted"));
            interruptControlThread();
        }
    }

    private void writeThreadRunner() throws InterruptedException {
        ByteBuffer outputBuffer = allocator.allocate(BUFFER_SIZE);
        long heartbeatIntervalMillis = spec.getHeartbeatInterval();
        while (true) {
            checkWriteInterrupted();
            long lastBufferTimestamp = System.currentTimeMillis();
            while (writeQueue.size() == 0) {
                if (System.currentTimeMillis() - lastBufferTimestamp > heartbeatIntervalMillis) {
                    break;
                }
                synchronized (isWriteRunning) {
                    isWriteRunning.wait(SLEEP_DELAY);
                }
                checkWriteInterrupted();
            }
            AbstractRequest job = writeQueue.poll();
            outputBuffer.clear();
            if (job != null) {
                pendingJobs.put(job.getMessage().getRequestId(), job);
                sessionMarshaler.marshal(job.getMessage(), outputBuffer);
            } else {
                HeartbeatRequest hb = new HeartbeatRequest(0);
                sessionMarshaler.marshal(hb, outputBuffer);
            }
            outputBuffer.flip();
            try {
                connector.write(outputBuffer);
            } catch (IOException ex) {
                error.compareAndSet(null, newError("io error in writer: " + ex.getMessage()));
                throw new InterruptedException();
            }
        }
    }

    private final AtomicBoolean isReadRunning = new AtomicBoolean(false);

    private void checkReadInterrupted() throws InterruptedException {
        if (!isReadRunning.get()) throw new InterruptedException();
    }

    private void interruptReadThread() {
        isReadRunning.set(false);
    }

    private void readThread() {
        try {
            readThreadRunner();
        } catch (InterruptedException ex) {
            error.compareAndSet(null, newError("interrupted"));
            interruptControlThread();
        }
    }

    private void readThreadRunner() throws InterruptedException {
        while (true) {
            checkReadInterrupted();
            try {
                ByteBuffer in = connector.read();
                readQueue.offer(in);
                synchronized (isControlRunning) {
                    isControlRunning.notifyAll();
                }
            } catch (IOException ex) {
                error.compareAndSet(null, newError("io exception in reader: " + ex.getMessage()));
                throw new InterruptedException();
            }
        }
    }

    private final AtomicBoolean isControlRunning = new AtomicBoolean(false);

    private void checkControlInterrupted() throws InterruptedException {
        if (!isControlRunning.get()) throw new InterruptedException();
    }

    private void interruptControlThread() {
        isControlRunning.set(false);
        synchronized (isControlRunning) {
            isControlRunning.notifyAll();
        }
    }

    private SessionMarshaler sessionMarshaler;

    private void controlRunner() throws InterruptedException {
        try {
            connector = factory.connect(spec, listener);
        } catch (LoginFailureException ex) {
            error.compareAndSet(null, ex.error);
            throw new InterruptedException();
        } catch (Throwable ex) {
            error.compareAndSet(null, newError("connection failed: " + ex.getMessage()));
            throw new InterruptedException();
        }
        checkControlInterrupted();
        sessionMarshaler = new SessionMarshaler(false, connector.getSessionId());
        final CountDownLatch latch = new CountDownLatch(2);
        isWriteRunning.set(true);
        executor.execute(new Runnable() {

            public void run() {
                try {
                    setThreadName("writer-thread");
                    writeThread();
                } finally {
                    latch.countDown();
                }
            }
        });
        isReadRunning.set(true);
        executor.execute(new Runnable() {

            public void run() {
                try {
                    setThreadName("reader-thread");
                    readThread();
                } finally {
                    latch.countDown();
                }
            }
        });
        try {
            while (true) {
                checkControlInterrupted();
                while (taskQueue.size() == 0 && readQueue.size() == 0) {
                    synchronized (isControlRunning) {
                        isControlRunning.wait();
                    }
                    checkControlInterrupted();
                }
                Runnable task;
                while ((task = taskQueue.poll()) != null) {
                    task.run();
                    checkControlInterrupted();
                }
                ByteBuffer buffer;
                while ((buffer = readQueue.poll()) != null) {
                    handleMessage(buffer);
                    checkControlInterrupted();
                    while ((task = taskQueue.poll()) != null) {
                        task.run();
                        checkControlInterrupted();
                    }
                }
            }
        } finally {
            interruptWriteThread();
            interruptReadThread();
            connector.close();
            latch.await();
        }
    }

    private void controlThread() {
        try {
            controlRunner();
            shutdown();
        } catch (InterruptedException ex) {
        }
        error.compareAndSet(null, newError("unexpected shutdown"));
        for (AbstractRequest r : writeQueue) {
            r.fireCompleted(error.get());
        }
        writeQueue.clear();
        for (AbstractRequest r : pendingJobs.values()) {
            r.fireCompleted(error.get());
        }
        pendingJobs.clear();
        listener.onError(error.get());
        isFinished.set(true);
        synchronized (isFinished) {
            isFinished.notifyAll();
        }
    }

    private final AtomicBoolean isShutdown = new AtomicBoolean(false);

    public void shutdown() {
        if (isShutdown.getAndSet(true)) {
            return;
        }
        error.compareAndSet(null, new OTError(ErrorEnum.E_OTFEED_OK.code, "shutdown"));
        interruptControlThread();
    }

    private final AtomicBoolean isFinished = new AtomicBoolean(false);

    public boolean isFinished() {
        return isFinished.get();
    }

    public void waitForCompletion() {
        try {
            synchronized (isFinished) {
                while (!isFinished.get()) {
                    isFinished.wait();
                }
            }
        } catch (InterruptedException ex) {
            shutdown();
        }
    }

    public boolean waitForCompletion(long millis) {
        long target = System.currentTimeMillis() + millis;
        try {
            synchronized (isFinished) {
                while (!isFinished.get()) {
                    long now = System.currentTimeMillis();
                    if (now <= target) break;
                    isFinished.wait(target - now);
                }
            }
        } catch (InterruptedException ex) {
            shutdown();
        }
        return isFinished.get();
    }

    private void handleMessage(ByteBuffer in) {
        Message response = sessionMarshaler.unmarshal(in);
        if (response instanceof ErrorResponse) {
            AbstractRequest job = pendingJobs.remove(response.getRequestId());
            if (job == null) {
                return;
            }
            job.fireCompleted(((ErrorResponse) response).error);
        } else {
            AbstractRequest job = pendingJobs.get(response.getRequestId());
            if (job == null) {
                return;
            }
            try {
                JobStatus status = job.handleMessage(response);
                if (status == JobStatus.FINISHED) {
                    pendingJobs.remove(response.getRequestId());
                    job.fireCompleted(null);
                }
            } catch (Exception ex) {
                ex.printStackTrace(System.out);
                System.out.println(ex);
                pendingJobs.remove(response.getRequestId());
                job.fireCompleted(Util.newError(ex.getMessage()));
            }
        }
    }

    /**
	 * Utility methods for subclasses: submits request.
	 */
    void submit(AbstractRequest request) {
        if (error.get() != null) {
            throw new IllegalStateException("shutting down");
        }
        writeQueue.offer(request);
        synchronized (isWriteRunning) {
            isWriteRunning.notifyAll();
        }
    }

    /**
	 * Utility method for subclasses: cancels request.
	 */
    void cancel(int requestId, final AbstractRequest request) {
        final Message cancelMessage = request.getCancelMessage(requestId);
        if (cancelMessage != null) {
            submit(new CancelRequest(cancelMessage));
        }
        runInEventThread(new Runnable() {

            public void run() {
                AbstractRequest target = pendingJobs.remove(request.getMessage().getRequestId());
                if (target != null) {
                    target.fireCompleted(new OTError(ErrorEnum.E_OTFEED_CANCELLED.code, "cancelled"));
                }
            }
        });
    }

    public void runInEventThread(Runnable runnable) {
        if (isShutdown.get()) {
            throw new IllegalStateException("shutdown");
        }
        taskQueue.offer(runnable);
        synchronized (isControlRunning) {
            isControlRunning.notifyAll();
        }
    }
}
