package org.sourceforge.jemmrpc.shared;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import org.apache.log4j.Logger;
import org.sourceforge.jemmrpc.client.RPCClient;
import org.sourceforge.jemmrpc.server.RPCServer;

/**
 * RPCHandler is used to implement the shared logic used by both {@link RPCClient} and
 * {@link RPCServer}. Given a socket RPCHandler basically manages the client and server interfaces,
 * the proxies and the actual message passing.
 *
 * N.B. A client/server flag is passed to the constructor because {@link ObjectInputStream}/{@link ObjectOutputStream}
 * must be invoked in alternate order on the two sides of the connection or it deadlocks.
 *
 * @author Rory Graves
 *
 */
public class RPCHandler implements Runnable {

    Logger logger = Logger.getLogger(RPCHandler.class);

    Socket socket;

    protected CountDownLatch initialisationLatch = new CountDownLatch(1);

    protected volatile boolean closing = false;

    protected ObjectInputStream is;

    protected ObjectOutputStream os;

    Map<Class<?>, Object> remoteInterfaces;

    Map<Class<?>, Object> localInterfaces;

    ExecutorService requestExecutor;

    ThreadLocal<String> threadIdTL = new ThreadLocal<String>();

    ThreadLocal<SynchronousQueue<Message>> syncQueueTL = new ThreadLocal<SynchronousQueue<Message>>();

    ConcurrentHashMap<String, SynchronousQueue<Message>> msgSyncPoints = new ConcurrentHashMap<String, SynchronousQueue<Message>>();

    boolean isClient;

    static ThreadLocal<Object> connectionIdTL = new ThreadLocal<Object>();

    protected RPCHandlerListener listener = null;

    Object connectionId;

    /**
     * Create an RPCHandler
     *
     * @param isClient Is this the client side of the connection (false for server)
     * @param socket The socket connection.
     * @param localInterfaces The interfaces this side offers.
     * @param requestExecutor The service to use for running requests.
     * @param connectionId The id of the connection.
     */
    public RPCHandler(boolean isClient, Socket socket, Map<Class<?>, Object> localInterfaces, ExecutorService requestExecutor, Object connectionId) {
        this.isClient = isClient;
        this.socket = socket;
        this.localInterfaces = localInterfaces;
        this.requestExecutor = requestExecutor;
        this.connectionId = connectionId;
    }

    /**
     * Set the listener for this handler for event notification.
     *
     * @param listener The listener to inform of events.
     */
    public synchronized void setHandlerListener(RPCHandlerListener listener) {
        this.listener = listener;
    }

    /**
     * Returns an local interface for calling the remote interface.
     *
     * @param ifClass The interface to get.
     * @return An object which offers the interface.
     * @throws IllegalArgumentException If the requested class is not an interface or not supported.
     */
    public synchronized Object getRemoteIF(Class<?> ifClass) {
        if (!ifClass.isInterface()) throw new IllegalArgumentException("given class is not an interface");
        Object obj = remoteInterfaces.get(ifClass);
        if (obj == null) {
            if (!remoteInterfaces.keySet().contains(ifClass)) throw new IllegalArgumentException("Interface " + ifClass + " not offered by server");
            obj = createProxyClass(ifClass);
            remoteInterfaces.put(ifClass, obj);
        }
        return obj;
    }

    protected Object createProxyClass(Class<?> ifClass) {
        final Class<?>[] ifs = { ifClass };
        final RPCProxyHandler ph = new RPCProxyHandler(this, ifClass);
        final Object obj = Proxy.newProxyInstance(this.getClass().getClassLoader(), ifs, ph);
        return obj;
    }

    public void run() {
        try {
            if (isClient) {
                is = new ObjectInputStream(socket.getInputStream());
                os = new ObjectOutputStream(socket.getOutputStream());
            } else {
                os = new ObjectOutputStream(socket.getOutputStream());
                is = new ObjectInputStream(socket.getInputStream());
            }
            final AvailableIFsMessage ifOMsg = new AvailableIFsMessage(localInterfaces.keySet().toArray(new Class<?>[0]));
            os.writeObject(ifOMsg);
            os.flush();
            try {
                final AvailableIFsMessage ifIMsg = (AvailableIFsMessage) is.readObject();
                synchronized (this) {
                    remoteInterfaces = new HashMap<Class<?>, Object>();
                    for (final Class<?> ifClass : ifIMsg.offeredIFs) remoteInterfaces.put(ifClass, null);
                }
            } catch (final ClassNotFoundException e) {
                throw new IOException("Error initialising connection");
            }
            initialisationLatch.countDown();
            while (true) {
                Object o = null;
                try {
                    o = is.readObject();
                } catch (final ClassNotFoundException e1) {
                    e1.printStackTrace();
                }
                if (o instanceof Message) {
                    final Message message = (Message) o;
                    try {
                        receiveMessage(message);
                    } catch (final Exception e) {
                        logger.warn("exception thrown whilst sending message to receiver", e);
                    }
                } else logger.warn("Invalid object on stream " + o);
            }
        } catch (final IOException se) {
            connectionTerminated();
        }
    }

    protected void receiveMessage(Message message) {
        if (message instanceof RPCCallRespMessage) {
            final String threadId = message.getThreadId();
            final SynchronousQueue<Message> replyQueue = msgSyncPoints.get(threadId);
            if (replyQueue != null) try {
                replyQueue.put(message);
            } catch (final InterruptedException e) {
                logger.info("Receive thread interrupted");
            } else logger.error("No client thread found for sync message to " + threadId);
        } else if (message instanceof RPCCallMessage) processCallMessage((RPCCallMessage) message); else if (message instanceof ErrorMessage) logger.warn("Error message recieved from server " + ((ErrorMessage) message).errorMsg); else logger.warn("Invalid message type received by client: " + message.getClass());
    }

