package net.sf.openrds;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Responsible for starting a very simple HTTP server that will receive GET
 * requests for classes and respond to them.
 * @author Rodrigo
 * @version Aug 27, 2005 1:06:55 PM
 */
final class SimpleRmiHttpServer extends Thread {

    /** Data written when a class is not found */
    private static final byte[] NOT_FOUND_404 = "HTTP/1.1 404 Not Found\nContent-Length: 0\n\n".getBytes();

    /** Thread group for socket workers */
    private ThreadGroup group = new ThreadGroup("SocketWorker Group");

    /** Port number */
    private final int port;

    /** Registers if this thread is running or not */
    private boolean running = true;

    /** Holds IO error from thread */
    private IOException error = null;

    /** ServerSocket to listen for requests */
    private ServerSocket serverSocket = null;

    /**
	 * Creates and starts a new HTTP server that will answer on the given port number.
	 * @param port port number to listen for requests.
	 * @throws IOException if it was not possible to start the server
	 */
    public SimpleRmiHttpServer(final int port) throws IOException {
        this.port = port;
        this.setName("RMI Http Server");
        this.setDaemon(true);
        synchronized (this) {
            this.start();
            this.doWait();
        }
        if (this.error != null) {
            throw this.error;
        }
    }

    /** {@inheritDoc} */
    public void run() {
        try {
            this.serverSocket = new ServerSocket(this.port);
            this.doNotify();
            while (this.running) {
                final Socket socket = this.serverSocket.accept();
                new SocketWorker(socket, this.group);
            }
        } catch (IOException e) {
            this.error = e;
        } finally {
            this.doNotify();
        }
    }

    /** Calls <b>wait()</b> and handles <b>InterruptedException</b> */
    private void doWait() {
        try {
            this.wait();
        } catch (InterruptedException e) {
        }
    }

    /** Acquires lock on <b>this</b> and calls <b>notifyAll()</b> */
    private void doNotify() {
        synchronized (this) {
            this.notifyAll();
        }
    }

    /**
	 * Stops the execution of this daemon and closes the socket listener.
	 * @param block if true, this method will block untill every child thread
	 * has terminated it's execution.
	 */
    public void stopRunning(final boolean block) {
        this.running = false;
        this.interrupt();
        try {
            this.serverSocket.close();
        } catch (IOException e1) {
        }
        if (block) {
            final Thread threads[] = new Thread[20];
            while (group.activeCount() > 0) {
                final int size = group.enumerate(threads);
                for (int i = 0; i < size; i++) {
                    try {
                        threads[i].join();
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
        }
    }

    /**
	 * This worker is responsible for reading data from an incomming connection
	 * and writing back the requested class.
	 * @author Rodrigo
	 */
    private static final class SocketWorker extends Thread {

        /** Connection beeing handled */
        private final Socket socket;

        /**
		 * Creates and starts a new worker to handle the given socket
		 * connection
		 * @param socket incomming connection
		 * @param group thread group of this thread
		 */
        private SocketWorker(final Socket socket, final ThreadGroup group) {
            super(group, "Socket worker for " + socket);
            this.socket = socket;
            this.start();
        }

        /** {@inheritDoc} */
        public void run() {
            try {
                final BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String className = null;
                String str = null;
                while (className == null && (str = in.readLine()) != null) {
                    if (str.startsWith("GET ") || str.startsWith("get ")) {
                        final int endIndex = str.indexOf(' ', 4);
                        className = str.substring(4, endIndex != -1 ? endIndex : str.length());
                    }
                }
                if (className != null) {
                    if (className.startsWith("/")) {
                        className = className.substring(1);
                    }
                    writeClass(className);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (Exception e) {
                }
            }
        }

        /**
		 * Writes the given class to socket's output.
		 * @param className class name
		 * @throws IOException on any IO error
		 */
        private void writeClass(String className) throws IOException {
            final InputStream classIn = ClassLoader.getSystemResourceAsStream(className);
            final OutputStream out = socket.getOutputStream();
            if (classIn != null) {
                try {
                    final byte[] buffer = new byte[2048];
                    int read;
                    while ((read = classIn.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                } finally {
                    classIn.close();
                }
            } else {
                out.write(NOT_FOUND_404);
            }
            out.flush();
        }
    }
}
