package pubweb.worker;

import static padrmi.Server.getDefaultServer;
import static pubweb.worker.Worker.debug;
import java.net.URL;
import padrmi.exception.PpException;
import pubweb.NotConnectedException;
import pubweb.service.Worker2Supernode;
import pubweb.supernode.StaticSupernodeConnector;
import pubweb.supernode.SupernodeConnector;
import pubweb.worker.bench.CompPowerEstimator;
import pubweb.worker.bench.SimpleCompPowerEstimator;

public class SupernodeRegistry extends Thread {

    public static final long DAEMON_THREAD_SLEEP_INTERVAL = 10000L;

    public static final long INITIAL_SLEEP_INTERVAL = 30000L;

    private volatile boolean loggedIn, running, stopped;

    private long latestReportBack;

    private long reportBackInterval;

    private CompPowerEstimator compPowerEstimator;

    private SupernodeConnector supernodeConnector;

    private Worker2Supernode supernode;

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
        try {
            Class<?> c = Class.forName(System.getProperty("pubweb.bench"));
            compPowerEstimator = (CompPowerEstimator) c.newInstance();
        } catch (Throwable t) {
            System.err.println("warning: could not load computation power estimator -- using default implementation:");
            t.printStackTrace();
            compPowerEstimator = new SimpleCompPowerEstimator();
        }
    }

    public void start() {
        loggedIn = false;
        running = true;
        stopped = false;
        super.start();
    }

    public void run() {
        try {
            sleep(INITIAL_SLEEP_INTERVAL);
        } catch (InterruptedException ie) {
        }
        while (running) {
            if (!loggedIn) {
                try {
                    supernodeConnector.connect();
                    supernode = (Worker2Supernode) getDefaultServer().getProxyFactory().createProxy(new URL(supernodeConnector.getSupernodeUrl() + "/" + Worker2Supernode.class.getSimpleName()), Worker2Supernode.class);
                    supernode.login(compPowerEstimator.getStaticProcessorParameters(), compPowerEstimator.estimateComputationPower());
                    latestReportBack = System.currentTimeMillis();
                    loggedIn = true;
                } catch (Exception e) {
                    System.out.println("failed to connect to supernode -- retrying ...");
                    if (debug) e.printStackTrace();
                }
            } else if (System.currentTimeMillis() - latestReportBack > reportBackInterval) {
                try {
                    try {
                        supernode.reportBack(compPowerEstimator.estimateComputationPower());
                        latestReportBack = System.currentTimeMillis();
                    } catch (PpException e) {
                        loggedIn = false;
                        System.out.println("error reporting back -> supernode connection lost");
                        if (debug) e.printStackTrace();
                    }
                } catch (Throwable t) {
                    loggedIn = false;
                    System.out.println("error determining cpu power -> not reporting back to supernode");
                    if (debug) t.printStackTrace();
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

    public Worker2Supernode getSupernode() throws NotConnectedException {
        if (!loggedIn) {
            throw new NotConnectedException("not connected to supernode");
        }
        return supernode;
    }

    public URL getSupernodeURL() {
        return supernodeConnector.getSupernodeUrl();
    }

    public void updateComputationPower() {
        latestReportBack = 0L;
    }
}