    /**
     * Internal method to handle RPC call method. This will invoke a thread using the executor
     * supplied in the constructor.
     *
     * @param message The message to process.
     */
    protected void processCallMessage(final RPCCallMessage message) {
        requestExecutor.execute(new Runnable() {

            public void run() {
                connectionIdTL.set(connectionId);
                try {
                    final Class<?> targetIF = message.getIfClass();
                    final Object targetIFImpl = localInterfaces.get(targetIF);
                    if (targetIFImpl != null) {
                        final Method method = targetIF.getMethod(message.methodName, message.parameterTypes);
                        if (method == null) throw new IllegalArgumentException("Interface method does not exist");
                        final Object resp = method.invoke(targetIFImpl, message.getParameters());
                        if (!message.asyncCall) writeMessage(new RPCCallRespMessage(message.threadId, true, resp));
                    } else throw new IllegalArgumentException("Interface not supported");
                } catch (final Exception e) {
                    final Throwable cause = e instanceof InvocationTargetException ? e.getCause() : e;
                    if (!message.asyncCall) writeMessage(new RPCCallRespMessage(message.threadId, false, cause)); else logger.warn("Exception caught whilst processing async call to " + message.ifClass + "." + message.methodName + "()", e);
                }
                connectionIdTL.set(null);
            }
        });
    }

    protected synchronized void connectionTerminated() {
        if (!closing) if (isClient) logger.error("Lost connection to Server"); else logger.info("Lost connection to client");
        closing = true;
        if (msgSyncPoints.size() > 0) {
            logger.error("Client connection closed with waiters active");
            final List<String> list = new ArrayList<String>();
            list.addAll(msgSyncPoints.keySet());
            final ErrorMessage errorMessage = new ErrorMessage("Server connection lost");
            for (final String threadId : list) {
                final SynchronousQueue<Message> queue = msgSyncPoints.get(threadId);
                if (queue != null) if (!queue.offer(errorMessage)) logger.warn("Unable to inform thread " + threadId + " of connection close");
            }
        }
        if (listener != null) listener.connectionTerminated();
    }

    /**
     * Close the connection to the server.
     */
    public void close() {
        closing = true;
        try {
            socket.close();
        } catch (final IOException ioe) {
            logger.warn("IOException thrown whilst closing client socket", ioe);
        }
    }

    protected synchronized void writeMessage(Message message) {
        try {
            os.writeObject(message);
            os.flush();
        } catch (final NotSerializableException nse) {
            logger.error("Sent message not serializable " + nse);
        } catch (final IOException e) {
            logger.warn("error caught writing object", e);
        }
    }

    /**
     * Send a synchronous message to the server. This method sends the given message and waits for a
     * response message.
     *
     * @param message The message to send
     * @return The message received in reply
     */
    public Message sendSyncMessage(Message message) {
        final String threadId = ThreadUtil.getThreadId();
        SynchronousQueue<Message> sq = syncQueueTL.get();
        if (sq == null) {
            sq = new SynchronousQueue<Message>();
            syncQueueTL.set(sq);
        }
        msgSyncPoints.put(threadId, sq);
        writeMessage(message);
        Message replyMsg = null;
        try {
            replyMsg = sq.take();
        } catch (final InterruptedException ie) {
            replyMsg = new ErrorMessage("InterruptedException received whilst waiting for reply");
        }
        msgSyncPoints.remove(threadId);
        return replyMsg;
    }

    /**
     * Internal method to make an asynchronous call to a given method.
     *
     * @param ifClass The interface class being called.
     * @param methodName The method being called.
     * @param parameterTypes The parameter types of the method
     * @param args The actual call arguments (based types wrapped by Proxy.invoke).
     */
    protected void makeAsyncCall(Class<?> ifClass, String methodName, Class<?>[] parameterTypes, Object[] args) {
        final RPCCallMessage callMessage = new RPCCallMessage(ThreadUtil.getThreadId(), true, ifClass, methodName, parameterTypes, args);
        writeMessage(callMessage);
    }

    /**
     * Internal method to make a make a synchronous call to a given method.
     *
     * @param ifClass The interface class being called.
     * @param methodName The method being called.
     * @param parameterTypes The parameter types of the method
     * @param args The actual call arguments (based types wrapped by Proxy.invoke).
     * @return The response message received from the target.
     */
    protected RPCCallRespMessage makeSyncCall(Class<?> ifClass, String methodName, Class<?>[] parameterTypes, Object[] args) {
        final RPCCallMessage callMessage = new RPCCallMessage(ThreadUtil.getThreadId(), false, ifClass, methodName, parameterTypes, args);
        final Message replyMsg = sendSyncMessage(callMessage);
        if (replyMsg instanceof RPCCallRespMessage) return (RPCCallRespMessage) replyMsg; else return new RPCCallRespMessage(ThreadUtil.getThreadId(), false, new IllegalStateException("Unexpected message returned " + replyMsg.getClass()));
    }

    /**
     * Retrieve the connection id object associated with the current processing thread. A connection
     * id is associated with each execution by the thread-pool.
     *
     * @return The connection id object supplied.
     */
    public static Object getConnectionId() {
        return connectionIdTL.get();
    }

    /**
     * Start the RPCHandler. This will trigger the handler to initialise the connection and be ready
     * to start serving calls.
     */
    public void start() {
        (new Thread(this)).start();
        try {
            initialisationLatch.await();
        } catch (final InterruptedException e) {
        }
    }
}
