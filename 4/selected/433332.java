package org.labrad;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import org.labrad.data.Context;
import org.labrad.data.Data;
import org.labrad.data.Packet;
import org.labrad.data.PacketInputStream;
import org.labrad.data.PacketOutputStream;
import org.labrad.data.Request;
import org.labrad.errors.IncorrectPasswordException;
import org.labrad.errors.LoginFailedException;
import org.labrad.events.ConnectionListener;
import org.labrad.events.ConnectionListenerSupport;
import org.labrad.events.MessageListener;
import org.labrad.events.MessageListenerSupport;
import org.labrad.util.LookupProvider;
import org.labrad.util.Util;

/**
 * 
 * @author Matthew Neeley
 *
 */
public class ConnectionImpl implements Connection {

    /**
   * Create a new connection object.
   * Properties such as host, port and password will be initialized
   * from environment variable, if these have been set.  Otherwise,
   * default values will be used.
   */
    public ConnectionImpl() {
        host = Util.getEnv("LABRADHOST", Constants.DEFAULT_HOST);
        port = Util.getEnvInt("LABRADPORT", Constants.DEFAULT_PORT);
        password = Util.getEnv("LABRADPASSWORD", Constants.DEFAULT_PASSWORD);
        connected = false;
    }

    private String name;

    private String host;

    private int port;

    private String password;

    private long ID;

    private String loginMessage;

    private boolean connected = false;

    /**
   * Get the ID assigned by the manager after connecting to LabRAD.
   * @return the iD
   */
    public long getId() {
        return ID;
    }

    /**
   * Get the welcome message returned by the manager
   * after connecting to LabRAD.
   * @return the login massage
   */
    public String getLoginMessage() {
        return loginMessage;
    }

    /**
   * Indicates whether we are connected to LabRAD.
   * @return a boolean indicating the connection status
   */
    public boolean isConnected() {
        return connected;
    }

    private void setConnected(boolean connected) {
        boolean old = this.connected;
        this.connected = connected;
        propertyChangeListeners.firePropertyChange("connected", old, connected);
        if (connected) {
            connectionListeners.fireConnected();
        } else {
            connectionListeners.fireDisconnected();
        }
    }

    private final PropertyChangeSupport propertyChangeListeners = new PropertyChangeSupport(this);

    private final MessageListenerSupport messageListeners = new MessageListenerSupport(this);

    private final ConnectionListenerSupport connectionListeners = new ConnectionListenerSupport(this);

