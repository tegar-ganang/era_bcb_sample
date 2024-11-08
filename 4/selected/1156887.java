package com.objectwave.simpleSockets;

import java.net.*;
import java.io.*;

/**
 * Shell class for implementing multithreaded servers.
 * 
 * @version 2.0
 */
public class ServeClient implements Runnable {

    protected Thread thread = null;

    protected ClientSocket socket = null;

    protected SimpleServer server = null;

    protected boolean debug = System.getProperty("ow.serverVerbose") != null;

    protected boolean alive = true;

    protected int id;

    /**
	 */
    public ServeClient(SimpleServer server, int ident) {
        this(server, ident, null);
    }

    /**
	* Every instance of this class will run on its own thread.  It will be
	* responsible for addressing communication with one client.
	*/
    public ServeClient(SimpleServer server, int ident, Thread t) {
        id = ident;
        this.socket = null;
        this.server = server;
        if (t == null) {
            this.thread = new Thread(this);
        } else {
            this.thread = t;
        }
        this.thread.setPriority(server.getClientThreadPriority());
        this.thread.setName("client" + ident);
        if (t == null) {
            this.thread.start();
        }
    }

    /**
	* Bind this awaiting client to the provided socket.
	* @param s The socket this client should now handle.
	*/
    public synchronized void bind(Socket s) throws IOException {
        this.socket = new ClientSocket(s);
        notifyAll();
    }

    protected int emitReplyStream(InputStream in) throws IOException {
        int totalRead = 0;
        if (in != null) {
            byte[] data = new byte[1024];
            for (int read = in.read(data); read != -1; read = in.read(data)) {
                totalRead += read;
                this.socket.write(data, 0, read);
            }
        }
        return totalRead;
    }

    protected int emitReply(String reply) throws IOException {
        if (reply != null) {
            this.socket.writeString(reply);
            return reply.length();
        }
        return 0;
    }

    public String getName() {
        return this.thread.getName();
    }

    /**
	 * Handle a socket exception during a readString().
	 * @param ex The socket exception to handle.
	 */
    protected void handleSocketException(Exception ex) {
        if (debug) System.out.println("Client lost. " + ex);
        kill(true);
    }

    /**
	* Has a client been bound to us?
	*/
    public synchronized boolean isBound() {
        return socket != null;
    }

    public synchronized void kill(boolean force) {
        alive = false;
        if (force && (socket != null)) terminateConnection();
    }

    protected void loop() throws IOException, EOFException {
        long tstart = 0;
        long tend = 0;
        String request = null;
        InputStream replyStream = null;
        int sent = 0;
        try {
            while (alive && !thread.isInterrupted()) {
                if (debug) System.out.println("Waiting to read from the socket.");
                request = socket.readString();
                if (debug) System.out.println("Read " + request.length() + " bytes from the port.");
                tstart = System.currentTimeMillis();
                replyStream = processRequestStream(request);
                sent = emitReplyStream(replyStream);
                tend = System.currentTimeMillis();
            }
        } catch (java.net.SocketException ex) {
            if (debug) System.out.println("loop(): " + ex);
            handleSocketException(ex);
        } catch (EOFException ex) {
            if (debug) System.out.println("loop(): " + ex);
            throw ex;
        } catch (IOException ex) {
            if (debug) {
                System.out.println("loop(): " + ex);
                ex.printStackTrace();
            }
            System.out.println("loop(): " + ex);
            throw ex;
        } finally {
            if (debug) System.out.println("Exiting loop(): alive = " + alive);
        }
    }

    /**
	* While we claim to be alive and we have no bound socket, wait.
	* Once a socket is bound, interrupt our wait state and return from method.
	*/
    public synchronized void loopForBinding() {
        if (alive) {
            while (socket == null) {
                try {
                    wait();
                } catch (InterruptedException ex) {
                }
                if (!alive) return;
            }
        }
    }

    /**
	* Sent the current thread into a wait state.
	*/
    public synchronized void pause() throws InterruptedException {
        if (debug) System.out.println("Pausing: " + Thread.currentThread().toString());
        wait();
    }

    /**
	 */
    public InputStream processRequestStream(String requestString) {
        return new StringBufferInputStream(processRequest(requestString));
    }

    /**
	// Override this method to process your request.  Returning a string
	// from this method will imply to this to call emitReply(<YourString>);
	*/
    protected String processRequest(String requestString) {
        if (debug) System.out.println("Request: " + requestString + " on thread: " + Thread.currentThread());
        if (debug) return getTestString();
        return null;
    }

    String getTestString() {
        return "AllRight!";
    }

    /**
	* Loop waiting for a client to be bound to us.
	*/
    public void run() {
        try {
            while (alive) {
                loopForBinding();
                if (alive && (socket != null) && (!runConnection())) break;
            }
        } catch (Exception ex) {
            if (debug) ex.printStackTrace();
        } finally {
            if (debug) {
                System.out.println("Client Servant is exiting. Thread should return to pool");
                Thread.currentThread().setName("PooledThread");
            }
        }
    }

    protected boolean runConnection() {
        try {
            loop();
        } catch (EOFException eof) {
        } catch (Exception e) {
            if (debug) e.printStackTrace();
            server.errorlog("caught exception [" + e.getClass().getName() + "] " + e.getMessage());
        }
        if (debug) System.out.println("Terminating Connection");
        terminateConnection();
        return false;
    }

    protected synchronized void terminateConnection() {
        if (socket == null) return;
        try {
            socket.close();
        } catch (IOException ex) {
        }
        socket = null;
    }

    public synchronized void unbind() {
        terminateConnection();
    }
}
