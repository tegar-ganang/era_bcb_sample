package de.kumpe.hadooptimizer.jeneva.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.BrokenBarrierException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import de.kumpe.hadooptimizer.jeneva.client.ClientService;
import de.kumpe.hadooptimizer.jeneva.server.ServerTask.ClientTaskService;

public final class JenevaServer<I> {

    static final Log log = LogFactory.getLog(JenevaServer.class);

    private final class ServerServiceImpl implements ServerService {

        @Override
        public <V> V execute(final ServerTask<V> serverTask) throws Exception {
            if (log.isDebugEnabled()) {
                log.debug("Executing ServerTask: " + serverTask);
            }
            serverTask.port = port;
            serverTask.clientTaskService = clientService;
            return serverTask.call();
        }
    }

    private final class ClientServiceImpl implements ClientService, ClientTaskService {

        private final Queue<Runnable> waitingTasks = new LinkedList<Runnable>();

        private Runnable currentClientTask;

        @Override
        public synchronized Runnable nextTask() throws RemoteException, InterruptedException {
            while (null == currentClientTask) {
                if (log.isTraceEnabled()) {
                    log.trace("Waiting for next client task...");
                }
                wait();
            }
            if (log.isDebugEnabled()) {
                log.debug("Returning next client task...");
            }
            return currentClientTask;
        }

        @Override
        public synchronized void addClientTask(final Runnable clientTask) {
            if (log.isTraceEnabled()) {
                log.trace("Adding next client task: " + clientTask);
            }
            waitingTasks.add(clientTask);
            if (null == currentClientTask) {
                setCurrentClientTask(waitingTasks.poll());
            }
        }

        @Override
        public synchronized void removeClientTask(final Runnable clientTask) {
            if (log.isTraceEnabled()) {
                log.trace("Removing next client task: " + clientTask);
            }
            waitingTasks.remove(clientTask);
            if (clientTask.equals(currentClientTask)) {
                setCurrentClientTask(null);
            }
        }

        private synchronized void setCurrentClientTask(final Runnable clientTask) {
            if (log.isTraceEnabled()) {
                log.trace("Set current client task: " + clientTask);
            }
            currentClientTask = clientTask;
            notifyAll();
        }

        @Override
        public byte[] findClass(final String name) throws ClassNotFoundException {
            if (log.isDebugEnabled()) {
                log.debug("Finding class " + name);
            }
            try {
                final String classFile = "/" + name.replace('.', '/') + ".class";
                final InputStream input = getClass().getResourceAsStream(classFile);
                if (null == input) {
                    throw new ClassNotFoundException("cannot find class file");
                }
                final ByteArrayOutputStream output = new ByteArrayOutputStream(input.available());
                final byte[] buf = new byte[1024];
                int read;
                while (0 <= (read = input.read(buf))) {
                    output.write(buf, 0, read);
                }
                if (log.isTraceEnabled()) {
                    log.trace("Class found.");
                }
                return output.toByteArray();
            } catch (final IOException e) {
                throw new ClassNotFoundException("cannot read class file", e);
            }
        }
    }

    public static <I> void main(final String[] args) throws RemoteException, AlreadyBoundException, InterruptedException, BrokenBarrierException, UnknownHostException {
        if (null == System.getSecurityManager()) {
            if (log.isInfoEnabled()) {
                log.info("Creating new SecurityManager...");
            }
            System.setSecurityManager(new SecurityManager());
        }
        new JenevaServer<I>(args).run();
        if (log.isInfoEnabled()) {
            log.info("Shutting down JenevaServer...");
        }
        System.exit(0);
    }

    public static ServerService getServerService() throws RemoteException, NotBoundException {
        final String host = getSystemProperty("jeneva.host");
        final int port = Integer.parseInt(getSystemProperty("jeneva.port"));
        return getServerService(host, port);
    }

    public static ServerService getServerService(final String host, final int port) throws RemoteException, NotBoundException, AccessException {
        final Registry registry = LocateRegistry.getRegistry(host, port);
        final ServerService serverService = (ServerService) registry.lookup(ServerService.class.getSimpleName());
        return serverService;
    }

    private final ServerServiceImpl serverService = new ServerServiceImpl();

    private final ClientServiceImpl clientService = new ClientServiceImpl();

    private final int port;

    private boolean quit;

    private JenevaServer(final String[] args) {
        port = Integer.parseInt(getSystemProperty("jeneva.port"));
    }

    private void run() throws RemoteException, AlreadyBoundException {
        if (log.isInfoEnabled()) {
            log.info("Creating Registry...");
        }
        final Registry registry = LocateRegistry.createRegistry(port);
        if (log.isInfoEnabled()) {
            log.info("Exporting ClientService...");
        }
        final Remote clientServiceStub = UnicastRemoteObject.exportObject(clientService, port);
        if (log.isTraceEnabled()) {
            log.trace("Stub: " + clientServiceStub);
        }
        if (log.isInfoEnabled()) {
            log.info("Binding ClientService to Registry...");
        }
        registry.bind(ClientService.class.getSimpleName(), clientServiceStub);
        if (log.isInfoEnabled()) {
            log.info("Exporting ServerService...");
        }
        final Remote serverServiceStub = UnicastRemoteObject.exportObject(serverService, port);
        if (log.isTraceEnabled()) {
            log.trace("Stub: " + serverServiceStub);
        }
        if (log.isInfoEnabled()) {
            log.info("Binding ServerService to Registry...");
        }
        registry.bind(ServerService.class.getSimpleName(), serverServiceStub);
        if (log.isInfoEnabled()) {
            log.info("JenevaServer ready.");
        }
        waitForQuit();
    }

    private static String getSystemProperty(final String property) {
        final String value = System.getProperty(property);
        if (null == value) {
            throw new NullPointerException(property + " system property was not specified");
        }
        return value;
    }

    public synchronized void quit() {
        quit = true;
        notifyAll();
    }

    private synchronized void waitForQuit() {
        try {
            while (!quit) {
                this.wait();
            }
        } catch (final InterruptedException e) {
        }
    }
}