    /**
   * Add a listener for property change events.
   * @param listener
   */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeListeners.addPropertyChangeListener(listener);
    }

    /**
   * Remove a listener for property change events.
   * @param listener
   */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeListeners.removePropertyChangeListener(listener);
    }

    /**
   * Add a listener for LabRAD message events.
   * @param listener
   */
    public void addMessageListener(MessageListener listener) {
        messageListeners.addListener(listener);
    }

    /**
   * Remove a listener for LabRAD message events.
   * @param listener
   */
    public void removeMessageListener(MessageListener listener) {
        messageListeners.removeListener(listener);
    }

    /**
   * Add a listener for connection events.
   * @param listener
   */
    public void addConnectionListener(ConnectionListener listener) {
        connectionListeners.addListener(listener);
    }

    /**
   * Remove a listener for connection events.
   * @param listener
   */
    public void removeConnectionListener(ConnectionListener listener) {
        connectionListeners.removeListener(listener);
    }

    private Socket socket;

    private Thread reader, writer;

    private PacketInputStream inputStream;

    private PacketOutputStream outputStream;

    private BlockingQueue<Packet> writeQueue;

    /** Request IDs that are available to be reused. */
    private RequestDispatcher requestDispatcher;

    /** Thread pool for handling lookups. */
    private ExecutorService executor = Executors.newCachedThreadPool();

    /** Performs server and method lookups. */
    private LookupProvider lookupProvider = new LookupProvider(this);

    /**
   * Connect to the LabRAD manager.
   * @throws UnknownHostException if the host and port are not valid
   * @throws IOException if a network error occurred
   * @throws IncorrectPasswordException if the password was not correct
   * @throws LoginFailedException if the login failed for some other reason
   */
    public void connect() throws UnknownHostException, IOException, LoginFailedException, IncorrectPasswordException {
        socket = new Socket(host, port);
        socket.setTcpNoDelay(true);
        socket.setKeepAlive(true);
        inputStream = new PacketInputStream(socket.getInputStream());
        outputStream = new PacketOutputStream(socket.getOutputStream());
        writeQueue = new LinkedBlockingQueue<Packet>();
        requestDispatcher = new RequestDispatcher(writeQueue);
        reader = new Thread(new Runnable() {

            public void run() {
                try {
                    while (!Thread.interrupted()) handlePacket(inputStream.readPacket());
                } catch (IOException e) {
                    if (!Thread.interrupted()) close(e);
                }
            }
        }, "Packet Reader Thread");
        writer = new Thread(new Runnable() {

            public void run() {
                try {
                    while (true) {
                        Packet p = writeQueue.take();
                        outputStream.writePacket(p);
                    }
                } catch (InterruptedException e) {
                } catch (IOException e) {
                    close(e);
                }
            }
        }, "Packet Writer Thread");
        reader.start();
        writer.start();
        try {
            connected = true;
            doLogin(password);
        } finally {
            connected = false;
        }
        setConnected(true);
    }

    /**
   * Logs in to LabRAD using the standard protocol.
   * @param password
   * @throws IncorrectPasswordException if the password was not correct
   * @throws LoginFailedException if the login failed for some other reason
   */
    private void doLogin(String password) throws LoginFailedException, IncorrectPasswordException {
        long mgr = Constants.MANAGER;
        Data data, response;
        try {
            response = sendAndWait(new Request(mgr)).get(0);
            MessageDigest md;
            try {
                md = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("MD5 hash not supported.");
            }
            byte[] challenge = response.getBytes();
            md.update(challenge);
            md.update(password.getBytes(Data.STRING_ENCODING));
            try {
                data = Data.valueOf(md.digest());
                response = sendAndWait(new Request(mgr).add(0, data)).get(0);
            } catch (ExecutionException ex) {
                throw new IncorrectPasswordException();
            }
            loginMessage = response.getString();
            response = sendAndWait(new Request(mgr).add(0, getLoginData())).get(0);
            ID = response.getWord();
        } catch (InterruptedException ex) {
            throw new LoginFailedException(ex);
        } catch (ExecutionException ex) {
            throw new LoginFailedException(ex);
        } catch (IOException ex) {
            throw new LoginFailedException(ex);
        }
    }

    private Data getLoginData() {
        Data data = Data.ofType("ws");
        data.get(0).setWord(Constants.PROTOCOL);
        data.get(1).setString(name);
        return data;
    }

    /**
   * Closes the network connection to LabRAD.
   */
    public void close() {
        close(new IOException("Connection closed."));
    }

    /**
   * Closes the connection to LabRAD after an error.
   * @param cause
   */
    private synchronized void close(Throwable cause) {
        if (isConnected()) {
            setConnected(false);
            executor.shutdown();
            requestDispatcher.failAll(cause);
            writer.interrupt();
            try {
                writer.join();
            } catch (InterruptedException e) {
            }
            reader.interrupt();
            try {
                socket.close();
            } catch (IOException e) {
            }
            try {
                reader.join();
            } catch (InterruptedException e) {
            }
        }
    }

    /** Low word of next context that will be created. */
    private long nextContext = 1L;

    private final Object contextLock = new Object();

    /**
   * Create a new context for this connection.
   * @return
   */
    public Context newContext() {
        synchronized (contextLock) {
            return new Context(0, nextContext++);
        }
    }

    /**
   * Send a LabRAD request.
   * @param request the request that will be made
   * @return a Future that returns a list of Data when the request is done
   */
    public Future<List<Data>> send(final Request request) {
        return send(request, null);
    }

    /**
   * Send a request with an explicit callback.  When the request is
   * completed, the callback will be dispatched using the
   * EventQueue.invokeLater mechanism.
   * @param request the request that will be made
   * @param callback provides methods that will be called when done
   * @return a Future that returns a list of Data when the request is done
   */
    public Future<List<Data>> send(final Request request, final RequestCallback callback) {
        Future<List<Data>> result;
        lookupProvider.doLookupsFromCache(request);
        if (request.needsLookup()) {
            result = executor.submit(new Callable<List<Data>>() {

                public List<Data> call() throws Exception {
                    lookupProvider.doLookups(request);
                    return sendWithoutLookups(request, callback).get();
                }
            });
        } else {
            result = sendWithoutLookups(request, callback);
        }
        return result;
    }

    /**
   * Makes a LabRAD request synchronously.  The request is sent over LabRAD and the
   * calling thread will block until the result is available.
   * @param request the request that will be sent
   * @return a list of Data, one for each record in the request
   * @throws InterruptedException if the network thread was interrupted
   * @throws ExecutionException if the request returned an error or was canceled
   * @throws IOException if a network error occurred
   */
    public List<Data> sendAndWait(Request request) throws InterruptedException, ExecutionException {
        return send(request).get();
    }

    /**
   * Sends a LabRAD message to the specified server.  In this case,
   * lookups are done synchronously so that any exceptions will
   * be thrown to the caller immediately.
   * @param server
   * @param records
   */
    public void sendMessage(final Request request) throws InterruptedException, ExecutionException {
        lookupProvider.doLookups(request);
        sendMessageWithoutLookups(request);
    }

    /**
   * Makes a LabRAD request asynchronously.  The request is sent over LabRAD and an
   * object is returned that can be queried to see if the request has completed or
   * to wait for completion.
   * @param request the request that will be sent
   * @param callback an optional callback to be invoked on completion
   * @throws RuntimeException if not connected or IDs not looked up
   */
    private Future<List<Data>> sendWithoutLookups(final Request request, final RequestCallback callback) {
        if (!isConnected()) {
            throw new RuntimeException("Not connected.");
        }
        if (request.needsLookup()) {
            throw new RuntimeException("Server and/or setting IDs not looked up!");
        }
        return requestDispatcher.startRequest(request, callback);
    }

    /**
   * Sends a LabRAD message without making any lookup requests.
   * @param request the request that will be made
   * @throws RuntimeException if not connected or IDs not looked up
   */
    private void sendMessageWithoutLookups(final Request request) {
        if (!isConnected()) {
            throw new RuntimeException("Not connected.");
        }
        if (request.needsLookup()) {
            throw new RuntimeException("Server and/or setting IDs not looked up!");
        }
        writeQueue.add(Packet.forMessage(request));
    }

    /**
   * Handle packets coming in from the wire.
   * @param packet
   */
    private void handlePacket(Packet packet) {
        int request = packet.getRequest();
        if (request < 0) {
            requestDispatcher.finishRequest(packet);
        } else if (request == 0) {
            messageListeners.fireMessage(packet);
        } else {
        }
    }

    /**
   * Tests some of the basic functionality of the client connection.
   * This method requires that the "Python Test Server" be running
   * to complete all of its tests successfully.
   * @param args
   * @throws IncorrectPasswordException
   * @throws LoginFailedException
   * @throws IOException
   * @throws ExecutionException
   * @throws InterruptedException
   */
    public static void main(String[] args) throws IncorrectPasswordException, LoginFailedException, IOException, ExecutionException, InterruptedException {
        Data response;
        long start, end;
        int nEcho = 5;
        int nRandomData = 1000;
        int nPings = 10000;
        String server = "Python Test Server";
        String setting = "Get Random Data";
        List<Future<List<Data>>> requests = new ArrayList<Future<List<Data>>>();
        ConnectionImpl c = new ConnectionImpl();
        c.connect();
        c.sendAndWait(new Request(server).add("Echo Delay", new Data("v[s]").setValue(1.0)));
        System.out.println("echo with delays...");
        start = System.currentTimeMillis();
        for (int i = 0; i < nEcho; i++) {
            requests.add(c.send(new Request(server).add("Delayed Echo", Data.valueOf(4L))));
        }
        for (Future<List<Data>> request : requests) {
            request.get();
            System.out.println("Got one!");
        }
        end = System.currentTimeMillis();
        System.out.println("done.  elapsed: " + (end - start) + " ms.");
        requests.clear();
        System.out.println("getting random data, with printing...");
        start = System.currentTimeMillis();
        for (int i = 0; i < nRandomData; i++) {
            requests.add(c.send(new Request(server).add(setting)));
        }
        for (Future<List<Data>> request : requests) {
            response = request.get().get(0);
            System.out.println("got packet: " + response.pretty());
        }
        end = System.currentTimeMillis();
        System.out.println("done.  elapsed: " + (end - start) + " ms.");
        requests.clear();
        System.out.println("getting random data, make pretty, but don't print...");
        requests.clear();
        start = System.currentTimeMillis();
        for (int i = 0; i < nRandomData; i++) {
            requests.add(c.send(new Request(server).add(setting)));
        }
        for (Future<List<Data>> request : requests) {
            request.get().get(0).pretty();
        }
        end = System.currentTimeMillis();
        System.out.println("done.  elapsed: " + (end - start) + " ms.");
        System.out.println("getting random data, no printing...");
        requests.clear();
        start = System.currentTimeMillis();
        for (int i = 0; i < nRandomData; i++) {
            requests.add(c.send(new Request(server).add(setting)));
        }
        for (Future<List<Data>> request : requests) {
            request.get();
        }
        end = System.currentTimeMillis();
        System.out.println("done.  elapsed: " + (end - start) + " ms.");
        start = System.currentTimeMillis();
        response = c.sendAndWait(new Request(server).add("debug")).get(0);
        System.out.println("Debug output: " + response.pretty());
        end = System.currentTimeMillis();
        System.out.println("done.  elapsed: " + (end - start) + " ms.");
        System.out.println("pinging manager " + nPings + " times...");
        requests.clear();
        start = System.currentTimeMillis();
        for (int i = 0; i < nPings; i++) {
            requests.add(c.send(new Request("Manager")));
        }
        for (Future<List<Data>> request : requests) {
            request.get();
        }
        end = System.currentTimeMillis();
        System.out.println("done.  elapsed: " + (end - start) + " ms.");
        c.close();
    }
}
