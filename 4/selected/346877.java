package org.hopto.pentaj.jexin.client;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;
import javax.net.ServerSocketFactory;
import org.apache.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.*;

/**
 * <pre>
 * TODO Get checked exceptions from method and allow any with 0 arg ctor to be injected (how to make this work when
 * I'm sending all the injectables up front?
 * TODO Keep map of methods by Method with signature, etc?? 
 * TODO Allow checked exceptions in injectable exceptions config but only allow them to be thrown from methods that declare them
 * </pre>
 * 
 * Sends limited stack trace information to a client and allows the client to inject {@link RuntimeException}s in real time
 * either before or after a method. This class is only intended to be used by the TraceAspect. It should be instantiated as a
 * Spring singleton and only in non-production environments.
 * 
 * Protocol details:
 * <ol>
 * <li>A client connects (any currently connected client will be disconnected)</li>
 * <li>The node name (String) is sent to the client</li>
 * <li>A {@link Map} of ID (Integer) to description (String) is sent to the client for all injectable exceptions</li>
 * <li>When the first monitored method is called a STACK_FRAME_START_ACTION is sent to the client</li>
 * <li>The thread ID (long) is sent to the client</li>
 * <li>The thread name (String) is sent to the client</li>
 * <li>The method signature (String) is sent to the client</li>
 * <li>The injectable exceptions for the method (int[]) is sent to the client</li>
 * <li>The client responds with the PROCEED_ACTION or the ID of an error to be injected</li>
 * <li>If the most recent stack frame is ending with a return and not throwing an exception, a STACK_FRAME_END_ACTION is sent to
 * the client followed by the thread ID (long)</li>
 * <li>If the most recent stack frame is ending because an exception was thrown, a STACK_FRAME_EXCEPTION_ACTION is sent to the
 * client followed by the thread ID (long) then followed by the {@link Throwable} description (String) (this includes any
 * exception injected because of the client's response to the STACK_FRAME_START_ACTION)</li>
 * <li>If a child stack frame is starting (the client received another STACK_FRAME_START_ACTION before the current frame ends),
 * go to step 3</li>
 * </ol>
 * 
 * If STACK_FRAME_END_ACTION or STACK_FRAME_EXCEPTION_ACTION occurs before the first STACK_FRAME_START_ACTION the client should
 * discard them.
 */
public class TraceServer {

    public static final int DEFAULT_PORT = 4466;

    public static final int DEFAULT_READ_TIMEOUT = 5000;

    static final int PROCEED_ACTION = Integer.MIN_VALUE;

    static final int STACK_FRAME_START_ACTION = PROCEED_ACTION + 1;

    static final int STACK_FRAME_END_ACTION = PROCEED_ACTION + 2;

    static final int STACK_FRAME_EXCEPTION_ACTION = PROCEED_ACTION + 3;

    private static final Logger log = Logger.getLogger(TraceServer.class);

    private static final int[] NO_INJECTABLES = new int[0];

    private final ServerSocketFactory socketFactory;

    private String nodeName;

    private int port = DEFAULT_PORT;

    private int readTimeout = DEFAULT_READ_TIMEOUT;

    private final Map<Integer, InjectableException> injectableExceptions = new HashMap<Integer, InjectableException>();

    private final Map<Integer, String> injectableExceptionInfo = new HashMap<Integer, String>();

    private ServerSocket serverSocket;

    private Thread serverThread;

    private volatile boolean running;

    private volatile Socket clientSocket;

    private ObjectOutputStream clientOutputStream;

    private ObjectInputStream clientInputStream;

    public TraceServer() {
        socketFactory = ServerSocketFactory.getDefault();
    }

