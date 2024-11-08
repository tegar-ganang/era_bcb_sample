package org.seamantics.rmi;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.channels.FileLock;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Properties;
import javax.jcr.Repository;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.apache.jackrabbit.jca.JCARepositoryManager;
import org.apache.jackrabbit.rmi.jackrabbit.JackrabbitServerAdapterFactory;
import org.apache.jackrabbit.rmi.remote.RemoteRepository;
import org.apache.jackrabbit.rmi.server.ServerAdapterFactory;
import org.jboss.mx.util.MBeanServerLocator;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;
import org.jboss.seam.log.Log;
import org.jboss.system.ServiceMBean;
import org.jboss.system.ServiceMBeanSupport;

@Name("org.seamantics.repositoryServer")
@Scope(ScopeType.APPLICATION)
@Startup
@Install(false)
public class RepositoryServer extends ServiceMBeanSupport implements RepositoryServerMBean {

    @Logger
    Log log;

    private String repositoryHomeDir;

    private boolean master = false;

    private int masterPort = 1099;

    private String masterHost = "localhost";

    private String slaveHost = "";

    private int slavePort = 0;

    private int masterHeartBeatInterval = 1000;

    private Thread masterHeartBeatThread;

    private String repositoryRmiName = "org.seamantics.repository";

    private boolean running = false;

    private RemoteRepository remoteRepository;

    private MBeanServer localMbeanServer;

    private MBeanServerConnection slaveMbeanServer;

    private URL jcrDatasourceUrl;

    private ObjectName serverObjectName;

    private File repositoryConfig;

    public static RepositoryServer instance() {
        return (RepositoryServer) Component.getInstance(RepositoryServer.class, ScopeType.APPLICATION);
    }

    @Create
    public void createComponent() throws Exception {
        if (repositoryHomeDir == null) {
            throw new IllegalStateException("no repository home dir defined");
        }
        localMbeanServer = MBeanServerLocator.locateJBoss();
        serverObjectName = new ObjectName("org.seamantics", "service", "repositoryServer");
        File serverBaseDir = (File) localMbeanServer.getAttribute(new ObjectName("jboss.system", "type", "ServerConfig"), "ServerHomeDir");
        File datasource = new File(serverBaseDir, "conf/seamantics/jcr-ds.xml");
        jcrDatasourceUrl = new URL("file://" + datasource.getAbsolutePath());
        repositoryConfig = new File(serverBaseDir, "conf/jackrabbit/repository.xml");
        lookupSlave();
        localMbeanServer.registerMBean(this, serverObjectName);
        try {
            start();
        } catch (Exception e) {
            destroyComponent();
            throw e;
        }
    }

    private void lookupSlave() throws NamingException {
        if (isMaster() && slavePort > 0) {
            Properties slaveEnv = new Properties();
            slaveEnv.put(Context.INITIAL_CONTEXT_FACTORY, "org.jnp.interfaces.NamingContextFactory");
            slaveEnv.put(Context.PROVIDER_URL, "jnp://" + slaveHost + ":" + slavePort);
            slaveEnv.put(Context.URL_PKG_PREFIXES, "org.jboss.naming:org.jnp.interfaces");
            InitialContext slaveCtx = new InitialContext(slaveEnv);
            try {
                slaveMbeanServer = (MBeanServerConnection) slaveCtx.lookup("jmx/rmi/RMIAdaptor");
            } catch (NamingException e) {
                log.info("Could not find slave server");
            }
        }
    }

    @Override
    public void startService() {
        if (isMaster()) {
            startMasterServer();
        } else {
            startMasterHeartBeat();
        }
    }

