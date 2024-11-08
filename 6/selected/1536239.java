package pubweb.user;

import static padrmi.Server.getDefaultServer;
import static pubweb.user.Consumer.debug;
import java.net.URL;
import padrmi.exception.PpException;
import pubweb.NotConnectedException;
import pubweb.service.Consumer2Supernode;
import pubweb.supernode.StaticSupernodeConnector;
import pubweb.supernode.SupernodeConnector;

public class SupernodeRegistry extends Thread {

    public static final long DAEMON_THREAD_SLEEP_INTERVAL = 10000L;

    private volatile boolean loggedIn, running, stopped;

    private long latestReportBack;

    private long reportBackInterval;

    private SupernodeConnector supernodeConnector;

    private Consumer2Supernode supernode;

    public SupernodeRegistry() {
        try {
            reportBackInterval = Long.parseLong(System.getProperty("pubweb.time.report")) * 1000L;
        } catch (NumberFormatException nfe) {
            System.err.println("warning: incorrect or missing setting for property pubweb.time.report -- using default value 300");
            reportBackInterval = 300000L;
        }
        try {
            Class<?> c = Class.forName(System.getProperty("pubweb.supernode.connection"));
            supernodeConnector = (SupernodeConnector) c.newInstance();
        } catch (Throwable t) {
            System.err.println("warning: could not load supernode connector -- using default implementation:");
            t.printStackTrace();
            supernodeConnector = new StaticSupernodeConnector();
        }
    }

    public void start() {
        loggedIn = false;
        running = true;
        stopped = false;
        super.start();
    }

    public void run() {
        while (running) {
            if (!loggedIn) {
                try {
                    supernodeConnector.connect();
                    supernode = (Consumer2Supernode) getDefaultServer().getProxyFactory().createProxy(new URL(supernodeConnector.getSupernodeUrl() + "/" + Consumer2Supernode.class.getSimpleName()), Consumer2Supernode.class);
                    loggedIn = true;
                    latestReportBack = 0L;
                } catch (Exception e) {
                    System.out.println("failed to connect to supernode -- retrying ...");
                    if (debug) e.printStackTrace();
                }
            }
            if (loggedIn && System.currentTimeMillis() - latestReportBack > reportBackInterval) {
                try {
                    supernode.login();
                    latestReportBack = System.currentTimeMillis();
                } catch (PpException e) {
                    loggedIn = false;
                    System.out.println("error reporting back -> supernode connection lost");
                    if (debug) e.printStackTrace();
                }
            }
            try {
                sleep(DAEMON_THREAD_SLEEP_INTERVAL);
            } catch (InterruptedException ie) {
            }
        }
        synchronized (this) {
            stopped = true;
            notify();
        }
    }

    public void shutdown() {
        running = false;
        interrupt();
        synchronized (this) {
            while (!stopped) try {
                wait();
            } catch (InterruptedException ie) {
            }
        }
        try {
            if (loggedIn) supernode.logout();
        } catch (Exception e) {
            System.out.println("failed to logout at supernode");
            if (debug) e.printStackTrace();
        }
    }

    public Consumer2Supernode getSupernode() throws NotConnectedException {
        if (!loggedIn) {
            throw new NotConnectedException("not connected to supernode");
        }
        return supernode;
    }

    public URL getSupernodeURL() {
        return supernodeConnector.getSupernodeUrl();
    }
}