    public TraceServer(ServerSocketFactory socketFactory) {
        this.socketFactory = socketFactory;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public void setInjectableExceptions(Set<InjectableException> injectableExceptions) {
        for (InjectableException injectableException : injectableExceptions) {
            this.injectableExceptions.put(injectableException.id, injectableException);
        }
    }

    /**
	 * Sets up the exception set and server thread and socket to accept new clients
	 */
    public void init() {
        for (InjectableException injectableException : injectableExceptions.values()) {
            this.injectableExceptionInfo.put(injectableException.id, injectableException.description);
        }
        try {
            serverSocket = socketFactory.createServerSocket(port, 0);
        } catch (IOException e) {
            log.error("Failed to create trace server socket on port " + port, e);
            throw new RuntimeException("Failed to create trace server socket on port " + port, e);
        }
        serverThread = new Thread(new Runnable() {

            public void run() {
                acceptClient();
            }
        });
        running = true;
        serverThread.start();
    }

    /**
	 * Closes client and server sockets
	 */
    public void destroy() {
        running = false;
        closeClientSocket();
        closeServerSocket();
    }

    /**
	 * Called before a method invocation. Sends the stack frame start info to any connected client. Visibility is package because
	 * only TraceAspect should be calling this method.
	 * 
	 * @param joinPoint
	 */
    void beforeMethod(JoinPoint joinPoint) {
        if (clientSocket == null) {
            return;
        }
        Object joinPointSignature = joinPoint.getSignature();
        String signature;
        Traceable annotation;
        if (joinPointSignature instanceof MethodSignature) {
            Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
            annotation = method.getAnnotation(Traceable.class);
            signature = buildSignature(method.getDeclaringClass(), method.getName(), method.getParameterTypes());
        } else if (joinPointSignature instanceof ConstructorSignature) {
            Constructor<?> constructor = ((ConstructorSignature) joinPoint.getSignature()).getConstructor();
            annotation = ((Traceable) constructor.getAnnotation(Traceable.class));
            signature = buildSignature(constructor.getDeclaringClass(), constructor.getDeclaringClass().getSimpleName(), constructor.getParameterTypes());
        } else {
            throw new IllegalArgumentException("Only methods and constructors can be traced");
        }
        int[] injectableExceptions = annotation == null ? NO_INJECTABLES : annotation.injectableExceptions();
        writeToClientAndProcessResponse(STACK_FRAME_START_ACTION, Thread.currentThread().getName(), signature, injectableExceptions);
    }

    /**
	 * Called after a method returns without throwing an exception. Sends the stack frame end info to any connected client.
	 * Visibility is package because only TraceAspect should be calling this method.
	 * 
	 */
    void afterMethod() {
        if (clientSocket == null) {
            return;
        }
        writeToClient(STACK_FRAME_END_ACTION);
    }

    /**
	 * Called after a method throws an exception. Sends the stack frame exception info to any connected client. Visibility is
	 * package because only TraceAspect should be calling this method.
	 * 
	 * @param t
	 */
    void afterMethod(Throwable t) {
        if (clientSocket == null) {
            return;
        }
        writeToClient(STACK_FRAME_EXCEPTION_ACTION, t.getClass().getSimpleName() + ": " + t.getMessage());
    }

    private synchronized void writeToClientAndProcessResponse(int action, Object... params) {
        if (clientOutputStream == null) {
            return;
        }
        writeToClient(action, params);
        processClientResponse(action);
    }

    private synchronized void writeToClient(int action, Object... params) {
        if (clientOutputStream == null) {
            return;
        }
        try {
            clientOutputStream.writeInt(action);
            clientOutputStream.writeLong(Thread.currentThread().getId());
            for (Object param : params) {
                clientOutputStream.writeObject(param);
            }
            clientOutputStream.flush();
        } catch (IOException e) {
            closeClientSocket();
        }
    }

    private void processClientResponse(int requestAction) {
        if (clientInputStream != null) {
            try {
                int action = clientInputStream.readInt();
                if (action == PROCEED_ACTION || requestAction == STACK_FRAME_EXCEPTION_ACTION) {
                    return;
                }
                injectException(action);
            } catch (IOException e) {
                closeClientSocket();
            }
        }
    }

    private void injectException(int id) {
        InjectableException injectableException = injectableExceptions.get(id);
        if (injectableException != null) {
            throw injectableException.exception;
        }
        log.warn("Ignored attempt to inject unknown exception; id=" + id);
    }

    private String buildSignature(Class<?> declaringClass, String methodName, Class<?>[] parameterTypes) {
        StringBuilder signature = new StringBuilder(declaringClass.getSimpleName());
        signature.append('.');
        signature.append(methodName);
        signature.append('(');
        boolean first = true;
        for (Class<?> clazz : parameterTypes) {
            if (first) {
                first = false;
            } else {
                signature.append(',');
            }
            signature.append(clazz.getSimpleName());
        }
        signature.append(')');
        return signature.toString();
    }

    private void acceptClient() {
        while (running) {
            try {
                setupNewClient(serverSocket.accept());
            } catch (IOException e) {
                if (running) {
                    log.error("Failed while setting up new client", e);
                }
                closeClientSocket();
            }
        }
    }

    private synchronized void setupNewClient(Socket newSocket) throws IOException {
        newSocket.setSoTimeout(readTimeout);
        closeClientSocket();
        clientSocket = newSocket;
        clientOutputStream = new ObjectOutputStream(clientSocket.getOutputStream());
        clientInputStream = new ObjectInputStream(clientSocket.getInputStream());
        sendClientConfig();
    }

    private void sendClientConfig() throws IOException {
        clientOutputStream.writeObject(nodeName);
        clientOutputStream.writeObject(injectableExceptionInfo);
        clientOutputStream.flush();
    }

    private synchronized void closeClientSocket() {
        if (clientSocket != null) {
            clientInputStream = null;
            clientOutputStream = null;
            try {
                clientSocket.close();
            } catch (IOException e) {
                log.warn("Failed to close client socket", e);
            }
            clientSocket = null;
        }
    }

    private void closeServerSocket() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                log.warn("Failed to close server socket", e);
            }
            serverSocket = null;
        }
    }
}