    public void startMasterServer() {
        checkServerIsNotRunning();
        boolean stoppedSlave = false;
        try {
            lookupSlave();
            if (slaveMbeanServer != null) {
                if ((Boolean) slaveMbeanServer.getAttribute(serverObjectName, "Running")) {
                    slaveMbeanServer.invoke(serverObjectName, "stop", new Object[] {}, new String[] {});
                    stoppedSlave = true;
                    log.info("slave repository server was serving - now master is take over and slave was switched off");
                }
            }
            startServer(masterHost, masterPort);
            if (slaveMbeanServer != null && stoppedSlave) {
                if ((Integer) slaveMbeanServer.getAttribute(serverObjectName, "State") == ServiceMBean.STOPPED) {
                    slaveMbeanServer.invoke(serverObjectName, "start", new Object[] {}, new String[] {});
                    log.info("started up slave repository server - it is listenen to our heartbeat now");
                } else {
                    log.fatal("slave was stopped by master, but failed to bring it back online");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not start master repository server", e);
        }
    }

    public void startSlaveServer() {
        checkServerIsNotRunning();
        try {
            startServer(slaveHost, slavePort);
        } catch (Exception e) {
            throw new RuntimeException("Could not start slave repository server", e);
        }
    }

    private void checkServerIsNotRunning() {
        if (isRunning()) throw new IllegalStateException("Seamantics RMI-Repository server is already running");
    }

    private void startServer(String rmiHost, int rmiPort) throws InstanceNotFoundException, MalformedObjectNameException, ReflectionException, MBeanException, NullPointerException, NamingException, RemoteException {
        localMbeanServer.invoke(new ObjectName("jboss.system", "service", "MainDeployer"), "deploy", new Object[] { jcrDatasourceUrl }, new String[] { "java.net.URL" });
        Repository repository = (Repository) new InitialContext().lookup("java:jcr/local");
        ServerAdapterFactory factory = new JackrabbitServerAdapterFactory();
        remoteRepository = factory.getRemoteRepository(repository);
        Properties env = new Properties();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "org.jnp.interfaces.NamingContextFactory");
        env.put(Context.PROVIDER_URL, "jnp://" + rmiHost + ":" + rmiPort);
        env.put(Context.URL_PKG_PREFIXES, "org.jboss.naming:org.jnp.interfaces");
        InitialContext ctx = new InitialContext(env);
        ctx.bind(getRemoteURL(), remoteRepository);
        running = true;
        log.info("Seamantics remote repository is now available on #0", getRemoteURL());
    }

    public void stopService() {
        if (isRunning()) {
            try {
                InitialContext ctx = new InitialContext();
                ctx.unbind(getRemoteURL());
                UnicastRemoteObject.unexportObject(remoteRepository, true);
                log.info("Seamantics remote repository removed from #0", getRemoteURL());
                localMbeanServer.invoke(new ObjectName("jboss.system", "service", "MainDeployer"), "undeploy", new Object[] { jcrDatasourceUrl }, new String[] { "java.net.URL" });
                JCARepositoryManager.getInstance().shutdown();
                running = false;
            } catch (Exception e) {
                throw new RuntimeException("Could not stop seamantics repository server", e);
            }
        }
    }

    @Destroy
    public void destroyComponent() throws MBeanRegistrationException, InstanceNotFoundException {
        stop();
        localMbeanServer.unregisterMBean(serverObjectName);
    }

    private void startMasterHeartBeat() {
        Runnable heartbeat = new Runnable() {

            private boolean jbossStarted = false;

            public void run() {
                while (true) {
                    if (!jbossStarted) {
                        try {
                            jbossStarted = (Boolean) localMbeanServer.getAttribute(new ObjectName("jboss.system", "type", "Server"), "Started");
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        if (getState() != ServiceMBean.STARTED && getState() != ServiceMBean.STARTING) {
                            log.info("Master heartbeat thread stopped");
                            return;
                        }
                        try {
                            RandomAccessFile repositoryLockFile = new RandomAccessFile(repositoryHomeDir + "/.lock", "rw");
                            try {
                                FileLock lock = repositoryLockFile.getChannel().tryLock();
                                if (lock != null && lock.isValid()) {
                                    lock.release();
                                    startSlaveServer();
                                    return;
                                } else {
                                    log.trace("Master repository is alive");
                                }
                            } finally {
                                repositoryLockFile.close();
                            }
                        } catch (Exception e) {
                            log.error("Master heartbeat thread failed", e);
                        }
                    }
                    try {
                        Thread.sleep(masterHeartBeatInterval);
                    } catch (InterruptedException e) {
                        log.fatal("master heartbeat thread was interrupted", e);
                        return;
                    }
                }
            }
        };
        masterHeartBeatThread = new Thread(heartbeat, "Seamantics Master Heartbeat");
        masterHeartBeatThread.start();
    }

    public boolean isRunning() {
        return running;
    }

    public String getRemoteURL() {
        if (isMaster()) {
            return "jnp://" + masterHost + ":" + masterPort + "/" + repositoryRmiName;
        } else {
            return "jnp://" + slaveHost + ":" + slavePort + "/" + repositoryRmiName;
        }
    }

    public void setMaster(boolean master) {
        this.master = master;
    }

    public boolean isMaster() {
        return master;
    }

    public String getRepositoryHomeDir() {
        return repositoryHomeDir;
    }

    public void setRepositoryHomeDir(String repositoryHomeDir) {
        this.repositoryHomeDir = repositoryHomeDir;
    }

    public int getMasterPort() {
        return masterPort;
    }

    public void setMasterPort(int masterPort) {
        this.masterPort = masterPort;
    }

    public String getMasterHost() {
        return masterHost;
    }

    public void setMasterHost(String masterHost) {
        this.masterHost = masterHost;
    }

    public String getSlaveHost() {
        return slaveHost;
    }

    public void setSlaveHost(String slaveHost) {
        this.slaveHost = slaveHost;
    }

    public int getSlavePort() {
        return slavePort;
    }

    public void setSlavePort(int slavePort) {
        this.slavePort = slavePort;
    }

    public int getMasterHeartBeatInterval() {
        return masterHeartBeatInterval;
    }

    public void setMasterHeartBeatInterval(int masterHeartBeatInterval) {
        this.masterHeartBeatInterval = masterHeartBeatInterval;
    }

    public String getRepositoryRmiName() {
        return repositoryRmiName;
    }

    public Thread getMasterHeartBeatThread() {
        return masterHeartBeatThread;
    }

    public File getRepositoryConfig() {
        return repositoryConfig;
    }
}
